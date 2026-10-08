package com.cornspies.fusionfactions.weapon;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

// A gun (DESIGN.md §4.2). Firing uses the vanilla item-use flow: the first shot when the use starts,
// then, for automatic weapons, one per fire interval while the button is held. Everything that
// matters runs on the server; the client only adds cosmetic effects (see client.WeaponClientEffects).
public class WeaponItem extends Item {
    private static final int BAR_COLOR = 0x55FFFF;
    private static final int BAR_COLOR_RELOADING = 0xFFAA00;

    private final WeaponStats stats;

    public WeaponItem(WeaponStats stats, Item.Properties properties) {
        super(properties);
        this.stats = stats;
    }

    public WeaponStats stats() {
        return this.stats;
    }

    public static int getAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO.get(), 0);
    }

    public static boolean isReloading(ItemStack stack) {
        return stack.has(ModDataComponents.RELOAD.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        if (level instanceof ServerLevel serverLevel) {
            this.tryFire(serverLevel, player, player.getItemInHand(hand), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (this.stats.automatic() && level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            this.tryFire(serverLevel, player, stack, false);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    // newPress: the trigger was just pulled, as opposed to being held down on an automatic weapon.
    private void tryFire(ServerLevel level, Player player, ItemStack stack, boolean newPress) {
        long now = level.getGameTime();
        Long nextShot = stack.get(ModDataComponents.NEXT_SHOT.get());
        // A value further away than one interval is stale (e.g. from another world) and is ignored.
        if (nextShot != null && nextShot > now && nextShot - now <= this.stats.fireIntervalTicks()) {
            return;
        }
        // Holding the trigger doesn't interrupt a reload; pulling it again does.
        if (isReloading(stack) && !newPress) {
            return;
        }
        stack.set(ModDataComponents.NEXT_SHOT.get(), now + this.stats.fireIntervalTicks());

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WEAPON_EMPTY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                tryStartReload(serverPlayer);
            }
            return;
        }

        // Firing interrupts a reload. The rounds loaded so far stay in the magazine.
        stack.remove(ModDataComponents.RELOAD.get());
        stack.set(ModDataComponents.AMMO.get(), ammo - 1);
        for (int i = 0; i < this.stats.pellets(); i++) {
            this.spawnBullet(level, player);
        }
        // Everyone but the shooter hears this. The shooter's client plays it right away (D22).
        level.playSound(player, player.getX(), player.getY(), player.getZ(), this.stats.fireSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void spawnBullet(ServerLevel level, Player player) {
        RandomSource random = player.getRandom();
        float pitch = player.getXRot() + (random.nextFloat() * 2.0F - 1.0F) * this.stats.spreadDegrees();
        float yaw = player.getYRot() + (random.nextFloat() * 2.0F - 1.0F) * this.stats.spreadDegrees();
        BulletEntity bullet = new BulletEntity(level, player, this.stats);
        bullet.shootFromRotation(player, pitch, yaw, 0.0F, this.stats.bulletSpeed(), 0.0F);
        level.addFreshEntity(bullet);
    }

    // Reload key (ReloadPayload). The client only says "reload"; everything is checked here.
    public static void tryStartReload(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponItem weapon) || isReloading(stack)) {
            return;
        }
        WeaponStats stats = weapon.stats;
        if (getAmmo(stack) >= stats.magazineSize()) {
            return;
        }
        if (!player.hasInfiniteMaterials() && countAmmo(player) < stats.ammoPerRound()) {
            player.sendOverlayMessage(Component.translatable("fusionfactions.weapon.no_ammo"));
            return;
        }
        stack.set(ModDataComponents.RELOAD.get(), player.level().getGameTime() + stats.reloadTicksPerRound());
    }

    // Loads one round per interval while the weapon stays selected. Partial reloads are kept.
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        Long nextRound = stack.get(ModDataComponents.RELOAD.get());
        if (nextRound == null) {
            return;
        }
        if (slot != EquipmentSlot.MAINHAND || !(owner instanceof Player player)) {
            // Switching to another item cancels the reload.
            stack.remove(ModDataComponents.RELOAD.get());
            return;
        }
        long now = level.getGameTime();
        if (now < nextRound && nextRound - now <= this.stats.reloadTicksPerRound()) {
            return;
        }
        int ammo = getAmmo(stack);
        if (ammo >= this.stats.magazineSize() || (!player.hasInfiniteMaterials() && !consumeAmmo(player, this.stats.ammoPerRound()))) {
            stack.remove(ModDataComponents.RELOAD.get());
            return;
        }
        stack.set(ModDataComponents.AMMO.get(), ammo + 1);
        if (ammo + 1 >= this.stats.magazineSize()) {
            stack.remove(ModDataComponents.RELOAD.get());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WEAPON_RELOAD_DONE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else {
            stack.set(ModDataComponents.RELOAD.get(), now + this.stats.reloadTicksPerRound());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WEAPON_RELOAD.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    public static int countAmmo(Player player) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.AMMO.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean consumeAmmo(Player player, int amount) {
        if (countAmmo(player) < amount) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && amount > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.AMMO.get())) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
        return true;
    }

    // The durability bar shows how full the magazine is.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Math.min(getAmmo(stack), this.stats.magazineSize()) / this.stats.magazineSize());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isReloading(stack) ? BAR_COLOR_RELOADING : BAR_COLOR;
    }
}
