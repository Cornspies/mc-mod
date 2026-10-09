package com.cornspies.fusionfactions.supplydrop;

import com.cornspies.fusionfactions.config.ServerConfig;
import com.cornspies.fusionfactions.util.Compass;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

// Server-wide chat messages and sounds for drops (D14, DESIGN.md §5.2 step 3).
final class DropAnnouncer {
    private static final int ROUNDING = 50;

    static void announced(MinecraftServer server, ServerLevel level, ActiveDrop drop, int seconds) {
        broadcast(server, Component.translatable("fusionfactions.drop.announced", drop.rarity().displayName(), location(level, drop.target()), seconds));
        playToAll(server, SoundEvents.GOAT_HORN_SOUND_VARIANTS.getFirst(), 1.0F);
    }

    static void reminder(MinecraftServer server, ServerLevel level, ActiveDrop drop, int seconds) {
        broadcast(server, Component.translatable("fusionfactions.drop.reminder", drop.rarity().displayName(), location(level, drop.target()), seconds));
        playToAll(server, SoundEvents.NOTE_BLOCK_BELL, 1.2F);
    }

    static void falling(MinecraftServer server, ServerLevel level, ActiveDrop drop) {
        broadcast(server, Component.translatable("fusionfactions.drop.falling", drop.rarity().displayName(), location(level, drop.target())));
    }

    static void landed(MinecraftServer server, ServerLevel level, ActiveDrop drop) {
        broadcast(server, Component.translatable("fusionfactions.drop.landed", drop.rarity().displayName(), location(level, drop.position())));
        playToAll(server, SoundEvents.GOAT_HORN_SOUND_VARIANTS.getFirst(), 0.8F);
    }

    static void looted(MinecraftServer server, ServerLevel level, ActiveDrop drop) {
        broadcast(server, Component.translatable("fusionfactions.drop.looted", drop.rarity().displayName(), location(level, drop.position())));
    }

    static void despawned(MinecraftServer server, ServerLevel level, ActiveDrop drop) {
        broadcast(server, Component.translatable("fusionfactions.drop.despawned", drop.rarity().displayName(), location(level, drop.position())));
    }

    // Location in the configured precision, e.g. "at X 120, Z -340".
    static Component location(ServerLevel level, BlockPos pos) {
        return switch (ServerConfig.ANNOUNCE_PRECISION.get()) {
            case EXACT -> Component.translatable("fusionfactions.drop.location.exact", pos.getX(), pos.getZ());
            case ROUNDED -> Component.translatable("fusionfactions.drop.location.rounded", round(pos.getX()), round(pos.getZ()));
            case DIRECTION -> {
                BlockPos spawn = DropLocations.worldSpawn(level);
                int dx = pos.getX() - spawn.getX();
                int dz = pos.getZ() - spawn.getZ();
                int distance = round((int) Math.sqrt((double) dx * dx + (double) dz * dz));
                yield distance < ROUNDING
                        ? Component.translatable("fusionfactions.drop.location.near_spawn")
                        : Component.translatable("fusionfactions.drop.location.direction", distance,
                                Component.translatable("fusionfactions.direction." + Compass.direction(dx, dz)));
            }
        };
    }

    private static int round(int value) {
        return Math.round(value / (float) ROUNDING) * ROUNDING;
    }

    private static void broadcast(MinecraftServer server, Component message) {
        Component prefix = Component.translatable("fusionfactions.drop.prefix").withStyle(ChatFormatting.GOLD);
        server.getPlayerList().broadcastSystemMessage(Component.empty().append(prefix).append(" ").append(message), false);
    }

    // Plays at each player's own position, so everyone hears it wherever they are.
    private static void playToAll(MinecraftServer server, Holder<SoundEvent> sound, float pitch) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSoundPacket(sound, SoundSource.UI, player.getX(), player.getY(), player.getZ(),
                    1.0F, pitch, player.getRandom().nextLong()));
        }
    }

    private DropAnnouncer() {
    }
}
