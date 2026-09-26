# Ore Purity

Every chunk carries a record of how much of each mineable ore it held when Atlas first saw it, and
from that a **purity** per ore: Barren, Poor, Normal, Rich or Pure. A mine only reads the chunk it
sits in, and bores at 0×, 0.5×, 1×, 1.5× or 2× its normal pace accordingly — see
[Mines](../mines/README.md#what-a-mine-does).

Atlas Goggles show it: the sidebar on the right of the screen stacks every ore of the dimension with
its purity in the chunk the wearer stands in, and a mine's panel names the purity of its own ore.

## What gets recorded

`ChunkOreSurvey` counts every ore block in a chunk once and stores the counts in the chunk's own
persistent data (`atlas:ore_survey`), so the record is saved, moved and deleted with the chunk and
Atlas keeps no file of its own for it.

- **New chunks** are counted the moment they finish generating (`ChunkLoadEvent.isNewChunk`), before
  a player can touch them. Vanilla features only write into the 3×3 chunks around their origin, and a
  chunk is not fully loaded until all eight neighbours have placed theirs, so that count is final.
- **Chunks that predate Atlas** are counted as they stand the first time they load, and chunks
  already loaded when the plugin enables are counted at startup.
- Once recorded, a chunk is never counted again, so mining ore out or placing it back changes nothing.

Counting runs off the main thread on a chunk snapshot. A chunk that unloads before its count lands is
counted again on its next load. The record stores raw counts, not grades, so the thresholds below can
change without re-surveying anything. Each ore is its own key, so when `Ore` gains an ore, an older
chunk has just that ore counted and added while its other counts stay the original ones. Adding a
*block* to an existing ore needs a new key for that ore, or old records will keep the smaller count.

| Ore | Blocks counted |
|:--|:--|
| Coal | `coal_ore`, `deepslate_coal_ore` |
| Copper | `copper_ore`, `deepslate_copper_ore`, `raw_copper_block` (copper veins) |
| Iron | `iron_ore`, `deepslate_iron_ore`, `raw_iron_block` (iron veins) |
| Redstone | `redstone_ore`, `deepslate_redstone_ore` |
| Lapis | `lapis_ore`, `deepslate_lapis_ore` |
| Amethyst | `budding_amethyst` — the only geode block that grows shards |
| Gold | `gold_ore`, `deepslate_gold_ore` — not `nether_gold_ore`, which yields nuggets, not raw gold |
| Nether Quartz | `nether_quartz_ore` |
| Emerald | `emerald_ore`, `deepslate_emerald_ore` |
| Diamond | `diamond_ore`, `deepslate_diamond_ore` |
| Ancient Debris | `ancient_debris` |

## Purity is relative

A grade compares a chunk with other chunks **of the same ore**, not with other ores: a Rich diamond
chunk is rich for diamonds. Barren means none at all. Among chunks that hold any, the grades split at
the 25th, 75th and 95th percentiles:

| Ore | Poor | Normal | Rich | Pure |
|:--|--:|--:|--:|--:|
| Coal | 1–42 | 43–120 | 121–240 | 241+ |
| Copper | 1–59 | 60–115 | 116–263 | 264+ |
| Iron | 1–64 | 65–83 | 84–101 | 102+ |
| Redstone | 1–28 | 29–42 | 43–51 | 52+ |
| Lapis | 1–18 | 19–27 | 28–33 | 34+ |
| Amethyst | 1–3 | 4–23 | 24–55 | 56+ |
| Gold | 1–20 | 21–30 | 31–39 | 40+ |
| Nether Quartz | 1–61 | 62–154 | 155–222 | 223+ |
| Emerald | 1 | 2–4 | 5–9 | 10+ |
| Diamond | 1–17 | 18–28 | 29–38 | 39+ |
| Ancient Debris | 1 | 2 | 3 | 4+ |

## Where the numbers came from

A throwaway harness on Paper 26.2 (seed `8675309`) generated 2,025 Overworld chunks on a 12-chunk grid
spanning 540 chunks a side, and 484 Nether chunks on the same spacing, and counted each one straight
out of generation.

| Overworld (2,025) | None | Mean | p5 | p25 | p50 | p75 | p95 | p99 | Max |
|:--|--:|--:|--:|--:|--:|--:|--:|--:|--:|
| Coal | 1.8% | 90.0 | 15 | 43 | 74 | 121 | 241 | 326 | 523 |
| Copper | 0.0% | 101.4 | 32 | 60 | 83 | 116 | 264 | 392 | 565 |
| Iron | 0.0% | 75.5 | 44 | 65 | 75 | 84 | 102 | 169 | 331 |
| Redstone | 0.0% | 35.6 | 19 | 29 | 35 | 43 | 52 | 58 | 76 |
| Lapis | 0.2% | 23.2 | 12 | 19 | 24 | 28 | 34 | 37 | 43 |
| Amethyst | 91.1% | 1.5 | 1 | 4 | 11 | 24 | 56 | 69 | 76 |
| Gold | 0.5% | 26.4 | 12 | 21 | 26 | 31 | 40 | 68 | 161 |
| Emerald | 97.7% | 0.1 | 1 | 1 | 3 | 5 | 10 | 13 | 13 |
| Diamond | 0.1% | 24.0 | 10 | 18 | 24 | 29 | 39 | 45 | 60 |

| Nether (484) | None | Mean | p5 | p25 | p50 | p75 | p95 | p99 | Max |
|:--|--:|--:|--:|--:|--:|--:|--:|--:|--:|
| Nether Quartz | 0.6% | 110.5 | 17 | 62 | 106 | 155 | 223 | 254 | 269 |
| Ancient Debris | 23.3% | 1.5 | 1 | 1 | 2 | 3 | 4 | 5 | 6 |

Percentiles are over chunks holding at least one. Lapis, copper, amethyst and nether quartz were added
later and measured by a second pass that re-read the same, still untouched, chunks. Amethyst's
thresholds rest on the 180 chunks that hold a geode. Every Overworld ore is absent from the Nether,
and nether quartz and ancient debris from the Overworld. Emerald only generates in mountain biomes, so
its thresholds come from about 46 chunks and are rounded.

The same run checked the survey itself:

- All 2,509 chunks were recorded, and every record matched an independent count.
- **Settling:** 502 chunks were counted fresh, then all eight neighbours were generated and the chunk
  counted again. None changed, which is what makes counting at generation time exact.
- All 2,509 records read back identically after a server restart.
- A count took about 390 µs per chunk.
