package com.cornspies.fusionfactions.registry;

import java.util.EnumMap;
import java.util.Map;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.MachineRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// One recipe type and serializer per machine (D70): fusionfactions:crushing, mixing, fabricating, assembling.
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, FusionFactions.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, FusionFactions.MODID);
    public static final DeferredRegister<RecipeBookCategory> BOOK_CATEGORIES = DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, FusionFactions.MODID);

    // Machine recipes don't show in the recipe book; every recipe still needs a category.
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> BOOK_CATEGORY = BOOK_CATEGORIES.register("machine", RecipeBookCategory::new);

    private static final Map<Machine, DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>>> TYPE_BY_MACHINE = new EnumMap<>(Machine.class);
    private static final Map<Machine, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>>> SERIALIZER_BY_MACHINE = new EnumMap<>(Machine.class);

    static {
        for (Machine machine : Machine.values()) {
            TYPE_BY_MACHINE.put(machine, TYPES.register(machine.recipeName(), id -> RecipeType.simple(id)));
            SERIALIZER_BY_MACHINE.put(machine, SERIALIZERS.register(machine.recipeName(),
                    () -> new RecipeSerializer<>(MachineRecipe.codec(machine), MachineRecipe.streamCodec(machine))));
        }
    }

    public static RecipeType<MachineRecipe> type(Machine machine) {
        return TYPE_BY_MACHINE.get(machine).get();
    }

    public static RecipeSerializer<MachineRecipe> serializer(Machine machine) {
        return SERIALIZER_BY_MACHINE.get(machine).get();
    }

    private ModRecipes() {
    }
}
