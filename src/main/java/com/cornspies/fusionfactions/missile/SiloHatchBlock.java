package com.cornspies.fusionfactions.missile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// The top of a missile silo (DESIGN.md §6.8). It opens when a missile leaves and closes again a few seconds later.
public class SiloHatchBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final int OPEN_TICKS = 80;

    public SiloHatchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false).setValue(SiloStructure.FORMED, false));
    }

    public static void open(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SiloHatchBlock) {
            if (!state.getValue(OPEN)) {
                level.setBlockAndUpdate(pos, state.setValue(OPEN, true));
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
            }
            level.scheduleTick(pos, state.getBlock(), OPEN_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            level.setBlockAndUpdate(pos, state.setValue(OPEN, false));
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, SiloStructure.FORMED);
    }
}
