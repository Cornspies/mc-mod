package com.cornspies.fusionfactions.workbench;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModMenuTypes;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeRules;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;

import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Weapon workbench menu (DESIGN.md §4.5): the weapon in the middle of a 3x3 grid, an upgrade field for each
// type around it. Fields the weapon doesn't accept are locked. All moves go through the server-side menu,
// which checks every part against the weapon (§3.2).
public class WeaponWorkbenchMenu extends AbstractContainerMenu {
    // Layout, shared with the screen. Slot positions are the item's top-left corner inside the 18x18 frame.
    public static final int GRID_X = 58;
    public static final int GRID_Y = 18;
    public static final int GRID_STEP = 22;
    public static final int INVENTORY_Y = 94;

    private static final int WEAPON_SLOT = 0;
    private static final int FIELDS_START = 1;
    private static final int INVENTORY_START = FIELDS_START + UpgradeType.values().length;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int INVENTORY_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final SimpleContainer weaponContainer = new SimpleContainer(1);

    // Client side.
    public WeaponWorkbenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public WeaponWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenuTypes.WEAPON_WORKBENCH.get(), containerId);
        this.access = access;
        this.addSlot(new Slot(this.weaponContainer, 0, fieldX(4), fieldY(4)) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof WeaponItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public Identifier getNoItemIcon() {
                return FusionFactions.id("container/weapon_workbench/weapon");
            }
        });
        UpgradeFields fields = new UpgradeFields(this.weaponContainer);
        for (UpgradeType type : UpgradeType.values()) {
            this.addSlot(new FieldSlot(fields, type));
        }
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
    }

    public static int fieldX(int field) {
        return GRID_X + field % 3 * GRID_STEP;
    }

    public static int fieldY(int field) {
        return GRID_Y + field / 3 * GRID_STEP;
    }

    public ItemStack weapon() {
        return this.weaponContainer.getItem(0);
    }

    // Whether a part of this type can go on the weapon now in the workbench.
    public boolean isOpen(UpgradeType type) {
        ItemStack weapon = this.weapon();
        return weapon.getItem() instanceof WeaponItem && UpgradeRules.accepts(weapon.getItem(), type);
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
        } else if (stack.getItem() instanceof WeaponItem) {
            if (!this.moveItemStackTo(stack, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof UpgradeItem part) {
            int field = FIELDS_START + part.type().ordinal();
            if (!this.moveItemStackTo(stack, field, field + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < HOTBAR_START) {
            if (!this.moveItemStackTo(stack, HOTBAR_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }

        // For a field, stack is a copy: emptying the slot is what takes the part off the weapon.
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
        return stillValid(this.access, player, ModBlocks.WEAPON_WORKBENCH.get());
    }

    // The workbench keeps nothing: the weapon (with its parts) goes back to the player.
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.weaponContainer));
    }

    // One upgrade field. Only open while a weapon that accepts this type is in the workbench; a part
    // that is installed but no longer accepted (config change) can still be taken out.
    public class FieldSlot extends Slot {
        private final UpgradeType type;

        FieldSlot(UpgradeFields fields, UpgradeType type) {
            super(fields, type.ordinal(), fieldX(type.field()), fieldY(type.field()));
            this.type = type;
        }

        public UpgradeType type() {
            return this.type;
        }

        public boolean isOpen() {
            return WeaponWorkbenchMenu.this.isOpen(this.type);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return UpgradeItem.isPart(stack, this.type) && this.isOpen();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return this.isOpen() ? FusionFactions.id("container/weapon_workbench/" + this.type.getSerializedName()) : null;
        }
    }
}
