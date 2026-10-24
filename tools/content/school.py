"""School After Hours - lockers, classrooms, a bell that rings whenever it wants. When it rings, class
is in session and the Hall Monitor walks the halls. Chalkboards remember how to leave."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, door, exit_block, flicker_light, stairs, level, sound, lang, \
    entity_model, entity_tex, part, ITEM_MODELS
import tex as T
import synth as Y
from content import afterhours_kit as K
from entity_paint import Canvas
from content.entities_core import humanoid, paint_humanoid

S = 32


def vct(accent=None):
    """Vinyl composition tile: four 16 px tiles per block, beige; accent blocks have one coloured tile."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("schv", sigma=1.2) * 0.5 + T.fbm("schvf", octaves=3, base=5) * 0.5, "#c6b89c", "#d4c8ae")
    for tx, ty in ([(1, 0)] if accent else []):
            col = accent
            if True:
                sub = T.ramp(T.noise(f"schvc{tx}{ty}", sigma=1.2, shape=(16, 16)), col, col)
                img[ty * 16:(ty + 1) * 16, tx * 16:(tx + 1) * 16] = sub * (0.9 + 0.2 * T.noise("schvcn", shape=(16, 16))[..., None])
    flecks = T.rng("schvfl").random((S, S))
    img = T.blend(img, T.solid("#5a5040"), (flecks > 0.95) * 0.4)
    img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.22)
    img = T.shade(img, 1 + np.clip(T.noise("schvs", sigma=4) - 0.62, 0, 1) * 0.3)
    return T.grain(img, "schv", 0.03)


