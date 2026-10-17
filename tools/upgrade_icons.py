#!/usr/bin/env python3
"""Draws the weapon upgrade part icons (DESIGN.md §4.5) and the workbench's empty-field silhouettes.

One 16x16 picture per upgrade type that shows what the part does, in a shaded metal palette with the tier color
(blue / orange / red, like the crate rarities) on the part's accent:

- penetration: an armor-piercing round with a tier-colored penetrator tip
- magazine: a magazine with a round on top and a tier band
- noise: a suppressor with tier bands
- recoil: a spring between two plates
- reload: a circular arrow around a magazine
- accuracy: a reticle with a tier-colored center
- range: a round with speed lines behind it
- sight: one picture per tier, like its name: a red dot (I), a holographic sight (II), a 2x optic (III)

Shapes are written without their outline; a dark outline is added around every shape. Each icon has one to three
tier pips in its lower right corner, so the tiers differ by more than their color.

Run from the repository root:  python3 tools/upgrade_icons.py
"""
import math

from workbench_textures import ASSETS, TIER_COLORS, write_png

OUTLINE = (26, 28, 34)
BASE = {
    '*': (226, 231, 238),  # highlight
    '-': (172, 180, 194),  # light metal
    '=': (116, 124, 140),  # metal
    '#': (74, 80, 92),     # dark metal
    'k': (40, 42, 48),     # black detail
    'Y': (246, 216, 132),  # brass, light
    'y': (214, 170, 70),   # brass
    'b': (150, 110, 42),   # brass, dark
    'r': (255, 64, 52),    # red glow
    'R': (255, 182, 170),  # red glow, light
    'g': (52, 78, 96),     # glass
    'G': (96, 132, 156),   # glass, light
    'c': (88, 200, 240),   # lens
    'C': (206, 246, 255),  # lens, light
}


def tier_palette(tier):
    color = TIER_COLORS[tier]
    light = tuple(round(c + (255 - c) * 0.45) for c in color)
    dark = tuple(round(c * 0.6) for c in color)
    return {**BASE, '+': color, 'w': light, 't': dark}


# Filled shapes, 16x16, no outline. '.' is empty. The lower right corner (columns 9-15, rows 12-15) stays free for
# the tier pips.
PENETRATION = [
    '................',
    '......w+........',
    '.....w++t.......',
    '.....w++t.......',
    '....w+++tt......',
    '....w+++tt......',
    '....kkkkkk......',
    '....Yyyyyb......',
    '....Yyyyyb......',
    '....Yyyyyb......',
    '....Yyyyyb......',
    '...YYyyyybb.....',
    '................',
    '................',
    '................',
    '................',
]

MAGAZINE = [
    '.....YYyb.......',
    '....*----#......',
    '....-####k......',
    '....-#==#k......',
    '....-####k......',
    '....w++++t......',
    '.....-####k.....',
    '.....-#==#k.....',
    '.....-####k.....',
    '......-####k....',
    '......-#==#k....',
    '......w+++tt....',
    '................',
    '................',
    '................',
    '................',
]

SPEED_ROUND = [
    '................',
    '................',
    '................',
    '................',
    '.--.....YYYYw+..',
    '......yyyyyy+++.',
    '.***..yyyyyy++++',
    '......yyyyyy+++t',
    '.--.....bbbbt+t.',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]

RED_DOT = [
    '................',
    '.....=----=.....',
    '....=-gggg-#....',
    '...=-gGgggg-#...',
    '...-gGgggggg#...',
    '...-ggggrggg#...',
    '...-gggrRrgg#...',
    '...-ggggrggg#...',
    '...=-gggggg-#...',
    '....=-gggg-#....',
    '.....=####=.....',
    '....w++++++t....',
    '....########....',
    '................',
    '................',
    '................',
]

HOLOGRAPHIC = [
    '................',
    '*-------------=.',
    '.=############k.',
    '.-#gGgggggggg#k.',
    '.-#Gggggrrrgg#k.',
    '.-#ggggrgggrg#k.',
    '.-#ggggrgrgrg#k.',
    '.-#ggggrgggrg#k.',
    '.-#gggggrrrgg#k.',
    '.=############k.',
    '....-######k....',
    '...w+++++++t....',
    '...#########....',
    '................',
    '................',
    '................',
]

OPTIC = [
    '................',
    '................',
    '................',
    '.cc*---------cc.',
    'cCC*--w++t---cCc',
    'cCc=--w++t===ccc',
    'ccc===w++t===ccc',
    '.cc##########cc.',
    '....##....##....',
    '...####..####...',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]


def blank():
    return [['.'] * 16 for _ in range(16)]


def suppressor():
    """A cylinder lying across the icon: shaded top to bottom, two tier bands, a dark muzzle cap with its bore."""
    grid = blank()
    shades = ['*', '-', '-', '=', '=', '#']
    bands = ['w', '+', '+', '+', 't', 't']
    for i, row in enumerate(range(4, 10)):
        for x in range(1, 15):
            ch = shades[i]
            if x in (5, 6, 10, 11):
                ch = bands[i]
            if x <= 2:
                ch = '#' if i > 0 else '='
            grid[row][x] = ch
    for row in (6, 7):
        grid[row][1] = 'k'
    return grid


