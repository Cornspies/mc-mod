package com.cornspies.fusionfactions.config;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

// Gameplay settings for a server (DESIGN.md §3.3, §5.4): supply crates here, weapon upgrades in UpgradeConfig,
// missiles in MissileConfig.
// Registered as FML's SYNCED type, which replaced the old SERVER type: it is loaded per server
// instance and can be overridden per world in <world>/syncedconfig.
public final class ServerConfig {
    public enum ZoneMode {
        SPAWN,
        CENTER
    }

    public enum AnnouncePrecision {
        EXACT,
        ROUNDED,
        DIRECTION
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue INTERVAL_MINUTES;
    public static final ModConfigSpec.IntValue MIN_PLAYERS_ONLINE;
    public static final ModConfigSpec.EnumValue<ZoneMode> ZONE_MODE;
    public static final ModConfigSpec.IntValue ZONE_CENTER_X;
    public static final ModConfigSpec.IntValue ZONE_CENTER_Z;
    public static final ModConfigSpec.IntValue ZONE_RADIUS;
    public static final ModConfigSpec.IntValue WEIGHT_COMMON;
    public static final ModConfigSpec.IntValue WEIGHT_RARE;
    public static final ModConfigSpec.IntValue WEIGHT_LEGENDARY;
    public static final ModConfigSpec.IntValue COUNTDOWN_SECONDS;
    public static final ModConfigSpec.IntValue DESPAWN_MINUTES;
    public static final ModConfigSpec.EnumValue<AnnouncePrecision> ANNOUNCE_PRECISION;
    public static final ModConfigSpec.ConfigValue<String> DIMENSION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Supply crates that fall from the sky and are announced to every player.").push("supplyCrates");

        ENABLED = builder
                .comment("Drop crates automatically on a timer. The admin commands work either way.")
                .define("enabled", true);
        INTERVAL_MINUTES = builder
                .comment("Minutes between automatic drops. The timer only runs while at least minPlayersOnline players are online.")
                .defineInRange("intervalMinutes", 90, 1, 7 * 24 * 60);
        MIN_PLAYERS_ONLINE = builder
                .comment("Players that must be online for the drop timer to run.")
                .defineInRange("minPlayersOnline", 2, 0, 1000);

        ZONE_MODE = builder
                .comment("Where crates can land.",
                        "SPAWN: within zoneRadius of the world spawn.",
                        "CENTER: within zoneRadius of zoneCenterX / zoneCenterZ.")
                .defineEnum("zoneMode", ZoneMode.SPAWN);
        ZONE_CENTER_X = builder
                .comment("X coordinate of the drop zone center. Only used when zoneMode is CENTER.")
                .defineInRange("zoneCenterX", 0, -30_000_000, 30_000_000);
        ZONE_CENTER_Z = builder
                .comment("Z coordinate of the drop zone center. Only used when zoneMode is CENTER.")
                .defineInRange("zoneCenterZ", 0, -30_000_000, 30_000_000);
        ZONE_RADIUS = builder
                .comment("Crates land at a random X and Z at most this many blocks from the zone center (a square).")
                .defineInRange("zoneRadius", 250, 0, 1_000_000);

        builder.comment("Relative chance of each crate rarity. Set one to 0 to disable it.").push("rarityWeights");
        WEIGHT_COMMON = builder.defineInRange("common", 60, 0, 1000);
        WEIGHT_RARE = builder.defineInRange("rare", 30, 0, 1000);
        WEIGHT_LEGENDARY = builder.defineInRange("legendary", 10, 0, 1000);
        builder.pop();

        COUNTDOWN_SECONDS = builder
                .comment("Seconds between the announcement and the crate starting to fall.")
                .defineInRange("countdownSeconds", 60, 0, 3600);
        DESPAWN_MINUTES = builder
                .comment("Minutes a landed crate stays before it despawns, if nobody empties it.")
                .defineInRange("despawnMinutes", 30, 1, 24 * 60);
        ANNOUNCE_PRECISION = builder
                .comment("How precisely announcements give the crate's location.",
                        "EXACT: exact X and Z.",
                        "ROUNDED: X and Z rounded to the nearest 50 blocks.",
                        "DIRECTION: compass direction and rough distance from the world spawn.")
                .defineEnum("announcePrecision", AnnouncePrecision.EXACT);
        DIMENSION = builder
                .comment("Dimension that crates drop in, e.g. minecraft:overworld.")
                .define("dimension", "minecraft:overworld", value -> value instanceof String s && Identifier.tryParse(s) != null);

        builder.pop();

        UpgradeConfig.define(builder);
        MissileConfig.define(builder);
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
