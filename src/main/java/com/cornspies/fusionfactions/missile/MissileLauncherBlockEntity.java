package com.cornspies.fusionfactions.missile;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

// Missile launcher (D55): fires tiers I–II from its own block, which needs open sky above it.
public class MissileLauncherBlockEntity extends AbstractLauncherBlockEntity {
    public MissileLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.MISSILE_LAUNCHER.get(), pos, state);
    }

    @Override
    protected int maxTier() {
        return MissileItem.LAUNCHER_MAX_TIER;
    }

    @Override
    protected int cooldownTicks() {
        return MissileConfig.launcherCooldownTicks();
    }

    @Override
    protected LaunchSite site(ServerLevel level) {
        return new LaunchSite(this.worldPosition, this.worldPosition.above(), 0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + FusionFactions.MODID + ".missile_launcher");
    }
}
