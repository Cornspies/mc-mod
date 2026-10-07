package com.cornspies.fusionfactions.supplydrop;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

// One announced drop, from countdown until its crate is gone. Saved in SupplyDropData.
public final class ActiveDrop {
    public enum Phase implements StringRepresentable {
        COUNTDOWN("countdown"),
        FALLING("falling"),
        LANDED("landed");

        public static final Codec<Phase> CODEC = StringRepresentable.fromEnum(Phase::values);

        private final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final Codec<ActiveDrop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(ActiveDrop::id),
            CrateRarity.CODEC.fieldOf("rarity").forGetter(ActiveDrop::rarity),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(ActiveDrop::dimension),
            BlockPos.CODEC.fieldOf("target").forGetter(ActiveDrop::target),
            Phase.CODEC.fieldOf("phase").forGetter(ActiveDrop::phase),
            Codec.LONG.fieldOf("phaseEndsAt").forGetter(ActiveDrop::phaseEndsAt),
            BlockPos.CODEC.optionalFieldOf("cratePos").forGetter(drop -> Optional.ofNullable(drop.cratePos)))
            .apply(instance, (id, rarity, dimension, target, phase, phaseEndsAt, cratePos) ->
                    new ActiveDrop(id, rarity, dimension, target, phase, phaseEndsAt, cratePos.orElse(null))));

    // Arbitrary constant; combined with the drop id it gives each drop its own chunk-ticket owner.
    private static final long TICKET_OWNER_MSB = 0x4646_5375_7070_6C79L;

    private final int id;
    private final CrateRarity rarity;
    private final ResourceKey<Level> dimension;
    // The landing spot picked at announcement time. Its chunk is force-loaded while the drop is active.
    private final BlockPos target;
    private Phase phase;
    // Game time at which the current phase ends: countdown end, falling timeout, or despawn time.
    private long phaseEndsAt;
    private @Nullable BlockPos cratePos;

    public ActiveDrop(int id, CrateRarity rarity, ResourceKey<Level> dimension, BlockPos target, Phase phase, long phaseEndsAt, @Nullable BlockPos cratePos) {
        this.id = id;
        this.rarity = rarity;
        this.dimension = dimension;
        this.target = target;
        this.phase = phase;
        this.phaseEndsAt = phaseEndsAt;
        this.cratePos = cratePos;
    }

    public int id() {
        return this.id;
    }

    public CrateRarity rarity() {
        return this.rarity;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public BlockPos target() {
        return this.target;
    }

    public Phase phase() {
        return this.phase;
    }

    public long phaseEndsAt() {
        return this.phaseEndsAt;
    }

    // Where the crate block is once landed, otherwise the target.
    public BlockPos position() {
        return this.cratePos != null ? this.cratePos : this.target;
    }

    public UUID ticketOwner() {
        return new UUID(TICKET_OWNER_MSB, this.id);
    }

    void startFalling(long timeoutAt) {
        this.phase = Phase.FALLING;
        this.phaseEndsAt = timeoutAt;
    }

    void land(BlockPos cratePos, long despawnAt) {
        this.phase = Phase.LANDED;
        this.phaseEndsAt = despawnAt;
        this.cratePos = cratePos;
    }
}
