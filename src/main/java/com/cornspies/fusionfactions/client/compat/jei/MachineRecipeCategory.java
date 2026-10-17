package com.cornspies.fusionfactions.client.compat.jei;

import java.util.List;

import com.cornspies.fusionfactions.industry.Machine;
import com.cornspies.fusionfactions.industry.MachineRecipe;
import com.cornspies.fusionfactions.power.MachineMenu;
import com.cornspies.fusionfactions.registry.ModBlocks;
import com.cornspies.fusionfactions.registry.ModItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

// One JEI category per machine, laid out like the machine's screen (slot positions from Machine): the inputs, the
// arrow and the output, with the recipe's power and time below. The inputs fill the first input slots in order, as
// JEI's transfer button puts them into the machine.
public class MachineRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MachineRecipe>> {
    private static final int MIN_WIDTH = 120;
    private static final int TEXT_HEIGHT = 10;
    private static final int TEXT_COLOR = 0xFF404040;

    private final Machine machine;
    // Offset from screen positions to positions in the category.
    private final int offsetX;
    private final int offsetY;
    private final int layoutHeight;

    public static MachineRecipeCategory create(Machine machine, IRecipeHolderType<MachineRecipe> type, IGuiHelper guiHelper) {
        List<MachineMenu.SlotSpec> slots = machine.slots();
        int left = slots.stream().mapToInt(MachineMenu.SlotSpec::x).min().orElse(0) - 1;
        int right = slots.stream().mapToInt(MachineMenu.SlotSpec::x).max().orElse(0) + 17;
        int top = Math.min(machine.arrowY(), slots.stream().mapToInt(MachineMenu.SlotSpec::y).min().orElse(0)) - 1;
        int bottom = slots.stream().mapToInt(MachineMenu.SlotSpec::y).max().orElse(0) + 17;
        int width = Math.max(MIN_WIDTH, right - left);
        int offsetX = (width - (right - left)) / 2 - left;
        return new MachineRecipeCategory(machine, type, guiHelper, width, offsetX, -top, bottom - top);
    }

    private MachineRecipeCategory(Machine machine, IRecipeHolderType<MachineRecipe> type, IGuiHelper guiHelper, int width, int offsetX, int offsetY,
            int layoutHeight) {
        super(type, ModBlocks.machine(machine).getName(), guiHelper.createDrawableItemLike(ModItems.machine(machine)), width, layoutHeight + TEXT_HEIGHT + 2);
        this.machine = machine;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.layoutHeight = layoutHeight;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        List<MachineMenu.SlotSpec> slots = this.machine.slots();
        for (int i = 0; i < this.machine.inputCount(); i++) {
            MachineMenu.SlotSpec slot = slots.get(i);
            IRecipeSlotBuilder input = builder.addInputSlot(slot.x() + this.offsetX, slot.y() + this.offsetY).setStandardSlotBackground();
            if (i < recipe.inputs().size()) {
                input.addItemStacks(stacks(recipe.inputs().get(i)));
            }
        }
        MachineMenu.SlotSpec output = slots.getLast();
        builder.addOutputSlot(output.x() + this.offsetX, output.y() + this.offsetY).setStandardSlotBackground().add(recipe.result());
        if (this.machine.inputCount() > 1) {
            builder.setShapeless(this.machine.arrowX() + this.offsetX + 6, this.machine.arrowY() + this.offsetY - 11);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        builder.addAnimatedRecipeArrowWidget(recipe.ticks())
                .setPosition(this.machine.arrowX() + this.offsetX - 1, this.machine.arrowY() + this.offsetY - 1);
        Component cost = Component.translatable("fusionfactions.jei.machine_cost", recipe.energyPerTick(), seconds(recipe.ticks()),
                String.format("%,d", (long) recipe.energyPerTick() * recipe.ticks()));
        builder.addText(cost, this.getWidth(), TEXT_HEIGHT)
                .setPosition(0, this.layoutHeight + 2)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setColor(TEXT_COLOR)
                .setShadow(false);
    }

    // Ingredient.items() is deprecated in favour of SlotDisplay, which has no count; recipe viewers still need the
    // items to show "2 niter".
    @SuppressWarnings("deprecation")
    private static List<ItemStack> stacks(SizedIngredient input) {
        return input.ingredient().items().map(item -> new ItemStack(item, input.count())).toList();
    }

    // 100 ticks -> "5", 150 -> "7.5".
    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format("%.1f", ticks / 20.0F);
    }
}
