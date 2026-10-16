package com.cornspies.fusionfactions.client;

import java.util.List;

import com.cornspies.fusionfactions.power.CombustionGeneratorBlockEntity;
import com.cornspies.fusionfactions.power.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// Combustion generator screen: the flame above the fuel slot burns down with the current fuel.
public class CombustionGeneratorScreen extends MachineScreen {
    // The flame outline in the panel and the lit flame on the sheet (tools/power_textures.py).
    private static final int FLAME_X = 81;
    private static final int FLAME_Y = 35;
    private static final int FLAME_SIZE = 14;
    private static final int CAPACITY = 100_000;

    public CombustionGeneratorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "combustion_generator");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int height = (int) Math.ceil(FLAME_SIZE * CombustionGeneratorBlockEntity.burnProgress(this.menu));
        if (height > 0) {
            int skip = FLAME_SIZE - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.background, this.leftPos + FLAME_X, this.topPos + FLAME_Y + skip, 176.0F, skip, FLAME_SIZE,
                    height, 256, 256);
        }
    }

    @Override
    protected int energyPerMille() {
        return this.menu.data(2);
    }

    @Override
    protected int capacity() {
        return CAPACITY;
    }

    @Override
    protected List<Line> lines() {
        int output = this.menu.data(3);
        Line status = output > 0 ? new Line(Component.translatable("fusionfactions.generator.burning"), TEXT)
                : this.menu.data(0) > 0 ? new Line(Component.translatable("fusionfactions.generator.full"), WARNING)
                : new Line(Component.translatable("fusionfactions.generator.no_fuel"), DIM);
        return List.of(status, new Line(Component.translatable("fusionfactions.machine.rate", output), output > 0 ? TEXT : DIM),
                new Line(Component.translatable("fusionfactions.machine.energy", this.energyPerMille() / 10), TEXT));
    }
}
