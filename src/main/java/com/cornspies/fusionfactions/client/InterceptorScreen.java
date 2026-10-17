package com.cornspies.fusionfactions.client;

import java.util.ArrayList;
import java.util.List;

import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.missile.InterceptorBlockEntity;
import com.cornspies.fusionfactions.power.MachineMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// Interceptor screen (DESIGN.md §6b.8): its rocket slot, what it's doing and its energy.
public class InterceptorScreen extends MachineScreen {
    public InterceptorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "interceptor");
    }

    @Override
    protected int energyPerMille() {
        return this.menu.data(0);
    }

    @Override
    protected int capacity() {
        return InterceptorBlockEntity.CAPACITY;
    }

    @Override
    protected List<Line> lines() {
        List<Line> lines = new ArrayList<>();
        switch (InterceptorBlockEntity.status(this.menu)) {
            case READY -> lines.add(new Line(Component.translatable("fusionfactions.interceptor.state.ready"), TEXT));
            case RELOADING -> lines.add(new Line(Component.translatable("fusionfactions.interceptor.state.reloading"), TEXT));
            case NO_POWER -> lines.add(new Line(Component.translatable("fusionfactions.interceptor.state.no_power"), WARNING));
            case NO_ROCKETS -> lines.add(new Line(Component.translatable("fusionfactions.interceptor.state.no_rockets"), WARNING));
            case DISABLED -> lines.add(new Line(Component.translatable("fusionfactions.interceptor.state.disabled", this.menu.data(3)), WARNING));
        }
        lines.add(new Line(Component.translatable("fusionfactions.interceptor.rockets", this.menu.data(1)), TEXT));
        lines.add(new Line(Component.translatable("fusionfactions.interceptor.range", MissileConfig.interceptorRange()), DIM));
        return lines;
    }
}
