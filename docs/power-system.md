# Power System

Technical documentation for Atlas's power generation, transfer, and storage mechanics.

## Overview

The power system is a network of blocks that generate, store, and transfer power. Each block tracks its own `currentPower` against a `maxStorage` capacity. Blocks update on independent timers and interact with neighbors through the `PowerBlockRegistry`.

## Power Generation

### Small Solar Panel

- **Capacity**: 10 units
- **Generation rate**: 1 unit per update cycle
- **Update interval**: 100 ticks (5 seconds)
- **Condition**: Only generates during daytime (world time 0–12000)
- **Receives power**: No (`canReceivePower = false`)

During each update, if it's daytime and `currentPower < maxStorage`, the panel adds 1 power. At night, no generation occurs but stored power is retained.

## Power Transfer

### Power Cable

- **Capacity**: 1 unit (pass-through buffer)
- **Transfer rate**: 1 unit per update cycle
- **Update interval**: 20 ticks (1 second)
- **Receives power**: Yes

Cables are directional — they have a `facing` direction set at placement time based on which face of the adjacent block the player clicked.

#### Transfer Algorithm

Each update cycle, a cable executes two operations in order:

1. **Pull**: Look at the block behind (opposite of facing). If that block has power and the cable has space, pull 1 unit.
2. **Push**: Look at the block ahead (facing direction). If the cable has power and the destination can accept it, push 1 unit.

This means a chain of cables facing the same direction moves power at 1 unit per second per cable link. The pull-then-push order within a single tick means a cable can receive and forward in the same update.

#### Directional Variants

When a player places the base `power_cable` item, `PowerBlockListener` detects the placement surface and swaps it to a directional variant:

| Placed Against Face | Cable Faces | Nexo Block ID |
|---------------------|-------------|---------------|
| Top of block | UP | `power_cable_up` |
| Bottom of block | DOWN | `power_cable_down` |
| North face | NORTH | `power_cable_north` |
| South face | SOUTH | `power_cable_south` |
| East face | EAST | `power_cable_east` |
| West face | WEST | `power_cable_west` |

Each directional variant has a corresponding `_powered` variant (e.g., `power_cable_north_powered`) that displays when the cable holds power.

## Storage Model

Every `PowerBlock` has:

- `maxStorage: Int` — Maximum power capacity
- `currentPower: Int` — Current stored power (0 to `maxStorage`)

The `addPower(amount)` and `removePower(amount)` methods enforce bounds and return the amount actually added/removed.

## Visual States

Blocks report their desired Nexo block ID via `getVisualStateBlockId()`. The base class compares this to the current state after each `powerUpdate()` call. If they differ, it replaces the block:

1. Set the block to `AIR` (without physics updates)
2. Place the new Nexo block via `NexoBlocks.place()`

This avoids unnecessary block updates when the state hasn't changed.

### Solar Panel Visual Thresholds

| Power Level | Block ID |
|-------------|----------|
| 0 | `small_solar_panel` |
| 1–3 | `small_solar_panel_low` |
| 4–7 | `small_solar_panel_medium` |
| 8–10 | `small_solar_panel_full` |

### Cable Visual States

| State | Block ID Pattern |
|-------|-----------------|
| Unpowered | `power_cable_{direction}` |
| Powered | `power_cable_{direction}_powered` |

## Update Scheduling

Each `PowerBlock` starts a `BukkitTask` repeating timer when registered. The timer calls `powerUpdate()` then `updateVisualState()` at the block's `updateIntervalTicks` rate.

On initial registration (including load from persistence), the block also defers a one-tick visual state correction to handle cases where the persisted state doesn't match the placed Nexo block.

## Persistence Format

All power blocks are saved to `plugins/Atlas/power_blocks.yml`:

```yaml
power_blocks:
  - blockId: small_solar_panel    # Nexo block ID (directional variant for cables)
    world: world                   # World name
    x: 100                         # Block coordinates
    y: 64
    z: -200
    currentPower: 7                # Stored power at save time
  - blockId: power_cable_north
    world: world
    x: 101
    y: 64
    z: -200
    currentPower: 1
    facing: NORTH                  # Only present for directional blocks
```

### Save Triggers

- **Auto-save**: Every 6000 ticks (5 minutes)
- **Shutdown**: Full save during `onDisable()`

### Load Process

1. Read the YAML file
2. For each entry, reconstruct the `PowerBlockData` DTO
3. Resolve the world and location
4. Use `PowerBlockFactory.createPowerBlock()` to instantiate the correct subclass
5. Restore `currentPower`
6. Register in the `PowerBlockRegistry` (which starts the tick timer)
7. Defer a one-tick visual state correction
