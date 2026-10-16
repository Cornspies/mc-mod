package com.cornspies.fusionfactions.industry;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.power.MachineBlockEntity;
import com.cornspies.fusionfactions.power.MachineMenu;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModMenuTypes;
import com.cornspies.fusionfactions.registry.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A processing machine (D70, DESIGN.md §6b.4): finds the recipe its input slots make, works on it while it has the
// FE it costs (if power is required) and room in its output slot, then takes the inputs and adds the result. Changing
// the inputs to another recipe starts over. Hoppers fill the inputs from the top and sides and empty the output from
// below; each input slot holds one kind of item, so a hopper can't fill every slot with the same thing.
public class ProcessingMachineBlockEntity extends MachineBlockEntity {
    public enum Status {
        IDLE,
        WORKING,
        NO_POWER,
        OUTPUT_FULL
    }

    private static final int CAPACITY = 50_000;
    private static final int MAX_INSERT = 2_000;
    // Data slots: progress, ticks the recipe takes, energy per mille, status, FE per tick of the recipe.
    public static final int DATA_COUNT = 5;

    private final Machine machine;
    private final RecipeManager.CachedCheck<MachineRecipeInput, MachineRecipe> check;
    private @Nullable ResourceKey<Recipe<?>> current;
    private int progress;
    private int total;
    private int energyPerTick;
    private Status status = Status.IDLE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            ProcessingMachineBlockEntity machine = ProcessingMachineBlockEntity.this;
            return switch (index) {
                case 0 -> Math.min(machine.progress, Short.MAX_VALUE);
                case 1 -> Math.min(machine.total, Short.MAX_VALUE);
                case 2 -> machine.energyPerMille();
                case 3 -> machine.status.ordinal();
                default -> Math.min(machine.energyPerTick, Short.MAX_VALUE);
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

    public ProcessingMachineBlockEntity(Machine machine, BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.machine(machine), pos, state, machine.inputCount() + 1, CAPACITY, MAX_INSERT, 0);
        this.machine = machine;
        this.check = RecipeManager.createCheck(ModRecipes.type(machine));
    }

    public Machine machine() {
        return this.machine;
    }

    private int outputSlot() {
        return this.machine.inputCount();
    }

    private MachineRecipeInput input() {
        return new MachineRecipeInput(this.items.subList(0, this.machine.inputCount()));
    }

    @Override
    public void serverTick(ServerLevel level) {
        MachineRecipeInput input = this.input();
        Optional<RecipeHolder<MachineRecipe>> holder = input.isEmpty() ? Optional.empty() : this.check.getRecipeFor(input, level);
        if (holder.isEmpty()) {
            this.stop(Status.IDLE);
            this.current = null;
            this.progress = 0;
            return;
        }
        MachineRecipe recipe = holder.get().value();
        if (!holder.get().id().equals(this.current)) {
            this.current = holder.get().id();
            this.progress = 0;
        }
        this.total = recipe.ticks();
        this.energyPerTick = recipe.energyPerTick();
        ItemStack result = recipe.assemble(input);
        if (!this.fits(result)) {
            this.stop(Status.OUTPUT_FULL);
            return;
        }
        if (MissileConfig.requirePower()) {
            if (this.energy.getAmountAsLong() < recipe.energyPerTick()) {
                this.stop(Status.NO_POWER);
                return;
            }
            this.energy.set(this.energy.getAmountAsInt() - recipe.energyPerTick());
        }
        this.status = Status.WORKING;
        this.setLit(true);
        if (++this.progress >= recipe.ticks()) {
            this.craft(recipe, input, result);
            this.progress = 0;
        }
        this.setChanged();
    }

    private void stop(Status status) {
        this.status = status;
        this.setLit(false);
    }

    private boolean fits(ItemStack result) {
        ItemStack output = this.items.get(this.outputSlot());
        return output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(MachineRecipe recipe, MachineRecipeInput input, ItemStack result) {
        int[] claimed = recipe.claim(input);
        if (claimed == null) {
            return;
        }
        for (int i = 0; i < recipe.inputs().size(); i++) {
            int needed = recipe.inputs().get(i).count();
            for (int slot = 0; slot < claimed.length && needed > 0; slot++) {
                if (claimed[slot] == i) {
                    ItemStack stack = this.items.get(slot);
                    int taken = Math.min(needed, stack.getCount());
                    stack.shrink(taken);
                    needed -= taken;
                    if (stack.isEmpty()) {
                        this.items.set(slot, ItemStack.EMPTY);
                    }
                }
            }
        }
        ItemStack output = this.items.get(this.outputSlot());
        if (output.isEmpty()) {
            this.items.set(this.outputSlot(), result);
        } else {
            output.grow(result.getCount());
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= this.machine.inputCount()) {
            return false;
        }
        for (int other = 0; other < this.machine.inputCount(); other++) {
            if (other != slot && ItemStack.isSameItemSameComponents(this.items.get(other), stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[] { this.outputSlot() };
        }
        int[] inputs = new int[this.machine.inputCount()];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = i;
        }
        return inputs;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == this.outputSlot();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + FusionFactions.MODID + "." + this.machine.id());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return menu(this.machine, containerId, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    public static MachineMenu clientMenu(Machine machine, int containerId, Inventory inventory) {
        return menu(machine, containerId, inventory, new SimpleContainer(machine.inputCount() + 1), new SimpleContainerData(DATA_COUNT),
                ContainerLevelAccess.NULL);
    }

    private static MachineMenu menu(Machine machine, int containerId, Inventory inventory, Container container, ContainerData data,
            ContainerLevelAccess access) {
        return new MachineMenu(ModMenuTypes.machine(machine), containerId, inventory, container, data, access, ModBlocks.machine(machine), machine.slots());
    }

    public static Status status(MachineMenu menu) {
        Status[] values = Status.values();
        return values[Math.clamp(menu.data(3), 0, values.length - 1)];
    }
}
