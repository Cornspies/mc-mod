package com.cornspies.fusionfactions.client;

import java.util.EnumMap;
import java.util.Map;

import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.weapon.ThreatPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// The lock warning (D62, DESIGN.md §6b.7) for a player with warning gear, from the server's ThreatPayloads: a warning
// at the top of the screen, an arrow around the crosshair pointing towards the threat, and a tone that gets faster
// from "seeking" to "locked" to "rocket incoming". A threat the server stops reporting fades after a moment.
public final class ThreatWarningHud {
    // The server repeats a threat every 4 ticks; one missing for this long is over.
    private static final int EXPIRE_TICKS = 12;
    private static final int ARROW_RADIUS = 34;
    private static final int SEEKING_COLOR = 0xFFFFB000;
    private static final int DANGER_COLOR = 0xFFFF3030;
    private static final int TEXT_Y = 40;

    private record Threat(Vec3 from, long expires) {
    }

    private static final Map<ThreatPayload.Kind, Threat> THREATS = new EnumMap<>(ThreatPayload.Kind.class);
    private static long ticks;
    private static int toneTimer;

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(ThreatPayload.TYPE, ThreatWarningHud::handle);
    }

    private static void handle(ThreatPayload payload, IPayloadContext context) {
        boolean worse = worst() == null || worst().ordinal() < payload.kind().ordinal();
        THREATS.put(payload.kind(), new Threat(payload.from(), ticks + EXPIRE_TICKS));
        if (worse) {
            toneTimer = 0;
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        THREATS.clear();
    }

    private static ThreatPayload.Kind worst() {
        ThreatPayload.Kind worst = null;
        for (ThreatPayload.Kind kind : THREATS.keySet()) {
            if (worst == null || kind.ordinal() > worst.ordinal()) {
                worst = kind;
            }
        }
        return worst;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) {
            return;
        }
        ticks++;
        THREATS.values().removeIf(threat -> threat.expires() < ticks);
        ThreatPayload.Kind worst = worst();
        if (worst == null || minecraft.player == null || toneTimer-- > 0) {
            return;
        }
        switch (worst) {
            case SEEKING -> {
                LockOnHud.play(minecraft, ModSounds.WARNING_SEEKING.get(), 1.0F);
                toneTimer = 14;
            }
            case LOCKED -> {
                LockOnHud.play(minecraft, ModSounds.WARNING_LOCKED.get(), 1.0F);
                toneTimer = 5;
            }
            case INCOMING -> {
                LockOnHud.play(minecraft, ModSounds.WARNING_INCOMING.get(), 1.0F);
                toneTimer = 2;
            }
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ThreatPayload.Kind worst = worst();
        if (player == null || worst == null || minecraft.gui.hud.isHidden()) {
            return;
        }
        Threat threat = THREATS.get(worst);
        int color = worst == ThreatPayload.Kind.SEEKING ? SEEKING_COLOR : DANGER_COLOR;
        // Incoming rockets flash.
        boolean visible = worst != ThreatPayload.Kind.INCOMING || (ticks / 3) % 2 == 0;
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2;
        if (visible) {
            graphics.centeredText(minecraft.font, Component.translatable("hud.fusionfactions.warning." + worst.name().toLowerCase()), x, TEXT_Y, color);
        }
        // The arrow points towards the threat, seen from above: up is straight ahead.
        Vec3 to = threat.from().subtract(player.getEyePosition(deltaTracker.getGameTimeDeltaPartialTick(true)));
        float bearing = (float) Math.toDegrees(Mth.atan2(-to.x, to.z));
        float relative = Mth.wrapDegrees(bearing - player.getViewYRot(deltaTracker.getGameTimeDeltaPartialTick(true)));
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.rotate((float) Math.toRadians(relative));
        pose.translate(0.0F, -ARROW_RADIUS);
        // A triangle pointing outwards, from rows of shrinking width.
        for (int row = 0; row < 6; row++) {
            graphics.fill(-6 + row, -row, 6 - row, -row + 1, color);
        }
        pose.popMatrix();
    }

    private ThreatWarningHud() {
    }
}
