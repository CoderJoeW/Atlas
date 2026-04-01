#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Fluid Pipe at 1024x1024.

Concept: Dark armored pipe housing with honeycomb grid. Front shows a
circular pipe opening, back shows a sealed cap, sides show the pipe
body with a directional flow arrow. When fluid is active, the pipe
interior and arrow glow with the fluid's color.

Creates 24 textures:
  front, back, side_{up,down,left,right} × 4 fluid states (none, water, lava, xp)
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

# Pipe interior
PIPE_HOLE = (12, 14, 18, 255)
PIPE_RIM = (80, 85, 98, 255)
PIPE_RIM_LIGHT = (100, 105, 118, 255)

# Pipe body (for side faces)
PIPE_BODY = (55, 60, 72, 255)
PIPE_BODY_LIGHT = (70, 75, 88, 255)
PIPE_BODY_DARK = (35, 38, 48, 255)

# Arrow
ARROW_COLOR = (140, 148, 165, 255)
ARROW_BRIGHT = (180, 188, 205, 255)

# Cap (back face)
CAP_COLOR = (65, 70, 82, 255)
CAP_BOLT = (95, 100, 115, 255)

# Fluid colors
FLUID_COLORS = {
    "none": None,
    "water": {
        "fill": (33, 150, 243, 255),
        "fill_dark": (25, 118, 200, 255),
        "glow": (40, 120, 255, 255),
        "bright": (50, 140, 255, 255),
    },
    "lava": {
        "fill": (230, 90, 20, 255),
        "fill_dark": (180, 60, 10, 255),
        "glow": (255, 140, 40, 255),
        "bright": (255, 160, 50, 255),
    },
    "xp": {
        "fill": (50, 200, 50, 255),
        "fill_dark": (30, 140, 30, 255),
        "glow": (60, 240, 60, 255),
        "bright": (80, 255, 80, 255),
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
#  Front face — pipe opening
# ---------------------------------------------------------------------------

def make_front(fluid="none"):
    """Front face: circular pipe opening with connector ring."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    outer_r = int(S * 0.28)
    mid_r = int(S * 0.22)
    inner_r = int(S * 0.16)

    fc = FLUID_COLORS[fluid]
    active = fc is not None

    # Outer flange ring
    draw_filled_circle(draw, cx, cy, outer_r, PIPE_RIM, outline=EDGE_DARK)

    # Gradient ring from rim to mid
    for r in range(outer_r - 2, mid_r, -1):
        t = (outer_r - r) / (outer_r - mid_r)
        c = lerp_color(PIPE_RIM_LIGHT, ARMOR_MID, t * 0.7)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Pipe wall (between mid and inner)
    for r in range(mid_r, inner_r, -1):
        t = (mid_r - r) / (mid_r - inner_r)
        c = lerp_color(PIPE_BODY, PIPE_BODY_DARK, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Pipe interior
    if active:
        # Fluid fill
        draw_filled_circle(draw, cx, cy, inner_r, fc["fill_dark"])
        # Brighter center
        center_r = int(inner_r * 0.6)
        for r in range(inner_r, 0, -1):
            t = 1.0 - (r / inner_r)
            c = lerp_color(fc["fill_dark"], fc["fill"], t * 0.8)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)
    else:
        draw_filled_circle(draw, cx, cy, inner_r, PIPE_HOLE,
                           outline=EDGE_DARK)

    # Flange bolts (8 bolts around the ring)
    bolt_r = int((outer_r + mid_r) / 2)
    for i in range(8):
        angle = math.radians(45 * i)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Glow when active
    if active:
        add_glow_ring(img, cx, cy, inner_r,
                      inner_r + int(inner_r * 0.4), fc["glow"])
        add_radial_glow(img, cx, cy, inner_r, fc["bright"],
                        intensity=0.25)

    add_corner_bolts(ImageDraw.Draw(img))
    return img


# ---------------------------------------------------------------------------
#  Back face — sealed cap
# ---------------------------------------------------------------------------

def make_back(fluid="none"):
    """Back face: sealed pipe cap with bolts."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    outer_r = int(S * 0.28)
    cap_r = int(S * 0.20)

    fc = FLUID_COLORS[fluid]
    active = fc is not None

    # Outer flange ring
    draw_filled_circle(draw, cx, cy, outer_r, PIPE_RIM, outline=EDGE_DARK)

    # Cap plate
    draw_filled_circle(draw, cx, cy, cap_r, CAP_COLOR, outline=EDGE_DARK)

    # Bevel effect on cap
    for r in range(cap_r, cap_r - 6, -1):
        t = (cap_r - r) / 6
        c = lerp_color(ARMOR_LIGHT, CAP_COLOR, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Center bolt cluster
    draw_filled_circle(draw, cx, cy, 20, ARMOR_LIGHT, outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, 12, CAP_BOLT)
    draw_filled_circle(draw, cx, cy, 6, RIVET_COLOR)

    # Cap bolts in a ring
    bolt_r = int(cap_r * 0.7)
    for i in range(6):
        angle = math.radians(60 * i)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Flange bolts
    flange_r = int((outer_r + cap_r) / 2) + 5
    for i in range(8):
        angle = math.radians(45 * i)
        bx = int(cx + flange_r * math.cos(angle))
        by = int(cy + flange_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Subtle glow around flange when active
    if active:
        add_glow_ring(img, cx, cy, outer_r,
                      outer_r + int(outer_r * 0.25), fc["glow"])

    add_corner_bolts(ImageDraw.Draw(img))
    return img


# ---------------------------------------------------------------------------
#  Side face — pipe body with directional arrow (pointing up)
# ---------------------------------------------------------------------------

def _draw_arrow_up(draw, cx, cy, arrow_color):
    """Draw a flow direction arrow pointing up."""
    # Arrow head (triangle)
    head_top = cy - int(S * 0.28)
    head_base = cy - int(S * 0.14)
    head_half_w = int(S * 0.12)

    draw.polygon([
        (cx, head_top),
        (cx - head_half_w, head_base),
        (cx + head_half_w, head_base),
    ], fill=arrow_color)

    # Arrow shaft
    shaft_top = head_base
    shaft_bottom = cy + int(S * 0.28)
    shaft_half_w = int(S * 0.04)

    draw.rectangle([
        cx - shaft_half_w, shaft_top,
        cx + shaft_half_w, shaft_bottom
    ], fill=arrow_color)


def make_side_up(fluid="none"):
    """Side face with flow arrow pointing up (base for rotation)."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    fc = FLUID_COLORS[fluid]
    active = fc is not None

    # Pipe body band (central horizontal strip)
    pipe_half = int(S * 0.18)
    pipe_y1 = 0
    pipe_y2 = S

    # Left and right pipe walls
    wall_left = cx - pipe_half
    wall_right = cx + pipe_half
    draw.rectangle([wall_left, pipe_y1, wall_right, pipe_y2],
                   fill=PIPE_BODY)

    # Pipe wall highlights
    draw.line([(wall_left, pipe_y1), (wall_left, pipe_y2)],
              fill=PIPE_BODY_LIGHT, width=3)
    draw.line([(wall_right, pipe_y1), (wall_right, pipe_y2)],
              fill=PIPE_BODY_DARK, width=3)

    # Pipe wall bevel
    for i in range(6):
        t = i / 6
        c = lerp_color(ARMOR_DARK, PIPE_BODY, t)
        draw.line([(wall_left - 6 + i, pipe_y1),
                   (wall_left - 6 + i, pipe_y2)], fill=c)
        c2 = lerp_color(PIPE_BODY, ARMOR_DARK, t)
        draw.line([(wall_right + 1 + i, pipe_y1),
                   (wall_right + 1 + i, pipe_y2)], fill=c2)

    # Fluid fill inside pipe
    if active:
        fill_half = int(S * 0.10)
        fill_left = cx - fill_half
        fill_right = cx + fill_half
        draw.rectangle([fill_left, 0, fill_right, S],
                       fill=fc["fill_dark"])
        # Brighter center stripe
        center_half = int(S * 0.04)
        draw.rectangle([cx - center_half, 0, cx + center_half, S],
                       fill=fc["fill"])

    # Joint flanges (horizontal bands across the pipe)
    for flange_y in [int(S * 0.15), int(S * 0.85)]:
        flange_h = 12
        draw.rectangle([wall_left - 8, flange_y - flange_h,
                        wall_right + 8, flange_y + flange_h],
                       fill=PIPE_RIM, outline=EDGE_DARK)
        # Flange bolts
        for fbx in [wall_left + 20, wall_right - 20]:
            draw_filled_circle(draw, fbx, flange_y, 8, ARMOR_LIGHT,
                               outline=EDGE_DARK)
            draw_filled_circle(draw, fbx, flange_y, 4, RIVET_COLOR)

    # Flow direction arrow
    arrow_c = fc["bright"] if active else ARROW_COLOR
    _draw_arrow_up(draw, cx, cy, arrow_c)

    # Arrow outline for visibility
    if active:
        # Glow along pipe edges
        for x in range(max(0, wall_left - 15), min(S, wall_right + 16)):
            for y_off in range(8):
                dl = abs(x - wall_left)
                dr = abs(x - wall_right)
                d = min(dl, dr)
                if d < 8:
                    t = 0.2 * (1.0 - d / 8)
                    for y in range(0, S, 4):
                        if 0 <= y < S:
                            base = img.getpixel((x, y))
                            img.putpixel((x, y),
                                         blend_over(base, fc["glow"], t))
                break  # only need outer loop once per x

    # Seam lines on armor sections
    m = 30
    seam_y = cy
    if not active:
        draw = ImageDraw.Draw(img)
        draw.line([(m, seam_y), (wall_left - 10, seam_y)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(wall_right + 10, seam_y), (S - m, seam_y)],
                  fill=SEAM_COLOR, width=2)
    else:
        add_glowing_seam(
            img, (m, seam_y), (wall_left - 10, seam_y),
            SEAM_COLOR, fc["glow"],
            seam_width=2, glow_width=8, intensity=0.15
        )
        add_glowing_seam(
            img, (wall_right + 10, seam_y), (S - m, seam_y),
            SEAM_COLOR, fc["glow"],
            seam_width=2, glow_width=8, intensity=0.15
        )

    draw = ImageDraw.Draw(img)
    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Public face functions + rotation
# ---------------------------------------------------------------------------

def make_side_down(fluid="none"):
    return make_side_up(fluid).rotate(180)


def make_side_left(fluid="none"):
    return make_side_up(fluid).rotate(90)


def make_side_right(fluid="none"):
    return make_side_up(fluid).rotate(-90)


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

FLUID_SUFFIXES = {"none": "", "water": "_filled", "lava": "_filled_lava",
                  "xp": "_filled_xp"}


def main():
    textures = {}

    for fluid, suffix in FLUID_SUFFIXES.items():
        textures[f"fluid_pipe_front{suffix}"] = make_front(fluid)
        textures[f"fluid_pipe_back{suffix}"] = make_back(fluid)
        textures[f"fluid_pipe_side{suffix}_up"] = make_side_up(fluid)
        textures[f"fluid_pipe_side{suffix}_down"] = make_side_down(fluid)
        textures[f"fluid_pipe_side{suffix}_left"] = make_side_left(fluid)
        textures[f"fluid_pipe_side{suffix}_right"] = make_side_right(fluid)

    save_textures(textures)


if __name__ == "__main__":
    main()
