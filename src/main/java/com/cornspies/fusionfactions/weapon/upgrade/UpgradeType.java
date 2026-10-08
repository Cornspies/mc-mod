package com.cornspies.fusionfactions.weapon.upgrade;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

// The seven upgrade types (D32). Each one is a field around the weapon in the workbench (DESIGN.md §4.5).
// The default per-tier values are percent (penetration: percentage points); servers can change them in
// the synced config (UpgradeConfig), together with which weapons accept which types.
public enum UpgradeType implements StringRepresentable {
    // Extra armor piercing, in percentage points.
    PENETRATION("penetration", 2, 10, 20, 30),
    // More rounds per magazine.
    MAGAZINE("magazine", 7, 20, 40, 60),
    // Shorter distance at which the shot can be heard (a suppressor).
    NOISE("noise", 3, 25, 50, 75),
    // Less camera kick.
    RECOIL("recoil", 5, 20, 35, 50),
    // Less time to load each round.
    RELOAD("reload", 8, 15, 30, 45),
    // Less spread.
    ACCURACY("accuracy", 1, 20, 35, 50),
    // Faster bullets: they fly further and drop less.
    RANGE("range", 6, 15, 30, 50);

    public static final Codec<UpgradeType> CODEC = StringRepresentable.fromEnum(UpgradeType::values);
    public static final StreamCodec<ByteBuf, UpgradeType> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], UpgradeType::ordinal);

    private final String name;
    private final int field;
    private final double[] defaultValues;

    UpgradeType(String name, int field, double... defaultValues) {
        this.name = name;
        this.field = field;
        this.defaultValues = defaultValues;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    // Position in the workbench's 3x3 grid, row by row (0 = top left, 4 = the weapon in the middle).
    public int field() {
        return this.field;
    }

    public double defaultValue(int tier) {
        return this.defaultValues[tier - 1];
    }

    public String translationKey() {
        return "upgrade.fusionfactions." + this.name;
    }

    public static @Nullable UpgradeType byName(String name) {
        for (UpgradeType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
