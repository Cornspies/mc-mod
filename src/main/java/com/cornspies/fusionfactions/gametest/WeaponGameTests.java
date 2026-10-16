package com.cornspies.fusionfactions.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.StartupConfig;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModEntityTypes;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.BulletEntity;
import com.cornspies.fusionfactions.weapon.ModDamageTypes;
import com.cornspies.fusionfactions.weapon.Trigger;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.Weapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Server-side weapon checks. Run with ./gradlew runGameTestServer.
public final class WeaponGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PISTOL_HITS_TARGET =
            TEST_FUNCTIONS.register("pistol_hits_target", () -> WeaponGameTests::pistolHitsTarget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PISTOL_HITS_TARGET_UNDERWATER =
            TEST_FUNCTIONS.register("pistol_hits_target_underwater", () -> WeaponGameTests::pistolHitsTargetUnderwater);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PISTOL_RANGE_UNDERWATER =
            TEST_FUNCTIONS.register("pistol_range_underwater", () -> WeaponGameTests::pistolRangeUnderwater);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SPENT_BULLET_SINKS =
            TEST_FUNCTIONS.register("spent_bullet_sinks", () -> WeaponGameTests::spentBulletSinks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BULLET_LEAVING_WATER_DROPS =
            TEST_FUNCTIONS.register("bullet_leaving_water_drops", () -> WeaponGameTests::bulletLeavingWaterDrops);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BULLET_IGNORES_LATE_UPDATES =
            TEST_FUNCTIONS.register("bullet_ignores_late_updates", () -> WeaponGameTests::bulletIgnoresLateUpdates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BODY_SHOT_DAMAGE =
            TEST_FUNCTIONS.register("body_shot_damage", () -> WeaponGameTests::bodyShotDamage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HEADSHOT_DAMAGE =
            TEST_FUNCTIONS.register("headshot_damage", () -> WeaponGameTests::headshotDamage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARMOR_PIERCING =
            TEST_FUNCTIONS.register("armor_piercing", () -> WeaponGameTests::armorPiercing);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SHOTGUN_FIRES_PELLETS =
            TEST_FUNCTIONS.register("shotgun_fires_pellets", () -> WeaponGameTests::shotgunFiresPellets);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEAPON_RECIPES =
            TEST_FUNCTIONS.register("weapon_recipes", () -> WeaponGameTests::weaponRecipes);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELOAD_CONSUMES_AMMO =
            TEST_FUNCTIONS.register("reload_consumes_ammo", () -> WeaponGameTests::reloadConsumesAmmo);

    private static final int MAX_TICKS = 100;
    // The firing tests use lanes of up to 40 blocks outside the 1x1x1 structure, so keep other tests away from them.
    private static final int PADDING = 42;

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(FusionFactions.id("default"));
        List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> functions = new ArrayList<>(List.of(PISTOL_HITS_TARGET,
                PISTOL_HITS_TARGET_UNDERWATER, PISTOL_RANGE_UNDERWATER, SPENT_BULLET_SINKS, BULLET_LEAVING_WATER_DROPS, BULLET_IGNORES_LATE_UPDATES,
                BODY_SHOT_DAMAGE, HEADSHOT_DAMAGE, ARMOR_PIERCING, SHOTGUN_FIRES_PELLETS, WEAPON_RECIPES, RELOAD_CONSUMES_AMMO));
        functions.addAll(UpgradeGameTests.TESTS);
        functions.addAll(TriggerGameTests.TESTS);
        functions.addAll(CrateLootGameTests.TESTS);
        functions.addAll(ResourceGameTests.TESTS);
        functions.addAll(PowerGameTests.TESTS);
        for (DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function : functions) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"),
                    MAX_TICKS, 0, true, Rotation.NONE, false, 1, 1, false, PADDING);
            event.registerTest(function.getId(), new FunctionGameTestInstance(function.key(), data));
        }
        MissileGameTests.register(event, environment);
        MachineGameTests.register(event, environment);
    }

    // One shot from the pistol hurts a pig 5.5 blocks away, with the right damage attribution.
    private static void pistolHitsTarget(GameTestHelper helper) {
        assertShotHits(helper, shootAtPig(helper, 6, false));
    }

    // Underwater, bullets slow down but still reach a pig 5.5 blocks away.
    private static void pistolHitsTargetUnderwater(GameTestHelper helper) {
        assertShotHits(helper, shootAtPig(helper, 6, true));
    }

    // Underwater, a pig 13.5 blocks away is out of range.
    private static void pistolRangeUnderwater(GameTestHelper helper) {
        Shot shot = shootAtPig(helper, 14, true);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(shot.pig().getHealth() >= shot.pig().getMaxHealth(), "the pig should be out of range underwater");
            helper.succeed();
        });
    }

    // Underwater, a bullet that drag has slowed down doesn't vanish: it curves down, sinks, and
    // disappears when it reaches the floor.
    private static void spentBulletSinks(GameTestHelper helper) {
        int length = 14;
        buildLane(helper, length, length);
        TrackedShot shot = fireStraightAhead(helper, length);
        helper.succeedWhen(() -> {
            helper.assertTrue(shot.isGone(), "the bullet should be gone");
            helper.assertTrue(shot.seenSinking[0], "the spent bullet should sink instead of vanishing");
            helper.assertTrue(shot.endedOnFloor(), "the bullet should reach the floor before it disappears");
            double travelled = shot.travelled();
            helper.assertTrue(travelled > 8.0 && travelled < 12.0, "the bullet should sink about 9 to 10 blocks out, not " + travelled);
        });
    }

    // A bullet fired from inside 3 blocks of water keeps its lost speed after leaving the water: it
    // drops in a short arc (about 20 blocks here) instead of flying on like a normal shot (about 40+).
    private static void bulletLeavingWaterDrops(GameTestHelper helper) {
        int length = 40;
        buildLane(helper, length, 3);
        TrackedShot shot = fireStraightAhead(helper, length);
        helper.succeedWhen(() -> {
            helper.assertTrue(shot.isGone(), "the bullet should be gone");
            helper.assertTrue(shot.endedOnFloor(), "the bullet should drop onto the floor");
            double travelled = shot.travelled();
            helper.assertTrue(travelled > 10.0 && travelled < 25.0, "the bullet should land about 20 blocks out, not " + travelled);
        });
    }

    // One pistol shot along the lane, with its path tracked every tick. Checked by condition, not at fixed
    // ticks: freshly force-loaded chunks can take a few ticks to start ticking, which can hold a bullet up.
    // Only bullets fired by this test's player count: all tests run at once in the same world.
    private record TrackedShot(GameTestHelper helper, Player shooter, AABB lane, Vec3 muzzle, double floorY, Vec3[] lastPosition, boolean[] seenSinking) {
        List<BulletEntity> bullets() {
            return this.helper.getLevel().getEntitiesOfClass(BulletEntity.class, this.lane, bullet -> bullet.getOwner() == this.shooter);
        }

        boolean isGone() {
            return this.bullets().isEmpty();
        }

        boolean endedOnFloor() {
            return this.lastPosition[0] != null && this.lastPosition[0].y - this.floorY < 0.5;
        }

        double travelled() {
            return this.lastPosition[0] == null ? 0.0 : this.lastPosition[0].z - this.muzzle.z;
        }
    }

    private static TrackedShot fireStraightAhead(GameTestHelper helper, int length) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PISTOL.get()));
        Trigger.press(player);
        Trigger.tick(player);

        Vec3 muzzle = new Vec3(feet.x, player.getEyeY() - 0.1, feet.z);
        double floorY = helper.absoluteVec(new Vec3(0.0, 1.0, 0.0)).y;
        AABB lane = new AABB(helper.absoluteVec(new Vec3(-2.0, 0.0, -1.0)), helper.absoluteVec(new Vec3(3.0, 5.0, length + 1.0)));
        TrackedShot shot = new TrackedShot(helper, player, lane, muzzle, floorY, new Vec3[1], new boolean[1]);
        for (int tick = 1; tick < MAX_TICKS; tick++) {
            helper.runAfterDelay(tick, () -> shot.bullets().forEach(bullet -> {
                shot.lastPosition()[0] = bullet.position();
                Vec3 motion = bullet.getDeltaMovement();
                if (motion.length() < 0.25 && motion.y < 0.0 && bullet.getY() < muzzle.y - 0.05) {
                    shot.seenSinking()[0] = true;
                }
            }));
        }
        return shot;
    }

    // Clients run bullets one tick ahead of the server's updates at times. Late updates must not pull the
    // bullet back or speed it up again; real changes (a deflection, a big difference) must still apply.
    // Drives the same entry points the client's packet handlers call.
    private static void bulletIgnoresLateUpdates(GameTestHelper helper) {
        BulletEntity bullet = new BulletEntity(ModEntityTypes.BULLET.get(), helper.getLevel());
        Vec3 start = helper.absoluteVec(new Vec3(0.5, 2.0, 0.5));
        bullet.setPos(start);
        bullet.setDeltaMovement(new Vec3(0.0, 0.0, 4.0));
        bullet.tick();
        Vec3 afterOneTick = bullet.position();
        helper.assertTrue(afterOneTick.distanceTo(start) > 3.9, "the bullet should move one tick forward");

        bullet.moveOrInterpolateTo(PositionPath.of(start));
        helper.assertTrue(bullet.position().equals(afterOneTick), "a position one tick behind should be ignored");

        Vec3 motion = bullet.getDeltaMovement();
        bullet.lerpMotion(motion.scale(1.5));
        helper.assertTrue(bullet.getDeltaMovement().equals(motion), "a late velocity in the same direction should be ignored");

        bullet.lerpMotion(motion.scale(-1.0));
        helper.assertTrue(bullet.getDeltaMovement().equals(motion.scale(-1.0)), "a velocity that turns the bullet should be applied");

        Vec3 farAway = afterOneTick.add(10.0, 0.0, 0.0);
        bullet.moveOrInterpolateTo(PositionPath.of(farAway));
        helper.assertTrue(bullet.position().equals(farAway), "a position far from the bullet should be applied");
        helper.succeed();
    }

    private record Shot(Player player, Pig pig) {
    }

    // A stone floor `length` blocks long, with a wall at the end so no bullet flies on into another test
    // (they all run at once in the same world). The first `waterBlocks` of it are flooded two blocks deep.
    private static void buildLane(GameTestHelper helper, int length, int waterBlocks) {
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 5; y++) {
                helper.setBlock(x, y, length + 2, Blocks.STONE);
            }
        }
        for (int z = 0; z <= length; z++) {
            helper.setBlock(0, 0, z, Blocks.STONE);
            if (z < waterBlocks) {
                helper.setBlock(0, 1, z, Blocks.WATER);
                helper.setBlock(0, 2, z, Blocks.WATER);
            }
            // The runner only keeps the 1x1x1 structure's chunk active. Bullets freeze in chunks that
            // don't tick, so force-load the lane. The runner un-forces all chunks after the batch.
            BlockPos lane = helper.absolutePos(new BlockPos(0, 0, z));
            helper.getLevel().setChunkForced(SectionPos.blockToSectionCoord(lane.getX()), SectionPos.blockToSectionCoord(lane.getZ()), true);
        }
    }

    // Builds a lane, puts a pig `distance` blocks away and fires one pistol shot at its middle.
    private static Shot shootAtPig(GameTestHelper helper, int distance, boolean underwater) {
        buildLane(helper, distance, underwater ? distance + 1 : 0);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(0, 1, distance));
        return new Shot(fireAt(helper, pig.getBoundingBox().getCenter()), pig);
    }

    // A mock player at the start of the lane fires one pistol shot at `target`, aiming from where the
    // bullet starts (just below the eyes).
    private static Player fireAt(GameTestHelper helper, Vec3 target) {
        return fireAt(helper, target, new ItemStack(ModItems.PISTOL.get()));
    }

    private static Player fireAt(GameTestHelper helper, Vec3 target, ItemStack weapon) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        Vec3 muzzle = feet.add(0.0, player.getEyeHeight() - 0.1, 0.0);
        Vec3 aim = target.subtract(muzzle);
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance()));
        player.snapTo(feet.x, feet.y, feet.z, yaw, pitch);
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        Trigger.press(player);
        Trigger.tick(player);
        return player;
    }

    // One shotgun shot at a husk 5.5 blocks away lands several of its 8 pellets.
    private static void shotgunFiresPellets(GameTestHelper helper) {
        Husk husk = spawnHuskTarget(helper, false);
        Player player = fireAt(helper, new Vec3(husk.getX(), husk.getY() + 0.9, husk.getZ()), new ItemStack(ModItems.SHOTGUN.get()));
        helper.assertTrue(WeaponItem.getAmmo(player.getMainHandItem()) == Weapons.SHOTGUN.magazineSize() - 1, "one shell should be used");
        float onePellet = expectedDamage(husk, Weapons.SHOTGUN.damage(), Weapons.SHOTGUN.armorPiercing());
        helper.succeedWhen(() -> {
            float taken = husk.getMaxHealth() - husk.getHealth();
            helper.assertTrue(taken > onePellet * 3.0F, "at least 4 pellets should hit, got " + taken / onePellet);
        });
    }

    // A pistol shot at a husk's chest does normal damage (after its natural armor and armor piercing).
    private static void bodyShotDamage(GameTestHelper helper) {
        Husk husk = spawnHuskTarget(helper, false);
        fireAt(helper, new Vec3(husk.getX(), husk.getY() + 0.9, husk.getZ()));
        assertDamageTaken(helper, husk, Weapons.PISTOL.damage(), Weapons.PISTOL.armorPiercing());
    }

    // A pistol shot at a husk's eyes is a headshot (D25).
    private static void headshotDamage(GameTestHelper helper) {
        Husk husk = spawnHuskTarget(helper, false);
        fireAt(helper, husk.getEyePosition());
        assertDamageTaken(helper, husk, Weapons.PISTOL.damage() * BulletEntity.HEADSHOT_MULTIPLIER, Weapons.PISTOL.armorPiercing());
    }

    // Against full diamond armor, the pistol's armor piercing (D24) lets about twice the vanilla damage through.
    private static void armorPiercing(GameTestHelper helper) {
        Husk husk = spawnHuskTarget(helper, true);
        // Armor from newly equipped items only counts after the husk has ticked, so fire a little later.
        helper.runAfterDelay(5, () -> {
            float pierced = expectedDamage(husk, Weapons.PISTOL.damage(), Weapons.PISTOL.armorPiercing());
            helper.assertTrue(pierced > expectedDamage(husk, Weapons.PISTOL.damage(), 0.0F) + 0.5F, "the test needs armor where piercing matters");
            fireAt(helper, new Vec3(husk.getX(), husk.getY() + 0.9, husk.getZ()));
        });
        assertDamageTaken(helper, husk, Weapons.PISTOL.damage(), Weapons.PISTOL.armorPiercing());
    }

    private static Husk spawnHuskTarget(GameTestHelper helper, boolean diamondArmor) {
        int distance = 6;
        buildLane(helper, distance, 0);
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(0, 1, distance));
        husk.setBaby(false);
        if (diamondArmor) {
            husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            husk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            husk.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            husk.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        }
        return husk;
    }

    // Vanilla's armor formula (CombatRules), with `piercing` of the armor's protection ignored.
    private static float expectedDamage(LivingEntity target, float damage, float piercing) {
        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float realArmor = Math.clamp(armor - damage / (2.0F + toughness / 4.0F), armor * 0.2F, 20.0F);
        return damage * (1.0F - realArmor / 25.0F * (1.0F - piercing));
    }

    // Passes once the target has taken exactly one bullet's damage, as expectedDamage computes it.
    private static void assertDamageTaken(GameTestHelper helper, LivingEntity target, float damage, float piercing) {
        helper.succeedWhen(() -> {
            float taken = target.getMaxHealth() - target.getHealth();
            float expected = expectedDamage(target, damage, piercing);
            helper.assertTrue(Math.abs(taken - expected) < 0.05F, "expected " + expected + " damage, got " + taken);
        });
    }

    private static void assertShotHits(GameTestHelper helper, Shot shot) {
        Pig pig = shot.pig();
        helper.succeedWhen(() -> {
            helper.assertTrue(WeaponItem.getAmmo(shot.player().getMainHandItem()) == Weapons.PISTOL.magazineSize() - 1, "one round should be used");
            helper.assertTrue(pig.getHealth() <= pig.getMaxHealth() - Weapons.PISTOL.damage() + 0.01F, "the pig should take one bullet of damage");
            DamageSource source = pig.getLastDamageSource();
            if (source == null) {
                helper.fail("the pig wasn't hurt");
                return;
            }
            helper.assertTrue(source.is(ModDamageTypes.BULLET), "damage should use the bullet damage type");
            helper.assertTrue(source.getEntity() == shot.player(), "the shooter should be the causing entity");
            helper.assertTrue(source.getDirectEntity() instanceof BulletEntity, "the bullet should be the direct entity");
        });
    }

    // The craftable weapons and parts have recipes. The crate-only weapons only have one if the startup
    // config allows it (D26; off by default).
    private static void weaponRecipes(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (String name : List.of("assembling/barrel", "assembling/fusion_core", "assembling/grip", "ammo", "pistol", "auto_pistol", "smg", "shotgun")) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, FusionFactions.id(name))).isPresent(), "missing recipe " + name);
        }
        boolean heavyAllowed = StartupConfig.HEAVY_WEAPON_RECIPES.getAsBoolean();
        for (String name : List.of("sniper_rifle", "gatling_gun")) {
            boolean present = recipes.byKey(ResourceKey.create(Registries.RECIPE, FusionFactions.id(name))).isPresent();
            helper.assertTrue(present == heavyAllowed, name + " recipe should " + (heavyAllowed ? "exist" : "not exist") + " with heavyWeaponRecipes = " + heavyAllowed);
        }
        helper.succeed();
    }

    // Reloading an empty pistol with 5 ammo items loads 5 rounds, uses all 5 items, then stops.
    private static void reloadConsumesAmmo(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(ModItems.PISTOL.get());
        gun.set(ModDataComponents.AMMO.get(), 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(new ItemStack(ModItems.AMMO.get(), 5));

        WeaponItem.tryStartReload(player);
        helper.assertTrue(WeaponItem.isReloading(player.getMainHandItem()), "the reload should start");

        // Mock players aren't ticked by the server, so tick the inventory by hand.
        for (int tick = 1; tick < MAX_TICKS; tick++) {
            helper.runAfterDelay(tick, () -> player.getInventory().tick());
        }
        helper.succeedWhen(() -> {
            ItemStack held = player.getMainHandItem();
            helper.assertTrue(WeaponItem.getAmmo(held) == 5, "five rounds should be loaded");
            helper.assertTrue(WeaponItem.countAmmo(player) == 0, "all five ammo items should be used");
            helper.assertTrue(!WeaponItem.isReloading(held), "the reload should stop when the ammo runs out");
        });
    }

    private WeaponGameTests() {
    }
}
