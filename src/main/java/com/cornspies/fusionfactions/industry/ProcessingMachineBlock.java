package com.cornspies.fusionfactions.industry;

import com.cornspies.fusionfactions.power.MachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// One of the processing machines (crusher, chemical mixer, circuit fabricator, assembler).
public class ProcessingMachineBlock extends MachineBlock {
    private final Machine machine;

    public ProcessingMachineBlock(Machine machine, BlockBehaviour.Properties properties) {
        super(properties);
        this.machine = machine;
    }

    public Machine machine() {
        return this.machine;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProcessingMachineBlockEntity(this.machine, pos, state);
    }
}
