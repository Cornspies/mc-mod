package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.WeaponStats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Cosmetic shot feedback on the shooter's own client (D22): muzzle flash, tracer, recoil and the
// fire sound, shown right away instead of after a server round trip. It predicts the server's
// timing from the same stats; the server alone decides what is actually fired and hit.
public final class WeaponClientEffects {
    private static final double TRACER_RANGE = 48.0;
    // Bullets slow down quickly in water (see BulletEntity), so the tracer is short there too.
    private static final double TRACER_RANGE_UNDERWATER = 9.0;
    private static final double TRACER_STEP = 0.8;

    private static int cooldown;
    private static boolean wasUsing;

    public static void onClientTick(ClientTickEvent.Post event) {
        if (cooldown > 0) {
            cooldown--;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND
                || !(player.getUseItem().getItem() instanceof WeaponItem weapon)) {
            wasUsing = false;
            return;
        }
        ItemStack stack = player.getUseItem();
        WeaponStats stats = weapon.stats();
        boolean newPress = !wasUsing;
        wasUsing = true;
        // Same rules as WeaponItem.tryFire: holding only repeats on automatic weapons and never interrupts a reload.
        if (!newPress && (!stats.automatic() || WeaponItem.isReloading(stack))) {
            return;
        }
        if (cooldown > 0 || WeaponItem.getAmmo(stack) <= 0) {
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

        level.addParticle(ParticleTypes.SMALL_FLAME, muzzle.x, muzzle.y, muzzle.z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, look.x * 0.05, look.y * 0.05 + 0.01, look.z * 0.05);
        }

        // Tracer from the muzzle to whatever block the crosshair is on.
        double range = player.isUnderWater() ? TRACER_RANGE_UNDERWATER : TRACER_RANGE;
        HitResult hit = player.pick(range, 1.0F, false);
        Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(range)) : hit.getLocation();
        Vec3 path = end.subtract(muzzle);
        double length = path.length();
        for (double distance = 1.0; distance < length; distance += TRACER_STEP) {
            Vec3 point = muzzle.add(path.scale(distance / length));
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 0.0, 0.0, 0.0);
        }

        // Recoil: a camera kick up and a little sideways (DESIGN.md §4.2).
        float kick = stats.recoilDegrees();
        player.setXRot(Mth.clamp(player.getXRot() - kick, -90.0F, 90.0F));
        player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * kick * 0.5F);

        level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.WEAPON_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
    }

    private WeaponClientEffects() {
    }
}
