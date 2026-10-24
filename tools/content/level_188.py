"""Level 188 - The Courtyard of Windows. A courtyard at night, walled in by a facade of windows that never ends."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, cross, flicker_light, pane, custom, \
    entity_model, entity_tex, lang, ITEM_MODELS, pillar
import tex as T
import synth as Y
from content.deepend_font import draw
from content.deepend_audio import wrapverb, fit
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas, skin

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def stucco(seed, lo="#9a8e7c", hi="#ada190"):
    img = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.7 + T.noise(seed + "f", sigma=0.5) * 0.3, lo, hi)
    # long rain streaks from the window sills above
    streak = T.noise(seed + "st", shape=(1, S), sigma=0.8)
    streak = np.repeat(streak, S, axis=0)
    img = T.shade(img, 1 - np.clip(streak - 0.55, 0, 1) * 0.5)
    return T.grain(T.stains(img, seed, "#6a6050", count=1, strength=0.2, size=8), seed, 0.03)


def facade():
    return stucco("cfac")


def facade_ledge():
    img = stucco("cled", "#b2a898", "#c0b6a6")
    img = T.shade(img, 1 - (yy >= 26) * 0.35 - (yy <= 2) * 0.1)
    img[25] = T.hexc("#d0c8b8")
    return img


def facade_base():
    img = T.ramp(T.fbm("cbase", octaves=3, base=4), "#5a554e", "#6a645c")
    joints = ((yy % 16) == 0) | (((xx + (yy // 16) * 8) % 16) == 0)
    return T.grain(T.shade(img, 1 - joints * 0.3), "cbase", 0.04)


def glass():
    """Old glass, dark, catching a little of the sky. Mostly it shows what is behind it."""
    t = (xx + yy) / 62.0
    rgb = T.ramp(t * 0.6 + T.noise("cglass", sigma=4) * 0.4, "#26364a", "#3a4c62", "#56687e")
    a = np.ones((S, S)) * 0.32
    streak = np.clip(1 - np.abs(((xx - yy) % 32) - 8) / 2.5, 0, 1)
    rgb = np.clip(rgb + streak[..., None] * 0.15, 0, 1)
    a = np.clip(a + streak * 0.15, 0, 1)
    frame = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29)
    rgb[frame] = T.hexc("#2a2622")
    a[frame] = 1
    return rgb, a


def glass_edge():
    return T.solid("#2a2622")


def room(kind):
    def f():
        if kind == "curtain":
            base = T.ramp(T.fbm("crc", octaves=2, base=3) * 0.3 + 0.5, "#c86a22", "#e88a3a", "#f8b060")
            folds = 0.75 + 0.25 * np.sin(xx / 32 * np.pi * 6) ** 2
            img = T.shade(base, folds)
        elif kind == "blinds":
            img = T.ramp(T.noise("crb", sigma=3) * 0.3 + 0.5, "#e8d890", "#fff0b0")
            img = T.shade(img, 1 - ((yy % 4) == 0) * 0.45)
        elif kind == "lamp":
            d = np.sqrt((xx - 9) ** 2 + (yy - 12) ** 2)
            img = T.ramp(np.clip(1 - d / 26, 0, 1), "#4a3020", "#b07a40", "#f8d8a0")
            img = T.rect(img, 18, 6, 28, 15, "#3a2a1a")   # a picture frame
            img = T.rect(img, 19, 7, 27, 14, "#8a7a5a")
        elif kind == "dark":
            img = T.ramp(T.noise("crd", sigma=3), "#07080c", "#101218")
            img = T.rect(img, 4, 18, 18, 32, "#0c0d10")   # the edge of a chair
        elif kind == "red":
            img = T.ramp(T.fbm("crr", octaves=2, base=3) * 0.3 + 0.5, "#8a0a0a", "#c81a14", "#f03020")
            img = T.shade(img, 0.8 + 0.2 * np.sin(xx / 32 * np.pi * 5) ** 2)
        elif kind == "tv":
            img = T.ramp(T.noise("crtv", sigma=3), "#4a70c8", "#8ab0ff")
        elif kind == "tv_off":
            img = T.ramp(T.noise("crtvo", sigma=3), "#0a0c14", "#141a28")
        elif kind in ("sil_top", "sil_bottom"):
            base = T.ramp(T.fbm("crs", octaves=2, base=3) * 0.3 + 0.5, "#c86a22", "#e88a3a", "#f8b060")
            img = T.shade(base, 0.75 + 0.25 * np.sin(xx / 32 * np.pi * 6) ** 2)
            if kind == "sil_top":
                head = ((xx - 15.5) / 5.5) ** 2 + ((yy - 14) / 7) ** 2 < 1
                shoulders = (yy > 24) & (np.abs(xx - 15.5) < 4 + (yy - 24) * 1.2)
                neck = (yy > 19) & (np.abs(xx - 15.5) < 3)
                m = (head | shoulders | neck).astype(float)
            else:
                m = (np.abs(xx - 15.5) < 10 - yy * 0.08).astype(float)
            soft = T.noise("crssoft", sigma=0.6) * 0.15
            img = T.blend(img, T.solid("#1a0e06"), np.clip(m * 0.92 - soft, 0, 1))
        elif kind == "help":
            img = T.ramp(T.noise("crh", sigma=3) * 0.3 + 0.5, "#d8d0b0", "#f0e8c8")
            img = draw(img, "HELP", "#1a1410", scale=2, jitter_seed="crhelp")
        else:
            img = T.solid("#000000")
        return T.grain(img, "room" + kind, 0.03)
    return f


def grass():
    img = T.ramp(T.fbm("cgr", octaves=4, base=3) * 0.6 + T.noise("cgrn", sigma=0.4) * 0.4, "#1e2a16", "#2c3a1e", "#3a4a26")
    blades = (T.rng("cgrb").random((S, S)) > 0.85).astype(float)
    return T.grain(T.shade(img, 1 + blades * 0.25), "cgr", 0.04)


def grass_side():
    img = T.ramp(T.fbm("cgrs", octaves=3, base=3), "#2a2018", "#3a2c20")
    top = grass()
    img[:5] = top[:5]
    return img


def path():
    img = T.ramp(T.fbm("cpath", octaves=3, base=4), "#6a6660", "#7c7870")
    joints = ((yy % 16) == 0) | ((xx % 16) == 0)
    img = T.shade(img, 1 - joints * 0.3)
    moss = (T.noise("cpm", sigma=1) > 0.7) & joints
    img = T.blend(img, T.solid("#3a4a26"), moss.astype(float) * 0.7)
    return T.grain(img, "cpath", 0.04)


def fountain_stone():
    img = T.ramp(T.fbm("cfs", octaves=3, base=3), "#8a8478", "#a09a8c")
    img = T.stains(img, "cfss", "#3a4a3a", count=2, strength=0.4, size=7)
    return T.grain(img, "cfs", 0.04)


def leaves():
    img = T.ramp(T.fbm("cleaf", octaves=3, base=2), "#2a2214", "#4a3a1c", "#6a4a20")
    return T.grain(T.shade(img, 0.9 + T.noise("cleafs", sigma=0.6) * 0.2), "cleaf", 0.06)


def drain():
    img = T.solid("#2a2a28")
    slots = ((xx % 4) < 2) & (yy > 3) & (yy < 28) & (xx > 3) & (xx < 28)
    img = T.blend(img, T.solid("#000000"), slots.astype(float))
    return T.border(img, "#4a4a46", 2)


def hedge():
    img = T.ramp(T.fbm("chedge", octaves=4, base=2), "#0e1a0c", "#1a2a14", "#2a3a1c")
    holes = (T.noise("chh", sigma=0.6) > 0.8).astype(float)
    return T.grain(T.shade(img, 1 - holes * 0.5), "chedge", 0.05)


def bench_wood():
    img = T.ramp(T.fbm("cbw", octaves=3, base=3), "#4a3420", "#5e442a")
    return T.grain(T.shade(img, 1 - ((xx % 8) == 0) * 0.3), "cbw", 0.04)


def iron():
    return T.grain(T.ramp(T.noise("cir", sigma=1), "#1a1c1e", "#2a2c30"), "cir", 0.04)


def lamp_glass():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.ramp(np.clip(1 - d / 22, 0, 1), "#f0c070", "#ffe8b0", "#fffbe8")


def bark():
    img = T.ramp(T.fbm("cbark", octaves=3, base=2, shape=(S, S)), "#1e1814", "#2e2620")
    img = T.shade(img, 1 - ((xx % 6) < 1) * 0.3)
    return T.grain(img, "cbark", 0.05)


def branches():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("cbranch")

    def limb(x, y, ang, ln, w):
        for i in range(ln):
            xi, yi = int(x), int(y)
            if 0 <= xi < S and 0 <= yi < S:
                rgb[yi, max(0, xi - w // 2):xi + w // 2 + 1] = T.hexc("#1e1814")
                a[yi, max(0, xi - w // 2):xi + w // 2 + 1] = 1
            x += np.cos(ang)
            y -= np.sin(ang)
            ang += (r.random() - 0.5) * 0.4
            if i == ln // 2 and ln > 6:
                limb(x, y, ang + (r.random() - 0.5) * 1.6, ln // 2, max(1, w - 1))
    limb(15.5, 31, np.pi / 2, 30, 2)
    limb(15.5, 22, np.pi / 4, 14, 1)
    limb(15.5, 18, 3 * np.pi / 4, 14, 1)
    return rgb, a


def door_tex(top):
    def f():
        img = T.ramp(T.fbm("cdoor", octaves=3, base=3), "#2a1a12", "#3a2618")
        img = T.border(img, "#140c08", 2)
        if top:
            img = T.rect(img, 6, 6, 26, 26, "#0a0a0c")
            img = T.rect(img, 8, 8, 24, 24, "#1a2230")
        else:
            img = T.rect(img, 6, 4, 26, 18, "#20140c")
            img = T.rect(img, 24, 0, 27, 3, "#a89060")
        return T.grain(img, "cdoor" + str(top), 0.04)
    return f


def door_ajar():
    img = T.solid("#020203")
    img[:, 26:] = T.ramp(T.fbm("cdaj", octaves=2, base=3)[:, 26:], "#2a1a12", "#3a2618")
    img[:, 25] = T.hexc("#120a06")
    return T.grain(img, "cdaj", 0.02)


tex("c188_facade", facade)
tex("c188_ledge", facade_ledge)
tex("c188_base", facade_base)
tex("c188_glass", glass)
tex("c188_glass_edge", glass_edge)
ROOMS = ["curtain", "blinds", "lamp", "dark", "red", "tv", "tv_off", "sil_top", "sil_bottom", "help"]
for k in ROOMS:
    tex(f"c188_room_{k}", room(k))
tex("c188_grass", grass)
tex("c188_grass_side", grass_side)
tex("c188_path", path)
tex("c188_fountain", fountain_stone)
tex("c188_leaves", leaves)
tex("c188_drain", drain)
tex("c188_hedge", hedge)
tex("c188_bench", bench_wood)
tex("c188_iron", iron)
tex("c188_lamp", lamp_glass)
tex("c188_bark", bark)
tex("c188_branches", branches)
tex("c188_door", door_tex(False))
tex("c188_door_top", door_tex(True))
tex("c188_door_ajar", door_ajar)

cube("courtyard_facade", "c188_facade", display="Courtyard Facade")
cube("courtyard_ledge", "c188_ledge", display="Courtyard Ledge")
cube("courtyard_facade_base", "c188_base", display="Courtyard Plinth")
pane("courtyard_window", "c188_glass", edge="c188_glass_edge", display="Courtyard Window", sound="glass",
     render="translucent")
LIT = {"curtain": 9, "blinds": 10, "lamp": 8, "red": 7, "sil_top": 9, "sil_bottom": 9, "help": 6}
for k in ROOMS:
    if k in ("tv", "tv_off"):
        continue
    if k in LIT:
        cube(f"courtyard_room_{k}", f"c188_room_{k}", display="Lit Window Room", light=LIT[k], emissive=True,
             flat=True)
    else:
        cube(f"courtyard_room_{k}", f"c188_room_{k}", display="Dark Window Room", flat=True)
flicker_light("courtyard_room_tv", "c188_room_tv", "c188_room_tv_off", "c188_room_tv", light=7, flicker=0.7,
              face="all", display="TV-Lit Window Room")
cube_faces("courtyard_grass", top="c188_grass", bottom="c188_grass_side", side="c188_grass_side",
           display="Courtyard Lawn", sound="grass")
cube("courtyard_path", "c188_path", display="Courtyard Path")
cube("courtyard_fountain_stone", "c188_fountain", display="Fountain Stone")
shaped("courtyard_dead_leaves", [(0, 0, 0, 16, 1, 16, "l")], {"l": "c188_leaves"}, display="Dead Leaves",
       collides=False, sound="grass", unbreakable=True)
exit_block("courtyard_drain", "c188_drain", "FALL", display="Fountain Drain", sound="metal")
cube("courtyard_hedge", "c188_hedge", display="Courtyard Hedge", sound="grass", render="cutout")
shaped("courtyard_bench", [
    (0, 6, 4, 16, 7, 12, {"all": "iron", "up": "wood"}), (0, 7, 11, 16, 13, 12.5, "wood"),
    (1, 0, 5, 2, 6, 11, "iron"), (14, 0, 5, 15, 6, 11, "iron"), (1, 6, 11, 2, 14, 12, "iron"), (14, 6, 11, 15, 14, 12, "iron"),
], {"wood": "c188_bench", "iron": "c188_iron"}, display="Courtyard Bench", unbreakable=True, sound="wood")
shaped("courtyard_lamp_post", [(7, 0, 7, 9, 16, 9, "iron"), (6, 0, 6, 10, 2, 10, "iron")], {"iron": "c188_iron"},
       display="Lamp Post", unbreakable=True, sound="metal")
shaped("courtyard_lamp_head", [
    (7, 0, 7, 9, 3, 9, "iron"), (5, 3, 5, 11, 10, 11, {"all": "glass", "up": "iron", "down": "iron"}),
    (4, 10, 4, 12, 11, 12, "iron"), (6, 11, 6, 10, 12, 10, "iron"),
], {"iron": "c188_iron", "glass": "c188_lamp"}, display="Lamp", light=14, emissive=True, unbreakable=True,
    sound="glass", collides=True)
pillar("courtyard_dead_trunk", "c188_bark", "c188_bark", display="Dead Tree")
cross("courtyard_dead_branches", "c188_branches", display="Dead Branches", sound="wood")
custom("courtyard_door", "com.noclip.block.CourtyardDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/c188_door"}},
       display="Locked Door", unbreakable=True, sound="wood")
custom("courtyard_door_top", "com.noclip.block.CourtyardDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/c188_door_top"}},
       display="Locked Door", unbreakable=True, sound="wood")
exit_block("courtyard_door_ajar", "c188_door_ajar", "DOOR", display="A Door, Ajar", sound="wood")

# ------------------------------------------------------------------ the watchers

def watcher_tex():
    c = Canvas()
    coat = skin("ww", "#16161a", "#22222a", sigma=0.8)
    pale = skin("wwp", "#d8d4cc", "#e8e4dc", sigma=1.2)

    def face(w, h):
        img = pale(w, h)
        img[2:5, 1:3] = T.hexc("#050505")
        img[2:5, 5:7] = T.hexc("#050505")
        img[0:1, :] = T.hexc("#101010")
        return img

    def head(w, h):
        img = pale(w, h)
        img[0:2, :] = T.hexc("#101010")
        return img

    def arm(w, h):
        img = coat(w, h)
        img[-3:] = T.hexc("#d8d4cc")
        return img

    paint_humanoid(c, head, coat, arm, coat, face=face)
    c.box(0, 0, 8, 8, 8, up=lambda w, h: T.solid("#101010", shape=(h, w)))
    return c.out()


entity_model("window_watcher", humanoid(arm_len=17, leg_len=14))
entity_tex("window_watcher", watcher_tex)
lang("entity.noclip.window_watcher", "Resident")
lang("item.noclip.window_watcher_spawn_egg", "Resident Spawn Egg")
ITEM_MODELS["window_watcher_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def courtyard_air():
    """Night air moving around a closed courtyard, a building's hum, and a TV in some flat, muffled to murmur."""
    d = 28.0
    t = Y.t_axis(d)
    n = len(t)
    wind = Y.bp(Y.pink(d, "cwind"), 80, 900) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 14.0) ** 2) * 0.6
    tv = np.zeros(n)
    for k in range(4):
        tv += fit(Y.whisper(f"ctv{k}", d), n)
    tv = Y.lp(Y.hp(tv, 200), 700) * 0.3 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 7.0))
    hum = Y.hum(d, 50.0, "chum", buzz=0.03, harmonics=(1, 2)) * 0.05
    x = wrapverb(wind + tv, 3.0, 0.5, "cair", damp=2000) + Y.lp(hum, 200)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.42), 1.5), 0.003), "stereo"


