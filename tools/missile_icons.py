#!/usr/bin/env python3
"""Draws the missile inventory icons at 64x64 (D68, DESIGN.md §6b.9), and the interceptor rocket's icon.

Each missile flies diagonally up to the right: a shaded metal body with the type's color band (as on the 3D model),
the nose and fins in the tier color (blue / orange / red), one to three tier stripes, and the motor's flame. A round
badge in the empty lower right corner shows the type in a large symbol: a blast (explosive), a drill bit
(breaching), a lightning bolt (EMP) or a flame (incendiary).

Shapes are described in the missile's own coordinates (u along the axis from the tail, v across it), rotated onto
the diagonal and sampled 4x4 times per pixel, so edges are smooth instead of stair-stepped. A dark outline keeps the
icon readable on any slot background. Only the inventory icons change; the 3D model (tools/missile_models.py) stays.

Run from the repository root:  python3 tools/missile_icons.py
"""
import math

from workbench_textures import ASSETS, TIER_COLORS, write_png

SIZE = 64
SAMPLES = 4
OUTLINE = (22, 24, 28)

# The type's color band on the body, as on the 3D model, and the badge symbol's colors (main, highlight).
TYPES = {
    'explosive': {'band': (24, 26, 30), 'symbol': (245, 95, 40), 'core': (255, 225, 110)},
    'breaching': {'band': (130, 80, 190), 'symbol': (175, 125, 235), 'core': (230, 215, 255)},
    'emp': {'band': (70, 210, 250), 'symbol': (70, 210, 250), 'core': (215, 250, 255)},
    'incendiary': {'band': (230, 140, 40), 'symbol': (240, 120, 30), 'core': (255, 220, 90)},
}

METAL_LIGHT, METAL_DARK = (205, 212, 222), (110, 118, 132)
NOZZLE = (60, 64, 72)
FLAME_OUTER, FLAME_INNER = (255, 130, 40), (255, 235, 140)
BADGE_BG, BADGE_RING = (34, 37, 44), (150, 158, 172)

# The missile's axis runs from (x, y) = AXIS_START towards the upper right.
AXIS_START = (7.0, 57.0)
ANGLE = math.radians(45.0)


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def shade(color, v, radius):
    """Lighter towards the upper side of the body (v < 0), darker below, like light from above."""
    t = (v / radius + 1.0) / 2.0 if radius > 0 else 0.5
    return lerp(lerp(color, (255, 255, 255), 0.35), lerp(color, (0, 0, 0), 0.45), t)


def to_local(x, y):
    dx, dy = x - AXIS_START[0], AXIS_START[1] - y
    u = dx * math.cos(ANGLE) + dy * math.sin(ANGLE)
    v = -dx * math.sin(ANGLE) + dy * math.cos(ANGLE)
    # v > 0 is the upper left side of the body; flip so negative v is "up" for the shading.
    return u, -v


def missile_sample(u, v, tier, band, interceptor=False):
    """The colour of the missile at (u, v), or None. Drawn back to front: flame, fins, nozzle, body, nose."""
    tier_color = TIER_COLORS[tier] if not interceptor else (225, 50, 40)
    body_color = METAL_LIGHT if not interceptor else (236, 238, 242)
    length = 74.0 if not interceptor else 66.0
    radius = 6.0 if not interceptor else 4.5
    body_start, nose_start = 14.0, length - 18.0
    result = None
    # Flame behind the nozzle.
    if 0.0 <= u < 11.0 and abs(v) < 4.2 * (u / 11.0) ** 0.7:
        result = lerp(FLAME_OUTER, FLAME_INNER, 1.0 - abs(v) / max(0.1, 4.2 * (u / 11.0) ** 0.7))
    # Fins: a swept triangle on each side at the tail.
    fin_end = body_start + 13.0
    if body_start - 1.0 <= u < fin_end:
        span = radius + 7.5 * (fin_end - u) / (fin_end - body_start + 1.0)
        if abs(v) <= span:
            result = shade(lerp(tier_color, (0, 0, 0), 0.15), v, span)
    # Nozzle.
    if 10.0 <= u < body_start and abs(v) <= radius - 1.5:
        result = shade(NOZZLE, v, radius)
    # Body, with the type band and the tier stripes.
    if body_start <= u < nose_start and abs(v) <= radius:
        color = body_color
        middle = (body_start + nose_start) / 2.0
        if band is not None and abs(u - middle) < 4.0:
            color = band
        stripe_start = middle + 7.0
        for i in range(tier if not interceptor else 0):
            if stripe_start + i * 3.5 <= u < stripe_start + i * 3.5 + 2.0:
                color = tier_color
        result = shade(color, v, radius)
    # Nose: an ogive in the tier color.
    if nose_start <= u < length:
        t = (u - nose_start) / (length - nose_start)
        r = radius * math.sqrt(max(0.0, 1.0 - t * t))
        if abs(v) <= r:
            result = shade(tier_color, v, max(r, 0.5))
    return result


