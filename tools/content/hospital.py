"""Hospital Ward - low flickering corridors, wards of curtained bays you can't see into, heart monitors
that beep in time with your own pulse. When your heart races, the patients can hear it."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, level, sound, lang, \
    entity_model, entity_tex, ITEM_MODELS
import tex as T
import synth as Y
from content import afterhours_kit as K
from entity_paint import Canvas
from content.entities_core import humanoid, paint_humanoid

S = 32


def sheet_vinyl(seed="hspf", stripe=None, vertical=False):
    img = T.ramp(T.fbm(seed, octaves=3, base=6) * 0.5 + T.noise(seed + "n", sigma=0.6) * 0.5, "#a9b2a8", "#b9c2b8")
    fl = T.rng(seed + "fl").random((S, S))
    img = T.blend(img, T.solid("#6a746a"), (fl > 0.94) * 0.5)
    img = T.blend(img, T.solid("#e4ece4"), (fl < 0.04) * 0.5)
    img = T.shade(img, 1 + np.clip(T.noise(seed + "sh", sigma=4) - 0.6, 0, 1) * 0.35)
    if stripe:
        yy, xx = np.mgrid[0:S, 0:S]
        band = ((xx if vertical else yy) >= 12) & ((xx if vertical else yy) < 20)
        worn = T.noise(seed + "sw", sigma=0.8) > 0.18
        img = T.blend(img, T.ramp(T.noise(seed + "sc", sigma=1), stripe, stripe), (band & worn) * 0.9)
    img = T.stains(img, seed + "st", "#7a7a5a", count=1, strength=0.12, size=8)
    return T.grain(img, seed, 0.03)


def wall_mint():
    img = T.ramp(T.fbm("hspw", octaves=4, base=4) * 0.6 + 0.2, "#a2c4b2", "#b2d2c0")
    img = T.stains(img, "hspws", "#7e9a88", count=1, strength=0.15, size=10)
    return T.grain(img, "hspw", 0.03)


def wall_rail():
    """Lower wall: mint paint, a steel crash rail, scuffed kickplate."""
    img = wall_mint()
    img[8:14] = T.ramp(T.noise("hsprail", sigma=1, shape=(6, S)), "#9aa2a6", "#c8d0d4")
    img[8] = T.hexc("#e0e6ea")
    img[13] = T.hexc("#5a6266")
    img[24:] = T.ramp(T.noise("hspkick", sigma=1, shape=(8, S)), "#7c8a80", "#8a988e")
    img[24] = T.hexc("#5a665c")
    r = T.rng("hspscuff")
    for k in range(5):  # gurney scuffs
        y, x = 15 + int(r.random() * 8), int(r.random() * 26)
        img[y, x:x + 4 + int(r.random() * 6)] = T.hexc("#4a524c")
    return T.grain(img, "hspwr", 0.03)


def ceiling():
    base = T.ramp(T.fbm("hspc", octaves=2, base=4), "#d8dcd6", "#e2e6e0")
    pits = (T.rng("hspcp").random((S, S)) > 0.88).astype(float)
    img = T.shade(base, 1 - pits * 0.08)
    return T.grain(T.border(img, "#a6aaa4", 1), "hspc", 0.02)


def ceiling_track():
    img = ceiling()
    img[14:18] = T.hexc("#b8bcbe")
    img[15] = T.hexc("#7a7e80")
    return img


def troffer(on):
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        img = T.solid("#eefcf4")
        img = T.shade(img, 1 - (((xx // 4) + (yy // 4)) % 2) * 0.03)
    else:
        img = T.ramp(T.noise("hsptro", sigma=2), "#5e6a64", "#6e7a74")
        img = T.rect(img, 6, 3, 9, 29, "#3e4844")
        img = T.rect(img, 23, 3, 26, 29, "#3e4844")
    img = T.border(img, "#c6cac4", 3)
    return T.border(img, "#8a8e88", 1)


def curtain(top):
    yy, xx = np.mgrid[0:S, 0:S]
    folds = 0.5 + 0.5 * np.sin(xx / 32 * 2 * np.pi * 4 + 0.6 * np.sin(yy / 9.0))
    img = T.ramp(folds * 0.6 + T.noise("hspcu" + str(top), sigma=1) * 0.4, "#7fa6a4", "#a6c8c4")
    # small printed pattern
    dots = ((xx % 8 == 3) & (yy % 8 == 3)) | ((xx % 8 == 7) & (yy % 8 == 7))
    img = T.blend(img, T.solid("#5e8684"), dots * 0.6)
    a = np.ones((S, S))
    if top:
        img[:3] = T.hexc("#c0c4c6")
        for x in range(2, S, 6):  # the rings on the track
            img[2:5, x:x + 2] = T.hexc("#8a8e90")
    else:
        img = K.grime_bottom(img, "hspcug", 10, 0.3)
        a[29:] = 0  # a gap at the floor. you can see feet, if there are feet
        img[28] = T.hexc("#5e7a78")
    return T.grain(img, "hspcu" + str(top), 0.03), a


def sheet_white(stained=False):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("hspsh" + str(stained), octaves=3, base=4), "#d8dcd8", "#eef0ec")
    img = T.shade(img, 1 - (np.sin(xx / 3.0 + yy / 7.0) > 0.85) * 0.06)
    if stained:
        img = T.stains(img, "hspshs", "#a8946a", count=2, strength=0.45, size=7)
    return T.grain(img, "hspsh" + str(stained), 0.02)


def blanket():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("hspbl", sigma=1), "#8aa8c8", "#9ab8d4")
    img = T.shade(img, 1 - (((xx % 4) == 0) | ((yy % 4) == 0)) * 0.12)
    return T.grain(img, "hspbl", 0.03)


def steel():
    yy, xx = np.mgrid[0:S, 0:S]
    from scipy.ndimage import uniform_filter1d
    br = uniform_filter1d(T.noise("hspst", sigma=0), 7, axis=1, mode="wrap")
    return T.grain(T.ramp(br * 2.2, "#8c9498", "#b8c0c4"), "hspst", 0.02)


def monitor_screen(kind):
    h = w = S
    img = T.ramp(T.noise("hspms" + kind, sigma=2), "#020a06", "#06120a")
    yy, xx = np.mgrid[0:h, 0:w]
    img = T.shade(img, 1 - (yy % 2 == 0) * 0.15)
    m = np.zeros((h, w))
    if kind == "flat":
        m = K.line_mask(h, w, 2, 12, 30, 12, 1.2)
    else:
        pts = [(2, 12), (9, 12), (11, 10), (13, 12), (15, 12), (16, 3), (17, 18), (18, 12), (23, 12), (25, 9), (27, 12), (30, 12)]
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            m = np.maximum(m, K.line_mask(h, w, x0, y0, x1, y1, 1.2))
    img = T.blend(img, T.solid("#2aff6a"), K.glow(m, 1.2) * 0.4)
    img = T.blend(img, T.solid("#8affaa"), m)
    # numbers: heart rate and SpO2
    num = "--" if kind == "flat" else "72"
    img = T.blend(img, T.solid("#2aff6a"), K.text_mask(num, h, w, 3, 20, font=K.F35))
    img = T.blend(img, T.solid("#40c8ff"), K.text_mask("98" if kind != "flat" else "--", h, w, 20, 20, font=K.F35))
    return T.border(img, "#1a1e20", 2)


def monitor_case():
    return T.grain(T.ramp(T.noise("hspmc", sigma=1.5), "#c8c4b8", "#d8d4c8"), "hspmc", 0.02)


def iv_bag():
    rgb = T.ramp(T.noise("hspiv", sigma=2), "#d8e8e8", "#f0f8f8")
    a = np.ones((S, S)) * 0.7
    return rgb, a


def lump():
    """A white sheet over something the shape of a person."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = sheet_white()
    folds = np.sin(xx / 2.5 + np.sin(yy / 5.0)) * 0.5 + 0.5
    return T.shade(img, 0.9 + folds * 0.12)


