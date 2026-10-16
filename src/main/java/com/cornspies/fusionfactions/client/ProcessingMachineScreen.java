package com.cornspies.fusionfactions.client;

import java.util.ArrayList;
import java.util.List;

import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.ProcessingMachineBlockEntity;
import com.cornspies.fusionfactions.power.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// Screen of a processing machine (crusher, chemical mixer, circuit fabricator, assembler): the arrow between inputs
// and output fills as the recipe progresses. Hovering it shows the state, which the assembler has no readout for.
public class ProcessingMachineScreen extends MachineScreen {
    private static final int ARROW_WIDTH = 22;
    private static final int ARROW_HEIGHT = 16;
    private static final int CAPACITY = 50_000;

    private final Machine machine;

    public ProcessingMachineScreen(Machine machine, MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, machine.id());
        this.machine = machine;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int total = this.menu.data(1);
        int width = total <= 0 ? 0 : Math.round(ARROW_WIDTH * Math.clamp(this.menu.data(0) / (float) total, 0.0F, 1.0F));
        if (width > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.background, this.leftPos + this.machine.arrowX(), this.topPos + this.machine.arrowY(), 176.0F, 0.0F,
                    width, ARROW_HEIGHT, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.isHovering(this.machine.arrowX(), this.machine.arrowY(), ARROW_WIDTH, ARROW_HEIGHT, mouseX, mouseY)) {
            List<Component> text = new ArrayList<>();
            for (Line line : this.lines()) {
                text.add(line.text());
            }
            graphics.setComponentTooltipForNextFrame(this.font, text, mouseX, mouseY);
        }
    }

    @Override
    protected boolean hasReadout() {
        return this.machine.hasReadout();
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
        List<Line> lines = new ArrayList<>();
        switch (ProcessingMachineBlockEntity.status(this.menu)) {
            case IDLE -> lines.add(new Line(Component.translatable("fusionfactions.machine.idle"), DIM));
            case NO_POWER -> lines.add(new Line(Component.translatable("fusionfactions.machine.no_power"), WARNING));
            case OUTPUT_FULL -> lines.add(new Line(Component.translatable("fusionfactions.machine.output_full"), WARNING));
            case WORKING -> {
                lines.add(new Line(Component.translatable("fusionfactions.machine.working"), TEXT));
                lines.add(new Line(Component.translatable("fusionfactions.machine.rate", this.menu.data(4)), TEXT));
            }
        }
        int total = this.menu.data(1);
        if (total > 0 && this.menu.data(0) > 0) {
            lines.add(new Line(Component.translatable("fusionfactions.machine.progress", 100 * this.menu.data(0) / total), TEXT));
        }
        return lines;
    }
}
