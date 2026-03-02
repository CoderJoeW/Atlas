# Atlas

A power generation and transfer system plugin for Minecraft Paper servers. Atlas adds custom power blocks — solar panels, cables, and more — with directional power flow, visual feedback, and persistent state across restarts.

## Features

- **Solar Panels** — Generate power during daytime with visual charge indicators
- **Power Cables** — Transfer power directionally between blocks, with powered/unpowered textures
- **Visual Power States** — Blocks change appearance based on their current charge level
- **Persistence** — Power block state saves automatically and survives server restarts
- **Right-Click Info Dialog** — View real-time power stats for any power block
- **Custom Crafting Recipes** — Craft power blocks using vanilla materials

## Requirements

- Paper 1.21+
- [Nexo](https://nexomc.com/) plugin
- Java 21

## Installation

1. Install the Nexo plugin on your Paper server
2. Drop the Atlas JAR into your server's `plugins/` folder
3. Start the server — Atlas will automatically copy its block definitions, textures, and recipes into Nexo's folders

## Configuration

Atlas creates a `config.yml` in `plugins/Atlas/` with resource pack settings:

```yaml
resource-pack:
  enabled: false      # Enable automatic resource pack distribution
  url: ""             # Direct download URL for the resource pack
  hash: ""            # SHA-1 hash for verification/caching
  required: false     # Kick players who decline the pack
  prompt: "Atlas requires a resource pack for custom textures!"
```

When `enabled` is `true`, players are sent the resource pack on join.

## Blocks

| Block | Nexo ID | Power | Behavior |
|-------|---------|-------|----------|
| Small Solar Panel | `small_solar_panel` | 10 max, generates 1/5s | Produces power during daytime (ticks 0-12000). Visual states: empty, low (1-3), medium (4-7), full (8-10) |
| Power Cable | `power_cable` | 1 max, transfers 1/s | Pulls power from behind, pushes forward. Auto-orients to placement direction (N/S/E/W/Up/Down). Powered/unpowered textures |

### Crafting Recipes

**Small Solar Panel** (shapeless): 3 Iron Ingots + 3 Lapis Lazuli + 3 Redstone

**Power Cable** (shapeless): 1 Copper Ingot

## Power Mechanics

- **Generation**: Solar panels produce 1 power every 5 seconds during daytime
- **Transfer**: Cables pull from the block behind them (opposite of facing direction) and push to the block in front, once per second
- **Flow Direction**: Determined by which face you place the cable against — clicking the top of a block places a cable facing UP
- **Visual States**: Blocks swap their Nexo model to reflect charge level, updating every tick cycle
- **Auto-Save**: All power block state is saved to `power_blocks.yml` every 5 minutes and on shutdown

## Commands

No commands are currently registered. All interaction is through block placement and right-click dialogs.
