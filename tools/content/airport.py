"""Airport. An endless, empty terminal at night. Moving walkways carry nobody, departure boards list
flights that never board, planes wait dark on the apron. Sometimes a gate calls final boarding."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, exit_block, custom, flicker_light, level, sound
import tex as T
import synth as Y
from content.classics_kit import draw_text, centered, box_model, facing_states, metal

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def terrazzo():
    img = T.ramp(T.noise("apt", sigma=0.4), "#b8b4aa", "#c8c4ba")
    chips = T.rng("aptc").random((S, S))
    img = T.blend(img, T.solid("#6a6a64"), (chips > 0.93) * 0.7)
    img = T.blend(img, T.solid("#e8e4da"), (chips < 0.05) * 0.7)
    return T.grain(T.shade(img, 0.95 + T.noise("aptsh", sigma=3) * 0.1), "apt", 0.02)


def gate_carpet():
    img = T.ramp(T.noise("apgc", sigma=0.5), "#2a3a5a", "#324466")
    pat = ((xx % 8 == 0) & (yy % 8 == 0)) | ((xx % 8 == 4) & (yy % 8 == 4))
    return T.grain(T.blend(img, T.solid("#c8a030"), pat * 0.8), "apgc", 0.04)


def wall_panel():
    img = T.ramp(T.noise("apw", sigma=2), "#d8d8d4", "#e2e2de")
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.15)
    return T.grain(img, "apw", 0.02)


def ceiling():
    img = T.ramp(T.noise("apce", sigma=1), "#c8ccd0", "#d4d8dc")
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.2)
    return T.grain(img, "apce", 0.02)


def skylight(on):
    def f():
        img = T.solid("#e8f0ff" if on else "#5a6068")
        img = T.shade(img, 1 - (((xx % 8) == 0) | ((yy % 8) == 0)) * 0.3)
        return T.border(img, "#8a9098", 2)
    return f


def glass():
    rgb = T.solid("#9ab0c0")
    a = np.ones((S, S)) * 0.25
    a[(xx < 1) | (xx > 30)] = 1
    rgb[(xx < 1) | (xx > 30)] = T.hexc("#5a6068")
    return rgb, a


def walkway(direction):
    def f():
        img = metal("apwk", "#3a3c40", "#4a4c50", sigma=1)
        img = T.shade(img, 1 - ((xx % 3) == 0) * 0.3)
        # chevrons pointing along the direction of travel
        for k in range(2):
            y0 = 4 + k * 16
            for d in range(6):
                img = T.rect(img, 10 + d, y0 + d, 12 + d, y0 + d + 2, "#c8a030")
                img = T.rect(img, 20 - d, y0 + d, 22 - d, y0 + d + 2, "#c8a030")
        img = T.rect(img, 0, 0, 2, 32, "#c8a030")
        img = T.rect(img, 30, 0, 32, 32, "#c8a030")
        return img[::-1].copy() if direction == "n" else img
    return f


def rail():
    img = T.ramp(T.noise("aprl", sigma=1), "#1a1a1c", "#26262a")
    return T.rect(img, 0, 0, 32, 4, "#8a8c90")


def seat():
    return T.grain(T.ramp(T.noise("apst", sigma=0.6), "#2a4a7a", "#325486"), "apst", 0.04)


def chrome():
    return metal("apch", "#9a9ca0", "#b8bac0", sigma=1)


def board(rows, statuses):
    def f():
        img = T.solid("#06070a")
        for i, (r, st) in enumerate(zip(rows, statuses)):
            col = "#ffb020" if st != "BRD" else "#60ff90"
            img = draw_text(img, r, 2, 3 + i * 7, "#e8e8e0")
            img = draw_text(img, st, 19, 3 + i * 7, col)
        img = T.shade(img, 1 - ((yy % 7) == 0) * 0.3)
        return T.border(img, "#2a2c30", 1)
    return f


def gate_sign(n):
    def f():
        img = T.solid("#1a2a4a")
        img = centered(img, "GATE", 6, "#ffd040")
        img = centered(img, str(n), 15, "#ffffff", scale=2)
        return T.border(img, "#c8c8c8", 1)
    return f


def jetway(open_):
    def f():
        if open_:
            img = T.vgrad(S, S, "#1a1a12", "#000000")
            img = T.blend(img, T.solid("#ffd880"), np.clip(1 - np.abs(xx - 15.5) / 10, 0, 1) * np.clip((yy - 4) / 28, 0, 1) * 0.25)
            return T.border(img, "#5a6068", 2)
        img = metal("apjw", "#7a8088", "#8a9098", sigma=1)
        img = T.rect(img, 15, 0, 17, 32, "#3a3c40")
        img = T.rect(img, 3, 10, 29, 18, "#a01810")
        img = centered(img, "CLOSED", 11, "#ffffff")
        return img
    return f


def tarmac():
    img = T.ramp(T.noise("aptm", sigma=0.5) * 0.6 + T.fbm("aptmf", octaves=3, base=5) * 0.4, "#2a2a2c", "#363638")
    img = T.blend(img, T.solid("#1a1a1a"), (T.fbm("aptmo", octaves=3, base=3) > 0.62) * 0.5)
    return T.grain(img, "aptm", 0.04)


def taxi_line():
    img = tarmac()
    return T.blend(img, T.solid("#d8b020"), (np.abs(xx - 15.5) < 2) * 0.9)


def runway_light(col):
    def f():
        d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
        return T.ramp(np.clip(1 - d / 20, 0, 1), "#1a1a1a", col, "#ffffff")
    return f


def fuselage():
    img = T.ramp(T.noise("apfu", sigma=2), "#d8dce0", "#e8eaee")
    img = T.rect(img, 0, 20, 32, 24, "#2a4a8a")
    for x in range(2, 32, 6):
        img = T.rect(img, x, 9, x + 3, 13, "#14181e")
    return T.grain(img, "apfu", 0.02)


def fuselage_plain():
    img = T.ramp(T.noise("apfp", sigma=2), "#d8dce0", "#e8eaee")
    return T.grain(T.rect(img, 0, 20, 32, 24, "#2a4a8a"), "apfp", 0.02)


def wing():
    img = metal("apwg", "#a8acb0", "#b8bcc0", sigma=2)
    return T.shade(img, 1 - ((xx % 16) == 0) * 0.2)


def cockpit():
    img = fuselage_plain()
    for x0 in (4, 12, 20):
        img = T.rect(img, x0, 8, x0 + 7, 14, "#0a0e14")
    return img


def engine_front():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(T.solid("#b8bcc0"), T.solid("#141414"), (d < 12).astype(float))
    return T.blend(img, T.solid("#5a5e62"), (d < 3).astype(float))


TEX = {"ap_terrazzo": terrazzo, "ap_gate_carpet": gate_carpet, "ap_wall": wall_panel, "ap_ceiling": ceiling,
       "ap_skylight": skylight(True), "ap_skylight_off": skylight(False), "ap_glass": glass,
       "ap_walkway_n": walkway("n"), "ap_walkway_s": walkway("s"), "ap_rail": rail, "ap_seat": seat,
       "ap_chrome": chrome,
       "ap_board_a": board(["L0", "L4", "HOME", "L11"], ["CNX", "DLY", "CNX", "DLY"]),
       "ap_board_b": board(["HOME", "HOME", "HOME", "HOME"], ["CNX", "CNX", "CNX", "CNX"]),
       "ap_board_c": board(["L37", "L9", "???", "L13"], ["DLY", "CNX", "BRD", "DLY"]),
       "ap_jetway_closed": jetway(False), "ap_jetway_open": jetway(True), "ap_tarmac": tarmac,
       "ap_taxi_line": taxi_line, "ap_light_blue": runway_light("#3a6aff"), "ap_light_red": runway_light("#ff2a1a"),
       "ap_fuselage": fuselage, "ap_fuselage_plain": fuselage_plain, "ap_wing": wing, "ap_cockpit": cockpit,
       "ap_engine": engine_front}
for n in range(1, 10):
    TEX[f"ap_gate_{n}"] = gate_sign(n)
for k, fn in TEX.items():
    tex(k, fn)

cube("airport_terrazzo", "ap_terrazzo", display="Terrazzo", sound="tile")
cube("airport_gate_carpet", "ap_gate_carpet", display="Gate Carpet", sound="wool")
cube("airport_wall", "ap_wall", display="Terminal Wall")
cube("airport_ceiling", "ap_ceiling", display="Terminal Ceiling", flat=True)
flicker_light("airport_skylight", "ap_skylight", "ap_skylight_off", "ap_ceiling", light=15, flicker=0.01,
              display="Ceiling Light")
flicker_light("airport_skylight_bad", "ap_skylight", "ap_skylight_off", "ap_ceiling", light=15, flicker=0.6,
              display="Failing Ceiling Light")
pane("airport_glass", "ap_glass", edge="ap_wall", display="Terminal Glass", sound="glass", render="translucent")
custom("airport_walkway", "com.noclip.block.AirportWalkwayBlock",
       {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": "noclip:block/ap_walkway_n",
                                                                  "bottom": "noclip:block/ap_rail",
                                                                  "side": "noclip:block/ap_rail"}},
       blockstate={"variants": {"facing=north": {"model": "noclip:block/airport_walkway"},
                                "facing=east": {"model": "noclip:block/airport_walkway", "y": 90},
                                "facing=south": {"model": "noclip:block/airport_walkway", "y": 180},
                                "facing=west": {"model": "noclip:block/airport_walkway", "y": 270}}},
       display="Moving Walkway", sound="metal", unbreakable=True)
shaped("airport_walkway_rail", [(6, 0, 0, 10, 14, 16, {"all": "rail", "up": "top"})], {"rail": "ap_rail", "top": "ap_chrome"},
       display="Walkway Handrail", sound="metal", unbreakable=True)
shaped("airport_seats", [(0, 6, 2, 16, 7.5, 14, "seat"), (0, 7.5, 12, 16, 15, 14, "seat"), (7, 6, 2, 9, 13, 14, "chrome"),
                         (1, 0, 6, 2, 6, 10, "chrome"), (14, 0, 6, 15, 6, 10, "chrome")],
       {"seat": "ap_seat", "chrome": "ap_chrome"}, display="Gate Seating", sound="metal", unbreakable=True)
for k in "abc":
    shaped(f"airport_board_{k}", [(0, 0, 0, 16, 16, 2, {"all": "frame", "south": "board"})],
           {"frame": "ap_rail", "board": f"ap_board_{k}"}, display="Departures", light=8, emissive=True,
           collides=False, sound="glass", unbreakable=True)
for n in range(1, 10):
    shaped(f"airport_gate_sign_{n}", [(1, 4, 7, 15, 14, 9, {"all": "frame", "south": "sign", "north": "sign"}),
                                      (7.5, 14, 7.5, 8.5, 16, 8.5, "frame")],
           {"frame": "ap_chrome", "sign": f"ap_gate_{n}"}, display="Gate Sign", light=7, emissive=True,
           collides=False, sound="metal", unbreakable=True)
shaped("airport_gate_desk", [(0, 0, 3, 16, 14, 16, {"all": "wall", "up": "chrome"})], {"wall": "ap_seat", "chrome": "ap_chrome"},
       display="Gate Desk", sound="metal", unbreakable=True)
cube("airport_jetway_closed", "ap_jetway_closed", display="Jetway Door", sound="metal")
exit_block("airport_jetway_open", "ap_jetway_open", "DOOR", display="Open Jetway", light=6, emissive=True)
cube("airport_tarmac", "ap_tarmac", display="Tarmac")
cube("airport_taxi_line", "ap_taxi_line", display="Taxi Line")
shaped("airport_light_blue", [(6, 0, 6, 10, 3, 10, "l")], {"l": "ap_light_blue"}, display="Taxiway Light", light=8,
       emissive=True, collides=False, sound="glass", unbreakable=True)
flicker_light("airport_light_red", "ap_light_red", "ap_light_red", "ap_light_red", light=9, flicker=0.95,
              display="Beacon", face="all")
cube("airport_fuselage", "ap_fuselage", display="Fuselage", sound="metal")
cube("airport_fuselage_plain", "ap_fuselage_plain", display="Fuselage", sound="metal")
cube("airport_cockpit", "ap_cockpit", display="Cockpit", sound="metal")
cube("airport_wing", "ap_wing", display="Wing", sound="metal")
cube_faces("airport_engine", top="ap_wing", bottom="ap_wing", side="ap_engine", display="Engine", sound="metal")

# ------------------------------------------------------------------ sound


def terminal_loop():
    d = 30.0
    t = Y.t_axis(d)
    air = Y.lp(Y.pink(d, "apair"), 900) * 0.45
    room = Y.lp(Y.brown(d, "aproom"), 100) * 0.35
    pad = np.zeros_like(t)
    chords = [(261.6, 329.6, 392.0, 493.9), (220.0, 261.6, 329.6, 392.0), (174.6, 220.0, 261.6, 329.6),
              (196.0, 246.9, 293.7, 349.2)]
    for k in range(4):
        n = int(7.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        env = np.sin(np.pi * tt / 7.5) ** 2
        note = sum(np.sin(2 * np.pi * f * tt + i) for i, f in enumerate(chords[k])) * env
        Y.place(pad, note, k * 7.5, 0.25)
    pad = Y.reverb(Y.lp(pad, 1200), 4.0, 0.8, "appad")[: len(t)] * 0.25
    walk = Y.bp(Y.brown(d, "apwalk"), 40, 160) * 0.3 * (1 + 0.1 * np.sin(2 * np.pi * 3 * t))
    x = Y.loopable(Y.norm(air + room + pad + walk, 0.42), 1.5)
    return Y.stereoize(x, 0.003), "stereo"


def chime():
    d = 2.6
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for k, f in enumerate((523.3, 659.3, 784.0, 1046.5)):
        n = int(1.8 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(x, np.sin(2 * np.pi * f * tt) * np.exp(-tt * 2.5), 0.05 + k * 0.32, 0.6)
    return Y.reverb(x, 3.0, 0.5, "apch")


def announcement():
    d = 6.0
    x = np.zeros(int(d * Y.SR))
    c = chime()
    Y.place(x, c[: int(2.6 * Y.SR)], 0.0, 1.0)
    t = Y.t_axis(3.0)
    f0 = 165 + 25 * np.sin(2 * np.pi * 0.6 * t)
    voice = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)), 300, 2600)
    voice *= (np.sin(2 * np.pi * 3.4 * t) > -0.3) * np.clip(np.sin(np.pi * t / 3.0) * 4, 0, 1)
    voice += Y.whisper("apv", 3.0) * 0.6
    Y.place(x, Y.bp(voice, 400, 3000) * 0.6, 2.4, 1.0)
    return Y.norm(Y.reverb(x, 3.5, 0.6, "apann"), 0.6)


def jet():
    d = 8.0
    t = Y.t_axis(d)
    x = Y.bp(Y.pink(d, "apjet"), 200, 4000) * np.sin(np.pi * t / d) ** 2
    whine = np.sin(2 * np.pi * np.cumsum(800 + 2400 * (t / d)) / Y.SR) * 0.05 * np.sin(np.pi * t / d)
    return Y.norm(Y.lp(x + whine, 2500), 0.4)


def wheels():
    out = np.zeros(int(4 * Y.SR))
    for k in range(40):
        n = int(0.03 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.03, f"apwh{k}"), 800, 5000)[:n] * np.exp(-tt * 150), 0.2 + k * 0.08, 0.4)
    out *= np.sin(np.pi * np.arange(len(out)) / len(out))
    return Y.norm(Y.reverb(out, 3.0, 0.7, "apwh"), 0.35)


sound("ambient.airport", [("ambient/airport", terminal_loop)], stream=True)
sound("ambient.airport.additions",
      [("ambient/airport_announcement", announcement), ("ambient/airport_jet", jet),
       ("ambient/airport_wheels", wheels)],
      subtitle="The terminal echoes")
sound("event.airport_boarding", [("event/airport_boarding", announcement)], subtitle="Final boarding call")

level("airport", fog="#0a0e16", sky="#05070c", ambient="ambient.airport", additions="ambient.airport.additions",
      additions_chance=0.003, fixed_time=18000, skylight=True, ceiling=False, height=48)
