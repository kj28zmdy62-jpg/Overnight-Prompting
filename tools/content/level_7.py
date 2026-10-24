"""Level 7 - Thalassophobia. Small rooms on stilts above a black ocean that never ends.
The only way on is down. Something very large lives down there."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, ladder, level, sound, lang, ITEM_MODELS, \
    entity_model, entity_tex, part, flicker_light
import tex as T
import synth as Y
from content.classics_kit import centered, metal
from entity_paint import Canvas, skin

S = 32
yy, xx = np.mgrid[0:S, 0:S]


# ------------------------------------------------------------------ textures

def silt():
    img = T.ramp(T.fbm("l7s", octaves=4, base=4) * 0.7 + T.noise("l7sf", sigma=0.5) * 0.3, "#1a1c1c", "#2a2c2a")
    shells = (T.rng("l7sh").random((S, S)) > 0.985)
    return T.grain(T.blend(img, T.solid("#6a6a60"), shells * 0.6), "l7s", 0.04)


def rock():
    img = T.ramp(T.fbm("l7r", octaves=4, base=3), "#101214", "#22262a")
    img = T.cracks(img, "l7rc", "#050606", 3, 0.6)
    return T.grain(img, "l7r", 0.04)


def wallpaper():
    img = T.ramp(T.noise("l7wp", sigma=1.2), "#5a7470", "#667e78")
    stripe = ((xx % 8) < 3)
    img = T.shade(img, 1 - stripe * 0.08)
    img = T.blend(img, T.solid("#8a9a88"), ((xx % 8) == 1) * 0.3)
    # tide line: the water has been up here before
    tide = np.clip(1 - np.abs(yy - 24 - np.sin(xx / 4.0)) / 3.0, 0, 1)
    img = T.blend(img, T.solid("#3a4a40"), tide * 0.6)
    below = (yy > 24).astype(float)
    img = T.blend(img, T.solid("#3e504a"), below * 0.35)
    img = T.stains(img, "l7wps", "#2e3a30", 2, 0.4, 7)
    return T.grain(img, "l7wp", 0.03)


def boards():
    img = T.ramp(T.fbm("l7b", octaves=2, base=3) * 0.5 + T.noise("l7bg", sigma=0.4) * 0.3, "#2a1e16", "#3a2a1e")
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.45)
    for row in range(4):
        off = (row * 11) % 32
        img[row * 8:(row + 1) * 8, off] *= 0.6
    sheen = T.noise("l7bs", sigma=3)
    return T.grain(T.shade(img, 0.9 + sheen * 0.25), "l7b", 0.04)


def ceiling():
    img = T.ramp(T.fbm("l7c", octaves=3, base=5), "#8a8a80", "#9a9a8e")
    return T.grain(T.stains(img, "l7cs", "#4a4a30", 2, 0.4, 9), "l7c", 0.03)


def concrete():
    img = T.ramp(T.fbm("l7cc", octaves=4, base=4), "#3a3e3a", "#4a4e48")
    algae = np.clip(T.fbm("l7alg", octaves=3, base=4) * 1.6 - 0.6, 0, 1)
    img = T.blend(img, T.ramp(T.noise("l7alc", sigma=0.6), "#1a2a1a", "#2a3a22"), algae * 0.8)
    barn = (T.rng("l7barn").random((S, S)) > 0.95)
    return T.grain(T.blend(img, T.solid("#8a8a7a"), barn * 0.5), "l7cc", 0.04)


def roof():
    return T.grain(T.ramp(T.fbm("l7rf", octaves=3, base=3), "#141412", "#20201c"), "l7rf", 0.05)


def window():
    img = T.solid("#020406")
    refl = np.clip(1 - np.abs((xx - yy) + 6) / 3.0, 0, 1)
    img = T.blend(img, T.solid("#2a3438"), refl * 0.3)
    img = T.rect(img, 15, 0, 17, 32, "#e8e4d8")
    img = T.rect(img, 0, 15, 32, 17, "#e8e4d8")
    return T.border(img, "#d8d4c8", 2)


def lamp():
    img = T.solid("#fff0c8")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#ffd890"), np.clip(d / 18, 0, 1))
    return T.border(img, "#8a8a80", 3)


def lamp_off():
    return T.border(T.solid("#5a5a52"), "#8a8a80", 3)


def painting_sea():
    img = T.vgrad(S, S, "#1a2028", "#05070a")
    waves = np.sin(xx / 2.5 + yy * 1.1) * 0.5 + 0.5
    img = T.blend(img, T.ramp(waves * 0.5, "#0a1218", "#1a2a34"), (yy > 12) * 1.0)
    # a small lit window on the horizon, and something long beneath it
    img = T.rect(img, 15, 11, 17, 13, "#ffd890")
    eel = (np.abs(yy - (22 + 2 * np.sin(xx / 4.0))) < 1.2) & (xx > 4) & (xx < 28)
    img = T.blend(img, T.solid("#000000"), eel * 0.8)
    img = T.border(img, "#5a4a30", 2)
    return T.grain(img, "l7pt", 0.03)


def fabric():
    return T.grain(T.ramp(T.noise("l7fab", sigma=0.7), "#3a2a4a", "#4a3a5a"), "l7fab", 0.05)


def wood():
    return T.grain(T.ramp(T.fbm("l7wd", octaves=2, base=3), "#4a3020", "#5a3a26"), "l7wd", 0.03)


def radio():
    img = wood()
    img = T.rect(img, 3, 4, 20, 26, "#2a1e14")
    img[5:25:2, 4:19] = T.hexc("#4a3a28")
    img = T.rect(img, 22, 6, 29, 12, "#e8d8a8")
    img = T.rect(img, 25, 6, 26, 12, "#a01810")
    img = T.rect(img, 23, 16, 28, 21, "#1a1a1a")
    return img


def buoy():
    img = T.solid("#c8c4b8")
    img = T.blend(img, T.solid("#b01812"), ((yy // 8) % 2 == 0) * 1.0)
    return T.grain(T.stains(img, "l7buoy", "#3a3a30", 2, 0.4, 6), "l7buoy", 0.04)


def buoy_light():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.ramp(np.clip(1 - d / 22, 0, 1), "#a01008", "#ff3a20", "#ffb0a0")


def sunken_frame():
    img = concrete()
    return T.blend(img, T.solid("#0e1a14"), np.clip(T.fbm("l7sfm", octaves=3, base=3) * 1.4 - 0.5, 0, 0.8))


def sunken_door():
    img = T.solid("#000000")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 28) ** 2)
    img = T.blend(img, T.solid("#0a2a1a"), np.clip(1 - d / 26, 0, 1) * 0.8)
    return T.grain(img, "l7sd", 0.02)


def deep_lamp():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(np.clip(1 - d / 22, 0, 1), "#2a5a20", "#8ac850", "#e8ffc0")
    return T.border(img, "#1a2a1a", 3)


def kelp():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("l7kelp")
    for k in range(3):
        x = 8 + k * 8 + r.random() * 2
        for y in range(S):
            x += np.sin(y / 5.0 + k) * 0.35
            xi = int(x) % S
            rgb[y, xi:xi + 2] = T.hexc("#1a2a12") if k % 2 else T.hexc("#24321a")
            a[y, xi:xi + 2] = 1
    return rgb, a


def l7_ladder():
    rgb = metal("l7lad", "#3a3a34", "#4a4a40", rust=("#4a2a16", "#6a3a1c"), rust_amt=0.2)
    a = (((xx >= 3) & (xx <= 5)) | ((xx >= 26) & (xx <= 28)) | ((yy % 8 < 2) & (xx > 3) & (xx < 28))).astype(float)
    return rgb, a


TEX = {"l7_silt": silt, "l7_rock": rock, "l7_wallpaper": wallpaper, "l7_boards": boards, "l7_ceiling": ceiling,
       "l7_concrete": concrete, "l7_roof": roof, "l7_window": window, "l7_lamp": lamp, "l7_lamp_off": lamp_off,
       "l7_painting": painting_sea, "l7_fabric": fabric, "l7_wood": wood, "l7_radio": radio, "l7_buoy": buoy,
       "l7_buoy_light": buoy_light, "l7_sunken_frame": sunken_frame, "l7_sunken_door": sunken_door,
       "l7_deep_lamp": deep_lamp, "l7_kelp": kelp, "l7_ladder": l7_ladder}
for k, fn in TEX.items():
    tex(k, fn)

cube("abyssal_silt", "l7_silt", display="Abyssal Silt", sound="sand")
cube("abyssal_rock", "l7_rock", display="Abyssal Rock")
cube("stilt_room_wallpaper", "l7_wallpaper", display="Water-Stained Wallpaper", sound="wool")
cube("stilt_room_boards", "l7_boards", display="Wet Floorboards", sound="wood")
cube("stilt_room_ceiling", "l7_ceiling", display="Stilt Room Ceiling", flat=True)
cube("stilt_concrete", "l7_concrete", display="Algae-Covered Concrete")
cube_faces("stilt_room_roof", top="l7_roof", bottom="l7_ceiling", side="l7_roof", display="Tar Roof", flat=True)
cube("stilt_room_window", "l7_window", display="Window Onto Nothing", sound="glass")
flicker_light("stilt_room_lamp", "l7_lamp", "l7_lamp_off", "l7_ceiling", light=14, flicker=0.08,
              display="Stilt Room Lamp")
shaped("stilt_room_painting", [(1, 2, 0, 15, 14, 1, {"all": "frame", "south": "art"})],
       {"frame": "l7_wood", "art": "l7_painting"}, display="Painting Of The Sea", collides=False, sound="wood",
       unbreakable=True)
shaped("stilt_room_armchair", [
    (2, 0, 2, 14, 7, 14, "fab"), (2, 7, 12, 14, 16, 14, "fab"),
    (1, 0, 3, 3, 10, 14, "fab"), (13, 0, 3, 15, 10, 14, "fab"),
], {"fab": "l7_fabric"}, display="Armchair", sound="wool", unbreakable=True)
shaped("stilt_room_table", [(1, 12, 1, 15, 14, 15, "wood"), (2, 0, 2, 4, 12, 4, "wood"), (12, 0, 2, 14, 12, 4, "wood"),
                            (2, 0, 12, 4, 12, 14, "wood"), (12, 0, 12, 14, 12, 14, "wood")],
       {"wood": "l7_wood"}, display="Table", sound="wood", unbreakable=True)
shaped("stilt_room_radio", [(3, 0, 5, 13, 8, 11, {"all": "wood", "south": "front"})],
       {"wood": "l7_wood", "front": "l7_radio"}, display="Radio", sound="wood", unbreakable=True, collides=False)
shaped("stilt_door_lamp", [(5, 4, 0, 11, 12, 1, "frame"), (6, 5, 1, 10, 11, 4, "lamp")],
       {"frame": "l7_concrete", "lamp": "l7_lamp"}, display="Door Lamp", light=13, emissive=True, collides=False,
       sound="glass", unbreakable=True)
shaped("ocean_buoy", [(4, 0, 4, 12, 10, 12, "buoy"), (7, 10, 7, 9, 14, 9, "buoy"), (6.5, 14, 6.5, 9.5, 17, 9.5, "light")],
       {"buoy": "l7_buoy", "light": "l7_buoy_light"}, display="Buoy", light=9, emissive=True, sound="metal",
       unbreakable=True)
cube("sunken_door_frame", "l7_sunken_frame", display="Barnacled Frame")
exit_block("sunken_doorway", "l7_sunken_door", "WATER", display="Sunken Doorway")
cube("deep_lamp", "l7_deep_lamp", display="Deep Lamp", light=12, emissive=True, sound="glass")
ladder("stilt_ladder", "l7_ladder", display="Rusted Ladder", unbreakable=True)

from assets import cross
cross("abyssal_kelp", "l7_kelp", display="Black Kelp")

# ------------------------------------------------------------------ the Leviathan


def leviathan_parts():
    return [
        part("body", (0, 8, 0), cubes=[(0, 0, -8, -7, -16, 16, 14, 32)]),
        part("head", (0, 8, -16), cubes=[(0, 46, -7, -6, -16, 14, 12, 16)]),
        part("jaw", (0, 5, -2), parent="head", cubes=[(60, 46, -6, 0, -14, 12, 3, 14)]),
        part("tail", (0, 0, 16), parent="body", cubes=[(0, 74, -6, -5, 0, 12, 10, 28)]),
        part("tail2", (0, 0, 28), parent="tail", cubes=[(0, 112, -3, -3, 0, 6, 6, 8), (100, 90, -1, -8, 0, 2, 16, 10)]),
        part("wing_l", (8, 4, -6), cubes=[(80, 64, 0, 0, -4, 14, 1, 10)]),
        part("wing_r", (-8, 4, -6), cubes=[(80, 76, -14, 0, -4, 14, 1, 10)]),
    ]


def leviathan_tex():
    c = Canvas(128, 128)
    hide = skin("lev", "#06080a", "#121a1e", sigma=0.8, spots="#1e2a2e")
    belly = skin("levb", "#3a3e3a", "#4e524a", sigma=1.0)

    def eyes(w, h):
        img = hide(w, h)
        r = T.rng("leveyes")
        for _ in range(9):
            x, y = int(r.random() * (w - 2)), int(r.random() * (h - 2))
            img[y:y + 2, x:x + 2] = T.hexc("#d8e0c8")
        return img

    c.box(0, 0, 16, 14, 32, all=hide, down=belly)
    c.box(0, 46, 14, 12, 16, all=hide, front=eyes, down=belly)
    c.box(60, 46, 12, 3, 14, all=lambda w, h: T.solid("#2a0e0e", shape=(h, w)))
    c.box(0, 74, 12, 10, 28, all=hide, down=belly)
    c.box(0, 112, 6, 6, 8, all=hide)
    c.box(100, 90, 2, 16, 10, all=hide)
    c.box(80, 64, 14, 1, 10, all=hide)
    c.box(80, 76, 14, 1, 10, all=hide)
    return c.out()


def leviathan_glow():
    c = Canvas(128, 128)
    from assets import box_uv
    x, y, w, h = box_uv(0, 46, 14, 12, 16)["front"]
    r = T.rng("leveyes")
    for _ in range(9):
        ex, ey = int(r.random() * (w - 2)), int(r.random() * (h - 2))
        c.fill_rect(x + ex, y + ey, 2, 2, "#e8f0d0")
    rgb, a = c.out()
    return rgb, (rgb.sum(axis=2) > 0.1).astype(float)


entity_model("leviathan", leviathan_parts(), tex_size=(128, 128))
entity_tex("leviathan", leviathan_tex)
entity_tex("leviathan_glow", leviathan_glow)
lang("entity.noclip.leviathan", "Something Below")
lang("item.noclip.leviathan_spawn_egg", "Something Below Spawn Egg")
ITEM_MODELS["leviathan_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def sea_loop():
    d = 28.0
    t = Y.t_axis(d)
    swell = 0.6 + 0.4 * np.sin(2 * np.pi * t * 4 / d)
    waves = Y.lp(Y.pink(d, "l7w"), 900) * swell
    lap = np.zeros_like(t)
    r = Y.rng("l7lap")
    for k in range(18):
        n = int(0.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        slap = Y.bp(Y.white(0.5, f"l7l{k}"), 300, 2500)[:n] * np.exp(-tt * 9)
        Y.place(lap, slap, r.random() * (d - 1), 0.3 + r.random() * 0.4)
    deep = Y.lp(Y.brown(d, "l7deep"), 60) * 1.2
    creak = np.zeros_like(t)
    n = int(1.5 * Y.SR)
    tt = np.arange(n) / Y.SR
    cr = np.sin(2 * np.pi * np.cumsum(90 + 30 * np.sin(2 * np.pi * 0.8 * tt)) / Y.SR) * np.sin(np.pi * tt / 1.5)
    Y.place(creak, Y.bp(cr * (1 + np.sign(np.sin(2 * np.pi * 31 * tt))), 200, 1500), 13.0, 0.15)
    x = waves * 0.6 + lap * 0.5 + deep + creak
    x = Y.loopable(Y.norm(x, 0.5), 1.5)
    return Y.stereoize(x, 0.002), "stereo"


def moan(seed, base=38.0, dur=7.0):
    t = Y.t_axis(dur)
    f0 = base * (1 + 0.25 * np.sin(np.pi * t / dur) + 0.05 * np.sin(2 * np.pi * 0.4 * t))
    ph = 2 * np.pi * np.cumsum(f0) / Y.SR
    x = np.sin(ph) + 0.6 * np.sin(2 * ph + 0.4) + 0.35 * np.sin(3.02 * ph) + 0.2 * np.sin(5.1 * ph)
    x *= np.sin(np.pi * t / dur) ** 1.5
    x += Y.lp(Y.white(dur, seed), 200) * 0.3 * np.sin(np.pi * t / dur)
    return Y.norm(Y.reverb(Y.lp(x, 1200), 4.0, 0.7, seed), 0.9)


def add_moan(i):
    def f():
        return moan(f"l7m{i}", 34 + i * 9, 6.5 + i)
    return f


def add_slap():
    out = np.zeros(int(2.5 * Y.SR))
    for k in range(3):
        n = int(0.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.4, f"l7sl{k}"), 200, 3000)[:n] * np.exp(-tt * 12), 0.1 + k * 0.6, 0.7)
    return Y.norm(Y.reverb(out, 1.5, 0.5, "l7sl"), 0.6)


def add_bubbles():
    out = np.zeros(int(3 * Y.SR))
    r = Y.rng("l7bub")
    for k in range(14):
        n = int(0.06 * Y.SR)
        tt = np.arange(n) / Y.SR
        f = 400 + r.random() * 900
        Y.place(out, np.sin(2 * np.pi * (f + 3000 * tt) * tt) * np.exp(-tt * 50), 0.2 + k * 0.17 + r.random() * 0.05, 0.5)
    return Y.norm(Y.lp(out, 2500), 0.5)


def radio_static():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "l7rad"), 800, 5000) * 0.5
    voice = Y.whisper("l7radv", 2.0)
    x[int(1.0 * Y.SR):int(1.0 * Y.SR) + len(voice)] += voice * 0.8
    x *= (0.7 + 0.3 * np.sign(np.sin(2 * np.pi * 7 * t)))
    return Y.norm(x * np.sin(np.pi * t / d) ** 0.3, 0.45)


def close_moan():
    return moan("l7close", 30.0, 8.0)


def surge():
    d = 3.0
    t = Y.t_axis(d)
    rush = Y.lp(Y.brown(d, "l7rush"), 400) * np.clip(t / 0.4, 0, 1) * np.exp(-t * 0.8)
    roar = np.sin(2 * np.pi * np.cumsum(70 - 20 * t) / Y.SR) * np.exp(-t * 0.9)
    return Y.norm(rush + roar * 0.8, 0.95)


sound("ambient.level_7", [("ambient/level_7", sea_loop)], stream=True)
sound("ambient.level_7.additions",
      [(f"ambient/level_7_moan{i}", add_moan(i)) for i in range(2)] +
      [("ambient/level_7_slap", add_slap), ("ambient/level_7_bubbles", add_bubbles),
       ("ambient/level_7_radio", radio_static)],
      subtitle="The sea moves")
sound("entity.leviathan.moan", [("entity/leviathan_moan", close_moan)], subtitle="Something huge calls, below")
sound("entity.leviathan.surge", [("entity/leviathan_surge", surge)], subtitle="Something rushes up from below")

level("level_7", fog="#08131c", sky="#020306", water="#0a1a24", water_fog="#02070a", ambient_light=0.05,
      ambient="ambient.level_7", additions="ambient.level_7.additions", additions_chance=0.004, fixed_time=18000,
      skylight=False, ceiling=False, height=64)
