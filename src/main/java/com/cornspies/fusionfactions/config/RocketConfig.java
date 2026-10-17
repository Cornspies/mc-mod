package com.cornspies.fusionfactions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// Rocket launcher settings (DESIGN.md §6b.6, §6b.7), part of the synced server config: how locking on works, how
// well rockets steer, their blast and what the handheld radar uses. The launcher's own stats (damage at the centre,
// reload, rocket speed) are in weapon.Weapons like every weapon's.
public final class RocketConfig {
    private static ModConfigSpec.IntValue lockTicks;
    private static ModConfigSpec.IntValue lockGraceTicks;
    private static ModConfigSpec.IntValue lockRange;
    private static ModConfigSpec.DoubleValue lockConeDegrees;
    private static ModConfigSpec.DoubleValue turnDegrees;
    private static ModConfigSpec.DoubleValue blastRadius;
    private static ModConfigSpec.IntValue radarDrain;

    static void define(ModConfigSpec.Builder builder) {
        builder.comment("The guided rocket launcher and the lock warning gear (Phase 3).").push("rocketLauncher");
        lockTicks = builder
                .comment("Ticks a player has to stay near the crosshair while aiming before the launcher locks on (20 ticks = 1 s).")
                .defineInRange("lockTicks", 30, 1, 1200);
        lockGraceTicks = builder
                .comment("Ticks the target may leave the crosshair or go behind cover before the lock is lost.")
                .defineInRange("lockGraceTicks", 10, 0, 200);
        lockRange = builder
                .comment("Blocks within which the launcher can lock on. The range upgrade raises it.")
                .defineInRange("lockRange", 96, 8, 512);
        lockConeDegrees = builder
                .comment("How far from the crosshair a player may be and still count as aimed at, in degrees.")
                .defineInRange("lockConeDegrees", 5.0, 0.5, 45.0);
        turnDegrees = builder
                .comment("How far a guided rocket can turn per tick, in degrees. Lower makes it easier to dodge.")
                .defineInRange("turnDegrees", 6.0, 0.0, 90.0);
        blastRadius = builder
                .comment("Blocks over which a rocket's blast damage falls off to nothing. Rockets never break blocks.")
                .defineInRange("blastRadius", 3.0, 0.5, 16.0);
        radarDrain = builder
                .comment("FE per tick a handheld radar uses while held.")
                .defineInRange("radarDrain", 10, 0, 100_000);
        builder.pop();
    }

    public static int lockTicks() {
        return get(lockTicks);
    }

    public static int lockGraceTicks() {
        return get(lockGraceTicks);
    }

    public static int lockRange() {
        return get(lockRange);
    }

    public static double lockConeDegrees() {
        return get(lockConeDegrees);
    }

    public static double turnDegrees() {
        return get(turnDegrees);
    }

    public static double blastRadius() {
        return get(blastRadius);
    }

    public static int radarDrain() {
        return get(radarDrain);
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return ServerConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private RocketConfig() {
    }
}
