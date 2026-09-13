"""Colour-language mockup for the Twin-Tank Mixer, ahead of any real Artlist generation.

Still the same throwaway box rasteriser as the other two scripts here - flat-shaded, painter's
algorithm, no textures - except boxes now carry real colour instead of one shared grey, so the
fluid-state language can be judged before spending Artlist credits on it.

Geometry is the "B. Piped, wider gap" proportions from the iteration pass: two tanks flanking a
central reactor stack, connected by pipe bridges across the gap. Each tank gets a gauge-window
box protruding slightly off its south face - offset outward rather than flush, so it never shares
a face plane with the tank body (the same z-fight trap the mines write-up warns about).

Run: python3 build_texture_mockup.py renders/texture-mockup.png
"""
import math
import sys

from PIL import Image, ImageDraw, ImageFont

PAD = 30
COS30 = math.cos(math.radians(30))
SIN30 = math.sin(math.radians(30))

BODY = (56, 58, 63)  # charcoal housing - shared language across every Atlas system
WINDOW_EMPTY = (32, 34, 38)  # dark glass, barely lifted off the housing colour
WATER_FILL = (58, 140, 214)
WATER_HIGHLIGHT = (140, 205, 245)
LAVA_FILL = (232, 96, 40)
LAVA_HIGHLIGHT = (255, 176, 64)
REACTOR_IDLE = BODY
REACTOR_ACTIVE = (255, 191, 74)  # Atlas power-system amber - this is where power is spent


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


def render(boxes, canvas_size=320):
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Depth-sort every FACE independently, not each box as a whole. A per-box average (as the
    # silhouette scripts do) dilutes a face's real depth across the box's opposite face too, which
    # is fine while every box sits at a similar z - but the reactor stack here reaches much deeper
    # in z than the tanks beside it, so a box-level sort let it paint over the water tank's window
    # even though it never geometrically overlaps it. Sorting by each face's own centroid fixes
    # that regardless of how boxes are offset from one another.
    faces = []
    for f, t, colour in boxes:
        for face, factor in box_faces(f, t):
            cx = sum(p[0] for p in face) / 4
            cy = sum(p[1] for p in face) / 4
            cz = sum(p[2] for p in face) / 4
            faces.append((cx + cy + cz, face, shade(colour, factor)))
    faces.sort(key=lambda item: item[0])

    pts = [p for _, face, _ in faces for p in face]
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

    for _, face, fill in faces:
        poly = [to_screen(p) for p in face]
        draw.polygon(poly, fill=fill, outline=(20, 20, 24, 255))

    return img


# --- shared geometry -------------------------------------------------------

def hull(west_window, east_window, reactor_colour):
    # z runs toward the camera in this projection (a larger z draws later / nearer), so the tanks -
    # meant to read in front - sit at the LARGER z range, and the reactor/manifold cluster they
    # flank sits behind them at smaller z. Getting this backwards is exactly what silently broke
    # the water window earlier: a taller box placed at bigger z than intended will paint over
    # anything in front of it, however far apart they are in x.
    boxes = [
        ([1, 0, 9], [6, 15, 14], BODY),       # west tank (water) - front
        ([10, 0, 9], [15, 15, 14], BODY),     # east tank (lava) - front
        ([1, 0, 2], [15, 3, 7], BODY),        # base manifold - behind
        ([6, 3, 2], [10, 17, 6], reactor_colour),  # reactor stack - behind
        ([2, 11, 7], [5, 12.5, 9], BODY),     # west pipe bridge, spanning the gap
        ([11, 11, 7], [14, 12.5, 9], BODY),   # east pipe bridge, spanning the gap
        # gauge windows - protrude outward off the tank's FRONT face (z14) so they never
        # share a plane with the tank body (see module docstring), and so they're always
        # the nearest thing on screen at that spot, never overpainted by the reactor.
        ([2.5, 6, 14], [4.5, 11, 14.6], west_window),
        ([11.5, 6, 14], [13.5, 11, 14.6], east_window),
    ]
    # liquid-surface highlight: top fifth of a lit window only, one shade brighter
    if west_window != WINDOW_EMPTY:
        highlight = WATER_HIGHLIGHT if west_window == WATER_FILL else LAVA_HIGHLIGHT
        boxes.append(([2.5, 9.5, 14], [4.5, 11, 14.6], highlight))
    if east_window != WINDOW_EMPTY:
        highlight = WATER_HIGHLIGHT if east_window == WATER_FILL else LAVA_HIGHLIGHT
        boxes.append(([11.5, 9.5, 14], [13.5, 11, 14.6], highlight))
    return boxes


STATES = {
    "Idle - both empty": hull(WINDOW_EMPTY, WINDOW_EMPTY, REACTOR_IDLE),
    "Water tank filling": hull(WATER_FILL, WINDOW_EMPTY, REACTOR_IDLE),
    "Lava tank filling": hull(WINDOW_EMPTY, LAVA_FILL, REACTOR_IDLE),
    "Active - producing": hull(WATER_FILL, LAVA_FILL, REACTOR_ACTIVE),
}


def contact_sheet(path):
    cols, rows = 2, 2
    cell = 320
    label_h = 30
    sheet = Image.new("RGB", (cols * cell, rows * (cell + label_h)), (24, 25, 27))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 16)
    except OSError:
        font = ImageFont.load_default()

    for i, (name, boxes) in enumerate(STATES.items()):
        col, row = i % cols, i // cols
        tile = render(boxes, cell)
        x0, y0 = col * cell, row * (cell + label_h)
        bg = Image.new("RGB", (cell, cell), (30, 31, 34))
        bg.paste(tile, (0, 0), tile)
        sheet.paste(bg, (x0, y0))
        draw.rectangle([x0, y0 + cell, x0 + cell, y0 + cell + label_h], fill=(20, 21, 23))
        bbox = draw.textbbox((0, 0), name, font=font)
        tw = bbox[2] - bbox[0]
        draw.text((x0 + (cell - tw) / 2, y0 + cell + 5), name, fill=(225, 225, 228), font=font)
        draw.rectangle([x0, y0, x0 + cell - 1, y0 + cell + label_h - 1], outline=(50, 51, 55))

    sheet.save(path)
    print(f"wrote {path} ({len(STATES)} states)")


if __name__ == "__main__":
    contact_sheet(sys.argv[1] if len(sys.argv) > 1 else "renders/texture-mockup.png")
