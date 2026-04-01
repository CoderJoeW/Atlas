#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Fluid Merger at 1024x1024.

Concept: Industrial fluid merging device that combines two fluid inputs
into a single output. The block visually communicates its function:
  - Front: Single large output port with downward flow arrow
  - Back: Twin input ports side by side
  - Side: Y-junction manifold showing two pipes merging into one
  - Top: Access hatch with fluid status indicator
  - Bottom: Reinforced base plate with drain

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

ARMOR_DARK = (38, 40, 48, 255)
ARMOR_MID = (52, 56, 66, 255)
ARMOR_LIGHT = (70, 75, 88, 255)
HEX_LINE = (48, 52, 62, 255)
EDGE_DARK = (22, 24, 30, 255)
SEAM_COLOR = (30, 33, 40, 255)
RIVET_COLOR = (100, 108, 125, 255)

# Pipe colors
PIPE_DARK = (42, 46, 56, 255)
PIPE_MID = (58, 62, 74, 255)
PIPE_LIGHT = (74, 78, 92, 255)
PIPE_EDGE = (28, 30, 38, 255)

# Connector hole
CONNECTOR_HOLE = (12, 14, 18, 255)

# Default (no fluid) — cyan/teal
CYAN_RING = (30, 170, 190, 255)
CYAN_BRIGHT = (50, 225, 250, 255)
CYAN_GLOW = (40, 210, 235, 255)
CYAN_DIM = (20, 100, 115, 255)

# Fluid state colors
FLUID_COLORS = {
    "none": {
        "ring": CYAN_RING, "bright": CYAN_BRIGHT,
        "glow": CYAN_GLOW, "dim": CYAN_DIM, "active": False,
    },
    "water": {
        "ring": (30, 100, 220, 255), "bright": (60, 150, 255, 255),
        "glow": (40, 130, 255, 255), "dim": (20, 65, 140, 255),
        "active": True,
    },
    "lava": {
        "ring": (220, 110, 25, 255), "bright": (255, 165, 50, 255),
        "glow": (255, 140, 35, 255), "dim": (140, 60, 15, 255),
        "active": True,
    },
    "xp": {
        "ring": (45, 195, 50, 255), "bright": (80, 255, 85, 255),
        "glow": (60, 235, 65, 255), "dim": (25, 120, 30, 255),
        "active": True,
    },
}

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


