package com.cornspies.fusionfactions.client;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// How weapons are held (D29, D38): in third person both arms raised like a loaded crossbow; in first
// person the weapon moves from the hip to the eye while aiming down sights, and kicks back when fired.
public final class WeaponItemExtensions implements IClientItemExtensions {
    private static final WeaponItemExtensions INSTANCE = new WeaponItemExtensions();
    // Vanilla's first-person hand position (FirstPersonHandsAndItemsRenderer.applyItemArmTransform).
    private static final float HIP_X = 0.56F;
    private static final float HIP_Y = -0.52F;
    private static final float HIP_Z = -0.72F;
    private static final float EQUIP_DROP = -0.6F;
    // How far a shot pushes the weapon back towards the eye, in blocks.
    private static final float KICK = 0.06F;

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(INSTANCE, ModItems.WEAPONS.stream().map(weapon -> weapon.get()).toArray(net.minecraft.world.item.Item[]::new));
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, PlayerRenderState playerRenderState, HumanoidArm arm, ItemStack stack, float partialTick,
            float equipProcess, float swingProcess) {
        // Only the weapon in the main hand aims. Checked by arm, not by stack object: the stack is replaced
        // with a new copy whenever the server updates it (every shot and every loaded round).
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || arm != minecraft.player.getMainArm()) {
            return false;
        }
        float aim = WeaponAim.progress(partialTick);
        aim = aim * aim * (3.0F - 2.0F * aim);
        float kick = WeaponAim.kick(partialTick) * KICK;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float[] eye = AimData.offset(stack.getItem(), WeaponAim.sightTier(stack));
        float drop = equipProcess * EQUIP_DROP;
        // No vanilla swing: the attack button fires instead of swinging.
        poseStack.translate(Mth.lerp(aim, side * HIP_X, eye[2]), Mth.lerp(aim, HIP_Y, eye[0]) + drop, Mth.lerp(aim, HIP_Z, eye[1]) + kick);
        return true;
    }

    private WeaponItemExtensions() {
    }
}
