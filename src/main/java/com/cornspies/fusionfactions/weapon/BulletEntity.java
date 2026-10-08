package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
    private static final int TRAIL_PARTICLES_PER_TICK = 3;
    // Same as vanilla arrows.
    private static final double WATER_INERTIA = 0.6;
    // Blocks per tick. Slower bullets are spent and disappear.
    private static final double MIN_SPEED = 0.25;

    private float damage;
    private int lifetimeTicks = 60;

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

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
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
            for (int i = 0; i < TRAIL_PARTICLES_PER_TICK; i++) {
                double part = i / (double) TRAIL_PARTICLES_PER_TICK;
                this.level().addParticle(inWater ? ParticleTypes.BUBBLE : ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + movement.x * part, this.getY() + movement.y * part, this.getZ() + movement.z * part, 0.0, 0.0, 0.0);
            }
        }
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        this.updateRotation();
        if (inWater) {
            // Water slows bullets like arrows, which limits the underwater range to about 9 blocks.
            this.setDeltaMovement(this.getDeltaMovement().scale(WATER_INERTIA));
        }
        this.applyGravity();
        if (!this.level().isClientSide() && (this.tickCount > this.lifetimeTicks || this.getDeltaMovement().lengthSqr() < MIN_SPEED * MIN_SPEED)) {
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
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.01);
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
