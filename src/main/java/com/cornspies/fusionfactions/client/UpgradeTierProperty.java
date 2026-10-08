package com.cornspies.fusionfactions.client;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;

// Item model property "fusionfactions:upgrade_tier": the tier (0 to 3) of the weapon's part of one upgrade
// type. The weapon item models use it to show installed parts (D34).
public record UpgradeTierProperty(UpgradeType upgrade) implements RangeSelectItemModelProperty {
    public static final Identifier ID = FusionFactions.id("upgrade_tier");
    public static final MapCodec<UpgradeTierProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(UpgradeType.CODEC.fieldOf("upgrade").forGetter(UpgradeTierProperty::upgrade)).apply(instance, UpgradeTierProperty::new));

    public static void register(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(ID, MAP_CODEC);
    }

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return WeaponUpgrades.of(stack).tier(this.upgrade);
    }

    @Override
    public MapCodec<UpgradeTierProperty> type() {
        return MAP_CODEC;
    }
}
