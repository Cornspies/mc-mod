package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.registry.ModSounds;

// Central weapon stats table (DESIGN.md §3.3). The roster is D23, the balance D24.
public final class Weapons {
    public static final WeaponStats PISTOL = WeaponStats.builder()
            .damage(5.0F)
            .magazine(12)
            .fireInterval(4)
            .reloadPerRound(6)
            .spread(1.0F)
            .recoil(3.0F)
            .fireSound(ModSounds.WEAPON_FIRE)
            .build();

    private Weapons() {
    }
}