def window_slam():
    out = np.zeros(int(3.5 * Y.SR))
    Y.place(out, Y.impact("cslam", 0.5), 0.0)
    rattle = Y.bp(Y.white(0.4, "crat"), 2000, 8000) * np.exp(-np.arange(int(0.4 * Y.SR)) / Y.SR * 12)
    Y.place(out, rattle, 0.02, 0.5)
    return Y.reverb(Y.lp(out, 3000), 3.5, 0.75, "cslam")


def phone_ring():
    """A phone ringing in one of the flats. Nobody answers it."""
    d = 5.0
    t = Y.t_axis(d)
    ring = (np.sin(2 * np.pi * 440 * t) + np.sin(2 * np.pi * 480 * t)) * 0.3
    gate = ((t % 3.0) < 1.0) * (np.sin(2 * np.pi * 20 * t) > 0)
    x = Y.lp(ring * gate, 1200)
    return Y.reverb(x, 2.5, 0.7, "cphone")


def tv_laugh():
    d = 2.6
    n = int(d * Y.SR)
    x = np.zeros(n)
    for k in range(8):
        x += fit(Y.whisper(f"claugh{k}", d), n)
    t = Y.t_axis(d)
    x *= (1 + 0.8 * np.sin(2 * np.pi * 5 * t)) * np.sin(np.pi * t / d)
    return Y.reverb(Y.lp(Y.hp(x, 300), 1100), 2.5, 0.6, "claugh")


