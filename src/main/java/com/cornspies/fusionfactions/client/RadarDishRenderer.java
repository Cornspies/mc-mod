package com.cornspies.fusionfactions.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.RadarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

// Draws the radar's dish (block/radar_dish from tools/machine_models.py), turned to the angle the radar keeps on
// the client. The rest of the radar is its block model.
public class RadarDishRenderer implements BlockEntityRenderer<RadarBlockEntity, RadarDishRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> DISH = new StandaloneModelKey<>(() -> FusionFactions.MODID + ":block/radar_dish");

    public static class State extends BlockEntityRenderState {
        float angle;
    }

    public RadarDishRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RadarBlockEntity radar, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(radar, state, partialTicks, cameraPosition, breakProgress);
        state.angle = radar.dishAngle(partialTicks);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BlockStateModelPart dish = Minecraft.getInstance().getModelManager().getStandaloneModel(DISH);
        if (dish == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, -state.angle);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        submitNodeCollector.submitBlockModel(poseStack, Sheets.cutoutBlockItemSheet(), List.of(dish), BlockModelRenderState.EMPTY_TINTS,
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
