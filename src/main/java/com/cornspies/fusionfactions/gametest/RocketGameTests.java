package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.RocketConfig;
import com.cornspies.fusionfactions.gear.HandheldRadarItem;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.GuidedRocketEntity;
import com.cornspies.fusionfactions.weapon.LockOn;
import com.cornspies.fusionfactions.weapon.LockWarning;
import com.cornspies.fusionfactions.weapon.Trigger;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.Weapons;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.workbench.WeaponWorkbenchMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

// Rocket launcher, lock-on and lock warning gear (DESIGN.md §6b.6, §6b.7). Registered by WeaponGameTests.
public final class RocketGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    static final List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> TESTS = List.of(
            TEST_FUNCTIONS.register("rocket_hurts_entities_not_blocks", () -> RocketGameTests::rocketHurtsEntitiesNotBlocks),
            TEST_FUNCTIONS.register("rocket_homes_on_target", () -> RocketGameTests::rocketHomesOnTarget),
            TEST_FUNCTIONS.register("lock_on_needs_aim_range_and_sight", () -> RocketGameTests::lockOnNeedsAimRangeAndSight),
            TEST_FUNCTIONS.register("lock_warning_needs_gear", () -> RocketGameTests::lockWarningNeedsGear),
            TEST_FUNCTIONS.register("implant_installs_in_helmet", () -> RocketGameTests::implantInstallsInHelmet),
            TEST_FUNCTIONS.register("handheld_radar_charges_and_drains", () -> RocketGameTests::handheldRadarChargesAndDrains));

    // A shot at a wall 8 blocks away: the wall stays, a pig next to the impact is hurt, the shooter isn't. One rocket
    // is used from the magazine.
    private static void rocketHurtsEntitiesNotBlocks(GameTestHelper helper) {
        buildRange(helper, 8);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 6));
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        shooter.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ROCKET_LAUNCHER.get()));
        Trigger.press(shooter);
        Trigger.tick(shooter);
        helper.assertValueEqual(WeaponItem.getAmmo(shooter.getMainHandItem()), 0, "rockets left in the launcher");
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the pig next to the impact should be hurt");
            for (int y = 1; y <= 3; y++) {
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, y, 8));
            }
            helper.assertTrue(shooter.getHealth() >= shooter.getMaxHealth(), "the shooter, far from the blast, should be unhurt");
        });
    }

    // A rocket fired straight ahead with a target 12 blocks out and 5 to the side turns towards it and hits it.
    private static void rocketHomesOnTarget(GameTestHelper helper) {
        buildRange(helper, 20);
        Pig target = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 1, 12));
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        shooter.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(new GuidedRocketEntity(helper.getLevel(), shooter, Weapons.ROCKET_LAUNCHER, target));
        helper.succeedWhen(() -> helper.assertTrue(target.getHealth() < target.getMaxHealth() || !target.isAlive(), "the rocket should home in on the pig"));
    }

    // A player counts as aimed at only near the crosshair, within the lock range and not behind a wall.
    private static void lockOnNeedsAimRangeAndSight(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Player target = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        shooter.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
        double range = RocketConfig.lockRange();

        target.snapTo(feet.x, feet.y, feet.z + 10.0, 0.0F, 0.0F);
        helper.assertTrue(LockOn.isAimedAt(shooter, target, range), "a player straight ahead should count as aimed at");
        target.snapTo(feet.x + 6.0, feet.y, feet.z + 10.0, 0.0F, 0.0F);
        helper.assertTrue(!LockOn.isAimedAt(shooter, target, range), "a player 30 degrees to the side should not count");
        target.snapTo(feet.x, feet.y, feet.z + range + 4.0, 0.0F, 0.0F);
        helper.assertTrue(!LockOn.isAimedAt(shooter, target, range), "a player out of range should not count");

        target.snapTo(feet.x, feet.y, feet.z + 10.0, 0.0F, 0.0F);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                helper.setBlock(x, y, 5, Blocks.STONE);
            }
        }
        helper.assertTrue(!LockOn.isAimedAt(shooter, target, range), "a player behind a wall should not count");
        helper.succeed();
    }

    // Only a helmet with an implant or a charged handheld radar in a hand warns of lock-ons.
    private static void lockWarningNeedsGear(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.assertTrue(!LockWarning.hasGear(player), "no gear, no warning");

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.TACTICAL_HELMET.get()));
        helper.assertTrue(!LockWarning.hasGear(player), "a helmet without an implant doesn't warn");
        ItemStack helmet = new ItemStack(ModItems.TACTICAL_HELMET.get());
        helmet.set(ModDataComponents.IMPLANT.get(), Unit.INSTANCE);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        helper.assertTrue(LockWarning.hasGear(player), "a helmet with an implant warns");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.HANDHELD_RADAR.get()));
        helper.assertTrue(!LockWarning.hasGear(player), "an empty radar doesn't warn");
        ItemStack radar = new ItemStack(ModItems.HANDHELD_RADAR.get());
        radar.set(ModDataComponents.ENERGY.get(), 1000);
        player.setItemInHand(InteractionHand.OFF_HAND, radar);
        helper.assertTrue(LockWarning.hasGear(player), "a charged radar in the off hand warns");
        helper.succeed();
    }

    // In the workbench, a helmet takes an implant in its one field and gives it back; weapons' fields stay closed.
    private static void implantInstallsInHelmet(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        WeaponWorkbenchMenu menu = new WeaponWorkbenchMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        int implantField = 1 + UpgradeType.values().length;
        int hotbar = implantField + 1 + 27;
        player.getInventory().setItem(0, new ItemStack(ModItems.THREAT_WARNING_IMPLANT.get()));
        menu.getSlot(0).set(new ItemStack(ModItems.TACTICAL_HELMET.get()));
        helper.assertTrue(menu.hasHelmet(), "the helmet should be in the workbench");

        menu.quickMoveStack(player, hotbar);
        helper.assertTrue(menu.weapon().has(ModDataComponents.IMPLANT.get()), "the implant should be installed");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the implant should have left the inventory");

        menu.quickMoveStack(player, implantField);
        helper.assertTrue(!menu.weapon().has(ModDataComponents.IMPLANT.get()), "the implant should be taken out");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.THREAT_WARNING_IMPLANT.get()), 1, "implants back in the inventory");
        helper.succeed();
    }

    // A charger fills the radar through the energy capability; held, it uses its configured drain once a second.
    private static void handheldRadarChargesAndDrains(GameTestHelper helper) {
        ItemStack radar = new ItemStack(ModItems.HANDHELD_RADAR.get());
        EnergyHandler energy = ItemAccess.forStack(radar).getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(energy != null, "the radar should have an energy handler");
        MissileGameTests.charge(energy, HandheldRadarItem.CAPACITY);
        helper.assertValueEqual(HandheldRadarItem.energy(radar), HandheldRadarItem.CAPACITY, "energy after charging");

        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, radar);
        for (int tick = 1; tick <= 20; tick++) {
            helper.runAfterDelay(tick, () -> radar.inventoryTick(helper.getLevel(), player, EquipmentSlot.MAINHAND));
        }
        helper.runAfterDelay(21, () -> {
            helper.assertValueEqual(HandheldRadarItem.energy(radar), HandheldRadarItem.CAPACITY - RocketConfig.radarDrain() * 20, "energy after a second held");
            helper.succeed();
        });
    }

    // A stone floor and a wall `length` blocks out, force-loaded, so rockets explode inside the test.
    private static void buildRange(GameTestHelper helper, int length) {
        for (int x = -2; x <= 6; x++) {
            for (int y = 0; y <= 5; y++) {
                helper.setBlock(x, y, length, Blocks.STONE);
            }
            for (int z = 0; z < length; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        // Every chunk the range touches, also sideways: rockets and pigs stop in chunks that don't tick.
        for (int x = -2; x <= 6; x += 4) {
            for (int z = 0; z <= length; z += 4) {
                BlockPos pos = helper.absolutePos(new BlockPos(x, 0, z));
                helper.getLevel().setChunkForced(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), true);
            }
        }
    }

    private RocketGameTests() {
    }
}
