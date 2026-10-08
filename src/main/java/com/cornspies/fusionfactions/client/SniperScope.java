package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

// Zoom for scoped weapons (D28): the zoom key cycles off, 3x and 6x while one is held in first person.
// While zoomed, the screen shows a sniper scope: a round lens in black, with a duplex mil-dot reticle.
// Purely client-side: zoom changes what the player sees, not how the weapon fires.
public final class SniperScope {
    private static final int[] ZOOM_LEVELS = { 1, 3, 6 };

    // Black with a transparent round lens, from tools/scope_overlay.py.
    private static final Identifier SCOPE_TEXTURE = FusionFactions.id("textures/gui/sniper_scope.png");
    // Size of the scope texture as a share of the screen height (or width, if that is smaller).
    private static final float SCOPE_SIZE = 1.04F;
    // Radius of the visible lens as a share of half the texture (see EDGE_START in the script).
    private static final float LENS_RADIUS = 0.9F;
    // Scope-in animation: the scope grows from this scale to full size, a little more each frame.
    private static final float OPEN_SCALE = 0.6F;
    private static final float LEVEL_CHANGE_SCALE = 0.93F;
    // The thick outer posts start at this share of the lens radius. Mil-dots split the thin part in five.
    private static final float POST_START = 0.5F;
    private static final int MIL_DOTS = 4;
    private static final int RETICLE_COLOR = 0xFF000000;
    private static final int CENTER_DOT_COLOR = 0xFFE01B1B;
    private static final int LABEL_COLOR = 0xFF9A9A9A;

    public static final KeyMapping ZOOM = new KeyMapping("key.fusionfactions.zoom", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, WeaponKeys.CATEGORY);

    private static int level;
    private static float scale = OPEN_SCALE;

    public static float zoom() {
        return ZOOM_LEVELS[level];
    }

    public static boolean isZoomed() {
        return level > 0;
    }

    private static boolean canZoom(Minecraft minecraft, Player player) {
        return minecraft.options.getCameraType().isFirstPerson()
                && player.getMainHandItem().getItem() instanceof WeaponItem weapon && weapon.baseStats().scoped();
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
            if (level > 1) {
                // A short kick when zooming further in.
                scale = Math.min(scale, LEVEL_CHANGE_SCALE);
            }
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

    // The scope replaces the crosshair and the weapon in hand.
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (isZoomed() && event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (isZoomed()) {
            event.setCanceled(true);
        }
    }

    public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isZoomed() || minecraft.player == null || minecraft.gui.hud.isHidden()) {
            scale = OPEN_SCALE;
            return;
        }
        // Same easing as the vanilla spyglass, towards full size.
        scale = Mth.lerp(Math.min(1.0F, 0.5F * deltaTracker.getGameTimeDeltaTicks()), scale, 1.0F);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int size = Mth.floor(Math.min(width, height) * SCOPE_SIZE * scale);
        // The reticle's center pixel is the middle of the screen, where bullets go.
        int centerX = width / 2;
        int centerY = height / 2;
        int left = centerX - size / 2;
        int top = centerY - size / 2;
        int right = left + size;
        int bottom = top + size;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCOPE_TEXTURE, left, top, 0.0F, 0.0F, size, size, size, size);
        graphics.fill(RenderPipelines.GUI, 0, 0, width, top, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, 0, bottom, width, height, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, 0, top, left, bottom, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, right, top, width, bottom, 0xFF000000);

        int radius = Mth.floor(size / 2.0F * LENS_RADIUS);
        renderReticle(graphics, centerX, centerY, radius);

        Component label = Component.translatable("hud.fusionfactions.zoom", ZOOM_LEVELS[level]);
        int labelX = centerX + radius + Math.max(6, radius / 12);
        if (labelX + minecraft.font.width(label) < width) {
            graphics.text(minecraft.font, label, labelX, centerY - minecraft.font.lineHeight / 2, LABEL_COLOR, false);
        }
    }

    // Duplex mil-dot reticle: thin lines through the middle, thick posts from the left, right and bottom
    // edges, dots along the thin part and a red dot in the center. Drawn in GUI pixels to stay sharp.
    private static void renderReticle(GuiGraphicsExtractor graphics, int x, int y, int radius) {
        int postStart = Math.round(radius * POST_START);
        int postHalfWidth = Math.max(1, Math.round(radius / 70.0F));
        graphics.fill(RenderPipelines.GUI, x - radius, y, x + radius + 1, y + 1, RETICLE_COLOR);
        graphics.fill(RenderPipelines.GUI, x, y - radius, x + 1, y + radius + 1, RETICLE_COLOR);
        graphics.fill(RenderPipelines.GUI, x - radius, y - postHalfWidth, x - postStart, y + postHalfWidth + 1, RETICLE_COLOR);
        graphics.fill(RenderPipelines.GUI, x + postStart + 1, y - postHalfWidth, x + radius + 1, y + postHalfWidth + 1, RETICLE_COLOR);
        graphics.fill(RenderPipelines.GUI, x - postHalfWidth, y + postStart + 1, x + postHalfWidth + 1, y + radius + 1, RETICLE_COLOR);
        for (int i = 1; i <= MIL_DOTS; i++) {
            int offset = Math.round(postStart * i / (MIL_DOTS + 1.0F));
            dot(graphics, x - offset, y, RETICLE_COLOR);
            dot(graphics, x + offset, y, RETICLE_COLOR);
            dot(graphics, x, y - offset, RETICLE_COLOR);
            dot(graphics, x, y + offset, RETICLE_COLOR);
        }
        dot(graphics, x, y, CENTER_DOT_COLOR);
    }

    // A 3x3 dot centered on the given pixel.
    private static void dot(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(RenderPipelines.GUI, x - 1, y - 1, x + 2, y + 2, color);
    }

    private SniperScope() {
    }
}
