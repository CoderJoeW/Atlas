#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Small Drill at 1024x1024.

Concept: Compact industrial drilling unit. Front face shows a large drill bit
with concentric cutting rings. Back face is a motor housing with exhaust port.
Side faces show directional flow arrows indicating drill direction, with
industrial panel details.

Creates 6 textures:
  small_drill_front, small_drill, small_drill_arrow_up,
  small_drill_arrow_down, small_drill_arrow_left, small_drill_arrow_right
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

# Housing / armor
ARMOR_DARK = (38, 40, 48, 255)
ARMOR_MID = (52, 56, 66, 255)
ARMOR_LIGHT = (70, 75, 88, 255)
HEX_LINE = (48, 52, 62, 255)
EDGE_DARK = (22, 24, 30, 255)
SEAM_COLOR = (30, 33, 40, 255)
RIVET_COLOR = (100, 108, 125, 255)

# Drill / mechanical colors
DRILL_STEEL = (160, 165, 175, 255)
DRILL_STEEL_DARK = (110, 115, 125, 255)
DRILL_STEEL_LIGHT = (190, 195, 205, 255)
DRILL_TIP = (200, 190, 140, 255)  # carbide/gold tip
DRILL_TIP_BRIGHT = (230, 215, 160, 255)
DRILL_ACCENT = (200, 170, 50, 255)  # gold/yellow accent
DRILL_GLOW = (220, 200, 80, 255)

# Motor / power
MOTOR_DARK = (30, 32, 38, 255)
MOTOR_COIL = (80, 60, 40, 255)

# Exhaust / vent
VENT_SLOT = (15, 18, 24, 255)

# Arrow color
ARROW_COLOR = (200, 170, 50, 255)
ARROW_OUTLINE = (140, 120, 40, 255)

# Hex geometry
HEX_RADIUS = 28

# ---------------------------------------------------------------------------
#  Helpers
# ---------------------------------------------------------------------------


def _hex_vertices(cx, cy, radius):
    verts = []
    for i in range(6):
        angle = math.radians(60 * i)
        verts.append((cx + radius * math.cos(angle),
                      cy + radius * math.sin(angle)))
    return verts


def _compute_hex_centers(size, radius, inset=12):
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
    draw.ellipse(
        [cx - radius, cy - radius, cx + radius, cy + radius],
        fill=fill, outline=outline
    )


def add_bevel_border(draw, x1, y1, x2, y2, light, dark, width=2):
    for i in range(width):
        draw.line([(x1 + i, y1 + i), (x2 - i, y1 + i)], fill=light)
        draw.line([(x1 + i, y1 + i), (x1 + i, y2 - i)], fill=light)
        draw.line([(x1 + i, y2 - i), (x2 - i, y2 - i)], fill=dark)
        draw.line([(x2 - i, y1 + i), (x2 - i, y2 - i)], fill=dark)


# ---------------------------------------------------------------------------
#  Hex armor base
# ---------------------------------------------------------------------------

