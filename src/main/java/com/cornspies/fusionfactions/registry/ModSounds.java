package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Placeholder sounds: assets/fusionfactions/sounds.json points them at vanilla sound files for now.
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, FusionFactions.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = register("weapon.pistol.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> AUTO_PISTOL_FIRE = register("weapon.auto_pistol.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMG_FIRE = register("weapon.smg.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_FIRE = register("weapon.shotgun.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNIPER_RIFLE_FIRE = register("weapon.sniper_rifle.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATLING_GUN_FIRE = register("weapon.gatling_gun.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATLING_GUN_SPIN = register("weapon.gatling_gun.spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EMPTY = register("weapon.empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RELOAD = register("weapon.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RELOAD_DONE = register("weapon.reload_done");
    // Hit markers, played only to the shooter.
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_HIT = register("weapon.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_HEADSHOT = register("weapon.headshot");
    // Missiles (DESIGN.md §6.5).
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_LAUNCH = register("missile.launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_INCOMING = register("missile.incoming");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_EXPLODE = register("missile.explode");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, id -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {
    }
}
