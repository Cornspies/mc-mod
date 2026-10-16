package com.cornspies.fusionfactions.power;

import java.util.List;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.PowerConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

// Combustion generator (D67, DESIGN.md §6b.3): burns anything a furnace burns, for as long as a furnace would, and
// makes PowerConfig.generatorOutput() FE per tick meanwhile. It pauses while its buffer is full, so no fuel is wasted,
// and pushes its FE into the blocks around it.
public class CombustionGeneratorBlockEntity extends MachineBlockEntity {
    public static final int FUEL_SLOT = 0;
    private static final int CAPACITY = 100_000;
    private static final int MAX_PUSH = 2_000;
    public static final List<MachineMenu.SlotSpec> SLOTS = List.of(new MachineMenu.SlotSpec(80, 53, true));
    // Data slots: ticks of burning left, total ticks of the current fuel, energy per mille, FE per tick right now.
    public static final int DATA_COUNT = 4;

    private int burnTime;
    private int burnDuration;
    private int output;
    private final NeighborEnergy neighbors = new NeighborEnergy();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            CombustionGeneratorBlockEntity generator = CombustionGeneratorBlockEntity.this;
            return switch (index) {
                case 0 -> Math.min(generator.burnTime, Short.MAX_VALUE);
                case 1 -> Math.min(generator.burnDuration, Short.MAX_VALUE);
                case 2 -> generator.energyPerMille();
                default -> generator.output;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.COMBUSTION_GENERATOR.get(), pos, state, 1, CAPACITY, 0, MAX_PUSH);
    }

    @Override
    public void serverTick(ServerLevel level) {
        int perTick = PowerConfig.generatorOutput();
        boolean room = this.energy.getAmountAsLong() + perTick <= this.energy.getCapacityAsLong();
        if (this.burnTime <= 0 && room) {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            int duration = this.burnDuration(level, fuel);
            if (duration > 0) {
                this.burnTime = this.burnDuration = duration;
                ItemStackTemplate remainder = fuel.getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty() && remainder != null) {
                    this.items.set(FUEL_SLOT, remainder.create());
                }
                this.setChanged();
            }
        }
        this.output = 0;
        if (this.burnTime > 0 && room) {
            this.burnTime--;
            this.energy.set(this.energy.getAmountAsInt() + perTick);
            this.output = perTick;
        }
        this.neighbors.push(level, this.worldPosition, this.energy, MAX_PUSH);
        this.setLit(this.burnTime > 0);
    }

    private int burnDuration(ServerLevel level, ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        }
        return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, this.getLootContext(level, fuel), 0);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL);
    }

    // Hoppers only take out what isn't fuel, like the empty bucket a lava bucket leaves.
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !stack.has(DataComponents.COOKING_FUEL);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BurnTime", this.burnTime);
        output.putInt("BurnDuration", this.burnDuration);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.burnDuration = input.getIntOr("BurnDuration", 0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + FusionFactions.MODID + ".combustion_generator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return menu(containerId, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    public static MachineMenu clientMenu(int containerId, Inventory inventory) {
        return menu(containerId, inventory, new SimpleContainer(SLOTS.size()), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    private static MachineMenu menu(int containerId, Inventory inventory, Container container, ContainerData data,
            ContainerLevelAccess access) {
        return new MachineMenu(ModMenuTypes.COMBUSTION_GENERATOR.get(), containerId, inventory, container, data, access,
                ModBlocks.COMBUSTION_GENERATOR.get(), SLOTS);
    }

    // How far the flame is burnt down, from the synced data: 1 when fresh fuel was just lit.
    public static float burnProgress(MachineMenu menu) {
        int duration = menu.data(1);
        return duration <= 0 ? 0.0F : Mth.clamp(menu.data(0) / (float) duration, 0.0F, 1.0F);
    }
}
