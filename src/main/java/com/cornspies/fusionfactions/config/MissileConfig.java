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
                .comment("Whether launchers need FE (Forge Energy) from another mod's cables or generators.",
                        "Off by default, because Minecraft itself has no power source.")
                .define("requirePower", false);
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

    // Before the config is loaded (e.g. in the main menu) the defaults apply.
    private static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return ServerConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private MissileConfig() {
    }
}
