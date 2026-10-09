package com.cornspies.fusionfactions.client;

import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.missile.RadarBlockEntity;
import com.cornspies.fusionfactions.missile.RadarMenu;
import com.cornspies.fusionfactions.util.Compass;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

// Radar screen (DESIGN.md §6.9): a scope with a blip per missile in range, north up, and a list of the contacts
// with their distance, direction and time to impact if they come down in range ("209 N: 14s"; red), or "passing".
public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private static final Identifier BACKGROUND = FusionFactions.id("textures/gui/container/radar.png");
    // The scope's middle and radius, and the list's text area, matching tools/missile_textures.py.
    private static final int SCOPE_X = 52;
    private static final int SCOPE_Y = 62;
    private static final int SCOPE_RADIUS = 41;
    private static final int LIST_X = 95;
    private static final int LIST_Y = 21;
    private static final int LIST_WIDTH = 72;
    private static final int LINE_HEIGHT = 9;
    private static final int MAX_LINES = 8;
    private static final int TEXT = 0xFF7CE08F;
    private static final int DIM = 0xFF4E8A5B;
    private static final int INCOMING = 0xFFE0402A;
    // Power bar along the bottom of the list, drawn only when the server requires power.
    private static final int BAR_Y = 104;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 110);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (MissileConfig.requirePower()) {
            int x0 = this.leftPos + 92;
            int x1 = this.leftPos + 170;
            int y = this.topPos + BAR_Y;
            graphics.fill(x0, y, x1, y + 3, 0xFF1A1A1A);
            graphics.fill(x0, y, x0 + Math.round((x1 - x0) * this.menu.energy()), y + 3, 0xFFE0402A);
        }
        // Blips: missiles coming down in range are red, the others green. They blink with the game clock.
        boolean on = this.minecraft.level == null || this.minecraft.level.getGameTime() / 10 % 2 == 0;
        int range = MissileConfig.radarRange();
        for (RadarBlockEntity.Contact contact : this.menu.contacts()) {
            int x = this.leftPos + SCOPE_X + Math.round((float) contact.dx() / range * SCOPE_RADIUS);
            int y = this.topPos + SCOPE_Y + Math.round((float) contact.dz() / range * SCOPE_RADIUS);
            int color = contact.impactSeconds() > 0 ? INCOMING : TEXT;
            graphics.fill(x - 1, y - 1, x + 2, y + 2, on ? color : (color & 0x00FFFFFF) | 0x80000000);
        }
        graphics.fill(this.leftPos + SCOPE_X, this.topPos + SCOPE_Y, this.leftPos + SCOPE_X + 1, this.topPos + SCOPE_Y + 1, 0xFFE0E0E0);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        int line = 0;
        for (FormattedCharSequence status : this.font.split(this.status(), LIST_WIDTH)) {
            graphics.text(this.font, status, LIST_X, LIST_Y + line++ * LINE_HEIGHT, TEXT, false);
        }
        List<RadarBlockEntity.Contact> contacts = this.menu.contacts();
        for (int i = 0; i < contacts.size() && line < MAX_LINES; i++) {
            RadarBlockEntity.Contact contact = contacts.get(i);
            int distance = (int) Math.round(Math.sqrt(contact.dx() * contact.dx() + contact.dz() * contact.dz()));
            Component direction = Component.translatable("fusionfactions.direction_short." + Compass.direction(contact.dx(), contact.dz()));
            Component text = contact.impactSeconds() > 0
                    ? Component.translatable("container.fusionfactions.radar.contact", distance, direction, contact.impactSeconds())
                    : Component.translatable("container.fusionfactions.radar.contact_passing", distance, direction);
            graphics.text(this.font, text, LIST_X, LIST_Y + line++ * LINE_HEIGHT, contact.impactSeconds() > 0 ? INCOMING : DIM, false);
        }
        int hidden = this.menu.contactCount() - contacts.size();
        if (hidden > 0 && line < MAX_LINES) {
            graphics.text(this.font, Component.translatable("container.fusionfactions.radar.more", hidden), LIST_X, LIST_Y + line * LINE_HEIGHT, DIM, false);
        }
    }

    private Component status() {
        return switch (this.menu.status()) {
            case ACTIVE -> this.menu.contactCount() == 0
                    ? Component.translatable("container.fusionfactions.radar.clear")
                    : Component.translatable("container.fusionfactions.radar.contacts", this.menu.contactCount());
            case NO_POWER -> Component.translatable("container.fusionfactions.radar.no_power");
            case DISABLED -> Component.translatable("container.fusionfactions.radar.disabled", this.menu.disabledSeconds());
        };
    }
}
