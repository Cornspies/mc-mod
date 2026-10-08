package com.cornspies.fusionfactions.weapon;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

// A fast projectile with slight drop (D19). Damage uses our own damage type with the shooter as the
// causing entity and the bullet as the direct entity (§3.2.4), so team friendly-fire rules apply.
public class BulletEntity extends Projectile {
    private static final double GRAVITY = 0.01;
    // Gravity for bullets that water has slowed down, in water and after leaving it. Fast bullets barely
    // notice, but once drag has slowed one, it curves down: underwater it sinks (about 2 blocks per
    // second) until it reaches the ground, in air it drops in a short arc.
    private static final double GRAVITY_SLOWED = 0.04;
    private static final int TRAIL_PARTICLES_PER_TICK = 3;
    // Underwater the trail is a dense line of bubbles, one per this many blocks (at least one per tick),
    // so the bullet's path is easy to follow (there's no instant tracer underwater, see WeaponClientEffects).
    private static final double BUBBLE_SPACING = 0.3;
    // Same as vanilla arrows.
    private static final double WATER_INERTIA = 0.6;
    // A bullet that water has slowed keeps losing speed in air, so the more water it went through, the
    // sooner it comes down after leaving it.
    private static final double AIR_INERTIA_AFTER_WATER = 0.9;
    // Blocks per tick. Slower bullets are spent: they don't hurt anything and just drop to the ground.
    private static final double MIN_SPEED = 0.25;
    // A spent bullet sinking through deep water disappears after this many ticks at the latest.
    private static final int MAX_AGE_TICKS = 600;
    // A server velocity update has to turn the bullet by more than this (30 degrees) to be applied.
    private static final double MIN_TURN_COS = 0.866;

    private float damage;
    private int lifetimeTicks = 60;
    // Distance moved in the last tick, used to recognise position updates that are one tick behind.
    private double lastStep;
    // Set once the bullet has been in water. Stays set after it leaves the water.
    private boolean slowedByWater;
    // Set once the bullet is too slow to hurt anything. Stays set, also if it speeds up again by falling.
    private boolean spent;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    BulletEntity(Level level, LivingEntity shooter, float damage, int lifetimeTicks) {
        this(ModEntityTypes.BULLET.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.damage = damage;
        this.lifetimeTicks = lifetimeTicks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    // Clients simulate bullets with the same physics from the same starting point, so they are already
    // where the server is. A server position that is slightly behind (about one tick of travel along
    // the flight direction) is just a late update, for example the sync the server sends in the spawn
    // tick. Snapping to it would pull the bullet back and leave a gap in its trail, so it's ignored.
    // Anything else, such as a deflection, still moves the client's bullet to the server's position.
    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return new InterpolationHandler.NoOpInterpolationHandler() {
            @Override
            public boolean interpolateTo(@Nullable PositionPath position, float yRot, float xRot, boolean hasRotation) {
                return position != null && BulletEntity.this.isOneTickBehind(position.endPosition());
            }
        };
    }

    // Velocity updates from the server are a tick late too. Only drag changes a bullet's speed, and the
    // client simulates that itself, so applying a late one would make the bullet too fast for a tick
    // and overshoot. Only updates that turn the bullet (a deflection) are applied.
    @Override
    public void lerpMotion(Vec3 movement) {
        Vec3 current = this.getDeltaMovement();
        boolean turns = current.lengthSqr() < 1.0E-6 || movement.lengthSqr() < 1.0E-6
                || movement.normalize().dot(current.normalize()) < MIN_TURN_COS;
        if (turns) {
            super.lerpMotion(movement);
        }
    }

    private boolean isOneTickBehind(Vec3 serverPosition) {
        Vec3 offset = serverPosition.subtract(this.position());
        return offset.dot(this.getDeltaMovement()) < 0.0 && offset.length() <= this.lastStep * 1.5 + 0.1;
    }

    @Override
    protected double getDefaultGravity() {
        return this.isInWater() || this.slowedByWater ? GRAVITY_SLOWED : GRAVITY;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !this.spent && super.canHitEntity(entity);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = this.getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            this.hitTargetOrDeflectSelf(hit);
        }
        if (this.isRemoved()) {
            return;
        }
        boolean inWater = this.isInWater();
        if (this.level().isClientSide()) {
            int particles = inWater ? Math.max(1, (int) Math.ceil(movement.length() / BUBBLE_SPACING)) : TRAIL_PARTICLES_PER_TICK;
            for (int i = 0; i < particles; i++) {
                double part = i / (double) particles;
                this.level().addParticle(inWater ? ParticleTypes.BUBBLE : ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + movement.x * part, this.getY() + movement.y * part, this.getZ() + movement.z * part, 0.0, 0.0, 0.0);
            }
        }
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        this.lastStep = movement.length();
        this.updateRotation();
        if (inWater) {
            // Water slows bullets like arrows, which limits their underwater range to about 9 blocks.
            this.setDeltaMovement(this.getDeltaMovement().scale(WATER_INERTIA));
            this.slowedByWater = true;
        } else if (this.slowedByWater) {
            this.setDeltaMovement(this.getDeltaMovement().scale(AIR_INERTIA_AFTER_WATER));
        }
        this.applyGravity();
        if (this.getDeltaMovement().lengthSqr() < MIN_SPEED * MIN_SPEED) {
            this.spent = true;
        }
        // The normal lifetime only applies in flight. Spent bullets drop until they reach the ground (onHitBlock).
        if (!this.level().isClientSide() && (this.tickCount > MAX_AGE_TICKS || (!this.spent && this.tickCount > this.lifetimeTicks))) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel level) {
            Entity target = result.getEntity();
            DamageSource source = this.damageSources().source(ModDamageTypes.BULLET, this, this.getOwner());
            target.hurtServer(level, source, this.damage);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel level) {
            Vec3 at = result.getLocation();
            level.sendParticles(this.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.SMOKE, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.01);
            this.discard();
        }
    }

    // Bullets live a few seconds at most. Saving them would let one that froze in a chunk that stopped
    // ticking come back later with a fresh lifetime.
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDeltaMovement(packet.getMovement());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", this.damage);
        output.putInt("Lifetime", this.lifetimeTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.damage = input.getFloatOr("Damage", 0.0F);
        this.lifetimeTicks = input.getIntOr("Lifetime", 60);
    }
}
