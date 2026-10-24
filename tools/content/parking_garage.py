"""Parking Garage - low concrete decks under sodium light, rows of parked cars, ramps into the dark.
Somewhere a car alarm is going off. Something is drawn to it. The keys are in that car."""
import numpy as np
import assets as A
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, stairs, level, sound, BLOCK_MODELS
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32


def concrete(seed, lo="#6a6862", hi="#7c7a72", oil=0):
    img = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.7 + T.noise(seed + "f", sigma=0.5) * 0.3, lo, hi)
    pores = (T.rng(seed + "p").random((S, S)) > 0.93).astype(float)
    img = T.shade(img, 1 - pores * 0.15)
    if oil:
        img = T.stains(img, seed + "oil", "#1e1c1a", count=oil, strength=0.6, size=7)
    return T.grain(T.cracks(img, seed + "c", "#3e3c38", 1, 0.4), seed, 0.04)


def floor_line():
    img = concrete("pgfl")
    worn = T.noise("pgflw", sigma=0.8) > 0.3
    yy, xx = np.mgrid[0:S, 0:S]
    return T.blend(img, T.solid("#d8d4c4"), ((xx >= 14) & (xx < 18) & worn) * 0.85)


def floor_arrow():
    img = concrete("pgfa")
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((np.abs(xx - 15.5) < 2.5) & (yy > 12) & (yy < 28)) | ((yy >= 4) & (yy <= 13) & (np.abs(xx - 15.5) <= (yy - 4) * 1.0))
    worn = T.noise("pgfaw", sigma=0.8) > 0.25
    return T.blend(img, T.solid("#e8e4d4"), (a & worn) * 0.85)


def floor_yellow():
    img = concrete("pgfy")
    yy, xx = np.mgrid[0:S, 0:S]
    stripe = ((xx + yy) // 6) % 2 == 0
    worn = T.noise("pgfyw", sigma=0.8) > 0.25
    return T.blend(img, np.where(stripe[..., None], T.hexc("#d8b020"), T.hexc("#1a1a1a")), worn * 0.85)


def pillar(level_text, band):
    def f():
        img = concrete("pgp" + level_text, "#8a8880", "#9a988e")
        img[10:24] = T.ramp(T.noise("pgpb" + level_text, sigma=1, shape=(14, S)), band, band)
        img = T.blend(img, T.solid("#f0ece0"), K.centered(level_text, S, S, 11, font=K.F57, scale=1 if len(level_text) > 2 else 2))
        return img
    return f


def ceiling():
    yy, xx = np.mgrid[0:S, 0:S]
    img = concrete("pgc", "#4e4c48", "#5a5852")
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.2)
    drip = (T.noise("pgcd", sigma=1) > 0.85).astype(float)
    return T.blend(img, T.solid("#e8e4d8"), drip * 0.25)  # efflorescence


def beam():
    img = concrete("pgbm", "#5e5c56", "#6a6862")
    img[0] = T.hexc("#3a3834")
    return img


def tube(on):
    if on:
        img = T.solid("#ffd8a0")
        img[12:20] = T.hexc("#fff0d8")
    else:
        img = T.ramp(T.noise("pgto", sigma=2), "#5a564e", "#6a665e")
    return T.border(img, "#8a8a84", 3)


def wall():
    img = concrete("pgw", "#7a7870", "#8a887e")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 16) == 0) * 0.15)
    return K.grime_bottom(img, "pgwg", 12, 0.3)


def stair_door(top):
    img = T.ramp(T.noise("pgsd" + str(top), sigma=2), "#8a2a20", "#9a3226")
    img = T.border(img, "#3a3a3a", 2, sides="we" + ("n" if top else "s"))
    if top:
        img = T.rect(img, 9, 4, 23, 14, "#e8e4d4")
        img = T.blend(img, T.solid("#1a1a1a"), K.centered("STAIR", S, S, 7, font=K.F35))
    else:
        img = T.rect(img, 4, 2, 28, 6, "#9a9a92")
        img = K.grime_bottom(img, "pgsdg", 8, 0.3)
    return T.grain(img, "pgsd" + str(top), 0.04)


def call_box():
    img = T.ramp(T.noise("pgcb", sigma=1.5), "#2a4ab0", "#3458c0")
    img = T.blend(img, T.solid("#f0f0f0"), K.centered("HELP", S, S, 4, font=K.F35))
    img = T.rect(img, 10, 13, 22, 24, "#1a1a1a")
    img = T.rect(img, 13, 26, 19, 30, "#e02020")
    return img


