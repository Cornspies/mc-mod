package com.cornspies.fusionfactions.supplydrop;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.ServerConfig;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Runs the supply drop cycle (DESIGN.md §5.2): schedule, announce and countdown, fall, land, clean up.
// All state lives in SupplyDropData; this class is stateless.
public final class SupplyDropManager {
    // Blocks above the landing spot where the falling crate appears.
    private static final int DROP_HEIGHT = 100;
    // If a falling crate hasn't landed this long after it should have, place the crate directly.
    private static final int FALL_GRACE_TICKS = 60 * 20;
    private static final int[] REMINDER_SECONDS = { 30, 10 };
    private static final int CHECK_INTERVAL_TICKS = 20;
    // How far up from the impact point to look for a free block for the crate.
    private static final int MAX_PLACEMENT_SHIFT = 8;

    // Holds the landing chunk of each active drop (§3.2.5). Saved tickets are dropped on load:
    // the first tick re-forces the chunks of the drops that are still active.
    private static final TicketController TICKETS = new TicketController(FusionFactions.id("supply_drops"), (level, tickets) -> {
        for (var owner : List.copyOf(tickets.getEntityTickets().keySet())) {
            tickets.removeAllTickets(owner);
        }
    });

    public record StartResult(@Nullable ActiveDrop drop, @Nullable Component error) {
        static StartResult success(ActiveDrop drop) {
            return new StartResult(drop, null);
        }

        static StartResult failure(Component error) {
            return new StartResult(null, error);
        }
    }

    public static void registerTicketController(RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        SupplyDropData data = SupplyDropData.get(server);
        if (!data.ticketsRestored) {
            data.drops.forEach(drop -> forceChunk(server, drop, true));
            data.ticketsRestored = true;
        }
        tickSchedule(server, data);
        long now = server.overworld().getGameTime();
        for (ActiveDrop drop : List.copyOf(data.drops)) {
            tickDrop(server, data, drop, now);
        }
    }

    // D12: fixed interval, counting down only while enough players are online.
    private static void tickSchedule(MinecraftServer server, SupplyDropData data) {
        long interval = intervalTicks();
        if (data.ticksUntilNextDrop < 0 || data.ticksUntilNextDrop > interval) {
            data.ticksUntilNextDrop = interval;
            data.setDirty();
        }
        if (!isScheduleRunning(server)) {
            return;
        }
        data.ticksUntilNextDrop--;
        data.setDirty();
        if (data.ticksUntilNextDrop <= 0) {
            data.ticksUntilNextDrop = interval;
            StartResult result = startDrop(server, CrateRarity.pick(server.overworld().getRandom()), null);
            if (result.error() != null) {
                FusionFactions.LOGGER.warn("Skipped scheduled supply drop: {}", result.error().getString());
            }
        }
    }

    public static long intervalTicks() {
        return ServerConfig.INTERVAL_MINUTES.getAsInt() * 60L * 20L;
    }

    public static boolean isScheduleRunning(MinecraftServer server) {
        return ServerConfig.ENABLED.getAsBoolean() && server.getPlayerCount() >= ServerConfig.MIN_PLAYERS_ONLINE.getAsInt();
    }

    // Announces a new drop. With `at` set, the crate lands at that column (admin command), otherwise somewhere in the zone.
    public static StartResult startDrop(MinecraftServer server, CrateRarity rarity, @Nullable ColumnPos at) {
        ServerLevel level = dropLevel(server);
        if (level == null) {
            return StartResult.failure(Component.translatable("fusionfactions.drop.error.dimension", ServerConfig.DIMENSION.get()));
        }
        Optional<BlockPos> target = at != null
                ? DropLocations.surfaceAt(level, at.x(), at.z())
                : DropLocations.pick(level, level.getRandom());
        if (target.isEmpty()) {
            return StartResult.failure(Component.translatable(at != null ? "fusionfactions.drop.error.position" : "fusionfactions.drop.error.no_location"));
        }

        SupplyDropData data = SupplyDropData.get(server);
        int countdownSeconds = ServerConfig.COUNTDOWN_SECONDS.getAsInt();
        ActiveDrop drop = new ActiveDrop(data.nextId++, rarity, level.dimension(), target.get(), ActiveDrop.Phase.COUNTDOWN,
                level.getGameTime() + countdownSeconds * 20L, null);
        data.drops.add(drop);
        data.setDirty();
        forceChunk(server, drop, true);

        FusionFactions.LOGGER.info("Supply drop #{} ({}) announced at {} in {}", drop.id(), rarity.getSerializedName(), drop.target(), level.dimension().identifier());
        DropAnnouncer.announced(server, level, drop, countdownSeconds);
        return StartResult.success(drop);
    }

    // Removes every active drop and its crate without announcing anything. Returns how many there were.
    public static int clearAll(MinecraftServer server) {
        SupplyDropData data = SupplyDropData.get(server);
        List<ActiveDrop> drops = List.copyOf(data.drops);
        for (ActiveDrop drop : drops) {
            ServerLevel level = server.getLevel(drop.dimension());
            if (level != null && drop.phase() == ActiveDrop.Phase.LANDED
                    && level.getBlockEntity(drop.position()) instanceof SupplyCrateBlockEntity crate) {
                crate.vanish(level);
            }
            // Falling crates notice their drop is gone and remove themselves.
            retire(server, data, drop);
        }
        return drops.size();
    }

