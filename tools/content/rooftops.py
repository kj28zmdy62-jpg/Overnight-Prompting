"""The Rooftops - a city of flat roofs at night with nothing below them: tar and gravel,
parapets, humming AC units, water towers, antennae blinking red, alleys you have to jump.
The wind comes in gusts. Most roof doors are locked."""
import numpy as np
from assets import cube, cube_faces, cross, tex, shaped, exit_block, level, sound
from content.ind_kit import anim_tex, text, steel, concrete, rust, vstreaks, glow_spot, rivets
import tex as T
import synth as Y

S = 32


def brick(seed="roofb", lo="#5a3428", hi="#6e4232"):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=1.2), lo, hi)
    mortar = ((yy % 8) == 0) | (((xx + (yy // 8) * 8) % 16) == 0)
    img = T.blend(img, T.solid("#3a3430"), mortar.astype(float) * 0.8)
    var = T.noise(seed + "v", sigma=4)
    img = T.shade(img, 0.85 + var * 0.25)
    return T.grain(vstreaks(img, seed + "s", "#1e1a18", 3, 0.3), seed, 0.04)


def window(lit=False, seed="roofw"):
    img = brick(seed)
    pane = T.ramp(T.noise(seed + "p", sigma=2), "#0c1016", "#161c24")
    if lit:
        g = glow_spot((S, S), 16, 18, 16, 1.0)
        pane = T.ramp(np.clip(g + T.noise(seed + "l", sigma=3) * 0.2, 0, 1), "#7a5020", "#e8b060", "#fff0c8")
    img[6:28, 7:25] = pane[6:28, 7:25]
    img[16, 7:25] = T.hexc("#2a2420")
    img[6:28, 15:17] = T.hexc("#2a2420")
    img[27:29, 5:27] = T.hexc("#8a8478")
    return img


def tar():
    img = T.ramp(T.noise("roofta", sigma=0.6), "#1e1e1e", "#2e2c2a", "#3e3c38")
    patches = np.clip(T.fbm("rooftp", octaves=3, base=3) * 2 - 1.2, 0, 1)
    img = T.blend(img, T.solid("#141414"), patches * 0.6)
    puddle = (T.noise("rooftw", sigma=3) > 0.78).astype(float)
    img = T.blend(img, T.solid("#2a3038"), puddle * 0.6)
    return T.grain(img, "roofta", 0.06)


def parapet_top():
    img = concrete("roofpt", "#7a7670", "#8e8a82", stains=2)
    return T.border(img, "#4a4640", 1, "ns")


def ac_side():
    img = steel("roofac", "#8a8c88", "#a2a49e", brushed=False)
    yy, xx = np.mgrid[0:S, 0:S]
    fins = ((xx % 3) == 0) & (yy > 6) & (yy < 28)
    img = T.blend(img, T.solid("#3a3c3a"), fins.astype(float) * 0.7)
    return vstreaks(rust(img, "roofacr", 0.2), "roofacv", "#5a4a3a", 2, 0.3)


def ac_top(i):
    img = steel("roofact", "#8a8c88", "#a2a49e", brushed=False)
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    ang = (np.degrees(np.arctan2(yy - 15.5, xx - 15.5)) + 360 + i * 30) % 90
    blade = (d < 12) & (ang < 40)
    img = T.blend(img, T.solid("#1a1a1a"), (d < 13).astype(float) * 0.85)
    img = T.blend(img, T.solid("#5a5c5a"), blade.astype(float))
    grill = ((xx % 4) == 0) | ((yy % 4) == 0)
    return T.blend(img, T.solid("#2a2a2a"), (grill & (d < 13)).astype(float) * 0.6)


def wood_tank():
    img = T.ramp(T.noise("rooftk", sigma=1.5), "#4a3624", "#5e4630")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 - ((xx % 5) == 0) * 0.35)
    for y in (6, 20):
        img[y:y + 2] = rust(steel("roofhoop", "#3a3a3a", "#4a4a4a"), "roofhpr", 0.6)[y:y + 2]
    return T.grain(vstreaks(img, "rooftkv", "#2a1e14", 4, 0.4), "rooftk", 0.05)


def tank_top():
    img = T.ramp(T.noise("roofttp", sigma=1.2), "#2a2420", "#3a3028")
    yy, xx = np.mgrid[0:S, 0:S]
    return T.shade(img, 1 - ((np.abs(xx - yy) % 6) == 0) * 0.3)


def leg():
    return rust(steel("roofleg", "#3a3a38", "#4a4a46"), "rooflegr", 0.5)


def beacon(i):
    on = i < 2
    g = glow_spot((S, S), 15.5, 15.5, 15, 1.0)
    img = T.ramp(g, "#ff2010", "#ff9080", "#ffffff") if on else T.ramp(g, "#3a0806", "#5a100c")
    return T.border(img, "#1a1a1a", 2)


def door(locked=True):
    img = steel("roofdr" + ("l" if locked else "o"), "#4a5258", "#5a6268", brushed=False)
    img = T.border(img, "#2a2e32", 2)
    img = rust(img, "roofdrr" + ("l" if locked else "o"), 0.35)
    img = T.rect(img, 22, 15, 26, 18, "#1a1a1a")
    if locked:
        img = T.rect(img, 6, 6, 26, 12, "#e8e4d8")
        img = text(img, "ALARM", 6, 7, "#c01818")
    else:
        img = T.rect(img, 6, 6, 26, 12, "#1a6a3a")
        img = text(img, "EXIT", 8, 7, "#ffffff")
    return img


def lamp():
    g = glow_spot((S, S), 15.5, 15.5, 14, 1.0)
    img = T.ramp(g, "#5a4010", "#ffd080", "#fffbe8")
    yy, xx = np.mgrid[0:S, 0:S]
    cage = ((xx % 8) == 0) | ((yy % 10) == 0)
    return T.blend(img, T.solid("#2a2a2a"), cage.astype(float) * 0.8)


def skylight():
    yy, xx = np.mgrid[0:S, 0:S]
    g = glow_spot((S, S), 16, 16, 22, 0.8)
    img = T.ramp(np.clip(g * 0.8 + T.noise("roofsk", sigma=4) * 0.2, 0, 1), "#3a4a3a", "#c8d8b0", "#f0f8e0")
    frame = ((xx % 16) < 2) | ((yy % 16) < 2)
    img = T.blend(img, T.solid("#3a3e42"), frame.astype(float))
    return T.stains(img, "roofsks", "#2a2a24", 2, 0.4, 8)


def plank():
    img = T.ramp(T.noise("roofpl", sigma=1.2), "#5a4a34", "#6e5a40")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 10) == 0) * 0.4)
    return T.grain(img, "roofpl", 0.05)


