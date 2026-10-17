package com.cornspies.fusionfactions;

import org.slf4j.Logger;

import com.cornspies.fusionfactions.config.ServerConfig;
import com.cornspies.fusionfactions.config.StartupConfig;
import com.cornspies.fusionfactions.gametest.CrateLootGameTests;
import com.cornspies.fusionfactions.gametest.MachineGameTests;
import com.cornspies.fusionfactions.gametest.MissileGameTests;
import com.cornspies.fusionfactions.gametest.PowerGameTests;
import com.cornspies.fusionfactions.gametest.ResourceGameTests;
import com.cornspies.fusionfactions.gametest.RocketGameTests;
import com.cornspies.fusionfactions.gametest.TriggerGameTests;
import com.cornspies.fusionfactions.gametest.UpgradeGameTests;
import com.cornspies.fusionfactions.gametest.WeaponGameTests;
import com.cornspies.fusionfactions.missile.MissileCommands;
import com.cornspies.fusionfactions.missile.MissileManager;
import com.cornspies.fusionfactions.registry.ModAttachments;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModCapabilities;
import com.cornspies.fusionfactions.registry.ModConditions;
import com.cornspies.fusionfactions.registry.ModCreativeTabs;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModEntityTypes;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModMenuTypes;
import com.cornspies.fusionfactions.registry.ModRecipes;
import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.supplydrop.SupplyDropCommands;
import com.cornspies.fusionfactions.supplydrop.SupplyDropManager;
import com.cornspies.fusionfactions.weapon.BulletEntity;
import com.cornspies.fusionfactions.weapon.GunshotPayload;
import com.cornspies.fusionfactions.weapon.LockOn;
import com.cornspies.fusionfactions.weapon.LockStatePayload;
import com.cornspies.fusionfactions.weapon.ReloadPayload;
import com.cornspies.fusionfactions.weapon.ThreatPayload;
import com.cornspies.fusionfactions.weapon.Trigger;
import com.cornspies.fusionfactions.weapon.TriggerPayload;
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
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModConditions.CONDITION_CODECS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModRecipes.BOOK_CATEGORIES.register(modEventBus);
        WeaponGameTests.TEST_FUNCTIONS.register(modEventBus);
        UpgradeGameTests.TEST_FUNCTIONS.register(modEventBus);
        TriggerGameTests.TEST_FUNCTIONS.register(modEventBus);
        CrateLootGameTests.TEST_FUNCTIONS.register(modEventBus);
        MissileGameTests.TEST_FUNCTIONS.register(modEventBus);
        ResourceGameTests.TEST_FUNCTIONS.register(modEventBus);
        PowerGameTests.TEST_FUNCTIONS.register(modEventBus);
        MachineGameTests.TEST_FUNCTIONS.register(modEventBus);
        RocketGameTests.TEST_FUNCTIONS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SYNCED, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.STARTUP, StartupConfig.SPEC);

        modEventBus.addListener(SupplyDropManager::registerTicketController);
        modEventBus.addListener(MissileManager::registerTicketController);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(ReloadPayload::register);
        modEventBus.addListener(GunshotPayload::register);
        modEventBus.addListener(TriggerPayload::register);
        modEventBus.addListener(LockStatePayload::register);
        modEventBus.addListener(ThreatPayload::register);
        modEventBus.addListener(WeaponGameTests::registerTests);
        NeoForge.EVENT_BUS.addListener(SupplyDropManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(SupplyDropCommands::register);
        NeoForge.EVENT_BUS.addListener(MissileManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(MissileCommands::register);
        NeoForge.EVENT_BUS.addListener(BulletEntity::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(Trigger::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LockOn::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ModRecipes::sendToClients);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
