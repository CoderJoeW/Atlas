"""Generates the shared mine model: a cutting gantry straddling an excavation.

The output chute faces +Z (south), matching the conveyor belt's yaw convention, so the four
facings are one model at yaw 0 / 180 / -90 / +90.

The ore being cut is NOT in this model - it is a `block_display` element in the block config, so
each tier shows its own ore block (iron_ore, diamond_ore, ancient_debris...) sitting in the jaws.
The excavation is sized around that block: scale 0.5 at position 0.25/0.375/0.25 puts it at
x/z 4-12, y 6-14, which is what the bit descends onto and what the rim is cut away to reveal.
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
POST = {f: [7, 0, 9, 16] for f in ("north", "south", "east", "west")}
BEAM = {f: [2, 7, 14, 9] for f in ("north", "south", "east", "west")}

E = []

# The rim runs full height on all four sides so a power cable's hub and arms (y 4-12) always meet
# solid metal, and it is cut away above 12 so the excavation and the ore in it stay visible.
E.append(box("rim_north", [0, 0, 0], [16, TOP, RIM], "#side", LEDGE))
E.append(box("rim_west", [0, 0, RIM], [RIM, TOP, 16 - RIM], "#side", LEDGE))
E.append(box("rim_east", [16 - RIM, 0, RIM], [16, TOP, 16 - RIM], "#side", LEDGE))
E.append(box("rim_south", [0, 0, 16 - RIM], [16, TOP, 16], "#side", LEDGE))
# Excavation floor. Takes the deck texture, whose centre is a dark bore that glows while digging,
# so the cut face under the ore lights up.
E.append(box("cut_floor", [RIM, 0, RIM], [16 - RIM, FLOOR, 16 - RIM], "#side",
             {"up_tex": "#deck", "up": [0, 0, 16, 16], "down_tex": "#bottom"}))

# Gantry legs standing on the rim, and the rails they carry.
for name, x, z in (("leg_nw", 1, 1), ("leg_ne", 12.5, 1), ("leg_sw", 1, 12.5), ("leg_se", 12.5, 12.5)):
    E.append(box(name, [x, TOP, z], [x + 2.5, 25, z + 2.5], "#tower", POST))
E.append(box("rail_west", [1, 23, 1], [3.5, 25, 15], "#tower", BEAM))
E.append(box("rail_east", [12.5, 23, 1], [15, 25, 15], "#tower", BEAM))
# The bridge the cutting head is slung from, running across the rails.
E.append(box("bridge", [1, 21.5, 6.5], [15, 24, 9.5], "#tower", BEAM))

# The cutting head, and the bit descending out of it onto the ore. Everything below the bridge is
# on the machine's centre line, directly above the block being cut - that alignment is what says
# the gantry is working rather than just standing there.
E.append(box("head", [5, 19, 5], [11, 21.5, 11], "#tower"))
E.append(box("drum", [5.5, 16.5, 5.5], [10.5, 19, 10.5], "#tower"))
E.append(box("bit_upper", [6.5, 15, 6.5], [9.5, 16.5, 9.5], "#tower"))
E.append(box("bit_tip", [7.25, 13.5, 7.25], [8.75, 15, 8.75], "#tower"))

# Output chute over the south rim, where the ore item is handed out.
E.append(box("chute", [5, TOP, 13], [11, 15, 16], "#side", {"up": [4, 4, 12, 8]}))

json.dump({"textures": {"particle": "#side"}, "elements": E}, open(sys.argv[1], "w"), indent=2)
print(f"{len(E)} elements")
