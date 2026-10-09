package com.cornspies.fusionfactions.missile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.MissileConfig;
import com.cornspies.fusionfactions.registry.ModBlockEntityTypes;
import com.cornspies.fusionfactions.registry.ModSounds;
import com.cornspies.fusionfactions.util.Compass;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

// Radar (D54, DESIGN.md §6.9). Once a second it looks for missiles in its range: on the climb, in cruise (their
// point on the line from launch to target) and on the way down. On first contact with a missile it alerts the team
// of the player who placed it, wherever they are, with the launch area: a square that contains the launch spot.
// Its menu lists the contacts, and while it has any it gives a redstone signal. Anyone can use it (D58).
public class RadarBlockEntity extends BlockEntity implements MenuProvider, EmpTarget {
    public enum Status {
        ACTIVE,
        NO_POWER,
        DISABLED
    }

    // A missile in range: its offset from the radar, and the seconds until it comes down inside the range (0 if
    // it only passes by).
    public record Contact(int flightId, int dx, int dz, int impactSeconds) {
    }

    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int ENERGY_MAX_INSERT = 10_000;
    // Contacts the menu shows; the alert covers every one.
    public static final int SHOWN_CONTACTS = 5;
    // Data slots: status, its value, energy per mille, contact count, then dx, dz and impact seconds per contact.
    public static final int DATA_COUNT = 4 + SHOWN_CONTACTS * 3;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            RadarBlockEntity.this.setChanged();
        }
    };
    private @Nullable UUID owner;
    private @Nullable String ownerName;
    private long disabledUntil;
    private Status status = Status.ACTIVE;
    private List<Contact> contacts = List.of();
    // Missiles the owner's team has already been alerted about. Not saved: after a restart a radar alerts again.
    private final Set<Integer> alerted = new HashSet<>();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            RadarBlockEntity radar = RadarBlockEntity.this;
            return switch (index) {
                case 0 -> radar.status.ordinal();
                case 1 -> radar.level == null ? 0 : (int) Mth.clamp((radar.disabledUntil - radar.level.getGameTime() + 19) / 20, 0, Short.MAX_VALUE);
                case 2 -> (int) (1000L * radar.energy.getAmountAsLong() / ENERGY_CAPACITY);
                case 3 -> radar.contacts.size();
                default -> {
                    int contact = (index - 4) / 3;
                    if (contact >= radar.contacts.size()) {
                        yield 0;
                    }
                    Contact c = radar.contacts.get(contact);
                    yield switch ((index - 4) % 3) {
                        case 0 -> c.dx();
                        case 1 -> c.dz();
                        default -> Math.min(c.impactSeconds(), Short.MAX_VALUE);
                    };
                }
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

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.RADAR.get(), pos, state);
    }

    public EnergyHandler energy() {
        return this.energy;
    }

    public List<Contact> contacts() {
        return this.contacts;
    }

    // The player who placed the radar; their team gets its alerts.
    void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getScoreboardName();
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RadarBlockEntity radar) {
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % SCAN_INTERVAL_TICKS == 0) {
            radar.scan(serverLevel);
        }
    }

    private void scan(ServerLevel level) {
        if (level.getGameTime() < this.disabledUntil) {
            this.setStatus(level, Status.DISABLED, List.of());
            return;
        }
        if (MissileConfig.requirePower()) {
            int cost = MissileConfig.radarEnergyPerTick() * SCAN_INTERVAL_TICKS;
            if (this.energy.getAmountAsLong() < cost) {
                this.setStatus(level, Status.NO_POWER, List.of());
                return;
            }
            this.energy.set(this.energy.getAmountAsInt() - cost);
        }
        long now = level.getServer().overworld().getGameTime();
        int range = MissileConfig.radarRange();
        List<Contact> found = new ArrayList<>();
        for (MissileFlight flight : MissileData.get(level.getServer()).flights()) {
            if (!flight.dimension().equals(level.dimension())) {
                continue;
            }
            Vec3 position = flight.horizontalPosition(now);
            int dx = (int) Math.round(position.x - (this.worldPosition.getX() + 0.5));
            int dz = (int) Math.round(position.z - (this.worldPosition.getZ() + 0.5));
            if (dx * dx + dz * dz > range * range) {
                continue;
            }
            boolean incoming = MissileFlight.horizontalDistance(this.worldPosition, flight.target()) <= range;
            int seconds = incoming ? (int) Math.max(1, (flight.impactTime() - now + 19) / 20) : 0;
            Contact contact = new Contact(flight.id(), dx, dz, seconds);
            found.add(contact);
            if (this.alerted.add(flight.id())) {
                this.alert(level, flight, contact);
            }
        }
        this.alerted.removeIf(id -> found.stream().noneMatch(contact -> contact.flightId() == id));
        this.setStatus(level, Status.ACTIVE, found);
    }

    private void setStatus(ServerLevel level, Status status, List<Contact> contacts) {
        this.status = status;
        this.contacts = List.copyOf(contacts);
        BlockState state = this.getBlockState();
        boolean signal = !contacts.isEmpty();
        if (state.hasProperty(RadarBlock.POWERED) && state.getValue(RadarBlock.POWERED) != signal) {
            level.setBlockAndUpdate(this.worldPosition, state.setValue(RadarBlock.POWERED, signal));
        }
    }

    // Tells the owner's team (or the owner alone, without a team) about a new contact.
    private void alert(ServerLevel level, MissileFlight flight, Contact contact) {
        List<ServerPlayer> recipients = this.recipients(level.getServer());
        if (recipients.isEmpty()) {
            return;
        }
        int distance = (int) Math.round(Math.sqrt(contact.dx() * contact.dx() + contact.dz() * contact.dz()));
        Component direction = Component.translatable("fusionfactions.direction." + Compass.direction(contact.dx(), contact.dz()));
        Component course = contact.impactSeconds() > 0
                ? Component.translatable("fusionfactions.radar.incoming", contact.impactSeconds()).withStyle(ChatFormatting.RED)
                : Component.translatable("fusionfactions.radar.passing");
        int[] area = launchArea(flight);
        BlockPos pos = this.worldPosition;
        Component message = Component.translatable("fusionfactions.radar.prefix", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD)
                .append(" ")
                .append(Component.translatable("fusionfactions.radar.alert", distance, direction, course, area[0], area[1], area[2], area[3])
                        .withStyle(ChatFormatting.YELLOW));
        for (ServerPlayer player : recipients) {
            player.sendSystemMessage(message);
            if (player.connection != null) {
                player.connection.send(new ClientboundSoundPacket(ModSounds.RADAR_ALERT, SoundSource.BLOCKS, player.getX(), player.getY(), player.getZ(),
                        1.0F, 1.0F, player.getRandom().nextLong()));
            }
        }
    }

    private List<ServerPlayer> recipients(MinecraftServer server) {
        if (this.owner == null || this.ownerName == null) {
            return List.of();
        }
        PlayerTeam team = server.getScoreboard().getPlayersTeam(this.ownerName);
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (team != null ? team.getPlayers().contains(player.getScoreboardName()) : player.getUUID().equals(this.owner)) {
                result.add(player);
            }
        }
        return result;
    }

    // The launch area of a flight as min X, max X, min Z, max Z: a square of twice radarAreaSize that contains the
    // launch spot somewhere, not in its middle. The same for every radar, so two radars don't narrow it down.
    public static int[] launchArea(MissileFlight flight) {
        int size = MissileConfig.radarAreaSize();
        RandomSource random = RandomSource.create(flight.id() * 31L + flight.launchPos().asLong());
        int centerX = flight.launchPos().getX() + random.nextIntBetweenInclusive(-size, size);
        int centerZ = flight.launchPos().getZ() + random.nextIntBetweenInclusive(-size, size);
        return new int[] { centerX - size, centerX + size, centerZ - size, centerZ + size };
    }

    // An EMP drains the radar and blinds it for a while.
    @Override
    public void empHit(long disabledUntil) {
        this.energy.set(0);
        this.disabledUntil = Math.max(this.disabledUntil, disabledUntil);
        this.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + FusionFactions.MODID + ".radar");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RadarMenu(containerId, this.data, ContainerLevelAccess.create(this.level, this.worldPosition), this.worldPosition);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.storeNullable("Owner", UUIDUtil.CODEC, this.owner);
        if (this.ownerName != null) {
            output.putString("OwnerName", this.ownerName);
        }
        output.putLong("DisabledUntil", this.disabledUntil);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Energy").ifPresent(this.energy::deserialize);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getString("OwnerName").orElse(null);
        this.disabledUntil = input.getLongOr("DisabledUntil", 0L);
    }
}
