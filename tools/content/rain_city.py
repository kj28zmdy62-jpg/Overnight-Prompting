"""Rain City. Neon streets at night in rain that never stops, puddles full of reflected
signs, and nobody anywhere. Something walks these streets with you. You can't see it.
You can see the puddles splash where it steps, and the signs die as it passes."""
import numpy as np
from assets import cube, cube_faces, shaped, custom, exit_block, pane, stairs, level, sound, BLOCK_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import animated, draw_text, centered, text_width, box_model, facing_states, paint_metal, FONT

S = 32
tx = A.tex

PINK, CYAN, AMBER, GREEN = "#ff4aa8", "#3ae8ff", "#ffb03a", "#6aff8a"


def wet_asphalt(seed, mark=None):
    img = T.ramp(T.fbm(seed, octaves=4, base=4) * 0.5 + T.noise(seed + "f", sigma=0.4) * 0.5, "#101216", "#181a20", "#20232a")
    sheen = T.noise(seed + "sh", sigma=2.5)
    img = T.blend(img, T.solid("#3a4250"), np.clip(sheen - 0.55, 0, 1) * 0.8)
    yy, xx = np.mgrid[0:S, 0:S]
    if mark == "lane":
        img = T.blend(img, T.solid("#c8c4b8"), ((np.abs(yy - 15.5) < 2) & (xx < 20)) * 0.85)
    elif mark == "zebra":
        img = T.blend(img, T.solid("#d8d4c8"), ((xx % 16) < 8) * 0.85)
    return T.grain(img, seed, 0.03)


def pavement():
    img = T.ramp(T.fbm("rcpave", octaves=3, base=4), "#2a2c30", "#34363c")
    img = T.border(img, "#1a1b1e", 1, sides="nw")
    img[:, 15] = img[:, 15] * 0.8
    img[15, :] = img[15, :] * 0.8
    sheen = T.noise("rcpsh", sigma=2) > 0.7
    img[sheen] = img[sheen] * 1.3
    return T.grain(np.clip(img, 0, 1), "rcpave", 0.03)


def curb():
    img = T.ramp(T.noise("rccurb", sigma=1), "#4a4c50", "#5a5c60")
    return T.grain(img, "rccurb", 0.03)


tx("rc_asphalt", lambda: wet_asphalt("rca"))
tx("rc_asphalt_lane", lambda: wet_asphalt("rcal", "lane"))
tx("rc_asphalt_zebra", lambda: wet_asphalt("rcaz", "zebra"))
tx("rc_pavement", pavement)
tx("rc_curb", curb)
cube("rc_asphalt", "rc_asphalt", display="Wet Asphalt")
cube("rc_asphalt_lane", "rc_asphalt_lane", display="Wet Asphalt")
cube("rc_asphalt_zebra", "rc_asphalt_zebra", display="Crosswalk")
cube("rc_pavement", "rc_pavement", display="Wet Pavement")
cube("rc_curb", "rc_curb", display="Curb")


def puddle():
    rgb = T.ramp(T.noise("rcpud", sigma=3), "#0a0c10", "#141820")
    a = np.ones((S, S)) * 0.62
    yy, xx = np.mgrid[0:S, 0:S]
    rings = (np.sin(np.sqrt((xx - 10) ** 2 + (yy - 20) ** 2) * 1.6) > 0.92) | (np.sin(np.sqrt((xx - 24) ** 2 + (yy - 8) ** 2) * 1.6) > 0.92)
    rgb[rings] = T.hexc("#4a5260")
    a[rings] = 0.7
    return rgb, a


def reflection(c):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        base = T.solid("#05060a")
        streak = np.exp(-((xx - 16 - np.sin(yy / 3) * 2) ** 2) / 30) * (0.5 + 0.5 * T.noise("rcrf" + c, sigma=1.5))
        glow = T.solid(c)
        img = T.blend(base, glow, np.clip(streak * 0.55, 0, 1))
        smear = (T.noise("rcrs" + c, sigma=0.8) > 0.7) & (np.abs(xx - 16) < 10)
        img[smear] = img[smear] * 0.5 + T.hexc(c) * 0.3
        return img
    return f


