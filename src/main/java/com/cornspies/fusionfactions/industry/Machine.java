package com.cornspies.fusionfactions.industry;

import java.util.ArrayList;
import java.util.List;

import com.cornspies.fusionfactions.power.MachineMenu;

// The four processing machines (D70, DESIGN.md §6b.4): their recipe type's name, slot layout (positions in the
// screen, matching tools/power_textures.py) and the FE and time a recipe takes unless it says otherwise.
public enum Machine {
    CRUSHER("crusher", "crushing", List.of(slot(76, 35)), slot(126, 35), 100, 35, true, 20, 100),
    CHEMICAL_MIXER("chemical_mixer", "mixing", List.of(slot(76, 17), slot(76, 35), slot(76, 53)), slot(126, 35), 100, 35, true, 40, 200),
    CIRCUIT_FABRICATOR("circuit_fabricator", "fabricating", List.of(slot(72, 26), slot(90, 26), slot(72, 44), slot(90, 44)), slot(136, 35), 110, 35, true,
            60, 200),
    ASSEMBLER("assembler", "assembling", grid(30, 17), slot(120, 35), 92, 35, false, 40, 100);

    private final String id;
    private final String recipeName;
    private final List<MachineMenu.SlotSpec> inputs;
    private final MachineMenu.SlotSpec output;
    private final int arrowX;
    private final int arrowY;
    private final boolean readout;
    private final int defaultEnergy;
    private final int defaultTicks;

    Machine(String id, String recipeName, List<MachineMenu.SlotSpec> inputs, MachineMenu.SlotSpec output, int arrowX, int arrowY, boolean readout,
            int defaultEnergy, int defaultTicks) {
        this.id = id;
        this.recipeName = recipeName;
        this.inputs = inputs;
        this.output = new MachineMenu.SlotSpec(output.x(), output.y(), false);
        this.arrowX = arrowX;
        this.arrowY = arrowY;
        this.readout = readout;
        this.defaultEnergy = defaultEnergy;
        this.defaultTicks = defaultTicks;
    }

    private static MachineMenu.SlotSpec slot(int x, int y) {
        return new MachineMenu.SlotSpec(x, y, true);
    }

    private static List<MachineMenu.SlotSpec> grid(int x, int y) {
        List<MachineMenu.SlotSpec> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                slots.add(slot(x + col * 18, y + row * 18));
            }
        }
        return List.copyOf(slots);
    }

    public String id() {
        return this.id;
    }

    // The recipe type and serializer name, e.g. fusionfactions:crushing.
    public String recipeName() {
        return this.recipeName;
    }

    public int inputCount() {
        return this.inputs.size();
    }

    // Input slots first, then the output slot.
    public List<MachineMenu.SlotSpec> slots() {
        List<MachineMenu.SlotSpec> slots = new ArrayList<>(this.inputs);
        slots.add(this.output);
        return slots;
    }

    public int arrowX() {
        return this.arrowX;
    }

    public int arrowY() {
        return this.arrowY;
    }

    // Whether the screen has a text readout; the assembler's 3x3 grid leaves no room for one.
    public boolean hasReadout() {
        return this.readout;
    }

    public int defaultEnergy() {
        return this.defaultEnergy;
    }

    public int defaultTicks() {
        return this.defaultTicks;
    }
}
