package com.cornspies.fusionfactions.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.config.PowerConfig;
import com.cornspies.fusionfactions.missile.MissileLauncherBlockEntity;
import com.cornspies.fusionfactions.power.CableBlock;
import com.cornspies.fusionfactions.power.ChargingStationBlockEntity;
import com.cornspies.fusionfactions.power.CombustionGeneratorBlockEntity;
import com.cornspies.fusionfactions.power.MachineBlock;
import com.cornspies.fusionfactions.power.SolarPanelBlockEntity;
import com.cornspies.fusionfactions.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// Power (D67, DESIGN.md §6b.3): generators, the solar panel and cable networks. Registered by WeaponGameTests.
public final class PowerGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, FusionFactions.MODID);

    static final List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> TESTS = List.of(
            TEST_FUNCTIONS.register("generator_burns_fuel", () -> PowerGameTests::generatorBurnsFuel),
            TEST_FUNCTIONS.register("cables_share_energy", () -> PowerGameTests::cablesShareEnergy),
            TEST_FUNCTIONS.register("solar_panel_needs_daylight", () -> PowerGameTests::solarPanelNeedsDaylight));

    // A generator with coal lights up, burns it and pushes its FE into the charging station next to it.
    private static void generatorBurnsFuel(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(2, 1, 2);
        BlockPos stationPos = generatorPos.east();
        helper.setBlock(generatorPos, ModBlocks.COMBUSTION_GENERATOR.get());
        helper.setBlock(stationPos, ModBlocks.CHARGING_STATION.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos, CombustionGeneratorBlockEntity.class);
        ChargingStationBlockEntity station = helper.getBlockEntity(stationPos, ChargingStationBlockEntity.class);
        generator.setItem(CombustionGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 2));
        helper.startSequence()
                .thenExecuteAfter(21, () -> {
                    helper.assertValueEqual(generator.getItem(CombustionGeneratorBlockEntity.FUEL_SLOT).getCount(), 1, "coal left after lighting one");
                    helper.assertTrue(helper.getBlockState(generatorPos).getValue(MachineBlock.LIT), "the generator should be lit");
                    long made = generator.energy().getAmountAsLong() + station.energy().getAmountAsLong();
                    helper.assertTrue(made >= 20L * PowerConfig.generatorOutput(), "about 20 ticks of FE should have been made, not " + made);
                    helper.assertTrue(station.energy().getAmountAsLong() > 0, "the station next to it should have been powered");
                })
                .thenSucceed();
    }

    // FE pushed into a cable goes evenly to every block on the network but the one it came from, up to the network's
    // throughput per tick. Cables connect to cables and to blocks that take FE.
    private static void cablesShareEnergy(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), ModBlocks.FE_CABLE.get());
        }
        BlockPos westPos = new BlockPos(1, 1, 3);
        BlockPos eastPos = new BlockPos(7, 1, 3);
        BlockPos stationPos = new BlockPos(4, 2, 3);
        helper.setBlock(westPos, ModBlocks.MISSILE_LAUNCHER.get());
        helper.setBlock(eastPos, ModBlocks.MISSILE_LAUNCHER.get());
        helper.setBlock(stationPos, ModBlocks.CHARGING_STATION.get());
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 3)).getValue(CableBlock.WEST), "the cable should connect to the launcher");
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 3)).getValue(CableBlock.EAST), "the cable should connect to the next cable");
        helper.assertFalse(helper.getBlockState(new BlockPos(2, 1, 3)).getValue(CableBlock.NORTH), "the cable should not connect to air");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 3)).getValue(CableBlock.UP), "the cable should connect to the station");

        EnergyHandler cable = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(new BlockPos(4, 1, 3)), Direction.NORTH);
        helper.assertTrue(cable != null, "a cable should take FE");
        int throughput = PowerConfig.cableThroughput();
        helper.assertValueEqual(insert(cable, throughput * 2), throughput, "FE carried in one tick");
        helper.assertValueEqual(insert(cable, 100), 0, "FE carried after the network is used up this tick");
        long west = helper.getBlockEntity(westPos, MissileLauncherBlockEntity.class).energy().getAmountAsLong();
        long east = helper.getBlockEntity(eastPos, MissileLauncherBlockEntity.class).energy().getAmountAsLong();
        long station = helper.getBlockEntity(stationPos, ChargingStationBlockEntity.class).energy().getAmountAsLong();
        helper.assertValueEqual(west + east + station, (long) throughput, "FE that arrived");
        for (long share : new long[] { west, east, station }) {
            helper.assertTrue(Math.abs(share - throughput / 3) <= 1, "each block should get a third, not " + share);
        }
        // From the station's side, the FE goes only to the launchers.
        EnergyHandler fromStation = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(new BlockPos(4, 1, 3)), Direction.UP);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    helper.assertValueEqual(insert(fromStation, 1000), 1000, "FE carried the next tick");
                    helper.assertValueEqual(helper.getBlockEntity(stationPos, ChargingStationBlockEntity.class).energy().getAmountAsLong(), station,
                            "FE sent back to the block it came from");
                })
                .thenSucceed();
    }

    private static int insert(EnergyHandler handler, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(amount, transaction);
            transaction.commit();
            return inserted;
        }
    }

    // A solar panel makes FE by day under open sky, and nothing under a roof or at night.
    private static void solarPanelNeedsDaylight(GameTestHelper helper) {
        BlockPos panelPos = new BlockPos(2, 1, 2);
        helper.setBlock(panelPos, ModBlocks.SOLAR_PANEL.get());
        BlockPos absolute = helper.absolutePos(panelPos);
        time(helper, "noon");
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(helper.getLevel().canSeeSky(absolute.above()), "the test needs open sky above the panel");
                    helper.assertTrue(SolarPanelBlockEntity.output(helper.getLevel(), absolute) > 0, "the panel should work at noon");
                    helper.setBlock(panelPos.above(3), Blocks.STONE);
                })
                // Sky light, which decides whether a block sees the sky, updates a moment later.
                .thenExecuteAfter(4, () -> {
                    helper.assertValueEqual(SolarPanelBlockEntity.output(helper.getLevel(), absolute), 0, "FE per tick under a roof");
                    helper.setBlock(panelPos.above(3), Blocks.AIR);
                    time(helper, "midnight");
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertValueEqual(SolarPanelBlockEntity.output(helper.getLevel(), absolute), 0, "FE per tick at midnight");
                    time(helper, "noon");
                })
                .thenSucceed();
    }

    // Sets the time of day with the time command (26.3 keeps time in world clocks).
    private static void time(GameTestHelper helper, String time) {
        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set " + time);
    }

    private PowerGameTests() {
    }
}
