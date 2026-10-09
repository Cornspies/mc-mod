#!/usr/bin/env python3
"""Generates the 2D textures for missiles (DESIGN.md §6.4).

- the missile item icons, one per type and tier, with the tier color (blue / orange / red)
- the target designator icon
- the missile launcher screen background (176x176 on a 256x256 sheet) and its empty-slot silhouettes
- the missile launcher block (top, side, bottom)
- the missile silo blocks: casing, hatch (closed and open) and controller

Run from the repository root:  python3 tools/missile_textures.py
"""
from workbench_textures import ASSETS, BG, BODY, DARK, DARK_EDGE, LIGHT, LIGHT_EDGE, SLOT_BG, SLOT_DARK, TIER_COLORS, icon_pixels, silhouette, write_png

# The band in the middle of the icon shows the type, like on the 3D model.
TYPES = {'explosive': (24, 26, 30), 'breaching': (130, 80, 190), 'emp': (70, 210, 250), 'incendiary': (230, 140, 40)}

# 16x16. '#' body, '-' light metal, '+' tier color, 'o' dark detail, '*' exhaust glow, '.' transparent.
MISSILE_ICON = [
    '..............+.',
    '.............+++',
    '............++++',
    '...........-+++.',
    '..........----..',
    '.........-+--...',
    '........----....',
    '.......oooo.....',
    '......oooo......',
    '.....----.......',
    '..#.----........',
    '..##---.........',
    '...##-#.........',
    '..**###.........',
    '.**..##.........',
    '*...............',
]

DESIGNATOR_ICON = [
    '................',
    '................',
    '..........+.....',
    '.........+.+....',
    '.##########.....',
    '.#--------#+....',
    '.#-oooooo-#.....',
    '.#--------#.....',
    '.##########.....',
    '....###.........',
    '....###.........',
    '....##..........',
    '....##..........',
    '................',
    '................',
    '................',
]

DESIGNATOR_COLOR = (230, 50, 40)
EXHAUST = (255, 170, 60)

GUI_W, GUI_H = 176, 176
MISSILE_SLOT, DESIGNATOR_SLOT = (25, 34), (25, 56)  # top-left of the slot frames (the menu's slot positions minus 1)
SCREEN = (49, 17, 170, 58)  # the readout panel, x0, y0, x1, y1
INVENTORY_Y = 93
SCREEN_BG, SCREEN_EDGE = (16, 26, 20), (40, 60, 46)


def missile_icon(kind, tier):
    return icon_pixels(MISSILE_ICON, {'#': BODY + (255,), '-': LIGHT + (255,), '+': TIER_COLORS[tier] + (255,),
                                      'o': TYPES[kind] + (255,), '*': EXHAUST + (255,)})


def designator_icon():
    return icon_pixels(DESIGNATOR_ICON, {'#': BODY + (255,), '-': LIGHT + (255,), '+': DESIGNATOR_COLOR + (255,), 'o': (40, 70, 90, 255)})


def gui():
    img = [[(0, 0, 0, 0)] * 256 for _ in range(256)]

    def put(x, y, color):
        img[y][x] = color + (255,) if len(color) == 3 else color

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                put(x, y, color)

    # Panel with vanilla's rounded bevel (same as the weapon workbench).
    fill(1, 1, GUI_W - 1, GUI_H - 1, BG)
    fill(2, 0, GUI_W - 3, 1, (0, 0, 0))
    fill(2, GUI_H - 1, GUI_W - 3, GUI_H, (0, 0, 0))
    fill(0, 2, 1, GUI_H - 3, (0, 0, 0))
    fill(GUI_W - 1, 2, GUI_W, GUI_H - 3, (0, 0, 0))
    for x, y in ((1, 1), (GUI_W - 2, GUI_H - 2), (GUI_W - 3, 1), (1, GUI_H - 3), (GUI_W - 2, 2), (2, GUI_H - 2)):
        put(x, y, (0, 0, 0))
    fill(2, 1, GUI_W - 3, 3, LIGHT_EDGE)
    fill(1, 2, 3, GUI_H - 3, LIGHT_EDGE)
    fill(3, GUI_H - 3, GUI_W - 2, GUI_H - 1, DARK_EDGE)
    fill(GUI_W - 3, 3, GUI_W - 1, GUI_H - 2, DARK_EDGE)
    for x, y in ((GUI_W - 3, 2), (2, GUI_H - 3)):
        put(x, y, BG)
    put(3, 3, LIGHT_EDGE)

    def slot(x, y, size=18):
        fill(x, y, x + size - 1, y + 1, SLOT_DARK)
        fill(x, y, x + 1, y + size - 1, SLOT_DARK)
        fill(x + 1, y + size - 1, x + size, y + size, LIGHT_EDGE)
        fill(x + size - 1, y + 1, x + size, y + size, LIGHT_EDGE)
        fill(x + 1, y + 1, x + size - 1, y + size - 1, SLOT_BG)
        put(x + size - 1, y, SLOT_BG)
        put(x, y + size - 1, SLOT_BG)

    # A link from the designator to the missile, then the slots.
    fill(MISSILE_SLOT[0] + 8, MISSILE_SLOT[1] + 17, MISSILE_SLOT[0] + 10, DESIGNATOR_SLOT[1] + 1, (160, 160, 160))
    slot(*MISSILE_SLOT)
    slot(*DESIGNATOR_SLOT)

    # The readout: a dark screen, sunken like a slot.
    x0, y0, x1, y1 = SCREEN
    fill(x0, y0, x1, y0 + 1, SLOT_DARK)
    fill(x0, y0, x0 + 1, y1, SLOT_DARK)
    fill(x0 + 1, y1 - 1, x1, y1, LIGHT_EDGE)
    fill(x1 - 1, y0 + 1, x1, y1, LIGHT_EDGE)
    fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, SCREEN_EDGE)
    fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, SCREEN_BG)

    for row in range(3):
        for col in range(9):
            slot(7 + col * 18, INVENTORY_Y + row * 18)
    for col in range(9):
        slot(7 + col * 18, INVENTORY_Y + 58)
    return img


