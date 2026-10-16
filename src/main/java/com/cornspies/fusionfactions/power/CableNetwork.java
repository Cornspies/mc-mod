package com.cornspies.fusionfactions.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.PowerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// One set of connected cables and the blocks around them that take FE. FE pushed in is split evenly between those
// blocks, up to PowerConfig.cableThroughput() per tick for the whole network.
public final class CableNetwork {
    // A larger network is cut off here; FE only reaches the part found first.
    private static final int MAX_CABLES = 4096;

    private record Endpoint(BlockPos pos, BlockCapabilityCache<EnergyHandler, @Nullable Direction> handler) {
    }

    private final ServerLevel level;
    private final Set<BlockPos> cables;
    private final List<Endpoint> endpoints;
    private final Throughput throughput = new Throughput();
    private int rotation;
    private boolean inserting;

    private CableNetwork(ServerLevel level, Set<BlockPos> cables, List<Endpoint> endpoints) {
        this.level = level;
        this.cables = cables;
        this.endpoints = endpoints;
    }

    static CableNetwork build(ServerLevel level, BlockPos start) {
        Set<BlockPos> cables = new HashSet<>();
        List<Endpoint> endpoints = new ArrayList<>();
        Set<BlockPos> seenEndpoints = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        cables.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos cable = queue.poll();
            for (Direction side : Direction.values()) {
                BlockPos neighbor = cable.relative(side);
                if (!level.isLoaded(neighbor) || cables.contains(neighbor)) {
                    continue;
                }
                if (level.getBlockState(neighbor).getBlock() instanceof CableBlock) {
                    if (cables.size() < MAX_CABLES) {
                        cables.add(neighbor);
                        queue.add(neighbor);
                    }
                } else if (level.getCapability(Capabilities.Energy.BLOCK, neighbor, side.getOpposite()) != null
                        && seenEndpoints.add(neighbor.immutable())) {
                    endpoints.add(new Endpoint(neighbor.immutable(),
                            BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, neighbor, side.getOpposite())));
                }
            }
        }
        return new CableNetwork(level, cables, endpoints);
    }

    Set<BlockPos> cables() {
        return this.cables;
    }

    // Pushes FE from the block at source into the other blocks on the network, an even share each, and the rest to
    // whoever still takes it.
    int insert(BlockPos source, int amount, TransactionContext transaction) {
        if (this.inserting || amount <= 0) {
            return 0;
        }
        this.throughput.updateSnapshots(transaction);
        long now = this.level.getGameTime();
        if (now != this.throughput.tick) {
            this.throughput.tick = now;
            this.throughput.used = 0;
            this.rotation++;
        }
        int allowed = Math.min(amount, PowerConfig.cableThroughput() - this.throughput.used);
        if (allowed <= 0) {
            return 0;
        }
        List<EnergyHandler> targets = new ArrayList<>();
        for (int i = 0; i < this.endpoints.size(); i++) {
            Endpoint endpoint = this.endpoints.get(Math.floorMod(this.rotation + i, this.endpoints.size()));
            EnergyHandler handler = endpoint.pos().equals(source) ? null : endpoint.handler().getCapability();
            if (handler != null) {
                targets.add(handler);
            }
        }
        int remaining = allowed;
        this.inserting = true;
        try {
            for (int i = 0; i < targets.size() && remaining > 0; i++) {
                int share = (remaining + targets.size() - i - 1) / (targets.size() - i);
                remaining -= targets.get(i).insert(share, transaction);
            }
            for (int i = 0; i < targets.size() && remaining > 0; i++) {
                remaining -= targets.get(i).insert(remaining, transaction);
            }
        } finally {
            this.inserting = false;
        }
        this.throughput.used += allowed - remaining;
        return allowed - remaining;
    }

    // FE carried this tick, rolled back with the transaction it was carried in.
    private static final class Throughput extends SnapshotJournal<long[]> {
        long tick = Long.MIN_VALUE;
        int used;

        @Override
        protected long[] createSnapshot() {
            return new long[] { this.tick, this.used };
        }

        @Override
        protected void revertToSnapshot(long[] snapshot) {
            this.tick = snapshot[0];
            this.used = (int) snapshot[1];
        }
    }
}
