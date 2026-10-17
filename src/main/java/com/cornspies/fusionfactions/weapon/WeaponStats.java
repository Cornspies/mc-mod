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
        // Maximum random deviation of each pellet when firing from the hip, in degrees.
        float spreadDegrees,
        // Spread while aiming down sights, as a share of the hip-fire spread (D39).
        float aimSpreadFactor,
        // Zoom while aiming down sights (1 = none). Sights raise it; scoped weapons use their scope levels instead.
        float aimZoom,
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
        Holder<SoundEvent> fireSound,
        // Blocks within which other players hear a shot. The noise upgrade shortens it.
        float soundRange,
        // Fires guided rockets that lock on while aiming (the rocket launcher, DESIGN.md §6b.6) instead of bullets.
        boolean guided,
        // Lock range as a share of the configured one; the range upgrade raises it for guided weapons.
        float lockRangeFactor) {

    public static Builder builder() {
        return new Builder();
    }

    // A builder holding these stats, for changing some of them (see UpgradeRules).
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.damage = this.damage;
        builder.pellets = this.pellets;
        builder.magazineSize = this.magazineSize;
        builder.fireIntervalTicks = this.fireIntervalTicks;
        builder.automatic = this.automatic;
        builder.spinUpTicks = this.spinUpTicks;
        builder.reloadTicksPerRound = this.reloadTicksPerRound;
        builder.ammoPerRound = this.ammoPerRound;
        builder.spreadDegrees = this.spreadDegrees;
        builder.aimSpreadFactor = this.aimSpreadFactor;
        builder.aimZoom = this.aimZoom;
        builder.recoilDegrees = this.recoilDegrees;
        builder.bulletSpeed = this.bulletSpeed;
        builder.bulletLifetimeTicks = this.bulletLifetimeTicks;
        builder.knockback = this.knockback;
        builder.armorPiercing = this.armorPiercing;
        builder.scoped = this.scoped;
        builder.fireSound = this.fireSound;
        builder.soundRange = this.soundRange;
        builder.guided = this.guided;
        builder.lockRangeFactor = this.lockRangeFactor;
        return builder;
    }

    // Defaults for everything that most weapons share (sounds carry 48 blocks; aiming halves the spread
    // and zooms 1.1x). damage, magazineSize, fireIntervalTicks,
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
        private float aimSpreadFactor = 0.5F;
        private float aimZoom = 1.1F;
        private float recoilDegrees;
        private float bulletSpeed = 4.0F;
        private int bulletLifetimeTicks = 50;
        private float knockback = 0.4F;
        private float armorPiercing = 0.3F;
        private boolean scoped;
        private Holder<SoundEvent> fireSound;
        private float soundRange = 48.0F;
        private boolean guided;
        private float lockRangeFactor = 1.0F;

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

        public Builder aimSpread(float shareOfHipSpread) {
            this.aimSpreadFactor = shareOfHipSpread;
            return this;
        }

        public Builder aimZoom(float zoom) {
            this.aimZoom = zoom;
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

        public Builder soundRange(float blocks) {
            this.soundRange = blocks;
            return this;
        }

        public Builder guided() {
            this.guided = true;
            return this;
        }

        public Builder lockRangeFactor(float factor) {
            this.lockRangeFactor = factor;
            return this;
        }

        public WeaponStats build() {
            return new WeaponStats(this.damage, this.pellets, this.magazineSize, this.fireIntervalTicks, this.automatic, this.spinUpTicks,
                    this.reloadTicksPerRound, this.ammoPerRound, this.spreadDegrees, this.aimSpreadFactor, this.aimZoom, this.recoilDegrees, this.bulletSpeed,
                    this.bulletLifetimeTicks, this.knockback, this.armorPiercing, this.scoped, this.fireSound, this.soundRange, this.guided,
                    this.lockRangeFactor);
        }
    }
}
