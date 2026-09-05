# Mining System — design

Seven ore mines built on the Iron Mine's rig, drawn as one family in
[`references/mining-system-reactive-states.jpg`](references/mining-system-reactive-states.jpg).

The sheet is laid out like the power, fluid and transport sheets: one row per machine, `Idle / No
power` on the left, `Digging` on the right. Everything it shows is a proposal except the Iron Mine
row, which is the block already specified in [`../iron-mine/README.md`](../iron-mine/README.md).

## One chassis, seven skins

Every mine is the same 2 × 4 × 2 drill rig: machinery house at the back, bolted bore collar in the
deck, a helical auger running down into the rock, two mast rails with a cross brace, a drive motor
on top, an amber power coupling on the **back** face and an output chute on the **front**. The
player learns the shape once and then reads the tier off the colour.

What changes between rows, and nothing else:

| | |
|---|---|
| **Glow colour** | The auger, bore hole, lamps and chute light |
| **Plating and trim** | Sooty black for coal, brass for gold, scorched black for netherite |
| **Bit** | Chisel, steel auger, diamond tip |
| **Emblem plate** | The ore, stamped on the machinery house |
| **Numbers** | Power draw and how often a piece drops |

Power is still the system colour on the coupling socket — the ore tint is the *working* light, so a
rig that is powered but not yet cutting still reads as an Atlas power consumer.

## The seven

Ordered by tier, which is also the order on the sheet:

| Mine | Ore | Glow | Power | Drops | Buffer |
|---|---|---|---|---|---|
| **Coal Mine** | Coal | Ember orange `#E2521F` | 2/s | 1 every 8 s | 16 |
| **Iron Mine** | Raw iron | Amber `#FFA92B` | 4/s | 1 every 10 s | 40 |
| **Redstone Mine** | Redstone dust | Crimson `#FF2D2D` | 6/s | 2 every 12 s | 72 |
| **Gold Mine** | Raw gold | Gold `#FFC531` | 8/s | 1 every 14 s | 112 |
| **Emerald Mine** | Emerald | Green `#2BE07A` | 12/s | 1 every 25 s | 300 |
| **Diamond Mine** | Diamond | Cyan `#45E0FF` | 16/s | 1 every 30 s | 480 |
| **Netherite Mine** | Ancient debris | Violet `#B45BFF` | 24/s | 1 every 60 s | 1440 |

The buffer column is not a separate dial: it is draw × cadence, so a full buffer is exactly one
drop's worth, the rule the Iron Mine already follows. The Iron Mine row is the shipped spec; the
other six are starting points to balance against, not settled numbers.

Two of them want a gate beyond power, or the tier collapses into "leave it running overnight":

- **Netherite Mine** — ancient debris is a Nether resource. Either it only runs in the Nether, or
  it takes lava the way the Lava Generator does, so it costs a fluid line as well as a cable.
- **Emerald Mine** — emeralds come out of trade, not automation, in vanilla. Its output is
  deliberately slower than diamond per unit of power to keep it a luxury rather than a printer.

Coal is the entry rig: cheap enough to be somebody's first machine, and it feeds the Auto Smelter,
so the first thing a new player automates powers the second thing.

## States

Two, exactly as the Iron Mine has them, and the sheet shows nothing else:

| State | Property | Reads as |
|---|---|---|
| Idle / no power | `running=false` | Lamps dark, socket ring dull, auger and bore hole cold, no ore |
| Digging | `running=true` | Lamps burn, socket ring glows, auger and bore hole glow in the ore tint, ore spills from the chute |

Each mine keeps **one block id** and flips `running`, rather than switching ids, because the
footprint is laid out from the facing and switching would drop it. Two states × four facings = eight
appearances per mine on one forced barrier state, costing zero auto-state slots — seven mines is
seven ids, not fifty-six.

## What this implies for the code

The seven differ only in id, textures, tint, power draw, cadence and drop, which is a table, not
seven classes. One `OreMine` block reading a `MineTier` enum gives the whole family, and the
footprint check, the frame cells, the back-face-only `canAcceptFrom`, the front chute and the
wrench toggle are all inherited from the Iron Mine unchanged. Each mine still needs its own
`atlas:<ore>_mine_frame` barrier id so a broken cell can find its own rig.

Textures are the expensive part: eight parts per mine, each with a lit twin, is 16 files per rig and
112 for the set. Only the emblem plate, the bit and the tint actually differ, so the deck, base,
tower and collar should be drawn once and recoloured per tier rather than generated seven times —
and the auger and tower still have to read the same at any horizontal crop, for the reason the Iron
Mine's do.

## Open

- Does a mine need to sit on or near the matching ore, or is it a flat generator like the Iron
  Mine? Flat is simpler and never needs relocating; ore-gated is more interesting to place.
- Netherite's gate: Nether-only, lava-fed, or both.
- Whether the tiers want a shared upgrade path (a speed module, an ore filter) or stay seven fixed
  blocks.
