"""Generates the sniper scope texture: black with a round lens in the middle.

The lens is transparent with a soft dark edge, like looking through a real scope tube. The reticle
is not part of the texture; client.SniperScope draws it in GUI pixels so the lines stay sharp.

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


def main():
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

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(b''.join(rows), 9)) + chunk(b'IEND', b'')
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(png)
    print(f'wrote {OUT.relative_to(Path.cwd()) if OUT.is_relative_to(Path.cwd()) else OUT}')


if __name__ == '__main__':
    main()
