"""Overgrown Lobby - Level 0, years later, reclaimed by moss, roots and light through a broken roof."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, cross, ladder, pillar
import tex as T
import synth as Y
from content.deepend_audio import wrapverb, fit
from content.level_0 import wallpaper_base_pattern, carpet as l0_carpet, ceiling as l0_ceiling

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def moss_field(seed, shape=(S, S)):
    return T.ramp(T.fbm(seed, octaves=4, base=3, shape=shape) * 0.6 + T.noise(seed + "n", sigma=0.5, shape=shape) * 0.4,
                  "#2e4a1a", "#3e6222", "#567a2c")


def mossy_wall(level_):
    """Yellow wallpaper with moss climbing it. level_ 0..2: how far the moss has got."""
    def f():
        img = wallpaper_base_pattern(f"ogw{level_}")
        img = T.stains(img, f"ogws{level_}", "#5a6a2a", count=2, strength=0.35, size=9)
        edge = np.array([18, 6, -40][level_]) + T.noise(f"ogwe{level_}", sigma=1.5) * 12
        m = (yy > edge).astype(float)
        speck = (T.noise(f"ogwsp{level_}", sigma=0.6) > 0.72).astype(float) * (yy > edge - 6)
        return T.blend(img, moss_field(f"ogwm{level_}"), np.clip(m + speck * 0.8, 0, 1))
    return f


def ivy_wall():
    img = wallpaper_base_pattern("ogivy")
    r = T.rng("ogivy")
    leaves = np.zeros((S, S))
    for k in range(5):
        x = r.random() * 32
        for y in range(S):
            x += (r.random() - 0.5) * 1.6
            xi = int(x) % S
            leaves[y, xi] = 1
            if r.random() < 0.35:
                leaves[max(0, y - 1):y + 2, max(0, xi - 1):xi + 2] = 1
    return T.blend(img, moss_field("ogivyl"), leaves * 0.95)


def moss_carpet():
    img = l0_carpet()
    m = T.noise("ogmc", sigma=2) > 0.35
    return T.blend(img, moss_field("ogmcm"), m.astype(float))


def moss_floor():
    return T.grain(moss_field("ogmf"), "ogmf", 0.05)


def roots():
    img = moss_carpet()
    r = T.rng("ogroots")
    for k in range(4):
        x, y = r.random() * 32, r.random() * 32
        ang = r.random() * 6.28
        for i in range(40):
            for w in range(2):
                img[int(y) % S, (int(x) + w) % S] = T.hexc("#4a3420") * (0.85 + 0.3 * r.random())
            ang += (r.random() - 0.5) * 0.5
            x += np.cos(ang)
            y += np.sin(ang)
    return img


def ceiling_broken():
    img = l0_ceiling()
    img = T.stains(img, "ogcb", "#4a5a2a", count=2, strength=0.5, size=10)
    return img


def roof_moss():
    img = moss_field("ogroof")
    return T.grain(T.shade(img, 0.9 + T.noise("ogroofs", sigma=1) * 0.2), "ogroof", 0.04)


def fern():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for k, ang in enumerate(np.linspace(-1.2, 1.2, 5)):
        x, y = 16.0, 31.0
        for i in range(26):
            xi, yi = int(x), int(y)
            if 0 <= xi < S and 0 <= yi < S:
                rgb[yi, xi] = T.hexc("#3e7a2a")
                a[yi, xi] = 1
                for s in (-1, 1):
                    lx = xi + s * (3 - i // 9)
                    if 0 <= lx < S and i % 2 == 0 and i > 3:
                        rgb[yi, min(xi, lx):max(xi, lx) + 1] = T.hexc("#4e9232")
                        a[yi, min(xi, lx):max(xi, lx) + 1] = 1
            x += np.sin(ang) * 0.7
            y -= np.cos(ang) * 1.0
    return rgb, a


def hanging_vines():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("oghv")
    for k in range(6):
        x = 3 + k * 5 + int(r.random() * 2)
        ln = 12 + int(r.random() * 20)
        for y in range(ln):
            xi = x + int(np.sin(y / 4.0 + k))
            rgb[y, xi % S] = T.hexc("#3a6a22")
            a[y, xi % S] = 1
            if y % 4 == 2:
                rgb[y, (xi + 1) % S] = T.hexc("#56922e")
                a[y, (xi + 1) % S] = 1
    return rgb, a


def flower(c):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        rgb[14:, 15] = T.hexc("#3e7a2a")
        a[14:, 15] = 1
        d = np.sqrt((xx - 15.5) ** 2 + (yy - 11) ** 2)
        rgb[d < 4] = T.hexc(c)
        a[d < 4] = 1
        rgb[d < 1.5] = T.hexc("#f0d040")
        return rgb, a
    return f


def climbing_vine():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for x0 in (6, 16, 25):
        for y in range(S):
            x = x0 + int(2 * np.sin(y / 5.0 + x0))
            rgb[y, x:x + 2] = T.hexc("#3a6a22")
            a[y, x:x + 2] = 1
            if y % 5 == 0:
                rgb[y:y + 3, x - 2:x + 4] = T.hexc("#56922e")
                a[y:y + 3, x - 2:x + 4] = 1
    return rgb, a


def bark():
    img = T.ramp(T.fbm("ogbark", octaves=3, base=2), "#3a2a1c", "#4e3a26")
    img = T.shade(img, 1 - ((xx % 7) < 1) * 0.3)
    return T.grain(T.stains(img, "ogbm", "#3e6222", count=2, strength=0.5, size=6), "ogbark", 0.04)


def leaves():
    rgb = T.ramp(T.fbm("oglf", octaves=3, base=2) * 0.6 + T.noise("oglfn", sigma=0.5) * 0.4, "#2a5a1a", "#3e7a26", "#5a9a34")
    a = (T.noise("oglfa", sigma=0.6) > 0.2).astype(float)
    return rgb, a


def sunlight():
    """The light at the top of the tree. Warm, real, and far too bright after all that yellow."""
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.ramp(np.clip(1 - d / 24, 0, 1), "#f8e8a0", "#fff8d8", "#ffffff")


def mushroom():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for (cx, cy, r_) in ((10, 22, 5), (21, 18, 6), (15, 26, 3)):
        stem = (np.abs(xx - cx) < 1.5) & (yy > cy) & (yy < S)
        cap = (((xx - cx) / r_) ** 2 + ((yy - cy) / (r_ * 0.6)) ** 2 < 1) & (yy <= cy)
        rgb[stem] = T.hexc("#e8e0c8")
        a[stem] = 1
        rgb[cap] = T.hexc("#c8e870")
        a[cap] = 1
    return rgb, a


for k in range(3):
    tex(f"og_wall_moss{k}", mossy_wall(k))
tex("og_wall_ivy", ivy_wall)
tex("og_moss_carpet", moss_carpet)
tex("og_moss_floor", moss_floor)
tex("og_roots", roots)
tex("og_ceiling", ceiling_broken)
tex("og_roof", roof_moss)
tex("og_fern", fern)
tex("og_hanging_vines", hanging_vines)
tex("og_flower_white", flower("#f4f2ec"))
tex("og_flower_purple", flower("#a070d0"))
tex("og_climbing_vine", climbing_vine)
tex("og_bark", bark)
tex("og_leaves", leaves)
tex("og_sunlight", sunlight)
tex("og_mushroom", mushroom)

cube("overgrown_wallpaper", "og_wall_moss0", display="Mossy Yellow Wallpaper", sound="wool")
cube("overgrown_wallpaper_mossier", "og_wall_moss1", display="Mossy Yellow Wallpaper", sound="moss")
cube("overgrown_wallpaper_lost", "og_wall_moss2", display="Moss-Covered Wall", sound="moss")
cube("overgrown_wallpaper_ivy", "og_wall_ivy", display="Ivy-Covered Wallpaper", sound="moss")
cube("overgrown_carpet", "og_moss_carpet", display="Mossy Carpet", sound="moss")
cube("overgrown_moss", "og_moss_floor", display="Lobby Moss", sound="moss")
cube("overgrown_roots", "og_roots", display="Roots Through Carpet", sound="moss")
cube("overgrown_ceiling_tile", "og_ceiling", display="Mouldering Ceiling Tile", sound="wool", flat=True)
cube_faces("overgrown_roof", top="og_roof", bottom="og_ceiling", side="og_roof", display="Overgrown Roof", sound="moss")
cross("overgrown_fern", "og_fern", display="Fern")
cross("overgrown_hanging_vines", "og_hanging_vines", display="Hanging Vines")
cross("overgrown_flower_white", "og_flower_white", display="Wildflower")
cross("overgrown_flower_purple", "og_flower_purple", display="Wildflower")
cross("overgrown_mushrooms", "og_mushroom", display="Pale Mushrooms", light=4)
ladder("overgrown_climbing_vine", "og_climbing_vine", display="Climbing Vine")
pillar("overgrown_trunk", "og_bark", "og_bark", display="Lobby Tree", sound="wood")
cube("overgrown_leaves", "og_leaves", display="Lobby Tree Leaves", sound="grass", render="cutout")
exit_block("overgrown_sunlight", "og_sunlight", "ASCEND", display="Sunlight", light=15, emissive=True)


def forest_lobby():
    """Birds and insects in a room built for fluorescent hum. The hum is still there, underneath."""
    d = 26.0
    t = Y.t_axis(d)
    n = len(t)
    hum = Y.lp(Y.hum(d, 60.0, "oghum", buzz=0.25), 1200) * 0.08
    insects = Y.bp(Y.white(d, "ogins"), 4000, 7000) * 0.05 * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t) ** 2) \
        * (0.5 + 0.5 * np.sin(2 * np.pi * t / 13.0) ** 2)
    birds = np.zeros(n)
    r = Y.rng("ogbirds")
    for k in range(14):
        at = r.random() * (d - 1)
        f0 = 2400 + r.random() * 1800
        for j in range(2 + int(r.random() * 3)):
            nn = int(0.08 * Y.SR)
            tt = np.arange(nn) / Y.SR
            ch = np.sin(2 * np.pi * np.cumsum(f0 + 800 * np.sin(np.pi * tt / 0.08)) / Y.SR) * np.sin(np.pi * tt / 0.08) ** 2
            Y.place(birds, ch, at + j * 0.12, 0.1 + r.random() * 0.08)
    drips = np.zeros(n)
    for k in range(10):
        nn = int(0.1 * Y.SR)
        tt = np.arange(nn) / Y.SR
        pl = np.sin(2 * np.pi * np.cumsum(1500 - tt * 4000) / Y.SR) * np.exp(-tt * 40)
        Y.place(drips, pl, 1.0 + k * 2.4 + r.random(), 0.15)
    x = wrapverb(birds + drips, 2.0, 0.5, "ogroom", damp=4000) + insects + hum
    return Y.stereoize(Y.loopable(Y.norm(x, 0.42), 1.5), 0.003), "stereo"


def branch_crack():
    out = np.zeros(int(2.5 * Y.SR))
    for k in range(3):
        nn = int(0.06 * Y.SR)
        c = Y.bp(Y.white(0.06, f"ogcr{k}"), 800, 6000)[:nn] * np.exp(-np.arange(nn) / Y.SR * 70)
        Y.place(out, c, 0.1 + k * 0.07, 0.8)
    return Y.reverb(out, 2.0, 0.5, "ogcrack")


def rustle():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "ogrus"), 1500, 7000) * (0.5 + 0.5 * np.sin(2 * np.pi * 6 * t) ** 2) * np.sin(np.pi * t / d) ** 2
    return Y.reverb(Y.norm(x, 0.5), 1.5, 0.4, "ogrus")


def creak():
    d = 2.5
    t = Y.t_axis(d)
    f0 = 90 + 40 * np.sin(2 * np.pi * 0.5 * t)
    x = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)), 150, 1200) * np.sin(np.pi * t / d) ** 2
    return Y.reverb(Y.norm(x, 0.5), 2.5, 0.6, "ogcreak")


def roots_grab():
    d = 1.2
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "ogroot"), 300, 2500) * np.exp(-t * 3) + Y.lp(Y.brown(d, "ogroot2"), 200) * np.exp(-t * 4)
    return Y.norm(x, 0.8)


sound("ambient.overgrown_lobby", [("ambient/overgrown_lobby", forest_lobby)], stream=True)
sound("ambient.overgrown_lobby.additions", [("ambient/overgrown_lobby_crack", branch_crack),
                                           ("ambient/overgrown_lobby_rustle", rustle),
                                           ("ambient/overgrown_lobby_creak", creak)], subtitle="Something in the undergrowth")
sound("event.overgrown_lobby_roots", [("event/overgrown_lobby_roots", roots_grab)], subtitle="Roots tighten")

level("overgrown_lobby", fog="#a8b07a", sky="#9cc0d8", ambient="ambient.overgrown_lobby",
      additions="ambient.overgrown_lobby.additions", additions_chance=0.005, fixed_time=6000, skylight=True,
      ceiling=False, height=64, particle="minecraft:spore_blossom_air", particle_chance=0.004)