def make_hex_armor_base():
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
    for bx, by in [(inset, inset), (S - inset, inset),
                   (inset, S - inset), (S - inset, S - inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)


# ---------------------------------------------------------------------------
#  Front face — drill bit
# ---------------------------------------------------------------------------

def make_front():
    """Front face: concentric drill bit with cutting edges and carbide tip."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Outer drill housing ring
    housing_r = int(S * 0.42)
    draw_filled_circle(draw, cx, cy, housing_r, ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, cx - housing_r, cy - housing_r,
                     cx + housing_r, cy + housing_r, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Housing bolts around perimeter
    for i in range(12):
        angle = math.radians(30 * i)
        bx = int(cx + (housing_r - 18) * math.cos(angle))
        by = int(cy + (housing_r - 18) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Outer cutting ring
    cut_outer = int(S * 0.35)
    cut_inner = int(S * 0.28)
    draw_filled_circle(draw, cx, cy, cut_outer, DRILL_STEEL_DARK,
                       outline=EDGE_DARK)
    # Gradient ring
    for r in range(cut_outer, cut_inner, -1):
        t = (cut_outer - r) / (cut_outer - cut_inner)
        c = lerp_color(DRILL_STEEL_DARK, DRILL_STEEL, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Cutting teeth on outer ring (radial lines)
    num_teeth = 16
    for i in range(num_teeth):
        angle = math.radians(360 / num_teeth * i)
        x1 = int(cx + cut_inner * math.cos(angle))
        y1 = int(cy + cut_inner * math.sin(angle))
        x2 = int(cx + cut_outer * math.cos(angle))
        y2 = int(cy + cut_outer * math.sin(angle))
        draw.line([(x1, y1), (x2, y2)], fill=EDGE_DARK, width=4)

    # Inner cutting ring
    inner_ring_r = int(S * 0.22)
    draw_filled_circle(draw, cx, cy, inner_ring_r, DRILL_STEEL,
                       outline=EDGE_DARK)

    # Spiral cutting grooves (3 spiral arms)
    for arm in range(3):
        base_angle = math.radians(120 * arm)
        points = []
        for step in range(50):
            t = step / 49
            r = inner_ring_r * (1.0 - t * 0.6)
            a = base_angle + t * math.pi * 1.5
            px = int(cx + r * math.cos(a))
            py = int(cy + r * math.sin(a))
            points.append((px, py))
        if len(points) > 1:
            draw.line(points, fill=DRILL_STEEL_DARK, width=6)

    # Center hub
    hub_r = int(S * 0.08)
    draw_filled_circle(draw, cx, cy, hub_r + 8, DRILL_STEEL_DARK,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, hub_r, DRILL_TIP, outline=EDGE_DARK)
    # Carbide tip gradient
    for r in range(hub_r, 0, -1):
        t = 1.0 - (r / hub_r)
        c = lerp_color(DRILL_TIP, DRILL_TIP_BRIGHT, t ** 0.6)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Center cross mark on tip
    draw.line([(cx - hub_r + 10, cy), (cx + hub_r - 10, cy)],
              fill=DRILL_STEEL_DARK, width=3)
    draw.line([(cx, cy - hub_r + 10), (cx, cy + hub_r - 10)],
              fill=DRILL_STEEL_DARK, width=3)

    # Subtle glow on the drill tip
    add_radial_glow(img, cx, cy, hub_r + 20, DRILL_GLOW, intensity=0.15)
    draw = ImageDraw.Draw(img)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Back face — motor housing
# ---------------------------------------------------------------------------

def make_back():
    """Back face: motor housing with exhaust port and power connection."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Upper panel — ID plate area
    panel_h = int(S * 0.08)
    draw.rectangle([9, 9, S - 10, panel_h + 9], fill=ARMOR_MID)
    add_bevel_border(draw, 9, 9, S - 10, panel_h + 9, ARMOR_LIGHT,
                     EDGE_DARK, width=2)
    for rx in range(60, S - 40, int(S * 0.08)):
        draw_filled_circle(draw, rx, 9 + panel_h // 2, 5, RIVET_COLOR)

    # Lower panel
    base_top = S - panel_h - 9
    draw.rectangle([9, base_top, S - 10, S - 10], fill=ARMOR_MID)
    add_bevel_border(draw, 9, base_top, S - 10, S - 10, ARMOR_LIGHT,
                     EDGE_DARK, width=2)
    for rx in range(60, S - 40, int(S * 0.08)):
        draw_filled_circle(draw, rx, base_top + panel_h // 2, 5,
                           RIVET_COLOR)

    # Central exhaust port
    exhaust_r = int(S * 0.18)
    draw_filled_circle(draw, cx, cy, exhaust_r + 12, ARMOR_MID,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, exhaust_r, VENT_SLOT,
                       outline=EDGE_DARK)

    # Exhaust grille (concentric rings)
    for i in range(1, 5):
        r = int(exhaust_r * (i / 5))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                     outline=ARMOR_MID)

    # Cross bars in exhaust
    draw.line([(cx - exhaust_r + 5, cy), (cx + exhaust_r - 5, cy)],
              fill=ARMOR_MID, width=6)
    draw.line([(cx, cy - exhaust_r + 5), (cx, cy + exhaust_r - 5)],
              fill=ARMOR_MID, width=6)

    # Exhaust bolts
    for i in range(8):
        angle = math.radians(45 * i)
        bx = int(cx + (exhaust_r + 6) * math.cos(angle))
        by = int(cy + (exhaust_r + 6) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 6, ARMOR_LIGHT, outline=EDGE_DARK)

    # Ventilation slots on sides
    vent_w = int(S * 0.12)
    vent_margin = int(S * 0.12)
    for side_x in [vent_margin, S - vent_margin - vent_w]:
        vy_start = panel_h + 40
        vy_end = base_top - 40
        num_slots = 8
        slot_h = (vy_end - vy_start) // num_slots
        for i in range(num_slots):
            sy = vy_start + i * slot_h
            draw.rectangle([side_x, sy, side_x + vent_w, sy + slot_h - 6],
                           fill=VENT_SLOT, outline=EDGE_DARK)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Side face — with directional arrow (base, then rotated)
# ---------------------------------------------------------------------------

def make_side_arrow_up():
    """Side face with upward-pointing arrow indicating drill direction."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Panel seams (horizontal dividers)
    seam_y1 = int(S * 0.15)
    seam_y2 = int(S * 0.85)
    draw.line([(30, seam_y1), (S - 30, seam_y1)], fill=SEAM_COLOR, width=3)
    draw.line([(30, seam_y2), (S - 30, seam_y2)], fill=SEAM_COLOR, width=3)

    # Side panel details — small vent grille on left
    grille_x = int(S * 0.06)
    grille_w = int(S * 0.10)
    grille_y1 = int(S * 0.25)
    grille_y2 = int(S * 0.75)
    draw.rectangle([grille_x, grille_y1, grille_x + grille_w, grille_y2],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    num_slots = 6
    slot_h = (grille_y2 - grille_y1) // num_slots
    for i in range(num_slots):
        sy = grille_y1 + i * slot_h + 4
        draw.rectangle([grille_x + 4, sy,
                        grille_x + grille_w - 4, sy + slot_h - 8],
                       fill=VENT_SLOT)

    # Small vent grille on right (mirror)
    grille_x2 = S - grille_x - grille_w
    draw.rectangle([grille_x2, grille_y1, grille_x2 + grille_w, grille_y2],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    for i in range(num_slots):
        sy = grille_y1 + i * slot_h + 4
        draw.rectangle([grille_x2 + 4, sy,
                        grille_x2 + grille_w - 4, sy + slot_h - 8],
                       fill=VENT_SLOT)

    # Central directional arrow (pointing up)
    arrow_w = int(S * 0.22)
    arrow_h = int(S * 0.35)
    arrow_top = cy - int(arrow_h * 0.55)
    arrow_bot = cy + int(arrow_h * 0.45)
    shaft_w = int(arrow_w * 0.4)
    head_w = arrow_w

    # Arrow shaft
    shaft_x1 = cx - shaft_w // 2
    shaft_x2 = cx + shaft_w // 2
    shaft_top = cy - int(arrow_h * 0.1)
    draw.rectangle([shaft_x1, shaft_top, shaft_x2, arrow_bot],
                   fill=ARROW_COLOR, outline=ARROW_OUTLINE)
    # Inner highlight on shaft
    draw.rectangle([shaft_x1 + 6, shaft_top + 6, shaft_x2 - 6, arrow_bot - 6],
                   outline=DRILL_TIP_BRIGHT)

    # Arrow head (triangle)
    head_top = arrow_top
    head_bot = shaft_top + 2
    head_points = [
        (cx, head_top),
        (cx - head_w // 2, head_bot),
        (cx + head_w // 2, head_bot),
    ]
    draw.polygon(head_points, fill=ARROW_COLOR, outline=ARROW_OUTLINE)
    # Inner highlight triangle
    inner_margin = 10
    inner_points = [
        (cx, head_top + inner_margin * 2),
        (cx - head_w // 2 + inner_margin + 6, head_bot - inner_margin),
        (cx + head_w // 2 - inner_margin - 6, head_bot - inner_margin),
    ]
    draw.polygon(inner_points, outline=DRILL_TIP_BRIGHT)

    # Subtle glow around arrow
    add_radial_glow(img, cx, cy - int(arrow_h * 0.1), int(S * 0.22),
                    DRILL_GLOW, intensity=0.1)
    draw = ImageDraw.Draw(img)

    # Rivets along seams
    for rx in range(80, S - 60, int(S * 0.1)):
        draw_filled_circle(draw, rx, seam_y1, 5, RIVET_COLOR)
        draw_filled_circle(draw, rx, seam_y2, 5, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    front = make_front()
    back = make_back()
    side_up = make_side_arrow_up()

    # Rotate for other arrow directions
    side_down = side_up.rotate(180)
    side_left = side_up.rotate(90, expand=False)
    side_right = side_up.rotate(-90, expand=False)

    textures = {
        "small_drill_front": front,
        "small_drill": back,
        "small_drill_arrow_up": side_up,
        "small_drill_arrow_down": side_down,
        "small_drill_arrow_left": side_left,
        "small_drill_arrow_right": side_right,
    }
    save_textures(textures)


if __name__ == "__main__":
    main()
