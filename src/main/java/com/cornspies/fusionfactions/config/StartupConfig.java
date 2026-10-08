package com.cornspies.fusionfactions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// Settings that must be known while datapacks load, before the server-side (SYNCED) config exists.
// FML's STARTUP type is loaded as soon as it's registered. Changes need a server restart.
public final class StartupConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HEAVY_WEAPON_RECIPES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        HEAVY_WEAPON_RECIPES = builder
                .comment("Allow crafting the sniper rifle and gatling gun. Off by default: they only come from supply crates.",
                        "Takes effect after a server restart.")
                .define("heavyWeaponRecipes", false);
        SPEC = builder.build();
    }

    private StartupConfig() {
    }
}
