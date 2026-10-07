package com.cornspies.fusionfactions.supplydrop;

import java.util.Optional;

import com.cornspies.fusionfactions.config.ServerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

// Picks landing spots inside the drop zone (D16, DESIGN.md §5.2 step 2).
final class DropLocations {
    private static final int MAX_ATTEMPTS = 16;
    // Space needed above the landing spot for the crate to fall through.
    private static final int MIN_HEADROOM = 16;

    static BlockPos zoneCenter(ServerLevel level) {
        return switch (ServerConfig.ZONE_MODE.get()) {
            case SPAWN -> worldSpawn(level);
            case CENTER -> new BlockPos(ServerConfig.ZONE_CENTER_X.getAsInt(), 0, ServerConfig.ZONE_CENTER_Z.getAsInt());
        };
    }

    // The world spawn if it's in this dimension, otherwise 0 / 0.
    static BlockPos worldSpawn(ServerLevel level) {
        GlobalPos spawn = level.getServer().getRespawnData().globalPos();
        return spawn.dimension() == level.dimension() ? spawn.pos() : BlockPos.ZERO;
    }

    // Tries random spots in the zone. Loads (or generates) each candidate chunk.
    static Optional<BlockPos> pick(ServerLevel level, RandomSource random) {
        BlockPos center = zoneCenter(level);
        int radius = ServerConfig.ZONE_RADIUS.getAsInt();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int x = center.getX() + random.nextIntBetweenInclusive(-radius, radius);
            int z = center.getZ() + random.nextIntBetweenInclusive(-radius, radius);
            Optional<BlockPos> spot = surfaceAt(level, x, z).filter(pos -> isSafe(level, pos));
            if (spot.isPresent()) {
                return spot;
            }
        }
        return Optional.empty();
    }

    // The first free block above the ground at x / z. Empty if outside the world border or without room above.
    static Optional<BlockPos> surfaceAt(ServerLevel level, int x, int z) {
        if (!level.getWorldBorder().isWithinBounds(x, z)) {
            return Optional.empty();
        }
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (y + MIN_HEADROOM > level.getMaxY()) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(x, y, z));
    }

    // Rejects water, lava, treetops and ground the crate can't sit on.
    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockPos groundPos = pos.below();
        BlockState ground = level.getBlockState(groundPos);
        return ground.getFluidState().isEmpty()
                && !ground.is(BlockTags.LEAVES)
                && ground.isFaceSturdy(level, groundPos, Direction.UP);
    }

    private DropLocations() {
    }
}