def dish():
    img = T.ramp(T.noise("roofds", sigma=2), "#b8b8b0", "#d0d0c8")
    return T.stains(img, "roofdss", "#6a6a60", 2, 0.4, 7)


def chair_web():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = np.where((((xx // 4) + (yy // 4)) % 2 == 0)[..., None], T.solid("#2a6a4a"), T.solid("#d8d0b8"))
    return T.grain(rgb, "roofch", 0.05)


tex("roof_brick", lambda: brick())
tex("roof_brick_b", lambda: brick("roofb2", "#4a3e36", "#5e5044"))
tex("roof_window", lambda: window())
tex("roof_window_lit", lambda: window(True, "roofwl"))
tex("roof_tar", tar)
tex("roof_parapet_top", parapet_top)
tex("roof_ac_side", ac_side)
anim_tex("roof_ac_top", ac_top, 3, frametime=1)
tex("roof_tank", wood_tank)
tex("roof_tank_top", tank_top)
tex("roof_leg", leg)
anim_tex("roof_beacon", beacon, 8, frametime=5)
tex("roof_door_locked", lambda: door(True))
tex("roof_door_open", lambda: door(False))
tex("roof_lamp", lamp)
tex("roof_skylight", skylight)
tex("roof_plank", plank)
tex("roof_dish", dish)
tex("roof_chair_web", chair_web)

cube("roof_brick", "roof_brick", display="Brick")
cube("roof_brick_b", "roof_brick_b", display="Brick")
cube("roof_window", "roof_window", display="Window", sound="glass")
cube("roof_window_lit", "roof_window_lit", display="Lit Window", sound="glass", light=10, emissive=True)
cube("roof_tar", "roof_tar", display="Tar Roof", sound="gravel")
cube_faces("roof_parapet", top="roof_parapet_top", bottom="roof_brick", side="roof_brick", display="Parapet")
cube_faces("roof_parapet_b", top="roof_parapet_top", bottom="roof_brick_b", side="roof_brick_b", display="Parapet")
shaped("roof_ac", [(1, 0, 1, 15, 13, 15, {"all": "s", "up": "t"}), (3, 13, 3, 13, 14, 13, "s")],
       {"s": "roof_ac_side", "t": "roof_ac_top"}, display="Condenser Unit", sound="metal", unbreakable=True)
cube_faces("roof_tank", top="roof_tank_top", bottom="roof_tank_top", side="roof_tank", display="Water Tank", sound="wood")
shaped("roof_tank_cap", [(1, 0, 1, 15, 4, 15, "t"), (4, 4, 4, 12, 8, 12, "t"), (7, 8, 7, 9, 11, 9, "l")],
       {"t": "roof_tank_top", "l": "roof_leg"}, display="Tank Roof", sound="wood", unbreakable=True)
shaped("roof_leg", [(6, 0, 6, 10, 16, 10, "l")], {"l": "roof_leg"}, display="Tower Leg", sound="metal",
       unbreakable=True)
shaped("roof_antenna", [(7, 0, 7, 9, 16, 9, "l"), (3, 8, 7.5, 13, 9, 8.5, "l")], {"l": "roof_leg"},
       display="Antenna Mast", sound="metal", unbreakable=True)
shaped("roof_beacon", [(5, 0, 5, 11, 6, 11, "b")], {"b": "roof_beacon"}, display="Aircraft Light", sound="glass",
       unbreakable=True, light=8, emissive=True, collides=False)
cube("roof_door_locked", "roof_door_locked", display="Roof Door", sound="metal")
exit_block("roof_door", "roof_door_open", "DOOR", trigger="USE", solid=True, display="Roof Door", sound="metal")
shaped("roof_lamp", [(5, 4, 10, 11, 10, 16, {"all": "l"})], {"l": "roof_lamp"}, display="Bulkhead Lamp",
       sound="glass", unbreakable=True, light=13, emissive=True, collides=False)
cube("roof_skylight", "roof_skylight", display="Skylight", sound="glass", light=11, emissive=True)
shaped("roof_plank", [(2, 13, 0, 14, 16, 16, "p")], {"p": "roof_plank"}, display="Scaffold Plank", sound="wood",
       unbreakable=True)
shaped("roof_dish", [(7, 0, 7, 9, 8, 9, "l"), (2, 6, 3, 14, 16, 5, {"all": "d"}), (7, 9, 5, 9, 11, 10, "l")],
       {"l": "roof_leg", "d": "roof_dish"}, display="Satellite Dish", sound="metal", unbreakable=True)
shaped("roof_chair", [(3, 6, 3, 13, 7, 13, "w"), (3, 7, 12, 13, 16, 13, "w"), (3, 0, 3, 4, 6, 4, "l"),
                      (12, 0, 3, 13, 6, 4, "l"), (3, 0, 12, 4, 6, 13, "l"), (12, 0, 12, 13, 6, 13, "l")],
       {"w": "roof_chair_web", "l": "roof_leg"}, display="Lawn Chair", sound="metal", unbreakable=True)


# ------------------------------------------------------------------ sound

def wind_loop():
    d = 28.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "roofwind"), 120, 2200) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 9.33) ** 2)
    city = Y.lp(Y.brown(d, "roofcity"), 180) * 0.7 + Y.bp(Y.pink(d, "roofcity2"), 200, 900) * 0.2
    hum = Y.hum(d, 60, "roofhum", buzz=0.1, harmonics=(1, 2, 3)) * 0.05
    x = wind + city + hum
    return Y.stereoize(Y.loopable(Y.norm(x, 0.58), 1.5), 0.006), "stereo"


