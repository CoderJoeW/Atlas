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
    """Create top face with hex armor and a circular gauge in the center."""
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

    cx, cy = S // 2, S // 2

    # --- Gauge housing: recessed circular plate ---
    gauge_outer_r = 360
    gauge_inner_r = 280
    gauge_face_r = 260

    # Dark recessed ring
    draw_filled_circle(draw, cx, cy, gauge_outer_r + 6,
                       EDGE_DARK)
    # Metallic gauge frame
    for r in range(gauge_inner_r, gauge_outer_r + 1):
        t = (r - gauge_inner_r) / (gauge_outer_r - gauge_inner_r)
        if t < 0.15:
            color = FRAME_DARK
        elif t < 0.35:
            color = FRAME_MID
        elif t < 0.65:
            color = FRAME_LIGHT
        elif t < 0.85:
            color = FRAME_MID
        else:
            color = FRAME_DARK
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color)

    # Gauge face (dark background)
    draw_filled_circle(draw, cx, cy, gauge_face_r, (15, 17, 22, 255))

    # Tick marks around the gauge face
    num_ticks = 24
    for i in range(num_ticks):
        angle = math.radians(i * (360 / num_ticks) - 90)
        # Start at inner edge of face
        tick_inner = gauge_face_r - 30
        tick_outer = gauge_face_r - 8
        x1 = cx + int(tick_inner * math.cos(angle))
        y1 = cy + int(tick_inner * math.sin(angle))
        x2 = cx + int(tick_outer * math.cos(angle))
        y2 = cy + int(tick_outer * math.sin(angle))
        tick_color = ARMOR_LIGHT if i % 4 == 0 else FRAME_DARK
        draw.line([(x1, y1), (x2, y2)], fill=tick_color, width=3)

    # --- Fill arc: sweeps clockwise from bottom (6 o'clock) ---
    if fill_fraction > 0 and energy_color:
        arc_r_outer = gauge_face_r - 40
        arc_r_inner = gauge_face_r - 120
        # Draw arc as filled wedge segments
        # Start angle: 90 degrees (6 o'clock / bottom)
        # Sweep clockwise by fill_fraction * 360
        sweep_degrees = fill_fraction * 360
        start_deg = 90  # bottom

        # Draw filled arc segments
        num_segments = max(4, int(sweep_degrees / 2))
        for seg in range(num_segments):
            t = seg / num_segments
            angle_deg = start_deg + t * sweep_degrees
            angle_next = start_deg + (seg + 1) / num_segments * sweep_degrees
            a1 = math.radians(angle_deg)
            a2 = math.radians(angle_next)

            # Quad: outer1, outer2, inner2, inner1
            quad = [
                (cx + arc_r_outer * math.cos(a1),
                 cy + arc_r_outer * math.sin(a1)),
                (cx + arc_r_outer * math.cos(a2),
                 cy + arc_r_outer * math.sin(a2)),
                (cx + arc_r_inner * math.cos(a2),
                 cy + arc_r_inner * math.sin(a2)),
                (cx + arc_r_inner * math.cos(a1),
                 cy + arc_r_inner * math.sin(a1)),
            ]
            draw.polygon(quad, fill=energy_color)

        # Bright leading edge of the arc
        end_angle = math.radians(start_deg + sweep_degrees)
        ex = cx + int((arc_r_outer + arc_r_inner) / 2 * math.cos(end_angle))
        ey = cy + int((arc_r_outer + arc_r_inner) / 2 * math.sin(end_angle))
        add_radial_glow(img, ex, ey, 40, glow_color, intensity=0.5)

        # Inner glow ring
        draw_thick_circle(draw, cx, cy, arc_r_inner - 4,
                          (*energy_color[:3], 100), 2)

        # Center readout glow
        add_radial_glow(img, cx, cy, arc_r_inner - 20,
                        glow_color, intensity=0.15)

    # Center hub bolt
    draw_filled_circle(draw, cx, cy, 24, FRAME_MID, outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, 12, RIVET_COLOR)

    # Bolts around gauge frame (8 evenly spaced)
    bolt_ring_r = gauge_outer_r - 14
    for i in range(8):
        angle = math.radians(45 * i)
        bx = int(cx + bolt_ring_r * math.cos(angle))
        by = int(cy + bolt_ring_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Corner bolts
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
