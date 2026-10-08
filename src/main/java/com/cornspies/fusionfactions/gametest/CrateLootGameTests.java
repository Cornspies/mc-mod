package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.supplydrop.CrateRarity;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Supply crate loot (D43, D44), rolled many times. Registered by WeaponGameTests.
public final class CrateLootGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    static final List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> TESTS = List.of(
            TEST_FUNCTIONS.register("crate_loot_follows_tiers", () -> CrateLootGameTests::crateLootFollowsTiers));

    private static final int ROLLS = 400;

    // Every crate has ammo and only our items; upgrade parts match the crate's tier; the heavy weapons
    // come only from legendary crates, about half of them.
    private static void crateLootFollowsTiers(GameTestHelper helper) {
        for (CrateRarity rarity : CrateRarity.values()) {
            int tier = rarity.ordinal() + 1;
            LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(rarity.lootTable());
            int heavy = 0;
            for (int roll = 0; roll < ROLLS; roll++) {
                LootParams params = new LootParams.Builder(helper.getLevel())
                        .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
                        .create(LootContextParamSets.CHEST);
                List<ItemStack> loot = table.getRandomItems(params, roll * 31L + tier);
                helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(ModItems.AMMO.get())), rarity + " crates should always hold ammo");
                boolean heavyHere = false;
                for (ItemStack stack : loot) {
                    String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
                    helper.assertValueEqual(namespace, FusionFactions.MODID, rarity + " crate item namespace");
                    if (stack.getItem() instanceof UpgradeItem part) {
                        // Rare crates may add a tier I part; otherwise the tier matches the crate.
                        boolean allowed = part.tier() == tier || (rarity == CrateRarity.RARE && part.tier() == 1)
                                || (rarity == CrateRarity.LEGENDARY && part.tier() == 2);
                        helper.assertTrue(allowed, rarity + " crate held a tier " + part.tier() + " part");
                    }
                    if (stack.is(ModItems.SNIPER_RIFLE.get()) || stack.is(ModItems.GATLING_GUN.get())) {
                        heavyHere = true;
                    }
                }
                if (heavyHere) {
                    heavy++;
                }
            }
            if (rarity == CrateRarity.LEGENDARY) {
                helper.assertTrue(heavy > ROLLS * 0.35 && heavy < ROLLS * 0.65, "about half the legendary crates should hold a heavy weapon, not " + heavy + "/" + ROLLS);
            } else {
                helper.assertValueEqual(heavy, 0, rarity + " crates with a heavy weapon");
            }
        }
        helper.succeed();
    }

    private CrateLootGameTests() {
    }
}
