package com.cornspies.fusionfactions.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// FE for the missile blocks (D47, D59): any side, so other mods' cables can power them.
public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.MISSILE_LAUNCHER.get(), (launcher, side) -> launcher.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntityTypes.SILO_CONTROLLER.get(), (controller, side) -> controller.energy());
    }

    private ModCapabilities() {
    }
}
