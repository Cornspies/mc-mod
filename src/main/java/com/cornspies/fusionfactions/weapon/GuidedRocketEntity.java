package com.cornspies.fusionfactions.weapon;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.RocketConfig;
import com.cornspies.fusionfactions.registry.ModEntityTypes;
import com.cornspies.fusionfactions.registry.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

// The rocket launcher's rocket (DESIGN.md §6b.6). Flies straight, or, fired with a lock, steers towards its target
// at most RocketConfig.turnDegrees per tick, so a sharp dodge or cover beats it. It bursts on the first block or
// entity it touches, or when its fuel runs out, and its blast only hurts entities (D64), with our rocket damage type
// and the shooter as the causing entity. Moved by the server only; clients draw it where the server says. Not saved.
public class GuidedRocketEntity extends Projectile {
    // A target this far away or further is given up (it teleported, or left for another dimension).
    private static final double MAX_TARGET_DISTANCE = 256.0;
    // The blast's knockback at the centre, falling off like the damage.
    private static final double KNOCKBACK = 0.9;
    // Targets with warning gear get an "incoming" update this often.
    private static final int WARNING_INTERVAL = 4;

    private int targetId = -1;
    private float damage;
    private int lifetimeTicks = 200;

    public GuidedRocketEntity(EntityType<? extends GuidedRocketEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public GuidedRocketEntity(Level level, LivingEntity shooter, WeaponStats stats, @Nullable Entity target) {
        this(ModEntityTypes.GUIDED_ROCKET.get(), level);
        this.setOwner(shooter);
        Vec3 look = shooter.getLookAngle();
        this.setPos(shooter.getEyePosition().add(0.0, -0.15, 0.0).add(look.scale(0.6)));
        this.setDeltaMovement(look.scale(stats.bulletSpeed()));
        faceAlong(this, this.getDeltaMovement());
        this.damage = stats.damage();
        this.lifetimeTicks = stats.bulletLifetimeTicks();
        this.targetId = target == null ? -1 : target.getId();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public @Nullable Entity target() {
        if (this.targetId < 0) {
            return null;
        }
        Entity target = this.level().getEntity(this.targetId);
        if (target == null || !target.isAlive() || target.isSpectator() || target.distanceToSqr(this) > MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) {
            return null;
        }
        return target;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = this.target();
        if (target != null) {
            this.steerTowards(target);
            if (target instanceof ServerPlayer player && this.tickCount % WARNING_INTERVAL == 0) {
                LockWarning.incoming(player, this.position());
            }
        }
        Vec3 movement = this.getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            this.hitTargetOrDeflectSelf(hit);
            if (this.isRemoved()) {
                return;
            }
        }
        // The motor's flame and a smoke trail behind the rocket, which carry its visibility at range.
        Vec3 tail = this.position().subtract(movement.normalize().scale(0.4));
        level.sendParticles(ParticleTypes.FLAME, true, true, tail.x, tail.y, tail.z, 1, 0.02, 0.02, 0.02, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, true, true, tail.x, tail.y, tail.z, 1, 0.05, 0.05, 0.05, 0.0);
        for (int i = 1; i <= 2; i++) {
            Vec3 at = tail.subtract(movement.scale(i / 3.0));
            level.sendParticles(ParticleTypes.SMOKE, true, true, at.x, at.y, at.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
        this.setPos(this.position().add(movement));
        faceAlong(this, this.getDeltaMovement());
        if (this.tickCount > this.lifetimeTicks) {
            this.explode(level, this.position());
        }
    }

    // Turns the flight direction towards the target's middle, by no more than the configured angle.
    private void steerTowards(Entity target) {
        Vec3 movement = this.getDeltaMovement();
        double speed = movement.length();
        Vec3 wanted = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(this.position());
        if (speed < 1.0E-6 || wanted.lengthSqr() < 1.0E-6) {
            return;
        }
        this.setDeltaMovement(turn(movement.scale(1.0 / speed), wanted.normalize(), Math.toRadians(RocketConfig.turnDegrees())).scale(speed));
    }

    // Turns an entity along a direction (yaw from atan2(x, z), pitch up from the horizon, like vanilla projectiles), at
    // once rather than eased like Projectile.updateRotation. Clients draw rockets with this rotation.
    public static void faceAlong(Entity entity, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-9) {
            return;
        }
        entity.setYRot((float) Math.toDegrees(Mth.atan2(direction.x, direction.z)));
        entity.setXRot((float) Math.toDegrees(Mth.atan2(direction.y, direction.horizontalDistance())));
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
    }

    // The unit vector from towards to, turned by at most maxAngle radians.
    static Vec3 turn(Vec3 from, Vec3 to, double maxAngle) {
        double angle = Math.acos(Mth.clamp(from.dot(to), -1.0, 1.0));
        if (angle <= maxAngle) {
            return to;
        }
        // Rotate in the plane of both vectors. Straight behind, any perpendicular plane will do.
        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1.0E-9) {
            axis = from.cross(Math.abs(from.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0));
        }
        Vec3 side = axis.normalize().cross(from);
        return from.scale(Math.cos(maxAngle)).add(side.scale(Math.sin(maxAngle))).normalize();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel level) {
            this.explode(level, result.getLocation());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel level) {
            // Just outside the block, so the block doesn't shield everything from its own blast.
            Vec3 back = this.getDeltaMovement().normalize().scale(0.3);
            this.explode(level, result.getLocation().subtract(back));
        }
    }

    // Damage falls off from full at the centre to nothing at the blast radius, measured to the nearest point of each
    // entity's hitbox, and is reduced by cover like a vanilla explosion's. No block is touched (D64).
    private void explode(ServerLevel level, Vec3 at) {
        double radius = RocketConfig.blastRadius();
        DamageSource source = this.damageSources().source(ModDamageTypes.ROCKET, this, this.getOwner());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
            if (!entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            AABB box = entity.getBoundingBox();
            Vec3 nearest = new Vec3(Mth.clamp(at.x, box.minX, box.maxX), Mth.clamp(at.y, box.minY, box.maxY), Mth.clamp(at.z, box.minZ, box.maxZ));
            double falloff = 1.0 - nearest.distanceTo(at) / radius;
            if (falloff <= 0.0) {
                continue;
            }
            double strength = falloff * ServerExplosion.getSeenPercent(at, entity);
            if (strength <= 0.0) {
                continue;
            }
            entity.hurtServer(level, source, (float) (this.damage * strength));
            Vec3 away = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(at);
            if (away.lengthSqr() > 1.0E-6) {
                double push = KNOCKBACK * strength * (1.0 - entity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                // push marks the entity for a motion update, players included.
                entity.push(away.normalize().scale(push).add(0.0, 0.2 * strength, 0.0));
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, true, true, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, true, true, at.x, at.y, at.z, 12, 0.6, 0.6, 0.6, 0.02);
        level.playSound(null, at.x, at.y, at.z, ModSounds.ROCKET_EXPLODE.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        this.discard();
    }

    // Rockets live a few seconds; one frozen in an unloaded chunk shouldn't come back later.
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
