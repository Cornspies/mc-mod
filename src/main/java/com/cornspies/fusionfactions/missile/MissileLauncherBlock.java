package com.cornspies.fusionfactions.missile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Missile launcher (D55). Anyone can open it (D58). Breaking it drops its missiles and designator.
public class MissileLauncherBlock extends BaseEntityBlock {
    public MissileLauncherBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MissileLauncherBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MissileLauncherBlockEntity launcher) {
            player.openMenu(launcher, launcher.menuPos());
        }
        return InteractionResult.SUCCESS;
    }
}
