package com.cornspies.fusionfactions.supplydrop;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.ServerConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ColumnPos;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Admin commands, permission level 2 (DESIGN.md §5.3):
//   /fusionfactions drop now [<rarity>|random [<x z>]]
//   /fusionfactions drop next | list | clear
public final class SupplyDropCommands {
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> now = Commands.literal("now")
                .executes(context -> dropNow(context.getSource(), null, null));
        now.then(withPosition(Commands.literal("random"), null));
        for (CrateRarity rarity : CrateRarity.values()) {
            now.then(withPosition(Commands.literal(rarity.getSerializedName()), rarity));
        }

        event.getDispatcher().register(Commands.literal(FusionFactions.MODID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("drop")
                        .then(now)
                        .then(Commands.literal("next").executes(context -> next(context.getSource())))
                        .then(Commands.literal("list").executes(context -> list(context.getSource())))
                        .then(Commands.literal("clear").executes(context -> clear(context.getSource())))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withPosition(LiteralArgumentBuilder<CommandSourceStack> literal, @Nullable CrateRarity rarity) {
        return literal
                .executes(context -> dropNow(context.getSource(), rarity, null))
                .then(Commands.argument("position", ColumnPosArgument.columnPos())
                        .executes(context -> dropNow(context.getSource(), rarity, ColumnPosArgument.getColumnPos(context, "position"))));
    }

    private static int dropNow(CommandSourceStack source, @Nullable CrateRarity rarity, @Nullable ColumnPos at) {
        MinecraftServer server = source.getServer();
        CrateRarity chosen = rarity != null ? rarity : CrateRarity.pick(server.overworld().getRandom());
        SupplyDropManager.StartResult result = SupplyDropManager.startDrop(server, chosen, at);
        ActiveDrop drop = result.drop();
        if (drop == null) {
            source.sendFailure(result.error() != null ? result.error() : Component.translatable("fusionfactions.drop.error.no_location"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.fusionfactions.drop.now", drop.id(), drop.rarity().displayName(), position(drop.target())), true);
        return drop.id();
    }

    private static int next(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (!ServerConfig.ENABLED.getAsBoolean()) {
            source.sendSuccess(() -> Component.translatable("commands.fusionfactions.drop.next.disabled"), false);
            return 0;
        }
        long ticks = SupplyDropData.get(server).ticksUntilNextDrop();
        Component time = duration(ticks >= 0 ? ticks : SupplyDropManager.intervalTicks());
        if (SupplyDropManager.isScheduleRunning(server)) {
            source.sendSuccess(() -> Component.translatable("commands.fusionfactions.drop.next", time), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.fusionfactions.drop.next.paused", time,
                    ServerConfig.MIN_PLAYERS_ONLINE.getAsInt(), server.getPlayerCount()), false);
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, ticks / 20));
    }

    private static int list(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<ActiveDrop> drops = SupplyDropData.get(server).drops();
        if (drops.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.fusionfactions.drop.list.empty"), false);
            return 0;
        }
        long now = server.overworld().getGameTime();
        source.sendSuccess(() -> Component.translatable("commands.fusionfactions.drop.list", drops.size()), false);
        for (ActiveDrop drop : drops) {
            Component timeLeft = duration(drop.phaseEndsAt() - now);
            String key = "commands.fusionfactions.drop.list." + drop.phase().getSerializedName();
            source.sendSuccess(() -> Component.translatable(key, drop.id(), drop.rarity().displayName(), position(drop.position()),
                    drop.dimension().identifier().toString(), timeLeft), false);
        }
        return drops.size();
    }

    private static int clear(CommandSourceStack source) {
        int count = SupplyDropManager.clearAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.fusionfactions.drop.clear", count), true);
        return count;
    }

    private static Component position(BlockPos pos) {
        return Component.translatable("commands.fusionfactions.drop.position", pos.getX(), pos.getY(), pos.getZ());
    }

    private static Component duration(long ticks) {
        long total = Math.max(0, ticks / 20);
        long hours = total / 3600;
        long minutes = total % 3600 / 60;
        long seconds = total % 60;
        if (hours > 0) {
            return Component.translatable("fusionfactions.duration.hours", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return Component.translatable("fusionfactions.duration.minutes", minutes, seconds);
        }
        return Component.translatable("fusionfactions.duration.seconds", seconds);
    }

    private SupplyDropCommands() {
    }
}
