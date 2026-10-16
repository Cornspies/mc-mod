package com.cornspies.fusionfactions.power;

import com.cornspies.fusionfactions.config.PowerConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

// Solar panel (D67, DESIGN.md §6b.3): weak, free FE by day under open sky, half of it in rain, nothing at night.
// Pushes its FE into the blocks around it.
public class SolarPanelBlockEntity extends BlockEntity {
    private static final int CAPACITY = 10_000;
    private static final int MAX_PUSH = 2_000;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, 0, MAX_PUSH) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            SolarPanelBlockEntity.this.setChanged();
        }
    };
    private final NeighborEnergy neighbors = new NeighborEnergy();

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SOLAR_PANEL.get(), pos, state);
    }

    public EnergyHandler energy() {
        return this.energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        if (level instanceof ServerLevel serverLevel) {
            int output = output(serverLevel, pos);
            if (output > 0) {
                panel.energy.set((int) Math.min(CAPACITY, panel.energy.getAmountAsLong() + output));
            }
            panel.neighbors.push(serverLevel, pos, panel.energy, MAX_PUSH);
        }
    }

    // FE per tick right now.
    public static int output(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || !level.isBrightOutside() || !level.canSeeSky(pos.above())) {
            return 0;
        }
        int output = PowerConfig.solarOutput();
        return level.isRainingAt(pos.above()) || level.isThundering() ? output / 2 : output;
    }

    public Component status() {
        return Component.translatable("fusionfactions.solar_panel.status", output(this.level, this.worldPosition), this.energy.getAmountAsLong(), CAPACITY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Energy").ifPresent(this.energy::deserialize);
    }
}
