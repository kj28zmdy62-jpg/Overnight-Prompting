"""Level 94 - Motion. Rolling clay hills, a lone house, and everything boils like stop-motion animation."""
import json
import os
import numpy as np
import assets as A
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, cross, entity_model, entity_tex, lang, \
    ITEM_MODELS, part
import tex as T
import synth as Y
from content.deepend_audio import wrapverb, fit
from entity_paint import Canvas

S = 32
yy, xx = np.mgrid[0:S, 0:S]
FRAMES = 3


def animated(name, frame_fn, frametime=4, alpha=False):
    """A texture that 'boils' like hand-animated clay: FRAMES slightly different drawings, cycled.

    frame_fn(i) returns one 32x32 frame (rgb, or (rgb, alpha)). The .mcmeta is written when the
    texture is painted, after the build has cleaned the texture folder.
    """
    def paint():
        frames = [frame_fn(i) for i in range(FRAMES)]
        path = os.path.join(A.ASSETS, "textures", "block", name + ".png.mcmeta")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            json.dump({"animation": {"frametime": frametime, "interpolate": False}}, f)
        if isinstance(frames[0], tuple):
            return np.concatenate([f[0] for f in frames], axis=0), np.concatenate([f[1] for f in frames], axis=0)
        return np.concatenate(frames, axis=0)
    tex(name, paint)


def clay(seed, i, lo, hi, prints=True):
    """Smooth modelling clay: soft lumps, a sheen, and the whorls of somebody's thumb."""
    s = f"{seed}{i}"
    img = T.ramp(T.fbm(s, octaves=2, base=7) * 0.7 + 0.15, lo, hi)
    if prints:
        r = T.rng("thumb", s)
        for _ in range(2):
            cx, cy = r.random() * 32, r.random() * 32
            d = np.sqrt(((xx - cx + 16) % 32 - 16) ** 2 * 1.3 + ((yy - cy + 16) % 32 - 16) ** 2)
            ridges = (np.sin(d * 1.9) > 0.55) & (d < 7)
            img = T.shade(img, 1 - ridges * 0.07)
    hl = np.clip(T.noise(s + "hl", sigma=3) - 0.65, 0, 1) * 0.5
    img = np.clip(img + hl[..., None] * 0.4, 0, 1)
    # the drawing shifts by a pixel between frames
    return np.roll(img, (i % 2, (i * 7) % 3 - 1), axis=(0, 1))


def grass_top(i):
    return clay("mgr", i, "#3aa83a", "#5ccc4a")


def grass_side(i):
    img = clay("mgs", i, "#8a5a32", "#a06a3c")
    top = clay("mgr", i, "#3aa83a", "#5ccc4a")
    edge = 9 + (np.sin(xx / 3.0 + i) * 2).astype(int)
    m = yy < edge
    return T.blend(img, top, m.astype(float))


def dirt(i):
    return clay("mdi", i, "#8a5a32", "#a06a3c")


def flower(c, seed):
    def f(i):
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        sway = int(round(np.sin(i * 2.1) * 1.5))
        stem = clay(seed + "st", i, "#2a8a2a", "#3aa83a")
        for y in range(14, 32):
            x = 15 + sway * (32 - y) // 18
            rgb[y, x:x + 2] = stem[y, x]
            a[y, x:x + 2] = 1
        petals = clay(seed, i, c, c, prints=False)
        cx, cy = 16 + sway, 10
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        ang = np.arctan2(yy - cy, xx - cx)
        m = d < 5 + 2 * np.cos(ang * 5 + i)
        rgb[m] = petals[m]
        a[m] = 1
        center = d < 2.2
        rgb[center] = T.hexc("#f8d838")
        return rgb, a
    return f


def leaves(i):
    rgb = clay("mlf", i, "#2a9a3a", "#4abc4a")
    a = np.ones((S, S))
    return rgb, a


def trunk(i):
    img = clay("mtr", i, "#6a4228", "#7e5232")
    return T.shade(img, 1 - ((xx + i) % 8 == 0) * 0.15)


