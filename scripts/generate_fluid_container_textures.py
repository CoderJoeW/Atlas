#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Fluid Container at 1024x1024.

Concept: Industrial fluid storage tank with a large glass viewing window
on the front showing the fluid level inside. Sides have vertical fluid
gauge strips. Top has a fill port cap. Back has pipe connections.
Bottom is a reinforced base plate with drain.

Creates 60 textures:
  6 faces (north, south, east, west, up, down) x 10 appearances:
    empty, water_low, water_medium, water_full,
    lava_low, lava_medium, lava_full,
    xp_low, xp_medium, xp_full
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

# Glass / window
GLASS_DARK = (18, 22, 30, 255)
GLASS_FRAME = (55, 60, 72, 255)
GLASS_SHEEN = (90, 100, 120, 80)

# Fluid colors - water (blue)
WATER_DARK = (20, 60, 120, 255)
WATER_MID = (30, 100, 180, 255)
WATER_LIGHT = (60, 150, 220, 255)
WATER_SURFACE = (80, 180, 240, 255)
WATER_GLOW = (40, 140, 220, 255)

# Fluid colors - lava (orange/red)
LAVA_DARK = (120, 30, 5, 255)
LAVA_MID = (200, 80, 10, 255)
LAVA_LIGHT = (240, 140, 30, 255)
LAVA_SURFACE = (255, 180, 50, 255)
LAVA_GLOW = (255, 120, 20, 255)

# Fluid colors - experience (green)
XP_DARK = (15, 80, 20, 255)
XP_MID = (30, 140, 40, 255)
XP_LIGHT = (60, 200, 70, 255)
XP_SURFACE = (100, 240, 100, 255)
XP_GLOW = (50, 200, 60, 255)

# Gauge colors
GAUGE_BG = (15, 18, 24, 255)
GAUGE_FRAME = (60, 65, 78, 255)
GAUGE_TICK = (45, 48, 58, 255)

# Fill level fractions (how high fluid fills the window)
FILL_LEVELS = {
    "low": 0.25,
    "medium": 0.60,
    "full": 0.92,
}

