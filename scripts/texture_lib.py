#!/usr/bin/env python3
"""Shared texture generation utilities for Atlas block textures.

Provides common drawing primitives, color blending, and pattern generators
that work at any resolution (16 through 1024).
"""

import os
import math
from PIL import Image, ImageDraw

# Default output directory for generated textures
OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources", "atlas", "resourcepack",
    "assets", "minecraft", "textures", "block", "custom"
)


# ---------------------------------------------------------------------------
#  Image creation
# ---------------------------------------------------------------------------

def new_img(size, fill=(0, 0, 0, 255)):
    """Create a new RGBA image of the given size with a solid fill."""
    return Image.new("RGBA", (size, size), fill)


# ---------------------------------------------------------------------------
#  Color utilities
# ---------------------------------------------------------------------------

def lerp_color(c1, c2, t):
    """Linearly interpolate between two RGBA colors. t in [0, 1]."""
    t = max(0.0, min(1.0, t))
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


def blend_over(base, overlay, alpha):
    """Blend overlay color onto base with a given alpha (0.0-1.0)."""
    a = max(0.0, min(1.0, alpha))
    return (
        int(base[0] * (1 - a) + overlay[0] * a),
        int(base[1] * (1 - a) + overlay[1] * a),
        int(base[2] * (1 - a) + overlay[2] * a),
        255,
    )


# ---------------------------------------------------------------------------
#  Basic drawing helpers
# ---------------------------------------------------------------------------

def add_border(draw, size, color=(35, 40, 50, 255), width=1):
    """Draw a rectangular border around the full image."""
    for i in range(width):
        draw.rectangle([i, i, size - 1 - i, size - 1 - i], outline=color)


def add_rivets(img, positions, color=(139, 149, 165, 255), rivet_size=1):
    """Place square rivet dots at the given (x, y) positions."""
    for x, y in positions:
        for dx in range(rivet_size):
            for dy in range(rivet_size):
                if 0 <= x + dx < img.width and 0 <= y + dy < img.height:
                    img.putpixel((x + dx, y + dy), color)


def fill_rect(img, x1, y1, x2, y2, color):
    """Fill a rectangle on the image (inclusive coords)."""
    draw = ImageDraw.Draw(img)
    draw.rectangle([x1, y1, x2, y2], fill=color)


# ---------------------------------------------------------------------------
#  Glow effects
# ---------------------------------------------------------------------------

def add_glow_ring(img, cx, cy, r_inner, r_outer, glow_color):
    """Add a radial glow ring that fades from glow_color to transparent."""
    for x in range(max(0, int(cx - r_outer - 1)), min(img.width, int(cx + r_outer + 2))):
        for y in range(max(0, int(cy - r_outer - 1)), min(img.height, int(cy + r_outer + 2))):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if r_inner <= d <= r_outer:
                t = 1.0 - (d - r_inner) / (r_outer - r_inner)
                a = (180 * t) / 255.0
                base = img.getpixel((x, y))
                img.putpixel((x, y), blend_over(base, glow_color, a))


def add_glow_to_edges(img, glow_color, intensity=0.35, depth=3):
    """Add a glow effect along the edges of the image."""
    s = img.width
    for x in range(s):
        for y in range(s):
            dist_edge = min(x, y, s - 1 - x, s - 1 - y)
            if dist_edge < depth:
                t = intensity * (1.0 - dist_edge / depth)
                base = img.getpixel((x, y))
                img.putpixel((x, y), blend_over(base, glow_color, t))


def add_radial_glow(img, cx, cy, radius, glow_color, intensity=0.6):
    """Add a soft radial glow emanating from center point."""
    for x in range(max(0, int(cx - radius)), min(img.width, int(cx + radius + 1))):
        for y in range(max(0, int(cy - radius)), min(img.height, int(cy + radius + 1))):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d <= radius:
                t = intensity * (1.0 - d / radius) ** 1.5
                base = img.getpixel((x, y))
                img.putpixel((x, y), blend_over(base, glow_color, t))


# ---------------------------------------------------------------------------
#  Hexagonal honeycomb pattern
# ---------------------------------------------------------------------------

