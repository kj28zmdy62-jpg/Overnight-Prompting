"""Shared helpers for the "Outside, but wrong" levels: animated textures, a pixel
font for signs, raw box models for custom block classes, and the cars that several
of those levels park in their lots and on their roads."""
import json
import os

import numpy as np

import assets as A
from assets import tex, shaped
import tex as T

S = 32

# ------------------------------------------------------------------ animated textures


def animated(name, frame_fn, frames, frametime=2, interpolate=False):
    """Register a texture whose frames are stacked vertically, with its .mcmeta sidecar.

    frame_fn(i) returns an rgb array or (rgb, alpha) for frame i. The sidecar is written
    while the texture is painted (after build.py has cleaned the texture folder).
    """
    def paint():
        rgbs, alphas, has_alpha = [], [], False
        for i in range(frames):
            out = frame_fn(i)
            if isinstance(out, tuple):
                rgbs.append(out[0])
                alphas.append(out[1])
                has_alpha = True
            else:
                rgbs.append(out)
                alphas.append(np.ones(out.shape[:2]))
        meta = os.path.join(A.ASSETS, "textures", "block", name + ".png.mcmeta")
        os.makedirs(os.path.dirname(meta), exist_ok=True)
        with open(meta, "w") as f:
            json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f)
        rgb = np.concatenate(rgbs, axis=0)
        return (rgb, np.concatenate(alphas, axis=0)) if has_alpha else rgb
    return tex(name, paint)


def sliced(prefix, fn, cols, rows):
    """Paint one big image (rows*32, cols*32) and register it as textures prefix_<col>_<row>.

    Row 0 is the top. Returns the list of names in row-major order.
    """
    cache = {}

    def big():
        if "img" not in cache:
            cache["img"] = fn()
        return cache["img"]

    names = []
    for r in range(rows):
        for c in range(cols):
            name = f"{prefix}_{c}_{r}"
            tex(name, lambda c=c, r=r: big()[r * S:(r + 1) * S, c * S:(c + 1) * S])
            names.append(name)
    return names


# ------------------------------------------------------------------ pixel font (3x5)

FONT = {
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
    "2": ["111", "001", "111", "100", "111"], "3": ["111", "001", "011", "001", "111"],
    "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "111", "001", "111"],
    "6": ["111", "100", "111", "101", "111"], "7": ["111", "001", "010", "010", "010"],
    "8": ["111", "101", "111", "101", "111"], "9": ["111", "101", "111", "001", "111"],
    "A": ["010", "101", "111", "101", "101"], "B": ["110", "101", "110", "101", "110"],
    "C": ["011", "100", "100", "100", "011"], "D": ["110", "101", "101", "101", "110"],
    "E": ["111", "100", "110", "100", "111"], "F": ["111", "100", "110", "100", "100"],
    "G": ["011", "100", "101", "101", "011"], "H": ["101", "101", "111", "101", "101"],
    "I": ["111", "010", "010", "010", "111"], "J": ["001", "001", "001", "101", "010"],
    "K": ["101", "101", "110", "101", "101"], "L": ["100", "100", "100", "100", "111"],
    "M": ["101", "111", "111", "101", "101"], "N": ["110", "101", "101", "101", "101"],
    "O": ["010", "101", "101", "101", "010"], "P": ["110", "101", "110", "100", "100"],
    "Q": ["010", "101", "101", "110", "011"], "R": ["110", "101", "110", "101", "101"],
    "S": ["011", "100", "010", "001", "110"], "T": ["111", "010", "010", "010", "010"],
    "U": ["101", "101", "101", "101", "111"], "V": ["101", "101", "101", "101", "010"],
    "W": ["101", "101", "111", "111", "101"], "X": ["101", "101", "010", "101", "101"],
    "Y": ["101", "101", "010", "010", "010"], "Z": ["111", "001", "010", "100", "111"],
    ".": ["000", "000", "000", "000", "010"], "-": ["000", "000", "111", "000", "000"],
    "?": ["111", "001", "010", "000", "010"], "!": ["010", "010", "010", "000", "010"],
    "'": ["010", "010", "000", "000", "000"], "/": ["001", "001", "010", "100", "100"],
    " ": ["000", "000", "000", "000", "000"], ":": ["000", "010", "000", "010", "000"],
}


def text_width(s, scale=1, gap=1):
    return len(s) * (3 * scale + gap) - gap


