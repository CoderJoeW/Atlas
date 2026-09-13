# Mines

Seven mines — Coal, Iron, Redstone, Gold, Emerald, Diamond and Netherite — built from the
reactive-states chart in [`references/mining-system-reactive-states.jpg`](references/mining-system-reactive-states.jpg).
That chart is the authority for how the system looks.

## What a mine does

A mine turns stored power straight into ore. It does not touch the world around it — the cut is
fiction — so there is no deposit to find, nothing to deplete, and no terrain is broken. Build one
anywhere a cable reaches and it works forever.

A mine pulls power from its neighbours every tick (20t), and the instant it can afford a haul it
spends the cost and starts **drilling** — a fixed `cycleTicks` duration that has to run its course
regardless of how much power arrives after that. This is deliberate: without a duration separate
from the power-pull rate, a mine sitting on a full battery would produce every single tick, and a
single mine of any tier would be all a player ever needed. Because completion is capped at once per
`cycleTicks` no matter how much power is banked, surplus power beyond that rate is wasted on a lone
mine — the reason a second mine of the same tier earns its keep is that it puts that surplus to
work as genuine parallel throughput instead. A fully powered mine chains hauls back to back with no
idle tick in between; a starved one just waits between them.

If a Conveyor Belt sits on any of the mine's six faces, a completed haul drops straight onto it
instead of into the world — no falling through the air first. Several belts attached at once share
the output round-robin, one haul per belt in turn, rather than one belt taking every haul. A mine
with no belt attached anywhere falls back to the original behaviour: the ore drops loose at
`+0.5, +1.5, +0.5`, the middle of the block directly above the mine, for a hopper or a player to
collect.

