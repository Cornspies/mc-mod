package com.cornspies.fusionfactions.config;

import java.time.ZoneId;
import java.util.List;

import com.cornspies.fusionfactions.missile.RaidWindows;

import net.neoforged.neoforge.common.ModConfigSpec;

// Missile settings (DESIGN.md §6), part of the synced server config. Clients get the same values when
// they join, for tooltips and the launcher screen. Per-tier values are lists for tiers I, II and III.
public final class MissileConfig {
    private static ModConfigSpec.ConfigValue<List<? extends String>> raidWindows;
    private static ModConfigSpec.ConfigValue<String> raidTimeZone;
    private static ModConfigSpec.BooleanValue requirePower;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> range;
    private static ModConfigSpec.IntValue minRange;
    private static ModConfigSpec.DoubleValue flightSpeed;
    private static ModConfigSpec.IntValue launcherCooldownSeconds;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> launchEnergy;
    private static ModConfigSpec.IntValue designatorRange;
    private static ModConfigSpec.DoubleValue hardBlockResistance;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> explosivePower;
    private static ModConfigSpec.IntValue siloCooldownSeconds;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> breachingPower;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> empRadius;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> empDisableSeconds;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> incendiaryPower;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> incendiaryFireRadius;
    private static ModConfigSpec.ConfigValue<List<? extends Number>> missileHealth;
    private static ModConfigSpec.IntValue radarRange;
    private static ModConfigSpec.IntValue radarAreaSize;
    private static ModConfigSpec.IntValue radarEnergyPerTick;
    private static ModConfigSpec.IntValue interceptorRange;
    private static ModConfigSpec.IntValue interceptorCooldownTicks;
    private static ModConfigSpec.DoubleValue interceptorDamage;
    private static ModConfigSpec.IntValue interceptorEnergy;

    static void define(ModConfigSpec.Builder builder) {
        builder.comment("Missiles, launchers and their counterplay (Phase 2).").push("missiles");

        raidWindows = builder
                .comment("When missiles can be launched. Each entry is \"<days> <HH:MM>-<HH:MM>\", for example",
                        "\"SAT 18:00-23:00\", \"MON-FRI 20:00-22:00\", \"SAT,SUN 14:00-02:00\" or \"DAILY 20:00-22:00\".",
                        "Days: MON, TUE, WED, THU, FRI, SAT, SUN, ranges like MON-FRI, DAILY, WEEKDAYS, WEEKEND.",
                        "An end before the start runs past midnight. Missiles already in the air still land.",
                        "Leave the list empty to allow launches at any time.")
                .defineListAllowEmpty("raidWindows", List.of(), () -> "DAILY 20:00-22:00", RaidWindows::isValid);
        raidTimeZone = builder
                .comment("Time zone for raidWindows, e.g. Europe/Berlin or America/New_York. Empty: the server's time zone.")
                .define("raidTimeZone", "", RaidWindows::isValidZone);
        requirePower = builder
                .comment("Whether launchers, silos, radars and machines need FE (Forge Energy) from generators and cables,",
                        "ours or another mod's. Interceptors always need FE. A config file from 0.2.0 or older keeps its old value (false).")
                .define("requirePower", true);
        range = tiers(builder, "range", "Maximum distance to the target per missile tier, in blocks (horizontal).",
                List.of(500, 1000, 2000), 64, 30_000_000);
        minRange = builder
                .comment("Minimum distance to the target, in blocks (horizontal).")
                .defineInRange("minRange", 64, 0, 10_000);
        flightSpeed = builder
                .comment("How fast missiles travel to their target, in blocks per second. Sets the flight time.")
                .defineInRange("flightSpeed", 25.0, 1.0, 1000.0);
        launcherCooldownSeconds = builder
                .comment("Seconds a missile launcher needs between launches.")
                .defineInRange("launcherCooldownSeconds", 60, 0, 24 * 3600);
        siloCooldownSeconds = builder
                .comment("Seconds a missile silo needs between launches.")
                .defineInRange("siloCooldownSeconds", 120, 0, 24 * 3600);
        launchEnergy = tiers(builder, "launchEnergy", "FE a launch takes per missile tier, if requirePower is on.",
                List.of(50_000, 100_000, 200_000), 0, 100_000_000);
        designatorRange = builder
                .comment("How far away the target designator can mark a block, in blocks.")
                .defineInRange("designatorRange", 256, 8, 1024);
        hardBlockResistance = builder
                .comment("Blocks with at least this blast resistance (obsidian is 1200) stop missile blasts and only",
                        "break to breaching missiles. Blocks in the tag #fusionfactions:missile_immune never break.")
                .defineInRange("hardBlockResistance", 600.0, 1.0, 1.0E9);
        explosivePower = tiers(builder, "explosivePower", "Blast power of explosive missiles per tier (TNT is 4).",
                List.of(6, 9, 12), 1, 32);
        breachingPower = tiers(builder, "breachingPower", "Blast power of breaching missiles per tier. They also break hard blocks.",
                List.of(4, 5, 6), 1, 32);
        empRadius = tiers(builder, "empRadius", "Radius in blocks in which EMP missiles drain all FE, per tier.",
                List.of(8, 12, 16), 1, 64);
        empDisableSeconds = tiers(builder, "empDisableSeconds", "Seconds EMP missiles disable missile launchers and silos in their radius, per tier.",
                List.of(30, 60, 90), 0, 3600);
        incendiaryPower = tiers(builder, "incendiaryPower", "Blast power of incendiary missiles per tier.",
                List.of(2, 3, 4), 1, 32);
        incendiaryFireRadius = tiers(builder, "incendiaryFireRadius", "Radius in blocks that incendiary missiles set on fire, per tier.",
                List.of(6, 8, 10), 1, 32);

        missileHealth = tiers(builder, "missileHealth", "Health of a missile on its way down, per tier. Bullets and interceptors damage it.",
                List.of(10, 20, 40), 1, 10_000);

        builder.comment("Radar blocks: they track missiles and alert the team of the player who placed them.").push("radar");
        radarRange = builder
                .comment("How far a radar sees missiles, in blocks (horizontal, any height).")
                .defineInRange("range", 256, 16, 4096);
        radarAreaSize = builder
                .comment("Half the size of the square a radar reports as the launch area, in blocks. The launch spot is",
                        "somewhere inside it, not necessarily in the middle.")
                .defineInRange("launchAreaSize", 100, 0, 10_000);
        radarEnergyPerTick = builder
                .comment("FE a radar uses per tick, if requirePower is on.")
                .defineInRange("energyPerTick", 20, 0, 100_000);
        builder.pop();

        builder.comment("Interceptor blocks: they shoot down missiles coming down near them.").push("interceptor");
        interceptorRange = builder
                .comment("How far an interceptor reaches, in blocks. It needs a clear line of sight to the missile.")
                .defineInRange("range", 96, 8, 512);
        interceptorCooldownTicks = builder
                .comment("Ticks between two interceptor shots (20 ticks = 1 second).")
                .defineInRange("cooldownTicks", 40, 1, 72_000);
        interceptorDamage = builder
                .comment("Damage of one interceptor hit. Compare with missileHealth.")
                .defineInRange("damage", 20.0, 0.0, 10_000.0);
        interceptorEnergy = builder
                .comment("FE an interceptor shot takes. Interceptors always need FE, whatever requirePower says.")
                .defineInRange("energyPerShot", 10_000, 0, 10_000_000);
        builder.pop();

        builder.pop();
    }

