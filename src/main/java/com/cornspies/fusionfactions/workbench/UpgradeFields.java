package com.cornspies.fusionfactions.workbench;

import com.cornspies.fusionfactions.registry.ModDataComponents;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.weapon.WeaponItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeItem;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// The workbench's upgrade fields as a container, one slot per UpgradeType (by ordinal). It holds nothing
// itself: each field shows the part installed on the weapon in the weapon slot, and putting a part in or
// taking it out changes the weapon right away (D35). The weapon is the only place the parts live, so
// nothing can be duplicated or left behind in the workbench.
final class UpgradeFields implements Container {
    private final Container weaponSlot;

    UpgradeFields(Container weaponSlot) {
        this.weaponSlot = weaponSlot;
    }

    private ItemStack weapon() {
        return this.weaponSlot.getItem(0);
    }

    @Override
    public int getContainerSize() {
        return UpgradeType.values().length;
    }

    @Override
    public boolean isEmpty() {
        return WeaponUpgrades.of(this.weapon()).isEmpty();
    }

    // A fresh part stack each time; changing it has no effect (use setItem / removeItem).
    @Override
    public ItemStack getItem(int slot) {
        ItemStack weapon = this.weapon();
        int tier = weapon.isEmpty() ? 0 : WeaponUpgrades.of(weapon).tier(UpgradeType.values()[slot]);
        return tier == 0 ? ItemStack.EMPTY : new ItemStack(ModItems.upgrade(UpgradeType.values()[slot], tier).get());
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack part = this.getItem(slot);
        if (count > 0 && !part.isEmpty()) {
            this.install(UpgradeType.values()[slot], 0);
            return part;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.removeItem(slot, 1);
    }

    // The slots only ever hand over single parts (max stack size 1).
    @Override
    public void setItem(int slot, ItemStack stack) {
        UpgradeType type = UpgradeType.values()[slot];
        if (stack.isEmpty()) {
            this.install(type, 0);
        } else if (stack.getItem() instanceof UpgradeItem part && part.type() == type) {
            this.install(type, part.tier());
        }
    }

    private void install(UpgradeType type, int tier) {
        ItemStack weapon = this.weapon();
        if (!(weapon.getItem() instanceof WeaponItem item)) {
            return;
        }
        WeaponUpgrades upgrades = WeaponUpgrades.of(weapon).with(type, tier);
        if (upgrades.isEmpty()) {
            weapon.remove(ModDataComponents.UPGRADES.get());
        } else {
            weapon.set(ModDataComponents.UPGRADES.get(), upgrades);
        }
        // Taking out a bigger magazine drops the rounds that no longer fit.
        int magazine = item.stats(weapon).magazineSize();
        if (WeaponItem.getAmmo(weapon) > magazine) {
            weapon.set(ModDataComponents.AMMO.get(), magazine);
        }
        this.weaponSlot.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        this.weaponSlot.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // The parts belong to the weapon, so there is nothing to clear here.
    @Override
    public void clearContent() {
    }
}
