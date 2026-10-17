package com.cornspies.fusionfactions.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeRules;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;
import com.cornspies.fusionfactions.workbench.WeaponWorkbenchMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Weapon workbench screen (DESIGN.md §4.5): the weapon in the middle, upgrade fields around it. Fields
// the weapon doesn't accept are covered by a lock; a tactical helmet only has the implant field. Hovering an empty
// field explains what goes there.
public class WeaponWorkbenchScreen extends AbstractContainerScreen<WeaponWorkbenchMenu> {
    private static final Identifier BACKGROUND = FusionFactions.id("textures/gui/container/weapon_workbench.png");
    private static final Identifier LOCKED = FusionFactions.id("container/weapon_workbench/locked");

    public WeaponWorkbenchScreen(WeaponWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 176);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        for (Slot slot : this.menu.slots) {
            // With a helmet in, the implant field sits where the sight field is.
            boolean implantField = slot instanceof WeaponWorkbenchMenu.FieldSlot field && field.type() == UpgradeType.SIGHT && this.menu.hasHelmet();
            if (slot instanceof WeaponWorkbenchMenu.FieldSlot field && !field.isOpen() && !implantField) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCKED, this.leftPos + slot.x, this.topPos + slot.y, 16, 16);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.hoveredSlot == null || this.hoveredSlot.hasItem() || !this.menu.getCarried().isEmpty()) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        if (this.hoveredSlot instanceof WeaponWorkbenchMenu.FieldSlot field) {
            UpgradeType type = field.type();
            lines.add(Component.translatable(type.translationKey()));
            if (field.isOpen()) {
                for (int tier = 1; tier <= WeaponUpgrades.MAX_TIER; tier++) {
                    lines.add(UpgradeItem.tierName(tier).copy().withStyle(UpgradeItem.tierColor(tier))
                            .append(Component.literal(": ").append(UpgradeRules.describe(type, tier)).withStyle(ChatFormatting.GRAY)));
                }
            } else {
                lines.add(Component.translatable(this.menu.weapon().isEmpty() ? "container.fusionfactions.weapon_workbench.no_weapon"
                        : "container.fusionfactions.weapon_workbench.locked").withStyle(ChatFormatting.GRAY));
            }
        } else if (this.hoveredSlot instanceof WeaponWorkbenchMenu.ImplantSlot) {
            lines.add(ModItems.THREAT_WARNING_IMPLANT.get().getName(ItemStack.EMPTY));
            lines.add(Component.translatable("container.fusionfactions.weapon_workbench.implant_slot").withStyle(ChatFormatting.GRAY));
        } else if (this.hoveredSlot.index == 0) {
            lines.add(Component.translatable("container.fusionfactions.weapon_workbench.weapon_slot").withStyle(ChatFormatting.GRAY));
        } else {
            return;
        }
        graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
    }
}
