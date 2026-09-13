"""Generates the shared material-factory model: twin fluid tanks flanking a reactor stack.

Approved direction: docs/design/material-factories/README.md. Both Cobblestone and Obsidian
Factory place the same hull - they differ only in recipe and output, same split as the mines.

Coordinates are real Minecraft model space (0-16 per axis, +X east, +Z south, +Y up) so this is
the actual in-game geometry, not a mockup. The layout is deliberately connected end to end so
nothing floats:

  tank (south, z9-14) -- pipe bridge (z7-9) -- backplate (z2-7, y0-13) -- reactor (rises from
  the backplate's top, y13-16)

Two tanks flank the reactor in x (west = water, east = lava). The backplate is a full-width,
full-cable-band wall behind everything, tall enough (y0-13) that a Power Cable meets solid
material on any side, and each tank's own body covers the west/east faces directly. A gauge
window sits on each tank's south face, protruding outward (never flush - see the z-fight note
below) so it can carry its own texture independent of the tank body.

No two elements share a same-direction face plane over an overlapping cross-section - the
z-fighting trap MineTest guards against. Every touching pair here is opposite-facing (one grows
+, the other -, at the same boundary), which is safe; run() checks this the same way MineTest
does, as a script-level guard rather than a duplicated Kotlin test.

Run: python3 build_hull.py <output.json>
"""
import json
import sys

WEST_TANK = ([1, 0, 9], [6, 15, 14])
EAST_TANK = ([10, 0, 9], [15, 15, 14])
BACKPLATE = ([1, 0, 2], [15, 13, 7])
REACTOR = ([6, 13, 2], [10, 16, 6])
WEST_PIPE = ([2, 11, 7], [5, 12.5, 9])
EAST_PIPE = ([11, 11, 7], [14, 12.5, 9])
WEST_WINDOW = ([2.5, 6, 14], [4.5, 11, 14.6])
EAST_WINDOW = ([11.5, 6, 14], [13.5, 11, 14.6])


def r3(v):
    return [round(float(c), 3) for c in v]


def box(name, f, t, tex, uv_overrides=None):
    e = {"name": name, "from": r3(f), "to": r3(t), "faces": {}}
    for face in ("north", "south", "east", "west", "up", "down"):
        e["faces"][face] = {
            "texture": (uv_overrides or {}).get(face + "_tex", tex),
            "uv": (uv_overrides or {}).get(face, [0, 0, 16, 16]),
        }
    return e


def build():
    return [
        box("west_tank", *WEST_TANK, "#housing"),
        box("east_tank", *EAST_TANK, "#housing"),
        box("backplate", *BACKPLATE, "#housing"),
        box("reactor", *REACTOR, "#reactor"),
        box("west_pipe", *WEST_PIPE, "#housing"),
        box("east_pipe", *EAST_PIPE, "#housing"),
        # gauge windows: only the outward (south) face carries the fluid texture; the other
        # five faces of this thin protruding box are barely visible and reuse #housing.
        box("west_window", *WEST_WINDOW, "#housing", {"south_tex": "#window_water"}),
        box("east_window", *EAST_WINDOW, "#housing", {"south_tex": "#window_lava"}),
    ]


def overlaps_1d(a0, a1, b0, b1):
    return a0 < b1 and b0 < a1


def check_coincident_faces(elements):
    """Same guard MineTest runs: fail on two elements sharing a from/from or to/to plane along
    one axis where their footprint also overlaps across the other two axes - a same-direction
    coincidence that would z-fight. Opposite-facing (from/to) matches are fine and not checked."""
    problems = []
    for i, a in enumerate(elements):
        for b in elements[i + 1 :]:
            for axis in range(3):
                for key in ("from", "to"):
                    if a[key][axis] != b[key][axis]:
                        continue
                    others = [ax for ax in range(3) if ax != axis]
                    if all(
                        overlaps_1d(a["from"][ax], a["to"][ax], b["from"][ax], b["to"][ax])
                        for ax in others
                    ):
                        problems.append((a["name"], b["name"], axis, key))
    return problems


def run(path):
    elements = build()
    problems = check_coincident_faces(elements)
    if problems:
        for a, b, axis, key in problems:
            print(f"COINCIDENT FACE: {a} / {b} share a {key} plane on axis {axis}")
        raise SystemExit(1)
    json.dump({"textures": {"particle": "#housing"}, "elements": elements}, open(path, "w"), indent=2)
    print(f"{len(elements)} elements, no coincident-face conflicts -> {path}")


if __name__ == "__main__":
    run(sys.argv[1] if len(sys.argv) > 1 else "material_factory_hull.json")
