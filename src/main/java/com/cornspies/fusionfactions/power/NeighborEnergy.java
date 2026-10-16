package com.cornspies.fusionfactions.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;

// Pushes FE from a block's buffer into the blocks around it that take FE: cables, machines, missile blocks and
// other mods' blocks. Each tick it starts at the next side, so no neighbor is always first.
public final class NeighborEnergy {
    @SuppressWarnings("unchecked")
    private final @Nullable BlockCapabilityCache<EnergyHandler, @Nullable Direction>[] caches = new BlockCapabilityCache[6];
    private int next;

    public int push(ServerLevel level, BlockPos pos, EnergyHandler from, int max) {
        int moved = 0;
        for (int i = 0; i < 6 && moved < max; i++) {
            Direction side = Direction.from3DDataValue((this.next + i) % 6);
            BlockCapabilityCache<EnergyHandler, @Nullable Direction> cache = this.caches[side.get3DDataValue()];
            if (cache == null) {
                cache = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, pos.relative(side), side.getOpposite());
                this.caches[side.get3DDataValue()] = cache;
            }
            EnergyHandler target = cache.getCapability();
            if (target != null) {
                moved += EnergyHandlerUtil.move(from, target, max - moved, null);
            }
        }
        this.next = (this.next + 1) % 6;
        return moved;
    }
}
