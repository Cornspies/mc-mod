#!/usr/bin/env python3
"""Generates the 2D textures for missiles (DESIGN.md §6.4).

- the missile item icons, one per type and tier, with the tier color (blue / orange / red)
- the target designator icon
- the missile launcher screen background (176x176 on a 256x256 sheet) and its empty-slot silhouettes
- the missile launcher block (top, side, bottom)
- the missile silo blocks: casing, hatch (closed and open) and controller
- the radar screen background and the textures of the radar and interceptor models (tools/machine_models.py)

Run from the repository root:  python3 tools/missile_textures.py
"""
import json

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


RADAR_W, RADAR_H = 176, 110
SCOPE = (8, 18, 88)  # x, y and size of the scope
RADAR_LIST = (92, 18, 170, 102)  # the contact list, x0, y0, x1, y1


def radar_gui():
    img = [[(0, 0, 0, 0)] * 256 for _ in range(256)]

    def put(x, y, color):
        img[y][x] = color + (255,) if len(color) == 3 else color

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                put(x, y, color)

    w, h = RADAR_W, RADAR_H
    fill(1, 1, w - 1, h - 1, BG)
    fill(2, 0, w - 3, 1, (0, 0, 0))
    fill(2, h - 1, w - 3, h, (0, 0, 0))
    fill(0, 2, 1, h - 3, (0, 0, 0))
    fill(w - 1, 2, w, h - 3, (0, 0, 0))
    for x, y in ((1, 1), (w - 2, h - 2), (w - 3, 1), (1, h - 3), (w - 2, 2), (2, h - 2)):
        put(x, y, (0, 0, 0))
    fill(2, 1, w - 3, 3, LIGHT_EDGE)
    fill(1, 2, 3, h - 3, LIGHT_EDGE)
    fill(3, h - 3, w - 2, h - 1, DARK_EDGE)
    fill(w - 3, 3, w - 1, h - 2, DARK_EDGE)
    for x, y in ((w - 3, 2), (2, h - 3)):
        put(x, y, BG)
    put(3, 3, LIGHT_EDGE)

    def sunken(x0, y0, x1, y1):
        fill(x0, y0, x1, y0 + 1, SLOT_DARK)
        fill(x0, y0, x0 + 1, y1, SLOT_DARK)
        fill(x0 + 1, y1 - 1, x1, y1, LIGHT_EDGE)
        fill(x1 - 1, y0 + 1, x1, y1, LIGHT_EDGE)
        fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, SCREEN_EDGE)
        fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, SCREEN_BG)

    # The scope: range rings and a cross on the dark screen, north up.
    sx, sy, size = SCOPE
    sunken(sx, sy, sx + size, sy + size)
    cx, cy, r = sx + size / 2, sy + size / 2, size / 2 - 3
    ring = (40, 96, 56)
    for y in range(sy + 2, sy + size - 2):
        for x in range(sx + 2, sx + size - 2):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if any(abs(d - r * f) < 0.5 for f in (1.0, 0.66, 0.33)) or (d < r and (abs(x + 0.5 - cx) < 0.5 or abs(y + 0.5 - cy) < 0.5)):
                put(x, y, ring)
    sunken(*RADAR_LIST)
    return img


# Block faces share the look of the supply crate and the workbench: a dark outline, a light bevel on the top and
# left, a shadow on the bottom and right, and sunken panels.
EDGE, SHADOW, METAL, HIGHLIGHT, SHINE = (30, 32, 38), (56, 60, 70), (82, 88, 100), (112, 120, 134), (146, 156, 170)
PANEL, HOLE, STRIPE = (48, 52, 60), (16, 17, 20), (30, 30, 32)
WARN, RED, GREEN, CYAN = (230, 170, 40), (230, 50, 40), (124, 224, 143), (70, 210, 250)
SCOPE_BG = (14, 26, 18)


def plate(color=METAL):
    """A 16x16 metal plate with an outline and a bevel."""
    px = [[color + (255,) for _ in range(16)] for _ in range(16)]
    for i in range(1, 15):
        px[1][i] = px[i][1] = HIGHLIGHT + (255,)
        px[14][i] = px[i][14] = SHADOW + (255,)
    for i in range(16):
        px[0][i] = px[15][i] = px[i][0] = px[i][15] = EDGE + (255,)
    return px


def put(px, x, y, color):
    px[y][x] = color + (255,)


