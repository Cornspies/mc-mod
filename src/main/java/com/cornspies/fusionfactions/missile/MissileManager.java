package com.cornspies.fusionfactions.missile;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Runs missile flights (DESIGN.md §6.5): launch climb, cruise, descent, impact. All state lives in MissileData.
public final class MissileManager {
    // The target area is loaded this long before impact, so the descent and the blast happen in loaded chunks.
    private static final int TARGET_LOAD_TICKS = 200;
    // If the descending missile hasn't hit anything this long after its impact time (it was killed, or got
    // stuck), it detonates at the target's surface.
    private static final int IMPACT_GRACE_TICKS = 100;
    // Blocks a blast breaks per tick; bigger blasts spread over a few ticks.
    private static final int BLOCKS_PER_TICK = 1024;

    // Holds the launch chunk during the climb, and the 3x3 chunks around the target from shortly before
    // impact until the blast is done (§3.2.5). Saved tickets are dropped on load: the first tick re-forces
    // what the flights still in the air need.
    private static final TicketController TICKETS = new TicketController(FusionFactions.id("missiles"), (level, tickets) -> {
        for (var owner : List.copyOf(tickets.getEntityTickets().keySet())) {
            tickets.removeAllTickets(owner);
        }
    });

    // A blast's blocks still to break, a batch per tick. Not saved: after a restart the rest stays standing.
    record PendingBlast(ResourceKey<Level> dimension, @Nullable Explosion explosion, Iterator<BlockPos> blocks, UUID ticketOwner, BlockPos target) {
    }

    public static void registerTicketController(RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        MissileData data = MissileData.get(server);
        long now = server.overworld().getGameTime();
        if (!data.ticketsRestored) {
            restoreTickets(server, data);
            data.ticketsRestored = true;
        }
        for (MissileFlight flight : List.copyOf(data.flights)) {
            tickFlight(server, data, flight, now);
        }
        breakBlocks(server, data);
    }

    // Starts a flight from launchPos to target. The caller has checked range, power and raid windows.
    public static MissileFlight launch(ServerLevel level, MissileType type, int tier, BlockPos launchPos, BlockPos target, @Nullable Entity shooter) {
        MissileData data = MissileData.get(level.getServer());
        long now = level.getServer().overworld().getGameTime();
        double distance = MissileFlight.horizontalDistance(launchPos, target);
        long impact = now + MissileFlight.flightTicks(distance, MissileConfig.flightSpeed());
        MissileFlight flight = new MissileFlight(data.nextId++, type, tier, level.dimension(), launchPos, target,
                shooter != null ? shooter.getUUID() : null, now, impact, MissileFlight.Phase.LAUNCH);
        data.flights.add(flight);
        data.setDirty();
        forceChunk(level, flight, launchPos, true);

        Vec3 start = Vec3.atBottomCenterOf(launchPos.above());
        level.addFreshEntity(MissileEntity.ascending(level, flight, start, shooter));
        level.playSound(null, start.x, start.y, start.z, ModSounds.MISSILE_LAUNCH.get(), SoundSource.BLOCKS, 8.0F, 1.0F);
        FusionFactions.LOGGER.info("Missile #{} ({} {}) launched from {} at {} in {} by {}, impact in {} s", flight.id(), type.getSerializedName(),
                tier, launchPos, target, level.dimension().identifier(), shooter != null ? shooter.getName().getString() : "the server",
                (impact - now) / 20);
        return flight;
    }

    static boolean isInPhase(MinecraftServer server, int flightId, MissileFlight.Phase phase) {
        MissileFlight flight = MissileData.get(server).find(flightId);
        return flight != null && flight.phase() == phase;
    }

    // Called by the descending missile when it hits a block or an entity.
    static void onImpact(ServerLevel level, MissileEntity missile, Vec3 at) {
        missile.discard();
        MissileData data = MissileData.get(level.getServer());
        MissileFlight flight = data.find(missile.flightId());
        if (flight != null && flight.phase() == MissileFlight.Phase.DESCENT) {
            detonate(level, data, flight, at, missile);
        }
    }

    // Removes every missile in flight without detonating it. Returns how many there were.
    public static int clearAll(MinecraftServer server) {
        MissileData data = MissileData.get(server);
        List<MissileFlight> flights = List.copyOf(data.flights);
        for (MissileFlight flight : flights) {
            // Their entities notice the flight is gone and remove themselves.
            retire(server, data, flight);
        }
        return flights.size();
    }

    // Removes one missile in flight without detonating it. Returns false if there is no such flight.
    public static boolean cancel(MinecraftServer server, int flightId) {
        MissileData data = MissileData.get(server);
        MissileFlight flight = data.find(flightId);
        if (flight == null) {
            return false;
        }
        retire(server, data, flight);
        return true;
    }

