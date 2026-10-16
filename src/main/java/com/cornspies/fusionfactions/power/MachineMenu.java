package com.cornspies.fusionfactions.power;

import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

// A machine's menu: its slots at the given positions, the player's inventory below, and the machine's state as data
// slots. Shift clicking moves an item into the first machine slot that takes it. Output slots take nothing.
public class MachineMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 84;

    // A slot's position in the screen and whether players can put items into it.
    public record SlotSpec(int x, int y, boolean input) {
    }

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block block;
    private final int machineSlots;

    public MachineMenu(MenuType<?> type, int containerId, Inventory inventory, Container container, ContainerData data, ContainerLevelAccess access,
            Block block, List<SlotSpec> slots) {
        super(type, containerId);
        checkContainerSize(container, slots.size());
        this.container = container;
        this.data = data;
        this.access = access;
        this.block = block;
        this.machineSlots = slots.size();
        for (int i = 0; i < slots.size(); i++) {
            SlotSpec spec = slots.get(i);
            this.addSlot(new Slot(container, i, spec.x(), spec.y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return spec.input() && this.container.canPlaceItem(this.getContainerSlot(), stack);
                }
            });
        }
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        this.addDataSlots(data);
    }

    public int data(int index) {
        return this.data.get(index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int inventoryEnd = this.slots.size();
        if (slotIndex < this.machineSlots) {
            if (!this.moveItemStackTo(stack, this.machineSlots, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < this.machineSlots && !moved; i++) {
                if (this.slots.get(i).mayPlace(stack)) {
                    moved = this.moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                int hotbarStart = inventoryEnd - 9;
                boolean inHotbar = slotIndex >= hotbarStart;
                if (!this.moveItemStackTo(stack, inHotbar ? this.machineSlots : hotbarStart, inHotbar ? hotbarStart : inventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, this.block);
    }
}
