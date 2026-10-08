package com.cornspies.fusionfactions.weapon;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Server to client: a shot was fired, play its sound. Vanilla sound packets only reach players within
// 16 blocks and the client fades a sound out at a distance fixed in sounds.json. This carries the
// weapon's own hearing range (WeaponStats.soundRange, shortened by the noise upgrade), which decides
// both who gets the packet and where the sound fades to silence (client.GunshotSounds).
public record GunshotPayload(Holder<SoundEvent> sound, Vec3 position, float range) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GunshotPayload> TYPE = new CustomPacketPayload.Type<>(FusionFactions.id("gunshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunshotPayload> STREAM_CODEC = StreamCodec.composite(
            SoundEvent.STREAM_CODEC, GunshotPayload::sound,
            Vec3.STREAM_CODEC, GunshotPayload::position,
            ByteBufCodecs.FLOAT, GunshotPayload::range,
            GunshotPayload::new);

    // The client registers the handler (client.GunshotSounds), so no client code is referenced here.
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC);
    }

    // Sends the shot to every player within its hearing range except the shooter, whose client plays it already.
    public static void broadcast(ServerLevel level, @Nullable ServerPlayer shooter, Vec3 position, WeaponStats stats) {
        GunshotPayload payload = new GunshotPayload(stats.fireSound(), position, stats.soundRange());
        PacketDistributor.sendToPlayersNear(level, shooter, position.x, position.y, position.z, stats.soundRange(), payload);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
