"""Generates the shared mine model: a rimmed pit with the ore sitting in the middle.

The ore being cut is NOT in this model - it is a `block_display` element in the block config, so
each tier shows its own ore block (iron_ore, diamond_ore, ancient_debris...) sitting in the pit.
The excavation is sized around that block: scale 0.5 at position 0.25/0.375/0.25 puts it at
x/z 4-12, y 6-14, which is what the rim is cut away to reveal.

This is deliberately just a rim and a floor - no legs, housing, bit or chute. Two earlier versions
put a machine over the pit (a tall gantry suspended on rails, then a compact drill-press housing);
both had thin, tightly-nested geometry that z-fought in game even though it passed every unit-level
coincident-face check here. The ore block sitting visibly in an open pit reads fine on its own and
has nothing left to glitch.
"""
import json, sys

RIM, TOP = 3.0, 12.0         # rim thickness, and its top - the cable band is y 4-12
FLOOR = 6.0                  # excavation floor, which the ore block stands on


def r3(v):
    return [round(float(c), 3) for c in v]


def box(name, f, t, tex, uvs=None):
    e = {"name": name, "from": r3(f), "to": r3(t), "faces": {}}
    for face in ("north", "south", "east", "west", "up", "down"):
        e["faces"][face] = {"texture": (uvs or {}).get(face + "_tex", tex),
                            "uv": (uvs or {}).get(face, [0, 0, 16, 16])}
    return e


LEDGE = {"up": [0, 2, 16, 6], "down": [0, 2, 16, 6]}

E = []

# The rim runs full height on all four sides so a power cable's hub and arms (y 4-12) always meet
# solid metal, and it is cut away above 12 so the pit and the ore in it stay visible.
E.append(box("rim_west", [0, 0, 0], [RIM, TOP, 16], "#side", LEDGE))
E.append(box("rim_east", [16 - RIM, 0, 0], [16, TOP, 16], "#side", LEDGE))
E.append(box("rim_north", [RIM, 0, 0], [16 - RIM, TOP, RIM], "#side", LEDGE))
E.append(box("rim_south", [RIM, 0, 16 - RIM], [16 - RIM, TOP, 16], "#side", LEDGE))
# Pit floor. Takes the deck texture, whose centre is a dark bore that glows while digging, so the
# floor under the ore lights up.
E.append(box("cut_floor", [RIM, 0, RIM], [16 - RIM, FLOOR, 16 - RIM], "#side",
             {"up_tex": "#deck", "up": [0, 0, 16, 16], "down_tex": "#bottom"}))

json.dump({"textures": {"particle": "#side"}, "elements": E}, open(sys.argv[1], "w"), indent=2)
print(f"{len(E)} elements")
