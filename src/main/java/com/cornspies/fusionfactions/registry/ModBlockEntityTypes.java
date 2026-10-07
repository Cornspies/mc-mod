package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FusionFactions.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyCrateBlockEntity>> SUPPLY_CRATE = BLOCK_ENTITY_TYPES.register("supply_crate",
            () -> new BlockEntityType<>(SupplyCrateBlockEntity::new, ModBlocks.SUPPLY_CRATE.get()));

    private ModBlockEntityTypes() {
    }
}
