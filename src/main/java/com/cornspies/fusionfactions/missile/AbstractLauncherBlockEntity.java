package com.cornspies.fusionfactions.missile;

import java.time.Duration;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.MissileConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

// What the missile launcher and the silo controller share (D55): a missile slot and a designator slot, an FE
// buffer, and the checks before a launch. Anyone can use them (D58). Every launch is checked here, on the server
// (§3.2): see status(). Subclasses say which tiers they fire, where the missile starts, and their cooldown.
public abstract class AbstractLauncherBlockEntity extends BaseContainerBlockEntity implements EmpTarget {
    public static final int MISSILE_SLOT = 0;
    public static final int DESIGNATOR_SLOT = 1;
    private static final int SIZE = 2;
    // Holds two of the most expensive launches. Mods' cables can push up to this much FE per tick.
    private static final int ENERGY_CAPACITY = 400_000;
    private static final int ENERGY_MAX_INSERT = 10_000;
    // Data slots for the menu: status check, its value, stored energy in per mille.
    public static final int DATA_COUNT = 3;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            AbstractLauncherBlockEntity.this.setChanged();
        }
    };
    // Game time from which the launcher can fire again, and until which an EMP keeps it shut down.
    private long cooldownUntil;
    private long disabledUntil;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> AbstractLauncherBlockEntity.this.cachedStatus().check().ordinal();
                case 1 -> AbstractLauncherBlockEntity.this.cachedStatus().syncedValue();
                default -> (int) (1000L * AbstractLauncherBlockEntity.this.energy.getAmountAsLong() / ENERGY_CAPACITY);
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

    // Where a launch starts, and the block above which the sky must be open. launchPos is null while the
    // launcher can't fire at all (an incomplete silo), with the number of missing blocks.
    protected record LaunchSite(@Nullable BlockPos launchPos, BlockPos skyPos, int missingBlocks) {
    }

    protected AbstractLauncherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // The highest missile tier this launcher fires.
    protected abstract int maxTier();

    protected abstract int cooldownTicks();

    protected abstract LaunchSite site(ServerLevel level);

    // Called after a missile left, e.g. to open the silo hatch.
    protected void onLaunch(ServerLevel level, LaunchSite site) {
    }

    // The position the screen measures the distance to the target from.
    public BlockPos menuPos() {
        if (this.level instanceof ServerLevel level) {
            BlockPos launchPos = this.site(level).launchPos();
            if (launchPos != null) {
                return launchPos;
            }
        }
        return this.worldPosition;
    }

    // The menu asks for the status every tick; compute it once per tick at most.
    private LaunchStatus cachedStatus = LaunchStatus.READY;
    private long cachedStatusTime = Long.MIN_VALUE;

    private LaunchStatus cachedStatus() {
        if (this.level instanceof ServerLevel level && level.getGameTime() != this.cachedStatusTime) {
            this.cachedStatus = this.status(level);
            this.cachedStatusTime = level.getGameTime();
        }
        return this.cachedStatus;
    }

    public EnergyHandler energy() {
        return this.energy;
    }

    // Everything the server checks before a launch, in the order a player would fix it.
    public LaunchStatus status(ServerLevel level) {
        ItemStack missile = this.items.get(MISSILE_SLOT);
        if (!(missile.getItem() instanceof MissileItem missileItem)) {
            return LaunchStatus.of(LaunchStatus.Check.NO_MISSILE);
        }
        if (missileItem.tier() > this.maxTier()) {
            return LaunchStatus.of(LaunchStatus.Check.NEEDS_SILO);
        }
        LaunchSite site = this.site(level);
        if (site.launchPos() == null) {
            return new LaunchStatus(LaunchStatus.Check.SILO_INCOMPLETE, site.missingBlocks());
        }
        GlobalPos target = TargetDesignatorItem.target(this.items.get(DESIGNATOR_SLOT));
        if (target == null) {
            return LaunchStatus.of(LaunchStatus.Check.NO_TARGET);
        }
        if (!target.dimension().equals(level.dimension())) {
            return LaunchStatus.of(LaunchStatus.Check.OTHER_DIMENSION);
        }
        int distance = (int) MissileFlight.horizontalDistance(site.launchPos(), target.pos());
        if (distance < MissileConfig.minRange()) {
            return new LaunchStatus(LaunchStatus.Check.TOO_CLOSE, MissileConfig.minRange());
        }
        int range = MissileConfig.range(missileItem.tier());
        if (distance > range) {
            return new LaunchStatus(LaunchStatus.Check.TOO_FAR, range);
        }
        if (!level.canSeeSky(site.skyPos())) {
            return LaunchStatus.of(LaunchStatus.Check.SKY_BLOCKED);
        }
        long disabled = this.disabledUntil - level.getGameTime();
        if (disabled > 0) {
            return new LaunchStatus(LaunchStatus.Check.DISABLED, (int) ((disabled + 19) / 20));
        }
        long cooldown = this.cooldownUntil - level.getGameTime();
        if (cooldown > 0) {
            return new LaunchStatus(LaunchStatus.Check.COOLDOWN, (int) ((cooldown + 19) / 20));
        }
        Optional<Duration> untilOpen = RaidWindows.untilOpen(RaidWindows.configured(), RaidWindows.now());
        if (untilOpen.isPresent()) {
            return new LaunchStatus(LaunchStatus.Check.RAID_WINDOW_CLOSED, (int) Math.max(1, (untilOpen.get().toSeconds() + 59) / 60));
        }
        if (MissileConfig.requirePower() && this.energy.getAmountAsLong() < MissileConfig.launchEnergy(missileItem.tier())) {
            return new LaunchStatus(LaunchStatus.Check.NO_POWER, MissileConfig.launchEnergy(missileItem.tier()));
        }
        return LaunchStatus.READY;
    }

    // The launch button. Returns the reason if it can't fire.
    public LaunchStatus launch(ServerLevel level, @Nullable ServerPlayer player) {
        LaunchStatus status = this.status(level);
        if (!status.isReady()) {
            return status;
        }
        ItemStack missile = this.items.get(MISSILE_SLOT);
        MissileItem missileItem = (MissileItem) missile.getItem();
        GlobalPos target = TargetDesignatorItem.target(this.items.get(DESIGNATOR_SLOT));
        if (MissileConfig.requirePower()) {
            this.energy.set(this.energy.getAmountAsInt() - MissileConfig.launchEnergy(missileItem.tier()));
        }
        missile.shrink(1);
        this.cooldownUntil = level.getGameTime() + this.cooldownTicks();
        this.setChanged();
        LaunchSite site = this.site(level);
        MissileManager.launch(level, missileItem.type(), missileItem.tier(), site.launchPos(), target.pos(), player);
        this.onLaunch(level, site);
        return status;
    }

    // An EMP drains the buffer and shuts the launcher down for a while.
    @Override
    public void empHit(long disabledUntil) {
        this.energy.set(0);
        this.disabledUntil = Math.max(this.disabledUntil, disabledUntil);
        this.setChanged();
    }

    public ContainerData data() {
        return this.data;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == MISSILE_SLOT ? stack.getItem() instanceof MissileItem : stack.getItem() instanceof TargetDesignatorItem;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.energy.serialize(output.child("Energy"));
        output.putLong("CooldownUntil", this.cooldownUntil);
        output.putLong("DisabledUntil", this.disabledUntil);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        input.child("Energy").ifPresent(this.energy::deserialize);
        this.cooldownUntil = input.getLongOr("CooldownUntil", 0L);
        this.disabledUntil = input.getLongOr("DisabledUntil", 0L);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MissileLauncherMenu(containerId, inventory, this, this.data, ContainerLevelAccess.create(this.level, this.worldPosition),
                this.menuPos());
    }
}
