package com.cornspies.fusionfactions.client.datagen;

import java.util.List;
import java.util.Set;

import com.cornspies.fusionfactions.FusionFactions;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;

// Run with ./gradlew runData. Output goes to src/generated/resources.
// Lives in the client package because the data run is a client run and ModelProvider is client-only.
public final class DataGenerators {
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);

        // Loot tables and recipes (with their unlock advancements) are reloadable registries in 26.3, so
        // they're generated as registry objects.
        RegistrySetBuilder reloadable = new RegistrySetBuilder()
                .add(Registries.LOOT_TABLE, new LootTableProvider(Set.of(),
                        List.of(new LootTableProvider.SubProviderEntry(SupplyCrateLoot::new, LootContextParamSets.CHEST),
                                new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK))))
                .add(RecipeProvider.asBootstrap(ModRecipeProvider::new));
        event.createReloadableRegistryObjects(reloadable, Set.of(FusionFactions.MODID));
    }

    private DataGenerators() {
    }
}