def nurse_counter():
    img = T.ramp(T.noise("hspnc", sigma=2), "#c8b898", "#d4c4a4")
    img[:4] = T.hexc("#e8e4dc")
    img[4] = T.hexc("#8a8270")
    return T.grain(img, "hspnc", 0.02)


def elevator(top, part):
    """Brushed steel doors; part 0 = left leaf, 1 = right leaf."""
    img = steel()
    seam = 31 if part == 0 else 0
    img[:, seam] = T.hexc("#3a3e40")
    if top:
        img[:4] = T.hexc("#6a7276")
    return img


def elevator_panel():
    img = T.solid("#1a1a1c")
    img = T.blend(img, T.solid("#ffb030"), K.centered("B", S, S, 9, font=K.F57, scale=2) )
    yy, xx = np.mgrid[0:S, 0:S]
    tri = (yy >= 4) & (yy <= 8) & (np.abs(xx - 15.5) <= (8 - yy) * 1.2)
    img = T.blend(img, T.solid("#ffb030"), tri.astype(float))
    return T.border(img, "#9aa2a6", 2)


def ward_sign(text):
    def f():
        img = T.solid("#2a4a6a")
        img = T.blend(img, T.solid("#f0f0f0"), K.centered(text, S, S, 12, font=K.F35))
        return T.border(img, "#d0d4d6", 1)
    return f


