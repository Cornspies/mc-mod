package com.cornspies.fusionfactions.supplydrop;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Server-wide drop state, saved to data/fusionfactions/supply_drops.dat so restarts keep the timer and active drops.
public class SupplyDropData extends SavedData {
    private static final Codec<SupplyDropData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("ticksUntilNextDrop").forGetter(data -> data.ticksUntilNextDrop),
            Codec.INT.fieldOf("nextId").forGetter(data -> data.nextId),
            ActiveDrop.CODEC.listOf().fieldOf("drops").forGetter(data -> data.drops))
            .apply(instance, SupplyDropData::new));

    public static final SavedDataType<SupplyDropData> TYPE = new SavedDataType<>(FusionFactions.id("supply_drops"), SupplyDropData::new, CODEC);

    // Counts down only while the schedule is running (D12). -1 until first set from the config.
    long ticksUntilNextDrop = -1;
    int nextId = 1;
    final List<ActiveDrop> drops;
    // Not saved: whether this server run has re-forced the chunks of active drops yet.
    boolean ticketsRestored;

    private SupplyDropData() {
        this.drops = new ArrayList<>();
    }

    private SupplyDropData(long ticksUntilNextDrop, int nextId, List<ActiveDrop> drops) {
        this.ticksUntilNextDrop = ticksUntilNextDrop;
        this.nextId = nextId;
        this.drops = new ArrayList<>(drops);
    }

    public static SupplyDropData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<ActiveDrop> drops() {
        return List.copyOf(this.drops);
    }

    public long ticksUntilNextDrop() {
        return this.ticksUntilNextDrop;
    }

    public @Nullable ActiveDrop find(int id) {
        for (ActiveDrop drop : this.drops) {
            if (drop.id() == id) {
                return drop;
            }
        }
        return null;
    }
}
