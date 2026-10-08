package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Placeholder sounds: assets/fusionfactions/sounds.json points them at vanilla sound files for now.
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, FusionFactions.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_FIRE = register("weapon.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EMPTY = register("weapon.empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RELOAD = register("weapon.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RELOAD_DONE = register("weapon.reload_done");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, id -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {
    }
}
