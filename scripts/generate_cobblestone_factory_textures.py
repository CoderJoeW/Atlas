#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Cobblestone Factory at 1024x1024.

Concept: Industrial stone fabricator with a heavy crushing mechanism.
The front face features a large rectangular output chute with a conveyor
of crushed stone visible inside — grinding rollers when active, dark and
still when idle. The sides show heavy mechanical pistons/rams. The top
has a reinforced intake hopper. The back has a power/coolant panel.
The bottom is a reinforced base plate.

Creates 12 textures:
  6 faces (north, south, east, west, up, down) x 2 states (idle, active)
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

# Stone / cobblestone colors
STONE_DARK = (68, 68, 68, 255)
STONE_MID = (105, 105, 105, 255)
STONE_LIGHT = (140, 140, 140, 255)
STONE_HIGHLIGHT = (170, 170, 170, 255)
COBBLE_DARK = (80, 78, 72, 255)
COBBLE_MID = (120, 118, 110, 255)
COBBLE_LIGHT = (155, 152, 145, 255)

# Mechanical / active colors
PISTON_DARK = (50, 52, 58, 255)
PISTON_MID = (72, 76, 86, 255)
PISTON_LIGHT = (90, 95, 108, 255)
PISTON_SHAFT = (130, 135, 150, 255)

# Active glow — orange-amber for mechanical energy
MECH_GLOW = (255, 160, 40, 255)
MECH_DIM = (180, 100, 20, 255)
AMBER_GLOW = (255, 140, 30, 255)
ORANGE_GLOW = (255, 120, 20, 255)

# Crusher interior
INTERIOR_BG = (12, 14, 18, 255)

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


# ---------------------------------------------------------------------------
#  Hex armor base
# ---------------------------------------------------------------------------

def make_hex_armor_base():
    """Create a hex armor face — the foundation for all factory faces."""
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
#  NORTH (FRONT) — crusher output chute with grinding rollers
# ---------------------------------------------------------------------------

