"""Level 13 - Infinite Apartments. 1970s hallways, every door apartment 13, every apartment the same.
If there is light under a door, somebody is home. Do not go in."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, door, exit_block, flicker_light, level, sound
import tex as T
import synth as Y
from content.classics_kit import centered, draw_text, metal

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def hall_carpet():
    base = T.ramp(T.noise("l13c", sigma=0.5) * 0.6 + T.fbm("l13cf", octaves=3, base=5) * 0.4, "#5a3410", "#6e4416")
    # a 70s geometric: interlocking orange rings
    u, v = (xx % 16) - 7.5, (yy % 16) - 7.5
    d = np.sqrt(u * u + v * v)
    base = T.blend(base, T.solid("#b8641a"), (np.abs(d - 5.5) < 1.1) * 0.8)
    base = T.blend(base, T.solid("#3a2208"), (np.abs(d - 2.5) < 0.8) * 0.7)
    wear = np.clip(T.fbm("l13cw", octaves=3, base=4) * 1.7 - 0.85, 0, 1)
    return T.grain(T.blend(base, T.solid("#3e2a14"), wear * 0.5), "l13c", 0.05)


def wallpaper(peel=False):
    def f():
        img = T.ramp(T.noise("l13w", sigma=1.5), "#a88a3a", "#b8984a")
        stripe = (xx % 8) < 3
        img = T.blend(img, T.solid("#7a5a24"), stripe * 0.55)
        img = T.blend(img, T.solid("#d8b868"), ((xx % 8) == 4) * 0.4)
        img = T.stains(img, "l13ws" + str(peel), "#5a4a20", 2, 0.35, 8)
        if peel:
            m = np.clip(T.fbm("l13pl", octaves=3, base=3) * 2.2 - 1.2, 0, 1)
            img = T.blend(img, T.ramp(T.noise("l13plc", sigma=0.7), "#8a8676", "#a29e8c"), m)
        return T.grain(img, "l13w" + str(peel), 0.04)
    return f


def baseboard():
    img = wallpaper()()
    img = T.rect(img, 0, 25, 32, 32, "#4a3018")
    img = T.rect(img, 0, 25, 32, 26, "#6a4826")
    return img


def popcorn():
    img = T.ramp(T.noise("l13pc", sigma=0.4), "#c8c2b0", "#dcd6c4")
    return T.grain(T.stains(img, "l13pcs", "#9a8a5a", 1, 0.3, 9), "l13pc", 0.05)


def dome(on):
    def f():
        d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
        img = popcorn()
        if on:
            glass = T.ramp(np.clip(1 - d / 13, 0, 1), "#e8c890", "#fff0d0")
        else:
            glass = T.ramp(np.clip(1 - d / 13, 0, 1), "#7a7466", "#8e887a")
        img = T.blend(img, glass, (d < 12).astype(float))
        img = T.blend(img, T.solid("#5a4a3a"), (np.abs(d - 12) < 0.8).astype(float))
        # dead flies in the dome
        for (x, y) in ((12, 14), (18, 17), (15, 20)):
            img[y, x] = T.hexc("#1a140e")
        return img
    return f


def apt_door(top, lit=False, label="13"):
    def f():
        img = T.ramp(T.noise("l13d" + label, sigma=2), "#2e4a36", "#365640")
        img = T.border(img, "#1a2a20", 2)
        if top:
            img = T.rect(img, 15, 10, 17, 12, "#c8b480")
            img = T.rect(img, 8, 18, 24, 27, "#c8b480")
            img = centered(img, label, 20, "#2a2010")
        else:
            img = T.rect(img, 22, 4, 26, 8, "#c8b480")
            img = T.rect(img, 23, 5, 25, 7, "#7a6a40")
            if lit:
                img = T.rect(img, 2, 29, 30, 32, "#ffd898")
                img = T.rect(img, 2, 28, 30, 29, "#b89050")
        return T.grain(img, "l13d" + label + str(top), 0.03)
    return f


def stair_door(top):
    def f():
        img = metal("l13sd", "#8a8a80", "#9a9a8e", sigma=2)
        img = T.border(img, "#4a4a44", 2)
        if top:
            img = T.rect(img, 6, 6, 26, 14, "#a02018")
            img = centered(img, "STAIRS", 8, "#f0e8d8")
            img = T.rect(img, 11, 18, 21, 28, "#1a1e22")
        else:
            img = T.rect(img, 4, 8, 28, 11, "#4a4a44")
        return img
    return f


def apt_floor():
    img = T.ramp(T.noise("l13af", sigma=0.6), "#a89a7a", "#b8aa88")
    return T.grain(T.stains(img, "l13afs", "#7a6a4a", 1, 0.3, 7), "l13af", 0.05)


def linoleum():
    img = T.solid("#d8d0b8")
    chk = ((xx // 8) + (yy // 8)) % 2 == 0
    img = T.blend(img, T.solid("#8a7a58"), chk * 1.0)
    img = T.shade(img, 0.9 + T.noise("l13lin", sigma=2) * 0.15)
    return T.grain(img, "l13lin", 0.03)


def apt_wall():
    img = T.ramp(T.fbm("l13aw", octaves=3, base=5), "#c8c0a8", "#d4ccb4")
    return T.grain(T.stains(img, "l13aws", "#a09070", 1, 0.2, 9), "l13aw", 0.03)


def window():
    img = T.solid("#06080e")
    # the building across the way: the same apartments, the same lit window
    img = T.rect(img, 3, 3, 29, 29, "#14141a")
    for wy in range(5, 28, 6):
        for wx in range(5, 28, 6):
            img = T.rect(img, wx, wy, wx + 3, wy + 4, "#0a0a0e")
    img = T.rect(img, 17, 11, 20, 15, "#ffd890")
    img = T.rect(img, 15, 0, 17, 32, "#d8d2c0")
    return T.border(img, "#e0dac8", 2)


def stove():
    img = T.ramp(T.noise("l13st", sigma=2), "#d8c070", "#e0c878")
    img = T.rect(img, 4, 10, 28, 28, "#2a2620")
    img = T.rect(img, 6, 12, 26, 26, "#3a342c")
    for x in (6, 12, 18, 24):
        img = T.rect(img, x, 3, x + 3, 6, "#1a1a1a")
    return img


def stove_top():
    img = T.ramp(T.noise("l13stt", sigma=2), "#d8c070", "#e0c878")
    for (cx, cy) in ((9, 9), (23, 9), (9, 23), (23, 23)):
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        img = T.blend(img, T.solid("#1a1a1a"), ((np.abs(d - 4) < 1) | (np.abs(d - 2) < 0.7)).astype(float))
    return img


def fridge():
    img = T.ramp(T.noise("l13fr", sigma=2), "#c8b060", "#d4bc6a")
    img = T.rect(img, 0, 11, 32, 12, "#6a5a30")
    img = T.rect(img, 26, 3, 28, 9, "#9a9a92")
    img = T.rect(img, 26, 14, 28, 26, "#9a9a92")
    img = T.rect(img, 6, 16, 12, 22, "#f0e8d0")
    img = draw_text(img, "13", 6, 17, "#a02018")
    return img


def lampshade():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 24) ** 2)
    return T.ramp(np.clip(1 - d / 30, 0, 1), "#d88a40", "#ffd090", "#fff2d4")


def lampshade_off():
    return T.ramp(T.noise("l13lo", sigma=2), "#9a8460", "#aa9470")


def lamp_pole():
    return metal("l13lp", "#8a7040", "#a88850")


TEX = {"l13_carpet": hall_carpet, "l13_wallpaper": wallpaper(), "l13_wallpaper_peel": wallpaper(True),
       "l13_baseboard": baseboard, "l13_popcorn": popcorn, "l13_dome": dome(True), "l13_dome_off": dome(False),
       "l13_door_top": apt_door(True), "l13_door_bottom": apt_door(False), "l13_door_bottom_lit": apt_door(False, True),
       "l13_stair_top": stair_door(True), "l13_stair_bottom": stair_door(False), "l13_apt_floor": apt_floor,
       "l13_linoleum": linoleum, "l13_apt_wall": apt_wall, "l13_window": window, "l13_stove": stove,
       "l13_stove_top": stove_top, "l13_fridge": fridge, "l13_shade": lampshade, "l13_shade_off": lampshade_off,
       "l13_lamp_pole": lamp_pole}
for k, fn in TEX.items():
    tex(k, fn)

cube("apartment_hall_carpet", "l13_carpet", display="Hallway Carpet", sound="wool")
cube("apartment_wallpaper", "l13_wallpaper", display="Hallway Wallpaper", sound="wool")
cube("apartment_wallpaper_peeling", "l13_wallpaper_peel", display="Peeling Wallpaper", sound="wool")
cube("apartment_baseboard", "l13_baseboard", display="Hallway Baseboard", sound="wool")
cube("apartment_popcorn_ceiling", "l13_popcorn", display="Popcorn Ceiling", flat=True)
flicker_light("apartment_dome_light", "l13_dome", "l13_dome_off", "l13_popcorn", light=13, flicker=0.12,
              display="Dome Light")
flicker_light("apartment_dome_light_bad", "l13_dome", "l13_dome_off", "l13_popcorn", light=13, flicker=0.8,
              display="Failing Dome Light")
cube_faces("apartment_dome_light_dead", bottom="l13_dome_off", top="l13_popcorn", side="l13_popcorn", flat=True,
           display="Dead Dome Light", sound="glass")
door("apartment_door", "l13_door_top", "l13_door_bottom", display="Apartment 13", unbreakable=True)
door("apartment_door_lit", "l13_door_top", "l13_door_bottom_lit", display="Apartment 13", unbreakable=True)
exit_block("apartment_stair_door", "l13_stair_bottom", "DESCEND", trigger="USE", solid=True, display="Stairwell Door",
           sound="metal")
exit_block("apartment_stair_door_top", "l13_stair_top", "ASCEND", trigger="USE", solid=True, display="Stairwell Door",
           sound="metal")
cube("apartment_floor", "l13_apt_floor", display="Apartment Carpet", sound="wool")
cube("apartment_linoleum", "l13_linoleum", display="Linoleum", sound="tile")
cube("apartment_wall", "l13_apt_wall", display="Apartment Wall")
cube("apartment_window", "l13_window", display="Window Across The Way", sound="glass")
shaped("apartment_stove", [(1, 0, 1, 15, 15, 15, {"all": "side", "up": "top", "south": "front"})],
       {"side": "l13_fridge", "top": "l13_stove_top", "front": "l13_stove"}, display="Stove", sound="metal",
       unbreakable=True)
shaped("apartment_fridge", [(1, 0, 1, 15, 16, 15, {"all": "side", "south": "front"})],
       {"side": "l13_stove_top", "front": "l13_fridge"}, display="Fridge", sound="metal", unbreakable=True)
shaped("apartment_lamp", [(6, 0, 6, 10, 1, 10, "pole"), (7.5, 1, 7.5, 8.5, 12, 8.5, "pole"), (4, 11, 4, 12, 16, 12, "shade")],
       {"pole": "l13_lamp_pole", "shade": "l13_shade"}, display="Floor Lamp", light=12, emissive=True,
       collides=False, sound="metal", unbreakable=True)
shaped("apartment_lamp_off", [(6, 0, 6, 10, 1, 10, "pole"), (7.5, 1, 7.5, 8.5, 12, 8.5, "pole"),
                              (4, 11, 4, 12, 16, 12, "shade")],
       {"pole": "l13_lamp_pole", "shade": "l13_shade_off"}, display="Floor Lamp", collides=False, sound="metal",
       unbreakable=True)

# ------------------------------------------------------------------ sound


def hall_loop():
    d = 26.0
    t = Y.t_axis(d)
    hum = Y.hum(d, 60.0, "l13hum", buzz=0.3, harmonics=(1, 2, 3)) * 0.12
    # muffled television through the walls: formant babble plus a laugh track swell
    tv = np.zeros_like(t)
    for k in range(int((d - 1.5) / 1.6)):
        Y.place(tv, Y.whisper(f"l13tv{k}", 1.4), k * 1.6, 1.0)
    laugh = Y.bp(Y.white(d, "l13lt"), 400, 2000) * np.clip(np.sin(2 * np.pi * t * 3 / d), 0, 1) ** 4 * 0.4
    tv = Y.lp(tv + laugh, 600) * 0.6
    room = Y.lp(Y.brown(d, "l13room"), 150) * 0.4
    pipes = np.zeros_like(t)
    Y.place(pipes, Y.lp(Y.impact("l13pipe", 0.6, ring=0.4), 900), 17.0, 0.2)
    x = Y.loopable(Y.norm(hum + tv + room + pipes, 0.42), 1.0)
    return Y.stereoize(x, 0.0015), "stereo"


def slam():
    out = np.zeros(int(3 * Y.SR))
    Y.place(out, Y.impact("l13slam", 1.0), 0.05)
    return Y.norm(Y.reverb(Y.lp(out, 1200), 2.5, 0.6, "l13slam"), 0.6)


def argument():
    d = 4.0
    x = (Y.whisper("l13ar1", d) + np.roll(Y.whisper("l13ar2", d), int(0.7 * Y.SR))) * 1.2
    t = Y.t_axis(d)
    f0 = 140 + 40 * np.sin(2 * np.pi * 0.9 * t)
    v = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)), 300, 1500) * (np.sin(2 * np.pi * 2.3 * t) > 0) * 0.3
    return Y.norm(Y.lp(x + v, 700), 0.4)


def flush():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "l13fl"), 300, 3000) * np.exp(-((t - 1.5) / 1.0) ** 2)
    x += Y.lp(Y.brown(d, "l13fl2"), 300) * np.exp(-((t - 2.2) / 1.2) ** 2)
    return Y.norm(Y.lp(x, 1200), 0.35)


def door_open():
    d = 1.8
    t = Y.t_axis(d)
    creak = np.sin(2 * np.pi * np.cumsum(300 + 200 * np.sin(2 * np.pi * 0.8 * t)) / Y.SR) * np.sin(np.pi * t / d)
    creak *= 1 + np.sign(np.sin(2 * np.pi * 40 * t))
    return Y.norm(Y.reverb(Y.bp(creak, 200, 2500), 1.5, 0.4, "l13do"), 0.6)


sound("ambient.level_13", [("ambient/level_13", hall_loop)], stream=True)
sound("ambient.level_13.additions",
      [("ambient/level_13_slam", slam), ("ambient/level_13_argument", argument), ("ambient/level_13_flush", flush)],
      subtitle="Somebody in another apartment")
sound("event.level_13_home", [("event/level_13_home", door_open)], subtitle="Somebody is home")

level("level_13", fog="#3a2a14", sky="#3a2a14", ambient="ambient.level_13", additions="ambient.level_13.additions",
      additions_chance=0.006, fixed_time=18000, height=32)
