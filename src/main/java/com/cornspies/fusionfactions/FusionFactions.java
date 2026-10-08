package com.cornspies.fusionfactions;

import org.slf4j.Logger;

import com.cornspies.fusionfactions.config.ServerConfig;
import com.cornspies.fusionfactions.config.StartupConfig;
import com.cornspies.fusionfactions.gametest.UpgradeGameTests;
import com.cornspies.fusionfactions.gametest.WeaponGameTests;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModConditions;
import com.cornspies.fusionfactions.registry.ModCreativeTabs;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModEntityTypes;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModMenuTypes;
import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.supplydrop.SupplyDropCommands;
import com.cornspies.fusionfactions.supplydrop.SupplyDropManager;
import com.cornspies.fusionfactions.weapon.BulletEntity;
import com.cornspies.fusionfactions.weapon.GunshotPayload;
import com.cornspies.fusionfactions.weapon.ReloadPayload;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

// Common entry point. Must not reference anything in the client package.
@Mod(FusionFactions.MODID)
public class FusionFactions {
    // Must match mod_id in gradle.properties
    public static final String MODID = "fusionfactions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FusionFactions(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModConditions.CONDITION_CODECS.register(modEventBus);
        WeaponGameTests.TEST_FUNCTIONS.register(modEventBus);
        UpgradeGameTests.TEST_FUNCTIONS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SYNCED, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.STARTUP, StartupConfig.SPEC);

        modEventBus.addListener(SupplyDropManager::registerTicketController);
        modEventBus.addListener(ReloadPayload::register);
        modEventBus.addListener(GunshotPayload::register);
        modEventBus.addListener(WeaponGameTests::registerTests);
        NeoForge.EVENT_BUS.addListener(SupplyDropManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(SupplyDropCommands::register);
        NeoForge.EVENT_BUS.addListener(BulletEntity::onIncomingDamage);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
