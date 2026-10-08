package com.cornspies.fusionfactions.weapon.upgrade;

import java.util.Locale;

import com.cornspies.fusionfactions.config.UpgradeConfig;
import com.cornspies.fusionfactions.weapon.WeaponStats;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;

// How upgrade parts change a weapon's stats (DESIGN.md §4.5). The numbers come from UpgradeConfig, so
// the server and its clients agree. Parts of a type the weapon doesn't accept (any more) do nothing.
public final class UpgradeRules {
    // Reductions (noise, recoil, reload time, spread) never reach zero, whatever the config says.
    private static final double MAX_REDUCTION = 0.95;

    public static boolean accepts(Item weapon, UpgradeType type) {
        return UpgradeConfig.accepts(BuiltInRegistries.ITEM.getKey(weapon).getPath(), type);
    }

    public static WeaponStats apply(Item weapon, WeaponStats base, WeaponUpgrades upgrades) {
        if (upgrades.isEmpty()) {
            return base;
        }
        WeaponStats.Builder stats = base.toBuilder();
        for (UpgradeType type : UpgradeType.values()) {
            int tier = upgrades.tier(type);
            if (tier == 0 || !accepts(weapon, type)) {
                continue;
            }
            double value = UpgradeConfig.value(type, tier) / 100.0;
            double kept = 1.0 - Math.min(value, MAX_REDUCTION);
            switch (type) {
                case PENETRATION -> stats.armorPiercing((float) Math.min(1.0, base.armorPiercing() + value));
                case MAGAZINE -> stats.magazine(Math.max(base.magazineSize(), (int) Math.round(base.magazineSize() * (1.0 + value))));
                case NOISE -> stats.soundRange((float) (base.soundRange() * kept));
                case RECOIL -> stats.recoil((float) (base.recoilDegrees() * kept));
                case RELOAD -> stats.reloadPerRound(Math.max(1, (int) Math.round(base.reloadTicksPerRound() * kept)));
                case ACCURACY -> stats.spread((float) (base.spreadDegrees() * kept));
                case RANGE -> stats.bulletSpeed((float) (base.bulletSpeed() * (1.0 + value)));
                case SIGHT -> stats.aimZoom((float) (1.0 + value));
            }
        }
        return stats.build();
    }

    // The effect of one part, e.g. "+40% rounds per magazine", for tooltips.
    public static MutableComponent describe(UpgradeType type, int tier) {
        double value = UpgradeConfig.value(type, tier);
        String number = value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
        return Component.translatable(type.translationKey() + ".effect", number);
    }

    private UpgradeRules() {
    }
}
