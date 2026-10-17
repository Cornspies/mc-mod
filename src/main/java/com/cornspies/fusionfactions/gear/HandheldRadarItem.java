package com.cornspies.fusionfactions.gear;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.RocketConfig;
import com.cornspies.fusionfactions.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// The handheld radar (D62, DESIGN.md §6b.7): held in either hand with charge left, it warns its holder when a
// rocket launcher locks on to them (weapon.LockWarning). It stores FE (the energy item capability, so the charging
// station and other mods' chargers fill it) and uses RocketConfig.radarDrain per tick while held.
public class HandheldRadarItem extends Item {
    public static final int CAPACITY = 100_000;
    // FE per tick a charger may put in.
    public static final int MAX_INSERT = 2_000;
    // The charge is taken once a second, so the stack isn't resent to the client every tick.
    private static final int DRAIN_INTERVAL = 20;
    private static final int BAR_COLOR = 0x55FFFF;

    public HandheldRadarItem(Item.Properties properties) {
        super(properties);
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    // Whether this stack is a handheld radar that can warn right now.
    public static boolean isActive(ItemStack stack) {
        return stack.getItem() instanceof HandheldRadarItem && energy(stack) > 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        boolean held = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!held || !(owner instanceof Player player) || player.hasInfiniteMaterials() || level.getGameTime() % DRAIN_INTERVAL != 0) {
            return;
        }
        int energy = energy(stack);
        if (energy > 0) {
            stack.set(ModDataComponents.ENERGY.get(), Math.max(0, energy - RocketConfig.radarDrain() * DRAIN_INTERVAL));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.fusionfactions.handheld_radar.energy", String.format("%,d", energy(stack)),
                String.format("%,d", CAPACITY)).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(energy(stack) > 0 ? "tooltip.fusionfactions.handheld_radar.use" : "tooltip.fusionfactions.handheld_radar.empty")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * energy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }
}
