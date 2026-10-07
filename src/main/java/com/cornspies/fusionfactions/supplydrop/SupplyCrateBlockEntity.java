package com.cornspies.fusionfactions.supplydrop;

import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A 27-slot container. Its loot table is rolled when the first player opens it.
// Removes itself once emptied, or when its despawn time is reached.
public class SupplyCrateBlockEntity extends RandomizableContainerBlockEntity {
    private static final int SIZE = 27;
    private static final int MARKER_INTERVAL_TICKS = 10;
    private static final int CHECK_INTERVAL_TICKS = 20;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    // Game time at which the crate removes itself. Stays at MAX_VALUE until a drop sets it.
    private long despawnAt = Long.MAX_VALUE;

    public SupplyCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SUPPLY_CRATE.get(), pos, state);
    }

    public void fill(CrateRarity rarity, long lootSeed, long despawnAt) {
        this.setLootTable(rarity.lootTable(), lootSeed);
        this.despawnAt = despawnAt;
        this.setChanged();
    }

    // The loot table is cleared when it's rolled, which happens when the first player opens the crate.
    public boolean isOpened() {
        return this.lootTable == null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SupplyCrateBlockEntity crate) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long time = level.getGameTime();
        if (!crate.isOpened() && time % MARKER_INTERVAL_TICKS == 0) {
            // Tall smoke column, visible from far away, until the crate is opened.
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, true,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0.15, 0.0, 0.15, 0.01);
        }
        if (time % CHECK_INTERVAL_TICKS == 0 && ((crate.isOpened() && crate.isEmpty()) || time >= crate.despawnAt)) {
            crate.vanish(serverLevel);
        }
    }

    // Removes the crate without dropping anything.
    public void vanish(ServerLevel level) {
        BlockPos pos = this.getBlockPos();
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.6F, 0.8F);
        level.removeBlock(pos, false);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Crates never spill their contents. Players empty them, or the contents vanish on despawn.
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && this.level != null && !user.getLivingEntity().isSpectator()) {
            this.level.playSound(null, this.getBlockPos(), SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }
        output.putLong("DespawnAt", this.despawnAt);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.items);
        }
        this.despawnAt = input.getLongOr("DespawnAt", Long.MAX_VALUE);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionfactions.supply_crate", this.getBlockState().getValue(SupplyCrateBlock.RARITY).displayName());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }
}
