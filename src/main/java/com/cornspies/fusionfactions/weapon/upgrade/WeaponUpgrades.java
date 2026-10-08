package com.cornspies.fusionfactions.weapon.upgrade;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

// The upgrade parts installed on a weapon: one tier (1 to 3) per type at most. Stored on the weapon
// stack as a data component (DESIGN.md §4.5), so a weapon keeps its parts wherever it goes.
public record WeaponUpgrades(Map<UpgradeType, Integer> tiers) {
    public static final int MAX_TIER = 3;
    public static final WeaponUpgrades NONE = new WeaponUpgrades(Map.of());

    public static final Codec<WeaponUpgrades> CODEC = Codec.unboundedMap(UpgradeType.CODEC, Codec.intRange(1, MAX_TIER))
            .xmap(WeaponUpgrades::new, WeaponUpgrades::tiers);
    public static final StreamCodec<ByteBuf, WeaponUpgrades> STREAM_CODEC = ByteBufCodecs
            .map(HashMap<UpgradeType, Integer>::new, UpgradeType.STREAM_CODEC, ByteBufCodecs.VAR_INT)
            .map(WeaponUpgrades::new, upgrades -> new HashMap<>(upgrades.tiers));

    public WeaponUpgrades {
        tiers = Map.copyOf(tiers);
    }

    public static WeaponUpgrades of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UPGRADES.get(), NONE);
    }

    // 0 if no part of this type is installed.
    public int tier(UpgradeType type) {
        return this.tiers.getOrDefault(type, 0);
    }

    public boolean isEmpty() {
        return this.tiers.isEmpty();
    }

    // A copy with this type set to the tier, or removed if the tier is 0.
    public WeaponUpgrades with(UpgradeType type, int tier) {
        Map<UpgradeType, Integer> copy = new EnumMap<>(UpgradeType.class);
        copy.putAll(this.tiers);
        if (tier > 0) {
            copy.put(type, tier);
        } else {
            copy.remove(type);
        }
        return copy.isEmpty() ? NONE : new WeaponUpgrades(copy);
    }
}
