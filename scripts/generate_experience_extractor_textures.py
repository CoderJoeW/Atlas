#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Experience Extractor at 1024x1024.

Concept: A mystical-industrial machine that extracts XP from items and
outputs Liquid Experience fluid. The front shows a large glass vial
chamber with a prominent XP orb and a fluid output nozzle. When active,
everything glows vivid green. The sides have a red power input connector.
The top has an intake hopper with directional arrow (idle) or green glow
(active). The back has an exhaust panel. The bottom is a reinforced base.

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

ARMOR_DARK = (38, 40, 48, 255)
ARMOR_MID = (52, 56, 66, 255)
ARMOR_LIGHT = (70, 75, 88, 255)
HEX_LINE = (48, 52, 62, 255)
EDGE_DARK = (22, 24, 30, 255)
SEAM_COLOR = (30, 33, 40, 255)
RIVET_COLOR = (100, 108, 125, 255)

# XP green
XP_DARK = (15, 80, 10, 255)
XP_MID = (30, 150, 20, 255)
XP_BRIGHT = (57, 255, 20, 255)
XP_GLOW = (80, 255, 40, 255)
XP_DIM = (20, 100, 15, 255)

# Vial / glass tint
GLASS_DARK = (18, 35, 22, 255)
GLASS_MID = (25, 50, 30, 255)
GLASS_EDGE = (35, 65, 40, 255)

# Power connector
POWER_RED = (180, 40, 40, 255)
POWER_RED_DIM = (100, 30, 30, 255)

INTERIOR_BG = (10, 12, 16, 255)

# Arrow
ARROW_COLOR = (30, 140, 20, 255)
ARROW_OUTLINE = (15, 80, 10, 255)

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


def draw_xp_orb(draw, img, cx, cy, radius, active=False):
    """Draw a diamond-shaped XP orb with facets and glow."""
    r = radius
    diamond = [(cx, cy - r), (cx + r, cy),
               (cx, cy + r), (cx - r, cy)]

    if active:
        draw.polygon(diamond, fill=XP_BRIGHT, outline=XP_MID)
        inner_r = int(r * 0.50)
        inner = [(cx, cy - inner_r), (cx + inner_r, cy),
                 (cx, cy + inner_r), (cx - inner_r, cy)]
        draw.polygon(inner, fill=XP_GLOW, outline=XP_BRIGHT)
        draw_filled_circle(draw, cx - int(r * 0.18),
                           cy - int(r * 0.18),
                           int(r * 0.10), XP_GLOW)
        add_radial_glow(img, cx, cy, int(r * 1.8), XP_GLOW,
                        intensity=0.30)
    else:
        draw.polygon(diamond, fill=XP_MID, outline=XP_DARK)
        inner_r = int(r * 0.50)
        inner = [(cx, cy - inner_r), (cx + inner_r, cy),
                 (cx, cy + inner_r), (cx - inner_r, cy)]
        draw.polygon(inner, fill=XP_BRIGHT, outline=XP_MID)
        draw_filled_circle(draw, cx - int(r * 0.18),
                           cy - int(r * 0.18),
                           int(r * 0.10), XP_GLOW)
        add_radial_glow(img, cx, cy, int(r * 1.2), XP_MID,
                        intensity=0.10)