def house_wall(i):
    img = clay("mhw", i, "#f0e2c0", "#fcf0d0")
    return T.shade(img, 1 - ((yy % 16) == 15) * 0.1)


def roof(i):
    img = clay("mrf", i, "#c83a2a", "#e04a32")
    return T.shade(img, 1 - ((yy % 8) == 7) * 0.25)


def window(i):
    img = clay("mwin", i, "#f8d060", "#ffe890", prints=False)
    frame = (xx < 3) | (xx > 28) | (yy < 3) | (yy > 28) | (np.abs(xx - 15.5) < 1.5) | (np.abs(yy - 15.5) < 1.5)
    return T.blend(img, clay("mwf", i, "#6a4228", "#7e5232", prints=False), frame.astype(float))


def floor_planks(i):
    img = clay("mfl", i, "#b07a48", "#c48a54")
    return T.shade(img, 1 - ((yy % 8) == 0) * 0.25)


def rug(i):
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = clay("mrug", i, "#3a6ad8", "#4a7ae8", prints=False)
    rings = (np.sin(d * 0.9 + i) > 0.6)
    return T.blend(img, T.solid("#f8d838"), rings.astype(float) * 0.7)


def tv_screen(i):
    """The TV shows a clay face. It blinks, out of time with everything else."""
    img = T.ramp(T.noise(f"mtvs{i}", sigma=0.6), "#202830", "#384858")
    face = np.sqrt((xx - 15.5) ** 2 + (yy - 16) ** 2) < 11
    img = T.blend(img, clay("mtvf", i, "#e8b890", "#f4c8a0", prints=False), face.astype(float))
    for ex in (11, 20):
        eye = ((xx - ex) ** 2 + (yy - 13) ** 2) < (8 if i != 2 else 1)
        img[eye] = T.hexc("#ffffff")
        if i != 2:
            img[((xx - ex - (i - 1)) ** 2 + (yy - 13) ** 2) < 3] = T.hexc("#101010")
    mouth = (np.abs(yy - 21 - (xx - 15.5) ** 2 / 30) < 1) & (np.abs(xx - 15.5) < 5)
    img[mouth] = T.hexc("#6a2018")
    frame = (xx < 3) | (xx > 28) | (yy < 3) | (yy > 28)
    return T.blend(img, clay("mtvc", i, "#5a3a24", "#6a4428", prints=False), frame.astype(float))


animated("m94_grass", grass_top)
animated("m94_grass_side", grass_side)
animated("m94_dirt", dirt)
animated("m94_flower_red", flower("#e03a3a", "mfr"), alpha=True)
animated("m94_flower_blue", flower("#3a7ae8", "mfb"), alpha=True)
animated("m94_flower_pink", flower("#f07ab8", "mfp"), alpha=True)
animated("m94_leaves", leaves)
animated("m94_trunk", trunk)
animated("m94_wall", house_wall)
animated("m94_roof", roof)
animated("m94_window", window)
animated("m94_floor", floor_planks)
animated("m94_rug", rug)
animated("m94_tv", tv_screen, frametime=6)

cube_faces("motion_clay_grass", top="m94_grass", bottom="m94_dirt", side="m94_grass_side", display="Clay Grass",
           sound="mud")
cube("motion_clay_dirt", "m94_dirt", display="Clay Earth", sound="mud")
for c in ("red", "blue", "pink"):
    cross(f"motion_clay_flower_{c}", f"m94_flower_{c}", display="Clay Flower", sound="mud")
cube("motion_clay_leaves", "m94_leaves", display="Clay Foliage", sound="mud")
cube("motion_clay_trunk", "m94_trunk", display="Clay Trunk", sound="mud")
cube("motion_clay_wall", "m94_wall", display="Clay House Wall", sound="mud")
cube("motion_clay_roof", "m94_roof", display="Clay Roof Tiles", sound="mud")
cube("motion_clay_window", "m94_window", display="Clay Window", light=10, emissive=True, sound="glass", flat=True)
cube("motion_clay_floor", "m94_floor", display="Clay Floorboards", sound="mud")
shaped("motion_clay_rug", [(0, 0, 0, 16, 1, 16, "r")], {"r": "m94_rug"}, display="Clay Rug", collides=False,
       sound="wool", unbreakable=True)
