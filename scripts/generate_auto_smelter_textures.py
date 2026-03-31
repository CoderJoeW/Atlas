#!/usr/bin/env python3
"""Generate sci-fi hex-armor textures for Auto Smelter at 1024x1024.

Concept: Industrial sci-fi furnace that screams "I smelt things" at
first glance. The front face dominates with a massive arched furnace
mouth showing a roaring fire when active (dark embers when idle).
The sides feature thick vertical heat pipes that glow orange when
active. The top has a heavy armored chimney/smokestack opening.
The back has an exhaust grate. The bottom is a reinforced base plate.

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

# Furnace fire / heat colors
FIRE_DEEP = (50, 12, 2, 255)
FIRE_DARK = (120, 35, 5, 255)
FIRE_MID = (220, 100, 15, 255)
FIRE_BRIGHT = (255, 170, 40, 255)
FIRE_HOT = (255, 230, 100, 255)
EMBER_DIM = (60, 20, 8, 255)
EMBER_WARM = (100, 35, 10, 255)
AMBER_GLOW = (255, 140, 30, 255)
ORANGE_GLOW = (255, 120, 20, 255)

# Furnace mouth
MOUTH_BG = (8, 10, 14, 255)

# Heat pipe colors
PIPE_DARK = (45, 48, 56, 255)
PIPE_MID = (60, 64, 74, 255)
PIPE_LIGHT = (78, 82, 94, 255)
PIPE_HOT = (180, 80, 20, 255)
PIPE_GLOWING = (220, 110, 25, 255)

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
    """Create a hex armor face — the foundation for all smelter faces."""
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


def draw_arch(draw, cx, top_y, bottom_y, half_width, fill):
    """Draw a filled arch shape (rectangle with semicircular top)."""
    # Straight sides
    draw.rectangle(
        [cx - half_width, top_y + half_width, cx + half_width, bottom_y],
        fill=fill
    )
    # Semicircular arch top
    draw.pieslice(
        [cx - half_width, top_y,
         cx + half_width, top_y + half_width * 2],
        start=180, end=360, fill=fill
    )


def draw_arch_outline(draw, cx, top_y, bottom_y, half_width,
                      outline_color, width=3):
    """Draw the outline of an arch shape."""
    # Left side
    draw.line(
        [(cx - half_width, top_y + half_width),
         (cx - half_width, bottom_y)],
        fill=outline_color, width=width
    )
    # Right side
    draw.line(
        [(cx + half_width, top_y + half_width),
         (cx + half_width, bottom_y)],
        fill=outline_color, width=width
    )
    # Bottom
    draw.line(
        [(cx - half_width, bottom_y),
         (cx + half_width, bottom_y)],
        fill=outline_color, width=width
    )
    # Arch top
    draw.arc(
        [cx - half_width, top_y,
         cx + half_width, top_y + half_width * 2],
        start=180, end=360, fill=outline_color, width=width
    )


# ---------------------------------------------------------------------------
#  NORTH (FRONT) — massive furnace mouth with arch
# ---------------------------------------------------------------------------

def make_north(active=False):
    """Front face: dominant arched furnace mouth.

    A huge arch opening takes up most of the face, making it
    unmistakably a furnace. Fire/embers visible inside.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx = S // 2
    # Arch dimensions — big and dominant
    arch_half_w = int(S * 0.34)
    arch_top = int(S * 0.10)
    arch_bottom = int(S * 0.88)

    # Heavy outer frame (thick border around the arch)
    frame_w = 24
    draw_arch(draw, cx, arch_top - frame_w,
              arch_bottom + frame_w,
              arch_half_w + frame_w, ARMOR_MID)
    draw_arch_outline(draw, cx, arch_top - frame_w,
                      arch_bottom + frame_w,
                      arch_half_w + frame_w,
                      ARMOR_LIGHT, width=4)

    # Inner frame border (dark inset)
    draw_arch(draw, cx, arch_top, arch_bottom,
              arch_half_w, EDGE_DARK)

    # Fire interior
    if active:
        # Build fire from bottom up: hottest at bottom
        interior_top = arch_top + arch_half_w  # below the arch curve
        for y in range(arch_bottom, interior_top - 1, -1):
            t = (arch_bottom - y) / max(1, arch_bottom - interior_top)
            if t < 0.15:
                c = lerp_color(FIRE_HOT, FIRE_BRIGHT, t / 0.15)
            elif t < 0.4:
                c = lerp_color(FIRE_BRIGHT, FIRE_MID,
                               (t - 0.15) / 0.25)
            elif t < 0.7:
                c = lerp_color(FIRE_MID, FIRE_DARK,
                               (t - 0.4) / 0.3)
            else:
                c = lerp_color(FIRE_DARK, FIRE_DEEP,
                               (t - 0.7) / 0.3)
            # Determine x bounds at this y level
            if y <= arch_top + arch_half_w:
                # In the arch curve region
                dy = y - (arch_top + arch_half_w)
                r_sq = arch_half_w ** 2 - dy ** 2
                if r_sq > 0:
                    half_x = int(math.sqrt(r_sq))
                else:
                    half_x = 0
            else:
                half_x = arch_half_w
            if half_x > 4:
                draw.line(
                    [(cx - half_x + 4, y), (cx + half_x - 4, y)],
                    fill=c
                )

        # Fire tongues — brighter streaks rising from the bottom
        for offset in [-80, -30, 25, 70]:
            tongue_x = cx + offset
            tongue_base = arch_bottom - 10
            tongue_h = 120 + (abs(offset) % 40)
            for ty in range(tongue_base, tongue_base - tongue_h, -1):
                tt = (tongue_base - ty) / tongue_h
                tc = lerp_color(FIRE_HOT, FIRE_BRIGHT, tt)
                w = max(1, int(18 * (1 - tt)))
                draw.line(
                    [(tongue_x - w, ty), (tongue_x + w, ty)],
                    fill=tc
                )
    else:
        # Idle: dark interior with dim ember bed at bottom
        # Fill the rectangular part
        draw.rectangle(
            [cx - arch_half_w + 3,
             arch_top + arch_half_w,
             cx + arch_half_w - 3,
             arch_bottom - 3],
            fill=MOUTH_BG
        )
        # Fill the arch curve part
        draw.pieslice(
            [cx - arch_half_w + 3, arch_top + 3,
             cx + arch_half_w - 3, arch_top + arch_half_w * 2 - 3],
            start=180, end=360, fill=MOUTH_BG
        )

        # Dim ember bed at the bottom
        ember_h = 40
        for y in range(arch_bottom - ember_h, arch_bottom - 3):
            t = (y - (arch_bottom - ember_h)) / ember_h
            c = lerp_color(MOUTH_BG, EMBER_WARM, t * 0.6)
            draw.line(
                [(cx - arch_half_w + 6, y),
                 (cx + arch_half_w - 6, y)],
                fill=c
            )
        # A few dim ember dots
        for ex, ey in [(cx - 60, arch_bottom - 15),
                       (cx + 40, arch_bottom - 12),
                       (cx - 20, arch_bottom - 20),
                       (cx + 80, arch_bottom - 18),
                       (cx, arch_bottom - 10)]:
            draw_filled_circle(draw, ex, ey, 6, EMBER_DIM)

    # Arch outline on top of everything
    draw_arch_outline(draw, cx, arch_top, arch_bottom,
                      arch_half_w, EDGE_DARK, width=4)

    # Bolts along the frame
    bolt_positions = [
        (cx - arch_half_w - frame_w + 10, arch_bottom + 10),
        (cx + arch_half_w + frame_w - 10, arch_bottom + 10),
        (cx - arch_half_w - frame_w + 10,
         arch_top + arch_half_w + 40),
        (cx + arch_half_w + frame_w - 10,
         arch_top + arch_half_w + 40),
        (cx - arch_half_w - frame_w + 10,
         (arch_bottom + arch_top + arch_half_w) // 2 + 40),
        (cx + arch_half_w + frame_w - 10,
         (arch_bottom + arch_top + arch_half_w) // 2 + 40),
    ]
    for bx, by in bolt_positions:
        draw_filled_circle(draw, bx, by, 10, ARMOR_LIGHT,
                           outline=EDGE_DARK)
        draw_filled_circle(draw, bx, by, 5, RIVET_COLOR)

    add_corner_bolts(draw)

    # Active: fire glow spilling out
    if active:
        add_radial_glow(img, cx, int(S * 0.55),
                        int(S * 0.4), AMBER_GLOW,
                        intensity=0.25)
        add_radial_glow(img, cx, arch_bottom - 20,
                        int(S * 0.3), FIRE_BRIGHT,
                        intensity=0.15)

    return img


# ---------------------------------------------------------------------------
#  SOUTH (BACK) — exhaust vent with heat signature
# ---------------------------------------------------------------------------

def make_south(active=False):
    """Back face: hex armor with large exhaust vent panel."""
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Large exhaust vent panel
    vent_w = int(S * 0.50)
    vent_h = int(S * 0.44)
    vx1 = cx - vent_w // 2
    vy1 = cy - vent_h // 2
    vx2 = cx + vent_w // 2
    vy2 = cy + vent_h // 2

    draw.rectangle([vx1, vy1, vx2, vy2], fill=ARMOR_MID)
    add_bevel_border(draw, vx1, vy1, vx2, vy2,
                     EDGE_DARK, ARMOR_LIGHT, width=5)

    # Horizontal vent slats (wide, like industrial exhaust)
    slat_h = 12
    slat_gap = 20
    y = vy1 + 20
    while y + slat_h < vy2 - 14:
        draw.rectangle(
            [vx1 + 18, y, vx2 - 18, y + slat_h],
            fill=(15, 17, 22, 255)
        )
        # Highlight on top of slat
        draw.line(
            [(vx1 + 18, y + slat_h + 1),
             (vx2 - 18, y + slat_h + 1)],
            fill=ARMOR_LIGHT, width=1
        )
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
    if active:
        add_glowing_seam(
            img, (m, cy), (vx1 - 10, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
        add_glowing_seam(
            img, (vx2 + 10, cy), (S - m, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=8, intensity=0.15
        )
    else:
        draw.line([(m, cy), (vx1 - 10, cy)],
                  fill=SEAM_COLOR, width=2)
        draw.line([(vx2 + 10, cy), (S - m, cy)],
                  fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    if active:
        add_radial_glow(img, cx, cy, int(vent_w * 0.35),
                        ORANGE_GLOW, intensity=0.12)

    return img


# ---------------------------------------------------------------------------
#  EAST / WEST (SIDES) — heat pipes running vertically
# ---------------------------------------------------------------------------

def make_side(active=False):
    """Side face: hex armor with two thick vertical heat pipes.

    The pipes run top-to-bottom and glow orange when active,
    reinforcing the furnace / industrial heat aesthetic.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Two vertical heat pipes
    pipe_w = 50
    pipe_gap = int(S * 0.26)
    pipe_top = 50
    pipe_bottom = S - 50

    for pipe_cx in [cx - pipe_gap // 2, cx + pipe_gap // 2]:
        px1 = pipe_cx - pipe_w // 2
        px2 = pipe_cx + pipe_w // 2

        if active:
            # Glowing hot pipe — gradient from center bright
            for x in range(px1, px2 + 1):
                t = abs(x - pipe_cx) / (pipe_w // 2)
                c = lerp_color(PIPE_GLOWING, PIPE_HOT, t)
                draw.line([(x, pipe_top), (x, pipe_bottom)],
                          fill=c)
        else:
            # Cold pipe — metallic
            for x in range(px1, px2 + 1):
                t = abs(x - pipe_cx) / (pipe_w // 2)
                c = lerp_color(PIPE_LIGHT, PIPE_DARK, t)
                draw.line([(x, pipe_top), (x, pipe_bottom)],
                          fill=c)

        # Pipe edge lines
        draw.line([(px1, pipe_top), (px1, pipe_bottom)],
                  fill=EDGE_DARK, width=2)
        draw.line([(px2, pipe_top), (px2, pipe_bottom)],
                  fill=EDGE_DARK, width=2)

        # Pipe clamp brackets (horizontal bands)
        clamp_color = ARMOR_LIGHT if not active else (90, 75, 55, 255)
        for clamp_y in range(pipe_top + 80, pipe_bottom - 60, 180):
            clamp_h = 18
            draw.rectangle(
                [px1 - 12, clamp_y, px2 + 12, clamp_y + clamp_h],
                fill=clamp_color, outline=EDGE_DARK
            )
            # Clamp bolts
            draw_filled_circle(draw, px1 - 6, clamp_y + clamp_h // 2,
                               5, RIVET_COLOR)
            draw_filled_circle(draw, px2 + 6, clamp_y + clamp_h // 2,
                               5, RIVET_COLOR)

    # Horizontal seam between pipes
    m = 30
    if active:
        add_glowing_seam(
            img, (m, cy), (cx - pipe_gap // 2 - pipe_w // 2 - 16, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=6, intensity=0.12
        )
        add_glowing_seam(
            img, (cx + pipe_gap // 2 + pipe_w // 2 + 16, cy),
            (S - m, cy),
            SEAM_COLOR, AMBER_GLOW,
            seam_width=2, glow_width=6, intensity=0.12
        )
    else:
        draw.line(
            [(m, cy),
             (cx - pipe_gap // 2 - pipe_w // 2 - 16, cy)],
            fill=SEAM_COLOR, width=2
        )
        draw.line(
            [(cx + pipe_gap // 2 + pipe_w // 2 + 16, cy),
             (S - m, cy)],
            fill=SEAM_COLOR, width=2
        )

    add_corner_bolts(draw)

    if active:
        # Warm glow along each pipe
        for pipe_cx in [cx - pipe_gap // 2, cx + pipe_gap // 2]:
            add_radial_glow(img, pipe_cx, cy,
                            pipe_w + 20, AMBER_GLOW,
                            intensity=0.10)

    return img


# ---------------------------------------------------------------------------
#  UP (TOP) — chimney / smokestack opening
# ---------------------------------------------------------------------------

def make_top(active=False):
    """Top face: circular chimney opening with thick armored rim.

    A large round smokestack opening dominates the top, with
    heavy bolt-studded rim. Glows from within when active.
    """
    img = make_hex_armor_base()
    draw = ImageDraw.Draw(img)

    cx, cy = S // 2, S // 2

    # Chimney outer rim
    rim_outer = int(S * 0.36)
    rim_inner = int(S * 0.28)

    # Thick metallic rim with gradient
    for r in range(rim_outer, rim_inner, -1):
        t = (rim_outer - r) / (rim_outer - rim_inner)
        c = lerp_color(ARMOR_LIGHT, ARMOR_MID, t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=c)

    # Rim edges
    draw.ellipse(
        [cx - rim_outer, cy - rim_outer,
         cx + rim_outer, cy + rim_outer],
        outline=EDGE_DARK
    )
    draw.ellipse(
        [cx - rim_inner, cy - rim_inner,
         cx + rim_inner, cy + rim_inner],
        outline=EDGE_DARK
    )

    # Dark chimney interior
    if active:
        # Warm glow from fire below
        for r in range(rim_inner - 2, 0, -1):
            t = r / rim_inner
            c = lerp_color(FIRE_MID, FIRE_DEEP, t)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                         outline=c)
        # Bright core
        core_r = int(rim_inner * 0.3)
        draw_filled_circle(draw, cx, cy, core_r, FIRE_DARK)
    else:
        draw_filled_circle(draw, cx, cy, rim_inner - 2,
                           MOUTH_BG)

    # Bolts around the rim (evenly spaced in a circle)
    bolt_count = 12
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
    if active:
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
            add_glowing_seam(
                img, (sx, sy), (ex, ey),
                SEAM_COLOR, AMBER_GLOW,
                seam_width=2, glow_width=6, intensity=0.10
            )
    else:
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
            draw.line([(sx, sy), (ex, ey)],
                      fill=SEAM_COLOR, width=2)

    add_corner_bolts(draw)

    if active:
        add_radial_glow(img, cx, cy, rim_inner + 30,
                        ORANGE_GLOW, intensity=0.18)

    return img


# ---------------------------------------------------------------------------
#  DOWN (BOTTOM) — reinforced base plate
# ---------------------------------------------------------------------------

def make_bottom(active=False):
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
    textures = {}

    for active in (False, True):
        state = "active" if active else "idle"

        textures[f"auto_smelter_{state}_north"] = make_north(
            active=active
        )
        textures[f"auto_smelter_{state}_south"] = make_south(
            active=active
        )
        textures[f"auto_smelter_{state}_east"] = make_side(
            active=active
        )
        textures[f"auto_smelter_{state}_west"] = make_side(
            active=active
        )
        textures[f"auto_smelter_{state}_up"] = make_top(
            active=active
        )
        textures[f"auto_smelter_{state}_down"] = make_bottom(
            active=active
        )

    save_textures(textures)


if __name__ == "__main__":
    main()
