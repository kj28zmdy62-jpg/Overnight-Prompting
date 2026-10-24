"""The Red Room - one small deep-red room and a low tone that never stops. Every time you look away,
something in it is different. The door only opens for someone who will not look away from it."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, lang, entity_model, entity_tex, ITEM_MODELS
import tex as T
import synth as Y
from content.rb_shared import text, wood, line, disc, ellipse
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32


def damask():
    """Flocked red-on-red damask: the pattern only shows where the light catches the pile."""
    yy, xx = np.mgrid[0:S, 0:S]
    base = T.ramp(T.fbm("redwall", octaves=3, base=5), "#4a0608", "#5a0a0c")
    u = (xx % 16) - 8
    v = (yy % 32) - 16
    motif = (np.abs(u) * 1.6 + np.abs(v) * 0.7 < 9) & ~(np.abs(u) * 1.6 + np.abs(v) * 0.7 < 6)
    motif |= (np.abs(u) < 1) & (np.abs(v) < 12)
    motif |= ((u * u + (v + 10) ** 2) < 6)
    img = T.blend(base, T.solid("#6e1214"), motif.astype(float) * 0.85)
    pile = T.noise("redpile", sigma=0.4)
    return T.grain(T.shade(img, 0.9 + pile * 0.2), "redwall", 0.02)


def baseboard():
    img = damask()
    board = wood("redbb", "#1a0a08", "#2a1210", shape=(10, S), grain_dir="h")
    img[22:] = board
    img[22] = T.hexc("#3a1a14")
    return img


def carpet():
    fib = T.noise("redcarpf", sigma=0.35)
    img = T.ramp(T.fbm("redcarp", octaves=3, base=4) * 0.5 + fib * 0.5, "#3a0406", "#520a0c", "#5e0e10")
    return T.grain(img, "redcarp", 0.04)


def ceiling():
    return T.grain(T.ramp(T.fbm("redceil", octaves=3, base=5), "#240406", "#300608"), "redceil", 0.03)


def door(state, top):
    """Lacquered black-red door. Ajar shows a hairline of nothing; open is nothing at all."""
    yy, xx = np.mgrid[0:S, 0:S]
    if state == "open":
        img = T.solid("#000000")
        img = T.blend(img, T.solid("#100002"), T.noise("redopen" + str(top), sigma=3) * 0.8)
        img[:, :2] = T.hexc("#2a0a08")
        img[:, -2:] = T.hexc("#2a0a08")
        if top:
            img[:2] = T.hexc("#2a0a08")
        return img
    img = wood("reddoor" + str(top), "#2a0608", "#3a0a0c", grain_dir="v", rings=3)
    for (y0, y1) in (((5, 29),) if top else ((2, 24),)):
        for (x0, x1) in ((5, 14), (18, 27)):
            img[y0:y1, x0] = img[y0:y1, x0] * 0.6
            img[y0, x0:x1] = img[y0, x0:x1] * 0.6
            img[y0:y1, x1] = np.clip(img[y0:y1, x1] * 1.5, 0, 1)
    img[:, :2] = T.hexc("#140204")
    img[:, -2:] = T.hexc("#140204")
    if top:
        img[:2] = T.hexc("#140204")
    else:
        img = T.rect(img, 23, 2, 26, 6, "#b08a3a")
        img = T.rect(img, 24, 3, 25, 5, "#f0d080")
    if state == "ajar":
        img[:, 25:28] = T.hexc("#000000")
        img[:, 24] = img[:, 24] * 0.5
    return T.grain(img, "reddoor" + state + str(top), 0.02)


def velvet():
    img = T.ramp(T.noise("redvel", sigma=0.8), "#5a0810", "#7a1018")
    return T.grain(T.shade(img, 0.85 + T.noise("redvels", sigma=3) * 0.3), "redvel", 0.03)


def lacquer():
    img = T.ramp(T.noise("redlac", sigma=2.5), "#0e0606", "#1c0c0a")
    sheen = np.clip(1 - np.abs(((np.mgrid[0:S, 0:S][1] - np.mgrid[0:S, 0:S][0]) % 32) - 10) / 4.0, 0, 1) * 0.12
    return np.clip(img + sheen[..., None], 0, 1)


def shade():
    yy = np.mgrid[0:S, 0:S][0]
    img = T.ramp(np.clip(1 - np.abs(yy - 16) / 18.0, 0, 1), "#a01010", "#ff5a40")
    pleat = ((np.mgrid[0:S, 0:S][1] % 4) == 0).astype(float) * 0.1
    return T.grain(T.shade(img, 1 - pleat), "redshade", 0.02)


def bulb():
    img = T.ramp(T.noise("redbulb", sigma=2), "#ff4030", "#ff7a50")
    return img


def portrait(stage):
    """A woman in a black dress against red. 0: looking at you. 1: smiling. 2: turned away. 3: gone."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("redport", sigma=2.5), "#3a0608", "#520a0c")
    if stage < 3:
        body = ellipse(S, S, 16, 30, 10, 9)
        img[body] = T.hexc("#0a0606")
        neck = (np.abs(xx - 16) < 2) & (yy > 17) & (yy < 23)
        head = ellipse(S, S, 16, 13, 5, 6.5)
        if stage == 2:
            img[head] = T.hexc("#120806")  # the back of her head
            img[neck] = T.hexc("#c8a890")
        else:
            img[head | neck] = T.ramp(T.noise("redface", sigma=1), "#c8a890", "#dcbca4")[head | neck]
            hair = ellipse(S, S, 16, 10, 6, 5) & (yy < 10)
            img[hair] = T.hexc("#120806")
            img[12, 13:15] = T.hexc("#140a08")
            img[12, 18:20] = T.hexc("#140a08")
            if stage == 1:
                # her eyes have gone dark all the way through
                img[11:14, 12:16] = T.hexc("#050202")
                img[11:14, 17:21] = T.hexc("#050202")
            if stage == 0:
                img[16, 15:18] = T.hexc("#7a2020")
            else:
                for x in range(12, 21):
                    img[16 + int(round(1.4 * np.sin(np.pi * (x - 12) / 8))), x] = T.hexc("#200606")
                img[16, 12] = img[16, 20] = T.hexc("#200606")
    else:
        # only the shape of where she stood, the paint paler there
        ghost = ellipse(S, S, 16, 30, 10, 9) | ellipse(S, S, 16, 13, 5, 6.5)
        img[ghost] = img[ghost] * 1.25
    img = T.shade(img, 0.9 + T.noise("redbrush" + str(stage), sigma=0.6) * 0.15)
    img = T.border(img, "#7a5a20", 2)
    img = T.border(img, "#b8903a", 1)
    return T.grain(img, "redport" + str(stage), 0.02)


