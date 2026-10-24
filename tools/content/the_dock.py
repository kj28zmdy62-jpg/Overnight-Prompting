"""The Dock. A wooden dock across a black lake at night, running on into the mist and
repeating. Every boathouse along it is number 4. Count them. Do not fall in."""
import numpy as np
from assets import cube, cube_faces, shaped, exit_block, fence, stairs, level, sound
import assets as A
import tex as T
import synth as Y
from content.outside_kit import draw_text, centered, paint_metal

S = 32
tx = A.tex


def boards(seed, lo="#3e3628", hi="#54493a"):
    img = T.ramp(T.fbm(seed, octaves=3, base=2), lo, hi)
    xx, yy = np.mgrid[0:S, 0:S][1], np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.5)
    img = T.shade(img, 1 - (np.sin(yy * 0.7 + T.noise(seed + "g", sigma=2) * 5) > 0.85) * 0.12)
    wet = (T.noise(seed + "w", sigma=2) > 0.68).astype(float)
    img = T.shade(img, 1 - wet * 0.25)
    moss = (T.noise(seed + "m", sigma=1) > 0.84) & ((xx % 8) < 2)
    img[moss] = T.hexc("#2a3a22")
    return T.grain(img, seed, 0.05)


def pile():
    img = T.ramp(T.fbm("tdpile", octaves=3, base=2), "#2a241c", "#3a3226")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - (np.sin(np.mgrid[0:S, 0:S][1] * 1.4) > 0.6) * 0.15)
    slime = T.noise("tdslime", sigma=1.2) > 0.6
    img[slime] = T.hexc("#1e2a1a")
    return T.grain(img, "tdpile", 0.05)


tx("td_boards", lambda: boards("tdb"))
tx("td_boards_b", lambda: boards("tdb2", "#3a3226", "#4e4434"))
tx("td_pile", pile)
cube("td_boards", "td_boards", sound="wood", display="Dock Boards")
cube("td_boards_b", "td_boards_b", sound="wood", display="Dock Boards")
shaped("td_pile", [(5, 0, 5, 11, 16, 11, "p")], {"p": "td_pile"}, display="Dock Pile", unbreakable=True, sound="wood")
shaped("td_cleat", [(5, 0, 7, 11, 1, 9, "m"), (3, 1, 7, 13, 2, 9, "m")], {"m": "td_iron"}, display="Mooring Cleat",
       unbreakable=True, collides=False, sound="metal")
tx("td_iron", lambda: paint_metal("tdiron", "#2a2a28", "#3a3a36", rust=0.5))
tx("td_rail", lambda: boards("tdrail", "#4a4234", "#5a5040"))
fence("td_rail", "td_rail", display="Dock Railing", sound="wood")
tx("td_mud", lambda: T.grain(T.ramp(T.fbm("tdmud", octaves=3, base=3), "#141410", "#22201a"), "tdmud", 0.05))
cube("td_mud", "td_mud", sound="mud", display="Lake Bed")


def lantern_glass(on):
    def f():
        img = T.solid("#ffc870" if on else "#2a2620")
        if on:
            yy, xx = np.mgrid[0:S, 0:S]
            img = T.shade(img, 1.05 - np.sqrt((xx - 15.5) ** 2 + (yy - 18) ** 2) / 50)
        img[:, 15:17] = T.hexc("#1a1a18")
        return T.border(img, "#1a1a18", 3)
    return f


tx("td_lantern_on", lantern_glass(True))
tx("td_lantern_off", lantern_glass(False))
shaped("td_lamp_post", [(7, 0, 7, 9, 16, 9, "p")], {"p": "td_pile"}, display="Lantern Post", unbreakable=True)
shaped("td_lantern", [(7, 12, 7, 9, 16, 9, "i"), (5, 3, 5, 11, 12, 11, "g"), (4, 12, 4, 12, 13, 12, "i"), (6, 2, 6, 10, 3, 10, "i")],
       {"i": "td_iron", "g": "td_lantern_on"}, display="Dock Lantern", light=13, emissive=True, unbreakable=True,
       collides=False)
shaped("td_lantern_dead", [(7, 12, 7, 9, 16, 9, "i"), (5, 3, 5, 11, 12, 11, "g"), (4, 12, 4, 12, 13, 12, "i"), (6, 2, 6, 10, 3, 10, "i")],
       {"i": "td_iron", "g": "td_lantern_off"}, display="Dead Dock Lantern", unbreakable=True, collides=False)

