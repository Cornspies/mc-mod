package com.cornspies.fusionfactions.client.datagen;

import com.cornspies.fusionfactions.supplydrop.CrateRarity;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

// Placeholder vanilla loot until the weapons exist. Final contents per tier are OD-19 (M4).
public class SupplyCrateLoot implements LootTableSubProvider {
    private final LootTableSubProvider.Context output;

    public SupplyCrateLoot(LootTableSubProvider.Context output) {
        this.output = output;
    }

    @Override
    public void run() {
        this.output.accept(CrateRarity.COMMON.lootTable(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.between(4, 6))
                        .add(item(Items.BREAD, 10, 2, 6))
                        .add(item(Items.COOKED_BEEF, 10, 2, 5))
                        .add(item(Items.IRON_INGOT, 10, 2, 6))
                        .add(item(Items.ARROW, 8, 8, 16))
                        .add(item(Items.GOLDEN_CARROT, 6, 2, 4))
                        .add(item(Items.ENDER_PEARL, 2, 1, 2))
                        .add(item(Items.IRON_SWORD, 3))
                        .add(item(Items.SHIELD, 3))
                        .add(item(Items.IRON_HELMET, 2))
                        .add(item(Items.IRON_CHESTPLATE, 2))));

        this.output.accept(CrateRarity.RARE.lootTable(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.between(5, 7))
                        .add(item(Items.GOLDEN_APPLE, 8, 1, 3))
                        .add(item(Items.IRON_INGOT, 8, 6, 12))
                        .add(item(Items.DIAMOND, 6, 1, 3))
                        .add(item(Items.ENDER_PEARL, 6, 2, 4))
                        .add(item(Items.ARROW, 6, 16, 32))
                        .add(item(Items.EXPERIENCE_BOTTLE, 6, 4, 8))
                        .add(item(Items.IRON_CHESTPLATE, 4))
                        .add(item(Items.IRON_LEGGINGS, 4))
                        .add(item(Items.DIAMOND_SWORD, 3))
                        .add(item(Items.CROSSBOW, 3))));

        this.output.accept(CrateRarity.LEGENDARY.lootTable(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.between(6, 8))
                        .add(item(Items.DIAMOND, 8, 3, 6))
                        .add(item(Items.GOLDEN_APPLE, 8, 3, 6))
                        .add(item(Items.ENDER_PEARL, 6, 4, 8))
                        .add(item(Items.EXPERIENCE_BOTTLE, 6, 8, 16))
                        .add(item(Items.DIAMOND_HELMET, 4))
                        .add(item(Items.DIAMOND_CHESTPLATE, 4))
                        .add(item(Items.DIAMOND_LEGGINGS, 4))
                        .add(item(Items.DIAMOND_BOOTS, 4))
                        .add(item(Items.NETHERITE_SCRAP, 3, 1, 2))
                        .add(item(Items.TOTEM_OF_UNDYING, 2))
                        .add(item(Items.ENCHANTED_GOLDEN_APPLE, 2))));
    }

    private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
        return LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
    }
}
