package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.InterceptorBlockEntity;
import com.cornspies.fusionfactions.missile.MissileLauncherBlockEntity;
import com.cornspies.fusionfactions.missile.RadarBlockEntity;
import com.cornspies.fusionfactions.missile.SiloControllerBlockEntity;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FusionFactions.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyCrateBlockEntity>> SUPPLY_CRATE = BLOCK_ENTITY_TYPES.register("supply_crate",
            () -> new BlockEntityType<>(SupplyCrateBlockEntity::new, ModBlocks.SUPPLY_CRATE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MissileLauncherBlockEntity>> MISSILE_LAUNCHER = BLOCK_ENTITY_TYPES.register(
            "missile_launcher", () -> new BlockEntityType<>(MissileLauncherBlockEntity::new, ModBlocks.MISSILE_LAUNCHER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiloControllerBlockEntity>> SILO_CONTROLLER = BLOCK_ENTITY_TYPES.register(
            "silo_controller", () -> new BlockEntityType<>(SiloControllerBlockEntity::new, ModBlocks.SILO_CONTROLLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITY_TYPES.register(
            "radar", () -> new BlockEntityType<>(RadarBlockEntity::new, ModBlocks.RADAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InterceptorBlockEntity>> INTERCEPTOR = BLOCK_ENTITY_TYPES.register(
            "interceptor", () -> new BlockEntityType<>(InterceptorBlockEntity::new, ModBlocks.INTERCEPTOR.get()));

    private ModBlockEntityTypes() {
    }
}