def ramp_sign(text):
    def f():
        img = T.solid("#1a1a1a")
        img = T.blend(img, T.solid("#f0c030"), K.centered(text, S, S, 12, font=K.F35))
        return T.border(img, "#f0c030", 2)
    return f


tex("pgar_floor", lambda: concrete("pgf"))
tex("pgar_floor_oil", lambda: concrete("pgfo", oil=2))
tex("pgar_floor_line", floor_line)
tex("pgar_floor_arrow", floor_arrow)
tex("pgar_floor_hazard", floor_yellow)
for txt, band in (("P3", "#2a6ab0"), ("P-13", "#b02a2a")):
    tex(f"pgar_pillar_{txt.replace('-', 'm').lower()}", pillar(txt, band))
tex("pgar_ceiling", ceiling)
tex("pgar_beam", beam)
tex("pgar_tube", lambda: tube(True))
tex("pgar_tube_off", lambda: tube(False))
tex("pgar_wall", wall)
tex("pgar_stair_door_top", lambda: stair_door(True))
tex("pgar_stair_door_bottom", lambda: stair_door(False))
tex("pgar_call_box", call_box)
tex("pgar_sign_up", ramp_sign("UP"))
tex("pgar_sign_down", ramp_sign("DOWN"))

cube("pgar_floor", "pgar_floor", display="Garage Deck", sound="stone")
cube("pgar_floor_oil", "pgar_floor_oil", display="Oil-Stained Deck", sound="stone")
cube("pgar_floor_line", "pgar_floor_line", display="Stall Line", sound="stone")
cube("pgar_floor_arrow", "pgar_floor_arrow", display="Painted Arrow", sound="stone")
cube("pgar_floor_hazard", "pgar_floor_hazard", display="Hazard Paint", sound="stone")
cube("pgar_pillar_p3", "pgar_pillar_p3", display="Column P3", sound="stone")
cube("pgar_pillar_pm13", "pgar_pillar_pm13", display="Column P-13", sound="stone")
cube("pgar_ceiling", "pgar_ceiling", display="Deck Soffit", sound="stone", flat=True)
cube("pgar_beam", "pgar_beam", display="Concrete Beam", sound="stone", flat=True)
cube("pgar_wall", "pgar_wall", display="Garage Wall", sound="stone")
shaped("pgar_light", [(1, 13, 5, 15, 16, 11, {"all": "housing", "down": "tube"})],
       {"housing": "pgar_beam", "tube": "pgar_tube"}, display="Sodium Tube", light=13, emissive=True,
       unbreakable=True, collides=False, sound="glass", render="solid")
shaped("pgar_light_off", [(1, 13, 5, 15, 16, 11, {"all": "housing", "down": "tube"})],
       {"housing": "pgar_beam", "tube": "pgar_tube_off"}, display="Dead Tube", unbreakable=True, collides=False,
       sound="glass", render="solid")
exit_block("pgar_stair_door", "pgar_stair_door_bottom", "DOOR", trigger="USE", solid=True, display="Stair Door", sound="metal")
cube("pgar_stair_door_top", "pgar_stair_door_top", display="Stair Door", sound="metal")
shaped("pgar_call_box", [(4, 2, 0, 12, 14, 4, {"all": "metal", "south": "front"})],
       {"metal": "pgar_beam", "front": "pgar_call_box"}, display="Emergency Call Box", light=6, emissive=True,
       unbreakable=True, collides=False)
cube("pgar_sign_up", "pgar_sign_up", display="Ramp Sign", sound="metal", light=4, emissive=True)
cube("pgar_sign_down", "pgar_sign_down", display="Ramp Sign", sound="metal", light=4, emissive=True)
stairs("pgar_ramp", "pgar_floor", "pgar_floor", display="Ramp")
exit_block("pgar_dark_up", "pitch_black", "ASCEND", display="Up")
exit_block("pgar_dark_down", "pitch_black", "DESCEND", display="Down")
cube("pgar_curb", "pgar_floor_hazard", display="Curb", sound="stone")

# ------------------------------------------------------------------ cars


CARS = {"maroon": ("#5a1a1e", "#6e2228"), "navy": ("#1a2440", "#222e52"), "beige": ("#a89878", "#b8a888"),
        "silver": ("#8a8e92", "#a0a4a8"), "green": ("#1e3a2a", "#284a36"), "white": ("#c8c8c4", "#d8d8d4")}


