package com.cornspies.fusionfactions.missile;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

// Missile silo controller (D55): fires every tier, tier III included, out of a complete silo (SiloStructure).
// Missiles start at the bottom of the shaft and leave through the hatch, which needs open sky above it.
public class SiloControllerBlockEntity extends AbstractLauncherBlockEntity {
    public SiloControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SILO_CONTROLLER.get(), pos, state);
    }

    @Override
    protected int maxTier() {
        return MissileType.MAX_TIER;
    }

    @Override
    protected int cooldownTicks() {
        return MissileConfig.siloCooldownTicks();
    }

    @Override
    protected LaunchSite site(ServerLevel level) {
        SiloStructure.Match match = SiloStructure.find(level, this.worldPosition);
        BlockPos hatch = match.hatch();
        if (hatch == null) {
            return new LaunchSite(null, this.worldPosition.above(), match.missing());
        }
        return new LaunchSite(hatch.below(SiloStructure.DEPTH), hatch.above(), 0);
    }

    @Override
    protected void onLaunch(ServerLevel level, LaunchSite site) {
        if (site.launchPos() != null) {
            SiloHatchBlock.open(level, site.launchPos().above(SiloStructure.DEPTH));
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + FusionFactions.MODID + ".missile_silo");
    }
}
