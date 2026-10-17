#!/usr/bin/env python3
"""Draws the missile inventory icons at 32x32 (D68, DESIGN.md §6b.9), and the interceptor rocket's icon.

Pixel art in the style of vanilla items: each missile flies diagonally up to the right, with a body shaded in three
flat tones and the type's color band (as on the 3D model), the nose and fins in the tier color (blue / orange / red),
one to three tier stripes and the motor's flame. Every shape gets an outline in a darker shade of its own color. A
round badge in the empty lower right corner shows the type as a small pixel symbol: a blast (explosive), a drill bit
(breaching), a lightning bolt (EMP) or a flame (incendiary).

The missile is laid out in its own coordinates (u along the axis from the tail, v across it) and sampled once per
pixel, so its edges are clean 45 degree steps. Only the inventory icons change; the 3D model (tools/missile_models.py)
stays.

Run from the repository root:  python3 tools/missile_icons.py
"""
import math

from workbench_textures import ASSETS, TIER_COLORS, write_png

SIZE = 32

TYPES = {
    'explosive': {'band': (40, 42, 48), 'symbol': (240, 96, 40), 'core': (255, 222, 104)},
    'breaching': {'band': (130, 80, 190), 'symbol': (172, 122, 232), 'core': (226, 210, 255)},
    'emp': {'band': (70, 210, 250), 'symbol': (80, 210, 250), 'core': (214, 250, 255)},
    'incendiary': {'band': (230, 140, 40), 'symbol': (238, 116, 30), 'core': (255, 218, 88)},
}

METAL = (184, 190, 200)
INTERCEPTOR_BODY = (232, 234, 238)
NOZZLE = (70, 74, 82)
FLAME_OUTER, FLAME_INNER = (255, 128, 36), (255, 228, 120)
BADGE_BG, BADGE_RING = (40, 44, 52), (150, 158, 172)

# The missile's axis runs from AXIS_START towards the upper right.
AXIS_START = (3.0, 29.0)


def tones(color):
    """Light, mid and dark shade of a color, for flat three-tone shading."""
    light = tuple(min(255, round(c + (255 - c) * 0.4)) for c in color)
    dark = tuple(round(c * 0.62) for c in color)
    return light, color, dark


def shaded(color, v, radius):
    """Upper side light, middle mid, lower side dark (light from the upper left)."""
    light, mid, dark = tones(color)
    if v < -radius * 0.34:
        return light
    if v > radius * 0.34:
        return dark
    return mid


def to_local(x, y):
    dx, dy = x - AXIS_START[0], AXIS_START[1] - y
    u = (dx + dy) / math.sqrt(2.0)
    v = (dx - dy) / math.sqrt(2.0)
    return u, v


def missile_sample(u, v, tier, band, interceptor=False):
    """The colour of the missile at (u, v), or None. Drawn back to front: flame, fins, nozzle, body, nose."""
    tier_color = TIER_COLORS[tier] if not interceptor else (225, 50, 40)
    body_color = METAL if not interceptor else INTERCEPTOR_BODY
    length = 36.0 if not interceptor else 32.0
    radius = 3.1 if not interceptor else 2.4
    body_start, nose_start = 7.0, length - 8.5
    result = None
    if 0.5 <= u < body_start - 1.0 and abs(v) <= 2.2 * (u / (body_start - 1.0)):
        result = FLAME_INNER if abs(v) < 0.8 else FLAME_OUTER
    fin_end = body_start + 6.5
    if body_start - 1.0 <= u < fin_end:
        span = radius + 3.6 * (fin_end - u) / (fin_end - body_start + 1.0)
        if abs(v) <= span:
            result = shaded(tier_color, v, span)
    if body_start - 1.5 <= u < body_start and abs(v) <= radius - 0.8:
        result = shaded(NOZZLE, v, radius)
    if body_start <= u < nose_start and abs(v) <= radius:
        color = body_color
        middle = (body_start + nose_start) / 2.0
        if band is not None and abs(u - middle) < 2.0:
            color = band
        for i in range(tier if not interceptor else 0):
            start = middle + 3.5 + i * 2.0
            if start <= u < start + 1.0:
                color = tier_color
        result = shaded(color, v, radius)
    if nose_start <= u < length:
        t = (u - nose_start) / (length - nose_start)
        r = radius * math.sqrt(max(0.0, 1.0 - t * t))
        if abs(v) <= r + 0.25:
            result = shaded(tier_color, v, max(r, 0.6))
    return result


