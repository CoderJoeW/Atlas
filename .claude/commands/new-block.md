# New Block Scaffold

Create a new block for the Atlas plugin. The user will describe what the block should do.

## Arguments

$ARGUMENTS

## Instructions

You are scaffolding a new block in a Minecraft plugin (Paper/Kotlin) that uses the Nexo resource pack system and a custom block framework. Follow the existing patterns exactly.

### Step 1: Gather Info

Before writing any code, determine from the user's description:

1. **Block name** (snake_case ID, e.g., `lava_generator`)
2. **Display name** (e.g., "Lava Generator")
3. **System type**: `power` or `fluid` — determines base class (`PowerBlock` or `FluidBlock`) and which registry/factory/dialog to integrate with
4. **Placement type**: `SIMPLE` (no direction), `DIRECTIONAL` (faces toward placement direction), or `DIRECTIONAL_OPPOSITE` (faces away)
5. **Visual states**: What variants does the block have? (e.g., inactive/active, charge levels, facing directions)
6. **Key properties**: maxStorage, updateIntervalTicks, canReceivePower, etc.
7. **Behavior**: What happens during powerUpdate()/fluidUpdate()?
8. **Recipe ingredients**: What materials craft it?
9. **Gradient colors** for the item name (hex pair, e.g., `#FF4500:#FF8C00`)

If any of these are unclear from the user's description, ask before proceeding.

### Step 2: Create the Block Class

Create `src/main/kotlin/com/coderjoe/atlas/{system}/block/{ClassName}.kt`

Follow the pattern based on placement type:

**SIMPLE block** (like LavaGenerator, SmallSolarPanel):
```kotlin
package com.coderjoe.atlas.{system}.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.{system}.{BaseClass}
import org.bukkit.Location

class {ClassName}(location: Location) : {BaseClass}(location, maxStorage = {N}) {

    override val canReceivePower: Boolean = {true/false}  // power blocks only
    override val updateIntervalTicks: Long = {N}L

    companion object {
        const val BLOCK_ID = "{block_id}"
        // Add variant IDs as needed, e.g.:
        // const val BLOCK_ID_ACTIVE = "{block_id}_active"

        val descriptor = BlockDescriptor(
            baseBlockId = BLOCK_ID,
            displayName = "{Display Name}",
            description = "{short description}",
            placementType = PlacementType.SIMPLE,
            directionalVariants = emptyMap(),
            allRegistrableIds = listOf(BLOCK_ID),  // include all variant IDs
            constructor = { loc, _ -> {ClassName}(loc) }
        )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = BLOCK_ID  // add state logic as needed

    override fun powerUpdate() {  // or fluidUpdate() for fluid blocks
        // Block behavior
    }
}
```

**DIRECTIONAL block** (like PowerCable, FluidPipe): Include `facing` property, `DIRECTIONAL_IDS` map for all 6 faces (NORTH, SOUTH, EAST, WEST, UP, DOWN), and use `ID_TO_FACING` reverse lookup. Constructor takes `(Location, BlockFace)`.

**DIRECTIONAL_OPPOSITE block** (like SmallDrill): Same directional pattern but placement type is `DIRECTIONAL_OPPOSITE`.

### Step 3: Add Block Definitions to atlas_blocks.yml

Append to `src/main/resources/nexo/items/atlas_blocks.yml`.

Find the next available `custom_variation` number. Power blocks use 100+ range, fluid blocks use 1-43 range. Check the file for the highest used number in the relevant range and increment.

Each variant needs its own YAML entry:
```yaml
{block_id}:
  itemname: "<gradient:{color1}:{color2}>{Display Name}"
  material: paper
  Pack:
    generate_model: true
    parent_model: block/cube          # or block/cube_all for same texture on all faces
    textures:
      north: atlas:block/{texture_name}
      south: atlas:block/{texture_name}
      east: atlas:block/{texture_name}
      west: atlas:block/{texture_name}
      up: atlas:block/{texture_top}
      down: atlas:block/{texture_bottom}
  Mechanics:
    custom_block:
      type: NOTEBLOCK
      custom_variation: {next_available_number}
      hardness: 5
      block_sounds:
        break_sound: block.metal.break
        place_sound: block.metal.place
        hit_sound: block.metal.hit
        step_sound: block.metal.step
        fall_sound: block.metal.fall
      drop:
        silktouch: false
        loots:
          - nexo_item: {base_block_id}    # ALWAYS drops the base ID, not the variant
            probability: 1.0
```

