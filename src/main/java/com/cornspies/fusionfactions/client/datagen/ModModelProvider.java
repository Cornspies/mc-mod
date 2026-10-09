package com.cornspies.fusionfactions.client.datagen;

import java.util.ArrayList;
import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.client.UpgradeTierProperty;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.missile.SiloHatchBlock;
import com.cornspies.fusionfactions.missile.SiloStructure;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlock;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
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

        blockModels.createTrivialBlock(ModBlocks.WEAPON_WORKBENCH.get(), TexturedModel.CUBE_BOTTOM_TOP);
        blockModels.createTrivialBlock(ModBlocks.MISSILE_LAUNCHER.get(), TexturedModel.CUBE_BOTTOM_TOP);
        blockModels.createTrivialBlock(ModBlocks.RADAR.get(), TexturedModel.CUBE_BOTTOM_TOP);
        blockModels.createTrivialBlock(ModBlocks.INTERCEPTOR.get(), TexturedModel.CUBE_BOTTOM_TOP);

        // Missile silo: casing on every side; the controller has a panel on its sides; the hatch's top opens. Once
        // the silo is complete, each block shows the model for its part of it (tools/silo_textures.py).
        Block casing = ModBlocks.SILO_CASING.get();
        Identifier casingModel = TexturedModel.CUBE.create(casing, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(casing).with(PropertyDispatch.initial(SiloStructure.PART)
                .generate(part -> BlockModelGenerators.plainVariant(part == 0 ? casingModel : silo("casing_" + part)))));
        blockModels.registerSimpleItemModel(casing, casingModel);
        Material casingTexture = TextureMapping.getBlockTexture(casing);
        Block controller = ModBlocks.SILO_CONTROLLER.get();
        Identifier controllerModel = ModelTemplates.CUBE_BOTTOM_TOP.create(controller, new TextureMapping()
                .put(TextureSlot.TOP, casingTexture)
                .put(TextureSlot.BOTTOM, casingTexture)
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(controller)), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(controller).with(PropertyDispatch.initial(SiloStructure.PART)
                .generate(part -> BlockModelGenerators.plainVariant(part == 0 ? controllerModel : silo("controller_" + part)))));
        blockModels.registerSimpleItemModel(controller, controllerModel);
        Block hatch = ModBlocks.SILO_HATCH.get();
        Identifier closed = ModelTemplates.CUBE_BOTTOM_TOP.create(hatch, new TextureMapping()
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(hatch, "_top"))
                .put(TextureSlot.BOTTOM, casingTexture)
                .put(TextureSlot.SIDE, casingTexture), blockModels.modelOutput);
        Identifier open = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(hatch, "_open", new TextureMapping()
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(hatch, "_top_open"))
                .put(TextureSlot.BOTTOM, casingTexture)
                .put(TextureSlot.SIDE, casingTexture), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(hatch).with(PropertyDispatch.initial(SiloStructure.FORMED, SiloHatchBlock.OPEN)
                .generate((formed, isOpen) -> BlockModelGenerators.plainVariant(formed
                        ? silo(isOpen ? "hatch_open" : "hatch")
                        : isOpen ? open : closed))));
        blockModels.registerSimpleItemModel(hatch, closed);

        // Missiles: a flat icon in inventories, on the ground and in frames; the 3D model from tools/missile_models.py
        // in hand and in flight (client.MissileRenderer).
        for (MissileType type : MissileType.values()) {
            for (int tier = 1; tier <= MissileType.MAX_TIER; tier++) {
                var missile = ModItems.missile(type, tier);
                ItemModel.Unbaked flat = ItemModelUtils.plainModel(itemModels.createFlatItemModel(missile.get(), ModelTemplates.FLAT_ITEM));
                ItemModel.Unbaked model = ItemModelUtils.plainModel(missile.getId().withPath(path -> "item/" + path + "_3d"));
                itemModels.itemModelOutput.accept(missile.get(), ItemModelGenerators.createFlatModelDispatch(flat, model));
            }
        }
        itemModels.generateFlatItem(ModItems.TARGET_DESIGNATOR.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        // Weapons use the 3D models from tools/weapon_models.py (D29), kept in src/main/resources. On top of the
        // gun, each upgrade type shows the installed part's model for its tier, or nothing (D34).
        for (var weapon : ModItems.WEAPONS) {
            Identifier base = weapon.getId().withPrefix("item/");
            List<ItemModel.Unbaked> layers = new ArrayList<>();
            layers.add(ItemModelUtils.plainModel(base));
            for (UpgradeType type : UpgradeType.values()) {
                List<RangeSelectItemModel.Entry> tiers = new ArrayList<>();
                for (int tier = 1; tier <= WeaponUpgrades.MAX_TIER; tier++) {
                    Identifier part = base.withSuffix("/" + type.getSerializedName() + "_" + tier);
                    tiers.add(ItemModelUtils.override(ItemModelUtils.plainModel(part), tier));
                }
                layers.add(ItemModelUtils.rangeSelect(new UpgradeTierProperty(type), new EmptyModel.Unbaked(), tiers));
            }
            itemModels.itemModelOutput.accept(weapon.get(), ItemModelUtils.composite(layers.toArray(ItemModel.Unbaked[]::new)));
        }
        for (UpgradeType type : UpgradeType.values()) {
            for (int tier = 1; tier <= WeaponUpgrades.MAX_TIER; tier++) {
                itemModels.generateFlatItem(ModItems.upgrade(type, tier).get(), ModelTemplates.FLAT_ITEM);
            }
        }
        itemModels.generateFlatItem(ModItems.AMMO.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.BARREL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FUSION_CORE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GRIP.get(), ModelTemplates.FLAT_ITEM);
    }

    // A formed silo model from tools/silo_textures.py.
    private static Identifier silo(String name) {
        return FusionFactions.id("block/silo/" + name);
    }
}
