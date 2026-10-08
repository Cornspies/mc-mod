package com.cornspies.fusionfactions.crafting;

import com.cornspies.fusionfactions.config.StartupConfig;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;

// Recipe condition for the crate-only weapons (D26): true only if the startup config allows crafting them.
public record HeavyWeaponRecipesCondition() implements ICondition {
    public static final HeavyWeaponRecipesCondition INSTANCE = new HeavyWeaponRecipesCondition();
    public static final MapCodec<HeavyWeaponRecipesCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return StartupConfig.HEAVY_WEAPON_RECIPES.getAsBoolean();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
