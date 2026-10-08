package com.cornspies.fusionfactions.weapon;

// Everything that defines how a weapon behaves. All values are read on the server; the client
// only uses them for cosmetic effects. The values themselves live in Weapons (single source of truth, OD-29).
public record WeaponStats(
        // Health points per pellet that hits (20 = full health).
        float damage,
        // Bullets per shot.
        int pellets,
        int magazineSize,
        // Minimum ticks between shots.
        int fireIntervalTicks,
        // Keeps firing while the use button is held. Otherwise one shot per click.
        boolean automatic,
        // Reloading adds one round every this many ticks.
        int reloadTicksPerRound,
        // Ammo items used per round loaded.
        int ammoPerRound,
        // Maximum random deviation of each pellet, in degrees.
        float spreadDegrees,
        // Cosmetic camera kick on the shooter's client, in degrees.
        float recoilDegrees,
        // Blocks per tick.
        float bulletSpeed,
        // Ticks before a bullet that hit nothing disappears.
        int bulletLifetimeTicks) {
}