def make_north(active=False):
    """Front face: large rectangular crusher chute with visible rollers.

    A wide rectangular opening shows the crushing mechanism inside.
    When active, rollers spin and crushed stone particles are visible.
    When idle, the mechanism is dark and still.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx = S // 2

    # Chute dimensions — wide rectangular opening
    chute_half_w = int(S * 0.32)
    chute_top = int(S * 0.14)
    chute_bottom = int(S * 0.86)

    # Heavy outer frame
    frame_w = 24
    draw.rectangle(
        [cx - chute_half_w - frame_w, chute_top - frame_w,
         cx + chute_half_w + frame_w, chute_bottom + frame_w],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw,
        cx - chute_half_w - frame_w, chute_top - frame_w,
        cx + chute_half_w + frame_w, chute_bottom + frame_w,
        ARMOR_LIGHT, EDGE_DARK, width=4
    )

    # Inner dark opening
    draw.rectangle(
        [cx - chute_half_w, chute_top,
         cx + chute_half_w, chute_bottom],
        fill=INTERIOR_BG
    )

    # Grinding rollers — three horizontal cylinders across the chute
    roller_positions = [
        chute_top + (chute_bottom - chute_top) * 1 // 4,
        chute_top + (chute_bottom - chute_top) * 2 // 4,
        chute_top + (chute_bottom - chute_top) * 3 // 4,
    ]
    roller_radius = 32

    for ry in roller_positions:
        # Draw roller as a horizontal cylinder with shading
        for dy in range(-roller_radius, roller_radius + 1):
            t = abs(dy) / roller_radius
            if active:
                c = lerp_color(PISTON_SHAFT, PISTON_DARK, t)
            else:
                c = lerp_color(PISTON_MID, PISTON_DARK, t)
            draw.line(
                [(cx - chute_half_w + 6, ry + dy),
                 (cx + chute_half_w - 6, ry + dy)],
                fill=c
            )

        # Roller edge lines
        draw.line(
            [(cx - chute_half_w + 6, ry - roller_radius),
             (cx + chute_half_w - 6, ry - roller_radius)],
            fill=EDGE_DARK, width=2
        )
        draw.line(
            [(cx - chute_half_w + 6, ry + roller_radius),
             (cx + chute_half_w - 6, ry + roller_radius)],
            fill=EDGE_DARK, width=2
        )

        # Roller teeth/ridges — vertical lines across the roller
        ridge_spacing = 40
        for rx in range(cx - chute_half_w + 26, cx + chute_half_w - 20,
                        ridge_spacing):
            for dy in range(-roller_radius + 4, roller_radius - 3):
                t = abs(dy) / roller_radius
                ridge_c = lerp_color(ARMOR_LIGHT, EDGE_DARK, t)
                draw.point((rx, ry + dy), fill=ridge_c)
                draw.point((rx + 1, ry + dy), fill=ridge_c)

        # Roller bearing caps on each end
        for bx in [cx - chute_half_w + 6, cx + chute_half_w - 6]:
            draw_filled_circle(draw, bx, ry, roller_radius - 4,
                               PISTON_DARK, outline=EDGE_DARK)
            draw_filled_circle(draw, bx, ry, 10, RIVET_COLOR,
                               outline=EDGE_DARK)

    if active:
        # Crushed stone particles between rollers
        import random
        rng = random.Random(42)  # deterministic seed
        for i in range(60):
            px = rng.randint(cx - chute_half_w + 20,
                             cx + chute_half_w - 20)
            # Place particles between rollers and below the last one
            zone = rng.choice([0, 1, 2, 3])
            if zone == 0:
                py = rng.randint(chute_top + 8,
                                 roller_positions[0] - roller_radius - 4)
            elif zone == 1:
                py = rng.randint(
                    roller_positions[0] + roller_radius + 4,
                    roller_positions[1] - roller_radius - 4)
            elif zone == 2:
                py = rng.randint(
                    roller_positions[1] + roller_radius + 4,
                    roller_positions[2] - roller_radius - 4)
            else:
                py = rng.randint(
                    roller_positions[2] + roller_radius + 4,
                    chute_bottom - 8)
            size = rng.randint(4, 12)
            sc = rng.choice([COBBLE_DARK, COBBLE_MID, COBBLE_LIGHT,
                             STONE_DARK, STONE_MID])
            draw.rectangle([px, py, px + size, py + size], fill=sc)

    # Chute outline on top
    draw.rectangle(
        [cx - chute_half_w, chute_top,
         cx + chute_half_w, chute_bottom],
        outline=EDGE_DARK
    )

    # Frame bolts
    bolt_positions = [
        (cx - chute_half_w - frame_w + 10, chute_top + 10),
        (cx + chute_half_w + frame_w - 10, chute_top + 10),
        (cx - chute_half_w - frame_w + 10, chute_bottom - 10),
        (cx + chute_half_w + frame_w - 10, chute_bottom - 10),
        (cx - chute_half_w - frame_w + 10, S // 2),
        (cx + chute_half_w + frame_w - 10, S // 2),
    ]
    for bx, by in bolt_positions:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)

    if active:
        add_radial_glow(img, cx, int(S * 0.5),
                        int(S * 0.3), AMBER_GLOW, intensity=0.12)

    return img


# ---------------------------------------------------------------------------
#  SOUTH (BACK) — power connection panel
# ---------------------------------------------------------------------------

def make_south(active=False):
    """Back face: hex armor with power connection panel and vents."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Large panel
    panel_w = int(S * 0.50)
    panel_h = int(S * 0.44)
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

    # Power connector circle at bottom of panel
    conn_cy = py2 - 50
    conn_r = 30
    if active:
        draw_filled_circle(draw, cx, conn_cy, conn_r + 4,
                           AMBER_GLOW, outline=EDGE_DARK)
    draw_filled_circle(draw, cx, conn_cy, conn_r,
                       PISTON_DARK if not active else MECH_DIM,
                       outline=EDGE_DARK)
    draw_filled_circle(draw, cx, conn_cy, 12,
                       RIVET_COLOR if not active else MECH_GLOW)

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
    if active:
        add_glowing_seam(
            img, (m, cy), (px1 - 10, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
        add_glowing_seam(
            img, (px2 + 10, cy), (S - m, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
    else:
        draw.line([(m, cy), (px1 - 10, cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(px2 + 10, cy), (S - m, cy)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    if active:
        add_radial_glow(img, cx, conn_cy, 50,
                        ORANGE_GLOW, intensity=0.15)

    return img


# ---------------------------------------------------------------------------
#  EAST / WEST (SIDES) — hydraulic pistons / rams
# ---------------------------------------------------------------------------

def make_side(active=False):
    """Side face: hex armor with two vertical hydraulic pistons.

    Heavy piston assemblies run vertically, suggesting the crushing
    force mechanism. When active, piston shafts glow with energy.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Two vertical pistons
    piston_w = 54
    piston_gap = int(S * 0.28)
    piston_top = 55
    piston_bottom = S - 55

    for piston_cx in [cx - piston_gap // 2, cx + piston_gap // 2]:
        ppx1 = piston_cx - piston_w // 2
        ppx2 = piston_cx + piston_w // 2

        # Piston housing (outer)
        draw.rectangle(
            [ppx1 - 8, piston_top, ppx2 + 8, piston_bottom],
            fill=ARMOR_MID, outline=EDGE_DARK
        )

        # Piston shaft with gradient
        if active:
            for x in range(ppx1, ppx2 + 1):
                t = abs(x - piston_cx) / (piston_w // 2)
                c = lerp_color(PISTON_SHAFT, PISTON_MID, t)
                draw.line([(x, piston_top + 10), (x, piston_bottom - 10)],
                          fill=c)
        else:
            for x in range(ppx1, ppx2 + 1):
                t = abs(x - piston_cx) / (piston_w // 2)
                c = lerp_color(PISTON_LIGHT, PISTON_DARK, t)
                draw.line([(x, piston_top + 10), (x, piston_bottom - 10)],
                          fill=c)

        # Piston edge lines
        draw.line([(ppx1, piston_top + 10), (ppx1, piston_bottom - 10)],
                  fill=EDGE_DARK, width=2)
        draw.line([(ppx2, piston_top + 10), (ppx2, piston_bottom - 10)],
                  fill=EDGE_DARK, width=2)

        # Piston clamp brackets
        clamp_color = ARMOR_LIGHT if not active else (90, 80, 55, 255)
        for clamp_y in range(piston_top + 80, piston_bottom - 60, 160):
            clamp_h = 20
            draw.rectangle(
                [ppx1 - 14, clamp_y, ppx2 + 14, clamp_y + clamp_h],
                fill=clamp_color, outline=EDGE_DARK
            )
            draw_filled_circle(draw, ppx1 - 8, clamp_y + clamp_h // 2,
                               5, RIVET_COLOR)
            draw_filled_circle(draw, ppx2 + 8, clamp_y + clamp_h // 2,
                               5, RIVET_COLOR)

        # Piston head cap (top)
        draw.rectangle(
            [ppx1 - 4, piston_top, ppx2 + 4, piston_top + 30],
            fill=ARMOR_LIGHT, outline=EDGE_DARK
        )
        add_bevel_border(draw, ppx1 - 4, piston_top, ppx2 + 4,
                         piston_top + 30, (85, 90, 105, 255),
                         EDGE_DARK, width=2)

        # Piston foot (bottom)
        draw.rectangle(
            [ppx1 - 4, piston_bottom - 30, ppx2 + 4, piston_bottom],
            fill=ARMOR_LIGHT, outline=EDGE_DARK
        )
        add_bevel_border(draw, ppx1 - 4, piston_bottom - 30, ppx2 + 4,
                         piston_bottom, (85, 90, 105, 255),
                         EDGE_DARK, width=2)

    # Horizontal seam between pistons
    m = 30
    left_edge = cx - piston_gap // 2 - piston_w // 2 - 22
    right_edge = cx + piston_gap // 2 + piston_w // 2 + 22
    if active:
        add_glowing_seam(
            img, (m, cy), (left_edge, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=6, intensity=0.12
        )
        add_glowing_seam(
            img, (right_edge, cy), (S - m, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=6, intensity=0.12
        )
    else:
        draw.line([(m, cy), (left_edge, cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(right_edge, cy), (S - m, cy)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    if active:
        for piston_cx in [cx - piston_gap // 2, cx + piston_gap // 2]:
            add_radial_glow(img, piston_cx, cy,
                            piston_w + 20, AMBER_GLOW,
                            intensity=0.08)

    return img


# ---------------------------------------------------------------------------
#  UP (TOP) — intake hopper
# ---------------------------------------------------------------------------

def make_top(active=False):
    """Top face: square intake hopper with thick armored rim.

    A large square hopper opening in the center where raw materials
    could be fed in. Stone texture visible inside when idle,
    glowing conveyor energy when active.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Hopper outer rim
    hopper_outer = int(S * 0.34)
    hopper_inner = int(S * 0.26)

    # Thick metallic rim
    draw.rectangle(
        [cx - hopper_outer, cy - hopper_outer,
         cx + hopper_outer, cy + hopper_outer],
        fill=ARMOR_MID
    )
    add_bevel_border(
        draw,
        cx - hopper_outer, cy - hopper_outer,
        cx + hopper_outer, cy + hopper_outer,
        ARMOR_LIGHT, EDGE_DARK, width=5
    )

    # Inner opening
    draw.rectangle(
        [cx - hopper_inner, cy - hopper_inner,
         cx + hopper_inner, cy + hopper_inner],
        fill=INTERIOR_BG
    )

    if active:
        # Warm interior glow suggesting active machinery below
        for r in range(hopper_inner, 0, -1):
            t = r / hopper_inner
            c = lerp_color(MECH_DIM, INTERIOR_BG, t * 0.8)
            draw.rectangle(
                [cx - r, cy - r, cx + r, cy + r],
                outline=c
            )
    else:
        # Visible stone rubble inside the hopper
        import random
        rng = random.Random(99)
        for i in range(30):
            sx = rng.randint(cx - hopper_inner + 10,
                             cx + hopper_inner - 20)
            sy = rng.randint(cy - hopper_inner + 10,
                             cy + hopper_inner - 20)
            sz = rng.randint(8, 22)
            sc = rng.choice([STONE_DARK, STONE_MID, COBBLE_DARK,
                             COBBLE_MID])
            draw.rectangle([sx, sy, sx + sz, sy + sz], fill=sc,
                           outline=EDGE_DARK)

    # Inner opening outline on top
    draw.rectangle(
        [cx - hopper_inner, cy - hopper_inner,
         cx + hopper_inner, cy + hopper_inner],
        outline=EDGE_DARK
    )

    # Rim bolts
    bolt_positions = [
        (cx - hopper_outer + 16, cy - hopper_outer + 16),
        (cx + hopper_outer - 16, cy - hopper_outer + 16),
        (cx - hopper_outer + 16, cy + hopper_outer - 16),
        (cx + hopper_outer - 16, cy + hopper_outer - 16),
        (cx, cy - hopper_outer + 16),
        (cx, cy + hopper_outer - 16),
        (cx - hopper_outer + 16, cy),
        (cx + hopper_outer - 16, cy),
    ]
    for bx, by in bolt_positions:
        draw_filled_circle(draw, bx, by, 8, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 4, RIVET_COLOR)

    # Cross seams from corners to hopper
    m = 30
    h_edge = hopper_outer + 16
    if active:
        for sx, sy, ex, ey in [
            (m, m, cx - h_edge, cy - h_edge),
            (S - m, m, cx + h_edge, cy - h_edge),
            (m, S - m, cx - h_edge, cy + h_edge),
            (S - m, S - m, cx + h_edge, cy + h_edge),
        ]:
            add_glowing_seam(
                img, (sx, sy), (ex, ey),
                SEAM_COLOR, AMBER_GLOW,
                seam_width=2, glow_width=6, intensity=0.10
            )
    else:
        for sx, sy, ex, ey in [
            (m, m, cx - h_edge, cy - h_edge),
            (S - m, m, cx + h_edge, cy - h_edge),
            (m, S - m, cx - h_edge, cy + h_edge),
            (S - m, S - m, cx + h_edge, cy + h_edge),
        ]:
            draw.line([(sx, sy), (ex, ey)],
                      fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    if active:
        add_radial_glow(img, cx, cy, hopper_inner + 30,
                        ORANGE_GLOW, intensity=0.15)

    return img


# ---------------------------------------------------------------------------
#  DOWN (BOTTOM) — reinforced base plate
# ---------------------------------------------------------------------------

def make_bottom(active=False):
    """Bottom face: heavy reinforced base plate with central vent."""
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
    textures = {}

    for active in (False, True):
        state = "active" if active else "idle"

        textures[f"cobblestone_factory_{state}_north"] = make_north(
            active=active
        )
        textures[f"cobblestone_factory_{state}_south"] = make_south(
            active=active
        )
        textures[f"cobblestone_factory_{state}_east"] = make_side(
            active=active
        )
        textures[f"cobblestone_factory_{state}_west"] = make_side(
            active=active
        )
        textures[f"cobblestone_factory_{state}_up"] = make_top(
            active=active
        )
        textures[f"cobblestone_factory_{state}_down"] = make_bottom(
            active=active
        )

    save_textures(textures)


if __name__ == "__main__":
    main()