def paint(lo, hi, seed):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.ramp(T.noise(seed, sigma=3), lo, hi)
        img = T.shade(img, 1 + np.clip(T.noise(seed + "sh", sigma=4) - 0.55, 0, 1) * 0.6)
        img = T.blend(img, T.solid("#7a6a5a"), (T.noise(seed + "dust", sigma=0.8) > 0.8) * 0.25)
        img[:, 15:16] = img[:, 15:16] * 0.7  # door seam
        return T.grain(img, seed, 0.02)
    return f


def car_glass(lit=False):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("pgcg" + str(lit), sigma=3), "#0c1014", "#141a20")
    img = T.blend(img, T.solid("#6a7a88"), np.clip(1 - np.abs((xx + yy * 0.8) % 28 - 9) / 3, 0, 1) * 0.35)
    if lit:
        img = T.blend(img, T.solid("#ffb040"), 0.15)
    return img


def tire():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("pgti", sigma=0.8), "#141414", "#202020")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.blend(img, T.solid("#8a8a8a"), ((d < 8) & (d > 5)) * 0.8)


def front(lit):
    img = T.solid("#1a1a1c")
    img = T.rect(img, 2, 10, 32 - 2, 14, "#8a8e92")
    col = "#fff8d0" if lit else "#c8c8b8"
    img = T.rect(img, 2, 4, 9, 10, col)
    img = T.rect(img, 23, 4, 30, 10, col)
    img = T.rect(img, 11, 5, 21, 9, "#3a3a3a")
    return img


def rear(lit):
    img = T.solid("#1a1a1c")
    col = "#ff3020" if lit else "#7a1a14"
    img = T.rect(img, 2, 4, 9, 10, col)
    img = T.rect(img, 23, 4, 30, 10, col)
    img = T.rect(img, 11, 6, 21, 11, "#d8d4c0")
    return img


for name, (lo, hi) in CARS.items():
    tex(f"pgar_paint_{name}", paint(lo, hi, "pgp" + name))
tex("pgar_car_glass", lambda: car_glass(False))
tex("pgar_car_glass_lit", lambda: car_glass(True))
tex("pgar_tire", tire)
tex("pgar_car_front", lambda: front(False))
tex("pgar_car_front_lit", lambda: front(True))
tex("pgar_car_rear", lambda: rear(False))
tex("pgar_car_rear_lit", lambda: rear(True))


def car_model(paint_tex, glass, fr, rr):
    """One block model spanning 2 x 3 blocks (x -6..22, z -16..32), nose to the north."""
    T_ = {"paint": "noclip:block/" + paint_tex, "glass": "noclip:block/" + glass, "tire": "noclip:block/pg_tire",
          "front": "noclip:block/" + fr, "rear": "noclip:block/" + rr, "particle": "noclip:block/" + paint_tex}

    def box(f, t, faces):
        return {"from": f, "to": t, "faces": {k: {"texture": "#" + v, "uv": [0, 0, 16, 16]} for k, v in faces.items()}}

    allp = {k: "paint" for k in ("north", "south", "east", "west", "up", "down")}
    els = [
        box([-6, 3, -15], [22, 11, 31], dict(allp, north="front", south="rear")),
        box([-4, 11, -2], [20, 18, 21], {"north": "glass", "south": "glass", "east": "glass", "west": "glass"}),
        box([-4.5, 18, -1], [20.5, 19, 20], dict(allp)),
        box([-6.5, 6, -16], [22.5, 8, -15], {k: "tire" for k in ("north", "east", "west", "up", "down")}),
        box([-6.5, 6, 31], [22.5, 8, 32], {k: "tire" for k in ("south", "east", "west", "up", "down")}),
    ]
    for zx in ((-12, -4), (19, 27)):
        for xs in ((-6.5, -3), (19, 22.5)):
            els.append(box([xs[0], 0, zx[0]], [xs[1], 7, zx[1]], {k: "tire" for k in ("north", "south", "east", "west", "up", "down")}))
    return {"parent": "minecraft:block/block", "textures": T_, "elements": els}


COLLIDE = [(0, 0, 0, 16, 16, 16, "paint")]
for name in CARS:
    shaped(f"pgar_car_{name}", COLLIDE, {"paint": f"pgar_paint_{name}"}, display="Parked Car", unbreakable=True,
           sound="metal")
    BLOCK_MODELS[f"pgar_car_{name}"] = car_model(f"pgar_paint_{name}", "pgar_car_glass", "pgar_car_front", "pgar_car_rear")

# the one with the alarm: lights flashing, keys in the ignition. Use it to get in.
exit_block("pgar_car_alarm", "pgar_paint_maroon", "SECRET", trigger="USE", solid=True, display="Car With Its Alarm Going",
           light=9, emissive=True, sound="metal",
           model=car_model("pgar_paint_maroon", "pgar_car_glass_lit", "pgar_car_front_lit", "pgar_car_rear_lit"))
