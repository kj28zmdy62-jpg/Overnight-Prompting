"""Level 10 - Bumper Crop. Wheat to the horizon under a sky the colour of dishwater. Barns, silos,
scarecrows. Calm. Then you hear the engine, and the wheat starts falling in a line."""
import numpy as np
from assets import cube, cube_faces, pillar, tex, shaped, cross, fence, stairs, exit_block, level, sound
import tex as T
import synth as Y
from content.classics_kit import centered, metal

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def soil():
    img = T.ramp(T.fbm("l10s", octaves=4, base=4) * 0.6 + T.noise("l10sn", sigma=0.6) * 0.4, "#2e2216", "#3e2e1e")
    furrow = np.sin(yy / 32.0 * np.pi * 4) * 0.08
    return T.grain(T.shade(img, 1 + furrow), "l10s", 0.05)


def track():
    img = T.ramp(T.fbm("l10t", octaves=4, base=4), "#5a4a36", "#6e5c44")
    ruts = (np.abs((xx % 16) - 8) < 2).astype(float) * 0.15
    stones = (T.rng("l10ts").random((S, S)) > 0.95)
    return T.grain(T.blend(T.shade(img, 1 - ruts), T.solid("#8a8070"), stones * 0.5), "l10t", 0.05)


def grass():
    img = T.ramp(T.fbm("l10g", octaves=3, base=4) * 0.5 + T.noise("l10gb", sigma=0.4) * 0.5, "#4a5a30", "#5a6a3a", "#6a7842")
    return T.grain(img, "l10g", 0.05)


def stalks(seed, heads, short=False):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        r = T.rng(seed)
        for k in range(26):
            x = r.random() * S
            top = (0 if not heads else 4 + r.random() * 10) if not short else 20 + r.random() * 6
            lean = (r.random() - 0.5) * 0.25
            col = T.hexc("#c8a858") * (0.75 + r.random() * 0.35)
            for y in range(int(top), S):
                xi = int(x + (S - y) * lean) % S
                rgb[y, xi] = np.clip(col * (0.8 + 0.2 * y / S), 0, 1)
                a[y, xi] = 1
            if heads and not short:
                hx = int(x + (S - top) * lean)
                for dy in range(-6, 1):
                    for dx in (0, 1):
                        yy_ = int(top) + dy
                        if 0 <= yy_ < S:
                            rgb[yy_, (hx + dx) % S] = T.hexc("#e0c070") * (0.85 + r.random() * 0.2)
                            a[yy_, (hx + dx) % S] = 1
        return rgb, a
    return f


def barn_wall():
    img = T.ramp(T.fbm("l10b", octaves=2, base=3) * 0.5 + T.noise("l10bg", sigma=0.5) * 0.3, "#6a1a14", "#7e241a")
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.4)
    peel = np.clip(T.fbm("l10bp", octaves=3, base=3) * 1.8 - 1.0, 0, 1)
    img = T.blend(img, T.ramp(T.noise("l10bw", sigma=0.6), "#6a5a48", "#7a6a56"), peel)
    return T.grain(img, "l10b", 0.04)


def barn_trim():
    img = T.ramp(T.noise("l10bt", sigma=1), "#d8d4c8", "#e8e4d8")
    return T.grain(T.shade(img, 1 - ((xx % 8) == 0) * 0.2), "l10bt", 0.03)


def barn_roof():
    img = metal("l10r", "#4a4a46", "#5a5a54", rust=("#5a3018", "#7a4220"), rust_amt=0.15)
    return T.shade(img, 1 - ((xx % 6) == 0) * 0.3 + ((xx % 6) == 1) * 0.15)


def barn_floor():
    img = T.ramp(T.fbm("l10bf", octaves=2, base=3), "#4a3a26", "#5a4630")
    straw = (T.noise("l10str", sigma=0.4) > 0.75)
    return T.grain(T.blend(T.shade(img, 1 - ((yy % 8) == 0) * 0.3), T.solid("#c8a858"), straw * 0.5), "l10bf", 0.04)


def hay_side():
    img = T.ramp(T.noise("l10h", sigma=0.4), "#a88a40", "#c8a858")
    img = T.shade(img, 1 - ((yy % 3) == 0) * 0.15)
    img = T.rect(img, 0, 8, 32, 10, "#6a5028")
    return T.rect(img, 0, 22, 32, 24, "#6a5028")


def hay_top():
    img = T.ramp(T.noise("l10ht", sigma=0.5), "#a88a40", "#c8a858")
    return T.rect(T.rect(img, 8, 0, 10, 32, "#6a5028"), 22, 0, 24, 32, "#6a5028")