def burst(x, y):
    angle = math.atan2(y, x)
    d = math.hypot(x, y)
    r = 5.0 + 5.5 * (0.5 + 0.5 * math.cos(8 * angle)) ** 2
    return 'core' if d < 3.6 else ('symbol' if d < r else None)


def drill(x, y):
    # A bit pointing down: shank on top, a cone with spiral grooves below.
    if -10.0 <= y < -6.0 and abs(x) <= 3.0:
        return 'core'
    if -6.0 <= y <= 10.0:
        half = 6.5 * (10.0 - y) / 16.0
        if abs(x) <= half:
            return 'core' if (x + y * 0.9) % 5.0 < 1.6 else 'symbol'
    return None


def point_in_polygon(x, y, points):
    inside = False
    for i in range(len(points)):
        x1, y1 = points[i]
        x2, y2 = points[(i + 1) % len(points)]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


BOLT = [(3.0, -11.0), (-6.0, 1.5), (-0.5, 1.5), (-3.5, 11.0), (6.5, -2.5), (1.0, -2.5), (5.5, -11.0)]


def lightning(x, y):
    if point_in_polygon(x, y, BOLT):
        inner = point_in_polygon(x * 1.35 + 0.4, y * 1.15, BOLT)
        return 'core' if inner else 'symbol'
    return None


def flame_shape(x, y, scale):
    x, y = x / scale, y / scale
    # A round bottom and a curved tip that leans a little.
    if math.hypot(x, y - 3.5) <= 6.0:
        return True
    if -11.0 <= y <= 3.5:
        t = (y + 11.0) / 14.5
        centre = 1.8 * math.sin((1.0 - t) * math.pi * 0.9)
        return abs(x - centre) <= 6.0 * t ** 1.3
    return False


def flame(x, y):
    if flame_shape(x, y + 0.5, 0.55):
        return 'core'
    return 'symbol' if flame_shape(x, y, 1.0) else None


SYMBOLS = {'explosive': burst, 'breaching': drill, 'emp': lightning, 'incendiary': flame}
BADGE_CENTER = (48.0, 48.0)
BADGE_RADIUS = 14.5


def badge_sample(x, y, kind):
    bx, by = x - BADGE_CENTER[0], y - BADGE_CENTER[1]
    d = math.hypot(bx, by)
    if d > BADGE_RADIUS:
        return None
    if d > BADGE_RADIUS - 1.8:
        return BADGE_RING
    part = SYMBOLS[kind](bx, by)
    if part is None:
        return lerp(BADGE_BG, (60, 64, 74), (BADGE_RADIUS - d) / BADGE_RADIUS * 0.6)
    return TYPES[kind][part]


def render(sample):
    """Supersamples sample(x, y) -> colour or None into a SIZE x SIZE RGBA image with a dark outline."""
    image = []
    alpha = []
    for py in range(SIZE):
        row, arow = [], []
        for px in range(SIZE):
            total = [0.0, 0.0, 0.0]
            hits = 0
            for sy in range(SAMPLES):
                for sx in range(SAMPLES):
                    color = sample(px + (sx + 0.5) / SAMPLES, py + (sy + 0.5) / SAMPLES)
                    if color is not None:
                        hits += 1
                        for i in range(3):
                            total[i] += color[i]
            coverage = hits / (SAMPLES * SAMPLES)
            row.append(tuple(total[i] / hits for i in range(3)) if hits else (0.0, 0.0, 0.0))
            arow.append(coverage)
        image.append(row)
        alpha.append(arow)
    pixels = []
    for py in range(SIZE):
        out = []
        for px in range(SIZE):
            a = alpha[py][px]
            near = max((alpha[py + dy][px + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                        if 0 <= px + dx < SIZE and 0 <= py + dy < SIZE), default=0.0)
            if a >= 0.999:
                r, g, b = image[py][px]
                out.append((round(r), round(g), round(b), 255))
            elif near > 0.5:
                # Edge: the shape's colour over the outline, so the edge stays smooth and dark-rimmed.
                r, g, b = lerp(OUTLINE, image[py][px], a) if a > 0 else OUTLINE
                out.append((round(r), round(g), round(b), round(255 * max(a, 0.85))))
            elif a > 0:
                r, g, b = image[py][px]
                out.append((round(r), round(g), round(b), round(255 * a)))
            else:
                out.append((0, 0, 0, 0))
        pixels.append(out)
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
    for kind in TYPES:
        for tier in (1, 2, 3):
            write_png(ASSETS / f'item/{kind}_missile_{tier}.png', SIZE, SIZE, missile_icon(kind, tier))
    write_png(ASSETS / 'item/interceptor_rocket.png', SIZE, SIZE, interceptor_rocket_icon())
    print('wrote 12 missile icons and the interceptor rocket icon at 64x64')


if __name__ == '__main__':
    main()
