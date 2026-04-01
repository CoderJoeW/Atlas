#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Fluid Merger at 1024x1024.

Concept: Same design language as Power Merger — dark armored housing with
honeycomb grid, centered connector port in a recessed panel. Color coding:
  - Input faces (back/side/top/bottom): always RED (matches power merger)
  - Output face (front): GREY when empty, fluid-colored when active
    - water: blue
    - lava: orange
    - experience: green

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

# Housing / armor (consistent with other Atlas blocks)
ARMOR_DARK = (38, 40, 48, 255)
ARMOR_MID = (52, 56, 66, 255)
ARMOR_LIGHT = (70, 75, 88, 255)
HEX_LINE = (48, 52, 62, 255)
EDGE_DARK = (22, 24, 30, 255)
SEAM_COLOR = (30, 33, 40, 255)
RIVET_COLOR = (100, 108, 125, 255)

# Connector hole
CONNECTOR_HOLE = (12, 14, 18, 255)

# Front (output) — grey when empty (no fluid flowing)
FRONT_RING = (120, 125, 135, 255)
FRONT_BRIGHT = (160, 165, 175, 255)
FRONT_GLOW = (140, 145, 155, 255)

# Input faces — always red (matches power merger input convention)
INPUT_RING = (200, 50, 40, 255)
INPUT_BRIGHT = (255, 70, 50, 255)
INPUT_GLOW = (255, 60, 40, 255)

