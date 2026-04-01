#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Power Splitter at 1024x1024.

Concept: Dark armored housing with honeycomb grid. The splitter's
function is communicated through color-coded connector ports:
  - Red = input (one large port on the front)
  - Yellow = output (two ports on the back)
Side and cap faces show one red circle on the left (input) splitting
into two yellow circles on the right (output) connected by lines.
Powered variants add glow effects in matching colors.

Creates 8 textures:
  Front (2): power_splitter_front, _front_powered
  Back  (2): power_splitter_back, _back_powered
  Side  (2): power_splitter_side, _side_powered
  Cap   (2): power_splitter_cap, _cap_powered
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
FRAME_MID = (58, 62, 74, 255)
FRAME_LIGHT = (80, 86, 100, 255)

# Input colors — red
RED = (200, 50, 40, 255)
RED_RING = (180, 45, 35, 255)
RED_BRIGHT = (255, 70, 50, 255)
RED_GLOW = (255, 60, 40, 255)

# Output colors — yellow
YELLOW = (220, 200, 40, 255)
YELLOW_RING = (200, 180, 40, 255)
YELLOW_BRIGHT = (255, 240, 60, 255)
YELLOW_GLOW = (255, 230, 50, 255)

# Connector
CONNECTOR_HOLE = (12, 14, 18, 255)

# Side/cap neutral color — gold/amber
GOLD_RING = (200, 170, 50, 255)
GOLD_BRIGHT = (255, 220, 60, 255)
GOLD_GLOW = (255, 200, 50, 255)

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


def draw_thick_circle(draw, cx, cy, radius, color, width):
    """Draw a circle outline with a given line width."""
    for w in range(width):
        r = radius - w
        if r > 0:
            draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                         outline=color)


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
    """Create a hex armor face — the foundation for all splitter faces."""
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

    # Connector ring gradient
    for r in range(outer_r, inner_r, -1):
        t = (outer_r - r) / (outer_r - inner_r)
        c = lerp_color(active_ring, ARMOR_MID, t * 0.6)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Connector hole
    draw_filled_circle(draw, cx, cy, inner_r, CONNECTOR_HOLE,
                       outline=EDGE_DARK)

    # Contact pins
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
#  FRONT FACE — single red input connector
# ---------------------------------------------------------------------------

def make_front(powered=False):
    """Front face: single large red input connector port."""
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

    # Red input connector
    draw_connector_port(img, draw, cx, cy, port_outer, port_inner,
                        RED_RING, RED_BRIGHT, RED_GLOW,
                        powered=powered)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  BACK FACE — single yellow output connector
# ---------------------------------------------------------------------------

def make_back(powered=False):
    """Back face: single large yellow output connector port."""
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

    # Yellow output connector
    draw_connector_port(img, draw, cx, cy, port_outer, port_inner,
                        YELLOW_RING, YELLOW_BRIGHT, YELLOW_GLOW,
                        powered=powered)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  SIDE FACE — single gold connector circle
# ---------------------------------------------------------------------------

def make_side(powered=False):
    """Side face: hex armor with a single centered gold connector."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    port_outer = int(S * 0.18)
    port_inner = int(S * 0.10)

    # Recessed panel
    panel_r = int(S * 0.26)
    draw.rectangle(
        [cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw,
        cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r,
        EDGE_DARK, ARMOR_LIGHT, width=4
    )

    # Gold connector
    draw_connector_port(img, draw, cx, cy, port_outer, port_inner,
                        GOLD_RING, GOLD_BRIGHT, GOLD_GLOW,
                        powered=powered)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Horizontal seam
    seam_margin = 30
    if powered:
        add_glowing_seam(
            img, (seam_margin, cy), (cx - panel_r - 10, cy),
            SEAM_COLOR, GOLD_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
        add_glowing_seam(
            img, (cx + panel_r + 10, cy), (S - seam_margin, cy),
            SEAM_COLOR, GOLD_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
    else:
        draw.line([(seam_margin, cy), (cx - panel_r - 10, cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(cx + panel_r + 10, cy), (S - seam_margin, cy)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  CAP FACE — single gold connector circle
# ---------------------------------------------------------------------------

def make_cap(powered=False):
    """Cap face (top/bottom): hex armor with a single centered gold
    connector, matching the side face style.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    port_outer = int(S * 0.18)
    port_inner = int(S * 0.10)

    # Recessed panel
    panel_r = int(S * 0.26)
    draw.rectangle(
        [cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw,
        cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r,
        EDGE_DARK, ARMOR_LIGHT, width=4
    )

    # Gold connector
    draw_connector_port(img, draw, cx, cy, port_outer, port_inner,
                        GOLD_RING, GOLD_BRIGHT, GOLD_GLOW,
                        powered=powered)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Cross seams
    seam_margin = 30
    if powered:
        add_glowing_seam(
            img, (seam_margin, cy), (cx - panel_r - 10, cy),
            SEAM_COLOR, GOLD_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
        add_glowing_seam(
            img, (cx + panel_r + 10, cy), (S - seam_margin, cy),
            SEAM_COLOR, GOLD_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
        add_glowing_seam(
            img, (cx, seam_margin), (cx, cy - panel_r - 10),
            SEAM_COLOR, GOLD_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
        add_glowing_seam(
            img, (cx, cy + panel_r + 10), (cx, S - seam_margin),
            SEAM_COLOR, GOLD_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
    else:
        draw.line([(seam_margin, cy), (cx - panel_r - 10, cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(cx + panel_r + 10, cy), (S - seam_margin, cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(cx, seam_margin), (cx, cy - panel_r - 10)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(cx, cy + panel_r + 10), (cx, S - seam_margin)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    textures = {}

    textures["power_splitter_front"] = make_front(powered=False)
    textures["power_splitter_front_powered"] = make_front(powered=True)
    textures["power_splitter_back"] = make_back(powered=False)
    textures["power_splitter_back_powered"] = make_back(powered=True)
    textures["power_splitter_side"] = make_side(powered=False)
    textures["power_splitter_side_powered"] = make_side(powered=True)
    textures["power_splitter_cap"] = make_cap(powered=False)
    textures["power_splitter_cap_powered"] = make_cap(powered=True)

    save_textures(textures)


if __name__ == "__main__":
    main()
