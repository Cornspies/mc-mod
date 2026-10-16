#!/usr/bin/env python3
"""Generates the textures of the power blocks and the machine screens (DESIGN.md §6b.3, §6b.4).

- combustion generator and charging station (front, lit front, side, top), solar panel, FE cable
- crusher, chemical mixer, circuit fabricator and assembler (front, lit front, side, top)
- machine screens: a panel with the machine's slots, a readout on the left (not on the assembler), an energy gauge on
  the right and the player's inventory, plus each machine's extras (the generator's flame, the charging station's
  charge bar, the processing machines' progress arrow)

The 3D models of the solar panel and the cable are in tools/machine_models.py.

Run from the repository root:  python3 tools/power_textures.py
"""
from missile_textures import (CORNER_BOLTS, EDGE, GREEN, HIGHLIGHT, HOLE, METAL, PANEL, SCREEN_BG, SCREEN_EDGE, SHADOW, SHINE, WARN, bolts, fill, inset,
                              plate, put, stripes)
from workbench_textures import ASSETS, BG, DARK_EDGE, LIGHT_EDGE, SLOT_BG, SLOT_DARK, write_png

FIRE, FIRE_HOT, FIRE_DIM = (240, 120, 30), (255, 210, 90), (110, 50, 20)
COPPER, COPPER_DARK = (205, 118, 62), (140, 72, 38)
CELL, CELL_LIGHT, CELL_LINE = (32, 58, 120), (60, 100, 180), (170, 180, 195)
CYAN = (70, 210, 250)


def generator(kind):
    px = plate()
    if kind in ('front', 'front_on'):
        # A firebox behind a grill, with a pressure dial above it.
        inset(px, 3, 6, 12, 13, HOLE)
        if kind == 'front_on':
            for y in range(7, 13):
                for x in range(4, 12):
                    put(px, x, y, FIRE_HOT if y >= 11 and (x + y) % 3 else FIRE if y >= 9 else FIRE_DIM)
        for x in (5, 8, 10):
            fill(px, x, 7, x, 12, SHADOW)
        fill(px, 6, 2, 9, 4, PANEL)
        put(px, 7, 3, WARN if kind == 'front_on' else SHADOW)
        put(px, 8, 3, SHINE)
    elif kind == 'side':
        for y in (4, 6, 8, 10):
            fill(px, 3, y, 12, y, HOLE)
        bolts(px, (2, 12), (12, 12))
    elif kind == 'top':
        # An exhaust grate.
        inset(px, 4, 4, 11, 11, HOLE)
        for i in range(5, 11, 2):
            fill(px, 5, i, 10, i, SHADOW)
        bolts(px, *CORNER_BOLTS)
    return px


BOLT = [(8, 3), (7, 4), (7, 5), (6, 6), (6, 7), (7, 7), (8, 7), (9, 7), (8, 8), (8, 9), (7, 10), (7, 11), (6, 12)]


def charging_station(kind):
    px = plate()
    if kind in ('front', 'front_on'):
        # A panel with a lightning bolt that lights up while it charges.
        inset(px, 3, 2, 12, 13, SCREEN_BG)
        for x, y in BOLT:
            put(px, x, y, (140, 255, 170) if kind == 'front_on' else (40, 70, 50))
        if kind == 'front_on':
            for x, y in BOLT[::3]:
                put(px, x + 1, y, GREEN)
    elif kind == 'side':
        # A copper conduit up the side.
        fill(px, 7, 1, 8, 14, COPPER_DARK)
        fill(px, 7, 1, 7, 14, COPPER)
        bolts(px, *CORNER_BOLTS)
    elif kind == 'top':
        # The charging pad, with two copper contacts.
        inset(px, 3, 3, 12, 12)
        fill(px, 5, 6, 6, 9, COPPER)
        fill(px, 9, 6, 10, 9, COPPER)
        put(px, 5, 6, (240, 170, 110))
        put(px, 9, 6, (240, 170, 110))
    return px


