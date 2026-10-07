package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.supplydrop.FallingCrateEntity;
import com.cornspies.fusionfactions.supplydrop.SupplyCrateBlock;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;

// Draws the falling crate as its crate block, the same way vanilla draws falling blocks.
public class FallingCrateRenderer extends EntityRenderer<FallingCrateEntity, FallingBlockRenderState> {
    public FallingCrateRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public FallingBlockRenderState createRenderState() {
        return new FallingBlockRenderState();
    }

    @Override
    public void extractRenderState(FallingCrateEntity entity, FallingBlockRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.movingBlockRenderState.randomSeedPos = pos;
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = SupplyCrateBlock.stateFor(entity.getRarity());
        if (entity.level() instanceof ClientLevel level) {
            state.movingBlockRenderState.biome = level.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = level.cardinalLighting();
            state.movingBlockRenderState.lightEngine = level.getLightEngine();
        }
    }

    @Override
    public void submit(FallingBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(-0.5, 0.0, -0.5);
        submitNodeCollector.submitMovingBlock(poseStack, state.movingBlockRenderState, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