def draw_pipe_connector(draw, img, cx, cy, r_outer, r_inner,
                        fc, bolt_count=6):
    """Draw a bolted pipe connector flange."""
    active = fc["active"]
    ring = fc["bright"] if active else fc["ring"]

    # Outer flange
    draw.ellipse([cx - r_outer, cy - r_outer,
                  cx + r_outer, cy + r_outer],
                 fill=ARMOR_MID, outline=EDGE_DARK)
    for r in range(r_outer, r_outer - 6, -1):
        t = (r_outer - r) / 6
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Inner groove
    groove_r = r_inner + 6
    draw.ellipse([cx - groove_r, cy - groove_r,
                  cx + groove_r, cy + groove_r],
                 outline=EDGE_DARK)

    # Glowing ring inside connector
    for r in range(r_inner + 5, r_inner, -1):
        t = (r - r_inner) / 5
        c = lerp_color(CONNECTOR_HOLE, ring, t * 0.7)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Pipe opening
    draw_filled_circle(draw, cx, cy, r_inner, CONNECTOR_HOLE)

    # Contact pins
    pin_r = int(r_inner * 0.55)
    pin_size = max(3, r_inner // 10)
    for angle_deg in [0, 90, 180, 270]:
        angle = math.radians(angle_deg)
        px = int(cx + pin_r * math.cos(angle))
        py = int(cy + pin_r * math.sin(angle))
        draw_filled_circle(draw, px, py, pin_size, ring)

    # Bolts
    for i in range(bolt_count):
        angle = math.radians(360 * i / bolt_count + 30)
        bx = int(cx + (r_outer - 10) * math.cos(angle))
        by = int(cy + (r_outer - 10) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 6, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 3, RIVET_COLOR)

    if active:
        add_glow_ring(img, cx, cy, r_outer,
                      r_outer + int(r_outer * 0.3), fc["glow"])
        add_radial_glow(img, cx, cy, r_inner, fc["bright"],
                        intensity=0.25)


def draw_pipe_segment(draw, x1, y1, x2, y2):
    """Draw a rectangular pipe segment with metallic shading."""
    pw = abs(x2 - x1) if abs(x2 - x1) > abs(y2 - y1) else abs(y2 - y1)
    # Determine orientation
    if abs(x2 - x1) > abs(y2 - y1):
        # Horizontal pipe
        for y in range(min(y1, y2), max(y1, y2) + 1):
            mid_y = (y1 + y2) / 2
            t = abs(y - mid_y) / max(1, abs(y2 - y1) / 2)
            c = lerp_color(PIPE_LIGHT, PIPE_DARK, t)
            draw.line([(min(x1, x2), y), (max(x1, x2), y)], fill=c)
    else:
        # Vertical pipe
        for x in range(min(x1, x2), max(x1, x2) + 1):
            mid_x = (x1 + x2) / 2
            t = abs(x - mid_x) / max(1, abs(x2 - x1) / 2)
            c = lerp_color(PIPE_LIGHT, PIPE_DARK, t)
            draw.line([(x, min(y1, y2)), (x, max(y1, y2))], fill=c)
    # Edge lines
    draw.rectangle([min(x1, x2), min(y1, y2),
                    max(x1, x2), max(y1, y2)], outline=PIPE_EDGE)


def draw_flow_arrow(draw, cx, cy, size, color, direction="down"):
    """Draw a directional flow arrow."""
    hs = size // 2
    if direction == "down":
        points = [
            (cx - hs, cy - hs // 2),
            (cx, cy + hs),
            (cx + hs, cy - hs // 2),
        ]
    elif direction == "up":
        points = [
            (cx - hs, cy + hs // 2),
            (cx, cy - hs),
            (cx + hs, cy + hs // 2),
        ]
    elif direction == "right":
        points = [
            (cx - hs // 2, cy - hs),
            (cx + hs, cy),
            (cx - hs // 2, cy + hs),
        ]
    else:
        points = [
            (cx + hs // 2, cy - hs),
            (cx - hs, cy),
            (cx + hs // 2, cy + hs),
        ]
    draw.polygon(points, fill=color)
    # Outline
    draw.polygon(points, outline=lerp_color(color, EDGE_DARK, 0.4))


# ---------------------------------------------------------------------------
#  FRONT (OUTPUT) - Single large output port with flow arrow
# ---------------------------------------------------------------------------

def make_front(fluid="none"):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    fc = FLUID_COLORS[fluid]

    # Large output connector
    draw_pipe_connector(draw, img, cx, cy, int(S * 0.24),
                        int(S * 0.14), fc, bolt_count=8)

    # Flow arrow below connector pointing down (output direction)
    arrow_y = int(S * 0.82)
    arrow_color = fc["bright"] if fc["active"] else fc["dim"]
    draw_flow_arrow(draw, cx, arrow_y, 70, arrow_color, "down")

    # "OUTPUT" label plate above connector
    lp_w = int(S * 0.30)
    lp_h = 30
    lp_y = int(S * 0.10)
    draw.rectangle([cx - lp_w // 2, lp_y,
                    cx + lp_w // 2, lp_y + lp_h],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, cx - lp_w // 2, lp_y,
                     cx + lp_w // 2, lp_y + lp_h,
                     ARMOR_LIGHT, EDGE_DARK, width=2)
    # Two small bars suggesting text
    bar_w = lp_w // 3
    draw.rectangle([cx - bar_w // 2, lp_y + 10,
                    cx + bar_w // 2, lp_y + 14],
                   fill=(45, 50, 60, 255))
    draw.rectangle([cx - bar_w // 3, lp_y + 18,
                    cx + bar_w // 3, lp_y + 22],
                   fill=(40, 44, 54, 255))

    # Horizontal seams
    m = 30
    if fc["active"]:
        add_glowing_seam(
            img, (m, cy), (cx - int(S * 0.28), cy),
            SEAM_COLOR, fc["glow"],
            seam_width=2, glow_width=8, intensity=0.12
        )
        add_glowing_seam(
            img, (cx + int(S * 0.28), cy), (S - m, cy),
            SEAM_COLOR, fc["glow"],
            seam_width=2, glow_width=8, intensity=0.12
        )
    else:
        draw.line([(m, cy), (cx - int(S * 0.28), cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(cx + int(S * 0.28), cy), (S - m, cy)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  BACK (INPUT) - Twin input ports
# ---------------------------------------------------------------------------

def make_back(fluid="none"):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    fc = FLUID_COLORS[fluid]

    # Twin input connectors (side by side)
    port_r_outer = int(S * 0.16)
    port_r_inner = int(S * 0.09)
    spacing = int(S * 0.23)

    for pcx in [cx - spacing, cx + spacing]:
        draw_pipe_connector(draw, img, pcx, cy, port_r_outer,
                            port_r_inner, fc, bolt_count=6)

    # Flow arrows above each port pointing into the block
    arrow_color = fc["bright"] if fc["active"] else fc["dim"]
    for pcx in [cx - spacing, cx + spacing]:
        draw_flow_arrow(draw, pcx, int(S * 0.18), 55,
                        arrow_color, "down")

    # Merge indicator between the two ports (converging lines)
    mid_y = cy
    merge_color = fc["ring"] if not fc["active"] else fc["bright"]
    # Left line angling toward center
    draw.line([(cx - spacing + port_r_outer + 6, mid_y),
               (cx, mid_y + int(S * 0.12))],
              fill=merge_color, width=4)
    # Right line angling toward center
    draw.line([(cx + spacing - port_r_outer - 6, mid_y),
               (cx, mid_y + int(S * 0.12))],
              fill=merge_color, width=4)
    # Central merge point dot
    draw_filled_circle(draw, cx, mid_y + int(S * 0.12), 8,
                       merge_color, outline=EDGE_DARK)

    # "INPUT" label plate
    lp_w = int(S * 0.26)
    lp_h = 28
    lp_y = int(S * 0.86)
    draw.rectangle([cx - lp_w // 2, lp_y,
                    cx + lp_w // 2, lp_y + lp_h],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, cx - lp_w // 2, lp_y,
                     cx + lp_w // 2, lp_y + lp_h,
                     ARMOR_LIGHT, EDGE_DARK, width=2)
    bar_w = lp_w // 3
    draw.rectangle([cx - bar_w // 2, lp_y + 8,
                    cx + bar_w // 2, lp_y + 12],
                   fill=(45, 50, 60, 255))

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  SIDE - Y-junction manifold (two pipes merging into one)
# ---------------------------------------------------------------------------

def make_side(fluid="none"):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    fc = FLUID_COLORS[fluid]

    pipe_w = 40  # half-width of pipe
    merge_y = int(S * 0.50)  # where the Y merges

    # Two input pipes from the top (left and right)
    input_left_x = int(S * 0.28)
    input_right_x = int(S * 0.72)
    pipe_top = int(S * 0.08)

    # Output pipe going down from merge point
    output_bottom = int(S * 0.92)

    # Draw output pipe first (bottom half, centered)
    draw_pipe_segment(draw, cx - pipe_w, merge_y,
                      cx + pipe_w, output_bottom)

    # Left input pipe (vertical down to merge)
    draw_pipe_segment(draw, input_left_x - pipe_w, pipe_top,
                      input_left_x + pipe_w, merge_y - 20)

    # Right input pipe (vertical down to merge)
    draw_pipe_segment(draw, input_right_x - pipe_w, pipe_top,
                      input_right_x + pipe_w, merge_y - 20)

    # Diagonal sections connecting inputs to merge point
    # Left diagonal
    diag_points_l = [
        (input_left_x - pipe_w, merge_y - 22),
        (input_left_x + pipe_w, merge_y - 22),
        (cx + pipe_w, merge_y + 2),
        (cx - pipe_w, merge_y + 2),
    ]
    draw.polygon(diag_points_l, fill=PIPE_MID, outline=PIPE_EDGE)
    # Left diagonal highlight
    for i in range(3):
        t = i / 3
        lx1 = int(input_left_x + t * (cx - input_left_x))
        ly1 = int((merge_y - 20) + t * 22)
        draw.line([(lx1, ly1), (lx1, ly1)],
                  fill=PIPE_LIGHT, width=2)

    # Right diagonal
    diag_points_r = [
        (input_right_x - pipe_w, merge_y - 22),
        (input_right_x + pipe_w, merge_y - 22),
        (cx + pipe_w, merge_y + 2),
        (cx - pipe_w, merge_y + 2),
    ]
    draw.polygon(diag_points_r, fill=PIPE_MID, outline=PIPE_EDGE)

    # Merge junction ring
    junction_r = pipe_w + 16
    draw.ellipse([cx - junction_r, merge_y - junction_r,
                  cx + junction_r, merge_y + junction_r],
                 fill=ARMOR_MID, outline=EDGE_DARK)
    for r in range(junction_r, junction_r - 6, -1):
        t = (junction_r - r) / 6
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([cx - r, merge_y - r, cx + r, merge_y + r],
                     outline=c)

    # Inner pipe opening at junction
    inner_r = pipe_w - 4
    junction_color = fc["ring"] if not fc["active"] else fc["bright"]
    for r in range(inner_r + 4, inner_r, -1):
        t = (r - inner_r) / 4
        c = lerp_color(CONNECTOR_HOLE, junction_color, t * 0.6)
        draw.ellipse([cx - r, merge_y - r, cx + r, merge_y + r],
                     outline=c)
    draw_filled_circle(draw, cx, merge_y, inner_r, CONNECTOR_HOLE)

    # Junction bolts
    for i in range(6):
        angle = math.radians(60 * i)
        bx = int(cx + (junction_r - 8) * math.cos(angle))
        by = int(merge_y + (junction_r - 8) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 5, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 2, RIVET_COLOR)

    # Pipe clamp brackets
    for clamp_y in [pipe_top + 60, merge_y - 80,
                    merge_y + 60, output_bottom - 60]:
        if clamp_y < merge_y - 30:
            # Clamps on input pipes
            for px in [input_left_x, input_right_x]:
                ch = 12
                draw.rectangle([px - pipe_w - 8, clamp_y,
                                px + pipe_w + 8, clamp_y + ch],
                               fill=ARMOR_LIGHT, outline=EDGE_DARK)
                draw_filled_circle(draw, px - pipe_w - 4,
                                   clamp_y + ch // 2, 3, RIVET_COLOR)
                draw_filled_circle(draw, px + pipe_w + 4,
                                   clamp_y + ch // 2, 3, RIVET_COLOR)
        elif clamp_y > merge_y + 20:
            # Clamps on output pipe
            ch = 12
            draw.rectangle([cx - pipe_w - 8, clamp_y,
                            cx + pipe_w + 8, clamp_y + ch],
                           fill=ARMOR_LIGHT, outline=EDGE_DARK)
            draw_filled_circle(draw, cx - pipe_w - 4,
                               clamp_y + ch // 2, 3, RIVET_COLOR)
            draw_filled_circle(draw, cx + pipe_w + 4,
                               clamp_y + ch // 2, 3, RIVET_COLOR)

    # Flow arrows on pipes
    arrow_color = fc["bright"] if fc["active"] else fc["dim"]
    # Input arrows (pointing down)
    for px in [input_left_x, input_right_x]:
        draw_flow_arrow(draw, px, pipe_top + 30, 30,
                        arrow_color, "down")
    # Output arrow (pointing down)
    draw_flow_arrow(draw, cx, output_bottom - 40, 35,
                    arrow_color, "down")

    if fc["active"]:
        add_radial_glow(img, cx, merge_y, junction_r + 15,
                        fc["glow"], intensity=0.15)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  TOP - Access hatch with fluid status indicator
# ---------------------------------------------------------------------------

def make_top(fluid="none"):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    fc = FLUID_COLORS[fluid]

    # Hatch panel (square, recessed)
    hatch_size = int(S * 0.52)
    hx1 = cx - hatch_size // 2
    hy1 = cy - hatch_size // 2
    hx2 = cx + hatch_size // 2
    hy2 = cy + hatch_size // 2

    draw.rectangle([hx1, hy1, hx2, hy2], fill=ARMOR_MID)
    add_bevel_border(draw, hx1, hy1, hx2, hy2,
                     EDGE_DARK, ARMOR_LIGHT, width=5)

    # Cross seams on hatch
    draw.line([(hx1 + 20, cy), (hx2 - 20, cy)],
              fill=SEAM_COLOR, width=3)
    draw.line([(cx, hy1 + 20), (cx, hy2 - 20)],
              fill=SEAM_COLOR, width=3)

    # Central status indicator (circular)
    indicator_r = int(S * 0.10)
    indicator_color = fc["bright"] if fc["active"] else fc["dim"]

    draw_filled_circle(draw, cx, cy, indicator_r + 6,
                       ARMOR_LIGHT, outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, indicator_r, indicator_color)
    draw_filled_circle(draw, cx, cy, indicator_r - 8,
                       CONNECTOR_HOLE)

    if fc["active"]:
        add_radial_glow(img, cx, cy, indicator_r + 25,
                        fc["glow"], intensity=0.18)

    # Hatch bolts (8 around perimeter)
    bolt_off = 18
    bolt_spots = [
        (hx1 + bolt_off, hy1 + bolt_off),
        (hx2 - bolt_off, hy1 + bolt_off),
        (hx1 + bolt_off, hy2 - bolt_off),
        (hx2 - bolt_off, hy2 - bolt_off),
        (cx, hy1 + bolt_off),
        (cx, hy2 - bolt_off),
        (hx1 + bolt_off, cy),
        (hx2 - bolt_off, cy),
    ]
    for bx, by in bolt_spots:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Corner seams from hatch to face corners
    m = 30
    if fc["active"]:
        for sx, sy, ex, ey in [
            (m, m, hx1 - 6, hy1 - 6),
            (S - m, m, hx2 + 6, hy1 - 6),
            (m, S - m, hx1 - 6, hy2 + 6),
            (S - m, S - m, hx2 + 6, hy2 + 6),
        ]:
            add_glowing_seam(img, (sx, sy), (ex, ey),
                             SEAM_COLOR, fc["glow"],
                             seam_width=2, glow_width=6,
                             intensity=0.08)
    else:
        for sx, sy, ex, ey in [
            (m, m, hx1 - 6, hy1 - 6),
            (S - m, m, hx2 + 6, hy1 - 6),
            (m, S - m, hx1 - 6, hy2 + 6),
            (S - m, S - m, hx2 + 6, hy2 + 6),
        ]:
            draw.line([(sx, sy), (ex, ey)],
                      fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  BOTTOM - Reinforced base plate
# ---------------------------------------------------------------------------

def make_bottom(fluid="none"):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    # Central drain grille
    grille_size = 320
    g1 = cx - grille_size // 2
    g2 = cx + grille_size // 2
    draw.rectangle([g1, g1, g2, g2], fill=ARMOR_MID,
                   outline=ARMOR_LIGHT)
    add_bevel_border(draw, g1, g1, g2, g2, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Vent slots
    for vy in range(g1 + 24, g2 - 16, 22):
        draw.rectangle([g1 + 18, vy, g2 - 18, vy + 8],
                       fill=EDGE_DARK)
        draw.rectangle([g1 + 20, vy + 1, g2 - 20, vy + 7],
                       fill=(15, 17, 22, 255))

    # Mounting feet
    foot_size = 65
    foot_inset = 55
    for fx, fy in [(foot_inset, foot_inset),
                   (S - foot_inset - foot_size, foot_inset),
                   (foot_inset, S - foot_inset - foot_size),
                   (S - foot_inset - foot_size,
                    S - foot_inset - foot_size)]:
        draw.rectangle([fx, fy, fx + foot_size, fy + foot_size],
                       fill=ARMOR_LIGHT, outline=EDGE_DARK)
        add_bevel_border(draw, fx, fy, fx + foot_size,
                         fy + foot_size,
                         (80, 86, 100, 255), EDGE_DARK, width=2)
        fcx = fx + foot_size // 2
        fcy = fy + foot_size // 2
        draw_filled_circle(draw, fcx, fcy, 12, ARMOR_MID,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, fcx, fcy, 6, RIVET_COLOR)

    # Cross seams
    draw.line([(cx, foot_inset + foot_size), (cx, g1)],
              fill=SEAM_COLOR, width=3)
    draw.line([(cx, g2), (cx, S - foot_inset - foot_size)],
              fill=SEAM_COLOR, width=3)
    draw.line([(foot_inset + foot_size, cy), (g1, cy)],
              fill=SEAM_COLOR, width=3)
    draw.line([(g2, cy), (S - foot_inset - foot_size, cy)],
              fill=SEAM_COLOR, width=3)

    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

FLUID_SUFFIXES = {"none": "", "water": "_water", "lava": "_lava",
                  "xp": "_xp"}

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
