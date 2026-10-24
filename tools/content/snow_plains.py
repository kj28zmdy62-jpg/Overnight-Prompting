"""Snow Plains. Streetlights stand in rows across an empty snowfield in a blizzard, as if
there were a road. Away from their light you get colder, and colder. There are tracks in
the snow. They are not yours."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, exit_block, flicker_light, level, sound
import assets as A
import tex as T
import synth as Y
from content.outside_kit import draw_text, centered, paint_metal

S = 32
tx = A.tex


def snow(seed, lo="#c8d0d8", hi="#eef2f6"):
    img = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.6 + T.noise(seed + "s", sigma=0.5) * 0.4, lo, hi)
    sparkle = (T.rng(seed + "sp").random((S, S)) > 0.985).astype(float)
    img = T.blend(img, T.solid("#ffffff"), sparkle)
    yy, xx = np.mgrid[0:S, 0:S]
    wind = (np.sin((xx * 0.4 + yy) * 0.6 + T.noise(seed + "w", sigma=3) * 6) > 0.8).astype(float) * 0.05
    return T.grain(T.shade(img, 1 - wind), seed, 0.025)


def snow_side():
    img = snow("spside", "#b8c2cc", "#dce2e8")
    yy = np.mgrid[0:S, 0:S][0]
    return T.shade(img, 1 - ((yy % 9) == 0) * 0.06)


def frozen_ground():
    img = T.ramp(T.fbm("spfg", octaves=3, base=3), "#5a5e64", "#6a6e74")
    return T.grain(img, "spfg", 0.05)


tx("sp_snow", lambda: snow("spsnow"))
tx("sp_snow_b", lambda: snow("spsnowb", "#c4ccd6", "#eaeef4"))
tx("sp_snow_side", snow_side)
tx("sp_frozen", frozen_ground)
cube_faces("sp_snow", top="sp_snow", bottom="sp_snow_side", side="sp_snow_side", sound="snow", display="Deep Snow")
cube_faces("sp_snow_b", top="sp_snow_b", bottom="sp_snow_side", side="sp_snow_side", sound="snow", display="Deep Snow")
cube("sp_frozen", "sp_frozen", sound="gravel", display="Frozen Ground")


def prints(fresh):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        yy, xx = np.mgrid[0:S, 0:S]
        for cx, cy in ((11, 8), (21, 24)):
            m = ((xx - cx) ** 2 / 7 + (yy - cy) ** 2 / 26) < 1
            heel = ((xx - cx) ** 2 / 6 + (yy - cy - 6) ** 2 / 6) < 1
            mm = m | heel
            rgb[mm] = T.hexc("#7a8a9c" if fresh else "#a8b4c2")
            a[mm] = 0.85 if fresh else 0.55
            rim = (((xx - cx) ** 2 / 10 + (yy - cy) ** 2 / 34) < 1) & ~mm
            rgb[rim] = T.hexc("#f4f8fc")
            a[rim] = 0.6 if fresh else 0.3
        return rgb, a
    return f


tx("sp_tracks_old", prints(False))
tx("sp_tracks_fresh", prints(True))
shaped("sp_tracks", [(0, 0, 0, 16, 0.15, 16, {"all": None, "up": "t"})], {"t": "sp_tracks_old"},
       display="Old Tracks", collides=False, unbreakable=True, sound="snow", render="translucent")
shaped("sp_tracks_fresh", [(0, 0, 0, 16, 0.15, 16, {"all": None, "up": "t"})], {"t": "sp_tracks_fresh"},
       display="Fresh Tracks", collides=False, unbreakable=True, sound="snow", render="translucent")

# ------------------------------------------------------------------ streetlights


def lamp_lens(on):
    def f():
        img = T.solid("#e8f4ff" if on else "#5a6068")
        if on:
            yy, xx = np.mgrid[0:S, 0:S]
            img = T.shade(img, 1.05 - np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2) / 70)
        img = T.border(img, "#3a3e44", 3)
        return img
    return f


def frosted_pole():
    img = paint_metal("sppole", "#3a4048", "#4a5058", rust=0.1)
    yy, xx = np.mgrid[0:S, 0:S]
    frost = (T.noise("spfrost", sigma=1) > 0.6) & (xx < 12)
    img[frost] = T.hexc("#dce4ec")
    return img


tx("sp_lamp_on", lamp_lens(True))
tx("sp_lamp_off", lamp_lens(False))
tx("sp_pole", frosted_pole)
shaped("sp_pole", [(6.5, 0, 6.5, 9.5, 16, 9.5, "p")], {"p": "sp_pole"}, display="Streetlight", unbreakable=True)
shaped("sp_pole_top", [(6.5, 0, 6.5, 9.5, 12, 9.5, "p"), (6.5, 10, 0, 9.5, 12, 9, "p")], {"p": "sp_pole"},
       display="Streetlight", unbreakable=True)
flicker_light("sp_lamp", "sp_lamp_on", "sp_lamp_off", "sp_pole", light=15, flicker=0.0, display="Mercury Lamp")
flicker_light("sp_lamp_failing", "sp_lamp_on", "sp_lamp_off", "sp_pole", light=15, flicker=0.5,
              display="Failing Mercury Lamp")
cube_faces("sp_lamp_dead", bottom="sp_lamp_off", top="sp_pole", side="sp_pole", flat=True, display="Dead Lamp")

# ------------------------------------------------------------------ things in the snow


def hatch():
    img = paint_metal("sphatch", "#5a4a3a", "#6a5a46", rust=0.4)
    img = T.border(img, "#2a2a28", 2)
    img = T.rect(img, 14, 4, 18, 28, "#3a3a38")
    img = T.rect(img, 22, 14, 27, 18, "#8a8a80")
    frost = T.noise("sphf", sigma=1.5) > 0.7
    img[frost] = T.hexc("#dce4ec")
    return img


tx("sp_hatch", hatch)
exit_block("sp_cellar", "sp_hatch", "DESCEND", trigger="USE", solid=True, display="Storm Cellar Hatch", sound="wood")


def fence_post():
    img = T.ramp(T.fbm("spfp", octaves=3, base=2), "#3a3028", "#4a3e34")
    img[:6] = T.hexc("#e8eef4")
    return T.grain(img, "spfp", 0.05)


tx("sp_post", fence_post)
shaped("sp_post", [(6, 0, 6, 10, 12, 10, "p")], {"p": "sp_post"}, display="Buried Fence Post", unbreakable=True)


def sign_plate():
    img = T.solid("#e8e4dc")
    img = T.border(img, "#1a1a1a", 1)
    img = centered(img, "STOP", 12, "#b02a22", scale=1)
    frost = T.noise("spsign", sigma=1.2) > 0.62
    img[frost] = T.hexc("#f4f8fc")
    return img


tx("sp_sign", sign_plate)
shaped("sp_road_sign", [(7, 0, 7, 9, 10, 9, "p"), (3, 10, 7.5, 13, 16, 8.5, {"all": "p", "north": "s!", "south": "p"})],
       {"p": "sp_pole", "s": "sp_sign"}, display="Stop Sign", unbreakable=True, sound="metal")


def mitten():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    m = (((xx - 16) ** 2 / 30 + (yy - 16) ** 2 / 60) < 1) | (((xx - 9) ** 2 / 6 + (yy - 13) ** 2 / 16) < 1)
    rgb[m] = T.hexc("#b02a22")
    rgb[m & (yy > 22)] = T.hexc("#e8e4dc")
    a[m] = 1
    return rgb, a


tx("sp_mitten", mitten)
shaped("sp_mitten", [(0, 0, 0, 16, 0.3, 16, {"all": None, "up": "m"})], {"m": "sp_mitten"}, display="Lost Mitten",
       collides=False, unbreakable=True, sound="wool")

# ------------------------------------------------------------------ sound


def blizzard():
    d = 26.0
    t = Y.t_axis(d)
    gust = 0.55 + 0.3 * np.sin(2 * np.pi * t / 13) + 0.15 * np.sin(2 * np.pi * t / 6.5 + 1)
    wind = Y.bp(Y.pink(d, "spwind"), 250, 3500) * gust
    whistle = np.zeros_like(t)
    for f in (720.0, 1080.0):
        ff = round(f * d) / d
        whistle += np.sin(2 * np.pi * ff * t + 4 * np.sin(2 * np.pi * t * round(0.3 * d) / d)) * 0.02 * gust ** 3
    hiss = Y.hp(Y.white(d, "spsnowh"), 5000) * 0.03 * gust
    x = Y.loopable(Y.norm(wind + whistle + hiss, 0.55), 1.5)
    return Y.stereoize(x, 0.003), "stereo"


def crunch_steps():
    out = np.zeros(int(4.0 * Y.SR))
    for k in range(6):
        n = int(0.3 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = Y.bp(Y.white(0.3, f"spc{k}"), 600, 5000) * np.exp(-tt * 14) * (1 + 0.5 * (Y.rng(f"spcr{k}").random(n) > 0.95))
        Y.place(out, c, 0.2 + k * 0.6, 0.5 + 0.1 * (k % 2))
    return Y.reverb(Y.lp(Y.norm(out, 0.4), 4000), 1.5, 0.35, "spsteps")


def lamp_creak():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(90 + 40 * np.sin(np.pi * t / d)) / Y.SR)), 300, 2000) * np.sin(np.pi * t / d)
    return Y.reverb(Y.norm(x, 0.3), 2.5, 0.6, "splamp")


def howl():
    d = 4.0
    t = Y.t_axis(d)
    f = 380 + 160 * np.sin(np.pi * t / d) ** 2 - 60 * (t / d)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * np.sin(np.pi * t / d) ** 2
    x += np.sin(2 * np.pi * np.cumsum(f * 1.5) / Y.SR) * 0.2 * np.sin(np.pi * t / d) ** 2
    return Y.reverb(Y.lp(Y.norm(x, 0.3), 1500), 4.0, 0.85, "sphowl")


def slide():
    out = np.zeros(int(3.0 * Y.SR))
    n = int(1.2 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, Y.lp(Y.white(1.2, "spslide"), 800) * np.sin(np.pi * tt / 1.2), 0.0, 0.6)
    Y.place(out, Y.impact("spthump", 0.8), 1.1, 0.8)
    return Y.reverb(Y.norm(out, 0.4), 2.5, 0.6, "spslide")


sound("ambient.snow_plains", [("ambient/snow_plains", blizzard)], stream=True)
sound("ambient.snow_plains.additions",
      [("ambient/snow_plains_creak", lamp_creak), ("ambient/snow_plains_howl", howl),
       ("ambient/snow_plains_slide", slide)],
      subtitle="Something in the blizzard")
sound("event.snow_plains_steps", [("event/snow_plains_steps", crunch_steps)], subtitle="Footsteps in the snow nearby")

level("snow_plains", fog="#8a96a4", sky="#8a96a4", ambient="ambient.snow_plains",
      additions="ambient.snow_plains.additions", additions_chance=0.003, fixed_time=13200, skylight=True,
      ceiling=False, height=80, particle="minecraft:snowflake", particle_chance=0.06)
