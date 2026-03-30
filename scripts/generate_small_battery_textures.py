#!/usr/bin/env python3
"""Generate sci-fi energy cell textures for the Small Battery at 1024x1024.

Concept: Full hex armor housing. Hex cells fill from bottom upward with
energy color to indicate charge level. Side faces show the fill directly.
Top face shows a circular gauge representing the same charge level.

Charge states:
  empty   — all dark
  low     — bottom 1/3 red
  medium  — bottom 1/2 orange-yellow
  high    — bottom 3/4 yellow
  full    — entire face green

Creates 12 textures:
  Top  (5): small_battery, _low, _medium, _high, _full
  Side (5): small_battery_side, _side_low, _side_medium, _side_high, _side_full
  Bottom(1): small_battery_bottom
"""

import math
from PIL import Image, ImageDraw

from texture_lib import (
    new_img, add_border, lerp_color, blend_over,
    draw_hex_grid_lines_only, add_radial_glow,
    save_textures,
)

S = 1024  # texture size

# ---------------------------------------------------------------------------
#  Color Palette
# ---------------------------------------------------------------------------

# Housing / armor
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

# Charge state colors
COLOR_RED = (220, 45, 30, 255)
COLOR_RED_GLOW = (255, 60, 40, 255)
COLOR_ORANGE = (240, 160, 30, 255)
COLOR_ORANGE_GLOW = (255, 190, 50, 255)
COLOR_YELLOW = (230, 220, 40, 255)
COLOR_YELLOW_GLOW = (255, 245, 80, 255)
COLOR_GREEN = (50, 210, 50, 255)
COLOR_GREEN_GLOW = (80, 255, 80, 255)

# Charge state definitions: (fill_fraction, energy_color, glow_color)
CHARGE_STATES = {
    "empty":  (0.0,  None, None),
    "low":    (1/3,  COLOR_RED, COLOR_RED_GLOW),
    "medium": (1/2,  COLOR_ORANGE, COLOR_ORANGE_GLOW),
    "high":   (3/4,  COLOR_YELLOW, COLOR_YELLOW_GLOW),
    "full":   (1.0,  COLOR_GREEN, COLOR_GREEN_GLOW),
}

# Hex geometry for the armor cells
HEX_RADIUS = 28
HEX_H = HEX_RADIUS * math.sqrt(3)
HEX_COL_STEP = HEX_RADIUS * 1.5
HEX_ROW_STEP = HEX_H


# ---------------------------------------------------------------------------
#  Helpers
# ---------------------------------------------------------------------------

def draw_thick_circle(draw, cx, cy, radius, color, width):
    """Draw a circle with a given line width."""
    for w in range(width):
        r = radius - w
        if r > 0:
            draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                         outline=color)


def draw_filled_circle(draw, cx, cy, radius, fill, outline=None):
    """Draw a filled circle."""
    draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
                 fill=fill, outline=outline)


def add_bevel_border(draw, x1, y1, x2, y2, light, dark, width=2):
    """Draw a beveled rectangle border (raised appearance)."""
    for i in range(width):
        draw.line([(x1 + i, y1 + i), (x2 - i, y1 + i)], fill=light)
        draw.line([(x1 + i, y1 + i), (x1 + i, y2 - i)], fill=light)
        draw.line([(x1 + i, y2 - i), (x2 - i, y2 - i)], fill=dark)
        draw.line([(x2 - i, y1 + i), (x2 - i, y2 - i)], fill=dark)


# ---------------------------------------------------------------------------
#  Hex cell grid computation
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
    """Compute all hex cell center positions for a grid covering the image."""
    centers = []
    col_step = radius * 1.5
    row_step = radius * math.sqrt(3)
    cols = int((size - 2 * inset) / col_step) + 3
    rows = int((size - 2 * inset) / row_step) + 3

    for col in range(-1, cols + 1):
        for row in range(-1, rows + 1):
            cx = inset + col * col_step
            cy = inset + row * row_step + (col % 2) * (row_step / 2)
            # Only include cells whose center is within the image
            if -radius < cx < size + radius and -radius < cy < size + radius:
                centers.append((cx, cy))
    return centers


HEX_CENTERS = _compute_hex_centers(S, HEX_RADIUS)


# ---------------------------------------------------------------------------
#  Hex armor face with charge fill
# ---------------------------------------------------------------------------

