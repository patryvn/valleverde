#!/usr/bin/env python3
"""Pure-stdlib PNG check for the player's visible humanoid colour groups."""
import struct
import sys
import zlib

def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c

def load_png(path):
    data = open(path, "rb").read()
    if data[:8] != bytes.fromhex("89504e470d0a1a0a"):
        raise SystemExit("not a PNG: " + path)
    pos = 8
    width = height = bit_depth = color_type = interlace = None
    compressed = bytearray()
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos+4])[0]
        pos += 4
        kind = data[pos:pos+4]
        pos += 4
        chunk = data[pos:pos+length]
        pos += length + 4
        if kind == b"IHDR":
            width, height, bit_depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", chunk)
        elif kind == b"IDAT":
            compressed.extend(chunk)
        elif kind == b"IEND":
            break
    channels = {2: 3, 6: 4}.get(color_type)
    if bit_depth != 8 or channels is None or interlace != 0:
        raise SystemExit("unsupported PNG format: depth=%s color=%s interlace=%s" % (bit_depth, color_type, interlace))
    stride = width * channels
    raw = zlib.decompress(compressed)
    rows = []
    previous = bytearray(stride)
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        row = bytearray(raw[offset:offset+stride])
        offset += stride
        for i in range(stride):
            left = row[i-channels] if i >= channels else 0
            up = previous[i]
            upper_left = previous[i-channels] if i >= channels else 0
            if filter_type == 1:
                row[i] = (row[i] + left) & 255
            elif filter_type == 2:
                row[i] = (row[i] + up) & 255
            elif filter_type == 3:
                row[i] = (row[i] + (left + up) // 2) & 255
            elif filter_type == 4:
                row[i] = (row[i] + paeth(left, up, upper_left)) & 255
            elif filter_type != 0:
                raise SystemExit("unsupported PNG filter %d" % filter_type)
        rows.append(row)
        previous = row
    return width, height, channels, rows

def verify(path):
    width, height, channels, rows = load_png(path)
    x0, x1 = int(width * .40), int(width * .60)
    y0, y1 = int(height * .30), int(height * .76)
    skin_top, skin_bottom = int(height * .36), int(height * .55)
    jacket_top, jacket_bottom = int(height * .42), int(height * .66)
    boots_top, boots_bottom = int(height * .63), int(height * .76)
    legs_top, legs_bottom = int(height * .55), int(height * .72)
    counts = {
        "skin": 0, "jacket": 0, "boots": 0, "dark": 0,
        "left_arm": 0, "right_arm": 0, "left_leg": 0, "right_leg": 0,
    }
    skin_y = []
    boots_y = []
    xs = []
    ys = []
    for y in range(y0, y1):
        row = rows[y]
        for x in range(x0, x1):
            i = x * channels
            r, g, b = row[i], row[i+1], row[i+2]
            if skin_top <= y < skin_bottom and 115 <= r <= 245 and 45 <= g <= 175 and 20 <= b <= 135 and r > g * 1.15 and g > b * 1.10:
                counts["skin"] += 1
                skin_y.append(y)
                xs.append(x); ys.append(y)
            if jacket_top <= y < jacket_bottom and b > r * 1.20 and b > g * 1.12 and r < 150 and g < 175 and b < 210:
                counts["jacket"] += 1
                xs.append(x); ys.append(y)
            if boots_top <= y < boots_bottom and 90 <= r <= 235 and 25 <= g <= 155 and b < 105 and r > g * 1.18:
                counts["boots"] += 1
                boots_y.append(y)
                xs.append(x); ys.append(y)
            if legs_top <= y < legs_bottom and max(r, g, b) < 58:
                counts["dark"] += 1
                xs.append(x); ys.append(y)
    # A single blue rectangle can satisfy the old torso-colour check. Require
    # independent left/right arm and leg pixels as well as the head and boots.
    limits = {
        "skin": 80, "jacket": 180, "boots": 70, "dark": 120,
        "left_arm": 80, "right_arm": 80, "left_leg": 80, "right_leg": 80,
    }
    failures = ["%s=%d<%d" % (name, counts[name], limit)
                for name, limit in limits.items() if counts[name] < limit]
    if skin_y and boots_y:
        separation = sum(boots_y) / len(boots_y) - sum(skin_y) / len(skin_y)
        if separation < 45:
            failures.append("head/feet separation %.1fpx<45px" % separation)
    else:
        separation = 0
        failures.append("head or boot colour group missing")
    bounds = (min(xs), min(ys), max(xs), max(ys)) if xs and ys else None
    if bounds is None or bounds[2] - bounds[0] < 20 or bounds[3] - bounds[1] < 80:
        failures.append("humanoid pixel bounds too small: %s" % (bounds,))
    # The lower central strip is clear of the joystick and action buttons.
    # A single flat green colour here is a visual regression of the terrain shader.
    terrain_colors = set()
    terrain_pixels = 0
    gx0, gx1 = int(width * .25), int(width * .75)
    gy0, gy1 = int(height * .74), int(height * .87)
    for y in range(gy0, min(gy1, height)):
        row = rows[y]
        for x in range(gx0, min(gx1, width)):
            i = x * channels
            r, g, b = row[i], row[i+1], row[i+2]
            if g > r * 1.15 and g > b * 1.05 and g > 45:
                terrain_pixels += 1
                terrain_colors.add((r // 4, g // 4, b // 4))
    if terrain_pixels < 5000:
        failures.append("too few visible grass pixels: %d<5000" % terrain_pixels)
    if len(terrain_colors) < 10:
        failures.append("terrain is too uniform: %d quantized green colours<10" % len(terrain_colors))
    if failures:
        raise SystemExit("Humanoid/world screenshot check failed: %s; counts=%s; bounds=%s; terrain=%d pixels/%d colours"
                         % (", ".join(failures), counts, bounds, terrain_pixels, len(terrain_colors)))
    print("Visible humanoid/world verified (%dx%d): skin=%d jacket=%d arms=%d/%d legs=%d/%d boots=%d dark-limbs=%d head-feet=%.1fpx bounds=%s terrain=%d pixels/%d colours"
          % (width, height, counts["skin"], counts["jacket"], counts["left_arm"],
             counts["right_arm"], counts["left_leg"], counts["right_leg"],
             counts["boots"], counts["dark"], separation, bounds,
             terrain_pixels, len(terrain_colors)))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify-player-screenshot.py screenshot.png")
    verify(sys.argv[1])
