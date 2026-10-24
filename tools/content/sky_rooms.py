"""Sky Rooms. Pieces of the Lobby torn loose and left floating above the clouds, joined
by strips of corridor floor one block wide. The wind comes in gusts. Falling is a way
out, but not a good one."""
import numpy as np
from assets import cube, cube_faces, shaped, level, sound
import assets as A
import tex as T
import synth as Y
from content.level_0 import carpet

S = 32
tx = A.tex


def torn():
    img = T.ramp(T.fbm("srtorn", octaves=4, base=3), "#3a3833", "#4a4741", "#5a564e")
    img = T.cracks(img, "srtc", "#1e1d1a", count=3, alpha=0.7)
    # rebar ends sticking out of the broken slab
    r = T.rng("srrebar")
    for _ in range(4):
        x, y = int(r.random() * 30) + 1, int(r.random() * 30) + 1
        img = T.rect(img, x, y, x + 2, y + 2, "#6a3a22")
        img = T.rect(img, x, y, x + 1, y + 1, "#8a5a32")
    return T.grain(img, "srtorn", 0.06)


def slab_top():
    img = T.ramp(T.fbm("srslab", octaves=3, base=4), "#4a4842", "#58554e")
    img = T.stains(img, "srslabs", "#2e2c28", count=2, strength=0.4, size=8)
    return T.grain(img, "srslab", 0.04)


def crumble():
    img = carpet()
    img = T.cracks(img, "srcr1", "#1a160c", count=3, alpha=0.85)
    img = T.cracks(img, "srcr2", "#2a2412", count=2, alpha=0.6)
    holes = (T.noise("srcrh", sigma=1.2) > 0.86).astype(float)
    return T.blend(img, T.solid("#9ec4e8"), holes * 0.9)


tx("sr_torn", torn)
tx("sr_slab_top", slab_top)
tx("sr_crumble", crumble)
cube_faces("sr_torn_concrete", top="sr_slab_top", bottom="sr_torn", side="sr_torn", display="Torn Concrete")
cube_faces("sr_upper_slab", top="sr_slab_top", bottom="sr_slab_top", side="sr_torn", display="Broken Floor Slab")
cube_faces("sr_crumbling_carpet", top="sr_crumble", bottom="sr_torn", side="sr_torn", sound="wool",
           display="Crumbling Carpet")


def wire():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for k, col in enumerate(("#5a1a12", "#1a3a5a", "#1a1a1a")):
        for y in range(S):
            x = int(10 + k * 5 + 2 * np.sin(y / 5 + k))
            rgb[y, x] = T.hexc(col)
            a[y, x] = 1
    return rgb, a


tx("sr_wires", wire)
from assets import cross
cross("sr_dangling_wires", "sr_wires", display="Dangling Wires")

# ------------------------------------------------------------------ sound


def altitude_wind():
    d = 28.0
    t = Y.t_axis(d)
    gust = 0.6 + 0.25 * np.sin(2 * np.pi * t / 14) + 0.15 * np.sin(2 * np.pi * t / 7 + 0.8)
    wind = Y.bp(Y.pink(d, "srwind"), 150, 1800) * gust
    howl = np.zeros_like(t)
    for f, ph in ((340.0, 0.0), (510.0, 1.7)):
        ff = round(f * d) / d
        howl += np.sin(2 * np.pi * ff * t + 3 * np.sin(2 * np.pi * t / 7 + ph)) * 0.025 * gust ** 2
    rumble = Y.lp(Y.brown(d, "srrum"), 60) * 0.45
    hum = Y.hum(d, 60.0, "srhum", buzz=0.2, harmonics=(1, 2, 3)) * 0.02
    x = Y.loopable(Y.norm(wind + Y.bp(howl, 200, 2000) + rumble + hum, 0.5), 1.5)
    return Y.stereoize(x, 0.0025), "stereo"


def airplane():
    d = 9.0
    t = Y.t_axis(d)
    env = np.sin(np.pi * t / d) ** 2
    f = 95 * (1 + 0.05 * np.tanh((4.5 - t) / 2))
    x = Y.lp(Y.brown(d, "srplane"), 400) * 0.6 + np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.2
    x += Y.bp(Y.white(d, "srjet"), 800, 3000) * 0.08
    return Y.reverb(Y.norm(x * env, 0.4), 3.0, 0.6, "srplane")


def falling_debris():
    out = np.zeros(int(6.0 * Y.SR))
    Y.place(out, Y.impact("srbreak", 0.8), 0.0, 0.8)
    r = Y.rng("srdeb")
    for k in range(16):
        g = 0.5 * (1 - k / 16)
        Y.place(out, Y.impact(f"srdb{k}", 0.15 + r.random() * 0.2), 0.3 + k * 0.25 + r.random() * 0.1, g)
    t = Y.t_axis(6.0)
    out = Y.lp(out, 2500) * np.exp(-t * 0.4)
    return Y.reverb(Y.norm(out, 0.5), 3.0, 0.7, "srdeb")


def tube_pop():
    out = np.zeros(int(2.0 * Y.SR))
    n = int(0.3 * Y.SR)
    tt = np.arange(n) / Y.SR
    pop = Y.hp(Y.white(0.3, "srpop"), 1500)[:n] * np.exp(-tt * 30)
    Y.place(out, pop, 0.05, 1.0)
    for k in range(10):
        Y.place(out, Y.hp(Y.white(0.03, f"srgl{k}"), 4000) * 0.3, 0.2 + k * 0.05)
    return Y.reverb(Y.norm(out, 0.5), 1.5, 0.4, "srpop")


def gust_warning():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.bp(Y.pink(d, "srgw"), 300, 3000) * (t / d) ** 2
    x += np.sin(2 * np.pi * np.cumsum(300 + 400 * (t / d)) / Y.SR) * 0.08 * (t / d) ** 2
    return Y.norm(x, 0.7)


def crack():
    out = np.zeros(int(1.5 * Y.SR))
    for k in range(5):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.05, f"srck{k}"), 800, 6000)[:n] * np.exp(-tt * 90), 0.05 + k * 0.07, 0.8 - k * 0.1)
    Y.place(out, Y.impact("srckthud", 0.4), 0.5, 0.6)
    return Y.reverb(Y.norm(out, 0.6), 1.0, 0.3, "srck")


sound("ambient.sky_rooms", [("ambient/sky_rooms", altitude_wind)], stream=True)
sound("ambient.sky_rooms.additions",
      [("ambient/sky_rooms_plane", airplane), ("ambient/sky_rooms_debris", falling_debris),
       ("ambient/sky_rooms_pop", tube_pop)],
      subtitle="Something falls away into the clouds")
sound("event.sky_rooms_gust", [("event/sky_rooms_gust", gust_warning)], subtitle="The wind rises")
sound("event.sky_rooms_crack", [("event/sky_rooms_crack", crack)], subtitle="The floor cracks")

level("sky_rooms", fog="#bcd6ee", sky="#7eaee0", ambient="ambient.sky_rooms",
      additions="ambient.sky_rooms.additions", additions_chance=0.003, fixed_time=4500, skylight=True,
      ceiling=False, height=256)
