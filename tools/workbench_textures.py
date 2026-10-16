#!/usr/bin/env python3
"""Generates the textures for weapon modification (DESIGN.md §4.5).

- the weapon workbench screen background (176x176 on a 256x256 sheet)
- one empty-field silhouette per upgrade type, the weapon slot silhouette and the locked-field overlay
  (GUI sprites, 16x16)
- the 21 upgrade part item icons, one per type and tier, with the tier color (blue / orange / red,
  like the supply crate rarities)
- the weapon workbench block (top, side, bottom)

Run from the repository root:  python3 tools/workbench_textures.py
"""
import struct
import zlib
from pathlib import Path

ASSETS = Path('src/main/resources/assets/fusionfactions/textures')

# Upgrade icons, 16x16. '#' body, '-' light metal, '+' tier color, 'o' dark detail, '.' transparent.
ICONS = {
    'penetration': [
        '.......++.......',
        '......++++......',
        '.....+o++o+.....',
        '.....#-++-#.....',
        '.....#-++-#.....',
        '....##-++-##....',
        '....#--++--#....',
        '....#--++--#....',
        '....#oo++oo#....',
        '....#--++--#....',
        '...##--++--##...',
        '..#-#--++--#-#..',
        '..#-#-++++-#-#..',
        '..#o########o#..',
        '....#oooooo#....',
        '......####......',
    ],
    'magazine': [
        '.....oo--oo.....',
        '....#--++--#....',
        '...##########...',
        '...#--#++#--#...',
        '...#oo#++#oo#...',
        '...#--#++#--#...',
        '....#-#++#-#....',
        '....#o#++#o#....',
        '....#-#++#-#....',
        '.....#o++#o#....',
        '.....#-#++-#....',
        '.....#o#++#o#...',
        '......#-#++-#...',
        '......#######...',
        '.....#ooooooo#..',
        '.....#########..',
    ],
    'noise': [
        '................',
        '................',
        '................',
        '..############..',
        '.##--#----#--##.',
        '#oo#++#--#++#oo#',
        '#--#++#--#++#--#',
        '#--#++#--#++#--#',
        '#oo#++#--#++#oo#',
        '.##--#----#--##.',
        '..############..',
        '................',
        '................',
        '................',
        '................',
        '................',
    ],
    'recoil': [
        '................',
        '.######.........',
        '.#----##........',
        '.#-oo--##.......',
        '.##--o--##......',
        '..##--o--##.....',
        '...##--o--##+...',
        '....##--o--#++#.',
        '....##--o--#++#.',
        '...##--o--##+...',
        '..##--o--##.....',
        '.##--o--##......',
        '.#----##........',
        '.######.........',
        '................',
        '................',
    ],
    'reload': [
        '..oo............',
        '.####...........',
        '.#--#....oo.....',
        '.#--#...####....',
        '.####...#--#....',
        '.#--######--#...',
        '.#--#++++#--#...',
        '.#--#++++#--#...',
        '.#--######--#...',
        '.####...#--#....',
        '.#--#...####....',
        '.#oo#...#--#....',
        '.####...#--#....',
        '........#oo#....',
        '........####....',
        '................',
    ],
    'accuracy': [
        '..###......###..',
        '..#-#..##..#-#..',
        '..#-#..##..#-#..',
        '.......++.......',
        '.......++.......',
        '.####.++++.####.',
        '.#-++-++++-++-#.',
        '.#-++-++++-++-#.',
        '.####.++++.####.',
        '.......++.......',
        '.......++.......',
        '..#-#..##..#-#..',
        '..#-#..##..#-#..',
        '..###......###..',
        '................',
        '................',
    ],
    'range': [
        '................',
        '................',
        '....##....##....',
        '...#++#..#++#...',
        '.############...',
        '##-++++--++-####',
        '#--++++--++---++',
        '#--++++--++---++',
        '##-++++--++-####',
        '.############...',
        '...#++#..#++#...',
        '....##....##....',
        '................',
        '................',
        '................',
        '................',
    ],
    'sight': [
        '................',
        '...##########...',
        '..#----------#..',
        '.##-oooooooo-##.',
        '.#--o......o--#.',
        '.#--o..++..o--#.',
        '.#--o.++++.o--#.',
        '.#--o.++++.o--#.',
        '.#--o..++..o--#.',
        '.#--oooooooo--#.',
        '.##############.',
        '..#----------#..',
        '################',
        '.#oo#......#oo#.',
        '..##........##..',
        '................',
    ],
}

WEAPON_ICON = [
    '................',
    '................',
    '.......####.....',
    '......######....',
    '..##############',
    '#####-##########',
    '..##..##..##....',
    '.....###..##....',
    '.....###..##....',
    '.....##.........',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]

LOCK_ICON = [
    '................',
    '................',
    '.....######.....',
    '....##....##....',
    '...##......##...',
    '...##......##...',
    '..############..',
    '..############..',
    '..#####..#####..',
    '..#####..#####..',
    '..#####..#####..',
    '..############..',
    '..############..',
    '................',
    '................',
    '................',
]