def wheel():
    return T.grain(T.ramp(T.noise("hspwh", sigma=1), "#1a1a1a", "#2a2a2a"), "hspwh", 0.02)


def vinyl_seat():
    return T.grain(T.ramp(T.noise("hspvs", sigma=1.5), "#2a5a5a", "#336666"), "hspvs", 0.03)


tex("hosp_floor", lambda: sheet_vinyl())
tex("hosp_floor_stripe", lambda: sheet_vinyl("hspfs", "#2a6ab0"))
tex("hosp_floor_stripe_red", lambda: sheet_vinyl("hspfr", "#b03030", True))
tex("hosp_wall", wall_mint)
tex("hosp_wall_rail", wall_rail)
tex("hosp_ceiling", ceiling)
tex("hosp_ceiling_track", ceiling_track)
tex("hosp_troffer", lambda: troffer(True))
tex("hosp_troffer_off", lambda: troffer(False))
tex("hosp_curtain", lambda: curtain(False))
tex("hosp_curtain_top", lambda: curtain(True))
tex("hosp_sheet", lambda: sheet_white())
tex("hosp_sheet_stained", lambda: sheet_white(True))
tex("hosp_blanket", blanket)
tex("hosp_steel", steel)
tex("hosp_screen", lambda: monitor_screen("ecg"))
tex("hosp_screen_flat", lambda: monitor_screen("flat"))
tex("hosp_monitor_case", monitor_case)
tex("hosp_iv_bag", iv_bag)
tex("hosp_lump", lump)
tex("hosp_counter", nurse_counter)
for top in (0, 1):
    for p in (0, 1):
        tex(f"hosp_elevator_{top}_{p}", (lambda top=top, p=p: elevator(top, p)))
tex("hosp_elevator_panel", elevator_panel)
tex("hosp_sign_ward", ward_sign("WARD"))
tex("hosp_sign_icu", ward_sign("ICU"))
tex("hosp_sign_lifts", ward_sign("LIFTS"))
tex("hosp_wheel", wheel)
tex("hosp_vinyl_seat", vinyl_seat)

cube("hosp_floor", "hosp_floor", display="Sheet Vinyl", sound="stone")
cube("hosp_floor_stripe", "hosp_floor_stripe", display="Wayfinding Stripe (Blue)", sound="stone")
cube("hosp_floor_stripe_red", "hosp_floor_stripe_red", display="Wayfinding Stripe (Red)", sound="stone")
cube("hosp_wall", "hosp_wall", display="Ward Wall", sound="stone")
cube("hosp_wall_rail", "hosp_wall_rail", display="Ward Wall with Crash Rail", sound="stone")
cube("hosp_ceiling", "hosp_ceiling", display="Ward Ceiling", sound="wool", flat=True)
cube("hosp_ceiling_track", "hosp_ceiling_track", display="Curtain Track", sound="metal", flat=True)
flicker_light("hosp_troffer", "hosp_troffer", "hosp_troffer_off", "hosp_ceiling", light=14, flicker=0.05,
              display="Ward Light")
