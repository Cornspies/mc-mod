package com.cornspies.fusionfactions.client;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeRules;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

// Aiming down sights (D38): while the use button is held with a weapon, in first person, the weapon moves
// to the middle of the view (WeaponItemExtensions), the crosshair goes away and the view zooms in a
// little, more with a sight (D40), which also shows its reticle. Scoped weapons (the sniper) show the
// scope instead, at 3x or 6x; the zoom key or the mouse wheel switches between them (D41).
// The client side of aiming is only visual; the server knows from the item use who aims (spread, speed).
public final class WeaponAim {
    private static final int[] SCOPE_LEVELS = { 3, 6 };
    // Ticks to raise the weapon to the eye.
    private static final int AIM_TICKS = 4;

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
    // Sight reticles: red dot and holographic ring in red, the 2x optic's chevron in amber.
    private static final int SIGHT_RED = 0xFFFF2A2A;
    private static final int SIGHT_RED_GLOW = 0x55FF2A2A;
    private static final int SIGHT_AMBER = 0xFFFFB428;

    public static final KeyMapping ZOOM = new KeyMapping("key.fusionfactions.zoom", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, WeaponKeys.CATEGORY);

    private static int scopeLevel;
    private static float progress;
    private static float previousProgress;
    private static boolean scoped;
    private static float scopeScale = OPEN_SCALE;
    // Visual kick of the weapon model when firing, decays every tick.
    private static float kick;
    private static float previousKick;

