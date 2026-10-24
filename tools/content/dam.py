"""Dam Interior - concrete chambers seventy metres tall, crossed by one-plank catwalks,
water roaring on the other side of every wall. Spillway galleries flood without much
warning. Don't look down."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, pane, pillar
from content.ind_kit import anim_tex, text, rivets, steel, concrete, hazard_stripes, rust, vstreaks, grate, glow_spot
import tex as T
import synth as Y

S = 32


def dam_concrete(seed="damc", wet=0.0):
    img = concrete(seed, "#7a7a74", "#8e8c84", stain="#4a4a44", stains=2)
    yy, xx = np.mgrid[0:S, 0:S]
    # pour lifts and form-tie holes
    img = T.shade(img, 1 - ((yy % 16) == 15) * 0.18)
    for (x, y) in ((8, 7), (24, 7), (8, 23), (24, 23)):
        img = T.rect(img, x - 1, y - 1, x + 1, y + 1, "#3a3a36")
    if wet:
        img = vstreaks(img, seed + "w", "#3a3c38", int(6 * wet), 0.5 * wet, (16, 32))
        calc = (T.noise(seed + "ca", sigma=1.2) > 1 - wet * 0.25).astype(float)
        img = T.blend(img, T.solid("#d8d6cc"), calc * 0.5)
        moss = np.clip(T.fbm(seed + "m", octaves=3, base=3) * 2 - 1.4 + wet * 0.3, 0, 1)
        img = T.blend(img, T.solid("#3a4a2a"), moss * 0.6)
    return img


def marker():
    img = dam_concrete("damm")
    img = T.rect(img, 4, 2, 28, 30, "#e0dcd0")
    for k in range(7):
        y = 3 + k * 4
        img[y, 4:12 if k % 2 else 18] = T.hexc("#1a1a1a")
    img = text(img, "EL", 18, 8, "#c01818")
    img = text(img, "412", 15, 16, "#1a1a1a")
    return T.border(img, "#5a5a56", 1)


def seep(i):
    img = dam_concrete("damsp", wet=1.0)
    yy, xx = np.mgrid[0:S, 0:S]
    for k, x in enumerate((7, 15, 22, 27)):
        run = ((yy + i * 3 + k * 7) % 32)
        bright = ((run % 9) < 2).astype(float)
        col = (np.abs(xx - x - np.sin(yy / 4.0 + k) * 1) < 1.2)
        img = T.blend(img, T.solid("#b8c8cc"), col * (0.35 + bright * 0.4))
    return img


def spill_floor(i):
    img = dam_concrete("damsf", wet=0.6)
    img = T.shade(img, 0.7)
    film = T.ramp(np.roll(T.noise("damsff", sigma=1.6), i * 4, axis=1), "#3a4a52", "#6a8a96", "#c8e0e8")
    return T.blend(img, film, np.full((S, S), 0.45))


def ledge():
    img = dam_concrete("damld")
    return hazard_stripes(img, 0, 5, "damldh", width=4)


def catwalk():
    rgb, a = grate("damcw", "#4a4e52", "#6a7076", pitch=4, bar=1, rust_amt=0.25)
    return rgb, a


def rail():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = rust(steel("damrl", "#c09a20", "#d8b030", brushed=False), "damrlr", 0.25)
    a = ((yy < 3) | ((yy > 13) & (yy < 16)) | ((xx % 16) < 2)).astype(float)
    return rgb, a


def penstock_side():
    img = rust(steel("dampn", "#3a4a58", "#4a5a6a", brushed=False), "dampnr", 0.35)
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 0.75 + 0.35 * np.sin(np.pi * xx / 31))
    img = rivets(img, [(x, 2) for x in range(1, 32, 4)] + [(x, 30) for x in range(3, 32, 4)], "#8a9aa8", "#101418")
    return vstreaks(img, "dampnv", "#5a3a1e", 3, 0.4)


def penstock_end():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = steel("dampne", "#3a4a58", "#4a5a6a", brushed=False)
    return T.blend(img, T.solid("#0a0e10"), (d < 12).astype(float))


def work_light():
    g = glow_spot((S, S), 15.5, 15.5, 14, 1.2)
    img = T.ramp(g, "#7a4a10", "#ffb050", "#fff0c8")
    return T.border(img, "#2a2a2a", 3)


def light_housing():
    return steel("damlh", "#2a2c2e", "#3a3c3e", brushed=False)


def sluice():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("damsl", sigma=1.5), "#050708", "#0e1214")
    bars = (xx % 6) < 2
    return T.blend(img, rust(steel("damslb", "#3a3e42", "#4a4e52"), "damslr", 0.5), bars.astype(float))


def door():
    img = rust(steel("damdr", "#5a6a5a", "#6a7a6a", brushed=False), "damdrr", 0.3)
    img = T.border(img, "#2a322a", 2)
    img = T.rect(img, 4, 4, 28, 12, "#e8e4d8")
    img = text(img, "INSP", 8, 5, "#1a1a1a")
    img = T.rect(img, 22, 17, 27, 20, "#1a1e1a")
    return img


def gallery_light():
    img = T.ramp(T.noise("damgl", sigma=2), "#e8f0e8", "#ffffff")
    yy, xx = np.mgrid[0:S, 0:S]
    cage = ((xx % 8) == 0) | ((yy % 8) == 0)
    img = T.blend(img, T.solid("#2a2a2a"), cage * 0.8)
    return T.border(img, "#3a3a3a", 2)


def ceiling():
    return T.shade(dam_concrete("damce", wet=0.3), 0.9)


tex("dam_concrete", lambda: dam_concrete())
tex("dam_concrete_b", lambda: dam_concrete("damcb"))
tex("dam_concrete_wet", lambda: dam_concrete("damcw2", wet=0.8))
tex("dam_marker", marker)
anim_tex("dam_seep", seep, 8, frametime=3)
anim_tex("dam_spill_floor", spill_floor, 8, frametime=2)
tex("dam_ledge", ledge)
tex("dam_catwalk", catwalk)
tex("dam_rail", rail)
tex("dam_penstock_side", penstock_side)
tex("dam_penstock_end", penstock_end)
tex("dam_work_light", work_light)
tex("dam_light_housing", light_housing)
tex("dam_sluice", sluice)
tex("dam_door", door)
tex("dam_gallery_light", gallery_light)
tex("dam_ceiling", ceiling)

cube("dam_concrete", "dam_concrete", display="Mass Concrete")
cube("dam_concrete_b", "dam_concrete_b", display="Mass Concrete")
cube("dam_concrete_wet", "dam_concrete_wet", display="Seeping Concrete")
cube("dam_marker", "dam_marker", display="Elevation Marker")
cube("dam_seep", "dam_seep", display="Seeping Wall")
cube("dam_spill_floor", "dam_spill_floor", display="Spillway Channel", sound="mud")
cube("dam_ledge", "dam_ledge", display="Gallery Ledge")
shaped("dam_catwalk", [(0, 13, 0, 16, 16, 16, "g")], {"g": "dam_catwalk"}, display="Catwalk Grating", sound="metal",
       unbreakable=True)
pane("dam_rail", "dam_rail", display="Catwalk Rail", sound="metal")
pillar("dam_penstock", "dam_penstock_end", "dam_penstock_side", display="Penstock", sound="metal")
shaped("dam_work_light", [(3, 3, 0, 13, 13, 5, {"all": "h", "south": "l"})],
       {"l": "dam_work_light", "h": "dam_light_housing"}, display="Work Light", sound="metal", unbreakable=True,
       light=15, emissive=True, collides=False)
exit_block("dam_sluice", "dam_sluice", "WATER", trigger="INSIDE", display="Sluice Intake", sound="metal")
exit_block("dam_door", "dam_door", "DOOR", trigger="USE", solid=True, display="Inspection Door", sound="metal")
cube_faces("dam_gallery_light", bottom="dam_gallery_light", top="dam_ceiling", side="dam_ceiling",
           display="Gallery Light", light=14, emissive=True, flat=True, sound="glass")
cube("dam_ceiling", "dam_ceiling", display="Gallery Ceiling", flat=True)


# ------------------------------------------------------------------ sound

def roar_loop():
    d = 26.0
    t = Y.t_axis(d)
    roar = Y.lp(Y.brown(d, "damroar"), 220) * 1.2 + Y.bp(Y.pink(d, "damhiss"), 300, 2500) * 0.25
    sub = np.sin(2 * np.pi * (round(31 * d) / d) * t) * 0.15
    swell = 0.85 + 0.15 * np.sin(2 * np.pi * t / 13.0)
    x = (roar + sub) * swell
    drips = np.zeros(len(t))
    r = Y.rng("damdrip")
    for k in range(9):
        n = int(0.08 * Y.SR)
        tt = np.arange(n) / Y.SR
        f = 900 + r.random() * 800
        Y.place(drips, np.sin(2 * np.pi * (f + tt * 3000) * tt) * np.exp(-tt * 50), 1 + r.random() * (d - 3), 0.15)
    x = Y.reverb(x + drips, 5.0, 0.55, "damrv")
    return Y.stereoize(Y.loopable(Y.norm(x, 0.62), 1.5), 0.005), "stereo"


def groan():
    d = 4.0
    t = Y.t_axis(d)
    f = 38 + 6 * np.sin(2 * np.pi * 0.4 * t)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * Y.env(len(t), 0.8, 1.5)
    x += Y.lp(Y.brown(d, "damgr"), 90) * 0.6 * Y.env(len(t), 0.5, 1.5)
    return Y.norm(Y.reverb(x, 4.0, 0.6, "damgrr"), 0.6)


def big_drip():
    n = int(0.15 * Y.SR)
    tt = np.arange(n) / Y.SR
    x = np.zeros(int(3.0 * Y.SR))
    Y.place(x, np.sin(2 * np.pi * (700 + tt * 2500) * tt) * np.exp(-tt * 30), 0.05, 1.0)
    return Y.norm(Y.reverb(x, 4.5, 0.75, "dambd"), 0.45)


def slam():
    return Y.norm(Y.reverb(Y.impact("damsl", 1.2, ring=0.5), 5.0, 0.7, "damslr"), 0.5)


def gurgle():
    d = 3.0
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("damgu")
    for k in range(18):
        n = int(0.09 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, np.sin(2 * np.pi * (120 + r.random() * 200 + tt * 900) * tt) * np.exp(-tt * 25), r.random() * 2.6, 0.5)
    return Y.norm(Y.reverb(Y.lp(out, 1200), 3.0, 0.6, "damgur"), 0.45)


def siren():
    d = 6.0
    t = Y.t_axis(d)
    f = 300 + 500 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 3.0))
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.5 + np.sin(2 * np.pi * np.cumsum(f * 1.5) / Y.SR) * 0.2
    x *= Y.env(len(t), 0.5, 0.8)
    return Y.norm(Y.reverb(x, 4.0, 0.6, "damsr"), 0.85)


def surge():
    d = 8.0
    t = Y.t_axis(d)
    env = np.clip(t / 1.5, 0, 1) * np.clip((d - t) / 3.0, 0, 1)
    x = (Y.lp(Y.brown(d, "damsu"), 400) * 1.5 + Y.bp(Y.white(d, "damsu2"), 400, 6000) * 0.5) * env
    return Y.norm(Y.reverb(x, 3.0, 0.4, "damsur"), 0.95)


sound("ambient.dam", [("ambient/dam", roar_loop)], stream=True)
sound("ambient.dam.additions",
      [("ambient/dam_groan", groan), ("ambient/dam_drip", big_drip), ("ambient/dam_slam", slam),
       ("ambient/dam_gurgle", gurgle)], subtitle="The dam holds")
sound("event.dam_siren", [("event/dam_siren", siren)], subtitle="Spillway siren")
sound("event.dam_surge", [("event/dam_surge", surge)], subtitle="Water surges through the gallery")

level("dam", fog="#1a1e20", sky="#1a1e20", water="#2a3e44", water_fog="#081014", ambient="ambient.dam",
      additions="ambient.dam.additions", additions_chance=0.008, fixed_time=18000, height=96,
      particle="minecraft:falling_water", particle_chance=0.004)