flicker_light("hosp_troffer_flicker", "hosp_troffer", "hosp_troffer_off", "hosp_ceiling", light=14, flicker=0.7,
              display="Failing Ward Light")
cube("hosp_sign_ward", "hosp_sign_ward", display="Ward Sign", sound="metal", light=4, emissive=True)
cube("hosp_sign_icu", "hosp_sign_icu", display="ICU Sign", sound="metal", light=4, emissive=True)
cube("hosp_sign_lifts", "hosp_sign_lifts", display="Lifts Sign", sound="metal", light=4, emissive=True)

# curtains: thin, hanging, walk-through, opaque. A gap at the floor.
shaped("hosp_curtain", [(0, 0, 7.5, 16, 16, 8.5, "c")], {"c": "hosp_curtain"}, display="Privacy Curtain",
       collides=False, unbreakable=True, sound="wool")
shaped("hosp_curtain_top", [(0, 0, 7.5, 16, 16, 8.5, "c")], {"c": "hosp_curtain_top"}, display="Privacy Curtain",
       collides=False, unbreakable=True, sound="wool")

BED = [
    (1, 6, 0, 15, 9, 16, {"all": "steel", "up": "sheet"}),
    (1, 2, 1, 2, 6, 2, "steel"), (14, 2, 1, 15, 6, 2, "steel"), (1, 2, 14, 2, 6, 15, "steel"),
    (14, 2, 14, 15, 6, 15, "steel"),
    (1, 0, 1, 2, 2, 2, "wheel"), (14, 0, 1, 15, 2, 2, "wheel"), (1, 0, 14, 2, 2, 15, "wheel"), (14, 0, 14, 15, 2, 15, "wheel"),
]
shaped("hosp_bed_head", BED + [(2, 9, 0, 14, 11, 5, "pillow"), (0, 6, 0, 16, 16, 1, "steel")],
       {"steel": "hosp_steel", "sheet": "hosp_sheet", "pillow": "hosp_sheet", "wheel": "hosp_wheel"},
       display="Hospital Bed (Head)", unbreakable=True)
shaped("hosp_bed_foot", BED + [(1, 9, 0, 15, 10, 15, "blanket"), (0, 6, 15, 16, 12, 16, "steel")],
       {"steel": "hosp_steel", "sheet": "hosp_sheet", "blanket": "hosp_blanket", "wheel": "hosp_wheel"},
       display="Hospital Bed (Foot)", unbreakable=True)
shaped("hosp_bed_foot_unmade", BED + [(1, 9, 3, 12, 11, 15, "blanket"), (0, 6, 15, 16, 12, 16, "steel")],
       {"steel": "hosp_steel", "sheet": "hosp_sheet_stained", "blanket": "hosp_blanket", "wheel": "hosp_wheel"},
       display="Unmade Hospital Bed", unbreakable=True)
shaped("hosp_bed_head_occupied", BED + [(2, 9, 0, 14, 11, 5, "pillow"), (0, 6, 0, 16, 16, 1, "steel"),
                                         (4, 9, 4, 12, 14, 16, "lump"), (5, 9, 0, 11, 15, 5, "lump")],
       {"steel": "hosp_steel", "sheet": "hosp_sheet", "pillow": "hosp_sheet", "wheel": "hosp_wheel", "lump": "hosp_lump"},
       display="Occupied Bed", unbreakable=True)
shaped("hosp_bed_foot_occupied", BED + [(4, 9, 0, 12, 13, 12, "lump"), (0, 6, 15, 16, 12, 16, "steel")],
       {"steel": "hosp_steel", "sheet": "hosp_sheet", "wheel": "hosp_wheel", "lump": "hosp_lump"},
       display="Occupied Bed", unbreakable=True)
