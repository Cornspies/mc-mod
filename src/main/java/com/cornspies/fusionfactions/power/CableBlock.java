package com.cornspies.fusionfactions.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.capabilities.Capabilities;

// FE cable (D67, DESIGN.md §6b.3). Connects to the cables next to it and to any block that takes or gives FE.
// Connected cables form a network (CableNetworks) that hands FE from generators to the blocks that take it.
public class CableBlock extends PipeBlock {
    public CableBlock(BlockBehaviour.Properties properties) {
        super(4.0F, properties);
        BlockState state = this.stateDefinition.any();
        for (var property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        for (Direction side : Direction.values()) {
            BlockPos neighbor = context.getClickedPos().relative(side);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(side), connects(context.getLevel(), neighbor, context.getLevel().getBlockState(neighbor), side));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        // A block next to the network changed, so the network's list of consumers may have too.
        if (level instanceof ServerLevel serverLevel) {
            CableNetworks.invalidate(serverLevel);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), connects(level, neighbourPos, neighbourState, directionToNeighbour));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel) {
            CableNetworks.invalidate(serverLevel);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        CableNetworks.invalidate(level);
    }

    private static boolean connects(LevelReader level, BlockPos neighbor, BlockState neighborState, Direction side) {
        if (neighborState.getBlock() instanceof CableBlock) {
            return true;
        }
        return level instanceof Level fullLevel && fullLevel.getCapability(Capabilities.Energy.BLOCK, neighbor, side.getOpposite()) != null;
    }
}
