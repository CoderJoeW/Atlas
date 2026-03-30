#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Fluid Merger at 1024x1024.

Concept: Dark armored housing with honeycomb grid. Each face has a
single centered connector circle. The circle color changes based on
fluid state:
  - none: cyan/teal (inactive)
  - water: blue
  - lava: orange
  - experience: green

The front face (output) uses a brighter cyan, the back face (input)
uses a deeper teal, and side/top/bottom use a neutral cyan.

Creates 20 textures:
  5 faces (front, back, side, top, bottom) x 4 fluid states
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

# Front (output) — bright cyan
FRONT_RING = (30, 180, 200, 255)
FRONT_BRIGHT = (50, 230, 255, 255)
FRONT_GLOW = (40, 220, 240, 255)

# Back (input) — deeper teal
BACK_RING = (20, 140, 150, 255)
BACK_BRIGHT = (30, 200, 210, 255)
BACK_GLOW = (25, 180, 190, 255)

# Side/top/bottom — neutral cyan
NEUTRAL_RING = (30, 160, 175, 255)
NEUTRAL_BRIGHT = (40, 210, 230, 255)
NEUTRAL_GLOW = (35, 190, 210, 255)

# Fluid state colors
FLUID_COLORS = {
    "none": None,  # uses the face's default color
    "water": {
        "ring": (30, 100, 220, 255),
        "bright": (50, 140, 255, 255),
        "glow": (40, 120, 255, 255),
    },
    "lava": {
        "ring": (220, 120, 30, 255),
        "bright": (255, 160, 50, 255),
        "glow": (255, 140, 40, 255),
    },
    "xp": {
        "ring": (50, 200, 50, 255),
        "bright": (80, 255, 80, 255),
        "glow": (60, 240, 60, 255),
    },
}

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
                        active=False):
    """Draw a color-coded circular connector port with contact pins."""
    active_ring = ring_color if not active else bright_color

    for r in range(outer_r, inner_r, -1):
        t = (outer_r - r) / (outer_r - inner_r)
        c = lerp_color(active_ring, ARMOR_MID, t * 0.6)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    draw_filled_circle(draw, cx, cy, inner_r, CONNECTOR_HOLE,
                       outline=EDGE_DARK)

    pin_r = int(inner_r * 0.5)
    pin_size = max(4, inner_r // 12)
    pin_color = ring_color if not active else bright_color
    for angle_deg in [0, 90, 180, 270]:
        angle = math.radians(angle_deg)
        px = int(cx + pin_r * math.cos(angle))
        py = int(cy + pin_r * math.sin(angle))
        draw_filled_circle(draw, px, py, pin_size, pin_color)

    if active:
        add_glow_ring(img, cx, cy, outer_r,
                      outer_r + int(outer_r * 0.35), glow_color)
        add_radial_glow(img, cx, cy, inner_r, bright_color,
                        intensity=0.3)


# ---------------------------------------------------------------------------
#  Face builders
# ---------------------------------------------------------------------------

def _get_colors(fluid, default_ring, default_bright, default_glow):
    """Return (ring, bright, glow, is_active) for a given fluid state."""
    fc = FLUID_COLORS[fluid]
    if fc is None:
        return default_ring, default_bright, default_glow, False
    return fc["ring"], fc["bright"], fc["glow"], True


def make_face_with_panel(fluid, default_ring, default_bright,
                         default_glow, seam_style="horizontal"):
    """Build a face with a single centered connector in a recessed panel.

    seam_style: 'horizontal', 'cross', or 'none'
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    port_outer = int(S * 0.22)
    port_inner = int(S * 0.12)

    ring, bright, glow, active = _get_colors(
        fluid, default_ring, default_bright, default_glow
    )

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
                        ring, bright, glow, active=active)

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
    seam_glow = glow if active else None
    if seam_style in ("horizontal", "cross"):
        if active:
            add_glowing_seam(
                img, (m, cy), (cx - panel_r - 10, cy),
                SEAM_COLOR, seam_glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
            add_glowing_seam(
                img, (cx + panel_r + 10, cy), (S - m, cy),
                SEAM_COLOR, seam_glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
        else:
            draw.line([(m, cy), (cx - panel_r - 10, cy)],
                      fill=SEAM_COLOR, width=2)
            draw.line([(cx + panel_r + 10, cy), (S - m, cy)],
                      fill=SEAM_COLOR, width=2)

    if seam_style == "cross":
        if active:
            add_glowing_seam(
                img, (cx, m), (cx, cy - panel_r - 10),
                SEAM_COLOR, seam_glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
            add_glowing_seam(
                img, (cx, cy + panel_r + 10), (cx, S - m),
                SEAM_COLOR, seam_glow,
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
#  Public face functions
# ---------------------------------------------------------------------------

def make_front(fluid="none"):
    """Front face (output): bright cyan connector."""
    return make_face_with_panel(
        fluid, FRONT_RING, FRONT_BRIGHT, FRONT_GLOW,
        seam_style="none"
    )


def make_back(fluid="none"):
    """Back face (input): deeper teal connector."""
    return make_face_with_panel(
        fluid, BACK_RING, BACK_BRIGHT, BACK_GLOW,
        seam_style="none"
    )


def make_side(fluid="none"):
    """Side face: neutral cyan connector with horizontal seams."""
    return make_face_with_panel(
        fluid, NEUTRAL_RING, NEUTRAL_BRIGHT, NEUTRAL_GLOW,
        seam_style="horizontal"
    )


def make_top(fluid="none"):
    """Top face: neutral cyan connector with cross seams."""
    return make_face_with_panel(
        fluid, NEUTRAL_RING, NEUTRAL_BRIGHT, NEUTRAL_GLOW,
        seam_style="cross"
    )


def make_bottom(fluid="none"):
    """Bottom face: neutral cyan connector with cross seams."""
    return make_face_with_panel(
        fluid, NEUTRAL_RING, NEUTRAL_BRIGHT, NEUTRAL_GLOW,
        seam_style="cross"
    )


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

# Mapping of fluid states to texture suffixes
FLUID_SUFFIXES = {
    "none": "",
    "water": "_water",
    "lava": "_lava",
    "xp": "_xp",
}

# Face builders and their texture name stems
FACES = {
    "front": make_front,
    "back": make_back,
    "side": make_side,
    "top": make_top,
    "bottom": make_bottom,
}


def main():
    textures = {}

    for face_name, make_fn in FACES.items():
        for fluid, suffix in FLUID_SUFFIXES.items():
            tex_name = f"fluid_merger_{face_name}{suffix}"
            textures[tex_name] = make_fn(fluid=fluid)

    save_textures(textures)


if __name__ == "__main__":
    main()
