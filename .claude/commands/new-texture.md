# New Texture

Generate a sci-fi themed texture set for an Atlas block using the texture_lib framework.

## Arguments

$ARGUMENTS

## Instructions

You are generating production-quality textures for a Minecraft block in the Atlas plugin. All textures use a consistent sci-fi industrial art direction and are built programmatically with Python/Pillow using the shared `scripts/texture_lib.py` library.

Before writing any code, read `scripts/texture_lib.py` and at least one existing generator script in `scripts/generate_*_textures.py` to understand the established patterns and available utilities.

### Art Direction

All Atlas block textures share a unified visual language:

- **Housing**: Dark matte armored panels (base ~38,40,48 RGB) with subtle color variation
- **Surface pattern**: Hexagonal honeycomb grid across panel surfaces — compute hex cell centers, draw each cell as a polygon with dark fill and subtle outline
- **Borders**: Thick dark outer frame with inner bevel (light top-left, dark bottom-right) for a raised appearance
- **Seam lines**: Thin dark panel seams dividing faces into logical sections, optionally glowing with `add_glowing_seam()` when the block is active
- **Hardware details**: Bolts/rivets at corners, along seam lines, and around key features — drawn as small filled circles with highlight centers
- **Functional elements**: Vent grates (horizontal slats), data plates (beveled rectangles with decorative line details), status LEDs, pipe connectors, gauge displays
- **Active/energy states**: Glowing elements using `add_radial_glow()` and `add_glow_ring()` — energy colors shift to communicate state (red=low/danger, amber/orange=medium/warm, yellow=high, green=full/ready, blue/cyan=active)
- **Gauges**: Circular gauge faces with metallic frames, tick marks, and colored arcs that fill clockwise to indicate levels

### Resolution

- Default resolution is **1024x1024** for new textures (maximum detail budget)
- The resolution is set as a module-level `S` constant and all coordinates should scale relative to `S`
- Smaller resolutions (128, 256, 512) are acceptable if the block is simple or the user requests it

### File Structure

Create the texture generator as a standalone Python script:

```
scripts/generate_{block_id}_textures.py
```

The script must:
1. Import shared utilities from `texture_lib` (never duplicate them)
2. Define a block-specific color palette at the top of the file
3. Build each face as a separate function (`make_top()`, `make_side()`, `make_bottom()`, etc.)
4. Handle visual state variants by layering energy/glow effects onto a base face
5. Use `save_textures()` from texture_lib to write all PNGs to the standard output directory
6. Be runnable standalone via `python3 scripts/generate_{block_id}_textures.py`

### Required Steps

#### Step 1: Understand the Block

From the user's description and/or the block's CraftEngine YAML config, determine:

1. **Block ID** (e.g., `small_battery`, `fluid_pump`)
2. **Parent model type** — determines which faces need unique textures:
   - `cube_bottom_top`: 3 textures — top, bottom, side (side shared on N/S/E/W)
   - `cube`: 6 textures — north, south, east, west, up, down
   - `cube_all`: 1 texture — all faces identical
3. **Visual states** — what variants exist? (e.g., inactive/active, charge levels, fluid types)
4. **Which faces change per state** — typically only one or two faces change; the rest are shared
5. **Energy/theme colors** — what glow colors fit this block's purpose?

If any of this is unclear, ask before proceeding.

#### Step 2: Design the Face Layout

Plan what goes on each face. Follow these conventions:

- **Top face**: The "showcase" face — gauges, status displays, functional indicators. This is usually what the player sees when looking down at placed blocks.
- **Side faces**: Armored panels with structural details — hex grid, seam lines, bolt rows, vent grates, data plates, pipe connectors, status LEDs.
- **Bottom face**: Heavy base plate — ventilation grille, mounting feet with bolts, structural cross-seams.

For directional blocks, the **front face** (facing the player) becomes the showcase face instead of top.

#### Step 3: Build the Color Palette

Define colors at the top of the script. Group them by purpose:

```python
# Housing / armor — keep consistent across all blocks
ARMOR_DARK = (38, 40, 48, 255)         # base matte housing
ARMOR_MID = (52, 56, 66, 255)          # lighter panel areas
ARMOR_LIGHT = (70, 75, 88, 255)        # highlights / bevels
HEX_LINE = (48, 52, 62, 255)           # honeycomb grid lines
EDGE_DARK = (22, 24, 30, 255)          # darkest edges
SEAM_COLOR = (30, 33, 40, 255)         # panel seams
RIVET_COLOR = (100, 108, 125, 255)     # bolt highlights
FRAME_DARK = (28, 30, 38, 255)         # frame dark tone
FRAME_MID = (58, 62, 74, 255)          # frame mid tone
FRAME_LIGHT = (80, 86, 100, 255)       # frame highlight

# Energy colors (customize per block)
ENERGY_COLOR = (...)                    # primary active color
ENERGY_GLOW = (...)                     # brighter glow variant
```

Keep the housing/armor palette consistent across all blocks. Only the energy/accent colors should vary per block.

#### Step 4: Implement the Generator

Follow these established patterns from existing generators:

**Hex armor base**: Compute hex cell centers once at module level, then draw cells as polygons with dark fill and subtle outlines. This is the foundation for all armored faces.

```python
def _hex_vertices(cx, cy, radius):
    """Return 6 vertices for a flat-top hexagon."""
    ...

HEX_CENTERS = _compute_hex_centers(S, HEX_RADIUS)

def make_hex_armor_face():
    img = new_img(S, ARMOR_DARK)
    draw = ImageDraw.Draw(img)
    add_border(draw, S, EDGE_DARK, width=6)
    # bevel, hex cells, etc.
    ...
```

**Face functions**: Build each face as a function on top of the armor base. Add face-specific details (bolts, seams, vents, gauges) on top.

**State variants**: Generate the base face once, then create variants by adding energy/glow effects. Don't rebuild the entire face for each state.

**Key texture_lib functions**:
- `new_img(size, fill)` — create base image
- `add_border(draw, size, color, width)` — outer border
- `draw_hex_grid_lines_only(img, hex_radius, line_color, ...)` — honeycomb pattern (alternative to manual hex cell drawing)
- `add_radial_glow(img, cx, cy, radius, color, intensity)` — soft circular glow
- `add_glow_ring(img, cx, cy, r_inner, r_outer, color)` — ring-shaped glow
- `add_glowing_seam(img, start, end, seam_color, glow_color, ...)` — glowing panel seams
- `lerp_color(c1, c2, t)` — color interpolation
- `blend_over(base, overlay, alpha)` — alpha blending
- `save_textures(textures)` — save all PNGs

#### Step 5: Generate and Verify

1. Run the script: `python3 scripts/generate_{block_id}_textures.py`
2. Visually inspect every generated texture by reading the PNG files
3. Verify the texture names match what the CraftEngine YAML references
4. Check that state variants are visually distinct from each other

### Checklist

Before finishing, verify:
- [ ] Script imports from `texture_lib` (no duplicated utilities)
- [ ] Color palette uses the standard armor colors for housing
- [ ] Hex honeycomb pattern is present on all armored faces
- [ ] Beveled borders on all faces
- [ ] Bolts/rivets at structurally appropriate locations
- [ ] State variants are visually distinct (different energy colors/intensities)
- [ ] All textures referenced in the CraftEngine YAML are generated
- [ ] Script runs successfully and all PNGs are created
- [ ] Each texture has been visually inspected
- [ ] No line in the script exceeds 140 characters