def wall_upper():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("schw", octaves=3, base=4) * 0.6 + 0.2, "#d8cfb4", "#e2d9c0")
    off = ((yy // 8) % 2) * 8
    mortar = ((yy % 8) == 0) | (((xx + off) % 16) == 0)
    img = T.shade(img, 1 - mortar * 0.1)
    return T.grain(T.stains(img, "schws", "#a89878", count=1, strength=0.12, size=9), "schw", 0.03)


def wall_lower():
    img = wall_upper()
    img[14:] = T.ramp(T.noise("schwl", sigma=1.2, shape=(18, S)), "#6a2024", "#78262a")
    img[13] = T.hexc("#3a1214")
    yy, xx = np.mgrid[0:S, 0:S]
    off = ((yy // 8) % 2) * 8
    mortar = (((yy % 8) == 0) | (((xx + off) % 16) == 0)) & (yy > 14)
    img = T.shade(img, 1 - mortar * 0.12)
    return K.grime_bottom(T.grain(img, "schwl", 0.03), "schwlg", 8, 0.3)


def ceiling():
    base = T.ramp(T.fbm("schc", octaves=2, base=4), "#d0ccc0", "#dcd8cc")
    worms = (T.noise("schcw", sigma=0.6) > 0.8).astype(float)  # fissured acoustic tile
    img = T.shade(base, 1 - worms * 0.12)
    return T.grain(T.border(img, "#a8a498", 1), "schc", 0.02)


def troffer(on=True):
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        img = T.solid("#fdfcf2")
        prism = ((xx + yy) % 3 == 0).astype(float) * 0.05
        img = T.shade(img, 1 - prism)
    else:
        img = T.ramp(T.noise("schtro", sigma=2), "#6a6860", "#7a786e")
        img = T.shade(img, 1 - ((xx + yy) % 3 == 0) * 0.08)
        img = T.rect(img, 6, 4, 9, 28, "#4a4842")
    img = T.border(img, "#c8c6bc", 3)
    return T.border(img, "#8a887e", 1)


def locker(top, variant=0, open_=False):
    yy, xx = np.mgrid[0:S, 0:S]
    base = ["#3e6e8c", "#3a6684", "#46768e"][variant]
    img = T.ramp(T.noise(f"schl{variant}{top}", sigma=2), base, base)
    img = T.shade(img, 0.9 + T.noise(f"schln{variant}{top}", sigma=1.5) * 0.15)
    img = T.shade(img, 1 + np.clip(T.noise("schlsh", sigma=3) - 0.6, 0, 1) * 0.4)
    seam = (xx % 16 == 0) | (xx % 16 == 15)
    img = T.shade(img, 1 - seam * 0.45)
    if top:
        img[0:2] = T.hexc("#24404e")
        for v in range(5, 13, 2):  # vents
            img[v, 3:13] = T.hexc("#1a2c36")
            img[v, 19:29] = T.hexc("#1a2c36")
        img = T.rect(img, 5, 18, 11, 21, "#c8c4b0")
        img = T.rect(img, 21, 18, 27, 21, "#c8c4b0")
    else:
        img = T.rect(img, 11, 2, 13, 9, "#b8bcc0")  # handles
        img = T.rect(img, 27, 2, 29, 9, "#b8bcc0")
        for v in range(22, 28, 2):
            img[v, 3:13] = T.hexc("#1a2c36")
            img[v, 19:29] = T.hexc("#1a2c36")
        img = K.grime_bottom(img, f"schlg{variant}", 8, 0.35)
        dents = (T.noise(f"schld{variant}", sigma=0.8) > 0.88) & (yy > 10)
        img = T.shade(img, 1 - dents * 0.2)
    if open_:
        dark = T.ramp(T.noise("schlo", sigma=2), "#020203", "#08080a")
        img[:, 17:31] = dark[:, 17:31]
    return T.grain(img, f"schl{variant}{top}{open_}", 0.03)


def door_tex(top):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("schd" + str(top), octaves=3, base=3) * 0.5 + (np.sin(xx / 1.7 + T.noise("schdg", sigma=2) * 5) * 0.25 + 0.25),
                 "#9a6a3a", "#b07e48")
    img = T.border(img, "#5a3a1e", 1)
    if top:
        img = T.rect(img, 11, 4, 21, 28, "#2a2a28")
        win = T.ramp(T.noise("schdw", sigma=1, shape=(22, 8)), "#20262a", "#2e363a")
        img[5:27, 12:20] = win
        # wire glass
        for k in range(5, 27, 3):
            img[k, 12:20] = T.hexc("#4a4e50")
    else:
        img = T.rect(img, 23, 0, 27, 3, "#c0b080")
        img = K.grime_bottom(img, "schdgr", 6, 0.3)
    return T.grain(img, "schd" + str(top), 0.03)


def chalkboard_canvas(lines, seed, kind="text"):
    """A 128x64 green-black board with handwriting, smudges and an aluminium chalk tray frame."""
    h, w = 64, 128
    img = T.ramp(T.fbm(seed, octaves=3, base=6, shape=(h, w)), "#1e2e26", "#26382e")
    # erased smears
    sm = T.fbm(seed + "smear", octaves=2, base=8, shape=(h, w))
    img = T.blend(img, T.solid("#4a5a50", shape=(h, w)), np.clip(sm - 0.55, 0, 1) * 1.3)
    m = np.zeros((h, w))
    y = 6
    for ln in lines:
        scale = 2 if ln.startswith("!") else 1
        text = ln.lstrip("!")
        m = np.maximum(m, K.text_mask(text, h, w, 6, y, font=K.F35, scale=scale, spacing=1, jitter=0.6, seed=seed + ln))
        y += 6 * scale + 2
    if kind == "arrow":
        m = np.maximum(m, K.line_mask(h, w, 80, 46, 118, 46, 2))
        m = np.maximum(m, K.line_mask(h, w, 118, 46, 110, 39, 2))
        m = np.maximum(m, K.line_mask(h, w, 118, 46, 110, 53, 2))
    if kind == "map":
        rr = T.rng(seed + "map")
        for k in range(9):
            x0, y0 = 70 + int(rr.random() * 40), 8 + int(rr.random() * 40)
            m = np.maximum(m, K.line_mask(h, w, x0, y0, x0 + (12 if k % 2 else 0), y0 + (0 if k % 2 else 12), 1.2))
        m = np.maximum(m, K.text_mask("X", h, w, 104, 40, font=K.F57))
    if kind == "ghost":
        m = m * 0.22  # half erased, still readable if you look
    grit = T.noise(seed + "grit", sigma=0.3, shape=(h, w))
    m = m * (0.55 + grit * 0.45)
    img = T.blend(img, T.solid("#e8ecdc", shape=(h, w)), m)
    img[:3] = T.hexc("#9a9c98")
    img[-4:] = T.hexc("#8a8c88")
    img[-4:-2, ::9] = T.hexc("#f0f0e8")  # chalk stubs in the tray
    img[:, :2] = T.hexc("#9a9c98")
    img[:, -2:] = T.hexc("#9a9c98")
    return T.grain(img, seed, 0.03)


BOARDS = [
    (["!WHEN THE BELL", "RINGS GET IN A ROOM", "SHUT THE DOOR", "DONT LET IT SEE YOU"], "text"),
    (["!GREEN ARROWS", "ON THE FLOOR", "GO TO THE STAIRS", "THE STAIRS GO DOWN"], "arrow"),
    (["I WILL NOT LEAVE", "I WILL NOT LEAVE", "I WILL NOT LEAVE", "I WILL NOT LEAVE", "I WILL NOT LEAVE",
      "I WILL NOT LEAV"], "text"),
    (["!HOMEWORK", "FIND THE STAIRS", "DUE: NEVER"], "text"),
    (["!DONT RUN", "IN THE HALLS", "IT HEARS SHOES", "WAIT FOR THE 2ND BELL"], "text"),
    (["!MAP", "WE ARE HERE", "STAIRS = X", "COUNT THE", "ARROWS"], "map"),
    (["!ONE LOCKER", "IS OPEN", "IT IS NOT", "A LOCKER"], "text"),
    (["!SEE ME", "AFTER CLASS", "", "!SEE ME"], "ghost"),
]


def bell_tex():
    img = T.ramp(T.noise("schb", sigma=1.5), "#a81c1c", "#c42828")
    return T.grain(T.shade(img, 1 + np.clip(T.noise("schbs", sigma=2) - 0.6, 0, 1) * 0.6), "schb", 0.02)


def metal():
    return T.grain(T.ramp(T.noise("schm", sigma=1), "#6a6c6e", "#8a8c8e"), "schm", 0.03)


def desk_top():
    img = T.ramp(T.noise("schdt", sigma=2), "#c8a878", "#d4b484")
    r = T.rng("schdtc")
    for k in range(3):  # carved initials and doodles
        m = K.line_mask(S, S, *(r.random(4) * S), 0.8)
        img = T.blend(img, T.solid("#7a5a30"), m * 0.6)
    return T.grain(img, "schdt", 0.03)


def chair_blue():
    return T.grain(T.ramp(T.noise("schcb", sigma=2), "#2a4a7a", "#34588a"), "schcb", 0.02)


def arrow_tex():
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.zeros((S, S))
    a[(np.abs(xx - 15.5) < 3) & (yy > 12) & (yy < 28)] = 1
    tri = (yy >= 3) & (yy <= 13) & (np.abs(xx - 15.5) <= (yy - 3) * 0.9)
    a[tri] = 1
    worn = T.noise("scharw", sigma=0.8)
    a = a * (worn > 0.22)
    rgb = T.ramp(T.noise("scharc", sigma=1), "#2a8a3a", "#36a048")
    return rgb, a


def fountain_tex():
    img = T.ramp(T.noise("schft", sigma=1.5), "#b8bcbe", "#d4d8da")
    img = T.rect(img, 6, 6, 26, 12, "#8a8e90")
    return T.grain(img, "schft", 0.02)


def stair_tread():
    img = T.ramp(T.noise("schst", sigma=1.5), "#5a5a58", "#6a6a66")
    img[:4] = T.hexc("#c8b030")
    img[:4, ::3] = T.hexc("#3a3420")
    return T.grain(img, "schst", 0.04)


def stair_sign():
    img = T.solid("#1a3a6a")
    img = T.blend(img, T.solid("#f0f0f0"), K.centered("STAIRS", S, S, 6, font=K.F35))
    yy, xx = np.mgrid[0:S, 0:S]
    steps = np.zeros((S, S))
    for k in range(4):
        steps[(yy >= 16 + k * 3) & (yy < 18 + k * 3) & (xx >= 8 + k * 4) & (xx < 24)] = 1
    img = T.blend(img, T.solid("#f0f0f0"), steps)
    return T.border(img, "#c8c8c8", 1)


def drawing(seed):
    def f():
        img = wall_upper()
        paper = T.ramp(T.noise(seed + "p", sigma=2, shape=(18, 14)), "#f4f0e4", "#fcf8ec")
        r = T.rng(seed)
        m = np.zeros((18, 14))
        cols = ["#d02a2a", "#2a5ad0", "#e0b020", "#2a9a3a", "#1a1a1a"]
        # a house, a sun, a family. one of the family is taller than the house
        m2 = K.line_mask(18, 14, 2, 14, 7, 14, 1) + K.line_mask(18, 14, 2, 14, 2, 9, 1) + K.line_mask(18, 14, 7, 14, 7, 9, 1)
        m2 += K.line_mask(18, 14, 1, 9, 4.5, 6, 1) + K.line_mask(18, 14, 4.5, 6, 8, 9, 1)
        paper = T.blend(paper, T.solid(cols[int(r.random() * 3)], shape=(18, 14)), np.clip(m2, 0, 1))
        paper = T.blend(paper, T.solid("#e0b020", shape=(18, 14)), K.disc(18, 14, 11, 3, 1.8).astype(float))
        tall = K.line_mask(18, 14, 11, 16, 11, 5, 1) + K.disc(18, 14, 11, 4.5, 1.2)
        paper = T.blend(paper, T.solid("#1a1a1a", shape=(18, 14)), np.clip(tall, 0, 1))
        img[6:24, 9:23] = paper
        img = T.rect(img, 15, 5, 17, 7, "#c02020")
        return img
    return f


tex("sch_floor", vct)
tex("sch_floor_maroon", lambda: vct("#7a2a2e"))
tex("sch_floor_blue", lambda: vct("#2e4a6e"))
tex("sch_wall", wall_upper)
tex("sch_wall_lower", wall_lower)
tex("sch_ceiling", ceiling)
tex("sch_troffer", lambda: troffer(True))
tex("sch_troffer_off", lambda: troffer(False))
for v in range(3):
    tex(f"sch_locker_top_{v}", (lambda v=v: locker(True, v)))
    tex(f"sch_locker_bottom_{v}", (lambda v=v: locker(False, v)))
tex("sch_locker_open_top", lambda: locker(True, 0, True))
tex("sch_locker_open_bottom", lambda: locker(False, 0, True))
tex("sch_door_top", lambda: door_tex(True))
tex("sch_door_bottom", lambda: door_tex(False))
for n, (lines, kind) in enumerate(BOARDS):
    for p, t in enumerate(K.tiles(chalkboard_canvas(lines, f"schbd{n}", kind))):
        tex(f"sch_board_{n}_{p}", (lambda t=t: t))
tex("sch_bell", bell_tex)
tex("sch_metal", metal)
tex("sch_desk_top", desk_top)
tex("sch_chair_blue", chair_blue)
tex("sch_arrow", arrow_tex)
tex("sch_fountain", fountain_tex)
tex("sch_stair_tread", stair_tread)
tex("sch_stair_sign", stair_sign)
for n in range(3):
    tex(f"sch_drawing_{n}", drawing(f"schdr{n}"))

cube("sch_floor", "sch_floor", display="Vinyl Composition Tile", sound="stone")
cube("sch_floor_maroon", "sch_floor_maroon", display="Vinyl Composition Tile", sound="stone")
cube("sch_floor_blue", "sch_floor_blue", display="Vinyl Composition Tile", sound="stone")
cube("sch_wall", "sch_wall", display="Painted Cinder Block", sound="stone")
cube("sch_wall_lower", "sch_wall_lower", display="Painted Cinder Block (Maroon)", sound="stone")
cube("sch_ceiling", "sch_ceiling", display="Acoustic Tile", sound="wool", flat=True)
flicker_light("sch_troffer", "sch_troffer", "sch_troffer_off", "sch_ceiling", light=15, flicker=0.0,
              display="Troffer Light")
flicker_light("sch_troffer_flicker", "sch_troffer", "sch_troffer_off", "sch_ceiling", light=15, flicker=0.5,
              display="Flickering Troffer")
for v in range(3):
    # lockers only have a front: the classroom side of the wall is just wall
    shaped(f"sch_locker_top_{v}", [(0, 0, 0, 16, 16, 16, {"all": "wall", "north": "front"})],
           {"wall": "sch_wall", "front": f"sch_locker_top_{v}"}, display="Locker", sound="metal", unbreakable=True,
           render="solid")
    shaped(f"sch_locker_{v}", [(0, 0, 0, 16, 16, 16, {"all": "wall", "north": "front"})],
           {"wall": "sch_wall_lower", "front": f"sch_locker_bottom_{v}"}, display="Locker", sound="metal",
           unbreakable=True, render="solid")
exit_block("sch_locker_open", "sch_locker_open_bottom", "DOOR", display="Open Locker")
exit_block("sch_locker_open_top", "sch_locker_open_top", "DOOR", display="Open Locker")
door("sch_door", "sch_door_top", "sch_door_bottom", display="Classroom Door", unbreakable=True)
for n in range(len(BOARDS)):
    for p in range(8):
        cube(f"sch_board_{n}_{p}", f"sch_board_{n}_{p}", display="Chalkboard", sound="stone")
for n in range(3):
    cube(f"sch_drawing_{n}", f"sch_drawing_{n}", display="Child's Drawing", sound="stone")

shaped("sch_bell", [
    (4, 4, 0, 12, 12, 3, "bell"), (7, 7, 3, 9, 9, 4, "metal"), (6, 12, 0, 10, 14, 1, "metal"),
], {"bell": "sch_bell", "metal": "sch_metal"}, display="School Bell", unbreakable=True, collides=False)
shaped("sch_desk", [
    (1, 10, 0, 15, 11, 9, {"all": "metal", "up": "top"}),
    (2, 0, 1, 3, 10, 2, "metal"), (13, 0, 1, 14, 10, 2, "metal"),
    (3, 5, 10, 13, 6, 15, "chair"), (3, 6, 15, 13, 13, 16, "chair"),
    (3, 0, 14, 4, 5, 15, "metal"), (12, 0, 14, 13, 5, 15, "metal"), (3, 0, 10, 4, 5, 11, "metal"),
    (12, 0, 10, 13, 5, 11, "metal"),
], {"metal": "sch_metal", "top": "sch_desk_top", "chair": "sch_chair_blue"}, display="Student Desk",
    unbreakable=True)
shaped("sch_teacher_desk", [
    (0, 0, 2, 16, 12, 14, {"all": "wood", "up": "top"}),
], {"wood": "sch_door_bottom", "top": "sch_desk_top"}, display="Teacher's Desk", unbreakable=True)
shaped("sch_floor_arrow", [(1, 0, 1, 15, 0.25, 15, "arrow")], {"arrow": "sch_arrow"},
       display="Painted Floor Arrow", collides=False, unbreakable=True)
shaped("sch_drinking_fountain", [
    (3, 6, 0, 13, 12, 8, "steel"), (6, 0, 0, 10, 6, 4, "metal"),
], {"steel": "sch_fountain", "metal": "sch_metal"}, display="Drinking Fountain", kind="pickup",
    item="noclip:almond_water", sound="metal")
stairs("sch_stairs", "sch_stair_tread", "sch_wall", display="Stairwell Steps")
exit_block("sch_stairwell_dark", "pitch_black", "DESCEND", display="Down")
cube("sch_stair_sign", "sch_stair_sign", display="Stairs Sign", sound="metal", light=5, emissive=True)

# ------------------------------------------------------------------ the Hall Monitor


def monitor_tex():
    c = Canvas()
    skin = lambda w, h: T.ramp(T.noise(f"hmsk{w}{h}", shape=(h, w), sigma=1.5), "#b8ac98", "#c8bca8")

    def face(w, h):
        img = skin(w, h)
        img[3, 2] = img[3, 5] = T.hexc("#0a0806")
        img[0:2] = T.hexc("#3a3026")
        return img

    def head_side(w, h):
        img = skin(w, h)
        img[0:2] = T.hexc("#3a3026")
        return img

    def cardigan(w, h):
        img = T.ramp(T.noise(f"hmcd{w}{h}", shape=(h, w), sigma=0.6), "#5a4630", "#6a5238")
        yy, xx = np.mgrid[0:h, 0:w]
        sash = np.abs((xx - yy * (w / max(h, 1)))) < 1.6
        img[sash] = T.hexc("#e86a1a")
        return img

    def sleeve(w, h):
        img = T.ramp(T.noise(f"hmsl{w}{h}", shape=(h, w), sigma=0.6), "#5a4630", "#6a5238")
        img[-2:] = T.hexc("#b8ac98")
        return img

    def slacks(w, h):
        img = T.grain(T.solid("#2e2e34", shape=(h, w)), f"hmsl{w}{h}", 0.05)
        img[-2:] = T.hexc("#120e0a")
        return img

    paint_humanoid(c, head_side, cardigan, sleeve, slacks, face=face)
    return c.out()


entity_model("hall_monitor", humanoid(arm_len=15, leg_len=16))
entity_tex("hall_monitor", monitor_tex)
lang("entity.noclip.hall_monitor", "Hall Monitor")
lang("item.noclip.hall_monitor_spawn_egg", "Hall Monitor Spawn Egg")
ITEM_MODELS["hall_monitor_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def school_tone():
    d = 26.0
    t = Y.t_axis(d)
    rumble = Y.lp(Y.brown(d, "schrum"), 90) * 0.5
    hvac = Y.bp(Y.pink(d, "schhvac"), 150, 900) * 0.16 * (0.85 + 0.15 * np.sin(2 * np.pi * t / 13))
    hum = Y.lp(Y.hum(d, 60.0, "schhum", buzz=0.3, harmonics=(2, 4)), 1500) * 0.1
    knocks = np.zeros_like(t)
    for at in (3.1, 3.5, 11.2, 19.7, 20.0, 20.3):
        Y.place(knocks, Y.impact(f"schrad{at}", 0.25, ring=0.4), at, 0.25)
    knocks = Y.lp(knocks, 1200)
    x = rumble + hvac + hum + K.loop_reverb(knocks, 2.5, 0.6, "schkn")
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.0014), "stereo"


def locker_slam(i):
    def f():
        out = np.zeros(int(4 * Y.SR))
        Y.place(out, Y.impact(f"schls{i}", 0.5, ring=0.9), 0.05)
        return Y.norm(Y.reverb(Y.bp(out, 120, 5000), 3.5, 0.75, f"schls{i}"), 0.8)
    return f


def murmur():
    d = 5.0
    out = np.zeros(int(d * Y.SR))
    for k in range(9):
        v = K.pa_voice(f"schmur{k}", syllables=6 + k % 4, f0=210 + k * 23, rate=0.12, band=(300, 2500))
        Y.place(out, v, (k * 0.47) % 3.0, 0.3)
    t = Y.t_axis(d)
    out *= np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(Y.lp(out, 1500), 3.5, 0.85, "schmur"), 0.35)


def basketball():
    out = np.zeros(int(5 * Y.SR))
    at, gap = 0.1, 0.55
    for k in range(7):
        n = int(0.15 * Y.SR)
        tt = np.arange(n) / Y.SR
        b = np.sin(2 * np.pi * 95 * tt) * np.exp(-tt * 30) + Y.bp(Y.white(0.15, f"schbb{k}"), 300, 2000)[:n] * np.exp(-tt * 60) * 0.5
        Y.place(out, b, at, 1 - k * 0.1)
        at += gap
        gap *= 0.82
    return Y.norm(Y.reverb(Y.lp(out, 2000), 3.8, 0.8, "schbb"), 0.6)


def chalk():
    d = 1.6
    t = Y.t_axis(d)
    strokes = (np.sin(2 * np.pi * 2.5 * t) > 0.1).astype(float)
    f0 = 2400 + 600 * np.sin(2 * np.pi * 7 * t)
    sq = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * 0.4 + Y.bp(Y.white(d, "schch"), 3000, 9000) * 0.5
    return Y.norm(Y.reverb(sq * Y.lp(strokes, 30), 1.5, 0.5, "schch"), 0.5)


def bell():
    d = 5.0
    t = Y.t_axis(d)
    strike = (np.sin(2 * np.pi * 22 * t) > 0.6).astype(float)
    strike = np.diff(strike, prepend=0).clip(0, 1)
    x = np.zeros_like(t)
    ring = np.zeros(int(0.4 * Y.SR))
    tt = np.arange(len(ring)) / Y.SR
    for f, a in ((1310, 1.0), (2620 * 1.03, 0.5), (3900, 0.3), (5120, 0.2)):
        ring += np.sin(2 * np.pi * f * tt) * a * np.exp(-tt * 9)
    from scipy.signal import fftconvolve
    x = fftconvolve(strike * (t < 3.6), ring)[:len(t)]
    x += Y.bp(Y.white(d, "schbell"), 2000, 8000) * 0.05 * (t < 3.6)
    return Y.norm(Y.reverb(x, 2.5, 0.45, "schbell"), 0.95)


def whistle():
    d = 1.4
    t = Y.t_axis(d)
    trill = 1 + 0.06 * np.sign(np.sin(2 * np.pi * 34 * t))
    f0 = 2950 * trill
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) + Y.bp(Y.white(d, "hmwh"), 2500, 5000) * 0.3
    x *= np.minimum(t * 30, 1) * np.exp(-np.maximum(t - 1.0, 0) * 12)
    return Y.norm(Y.reverb(x, 2.0, 0.5, "hmwh"), 0.85)


def heel_steps():
    out = np.zeros(int(2.5 * Y.SR))
    for k in range(4):
        Y.place(out, Y.footstep(f"hmst{k}", "tile"), 0.1 + k * 0.55, 0.9)
    return Y.norm(Y.reverb(out, 2.5, 0.6, "hmst"), 0.8)


sound("ambient.school", [("ambient/school", school_tone)], stream=True)
sound("ambient.school.additions",
      [(f"ambient/school_locker{i}", locker_slam(i)) for i in range(2)] +
      [("ambient/school_murmur", murmur), ("ambient/school_basketball", basketball), ("ambient/school_chalk", chalk)],
      subtitle="The school is not empty")
sound("event.school_bell", [("event/school_bell", bell)], subtitle="The bell rings")
sound("entity.hall_monitor.whistle", [("entity/hall_monitor_whistle", whistle)], subtitle="A whistle")
sound("entity.hall_monitor.step", [("entity/hall_monitor_step", heel_steps)], subtitle="Hard shoes in the hall")

level("school", fog="#4a4438", sky="#4a4438", ambient="ambient.school", additions="ambient.school.additions",
      additions_chance=0.005, fixed_time=18000, height=48)
