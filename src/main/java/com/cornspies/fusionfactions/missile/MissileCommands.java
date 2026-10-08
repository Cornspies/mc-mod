package com.cornspies.fusionfactions.missile;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Admin commands, permission level 2 (DESIGN.md §6.10):
//   /fusionfactions missile list | clear [<id>]
//   /fusionfactions missile launch <type> <tier> <x z> [<from x z>]
public final class MissileCommands {
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> launch = Commands.literal("launch");
        for (MissileType type : MissileType.values()) {
            launch.then(Commands.literal(type.getSerializedName())
                    .then(Commands.argument("tier", IntegerArgumentType.integer(1, MissileType.MAX_TIER))
                            .then(Commands.argument("target", ColumnPosArgument.columnPos())
                                    .executes(context -> launch(context.getSource(), type, IntegerArgumentType.getInteger(context, "tier"),
                                            ColumnPosArgument.getColumnPos(context, "target"), null))
                                    .then(Commands.argument("from", ColumnPosArgument.columnPos())
                                            .executes(context -> launch(context.getSource(), type, IntegerArgumentType.getInteger(context, "tier"),
                                                    ColumnPosArgument.getColumnPos(context, "target"), ColumnPosArgument.getColumnPos(context, "from")))))));
        }

        event.getDispatcher().register(Commands.literal(FusionFactions.MODID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("missile")
                        .then(launch)
                        .then(Commands.literal("list").executes(context -> list(context.getSource())))
                        .then(Commands.literal("clear").executes(context -> clear(context.getSource()))
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(context -> cancel(context.getSource(), IntegerArgumentType.getInteger(context, "id")))))));
    }

    // Ignores raid windows, range and power. Launches from the given column's surface, or from the source's position.
    private static int launch(CommandSourceStack source, MissileType type, int tier, ColumnPos target, @Nullable ColumnPos from) {
        ServerLevel level = source.getLevel();
        BlockPos targetPos = surface(level, target);
        BlockPos launchPos = from != null ? surface(level, from).above() : BlockPos.containing(source.getPosition());
        MissileFlight flight = MissileManager.launch(level, type, tier, launchPos, targetPos, source.getEntity());
        long seconds = (flight.impactTime() - flight.launchTime()) / 20;
        source.sendSuccess(() -> Component.translatable("commands.fusionfactions.missile.launch", flight.id(), missileName(flight),
                position(targetPos), seconds), true);
        return flight.id();
    }

    // The top block of a column, where a missile aimed at it would hit. Loads the chunk: the heightmap of an
    // unloaded chunk would say the world's bottom.
    private static BlockPos surface(ServerLevel level, ColumnPos column) {
        level.getChunk(SectionPos.blockToSectionCoord(column.x()), SectionPos.blockToSectionCoord(column.z()));
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(column.x(), 0, column.z())).below();
    }

    private static int list(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<MissileFlight> flights = MissileData.get(server).flights();
        if (flights.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.fusionfactions.missile.list.empty"), false);
            return 0;
        }
        long now = server.overworld().getGameTime();
        source.sendSuccess(() -> Component.translatable("commands.fusionfactions.missile.list", flights.size()), false);
        for (MissileFlight flight : flights) {
            long seconds = Math.max(0, flight.impactTime() - now) / 20;
            source.sendSuccess(() -> Component.translatable("commands.fusionfactions.missile.list.entry", flight.id(), missileName(flight),
                    position(flight.launchPos()), position(flight.target()), flight.dimension().identifier().toString(), seconds), false);
        }
        return flights.size();
    }

    private static int clear(CommandSourceStack source) {
        int count = MissileManager.clearAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.fusionfactions.missile.clear", count), true);
        return count;
    }

    private static int cancel(CommandSourceStack source, int id) {
        if (!MissileManager.cancel(source.getServer(), id)) {
            source.sendFailure(Component.translatable("commands.fusionfactions.missile.clear.unknown", id));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.fusionfactions.missile.clear.one", id), true);
        return 1;
    }

    private static Component missileName(MissileFlight flight) {
        return Component.translatable("item.fusionfactions." + flight.type().getSerializedName() + "_missile_" + flight.tier());
    }

    private static Component position(BlockPos pos) {
        return Component.translatable("commands.fusionfactions.drop.position", pos.getX(), pos.getY(), pos.getZ());
    }

    private MissileCommands() {
    }
}