for kind, scr in (("", "hosp_screen"), ("_flat", "hosp_screen_flat")):
    shaped(f"hosp_monitor{kind}", [
        (7, 0, 7, 9, 9, 9, "steel"), (4, 0, 4, 12, 1, 12, "steel"),
        (2, 9, 5, 14, 16, 11, {"all": "case", "north": "screen"}),
    ], {"steel": "hosp_steel", "case": "hosp_monitor_case", "screen": scr}, display="Heart Monitor", light=3,
        emissive=True, unbreakable=True)
shaped("hosp_iv_pole", [
    (7.5, 0, 7.5, 8.5, 16, 8.5, "steel"), (4, 0, 7, 12, 1, 9, "wheel"), (7, 0, 4, 9, 1, 12, "wheel"),
    (5, 12, 7, 8, 16, 9, "bag"),
], {"steel": "hosp_steel", "wheel": "hosp_wheel", "bag": "hosp_iv_bag"}, display="IV Pole", collides=False,
    unbreakable=True, render="translucent")
shaped("hosp_gurney", [
    (2, 8, 0, 14, 10, 16, {"all": "steel", "up": "sheet"}), (3, 10, 0, 13, 11, 4, "sheet"),
    (3, 1, 2, 4, 8, 3, "steel"), (12, 1, 2, 13, 8, 3, "steel"), (3, 1, 13, 4, 8, 14, "steel"), (12, 1, 13, 13, 8, 14, "steel"),
    (3, 0, 2, 4, 1, 3, "wheel"), (12, 0, 2, 13, 1, 3, "wheel"), (3, 0, 13, 4, 1, 14, "wheel"), (12, 0, 13, 13, 1, 14, "wheel"),
], {"steel": "hosp_steel", "sheet": "hosp_sheet_stained", "wheel": "hosp_wheel"}, display="Gurney", unbreakable=True)
shaped("hosp_wheelchair", [
    (4, 6, 4, 12, 7, 12, "seat"), (4, 7, 11, 12, 15, 12, "seat"),
    (2, 0, 3, 3, 12, 13, "wheel"), (13, 0, 3, 14, 12, 13, "wheel"),
    (5, 0, 2, 6, 6, 3, "steel"), (10, 0, 2, 11, 6, 3, "steel"),
], {"seat": "hosp_vinyl_seat", "wheel": "hosp_wheel", "steel": "hosp_steel"}, display="Wheelchair", unbreakable=True)
shaped("hosp_waiting_chair", [
    (2, 6, 2, 14, 8, 13, "seat"), (2, 8, 12, 14, 16, 14, "seat"), (2, 0, 3, 3, 6, 12, "steel"), (13, 0, 3, 14, 6, 12, "steel"),
], {"seat": "hosp_vinyl_seat", "steel": "hosp_steel"}, display="Vinyl Chair", unbreakable=True)
cube_faces("hosp_nurse_counter", top="hosp_counter", bottom="hosp_counter", side="hosp_counter",
           display="Nurses' Station Counter")
shaped("hosp_medicine_cart", [
    (2, 1, 3, 14, 13, 13, {"all": "steel", "up": "case"}), (3, 0, 4, 4, 1, 5, "wheel"), (12, 0, 4, 13, 1, 5, "wheel"),
], {"steel": "hosp_steel", "case": "hosp_monitor_case", "wheel": "hosp_wheel"}, display="Medicine Cart",
    kind="pickup", item="noclip:almond_water*2,noclip:battery*1", sound="metal")
for p in (0, 1):
    exit_block(f"hosp_elevator_{p}", f"hosp_elevator_0_{p}", "DESCEND", trigger="USE", solid=True,
               display="Elevator", sound="metal")
    cube(f"hosp_elevator_top_{p}", f"hosp_elevator_1_{p}", display="Elevator", sound="metal")
cube("hosp_elevator_panel", "hosp_elevator_panel", display="Floor Indicator", sound="metal", light=5, emissive=True)

# ------------------------------------------------------------------ the patient


