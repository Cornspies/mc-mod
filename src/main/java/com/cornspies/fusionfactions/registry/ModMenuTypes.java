package com.cornspies.fusionfactions.registry;

import java.util.EnumMap;
import java.util.Map;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.ProcessingMachineBlockEntity;
import com.cornspies.fusionfactions.missile.InterceptorBlockEntity;
import com.cornspies.fusionfactions.missile.MissileLauncherMenu;
import com.cornspies.fusionfactions.missile.RadarMenu;
import com.cornspies.fusionfactions.power.ChargingStationBlockEntity;
import com.cornspies.fusionfactions.power.CombustionGeneratorBlockEntity;
import com.cornspies.fusionfactions.power.MachineMenu;
import com.cornspies.fusionfactions.workbench.WeaponWorkbenchMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, FusionFactions.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<WeaponWorkbenchMenu>> WEAPON_WORKBENCH = MENU_TYPES.register("weapon_workbench",
            () -> new MenuType<>(WeaponWorkbenchMenu::new, FeatureFlags.VANILLA_SET));

    // Opened with the launcher's position as extra data.
    public static final DeferredHolder<MenuType<?>, MenuType<MissileLauncherMenu>> MISSILE_LAUNCHER = MENU_TYPES.register("missile_launcher",
            () -> IMenuTypeExtension.create(MissileLauncherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RadarMenu>> RADAR = MENU_TYPES.register("radar",
            () -> IMenuTypeExtension.create(RadarMenu::new));

    // Machines (D67, D70): generic machine menus, one type per machine.
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> COMBUSTION_GENERATOR = MENU_TYPES.register("combustion_generator",
            () -> new MenuType<>(CombustionGeneratorBlockEntity::clientMenu, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> CHARGING_STATION = MENU_TYPES.register("charging_station",
            () -> new MenuType<>(ChargingStationBlockEntity::clientMenu, FeatureFlags.VANILLA_SET));

    // The interceptor's rocket slot (D66) uses the machine menu too.
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> INTERCEPTOR = MENU_TYPES.register("interceptor",
            () -> new MenuType<>(InterceptorBlockEntity::clientMenu, FeatureFlags.VANILLA_SET));

    private static final Map<Machine, DeferredHolder<MenuType<?>, MenuType<MachineMenu>>> MACHINES = new EnumMap<>(Machine.class);

    static {
        for (Machine machine : Machine.values()) {
            MACHINES.put(machine, MENU_TYPES.register(machine.id(),
                    () -> new MenuType<>((id, inventory) -> ProcessingMachineBlockEntity.clientMenu(machine, id, inventory), FeatureFlags.VANILLA_SET)));
        }
    }

    public static MenuType<MachineMenu> machine(Machine machine) {
        return MACHINES.get(machine).get();
    }

    private ModMenuTypes() {
    }
}
