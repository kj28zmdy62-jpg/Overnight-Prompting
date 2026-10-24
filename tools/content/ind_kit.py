"""Shared helpers for the industrial/underground levels (server_farm ... dam).

Registers nothing on import. Gives:
  anim_tex(name, frame_fn, frames, frametime)  - animated block texture (writes the .mcmeta)
  text(img, s, x, y, color, scale)             - 3x5 pixel font for signage, labels, numbers
  steel(), concrete(), rust(), rivets(), hazard_stripes(), grate() - common painters
"""
import json
import os
import numpy as np
import assets as A
import tex as T

S = 32


# ------------------------------------------------------------------ animated textures

def anim_tex(name, frame_fn, frames, frametime=2, interpolate=False, alpha=False):
    """frame_fn(i) -> rgb (32,32,3) or (rgb, alpha). Frames are stacked vertically and an
    .mcmeta is written next to the png when the texture is painted."""
    def paint():
        rgbs, alphas = [], []
        for i in range(frames):
            out = frame_fn(i)
            if isinstance(out, tuple):
                rgbs.append(out[0])
                alphas.append(out[1])
            else:
                rgbs.append(out)
                alphas.append(np.ones(out.shape[:2]))
        path = os.path.join(A.ASSETS, "textures", "block", name + ".png.mcmeta")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        meta = {"animation": {"frametime": frametime}}
        if interpolate:
            meta["animation"]["interpolate"] = True
        with open(path, "w") as f:
            json.dump(meta, f)
        rgb = np.concatenate(rgbs, axis=0)
        if alpha:
            return rgb, np.concatenate(alphas, axis=0)
        return rgb
    return A.tex(name, paint)


# ------------------------------------------------------------------ pixel font

_FONT = {
    "A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110",
    "E": "111100110100111", "F": "111100110100100", "G": "011100101101011", "H": "101101111101101",
    "I": "111010010010111", "J": "001001001101010", "K": "101101110101101", "L": "100100100100111",
    "M": "101111111101101", "N": "110101101101101", "O": "010101101101010", "P": "110101110100100",
    "Q": "010101101110011", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010",
    "U": "101101101101111", "V": "101101101101010", "W": "101101111111101", "X": "101101010101101",
    "Y": "101101010010010", "Z": "111001010100111",
    "0": "111101101101111", "1": "010110010010111", "2": "110001010100111", "3": "110001010001110",
    "4": "101101111001001", "5": "111100110001110", "6": "011100111101111", "7": "111001010010010",
    "8": "111101111101111", "9": "111101111001110",
    "-": "000000111000000", ".": "000000000000010", ":": "000010000010000", "/": "001001010100100",
    "!": "010010010000010", "?": "110001010000010", "#": "101111101111101", "%": "101001010100101",
    "+": "000010111010000", "=": "000111000111000", ">": "100010001010100", "<": "001010100010001",
    "_": "000000000000111", "'": "010010000000000", " ": "000000000000000", "*": "101010111010101",
}


def text(img, s, x, y, color, scale=1, alpha=1.0):
    """Draw text with the 3x5 font. Each glyph is 3*scale wide with 1*scale spacing."""
    out = img.copy()
    c = T.hexc(color) if isinstance(color, str) else np.asarray(color)
    h, w = out.shape[:2]
    cx = x
    for ch in s.upper():
        g = _FONT.get(ch, _FONT["?"])
        for r in range(5):
            for q in range(3):
                if g[r * 3 + q] == "1":
                    for dy in range(scale):
                        for dx in range(scale):
                            py, px = y + r * scale + dy, cx + q * scale + dx
                            if 0 <= py < h and 0 <= px < w:
                                out[py, px] = out[py, px] * (1 - alpha) + c * alpha
        cx += 4 * scale
    return out


def text_width(s, scale=1):
    return len(s) * 4 * scale - scale


def text_mask(s, w, h, x, y, scale=1):
    m = np.zeros((h, w))
    img = text(np.zeros((h, w, 3)), s, x, y, "#ffffff", scale)
    m[img[..., 0] > 0.5] = 1
    return m


# ------------------------------------------------------------------ common painters

def mgrid():
    return np.mgrid[0:S, 0:S]


def concrete(seed, lo, hi, pores=0.93, stain="#2e2d2a", stains=2, crack=0):
    img = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.7 + T.noise(seed + "f", sigma=0.5) * 0.3, lo, hi)
    p = (T.rng(seed + "p").random((S, S)) > pores).astype(float)
    img = T.shade(img, 1 - p * 0.16)
    if stains:
        img = T.stains(img, seed + "st", stain, count=stains, strength=0.3, size=8)
    if crack:
        img = T.cracks(img, seed + "cr", "#1c1c1a", count=crack, alpha=0.5)
    return T.grain(img, seed, 0.04)


