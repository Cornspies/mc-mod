package com.cornspies.fusionfactions.missile;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

// Missile types (D48). Each comes in tiers I to III; what they do on impact is in MissileBlast.
public enum MissileType implements StringRepresentable {
    EXPLOSIVE("explosive");

    public static final Codec<MissileType> CODEC = StringRepresentable.fromEnum(MissileType::values);
    public static final int MAX_TIER = 3;

    private final String name;

    MissileType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String translationKey() {
        return "missile.fusionfactions." + this.name;
    }

    public static @Nullable MissileType byName(String name) {
        for (MissileType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
