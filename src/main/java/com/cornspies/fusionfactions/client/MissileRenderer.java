package com.cornspies.fusionfactions.client;

import com.cornspies.fusionfactions.missile.MissileEntity;
import com.cornspies.fusionfactions.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// Draws a flying missile with its item's 3D model (tools/missile_models.py), nose up on the climb and nose
// down on the way down, MissileEntity.MODEL_SCALE times the item's size.
public class MissileRenderer extends EntityRenderer<MissileEntity, MissileRenderer.State> {
    // The model's tail and nose, in blocks from its centre, before scaling (y -5.2 and 24 of 16).
    private static final float MODEL_TAIL = 5.2F / 16.0F + 0.5F;
    private static final float MODEL_NOSE = 24.0F / 16.0F - 0.5F;

    private final ItemModelResolver itemModelResolver;

    public static class State extends EntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        boolean descending;
    }

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MissileEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.descending = entity.isDescending();
        ItemStack stack = new ItemStack(ModItems.missile(entity.missileType(), entity.tier()).get());
        this.itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.NONE, entity);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        float scale = MissileEntity.MODEL_SCALE;
        if (state.descending) {
            // Nose down, touching the entity's position, so it hits where it is drawn.
            poseStack.translate(0.0F, MODEL_NOSE * scale, 0.0F);
            poseStack.rotateDegrees(Axis.XP, 180.0F);
        } else {
            poseStack.translate(0.0F, MODEL_TAIL * scale, 0.0F);
        }
        poseStack.scale(scale, scale, scale);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
