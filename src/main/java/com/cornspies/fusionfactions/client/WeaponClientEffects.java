package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.WeaponStats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Cosmetic shot feedback on the shooter's own client (D22): muzzle flash, tracer, recoil and the
// fire sound, shown right away instead of after a server round trip. It predicts the server's
// timing from the same stats; the server alone decides what is actually fired and hit.
public final class WeaponClientEffects {
    private static final double TRACER_RANGE = 48.0;
    private static final double TRACER_STEP = 0.8;

    private static int cooldown;

    public static void onClientTick(ClientTickEvent.Post event) {
        if (cooldown > 0) {
            cooldown--;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !WeaponKeys.triggerHeld()
                || !(player.getMainHandItem().getItem() instanceof WeaponItem weapon)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        WeaponStats stats = weapon.stats(stack);
        // Same rules as WeaponItem.tryFire: holding only repeats on automatic weapons and never interrupts a reload.
        if (!WeaponKeys.triggerPulled() && (!stats.automatic() || WeaponItem.isReloading(stack))) {
            return;
        }
        if (cooldown > 0 || WeaponItem.getAmmo(stack) <= 0 || WeaponKeys.triggerHeldTicks() < stats.spinUpTicks()) {
            return;
        }
        cooldown = stats.fireIntervalTicks();
        shotEffects(minecraft.level, player, stats);
    }

    private static void shotEffects(ClientLevel level, LocalPlayer player, WeaponStats stats) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Roughly where the gun's muzzle is: forward, a little right and down.
        Vec3 right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
        Vec3 muzzle = eye.add(look.scale(0.7)).add(right.scale(0.3)).add(0.0, -0.2, 0.0);

        if (player.isUnderWater()) {
            // Water slows bullets down (see BulletEntity), so an instant tracer would be wrong here.
            // The bullet's own bubble trail shows how far it gets; the muzzle just puffs bubbles.
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.BUBBLE, muzzle.x, muzzle.y, muzzle.z, look.x * 0.2, look.y * 0.2, look.z * 0.2);
            }
        } else if (stats.guided()) {
            // A rocket leaves a back blast instead of a tracer; the rocket draws its own trail.
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, eye.x - look.x * 0.8, eye.y - look.y * 0.8 - 0.1, eye.z - look.z * 0.8,
                        -look.x * 0.15 + (player.getRandom().nextDouble() - 0.5) * 0.1, 0.02, -look.z * 0.15 + (player.getRandom().nextDouble() - 0.5) * 0.1);
            }
            level.addParticle(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, look.x * 0.1, look.y * 0.1, look.z * 0.1);
        } else {
            level.addParticle(ParticleTypes.SMALL_FLAME, muzzle.x, muzzle.y, muzzle.z, 0.0, 0.0, 0.0);
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, look.x * 0.05, look.y * 0.05 + 0.01, look.z * 0.05);
            }
            // Tracer from the muzzle to whatever block the crosshair is on.
            HitResult hit = player.pick(TRACER_RANGE, 1.0F, false);
            Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(TRACER_RANGE)) : hit.getLocation();
            Vec3 path = end.subtract(muzzle);
            double length = path.length();
            for (double distance = 1.0; distance < length; distance += TRACER_STEP) {
                Vec3 point = muzzle.add(path.scale(distance / length));
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 0.0, 0.0, 0.0);
            }
        }

        // Recoil: a camera kick up and a little sideways (DESIGN.md §4.2). Zoomed in, it's scaled down so
        // it moves the scope picture as much as an unzoomed kick moves the normal view.
        float kick = stats.recoilDegrees() / WeaponAim.zoom();
        player.setXRot(Mth.clamp(player.getXRot() - kick, -90.0F, 90.0F));
        player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * kick * 0.5F);

        WeaponAim.onShot();
        GunshotSounds.play(stats.fireSound().value(), player.position(), stats.soundRange());
    }

    private WeaponClientEffects() {
    }
}
