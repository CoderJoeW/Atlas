#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Power Merger at 1024x1024.

Concept: Dark armored housing with honeycomb grid. Each face has a
single centered connector circle. Color coding matches the power
splitter: red = input, yellow = output, gold = neutral housing.
The merger is the inverse of the splitter (many inputs -> one output).

Creates 10 textures:
  5 faces (front, back, side, top, bottom) x 2 states (off, powered)
"""

import math
from PIL import Image, ImageDraw

from texture_lib import (
    new_img, add_border, lerp_color, blend_over,
    add_radial_glow, add_glow_ring, add_glowing_seam,
    save_textures,
)

S = 1024  # texture size

# ---------------------------------------------------------------------------
#  Color Palette
# ---------------------------------------------------------------------------

# Housing / armor (consistent with other Atlas blocks)
ARMOR_DARK = (38, 40, 48, 255)
ARMOR_MID = (52, 56, 66, 255)
ARMOR_LIGHT = (70, 75, 88, 255)
HEX_LINE = (48, 52, 62, 255)
EDGE_DARK = (22, 24, 30, 255)
SEAM_COLOR = (30, 33, 40, 255)
RIVET_COLOR = (100, 108, 125, 255)
FRAME_DARK = (28, 30, 38, 255)

# Connector hole
CONNECTOR_HOLE = (12, 14, 18, 255)

# Front (output) — yellow
FRONT_RING = (200, 180, 40, 255)
FRONT_BRIGHT = (255, 240, 60, 255)
FRONT_GLOW = (255, 230, 50, 255)

# Back (input) — red
BACK_RING = (200, 50, 40, 255)
BACK_BRIGHT = (255, 70, 50, 255)
BACK_GLOW = (255, 60, 40, 255)

# Side/top/bottom — gold
NEUTRAL_RING = (200, 170, 50, 255)
NEUTRAL_BRIGHT = (255, 220, 60, 255)
NEUTRAL_GLOW = (255, 200, 50, 255)

# Hex geometry
HEX_RADIUS = 28

# ---------------------------------------------------------------------------
#  Helpers
# ---------------------------------------------------------------------------


def _hex_vertices(cx, cy, radius):
    """Return 6 vertices for a flat-top hexagon."""
    verts = []
    for i in range(6):
        angle = math.radians(60 * i)
        verts.append((cx + radius * math.cos(angle),
                      cy + radius * math.sin(angle)))
    return verts


def _compute_hex_centers(size, radius, inset=12):
    """Compute hex cell center positions covering the image."""
    centers = []
    col_step = radius * 1.5
    row_step = radius * math.sqrt(3)
    cols = int((size - 2 * inset) / col_step) + 3
    rows = int((size - 2 * inset) / row_step) + 3
    for col in range(-1, cols + 1):
        for row in range(-1, rows + 1):
            cx = inset + col * col_step
            cy = inset + row * row_step + (col % 2) * (row_step / 2)
            if -radius < cx < size + radius and -radius < cy < size + radius:
                centers.append((cx, cy))
    return centers


HEX_CENTERS = _compute_hex_centers(S, HEX_RADIUS)


def draw_filled_circle(draw, cx, cy, radius, fill, outline=None):
    """Draw a filled circle."""
    draw.ellipse(
        [cx - radius, cy - radius, cx + radius, cy + radius],
        fill=fill, outline=outline
    )


def add_bevel_border(draw, x1, y1, x2, y2, light, dark, width=2):
    """Draw a beveled rectangle border (raised appearance)."""
    for i in range(width):
        draw.line([(x1 + i, y1 + i), (x2 - i, y1 + i)], fill=light)
        draw.line([(x1 + i, y1 + i), (x1 + i, y2 - i)], fill=light)
        draw.line([(x1 + i, y2 - i), (x2 - i, y2 - i)], fill=dark)
        draw.line([(x2 - i, y1 + i), (x2 - i, y2 - i)], fill=dark)


# ---------------------------------------------------------------------------
#  Hex armor base
# ---------------------------------------------------------------------------

def make_hex_armor_base():
    """Create a hex armor face — the foundation for all merger faces."""
    img = new_img(S, ARMOR_DARK)
    draw = ImageDraw.Draw(img)

    add_border(draw, S, EDGE_DARK, width=6)
    add_bevel_border(draw, 6, 6, S - 7, S - 7, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    for cx, cy in HEX_CENTERS:
        verts = _hex_vertices(cx, cy, HEX_RADIUS - 1)
        draw.polygon(verts, fill=ARMOR_DARK, outline=HEX_LINE)

    return img


def add_corner_bolts(draw, inset=30):
    """Add bolts at the four corners of a face."""
    for bx, by in [(inset, inset), (S - inset, inset),
                   (inset, S - inset), (S - inset, S - inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)


def draw_connector_port(img, draw, cx, cy, outer_r, inner_r,
                        ring_color, bright_color, glow_color,
                        powered=False):
    """Draw a color-coded circular connector port with contact pins."""
    active_ring = ring_color if not powered else bright_color

    for r in range(outer_r, inner_r, -1):
        t = (outer_r - r) / (outer_r - inner_r)
        c = lerp_color(active_ring, ARMOR_MID, t * 0.6)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    draw_filled_circle(draw, cx, cy, inner_r, CONNECTOR_HOLE,
                       outline=EDGE_DARK)

    pin_r = int(inner_r * 0.5)
    pin_size = max(4, inner_r // 12)
    pin_color = ring_color if not powered else bright_color
    for angle_deg in [0, 90, 180, 270]:
        angle = math.radians(angle_deg)
        px = int(cx + pin_r * math.cos(angle))
        py = int(cy + pin_r * math.sin(angle))
        draw_filled_circle(draw, px, py, pin_size, pin_color)

    if powered:
        add_glow_ring(img, cx, cy, outer_r,
                      outer_r + int(outer_r * 0.35), glow_color)
        add_radial_glow(img, cx, cy, inner_r, bright_color,
                        intensity=0.3)


# ---------------------------------------------------------------------------
#  Face builder
# ---------------------------------------------------------------------------

def make_face(ring, bright, glow, powered=False,
              seam_style="horizontal"):
    """Build a face with a single centered connector in a recessed panel."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    port_outer = int(S * 0.22)
    port_inner = int(S * 0.12)

    # Recessed panel
    panel_r = int(S * 0.30)
    draw.rectangle(
        [cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw,
        cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r,
        EDGE_DARK, ARMOR_LIGHT, width=4
    )

    # Connector port
    draw_connector_port(img, draw, cx, cy, port_outer, port_inner,
                        ring, bright, glow, powered=powered)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Seams
    m = 30
    if seam_style in ("horizontal", "cross"):
        if powered:
            add_glowing_seam(
                img, (m, cy), (cx - panel_r - 10, cy),
                SEAM_COLOR, glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
            add_glowing_seam(
                img, (cx + panel_r + 10, cy), (S - m, cy),
                SEAM_COLOR, glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
        else:
            draw.line([(m, cy), (cx - panel_r - 10, cy)],
                      fill=SEAM_COLOR, width=2)
            draw.line([(cx + panel_r + 10, cy), (S - m, cy)],
                      fill=SEAM_COLOR, width=2)

    if seam_style == "cross":
        if powered:
            add_glowing_seam(
                img, (cx, m), (cx, cy - panel_r - 10),
                SEAM_COLOR, glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
            add_glowing_seam(
                img, (cx, cy + panel_r + 10), (cx, S - m),
                SEAM_COLOR, glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
        else:
            draw.line([(cx, m), (cx, cy - panel_r - 10)],
                      fill=SEAM_COLOR, width=2)
            draw.line([(cx, cy + panel_r + 10), (cx, S - m)],
                      fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    textures = {}

    for powered in (False, True):
        sfx = "_powered" if powered else ""

        # Front (output) — yellow
        textures[f"power_merger_front{sfx}"] = make_face(
            FRONT_RING, FRONT_BRIGHT, FRONT_GLOW,
            powered=powered, seam_style="none"
        )
        # Back (input) — red
        textures[f"power_merger_back{sfx}"] = make_face(
            BACK_RING, BACK_BRIGHT, BACK_GLOW,
            powered=powered, seam_style="none"
        )
        # Side (input) — red with horizontal seams
        textures[f"power_merger_side{sfx}"] = make_face(
            BACK_RING, BACK_BRIGHT, BACK_GLOW,
            powered=powered, seam_style="horizontal"
        )
        # Top (input) — red with cross seams
        textures[f"power_merger_top{sfx}"] = make_face(
            BACK_RING, BACK_BRIGHT, BACK_GLOW,
            powered=powered, seam_style="cross"
        )
        # Bottom (input) — red with cross seams
        textures[f"power_merger_bottom{sfx}"] = make_face(
            BACK_RING, BACK_BRIGHT, BACK_GLOW,
            powered=powered, seam_style="cross"
        )

    save_textures(textures)


if __name__ == "__main__":
    main()
