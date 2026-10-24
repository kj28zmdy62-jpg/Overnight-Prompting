"""Level 37 - Sublimity, "The Poolrooms". White tile, warm shallow water, daylight from nowhere."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, ladder, stairs, flicker_light, BLOCKS, \
    carpet, lang
import tex as T
import synth as Y
from content.deepend_audio import wrapverb

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def glaze(img, seed, amount=0.07):
    """A soft diagonal sheen across glossy tile."""
    sheen = np.clip(1 - np.abs(((xx + yy) % 32) - 10) / 6.0, 0, 1) * amount
    return np.clip(img + sheen[..., None], 0, 1)


def tiles(seed, size=8, lo="#e9eeec", hi="#f7faf8", grout="#d3dbda", grout_w=1, var=0.035):
    """Square glazed tiles with grout lines; every tile gets its own slight tone."""
    base = T.ramp(T.fbm(seed, octaves=2, base=6) * 0.4 + 0.3, lo, hi)
    r = T.rng(seed, "tone")
    n = S // size
    tone = 1 + (r.random((n, n)) - 0.5) * 2 * var
    img = T.shade(base, np.kron(tone, np.ones((size, size))))
    # each tile is a touch brighter at its top-left: a pillowed glaze
    ty, tx = yy % size, xx % size
    pillow = 1 + 0.04 * (1 - (ty + tx) / (2.0 * size))
    img = T.shade(img, pillow)
    g = ((ty < grout_w) | (tx < grout_w)).astype(float)
    img = T.blend(img, T.solid(grout), g * 0.9)
    return T.grain(glaze(img, seed), seed, 0.012)


def wall_tile():
    return tiles("l37w")


def wall_tile_grime():
    img = tiles("l37wg")
    # limescale tide line and faint rust under where water once sat higher
    tide = np.clip(1 - np.abs(yy - 22) / 3.0, 0, 1) * (0.5 + 0.5 * T.noise("l37tide", sigma=1.5))
    img = T.blend(img, T.solid("#cfc8b0"), tide * 0.35)
    return T.stains(img, "l37wgs", "#b9b394", count=1, strength=0.25, size=7)


def wall_tile_band():
    img = tiles("l37wb")
    band = (yy >= 12) & (yy < 20)
    mos = mosaic("l37band", "#2f8fb0", "#4fb7d0", "#77d3e0", size=4)
    return T.blend(img, mos, band.astype(float))


def mosaic(seed, a, b, c, size=4, grout="#d6eef0"):
    r = T.rng(seed, "mos")
    n = S // size
    pick = r.random((n, n))
    t = np.kron(pick, np.ones((size, size)))
    img = T.ramp(t, a, b, c)
    g = ((yy % size == 0) | (xx % size == 0)).astype(float)
    img = T.blend(img, T.solid(grout), g * 0.55)
    return T.grain(img, seed, 0.02)


def caustics(seed, strength=0.22):
    """Bright wavy net of refracted light, the pattern sunlight leaves on a pool floor."""
    a = T.noise(seed + "a", sigma=2.2)
    b = T.noise(seed + "b", sigma=1.6)
    lines = np.clip(1 - np.abs(a - 0.5) * 9, 0, 1) + np.clip(1 - np.abs(b - 0.5) * 9, 0, 1)
    return np.clip(lines, 0, 1) * strength


def pool_tile():
    img = mosaic("l37p", "#3aa7c4", "#5cc4d8", "#8adbe6")
    return np.clip(img + caustics("l37pc")[..., None], 0, 1)


def pool_tile_lane():
    img = pool_tile()
    lane = (np.abs(xx - 15.5) < 4).astype(float)
    dark = mosaic("l37lane", "#123a5c", "#1a4a70", "#22567c")
    return T.blend(img, dark, lane)


def deep_tile():
    """The bottom of the dark pools. The mosaic just gets darker, and darker."""
    img = mosaic("l37d", "#071a26", "#0b2534", "#11303f", grout="#1a3c48")
    return np.clip(img + caustics("l37dc", 0.06)[..., None], 0, 1)


def floor_tile():
    return tiles("l37f", size=16, lo="#e6e8e2", hi="#f2f3ee", grout="#cdd3d0", var=0.02)


def floor_tile_wet():
    img = floor_tile()
    sheen = T.noise("l37wet", sigma=3)
    img = T.shade(img, 0.94 + sheen * 0.1)
    return T.blend(img, T.solid("#cfe6ea"), np.clip(sheen - 0.5, 0, 1) * 0.6)


def coping():
    """Rounded white edge stone of the pool, with the blue anti-slip lip."""
    img = T.ramp(T.fbm("l37cop", octaves=3, base=4), "#eceee8", "#f8f9f4")
    lip = (yy >= 26).astype(float)
    img = T.blend(img, T.solid("#2f86a8"), lip)
    img = T.shade(img, 1 - (yy == 25) * 0.15)
    return T.grain(glaze(img, "l37cop", 0.05), "l37cop", 0.015)


def coping_top():
    img = T.ramp(T.fbm("l37copt", octaves=3, base=4), "#eceee8", "#f8f9f4")
    img = T.blend(img, T.solid("#2f86a8"), (yy >= 27).astype(float))
    pits = (T.rng("copp").random((S, S)) > 0.92).astype(float)
    return T.shade(img, 1 - pits * 0.05)


def ceiling_tile():
    return tiles("l37c", size=16, lo="#eef1ef", hi="#f6f8f6", grout="#d4dad8", var=0.015)


def skylight():
    """A frosted panel that glows like an overcast noon. There is nothing above it."""
    g = np.clip(1 - np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2) / 26, 0, 1)
    img = T.ramp(g * 0.5 + T.noise("l37sky", sigma=4) * 0.15 + 0.35, "#e8f4f6", "#fbffff", "#ffffff")
    frame = ((xx < 2) | (yy < 2) | (xx > 29) | (yy > 29)).astype(float)
    mull = ((xx == 15) | (xx == 16) | (yy == 15) | (yy == 16)).astype(float) * 0.5
    img = T.blend(img, T.solid("#d4dcdc"), np.clip(frame + mull * 0.4, 0, 1))
    return img


def skylight_off():
    img = T.ramp(T.noise("l37skyo", sigma=4), "#9fb0b4", "#b4c2c4")
    frame = ((xx < 2) | (yy < 2) | (xx > 29) | (yy > 29)).astype(float)
    return T.blend(img, T.solid("#c4cccc"), frame)


def pool_light():
    """Round underwater lamp set into the pool wall."""
    img = pool_tile()
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#c8d0d0"), (d < 11).astype(float))
    glow = T.ramp(np.clip(1 - d / 9, 0, 1), "#bff6ff", "#e8ffff", "#ffffff")
    return T.blend(img, glow, (d < 9).astype(float))


def window_sky():
    """A window onto a summer sky. The building has no outside."""
    t = yy / 31.0
    img = T.ramp(t, "#5aa8e8", "#8cc6f0", "#cfe8f8")
    cloud = T.fbm("l37cloud", octaves=4, base=5)
    c = np.clip((cloud - 0.52) * 4, 0, 1) * np.clip(1.2 - t, 0, 1)
    img = T.blend(img, T.solid("#ffffff"), c * 0.85)
    frame = ((xx < 3) | (yy < 3) | (xx > 28) | (yy > 28)).astype(float)
    mull = (np.abs(xx - 15.5) < 1).astype(float)
    return T.blend(img, T.solid("#f0f2ee"), np.clip(frame + mull, 0, 1))


def drain():
    img = deep_tile()
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    plate = (d < 13).astype(float)
    img = T.blend(img, T.solid("#5a6468"), plate)
    slots = (d < 11) & ((xx % 4) < 2)
    img = T.blend(img, T.solid("#000000"), slots.astype(float))
    return T.grain(img, "l37drain", 0.04)


def ladder_tex():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    chrome = T.ramp(T.noise("l37chr", sigma=1, shape=(S, S)), "#9aa4a8", "#e8eef0")
    for x0 in (4, 26):
        rgb[:, x0:x0 + 3] = chrome[:, x0:x0 + 3]
        a[:, x0:x0 + 3] = 1
        rgb[:, x0] *= 0.8
    for y0 in (6, 22):
        rgb[y0:y0 + 3, 4:29] = T.hexc("#d8dee0")
        rgb[y0 + 2, 4:29] = T.hexc("#7a8488")
        a[y0:y0 + 3, 4:29] = 1
    return rgb, a


def slide_tex():
    """Glossy moulded plastic, once bright blue, sun-bleached in streaks."""
    img = T.ramp(T.fbm("l37sl", octaves=3, base=5) * 0.5 + yy / 64.0, "#2a7fc8", "#3a93d8", "#62b0e6")
    streak = (np.abs(((xx * 3 + yy) % 32) - 16) < 2).astype(float) * 0.12
    img = np.clip(img + streak[..., None], 0, 1)
    return T.grain(glaze(img, "l37sl", 0.1), "l37sl", 0.015)


def float_red():
    stripe = ((np.arctan2(yy - 15.5, xx - 15.5) / np.pi * 4) % 2 < 1)
    return np.where(stripe[..., None], T.hexc("#e8443c"), T.hexc("#f6f2ea"))


def footprints():
    """Wet footprints, walking toward a wall."""
    rgb = np.ones((S, S, 3)) * T.hexc("#9fb6ba")
    a = np.zeros((S, S))
    for (cx, cy, side) in ((10, 26, -1), (21, 18, 1), (11, 9, -1), (22, 1, 1)):
        d = ((xx - cx) / 3.2) ** 2 + ((yy - cy) / 5.0) ** 2
        heel = ((xx - cx) / 2.6) ** 2 + ((yy - cy - 5) / 2.4) ** 2
        toes = sum(((xx - (cx - 2 + k * 1.3)) ** 2 + (yy - (cy - 6 + abs(k - 1.5) * 0.5)) ** 2 < 0.9)
                   for k in range(4))
        m = (d < 1) | (heel < 1) | (toes > 0)
        a[m] = 0.55
    return rgb, a


tex("l37_tile", wall_tile)
tex("l37_tile_grime", wall_tile_grime)
tex("l37_tile_band", wall_tile_band)
tex("l37_pool", pool_tile)
tex("l37_pool_lane", pool_tile_lane)
tex("l37_deep", deep_tile)
tex("l37_floor", floor_tile)
tex("l37_floor_wet", floor_tile_wet)
tex("l37_coping", coping)
tex("l37_coping_top", coping_top)
tex("l37_ceiling", ceiling_tile)
tex("l37_skylight", skylight)
tex("l37_skylight_off", skylight_off)
tex("l37_pool_light", pool_light)
tex("l37_window", window_sky)
tex("l37_drain", drain)
tex("l37_ladder", ladder_tex)
tex("l37_slide", slide_tex)
tex("l37_float", float_red)
tex("l37_footprints", footprints)
tex("l37_lounger", lambda: T.grain(T.ramp(T.noise("l37lo", sigma=1), "#e8e6de", "#f6f4ee"), "l37lo", 0.02))
tex("l37_lounger_strap", lambda: T.grain(T.ramp(T.noise("l37ls", sigma=1), "#2f86a8", "#3a96b6"), "l37ls", 0.03))

cube("pool_tile_white", "l37_tile", display="White Pool Tile", sound="tile")
cube("pool_tile_limescale", "l37_tile_grime", display="Limescaled Pool Tile", sound="tile")
cube("pool_tile_banded", "l37_tile_band", display="Banded Pool Tile", sound="tile")
cube("pool_mosaic", "l37_pool", display="Pool Mosaic", sound="tile")
cube("pool_mosaic_lane", "l37_pool_lane", display="Lane-Marked Pool Mosaic", sound="tile")
cube("pool_mosaic_deep", "l37_deep", display="Deep-End Mosaic", sound="tile")
cube("pool_deck_tile", "l37_floor", display="Pool Deck Tile", sound="tile")
cube("pool_deck_tile_wet", "l37_floor_wet", display="Wet Pool Deck Tile", sound="tile")
cube_faces("pool_coping", top="l37_coping_top", bottom="l37_floor", side="l37_coping", display="Pool Coping",
           sound="tile")
cube("pool_ceiling_tile", "l37_ceiling", display="Poolroom Ceiling Tile", sound="tile", flat=True)
flicker_light("pool_skylight", "l37_skylight", "l37_skylight_off", "l37_ceiling", light=15, flicker=0.0,
              display="Frosted Skylight")
cube("pool_underwater_light", "l37_pool_light", display="Underwater Pool Light", light=12, emissive=True,
     sound="glass")
cube("pool_window_nowhere", "l37_window", display="Window to Nowhere", light=11, emissive=True, sound="glass",
     flat=True)
exit_block("pool_drain", "l37_drain", "WATER", display="Pool Drain", sound="metal")
ladder("pool_ladder", "l37_ladder", display="Pool Ladder")
carpet("pool_wet_footprints", "l37_footprints", display="Wet Footprints", sound="tile")

# a moulded slide: stair-shaped, and slick enough to carry you down into the water
stairs("pool_slide", "l37_slide", "pool_tile_white", display="Pool Slide")
for b in BLOCKS:
    if b["name"] == "pool_slide":
        b["kind"] = "class"
        b["class"] = "com.noclip.block.PoolSlideBlock"
        b.pop("base", None)
        b["sound"] = "glass"

shaped("pool_float_ring", [
    (2, 0, 2, 14, 3, 5, "ring"), (2, 0, 11, 14, 3, 14, "ring"),
    (2, 0, 5, 5, 3, 11, "ring"), (11, 0, 5, 14, 3, 11, "ring"),
], {"ring": "l37_float"}, display="Pool Float", collides=False, sound="wool")

shaped("pool_lounger", [
    (2, 0, 1, 3, 4, 2, "frame"), (13, 0, 1, 14, 4, 2, "frame"),
    (2, 0, 14, 3, 4, 15, "frame"), (13, 0, 14, 14, 4, 15, "frame"),
    (2, 4, 1, 14, 5, 15, {"all": "frame", "up": "strap"}),
    (2, 5, 12, 14, 11, 14, {"all": "frame", "north": "strap"}),
], {"frame": "l37_lounger", "strap": "l37_lounger_strap"}, display="Pool Lounger", sound="wood")

# ------------------------------------------------------------------ sound


def pool_ambience():
    """Water lapping against tile in a room too big to echo properly, a filter pump, and a slow major pad."""
    d = 28.0
    t = Y.t_axis(d)
    lap = Y.bp(Y.pink(d, "l37lap"), 180, 1400)
    swell = 0.55 + 0.45 * np.sin(2 * np.pi * t / 7.0) * np.sin(2 * np.pi * t / 4.0 + 1.0)
    lap = lap * swell * 0.5
    slosh = np.zeros_like(t)
    r = Y.rng("l37slosh")
    for k in range(18):
        n = int(0.35 * Y.SR)
        tt = np.arange(n) / Y.SR
        blob = Y.bp(Y.white(0.35, f"sl{k}"), 400 + r.random() * 600, 2600)[:n] * np.sin(np.pi * tt / 0.35) ** 2
        Y.place(slosh, blob, 0.5 + k * 1.5 + r.random() * 0.6, 0.18 + r.random() * 0.2)
    pump = Y.hum(d, 50.0, "l37pump", buzz=0.05, harmonics=(1, 2, 3)) * 0.06
    # pad: a soft open fifth that never resolves
    pad = Y.drone(d, [130.81, 196.0, 329.63, 246.94], "l37pad", detune=0.4, lfo=1 / 14.0) * 0.07
    pad = Y.lp(pad, 1800)
    x = wrapverb(lap + slosh, 3.5, 0.6, "l37room", damp=3000) + Y.lp(pump, 300) + pad
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.5), 0.0021), "stereo"


def far_splash(i):
    def f():
        d = 4.5
        out = np.zeros(int(d * Y.SR))
        n = int(0.6 * Y.SR)
        tt = np.arange(n) / Y.SR
        s = Y.bp(Y.white(0.6, f"spl{i}"), 500, 5000)[:n] * np.exp(-tt * 7) + Y.lp(Y.white(0.6, f"splb{i}"), 300)[:n] * np.exp(-tt * 12)
        Y.place(out, s, 0.1, 1.0)
        for k in range(5):
            Y.place(out, s[: n // 3] * 0.25, 0.5 + k * 0.17, 0.3 - k * 0.04)
        return Y.reverb(Y.lp(out, 2400 - i * 500), 4.0, 0.8, f"spl{i}")
    return f


def drip_echo(i):
    def f():
        out = np.zeros(int(4.0 * Y.SR))
        n = int(0.1 * Y.SR)
        tt = np.arange(n) / Y.SR
        fr = 1800 + i * 250 - tt * 6000
        plink = np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 45)
        Y.place(out, plink, 0.05, 0.8)
        return Y.reverb(out, 3.8, 0.75, f"l37drip{i}")
    return f


def laughter():
    """Children laughing somewhere very far away. There are no children here."""
    d = 3.5
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("l37laugh")
    for k in range(9):
        n = int(0.13 * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 420 + r.random() * 160 - k * 6
        v = np.sin(2 * np.pi * f0 * tt + 3 * np.sin(2 * np.pi * 7 * tt)) * np.sin(np.pi * tt / 0.13) ** 2
        v += Y.bp(Y.white(0.13, f"lg{k}"), 1500, 4000)[:n] * 0.3 * np.sin(np.pi * tt / 0.13)
        Y.place(out, v, 0.2 + k * 0.17 + r.random() * 0.03, 0.4 + 0.3 * np.sin(np.pi * k / 9))
    out = Y.lp(Y.hp(out, 300), 2200)
    return Y.reverb(out, 4.5, 0.85, "l37laugh")


def whistle():
    """A lifeguard's whistle. Once. From a room you cannot find."""
    d = 3.0
    t = Y.t_axis(d)
    e = np.clip(t / 0.03, 0, 1) * (t < 0.55) + (t >= 0.55) * np.exp(-(t - 0.55) * 30)
    trill = 1 + 0.03 * np.sign(np.sin(2 * np.pi * 34 * t))
    x = np.sin(2 * np.pi * np.cumsum(2700 * trill) / Y.SR) * e * 0.5
    x += Y.bp(Y.white(d, "whb"), 2400, 3200) * e * 0.3
    return Y.reverb(Y.lp(x, 4000), 4.0, 0.85, "l37wh")


