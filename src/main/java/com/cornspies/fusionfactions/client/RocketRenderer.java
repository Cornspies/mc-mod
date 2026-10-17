package com.cornspies.fusionfactions.client;

import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
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
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

// Draws a rocket from tools/machine_models.py, nose first along its flight (the rotation the server gives it): an
// interceptor's rocket (entity/interceptor_rocket) or a rocket launcher's (entity/guided_rocket). The models point up.
public class RocketRenderer extends EntityRenderer<Entity, RocketRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> INTERCEPTOR = new StandaloneModelKey<>(
            () -> FusionFactions.MODID + ":entity/interceptor_rocket");
    public static final StandaloneModelKey<BlockStateModelPart> GUIDED = new StandaloneModelKey<>(() -> FusionFactions.MODID + ":entity/guided_rocket");

    private final StandaloneModelKey<BlockStateModelPart> model;
    private final float scale;
    // Half the model's length, in blocks.
    private final float middle;

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
    }

    public RocketRenderer(EntityRendererProvider.Context context, StandaloneModelKey<BlockStateModelPart> model, float scale, float lengthPixels) {
        super(context);
        this.model = model;
        this.scale = scale;
        this.middle = lengthPixels / 32.0F;
    }

    // The interceptor's rocket is 12 pixels long, drawn a bit larger so it can be seen at the speed it flies.
    public static RocketRenderer interceptor(EntityRendererProvider.Context context) {
        return new RocketRenderer(context, INTERCEPTOR, 1.5F, 12.0F);
    }

    public static RocketRenderer guided(EntityRendererProvider.Context context) {
        return new RocketRenderer(context, GUIDED, 1.0F, 16.0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Entity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // The server keeps the rocket turned along its flight (yaw from atan2(x, z), pitch up from the horizon) and
        // syncs that. The client gets new positions between its ticks, so its own last step is no use here.
        state.yaw = entity.getYRot(partialTicks);
        state.pitch = 90.0F - entity.getXRot(partialTicks);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BlockStateModelPart model = Minecraft.getInstance().getModelManager().getStandaloneModel(this.model);
        if (model != null) {
            poseStack.pushPose();
            // The model points up (+y); turn it towards +z by the pitch, then around the vertical axis by the yaw.
            poseStack.rotateDegrees(Axis.YP, state.yaw);
            poseStack.rotateDegrees(Axis.XP, state.pitch);
            poseStack.scale(this.scale, this.scale, this.scale);
            poseStack.translate(-0.5F, -this.middle, -0.5F);
            submitNodeCollector.submitBlockModel(poseStack, Sheets.cutoutBlockItemSheet(), List.of(model), BlockModelRenderState.EMPTY_TINTS,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
