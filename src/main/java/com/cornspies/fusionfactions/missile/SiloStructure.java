package com.cornspies.fusionfactions.missile;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

// The missile silo multiblock (DESIGN.md §6.8): a 3x3 shaft, 4 blocks deep. From the top: a ring of casing with
// the hatch in the middle, two rings of casing around an empty shaft, and a full floor of casing. The
// controller takes the place of one casing in any of the three rings.
public final class SiloStructure {
    private static final int[][] RING = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };
    // Layers below the hatch.
    public static final int DEPTH = 3;

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

    private SiloStructure() {
    }
}