def silo():
    img = metal("l10si", "#8a8c8a", "#9ea09c", sigma=1.5, rust=("#6a4a30", "#8a5a34"), rust_amt=-0.1)
    img = T.shade(img, 1 - ((xx % 4) == 0) * 0.2 + ((xx % 4) == 2) * 0.1)
    img = T.shade(img, 1 - ((yy % 16) == 0) * 0.3)
    return T.grain(img, "l10si", 0.03)


def silo_door():
    img = metal("l10sd", "#5a5c58", "#6e706a", sigma=1.5)
    img = T.border(img, "#2a2c28", 2)
    img = T.rect(img, 22, 14, 26, 18, "#b0aca0")
    img = T.rect(img, 4, 4, 28, 10, "#e0d8c0")
    img = centered(img, "NO", 5, "#a01810")
    img = T.rect(img, 4, 22, 28, 28, "#e0d8c0")
    img = centered(img, "ENTRY", 23, "#a01810")
    return img


def hatch():
    img = T.ramp(T.fbm("l10ch", octaves=2, base=3), "#3a2a1a", "#4a3622")
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.4)
    img = T.rect(img, 2, 4, 30, 7, "#2a2a28")
    img = T.rect(img, 2, 25, 30, 28, "#2a2a28")
    return T.rect(img, 14, 14, 18, 18, "#6a6a62")


