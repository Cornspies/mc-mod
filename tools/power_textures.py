#!/usr/bin/env python3
"""Generates the textures of the power blocks and the machine screens (DESIGN.md §6b.3, §6b.4).

- combustion generator and charging station (front, lit front, side, top), solar panel, FE cable
- machine screens: a panel with the machine's slots, a readout on the left, an energy gauge on the right and the
  player's inventory, plus each machine's extras (the generator's flame, the charging station's charge bar)

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
READOUT = (7, 16, 74, 70)  # x0, y0, x1, y1 of the text readout
GAUGE = (151, 16, 167, 70)  # the energy gauge's frame; the screen fills the inside


def machine_gui(slots, extra=None):
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
FLAME_POS = (81, 35)


def generator_extra(img, px, area, sunken):
    for y, row in enumerate(FLAME):
        for x, c in enumerate(row):
            if c == 'o':
                px(FLAME_POS[0] + x, FLAME_POS[1] + y, (130, 130, 130))
                hot = y >= 9 and 3 <= x <= 10
                px(176 + x, y, FIRE_HOT if hot else FIRE if y >= 4 else (220, 80, 20))


# The bar under the charging station's slot, filled by the item's charge.
CHARGE_BAR = (62, 57, 116, 63)


def charging_extra(img, px, area, sunken):
    sunken(*CHARGE_BAR, (26, 26, 26))
    for x, y in BOLT:
        px(81 + x, 13 + y, (130, 130, 130))


def main():
    for kind in ('front', 'front_on', 'side', 'top'):
        write_png(ASSETS / f'block/combustion_generator_{kind}.png', 16, 16, generator(kind))
        write_png(ASSETS / f'block/charging_station_{kind}.png', 16, 16, charging_station(kind))
    for kind in ('top', 'side', 'bottom'):
        write_png(ASSETS / f'block/solar_panel_{kind}.png', 16, 16, solar(kind))
    write_png(ASSETS / 'block/fe_cable.png', 16, 16, cable())
    write_png(ASSETS / 'gui/container/combustion_generator.png', 256, 256, machine_gui([(80, 53)], generator_extra))
    write_png(ASSETS / 'gui/container/charging_station.png', 256, 256, machine_gui([(80, 35)], charging_extra))
    print('wrote power textures')


if __name__ == '__main__':
    main()
