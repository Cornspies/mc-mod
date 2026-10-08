package com.cornspies.fusionfactions.client.datagen;

import com.cornspies.fusionfactions.crafting.HeavyWeaponRecipesCondition;
import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

// Weapon recipes (D27): vanilla items make three parts (barrel, core, grip), and the parts make the
// weapons. The auto pistol and SMG are upgrades of the previous weapon, as in Fusion Warfare.
// The sniper rifle and gatling gun recipes only load if the startup config allows them (D26).
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
    }

    @Override
    protected void buildRecipes() {
        this.shaped(RecipeCategory.MISC, ModItems.BARREL.get())
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), this.has(Items.IRON_INGOT))
                .save(this.output);
        this.shaped(RecipeCategory.MISC, ModItems.FUSION_CORE.get())
                .pattern("GRG")
                .pattern("RAR")
                .pattern("GRG")
                .define('G', Items.GOLD_NUGGET)
                .define('R', Items.REDSTONE)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(Items.AMETHYST_SHARD), this.has(Items.AMETHYST_SHARD))
                .save(this.output);
        this.shaped(RecipeCategory.MISC, ModItems.GRIP.get())
                .pattern("LI")
                .pattern("L ")
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.LEATHER), this.has(Items.LEATHER))
                .save(this.output);
        this.shapeless(RecipeCategory.COMBAT, ModItems.AMMO.get(), 8)
                .requires(Items.COPPER_INGOT)
                .requires(Items.GUNPOWDER)
                .requires(Items.REDSTONE)
                .unlockedBy(getHasName(Items.GUNPOWDER), this.has(Items.GUNPOWDER))
                .save(this.output);

        this.shapeless(RecipeCategory.COMBAT, ModItems.PISTOL.get())
                .requires(ModItems.BARREL.get())
                .requires(ModItems.FUSION_CORE.get())
                .requires(ModItems.GRIP.get())
                .unlockedBy(getHasName(ModItems.FUSION_CORE.get()), this.has(ModItems.FUSION_CORE.get()))
                .save(this.output);
        this.shapeless(RecipeCategory.COMBAT, ModItems.AUTO_PISTOL.get())
                .requires(ModItems.PISTOL.get())
                .requires(ModItems.FUSION_CORE.get())
                .unlockedBy(getHasName(ModItems.PISTOL.get()), this.has(ModItems.PISTOL.get()))
                .save(this.output);
        this.shapeless(RecipeCategory.COMBAT, ModItems.SMG.get())
                .requires(ModItems.AUTO_PISTOL.get())
                .requires(ModItems.BARREL.get())
                .requires(ModItems.GRIP.get())
                .unlockedBy(getHasName(ModItems.AUTO_PISTOL.get()), this.has(ModItems.AUTO_PISTOL.get()))
                .save(this.output);
        this.shapeless(RecipeCategory.COMBAT, ModItems.SHOTGUN.get())
                .requires(ModItems.BARREL.get(), 2)
                .requires(ModItems.FUSION_CORE.get())
                .requires(ModItems.GRIP.get())
                .unlockedBy(getHasName(ModItems.FUSION_CORE.get()), this.has(ModItems.FUSION_CORE.get()))
                .save(this.output);

        RecipeOutput heavyWeapons = this.output.withConditions(HeavyWeaponRecipesCondition.INSTANCE);
        this.shapeless(RecipeCategory.COMBAT, ModItems.SNIPER_RIFLE.get())
                .requires(ModItems.BARREL.get(), 2)
                .requires(ModItems.FUSION_CORE.get(), 2)
                .requires(ModItems.GRIP.get())
                .requires(Items.SPYGLASS)
                .unlockedBy(getHasName(ModItems.FUSION_CORE.get()), this.has(ModItems.FUSION_CORE.get()))
                .save(heavyWeapons);
        this.shapeless(RecipeCategory.COMBAT, ModItems.GATLING_GUN.get())
                .requires(ModItems.BARREL.get(), 4)
                .requires(ModItems.FUSION_CORE.get(), 2)
                .requires(ModItems.GRIP.get())
                .requires(Items.IRON_BLOCK)
                .unlockedBy(getHasName(ModItems.FUSION_CORE.get()), this.has(ModItems.FUSION_CORE.get()))
                .save(heavyWeapons);
    }
}
