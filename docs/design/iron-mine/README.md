# Iron Mine — design

The Iron Mine replaces the Small Drill and the Soft Touch Drill, which were both retired on
2026-09-04. It is the first Atlas block that fills more than one cell.

## Shape

A drill rig boring straight down into the rock: **2 wide × 4 tall × 2 deep** — 16 cells. The
cell the player placed is the **front-left-bottom corner**; the rest of the rig grows right, up
and backwards from it, so the body always sits behind the face the player was looking at.

Placement is `HORIZONTAL_OPPOSITE`: the front faces the player who set it down, on the horizontal
plane only, whichever surface it was placed against. `DIRECTIONAL`/`DIRECTIONAL_OPPOSITE` read the
face the block was placed against, which answers UP for anything set down on the ground — no use
for a machine with a front and a back.

The footprint is tested *before* the placement is allowed. If any of the other fifteen cells is
occupied, the placement is cancelled, the player keeps the item and is told what is in the way, so
a rig that will not fit is never half built.

## Two working faces, opposite each other

| Face | Does |
|---|---|
| **Back** | The only face that takes power. `canAcceptFrom` refuses every other side. |
| **Front** | Raw iron drops out of the chute, just clear of the front face. |

Nothing else on the rig does anything, which is the point: the player can read the machine from
across the room. The back carries a large amber power coupling socket and the front a riveted
output chute with raw iron ore spilling out of it, so the art says the same thing the code does.

The mine never hands power back out (`canOutputToward` is always false) — it is a sink, not a
battery.

## Behaviour

Burns **4 power/s** and drops **1 raw iron every 10 s**; a full 40-power buffer is exactly one
piece of iron's worth. It generates on a timer and never touches the world below it, so it neither
mines itself out nor needs relocating. The wrench dialog toggles it on and off, and both the toggle
and the part-finished run survive a restart.

## Rendering

One display entity on the anchor cell carries the whole rig, and the other fifteen cells are filled
with `atlas:iron_mine_frame` — a forced barrier state with **no renderer at all**: invisible, but
solid, so the rig you can see is the rig you walk into and nothing can be built inside it. The
frame drops nothing; breaking any cell takes the whole machine down and drops the one Iron Mine
item.

### Silhouette

The first pass was a pithead headframe and read as a **watchtower**, not a mine. What it lacked was
anything actually digging. The rig is now built around the bore hole: a machinery house at the back,
a bolted steel **bore collar** set into the deck, a thick **drill string with helical auger
flighting** running down through it into the ground, two riveted mast rails and a cross brace
holding the string, and a boxy **drive motor** capping the top. The ore comes out of the chute at
the front.

### Why the model is half size

A model element may not leave the `-16..32` range, so a 2 × 4 × 2 block structure — 32 × 64 × 32
pixels — cannot be authored directly. `iron_mine_base.json` is drawn at **half scale** in a
16 × 32 × 16 box and the display entity is `scale: [2, 2, 2]`, which brings it back to full size.

`position` puts the entity over the **middle of the rig's 2 × 2 base** rather than the middle of
the anchor cell, and `y` is `1.0` because an item display centres the model on its position and the
model is four blocks tall. Since the yaw rotates about that point and the footprint is symmetrical
about it, each facing needs its own corner:

| `facing` | `yaw` | `position` |
|---|---|---|
| north | `180` | `[1.0, 1.0, 1.0]` |
| south | *(none)* | `[0.0, 1.0, 0.0]` |
| east | `-90` | `[0.0, 1.0, 1.0]` |
| west | `90` | `[1.0, 1.0, 0.0]` |

`entity_culling: false`, because CraftEngine otherwise culls the display entity against the single
cell it belongs to and a four-block rig would vanish while its top was still plainly in view.

## States

| State | Property | Reads as |
|---|---|---|
| Idle / no power | `running=false` | Lamps dark, the socket ring a dull amber, the auger and bore hole cold |
| Digging | `running=true` | Lamps burn, the socket ring glows, the auger and bore hole glow amber, light spills from the chute |

Like the belt, the rig keeps **one block id** and flips a `running` property rather than switching
ids — switching would drop the facing the whole footprint is laid out from. Two states × four
facings = eight appearances on one forced barrier state, costing zero auto-state slots.

## Textures

Power system, so the energy colour is **amber** on charcoal. Eight parts: `base` and `deck` are
shared between both states, and `housing`, `port`, `chute`, `tower`, `collar` and `drill` each have
a lit twin. Generated per `.claude/commands/new-block.md` step 8; every lit variant was measured
strictly brighter than its unlit source (+5.1 to +16.6 mean luminance).

Two textures are cropped rather than shown whole, so both are drawn to read the same at any
horizontal crop: the `tower` lattice truss (the mast rails sample a narrow vertical slice) and the
`drill` auger, which is a single shaft centred on flat black with the flights reaching wider than
the shaft — the string samples the middle half of it. Every other face samples its whole texture.

**Style trap:** asking for the drill pipe by image-to-image off the armour panel produced a
photoreal close-up of three steel augers, nothing like the flat hand-drawn set. Re-prompting from
the on-style collar kept the collar in the frame instead. What worked was a fresh text-to-image
naming the style constraints outright — flat, head-on, orthographic, crisp dark outlines, *no
photographic realism, no glossy reflections* — plus the exact charcoal RGB.