tx("rc_puddle", puddle)
cube("rc_puddle", "rc_puddle", display="Puddle", kind="glass", render="translucent", sound="mud")
for name, c in (("pink", PINK), ("cyan", CYAN), ("amber", AMBER)):
    tx(f"rc_reflect_{name}", reflection(c))
    cube(f"rc_reflect_{name}", f"rc_reflect_{name}", display="Reflected Neon", light=3, emissive=True, flat=True)

# ------------------------------------------------------------------ buildings


def facade(seed, lo, hi, kind):
    def f():
        img = T.ramp(T.fbm(seed, octaves=3, base=3), lo, hi)
        yy, xx = np.mgrid[0:S, 0:S]
        if kind == "tile":
            img = T.shade(img, 1 - (((xx % 8) == 0) | ((yy % 8) == 0)) * 0.18)
        elif kind == "brick":
            off = ((yy // 4) % 2) * 4
            img = T.shade(img, 1 - (((yy % 4) == 0) | (((xx + off) % 8) == 0)) * 0.3)
        else:
            img = T.shade(img, 1 - ((yy % 16) == 0) * 0.2)
        streaks = (T.noise(seed + "st", shape=(1, S), sigma=0.6)[0] > 0.7)[None, :] & (T.noise(seed + "st2", sigma=2) > 0.4)
        img[streaks] = img[streaks] * 0.7
        return T.grain(img, seed, 0.04)
    return f


def window(state):
    def f():
        if state == "dark":
            img = T.ramp(T.noise("rcwd", sigma=3), "#06080c", "#10141a")
            img = T.blend(img, T.solid("#2a3040"), (T.noise("rcwdr", sigma=2) > 0.7) * 0.5)
        elif state == "warm":
            img = T.ramp(T.noise("rcww", sigma=3), "#d89a50", "#f0c070")
            blind = (np.mgrid[0:S, 0:S][0] % 4) < 1
            img[blind] = img[blind] * 0.7
        else:
            img = T.ramp(T.noise("rcwc", sigma=3), "#5a8aa8", "#8ab8d8")
            tv = (T.noise("rcwtv", sigma=4) > 0.6)
            img[tv] = img[tv] * 1.2
        img = T.border(img, "#202228", 2)
        img[15:17, :] = T.hexc("#202228")
        return np.clip(img, 0, 1)
    return f


for name, (lo, hi, kind) in {"grey": ("#2e3034", "#3a3c42", "plain"), "brick": ("#3a2420", "#4a2e28", "brick"),
                             "tile": ("#2a3436", "#34403e", "tile")}.items():
    tx(f"rc_facade_{name}", facade("rcf" + name, lo, hi, kind))
    cube(f"rc_facade_{name}", f"rc_facade_{name}", display="Building Facade")
for state in ("dark", "warm", "cool"):
    tx(f"rc_window_{state}", window(state))
    if state == "dark":
        cube("rc_window_dark", "rc_window_dark", display="Dark Window", sound="glass")
    else:
        cube(f"rc_window_{state}", f"rc_window_{state}", display="Lit Window", sound="glass", light=10, emissive=True, flat=True)


def shopfront():
    rgb = T.ramp(np.linspace(0, 1, S)[:, None] * np.ones((1, S)), "#f0e0c0", "#c8b090")
    a = np.ones((S, S)) * 0.55
    yy, xx = np.mgrid[0:S, 0:S]
    shelves = (yy % 10) < 1
    rgb[shelves] = T.hexc("#5a4a3a")
    a[shelves] = 0.9
    frame = (xx < 1) | (xx > 30)
    rgb[frame] = T.hexc("#1a1a1e")
    a[frame] = 1
    return rgb, a


tx("rc_shopfront", shopfront)
pane("rc_shopfront", "rc_shopfront", display="Shop Window", sound="glass", render="translucent")
tx("rc_shop_light", lambda: T.solid("#f8ecd0"))
cube("rc_shop_ceiling", "rc_shop_light", display="Shop Ceiling Light", light=14, emissive=True, flat=True, sound="glass")
tx("rc_shop_floor", lambda: T.border(T.ramp(T.noise("rcsf", sigma=2), "#c8c4b8", "#d4d0c4"), "#8a8680", 1))
cube("rc_shop_floor", "rc_shop_floor", display="Shop Floor", sound="tile")


def awning(c1, c2):
    def f():
        xx = np.mgrid[0:S, 0:S][1]
        img = np.where(((xx // 8) % 2 == 0)[..., None], T.hexc(c1), T.hexc(c2))
        drip = (T.noise("rcaw" + c1, sigma=1.5) > 0.7)
        img[drip] = img[drip] * 0.8
        return T.grain(img, "rcaw" + c1, 0.03)
    return f


tx("rc_awning_red", awning("#5a1a1e", "#3a1014"))
tx("rc_awning_green", awning("#1a3a2a", "#10261c"))
for c in ("red", "green"):
    shaped(f"rc_awning_{c}", [(0, 10, 0, 16, 12, 16, "a")], {"a": f"rc_awning_{c}"}, display="Awning",
           unbreakable=True, collides=False, sound="wool")

# ------------------------------------------------------------------ neon


def neon_word(text, color, on=True):
    def f():
        img = T.solid("#0a0a0e")
        txt = np.zeros((S, S, 3))
        sc = 2 if text_width(text, 2) <= 28 else 1
        txt = centered(txt, text, (S - 5 * sc) // 2, "#ffffff", scale=sc)
        m = txt[..., 0] > 0.5
        from scipy.ndimage import gaussian_filter
        glow = gaussian_filter(m.astype(float), 1.6)
        c = T.hexc(color)
        if on:
            img = img + c[None, None, :] * np.clip(glow * 2.2, 0, 1)[..., None] * 0.8
            img[m] = np.clip(c * 0.5 + 0.6, 0, 1)
        else:
            img[m] = c * 0.18
        return np.clip(T.border(img, "#2a2a30", 1), 0, 1)
    return f


def neon_letter(ch, color, on=True):
    def f():
        img = T.solid("#0a0a0e")
        g = FONT.get(ch, FONT["?"])
        m = np.zeros((S, S), bool)
        for gy, row in enumerate(g):
            for gx, bit in enumerate(row):
                if bit == "1":
                    m[4 + gy * 5:4 + gy * 5 + 4, 8 + gx * 5:8 + gx * 5 + 4] = True
        from scipy.ndimage import gaussian_filter
        glow = gaussian_filter(m.astype(float), 1.8)
        c = T.hexc(color)
        if on:
            img = img + c[None, None, :] * np.clip(glow * 2.0, 0, 1)[..., None] * 0.85
            img[m] = np.clip(c * 0.5 + 0.6, 0, 1)
        else:
            img[m] = c * 0.18
        return np.clip(T.border(img, "#2a2a30", 1), 0, 1)
    return f


tx("rc_neon_case", lambda: paint_metal("rcnc", "#1a1a1e", "#26262c"))
NEON_BOX = [(0, 0, 13, 16, 16, 16, {"all": "case", "north": "face"})]


def neon(name, on_painter, off_painter, display):
    tx(name + "_on", on_painter)
    tx(name + "_off", off_painter)
    for state in ("on", "off"):
        BLOCK_MODELS[f"{name}_{state}"] = box_model(NEON_BOX, {"case": "rc_neon_case", "face": f"{name}_{state}"})
    custom(name, "com.noclip.block.NeonBlock", box_model(NEON_BOX, {"case": "rc_neon_case", "face": f"{name}_on"}),
           blockstate=facing_states(lambda c: f"{name}_{'on' if c['lit'] == 'true' else 'off'}", {"lit": ["true", "false"]}),
           display=display, sound="glass", unbreakable=True, render="cutout", emissive=True)


WORDS = [("BAR", PINK), ("OPEN", CYAN), ("24H", AMBER), ("EAT", GREEN), ("INN", PINK), ("RAMEN", CYAN),
         ("LIVE", AMBER), ("ROOMS", GREEN)]
for i, (word, col) in enumerate(WORDS):
    neon(f"rc_neon_{i}", neon_word(word, col), neon_word(word, col, False), f"Neon Sign ({word.title()})")
LETTERS = sorted(set("HOTELKARAOKEMOTEL"))
for col_name, col in (("pink", PINK), ("cyan", CYAN)):
    for ch in LETTERS:
        neon(f"rc_letter_{col_name}_{ch.lower()}", neon_letter(ch, col), neon_letter(ch, col, False), f"Neon Letter {ch}")

# ------------------------------------------------------------------ street furniture


def vending():
    img = T.solid("#e8ecf0")
    img = T.rect(img, 3, 3, 20, 22, "#2a3a4a")
    r = T.rng("rcvend")
    cols = ["#c03028", "#2a5aa8", "#e8c040", "#3a8a3a", "#e0e0e0"]
    for k in range(12):
        x, y = 4 + (k % 4) * 4, 4 + (k // 4) * 6
        img = T.rect(img, x, y, x + 3, y + 5, cols[int(r.random() * len(cols))])
    img = T.rect(img, 22, 6, 28, 10, "#101010")
    img = T.rect(img, 22, 12, 28, 20, "#5a5a5a")
    img = T.rect(img, 4, 25, 20, 29, "#101010")
    return img


tx("rc_vending", vending)
tx("rc_vending_side", lambda: T.grain(T.solid("#2a5aa8"), "rcvs", 0.03))
shaped("rc_vending", [(1, 0, 3, 15, 16, 16, {"all": "s", "north": "f"})], {"s": "rc_vending_side", "f": "rc_vending"},
       display="Vending Machine", light=12, emissive=True, unbreakable=True, sound="metal", render="solid")


def traffic(i):
    img = paint_metal("rctl", "#1a1c1a", "#2a2c2a")
    yy, xx = np.mgrid[0:S, 0:S]
    for k, (cy, col) in enumerate(((6, "#3a0a08"), (16, "#ffb020" if i == 0 else "#3a2a08"), (26, "#082a10"))):
        img[((xx - 16) ** 2 + (yy - cy) ** 2) < 14] = T.hexc(col)
    return img


animated("rc_traffic", traffic, 2, frametime=14)
tx("rc_pole", lambda: paint_metal("rcpole", "#2a2c30", "#3a3c40"))
shaped("rc_pole", [(7, 0, 7, 9, 16, 9, "p")], {"p": "rc_pole"}, display="Pole", unbreakable=True)
shaped("rc_traffic_light", [(7, 0, 7, 9, 4, 9, "p"), (4, 4, 4, 12, 16, 12, {"all": "p", "north": "t!", "south": "t!", "east": "t!", "west": "t!"})],
       {"p": "rc_pole", "t": "rc_traffic"}, display="Traffic Light", light=8, emissive=True, unbreakable=True)
tx("rc_lamp", lambda: T.solid("#dceeff"))
shaped("rc_street_lamp", [(7, 0, 7, 9, 10, 9, "p"), (7, 9, 2, 9, 11, 9, "p"), (5, 7, 0, 11, 9, 5, {"all": "p", "down": "l"})],
       {"p": "rc_pole", "l": "rc_lamp"}, display="Street Lamp", light=14, emissive=True, unbreakable=True)


def manhole():
    img = paint_metal("rcmh", "#1e1e20", "#2a2a2c", rust=0.2)
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.shade(img, 1 - ((d > 13.5) & (d < 15)) * 0.6 - (((xx + yy) % 5 == 0) & (d < 12)) * 0.2)
    return img


tx("rc_manhole", manhole)
cube("rc_manhole", "rc_manhole", display="Manhole Cover", sound="metal")


def subway_sign():
    img = T.solid("#1a2a6a")
    img = T.border(img, "#e8e8e0", 1)
    img = centered(img, "SUB", 6, "#e8e8e0", scale=2)
    img = centered(img, "WAY", 18, "#e8e8e0")
    return img


tx("rc_subway_sign", subway_sign)
shaped("rc_subway_sign", [(7, 0, 7, 9, 10, 9, "p"), (2, 10, 6, 14, 16, 10, {"all": "p", "north": "s!", "south": "s!"})],
       {"p": "rc_pole", "s": "rc_subway_sign"}, display="Subway Sign", light=9, emissive=True, unbreakable=True,
       render="solid")
tx("rc_stair_dark", lambda: T.solid("#020204"))
exit_block("rc_subway_dark", "rc_stair_dark", "DESCEND", display="Subway Stairs")
stairs("rc_stairs", "rc_pavement", "rc_pavement", display="Subway Stairs")


def hotel_door():
    img = T.ramp(T.noise("rchd", sigma=2), "#3a1a14", "#4a2218")
    img = T.border(img, "#c8a050", 2)
    img = T.rect(img, 6, 6, 26, 20, "#ffd890")
    img[13, 6:26] = T.hexc("#c8a050")
    return img


tx("rc_hotel_door", hotel_door)
exit_block("rc_hotel_door", "rc_hotel_door", "DOOR", trigger="USE", solid=True, display="Hotel Door", sound="wood",
           light=7)

# ------------------------------------------------------------------ sound


def rain_loop():
    d = 24.0
    t = Y.t_axis(d)
    rain = Y.bp(Y.pink(d, "rcrain"), 500, 9000) * 0.6
    drops = np.zeros_like(t)
    r = Y.rng("rcdrops")
    for k in range(260):
        n = int(0.03 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(drops, Y.hp(Y.white(0.03, f"rcd{k % 20}"), 2500)[:n] * np.exp(-tt * 200), r.random() * (d - 0.05), 0.05 + r.random() * 0.15)
    gutter = Y.lp(Y.white(d, "rcgut"), 700) * 0.15 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 6))
    hum = Y.hum(d, 60, "rcneon", buzz=0.6, harmonics=(2, 4, 6)) * 0.02
    x = Y.loopable(Y.norm(rain + drops + gutter + hum, 0.5), 1.5)
    return Y.stereoize(x, 0.0024), "stereo"


def thunder():
    d = 6.0
    t = Y.t_axis(d)
    x = Y.lp(Y.brown(d, "rcth"), 160) * np.exp(-t * 0.7) * (1 - np.exp(-t * 6))
    x += Y.lp(Y.white(d, "rcth2"), 400) * np.exp(-((t - 0.8) ** 2) / 0.1) * 0.4
    return Y.norm(x, 0.7)


def siren():
    d = 5.0
    t = Y.t_axis(d)
    f = 700 + 300 * np.sin(2 * np.pi * 0.4 * t)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * np.sin(np.pi * t / d) ** 2
    return Y.reverb(Y.lp(Y.norm(x, 0.2), 2000), 4.0, 0.85, "rcsiren")


def neon_buzz():
    d = 1.5
    t = Y.t_axis(d)
    x = Y.hum(d, 60, "rcnb", buzz=1.0, harmonics=(2, 4, 6, 8)) * (Y.rng("rcnbr").random(len(t)) > 0.3)
    return Y.norm(Y.hp(x, 300) * Y.env(len(t), 0.02, 0.2), 0.4)


def splash_step():
    out = np.zeros(int(0.8 * Y.SR))
    n = int(0.4 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, Y.bp(Y.white(0.4, "rcss"), 500, 6000) * np.exp(-tt * 12) * (0.6 + 0.4 * np.sin(tt * 300)), 0.02, 1.0)
    Y.place(out, Y.lp(Y.white(0.4, "rcss2"), 250) * np.exp(-tt * 20), 0.0, 0.8)
    return Y.norm(out, 0.7)


sound("ambient.rain_city", [("ambient/rain_city", rain_loop)], stream=True)
sound("ambient.rain_city.additions",
      [("ambient/rain_city_thunder", thunder), ("ambient/rain_city_siren", siren), ("ambient/rain_city_buzz", neon_buzz)],
      subtitle="Something in the city")
sound("event.rain_city_step", [("event/rain_city_step", splash_step)], subtitle="Footsteps in the puddles")

level("rain_city", fog="#0e0c16", sky="#0a0812", ambient="ambient.rain_city", additions="ambient.rain_city.additions",
      additions_chance=0.003, fixed_time=18000, skylight=True, ceiling=False, height=96,
      particle="minecraft:rain", particle_chance=0.2)
