package com.cornspies.fusionfactions.client.datagen;

import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.crafting.HeavyWeaponRecipesCondition;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModTags;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

// Recipes. Weapons (D27) are built from three parts (barrel, core, grip); the auto pistol and SMG are upgrades of the
// previous weapon, as in Fusion Warfare, and the sniper rifle and gatling gun recipes only load if the startup config
// allows them (D26). Since Phase 3 the parts, the missile blocks and the missiles come out of the machines (D72, D74,
// DESIGN.md §6b.5); generators, cables and the machines themselves are crafted, so a factory can be started.
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

        // Power (D67, DESIGN.md §6b.3): crafted, because the machines need power before they can make anything.
        this.shaped(RecipeCategory.REDSTONE, ModItems.COMBUSTION_GENERATOR.get())
                .pattern("III")
                .pattern("CFC")
                .pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('F', Items.FURNACE)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy(getHasName(Items.FURNACE), this.has(Items.FURNACE))
                .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, ModItems.SOLAR_PANEL.get())
                .pattern("GGG")
                .pattern("ADA")
                .pattern("III")
                .define('G', Tags.BlockItems.GLASS_PANES.item())
                .define('A', ModTags.INGOTS_ALUMINUM)
                .define('D', Items.DAYLIGHT_DETECTOR)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(ModItems.ALUMINUM_INGOT.get()), this.has(ModTags.INGOTS_ALUMINUM))
                .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, ModItems.FE_CABLE.get(), 8)
                .pattern("KKK")
                .pattern("CCC")
                .pattern("KKK")
                .define('K', Items.DRIED_KELP)
                .define('C', Tags.Items.INGOTS_COPPER)
                .unlockedBy(getHasName(Items.COPPER_INGOT), this.has(Tags.Items.INGOTS_COPPER))
                .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, ModItems.CHARGING_STATION.get())
                .pattern("ICI")
                .pattern("ARA")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('A', ModTags.INGOTS_ALUMINUM)
                .define('R', Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item())
                .unlockedBy(getHasName(ModItems.ALUMINUM_INGOT.get()), this.has(ModTags.INGOTS_ALUMINUM))
                .save(this.output);

        this.smeltAndBlast(List.of(ModItems.SILICON_DUST.get()), ModItems.SILICON.get(), 0.3F);
        this.machines();
        this.crushing();
        this.mixing();
        this.fabricating();
        this.assembling();

        // Ammo (D72): niter takes the place of gunpowder.
        this.shapeless(RecipeCategory.COMBAT, ModItems.AMMO.get(), 8)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(c("dusts/niter"))
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy(getHasName(ModItems.NITER.get()), this.has(c("dusts/niter")))
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

    // The machines (D70), crafted from vanilla items and aluminum. The crusher needs none, so it can be the first one.
    private void machines() {
        this.shaped(RecipeCategory.MISC, ModItems.machine(Machine.CRUSHER))
                .pattern("IGI")
                .pattern("CPC")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Items.GRINDSTONE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('P', Items.PISTON)
                .unlockedBy(getHasName(ModItems.RAW_BAUXITE.get()), this.has(ModItems.RAW_BAUXITE.get()))
                .save(this.output);
        this.shaped(RecipeCategory.MISC, ModItems.machine(Machine.CHEMICAL_MIXER))
                .pattern("AGA")
                .pattern("GCG")
                .pattern("IPI")
                .define('A', ModTags.INGOTS_ALUMINUM)
                .define('G', Tags.BlockItems.GLASS_BLOCKS.item())
                .define('C', Items.CAULDRON)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', Items.PISTON)
                .unlockedBy(getHasName(ModItems.NITER.get()), this.has(ModItems.NITER.get()))
                .save(this.output);
        this.shaped(RecipeCategory.MISC, ModItems.machine(Machine.CIRCUIT_FABRICATOR))
                .pattern("ARA")
                .pattern("CDC")
                .pattern("III")
                .define('A', ModTags.INGOTS_ALUMINUM)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Items.COMPARATOR)
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(ModItems.ALUMINUM_INGOT.get()), this.has(ModTags.INGOTS_ALUMINUM))
                .save(this.output);
        this.shaped(RecipeCategory.MISC, ModItems.machine(Machine.ASSEMBLER))
                .pattern("AAA")
                .pattern("PCP")
                .pattern("IRI")
                .define('A', ModTags.INGOTS_ALUMINUM)
                .define('P', Items.PISTON)
                .define('C', Items.CRAFTING_TABLE)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item())
                .unlockedBy(getHasName(ModItems.ALUMINUM_INGOT.get()), this.has(ModTags.INGOTS_ALUMINUM))
                .save(this.output);
    }

    // Crusher: ores and raw materials into two dusts, quartz and sand into silicon dust.
    private void crushing() {
        this.machine(Machine.CRUSHER, ModItems.ALUMINUM_DUST.get(), 2).input(ModItems.RAW_BAUXITE.get()).save(this.output, "aluminum_dust_from_raw_bauxite");
        this.machine(Machine.CRUSHER, ModItems.ALUMINUM_DUST.get(), 2).input(this.tag(c("ores/aluminum")), 1).save(this.output, "aluminum_dust_from_ore");
        this.machine(Machine.CRUSHER, ModItems.ALUMINUM_DUST.get(), 1).input(this.tag(ModTags.INGOTS_ALUMINUM), 1).save(this.output,
                "aluminum_dust_from_ingot");
        this.machine(Machine.CRUSHER, ModItems.RARE_EARTH_DUST.get(), 2).input(ModItems.RAW_RARE_EARTH.get()).save(this.output,
                "rare_earth_dust_from_raw_rare_earth");
        this.machine(Machine.CRUSHER, ModItems.RARE_EARTH_DUST.get(), 2).input(this.tag(c("ores/rare_earth")), 1).save(this.output,
                "rare_earth_dust_from_ore");
        this.machine(Machine.CRUSHER, ModItems.LITHIUM_DUST.get(), 2).input(ModItems.RAW_LITHIUM.get()).save(this.output, "lithium_dust_from_raw_lithium");
        this.machine(Machine.CRUSHER, ModItems.LITHIUM_DUST.get(), 2).input(this.tag(c("ores/lithium")), 1).save(this.output, "lithium_dust_from_ore");
        this.machine(Machine.CRUSHER, ModItems.NITER.get(), 5).input(this.tag(c("ores/niter")), 1).save(this.output, "niter_from_ore");
        this.machine(Machine.CRUSHER, ModItems.SILICON_DUST.get(), 2).input(this.tag(Tags.Items.GEMS_QUARTZ), 1).save(this.output, "silicon_dust_from_quartz");
        this.machine(Machine.CRUSHER, ModItems.SILICON_DUST.get(), 1).input(this.tag(Tags.BlockItems.SANDS.item()), 1).ticks(200).save(this.output,
                "silicon_dust_from_sand");
    }

    // Chemical mixer: solid rocket fuel from aluminum powder, the oxidizer and a binder.
    private void mixing() {
        this.machine(Machine.CHEMICAL_MIXER, ModItems.ROCKET_FUEL.get(), 2)
                .input(this.tag(c("dusts/aluminum")), 1)
                .input(this.tag(c("dusts/niter")), 2)
                .input(Ingredient.of(Items.SLIME_BALL, Items.RESIN_CLUMP), 1)
                .save(this.output, "rocket_fuel");
    }

    // Circuit fabricator: wafers, and the three chips (D71), each built on the one before.
    private void fabricating() {
        this.machine(Machine.CIRCUIT_FABRICATOR, ModItems.SILICON_WAFER.get(), 2).input(this.tag(c("silicon")), 1).ticks(100).save(this.output,
                "silicon_wafer");
        this.machine(Machine.CIRCUIT_FABRICATOR, ModItems.BASIC_CHIP.get(), 1)
                .input(ModItems.SILICON_WAFER.get())
                .input(this.tag(Tags.Items.INGOTS_COPPER), 1)
                .input(this.tag(Tags.Items.DUSTS_REDSTONE), 2)
                .save(this.output, "basic_chip");
        this.machine(Machine.CIRCUIT_FABRICATOR, ModItems.ADVANCED_CHIP.get(), 1)
                .input(ModItems.BASIC_CHIP.get())
                .input(ModItems.SILICON_WAFER.get())
                .input(this.tag(Tags.Items.INGOTS_GOLD), 1)
                .input(this.tag(c("dusts/rare_earth")), 1)
                .ticks(300)
                .save(this.output, "advanced_chip");
        this.machine(Machine.CIRCUIT_FABRICATOR, ModItems.MILITARY_CHIP.get(), 1)
                .input(ModItems.ADVANCED_CHIP.get())
                .input(this.tag(c("dusts/rare_earth")), 2)
                .input(this.tag(Tags.Items.GEMS_DIAMOND), 1)
                .input(this.tag(c("dusts/lithium")), 1)
                .ticks(400)
                .energy(100)
                .save(this.output, "military_chip");
    }

    // Assembler (D74): airframes and batteries, the weapons' parts, the missile blocks and the missiles.
    private void assembling() {
        Ingredient aluminum = this.tag(ModTags.INGOTS_ALUMINUM);
        Ingredient iron = this.tag(Tags.Items.INGOTS_IRON);
        Ingredient redstone = this.tag(Tags.Items.DUSTS_REDSTONE);
        this.machine(Machine.ASSEMBLER, ModItems.AIRFRAME.get(), 1).input(aluminum, 4).input(iron, 1).save(this.output, "airframe");
        this.machine(Machine.ASSEMBLER, ModItems.BATTERY.get(), 1)
                .input(this.tag(c("ingots/lithium")), 1)
                .input(this.tag(Tags.Items.INGOTS_COPPER), 1)
                .input(redstone, 1)
                .save(this.output, "battery");

        // Weapon parts (D72).
        // Recipes differ in the kinds of items they take, not only in amounts: the barrel's copper sets it apart from the
        // airframe.
        this.machine(Machine.ASSEMBLER, ModItems.BARREL.get(), 1)
                .input(aluminum, 2)
                .input(iron, 1)
                .input(this.tag(Tags.Items.INGOTS_COPPER), 1)
                .save(this.output, "barrel");
        this.machine(Machine.ASSEMBLER, ModItems.GRIP.get(), 1).input(this.tag(Tags.Items.LEATHERS), 2).input(aluminum, 1).save(this.output, "grip");
        this.machine(Machine.ASSEMBLER, ModItems.FUSION_CORE.get(), 1)
                .input(ModItems.BASIC_CHIP.get())
                .input(Items.AMETHYST_SHARD)
                .input(aluminum, 1)
                .save(this.output, "fusion_core");

        // Missile blocks (DESIGN.md §6.4, §6b.5).
        this.machine(Machine.ASSEMBLER, ModItems.TARGET_DESIGNATOR.get(), 1)
                .input(Items.SPYGLASS)
                .input(ModItems.BASIC_CHIP.get())
                .input(aluminum, 2)
                .input(redstone, 1)
                .save(this.output, "target_designator");
        this.machine(Machine.ASSEMBLER, ModItems.MISSILE_LAUNCHER.get(), 1)
                .input(aluminum, 4)
                .input(this.tag(Tags.BlockItems.OBSIDIANS.item()), 2)
                .input(this.tag(Tags.BlockItems.STORAGE_BLOCKS_IRON.item()), 1)
                .input(ModItems.ADVANCED_CHIP.get())
                .input(Items.DISPENSER)
                .ticks(200)
                .save(this.output, "missile_launcher");
        this.machine(Machine.ASSEMBLER, ModItems.SILO_CASING.get(), 8)
                .input(this.tag(Tags.BlockItems.OBSIDIANS.item()), 4)
                .input(aluminum, 4)
                .input(this.tag(Tags.BlockItems.STORAGE_BLOCKS_IRON.item()), 1)
                .save(this.output, "silo_casing");
        this.machine(Machine.ASSEMBLER, ModItems.SILO_HATCH.get(), 1)
                .input(ModItems.SILO_CASING.get(), 2)
                .input(Items.IRON_TRAPDOOR)
                .input(aluminum, 2)
                .input(redstone, 1)
                .save(this.output, "silo_hatch");
        this.machine(Machine.ASSEMBLER, ModItems.SILO_CONTROLLER.get(), 1)
                .input(ModItems.SILO_CASING.get(), 2)
                .input(ModItems.MISSILE_LAUNCHER.get())
                .input(ModItems.MILITARY_CHIP.get())
                .input(this.tag(Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item()), 1)
                .ticks(300)
                .save(this.output, "silo_controller");
        this.machine(Machine.ASSEMBLER, ModItems.RADAR.get(), 1)
                .input(aluminum, 4)
                .input(Items.COMPASS)
                .input(Items.AMETHYST_SHARD, 2)
                .input(ModItems.ADVANCED_CHIP.get())
                .input(this.tag(Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item()), 1)
                .ticks(200)
                .save(this.output, "radar");
        this.machine(Machine.ASSEMBLER, ModItems.INTERCEPTOR.get(), 1)
                .input(aluminum, 4)
                .input(Items.FIREWORK_ROCKET, 2)
                .input(Items.DISPENSER)
                .input(ModItems.ADVANCED_CHIP.get())
                .ticks(200)
                .save(this.output, "interceptor");

        // Interceptor ammo (D66): an airframe, rocket fuel and a basic chip make four.
        this.machine(Machine.ASSEMBLER, ModItems.INTERCEPTOR_ROCKET.get(), 4)
                .input(ModItems.AIRFRAME.get())
                .input(ModItems.ROCKET_FUEL.get())
                .input(ModItems.BASIC_CHIP.get())
                .save(this.output, "interceptor_rocket");

        // Rocket launcher gear (D62, D63, D65, DESIGN.md §6b.6, §6b.7). The launcher itself is crate-only. Rockets differ
        // from an explosive missile I by the battery.
        this.machine(Machine.ASSEMBLER, ModItems.ROCKET.get(), 4)
                .input(ModItems.AIRFRAME.get())
                .input(ModItems.ROCKET_FUEL.get())
                .input(ModItems.BASIC_CHIP.get())
                .input(ModItems.BATTERY.get())
                .input(Items.TNT)
                .energy(60)
                .ticks(200)
                .save(this.output, "rocket");
        this.machine(Machine.ASSEMBLER, ModItems.TACTICAL_HELMET.get(), 1)
                .input(aluminum, 3)
                .input(this.tag(Tags.Items.GEMS_DIAMOND), 2)
                .input(ModItems.ADVANCED_CHIP.get())
                .input(this.tag(Tags.BlockItems.GLASS_PANES.item()), 1)
                .ticks(200)
                .save(this.output, "tactical_helmet");
        this.machine(Machine.ASSEMBLER, ModItems.THREAT_WARNING_IMPLANT.get(), 1)
                .input(ModItems.ADVANCED_CHIP.get())
                .input(this.tag(Tags.Items.INGOTS_GOLD), 1)
                .input(redstone, 2)
                .input(Items.AMETHYST_SHARD)
                .ticks(200)
                .save(this.output, "threat_warning_implant");
        this.machine(Machine.ASSEMBLER, ModItems.HANDHELD_RADAR.get(), 1)
                .input(ModItems.ADVANCED_CHIP.get())
                .input(ModItems.BATTERY.get())
                .input(Items.COMPASS)
                .input(aluminum, 2)
                .input(this.tag(Tags.BlockItems.GLASS_PANES.item()), 1)
                .ticks(200)
                .save(this.output, "handheld_radar");

        // Missiles (D48, D71): airframes, rocket fuel, the tier's chip and the type's payload. Tier III stays crate-only
        // (D57).
        this.missile(MissileType.EXPLOSIVE, List.of(payload(Items.TNT, 2)), List.of(payload(Items.TNT, 4)));
        this.missile(MissileType.BREACHING, List.of(payload(this.tag(Tags.Items.GEMS_DIAMOND), 1), payload(Items.TNT, 1)),
                List.of(payload(Ingredient.of(Items.NETHERITE_SCRAP), 2), payload(Items.TNT, 2)));
        this.missile(MissileType.EMP, List.of(payload(this.tag(BlockItemTags.LIGHTNING_RODS.item()), 2)),
                List.of(payload(this.tag(BlockItemTags.LIGHTNING_RODS.item()), 2), payload(this.tag(Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item()), 2)));
        this.missile(MissileType.INCENDIARY, List.of(payload(Items.BLAZE_POWDER, 4), payload(Items.TNT, 1)),
                List.of(payload(Items.BLAZE_ROD, 4), payload(Items.TNT, 2)));
    }

    private record Payload(Ingredient ingredient, int count) {
    }

    private static Payload payload(Ingredient ingredient, int count) {
        return new Payload(ingredient, count);
    }

    private static Payload payload(ItemLike item, int count) {
        return new Payload(Ingredient.of(item), count);
    }

    // Tier I: an airframe, 2 rocket fuel and a basic chip; tier II: 2 airframes, 4 rocket fuel and an advanced chip.
    private void missile(MissileType type, List<Payload> tierOne, List<Payload> tierTwo) {
        for (int tier = 1; tier <= 2; tier++) {
            MachineRecipeBuilder builder = this.machine(Machine.ASSEMBLER, ModItems.missile(type, tier).get(), 1)
                    .input(ModItems.AIRFRAME.get(), tier)
                    .input(ModItems.ROCKET_FUEL.get(), 2 * tier)
                    .input(ModItems.chip(tier))
                    .ticks(200 * tier)
                    .energy(80);
            for (Payload payload : tier == 1 ? tierOne : tierTwo) {
                builder.input(payload.ingredient(), payload.count());
            }
            builder.save(this.output, type.getSerializedName() + "_missile_" + tier);
        }
    }

    private MachineRecipeBuilder machine(Machine machine, ItemLike result, int count) {
        return new MachineRecipeBuilder(machine, result, count);
    }

    // A common c: item tag (D47), such as c:dusts/niter.
    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
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
