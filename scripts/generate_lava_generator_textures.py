#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Lava Generator at 1024x1024.

Concept: Heavy industrial lava generation unit. Side face shows a large
heat window/chamber with containment grid. Top has a magma core output
port with heat vents. Bottom is a heavy base plate with ventilation.
Active state glows with orange/lava colors.

Creates 5 textures:
  top, top_active, bottom, side, side_active
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

# Lava / heat colors
LAVA_BRIGHT = (255, 160, 30, 255)
LAVA_MID = (230, 90, 20, 255)
LAVA_DIM = (180, 60, 10, 255)
LAVA_DARK = (120, 30, 5, 255)
LAVA_GLOW = (255, 140, 40, 255)
EMBER = (200, 50, 10, 255)

# Heat chamber
CHAMBER_DARK = (15, 12, 10, 255)
CHAMBER_GRID = (55, 50, 45, 255)
CHAMBER_FRAME = (65, 60, 55, 255)

# Inactive window
WINDOW_DARK = (20, 22, 28, 255)
WINDOW_GRID = (40, 42, 50, 255)

# Vent / exhaust
VENT_SLOT = (15, 18, 24, 255)

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
#  Side face — heat chamber window
# ---------------------------------------------------------------------------

def make_side(active=False):
    """Side face: heat chamber window with containment grid."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Upper panel strip
    panel_h = int(S * 0.08)
    draw.rectangle([9, 9, S - 10, panel_h + 9], fill=ARMOR_MID)
    add_bevel_border(draw, 9, 9, S - 10, panel_h + 9, ARMOR_LIGHT,
                     EDGE_DARK, width=2)
    # Rivets along upper panel
    for rx in range(60, S - 40, int(S * 0.08)):
        draw_filled_circle(draw, rx, 9 + panel_h // 2, 5, RIVET_COLOR)

    # Lower panel strip
    base_top = S - panel_h - 9
    draw.rectangle([9, base_top, S - 10, S - 10], fill=ARMOR_MID)
    add_bevel_border(draw, 9, base_top, S - 10, S - 10, ARMOR_LIGHT,
                     EDGE_DARK, width=2)
    for rx in range(60, S - 40, int(S * 0.08)):
        draw_filled_circle(draw, rx, base_top + panel_h // 2, 5,
                           RIVET_COLOR)

    # Main heat chamber window
    win_margin_x = int(S * 0.10)
    win_y1 = panel_h + 9 + 20
    win_y2 = base_top - 20
    win_x1 = win_margin_x
    win_x2 = S - win_margin_x

    # Window frame
    frame_w = 8
    draw.rectangle([win_x1 - frame_w, win_y1 - frame_w,
                    win_x2 + frame_w, win_y2 + frame_w],
                   fill=CHAMBER_FRAME if active else ARMOR_MID,
                   outline=EDGE_DARK)
    add_bevel_border(draw, win_x1 - frame_w, win_y1 - frame_w,
                     win_x2 + frame_w, win_y2 + frame_w,
                     ARMOR_LIGHT, EDGE_DARK, width=3)

    # Window interior
    if active:
        # Lava glow background
        draw.rectangle([win_x1, win_y1, win_x2, win_y2], fill=LAVA_DARK)
        # Gradient from dark edges to bright center
        win_cx = (win_x1 + win_x2) // 2
        win_cy = (win_y1 + win_y2) // 2
        win_w = win_x2 - win_x1
        win_h = win_y2 - win_y1
        for x in range(win_x1, win_x2 + 1):
            for y in range(win_y1, win_y2 + 1):
                dx = abs(x - win_cx) / (win_w / 2)
                dy = abs(y - win_cy) / (win_h / 2)
                d = min(1.0, math.sqrt(dx * dx + dy * dy))
                t = (1.0 - d) * 0.7
                base = img.getpixel((x, y))
                img.putpixel((x, y), blend_over(base, LAVA_BRIGHT, t))
        draw = ImageDraw.Draw(img)
    else:
        draw.rectangle([win_x1, win_y1, win_x2, win_y2], fill=WINDOW_DARK)

    # Containment grid bars (vertical)
    num_vbars = 8
    bar_spacing = (win_x2 - win_x1) // (num_vbars + 1)
    bar_color = CHAMBER_GRID if active else WINDOW_GRID
    for i in range(1, num_vbars + 1):
        bx = win_x1 + i * bar_spacing
        draw.line([(bx, win_y1), (bx, win_y2)], fill=bar_color, width=4)

    # Containment grid bars (horizontal)
    num_hbars = 4
    hbar_spacing = (win_y2 - win_y1) // (num_hbars + 1)
    for i in range(1, num_hbars + 1):
        by = win_y1 + i * hbar_spacing
        draw.line([(win_x1, by), (win_x2, by)], fill=bar_color, width=4)

    # Status indicator light (bottom-right of lower panel)
    ind_cx = S - int(S * 0.12)
    ind_cy = base_top + panel_h // 2
    ind_r = 16
    if active:
        draw_filled_circle(draw, ind_cx, ind_cy, ind_r, LAVA_BRIGHT,
                           outline=EDGE_DARK)
        add_radial_glow(img, ind_cx, ind_cy, ind_r + 8, LAVA_GLOW,
                        intensity=0.3)
        draw = ImageDraw.Draw(img)
    else:
        draw_filled_circle(draw, ind_cx, ind_cy, ind_r, ARMOR_MID,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, ind_cx, ind_cy, ind_r - 4, EDGE_DARK)

    # Glow seams when active
    if active:
        # Glow around window frame
        add_glow_ring(img, (win_x1 + win_x2) // 2, (win_y1 + win_y2) // 2,
                      max(win_w, win_h) // 2 - 20,
                      max(win_w, win_h) // 2 + 30, LAVA_GLOW)
        draw = ImageDraw.Draw(img)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Top face — magma core output with heat vents
# ---------------------------------------------------------------------------

def make_top(active=False):
    """Top face: magma core output port with radial heat vents."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Panel seams (cross pattern)
    m = 30
    if active:
        add_glowing_seam(img, (m, cy), (S - m, cy), SEAM_COLOR, LAVA_GLOW,
                         seam_width=3, glow_width=10, intensity=0.15)
        add_glowing_seam(img, (cx, m), (cx, S - m), SEAM_COLOR, LAVA_GLOW,
                         seam_width=3, glow_width=10, intensity=0.15)
        draw = ImageDraw.Draw(img)
    else:
        draw.line([(m, cy), (S - m, cy)], fill=SEAM_COLOR, width=3)
        draw.line([(cx, m), (cx, S - m)], fill=SEAM_COLOR, width=3)

    # Central core port
    core_outer = int(S * 0.25)
    core_inner = int(S * 0.15)

    # Outer housing ring
    draw_filled_circle(draw, cx, cy, core_outer + 10, ARMOR_LIGHT,
                       outline=EDGE_DARK)
    for r in range(core_outer + 8, core_outer - 2, -1):
        t = (core_outer + 8 - r) / 10
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Core interior
    if active:
        # Molten lava core
        draw_filled_circle(draw, cx, cy, core_inner, LAVA_DARK)
        for r in range(core_inner, 0, -1):
            t = 1.0 - (r / core_inner)
            c = lerp_color(LAVA_DARK, LAVA_BRIGHT, t ** 0.8)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)
        add_glow_ring(img, cx, cy, core_inner,
                      core_inner + int(core_inner * 0.5), LAVA_GLOW)
        add_radial_glow(img, cx, cy, core_outer + 20, LAVA_GLOW,
                        intensity=0.25)
        draw = ImageDraw.Draw(img)
    else:
        draw_filled_circle(draw, cx, cy, core_inner, EDGE_DARK,
                           outline=ARMOR_MID)
        # Dark cross pattern on inactive core
        draw.line([(cx - core_inner + 8, cy), (cx + core_inner - 8, cy)],
                  fill=ARMOR_MID, width=4)
        draw.line([(cx, cy - core_inner + 8), (cx, cy + core_inner - 8)],
                  fill=ARMOR_MID, width=4)

    # Gradient ring between outer and inner
    for r in range(core_outer - 2, core_inner, -1):
        t = (core_outer - r) / (core_outer - core_inner)
        if active:
            c = lerp_color(ARMOR_MID, LAVA_DIM, t * 0.5)
        else:
            c = lerp_color(ARMOR_MID, EDGE_DARK, t * 0.4)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Core bolts
    bolt_r = int((core_outer + core_inner) / 2) + 5
    for i in range(8):
        angle = math.radians(45 * i + 22.5)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Heat vent slots in each quadrant
    vent_r_start = core_outer + 30
    vent_r_end = int(S * 0.42)
    for quadrant_angle in [45, 135, 225, 315]:
        for i in range(4):
            angle = math.radians(quadrant_angle + (i - 1.5) * 6)
            x1 = int(cx + vent_r_start * math.cos(angle))
            y1 = int(cy + vent_r_start * math.sin(angle))
            x2 = int(cx + vent_r_end * math.cos(angle))
            y2 = int(cy + vent_r_end * math.sin(angle))
            if active:
                draw.line([(x1, y1), (x2, y2)], fill=EMBER, width=3)
            else:
                draw.line([(x1, y1), (x2, y2)], fill=SEAM_COLOR, width=2)

    # Corner bolts
    quad_positions = [
        (int(S * 0.12), int(S * 0.12)),
        (int(S * 0.88), int(S * 0.12)),
        (int(S * 0.12), int(S * 0.88)),
        (int(S * 0.88), int(S * 0.88)),
    ]
    for bx, by in quad_positions:
        draw_filled_circle(draw, bx, by, 12, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 6, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Bottom face — heavy base plate
# ---------------------------------------------------------------------------

def make_bottom():
    """Bottom face: heavy base plate with ventilation grille."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Raised base plate
    plate_margin = int(S * 0.08)
    draw.rectangle([plate_margin, plate_margin,
                    S - plate_margin, S - plate_margin],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, plate_margin, plate_margin,
                     S - plate_margin, S - plate_margin,
                     ARMOR_LIGHT, EDGE_DARK, width=3)

    # Central ventilation grille
    grille_margin = int(S * 0.2)
    slot_y_start = grille_margin + 20
    slot_y_end = S - grille_margin - 20
    num_slots = 12
    slot_spacing = (slot_y_end - slot_y_start) // num_slots

    for i in range(num_slots):
        sy = slot_y_start + i * slot_spacing
        draw.rectangle([grille_margin + 10, sy,
                        S - grille_margin - 10, sy + slot_spacing - 8],
                       fill=VENT_SLOT, outline=EDGE_DARK)

    # Mounting feet
    foot_size = int(S * 0.08)
    foot_positions = [
        (plate_margin + 10, plate_margin + 10),
        (S - plate_margin - foot_size - 10, plate_margin + 10),
        (plate_margin + 10, S - plate_margin - foot_size - 10),
        (S - plate_margin - foot_size - 10,
         S - plate_margin - foot_size - 10),
    ]
    for fx, fy in foot_positions:
        draw.rectangle([fx, fy, fx + foot_size, fy + foot_size],
                       fill=ARMOR_LIGHT, outline=EDGE_DARK)
        add_bevel_border(draw, fx, fy, fx + foot_size, fy + foot_size,
                         RIVET_COLOR, EDGE_DARK, width=2)
        foot_cx = fx + foot_size // 2
        foot_cy = fy + foot_size // 2
        draw_filled_circle(draw, foot_cx, foot_cy, 12, ARMOR_MID,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, foot_cx, foot_cy, 6, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    textures = {
        "lava_generator_side": make_side(active=False),
        "lava_generator_side_active": make_side(active=True),
        "lava_generator_top": make_top(active=False),
        "lava_generator_top_active": make_top(active=True),
        "lava_generator_bottom": make_bottom(),
    }
    save_textures(textures)


if __name__ == "__main__":
    main()
