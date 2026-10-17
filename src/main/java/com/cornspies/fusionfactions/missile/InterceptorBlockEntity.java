package com.cornspies.fusionfactions.missile;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.power.MachineBlockEntity;
import com.cornspies.fusionfactions.power.MachineMenu;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModMenuTypes;
import com.cornspies.fusionfactions.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

// Interceptor (D53, D66, DESIGN.md §6.9, §6b.8): fires a homing rocket at the closest missile coming down within its
// range that it can see, one shot per cooldown, as long as the missile still needs one. Every shot uses one
// interceptor rocket from its slot (hoppers can fill it) and FE, whatever requirePower says. Works for anyone (D58);
// right click opens its menu.
public class InterceptorBlockEntity extends MachineBlockEntity implements EmpTarget {
    public static final int SLOT = 0;
    public static final int MAX_ROCKETS = 16;
    public static final int CAPACITY = 100_000;
    private static final int MAX_INSERT = 10_000;
    private static final int SEARCH_INTERVAL_TICKS = 2;
    public static final List<MachineMenu.SlotSpec> SLOTS = List.of(new MachineMenu.SlotSpec(89, 35, true));
    // Data slots: energy per mille, rockets in the slot, status, seconds left of an EMP shutdown.
    public static final int DATA_COUNT = 4;

    public enum Status {
        READY,
        RELOADING,
        NO_POWER,
        NO_ROCKETS,
        DISABLED
    }

    private long cooldownUntil;
    private long disabledUntil;
    private long now;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            InterceptorBlockEntity interceptor = InterceptorBlockEntity.this;
            return switch (index) {
                case 0 -> interceptor.energyPerMille();
                case 1 -> interceptor.items.get(SLOT).getCount();
                case 2 -> interceptor.status().ordinal();
                default -> (int) Math.max(0L, (interceptor.disabledUntil - interceptor.now + 19) / 20);
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

    public InterceptorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.INTERCEPTOR.get(), pos, state, 1, CAPACITY, MAX_INSERT, 0);
    }

    @Override
    public void serverTick(ServerLevel level) {
        this.now = level.getGameTime();
        if (this.now % SEARCH_INTERVAL_TICKS != 0 || this.status() != Status.READY) {
            return;
        }
        Vec3 muzzle = Vec3.atBottomCenterOf(this.worldPosition.above());
        MissileEntity target = this.findTarget(level, muzzle);
        if (target == null) {
            return;
        }
        level.addFreshEntity(new InterceptorRocketEntity(level, muzzle, target, MissileConfig.interceptorDamage()));
        level.playSound(null, this.worldPosition, ModSounds.INTERCEPTOR_LAUNCH.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
        this.items.get(SLOT).shrink(1);
        this.energy.set(this.energy.getAmountAsInt() - MissileConfig.interceptorEnergy());
        this.cooldownUntil = this.now + MissileConfig.interceptorCooldownTicks();
        this.setChanged();
    }

    // What keeps it from firing, most important first; READY if nothing does.
    public Status status() {
        if (this.now < this.disabledUntil) {
            return Status.DISABLED;
        }
        if (this.energy.getAmountAsLong() < MissileConfig.interceptorEnergy()) {
            return Status.NO_POWER;
        }
        if (this.items.get(SLOT).isEmpty()) {
            return Status.NO_ROCKETS;
        }
        if (this.now < this.cooldownUntil) {
            return Status.RELOADING;
        }
        return Status.READY;
    }

    public static Status status(MachineMenu menu) {
        Status[] values = Status.values();
        return values[Math.floorMod(menu.data(2), values.length)];
    }

    // The closest missile on its way down in range and in sight that still needs fire.
    private @Nullable MissileEntity findTarget(ServerLevel level, Vec3 muzzle) {
        double range = MissileConfig.interceptorRange();
        MissileEntity best = null;
        double bestDistance = range * range;
        for (MissileEntity missile : level.getEntitiesOfClass(MissileEntity.class, AABB.ofSize(muzzle, range * 2, range * 2, range * 2),
                MissileEntity::needsInterception)) {
            Vec3 aim = missile.position().add(0.0, missile.getBbHeight() / 2.0, 0.0);
            double distance = aim.distanceToSqr(muzzle);
            if (distance < bestDistance && level.clip(new ClipContext(muzzle, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty())).getType() == HitResult.Type.MISS) {
                best = missile;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    public void empHit(long disabledUntil) {
        this.energy.set(0);
        this.disabledUntil = Math.max(this.disabledUntil, disabledUntil);
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.INTERCEPTOR_ROCKET.get());
    }

    @Override
    public int getMaxStackSize() {
        return MAX_ROCKETS;
    }

    // Hoppers fill it but can't empty it.
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + FusionFactions.MODID + ".interceptor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return menu(containerId, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    public static MachineMenu clientMenu(int containerId, Inventory inventory) {
        return menu(containerId, inventory, new SimpleContainer(SLOTS.size()), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    private static MachineMenu menu(int containerId, Inventory inventory, Container container, ContainerData data, ContainerLevelAccess access) {
        return new MachineMenu(ModMenuTypes.INTERCEPTOR.get(), containerId, inventory, container, data, access, ModBlocks.INTERCEPTOR.get(), SLOTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("CooldownUntil", this.cooldownUntil);
        output.putLong("DisabledUntil", this.disabledUntil);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.cooldownUntil = input.getLongOr("CooldownUntil", 0L);
        this.disabledUntil = input.getLongOr("DisabledUntil", 0L);
    }
}
