package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.Trigger;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.WeaponStats;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Trigger and aiming checks (D37–D39). Registered by WeaponGameTests. The shooters aim at the floor so
// no bullet leaves the test.
public final class TriggerGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    static final List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> TESTS = List.of(
            TEST_FUNCTIONS.register("semi_auto_fires_once_per_pull", () -> TriggerGameTests::semiAutoFiresOncePerPull),
            TEST_FUNCTIONS.register("automatic_fires_while_held", () -> TriggerGameTests::automaticFiresWhileHeld),
            TEST_FUNCTIONS.register("aiming_reduces_spread", () -> TriggerGameTests::aimingReducesSpread));

    // Holding the trigger of a semi-automatic weapon fires once; pulling it again fires again.
    private static void semiAutoFiresOncePerPull(GameTestHelper helper) {
        Player player = shooter(helper, ModItems.PISTOL.get());
        Trigger.press(player);
        Trigger.tick(player);
        helper.assertValueEqual(WeaponItem.getAmmo(player.getMainHandItem()), 11, "rounds after the first pull");
        helper.onEachTick(() -> Trigger.tick(player));
        helper.runAfterDelay(10, () -> {
            helper.assertValueEqual(WeaponItem.getAmmo(player.getMainHandItem()), 11, "rounds after holding the trigger");
            Trigger.release(player);
            Trigger.press(player);
            Trigger.tick(player);
            helper.assertValueEqual(WeaponItem.getAmmo(player.getMainHandItem()), 10, "rounds after the second pull");
            helper.succeed();
        });
    }

    // An automatic weapon keeps firing at its fire interval while the trigger is held, and stops on release.
    private static void automaticFiresWhileHeld(GameTestHelper helper) {
        Player player = shooter(helper, ModItems.SMG.get());
        int magazine = WeaponItem.getAmmo(player.getMainHandItem());
        Trigger.press(player);
        helper.onEachTick(() -> Trigger.tick(player));
        helper.runAfterDelay(10, () -> {
            int fired = magazine - WeaponItem.getAmmo(player.getMainHandItem());
            // One shot every 2 ticks for 10 ticks.
            helper.assertTrue(fired >= 4 && fired <= 6, "the SMG should fire about 5 times in 10 ticks, not " + fired);
            Trigger.release(player);
            int left = WeaponItem.getAmmo(player.getMainHandItem());
            helper.runAfterDelay(6, () -> {
                helper.assertValueEqual(WeaponItem.getAmmo(player.getMainHandItem()), left, "rounds after releasing the trigger");
                helper.succeed();
            });
        });
    }

    // Aiming down sights halves the spread; the sniper is only accurate when scoped.
    private static void aimingReducesSpread(GameTestHelper helper) {
        for (Item item : List.of(ModItems.SMG.get(), ModItems.SNIPER_RIFLE.get())) {
            Player player = shooter(helper, item);
            ItemStack stack = player.getMainHandItem();
            WeaponStats stats = ((WeaponItem) item).stats(stack);
            helper.assertValueEqual(WeaponItem.spread(player, stack, stats), stats.spreadDegrees(), "hip-fire spread");
            player.startUsingItem(InteractionHand.MAIN_HAND);
            helper.assertTrue(WeaponItem.isAiming(player, stack), "using the weapon should count as aiming");
            helper.assertValueEqual(WeaponItem.spread(player, stack, stats), stats.spreadDegrees() * stats.aimSpreadFactor(), "aimed spread");
        }
        helper.assertValueEqual(WeaponItem.spread(shooter(helper, ModItems.SNIPER_RIFLE.get()), new ItemStack(ModItems.SNIPER_RIFLE.get()),
                ModItems.SNIPER_RIFLE.get().baseStats()), 4.0F, "the sniper's hip-fire spread");
        helper.succeed();
    }

    // A survival player above the test block, looking straight down.
    private static Player shooter(GameTestHelper helper, Item weapon) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 position = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        player.snapTo(position.x, position.y, position.z, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon));
        return player;
    }

    private TriggerGameTests() {
    }
}
