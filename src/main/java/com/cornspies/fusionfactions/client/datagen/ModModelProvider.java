package com.cornspies.fusionfactions.client.datagen;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlock;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, FusionFactions.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // One model per rarity: supply_crate_<rarity>, textured with supply_crate_<rarity>_top / _side.
        Block crate = ModBlocks.SUPPLY_CRATE.get();
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(crate)
                .with(PropertyDispatch.initial(SupplyCrateBlock.RARITY).generate(rarity -> {
                    String suffix = "_" + rarity.getSerializedName();
                    Material top = TextureMapping.getBlockTexture(crate, suffix + "_top");
                    TextureMapping textures = new TextureMapping()
                            .put(TextureSlot.TOP, top)
                            .put(TextureSlot.BOTTOM, top)
                            .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(crate, suffix + "_side"));
                    return BlockModelGenerators.plainVariant(
                            ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(crate, suffix, textures, blockModels.modelOutput));
                })));

        // Placeholder sprites. Weapon models are OD-13 (M3).
        itemModels.generateFlatItem(ModItems.PISTOL.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.AMMO.get(), ModelTemplates.FLAT_ITEM);
    }
}
