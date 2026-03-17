<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-62B47A?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.21+"/>
  <img src="https://img.shields.io/badge/Paper-Server-EDA64C?style=for-the-badge" alt="Paper"/>
  <img src="https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21+"/>
  <img src="https://img.shields.io/badge/No_Client_Mods-Required-4FC08D?style=for-the-badge" alt="No Client Mods"/>
  <img src="https://img.shields.io/github/v/release/CoderJoeW/Atlas?style=for-the-badge&color=blue" alt="Latest Release"/>
</p>

<h1 align="center">Atlas</h1>

<p align="center">
  <strong>The industrial update Minecraft never got.</strong><br/>
  Power grids. Fluid pipelines. Automated factories. Zero client mods.
</p>

<p align="center">
  <a href="https://github.com/CoderJoeW/Atlas/releases"><b>Download</b></a> &nbsp;&bull;&nbsp;
  <a href="#getting-started"><b>Get Started</b></a> &nbsp;&bull;&nbsp;
  <a href="#systems-overview"><b>Explore Systems</b></a>
</p>

---

## What is Atlas?

Ever wished vanilla Minecraft had *real* engineering? Atlas brings **power grids, fluid pipelines, and automated factories** to your Paper server — no mods, no client installs, just drop it in and go.

Harness solar energy. Pump lava through pipelines. Smelt ores automatically. Build conveyor-belt assembly lines. All with **16 custom blocks**, unique textures, crafting recipes, and a built-in guide book that teaches your players everything.

Every block is crafted from vanilla materials, follows vanilla placement and breaking mechanics, and blends seamlessly with the base game. If Mojang made an industrial update, it would feel like this.

---

## Why Atlas?

| | |
|:---:|---|
| **Zero Client Mods** | Server-side resource packs through CraftEngine. Players join and it just works. |
| **Interconnected Systems** | Pump lava, pipe it to a generator, cable the power to a drill, conveyor-belt the drops into a smelter — all hands-free. |
| **Visual Feedback** | Batteries show charge level. Containers show fill amount. Cables light up when powered. Know what your factory is doing at a glance. |
| **Easy to Learn** | Craft a book into an Atlas Guide for full in-game documentation. Start with one solar panel and scale to full automation. |
| **Lightweight** | Auto-saves every 5 minutes, persists across restarts, runs cleanly alongside your other plugins. |

---

## Systems Overview

Atlas has three core systems that chain together to enable full automation.

### Power

> Generate, store, and transfer energy to run your machines.

| Block | Role |
|:------|:-----|
| **Small Solar Panel** | Free, renewable energy — just needs clear sky |
| **Lava Generator** | Burns lava for serious output. Feed it from your fluid pipeline |
| **Small Battery** | Buffers power between generators and machines. Glowing charge indicator |
| **Power Cable** | Moves power in one direction, block by block |
| **Power Splitter** | Transfers power across multiple directions at once |
| **Power Merger** | Funnels power from several sources into one output |

<details>
<summary><b>How does power flow?</b></summary>
<br/>
The power system is <b>pull-based</b>. Each block pulls energy from the block behind it. Place cables in a line from generator to machine and power flows automatically — no configuration needed.
</details>

---

### Fluids

> Extract, transport, and store water and lava for use across your builds.

| Block | Role |
|:------|:-----|
| **Fluid Pump** | Pulls water or lava from cauldrons and source blocks |
| **Fluid Pipe** | Carries fluid in one direction, just like a power cable |
| **Fluid Container** | Stores up to 10 units. Visual fill indicator shows level and fluid type |
| **Fluid Merger** | Combines fluid from multiple input directions |

> **Pro tip:** Pipe lava into a Lava Generator for a self-sustaining power loop.

---

### Transport & Automation

> Move items around your world and process them automatically.

| Block | Role |
|:------|:-----|
| **Conveyor Belt** | Pushes dropped items in its facing direction. No power needed |
| **Small Drill** | Mines blocks ahead of it. Point it at stone and watch it dig |
| **Auto Smelter** | Smelts items that pass through on a conveyor belt |
| **Cobblestone Factory** | Produces cobblestone on demand |
| **Obsidian Factory** | Produces obsidian on demand |

> **Try this:** `Drill` -> `Conveyor Belt` -> `Auto Smelter` = fully automated ore processing with zero player input.

---

### Atlas Guide Book

Craft a **Book** into an **Atlas Guide** to get a full illustrated manual covering every block, every system, and tips for building efficient factories. Everything you need to know, right in your inventory.

---

## Getting Started

### Requirements

| Dependency | Version |
|:-----------|:--------|
| [Paper](https://papermc.io/) | 1.21+ |
| [CraftEngine](https://github.com/MoMiRealmS/CraftEngine) | 0.0.67+ |
| Java | 21+ |

### Installation

```
1.  Download the latest Atlas JAR from Releases.
2.  Drop it into your server's plugins/ folder alongside CraftEngine.
3.  Restart the server.
```

That's it. Blocks, recipes, and resource pack assets register automatically. Your players can start crafting immediately.

### Configuration

Atlas creates a `config.yml` in its plugin data folder:

```yaml
logging: true  # Toggle plugin logging
```

Block data is auto-saved to JSON every 5 minutes and persists across restarts.

---

## Texture Editor

Atlas ships with a **custom-built desktop texture editor** purpose-made for creating block textures. No need to juggle Photoshop or Aseprite — this editor understands Minecraft blocks natively.

Load any Atlas block by ID and edit all six faces at once with a real-time 3D preview showing exactly how your texture will look in-game. Switch between block states and variants without leaving the editor.

<details>
<summary><b>Feature list</b></summary>
<br/>

| Feature | Description |
|:--------|:------------|
| **Layer system** | Multiple layers per face with opacity, visibility, and merge controls |
| **Shape tools** | Brush, line, rectangle, ellipse, gradient, flood fill, and eyedropper |
| **Symmetry modes** | Mirror strokes horizontally, vertically, or across all four quadrants |
| **Reference overlay** | Load any image as a semi-transparent tracing guide |
| **3D preview** | Real-time OpenGL cube preview with tiling preview to catch seam issues |
| **100-level undo** | Delta-compressed history so you can experiment freely |
| **Keyboard-driven** | Hotkeys for every tool, face, and action |

</details>

**Launch it:**
```bash
python3 tools/texture-editor/main.py
```

> Requires Python 3 with PyQt6, PyOpenGL, Pillow, PyYAML, and numpy (see `tools/texture-editor/requirements.txt`).

---

## For Developers

<details>
<summary><b>Building from Source</b></summary>

```bash
git clone https://github.com/CoderJoeW/Atlas.git
cd Atlas
mvn clean package
```

The shaded JAR will be in `target/`. Requires Maven and Java 21+.

</details>

<details>
<summary><b>Running Tests</b></summary>

```bash
mvn test
```

</details>

<details>
<summary><b>Code Formatting</b></summary>

Atlas uses [Spotless](https://github.com/diffplug/spotless) with KtLint for Kotlin and Google Java Format for Java.

```bash
mvn spotless:check   # Verify formatting
mvn spotless:apply   # Auto-fix formatting
```

</details>

### Tech Stack

| | |
|:---|:---|
| **Language** | Kotlin 2.3 |
| **Server API** | Paper 1.21 |
| **Custom Blocks** | CraftEngine |
| **Build** | Maven with Shade plugin |
| **Testing** | JUnit 5 + MockK |
| **CI** | GitHub Actions |

---

<p align="center">
  <sub>Built for builders. Powered by redstone-level obsession.</sub>
</p>
