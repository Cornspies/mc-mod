package com.cornspies.fusionfactions.client;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// Held pose for weapons (D29): players aim them with both arms raised, like a loaded crossbow.
public final class WeaponItemExtensions implements IClientItemExtensions {
    private static final WeaponItemExtensions INSTANCE = new WeaponItemExtensions();

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(INSTANCE, ModItems.PISTOL.get(), ModItems.AUTO_PISTOL.get(), ModItems.SMG.get(), ModItems.SHOTGUN.get(),
                ModItems.SNIPER_RIFLE.get(), ModItems.GATLING_GUN.get());
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    private WeaponItemExtensions() {
    }
}