def patient_tex():
    c = Canvas()
    skin = lambda w, h: T.ramp(T.noise(f"ptsk{w}{h}", shape=(h, w), sigma=1.2), "#b8b4a4", "#cac6b6")

    def gauze(w, h):
        img = T.ramp(T.noise(f"ptgz{w}{h}", shape=(h, w), sigma=0.5), "#d8d4c4", "#ece8da")
        for y in range(0, h, 2):
            img[y] = img[y] * 0.9
        return img

    def face(w, h):
        img = gauze(w, h)
        img[3, 1:3] = T.hexc("#140e0c")
        img[3, 5:7] = T.hexc("#140e0c")
        img[6, 3:5] = T.hexc("#6a5a4a")
        return img

    def gown(w, h):
        img = T.ramp(T.noise(f"ptgw{w}{h}", shape=(h, w), sigma=0.8), "#8ab0c4", "#9cc0d2")
        yy, xx = np.mgrid[0:h, 0:w]
        img[((xx + yy) % 4 == 0)] *= 0.88
        return img

    def arm(w, h):
        img = skin(w, h)
        img[:min(4, h)] = gown(w, min(4, h))
        if h > 8:
            img[h - 4] = T.hexc("#e8e4dc")  # hospital bracelet
        return img

    def leg(w, h):
        img = skin(w, h)
        img[:min(3, h)] = gown(w, min(3, h))
        img[-1] = T.hexc("#8a7a6a")
        return img

    paint_humanoid(c, gauze, gown, arm, leg, face=face)
    return c.out()


entity_model("ward_patient", humanoid(arm_len=13, leg_len=13))
entity_tex("ward_patient", patient_tex)
lang("entity.noclip.ward_patient", "The Patient")
lang("item.noclip.ward_patient_spawn_egg", "Patient Spawn Egg")
ITEM_MODELS["ward_patient_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def ward_tone():
    d = 24.0
    t = Y.t_axis(d)
    # a ventilator somewhere: a slow push and sigh every four seconds
    cyc = (t % 4.0) / 4.0
    push = np.clip(np.sin(np.pi * cyc / 0.45), 0, None) * (cyc < 0.45)
    sigh = np.clip(np.sin(np.pi * (cyc - 0.5) / 0.4), 0, None) * (cyc >= 0.5) * (cyc < 0.9)
    vent = Y.bp(Y.white(d, "hspvent"), 300, 1400) * push * 0.18 + Y.bp(Y.white(d, "hspvent2"), 200, 900) * sigh * 0.12
    hum = Y.lp(Y.hum(d, 50.0, "hsphum", buzz=0.4, harmonics=(2, 4, 6)), 2400) * 0.2
    air = Y.lp(Y.brown(d, "hspair"), 160) * 0.25
    # other monitors, far off, none in time with each other
    beeps = np.zeros_like(t)
    for k, (period, f, g) in enumerate(((1.5, 880, 0.05), (0.92, 1046, 0.035), (2.0, 740, 0.04))):
        n = int(0.09 * Y.SR)
        tt = np.arange(n) / Y.SR
        b = np.sin(2 * np.pi * f * tt) * np.minimum(tt * 400, 1) * np.exp(-tt * 10)
        for at in np.arange(0.3 + k * 0.4, d, period):
            K.circular_place(beeps, b, at, g)
    beeps = K.loop_reverb(Y.lp(beeps, 3000), 2.5, 0.75, "hspbp")
    x = vent + hum + air + beeps
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.0011), "stereo"


def gurney():
    d = 4.5
    t = Y.t_axis(d)
    rattle = Y.bp(Y.white(d, "hspgr"), 300, 3000) * (0.5 + 0.5 * (np.sin(2 * np.pi * 9 * t) > 0.6))
    squeak = np.sin(2 * np.pi * 2300 * t) * (np.sin(2 * np.pi * 1.7 * t) > 0.92) * 0.3
    x = (rattle * 0.6 + squeak) * np.sin(np.pi * t / d) ** 1.5
    return Y.norm(Y.reverb(Y.lp(x, 4000), 3.0, 0.7, "hspgr"), 0.55)


