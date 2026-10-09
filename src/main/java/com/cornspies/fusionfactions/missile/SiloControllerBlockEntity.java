package com.cornspies.fusionfactions.missile;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Missile silo controller (D55): fires every tier, tier III included, out of a complete silo (SiloStructure).
// Missiles start at the bottom of the shaft and leave through the hatch, which needs open sky above it.
public class SiloControllerBlockEntity extends AbstractLauncherBlockEntity {
    // How often the controller looks at its silo, and how long the formation animation spends on each layer.
    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final int LAYER_TICKS = 8;

    // The hatch of the silo this controller has formed, or null.
    private @Nullable BlockPos formedHatch;
    // While forming: the next layer to light up, counted down from the floor (DEPTH) to the hatch (0). -1 when idle.
    private int formingLayer = -1;
    private int formingTimer;

    public SiloControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SILO_CONTROLLER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SiloControllerBlockEntity controller) {
        if (level instanceof ServerLevel serverLevel) {
            controller.tick(serverLevel);
        }
    }

    // Forms the silo once it's complete: layer by layer from the floor up, with sparks and rising tones, then
    // a beam of light out of the hatch. Turns it back to plain blocks as soon as it's no longer complete.
    private void tick(ServerLevel level) {
        if (this.formingLayer >= 0 && this.formedHatch != null) {
            if (--this.formingTimer <= 0) {
                this.formLayer(level, this.formedHatch);
            }
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        BlockPos hatch = SiloStructure.find(level, this.worldPosition).hatch();
        if (Objects.equals(hatch, this.formedHatch)) {
            return;
        }
        if (this.formedHatch != null) {
            this.unform(level);
        }
        this.formedHatch = hatch;
        this.setChanged();
        if (hatch != null) {
            this.formingLayer = SiloStructure.DEPTH;
            this.formingTimer = LAYER_TICKS;
        }
    }

    private void formLayer(ServerLevel level, BlockPos hatch) {
        int layer = this.formingLayer;
        SiloStructure.setFormed(level, hatch, layer, true);
        BlockPos center = hatch.below(layer);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 40, 1.0, 0.4, 1.0, 0.05);
        level.playSound(null, center, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 0.6F + 0.25F * (SiloStructure.DEPTH - layer));
        this.formingLayer--;
        this.formingTimer = LAYER_TICKS;
        if (this.formingLayer < 0) {
            level.playSound(null, hatch, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            for (int i = 0; i < 12; i++) {
                level.sendParticles(ParticleTypes.END_ROD, hatch.getX() + 0.5, hatch.getY() - SiloStructure.DEPTH + 1 + i * 0.6, hatch.getZ() + 0.5,
                        3, 0.15, 0.1, 0.15, 0.01);
            }
        }
    }

    private void unform(ServerLevel level) {
        if (this.formedHatch == null) {
            return;
        }
        for (int layer = 0; layer <= SiloStructure.DEPTH; layer++) {
            SiloStructure.setFormed(level, this.formedHatch, layer, false);
        }
        level.playSound(null, this.formedHatch, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.formedHatch = null;
        this.formingLayer = -1;
        this.setChanged();
    }

    // Breaking the controller turns the rest of its silo back to plain blocks.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel level) {
            this.unform(level);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("FormedHatch", BlockPos.CODEC, this.formedHatch);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.formedHatch = input.read("FormedHatch", BlockPos.CODEC).orElse(null);
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