    private static ModConfigSpec.ConfigValue<List<? extends Number>> tiers(ModConfigSpec.Builder builder, String name, String comment,
            List<? extends Number> defaults, double min, double max) {
        return builder.comment(comment).defineList(List.of(name), () -> defaults, () -> min,
                value -> value instanceof Number number && number.doubleValue() >= min && number.doubleValue() <= max,
                ModConfigSpec.Range.of(3, 3));
    }

    public static List<? extends String> raidWindows() {
        return get(raidWindows);
    }

    public static ZoneId raidTimeZone() {
        String zone = get(raidTimeZone);
        return zone.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(zone);
    }

    public static boolean requirePower() {
        return get(requirePower);
    }

    public static int range(int tier) {
        return get(range).get(tier - 1).intValue();
    }

    public static int minRange() {
        return get(minRange);
    }

    public static double flightSpeed() {
        return get(flightSpeed);
    }

    public static int launcherCooldownTicks() {
        return get(launcherCooldownSeconds) * 20;
    }

    public static int launchEnergy(int tier) {
        return get(launchEnergy).get(tier - 1).intValue();
    }

    public static int designatorRange() {
        return get(designatorRange);
    }

    public static double hardBlockResistance() {
        return get(hardBlockResistance);
    }

    public static float explosivePower(int tier) {
        return get(explosivePower).get(tier - 1).floatValue();
    }

    public static int siloCooldownTicks() {
        return get(siloCooldownSeconds) * 20;
    }

    public static float breachingPower(int tier) {
        return get(breachingPower).get(tier - 1).floatValue();
    }

    public static int empRadius(int tier) {
        return get(empRadius).get(tier - 1).intValue();
    }

    public static int empDisableTicks(int tier) {
        return get(empDisableSeconds).get(tier - 1).intValue() * 20;
    }

    public static float incendiaryPower(int tier) {
        return get(incendiaryPower).get(tier - 1).floatValue();
    }

    public static int incendiaryFireRadius(int tier) {
        return get(incendiaryFireRadius).get(tier - 1).intValue();
    }

    public static float missileHealth(int tier) {
        return get(missileHealth).get(tier - 1).floatValue();
    }

    public static int radarRange() {
        return get(radarRange);
    }

    public static int radarAreaSize() {
        return get(radarAreaSize);
    }

    public static int radarEnergyPerTick() {
        return get(radarEnergyPerTick);
    }

    public static int interceptorRange() {
        return get(interceptorRange);
    }

    public static int interceptorCooldownTicks() {
        return get(interceptorCooldownTicks);
    }

    public static float interceptorDamage() {
        return get(interceptorDamage).floatValue();
    }

    public static int interceptorEnergy() {
        return get(interceptorEnergy);
    }

    // Before the config is loaded (e.g. in the main menu) the defaults apply.
    private static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return ServerConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private MissileConfig() {
    }
}
