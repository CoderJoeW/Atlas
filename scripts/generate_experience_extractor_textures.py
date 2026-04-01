#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Experience Extractor at 1024x1024.

Concept: A mystical-industrial machine that extracts XP from items.
The front shows an extraction chamber with a glowing green XP orb symbol
and a fluid output port. When active, the orb and port glow brighter.
The sides have a red power input connector. The top has a square intake
hopper with directional arrow (idle) or green glow (active). The back
has an exhaust/vent panel. The bottom is a reinforced base plate.

Creates 10 textures:
  - experience_extractor_front (idle)
  - experience_extractor_front_active (powered)
  - experience_extractor_back
  - experience_extractor_side (power connector)
  - experience_extractor_housing (bottom)
  - experience_extractor_top_north/south/east/west (directional idle)
  - experience_extractor_top_active (powered)
"""

import math
from PIL import Image, ImageDraw

from texture_lib import (
    new_img, add_border, lerp_color, blend_over,
    add_radial_glow, add_glowing_seam,
    save_textures,
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

# XP / experience green colors
XP_DARK = (15, 80, 10, 255)
XP_MID = (30, 150, 20, 255)
XP_BRIGHT = (57, 255, 20, 255)   # #39FF14 from item name
XP_GLOW = (80, 255, 40, 255)
XP_DIM = (20, 100, 15, 255)

# Power connector — red for power input
POWER_RED = (180, 40, 40, 255)
POWER_RED_DIM = (100, 30, 30, 255)

# Interior
INTERIOR_BG = (10, 12, 16, 255)

# Arrow color for directional indicator
ARROW_COLOR = (30, 140, 20, 255)
ARROW_OUTLINE = (15, 80, 10, 255)

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


def make_hex_armor_base():
    """Create a hex armor face — full 1024x1024 base."""
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
    """Add bolts at the four corners of a face."""
    for bx, by in [(inset, inset), (S - inset, inset),
                   (inset, S - inset), (S - inset, S - inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)


def draw_xp_orb(draw, img, cx, cy, radius, active=False):
    """Draw a diamond-shaped XP orb symbol with optional glow.

    The orb is a rotated square (diamond) with inner facets,
    resembling a Minecraft experience orb.
    """
    r = radius

    # Outer diamond shape
    diamond_pts = [
        (cx, cy - r),       # top
        (cx + r, cy),       # right
        (cx, cy + r),       # bottom
        (cx - r, cy),       # left
    ]

    if active:
        fill = XP_BRIGHT
        outline = XP_MID
    else:
        fill = XP_MID
        outline = XP_DARK

    draw.polygon(diamond_pts, fill=fill, outline=outline)

    # Inner facet — smaller diamond
    inner_r = int(r * 0.55)
    inner_pts = [
        (cx, cy - inner_r),
        (cx + inner_r, cy),
        (cx, cy + inner_r),
        (cx - inner_r, cy),
    ]
    if active:
        draw.polygon(inner_pts, fill=XP_GLOW, outline=XP_BRIGHT)
    else:
        draw.polygon(inner_pts, fill=XP_BRIGHT, outline=XP_MID)

    # Highlight dot
    draw_filled_circle(draw, cx - int(r * 0.2), cy - int(r * 0.2),
                       int(r * 0.12),
                       XP_GLOW if active else XP_BRIGHT)

    # Glow effect
    if active:
        add_radial_glow(img, cx, cy, r + 40, XP_GLOW, intensity=0.25)
    else:
        add_radial_glow(img, cx, cy, r + 20, XP_MID, intensity=0.08)


# ---------------------------------------------------------------------------
#  FRONT — extraction chamber with XP orb
# ---------------------------------------------------------------------------

def make_front(active=False):
    """Front face: extraction chamber panel with XP orb symbol and
    fluid output port.

    Active variant glows green.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx = S // 2

    # Extraction chamber panel
    panel_w = int(S * 0.60)
    panel_h = int(S * 0.55)
    px1 = cx - panel_w // 2
    py1 = int(S * 0.12)
    px2 = cx + panel_w // 2
    py2 = py1 + panel_h

    draw.rectangle([px1, py1, px2, py2], fill=ARMOR_MID)
    add_bevel_border(draw, px1, py1, px2, py2,
                     EDGE_DARK, ARMOR_LIGHT, width=5)

    # Dark chamber window
    win_inset = 30
    wx1 = px1 + win_inset
    wy1 = py1 + win_inset
    wx2 = px2 - win_inset
    wy2 = py2 - win_inset
    draw.rectangle([wx1, wy1, wx2, wy2], fill=INTERIOR_BG)
    draw.rectangle([wx1, wy1, wx2, wy2], outline=EDGE_DARK)

    # XP orb in the chamber center
    orb_cy = (wy1 + wy2) // 2
    draw_xp_orb(draw, img, cx, orb_cy, 80, active=active)

    # Panel bolts
    for bx, by in [(px1 + 16, py1 + 16), (px2 - 16, py1 + 16),
                   (px1 + 16, py2 - 16), (px2 - 16, py2 - 16)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Fluid output port at the bottom center
    port_cy = py2 + (S - py2) // 2
    port_r = 45

    # Port mounting plate
    draw_filled_circle(draw, cx, port_cy, port_r + 20, ARMOR_MID,
                       outline=EDGE_DARK)

    # Port ring
    if active:
        for r in range(port_r, port_r - 15, -1):
            t = (port_r - r) / 15
            c = lerp_color(XP_DIM, XP_MID, t)
            draw.ellipse([cx - r, port_cy - r, cx + r, port_cy + r],
                         outline=c)
        add_radial_glow(img, cx, port_cy, port_r + 30,
                        XP_GLOW, intensity=0.15)
    else:
        for r in range(port_r, port_r - 15, -1):
            t = (port_r - r) / 15
            c = lerp_color(ARMOR_DARK, XP_DIM, t)
            draw.ellipse([cx - r, port_cy - r, cx + r, port_cy + r],
                         outline=c)

    draw.ellipse(
        [cx - port_r, port_cy - port_r, cx + port_r, port_cy + port_r],
        outline=EDGE_DARK
    )

    # Port center
    draw_filled_circle(draw, cx, port_cy, port_r - 16,
                       INTERIOR_BG, outline=EDGE_DARK)
    draw_filled_circle(draw, cx, port_cy, 10,
                       XP_MID if active else XP_DIM,
                       outline=EDGE_DARK)

    # Port mounting bolts
    for i in range(6):
        angle = math.radians(60 * i)
        bx = int(cx + (port_r + 12) * math.cos(angle))
        by = int(port_cy + (port_r + 12) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 6, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 3, RIVET_COLOR)

    # Horizontal seams
    m = 30
    if active:
        add_glowing_seam(
            img, (m, S // 2), (px1 - 10, S // 2),
            SEAM_COLOR, XP_MID,
            seam_width=2, glow_width=6, intensity=0.10
        )
        add_glowing_seam(
            img, (px2 + 10, S // 2), (S - m, S // 2),
            SEAM_COLOR, XP_MID,
            seam_width=2, glow_width=6, intensity=0.10
        )
    else:
        draw.line([(m, S // 2), (px1 - 10, S // 2)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(px2 + 10, S // 2), (S - m, S // 2)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  BACK — exhaust/vent panel
# ---------------------------------------------------------------------------

def make_back():
    """Back face: hex armor with vent panel."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Vent panel
    panel_w = int(S * 0.48)
    panel_h = int(S * 0.42)
    px1 = cx - panel_w // 2
    py1 = cy - panel_h // 2
    px2 = cx + panel_w // 2
    py2 = cy + panel_h // 2

    draw.rectangle([px1, py1, px2, py2], fill=ARMOR_MID)
    add_bevel_border(draw, px1, py1, px2, py2,
                     EDGE_DARK, ARMOR_LIGHT, width=5)

    # Horizontal vent slats
    slat_h = 10
    slat_gap = 18
    y = py1 + 20
    while y + slat_h < py2 - 14:
        draw.rectangle(
            [px1 + 18, y, px2 - 18, y + slat_h],
            fill=(15, 17, 22, 255)
        )
        draw.line(
            [(px1 + 18, y + slat_h + 1), (px2 - 18, y + slat_h + 1)],
            fill=ARMOR_LIGHT, width=1
        )
        y += slat_h + slat_gap

    # Panel bolts
    bolt_off = 18
    for bx, by in [(px1 + bolt_off, py1 + bolt_off),
                   (px2 - bolt_off, py1 + bolt_off),
                   (px1 + bolt_off, py2 - bolt_off),
                   (px2 - bolt_off, py2 - bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Seams
    m = 30
    draw.line([(m, cy), (px1 - 10, cy)], fill=SEAM_COLOR, width=2)
    draw.line([(px2 + 10, cy), (S - m, cy)], fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  SIDE — red power input connector
# ---------------------------------------------------------------------------

def make_side():
    """Side face: hex armor with red power input connector."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Power connector
    conn_outer_r = 90
    conn_inner_r = 60

    # Mounting plate
    plate_r = conn_outer_r + 30
    draw_filled_circle(draw, cx, cy, plate_r, ARMOR_MID,
                       outline=EDGE_DARK)

    # Red ring gradient
    for r in range(conn_outer_r, conn_inner_r, -1):
        t = (conn_outer_r - r) / (conn_outer_r - conn_inner_r)
        c = lerp_color(POWER_RED_DIM, POWER_RED, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    draw.ellipse(
        [cx - conn_outer_r, cy - conn_outer_r,
         cx + conn_outer_r, cy + conn_outer_r],
        outline=EDGE_DARK
    )
    draw.ellipse(
        [cx - conn_inner_r, cy - conn_inner_r,
         cx + conn_inner_r, cy + conn_inner_r],
        outline=EDGE_DARK
    )

    # Dark center
    draw_filled_circle(draw, cx, cy, conn_inner_r - 2, INTERIOR_BG)

    # Center pin
    draw_filled_circle(draw, cx, cy, 18, POWER_RED_DIM,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, 8, RIVET_COLOR)

    # Mounting bolts
    bolt_count = 8
    bolt_r = plate_r - 14
    for i in range(bolt_count):
        angle = math.radians(360 * i / bolt_count)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Seams
    m = 30
    draw.line([(m, cy), (cx - plate_r - 10, cy)],
              fill=SEAM_COLOR, width=2)
    draw.line([(cx + plate_r + 10, cy), (S - m, cy)],
              fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  TOP — directional intake hopper (idle) and active glow
# ---------------------------------------------------------------------------

def make_top(direction="north"):
    """Top face (idle): square intake hopper with directional arrow."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Square hopper
    hopper_size = int(S * 0.28)
    hx1 = cx - hopper_size
    hy1 = cy - hopper_size
    hx2 = cx + hopper_size
    hy2 = cy + hopper_size

    # Hopper frame
    frame_w = 28
    draw.rectangle(
        [hx1 - frame_w, hy1 - frame_w, hx2 + frame_w, hy2 + frame_w],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw, hx1 - frame_w, hy1 - frame_w,
        hx2 + frame_w, hy2 + frame_w,
        ARMOR_LIGHT, EDGE_DARK, width=4
    )

    # Dark hopper interior
    draw.rectangle([hx1, hy1, hx2, hy2], fill=INTERIOR_BG)

    # Small XP orb visible inside
    draw_xp_orb(draw, img, cx, cy, 50, active=False)

    # Hopper outline
    draw.rectangle([hx1, hy1, hx2, hy2], outline=EDGE_DARK)

    # Frame bolts
    for bx, by in [
        (hx1 - frame_w // 2, hy1 - frame_w // 2),
        (hx2 + frame_w // 2, hy1 - frame_w // 2),
        (hx1 - frame_w // 2, hy2 + frame_w // 2),
        (hx2 + frame_w // 2, hy2 + frame_w // 2),
        (cx, hy1 - frame_w // 2),
        (cx, hy2 + frame_w // 2),
        (hx1 - frame_w // 2, cy),
        (hx2 + frame_w // 2, cy),
    ]:
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Directional arrow
    arrow_len = 90
    arrow_w = 44
    arrow_head_w = 72
    arrow_head_len = 55

    if direction == "north":
        ay = hy1 - frame_w - 28
        draw.rectangle(
            [cx - arrow_w // 2, ay - arrow_len, cx + arrow_w // 2, ay],
            fill=ARROW_COLOR, outline=ARROW_OUTLINE
        )
        draw.polygon([
            (cx - arrow_head_w, ay - arrow_len),
            (cx + arrow_head_w, ay - arrow_len),
            (cx, ay - arrow_len - arrow_head_len),
        ], fill=ARROW_COLOR, outline=ARROW_OUTLINE)
    elif direction == "south":
        ay = hy2 + frame_w + 28
        draw.rectangle(
            [cx - arrow_w // 2, ay, cx + arrow_w // 2, ay + arrow_len],
            fill=ARROW_COLOR, outline=ARROW_OUTLINE
        )
        draw.polygon([
            (cx - arrow_head_w, ay + arrow_len),
            (cx + arrow_head_w, ay + arrow_len),
            (cx, ay + arrow_len + arrow_head_len),
        ], fill=ARROW_COLOR, outline=ARROW_OUTLINE)
    elif direction == "east":
        ax = hx2 + frame_w + 28
        draw.rectangle(
            [ax, cy - arrow_w // 2, ax + arrow_len, cy + arrow_w // 2],
            fill=ARROW_COLOR, outline=ARROW_OUTLINE
        )
        draw.polygon([
            (ax + arrow_len, cy - arrow_head_w),
            (ax + arrow_len, cy + arrow_head_w),
            (ax + arrow_len + arrow_head_len, cy),
        ], fill=ARROW_COLOR, outline=ARROW_OUTLINE)
    elif direction == "west":
        ax = hx1 - frame_w - 28
        draw.rectangle(
            [ax - arrow_len, cy - arrow_w // 2, ax, cy + arrow_w // 2],
            fill=ARROW_COLOR, outline=ARROW_OUTLINE
        )
        draw.polygon([
            (ax - arrow_len, cy - arrow_head_w),
            (ax - arrow_len, cy + arrow_head_w),
            (ax - arrow_len - arrow_head_len, cy),
        ], fill=ARROW_COLOR, outline=ARROW_OUTLINE)

    # Corner seams
    m = 30
    for sx, sy, ex, ey in [
        (m, m, hx1 - frame_w - 10, hy1 - frame_w - 10),
        (S - m, m, hx2 + frame_w + 10, hy1 - frame_w - 10),
        (m, S - m, hx1 - frame_w - 10, hy2 + frame_w + 10),
        (S - m, S - m, hx2 + frame_w + 10, hy2 + frame_w + 10),
    ]:
        draw.line([(sx, sy), (ex, ey)], fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    return img


def make_top_active():
    """Top face (powered): glowing green hopper."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Square hopper
    hopper_size = int(S * 0.28)
    hx1 = cx - hopper_size
    hy1 = cy - hopper_size
    hx2 = cx + hopper_size
    hy2 = cy + hopper_size

    # Hopper frame
    frame_w = 28
    draw.rectangle(
        [hx1 - frame_w, hy1 - frame_w, hx2 + frame_w, hy2 + frame_w],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw, hx1 - frame_w, hy1 - frame_w,
        hx2 + frame_w, hy2 + frame_w,
        ARMOR_LIGHT, EDGE_DARK, width=4
    )

    # Glowing green interior
    draw.rectangle([hx1, hy1, hx2, hy2], fill=INTERIOR_BG)
    for r in range(hopper_size, 0, -1):
        t = r / hopper_size
        c = lerp_color(XP_DIM, INTERIOR_BG, t * 0.6)
        draw.rectangle([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Active XP orb
    draw_xp_orb(draw, img, cx, cy, 55, active=True)

    # Hopper outline
    draw.rectangle([hx1, hy1, hx2, hy2], outline=EDGE_DARK)

    # Frame bolts
    for bx, by in [
        (hx1 - frame_w // 2, hy1 - frame_w // 2),
        (hx2 + frame_w // 2, hy1 - frame_w // 2),
        (hx1 - frame_w // 2, hy2 + frame_w // 2),
        (hx2 + frame_w // 2, hy2 + frame_w // 2),
        (cx, hy1 - frame_w // 2),
        (cx, hy2 + frame_w // 2),
        (hx1 - frame_w // 2, cy),
        (hx2 + frame_w // 2, cy),
    ]:
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Glowing corner seams
    m = 30
    for sx, sy, ex, ey in [
        (m, m, hx1 - frame_w - 10, hy1 - frame_w - 10),
        (S - m, m, hx2 + frame_w + 10, hy1 - frame_w - 10),
        (m, S - m, hx1 - frame_w - 10, hy2 + frame_w + 10),
        (S - m, S - m, hx2 + frame_w + 10, hy2 + frame_w + 10),
    ]:
        add_glowing_seam(
            img, (sx, sy), (ex, ey),
            SEAM_COLOR, XP_MID,
            seam_width=2, glow_width=6, intensity=0.12
        )

    add_corner_bolts(draw)

    # Overall glow
    add_radial_glow(img, cx, cy, hopper_size + 40,
                    XP_GLOW, intensity=0.18)

    return img


# ---------------------------------------------------------------------------
#  BOTTOM — reinforced base plate
# ---------------------------------------------------------------------------

def make_bottom():
    """Bottom face: heavy reinforced base plate with vent grille."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Central vent grille
    grille_size = 360
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
    textures = {
        "experience_extractor_front": make_front(active=False),
        "experience_extractor_front_active": make_front(active=True),
        "experience_extractor_back": make_back(),
        "experience_extractor_side": make_side(),
        "experience_extractor_housing": make_bottom(),
        "experience_extractor_top_north": make_top("north"),
        "experience_extractor_top_south": make_top("south"),
        "experience_extractor_top_east": make_top("east"),
        "experience_extractor_top_west": make_top("west"),
        "experience_extractor_top_active": make_top_active(),
    }

    save_textures(textures)


if __name__ == "__main__":
    main()
