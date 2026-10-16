package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.MachineRecipeInput;
import com.cornspies.fusionfactions.industry.ProcessingMachineBlockEntity;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Machines and the material chain (D70-D74, DESIGN.md §6b.4, §6b.5). Registered by WeaponGameTests.
public final class MachineGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRUSHER_GRINDS_BAUXITE =
            TEST_FUNCTIONS.register("crusher_grinds_bauxite", () -> MachineGameTests::crusherGrindsBauxite);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASSEMBLER_BUILDS_MISSILE =
            TEST_FUNCTIONS.register("assembler_builds_missile", () -> MachineGameTests::assemblerBuildsMissile);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MATERIAL_CHAIN =
            TEST_FUNCTIONS.register("material_chain", () -> MachineGameTests::materialChain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_SLOTS =
            TEST_FUNCTIONS.register("machine_slots", () -> MachineGameTests::machineSlots);

    // The assembler takes 200 ticks for a missile.
    private static final int MAX_TICKS = 400;
    private static final int PADDING = 8;

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        for (var function : List.of(CRUSHER_GRINDS_BAUXITE, ASSEMBLER_BUILDS_MISSILE, MATERIAL_CHAIN, MACHINE_SLOTS)) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"),
                    MAX_TICKS, 0, true, Rotation.NONE, false, 1, 1, false, PADDING);
            event.registerTest(function.getId(), new FunctionGameTestInstance(function.key(), data));
        }
    }

    private static ProcessingMachineBlockEntity place(GameTestHelper helper, Machine machine, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.machine(machine));
        return helper.getBlockEntity(pos, ProcessingMachineBlockEntity.class);
    }

    // A crusher turns one raw bauxite into two aluminum dust. Without power it waits, if power is required.
    private static void crusherGrindsBauxite(GameTestHelper helper) {
        ProcessingMachineBlockEntity crusher = place(helper, Machine.CRUSHER, new BlockPos(2, 1, 2));
        crusher.setItem(0, new ItemStack(ModItems.RAW_BAUXITE.get(), 2));
        int output = Machine.CRUSHER.inputCount();
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    if (MissileConfig.requirePower()) {
                        helper.assertTrue(crusher.getItem(output).isEmpty(), "a crusher without power should not work");
                        helper.assertValueEqual(crusher.getItem(0).getCount(), 2, "raw bauxite left without power");
                    }
                    MissileGameTests.charge(crusher.energy(), 40_000);
                })
                .thenWaitUntil(() -> helper.assertTrue(crusher.getItem(output).getCount() >= 2, "the crusher should have made aluminum dust"))
                .thenExecute(() -> {
                    helper.assertTrue(crusher.getItem(output).is(ModItems.ALUMINUM_DUST.get()), "crusher output");
                    helper.assertValueEqual(crusher.getItem(0).getCount(), 1, "raw bauxite left after one round");
                })
                .thenSucceed();
    }

    // An assembler builds an explosive missile I from inputs spread over its grid, and uses them up.
    private static void assemblerBuildsMissile(GameTestHelper helper) {
        ProcessingMachineBlockEntity assembler = place(helper, Machine.ASSEMBLER, new BlockPos(2, 1, 2));
        MissileGameTests.charge(assembler.energy(), 40_000);
        assembler.setItem(0, new ItemStack(Items.TNT, 2));
        assembler.setItem(4, new ItemStack(ModItems.AIRFRAME.get()));
        assembler.setItem(5, new ItemStack(ModItems.ROCKET_FUEL.get(), 2));
        assembler.setItem(8, new ItemStack(ModItems.BASIC_CHIP.get()));
        int output = Machine.ASSEMBLER.inputCount();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!assembler.getItem(output).isEmpty(), "the assembler should have built something"))
                .thenExecute(() -> {
                    helper.assertTrue(assembler.getItem(output).is(ModItems.missile(MissileType.EXPLOSIVE, 1).get()), "assembler output");
                    for (int slot = 0; slot < output; slot++) {
                        helper.assertTrue(assembler.getItem(slot).isEmpty(), "input slot " + slot + " should be used up");
                    }
                })
                .thenSucceed();
    }

    // Every step from ore to missile has a recipe in the right machine.
    private static void materialChain(GameTestHelper helper) {
        expect(helper, Machine.CRUSHER, List.of(new ItemStack(Items.QUARTZ)), ModItems.SILICON_DUST.get(), 2);
        expect(helper, Machine.CRUSHER, List.of(new ItemStack(ModItems.RAW_RARE_EARTH.get())), ModItems.RARE_EARTH_DUST.get(), 2);
        expect(helper, Machine.CRUSHER, List.of(new ItemStack(ModItems.NITER_ORE.get())), ModItems.NITER.get(), 5);
        expect(helper, Machine.CHEMICAL_MIXER, List.of(new ItemStack(ModItems.ALUMINUM_DUST.get()), new ItemStack(ModItems.NITER.get(), 2),
                new ItemStack(Items.SLIME_BALL)), ModItems.ROCKET_FUEL.get(), 2);
        expect(helper, Machine.CIRCUIT_FABRICATOR, List.of(new ItemStack(ModItems.SILICON.get()), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                ModItems.SILICON_WAFER.get(), 2);
        expect(helper, Machine.CIRCUIT_FABRICATOR, List.of(new ItemStack(ModItems.SILICON_WAFER.get()), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.REDSTONE, 2), ItemStack.EMPTY), ModItems.BASIC_CHIP.get(), 1);
        expect(helper, Machine.CIRCUIT_FABRICATOR, List.of(new ItemStack(ModItems.BASIC_CHIP.get()), new ItemStack(ModItems.SILICON_WAFER.get()),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(ModItems.RARE_EARTH_DUST.get())), ModItems.ADVANCED_CHIP.get(), 1);
        expect(helper, Machine.CIRCUIT_FABRICATOR, List.of(new ItemStack(ModItems.ADVANCED_CHIP.get()), new ItemStack(ModItems.RARE_EARTH_DUST.get(), 2),
                new ItemStack(Items.DIAMOND), new ItemStack(ModItems.LITHIUM_DUST.get())), ModItems.MILITARY_CHIP.get(), 1);
        expect(helper, Machine.ASSEMBLER, List.of(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4), new ItemStack(Items.IRON_INGOT)), ModItems.AIRFRAME.get(), 1);
        expect(helper, Machine.ASSEMBLER, List.of(new ItemStack(ModItems.BASIC_CHIP.get()), new ItemStack(Items.AMETHYST_SHARD),
                new ItemStack(ModItems.ALUMINUM_INGOT.get())), ModItems.FUSION_CORE.get(), 1);
        // Not enough of an input: no recipe.
        helper.assertTrue(recipe(helper, Machine.ASSEMBLER, List.of(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 3), new ItemStack(Items.IRON_INGOT))) == null,
                "three aluminum should not make an airframe");
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, Machine machine, List<ItemStack> inputs, Item result, int count) {
        ItemStack made = recipe(helper, machine, inputs);
        helper.assertTrue(made != null && made.is(result) && made.getCount() == count,
                machine.id() + " should make " + count + " " + result + " from " + inputs + ", not " + made);
    }

    private static ItemStack recipe(GameTestHelper helper, Machine machine, List<ItemStack> inputs) {
        java.util.ArrayList<ItemStack> slots = new java.util.ArrayList<>(inputs);
        while (slots.size() < machine.inputCount()) {
            slots.add(ItemStack.EMPTY);
        }
        MachineRecipeInput input = new MachineRecipeInput(slots);
        return helper.getLevel().recipeAccess().getRecipeFor(ModRecipes.type(machine), input, helper.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(null);
    }

    // Each input slot holds one kind of item, and nothing goes into the output slot.
    private static void machineSlots(GameTestHelper helper) {
        ProcessingMachineBlockEntity mixer = place(helper, Machine.CHEMICAL_MIXER, new BlockPos(2, 1, 2));
        mixer.setItem(0, new ItemStack(ModItems.NITER.get()));
        helper.assertTrue(mixer.canPlaceItem(0, new ItemStack(ModItems.NITER.get())), "more niter should fit with the niter");
        helper.assertFalse(mixer.canPlaceItem(1, new ItemStack(ModItems.NITER.get())), "niter should not go into a second slot");
        helper.assertTrue(mixer.canPlaceItem(1, new ItemStack(Items.SLIME_BALL)), "another item should fit in another slot");
        helper.assertFalse(mixer.canPlaceItem(Machine.CHEMICAL_MIXER.inputCount(), new ItemStack(ModItems.ROCKET_FUEL.get())), "nothing goes into the output");
        helper.succeed();
    }

    private MachineGameTests() {
    }
}
