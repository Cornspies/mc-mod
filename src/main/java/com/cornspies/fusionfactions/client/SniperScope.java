package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

// Zoom for scoped weapons (D28): the zoom key cycles off, 3x and 6x while one is held in first person.
// Purely client-side: zoom changes what the player sees, not how the weapon fires.
public final class SniperScope {
    private static final float[] ZOOM_LEVELS = { 1.0F, 3.0F, 6.0F };

    public static final KeyMapping ZOOM = new KeyMapping("key.fusionfactions.zoom", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, WeaponKeys.CATEGORY);

    private static int level;

    public static float zoom() {
        return ZOOM_LEVELS[level];
    }

    public static boolean isZoomed() {
        return level > 0;
    }

    private static boolean canZoom(Minecraft minecraft, Player player) {
        return minecraft.options.getCameraType().isFirstPerson()
                && player.getMainHandItem().getItem() instanceof WeaponItem weapon && weapon.stats().scoped();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !canZoom(minecraft, player)) {
            // Switching items or camera ends the zoom; key presses meanwhile are dropped.
            level = 0;
            while (ZOOM.consumeClick()) {
            }
            return;
        }
        while (ZOOM.consumeClick()) {
            level = (level + 1) % ZOOM_LEVELS.length;
            player.playSound(isZoomed() ? SoundEvents.SPYGLASS_USE : SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
        }
    }

    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (isZoomed()) {
            event.setNewFovModifier(event.getNewFovModifier() / zoom());
        }
    }

    // Slower mouse while zoomed, so the view moves at the same on-screen speed. Turn speed is
    // (sensitivity * 0.6 + 0.2)^3 * 8 (MouseHandler), so the sensitivity is scaled through a cube root.
    public static void onPlayerTurn(CalculatePlayerTurnEvent event) {
        if (isZoomed()) {
            double base = event.getMouseSensitivity() * 0.6 + 0.2;
            double scaled = base / Math.cbrt(zoom());
            event.setMouseSensitivity(Math.max(0.0, (scaled - 0.2) / 0.6));
        }
    }

    // The vanilla spyglass scope, drawn the same way Hud does for the spyglass.
    public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isZoomed() || minecraft.player == null || minecraft.gui.hud.isHidden()) {
            return;
        }
        int size = Mth.floor(Math.min(graphics.guiWidth(), graphics.guiHeight()) * 1.125F);
        int left = (graphics.guiWidth() - size) / 2;
        int top = (graphics.guiHeight() - size) / 2;
        int right = left + size;
        int bottom = top + size;
        graphics.blit(RenderPipelines.GUI_TEXTURED, Hud.SPYGLASS_SCOPE_LOCATION, left, top, 0.0F, 0.0F, size, size, size, size);
        graphics.fill(RenderPipelines.GUI, 0, bottom, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
        graphics.fill(RenderPipelines.GUI, 0, 0, graphics.guiWidth(), top, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, 0, top, left, bottom, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, right, top, graphics.guiWidth(), bottom, 0xFF000000);
    }

    private SniperScope() {
    }
}
