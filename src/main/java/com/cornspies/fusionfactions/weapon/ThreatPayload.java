package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.FusionFactions;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Server to client: a threat to a player with warning gear (LockWarning): someone is locking on, has locked on, or a
// guided rocket is coming, and from where. Sent again every few ticks while it lasts; the client forgets a threat it
// hears nothing more about (client.ThreatWarningHud).
public record ThreatPayload(Kind kind, Vec3 from) implements CustomPacketPayload {
    public enum Kind {
        SEEKING,
        LOCKED,
        INCOMING
    }

    public static final CustomPacketPayload.Type<ThreatPayload> TYPE = new CustomPacketPayload.Type<>(FusionFactions.id("threat"));
    private static final StreamCodec<ByteBuf, Kind> KIND_CODEC = ByteBufCodecs.idMapper(i -> Kind.values()[Math.floorMod(i, Kind.values().length)],
            Kind::ordinal);
    public static final StreamCodec<ByteBuf, ThreatPayload> STREAM_CODEC = StreamCodec.composite(
            KIND_CODEC, ThreatPayload::kind,
            Vec3.STREAM_CODEC, ThreatPayload::from,
            ThreatPayload::new);

    // The client registers the handler (client.ThreatWarningHud).
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