# ------------------------------------------------------------------ boathouses


def siding():
    img = T.ramp(T.fbm("tdsid", octaves=3, base=2), "#2e3a3a", "#3a4646")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 - ((xx % 6) == 0) * 0.4)
    peel = T.noise("tdpeel", sigma=1) > 0.75
    img[peel] = T.hexc("#5a5446")
    return T.grain(img, "tdsid", 0.05)


def shingle():
    img = T.ramp(T.noise("tdsh", sigma=0.8), "#1e1c1a", "#2e2a26")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 6) == 0) * 0.35)
    moss = T.noise("tdshm", sigma=1.5) > 0.7
    img[moss] = T.hexc("#2a3622")
    return T.grain(img, "tdsh", 0.05)


def number_sign(n):
    def f():
        img = T.ramp(T.noise("tdns" + n, sigma=2), "#c8c0a8", "#d8d0b8")
        img = T.border(img, "#2a2a28", 2)
        img = centered(img, n, 8, "#1a1a18", scale=3, gap=2)
        img = T.stains(img, "tdnss" + n, "#6a5a40", count=2, strength=0.4, size=6)
        return img
    return f


def boathouse_door(open_):
    def f():
        img = T.ramp(T.fbm("tdbd" + str(open_), octaves=3, base=2), "#3a2e22", "#4a3c2e")
        xx = np.mgrid[0:S, 0:S][1]
        img = T.shade(img, 1 - ((xx % 8) == 0) * 0.4)
        img = T.border(img, "#1e1a14", 2)
        if open_:
            img = T.rect(img, 3, 3, 29, 32, "#030303")
        else:
            img = T.rect(img, 22, 14, 26, 18, "#6a6a60")
        return img
    return f


tx("td_siding", siding)
tx("td_shingle", shingle)
tx("td_sign_4", number_sign("4"))
tx("td_sign_13", number_sign("13"))
tx("td_door", boathouse_door(False))
tx("td_door_open", boathouse_door(True))
cube("td_siding", "td_siding", sound="wood", display="Boathouse Siding")
cube("td_shingle", "td_shingle", sound="wood", display="Boathouse Roof")
stairs("td_roof", "td_shingle", "td_shingle", display="Boathouse Roof", sound="wood")
for n in ("4", "13"):
    shaped(f"td_number_{n}", [(0, 0, 0, 16, 16, 16, {"all": "w", "north": "s"})], {"w": "td_siding", "s": f"td_sign_{n}"},
           display=f"Boathouse {n}", unbreakable=True, sound="wood")
shaped("td_door", [(0, 0, 0, 16, 16, 16, {"all": "w", "north": "d"})], {"w": "td_siding", "d": "td_door"},
       display="Boathouse Door", unbreakable=True, sound="wood")
exit_block("td_door_open", "td_door_open", "DOOR", display="Open Boathouse Door")


def window_dark():
    img = T.ramp(T.noise("tdwin", sigma=3), "#0a0e10", "#141a1c")
    img = T.border(img, "#5a5446", 2)
    img[15:17, :] = T.hexc("#5a5446")
    return img


def window_lit():
    img = T.solid("#ffc870")
    img = T.shade(img, 0.9 + T.noise("tdwl", sigma=3) * 0.15)
    yy, xx = np.mgrid[0:S, 0:S]
    figure = (((xx - 20) ** 2 + (yy - 11) ** 2) < 10) | ((np.abs(xx - 20) < 4) & (yy > 14))
    img[figure] = T.hexc("#2a1a0e")
    img = T.border(img, "#5a5446", 2)
    img[15:17, :] = T.hexc("#5a5446")
    return img


tx("td_window", window_dark)
tx("td_window_lit", window_lit)
cube("td_window", "td_window", sound="glass", display="Boathouse Window")
cube("td_window_lit", "td_window_lit", sound="glass", display="Lit Boathouse Window", light=9, emissive=True, flat=True)


def life_ring():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    ring = (d < 13) & (d > 7)
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    red = ((ang * 4 / np.pi).astype(int) % 2 == 0)
    rgb[ring] = T.hexc("#e8e4dc")
    rgb[ring & red] = T.hexc("#b02a22")
    a[ring] = 1
    return rgb, a