# Output fluid overrides — when a fluid is active, the output face
# changes to the fluid's color. Input faces stay red always.
OUTPUT_FLUID_COLORS = {
    "none": None,
    "water": {
        "ring": (30, 100, 220, 255),
        "bright": (50, 140, 255, 255),
        "glow": (40, 120, 255, 255),
    },
    "lava": {
        "ring": (220, 120, 30, 255),
        "bright": (255, 160, 50, 255),
        "glow": (255, 140, 40, 255),
    },
    "xp": {
        "ring": (50, 200, 50, 255),
        "bright": (80, 255, 80, 255),
        "glow": (60, 240, 60, 255),
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


def draw_connector_port(img, draw, cx, cy, outer_r, inner_r,
                        ring_color, bright_color, glow_color,
                        active=False):
    """Draw a color-coded circular connector port with contact pins."""
    active_ring = ring_color if not active else bright_color

    for r in range(outer_r, inner_r, -1):
        t = (outer_r - r) / (outer_r - inner_r)
        c = lerp_color(active_ring, ARMOR_MID, t * 0.6)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    draw_filled_circle(draw, cx, cy, inner_r, CONNECTOR_HOLE,
                       outline=EDGE_DARK)

    pin_r = int(inner_r * 0.5)
    pin_size = max(4, inner_r // 12)
    pin_color = ring_color if not active else bright_color
    for angle_deg in [0, 90, 180, 270]:
        angle = math.radians(angle_deg)
        px = int(cx + pin_r * math.cos(angle))
        py = int(cy + pin_r * math.sin(angle))
        draw_filled_circle(draw, px, py, pin_size, pin_color)

    if active:
        add_glow_ring(img, cx, cy, outer_r,
                      outer_r + int(outer_r * 0.35), glow_color)
        add_radial_glow(img, cx, cy, inner_r, bright_color,
                        intensity=0.3)


# ---------------------------------------------------------------------------
#  Fluid-specific details (differentiate from power merger)
# ---------------------------------------------------------------------------

PIPE_COLOR = (60, 65, 78, 255)
PIPE_HIGHLIGHT = (80, 85, 98, 255)
PIPE_SHADOW = (30, 33, 40, 255)
FLANGE_COLOR = (90, 95, 108, 255)
GAUGE_BG = (18, 20, 26, 255)
GAUGE_RIM = (85, 90, 102, 255)
GAUGE_TICK = (110, 115, 128, 255)
GAUGE_NEEDLE = (200, 50, 40, 255)


def draw_pipe_stub(draw, x1, y1, x2, y2, horizontal=True):
    """Draw a pipe stub with highlight and flange ends."""
    pw = 18  # pipe width
    if horizontal:
        draw.rectangle([x1, y1 - pw // 2, x2, y1 + pw // 2], fill=PIPE_COLOR)
        draw.line([(x1, y1 - pw // 2), (x2, y1 - pw // 2)],
                  fill=PIPE_HIGHLIGHT, width=2)
        draw.line([(x1, y1 + pw // 2), (x2, y1 + pw // 2)],
                  fill=PIPE_SHADOW, width=2)
        # Flanges at each end
        fw = pw + 10
        for fx in [x1, x2]:
            draw.rectangle([fx - 4, y1 - fw // 2, fx + 4, y1 + fw // 2],
                           fill=FLANGE_COLOR, outline=EDGE_DARK)
    else:
        draw.rectangle([x1 - pw // 2, y1, x1 + pw // 2, y2], fill=PIPE_COLOR)
        draw.line([(x1 - pw // 2, y1), (x1 - pw // 2, y2)],
                  fill=PIPE_HIGHLIGHT, width=2)
        draw.line([(x1 + pw // 2, y1), (x1 + pw // 2, y2)],
                  fill=PIPE_SHADOW, width=2)
        fw = pw + 10
        for fy in [y1, y2]:
            draw.rectangle([x1 - fw // 2, fy - 4, x1 + fw // 2, fy + 4],
                           fill=FLANGE_COLOR, outline=EDGE_DARK)


def draw_pressure_gauge(draw, gx, gy, radius, fluid, glow_color=None):
    """Draw a small circular pressure gauge."""
    # Gauge body
    draw_filled_circle(draw, gx, gy, radius + 3, FLANGE_COLOR,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, gx, gy, radius, GAUGE_BG)
    draw_filled_circle(draw, gx, gy, radius, None, outline=GAUGE_RIM)

    # Tick marks around the gauge (from 7 o'clock to 5 o'clock)
    for i in range(7):
        angle = math.radians(225 - i * 30)
        tx1 = int(gx + (radius - 3) * math.cos(angle))
        ty1 = int(gy - (radius - 3) * math.sin(angle))
        tx2 = int(gx + (radius - 7) * math.cos(angle))
        ty2 = int(gy - (radius - 7) * math.sin(angle))
        draw.line([(tx1, ty1), (tx2, ty2)], fill=GAUGE_TICK, width=1)

    # Needle — points higher when fluid is active
    if fluid == "none":
        needle_angle = math.radians(210)  # low / empty
    elif fluid in ("water", "lava", "xp"):
        needle_angle = math.radians(330)  # high / full
    else:
        needle_angle = math.radians(270)  # mid

    needle_color = glow_color if glow_color else GAUGE_NEEDLE
    nx = int(gx + (radius - 8) * math.cos(needle_angle))
    ny = int(gy - (radius - 8) * math.sin(needle_angle))
    draw.line([(gx, gy), (nx, ny)], fill=needle_color, width=2)
    draw_filled_circle(draw, gx, gy, 3, needle_color)


def add_fluid_details(draw, cx, cy, panel_r, port_outer, fluid,
                      glow_color, seam_style):
    """Add pipe stubs and pressure gauge to differentiate from power merger."""
    # Pipe stubs from panel edge toward the connector port
    pipe_end = port_outer + 15

    if seam_style in ("horizontal", "cross", "none"):
        # Left pipe stub
        draw_pipe_stub(draw, cx - panel_r + 8, cy,
                       cx - pipe_end, cy, horizontal=True)
        # Right pipe stub
        draw_pipe_stub(draw, cx + pipe_end, cy,
                       cx + panel_r - 8, cy, horizontal=True)

    if seam_style in ("cross",):
        # Top pipe stub
        draw_pipe_stub(draw, cx, cy - panel_r + 8,
                       cx, cy - pipe_end, horizontal=False)
        # Bottom pipe stub
        draw_pipe_stub(draw, cx, cy + pipe_end,
                       cx, cy + panel_r - 8, horizontal=False)

    # Pressure gauge in upper-right corner of panel
    gauge_r = 22
    gauge_x = cx + panel_r - 45
    gauge_y = cy - panel_r + 45
    draw_pressure_gauge(draw, gauge_x, gauge_y, gauge_r, fluid,
                        glow_color=glow_color if fluid != "none" else None)


# ---------------------------------------------------------------------------
#  Face builder
# ---------------------------------------------------------------------------

def _get_colors(fluid, default_ring, default_bright, default_glow,
                is_output=False):
    """Return (ring, bright, glow, is_active) for a given fluid state.

    Input faces always use their default colors (red) regardless of fluid.
    Output face switches to the fluid's color when active.
    """
    if not is_output:
        # Input faces: always red, glow when fluid is active
        active = fluid != "none"
        return default_ring, default_bright, default_glow, active
    fc = OUTPUT_FLUID_COLORS[fluid]
    if fc is None:
        return default_ring, default_bright, default_glow, False
    return fc["ring"], fc["bright"], fc["glow"], True


def make_face(fluid, default_ring, default_bright, default_glow,
              seam_style="horizontal", is_output=False):
    """Build a face with a single centered connector in a recessed panel."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2
    port_outer = int(S * 0.22)
    port_inner = int(S * 0.12)

    ring, bright, glow, active = _get_colors(
        fluid, default_ring, default_bright, default_glow,
        is_output=is_output
    )

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

    # Connector port
    draw_connector_port(img, draw, cx, cy, port_outer, port_inner,
                        ring, bright, glow, active=active)

    # Fluid-specific details (pipe stubs + pressure gauge)
    add_fluid_details(draw, cx, cy, panel_r, port_outer, fluid,
                      glow, seam_style)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    # Seams
    m = 30
    seam_glow = glow if active else None
    if seam_style in ("horizontal", "cross"):
        if active:
            add_glowing_seam(
                img, (m, cy), (cx - panel_r - 10, cy),
                SEAM_COLOR, seam_glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
            add_glowing_seam(
                img, (cx + panel_r + 10, cy), (S - m, cy),
                SEAM_COLOR, seam_glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
        else:
            draw.line([(m, cy), (cx - panel_r - 10, cy)],
                      fill=SEAM_COLOR, width=2)
            draw.line([(cx + panel_r + 10, cy), (S - m, cy)],
                      fill=SEAM_COLOR, width=2)

    if seam_style == "cross":
        if active:
            add_glowing_seam(
                img, (cx, m), (cx, cy - panel_r - 10),
                SEAM_COLOR, seam_glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
            add_glowing_seam(
                img, (cx, cy + panel_r + 10), (cx, S - m),
                SEAM_COLOR, seam_glow,
                seam_width=2, glow_width=8, intensity=0.15
            )
        else:
            draw.line([(cx, m), (cx, cy - panel_r - 10)],
                      fill=SEAM_COLOR, width=2)
            draw.line([(cx, cy + panel_r + 10), (cx, S - m)],
                      fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Public face functions
# ---------------------------------------------------------------------------

def make_front(fluid="none"):
    """Front face (output): bright cyan connector."""
    return make_face(
        fluid, FRONT_RING, FRONT_BRIGHT, FRONT_GLOW,
        seam_style="none", is_output=True
    )


def make_back(fluid="none"):
    """Back face (input): deep blue connector."""
    return make_face(
        fluid, INPUT_RING, INPUT_BRIGHT, INPUT_GLOW,
        seam_style="none"
    )


def make_side(fluid="none"):
    """Side face: input deep blue connector with horizontal seams."""
    return make_face(
        fluid, INPUT_RING, INPUT_BRIGHT, INPUT_GLOW,
        seam_style="horizontal"
    )


def make_top(fluid="none"):
    """Top face: large gauge showing fluid fill level."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Recessed panel
    panel_r = int(S * 0.38)
    draw.rectangle(
        [cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw,
        cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r,
        EDGE_DARK, ARMOR_LIGHT, width=4
    )

    # Determine fluid colors and needle angle
    active = fluid != "none"
    fc = OUTPUT_FLUID_COLORS[fluid]
    if fc is None:
        needle_color = GAUGE_TICK
        zone_color = (60, 65, 78, 255)
        needle_angle_deg = 225  # low / empty
    else:
        needle_color = fc["bright"]
        zone_color = fc["ring"]
        needle_angle_deg = 315  # high / full

    # Large gauge circle
    gauge_r = int(S * 0.28)
    # Outer bezel ring
    draw_filled_circle(draw, cx, cy, gauge_r + 8, FLANGE_COLOR,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, gauge_r + 4, GAUGE_RIM)
    # Gauge face
    draw_filled_circle(draw, cx, cy, gauge_r, GAUGE_BG)

    # Colored arc zone (from 7 o'clock to 1 o'clock, showing capacity)
    arc_inner = gauge_r - 30
    arc_outer = gauge_r - 10
    for deg in range(225, 314):
        angle = math.radians(deg)
        for r in range(arc_inner, arc_outer):
            ax = int(cx + r * math.cos(angle))
            ay = int(cy - r * math.sin(angle))
            if 0 <= ax < S and 0 <= ay < S:
                t = 0.3 if not active else 0.7
                base = img.getpixel((ax, ay))
                img.putpixel((ax, ay), blend_over(base, zone_color, t))

    # Re-get draw after putpixel operations
    draw = ImageDraw.Draw(img)

    # Major tick marks (7 o'clock to 1 o'clock, 9 ticks)
    for i in range(9):
        angle = math.radians(225 - i * 11.25)  # ~90 degree sweep
        # Outer tick
        tx1 = int(cx + (gauge_r - 6) * math.cos(angle))
        ty1 = int(cy - (gauge_r - 6) * math.sin(angle))
        tx2 = int(cx + (gauge_r - 20) * math.cos(angle))
        ty2 = int(cy - (gauge_r - 20) * math.sin(angle))
        w = 3 if i % 4 == 0 else 1
        draw.line([(tx1, ty1), (tx2, ty2)], fill=GAUGE_TICK, width=w)

    # Minor tick marks
    for i in range(33):
        angle = math.radians(225 - i * 2.8125)
        tx1 = int(cx + (gauge_r - 6) * math.cos(angle))
        ty1 = int(cy - (gauge_r - 6) * math.sin(angle))
        tx2 = int(cx + (gauge_r - 12) * math.cos(angle))
        ty2 = int(cy - (gauge_r - 12) * math.sin(angle))
        draw.line([(tx1, ty1), (tx2, ty2)], fill=EDGE_DARK, width=1)

    # Labels: "E" (empty) near 7 o'clock, "F" (full) near 1 o'clock
    e_angle = math.radians(220)
    e_x = int(cx + (gauge_r - 45) * math.cos(e_angle))
    e_y = int(cy - (gauge_r - 45) * math.sin(e_angle))
    # Draw E as lines
    draw.line([(e_x - 6, e_y - 8), (e_x - 6, e_y + 8)],
              fill=GAUGE_TICK, width=2)
    draw.line([(e_x - 6, e_y - 8), (e_x + 5, e_y - 8)],
              fill=GAUGE_TICK, width=2)
    draw.line([(e_x - 6, e_y), (e_x + 3, e_y)],
              fill=GAUGE_TICK, width=2)
    draw.line([(e_x - 6, e_y + 8), (e_x + 5, e_y + 8)],
              fill=GAUGE_TICK, width=2)

    f_angle = math.radians(320)
    f_x = int(cx + (gauge_r - 45) * math.cos(f_angle))
    f_y = int(cy - (gauge_r - 45) * math.sin(f_angle))
    # Draw F as lines
    draw.line([(f_x - 6, f_y - 8), (f_x - 6, f_y + 8)],
              fill=GAUGE_TICK, width=2)
    draw.line([(f_x - 6, f_y - 8), (f_x + 5, f_y - 8)],
              fill=GAUGE_TICK, width=2)
    draw.line([(f_x - 6, f_y), (f_x + 3, f_y)],
              fill=GAUGE_TICK, width=2)

    # Needle
    needle_angle = math.radians(needle_angle_deg)
    needle_len = gauge_r - 35
    nx = int(cx + needle_len * math.cos(needle_angle))
    ny = int(cy - needle_len * math.sin(needle_angle))
    draw.line([(cx, cy), (nx, ny)], fill=needle_color, width=4)
    # Needle hub
    draw_filled_circle(draw, cx, cy, 12, FLANGE_COLOR, outline=EDGE_DARK)
    draw_filled_circle(draw, cx, cy, 6, needle_color)

    # Glow effect when active
    if active:
        add_glow_ring(img, cx, cy, gauge_r,
                      gauge_r + int(gauge_r * 0.4), fc["glow"])
        add_radial_glow(img, cx, cy, gauge_r + 30,
                        fc["glow"], intensity=0.45)
        draw = ImageDraw.Draw(img)

    # Panel bolts
    bolt_off = panel_r - 16
    for bx, by in [(cx - bolt_off, cy - bolt_off),
                   (cx + bolt_off, cy - bolt_off),
                   (cx - bolt_off, cy + bolt_off),
                   (cx + bolt_off, cy + bolt_off)]:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


def make_bottom(fluid="none"):
    """Bottom face: input deep blue connector with cross seams."""
    return make_face(
        fluid, INPUT_RING, INPUT_BRIGHT, INPUT_GLOW,
        seam_style="cross"
    )


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