def fill(px, x0, y0, x1, y1, color):
    """Fills x0..x1, y0..y1, inclusive."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, color)


def inset(px, x0, y0, x1, y1, color=PANEL):
    """A sunken panel: shadow on its top and left edge, light on its bottom and right edge."""
    fill(px, x0, y0, x1, y1, color)
    for x in range(x0, x1 + 1):
        put(px, x, y0, EDGE)
        put(px, x, y1, HIGHLIGHT)
    for y in range(y0, y1 + 1):
        put(px, x0, y, EDGE)
        put(px, x1, y, HIGHLIGHT)


def stripes(px, x0, y0, x1, y1, width=2):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, WARN if (x + y) // width % 2 == 0 else STRIPE)


def bolts(px, *positions):
    for x, y in positions:
        put(px, x, y, SHINE)
        put(px, x + 1, y + 1, SHADOW)


CORNER_BOLTS = ((2, 2), (12, 2), (2, 12), (12, 12))


def block_face(kind):
    """The missile launcher: a hatch on top, warning stripes and vents on the sides."""
    px = plate()
    if kind == 'top':
        stripes(px, 2, 2, 13, 13)
        inset(px, 3, 3, 12, 12)
        # Two doors that slide apart, with a seam and handles.
        fill(px, 4, 4, 11, 11, (66, 72, 84))
        for y in range(4, 12):
            put(px, 7, y, HOLE)
            put(px, 8, y, EDGE)
        for y in (6, 9):
            put(px, 5, y, SHINE)
            put(px, 10, y, SHINE)
    elif kind == 'side':
        stripes(px, 2, 2, 13, 3)
        inset(px, 3, 5, 12, 12)
        for y in (7, 9, 11):
            fill(px, 4, y, 11, y, HOLE)
        put(px, 13, 6, RED)
        bolts(px, (2, 12), (12, 12))
    else:
        inset(px, 4, 4, 11, 11)
        bolts(px, *CORNER_BOLTS)
    return px


def silo_face(kind):
    """The missile silo's blocks before the silo is complete (formed look: tools/silo_textures.py)."""
    px = plate()
    if kind == 'casing':
        # Four heavy plates, each with its own bevel.
        for i in range(1, 15):
            put(px, i, 7, EDGE)
            put(px, 7, i, EDGE)
            put(px, i, 8, HIGHLIGHT)
            put(px, 8, i, HIGHLIGHT)
            put(px, i, 6, SHADOW)
            put(px, 6, i, SHADOW)
        bolts(px, (3, 3), (10, 3), (3, 10), (10, 10))
    elif kind == 'hatch_top':
        stripes(px, 2, 2, 13, 13)
        inset(px, 3, 3, 12, 12, (66, 72, 84))
        for y in range(4, 12):
            put(px, 7, y, WARN if y // 2 % 2 == 0 else STRIPE)
            put(px, 8, y, WARN if y // 2 % 2 == 1 else STRIPE)
        bolts(px, (4, 4), (10, 4), (4, 10), (10, 10))
    elif kind == 'hatch_top_open':
        stripes(px, 2, 2, 13, 13)
        inset(px, 3, 3, 12, 12, HOLE)
        fill(px, 4, 4, 11, 4, (24, 26, 30))
        fill(px, 4, 4, 4, 11, (24, 26, 30))
    elif kind == 'controller':
        # A control panel: a screen, three buttons and a warning light.
        inset(px, 2, 2, 13, 8, SCOPE_BG)
        fill(px, 4, 4, 10, 4, GREEN)
        fill(px, 4, 6, 7, 6, (78, 138, 91))
        for x, color in ((3, RED), (6, (240, 150, 30)), (9, (70, 120, 255))):
            fill(px, x, 10, x + 1, 12, color)
            put(px, x, 10, tuple(min(255, c + 40) for c in color))
        put(px, 12, 11, WARN)
    return px


# The radar and the interceptor are 3D models (tools/machine_models.py). Their housings are lower than a block, so
# their side textures only use the bottom rows: the radar's housing is 8 pixels high, the interceptor's 7.
RADAR_HOUSING, INTERCEPTOR_HOUSING = 8, 7


def housing_side(height):
    """The bottom `height` rows of a side face, framed and bevelled like a plate; the rows above are unused."""
    px = plate()
    top = 16 - height
    fill(px, 0, 0, 15, top - 1, METAL)
    fill(px, 0, top, 15, top, EDGE)
    fill(px, 1, top + 1, 14, top + 1, HIGHLIGHT)
    fill(px, 1, top + 2, 1, 14, HIGHLIGHT)
    return px


def radar_side_frames():
    """The radar housing's scope screen, with a sweep that runs across it and lights up a blip as it passes."""
    frames = []
    for frame in range(8):
        px = housing_side(RADAR_HOUSING)
        inset(px, 2, 10, 11, 13, SCOPE_BG)
        sweep = 3 + frame
        for x in range(3, 11):
            behind = sweep - x
            color = GREEN if behind == 0 else (60, 130, 75) if behind == 1 else (34, 70, 42) if behind == 2 else None
            if color:
                put(px, x, 11, color)
                put(px, x, 12, color)
        if 0 <= sweep - 8 <= 2:
            put(px, 8, 11, RED)
        put(px, 13, 11, GREEN if frame % 4 < 2 else (40, 80, 50))
        put(px, 13, 12, WARN)
        frames.append(px)
    return frames


def radar_face(kind):
    px = plate()
    if kind == 'radar_top':
        # Around the mast: a turntable ring and vent slots.
        inset(px, 4, 4, 11, 11)
        for x, y in ((2, 7), (13, 7)):
            fill(px, x, y, x, y + 1, HOLE)
        for x, y in ((7, 2), (7, 13)):
            fill(px, x, y, x + 1, y, HOLE)
        bolts(px, *CORNER_BOLTS)
    elif kind == 'radar_dish':
        # The dish's front: bright metal with rings around the feed, drawn over the whole texture; the model uses
        # a 12x5 strip of it.
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + ((y - 7.5) * 2.4) ** 2) ** 0.5
                ring = int(d) % 3 == 0
                put(px, x, y, SHINE if ring else HIGHLIGHT if d < 7 else METAL)
    elif kind == 'interceptor_side':
        px = housing_side(INTERCEPTOR_HOUSING)
        stripes(px, 2, 11, 13, 13)
    elif kind == 'interceptor_top':
        stripes(px, 2, 2, 13, 3)
        stripes(px, 2, 12, 13, 13)
        inset(px, 4, 4, 11, 11)
    elif kind == 'interceptor_pod':
        # The launch pod's sides, 10x7 pixels at 3..12, 4..10.
        fill(px, 0, 0, 15, 15, METAL)
        fill(px, 3, 4, 12, 4, EDGE)
        fill(px, 3, 10, 12, 10, EDGE)
        fill(px, 3, 5, 12, 5, HIGHLIGHT)
        stripes(px, 4, 6, 11, 6, 1)
        inset(px, 5, 7, 10, 9)
        put(px, 7, 8, HOLE)
        put(px, 8, 8, HOLE)
        fill(px, 3, 5, 3, 9, HIGHLIGHT)
        fill(px, 12, 5, 12, 9, SHADOW)
    elif kind == 'interceptor_pod_top':
        # Four launch tubes in the 10x10 top at 3..12.
        fill(px, 0, 0, 15, 15, METAL)
        for i in range(3, 13):
            put(px, i, 3, HIGHLIGHT)
            put(px, 3, i, HIGHLIGHT)
            put(px, i, 12, SHADOW)
            put(px, 12, i, SHADOW)
        for x0, y0 in ((4, 4), (8, 4), (4, 8), (8, 8)):
            fill(px, x0, y0, x0 + 3, y0 + 3, SHINE)
            fill(px, x0 + 1, y0 + 1, x0 + 2, y0 + 2, HOLE)
            put(px, x0 + 3, y0 + 3, SHADOW)
    else:
        inset(px, 4, 4, 11, 11)
        bolts(px, *CORNER_BOLTS)
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
    write_png(ASSETS / 'gui/container/radar.png', 256, 256, radar_gui())
    for kind in ('radar_top', 'radar_bottom', 'radar_dish', 'interceptor_top', 'interceptor_side', 'interceptor_bottom', 'interceptor_pod',
                 'interceptor_pod_top'):
        write_png(ASSETS / f'block/{kind}.png', 16, 16, radar_face(kind))
    frames = radar_side_frames()
    write_png(ASSETS / 'block/radar_side.png', 16, 16 * len(frames), [row for frame in frames for row in frame])
    (ASSETS / 'block/radar_side.png.mcmeta').write_text(json.dumps({'animation': {'frametime': 3}}, indent=2) + '\n')
    for kind in ('casing', 'hatch_top', 'hatch_top_open', 'controller'):
        write_png(ASSETS / f'block/silo_{kind}.png', 16, 16, silo_face(kind))
    print('wrote missile textures')


if __name__ == '__main__':
    main()
