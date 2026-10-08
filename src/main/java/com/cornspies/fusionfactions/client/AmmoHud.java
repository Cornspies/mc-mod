package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.weapon.WeaponItem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Ammo counter next to the hotbar while a weapon is held (DESIGN.md §4.2).
public final class AmmoHud {
    private static final int COLOR = 0xFFFFFFFF;
    private static final int COLOR_EMPTY = 0xFFFF5555;
    // Just right of the hotbar, which is 182 pixels wide and centered.
    private static final int OFFSET_X = 98;
    private static final int OFFSET_Y = 15;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gui.hud.isHidden() || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return;
        }
        int ammo = WeaponItem.getAmmo(stack);
        int magazine = weapon.stats(stack).magazineSize();
        Component text;
        if (WeaponItem.isReloading(stack)) {
            text = Component.translatable("hud.fusionfactions.reloading", ammo, magazine);
        } else if (player.hasInfiniteMaterials()) {
            text = Component.translatable("hud.fusionfactions.ammo.infinite", ammo, magazine);
        } else {
            text = Component.translatable("hud.fusionfactions.ammo", ammo, magazine, WeaponItem.countAmmo(player));
        }
        graphics.text(minecraft.font, text, graphics.guiWidth() / 2 + OFFSET_X, graphics.guiHeight() - OFFSET_Y,
                ammo > 0 ? COLOR : COLOR_EMPTY, true);
    }

    private AmmoHud() {
    }
}