A.ITEM_MODELS["pgar_car_alarm"] = {"parent": "noclip:block/pg_car_alarm"}

# ------------------------------------------------------------------ sound


def garage_tone():
    d = 28.0
    t = Y.t_axis(d)
    drone = Y.lp(Y.brown(d, "pgdr"), 70) * 0.6
    fans = Y.bp(Y.pink(d, "pgfan"), 90, 500) * 0.25 * (0.85 + 0.15 * np.sin(2 * np.pi * 3 * t / d))
    hum = Y.lp(Y.hum(d, 50.0, "pghum", buzz=0.2, harmonics=(2, 4)), 900) * 0.12
    drips = np.zeros_like(t)
    for at in (2.3, 9.8, 15.1, 22.6, 25.9):
        n = int(0.1 * Y.SR)
        tt = np.arange(n) / Y.SR
        drips[int(at * Y.SR):int(at * Y.SR) + n] += np.sin(2 * np.pi * np.cumsum(1600 - tt * 4000) / Y.SR) * np.exp(-tt * 40) * 0.3
    x = drone + fans + hum + K.loop_reverb(drips, 3.0, 0.7, "pgdrip")
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.0025), "stereo"


def alarm():
    """A 1990s car alarm: siren sweep, then a whoop, horn pulses. Mono, about 2 s, repeats."""
    d = 2.0
    t = Y.t_axis(d)
    f = np.where(t < 1.0, 700 + 900 * ((t * 4) % 1), 1300 - 500 * ((t * 3) % 1))
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)) * 0.6
    horn = (np.sin(2 * np.pi * 420 * t) + np.sin(2 * np.pi * 500 * t)) * ((t * 3) % 1 < 0.5) * (t > 1.4) * 0.4
    return Y.norm(Y.reverb(Y.bp(x + horn, 300, 4000), 2.5, 0.5, "pgalarm"), 0.9)


def tire_squeal():
    d = 2.5
    t = Y.t_axis(d)
    f = 900 + 200 * np.sin(2 * np.pi * 3 * t)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.4 + Y.bp(Y.white(d, "pgsq"), 1500, 4000) * 0.4
    x *= np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(x, 3.5, 0.75, "pgsq"), 0.5)


def car_door():
    out = np.zeros(int(3 * Y.SR))
    Y.place(out, Y.impact("pgcd", 0.5, ring=0.2), 0.05)
    return Y.norm(Y.reverb(Y.lp(out, 2500), 3.5, 0.8, "pgcd"), 0.7)


def engine():
    d = 5.0
    t = Y.t_axis(d)
    rpm = 28 * (1 + 0.3 * np.exp(-t * 2)) * (t < 4.0)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(rpm) / Y.SR)) * 0.3 + Y.lp(Y.white(d, "pgen"), 200) * 0.4
    x = Y.lp(x, 400) * np.minimum(t * 4, 1) * np.clip((4.2 - t) * 2, 0, 1)
    return Y.norm(Y.reverb(x, 3.0, 0.7, "pgen"), 0.5)


def footsteps_far():
    out = np.zeros(int(5 * Y.SR))
    for k in range(7):
        Y.place(out, Y.footstep(f"pgfs{k}", "concrete"), 0.2 + k * 0.5, 0.7)
    return Y.norm(Y.reverb(out, 3.5, 0.8, "pgfs"), 0.5)


def chirp():
    out = np.zeros(int(2.0 * Y.SR))
    for k in range(2):
        n = int(0.12 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, np.sign(np.sin(2 * np.pi * 2000 * tt)) * 0.4, 0.1 + k * 0.2)
    return Y.norm(Y.reverb(out, 3.0, 0.7, "pgch"), 0.5)


sound("ambient.parking_garage", [("ambient/parking_garage", garage_tone)], stream=True)
sound("ambient.parking_garage.additions",
      [("ambient/parking_garage_squeal", tire_squeal), ("ambient/parking_garage_door", car_door),
       ("ambient/parking_garage_engine", engine), ("ambient/parking_garage_steps", footsteps_far),
       ("ambient/parking_garage_chirp", chirp)],
      subtitle="Something in the garage")
sound("event.parking_garage_alarm", [("event/parking_garage_alarm", alarm)], subtitle="A car alarm")

level("parking_garage", fog="#2a2218", sky="#2a2218", ambient="ambient.parking_garage",
      additions="ambient.parking_garage.additions", additions_chance=0.005, fixed_time=18000, height=48)
