package com.cornspies.fusionfactions.missile;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

// Silo casing (DESIGN.md §6.8). Plain until its silo is complete; then the controller sets its part, and it
// shows the formed, connected look for its place in the silo.
public class SiloCasingBlock extends Block {
    public SiloCasingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SiloStructure.PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SiloStructure.PART);
    }
}
