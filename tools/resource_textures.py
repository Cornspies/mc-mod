#!/usr/bin/env python3
"""Generates the textures and ore models for the Phase 3 resources (DESIGN.md §6b.2).

- an overlay per ore (bauxite, niter, rare earth, lithium), drawn over vanilla's stone or deepslate texture: the ore
  models reference those textures, they are not copied
- the material icons: raw items, niter, ingots and dusts, and what the machines make (DESIGN.md §6b.5): silicon,
  wafers, the three chips, rocket fuel, the battery and the airframe

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
    'silicon': {'o': (35, 38, 48), 'd': (70, 76, 92), 'm': (100, 108, 128), 'l': (140, 150, 172), 'h': (190, 200, 220), 'p': (120, 130, 200)},
    'wafer': {'o': (30, 34, 48), 'd': (50, 60, 90), 'm': (70, 85, 130), 'l': (110, 130, 190), 'h': (180, 200, 240)},
    'fuel': {'o': (40, 32, 28), 'h': (200, 200, 205), 'l': (170, 170, 175), 'm': (110, 110, 118), 'd': (70, 70, 78), 'x': (150, 150, 158),
             'y': (230, 140, 40)},
    'battery': {'o': (30, 30, 34), '*': (232, 234, 238), '-': (182, 186, 194), '=': (122, 126, 134), 'H': (255, 210, 150), 'h': (226, 150, 80),
                'm': (172, 102, 46), 'd': (112, 62, 28), 'K': (80, 82, 90), 'k': (38, 40, 46), 'x': (24, 25, 29), 'y': (255, 222, 64)},
    'airframe': {'o': (60, 66, 78), 'd': (130, 140, 155), 'm': (170, 180, 195), 'l': (205, 212, 224), 'h': (240, 244, 250), 'x': (110, 118, 132)},
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

# A chip: a black die on a green board with gold pins, marked in its tier's color (blue, orange, red).
CHIP = [
    '................',
    '................',
    '...p.p.p.p.p....',
    '..oooooooooooo..',
    '..otttttttttto..',
    '.pobkkkkkkkkbop.',
    '..obkhkkkkkkbo..',
    '.pobkkkkkkkkbop.',
    '..obkkkkkkkkbo..',
    '.pobkkkkkkkkbop.',
    '..obbbbbbbttbo..',
    '..oooooooooooo..',
    '...p.p.p.p.p....',
    '................',
    '................',
    '................',
]

CHIP_TIERS = {'basic_chip': (70, 120, 255), 'advanced_chip': (240, 150, 30), 'military_chip': (230, 50, 40)}

WAFER = [
    '................',
    '................',
    '.....oooooo.....',
    '....ohhllllo....',
    '...ohlmmmmllo...',
    '..ohlmlmlmlmlo..',
    '..olmmmmmmmmmo..',
    '..olmlmlmlmlmo..',
    '..ommmmmmmmmmo..',
    '..olmlmlmlmlmo..',
    '..ommmmmmmmmdo..',
    '...odmmmmmmdo...',
    '....oddddddo....',
    '.....oooooo.....',
    '................',
    '................',
]

# A canister of solid propellant with a hazard label.
FUEL = [
    '................',
    '................',
    '.....oooooo.....',
    '....ohhhhllo....',
    '....oxxxxxxo....',
    '....ommmmmdo....',
    '....ommmmmdo....',
    '....oyyyyyyo....',
    '....oyyyyyyo....',
    '....ommmmmdo....',
    '....ommmmmdo....',
    '....ommmmmdo....',
    '....oxxxxxxo....',
    '....oddddddo....',
    '.....oooooo.....',
    '................',
]

# A battery cell: black body with a yellow lightning bolt, copper top with a '+', silver terminal.
BATTERY = [
    '................',
    '......oooo......',
    '.....o*--=o.....',
    '...oooooooooo...',
    '...oHhdhhhhmo...',
    '...ohdddhhhmo...',
    '...ommdmmmmdo...',
    '...oKkkkkkkko...',
    '...oKkkkyykko...',
    '...oKkkyykkko...',
    '...oKkyyyykko...',
    '...oKkkyykkko...',
    '...oKkyykkkko...',
    '...oKkkkkkkxo...',
    '...oooooooooo...',
    '................',
]

# A ribbed section of fuselage.
AIRFRAME = [
    '................',
    '................',
    '................',
    '................',
    '..oooooooooooo..',
    '.ohhhhhhhhhhhho.',
    '.olxllxllxllxlo.',
    '.olxllxllxllxlo.',
    '.omxmmxmmxmmxmo.',
    '.odxddxddxddxdo.',
    '..oooooooooooo..',
    '................',
    '................',
    '................',
    '................',
    '................',
]

ITEMS = {
    'silicon_dust': (DUST, 'silicon'),
    'silicon': (RAW, 'silicon'),
    'silicon_wafer': (WAFER, 'wafer'),
    'rocket_fuel': (FUEL, 'fuel'),
    'battery': (BATTERY, 'battery'),
    'airframe': (AIRFRAME, 'airframe'),
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
    for name, tier_color in CHIP_TIERS.items():
        chip = {'o': (20, 40, 24), 'b': (40, 110, 60), 'k': (28, 28, 32), 'h': (80, 80, 90), 'p': (220, 180, 70), 't': tier_color}
        write_png(ASSETS / f'textures/item/{name}.png', 16, 16, pixels(CHIP, chip))
    print('wrote resource textures and ore models')


if __name__ == '__main__':
    main()
