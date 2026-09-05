# Conveyor Belt — visual design

The belt follows
[`references/transport-system-reactive-states.jpg`](references/transport-system-reactive-states.jpg),
supplied 2026-09-04.

## Direction

Charcoal armoured housing with brushed steel side rails, a drum at each end, and a mesh panel
along the lower flank. The belt surface carries a run of chevrons pointing the way items travel.

**Transport energy is green.** The power system glows amber; the housings are the same charcoal, so
the glow colour is what separates the two systems at a glance. The belt's previous top texture used
*gold* chevrons, which read as a power block and is why it is being replaced.

## States

| State | Property | Reads as |
|---|---|---|
| Empty / Idle | `running=false` | Chevrons unlit — dark, embossed relief only |
| Loaded / Running | `running=true` | The same chevrons ignite green and throw light onto the rails |

`running` is set by `ConveyorBelt.transportUpdate()` from whether anything is riding the belt, and
written on the start/stop edge only.

## Facing

The chart's north/south/east/west variants are the *same art rotated*. The block is entity-rendered,
so all four are one model spun with `yaw` rather than four pre-rotated textures. Two states × four
facings = eight appearances sharing one forced barrier state, costing **zero** auto-state slots.

An **unrotated** display entity shows the chevrons pointing *south*, not north, so each facing's yaw
is its compass angle plus a further 180°:

| `facing` | `yaw` |
|---|---|
| north | `180` |
| south | *(none)* |
| east | `-90` |
| west | `90` |

Items always travel along `facing` — that is also the side `containerAhead()` deposits into — so a
missing half turn does not misroute anything, it just draws the arrows pointing back down the belt
while the items run the other way. That was the state of the block until it was corrected.

Only the top texture reveals the error: the two ends carry identical art and both sides share one
texture, so a 180° turn of this model is invisible everywhere except the chevrons.

## Model and UVs

`assets/minecraft/models/block/custom/conveyor_belt_base.json` — a single element.

| | |
|---|---|
| Bounds | `[0, 0, 0] → [16, 6, 16]` |
| Height | 6px of 16 |
| Faces | `up` = `#up`, `down` = `#down` (cullface down), four sides at `uv: [0, 10, 16, 16]` |

**The side, front and back faces sample only the bottom 6/16 of their texture.** The top 10/16 is
never drawn. Art placed there is invisible — this is exactly what was wrong with the old front face,
whose detail sat in the unseen region while the visible band was a bare plate. The `up` and `down`
faces use default UVs and are drawn in full.

## Textures

| Face | Idle | Running |
|---|---|---|
| top | `conveyor_belt_top` | `conveyor_belt_top_running` (animated) |
| side (east/west) | `conveyor_belt_side` | `conveyor_belt_side_running` |
| front (north) | `conveyor_belt_front` | `conveyor_belt_front_running` |
| back (south) | `conveyor_belt_back` | `conveyor_belt_back_running` |
| bottom | `conveyor_belt_bottom` | *shared with idle* |

The underside is not lit in the chart and is barely visible in play, so both states share one
bottom texture.

The two ends are the same end-on elevation — the chart draws the belt symmetrically, with a drum at
each end — so front and back currently ship identical art under their two names. They are kept as
separate files rather than one shared name so either end can be differentiated later (an intake
grille, an output chute) without touching the config.

## Animation

Only the running top is animated — `conveyor_belt_top_running.png` is an 8-frame vertical strip
with `frametime: 2, interpolate: true`, so one cycle runs 16 ticks. The idle belt is a still.

**The chevrons are not scrolled.** Their pitch measures ~66.7px, which does not divide the 512px
texture, so a positional roll would drag a phase-mismatched seam down the belt every cycle — the
same tearing that killed the first attempt at the fluid pipe animation. Instead a chase travels
along the fixed chevrons: geometry, rails and bolts never move, and the loop closes exactly because
the wave's 128px spatial period divides 512 and 8 frames advance it one full period.

The glow is modulated **down** from full rather than added on top. The lit chevrons are already
near saturation, so adding light simply clipped and the chase was invisible. The floor is 55% of
full glow, which keeps every frame unmistakably lit against an idle belt that has none.
