#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Obsidian Factory at 1024x1024.

Concept: Heavy industrial obsidian production unit with dark purple
accent color. North face has control panel with gauges. South has an
output port. East/West are side panels with cooling vents. Top has a
mixing chamber viewport. Bottom has a heavy base plate.

Active state adds purple glow effects throughout.

Creates 12 textures:
  6 faces (north, south, east, west, up, down) × 2 states (idle, active)
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

# Obsidian / purple accent
OBSIDIAN_DARK = (26, 10, 46, 255)
OBSIDIAN_MID = (55, 20, 90, 255)
OBSIDIAN_BRIGHT = (75, 0, 130, 255)
OBSIDIAN_GLOW = (120, 40, 200, 255)
OBSIDIAN_ACCENT = (150, 60, 255, 255)

# Gauge
GAUGE_BG = (18, 20, 26, 255)
GAUGE_RIM = (85, 90, 102, 255)
GAUGE_TICK = (110, 115, 128, 255)

# Port / connector
HOLE_DARK = (12, 14, 18, 255)
PORT_RIM = (80, 85, 98, 255)

# Vent
VENT_SLOT = (15, 18, 24, 255)

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


def draw_small_gauge(draw, gx, gy, radius, active=False):
    """Draw a small circular gauge."""
    draw_filled_circle(draw, gx, gy, radius + 3, GAUGE_RIM,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, gx, gy, radius, GAUGE_BG)

    for i in range(7):
        angle = math.radians(225 - i * 30)
        tx1 = int(gx + (radius - 3) * math.cos(angle))
        ty1 = int(gy - (radius - 3) * math.sin(angle))
        tx2 = int(gx + (radius - 7) * math.cos(angle))
        ty2 = int(gy - (radius - 7) * math.sin(angle))
        draw.line([(tx1, ty1), (tx2, ty2)], fill=GAUGE_TICK, width=1)

    if active:
        needle_angle = math.radians(315)
        needle_color = OBSIDIAN_ACCENT
    else:
        needle_angle = math.radians(225)
        needle_color = GAUGE_TICK

    nx = int(gx + (radius - 8) * math.cos(needle_angle))
    ny = int(gy - (radius - 8) * math.sin(needle_angle))
    draw.line([(gx, gy), (nx, ny)], fill=needle_color, width=2)
    draw_filled_circle(draw, gx, gy, 3, needle_color)


# ---------------------------------------------------------------------------
#  North face — control panel with gauges
# ---------------------------------------------------------------------------

