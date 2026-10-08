package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.registry.ModSounds;

// Central weapon stats table (DESIGN.md §3.3). The roster is D23, the balance D24; the numbers are a first
// balance pass (DESIGN.md §4.3) and can be tuned here. Defaults not listed are in WeaponStats.Builder:
// 1 pellet, 1 ammo per round, 4 blocks/tick, 50 ticks lifetime, 0.4 knockback, 30% armor piercing, heard
// within 48 blocks. Upgrades (UpgradeRules) change these per weapon stack.
public final class Weapons {
    // Semi-auto sidearm: 7 hits against full diamond.
    public static final WeaponStats PISTOL = WeaponStats.builder()
            .damage(6.0F)
            .magazine(12)
            .fireInterval(4)
            .reloadPerRound(5)
            .spread(1.0F)
            .recoil(3.0F)
            .fireSound(ModSounds.PISTOL_FIRE)
            .build();

    // Full-auto sidearm: weaker hits, more of them.
    public static final WeaponStats AUTO_PISTOL = WeaponStats.builder()
            .damage(4.0F)
            .magazine(20)
            .fireInterval(3)
            .automatic()
            .reloadPerRound(3)
            .spread(2.5F)
            .recoil(1.5F)
            .bulletLifetime(40)
            .knockback(0.25F)
            .fireSound(ModSounds.AUTO_PISTOL_FIRE)
            .build();

    // Full-auto, close range: fastest fire rate of the craftable weapons, wide spread.
    public static final WeaponStats SMG = WeaponStats.builder()
            .damage(3.5F)
            .magazine(30)
            .fireInterval(2)
            .automatic()
            .reloadPerRound(3)
            .spread(3.0F)
            .recoil(1.0F)
            .bulletLifetime(30)
            .knockback(0.15F)
            .fireSound(ModSounds.SMG_FIRE)
            .build();

    // Pellet burst, close range: 8 pellets per shot, short reach. Shells cost 2 ammo.
    public static final WeaponStats SHOTGUN = WeaponStats.builder()
            .damage(2.5F)
            .pellets(8)
            .magazine(6)
            .fireInterval(16)
            .reloadPerRound(10)
            .ammoPerRound(2)
            .spread(6.0F)
            .recoil(10.0F)
            .bulletSpeed(3.5F)
            .bulletLifetime(12)
            .knockback(0.2F)
            .fireSound(ModSounds.SHOTGUN_FIRE)
            .build();

    // Crate-only (D26). Two body shots kill through diamond, a headshot kills unarmored or iron.
    // Fast, pierces half of the armor; rounds cost 2 ammo. Heard twice as far as other guns.
    // Inaccurate from the hip, dead on when scoped (D39).
    public static final WeaponStats SNIPER_RIFLE = WeaponStats.builder()
            .damage(18.0F)
            .magazine(5)
            .fireInterval(30)
            .reloadPerRound(20)
            .ammoPerRound(2)
            .spread(4.0F)
            .aimSpread(0.0F)
            .recoil(12.0F)
            .bulletSpeed(8.0F)
            .bulletLifetime(40)
            .knockback(0.8F)
            .armorPiercing(0.5F)
            .scoped()
            .fireSound(ModSounds.SNIPER_RIFLE_FIRE)
            .soundRange(96.0F)
            .build();

    // Crate-only (D26). Needs a second to spin up, then fires every tick.
    public static final WeaponStats GATLING_GUN = WeaponStats.builder()
            .damage(2.5F)
            .magazine(120)
            .fireInterval(1)
            .automatic()
            .spinUp(20)
            .reloadPerRound(1)
            .spread(3.5F)
            .recoil(0.4F)
            .bulletLifetime(40)
            .knockback(0.05F)
            .fireSound(ModSounds.GATLING_GUN_FIRE)
            .build();

    private Weapons() {
    }
}
