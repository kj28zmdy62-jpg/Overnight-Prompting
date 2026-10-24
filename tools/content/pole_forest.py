"""Telephone Pole Forest. A forest where every tree is a creosote pole, strung together
with wire that hums in the fog. Now and then the hum climbs. When the wires sing, get
away from the poles."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, exit_block, hazard, level, sound
import assets as A
import tex as T
import synth as Y
from content.outside_kit import animated, draw_text, centered, paint_metal

S = 32
tx = A.tex


def litter():
    img = T.ramp(T.fbm("pfl", octaves=4, base=4), "#2a2218", "#3a2e20", "#4a3a28")
    r = T.rng("pfneedles")
    for _ in range(170):
        x, y = r.random() * S, r.random() * S
        ang = r.random() * np.pi
        c = T.hexc("#6a5032") * (0.7 + r.random() * 0.5)
        for k in range(4):
            px, py = int(x + np.cos(ang) * k) % S, int(y + np.sin(ang) * k) % S
            img[py, px] = c
    return T.grain(img, "pfl", 0.05)


def moss():
    img = T.ramp(T.fbm("pfm", octaves=4, base=3), "#2a3220", "#36422a", "#445234")
    return T.grain(T.shade(img, 0.9 + T.noise("pfm2", sigma=0.4) * 0.2), "pfm", 0.05)


def soil():
    return T.grain(T.ramp(T.fbm("pfsoil", octaves=4, base=3), "#22201a", "#2e2a22"), "pfsoil", 0.05)


def soil_side():
    img = soil()
    top = litter()
    yy = np.mgrid[0:S, 0:S][0]
    fr = 4 + (T.noise("pffr", shape=(1, S), sigma=0.8)[0] * 5).astype(int)
    return T.blend(img, top, (yy < fr[None, :]).astype(float))


def fern():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("pffern")
    for f in range(5):
        x0 = 6 + r.random() * 20
        ang = (r.random() - 0.5) * 1.6
        ln = 14 + r.random() * 12
        for k in range(int(ln)):
            px = x0 + np.sin(ang) * k
            py = S - 1 - np.cos(ang) * k * 0.9 - (k / ln) ** 2 * -3
            if 0 <= int(px) < S and 0 <= int(py) < S:
                rgb[int(py), int(px)] = T.hexc("#3a4a26")
                a[int(py), int(px)] = 1
            if k % 2 == 0 and k > 2:
                for side in (-1, 1):
                    for j in range(1, int(4 * (1 - k / ln)) + 2):
                        qx = int(px + side * j * np.cos(ang)), int(py - j * 0.5)
                        if 0 <= qx[0] < S and 0 <= qx[1] < S:
                            rgb[qx[1], qx[0]] = T.hexc("#4e5e30") * (0.8 + r.random() * 0.3)
                            a[qx[1], qx[0]] = 1
    return rgb, a


def brush():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("pfbrush")
    for _ in range(9):
        x, y = 8 + r.random() * 16, S - 1
        ang = (r.random() - 0.5) * 1.2
        for k in range(22):
            ang += (r.random() - 0.5) * 0.4
            x += np.sin(ang)
            y -= np.cos(ang)
            if 0 <= int(x) < S and 0 <= int(y) < S:
                rgb[int(y), int(x)] = T.hexc("#3a2e24")
                a[int(y), int(x)] = 1
    return rgb, a


tx("pf_litter", litter)
tx("pf_moss", moss)
tx("pf_soil", soil)
tx("pf_soil_side", soil_side)
tx("pf_fern", fern)
tx("pf_brush", brush)
cube_faces("pf_litter", top="pf_litter", bottom="pf_soil", side="pf_soil_side", sound="gravel", display="Forest Floor")
cube_faces("pf_moss", top="pf_moss", bottom="pf_soil", side="pf_soil_side", sound="moss", display="Moss")
cube("pf_soil", "pf_soil", sound="gravel", display="Dark Soil")
cross("pf_fern", "pf_fern", display="Fern")
cross("pf_brush", "pf_brush", display="Dead Brush")

# ------------------------------------------------------------------ poles and wire


def creosote(seed="pfp", tag=None):
    img = T.ramp(T.fbm(seed, octaves=3, base=2), "#2a221c", "#3a3026", "#463a2e")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 - (np.sin(xx * 1.7 + T.noise(seed + "g", sigma=2) * 5) > 0.7) * 0.2)
    img = T.cracks(img, seed + "c", "#14100c", count=2, alpha=0.6)
    if tag:
        img = T.rect(img, 9, 8, 23, 18, "#8a8c88")
        img = T.border(img, "#2a2a28", 0)
        img = draw_text(img, tag, 10, 11, "#1a1a1a")
    staples = (T.rng(seed + "st").random((S, S)) > 0.985).astype(float)
    img = T.blend(img, T.solid("#7a7a72"), staples)
    return T.grain(img, seed, 0.05)


def crossarm():
    img = T.ramp(T.fbm("pfca", octaves=3, base=2), "#4a4034", "#5a4e40")
    yy = np.mgrid[0:S, 0:S][0]
    return T.grain(T.shade(img, 1 - (np.sin(yy * 1.5) > 0.8) * 0.15), "pfca", 0.04)


def insulator():
    img = T.ramp(T.noise("pfins", sigma=1), "#3a6a5a", "#5a9a82")
    yy = np.mgrid[0:S, 0:S][0]
    return T.shade(img, 1 - ((yy % 6) < 2) * 0.25)


def wire():
    return np.ones((S, S, 3)) * T.hexc("#0c0c0c"), np.ones((S, S))


def transformer():
    img = paint_metal("pftr", "#6a6e70", "#82868a", rust=0.25)
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.2)
    img = T.rect(img, 10, 12, 22, 18, "#d8d0b0")
    img = draw_text(img, "25", 12, 13, "#1a1a1a")
    return img


tx("pf_pole", lambda: creosote())
tx("pf_pole_tag", lambda: creosote("pfpt", "PF7"))
tx("pf_pole_tag2", lambda: creosote("pfpt2", "404"))
tx("pf_crossarm", crossarm)
tx("pf_insulator", insulator)
tx("pf_wire", wire)
tx("pf_transformer", transformer)

POLE = [(6, 0, 6, 10, 16, 10, "p")]
shaped("pf_pole", POLE, {"p": "pf_pole"}, display="Telephone Pole", unbreakable=True)
shaped("pf_pole_tagged", POLE, {"p": "pf_pole_tag"}, display="Telephone Pole", unbreakable=True)
shaped("pf_pole_tagged2", POLE, {"p": "pf_pole_tag2"}, display="Telephone Pole", unbreakable=True)
# crossarm along x with two insulators; placed facing EAST/WEST it runs along z
shaped("pf_crossarm", [(6, 0, 6, 10, 16, 10, "p"), (0, 11, 7, 16, 13, 9, "a"),
                       (1, 13, 7.5, 2, 15, 8.5, "i"), (14, 13, 7.5, 15, 15, 8.5, "i"),
                       (4.5, 13, 7.5, 5.5, 15, 8.5, "i"), (10.5, 13, 7.5, 11.5, 15, 8.5, "i")],
       {"p": "pf_pole", "a": "pf_crossarm", "i": "pf_insulator"}, display="Pole Crossarm", unbreakable=True)
# four wires running along z (after rotation: along x), at the insulator positions
WIRE_HI = [(1.3, 14.6, 0, 1.7, 15, 16, "w"), (14.3, 14.6, 0, 14.7, 15, 16, "w"),
           (4.8, 14.6, 0, 5.2, 15, 16, "w"), (10.8, 14.6, 0, 11.2, 15, 16, "w")]
WIRE_SAG = [(b[0], b[1] - 7, b[2], b[3], b[4] - 7, b[5], b[6]) for b in WIRE_HI]
shaped("pf_wire", WIRE_HI, {"w": "pf_wire"}, display="Telephone Wire", collides=False, unbreakable=True, sound="metal")
shaped("pf_wire_sag", WIRE_SAG, {"w": "pf_wire"}, display="Sagging Wire", collides=False, unbreakable=True, sound="metal")
shaped("pf_transformer", [(6, 0, 6, 10, 16, 10, "p"), (8, 1, 3, 15, 13, 11, "t"), (9, 13, 4, 14, 14, 10, "t")],
       {"p": "pf_pole", "t": "pf_transformer"}, display="Pole Transformer", unbreakable=True, sound="metal")
shaped("pf_pole_fallen", [(0, 0, 6, 16, 4, 10, "p")], {"p": "pf_pole"}, display="Fallen Pole", unbreakable=True)
shaped("pf_pole_stump", [(6, 0, 6, 10, 9, 10, "p")], {"p": "pf_pole"}, display="Snapped Pole", unbreakable=True)


def spark(i):
    r = T.rng("pfspark", i)
    rgb = T.ramp(T.noise(f"pfsd{i}", sigma=1.5), "#1a1612", "#2a241c")
    yy, xx = np.mgrid[0:S, 0:S]
    cable = np.abs(yy - 16 - np.sin(xx / 4 + i) * 2) < 1.4
    rgb[cable] = T.hexc("#0a0a0a")
    if i % 3 != 2:
        cx = int(r.random() * 24) + 4
        for k in range(10):
            ang = r.random() * np.pi * 2
            ln = 2 + r.random() * 6
            for j in range(int(ln)):
                px, py = int(cx + np.cos(ang) * j), int(16 + np.sin(ang) * j)
                if 0 <= px < S and 0 <= py < S:
                    rgb[py, px] = T.hexc("#cfe8ff") if j < 2 else T.hexc("#7ab0ff")
    return rgb


animated("pf_live_wire", spark, 6, frametime=2)
hazard("pf_live_wire", "pf_live_wire", damage=3.0, display="Downed Live Wire", light=6, emissive=True)


def manhole():
    img = paint_metal("pfmh", "#2e2e2c", "#3e3e3a", rust=0.3)
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.shade(img, 1 - ((d > 14) & (d < 15.5)) * 0.5 - (((xx + yy) % 6 == 0) & (d < 13)) * 0.15)
    img = centered(img, "BELL", 13, "#1a1a18")
    return img


tx("pf_manhole", manhole)
exit_block("pf_manhole", "pf_manhole", "DESCEND", trigger="USE", solid=True, display="Cable Vault Cover",
           sound="metal")


def substation_door():
    img = paint_metal("pfsd", "#5a6064", "#6a7074", rust=0.2)
    img = T.border(img, "#3a3e40", 2)
    img = T.rect(img, 6, 4, 26, 16, "#e8d040")
    img = T.rect(img, 6, 4, 26, 6, "#1a1a1a")
    img = centered(img, "DANGER", 8, "#1a1a1a")
    img = T.rect(img, 23, 20, 26, 24, "#2a2a28")
    return img


tx("pf_substation_door", substation_door)
exit_block("pf_substation_door", "pf_substation_door", "DOOR", trigger="USE", solid=True, display="Substation Door",
           sound="metal")
tx("pf_substation_wall", lambda: T.grain(T.ramp(T.fbm("pfsw", octaves=3, base=3), "#5a5a56", "#6a6a64"), "pfsw", 0.05))
cube("pf_substation_wall", "pf_substation_wall", display="Substation Wall")

# ------------------------------------------------------------------ sound


def singing_wires():
    d = 24.0
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    r = Y.rng("pfsing")
    for base in (120.0, 180.0, 240.0, 360.0):
        for k in range(3):
            f = round((base + (r.random() - 0.5) * 0.8) * d) / d
            amp = 0.05 / (base / 120.0) * (0.6 + 0.4 * np.sin(2 * np.pi * t * round((0.05 + r.random() * 0.1) * d) / d + r.random() * 6))
            out += np.sin(2 * np.pi * f * t + r.random() * 6.28) * amp
    wind = Y.bp(Y.pink(d, "pfwind"), 200, 1200) * 0.15 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 12))
    drip = np.zeros_like(t)
    for k in range(10):
        n = int(0.08 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(drip, np.sin(2 * np.pi * (1200 + r.random() * 600) * tt) * np.exp(-tt * 50) * 0.08, r.random() * (d - 0.2))
    x = Y.loopable(Y.norm(out + wind + Y.reverb(drip, 1.5, 0.6, "pfdrip")[:len(t)], 0.5), 1.5)
    return Y.stereoize(x, 0.0019), "stereo"


def pop():
    out = np.zeros(int(3.0 * Y.SR))
    n = int(0.4 * Y.SR)
    tt = np.arange(n) / Y.SR
    crack = Y.hp(Y.white(0.4, "pfpop"), 800)[:n] * np.exp(-tt * 18)
    Y.place(out, crack, 0.05, 1.0)
    Y.place(out, Y.hum(1.5, 60, "pfpophum", buzz=1.0)[: int(1.5 * Y.SR)] * np.exp(-np.arange(int(1.5 * Y.SR)) / Y.SR * 3) * 0.4, 0.1)
    return Y.reverb(Y.lp(Y.norm(out, 0.5), 5000), 3.0, 0.75, "pfpop")


def twang():
    d = 3.5
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for f, g in ((87.3, 1.0), (174.6, 0.5), (262.0, 0.3), (349.2, 0.2)):
        x += np.sin(2 * np.pi * f * t * (1 + 0.002 * np.sin(2 * np.pi * 5 * t))) * g * np.exp(-t * (1.0 + f / 300))
    return Y.reverb(Y.norm(x, 0.45), 3.0, 0.7, "pftwang")


def creak():
    d = 2.0
    t = Y.t_axis(d)
    f = 70 + 30 * np.sin(np.pi * t / d)
    x = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)) * (1 + 0.5 * Y.white(d, "pfcr")), 200, 1500) * np.sin(np.pi * t / d)
    return Y.reverb(Y.norm(x, 0.35), 2.5, 0.65, "pfcreak")


def crosstalk():
    v = Y.whisper("pfvoice", 2.2)
    v = Y.bp(v, 500, 2800) * 1.5
    line = Y.bp(Y.white(2.4, "pfline"), 1500, 3400) * 0.06
    out = np.zeros(int(2.4 * Y.SR))
    Y.place(out, v, 0.1)
    return Y.reverb(Y.norm(out + line, 0.35), 2.0, 0.6, "pfvoice")


def rise():
    d = 4.0
    t = Y.t_axis(d)
    f = 120 * (1 + 2.5 * (t / d) ** 2)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) + 0.5 * np.sin(2 * np.pi * np.cumsum(f * 2.01) / Y.SR)
    x *= (t / d) ** 1.5
    x += Y.hp(Y.white(d, "pfrise"), 3000) * 0.15 * (t / d) ** 3
    return Y.norm(x, 0.75)


def zap():
    d = 1.8
    t = Y.t_axis(d)
    x = Y.hp(Y.white(d, "pfzap"), 600) * np.exp(-t * 3.5)
    x *= (np.sin(2 * np.pi * 60 * t) > 0.2) * 0.6 + 0.4
    x += Y.lp(Y.white(d, "pfzap2"), 150) * np.exp(-t * 10) * 3
    return Y.norm(x, 0.95)


def transformer_hum():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.hum(d, 60.0, "pftrh", buzz=0.6, harmonics=(2, 4, 6, 8, 10)) * Y.env(len(t), 0.3, 0.3)
    return Y.norm(Y.hp(x, 90), 0.5)


sound("ambient.pole_forest", [("ambient/pole_forest", singing_wires)], stream=True)
sound("ambient.pole_forest.additions",
      [("ambient/pole_forest_pop", pop), ("ambient/pole_forest_twang", twang),
       ("ambient/pole_forest_creak", creak), ("ambient/pole_forest_crosstalk", crosstalk)],
      subtitle="Something on the line")
sound("event.pole_forest_rise", [("event/pole_forest_rise", rise)], subtitle="The wires start to sing")
sound("event.pole_forest_zap", [("event/pole_forest_zap", zap)], subtitle="Current arcs down a pole")
sound("event.pole_forest_hum", [("event/pole_forest_hum", transformer_hum)], subtitle="A transformer hums")

level("pole_forest", fog="#46525c", sky="#46525c", ambient="ambient.pole_forest",
      additions="ambient.pole_forest.additions", additions_chance=0.004, fixed_time=12900, skylight=True,
      ceiling=False, height=96)
