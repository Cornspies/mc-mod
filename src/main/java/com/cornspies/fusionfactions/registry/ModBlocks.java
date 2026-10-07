package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FusionFactions.MODID);

    // No block item: crates only come from drops.
    public static final DeferredBlock<SupplyCrateBlock> SUPPLY_CRATE = BLOCKS.registerBlock("supply_crate", SupplyCrateBlock::new, properties -> properties
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            // Unbreakable and blast-proof like bedrock (D17).
            .strength(-1.0F, 3_600_000.0F)
            .noLootTable()
            .pushReaction(PushReaction.IMMOVEABLE));

    private ModBlocks() {
    }
}