def steel(seed, lo="#5d636a", hi="#7a8189", brushed=True):
    n = T.noise(seed, sigma=1.5)
    if brushed:
        streak = T.noise(seed + "b", shape=(S, S), sigma=0)
        from scipy.ndimage import gaussian_filter1d
        streak = gaussian_filter1d(streak, 6, axis=1, mode="wrap")
        n = n * 0.5 + (streak - streak.min()) / (np.ptp(streak) + 1e-9) * 0.5
    return T.grain(T.ramp(n, lo, hi), seed, 0.03)


def rust(img, seed, amount=0.35, color="#6b3a1e", color2="#8a4a22"):
    m = np.clip(T.fbm(seed, octaves=3, base=3) * 1.8 - (1.4 - amount * 1.2), 0, 1)
    r = T.ramp(T.noise(seed + "r", sigma=0.6), color, color2)
    return T.blend(img, r, m)


def rivets(img, positions, color_hi="#a8aeb4", color_lo="#30343a"):
    out = img.copy()
    for (x, y) in positions:
        out[y % S, x % S] = T.hexc(color_hi)
        out[(y + 1) % S, (x + 1) % S] = T.hexc(color_lo)
    return out


def hazard_stripes(img, y0, y1, seed="hz", c1="#c9a227", c2="#1c1c1a", width=5, worn=0.25):
    yy, xx = mgrid()
    stripe = (((xx + yy) // width) % 2 == 0)
    col = np.where(stripe[..., None], T.hexc(c1), T.hexc(c2))
    m = ((yy >= y0) & (yy < y1) & (T.noise(seed, sigma=0.8) > worn)).astype(float)
    return T.blend(img, col, m)


def grate(seed, color_lo="#3d4146", color_hi="#5a6067", pitch=4, bar=1, rust_amt=0.0):
    """Open steel grating: returns (rgb, alpha)."""
    yy, xx = mgrid()
    img = steel(seed, color_lo, color_hi, brushed=False)
    if rust_amt:
        img = rust(img, seed + "r", rust_amt)
    a = (((xx % pitch) < bar) | ((yy % pitch) < bar)).astype(float)
    a[:1, :] = a[-1:, :] = 1
    a[:, :1] = a[:, -1:] = 1
    return img, a


def vstreaks(img, seed, color, count=4, strength=0.3, length=(10, 30)):
    """Vertical drip/run-off streaks."""
    out = img.copy()
    r = T.rng(seed)
    c = T.hexc(color)
    for _ in range(count):
        x = int(r.random() * S)
        y0 = int(r.random() * S)
        ln = int(length[0] + r.random() * (length[1] - length[0]))
        wdt = 1 + int(r.random() * 2)
        for y in range(ln):
            a = strength * (1 - y / ln)
            for dx in range(wdt):
                yy, xx = (y0 + y) % S, (x + dx) % S
                out[yy, xx] = out[yy, xx] * (1 - a) + c * a
    return out


def glow_spot(shape, cx, cy, rad, falloff=2.0):
    h, w = shape
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / rad
    return np.clip(1 - d, 0, 1) ** falloff


# ------------------------------------------------------------------ box models for custom block classes

def box_model(boxes, textures, particle=None):
    """Block model json from boxes (x1,y1,z1,x2,y2,z2, key|facemap) like assets.shaped, facing north."""
    elements = []
    for b in boxes:
        x1, y1, z1, x2, y2, z2, t = b
        faces = {}
        for f in ("down", "up", "north", "south", "west", "east"):
            key = t.get(f, t.get("all")) if isinstance(t, dict) else t
            if key is None:
                continue
            faces[f] = {"texture": "#" + key, "uv": A._uv(f, x1, y1, z1, x2, y2, z2)}
        elements.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces})
    tx = {k: A._ref(v) for k, v in textures.items()}
    tx["particle"] = A._ref(particle or list(textures.values())[0])
    return {"parent": "minecraft:block/block", "textures": tx, "elements": elements}


def facing_states(model_for, extra=None):
    """Blockstate json for a horizontally-facing block. model_for(extra_value) -> model name.
    extra: (property, [values]) for one additional property, or None."""
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    for d, r in rot.items():
        values = extra[1] if extra else [None]
        for v in values:
            e = {"model": "noclip:block/" + model_for(v)}
            if r:
                e["y"] = r
            key = f"facing={d}" + (f",{extra[0]}={v}" if extra else "")
            variants[key] = e
    return {"variants": variants}