tx("td_life_ring", life_ring)
shaped("td_life_ring", [(1, 1, 15, 15, 15, 16, {"all": None, "north": "r", "south": "r"})], {"r": "td_life_ring"},
       display="Life Ring", unbreakable=True, collides=False)

# ------------------------------------------------------------------ sound


def lapping():
    d = 24.0
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    r = Y.rng("tdlap")
    for k in range(26):
        at = (k / 26) * d + r.random() * 0.4
        n = int(0.7 * Y.SR)
        tt = np.arange(n) / Y.SR
        slap = Y.bp(Y.white(0.7, f"tdl{k}"), 200 + r.random() * 200, 1500)[:n] * np.exp(-tt * 7) * np.clip(tt / 0.05, 0, 1)
        Y.place(out, slap, at, 0.2 + r.random() * 0.25)
    low = Y.lp(Y.brown(d, "tdlow"), 90) * 0.3
    creak = np.zeros_like(t)
    for k in range(4):
        n = int(0.8 * Y.SR)
        tt = np.arange(n) / Y.SR
        f = 120 + 50 * np.sin(np.pi * tt / 0.8)
        Y.place(creak, Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)), 200, 1400) * np.sin(np.pi * tt / 0.8) * 0.05, 2 + k * 5.5)
    x = Y.loopable(Y.norm(Y.reverb(out, 1.0, 0.3, "tdlr")[:len(t)] + low + creak, 0.45), 1.5)
    return Y.stereoize(x, 0.0021), "stereo"


def loon():
    d = 4.0
    t = Y.t_axis(d)
    f = np.where(t < 1.6, 700 + 300 * np.sin(np.pi * t / 1.6), 1000 - 120 * (t - 1.6))
    x = np.sin(2 * np.pi * np.cumsum(f * (1 + 0.01 * np.sin(2 * np.pi * 6 * t))) / Y.SR) * np.sin(np.pi * t / d) ** 0.7
    return Y.reverb(Y.lp(Y.norm(x, 0.3), 2500), 4.5, 0.85, "tdloon")


def splash():
    out = np.zeros(int(3.0 * Y.SR))
    n = int(1.0 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, Y.bp(Y.white(1.0, "tdsp"), 300, 6000) * np.exp(-tt * 6) * np.clip(tt / 0.01, 0, 1), 0.05, 1.0)
    Y.place(out, Y.lp(Y.white(1.0, "tdsp2"), 300) * np.exp(-tt * 4), 0.08, 0.6)
    return Y.reverb(Y.norm(out, 0.4), 3.0, 0.8, "tdsplash")


def bell():
    out = np.zeros(int(4.0 * Y.SR))
    for k in range(3):
        n = int(2.0 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, (np.sin(2 * np.pi * 784 * tt) + 0.4 * np.sin(2 * np.pi * 2090 * tt)) * np.exp(-tt * 3), k * 0.45, 0.4)
    return Y.reverb(Y.lp(Y.norm(out, 0.25), 3000), 4.0, 0.9, "tdbell")


def pulled():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.lp(Y.white(d, "tdpull"), 500) * (1 - t / d)
    bubbles = np.zeros_like(t)
    r = Y.rng("tdbub")
    for k in range(30):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(bubbles, np.sin(2 * np.pi * (300 + r.random() * 600 + tt * 4000) * tt) * np.exp(-tt * 60), r.random() * 2.5, 0.3)
    return Y.norm(x + bubbles, 0.7)


sound("ambient.the_dock", [("ambient/the_dock", lapping)], stream=True)
sound("ambient.the_dock.additions",
      [("ambient/the_dock_loon", loon), ("ambient/the_dock_splash", splash), ("ambient/the_dock_bell", bell)],
      subtitle="Something out on the lake")
sound("event.the_dock_pulled", [("event/the_dock_pulled", pulled)], subtitle="Something pulls you under")

level("the_dock", fog="#0e1a1c", sky="#0a1214", water="#0c1416", water_fog="#020606", ambient="ambient.the_dock",
      additions="ambient.the_dock.additions", additions_chance=0.003, fixed_time=18000, skylight=True,
      ceiling=False, height=64)
