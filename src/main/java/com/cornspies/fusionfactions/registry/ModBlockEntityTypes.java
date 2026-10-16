package com.cornspies.fusionfactions.registry;

import java.util.EnumMap;
import java.util.Map;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.ProcessingMachineBlockEntity;
import com.cornspies.fusionfactions.missile.InterceptorBlockEntity;
import com.cornspies.fusionfactions.missile.MissileLauncherBlockEntity;
import com.cornspies.fusionfactions.missile.RadarBlockEntity;
import com.cornspies.fusionfactions.missile.SiloControllerBlockEntity;
import com.cornspies.fusionfactions.power.ChargingStationBlockEntity;
import com.cornspies.fusionfactions.power.CombustionGeneratorBlockEntity;
import com.cornspies.fusionfactions.power.SolarPanelBlockEntity;
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

    // Power (D67).
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR = BLOCK_ENTITY_TYPES.register(
            "combustion_generator", () -> new BlockEntityType<>(CombustionGeneratorBlockEntity::new, ModBlocks.COMBUSTION_GENERATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL = BLOCK_ENTITY_TYPES.register(
            "solar_panel", () -> new BlockEntityType<>(SolarPanelBlockEntity::new, ModBlocks.SOLAR_PANEL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION = BLOCK_ENTITY_TYPES.register(
            "charging_station", () -> new BlockEntityType<>(ChargingStationBlockEntity::new, ModBlocks.CHARGING_STATION.get()));

    // Processing machines (D70): one type per machine, all the same class.
    private static final Map<Machine, DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>>> MACHINES = new EnumMap<>(Machine.class);

    static {
        for (Machine machine : Machine.values()) {
            MACHINES.put(machine, BLOCK_ENTITY_TYPES.register(machine.id(),
                    () -> new BlockEntityType<>((pos, state) -> new ProcessingMachineBlockEntity(machine, pos, state), ModBlocks.machine(machine))));
        }
    }

    public static BlockEntityType<ProcessingMachineBlockEntity> machine(Machine machine) {
        return MACHINES.get(machine).get();
    }

    private ModBlockEntityTypes() {
    }
}
