"""Level 9 - Darkened Suburbs. The same house, over and over, at 2 a.m. Streetlights buzz over
empty roads. Behind you, one by one, they go out."""
import numpy as np
from assets import cube, cube_faces, pillar, tex, shaped, door, fence, stairs, exit_block, flicker_light, carpet, \
    level, sound
import tex as T
import synth as Y
from content.classics_kit import draw_text, centered, metal

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def grass():
    img = T.ramp(T.fbm("l9g", octaves=3, base=4) * 0.5 + T.noise("l9gb", sigma=0.4) * 0.5, "#1a2a16", "#24361e", "#2e4224")
    blades = (T.rng("l9bl").random((S, S)) > 0.88)
    return T.grain(T.blend(img, T.solid("#3a5030"), blades * 0.4), "l9g", 0.05)


def asphalt(line=None):
    def f():
        img = T.ramp(T.noise("l9a", sigma=0.5) * 0.6 + T.fbm("l9af", octaves=3, base=5) * 0.4, "#1e1e20", "#2a2a2c")
        stones = (T.rng("l9as").random((S, S)) > 0.93)
        img = T.blend(img, T.solid("#4a4a4a"), stones * 0.5)
        img = T.cracks(img, "l9ac", "#121214", 1, 0.5)
        if line == "x":
            m = (np.abs(yy - 15.5) < 1.5) & ((xx % 16) < 10)
            img = T.blend(img, T.solid("#c8a830"), m * 0.85)
        elif line == "z":
            m = (np.abs(xx - 15.5) < 1.5) & ((yy % 16) < 10)
            img = T.blend(img, T.solid("#c8a830"), m * 0.85)
        return T.grain(img, "l9a" + str(line), 0.04)
    return f


def sidewalk():
    img = T.ramp(T.fbm("l9sw", octaves=3, base=5), "#5a5a56", "#6a6a64")
    img = T.shade(img, 1 - (((xx % 32) == 0) | ((yy % 32) == 0)) * 0.35)
    img = T.cracks(img, "l9swc", "#3a3a36", 1, 0.4)
    return T.grain(img, "l9sw", 0.04)


def siding(seed="l9sd", lo="#7e8c94", hi="#8c9aa2"):
    def f():
        img = T.ramp(T.noise(seed, sigma=2), lo, hi)
        img = T.shade(img, 1 - ((yy % 8) == 7) * 0.35 + ((yy % 8) == 0) * 0.08)
        return T.grain(T.stains(img, seed + "s", "#4a5054", 1, 0.2, 9), seed, 0.03)
    return f


def window(lit=False, figure=False):
    def f():
        if lit:
            img = T.ramp(T.noise("l9wl", sigma=3), "#d89a48", "#ffd08a")
        else:
            img = T.ramp(T.noise("l9wd", sigma=3), "#05070c", "#0e1218")
            refl = np.clip(1 - np.abs((xx - yy) - 4) / 3.0, 0, 1)
            img = T.blend(img, T.solid("#3a4250"), refl * 0.25)
        # curtains drawn at the sides
        cur = "#6a2a24" if lit else "#1e1416"
        img = T.rect(img, 2, 2, 8, 30, cur)
        img = T.rect(img, 24, 2, 30, 30, cur)
        for x in (3, 5, 25, 27):
            img[2:30, x] *= 0.75
        if figure:
            d = np.sqrt((xx - 16) ** 2 + (yy - 10) ** 2)
            body = (yy > 13) & (np.abs(xx - 16) < 5 - (yy - 13) * 0.0)
            img = T.blend(img, T.solid("#0a0604"), ((d < 3.5) | body).astype(float))
        img = T.rect(img, 15, 0, 17, 32, "#e8e8e2")
        img = T.rect(img, 0, 15, 32, 17, "#e8e8e2")
        return T.border(img, "#f0f0ea", 2)
    return f


