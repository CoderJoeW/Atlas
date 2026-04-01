#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Power Cable at 1024x1024.

Concept: Dark armored housing with honeycomb grid on all faces.
Directional gold arrows indicate flow direction. Powered variant
adds glowing amber/orange energy effects to arrows and seams.

Creates 20 textures:
  Front  (2): power_cable_front, _front_powered
  Back   (2): power_cable_back, _back_powered
  Side   (8): power_cable_side_{up,down,left,right} + _powered
  Cap    (8): power_cable_cap_{up,down,left,right} + _powered
"""

import math
from PIL import Image, ImageDraw

from texture_lib import (
    new_img, add_border, lerp_color, blend_over,
    draw_hex_grid_lines_only, add_radial_glow, add_glow_ring,
    add_glowing_seam, save_textures,
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

# Energy colors — gold/amber for cable direction arrows
GOLD = (210, 175, 40, 255)
GOLD_DIM = (160, 130, 30, 255)
GOLD_BRIGHT = (255, 220, 60, 255)
AMBER_GLOW = (255, 180, 40, 255)
ORANGE_GLOW = (255, 140, 30, 255)

# Connector port colors
CONNECTOR_RING = (180, 150, 50, 255)
CONNECTOR_HOLE = (12, 14, 18, 255)

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
    """Create a hex armor face — the foundation for all cable faces."""
    img = new_img(S, ARMOR_DARK)
    draw = ImageDraw.Draw(img)

    # Outer border
    add_border(draw, S, EDGE_DARK, width=6)
    add_bevel_border(draw, 6, 6, S - 7, S - 7, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Hex cells
    for cx, cy in HEX_CENTERS:
        verts = _hex_vertices(cx, cy, HEX_RADIUS - 1)
        draw.polygon(verts, fill=ARMOR_DARK, outline=HEX_LINE)

    return img


def add_corner_bolts(draw, inset=30):
    """Add bolts at the four corners of a face."""
    positions = [
        (inset, inset), (S - inset, inset),
        (inset, S - inset), (S - inset, S - inset),
    ]
    for bx, by in positions:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)


# ---------------------------------------------------------------------------
#  Arrow drawing — chevron + shaft pointing UP at 1024 scale
# ---------------------------------------------------------------------------

def draw_arrow_up(img, color, shaft_color=None, thick=False):
    """Draw a directional arrow/chevron pointing UP, centered.

    The arrow has a V-shaped chevron head and a straight shaft below.
    If thick=True, uses wider strokes (for powered glow underlay).
    """
    draw = ImageDraw.Draw(img)
    cx = S // 2
    if shaft_color is None:
        shaft_color = color

    # Arrow geometry
    tip_y = int(S * 0.18)
    wing_y = int(S * 0.40)
    shaft_top = wing_y - int(S * 0.04)
    shaft_bottom = int(S * 0.82)
    wing_half = int(S * 0.28)
    shaft_half = int(S * 0.045)
    stroke = 28 if thick else 18

    # Chevron head — two angled lines forming a V pointing up
    # Left wing
    draw.line(
        [(cx, tip_y), (cx - wing_half, wing_y)],
        fill=color, width=stroke
    )
    # Right wing
    draw.line(
        [(cx, tip_y), (cx + wing_half, wing_y)],
        fill=color, width=stroke
    )

    # Shaft
    draw.rectangle(
        [cx - shaft_half, shaft_top, cx + shaft_half, shaft_bottom],
        fill=shaft_color
    )


def draw_arrow_glow(img, glow_color, intensity=0.25):
    """Add a soft glow along the arrow path (for powered state)."""
    cx = S // 2
    # Glow along the shaft
    add_radial_glow(
        img, cx, int(S * 0.50), int(S * 0.18),
        glow_color, intensity=intensity
    )
    # Glow at the chevron tip
    add_radial_glow(
        img, cx, int(S * 0.25), int(S * 0.22),
        glow_color, intensity=intensity * 0.8
    )


# ---------------------------------------------------------------------------
#  FRONT FACE — connector port
# ---------------------------------------------------------------------------

def make_front(powered=False):
    """Front face: hex armor with a central circular connector port."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    port_outer = int(S * 0.22)
    port_inner = int(S * 0.12)

    # Recessed panel behind the port
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

    # Connector ring
    ring_color = CONNECTOR_RING if not powered else GOLD_BRIGHT
    for r in range(port_outer, port_inner, -1):
        t = (port_outer - r) / (port_outer - port_inner)
        c = lerp_color(ring_color, ARMOR_MID, t * 0.6)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Connector hole
    draw_filled_circle(draw, cx, cy, port_inner, CONNECTOR_HOLE,
                       outline=EDGE_DARK)

    # Contact pins inside the hole (small bright dots)
    pin_r = int(S * 0.06)
    pin_size = 6
    pin_color = GOLD_DIM if not powered else GOLD_BRIGHT
    for angle_deg in [0, 90, 180, 270]:
        angle = math.radians(angle_deg)
        px = int(cx + pin_r * math.cos(angle))
        py = int(cy + pin_r * math.sin(angle))
        draw_filled_circle(draw, px, py, pin_size, pin_color)

    # Bolts around the port panel
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)

    # Powered: glow around connector
    if powered:
        add_glow_ring(img, cx, cy, port_outer, port_outer + int(S * 0.08),
                      AMBER_GLOW)
        add_radial_glow(img, cx, cy, port_inner, GOLD_BRIGHT,
                        intensity=0.3)

    return img