A drill in progress **eats the ore block in the pit away**, a step at a time, so a long cycle reads
as work happening rather than as a silent countdown. See
[Two states, and what "digging" means](#two-states-and-what-digging-means) for the stages.

The tiers differ only in what they dig, what a haul costs and how long the cycle takes:

| Mine | Output | Storage | Power per haul | Cycle | Small Solar Panels for full-rate hauling |
|:--|:--|--:|--:|--:|--:|
| Coal | `COAL` | 10 | 2 | 200t (10s) | 60 |
| Iron | `RAW_IRON` | 20 | 5 | 300t (15s) | 100 |
| Redstone | `REDSTONE` | 20 | 5 | 300t (15s) | 100 |
| Gold | `RAW_GOLD` | 30 | 8 | 400t (20s) | 120 |
| Emerald | `EMERALD` | 50 | 14 | 600t (30s) | 140 |
| Diamond | `DIAMOND` | 60 | 18 | 800t (40s) | 135 |
| Netherite | `ANCIENT_DEBRIS` | 100 | 30 | 1000t (50s) | 180 |

Cycles were widened to 5x their original length (Coal 2s→10s, Netherite 10s→50s) specifically so a
single mine's throughput ceiling stops being "enough for anyone" — see the drilling explanation
above. That drop in required power/tick is also why the panel counts above fell by the same 5x
(a mine now needs a fifth as much sustained power to hit its own, now-slower, full rate).

A [Small Solar Panel](../small-solar-panel/README.md) generates 2 power per full daytime — one
Coal haul, roughly every 10 real-life minutes. That's the deliberate baseline: one panel is a
trickle charge, not a power source for actually running a mine. Hauling any tier at its full cycle
rate takes a large bank of panels (or a higher-tier generator, once one exists), not a handful.

All seven share `Mine`, an abstract `PowerBlock` in `utility/block`. A subclass supplies only its
block id, cycle length, haul cost and output material — there is no per-mine behaviour.

A mine is a **sink only**: it overrides `canOutputToward` to refuse. Without that it would be
listed as a network source as well, and on a shared run one mine could siphon another's buffer a
unit at a time — a netherite mine banking 29 of the 30 it needs, drained by a coal mine next door,
never completing a bore.

## Two states, and what "digging" means

The chart draws two states per mine: **idle / no power** and **digging**. They are one CraftEngine
block definition, the same shape the factories use — but with a `stage` **int** (1–5) rather than a
`powered` boolean, because a bore now takes 200–1000 ticks and has to read as progress rather than
as a light switch. Stage 1 is idle; stages 2–5 are the four steps of a drill, and across them the
ore block in the pit shrinks from half a cell to an eighth, staying centred on the pit floor, then
snaps back to whole when the haul drops. Stage 2 is the ore still whole but lit, so committing a
haul reads as the ore coming alive and everything after it as the ore being consumed.

That works out to twenty appearances per mine — five stages × four facings — all forced
`state: barrier` with an `entity_renderer`, so none of them claims a slot from the exhausted
auto-state pools. `Mine.DIGGING_STAGES` and the configs' `range: 1~5` have to agree; `MineTest`
pins them together, and pins that the ore actually shrinks at every step.

**This is the second attempt.** The first faked the vanilla break-progress crack texture over the
mine's own block with Paper's `Player.sendBlockDamage(Location, Float, Int)`, climbing from bare to
fully cracked across the cycle. It could never have worked, and nothing said so: the client draws a
crack by re-tessellating the *block model* at that position with the crumbling texture, and a
barrier's render shape is `INVISIBLE`, so there is no model to crumble. The packets went out every
tick and rendered nothing. **No entity-rendered Atlas block can use `sendBlockDamage`** — which is
all of them but the fluid pipe and the power cable. Anything that has to show progress has to show
it in the appearance itself.

**The digging state means "this tick completed a haul", not "this block holds some charge."** The
inherited `updatePoweredState()` answers the latter, which for a mine is a lie whenever it is fed
too slowly: a netherite mine costs 30 a haul, so a trickle would leave it sitting at 1–29 power
looking like it was cutting, with its ore lit, while producing nothing. `Mine` sets the property
from affordability instead, so a starved mine reads as idle.

## The rim model

`models/block/custom/mine_gantry.json` is shared by every appearance and is generated by
[`build_gantry.py`](build_gantry.py).

The machine is now just **a rim around a pit**: a full-height rim cut away above y 12, and a floor
dropped to y 6. Nothing else. The ore itself is a separate `block_display` element in the block
config (see [Telling the mines apart](#telling-the-mines-apart)), sitting visibly in the pit — that
is the whole read: an open pit with ore in it.

This went through two more elaborate versions first, both retired for the same failure mode. The
original suspended a head from a bridge on rails between four legs towering up to y=25 for a
crane-like silhouette; a follow-up compressed that into a compact "drill press" housing over a short
set of legs, all kept at or below y=16 so a Conveyor Belt on top of the mine — since
[mines hand hauls straight to an attached belt](#what-a-mine-does) — would not clip through it.
Both had thin, tightly-nested parts (legs, rails, a housing sunk half a pixel into them) that
**z-fought in game** even though every pair passed `MineTest`'s coincident-face check — that check
catches boxes sharing an exact plane, not the near-miss gaps that still flicker at in-game render
distances. Cutting the machine geometry entirely removes the surface for that to happen on.

**The rim runs full height on all four sides** because a power cable's hub and arms occupy y 4–12,
and a machine that stops short of that band leaves the cable joining onto thin air. It is cut away
above 12 so the pit and the ore in it stay visible. `MineTest` pins the band.

**No two boxes may share a face plane in the same direction.** Two such faces draw at the same
depth and the surface flickers. Opposite-facing coincident faces are fine, because backface culling
drops one of them. So the rim panels tile edge to edge with no overlapping area. `MineTest` walks
every pair of elements and fails on any shared `from`/`from` or `to`/`to` plane where the boxes also
overlap across the other two axes - necessary, but per the paragraph above, not sufficient on its
own to catch every kind of glitch.

Because an open rim is nowhere near a full cube, the block sets `can-occlude: false` and
`is-view-blocking: false`; otherwise neighbours cull their faces against it and the world shows
through. Nothing leaves the cell, so no `entity_culling` override is needed.

## Telling the mines apart

The hardware is identical across all seven. What distinguishes them is the **ore block sitting in
the pit**: `entity_renderer` takes a *list* of elements, and one element type is
`block_display`, so each mine renders a genuine `minecraft:iron_ore` / `diamond_ore` /
`ancient_debris` — not painted art — at half scale, centred in the pit.

While digging, that block renders at `block_light: 15`, so a working mine reads from across a base;
idle, it goes dark.

The mine renders the rim and the held ore, and nothing else. An earlier pass also hung the *item*
the mine drops over an output chute, which said the same thing twice; a later pass hung a whole
machine over the pit, which turned out to glitch (see [above](#the-rim-model)).

`MineTest` derives the expected ore block from each block class's own `output` material, so
changing a mine's output without changing its config fails the suite.

## Facing

Mines are still `DIRECTIONAL_OPPOSITE` — the block turns to face back toward whoever placed it —
but now that the model is a symmetric rim with no chute, that rotation has **no visible effect**.
It is kept because `Mine.direction` is purely cosmetic infrastructure shared with every other
utility block (see the class doc in `Mine.kt`: "a mine draws power from any side and drops ore
[wherever a belt is attached, or] straight up"), not because the current model needs it. If a
future model reintroduces a directional feature, the twenty appearances per mine (four facings ×
five `stage` values) are already wired up to take it; until then, all four facings of a given mine
render identically.

Nothing in the renderer is off centre, so **yaw alone carries the rotation**. That is worth knowing
before adding anything: `position` is an absolute offset from the block corner and `yaw` turns a
display *in place* without orbiting it, so any off-centre element would need its own position per
facing or it would be stranded inside the machine on three of the four rotations.

## Art

Idle art is **shared across all seven mines**: `mine_deck`, `mine_side`, `mine_bottom`. The chart
draws every idle rig in the same charcoal, and only the digging state carries colour.

Digging art is per mine — `{id}_deck_digging` and `{id}_side_digging` — where the bore and the vent
row ignite in that ore's colour:

| Mine | Glow |
|:--|:--|
| Coal | ember orange-red `255,107,44` |
| Iron | molten orange `255,154,46` |
| Redstone | electric red `255,32,32` |
| Gold | gold `255,215,0` |
| Emerald | green `44,255,140` |
| Diamond | pale cyan `77,232,255` |
| Netherite | magenta-violet `224,64,251` |

The mining system's energy colour is **per ore**, not one colour for the whole system — unlike
power (amber) and transport (green). The chart is explicit about this.

`mine_deck` is worth a note: its centre is a dark bore ringed by a steel collar, and its digging
variants glow out of that bore. It is mapped onto the excavation floor, so the cut under the ore is
what lights up.

All eighteen textures were generated with the Artlist MCP (Nano Banana Pro) at 2K and downscaled to
512×512, deriving every face and state by image-to-image from the first deck so the set stays
consistent.

**UV notes.** `mine_side` clads the rim and is composed of horizontal bands with no tall focal
feature, so it survives being sampled at several different heights.

## Rejected silhouettes

Five designs were rejected before this one, and the reasons keep recurring:

1. *A four-legged lattice mast with a box on top* — the retired Iron Mine's exact trap. A straight
   lattice tower reads as a **watchtower**, never a mine.
2. *A fat screw with five broad flights* — the flights were 12 wide and 1.5 thick, so it read as
   **a stack of plates on a crate**.
3. *A bit hung inside a mast with a crown* — the cage won and **the bit hid behind its own
   supporting structure**.
4. *A long twisting auger cone* — the most legible of the four as a drill, and still rejected.
5. *A timbered shaft mouth* — low and readable, and still not what was wanted.

Four of those five were variations on "tall machine on a pad", which turned out to be the wrong
premise rather than the wrong execution. What settled it was rendering ten genuinely different
archetypes as one contact sheet at a common scale and picking by number — the gantry was option 6.
Judge a silhouette from a render, never from the JSON.

## No GUI icons

The mines have no painted inventory icon — the model renders in the slot as it does for the
factories. If the in-hand look reads badly, seven `item/custom/{id}` icons
wired through a `minecraft:select` on `display_context` are the fix.
