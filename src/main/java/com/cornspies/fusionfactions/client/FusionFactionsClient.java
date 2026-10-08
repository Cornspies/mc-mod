package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.client.datagen.DataGenerators;
import com.cornspies.fusionfactions.registry.ModEntityTypes;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// Client entry point. This class is not loaded on dedicated servers, so client-only code is safe here.
@Mod(value = FusionFactions.MODID, dist = Dist.CLIENT)
public class FusionFactionsClient {
    public FusionFactionsClient(IEventBus modEventBus, ModContainer container) {
        // Config screen under Mods > Fusion Factions > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(DataGenerators::gatherData);
        modEventBus.addListener(FusionFactionsClient::registerRenderers);
        modEventBus.addListener(WeaponKeys::register);
        modEventBus.addListener(FusionFactionsClient::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(WeaponKeys::onClientTick);
        NeoForge.EVENT_BUS.addListener(WeaponClientEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(SniperScope::onClientTick);
        NeoForge.EVENT_BUS.addListener(SniperScope::onComputeFov);
        NeoForge.EVENT_BUS.addListener(SniperScope::onPlayerTurn);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.FALLING_CRATE.get(), FallingCrateRenderer::new);
        // Bullets are drawn as a particle trail by the entity itself.
        event.registerEntityRenderer(ModEntityTypes.BULLET.get(), NoopRenderer::new);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, FusionFactions.id("scope"), SniperScope::renderOverlay);
        event.registerAbove(VanillaGuiLayers.HOTBAR, FusionFactions.id("ammo"), AmmoHud::render);
    }
}
