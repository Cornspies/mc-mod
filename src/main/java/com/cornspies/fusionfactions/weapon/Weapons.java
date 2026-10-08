package com.cornspies.fusionfactions.weapon;

// Central weapon stats table (DESIGN.md §3.3). Balance is OD-06 and the final roster is OD-03, both for M3.
public final class Weapons {
    // M2 test gun. Placeholder numbers: about 4 body shots against an unarmored player.
    public static final WeaponStats PISTOL = new WeaponStats(
            5.0F, // damage
            1, // pellets
            12, // magazine
            4, // fire interval (ticks)
            false, // automatic
            6, // reload ticks per round
            1, // ammo per round
            1.0F, // spread
            3.0F, // recoil
            4.0F, // bullet speed
            50); // bullet lifetime (ticks)

    private Weapons() {
    }
}
