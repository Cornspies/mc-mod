package com.cornspies.fusionfactions.client.datagen;

import java.util.Set;

import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

// Block drops. Supply crates have no loot table (they never drop as items, D17).
public class ModBlockLoot extends BlockLootSubProvider {
    public ModBlockLoot(LootTableSubProvider.Context output) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), output);
    }

    @Override
    protected void generate() {
        this.dropSelf(ModBlocks.WEAPON_WORKBENCH.get());
        this.dropSelf(ModBlocks.MISSILE_LAUNCHER.get());
        this.dropSelf(ModBlocks.SILO_CASING.get());
        this.dropSelf(ModBlocks.SILO_HATCH.get());
        this.dropSelf(ModBlocks.SILO_CONTROLLER.get());
        this.dropSelf(ModBlocks.RADAR.get());
        this.dropSelf(ModBlocks.INTERCEPTOR.get());
        this.dropSelf(ModBlocks.COMBUSTION_GENERATOR.get());
        this.dropSelf(ModBlocks.SOLAR_PANEL.get());
        this.dropSelf(ModBlocks.FE_CABLE.get());
        this.dropSelf(ModBlocks.CHARGING_STATION.get());

        // Ores drop their raw item, more with Fortune; niter drops 2-4 like redstone. Silk Touch drops the ore.
        this.add(ModBlocks.BAUXITE_ORE.get(), block -> this.createOreDrop(block, ModItems.RAW_BAUXITE.get()));
        this.add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), block -> this.createOreDrop(block, ModItems.RAW_BAUXITE.get()));
        this.add(ModBlocks.NITER_ORE.get(), this::createNiterOreDrops);
        this.add(ModBlocks.DEEPSLATE_NITER_ORE.get(), this::createNiterOreDrops);
        this.add(ModBlocks.RARE_EARTH_ORE.get(), block -> this.createOreDrop(block, ModItems.RAW_RARE_EARTH.get()));
        this.add(ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get(), block -> this.createOreDrop(block, ModItems.RAW_RARE_EARTH.get()));
        this.add(ModBlocks.LITHIUM_ORE.get(), block -> this.createOreDrop(block, ModItems.RAW_LITHIUM.get()));
        this.add(ModBlocks.DEEPSLATE_LITHIUM_ORE.get(), block -> this.createOreDrop(block, ModItems.RAW_LITHIUM.get()));
    }

    private LootTable.Builder createNiterOreDrops(Block block) {
        return this.createSilkTouchDispatchTable(block, (LootPoolEntryContainer.Builder<?>) this.applyExplosionDecay(block,
                LootItem.lootTableItem(ModItems.NITER.get())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 4)))
                        .apply(ApplyBonusCount.addUniformBonusCount(this.enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().<Block>map(holder -> holder.value())::iterator;
    }
}
