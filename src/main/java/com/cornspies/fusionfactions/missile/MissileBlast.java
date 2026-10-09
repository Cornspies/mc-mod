package com.cornspies.fusionfactions.missile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.registry.ModTags;
import com.cornspies.fusionfactions.weapon.ModDamageTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// What happens on impact (DESIGN.md §6.6, D51, D52): explosive and breaching blasts, incendiary fire, EMP. Like a vanilla explosion, rays go out from the center
// and lose strength in every block they pass, by its blast resistance, so walls shield what is behind
// them. Missiles punch through blocks better than TNT, but hard blocks (obsidian class) stop them, and
// #fusionfactions:missile_immune never breaks. Broken blocks drop nothing; containers spill their contents.
// NeoForge's explosion events fire first, so claim mods that protect against explosions work too.
public final class MissileBlast {
    // Rays advance this far per step, in blocks.
    private static final float STEP = 0.3F;
    // Strength a ray loses per block of air. Sets the reach: power / AIR_LOSS blocks (about 5 / 7 / 9).
    static final float AIR_LOSS = 1.3F;
    // Share of a block's blast resistance a ray loses per block. Vanilla explosions lose about 3.3 times
    // the resistance per block, so missiles get through stone about 13 times better than TNT.
    private static final float RESISTANCE_FACTOR = 0.25F;
    // How often a ray varies in strength, like vanilla: 0.7 to 1.3 times the power.
    private static final float MIN_RAY = 0.7F;
    private static final float RAY_SPREAD = 0.6F;
    private static final int GRID = 16;
    // Incendiary missiles: how long entities in the fire radius burn, and 1 in how many spots catch fire.
    private static final float BURN_SECONDS = 8.0F;
    private static final int FIRE_CHANCE = 3;
    private static final int EMP_PARTICLES = 400;
    // What breaching missiles treat hard blocks as: stone.
    private static final float BREACHED_RESISTANCE = 6.0F;
    // Players within this distance get the explosion effects and their knockback (vanilla sends 64).
    private static final double EFFECT_DISTANCE = 96.0;
    // The explosion sound carries this many blocks (16 per volume).
    private static final float SOUND_VOLUME = 16.0F;

    // The blocks a blast breaks, the explosion object their blocks get to see, and what happens once they are
    // broken (incendiary fire). The caller breaks the blocks over the next ticks (breakBlock), then runs after.
    public record Result(@Nullable Explosion explosion, List<BlockPos> blocks, @Nullable Runnable after) {
    }

    // Detonates a missile at center: hurts entities now and returns the rest. Null if a mod cancelled the
    // explosion (e.g. a claim mod).
    public static @Nullable Result detonate(ServerLevel level, MissileType type, int tier, Vec3 center, @Nullable Entity missile, @Nullable Entity shooter) {
        DamageSource source = level.damageSources().source(ModDamageTypes.MISSILE, missile, shooter);
        return switch (type) {
            case EXPLOSIVE -> blast(level, source, center, MissileConfig.explosivePower(tier), false, missile);
            case BREACHING -> blast(level, source, center, MissileConfig.breachingPower(tier), true, missile);
            case INCENDIARY -> incendiary(level, source, center, tier, missile);
            case EMP -> emp(level, center, tier);
        };
    }

    private static @Nullable Result blast(ServerLevel level, DamageSource source, Vec3 center, float power, boolean breaching, @Nullable Entity missile) {
        // Our own blast, but the events and blocks get a vanilla explosion object to look at.
        ServerExplosion explosion = new ServerExplosion(level, missile, source, null, center, power, false, Explosion.BlockInteraction.DESTROY);
        if (EventHooks.onExplosionStart(level, explosion)) {
            return null;
        }
        List<BlockPos> blocks = affectedBlocks(level, explosion, center, power, breaching);
        float reach = power / AIR_LOSS;
        List<Entity> entities = level.getEntities(missile, AABB.ofSize(center, reach * 4.0, reach * 4.0, reach * 4.0));
        EventHooks.onExplosionDetonate(level, explosion, entities, blocks);

        Map<Player, Vec3> knockback = hurtEntities(level, explosion, source, center, reach, entities);
        effects(level, center, power, blocks.size(), knockback);
        return new Result(explosion, blocks, null);
    }

