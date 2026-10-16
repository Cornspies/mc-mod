package com.cornspies.fusionfactions.client.datagen;

import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.crafting.HeavyWeaponRecipesCondition;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

// Weapon recipes (D27): vanilla items make three parts (barrel, core, grip), and the parts make the
// weapons. The auto pistol and SMG are upgrades of the previous weapon, as in Fusion Warfare.
// The sniper rifle and gatling gun recipes only load if the startup config allows them (D26).
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
    }

    @Override
    protected void buildRecipes() {
        // Resources (DESIGN.md §6b.2): ores, raw items and dusts smelt into ingots, faster in a blast furnace. Rare earth
        // and niter are used as they are (rare earth is ground in the crusher).
        this.smeltAndBlast(List.of(ModItems.RAW_BAUXITE.get(), ModItems.BAUXITE_ORE.get(), ModItems.DEEPSLATE_BAUXITE_ORE.get(),
                ModItems.ALUMINUM_DUST.get()), ModItems.ALUMINUM_INGOT.get(), 0.7F);
        this.smeltAndBlast(List.of(ModItems.RAW_LITHIUM.get(), ModItems.LITHIUM_ORE.get(), ModItems.DEEPSLATE_LITHIUM_ORE.get(),
                ModItems.LITHIUM_DUST.get()), ModItems.LITHIUM_INGOT.get(), 1.0F);

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

        // Weapon workbench (D30): a crafting table fitted with weapon parts.
        this.shaped(RecipeCategory.MISC, ModItems.WEAPON_WORKBENCH.get())
                .pattern("IFI")
                .pattern("BCG")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('F', ModItems.FUSION_CORE.get())
                .define('B', ModItems.BARREL.get())
                .define('C', Items.CRAFTING_TABLE)
                .define('G', ModItems.GRIP.get())
                .unlockedBy(getHasName(ModItems.FUSION_CORE.get()), this.has(ModItems.FUSION_CORE.get()))
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

        // Missiles (DESIGN.md §6.4): expensive, from vanilla items and our parts, with common c: tags so other
        // mods' metals count too (D47). Tier III missiles are crate-only (D57).
        this.shaped(RecipeCategory.COMBAT, ModItems.TARGET_DESIGNATOR.get())
                .pattern(" SR")
                .pattern("IFI")
                .pattern(" I ")
                .define('S', Items.SPYGLASS)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', ModItems.FUSION_CORE.get())
                .unlockedBy(getHasName(Items.SPYGLASS), this.has(Items.SPYGLASS))
                .save(this.output);
        this.shaped(RecipeCategory.COMBAT, ModItems.MISSILE_LAUNCHER.get())
                .pattern("BOB")
                .pattern("IFI")
                .pattern("ODO")
                .define('B', ModItems.BARREL.get())
                .define('O', Tags.Items.OBSIDIANS)
                .define('I', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('F', ModItems.FUSION_CORE.get())
                .define('D', Items.DISPENSER)
                .unlockedBy(getHasName(ModItems.FUSION_CORE.get()), this.has(ModItems.FUSION_CORE.get()))
                .save(this.output);
        this.shaped(RecipeCategory.COMBAT, ModItems.missile(MissileType.EXPLOSIVE, 1).get())
                .pattern("ITI")
                .pattern("ITI")
                .pattern("GFG")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TNT)
                .define('G', Tags.Items.GUNPOWDERS)
                .define('F', ModItems.FUSION_CORE.get())
                .unlockedBy(getHasName(Items.TNT), this.has(Items.TNT))
                .save(this.output);
        this.shaped(RecipeCategory.COMBAT, ModItems.missile(MissileType.EXPLOSIVE, 2).get())
                .pattern("DTD")
                .pattern("TMT")
                .pattern("DFD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('T', Items.TNT)
                .define('M', ModItems.missile(MissileType.EXPLOSIVE, 1).get())
                .define('F', ModItems.FUSION_CORE.get())
                .unlockedBy(getHasName(ModItems.missile(MissileType.EXPLOSIVE, 1).get()), this.has(ModItems.missile(MissileType.EXPLOSIVE, 1).get()))
                .save(this.output);

        // The other missile types (D48): the same body, with a different payload. Tier II is tier I upgraded.
        this.missile(MissileType.BREACHING, this.tag(Tags.Items.GEMS_DIAMOND), Items.NETHERITE_SCRAP);
        this.missile(MissileType.EMP, this.tag(BlockItemTags.LIGHTNING_RODS.item()), Items.REDSTONE_BLOCK);
        this.missile(MissileType.INCENDIARY, Ingredient.of(Items.BLAZE_POWDER), Items.BLAZE_ROD);

        // Missile silo (D55, DESIGN.md §6.8): 31 casing, a hatch and a controller.
        this.shaped(RecipeCategory.COMBAT, ModItems.SILO_CASING.get(), 4)
                .pattern("IOI")
                .pattern("O O")
                .pattern("IOI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('O', Tags.Items.OBSIDIANS)
                .unlockedBy(getHasName(ModItems.MISSILE_LAUNCHER.get()), this.has(ModItems.MISSILE_LAUNCHER.get()))
                .save(this.output);
        this.shaped(RecipeCategory.COMBAT, ModItems.SILO_HATCH.get())
                .pattern("ITI")
                .pattern("C C")
                .pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.IRON_TRAPDOOR)
                .define('C', ModItems.SILO_CASING.get())
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy(getHasName(ModItems.SILO_CASING.get()), this.has(ModItems.SILO_CASING.get()))
                .save(this.output);
        this.shaped(RecipeCategory.COMBAT, ModItems.SILO_CONTROLLER.get())
                .pattern("BFB")
                .pattern("CLC")
                .pattern("CRC")
                .define('B', ModItems.BARREL.get())
                .define('F', ModItems.FUSION_CORE.get())
                .define('C', ModItems.SILO_CASING.get())
                .define('L', ModItems.MISSILE_LAUNCHER.get())
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy(getHasName(ModItems.SILO_CASING.get()), this.has(ModItems.SILO_CASING.get()))
                .save(this.output);

        // Counterplay (D53, DESIGN.md §6.9).
        this.shaped(RecipeCategory.COMBAT, ModItems.RADAR.get())
                .pattern("ACA")
                .pattern("IFI")
                .pattern("IRI")
                .define('A', Items.AMETHYST_SHARD)
                .define('C', Items.COMPASS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', ModItems.FUSION_CORE.get())
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy(getHasName(ModItems.FUSION_CORE.get()), this.has(ModItems.FUSION_CORE.get()))
                .save(this.output);
        this.shaped(RecipeCategory.COMBAT, ModItems.INTERCEPTOR.get())
                .pattern("BRB")
                .pattern("IFI")
                .pattern("IDI")
                .define('B', ModItems.BARREL.get())
                .define('R', Items.FIREWORK_ROCKET)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', ModItems.FUSION_CORE.get())
                .define('D', Items.DISPENSER)
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

    // Tier I: the payload, TNT, iron, gunpowder and a fusion core. Tier II: tier I with more payload, a richer
    // payload in the corners, and another fusion core.
    private void missile(MissileType type, Ingredient payload, ItemLike richPayload) {
        Item tierOne = ModItems.missile(type, 1).get();
        this.shaped(RecipeCategory.COMBAT, tierOne)
                .pattern("IPI")
                .pattern("ITI")
                .pattern("GFG")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', payload)
                .define('T', Items.TNT)
                .define('G', Tags.Items.GUNPOWDERS)
                .define('F', ModItems.FUSION_CORE.get())
                .unlockedBy(getHasName(ModItems.MISSILE_LAUNCHER.get()), this.has(ModItems.MISSILE_LAUNCHER.get()))
                .save(this.output);
        this.shaped(RecipeCategory.COMBAT, ModItems.missile(type, 2).get())
                .pattern("RTR")
                .pattern("PMP")
                .pattern("RFR")
                .define('R', richPayload)
                .define('T', Items.TNT)
                .define('P', payload)
                .define('M', tierOne)
                .define('F', ModItems.FUSION_CORE.get())
                .unlockedBy(getHasName(tierOne), this.has(tierOne))
                .save(this.output);
    }

    private void smeltAndBlast(List<ItemLike> inputs, ItemLike result, float experience) {
        String resultName = getItemName(result);
        for (ItemLike input : inputs) {
            String suffix = "_" + getItemName(input);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, 200)
                    .group(resultName)
                    .unlockedBy(getHasName(input), this.has(input))
                    .save(this.output, ResourceKey.create(Registries.RECIPE, FusionFactions.id(resultName + "_from_smelting" + suffix)));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, 100)
                    .group(resultName)
                    .unlockedBy(getHasName(input), this.has(input))
                    .save(this.output, ResourceKey.create(Registries.RECIPE, FusionFactions.id(resultName + "_from_blasting" + suffix)));
        }
    }
}
