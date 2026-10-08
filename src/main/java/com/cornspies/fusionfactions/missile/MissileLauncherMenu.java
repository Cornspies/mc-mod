package com.cornspies.fusionfactions.missile;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Missile launcher menu (DESIGN.md §6.4): missile and designator slots, and the launch button. The server
// checks every launch again (MissileLauncherBlockEntity.status), whatever the screen showed.
public class MissileLauncherMenu extends AbstractContainerMenu {
    public static final int LAUNCH_BUTTON = 0;
    public static final int MISSILE_X = 26;
    public static final int MISSILE_Y = 35;
    public static final int DESIGNATOR_X = 26;
    public static final int DESIGNATOR_Y = 57;
    public static final int INVENTORY_Y = 94;

    private static final int INVENTORY_START = 2;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int INVENTORY_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final BlockPos pos;

    // Client side: the position comes with the open-menu packet.
    public MissileLauncherMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(MissileLauncherBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL,
                buffer.readBlockPos());
    }

    public MissileLauncherMenu(int containerId, Inventory inventory, Container container, ContainerData data, ContainerLevelAccess access, BlockPos pos) {
        super(ModMenuTypes.MISSILE_LAUNCHER.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, MissileLauncherBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.access = access;
        this.pos = pos;
        this.addSlot(new FilteredSlot(container, MissileLauncherBlockEntity.MISSILE_SLOT, MISSILE_X, MISSILE_Y, "missile"));
        this.addSlot(new FilteredSlot(container, MissileLauncherBlockEntity.DESIGNATOR_SLOT, DESIGNATOR_X, DESIGNATOR_Y, "designator"));
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        this.addDataSlots(data);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public ItemStack missile() {
        return this.container.getItem(MissileLauncherBlockEntity.MISSILE_SLOT);
    }

    public @Nullable GlobalPos target() {
        return TargetDesignatorItem.target(this.container.getItem(MissileLauncherBlockEntity.DESIGNATOR_SLOT));
    }

    public LaunchStatus status() {
        return LaunchStatus.fromSynced(this.data.get(0), this.data.get(1));
    }

    // Stored energy, from 0 to 1.
    public float energy() {
        return this.data.get(2) / 1000.0F;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != LAUNCH_BUTTON || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        this.access.execute((level, blockPos) -> {
            if (level instanceof ServerLevel serverLevel && level.getBlockEntity(blockPos) instanceof MissileLauncherBlockEntity launcher) {
                launcher.launch(serverLevel, serverPlayer);
            }
        });
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < INVENTORY_START) {
            if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof MissileItem) {
            if (!this.moveItemStackTo(stack, MissileLauncherBlockEntity.MISSILE_SLOT, MissileLauncherBlockEntity.MISSILE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof TargetDesignatorItem) {
            if (!this.moveItemStackTo(stack, MissileLauncherBlockEntity.DESIGNATOR_SLOT, MissileLauncherBlockEntity.DESIGNATOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < HOTBAR_START) {
            if (!this.moveItemStackTo(stack, HOTBAR_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
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
        return stillValid(this.access, player, ModBlocks.MISSILE_LAUNCHER.get());
    }

    // A slot that only takes what the launcher accepts there, with an empty-slot icon.
    private static class FilteredSlot extends Slot {
        private final Identifier icon;

        FilteredSlot(Container container, int slot, int x, int y, String icon) {
            super(container, slot, x, y);
            this.icon = FusionFactions.id("container/missile_launcher/" + icon);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return this.getContainerSlot() == MissileLauncherBlockEntity.DESIGNATOR_SLOT ? 1 : super.getMaxStackSize(stack);
        }

        @Override
        public Identifier getNoItemIcon() {
            return this.icon;
        }
    }
}
