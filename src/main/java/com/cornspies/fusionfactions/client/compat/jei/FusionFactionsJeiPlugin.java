package com.cornspies.fusionfactions.client.compat.jei;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cornspies.fusionfactions.FusionFactions;
import com.cornspies.fusionfactions.client.ClientMachineRecipes;
import com.cornspies.fusionfactions.client.ProcessingMachineScreen;
import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.MachineRecipe;
import com.cornspies.fusionfactions.power.MachineMenu;
import com.cornspies.fusionfactions.registry.ModItems;
import com.cornspies.fusionfactions.registry.ModMenuTypes;
import com.cornspies.fusionfactions.registry.ModRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;

// JEI support, loaded by JEI only when it is installed (it finds the @JeiPlugin annotation); nothing else references
// these classes. Shows the machine recipes in one category per machine, the machines as their catalysts, a "show
// recipes" area on each machine's progress arrow, and a transfer button that moves the inputs into the machine.
@JeiPlugin
public class FusionFactionsJeiPlugin implements IModPlugin {
    private static final Identifier UID = FusionFactions.id("jei_plugin");
    private static final int PLAYER_INVENTORY_SLOTS = 36;

    private final Map<Machine, IRecipeHolderType<MachineRecipe>> types = new EnumMap<>(Machine.class);

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    private IRecipeHolderType<MachineRecipe> type(Machine machine) {
        return this.types.computeIfAbsent(machine, m -> IRecipeHolderType.create(ModRecipes.type(m)));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (Machine machine : Machine.values()) {
            registration.addRecipeCategories(MachineRecipeCategory.create(machine, this.type(machine), registration.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Machine machine : Machine.values()) {
            registration.addRecipes(this.type(machine), ClientMachineRecipes.of(machine));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Machine machine : Machine.values()) {
            registration.addCraftingStation(this.type(machine), ModItems.machine(machine));
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // All four machines share one screen class, so the area depends on the screen's machine.
        registration.addGuiContainerHandler(ProcessingMachineScreen.class, new IGuiContainerHandler<ProcessingMachineScreen>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(ProcessingMachineScreen screen, double mouseX, double mouseY) {
                Machine machine = screen.machine();
                return List.of(IGuiClickableArea.createBasic(machine.arrowX(), machine.arrowY(), ProcessingMachineScreen.ARROW_WIDTH,
                        ProcessingMachineScreen.ARROW_HEIGHT, FusionFactionsJeiPlugin.this.type(machine)));
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Menu slots: the machine's inputs, its output, then the player's inventory.
        for (Machine machine : Machine.values()) {
            int inputs = machine.inputCount();
            registration.addRecipeTransferHandler(MachineMenu.class, ModMenuTypes.machine(machine), this.type(machine), 0, inputs, inputs + 1,
                    PLAYER_INVENTORY_SLOTS);
        }
    }
}
