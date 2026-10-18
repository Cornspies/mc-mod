package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.client.datagen.DataGenerators;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.power.MachineMenu;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModEntityTypes;
import com.cornspies.fusionfactions.registry.ModMenuTypes;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.common.NeoForge;

// Client entry point. This class is not loaded on dedicated servers, so client-only code is safe here.
@Mod(value = FusionFactions.MODID, dist = Dist.CLIENT)
public class FusionFactionsClient {
    public FusionFactionsClient(IEventBus modEventBus, ModContainer container) {
        // Config screen under Mods > Fusion Factions > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(DataGenerators::gatherData);
        modEventBus.addListener(DataGenerators::gatherRegistries);
        modEventBus.addListener(FusionFactionsClient::registerRenderers);
        modEventBus.addListener(FusionFactionsClient::registerStandaloneModels);
        modEventBus.addListener(WeaponKeys::register);
        modEventBus.addListener(FusionFactionsClient::registerGuiLayers);
        modEventBus.addListener(WeaponItemExtensions::register);
        modEventBus.addListener(GunshotSounds::register);
        modEventBus.addListener(LockOnHud::register);
        modEventBus.addListener(ThreatWarningHud::register);
        modEventBus.addListener(UpgradeTierProperty::register);
        modEventBus.addListener(AimData::register);
        modEventBus.addListener(FusionFactionsClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(ClientMachineRecipes::onRecipesReceived);
        NeoForge.EVENT_BUS.addListener(ClientMachineRecipes::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(WeaponKeys::onClientTick);
        NeoForge.EVENT_BUS.addListener(WeaponClientEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(WeaponKeys::onInteraction);
        NeoForge.EVENT_BUS.addListener(WeaponAim::onClientTick);
        NeoForge.EVENT_BUS.addListener(WeaponAim::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(WeaponAim::onComputeFov);
        NeoForge.EVENT_BUS.addListener(WeaponAim::onPlayerTurn);
        NeoForge.EVENT_BUS.addListener(WeaponAim::onRenderGuiLayer);
        NeoForge.EVENT_BUS.addListener(WeaponAim::onRenderHand);
        NeoForge.EVENT_BUS.addListener(LockOnHud::onClientTick);
        NeoForge.EVENT_BUS.addListener(LockOnHud::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ThreatWarningHud::onClientTick);
        NeoForge.EVENT_BUS.addListener(ThreatWarningHud::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(LoopingSounds::onEntityJoin);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.FALLING_CRATE.get(), FallingCrateRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MISSILE.get(), MissileRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.INTERCEPTOR_ROCKET.get(), RocketRenderer::interceptor);
        event.registerEntityRenderer(ModEntityTypes.GUIDED_ROCKET.get(), RocketRenderer::guided);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.RADAR.get(), RadarDishRenderer::new);
        // Bullets are drawn as a particle trail by the entity itself.
        event.registerEntityRenderer(ModEntityTypes.BULLET.get(), NoopRenderer::new);
    }

    // Models drawn by renderers rather than as a block or item (tools/machine_models.py).
    private static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        event.register(RadarDishRenderer.DISH, SimpleUnbakedStandaloneModel.simpleModelWrapper(FusionFactions.id("block/radar_dish")));
        event.register(RocketRenderer.INTERCEPTOR, SimpleUnbakedStandaloneModel.simpleModelWrapper(FusionFactions.id("entity/interceptor_rocket")));
        event.register(RocketRenderer.GUIDED, SimpleUnbakedStandaloneModel.simpleModelWrapper(FusionFactions.id("entity/guided_rocket")));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.WEAPON_WORKBENCH.get(), WeaponWorkbenchScreen::new);
        event.register(ModMenuTypes.MISSILE_LAUNCHER.get(), MissileLauncherScreen::new);
        event.register(ModMenuTypes.RADAR.get(), RadarScreen::new);
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenuTypes.CHARGING_STATION.get(), ChargingStationScreen::new);
        event.register(ModMenuTypes.INTERCEPTOR.get(), InterceptorScreen::new);
        for (Machine machine : Machine.values()) {
            event.register(ModMenuTypes.machine(machine),
                    (MachineMenu menu, Inventory inventory, Component title) -> new ProcessingMachineScreen(machine, menu, inventory, title));
        }
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, FusionFactions.id("scope"), WeaponAim::renderOverlay);
        event.registerAbove(VanillaGuiLayers.HOTBAR, FusionFactions.id("ammo"), AmmoHud::render);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, FusionFactions.id("lock_on"), LockOnHud::render);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, FusionFactions.id("threat_warning"), ThreatWarningHud::render);
    }
}
