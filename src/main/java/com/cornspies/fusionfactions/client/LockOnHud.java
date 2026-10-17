package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.weapon.LockStatePayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3fc;

// The shooter's side of locking on (DESIGN.md §6b.6), from what the server sends (LockStatePayload): brackets close in
// on the target while the launcher seeks, with beeps that speed up; once locked, the brackets turn red with a steady
// fast tone. Purely a display, the server decides the lock.
public final class LockOnHud {
    private static final int SEEKING_COLOR = 0xFFFFB000;
    private static final int LOCKED_COLOR = 0xFFFF3030;
    // While seeking, the brackets start this many times the target's size and close in as the lock builds.
    private static final float START_SCALE = 2.5F;
    private static final int MIN_HALF_SIZE = 8;
    private static final int BRACKET = 5;

    private static int targetId = -1;
    private static int progress;
    private static int toneTimer;

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(LockStatePayload.TYPE, LockOnHud::handle);
    }

    private static void handle(LockStatePayload payload, IPayloadContext context) {
        boolean wasLocked = isLocked();
        targetId = payload.targetId();
        progress = payload.progress();
        if (isLocked() && !wasLocked) {
            toneTimer = 0;
        }
    }

    private static boolean isLocked() {
        return targetId >= 0 && progress >= 100;
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        targetId = -1;
        progress = 0;
    }

    // Seeking: a beep that gets faster and higher as the lock builds. Locked: a fast high tone.
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || targetId < 0 || minecraft.isPaused()) {
            return;
        }
        if (toneTimer-- > 0) {
            return;
        }
        if (isLocked()) {
            play(minecraft, ModSounds.LOCK_LOCKED.get(), 1.0F);
            toneTimer = 2;
        } else {
            play(minecraft, ModSounds.LOCK_SEEKING.get(), 1.0F + progress / 200.0F);
            toneTimer = Math.max(2, 10 - progress / 12);
        }
    }

    static void play(Minecraft minecraft, SoundEvent sound, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.6F));
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.hud.isHidden() || targetId < 0) {
            return;
        }
        Entity target = minecraft.level.getEntity(targetId);
        if (target == null) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        Vec3 feet = target.getPosition(partialTick);
        Vec3 head = feet.add(0.0, target.getBbHeight(), 0.0);
        int[] bottom = toScreen(minecraft, graphics, feet);
        int[] top = toScreen(minecraft, graphics, head);
        if (bottom == null || top == null) {
            return;
        }
        int x = (top[0] + bottom[0]) / 2;
        int y = (top[1] + bottom[1]) / 2;
        int half = Math.max(MIN_HALF_SIZE, Math.round(Math.abs(bottom[1] - top[1]) * 0.6F));
        boolean locked = isLocked();
        if (!locked) {
            half = Math.round(half * (START_SCALE - (START_SCALE - 1.0F) * progress / 100.0F));
        }
        int color = locked ? LOCKED_COLOR : SEEKING_COLOR;
        brackets(graphics, x, y, half, color);
        if (locked) {
            graphics.centeredText(minecraft.font, Component.translatable("hud.fusionfactions.lock.locked"), x, y + half + 3, color);
        } else {
            graphics.centeredText(minecraft.font, Component.translatable("hud.fusionfactions.lock.seeking", progress), x, y + half + 3, color);
        }
    }

    // Four corner brackets around the point.
    private static void brackets(GuiGraphicsExtractor graphics, int x, int y, int half, int color) {
        int l = x - half;
        int r = x + half;
        int t = y - half;
        int b = y + half;
        graphics.fill(l, t, l + BRACKET, t + 1, color);
        graphics.fill(l, t, l + 1, t + BRACKET, color);
        graphics.fill(r - BRACKET, t, r, t + 1, color);
        graphics.fill(r - 1, t, r, t + BRACKET, color);
        graphics.fill(l, b - 1, l + BRACKET, b, color);
        graphics.fill(l, b - BRACKET, l + 1, b, color);
        graphics.fill(r - BRACKET, b - 1, r, b, color);
        graphics.fill(r - 1, b - BRACKET, r, b, color);
    }

    // Where a point in the world is on the HUD, or null if it's behind the camera.
    static int[] toScreen(Minecraft minecraft, GuiGraphicsExtractor graphics, Vec3 point) {
        var camera = minecraft.gameRenderer.mainCamera();
        Vector3fc forward = camera.forwardVector();
        Vec3 offset = point.subtract(camera.position());
        if (offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() <= 0.1) {
            return null;
        }
        Vec3 ndc = minecraft.gameRenderer.projectPointToScreen(point);
        int x = Math.round((float) (ndc.x + 1.0) * 0.5F * graphics.guiWidth());
        int y = Math.round((float) (1.0 - ndc.y) * 0.5F * graphics.guiHeight());
        return new int[] {x, y};
    }

    private LockOnHud() {
    }
}