def bakelite():
    return T.grain(T.ramp(T.noise("redbak", sigma=2), "#0a0808", "#1a1614"), "redbak", 0.02)


def brass():
    return T.grain(T.ramp(T.noise("redbrass", sigma=1.5), "#6a4a1a", "#a8803a"), "redbrass", 0.03)


tex("red_damask", damask)
tex("red_baseboard", baseboard)
tex("red_carpet", carpet)
tex("red_ceiling", ceiling)
for st in ("closed", "ajar", "open"):
    for top in (False, True):
        tex(f"red_door_{st}_{'high' if top else 'low'}", (lambda a, b: lambda: door(a, b))(st, top))
tex("red_velvet", velvet)
tex("red_lacquer", lacquer)
tex("red_shade", shade)
tex("red_bulb", bulb)
for i in range(4):
    tex(f"red_portrait_{i}", (lambda k: lambda: portrait(k))(i))
tex("red_bakelite", bakelite)
tex("red_brass", brass)

cube("red_damask", "red_damask", display="Red Damask", sound="wool")
cube("red_baseboard", "red_baseboard", display="Red Damask", sound="wool")
cube("red_carpet", "red_carpet", display="Red Carpet", sound="wool")
cube("red_ceiling", "red_ceiling", display="Red Ceiling", sound="wool", flat=True)
cube("red_door_closed_low", "red_door_closed_low", display="Red Door", sound="wood")
cube("red_door_closed_high", "red_door_closed_high", display="Red Door", sound="wood")
cube("red_door_ajar_low", "red_door_ajar_low", display="Red Door", sound="wood")
cube("red_door_ajar_high", "red_door_ajar_high", display="Red Door", sound="wood")
exit_block("red_door_open_low", "red_door_open_low", "DOOR", display="Open Door")
exit_block("red_door_open_high", "red_door_open_high", "DOOR", display="Open Door")

