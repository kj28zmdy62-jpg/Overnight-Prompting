"""Golf Course at Dusk. Fairways forever under a violet sky, sprinklers ticking, a flag in
every hole, and far off a clubhouse with its lights on. When the sprinklers stop, the
groundskeeper is out. Keep off the grass."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, custom, exit_block, level, sound, lang, entity_model, \
    entity_tex, ITEM_MODELS, BLOCK_MODELS, stairs
import assets as A
import tex as T
import synth as Y
from content.outside_kit import sliced, draw_text, centered, box_model, paint_metal
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32
tx = A.tex


def mown(seed, lo, hi, stripe=True):
    img = T.ramp(T.noise(seed, sigma=0.5) * 0.5 + T.fbm(seed + "f", octaves=3, base=6) * 0.5, lo, hi)
    if stripe:
        xx = np.mgrid[0:S, 0:S][1]
        img = T.shade(img, 1 + ((xx // 16) % 2) * 0.07)
    return T.grain(img, seed, 0.03)


def rough():
    img = T.ramp(T.noise("gcr", sigma=0.4) * 0.6 + T.fbm("gcrf", octaves=3, base=4) * 0.4, "#1e3016", "#2a3e1e", "#38502a")
    return T.grain(img, "gcr", 0.06)


def sand():
    img = T.ramp(T.noise("gcsand", sigma=0.5) * 0.6 + T.fbm("gcsf", octaves=3, base=5) * 0.4, "#b8aa88", "#c8bc9a", "#d4c8a8")
    yy, xx = np.mgrid[0:S, 0:S]
    rake = (np.sin(xx * 1.2 + np.sin(yy * 0.2) * 2) > 0.7).astype(float) * 0.06
    return T.grain(T.shade(img, 1 - rake), "gcsand", 0.04)


def soil():
    return T.grain(T.ramp(T.fbm("gcsoil", octaves=3, base=3), "#2a2218", "#362c20"), "gcsoil", 0.05)


tx("gc_fairway", lambda: mown("gcfw", "#3a6a2a", "#4a7a34"))
tx("gc_green", lambda: mown("gcgr", "#4a8a3a", "#5a9a44", stripe=False))
tx("gc_tee", lambda: mown("gctee", "#3e7030", "#4c7e38"))
tx("gc_rough", rough)
tx("gc_sand", sand)
tx("gc_soil", soil)
tx("gc_turf_side", lambda: T.blend(soil(), rough(), (np.mgrid[0:S, 0:S][0] < 5).astype(float)))
for name, top, disp in (("gc_fairway", "gc_fairway", "Fairway"), ("gc_green", "gc_green", "Putting Green"),
                        ("gc_tee", "gc_tee", "Tee Box"), ("gc_rough", "gc_rough", "Rough")):
    cube_faces(name, top=top, bottom="gc_soil", side="gc_turf_side", sound="grass", display=disp)
cube("gc_sand", "gc_sand", sound="sand", display="Bunker Sand")
cube("gc_soil", "gc_soil", sound="gravel", display="Soil")


def long_grass():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("gclg")
    for _ in range(24):
        x0, h = 2 + r.random() * 28, 8 + r.random() * 16
        lean = (r.random() - 0.5) * 0.5
        for y in range(int(h)):
            px = int(x0 + lean * y)
            if 0 <= px < S:
                rgb[S - 1 - y, px] = T.hexc("#2e4422") * (0.75 + 0.3 * y / h)
                a[S - 1 - y, px] = 1
    return rgb, a


tx("gc_long_grass", long_grass)
cross("gc_long_grass", "gc_long_grass", display="Long Grass")


def bark():
    img = T.ramp(T.fbm("gcbark", octaves=3, base=2), "#2a2018", "#3a2e22")
    xx = np.mgrid[0:S, 0:S][1]
    return T.grain(T.shade(img, 1 - (np.sin(xx * 1.5 + T.noise("gcbn", sigma=2) * 5) > 0.6) * 0.2), "gcbark", 0.05)


def needles():
    base = T.ramp(T.noise("gcndl", sigma=0.6), "#16261a", "#1e3222", "#284030")
    a = (T.rng("gcndla").random((S, S)) > 0.15).astype(float)
    return base, a


tx("gc_bark", bark)
tx("gc_needles", needles)
from assets import pillar
pillar("gc_trunk", "gc_bark", "gc_bark", display="Pine Trunk", sound="wood")
cube("gc_needles", "gc_needles", display="Pine Needles", sound="grass", render="cutout_mipped")

# ------------------------------------------------------------------ the pins


def cloth(c1, c2):
    def f():
        rgb = np.ones((S, S, 3)) * T.hexc(c1)
        yy, xx = np.mgrid[0:S, 0:S]
        rgb = T.shade(rgb, 0.9 + np.sin(xx / 3.0) * 0.08)
        rgb = centered(rgb, "?", 12, c2, scale=2)
        return rgb
    return f


tx("gc_flag_red", cloth("#c0201a", "#f0e8d8"))
tx("gc_flag_pole", lambda: T.grain(T.ramp(T.noise("gcfp", sigma=1), "#d8d4c8", "#ecE8dc".lower()), "gcfp", 0.02))
tx("gc_cup_dark", lambda: T.solid("#020202"))
POLE = [(7.25, 0, 7.25, 8.75, 16, 8.75, "p")]
custom("gc_flagstick", "com.noclip.block.FlagstickBlock", box_model(POLE, {"p": "gc_flag_pole"}),
       display="Flagstick", sound="wood", unbreakable=True, render="cutout")
# the cloth streams out to the north before rotation; placed facing the clubhouse it points the way
shaped("gc_flag", [(7.25, 0, 7.25, 8.75, 12, 8.75, "p"), (8, 4, 0, 8.01, 12, 7.25, {"all": None, "east": "c!", "west": "c!"})],
       {"p": "gc_flag_pole", "c": "gc_flag_red"}, display="Pin Flag", unbreakable=True, collides=False)
exit_block("gc_cup", "gc_cup_dark", "FALL", display="The Cup",
           model=box_model([(0, 0, 0, 16, 16, 16, {"all": "g", "up": "h!"})], {"g": "gc_green", "h": "gc_cup_hole"}))


def cup_hole():
    img = mown("gccup", "#4a8a3a", "#5a9a44", stripe=False)
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img[d < 8] = T.hexc("#020202")
    img[(d >= 8) & (d < 9)] = T.hexc("#e8e4dc")
    return img


tx("gc_cup_hole", cup_hole)

# ------------------------------------------------------------------ course furniture


def sprinkler():
    return paint_metal("gcspr", "#2a2a2a", "#4a4a48", rust=0.2)


tx("gc_sprinkler", sprinkler)
shaped("gc_sprinkler", [(6, 0, 6, 10, 3, 10, "s"), (7, 3, 7, 9, 5, 9, "s"), (8, 4, 6, 13, 5, 7, "s")],
       {"s": "gc_sprinkler"}, display="Impact Sprinkler", collides=False, unbreakable=True, sound="metal")


def marker(colour):
    def f():
        img = T.ramp(T.noise("gcmk" + colour, sigma=1), colour, colour)
        return T.grain(img, "gcmk" + colour, 0.03)
    return f


tx("gc_marker_blue", marker("#2a4aa8"))
tx("gc_marker_white", marker("#e8e4dc"))
shaped("gc_tee_marker", [(6, 0, 6, 10, 3, 10, "m")], {"m": "gc_marker_blue"}, display="Tee Marker", collides=False,
       unbreakable=True)


def bench_wood():
    img = T.ramp(T.fbm("gcbw", octaves=3, base=2), "#4a3424", "#5a4030")
    yy = np.mgrid[0:S, 0:S][0]
    return T.grain(T.shade(img, 1 - ((yy % 8) == 0) * 0.4), "gcbw", 0.04)


tx("gc_bench", bench_wood)
shaped("gc_bench", [(1, 0, 4, 2, 7, 12, "w"), (14, 0, 4, 15, 7, 12, "w"), (0, 6, 4, 16, 7, 11, "w"),
                    (0, 7, 11, 16, 14, 12, "w")], {"w": "gc_bench"}, display="Course Bench", unbreakable=True)


def cart_body():
    img = T.ramp(T.noise("gccart", sigma=2), "#d8d4c8", "#e4e0d4")
    return T.grain(T.stains(img, "gccarts", "#8a8a7a", count=1, strength=0.3, size=7), "gccart", 0.03)


tx("gc_cart", cart_body)
tx("gc_cart_roof", lambda: T.grain(T.solid("#1e4a2a"), "gccr", 0.04))
tx("gc_tyre", lambda: T.grain(T.solid("#141414"), "gcty", 0.04))
shaped("gc_cart_front", [(1, 2, 0, 15, 8, 16, "b"), (0, 0, 3, 2, 4, 7, "t"), (14, 0, 3, 16, 4, 7, "t"),
                         (2, 8, 12, 14, 9, 16, "b")], {"b": "gc_cart", "t": "gc_tyre"},
       display="Golf Cart", unbreakable=True, sound="metal")
shaped("gc_cart_back", [(1, 2, 0, 15, 6, 16, "b"), (2, 6, 2, 14, 10, 8, "b"), (2, 10, 0, 14, 11, 9, "b"),
                        (0, 0, 9, 2, 4, 13, "t"), (14, 0, 9, 16, 4, 13, "t"), (1.5, 6, 14, 2.5, 16, 15, "b"),
                        (13.5, 6, 14, 14.5, 16, 15, "b"), (0, 15, 0, 16, 16, 16, "r")],
       {"b": "gc_cart", "t": "gc_tyre", "r": "gc_cart_roof"}, display="Golf Cart", unbreakable=True, sound="metal")


def keep_off():
    img = T.solid("#1e4a2a")
    img = T.border(img, "#e8e4dc", 1)
    img = centered(img, "KEEP", 4, "#e8e4dc")
    img = centered(img, "OFF", 11, "#e8e4dc")
    img = centered(img, "GRASS", 18, "#e8e4dc")
    return img


tx("gc_keep_off", keep_off)
shaped("gc_sign", [(7.5, 0, 7.5, 8.5, 8, 8.5, "p"), (3, 8, 7.5, 13, 16, 8.5, {"all": "p", "north": "s!", "south": "s!"})],
       {"p": "gc_flag_pole", "s": "gc_keep_off"}, display="Keep Off the Grass", unbreakable=True)

# ------------------------------------------------------------------ the clubhouse


def club_wall():
    img = T.ramp(T.noise("gcclw", sigma=2), "#d8d0bc", "#e4dcc8")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.1)
    return T.grain(img, "gcclw", 0.03)


def club_window():
    img = T.solid("#ffd890")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1.0 - yy / 120)
    curtain = (xx < 6) | (xx > 25)
    img[curtain] = T.hexc("#c89a5a")
    img = T.border(img, "#2a2e26", 2)
    img[15:17, :] = T.hexc("#2a2e26")
    return img


def club_door():
    img = T.ramp(T.noise("gccd", sigma=2), "#2a3a2a", "#344634")
    img = T.border(img, "#c8a050", 2)
    img = T.rect(img, 8, 6, 24, 16, "#ffd890")
    img = T.rect(img, 22, 20, 25, 23, "#c8a050")
    return img


def club_sign():
    w, h = 4 * S, S
    img = np.ones((h, w, 3)) * T.hexc("#1e3a26")
    img = T.border(img, "#c8a050", 2)
    img = centered(img, "CLUBHOUSE", 6, "#e8d8a8", scale=2)
    img = centered(img, "MEMBERS ONLY", 22, "#c8a050")
    return img


tx("gc_club_wall", club_wall)
tx("gc_club_window", club_window)
tx("gc_club_door", club_door)
tx("gc_club_roof", lambda: T.grain(T.ramp(T.noise("gccrf", sigma=0.8), "#2a2e34", "#3a3e44"), "gccrf", 0.05))
sliced("gc_club_sign", club_sign, 4, 1)
cube("gc_club_wall", "gc_club_wall", display="Clubhouse Wall")
cube("gc_club_window", "gc_club_window", display="Lit Window", light=11, emissive=True, flat=True, sound="glass")
cube("gc_club_roof", "gc_club_roof", display="Clubhouse Roof")
stairs("gc_club_roof_stairs", "gc_club_roof", "gc_club_roof", display="Clubhouse Roof")
exit_block("gc_club_door", "gc_club_door", "DOOR", trigger="USE", solid=True, display="Clubhouse Door", sound="wood",
           light=6)
for c in range(4):
    shaped(f"gc_club_sign_{c}", [(0, 0, 6, 16, 16, 10, {"all": "w", "north": "s"})],
           {"w": "gc_club_wall", "s": f"gc_club_sign_{c}_0"}, display="Clubhouse Sign", light=8, emissive=True,
           unbreakable=True, render="solid")
tx("gc_lamp", lambda: T.solid("#ffe0a0"))
shaped("gc_porch_lamp", [(6, 0, 6, 10, 6, 10, {"all": "l"}), (7, 6, 7, 9, 16, 9, "p")], {"l": "gc_lamp", "p": "gc_bench"},
       display="Porch Lamp", light=13, emissive=True, unbreakable=True, collides=False)

# ------------------------------------------------------------------ the groundskeeper


def keeper_tex():
    c = Canvas()
    sk = lambda w, h: T.ramp(T.noise(f"gck{w}{h}", shape=(h, w), sigma=1), "#8a7a6a", "#9a8a78")

    def face(w, h):
        img = T.ramp(T.noise("gckf", shape=(h, w), sigma=1.5), "#7a6a5a", "#8a7a68")
        img[:2] = T.hexc("#3a5a3a")
        img[2, :] = T.hexc("#24301e")
        return img

    def head(w, h):
        img = sk(w, h)
        img[:3] = T.hexc("#3a5a3a")
        return img

    def overalls(w, h):
        img = T.grain(T.ramp(T.noise(f"gco{w}{h}", shape=(h, w), sigma=1), "#2a3e5a", "#344a6a"), f"gco{w}", 0.05)
        if w == 8 and h == 12:
            img[:4, 1:3] = T.hexc("#6a5a3a")
            img[:4, 5:7] = T.hexc("#6a5a3a")
            img[6:8, 2:6] = T.hexc("#2a3a52")
        img = T.stains(img, f"gcos{w}{h}", "#3a3a2a", count=1, strength=0.4, size=3)
        return img

    def arm(w, h):
        img = T.grain(T.ramp(T.noise(f"gca{w}{h}", shape=(h, w), sigma=1), "#6a5a3a", "#7a6a48"), f"gca{w}", 0.05)
        img[-3:] = T.hexc("#3a2a1a")
        return img

    def boots(w, h):
        img = overalls(w, h)
        img[-3:] = T.hexc("#1a1612")
        return img

    paint_humanoid(c, head, overalls, arm, boots, face=face)
    c.box(0, 0, 8, 8, 8, up=lambda w, h: T.grain(T.solid("#3a5a3a", shape=(h, w)), "gckcap", 0.05))
    c.box(24, 0, 8, 1, 4, all=lambda w, h: T.solid("#2e4a2e", shape=(h, w)))
    c.box(56, 16, 1, 16, 1, all="#5a4630")
    c.box(48, 40, 5, 1, 1, all="#6a6a66")
    return c.out()


parts = humanoid(head_extra=[(24, 0, -4, -8.5, -8, 8, 1, 4)], arm_len=13, leg_len=14)
# a rake held upright in the right hand
from assets import part
parts[2]["cubes"] = parts[2]["cubes"] + [(56, 16, -1, 4, -4, 1, 16, 1), (48, 40, -3, 19.5, -5, 5, 1, 1)]
entity_model("gc_groundskeeper", parts)
entity_tex("gc_groundskeeper", keeper_tex)
lang("entity.noclip.gc_groundskeeper", "The Groundskeeper")
lang("item.noclip.gc_groundskeeper_spawn_egg", "The Groundskeeper Spawn Egg")
ITEM_MODELS["gc_groundskeeper_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def dusk_course():
    d = 26.0
    t = Y.t_axis(d)
    r = Y.rng("gcfrog")
    frogs = np.zeros_like(t)
    for k in range(5):
        at = r.random() * (d - 1)
        n = int(0.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        croak = np.sign(np.sin(2 * np.pi * (140 + r.random() * 60) * tt)) * (np.sin(2 * np.pi * 25 * tt) > 0) * np.sin(np.pi * tt / 0.5)
        Y.place(frogs, Y.lp(croak, 900) * 0.05, at)
    crick = np.zeros_like(t)
    for k in range(5):
        f = round((4400 + r.random() * 900) * d) / d
        rate = round((2 + r.random() * 2) * d) / d
        crick += np.sin(2 * np.pi * f * t) * (np.sin(2 * np.pi * rate * t + k) > 0.4) * 0.025
    wind = Y.bp(Y.pink(d, "gcwind"), 200, 1200) * 0.07
    x = Y.loopable(Y.norm(frogs + crick + wind, 0.4), 1.5)
    return Y.stereoize(x, 0.0017), "stereo"


def sprinkler_tick():
    d = 2.6
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    for k in range(10):
        n = int(0.04 * Y.SR)
        tt = np.arange(n) / Y.SR
        tick = np.sin(2 * np.pi * 2600 * tt) * np.exp(-tt * 160) + Y.white(0.04, f"gct{k}")[:n] * np.exp(-tt * 200) * 0.4
        Y.place(out, tick, 0.05 + k * 0.24, 0.6)
    spray = Y.bp(Y.white(d, "gcspray"), 2000, 9000) * 0.12
    return Y.norm(out + spray, 0.5)


def thwack():
    out = np.zeros(int(3.0 * Y.SR))
    n = int(0.08 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, (np.sin(2 * np.pi * 1800 * tt) + Y.white(0.08, "gcth")[:n]) * np.exp(-tt * 80), 0.05, 1.0)
    return Y.reverb(Y.norm(out, 0.4), 3.0, 0.8, "gcth")


def flag_clink():
    out = np.zeros(int(2.0 * Y.SR))
    n = int(1.2 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, (np.sin(2 * np.pi * 1320 * tt) + 0.5 * np.sin(2 * np.pi * 3100 * tt)) * np.exp(-tt * 6), 0.0, 0.5)
    return Y.reverb(Y.norm(out, 0.35), 2.5, 0.7, "gcclink")


def mower():
    d = 5.0
    t = Y.t_axis(d)
    env = np.sin(np.pi * t / d) ** 2
    x = np.sign(np.sin(2 * np.pi * 48 * t)) * 0.3 + Y.lp(Y.white(d, "gcmow"), 600) * 0.4
    return Y.reverb(Y.lp(Y.norm(x * env, 0.3), 1200), 3.0, 0.8, "gcmow")


def silence_drop():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.drone(d, [65.4, 69.3], "gcsil", lfo=0) * np.exp(-t * 1.2) * np.clip(t / 0.05, 0, 1)
    return Y.norm(x, 0.5)


def rake():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "gcrake"), 1500, 6000) * (np.sin(np.pi * t / 0.9) ** 2) * (t < 1.8)
    return Y.norm(x, 0.5)


sound("ambient.golf_course", [("ambient/golf_course", dusk_course)], stream=True)
sound("ambient.golf_course.additions",
      [("ambient/golf_course_thwack", thwack), ("ambient/golf_course_clink", flag_clink),
       ("ambient/golf_course_mower", mower)],
      subtitle="Something out on the course")
sound("event.golf_course_sprinkler", [("event/golf_course_sprinkler", sprinkler_tick)], subtitle="A sprinkler ticks")
sound("event.golf_course_silence", [("event/golf_course_silence", silence_drop)], subtitle="The sprinklers stop")
sound("event.golf_course_rake", [("event/golf_course_rake", rake)], subtitle="Raking, close by")

level("golf_course", fog="#3a3a5a", sky="#3a3a6a", water="#1a2a3a", water_fog="#0a1018",
      ambient="ambient.golf_course", additions="ambient.golf_course.additions", additions_chance=0.0025,
      fixed_time=13000, skylight=True, ceiling=False, height=80)