def block_face(kind):
    metal, dark, edge, warn = (82, 88, 100), (50, 54, 62), (36, 38, 44), (230, 170, 40)
    px = [[metal + (255,) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        for j in (0, 15):
            px[i][j] = edge + (255,)
            px[j][i] = edge + (255,)
    if kind == 'top':
        # A launch hatch: a dark round opening with rails and warning corners.
        for y in range(16):
            for x in range(16):
                d = (x - 7.5) ** 2 + (y - 7.5) ** 2
                if d < 20:
                    px[y][x] = (20, 22, 26, 255)
                elif d < 30:
                    px[y][x] = dark + (255,)
        for x, y in ((1, 1), (2, 1), (1, 2), (14, 1), (13, 1), (14, 2), (1, 14), (2, 14), (1, 13), (14, 14), (13, 14), (14, 13)):
            px[y][x] = warn + (255,)
        for y in range(5, 11):
            px[y][7] = (110, 118, 132, 255)
            px[y][8] = (110, 118, 132, 255)
    elif kind == 'side':
        # Warning stripes along the top, a panel with vents below.
        for x in range(1, 15):
            for y in (2, 3):
                px[y][x] = (warn if (x + y) // 2 % 2 == 0 else (30, 30, 30)) + (255,)
        for y in range(6, 14):
            for x in range(3, 13):
                px[y][x] = dark + (255,)
        for y in (8, 10, 12):
            for x in range(4, 12):
                px[y][x] = (24, 26, 30, 255)
        px[6][11] = (230, 50, 40, 255)
    else:
        for y in range(1, 15):
            for x in range(1, 15):
                if (x + y) % 4 == 0:
                    px[y][x] = dark + (255,)
    return px


def silo_face(kind):
    metal, dark, edge, warn, light = (82, 88, 100), (50, 54, 62), (36, 38, 44), (230, 170, 40), (120, 128, 142)
    px = [[metal + (255,) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        for j in (0, 15):
            px[i][j] = edge + (255,)
            px[j][i] = edge + (255,)
    if kind == 'casing':
        # Heavy plates with rivets in the corners.
        for i in range(1, 15):
            px[7][i] = dark + (255,)
            px[i][7] = dark + (255,)
        for x, y in ((2, 2), (12, 2), (2, 12), (12, 12), (5, 5), (9, 9), (5, 9), (9, 5)):
            px[y][x] = light + (255,)
    elif kind in ('hatch_top', 'hatch_top_open'):
        # Two doors with warning stripes along their edges, or the dark shaft once they're open.
        for y in range(1, 15):
            for x in range(1, 15):
                if kind == 'hatch_top_open':
                    px[y][x] = ((14, 15, 18) if 3 <= x <= 12 and 3 <= y <= 12 else warn if (x + y) // 2 % 2 == 0 else (30, 30, 30)) + (255,)
                elif x in (7, 8):
                    px[y][x] = (warn if y // 2 % 2 == 0 else (30, 30, 30)) + (255,)
                elif y in (1, 14) or x in (1, 14):
                    px[y][x] = dark + (255,)
    elif kind == 'controller':
        # A control panel: a screen, buttons and a warning light.
        for y in range(2, 8):
            for x in range(2, 14):
                px[y][x] = (16, 26, 20, 255)
        for x in range(3, 10):
            px[4][x] = (124, 224, 143, 255)
        for x in range(3, 7):
            px[6][x] = (78, 138, 91, 255)
        for x, color in ((3, (230, 50, 40)), (6, (240, 150, 30)), (9, (70, 120, 255))):
            for y in range(10, 13):
                px[y][x] = color + (255,)
                px[y][x + 1] = color + (255,)
        px[11][12] = warn + (255,)
    return px


def main():
    for rows in (MISSILE_ICON, DESIGNATOR_ICON):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows)
    for kind in TYPES:
        for tier in (1, 2, 3):
            write_png(ASSETS / f'item/{kind}_missile_{tier}.png', 16, 16, missile_icon(kind, tier))
    write_png(ASSETS / 'item/target_designator.png', 16, 16, designator_icon())
    write_png(ASSETS / 'gui/container/missile_launcher.png', 256, 256, gui())
    sprites = ASSETS / 'gui/sprites/container/missile_launcher'
    write_png(sprites / 'missile.png', 16, 16, silhouette(MISSILE_ICON))
    write_png(sprites / 'designator.png', 16, 16, silhouette(DESIGNATOR_ICON))
    for kind in ('top', 'side', 'bottom'):
        write_png(ASSETS / f'block/missile_launcher_{kind}.png', 16, 16, block_face(kind))
    for kind in ('casing', 'hatch_top', 'hatch_top_open', 'controller'):
        write_png(ASSETS / f'block/silo_{kind}.png', 16, 16, silo_face(kind))
    print('wrote missile textures')


if __name__ == '__main__':
    main()
