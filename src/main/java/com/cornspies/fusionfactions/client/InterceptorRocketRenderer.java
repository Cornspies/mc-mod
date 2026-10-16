package com.cornspies.fusionfactions.client;

import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.InterceptorRocketEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

// Draws an interceptor's rocket (entity/interceptor_rocket from tools/machine_models.py), nose first along the way
// it moved since the last tick.
public class InterceptorRocketRenderer extends EntityRenderer<InterceptorRocketEntity, InterceptorRocketRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> MODEL = new StandaloneModelKey<>(
            () -> FusionFactions.MODID + ":entity/interceptor_rocket");
    // The model is 12 pixels long; drawn a bit larger so it can be seen at the speed it flies.
    private static final float SCALE = 1.5F;
    private static final float MODEL_MIDDLE = 6.0F / 16.0F;

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
    }

    public InterceptorRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InterceptorRocketEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Vec3 motion = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        double length = motion.length();
        if (length < 1.0E-4) {
            state.yaw = 0.0F;
            state.pitch = 0.0F;
            return;
        }
        state.yaw = (float) Math.toDegrees(Mth.atan2(motion.x, motion.z));
        state.pitch = (float) Math.toDegrees(Math.acos(Mth.clamp(motion.y / length, -1.0, 1.0)));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BlockStateModelPart model = Minecraft.getInstance().getModelManager().getStandaloneModel(MODEL);
        if (model != null) {
            poseStack.pushPose();
            // The model points up (+y); turn it towards +z by the pitch, then around the vertical axis by the yaw.
            poseStack.rotateDegrees(Axis.YP, state.yaw);
            poseStack.rotateDegrees(Axis.XP, state.pitch);
            poseStack.scale(SCALE, SCALE, SCALE);
            poseStack.translate(-0.5F, -MODEL_MIDDLE, -0.5F);
            submitNodeCollector.submitBlockModel(poseStack, Sheets.cutoutBlockItemSheet(), List.of(model), BlockModelRenderState.EMPTY_TINTS,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