TIER_COLORS = {1: (70, 120, 255), 2: (240, 150, 30), 3: (230, 50, 40)}
BODY, LIGHT, DARK = (50, 54, 62), (150, 158, 172), (24, 26, 30)


def write_png(path, width, height, pixels):
    """pixels: list of rows of (r, g, b, a)."""
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in pixels)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def icon_pixels(rows, colors):
    return [[colors.get(ch, (0, 0, 0, 0)) for ch in row] for row in rows]


def item_icon(rows, tier):
    tint = TIER_COLORS[tier] + (255,)
    return icon_pixels(rows, {'#': BODY + (255,), '-': LIGHT + (255,), '+': tint, 'o': DARK + (255,)})


def silhouette(rows):
    # Like vanilla's empty armor slot icons: a faint dark shape on the slot background.
    return icon_pixels(rows, {'#': (0, 0, 0, 70), '-': (0, 0, 0, 45), '+': (0, 0, 0, 90), 'o': (0, 0, 0, 70)})


def locked():
    lock = {'#': (60, 60, 60, 255)}
    return [[lock.get(ch, (0, 0, 0, 110)) for ch in row] for row in LOCK_ICON]


# --- workbench screen ---------------------------------------------------------------------------

GUI_W, GUI_H = 176, 176
BG, LIGHT_EDGE, DARK_EDGE, SLOT_BG, SLOT_DARK = (198, 198, 198), (255, 255, 255), (85, 85, 85), (139, 139, 139), (55, 55, 55)
GRID_X, GRID_Y, GRID_STEP = 57, 17, 22  # top-left of the slot frames (the menu's slot positions minus 1)
INVENTORY_Y = 93


def gui():
    img = [[(0, 0, 0, 0)] * 256 for _ in range(256)]

    def put(x, y, color):
        img[y][x] = color + (255,) if len(color) == 3 else color

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                put(x, y, color)

    # Panel with vanilla's rounded bevel.
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

    # Connectors from the weapon to the fields, drawn first so the slot frames sit on top.
    cx, cy = GRID_X + GRID_STEP + 9, GRID_Y + GRID_STEP + 9
    for field in range(9):
        if field == 4:
            continue
        fx, fy = GRID_X + field % 3 * GRID_STEP + 9, GRID_Y + field // 3 * GRID_STEP + 9
        steps = max(abs(fx - cx), abs(fy - cy))
        for i in range(steps + 1):
            x = round(cx + (fx - cx) * i / steps)
            y = round(cy + (fy - cy) * i / steps)
            fill(x - 1, y - 1, x + 1, y + 1, (160, 160, 160))
    for field in range(9):
        slot(GRID_X + field % 3 * GRID_STEP, GRID_Y + field // 3 * GRID_STEP)

    for row in range(3):
        for col in range(9):
            slot(7 + col * 18, INVENTORY_Y + row * 18)
    for col in range(9):
        slot(7 + col * 18, INVENTORY_Y + 58)
    return img


# --- workbench block --------------------------------------------------------------------------------

def block_face(kind):
    metal, dark, edge, cyan = (82, 88, 100), (50, 54, 62), (36, 38, 44), (70, 210, 250)
    px = [[metal + (255,) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        for j in (0, 15):
            px[i][j] = edge + (255,)
            px[j][i] = edge + (255,)
    if kind == 'top':
        # A work mat with the 3x3 field layout.
        for y in range(2, 14):
            for x in range(2, 14):
                px[y][x] = dark + (255,)
        for gy in range(3):
            for gx in range(3):
                x0, y0 = 3 + gx * 4, 3 + gy * 4
                color = cyan if (gx, gy) == (1, 1) else (110, 118, 132)
                for y in range(y0, y0 + 2):
                    for x in range(x0, x0 + 2):
                        px[y][x] = color + (255,)
    elif kind == 'side':
        for x in range(1, 15):
            px[3][x] = dark + (255,)
            px[4][x] = cyan + (255,)
        for y in range(6, 15):
            for x in range(3, 13):
                px[y][x] = dark + (255,)
        for y in (8, 11):
            for x in range(4, 12):
                px[y][x] = (110, 118, 132, 255)
    else:
        for y in range(1, 15):
            for x in range(1, 15):
                if (x + y) % 4 == 0:
                    px[y][x] = dark + (255,)
    return px


def main():
    write_png(ASSETS / 'gui/container/weapon_workbench.png', 256, 256, gui())
    sprites = ASSETS / 'gui/sprites/container/weapon_workbench'
    for name, rows in ICONS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        write_png(sprites / f'{name}.png', 16, 16, silhouette(rows))
        for tier in (1, 2, 3):
            write_png(ASSETS / f'item/{name}_upgrade_{tier}.png', 16, 16, item_icon(rows, tier))
    write_png(sprites / 'weapon.png', 16, 16, silhouette(WEAPON_ICON))
    write_png(sprites / 'locked.png', 16, 16, locked())
    for kind in ('top', 'side', 'bottom'):
        write_png(ASSETS / f'block/weapon_workbench_{kind}.png', 16, 16, block_face(kind))
    print('wrote workbench textures')


if __name__ == '__main__':
    main()
