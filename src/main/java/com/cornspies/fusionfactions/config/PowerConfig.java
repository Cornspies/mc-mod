package com.cornspies.fusionfactions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// Power settings (DESIGN.md §6b.3), part of the synced server config. Whether blocks need power at all is
// missiles.requirePower.
public final class PowerConfig {
    private static ModConfigSpec.IntValue generatorOutput;
    private static ModConfigSpec.IntValue solarOutput;
    private static ModConfigSpec.IntValue cableThroughput;
    private static ModConfigSpec.IntValue chargingRate;

    static void define(ModConfigSpec.Builder builder) {
        builder.comment("Generators, cables and the charging station (Phase 3).").push("power");
        generatorOutput = builder
                .comment("FE per tick a combustion generator makes while it burns fuel.")
                .defineInRange("generatorOutput", 40, 1, 100_000);
        solarOutput = builder
                .comment("FE per tick a solar panel makes under open sky by day. Half in rain, nothing at night.")
                .defineInRange("solarOutput", 8, 0, 100_000);
        cableThroughput = builder
                .comment("FE per tick one network of connected cables carries in total.")
                .defineInRange("cableThroughput", 2000, 1, 1_000_000);
        chargingRate = builder
                .comment("FE per tick the charging station puts into the item in its slot.")
                .defineInRange("chargingRate", 500, 1, 1_000_000);
        builder.pop();
    }

    public static int generatorOutput() {
        return get(generatorOutput);
    }

    public static int solarOutput() {
        return get(solarOutput);
    }

    public static int cableThroughput() {
        return get(cableThroughput);
    }

    public static int chargingRate() {
        return get(chargingRate);
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return ServerConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private PowerConfig() {
    }
}
