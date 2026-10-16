package com.cornspies.fusionfactions.client.datagen;

import java.util.ArrayList;
import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.MachineRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

// Builds a machine recipe (industry.MachineRecipe) for the data run. Machine recipes have no recipe book entry and no
// unlock advancement.
final class MachineRecipeBuilder {
    private final Machine machine;
    private final ItemLike result;
    private final int count;
    private final List<SizedIngredient> inputs = new ArrayList<>();
    private int ticks;
    private int energy;

    MachineRecipeBuilder(Machine machine, ItemLike result, int count) {
        this.machine = machine;
        this.result = result;
        this.count = count;
        this.ticks = machine.defaultTicks();
        this.energy = machine.defaultEnergy();
    }

    MachineRecipeBuilder input(Ingredient ingredient, int count) {
        this.inputs.add(new SizedIngredient(ingredient, count));
        return this;
    }

    MachineRecipeBuilder input(ItemLike item, int count) {
        return this.input(Ingredient.of(item), count);
    }

    MachineRecipeBuilder input(ItemLike item) {
        return this.input(item, 1);
    }

    MachineRecipeBuilder ticks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    MachineRecipeBuilder energy(int energyPerTick) {
        this.energy = energyPerTick;
        return this;
    }

    // Saved as fusionfactions:<recipe type>/<name>, e.g. crushing/aluminum_dust_from_raw_bauxite.
    void save(RecipeOutput output, String name) {
        MachineRecipe recipe = new MachineRecipe(this.machine, this.inputs, new ItemStackTemplate(this.result.asItem(), this.count), this.ticks, this.energy);
        output.accept(ResourceKey.create(Registries.RECIPE, FusionFactions.id(this.machine.recipeName() + "/" + name)), recipe, null);
    }
}
