"""Second-pass iteration on archetype 1 (Twin-Tank Mixer) from the silhouette review.

Reuses the same throwaway isometric rasteriser as build_silhouettes.py - see that file's docstring
for the projection/shading approach. This one renders variants of a single archetype at a larger
size so proportion differences actually read, instead of the full ten-way overview scale.

Run: python3 build_twin_tank_iterations.py renders/twin-tank-iterations.png
"""
import math
import sys

from PIL import Image, ImageDraw, ImageFont

PAD = 28
COS30 = math.cos(math.radians(30))
SIN30 = math.sin(math.radians(30))
BASE_GRAY = (150, 150, 158)
ACCENT_GRAY = (120, 132, 134)  # pipes/collar - a hair cooler, so they read as a separate part


def shade(rgb, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in rgb)


def project(x, y, z):
    sx = (x - z) * COS30
    sy = (x + z) * SIN30 - y
    return sx, sy


def box_faces(f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    top = [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    south = [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    east = [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)]
    return [(top, 1.0), (south, 0.8), (east, 0.6)]


def render(boxes, canvas_size=340):
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    def centre(b):
        f, t, _ = b
        return sum(f) + sum(t)

    ordered = sorted(boxes, key=centre)

    pts = []
    for f, t, _ in boxes:
        for face, _ in box_faces(f, t):
            pts.extend(face)
    xs = [project(*p)[0] for p in pts]
    ys = [project(*p)[1] for p in pts]
    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)
    w, h = max_x - min_x, max_y - min_y
    scale = min((canvas_size - 2 * PAD) / w, (canvas_size - 2 * PAD) / h)
    ox = (canvas_size - w * scale) / 2 - min_x * scale
    oy = (canvas_size - h * scale) / 2 - min_y * scale

    def to_screen(p):
        sx, sy = project(*p)
        return (sx * scale + ox, sy * scale + oy)

    for f, t, colour in ordered:
        for face, factor in box_faces(f, t):
            poly = [to_screen(p) for p in face]
            draw.polygon(poly, fill=shade(colour, factor), outline=(30, 30, 34, 255))

    return img


VARIANTS = {}


def variant(name):
    def wrap(fn):
        VARIANTS[name] = fn()
        return fn

    return wrap


@variant("A. Original proportions")
def _():
    g = BASE_GRAY
    return [
        ([1, 0, 1], [6, 14, 6], g),
        ([10, 0, 1], [15, 14, 6], g),
        ([1, 0, 6], [15, 4, 11], g),
        ([6, 4, 6], [10, 16, 10], g),
    ]


@variant("B. Piped, wider gap")
def _():
    g, p = BASE_GRAY, ACCENT_GRAY
    return [
        ([1, 0, 2], [6, 15, 7], g),
        ([10, 0, 2], [15, 15, 7], g),
        ([1, 0, 9], [15, 3, 14], g),
        ([6, 3, 9], [10, 17, 13], g),
        # pipes bridging each tank's shoulder to the reactor - the visible "mixer" cue
        ([6, 11, 3], [11, 12.5, 5], p),
        ([5, 11, 3], [10, 12.5, 5], p),
    ]


@variant("C. Full collar (cable band)")
def _():
    g, c = BASE_GRAY, ACCENT_GRAY
    # a continuous collar spans the whole footprint at y4-12 so a cable meets solid geometry from
    # any side; the twin-tank read survives above/below it instead of depending on the gap.
    boxes = [
        ([0, 0, 0], [16, 4, 16], g),
        ([1, 4, 1], [15, 12, 15], c),
        ([1, 12, 1], [6, 18, 6], g),
        ([10, 12, 1], [15, 18, 6], g),
        ([6, 12, 6], [10, 15, 10], g),
    ]
    return boxes


@variant("D. Compact squat")
def _():
    g = BASE_GRAY
    return [
        ([1, 0, 1], [7, 9, 7], g),
        ([9, 0, 1], [15, 9, 7], g),
        ([1, 0, 7], [15, 3, 12], g),
        ([6, 3, 7], [10, 11, 11], g),
    ]


def contact_sheet(path):
    cols, rows = 2, 2
    cell = 340
    label_h = 30
    sheet = Image.new("RGB", (cols * cell, rows * (cell + label_h)), (235, 235, 238))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 17)
    except OSError:
        font = ImageFont.load_default()

    for i, (name, boxes) in enumerate(VARIANTS.items()):
        col, row = i % cols, i // cols
        tile = render(boxes, cell)
        x0, y0 = col * cell, row * (cell + label_h)
        sheet.paste(tile, (x0, y0), tile)
        draw.rectangle([x0, y0 + cell, x0 + cell, y0 + cell + label_h], fill=(220, 220, 224))
        bbox = draw.textbbox((0, 0), name, font=font)
        tw = bbox[2] - bbox[0]
        draw.text((x0 + (cell - tw) / 2, y0 + cell + 5), name, fill=(20, 20, 24), font=font)
        draw.rectangle([x0, y0, x0 + cell - 1, y0 + cell + label_h - 1], outline=(200, 200, 204))

    sheet.save(path)
    print(f"wrote {path} ({len(VARIANTS)} variants)")


if __name__ == "__main__":
    contact_sheet(sys.argv[1] if len(sys.argv) > 1 else "renders/twin-tank-iterations.png")
