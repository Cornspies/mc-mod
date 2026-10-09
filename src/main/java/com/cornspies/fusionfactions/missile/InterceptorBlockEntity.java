package com.cornspies.fusionfactions.missile;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

// Interceptor (D53, DESIGN.md §6.9): fires a homing rocket at the closest missile coming down within its range
// that it can see, one shot per cooldown, as long as the missile still needs one. Works for anyone (D58).
public class InterceptorBlockEntity extends BlockEntity implements EmpTarget {
    private static final int SEARCH_INTERVAL_TICKS = 2;
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int ENERGY_MAX_INSERT = 10_000;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            InterceptorBlockEntity.this.setChanged();
        }
    };
    private long cooldownUntil;
    private long disabledUntil;

    public InterceptorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.INTERCEPTOR.get(), pos, state);
    }

    public EnergyHandler energy() {
        return this.energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InterceptorBlockEntity interceptor) {
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % SEARCH_INTERVAL_TICKS == 0) {
            interceptor.tick(serverLevel);
        }
    }

    private void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now < this.cooldownUntil || now < this.disabledUntil
                || MissileConfig.requirePower() && this.energy.getAmountAsLong() < MissileConfig.interceptorEnergy()) {
            return;
        }
        Vec3 muzzle = Vec3.atBottomCenterOf(this.worldPosition.above());
        MissileEntity target = this.findTarget(level, muzzle);
        if (target == null) {
            return;
        }
        level.addFreshEntity(new InterceptorRocketEntity(level, muzzle, target, MissileConfig.interceptorDamage()));
        level.playSound(null, this.worldPosition, ModSounds.INTERCEPTOR_LAUNCH.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
        if (MissileConfig.requirePower()) {
            this.energy.set(this.energy.getAmountAsInt() - MissileConfig.interceptorEnergy());
        }
        this.cooldownUntil = now + MissileConfig.interceptorCooldownTicks();
        this.setChanged();
    }

    // The closest missile on its way down in range and in sight that still needs fire.
    private @Nullable MissileEntity findTarget(ServerLevel level, Vec3 muzzle) {
        double range = MissileConfig.interceptorRange();
        MissileEntity best = null;
        double bestDistance = range * range;
        for (MissileEntity missile : level.getEntitiesOfClass(MissileEntity.class, AABB.ofSize(muzzle, range * 2, range * 2, range * 2),
                MissileEntity::needsInterception)) {
            Vec3 aim = missile.position().add(0.0, missile.getBbHeight() / 2.0, 0.0);
            double distance = aim.distanceToSqr(muzzle);
            if (distance < bestDistance && level.clip(new ClipContext(muzzle, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty())).getType() == HitResult.Type.MISS) {
                best = missile;
                bestDistance = distance;
            }
        }
        return best;
    }

    // What the interceptor is doing, for players who click it.
    public Component status(ServerLevel level) {
        long now = level.getGameTime();
        if (now < this.disabledUntil) {
            return Component.translatable("fusionfactions.interceptor.disabled", (this.disabledUntil - now + 19) / 20);
        }
        if (MissileConfig.requirePower() && this.energy.getAmountAsLong() < MissileConfig.interceptorEnergy()) {
            return Component.translatable("fusionfactions.interceptor.no_power", MissileConfig.interceptorEnergy());
        }
        if (now < this.cooldownUntil) {
            return Component.translatable("fusionfactions.interceptor.reloading");
        }
        return Component.translatable("fusionfactions.interceptor.ready", MissileConfig.interceptorRange());
    }

    @Override
    public void empHit(long disabledUntil) {
        this.energy.set(0);
        this.disabledUntil = Math.max(this.disabledUntil, disabledUntil);
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putLong("CooldownUntil", this.cooldownUntil);
        output.putLong("DisabledUntil", this.disabledUntil);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Energy").ifPresent(this.energy::deserialize);
        this.cooldownUntil = input.getLongOr("CooldownUntil", 0L);
        this.disabledUntil = input.getLongOr("DisabledUntil", 0L);
    }
}
