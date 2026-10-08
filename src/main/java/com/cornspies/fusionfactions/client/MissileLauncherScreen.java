package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.missile.LaunchStatus;
import com.cornspies.fusionfactions.missile.MissileFlight;
import com.cornspies.fusionfactions.missile.MissileItem;
import com.cornspies.fusionfactions.missile.MissileLauncherMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Missile launcher screen (DESIGN.md §6.4): missile and designator slots, a readout with the target, the
// distance and why the launcher can't fire (from the server), the power bar if power is required, and the
// launch button. The button only sends "launch pressed"; the server decides.
public class MissileLauncherScreen extends AbstractContainerScreen<MissileLauncherMenu> {
    private static final Identifier BACKGROUND = FusionFactions.id("textures/gui/container/missile_launcher.png");
    // The readout panel's text area, matching tools/missile_textures.py.
    private static final int TEXT_X = 53;
    private static final int TEXT_Y = 21;
    private static final int TEXT_WIDTH = 113;
    private static final int TEXT_COLOR = 0xFF7CE08F;
    private static final int TEXT_DIM = 0xFF4E8A5B;
    // Power bar left of the slots, drawn only when the server requires power.
    private static final int BAR_X = 9;
    private static final int BAR_Y = 18;
    private static final int BAR_HEIGHT = 56;

    private Button launchButton;

    public MissileLauncherScreen(MissileLauncherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 176);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.launchButton = this.addRenderableWidget(Button.builder(Component.translatable("container.fusionfactions.missile_launcher.launch"), button -> {
            if (this.menu.clickMenuButton(this.minecraft.player, MissileLauncherMenu.LAUNCH_BUTTON)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MissileLauncherMenu.LAUNCH_BUTTON);
            }
        }).bounds(this.leftPos + 49, this.topPos + 60, 121, 20).build());
        this.updateButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateButton();
    }

    private void updateButton() {
        this.launchButton.active = this.menu.status().isReady();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (MissileConfig.requirePower()) {
            int x = this.leftPos + BAR_X;
            int top = this.topPos + BAR_Y;
            graphics.fill(x - 1, top - 1, x + 9, top + BAR_HEIGHT + 1, 0xFF373737);
            graphics.fill(x, top, x + 8, top + BAR_HEIGHT, 0xFF1A1A1A);
            int filled = Math.round(BAR_HEIGHT * this.menu.energy());
            graphics.fill(x, top + BAR_HEIGHT - filled, x + 8, top + BAR_HEIGHT, 0xFFE0402A);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int y = TEXT_Y;
        GlobalPos target = this.menu.target();
        if (target == null) {
            graphics.text(this.font, Component.translatable("container.fusionfactions.missile_launcher.no_target"), TEXT_X, y, TEXT_DIM, false);
        } else {
            BlockPos pos = target.pos();
            graphics.text(this.font, Component.translatable("container.fusionfactions.missile_launcher.target", pos.getX(), pos.getY(), pos.getZ()),
                    TEXT_X, y, TEXT_COLOR, false);
            y += 10;
            int distance = (int) MissileFlight.horizontalDistance(this.menu.pos(), pos);
            Component line = this.menu.missile().getItem() instanceof MissileItem missile
                    ? Component.translatable("container.fusionfactions.missile_launcher.distance_range", distance, MissileConfig.range(missile.tier()))
                    : Component.translatable("container.fusionfactions.missile_launcher.distance", distance);
            graphics.text(this.font, line, TEXT_X, y, TEXT_COLOR, false);
        }
        LaunchStatus status = this.menu.status();
        graphics.textWithWordWrap(this.font, status.message(), TEXT_X, TEXT_Y + 20, TEXT_WIDTH, TEXT_COLOR);
    }
}
