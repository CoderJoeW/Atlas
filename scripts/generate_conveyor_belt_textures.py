#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Conveyor Belt at 1024x1024.

Concept: A low-profile industrial transport belt with a sci-fi aesthetic.
The block is only 6/16 tall (slab height), so side faces render only the
bottom 37.5% of the texture (UV rows 10-16 in Minecraft's 0-16 space).

The top face is the most visible — it shows a dark metallic belt surface
with chevron ridges indicating transport direction, framed by armored
rails on the edges. The front (output) end has an amber output indicator.
The back (input) end is a darker intake. Sides show the belt mechanism
with visible rollers.

Creates 8 textures:
  - conveyor_belt_front (output end)
  - conveyor_belt_back (input end)
  - conveyor_belt_side (left/right sides)
  - conveyor_belt_bottom (underside)
  - conveyor_belt_top_north/south/east/west (directional top faces)
"""

import math
from PIL import Image, ImageDraw

from texture_lib import (
    new_img, add_border, lerp_color, blend_over,
    add_radial_glow, save_textures,
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

# Belt surface colors
BELT_DARK = (32, 34, 40, 255)
BELT_MID = (44, 47, 55, 255)
BELT_LIGHT = (56, 60, 70, 255)

# Chevron colors — amber/orange
CHEVRON_FILL = (140, 95, 20, 255)
CHEVRON_BRIGHT = (180, 120, 30, 255)
CHEVRON_OUTLINE = (100, 70, 15, 255)

# Output indicator (front)
OUTPUT_AMBER = (200, 130, 25, 255)
OUTPUT_GLOW = (255, 170, 40, 255)

# Input indicator (back)
INPUT_DIM = (50, 55, 65, 255)

# Roller colors
ROLLER_DARK = (45, 48, 56, 255)
ROLLER_LIGHT = (85, 90, 104, 255)

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


def draw_chevron_north(draw, tip_x, tip_y, arm_len, thickness, half_span):
    """Draw a single upward-pointing chevron (^) as two polygon arms.

    Args:
        tip_x, tip_y: The point of the chevron (topmost point).
        arm_len: Length along each diagonal arm.
        thickness: Thickness of the chevron arms.
        half_span: Half the total width from tip to each arm end.
    """
    # Left arm: from tip going down-left
    # Right arm: from tip going down-right
    # Outer V
    left_out = (tip_x - half_span, tip_y + half_span)
    right_out = (tip_x + half_span, tip_y + half_span)
    tip = (tip_x, tip_y)
    # Inner V (shifted down by thickness)
    left_in = (tip_x - half_span, tip_y + half_span + thickness)
    right_in = (tip_x + half_span, tip_y + half_span + thickness)
    tip_in = (tip_x, tip_y + thickness)

    # Draw as a single polygon (outer V minus inner V)
    # Left arm polygon
    draw.polygon([
        tip, left_out,
        left_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)
    # Right arm polygon
    draw.polygon([
        tip, right_out,
        right_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)


def draw_chevron_south(draw, tip_x, tip_y, thickness, half_span):
    """Draw a downward-pointing chevron (v)."""
    left_out = (tip_x - half_span, tip_y - half_span)
    right_out = (tip_x + half_span, tip_y - half_span)
    tip = (tip_x, tip_y)
    left_in = (tip_x - half_span, tip_y - half_span - thickness)
    right_in = (tip_x + half_span, tip_y - half_span - thickness)
    tip_in = (tip_x, tip_y - thickness)

    draw.polygon([
        tip, left_out, left_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)
    draw.polygon([
        tip, right_out, right_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)


def draw_chevron_east(draw, tip_x, tip_y, thickness, half_span):
    """Draw a rightward-pointing chevron (>)."""
    top_out = (tip_x - half_span, tip_y - half_span)
    bot_out = (tip_x - half_span, tip_y + half_span)
    tip = (tip_x, tip_y)
    top_in = (tip_x - half_span - thickness, tip_y - half_span)
    bot_in = (tip_x - half_span - thickness, tip_y + half_span)
    tip_in = (tip_x - thickness, tip_y)

    draw.polygon([
        tip, top_out, top_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)
    draw.polygon([
        tip, bot_out, bot_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)


def draw_chevron_west(draw, tip_x, tip_y, thickness, half_span):
    """Draw a leftward-pointing chevron (<)."""
    top_out = (tip_x + half_span, tip_y - half_span)
    bot_out = (tip_x + half_span, tip_y + half_span)
    tip = (tip_x, tip_y)
    top_in = (tip_x + half_span + thickness, tip_y - half_span)
    bot_in = (tip_x + half_span + thickness, tip_y + half_span)
    tip_in = (tip_x + thickness, tip_y)

    draw.polygon([
        tip, top_out, top_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)
    draw.polygon([
        tip, bot_out, bot_in, tip_in,
    ], fill=CHEVRON_FILL, outline=CHEVRON_OUTLINE)


# ---------------------------------------------------------------------------
#  TOP faces — belt surface with directional chevrons
# ---------------------------------------------------------------------------

def make_top(direction="north"):
    """Top face: dark belt surface with chevron arrows showing direction.

    The belt surface fills the center with armored rail edges on the sides
    perpendicular to the travel direction. Clean polygon chevrons point
    in the travel direction.

    Args:
        direction: "north", "south", "east", "west"
    """
    img = new_img(S, ARMOR_DARK)
    draw = ImageDraw.Draw(img)

    horizontal = direction in ("east", "west")

    # Outer border
    add_border(draw, S, EDGE_DARK, width=6)
    add_bevel_border(draw, 6, 6, S - 7, S - 7, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    rail_w = 80

    if horizontal:
        # Rails on top and bottom edges
        draw.rectangle([10, 10, S - 10, rail_w], fill=ARMOR_MID)
        add_bevel_border(draw, 10, 10, S - 10, rail_w,
                         ARMOR_LIGHT, EDGE_DARK, width=3)
        draw.rectangle([10, S - rail_w, S - 10, S - 10], fill=ARMOR_MID)
        add_bevel_border(draw, 10, S - rail_w, S - 10, S - 10,
                         ARMOR_LIGHT, EDGE_DARK, width=3)

        for rx in range(80, S - 40, 180):
            for ry in [rail_w // 2, S - rail_w // 2]:
                draw_filled_circle(draw, rx, ry, 8, ARMOR_LIGHT,
                                   outline=EDGE_DARK)
                draw_filled_circle(draw, rx, ry, 4, RIVET_COLOR)

        # Belt surface between rails
        belt_y1 = rail_w + 4
        belt_y2 = S - rail_w - 4
        draw.rectangle([14, belt_y1, S - 14, belt_y2], fill=BELT_DARK)

        # Belt tread ridges — vertical lines
        for rx in range(14, S - 14, 30):
            draw.line([(rx, belt_y1 + 2), (rx, belt_y2 - 2)],
                      fill=BELT_MID, width=2)

        # Chevrons
        belt_cy = (belt_y1 + belt_y2) // 2
        half_span = (belt_y2 - belt_y1) // 2 - 30
        chevron_thickness = 22
        chevron_spacing = 150

        if direction == "east":
            for cx in range(150, S - 40, chevron_spacing):
                draw_chevron_east(draw, cx, belt_cy,
                                  chevron_thickness, half_span)
        else:
            for cx in range(S - 150, 40, -chevron_spacing):
                draw_chevron_west(draw, cx, belt_cy,
                                  chevron_thickness, half_span)
    else:
        # Rails on left and right edges
        draw.rectangle([10, 10, rail_w, S - 10], fill=ARMOR_MID)
        add_bevel_border(draw, 10, 10, rail_w, S - 10,
                         ARMOR_LIGHT, EDGE_DARK, width=3)
        draw.rectangle([S - rail_w, 10, S - 10, S - 10], fill=ARMOR_MID)
        add_bevel_border(draw, S - rail_w, 10, S - 10, S - 10,
                         ARMOR_LIGHT, EDGE_DARK, width=3)

        for ry in range(80, S - 40, 180):
            for rx in [rail_w // 2, S - rail_w // 2]:
                draw_filled_circle(draw, rx, ry, 8, ARMOR_LIGHT,
                                   outline=EDGE_DARK)
                draw_filled_circle(draw, rx, ry, 4, RIVET_COLOR)

        # Belt surface between rails
        belt_x1 = rail_w + 4
        belt_x2 = S - rail_w - 4
        draw.rectangle([belt_x1, 14, belt_x2, S - 14], fill=BELT_DARK)

        # Belt tread ridges — horizontal lines
        for ry in range(14, S - 14, 30):
            draw.line([(belt_x1 + 2, ry), (belt_x2 - 2, ry)],
                      fill=BELT_MID, width=2)

        # Chevrons
        belt_cx = (belt_x1 + belt_x2) // 2
        half_span = (belt_x2 - belt_x1) // 2 - 30
        chevron_thickness = 22
        chevron_spacing = 150

        if direction == "north":
            for cy in range(S - 150, 40, -chevron_spacing):
                draw_chevron_north(draw, belt_cx, cy, 0,
                                   chevron_thickness, half_span)
        else:
            for cy in range(150, S - 40, chevron_spacing):
                draw_chevron_south(draw, belt_cx, cy,
                                   chevron_thickness, half_span)

    return img


# ---------------------------------------------------------------------------
#  FRONT face — output end with amber indicator
# ---------------------------------------------------------------------------

def make_front():
    """Front face (output end): shows belt edge and amber output indicator.

    Only the bottom 37.5% of this texture is visible (UV 10-16).
    The visible region is rows 640-1024.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx = S // 2
    vis_top = int(S * 10 / 16)  # 640
    vis_bot = S
    vis_h = vis_bot - vis_top  # 384

    # Armored housing strip across the visible area
    draw.rectangle([10, vis_top, S - 10, vis_bot - 10],
                   fill=ARMOR_MID)
    add_bevel_border(draw, 10, vis_top, S - 10, vis_bot - 10,
                     ARMOR_LIGHT, EDGE_DARK, width=4)

    # Belt opening — wider and taller for visibility
    belt_w = int(S * 0.64)
    belt_h = int(vis_h * 0.45)
    bx1 = cx - belt_w // 2
    by1 = vis_top + int(vis_h * 0.10)
    bx2 = cx + belt_w // 2
    by2 = by1 + belt_h

    draw.rectangle([bx1, by1, bx2, by2], fill=BELT_DARK)

    # Belt tread lines inside the opening
    for ry in range(by1 + 8, by2 - 4, 14):
        draw.line([(bx1 + 6, ry), (bx2 - 6, ry)],
                  fill=BELT_MID, width=2)

    draw.rectangle([bx1, by1, bx2, by2], outline=EDGE_DARK)

    # Amber output indicator — larger triangle
    arrow_cy = by2 + (vis_bot - 14 - by2) // 2
    arrow_sz = 40

    tri_pts = [
        (cx - arrow_sz, arrow_cy - arrow_sz // 2),
        (cx + arrow_sz, arrow_cy - arrow_sz // 2),
        (cx, arrow_cy + arrow_sz // 2),
    ]
    draw.polygon(tri_pts, fill=OUTPUT_AMBER, outline=EDGE_DARK)

    # Glow around the arrow
    add_radial_glow(img, cx, arrow_cy, 70, OUTPUT_GLOW, intensity=0.25)

    # Bolts flanking the belt opening
    for bx in [bx1 - 50, bx2 + 50]:
        if 20 < bx < S - 20:
            draw_filled_circle(draw, bx, (by1 + by2) // 2, 10,
                               ARMOR_LIGHT, outline=EDGE_DARK)
            draw_filled_circle(draw, bx, (by1 + by2) // 2, 5,
                               RIVET_COLOR)

    # Bottom corner bolts
    for bx in [40, S - 40]:
        draw_filled_circle(draw, bx, vis_bot - 28, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, vis_bot - 28, 4, RIVET_COLOR)

    return img


# ---------------------------------------------------------------------------
#  BACK face — input end
# ---------------------------------------------------------------------------

def make_back():
    """Back face (input end): darker intake with no glow.

    Only bottom 37.5% visible (rows 640-1024).
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx = S // 2
    vis_top = int(S * 10 / 16)
    vis_bot = S
    vis_h = vis_bot - vis_top

    # Armored housing strip
    draw.rectangle([10, vis_top, S - 10, vis_bot - 10],
                   fill=ARMOR_MID)
    add_bevel_border(draw, 10, vis_top, S - 10, vis_bot - 10,
                     ARMOR_LIGHT, EDGE_DARK, width=4)

    # Belt intake opening
    belt_w = int(S * 0.64)
    belt_h = int(vis_h * 0.45)
    bx1 = cx - belt_w // 2
    by1 = vis_top + int(vis_h * 0.10)
    bx2 = cx + belt_w // 2
    by2 = by1 + belt_h

    draw.rectangle([bx1, by1, bx2, by2], fill=BELT_DARK)

    # Belt tread lines
    for ry in range(by1 + 8, by2 - 4, 14):
        draw.line([(bx1 + 6, ry), (bx2 - 6, ry)],
                  fill=BELT_MID, width=2)

    draw.rectangle([bx1, by1, bx2, by2], outline=EDGE_DARK)

    # Intake indicator — dimmer, inward-pointing triangle (upward)
    arrow_cy = by2 + (vis_bot - 14 - by2) // 2
    arrow_sz = 40

    tri_pts = [
        (cx - arrow_sz, arrow_cy + arrow_sz // 2),
        (cx + arrow_sz, arrow_cy + arrow_sz // 2),
        (cx, arrow_cy - arrow_sz // 2),
    ]
    draw.polygon(tri_pts, fill=INPUT_DIM, outline=EDGE_DARK)

    # Bolts flanking the belt opening
    for bx in [bx1 - 50, bx2 + 50]:
        if 20 < bx < S - 20:
            draw_filled_circle(draw, bx, (by1 + by2) // 2, 10,
                               ARMOR_LIGHT, outline=EDGE_DARK)
            draw_filled_circle(draw, bx, (by1 + by2) // 2, 5,
                               RIVET_COLOR)

    # Bottom corner bolts
    for bx in [40, S - 40]:
        draw_filled_circle(draw, bx, vis_bot - 28, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, vis_bot - 28, 4, RIVET_COLOR)

    return img


# ---------------------------------------------------------------------------
#  SIDE face — belt mechanism with rollers
# ---------------------------------------------------------------------------

def make_side():
    """Side face: shows belt edge and roller mechanism.

    Only bottom 37.5% visible (rows 640-1024). Rollers are drawn as
    shaded circles (cross-section view).
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    vis_top = int(S * 10 / 16)
    vis_bot = S
    vis_h = vis_bot - vis_top

    # Armored housing strip
    draw.rectangle([10, vis_top, S - 10, vis_bot - 10],
                   fill=ARMOR_MID)
    add_bevel_border(draw, 10, vis_top, S - 10, vis_bot - 10,
                     ARMOR_LIGHT, EDGE_DARK, width=4)

    # Belt track — dark band at the top of the visible area
    track_h = int(vis_h * 0.28)
    track_y1 = vis_top + 10
    track_y2 = track_y1 + track_h
    draw.rectangle([18, track_y1, S - 18, track_y2],
                   fill=BELT_DARK, outline=EDGE_DARK)

    # Belt tread texture on the track
    for ry in range(track_y1 + 6, track_y2 - 4, 12):
        draw.line([(22, ry), (S - 22, ry)],
                  fill=BELT_MID, width=1)

    # Rollers underneath — drawn as shaded circles (cross-section)
    roller_cy = track_y2 + int(vis_h * 0.22)
    roller_r = 28
    roller_spacing = 170
    for rx in range(90, S - 60, roller_spacing):
        # Shaded circle for roller cross-section
        for dy in range(-roller_r, roller_r + 1):
            for dx in range(-roller_r, roller_r + 1):
                dist = math.sqrt(dx * dx + dy * dy)
                if dist <= roller_r:
                    t = dist / roller_r
                    c = lerp_color(ROLLER_LIGHT, ROLLER_DARK, t)
                    px, py = rx + dx, roller_cy + dy
                    if 0 <= px < S and 0 <= py < S:
                        img.putpixel((px, py), c)
        # Roller outline
        draw_filled_circle(draw, rx, roller_cy, roller_r, None,
                           outline=EDGE_DARK)
        # Axle dot
        draw_filled_circle(draw, rx, roller_cy, 8, RIVET_COLOR,
                           outline=EDGE_DARK)

    # Horizontal seam below rollers
    seam_y = roller_cy + roller_r + 16
    if seam_y < vis_bot - 20:
        draw.line([(18, seam_y), (S - 18, seam_y)],
                  fill=SEAM_COLOR, width=2)

    # Bottom corner bolts
    for bx in [40, S - 40]:
        draw_filled_circle(draw, bx, vis_bot - 28, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, vis_bot - 28, 4, RIVET_COLOR)

    return img


# ---------------------------------------------------------------------------
#  BOTTOM face — reinforced base plate
# ---------------------------------------------------------------------------

def make_bottom():
    """Bottom face: flat reinforced base plate with grid pattern."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Central grid plate
    plate_inset = 60
    px1 = plate_inset
    py1 = plate_inset
    px2 = S - plate_inset
    py2 = S - plate_inset

    draw.rectangle([px1, py1, px2, py2], fill=ARMOR_MID)
    add_bevel_border(draw, px1, py1, px2, py2, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Grid lines
    grid_spacing = 64
    for gx in range(px1 + grid_spacing, px2, grid_spacing):
        draw.line([(gx, py1 + 6), (gx, py2 - 6)],
                  fill=SEAM_COLOR, width=2)
    for gy in range(py1 + grid_spacing, py2, grid_spacing):
        draw.line([(px1 + 6, gy), (px2 - 6, gy)],
                  fill=SEAM_COLOR, width=2)

    # Corner mounting bolts
    bolt_inset = 40
    for bx, by in [(px1 + bolt_inset, py1 + bolt_inset),
                   (px2 - bolt_inset, py1 + bolt_inset),
                   (px1 + bolt_inset, py2 - bolt_inset),
                   (px2 - bolt_inset, py2 - bolt_inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)

    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    textures = {
        "conveyor_belt_front": make_front(),
        "conveyor_belt_back": make_back(),
        "conveyor_belt_side": make_side(),
        "conveyor_belt_bottom": make_bottom(),
        "conveyor_belt_top_north": make_top("north"),
        "conveyor_belt_top_south": make_top("south"),
        "conveyor_belt_top_east": make_top("east"),
        "conveyor_belt_top_west": make_top("west"),
    }

    save_textures(textures)


if __name__ == "__main__":
    main()