def scarecrow_cloth():
    img = T.ramp(T.noise("l10sc", sigma=0.8), "#4a4a5a", "#5a5a6a")
    img = T.blend(img, T.solid("#6a3a2a"), (((xx // 8) + (yy // 8)) % 2 == 0) * 0.3)
    return T.grain(img, "l10sc", 0.06)


def scarecrow_face():
    img = T.ramp(T.noise("l10sf", sigma=1), "#b8a888", "#c8b898")
    img = T.rect(img, 8, 10, 12, 14, "#0a0806")
    img = T.rect(img, 20, 10, 24, 14, "#0a0806")
    for x in range(8, 25, 2):
        img[22 + (x % 4 == 0), x] = T.hexc("#1a0e08")
    return img


def wood_post():
    return T.grain(T.ramp(T.noise("l10wp", sigma=0.5), "#3a2c1e", "#4a3a28"), "l10wp", 0.05)


def straw_tex():
    return T.grain(T.ramp(T.noise("l10st", sigma=0.4), "#b89a50", "#d8b868"), "l10st", 0.06)


TEX = {"l10_soil": soil, "l10_track": track, "l10_grass": grass,
       "l10_wheat_low": stalks("l10wl", False), "l10_wheat_top": stalks("l10wt", True),
       "l10_wheat_top_b": stalks("l10wt2", True), "l10_stubble": stalks("l10stb", False, short=True),
       "l10_barn": barn_wall, "l10_barn_trim": barn_trim, "l10_barn_roof": barn_roof, "l10_barn_floor": barn_floor,
       "l10_hay": hay_side, "l10_hay_top": hay_top, "l10_silo": silo, "l10_silo_door": silo_door,
       "l10_hatch": hatch, "l10_scarecrow": scarecrow_cloth, "l10_scarecrow_face": scarecrow_face,
       "l10_post": wood_post, "l10_straw": straw_tex}
for k, fn in TEX.items():
    tex(k, fn)

cube("crop_soil", "l10_soil", display="Tilled Soil", sound="gravel")
cube("crop_track", "l10_track", display="Dirt Track", sound="gravel")
cube("crop_grass", "l10_grass", display="Verge Grass", sound="grass")
cross("wheat_lower", "l10_wheat_low", display="Wheat")
cross("wheat_upper", "l10_wheat_top", display="Wheat")
cross("wheat_upper_b", "l10_wheat_top_b", display="Wheat")
cross("wheat_stubble", "l10_stubble", display="Cut Wheat")
cube("barn_planks", "l10_barn", display="Barn Planks", sound="wood")
cube("barn_trim", "l10_barn_trim", display="Barn Trim", sound="wood")
cube("barn_roof", "l10_barn_roof", display="Barn Roof", sound="metal")
stairs("barn_roof_stairs", "l10_barn_roof", "barn_roof", display="Barn Roof")
cube("barn_floor", "l10_barn_floor", display="Barn Floor", sound="wood")
pillar("crop_hay_bale", "l10_hay_top", "l10_hay", display="Hay Bale", sound="grass")
cube("silo_wall", "l10_silo", display="Silo", sound="metal")
exit_block("silo_door", "l10_silo_door", "DOOR", trigger="USE", solid=True, display="Silo Door", sound="metal")
exit_block("barn_cellar_hatch", "l10_hatch", "DESCEND", trigger="USE", solid=True, display="Cellar Hatch", sound="wood")
fence("crop_fence", "l10_post", display="Field Fence", sound="wood")
pillar("crop_pole", "l10_post", "l10_post", display="Telephone Pole", sound="wood")
shaped("scarecrow_body", [(7, 0, 7, 9, 16, 9, "post"), (4, 6, 6, 12, 16, 10, "cloth"), (0, 13, 7, 16, 15, 9, "post"),
                          (0, 12, 6, 3, 15, 10, "straw"), (13, 12, 6, 16, 15, 10, "straw")],
       {"post": "l10_post", "cloth": "l10_scarecrow", "straw": "l10_straw"}, display="Scarecrow", sound="wool",
       unbreakable=True)
shaped("scarecrow_head", [(7, 0, 7, 9, 3, 9, "post"), (4, 3, 4, 12, 11, 12, {"all": "face_side", "south": "face"}),
                          (2, 11, 2, 14, 12, 14, "straw"), (5, 12, 5, 11, 15, 11, "straw")],
       {"post": "l10_post", "face": "l10_scarecrow_face", "face_side": "l10_straw", "straw": "l10_straw"},
       display="Scarecrow Head", sound="wool", unbreakable=True)

# ------------------------------------------------------------------ sound


def field_loop():
    d = 26.0
    t = Y.t_axis(d)
    gust = 0.5 + 0.5 * np.sin(2 * np.pi * t * 3 / d) * np.sin(2 * np.pi * t * 2 / d + 1)
    rustle = Y.bp(Y.white(d, "l10rs"), 1800, 7000) * 0.12 * gust
    wind = Y.lp(Y.pink(d, "l10wind"), 400) * 0.6 * (0.6 + 0.4 * gust)
    low = Y.lp(Y.brown(d, "l10low"), 80) * 0.3
    x = Y.loopable(Y.norm(rustle + wind + low, 0.45), 1.5)
    return Y.stereoize(x, 0.003), "stereo"


def crow(i):
    def f():
        out = np.zeros(int(3.0 * Y.SR))
        for k in range(2 + i):
            n = int(0.35 * Y.SR)
            tt = np.arange(n) / Y.SR
            f0 = 900 - 300 * tt / 0.35
            v = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * np.sin(np.pi * tt / 0.35)
            v = Y.bp(v + Y.white(0.35, f"cr{i}{k}")[:n] * 0.6, 500, 3500)
            Y.place(out, v, 0.1 + k * 0.6, 0.8)
        return Y.norm(Y.reverb(Y.lp(out, 3000), 2.5, 0.5, f"l10cv{i}"), 0.4)
    return f


def windmill():
    d = 4.0
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * np.cumsum(220 + 80 * np.sin(2 * np.pi * 0.5 * t)) / Y.SR)
    x *= (np.sin(2 * np.pi * 0.5 * t) > 0.3) * (1 + np.sign(np.sin(2 * np.pi * 25 * t)))
    return Y.norm(Y.reverb(Y.bp(x, 300, 2000), 2.5, 0.6, "l10wm"), 0.3)


def harvester():
    """An engine, a clattering reel, nobody driving it."""
    d = 3.0
    t = Y.t_axis(d)
    f0 = 38.0
    eng = sum(np.sin(2 * np.pi * f0 * h * t + h) / h for h in range(1, 9))
    eng *= 1 + 0.3 * np.sign(np.sin(2 * np.pi * f0 / 2 * t))
    reel = np.zeros_like(t)
    for k in range(int(d * 6)):
        n = int(0.04 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(reel, Y.bp(Y.white(0.04, f"hv{k}"), 1000, 6000)[:n] * np.exp(-tt * 100), k / 6.0, 0.6)
    chop = Y.bp(Y.white(d, "hvc"), 2000, 8000) * 0.25 * (np.sin(2 * np.pi * 12 * t) > 0)
    return Y.norm(Y.lp(eng, 600) + reel + chop, 0.95)


sound("ambient.level_10", [("ambient/level_10", field_loop)], stream=True)
sound("ambient.level_10.additions",
      [(f"ambient/level_10_crow{i}", crow(i)) for i in range(2)] + [("ambient/level_10_windmill", windmill)],
      subtitle="Crows over the wheat")
sound("event.level_10_harvester", [("event/level_10_harvester", harvester)], subtitle="An engine in the wheat")

level("level_10", fog="#9a9c98", sky="#8e928e", ambient="ambient.level_10", additions="ambient.level_10.additions",
      additions_chance=0.004, fixed_time=6000, skylight=True, ceiling=False, height=64)
