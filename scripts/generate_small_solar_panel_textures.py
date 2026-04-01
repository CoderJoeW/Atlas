#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Small Solar Panel at 1024x1024.

Concept: Dark armored housing with honeycomb grid. The top face is the
showcase — a grid of blue photovoltaic cells in a beveled frame. The
"full" variant has brighter cells and a green status LED. Side faces
have a yellow output connector (the panel generates power). Bottom
is a heavy base plate with vents.

Creates 4 textures:
  small_solar_panel.png          — top face (inactive)
  small_solar_panel_full.png     — top face (full/active)
  small_solar_panel_side.png     — side face (shared)
  small_solar_panel_bottom.png   — bottom face (shared)
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
FRAME_DARK = (28, 30, 38, 255)

# Connector hole
CONNECTOR_HOLE = (12, 14, 18, 255)

# Output connector — yellow (produces power)
OUTPUT_RING = (200, 180, 40, 255)
OUTPUT_BRIGHT = (255, 240, 60, 255)
OUTPUT_GLOW = (255, 230, 50, 255)

# Solar cell colors
CELL_DARK = (15, 40, 120, 255)
CELL_MID = (25, 60, 160, 255)
CELL_LIGHT = (40, 85, 200, 255)
CELL_HIGHLIGHT = (60, 110, 230, 255)
CELL_GRID = (10, 25, 70, 255)

# Active/full solar cell colors (brighter)
CELL_FULL_DARK = (25, 60, 160, 255)
CELL_FULL_MID = (40, 90, 210, 255)
CELL_FULL_LIGHT = (60, 120, 245, 255)
CELL_FULL_HIGHLIGHT = (100, 160, 255, 255)

