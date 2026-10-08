package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.FusionFactions;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client to server: "the reload key was pressed". Intent only, no data (§3.2.1).
public record ReloadPayload() implements CustomPacketPayload {
    public static final ReloadPayload INSTANCE = new ReloadPayload();
    public static final CustomPacketPayload.Type<ReloadPayload> TYPE = new CustomPacketPayload.Type<>(FusionFactions.id("reload"));
    private static final StreamCodec<ByteBuf, ReloadPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void register(RegisterPayloadHandlersEvent event) {
        // Handlers run on the main server thread by default.
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ReloadPayload::handle);
    }

    private static void handle(ReloadPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            WeaponItem.tryStartReload(player);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
