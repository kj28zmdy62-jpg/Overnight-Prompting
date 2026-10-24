"""Elevator Shaft - a vertical level. Climb ladders and maintenance platforms up shafts that never
end. Landing doors with the wrong floor numbers. Somewhere above or below, the car is moving."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, ladder, level, sound, custom, cross, lang, \
    entity_model, entity_tex, part, ITEM_MODELS, BLOCK_MODELS
from entity_paint import Canvas
from content.ind_kit import text, vstreaks, rust, steel, concrete, grate, box_model, facing_states, rivets
import tex as T
import synth as Y

S = 32


def shaft_wall(seed="elw"):
    yy, xx = np.mgrid[0:S, 0:S]
    img = concrete(seed, "#4e4f4c", "#62635e", stain="#2a2b28", stains=2)
    img = T.shade(img, 1 - ((yy % 32) == 0) * 0.2 - ((xx % 16) == 0) * 0.06)
    img = vstreaks(img, seed + "v", "#2a2622", count=4, strength=0.35, length=(12, 32))
    # tie-rod holes from the formwork
    for (x, y) in ((8, 8), (24, 8), (8, 24), (24, 24)):
        img[y - 1:y + 1, x - 1:x + 1] = T.hexc("#1e1e1c")
    return img


def wall_number(label):
    def f():
        img = shaft_wall("eln" + label)
        w = len(label) * 8 - 2
        img = text(img, label, 16 - w // 2, 9, "#c8c0a0", scale=2)
        return vstreaks(img, "elnv" + label, "#2a2622", 2, 0.3)
    return f


def landing_door(seed="eld", open_=False):
    yy, xx = np.mgrid[0:S, 0:S]
    if open_:
        img = T.ramp(T.noise("eldo", sigma=3), "#020202", "#0a0908")
        img = T.border(img, "#6a6e72", 2, "ew")
        return img
    img = steel(seed, "#7e8488", "#9ca2a6", brushed=True)
    img = T.shade(img, 1 - (np.abs(xx - 15.5) < 0.6) * 0.6)
    img = T.border(img, "#4a4e52", 1)
    img = vstreaks(img, seed + "v", "#5a5048", 3, 0.25, (8, 20))
    return T.stains(img, seed + "s", "#4a4038", 1, 0.3, 6)


def door_top():
    img = landing_door("eldt")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    img = np.where((yy < 5)[..., None], T.solid("#3a3e42"), img)
    return img


def grating():
    return grate("elg", "#3a3e42", "#58606a", pitch=4, bar=1, rust_amt=0.3)


def beam():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("elbeam", sigma=1.5), "#6a2a1a", "#8a3a24")
    img = T.shade(img, 1 - ((yy < 3) | (yy > 28)) * -0.15)
    img = rust(img, "elbeamr", 0.35, "#3a1a10", "#5a2a18")
    img = rivets(img, [(x, 5) for x in range(3, 32, 7)] + [(x, 26) for x in range(6, 32, 7)], "#a06a50", "#1a0a06")
    return T.grain(img, "elbeam", 0.04)


def rail():
    yy, xx = np.mgrid[0:S, 0:S]
    img = steel("elrail", "#5a6066", "#7a8086", brushed=False)
    img = T.shade(img, 1 - (np.abs(xx - 16) < 2) * -0.2)
    img = T.shade(img, 1 - ((yy % 16) == 0) * 0.3)
    return rust(img, "elrailr", 0.2)


def cable():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("elcab", sigma=0.6), "#1a1a1a", "#3a3a38")
    twist = (((yy + xx // 2) % 4) == 0)
    rgb = T.shade(rgb, 1 + twist * 0.3)
    a = np.zeros((S, S))
    for cx in (9, 15, 21):
        a[:, cx - 1:cx + 1] = 1
    return rgb, a


def lamp():
    img = T.ramp(T.noise("ellamp", sigma=2), "#ffe0a0", "#fff4d8")
    return img


def lamp_cage():
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx % 6) < 1) | ((yy % 6) < 1)).astype(float)
    return T.solid("#262624"), a


def call_panel():
    yy, xx = np.mgrid[0:S, 0:S]
    img = steel("elcall", "#8a9096", "#a4aab0", brushed=True)
    img = T.border(img, "#4a5056", 1)
    for cy, up in ((10, True), (21, False)):
        d = np.sqrt((xx - 16) ** 2 + (yy - cy) ** 2)
        img = T.blend(img, T.solid("#3a3a3a"), (d < 4.5).astype(float))
        tri = (np.abs(xx - 16) < (yy - cy + 3) * 0.8) & (yy > cy - 3) & (yy < cy + 2) if up else \
            (np.abs(xx - 16) < (cy + 3 - yy) * 0.8) & (yy < cy + 3) & (yy > cy - 2)
        img = T.blend(img, T.solid("#ffa030"), tri.astype(float))
    return img


def bottom():
    return T.solid("#000000")


tex("el_wall", lambda: shaft_wall())
for lab in ("13", "13A", "14", "B9", "112", "1203", "4", "0"):
    tex(f"el_num_{lab.lower()}", wall_number(lab))
tex("el_door", lambda: landing_door())
tex("el_door_top", door_top)
tex("el_door_open", lambda: landing_door(open_=True))
tex("el_grate", grating)
tex("el_beam", beam)
tex("el_rail", rail)
tex("el_cable", cable)
tex("el_lamp", lamp)
tex("el_lamp_cage", lamp_cage)
tex("el_call", call_panel)

cube("el_wall", "el_wall", display="Shaft Wall")
NUMBERS = ("13", "13A", "14", "B9", "112", "1203", "4", "0")
for lab in NUMBERS:
    cube(f"el_num_{lab.lower()}", f"el_num_{lab.lower()}", display=f"Floor {lab}")
cube("el_landing_door", "el_door", display="Landing Door", sound="metal")
cube("el_landing_door_top", "el_door_top", display="Landing Door", sound="metal")
exit_block("el_open_door", "el_door_open", "DOOR", display="Open Landing Door", sound="metal")
cube("el_grating", "el_grate", display="Platform Grating", sound="metal", render="cutout")
cube("el_beam", "el_beam", display="I-Beam", sound="metal")
shaped("el_rail", [(5, 0, 5, 11, 16, 11, "r"), (2, 0, 7, 14, 16, 9, "r")], {"r": "el_rail"},
       display="Guide Rail", sound="metal", unbreakable=True)
cross("el_cable", "el_cable", display="Hoist Ropes", sound="chain")
ladder("el_rungs", "el_rail", display="Rungs", unbreakable=True)
shaped("el_lamp", [(5, 3, 0, 11, 9, 4, {"all": "c", "south": "g"}), (6, 4, 0, 10, 8, 3, "g")],
       {"g": "el_lamp", "c": "el_lamp_cage"}, display="Shaft Lamp", light=13, emissive=True, collides=False,
       unbreakable=True, sound="glass")
exit_block("el_bottom", "pitch_black", "FALL", display="The Bottom")
custom("el_call", "com.noclip.block.CallButtonBlock",
       box_model([(5, 3, 0, 11, 13, 1, {"all": "c", "south": "c"})], {"c": "el_call"}),
       blockstate=facing_states(lambda v: "el_call"), display="Call Button", sound="metal", unbreakable=True,
       light=6, emissive=True, render="cutout")

# ------------------------------------------------------------------ entity: the car


def car_parts():
    # 60 x 48 x 60 px cab, ground at y=24
    return [part("body", (0, 0, 0), cubes=[(0, 0, -30, -24, -30, 60, 48, 60)])]


def car_tex():
    c = Canvas(256, 128)

    def side(w, h, door=False):
        base = np.tile(steel("elcarp", "#5a6066", "#767c82", brushed=True), (h // 32 + 1, w // 32 + 1, 1))[:h, :w]
        yy, xx = np.mgrid[0:h, 0:w]
        base = T.shade(base, 1 - ((xx % 20) == 0) * 0.25)
        for x in range(3, w, 6):
            base[2, x] = T.hexc("#a0a6ac")
            base[h - 3, x] = T.hexc("#a0a6ac")
        if door:
            mid = w // 2
            base = T.shade(base, 1 - (np.abs(xx - mid) < 1) * 0.7)
            base = T.blend(base, T.solid("#3a3e42", shape=(h, w)), ((xx < 8) | (xx > w - 9)).astype(float) * 0.5)
        grime = vstreaks(np.zeros((32, 32, 3)) + 0.5, "elcarg", "#000000", 6, 0.6)[..., 0]
        g = np.tile(grime, (h // 32 + 1, w // 32 + 1))[:h, :w]
        return T.shade(base, 0.6 + g * 0.8)

    def top(w, h):
        img = np.tile(grate("elcartop", "#3a3e42", "#50565c")[0], (h // 32 + 1, w // 32 + 1, 1))[:h, :w]
        yy, xx = np.mgrid[0:h, 0:w]
        hub = (np.abs(xx - w / 2) < 6) & (np.abs(yy - h / 2) < 6)
        return np.where(hub[..., None], T.solid("#202224", shape=(h, w)), img)

    def under(w, h):
        img = np.tile(T.ramp(T.noise("elcaru", sigma=1), "#1a1a1a", "#2a2a2a"), (h // 32 + 1, w // 32 + 1, 1))[:h, :w]
        return img

    c.box(0, 0, 60, 48, 60, all=lambda w, h: side(w, h), front=lambda w, h: side(w, h, True), up=top, down=under)
    return c.out()


def car_glow():
    """Light leaking from the cab: the gap between the doors, the car-top lamp."""
    c = Canvas(256, 128)

    def front(w, h):
        img = np.zeros((h, w, 3))
        mid = w // 2
        img[4:h - 2, mid - 1:mid + 1] = T.hexc("#fff0c0")
        img[h - 3:h - 1, 8:w - 8] = T.hexc("#ffe8b0") * 0.8
        return img

    def topg(w, h):
        img = np.zeros((h, w, 3))
        img[h // 2 - 2:h // 2 + 2, w // 2 - 2:w // 2 + 2] = T.hexc("#ffffff")
        return img

    c.box(0, 0, 60, 48, 60, front=front, up=topg)
    rgb, a = c.out()
    a = (rgb.sum(axis=2) > 0.1).astype(float)
    return rgb, a


entity_model("elevator_car", car_parts(), tex_size=(256, 128))
entity_tex("elevator_car", car_tex)
entity_tex("elevator_car_glow", car_glow)
lang("entity.noclip.elevator_car", "The Car")
lang("item.noclip.elevator_car_spawn_egg", "The Car Spawn Egg")
ITEM_MODELS["elevator_car_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def shaft_wind():
    d = 26.0
    t = Y.t_axis(d)
    howl = Y.bp(Y.pink(d, "elhowl"), 180, 420) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 13 + 1)) * 1.2
    high = Y.bp(Y.pink(d, "elhigh"), 900, 1300) * 0.25 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 6.5))
    rumble = Y.lp(Y.brown(d, "elrum"), 60) * 0.8
    hum = Y.hum(d, 50.0, "elhum", buzz=0.05, harmonics=(1, 2, 3)) * 0.06
    x = howl + high + rumble + Y.lp(hum, 400)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 2.0), 0.0025), "stereo"


def ding():
    d = 3.5
    t = Y.t_axis(d)
    x = np.zeros(len(t))
    for f, at in ((1318.5, 0.0), (1046.5, 0.35)):
        tt = np.clip(t - at, 0, None)
        x += np.sin(2 * np.pi * f * tt) * np.exp(-tt * 2.5) * (t >= at) + 0.3 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt * 6) * (t >= at)
    return Y.norm(Y.reverb(Y.lp(x, 2500), 4.0, 0.7, "elding"), 0.5)


def muzak():
    """Elevator music from somewhere far below, through steel and concrete."""
    d = 9.0
    t = Y.t_axis(d)
    chords = [(261.6, 329.6, 392.0, 493.9), (220.0, 261.6, 329.6, 392.0), (174.6, 220.0, 261.6, 329.6), (196.0, 246.9, 293.7, 349.2)]
    x = np.zeros(len(t))
    for i, ch in enumerate(chords):
        at = i * 2.2
        tt = np.clip(t - at, 0, None)
        env = (t >= at) * np.exp(-tt * 0.6) * (1 - np.exp(-tt * 20))
        for f in ch:
            x += (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 2 * tt)) * env * 0.25
    mel = [659.3, 587.3, 523.3, 493.9, 523.3, 587.3, 523.3, 440.0]
    for i, f in enumerate(mel):
        at = i * 1.1
        tt = np.clip(t - at, 0, None)
        x += np.sin(2 * np.pi * f * tt) * (t >= at) * np.exp(-tt * 3) * 0.35
    wobble = 1 + 0.004 * np.sin(2 * np.pi * 0.7 * t)
    x = np.interp(np.cumsum(wobble) - 1, np.arange(len(x)), x)
    x = Y.lp(x, 900) * np.clip(t / 1.5, 0, 1) * np.clip((d - t) / 2.0, 0, 1)
    return Y.norm(Y.reverb(x, 4.5, 0.75, "elmuz"), 0.35)


def cable_creak(i):
    def f():
        d = 2.5
        t = Y.t_axis(d)
        f0 = 180 + i * 40 + 30 * np.sin(2 * np.pi * 0.8 * t)
        x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (Y.rng(f"elcc{i}").random(len(t)) > 0.6) * np.sin(np.pi * t / d)
        return Y.norm(Y.reverb(Y.bp(x, 120, 2000), 3.0, 0.6, f"elcc{i}"), 0.5)
    return f


def cables_sing():
    """The ropes start to hum and slap: the car is coming."""
    d = 5.0
    t = Y.t_axis(d)
    env = np.clip(t / 4.0, 0, 1) ** 2
    hum = sum(np.sin(2 * np.pi * f * t + np.sin(2 * np.pi * 5 * t) * 0.5) * g for f, g in ((92, 1.0), (184, 0.5), (276, 0.3)))
    slap = Y.bp(Y.white(d, "elslap"), 300, 3000) * (np.sin(2 * np.pi * 7 * t) > 0.95) * 0.8
    x = (hum * 0.6 + slap) * env + Y.lp(Y.brown(d, "elsing"), 100) * env
    return Y.norm(Y.reverb(x, 2.5, 0.5, "elsing"), 0.8)


def car_rush():
    d = 2.0
    t = Y.t_axis(d)
    env = np.sin(np.pi * t / d) ** 1.5
    x = Y.lp(Y.brown(d, "elrush"), 200) * env * 2 + Y.bp(Y.white(d, "elrush2"), 200, 4000) * env * 0.8
    clack = np.zeros(len(t))
    for k in range(6):
        Y.place(clack, Y.impact(f"elclk{k}", 0.25, ring=0.5), 0.15 + k * 0.3, 0.5)
    return Y.norm(x + clack, 0.95)


sound("ambient.elevator_shaft", [("ambient/elevator_shaft", shaft_wind)], stream=True)
sound("ambient.elevator_shaft.additions",
      [("ambient/elevator_shaft_ding", ding), ("ambient/elevator_shaft_muzak", muzak)] +
      [(f"ambient/elevator_shaft_creak{i}", cable_creak(i)) for i in range(2)],
      subtitle="Something moves in the shaft")
sound("event.elevator_shaft_cables", [("event/elevator_shaft_cables", cables_sing)], subtitle="The hoist ropes sing")
sound("event.elevator_shaft_ding", [("event/elevator_shaft_ding", ding)], subtitle="A bell dings")
sound("entity.elevator_car.rush", [("entity/elevator_car_rush", car_rush)], subtitle="The car rushes past")

level("elevator_shaft", fog="#0e1114", sky="#0e1114", ambient="ambient.elevator_shaft",
      additions="ambient.elevator_shaft.additions", additions_chance=0.004, fixed_time=18000, height=256,
      particle="minecraft:white_ash", particle_chance=0.003)
