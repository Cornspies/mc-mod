package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.ReloadPayload;
import com.cornspies.fusionfactions.weapon.TriggerPayload;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

// Weapon input. The trigger is vanilla's attack key (D37), so it follows the player's key binding; with
// a weapon in hand it fires instead of hitting or mining. Reload and scope zoom have their own keys.
public final class WeaponKeys {
    static final KeyMapping.Category CATEGORY = new KeyMapping.Category(FusionFactions.id("weapons"));
    // 26.3 uses SDL key codes: always use the InputConstants names, never raw numbers.
    public static final KeyMapping RELOAD = new KeyMapping("key.fusionfactions.reload", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);

    private static boolean triggerHeld;
    private static boolean triggerPulled;
    private static int triggerHeldTicks;

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(RELOAD);
        event.register(WeaponAim.ZOOM);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        while (RELOAD.consumeClick()) {
            if (holdsWeapon(player)) {
                ClientPacketDistributor.sendToServer(ReloadPayload.INSTANCE);
            }
        }

        if (player == null) {
            triggerHeld = false;
            triggerPulled = false;
            return;
        }
        // Tell the server only about changes; it fires on its own ticks while the trigger is held.
        boolean down = minecraft.gui.screen() == null && minecraft.options.keyAttack.isDown() && holdsWeapon(player) && !player.isSpectator();
        triggerPulled = down && !triggerHeld;
        if (down != triggerHeld) {
            ClientPacketDistributor.sendToServer(new TriggerPayload(down));
            triggerHeldTicks = 0;
        } else if (down) {
            triggerHeldTicks++;
        }
        triggerHeld = down;
    }

    // With a weapon in hand, the attack button doesn't hit, mine or swing.
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && holdsWeapon(Minecraft.getInstance().player)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static boolean holdsWeapon(Player player) {
        return player != null && player.getMainHandItem().getItem() instanceof WeaponItem;
    }

    public static boolean triggerHeld() {
        return triggerHeld;
    }

    // The trigger was pulled this tick.
    public static boolean triggerPulled() {
        return triggerPulled;
    }

    public static int triggerHeldTicks() {
        return triggerHeldTicks;
    }

    private WeaponKeys() {
    }
}
