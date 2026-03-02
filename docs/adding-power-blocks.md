# Adding a New Power Block

This guide walks through adding a new power block type to Atlas, using the existing `SmallSolarPanel` as a reference.

## 1. Create the Block Class

Create a new file in `com.coderjoe.atlas.power.block`:

```kotlin
package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location

class Battery(location: Location) : PowerBlock(location, maxStorage = 50) {

    companion object {
        const val BLOCK_ID = "battery"
    }

    // How often powerUpdate() runs (in ticks). Default is 100 (5s).
    override val updateIntervalTicks: Long = 100L

    // Whether this block can receive power from cables. Default is true.
    // Set to false for generators like SmallSolarPanel.
    override val canReceivePower: Boolean = true

    override fun getVisualStateBlockId(): String = when {
        currentPower == 0 -> "battery"
        currentPower <= maxStorage / 2 -> "battery_half"
        else -> "battery_full"
    }

    override fun powerUpdate() {
        // Your power logic here.
        // This runs every updateIntervalTicks.
        // Access currentPower, addPower(), removePower(), hasPower(), canAcceptPower().
    }
}
```

### Key base class methods

| Method | Description |
|--------|-------------|
| `addPower(amount): Int` | Adds power up to `maxStorage`, returns amount actually added |
| `removePower(amount): Int` | Removes power, returns amount actually removed |
| `hasPower(): Boolean` | `true` if `currentPower > 0` |
| `canAcceptPower(): Boolean` | `true` if `canReceivePower` and not full |

## 2. Register in PowerBlockInitializer

Open `com.coderjoe.atlas.power.PowerBlockInitializer` and add a registration call:

```kotlin
PowerBlockFactory.register("battery") { location, _ ->
    Battery(location)
}
```

The second parameter (`BlockFace`) is the facing direction — only relevant for directional blocks like `PowerCable`. For non-directional blocks, ignore it with `_`.

## 3. Add Nexo Block Definition

Add an entry to `src/main/resources/nexo/items/atlas_blocks.yml`:

```yaml
battery:
  itemname: "<gradient:#00FF00:#008000>Battery"
  material: paper
  Pack:
    generate_model: true
    parent_model: block/cube_all
    textures:
      all: atlas:block/battery
  Mechanics:
    custom_block:
      type: NOTEBLOCK
      custom_variation: 120    # Must be unique across all Atlas blocks
      hardness: 3
      block_sounds:
        break_sound: block.metal.break
        place_sound: block.metal.place
        hit_sound: block.metal.hit
        step_sound: block.metal.step
        fall_sound: block.metal.fall
      drop:
        silktouch: false
        loots:
          - nexo_item: battery
            probability: 1.0
```

If you have visual state variants (e.g., `battery_half`, `battery_full`), add separate block entries for each with unique `custom_variation` numbers. Their drops should reference the base item (`battery`).

## 4. Create Textures

Place PNG textures in `src/main/resources/nexo/pack/assets/atlas/textures/block/`:

- `battery.png` — Base/empty state
- `battery_half.png` — Half-charged state (if using visual states)
- `battery_full.png` — Full state (if using visual states)

Then register the texture names in `NexoIntegration.copyTextures()` so they get copied to Nexo's pack folder:

```kotlin
val textures = listOf(
    // ... existing textures ...
    "battery",
    "battery_half",
    "battery_full"
)
```

## 5. Add a Crafting Recipe

Add a recipe to `src/main/resources/nexo/recipes/shapeless/atlas_recipes.yml`:

```yaml
battery_recipe:
  result:
    nexo_item: battery
    amount: 1
  ingredients:
    A:
      amount: 4
      minecraft_type: IRON_INGOT
    B:
      amount: 2
      minecraft_type: REDSTONE
```

## 6. Update the Dialog (Optional)

If you want custom display text in the right-click info dialog, update `PowerBlockDialog`:

- Add a display name in `getBlockDisplayName()`
- Add an info line in `buildPowerInfo()`

## 7. Update Drop Handling

In `PowerBlockListener.onBlockBreak()`, add a case for your block's base item ID so that visual-state variants drop the correct item:

```kotlin
val baseItemId = when (powerBlock) {
    is PowerCable -> PowerCable.BLOCK_ID
    is SmallSolarPanel -> SmallSolarPanel.BLOCK_ID
    is Battery -> Battery.BLOCK_ID
    else -> null
}
```

## 8. Test

1. Build: `mvn clean package`
2. Copy the JAR to your test server's `plugins/` folder
3. Delete Nexo's cached Atlas files so the new definitions get copied (or manually add them)
4. Restart the server
5. Verify:
   - Block can be placed and appears with correct texture
   - Right-click shows the info dialog
   - Power logic works (generation, transfer, storage)
   - Visual states change with power level
   - Block persists across server restarts
   - Breaking the block drops the correct item

## Checklist

- [ ] Block class extends `PowerBlock` with correct `maxStorage`
- [ ] `BLOCK_ID` constant matches the Nexo YAML key
- [ ] Registered in `PowerBlockInitializer`
- [ ] Nexo block definition with unique `custom_variation`
- [ ] Textures created and registered in `NexoIntegration`
- [ ] Visual state variants defined (if applicable)
- [ ] Crafting recipe added
- [ ] Dialog display name and info line added
- [ ] Drop handling in `PowerBlockListener`
- [ ] Tested placement, persistence, and power flow
