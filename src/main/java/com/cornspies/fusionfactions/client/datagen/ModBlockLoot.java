package com.cornspies.fusionfactions.client.datagen;

import java.util.Set;

import com.cornspies.fusionfactions.registry.ModBlocks;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

// Block drops. Supply crates have no loot table (they never drop as items, D17).
public class ModBlockLoot extends BlockLootSubProvider {
    public ModBlockLoot(LootTableSubProvider.Context output) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), output);
    }

    @Override
    protected void generate() {
        this.dropSelf(ModBlocks.WEAPON_WORKBENCH.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().<Block>map(holder -> holder.value())::iterator;
    }
}
