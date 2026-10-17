package com.cornspies.fusionfactions.client;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.industry.ProcessingMachineBlock;
import com.cornspies.fusionfactions.missile.InterceptorRocketEntity;
import com.cornspies.fusionfactions.power.ChargingStationBlock;
import com.cornspies.fusionfactions.power.CombustionGeneratorBlock;
import com.cornspies.fusionfactions.power.MachineBlock;
import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.weapon.GuidedRocketEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// Sounds that loop for as long as something runs: the machines, the generator and the charging station while they
// work (their LIT state, which the client already has), and rockets while they fly. The client starts and stops
// them itself, so the server sends nothing extra.
public final class LoopingSounds {
    private static final int SCAN_INTERVAL = 10;
    private static final int SCAN_CHUNKS = 2;
    // Loops start within this distance and stop beyond it; the sounds fade out well before (sounds.json).
    private static final double BLOCK_RANGE = 24.0;
    private static final float FADE_PER_TICK = 0.1F;

    private static final Map<BlockPos, BlockLoop> BLOCK_LOOPS = new HashMap<>();
    private static int scanTimer;

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            BLOCK_LOOPS.clear();
            return;
        }
        BLOCK_LOOPS.values().removeIf(BlockLoop::isStopped);
        if (scanTimer-- > 0) {
            return;
        }
        scanTimer = SCAN_INTERVAL;
        ChunkPos center = player.chunkPosition();
        for (int dx = -SCAN_CHUNKS; dx <= SCAN_CHUNKS; dx++) {
            for (int dz = -SCAN_CHUNKS; dz <= SCAN_CHUNKS; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(center.x() + dx, center.z() + dz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (BLOCK_LOOPS.containsKey(pos) || pos.distToCenterSqr(player.position()) > BLOCK_RANGE * BLOCK_RANGE) {
                        continue;
                    }
                    SoundEvent sound = runningSound(level.getBlockState(pos));
                    if (sound != null) {
                        BlockLoop loop = new BlockLoop(sound, pos.immutable());
                        BLOCK_LOOPS.put(loop.pos, loop);
                        minecraft.getSoundManager().play(loop);
                    }
                }
            }
        }
    }

    // The loop a block plays in this state, or null if it isn't working.
    private static @Nullable SoundEvent runningSound(BlockState state) {
        if (!state.hasProperty(MachineBlock.LIT) || !state.getValue(MachineBlock.LIT)) {
            return null;
        }
        Block block = state.getBlock();
        if (block instanceof ProcessingMachineBlock machine) {
            return ModSounds.MACHINE_RUNNING.get(machine.machine()).get();
        }
        if (block instanceof CombustionGeneratorBlock) {
            return ModSounds.GENERATOR_RUNNING.get();
        }
        if (block instanceof ChargingStationBlock) {
            return ModSounds.CHARGING_STATION_RUNNING.get();
        }
        return null;
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof GuidedRocketEntity rocket) {
            Minecraft.getInstance().getSoundManager().play(new EntityLoop(rocket, 1.0F, 1.0F));
        } else if (event.getEntity() instanceof InterceptorRocketEntity rocket) {
            Minecraft.getInstance().getSoundManager().play(new EntityLoop(rocket, 0.7F, 1.4F));
        }
    }

    // Fades in, plays while the block keeps working and the player stays near, then fades out.
    private static final class BlockLoop extends AbstractTickableSoundInstance {
        private final SoundEvent event;
        private final BlockPos pos;

        BlockLoop(SoundEvent event, BlockPos pos) {
            super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.event = event;
            this.pos = pos;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                this.stop();
                return;
            }
            boolean running = runningSound(minecraft.level.getBlockState(this.pos)) == this.event
                    && this.pos.distToCenterSqr(minecraft.player.position()) <= BLOCK_RANGE * BLOCK_RANGE;
            if (running) {
                this.volume = Math.min(1.0F, this.volume + FADE_PER_TICK);
            } else {
                this.volume -= FADE_PER_TICK;
                if (this.volume <= 0.0F) {
                    this.stop();
                }
            }
        }
    }

    // Follows an entity until it is removed.
    private static final class EntityLoop extends AbstractTickableSoundInstance {
        private final Entity entity;

        EntityLoop(Entity entity, float volume, float pitch) {
            super(ModSounds.ROCKET_FLY.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.entity = entity;
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.pitch = pitch;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }

        @Override
        public void tick() {
            if (this.entity.isRemoved()) {
                this.stop();
                return;
            }
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }
    }

    private LoopingSounds() {
    }
}