    // The weapon being aimed with, or null. Only in first person.
    private static @Nullable ItemStack aimedWeapon() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson() || !player.isUsingItem()) {
            return null;
        }
        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof WeaponItem && stack == player.getMainHandItem() ? stack : null;
    }

    private static boolean isScopedWeapon(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof WeaponItem weapon && weapon.baseStats().scoped();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ItemStack weapon = aimedWeapon();
        previousProgress = progress;
        progress = Mth.clamp(progress + (weapon != null ? 1.0F : -1.0F) / AIM_TICKS, 0.0F, 1.0F);
        previousKick = kick;
        kick *= 0.5F;

        boolean nowScoped = isScopedWeapon(weapon) && progress >= 1.0F;
        LocalPlayer player = Minecraft.getInstance().player;
        if (nowScoped != scoped && player != null) {
            player.playSound(nowScoped ? SoundEvents.SPYGLASS_USE : SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
        }
        scoped = nowScoped;
        while (ZOOM.consumeClick()) {
            if (isScopedWeapon(weapon)) {
                setScopeLevel(1 - scopeLevel);
            }
        }
    }

    private static void setScopeLevel(int level) {
        if (level == scopeLevel) {
            return;
        }
        scopeLevel = level;
        if (level == 1) {
            // A short kick when zooming further in.
            scopeScale = Math.min(scopeScale, LEVEL_CHANGE_SCALE);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(SoundEvents.SPYGLASS_USE, 0.6F, level == 1 ? 1.2F : 1.0F);
        }
    }

    // While aiming a scoped weapon, the mouse wheel switches the zoom instead of the hotbar slot.
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (isScopedWeapon(aimedWeapon()) && event.getScrollDeltaY() != 0.0) {
            setScopeLevel(event.getScrollDeltaY() > 0.0 ? 1 : 0);
            event.setCanceled(true);
        }
    }

    // How far the weapon is raised to the eye, 0 to 1.
    public static float progress(float partialTick) {
        return Mth.lerp(partialTick, previousProgress, progress);
    }

    public static float kick(float partialTick) {
        return Mth.lerp(partialTick, previousKick, kick);
    }

    public static void onShot() {
        kick = 1.0F;
    }

    public static boolean isScoped() {
        return scoped;
    }

    // Current zoom, used for the field of view, mouse speed and recoil.
    public static float zoom() {
        ItemStack weapon = aimedWeapon();
        if (weapon == null) {
            return 1.0F;
        }
        if (isScopedWeapon(weapon)) {
            return scoped ? SCOPE_LEVELS[scopeLevel] : 1.0F;
        }
        return ((WeaponItem) weapon.getItem()).stats(weapon).aimZoom();
    }

    // The sight on the weapon, 0 for none (or one the server doesn't allow on this weapon).
    public static int sightTier(ItemStack stack) {
        return UpgradeRules.accepts(stack.getItem(), UpgradeType.SIGHT) ? WeaponUpgrades.of(stack).tier(UpgradeType.SIGHT) : 0;
    }

    // Vanilla eases the field of view towards this value, which animates the zoom.
    public static void onComputeFov(ComputeFovModifierEvent event) {
        float zoom = zoom();
        if (zoom != 1.0F) {
            event.setNewFovModifier(event.getNewFovModifier() / zoom);
        }
    }

    // Slower mouse while zoomed, so the view moves at the same on-screen speed. Turn speed is
    // (sensitivity * 0.6 + 0.2)^3 * 8 (MouseHandler), so the sensitivity is scaled through a cube root.
    public static void onPlayerTurn(CalculatePlayerTurnEvent event) {
        float zoom = zoom();
        if (zoom != 1.0F) {
            double base = event.getMouseSensitivity() * 0.6 + 0.2;
            double scaled = base / Math.cbrt(zoom);
            event.setMouseSensitivity(Math.max(0.0, (scaled - 0.2) / 0.6));
        }
    }

    // Aiming replaces the crosshair (the sights or the reticle show where shots go); the scope also
    // replaces the weapon in hand.
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (progress > 0.5F && event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (scoped) {
            event.setCanceled(true);
        }
    }

    public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden()) {
            scopeScale = OPEN_SCALE;
            return;
        }
        if (scoped) {
            renderScope(minecraft, graphics, deltaTracker);
            return;
        }
        scopeScale = OPEN_SCALE;
        ItemStack weapon = aimedWeapon();
        if (weapon != null && progress(deltaTracker.getGameTimeDeltaPartialTick(false)) > 0.9F) {
            int tier = sightTier(weapon);
            if (tier > 0) {
                renderSightReticle(graphics, tier, graphics.guiWidth() / 2, graphics.guiHeight() / 2);
            }
        }
    }

    // Red dot, holographic ring with a dot, or the optic's chevron, on the middle of the screen.
    private static void renderSightReticle(GuiGraphicsExtractor graphics, int tier, int x, int y) {
        switch (tier) {
            case 1 -> {
                graphics.fill(RenderPipelines.GUI, x - 1, y - 1, x + 2, y + 2, SIGHT_RED_GLOW);
                graphics.fill(RenderPipelines.GUI, x, y, x + 1, y + 1, SIGHT_RED);
            }
            case 2 -> {
                int radius = 7;
                for (int step = 0; step < 48; step++) {
                    double angle = step * Math.PI * 2.0 / 48.0;
                    int px = x + (int) Math.round(Math.cos(angle) * radius);
                    int py = y + (int) Math.round(Math.sin(angle) * radius);
                    graphics.fill(RenderPipelines.GUI, px, py, px + 1, py + 1, SIGHT_RED);
                }
                graphics.fill(RenderPipelines.GUI, x, y, x + 1, y + 1, SIGHT_RED);
            }
            default -> {
                for (int i = 0; i <= 4; i++) {
                    graphics.fill(RenderPipelines.GUI, x - i, y + i, x - i + 1, y + i + 1, SIGHT_AMBER);
                    graphics.fill(RenderPipelines.GUI, x + i, y + i, x + i + 1, y + i + 1, SIGHT_AMBER);
                }
                graphics.fill(RenderPipelines.GUI, x - 16, y, x - 7, y + 1, RETICLE_COLOR);
                graphics.fill(RenderPipelines.GUI, x + 8, y, x + 17, y + 1, RETICLE_COLOR);
                graphics.fill(RenderPipelines.GUI, x, y + 8, x + 1, y + 17, RETICLE_COLOR);
            }
        }
    }

    private static void renderScope(Minecraft minecraft, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        // Same easing as the vanilla spyglass, towards full size.
        scopeScale = Mth.lerp(Math.min(1.0F, 0.5F * deltaTracker.getGameTimeDeltaTicks()), scopeScale, 1.0F);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int size = Mth.floor(Math.min(width, height) * SCOPE_SIZE * scopeScale);
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
        renderScopeReticle(graphics, centerX, centerY, radius);

        Component label = Component.translatable("hud.fusionfactions.zoom", SCOPE_LEVELS[scopeLevel]);
        int labelX = centerX + radius + Math.max(6, radius / 12);
        if (labelX + minecraft.font.width(label) < width) {
            graphics.text(minecraft.font, label, labelX, centerY - minecraft.font.lineHeight / 2, LABEL_COLOR, false);
        }
    }

    // Duplex mil-dot reticle: thin lines through the middle, thick posts from the left, right and bottom
    // edges, dots along the thin part and a red dot in the center. Drawn in GUI pixels to stay sharp.
    private static void renderScopeReticle(GuiGraphicsExtractor graphics, int x, int y, int radius) {
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

    private WeaponAim() {
    }
}
