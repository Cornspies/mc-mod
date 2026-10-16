package com.cornspies.fusionfactions.industry;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

// A machine recipe (DESIGN.md §6b.4): shapeless inputs with counts, in any of the machine's input slots and nothing else,
// make one result in a number of ticks, at an FE cost per tick. A datapack can add or replace them:
// {"type": "fusionfactions:crushing", "inputs": [{"ingredient": "fusionfactions:raw_bauxite", "count": 1}],
//  "result": {"id": "fusionfactions:aluminum_dust", "count": 2}, "ticks": 100, "energy_per_tick": 20}
public final class MachineRecipe implements Recipe<MachineRecipeInput> {
    private final Machine machine;
    private final List<SizedIngredient> inputs;
    private final ItemStackTemplate result;
    private final int ticks;
    private final int energyPerTick;

    public MachineRecipe(Machine machine, List<SizedIngredient> inputs, ItemStackTemplate result, int ticks, int energyPerTick) {
        this.machine = machine;
        this.inputs = List.copyOf(inputs);
        this.result = result;
        this.ticks = ticks;
        this.energyPerTick = energyPerTick;
    }

    public static MapCodec<MachineRecipe> codec(Machine machine) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.NESTED_CODEC.listOf(1, machine.inputCount()).fieldOf("inputs").forGetter(MachineRecipe::inputs),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(MachineRecipe::result),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("ticks", machine.defaultTicks()).forGetter(MachineRecipe::ticks),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy_per_tick", machine.defaultEnergy()).forGetter(MachineRecipe::energyPerTick))
                .apply(i, (inputs, result, ticks, energy) -> new MachineRecipe(machine, inputs, result, ticks, energy)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec(Machine machine) {
        return StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::inputs,
                ItemStackTemplate.STREAM_CODEC, MachineRecipe::result,
                ByteBufCodecs.VAR_INT, MachineRecipe::ticks,
                ByteBufCodecs.VAR_INT, MachineRecipe::energyPerTick,
                (inputs, result, ticks, energy) -> new MachineRecipe(machine, inputs, result, ticks, energy));
    }

    public Machine machine() {
        return this.machine;
    }

    public List<SizedIngredient> inputs() {
        return this.inputs;
    }

    public ItemStackTemplate result() {
        return this.result;
    }

    public int ticks() {
        return this.ticks;
    }

    public int energyPerTick() {
        return this.energyPerTick;
    }

    // For each input slot, the recipe input that takes its items (-1 for empty slots): each input claims the slots
    // holding what it accepts, so two inputs never share a slot. Null if the slots don't hold enough of every input, or
    // hold something the recipe doesn't use.
    public int @Nullable [] claim(MachineRecipeInput input) {
        int[] claimedBy = new int[input.size()];
        java.util.Arrays.fill(claimedBy, -1);
        for (int i = 0; i < this.inputs.size(); i++) {
            SizedIngredient ingredient = this.inputs.get(i);
            int found = 0;
            for (int slot = 0; slot < input.size(); slot++) {
                ItemStack stack = input.getItem(slot);
                if (claimedBy[slot] < 0 && !stack.isEmpty() && ingredient.ingredient().test(stack)) {
                    claimedBy[slot] = i;
                    found += stack.getCount();
                }
            }
            if (found < ingredient.count()) {
                return null;
            }
        }
        // Items the recipe doesn't use mean it's not this recipe: 4 aluminum and an iron ingot make an airframe, not a
        // barrel plus leftovers.
        for (int slot = 0; slot < input.size(); slot++) {
            if (claimedBy[slot] < 0 && !input.getItem(slot).isEmpty()) {
                return null;
            }
        }
        return claimedBy;
    }

    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        return this.claim(input) != null;
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input) {
        return this.result.create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<MachineRecipe> getSerializer() {
        return ModRecipes.serializer(this.machine);
    }

    @Override
    public RecipeType<MachineRecipe> getType() {
        return ModRecipes.type(this.machine);
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipes.BOOK_CATEGORY.get();
    }
}