def make_hex_armor_face(fill_fraction=0.0, energy_color=None,
                        glow_color=None):
    """Create a hex armor face with cells lit below a Y threshold.

    fill_fraction: 0.0 = all dark, 1.0 = all lit.
    Energy fills from bottom upward.
    """
    img = new_img(S, ARMOR_DARK)
    draw = ImageDraw.Draw(img)

    # Outer border
    add_border(draw, S, EDGE_DARK, width=6)
    add_bevel_border(draw, 6, 6, S - 7, S - 7, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Compute the Y threshold: cells with center below this are lit
    # fill_fraction=1.0 means everything lit (threshold at top)
    # fill_fraction=0.0 means nothing lit (threshold below bottom)
    margin = 12
    fill_y = S - margin - fill_fraction * (S - 2 * margin)

    # Draw hex cells — all dark with subtle outlines
    for cx, cy in HEX_CENTERS:
        verts = _hex_vertices(cx, cy, HEX_RADIUS - 1)
        draw.polygon(verts, fill=ARMOR_DARK, outline=HEX_LINE)

    return img


# ---------------------------------------------------------------------------
#  SIDE FACE
# ---------------------------------------------------------------------------

def make_side():
    """Create a side face with hex armor."""
    img = make_hex_armor_face()
    draw = ImageDraw.Draw(img)

    # Horizontal panel seam at the midpoint
    draw.line([(12, S // 2), (S - 12, S // 2)],
              fill=SEAM_COLOR, width=2)

    # Corner bolts
    bolt_inset = 24
    for bx, by in [(bolt_inset, bolt_inset),
                   (S - bolt_inset, bolt_inset),
                   (bolt_inset, S - bolt_inset),
                   (S - bolt_inset, S - bolt_inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    return img


# ---------------------------------------------------------------------------
#  TOP FACE — circular gauge
# ---------------------------------------------------------------------------

def make_top(fill_fraction=0.0, energy_color=None, glow_color=None):
    """Create top face with hex armor and a segmented charge bar."""
    img = make_hex_armor_face()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    # --- Charge bar: recessed rectangular panel ---
    num_segments = 10
    bar_margin = 120        # inset from image edges
    bar_height = 360        # total height of the bar area
    bar_y = cy - bar_height // 2
    bar_x1 = bar_margin
    bar_x2 = S - bar_margin
    bar_w = bar_x2 - bar_x1
    seg_gap = 12            # gap between segments
    seg_w = (bar_w - (num_segments + 1) * seg_gap) // num_segments
    seg_inset = 20          # vertical inset within the panel

    # Recessed panel background
    panel_pad = 24
    draw.rectangle([bar_x1 - panel_pad, bar_y - panel_pad,
                   bar_x2 + panel_pad, bar_y + bar_height + panel_pad],
                  fill=ARMOR_MID)
    add_bevel_border(draw,
                     bar_x1 - panel_pad, bar_y - panel_pad,
                     bar_x2 + panel_pad, bar_y + bar_height + panel_pad,
                     EDGE_DARK, ARMOR_LIGHT, width=4)

    # Inner dark recess for the bar
    draw.rectangle([bar_x1, bar_y, bar_x2, bar_y + bar_height],
                  fill=(15, 17, 22, 255))
    add_bevel_border(draw, bar_x1, bar_y, bar_x2, bar_y + bar_height,
                     EDGE_DARK, FRAME_DARK, width=3)

    # Segment slots
    lit_count = int(fill_fraction * num_segments + 0.5)
    for i in range(num_segments):
        sx = bar_x1 + seg_gap + i * (seg_w + seg_gap)
        sy = bar_y + seg_inset
        sw = seg_w
        sh = bar_height - 2 * seg_inset

        if i < lit_count and energy_color:
            # Lit segment
            draw.rectangle([sx, sy, sx + sw, sy + sh],
                          fill=energy_color)
            # Brighter center stripe
            stripe_inset = sw // 4
            bright = lerp_color(energy_color, glow_color, 0.4)
            draw.rectangle([sx + stripe_inset, sy + 6,
                           sx + sw - stripe_inset, sy + sh - 6],
                          fill=bright)
            # Glow bleed from lit segment
            add_radial_glow(img, sx + sw // 2, cy,
                            sw, glow_color, intensity=0.15)
        else:
            # Dark empty slot
            draw.rectangle([sx, sy, sx + sw, sy + sh],
                          fill=(20, 22, 28, 255))
            # Subtle inner border
            draw.rectangle([sx + 2, sy + 2, sx + sw - 2, sy + sh - 2],
                          outline=(28, 30, 38, 255))

    # Bolts at panel corners
    bolt_positions = [
        (bar_x1 - panel_pad + 12, bar_y - panel_pad + 12),
        (bar_x2 + panel_pad - 12, bar_y - panel_pad + 12),
        (bar_x1 - panel_pad + 12, bar_y + bar_height + panel_pad - 12),
        (bar_x2 + panel_pad - 12, bar_y + bar_height + panel_pad - 12),
    ]
    for bx, by in bolt_positions:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Corner bolts on the face
    bolt_inset = 40
    for bx, by in [(bolt_inset, bolt_inset),
                   (S - bolt_inset, bolt_inset),
                   (bolt_inset, S - bolt_inset),
                   (S - bolt_inset, S - bolt_inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    return img


# ---------------------------------------------------------------------------
#  BOTTOM FACE — heavy base plate
# ---------------------------------------------------------------------------

def make_bottom():
    """Create bottom face: hex armor with vent grille and mounting feet."""
    img = new_img(S, ARMOR_DARK)
    draw = ImageDraw.Draw(img)

    # Outer border
    add_border(draw, S, EDGE_DARK, width=6)
    add_bevel_border(draw, 6, 6, S - 7, S - 7, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Background hex pattern
    for cx, cy in HEX_CENTERS:
        verts = _hex_vertices(cx, cy, HEX_RADIUS - 1)
        draw.polygon(verts, fill=ARMOR_DARK, outline=HEX_LINE)

    center = S // 2

    # Central vent grille
    grille_size = 400
    g1 = center - grille_size // 2
    g2 = center + grille_size // 2
    draw.rectangle([g1, g1, g2, g2], fill=ARMOR_MID, outline=ARMOR_LIGHT)
    add_bevel_border(draw, g1, g1, g2, g2, ARMOR_LIGHT, EDGE_DARK, width=3)

    # Vent slots
    for vy in range(g1 + 30, g2 - 20, 24):
        draw.rectangle([g1 + 20, vy, g2 - 20, vy + 10], fill=EDGE_DARK)
        draw.rectangle([g1 + 22, vy + 2, g2 - 22, vy + 8],
                       fill=(15, 17, 22, 255))

    # Mounting feet in corners
    foot_size = 80
    foot_inset = 60
    for fx, fy in [(foot_inset, foot_inset),
                   (S - foot_inset - foot_size, foot_inset),
                   (foot_inset, S - foot_inset - foot_size),
                   (S - foot_inset - foot_size,
                    S - foot_inset - foot_size)]:
        draw.rectangle([fx, fy, fx + foot_size, fy + foot_size],
                       fill=ARMOR_LIGHT, outline=EDGE_DARK)
        add_bevel_border(draw, fx, fy, fx + foot_size, fy + foot_size,
                         FRAME_LIGHT, EDGE_DARK, width=2)
        fcx = fx + foot_size // 2
        fcy = fy + foot_size // 2
        draw_filled_circle(draw, fcx, fcy, 14, ARMOR_MID,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, fcx, fcy, 7, RIVET_COLOR)

    # Cross seams
    draw.line([(center, foot_inset + foot_size), (center, g1)],
              fill=SEAM_COLOR, width=3)
    draw.line([(center, g2), (center, S - foot_inset - foot_size)],
              fill=SEAM_COLOR, width=3)
    draw.line([(foot_inset + foot_size, center), (g1, center)],
              fill=SEAM_COLOR, width=3)
    draw.line([(g2, center), (S - foot_inset - foot_size, center)],
              fill=SEAM_COLOR, width=3)

    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    textures = {}

    # Top face for each charge state
    for state, (frac, color, glow) in CHARGE_STATES.items():
        suffix = "" if state == "empty" else f"_{state}"
        textures[f"small_battery{suffix}"] = make_top(frac, color, glow)

    # Side and bottom (shared across all charge states)
    textures["small_battery_side"] = make_side()
    textures["small_battery_bottom"] = make_bottom()

    save_textures(textures)


if __name__ == "__main__":
    main()
