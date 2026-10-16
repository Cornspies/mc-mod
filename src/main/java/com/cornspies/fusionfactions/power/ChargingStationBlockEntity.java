package com.cornspies.fusionfactions.power;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.PowerConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// Charging station (D67, DESIGN.md §6b.3): charges the FE item in its slot (the handheld radar, or any other mod's
// FE item) from its buffer, PowerConfig.chargingRate() FE per tick. Hoppers can take an item out once it is full.
public class ChargingStationBlockEntity extends MachineBlockEntity {
    public static final int SLOT = 0;
    private static final int CAPACITY = 50_000;
    private static final int MAX_INSERT = 2_000;
    public static final List<MachineMenu.SlotSpec> SLOTS = List.of(new MachineMenu.SlotSpec(89, 35, true));
    // Data slots: energy per mille, the item's charge per mille (-1 without a chargeable item), FE per tick right now.
    public static final int DATA_COUNT = 3;

    private int rate;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            ChargingStationBlockEntity station = ChargingStationBlockEntity.this;
            return switch (index) {
                case 0 -> station.energyPerMille();
                case 1 -> station.itemCharge();
                default -> station.rate;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CHARGING_STATION.get(), pos, state, 1, CAPACITY, MAX_INSERT, 0);
    }

    @Override
    public void serverTick(ServerLevel level) {
        this.rate = 0;
        ItemStack stack = this.items.get(SLOT);
        EnergyHandler item = itemEnergy(stack);
        int available = Math.min(PowerConfig.chargingRate(), this.energy.getAmountAsInt());
        if (item != null && available > 0) {
            int inserted;
            try (Transaction transaction = Transaction.openRoot()) {
                inserted = item.insert(available, transaction);
                transaction.commit();
            }
            if (inserted > 0) {
                this.energy.set(this.energy.getAmountAsInt() - inserted);
                this.rate = inserted;
                this.setChanged();
            }
        }
        this.setLit(this.rate > 0);
    }

    private static @Nullable EnergyHandler itemEnergy(ItemStack stack) {
        return stack.isEmpty() ? null : ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
    }

    private int itemCharge() {
        EnergyHandler item = itemEnergy(this.items.get(SLOT));
        if (item == null || item.getCapacityAsLong() <= 0) {
            return -1;
        }
        return (int) (1000L * item.getAmountAsLong() / item.getCapacityAsLong());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return itemEnergy(stack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        EnergyHandler item = itemEnergy(stack);
        return item == null || item.getAmountAsLong() >= item.getCapacityAsLong();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + FusionFactions.MODID + ".charging_station");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return menu(containerId, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    public static MachineMenu clientMenu(int containerId, Inventory inventory) {
        return menu(containerId, inventory, new SimpleContainer(SLOTS.size()), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    private static MachineMenu menu(int containerId, Inventory inventory, Container container, ContainerData data, ContainerLevelAccess access) {
        return new MachineMenu(ModMenuTypes.CHARGING_STATION.get(), containerId, inventory, container, data, access, ModBlocks.CHARGING_STATION.get(),
                SLOTS);
    }
}
