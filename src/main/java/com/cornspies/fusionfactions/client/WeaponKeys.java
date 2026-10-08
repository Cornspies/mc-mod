package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.ReloadPayload;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class WeaponKeys {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(FusionFactions.id("weapons"));
    // 26.3 uses SDL key codes: always use the InputConstants names, never raw numbers.
    public static final KeyMapping RELOAD = new KeyMapping("key.fusionfactions.reload", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(RELOAD);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        while (RELOAD.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player != null && player.getMainHandItem().getItem() instanceof WeaponItem) {
                ClientPacketDistributor.sendToServer(ReloadPayload.INSTANCE);
            }
        }
    }

    private WeaponKeys() {
    }
}
