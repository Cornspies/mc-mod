package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.missile.MissileType;
import com.cornspies.fusionfactions.weapon.upgrade.UpgradeType;
import com.cornspies.fusionfactions.weapon.upgrade.WeaponUpgrades;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FusionFactions.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fusionfactions"))
            .icon(() -> new ItemStack(ModItems.PISTOL.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.PISTOL.get());
                output.accept(ModItems.AUTO_PISTOL.get());
                output.accept(ModItems.SMG.get());
                output.accept(ModItems.SHOTGUN.get());
                output.accept(ModItems.SNIPER_RIFLE.get());
                output.accept(ModItems.GATLING_GUN.get());
                output.accept(ModItems.AMMO.get());
                output.accept(ModItems.BARREL.get());
                output.accept(ModItems.FUSION_CORE.get());
                output.accept(ModItems.GRIP.get());
                output.accept(ModItems.WEAPON_WORKBENCH.get());
                for (UpgradeType type : UpgradeType.values()) {
                    for (int tier = 1; tier <= WeaponUpgrades.MAX_TIER; tier++) {
                        output.accept(ModItems.upgrade(type, tier).get());
                    }
                }
                output.accept(ModItems.MISSILE_LAUNCHER.get());
                output.accept(ModItems.SILO_CONTROLLER.get());
                output.accept(ModItems.SILO_HATCH.get());
                output.accept(ModItems.SILO_CASING.get());
                output.accept(ModItems.RADAR.get());
                output.accept(ModItems.INTERCEPTOR.get());
                output.accept(ModItems.TARGET_DESIGNATOR.get());
                for (MissileType type : MissileType.values()) {
                    for (int tier = 1; tier <= MissileType.MAX_TIER; tier++) {
                        output.accept(ModItems.missile(type, tier).get());
                    }
                }
                output.accept(ModItems.COMBUSTION_GENERATOR.get());
                output.accept(ModItems.SOLAR_PANEL.get());
                output.accept(ModItems.FE_CABLE.get());
                output.accept(ModItems.CHARGING_STATION.get());
                for (Machine machine : Machine.values()) {
                    output.accept(ModItems.machine(machine));
                }
                ModItems.ORES.forEach(ore -> output.accept(ore.get()));
                ModItems.MATERIALS.forEach(material -> output.accept(material.get()));
            })
            .build());

    private ModCreativeTabs() {
    }
}
