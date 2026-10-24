"""Dead Mall - a 1990s shopping mall after closing. Terrazzo, teal and mauve, skylights full of
night, fountains still running, slowed-down muzak. The mannequins step out of the windows when
you look away."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, exit_block, stairs, cross, flicker_light, level, sound
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32

# ------------------------------------------------------------------ surfaces

CHIPS = [("#2f7c78", 34), ("#d0806e", 26), ("#4a4a4c", 30), ("#f4efe6", 22), ("#b58c4a", 10)]


def terrazzo(seed, lo, hi, chips=CHIPS, sheen=True):
    img = T.ramp(T.fbm(seed, octaves=3, base=6) * 0.6 + 0.2, lo, hi)
    r = T.rng(seed, "chips")
    for col, n in chips:
        c = T.hexc(col)
        for _ in range(int(n * 0.5)):
            x, y = int(r.random() * S), int(r.random() * S)
            shade = 0.85 + r.random() * 0.3
            c = T.hexc(col) * 0.55 + img[y, x] * 0.45
            img[y, x] = c * shade
            if r.random() < 0.45:
                img[y, (x + 1) % S] = c * shade
            if r.random() < 0.25:
                img[(y + 1) % S, x] = c * shade * 0.95
    if sheen:
        # polished floor: soft bright patches where the overhead light catches
        img = T.shade(img, 1 + np.clip(T.noise(seed + "sh", sigma=4) - 0.6, 0, 1) * 0.25)
    return T.grain(img, seed, 0.03)


def floor_main():
    img = terrazzo("mtz", "#d3c8b8", "#e2d9ca")
    return T.stains(img, "mtzs", "#a89a80", count=1, strength=0.12, size=10)


def floor_teal():
    img = terrazzo("mtt", "#2c6f6c", "#367d79",
                   chips=[("#e8e0d0", 40), ("#1c4644", 30), ("#d0806e", 10), ("#b58c4a", 6)])
    img = T.border(img, "#b8964e", 1)
    return img


def floor_salmon():
    img = terrazzo("mts", "#c98674", "#d6947f",
                   chips=[("#efe6d8", 40), ("#8a4a3e", 26), ("#2f7c78", 12)])
    return T.border(img, "#b8964e", 1)


def wall():
    img = T.ramp(T.fbm("mwall", octaves=4, base=3) * 0.7 + T.noise("mwallf", sigma=0.5) * 0.3,
                 "#cfae9e", "#dcbcab", "#e3c7b6")
    img = T.stains(img, "mwalls", "#a88070", count=1, strength=0.15, size=12)
    return T.grain(img, "mwall", 0.035)


def wall_base():
    img = wall()
    tile = T.ramp(T.noise("mwbt", sigma=1.2), "#2a706c", "#3a8884")
    yy, xx = np.mgrid[0:S, 0:S]
    grout = ((xx % 6) == 0) | (yy == 22) | (yy == 27)
    tile = T.shade(tile, 1 - grout * 0.35)
    img[22:] = tile[22:]
    img[21] = T.hexc("#b8964e")
    img[20] = T.hexc("#8a6a3a") * 0.9 + img[20] * 0.1
    return K.grime_bottom(T.grain(img, "mwb", 0.03), "mwbg", 8, 0.3)


def bulkhead():
    img = T.ramp(T.fbm("mbulk", octaves=3, base=4), "#6e4258", "#7e4e66")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.18)
    for y, c in ((1, "#d0ad5c"), (2, "#8e7034"), (29, "#d0ad5c"), (30, "#8e7034")):
        img[y] = T.hexc(c)
    return T.grain(img, "mbulk", 0.03)


def fascia():
    img = T.ramp(T.fbm("mfas", octaves=3, base=5), "#c8c3b6", "#d6d1c4")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 16) == 15) * 0.25 + ((yy % 16) == 0) * 0.06)
    rivet = ((yy % 16) == 3) & ((xx % 8) == 4)
    img = T.shade(img, 1 - rivet * 0.3)
    return T.grain(T.stains(img, "mfass", "#8c8470", count=1, strength=0.12, size=9), "mfas", 0.03)


def ceiling():
    base = T.ramp(T.fbm("mceil", octaves=2, base=4), "#d7d2c4", "#e2ddd0")
    pits = (T.rng("mcp").random((S, S)) > 0.85).astype(float)
    img = T.shade(base, 1 - pits * 0.1)
    yy, xx = np.mgrid[0:S, 0:S]
    grid = ((xx % 16) == 0) | ((yy % 16) == 0)
    img = T.blend(img, T.solid("#b9b3a4"), grid.astype(float))
    return T.grain(img, "mceil", 0.025)


def ceiling_stained():
    return T.stains(ceiling(), "mceils", "#9a7a48", count=1, strength=0.5, size=9)


def canlight(on=True):
    img = ceiling()
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    ring = (d < 10) & (d >= 8)
    img = T.blend(img, T.solid("#b8b8b4"), ring.astype(float))
    img = T.blend(img, T.solid("#e6e6e2"), ((d < 10.6) & (d >= 10)).astype(float))
    if on:
        core = T.ramp(np.clip(1 - d / 8, 0, 1), "#ffd58f", "#fff1cf", "#ffffff")
        img = T.blend(img, core, (d < 8).astype(float))
    else:
        dead = T.ramp(T.noise("mcan", sigma=1.5), "#4c4944", "#5c5852")
        img = T.blend(img, dead, (d < 8).astype(float))
        img = T.blend(img, T.solid("#6e6a62"), ((d < 3) & (d > 1.5)).astype(float))
    return img


def skylight():
    pane_c = T.ramp(T.fbm("msky", octaves=3, base=5), "#070d1c", "#0e1830", "#16233f")
    yy, xx = np.mgrid[0:S, 0:S]
    streak = np.clip(1 - np.abs((xx + yy * 0.6) % 22 - 6) / 2.5, 0, 1) * 0.35
    img = T.blend(pane_c, T.solid("#3a4c72"), streak)
    stars = (T.rng("mstar").random((S, S)) > 0.992).astype(float)
    img = T.blend(img, T.solid("#9aa6c8"), stars * 0.6)
    mull = ((xx % 16) < 2) | ((yy % 16) < 2)
    img = T.blend(img, T.ramp(T.noise("mmul", sigma=1), "#1f2629", "#2c3438"), mull.astype(float))
    return T.grain(img, "msky", 0.02)


def shutter(bottom=False, tag=False):
    yy, xx = np.mgrid[0:S, 0:S]
    rib = np.array([1.0, 0.86, 0.68, 0.84])[yy % 4]
    base = T.ramp(T.fbm("mshut", octaves=3, base=5), "#7c848a", "#929a9f")
    img = T.shade(base, rib)
    rust = (T.noise("mshr", sigma=1.2) > 0.82).astype(float) * (yy > 6)
    img = T.blend(img, T.solid("#7a5636"), rust * 0.45)
    img = K.grime_bottom(img, "mshg", 14, 0.35)
    if tag:
        # someone tagged it, years ago
        m = np.zeros((S, S))
        for k, (a, b, c, d) in enumerate([(4, 20, 10, 9), (10, 9, 14, 21), (14, 21, 20, 10), (20, 10, 27, 19)]):
            m = np.maximum(m, K.line_mask(S, S, a, b, c, d, 2.0))
        img = T.blend(img, T.solid("#b23a6a"), m * 0.75)
    if bottom:
        img[26:30] = T.ramp(T.noise("mshb", sigma=1, shape=(4, S)), "#4c5257", "#5a6166")
        img[30:] = T.hexc("#2a2d30")
        img = T.rect(img, 13, 22, 19, 26, "#3a3e42")
        img = T.rect(img, 14, 23, 18, 25, "#b0a070")
    return T.grain(img, "mshut" + str(bottom) + str(tag), 0.04)


def grille():
    yy, xx = np.mgrid[0:S, 0:S]
    bars = ((yy % 4) < 1)
    links = ((xx + (yy // 4 % 2) * 4) % 8 == 0)
    a = (bars | links).astype(float)
    rgb = T.ramp(T.noise("mgr", sigma=1), "#8e9498", "#b4babd")
    rgb = T.shade(rgb, 1 - links * 0.15)
    return rgb, a


def glass():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("mgl", sigma=3), "#22323c", "#2c3e48")
    streak = np.clip(1 - np.abs((xx - yy * 0.7) % 26 - 8) / 3, 0, 1)
    rgb = T.blend(rgb, T.solid("#9fb4c0"), streak * 0.4)
    a = 0.28 + streak * 0.35
    frame = (xx < 1) | (xx > 30) | (yy < 1) | (yy > 30)
    rgb = T.blend(rgb, T.solid("#a2a8ac"), frame.astype(float))
    a = np.where(frame, 1.0, a)
    return rgb, a


SIGNS = [
    ("CANDY", "BULK SWEETS", "#ff5fa8"),
    ("SHOES", "THE SHOE BOX", "#4ff0d8"),
    ("VIDEO", "RENT 2 GET 1", "#ffd84a"),
    ("MUSIC", "TAPES & CDS", "#b77cff"),
    ("GIFTS", "PALM COURT", "#ff9a4a"),
    ("PRETZEL", "HOT & FRESH", "#ffc23a"),
    ("PHOTO", "1 HOUR", "#8ad4ff"),
    ("TOYS", "KIDS KINGDOM", "#ff4a4a"),
    ("PERFUME", "FRAGRANCE", "#ff8ad8"),
    ("BOOKS", "MAGAZINES", "#7cff8a"),
]


def sign_image(big, small, color, lit=True):
    h, w = 32, 96
    img = T.ramp(T.fbm("msg" + big, octaves=2, base=4, shape=(h, w)), "#120e16", "#1c1622")
    yy, xx = np.mgrid[0:h, 0:w]
    img = T.shade(img, 1 - ((yy == 0) | (yy == h - 1) | (xx == 0) | (xx == w - 1)) * 0.5)
    m1 = K.centered(big, h, w, 3, scale=2, spacing=1)
    m2 = K.centered(small, h, w, 22, scale=1, spacing=1)
    m = np.maximum(m1, m2)
    c = T.hexc(color)
    if lit:
        halo = K.glow(m, 2.2)
        img = T.blend(img, np.ones((h, w, 3)) * c, halo * 0.55)
        core = np.ones((h, w, 3)) * (c * 0.55 + 0.45)
        img = T.blend(img, core, m)
    else:
        img = T.blend(img, np.ones((h, w, 3)) * (c * 0.25 + 0.12), m * 0.8)
    return T.grain(img, "msgg" + big, 0.03)


def store_carpet():
    img = T.ramp(T.fbm("mscp", octaves=3, base=4) * 0.5 + T.noise("mscpf", sigma=0.4) * 0.5, "#3e1824", "#4e2030")
    r = T.rng("confetti")
    for col in ("#2fa8a0", "#e0b030", "#8a5ad0", "#e05a8a"):
        for _ in range(7):
            x, y = r.random() * S, r.random() * S
            ang = r.random() * np.pi
            for k in range(4):
                px = int(x + np.cos(ang) * k) % S
                py = int(y + np.sin(ang) * k + np.sin(k * 1.4)) % S
                img[py, px] = T.hexc(col) * 0.8
    return T.grain(img, "mscp", 0.04)


def store_tile():
    yy, xx = np.mgrid[0:S, 0:S]
    chk = (((xx // 8) + (yy // 8)) % 2 == 0)
    img = np.where(chk[..., None], T.hexc("#d8d4cc"), T.hexc("#1e1e22"))
    img = T.shade(img, 0.9 + T.noise("mstl", sigma=2) * 0.15)
    return T.grain(T.stains(img, "mstls", "#5a5040", count=2, strength=0.25, size=7), "mstl", 0.04)


def store_wall():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("mswl", octaves=3, base=4), "#4c4a52", "#5a5860")
    img = T.shade(img, 1 - ((yy % 4) == 3) * 0.4)
    return T.grain(img, "mswl", 0.03)


def store_ceiling():
    img = T.ramp(T.fbm("msce", octaves=2, base=4), "#18161b", "#222026")
    yy, xx = np.mgrid[0:S, 0:S]
    return T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.3)


def store_light():
    img = T.solid("#fff6e4")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (((xx % 4) == 0) | ((yy % 4) == 0)) * 0.05)
    return T.border(T.border(img, "#9a968c", 2), "#4a4844", 1)


def planter_side():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mpl", sigma=1.2), "#c27c68", "#d08c76")
    grout = ((xx % 8) == 0) | ((yy % 8) == 0)
    img = T.shade(img, 1 - grout * 0.3)
    img[:3] = T.ramp(T.noise("mplc", sigma=1, shape=(3, S)), "#b8964e", "#d4b468")
    img[3] = T.hexc("#6e5428")
    return K.grime_bottom(T.grain(img, "mpl", 0.03), "mplg", 8, 0.25)


def planter_top():
    img = T.ramp(T.noise("mpt", sigma=0.8), "#2a1e14", "#3e2c1c")
    leaves = (T.noise("mptl", sigma=0.7) > 0.78).astype(float)
    img = T.blend(img, T.solid("#5a5a28"), leaves * 0.6)
    return T.border(img, "#c4a25a", 2)


def palm_fronds():
    h = w = S
    yy, xx = np.mgrid[0:h, 0:w]
    a = np.zeros((h, w))
    for k in range(9):
        ang = np.pi * (0.05 + 0.9 * k / 8)
        ex, ey = 15.5 + np.cos(ang) * 15, 20 - np.sin(ang) * 15 + (abs(np.cos(ang)) * 8)
        a = np.maximum(a, K.line_mask(h, w, 15.5, 20, ex, ey, 2.4))
        # leaflets
        for t in np.linspace(0.3, 1.0, 5):
            px, py = 15.5 + (ex - 15.5) * t, 20 + (ey - 20) * t
            a = np.maximum(a, K.line_mask(h, w, px, py, px + 2, py + 3, 1.2))
    trunk = (np.abs(xx - 15.5) < 1.5) & (yy > 18)
    a = np.maximum(a, trunk.astype(float))
    rgb = T.ramp(T.noise("mpf", sigma=0.8), "#2e5a26", "#4a7a34")
    rgb = np.where(trunk[..., None], T.hexc("#6a5030"), rgb)
    # dust on the plastic
    rgb = T.blend(rgb, T.solid("#8a8a70"), (T.noise("mpfd", sigma=1) > 0.75) * 0.4)
    return rgb, a


def palm_trunk():
    yy, xx = np.mgrid[0:S, 0:S]
    a = (np.abs(xx - 15.5) < 2.5).astype(float)
    rgb = T.ramp(T.noise("mptk", sigma=0.6), "#5a4028", "#7a5838")
    rgb = T.shade(rgb, 1 - ((yy % 5) == 0) * 0.35)
    return rgb, a


def wood_slat():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("mwd", octaves=3, base=3, shape=(S, S)), "#8a5a34", "#a06c40")
    img = T.shade(img, 1 - ((yy % 6) == 0) * 0.35)
    return T.grain(img, "mwd", 0.05)


def brass():
    return T.grain(T.ramp(T.noise("mbr", sigma=1.5), "#9a7a38", "#d0b060"), "mbr", 0.04)


def dark_metal():
    return T.grain(T.ramp(T.noise("mdm", sigma=1.2), "#2a2c2e", "#3a3c3e"), "mdm", 0.04)


def mosaic(seed, lo, hi, coins=False):
    yy, xx = np.mgrid[0:S, 0:S]
    cell = T.rng(seed).random((S // 4, S // 4))
    img = T.ramp(np.kron(cell, np.ones((4, 4))), lo, hi)
    img = T.shade(img, 1 - (((xx % 4) == 0) | ((yy % 4) == 0)) * 0.3)
    if coins:
        r = T.rng(seed + "coins")
        for _ in range(9):
            x, y = int(r.random() * 30), int(r.random() * 30)
            c = "#b87333" if r.random() < 0.6 else "#c0c4c8"
            img[y:y + 2, x:x + 2] = T.hexc(c) * (0.8 + r.random() * 0.3)
    return T.grain(img, seed, 0.03)


def fountain_light():
    img = mosaic("mfl", "#1e5a5c", "#2a6e70")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.ramp(np.clip(1 - d / 7, 0, 1), "#7ff0ff", "#e8ffff"), (d < 7).astype(float))
    return T.blend(img, T.solid("#b8964e"), ((d < 8) & (d >= 7)).astype(float))


def plastic():
    return T.grain(T.ramp(T.noise("mplas", sigma=2), "#d6d1c8", "#ebe6dd"), "mplas", 0.02)


def plastic_joint():
    img = plastic()
    img[15:17] = T.hexc("#9a958c")
    return img


def cloth(seed, a, b):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.ramp(T.noise(seed, sigma=1.0), a, b)
        img = T.shade(img, 1 - ((xx % 5) == 0) * 0.2)
        return T.grain(img, seed, 0.05)
    return f


def escalator_step():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mesc", sigma=1), "#3a3d40", "#4a4e52")
    img = T.shade(img, 1 - ((xx % 3) == 0) * 0.45)
    img[:3] = T.hexc("#c8a830")
    img[:3, ::4] = T.hexc("#3a3420")
    return T.grain(img, "mesc", 0.04)


def escalator_body():
    yy, xx = np.mgrid[0:S, 0:S]
    brushed = T.noise("mesb", shape=(S, S), sigma=0) * 0.5
    from scipy.ndimage import uniform_filter1d
    brushed = uniform_filter1d(brushed, 9, axis=1, mode="wrap")
    img = T.ramp(brushed * 2, "#7c8288", "#9aa0a6")
    img[:2] = T.hexc("#1a1a1c")
    return T.grain(img, "mesb", 0.02)


def service_door(top):
    img = T.ramp(T.noise("msd" + str(top), sigma=2), "#aea48c", "#bcb29a")
    img = T.border(img, "#5c564a", 2, sides="we" + ("n" if top else "s"))
    if top:
        img = T.rect(img, 9, 6, 23, 18, "#2a2e30")
        win = T.ramp(T.noise("msdw", sigma=1, shape=(10, 12)), "#1a2228", "#26323a")
        img[7:17, 10:22] = win
        img[7:17, 10:22:3] = T.hexc("#4a5054")
        lab = T.rect(img, 6, 21, 26, 29, "#e8e2d2")
        m = K.centered("STAFF", S, S, 22, font=K.F35)
        img = T.blend(lab, T.solid("#b02020"), m)
    else:
        img = T.rect(img, 4, 2, 28, 6, "#7e7a72")
        img = T.rect(img, 4, 2, 28, 3, "#a8a49a")
        img = K.grime_bottom(img, "msdg", 10, 0.35)
    return T.grain(img, "msdgr" + str(top), 0.04)


def directory_map():
    img = T.solid("#1a2a30")
    r = T.rng("mdir")
    cols = ["#2f7c78", "#d0806e", "#b77cff", "#e0b030", "#8ad4ff"]
    for gx in range(4):
        for gy in range(4):
            if r.random() < 0.85:
                x0, y0 = 4 + gx * 6, 8 + gy * 5
                img = T.rect(img, x0, y0, x0 + 5, y0 + 4, cols[int(r.random() * len(cols))])
    img = T.rect(img, 0, 0, S, 8, "#1a2a30")
    m = K.centered("HERE", S, S, 2, font=K.F35)
    img = T.blend(img, T.solid("#f0ece0"), m)
    # you are here. and here. and here.
    for _ in range(7):
        x, y = 4 + int(r.random() * 23), 9 + int(r.random() * 19)
        img = T.rect(img, x, y, x + 2, y + 2, "#ff2a2a")
    return T.border(img, "#b8964e", 2)


def wet_floor():
    img = T.solid("#e8c020")
    m = K.centered("CAUTION", S, S, 6, font=K.F35)
    img = T.blend(img, T.solid("#1a1a1a"), m)
    yy, xx = np.mgrid[0:S, 0:S]
    fig = K.disc(S, S, 16, 16, 2.2) | ((np.abs(xx - 16) < 1) & (yy > 17) & (yy < 24))
    fig = fig | (K.line_mask(S, S, 16, 24, 11, 28, 1.6) > 0) | (K.line_mask(S, S, 16, 20, 21, 25, 1.4) > 0)
    img = T.blend(img, T.solid("#1a1a1a"), fig.astype(float))
    return T.grain(img, "mwf", 0.04)


def pedestal():
    return T.grain(T.ramp(T.noise("mped", sigma=1.5), "#2a2a2e", "#3c3c42"), "mped", 0.03)


def dark_void():
    return T.grain(T.solid("#010102"), "mvoid", 0.01)


tex("mall_terrazzo", floor_main)
tex("mall_terrazzo_teal", floor_teal)
tex("mall_terrazzo_salmon", floor_salmon)
tex("mall_wall", wall)
tex("mall_wall_base", wall_base)
tex("mall_bulkhead", bulkhead)
tex("mall_fascia", fascia)
tex("mall_ceiling", ceiling)
tex("mall_ceiling_stained", ceiling_stained)
tex("mall_canlight", lambda: canlight(True))
tex("mall_canlight_off", lambda: canlight(False))
tex("mall_skylight", skylight)
tex("mall_shutter", lambda: shutter())
tex("mall_shutter_tagged", lambda: shutter(tag=True))
tex("mall_shutter_bottom", lambda: shutter(bottom=True))
tex("mall_grille", grille)
tex("mall_glass", glass)
tex("mall_store_carpet", store_carpet)
tex("mall_store_tile", store_tile)
tex("mall_store_wall", store_wall)
tex("mall_store_ceiling", store_ceiling)
tex("mall_store_light", store_light)
tex("mall_planter_side", planter_side)
tex("mall_planter_top", planter_top)
tex("mall_palm_fronds", palm_fronds)
tex("mall_palm_trunk", palm_trunk)
tex("mall_wood", wood_slat)
tex("mall_brass", brass)
tex("mall_dark_metal", dark_metal)
tex("mall_mosaic_teal", lambda: mosaic("mmt", "#1e5a5c", "#2e7476"))
tex("mall_mosaic_coins", lambda: mosaic("mmc", "#1e5a5c", "#2e7476", coins=True))
tex("mall_fountain_light", fountain_light)
tex("mall_plastic", plastic)
tex("mall_plastic_joint", plastic_joint)
tex("mall_cloth_a", cloth("mca", "#6a2a3a", "#7e3446"))
tex("mall_cloth_b", cloth("mcb", "#2a4a6a", "#34587c"))
tex("mall_cloth_c", cloth("mcc", "#c8b890", "#d8c8a0"))
tex("mall_escalator_step", escalator_step)
tex("mall_escalator_body", escalator_body)
tex("mall_service_door_top", lambda: service_door(True))
tex("mall_service_door_bottom", lambda: service_door(False))
tex("mall_directory_map", directory_map)
tex("mall_wet_floor", wet_floor)
tex("mall_pedestal", pedestal)
tex("mall_void", dark_void)

for n, (big, small, col) in enumerate(SIGNS):
    for part, t in enumerate(K.tiles(sign_image(big, small, col, True))):
        tex(f"mall_sign_{n}_{part}", (lambda t=t: t))
for part, t in enumerate(K.tiles(sign_image("OPEN", "CLOSED", "#c8c0c8", False))):
    tex(f"mall_sign_dead_{part}", (lambda t=t: t))

# ------------------------------------------------------------------ blocks

cube("mall_terrazzo", "mall_terrazzo", display="Terrazzo Floor", sound="stone")
cube("mall_terrazzo_teal", "mall_terrazzo_teal", display="Teal Terrazzo Inlay", sound="stone")
cube("mall_terrazzo_salmon", "mall_terrazzo_salmon", display="Salmon Terrazzo Inlay", sound="stone")
cube("mall_wall", "mall_wall", display="Mall Stucco", sound="stone")
cube("mall_wall_base", "mall_wall_base", display="Mall Stucco Baseboard", sound="stone")
cube("mall_bulkhead", "mall_bulkhead", display="Storefront Bulkhead", sound="stone")
cube("mall_fascia", "mall_fascia", display="Painted Steel Fascia", sound="metal")
cube("mall_ceiling", "mall_ceiling", display="Mall Ceiling", sound="wool", flat=True)
cube("mall_ceiling_stained", "mall_ceiling_stained", display="Stained Mall Ceiling", sound="wool", flat=True)
flicker_light("mall_canlight", "mall_canlight", "mall_canlight_off", "mall_ceiling", light=13, flicker=0.0,
              display="Recessed Downlight")
flicker_light("mall_canlight_flicker", "mall_canlight", "mall_canlight_off", "mall_ceiling", light=13, flicker=0.5,
              display="Failing Downlight")
cube_faces("mall_canlight_dead", bottom="mall_canlight_off", top="mall_ceiling", side="mall_ceiling",
           display="Dead Downlight", sound="glass", flat=True)
cube("mall_skylight", "mall_skylight", display="Skylight", sound="glass", flat=True, light=6, emissive=True)
cube("mall_shutter", "mall_shutter", display="Security Shutter", sound="metal")
cube("mall_shutter_tagged", "mall_shutter_tagged", display="Tagged Security Shutter", sound="metal")
cube("mall_shutter_bottom", "mall_shutter_bottom", display="Security Shutter Rail", sound="metal")
cube("mall_grille", "mall_grille", display="Rolling Grille", sound="chain", render="cutout")
pane("mall_glass", "mall_glass", display="Storefront Glass", sound="glass", render="translucent")
cube("mall_store_carpet", "mall_store_carpet", display="Store Carpet", sound="wool")
cube("mall_store_tile", "mall_store_tile", display="Checker Tile", sound="stone")
cube("mall_store_wall", "mall_store_wall", display="Slatwall", sound="wood")
cube("mall_store_ceiling", "mall_store_ceiling", display="Store Ceiling", sound="wool", flat=True)
cube_faces("mall_store_light", bottom="mall_store_light", top="mall_store_ceiling", side="mall_store_ceiling",
           display="Store Light", sound="glass", flat=True, light=14, emissive=True)
cube_faces("mall_planter", top="mall_planter_top", bottom="mall_planter_side", side="mall_planter_side",
           display="Mall Planter", sound="stone")
cross("mall_palm_trunk", "mall_palm_trunk", display="Plastic Palm Trunk", sound="wood")
cross("mall_palm_fronds", "mall_palm_fronds", display="Plastic Palm Fronds", sound="grass")

for n in range(len(SIGNS)):
    for part in range(3):
        cube(f"mall_sign_{n}_{part}", f"mall_sign_{n}_{part}", display="Store Sign", sound="glass",
             light=7, emissive=True)
for part in range(3):
    cube(f"mall_sign_dead_{part}", f"mall_sign_dead_{part}", display="Dead Store Sign", sound="glass")

shaped("mall_bench", [
    (1, 0, 4, 3, 6, 12, "metal"), (13, 0, 4, 15, 6, 12, "metal"),
    (0, 6, 3, 16, 8, 13, "wood"),
], {"wood": "mall_wood", "metal": "mall_dark_metal"}, display="Mall Bench", unbreakable=True)
shaped("mall_trash_can", [
    (4, 0, 4, 12, 13, 12, "wood"), (3.5, 13, 3.5, 12.5, 15, 12.5, "brass"), (6, 15, 6, 10, 16, 10, "metal"),
], {"wood": "mall_wood", "brass": "mall_brass", "metal": "mall_dark_metal"}, display="Mall Trash Can",
    unbreakable=True)
shaped("mall_wet_floor_sign", [
    (4, 0, 6, 12, 14, 7, "sign"), (4, 0, 9, 12, 14, 10, "sign"), (7, 14, 6, 9, 15, 10, "sign"),
], {"sign": "mall_wet_floor"}, display="Wet Floor Sign", collides=False)
shaped("mall_fountain_rim", [
    (0, 0, 0, 16, 9, 16, {"all": "tile", "up": "top"}),
], {"tile": "mall_mosaic_teal", "top": "mall_terrazzo"}, display="Fountain Rim", unbreakable=True)
shaped("mall_fountain_spout", [
    (6, 0, 6, 10, 12, 10, "stone"), (2, 12, 2, 14, 14, 14, {"all": "brass", "up": "water"}),
    (7, 14, 7, 9, 16, 9, "brass"),
], {"stone": "mall_terrazzo", "brass": "mall_brass", "water": "mall_mosaic_teal"}, display="Fountain Spout",
    unbreakable=True)
cube("mall_mosaic_teal", "mall_mosaic_teal", display="Fountain Mosaic", sound="stone")
cube("mall_mosaic_coins", "mall_mosaic_coins", display="Coin-Strewn Mosaic", sound="stone")
cube("mall_fountain_light", "mall_fountain_light", display="Fountain Light", sound="glass", light=10, emissive=True)

# a display mannequin in two halves; the behaviour turns them into the real thing
shaped("mall_mannequin_display", [
    (4, 0, 4, 12, 1, 12, "base"),
    (5, 1, 7, 7.5, 13, 9, "joint"), (8.5, 1, 7, 11, 13, 9, "joint"),
    (4.5, 13, 6, 11.5, 16, 10, "plastic"),
    (2.5, 9, 7, 4.5, 16, 9, "plastic"), (11.5, 9, 7, 13.5, 16, 9, "plastic"),
], {"base": "mall_pedestal", "plastic": "mall_plastic", "joint": "mall_plastic_joint"},
    display="Display Mannequin", unbreakable=True)
shaped("mall_mannequin_upper", [
    (4.5, 0, 6, 11.5, 9, 10, "plastic"),
    (2.5, 0, 7, 4.5, 9, 9, "plastic"), (11.5, 0, 7, 13.5, 9, 9, "plastic"),
    (7, 9, 7, 9, 10, 9, "plastic"), (5.5, 10, 5.5, 10.5, 16, 10.5, "plastic"),
], {"plastic": "mall_plastic"}, display="Display Mannequin", unbreakable=True)
shaped("mall_pedestal", [(4, 0, 4, 12, 1, 12, "base"), (7.5, 1, 7.5, 8.5, 6, 8.5, "base")],
       {"base": "mall_pedestal"}, display="Empty Display Stand", unbreakable=True, collides=False)
shaped("mall_clothing_rack", [
    (2, 0, 7, 14, 1, 9, "metal"), (2, 1, 7.5, 3, 14, 8.5, "metal"), (13, 1, 7.5, 14, 14, 8.5, "metal"),
    (2, 13, 7.5, 14, 14, 8.5, "metal"),
    (3, 4, 5, 6, 13, 11, "a"), (6.5, 3, 5, 9.5, 13, 11, "b"), (10, 5, 5, 13, 13, 11, "c"),
], {"metal": "mall_dark_metal", "a": "mall_cloth_a", "b": "mall_cloth_b", "c": "mall_cloth_c"},
    display="Clothing Rack", unbreakable=True)
shaped("mall_directory_post", [(6.5, 0, 6.5, 9.5, 16, 9.5, "metal"), (4, 0, 4, 12, 1, 12, "metal")],
       {"metal": "mall_dark_metal"}, display="Directory Post", unbreakable=True)
shaped("mall_directory", [(1, 0, 6, 15, 16, 10, {"all": "metal", "north": "map", "south": "map"})],
       {"metal": "mall_dark_metal", "map": "mall_directory_map"}, display="Mall Directory", light=6,
       emissive=True, unbreakable=True)

cube("mall_escalator_body", "mall_escalator_body", display="Escalator Housing", sound="metal")
stairs("mall_escalator", "mall_escalator_step", "mall_escalator_body", display="Stopped Escalator", sound="metal")
exit_block("mall_escalator_dark_up", "mall_void", "ASCEND", display="The Upper Level")
exit_block("mall_escalator_dark_down", "mall_void", "DESCEND", display="The Lower Level")
exit_block("mall_service_door", "mall_service_door_bottom", "DOOR", trigger="USE", solid=True,
           display="Service Corridor Door", sound="metal",
           model={"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/mall_service_door_bottom"}})
cube("mall_service_door_top", "mall_service_door_top", display="Service Corridor Door", sound="metal")

# ------------------------------------------------------------------ sound


def muzak():
    """A smooth-jazz loop, played on a tape that has been stretched too many times."""
    bpm = 92.0
    beat = 60.0 / bpm
    bars = 8
    d0 = bars * 4 * beat
    buf = np.zeros(int(d0 * Y.SR))
    prog = [["D#3", "G3", "A#3", "D4"], ["C3", "D#3", "G3", "A#3"], ["F3", "G#3", "C4", "D#4"],
            ["A#2", "D3", "G#3", "C4"]]
    bass = ["D#2", "C2", "F2", "A#1"]
    for bar in range(bars):
        ch = prog[(bar // 2) % 4]
        t0 = bar * 4 * beat
        # comping: on 1 and the "and" of 2
        for off, vel in ((0.0, 0.8), (1.5, 0.55), (3.0, 0.4)):
            for k, n in enumerate(ch):
                K.circular_place(buf, K.epiano(K.hz(n), 2.6, vel), t0 + off * beat + k * 0.012, 0.22)
        b = K.hz(bass[(bar // 2) % 4])
        for off in (0, 2.5):
            tt = Y.t_axis(beat * 1.4)
            note = np.sin(2 * np.pi * b * tt) * np.exp(-tt * 2.2) * (1 - np.exp(-tt * 200))
            K.circular_place(buf, note, t0 + off * beat, 0.5)
    melody = ["G4", "A#4", "C5", "D5", "C5", "A#4", "G4", "F4", "D#4", "F4", "G4", "A#4", "G4", "F4", "D#4", "C4"]
    for k, n in enumerate(melody):
        tt = Y.t_axis(beat * 1.9)
        f = K.hz(n)
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * tt)
        ph = 2 * np.pi * np.cumsum(f * vib) / Y.SR
        note = (np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.12 * np.sin(3 * ph)) * np.minimum(tt * 8, 1) * np.exp(-tt * 0.9)
        K.circular_place(buf, note, k * 2 * beat + beat * 0.5, 0.16)
    # the stretch: slower and lower, warbling
    x = K.resample(buf, 1.28)
    x = K.wow(x, 0.0025, 0.35, "mallwow")
    x = Y.lp(x, 2600)
    x = Y.hp(x, 160)
    x = K.loop_reverb(x, 3.5, 0.55, "mallrev", damp=3000)
    d = len(x) / Y.SR
    fountain = Y.bp(Y.white(d, "mfount"), 900, 5000) * 0.05 + Y.lp(Y.brown(d, "mhvac"), 160) * 0.12
    x = Y.norm(x, 0.42) + fountain
    x = Y.loopable(Y.norm(x, 0.5), 0.5)
    return Y.stereoize(x, 0.0016), "stereo"


def heels(i):
    def f():
        out = np.zeros(int(7 * Y.SR))
        n = 9 + i * 2
        for k in range(n):
            g = np.sin(np.pi * (k + 1) / (n + 1))
            Y.place(out, Y.footstep(f"mh{i}{k}", "tile"), 0.2 + k * (0.42 + i * 0.04), g * 0.8)
        return Y.reverb(Y.lp(out, 2500), 3.5, 0.7, f"mh{i}")
    return f


def shutter_rattle():
    d = 3.5
    t = Y.t_axis(d)
    rate = 26 - 10 * t / d
    ph = np.cumsum(rate / Y.SR)
    clicks = (np.diff(np.floor(ph), prepend=0) > 0).astype(float)
    x = Y.bp(clicks, 400, 4000) * 3 + Y.bp(Y.white(d, "msr"), 200, 1500) * 0.15
    x *= np.sin(np.pi * t / d) ** 0.5
    end = np.zeros_like(t)
    Y.place(end, Y.impact("msrend", 0.7, ring=0.3), d - 0.4)
    return Y.norm(Y.reverb(Y.norm(x) + end * 0.8, 3.0, 0.65, "msr"), 0.8)


def pa_chime():
    x = K.chime([K.hz("E5"), K.hz("C5")], gap=0.6, decay=2.0, seed="mpa")
    return Y.norm(Y.reverb(x, 3.0, 0.55, "mpa"), 0.7)


def cart_squeak():
    d = 4.0
    t = Y.t_axis(d)
    rot = (np.sin(2 * np.pi * 2.1 * t) > 0.85).astype(float)
    sq = np.sin(2 * np.pi * (1900 + 300 * np.sin(2 * np.pi * 2.1 * t)) * t) * Y.lp(rot, 40)
    roll = Y.bp(Y.white(d, "mcart"), 80, 600) * 0.6
    x = (sq * 0.5 + roll) * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(x, 3.0, 0.6, "mcart"), 0.6)


def closing():
    c = K.chime([K.hz("G5"), K.hz("E5"), K.hz("C5")], gap=0.55, decay=1.8, seed="mclose")
    v = K.pa_voice("mallclose", syllables=22, f0=190, rate=0.16, female=False)
    out = np.zeros(len(c) + len(v) + int(0.5 * Y.SR))
    Y.place(out, c, 0.0, 0.8)
    Y.place(out, v, 1.9, 0.75)
    return Y.norm(Y.reverb(out, 3.5, 0.55, "mclosev"), 0.85)


def plastic_step():
    out = np.zeros(int(1.6 * Y.SR))
    for k, at in enumerate((0.0, 0.32)):
        n = int(0.2 * Y.SR)
        tt = np.arange(n) / Y.SR
        knock = Y.bp(Y.white(0.2, f"mps{k}"), 600, 3000)[:n] * np.exp(-tt * 60)
        knock += np.sin(2 * np.pi * (310 - k * 40) * tt) * np.exp(-tt * 35) * 0.7
        Y.place(out, knock, at, 1.0 - k * 0.2)
    return Y.norm(Y.reverb(out, 1.5, 0.35, "mps"), 0.8)


sound("ambient.dead_mall", [("ambient/dead_mall", muzak)], stream=True)
sound("ambient.dead_mall.additions",
      [(f"ambient/dead_mall_heels{i}", heels(i)) for i in range(2)] +
      [("ambient/dead_mall_shutter", shutter_rattle), ("ambient/dead_mall_chime", pa_chime),
       ("ambient/dead_mall_cart", cart_squeak)],
      subtitle="The mall settles")
sound("event.dead_mall_closing", [("event/dead_mall_closing", closing)], subtitle="Closing announcement")
sound("event.dead_mall_step", [("event/dead_mall_step", plastic_step)], subtitle="Plastic feet on a pedestal")

level("dead_mall", fog="#2e2836", sky="#2e2836", water="#2f7f86", water_fog="#0f2a2e", ambient="ambient.dead_mall",
      additions="ambient.dead_mall.additions", additions_chance=0.005, fixed_time=18000, height=48)
