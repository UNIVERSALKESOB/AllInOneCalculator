#!/usr/bin/env python3
"""Generates the launcher icon PNGs without any external image libraries."""
import os
import struct
import zlib

S = 1024  # supersampled canvas
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "res")

# canvas as list of float RGBA per pixel
buf = [0.0] * (S * S * 4)


def blend(x, y, r, g, b, a):
    i = (y * S + x) * 4
    ia = 1.0 - a
    buf[i] = r * a + buf[i] * ia
    buf[i + 1] = g * a + buf[i + 1] * ia
    buf[i + 2] = b * a + buf[i + 2] * ia
    buf[i + 3] = a + buf[i + 3] * ia


def rrect(x, y, w, h, rad, color, alpha=1.0):
    r, g, b = color
    x0, y0, x1, y1 = x, y, x + w, y + h
    r = min(rad, w / 2, h / 2)
    for py in range(int(max(0, y0)), int(min(S, y1 + 1))):
        for px in range(int(max(0, x0)), int(min(S, x1 + 1))):
            cx = px + 0.5
            cy = py + 0.5
            dx = max(x0 + r - cx, 0, cx - (x1 - r))
            dy = max(y0 + r - cy, 0, cy - (y1 - r))
            if dx * dx + dy * dy <= r * r:
                blend(px, py, r, g, b, alpha)


def circle(cx, cy, rad, color, alpha=1.0):
    r, g, b = color
    for py in range(int(max(0, cy - rad - 1)), int(min(S, cy + rad + 1))):
        for px in range(int(max(0, cx - rad - 1)), int(min(S, cx + rad + 1))):
            dx = px + 0.5 - cx
            dy = py + 0.5 - cy
            if dx * dx + dy * dy <= rad * rad:
                blend(px, py, r, g, b, alpha)


def gradient_rrect(x, y, w, h, rad, top, bottom):
    # draw a vertical gradient clipped to a rounded rect
    r = min(rad, w / 2, h / 2)
    x0, y0, x1, y1 = x, y, x + w, y + h
    for py in range(int(max(0, y0)), int(min(S, y1 + 1))):
        t = (py - y0) / max(1.0, (y1 - y0))
        c = tuple(top[i] + (bottom[i] - top[i]) * t for i in range(3))
        for px in range(int(max(0, x0)), int(min(S, x1 + 1))):
            cx, cy = px + 0.5, py + 0.5
            dx = max(x0 + r - cx, 0, cx - (x1 - r))
            dy = max(y0 + r - cy, 0, cy - (y1 - r))
            if dx * dx + dy * dy <= r * r:
                blend(px, py, c[0], c[1], c[2], 1.0)


TEAL = (0.176, 0.831, 0.655)
TEAL_DARK = (0.043, 0.560, 0.486)
WHITE = (1.0, 0.976, 0.965)
INK = (0.055, 0.165, 0.137)
KEY = (0.118, 0.153, 0.196)

# background tile
gradient_rrect(0, 0, S, S, int(S * 0.285), TEAL, TEAL_DARK)

# calculator body
bx, by, bw, bh = 208, 150, 608, 724
rrect(bx, by, bw, bh, 96, WHITE)

# screen
rrect(bx + 76, by + 74, bw - 152, 196, 40, INK)

# tiny teal bars on the screen ("1 2 3")
for i, w in enumerate((56, 56, 56)):
    rrect(bx + 130 + i * 92, by + 140, w, 62, 14, TEAL)

# 3x3 keypad
ks = 118
gap = 44
kx0 = bx + 76
ky0 = by + 330
for row in range(3):
    for col in range(3):
        x = kx0 + col * (ks + gap)
        y = ky0 + row * (ks + gap)
        last = row == 2 and col == 2
        rrect(x, y, ks, ks, 34, TEAL if last else KEY)

# save
os.makedirs(OUT, exist_ok=True)


def downsample(n):
    step = S // n
    if step < 1:
        step = 1
    out = bytearray(n * n * 4)
    for y in range(n):
        for x in range(n):
            r = g = b = a = 0.0
            count = 0
            for yy in range(y * step, min(S, (y + 1) * step)):
                base = yy * S
                for xx in range(x * step, min(S, (x + 1) * step)):
                    i = (base + xx) * 4
                    r += buf[i]
                    g += buf[i + 1]
                    b += buf[i + 2]
                    a += buf[i + 3]
                    count += 1
            o = (y * n + x) * 4
            if count:
                out[o] = int(max(0, min(255, r / count * 255 + 0.5)))
                out[o + 1] = int(max(0, min(255, g / count * 255 + 0.5)))
                out[o + 2] = int(max(0, min(255, b / count * 255 + 0.5)))
                out[o + 3] = int(max(0, min(255, a / count * 255 + 0.5)))
    return out


def write_png(path, size, pixels):
    raw = bytearray()
    stride = size * 4
    for y in range(size):
        raw.append(0)
        raw += pixels[y * stride:(y + 1) * stride]

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)
    print("wrote", path, size)


variants = [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]
for folder, size in variants:
    d = os.path.join(OUT, "mipmap-" + folder)
    os.makedirs(d, exist_ok=True)
    write_png(os.path.join(d, "ic_launcher.png"), size, downsample(size))
