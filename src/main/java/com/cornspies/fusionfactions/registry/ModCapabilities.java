package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.power.CableNetworks;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// FE for the missile blocks, generators and machines (D47, D59, D67): any side, so cables, ours or other mods', can
// reach them. Cables have no block entity: their handler passes FE on to the cable network.
public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.MISSILE_LAUNCHER.get(), (launcher, side) -> launcher.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.SILO_CONTROLLER.get(), (controller, side) -> controller.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.RADAR.get(), (radar, side) -> radar.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.INTERCEPTOR.get(), (interceptor, side) -> interceptor.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.COMBUSTION_GENERATOR.get(), (generator, side) -> generator.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.SOLAR_PANEL.get(), (panel, side) -> panel.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.CHARGING_STATION.get(), (station, side) -> station.energy());
        for (Machine machine : Machine.values()) {
            event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.machine(machine), (entity, side) -> entity.energy());
        }
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, blockEntity, side) -> level instanceof ServerLevel serverLevel && side != null
                ? CableNetworks.handler(serverLevel, pos, side)
                : null, ModBlocks.FE_CABLE.get());
    }

    private ModCapabilities() {
    }
}
