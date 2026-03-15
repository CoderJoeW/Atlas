#!/usr/bin/env python3
"""Generate sci-fi fluid pump and pipe textures at 32x32.

Creates 27 textures:
  Pump (9): top, bottom, side × (base, active-water, active-lava)
  Pipe (18): front, back × (base, water, lava)
             side_{up,down,left,right} × (base, water, lava)
"""

import os
import math
from PIL import Image, ImageDraw

OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources", "atlas", "resourcepack",
    "assets", "minecraft", "textures", "block", "custom"
)

S = 32  # texture size

# ---------- Color Palette ----------
DARK_STEEL = (61, 68, 80, 255)        # #3D4450
MID_STEEL = (85, 95, 110, 255)
LIGHT_STEEL = (107, 114, 128, 255)    # #6B7280
RIVET = (139, 149, 165, 255)          # #8B95A5
EDGE_DARK = (35, 40, 50, 255)
PANEL_LINE = (45, 50, 60, 255)
ARROW_WHITE = (220, 240, 255, 255)
HOLE_DARK = (20, 24, 30, 255)

# Water colors
CYAN = (0, 212, 255, 255)             # #00D4FF
CYAN_GLOW = (0, 229, 255, 255)       # #00E5FF
CYAN_DIM = (0, 150, 200, 255)
FLUID_BLUE = (33, 150, 243, 255)      # #2196F3
FLUID_BLUE_DARK = (25, 118, 200, 255)

# Lava colors
LAVA_BRIGHT = (255, 140, 0, 255)      # bright orange
LAVA_GLOW = (255, 100, 20, 255)       # orange-red glow
LAVA_DIM = (200, 80, 10, 255)         # dim orange
FLUID_LAVA = (230, 90, 20, 255)       # lava fill
FLUID_LAVA_DARK = (180, 60, 10, 255)  # darker lava


def new_img(fill=DARK_STEEL):
    return Image.new("RGBA", (S, S), fill)


def add_border(draw, color=EDGE_DARK, width=1):
    for i in range(width):
        draw.rectangle([i, i, S - 1 - i, S - 1 - i], outline=color)


def add_rivets(img, positions):
    for x, y in positions:
        img.putpixel((x, y), RIVET)


def add_corner_rivets(img):
    add_rivets(img, [(3, 3), (28, 3), (3, 28), (28, 28)])


def add_panel_lines_horizontal(draw, ys):
    for y in ys:
        draw.line([(2, y), (S - 3, y)], fill=PANEL_LINE)


def add_glow_ring(img, cx, cy, r_inner, r_outer, glow_color):
    for x in range(S):
        for y in range(S):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if r_inner <= d <= r_outer:
                t = 1.0 - (d - r_inner) / (r_outer - r_inner)
                a = int(180 * t) / 255.0
                base = img.getpixel((x, y))
                nr = int(base[0] * (1 - a) + glow_color[0] * a)
                ng = int(base[1] * (1 - a) + glow_color[1] * a)
                nb = int(base[2] * (1 - a) + glow_color[2] * a)
                img.putpixel((x, y), (nr, ng, nb, 255))


def add_glow_to_edges(img, glow_color, intensity=0.35):
    for x in range(S):
        for y in range(S):
            dist_edge = min(x, y, S - 1 - x, S - 1 - y)
            if dist_edge < 3:
                t = intensity * (1.0 - dist_edge / 3.0)
                base = img.getpixel((x, y))
                nr = int(base[0] * (1 - t) + glow_color[0] * t)
                ng = int(base[1] * (1 - t) + glow_color[1] * t)
                nb = int(base[2] * (1 - t) + glow_color[2] * t)
                img.putpixel((x, y), (nr, ng, nb, 255))


def rotate_variants(base_img, prefix):
    """Generate up/down/left/right variants by rotating the base (which points up)."""
    return {
        f"{prefix}_up": base_img.copy(),
        f"{prefix}_down": base_img.rotate(180),
        f"{prefix}_left": base_img.rotate(90),     # 90 CCW
        f"{prefix}_right": base_img.rotate(-90),    # 90 CW
    }


# =====================================================================
#  PUMP TEXTURES
# =====================================================================

