#!/usr/bin/env python3
"""Generates the blocky 3D weapon models (DESIGN.md D29) and their shared palette texture.

Each weapon is a list of colored boxes in model units (16 = one block). The barrel points to -z
(north), the grip hangs down at the back. Every face samples one flat swatch of a 16x16 palette
texture, and boxes marked glow are fullbright (light_emission 15).

Run from the repository root:  python3 tools/weapon_models.py
Writes:  src/main/resources/assets/fusionfactions/models/item/<weapon>.json
         src/main/resources/assets/fusionfactions/textures/item/weapon_palette.png
The models are placeholders until real Blockbench models replace them.
"""
import json
import math
import struct
import zlib
from pathlib import Path

ASSETS = Path('src/main/resources/assets/fusionfactions')

# Palette: 4x4 swatches of 4x4 pixels. Index -> RGB.
PALETTE = [
    (24, 26, 30),     # 0 black
    (50, 54, 62),     # 1 dark gunmetal
    (82, 88, 100),    # 2 gunmetal
    (140, 150, 164),  # 3 light metal
    (70, 210, 250),   # 4 cyan
    (190, 245, 255),  # 5 bright cyan
    (105, 68, 40),    # 6 leather
    (72, 46, 26),     # 7 dark leather
    (230, 140, 40),   # 8 orange
    (40, 70, 90),     # 9 lens
    (60, 72, 96),     # 10 blue steel
]
BLACK, DARK, METAL, LIGHT, CYAN, BRIGHT, LEATHER, LEATHER_DARK, ORANGE, LENS, BLUE = range(11)


def swatch_uv(index):
    col, row = index % 4, index // 4
    return [col * 4 + 1, row * 4 + 1, col * 4 + 3, row * 4 + 3]


def box(frm, to, color, glow=False, rotation=None):
    faces = {face: {'uv': swatch_uv(color), 'texture': '#palette'}
             for face in ('north', 'east', 'south', 'west', 'up', 'down')}
    element = {'from': list(frm), 'to': list(to), 'faces': faces}
    if glow:
        element['light_emission'] = 15
    if rotation:
        element['rotation'] = rotation
    return element


def tilt(angle, origin):
    """Rotation around the x axis (the gun's width), e.g. for grips that lean back."""
    return {'origin': list(origin), 'axis': 'x', 'angle': angle}


def side_strips(y0, y1, z0, z1, x0, x1, color=CYAN, glow=True):
    """A thin strip on both sides of a part spanning x0..x1."""
    return [box((x0 - 0.1, y0, z0), (x0, y1, z1), color, glow), box((x1, y0, z0), (x1 + 0.1, y1, z1), color, glow)]


def grip(z0, z1, top=8.5, bottom=3.5, lean=12, color=LEATHER):
    rot = tilt(lean, (8, top, z1))
    return [box((7.2, bottom, z0), (8.8, top, z1), color, rotation=rot),
            box((7.15, bottom - 0.4, z0 - 0.1), (8.85, bottom, z1 + 0.1), BLACK, rotation=rot)]


def trigger(z):
    return [box((7.6, 7.0, z - 1.5), (8.4, 7.4, z + 1.2), BLACK),
            box((7.6, 7.4, z - 1.5), (8.4, 8.6, z - 1.1), BLACK),
            box((7.8, 7.5, z), (8.2, 8.6, z + 0.4), BLACK)]


def pistol():
    return [box((7, 9.5, 2), (9, 12, 12), METAL),
            box((7.25, 12, 3), (8.75, 12.4, 11), DARK),
            box((7.5, 10, 1), (8.5, 11, 2), BLACK),
            *side_strips(10.4, 11, 4, 10, 7, 9),
            box((7.2, 8.5, 3.5), (8.8, 9.5, 12), DARK),
            *grip(9.5, 12),
            *trigger(8)]


def auto_pistol():
    rot = tilt(12, (8, 8.5, 12))
    return [box((7, 9.5, 2), (9, 12, 12), BLUE),
            box((7.25, 12, 3), (8.75, 12.4, 11), DARK),
            box((7.25, 9.75, 0), (8.75, 11.75, 2), DARK),
            box((7.15, 10.25, 0.6), (8.85, 11.25, 1.2), CYAN, glow=True),
            *side_strips(10.4, 11, 4, 10, 7, 9, ORANGE, glow=False),
            box((7.2, 8.5, 3.5), (8.8, 9.5, 12), DARK),
            *grip(9.5, 12, color=DARK),
            box((7.35, 0.5, 9.8), (8.65, 3.6, 11.8), DARK, rotation=rot),
            *trigger(8)]


