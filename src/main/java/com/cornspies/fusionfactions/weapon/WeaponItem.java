package com.cornspies.fusionfactions.weapon;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeRules;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

// A gun (DESIGN.md §4.2). The attack button is the trigger (D37, see Trigger): the first shot when it's
// pulled, then, for automatic weapons, one per fire interval while it's held. The use button aims down
// sights (D38), through vanilla's item-use flow, so the server knows who is aiming. Everything that
// matters runs on the server; the client only adds cosmetic effects (see client.WeaponClientEffects).
public class WeaponItem extends Item {
    private static final int BAR_COLOR = 0x55FFFF;
    private static final int BAR_COLOR_RELOADING = 0xFFAA00;

    private final WeaponStats baseStats;

    public WeaponItem(WeaponStats baseStats, Item.Properties properties) {
        super(properties);
        this.baseStats = baseStats;
    }

    // The weapon's own stats, without upgrades (see Weapons).
    public WeaponStats baseStats() {
        return this.baseStats;
    }

    // The stats of this weapon stack, with its installed upgrade parts (DESIGN.md §4.5).
    public WeaponStats stats(ItemStack stack) {
        return UpgradeRules.apply(this, this.baseStats, WeaponUpgrades.of(stack));
    }

    public static int getAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO.get(), 0);
    }

    public static boolean isReloading(ItemStack stack) {
        return stack.has(ModDataComponents.RELOAD.get());
    }

    // Aiming down sights: held as long as the use button is (D38).
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    // Whether the player aims with this weapon in the main hand. Compares the item, not the stack object:
    // every shot or loaded round replaces the client's copy of the stack, and vanilla keeps the item in
    // use when the item type stays the same.
    public static boolean isAiming(Player player, ItemStack stack) {
        return player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND && player.getMainHandItem().is(stack.getItem());
    }

    // Maximum deviation of a pellet in degrees: less when aiming down sights (D39).
    public static float spread(Player player, ItemStack stack, WeaponStats stats) {
        return stats.spreadDegrees() * (isAiming(player, stack) ? stats.aimSpreadFactor() : 1.0F);
    }

    // Called by Trigger every tick while the trigger is held. newPress: it was just pulled.
    public void triggerTick(ServerLevel level, Player player, ItemStack stack, boolean newPress, int heldTicks) {
        if (newPress || this.baseStats.automatic()) {
            this.tryFire(level, player, stack, newPress, heldTicks);
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
    private void tryFire(ServerLevel level, Player player, ItemStack stack, boolean newPress, int heldTicks) {
        WeaponStats stats = this.stats(stack);
        long now = level.getGameTime();
        Long nextShot = stack.get(ModDataComponents.NEXT_SHOT.get());
        // A value further away than one interval is stale (e.g. from another world) and is ignored.
        if (nextShot != null && nextShot > now && nextShot - now <= stats.fireIntervalTicks()) {
            return;
        }
        // Holding the trigger doesn't interrupt a reload; pulling it again does.
        if (isReloading(stack) && !newPress) {
            return;
        }
        if (stats.spinUpTicks() > 0 && heldTicks < stats.spinUpTicks()) {
            // Still spinning up. Pulling the trigger already stops a reload, so holding it doesn't resume one.
            stack.remove(ModDataComponents.RELOAD.get());
            return;
        }
        stack.set(ModDataComponents.NEXT_SHOT.get(), now + stats.fireIntervalTicks());

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
        this.shoot(level, player, stack, stats);
        // Everyone in hearing range but the shooter hears this. The shooter's client plays it right away (D22).
        GunshotPayload.broadcast(level, player instanceof ServerPlayer serverPlayer ? serverPlayer : null, player.position(), stats);
    }

    // One shot, after the round has been taken from the magazine: the bullets (pellets) with spread.
    protected void shoot(ServerLevel level, Player player, ItemStack stack, WeaponStats stats) {
        float spread = spread(player, stack, stats);
        for (int i = 0; i < stats.pellets(); i++) {
            spawnBullet(level, player, stats, spread);
        }
    }

    private static void spawnBullet(ServerLevel level, Player player, WeaponStats stats, float spread) {
        RandomSource random = player.getRandom();
        float pitch = player.getXRot() + (random.nextFloat() * 2.0F - 1.0F) * spread;
        float yaw = player.getYRot() + (random.nextFloat() * 2.0F - 1.0F) * spread;
        BulletEntity bullet = new BulletEntity(level, player, stats);
        bullet.shootFromRotation(player, pitch, yaw, 0.0F, stats.bulletSpeed(), 0.0F);
        level.addFreshEntity(bullet);
    }

    // Reload key (ReloadPayload). The client only says "reload"; everything is checked here.
    public static void tryStartReload(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponItem weapon) || isReloading(stack)) {
            return;
        }
        WeaponStats stats = weapon.stats(stack);
        if (getAmmo(stack) >= stats.magazineSize()) {
            return;
        }
        if (!player.hasInfiniteMaterials() && weapon.countAmmo(player) < stats.ammoPerRound()) {
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
        WeaponStats stats = this.stats(stack);
        long now = level.getGameTime();
        if (now < nextRound && nextRound - now <= stats.reloadTicksPerRound()) {
            return;
        }
        int ammo = getAmmo(stack);
        if (ammo >= stats.magazineSize() || (!player.hasInfiniteMaterials() && !this.consumeAmmo(player, stats.ammoPerRound()))) {
            stack.remove(ModDataComponents.RELOAD.get());
            return;
        }
        stack.set(ModDataComponents.AMMO.get(), ammo + 1);
        if (ammo + 1 >= stats.magazineSize()) {
            stack.remove(ModDataComponents.RELOAD.get());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WEAPON_RELOAD_DONE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else {
            stack.set(ModDataComponents.RELOAD.get(), now + stats.reloadTicksPerRound());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WEAPON_RELOAD.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    // The item this weapon loads: the shared ammo (D20); the rocket launcher has its own (D65).
    public Item ammoItem() {
        return ModItems.AMMO.get();
    }

    public int countAmmo(Player player) {
        Item ammoItem = this.ammoItem();
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ammoItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean consumeAmmo(Player player, int amount) {
        if (this.countAmmo(player) < amount) {
            return false;
        }
        Item ammoItem = this.ammoItem();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && amount > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ammoItem)) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
        return true;
    }

    // Lists the installed upgrade parts (DESIGN.md §4.5).
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        WeaponUpgrades upgrades = WeaponUpgrades.of(stack);
        for (UpgradeType type : UpgradeType.values()) {
            int tier = upgrades.tier(type);
            if (tier == 0) {
                continue;
            }
            Component name = Component.translatable(type.translationKey()).append(" ").append(UpgradeItem.tierName(tier));
            if (UpgradeRules.accepts(this, type)) {
                builder.accept(name.copy().withStyle(UpgradeItem.tierColor(tier))
                        .append(Component.literal(": ").append(UpgradeRules.describe(this.baseStats, type, tier)).withStyle(ChatFormatting.GRAY)));
            } else {
                builder.accept(Component.translatable("tooltip.fusionfactions.weapon.upgrade_inactive", name).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    // The durability bar shows how full the magazine is.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int magazine = this.stats(stack).magazineSize();
        return Math.round(13.0F * Math.min(getAmmo(stack), magazine) / magazine);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isReloading(stack) ? BAR_COLOR_RELOADING : BAR_COLOR;
    }
}
