"""Renders the goggles' worn model, textured, on a stand-in Steve head.

usage: python3 docs/design/atlas-goggles/render_goggles.py <out.png>

An orthographic z-buffered rasteriser over the model JSON: each visible face samples its UV
rectangle through an affine map (exact under orthographic projection), keeps the nearest texel per
pixel, and is shaded the way Minecraft shades faces (up 1.0, north/south 0.8, east/west 0.6, down 0.5). It
is for checking fit and silhouette, not a substitute for looking at it in game.
"""

import functools
import json
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

from build_model import EYE_Y, HEAD_HI, HEAD_LO, LEFT_EYE_X, MODEL, PACK, RIGHT_EYE_X

TEXTURES = PACK / "minecraft/textures"
STEVE_EYE_W, STEVE_EYE_H = 3.2, 1.6  # two head pixels by one
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
PX_PER_UV = 32  # 512px textures across 16 UV units


@functools.cache
def texture(ref):
    return Image.open(TEXTURES / (ref.split(":")[1] + ".png")).convert("RGBA")


def corners(face, f, t):
    """Face corners as (top-left, top-right, bottom-left) in the texture's orientation."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        "north": ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        "south": ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        "east": ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
        "west": ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
        "up": ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        "down": ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
    }[face]


NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0),
           "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


class Camera:
    def __init__(self, yaw, pitch, scale, size):
        self.yaw, self.pitch, self.scale, self.size = math.radians(yaw), math.radians(pitch), scale, size

    def rotate(self, p):
        """A model-space point, turned about the model's centre."""
        return self.rotate_vector(tuple(v - 8 for v in p))

    def rotate_vector(self, v):
        x, y, z = v
        cy, sy = math.cos(self.yaw), math.sin(self.yaw)
        x, z = x * cy - z * sy, x * sy + z * cy
        cp, sp = math.cos(self.pitch), math.sin(self.pitch)
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z  # the camera looks along +z

    def screen(self, p):
        x, y, z = self.rotate(p)
        # looking along +z from the north, east (+x) is on the left
        return (self.size / 2 - x * self.scale, self.size / 2 - y * self.scale), z


def faces_of(elements, head):
    out = []
    for e in elements:
        for name, face in e["faces"].items():
            out.append((e["from"], e["to"], name, face))
    if head:
        f, t = [HEAD_LO] * 3, [HEAD_HI] * 3
        for name in NORMALS:
            out.append((f, t, name, {"colour": (168, 125, 98)}))
        # Steve's eyes, on the front of the head, a hair in front of it
        y0, y1, z = EYE_Y - STEVE_EYE_H / 2, EYE_Y + STEVE_EYE_H / 2, HEAD_LO - 0.01
        for cx in (LEFT_EYE_X, RIGHT_EYE_X):
            x0 = cx - STEVE_EYE_W / 2
            out.append(([x0, y0, z], [x0 + STEVE_EYE_W, y1, z], "north", {"colour": (60, 60, 150)}))
    return out


def render(elements, cam, head=True):
    """Draws every face that points at the camera, keeping the nearest texel per pixel."""
    size = cam.size
    colour = np.zeros((size, size, 4))
    zbuf = np.full((size, size), np.inf)
    ys, xs = np.mgrid[0:size, 0:size] + 0.5
    for f, t, name, face in faces_of(elements, head):
        n = cam.rotate_vector(NORMALS[name])
        if n[2] >= -1e-6:  # facing away from the camera
            continue
        tl, tr, bl = corners(name, f, t)
        (ptl, dtl), (ptr, dtr), (pbl, dbl) = (cam.screen(p) for p in (tl, tr, bl))

        if "colour" in face:
            crop = Image.new("RGBA", (1, 1), face["colour"] + (255,))
        else:
            tex = texture(face["texture_ref"])
            u0, v0, u1, v1 = (v * PX_PER_UV for v in face["uv"])
            crop = tex.crop((round(min(u0, u1)), round(min(v0, v1)), round(max(u0, u1)), round(max(v0, v1))))
            if u1 < u0:
                crop = crop.transpose(Image.FLIP_LEFT_RIGHT)
            if v1 < v0:
                crop = crop.transpose(Image.FLIP_TOP_BOTTOM)
        w, h = max(crop.width, 1), max(crop.height, 1)
        texels = np.asarray(crop.convert("RGBA")).astype(float)

        # screen = tl + s * (tr - tl) / w + t * (bl - tl) / h, solved for (s, t) at every pixel
        a = np.array([[(ptr[0] - ptl[0]) / w, (pbl[0] - ptl[0]) / h],
                      [(ptr[1] - ptl[1]) / w, (pbl[1] - ptl[1]) / h]])
        if abs(np.linalg.det(a)) < 1e-9:
            continue
        inv = np.linalg.inv(a)
        dx, dy = xs - ptl[0], ys - ptl[1]
        s_ = inv[0, 0] * dx + inv[0, 1] * dy
        t_ = inv[1, 0] * dx + inv[1, 1] * dy
        inside = (s_ >= 0) & (s_ < w) & (t_ >= 0) & (t_ < h)
        depth = dtl + s_ / w * (dtr - dtl) + t_ / h * (dbl - dtl)
        si = np.clip(s_.astype(int), 0, w - 1)
        ti = np.clip(t_.astype(int), 0, h - 1)
        sample = texels[ti, si]
        sample[..., :3] *= SHADE[name]
        win = inside & (sample[..., 3] >= 128) & (depth < zbuf - 1e-4)
        colour[win] = sample[win]
        colour[win, 3] = 255
        zbuf[win] = depth[win]
    return Image.fromarray(colour.astype(np.uint8))


def load():
    model = json.loads(MODEL.read_text())
    refs = model["textures"]
    for e in model["elements"]:
        for face in e["faces"].values():
            face["texture_ref"] = refs[face["texture"].lstrip("#")]
    return model["elements"]


if __name__ == "__main__":
    elements = load()
    size = 420
    views = [
        ("front three-quarter", Camera(-35, -20, 18, size), True),
        ("front", Camera(0, 0, 18, size), True),
        ("side", Camera(-90, -10, 18, size), True),
        ("rear three-quarter", Camera(-150, -25, 18, size), True),
        ("goggles alone", Camera(-35, -20, 18, size), False),
    ]
    sheet = Image.new("RGBA", (size * len(views), size + 30), (150, 160, 170, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (label, cam, head) in enumerate(views):
        sheet.alpha_composite(render(elements, cam, head), (i * size, 30))
        draw.text((i * size + 10, 8), label, fill=(20, 20, 20, 255))
    sheet.save(sys.argv[1])
    print("wrote", sys.argv[1])
