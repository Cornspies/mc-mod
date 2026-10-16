package com.cornspies.fusionfactions.client;

import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.power.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

// A machine's screen (textures from tools/power_textures.py): the readout on the left lists the machine's state (the
// assembler has none), the gauge on the right shows its energy (hover it for the numbers).
public abstract class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    // Readout text area and gauge inside, matching READOUT and GAUGE in tools/power_textures.py.
    private static final int READOUT_X = 10;
    private static final int READOUT_Y = 19;
    private static final int READOUT_WIDTH = 55;
    private static final int LINE_HEIGHT = 10;
    private static final int GAUGE_X0 = 156;
    private static final int GAUGE_X1 = 169;
    private static final int GAUGE_Y0 = 17;
    private static final int GAUGE_Y1 = 69;
    protected static final int TEXT = 0xFF7CE08F;
    protected static final int DIM = 0xFF4E8A5B;
    protected static final int WARNING = 0xFFE0A030;

    protected final Identifier background;

    protected MachineScreen(MachineMenu menu, Inventory inventory, Component title, String texture) {
        super(menu, inventory, title, 176, 166);
        this.background = FusionFactions.id("textures/gui/container/" + texture + ".png");
    }

    // Stored energy from 0 to 1000, and the buffer's size, for the gauge.
    protected abstract int energyPerMille();

    protected abstract int capacity();

    // The readout's lines, top to bottom, with their colors.
    protected abstract List<Line> lines();

    protected record Line(Component text, int color) {
    }

    protected boolean hasReadout() {
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.background, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        int filled = Math.round((GAUGE_Y1 - GAUGE_Y0) * Math.clamp(this.energyPerMille(), 0, 1000) / 1000.0F);
        if (filled > 0) {
            int x0 = this.leftPos + GAUGE_X0;
            int x1 = this.leftPos + GAUGE_X1;
            int y1 = this.topPos + GAUGE_Y1;
            graphics.fill(x0, y1 - filled, x1, y1, 0xFFE0402A);
            graphics.fill(x0, y1 - filled, x0 + 2, y1, 0xFFFF7A5C);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
        if (!this.hasReadout()) {
            return;
        }
        int y = READOUT_Y;
        for (Line line : this.lines()) {
            for (FormattedCharSequence part : this.font.split(line.text(), READOUT_WIDTH)) {
                graphics.text(this.font, part, READOUT_X, y, line.color(), false);
                y += LINE_HEIGHT;
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.isHovering(GAUGE_X0, GAUGE_Y0, GAUGE_X1 - GAUGE_X0, GAUGE_Y1 - GAUGE_Y0, mouseX, mouseY)) {
            Component text = Component.translatable("fusionfactions.machine.energy_tooltip", this.energyPerMille() / 10, String.format("%,d", this.capacity()));
            graphics.setTooltipForNextFrame(text, mouseX, mouseY);
        }
    }
}
