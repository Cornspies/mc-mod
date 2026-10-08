package com.cornspies.fusionfactions.missile;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// Target designator (D49): right click marks the block you look at, up to designatorRange blocks away, as a
// missile target. Sneak + right click clears it. Put it into a launcher to aim the launcher there.
// The server does the ray cast, so the target is always a block the player really looked at.
public class TargetDesignatorItem extends Item {
    public TargetDesignatorItem(Item.Properties properties) {
        super(properties);
    }

    public static @Nullable GlobalPos target(ItemStack stack) {
        return stack.get(ModDataComponents.TARGET.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            stack.remove(ModDataComponents.TARGET.get());
            player.sendOverlayMessage(Component.translatable("fusionfactions.designator.cleared"));
            return InteractionResult.SUCCESS;
        }
        HitResult hit = player.pick(MissileConfig.designatorRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            player.sendOverlayMessage(Component.translatable("fusionfactions.designator.no_block", MissileConfig.designatorRange())
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        BlockPos pos = blockHit.getBlockPos();
        stack.set(ModDataComponents.TARGET.get(), GlobalPos.of(serverLevel.dimension(), pos));
        player.sendOverlayMessage(Component.translatable("fusionfactions.designator.set", pos.getX(), pos.getY(), pos.getZ()));
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.6F);
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        GlobalPos target = target(stack);
        if (target == null) {
            builder.accept(Component.translatable("tooltip.fusionfactions.designator.none").withStyle(ChatFormatting.GRAY));
        } else {
            BlockPos pos = target.pos();
            builder.accept(Component.translatable("tooltip.fusionfactions.designator.target", pos.getX(), pos.getY(), pos.getZ(),
                    target.dimension().identifier().toString()).withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable("tooltip.fusionfactions.designator.use", MissileConfig.designatorRange()).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return target(stack) != null;
    }
}
