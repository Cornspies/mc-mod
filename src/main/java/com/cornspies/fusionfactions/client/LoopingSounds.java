package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.missile.InterceptorRocketEntity;
import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.weapon.GuidedRocketEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// Sounds that loop for as long as something runs: guided and interceptor rockets while they fly. The client starts
// and stops them itself, so the server sends nothing extra. (Blocks had running sounds too; they are shelved, see
// tools/sound_effects.py.)
public final class LoopingSounds {
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof GuidedRocketEntity rocket) {
            Minecraft.getInstance().getSoundManager().play(new EntityLoop(rocket, 1.0F, 1.0F));
        } else if (event.getEntity() instanceof InterceptorRocketEntity rocket) {
            Minecraft.getInstance().getSoundManager().play(new EntityLoop(rocket, 0.7F, 1.4F));
        }
    }

    // Follows an entity until it is removed.
    private static final class EntityLoop extends AbstractTickableSoundInstance {
        private final Entity entity;

        EntityLoop(Entity entity, float volume, float pitch) {
            super(ModSounds.ROCKET_FLY.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.entity = entity;
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.pitch = pitch;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }

        @Override
        public void tick() {
            if (this.entity.isRemoved()) {
                this.stop();
                return;
            }
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }
    }

    private LoopingSounds() {
    }
}
