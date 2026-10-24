"""Level 11 - The Endless City. Towers and streets in a permanent dusk, windows lit, nobody home.
Safe. Nothing that follows you in makes it past the first block."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, level, sound
import tex as T
import synth as Y
from content.classics_kit import centered, draw_text, metal, bricks

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def road(line=None):
    def f():
        img = T.ramp(T.noise("l11r", sigma=0.5) * 0.6 + T.fbm("l11rf", octaves=3, base=5) * 0.4, "#26262a", "#323236")
        img = T.cracks(img, "l11rc", "#18181a", 1, 0.4)
        if line == "x":
            img = T.blend(img, T.solid("#d8d8d0"), ((np.abs(yy - 15.5) < 1.2) & ((xx % 16) < 9)) * 0.85)
        elif line == "z":
            img = T.blend(img, T.solid("#d8d8d0"), ((np.abs(xx - 15.5) < 1.2) & ((yy % 16) < 9)) * 0.85)
        elif line == "cross":
            img = T.blend(img, T.solid("#e0e0d8"), (((xx // 4) % 2) == 0) * 0.85)
        return T.grain(img, "l11r" + str(line), 0.04)
    return f


def pavement():
    img = T.ramp(T.fbm("l11p", octaves=3, base=5), "#6a6660", "#7a766e")
    img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.3)
    gum = (T.rng("l11gum").random((S, S)) > 0.985)
    return T.grain(T.blend(img, T.solid("#3a3a38"), gum * 0.6), "l11p", 0.04)


def facade(kind, lit):
    def f():
        if kind == "glass":
            base = T.ramp(T.noise("l11gl", sigma=3), "#1a2a38", "#2a3a4a")
            frame = "#5a6a74"
        elif kind == "brick":
            base = bricks("l11br", "#6a3a2a", "#7a4632", mortar="#4a3a30")
            frame = "#3a2a22"
        else:
            base = T.ramp(T.fbm("l11cn" + kind, octaves=3, base=4), "#8a8478", "#9a9488")
            frame = "#6a665c"
        img = base.copy()
        if kind == "glass":
            img = T.rect(img, 0, 0, 32, 2, frame)
            img = T.rect(img, 0, 0, 2, 32, frame)
            refl = np.clip(1 - np.abs((xx - yy * 0.5) - 10) / 6.0, 0, 1)
            img = T.blend(img, T.solid("#e8a060"), refl * 0.25)
        win_col = "#ffd890" if lit == 1 else ("#c8e0ff" if lit == 2 else "#10141a")
        for wx0 in (4, 19):
            if kind == "glass":
                continue
            img = T.rect(img, wx0, 6, wx0 + 9, 24, "#2a2826")
            inner = T.ramp(T.noise(f"l11w{kind}{lit}{wx0}", sigma=2), win_col, win_col)
            if lit:
                inner = T.shade(inner, 0.8 + 0.3 * T.noise(f"l11wl{wx0}", sigma=3))
            img[7:23, wx0 + 1:wx0 + 8] = inner[7:23, wx0 + 1:wx0 + 8]
        if kind == "glass" and lit:
            m = np.zeros((S, S))
            m[4:28, 4:28] = 1
            img = T.blend(img, T.solid(win_col), m * 0.75)
        return T.grain(img, f"l11f{kind}{lit}", 0.03)
    return f


def shopfront():
    img = T.ramp(T.noise("l11sh", sigma=3), "#e8c890", "#ffe0a8")
    img = T.rect(img, 0, 0, 32, 3, "#3a2a20")
    img = T.rect(img, 15, 0, 17, 32, "#3a2a20")
    img = T.blend(img, T.solid("#ffffff"), np.clip(1 - np.abs(xx - yy - 4) / 4, 0, 1) * 0.2)
    return img


def awning():
    img = T.solid("#8a1a1a")
    img = T.blend(img, T.solid("#e8e0d0"), ((xx // 4) % 2 == 0) * 1.0)
    return T.grain(img, "l11aw", 0.04)


def sign(text, bg, fg):
    def f():
        img = T.solid(bg)
        img = centered(img, text, 13, fg)
        return T.border(img, "#1a1a1a", 2)
    return f


def roof():
    img = T.ramp(T.fbm("l11rf", octaves=3, base=4), "#3a3a36", "#46463e")
    return T.grain(T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.3), "l11rf", 0.04)


def ac_unit():
    img = metal("l11ac", "#9a9c98", "#aeb0aa")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#2a2c2a"), (d < 11).astype(float))
    img = T.blend(img, T.solid("#7a7c78"), ((np.abs(d - 5) < 1) | (np.abs(xx - 15.5) < 1) | (np.abs(yy - 15.5) < 1)) * (d < 11) * 1.0)
    return img


def lobby_floor():
    img = T.ramp(T.noise("l11lf", sigma=2), "#c8c0b0", "#d8d0c0")
    img = T.blend(img, T.solid("#8a8070"), (((xx // 16) + (yy // 16)) % 2 == 0) * 0.35)
    return T.grain(img, "l11lf", 0.02)


def lobby_wall():
    return T.grain(T.ramp(T.fbm("l11lw", octaves=2, base=5), "#b8b0a0", "#c4bcac"), "l11lw", 0.02)


def lamp_on():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.border(T.ramp(np.clip(1 - d / 22, 0, 1), "#e8a050", "#ffd890", "#fff0c8"), "#2a2a2a", 3)


def lamp_off():
    return T.border(T.solid("#4a4a44"), "#2a2a2a", 3)


def pole():
    return metal("l11pole", "#2a3a32", "#34463c", sigma=1.5)


def signal(state):
    def f():
        img = T.solid("#1a1a1a")
        for i, c in enumerate(("#3a0a08", "#ffb020" if state else "#3a2a08", "#08200e")):
            d = np.sqrt((xx - 15.5) ** 2 + (yy - (6 + i * 10)) ** 2)
            img = T.blend(img, T.solid(c), (d < 4).astype(float))
        return img
    return f


def bench():
    return T.grain(T.ramp(T.noise("l11be", sigma=0.6), "#5a3a22", "#6a4628"), "l11be", 0.04)


def subway_sign():
    img = T.solid("#1a3a8a")
    img = centered(img, "SUBWAY", 6, "#ffffff")
    img = centered(img, "V V V", 18, "#ffffff")
    return T.border(img, "#d8d8d8", 2)


def subway_dark():
    return T.vgrad(S, S, "#3a3a36", "#000000")


def transit_door():
    img = metal("l11td", "#8a8e90", "#9ea2a4", sigma=0.8)
    img = T.rect(img, 15, 0, 17, 32, "#3a3c3e")
    img = T.rect(img, 4, 4, 28, 12, "#1a1a1a")
    img = centered(img, "OUT", 6, "#ffb020")
    return img


TEX = {"l11_road": road(), "l11_road_x": road("x"), "l11_road_z": road("z"), "l11_crosswalk": road("cross"),
       "l11_pavement": pavement, "l11_shopfront": shopfront, "l11_awning": awning, "l11_roof": roof,
       "l11_ac": ac_unit, "l11_lobby_floor": lobby_floor, "l11_lobby_wall": lobby_wall, "l11_lamp_on": lamp_on,
       "l11_lamp_off": lamp_off, "l11_pole": pole, "l11_signal_on": signal(True), "l11_signal_off": signal(False),
       "l11_bench": bench, "l11_subway_sign": subway_sign, "l11_subway_dark": subway_dark,
       "l11_transit_door": transit_door,
       "l11_sign_diner": sign("DINER", "#1a1a1a", "#ff4060"), "l11_sign_open": sign("OPEN", "#1a1a1a", "#60ff90"),
       "l11_sign_hotel": sign("HOTEL", "#1a1a1a", "#ffd040"), "l11_sign_safe": sign("SAFE", "#1a1a1a", "#60c0ff")}
for kind in ("concrete", "brick", "glass", "stone"):
    for lit in (0, 1, 2):
        TEX[f"l11_{kind}_{lit}"] = facade(kind, lit)
for k, fn in TEX.items():
    tex(k, fn)

cube("city_road", "l11_road", display="City Road")
cube("city_road_line_x", "l11_road_x", display="City Road")
cube("city_road_line_z", "l11_road_z", display="City Road")
cube("city_crosswalk", "l11_crosswalk", display="Crosswalk")
cube("city_pavement", "l11_pavement", display="Pavement")
for kind in ("concrete", "brick", "glass", "stone"):
    cube_faces(f"city_{kind}", top="l11_roof", bottom="l11_roof", side=f"l11_{kind}_0", display="Tower Facade")
    cube_faces(f"city_{kind}_lit", top="l11_roof", bottom="l11_roof", side=f"l11_{kind}_1", display="Lit Window",
               light=6, emissive=True)
    cube_faces(f"city_{kind}_tv", top="l11_roof", bottom="l11_roof", side=f"l11_{kind}_2", display="Lit Window",
               light=5, emissive=True)
cube("city_shopfront", "l11_shopfront", display="Shop Window", light=9, emissive=True, sound="glass")
cube("city_awning", "l11_awning", display="Awning", sound="wool")
cube("city_roof", "l11_roof", display="Rooftop")
shaped("city_ac_unit", [(1, 0, 1, 15, 10, 15, {"all": "side", "up": "fan"})], {"side": "l11_pole", "fan": "l11_ac"},
       display="Rooftop Unit", sound="metal", unbreakable=True)
cube("city_lobby_floor", "l11_lobby_floor", display="Lobby Floor", sound="tile")
cube("city_lobby_wall", "l11_lobby_wall", display="Lobby Wall")
flicker_light("city_lobby_light", "l11_lamp_on", "l11_lamp_off", "l11_lobby_wall", light=14, flicker=0.02,
              display="Lobby Light")
flicker_light("city_streetlamp", "l11_lamp_on", "l11_lamp_off", "l11_pole", light=15, flicker=0.01,
              display="City Streetlight")
shaped("city_lamp_pole", [(6.5, 0, 6.5, 9.5, 16, 9.5, "pole")], {"pole": "l11_pole"}, display="Lamp Post",
       sound="metal", unbreakable=True)
flicker_light("city_signal", "l11_signal_on", "l11_signal_off", "l11_signal_off", light=6, flicker=0.9,
              display="Traffic Signal", face="all")
shaped("city_bench", [(0, 5, 3, 16, 6.5, 12, "wood"), (0, 6.5, 11, 16, 13, 12.5, "wood"), (1, 0, 4, 2.5, 5, 11, "metal"),
                      (13.5, 0, 4, 15, 5, 11, "metal")], {"wood": "l11_bench", "metal": "l11_pole"},
       display="Bench", sound="wood", unbreakable=True)
for name in ("diner", "open", "hotel", "safe"):
    shaped(f"city_sign_{name}", [(0, 2, 0, 16, 14, 2, {"all": "frame", "south": "sign"})],
           {"frame": "l11_pole", "sign": f"l11_sign_{name}"}, display="Neon Sign", light=10, emissive=True,
           collides=False, sound="glass", unbreakable=True)
shaped("city_vending", [(1, 0, 1, 15, 16, 15, {"all": "side", "south": "front"})],
       {"side": "l4_vend_side", "front": "l4_vend_low"}, display="Vending Machine", kind="pickup",
       item="noclip:almond_water*4,noclip:energy_bar*3,noclip:canned_beans*2,noclip:battery*2", sound="metal",
       light=7, emissive=True, strength=0.5)
cube("city_subway_sign", "l11_subway_sign", display="Subway Sign", light=8, emissive=True)
exit_block("city_subway_stairs", "l11_subway_dark", "DESCEND", display="Subway Stairs")
exit_block("city_transit_door", "l11_transit_door", "DOOR", trigger="USE", solid=True, display="Transit Door",
           sound="metal")

# ------------------------------------------------------------------ sound


def city_loop():
    d = 30.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "l11w"), 200, 1400) * 0.4 * (0.6 + 0.4 * np.sin(2 * np.pi * t * 3 / d))
    traffic = Y.lp(Y.brown(d, "l11tr"), 220) * 0.6
    hum = Y.drone(d, [110.0, 164.8], "l11hum", lfo=0.05) * 0.04
    # a music box tune drifting from an open window somewhere
    notes = [523.3, 659.3, 784.0, 659.3, 698.5, 587.3, 523.3, 493.9]
    box = np.zeros_like(t)
    for k in range(int(d / 0.75)):
        n = int(1.2 * Y.SR)
        tt = np.arange(n) / Y.SR
        f = notes[k % len(notes)] * 2
        Y.place(box, (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 2.76 * tt)) * np.exp(-tt * 4), k * 0.75, 0.3)
    box = Y.reverb(Y.lp(box, 3000), 3.0, 0.7, "l11box")[: len(t)] * 0.15
    x = Y.loopable(Y.norm(wind + traffic + hum + box, 0.42), 1.5)
    return Y.stereoize(x, 0.003), "stereo"


def siren():
    d = 6.0
    t = Y.t_axis(d)
    f0 = 700 + 250 * np.sin(2 * np.pi * 0.4 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(Y.lp(x, 1500), 4.0, 0.8, "l11sir"), 0.25)


def pigeons():
    out = np.zeros(int(2.5 * Y.SR))
    r = Y.rng("l11pig")
    for k in range(20):
        n = int(0.04 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.04, f"pg{k}"), 300, 2500)[:n] * np.exp(-tt * 60), 0.1 + k * 0.06 + r.random() * 0.02, 0.6)
    return Y.norm(Y.reverb(out, 2.0, 0.5, "l11pig"), 0.4)


def bells():
    d = 7.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for k, f in enumerate((392.0, 329.6, 349.2, 261.6)):
        n = int(3.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        bell = sum(np.sin(2 * np.pi * f * m * tt) * np.exp(-tt * (1.2 + m)) / m for m in (1, 2.4, 3.0, 4.2))
        Y.place(x, bell, 0.2 + k * 1.2, 0.6)
    return Y.norm(Y.reverb(Y.lp(x, 2500), 4.0, 0.7, "l11bell"), 0.35)


sound("ambient.level_11", [("ambient/level_11", city_loop)], stream=True)
sound("ambient.level_11.additions",
      [("ambient/level_11_siren", siren), ("ambient/level_11_pigeons", pigeons), ("ambient/level_11_bells", bells)],
      subtitle="The city breathes")

level("level_11", fog="#c8906a", sky="#d89060", ambient="ambient.level_11", additions="ambient.level_11.additions",
      additions_chance=0.003, fixed_time=12600, skylight=True, ceiling=False, height=128)