def curtain():
    d = 1.6
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "ccurt"), 800, 5000) * np.sin(np.pi * t / d) ** 3
    return Y.reverb(Y.norm(x, 0.4), 2.0, 0.6, "ccurt")


def glass_knock():
    out = np.zeros(int(1.6 * Y.SR))
    for k in range(3):
        n = int(0.12 * Y.SR)
        tt = np.arange(n) / Y.SR
        k1 = Y.bp(Y.white(0.12, f"ck{k}"), 1500, 6000)[:n] * np.exp(-tt * 60)
        k1 += np.sin(2 * np.pi * 1900 * tt) * np.exp(-tt * 40) * 0.4
        Y.place(out, k1, 0.05 + k * 0.32, 0.9)
    return Y.norm(Y.reverb(out, 1.4, 0.4, "cknock"), 0.9)


def glass_break():
    d = 2.0
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("cbreak")
    for k in range(40):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        sh = np.sin(2 * np.pi * (2000 + r.random() * 6000) * tt) * np.exp(-tt * 60)
        Y.place(out, sh, r.random() * 0.8, 0.3 + r.random() * 0.4)
    Y.place(out, Y.bp(Y.white(0.5, "cbr2"), 1500, 10000) * np.exp(-np.arange(int(0.5 * Y.SR)) / Y.SR * 8), 0.0, 0.8)
    return Y.norm(Y.reverb(out, 1.5, 0.4, "cbreak"), 0.95)


def locked_rattle():
    out = np.zeros(int(1.2 * Y.SR))
    for k in range(4):
        Y.place(out, Y.impact(f"crattle{k}", 0.15, ring=0.3), 0.05 + k * 0.18, 0.6)
    return Y.norm(Y.reverb(out, 0.8, 0.3, "crattle"), 0.8)


sound("ambient.level_188", [("ambient/level_188", courtyard_air)], stream=True)
sound("ambient.level_188.additions",
      [("ambient/level_188_slam", window_slam), ("ambient/level_188_phone", phone_ring),
       ("ambient/level_188_laugh", tv_laugh), ("ambient/level_188_curtain", curtain)],
      subtitle="Something in one of the windows")
sound("entity.window_watcher.knock", [("entity/window_watcher_knock", glass_knock)], subtitle="Knocking on glass")
sound("entity.window_watcher.break", [("entity/window_watcher_break", glass_break)], subtitle="Glass breaks")
sound("event.level_188_locked", [("event/level_188_locked", locked_rattle)], subtitle="The handle will not turn")

level("level_188", fog="#0d1424", sky="#0a1020", ambient="ambient.level_188", additions="ambient.level_188.additions",
      additions_chance=0.004, fixed_time=18000, skylight=True, ceiling=False, height=256)
