package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.registry.ModAttachments;
import com.cornspies.fusionfactions.registry.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The trigger is the attack button (D37). The client only reports pressing and releasing it
// (TriggerPayload); the server fires from here, one check per player tick, with everything validated
// in WeaponItem. The state lives on the player entity, so it's gone after death or logout.
public final class Trigger {
    // Per player, not saved.
    public static final class State {
        boolean held;
        // The trigger was pulled since the last tick (semi-automatic weapons fire once per pull).
        boolean newPress;
        // Ticks since the trigger was pulled (the gatling spins up first).
        int heldTicks;
    }

    public static void press(Player player) {
        State state = player.getData(ModAttachments.TRIGGER);
        if (state.held) {
            return;
        }
        state.held = true;
        state.newPress = true;
        state.heldTicks = 0;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof WeaponItem weapon && weapon.baseStats().spinUpTicks() > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GATLING_GUN_SPIN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    public static void release(Player player) {
        State state = player.getExistingDataOrNull(ModAttachments.TRIGGER);
        if (state != null) {
            state.held = false;
            state.newPress = false;
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        tick(event.getEntity());
    }

    // Public for game tests, whose mock players don't tick.
    public static void tick(Player player) {
        State state = player.getExistingDataOrNull(ModAttachments.TRIGGER);
        if (state == null || !state.held || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof WeaponItem weapon && player.isAlive() && !player.isSpectator()) {
            weapon.triggerTick(level, player, stack, state.newPress, state.heldTicks);
        }
        state.newPress = false;
        state.heldTicks++;
    }

    private Trigger() {
    }
}
