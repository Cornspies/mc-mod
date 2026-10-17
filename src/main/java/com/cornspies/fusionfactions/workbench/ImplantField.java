package com.cornspies.fusionfactions.workbench;

import com.cornspies.fusionfactions.gear.TacticalArmor;
import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// The workbench's implant field (D63), a live view of the helmet in the middle slot like UpgradeFields: it shows the
// threat-warning implant if the helmet has one, and putting one in or taking it out changes the helmet right away.
final class ImplantField implements Container {
    private final Container middleSlot;

    ImplantField(Container middleSlot) {
        this.middleSlot = middleSlot;
    }

    private ItemStack helmet() {
        ItemStack stack = this.middleSlot.getItem(0);
        return stack.is(ModItems.TACTICAL_HELMET.get()) ? stack : ItemStack.EMPTY;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return !TacticalArmor.hasImplant(this.helmet());
    }

    // A fresh stack each time; changing it has no effect (use setItem / removeItem).
    @Override
    public ItemStack getItem(int slot) {
        return TacticalArmor.hasImplant(this.helmet()) ? new ItemStack(ModItems.THREAT_WARNING_IMPLANT.get()) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack implant = this.getItem(slot);
        if (count > 0 && !implant.isEmpty()) {
            this.install(false);
            return implant;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            this.install(false);
        } else if (stack.is(ModItems.THREAT_WARNING_IMPLANT.get())) {
            this.install(true);
        }
    }

    private void install(boolean implant) {
        ItemStack helmet = this.helmet();
        if (helmet.isEmpty()) {
            return;
        }
        if (implant) {
            helmet.set(ModDataComponents.IMPLANT.get(), Unit.INSTANCE);
        } else {
            helmet.remove(ModDataComponents.IMPLANT.get());
        }
        this.middleSlot.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        this.middleSlot.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // The implant belongs to the helmet, so there is nothing to clear here.
    @Override
    public void clearContent() {
    }
}
