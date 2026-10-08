package com.cornspies.fusionfactions.missile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Missiles in flight, saved to data/fusionfactions/missiles.dat.
public class MissileData extends SavedData {
    private static final Codec<MissileData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("nextId").forGetter(data -> data.nextId),
            MissileFlight.CODEC.listOf().fieldOf("flights").forGetter(data -> data.flights))
            .apply(instance, MissileData::new));

    public static final SavedDataType<MissileData> TYPE = new SavedDataType<>(FusionFactions.id("missiles"), MissileData::new, CODEC);

    int nextId = 1;
    final List<MissileFlight> flights;
    // Not saved: blasts whose blocks are still breaking, and whether this server run has re-forced the
    // chunks of flights in the air yet.
    final ArrayDeque<MissileManager.PendingBlast> pending = new ArrayDeque<>();
    boolean ticketsRestored;

    private MissileData() {
        this.flights = new ArrayList<>();
    }

    private MissileData(int nextId, List<MissileFlight> flights) {
        this.nextId = nextId;
        this.flights = new ArrayList<>(flights);
    }

    public static MissileData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<MissileFlight> flights() {
        return List.copyOf(this.flights);
    }

    public @Nullable MissileFlight find(int id) {
        for (MissileFlight flight : this.flights) {
            if (flight.id() == id) {
                return flight;
            }
        }
        return null;
    }
}