FLUID_COLORS = {
    "water": (WATER_DARK, WATER_MID, WATER_LIGHT, WATER_SURFACE,
              WATER_GLOW),
    "lava": (LAVA_DARK, LAVA_MID, LAVA_LIGHT, LAVA_SURFACE,
             LAVA_GLOW),
    "xp": (XP_DARK, XP_MID, XP_LIGHT, XP_SURFACE, XP_GLOW),
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
#  Fluid drawing helpers
# ---------------------------------------------------------------------------

def draw_fluid_in_rect(draw, img, x1, y1, x2, y2, fill_frac,
                       fluid_type):
    """Fill a rectangular region with fluid up to fill_frac height."""
    if fluid_type is None or fill_frac <= 0:
        return

    dark, mid, light, surface, glow = FLUID_COLORS[fluid_type]
    total_h = y2 - y1
    fluid_h = int(total_h * fill_frac)
    fluid_top = y2 - fluid_h

    # Draw fluid body with vertical gradient (lighter at top)
    for y in range(fluid_top, y2 + 1):
        t = (y - fluid_top) / max(1, fluid_h)
        c = lerp_color(light, dark, t)
        draw.line([(x1, y), (x2, y)], fill=c)

    # Surface line (bright band at top of fluid)
    surface_h = max(3, int(fluid_h * 0.03))
    for dy in range(surface_h):
        t = dy / max(1, surface_h)
        c = lerp_color(surface, light, t)
        draw.line([(x1, fluid_top + dy), (x2, fluid_top + dy)],
                  fill=c)

    # Surface highlight shimmer
    shimmer_y = fluid_top + 1
    for sx in range(x1 + 20, x2 - 20, 80):
        sw = 30
        sc = (*surface[:3], 140)
        draw.line([(sx, shimmer_y), (sx + sw, shimmer_y)],
                  fill=sc, width=2)

    # Radial glow from fluid center
    fcx = (x1 + x2) // 2
    fcy = (fluid_top + y2) // 2
    glow_r = min(fluid_h, x2 - x1) // 2
    if glow_r > 20:
        add_radial_glow(img, fcx, fcy, glow_r, glow,
                        intensity=0.12)


def draw_gauge_strip(draw, img, gx, y1, y2, gauge_w, fill_frac,
                     fluid_type):
    """Draw a vertical fluid gauge strip with fill level indicator."""
    # Gauge background
    draw.rectangle([gx, y1, gx + gauge_w, y2], fill=GAUGE_BG)
    draw.rectangle([gx, y1, gx + gauge_w, y2], outline=GAUGE_FRAME)

    # Tick marks
    num_ticks = 12
    for i in range(num_ticks + 1):
        ty = y1 + int(i * (y2 - y1) / num_ticks)
        tw = gauge_w // 3 if i % 3 != 0 else gauge_w // 2
        draw.line([(gx + 1, ty), (gx + tw, ty)],
                  fill=GAUGE_TICK, width=1)

    # Fluid level in gauge
    if fluid_type is not None and fill_frac > 0:
        dark, mid, light, surface, glow = FLUID_COLORS[fluid_type]
        gauge_inner_h = y2 - y1 - 4
        fluid_h = int(gauge_inner_h * fill_frac)
        fluid_top = y2 - 2 - fluid_h

        for y in range(fluid_top, y2 - 1):
            t = (y - fluid_top) / max(1, fluid_h)
            c = lerp_color(light, dark, t)
            draw.line([(gx + 2, y), (gx + gauge_w - 2, y)], fill=c)

        # Bright surface line
        draw.line([(gx + 2, fluid_top), (gx + gauge_w - 2, fluid_top)],
                  fill=surface, width=2)


# ---------------------------------------------------------------------------
#  NORTH (FRONT) - Large glass viewing window
# ---------------------------------------------------------------------------

def make_north(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx = S // 2

    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Window frame dimensions
    win_x1 = int(S * 0.14)
    win_y1 = int(S * 0.10)
    win_x2 = int(S * 0.86)
    win_y2 = int(S * 0.82)

    # Outer frame
    frame_w = 20
    draw.rectangle([win_x1 - frame_w, win_y1 - frame_w,
                    win_x2 + frame_w, win_y2 + frame_w],
                   fill=ARMOR_MID)
    add_bevel_border(draw, win_x1 - frame_w, win_y1 - frame_w,
                     win_x2 + frame_w, win_y2 + frame_w,
                     ARMOR_LIGHT, EDGE_DARK, width=4)

    # Inner frame border
    draw.rectangle([win_x1, win_y1, win_x2, win_y2],
                   fill=GLASS_DARK, outline=EDGE_DARK)

    # Inner glass tint - slightly lighter than pure dark
    draw.rectangle([win_x1 + 3, win_y1 + 3, win_x2 - 3, win_y2 - 3],
                   fill=(22, 26, 36, 255))

    # Draw fluid inside the window
    draw_fluid_in_rect(draw, img, win_x1 + 4, win_y1 + 4,
                       win_x2 - 4, win_y2 - 4, fill_frac,
                       fluid_type)

    # Glass sheen overlay (subtle diagonal highlight near top-left)
    sheen_img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sheen_draw = ImageDraw.Draw(sheen_img)
    sheen_x1 = win_x1 + int((win_x2 - win_x1) * 0.06)
    sheen_x2 = win_x1 + int((win_x2 - win_x1) * 0.14)
    sheen_points = [
        (sheen_x1, win_y1 + 8),
        (sheen_x2, win_y1 + 8),
        (sheen_x2 - 30, win_y2 - 8),
        (sheen_x1 - 30, win_y2 - 8),
    ]
    sheen_draw.polygon(sheen_points, fill=(180, 200, 220, 25))
    img = Image.alpha_composite(img, sheen_img)
    draw = ImageDraw.Draw(img)

    # Frame bolts
    bolt_off = 12
    cy_win = (win_y1 + win_y2) // 2
    bolt_positions = [
        (win_x1 - bolt_off, win_y1 - bolt_off),
        (win_x2 + bolt_off, win_y1 - bolt_off),
        (win_x1 - bolt_off, win_y2 + bolt_off),
        (win_x2 + bolt_off, win_y2 + bolt_off),
        (cx, win_y1 - bolt_off),
        (cx, win_y2 + bolt_off),
        (win_x1 - bolt_off, cy_win),
        (win_x2 + bolt_off, cy_win),
    ]
    for bx, by in bolt_positions:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Fluid output nozzle at bottom center
    nozzle_cx = cx
    nozzle_cy = int(S * 0.92)
    nozzle_r_outer = 48
    nozzle_r_inner = 28

    # Mounting ring
    draw.ellipse([nozzle_cx - nozzle_r_outer,
                  nozzle_cy - nozzle_r_outer,
                  nozzle_cx + nozzle_r_outer,
                  nozzle_cy + nozzle_r_outer],
                 fill=ARMOR_MID, outline=EDGE_DARK)
    # Ring bevel
    for r in range(nozzle_r_outer, nozzle_r_outer - 6, -1):
        t = (nozzle_r_outer - r) / 6
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([nozzle_cx - r, nozzle_cy - r,
                      nozzle_cx + r, nozzle_cy + r], outline=c)

    # Nozzle opening
    if fluid_type is not None and fill_frac > 0:
        _, _, _, _, glow = FLUID_COLORS[fluid_type]
        draw_filled_circle(draw, nozzle_cx, nozzle_cy,
                           nozzle_r_inner, glow)
        draw_filled_circle(draw, nozzle_cx, nozzle_cy,
                           nozzle_r_inner - 8, EDGE_DARK)
        add_radial_glow(img, nozzle_cx, nozzle_cy,
                        nozzle_r_outer + 10, glow,
                        intensity=0.15)
    else:
        draw_filled_circle(draw, nozzle_cx, nozzle_cy,
                           nozzle_r_inner, EDGE_DARK)

    # Mounting ring bolts
    for i in range(6):
        angle = math.radians(60 * i + 30)
        bx = int(nozzle_cx + (nozzle_r_outer - 10) * math.cos(angle))
        by = int(nozzle_cy + (nozzle_r_outer - 10) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)

    # Fluid glow on the frame
    if fluid_type is not None and fill_frac > 0:
        _, _, _, _, glow = FLUID_COLORS[fluid_type]
        add_radial_glow(img, cx, (win_y1 + win_y2) // 2,
                        int(S * 0.3), glow, intensity=0.08)

    return img


# ---------------------------------------------------------------------------
#  SOUTH (BACK) - Pipe connections and vent panel
# ---------------------------------------------------------------------------

def make_south(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Large vent panel
    vent_w = int(S * 0.50)
    vent_h = int(S * 0.44)
    vx1 = cx - vent_w // 2
    vy1 = cy - vent_h // 2
    vx2 = cx + vent_w // 2
    vy2 = cy + vent_h // 2

    draw.rectangle([vx1, vy1, vx2, vy2], fill=ARMOR_MID)
    add_bevel_border(draw, vx1, vy1, vx2, vy2,
                     EDGE_DARK, ARMOR_LIGHT, width=5)

    # Horizontal vent slats
    slat_h = 12
    slat_gap = 20
    y = vy1 + 20
    while y + slat_h < vy2 - 14:
        draw.rectangle([vx1 + 18, y, vx2 - 18, y + slat_h],
                       fill=(15, 17, 22, 255))
        draw.line([(vx1 + 18, y + slat_h + 1),
                   (vx2 - 18, y + slat_h + 1)],
                  fill=ARMOR_LIGHT, width=1)
        y += slat_h + slat_gap

    # Panel bolts
    bolt_off = 18
    for bx, by in [(vx1 + bolt_off, vy1 + bolt_off),
                   (vx2 - bolt_off, vy1 + bolt_off),
                   (vx1 + bolt_off, vy2 - bolt_off),
                   (vx2 - bolt_off, vy2 - bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Horizontal seams
    m = 30
    draw.line([(m, cy), (vx1 - 10, cy)], fill=SEAM_COLOR, width=2)
    draw.line([(vx2 + 10, cy), (S - m, cy)], fill=SEAM_COLOR,
              width=2)

    # Input connector at bottom
    conn_cx = cx
    conn_cy = int(S * 0.88)
    conn_r = 38
    draw.ellipse([conn_cx - conn_r, conn_cy - conn_r,
                  conn_cx + conn_r, conn_cy + conn_r],
                 fill=ARMOR_MID, outline=EDGE_DARK)
    for r in range(conn_r, conn_r - 5, -1):
        t = (conn_r - r) / 5
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([conn_cx - r, conn_cy - r,
                      conn_cx + r, conn_cy + r], outline=c)

    if fluid_type is not None and fill_frac > 0:
        _, _, _, _, glow = FLUID_COLORS[fluid_type]
        draw_filled_circle(draw, conn_cx, conn_cy, 22, glow)
        draw_filled_circle(draw, conn_cx, conn_cy, 14, EDGE_DARK)
    else:
        draw_filled_circle(draw, conn_cx, conn_cy, 22, EDGE_DARK)

    for i in range(6):
        angle = math.radians(60 * i)
        bx = int(conn_cx + (conn_r - 8) * math.cos(angle))
        by = int(conn_cy + (conn_r - 8) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  EAST / WEST (SIDES) - Vertical fluid gauge strips
# ---------------------------------------------------------------------------

def make_side(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Central vertical gauge strip
    gauge_w = 36
    gauge_x = cx - gauge_w // 2
    gauge_y1 = int(S * 0.10)
    gauge_y2 = int(S * 0.82)

    # Gauge mounting panel
    panel_w = gauge_w + 40
    panel_x = cx - panel_w // 2
    draw.rectangle([panel_x, gauge_y1 - 10,
                    panel_x + panel_w, gauge_y2 + 10],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, panel_x, gauge_y1 - 10,
                     panel_x + panel_w, gauge_y2 + 10,
                     ARMOR_LIGHT, EDGE_DARK, width=2)

    # Draw the gauge
    draw_gauge_strip(draw, img, gauge_x, gauge_y1, gauge_y2,
                     gauge_w, fill_frac, fluid_type)

    # Gauge frame bolts (top and bottom)
    for by in [gauge_y1 - 5, gauge_y2 + 5]:
        for bx_off in [-panel_w // 2 + 10, panel_w // 2 - 10]:
            draw_filled_circle(draw, cx + bx_off, by, 6,
                               RIVET_COLOR)

    # Level markings (L, M, F)
    mark_positions = [
        (gauge_y2 - int((gauge_y2 - gauge_y1) * 0.25), "L"),
        (gauge_y2 - int((gauge_y2 - gauge_y1) * 0.60), "M"),
        (gauge_y2 - int((gauge_y2 - gauge_y1) * 0.92), "F"),
    ]
    for my, _label in mark_positions:
        # Tick line extending from gauge
        draw.line([(gauge_x + gauge_w + 2, my),
                   (gauge_x + gauge_w + 14, my)],
                  fill=ARMOR_LIGHT, width=2)

    # Structural reinforcement bands (horizontal)
    band_positions = [int(S * 0.06), int(S * 0.86)]
    for by in band_positions:
        draw.rectangle([int(S * 0.08), by,
                        int(S * 0.92), by + 16],
                       fill=ARMOR_MID, outline=EDGE_DARK)
        # Band bolts
        for bx in [int(S * 0.12), int(S * 0.88)]:
            draw_filled_circle(draw, bx, by + 8, 6, RIVET_COLOR)

    # Horizontal seams
    m = 30
    draw.line([(m, cy), (panel_x - 6, cy)],
              fill=SEAM_COLOR, width=2)
    draw.line([(panel_x + panel_w + 6, cy), (S - m, cy)],
              fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    # Fluid glow along gauge
    if fluid_type is not None and fill_frac > 0:
        _, _, _, _, glow = FLUID_COLORS[fluid_type]
        fluid_h = int((gauge_y2 - gauge_y1) * fill_frac)
        glow_cy = gauge_y2 - fluid_h // 2
        add_radial_glow(img, cx, glow_cy,
                        gauge_w + 15, glow, intensity=0.12)

    return img


# ---------------------------------------------------------------------------
#  UP (TOP) - Fill port cap
# ---------------------------------------------------------------------------

def make_top(fluid_type=None, fill_level=None):
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    fill_frac = FILL_LEVELS.get(fill_level, 0)

    # Armored rim around fill port
    rim_outer = int(S * 0.34)
    rim_inner = int(S * 0.24)

    # Thick metallic rim with gradient
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

    # Fill port interior - shows fluid surface from above
    if fluid_type is not None and fill_frac > 0:
        dark, mid, light, surface, glow = FLUID_COLORS[fluid_type]
        # Fluid visible from above - concentric gradient
        for r in range(rim_inner - 2, 0, -1):
            t = r / (rim_inner - 2)
            c = lerp_color(surface, mid, t * 0.6)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                         outline=c)
        # Bright center
        core_r = int(rim_inner * 0.3)
        draw_filled_circle(draw, cx, cy, core_r, surface)
        add_radial_glow(img, cx, cy, rim_inner + 20, glow,
                        intensity=0.15)
    else:
        # Empty dark interior
        draw_filled_circle(draw, cx, cy, rim_inner - 2, GLASS_DARK)

    # Bolts around the rim
    bolt_count = 10
    bolt_r = (rim_outer + rim_inner) // 2
    for i in range(bolt_count):
        angle = math.radians(360 * i / bolt_count)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Cross seams from corners to rim
    m = 30
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
            _, _, _, _, glow = FLUID_COLORS[fluid_type]
            add_glowing_seam(
                img, (sx, sy), (ex, ey),
                SEAM_COLOR, glow,
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

    # Empty textures (6 faces)
    textures["fluid_container_north"] = make_north()
    textures["fluid_container_south"] = make_south()
    textures["fluid_container_east"] = make_side()
    textures["fluid_container_west"] = make_side()
    textures["fluid_container_up"] = make_top()
    textures["fluid_container_down"] = make_bottom()

    # Fluid-filled textures (3 fluids x 3 levels x 6 faces = 54)
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