# wingback chair, back to the south
shaped("red_chair", [
    (1, 4, 1, 15, 8, 14, "vel"), (1, 8, 12, 15, 24, 15, "vel"), (0, 4, 1, 3, 13, 14, "vel"), (13, 4, 1, 16, 13, 14, "vel"),
    (0, 17, 10, 3, 23, 15, "vel"), (13, 17, 10, 16, 23, 15, "vel"),
    (2, 0, 2, 4, 4, 4, "wood"), (12, 0, 2, 14, 4, 4, "wood"), (2, 0, 12, 4, 4, 14, "wood"), (12, 0, 12, 14, 4, 14, "wood"),
], {"vel": "red_velvet", "wood": "red_lacquer"}, display="Red Chair", unbreakable=True, sound="wool")

# a little table with a telephone, receiver on or off the hook
TABLE = [(1, 12, 1, 15, 14, 15, "wood"), (2, 0, 2, 4, 12, 4, "wood"), (12, 0, 2, 14, 12, 4, "wood"),
         (2, 0, 12, 4, 12, 14, "wood"), (12, 0, 12, 14, 12, 14, "wood")]
PHONE = [(4, 14, 5, 12, 17, 11, "bak"), (6, 17, 6, 10, 18, 10, "brass")]
shaped("red_phone_table", TABLE + PHONE + [(3, 18, 7, 13, 19.5, 9, "bak"), (3, 17, 7, 5, 19, 9, "bak"), (11, 17, 7, 13, 19, 9, "bak")],
       {"wood": "red_lacquer", "bak": "red_bakelite", "brass": "red_brass"}, display="Telephone Table", unbreakable=True)
shaped("red_phone_table_off", TABLE + PHONE + [(13, 2, 7, 15, 12, 9, "bak"), (12, 1, 6, 16, 3, 10, "bak")],
       {"wood": "red_lacquer", "bak": "red_bakelite", "brass": "red_brass"}, display="Telephone Table", unbreakable=True)

# floor lamp in one piece, two blocks tall (shade sits in the upper half of the model space)
shaped("red_lamp", [(5, 0, 5, 11, 1, 11, "brass"), (7, 1, 7, 9, 22, 9, "brass"),
                    (3, 20, 3, 13, 29, 13, {"all": "shade", "up": None, "down": None})],
       {"shade": "red_shade", "brass": "red_brass"}, display="Red Lamp", light=10, emissive=True, collides=False,
       unbreakable=True, sound="wool")
shaped("red_bulb", [(6, 10, 6, 10, 16, 10, "brass"), (6, 6, 6, 10, 10, 10, "bulb")],
       {"bulb": "red_bulb", "brass": "red_brass"}, display="Bare Bulb", light=7, emissive=True, collides=False,
       unbreakable=True, sound="glass")
for i in range(4):
    shaped(f"red_portrait_{i}", [(1, 0, 0, 15, 16, 1, {"all": "frame", "south": "face"})],
           {"face": f"red_portrait_{i}", "frame": "red_brass"}, display="Portrait", collides=False, unbreakable=True)

# ------------------------------------------------------------------ the Guest


def guest_tex():
    c = Canvas()
    black = lambda w, h: T.grain(T.ramp(T.noise(f"rgb{w}{h}", shape=(h, w), sigma=1), "#060404", "#120a0a"), f"rgb{w}", 0.03)

    def head(w, h):
        img = T.ramp(T.noise(f"rgh{w}{h}", shape=(h, w), sigma=1.5), "#d8ccc0", "#e8dcd0")
        img[:2] = T.hexc("#060404")
        return img

    def face(w, h):
        img = head(w, h)
        img[2:4, 1:3] = T.hexc("#060404")
        img[2:4, 5:7] = T.hexc("#060404")
        # a mouth far too wide, the red of the walls inside it
        img[5, 0:8] = T.hexc("#060404")
        img[6, 1:7] = T.hexc("#6e0a0c")
        img[7, 2:6] = T.hexc("#060404")
        return img

    def hand(w, h):
        img = black(w, h)
        img[-3:] = T.hexc("#d8ccc0")
        return img

    paint_humanoid(c, head, black, hand, black, face=face)
    return c.out()


