package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.FusionFactions;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

// Damage types are datapack entries: data/fusionfactions/damage_type/<name>.json.
public final class ModDamageTypes {
    // In minecraft:bypasses_cooldown (D21) and minecraft:is_projectile.
    public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, FusionFactions.id("bullet"));
    // Missile blasts (DESIGN.md §6.5). In minecraft:is_explosion, so Blast Protection counts.
    public static final ResourceKey<DamageType> MISSILE = ResourceKey.create(Registries.DAMAGE_TYPE, FusionFactions.id("missile"));

    private ModDamageTypes() {
    }
}
