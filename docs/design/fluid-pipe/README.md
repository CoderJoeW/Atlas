# Fluid Pipe — why it stays on real vanilla block states

The fluid pipe and the power cable are the two blocks that still use real vanilla auto-state
pools instead of the forced-`barrier` + `entity_renderer` pattern everything else uses.
[[entity-renderer-default]] records the exception but not the reason. This is the reason.

## entity_renderer cannot tile across block boundaries

`entity_renderer` draws a **display entity**, not a block model sitting in the world grid. For a
self-contained block that is invisible as a problem — a mine, a factory or a battery slightly
offset or scaled inside its own cell still reads correctly.

A pipe's entire visual job is the opposite: its arms have to meet the neighbouring pipe's arms
**exactly** at the shared block face. Any offset or scale difference in the display-entity
transform lands precisely on the joints, so every connection in a run looks wrong even though
each individual pipe looks fine on its own.

This was tried and reverted (2026-09-12): all 192 appearances were migrated to
`state: barrier` + `entity_renderer`, the configs loaded with no warnings at all, and the
in-game result was pipe runs whose connections did not line up. **Do not migrate the pipe or the
cable off real block states**, however tempting it is when a pool fills up.

## The capacity wall is usually stale cache, not a real shortage

The pipe needs 192 appearances (64 connection shapes × 3 fluid states), hand-assigned across
`cave_vines` / `weeping_vines` / `twisting_vines` / `tripwire` / `leaves`. When the server starts
refusing allocations —

```
The visual state group 'cave_vines' has reached its maximum capacity of '96' slots
and cannot allocate a state for 'atlas:fluid_pipe[appearance=c000010_water]'
```

— the instinct is to move that one appearance to another pool. **That does not converge.** Each
move just pushes the failure onto the next appearance in line, because the pool was not actually
short: `plugins/CraftEngine/cache/visual_block_states.json` was still holding a reservation for
every appearance from previous loads, and those duplicates were consuming the pool.

Use [`prune_cache.py`](prune_cache.py) with the server stopped - it drops one block's
reservations, and with no block id it just reports what is held and by whom:

```bash
python3 docs/design/fluid-pipe/prune_cache.py run/plugins/CraftEngine/cache/visual_block_states.json
python3 docs/design/fluid-pipe/prune_cache.py run/plugins/CraftEngine/cache/visual_block_states.json atlas:fluid_pipe
```

Dropping an appearance's cached entry is also **required whenever its `auto-state` group
changes** - the cached assignment wins, so a rebalance silently does nothing until it is pruned.

## Real pool capacities

**An appearance is not always one slot.** `cave_vines` refuses with "maximum capacity of '96'
slots", but an appearance there costs two states (`cave_vines` + `cave_vines_plant`), so it fits
**48**. Reading that 96 as appearances overshoots by double - which is how the pipe ended up 22
appearances over a pool that was already exactly full at 48.

The original config was over by **2**: it put 50 on `cave_vines` against a real ceiling of 48, and
55 on `tripwire`, which with the 7 another pack holds landed exactly on its 126 ceiling with no
margin at all.

**Most of the pool pressure is Atlas's own**, not other packs - the power cable alone holds ~64
tripwire slots. When measuring what is free, count only reservations whose owning block id is not
`atlas:`; treating every reservation on a host as foreign understates Atlas's own budget just as
badly as ignoring other packs overstates it. `AutoStateBudgetTest` now subtracts the measured
foreign share, so it fails instead of going green on a config the server will refuse.