def solar(kind):
    px = plate()
    if kind == 'top':
        # 2x2 cells with silver grid lines in a metal frame.
        fill(px, 1, 1, 14, 14, CELL)
        for i in range(1, 15):
            for j in (1, 7, 8, 14):
                put(px, i, j, CELL_LINE)
                put(px, j, i, CELL_LINE)
        for x0, y0 in ((2, 2), (9, 2), (2, 9), (9, 9)):
            fill(px, x0, y0, x0 + 4, y0, CELL_LIGHT)
            fill(px, x0, y0, x0, y0 + 4, CELL_LIGHT)
    elif kind == 'side':
        # The model only uses the bottom 6 rows: a frame around a dark edge.
        fill(px, 0, 0, 15, 9, METAL)
        fill(px, 0, 10, 15, 10, EDGE)
        fill(px, 1, 11, 14, 11, HIGHLIGHT)
        fill(px, 1, 12, 14, 13, PANEL)
        fill(px, 1, 14, 14, 14, SHADOW)
    else:
        inset(px, 4, 4, 11, 11)
        bolts(px, *CORNER_BOLTS)
    return px


def cable():
    """Rubber insulation with a copper stripe in rows 7-8; the cable models use rows 6-9 for their long faces."""
    px = [[(52, 54, 62, 255) for _ in range(16)] for _ in range(16)]
    for x in range(16):
        put(px, x, 6, (72, 76, 86))
        put(px, x, 7, COPPER)
        put(px, x, 8, COPPER_DARK)
        put(px, x, 9, (36, 38, 44))
    return px


# Machine screens: 176x166 on a 256x256 sheet, like vanilla's furnace.
GUI_W, GUI_H = 176, 166
INVENTORY_Y = 83  # top-left of the inventory slot frames (the menu's slot positions minus 1)
READOUT = (7, 16, 69, 70)  # x0, y0, x1, y1 of the text readout
GAUGE = (155, 16, 170, 70)  # the energy gauge's frame; the screen fills the inside


