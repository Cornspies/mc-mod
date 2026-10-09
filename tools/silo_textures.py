#!/usr/bin/env python3
"""Generates the formed look of the missile silo (DESIGN.md §6.8): darker textures that connect across blocks.

Once a silo is complete, every casing and the controller get a block state `part` for their place in the
3x3x4 silo (missile.SiloStructure.PART). Each part has its own block model here, so the outside walls show one
continuous 3-block-wide panel, the top ring frames the hatch, and the shaft is lined inside. The controller
shows an animated control panel on its outer faces, and the formed hatch blinks.

Run from the repository root:  python3 tools/silo_textures.py
Writes:  textures/block/silo/*.png (+ .mcmeta for the animated ones) and models/block/silo/*.json
"""
import json
from pathlib import Path

from workbench_textures import write_png

ROOT = Path('src/main/resources/assets/fusionfactions')
TEXTURES = ROOT / 'textures/block/silo'
MODELS = ROOT / 'models/block/silo'
DEPTH = 3  # layers below the hatch, as in SiloStructure

BASE, SEAM, HIGH, RIVET = (44, 48, 56), (26, 28, 33), (66, 72, 84), (92, 98, 112)
WARN, STRIPE, CYAN, RED = (190, 140, 30), (24, 24, 26), (60, 190, 230), (220, 50, 40)
INNER, RAIL = (30, 33, 38), (52, 56, 64)


def canvas(width, height, color=BASE):
    return [[color + (255,) for _ in range(width)] for _ in range(height)]


def put(img, x, y, color):
    if 0 <= y < len(img) and 0 <= x < len(img[0]):
        img[y][x] = color + (255,)


def hline(img, y, x0, x1, color):
    for x in range(x0, x1 + 1):
        put(img, x, y, color)


def vline(img, x, y0, y1, color):
    for y in range(y0, y1 + 1):
        put(img, x, y, color)


def tile(img, col, row):
    return [r[col * 16:col * 16 + 16] for r in img[row * 16:row * 16 + 16]]


