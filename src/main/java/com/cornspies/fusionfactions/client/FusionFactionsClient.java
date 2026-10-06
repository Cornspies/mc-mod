package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.FusionFactions;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Client entry point. This class is not loaded on dedicated servers, so client-only code is safe here.
@Mod(value = FusionFactions.MODID, dist = Dist.CLIENT)
public class FusionFactionsClient {
    public FusionFactionsClient(ModContainer container) {
        // Config screen under Mods > Fusion Factions > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