def gurgle():
    d = 2.5
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    r = Y.rng("l37gurg")
    for k in range(16):
        n = int(0.07 * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 140 + r.random() * 260
        b = np.sin(2 * np.pi * np.cumsum(f0 + tt * 3000) / Y.SR) * np.exp(-tt * 40)
        Y.place(out, b, 0.1 + k * 0.13 + r.random() * 0.05, 0.4 + r.random() * 0.5)
    return Y.norm(Y.reverb(Y.lp(out, 1500), 1.5, 0.5, "l37g"), 0.7)


def slide_splash():
    d = 2.0
    out = np.zeros(int(d * Y.SR))
    n = int(1.0 * Y.SR)
    tt = np.arange(n) / Y.SR
    s = Y.bp(Y.white(1.0, "l37ss"), 300, 7000)[:n] * np.exp(-tt * 4.5)
    s += Y.lp(Y.white(1.0, "l37ss2"), 200)[:n] * np.exp(-tt * 8) * 2
    Y.place(out, s, 0.0)
    return Y.norm(Y.reverb(out, 2.5, 0.5, "l37ss"), 0.85)


sound("ambient.level_37", [("ambient/level_37", pool_ambience)], stream=True)
sound("ambient.level_37.additions",
      [(f"ambient/level_37_splash{i}", far_splash(i)) for i in range(3)] +
      [(f"ambient/level_37_drip{i}", drip_echo(i)) for i in range(3)] +
      [("ambient/level_37_laughter", laughter), ("ambient/level_37_whistle", whistle)],
      subtitle="Water moves somewhere")
sound("event.level_37_undertow", [("event/level_37_undertow", gurgle)], subtitle="The drain gurgles")
sound("event.level_37_splash", [("event/level_37_splash", slide_splash)], subtitle="Splash")

level("level_37", fog="#dcecee", sky="#dcecee", water="#3fc6dc", water_fog="#5cc8d8", ambient="ambient.level_37",
      additions="ambient.level_37.additions", additions_chance=0.005, fixed_time=6000, height=48,
      ambient_light=0.18)