    private static void tickFlight(MinecraftServer server, MissileData data, MissileFlight flight, long now) {
        ServerLevel level = server.getLevel(flight.dimension());
        if (level == null) {
            retire(server, data, flight);
            return;
        }
        if (now >= flight.impactTime() - TARGET_LOAD_TICKS && !flight.targetLoaded) {
            forceTargetArea(level, flight, true);
            flight.targetLoaded = true;
        }
        switch (flight.phase()) {
            case LAUNCH -> {
                if (now >= flight.launchTime() + MissileFlight.LAUNCH_TICKS) {
                    flight.setPhase(MissileFlight.Phase.CRUISE);
                    data.setDirty();
                    forceChunk(level, flight, flight.launchPos(), false);
                }
            }
            case CRUISE -> {
                if (now >= flight.descentStart()) {
                    flight.setPhase(MissileFlight.Phase.DESCENT);
                    data.setDirty();
                }
            }
            case DESCENT -> {
                if (!flight.descentSpawned) {
                    spawnDescent(level, flight, now);
                } else if (now >= flight.impactTime() + IMPACT_GRACE_TICKS) {
                    FusionFactions.LOGGER.warn("Missile #{} never hit anything, detonating it at the target", flight.id());
                    Vec3 ground = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, flight.target()));
                    // A stand-in that is never spawned, so the blast still has a missile as its direct source.
                    detonate(level, data, flight, ground, MissileEntity.descending(level, flight, ground, shooter(level, flight)));
                }
            }
        }
    }

    // The missile comes down above the target, at the height it should be at now: DESCENT_HEIGHT at the
    // start of the descent, less after a restart in the middle of one.
    private static void spawnDescent(ServerLevel level, MissileFlight flight, long now) {
        BlockPos target = flight.target();
        long remaining = Math.max(0, flight.impactTime() - now);
        double y = target.getY() + 1.0 + MissileEntity.DESCENT_SPEED * remaining;
        y = Math.min(y, level.getMaxY() - 1.0);
        level.addFreshEntity(MissileEntity.descending(level, flight, new Vec3(target.getX() + 0.5, y, target.getZ() + 0.5), shooter(level, flight)));
        level.playSound(null, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, ModSounds.MISSILE_INCOMING.get(), SoundSource.BLOCKS, 6.0F, 1.0F);
        flight.descentSpawned = true;
    }

    // The flight ends here. Its blocks break over the next ticks; the target area stays loaded until then.
    private static void detonate(ServerLevel level, MissileData data, MissileFlight flight, Vec3 at, @Nullable Entity missile) {
        FusionFactions.LOGGER.info("Missile #{} hit {} in {}", flight.id(), BlockPos.containing(at), level.dimension().identifier());
        data.flights.remove(flight);
        data.setDirty();
        MissileBlast.Result blast = MissileBlast.detonate(level, flight.type(), flight.tier(), at, missile, shooter(level, flight));
        if (blast == null) {
            FusionFactions.LOGGER.info("Missile #{}'s explosion was cancelled by another mod", flight.id());
        }
        data.pending.add(new PendingBlast(level.dimension(), blast == null ? null : blast.explosion(),
                blast == null ? List.<BlockPos>of().iterator() : blast.blocks().iterator(), flight.ticketOwner(), flight.target()));
    }

    private static void breakBlocks(MinecraftServer server, MissileData data) {
        int budget = BLOCKS_PER_TICK;
        while (budget > 0 && !data.pending.isEmpty()) {
            PendingBlast blast = data.pending.peek();
            ServerLevel level = server.getLevel(blast.dimension());
            if (level != null) {
                while (budget > 0 && blast.blocks().hasNext()) {
                    MissileBlast.breakBlock(level, blast.explosion(), blast.blocks().next());
                    budget--;
                }
                if (blast.blocks().hasNext()) {
                    return;
                }
                forceTargetArea(level, blast.ticketOwner(), blast.target(), false);
            }
            data.pending.poll();
        }
    }

    private static void restoreTickets(MinecraftServer server, MissileData data) {
        for (MissileFlight flight : data.flights) {
            ServerLevel level = server.getLevel(flight.dimension());
            if (level != null && flight.phase() == MissileFlight.Phase.LAUNCH) {
                forceChunk(level, flight, flight.launchPos(), true);
            }
        }
    }

    private static void retire(MinecraftServer server, MissileData data, MissileFlight flight) {
        data.flights.remove(flight);
        data.setDirty();
        ServerLevel level = server.getLevel(flight.dimension());
        if (level != null) {
            forceChunk(level, flight, flight.launchPos(), false);
            forceTargetArea(level, flight, false);
        }
    }

    private static @Nullable Entity shooter(ServerLevel level, MissileFlight flight) {
        return flight.shooter() == null ? null : level.getServer().getPlayerList().getPlayer(flight.shooter());
    }

    private static void forceChunk(ServerLevel level, MissileFlight flight, BlockPos pos, boolean add) {
        TICKETS.forceChunk(level, flight.ticketOwner(), SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), add, false);
    }

    private static void forceTargetArea(ServerLevel level, MissileFlight flight, boolean add) {
        forceTargetArea(level, flight.ticketOwner(), flight.target(), add);
    }

    private static void forceTargetArea(ServerLevel level, UUID owner, BlockPos target, boolean add) {
        int centerX = SectionPos.blockToSectionCoord(target.getX());
        int centerZ = SectionPos.blockToSectionCoord(target.getZ());
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                TICKETS.forceChunk(level, owner, x, z, add, false);
            }
        }
    }

    private MissileManager() {
    }
}
