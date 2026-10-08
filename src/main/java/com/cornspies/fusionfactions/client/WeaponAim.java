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
// little. Once it's raised, a sight (D40) replaces the weapon with its sight picture: a thin frame with
// a glowing reticle (red dot, holographic), or a scope view (2x optic). Scoped weapons (the sniper) show
// their scope at 3x or 6x; the zoom key or the mouse wheel switches between them (D41).
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
    // Sight pictures (D40). Glowing reticles from tools/scope_overlay.py.
    private static final Identifier RED_DOT = FusionFactions.id("textures/gui/sight_red_dot.png");
    private static final Identifier HOLOGRAPHIC = FusionFactions.id("textures/gui/sight_holo.png");
    private static final Identifier CHEVRON = FusionFactions.id("textures/gui/sight_chevron.png");
    private static final int SIGHT_FRAME = 0xF0202328;
    private static final int SIGHT_GLASS = 0x1466B8FF;
    private static final int[] TIER_ACCENT = { 0xFF4678FF, 0xFFF0961E, 0xFFE63228 };
    // The optic's scope view is a little smaller than the sniper's.
    private static final float OPTIC_SIZE = 0.9F;
    // Frames start a bit larger and settle into place when the sight comes up.
    private static final float FRAME_OPEN_SCALE = 1.25F;

    // What replaces the weapon once it's raised to the eye.
    private enum Overlay {
        NONE, RED_DOT, HOLOGRAPHIC, OPTIC, SCOPE
    }

    public static final KeyMapping ZOOM = new KeyMapping("key.fusionfactions.zoom", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, WeaponKeys.CATEGORY);

    private static int scopeLevel;
    private static float progress;
    private static float previousProgress;
    private static Overlay overlay = Overlay.NONE;
    private static float overlayScale = OPEN_SCALE;
    // Visual kick of the weapon model when firing, decays every tick.
    private static float kick;
    private static float previousKick;

    // The weapon being aimed with, or null. Only in first person.
    private static @Nullable ItemStack aimedWeapon() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof WeaponItem && WeaponItem.isAiming(player, stack) ? stack : null;
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

        Overlay now = overlayFor(weapon);
        if (now != overlay) {
            LocalPlayer player = Minecraft.getInstance().player;
            boolean scopeNow = now == Overlay.SCOPE || now == Overlay.OPTIC;
            boolean scopeBefore = overlay == Overlay.SCOPE || overlay == Overlay.OPTIC;
            if (player != null && scopeNow != scopeBefore) {
                player.playSound(scopeNow ? SoundEvents.SPYGLASS_USE : SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
            }
            overlayScale = now == Overlay.RED_DOT || now == Overlay.HOLOGRAPHIC ? FRAME_OPEN_SCALE : OPEN_SCALE;
            overlay = now;
        }
        while (ZOOM.consumeClick()) {
            if (isScopedWeapon(weapon)) {
                setScopeLevel(1 - scopeLevel);
            }
        }
    }

    private static Overlay overlayFor(@Nullable ItemStack weapon) {
        if (weapon == null || progress < 1.0F) {
            return Overlay.NONE;
        }
        if (isScopedWeapon(weapon)) {
            return Overlay.SCOPE;
        }
        return switch (sightTier(weapon)) {
            case 1 -> Overlay.RED_DOT;
            case 2 -> Overlay.HOLOGRAPHIC;
            case 3 -> Overlay.OPTIC;
            default -> Overlay.NONE;
        };
    }

    private static void setScopeLevel(int level) {
        if (level == scopeLevel) {
            return;
        }
        scopeLevel = level;
        if (level == 1) {
            // A short kick when zooming further in.
            overlayScale = Math.min(overlayScale, LEVEL_CHANGE_SCALE);
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


    // Current zoom, used for the field of view, mouse speed and recoil.
    public static float zoom() {
        ItemStack weapon = aimedWeapon();
        if (weapon == null) {
            return 1.0F;
        }
        if (isScopedWeapon(weapon)) {
            return overlay == Overlay.SCOPE ? SCOPE_LEVELS[scopeLevel] : 1.0F;
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

    // Aiming replaces the crosshair (the sights or the reticle show where shots go); a sight picture or
    // scope also replaces the weapon in hand.
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (progress > 0.5F && event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (overlay != Overlay.NONE) {
            event.setCanceled(true);
        }
    }

    public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || overlay == Overlay.NONE) {
            return;
        }
        // Same easing as the vanilla spyglass, towards full size.
        overlayScale = Mth.lerp(Math.min(1.0F, 0.5F * deltaTracker.getGameTimeDeltaTicks()), overlayScale, 1.0F);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // The reticle's centre is the middle of the screen, where bullets go.
        int x = width / 2;
        int y = height / 2;
        switch (overlay) {
            case SCOPE -> renderScope(minecraft, graphics, SCOPE_SIZE);
            case OPTIC -> {
                renderScope(minecraft, graphics, OPTIC_SIZE);
                renderOpticReticle(graphics, x, y, height);
            }
            case RED_DOT -> {
                renderSightFrame(graphics, x, y, height, 0.66F, 0.48F, 1, false);
                blitCentered(graphics, RED_DOT, x, y, Math.round(height * 0.22F));
            }
            case HOLOGRAPHIC -> {
                renderSightFrame(graphics, x, y, height, 0.82F, 0.56F, 2, true);
                blitCentered(graphics, HOLOGRAPHIC, x, y, Math.round(height * 0.33F));
            }
            default -> {
            }
        }
    }

    // A sight seen from behind: a thin frame around a large, faintly tinted window on a short base with an
    // accent in the tier color. It scales with the screen and settles into place; it leaves most of the
    // view free.
    private static void renderSightFrame(GuiGraphicsExtractor graphics, int x, int y, int height, float widthShare, float heightShare,
            int tier, boolean hood) {
        int halfWidth = Math.round(height * widthShare * overlayScale / 2.0F);
        int halfHeight = Math.round(height * heightShare * overlayScale / 2.0F);
        int frame = Math.max(2, Math.round(height / 80.0F * overlayScale));
        int top = hood ? frame * 3 : frame;
        int left = x - halfWidth;
        int right = x + halfWidth;
        int upper = y - halfHeight;
        int lower = y + halfHeight;
        graphics.fill(RenderPipelines.GUI, left, upper, right, lower, SIGHT_GLASS);
        graphics.fill(RenderPipelines.GUI, left - frame, upper - top, right + frame, upper, SIGHT_FRAME);
        graphics.fill(RenderPipelines.GUI, left - frame, upper, left, lower, SIGHT_FRAME);
        graphics.fill(RenderPipelines.GUI, right, upper, right + frame, lower, SIGHT_FRAME);
        // A short base under the window.
        int baseHalf = Math.round(halfWidth * 0.8F);
        graphics.fill(RenderPipelines.GUI, left - frame, lower, right + frame, lower + frame, SIGHT_FRAME);
        graphics.fill(RenderPipelines.GUI, x - baseHalf, lower + frame, x + baseHalf, lower + frame * 4, SIGHT_FRAME);
        graphics.fill(RenderPipelines.GUI, x - baseHalf + frame, lower + frame * 2, x + baseHalf - frame, lower + frame * 2 + Math.max(1, frame / 2),
                TIER_ACCENT[tier - 1]);
    }

    // The 2x optic: an amber chevron whose tip marks the aim point, a thin post below it with range
    // marks, and thin lines in from the sides.
    private static void renderOpticReticle(GuiGraphicsExtractor graphics, int x, int y, int height) {
        int lens = Math.round(Math.min(graphics.guiWidth(), height) * OPTIC_SIZE * overlayScale * LENS_RADIUS / 2.0F);
        int chevron = Math.round(height * 0.3F);
        blitCentered(graphics, CHEVRON, x, y, chevron);
        int postTop = y + Math.round(chevron * 0.2F);
        graphics.fill(RenderPipelines.GUI, x, postTop, x + 1, y + lens, RETICLE_COLOR);
        int step = Math.max(4, Math.round(height * 0.04F));
        for (int mark = postTop + step; mark < y + lens * 0.7F; mark += step) {
            graphics.fill(RenderPipelines.GUI, x - 2, mark, x + 3, mark + 1, RETICLE_COLOR);
        }
        int inner = Math.round(lens * 0.35F);
        graphics.fill(RenderPipelines.GUI, x - lens, y, x - inner, y + 1, RETICLE_COLOR);
        graphics.fill(RenderPipelines.GUI, x + inner + 1, y, x + lens + 1, y + 1, RETICLE_COLOR);
    }

    // Draws a square texture centred on a pixel. Reticle textures are blurred (see their .mcmeta) so they
    // scale smoothly.
    private static void blitCentered(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size) {
        int left = x - size / 2;
        int top = y - size / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0.0F, 0.0F, size, size, size, size);
    }

    // The scope view: black around a round lens. For the sniper, its mil-dot reticle and the zoom level.
    private static void renderScope(Minecraft minecraft, GuiGraphicsExtractor graphics, float sizeShare) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int size = Mth.floor(Math.min(width, height) * sizeShare * overlayScale);
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
        if (overlay != Overlay.SCOPE) {
            return;
        }

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
