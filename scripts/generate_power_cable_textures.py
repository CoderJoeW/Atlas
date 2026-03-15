#!/usr/bin/env python3
"""Generate directional arrow textures for power cables.

Creates 12 textures (16x16 pixel art):
- power_cable_front.png: Gold connector circle on dark gray
- power_cable_back.png: Darker inset square on dark gray
- power_cable_side_{up,down,left,right}.png: Gold arrow pointing in direction
- power_cable_cap_{up,down,left,right}.png: Gold arrow for top/bottom faces
"""

import os
from PIL import Image, ImageDraw

OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources", "atlas", "resourcepack",
    "assets", "minecraft", "textures", "block", "custom"
)

# Colors
DARK_GRAY = (58, 58, 58, 255)       # #3a3a3a - base
DARKER_GRAY = (40, 40, 40, 255)     # #282828 - back inset
GOLD = (255, 215, 0, 255)           # #FFD700 - arrows/connector
GOLD_DIM = (200, 168, 0, 255)       # dimmer gold for border accents
OUTLINE = (30, 30, 30, 255)         # near-black outline


def new_img():
    return Image.new("RGBA", (16, 16), DARK_GRAY)


def make_front():
    """Front face: gold connector circle centered."""
    img = new_img()
    draw = ImageDraw.Draw(img)
    # Outer circle (gold ring)
    draw.ellipse([5, 5, 10, 10], fill=GOLD, outline=OUTLINE)
    # Inner dark center (connector hole)
    draw.rectangle([7, 7, 8, 8], fill=DARKER_GRAY)
    return img


def make_back():
    """Back face: darker inset square."""
    img = new_img()
    draw = ImageDraw.Draw(img)
    # Inset square
    draw.rectangle([5, 5, 10, 10], fill=DARKER_GRAY, outline=OUTLINE)
    return img


def make_side_up():
    """Side face with gold arrow pointing UP and a gold border strip at top edge."""
    img = new_img()
    draw = ImageDraw.Draw(img)

    # Gold border strip at top (the edge toward the front face)
    draw.rectangle([0, 0, 15, 1], fill=GOLD_DIM)

    # Arrow/chevron pointing up, centered horizontally
    # Tip of arrow at y=3
    # Row by row (symmetric chevron)
    arrow_pixels = [
        (7, 3), (8, 3),                          # tip
        (6, 4), (7, 4), (8, 4), (9, 4),          # row 2
        (5, 5), (6, 5), (9, 5), (10, 5),         # row 3 (hollow)
        (4, 6), (5, 6), (10, 6), (11, 6),        # row 4 (hollow)
        # Shaft below chevron
        (7, 7), (8, 7),
        (7, 8), (8, 8),
        (7, 9), (8, 9),
        (7, 10), (8, 10),
        (7, 11), (8, 11),
        (7, 12), (8, 12),
    ]
    for x, y in arrow_pixels:
        img.putpixel((x, y), GOLD)

    return img


def make_cap_up():
    """Cap face (top/bottom) with gold arrow pointing UP. Slightly different border style."""
    img = new_img()
    draw = ImageDraw.Draw(img)

    # Subtle corner accents instead of full border strip
    draw.rectangle([0, 0, 2, 0], fill=GOLD_DIM)
    draw.rectangle([13, 0, 15, 0], fill=GOLD_DIM)

    # Arrow/chevron pointing up - same design as side but slightly smaller shaft
    arrow_pixels = [
        (7, 3), (8, 3),                          # tip
        (6, 4), (7, 4), (8, 4), (9, 4),          # row 2
        (5, 5), (6, 5), (9, 5), (10, 5),         # row 3 (hollow)
        (4, 6), (5, 6), (10, 6), (11, 6),        # row 4 (hollow)
        # Shaft
        (7, 7), (8, 7),
        (7, 8), (8, 8),
        (7, 9), (8, 9),
        (7, 10), (8, 10),
        (7, 11), (8, 11),
    ]
    for x, y in arrow_pixels:
        img.putpixel((x, y), GOLD)

    return img


def rotate_variants(base_img, prefix):
    """Generate up/down/left/right variants by rotating the base (which points up)."""
    imgs = {}
    imgs[f"{prefix}_up"] = base_img.copy()
    imgs[f"{prefix}_down"] = base_img.rotate(180)
    imgs[f"{prefix}_left"] = base_img.rotate(90)    # 90 CCW
    imgs[f"{prefix}_right"] = base_img.rotate(-90)   # 90 CW (= 270 CCW)
    return imgs


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    textures = {}

    # Front and back
    textures["power_cable_front"] = make_front()
    textures["power_cable_back"] = make_back()

    # Side variants (rotated from base pointing up)
    side_base = make_side_up()
    textures.update(rotate_variants(side_base, "power_cable_side"))

    # Cap variants (rotated from base pointing up)
    cap_base = make_cap_up()
    textures.update(rotate_variants(cap_base, "power_cable_cap"))

    # Save all
    for name, img in textures.items():
        path = os.path.join(OUTPUT_DIR, f"{name}.png")
        img.save(path)
        print(f"  Created {name}.png")

    print(f"\nGenerated {len(textures)} textures in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