def moan():
    d = 3.0
    t = Y.t_axis(d)
    f0 = 140 * (1 - 0.15 * t / d) * (1 + 0.02 * np.sin(2 * np.pi * 5 * t))
    ph = 2 * np.pi * np.cumsum(f0) / Y.SR
    v = (np.sin(ph) + 0.4 * np.sin(2 * ph) + 0.2 * np.sin(3 * ph)) * np.sin(np.pi * t / d) ** 0.8
    v = K._formant_filter(v, 570, 120) + K._formant_filter(v, 840, 160) * 0.6
    return Y.norm(Y.reverb(Y.lp(v, 1800), 3.0, 0.8, "hspmoan"), 0.5)


def page():
    c = K.chime([K.hz("C6"), K.hz("A5")], gap=0.35, decay=1.2, seed="hsppage")
    v = K.pa_voice("hsppagev", syllables=13, f0=125, rate=0.16, female=True)
    out = np.zeros(len(c) + len(v))
    Y.place(out, c, 0.0, 0.7)
    Y.place(out, v, 0.9, 0.8)
    return Y.norm(Y.reverb(out, 3.0, 0.7, "hsppage"), 0.6)


def curtain_rings():
    d = 1.6
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("hsprng")
    for k in range(16):
        n = int(0.04 * Y.SR)
        tt = np.arange(n) / Y.SR
        click = Y.bp(Y.white(0.04, f"hsprn{k}"), 2500, 9000)[:n] * np.exp(-tt * 120)
        click += np.sin(2 * np.pi * (3200 + r.random() * 800) * tt) * np.exp(-tt * 90) * 0.3
        Y.place(out, click, 0.05 + k * 0.06 + r.random() * 0.02, 0.5 + r.random() * 0.4)
    return Y.norm(Y.reverb(out, 1.4, 0.4, "hsprng"), 0.6)


def beep():
    n = int(0.16 * Y.SR)
    tt = np.arange(n) / Y.SR
    x = np.sin(2 * np.pi * 1000 * tt) * np.minimum(tt * 500, 1) * np.minimum((0.16 - tt) * 200, 1)
    return Y.norm(x, 0.7)


def flatline():
    d = 2.5
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * 1000 * t) * np.minimum(t * 300, 1) * np.minimum((d - t) * 20, 1)
    return Y.norm(x, 0.6)


def breath():
    d = 3.2
    t = Y.t_axis(d)
    cyc = (t % 1.6) / 1.6
    env = np.sin(np.pi * np.clip(cyc / 0.4, 0, 1)) * (cyc < 0.4) + np.sin(np.pi * np.clip((cyc - 0.5) / 0.45, 0, 1)) * (cyc >= 0.5) * 0.8
    rasp = 1 + 0.6 * (np.sin(2 * np.pi * 37 * t) > 0)
    x = Y.bp(Y.white(d, "ptbr"), 300, 2500) * env * rasp
    return Y.norm(x, 0.8)


def shuffle():
    out = np.zeros(int(2.4 * Y.SR))
    for k in range(4):
        n = int(0.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        drag = Y.bp(Y.white(0.4, f"ptsh{k}"), 200, 2500)[:n] * np.sin(np.pi * tt / 0.4) ** 2
        Y.place(out, drag, 0.1 + k * 0.55, 0.8)
    return Y.norm(out, 0.7)


sound("ambient.hospital", [("ambient/hospital", ward_tone)], stream=True)
sound("ambient.hospital.additions",
      [("ambient/hospital_gurney", gurney), ("ambient/hospital_moan", moan), ("ambient/hospital_page", page),
       ("ambient/hospital_curtain", curtain_rings)],
      subtitle="The ward at night")
sound("event.hospital_beep", [("event/hospital_beep", beep)], subtitle="Your heart monitor")
sound("event.hospital_flatline", [("event/hospital_flatline", flatline)], subtitle="Flatline")
sound("entity.ward_patient.breath", [("entity/ward_patient_breath", breath)], subtitle="Wet breathing")
sound("entity.ward_patient.shuffle", [("entity/ward_patient_shuffle", shuffle)], subtitle="Bare feet shuffle")

level("hospital", fog="#2a3430", sky="#2a3430", ambient="ambient.hospital", additions="ambient.hospital.additions",
      additions_chance=0.005, fixed_time=18000, height=48)