# Status LED
LED_RED = (200, 50, 40, 255)
LED_RED_GLOW = (255, 60, 40, 255)
LED_GREEN = (50, 220, 50, 255)
LED_GREEN_GLOW = (80, 255, 80, 255)

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
    """Create a hex armor face."""
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
    """Add bolts at the four corners."""
    for bx, by in [(inset, inset), (S - inset, inset),
                   (inset, S - inset), (S - inset, S - inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)


def draw_connector_port(img, draw, cx, cy, outer_r, inner_r,
                        ring_color, bright_color, glow_color,
                        powered=False):
    """Draw a color-coded circular connector port."""
    active_ring = ring_color if not powered else bright_color

    for r in range(outer_r, inner_r, -1):
        t = (outer_r - r) / (outer_r - inner_r)
        c = lerp_color(active_ring, ARMOR_MID, t * 0.6)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    draw_filled_circle(draw, cx, cy, inner_r, CONNECTOR_HOLE,
                       outline=EDGE_DARK)

    pin_r = int(inner_r * 0.5)
    pin_size = max(4, inner_r // 12)
    pin_color = ring_color if not powered else bright_color
    for angle_deg in [0, 90, 180, 270]:
        angle = math.radians(angle_deg)
        px = int(cx + pin_r * math.cos(angle))
        py = int(cy + pin_r * math.sin(angle))
        draw_filled_circle(draw, px, py, pin_size, pin_color)

    if powered:
        add_glow_ring(img, cx, cy, outer_r,
                      outer_r + int(outer_r * 0.35), glow_color)
        add_radial_glow(img, cx, cy, inner_r, bright_color,
                        intensity=0.3)


# ---------------------------------------------------------------------------
#  TOP FACE — solar cell grid
# ---------------------------------------------------------------------------

def make_top(full=False):
    """Top face: realistic solar panel — many small photovoltaic cells
    with thin silver bus bars, in a metallic aluminum frame.

    A real solar panel has:
    - Many small rectangular cells in a tight grid
    - Thin silver bus bars (vertical connectors between cells)
    - Fine horizontal finger lines across each cell
    - Slight color variation between cells
    - Aluminum/silver frame
    """
    img = new_img(S, ARMOR_DARK)
    draw = ImageDraw.Draw(img)

    # Outer border
    add_border(draw, S, EDGE_DARK, width=6)
    add_bevel_border(draw, 6, 6, S - 7, S - 7, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Aluminum frame
    frame_inset = 40
    fx1 = frame_inset
    fy1 = frame_inset
    fx2 = S - frame_inset
    fy2 = S - frame_inset

    # Silver/aluminum frame color
    frame_color = (140, 148, 160, 255)
    frame_inner = (110, 118, 130, 255)
    draw.rectangle([fx1, fy1, fx2, fy2], fill=frame_color)
    add_bevel_border(draw, fx1, fy1, fx2, fy2,
                     (170, 178, 190, 255), (90, 96, 108, 255),
                     width=4)

    # Dark backing behind the cells
    cell_margin = 18
    cx1 = fx1 + cell_margin
    cy1 = fy1 + cell_margin
    cx2 = fx2 - cell_margin
    cy2 = fy2 - cell_margin
    draw.rectangle([cx1, cy1, cx2, cy2], fill=(5, 10, 25, 255))

    cell_area_w = cx2 - cx1
    cell_area_h = cy2 - cy1

    # Cell grid: 6 columns x 10 rows for realistic proportions
    cols = 6
    rows = 10
    bus_bar_w = 3          # silver bus bar width between columns
    cell_gap_h = 2         # tiny gap between rows
    cell_w = (cell_area_w - (cols - 1) * bus_bar_w) // cols
    cell_h = (cell_area_h - (rows - 1) * cell_gap_h) // rows

    # Pick cell colors based on state
    if full:
        c_base = CELL_FULL_DARK
        c_top = CELL_FULL_MID
        c_hi = CELL_FULL_HIGHLIGHT
    else:
        c_base = CELL_DARK
        c_top = CELL_MID
        c_hi = CELL_HIGHLIGHT

    # Silver bus bar color
    bus_color = (160, 170, 185, 255)
    finger_color = (80, 100, 150, 255)

    for row in range(rows):
        for col in range(cols):
            # Cell position
            x1 = cx1 + col * (cell_w + bus_bar_w)
            y1 = cy1 + row * (cell_h + cell_gap_h)
            x2 = x1 + cell_w
            y2 = y1 + cell_h

            # Slight random-ish color variation per cell
            variation = ((row * 7 + col * 13) % 5) - 2
            cell_base = (
                max(0, min(255, c_base[0] + variation * 3)),
                max(0, min(255, c_base[1] + variation * 3)),
                max(0, min(255, c_base[2] + variation * 4)),
                255,
            )
            cell_top = (
                max(0, min(255, c_top[0] + variation * 3)),
                max(0, min(255, c_top[1] + variation * 3)),
                max(0, min(255, c_top[2] + variation * 4)),
                255,
            )

            # Cell gradient (lighter at top, darker at bottom)
            for y in range(y1, min(y2, cy2)):
                t = (y - y1) / max(1, cell_h)
                c = lerp_color(cell_top, cell_base, t)
                draw.line([(max(x1, cx1), y),
                           (min(x2, cx2), y)], fill=c)

            # Fine horizontal finger lines (thin silver lines
            # for current collection, every ~12px)
            for fy in range(y1 + 8, y2 - 4, 12):
                if cy1 < fy < cy2:
                    draw.line(
                        [(max(x1 + 1, cx1), fy),
                         (min(x2 - 1, cx2), fy)],
                        fill=finger_color, width=1
                    )

    # Vertical bus bars between cell columns (silver connectors)
    for col in range(1, cols):
        bx = cx1 + col * cell_w + (col - 1) * bus_bar_w
        draw.rectangle(
            [bx, cy1, bx + bus_bar_w, cy2],
            fill=bus_color
        )

    # Two thicker horizontal bus bars (main current collectors)
    # positioned at 1/3 and 2/3 of the panel height
    main_bus_w = 3
    for frac in (1 / 3, 2 / 3):
        by = int(cy1 + cell_area_h * frac)
        draw.rectangle(
            [cx1, by - main_bus_w // 2, cx2, by + main_bus_w // 2],
            fill=bus_color
        )

    # Corner bolts on the frame
    bolt_inset = frame_inset // 2 + 2
    for bx, by in [(bolt_inset, bolt_inset),
                   (S - bolt_inset, bolt_inset),
                   (bolt_inset, S - bolt_inset),
                   (S - bolt_inset, S - bolt_inset)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Status LED in the bottom-right of the frame
    led_x = fx2 - 16
    led_y = fy2 - 16
    if full:
        draw_filled_circle(draw, led_x, led_y, 10, LED_GREEN,
                           outline=EDGE_DARK)
        add_radial_glow(img, led_x, led_y, 25, LED_GREEN_GLOW,
                        intensity=0.5)
    else:
        draw_filled_circle(draw, led_x, led_y, 10, LED_RED,
                           outline=EDGE_DARK)

    # Full state: subtle blue glow across the panel
    if full:
        mid_x = (cx1 + cx2) // 2
        mid_y = (cy1 + cy2) // 2
        add_radial_glow(img, mid_x, mid_y,
                        cell_area_w // 2, c_hi, intensity=0.06)

    return img


# ---------------------------------------------------------------------------
#  SIDE FACE — hex armor with yellow output connector
# ---------------------------------------------------------------------------

def make_side():
    """Side face: hex armor with a yellow output connector circle."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    port_outer = int(S * 0.22)
    port_inner = int(S * 0.12)

    # Recessed panel
    panel_r = int(S * 0.30)
    draw.rectangle(
        [cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw,
        cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r,
        EDGE_DARK, ARMOR_LIGHT, width=4
    )

    # Yellow output connector
    draw_connector_port(img, draw, cx, cy, port_outer, port_inner,
                        OUTPUT_RING, OUTPUT_BRIGHT, OUTPUT_GLOW,
                        powered=False)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Horizontal seams
    m = 30
    draw.line([(m, cy), (cx - panel_r - 10, cy)],
              fill=SEAM_COLOR, width=2)
    draw.line([(cx + panel_r + 10, cy), (S - m, cy)],
              fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  BOTTOM FACE — hex armor base plate
# ---------------------------------------------------------------------------

def make_bottom():
    """Bottom face: hex armor with vent grille and mounting feet."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Central vent grille
    grille_size = 360
    g1 = cx - grille_size // 2
    g2 = cx + grille_size // 2
    draw.rectangle([g1, g1, g2, g2], fill=ARMOR_MID, outline=ARMOR_LIGHT)
    add_bevel_border(draw, g1, g1, g2, g2, ARMOR_LIGHT, EDGE_DARK,
                     width=3)

    # Vent slots
    for vy in range(g1 + 24, g2 - 16, 22):
        draw.rectangle([g1 + 18, vy, g2 - 18, vy + 8], fill=EDGE_DARK)
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
        add_bevel_border(draw, fx, fy, fx + foot_size, fy + foot_size,
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

    textures["small_solar_panel"] = make_top(full=False)
    textures["small_solar_panel_full"] = make_top(full=True)
    textures["small_solar_panel_side"] = make_side()
    textures["small_solar_panel_bottom"] = make_bottom()

    save_textures(textures)


if __name__ == "__main__":
    main()
