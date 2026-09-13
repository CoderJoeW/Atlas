"""Throwaway isometric rasteriser for the material-factory silhouette review.

Per docs/design/mines' silhouette-review process: render untextured geometry as one contact
sheet of distinct archetypes before any texture work starts, so a bad shape gets caught at the
box-list stage instead of after paying for art. Each archetype is a list of axis-aligned boxes in
model-space units (0-16 per axis, matching a vanilla block model's grid), painter's-algorithm
sorted back-to-front and flat-shaded like Minecraft's own block lighting
(top 100%, south/north 80%, east/west 60%).

Run: python3 build_silhouettes.py renders/silhouette-options.png
"""
import math
import sys

from PIL import Image, ImageDraw, ImageFont

SCALE = 9  # px per model unit
PAD = 24
COS30 = math.cos(math.radians(30))
SIN30 = math.sin(math.radians(30))

BASE_GRAY = (150, 150, 158)


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


def render(boxes, canvas_size=260):
    """boxes: list of (from, to) tuples in model units. Returns a PIL Image."""
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # depth-sort boxes back-to-front by centre (x+y+z) - camera looks from +x+y+z toward origin
    def centre(b):
        f, t = b
        return sum(f) + sum(t)

    ordered = sorted(boxes, key=centre)

    # auto-fit: project every corner of every box first to find bounds
    pts = []
    for f, t in boxes:
        for face, _ in box_faces(f, t):
            pts.extend(face)
    xs = [project(*p)[0] for p in pts]
    ys = [project(*p)[1] for p in pts]
    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)
    w = max_x - min_x
    h = max_y - min_y
    scale = min((canvas_size - 2 * PAD) / w, (canvas_size - 2 * PAD) / h)
    ox = (canvas_size - w * scale) / 2 - min_x * scale
    oy = (canvas_size - h * scale) / 2 - min_y * scale

    def to_screen(p):
        sx, sy = project(*p)
        return (sx * scale + ox, sy * scale + oy)

    for f, t in ordered:
        for face, factor in box_faces(f, t):
            poly = [to_screen(p) for p in face]
            draw.polygon(poly, fill=shade(BASE_GRAY, factor), outline=(30, 30, 34, 255))

    return img


ARCHETYPES = {}


def archetype(name):
    def wrap(fn):
        ARCHETYPES[name] = fn()
        return fn

    return wrap


@archetype("1. Twin-Tank Mixer")
def _():
    return [
        ([1, 0, 1], [6, 14, 6]),
        ([10, 0, 1], [15, 14, 6]),
        ([1, 0, 6], [15, 4, 11]),
        ([6, 4, 6], [10, 16, 10]),
    ]


@archetype("2. Squat Crucible")
def _():
    return [
        ([1, 0, 1], [15, 3, 15]),
        ([1, 3, 1], [2, 8, 15]),
        ([14, 3, 1], [15, 8, 15]),
        ([1, 3, 1], [15, 8, 2]),
        ([1, 3, 14], [15, 8, 15]),
        ([6, 3, 6], [10, 16, 10]),
    ]


@archetype("3. Cross Reactor")
def _():
    return [
        ([5, 2, 5], [11, 14, 11]),
        ([0, 5, 6], [5, 9, 10]),
        ([11, 5, 6], [16, 9, 10]),
        ([6, 5, 0], [10, 9, 5]),
        ([6, 5, 11], [10, 9, 16]),
    ]


@archetype("4. Vertical Stack")
def _():
    return [
        ([1, 0, 1], [15, 3, 15]),
        ([6, 3, 6], [10, 22, 10]),
    ]


@archetype("5. Split Basin")
def _():
    return [
        ([1, 0, 1], [15, 10, 15]),
        ([7, 0, 1], [9, 15, 15]),
    ]


@archetype("6. Armoured Cube")
def _():
    return [
        ([2, 2, 2], [14, 14, 14]),
        ([5, 14, 5], [7, 16, 7]),
        ([9, 14, 9], [11, 16, 11]),
    ]


@archetype("7. Anvil Press")
def _():
    return [
        ([6, 0, 6], [10, 6, 10]),
        ([2, 6, 2], [14, 10, 14]),
        ([4, 0, 4], [5, 6, 5]),
        ([11, 0, 11], [12, 6, 12]),
    ]


@archetype("8. Gantry Hopper")
def _():
    return [
        ([1, 0, 1], [15, 3, 15]),
        ([1, 0, 1], [3, 12, 3]),
        ([13, 0, 1], [15, 12, 3]),
        ([1, 0, 13], [3, 12, 15]),
        ([13, 0, 13], [15, 12, 15]),
        ([4, 8, 4], [12, 12, 12]),
        ([6, 4, 6], [10, 8, 10]),
    ]


@archetype("9. Barrel Drum")
def _():
    return [
        ([2, 1, 2], [14, 13, 14]),
        ([1, 4, 1], [15, 6, 15]),
        ([1, 9, 1], [15, 11, 15]),
        ([4, 13, 4], [12, 15, 12]),
    ]


@archetype("10. Twin Chimney")
def _():
    return [
        ([1, 0, 1], [15, 5, 15]),
        ([3, 5, 3], [7, 18, 7]),
        ([9, 5, 9], [13, 14, 13]),
    ]


def contact_sheet(path):
    cols, rows = 5, 2
    cell = 260
    label_h = 26
    sheet = Image.new("RGB", (cols * cell, rows * (cell + label_h)), (235, 235, 238))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 15)
    except OSError:
        font = ImageFont.load_default()

    for i, (name, boxes) in enumerate(ARCHETYPES.items()):
        col, row = i % cols, i // cols
        tile = render(boxes, cell)
        x0, y0 = col * cell, row * (cell + label_h)
        sheet.paste(tile, (x0, y0), tile)
        draw.rectangle([x0, y0 + cell, x0 + cell, y0 + cell + label_h], fill=(220, 220, 224))
        bbox = draw.textbbox((0, 0), name, font=font)
        tw = bbox[2] - bbox[0]
        draw.text((x0 + (cell - tw) / 2, y0 + cell + 4), name, fill=(20, 20, 24), font=font)
        draw.rectangle([x0, y0, x0 + cell - 1, y0 + cell + label_h - 1], outline=(200, 200, 204))

    sheet.save(path)
    print(f"wrote {path} ({len(ARCHETYPES)} archetypes)")


if __name__ == "__main__":
    contact_sheet(sys.argv[1] if len(sys.argv) > 1 else "renders/silhouette-options.png")