def draw_vial(draw, img, cx, top_y, bottom_y, half_w, neck_half_w,
              neck_h, active=False):
    """Draw a glass vial/flask shape — a narrow neck leading to a wide body.

    The vial is drawn as a trapezoidal neck tapering into a rectangular
    body with rounded-feeling corners (beveled edges).
    """
    neck_top = top_y
    neck_bottom = top_y + neck_h
    body_top = neck_bottom
    body_bottom = bottom_y

    # Neck
    neck_pts = [
        (cx - neck_half_w, neck_top),
        (cx + neck_half_w, neck_top),
        (cx + half_w, body_top),
        (cx - half_w, body_top),
    ]
    fill_c = GLASS_MID if not active else lerp_color(GLASS_MID, XP_DIM, 0.5)
    draw.polygon(neck_pts, fill=fill_c, outline=GLASS_EDGE)

    # Body
    draw.rectangle([cx - half_w, body_top, cx + half_w, body_bottom],
                   fill=GLASS_DARK if not active else
                   lerp_color(GLASS_DARK, XP_DARK, 0.4))

    # Glass rim at top of neck
    draw.rectangle(
        [cx - neck_half_w - 8, neck_top - 8,
         cx + neck_half_w + 8, neck_top + 8],
        fill=ARMOR_LIGHT, outline=EDGE_DARK
    )

    # Glass sheen lines (vertical highlight on left side of body)
    sheen_x = cx - half_w + 16
    for dy in range(body_top + 10, body_bottom - 10):
        if (dy // 4) % 2 == 0:
            draw.point((sheen_x, dy), fill=GLASS_EDGE)
            draw.point((sheen_x + 1, dy), fill=GLASS_EDGE)

    # Body outline
    draw.rectangle([cx - half_w, body_top, cx + half_w, body_bottom],
                   outline=GLASS_EDGE)

    # Fluid level inside (bottom portion of body)
    if active:
        fluid_top = body_top + int((body_bottom - body_top) * 0.25)
        for y in range(fluid_top, body_bottom - 2):
            t = (y - fluid_top) / max(1, body_bottom - 2 - fluid_top)
            c = lerp_color(XP_MID, XP_DIM, t * 0.4)
            draw.line([(cx - half_w + 4, y), (cx + half_w - 4, y)],
                      fill=c)
    else:
        fluid_top = body_top + int((body_bottom - body_top) * 0.65)
        for y in range(fluid_top, body_bottom - 2):
            t = (y - fluid_top) / max(1, body_bottom - 2 - fluid_top)
            c = lerp_color(XP_DARK, (12, 50, 10, 255), t * 0.5)
            draw.line([(cx - half_w + 4, y), (cx + half_w - 4, y)],
                      fill=c)


# ---------------------------------------------------------------------------
#  FRONT — vial chamber with XP orb and fluid nozzle
# ---------------------------------------------------------------------------

def make_front(active=False):
    """Front face: large glass vial chamber with XP orb and output nozzle."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx = S // 2

    # Armored chamber frame
    frame_inset = int(S * 0.10)
    fx1 = frame_inset
    fy1 = frame_inset
    fx2 = S - frame_inset
    fy2 = int(S * 0.76)

    draw.rectangle([fx1, fy1, fx2, fy2], fill=ARMOR_MID)
    add_bevel_border(draw, fx1, fy1, fx2, fy2,
                     ARMOR_LIGHT, EDGE_DARK, width=5)

    # Dark chamber interior
    ch_inset = 28
    cx1 = fx1 + ch_inset
    cy1 = fy1 + ch_inset
    cx2 = fx2 - ch_inset
    cy2 = fy2 - ch_inset

    draw.rectangle([cx1, cy1, cx2, cy2], fill=INTERIOR_BG)

    # Glass vial inside the chamber
    vial_half_w = int((cx2 - cx1) * 0.30)
    vial_neck_hw = int(vial_half_w * 0.40)
    vial_top = cy1 + 30
    vial_bottom = cy2 - 30
    vial_neck_h = int((vial_bottom - vial_top) * 0.20)

    draw_vial(draw, img, cx, vial_top, vial_bottom,
              vial_half_w, vial_neck_hw, vial_neck_h, active=active)

    # XP orb floating above the fluid in the vial
    orb_cy = vial_top + vial_neck_h + int(
        (vial_bottom - vial_top - vial_neck_h) * 0.35)
    draw_xp_orb(draw, img, cx, orb_cy, 55, active=active)

    # Chamber outline
    draw.rectangle([cx1, cy1, cx2, cy2], outline=EDGE_DARK)

    # Frame bolts
    for bx, by in [(fx1 + 18, fy1 + 18), (fx2 - 18, fy1 + 18),
                   (fx1 + 18, fy2 - 18), (fx2 - 18, fy2 - 18),
                   (cx, fy1 + 18), (cx, fy2 - 18)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Fluid output nozzle below the chamber
    nozzle_cy = fy2 + (S - fy2) // 2
    nozzle_r = 55

    # Nozzle mounting plate
    draw_filled_circle(draw, cx, nozzle_cy, nozzle_r + 24,
                       ARMOR_MID, outline=EDGE_DARK)

    # Nozzle ring
    ring_color = XP_MID if active else XP_DARK
    for r in range(nozzle_r, nozzle_r - 18, -1):
        t = (nozzle_r - r) / 18
        c = lerp_color(ARMOR_DARK, ring_color, t)
        draw.ellipse([cx - r, nozzle_cy - r, cx + r, nozzle_cy + r],
                     outline=c)

    draw.ellipse(
        [cx - nozzle_r, nozzle_cy - nozzle_r,
         cx + nozzle_r, nozzle_cy + nozzle_r],
        outline=EDGE_DARK
    )

    # Nozzle center opening
    inner_r = nozzle_r - 20
    draw_filled_circle(draw, cx, nozzle_cy, inner_r,
                       INTERIOR_BG, outline=EDGE_DARK)

    # Center fluid dot
    if active:
        draw_filled_circle(draw, cx, nozzle_cy, 16, XP_MID,
                           outline=XP_DARK)
        add_radial_glow(img, cx, nozzle_cy, nozzle_r + 30,
                        XP_GLOW, intensity=0.18)
    else:
        draw_filled_circle(draw, cx, nozzle_cy, 16, XP_DARK,
                           outline=EDGE_DARK)

    # Nozzle mounting bolts
    for i in range(6):
        angle = math.radians(60 * i + 30)
        bx = int(cx + (nozzle_r + 14) * math.cos(angle))
        by = int(nozzle_cy + (nozzle_r + 14) * math.sin(angle))
        draw_filled_circle(draw, bx, by, 7, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 3, RIVET_COLOR)

    # Seams flanking the chamber
    m = 30
    seam_y = (fy1 + fy2) // 2
    if active:
        add_glowing_seam(
            img, (m, seam_y), (fx1 - 6, seam_y),
            SEAM_COLOR, XP_MID,
            seam_width=2, glow_width=8, intensity=0.12
        )
        add_glowing_seam(
            img, (fx2 + 6, seam_y), (S - m, seam_y),
            SEAM_COLOR, XP_MID,
            seam_width=2, glow_width=8, intensity=0.12
        )
    else:
        draw.line([(m, seam_y), (fx1 - 6, seam_y)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(fx2 + 6, seam_y), (S - m, seam_y)],
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

    panel_w = int(S * 0.48)
    panel_h = int(S * 0.42)
    px1 = cx - panel_w // 2
    py1 = cy - panel_h // 2
    px2 = cx + panel_w // 2
    py2 = cy + panel_h // 2

    draw.rectangle([px1, py1, px2, py2], fill=ARMOR_MID)
    add_bevel_border(draw, px1, py1, px2, py2,
                     EDGE_DARK, ARMOR_LIGHT, width=5)

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

    bolt_off = 18
    for bx, by in [(px1 + bolt_off, py1 + bolt_off),
                   (px2 - bolt_off, py1 + bolt_off),
                   (px1 + bolt_off, py2 - bolt_off),
                   (px2 - bolt_off, py2 - bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

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

    conn_outer_r = 90
    conn_inner_r = 60

    plate_r = conn_outer_r + 30
    draw_filled_circle(draw, cx, cy, plate_r, ARMOR_MID,
                       outline=EDGE_DARK)

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

    draw_filled_circle(draw, cx, cy, conn_inner_r - 2, INTERIOR_BG)
    draw_filled_circle(draw, cx, cy, 18, POWER_RED_DIM,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, 8, RIVET_COLOR)

    bolt_count = 8
    bolt_r = plate_r - 14
    for i in range(bolt_count):
        angle = math.radians(360 * i / bolt_count)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

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
    """Top face (idle): intake hopper with glass vial neck visible and
    directional arrow."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    hopper_size = int(S * 0.28)
    hx1 = cx - hopper_size
    hy1 = cy - hopper_size
    hx2 = cx + hopper_size
    hy2 = cy + hopper_size

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

    # Circular vial opening in center of hopper
    vial_r = int(hopper_size * 0.45)
    draw_filled_circle(draw, cx, cy, vial_r + 10, GLASS_EDGE)
    draw_filled_circle(draw, cx, cy, vial_r, GLASS_DARK,
                       outline=GLASS_EDGE)

    # XP orb visible deep inside
    draw_xp_orb(draw, img, cx, cy, 40, active=False)

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
    """Top face (powered): glowing green hopper with bright vial opening."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    hopper_size = int(S * 0.28)
    hx1 = cx - hopper_size
    hy1 = cy - hopper_size
    hx2 = cx + hopper_size
    hy2 = cy + hopper_size

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

    # Glowing interior
    draw.rectangle([hx1, hy1, hx2, hy2], fill=INTERIOR_BG)
    for r in range(hopper_size, 0, -1):
        t = r / hopper_size
        c = lerp_color(XP_DIM, INTERIOR_BG, t * 0.55)
        draw.rectangle([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Glowing vial opening
    vial_r = int(hopper_size * 0.45)
    draw_filled_circle(draw, cx, cy, vial_r + 10, GLASS_EDGE)
    draw_filled_circle(draw, cx, cy, vial_r, XP_DARK,
                       outline=XP_MID)

    # Active XP orb
    draw_xp_orb(draw, img, cx, cy, 45, active=True)

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
            seam_width=2, glow_width=8, intensity=0.15
        )

    add_corner_bolts(draw)
    add_radial_glow(img, cx, cy, hopper_size + 50,
                    XP_GLOW, intensity=0.20)

    return img


# ---------------------------------------------------------------------------
#  BOTTOM — reinforced base plate
# ---------------------------------------------------------------------------

def make_bottom():
    """Bottom face: heavy reinforced base plate."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    grille_size = 360
    g1 = cx - grille_size // 2
    g2 = cx + grille_size // 2
    draw.rectangle([g1, g1, g2, g2], fill=ARMOR_MID,
                   outline=ARMOR_LIGHT)
    add_bevel_border(draw, g1, g1, g2, g2, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    for vy in range(g1 + 24, g2 - 16, 22):
        draw.rectangle([g1 + 18, vy, g2 - 18, vy + 8], fill=EDGE_DARK)
        draw.rectangle([g1 + 20, vy + 1, g2 - 20, vy + 7],
                       fill=(15, 17, 22, 255))

    foot_size = 70
    foot_inset = 55
    for fx, fy in [(foot_inset, foot_inset),
                   (S - foot_inset - foot_size, foot_inset),
                   (foot_inset, S - foot_inset - foot_size),
                   (S - foot_inset - foot_size,
                    S - foot_inset - foot_size)]:
        draw.rectangle([fx, fy, fx + foot_size, fy + foot_size],
                       fill=ARMOR_LIGHT, outline=EDGE_DARK)
        add_bevel_border(draw, fx, fy, fx + foot_size, fy + foot_size,
                         (80, 86, 100, 255), EDGE_DARK, width=2)
        fcx = fx + foot_size // 2
        fcy = fy + foot_size // 2
        draw_filled_circle(draw, fcx, fcy, 12, ARMOR_MID,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, fcx, fcy, 6, RIVET_COLOR)

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
