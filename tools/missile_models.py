#!/usr/bin/env python3
"""Generates the 3D missile models (DESIGN.md §6.4), one per type and tier.

A missile stands upright, nose up (+y), on the weapon palette (tools/weapon_models.py): a light metal body,
a dark band, fins at the tail, a glowing exhaust, and the nose and a glowing band in the tier color
(blue / orange / red). The item shows this model in hand and as the flying missile; inventories show the flat
icon from tools/missile_textures.py (client.datagen.ModModelProvider picks by display context).

Run from the repository root:  python3 tools/missile_models.py
Writes:  src/main/resources/assets/fusionfactions/models/item/<type>_missile_<tier>_3d.json
"""
from weapon_models import ASSETS, BLACK, CYAN, DARK, LIGHT, ORANGE, TIER_COLORS, box, write_model, write_palette

# The band in the middle shows the type: dark for explosive, violet for breaching, glowing cyan for EMP and
# glowing orange for incendiary missiles. The nose shows the tier.
TYPES = {'explosive': (DARK, False), 'breaching': (14, False), 'emp': (CYAN, True), 'incendiary': (ORANGE, True)}


def missile(kind, tier):
    c = TIER_COLORS[tier]
    band, glow = TYPES[kind]
    fins = [box((4.5, -4, 7.75), (6.5, 1, 8.25), DARK), box((9.5, -4, 7.75), (11.5, 1, 8.25), DARK),
            box((7.75, -4, 4.5), (8.25, 1, 6.5), DARK), box((7.75, -4, 9.5), (8.25, 1, 11.5), DARK)]
    return [box((6.5, -4, 6.5), (9.5, 18, 9.5), LIGHT),
            box((6.4, 5, 6.4), (9.6, 8, 9.6), band, glow=glow),
            box((6.4, 15, 6.4), (9.6, 16, 9.6), c, glow=True),
            box((6.9, 18, 6.9), (9.1, 21, 9.1), c),
            box((7.4, 21, 7.4), (8.6, 23, 8.6), c),
            box((7.8, 23, 7.8), (8.2, 24, 8.2), DARK),
            *fins,
            box((7, -5, 7), (9, -4, 9), BLACK),
            box((7.3, -5.2, 7.3), (8.7, -5, 8.7), ORANGE, glow=True)]


# Held like a short tube across the hand; on the ground it lies flat. The flying missile uses no
# transform (display context "none") and is placed by the entity renderer.
DISPLAY = {
    'thirdperson_righthand': {'rotation': [0, 0, -50], 'translation': [0, 3, 1], 'scale': [0.55, 0.55, 0.55]},
    'thirdperson_lefthand': {'rotation': [0, 0, 50], 'translation': [0, 3, 1], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [0, 0, -40], 'translation': [1, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'firstperson_lefthand': {'rotation': [0, 0, 40], 'translation': [1, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'head': {'translation': [0, 8, 0], 'scale': [0.6, 0.6, 0.6]},
}


def main():
    # The palette gained the violet swatch for breaching missiles.
    write_palette(ASSETS / 'textures/item/weapon_palette.png')
    for kind in TYPES:
        for tier in (1, 2, 3):
            write_model(ASSETS / f'models/item/{kind}_missile_{tier}_3d.json', missile(kind, tier), DISPLAY)
    print('wrote missile models')


if __name__ == '__main__':
    main()
