package com.cornspies.fusionfactions.gear;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// The tactical helmet (D63): armor like a diamond helmet (TacticalArmor), which warns its wearer of lock-ons once a
// threat-warning implant is installed at the weapon workbench.
public class TacticalHelmetItem extends Item {
    public TacticalHelmetItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        if (TacticalArmor.hasImplant(stack)) {
            builder.accept(Component.translatable("tooltip.fusionfactions.tactical_helmet.implant").withStyle(ChatFormatting.AQUA));
        } else {
            builder.accept(Component.translatable("tooltip.fusionfactions.tactical_helmet.no_implant").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
