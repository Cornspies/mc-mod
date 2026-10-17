package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;
import com.mojang.serialization.Codec;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Weapon state lives on the item stack (DESIGN.md §3.3). Changes to these don't replay the equip animation.
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FusionFactions.MODID);

    // Rounds in the magazine.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO = DATA_COMPONENTS.registerComponentType("ammo",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation());

    // Present while reloading: the game time at which the next round goes in.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELOAD = DATA_COMPONENTS.registerComponentType("reload",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).ignoreSwapAnimation());

    // Game time at which the weapon may fire again.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NEXT_SHOT = DATA_COMPONENTS.registerComponentType("next_shot",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).ignoreSwapAnimation());

    // Upgrade parts installed at the weapon workbench (DESIGN.md §4.5).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponUpgrades>> UPGRADES = DATA_COMPONENTS.registerComponentType("upgrades",
            builder -> builder.persistent(WeaponUpgrades.CODEC).networkSynchronized(WeaponUpgrades.STREAM_CODEC));

    // The block a target designator points missiles at (D49).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> TARGET = DATA_COMPONENTS.registerComponentType("target",
            builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    // FE stored in an item (the handheld radar, D62). Changes come once a second while it's held, so no swap animation.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = DATA_COMPONENTS.registerComponentType("energy",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation());

    // A threat-warning implant installed in a tactical helmet (D62, D63).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> IMPLANT = DATA_COMPONENTS.registerComponentType("implant",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    private ModDataComponents() {
    }
}
