package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.industry.Ores;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Phase 3 resources (DESIGN.md §6b.2): ore drops, mining levels and where the ores generate. Registered by
// WeaponGameTests.
public final class ResourceGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    static final List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> TESTS = List.of(
            TEST_FUNCTIONS.register("ores_drop_raw_materials", () -> ResourceGameTests::oresDropRawMaterials),
            TEST_FUNCTIONS.register("ores_generate_in_their_biomes", () -> ResourceGameTests::oresGenerateInTheirBiomes),
            TEST_FUNCTIONS.register("ore_veins_replace_stone", () -> ResourceGameTests::oreVeinsReplaceStone));

    // Each ore drops its raw item with a fitting pickaxe and itself with Silk Touch; niter drops 2-4. Bauxite and niter
    // need a stone pickaxe, rare earth and lithium an iron one.
    private static void oresDropRawMaterials(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        ItemStack silkTouch = new ItemStack(Items.IRON_PICKAXE);
        silkTouch.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        List<Object[]> ores = List.of(
                new Object[] { ModBlocks.BAUXITE_ORE.get(), ModItems.RAW_BAUXITE.get(), false },
                new Object[] { ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), ModItems.RAW_BAUXITE.get(), false },
                new Object[] { ModBlocks.NITER_ORE.get(), ModItems.NITER.get(), false },
                new Object[] { ModBlocks.DEEPSLATE_NITER_ORE.get(), ModItems.NITER.get(), false },
                new Object[] { ModBlocks.RARE_EARTH_ORE.get(), ModItems.RAW_RARE_EARTH.get(), true },
                new Object[] { ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get(), ModItems.RAW_RARE_EARTH.get(), true },
                new Object[] { ModBlocks.LITHIUM_ORE.get(), ModItems.RAW_LITHIUM.get(), true },
                new Object[] { ModBlocks.DEEPSLATE_LITHIUM_ORE.get(), ModItems.RAW_LITHIUM.get(), true });
        for (Object[] ore : ores) {
            Block block = (Block) ore[0];
            Item raw = (Item) ore[1];
            boolean needsIron = (Boolean) ore[2];
            BlockState state = block.defaultBlockState();
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, pickaxe);
            helper.assertValueEqual(drops.size(), 1, block + " drop stacks");
            helper.assertTrue(drops.getFirst().is(raw), block + " should drop " + raw + ", not " + drops.getFirst());
            if (raw == ModItems.NITER.get()) {
                int count = drops.getFirst().getCount();
                helper.assertTrue(count >= 2 && count <= 4, block + " should drop 2-4 niter, not " + count);
            }
            List<ItemStack> silk = Block.getDrops(state, level, pos, null, null, silkTouch);
            helper.assertTrue(silk.size() == 1 && silk.getFirst().is(block.asItem()), block + " should drop itself with Silk Touch");
            helper.assertTrue(pickaxe.isCorrectToolForDrops(state), "an iron pickaxe should mine " + block);
            helper.assertValueEqual(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state), !needsIron, "stone pickaxe on " + block);
            helper.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state), "a wooden pickaxe should not mine " + block);
        }
        helper.succeed();
    }

    // Bauxite and niter generate in every overworld biome; rare earth only in mountains; lithium only in deserts and
    // badlands (the biome modifiers have been applied to the biomes by the time a test runs).
    private static void oresGenerateInTheirBiomes(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var placed = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        expect(helper, biomes.getOrThrow(Biomes.PLAINS), placed, true, true, false, false);
        expect(helper, biomes.getOrThrow(Biomes.JAGGED_PEAKS), placed, true, true, true, false);
        expect(helper, biomes.getOrThrow(Biomes.STONY_PEAKS), placed, true, true, true, false);
        expect(helper, biomes.getOrThrow(Biomes.DESERT), placed, true, true, false, true);
        expect(helper, biomes.getOrThrow(Biomes.BADLANDS), placed, true, true, false, true);
        expect(helper, biomes.getOrThrow(Biomes.NETHER_WASTES), placed, false, false, false, false);
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, Holder<Biome> biome, HolderLookup.RegistryLookup<PlacedFeature> placed,
            boolean bauxite, boolean niter, boolean rareEarth, boolean lithium) {
        boolean[] expected = { bauxite, niter, rareEarth, lithium };
        List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
        for (Ores ore : Ores.values()) {
            Holder<PlacedFeature> feature = placed.getOrThrow(ore.placedFeature());
            boolean present = steps.stream().anyMatch(step -> step.contains(feature));
            helper.assertValueEqual(present, expected[ore.ordinal()], ore.oreName() + " ore in " + biome.unwrapKey().map(ResourceKey::identifier).orElseThrow());
        }
    }

    // Each ore feature, placed in a block of stone with deepslate below, turns some of it into its ore.
    private static void oreVeinsReplaceStone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        Block[][] ores = {
                { ModBlocks.BAUXITE_ORE.get(), ModBlocks.DEEPSLATE_BAUXITE_ORE.get() },
                { ModBlocks.NITER_ORE.get(), ModBlocks.DEEPSLATE_NITER_ORE.get() },
                { ModBlocks.RARE_EARTH_ORE.get(), ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get() },
                { ModBlocks.LITHIUM_ORE.get(), ModBlocks.DEEPSLATE_LITHIUM_ORE.get() } };
        RandomSource random = RandomSource.create(42);
        for (Ores ore : Ores.values()) {
            BlockPos min = new BlockPos(1, 1, 1);
            BlockPos max = new BlockPos(7, 7, 7);
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                helper.setBlock(pos, pos.getY() <= 3 ? Blocks.DEEPSLATE : Blocks.STONE);
            }
            Feature feature = features.getOrThrow(ore.feature()).value();
            for (int attempt = 0; attempt < 4; attempt++) {
                feature.place(level, level.getChunkSource().getGenerator(), random, helper.absolutePos(new BlockPos(4, 4, 4)));
            }
            int stoneOres = 0;
            int deepslateOres = 0;
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                BlockState state = helper.getBlockState(pos);
                if (state.is(ores[ore.ordinal()][0])) {
                    stoneOres++;
                    helper.assertTrue(pos.getY() > 3, ore.oreName() + " stone ore in deepslate at " + pos);
                } else if (state.is(ores[ore.ordinal()][1])) {
                    deepslateOres++;
                    helper.assertTrue(pos.getY() <= 3, ore.oreName() + " deepslate ore in stone at " + pos);
                }
            }
            helper.assertTrue(stoneOres + deepslateOres > 0, ore.oreName() + " feature placed no ore");
        }
        helper.succeed();
    }

    private ResourceGameTests() {
    }
}
