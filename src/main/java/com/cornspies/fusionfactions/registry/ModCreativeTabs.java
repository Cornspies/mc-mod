package com.cornspies.fusionfactions.registry;

import com.cornspies.fusionfactions.FusionFactions;
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
            })
            .build());

    private ModCreativeTabs() {
    }
}
