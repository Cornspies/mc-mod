package com.cornspies.fusionfactions.registry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import com.cornspies.fusionfactions.FusionFactions;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    // Blocks no missile breaks, not even a breaching one (D51). Admins can add blocks with a datapack.
    public static final TagKey<Block> MISSILE_IMMUNE = TagKey.create(Registries.BLOCK, FusionFactions.id("missile_immune"));

    // Aluminum from any mod (D47), like our own ingot.
    public static final TagKey<Item> INGOTS_ALUMINUM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/aluminum"));

    private ModTags() {
    }
}
