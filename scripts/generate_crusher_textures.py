#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Crusher at 1024x1024.

Concept: Heavy industrial ore crusher with visible jaw mechanism.
The front face shows interlocking crusher jaws/teeth — jagged triangular
teeth from top and bottom meeting in the center. The sides show a power
connector and heavy armor plating. The top has a square intake hopper
with a directional arrow (idle) or glowing active indicator (powered).
The back has an exhaust/output panel. The bottom is a reinforced base.

The crusher is directional (facing) and has a powered boolean state.
Idle top faces are directional (arrow), powered top is a single shared
texture with an active glow.

Creates 9 textures:
  - crusher_front (jaw teeth)
  - crusher_back (output panel)
  - crusher_side (power connector)
  - crusher_housing (bottom base)
  - crusher_top_north/south/east/west (directional idle tops)
  - crusher_top_active (powered top, shared)
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

# Crusher jaw / teeth colors
JAW_DARK = (55, 58, 65, 255)
JAW_MID = (75, 80, 90, 255)
JAW_LIGHT = (95, 100, 115, 255)
JAW_EDGE = (110, 115, 130, 255)
TOOTH_TIP = (140, 145, 160, 255)

# Interior
INTERIOR_BG = (10, 12, 16, 255)

# Power connector — red for power input
POWER_RED = (180, 40, 40, 255)
POWER_RED_BRIGHT = (220, 60, 50, 255)
POWER_RED_DIM = (100, 30, 30, 255)

# Active glow — orange-amber
AMBER_GLOW = (255, 140, 30, 255)
ORANGE_GLOW = (255, 120, 20, 255)
ACTIVE_FILL = (200, 120, 20, 255)

# Arrow color for directional indicator
ARROW_DIM = (70, 75, 88, 255)

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


# ---------------------------------------------------------------------------
#  FRONT — crusher jaw teeth
# ---------------------------------------------------------------------------

