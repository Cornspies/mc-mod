package com.cornspies.fusionfactions.missile;

import java.util.ArrayList;
import java.util.List;

import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

// Radar menu (DESIGN.md §6.9): no slots, just what the radar sees, synced as data slots.
public class RadarMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final BlockPos pos;

    // Client side: the position comes with the open-menu packet.
    public RadarMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, new SimpleContainerData(RadarBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL, buffer.readBlockPos());
    }

    public RadarMenu(int containerId, ContainerData data, ContainerLevelAccess access, BlockPos pos) {
        super(ModMenuTypes.RADAR.get(), containerId);
        checkContainerDataCount(data, RadarBlockEntity.DATA_COUNT);
        this.data = data;
        this.access = access;
        this.pos = pos;
        this.addDataSlots(data);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public RadarBlockEntity.Status status() {
        RadarBlockEntity.Status[] values = RadarBlockEntity.Status.values();
        return values[Mth.clamp(this.data.get(0), 0, values.length - 1)];
    }

    public int disabledSeconds() {
        return this.data.get(1);
    }

    public float energy() {
        return this.data.get(2) / 1000.0F;
    }

    public int contactCount() {
        return this.data.get(3);
    }

    // The contacts shown (at most SHOWN_CONTACTS), without their flight ids.
    public List<RadarBlockEntity.Contact> contacts() {
        List<RadarBlockEntity.Contact> contacts = new ArrayList<>();
        for (int i = 0; i < Math.min(this.contactCount(), RadarBlockEntity.SHOWN_CONTACTS); i++) {
            int base = 4 + i * 3;
            // Data slots are shorts: offsets keep their sign when read back.
            contacts.add(new RadarBlockEntity.Contact(0, (short) this.data.get(base), (short) this.data.get(base + 1), this.data.get(base + 2)));
        }
        return contacts;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.RADAR.get());
    }
}