def smg():
    return [box((6.75, 8.5, 2), (9.25, 12, 14), METAL),
            box((7, 12, 3), (9, 12.6, 13), DARK),
            box((7.25, 9, -3), (8.75, 11.5, 2), DARK),
            box((7.5, 9.5, -4), (8.5, 11, -3), BLACK),
            *side_strips(10, 11, 4, 12, 6.75, 9.25),
            box((7.3, 2.5, 4.5), (8.7, 8.5, 6.5), DARK, rotation=tilt(-8, (8, 8.5, 5.5))),
            *grip(10, 12.2),
            *trigger(8.5),
            box((7.5, 9, 14), (8.5, 10.3, 19), DARK),
            box((7, 7, 19), (9, 11, 20), BLACK)]


def shotgun():
    return [box((7.1, 10.5, -8), (8.9, 12.3, 8), DARK),
            box((6.9, 10.3, -8.6), (9.1, 12.5, -7.6), BLACK),
            box((7.4, 9.0, -6), (8.6, 10.5, 8), METAL),
            box((6.9, 8.6, -4), (9.1, 10.6, 3), LEATHER),
            box((6.8, 8.8, -3), (9.2, 10.4, -2.4), CYAN, glow=True),
            box((6.8, 8.8, 1.4), (9.2, 10.4, 2.0), CYAN, glow=True),
            box((6.8, 8, 8), (9.2, 12.5, 14), METAL),
            *side_strips(9.5, 11.5, 9, 13, 6.8, 9.2),
            *trigger(11.5),
            box((7.2, 6.5, 14), (8.8, 10.5, 17), LEATHER, rotation=tilt(-8, (8, 10.5, 14))),
            box((7.2, 6.0, 17), (8.8, 11.0, 23.5), LEATHER, rotation=tilt(-8, (8, 10.5, 14))),
            box((7.0, 5.5, 23.5), (9.0, 11.2, 24.5), BLACK, rotation=tilt(-8, (8, 10.5, 14)))]


def sniper_rifle():
    parts = [box((7.5, 10.2, -16), (8.5, 11.2, 4), DARK),
             box((7.2, 9.9, -17.5), (8.8, 11.5, -15.5), BLACK),
             box((7.1, 9.5, -2), (8.9, 11.9, 5), METAL),
             box((6.9, 9, 5), (9.1, 12, 14), METAL),
             *side_strips(10, 11, 6, 13, 6.9, 9.1),
             box((7.4, 12.8, 2), (8.6, 14, 12), BLACK),
             box((7.1, 12.5, 1), (8.9, 14.3, 3), DARK),
             box((7.2, 12.6, 11), (8.8, 14.2, 13), DARK),
             box((7.3, 12.7, 0.9), (8.7, 14.1, 1), BRIGHT, glow=True),
             box((7.6, 12, 4), (8.4, 12.8, 5), DARK),
             box((7.6, 12, 9), (8.4, 12.8, 10), DARK),
             box((7.3, 6.5, 8), (8.7, 9, 11), DARK),
             *trigger(12),
             *grip(13, 15),
             box((7.2, 8, 14), (8.8, 11.8, 24), METAL),
             box((7.3, 11.8, 16), (8.7, 12.5, 21), DARK),
             box((7.2, 6.5, 19), (8.8, 8, 24), METAL),
             box((7, 6.5, 24), (9, 12, 25), BLACK)]
    for z in (-12, -8, -4):
        parts.append(box((7.4, 10.1, z), (8.6, 11.3, z + 0.6), CYAN, glow=True))
    return parts


def gatling_gun():
    parts = []
    for i in range(6):
        angle = math.radians(i * 60)
        dx, dy = 1.5 * math.cos(angle), 1.5 * math.sin(angle)
        parts.append(box((8 + dx - 0.4, 10 + dy - 0.4, -12), (8 + dx + 0.4, 10 + dy + 0.4, 6), DARK))
    parts += [box((7.3, 9.3, -12), (8.7, 10.7, 6), CYAN, glow=True),
              box((6, 8, -11), (10, 12, -10), METAL),
              box((6, 8, -4), (10, 12, -3), METAL),
              box((5.5, 7, 6), (10.5, 13, 15), METAL),
              *side_strips(9, 11, 8, 13, 5.5, 10.5),
              box((7.25, 13, 7), (8.75, 15.5, 8), BLACK),
              box((7.25, 13, 12), (8.75, 15.5, 13), BLACK),
              box((7.25, 15, 7), (8.75, 15.6, 13), BLACK),
              box((10.5, 5, 8), (13, 9, 13), DARK),
              box((10.5, 8.5, 8), (13, 9, 13), ORANGE),
              box((6, 8, 15), (10, 12, 18), DARK),
              *grip(14, 16, top=7, bottom=2.5)]
    return parts


WEAPONS = {
    'pistol': pistol,
    'auto_pistol': auto_pistol,
    'smg': smg,
    'shotgun': shotgun,
    'sniper_rifle': sniper_rifle,
    'gatling_gun': gatling_gun,
}


