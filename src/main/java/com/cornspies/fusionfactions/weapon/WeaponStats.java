package com.cornspies.fusionfactions.weapon;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

// Everything that defines how a weapon behaves. All values are read on the server; the client
// only uses them for cosmetic effects. The values themselves live in Weapons (single source of truth, OD-29).
public record WeaponStats(
        // Health points per pellet that hits (20 = full health), before armor. Headshots multiply this.
        float damage,
        // Bullets per shot.
        int pellets,
        int magazineSize,
        // Minimum ticks between shots.
        int fireIntervalTicks,
        // Keeps firing while the use button is held. Otherwise one shot per click.
        boolean automatic,
        // Ticks the trigger has to be held before the first shot (the gatling's barrels spin up).
        int spinUpTicks,
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
        int bulletLifetimeTicks,
        // Knockback per pellet that hits (vanilla melee hits use 0.4). Knockback resistance reduces it.
        float knockback,
        // Share of the armor's protection that bullets ignore, 0 to 1 (D24).
        float armorPiercing,
        // Has a zoom scope (D28).
        boolean scoped,
        Holder<SoundEvent> fireSound) {

    public static Builder builder() {
        return new Builder();
    }

    // Defaults for everything that most weapons share. damage, magazineSize, fireIntervalTicks,
    // reloadTicksPerRound and fireSound must always be set.
    public static final class Builder {
        private float damage;
        private int pellets = 1;
        private int magazineSize;
        private int fireIntervalTicks;
        private boolean automatic;
        private int spinUpTicks;
        private int reloadTicksPerRound;
        private int ammoPerRound = 1;
        private float spreadDegrees;
        private float recoilDegrees;
        private float bulletSpeed = 4.0F;
        private int bulletLifetimeTicks = 50;
        private float knockback = 0.4F;
        private float armorPiercing = 0.3F;
        private boolean scoped;
        private Holder<SoundEvent> fireSound;

        public Builder damage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder pellets(int pellets) {
            this.pellets = pellets;
            return this;
        }

        public Builder magazine(int magazineSize) {
            this.magazineSize = magazineSize;
            return this;
        }

        public Builder fireInterval(int ticks) {
            this.fireIntervalTicks = ticks;
            return this;
        }

        public Builder automatic() {
            this.automatic = true;
            return this;
        }

        public Builder spinUp(int ticks) {
            this.spinUpTicks = ticks;
            return this;
        }

        public Builder reloadPerRound(int ticks) {
            this.reloadTicksPerRound = ticks;
            return this;
        }

        public Builder ammoPerRound(int ammo) {
            this.ammoPerRound = ammo;
            return this;
        }

        public Builder spread(float degrees) {
            this.spreadDegrees = degrees;
            return this;
        }

        public Builder recoil(float degrees) {
            this.recoilDegrees = degrees;
            return this;
        }

        public Builder bulletSpeed(float blocksPerTick) {
            this.bulletSpeed = blocksPerTick;
            return this;
        }

        public Builder bulletLifetime(int ticks) {
            this.bulletLifetimeTicks = ticks;
            return this;
        }

        public Builder knockback(float knockback) {
            this.knockback = knockback;
            return this;
        }

        public Builder armorPiercing(float share) {
            this.armorPiercing = share;
            return this;
        }

        public Builder scoped() {
            this.scoped = true;
            return this;
        }

        public Builder fireSound(Holder<SoundEvent> sound) {
            this.fireSound = sound;
            return this;
        }

        public WeaponStats build() {
            return new WeaponStats(this.damage, this.pellets, this.magazineSize, this.fireIntervalTicks, this.automatic, this.spinUpTicks,
                    this.reloadTicksPerRound, this.ammoPerRound, this.spreadDegrees, this.recoilDegrees, this.bulletSpeed,
                    this.bulletLifetimeTicks, this.knockback, this.armorPiercing, this.scoped, this.fireSound);
        }
    }
}
