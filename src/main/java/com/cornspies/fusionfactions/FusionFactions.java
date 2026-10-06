package com.cornspies.fusionfactions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// Common entry point. Must not reference anything in the client package.
@Mod(FusionFactions.MODID)
public class FusionFactions {
    // Must match mod_id in gradle.properties
    public static final String MODID = "fusionfactions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FusionFactions(IEventBus modEventBus, ModContainer modContainer) {
    }
}
