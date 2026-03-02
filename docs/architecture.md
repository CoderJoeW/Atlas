# Atlas Architecture

## Plugin Lifecycle

### Startup (`onEnable`)

1. Create the plugin data folder
2. **Nexo Integration** — Copy block definitions, textures, and recipes into Nexo's folders (only if they don't already exist)
3. **Resource Pack** — Load `config.yml`, configure resource pack distribution. If enabled, register `PlayerJoinListener`
4. **Power Dialog** — Initialize the `PowerBlockDialog` singleton with the plugin instance
5. **Power System** — Register block types in the factory, create the registry, load persisted blocks from disk, register the `PowerBlockListener`, start the auto-save timer

### Shutdown (`onDisable`)

1. Cancel the auto-save timer
2. Save all power blocks to `power_blocks.yml`
3. Cancel all active dialog refresh tasks
4. Stop all power block tick tasks

## Package Structure

```
com.coderjoe.atlas/
├── Atlas.kt                          # Main plugin class — lifecycle, wiring
│
├── integration/
│   ├── NexoIntegration.kt            # Copies block configs, textures, recipes to Nexo
│   └── ResourcePackManager.kt        # Resource pack URL/hash config and player delivery
│
├── listener/
│   ├── PlayerJoinListener.kt         # Sends resource pack on join
│   └── PowerBlockListener.kt         # Block place/break/interact → registry ops
│
├── power/
│   ├── PowerBlock.kt                 # Abstract base — storage, ticking, visual state
│   ├── PowerBlockData.kt             # Serialization DTO for persistence
│   ├── PowerBlockFactory.kt          # Registry of block ID → constructor mappings
│   ├── PowerBlockInitializer.kt      # Registers all concrete block types at startup
│   ├── PowerBlockRegistry.kt         # Tracks live power blocks by location
│   │
│   ├── block/
│   │   ├── PowerCable.kt             # Directional power transfer
│   │   └── SmallSolarPanel.kt        # Daytime power generation
│   │
│   └── persistence/
│       └── PowerBlockPersistence.kt  # YAML save/load for power block state
│
└── ui/
    └── PowerBlockDialog.kt           # Paper Dialog API — real-time power info
```

## Design Patterns

### Factory (`PowerBlockFactory`)

A static registry mapping Nexo block IDs to constructor lambdas. When a block is placed or loaded from disk, the factory creates the correct `PowerBlock` subclass by ID. New block types only need to register themselves here.

### Registry (`PowerBlockRegistry`)

Tracks all live power blocks in a `ConcurrentHashMap` keyed by `"world:x,y,z"`. Provides adjacency queries used by cables for power transfer. Exposes a singleton `instance` for cross-cutting access (e.g., from `PowerBlockDialog`).

### Observer (Bukkit Event System)

`PowerBlockListener` subscribes to `BlockPlaceEvent`, `BlockBreakEvent`, and `PlayerInteractEvent`. It bridges Bukkit's event system to the power domain — creating/destroying power blocks and opening dialogs.

### Template Method (`PowerBlock`)

The abstract `PowerBlock` base class defines the tick lifecycle (`start` → `powerUpdate` + `updateVisualState` → `stop`). Subclasses implement `powerUpdate()` for their specific logic and `getVisualStateBlockId()` to control appearance.

## Dependencies Between Classes

```
Atlas
├── NexoIntegration
├── ResourcePackManager
│   └── PlayerJoinListener
├── PowerBlockDialog
├── PowerBlockInitializer
│   └── PowerBlockFactory
├── PowerBlockRegistry
├── PowerBlockPersistence
│   ├── PowerBlockData
│   ├── PowerBlockFactory
│   └── PowerBlockRegistry
└── PowerBlockListener
    ├── PowerBlockFactory
    ├── PowerBlockRegistry
    └── PowerBlockDialog

PowerCable
├── PowerBlock
└── PowerBlockRegistry (via singleton)

SmallSolarPanel
└── PowerBlock
```

## Persistence

Power blocks are saved to `plugins/Atlas/power_blocks.yml` as a list of maps:

```yaml
power_blocks:
  - blockId: small_solar_panel
    world: world
    x: 100
    y: 64
    z: -200
    currentPower: 7
  - blockId: power_cable_north
    world: world
    x: 101
    y: 64
    z: -200
    currentPower: 1
    facing: NORTH
```

Auto-save runs every 6000 ticks (5 minutes). A full save also runs on shutdown.

## Tick Intervals

| Component | Interval | Purpose |
|-----------|----------|---------|
| `SmallSolarPanel` | 100 ticks (5s) | Generate power, update visual state |
| `PowerCable` | 20 ticks (1s) | Pull/push power, update visual state |
| `PowerBlockDialog` refresh | 10 ticks (0.5s) | Update dialog display for viewing player |
| Auto-save | 6000 ticks (5 min) | Persist all power block state to disk |
