package com.cornspies.fusionfactions.missile;

import java.util.function.Consumer;

import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// A missile (D48): one type at one tier, loaded into a launcher (tiers I–II) or a silo (D55).
public class MissileItem extends Item {
    // The highest tier the missile launcher block can fire; higher ones need the silo (D55).
    public static final int LAUNCHER_MAX_TIER = 2;

    private final MissileType type;
    private final int tier;

    public MissileItem(MissileType type, int tier, Item.Properties properties) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    public MissileType type() {
        return this.type;
    }

    public int tier() {
        return this.tier;
    }

    // What the missile does, with the configured numbers.
    public static MutableComponent effect(MissileType type, int tier) {
        String key = type.translationKey() + ".effect";
        return switch (type) {
            case EXPLOSIVE -> Component.translatable(key, Math.round(MissileConfig.explosivePower(tier)));
            case BREACHING -> Component.translatable(key, Math.round(MissileConfig.breachingPower(tier)));
            case EMP -> Component.translatable(key, MissileConfig.empRadius(tier), MissileConfig.empDisableTicks(tier) / 20);
            case INCENDIARY -> Component.translatable(key, Math.round(MissileConfig.incendiaryPower(tier)), MissileConfig.incendiaryFireRadius(tier));
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        // Same tier colors as the upgrade parts and supply crates.
        return super.getName(stack).copy().withStyle(UpgradeItem.tierColor(this.tier));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(effect(this.type, this.tier).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.fusionfactions.missile.range", MissileConfig.minRange(), MissileConfig.range(this.tier))
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(this.tier <= LAUNCHER_MAX_TIER ? "tooltip.fusionfactions.missile.launcher" : "tooltip.fusionfactions.missile.silo")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
