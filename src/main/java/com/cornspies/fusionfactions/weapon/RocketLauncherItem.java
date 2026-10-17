package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.registry.ModItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// The rocket launcher (D61, D65, DESIGN.md §6b.6): a weapon like the others (trigger, aiming, reload, workbench), but
// it loads rockets instead of the shared ammo and fires one guided rocket, homing on the player it has locked on to
// (LockOn) or flying straight without a lock.
public class RocketLauncherItem extends WeaponItem {
    public RocketLauncherItem(WeaponStats baseStats, Item.Properties properties) {
        super(baseStats, properties);
    }

    @Override
    public Item ammoItem() {
        return ModItems.ROCKET.get();
    }

    @Override
    protected void shoot(ServerLevel level, Player player, ItemStack stack, WeaponStats stats) {
        level.addFreshEntity(new GuidedRocketEntity(level, player, stats, LockOn.lockedTarget(player)));
    }
}