def make_front():
    """Front face: interlocking crusher jaw teeth.

    Large jagged teeth from top and bottom interlock in the center,
    with dark interior visible between them. Heavy armored frame
    surrounds the jaw opening.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx = S // 2

    # Jaw opening frame
    frame_w = 28
    jaw_x1 = int(S * 0.12)
    jaw_x2 = int(S * 0.88)
    jaw_y1 = int(S * 0.10)
    jaw_y2 = int(S * 0.90)

    # Heavy outer frame
    draw.rectangle(
        [jaw_x1 - frame_w, jaw_y1 - frame_w,
         jaw_x2 + frame_w, jaw_y2 + frame_w],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw, jaw_x1 - frame_w, jaw_y1 - frame_w,
        jaw_x2 + frame_w, jaw_y2 + frame_w,
        ARMOR_LIGHT, EDGE_DARK, width=4
    )

    # Dark interior
    draw.rectangle([jaw_x1, jaw_y1, jaw_x2, jaw_y2], fill=INTERIOR_BG)

    # Crusher teeth from top
    tooth_w = 80
    tooth_h = 180
    num_teeth = (jaw_x2 - jaw_x1) // tooth_w
    tooth_start_x = jaw_x1 + ((jaw_x2 - jaw_x1) - num_teeth * tooth_w) // 2

    for i in range(num_teeth):
        tx = tooth_start_x + i * tooth_w + tooth_w // 2
        # Top tooth — triangle pointing down
        top_pts = [
            (tx - tooth_w // 2 + 4, jaw_y1),
            (tx + tooth_w // 2 - 4, jaw_y1),
            (tx, jaw_y1 + tooth_h),
        ]
        draw.polygon(top_pts, fill=JAW_MID, outline=EDGE_DARK)
        # Highlight on left edge
        draw.line([top_pts[0], top_pts[2]], fill=JAW_LIGHT, width=3)
        # Dark on right edge
        draw.line([top_pts[1], top_pts[2]], fill=JAW_DARK, width=3)
        # Tooth tip highlight
        draw_filled_circle(draw, tx, jaw_y1 + tooth_h - 8, 6,
                           TOOTH_TIP)

    # Crusher teeth from bottom
    for i in range(num_teeth):
        tx = tooth_start_x + i * tooth_w + tooth_w // 2
        # Offset by half a tooth width for interlocking
        tx_bot = tx + tooth_w // 2
        if tx_bot > jaw_x2 - 10:
            continue
        # Bottom tooth — triangle pointing up
        bot_pts = [
            (tx_bot - tooth_w // 2 + 4, jaw_y2),
            (tx_bot + tooth_w // 2 - 4, jaw_y2),
            (tx_bot, jaw_y2 - tooth_h),
        ]
        draw.polygon(bot_pts, fill=JAW_MID, outline=EDGE_DARK)
        draw.line([bot_pts[0], bot_pts[2]], fill=JAW_LIGHT, width=3)
        draw.line([bot_pts[1], bot_pts[2]], fill=JAW_DARK, width=3)
        draw_filled_circle(draw, tx_bot, jaw_y2 - tooth_h + 8, 6,
                           TOOTH_TIP)

    # Also add partial bottom teeth at the edges for interlocking feel
    # First bottom tooth (left-aligned)
    tx_first = tooth_start_x
    if tx_first >= jaw_x1 + 10:
        bot_pts = [
            (tx_first - tooth_w // 2 + 4, jaw_y2),
            (tx_first + tooth_w // 2 - 4, jaw_y2),
            (tx_first, jaw_y2 - tooth_h),
        ]
        draw.polygon(bot_pts, fill=JAW_MID, outline=EDGE_DARK)
        draw.line([bot_pts[0], bot_pts[2]], fill=JAW_LIGHT, width=3)
        draw.line([bot_pts[1], bot_pts[2]], fill=JAW_DARK, width=3)
        draw_filled_circle(draw, tx_first, jaw_y2 - tooth_h + 8, 6,
                           TOOTH_TIP)

    # Frame outline on top
    draw.rectangle([jaw_x1, jaw_y1, jaw_x2, jaw_y2], outline=EDGE_DARK)

    # Frame bolts
    for bx in [jaw_x1 - frame_w // 2, jaw_x2 + frame_w // 2]:
        for by in [jaw_y1 - frame_w // 2, jaw_y2 + frame_w // 2,
                   S // 2]:
            draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                               outline=EDGE_DARK)
            draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  BACK — output panel
# ---------------------------------------------------------------------------

def make_back():
    """Back face: hex armor with output vent panel."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Output vent panel
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
            [(px1 + 18, y + slat_h + 1),
             (px2 - 18, y + slat_h + 1)],
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

    # Horizontal seams
    m = 30
    draw.line([(m, cy), (px1 - 10, cy)], fill=SEAM_COLOR, width=2)
    draw.line([(px2 + 10, cy), (S - m, cy)], fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  SIDE — power connector and armor plating
# ---------------------------------------------------------------------------

def make_side():
    """Side face: hex armor with red power input connector circle."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Power connector — red circle (power input)
    conn_outer_r = 90
    conn_inner_r = 60

    # Connector mounting plate
    plate_r = conn_outer_r + 30
    draw_filled_circle(draw, cx, cy, plate_r, ARMOR_MID,
                       outline=EDGE_DARK)

    # Outer ring gradient
    for r in range(conn_outer_r, conn_inner_r, -1):
        t = (conn_outer_r - r) / (conn_outer_r - conn_inner_r)
        c = lerp_color(POWER_RED_DIM, POWER_RED, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Outer/inner ring edges
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

    # Inner dark center
    draw_filled_circle(draw, cx, cy, conn_inner_r - 2, INTERIOR_BG)

    # Center contact pin
    draw_filled_circle(draw, cx, cy, 18, POWER_RED_DIM,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, 8, RIVET_COLOR)

    # Mounting bolts around the plate
    bolt_count = 8
    bolt_r = plate_r - 14
    for i in range(bolt_count):
        angle = math.radians(360 * i / bolt_count)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Horizontal seam
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
    """Top face (idle): square intake hopper with directional arrow.

    A square opening in the center for ore input, with an arrow
    indicating the facing/output direction.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Square hopper opening
    hopper_size = int(S * 0.30)
    hx1 = cx - hopper_size
    hy1 = cy - hopper_size
    hx2 = cx + hopper_size
    hy2 = cy + hopper_size

    # Hopper frame
    frame_w = 30
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

    # Crusher jaw teeth visible inside the hopper (top and bottom rows)
    tooth_w = 60
    tooth_h = 80
    num_teeth = (hx2 - hx1) // tooth_w

    for i in range(num_teeth):
        tx = hx1 + i * tooth_w + tooth_w // 2
        # Top teeth pointing down
        pts_top = [
            (tx - tooth_w // 2 + 4, hy1 + 2),
            (tx + tooth_w // 2 - 4, hy1 + 2),
            (tx, hy1 + tooth_h),
        ]
        draw.polygon(pts_top, fill=JAW_DARK, outline=EDGE_DARK)

        # Bottom teeth pointing up
        pts_bot = [
            (tx - tooth_w // 2 + 4, hy2 - 2),
            (tx + tooth_w // 2 - 4, hy2 - 2),
            (tx, hy2 - tooth_h),
        ]
        draw.polygon(pts_bot, fill=JAW_DARK, outline=EDGE_DARK)

    # Hopper outline
    draw.rectangle([hx1, hy1, hx2, hy2], outline=EDGE_DARK)

    # Hopper frame bolts
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

    # Directional arrow outside the hopper pointing toward facing direction
    arrow_len = 100
    arrow_w = 50
    arrow_head_w = 80
    arrow_head_len = 60

    if direction == "north":
        # Arrow pointing up
        ay = hy1 - frame_w - 30
        # Shaft
        draw.rectangle(
            [cx - arrow_w // 2, ay - arrow_len,
             cx + arrow_w // 2, ay],
            fill=ARROW_DIM, outline=EDGE_DARK
        )
        # Head
        draw.polygon([
            (cx - arrow_head_w, ay - arrow_len),
            (cx + arrow_head_w, ay - arrow_len),
            (cx, ay - arrow_len - arrow_head_len),
        ], fill=ARROW_DIM, outline=EDGE_DARK)
    elif direction == "south":
        ay = hy2 + frame_w + 30
        draw.rectangle(
            [cx - arrow_w // 2, ay,
             cx + arrow_w // 2, ay + arrow_len],
            fill=ARROW_DIM, outline=EDGE_DARK
        )
        draw.polygon([
            (cx - arrow_head_w, ay + arrow_len),
            (cx + arrow_head_w, ay + arrow_len),
            (cx, ay + arrow_len + arrow_head_len),
        ], fill=ARROW_DIM, outline=EDGE_DARK)
    elif direction == "east":
        ax = hx2 + frame_w + 30
        draw.rectangle(
            [ax, cy - arrow_w // 2,
             ax + arrow_len, cy + arrow_w // 2],
            fill=ARROW_DIM, outline=EDGE_DARK
        )
        draw.polygon([
            (ax + arrow_len, cy - arrow_head_w),
            (ax + arrow_len, cy + arrow_head_w),
            (ax + arrow_len + arrow_head_len, cy),
        ], fill=ARROW_DIM, outline=EDGE_DARK)
    elif direction == "west":
        ax = hx1 - frame_w - 30
        draw.rectangle(
            [ax - arrow_len, cy - arrow_w // 2,
             ax, cy + arrow_w // 2],
            fill=ARROW_DIM, outline=EDGE_DARK
        )
        draw.polygon([
            (ax - arrow_len, cy - arrow_head_w),
            (ax - arrow_len, cy + arrow_head_w),
            (ax - arrow_len - arrow_head_len, cy),
        ], fill=ARROW_DIM, outline=EDGE_DARK)

    # Corner seams
    m = 30
    h_edge = hx2 + frame_w + 10
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
    """Top face (powered): glowing hopper indicating active crushing.

    Same hopper layout but the interior glows orange-amber to show
    the crusher is actively processing.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Square hopper opening
    hopper_size = int(S * 0.30)
    hx1 = cx - hopper_size
    hy1 = cy - hopper_size
    hx2 = cx + hopper_size
    hy2 = cy + hopper_size

    # Hopper frame
    frame_w = 30
    draw.rectangle(
        [hx1 - frame_w, hy1 - frame_w, hx2 + frame_w, hy2 + frame_w],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw, hx1 - frame_w, hy1 - frame_w,
        hx2 + frame_w, hy2 + frame_w,
        ARMOR_LIGHT, EDGE_DARK, width=4
    )

    # Glowing hopper interior — gradient from center
    draw.rectangle([hx1, hy1, hx2, hy2], fill=INTERIOR_BG)
    for r in range(hopper_size, 0, -1):
        t = r / hopper_size
        c = lerp_color(ACTIVE_FILL, INTERIOR_BG, t * 0.7)
        draw.rectangle(
            [cx - r, cy - r, cx + r, cy + r],
            outline=c
        )

    # Crusher jaw teeth visible inside (dimmer, backlit by glow)
    tooth_w = 60
    tooth_h = 70
    num_teeth = (hx2 - hx1) // tooth_w

    for i in range(num_teeth):
        tx = hx1 + i * tooth_w + tooth_w // 2
        pts_top = [
            (tx - tooth_w // 2 + 4, hy1 + 2),
            (tx + tooth_w // 2 - 4, hy1 + 2),
            (tx, hy1 + tooth_h),
        ]
        draw.polygon(pts_top, fill=JAW_DARK, outline=EDGE_DARK)

        pts_bot = [
            (tx - tooth_w // 2 + 4, hy2 - 2),
            (tx + tooth_w // 2 - 4, hy2 - 2),
            (tx, hy2 - tooth_h),
        ]
        draw.polygon(pts_bot, fill=JAW_DARK, outline=EDGE_DARK)

    # Hopper outline
    draw.rectangle([hx1, hy1, hx2, hy2], outline=EDGE_DARK)

    # Hopper frame bolts
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
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=6, intensity=0.12
        )

    add_corner_bolts(draw)

    # Radial glow from the hopper
    add_radial_glow(img, cx, cy, hopper_size + 40,
                    ORANGE_GLOW, intensity=0.20)

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
    textures = {
        "crusher_front": make_front(),
        "crusher_back": make_back(),
        "crusher_side": make_side(),
        "crusher_housing": make_bottom(),
        "crusher_top_north": make_top("north"),
        "crusher_top_south": make_top("south"),
        "crusher_top_east": make_top("east"),
        "crusher_top_west": make_top("west"),
        "crusher_top_active": make_top_active(),
    }

    save_textures(textures)


if __name__ == "__main__":
    main()
