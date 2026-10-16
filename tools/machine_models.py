#!/usr/bin/env python3
"""Generates the 3D models of the radar, the interceptor and the interceptor's rocket (DESIGN.md §6.9).

- radar: a housing with a scope screen (the block), and a dish on a mast that the client turns while the radar is
  online (client.RadarDishRenderer draws block/radar_dish on its own). block/radar_inventory has both, for the item.
- interceptor: a housing with a launch pod of four tubes on top.
- interceptor rocket: a small rocket, drawn by client.InterceptorRocketRenderer.

Block models can only use textures from the block atlas, so the parts are drawn with a copy of the weapon palette
(tools/weapon_models.py) at textures/block/machine_palette.png. The textured faces come from
tools/missile_textures.py.

Run from the repository root:  python3 tools/machine_models.py
"""
import json

from weapon_models import ASSETS, CYAN, DARK, LIGHT, METAL, ORANGE, box, write_palette

RED = 13
FACES = ('north', 'east', 'south', 'west', 'up', 'down')
SIDES = ('north', 'east', 'south', 'west')


def tex(name):
    return f'fusionfactions:block/{name}'


def glowing(elements):
    # Glowing parts are drawn at full brightness, without the side shading.
    for element in elements:
        if element.get('light_emission'):
            for face in element['faces'].values():
                face['shade'] = False
    return elements


def textured(frm, to, faces, cull=()):
    """A box whose faces are (texture slot, uv) pairs; faces left out use the palette's metal."""
    element = box(frm, to, METAL)
    for face, (slot, uv) in faces.items():
        element['faces'][face] = {'uv': list(uv), 'texture': f'#{slot}'}
    for face in cull:
        element['faces'][face]['cullface'] = face
    return element


def rotated(elements, angle, origin, axis='x'):
    for element in elements:
        element['rotation'] = {'origin': list(origin), 'axis': axis, 'angle': angle}
    return elements


# The radar housing is 8 pixels high; the mast stands on it.
def radar_base():
    side = ('side', (0, 8, 16, 16))
    return [textured((0, 0, 0), (16, 8, 16), {**{s: side for s in SIDES}, 'up': ('top', (0, 0, 16, 16)), 'down': ('bottom', (0, 0, 16, 16))},
                     cull=(*SIDES, 'down')),
            box((5, 8, 5), (11, 9, 11), DARK),
            box((6.5, 9, 6.5), (9.5, 10.5, 9.5), METAL)]


# The dish turns around the block's vertical middle line. It looks north (-z) when not turned, tilted up.
def radar_dish():
    turntable = [box((6, 10.5, 6), (10, 11.5, 10), DARK),
                 box((4.5, 11, 7.25), (5.5, 13.5, 8.75), METAL),
                 box((10.5, 11, 7.25), (11.5, 13.5, 8.75), METAL),
                 box((5.5, 11.5, 7.5), (10.5, 12, 8.5), DARK)]
    dish = [textured((2, 11, 8), (14, 16, 9), {'north': ('dish', (2, 5, 14, 10))}),
            box((2, 15.5, 7), (14, 16, 8), LIGHT),
            box((2, 11, 7), (14, 11.5, 8), LIGHT),
            box((1.5, 11, 7), (2, 16, 9), LIGHT),
            box((14, 11, 7), (14.5, 16, 9), LIGHT),
            box((7.6, 13.2, 4), (8.4, 13.8, 7), DARK),
            box((7.2, 12.8, 3), (8.8, 14.2, 4), METAL),
            box((7.5, 13.1, 2.8), (8.5, 13.9, 3), RED, glow=True)]
    return turntable + rotated(dish, -22.5, (8, 12, 8.5))


# The interceptor housing is 7 pixels high, with a pod of four launch tubes on a short pedestal.
def interceptor():
    side = ('side', (0, 9, 16, 16))
    pod_side = ('pod', (3, 4, 13, 11))
    rails = [box((x, 9, z), (x + 0.4, 14, z + 0.4), CYAN, glow=True) for x in (2.8, 12.8) for z in (2.8, 12.8)]
    return [textured((0, 0, 0), (16, 7, 16), {**{s: side for s in SIDES}, 'up': ('top', (0, 0, 16, 16)), 'down': ('bottom', (0, 0, 16, 16))},
                     cull=(*SIDES, 'down')),
            box((5, 7, 5), (11, 8, 11), DARK),
            textured((3, 8, 3), (13, 15, 13), {**{s: pod_side for s in SIDES}, 'up': ('pod_top', (3, 3, 13, 13))}),
            *rails]


# Nose up (+y), centred on x and z 8, 12 pixels long; the renderer turns it along its flight.
def rocket():
    fins = [box((6.2, 1, 7.8), (7.2, 3.5, 8.2), DARK), box((8.8, 1, 7.8), (9.8, 3.5, 8.2), DARK),
            box((7.8, 1, 6.2), (8.2, 3.5, 7.2), DARK), box((7.8, 1, 8.8), (8.2, 3.5, 9.8), DARK)]
    return [box((7.4, 0, 7.4), (8.6, 1, 8.6), ORANGE, glow=True),
            box((7.2, 1, 7.2), (8.8, 9, 8.8), LIGHT),
            box((7.1, 6, 7.1), (8.9, 7, 8.9), CYAN, glow=True),
            box((7.4, 9, 7.4), (8.6, 11, 8.6), RED),
            box((7.7, 11, 7.7), (8.3, 12, 8.3), RED, glow=True),
            *fins]


def write(path, elements, textures, parent=None):
    model = {'textures': {'palette': tex('machine_palette'), **textures}, 'elements': glowing(elements)}
    if parent:
        model = {'parent': parent, **model}
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(model, indent=2) + '\n')


def main():
    write_palette(ASSETS / 'textures/block/machine_palette.png')
    models = ASSETS / 'models'
    radar = {'side': tex('radar_side'), 'top': tex('radar_top'), 'bottom': tex('radar_bottom'), 'dish': tex('radar_dish'),
             'particle': tex('radar_top')}
    write(models / 'block/radar.json', radar_base(), radar, 'minecraft:block/block')
    write(models / 'block/radar_dish.json', radar_dish(), radar)
    write(models / 'block/radar_inventory.json', radar_base() + radar_dish(), radar, 'minecraft:block/block')
    write(models / 'block/interceptor.json', interceptor(),
          {'side': tex('interceptor_side'), 'top': tex('interceptor_top'), 'bottom': tex('interceptor_bottom'), 'pod': tex('interceptor_pod'),
           'pod_top': tex('interceptor_pod_top'), 'particle': tex('interceptor_top')}, 'minecraft:block/block')
    write(models / 'entity/interceptor_rocket.json', rocket(), {'particle': tex('machine_palette')})
    print('wrote radar, interceptor and rocket models')


if __name__ == '__main__':
    main()
