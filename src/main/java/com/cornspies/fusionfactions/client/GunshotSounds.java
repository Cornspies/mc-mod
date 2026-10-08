package com.cornspies.fusionfactions.client;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.weapon.GunshotPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Plays gunshots (GunshotPayload) so that they fade to silence at the weapon's hearing range, instead of
// the fixed distance in sounds.json. The noise upgrade shortens that range (DESIGN.md §4.5).
public final class GunshotSounds {
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(GunshotPayload.TYPE, GunshotSounds::handle);
    }

    private static void handle(GunshotPayload payload, IPayloadContext context) {
        play(payload.sound().value(), payload.position(), payload.range());
    }

    public static void play(SoundEvent sound, Vec3 position, float range) {
        Minecraft.getInstance().getSoundManager().play(new Gunshot(sound, position, range));
    }

    private static final class Gunshot extends SimpleSoundInstance {
        private final int range;

        Gunshot(SoundEvent sound, Vec3 position, float range) {
            super(sound, SoundSource.PLAYERS, 1.0F, 1.0F, SoundInstance.createUnseededRandom(), position.x, position.y, position.z);
            this.range = Math.max(1, Math.round(range));
        }

        // Same sound file, volume and pitch as in sounds.json; only the distance at which it fades out changes.
        @Override
        public @Nullable WeighedSoundEvents getOrResolve(SoundManager soundManager) {
            WeighedSoundEvents events = super.getOrResolve(soundManager);
            Sound sound = this.sound;
            if (sound != null && sound != SoundManager.EMPTY_SOUND && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
                this.sound = new Sound(sound.getLocation(), sound.getVolume(), sound.getPitch(), sound.getWeight(), sound.getType(),
                        sound.shouldStream(), sound.shouldPreload(), this.range);
            }
            return events;
        }
    }

    private GunshotSounds() {
    }
}
