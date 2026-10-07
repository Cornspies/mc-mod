package com.cornspies.fusionfactions.supplydrop;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.ServerConfig;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.storage.loot.LootTable;

// Supply crate tiers (D15). Each has its own loot table: fusionfactions:supply_crate/<name>.
public enum CrateRarity implements StringRepresentable {
    COMMON("common", ChatFormatting.BLUE),
    RARE("rare", ChatFormatting.GOLD),
    LEGENDARY("legendary", ChatFormatting.RED);

    public static final Codec<CrateRarity> CODEC = StringRepresentable.fromEnum(CrateRarity::values);

    private final String name;
    private final ChatFormatting color;
    private final ResourceKey<LootTable> lootTable;

    CrateRarity(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
        this.lootTable = ResourceKey.create(Registries.LOOT_TABLE, FusionFactions.id("supply_crate/" + name));
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public ResourceKey<LootTable> lootTable() {
        return this.lootTable;
    }

    public MutableComponent displayName() {
        return Component.translatable("fusionfactions.rarity." + this.name).withStyle(this.color);
    }

    private int weight() {
        return switch (this) {
            case COMMON -> ServerConfig.WEIGHT_COMMON.getAsInt();
            case RARE -> ServerConfig.WEIGHT_RARE.getAsInt();
            case LEGENDARY -> ServerConfig.WEIGHT_LEGENDARY.getAsInt();
        };
    }

    // Weighted pick using the configured weights. Falls back to COMMON if every weight is 0.
    public static CrateRarity pick(RandomSource random) {
        int total = 0;
        for (CrateRarity rarity : values()) {
            total += rarity.weight();
        }
        if (total <= 0) {
            return COMMON;
        }
        int roll = random.nextInt(total);
        for (CrateRarity rarity : values()) {
            roll -= rarity.weight();
            if (roll < 0) {
                return rarity;
            }
        }
        return COMMON;
    }
}
