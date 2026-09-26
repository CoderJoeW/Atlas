"""Writes the Atlas Goggles' worn model into the resource pack.

usage: python3 docs/design/atlas-goggles/build_model.py

Model space: a head-slot item is drawn at 0.625 of a block, so the player's 8px head fills
x/y/z 1.6..14.4 of the 16-unit model. The front of the model (north, low z) faces forward, as a
carved pumpkin's face does. Steve's eyes are centred at y 7.2 and x 4.8 / 11.2, and the lens
housings are centred on them.

Textures are 512px across 16 UV units, so faces use UV spans equal to their size and the
material keeps one scale everywhere. The strap samples a 2-unit band centred on the strap
texture's rivet row at v 2.38.
"""

import json
from pathlib import Path

PACK = Path("src/main/resources/atlas/resourcepack/assets")
MODEL = PACK / "minecraft/models/item/custom/atlas_goggles_worn.json"

HEAD_LO, HEAD_HI = 1.6, 14.4
EYE_Y = 7.2
LEFT_EYE_X, RIGHT_EYE_X = 4.8, 11.2
LENS = 5.0          # housing width and height
DEPTH = 2.0         # housing depth, out from the face
PLATE = 0.25        # lens plate thickness
STRAP_H, STRAP_T = 2.0, 0.5
RIVET_V = 2.38


def housing(w, h, du, dv):
    return {"texture": "#housing", "uv": [du, dv, du + w, dv + h]}


def strap(length, height=STRAP_H):
    v0 = RIVET_V - STRAP_H / 2
    return {"texture": "#strap", "uv": [0, v0, length, v0 + height]}


def box(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


def solid(frm, to, du, dv):
    """A box of housing material with all six faces, each sampling a patch its own size."""
    w, h, d = (to[i] - frm[i] for i in range(3))
    return box(frm, to, {
        "north": housing(w, h, du, dv),
        "south": housing(w, h, du + 1, dv),
        "east": housing(d, h, du + 2, dv),
        "west": housing(d, h, du + 3, dv),
        "up": housing(w, d, du, dv + 1),
        "down": housing(w, d, du + 1, dv + 1),
    })


def lens_housing(cx):
    x0, x1 = cx - LENS / 2, cx + LENS / 2
    y0, y1 = EYE_Y - LENS / 2, EYE_Y + LENS / 2
    z0, z1 = HEAD_LO - DEPTH, HEAD_LO
    cup = solid([x0, y0, z0], [x1, y1, z1], 1, 1)
    del cup["faces"]["south"]  # against the face, never seen
    # the lens sits proud of the housing; its chamfered corners are transparent, so the housing
    # shows through them as the bezel's cut corners
    plate = box([x0, y0, z0 - PLATE], [x1, y1, z0], {
        "north": {"texture": "#lens", "uv": [0, 0, 16, 16]},
        "east": housing(PLATE, LENS, 12, 1),
        "west": housing(PLATE, LENS, 13, 1),
        "up": housing(LENS, PLATE, 1, 13),
        "down": housing(LENS, PLATE, 1, 14),
    })
    return [cup, plate]


def strap_run(frm, to, faces_long, faces_edge):
    length = max(t - f for f, t in zip(frm, to))
    faces = {f: strap(length) for f in faces_long}
    faces.update({f: strap(length, STRAP_T) for f in faces_edge})
    return box(frm, to, faces)


def elements():
    out = lens_housing(LEFT_EYE_X) + lens_housing(RIGHT_EYE_X)
    inner_l, inner_r = LEFT_EYE_X + LENS / 2, RIGHT_EYE_X - LENS / 2
    outer_l, outer_r = LEFT_EYE_X - LENS / 2, RIGHT_EYE_X + LENS / 2

    # bridge over the nose
    out.append(solid([inner_l, EYE_Y - 0.5, 0.0], [inner_r, EYE_Y + 0.5, 1.2], 4, 12))

    # clasps join the strap to the outer side of each housing
    sy0, sy1 = EYE_Y - STRAP_H / 2, EYE_Y + STRAP_H / 2
    for x0, x1 in ((HEAD_LO - STRAP_T - 0.1, outer_l), (outer_r, HEAD_HI + STRAP_T + 0.1)):
        out.append(solid([x0, sy0 - 0.3, -0.1], [x1, sy1 + 0.3, 1.4], 2, 3))

    # strap: both sides and the back, just outside the head
    out.append(strap_run([HEAD_LO - STRAP_T, sy0, 1.4], [HEAD_LO, sy1, HEAD_HI + STRAP_T],
                         ("west", "east"), ("up", "down")))
    out.append(strap_run([HEAD_HI, sy0, 1.4], [HEAD_HI + STRAP_T, sy1, HEAD_HI + STRAP_T],
                         ("east", "west"), ("up", "down")))
    out.append(strap_run([HEAD_LO, sy0, HEAD_HI], [HEAD_HI, sy1, HEAD_HI + STRAP_T],
                         ("south", "north"), ("up", "down")))
    return out


def rounded(elems):
    for e in elems:
        e["from"] = [round(v, 3) for v in e["from"]]
        e["to"] = [round(v, 3) for v in e["to"]]
        for face in e["faces"].values():
            face["uv"] = [round(v, 3) for v in face["uv"]]
            assert all(0 <= v <= 16 for v in face["uv"]), face
        assert all(-16 <= v <= 32 for v in e["from"] + e["to"]), e
    return elems


def held(yaw, translation):
    return {"rotation": [0, yaw, 0], "translation": translation, "scale": [0.45] * 3}


def model():
    return {
        "textures": {
            "particle": "minecraft:item/custom/atlas_goggles_housing",
            "housing": "minecraft:item/custom/atlas_goggles_housing",
            "lens": "minecraft:item/custom/atlas_goggles_lens",
            "strap": "minecraft:item/custom/atlas_goggles_strap",
        },
        "elements": rounded(elements()),
        "display": {
            "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "thirdperson_righthand": held(180, [0, 1, -2]),
            "thirdperson_lefthand": held(180, [0, 1, -2]),
            "firstperson_righthand": held(160, [1, 3, 0]),
            "firstperson_lefthand": held(200, [-1, 3, 0]),
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
        },
    }


if __name__ == "__main__":
    worn = model()
    MODEL.write_text(json.dumps(worn, indent=2) + "\n")
    print(f"wrote {MODEL} ({len(worn['elements'])} elements)")
