"""The Lift Lobby. Polished marble, brass doors, a dial above every lift that never quite settles.
The Lift Attendant asks which floor you want, and means it."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, exit_block, level, sound, lang, \
    entity_model, entity_tex, part, ITEM_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, wood, text_mask, wow, epiano, chime, voice, speaker
from entity_paint import Canvas
from content.entities_core import humanoid, paint_humanoid

S = 32


# ------------------------------------------------------------------ textures

def marble(seed, lo, hi, vein, vein_strength=0.55):
    base = T.ramp(T.fbm(seed, octaves=4, base=6), lo, hi)
    # veins: thin bright lines where a warped field crosses zero
    f = T.fbm(seed + "v", octaves=2, base=8)
    w = T.fbm(seed + "w", octaves=2, base=4)
    field = np.sin((f * 5 + w * 1.5) * np.pi)
    veins = np.clip(1 - np.abs(field) * 2.2, 0, 1) ** 1.2
    img = T.blend(base, T.solid(vein), veins * vein_strength)
    # polish: soft diagonal sheen
    yy, xx = np.mgrid[0:S, 0:S]
    sheen = np.clip(1 - np.abs(((xx + yy) % 32) - 10) / 7.0, 0, 1) * 0.07
    img = T.shade(img, 1 + sheen)
    img = T.border(img, "#2a2620", 1, alpha=0.18)
    return T.grain(img, seed, 0.015)


def marble_cream():
    return marble("lmc", "#d9cfb8", "#ece4d0", "#a89a7c", 0.4)


def marble_black():
    return marble("lmb", "#141313", "#262422", "#a8a49a", 0.5)


def runner_edge():
    img = T.ramp(T.noise("lre", sigma=0.6) * 0.5 + T.fbm("lre2", base=4) * 0.5, "#5e1018", "#7a1a22")
    yy, xx = np.mgrid[0:S, 0:S]
    # gold border band at the north edge, a thin inner line, and a repeating lozenge motif
    img = T.blend(img, T.solid("#b8923a"), ((yy >= 1) & (yy <= 4)).astype(float) * 0.9)
    img = T.blend(img, T.solid("#2a0a0e"), (yy == 0).astype(float))
    img = T.blend(img, T.solid("#b8923a"), (yy == 7).astype(float) * 0.7)
    loz = (np.abs((xx % 16) - 8) + np.abs(yy - 19) < 6) & (np.abs((xx % 16) - 8) + np.abs(yy - 19) > 3.5)
    img = T.blend(img, T.solid("#a07a32"), loz.astype(float) * 0.6)
    worn = T.noise("lrw", sigma=2) > 0.75
    img = T.shade(img, 1 - worn * 0.12)
    return T.grain(img, "lre", 0.05)


def wall_paper():
    """Cream silk with a faint damask lozenge and vertical sheen."""
    img = T.ramp(T.fbm("lwp", octaves=3, base=6), "#cbbd98", "#d8cba8")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.abs((xx % 16) - 8) + np.abs((yy % 16) - 8) * 0.7
    motif = ((d > 4) & (d < 5.2)) | ((xx % 16 == 8) & (np.abs((yy % 16) - 8) < 2))
    img = T.shade(img, 1 - motif * 0.06)
    img = T.shade(img, 1 + np.sin(xx / 32 * 2 * np.pi * 4) * 0.015)
    img = T.stains(img, "lwps", "#a8925e", count=1, strength=0.15, size=10)
    return T.grain(img, "lwp", 0.02)


def wainscot():
    img = wood("lws", "#3a2416", "#5a3822", plank=16, vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    # raised panel: bevel lines
    inner = (xx >= 3) & (xx <= 28) & (yy >= 8) & (yy <= 29)
    edge_l = ((xx == 3) | (yy == 8)) & inner
    edge_d = ((xx == 28) | (yy == 29)) & inner
    img = T.shade(img, 1 + edge_l * 0.25 - edge_d * 0.3)
    # brass chair rail along the top
    rail = brushed("lwsr", "#8a6a2a", "#d0aa52", vertical=False)
    img[0:4] = rail[0:4]
    img[4] = T.hexc("#2a1a10")
    img[0] = img[0] * 1.15
    return T.grain(np.clip(img, 0, 1), "lws", 0.02)


def cornice():
    img = T.ramp(T.fbm("lco", octaves=2, base=4), "#d8cdb0", "#e6dcc2")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    xx = np.arange(S)[None, :] * np.ones((S, 1))
    bands = np.zeros((S, S))
    for y0, y1, v in ((0, 3, -0.15), (3, 5, 0.12), (12, 14, -0.2), (14, 16, 0.12), (22, 24, -0.1)):
        bands[(yy >= y0) & (yy < y1)] = v
    img = T.shade(img, 1 + bands)
    gold = (yy >= 17) & (yy < 20)
    img = T.blend(img, brushed("lcog", "#9a7a32", "#d4b05a", vertical=False), gold.astype(float))
    dent = ((yy >= 6) & (yy < 11) & (xx % 4 == 0))
    img = T.shade(img, 1 - dent * 0.2)
    return T.grain(img, "lco", 0.02)


def ceiling():
    img = T.ramp(T.fbm("lce", octaves=2, base=5), "#d8cfb6", "#e4dcc6")
    yy, xx = np.mgrid[0:S, 0:S]
    # coffer: recessed square with a gilded inner fillet
    outer = (xx < 3) | (xx > 28) | (yy < 3) | (yy > 28)
    img = T.shade(img, 1 + outer * 0.06)
    fillet = ((xx == 5) | (xx == 26) | (yy == 5) | (yy == 26)) & (xx >= 5) & (xx <= 26) & (yy >= 5) & (yy <= 26)
    img = T.blend(img, T.solid("#b8963e"), fillet.astype(float) * 0.8)
    inner_shadow = ((xx == 3) | (yy == 3)) & ~outer
    img = T.shade(img, 1 - inner_shadow * 0.18)
    img = T.stains(img, "lces", "#a08a5a", count=1, strength=0.12, size=8)
    return T.grain(img, "lce", 0.015)


def ceiling_light(on=True):
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        cx, cy = 15.5, 15.5
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        img = T.ramp(np.clip(1 - d / 22, 0, 1), "#f0d8a0", "#fff6e0")
    else:
        img = T.ramp(T.noise("lclo", sigma=2), "#6e6658", "#80786a")
    # art deco sunburst leading
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    rays = (np.abs(np.sin(ang * 6)) < 0.08) & (np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2) > 4)
    ring = np.abs(np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2) - 4) < 0.7
    img = T.blend(img, T.solid("#8a6a2a"), (rays | ring).astype(float) * (0.55 if on else 0.8))
    img = T.border(img, "#b8963e", 2)
    img = T.border(img, "#6a5020", 1)
    if not on:
        img = T.stains(img, "lclos", "#3a3020", count=2, strength=0.3, size=6)
    return img


def frame():
    img = brushed("lfr", "#3a2c1a", "#5a4428", vertical=True)
    xx = np.arange(S)[None, :] * np.ones((S, 1))
    flute = (xx % 6 == 0)
    img = T.shade(img, 1 - flute * 0.35 + (xx % 6 == 1) * 0.15)
    return T.grain(img, "lfr", 0.03)


def door_leaf(top=False):
    img = brushed("ldl" + str(top), "#9a7a3a", "#d6b466", vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    img[:, 0] = T.hexc("#3a2a12")
    img[:, -1] = T.hexc("#3a2a12")
    img[:, 1] *= 1.15
    # finger smudges near hand height, slight tarnish
    img = T.stains(img, "ldls" + str(top), "#6a5226", count=1, strength=0.18, size=7)
    if top:
        # half sunburst etched into the upper leaf, rising from the bottom edge
        ang = np.arctan2(31 - yy, xx - 15.5)
        r = np.sqrt((xx - 15.5) ** 2 + (31 - yy) ** 2)
        rays = (np.abs(np.sin(ang * 7)) < 0.12) & (r > 5) & (r < 26)
        arcs = (np.abs(r - 26) < 0.6) | (np.abs(r - 5) < 0.6)
        img = T.shade(img, 1 - (rays | arcs) * 0.35)
    else:
        img = T.shade(img, 1 - ((yy == 0) | (yy == 31)) * 0.1)
    return T.grain(np.clip(img, 0, 1), "ldl" + str(top), 0.02)


def dial(side, wrong=False):
    """Half of a semicircular floor indicator. side 'l' or 'r'. The needle is in the right half."""
    W = 64
    yy, xx = np.mgrid[0:S, 0:W]
    img = brushed("ldi" + side + str(wrong), "#3a2c1a", "#5a4428", vertical=False, shape=(S, W))
    cx, cy = 31.5, 28
    r = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    face = (r < 24) & (yy <= cy)
    glow = "#f2b85a" if not wrong else "#e0402a"
    face_col = T.ramp(np.clip(1 - r / 30, 0, 1), "#a8641a" if not wrong else "#801812", glow)
    img = T.blend(img, face_col, face.astype(float))
    rim = (np.abs(r - 24) < 1.2) & (yy <= cy)
    img = T.blend(img, T.solid("#d4b05a", shape=(S, W)), rim.astype(float))
    # tick marks and numerals around the arc
    ang = np.arctan2(cy - yy, xx - cx)
    for k in range(9):
        a = np.pi * (1 - k / 8)
        tick = (np.abs(ang - a) < 0.035) & (r > 19) & (r < 23)
        img = T.blend(img, T.solid("#2a1608", shape=(S, W)), tick.astype(float))
    # needle
    needle_a = np.pi * (0.22 if not wrong else -0.08)
    na = np.abs(ang - needle_a) < 0.05
    needle = na & (r < 20) & (r > 2)
    img = T.blend(img, T.solid("#1a0c04", shape=(S, W)), needle.astype(float))
    hub = r < 2.5
    img = T.blend(img, T.solid("#d4b05a", shape=(S, W)), hub.astype(float))
    img = T.border(img, "#2a1e10", 1)
    half = img[:, :S] if side == "l" else img[:, S:]
    return T.grain(half, "ldi" + side, 0.015)


def call_button(lit=False):
    img = brushed("lcb", "#8a6a2a", "#c8a24c", vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.border(img, "#4a3818", 1)
    for cy, up in ((11, True), (21, False)):
        r = np.sqrt((xx - 15.5) ** 2 + (yy - cy) ** 2)
        ring = r < 4.5
        img = T.blend(img, T.solid("#3a2a10"), ring.astype(float))
        btn = r < 3.5
        col = "#ffcf70" if (lit and up) else "#d8d0c0"
        img = T.blend(img, T.solid(col), btn.astype(float))
        tri = (np.abs(xx - 15.5) < ((yy - (cy - 2)) * 0.9 if up else ((cy + 2) - yy) * 0.9)) & (np.abs(yy - cy) < 2.2)
        img = T.blend(img, T.solid("#5a4020"), tri.astype(float) * 0.9)
    return img


def cabin_wall():
    img = wood("lcw", "#5a3a20", "#7a5230", plank=32, vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    rail = (yy >= 18) & (yy <= 20)
    img = T.blend(img, brushed("lcwr", "#9a7a3a", "#e0c070", vertical=False), rail.astype(float))
    img = T.shade(img, 1 - (yy == 21) * 0.3)
    return T.grain(img, "lcw", 0.02)


def cabin_mirror():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(np.clip((xx + yy) / 64 + T.noise("lcm", sigma=3) * 0.2, 0, 1), "#3a3830", "#8a8a80")
    streak = np.abs(((xx - yy) % 24) - 6) < 1.5
    img = T.shade(img, 1 + streak * 0.18)
    img = T.stains(img, "lcms", "#2a2820", count=2, strength=0.2, size=5)
    return T.border(img, "#b8963e", 2)


def cabin_light():
    img = T.solid("#fff1d0")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (((xx % 8) == 0) | ((yy % 8) == 0)) * 0.08)
    return T.border(img, "#b8963e", 2)


def cabin_floor():
    img = T.ramp(T.noise("lcf", sigma=0.6) * 0.5 + T.fbm("lcf2") * 0.5, "#4a1a18", "#5e2420")
    return T.border(img, "#2a1a10", 1)


def shaft():
    img = T.ramp(T.noise("lsh", sigma=3), "#020202", "#0a0a0c")
    xx = np.arange(S)[None, :] * np.ones((S, 1))
    yy = np.arange(S)[:, None] * np.ones((1, S))
    for cx in (9, 22):
        c = np.abs(xx - cx - np.sin(yy / 9.0) * 0.4) < 0.9
        img = T.blend(img, T.solid("#1c1a16"), c.astype(float))
    return T.grain(img, "lsh", 0.02)


def stairs_door():
    img = T.ramp(T.noise("lsd", sigma=2), "#5a5e5a", "#6a6e68")
    img = T.border(img, "#2e302e", 2)
    img = T.rect(img, 0, 6, 32, 14, "#e8e2d0")
    img = ink(img, centered("STAIRS", 32, 32, y=7, scale=1, spacing=0), "#a01818")
    img = T.rect(img, 22, 19, 27, 21, "#b0b0a8")
    img = T.stains(img, "lsds", "#3a3c38", count=2, strength=0.25, size=6)
    return T.grain(img, "lsd", 0.04)


def leather():
    img = T.ramp(T.noise("lle", sigma=1.2), "#3a1410", "#56201a")
    yy, xx = np.mgrid[0:S, 0:S]
    tuft = ((xx % 8 == 4) & (yy % 8 == 4))
    crease = (np.abs((xx % 8) - 4) == np.abs((yy % 8) - 4)) & ~tuft
    img = T.shade(img, 1 - tuft * 0.5 - crease * 0.12)
    return T.grain(img, "lle", 0.04)


def brass():
    return T.grain(brushed("lbr", "#8a6a2a", "#d8b45a", vertical=True), "lbr", 0.02)


def palm():
    rgb = T.ramp(T.noise("lpa", sigma=1.0), "#1e3a1a", "#3e6a2a")
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.zeros((S, S))
    # fronds: arcs of thin leaflets fanning out from the bottom centre
    for k, ang in enumerate(np.linspace(-1.2, 1.2, 6)):
        for t in np.linspace(0, 1, 60):
            x = 15.5 + np.sin(ang) * t * 16
            y = 31 - np.cos(ang) * t * 22 + t * t * 10 * abs(np.sin(ang))
            for dl in (-1, 1):
                for s in range(0, 4):
                    lx = int(x + dl * s * np.cos(ang) * 0.9)
                    ly = int(y + s * 0.8)
                    if 0 <= lx < S and 0 <= ly < S and t > 0.15:
                        a[ly, lx] = 1
            if 0 <= int(x) < S and 0 <= int(y) < S:
                a[int(y), int(x)] = 1
                rgb[int(y), int(x)] = T.hexc("#5a4a2a")
    return rgb, a


def soil():
    return T.grain(T.ramp(T.noise("lso", sigma=0.8), "#1a120c", "#2e2016"), "lso", 0.05)


def suitcase():
    img = T.ramp(T.noise("lsu", sigma=1.5), "#5a3a22", "#6e4a2c")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, T.solid("#2a1a10"), ((xx % 16 == 4) | (xx % 16 == 5)).astype(float) * 0.8)
    img = T.border(img, "#3a2414", 1)
    return T.grain(img, "lsu", 0.04)


def podium():
    img = wood("lpo", "#2a180e", "#4a2c1a", plank=32, vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, brushed("lpob", "#8a6a2a", "#d0aa52"), ((yy < 3) | (yy > 28)).astype(float))
    return img


tex("lift_marble_cream", marble_cream)
tex("lift_marble_black", marble_black)
tex("lift_runner_edge", runner_edge)
tex("lift_runner_edge_s", lambda: runner_edge()[::-1].copy())
tex("lift_wall", wall_paper)
tex("lift_wainscot", wainscot)
tex("lift_cornice", cornice)
tex("lift_ceiling", ceiling)
tex("lift_ceiling_light", lambda: ceiling_light(True))
tex("lift_ceiling_light_off", lambda: ceiling_light(False))
tex("lift_frame", frame)
tex("lift_door", lambda: door_leaf(False))
tex("lift_door_top", lambda: door_leaf(True))
tex("lift_dial_l", lambda: dial("l"))
tex("lift_dial_r", lambda: dial("r"))
tex("lift_dial_wrong_l", lambda: dial("l", True))
tex("lift_dial_wrong_r", lambda: dial("r", True))
tex("lift_call_button", lambda: call_button(False))
tex("lift_cabin_wall", cabin_wall)
tex("lift_cabin_mirror", cabin_mirror)
tex("lift_cabin_light", cabin_light)
tex("lift_cabin_floor", cabin_floor)
tex("lift_shaft", shaft)
tex("lift_stairs_door", stairs_door)
tex("lift_leather", leather)
tex("lift_brass", brass)
tex("lift_palm", palm)
tex("lift_soil", soil)
tex("lift_suitcase", suitcase)
tex("lift_podium", podium)

cube("lift_marble_cream", "lift_marble_cream", display="Cream Marble", sound="stone")
cube("lift_marble_black", "lift_marble_black", display="Black Marble", sound="stone")
cube("lift_runner_n", "lift_runner_edge", display="Lobby Runner", sound="wool")
cube("lift_runner_s", "lift_runner_edge_s", display="Lobby Runner", sound="wool")
cube("lift_wall", "lift_wall", display="Silk Wallcovering", sound="wool")
cube("lift_wainscot", "lift_wainscot", display="Walnut Wainscot", sound="wood")
cube("lift_cornice", "lift_cornice", display="Plaster Cornice")
cube("lift_ceiling", "lift_ceiling", display="Coffered Ceiling", flat=True)
flicker_light("lift_ceiling_light", "lift_ceiling_light", "lift_ceiling_light_off", "lift_ceiling", light=14,
              flicker=0.0, display="Deco Ceiling Light")
flicker_light("lift_ceiling_light_flicker", "lift_ceiling_light", "lift_ceiling_light_off", "lift_ceiling", light=14,
              flicker=0.7, display="Failing Deco Ceiling Light")
cube_faces("lift_ceiling_light_dead", bottom="lift_ceiling_light_off", top="lift_ceiling", side="lift_ceiling",
           flat=True, display="Dead Deco Ceiling Light", sound="glass")
cube("lift_frame", "lift_frame", display="Bronze Lift Surround", sound="metal")
for nm, tx, disp in (("lift_door", "lift_door", "Lift Door"), ("lift_door_top", "lift_door_top", "Lift Door")):
    shaped(nm, [(0, 0, 0, 16, 16, 16, {"all": "frame", "north": "leaf"})], {"leaf": tx, "frame": "lift_frame"},
           display=disp, sound="metal", unbreakable=True, render="solid")
for nm, tx in (("lift_dial_l", "lift_dial_l"), ("lift_dial_r", "lift_dial_r"),
               ("lift_dial_wrong_l", "lift_dial_wrong_l"), ("lift_dial_wrong_r", "lift_dial_wrong_r")):
    shaped(nm, [(0, 0, 0, 16, 16, 16, {"all": "frame", "north": "dial"})], {"dial": tx, "frame": "lift_frame"},
           display="Floor Indicator", sound="metal", unbreakable=True, render="solid", emissive=True)

# call button plate: a Java block (rings for the lift) built from one thin box
shaped("lift_call_button", [(4, 3, 0, 12, 13, 1, {"all": "plate"})], {"plate": "lift_call_button"},
       display="Lift Call Button", sound="metal", unbreakable=True, collides=False, emissive=True,
       kind="class", **{"class": "com.noclip.block.LiftCallButtonBlock"})
BLOCKSTATES["lift_call_button"] = {"variants": {
    f"facing={f}": ({"model": "noclip:block/lift_call_button", "y": r} if r else {"model": "noclip:block/lift_call_button"})
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}

cube("lift_cabin_wall", "lift_cabin_wall", display="Lift Car Panelling", sound="wood")
cube("lift_cabin_mirror", "lift_cabin_mirror", display="Lift Car Mirror", sound="glass")
cube("lift_cabin_light", "lift_cabin_light", display="Lift Car Light", light=13, emissive=True, flat=True,
     sound="glass")
# 15/16 tall so nobody is ever placed inside a closed car on arrival
shaped("lift_cabin_floor", [(0, 0, 0, 16, 15, 16, "f")], {"f": "lift_cabin_floor"}, display="Lift Car Floor",
       sound="wool", unbreakable=True, render="solid")
exit_block("lift_shaft_dark", "lift_shaft", "FALL", display="Open Lift Shaft")
exit_block("lift_stairs_door", "lift_stairs_door", "DOOR", trigger="USE", solid=True, display="Stair Door",
           sound="metal")

shaped("lift_bench", [
    (1, 5, 3, 15, 8, 13, "leather"),
    (1, 8, 11, 15, 15, 13, "leather"),
    (2, 0, 4, 3, 5, 5, "brass"), (13, 0, 4, 14, 5, 5, "brass"),
    (2, 0, 11, 3, 5, 12, "brass"), (13, 0, 11, 14, 5, 12, "brass"),
], {"leather": "lift_leather", "brass": "lift_brass"}, display="Tufted Bench", sound="wool", unbreakable=True)
shaped("lift_planter", [
    (3, 0, 3, 13, 9, 13, {"all": "brass", "up": "soil"}),
    (0, 7, 7.5, 16, 16, 8.5, {"all": None, "north": "palm", "south": "palm"}),
    (7.5, 7, 0, 8.5, 16, 16, {"all": None, "east": "palm", "west": "palm"}),
], {"brass": "lift_brass", "soil": "lift_soil", "palm": "lift_palm"}, display="Potted Palm", sound="grass",
    unbreakable=True)
shaped("lift_sconce", [
    (5, 4, 0, 11, 12, 2, "brass"),
    (4, 7, 2, 12, 13, 6, {"all": "glow", "down": "brass"}),
], {"brass": "lift_brass", "glow": "lift_cabin_light"}, display="Wall Sconce", light=11, emissive=True,
    collides=False, sound="glass", unbreakable=True)
shaped("lift_ashtray", [
    (6, 0, 6, 10, 1, 10, "brass"), (7, 1, 7, 9, 10, 9, "brass"), (5, 10, 5, 11, 12, 11, {"all": "brass", "up": "soil"}),
], {"brass": "lift_brass", "soil": "lift_soil"}, display="Standing Ashtray", sound="metal", unbreakable=True)
shaped("lift_luggage", [
    (1, 0, 2, 15, 1, 14, "brass"),
    (1, 1, 2, 2, 16, 3, "brass"), (14, 1, 2, 15, 16, 3, "brass"), (1, 15, 2, 15, 16, 3, "brass"),
    (3, 1, 4, 13, 9, 11, "case"), (4, 9, 5, 11, 14, 10, "case"),
], {"brass": "lift_brass", "case": "lift_suitcase"}, display="Luggage Cart", sound="metal", unbreakable=True)
shaped("lift_podium", [
    (2, 0, 3, 14, 13, 13, "wood"), (1, 13, 2, 15, 15, 14, {"all": "wood", "up": "leather"}),
], {"wood": "lift_podium", "leather": "lift_leather"}, display="Attendant's Podium", sound="wood", unbreakable=True)

# ------------------------------------------------------------------ the Lift Attendant


def attendant_tex():
    c = Canvas()
    wool = lambda seed: (lambda w, h: T.grain(T.ramp(T.noise(f"la{seed}{w}{h}", shape=(h, w), sigma=0.8),
                                                        "#5a1018", "#6e1a22"), f"la{seed}", 0.03))
    pale = lambda w, h: T.ramp(T.noise(f"lap{w}{h}", shape=(h, w), sigma=1.2), "#e4d8cc", "#efe6dc")

    def face(w, h):
        img = pale(w, h)
        ink_c = T.hexc("#2a1a16")
        # eyes closed in two gentle arcs, a smile that is a little too wide
        img[3, 1:3] = ink_c
        img[2, 1] = img[2, 2] = img[3, 0] * 0 + T.hexc("#c8b8aa")
        img[3, 5:7] = ink_c
        for x in range(1, 7):
            y = 5 + (1 if 2 <= x <= 5 else 0)
            img[y, x] = T.hexc("#6a2a26")
        img[0, :] = T.hexc("#1a1210")
        return img

    def hair(w, h):
        img = T.grain(T.solid("#1a1210", shape=(h, w)), "lah", 0.05)
        img[-3:] = pale(w, 3)
        return img

    def jacket(w, h):
        img = wool("j")(w, h)
        if w == 8:
            img[:, 3:5] = T.hexc("#3a0a10")
            for y in (2, 5, 8):
                if y < h:
                    img[y, 2] = T.hexc("#e0b850")
                    img[y, 5] = T.hexc("#e0b850")
            img[0:2, 3:5] = T.hexc("#f0ece4")
        img[-1] = T.hexc("#3a0a10")
        return img

    def sleeve(w, h):
        img = wool("s")(w, h)
        img[1, :] = T.hexc("#d4ac48")
        img[-3:] = T.hexc("#f4f0ea")
        return img

    def trousers(w, h):
        img = T.grain(T.solid("#1a1418", shape=(h, w)), f"lat{w}{h}", 0.04)
        img[:, w // 2] = T.hexc("#c8a040")
        img[-2:] = T.hexc("#0a0a0a")
        return img

    paint_humanoid(c, hair, jacket, sleeve, trousers, face=face)
    # pillbox hat (32,0 region)
    hat = lambda w, h: T.grain(T.ramp(T.noise(f"lahat{w}{h}", shape=(h, w), sigma=1), "#5a1018", "#721c26"), "lahat", 0.03)

    def hat_side(w, h):
        img = hat(w, h)
        img[-1] = T.hexc("#d4ac48")
        img[0] = T.hexc("#d4ac48")
        return img

    c.box(32, 0, 6, 3, 6, all=hat_side, up=hat)
    return c.out()


entity_model("lift_attendant", humanoid(head_extra=[(32, 0, -3, -11, -3, 6, 3, 6)], arm_len=13, leg_len=13))
entity_tex("lift_attendant", attendant_tex)
lang("entity.noclip.lift_attendant", "Lift Attendant")
lang("item.noclip.lift_attendant_spawn_egg", "Lift Attendant Spawn Egg")
ITEM_MODELS["lift_attendant_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def muzak():
    """Lobby muzak: a soft electric piano bossa, slowed, warped, through ceiling speakers."""
    d = 24.0
    n = int(d * Y.SR)
    out = np.zeros(n)
    # Cmaj9 - Am9 - Dm9 - G13, two bars each at 80 bpm (one chord per 6 s)
    chords = [(130.8, [261.6, 329.6, 392.0, 493.9, 587.3]),
              (110.0, [220.0, 261.6, 329.6, 392.0, 493.9]),
              (146.8, [293.7, 349.2, 440.0, 523.3, 659.3]),
              (98.0, [246.9, 349.2, 392.0, 440.0, 659.3])]
    beat = 60 / 80
    for ci, (bass, notes) in enumerate(chords):
        t0 = ci * 6.0
        # bossa comping pattern
        for hit in (0, 1.5, 2.5, 4, 5.5, 6.5):
            at = t0 + hit * beat * 0.5 * 1.6
            if at >= t0 + 6:
                continue
            for k, f in enumerate(notes[1:]):
                Y.place(out, epiano(f, 1.6, f"mz{ci}{hit}{k}", 0.8) * 0.12, at + k * 0.006)
        for hit, mul in ((0, 1.0), (1.5, 1.5), (3.0, 1.0), (4.5, 1.5)):
            at = t0 + hit
            tt = Y.t_axis(1.2)
            b = np.sin(2 * np.pi * bass * mul * 0.5 * tt) * np.exp(-tt * 2.2) * Y.env(len(tt), 0.01, 0.2)
            Y.place(out, b * 0.35, at)
    # a melody that drifts in and loses its way
    mel = [659.3, 587.3, 523.3, 493.9, 440.0, 493.9, 523.3, 392.0, 440.0, 349.2, 329.6, 293.7]
    for i, f in enumerate(mel):
        Y.place(out, epiano(f * 2, 1.8, f"mzm{i}", 1.2) * 0.09, 0.5 + i * 2.0)
    # brushed snare
    for k in range(int(d / (beat * 0.5))):
        at = k * beat * 0.5
        hit = Y.bp(Y.white(0.12, f"brush{k}"), 2000, 9000) * np.exp(-np.arange(int(0.12 * Y.SR)) / Y.SR * 30)
        Y.place(out, hit * (0.05 if k % 2 else 0.025), at)
    out = wow(out, depth=0.006, rate=0.23, seed="mzwow")
    out = speaker(out, 300, 3800)
    out = Y.reverb(out, 1.6, 0.35, "mzrv")[:n]
    room = Y.lp(Y.brown(d, "liftroom"), 160) * 0.12 + Y.hum(d, 60.0, "lifthum", buzz=0.05, harmonics=(1, 2, 3)) * 0.02
    x = Y.loopable(Y.norm(out + room, 0.5), 1.0)
    return Y.stereoize(x, 0.0012), "stereo"


def ding():
    out = chime([1046.5, 830.6], dur=3.0, decay=1.4)
    return Y.norm(Y.reverb(out, 1.8, 0.35, "ding"), 0.8)


def ding_far(i):
    def f():
        x = chime([1046.5 * (0.94 + 0.04 * i), 830.6 * (0.94 + 0.04 * i)], dur=3.0, decay=1.6)
        return Y.norm(Y.reverb(Y.lp(x, 2200), 3.0, 0.7, f"dingf{i}"), 0.45)
    return f


def cable_creak():
    d = 3.0
    t = Y.t_axis(d)
    f0 = 140 + 60 * np.sin(2 * np.pi * 0.4 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t)) * np.sin(np.pi * t / d) ** 2
    x += Y.bp(Y.white(d, "cab"), 600, 2400) * 0.15 * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(Y.lp(x, 1800), 3.5, 0.75, "cable"), 0.5)


def doors_rumble():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.lp(Y.white(d, "dr"), 220) * np.clip(np.sin(np.pi * t / 2.2), 0, None)
    thud = np.zeros_like(t)
    Y.place(thud, Y.impact("drth", 0.8), 2.2, 0.8)
    return Y.norm(Y.reverb(x + thud, 2.5, 0.6, "drrv"), 0.5)


def going_up():
    x = voice("goingup", 1.4, pitch=105, breath=0.2)
    x = speaker(x, 400, 2600)
    return Y.norm(Y.reverb(x, 3.0, 0.7, "gurv"), 0.35)


def attendant_hm(i):
    def f():
        x = voice(f"att{i}", 0.6 + 0.2 * i, pitch=95 + 10 * i, breath=0.1)
        return Y.norm(Y.reverb(x, 0.8, 0.2, f"attrv{i}"), 0.6)
    return f


sound("ambient.lift_lobby", [("ambient/lift_lobby", muzak)], stream=True)
sound("ambient.lift_lobby.additions",
      [(f"ambient/lift_lobby_ding{i}", ding_far(i)) for i in range(3)] +
      [("ambient/lift_lobby_cable", cable_creak), ("ambient/lift_lobby_doors", doors_rumble),
       ("ambient/lift_lobby_goingup", going_up)],
      subtitle="A lift arrives somewhere")
sound("event.lift_lobby_ding", [("event/lift_lobby_ding", ding)], subtitle="Ding")
sound("event.lift_lobby_doors", [("event/lift_lobby_doors", doors_rumble)], subtitle="Lift doors close")
sound("entity.lift_attendant.speak", [(f"entity/lift_attendant_hm{i}", attendant_hm(i)) for i in range(3)],
      subtitle="The Lift Attendant speaks")

level("lift_lobby", fog="#3a2a1a", sky="#3a2a1a", ambient="ambient.lift_lobby",
      additions="ambient.lift_lobby.additions", additions_chance=0.003, fixed_time=18000, height=48)
