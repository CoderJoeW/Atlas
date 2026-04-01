#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Fluid Container at 1024x1024.

Concept: Heavy-duty industrial pressure vessel with a thick-framed glass
viewport on the front showing internal pipe structure and fluid level.
Sides feature dual vertical sight-glass gauges flanked by reinforcement
ribs. Back has twin pipe connectors with a pressure relief valve. Top
has a heavy bolted fill hatch. Bottom is a reinforced base plate.

Creates 60 textures:
  6 faces (north, south, east, west, up, down) x 10 appearances:
    empty, water_low, water_medium, water_full,
    lava_low, lava_medium, lava_full,
    xp_low, xp_medium, xp_full
"""

import math
import random
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

# Glass / window
GLASS_DARK = (12, 16, 24, 255)
GLASS_TINT = (18, 22, 32, 255)

# Internal tank structure colors
TANK_PIPE = (42, 46, 56, 255)
TANK_PIPE_HI = (58, 62, 74, 255)
TANK_BRACKET = (50, 54, 64, 255)
TANK_WELD = (35, 38, 46, 255)

# Fluid colors - water (blue)
WATER_DARK = (15, 50, 110, 255)
WATER_MID = (25, 90, 170, 255)
WATER_LIGHT = (50, 140, 210, 255)
WATER_SURFACE = (70, 175, 235, 255)
WATER_GLOW = (40, 140, 220, 255)
WATER_BUBBLE = (100, 190, 245, 180)

# Fluid colors - lava (orange/red)
LAVA_DARK = (110, 25, 5, 255)
LAVA_MID = (190, 70, 10, 255)
LAVA_LIGHT = (235, 130, 25, 255)
LAVA_SURFACE = (255, 175, 45, 255)
LAVA_GLOW = (255, 120, 20, 255)
LAVA_BUBBLE = (255, 200, 80, 200)

# Fluid colors - experience (green)
XP_DARK = (10, 70, 15, 255)
XP_MID = (25, 130, 35, 255)
XP_LIGHT = (50, 190, 60, 255)
XP_SURFACE = (90, 235, 95, 255)
XP_GLOW = (50, 200, 60, 255)
XP_BUBBLE = (120, 240, 130, 180)

# Gauge colors
GAUGE_BG = (10, 13, 20, 255)
GAUGE_FRAME = (55, 60, 72, 255)
GAUGE_TICK = (40, 44, 54, 255)
GAUGE_TICK_MAJOR = (65, 70, 82, 255)

FILL_LEVELS = {"low": 0.25, "medium": 0.60, "full": 0.92}

FLUID_COLORS = {
    "water": (WATER_DARK, WATER_MID, WATER_LIGHT, WATER_SURFACE,
              WATER_GLOW, WATER_BUBBLE),
    "lava": (LAVA_DARK, LAVA_MID, LAVA_LIGHT, LAVA_SURFACE,
             LAVA_GLOW, LAVA_BUBBLE),
    "xp": (XP_DARK, XP_MID, XP_LIGHT, XP_SURFACE,
            XP_GLOW, XP_BUBBLE),
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


def draw_pipe_connector(draw, img, cx, cy, r_outer, r_inner,
                        fluid_type=None, fill_frac=0, bolt_count=6):
    """Draw a bolted pipe connector flange with optional fluid glow."""
    # Outer flange ring
    draw.ellipse([cx - r_outer, cy - r_outer,
                  cx + r_outer, cy + r_outer],
                 fill=ARMOR_MID, outline=EDGE_DARK)
    # Bevel on flange
    for r in range(r_outer, r_outer - 6, -1):
        t = (r_outer - r) / 6
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Inner ring groove
    groove_r = r_inner + 6
    draw.ellipse([cx - groove_r, cy - groove_r,
                  cx + groove_r, cy + groove_r],
                 outline=EDGE_DARK)

    # Pipe opening
    if fluid_type is not None and fill_frac > 0:
        _, _, _, _, glow, _ = FLUID_COLORS[fluid_type]
        draw_filled_circle(draw, cx, cy, r_inner, glow)
        draw_filled_circle(draw, cx, cy, r_inner - 8, EDGE_DARK)
        # Center dot
        draw_filled_circle(draw, cx, cy, 4, glow)
        add_radial_glow(img, cx, cy, r_outer + 15, glow,
                        intensity=0.15)
    else:
        draw_filled_circle(draw, cx, cy, r_inner, EDGE_DARK)
        draw_filled_circle(draw, cx, cy, 4, (30, 33, 40, 255))

    # Bolts around flange
    for i in range(bolt_count):
        angle = math.radians(360 * i / bolt_count + 30)
        bx = int(cx + (r_outer - 10) * math.cos(angle))
        by = int(cy + (r_outer - 10) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 6, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 3, RIVET_COLOR)


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
#  Fluid drawing
# ---------------------------------------------------------------------------

def draw_fluid_body(draw, img, x1, y1, x2, y2, fill_frac,
                    fluid_type, bubbles=True):
    """Fill a region with fluid, surface line, shimmer, and bubbles."""
    if fluid_type is None or fill_frac <= 0:
        return

    dark, mid, light, surface, glow, bubble_c = FLUID_COLORS[fluid_type]
    total_h = y2 - y1
    fluid_h = int(total_h * fill_frac)
    fluid_top = y2 - fluid_h

    # Fluid body gradient
    for y in range(fluid_top, y2 + 1):
        t = (y - fluid_top) / max(1, fluid_h)
        c = lerp_color(light, dark, t)
        draw.line([(x1, y), (x2, y)], fill=c)

    # Surface band (thicker, more visible)
    surface_h = max(4, int(fluid_h * 0.04))
    for dy in range(surface_h):
        t = dy / max(1, surface_h)
        c = lerp_color(surface, light, t)
        draw.line([(x1, fluid_top + dy), (x2, fluid_top + dy)],
                  fill=c)

    # Shimmer highlights on surface
    rng = random.Random(42)  # deterministic
    w = x2 - x1
    for sx in range(x1 + 15, x2 - 40, 60):
        sw = 20 + rng.randint(0, 25)
        draw.line([(sx, fluid_top + 1), (sx + sw, fluid_top + 1)],
                  fill=(*surface[:3], 180), width=2)

    # Bubbles (deterministic positions)
    if bubbles and fluid_h > 60:
        for i in range(18):
            bx = x1 + 20 + rng.randint(0, w - 40)
            by = fluid_top + 20 + rng.randint(0, max(1, fluid_h - 50))
            br = 4 + rng.randint(0, 8)
            # Only draw if within fluid
            if by + br < y2 - 5 and by - br > fluid_top + surface_h:
                ba = 40 + rng.randint(0, 60)
                bc = (*bubble_c[:3], ba)
                draw.ellipse([bx - br, by - br, bx + br, by + br],
                             outline=bc)
                # Tiny highlight dot
                draw.ellipse([bx - 1, by - br + 2, bx + 1, by - br + 4],
                             fill=(*surface[:3], ba))

    # Subtle internal glow
    fcx = (x1 + x2) // 2
    fcy = (fluid_top + y2) // 2
    glow_r = min(fluid_h, x2 - x1) // 3
    if glow_r > 20:
        add_radial_glow(img, fcx, fcy, glow_r, glow, intensity=0.10)


def draw_gauge(draw, img, gx, y1, y2, gauge_w, fill_frac,
               fluid_type, ticks_right=True):
    """Draw a vertical sight-glass gauge with graduated markings."""
    # Recessed groove behind gauge
    draw.rectangle([gx - 3, y1 - 3, gx + gauge_w + 3, y2 + 3],
                   fill=EDGE_DARK)

    # Gauge tube background
    draw.rectangle([gx, y1, gx + gauge_w, y2], fill=GAUGE_BG)

    # Glass edge highlights
    draw.line([(gx, y1), (gx, y2)], fill=GAUGE_FRAME, width=2)
    draw.line([(gx + gauge_w, y1), (gx + gauge_w, y2)],
              fill=GAUGE_FRAME, width=2)
    draw.line([(gx, y1), (gx + gauge_w, y1)],
              fill=GAUGE_FRAME, width=2)
    draw.line([(gx, y2), (gx + gauge_w, y2)],
              fill=GAUGE_FRAME, width=2)

    # Graduated tick marks
    gauge_h = y2 - y1
    num_major = 4
    num_minor = 12
    tick_side = gx + gauge_w + 4 if ticks_right else gx - 4

    for i in range(num_minor + 1):
        ty = y1 + int(i * gauge_h / num_minor)
        is_major = (i % (num_minor // num_major) == 0)
        tw = 14 if is_major else 8
        tc = GAUGE_TICK_MAJOR if is_major else GAUGE_TICK
        if ticks_right:
            draw.line([(tick_side, ty), (tick_side + tw, ty)],
                      fill=tc, width=2 if is_major else 1)
        else:
            draw.line([(tick_side - tw, ty), (tick_side, ty)],
                      fill=tc, width=2 if is_major else 1)

    # Fluid fill in gauge
    if fluid_type is not None and fill_frac > 0:
        dark, mid, light, surface, glow, _ = FLUID_COLORS[fluid_type]
        inner_h = gauge_h - 4
        fh = int(inner_h * fill_frac)
        ft = y2 - 2 - fh

        for y in range(ft, y2 - 1):
            t = (y - ft) / max(1, fh)
            c = lerp_color(light, dark, t)
            draw.line([(gx + 2, y), (gx + gauge_w - 2, y)], fill=c)

        # Surface line
        draw.line([(gx + 2, ft), (gx + gauge_w - 2, ft)],
                  fill=surface, width=2)

        # Subtle gauge glow
        gcx = gx + gauge_w // 2
        gcy = (ft + y2) // 2
        add_radial_glow(img, gcx, gcy, gauge_w, glow,
                        intensity=0.10)


# ---------------------------------------------------------------------------
#  Internal tank structure (visible through glass when empty/low)
# ---------------------------------------------------------------------------

def draw_tank_internals(draw, x1, y1, x2, y2):
    """Draw internal pipes and brackets visible inside the empty tank."""
    cx = (x1 + x2) // 2
    w = x2 - x1
    h = y2 - y1

    # Intake pipe coming from top center
    pipe_w = 28
    pipe_x = cx - pipe_w // 2
    draw.rectangle([pipe_x, y1, pipe_x + pipe_w, y1 + int(h * 0.25)],
                   fill=TANK_PIPE)
    # Pipe highlight
    draw.line([(pipe_x + 4, y1), (pipe_x + 4, y1 + int(h * 0.25))],
              fill=TANK_PIPE_HI, width=2)
    # Pipe outline
    draw.rectangle([pipe_x, y1, pipe_x + pipe_w, y1 + int(h * 0.25)],
                   outline=TANK_WELD)

    # Pipe nozzle at bottom of intake
    noz_y = y1 + int(h * 0.25)
    draw.rectangle([pipe_x - 6, noz_y, pipe_x + pipe_w + 6, noz_y + 10],
                   fill=TANK_BRACKET, outline=TANK_WELD)

    # Horizontal bracing bars
    for frac in (0.40, 0.70):
        bar_y = y1 + int(h * frac)
        draw.rectangle([x1 + 8, bar_y, x2 - 8, bar_y + 6],
                       fill=TANK_BRACKET, outline=TANK_WELD)
        # Bracket mounting points
        for bx in [x1 + 20, x2 - 20]:
            draw.rectangle([bx - 5, bar_y - 4, bx + 5, bar_y + 10],
                           fill=TANK_PIPE, outline=TANK_WELD)

    # Drain outlet at bottom center
    drain_y = y2 - int(h * 0.08)
    draw.rectangle([pipe_x - 2, drain_y, pipe_x + pipe_w + 2, y2],
                   fill=TANK_PIPE, outline=TANK_WELD)
    draw.rectangle([pipe_x + 4, drain_y + 2,
                    pipe_x + pipe_w - 4, y2 - 2],
                   fill=GLASS_DARK)

    # Weld seam lines along tank edges
    draw.line([(x1 + 4, y1 + 10), (x1 + 4, y2 - 10)],
              fill=TANK_WELD, width=2)
    draw.line([(x2 - 4, y1 + 10), (x2 - 4, y2 - 10)],
              fill=TANK_WELD, width=2)

    # Measurement scale on right inside wall
    for i in range(0, 11):
        my = y2 - int(h * 0.05) - int((h * 0.88) * i / 10)
        mw = 16 if i % 5 == 0 else 8
        mc = (45, 50, 62, 255) if i % 5 == 0 else (35, 38, 48, 255)
        draw.line([(x2 - 10, my), (x2 - 10 - mw, my)],
                  fill=mc, width=2 if i % 5 == 0 else 1)


# ---------------------------------------------------------------------------
#  NORTH (FRONT) - Heavy viewport with internal structure
# ---------------------------------------------------------------------------

def make_north(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx = S // 2
    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Thick outer frame
    frame_outer = 18
    win_x1 = int(S * 0.12)
    win_y1 = int(S * 0.08)
    win_x2 = int(S * 0.88)
    win_y2 = int(S * 0.78)

    # Heavy frame surround
    fx1 = win_x1 - frame_outer
    fy1 = win_y1 - frame_outer
    fx2 = win_x2 + frame_outer
    fy2 = win_y2 + frame_outer

    draw.rectangle([fx1, fy1, fx2, fy2], fill=ARMOR_MID)
    add_bevel_border(draw, fx1, fy1, fx2, fy2,
                     ARMOR_LIGHT, EDGE_DARK, width=5)

    # Recessed inner frame
    draw.rectangle([win_x1 - 4, win_y1 - 4, win_x2 + 4, win_y2 + 4],
                   fill=EDGE_DARK)
    draw.rectangle([win_x1, win_y1, win_x2, win_y2], fill=GLASS_DARK)
    draw.rectangle([win_x1 + 2, win_y1 + 2, win_x2 - 2, win_y2 - 2],
                   fill=GLASS_TINT)

    # Internal tank structure (visible behind glass)
    draw_tank_internals(draw, win_x1 + 3, win_y1 + 3,
                        win_x2 - 3, win_y2 - 3)

    # Fluid on top of internals
    draw_fluid_body(draw, img, win_x1 + 3, win_y1 + 3,
                    win_x2 - 3, win_y2 - 3, fill_frac, fluid_type)

    # Glass sheen overlay (two subtle diagonal strips)
    sheen = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sheen)
    # Narrow bright strip
    sx1 = win_x1 + int((win_x2 - win_x1) * 0.08)
    sx2 = win_x1 + int((win_x2 - win_x1) * 0.12)
    sd.polygon([
        (sx1, win_y1 + 6), (sx2, win_y1 + 6),
        (sx2 - 20, win_y2 - 6), (sx1 - 20, win_y2 - 6)
    ], fill=(200, 220, 240, 18))
    # Wider faint strip
    sx3 = sx2 + 12
    sx4 = sx3 + 20
    sd.polygon([
        (sx3, win_y1 + 6), (sx4, win_y1 + 6),
        (sx4 - 20, win_y2 - 6), (sx3 - 20, win_y2 - 6)
    ], fill=(200, 220, 240, 10))
    img = Image.alpha_composite(img, sheen)
    draw = ImageDraw.Draw(img)

    # Window frame inner border line
    draw.rectangle([win_x1, win_y1, win_x2, win_y2], outline=EDGE_DARK)

    # Heavy bolts around frame (corners + midpoints)
    cy_win = (win_y1 + win_y2) // 2
    bolt_spots = [
        (fx1 + 14, fy1 + 14), (fx2 - 14, fy1 + 14),
        (fx1 + 14, fy2 - 14), (fx2 - 14, fy2 - 14),
        (cx, fy1 + 14), (cx, fy2 - 14),
        (fx1 + 14, cy_win), (fx2 - 14, cy_win),
    ]
    for bx, by in bolt_spots:
        draw_filled_circle(draw, bx, by, 11, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Output nozzle at bottom center
    draw_pipe_connector(draw, img, cx, int(S * 0.90), 52, 30,
                        fluid_type, fill_frac)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  SOUTH (BACK) - Twin pipe connectors + pressure panel
# ---------------------------------------------------------------------------

def make_south(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Pressure data panel (upper center)
    px1 = int(S * 0.25)
    py1 = int(S * 0.12)
    px2 = int(S * 0.75)
    py2 = int(S * 0.42)

    draw.rectangle([px1, py1, px2, py2], fill=ARMOR_MID)
    add_bevel_border(draw, px1, py1, px2, py2,
                     EDGE_DARK, ARMOR_LIGHT, width=4)

    # Panel contents: horizontal data lines (like a readout)
    line_y = py1 + 22
    while line_y + 10 < py2 - 16:
        lw = 40 + (hash(line_y) % 80)
        lx = px1 + 22
        line_c = (35, 40, 50, 255)
        draw.rectangle([lx, line_y, lx + lw, line_y + 6],
                       fill=line_c)
        # Status dot
        if fluid_type and fill_frac > 0:
            _, _, _, _, glow, _ = FLUID_COLORS[fluid_type]
            draw_filled_circle(draw, px2 - 24, line_y + 3, 4, glow)
        else:
            draw_filled_circle(draw, px2 - 24, line_y + 3, 4,
                               (40, 44, 54, 255))
        line_y += 24

    # Panel bolts
    for bx, by in [(px1 + 14, py1 + 14), (px2 - 14, py1 + 14),
                   (px1 + 14, py2 - 14), (px2 - 14, py2 - 14)]:
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Twin pipe connectors (lower half, spaced apart)
    pipe_y = int(S * 0.66)
    for pipe_cx in [int(S * 0.32), int(S * 0.68)]:
        draw_pipe_connector(draw, img, pipe_cx, pipe_y, 56, 32,
                            fluid_type, fill_frac, bolt_count=8)

    # Pressure relief valve (small, between the pipes)
    valve_cx = cx
    valve_cy = int(S * 0.88)
    # Valve body
    draw.rectangle([valve_cx - 20, valve_cy - 14,
                    valve_cx + 20, valve_cy + 14],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, valve_cx - 20, valve_cy - 14,
                     valve_cx + 20, valve_cy + 14,
                     ARMOR_LIGHT, EDGE_DARK, width=2)
    # Valve handle (T-shape)
    draw.rectangle([valve_cx - 3, valve_cy - 28,
                    valve_cx + 3, valve_cy - 14],
                   fill=ARMOR_LIGHT, outline=EDGE_DARK)
    draw.rectangle([valve_cx - 14, valve_cy - 32,
                    valve_cx + 14, valve_cy - 26],
                   fill=ARMOR_LIGHT, outline=EDGE_DARK)

    # Horizontal seam
    m = 30
    draw.line([(m, cy), (px1 - 10, cy)], fill=SEAM_COLOR, width=2)
    draw.line([(px2 + 10, cy), (S - m, cy)], fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  EAST / WEST (SIDES) - Reinforcement ribs + dual sight gauges
# ---------------------------------------------------------------------------

def make_side(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Vertical reinforcement ribs (pressure vessel look)
    rib_w = 30
    rib_top = int(S * 0.07)
    rib_bot = int(S * 0.93)
    rib_positions = [int(S * 0.18), int(S * 0.82) - rib_w]

    for rx in rib_positions:
        # Rib body
        draw.rectangle([rx, rib_top, rx + rib_w, rib_bot],
                       fill=ARMOR_MID)
        # Rib highlight (center bright strip)
        draw.rectangle([rx + rib_w // 2 - 3, rib_top,
                        rx + rib_w // 2 + 3, rib_bot],
                       fill=ARMOR_LIGHT)
        # Rib edges
        draw.line([(rx, rib_top), (rx, rib_bot)],
                  fill=EDGE_DARK, width=2)
        draw.line([(rx + rib_w, rib_top), (rx + rib_w, rib_bot)],
                  fill=EDGE_DARK, width=2)

        # Clamp brackets along rib
        for clamp_y in range(rib_top + 60, rib_bot - 40, 160):
            ch = 14
            draw.rectangle([rx - 10, clamp_y, rx + rib_w + 10,
                            clamp_y + ch],
                           fill=ARMOR_LIGHT, outline=EDGE_DARK)
            draw_filled_circle(draw, rx - 5, clamp_y + ch // 2,
                               4, RIVET_COLOR)
            draw_filled_circle(draw, rx + rib_w + 5, clamp_y + ch // 2,
                               4, RIVET_COLOR)

    # Dual sight-glass gauges (between the ribs)
    gauge_w = 44
    gauge_y1 = int(S * 0.12)
    gauge_y2 = int(S * 0.88)
    gauge_left_x = int(S * 0.34)
    gauge_right_x = int(S * 0.58)

    draw_gauge(draw, img, gauge_left_x, gauge_y1, gauge_y2,
               gauge_w, fill_frac, fluid_type, ticks_right=True)
    draw_gauge(draw, img, gauge_right_x, gauge_y1, gauge_y2,
               gauge_w, fill_frac, fluid_type, ticks_right=False)

    # Connecting bar between gauges at top and bottom
    bar_h = 10
    for by in [gauge_y1 - 8, gauge_y2 + 2]:
        draw.rectangle([gauge_left_x - 3, by,
                        gauge_right_x + gauge_w + 3, by + bar_h],
                       fill=ARMOR_MID, outline=EDGE_DARK)

    # Central label plate between gauges
    lp_x1 = gauge_left_x + gauge_w + 8
    lp_x2 = gauge_right_x - 8
    lp_y1 = cy - 30
    lp_y2 = cy + 30
    if lp_x2 > lp_x1 + 10:
        draw.rectangle([lp_x1, lp_y1, lp_x2, lp_y2],
                       fill=ARMOR_MID, outline=EDGE_DARK)
        add_bevel_border(draw, lp_x1, lp_y1, lp_x2, lp_y2,
                         ARMOR_LIGHT, EDGE_DARK, width=2)
        # Two small lines suggesting text
        draw.rectangle([lp_x1 + 6, cy - 8, lp_x2 - 6, cy - 3],
                       fill=(40, 44, 54, 255))
        draw.rectangle([lp_x1 + 6, cy + 3, lp_x2 - 6, cy + 8],
                       fill=(40, 44, 54, 255))

    # Horizontal seams
    m = 30
    draw.line([(m, cy), (rib_positions[0] - 4, cy)],
              fill=SEAM_COLOR, width=2)
    draw.line([(rib_positions[1] + rib_w + 4, cy), (S - m, cy)],
              fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  UP (TOP) - Heavy fill hatch
# ---------------------------------------------------------------------------

def make_top(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Reinforcement cross-plates (X pattern under the hatch)
    plate_w = 24
    m = 60
    for sx, sy, ex, ey in [
        (m, m, S - m, S - m), (S - m, m, m, S - m)
    ]:
        draw.line([(sx, sy), (ex, ey)], fill=ARMOR_MID,
                  width=plate_w)
        draw.line([(sx, sy), (ex, ey)], fill=EDGE_DARK, width=2)

    # Hatch rim (large circular opening)
    rim_outer = int(S * 0.36)
    rim_inner = int(S * 0.26)

    # Thick metallic rim
    for r in range(rim_outer, rim_inner, -1):
        t = (rim_outer - r) / (rim_outer - rim_inner)
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Rim edges
    draw.ellipse([cx - rim_outer, cy - rim_outer,
                  cx + rim_outer, cy + rim_outer],
                 outline=EDGE_DARK)
    draw.ellipse([cx - rim_inner, cy - rim_inner,
                  cx + rim_inner, cy + rim_inner],
                 outline=EDGE_DARK)

    # Hatch interior
    if fluid_type is not None and fill_frac > 0:
        dark, mid, light, surface, glow, _ = FLUID_COLORS[fluid_type]
        # Fluid surface seen from above
        for r in range(rim_inner - 2, 0, -1):
            t = r / (rim_inner - 2)
            c = lerp_color(surface, mid, t * 0.5)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                         outline=c)
        # Bright center
        core_r = int(rim_inner * 0.25)
        draw_filled_circle(draw, cx, cy, core_r, surface)
        # Concentric ripples
        for ring_r in range(core_r + 30, rim_inner - 10, 40):
            draw.ellipse([cx - ring_r, cy - ring_r,
                          cx + ring_r, cy + ring_r],
                         outline=(*surface[:3], 50))
        add_radial_glow(img, cx, cy, rim_inner + 25, glow,
                        intensity=0.15)
    else:
        draw_filled_circle(draw, cx, cy, rim_inner - 2, GLASS_DARK)
        # Visible internal braces when empty
        draw.line([(cx - rim_inner + 15, cy),
                   (cx + rim_inner - 15, cy)],
                  fill=TANK_BRACKET, width=6)
        draw.line([(cx, cy - rim_inner + 15),
                   (cx, cy + rim_inner - 15)],
                  fill=TANK_BRACKET, width=6)

    # Bolts around rim
    bolt_count = 12
    bolt_r = (rim_outer + rim_inner) // 2
    for i in range(bolt_count):
        angle = math.radians(360 * i / bolt_count)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 9, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Corner seams
    seam_end = rim_outer + 20
    for sx, sy, ex, ey in [
        (m, m, cx - int(seam_end * 0.7),
         cy - int(seam_end * 0.7)),
        (S - m, m, cx + int(seam_end * 0.7),
         cy - int(seam_end * 0.7)),
        (m, S - m, cx - int(seam_end * 0.7),
         cy + int(seam_end * 0.7)),
        (S - m, S - m, cx + int(seam_end * 0.7),
         cy + int(seam_end * 0.7)),
    ]:
        if fluid_type is not None and fill_frac > 0:
            _, _, _, _, glow, _ = FLUID_COLORS[fluid_type]
            add_glowing_seam(
                img, (sx, sy), (ex, ey), SEAM_COLOR, glow,
                seam_width=2, glow_width=6, intensity=0.08
            )
        else:
            draw.line([(sx, sy), (ex, ey)],
                      fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  DOWN (BOTTOM) - Reinforced base plate with drain
# ---------------------------------------------------------------------------

def make_bottom(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    # Central drain grille
    grille_size = 360
    g1 = cx - grille_size // 2
    g2 = cx + grille_size // 2
    draw.rectangle([g1, g1, g2, g2], fill=ARMOR_MID,
                   outline=ARMOR_LIGHT)
    add_bevel_border(draw, g1, g1, g2, g2, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Vent/drain slots
    for vy in range(g1 + 24, g2 - 16, 22):
        draw.rectangle([g1 + 18, vy, g2 - 18, vy + 8],
                       fill=EDGE_DARK)
        draw.rectangle([g1 + 20, vy + 1, g2 - 20, vy + 7],
                       fill=(15, 17, 22, 255))

    # Mounting feet in corners
    foot_size = 70
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

def main():
    textures = {}

    textures["fluid_container_north"] = make_north()
    textures["fluid_container_south"] = make_south()
    textures["fluid_container_east"] = make_side()
    textures["fluid_container_west"] = make_side()
    textures["fluid_container_up"] = make_top()
    textures["fluid_container_down"] = make_bottom()

    for fluid in ("water", "lava", "xp"):
        for level in ("low", "medium", "full"):
            suffix = f"_{fluid}_{level}"
            textures[f"fluid_container_north{suffix}"] = make_north(
                fluid_type=fluid, fill_level=level
            )
            textures[f"fluid_container_south{suffix}"] = make_south(
                fluid_type=fluid, fill_level=level
            )
            textures[f"fluid_container_east{suffix}"] = make_side(
                fluid_type=fluid, fill_level=level
            )
            textures[f"fluid_container_west{suffix}"] = make_side(
                fluid_type=fluid, fill_level=level
            )
            textures[f"fluid_container_up{suffix}"] = make_top(
                fluid_type=fluid, fill_level=level
            )
            textures[f"fluid_container_down{suffix}"] = make_bottom(
                fluid_type=fluid, fill_level=level
            )

    save_textures(textures)


if __name__ == "__main__":
    main()
