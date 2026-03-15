# New Block Scaffold

Create a new block for the Atlas plugin. The user will describe what the block should do.

## Arguments

$ARGUMENTS

## Instructions

You are scaffolding a new block in a Minecraft plugin (Paper/Kotlin) that uses the CraftEngine resource pack system and a custom block framework. Follow the existing patterns exactly.

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

### Step 3: Add CraftEngine Block Configuration

Create `src/main/resources/atlas/configuration/{block_id}.yml`.

Follow the CraftEngine configuration format. Each variant gets its own `items` section (use `items#1`, `items#2`, etc. for additional variants). The base variant uses `loot: template: default:loot_table/self`, while other variants use explicit loot pools that drop the base item.

Reference existing configuration files (e.g., `small_solar_panel.yml`, `lava_generator.yml`) for the exact format. Here's the general structure:

```yaml
items:
  atlas:{block_id}:
    material: paper
    data:
      item-name: "<!i><gradient:{color1}:{color2}>{Display Name}"
    model: minecraft:block/custom/{block_id}
    behavior:
      type: block_item
      block:
        loot:
          template: default:loot_table/self
        settings:
          hardness: 4.0
          resistance: 4.0
          is-suffocating: true
          is-redstone-conductor: false
          push-reaction: push_only
          tags: ["minecraft:mineable/pickaxe"]
          sounds:
            break: minecraft:block.metal.break
            step: minecraft:block.metal.step
            place: minecraft:block.metal.place
            hit: minecraft:block.metal.hit
            fall: minecraft:block.metal.fall
        state:
          auto-state: solid
          model:
            path: minecraft:block/custom/{block_id}
            generation:
              parent: minecraft:block/cube_bottom_top   # or block/cube, block/cube_all
              textures:
                top: minecraft:block/custom/{block_id}_top
                bottom: minecraft:block/custom/{block_id}_bottom
                side: minecraft:block/custom/{block_id}_side
```

For additional variant entries (active states, directional, etc.), add `items#1`, `items#2`, etc. sections with explicit loot that drops the base item:
```yaml
items#1:
  atlas:{block_id}_{variant}:
    # ... same structure but with:
    # loot pools that drop atlas:{block_id} (the base item)
```

### Step 4: Add Recipe

Add the recipe in the same configuration file. Append a `recipes:` section:
```yaml
recipes:
  atlas:{block_id}:
    type: shapeless
    category: misc
    unlock-on-ingredient-obtained: true
    ingredients:
      - minecraft:{material}
      - minecraft:{material}
    result:
      id: atlas:{block_id}
      count: 1
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

Create 128x128 PNG placeholder textures at `src/main/resources/atlas/resourcepack/assets/minecraft/textures/block/custom/`.

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
- [ ] CraftEngine configuration YAML created with all variants
- [ ] Recipe added in the configuration file
- [ ] Descriptor registered in Atlas.kt
- [ ] Dialog cases added
- [ ] TestHelper updated with new descriptor
- [ ] Block count assertions updated (both test files)
- [ ] Block-specific test file created
- [ ] Placeholder textures generated for all referenced texture names
