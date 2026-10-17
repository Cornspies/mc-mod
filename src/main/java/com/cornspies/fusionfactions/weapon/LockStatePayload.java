package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.FusionFactions;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Server to client: the shooter's own lock-on (LockOn), for the lock marker and tones (client.LockOnHud).
// targetId -1 means no target; progress is 0 to 100, 100 means locked.
public record LockStatePayload(int targetId, int progress) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LockStatePayload> TYPE = new CustomPacketPayload.Type<>(FusionFactions.id("lock_state"));
    public static final StreamCodec<ByteBuf, LockStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LockStatePayload::targetId,
            ByteBufCodecs.VAR_INT, LockStatePayload::progress,
            LockStatePayload::new);

    // The client registers the handler (client.LockOnHud).
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC);
    }

    public boolean locked() {
        return this.targetId >= 0 && this.progress >= 100;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
