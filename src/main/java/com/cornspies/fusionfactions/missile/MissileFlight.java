package com.cornspies.fusionfactions.missile;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// One missile from launch to impact (DESIGN.md §6.5, D56). Saved in MissileData, so flights survive a restart.
// Up, then down: it climbs out of the sky above the launcher, "cruises" off-screen for a time that depends on
// the distance, and comes down on the target. Only the climb and the descent are entities.
public final class MissileFlight {
    public enum Phase implements StringRepresentable {
        LAUNCH("launch"),
        CRUISE("cruise"),
        DESCENT("descent");

        public static final Codec<Phase> CODEC = StringRepresentable.fromEnum(Phase::values);

        private final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    // Climb out of the sky above the launcher.
    public static final int LAUNCH_TICKS = 60;
    // Fall from DESCENT_HEIGHT onto the target.
    public static final int DESCENT_TICKS = 80;
    public static final int DESCENT_HEIGHT = 160;

    public static final Codec<MissileFlight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(MissileFlight::id),
            MissileType.CODEC.fieldOf("type").forGetter(MissileFlight::type),
            Codec.intRange(1, MissileType.MAX_TIER).fieldOf("tier").forGetter(MissileFlight::tier),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(MissileFlight::dimension),
            BlockPos.CODEC.fieldOf("launchPos").forGetter(MissileFlight::launchPos),
            BlockPos.CODEC.fieldOf("target").forGetter(MissileFlight::target),
            UUIDUtil.CODEC.optionalFieldOf("shooter").forGetter(flight -> Optional.ofNullable(flight.shooter)),
            Codec.LONG.fieldOf("launchTime").forGetter(MissileFlight::launchTime),
            Codec.LONG.fieldOf("impactTime").forGetter(MissileFlight::impactTime),
            Phase.CODEC.fieldOf("phase").forGetter(MissileFlight::phase))
            .apply(instance, (id, type, tier, dimension, launchPos, target, shooter, launchTime, impactTime, phase) ->
                    new MissileFlight(id, type, tier, dimension, launchPos, target, shooter.orElse(null), launchTime, impactTime, phase)));

    // Arbitrary constant; combined with the flight id it gives each flight its own chunk-ticket owner.
    private static final long TICKET_OWNER_MSB = 0x4646_4D69_7373_696CL;

    private final int id;
    private final MissileType type;
    private final int tier;
    private final ResourceKey<Level> dimension;
    private final BlockPos launchPos;
    private final BlockPos target;
    // The player who launched it: the causing entity of its damage (§3.2.4). Null for admin launches.
    private final @Nullable UUID shooter;
    private final long launchTime;
    private final long impactTime;
    private Phase phase;
    // Not saved: whether this server run has spawned the descending missile. After a restart the
    // manager spawns it again at the height it should have reached.
    boolean descentSpawned;
    boolean targetLoaded;

    public MissileFlight(int id, MissileType type, int tier, ResourceKey<Level> dimension, BlockPos launchPos, BlockPos target,
            @Nullable UUID shooter, long launchTime, long impactTime, Phase phase) {
        this.id = id;
        this.type = type;
        this.tier = tier;
        this.dimension = dimension;
        this.launchPos = launchPos;
        this.target = target;
        this.shooter = shooter;
        this.launchTime = launchTime;
        this.impactTime = impactTime;
        this.phase = phase;
    }

    // Ticks from launch to impact: the climb, the cruise at the configured speed, and the descent.
    public static long flightTicks(double distance, double blocksPerSecond) {
        return LAUNCH_TICKS + (long) Math.ceil(distance / blocksPerSecond * 20.0) + DESCENT_TICKS;
    }

    public static double horizontalDistance(BlockPos from, BlockPos to) {
        return Math.sqrt(Mth.square((double) to.getX() - from.getX()) + Mth.square((double) to.getZ() - from.getZ()));
    }

    public int id() {
        return this.id;
    }

    public MissileType type() {
        return this.type;
    }

    public int tier() {
        return this.tier;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public BlockPos launchPos() {
        return this.launchPos;
    }

    public BlockPos target() {
        return this.target;
    }

    public @Nullable UUID shooter() {
        return this.shooter;
    }

    public long launchTime() {
        return this.launchTime;
    }

    public long impactTime() {
        return this.impactTime;
    }

    public Phase phase() {
        return this.phase;
    }

    void setPhase(Phase phase) {
        this.phase = phase;
    }

    public long descentStart() {
        return this.impactTime - DESCENT_TICKS;
    }

    // Where the missile is at a given time, horizontally on the straight line from launch to target
    // (D56). Radars use this to see missiles in cruise.
    public Vec3 horizontalPosition(long time) {
        long cruiseStart = this.launchTime + LAUNCH_TICKS;
        long cruiseEnd = this.descentStart();
        double progress = cruiseEnd <= cruiseStart ? 1.0 : Mth.clamp((double) (time - cruiseStart) / (cruiseEnd - cruiseStart), 0.0, 1.0);
        return Vec3.atBottomCenterOf(this.launchPos).lerp(Vec3.atBottomCenterOf(this.target), progress);
    }

    public UUID ticketOwner() {
        return new UUID(TICKET_OWNER_MSB, this.id);
    }
}