def spring():
    """Two plates with a slanted coil spring between them."""
    grid = blank()
    for y in range(2, 12):
        grid[y][1], grid[y][2] = '-', '='
        grid[y][13], grid[y][14] = '-', '#'
    for y in range(6, 8):
        for x in range(3, 13):
            grid[y][x] = '='
    for c in (4, 6, 8, 10):
        coil = [(c + 1, 3, 'w'), (c + 1, 4, '+'), (c, 5, '+'), (c, 6, '+'), (c, 7, '+'), (c, 8, '+'), (c - 1, 9, 't'), (c - 1, 10, 't')]
        for x, y, ch in coil:
            grid[y][x] = ch
    return grid


def reload_arrow():
    """A circular arrow in the tier color, running clockwise, around a small magazine."""
    grid = blank()
    cx, cy = 7.0, 7.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            angle = math.degrees(math.atan2(y - cy, x - cx))
            if 4.6 <= d <= 6.2 and not (-75 <= angle <= -15):
                grid[y][x] = 'w' if y < cy else ('+' if d < 5.4 else 't')
    # The arrowhead where the ring opens at the top, pointing along it (to the right).
    for x, ys in ((8, range(0, 5)), (9, range(1, 4)), (10, range(2, 3))):
        for y in ys:
            grid[y][x] = '+' if y > 1 else 'w'
    for y in range(5, 10):
        for x in range(6, 9):
            grid[y][x] = '*' if x == 6 else ('-' if x == 7 else '#')
    grid[5][7] = 'y'
    return grid


def reticle():
    """A ring with four ticks and a tier-colored center."""
    grid = blank()
    cx, cy = 7.0, 6.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 4.5 <= d <= 5.5:
                grid[y][x] = '-' if y <= cy else '='
    for i in range(3):
        grid[int(cy)][1 + i] = grid[int(cy)][11 + i] = '-'
        grid[i][int(cx)] = grid[10 + i][int(cx)] = '-'
    for x, y, ch in ((6, 5, 'w'), (7, 5, '+'), (6, 6, '+'), (7, 6, 't')):
        grid[y][x] = ch
    return grid


def rows_to_grid(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return [list(r) for r in rows]


SHAPES = {
    'penetration': lambda tier: rows_to_grid(PENETRATION),
    'magazine': lambda tier: rows_to_grid(MAGAZINE),
    'noise': lambda tier: suppressor(),
    'recoil': lambda tier: spring(),
    'reload': lambda tier: reload_arrow(),
    'accuracy': lambda tier: reticle(),
    'range': lambda tier: rows_to_grid(SPEED_ROUND),
    'sight': lambda tier: rows_to_grid({1: RED_DOT, 2: HOLOGRAPHIC, 3: OPTIC}[tier]),
}


def outlined(grid):
    """Adds an outline ('o') on every empty pixel next to a filled one (also diagonally, like vanilla items)."""
    out = [row[:] for row in grid]
    for y in range(16):
        for x in range(16):
            if grid[y][x] != '.':
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and grid[ny][nx] not in ('.', 'o'):
                    out[y][x] = 'o'
                    break
    return out


def with_pips(grid, tier):
    """One to three short bars in the tier color in the lower right corner, on a small dark plate."""
    left = 13 - 2 * (tier - 1)
    for y in range(12, 16):
        for x in range(left, 16):
            grid[y][x] = 'o'
    for i in range(tier):
        grid[13][14 - 2 * i] = 'w'
        grid[14][14 - 2 * i] = '+'
    return grid


def icon(kind, tier):
    palette = tier_palette(tier)
    grid = with_pips(outlined(SHAPES[kind](tier)), tier)
    return [[(0, 0, 0, 0) if ch == '.' else (OUTLINE if ch == 'o' else palette[ch]) + (255,) for ch in row] for row in grid]


def field_silhouette(kind):
    """The empty workbench field: the shape (the sight's tier II picture) as a faint shadow, like vanilla's empty slots."""
    grid = outlined(SHAPES[kind](2))
    return [[(0, 0, 0, 0) if ch == '.' else ((0, 0, 0, 80) if ch == 'o' else (0, 0, 0, 50)) for ch in row] for row in grid]


def main():
    sprites = ASSETS / 'gui/sprites/container/weapon_workbench'
    for kind in SHAPES:
        write_png(sprites / f'{kind}.png', 16, 16, field_silhouette(kind))
        for tier in (1, 2, 3):
            write_png(ASSETS / f'item/{kind}_upgrade_{tier}.png', 16, 16, icon(kind, tier))
    print('wrote 24 upgrade icons and 8 field silhouettes')


if __name__ == '__main__':
    main()