def machine_gui(slots, extra=None, readout=True):
    img = [[(0, 0, 0, 0)] * 256 for _ in range(256)]

    def px(x, y, color):
        img[y][x] = color + (255,) if len(color) == 3 else color

    def area(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px(x, y, color)

    def sunken(x0, y0, x1, y1, inner, edge=None):
        area(x0, y0, x1, y0 + 1, SLOT_DARK)
        area(x0, y0, x0 + 1, y1, SLOT_DARK)
        area(x0 + 1, y1 - 1, x1, y1, LIGHT_EDGE)
        area(x1 - 1, y0 + 1, x1, y1, LIGHT_EDGE)
        if edge:
            area(x0 + 1, y0 + 1, x1 - 1, y1 - 1, edge)
            area(x0 + 2, y0 + 2, x1 - 2, y1 - 2, inner)
        else:
            area(x0 + 1, y0 + 1, x1 - 1, y1 - 1, inner)

    w, h = GUI_W, GUI_H
    area(1, 1, w - 1, h - 1, BG)
    area(2, 0, w - 3, 1, (0, 0, 0))
    area(2, h - 1, w - 3, h, (0, 0, 0))
    area(0, 2, 1, h - 3, (0, 0, 0))
    area(w - 1, 2, w, h - 3, (0, 0, 0))
    for x, y in ((1, 1), (w - 2, h - 2), (w - 3, 1), (1, h - 3), (w - 2, 2), (2, h - 2)):
        px(x, y, (0, 0, 0))
    area(2, 1, w - 3, 3, LIGHT_EDGE)
    area(1, 2, 3, h - 3, LIGHT_EDGE)
    area(3, h - 3, w - 2, h - 1, DARK_EDGE)
    area(w - 3, 3, w - 1, h - 2, DARK_EDGE)
    for x, y in ((w - 3, 2), (2, h - 3)):
        px(x, y, BG)
    px(3, 3, LIGHT_EDGE)

    def slot(x, y):
        sunken(x, y, x + 18, y + 18, SLOT_BG)
        px(x + 17, y, SLOT_BG)
        px(x, y + 17, SLOT_BG)

    if readout:
        sunken(*READOUT, SCREEN_BG, SCREEN_EDGE)
    sunken(*GAUGE, (26, 26, 26))
    for x, y in slots:
        slot(x - 1, y - 1)
    for row in range(3):
        for col in range(9):
            slot(7 + col * 18, INVENTORY_Y + row * 18)
    for col in range(9):
        slot(7 + col * 18, INVENTORY_Y + 58)
    if extra:
        extra(img, px, area, sunken)
    return img


# The flame above the generator's fuel slot: an outline in the panel, and the lit flame at (176, 0) that the screen
# draws over it, cut by how much fuel is left.
FLAME = [
    '......o.......',
    '.....oo.......',
    '.....ooo......',
    '....oooo..o...',
    '....ooooo.oo..',
    '...ooooooooo..',
    '...oooooooooo.',
    '..ooooooooooo.',
    '..ooooooooooo.',
    '.ooooooooooooo',
    '.ooooooooooooo',
    '.ooooooooooooo',
    '..ooooooooooo.',
    '....ooooooo...',
]
FLAME_POS = (90, 35)


def generator_extra(img, px, area, sunken):
    for y, row in enumerate(FLAME):
        for x, c in enumerate(row):
            if c == 'o':
                px(FLAME_POS[0] + x, FLAME_POS[1] + y, (130, 130, 130))
                hot = y >= 9 and 3 <= x <= 10
                px(176 + x, y, FIRE_HOT if hot else FIRE if y >= 4 else (220, 80, 20))


# The bar under the charging station's slot, filled by the item's charge.
CHARGE_BAR = (71, 57, 126, 63)


def charging_extra(img, px, area, sunken):
    sunken(*CHARGE_BAR, (26, 26, 26))
    for x, y in BOLT:
        px(90 + x, 13 + y, (130, 130, 130))


# The processing machines' screens: input slots, output slot and arrow positions, matching industry.Machine.
PROCESSING = {
    'crusher': ([(76, 35)], (126, 35), (100, 35), True),
    'chemical_mixer': ([(76, 17), (76, 35), (76, 53)], (126, 35), (100, 35), True),
    'circuit_fabricator': ([(72, 26), (90, 26), (72, 44), (90, 44)], (136, 35), (110, 35), True),
    'assembler': ([(30 + c * 18, 17 + r * 18) for r in range(3) for c in range(3)], (120, 35), (92, 35), False),
}


def arrow_pixels():
    """A 22x16 arrow pointing right."""
    for y in range(16):
        for x in range(22):
            if (x <= 13 and 5 <= y <= 10) or (x >= 13 and abs(y - 7.5) <= 21.5 - x):
                yield x, y


def arrow_extra(pos):
    """The empty arrow in the panel, and the filled one at (176, 0) that the screen draws over it by progress."""
    def extra(img, px, area, sunken):
        for x, y in arrow_pixels():
            px(pos[0] + x, pos[1] + y, (139, 139, 139))
            px(176 + x, y, (255, 255, 255) if y in (6, 7) and x < 14 else (230, 230, 230))
    return extra


def crusher(kind):
    """Two toothed rollers behind a slot; they glow hot while grinding."""
    px = plate()
    if kind in ('front', 'front_on'):
        inset(px, 2, 4, 13, 11, HOLE)
        for cx in (5, 10):
            fill(px, cx - 2, 5, cx + 2, 10, PANEL)
            for y in range(5, 11, 2):
                put(px, cx - 2, y, SHINE)
                put(px, cx + 2, y + 1, SHINE)
        if kind == 'front_on':
            fill(px, 7, 5, 8, 10, FIRE)
            put(px, 7, 7, FIRE_HOT)
            put(px, 8, 8, FIRE_HOT)
        stripes(px, 2, 12, 13, 13)
    elif kind == 'side':
        stripes(px, 2, 12, 13, 13)
        bolts(px, (2, 2), (12, 2))
        for y in (5, 7, 9):
            fill(px, 4, y, 11, y, HOLE)
    elif kind == 'top':
        # The feed hopper.
        inset(px, 3, 3, 12, 12, PANEL)
        inset(px, 5, 5, 10, 10, HOLE)
        bolts(px, *CORNER_BOLTS)
    return px


def mixer(kind):
    """A glass tank with the mixture in it; it bubbles while mixing."""
    px = plate()
    liquid, bright = (60, 120, 70), (120, 230, 120)
    if kind in ('front', 'front_on'):
        inset(px, 4, 2, 11, 13, (30, 40, 44))
        fill(px, 5, 7, 10, 12, bright if kind == 'front_on' else liquid)
        if kind == 'front_on':
            for x, y in ((6, 9), (9, 8), (7, 5), (8, 11)):
                put(px, x, y, (220, 255, 220))
        fill(px, 5, 3, 5, 6, (90, 110, 120))
        bolts(px, (2, 2), (12, 2), (2, 12), (12, 12))
    elif kind == 'side':
        # A pipe running down the side.
        fill(px, 3, 1, 4, 14, COPPER_DARK)
        fill(px, 3, 1, 3, 14, COPPER)
        fill(px, 3, 8, 12, 9, COPPER_DARK)
        fill(px, 3, 8, 12, 8, COPPER)
        bolts(px, (11, 2), (11, 12))
    elif kind == 'top':
        inset(px, 3, 3, 12, 12, PANEL)
        fill(px, 6, 6, 9, 9, SHADOW)
        fill(px, 7, 2, 8, 6, SHINE)
    return px


def fabricator(kind):
    """A screen with circuit traces that light up while it works."""
    px = plate()
    if kind in ('front', 'front_on'):
        inset(px, 2, 2, 13, 12, SCREEN_BG)
        trace = CYAN if kind == 'front_on' else (30, 70, 80)
        for x, y in ((4, 4), (5, 4), (6, 4), (6, 5), (6, 6), (7, 6), (8, 6), (9, 6), (9, 7), (9, 8), (10, 8), (11, 8),
                     (4, 9), (5, 9), (5, 10), (6, 10), (7, 10), (8, 10), (11, 4), (11, 5), (11, 6)):
            put(px, x, y, trace)
        fill(px, 7, 8, 8, 9, (200, 180, 60) if kind == 'front_on' else (70, 60, 30))
        fill(px, 3, 14, 12, 14, SHADOW)
    elif kind == 'side':
        for x in (4, 6, 8, 10):
            fill(px, x, 3, x, 12, HOLE)
        bolts(px, (2, 13), (12, 13))
    elif kind == 'top':
        # The etching laser over a wafer.
        inset(px, 3, 3, 12, 12, (40, 46, 60))
        for i in range(4, 12, 2):
            fill(px, 4, i, 11, i, (60, 70, 95))
        fill(px, 7, 7, 8, 8, (230, 60, 60))
    return px


def assembler(kind):
    """A window onto the assembly arm; a lamp lights up while it builds."""
    px = plate()
    if kind in ('front', 'front_on'):
        inset(px, 2, 3, 13, 12, (30, 33, 38))
        fill(px, 4, 10, 11, 11, PANEL)
        fill(px, 5, 5, 6, 10, SHINE)
        fill(px, 6, 5, 10, 6, SHINE)
        fill(px, 10, 6, 10, 8, SHADOW)
        put(px, 3, 1, WARN if kind == 'front_on' else SHADOW)
        put(px, 12, 1, WARN if kind == 'front_on' else SHADOW)
        stripes(px, 2, 13, 13, 13)
    elif kind == 'side':
        # A gear.
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if 2.2 < d < 4.6 or (4.6 <= d < 5.6 and int((x + y) % 3) == 0):
                    put(px, x, y, SHADOW)
        bolts(px, *CORNER_BOLTS)
    elif kind == 'top':
        inset(px, 2, 2, 13, 13, PANEL)
        for i in range(4, 13, 3):
            fill(px, 3, i, 12, i, SHADOW)
            fill(px, i, 3, i, 12, SHADOW)
    return px


MACHINE_FACES = {'crusher': crusher, 'chemical_mixer': mixer, 'circuit_fabricator': fabricator, 'assembler': assembler}


def main():
    for kind in ('front', 'front_on', 'side', 'top'):
        write_png(ASSETS / f'block/combustion_generator_{kind}.png', 16, 16, generator(kind))
        write_png(ASSETS / f'block/charging_station_{kind}.png', 16, 16, charging_station(kind))
    for kind in ('top', 'side', 'bottom'):
        write_png(ASSETS / f'block/solar_panel_{kind}.png', 16, 16, solar(kind))
    write_png(ASSETS / 'block/fe_cable.png', 16, 16, cable())
    write_png(ASSETS / 'gui/container/combustion_generator.png', 256, 256, machine_gui([(89, 53)], generator_extra))
    write_png(ASSETS / 'gui/container/charging_station.png', 256, 256, machine_gui([(89, 35)], charging_extra))
    for name, (inputs, output, arrow, readout) in PROCESSING.items():
        write_png(ASSETS / f'gui/container/{name}.png', 256, 256, machine_gui(inputs + [output], arrow_extra(arrow), readout))
        for kind in ('front', 'front_on', 'side', 'top'):
            write_png(ASSETS / f'block/{name}_{kind}.png', 16, 16, MACHINE_FACES[name](kind))
    print('wrote power textures')


if __name__ == '__main__':
    main()