exit_block("motion_clay_tv", "m94_tv", "SECRET", trigger="USE", solid=True, display="Television", light=7,
           emissive=True, sound="mud")
shaped("motion_clay_armchair", [
    (1, 0, 1, 15, 7, 15, {"all": "chair"}), (1, 7, 12, 15, 16, 15, "chair"), (1, 7, 1, 3, 11, 12, "chair"),
    (13, 7, 1, 15, 11, 12, "chair"),
], {"chair": "m94_roof"}, display="Clay Armchair", unbreakable=True, sound="mud")

# ------------------------------------------------------------------ the clay people

entity_model("motion_clay_figure", [
    part("head", (0, 2, 0), cubes=[(0, 0, -5, -10, -5, 10, 10, 10)]),
    part("body", (0, 2, 0), cubes=[(0, 20, -4, 0, -2.5, 8, 11, 5)]),
    part("right_arm", (-5, 3, 0), cubes=[(26, 20, -2, -1, -1.5, 3, 11, 3)]),
    part("left_arm", (5, 3, 0), cubes=[(38, 20, -1, -1, -1.5, 3, 11, 3)]),
    part("right_leg", (-2, 13, 0), cubes=[(0, 36, -1.5, 0, -1.5, 3, 11, 3)]),
    part("left_leg", (2, 13, 0), cubes=[(12, 36, -1.5, 0, -1.5, 3, 11, 3)]),
])


def clay_figure_tex():
    c = Canvas()
    skin = lambda w, h: T.ramp(T.fbm(f"mcf{w}{h}", shape=(h, w), octaves=2, base=3) * 0.6 + 0.2, "#e8b890", "#f4c8a0")
    shirt = lambda w, h: T.ramp(T.fbm(f"mcs{w}{h}", shape=(h, w), octaves=2, base=3) * 0.6 + 0.2, "#e0c030", "#f0d040")
    trousers = lambda w, h: T.ramp(T.fbm(f"mct{w}{h}", shape=(h, w), octaves=2, base=3) * 0.6 + 0.2, "#3a6ad8", "#4a7ae8")

    def face(w, h):
        img = skin(w, h)
        hy, hx = np.mgrid[0:h, 0:w]
        for ex in (2.5, 7.0):
            eye = ((hx - ex) ** 2 + (hy - 3.5) ** 2) < 3.2
            img[eye] = T.hexc("#ffffff")
            img[((hx - ex) ** 2 + (hy - 3.5) ** 2) < 0.6] = T.hexc("#101010")
        # a smile pressed in with a fingernail, slightly too wide
        for x in range(1, 9):
            y = int(round(6.6 + 1.0 * np.sin(np.pi * (x - 1) / 7)))
            img[min(y, h - 1), x] = T.hexc("#7a2a1a")
        return img

    c.box(0, 0, 10, 10, 10, all=skin, front=face)
    c.box(0, 20, 8, 11, 5, all=shirt)
    c.box(26, 20, 3, 11, 3, all=skin)
    c.box(38, 20, 3, 11, 3, all=skin)
    c.box(0, 36, 3, 11, 3, all=trousers)
    c.box(12, 36, 3, 11, 3, all=trousers)
    return c.out()


