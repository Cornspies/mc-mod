package com.cornspies.fusionfactions.client;

import java.util.ArrayList;
import java.util.List;

import com.cornspies.fusionfactions.power.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// Charging station screen: the bar under the slot shows the item's charge.
public class ChargingStationScreen extends MachineScreen {
    // Inside of the charge bar (CHARGE_BAR in tools/power_textures.py).
    private static final int BAR_X0 = 63;
    private static final int BAR_X1 = 115;
    private static final int BAR_Y0 = 58;
    private static final int BAR_Y1 = 62;
    private static final int CAPACITY = 50_000;

    public ChargingStationScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "charging_station");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int charge = this.menu.data(1);
        if (charge > 0) {
            int width = Math.round((BAR_X1 - BAR_X0) * charge / 1000.0F);
            graphics.fill(this.leftPos + BAR_X0, this.topPos + BAR_Y0, this.leftPos + BAR_X0 + width, this.topPos + BAR_Y1, 0xFF5CE07A);
        }
    }

    @Override
    protected int energyPerMille() {
        return this.menu.data(0);
    }

    @Override
    protected int capacity() {
        return CAPACITY;
    }

    @Override
    protected List<Line> lines() {
        int charge = this.menu.data(1);
        int rate = this.menu.data(2);
        List<Line> lines = new ArrayList<>();
        if (charge < 0) {
            lines.add(new Line(Component.translatable("fusionfactions.charging.no_item"), DIM));
        } else if (rate > 0) {
            lines.add(new Line(Component.translatable("fusionfactions.charging.charging"), TEXT));
            lines.add(new Line(Component.translatable("fusionfactions.machine.rate", rate), TEXT));
        } else if (charge >= 1000) {
            lines.add(new Line(Component.translatable("fusionfactions.charging.item_full"), TEXT));
        } else {
            lines.add(new Line(Component.translatable("fusionfactions.charging.no_power"), WARNING));
        }
        if (charge >= 0) {
            lines.add(new Line(Component.translatable("fusionfactions.charging.item", charge / 10), TEXT));
        }
        lines.add(new Line(Component.translatable("fusionfactions.machine.energy", this.energyPerMille() / 10), TEXT));
        return lines;
    }
}
