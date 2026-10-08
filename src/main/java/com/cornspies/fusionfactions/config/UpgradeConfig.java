package com.cornspies.fusionfactions.config;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;

import net.neoforged.neoforge.common.ModConfigSpec;

// Weapon upgrade tuning (DESIGN.md §4.5), part of the synced server config so admins can change it
// without a new build: how strong each tier is, and which weapons accept which upgrade types.
// Clients get the same values when they join, for tooltips and the workbench.
// The defaults are the UpgradeType defaults and DEFAULT_FIELDS below.
public final class UpgradeConfig {
    // Which upgrade types each weapon accepts by default (DESIGN.md §4.5). Keys are weapon item names.
    private static final Map<String, Set<UpgradeType>> DEFAULT_FIELDS = new LinkedHashMap<>();

    static {
        // Every weapon but the sniper (which has its scope) takes a sight (D40).
        DEFAULT_FIELDS.put("pistol", Set.of(UpgradeType.PENETRATION, UpgradeType.MAGAZINE, UpgradeType.NOISE, UpgradeType.RELOAD,
                UpgradeType.ACCURACY, UpgradeType.RANGE, UpgradeType.SIGHT));
        DEFAULT_FIELDS.put("auto_pistol", Set.of(UpgradeType.MAGAZINE, UpgradeType.NOISE, UpgradeType.RECOIL, UpgradeType.RELOAD,
                UpgradeType.ACCURACY, UpgradeType.SIGHT));
        DEFAULT_FIELDS.put("smg", Set.of(UpgradeType.MAGAZINE, UpgradeType.NOISE, UpgradeType.RECOIL, UpgradeType.RELOAD,
                UpgradeType.ACCURACY, UpgradeType.SIGHT));
        DEFAULT_FIELDS.put("shotgun", Set.of(UpgradeType.MAGAZINE, UpgradeType.RECOIL, UpgradeType.RELOAD, UpgradeType.ACCURACY,
                UpgradeType.RANGE, UpgradeType.SIGHT));
        DEFAULT_FIELDS.put("sniper_rifle", Set.of(UpgradeType.PENETRATION, UpgradeType.MAGAZINE, UpgradeType.NOISE, UpgradeType.RECOIL,
                UpgradeType.RELOAD, UpgradeType.RANGE));
        DEFAULT_FIELDS.put("gatling_gun", Set.of(UpgradeType.PENETRATION, UpgradeType.MAGAZINE, UpgradeType.RECOIL, UpgradeType.RELOAD,
                UpgradeType.ACCURACY, UpgradeType.SIGHT));
    }

    private static final Map<UpgradeType, String> VALUE_COMMENTS = Map.of(
            UpgradeType.PENETRATION, "Extra armor piercing per tier, in percentage points (10 = bullets ignore 10% more of the armor). Capped at 100%.",
            UpgradeType.MAGAZINE, "Extra rounds per tier, in percent of the magazine size (rounded).",
            UpgradeType.NOISE, "How much shorter the distance at which the shot can be heard is, per tier, in percent.",
            UpgradeType.RECOIL, "Less camera kick per tier, in percent.",
            UpgradeType.RELOAD, "Less time to load each round per tier, in percent.",
            UpgradeType.ACCURACY, "Less spread per tier, in percent.",
            UpgradeType.RANGE, "Faster bullets per tier, in percent. They fly that much further and drop less.",
            UpgradeType.SIGHT, "Extra zoom while aiming per sight tier, in percent (25 = 1.25x). Without a sight, aiming zooms 1.1x.");

    private static final Map<UpgradeType, ModConfigSpec.ConfigValue<List<? extends Number>>> VALUES = new EnumMap<>(UpgradeType.class);
    private static final Map<String, ModConfigSpec.ConfigValue<List<? extends String>>> FIELDS = new LinkedHashMap<>();

    static void define(ModConfigSpec.Builder builder) {
        builder.comment("Weapon upgrades, installed at the weapon workbench.").push("weaponUpgrades");

        builder.comment("Effect of each upgrade type for tiers I, II and III. Reductions are capped at 95%.").push("tiers");
        for (UpgradeType type : UpgradeType.values()) {
            List<Double> defaults = List.of(type.defaultValue(1), type.defaultValue(2), type.defaultValue(3));
            VALUES.put(type, builder.comment(VALUE_COMMENTS.get(type)).<Number>defineList(List.of(type.getSerializedName()), () -> defaults,
                    () -> 0.0, value -> value instanceof Number number && number.doubleValue() >= 0.0 && number.doubleValue() <= 1000.0,
                    ModConfigSpec.Range.of(WeaponUpgrades.MAX_TIER, WeaponUpgrades.MAX_TIER)));
        }
        builder.pop();

        builder.comment("Upgrade types each weapon accepts, i.e. which fields open around it in the workbench.",
                "Types: " + String.join(", ", Arrays.stream(UpgradeType.values()).map(UpgradeType::getSerializedName).toList()) + ".",
                "Parts already installed of a type that is not listed here stay on the weapon but have no effect.").push("fields");
        for (Map.Entry<String, Set<UpgradeType>> entry : DEFAULT_FIELDS.entrySet()) {
            // In field order, so the file reads like the workbench.
            List<String> defaults = Arrays.stream(UpgradeType.values()).filter(entry.getValue()::contains).map(UpgradeType::getSerializedName).toList();
            FIELDS.put(entry.getKey(), builder.<String>defineList(List.of(entry.getKey()), () -> defaults, () -> "",
                    value -> value instanceof String name && UpgradeType.byName(name) != null, null));
        }
        builder.pop();

        builder.pop();
    }

    // Effect of one tier, as configured. Before the config is loaded (e.g. in the main menu) the defaults apply.
    public static double value(UpgradeType type, int tier) {
        List<? extends Number> values = get(VALUES.get(type));
        return values.get(tier - 1).doubleValue();
    }

    // Whether a weapon (by item name) accepts an upgrade type.
    public static boolean accepts(String weaponName, UpgradeType type) {
        ModConfigSpec.ConfigValue<List<? extends String>> fields = FIELDS.get(weaponName);
        return fields != null && get(fields).contains(type.getSerializedName());
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return ServerConfig.SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private UpgradeConfig() {
    }
}
