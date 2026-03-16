<p align="center">
  <h1 align="center">Atlas</h1>
  <p align="center">
    <strong>Industrial automation for Minecraft — powered by your imagination.</strong>
  </p>
  <p align="center">
    <a href="https://github.com/CoderJoeW/Atlas/releases">Download</a> &middot;
    <a href="#getting-started">Get Started</a> &middot;
    <a href="#systems-overview">Explore Systems</a>
  </p>
</p>

---

Ever wished vanilla Minecraft had real engineering? Atlas brings **power grids, fluid pipelines, and automated factories** to your Paper server — no mods, no client installs, just drop it in and go.

Harness solar energy, pump lava through pipelines, smelt ores automatically, and build conveyor-belt assembly lines — all with **16 custom blocks**, unique textures, crafting recipes, and a built-in guide book to teach your players everything.

Atlas is designed to feel like a natural extension of vanilla Minecraft, not a separate system bolted on top. Every block is crafted from vanilla materials, follows vanilla placement and breaking mechanics, and blends seamlessly with the look and feel of the base game. It's the industrial update Minecraft never got.

## Why Atlas?

- **Zero client mods required.** Atlas uses server-side resource packs through CraftEngine. Players join and it just works.
- **Interconnected systems.** Power, fluids, and transport are designed to work together. Pump lava, pipe it to a generator, cable the power to a drill, and conveyor-belt the drops into a smelter — all hands-free.
- **Visual feedback everywhere.** Batteries show charge level. Fluid containers show fill amount and fluid type. Cables light up when powered. You always know what your factory is doing at a glance.
- **Easy to learn, deep to master.** Craft a book into an Atlas Guide for in-game documentation on every block. Start with a single solar panel and scale up to fully automated processing lines.
- **Lightweight and reliable.** Auto-saves every 5 minutes, persists across restarts, and runs cleanly alongside your other plugins.

## Systems Overview

Atlas has three core systems that chain together to enable full automation.

### Power

Generate, store, and transfer energy to run your machines.

| Block | What it does |
|-------|--------------|
| **Small Solar Panel** | Generates power during the day. Free, renewable energy — just needs clear sky. |
| **Lava Generator** | Burns lava for serious power output. Feed it from your fluid pipeline. |
| **Small Battery** | Buffers power between generators and machines. Glowing charge indicator. |
| **Power Cable** | Moves power in one direction, block by block. |
| **Multi Power Cable** | Transfers power across multiple directions at once. |
| **Power Merger** | Funnels power from several sources into one output. |

> **How it works:** The power system is pull-based. Each block pulls energy from the block behind it. Place cables in a line from generator to machine and power flows automatically.

### Fluids

Extract, transport, and store water and lava for use across your builds.

| Block | What it does |
|-------|--------------|
| **Fluid Pump** | Pulls water or lava from cauldrons and source blocks. |
| **Fluid Pipe** | Carries fluid in one direction, just like a power cable. |
| **Fluid Container** | Stores up to 10 units. Visual fill indicator shows level and fluid type. |
| **Fluid Merger** | Combines fluid from multiple input directions. |

> **Pro tip:** Pipe lava into a Lava Generator for a self-sustaining power loop.

### Transport & Automation

Move items around your world and process them automatically.

| Block | What it does |
|-------|--------------|
| **Conveyor Belt** | Pushes dropped items in its facing direction. No power needed. |
| **Small Drill** | Mines blocks ahead of it. Point it at stone and watch it dig. |
| **Auto Smelter** | Smelts items that pass through it on a conveyor belt. |
| **Cobblestone Factory** | Produces cobblestone on demand. |
| **Obsidian Factory** | Produces obsidian on demand. |

> **Build this:** Drill → Conveyor Belt → Auto Smelter. Fully automated ore processing with zero player input.

### In-Game Guide Book

Craft a **Book** into an **Atlas Guide** to get a full illustrated manual covering every block, every system, and tips for building efficient factories. No wiki-diving required.

## Getting Started

### Requirements

- [Paper](https://papermc.io/) server 1.21+
- [CraftEngine](https://github.com/MoMiRealmS/CraftEngine) plugin (0.0.67+)
- Java 21+

### Installation

1. Download the latest Atlas JAR from [Releases](https://github.com/CoderJoeW/Atlas/releases).
2. Drop it into your server's `plugins/` folder alongside CraftEngine.
3. Restart the server — blocks, recipes, and resource pack assets register automatically.

That's it. Your players can start crafting Atlas blocks immediately.

### Configuration

Atlas creates a `config.yml` in its plugin data folder:

```yaml
logging: true  # Toggle plugin logging
```

Block data is auto-saved to JSON every 5 minutes and persists across restarts.

---

## Texture Editor

Atlas ships with a **custom-built desktop texture editor** purpose-made for creating block textures. No need to juggle Photoshop or Aseprite — this editor understands Minecraft blocks natively.

**Built for blocks, not generic images.** Load any Atlas block by ID and edit all six faces at once with a real-time 3D preview showing exactly how your texture will look in-game. Switch between block states and variants without leaving the editor.

**Features:**
- **Layer system** — multiple layers per face with opacity, visibility, and merge controls
- **Shape tools** — brush, line, rectangle, ellipse, gradient, flood fill, and eyedropper
- **Symmetry modes** — mirror your strokes horizontally, vertically, or across all four quadrants for perfectly balanced textures
- **Reference overlay** — load any image as a semi-transparent tracing guide
- **3D preview** — real-time OpenGL cube preview with your textures applied, plus a tiling preview to catch seam issues
- **100-level undo** — delta-compressed history so you can experiment freely
- **Keyboard-driven workflow** — hotkeys for every tool, face, and action

**Launch it:**
```bash
python3 tools/texture-editor/main.py
```

Requires Python 3 with PyQt6, PyOpenGL, Pillow, PyYAML, and numpy (see `tools/texture-editor/requirements.txt`).

---

## For Developers

### Building from Source

```bash
git clone https://github.com/CoderJoeW/Atlas.git
cd Atlas
mvn clean package
```

The shaded JAR will be in `target/`. Requires Maven and Java 21+.

### Running Tests

```bash
mvn test
```

### Code Formatting

Atlas uses [Spotless](https://github.com/diffplug/spotless) with KtLint for Kotlin and Google Java Format for Java.

```bash
mvn spotless:check   # Verify formatting
mvn spotless:apply   # Auto-fix formatting
```

### Tech Stack

| | |
|---|---|
| **Language** | Kotlin 2.3 |
| **Server API** | Paper 1.21 |
| **Custom Blocks** | CraftEngine |
| **Build** | Maven with Shade plugin |
| **Testing** | JUnit 5 + MockK |
| **CI** | GitHub Actions (build, code quality, coverage) |
