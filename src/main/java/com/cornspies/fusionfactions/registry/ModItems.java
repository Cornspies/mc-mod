package com.cornspies.fusionfactions.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.industry.Machine;
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
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
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

    // Resources (D69, DESIGN.md §6b.2): ores, what they drop, and what they are smelted or ground into.
    public static final DeferredItem<BlockItem> BAUXITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_BAUXITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_BAUXITE_ORE);
    public static final DeferredItem<BlockItem> NITER_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.NITER_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_NITER_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_NITER_ORE);
    public static final DeferredItem<BlockItem> RARE_EARTH_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.RARE_EARTH_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_RARE_EARTH_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_RARE_EARTH_ORE);
    public static final DeferredItem<BlockItem> LITHIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.LITHIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LITHIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_LITHIUM_ORE);
    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.registerSimpleItem("raw_bauxite");
    public static final DeferredItem<Item> NITER = ITEMS.registerSimpleItem("niter");
    public static final DeferredItem<Item> RAW_RARE_EARTH = ITEMS.registerSimpleItem("raw_rare_earth");
    public static final DeferredItem<Item> RAW_LITHIUM = ITEMS.registerSimpleItem("raw_lithium");
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> LITHIUM_INGOT = ITEMS.registerSimpleItem("lithium_ingot");
    public static final DeferredItem<Item> ALUMINUM_DUST = ITEMS.registerSimpleItem("aluminum_dust");
    public static final DeferredItem<Item> RARE_EARTH_DUST = ITEMS.registerSimpleItem("rare_earth_dust");
    public static final DeferredItem<Item> LITHIUM_DUST = ITEMS.registerSimpleItem("lithium_dust");

    // Power (D67).
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> SOLAR_PANEL = ITEMS.registerSimpleBlockItem(ModBlocks.SOLAR_PANEL);
    public static final DeferredItem<BlockItem> FE_CABLE = ITEMS.registerSimpleBlockItem(ModBlocks.FE_CABLE);
    public static final DeferredItem<BlockItem> CHARGING_STATION = ITEMS.registerSimpleBlockItem(ModBlocks.CHARGING_STATION);

    // Machines (D70) and the materials they make (D71, D72, DESIGN.md §6b.5). Rocket fuel also burns in furnaces and
    // generators, four times as long as coal.
    private static final Map<Machine, DeferredItem<BlockItem>> MACHINES = new EnumMap<>(Machine.class);

    static {
        for (Machine machine : Machine.values()) {
            MACHINES.put(machine, ITEMS.registerSimpleBlockItem(machine.id(), () -> ModBlocks.machine(machine)));
        }
    }

    public static final DeferredItem<Item> SILICON_DUST = ITEMS.registerSimpleItem("silicon_dust");
    public static final DeferredItem<Item> SILICON = ITEMS.registerSimpleItem("silicon");
    public static final DeferredItem<Item> SILICON_WAFER = ITEMS.registerSimpleItem("silicon_wafer");
    public static final DeferredItem<Item> BASIC_CHIP = ITEMS.registerSimpleItem("basic_chip");
    public static final DeferredItem<Item> ADVANCED_CHIP = ITEMS.registerSimpleItem("advanced_chip");
    public static final DeferredItem<Item> MILITARY_CHIP = ITEMS.registerSimpleItem("military_chip");
    public static final DeferredItem<Item> ROCKET_FUEL = ITEMS.registerSimpleItem("rocket_fuel", properties -> properties.component(DataComponents.COOKING_FUEL,
            new CookingFuel(new ResolvableInt.Constant(6400), ResolvableFloat.fromKey(ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER))));
    public static final DeferredItem<Item> BATTERY = ITEMS.registerSimpleItem("battery");
    public static final DeferredItem<Item> AIRFRAME = ITEMS.registerSimpleItem("airframe", properties -> properties.stacksTo(16));

    public static BlockItem machine(Machine machine) {
        return MACHINES.get(machine).get();
    }

    // Chips by missile tier (D71).
    public static Item chip(int tier) {
        return switch (tier) {
            case 1 -> BASIC_CHIP.get();
            case 2 -> ADVANCED_CHIP.get();
            default -> MILITARY_CHIP.get();
        };
    }

    public static final List<DeferredItem<BlockItem>> ORES = List.of(BAUXITE_ORE, DEEPSLATE_BAUXITE_ORE, NITER_ORE, DEEPSLATE_NITER_ORE, RARE_EARTH_ORE,
            DEEPSLATE_RARE_EARTH_ORE, LITHIUM_ORE, DEEPSLATE_LITHIUM_ORE);
    public static final List<DeferredItem<Item>> MATERIALS = List.of(RAW_BAUXITE, NITER, RAW_RARE_EARTH, RAW_LITHIUM, ALUMINUM_INGOT, LITHIUM_INGOT,
            ALUMINUM_DUST, RARE_EARTH_DUST, LITHIUM_DUST, SILICON_DUST, SILICON, SILICON_WAFER, BASIC_CHIP, ADVANCED_CHIP, MILITARY_CHIP, ROCKET_FUEL,
            BATTERY, AIRFRAME);

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