def make_pump_top():
    """Top: flat tech plate with pressure gauge, bolt pattern, and panel seams."""
    img = new_img()
    draw = ImageDraw.Draw(img)
    add_border(draw)

    # Panel seam cross dividing into quadrants
    draw.line([(15, 2), (15, 29)], fill=PANEL_LINE)
    draw.line([(16, 2), (16, 29)], fill=PANEL_LINE)
    draw.line([(2, 15), (29, 15)], fill=PANEL_LINE)
    draw.line([(2, 16), (29, 16)], fill=PANEL_LINE)

    # Pressure gauge (small circle, upper-left quadrant)
    draw.ellipse([6, 6, 12, 12], fill=MID_STEEL, outline=LIGHT_STEEL)
    draw.ellipse([8, 8, 10, 10], fill=EDGE_DARK)
    # Gauge needle
    img.putpixel((9, 7), RIVET)

    # Small vent grate (upper-right quadrant)
    for vy in [7, 9, 11]:
        draw.line([(19, vy), (25, vy)], fill=EDGE_DARK)

    # ID plate (lower-left quadrant)
    draw.rectangle([5, 19, 12, 25], fill=MID_STEEL, outline=PANEL_LINE)

    # Exhaust port (lower-right quadrant)
    draw.ellipse([19, 19, 26, 26], fill=EDGE_DARK, outline=PANEL_LINE)
    draw.ellipse([21, 21, 24, 24], fill=HOLE_DARK)

    # Corner bolts
    for bx, by in [(3, 3), (28, 3), (3, 28), (28, 28)]:
        draw.rectangle([bx, by, bx + 1, by + 1], fill=RIVET)

    return img


def make_pump_bottom():
    """Bottom: heavy base plate with ventilation grille and mounting feet."""
    img = new_img()
    draw = ImageDraw.Draw(img)
    add_border(draw)

    # Raised base plate
    draw.rectangle([4, 4, 27, 27], fill=MID_STEEL, outline=PANEL_LINE)

    # Central ventilation grille (wide horizontal slots)
    for vy in [8, 11, 14, 17, 20, 23]:
        draw.line([(7, vy), (24, vy)], fill=EDGE_DARK)
        draw.line([(7, vy + 1), (24, vy + 1)], fill=HOLE_DARK)

    # Mounting feet (4 corner squares)
    for fx, fy in [(5, 5), (23, 5), (5, 23), (23, 23)]:
        draw.rectangle([fx, fy, fx + 3, fy + 3], fill=LIGHT_STEEL, outline=PANEL_LINE)
        draw.rectangle([fx + 1, fy + 1, fx + 2, fy + 2], fill=RIVET)

    return img


def make_pump_side():
    """Side: pump housing with circular mechanism, pressure line, and detail."""
    img = new_img()
    draw = ImageDraw.Draw(img)
    add_border(draw)

    # Upper panel area
    draw.rectangle([2, 2, 29, 7], fill=MID_STEEL, outline=PANEL_LINE)
    # Small indicator window
    draw.rectangle([4, 4, 7, 6], fill=EDGE_DARK)

    # Pump housing — large circular mechanism in center
    draw.ellipse([8, 9, 23, 24], fill=MID_STEEL, outline=LIGHT_STEEL)
    draw.ellipse([11, 12, 20, 21], fill=DARK_STEEL, outline=PANEL_LINE)
    draw.ellipse([14, 15, 17, 18], fill=EDGE_DARK, outline=PANEL_LINE)
    # Center bolt
    img.putpixel((15, 16), RIVET)
    img.putpixel((16, 16), RIVET)
    img.putpixel((15, 17), RIVET)
    img.putpixel((16, 17), RIVET)

    # Pressure line running from housing to upper panel
    draw.line([(15, 8), (15, 9)], fill=LIGHT_STEEL)
    draw.line([(16, 8), (16, 9)], fill=LIGHT_STEEL)

    # Lower base strip
    draw.rectangle([2, 25, 29, 29], fill=MID_STEEL, outline=PANEL_LINE)

    # Pipe connectors on left and right edges
    draw.rectangle([2, 13, 5, 19], fill=LIGHT_STEEL, outline=PANEL_LINE)
    draw.rectangle([3, 15, 4, 17], fill=HOLE_DARK)
    draw.rectangle([26, 13, 29, 19], fill=LIGHT_STEEL, outline=PANEL_LINE)
    draw.rectangle([27, 15, 28, 17], fill=HOLE_DARK)

    # Rivets along top panel
    for rx in [10, 15, 20, 25]:
        img.putpixel((rx, 3), RIVET)

    # Rivets along bottom strip
    for rx in [6, 11, 20, 25]:
        img.putpixel((rx, 27), RIVET)

    return img


