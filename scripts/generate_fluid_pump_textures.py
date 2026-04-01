#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Fluid Pump at 1024x1024.

Concept: Heavy industrial pump housing with honeycomb grid. Side face
shows a large circular pump mechanism with pipe connectors. Top has a
pressure gauge and exhaust port. Bottom has ventilation grille and
mounting feet. Active states glow with fluid color.

Creates 9 textures:
  top, bottom, side × 3 states (inactive, active water, active lava)
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

# Pump mechanism
MECH_OUTER = (75, 80, 95, 255)
MECH_MID = (60, 65, 78, 255)
MECH_INNER = (45, 48, 58, 255)
MECH_BOLT = (90, 95, 110, 255)
MECH_HUB = (30, 33, 40, 255)

# Connector / pipe elements
PIPE_RIM = (80, 85, 98, 255)
HOLE_DARK = (12, 14, 18, 255)

# Gauge
GAUGE_BG = (18, 20, 26, 255)
GAUGE_RIM = (85, 90, 102, 255)
GAUGE_TICK = (110, 115, 128, 255)

# Vent grille
VENT_SLOT = (15, 18, 24, 255)

# Fluid colors
FLUID_COLORS = {
    "inactive": None,
    "water": {
        "accent": (0, 212, 255, 255),
        "glow": (0, 229, 255, 255),
        "dim": (0, 150, 200, 255),
        "fill": (33, 150, 243, 255),
    },
    "lava": {
        "accent": (255, 140, 0, 255),
        "glow": (255, 100, 20, 255),
        "dim": (200, 80, 10, 255),
        "fill": (230, 90, 20, 255),
    },
}

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
#  Side face — pump mechanism
# ---------------------------------------------------------------------------

