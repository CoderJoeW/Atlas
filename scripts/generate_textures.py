#!/usr/bin/env python3
"""Generate power-level texture variants.

Creates:
- SmallSolarPanel indicator variants: _low (red), _medium (yellow), _full (green)
- PowerCable powered variants: brightened gold elements for all cable textures
"""

import os
from PIL import Image

OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources", "atlas", "resourcepack",
    "assets", "minecraft", "textures", "block", "custom"
)

# Indicator light colors (2x2 block in bottom-right corner)
INDICATOR_RED = (255, 50, 50, 255)
INDICATOR_YELLOW = (255, 220, 50, 255)
INDICATOR_GREEN = (50, 255, 50, 255)

# Gold color range for detecting cable gold pixels
GOLD_RGB = (255, 215, 0)
GOLD_DIM_RGB = (200, 168, 0)

# Bright powered green
POWERED_GOLD = (50, 255, 50, 255)
POWERED_GOLD_DIM = (30, 200, 30, 255)


def add_indicator(base_img, color):
    """Add a 2x2 indicator light in the bottom-right corner of the texture."""
    img = base_img.copy()
    # Place 2x2 indicator at pixels (13,13) to (14,14)
    for x in range(13, 15):
        for y in range(13, 15):
            img.putpixel((x, y), color)
    return img


def is_gold_pixel(pixel):
    """Check if a pixel is a gold/gold_dim color (used in cable textures)."""
    r, g, b = pixel[0], pixel[1], pixel[2]
    # Match gold (#FFD700) and dim gold (#C8A800) with some tolerance
    if r >= 180 and g >= 150 and b <= 50:
        return True
    return False


def brighten_gold(base_img):
    """Create a powered variant by brightening gold pixels to a glowing yellow."""
    img = base_img.copy()
    for x in range(img.width):
        for y in range(img.height):
            pixel = img.getpixel((x, y))
            if is_gold_pixel(pixel):
                # Brighten: if it was dim gold, make it powered dim; if full gold, make it powered
                if pixel[1] < 200:  # dim gold
                    img.putpixel((x, y), POWERED_GOLD_DIM)
                else:
                    img.putpixel((x, y), POWERED_GOLD)
    return img


def generate_solar_variants():
    """Generate SmallSolarPanel indicator variants."""
    base_path = os.path.join(OUTPUT_DIR, "small_solar_panel.png")
    if not os.path.exists(base_path):
        print(f"  WARNING: {base_path} not found, skipping solar variants")
        return

    base = Image.open(base_path).convert("RGBA")

    variants = {
        "small_solar_panel_low": INDICATOR_RED,
        "small_solar_panel_medium": INDICATOR_YELLOW,
        "small_solar_panel_full": INDICATOR_GREEN,
    }

    for name, color in variants.items():
        img = add_indicator(base, color)
        path = os.path.join(OUTPUT_DIR, f"{name}.png")
        img.save(path)
        print(f"  Created {name}.png")


def generate_powered_cable_variants():
    """Generate powered cable texture variants by brightening gold elements."""
    cable_textures = [
        "power_cable_front",
        "power_cable_back",
        "power_cable_side_up",
        "power_cable_side_down",
        "power_cable_side_left",
        "power_cable_side_right",
        "power_cable_cap_up",
        "power_cable_cap_down",
        "power_cable_cap_left",
        "power_cable_cap_right",
    ]

    for name in cable_textures:
        base_path = os.path.join(OUTPUT_DIR, f"{name}.png")
        if not os.path.exists(base_path):
            print(f"  WARNING: {base_path} not found, skipping")
            continue

        base = Image.open(base_path).convert("RGBA")
        powered = brighten_gold(base)
        out_name = f"{name}_powered"
        path = os.path.join(OUTPUT_DIR, f"{out_name}.png")
        powered.save(path)
        print(f"  Created {out_name}.png")


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    print("Generating SmallSolarPanel indicator variants...")
    generate_solar_variants()

    print("\nGenerating powered cable variants...")
    generate_powered_cable_variants()

    print("\nDone!")


if __name__ == "__main__":
    main()
