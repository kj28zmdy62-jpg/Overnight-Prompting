"""Static Fields. Endless grey grass under a grey sky, and one old television in the
middle of it, playing static. Whatever you see on the screen is already behind you."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, custom, level, sound, lang, entity_model, entity_tex, \
    BLOCK_MODELS, ITEM_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import animated, box_model, draw_text
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32

# ------------------------------------------------------------------ ground


def grass_top(seed="sfg", lo="#5f6a58", mid="#6f7a66", hi="#83897a"):
    base = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.6 + T.noise(seed + "b", sigma=0.4) * 0.4, lo, mid, hi)
    # individual blades as short bright/dark streaks
    r = T.rng(seed + "blades")
    img = base.copy()
    for _ in range(140):
        x, y = int(r.random() * S), int(r.random() * S)
        ln = 2 + int(r.random() * 3)
        c = T.hexc(hi) * (0.9 + r.random() * 0.2) if r.random() < 0.6 else T.hexc(lo) * 0.8
        for k in range(ln):
            img[(y + k) % S, (x + (k if r.random() < 0.3 else 0)) % S] = c
    img = T.blend(img, T.solid("#8b8a74"), (T.noise(seed + "dry", sigma=2) > 0.78) * 0.35)
    return T.grain(img, seed, 0.05)


def soil():
    img = T.ramp(T.fbm("sfsoil", octaves=4, base=4), "#4e4a43", "#5c574e", "#68625a")
    pebbles = (T.rng("sfpeb").random((S, S)) > 0.95).astype(float)
    img = T.blend(img, T.solid("#76716a"), pebbles * 0.7)
    roots = T.cracks(np.zeros((S, S, 3)), "sfroot", "#ffffff", count=2, alpha=1.0)[..., 0]
    img = T.blend(img, T.solid("#2a2622"), roots * 0.5)
    return T.grain(img, "sfsoil", 0.05)


def grass_side():
    img = soil()
    top = grass_top("sfgs")
    yy, xx = np.mgrid[0:S, 0:S]
    fringe = 12 + (T.noise("sffr", shape=(1, S), sigma=0.8)[0] * 12).astype(int)
    m = (yy < fringe[None, :]).astype(float)
    return T.blend(img, top, m)


def dead_top():
    return grass_top("sfdead", "#6d6852", "#7f785e", "#958c6c")


def imprint(part):
    """A flattened outline in the grass, the shape of someone who lay here a long time."""
    def f():
        img = grass_top("sfimp" + part)
        yy, xx = np.mgrid[0:S, 0:S]
        if part == "upper":
            head = ((xx - 16) ** 2 / 25 + (yy - 8) ** 2 / 30) < 1
            torso = (np.abs(xx - 16) < 7) & (yy > 13)
            arms = ((np.abs(xx - 6) < 2.5) | (np.abs(xx - 26) < 2.5)) & (yy > 15)
            m = head | torso | arms
        else:
            legs = ((np.abs(xx - 12) < 3) | (np.abs(xx - 20) < 3)) & (yy < 28)
            hips = (np.abs(xx - 16) < 7) & (yy < 8)
            m = legs | hips
        flat = T.ramp(T.noise("sfimpf" + part, sigma=0.6), "#8e8c6e", "#a6a282")
        # flattened blades all lie the same way
        flat = T.shade(flat, 1 - ((yy + xx // 3) % 3 == 0) * 0.08)
        from scipy.ndimage import binary_dilation
        edge = binary_dilation(m, iterations=1) & ~m
        img = T.blend(img, flat, m.astype(float) * 0.9)
        return T.shade(img, 1 - edge * 0.25)
    return f


def tall_grass(seed, lo, hi, dry=False):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        r = T.rng(seed)
        for _ in range(26 if not dry else 18):
            x0 = 2 + r.random() * 28
            h = 10 + r.random() * 20
            lean = (r.random() - 0.5) * 0.5
            c = T.ramp(np.array([[r.random()]]), lo, hi)[0, 0]
            for y in range(int(h)):
                py = S - 1 - y
                px = int(x0 + lean * y + (y / h) ** 2 * lean * 6)
                if 0 <= px < S and py >= 0:
                    rgb[py, px] = c * (0.7 + 0.3 * y / h)
                    a[py, px] = 1
                    if y < h * 0.4 and px + 1 < S:
                        rgb[py, px + 1] = c * 0.75
                        a[py, px + 1] = 1
            if dry and r.random() < 0.6:
                # seed head
                py = int(S - h)
                px = int(x0 + lean * h * 1.4)
                for k in range(4):
                    if 0 <= px < S and 0 <= py + k < S:
                        rgb[py + k, px] = T.hexc("#a8a084")
                        a[py + k, px] = 1
        return rgb, a
    return f


T_GRASS = "sf_grass_top"
for name, fn in (("sf_grass_top", grass_top), ("sf_grass_side", grass_side), ("sf_soil", soil),
                 ("sf_dead_top", dead_top), ("sf_imprint_upper", imprint("upper")),
                 ("sf_imprint_lower", imprint("lower")),
                 ("sf_tall_grass", tall_grass("sftg", "#55604e", "#8a907c")),
                 ("sf_dry_grass", tall_grass("sfdg", "#7a7258", "#a39a7a", dry=True))):
    A.tex(name, fn)

cube_faces("sf_grass", top="sf_grass_top", bottom="sf_soil", side="sf_grass_side", sound="grass",
           display="Grey Grass")
cube_faces("sf_dead_grass", top="sf_dead_top", bottom="sf_soil", side="sf_grass_side", sound="grass",
           display="Dead Grass")
cube_faces("sf_imprint_upper", top="sf_imprint_upper", bottom="sf_soil", side="sf_grass_side", sound="grass",
           display="Flattened Grass")
cube_faces("sf_imprint_lower", top="sf_imprint_lower", bottom="sf_soil", side="sf_grass_side", sound="grass",
           display="Flattened Grass")
cube("sf_soil", "sf_soil", sound="gravel", display="Grey Soil")
cross("sf_tall_grass", "sf_tall_grass", display="Tall Grey Grass")
cross("sf_dry_grass", "sf_dry_grass", display="Dry Grass")

# ------------------------------------------------------------------ the television


def static_frame(i, figure=None):
    r = T.rng("sfstatic", i)
    n = r.random((S, S))
    # rolling bright band like a badly tuned set
    yy = np.arange(S)[:, None] * np.ones((1, S))
    band = np.exp(-((yy - (i * 5) % S) ** 2) / 10.0) * 0.25
    v = np.clip(n * 0.85 + band + (r.random((S, 1)) - 0.5) * 0.25, 0, 1)
    img = np.stack([v * 0.92, v * 0.95, v], axis=-1)
    if figure == "man":
        # a dark figure standing in the static, slightly too tall, slightly too close
        xx = np.mgrid[0:S, 0:S][1]
        ax = np.abs(xx - 16)
        head = ((xx - 16) ** 2 / 6.5 + (yy - 7) ** 2 / 10.5) < 1
        neck = (ax < 1.3) & (yy >= 9) & (yy < 13)
        torso = (ax < 6.2 - (yy - 13) * 0.08) & (yy >= 12.5) & (yy < 31)
        gap = (np.abs(ax - 4.4) < 0.5) & (yy > 16) & (yy < 28)
        legs_gap = (ax < 0.6) & (yy > 25)
        m = ((head | neck | torso) & ~gap & ~legs_gap).astype(float)
        jitter = (r.random((S, S)) > 0.85).astype(float)
        img = T.blend(img, T.solid("#0a0a0c"), np.clip(m - jitter * 0.4, 0, 1))
    elif figure == "door":
        xx = np.mgrid[0:S, 0:S][1]
        door = (np.abs(xx - 16) < 6) & (yy > 6) & (yy < 31)
        frame = (np.abs(xx - 16) < 8) & (yy > 4) & (yy < 31) & ~door
        img = T.blend(img, T.solid("#020203"), door.astype(float) * (0.9 + 0.1 * r.random((S, S))))
        img = T.blend(img, T.solid("#d8d8d0"), frame.astype(float) * 0.5)
    # scanlines and the curved bezel shadow of a CRT
    img = T.shade(img, 1 - (yy % 2 == 0) * 0.18)
    d = np.sqrt(((np.mgrid[0:S, 0:S][1] - 15.5) / 16) ** 2 + ((yy - 15.5) / 16) ** 2)
    return T.shade(img, 1 - np.clip(d - 0.75, 0, 1) * 1.2)


def screen_off():
    img = T.ramp(T.noise("sfoff", sigma=3), "#16191a", "#22272a")
    yy, xx = np.mgrid[0:S, 0:S]
    glare = np.exp(-(((xx - 9) ** 2) / 20 + ((yy - 8) ** 2) / 12)) * 0.25
    # the faint reflection of a figure behind you
    fig = ((((xx - 22) ** 2 + (yy - 12) ** 2) < 7) | ((np.abs(xx - 22) < 3.5) & (yy > 14))).astype(float) * 0.06
    return T.shade(img, 1 + glare - fig)


def casing():
    img = T.ramp(T.fbm("sfcase", octaves=3, base=4), "#4a3a2a", "#5c4834")
    yy, xx = np.mgrid[0:S, 0:S]
    grain_lines = (np.sin(yy * 1.7 + T.noise("sfcw", sigma=3) * 6) > 0.85).astype(float) * 0.12
    img = T.shade(img, 1 - grain_lines)
    return T.grain(T.stains(img, "sfcase", "#2a2018", count=1, strength=0.3, size=7), "sfcase", 0.04)


def bezel():
    img = T.ramp(T.noise("sfbez", sigma=1.5), "#3a3836", "#4a4744")
    img = T.rect(img, 22, 20, 28, 26, "#8a8478")
    img = T.rect(img, 23, 21, 27, 25, "#2a2826")
    img = T.rect(img, 22, 4, 28, 10, "#8a8478")
    img = T.rect(img, 24, 6, 26, 8, "#2a2826")
    return T.grain(img, "sfbez", 0.04)


def metal():
    return T.grain(T.ramp(T.noise("sfmet", sigma=1), "#6a6a68", "#9a9a96"), "sfmet", 0.04)


animated("sf_screen_static", lambda i: static_frame(i), 8, frametime=1)
animated("sf_screen_man", lambda i: static_frame(i, "man"), 8, frametime=1)
animated("sf_screen_door", lambda i: static_frame(i, "door"), 8, frametime=2)
A.tex("sf_screen_off", screen_off)
A.tex("sf_tv_casing", casing)
A.tex("sf_tv_bezel", bezel)
A.tex("sf_tv_metal", metal)


def tv_boxes(screen):
    return [
        # wooden stand with splayed legs
        (2, 0, 4, 3, 5, 5, "wood"), (13, 0, 4, 14, 5, 5, "wood"),
        (2, 0, 11, 3, 5, 12, "wood"), (13, 0, 11, 14, 5, 12, "wood"),
        (1, 4, 3, 15, 5, 13, "wood"),
        # the set: a deep wooden cabinet with a bulging tube behind
        (1, 5, 5, 15, 15, 14, {"all": "wood", "south": "bezel"}),
        (4, 7, 2, 12, 13, 5, "wood"),
        (2.5, 6.5, 14, 11.5, 13.5, 14.5, {"all": "wood", "south": screen + "!"}),
        # rabbit ears
        (7.5, 15, 9, 8.5, 24, 10, "metal!", ("z", 22.5, (8, 15, 9.5))),
        (7.5, 15, 9, 8.5, 24, 10, "metal!", ("z", -22.5, (8, 15, 9.5))),
    ]


TV_TEX = {"wood": "sf_tv_casing", "bezel": "sf_tv_bezel", "metal": "sf_tv_metal"}
screens = ["sf_screen_static", "sf_screen_man", "sf_screen_door", "sf_screen_off"]
for mode, scr in enumerate(screens):
    BLOCK_MODELS[f"sf_tv_m{mode}"] = box_model(tv_boxes("screen"), dict(TV_TEX, screen=scr))
custom("sf_tv", "com.noclip.block.StaticTvBlock", box_model(tv_boxes("screen"), dict(TV_TEX, screen=screens[0])),
       blockstate={"variants": {f"mode={m}": {"model": f"noclip:block/sf_tv_m{m}"} for m in range(4)}},
       display="Old Television", sound="wood", unbreakable=True, render="cutout")

# a television lying face down in the grass, its back split open
shaped("sf_tv_fallen", [
    (2, 0, 2, 14, 9, 13, {"all": "wood", "up": "wood"}),
    (4, 9, 4, 12, 13, 10, "wood"),
], {"wood": "sf_tv_casing"}, display="Fallen Television", sound="wood", unbreakable=True)


def fabric():
    img = T.ramp(T.fbm("sfchair", octaves=3, base=3), "#4a5040", "#5e6450")
    yy, xx = np.mgrid[0:S, 0:S]
    weave = (((xx + yy) % 4) == 0).astype(float) * 0.06
    img = T.shade(img, 1 - weave)
    img = T.stains(img, "sfchs", "#2e3228", count=2, strength=0.4, size=6)
    return T.grain(img, "sfchair", 0.05)


A.tex("sf_armchair", fabric)
A.tex("sf_wood", lambda: T.grain(T.ramp(T.fbm("sfwood", octaves=3, base=2), "#4a4136", "#5e5446"), "sfwood", 0.05))
shaped("sf_armchair", [
    (1, 0, 1, 2, 3, 2, "wood"), (14, 0, 1, 15, 3, 2, "wood"), (1, 0, 14, 2, 3, 15, "wood"), (14, 0, 14, 15, 3, 15, "wood"),
    (1, 3, 1, 15, 8, 15, "fabric"),
    (1, 8, 12, 15, 16, 16, "fabric"),
    (0, 8, 2, 3, 13, 15, "fabric"), (13, 8, 2, 16, 13, 15, "fabric"),
], {"fabric": "sf_armchair", "wood": "sf_wood"}, display="Armchair", sound="wool", unbreakable=True)


def cord():
    rgb = np.ones((S, S, 3)) * T.hexc("#18181a")
    a = np.zeros((S, S))
    xx = np.mgrid[0:S, 0:S][1]
    a[:, 14:18] = 1
    rgb[:, 15] = T.hexc("#3a3a3e")
    return rgb, a


A.tex("sf_cord", cord)
shaped("sf_cord", [(6, 0, 0, 10, 0.6, 16, {"all": "c", "north": None, "south": None})], {"c": "sf_cord"},
       display="Power Cord", collides=False, sound="wool", unbreakable=True)


def post():
    img = T.ramp(T.fbm("sfpost", octaves=3, base=2), "#3e3830", "#575046")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (np.sin(xx * 2.1 + T.noise("sfpw", sigma=2) * 4) > 0.8) * 0.15)
    return T.grain(img, "sfpost", 0.05)


def wire():
    rgb = np.ones((S, S, 3)) * T.hexc("#5a5a58")
    a = np.zeros((S, S))
    a[10, :] = 1
    a[22, :] = 1
    for x in range(0, S, 8):
        a[9:12, x] = 1
        a[21:24, x + 4 if x + 4 < S else x] = 1
    return rgb, a


A.tex("sf_post", post)
A.tex("sf_wire", wire)
shaped("sf_fence_post", [(6, 0, 6, 10, 18, 10, "p"), (5, 17, 5, 11, 18, 11, "p")], {"p": "sf_post"},
       display="Old Fence Post", unbreakable=True)
shaped("sf_fence_wire", [(0, 0, 7.5, 16, 16, 8.5, {"all": "w", "east": None, "west": None, "up": None, "down": None})],
       {"w": "sf_wire"}, display="Fence Wire", collides=False, unbreakable=True, sound="metal")


def horn():
    img = T.ramp(T.noise("sfhorn", sigma=1.5), "#7a7a70", "#a09e92")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#1a1a18"), (d < 10).astype(float) * 0.9)
    img = T.blend(img, T.solid("#3a3a36"), ((d < 4)).astype(float))
    return T.stains(img, "sfhs", "#5a3a20", count=2, strength=0.5, size=5)


A.tex("sf_horn", horn)
shaped("sf_speaker_pole", [(6, 0, 6, 10, 16, 10, "p")], {"p": "sf_post"}, display="Pole", unbreakable=True)
shaped("sf_speaker", [(6, 0, 6, 10, 10, 10, "p"), (3, 8, 0, 13, 16, 7, {"all": "h", "north": "h!"}),
                      (11, 2, 7, 13, 4, 16, "m")],
       {"p": "sf_post", "h": "sf_horn", "m": "sf_tv_metal"}, display="Loudspeaker", unbreakable=True, sound="metal")

# ------------------------------------------------------------------ the man in the static


def static_paint(frame):
    def painter(w, h):
        r = T.rng("smp", frame, w, h)
        v = r.random((h, w))
        v = np.where(v > 0.6, 0.85 + v * 0.15, v * 0.25)
        img = np.stack([v * 0.9, v * 0.93, v], axis=-1)
        return img
    return painter


def static_man_tex(frame):
    def f():
        c = Canvas()
        p = static_paint(frame)
        dark = lambda w, h: T.shade(p(w, h), 0.35)
        paint_humanoid(c, p, dark, dark, dark)
        return c.out()
    return f


entity_model("static_man", humanoid(arm_len=16, leg_len=15))
for i in range(3):
    entity_tex(f"static_man_{i}", static_man_tex(i))
lang("entity.noclip.static_man", "The Broadcast")
lang("item.noclip.static_man_spawn_egg", "The Broadcast Spawn Egg")
ITEM_MODELS["static_man_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def wind_loop():
    d = 24.0
    t = Y.t_axis(d)
    gust = 0.55 + 0.25 * np.sin(2 * np.pi * t / 12) + 0.2 * np.sin(2 * np.pi * t / 8 + 1.3)
    wind = Y.bp(Y.pink(d, "sfwind"), 180, 1400) * gust
    rustle = Y.hp(Y.white(d, "sfrust"), 3500) * 0.05 * (gust ** 3)
    low = Y.lp(Y.brown(d, "sfrum"), 90) * 0.4
    # a television somewhere, too far to place: hiss riding under the wind
    hiss = Y.bp(Y.white(d, "sfhiss"), 3000, 9000) * 0.025 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 6))
    whine = np.sin(2 * np.pi * (round(7812 * d) / d) * t) * 0.004
    x = Y.loopable(Y.norm(wind + rustle + low + hiss + whine, 0.5), 1.5)
    return Y.stereoize(x, 0.0018), "stereo"


def tv_static():
    d = 2.6
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "sftv"), 1200, 11000) * (0.85 + 0.15 * np.sin(2 * np.pi * 60 * t))
    x += np.sin(2 * np.pi * 59.94 * t) * 0.05
    return Y.norm(x * Y.env(len(t), 0.25, 0.25), 0.5)


def burst():
    d = 1.6
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "sfburst"), 400, 12000) * np.exp(-t * 2.2)
    x += Y.lp(Y.white(d, "sfburst2"), 120) * np.exp(-t * 8) * 4
    crush = np.round(x * 4) / 4
    return Y.norm(x * 0.4 + crush * 0.6, 0.95)


def sting():
    d = 3.5
    t = Y.t_axis(d)
    x = Y.drone(d, [49.0, 51.9, 98.0], "sfsting", lfo=0) * np.clip(t / 2.5, 0, 1) ** 2 * np.exp(-np.maximum(t - 2.8, 0) * 6)
    x += Y.bp(Y.white(d, "sfsting2"), 2000, 8000) * np.clip(t / 3.0, 0, 1) ** 3 * 0.4
    return Y.norm(x, 0.8)


def channel_change():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.impact("sfclunk", 0.3), 0.0, 0.6)
    st = Y.bp(Y.white(0.4, "sfcc"), 1500, 9000) * 0.4
    Y.place(out, st, 0.05)
    voice = Y.whisper("sfvoice", 0.8)
    voice = Y.bp(voice, 300, 3000) * 2.0
    Y.place(out, voice, 0.45, 0.8)
    Y.place(out, Y.bp(Y.white(0.6, "sfcc2"), 1500, 9000) * 0.35, 1.25)
    return Y.reverb(Y.norm(out, 0.6), 2.5, 0.55, "sfcc")


def test_tone():
    d = 1.8
    t = Y.t_axis(d)
    wob = 1 + 0.004 * np.sin(2 * np.pi * 0.7 * t)
    x = np.sin(2 * np.pi * np.cumsum(1000 * wob) / Y.SR) * Y.env(len(t), 0.01, 0.05) * (t < 1.3)
    return Y.reverb(Y.norm(x, 0.35), 3.0, 0.7, "sftone")


def crow():
    out = np.zeros(int(3.5 * Y.SR))
    for k, at in enumerate((0.1, 0.7, 1.35)):
        d = 0.35
        n = int(d * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 520 - tt * 300
        buzz = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR))
        caw = Y.bp(buzz + Y.white(d, f"sfcaw{k}") * 0.5, 600, 2600) * np.sin(np.pi * tt / d)
        Y.place(out, caw, at, 0.7 - k * 0.15)
    return Y.reverb(Y.lp(Y.norm(out, 0.5), 3000), 3.0, 0.7, "sfcrow")


def gust():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.bp(Y.pink(d, "sfgust"), 300, 5000) * np.sin(np.pi * t / d) ** 2
    x += Y.hp(Y.white(d, "sfgust2"), 4000) * np.sin(np.pi * t / d) ** 4 * 0.2
    return Y.norm(x, 0.5)


sound("ambient.static_fields", [("ambient/static_fields", wind_loop)], stream=True)
sound("ambient.static_fields.additions",
      [("ambient/static_fields_channel", channel_change), ("ambient/static_fields_tone", test_tone),
       ("ambient/static_fields_crow", crow), ("ambient/static_fields_gust", gust)],
      subtitle="Something on the wind")
sound("event.static_fields_tv", [("event/static_fields_tv", tv_static)], subtitle="Television static")
sound("event.static_fields_burst", [("event/static_fields_burst", burst)], subtitle="Static, right behind you")
sound("event.static_fields_sting", [("event/static_fields_sting", sting)], subtitle="The picture changes")

level("static_fields", fog="#9a9d98", sky="#9a9d98", ambient="ambient.static_fields",
      additions="ambient.static_fields.additions", additions_chance=0.0035, fixed_time=6000, skylight=True,
      ceiling=False, height=96)