def bounds(elements):
    lo = [min(e['from'][i] for e in elements) for i in range(3)]
    hi = [max(e['to'][i] for e in elements) for i in range(3)]
    return lo, hi


def center_z(elements):
    """Shift the gun along its length so it's centred on z = 8. Model coordinates must stay within -16..32.
    Returns the shift."""
    lo, hi = bounds(elements)
    shift = round(8 - (lo[2] + hi[2]) / 2, 2)
    for element in elements:
        element['from'][2] = round(element['from'][2] + shift, 2)
        element['to'][2] = round(element['to'][2] + shift, 2)
        if 'rotation' in element:
            element['rotation']['origin'][2] = round(element['rotation']['origin'][2] + shift, 2)
    lo, hi = bounds(elements)
    assert lo[2] >= -16 and hi[2] <= 32, f'model too long: {lo[2]}..{hi[2]}'
    return shift


# Where the hand holds each gun, in the gun's own coordinates before centering: (y, z) of the grip.
GRIPS = {
    'pistol': (6.5, 11),
    'auto_pistol': (6.5, 11),
    'smg': (6.5, 11),
    'shotgun': (8.5, 15.5),
    'sniper_rifle': (7, 14),
    'gatling_gun': (5, 15),
}
HAND_SCALE = 0.6
FIRST_PERSON_SCALE = 0.55
HAND_YAW = -17


def display(elements, grip, shift_z):
    """Display transforms. In hand, the grip point sits where vanilla puts the held item.

    First person looks along -z with +y up, so the gun (muzzle at -z, top at +y) needs no rotation.
    In third person the weapons use the crossbow aiming pose (client.WeaponItemExtensions), which
    raises the arm so that the item space again points forward with +y up, apart from the arm's yaw.
    Both were checked with in-game screenshots. Translations are in pixels.
    """
    lo, hi = bounds(elements)
    gy, gz = grip[0] - 8, grip[1] + shift_z - 8
    s1, s3 = FIRST_PERSON_SCALE, HAND_SCALE
    first = {'rotation': [0, 0, 0], 'translation': [0, round(-s1 * gy - 1, 2), round(-s1 * gz - 2, 2)], 'scale': [s1, s1, s1]}
    # The pose turns the holding arm inward (AnimationUtils.animateCrossbowHold, 0.3 rad), so long guns
    # would point across the chest; HAND_YAW turns them back to straight ahead. The rotation turns the
    # grip point too, so the translation follows it to keep the grip in the hand.
    yaw = math.radians(HAND_YAW)
    hand = {'rotation': [0, round(math.degrees(yaw), 1), 0],
            'translation': [round(-s3 * gz * math.sin(yaw), 2), round(-s3 * gy, 2), round(-s3 * gz * math.cos(yaw), 2)],
            'scale': [s3, s3, s3]}
    # Inventory and item frames: side view, muzzle pointing left, fitted to the slot and centred.
    length = hi[2] - lo[2]
    fit = round(min(1.0, 15.0 / length), 3)
    center_y = (lo[1] + hi[1]) / 2 - 8
    side = {'rotation': [0, 90, 0], 'translation': [0, round(-fit * center_y, 2), 0], 'scale': [fit, fit, fit]}
    return {
        'thirdperson_righthand': hand,
        'thirdperson_lefthand': hand,
        'firstperson_righthand': first,
        'firstperson_lefthand': first,
        'gui': side,
        'fixed': side,
        'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
        'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [0.8, 0.8, 0.8]},
    }


def write_palette(path):
    size = 16
    rows = []
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            index = (y // 4) * 4 + (x // 4)
            r, g, b = PALETTE[index] if index < len(PALETTE) else (255, 0, 255)
            row += bytes((r, g, b, 255))
        rows.append(bytes(row))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(b''.join(rows))) + chunk(b'IEND', b'')
    path.write_bytes(png)


def main():
    (ASSETS / 'models/item').mkdir(parents=True, exist_ok=True)
    write_palette(ASSETS / 'textures/item/weapon_palette.png')
    for name, build in WEAPONS.items():
        elements = build()
        shift_z = center_z(elements)
        model = {
            'textures': {'palette': 'fusionfactions:item/weapon_palette', 'particle': 'fusionfactions:item/weapon_palette'},
            'gui_light': 'front',
            'elements': elements,
            'display': display(elements, GRIPS[name], shift_z),
        }
        out = ASSETS / f'models/item/{name}.json'
        out.write_text(json.dumps(model, indent=2) + '\n')
        lo, hi = bounds(elements)
        print(f'{name}: {len(elements)} boxes, {hi[2] - lo[2]:.1f} long')


if __name__ == '__main__':
    main()