entity_tex("motion_clay_figure", clay_figure_tex)
lang("entity.noclip.motion_clay_figure", "Clay Figure")
lang("item.noclip.motion_clay_figure_spawn_egg", "Clay Figure Spawn Egg")
ITEM_MODELS["motion_clay_figure_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def motion_loop():
    """A kalimba lullaby that skips like a dropped frame, wind over the hills, and a shutter clicking at twelve frames a second."""
    d = 24.0
    t = Y.t_axis(d)
    n = len(t)
    melody = [523.25, 659.25, 783.99, 659.25, 587.33, 698.46, 880.0, 698.46, 523.25, 659.25, 783.99, 1046.5]
    mus = np.zeros(n)
    beat = 0.5
    k = 0
    at = 0.2
    while at < d - 1.5:
        f0 = melody[k % len(melody)]
        tt = Y.t_axis(1.4)
        tone = (np.sin(2 * np.pi * f0 * tt) + 0.25 * np.sin(2 * np.pi * f0 * 3.01 * tt)) * np.exp(-tt * 4)
        Y.place(mus, tone, at, 0.22)
        # every so often a frame is dropped and a note repeats
        at += beat if (k % 7) != 5 else beat * 0.5
        k += 1 if (k % 9) != 4 else 0
        if (k % 9) == 4:
            k += 1
    wind = Y.bp(Y.pink(d, "m94wind"), 150, 1800) * 0.12 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 12.0) ** 2)
    shutter = np.zeros(n)
    click = Y.bp(Y.white(0.01, "m94click"), 2000, 9000)[: int(0.01 * Y.SR)]
    for j in range(int(d * 12)):
        Y.place(shutter, click, j / 12.0, 0.04)
    x = wrapverb(mus, 1.5, 0.35, "m94room", damp=5000) + wind + shutter
    return Y.stereoize(Y.loopable(Y.norm(x, 0.42), 1.0), 0.002), "stereo"


def squish(i):
    def f():
        d = 0.5
        t = Y.t_axis(d)
        x = Y.lp(Y.white(d, f"m94sq{i}"), 700 + i * 200) * np.exp(-t * 14)
        x += np.sin(2 * np.pi * (140 + i * 30) * t * (1 - t)) * np.exp(-t * 18) * 0.5
        return Y.norm(x, 0.7)
    return f


def giggle_slow():
    d = 3.0
    out = np.zeros(int(d * Y.SR))
    for k in range(5):
        nn = int(0.22 * Y.SR)
        tt = np.arange(nn) / Y.SR
        syl = np.sin(2 * np.pi * (260 - k * 8) * tt) * np.sin(np.pi * tt / tt[-1]) ** 2
        Y.place(out, syl, 0.1 + k * 0.33, 0.6)
    return Y.reverb(Y.lp(out, 1500), 2.5, 0.6, "m94gig")


def shutter_burst():
    out = np.zeros(int(2.0 * Y.SR))
    click = Y.bp(Y.white(0.03, "m94sb"), 1500, 8000)[: int(0.03 * Y.SR)] * np.exp(-np.arange(int(0.03 * Y.SR)) / Y.SR * 150)
    for j in range(10):
        Y.place(out, click, 0.1 + j * 0.083, 0.8)
    return Y.reverb(out, 1.0, 0.3, "m94sb")


def kettle():
    d = 4.0
    t = Y.t_axis(d)
    f0 = 1800 + 400 * np.clip(t / 3, 0, 1)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.clip(t / 2.5, 0, 1) * (t < 3.6) * 0.3
    x += Y.bp(Y.white(d, "m94k"), 2500, 5000) * np.clip(t / 3, 0, 1) * 0.1
    return Y.reverb(Y.lp(x, 4000), 2.0, 0.6, "m94k")


sound("ambient.level_94", [("ambient/level_94", motion_loop)], stream=True)
sound("ambient.level_94.additions",
      [("ambient/level_94_giggle", giggle_slow), ("ambient/level_94_shutter", shutter_burst),
       ("ambient/level_94_kettle", kettle)],
      subtitle="Something is being animated")
sound("entity.motion_clay_figure.hop", [(f"entity/motion_clay_figure_hop{i}", squish(i)) for i in range(3)],
      subtitle="Clay squelches")

level("level_94", fog="#bfe0f8", sky="#7ec0f8", ambient="ambient.level_94", additions="ambient.level_94.additions",
      additions_chance=0.004, fixed_time=6000, skylight=True, ceiling=False, height=96)
