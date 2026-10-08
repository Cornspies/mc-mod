package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.BulletEntity;
import com.cornspies.fusionfactions.weapon.ModDamageTypes;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
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
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELOAD_CONSUMES_AMMO =
            TEST_FUNCTIONS.register("reload_consumes_ammo", () -> WeaponGameTests::reloadConsumesAmmo);

    private static final int MAX_TICKS = 100;
    // The firing tests use lanes of up to 15 blocks outside the 1x1x1 structure, so keep other tests away from them.
    private static final int PADDING = 16;

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(FusionFactions.id("default"));
        for (DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function
                : List.of(PISTOL_HITS_TARGET, PISTOL_HITS_TARGET_UNDERWATER, PISTOL_RANGE_UNDERWATER, RELOAD_CONSUMES_AMMO)) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"),
                    MAX_TICKS, 0, true, Rotation.NONE, false, 1, 1, false, PADDING);
            event.registerTest(function.getId(), new FunctionGameTestInstance(function.key(), data));
        }
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

    private record Shot(Player player, Pig pig) {
    }

    // Builds a lane, puts a pig `distance` blocks away and fires one pistol shot at its middle.
    private static Shot shootAtPig(GameTestHelper helper, int distance, boolean underwater) {
        for (int z = 0; z <= distance; z++) {
            helper.setBlock(0, 0, z, Blocks.STONE);
            if (underwater) {
                helper.setBlock(0, 1, z, Blocks.WATER);
                helper.setBlock(0, 2, z, Blocks.WATER);
            }
            // The runner only keeps the 1x1x1 structure's chunk active. Bullets freeze in chunks that
            // don't tick, so force-load the lane. The runner un-forces all chunks after the batch.
            BlockPos lane = helper.absolutePos(new BlockPos(0, 0, z));
            helper.getLevel().setChunkForced(SectionPos.blockToSectionCoord(lane.getX()), SectionPos.blockToSectionCoord(lane.getZ()), true);
        }
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(0, 1, distance));
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);

        // Aim from where the bullet starts (just below the eyes) at the middle of the pig.
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        Vec3 muzzle = feet.add(0.0, player.getEyeHeight() - 0.1, 0.0);
        Vec3 aim = pig.getBoundingBox().getCenter().subtract(muzzle);
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance()));
        player.snapTo(feet.x, feet.y, feet.z, yaw, pitch);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PISTOL.get()));
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        return new Shot(player, pig);
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
