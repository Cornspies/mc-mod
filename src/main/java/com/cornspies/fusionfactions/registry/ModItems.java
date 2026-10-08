package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.WeaponStats;
import com.cornspies.fusionfactions.weapon.Weapons;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.UseEffects;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FusionFactions.MODID);

    public static final DeferredItem<WeaponItem> PISTOL = weapon("pistol", Weapons.PISTOL);
    // The one shared ammo item (D20).
    public static final DeferredItem<Item> AMMO = ITEMS.registerSimpleItem("ammo");

    private static DeferredItem<WeaponItem> weapon(String name, WeaponStats stats) {
        return ITEMS.registerItem(name, properties -> new WeaponItem(stats, properties), properties -> properties
                .stacksTo(1)
                // New weapons come loaded.
                .component(ModDataComponents.AMMO.get(), stats.magazineSize())
                // Vanilla slows players to 20% and stops sprinting while an item is in use. Not for guns.
                .component(DataComponents.USE_EFFECTS, new UseEffects(true, true, 1.0F)));
    }

    private ModItems() {
    }
}