    static boolean isFalling(MinecraftServer server, int dropId) {
        ActiveDrop drop = SupplyDropData.get(server).find(dropId);
        return drop != null && drop.phase() == ActiveDrop.Phase.FALLING;
    }

    // Called by the falling crate when it hits the ground.
    static void onCrateLanded(ServerLevel level, FallingCrateEntity entity) {
        entity.discard();
        SupplyDropData data = SupplyDropData.get(level.getServer());
        ActiveDrop drop = data.find(entity.dropId());
        if (drop != null && drop.phase() == ActiveDrop.Phase.FALLING) {
            land(level, data, drop, entity.blockPosition());
        }
    }

    private static void tickDrop(MinecraftServer server, SupplyDropData data, ActiveDrop drop, long now) {
        ServerLevel level = server.getLevel(drop.dimension());
        if (level == null) {
            retire(server, data, drop);
            return;
        }
        switch (drop.phase()) {
            case COUNTDOWN -> {
                long remaining = drop.phaseEndsAt() - now;
                if (remaining <= 0) {
                    release(level, data, drop, now);
                } else if (remaining % 20 == 0) {
                    int seconds = (int) (remaining / 20);
                    for (int reminder : REMINDER_SECONDS) {
                        if (seconds == reminder) {
                            DropAnnouncer.reminder(server, level, drop, seconds);
                        }
                    }
                }
            }
            case FALLING -> {
                if (now >= drop.phaseEndsAt()) {
                    FusionFactions.LOGGER.warn("Falling crate of supply drop #{} never landed, placing it directly", drop.id());
                    BlockPos ground = DropLocations.surfaceAt(level, drop.target().getX(), drop.target().getZ()).orElse(drop.target());
                    land(level, data, drop, ground);
                }
            }
            case LANDED -> {
                if (now % CHECK_INTERVAL_TICKS == 0) {
                    checkLanded(level, data, drop, now);
                }
            }
        }
    }

    // Countdown is over: spawn the falling crate high above the target.
    private static void release(ServerLevel level, SupplyDropData data, ActiveDrop drop, long now) {
        BlockPos target = drop.target();
        double startY = Math.min(level.getMaxY() - 1, target.getY() + DROP_HEIGHT);
        level.addFreshEntity(new FallingCrateEntity(level, target.getX() + 0.5, startY, target.getZ() + 0.5, drop.rarity(), drop.id()));
        long fallTicks = (long) Math.ceil((startY - target.getY()) / FallingCrateEntity.DESCENT_PER_TICK);
        drop.startFalling(now + fallTicks + FALL_GRACE_TICKS);
        data.setDirty();
        DropAnnouncer.falling(level.getServer(), level, drop);
    }

    private static void land(ServerLevel level, SupplyDropData data, ActiveDrop drop, BlockPos impact) {
        BlockPos pos = impact;
        for (int shift = 0; shift < MAX_PLACEMENT_SHIFT && !level.getBlockState(pos).canBeReplaced(); shift++) {
            pos = pos.above();
        }
        long despawnAt = level.getGameTime() + ServerConfig.DESPAWN_MINUTES.getAsInt() * 60L * 20L;
        level.setBlockAndUpdate(pos, SupplyCrateBlock.stateFor(drop.rarity()));
        if (level.getBlockEntity(pos) instanceof SupplyCrateBlockEntity crate) {
            crate.fill(drop.rarity(), level.getRandom().nextLong(), despawnAt);
        }
        drop.land(pos, despawnAt);
        data.setDirty();

        level.playSound(null, pos, ModSounds.SUPPLY_CRATE_LAND.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 20, 0.8, 0.1, 0.8, 0.02);
        FusionFactions.LOGGER.info("Supply drop #{} landed at {}", drop.id(), pos);
        DropAnnouncer.landed(level.getServer(), level, drop);
    }

    // Retires the drop once its crate is gone: emptied by players, or despawned.
    private static void checkLanded(ServerLevel level, SupplyDropData data, ActiveDrop drop, long now) {
        BlockPos pos = drop.position();
        boolean expired = now >= drop.phaseEndsAt();
        if (!level.isLoaded(pos)) {
            // Only possible if the ticket was lost. The crate removes itself on its next load.
            if (expired) {
                retire(level.getServer(), data, drop);
            }
            return;
        }
        boolean present = level.getBlockState(pos).is(ModBlocks.SUPPLY_CRATE.get());
        if (present && expired && level.getBlockEntity(pos) instanceof SupplyCrateBlockEntity crate) {
            crate.vanish(level);
            present = false;
        }
        if (!present) {
            if (expired) {
                DropAnnouncer.despawned(level.getServer(), level, drop);
            } else {
                DropAnnouncer.looted(level.getServer(), level, drop);
            }
            retire(level.getServer(), data, drop);
        }
    }

    private static void retire(MinecraftServer server, SupplyDropData data, ActiveDrop drop) {
        data.drops.remove(drop);
        data.setDirty();
        forceChunk(server, drop, false);
    }

    private static void forceChunk(MinecraftServer server, ActiveDrop drop, boolean add) {
        ServerLevel level = server.getLevel(drop.dimension());
        if (level != null) {
            TICKETS.forceChunk(level, drop.ticketOwner(), SectionPos.blockToSectionCoord(drop.target().getX()),
                    SectionPos.blockToSectionCoord(drop.target().getZ()), add, false);
        }
    }

    private static @Nullable ServerLevel dropLevel(MinecraftServer server) {
        Identifier id = Identifier.tryParse(ServerConfig.DIMENSION.get());
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    private SupplyDropManager() {
    }
}