entity_model("red_guest", humanoid(arm_len=17, leg_len=16))
entity_tex("red_guest", guest_tex)
lang("entity.noclip.red_guest", "The Guest")
lang("item.noclip.red_guest_spawn_egg", "The Guest Spawn Egg")
ITEM_MODELS["red_guest_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def tone():
    """The tone. Two low notes a quarter-tone apart, beating like a slow pulse, and a whine above."""
    d = 24.0
    t = Y.t_axis(d)
    f1, f2 = 55.0, round(57.5 * d) / d
    x = np.sin(2 * np.pi * f1 * t) + np.sin(2 * np.pi * f2 * t) * 0.9 + np.sin(2 * np.pi * 27.5 * t) * 0.6
    x += np.sin(2 * np.pi * 110.0 * t) * 0.25 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 12.0))
    whine = np.sin(2 * np.pi * round(7040 * d) / d * t) * 0.006
    room = Y.lp(Y.brown(d, "redroom"), 120) * 0.25
    out = Y.norm(Y.lp(x, 400) + whine + room, 0.55)
    return Y.stereoize(Y.loopable(out, 1.0), 0.0006), "stereo"


def exhale():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "redex"), 200, 1400) * np.clip(np.sin(np.pi * t / 1.6), 0, 1) * (t < 1.6)
    return Y.norm(Y.reverb(x, 0.6, 0.25, "redex"), 0.5)


def rustle():
    d = 1.4
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "redrus"), 1500, 7000) * (Y.lp(np.abs(Y.white(d, "redrus2")), 20) * 6)
    return Y.norm(x * np.sin(np.pi * t / d), 0.4)


def phone_far():
    """A telephone ringing in a room above. Twice. Then nothing."""
    d = 5.0
    t = Y.t_axis(d)
    ring = np.zeros_like(t)
    for start in (0.2, 2.6):
        seg = (t > start) & (t < start + 1.6)
        ring += seg * np.sin(2 * np.pi * 440 * t) * np.sign(np.sin(2 * np.pi * 20 * t)) * 0.5
        ring += seg * np.sin(2 * np.pi * 480 * t) * 0.3
    return Y.norm(Y.reverb(Y.lp(ring, 900), 1.8, 0.6, "redph"), 0.35)


def shift():
    """Something moved. You did not see what."""
    d = 1.0
    t = Y.t_axis(d)
    x = Y.lp(Y.white(d, "redsh"), 300) * np.exp(-((t - 0.3) ** 2) / 0.02)
    x += np.sin(2 * np.pi * 41 * t) * np.exp(-t * 4) * 0.5
    return Y.norm(x, 0.6)


def creak():
    d = 2.4
    t = Y.t_axis(d)
    f0 = 120 + 60 * t + 20 * np.sin(2 * np.pi * 2.2 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (Y.rng("redcr").random(len(t)) > 0.5)
    return Y.norm(Y.reverb(Y.bp(x, 200, 2500) * np.sin(np.pi * t / d), 1.2, 0.4, "redcr"), 0.6)


def caught():
    d = 2.5
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for f in (110.0, 116.5, 155.6, 164.8):
        x += np.sin(2 * np.pi * f * t + 2 * np.sin(2 * np.pi * 7 * t))
    x *= np.exp(-t * 1.5) * (1 - np.exp(-t * 60))
    x += Y.hp(Y.white(d, "redcau"), 2000) * np.exp(-t * 4) * 0.5
    return Y.norm(Y.reverb(x, 1.5, 0.4, "redcau"), 0.95), "stereo"


sound("ambient.red_room", [("ambient/red_room", tone)], stream=True)
sound("ambient.red_room.additions",
      [("ambient/red_room_exhale", exhale), ("ambient/red_room_rustle", rustle), ("ambient/red_room_phone", phone_far)],
      subtitle="Something in the room")
sound("event.red_room_shift", [("event/red_room_shift", shift)], subtitle="Something moved")
sound("event.red_room_creak", [("event/red_room_creak", creak)], subtitle="The door creaks")
sound("event.red_room_caught", [("event/red_room_caught", caught)], subtitle="It is right behind you")

level("red_room", fog="#1a0204", sky="#1a0204", ambient="ambient.red_room", additions="ambient.red_room.additions",
      additions_chance=0.004, fixed_time=18000, height=32)
