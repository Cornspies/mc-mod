package com.cornspies.fusionfactions.weapon;

import com.cornspies.fusionfactions.gear.HandheldRadarItem;
import com.cornspies.fusionfactions.gear.TacticalArmor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

// Lock warnings (D62, DESIGN.md §6b.7): only a player wearing a helmet with a threat-warning implant, or holding a
// charged handheld radar, learns that someone is locking on or that a guided rocket is coming. Everyone else only
// has the rocket's sound and smoke trail.
public final class LockWarning {
    public static boolean hasGear(Player player) {
        return TacticalArmor.hasImplant(player.getItemBySlot(EquipmentSlot.HEAD))
                || HandheldRadarItem.isActive(player.getMainHandItem())
                || HandheldRadarItem.isActive(player.getOffhandItem());
    }

    // Someone at from is locking on to the target (locked: has locked on).
    public static void locking(ServerPlayer target, Vec3 from, boolean locked) {
        send(target, locked ? ThreatPayload.Kind.LOCKED : ThreatPayload.Kind.SEEKING, from);
    }

    // A guided rocket at from is homing on the target.
    public static void incoming(ServerPlayer target, Vec3 from) {
        send(target, ThreatPayload.Kind.INCOMING, from);
    }

    private static void send(ServerPlayer target, ThreatPayload.Kind kind, Vec3 from) {
        if (hasGear(target)) {
            PacketDistributor.sendToPlayer(target, new ThreatPayload(kind, from));
        }
    }

    private LockWarning() {
    }
}
