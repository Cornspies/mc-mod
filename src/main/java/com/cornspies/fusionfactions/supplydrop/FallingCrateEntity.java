package com.cornspies.fusionfactions.supplydrop;

import com.cornspies.fusionfactions.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The crate on its way down (DESIGN.md §5.2 step 5). Falls at a steady speed and trails smoke.
// The server decides when it lands; the manager then places the crate block.
public class FallingCrateEntity extends Entity {
    // Blocks per tick: slow, like a crate under a parachute.
    public static final double DESCENT_PER_TICK = 0.2;
    private static final int TRAIL_INTERVAL_TICKS = 2;

    private static final EntityDataAccessor<Integer> DATA_RARITY = SynchedEntityData.defineId(FallingCrateEntity.class, EntityDataSerializers.INT);

    private int dropId = -1;

    public FallingCrateEntity(EntityType<? extends FallingCrateEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.setNoGravity(true);
    }

    FallingCrateEntity(Level level, double x, double y, double z, CrateRarity rarity, int dropId) {
        this(ModEntityTypes.FALLING_CRATE.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.entityData.set(DATA_RARITY, rarity.ordinal());
        this.dropId = dropId;
    }

    public CrateRarity getRarity() {
        CrateRarity[] values = CrateRarity.values();
        return values[Mth.clamp(this.entityData.get(DATA_RARITY), 0, values.length - 1)];
    }

    int dropId() {
        return this.dropId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RARITY, 0);
    }

    @Override
    public void tick() {
        // Runs on both sides so clients see smooth motion between position updates.
        this.setDeltaMovement(0.0, -DESCENT_PER_TICK, 0.0);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.level() instanceof ServerLevel level) {
            if (!SupplyDropManager.isFalling(level.getServer(), this.dropId)) {
                // The drop was cleared, or this entity is a leftover.
                this.discard();
                return;
            }
            if (this.tickCount % TRAIL_INTERVAL_TICKS == 0) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, true, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.1, 0.0, 0.1, 0.005);
            }
            if (this.onGround() || this.verticalCollision) {
                SupplyDropManager.onCrateLanded(level, this);
            } else if (this.getY() < level.getMinY()) {
                // Fell out of the world; the manager places the crate after its timeout.
                this.discard();
            }
        }
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.SERVER_AND_CLIENT;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Visible from far away, like the announcements suggest.
        return distance < 256.0 * 256.0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("DropId", this.dropId);
        output.store("Rarity", CrateRarity.CODEC, this.getRarity());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.dropId = input.getIntOr("DropId", -1);
        input.read("Rarity", CrateRarity.CODEC).ifPresent(rarity -> this.entityData.set(DATA_RARITY, rarity.ordinal()));
    }
}
