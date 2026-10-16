package com.cornspies.fusionfactions.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

// What every machine has: its slots (which hoppers can reach, see WorldlyContainer), an FE buffer that other
// blocks and cables reach from any side, and a server tick.
public abstract class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    protected NonNullList<ItemStack> items;
    protected final SimpleEnergyHandler energy;
    private final int[] allSlots;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, int capacity, int maxInsert, int maxExtract) {
        super(type, pos, state);
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.allSlots = new int[slots];
        for (int i = 0; i < slots; i++) {
            this.allSlots[i] = i;
        }
        this.energy = new SimpleEnergyHandler(capacity, maxInsert, maxExtract) {
            @Override
            protected void onEnergyChanged(int previousAmount) {
                MachineBlockEntity.this.setChanged();
            }
        };
    }

    public EnergyHandler energy() {
        return this.energy;
    }

    // Stored energy in per mille, for the menu's gauge.
    protected int energyPerMille() {
        return (int) (1000L * this.energy.getAmountAsLong() / Math.max(1, this.energy.getCapacityAsLong()));
    }

    public abstract void serverTick(ServerLevel level);

    // Lights the machine's front while it works.
    protected void setLit(boolean lit) {
        BlockState state = this.getBlockState();
        if (this.level != null && state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT) != lit) {
            this.level.setBlock(this.worldPosition, state.setValue(MachineBlock.LIT, lit), 3);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.allSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.energy.serialize(output.child("Energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        input.child("Energy").ifPresent(this.energy::deserialize);
    }

    @Override
    public int getContainerSize() {
        return this.allSlots.length;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }
}
