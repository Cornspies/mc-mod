package com.cornspies.fusionfactions.missile;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModEntityTypes;
import com.cornspies.fusionfactions.weapon.ModDamageTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// An interceptor's shot (DESIGN.md §6.9): a fast rocket that steers straight at its missile and damages it on
// contact. Moved by the server; clients draw it with its smoke trail. Not saved.
public class InterceptorRocketEntity extends Projectile {
    // Blocks per tick.
    private static final double SPEED = 4.0;
    private static final int MAX_AGE_TICKS = 80;

    private int targetId = -1;
    private float damage;
    private boolean counted;

    public InterceptorRocketEntity(EntityType<? extends InterceptorRocketEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    InterceptorRocketEntity(Level level, Vec3 start, MissileEntity target, float damage) {
        this(ModEntityTypes.INTERCEPTOR_ROCKET.get(), level);
        this.setPos(start);
        this.targetId = target.getId();
        this.damage = damage;
        target.incomingDamage += damage;
        this.counted = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        MissileEntity target = this.target();
        if (target == null || !target.isPickable() || this.tickCount > MAX_AGE_TICKS) {
            this.discard();
            return;
        }
        Vec3 aim = target.position().add(0.0, target.getBbHeight() / 2.0, 0.0);
        Vec3 toTarget = aim.subtract(this.position());
        double distance = toTarget.length();
        Vec3 step = distance <= SPEED ? toTarget : toTarget.scale(SPEED / distance);
        for (int i = 0; i < 3; i++) {
            Vec3 at = this.position().add(step.scale(i / 3.0));
            level.sendParticles(ParticleTypes.FIREWORK, true, true, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
            level.sendParticles(ParticleTypes.SMOKE, true, true, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        this.setPos(this.position().add(step));
        if (distance <= SPEED || this.getBoundingBox().inflate(0.5).intersects(target.getBoundingBox())) {
            level.sendParticles(ParticleTypes.EXPLOSION, true, true, aim.x, aim.y, aim.z, 1, 0.0, 0.0, 0.0, 0.0);
            this.release(target);
            target.hurtServer(level, this.damageSources().source(ModDamageTypes.MISSILE, this, this.getOwner()), this.damage);
            this.discard();
        }
    }

    private @Nullable MissileEntity target() {
        return this.level().getEntity(this.targetId) instanceof MissileEntity missile ? missile : null;
    }

    private void release(@Nullable MissileEntity target) {
        if (this.counted && target != null) {
            target.incomingDamage -= this.damage;
        }
        this.counted = false;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.release(this.target());
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }
}
