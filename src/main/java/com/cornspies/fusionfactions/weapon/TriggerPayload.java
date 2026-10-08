package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.FusionFactions;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client to server: the trigger (attack button) was pressed or released while holding a weapon (D37).
// Intent only (§3.2.1): the server decides whether and when a shot is fired.
public record TriggerPayload(boolean pressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TriggerPayload> TYPE = new CustomPacketPayload.Type<>(FusionFactions.id("trigger"));
    private static final StreamCodec<ByteBuf, TriggerPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(TriggerPayload::new, TriggerPayload::pressed);

    public static void register(RegisterPayloadHandlersEvent event) {
        // Handlers run on the main server thread by default.
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, TriggerPayload::handle);
    }

    private static void handle(TriggerPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (payload.pressed) {
                Trigger.press(player);
            } else {
                Trigger.release(player);
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