def make_pump_top_active(glow_color, accent_color):
    """Active pump top: pressure gauge and exhaust glow."""
    img = make_pump_top()
    draw = ImageDraw.Draw(img)
    # Gauge glows
    draw.ellipse([8, 8, 10, 10], fill=accent_color)
    # Exhaust port glows
    draw.ellipse([21, 21, 24, 24], fill=accent_color)
    add_glow_to_edges(img, glow_color, intensity=0.15)
    return img


def make_pump_bottom_active(dim_color, glow_color):
    """Active pump bottom: vents glow with fluid color."""
    img = make_pump_bottom()
    draw = ImageDraw.Draw(img)
    # Vent slots glow
    for vy in [8, 11, 14, 17, 20, 23]:
        draw.line([(7, vy + 1), (24, vy + 1)], fill=dim_color)
    add_glow_to_edges(img, glow_color, intensity=0.2)
    return img


def make_pump_side_active(dim_color, accent_color, glow_color):
    """Active pump side: pump housing and connectors glow."""
    img = make_pump_side()
    draw = ImageDraw.Draw(img)
    # Pump center glows
    draw.ellipse([14, 15, 17, 18], fill=accent_color, outline=dim_color)
    # Indicator window glows
    draw.rectangle([4, 4, 7, 6], fill=accent_color)
    # Pipe connector interiors glow
    draw.rectangle([3, 15, 4, 17], fill=dim_color)
    draw.rectangle([27, 15, 28, 17], fill=dim_color)
    # Pressure line glows
    draw.line([(15, 8), (15, 9)], fill=accent_color)
    draw.line([(16, 8), (16, 9)], fill=accent_color)
    add_glow_to_edges(img, glow_color, intensity=0.15)
    return img


# =====================================================================
#  PIPE TEXTURES
# =====================================================================

def make_pipe_front():
    img = new_img()
    draw = ImageDraw.Draw(img)
    add_border(draw)
    draw.ellipse([8, 8, 23, 23], fill=MID_STEEL, outline=LIGHT_STEEL)
    draw.ellipse([11, 11, 20, 20], fill=HOLE_DARK, outline=EDGE_DARK)
    for pos in [(15, 7), (15, 24), (7, 15), (24, 15)]:
        img.putpixel(pos, RIVET)
    add_corner_rivets(img)
    return img


def make_pipe_back():
    img = new_img()
    draw = ImageDraw.Draw(img)
    add_border(draw)
    draw.ellipse([8, 8, 23, 23], fill=MID_STEEL, outline=LIGHT_STEEL)
    draw.ellipse([13, 13, 18, 18], fill=LIGHT_STEEL, outline=PANEL_LINE)
    draw.rectangle([15, 15, 16, 16], fill=RIVET)
    add_corner_rivets(img)
    return img


# Arrow pointing UP — base for rotation
ARROW_PIXELS = [
    (15, 6), (16, 6),
    (14, 7), (15, 7), (16, 7), (17, 7),
    (13, 8), (14, 8), (17, 8), (18, 8),
    (12, 9), (13, 9), (18, 9), (19, 9),
    (11, 10), (12, 10), (19, 10), (20, 10),
    (15, 11), (16, 11), (15, 12), (16, 12),
    (15, 13), (16, 13), (15, 14), (16, 14),
    (15, 15), (16, 15), (15, 16), (16, 16),
    (15, 17), (16, 17), (15, 18), (16, 18),
    (15, 19), (16, 19), (15, 20), (16, 20),
    (15, 21), (16, 21), (15, 22), (16, 22),
    (15, 23), (16, 23), (15, 24), (16, 24),
]

SIDE_RIVET_POSITIONS = [(3, 5), (3, 15), (3, 26), (28, 5), (28, 15), (28, 26)]


