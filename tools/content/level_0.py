"""Level 0 - The Lobby. Mono-yellow wallpaper, moist carpet, endless hum."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, level, sound, lang
import tex as T
import synth as Y

S = 32
YELLOW = ("#b9a65a", "#cbb96c", "#d6c67c")


def wallpaper_base_pattern(seed="wp"):
    """Faded mono-yellow paper with faint vertical bands and a repeating chevron motif."""
    n = T.fbm(seed, octaves=4, base=6)
    img = T.ramp(n * 0.6 + 0.2, *YELLOW)
    yy, xx = np.mgrid[0:S, 0:S]
    band = ((xx % 8) < 1).astype(float) * 0.06 + ((xx % 8) == 4).astype(float) * 0.03
    chev = (np.abs(((yy + np.abs((xx % 8) - 4)) % 8) - 4) < 0.6).astype(float) * ((xx % 8) > 1) * ((xx % 8) < 7)
    img = T.shade(img, 1 - band * 1.2 - chev * 0.075)
    # long vertical discolouration, like the paper has been damp for years
    streak = T.noise(seed + "streak", sigma=0, shape=(1, S))
    streak = np.repeat(T.noise(seed + "s2", shape=(1, S), sigma=0) * 0 + streak, S, axis=0)
    from scipy.ndimage import gaussian_filter1d
    streak = gaussian_filter1d(streak, 1.5, axis=1, mode="wrap")
    img = T.shade(img, 1 - (streak - 0.5) * 0.08)
    return T.grain(img, seed, 0.035)


def wallpaper():
    return wallpaper_base_pattern()


def wallpaper_stained():
    img = wallpaper_base_pattern("wps")
    img = T.stains(img, "wps", "#7d6a2c", count=2, strength=0.45, size=9)
    # drip trails running down from the stain
    r = T.rng("drip")
    for _ in range(3):
        x = int(r.random() * S)
        y0 = int(r.random() * 10)
        ln = 8 + int(r.random() * 18)
        for y in range(y0, min(S, y0 + ln)):
            a = 0.25 * (1 - (y - y0) / ln)
            img[y, x] = img[y, x] * (1 - a) + T.hexc("#6b5a24") * a
    return img


def wallpaper_base():
    """Bottom row of wall: paper with a scuffed brown baseboard."""
    img = wallpaper_base_pattern("wpb")
    yy = np.arange(S)
    board = T.ramp(T.fbm("board", octaves=3, base=2), "#5a4630", "#6e573a")
    img[S - 6:] = board[S - 6:]
    img[S - 6] = T.hexc("#7c6444")
    img[S - 1] = T.hexc("#3d2f20")
    # grime creeping up from the carpet
    grime = np.clip((yy[:, None] - (S - 12)) / 8.0, 0, 1) * T.noise("grime", sigma=1.5)
    img = T.shade(img, 1 - grime * 0.25)
    return T.grain(img, "wpb", 0.03)


def wallpaper_peeling():
    """A seam where the paper has lifted and curled, showing a sliver of drywall."""
    img = wallpaper_base_pattern("wpp")
    yy, xx = np.mgrid[0:S, 0:S]
    seam = 21
    wob = (np.sin(yy / 3.0) * 0.8 + T.noise("pw", sigma=1.5)[:, :1] * 1.5).astype(float)
    gap = (np.abs(xx - seam - wob) < 1.2) & (yy > 6) & (yy < 27)
    drywall = T.ramp(T.noise("dw", sigma=1), "#9a968a", "#b3ae9f")
    img = T.blend(img, drywall, gap.astype(float))
    curl = (np.abs(xx - seam - 2 - wob) < 1.0) & (yy > 6) & (yy < 27)
    img = T.blend(img, T.solid("#e3d48f"), curl.astype(float) * 0.9)
    shadow = (np.abs(xx - seam - 3.5 - wob) < 1.0) & (yy > 6) & (yy < 27)
    img = T.shade(img, 1 - shadow.astype(float) * 0.18)
    return T.stains(img, "wpp2", "#7d6a2c", count=1, strength=0.2, size=6)


def wallpaper_outlet():
    img = wallpaper_base_pattern("wpo")
    img = T.rect(img, 12, 18, 20, 30, "#e7e1cf")
    img = T.rect(img, 12, 18, 20, 19, "#f5f0e0")
    img = T.rect(img, 14, 21, 15, 24, "#3a3a3a")
    img = T.rect(img, 17, 21, 18, 24, "#3a3a3a")
    img = T.rect(img, 15, 26, 17, 28, "#3a3a3a")
    return img


def wallpaper_smiley():
    """Someone drew this. Recently."""
    img = wallpaper_base_pattern("wpsm")
    ink = T.hexc("#2a2418")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 14.5) ** 2)
    ring = (np.abs(d - 9) < 0.8)
    eyes = ((xx - 12) ** 2 + (yy - 11) ** 2 < 2) | ((xx - 19) ** 2 + (yy - 11) ** 2 < 2)
    mouth = (np.abs(d - 5.5) < 0.7) & (yy > 15)
    m = (ring | eyes | mouth).astype(float) * (0.7 + 0.3 * T.noise("ink", sigma=0.5))
    return T.blend(img, np.ones_like(img) * ink, m)


def carpet():
    fib = T.noise("cf", sigma=0.4)
    mot = T.fbm("cm", octaves=3, base=5)
    img = T.ramp(mot * 0.7 + fib * 0.3, "#6e5f2e", "#8a7838", "#9c8a44")
    return T.grain(img, "carpet", 0.05)


def carpet_damp():
    fib = T.noise("cdf", sigma=0.4)
    mot = T.fbm("cdm", octaves=3, base=5)
    img = T.ramp(mot * 0.6 + fib * 0.4, "#4a4020", "#5c512a", "#6c6032")
    sheen = (T.noise("sheen", sigma=2) > 0.7).astype(float) * 0.12
    return T.grain(T.shade(img, 1 + sheen), "carpetd", 0.05)


def ceiling():
    base = T.ramp(T.fbm("ct", octaves=2, base=4), "#cdc6a8", "#ddd6ba")
    pits = (T.rng("pits").random((S, S)) > 0.86).astype(float)
    img = T.shade(base, 1 - pits * 0.12)
    img = T.border(img, "#a8a28c", 1)
    img[0, :] = T.hexc("#bab39b")
    img[:, 0] = T.hexc("#bab39b")
    return T.grain(img, "ceil", 0.025)


def ceiling_stained():
    return T.stains(ceiling(), "cs", "#8a6d32", count=1, strength=0.55, size=11)


def ceiling_missing():
    img = T.solid("#0c0b09")
    img = T.blend(img, T.solid("#2a2620"), T.noise("cmis", sigma=2) * 0.4)
    # a dangling wire
    for y in range(4, 28):
        x = int(10 + 3 * np.sin(y / 4.0))
        img[y, x] = T.hexc("#5a1a12")
    img = T.border(img, "#a8a28c", 1)
    return img


def light_on():
    img = T.solid("#fffbea")
    yy, xx = np.mgrid[0:S, 0:S]
    diffuser = ((xx % 3 == 0) | (yy % 3 == 0)).astype(float) * 0.04
    tubes = (np.abs((xx % 16) - 8) < 2).astype(float) * 0.03
    img = T.shade(img, 1 - diffuser + tubes)
    img = T.border(img, "#b8b4a4", 2)
    img = T.border(img, "#8e8a7c", 1)
    return img


def light_off():
    img = T.ramp(T.noise("loff", sigma=2), "#7c7a70", "#8f8c80")
    yy, xx = np.mgrid[0:S, 0:S]
    diffuser = ((xx % 3 == 0) | (yy % 3 == 0)).astype(float) * 0.06
    img = T.shade(img, 1 - diffuser)
    img = T.rect(img, 6, 3, 9, 29, "#5e5c55")
    img = T.rect(img, 22, 3, 25, 29, "#5e5c55")
    img = T.rect(img, 6, 3, 9, 6, "#2b2a26")
    img = T.border(img, "#b8b4a4", 2)
    img = T.border(img, "#8e8a7c", 1)
    return img


tex("l0_wallpaper", wallpaper)
tex("l0_wallpaper_stained", wallpaper_stained)
tex("l0_wallpaper_base", wallpaper_base)
tex("l0_wallpaper_peeling", wallpaper_peeling)
tex("l0_wallpaper_outlet", wallpaper_outlet)
tex("l0_wallpaper_smiley", wallpaper_smiley)
tex("l0_carpet", carpet)
tex("l0_carpet_damp", carpet_damp)
tex("l0_ceiling", ceiling)
tex("l0_ceiling_stained", ceiling_stained)
tex("l0_ceiling_missing", ceiling_missing)
tex("l0_light", light_on)
tex("l0_light_off", light_off)

cube("yellow_wallpaper", "l0_wallpaper", sound="wool")
cube("yellow_wallpaper_stained", "l0_wallpaper_stained", display="Stained Yellow Wallpaper", sound="wool")
cube("yellow_wallpaper_baseboard", "l0_wallpaper_base", display="Yellow Wallpaper Baseboard", sound="wool")
cube("yellow_wallpaper_peeling", "l0_wallpaper_peeling", display="Peeling Yellow Wallpaper", sound="wool")
cube("yellow_wallpaper_outlet", "l0_wallpaper_outlet", display="Wall Outlet", sound="wool")
cube("yellow_wallpaper_smiley", "l0_wallpaper_smiley", display="Scrawled Wallpaper", sound="wool")
cube("moist_carpet", "l0_carpet", sound="moist")
cube("damp_carpet", "l0_carpet_damp", sound="moist")
cube("ceiling_tile", "l0_ceiling", sound="wool", flat=True)
cube("stained_ceiling_tile", "l0_ceiling_stained", sound="wool", flat=True)
cube("missing_ceiling_tile", "l0_ceiling_missing", sound="wool", flat=True)
flicker_light("fluorescent_light", "l0_light", "l0_light_off", "l0_ceiling", light=15, flicker=0.0,
              display="Fluorescent Light")
flicker_light("flickering_fluorescent_light", "l0_light", "l0_light_off", "l0_ceiling", light=15, flicker=0.6,
              display="Flickering Fluorescent Light")
cube_faces("dead_fluorescent_light", bottom="l0_light_off", top="l0_ceiling", side="l0_ceiling",
           display="Dead Fluorescent Light", sound="glass", flat=True)


def chair_fabric():
    return T.grain(T.ramp(T.noise("chf", sigma=0.6), "#2f3440", "#3b4150"), "chf", 0.04)


def chair_metal():
    return T.grain(T.ramp(T.noise("chm", sigma=1), "#5a5c60", "#7a7c80"), "chm", 0.03)


tex("office_chair_fabric", chair_fabric)
tex("office_chair_metal", chair_metal)
shaped("office_chair", [
    (3, 6, 3, 13, 8, 13, "fabric"),
    (3, 8, 12, 13, 18, 14, "fabric"),
    (7, 1, 7, 9, 6, 9, "metal"),
    (2, 0, 7, 14, 1, 9, "metal"),
    (7, 0, 2, 9, 1, 14, "metal"),
], {"fabric": "office_chair_fabric", "metal": "office_chair_metal"}, display="Office Chair", sound="wool")

# ------------------------------------------------------------------ sound


def lobby_hum():
    d = 24.0
    x = Y.hum(d, 60.0, "l0", buzz=0.35) * 0.55
    # a second ballast slightly out of phase gives the slow beating you feel in your teeth
    x += Y.hum(d, 60.0 + 1 / 6.0, "l0b", buzz=0.2, harmonics=(2, 4, 6)) * 0.25
    # high whine of a failing tube
    t = Y.t_axis(d)
    whine = np.sin(2 * np.pi * (round(9600 * d) / d) * t) * 0.012 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 8))
    room = Y.lp(Y.brown(d, "l0room"), 300) * 0.25
    x = Y.lp(x, 2500) + whine + room
    x = Y.loopable(Y.norm(x, 0.5), 1.0)
    return Y.stereoize(x, 0.0007), "stereo"


def distant_steps(i):
    def f():
        out = np.zeros(int(6 * Y.SR))
        rate = 0.55 + 0.1 * i
        for k in range(7 + i):
            g = 0.25 + 0.5 * np.sin(np.pi * k / (7 + i))
            Y.place(out, Y.footstep(f"ds{i}{k}", "carpet"), 0.3 + k * rate, g)
        out = Y.lp(out, 900)
        return Y.reverb(out, 2.5, 0.65, f"ds{i}")
    return f


def buzz_surge():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.hum(d, 60.0, "surge", buzz=1.0) * np.exp(-((t - 1.2) ** 2) / 0.3)
    crackle = (Y.rng("crk").random(len(t)) > 0.9993).astype(float) * Y.white(d, "crk2")
    return Y.norm(Y.hp(x, 100) + Y.bp(crackle, 2000, 9000) * 2, 0.6)


def distant_thump():
    out = np.zeros(int(4 * Y.SR))
    Y.place(out, Y.impact("dthump", 1.6), 0.1)
    return Y.reverb(Y.lp(out, 400), 3.0, 0.6, "dthump")


sound("ambient.level_0", [("ambient/level_0", lobby_hum)], stream=True)
sound("ambient.level_0.additions",
      [(f"ambient/level_0_steps{i}", distant_steps(i)) for i in range(3)] +
      [("ambient/level_0_surge", buzz_surge), ("ambient/level_0_thump", distant_thump)],
      subtitle="Something in the halls")

level("level_0", fog="#b9a95e", sky="#b9a95e", ambient="ambient.level_0",
      additions="ambient.level_0.additions", additions_chance=0.004, fixed_time=6000, height=64)


def squelch(i):
    def f():
        n = int(0.32 * Y.SR)
        w = Y.white(0.32, f"sq{i}")
        t = np.arange(n) / Y.SR
        body = Y.lp(w, 700 + i * 80) * np.exp(-t * 18)
        wet = Y.bp(w, 1800, 5200) * np.exp(-((t - 0.06) ** 2) / 0.0015) * 0.5
        bubbles = np.zeros(n)
        for k in range(3):
            fr = 900 + 400 * k + i * 120
            st = int((0.05 + 0.04 * k) * Y.SR)
            seg = np.arange(n - st) / Y.SR
            bubbles[st:] += np.sin(2 * np.pi * fr * seg * (1 + seg * 3)) * np.exp(-seg * 60) * 0.25
        return Y.norm(body + wet + bubbles, 0.7)
    return f


def zap(i):
    def f():
        d = 0.5
        t = Y.t_axis(d)
        buzz = np.sign(np.sin(2 * np.pi * 120 * t)) * 0.4 + Y.bp(Y.white(d, f"zap{i}"), 2000, 8000) * 0.5
        env_ = (np.random.default_rng(i).random(len(t)) > 0.4).astype(float)
        env_ = Y.lp(env_, 60) * np.exp(-t * 5)
        return Y.norm(Y.hp(buzz * env_, 150), 0.5)
    return f


sound("block.moist_carpet.step", [(f"block/moist_carpet_step{i}", squelch(i)) for i in range(4)], volume=0.6)
sound("block.light.zap", [(f"block/light_zap{i}", zap(i)) for i in range(3)], subtitle="A light buzzes")