def siren():
    d = 6.0
    t = Y.t_axis(d)
    f = 700 + 300 * np.sin(2 * np.pi * 0.35 * t)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * Y.env(len(t), 1.5, 2.0)
    return Y.norm(Y.reverb(Y.lp(x, 2500), 4.0, 0.7, "roofsir"), 0.3)


def helicopter():
    d = 6.0
    t = Y.t_axis(d)
    thump = (np.sin(2 * np.pi * 11 * t) > 0.6) * Y.lp(Y.white(d, "roofheli"), 300)
    x = thump * Y.env(len(t), 2.5, 2.5)
    return Y.norm(Y.reverb(x, 3.0, 0.5, "roofhelr"), 0.4)


def ac_cycle():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.hum(d, 30, "roofaccy", buzz=0.4) * 0.4 * Y.env(len(t), 0.4, 1.0) + Y.bp(Y.white(d, "roofacw"), 300, 1500) * 0.2 * Y.env(len(t), 0.6, 1.0)
    Y.place(x, Y.impact("roofack", 0.2, ring=0.3) * 0.5, 0.0, 1.0)
    return Y.norm(x, 0.4)


def flutter():
    out = np.zeros(int(1.5 * Y.SR))
    r = Y.rng("roofflt")
    for k in range(16):
        n = int(0.03 * Y.SR)
        Y.place(out, Y.bp(Y.white(0.03, f"roofft{k}"), 400, 3000)[:n] * Y.decay(n, 0.01), 0.1 + k * 0.05 + r.random() * 0.02, 0.6)
    return Y.norm(Y.reverb(out, 1.5, 0.3, "rooffltr"), 0.4)


def gust():
    d = 4.0
    t = Y.t_axis(d)
    env = np.clip(t / 1.5, 0, 1) ** 2 * np.clip((d - t) / 1.5, 0, 1)
    x = Y.bp(Y.pink(d, "roofgust"), 200, 3000) * env + np.sin(2 * np.pi * np.cumsum(300 + 200 * env) / Y.SR) * env * 0.05
    return Y.norm(x, 0.9)


sound("ambient.rooftops", [("ambient/rooftops", wind_loop)], stream=True)
sound("ambient.rooftops.additions",
      [("ambient/rooftops_siren", siren), ("ambient/rooftops_heli", helicopter), ("ambient/rooftops_ac", ac_cycle),
       ("ambient/rooftops_flutter", flutter)], subtitle="The city, far below")
sound("event.rooftops_gust", [("event/rooftops_gust", gust)], subtitle="A gust of wind")

level("rooftops", fog="#1a1418", sky="#0e0c14", ambient="ambient.rooftops", additions="ambient.rooftops.additions",
      additions_chance=0.006, fixed_time=18000, skylight=True, ceiling=False, height=80)
