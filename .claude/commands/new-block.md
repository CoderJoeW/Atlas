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
3. **System type**: `power`, `fluid`, `transport`, or `utility` — determines base class (`PowerBlock`, `FluidBlock`, `TransportBlock`) and which registry/factory/dialog to integrate with. Utility blocks use the power system (extend `PowerBlock`) but live in the `utility` package.
4. **Placement type**: `SIMPLE` (no direction), `DIRECTIONAL` (faces toward placement direction), or `DIRECTIONAL_OPPOSITE` (faces away)
5. **Visual states**: What variants does the block have? (e.g., inactive/active, charge levels, facing directions)
6. **Key properties**: maxStorage, updateIntervalTicks, canReceivePower, etc.
7. **Behavior**: What happens during powerUpdate()/fluidUpdate()/transportUpdate()?
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

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "{Display Name}",
                description = "{short description}",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(),  // include any variant IDs beyond the base
                constructor = { loc, _ -> {ClassName}(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = BLOCK_ID  // add state logic as needed

    override fun powerUpdate() {  // or fluidUpdate() for fluid blocks
        // Block behavior
    }
}
```

**DIRECTIONAL block** (like PowerCable, FluidPipe): Include `facing` property via `override val facing: BlockFace` in the constructor. Constructor takes `(Location, BlockFace)`. Use `ADJACENT_FACES` (inherited from `AtlasBlock`) when iterating over all 6 block faces — never hardcode the face list.

**DIRECTIONAL_OPPOSITE block** (like SmallDrill): Same directional pattern but placement type is `DIRECTIONAL_OPPOSITE`.

### Important code patterns

- **Fluid transfer ordering**: When transferring fluid between blocks, always `removeFluid()` first to capture the value, then call `target.storeFluid(fluid)`. If `storeFluid` fails, restore with `storeFluid(fluid)` on self. Never pass `storedFluid` directly to another block's `storeFluid()` — the mutable field could become stale.
- **Face iteration**: Use the inherited `ADJACENT_FACES` constant from `AtlasBlock` instead of creating a new `listOf(BlockFace.NORTH, ...)`. Filter it as needed (e.g., `ADJACENT_FACES.filter { it != facing.oppositeFace }`).
- **BlockDescriptor fields**: The descriptor uses `additionalBlockIds` (not `allRegistrableIds`) for variant IDs beyond the base. The total registered count = 1 (base) + additionalBlockIds.size.

### Step 3: Add CraftEngine Block Configuration

Create `src/main/resources/atlas/configuration/{block_id}.yml`.

Follow the CraftEngine configuration format. Reference existing configuration files for the exact format. Here's the general structure for a **non-directional** block:

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

For **directional** blocks, use `states` with `properties`, `appearances`, and `variants` sections. Reference `power_splitter.yml` (facing + boolean property), `fluid_merger.yml` (facing + string property), or `fluid_pipe.yml` for the exact format. Key pattern:
- Define properties (facing: direction, plus any state property like powered/fluid)
- Define appearances for each direction (north uses full generation block, south/east/west use y-rotation, up/down use separate models with remapped textures)
- Map variants to appearances

For additional variant entries (active states, etc.), add `items#1`, `items#2`, etc. sections with explicit loot that drops the base item.

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

Ensure the recipe ingredients are unique — no two blocks should have identical shapeless recipes.

### Step 5: Register in Atlas.kt

Add the descriptor to `src/main/kotlin/com/coderjoe/atlas/Atlas.kt`:

- **Power blocks**: Add to the list in `powerDescriptors()`
- **Fluid blocks**: Add to the list in `fluidDescriptors()`
- **Transport blocks**: Add to the list in `transportDescriptors()`
- **Utility blocks**: Add to `powerDescriptors()` (utility blocks use the power system)

### Step 6: Add Dialog Integration

For **power blocks** (including utility), edit `src/main/kotlin/com/coderjoe/atlas/power/PowerBlockDialog.kt`:
1. Add import for the new class
2. Add `is {ClassName} -> "{Display Name}"` case in `getBlockDisplayName()`
3. Add description case in `getBlockDescription()`:
   ```kotlin
   is {ClassName} -> Component.text("{description}")
       .color(NamedTextColor.GRAY)
   ```

For **fluid blocks**, edit `src/main/kotlin/com/coderjoe/atlas/fluid/FluidBlockDialog.kt` following the same pattern.

For **transport blocks**, edit `src/main/kotlin/com/coderjoe/atlas/transport/TransportBlockDialog.kt` following the same pattern.

### Step 7: Update Tests

1. **Update `TestHelper.initPowerFactory()`** (or `initFluidFactory()` / `initTransportFactory()`) in `src/test/kotlin/com/coderjoe/atlas/TestHelper.kt` — add the new descriptor to the registration list.

2. **Update block count assertions** in:
   - `src/test/kotlin/com/coderjoe/atlas/AtlasPluginTest.kt` — update the count in the relevant `{system} system initializes with N block types` test
   - `src/test/kotlin/com/coderjoe/atlas/power/PowerBlockInitializerTest.kt` (or fluid/transport equivalent) — update the count and comment

   The count = total number of registered IDs (1 base + additionalBlockIds per block). Add the number of new IDs to the existing count.

3. **Create block test file** at `src/test/kotlin/com/coderjoe/atlas/{system}/{ClassName}Test.kt` with tests for:
   - Key properties (maxStorage, canReceivePower, etc.)
   - Visual state transitions
   - Core behavior (powerUpdate/fluidUpdate/transportUpdate logic)
   - Descriptor properties (baseBlockId, displayName)
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
- [ ] `additionalBlockIds` includes all variant IDs beyond the base
- [ ] Uses `ADJACENT_FACES` constant (not hardcoded face lists) when iterating faces
- [ ] Fluid transfers use remove-first-then-store pattern
- [ ] CraftEngine configuration YAML created with all variants
- [ ] Recipe added in the configuration file (with unique ingredients)
- [ ] Descriptor registered in Atlas.kt
- [ ] Dialog cases added
- [ ] TestHelper updated with new descriptor
- [ ] Block count assertions updated (both test files)
- [ ] Block-specific test file created
- [ ] Placeholder textures generated for all referenced texture names
