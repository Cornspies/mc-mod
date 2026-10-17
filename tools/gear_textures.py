#!/usr/bin/env python3
"""Generates the textures for the rocket launcher's gear (DESIGN.md §6b.6, §6b.7).

- item icons: the tactical helmet, the threat-warning implant and the handheld radar (the guided rocket's icon is
  drawn by tools/missile_icons.py, like the missiles)
- the tactical helmet as worn (textures/entity/equipment/humanoid/tactical.png, a vanilla-style 64x32 armor layer of
  which only the head is used), referenced by assets/fusionfactions/equipment/tactical.json
- the empty implant field in the weapon workbench, a faint silhouette like the other field icons

The rocket launcher itself is a 3D model from tools/weapon_models.py, its rocket in flight one from
tools/machine_models.py.

Run from the repository root:  python3 tools/gear_textures.py
"""
from pathlib import Path

from workbench_textures import silhouette, write_png

ASSETS = Path('src/main/resources/assets/fusionfactions')

# 16x16 pictures; '.' is transparent, the other letters are keys into the palette passed with them.
HELMET = [
    '................',
    '................',
    '.....oooooo.....',
    '...oohhhhllooo..',
    '..ohhllllllmmo..',
    '..ohlllllllmmdo.',
    '.ohllllllllmmdo.',
    '.olllllmmmmmddo.',
    '.ovvvvvvccvvdddo',
    '.ovvvvvvvvvvdddo',
    '.oommmmmmoppdddo',
    '..o.....oppoddo.',
    '........oppoddo.',
    '.........ooodo..',
    '...........oo...',
    '................',
]
HELMET_COLORS = {'o': (26, 30, 28), 'h': (128, 140, 128), 'l': (92, 104, 94), 'm': (70, 80, 72), 'd': (50, 58, 52), 'v': (22, 26, 34),
                 'c': (70, 210, 250), 'p': (36, 36, 40)}

IMPLANT = [
    '................',
    '................',
    '.......c........',
    '.......c........',
    '....oooooooo....',
    '...ommmmmmmmo...',
    '...omkkkkkkmo...',
    '..comkccckkmoc..',
    '...omkkkkkkmo...',
    '...omkkkcckmo...',
    '...ommmmmmmmo...',
    '....oooooooo....',
    '.....p.p.p.p....',
    '.....p.p.p.p....',
    '................',
    '................',
]
IMPLANT_COLORS = {'o': (30, 34, 40), 'm': (150, 158, 170), 'k': (30, 36, 46), 'c': (70, 210, 250), 'p': (220, 180, 70)}

RADAR = [
    '...........o....',
    '...........o....',
    '...........o....',
    '....oooooooooo..',
    '...ommmmmmmmmmo.',
    '...omkkkkkkkkmo.',
    '...omkkkkgkkkmo.',
    '...omkkkggkkkmo.',
    '...omkkgbggrkmo.',
    '...omkkkkkkkkmo.',
    '...omkkkkkkkkmo.',
    '...ommmmmmmmmmo.',
    '...omyymmmmxxmo.',
    '...ommmmmmmmmmo.',
    '....ommmmmmmmo..',
    '.....oooooooo...',
]
RADAR_COLORS = {'o': (26, 28, 32), 'm': (80, 88, 98), 'k': (14, 30, 24), 'g': (60, 200, 110), 'b': (170, 255, 200), 'r': (240, 60, 50),
                'y': (230, 160, 40), 'x': (70, 210, 250)}

# The empty implant field: a chip with a lead, like IMPLANT.
IMPLANT_FIELD = [
    '................',
    '................',
    '.......#........',
    '.......#........',
    '....########....',
    '...#--------#...',
    '...#-######-#...',
    '..##-######-##..',
    '...#-######-#...',
    '...#-######-#...',
    '...#--------#...',
    '....########....',
    '.....#.#.#.#....',
    '.....#.#.#.#....',
    '................',
    '................',
]


def pixels(rows, palette):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return [[(0, 0, 0, 0) if c == '.' else palette[c] + (255,) for c in row] for row in rows]


# The worn helmet. In vanilla's 64x32 armor layout the head's faces are 8x8: top at (8, 0), right side at (0, 8),
# front at (8, 8), left side at (16, 8) and back at (24, 8). The front leaves the lower face open, the sides have
# ear protectors, a dark visor with a glowing stripe runs across the front.
SHELL, LIGHT, SHADOW, OUTLINE = (70, 80, 72), (100, 112, 102), (50, 58, 52), (32, 36, 34)
VISOR, STRIPE, PAD = (22, 26, 34), (70, 210, 250), (36, 36, 40)

HEAD_FRONT = [
    'lllllllll',
    'smmmmmmms',
    'vvvvvvvv',
    'vccvvccv',
    's......s',
    's......s',
    '........',
    '........',
]
HEAD_SIDE = [
    'llllllll',
    'mmmmmmmm',
    'mmmmmmmm',
    'mmmppmmm',
    'smpppps.',
    'smpppps.',
    '..sss...',
    '........',
]
HEAD_BACK = [
    'llllllll',
    'mmmmmmmm',
    'mmmmmmmm',
    'mmmmmmmm',
    'smmmmmms',
    'ssssssss',
    '........',
    '........',
]
HEAD_TOP = [
    'olllllll',
    'lhhhhhhl',
    'lhllllhl',
    'lllmmlll',
    'lllmmlll',
    'lhllllhl',
    'lhhhhhhl',
    'llllllll',
]
HEAD_COLORS = {'l': LIGHT, 'm': SHELL, 's': SHADOW, 'o': OUTLINE, 'v': VISOR, 'c': STRIPE, 'p': PAD, 'h': (124, 136, 126)}


def helmet_layer():
    image = [[(0, 0, 0, 0)] * 64 for _ in range(32)]

    def paste(rows, x0, y0, mirror=False):
        for y, row in enumerate(rows):
            row = row[:8].ljust(8, '.')
            if mirror:
                row = row[::-1]
            for x, c in enumerate(row):
                if c != '.':
                    image[y0 + y][x0 + x] = HEAD_COLORS[c] + (255,)

    paste(HEAD_TOP, 8, 0)
    paste(HEAD_SIDE, 0, 8)
    paste(HEAD_FRONT, 8, 8)
    paste(HEAD_SIDE, 16, 8, mirror=True)
    paste(HEAD_BACK, 24, 8)
    return image


def main():
    write_png(ASSETS / 'textures/item/tactical_helmet.png', 16, 16, pixels(HELMET, HELMET_COLORS))
    write_png(ASSETS / 'textures/item/threat_warning_implant.png', 16, 16, pixels(IMPLANT, IMPLANT_COLORS))
    write_png(ASSETS / 'textures/item/handheld_radar.png', 16, 16, pixels(RADAR, RADAR_COLORS))
    write_png(ASSETS / 'textures/entity/equipment/humanoid/tactical.png', 64, 32, helmet_layer())
    write_png(ASSETS / 'textures/gui/sprites/container/weapon_workbench/implant.png', 16, 16, silhouette(IMPLANT_FIELD))
    print('wrote helmet, implant and handheld radar textures')


if __name__ == '__main__':
    main()
