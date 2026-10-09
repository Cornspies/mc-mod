package com.cornspies.fusionfactions.client.datagen;

import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.supplydrop.CrateRarity;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

// Supply crate loot (OD-19 → D43): only our own items. Legendary crates may hold a tier III missile (D57). Every crate has ammo, weapon parts and upgrade
// parts of its tier (tier I in common, II in rare, III in legendary crates); weapons are a chance, the
// crate-only sniper and gatling only in legendary crates (D44). Each pool is one kind of loot, so the
// odds stay readable. Servers can override the tables with a datapack (supply_crate/<rarity>).
public class SupplyCrateLoot implements LootTableSubProvider {
    private final LootTableSubProvider.Context output;

    public SupplyCrateLoot(LootTableSubProvider.Context output) {
        this.output = output;
    }

    @Override
    public void run() {
        this.output.accept(CrateRarity.COMMON.lootTable(), LootTable.lootTable()
                .withPool(single(item(ModItems.AMMO.get(), 1, 16, 32)))
                .withPool(weaponParts(1, 2, 1, 2))
                .withPool(upgrades(1, 1, 2))
                // Half the common crates hold a sidearm.
                .withPool(single(item(ModItems.PISTOL.get(), 3), item(ModItems.AUTO_PISTOL.get(), 2), nothing(5))));

        this.output.accept(CrateRarity.RARE.lootTable(), LootTable.lootTable()
                .withPool(single(item(ModItems.AMMO.get(), 1, 32, 48)))
                .withPool(weaponParts(2, 3, 1, 3))
                .withPool(upgrades(2, 1, 2))
                // Half the time a tier I part on top.
                .withPool(single(upgradeEntries(1, nothing(UpgradeType.values().length))))
                // Most rare crates hold a weapon, usually a close-range one.
                .withPool(single(item(ModItems.SMG.get(), 3), item(ModItems.SHOTGUN.get(), 3), item(ModItems.AUTO_PISTOL.get(), 1), nothing(3))));

        this.output.accept(CrateRarity.LEGENDARY.lootTable(), LootTable.lootTable()
                .withPool(single(item(ModItems.AMMO.get(), 1, 48, 64)))
                .withPool(upgrades(3, 1, 2))
                .withPool(upgrades(2, 1, 2))
                // Half the legendary crates hold one of the crate-only heavy weapons (D44)...
                .withPool(single(item(ModItems.SNIPER_RIFLE.get(), 1), item(ModItems.GATLING_GUN.get(), 1), nothing(2)))
                // ...and some a close-range weapon as well.
                .withPool(single(item(ModItems.SMG.get(), 1), item(ModItems.SHOTGUN.get(), 1), nothing(4)))
                // Tier III missiles only come from here (D57): a quarter of the crates hold one, any type.
                .withPool(single(missileEntries(3, nothing(3 * MissileType.values().length)))));
    }

    // One roll from the given entries.
    private static LootPool.Builder single(LootPoolEntryContainer.Builder<?>... entries) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
        for (LootPoolEntryContainer.Builder<?> entry : entries) {
            pool.add(entry);
        }
        return pool;
    }

    // minRolls to maxRolls stacks of barrels, fusion cores and grips, each minCount to maxCount.
    private static LootPool.Builder weaponParts(int minRolls, int maxRolls, int minCount, int maxCount) {
        return LootPool.lootPool().setRolls(ContextIntProviders.between(minRolls, maxRolls))
                .add(item(ModItems.BARREL.get(), 1, minCount, maxCount))
                .add(item(ModItems.FUSION_CORE.get(), 1, minCount, maxCount))
                .add(item(ModItems.GRIP.get(), 1, minCount, maxCount));
    }

    // minRolls to maxRolls upgrade parts of one tier, every type equally likely.
    private static LootPool.Builder upgrades(int tier, int minRolls, int maxRolls) {
        return single(upgradeEntries(tier)).setRolls(ContextIntProviders.between(minRolls, maxRolls));
    }

    private static LootPoolEntryContainer.Builder<?>[] upgradeEntries(int tier, LootPoolEntryContainer.Builder<?>... extra) {
        UpgradeType[] types = UpgradeType.values();
        LootPoolEntryContainer.Builder<?>[] entries = new LootPoolEntryContainer.Builder<?>[types.length + extra.length];
        for (int i = 0; i < types.length; i++) {
            entries[i] = item(ModItems.upgrade(types[i], tier).get(), 1);
        }
        System.arraycopy(extra, 0, entries, types.length, extra.length);
        return entries;
    }

    private static LootPoolEntryContainer.Builder<?>[] missileEntries(int tier, LootPoolEntryContainer.Builder<?>... extra) {
        MissileType[] types = MissileType.values();
        LootPoolEntryContainer.Builder<?>[] entries = new LootPoolEntryContainer.Builder<?>[types.length + extra.length];
        for (int i = 0; i < types.length; i++) {
            entries[i] = item(ModItems.missile(types[i], tier).get(), 1);
        }
        System.arraycopy(extra, 0, entries, types.length, extra.length);
        return entries;
    }

    private static LootPoolEntryContainer.Builder<?> nothing(int weight) {
        return EmptyLootItem.emptyItem().setWeight(weight);
    }

    private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
        return LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
    }
}
