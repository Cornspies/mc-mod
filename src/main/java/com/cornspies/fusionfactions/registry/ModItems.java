package com.cornspies.fusionfactions.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.MissileItem;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.missile.TargetDesignatorItem;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.WeaponStats;
import com.cornspies.fusionfactions.weapon.Weapons;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.UseEffects;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FusionFactions.MODID);

    public static final DeferredItem<WeaponItem> PISTOL = weapon("pistol", Weapons.PISTOL);
    public static final DeferredItem<WeaponItem> AUTO_PISTOL = weapon("auto_pistol", Weapons.AUTO_PISTOL);
    public static final DeferredItem<WeaponItem> SMG = weapon("smg", Weapons.SMG);
    public static final DeferredItem<WeaponItem> SHOTGUN = weapon("shotgun", Weapons.SHOTGUN);
    public static final DeferredItem<WeaponItem> SNIPER_RIFLE = weapon("sniper_rifle", Weapons.SNIPER_RIFLE);
    public static final DeferredItem<WeaponItem> GATLING_GUN = weapon("gatling_gun", Weapons.GATLING_GUN);
    // The one shared ammo item (D20).
    public static final DeferredItem<Item> AMMO = ITEMS.registerSimpleItem("ammo");
    // Crafting parts (D27).
    public static final DeferredItem<Item> BARREL = ITEMS.registerSimpleItem("barrel");
    public static final DeferredItem<Item> FUSION_CORE = ITEMS.registerSimpleItem("fusion_core");
    public static final DeferredItem<Item> GRIP = ITEMS.registerSimpleItem("grip");
    public static final DeferredItem<BlockItem> WEAPON_WORKBENCH = ITEMS.registerSimpleBlockItem(ModBlocks.WEAPON_WORKBENCH);

    // Missiles (DESIGN.md §6.4).
    public static final DeferredItem<TargetDesignatorItem> TARGET_DESIGNATOR = ITEMS.registerItem("target_designator", TargetDesignatorItem::new,
            properties -> properties.stacksTo(1));
    public static final DeferredItem<BlockItem> MISSILE_LAUNCHER = ITEMS.registerSimpleBlockItem(ModBlocks.MISSILE_LAUNCHER);
    public static final DeferredItem<BlockItem> SILO_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.SILO_CASING);
    public static final DeferredItem<BlockItem> SILO_HATCH = ITEMS.registerSimpleBlockItem(ModBlocks.SILO_HATCH);
    public static final DeferredItem<BlockItem> SILO_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.SILO_CONTROLLER);
    public static final DeferredItem<BlockItem> RADAR = ITEMS.registerSimpleBlockItem(ModBlocks.RADAR);
    public static final DeferredItem<BlockItem> INTERCEPTOR = ITEMS.registerSimpleBlockItem(ModBlocks.INTERCEPTOR);

    // Movement speed while aiming down sights, as a share of walking speed.
    private static final float AIM_MOVE_SPEED = 0.8F;

    public static final List<DeferredItem<WeaponItem>> WEAPONS = List.of(PISTOL, AUTO_PISTOL, SMG, SHOTGUN, SNIPER_RIFLE, GATLING_GUN);

    // Upgrade parts (D31–D33), one per type and tier, named like "magazine_upgrade_2". Crate-only: no recipes.
    private static final Map<UpgradeType, List<DeferredItem<UpgradeItem>>> UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        for (UpgradeType type : UpgradeType.values()) {
            List<DeferredItem<UpgradeItem>> tiers = new ArrayList<>();
            for (int tier = 1; tier <= WeaponUpgrades.MAX_TIER; tier++) {
                int t = tier;
                tiers.add(ITEMS.registerItem(type.getSerializedName() + "_upgrade_" + tier, properties -> new UpgradeItem(type, t, properties),
                        properties -> properties.stacksTo(16)));
            }
            UPGRADES.put(type, List.copyOf(tiers));
        }
    }

    // One missile item per type and tier (D48), named like "explosive_missile_2".
    private static final Map<MissileType, List<DeferredItem<MissileItem>>> MISSILES = new EnumMap<>(MissileType.class);

    static {
        for (MissileType type : MissileType.values()) {
            List<DeferredItem<MissileItem>> tiers = new ArrayList<>();
            for (int tier = 1; tier <= MissileType.MAX_TIER; tier++) {
                int t = tier;
                tiers.add(ITEMS.registerItem(type.getSerializedName() + "_missile_" + tier, properties -> new MissileItem(type, t, properties),
                        properties -> properties.stacksTo(4)));
            }
            MISSILES.put(type, List.copyOf(tiers));
        }
    }

    public static DeferredItem<MissileItem> missile(MissileType type, int tier) {
        return MISSILES.get(type).get(tier - 1);
    }

    public static DeferredItem<UpgradeItem> upgrade(UpgradeType type, int tier) {
        return UPGRADES.get(type).get(tier - 1);
    }

    private static DeferredItem<WeaponItem> weapon(String name, WeaponStats stats) {
        return ITEMS.registerItem(name, properties -> new WeaponItem(stats, properties), properties -> properties
                .stacksTo(1)
                // New weapons come loaded.
                .component(ModDataComponents.AMMO.get(), stats.magazineSize())
                // Using a gun means aiming down sights (D39): no sprinting, and a bit slower than walking
                // (vanilla would slow to 20%, as for bows).
                .component(DataComponents.USE_EFFECTS, new UseEffects(false, true, AIM_MOVE_SPEED)));
    }

    private ModItems() {
    }
}
