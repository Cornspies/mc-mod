package com.cornspies.fusionfactions.client;

import java.util.List;

import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.MachineRecipe;
import com.cornspies.fusionfactions.registry.ModRecipes;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

// The machine recipes the server sent (ModRecipes.sendToClients), for recipe viewers. JEI starts after this event
// (it listens with the lowest priority), so the recipes are here by the time our plugin asks for them.
public final class ClientMachineRecipes {
    private static RecipeMap recipes = RecipeMap.EMPTY;

    public static void onRecipesReceived(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        recipes = RecipeMap.EMPTY;
    }

    public static List<RecipeHolder<MachineRecipe>> of(Machine machine) {
        return List.copyOf(recipes.byType(ModRecipes.type(machine)));
    }

    private ClientMachineRecipes() {
    }
}
