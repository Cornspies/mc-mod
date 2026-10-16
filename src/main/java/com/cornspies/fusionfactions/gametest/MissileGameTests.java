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
import com.cornspies.fusionfactions.missile.InterceptorBlockEntity;
import com.cornspies.fusionfactions.missile.LaunchStatus;
import com.cornspies.fusionfactions.missile.MissileData;
import com.cornspies.fusionfactions.missile.MissileFlight;
import com.cornspies.fusionfactions.missile.MissileLauncherBlockEntity;
import com.cornspies.fusionfactions.missile.MissileManager;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.missile.MissileEntity;
import com.cornspies.fusionfactions.missile.RadarBlock;
import com.cornspies.fusionfactions.missile.RadarBlockEntity;
import com.cornspies.fusionfactions.missile.RaidWindows;
import com.cornspies.fusionfactions.missile.SiloControllerBlockEntity;
import com.cornspies.fusionfactions.missile.SiloHatchBlock;
import com.cornspies.fusionfactions.missile.SiloStructure;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.ModDamageTypes;

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
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// Missiles (DESIGN.md §6): whole flights onto small targets for each missile type, the launcher's checks, the
// silo structure, raid windows.
// Registered by WeaponGameTests, with more time: a flight takes at least 7 seconds.
public final class MissileGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSILE_HITS_TARGET =
            TEST_FUNCTIONS.register("missile_hits_target", () -> MissileGameTests::missileHitsTarget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAUNCHER_CHECKS =
            TEST_FUNCTIONS.register("launcher_checks", () -> MissileGameTests::launcherChecks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RAID_WINDOWS =
            TEST_FUNCTIONS.register("raid_windows", () -> MissileGameTests::raidWindows);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BREACHING_BREAKS_OBSIDIAN =
            TEST_FUNCTIONS.register("breaching_breaks_obsidian", () -> MissileGameTests::breachingBreaksObsidian);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EMP_SHUTS_DOWN_LAUNCHER =
            TEST_FUNCTIONS.register("emp_shuts_down_launcher", () -> MissileGameTests::empShutsDownLauncher);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INCENDIARY_SETS_FIRE =
            TEST_FUNCTIONS.register("incendiary_sets_fire", () -> MissileGameTests::incendiarySetsFire);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SILO_STRUCTURE =
            TEST_FUNCTIONS.register("silo_structure", () -> MissileGameTests::siloStructure);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SILO_FORMS =
            TEST_FUNCTIONS.register("silo_forms", () -> MissileGameTests::siloForms);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSILE_SHOT_DOWN =
            TEST_FUNCTIONS.register("missile_shot_down", () -> MissileGameTests::missileShotDown);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTERCEPTOR_STOPS_MISSILE =
            TEST_FUNCTIONS.register("interceptor_stops_missile", () -> MissileGameTests::interceptorStopsMissile);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RADAR_TRACKS_MISSILE =
            TEST_FUNCTIONS.register("radar_tracks_missile", () -> MissileGameTests::radarTracksMissile);

    private static final int MAX_TICKS = 400;
    // An interceptor reaches further than the usual padding; keep other tests' missiles out of its range (128 is the most
    // a test may ask for).
    private static final int INTERCEPTOR_PADDING = 128;
    // Keeps other tests out of the blast.
    private static final int PADDING = 48;

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        for (var function : List.of(MISSILE_HITS_TARGET, LAUNCHER_CHECKS, RAID_WINDOWS, BREACHING_BREAKS_OBSIDIAN, EMP_SHUTS_DOWN_LAUNCHER,
                INCENDIARY_SETS_FIRE, SILO_STRUCTURE, SILO_FORMS, MISSILE_SHOT_DOWN, INTERCEPTOR_STOPS_MISSILE, RADAR_TRACKS_MISSILE)) {
            int padding = function == INTERCEPTOR_STOPS_MISSILE ? INTERCEPTOR_PADDING : PADDING;
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"),
                    MAX_TICKS, 0, true, Rotation.NONE, false, 1, 1, false, padding);
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

        if (MissileConfig.requirePower()) {
            assertStatus(helper, launcher, LaunchStatus.Check.NO_POWER);
            charge(launcher.energy(), MissileConfig.launchEnergy(1));
        }
        if (RaidWindows.isOpen(RaidWindows.configured(), RaidWindows.now())) {
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

    // A stone platform 7x7, 4 deep, with its top at y -3 next to the test's position (not above the test's own
    // block). Returns the top center.
    private static BlockPos platform(GameTestHelper helper, int x, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = -6; y <= -3; y++) {
                    helper.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.STONE);
                }
            }
        }
        return new BlockPos(x, -3, z);
    }

    private static MissileFlight launchAt(GameTestHelper helper, MissileType type, int tier, BlockPos relativeTarget) {
        return MissileManager.launch(helper.getLevel(), type, tier, helper.absolutePos(BlockPos.ZERO), helper.absolutePos(relativeTarget), null);
    }

    private static boolean landed(GameTestHelper helper, MissileFlight flight) {
        return MissileData.get(helper.getLevel().getServer()).find(flight.id()) == null;
    }

    // A breaching missile I breaks obsidian next to its impact; bedrock still survives (D51).
    private static void breachingBreaksObsidian(GameTestHelper helper) {
        BlockPos top = platform(helper, 6, 6);
        BlockPos obsidian = top.offset(-1, 1, 0);
        BlockPos bedrock = top.offset(1, 1, 0);
        helper.setBlock(obsidian, Blocks.OBSIDIAN);
        helper.setBlock(bedrock, Blocks.BEDROCK);
        MissileFlight flight = launchAt(helper, MissileType.BREACHING, 1, top);
        helper.succeedWhen(() -> {
            helper.assertTrue(landed(helper, flight), "the missile should have hit");
            helper.assertBlockNotPresent(Blocks.OBSIDIAN, obsidian);
            helper.assertBlockPresent(Blocks.BEDROCK, bedrock);
        });
    }

    // An EMP missile I next to a powered launcher drains it and shuts it down, and breaks no blocks.
    private static void empShutsDownLauncher(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos top = platform(helper, 6, 6);
        BlockPos launcherPos = top.offset(2, 1, 2);
        helper.setBlock(launcherPos, ModBlocks.MISSILE_LAUNCHER.get());
        MissileLauncherBlockEntity launcher = (MissileLauncherBlockEntity) level.getBlockEntity(helper.absolutePos(launcherPos));
        try (Transaction transaction = Transaction.openRoot()) {
            launcher.energy().insert(10_000, transaction);
            transaction.commit();
        }
        launcher.setItem(MissileLauncherBlockEntity.MISSILE_SLOT, new ItemStack(ModItems.missile(MissileType.EXPLOSIVE, 1).get()));
        ItemStack designator = new ItemStack(ModItems.TARGET_DESIGNATOR.get());
        designator.set(ModDataComponents.TARGET.get(), GlobalPos.of(level.dimension(), helper.absolutePos(launcherPos).offset(200, 0, 0)));
        launcher.setItem(MissileLauncherBlockEntity.DESIGNATOR_SLOT, designator);
        helper.assertTrue(launcher.energy().getAmountAsLong() > 0, "the launcher should hold FE before the EMP");

        MissileFlight flight = launchAt(helper, MissileType.EMP, 1, top);
        helper.succeedWhen(() -> {
            helper.assertTrue(landed(helper, flight), "the missile should have hit");
            helper.assertValueEqual(launcher.energy().getAmountAsLong(), 0L, "FE left in the launcher");
            helper.assertBlockPresent(Blocks.STONE, top);
            LaunchStatus status = launcher.status(level);
            if (RaidWindows.isOpen(RaidWindows.configured(), RaidWindows.now())) {
                helper.assertValueEqual(status.check(), LaunchStatus.Check.DISABLED, "launcher status after the EMP");
            }
        });
    }

    // An incendiary missile I sets fire around its impact once its blast is done.
    private static void incendiarySetsFire(GameTestHelper helper) {
        BlockPos top = platform(helper, 6, 6);
        MissileFlight flight = launchAt(helper, MissileType.INCENDIARY, 1, top);
        helper.succeedWhen(() -> {
            helper.assertTrue(landed(helper, flight), "the missile should have hit");
            boolean fire = false;
            for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(top.offset(-6, -3, -6)), helper.absolutePos(top.offset(6, 4, 6)))) {
                if (helper.getLevel().getBlockState(pos).is(Blocks.FIRE)) {
                    fire = true;
                    break;
                }
            }
            helper.assertTrue(fire, "there should be fire around the impact");
        });
    }

    // A complete silo fires a tier III missile and opens its hatch; without one of its casings it reports the
    // missing block (D55, DESIGN.md §6.8).
    private static void siloStructure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hatch = new BlockPos(5, 0, 5);
        buildSilo(helper, hatch);
        BlockPos controllerPos = hatch.offset(-1, -1, 0);
        SiloControllerBlockEntity controller = (SiloControllerBlockEntity) level.getBlockEntity(helper.absolutePos(controllerPos));
        controller.setItem(MissileLauncherBlockEntity.MISSILE_SLOT, new ItemStack(ModItems.missile(MissileType.EXPLOSIVE, 3).get(), 2));
        ItemStack designator = new ItemStack(ModItems.TARGET_DESIGNATOR.get());
        designator.set(ModDataComponents.TARGET.get(), GlobalPos.of(level.dimension(), helper.absolutePos(hatch).offset(300, 0, 0)));
        controller.setItem(MissileLauncherBlockEntity.DESIGNATOR_SLOT, designator);
        helper.assertValueEqual(controller.menuPos(), helper.absolutePos(hatch.below(SiloStructure.DEPTH)), "where silo missiles start");

        charge(controller.energy(), MissileConfig.launchEnergy(3));
        if (RaidWindows.isOpen(RaidWindows.configured(), RaidWindows.now())) {
            assertSiloStatus(helper, controller, LaunchStatus.Check.READY);
            helper.assertTrue(controller.launch(level, null).isReady(), "the silo should fire a tier III missile");
            List<MissileFlight> flights = MissileData.get(level.getServer()).flights();
            MissileManager.cancel(level.getServer(), flights.getLast().id());
            helper.assertTrue(level.getBlockState(helper.absolutePos(hatch)).getValue(SiloHatchBlock.OPEN), "the hatch should open");
        }

        helper.setBlock(hatch.offset(1, -SiloStructure.DEPTH, 1), Blocks.STONE);
        LaunchStatus incomplete = controller.status(level);
        helper.assertValueEqual(incomplete.check(), LaunchStatus.Check.SILO_INCOMPLETE, "status without a casing");
        helper.assertValueEqual(incomplete.value(), 1, "missing blocks");
        helper.succeed();
    }

    // A complete silo forms, layer by layer, into its connected look: every casing and the controller get their
    // part and the hatch is formed. Without a casing it turns back to plain blocks.
    private static void siloForms(GameTestHelper helper) {
        BlockPos hatch = new BlockPos(5, 0, 5);
        buildSilo(helper, hatch);
        BlockPos casing = hatch.offset(1, -SiloStructure.DEPTH, 1);
        BlockPos controller = hatch.offset(-1, -1, 0);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getBlockState(hatch).getValue(SiloStructure.FORMED), "the hatch should be formed");
                    helper.assertValueEqual(helper.getBlockState(casing).getValue(SiloStructure.PART),
                            SiloStructure.part(helper.absolutePos(hatch), helper.absolutePos(casing)), "a floor casing's part");
                    helper.assertValueEqual(helper.getBlockState(controller).getValue(SiloStructure.PART),
                            SiloStructure.part(helper.absolutePos(hatch), helper.absolutePos(controller)), "the controller's part");
                })
                .thenExecute(() -> helper.setBlock(casing, Blocks.STONE))
                .thenWaitUntil(() -> {
                    helper.assertFalse(helper.getBlockState(hatch).getValue(SiloStructure.FORMED), "the hatch should be plain again");
                    helper.assertValueEqual(helper.getBlockState(controller).getValue(SiloStructure.PART), 0, "the controller's part");
                })
                .thenSucceed();
    }

    // A silo with its hatch at the given position and the controller in the ring below it, on the west side.
    private static void buildSilo(GameTestHelper helper, BlockPos hatch) {
        for (int layer = 0; layer <= SiloStructure.DEPTH; layer++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = hatch.offset(dx, -layer, dz);
                    boolean middle = dx == 0 && dz == 0;
                    if (!middle || layer == SiloStructure.DEPTH) {
                        helper.setBlock(pos, ModBlocks.SILO_CASING.get());
                    } else if (layer == 0) {
                        helper.setBlock(pos, ModBlocks.SILO_HATCH.get());
                    } else {
                        helper.setBlock(pos, Blocks.AIR);
                    }
                }
            }
        }
        helper.setBlock(hatch.offset(-1, -1, 0), ModBlocks.SILO_CONTROLLER.get());
    }

    // A missile on its way down can be hit; enough damage bursts it in the air, and nothing below is harmed.
    private static void missileShotDown(GameTestHelper helper) {
        BlockPos top = platform(helper, 6, 6);
        MissileFlight flight = launchAt(helper, MissileType.EXPLOSIVE, 1, top);
        AABB column = new AABB(helper.absoluteVec(new Vec3(4, -4, 4)), helper.absoluteVec(new Vec3(9, 200, 9)));
        MissileEntity[] missile = new MissileEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    List<MissileEntity> missiles = helper.getLevel().getEntitiesOfClass(MissileEntity.class, column, MissileEntity::isDescending);
                    helper.assertFalse(missiles.isEmpty(), "the missile should be coming down");
                    missile[0] = missiles.getFirst();
                })
                .thenExecute(() -> {
                    helper.assertTrue(missile[0].canBeHitByProjectile(), "bullets should be able to hit the missile");
                    helper.assertTrue(missile[0].hurtServer(helper.getLevel(), helper.getLevel().damageSources().source(ModDamageTypes.BULLET),
                            MissileConfig.missileHealth(1) + 1.0F), "the missile should take the hit");
                    helper.assertTrue(landed(helper, flight), "the flight should end when the missile is shot down");
                })
                .thenExecuteAfter(100, () -> helper.assertBlockPresent(Blocks.STONE, top))
                .thenSucceed();
    }

    // An interceptor next to the target shoots the missile down before it lands.
    private static void interceptorStopsMissile(GameTestHelper helper) {
        BlockPos top = platform(helper, 6, 6);
        helper.setBlock(top.offset(0, 1, 3), ModBlocks.INTERCEPTOR.get());
        charge(helper.getBlockEntity(top.offset(0, 1, 3), InterceptorBlockEntity.class).energy(), 50_000);
        MissileFlight flight = launchAt(helper, MissileType.EXPLOSIVE, 1, top);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(landed(helper, flight), "the missile should be gone"))
                .thenExecuteAfter(20, () -> {
                    helper.assertBlockPresent(Blocks.STONE, top);
                    helper.assertBlockPresent(ModBlocks.INTERCEPTOR.get(), top.offset(0, 1, 3));
                })
                .thenSucceed();
    }

    // A radar sees a missile coming down in its range, gives a redstone signal, and loses the contact once the
    // missile is gone. The launch area it reports contains the launch spot.
    private static void radarTracksMissile(GameTestHelper helper) {
        BlockPos top = platform(helper, 6, 6);
        BlockPos radarPos = top.offset(-2, 1, -2);
        helper.setBlock(radarPos, ModBlocks.RADAR.get());
        RadarBlockEntity radar = (RadarBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(radarPos));
        charge(radar.energy(), 50_000);
        MissileFlight flight = launchAt(helper, MissileType.EXPLOSIVE, 1, top);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertFalse(radar.contacts().isEmpty(), "the radar should see the missile");
                    helper.assertTrue(radar.contacts().getFirst().impactSeconds() > 0, "the missile comes down in range");
                    helper.assertTrue(helper.getBlockState(radarPos).getValue(RadarBlock.POWERED), "the radar should give a signal");
                })
                .thenExecute(() -> {
                    int[] area = RadarBlockEntity.launchArea(flight);
                    BlockPos launch = flight.launchPos();
                    helper.assertTrue(area[0] <= launch.getX() && launch.getX() <= area[1] && area[2] <= launch.getZ() && launch.getZ() <= area[3],
                            "the launch area should contain the launch spot");
                    helper.assertValueEqual(area[1] - area[0], 2 * MissileConfig.radarAreaSize(), "launch area width");
                    MissileManager.cancel(helper.getLevel().getServer(), flight.id());
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(radar.contacts().isEmpty(), "the contact should be gone");
                    helper.assertFalse(helper.getBlockState(radarPos).getValue(RadarBlock.POWERED), "the signal should stop");
                })
                .thenSucceed();
    }

    private static void assertSiloStatus(GameTestHelper helper, SiloControllerBlockEntity controller, LaunchStatus.Check expected) {
        helper.assertValueEqual(controller.status(helper.getLevel()).check(), expected, "silo status");
    }

    private MissileGameTests() {
    }

    // Fills a block's FE buffer with at least amount FE, in steps as large as it takes.
    static void charge(EnergyHandler energy, int amount) {
        for (int i = 0; i < 1000 && energy.getAmountAsLong() < amount; i++) {
            try (Transaction transaction = Transaction.openRoot()) {
                energy.insert(amount, transaction);
                transaction.commit();
            }
        }
    }
}