# Badge symbols, 7x7: 'x' the symbol's color, 'y' its highlight, 'k' dark detail.
SYMBOLS = {
    'explosive': [
        'x..x..x',
        '.x.x.x.',
        '..yyy..',
        'xxyyyxx',
        '..yyy..',
        '.x.x.x.',
        'x..x..x',
    ],
    'breaching': [
        '..yyy..',
        '..yyy..',
        'xxxxxxx',
        '.xyxyx.',
        '..xyx..',
        '..xx...',
        '...x...',
    ],
    'emp': [
        '....yy.',
        '...yy..',
        '..yy...',
        '.xxxxx.',
        '...xx..',
        '..xx...',
        '.xx....',
    ],
    'incendiary': [
        '...x...',
        '..xx...',
        '..xxx.x',
        '.xxyxxx',
        'xxyyyxx',
        'xxyyyxx',
        '.xxyxx.',
    ],
}
BADGE_CENTER = (24.5, 24.5)
BADGE_RADIUS = 6.6


def badge_sample(x, y, kind):
    d = math.hypot(x - BADGE_CENTER[0], y - BADGE_CENTER[1])
    if d > BADGE_RADIUS:
        return None
    sx, sy = int(x) - 21, int(y) - 21
    if 0 <= sx < 7 and 0 <= sy < 7:
        ch = SYMBOLS[kind][sy][sx]
        if ch == 'x':
            return TYPES[kind]['symbol']
        if ch == 'y':
            return TYPES[kind]['core']
    return BADGE_RING if d > BADGE_RADIUS - 1.2 else BADGE_BG


def render(sample):
    """Samples each pixel's centre, then outlines every shape in a darker shade of the colour next to it."""
    colors = [[sample(x + 0.5, y + 0.5) for x in range(SIZE)] for y in range(SIZE)]
    pixels = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            color = colors[y][x]
            if color is not None:
                row.append(color + (255,))
                continue
            near = [colors[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                    if 0 <= x + dx < SIZE and 0 <= y + dy < SIZE and colors[y + dy][x + dx] is not None]
            if near:
                darkest = min(near, key=sum)
                row.append(tuple(round(c * 0.35) for c in darkest) + (255,))
            else:
                row.append((0, 0, 0, 0))
        pixels.append(row)
    return pixels


def missile_icon(kind, tier):
    def sample(x, y):
        badge = badge_sample(x, y, kind)
        if badge is not None:
            return badge
        u, v = to_local(x, y)
        return missile_sample(u, v, tier, TYPES[kind]['band'])
    return render(sample)


def interceptor_rocket_icon():
    def sample(x, y):
        u, v = to_local(x, y)
        return missile_sample(u, v, 1, (70, 210, 250), interceptor=True)
    return render(sample)


def main():
    for kind, rows in SYMBOLS.items():
        assert len(rows) == 7 and all(len(r) == 7 for r in rows), kind
    for kind in TYPES:
        for tier in (1, 2, 3):
            write_png(ASSETS / f'item/{kind}_missile_{tier}.png', SIZE, SIZE, missile_icon(kind, tier))
    write_png(ASSETS / 'item/interceptor_rocket.png', SIZE, SIZE, interceptor_rocket_icon())
    print('wrote 12 missile icons and the interceptor rocket icon at 32x32')


if __name__ == '__main__':
    main()
