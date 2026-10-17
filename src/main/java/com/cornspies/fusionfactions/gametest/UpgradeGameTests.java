package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.UpgradeConfig;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.WeaponStats;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;
import com.cornspies.fusionfactions.workbench.WeaponWorkbenchMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Weapon upgrade checks (DESIGN.md §4.5). Registered by WeaponGameTests. Expected values are computed
// from the config, so the tests still hold when a server changes the numbers.
public final class UpgradeGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    static final List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> TESTS = List.of(
            TEST_FUNCTIONS.register("upgrades_change_stats", () -> UpgradeGameTests::upgradesChangeStats),
            TEST_FUNCTIONS.register("workbench_installs_and_returns_parts", () -> UpgradeGameTests::workbenchInstallsAndReturnsParts),
            TEST_FUNCTIONS.register("workbench_returns_weapon_on_close", () -> UpgradeGameTests::workbenchReturnsWeaponOnClose));

    // Index of the first hotbar slot in the workbench menu: weapon, fields, the implant field, 27 inventory slots.
    private static final int IMPLANT_FIELD = 1 + UpgradeType.values().length;
    private static final int HOTBAR = IMPLANT_FIELD + 1 + 27;

    // Each part changes its stat by the configured amount; a part the weapon doesn't accept does nothing.
    private static void upgradesChangeStats(GameTestHelper helper) {
        WeaponItem pistol = ModItems.PISTOL.get();
        ItemStack stack = new ItemStack(pistol);
        WeaponUpgrades upgrades = WeaponUpgrades.NONE;
        for (UpgradeType type : UpgradeType.values()) {
            upgrades = upgrades.with(type, 3);
        }
        stack.set(ModDataComponents.UPGRADES.get(), upgrades);
        WeaponStats base = pistol.baseStats();
        WeaponStats stats = pistol.stats(stack);

        helper.assertTrue(UpgradeConfig.accepts("pistol", UpgradeType.MAGAZINE), "the test expects the pistol to accept magazines");
        helper.assertTrue(!UpgradeConfig.accepts("pistol", UpgradeType.RECOIL), "the test expects the pistol to have no recoil field");
        int magazine = (int) Math.round(base.magazineSize() * (1.0 + value(UpgradeType.MAGAZINE)));
        helper.assertValueEqual(stats.magazineSize(), magazine, "magazine size");
        helper.assertValueEqual(stats.soundRange(), (float) (base.soundRange() * (1.0 - value(UpgradeType.NOISE))), "hearing range");
        helper.assertValueEqual(stats.armorPiercing(), (float) Math.min(1.0, base.armorPiercing() + value(UpgradeType.PENETRATION)), "armor piercing");
        helper.assertValueEqual(stats.bulletSpeed(), (float) (base.bulletSpeed() * (1.0 + value(UpgradeType.RANGE))), "bullet speed");
        helper.assertValueEqual(stats.spreadDegrees(), (float) (base.spreadDegrees() * (1.0 - value(UpgradeType.ACCURACY))), "spread");
        helper.assertValueEqual(stats.recoilDegrees(), base.recoilDegrees(), "recoil (no field on the pistol)");
        helper.assertValueEqual(stats.damage(), base.damage(), "damage (no damage upgrade)");
        helper.succeed();
    }

    // Shift-clicking installs one part from a stack and takes it out again; parts the weapon has no
    // field for are refused; taking out a bigger magazine drops the rounds that no longer fit.
    private static void workbenchInstallsAndReturnsParts(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        WeaponWorkbenchMenu menu = new WeaponWorkbenchMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        Inventory inventory = player.getInventory();
        Item magazinePart = ModItems.upgrade(UpgradeType.MAGAZINE, 3).get();
        inventory.setItem(0, new ItemStack(magazinePart, 2));
        inventory.setItem(1, new ItemStack(ModItems.upgrade(UpgradeType.RECOIL, 1).get()));
        menu.getSlot(0).set(new ItemStack(ModItems.PISTOL.get()));

        menu.quickMoveStack(player, HOTBAR);
        ItemStack weapon = menu.weapon();
        helper.assertValueEqual(WeaponUpgrades.of(weapon).tier(UpgradeType.MAGAZINE), 3, "magazine tier after installing");
        helper.assertValueEqual(inventory.getItem(0).getCount(), 1, "magazine parts left in the inventory");

        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertValueEqual(WeaponUpgrades.of(weapon).tier(UpgradeType.RECOIL), 0, "recoil tier (the pistol has no recoil field)");
        helper.assertValueEqual(inventory.getItem(1).getCount(), 1, "the recoil part stays in the inventory");

        int bigMagazine = ModItems.PISTOL.get().stats(weapon).magazineSize();
        weapon.set(ModDataComponents.AMMO.get(), bigMagazine);
        menu.quickMoveStack(player, 1 + UpgradeType.MAGAZINE.ordinal());
        helper.assertValueEqual(WeaponUpgrades.of(weapon).tier(UpgradeType.MAGAZINE), 0, "magazine tier after taking it out");
        helper.assertValueEqual(WeaponItem.getAmmo(weapon), ModItems.PISTOL.get().baseStats().magazineSize(), "rounds after taking the magazine out");
        helper.assertValueEqual(inventory.countItem(magazinePart), 2, "magazine parts in the inventory");
        helper.succeed();
    }

    // The workbench stores nothing: closing it gives the weapon back, parts and all. Here the player has
    // left (mock players have no connection to send inventory updates to), so it drops at their feet.
    private static void workbenchReturnsWeaponOnClose(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, ModBlocks.WEAPON_WORKBENCH.get());
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(0.5, 1.0, 0.5)));
        WeaponWorkbenchMenu menu = new WeaponWorkbenchMenu(1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
        menu.getSlot(0).set(new ItemStack(ModItems.SNIPER_RIFLE.get()));
        menu.getSlot(1 + UpgradeType.NOISE.ordinal()).set(new ItemStack(ModItems.upgrade(UpgradeType.NOISE, 2).get()));
        player.discard();
        menu.removed(player);

        helper.assertTrue(menu.weapon().isEmpty(), "the workbench should be empty after closing");
        ItemStack returned = ItemStack.EMPTY;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3.0))) {
            if (item.getItem().is(ModItems.SNIPER_RIFLE.get())) {
                returned = item.getItem();
            }
        }
        helper.assertTrue(!returned.isEmpty(), "the sniper rifle should be dropped for the player");
        helper.assertValueEqual(WeaponUpgrades.of(returned).tier(UpgradeType.NOISE), 2, "the suppressor should still be on the rifle");
        helper.succeed();
    }

    private static double value(UpgradeType type) {
        return UpgradeConfig.value(type, 3) / 100.0;
    }

    private UpgradeGameTests() {
    }
}