def make_north(active=False):
    """North face: control panel with gauges and status indicators."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    # Upper control panel
    panel_x1 = int(S * 0.08)
    panel_y1 = int(S * 0.10)
    panel_x2 = S - int(S * 0.08)
    panel_y2 = int(S * 0.55)
    draw.rectangle([panel_x1, panel_y1, panel_x2, panel_y2],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, panel_x1, panel_y1, panel_x2, panel_y2,
                     ARMOR_LIGHT, EDGE_DARK, width=3)

    # Three gauges across the panel
    gauge_r = int(S * 0.07)
    gauge_y = int(S * 0.30)
    for i, gx in enumerate([int(S * 0.25), cx, int(S * 0.75)]):
        draw_small_gauge(draw, gx, gauge_y, gauge_r, active=active)

    # Status indicator strip below gauges
    strip_y1 = int(S * 0.42)
    strip_y2 = int(S * 0.50)
    draw.rectangle([panel_x1 + 20, strip_y1, panel_x2 - 20, strip_y2],
                   fill=EDGE_DARK, outline=ARMOR_MID)

    # Status LEDs
    num_leds = 8
    led_spacing = (panel_x2 - panel_x1 - 60) // (num_leds + 1)
    for i in range(num_leds):
        lx = panel_x1 + 40 + (i + 1) * led_spacing
        ly = (strip_y1 + strip_y2) // 2
        if active:
            # Light up progressively in purple
            led_color = OBSIDIAN_ACCENT if i < 6 else OBSIDIAN_MID
            draw_filled_circle(draw, lx, ly, 8, led_color)
        else:
            draw_filled_circle(draw, lx, ly, 8, SEAM_COLOR)

    # Lower output slot
    slot_x1 = int(S * 0.30)
    slot_y1 = int(S * 0.62)
    slot_x2 = int(S * 0.70)
    slot_y2 = int(S * 0.78)
    draw.rectangle([slot_x1, slot_y1, slot_x2, slot_y2],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, slot_x1, slot_y1, slot_x2, slot_y2,
                     EDGE_DARK, ARMOR_LIGHT, width=3)
    # Slot opening
    slot_inner_margin = 15
    slot_color = OBSIDIAN_DARK if active else HOLE_DARK
    draw.rectangle([slot_x1 + slot_inner_margin, slot_y1 + slot_inner_margin,
                    slot_x2 - slot_inner_margin, slot_y2 - slot_inner_margin],
                   fill=slot_color, outline=EDGE_DARK)

    if active:
        # Purple glow from slot
        add_radial_glow(img, (slot_x1 + slot_x2) // 2,
                        (slot_y1 + slot_y2) // 2,
                        int((slot_x2 - slot_x1) * 0.6), OBSIDIAN_GLOW,
                        intensity=0.2)
        draw = ImageDraw.Draw(img)

    # Panel rivets
    for rx in [panel_x1 + 15, panel_x2 - 15]:
        for ry in [panel_y1 + 15, panel_y2 - 15]:
            draw_filled_circle(draw, rx, ry, 8, ARMOR_LIGHT,
                               outline=EDGE_DARK)
            draw_filled_circle(draw, rx, ry, 4, RIVET_COLOR)

    # Seams
    m = 30
    if active:
        add_glowing_seam(img, (m, int(S * 0.88)), (S - m, int(S * 0.88)),
                         SEAM_COLOR, OBSIDIAN_GLOW,
                         seam_width=2, glow_width=8, intensity=0.15)
        draw = ImageDraw.Draw(img)
    else:
        draw.line([(m, int(S * 0.88)), (S - m, int(S * 0.88))],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  South face — output port
# ---------------------------------------------------------------------------

def make_south(active=False):
    """South face: obsidian output port."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    # Recessed panel
    panel_r = int(S * 0.30)
    draw.rectangle([cx - panel_r, cy - panel_r, cx + panel_r, cy + panel_r],
                   fill=ARMOR_MID)
    add_bevel_border(draw, cx - panel_r, cy - panel_r,
                     cx + panel_r, cy + panel_r,
                     EDGE_DARK, ARMOR_LIGHT, width=4)

    # Output port — square with beveled edges (obsidian shaped)
    port_r = int(S * 0.16)
    draw.rectangle([cx - port_r, cy - port_r, cx + port_r, cy + port_r],
                   fill=HOLE_DARK, outline=EDGE_DARK)
    add_bevel_border(draw, cx - port_r, cy - port_r,
                     cx + port_r, cy + port_r,
                     ARMOR_MID, EDGE_DARK, width=5)

    # Inner obsidian color when active
    if active:
        inner_margin = 12
        draw.rectangle([cx - port_r + inner_margin,
                        cy - port_r + inner_margin,
                        cx + port_r - inner_margin,
                        cy + port_r - inner_margin],
                       fill=OBSIDIAN_DARK)
        add_radial_glow(img, cx, cy, port_r, OBSIDIAN_GLOW, intensity=0.3)
        add_glow_ring(img, cx, cy, port_r, port_r + int(port_r * 0.4),
                      OBSIDIAN_GLOW)
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

    # Horizontal seams
    m = 30
    if active:
        add_glowing_seam(img, (m, cy), (cx - panel_r - 10, cy),
                         SEAM_COLOR, OBSIDIAN_GLOW,
                         seam_width=2, glow_width=8, intensity=0.15)
        add_glowing_seam(img, (cx + panel_r + 10, cy), (S - m, cy),
                         SEAM_COLOR, OBSIDIAN_GLOW,
                         seam_width=2, glow_width=8, intensity=0.15)
        draw = ImageDraw.Draw(img)
    else:
        draw.line([(m, cy), (cx - panel_r - 10, cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(cx + panel_r + 10, cy), (S - m, cy)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  East/West — side panels with cooling vents
# ---------------------------------------------------------------------------

def make_side(active=False):
    """Side face (east/west): cooling vent panel."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    # Upper panel strip
    panel_h = int(S * 0.08)
    draw.rectangle([9, 9, S - 10, panel_h + 9], fill=ARMOR_MID)
    add_bevel_border(draw, 9, 9, S - 10, panel_h + 9, ARMOR_LIGHT,
                     EDGE_DARK, width=2)
    for rx in range(60, S - 40, int(S * 0.08)):
        draw_filled_circle(draw, rx, 9 + panel_h // 2, 5, RIVET_COLOR)

    # Lower panel strip
    base_top = S - panel_h - 9
    draw.rectangle([9, base_top, S - 10, S - 10], fill=ARMOR_MID)
    add_bevel_border(draw, 9, base_top, S - 10, S - 10, ARMOR_LIGHT,
                     EDGE_DARK, width=2)
    for rx in range(60, S - 40, int(S * 0.08)):
        draw_filled_circle(draw, rx, base_top + panel_h // 2, 5,
                           RIVET_COLOR)

    # Central cooling vent section
    vent_x1 = int(S * 0.12)
    vent_y1 = panel_h + 9 + 20
    vent_x2 = S - int(S * 0.12)
    vent_y2 = base_top - 20

    draw.rectangle([vent_x1, vent_y1, vent_x2, vent_y2],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, vent_x1, vent_y1, vent_x2, vent_y2,
                     ARMOR_LIGHT, EDGE_DARK, width=3)

    # Vent slots
    num_slots = 10
    slot_margin = 20
    slot_spacing = (vent_y2 - vent_y1 - 2 * slot_margin) // num_slots
    for i in range(num_slots):
        sy = vent_y1 + slot_margin + i * slot_spacing
        draw.rectangle([vent_x1 + slot_margin, sy,
                        vent_x2 - slot_margin, sy + slot_spacing - 8],
                       fill=VENT_SLOT, outline=EDGE_DARK)
        if active:
            # Purple glow through vents
            draw.rectangle([vent_x1 + slot_margin + 4, sy + 2,
                            vent_x2 - slot_margin - 4,
                            sy + slot_spacing - 10],
                           fill=OBSIDIAN_DARK)

    # Circular side port
    port_cx = cx
    port_cy = cy
    port_outer = int(S * 0.12)
    port_inner = int(S * 0.07)

    draw_filled_circle(draw, port_cx, port_cy, port_outer, PORT_RIM,
                       outline=EDGE_DARK)
    for r in range(port_outer - 2, port_inner, -1):
        t = (port_outer - r) / (port_outer - port_inner)
        c = lerp_color(PORT_RIM, ARMOR_MID, t * 0.6)
        draw.ellipse([port_cx - r, port_cy - r, port_cx + r, port_cy + r],
                     outline=c)

    if active:
        draw_filled_circle(draw, port_cx, port_cy, port_inner,
                           OBSIDIAN_DARK, outline=EDGE_DARK)
        add_glow_ring(img, port_cx, port_cy, port_inner,
                      port_inner + int(port_inner * 0.5), OBSIDIAN_GLOW)
        add_radial_glow(img, port_cx, port_cy, port_inner,
                        OBSIDIAN_ACCENT, intensity=0.25)
        draw = ImageDraw.Draw(img)
    else:
        draw_filled_circle(draw, port_cx, port_cy, port_inner,
                           HOLE_DARK, outline=EDGE_DARK)

    # Port bolts
    bolt_r = int((port_outer + port_inner) / 2) + 3
    for i in range(6):
        angle = math.radians(60 * i)
        bx = int(port_cx + bolt_r * math.cos(angle))
        by = int(port_cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 6, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 3, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Top face — mixing chamber viewport
# ---------------------------------------------------------------------------

def make_top(active=False):
    """Top face: mixing chamber with central viewport."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    # Cross seams
    m = 30
    if active:
        add_glowing_seam(img, (m, cy), (S - m, cy), SEAM_COLOR,
                         OBSIDIAN_GLOW, seam_width=3, glow_width=10,
                         intensity=0.15)
        add_glowing_seam(img, (cx, m), (cx, S - m), SEAM_COLOR,
                         OBSIDIAN_GLOW, seam_width=3, glow_width=10,
                         intensity=0.15)
        draw = ImageDraw.Draw(img)
    else:
        draw.line([(m, cy), (S - m, cy)], fill=SEAM_COLOR, width=3)
        draw.line([(cx, m), (cx, S - m)], fill=SEAM_COLOR, width=3)

    # Central mixing chamber viewport
    chamber_outer = int(S * 0.25)
    chamber_inner = int(S * 0.18)

    # Outer housing ring
    draw_filled_circle(draw, cx, cy, chamber_outer + 10, ARMOR_LIGHT,
                       outline=EDGE_DARK)
    for r in range(chamber_outer + 8, chamber_outer - 2, -1):
        t = (chamber_outer + 8 - r) / 10
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Gradient ring
    for r in range(chamber_outer - 2, chamber_inner, -1):
        t = (chamber_outer - r) / (chamber_outer - chamber_inner)
        if active:
            c = lerp_color(ARMOR_MID, OBSIDIAN_DARK, t * 0.5)
        else:
            c = lerp_color(ARMOR_MID, EDGE_DARK, t * 0.4)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Chamber interior
    if active:
        # Swirling obsidian/lava mixture
        draw_filled_circle(draw, cx, cy, chamber_inner, OBSIDIAN_DARK)
        for r in range(chamber_inner, 0, -1):
            t = 1.0 - (r / chamber_inner)
            c = lerp_color(OBSIDIAN_DARK, OBSIDIAN_MID, t ** 0.6)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)
        # Bright core
        core_r = int(chamber_inner * 0.3)
        for r in range(core_r, 0, -1):
            t = 1.0 - (r / core_r)
            c = lerp_color(OBSIDIAN_MID, OBSIDIAN_BRIGHT, t)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

        add_glow_ring(img, cx, cy, chamber_inner,
                      chamber_inner + int(chamber_inner * 0.5),
                      OBSIDIAN_GLOW)
        add_radial_glow(img, cx, cy, chamber_outer + 20,
                        OBSIDIAN_GLOW, intensity=0.2)
        draw = ImageDraw.Draw(img)
    else:
        draw_filled_circle(draw, cx, cy, chamber_inner, EDGE_DARK,
                           outline=ARMOR_MID)

    # Chamber bolts
    bolt_r = int((chamber_outer + chamber_inner) / 2) + 5
    for i in range(8):
        angle = math.radians(45 * i + 22.5)
        bx = int(cx + bolt_r * math.cos(angle))
        by = int(cy + bolt_r * math.sin(angle))
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT, outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Corner quadrant bolts
    for bx, by in [(int(S * 0.12), int(S * 0.12)),
                   (int(S * 0.88), int(S * 0.12)),
                   (int(S * 0.12), int(S * 0.88)),
                   (int(S * 0.88), int(S * 0.88))]:
        draw_filled_circle(draw, bx, by, 12, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 6, RIVET_COLOR)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  Bottom face — heavy base plate
# ---------------------------------------------------------------------------

def make_bottom(active=False):
    """Bottom face: heavy base plate with ventilation."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2

    plate_margin = int(S * 0.08)
    draw.rectangle([plate_margin, plate_margin,
                    S - plate_margin, S - plate_margin],
                   fill=ARMOR_MID, outline=EDGE_DARK)
    add_bevel_border(draw, plate_margin, plate_margin,
                     S - plate_margin, S - plate_margin,
                     ARMOR_LIGHT, EDGE_DARK, width=3)

    # Central vent grille
    grille_margin = int(S * 0.2)
    slot_y_start = grille_margin + 20
    slot_y_end = S - grille_margin - 20
    num_slots = 12
    slot_spacing = (slot_y_end - slot_y_start) // num_slots

    for i in range(num_slots):
        sy = slot_y_start + i * slot_spacing
        draw.rectangle([grille_margin + 10, sy,
                        S - grille_margin - 10, sy + slot_spacing - 8],
                       fill=VENT_SLOT, outline=EDGE_DARK)
        if active:
            draw.rectangle([grille_margin + 14, sy + 2,
                            S - grille_margin - 14,
                            sy + slot_spacing - 10],
                           fill=OBSIDIAN_DARK)

    # Mounting feet
    foot_size = int(S * 0.08)
    foot_positions = [
        (plate_margin + 10, plate_margin + 10),
        (S - plate_margin - foot_size - 10, plate_margin + 10),
        (plate_margin + 10, S - plate_margin - foot_size - 10),
        (S - plate_margin - foot_size - 10,
         S - plate_margin - foot_size - 10),
    ]
    for fx, fy in foot_positions:
        draw.rectangle([fx, fy, fx + foot_size, fy + foot_size],
                       fill=ARMOR_LIGHT, outline=EDGE_DARK)
        add_bevel_border(draw, fx, fy, fx + foot_size, fy + foot_size,
                         RIVET_COLOR, EDGE_DARK, width=2)
        foot_cx = fx + foot_size // 2
        foot_cy = fy + foot_size // 2
        draw_filled_circle(draw, foot_cx, foot_cy, 12, ARMOR_MID,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, foot_cx, foot_cy, 6, RIVET_COLOR)

    if active:
        add_radial_glow(img, cx, cy, int(S * 0.25), OBSIDIAN_GLOW,
                        intensity=0.1)
        draw = ImageDraw.Draw(img)

    add_corner_bolts(draw)
    return img


# ---------------------------------------------------------------------------
#  MAIN
# ---------------------------------------------------------------------------

def main():
    textures = {}

    for active_state, prefix in [(False, "idle"), (True, "active")]:
        textures[f"obsidian_factory_{prefix}_north"] = make_north(active_state)
        textures[f"obsidian_factory_{prefix}_south"] = make_south(active_state)
        textures[f"obsidian_factory_{prefix}_east"] = make_side(active_state)
        textures[f"obsidian_factory_{prefix}_west"] = make_side(active_state)
        textures[f"obsidian_factory_{prefix}_up"] = make_top(active_state)
        textures[f"obsidian_factory_{prefix}_down"] = make_bottom(active_state)

    save_textures(textures)


if __name__ == "__main__":
    main()