def make_pipe_side_up():
    """Side face with arrow pointing UP (base for rotation to other directions)."""
    img = new_img()
    draw = ImageDraw.Draw(img)
    add_border(draw)
    add_panel_lines_horizontal(draw, [8, 16, 24])
    for x, y in ARROW_PIXELS:
        img.putpixel((x, y), ARROW_WHITE)
    add_rivets(img, SIDE_RIVET_POSITIONS)
    return img


def make_pipe_side_up_filled(fluid_color, fluid_dark):
    """Filled side face with arrow pointing UP (base for rotation)."""
    img = new_img()
    draw = ImageDraw.Draw(img)
    add_border(draw)
    # Fluid fill band behind arrow
    draw.rectangle([4, 10, 27, 21], fill=fluid_dark)
    draw.rectangle([5, 12, 26, 19], fill=fluid_color)
    add_panel_lines_horizontal(draw, [8, 24])
    for x, y in ARROW_PIXELS:
        img.putpixel((x, y), ARROW_WHITE)
    add_rivets(img, SIDE_RIVET_POSITIONS)
    return img


def make_pipe_front_filled(fluid_color, fluid_dark):
    img = make_pipe_front()
    draw = ImageDraw.Draw(img)
    draw.ellipse([11, 11, 20, 20], fill=fluid_color, outline=EDGE_DARK)
    draw.ellipse([13, 13, 17, 17], fill=fluid_dark)
    return img


def make_pipe_back_filled(fluid_color, accent_color):
    img = make_pipe_back()
    draw = ImageDraw.Draw(img)
    draw.ellipse([10, 10, 21, 21], outline=fluid_color)
    draw.rectangle([15, 15, 16, 16], fill=accent_color)
    return img


# =====================================================================
#  MAIN
# =====================================================================

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    textures = {}

    # --- Pump textures ---
    textures["fluid_pump_top"] = make_pump_top()
    textures["fluid_pump_bottom"] = make_pump_bottom()
    textures["fluid_pump_side"] = make_pump_side()

    textures["fluid_pump_top_active"] = make_pump_top_active(CYAN_GLOW, CYAN)
    textures["fluid_pump_bottom_active"] = make_pump_bottom_active(CYAN_DIM, CYAN_GLOW)
    textures["fluid_pump_side_active"] = make_pump_side_active(CYAN_DIM, CYAN, CYAN_GLOW)

    textures["fluid_pump_top_active_lava"] = make_pump_top_active(LAVA_GLOW, LAVA_BRIGHT)
    textures["fluid_pump_bottom_active_lava"] = make_pump_bottom_active(LAVA_DIM, LAVA_GLOW)
    textures["fluid_pump_side_active_lava"] = make_pump_side_active(LAVA_DIM, LAVA_BRIGHT, LAVA_GLOW)

    # --- Pipe front/back ---
    textures["fluid_pipe_front"] = make_pipe_front()
    textures["fluid_pipe_back"] = make_pipe_back()

    textures["fluid_pipe_front_filled"] = make_pipe_front_filled(FLUID_BLUE, FLUID_BLUE_DARK)
    textures["fluid_pipe_back_filled"] = make_pipe_back_filled(FLUID_BLUE, CYAN)

    textures["fluid_pipe_front_filled_lava"] = make_pipe_front_filled(FLUID_LAVA, FLUID_LAVA_DARK)
    textures["fluid_pipe_back_filled_lava"] = make_pipe_back_filled(FLUID_LAVA, LAVA_BRIGHT)

    # --- Pipe side variants (rotated from base pointing up) ---
    side_base = make_pipe_side_up()
    textures.update(rotate_variants(side_base, "fluid_pipe_side"))

    side_water = make_pipe_side_up_filled(FLUID_BLUE, FLUID_BLUE_DARK)
    textures.update(rotate_variants(side_water, "fluid_pipe_side_filled"))

    side_lava = make_pipe_side_up_filled(FLUID_LAVA, FLUID_LAVA_DARK)
    textures.update(rotate_variants(side_lava, "fluid_pipe_side_filled_lava"))

    # Save all
    for name, img in textures.items():
        path = os.path.join(OUTPUT_DIR, f"{name}.png")
        img.save(path)
        print(f"  Created {name}.png ({img.size[0]}x{img.size[1]})")

    print(f"\nGenerated {len(textures)} textures in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
