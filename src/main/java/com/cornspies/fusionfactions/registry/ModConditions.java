package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.crafting.HeavyWeaponRecipesCondition;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, FusionFactions.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<HeavyWeaponRecipesCondition>> HEAVY_WEAPON_RECIPES =
            CONDITION_CODECS.register("heavy_weapon_recipes", () -> HeavyWeaponRecipesCondition.CODEC);

    private ModConditions() {
    }
}