def draw_hex_grid(img, hex_radius, line_color, fill_color=None, inset=0,
                  mask_fn=None):
    """Draw a hexagonal honeycomb grid across the image.

    Args:
        img: Target PIL image.
        hex_radius: Radius of each hexagon (center to vertex).
        line_color: Color for hex outlines.
        fill_color: Optional fill color for hex interiors (None = transparent).
        inset: Pixel inset from image edges to start the grid.
        mask_fn: Optional callable(cx, cy) -> bool. If provided, only draw
                 hexagons where mask_fn returns True.
    """
    s = img.width
    draw = ImageDraw.Draw(img)

    # Flat-top hex geometry
    hex_w = hex_radius * 2
    hex_h = hex_radius * math.sqrt(3)
    col_step = hex_w * 0.75
    row_step = hex_h

    cols = int((s - 2 * inset) / col_step) + 2
    rows = int((s - 2 * inset) / row_step) + 2

    for col in range(-1, cols + 1):
        for row in range(-1, rows + 1):
            cx = inset + col * col_step
            cy = inset + row * row_step + (col % 2) * (hex_h / 2)

            if mask_fn and not mask_fn(cx, cy):
                continue

            vertices = []
            for i in range(6):
                angle = math.radians(60 * i)
                vx = cx + hex_radius * math.cos(angle)
                vy = cy + hex_radius * math.sin(angle)
                vertices.append((vx, vy))

            if fill_color:
                draw.polygon(vertices, fill=fill_color)
            draw.polygon(vertices, outline=line_color)


def draw_hex_grid_lines_only(img, hex_radius, line_color, line_width=1,
                             inset=0, mask_fn=None):
    """Draw only the lines of a hexagonal grid (no fill), with configurable width."""
    s = img.width
    draw = ImageDraw.Draw(img)

    hex_w = hex_radius * 2
    hex_h = hex_radius * math.sqrt(3)
    col_step = hex_w * 0.75
    row_step = hex_h

    cols = int((s - 2 * inset) / col_step) + 2
    rows = int((s - 2 * inset) / row_step) + 2

    for col in range(-1, cols + 1):
        for row in range(-1, rows + 1):
            cx = inset + col * col_step
            cy = inset + row * row_step + (col % 2) * (hex_h / 2)

            if mask_fn and not mask_fn(cx, cy):
                continue

            vertices = []
            for i in range(6):
                angle = math.radians(60 * i)
                vx = cx + hex_radius * math.cos(angle)
                vy = cy + hex_radius * math.sin(angle)
                vertices.append((vx, vy))

            for i in range(6):
                draw.line([vertices[i], vertices[(i + 1) % 6]],
                          fill=line_color, width=line_width)


# ---------------------------------------------------------------------------
#  Panel and seam drawing
# ---------------------------------------------------------------------------

def add_panel_seam(draw, start, end, color, width=1):
    """Draw a panel seam line."""
    draw.line([start, end], fill=color, width=width)


def add_glowing_seam(img, start, end, seam_color, glow_color, seam_width=2,
                     glow_width=6, intensity=0.3):
    """Draw a panel seam with a glow effect around it."""
    draw = ImageDraw.Draw(img)
    # Draw glow passes (wider, semi-transparent)
    for w in range(glow_width, seam_width, -1):
        t = intensity * (1.0 - (w - seam_width) / (glow_width - seam_width))
        # We can't do per-pixel blending with draw.line easily,
        # so we draw the seam at the desired color with diminishing opacity
        gc = (*glow_color[:3], int(255 * t))
        draw.line([start, end], fill=gc, width=w)
    # Draw the solid seam on top
    draw.line([start, end], fill=seam_color, width=seam_width)


# ---------------------------------------------------------------------------
#  Output helpers
# ---------------------------------------------------------------------------

def save_textures(textures, output_dir=None):
    """Save a dict of {name: Image} to the output directory as PNGs."""
    out = output_dir or OUTPUT_DIR
    os.makedirs(out, exist_ok=True)
    for name, img in textures.items():
        path = os.path.join(out, f"{name}.png")
        img.save(path)
        print(f"  Created {name}.png ({img.size[0]}x{img.size[1]})")
    print(f"\nGenerated {len(textures)} textures in {out}")
