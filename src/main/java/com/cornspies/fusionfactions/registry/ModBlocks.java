package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.InterceptorBlock;
import com.cornspies.fusionfactions.missile.MissileLauncherBlock;
import com.cornspies.fusionfactions.missile.RadarBlock;
import com.cornspies.fusionfactions.missile.SiloCasingBlock;
import com.cornspies.fusionfactions.missile.SiloControllerBlock;
import com.cornspies.fusionfactions.missile.SiloHatchBlock;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlock;
import com.cornspies.fusionfactions.workbench.WeaponWorkbenchBlock;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FusionFactions.MODID);

    // No block item: crates only come from drops.
    public static final DeferredBlock<SupplyCrateBlock> SUPPLY_CRATE = BLOCKS.registerBlock("supply_crate", SupplyCrateBlock::new, properties -> properties
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            // Unbreakable and blast-proof like bedrock (D17).
            .strength(-1.0F, 3_600_000.0F)
            .noLootTable()
            .pushReaction(PushReaction.IMMOVEABLE));

    // Weapon workbench (D30). Crafted; breaks like a crafting table, a bit tougher.
    public static final DeferredBlock<WeaponWorkbenchBlock> WEAPON_WORKBENCH = BLOCKS.registerBlock("weapon_workbench", WeaponWorkbenchBlock::new,
            properties -> properties
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(3.0F, 6.0F));

    // Missile launcher (D55). As hard as an iron block; anyone can break it (D58).
    public static final DeferredBlock<MissileLauncherBlock> MISSILE_LAUNCHER = BLOCKS.registerBlock("missile_launcher", MissileLauncherBlock::new,
            properties -> properties
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F));

    // Missile silo (D55, DESIGN.md §6.8): casing, hatch and controller, as hard as the launcher.
    public static final DeferredBlock<SiloCasingBlock> SILO_CASING = BLOCKS.registerBlock("silo_casing", SiloCasingBlock::new, ModBlocks::siloProperties);
    public static final DeferredBlock<SiloHatchBlock> SILO_HATCH = BLOCKS.registerBlock("silo_hatch", SiloHatchBlock::new, ModBlocks::siloProperties);
    public static final DeferredBlock<SiloControllerBlock> SILO_CONTROLLER = BLOCKS.registerBlock("silo_controller", SiloControllerBlock::new,
            ModBlocks::siloProperties);

    // Counterplay (D53, DESIGN.md §6.9), as hard as the launcher. Their models are smaller than a block.
    public static final DeferredBlock<RadarBlock> RADAR = BLOCKS.registerBlock("radar", RadarBlock::new,
            properties -> siloProperties(properties).noOcclusion());
    public static final DeferredBlock<InterceptorBlock> INTERCEPTOR = BLOCKS.registerBlock("interceptor", InterceptorBlock::new,
            properties -> siloProperties(properties).noOcclusion());

    // Ores (D69, DESIGN.md §6b.2), as hard as iron ore. Bauxite and niter need a stone pickaxe, rare earth and lithium
    // an iron one (block tags minecraft:needs_*_tool). Niter drops experience like redstone ore.
    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = ore("bauxite_ore", ConstantInt.of(0), false);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = ore("deepslate_bauxite_ore", ConstantInt.of(0), true);
    public static final DeferredBlock<DropExperienceBlock> NITER_ORE = ore("niter_ore", UniformInt.of(1, 4), false);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_NITER_ORE = ore("deepslate_niter_ore", UniformInt.of(1, 4), true);
    public static final DeferredBlock<DropExperienceBlock> RARE_EARTH_ORE = ore("rare_earth_ore", ConstantInt.of(0), false);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_RARE_EARTH_ORE = ore("deepslate_rare_earth_ore", ConstantInt.of(0), true);
    public static final DeferredBlock<DropExperienceBlock> LITHIUM_ORE = ore("lithium_ore", ConstantInt.of(0), false);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_LITHIUM_ORE = ore("deepslate_lithium_ore", ConstantInt.of(0), true);

    private static DeferredBlock<DropExperienceBlock> ore(String name, IntProvider experience, boolean deepslate) {
        return BLOCKS.registerBlock(name, properties -> new DropExperienceBlock(experience, properties), properties -> deepslate
                ? properties.mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F)
                        .sound(SoundType.DEEPSLATE)
                : properties.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F));
    }

    private static BlockBehaviour.Properties siloProperties(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F);
    }

    private ModBlocks() {
    }
}
