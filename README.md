# Atlas

A Minecraft server plugin that adds power generation, fluid management, and item transport systems. Built for [Paper](https://papermc.io/) 1.21+ servers using [CraftEngine](https://github.com/MoMiRealmS/CraftEngine) for custom blocks and models.

## Features

Atlas introduces three interconnected systems with 16 custom blocks, each with unique textures, crafting recipes, and an in-game guide book.

### Power System

Generators produce power, cables transfer it, batteries store it, and machines consume it. The system is **pull-based** — each block pulls power from the block behind it (opposite its facing direction).

| Block | Description |
|-------|-------------|
| **Small Solar Panel** | Generates 1 power/minute during daytime. Requires clear sky above. |
| **Lava Generator** | Generates 5 power per lava unit consumed. Storage: 50. |
| **Power Cable** | Transfers power in one direction. Storage: 1. |
| **Multi Power Cable** | Multi-directional power transfer. Storage: 10. |
| **Power Merger** | Combines power from multiple input directions. |
| **Small Battery** | Stores up to 10 power. Visual indicator shows charge level. |

### Fluid System

Pumps extract fluid from cauldrons or source blocks, pipes transport it, and containers store it. Supports **water** and **lava**. Also pull-based.

| Block | Description |
|-------|-------------|
| **Fluid Pump** | Extracts fluid from adjacent sources. Costs 1 power/operation. |
| **Fluid Pipe** | Transports fluid in one direction. Storage: 1 unit. |
| **Fluid Container** | Stores up to 10 units. Visual shows fill level and fluid type. |
| **Fluid Merger** | Combines fluid from multiple input directions. |

### Transport System

| Block | Description |
|-------|-------------|
| **Conveyor Belt** | Moves dropped items in its facing direction. No power required. |

### Utility Machines

| Block | Description |
|-------|-------------|
| **Small Drill** | Mines blocks in its facing direction. Costs 10 power/block. |
| **Auto Smelter** | Smelts items passing through it. Costs 2 power/item. |
| **Cobblestone Factory** | Generates cobblestone. |
| **Obsidian Factory** | Generates obsidian. |

### Guide Book

Craft a **Book** into an **Atlas Guide** — a written book with detailed information on every block and system.

## Requirements

- Java 21+
- [Paper](https://papermc.io/) server 1.21+
- [CraftEngine](https://github.com/MoMiRealmS/CraftEngine) plugin (0.0.67+)

## Installation

1. Download the latest Atlas JAR from [Releases](https://github.com/CoderJoeW/Atlas/releases).
2. Place it in your server's `plugins/` directory alongside CraftEngine.
3. Restart the server. Atlas will register its blocks, recipes, and resource pack assets automatically.

## Configuration

Atlas creates a `config.yml` in its plugin data folder with the following options:

```yaml
logging: true  # Enable or disable plugin logging
```

Block data is persisted automatically to JSON files and auto-saved every 5 minutes.

## Building from Source

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

## Tech Stack

- **Language:** Kotlin 2.3
- **Server API:** Paper 1.21
- **Custom Blocks:** CraftEngine
- **Build:** Maven with Shade plugin
- **Testing:** JUnit 5 + MockK
- **CI:** GitHub Actions (build, code quality, code coverage)
