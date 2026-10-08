package com.cornspies.fusionfactions.weapon.upgrade;

import java.util.function.Consumer;

import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// An upgrade part (D31–D33): one type at one tier, installed at the weapon workbench.
public class UpgradeItem extends Item {
    private final UpgradeType type;
    private final int tier;

    public UpgradeItem(UpgradeType type, int tier, Item.Properties properties) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    public UpgradeType type() {
        return this.type;
    }

    public int tier() {
        return this.tier;
    }

    // Tier colors match the supply crate rarities (D15): blue, orange, red.
    public static ChatFormatting tierColor(int tier) {
        return switch (tier) {
            case 1 -> ChatFormatting.BLUE;
            case 2 -> ChatFormatting.GOLD;
            default -> ChatFormatting.RED;
        };
    }

    public static Component tierName(int tier) {
        return Component.translatable("upgrade.fusionfactions.tier." + tier);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(tierColor(this.tier));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(UpgradeRules.describe(this.type, this.tier).withStyle(ChatFormatting.GRAY));
        MutableComponent weapons = null;
        for (var weapon : ModItems.WEAPONS) {
            if (UpgradeRules.accepts(weapon.get(), this.type)) {
                Component name = weapon.get().getName(new ItemStack(weapon.get()));
                weapons = weapons == null ? name.copy() : weapons.append(", ").append(name);
            }
        }
        builder.accept(weapons == null
                ? Component.translatable("tooltip.fusionfactions.upgrade.fits_none").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.fusionfactions.upgrade.fits", weapons).withStyle(ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.fusionfactions.upgrade.install").withStyle(ChatFormatting.DARK_GRAY));
    }

    // True if the stack is an upgrade part of this type.
    public static boolean isPart(ItemStack stack, UpgradeType type) {
        return stack.getItem() instanceof UpgradeItem part && part.type == type;
    }
}
