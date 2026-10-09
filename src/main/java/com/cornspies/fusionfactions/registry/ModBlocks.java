package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.MissileLauncherBlock;
import com.cornspies.fusionfactions.missile.SiloCasingBlock;
import com.cornspies.fusionfactions.missile.SiloControllerBlock;
import com.cornspies.fusionfactions.missile.SiloHatchBlock;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlock;
import com.cornspies.fusionfactions.workbench.WeaponWorkbenchBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    private static BlockBehaviour.Properties siloProperties(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F);
    }

    private ModBlocks() {
    }
}
