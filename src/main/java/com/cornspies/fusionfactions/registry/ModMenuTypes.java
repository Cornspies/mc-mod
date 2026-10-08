package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.missile.MissileLauncherMenu;
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

    private ModMenuTypes() {
    }
}