def draw_text(img, s, x, y, color, scale=1, gap=1, alpha=1.0):
    """Draw text into img (H,W,3) at pixel x,y (top-left). Returns the image."""
    out = img.copy()
    c = T.hexc(color) if isinstance(color, str) else np.asarray(color)
    h, w = out.shape[:2]
    for ch in s.upper():
        glyph = FONT.get(ch, FONT["?"])
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            px, py = x + gx * scale + sx, y + gy * scale + sy
                            if 0 <= px < w and 0 <= py < h:
                                out[py, px] = out[py, px] * (1 - alpha) + c * alpha
        x += 3 * scale + gap
    return out


def text_mask(s, shape, x, y, scale=1, gap=1):
    m = np.zeros(shape)
    img = draw_text(np.zeros(shape + (3,)), s, x, y, "#ffffff", scale, gap)
    m[img[..., 0] > 0.5] = 1
    return m


def centered(img, s, y, color, scale=1, gap=1, alpha=1.0):
    w = img.shape[1]
    return draw_text(img, s, (w - text_width(s, scale, gap)) // 2, y, color, scale, gap, alpha)


# ------------------------------------------------------------------ raw box models (for custom block classes)


def box_model(boxes, textures, particle=None):
    """Same element format as assets.shaped(), but only returns the model json."""
    elements = []
    for b in boxes:
        x1, y1, z1, x2, y2, z2, t = b[:7]
        faces = {}
        for f in ("down", "up", "north", "south", "west", "east"):
            key = t.get(f, t.get("all")) if isinstance(t, dict) else t
            if key is None:
                continue
            full = key.endswith("!")
            key = key.rstrip("!")
            faces[f] = {"texture": "#" + key, "uv": [0, 0, 16, 16] if full else A._uv(f, x1, y1, z1, x2, y2, z2)}
        el = {"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces}
        if len(b) > 7 and b[7]:
            axis, angle, origin = b[7]
            el["rotation"] = {"origin": list(origin), "axis": axis, "angle": angle}
        elements.append(el)
    tx = {k: A._ref(v) for k, v in textures.items()}
    tx["particle"] = A._ref(particle or list(textures.values())[0])
    return {"parent": "minecraft:block/block", "textures": tx, "elements": elements}


def facing_states(model_for, extra_props=None):
    """Blockstate json with a horizontal facing (north-authored models) and optional extra properties.

    model_for(props_dict) -> model name. extra_props: {"mode": ["0","1"]}.
    """
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    combos = [{}]
    for k, vals in (extra_props or {}).items():
        combos = [dict(c, **{k: v}) for c in combos for v in vals]
    for f, y in rot.items():
        for c in combos:
            key = ",".join([f"facing={f}"] + [f"{k}={v}" for k, v in sorted(c.items())])
            v = {"model": "noclip:block/" + model_for(c)}
            if y:
                v["y"] = y
            variants[key] = v
    return {"variants": variants}


# ------------------------------------------------------------------ shared materials


def paint_metal(seed, lo, hi, rust=0.0):
    img = T.ramp(T.fbm(seed, octaves=3, base=3), lo, hi)
    if rust:
        m = np.clip((T.noise(seed + "r", sigma=1.3) - (1 - rust)) * 4, 0, 1)
        img = T.blend(img, T.ramp(T.noise(seed + "rc", sigma=0.6), "#5a3018", "#8a4a22"), m * 0.8)
    return T.grain(img, seed, 0.04)


def car_paint(seed, lo, hi):
    img = T.ramp(T.fbm(seed, octaves=3, base=5), lo, hi)
    # dust settled along the lower half, a sheen across the top
    yy = np.arange(S)[:, None] * np.ones((1, S))
    img = T.shade(img, 1 + np.clip((10 - yy) / 10, 0, 1) * 0.12 - np.clip((yy - 20) / 12, 0, 1) * 0.12)
    dust = T.noise(seed + "d", sigma=1.0) * np.clip((yy - 16) / 16, 0, 1)
    img = T.blend(img, T.solid("#8a8070"), dust * 0.35)
    return T.grain(img, seed, 0.03)


# ------------------------------------------------------------------ cars
# A car is 2 blocks wide and 4 long: hood, cabin front, cabin back, trunk (north to south).
# Each block is symmetric across x so the same block fills both columns; the car's
# front points north before rotation.

CAR_COLOURS = {
    "red": ("#6a1e1a", "#8a2c24"),
    "beige": ("#a89a78", "#c0b28e"),
    "blue": ("#1e2a3e", "#2c3a52"),
    "green": ("#2e3a2a", "#3e4c38"),
}


def _glass():
    img = T.ramp(T.noise("carglass", sigma=2), "#14181c", "#2a3238")
    yy, xx = np.mgrid[0:S, 0:S]
    streak = (np.abs((xx - yy * 0.6) % 22 - 4) < 2).astype(float) * 0.25
    return T.shade(img, 1 + streak)


def _tyre():
    img = T.ramp(T.noise("tyre", sigma=1), "#141414", "#222222")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#6a6a66"), ((d < 7) & (d > 3)).astype(float))
    return img


def _chrome():
    return T.grain(T.ramp(T.noise("chrome", sigma=1.5), "#6c6e70", "#b8bcc0"), "chrome", 0.04)


def _headlight():
    img = T.ramp(T.noise("hl", sigma=1), "#cfc8a8", "#e8e0c0")
    return T.border(img, "#5a5a58", 2)


def _headlight_on():
    img = T.solid("#fffbe6")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.shade(img, 1 - np.clip(d / 30, 0, 0.12))
    return T.border(img, "#8a8a80", 2)


def _taillight():
    return T.border(T.ramp(T.noise("tl", sigma=1), "#5a0e0c", "#7a1a14"), "#2a2a28", 2)


def _plate():
    img = T.solid("#d8d4c4")
    img = T.border(img, "#2a3a6a", 2)
    return draw_text(img, "TMP 404", 3, 13, "#1a2a5a")


def _underside():
    return T.grain(T.solid("#141312"), "carunder", 0.03)


tex("ol_car_glass", _glass)
tex("ol_car_tyre", _tyre)
tex("ol_car_chrome", _chrome)
tex("ol_car_headlight", _headlight)
tex("ol_car_headlight_on", _headlight_on)
tex("ol_car_taillight", _taillight)
tex("ol_car_plate", _plate)
tex("ol_car_under", _underside)

for colour, (lo, hi) in CAR_COLOURS.items():
    tex(f"ol_car_{colour}", lambda lo=lo, hi=hi, c=colour: car_paint("car" + c, lo, hi))


def _car_blocks(colour, lit=False):
    p = f"ol_car_{colour}"
    tx = {"paint": p, "glass": "ol_car_glass", "tyre": "ol_car_tyre", "chrome": "ol_car_chrome",
          "head": "ol_car_headlight_on" if lit else "ol_car_headlight", "tail": "ol_car_taillight",
          "plate": "ol_car_plate", "under": "ol_car_under"}
    wheels = [(0, 0, 3, 2, 7, 11, "tyre"), (14, 0, 3, 16, 7, 11, "tyre")]
    suffix = "_lit" if lit else ""
    common = dict(sound="metal", unbreakable=True)
    # hood: low nose with headlights and grille facing north
    shaped(f"ol_car_{colour}_hood{suffix}", [
        (0, 3, 1, 16, 11, 16, {"all": "paint", "down": "under"}),
        (0, 4, 0, 16, 9, 1, {"all": "chrome", "north": "chrome"}),
        (2, 5, 0, 6, 8, 0.5, {"all": None, "north": "head"}),
        (10, 5, 0, 14, 8, 0.5, {"all": None, "north": "head"}),
    ] + wheels, tx, display="Abandoned Car", light=12 if lit else None, **common)
    # cabin front: windscreen sloping back over the dash
    shaped(f"ol_car_{colour}_cabin_front{suffix}", [
        (0, 3, 0, 16, 12, 16, {"all": "paint", "down": "under"}),
        (1, 12, 6, 15, 16, 16, {"all": "glass", "up": "paint"}),
        (1, 12, 3, 15, 14, 6, {"all": "glass"}),
    ], tx, display="Abandoned Car", **common)
    shaped(f"ol_car_{colour}_cabin_back{suffix}", [
        (0, 3, 0, 16, 12, 16, {"all": "paint", "down": "under"}),
        (1, 12, 0, 15, 16, 11, {"all": "glass", "up": "paint"}),
        (1, 12, 11, 15, 14, 14, {"all": "glass"}),
    ], tx, display="Abandoned Car", **common)
    shaped(f"ol_car_{colour}_trunk{suffix}", [
        (0, 3, 0, 16, 11, 15, {"all": "paint", "down": "under"}),
        (0, 4, 15, 16, 9, 16, {"all": "chrome"}),
        (2, 6, 15.6, 6, 9, 16, {"all": None, "south": "tail"}),
        (10, 6, 15.6, 14, 9, 16, {"all": None, "south": "tail"}),
        (6, 5, 15.6, 10, 7, 16, {"all": None, "south": "plate"}),
    ] + [(0, 0, 4, 2, 7, 12, "tyre"), (14, 0, 4, 16, 7, 12, "tyre")], tx, display="Abandoned Car", **common)


for colour in CAR_COLOURS:
    _car_blocks(colour)
_car_blocks("beige", lit=True)
_car_blocks("red", lit=True)
