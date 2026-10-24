"""Helpers shared by the wiki-classic levels (2, 3, 4, 5, 7, 8, 9, 10, 11, 13, Manila Room, Airport).

Defines no blocks itself: a tiny pixel font for signs and screens, a box-model
builder for custom block classes, and a few texture motifs.
"""
import numpy as np
import tex as T
from assets import _uv, _ref

S = 32

# 3x5 pixel font. Each glyph is five rows of three bits.
FONT = {
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
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
    "2": ["110", "001", "010", "100", "111"], "3": ["110", "001", "010", "001", "110"],
    "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "110", "001", "110"],
    "6": ["011", "100", "111", "101", "111"], "7": ["111", "001", "010", "010", "010"],
    "8": ["111", "101", "111", "101", "111"], "9": ["111", "101", "111", "001", "110"],
    " ": ["000", "000", "000", "000", "000"], ".": ["000", "000", "000", "000", "010"],
    ":": ["000", "010", "000", "010", "000"], "!": ["010", "010", "010", "000", "010"],
    "?": ["110", "001", "010", "000", "010"], "-": ["000", "000", "111", "000", "000"],
    ">": ["100", "010", "001", "010", "100"], "<": ["001", "010", "100", "010", "001"],
    "/": ["001", "001", "010", "100", "100"], "=": ["000", "111", "000", "111", "000"],
    "'": ["010", "010", "000", "000", "000"], "_": ["000", "000", "000", "000", "111"],
    "^": ["010", "101", "000", "000", "000"], "#": ["101", "111", "101", "111", "101"],
}


def text_width(s, scale=1, gap=1):
    return len(s) * (3 * scale + gap) - gap


def draw_text(img, s, x, y, color, scale=1, gap=1, alpha=1.0, jitter=None):
    """Draw text into img (H,W,3) at pixel x,y. color is hex or rgb. Returns img."""
    out = img.copy()
    c = T.hexc(color) if isinstance(color, str) else np.asarray(color)
    h, w = out.shape[:2]
    cx = x
    r = T.rng("txt", s, x, y) if jitter else None
    for ch in s.upper():
        g = FONT.get(ch, FONT["?"])
        for gy, row in enumerate(g):
            for gx, bit in enumerate(row):
                if bit != "1":
                    continue
                for sy in range(scale):
                    for sx in range(scale):
                        px, py = cx + gx * scale + sx, y + gy * scale + sy
                        if 0 <= px < w and 0 <= py < h:
                            a = alpha
                            if r is not None:
                                a *= 1 - jitter * r.random()
                            out[py, px] = out[py, px] * (1 - a) + c * a
        cx += 3 * scale + gap
    return out


def centered(img, s, y, color, scale=1, gap=1, alpha=1.0, jitter=None):
    w = img.shape[1]
    return draw_text(img, s, (w - text_width(s, scale, gap)) // 2, y, color, scale, gap, alpha, jitter)


def box_model(boxes, textures, particle=None, ambient_occlusion=True):
    """Block model json from boxes (x1,y1,z1,x2,y2,z2, tex_or_facemap) like assets.shaped, for custom()."""
    elements = []
    for b in boxes:
        x1, y1, z1, x2, y2, z2, t = b
        faces = {}
        for f in ("down", "up", "north", "south", "west", "east"):
            key = t.get(f, t.get("all")) if isinstance(t, dict) else t
            if key is None:
                continue
            faces[f] = {"texture": "#" + key, "uv": _uv(f, x1, y1, z1, x2, y2, z2)}
        elements.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces})
    tx = {k: _ref(v) for k, v in textures.items()}
    tx["particle"] = _ref(particle or list(textures.values())[0])
    m = {"parent": "minecraft:block/block", "textures": tx, "elements": elements}
    if not ambient_occlusion:
        m["ambientocclusion"] = False
    return m


def facing_states(name, extra=None):
    """Blockstate variants for a HorizontalDirectionalBlock-style custom block (model authored facing north)."""
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    v = {}
    for f, y in rot.items():
        key = f"facing={f}" + (("," + extra) if extra else "")
        e = {"model": f"noclip:block/{name}"}
        if y:
            e["y"] = y
        v[key] = e
    return {"variants": v}


def metal(seed, lo, hi, sigma=1.2, rust=None, rust_amt=0.0):
    img = T.ramp(T.noise(seed, sigma=sigma) * 0.6 + T.fbm(seed + "f", octaves=3, base=3) * 0.4, lo, hi)
    if rust:
        m = np.clip(T.fbm(seed + "r", octaves=4, base=4) * 1.8 - 1.0 + rust_amt, 0, 1)
        img = T.blend(img, T.ramp(T.noise(seed + "rc", sigma=0.6), *rust), m)
    return T.grain(img, seed, 0.04)


def bricks(seed, lo, hi, mortar="#3a3632", bw=8, bh=4, mortar_dark=0.0):
    img = T.ramp(T.fbm(seed, octaves=3, base=3), lo, hi)
    yy, xx = np.mgrid[0:S, 0:S]
    row = yy // bh
    off = (row % 2) * (bw // 2)
    brick_id = ((xx + off) // bw) + row * 17
    r = T.rng(seed + "b")
    tint = r.random(2000)[brick_id % 2000]
    img = T.shade(img, 0.85 + tint[..., None][..., 0] * 0.3)
    m = ((yy % bh) == 0) | (((xx + off) % bw) == 0)
    img = T.blend(img, T.solid(mortar), m.astype(float) * (1 - mortar_dark))
    return T.grain(img, seed, 0.05)


def vgrad_alpha(h, w, a0, a1):
    return np.linspace(a0, a1, h)[:, None] * np.ones((1, w))