def shingles():
    img = T.ramp(T.noise("l9sh", sigma=0.8), "#26262a", "#323236")
    off = ((yy // 6) % 2) * 4
    img = T.shade(img, 1 - ((yy % 6) == 5) * 0.4 - (((xx + off) % 8) == 0) * 0.25)
    return T.grain(img, "l9sh", 0.05)


def trim():
    return T.grain(T.ramp(T.noise("l9tr", sigma=2), "#d8d8d2", "#e6e6e0"), "l9tr", 0.02)


def house_door(top, lit=False):
    def f():
        img = T.ramp(T.noise("l9dr", sigma=2), "#4a1e1a", "#5a2620")
        img = T.border(img, "#2a0e0c", 2)
        if top:
            img = T.rect(img, 7, 6, 25, 14, "#ffd08a" if lit else "#0a0c10")
            img = T.rect(img, 15, 6, 17, 14, "#4a1e1a")
        else:
            img = T.rect(img, 6, 4, 26, 26, "#4e221c")
            img = T.rect(img, 23, 2, 26, 5, "#c8a44a")
        return T.grain(img, "l9dr" + str(top), 0.03)
    return f


def porch_light(on):
    def f():
        if on:
            d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
            return T.ramp(np.clip(1 - d / 22, 0, 1), "#d8902a", "#ffd890", "#fff4dc")
        return T.ramp(T.noise("l9plo", sigma=2), "#3a3830", "#4a463a")
    return f


def lamp_head(on):
    def f():
        img = T.solid("#2a2c2e")
        if on:
            d = np.sqrt(((xx - 15.5) / 1.4) ** 2 + (yy - 15.5) ** 2)
            img = T.blend(img, T.ramp(np.clip(1 - d / 13, 0, 1), "#e89030", "#ffc860", "#fff0c0"), (d < 12).astype(float))
        else:
            d = np.sqrt(((xx - 15.5) / 1.4) ** 2 + (yy - 15.5) ** 2)
            img = T.blend(img, T.solid("#4a4440"), (d < 12).astype(float))
        return T.border(img, "#1a1c1e", 2)
    return f


def pole():
    return metal("l9pole", "#3a3c3e", "#4e5052", sigma=1.5)


def picket():
    rgb = T.ramp(T.noise("l9pk", sigma=2), "#c8c8c0", "#dcdcd4")
    return T.grain(rgb, "l9pk", 0.04)


def manhole():
    img = sidewalk()
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    iron = metal("l9mh", "#2a2a28", "#3a3a36")
    img = T.blend(img, iron, (d < 14).astype(float))
    img = T.blend(img, T.solid("#141412"), (np.abs(d - 13.5) < 1).astype(float))
    grid = ((xx % 5) == 0) | ((yy % 5) == 0)
    img = T.blend(img, T.solid("#4a4a44"), (grid & (d < 11)).astype(float) * 0.6)
    return img


def mailbox():
    return metal("l9mb", "#3a3e44", "#4a4e54", sigma=2)


def trunk_side():
    img = T.ramp(T.noise("l9tk", sigma=0.6), "#1e1812", "#2a2018")
    return T.shade(img, 1 - ((xx % 5) == 0) * 0.3)


def trunk_end():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.ramp((np.sin(d) * 0.5 + 0.5) * 0.4 + 0.3, "#2a2018", "#5a4632")


def leaves():
    rgb = T.ramp(T.noise("l9lv", sigma=0.7), "#0e1a0e", "#1a2a16")
    a = (T.noise("l9lva", sigma=0.6) > 0.32).astype(float)
    return rgb, a


def interior_floor():
    img = T.ramp(T.fbm("l9fl", octaves=2, base=3), "#4a3626", "#5a4230")
    return T.grain(T.shade(img, 1 - ((yy % 8) == 0) * 0.35), "l9fl", 0.03)


def wallpaper_in():
    img = T.ramp(T.noise("l9wi", sigma=1.5), "#b8ae94", "#c4ba9e")
    img = T.blend(img, T.solid("#9a8e70"), (((xx + yy // 4) % 8) == 0) * 0.3)
    return T.grain(img, "l9wi", 0.03)


def couch():
    return T.grain(T.ramp(T.noise("l9co", sigma=0.8), "#4a5a3a", "#566844"), "l9co", 0.05)


def tv_case():
    return T.grain(T.ramp(T.noise("l9tvc", sigma=2), "#2a2420", "#3a3028"), "l9tvc", 0.03)


def tv_screen(on):
    def f():
        if on:
            n = T.rng("l9static").random((S, S))
            img = np.stack([n, n, n], axis=2) * 0.7 + 0.2
            img = T.shade(img, 1 - ((yy % 2) == 0) * 0.2)
        else:
            img = T.ramp(T.noise("l9tvo", sigma=3), "#0e1010", "#1a1e1e")
        return T.border(img, "#141210", 2)
    return f


def basement_door():
    img = T.ramp(T.noise("l9bd", sigma=2), "#5a5a52", "#6a6a60")
    img = T.border(img, "#2a2a26", 2)
    img = T.rect(img, 22, 14, 26, 18, "#b0aca0")
    img = T.rect(img, 0, 30, 32, 32, "#ffb060")
    return T.grain(img, "l9bd", 0.04)


TEX = {"l9_grass": grass, "l9_road": asphalt(), "l9_road_line_x": asphalt("x"), "l9_road_line_z": asphalt("z"),
       "l9_sidewalk": sidewalk, "l9_siding": siding(), "l9_window": window(), "l9_window_lit": window(True),
       "l9_window_figure": window(True, True), "l9_shingles": shingles, "l9_trim": trim,
       "l9_door_top": house_door(True), "l9_door_bottom": house_door(False), "l9_porch_on": porch_light(True),
       "l9_porch_off": porch_light(False), "l9_lamp_on": lamp_head(True), "l9_lamp_off": lamp_head(False),
       "l9_pole": pole, "l9_picket": picket, "l9_manhole": manhole, "l9_mailbox": mailbox, "l9_trunk": trunk_side,
       "l9_trunk_end": trunk_end, "l9_leaves": leaves, "l9_floor": interior_floor, "l9_wallpaper": wallpaper_in,
       "l9_couch": couch, "l9_tv_case": tv_case, "l9_tv_on": tv_screen(True), "l9_tv_off": tv_screen(False),
       "l9_basement_door": basement_door}
for k, fn in TEX.items():
    tex(k, fn)

cube("suburb_grass", "l9_grass", display="Lawn", sound="grass")
cube("suburb_road", "l9_road", display="Asphalt")
cube("suburb_road_line_x", "l9_road_line_x", display="Asphalt With Center Line")
cube("suburb_road_line_z", "l9_road_line_z", display="Asphalt With Center Line")
cube("suburb_sidewalk", "l9_sidewalk", display="Sidewalk")
cube_faces("suburb_siding", top="l9_trim", bottom="l9_trim", side="l9_siding", display="Vinyl Siding", sound="wood")
cube("suburb_window", "l9_window", display="Dark Window", sound="glass")
cube("suburb_window_lit", "l9_window_lit", display="Lit Window", sound="glass", light=10, emissive=True)
cube("suburb_window_figure", "l9_window_figure", display="Lit Window", sound="glass", light=10, emissive=True)
cube("suburb_shingles", "l9_shingles", display="Roof Shingles")
stairs("suburb_roof_stairs", "l9_shingles", "suburb_shingles", display="Roof Shingles")
cube("suburb_trim", "l9_trim", display="White Trim")
door("suburb_front_door", "l9_door_top", "l9_door_bottom", display="Front Door", unbreakable=True)
cube("suburb_door_closed", "l9_door_bottom", display="Front Door", sound="wood")
cube("suburb_door_closed_top", "l9_door_top", display="Front Door", sound="wood")
shaped("suburb_porch_light", [(6, 4, 0, 10, 12, 1, "trim"), (6.5, 5, 1, 9.5, 11, 4, "glass")],
       {"trim": "l9_trim", "glass": "l9_porch_on"}, display="Porch Light", light=11, emissive=True, collides=False,
       sound="glass", unbreakable=True)
shaped("suburb_porch_light_off", [(6, 4, 0, 10, 12, 1, "trim"), (6.5, 5, 1, 9.5, 11, 4, "glass")],
       {"trim": "l9_trim", "glass": "l9_porch_off"}, display="Porch Light", collides=False, sound="glass",
       unbreakable=True)
flicker_light("suburb_streetlamp", "l9_lamp_on", "l9_lamp_off", "l9_pole", light=15, flicker=0.0,
              display="Streetlight")
shaped("suburb_lamp_pole", [(6.5, 0, 6.5, 9.5, 16, 9.5, "pole")], {"pole": "l9_pole"}, display="Streetlight Pole",
       sound="metal", unbreakable=True)
fence("suburb_picket_fence", "l9_picket", display="Picket Fence", sound="wood")
exit_block("suburb_manhole", "l9_manhole", "DESCEND", trigger="USE", solid=True, display="Manhole Cover", sound="metal")
shaped("suburb_mailbox", [(7, 0, 7, 9, 10, 9, "post"), (5, 10, 4, 11, 15, 13, "box"), (11, 12, 9, 12, 16, 10, "flag")],
       {"post": "l9_trunk", "box": "l9_mailbox", "flag": "l9_porch_off"}, display="Mailbox", sound="metal",
       unbreakable=True)
pillar("suburb_tree_trunk", "l9_trunk_end", "l9_trunk", display="Tree Trunk", sound="wood")
cube("suburb_leaves", "l9_leaves", display="Leaves", sound="grass", render="cutout")
cube("suburb_floorboards", "l9_floor", display="Floorboards", sound="wood")
cube("suburb_wallpaper", "l9_wallpaper", display="Living Room Wallpaper", sound="wool")
shaped("suburb_couch", [(0, 0, 4, 16, 7, 14, "f"), (0, 7, 12, 16, 15, 15, "f")], {"f": "l9_couch"},
       display="Couch", sound="wool", unbreakable=True)
shaped("suburb_tv", [(2, 0, 4, 14, 11, 13, {"all": "case", "south": "off"})], {"case": "l9_tv_case", "off": "l9_tv_off"},
       display="Television", sound="glass", unbreakable=True)
shaped("suburb_tv_on", [(2, 0, 4, 14, 11, 13, {"all": "case", "south": "on"})], {"case": "l9_tv_case", "on": "l9_tv_on"},
       display="Television", sound="glass", unbreakable=True, light=8, emissive=True)
exit_block("suburb_basement_door", "l9_basement_door", "DOOR", trigger="USE", solid=True, display="Basement Door",
           sound="wood", light=3)

# ------------------------------------------------------------------ sound


def night_loop():
    d = 26.0
    t = Y.t_axis(d)
    crickets = np.zeros_like(t)
    r = Y.rng("l9cr")
    for c in range(5):
        f = 4200 + r.random() * 900
        rate = 14 + r.random() * 6
        chirp = (np.sin(2 * np.pi * rate * t + r.random() * 6) > 0.6).astype(float)
        group = (np.sin(2 * np.pi * (0.3 + r.random() * 0.2) * t + r.random() * 6) > 0).astype(float)
        crickets += np.sin(2 * np.pi * f * t) * chirp * group * (0.3 + r.random() * 0.4)
    crickets = Y.lp(crickets, 7000)
    wind = Y.lp(Y.pink(d, "l9wind"), 500) * 0.35 * (0.6 + 0.4 * np.sin(2 * np.pi * t * 2 / d))
    highway = Y.bp(Y.brown(d, "l9hw"), 60, 300) * 0.4
    buzz = Y.hum(d, 60.0, "l9buzz", buzz=0.4, harmonics=(2, 4)) * 0.03
    x = crickets * 0.08 + wind + highway + buzz
    x = Y.loopable(Y.norm(x, 0.45), 1.0)
    return Y.stereoize(x, 0.002), "stereo"


def bark(i):
    def f():
        out = np.zeros(int(3.0 * Y.SR))
        for k in range(2 + i):
            n = int(0.22 * Y.SR)
            tt = np.arange(n) / Y.SR
            f0 = 520 - 300 * tt / 0.22
            v = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * np.exp(-tt * 12)
            v = Y.bp(v + Y.white(0.22, f"b{i}{k}")[:n] * 0.4 * np.exp(-tt * 20), 300, 2500)
            Y.place(out, v, 0.1 + k * 0.45, 0.8)
        return Y.norm(Y.reverb(Y.lp(out, 1600), 3.0, 0.7, f"l9b{i}"), 0.45)
    return f


def car_pass():
    d = 7.0
    t = Y.t_axis(d)
    env = np.exp(-((t - 3.5) / 1.4) ** 2)
    f0 = 90 * (1 + 0.08 * np.tanh((3.5 - t) * 2))
    eng = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) + 0.5 * np.sin(4 * np.pi * np.cumsum(f0) / Y.SR)
    tyres = Y.bp(Y.pink(d, "l9tyre"), 300, 1800)
    x = (eng * 0.4 + tyres) * env
    return Y.norm(Y.lp(x, 1500), 0.4)


def chimes():
    d = 4.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    r = Y.rng("l9chime")
    for k in range(5):
        f = (1046.5, 1174.7, 1396.9, 1568.0, 1760.0)[int(r.random() * 5)]
        n = int(2.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(x, np.sin(2 * np.pi * f * tt) * np.exp(-tt * 2.5), 0.2 + k * 0.5 + r.random() * 0.2, 0.4)
    return Y.norm(Y.reverb(x, 2.0, 0.4, "l9ch"), 0.35)


def screen_door():
    out = np.zeros(int(2.5 * Y.SR))
    Y.place(out, Y.impact("l9sd1", 0.4), 0.05, 1.0)
    Y.place(out, Y.impact("l9sd2", 0.3), 0.25, 0.4)
    return Y.norm(Y.reverb(Y.bp(out, 200, 3000), 2.5, 0.65, "l9sdv"), 0.5)


def lamp_off():
    d = 1.2
    t = Y.t_axis(d)
    clunk = np.zeros_like(t)
    Y.place(clunk, Y.impact("l9lo", 0.2), 0.0, 0.8)
    buzz = Y.hp(np.sign(np.sin(2 * np.pi * 120 * t)), 800) * np.exp(-t * 6) * 0.3
    return Y.norm(clunk + buzz, 0.7)


sound("ambient.level_9", [("ambient/level_9", night_loop)], stream=True)
sound("ambient.level_9.additions",
      [(f"ambient/level_9_bark{i}", bark(i)) for i in range(2)] +
      [("ambient/level_9_car", car_pass), ("ambient/level_9_chimes", chimes), ("ambient/level_9_screendoor", screen_door)],
      subtitle="Somewhere down the street")
sound("event.level_9_lamp_off", [("event/level_9_lamp_off", lamp_off)], subtitle="A streetlight goes out")

level("level_9", fog="#0a0e18", sky="#05070e", ambient="ambient.level_9", additions="ambient.level_9.additions",
      additions_chance=0.004, fixed_time=18000, skylight=True, ceiling=False, height=48)
