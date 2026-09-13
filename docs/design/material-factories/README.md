# Material Factories — Twin-Tank Mixer

Cobblestone and Obsidian Factory are **plain full-cube blocks** (`minecraft:block/cube`
generation) — not a custom model. Each gets its own texture set, clad in its own output material
(rough grey cobblestone plating vs. glossy black-violet obsidian plating), so the two read as
distinct machines on sight rather than needing the tooltip to tell them apart. Neither the model
nor the generated art spells out either name anywhere - that's the tooltip's job alone.

Built from the silhouette review in this folder: ten archetypes rendered as
[`renders/silhouette-options.png`](renders/silhouette-options.png), option 1 (Twin-Tank Mixer)
picked, refined through four proportion variants
([`renders/twin-tank-iterations.png`](renders/twin-tank-iterations.png)), and a colour-language
pass ([`renders/texture-mockup.png`](renders/texture-mockup.png)).

**A custom multi-box hull (`build_hull.py`) was built and then dropped.** It modelled the mixer
as real geometry - two tanks, a backplate, a reactor stack - and passed every test, including a
coincident-face check mirroring `MineTest`'s. It's kept in this folder as a record of that path,
but the block ships as a full cube: the shape is now carried entirely by the four side textures
below, painted to read as the same twin-tank scene without any geometry.

## What it looks like

Every side face (north/south/east/west) shows the same flat, head-on illustration: the block's own
housing plating with two round gauge-window portholes side by side (left = water, right = lava),
though the block has no facing and looks the same from any side. Top and bottom reuse the plain
housing panel. No lettering, numbers, or logos are painted into any of it - the tooltip name is the
only place either name appears.

**Cobblestone Factory** is clad in rough grey fitted-stone plating (`#8A8A8A`-`#6E6E6E`), matching
its gradient item name. **Obsidian Factory** is clad in glossy black-violet volcanic-glass plating
(`#1A0A2E` base, `#4B0082` sheen). Both share the exact same porthole bezel/bolt style and the same
functional fluid colours - the housing material is what tells them apart, not the mechanism.

## Three lamps, eight states

The block carries one lamp per ingredient, each reporting only itself:

| Lamp | Where | Lit when |
|---|---|---|
| Water porthole | left window, side faces | a unit of water is banked |
| Lava porthole | right window, side faces | a unit of lava is banked |
| Power bar | recessed amber segment bar below the portholes | `currentPower >= powerCost` |

That is three booleans — `water`, `lava`, `powered` — so eight combinations, eight appearances and
eight display items per factory, all forced to `state: barrier` with an `entity_renderer` so none
of them claims a slot from the auto-state pools. `MaterialFactory.renderLamps()` sets all three in
one `setBooleanProperties` call, guarded so the block is only re-placed when a lamp actually
changes.

The power lamp is a **bar**, not a third porthole, deliberately: power-amber and lava-orange are
close enough that three lit circles would read as one gauge cluster. Different shape, different
meaning. It echoes the Small Battery's segment readout.

Power is gated on *affording a haul*, not `hasPower()` — a trickle-fed obsidian factory (25/haul,
50 max) would otherwise read as ready for most of the time it spends filling up.

**An earlier version of this document claimed there was no persisted "water only" state, because
fluid was consumed within a single tick.** That stopped being true when the factories moved to
push-fed fluid: `MaterialFactory` now banks each unit as it lands and holds it until the other
fluid and the power are both there, so "water in, waiting on lava" is a state the block can sit in
indefinitely. The four-way split in the colour mockup turned out to describe real states after all.

Banked fluid is persisted alongside `currentPower` (`PowerBlockPersistence`), so a restart puts
back a half-filled machine rather than silently eating a unit the pump already spent power to
lift.

## Textures

Two housing panels plus eight face variants per factory, all Artlist / Nano Banana, 1K generated
then downscaled to 512×512. The housing panel (top/bottom) is unchanged; the face set is keyed by
which lamps are lit:

| Face suffix | Water | Lava | Power |
|---|---|---|---|
| `_idle` | dark | dark | dark |
| `_water` | lit | dark | dark |
| `_lava` | dark | lit | dark |
| `_water_lava` | lit | lit | dark |
| `_powered` | dark | dark | lit |
| `_water_powered` | lit | dark | lit |
| `_lava_powered` | dark | lit | lit |
| `_water_lava_powered` | lit | lit | lit |

Named `material_factory_<cobblestone|obsidian>_face_<suffix>.png`, plus
`material_factory_<material>_housing.png`.

**How the set is built:** the `_idle` face is re-generated first as a single i2i edit of the old
idle face, adding the unlit amber bar bezel below the portholes. Every other combination is then a
separate *additive* i2i edit **from that one unlit base, never chained** — the shared source keeps
the plating and bezels identical and avoids the drift that chaining introduces, the same way the
Small Battery's charge ramp was built. Each edit's prompt names only the lamps it lights and
instructs the model to hold everything else pixel-identical.

The retired `_active` faces are superseded by `_water_lava_powered`.

Each housing panel was generated once from a text prompt. Each idle face is a multi-reference i2i
composing that housing with the porthole bezel (a shared reference generation - the bezel style is
identical for both, only the plating behind it differs). Each active face is then a further i2i
edit of its own idle face: left porthole lit water-blue, right lit molten-orange, everything else
held pixel-identical.

An earlier pass shared one housing/face set between the two blocks and used generic "material
factory" naming throughout, on the idea that the two machines should look the same. That read as
under-differentiated in review - a player shouldn't need the tooltip to tell them apart - so the
housing generation is now factory-specific and the porthole composite/lit-edit chain runs once per
material instead of once overall. (Four more textures - a reactor-glow panel and a protruding-
window set - were generated earlier still, for a custom hull model that was dropped in favour of
a plain cube; they're no longer shipped.)

## Recipe / cost (unchanged from the pre-rework blocks)

| | Cobblestone Factory | Obsidian Factory |
|---|---|---|
| Storage | 4 | 50 |
| Power/haul | 2 | 25 |
| Output | Cobblestone | Obsidian |
| Recipe | 3 iron + 3 redstone + 1 cobblestone | 3 iron + 3 redstone + 1 obsidian + 1 diamond |
