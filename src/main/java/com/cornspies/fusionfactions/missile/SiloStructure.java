package com.cornspies.fusionfactions.missile;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

// The missile silo multiblock (DESIGN.md §6.8): a 3x3 shaft, 4 blocks deep. From the top: a ring of casing with
// the hatch in the middle, two rings of casing around an empty shaft, and a full floor of casing. The
// controller takes the place of one casing in any of the three rings.
public final class SiloStructure {
    private static final int[][] RING = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };
    // Layers below the hatch.
    public static final int DEPTH = 3;
    // Where a casing or the controller sits in a formed silo, for its connected look: 0 while the silo isn't
    // complete, otherwise 1 + layer * 9 + (dx + 1) * 3 + (dz + 1), with the layer counted down from the hatch
    // and dx, dz the offset from the shaft (tools/silo_textures.py makes a model for each).
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 36);
    // Whether the hatch belongs to a complete silo.
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    // The best match for a controller: the hatch position if the silo is complete, and how many blocks are
    // missing or wrong otherwise (for the closest arrangement).
    public record Match(@Nullable BlockPos hatch, int missing) {
    }

    public static Match find(BlockGetter level, BlockPos controller) {
        BlockPos best = null;
        int fewest = Integer.MAX_VALUE;
        for (int layer = 0; layer < DEPTH; layer++) {
            for (int[] offset : RING) {
                BlockPos hatch = controller.offset(-offset[0], layer, -offset[1]);
                int missing = missing(level, hatch, controller);
                if (missing < fewest) {
                    fewest = missing;
                    best = hatch;
                }
            }
        }
        return new Match(fewest == 0 ? best : null, fewest);
    }

    // Blocks that don't match the silo with its hatch at the given position.
    private static int missing(BlockGetter level, BlockPos hatch, BlockPos controller) {
        int missing = level.getBlockState(hatch).is(ModBlocks.SILO_HATCH.get()) ? 0 : 1;
        for (int layer = 0; layer <= DEPTH; layer++) {
            BlockPos center = hatch.below(layer);
            for (int[] offset : RING) {
                BlockPos pos = center.offset(offset[0], 0, offset[1]);
                if (!pos.equals(controller) && !isCasing(level.getBlockState(pos))) {
                    missing++;
                }
            }
            if (layer == DEPTH) {
                missing += isCasing(level.getBlockState(center)) ? 0 : 1;
            } else if (layer > 0) {
                missing += level.getBlockState(center).isAir() ? 0 : 1;
            }
        }
        return missing;
    }

    private static boolean isCasing(BlockState state) {
        return state.is(ModBlocks.SILO_CASING.get());
    }

    public static int part(BlockPos hatch, BlockPos pos) {
        int layer = hatch.getY() - pos.getY();
        return 1 + layer * 9 + (pos.getX() - hatch.getX() + 1) * 3 + (pos.getZ() - hatch.getZ() + 1);
    }

    // Every position of the silo with its hatch at the given position, from the hatch layer down.
    public static Iterable<BlockPos> layer(BlockPos hatch, int layer) {
        BlockPos center = hatch.below(layer);
        return BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1));
    }

    // Gives the silo blocks of one layer their formed (or plain) look. Only changes the block state, so
    // neighbours aren't updated.
    public static void setFormed(Level level, BlockPos hatch, int layer, boolean formed) {
        for (BlockPos pos : layer(hatch, layer)) {
            BlockState state = level.getBlockState(pos);
            BlockState changed = state;
            if (state.hasProperty(PART) && (state.is(ModBlocks.SILO_CASING.get()) || state.is(ModBlocks.SILO_CONTROLLER.get()))) {
                changed = state.setValue(PART, formed ? part(hatch, pos) : 0);
            } else if (state.is(ModBlocks.SILO_HATCH.get())) {
                changed = state.setValue(FORMED, formed);
            }
            if (changed != state) {
                level.setBlock(pos.immutable(), changed, Block.UPDATE_CLIENTS);
            }
        }
    }

    private SiloStructure() {
    }
}
