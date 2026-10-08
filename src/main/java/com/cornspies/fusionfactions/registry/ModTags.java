package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    // Blocks no missile breaks, not even a breaching one (D51). Admins can add blocks with a datapack.
    public static final TagKey<Block> MISSILE_IMMUNE = TagKey.create(Registries.BLOCK, FusionFactions.id("missile_immune"));

    private ModTags() {
    }
}
