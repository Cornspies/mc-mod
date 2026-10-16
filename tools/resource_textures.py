#!/usr/bin/env python3
"""Generates the textures and ore models for the Phase 3 resources (DESIGN.md §6b.2).

- an overlay per ore (bauxite, niter, rare earth, lithium), drawn over vanilla's stone or deepslate texture: the ore
  models reference those textures, they are not copied
- the material icons: raw items, niter, ingots and dusts

Run from the repository root:  python3 tools/resource_textures.py
"""
import json
from pathlib import Path

from workbench_textures import write_png

ASSETS = Path('src/main/resources/assets/fusionfactions')

# Colors per material: outline, dark, mid, light, highlight, and an accent.
PALETTES = {
    'bauxite': {'o': (70, 30, 18), 'd': (120, 52, 30), 'm': (160, 78, 45), 'l': (195, 110, 70), 'h': (225, 150, 105), 'p': (95, 40, 25)},
    'niter': {'o': (70, 70, 68), 'd': (168, 168, 162), 'm': (204, 204, 198), 'l': (234, 234, 228), 'h': (255, 255, 250), 'p': (190, 200, 210)},
    'rare_earth': {'o': (90, 62, 20), 'd': (160, 115, 35), 'm': (205, 155, 45), 'l': (235, 195, 80), 'h': (255, 230, 140), 'p': (150, 80, 190)},
    'lithium': {'o': (105, 76, 100), 'd': (175, 130, 165), 'm': (210, 165, 200), 'l': (235, 200, 228), 'h': (252, 232, 248), 'p': (160, 210, 230)},
    'aluminum': {'o': (60, 66, 78), 'd': (130, 140, 155), 'm': (170, 180, 195), 'l': (205, 212, 224), 'h': (240, 244, 250), 'p': (150, 160, 175)},
}

# 16x16 pictures; '.' is transparent, the other letters are keys into a palette.
ORE_OVERLAYS = {
    # Rounded red-brown nodules.
    'bauxite': [
        '................',
        '..oo.......oo...',
        '.omlo.....omlo..',
        '.odmo....odmmlo.',
        '..oo.....oddmo..',
        '..........ooo...',
        '....ooo.........',
        '...omllo....oo..',
        '...odmmo...omlo.',
        '...oddmo...odmo.',
        '....ooo.....oo..',
        '.oo.............',
        'omlo.....ooo....',
        'odmo....omllo...',
        '.oo.....oddmo...',
        '.........ooo....',
    ],
    # White crystal flecks.
    'niter': [
        '................',
        '...h........h...',
        '..hlo......hlo..',
        '..lmo...h..lmo..',
        '...o...hlo..o...',
        '.......lmo......',
        '........o.......',
        '.h...........h..',
        'hlo.....h...hlo.',
        'lmo....hlo..lmo.',
        '.o.....lmo...o..',
        '........o.......',
        '...h.........h..',
        '..hlo...h...hlo.',
        '..lmo..hlo..lmo.',
        '...o....o....o..',
    ],
    # Honey-yellow crystals with violet specks.
    'rare_earth': [
        '................',
        '..ooo......oo...',
        '.ohlmo....ohlo..',
        '.olpmo....olmo..',
        '.ommdo.....oo...',
        '..ooo...........',
        '.......oooo.....',
        '......ohllmo....',
        '......olmpmo....',
        '......ommmdo....',
        '.......oooo..oo.',
        '.oo.........ohlo',
        'ohlo........ommo',
        'ommo...oo....oo.',
        '.oo...ohlo......',
        '.......oo.......',
    ],
    # Pale pink crystal blades.
    'lithium': [
        '................',
        '.........oo.....',
        '........ohlo....',
        '.......ohlo.....',
        '..oo...olmo.....',
        '.ohlo...oo......',
        '.olmo.........o.',
        '..oo.........olo',
        '.............omo',
        '.....oo.......o.',
        '....ohlo........',
        '...ohlmo....oo..',
        '...olmo....ohlo.',
        '....oo....ohlmo.',
        '..........olmo..',
        '...........oo...',
    ],
}

RAW = [
    '................',
    '................',
    '................',
    '.....ooooo......',
    '....ohhlllo.....',
    '...ohllmmlmo....',
    '..ohllmmpmmdo...',
    '..olmmmmdmmdo...',
    '.olmmdmmmmmddo..',
    '.olmpmmmdmmddo..',
    '.ommmdmmmmdddpo.',
    '..odmmmmmddddo..',
    '..oddddddddoo...',
    '...ooddddoo.....',
    '.....oooo.......',
    '................',
]

INGOT = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '.....ooooooooo..',
    '....ohhhhhhhlo..',
    '...ohlllllllmdo.',
    '..ohlllllllmddo.',
    '..oommmmmmmmdo..',
    '..ommmmmmmmmdo..',
    '..oddddddddddo..',
    '...oooooooooo...',
    '................',
    '................',
    '................',
]

DUST = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.......oo.......',
    '......ohlo......',
    '.....ohllmo.....',
    '....ohlmlmmo....',
    '...ohllmmlmdo...',
    '..ohlmmlmpmddo..',
    '.ohlmmmmmmdmddo.',
    '.odddddddddddo..',
    '..ooooooooooo...',
    '................',
]

CRYSTALS = [
    '................',
    '................',
    '.........o......',
    '........ohlo....',
    '....o...olmo....',
    '...ohlo.olmo.o..',
    '...olmo.olmdohlo',
    '..o.lmdoolmdolmo',
    '.ohlolmdolmdolmo',
    '.olmolmdolmdolmo',
    '.olmdomdolmdolmd',
    '.oommddoddddddd.',
    '..oddddddddddo..',
    '...oooooooooo...',
    '................',
    '................',
]

ITEMS = {
    'raw_bauxite': (RAW, 'bauxite'),
    'raw_rare_earth': (RAW, 'rare_earth'),
    'raw_lithium': (RAW, 'lithium'),
    'niter': (CRYSTALS, 'niter'),
    'aluminum_ingot': (INGOT, 'aluminum'),
    'lithium_ingot': (INGOT, 'lithium'),
    'aluminum_dust': (DUST, 'aluminum'),
    'rare_earth_dust': (DUST, 'rare_earth'),
    'lithium_dust': (DUST, 'lithium'),
}


def pixels(rows, palette):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    return [[(0, 0, 0, 0) if c == '.' else palette[c] + (255,) for c in row] for row in rows]


def ore_model(base, overlay):
    def faces(texture):
        return {face: {'texture': texture, 'cullface': face} for face in ('down', 'up', 'north', 'south', 'west', 'east')}
    return {
        'parent': 'minecraft:block/block',
        'textures': {'particle': base, 'base': base, 'overlay': overlay},
        'elements': [
            {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces('#base')},
            {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces('#overlay')},
        ],
    }


def main():
    for ore, rows in ORE_OVERLAYS.items():
        write_png(ASSETS / f'textures/block/{ore}_ore_overlay.png', 16, 16, pixels(rows, PALETTES[ore]))
        overlay = f'fusionfactions:block/{ore}_ore_overlay'
        for prefix, base in (('', 'minecraft:block/stone'), ('deepslate_', 'minecraft:block/deepslate')):
            path = ASSETS / f'models/block/{prefix}{ore}_ore.json'
            path.write_text(json.dumps(ore_model(base, overlay), indent=2) + '\n')
    for name, (rows, palette) in ITEMS.items():
        write_png(ASSETS / f'textures/item/{name}.png', 16, 16, pixels(rows, PALETTES[palette]))
    print('wrote resource textures and ore models')


if __name__ == '__main__':
    main()