Notes:
- All variant entries (active, directional, etc.) must each have a unique `custom_variation` number
- The `drop` always references the base block ID so variants drop the right item
- Use `block/cube_all` with `all:` texture if all 6 faces are the same
- Use `block/cube` with individual face textures if faces differ
- Use `block/cube_bottom_top` with `top:`, `bottom:`, `side:` if top/bottom differ from sides

### Step 4: Add Recipe

Append to `src/main/resources/nexo/recipes/shapeless/atlas_recipes.yml`:
```yaml
{block_id}_recipe:
  result:
    nexo_item: {block_id}
    amount: 1
  ingredients:
    A:
      amount: {n}
      minecraft_type: {MATERIAL}
    B:
      amount: {n}
      minecraft_type: {MATERIAL}
```

### Step 5: Register in Atlas.kt

Add the descriptor to `src/main/kotlin/com/coderjoe/atlas/Atlas.kt`:

- **Power blocks**: Add `com.coderjoe.atlas.power.block.{ClassName}.descriptor` to the list in `powerDescriptors()`
- **Fluid blocks**: Add `com.coderjoe.atlas.fluid.block.{ClassName}.descriptor` to the list in `fluidDescriptors()`

### Step 6: Add Dialog Integration

For **power blocks**, edit `src/main/kotlin/com/coderjoe/atlas/power/PowerBlockDialog.kt`:
1. Add import for the new class
2. Add `is {ClassName} -> "{Display Name}"` case in `getBlockDisplayName()`
3. Add description case in `getBlockDescription()`:
   ```kotlin
   is {ClassName} -> Component.text("{description}")
       .color(NamedTextColor.GRAY)
   ```

For **fluid blocks**, edit `src/main/kotlin/com/coderjoe/atlas/fluid/FluidBlockDialog.kt` following the same pattern.

### Step 7: Update Tests

1. **Update `TestHelper.initPowerFactory()`** (or `initFluidFactory()`) in `src/test/kotlin/com/coderjoe/atlas/TestHelper.kt` — add the new descriptor to the registration list.

2. **Update block count assertions** in:
   - `src/test/kotlin/com/coderjoe/atlas/AtlasPluginTest.kt` — update the count in `power system initializes with N block types` (or fluid equivalent)
   - `src/test/kotlin/com/coderjoe/atlas/power/PowerBlockInitializerTest.kt` (or fluid equivalent) — update the count and comment

   The count = total number of IDs across all `allRegistrableIds` lists. Add the number of new variant IDs to the existing count.

3. **Create block test file** at `src/test/kotlin/com/coderjoe/atlas/{system}/{ClassName}Test.kt` with tests for:
   - Key properties (maxStorage, canReceivePower, etc.)
   - Visual state transitions
   - Core behavior (powerUpdate/fluidUpdate logic)
   - Descriptor properties (baseBlockId, displayName, allRegistrableIds count)
   - Edge cases (full storage, no adjacent blocks, wrong fluid type, etc.)

### Step 8: Generate Placeholder Textures

Create 128x128 PNG placeholder textures at `src/main/resources/nexo/pack/assets/atlas/textures/block/`.

Use Python with Pillow to generate dark industrial-style textures matching the project's art style:
- Base color: dark gray/charcoal (~38,38,44 RGB)
- Borders: darker frame (~24,24,28) with light bevel (~52,52,58)
- Details: rivets, panels, vents, indicator lights
- Active states: use orange/lava glow (~255,160,20) or blue/cyan (~30,144,255) depending on the block's theme
- Add subtle noise for texture (random +/-3 per channel)

Name textures following the convention: `{block_id}_{face}.png`, `{block_id}_{face}_{state}.png`

### Checklist

Before finishing, verify:
- [ ] Block class created with correct descriptor
- [ ] All variant IDs listed in `allRegistrableIds`
- [ ] YAML entries added for every variant with unique `custom_variation` numbers
- [ ] Recipe added
- [ ] Descriptor registered in Atlas.kt
- [ ] Dialog cases added
- [ ] TestHelper updated with new descriptor
- [ ] Block count assertions updated (both test files)
- [ ] Block-specific test file created
- [ ] Placeholder textures generated for all referenced texture names
