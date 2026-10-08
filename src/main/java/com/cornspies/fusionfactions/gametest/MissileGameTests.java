package com.cornspies.fusionfactions.gametest;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.missile.LaunchStatus;
import com.cornspies.fusionfactions.missile.MissileData;
import com.cornspies.fusionfactions.missile.MissileFlight;
import com.cornspies.fusionfactions.missile.MissileLauncherBlockEntity;
import com.cornspies.fusionfactions.missile.MissileManager;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.missile.RaidWindows;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Missiles (DESIGN.md §6): a whole flight onto a small target, the launcher's checks, raid windows.
// Registered by WeaponGameTests, with more time: a flight takes at least 7 seconds.
public final class MissileGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSILE_HITS_TARGET =
            TEST_FUNCTIONS.register("missile_hits_target", () -> MissileGameTests::missileHitsTarget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAUNCHER_CHECKS =
            TEST_FUNCTIONS.register("launcher_checks", () -> MissileGameTests::launcherChecks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RAID_WINDOWS =
            TEST_FUNCTIONS.register("raid_windows", () -> MissileGameTests::raidWindows);

    private static final int MAX_TICKS = 400;
    // Keeps other tests out of the blast.
    private static final int PADDING = 48;

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        for (var function : List.of(MISSILE_HITS_TARGET, LAUNCHER_CHECKS, RAID_WINDOWS)) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"),
                    MAX_TICKS, 0, true, Rotation.NONE, false, 1, 1, false, PADDING);
            event.registerTest(function.getId(), new FunctionGameTestInstance(function.key(), data));
        }
    }

    // An explosive missile I, launched from the test's position, comes down on a stone platform next to it (not
    // above the test's own block, which the framework keeps). The blast breaks stone and a chest without
    // dropping them, the chest spills its contents, obsidian and bedrock survive, and a pig on the platform is hurt.
    private static void missileHitsTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = new BlockPos(6, -2, 6);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -6; y <= -3; y++) {
                    helper.setBlock(center.offset(x, y + 2, z), Blocks.STONE);
                }
            }
        }
        BlockPos chest = center.offset(2, 0, 0);
        BlockPos obsidian = center.offset(-2, 0, 0);
        BlockPos bedrock = center.offset(0, 0, 2);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(obsidian, Blocks.OBSIDIAN);
        helper.setBlock(bedrock, Blocks.BEDROCK);
        if (level.getBlockEntity(helper.absolutePos(chest)) instanceof ChestBlockEntity box) {
            box.setItem(0, new ItemStack(Items.DIAMOND, 5));
        }
        Pig pig = helper.spawn(EntityTypes.PIG, center.offset(0, 1, -2));
        pig.setNoAi(true);

        BlockPos target = helper.absolutePos(center.below());
        MissileFlight flight = MissileManager.launch(level, MissileType.EXPLOSIVE, 1, helper.absolutePos(BlockPos.ZERO), target, null);
        AABB area = new AABB(helper.absoluteVec(new Vec3(-2, -10, -2)), helper.absoluteVec(new Vec3(15, 6, 15)));

        helper.succeedWhen(() -> {
            helper.assertTrue(MissileData.get(level.getServer()).find(flight.id()) == null, "the missile should have hit");
            helper.assertBlockNotPresent(Blocks.STONE, center.below());
            helper.assertBlockNotPresent(Blocks.CHEST, chest);
            helper.assertBlockPresent(Blocks.OBSIDIAN, obsidian);
            helper.assertBlockPresent(Blocks.BEDROCK, bedrock);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area);
            helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.DIAMOND)), "the chest should spill its diamonds");
            helper.assertTrue(drops.stream().noneMatch(item -> item.getItem().is(Items.STONE) || item.getItem().is(Items.COBBLESTONE)
                    || item.getItem().is(Items.CHEST)), "broken blocks should drop nothing, not " + drops.stream().map(ItemEntity::getItem).toList());
            helper.assertTrue(pig.isDeadOrDying() || pig.getHealth() < pig.getMaxHealth(), "the pig should be hurt");
        });
    }

    // The launcher refuses until it has a missile and a target in range, fires once, then cools down.
    // The launched missile is cancelled straight away, so it doesn't land anywhere.
    private static void launcherChecks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.MISSILE_LAUNCHER.get());
        MissileLauncherBlockEntity launcher = (MissileLauncherBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(launcher != null, "the launcher should have a block entity");
        assertStatus(helper, launcher, LaunchStatus.Check.NO_MISSILE);

        launcher.setItem(MissileLauncherBlockEntity.MISSILE_SLOT, new ItemStack(ModItems.missile(MissileType.EXPLOSIVE, 1).get(), 2));
        assertStatus(helper, launcher, LaunchStatus.Check.NO_TARGET);

        ItemStack designator = new ItemStack(ModItems.TARGET_DESIGNATOR.get());
        launcher.setItem(MissileLauncherBlockEntity.DESIGNATOR_SLOT, designator);
        BlockPos origin = helper.absolutePos(pos);
        aim(launcher, level, origin.offset(10, 0, 0));
        assertStatus(helper, launcher, LaunchStatus.Check.TOO_CLOSE);
        aim(launcher, level, origin.offset(MissileConfig.range(1) + 10, 0, 0));
        assertStatus(helper, launcher, LaunchStatus.Check.TOO_FAR);
        aim(launcher, level, origin.offset(200, 0, 0));
        launcher.setItem(MissileLauncherBlockEntity.MISSILE_SLOT, new ItemStack(ModItems.missile(MissileType.EXPLOSIVE, 3).get()));
        assertStatus(helper, launcher, LaunchStatus.Check.NEEDS_SILO);
        launcher.setItem(MissileLauncherBlockEntity.MISSILE_SLOT, new ItemStack(ModItems.missile(MissileType.EXPLOSIVE, 1).get(), 2));

        if (RaidWindows.isOpen(RaidWindows.configured(), RaidWindows.now()) && !MissileConfig.requirePower()) {
            assertStatus(helper, launcher, LaunchStatus.Check.READY);
            int before = MissileData.get(level.getServer()).flights().size();
            helper.assertTrue(launcher.launch(level, null).isReady(), "the launch should go through");
            List<MissileFlight> flights = MissileData.get(level.getServer()).flights();
            helper.assertValueEqual(flights.size(), before + 1, "missiles in flight");
            MissileFlight flight = flights.getLast();
            MissileManager.cancel(level.getServer(), flight.id());
            helper.assertValueEqual(launcher.getItem(MissileLauncherBlockEntity.MISSILE_SLOT).getCount(), 1, "missiles left in the launcher");
            assertStatus(helper, launcher, LaunchStatus.Check.COOLDOWN);
        }

        helper.setBlock(pos.above(), Blocks.STONE);
        LaunchStatus blocked = launcher.status(level);
        helper.assertTrue(blocked.check() == LaunchStatus.Check.SKY_BLOCKED || blocked.check() == LaunchStatus.Check.COOLDOWN,
                "a roof should block the launch, not " + blocked);
        helper.succeed();
    }

    private static void aim(MissileLauncherBlockEntity launcher, ServerLevel level, BlockPos target) {
        launcher.getItem(MissileLauncherBlockEntity.DESIGNATOR_SLOT).set(ModDataComponents.TARGET.get(), GlobalPos.of(level.dimension(), target));
    }

    private static void assertStatus(GameTestHelper helper, MissileLauncherBlockEntity launcher, LaunchStatus.Check expected) {
        helper.assertValueEqual(launcher.status(helper.getLevel()).check(), expected, "launcher status");
    }

    // Parsing and matching of raid window entries (D46), including windows that run past midnight.
    private static void raidWindows(GameTestHelper helper) {
        ZonedDateTime saturdayEvening = LocalDateTime.of(2026, 10, 10, 19, 30).atZone(ZoneOffset.UTC);
        helper.assertValueEqual(saturdayEvening.getDayOfWeek(), DayOfWeek.SATURDAY, "test date");
        List<RaidWindows.Window> saturday = List.of(RaidWindows.parse("SAT 18:00-23:00"));
        helper.assertTrue(RaidWindows.isOpen(saturday, saturdayEvening), "Saturday 19:30 is in SAT 18:00-23:00");
        helper.assertFalse(RaidWindows.isOpen(saturday, saturdayEvening.plusHours(4)), "23:30 is after the window");
        helper.assertFalse(RaidWindows.isOpen(saturday, saturdayEvening.plusDays(1)), "Sunday is not in the window");
        helper.assertValueEqual(RaidWindows.untilOpen(saturday, saturdayEvening.minusHours(3)), Optional.of(Duration.ofMinutes(90)),
                "time until the window opens at 16:30");

        List<RaidWindows.Window> overnight = List.of(RaidWindows.parse("fri 22:00-02:00"));
        helper.assertTrue(RaidWindows.isOpen(overnight, saturdayEvening.withHour(1)), "Saturday 01:30 is in FRI 22:00-02:00");
        helper.assertFalse(RaidWindows.isOpen(overnight, saturdayEvening.withHour(3)), "Saturday 03:30 is not");

        List<RaidWindows.Window> weekdays = List.of(RaidWindows.parse("MON-FRI 20:00-22:00"));
        helper.assertFalse(RaidWindows.isOpen(weekdays, saturdayEvening.withHour(21)), "Saturday is not a weekday");
        helper.assertTrue(RaidWindows.isOpen(weekdays, saturdayEvening.minusDays(1).withHour(21)), "Friday 21:30 is");

        helper.assertTrue(RaidWindows.isOpen(List.of(), saturdayEvening), "no windows means always open");
        helper.assertTrue(RaidWindows.parse("SAT 18-23") == null, "times need minutes");
        helper.assertTrue(RaidWindows.parse("XYZ 18:00-23:00") == null, "unknown day");
        helper.assertTrue(RaidWindows.parse("SAT,SUN 14:00-02:00") != null, "day lists");
        helper.succeed();
    }

    private MissileGameTests() {
    }
}
