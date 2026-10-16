package com.cornspies.fusionfactions.client.datagen;

import java.util.List;

import com.cornspies.fusionfactions.industry.Ores;
import com.cornspies.fusionfactions.registry.ModBlocks;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Ore generation (D69, DESIGN.md §6b.2): one ore feature per ore, its placement, and a biome modifier that adds it to
// the biomes in the ore's biome tag (industry.Ores).
public final class ModWorldGen {
    public static RegistrySetBuilder entries() {
        return new RegistrySetBuilder()
                .add(Registries.FEATURE, ModWorldGen::features)
                .add(Registries.PLACED_FEATURE, ModWorldGen::placedFeatures)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModWorldGen::biomeModifiers);
    }

    // Vein sizes: bauxite like copper, niter a bit smaller, the rare ores small.
    private static void features(BootstrapContext<Feature> context) {
        context.register(Ores.BAUXITE.feature(), new OreFeature(targets(ModBlocks.BAUXITE_ORE.get(), ModBlocks.DEEPSLATE_BAUXITE_ORE.get()), 10));
        context.register(Ores.NITER.feature(), new OreFeature(targets(ModBlocks.NITER_ORE.get(), ModBlocks.DEEPSLATE_NITER_ORE.get()), 8));
        context.register(Ores.RARE_EARTH.feature(), new OreFeature(targets(ModBlocks.RARE_EARTH_ORE.get(), ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get()), 7));
        context.register(Ores.LITHIUM.feature(), new OreFeature(targets(ModBlocks.LITHIUM_ORE.get(), ModBlocks.DEEPSLATE_LITHIUM_ORE.get()), 7));
    }

    private static List<BlockReplacement> targets(Block stoneOre, Block deepslateOre) {
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        return List.of(BlockReplacement.replace(stone, stoneOre.defaultBlockState()), BlockReplacement.replace(deepslate, deepslateOre.defaultBlockState()));
    }

    // Veins per chunk and heights. Bauxite matches copper (16 veins, most around y 48), about 40-80 blocks per chunk;
    // niter about half that. Rare earth reaches up into the mountains it spawns in; the rare ores come to roughly 5-10
    // blocks per chunk of their biomes.
    private static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
        PlacementUtils.register(context, Ores.BAUXITE.placedFeature(), features.getOrThrow(Ores.BAUXITE.feature()),
                placement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112))));
        PlacementUtils.register(context, Ores.NITER.placedFeature(), features.getOrThrow(Ores.NITER.feature()),
                placement(8, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(80))));
        PlacementUtils.register(context, Ores.RARE_EARTH.placedFeature(), features.getOrThrow(Ores.RARE_EARTH.feature()),
                placement(8, HeightRangePlacement.uniform(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(160))));
        PlacementUtils.register(context, Ores.LITHIUM.placedFeature(), features.getOrThrow(Ores.LITHIUM.feature()),
                placement(3, HeightRangePlacement.uniform(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(64))));
    }

    private static List<PlacementModifier> placement(int veinsPerChunk, PlacementModifier height) {
        return List.of(CountPlacement.of(veinsPerChunk), InSquarePlacement.spread(), height, BiomeFilter.biome());
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        for (Ores ore : Ores.values()) {
            context.register(ore.biomeModifier(), new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(ore.biomes()),
                    HolderSet.direct(placed.getOrThrow(ore.placedFeature())), GenerationStep.Decoration.UNDERGROUND_ORES));
        }
    }

    private ModWorldGen() {
    }
}
