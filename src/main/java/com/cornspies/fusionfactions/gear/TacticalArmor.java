package com.cornspies.fusionfactions.gear;

import java.util.Map;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModTags;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

// The tactical helmet's armor (D63, DESIGN.md §6b.7): diamond-level protection (3 armor, 2 toughness, diamond
// durability), repaired with aluminum. Drawn from assets/fusionfactions/equipment/tactical.json. A threat-warning
// implant is installed into it at the weapon workbench (workbench.ImplantSlot).
public final class TacticalArmor {
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, FusionFactions.id("tactical"));
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(33, Map.of(ArmorType.HELMET, 3), 10, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.0F,
            ModTags.REPAIRS_TACTICAL_ARMOR, ASSET);

    public static boolean hasImplant(ItemStack stack) {
        return stack.has(ModDataComponents.IMPLANT.get());
    }

    private TacticalArmor() {
    }
}