def make_side(state="inactive"):
    """Side face: pump housing with large circular mechanism and connectors."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    fc = FLUID_COLORS[state]
    active = fc is not None

    # Upper panel strip
    panel_h = int(S * 0.10)
    draw.rectangle([9, 9, S - 10, panel_h], fill=ARMOR_MID)
    add_bevel_border(draw, 9, 9, S - 10, panel_h, ARMOR_LIGHT, EDGE_DARK,
                     width=2)

    # Indicator window in upper panel
    win_x = int(S * 0.08)
    win_w = int(S * 0.12)
    win_h = int(panel_h * 0.5)
    win_y = int(panel_h * 0.25)
    indicator_color = fc["accent"] if active else EDGE_DARK
    draw.rectangle([win_x, win_y, win_x + win_w, win_y + win_h],
                   fill=indicator_color, outline=EDGE_DARK)

    # Rivets along upper panel
    for rx in range(int(S * 0.3), S - 30, int(S * 0.12)):
        draw_filled_circle(draw, rx, panel_h // 2 + 5, 5, RIVET_COLOR)

    # Lower base strip
    base_top = S - panel_h
    draw.rectangle([9, base_top, S - 10, S - 10], fill=ARMOR_MID)
    add_bevel_border(draw, 9, base_top, S - 10, S - 10, ARMOR_LIGHT,
                     EDGE_DARK, width=2)
    # Rivets along base
    for rx in range(50, S - 30, int(S * 0.1)):
        draw_filled_circle(draw, rx, base_top + panel_h // 2, 5,
                           RIVET_COLOR)

    # Main pump mechanism — large circle
    mech_r = int(S * 0.25)
    # Outer housing ring
    draw_filled_circle(draw, cx, cy, mech_r + 15, MECH_OUTER,
                       outline=EDGE_DARK)
    # Gradient ring
    for r in range(mech_r + 12, mech_r - 5, -1):
        t = (mech_r + 12 - r) / 17
        c = lerp_color(MECH_OUTER, MECH_MID, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Inner mechanism
    draw_filled_circle(draw, cx, cy, mech_r - 5, MECH_MID,
                       outline=EDGE_DARK)

    # Concentric rings inside mechanism
    inner_r = int(mech_r * 0.6)
    draw_filled_circle(draw, cx, cy, inner_r, MECH_INNER,
                       outline=EDGE_DARK)

    # Center hub
    hub_r = int(mech_r * 0.25)
    hub_color = fc["accent"] if active else MECH_HUB
    draw_filled_circle(draw, cx, cy, hub_r, hub_color, outline=EDGE_DARK)

    # Hub cross pattern
    cross_color = fc["dim"] if active else EDGE_DARK
    draw.line([(cx - hub_r + 5, cy), (cx + hub_r - 5, cy)],
              fill=cross_color, width=4)
    draw.line([(cx, cy - hub_r + 5), (cx, cy + hub_r - 5)],
              fill=cross_color, width=4)

    # Center bolt
    bolt_color = fc["accent"] if active else RIVET_COLOR
    draw_filled_circle(draw, cx, cy, 12, ARMOR_LIGHT, outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, 6, bolt_color)

    # Mechanism bolts (around inner ring)
    bolt_ring_r = int((mech_r - 5 + inner_r) / 2)
    for i in range(8):
        angle = math.radians(45 * i + 22.5)
        bx = int(cx + bolt_ring_r * math.cos(angle))
        by = int(cy + bolt_ring_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, MECH_BOLT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Pressure line from mechanism to upper panel
    line_x = cx
    draw.line([(line_x - 4, panel_h), (line_x - 4, cy - mech_r - 15)],
              fill=PIPE_RIM, width=3)
    draw.line([(line_x + 4, panel_h), (line_x + 4, cy - mech_r - 15)],
              fill=PIPE_RIM, width=3)
    if active:
        draw.line([(line_x, panel_h + 2), (line_x, cy - mech_r - 13)],
                  fill=fc["dim"], width=3)

    # Pipe connectors on left and right edges
    conn_h = int(S * 0.08)
    conn_w = int(S * 0.06)
    for side_x, align in [(9, "left"), (S - 10 - conn_w, "right")]:
        draw.rectangle([side_x, cy - conn_h, side_x + conn_w, cy + conn_h],
                       fill=PIPE_RIM, outline=EDGE_DARK)
        # Pipe opening
        hole_margin = 8
        hole_color = fc["dim"] if active else HOLE_DARK
        draw.rectangle([side_x + hole_margin, cy - conn_h + hole_margin,
                        side_x + conn_w - hole_margin,
                        cy + conn_h - hole_margin],
                       fill=hole_color, outline=EDGE_DARK)

    # Glow effects when active
    if active:
        add_glow_ring(img, cx, cy, inner_r,
                      inner_r + int(inner_r * 0.5), fc["glow"])
        add_radial_glow(img, cx, cy, hub_r + 10, fc["accent"],
                        intensity=0.3)

    draw = ImageDraw.Draw(img)
    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Top face — pressure gauge and exhaust
# ---------------------------------------------------------------------------

def make_top(state="inactive"):
    """Top face: tech plate with pressure gauge, vent, and panel seams."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    fc = FLUID_COLORS[state]
    active = fc is not None

    # Panel seam cross
    m = 30
    seam_points = [
        ((m, cy), (S - m, cy)),
        ((cx, m), (cx, S - m)),
    ]
    for start, end in seam_points:
        if active:
            add_glowing_seam(img, start, end, SEAM_COLOR, fc["glow"],
                             seam_width=3, glow_width=10, intensity=0.15)
        else:
            draw = ImageDraw.Draw(img)
            draw.line([start, end], fill=SEAM_COLOR, width=3)

    draw = ImageDraw.Draw(img)

    # Upper-left quadrant: Pressure gauge
    gauge_cx = int(S * 0.28)
    gauge_cy = int(S * 0.28)
    gauge_r = int(S * 0.14)

    # Gauge bezel
    draw_filled_circle(draw, gauge_cx, gauge_cy, gauge_r + 6, GAUGE_RIM,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, gauge_cx, gauge_cy, gauge_r, GAUGE_BG)

    # Tick marks
    for i in range(9):
        angle = math.radians(225 - i * 11.25)
        tx1 = int(gauge_cx + (gauge_r - 4) * math.cos(angle))
        ty1 = int(gauge_cy - (gauge_r - 4) * math.sin(angle))
        tx2 = int(gauge_cx + (gauge_r - 14) * math.cos(angle))
        ty2 = int(gauge_cy - (gauge_r - 14) * math.sin(angle))
        w = 3 if i % 4 == 0 else 1
        draw.line([(tx1, ty1), (tx2, ty2)], fill=GAUGE_TICK, width=w)

    # Needle
    if active:
        needle_angle = math.radians(315)
        needle_color = fc["accent"]
    else:
        needle_angle = math.radians(225)
        needle_color = GAUGE_TICK

    nx = int(gauge_cx + (gauge_r - 18) * math.cos(needle_angle))
    ny = int(gauge_cy - (gauge_r - 18) * math.sin(needle_angle))
    draw.line([(gauge_cx, gauge_cy), (nx, ny)], fill=needle_color, width=3)
    draw_filled_circle(draw, gauge_cx, gauge_cy, 6, needle_color)

    # Upper-right quadrant: Vent grille
    vent_x1 = int(S * 0.56)
    vent_y1 = int(S * 0.14)
    vent_x2 = int(S * 0.86)
    vent_y2 = int(S * 0.42)
    draw.rectangle([vent_x1, vent_y1, vent_x2, vent_y2],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    # Vent slots
    slot_spacing = (vent_y2 - vent_y1 - 12) // 7
    for i in range(7):
        sy = vent_y1 + 8 + i * slot_spacing
        draw.line([(vent_x1 + 8, sy), (vent_x2 - 8, sy)],
                  fill=VENT_SLOT, width=4)
        if active:
            draw.line([(vent_x1 + 10, sy + 1), (vent_x2 - 10, sy + 1)],
                      fill=fc["dim"], width=2)

    # Lower-left quadrant: ID plate
    plate_x1 = int(S * 0.12)
    plate_y1 = int(S * 0.58)
    plate_x2 = int(S * 0.42)
    plate_y2 = int(S * 0.82)
    draw.rectangle([plate_x1, plate_y1, plate_x2, plate_y2],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, plate_x1, plate_y1, plate_x2, plate_y2,
                     ARMOR_LIGHT, EDGE_DARK, width=2)
    # Etched lines on plate
    for ly in range(plate_y1 + 20, plate_y2 - 15, 18):
        draw.line([(plate_x1 + 15, ly), (plate_x2 - 15, ly)],
                  fill=SEAM_COLOR, width=1)

    # Lower-right quadrant: Exhaust port
    exhaust_cx = int(S * 0.72)
    exhaust_cy = int(S * 0.72)
    exhaust_r = int(S * 0.12)
    draw_filled_circle(draw, exhaust_cx, exhaust_cy, exhaust_r + 6,
                       MECH_OUTER, outline=EDGE_DARK)
    draw_filled_circle(draw, exhaust_cx, exhaust_cy, exhaust_r,
                       EDGE_DARK, outline=MECH_MID)
    exhaust_inner = int(exhaust_r * 0.55)
    exhaust_color = fc["dim"] if active else HOLE_DARK
    draw_filled_circle(draw, exhaust_cx, exhaust_cy, exhaust_inner,
                       exhaust_color, outline=EDGE_DARK)

    if active:
        add_radial_glow(img, exhaust_cx, exhaust_cy, exhaust_r,
                        fc["glow"], intensity=0.2)

    # Corner bolts for each quadrant
    quad_bolt_positions = [
        (int(S * 0.08), int(S * 0.08)),
        (int(S * 0.92), int(S * 0.08)),
        (int(S * 0.08), int(S * 0.92)),
        (int(S * 0.92), int(S * 0.92)),
    ]
    draw = ImageDraw.Draw(img)
    for bx, by in quad_bolt_positions:
        draw_filled_circle(draw, bx, by, 12, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 6, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Bottom face — heavy base plate with ventilation
# ---------------------------------------------------------------------------

def make_bottom(state="inactive"):
    """Bottom face: base plate with ventilation grille and mounting feet."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    fc = FLUID_COLORS[state]
    active = fc is not None

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
    grille_x1 = grille_margin
    grille_x2 = S - grille_margin
    slot_y_start = grille_margin + 20
    slot_y_end = S - grille_margin - 20
    num_slots = 12
    slot_spacing = (slot_y_end - slot_y_start) // num_slots

    for i in range(num_slots):
        sy = slot_y_start + i * slot_spacing
        draw.rectangle([grille_x1 + 10, sy,
                        grille_x2 - 10, sy + slot_spacing - 8],
                       fill=VENT_SLOT, outline=EDGE_DARK)
        if active:
            draw.rectangle([grille_x1 + 14, sy + 2,
                            grille_x2 - 14, sy + slot_spacing - 10],
                           fill=fc["dim"])

    # Mounting feet (4 corners of the base plate)
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

    if active:
        add_radial_glow(img, cx, cy, int(S * 0.3), fc["glow"],
                        intensity=0.1)

    draw = ImageDraw.Draw(img)
    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

STATES = {
    "inactive": "",
    "water": "_active",
    "lava": "_active_lava",
}


def main():
    textures = {}

    for state, suffix in STATES.items():
        textures[f"fluid_pump_top{suffix}"] = make_top(state)
        textures[f"fluid_pump_bottom{suffix}"] = make_bottom(state)
        textures[f"fluid_pump_side{suffix}"] = make_side(state)

    save_textures(textures)


if __name__ == "__main__":
    main()
