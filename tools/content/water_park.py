"""Closed Water Park - an indoor dome at night. The pools are drained, the slides go up into the dark,
the lifeguard chairs are empty, and the air smells of chlorine. No running on the deck. Every few minutes
the wave pool opens again."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, stairs, ladder, level, sound
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32


def mosaic(seed, lo, hi, wet=0.0, line=None):
    yy, xx = np.mgrid[0:S, 0:S]
    cell = T.rng(seed).random((S // 4, S // 4))
    img = T.ramp(np.kron(cell, np.ones((4, 4))) * 0.5 + 0.25, lo, hi)
    img = T.shade(img, 1 - (((xx % 4) == 0) | ((yy % 4) == 0)) * 0.22)
    if line:
        img = T.blend(img, T.solid(line), ((xx >= 12) & (xx < 20)).astype(float) * 0.9)
    if wet:
        img = T.shade(img, 1 + np.clip(T.noise(seed + "wet", sigma=3) - 0.5, 0, 1) * wet)
    img = T.stains(img, seed + "s", "#7a8a70", count=1, strength=0.18, size=6)
    return T.grain(img, seed, 0.02)


def deck():
    img = mosaic("wpd", "#d8e4e4", "#e8f0f0", wet=0.8)
    yy, xx = np.mgrid[0:S, 0:S]
    nonslip = ((xx + yy) % 6 == 0)
    return T.shade(img, 1 - nonslip * 0.05)


def pool_wall():
    return mosaic("wpw", "#3a8ac8", "#5aa8e0")


def pool_wall_depth(text):
    def f():
        img = pool_wall()
        img = T.blend(img, T.solid("#0a1a3a"), K.centered(text, S, S, 12, font=K.F57))
        return img
    return f


def coping():
    img = T.ramp(T.noise("wpcp", sigma=1.5), "#e8e8e0", "#f4f4ec")
    img[26:] = T.hexc("#1a5a9a")
    return T.grain(img, "wpcp", 0.02)


def drain():
    yy, xx = np.mgrid[0:S, 0:S]
    img = pool_wall()
    d = np.maximum(np.abs(xx - 15.5), np.abs(yy - 15.5))
    grate = (d < 12) & ((xx % 3) == 0)
    img = T.blend(img, T.solid("#05080a"), (d < 12).astype(float))
    img = T.blend(img, T.solid("#9aa0a4"), grate.astype(float))
    return T.blend(img, T.solid("#7a8084"), ((d >= 12) & (d < 13)).astype(float))


def glass_roof():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("wpgr", octaves=3, base=5), "#06100c", "#0c1a14")
    streaks = (T.noise("wprn", sigma=0, shape=(S, S)) > 0.985).astype(float)
    from scipy.ndimage import uniform_filter1d
    streaks = uniform_filter1d(streaks, 6, axis=0, mode="wrap") > 0.1
    img = T.blend(img, T.solid("#4a6a60"), streaks * 0.5)  # condensation running down the glass
    truss = ((xx % 16) < 2) | ((yy % 16) < 2)
    img = T.blend(img, T.ramp(T.noise("wptr", sigma=1), "#c8ccc8", "#e0e4e0"), truss.astype(float))
    return img


def truss():
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((np.abs(xx - yy) < 2) | (np.abs(xx + yy - 31) < 2) | (yy < 3) | (yy > 28)).astype(float)
    return T.ramp(T.noise("wptru", sigma=1), "#c8ccc8", "#e0e4e0"), a


def slide(col):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        lo, hi = col
        img = T.ramp(np.sin(yy / 32 * np.pi * 2) * 0.3 + 0.5 + T.noise("wps" + lo, sigma=2) * 0.2, lo, hi)
        img = T.shade(img, 1 + np.clip(np.sin(xx / 32 * np.pi) - 0.75, 0, 1) * 1.2)
        seam = (xx % 16 == 0)
        return T.shade(img, 1 - seam * 0.25)
    return f


def slide_mouth():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 17) ** 2)
    img = slide(("#e8a020", "#f8c040"))()
    img = T.blend(img, T.ramp(np.clip(d / 13, 0, 1), "#000000", "#0a0a0a"), (d < 13).astype(float))
    return img


def scaffold():
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((xx < 3) | (xx > 28) | (yy < 3) | (yy > 28) | (np.abs(xx - yy) < 2)).astype(float)
    return T.ramp(T.noise("wpsc", sigma=1), "#3a7aa8", "#4a8ab8"), a


def grating():
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx % 4) < 1) | ((yy % 4) < 1)).astype(float)
    return T.ramp(T.noise("wpgt", sigma=1), "#9aa0a4", "#b8bec2"), np.maximum(a, 0.0)


def white_plastic():
    img = T.ramp(T.noise("wpwp", sigma=2), "#e8ecec", "#f8fcfc")
    return T.grain(T.stains(img, "wpwps", "#a8b0a0", count=1, strength=0.15, size=7), "wpwp", 0.015)


def lounger_strap():
    yy, xx = np.mgrid[0:S, 0:S]
    img = np.where(((yy // 4) % 2 == 0)[..., None], T.hexc("#e86a1a"), T.hexc("#f0f0e8"))
    return T.grain(img, "wpls", 0.03)


def ring():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    img = deck()
    torus = (d < 13) & (d > 7)
    col = np.where(((ang * 4 / np.pi) % 2 < 1)[..., None], T.hexc("#e82020"), T.hexc("#f0f0f0"))
    return T.blend(img, col, torus.astype(float))


def sign(lines, bg, fg, seed):
    def f():
        img = T.solid(bg)
        y = 16 - len(lines) * 4
        for ln in lines:
            img = T.blend(img, T.solid(fg), K.centered(ln, S, S, y, font=K.F35))
            y += 8
        return T.border(img, "#f0f0f0", 1)
    return f


def wall():
    img = T.ramp(T.fbm("wpwl", octaves=3, base=4), "#b8d0c8", "#c8e0d8")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.08)
    img = T.stains(img, "wpwls", "#6a8a7a", count=2, strength=0.3, size=8)  # mildew
    return T.grain(img, "wpwl", 0.03)


def lamp():
    img = T.solid("#e8fff4")
    return T.border(img, "#8a9a94", 3)


def palm_leaves():
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.zeros((S, S))
    for k in range(7):
        ang = np.pi * (0.1 + 0.8 * k / 6)
        ex, ey = 15.5 + np.cos(ang) * 15, 18 - np.sin(ang) * 12 + abs(np.cos(ang)) * 8
        a = np.maximum(a, K.line_mask(S, S, 15.5, 18, ex, ey, 3.0))
    a = np.maximum(a, ((np.abs(xx - 15.5) < 1.5) & (yy > 16)).astype(float))
    rgb = T.ramp(T.noise("wppl", sigma=0.8), "#1e5a2a", "#2e7a36")
    return rgb, a


tex("wp_deck", deck)
tex("wp_pool", pool_wall)
tex("wp_pool_lane", lambda: mosaic("wpln", "#3a8ac8", "#5aa8e0", line="#0a2a5a"))
tex("wp_pool_puddle", lambda: mosaic("wppd", "#2a6aa0", "#3a7ab0", wet=1.6))
for txt in ("3FT", "5FT", "9FT"):
    tex(f"wp_pool_{txt.lower()}", pool_wall_depth(txt))
tex("wp_coping", coping)
tex("wp_drain", drain)
tex("wp_glass_roof", glass_roof)
tex("wp_truss", truss)
for name, col in (("orange", ("#e8a020", "#f8c040")), ("teal", ("#1aa8a0", "#30c8c0")), ("purple", ("#7a3ab8", "#9a5ad0"))):
    tex(f"wp_slide_{name}", slide(col))
tex("wp_slide_mouth", slide_mouth)
tex("wp_scaffold", scaffold)
tex("wp_grating", grating)
tex("wp_white_plastic", white_plastic)
tex("wp_strap", lounger_strap)
tex("wp_ring", ring)
tex("wp_sign_closed", sign(["POOL", "CLOSED"], "#c82020", "#f8f8f0", "wpsc1"))
tex("wp_sign_norun", sign(["NO", "RUNNING"], "#f0f0e8", "#c82020", "wpsc2"))
tex("wp_sign_waves", sign(["WAVES", "EVERY", "HOUR"], "#1a5aa8", "#f8f8f0", "wpsc3"))
tex("wp_wall", wall)
tex("wp_lamp", lamp)
tex("wp_palm_leaves", palm_leaves)

cube("wp_deck", "wp_deck", display="Pool Deck", sound="stone")
cube("wp_pool", "wp_pool", display="Pool Tile", sound="stone")
cube("wp_pool_lane", "wp_pool_lane", display="Lane Line", sound="stone")
cube("wp_pool_puddle", "wp_pool_puddle", display="Wet Pool Tile", sound="stone")
for txt in ("3ft", "5ft", "9ft"):
    cube(f"wp_pool_{txt}", f"wp_pool_{txt}", display="Depth Marker", sound="stone")
cube("wp_coping", "wp_coping", display="Pool Coping", sound="stone")
exit_block("wp_drain", "wp_drain", "WATER", display="Main Drain")
cube("wp_glass_roof", "wp_glass_roof", display="Dome Glass", sound="glass", flat=True)
cube("wp_wall", "wp_wall", display="Natatorium Wall", sound="stone")
for name in ("orange", "teal", "purple"):
    cube(f"wp_slide_{name}", f"wp_slide_{name}", display="Slide Tube", sound="wool")
exit_block("wp_slide_mouth", "wp_slide_mouth", "FALL", display="Slide Entrance")
cube("wp_scaffold", "wp_scaffold", display="Slide Tower", sound="metal", render="cutout")
cube("wp_grating", "wp_grating", display="Grating", sound="metal", render="cutout")
stairs("wp_steps", "wp_deck", "wp_deck", display="Tower Steps", sound="stone")
cube_faces("wp_pool_light", top="wp_pool", bottom="wp_pool", side="wp_lamp", display="Underwater Light", light=10, emissive=True, sound="glass")
ladder("wp_ladder", "wp_truss", display="Pool Ladder")
cube("wp_sign_closed", "wp_sign_closed", display="Sign", sound="wood")
cube("wp_sign_norun", "wp_sign_norun", display="Sign", sound="wood")
cube("wp_sign_waves", "wp_sign_waves", display="Sign", sound="wood")
cube("wp_ring", "wp_ring", display="Life Ring", sound="wool")
flicker_light("wp_lamp", "wp_lamp", "wp_wall", "wp_wall", light=13, flicker=0.05, display="Pool Light")
from assets import cross
cross("wp_palm_leaves", "wp_palm_leaves", display="Plastic Palm")
shaped("wp_lounger", [(1, 4, 0, 15, 6, 16, {"all": "frame", "up": "strap"}), (1, 6, 12, 15, 12, 16, {"all": "frame", "north": "strap"}),
                      (1, 0, 1, 2, 4, 2, "frame"), (14, 0, 1, 15, 4, 2, "frame"), (1, 0, 14, 2, 4, 15, "frame"), (14, 0, 14, 15, 4, 15, "frame")],
       {"frame": "wp_white_plastic", "strap": "wp_strap"}, display="Pool Lounger", unbreakable=True)
shaped("wp_lifeguard_base", [(1, 0, 1, 3, 16, 3, "w"), (13, 0, 1, 15, 16, 3, "w"), (1, 0, 13, 3, 16, 15, "w"),
                             (13, 0, 13, 15, 16, 15, "w"), (1, 6, 1, 15, 7, 3, "w"), (1, 11, 13, 15, 12, 15, "w")],
       {"w": "wp_white_plastic"}, display="Lifeguard Chair", unbreakable=True)
shaped("wp_lifeguard_seat", [(1, 0, 1, 3, 4, 3, "w"), (13, 0, 1, 15, 4, 3, "w"), (1, 0, 13, 3, 4, 15, "w"),
                             (13, 0, 13, 15, 4, 15, "w"), (2, 4, 2, 14, 6, 14, "w"), (2, 6, 12, 14, 16, 14, "w"),
                             (1, 6, 2, 2, 10, 13, "w"), (14, 6, 2, 15, 10, 13, "w")],
       {"w": "wp_white_plastic"}, display="Lifeguard Chair", unbreakable=True)
shaped("wp_umbrella_pole", [(7, 0, 7, 9, 16, 9, "w")], {"w": "wp_white_plastic"}, display="Umbrella", unbreakable=True)
shaped("wp_umbrella_top", [(7, 0, 7, 9, 6, 9, "w"), (-0, 6, 0, 16, 8, 16, {"all": "strap"})],
       {"w": "wp_white_plastic", "strap": "wp_strap"}, display="Umbrella", unbreakable=True, collides=False)

# ------------------------------------------------------------------ sound


def natatorium():
    d = 28.0
    t = Y.t_axis(d)
    hvac = Y.lp(Y.brown(d, "wphv"), 200) * 0.4 + Y.bp(Y.pink(d, "wphv2"), 300, 2500) * 0.1
    drips = np.zeros_like(t)
    r = Y.rng("wpdrips")
    for k in range(22):
        n = int(0.12 * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 900 + r.random() * 1400
        plink = np.sin(2 * np.pi * np.cumsum(f0 - tt * 3000) / Y.SR) * np.exp(-tt * 35)
        K.circular_place(drips, plink, r.random() * d, 0.15 + r.random() * 0.2)
    drips = K.loop_reverb(drips, 4.5, 0.85, "wpdr")
    pump = np.sin(2 * np.pi * 29 * t) * 0.06 + Y.lp(Y.white(d, "wppump"), 120) * 0.15
    x = hvac + drips + pump
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.003), "stereo"


def whistle():
    d = 1.0
    t = Y.t_axis(d)
    f = 3100 * (1 + 0.05 * np.sign(np.sin(2 * np.pi * 30 * t)))
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * np.minimum(t * 40, 1) * np.exp(-np.maximum(t - 0.6, 0) * 15)
    return Y.norm(Y.reverb(x, 3.0, 0.6, "wpwh"), 0.8)


def splash():
    d = 2.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "wpspl"), 500, 6000) * np.exp(-t * 3) * (1 - np.exp(-t * 60))
    return Y.norm(Y.reverb(x, 4.0, 0.8, "wpspl"), 0.5)


def slide_rumble():
    d = 3.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "wpsr"), 200, 1500) * np.sin(np.pi * t / d) ** 2 * (0.6 + 0.4 * np.sin(2 * np.pi * 3 * t))
    return Y.norm(Y.reverb(x, 3.0, 0.7, "wpsr"), 0.5)


def wave_open():
    c = K.chime([K.hz("C5"), K.hz("G5"), K.hz("E6")], gap=0.4, decay=1.6, seed="wpwo")
    v = K.pa_voice("wpwov", syllables=16, f0=200, rate=0.15, female=True)
    d = 6.0
    out = np.zeros(int(d * Y.SR) + len(v))
    Y.place(out, c, 0.0, 0.8)
    Y.place(out, v, 1.6, 0.8)
    return Y.norm(Y.reverb(out, 4.0, 0.7, "wpwo"), 0.85)


def surge():
    d = 6.0
    t = Y.t_axis(d)
    x = Y.lp(Y.white(d, "wpsu"), 900) * np.minimum(t / 2, 1) * np.minimum((d - t), 1)
    x += Y.lp(Y.brown(d, "wpsu2"), 80) * 0.8 * np.minimum(t / 2, 1)
    return Y.norm(Y.reverb(x, 3.0, 0.6, "wpsu"), 0.9)


sound("ambient.water_park", [("ambient/water_park", natatorium)], stream=True)
sound("ambient.water_park.additions",
      [("ambient/water_park_whistle", whistle), ("ambient/water_park_splash", splash),
       ("ambient/water_park_slide", slide_rumble)],
      subtitle="Somebody swimming")
sound("event.water_park_whistle", [("event/water_park_whistle", whistle)], subtitle="Lifeguard's whistle")
sound("event.water_park_waves", [("event/water_park_waves", wave_open)], subtitle="The wave pool opens")
sound("event.water_park_surge", [("event/water_park_surge", surge)], subtitle="Water rushes in")

level("water_park", fog="#4a6a62", sky="#4a6a62", water="#3ab8c8", water_fog="#0a3a40", ambient="ambient.water_park",
      additions="ambient.water_park.additions", additions_chance=0.005, fixed_time=18000, height=48,
      particle="minecraft:white_ash", particle_chance=0.004)
