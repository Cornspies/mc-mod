package com.cornspies.fusionfactions.client.datagen;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

// Run with ./gradlew runData. Output goes to src/generated/resources.
// Lives in the client package because the data run is a client run and ModelProvider is client-only.
public final class DataGenerators {
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
    }

    // Loot tables, recipes (with their unlock advancements) and ore generation (features, their placement and the
    // biome modifiers that add them) are datapack registries in 26.3, so they're generated as registry entries.
    // NeoForge only writes the entries of the mod given with --mod.
    public static void gatherRegistries(GatherDataRegistryEntriesEvent event) {
        event.lootTable(new LootTableProvider.SubProviderEntry(SupplyCrateLoot::new, LootContextParamSets.CHEST),
                        new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK))
                .recipe(ModRecipeProvider::new);
        ModWorldGen.gather(event);
    }

    private DataGenerators() {
    }
}
