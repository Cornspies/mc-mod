package com.cornspies.fusionfactions.missile;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// The visible part of a missile flight (D56): the climb out of the sky above the launcher, and the fall onto
// the target. The manager decides when each starts and ends; the climb goes through anything and just
// disappears, the descent detonates on the first block or entity it touches. Motion is the same on both
// sides, so clients see it move smoothly. Not saved: the manager spawns a new one after a restart.
public class MissileEntity extends Projectile {
    // The model is drawn this many times the item's size, about 5.5 blocks long, so it can be seen from far
    // away (client.MissileRenderer).
    public static final float MODEL_SCALE = 3.0F;
    private static final double MODEL_LENGTH = 29.2 / 16.0 * MODEL_SCALE;
    // Blocks per tick on the way down.
    public static final double DESCENT_SPEED = (double) MissileFlight.DESCENT_HEIGHT / MissileFlight.DESCENT_TICKS;
    // The climb starts slow and speeds up, like a rocket leaving the pad.
    private static final double ASCENT_START_SPEED = 0.15;
    private static final double ASCENT_ACCELERATION = 1.1;
    private static final double ASCENT_MAX_SPEED = 4.0;
    // Blocks between smoke puffs of the trail.
    private static final double TRAIL_SPACING = 0.7;

    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DESCENDING = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.BOOLEAN);

    private int flightId = -1;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    private MissileEntity(Level level, MissileFlight flight, Vec3 position, boolean descending, @Nullable Entity shooter) {
        this(ModEntityTypes.MISSILE.get(), level);
        this.setPos(position);
        this.xo = position.x;
        this.yo = position.y;
        this.zo = position.z;
        this.flightId = flight.id();
        this.entityData.set(DATA_TYPE, flight.type().ordinal());
        this.entityData.set(DATA_TIER, flight.tier());
        this.entityData.set(DATA_DESCENDING, descending);
        this.setOwner(shooter);
        // Nose up on the climb, nose down on the way down.
        this.setXRot(descending ? -90.0F : 90.0F);
        this.xRotO = this.getXRot();
    }

    static MissileEntity ascending(Level level, MissileFlight flight, Vec3 position, @Nullable Entity shooter) {
        return new MissileEntity(level, flight, position, false, shooter);
    }

    static MissileEntity descending(Level level, MissileFlight flight, Vec3 position, @Nullable Entity shooter) {
        return new MissileEntity(level, flight, position, true, shooter);
    }

    public MissileType missileType() {
        MissileType[] values = MissileType.values();
        return values[Mth.clamp(this.entityData.get(DATA_TYPE), 0, values.length - 1)];
    }

    public int tier() {
        return Mth.clamp(this.entityData.get(DATA_TIER), 1, MissileType.MAX_TIER);
    }

    public boolean isDescending() {
        return this.entityData.get(DATA_DESCENDING);
    }

    int flightId() {
        return this.flightId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TYPE, 0);
        builder.define(DATA_TIER, 1);
        builder.define(DATA_DESCENDING, false);
    }

    @Override
    public void tick() {
        super.tick();
        boolean descending = this.isDescending();
        Vec3 motion = descending
                ? new Vec3(0.0, -DESCENT_SPEED, 0.0)
                : new Vec3(0.0, Math.min(ASCENT_MAX_SPEED, ASCENT_START_SPEED * Math.pow(ASCENT_ACCELERATION, this.tickCount)), 0.0);
        this.setDeltaMovement(motion);

        if (this.level() instanceof ServerLevel level) {
            MissileFlight.Phase phase = descending ? MissileFlight.Phase.DESCENT : MissileFlight.Phase.LAUNCH;
            if (!MissileManager.isInPhase(level.getServer(), this.flightId, phase)) {
                // The climb is over, the flight was cleared, or this entity is a leftover.
                this.discard();
                return;
            }
            this.trail(level, descending, motion);
            if (descending) {
                HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
                if (hit.getType() != HitResult.Type.MISS) {
                    MissileManager.onImpact(level, this, hit.getLocation());
                    return;
                }
                if (this.getY() < level.getMinY()) {
                    this.discard();
                    return;
                }
            }
        }
        this.setPos(this.position().add(motion));
    }

    // A smoke trail that can be seen from far away, with flames at the tail. Puffs along the whole step, so the
    // trail has no gaps even when the missile moves several blocks per tick.
    private void trail(ServerLevel level, boolean descending, Vec3 motion) {
        double tailY = descending ? this.getY() + MODEL_LENGTH : this.getY();
        double length = motion.length();
        int puffs = Math.max(1, (int) Math.ceil(length / TRAIL_SPACING));
        for (int i = 0; i < puffs; i++) {
            double y = tailY - motion.y * i / puffs;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, true, this.getX(), y, this.getZ(), 1, 0.1, 0.1, 0.1, 0.005);
        }
        level.sendParticles(ParticleTypes.FLAME, true, true, this.getX(), tailY, this.getZ(), 3, 0.08, 0.15, 0.08, 0.01);
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.SERVER_AND_CLIENT;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0 * 512.0;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("FlightId", this.flightId);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.flightId = input.getIntOr("FlightId", -1);
    }
}