def wall():
    """One outside wall of the silo, 3 blocks wide and 4 deep, as a single 48x64 picture."""
    img = canvas(48, 64)
    hline(img, 0, 0, 47, HIGH)
    vline(img, 0, 0, 63, SEAM)
    vline(img, 47, 0, 63, SEAM)
    hline(img, 63, 0, 47, SEAM)
    # Warning band along the top of the silo.
    for y in range(2, 6):
        for x in range(1, 47):
            put(img, x, y, WARN if (x + y) // 3 % 2 == 0 else STRIPE)
    # Panel seams that run across block borders, so the wall reads as one piece.
    for y in (9, 30, 50):
        hline(img, y, 1, 46, SEAM)
        hline(img, y + 1, 1, 46, HIGH)
    vline(img, 23, 10, 62, SEAM)
    vline(img, 24, 10, 62, HIGH)
    for y in (9, 30, 50):
        for x in range(4, 46, 6):
            put(img, x, y + 3, RIVET)
    for x in (6, 7, 40, 41):
        put(img, x, 13, CYAN)
    return img


def top():
    """The top ring around the hatch, 3x3 blocks; the middle block is the hatch and drawn separately."""
    img = canvas(48, 48)
    hline(img, 0, 0, 47, HIGH)
    vline(img, 0, 0, 47, HIGH)
    hline(img, 47, 0, 47, SEAM)
    vline(img, 47, 0, 47, SEAM)
    # A striped frame around the hatch.
    for y in range(12, 36):
        for x in range(12, 36):
            if not (16 <= x < 32 and 16 <= y < 32):
                put(img, x, y, WARN if (x + y) // 2 % 2 == 0 else STRIPE)
    for x, y in ((4, 4), (43, 4), (4, 43), (43, 43), (23, 4), (4, 23), (43, 23), (23, 43)):
        put(img, x, y, RIVET)
        put(img, x + 1, y, RIVET)
    for i in range(6, 42, 3):
        put(img, i, 8, SEAM)
        put(img, i, 39, SEAM)
    return img


def hatch_frames():
    """The formed hatch: two heavy doors, with red lights in the corners that blink."""
    frames = []
    for on in (True, False):
        img = canvas(16, 16, (36, 40, 47))
        vline(img, 7, 0, 15, STRIPE)
        vline(img, 8, 0, 15, SEAM)
        for y in range(2, 14, 3):
            put(img, 3, y, RIVET)
            put(img, 12, y, RIVET)
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            put(img, x, y, RED if on else (70, 30, 28))
        frames.append(img)
    return frames


def hatch_open():
    img = canvas(16, 16, (10, 11, 13))
    for i in range(16):
        for j in (0, 15):
            put(img, i, j, CYAN if i % 4 == 1 else SEAM)
            put(img, j, i, CYAN if i % 4 == 1 else SEAM)
    return img


def inner():
    """The inside of the shaft: rails and a dim light strip, with no borders so the shaft looks seamless."""
    img = canvas(16, 16, INNER)
    vline(img, 3, 0, 15, RAIL)
    vline(img, 12, 0, 15, RAIL)
    for y in range(0, 16, 4):
        put(img, 7, y, (40, 120, 150))
        put(img, 8, y, (40, 120, 150))
    return img


def floor():
    """The bottom of the shaft: a launch pad."""
    img = canvas(16, 16, INNER)
    for y in range(16):
        for x in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if 30 < d < 42:
                put(img, x, y, WARN)
    hline(img, 7, 2, 13, RAIL)
    vline(img, 7, 2, 13, RAIL)
    return img


def controller_frames(base_tile):
    """The control panel on the controller's outer faces, drawn over the wall: a screen with scrolling lines and
    blinking buttons."""
    frames = []
    for frame in range(8):
        img = [row[:] for row in base_tile]
        for y in range(3, 14):
            for x in range(2, 14):
                put(img, x, y, (20, 22, 26))
        for y in range(4, 10):
            for x in range(3, 13):
                put(img, x, y, (14, 24, 18))
        for i in range(3):
            y = 4 + (i * 2 + frame) % 6
            length = (5, 8, 3)[(i + frame // 2) % 3]
            hline(img, y, 4, 3 + length, (124, 224, 143))
        for i, color in enumerate((RED, WARN, (70, 120, 255))):
            lit = (frame // 2 + i) % 3 != 0
            c = color if lit else tuple(v // 3 for v in color)
            put(img, 4 + i * 3, 11, c)
            put(img, 5 + i * 3, 11, c)
        frames.append(img)
    return frames


def write_animated(path, frames, frametime):
    stacked = [row for frame in frames for row in frame]
    write_png(path, 16, 16 * len(frames), stacked)
    path.with_suffix('.png.mcmeta').write_text(json.dumps({'animation': {'frametime': frametime}}, indent=2) + '\n')


def texture(name):
    return f'fusionfactions:block/silo/{name}'


DIRECTIONS = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}


def faces(part, controller):
    """Textures for each face of the block at a part (1 + layer * 9 + (dx + 1) * 3 + (dz + 1))."""
    layer, rest = divmod(part - 1, 9)
    x, z = rest // 3 - 1, rest % 3 - 1
    result = {}
    for name, (dx, dz) in DIRECTIONS.items():
        if x * dx + z * dz == 1:
            # The outside of the silo. Seen from outside, the viewer's right is (dz, -dx).
            col = x * dz - z * dx + 1
            result[name] = texture(f'controller_{col}_{layer}' if controller and layer < DEPTH else f'wall_{col}_{layer}')
        else:
            result[name] = texture('inner')
    if layer == 0 and (x, z) != (0, 0):
        result['up'] = texture(f'top_{x + 1}_{z + 1}')
    elif layer == DEPTH and x == 0 and z == 0:
        result['up'] = texture('floor')
    else:
        result['up'] = texture('inner')
    result['down'] = texture('bottom')
    result['particle'] = texture('wall_1_1')
    return result


def write_model(name, textures):
    MODELS.mkdir(parents=True, exist_ok=True)
    (MODELS / f'{name}.json').write_text(json.dumps({'parent': 'minecraft:block/cube', 'textures': textures}, indent=2) + '\n')


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    walls = wall()
    for col in range(3):
        for layer in range(DEPTH + 1):
            write_png(TEXTURES / f'wall_{col}_{layer}.png', 16, 16, tile(walls, col, layer))
            if layer < DEPTH:
                write_animated(TEXTURES / f'controller_{col}_{layer}.png', controller_frames(tile(walls, col, layer)), 4)
    tops = top()
    for i in range(3):
        for j in range(3):
            if (i, j) != (1, 1):
                write_png(TEXTURES / f'top_{i}_{j}.png', 16, 16, tile(tops, i, j))
    write_animated(TEXTURES / 'hatch.png', hatch_frames(), 10)
    write_png(TEXTURES / 'hatch_open.png', 16, 16, hatch_open())
    write_png(TEXTURES / 'inner.png', 16, 16, inner())
    write_png(TEXTURES / 'floor.png', 16, 16, floor())
    write_png(TEXTURES / 'bottom.png', 16, 16, canvas(16, 16))

    for part in range(1, 37):
        write_model(f'casing_{part}', faces(part, False))
        write_model(f'controller_{part}', faces(part, True))
    for name, top_texture in (('hatch', 'hatch'), ('hatch_open', 'hatch_open')):
        sides = texture('inner')
        write_model(name, {'north': sides, 'south': sides, 'east': sides, 'west': sides, 'up': texture(top_texture),
                           'down': texture('bottom'), 'particle': texture('wall_1_1')})
    print('wrote formed silo textures and models')


if __name__ == '__main__':
    main()
