package com.cornspies.fusionfactions.weapon;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.RocketConfig;
import com.cornspies.fusionfactions.registry.ModAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Locking on with the rocket launcher (D61, DESIGN.md §6b.6), decided on the server only. While a player aims a
// guided weapon, the nearest other player close to the crosshair, in range and in sight becomes the target; keeping
// them there for RocketConfig.lockTicks locks on. Losing them for longer than lockGraceTicks drops the target. The
// shooter's client hears and sees the progress (LockStatePayload); the target only learns about it with warning
// gear (LockWarning).
public final class LockOn {
    // Targets with warning gear get an update this often while someone locks on to them.
    private static final int WARNING_INTERVAL = 4;

    // Per player, not saved.
    public static final class State {
        int targetId = -1;
        int lockTicks;
        int lostTicks;
        // What the shooter's client was last told.
        int sentTargetId = -1;
        int sentProgress = -1;
        int age;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    // Public for game tests, whose mock players don't tick.
    public static void tick(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        boolean aiming = stack.getItem() instanceof WeaponItem weapon && weapon.baseStats().guided() && WeaponItem.isAiming(player, stack)
                && player.isAlive() && !player.isSpectator();
        State state = aiming ? player.getData(ModAttachments.LOCK_ON) : player.getExistingDataOrNull(ModAttachments.LOCK_ON);
        if (state == null) {
            return;
        }
        state.age++;
        if (!aiming) {
            state.targetId = -1;
            state.lockTicks = 0;
            state.lostTicks = 0;
        } else {
            WeaponStats stats = ((WeaponItem) stack.getItem()).stats(stack);
            double range = RocketConfig.lockRange() * stats.lockRangeFactor();
            Player current = state.targetId >= 0 && player.level().getEntity(state.targetId) instanceof Player target ? target : null;
            if (current != null && isAimedAt(player, current, range)) {
                state.lostTicks = 0;
                state.lockTicks = Math.min(state.lockTicks + 1, RocketConfig.lockTicks());
            } else if (current != null && current.isAlive() && !current.isSpectator() && ++state.lostTicks <= RocketConfig.lockGraceTicks()) {
                // Briefly out of sight: the progress (or lock) holds for now.
            } else {
                Player candidate = findTarget(player, range);
                state.targetId = candidate == null ? -1 : candidate.getId();
                state.lockTicks = candidate == null ? 0 : 1;
                state.lostTicks = 0;
            }
        }
        sync(player, state);
    }

    // Tells the shooter's client about changes, and warns a target with gear now and then.
    private static void sync(ServerPlayer player, State state) {
        int progress = state.targetId < 0 ? 0 : Math.round(100.0F * state.lockTicks / RocketConfig.lockTicks());
        boolean locked = state.targetId >= 0 && state.lockTicks >= RocketConfig.lockTicks();
        if (state.targetId != state.sentTargetId || progress != state.sentProgress) {
            PacketDistributor.sendToPlayer(player, new LockStatePayload(state.targetId, progress));
            boolean newlyLocked = locked && state.sentProgress < 100;
            state.sentTargetId = state.targetId;
            state.sentProgress = progress;
            if (newlyLocked && player.level().getEntity(state.targetId) instanceof ServerPlayer target) {
                LockWarning.locking(target, player.position(), true);
                return;
            }
        }
        if (state.targetId >= 0 && state.age % WARNING_INTERVAL == 0 && player.level().getEntity(state.targetId) instanceof ServerPlayer target) {
            LockWarning.locking(target, player.position(), locked);
        }
    }

    // The player this shooter has locked on to, if any.
    public static @Nullable Player lockedTarget(Player shooter) {
        State state = shooter.getExistingDataOrNull(ModAttachments.LOCK_ON);
        if (state == null || state.targetId < 0 || state.lockTicks < RocketConfig.lockTicks()) {
            return null;
        }
        return shooter.level().getEntity(state.targetId) instanceof Player target && target.isAlive() ? target : null;
    }

    private static @Nullable Player findTarget(ServerPlayer shooter, double range) {
        Player best = null;
        double bestAngle = Double.MAX_VALUE;
        for (ServerPlayer candidate : ((ServerLevel) shooter.level()).players()) {
            if (candidate == shooter || !isAimedAt(shooter, candidate, range)) {
                continue;
            }
            double angle = angle(shooter, center(candidate));
            if (angle < bestAngle) {
                best = candidate;
                bestAngle = angle;
            }
        }
        return best;
    }

    // In range, near the crosshair and in sight (eyes or middle of the body).
    public static boolean isAimedAt(Player shooter, Player target, double range) {
        if (target == shooter || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        Vec3 eye = shooter.getEyePosition();
        Vec3 middle = center(target);
        if (eye.distanceToSqr(middle) > range * range) {
            return false;
        }
        double cone = Math.toRadians(RocketConfig.lockConeDegrees());
        Vec3 head = target.getEyePosition();
        boolean nearCrosshair = angle(shooter, middle) <= cone || angle(shooter, head) <= cone;
        return nearCrosshair && (canSee(shooter, middle) || canSee(shooter, head));
    }

    private static Vec3 center(Player player) {
        return player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
    }

    // Angle between the shooter's view and the direction to the point, in radians.
    private static double angle(Player shooter, Vec3 point) {
        Vec3 to = point.subtract(shooter.getEyePosition());
        if (to.lengthSqr() < 1.0E-6) {
            return 0.0;
        }
        return Math.acos(Mth.clamp(shooter.getLookAngle().dot(to.normalize()), -1.0, 1.0));
    }

    private static boolean canSee(Player shooter, Vec3 point) {
        return shooter.level().clip(new ClipContext(shooter.getEyePosition(), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter))
                .getType() == HitResult.Type.MISS;
    }

    private LockOn() {
    }
}