# ---------------------------------------------------------------------------
#  BACK FACE — recessed output panel
# ---------------------------------------------------------------------------

def make_back(powered=False):
    """Back face: hex armor with a dark recessed square."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    recess_r = int(S * 0.18)

    # Recessed square
    draw.rectangle(
        [cx - recess_r, cy - recess_r, cx + recess_r, cy + recess_r],
        fill=(18, 20, 26, 255)
    )
    add_bevel_border(
        draw,
        cx - recess_r, cy - recess_r, cx + recess_r, cy + recess_r,
        EDGE_DARK, ARMOR_LIGHT, width=4
    )

    # Inner grate lines
    for offset in range(-recess_r + 20, recess_r, 24):
        draw.line(
            [(cx - recess_r + 10, cy + offset),
             (cx + recess_r - 10, cy + offset)],
            fill=(28, 30, 36, 255), width=2
        )

    add_corner_bolts(draw)

    # Horizontal seam
    draw.line([(12, S // 2), (cx - recess_r - 10, S // 2)],
              fill=SEAM_COLOR, width=2)
    draw.line([(cx + recess_r + 10, S // 2), (S - 12, S // 2)],
              fill=SEAM_COLOR, width=2)

    if powered:
        add_radial_glow(img, cx, cy, recess_r + int(S * 0.06),
                        AMBER_GLOW, intensity=0.15)

    return img


# ---------------------------------------------------------------------------
#  SIDE FACE — hex armor with directional arrow
# ---------------------------------------------------------------------------

def make_side_up(powered=False):
    """Side face with hex armor and a gold arrow pointing UP.

    The arrow indicates energy flow direction. A gold border strip
    at the top edge marks the side facing the front connector.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    # Gold border strip at top edge (toward front face)
    strip_h = 14
    strip_color = GOLD_DIM if not powered else GOLD_BRIGHT
    draw.rectangle([10, 10, S - 10, 10 + strip_h], fill=strip_color)

    # Arrow
    if powered:
        draw_arrow_up(img, GOLD_BRIGHT, GOLD_BRIGHT, thick=True)
        draw_arrow_glow(img, AMBER_GLOW, intensity=0.3)
    else:
        draw_arrow_up(img, GOLD, GOLD_DIM)

    add_corner_bolts(draw, inset=30)

    # Horizontal seam below the arrow shaft
    seam_y = int(S * 0.90)
    if powered:
        add_glowing_seam(
            img, (30, seam_y), (S - 30, seam_y),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=8, intensity=0.2
        )
    else:
        draw.line([(30, seam_y), (S - 30, seam_y)],
                  fill=SEAM_COLOR, width=2)

    return img


# ---------------------------------------------------------------------------
#  CAP FACE — hex armor with directional arrow (no border strip)
# ---------------------------------------------------------------------------

def make_cap_up(powered=False):
    """Cap face (top/bottom of cable) with arrow pointing UP.

    Similar to side but with corner accent marks instead of a
    full border strip.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    # Corner accent marks
    accent_color = GOLD_DIM if not powered else GOLD_BRIGHT
    accent_len = int(S * 0.08)
    m = 12  # margin from edge
    draw.rectangle([m, m, m + accent_len, m + 6], fill=accent_color)
    draw.rectangle([S - m - accent_len, m, S - m, m + 6],
                   fill=accent_color)

    # Arrow
    if powered:
        draw_arrow_up(img, GOLD_BRIGHT, GOLD_BRIGHT, thick=True)
        draw_arrow_glow(img, AMBER_GLOW, intensity=0.25)
    else:
        draw_arrow_up(img, GOLD, GOLD_DIM)

    add_corner_bolts(draw, inset=30)

    return img


# ---------------------------------------------------------------------------
#  Rotation helpers
# ---------------------------------------------------------------------------

def rotate_variants(make_fn, prefix, powered=False):
    """Generate up/down/left/right variants by rotating the UP base."""
    base = make_fn(powered=powered)
    suffix = "_powered" if powered else ""
    imgs = {}
    imgs[f"{prefix}_up{suffix}"] = base.copy()
    imgs[f"{prefix}_down{suffix}"] = base.rotate(180)
    imgs[f"{prefix}_left{suffix}"] = base.rotate(90)
    imgs[f"{prefix}_right{suffix}"] = base.rotate(-90)
    return imgs


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    textures = {}

    # Front and back (unpowered + powered)
    textures["power_cable_front"] = make_front(powered=False)
    textures["power_cable_front_powered"] = make_front(powered=True)
    textures["power_cable_back"] = make_back(powered=False)
    textures["power_cable_back_powered"] = make_back(powered=True)

    # Side variants (unpowered + powered)
    textures.update(rotate_variants(
        make_side_up, "power_cable_side", powered=False
    ))
    textures.update(rotate_variants(
        make_side_up, "power_cable_side", powered=True
    ))

    # Cap variants (unpowered + powered)
    textures.update(rotate_variants(
        make_cap_up, "power_cable_cap", powered=False
    ))
    textures.update(rotate_variants(
        make_cap_up, "power_cable_cap", powered=True
    ))

    save_textures(textures)


if __name__ == "__main__":
    main()
