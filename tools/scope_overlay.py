"""Generates the scope and sight textures (DESIGN.md §4.2, D40, D41).

- sniper_scope.png: black with a round lens in the middle. The lens is transparent with a soft dark edge,
  like looking through a real scope tube. Also used by the 2x optic. The sniper's reticle is not part of
  it; client.WeaponAim draws it in GUI pixels so the lines stay sharp.
- sight_red_dot.png, sight_holo.png, sight_chevron.png: the glowing reticles of the three sight tiers
  (red dot; red ring with a dot; amber chevron), drawn by client.WeaponAim in the middle of the screen.

Run from the project root:  python3 tools/scope_overlay.py
"""
import math
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / 'src/main/resources/assets/fusionfactions/textures/gui/sniper_scope.png'
SIZE = 512
SAMPLES = 4  # per axis, for a smooth lens edge

# Alpha by distance from the center, as a share of the texture's half width.
SHADE_START = 0.62   # the lens starts darkening here
EDGE_START = 0.88    # the tube's shadow ramps up from here
EDGE_END = 0.985     # fully black from here outwards
SHADE_MAX = 0.28     # darkness where the tube's shadow begins


def smoothstep(edge0, edge1, x):
    t = min(1.0, max(0.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3 - 2 * t)


def alpha_at(r):
    if r >= EDGE_END:
        return 1.0
    shade = SHADE_MAX * smoothstep(SHADE_START, EDGE_START, r)
    return shade + (1.0 - shade) * smoothstep(EDGE_START, EDGE_END, r)


RED = (255, 48, 40)
AMBER = (255, 182, 40)


def segment_distance(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def glow(distance, core, sigma, strength=0.6):
    """Alpha of a glowing line or dot: solid out to core, then a soft halo."""
    if distance <= core:
        return 1.0
    return strength * math.exp(-((distance - core) ** 2) / (2 * sigma * sigma))


def reticle(path, size, color, alpha_of):
    """Writes a reticle texture; alpha_of(x, y) gives the coverage at a point (pixel units, 0..size)."""
    rows = []
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            total = 0.0
            for sy in range(SAMPLES):
                for sx in range(SAMPLES):
                    total += alpha_of(x + (sx + 0.5) / SAMPLES, y + (sy + 0.5) / SAMPLES)
            a = min(1.0, total / SAMPLES ** 2)
            row += bytes((*color, round(255 * a)))
        rows.append(bytes(row))
    write_png(path, size, rows)


def write_png(path, size, rows):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(b''.join(rows), 9)) + chunk(b'IEND', b'')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def reticles():
    gui = OUT.parent
    c = 32
    reticle(gui / 'sight_red_dot.png', 64, RED, lambda x, y: glow(math.hypot(x - c, y - c), 3.0, 5.0))

    c = 64

    def holo(x, y):
        r = math.hypot(x - c, y - c)
        ring = glow(abs(r - 46), 1.6, 2.5, 0.5)
        dot = glow(r, 2.4, 3.0, 0.5)
        # Short ticks just outside the ring, top, bottom, left and right.
        ticks = max(glow(segment_distance(x, y, c, c - 50, c, c - 58), 1.4, 2.0, 0.4),
                    glow(segment_distance(x, y, c, c + 50, c, c + 58), 1.4, 2.0, 0.4),
                    glow(segment_distance(x, y, c - 50, c, c - 58, c), 1.4, 2.0, 0.4),
                    glow(segment_distance(x, y, c + 50, c, c + 58, c), 1.4, 2.0, 0.4))
        return max(ring, dot, ticks)
    reticle(gui / 'sight_holo.png', 128, RED, holo)

    def chevron(x, y):
        # Apex on the centre: that's where the shot goes.
        arms = min(segment_distance(x, y, c, c, c - 22, c + 20), segment_distance(x, y, c, c, c + 22, c + 20))
        return glow(arms, 2.0, 2.5, 0.5)
    reticle(gui / 'sight_chevron.png', 128, AMBER, chevron)


def main():
    reticles()
    half = SIZE / 2
    rows = []
    for y in range(SIZE):
        row = bytearray([0])
        for x in range(SIZE):
            total = 0.0
            for sy in range(SAMPLES):
                for sx in range(SAMPLES):
                    dx = (x + (sx + 0.5) / SAMPLES - half) / half
                    dy = (y + (sy + 0.5) / SAMPLES - half) / half
                    total += alpha_at(math.hypot(dx, dy))
            row += bytes((0, 0, 0, round(255 * total / SAMPLES ** 2)))
        rows.append(bytes(row))

    write_png(OUT, SIZE, rows)
    print(f'wrote {OUT.relative_to(Path.cwd()) if OUT.is_relative_to(Path.cwd()) else OUT}')


if __name__ == '__main__':
    main()
