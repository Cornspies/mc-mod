package com.cornspies.fusionfactions.power;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.cornspies.fusionfactions.config.PowerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// The cable networks of each level, built when FE first flows through a cable and dropped whenever a cable or a block
// next to one changes. Server thread only.
public final class CableNetworks {
    private static final Map<ServerLevel, Map<BlockPos, CableNetwork>> NETWORKS = new WeakHashMap<>();

    public static void invalidate(ServerLevel level) {
        NETWORKS.remove(level);
    }

    public static CableNetwork get(ServerLevel level, BlockPos cable) {
        Map<BlockPos, CableNetwork> networks = NETWORKS.computeIfAbsent(level, l -> new HashMap<>());
        CableNetwork network = networks.get(cable);
        if (network == null) {
            network = CableNetwork.build(level, cable);
            for (BlockPos pos : network.cables()) {
                networks.put(pos, network);
            }
        }
        return network;
    }

    // The FE handler a cable shows on one side: whatever is pushed in goes to the network, never back to the block
    // on that side. A cable holds no FE itself.
    public static EnergyHandler handler(ServerLevel level, BlockPos cable, Direction side) {
        BlockPos source = cable.relative(side);
        return new EnergyHandler() {
            @Override
            public long getAmountAsLong() {
                return 0;
            }

            @Override
            public long getCapacityAsLong() {
                return PowerConfig.cableThroughput();
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                return CableNetworks.get(level, cable).insert(source, amount, transaction);
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }

    private CableNetworks() {
    }
}