    // A small blast, entities in the fire radius catch fire now, and fire spreads over the area once the
    // blast's blocks are gone.
    private static @Nullable Result incendiary(ServerLevel level, DamageSource source, Vec3 center, int tier, @Nullable Entity missile) {
        Result blast = blast(level, source, center, MissileConfig.incendiaryPower(tier), false, missile);
        if (blast == null) {
            return null;
        }
        int radius = MissileConfig.incendiaryFireRadius(tier);
        for (Entity entity : level.getEntities(missile, AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0))) {
            if (entity.distanceToSqr(center) <= radius * radius && !entity.fireImmune()) {
                entity.igniteForSeconds(BURN_SECONDS);
            }
        }
        return new Result(blast.explosion(), blast.blocks(), () -> spreadFire(level, source, center, radius, missile));
    }

    // Fire on about a third of the spots in the radius where fire can stay. The spots go through the explosion
    // event as well, so claim mods can keep fire off protected land.
    private static void spreadFire(ServerLevel level, DamageSource source, Vec3 center, int radius, @Nullable Entity missile) {
        List<BlockPos> spots = new ArrayList<>();
        BlockPos middle = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-radius, -radius, -radius), middle.offset(radius, radius, radius))) {
            if (pos.distSqr(middle) <= radius * radius && level.getRandom().nextInt(FIRE_CHANCE) == 0 && level.getBlockState(pos).isAir()
                    && BaseFireBlock.getState(level, pos).canSurvive(level, pos)) {
                spots.add(pos.immutable());
            }
        }
        ServerExplosion fire = new ServerExplosion(level, missile, source, null, center, radius, true, Explosion.BlockInteraction.DESTROY);
        EventHooks.onExplosionDetonate(level, fire, new ArrayList<>(), spots);
        for (BlockPos pos : spots) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
            }
        }
    }

    // No block or entity damage: every block in the radius loses all its FE, and missile blocks there are
    // disabled for a while (EmpTarget).
    private static Result emp(ServerLevel level, Vec3 center, int tier) {
        int radius = MissileConfig.empRadius(tier);
        long disabledUntil = level.getGameTime() + MissileConfig.empDisableTicks(tier);
        BlockPos middle = BlockPos.containing(center);
        for (int chunkX = SectionPos.blockToSectionCoord(middle.getX() - radius); chunkX <= SectionPos.blockToSectionCoord(middle.getX() + radius); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(middle.getZ() - radius); chunkZ <= SectionPos.blockToSectionCoord(middle.getZ() + radius); chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : List.copyOf(chunk.getBlockEntities().values())) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.distSqr(middle) > radius * radius) {
                        continue;
                    }
                    if (blockEntity instanceof EmpTarget target) {
                        target.empHit(disabledUntil);
                    }
                    drainEnergy(level, pos);
                }
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, center.x, center.y + 1.0, center.z, EMP_PARTICLES, radius * 0.4, radius * 0.25,
                radius * 0.4, 0.3);
        level.sendParticles(ParticleTypes.END_ROD, true, true, center.x, center.y + 1.0, center.z, EMP_PARTICLES / 4, radius * 0.3, radius * 0.2,
                radius * 0.3, 0.1);
        level.playSound(null, center.x, center.y, center.z, ModSounds.MISSILE_EMP.get(), SoundSource.BLOCKS, SOUND_VOLUME, 1.0F);
        return new Result(null, List.of(), null);
    }

    // Takes all FE out of a block, from every side its energy handler is reachable from.
    private static void drainEnergy(ServerLevel level, BlockPos pos) {
        for (int i = -1; i < Direction.values().length; i++) {
            Direction side = i < 0 ? null : Direction.values()[i];
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (handler != null && handler.getAmountAsLong() > 0) {
                try (Transaction transaction = Transaction.openRoot()) {
                    handler.extract(handler.getAmountAsInt(), transaction);
                    transaction.commit();
                }
            }
        }
    }

    // Blocks the blast breaks. Breaching missiles treat hard blocks like stone.
    static List<BlockPos> affectedBlocks(ServerLevel level, Explosion explosion, Vec3 center, float power, boolean breaching) {
        Set<BlockPos> result = new HashSet<>();
        double hard = MissileConfig.hardBlockResistance();
        for (int x = 0; x < GRID; x++) {
            for (int y = 0; y < GRID; y++) {
                for (int z = 0; z < GRID; z++) {
                    if (x != 0 && x != GRID - 1 && y != 0 && y != GRID - 1 && z != 0 && z != GRID - 1) {
                        continue;
                    }
                    // One ray towards each point on the surface of a cube around the center.
                    Vec3 direction = new Vec3(x / (GRID - 1.0) * 2.0 - 1.0, y / (GRID - 1.0) * 2.0 - 1.0, z / (GRID - 1.0) * 2.0 - 1.0)
                            .normalize().scale(STEP);
                    float strength = power * (MIN_RAY + level.getRandom().nextFloat() * RAY_SPREAD);
                    Vec3 at = center;
                    while (strength > 0.0F) {
                        BlockPos pos = BlockPos.containing(at);
                        if (!level.isInWorldBounds(pos)) {
                            break;
                        }
                        BlockState state = level.getBlockState(pos);
                        FluidState fluid = level.getFluidState(pos);
                        if (!state.isAir() || !fluid.isEmpty()) {
                            if (state.is(ModTags.MISSILE_IMMUNE)) {
                                break;
                            }
                            float resistance = Math.max(state.getExplosionResistance(level, pos, explosion), fluid.getExplosionResistance(level, pos, explosion));
                            if (resistance >= hard) {
                                if (!breaching) {
                                    break;
                                }
                                resistance = BREACHED_RESISTANCE;
                            }
                            strength -= STEP * resistance * RESISTANCE_FACTOR;
                            if (strength > 0.0F && !state.isAir()) {
                                result.add(pos);
                            }
                        }
                        at = at.add(direction);
                        strength -= STEP * AIR_LOSS;
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    // Damage and knockback with vanilla's formulas, over twice the blast's reach.
    private static Map<Player, Vec3> hurtEntities(ServerLevel level, ServerExplosion explosion, DamageSource source, Vec3 center, float reach,
            List<Entity> entities) {
        Map<Player, Vec3> playerKnockback = new HashMap<>();
        double diameter = reach * 2.0;
        for (Entity entity : entities) {
            if (entity.ignoreExplosion(explosion)) {
                continue;
            }
            double distance = Math.sqrt(entity.distanceToSqr(center)) / diameter;
            if (distance > 1.0) {
                continue;
            }
            double impact = (1.0 - distance) * ServerExplosion.getSeenPercent(center, entity);
            entity.hurtServer(level, source, (float) ((impact * impact + impact) / 2.0 * 7.0 * diameter + 1.0));
            double resistance = entity instanceof LivingEntity living ? living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) : 0.0;
            Vec3 push = entity.getEyePosition().subtract(center).normalize().scale(impact * (1.0 - resistance));
            entity.pushFromExplosion(push);
            if (entity instanceof Player player && !player.isSpectator() && !(player.isCreative() && player.getAbilities().flying)) {
                playerKnockback.put(player, push);
            }
            entity.onExplosionHit(explosion.getDirectSourceEntity());
        }
        return playerKnockback;
    }

    private static void effects(ServerLevel level, Vec3 center, float power, int blockCount, Map<Player, Vec3> knockback) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) < EFFECT_DISTANCE * EFFECT_DISTANCE) {
                // Vanilla's explosion packet: particles, the close-up bang, and the player's knockback.
                player.connection.send(new ClientboundExplodePacket(center, power, blockCount, Optional.ofNullable(knockback.get(player)),
                        ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE, WeightedList.of(), true));
            }
        }
        // The far rumble, heard much further away than vanilla explosions.
        level.playSound(null, center.x, center.y, center.z, ModSounds.MISSILE_EXPLODE.get(), SoundSource.BLOCKS, SOUND_VOLUME, 1.0F);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, true, center.x, center.y + 1.0, center.z, 30, power * 0.3, 0.5, power * 0.3, 0.02);
    }

    // Breaks one block of a blast. Shulker boxes drop as an item with their contents; other containers
    // spill their contents when their block entity is removed. Nothing else drops (D52).
    static void breakBlock(ServerLevel level, @Nullable Explosion explosion, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (explosion == null || state.isAir() || state.is(ModTags.MISSILE_IMMUNE)) {
            return;
        }
        if (state.is(BlockTags.SHULKER_BOXES)) {
            Block.dropResources(state, level, pos, level.getBlockEntity(pos));
        }
        state.onBlockExploded(level, pos, explosion);
    }

    private MissileBlast() {
    }
}
