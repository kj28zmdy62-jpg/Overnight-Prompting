"""Night Supermarket - endless aisles at 3 a.m., freezer cases humming, and an intercom that knows your
name and which aisle you are standing in."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, level, sound, lang, \
    entity_model, entity_tex, ITEM_MODELS
import tex as T
import synth as Y
from content import afterhours_kit as K
from entity_paint import Canvas
from content.entities_core import humanoid, paint_humanoid

S = 32


def floor_tile():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("smf", sigma=1.2) * 0.4 + T.fbm("smff", octaves=3, base=5) * 0.6, "#d6d6d0", "#e4e4de")
    fl = T.rng("smfl").random((S, S))
    img = T.blend(img, T.solid("#8a8a84"), (fl > 0.95) * 0.4)
    img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.15)
    img = T.shade(img, 1 + np.clip(T.noise("smfs", sigma=4) - 0.55, 0, 1) * 0.35)  # floor wax shine
    # cart wheel marks
    for k in range(3):
        y0 = T.rng("smfw", k).random() * S
        img = T.blend(img, T.solid("#5a5a56"), K.line_mask(S, S, 0, y0, S, y0 + 4 - k * 3, 0.8) * 0.25)
    return T.grain(img, "smf", 0.025)


def floor_accent():
    img = floor_tile()
    yy, xx = np.mgrid[0:S, 0:S]
    q = ((xx < 16) & (yy < 16)) | ((xx >= 16) & (yy >= 16))
    return T.blend(img, T.ramp(T.noise("smfa", sigma=1), "#3a3a3c", "#46464a"), q * 0.9)


def ceiling():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("smc", octaves=3, base=5), "#1e1f22", "#26282c")
    corr = ((xx % 4) < 2).astype(float) * 0.15  # corrugated deck
    img = T.shade(img, 1 - corr)
    img[14:18] = T.ramp(T.noise("smcb", sigma=1, shape=(4, S)), "#34363a", "#3c3e42")  # joist
    return T.grain(img, "smc", 0.03)


def wall():
    img = T.ramp(T.fbm("smw", octaves=3, base=4), "#c8c4b8", "#d4d0c4")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.08)  # ribbed metal panel
    img[26:] = T.ramp(T.noise("smwk", sigma=1, shape=(6, S)), "#2a5a3a", "#326a44")
    return K.grime_bottom(T.grain(img, "smw", 0.03), "smwg", 8, 0.2)


def shelf_frame(img, tags=True, seed="sh"):
    """Shelf lips at the bottom of each of the two 16 px shelf levels, with price tags."""
    for y0 in (14, 30):
        img[y0:y0 + 2] = T.ramp(T.noise(seed + str(y0), sigma=1, shape=(2, S)), "#9aa0a4", "#c0c6ca")
        if tags:
            r = T.rng(seed, "tags", y0)
            for k in range(3):
                x = int(r.random() * 26)
                img[y0:y0 + 2, x:x + 5] = T.hexc("#f4f0e0")
                img[y0 + 1, x + 1:x + 4] = T.hexc("#c02020")
    return img


def products(kind, seed):
    def f():
        img = T.ramp(T.noise(seed + "bg", sigma=2, shape=(S, S + 10)), "#2a2a2e", "#36363a")  # shelf back, in shadow
        r = T.rng(seed)
        for level_y in (0, 16):
            x = 0
            while x < S:
                if kind == "empty":
                    break
                if r.random() < 0.06:  # a gap where something was taken
                    x += 4
                    continue
                if kind == "cans":
                    w, h = 5, 9
                    col = ["#b02a2a", "#2a7a3a", "#d8a020", "#2a4a9a", "#e8e4d8"][int(r.random() * 5)]
                    y1 = level_y + 14
                    img[y1 - h:y1, x:x + w] = T.hexc(col)
                    img[y1 - h:y1 - h + 1, x:x + w] = T.hexc("#c8ccd0")
                    img[y1 - 6:y1 - 4, x:x + w] = T.hexc("#f0ece0")
                    img[y1 - h:y1, x + w - 1] = T.hexc(col) * 0.7
                elif kind == "cereal":
                    w, h = 6, 13
                    col = ["#e8c020", "#c83030", "#3a6ac8", "#8a3ac8", "#e87020"][int(r.random() * 5)]
                    y1 = level_y + 14
                    img[y1 - h:y1, x:x + w] = T.hexc(col)
                    img[y1 - h + 2:y1 - h + 4, x + 1:x + w - 1] = T.hexc("#f8f4e8")
                    img[y1 - 6:y1 - 2, x + 2:x + 4] = T.hexc(col) * 0.6
                    img[y1 - h:y1, x + w - 1] = T.hexc(col) * 0.65
                elif kind == "bottles":
                    w, h = 4, 12
                    col = ["#3a8a3a", "#8a2a1a", "#1a1a1a", "#c8a020"][int(r.random() * 4)]
                    y1 = level_y + 14
                    img[y1 - h + 3:y1, x:x + w] = T.hexc(col)
                    img[y1 - h:y1 - h + 3, x + 1:x + 3] = T.hexc(col) * 0.8
                    img[y1 - h, x + 1:x + 3] = T.hexc("#d02020")
                    img[y1 - 7:y1 - 4, x:x + w] = T.hexc("#f0e8d0")
                    img[y1 - h + 4:y1 - 1, x + 1] = T.hexc(col) * 1.4
                elif kind == "bags":
                    w, h = 7, 11
                    col = ["#e8d020", "#2a8ac8", "#c83a2a", "#3aa83a"][int(r.random() * 4)]
                    y1 = level_y + 14
                    bag = T.ramp(T.noise(f"{seed}b{x}{level_y}", sigma=0.8, shape=(h, w)), col, col)
                    bag = T.shade(bag, 0.8 + T.noise(f"{seed}c{x}{level_y}", sigma=0.6, shape=(h, w)) * 0.45)
                    img[y1 - h:y1, x:x + w] = bag
                    img[y1 - h:y1 - h + 1, x:x + w] = T.hexc("#d8d8d8")
                elif kind == "jugs":
                    w, h = 7, 12
                    col = ["#e8701a", "#2a5ac8", "#e8e8e0", "#7a3ab0"][int(r.random() * 4)]
                    y1 = level_y + 14
                    img[y1 - h + 2:y1, x:x + w] = T.hexc(col)
                    img[y1 - h:y1 - h + 2, x + 4:x + 6] = T.hexc("#e8e8e0")
                    img[y1 - h + 3:y1 - h + 6, x + 1:x + 3] = T.hexc("#2a2a2a") * 0.5 + T.hexc(col) * 0.5
                    img[y1 - 6:y1 - 3, x + 1:x + w - 1] = T.hexc("#f0f0f0")
                x += w + (1 if r.random() < 0.5 else 0)
        img = img[:, :S].copy()
        if kind == "empty":
            img = T.blend(img, T.solid("#5a5a5e"), (T.noise(seed + "dust", sigma=1) > 0.7) * 0.4)
            # one can left, right at the back
            img[19:28, 22:27] = T.hexc("#7a1a1a")
            img[19, 22:27] = T.hexc("#c8ccd0")
        img = shelf_frame(img, kind != "empty", seed)
        return T.grain(img, seed, 0.03)
    return f


def overstock():
    img = T.ramp(T.noise("smos", sigma=1.5), "#a5824e", "#b8935a")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.3)
    img = T.rect(img, 0, 14, 32, 17, "#c9b088")
    img[29:] = T.hexc("#9aa0a4")
    return T.grain(img, "smos", 0.04)


def endcap():
    img = T.solid("#c8202a")
    img = T.blend(img, T.solid("#fff04a"), K.centered("SALE", S, S, 4, font=K.F57, scale=1))
    img = T.blend(img, T.solid("#ffffff"), K.centered("2 FOR 1", S, S, 15, font=K.F35))
    img = T.blend(img, T.solid("#ffffff"), K.centered("TODAY", S, S, 23, font=K.F35))
    return T.border(T.grain(img, "smec", 0.02), "#7a1018", 1)


def aisle_sign(n):
    def f():
        img = T.ramp(T.noise("smas", sigma=2), "#1a3a6a", "#1e4274")
        img = T.blend(img, T.solid("#f0f0f0"), K.centered("AISLE", S, S, 3, font=K.F35))
        label = str(n)
        sc = 2 if len(label) <= 2 else 1
        img = T.blend(img, T.solid("#ffffff"), K.centered(label, S, S, 12, font=K.F57, scale=sc))
        return T.border(img, "#c8c8c8", 1)
    return f


def freezer(part):
    yy, xx = np.mgrid[0:S, 0:S]
    if part == "top":
        img = T.ramp(T.noise("smfzt", sigma=2), "#e8f4ff", "#f8fcff")
        img = T.blend(img, T.solid("#1a4a9a"), K.centered("FROZEN", S, S, 7, font=K.F35))
        img = T.blend(img, T.solid("#3a6ac8"), K.centered("FOODS", S, S, 16, font=K.F35))
        img[:2] = T.hexc("#8a9aa8")
        img[-3:] = T.hexc("#6a7a88")
        return img
    inside = T.ramp(T.noise("smfzi" + part, sigma=1.5), "#a8d8ff", "#d8f0ff")
    r = T.rng("smfzp" + part)
    for k in range(10):  # boxes behind the glass
        x, y = int(r.random() * 24), int(r.random() * 24)
        col = ["#e83a3a", "#3a8ae8", "#f0c020", "#ffffff", "#3ab84a"][int(r.random() * 5)]
        inside[y:y + 7, x:x + 5] = T.hexc(col) * 0.85
    inside[::11] = T.hexc("#8aa0b4")
    frost = np.clip(1 - np.minimum.reduce([xx, yy, 31 - xx, 31 - yy]) / 6.0, 0, 1)
    img = T.blend(inside, T.solid("#f4fbff"), frost * 0.6)
    frame = (xx < 2) | (xx > 29) | (yy < 1) | (yy > 30)
    img = T.blend(img, T.solid("#8a9298"), frame.astype(float))
    img = T.rect(img, 26, 6 if part == "mid" else 0, 28, 26 if part == "mid" else 10, "#d0d4d8")
    if part == "low":
        img[24:] = T.ramp(T.noise("smfzk", sigma=1, shape=(8, S)), "#3a3e42", "#4a4e52")
    return T.grain(img, "smfz" + part, 0.02)


def strip_tube():
    img = T.solid("#ffffff")
    return T.border(img, "#c8ccd0", 3)


def strip_off():
    return T.border(T.ramp(T.noise("smso", sigma=2), "#6a6e72", "#7a7e82"), "#c8ccd0", 3)


def housing():
    return T.grain(T.ramp(T.noise("smhs", sigma=1), "#c0c4c8", "#d0d4d8"), "smhs", 0.02)


def window():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("smwin", sigma=3), "#020406", "#06090e")
    # the parking lot: distant sodium lamps, nothing else
    r = T.rng("smwinl")
    for k in range(2):
        x, y = int(r.random() * 28) + 2, 14 + int(r.random() * 6)
        img = T.blend(img, T.solid("#ffaa40"), K.glow(K.disc(S, S, x, y, 0.6).astype(float), 1.0) * 0.6)
    streak = np.clip(1 - np.abs((xx + yy * 0.4) % 30 - 9) / 2.5, 0, 1) * 0.18
    img = T.blend(img, T.solid("#8a9aa8"), streak)
    img[:, :1] = T.hexc("#7a8288")
    img[:1] = T.hexc("#7a8288")
    return img


def auto_door():
    img = window()
    img[:, 15:17] = T.hexc("#5a6268")
    img[14:16, 4:12] = T.hexc("#8a9298")
    img[14:16, 20:28] = T.hexc("#8a9298")
    return img


def conveyor():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("smcv", sigma=0.6), "#141414", "#1e1e1e")
    img = T.shade(img, 1 - ((xx % 6) == 0) * 0.3)
    return img


def register_body():
    img = T.ramp(T.noise("smrb", sigma=1.5), "#d8d4c8", "#e4e0d4")
    img[:3] = T.hexc("#3a3a3e")
    return T.grain(img, "smrb", 0.02)


def register_screen():
    img = T.solid("#06100a")
    img = T.blend(img, T.solid("#3aff7a"), K.centered("0.00", S, S, 6, font=K.F57))
    img = T.blend(img, T.solid("#3aff7a"), K.centered("CLOSED", S, S, 18, font=K.F35) * 0.7)
    return T.border(img, "#2a2a2a", 3)


def lane_light(n, lit):
    def f():
        img = T.solid("#1a1a1a")
        col = "#3aff5a" if lit else "#5a1a1a"
        img = T.blend(img, T.solid(col), K.centered(str(n), S, S, 9, font=K.F57, scale=2))
        return T.border(img, "#d8d8d8", 2)
    return f


def wire():
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx % 4) == 0) | ((yy % 4) == 0)).astype(float)
    rgb = T.ramp(T.noise("smwr", sigma=1), "#9aa0a6", "#c8ced4")
    return rgb, a


def red_plastic():
    return T.grain(T.ramp(T.noise("smrp", sigma=1.5), "#b02020", "#c82a2a"), "smrp", 0.02)


def produce(kind):
    def f():
        img = T.ramp(T.noise("smpr" + kind, sigma=2), "#2a1a0e", "#3a2814")
        r = T.rng("smpr" + kind)
        col = {"apples": ("#b81e1e", "#d83a2a"), "oranges": ("#e87a1a", "#f8962a"), "limes": ("#5aa82a", "#7ac83a"),
               "rotten": ("#4a3a1e", "#6a5426")}[kind]
        for k in range(46):
            x, y = r.random() * S, r.random() * S
            m = K.disc(S, S, x, y, 2.6 + r.random())
            img = T.blend(img, T.ramp(T.noise(f"smpf{kind}{k}", sigma=1), *col), m.astype(float))
            img = T.blend(img, T.solid("#ffffff"), K.disc(S, S, x - 1, y - 1, 0.6).astype(float) * 0.4)
        return T.grain(img, "smpr" + kind, 0.03)
    return f


def crate_wood():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("smcw", octaves=3, base=3), "#8a6a40", "#a07c4c")
    return T.grain(T.shade(img, 1 - ((yy % 8) == 0) * 0.35), "smcw", 0.04)


tex("smkt_floor", floor_tile)
tex("smkt_floor_accent", floor_accent)
tex("smkt_ceiling", ceiling)
tex("smkt_wall", wall)
for kind in ("cans", "cereal", "bottles", "bags", "jugs", "empty"):
    tex(f"smkt_shelf_{kind}", products(kind, "smp" + kind))
tex("smkt_overstock", overstock)
tex("smkt_endcap", endcap)
for n in range(1, 14):
    tex(f"smkt_aisle_{n}", aisle_sign(n))
tex("smkt_aisle_unknown", aisle_sign("??"))
for p in ("top", "mid", "low"):
    tex(f"smkt_freezer_{p}", (lambda p=p: freezer(p)))
tex("smkt_tube", strip_tube)
tex("smkt_tube_off", strip_off)
tex("smkt_housing", housing)
tex("smkt_window", window)
tex("smkt_auto_door", auto_door)
tex("smkt_conveyor", conveyor)
tex("smkt_register_body", register_body)
tex("smkt_register_screen", register_screen)
for n in range(1, 9):
    tex(f"smkt_lane_{n}", lane_light(n, False))
tex("smkt_lane_open", lane_light(4, True))
tex("smkt_wire", wire)
tex("smkt_red_plastic", red_plastic)
for kind in ("apples", "oranges", "limes", "rotten"):
    tex(f"smkt_produce_{kind}", produce(kind))
tex("smkt_crate_wood", crate_wood)

cube("smkt_floor", "smkt_floor", display="Supermarket Floor", sound="stone")
cube("smkt_floor_accent", "smkt_floor_accent", display="Supermarket Floor (Accent)", sound="stone")
cube("smkt_ceiling", "smkt_ceiling", display="Open Deck Ceiling", sound="metal", flat=True)
cube("smkt_wall", "smkt_wall", display="Metal Panel Wall", sound="metal")
for kind in ("cans", "cereal", "bottles", "bags", "jugs", "empty"):
    cube(f"smkt_shelf_{kind}", f"smkt_shelf_{kind}", display="Shelving", sound="metal")
cube_faces("smkt_overstock", top="smkt_overstock", bottom="smkt_overstock", side="smkt_overstock",
           display="Overstock", sound="wool")
cube("smkt_endcap", "smkt_endcap", display="Endcap Display", sound="metal")
for n in range(1, 14):
    cube(f"smkt_aisle_{n}", f"smkt_aisle_{n}", display=f"Aisle {n}", sound="metal", light=4, emissive=True)
cube("smkt_aisle_unknown", "smkt_aisle_unknown", display="Aisle ??", sound="metal", light=4, emissive=True)
cube("smkt_freezer_top", "smkt_freezer_top", display="Freezer Case", sound="metal", light=8, emissive=True)
cube("smkt_freezer_mid", "smkt_freezer_mid", display="Freezer Case", sound="glass", light=10, emissive=True)
cube("smkt_freezer_low", "smkt_freezer_low", display="Freezer Case", sound="glass", light=6, emissive=True)
cube("smkt_window", "smkt_window", display="Storefront Window", sound="glass")
exit_block("smkt_auto_door", "smkt_auto_door", "DOOR", display="Automatic Door", sound="glass")

for name, tube in (("smkt_light", "smkt_tube"), ("smkt_light_off", "smkt_tube_off")):
    shaped(name, [(0, 12, 5, 16, 16, 11, {"all": "housing", "down": "tube"})],
           {"housing": "smkt_housing", "tube": tube}, display="Hanging Strip Light",
           light=15 if name == "smkt_light" else None, emissive=True if name == "smkt_light" else None,
           unbreakable=True, collides=False, sound="glass", render="solid")

shaped("smkt_cart", [
    (2, 5, 1, 14, 13, 13, "wire"), (3, 1, 13, 13, 2, 15, "wire"), (2, 13, 13, 14, 14, 15, "red"),
    (3, 0, 2, 4, 1, 3, "wheel"), (12, 0, 2, 13, 1, 3, "wheel"), (3, 0, 12, 4, 1, 13, "wheel"), (12, 0, 12, 13, 1, 13, "wheel"),
    (3, 1, 2, 4, 5, 3, "frame"), (12, 1, 2, 13, 5, 3, "frame"), (3, 1, 12, 4, 5, 13, "frame"), (12, 1, 12, 13, 5, 13, "frame"),
], {"wire": "smkt_wire", "red": "smkt_red_plastic", "wheel": "smkt_conveyor", "frame": "smkt_housing"},
    display="Shopping Cart", unbreakable=True)
shaped("smkt_cart_full", [
    (2, 5, 1, 14, 13, 13, "wire"), (3, 6, 2, 13, 12, 12, "goods"), (2, 13, 13, 14, 14, 15, "red"),
    (3, 0, 2, 4, 1, 3, "wheel"), (12, 0, 2, 13, 1, 3, "wheel"), (3, 0, 12, 4, 1, 13, "wheel"), (12, 0, 12, 13, 1, 13, "wheel"),
    (3, 1, 2, 4, 5, 3, "frame"), (12, 1, 2, 13, 5, 3, "frame"), (3, 1, 12, 4, 5, 13, "frame"), (12, 1, 12, 13, 5, 13, "frame"),
], {"wire": "smkt_wire", "red": "smkt_red_plastic", "wheel": "smkt_conveyor", "frame": "smkt_housing",
    "goods": "smkt_shelf_cans"}, display="Abandoned Cart", kind="pickup",
    item="noclip:canned_beans*3,noclip:energy_bar*2,noclip:almond_water*1", sound="metal")
shaped("smkt_checkout", [
    (0, 0, 2, 16, 12, 14, {"all": "body", "up": "belt"}),
], {"body": "smkt_register_body", "belt": "smkt_conveyor"}, display="Checkout Counter", unbreakable=True)
shaped("smkt_register", [
    (0, 0, 2, 16, 12, 14, "body"), (4, 12, 4, 12, 16, 9, {"all": "body", "north": "screen"}),
], {"body": "smkt_register_body", "screen": "smkt_register_screen"}, display="Register", unbreakable=True,
    light=3, emissive=True)
for n in range(1, 9):
    shaped(f"smkt_lane_light_{n}", [(7, 0, 7, 9, 10, 9, "pole"), (4, 10, 4, 12, 16, 12, "num")],
           {"pole": "smkt_housing", "num": f"smkt_lane_{n}"}, display=f"Lane {n}", unbreakable=True, collides=False)
shaped("smkt_lane_light_open", [(7, 0, 7, 9, 10, 9, "pole"), (4, 10, 4, 12, 16, 12, "num")],
       {"pole": "smkt_housing", "num": "smkt_lane_open"}, display="Lane Open", unbreakable=True, collides=False,
       light=9, emissive=True)
for kind in ("apples", "oranges", "limes", "rotten"):
    shaped(f"smkt_bin_{kind}", [(0, 0, 0, 16, 10, 16, {"all": "wood", "up": "fruit"})],
           {"wood": "smkt_crate_wood", "fruit": f"smkt_produce_{kind}"}, display="Produce Bin", unbreakable=True)

# ------------------------------------------------------------------ the night stocker


def stocker_tex():
    c = Canvas()
    skin = lambda w, h: T.ramp(T.noise(f"stsk{w}{h}", shape=(h, w), sigma=1.2), "#c8a888", "#d8b898")

    def face(w, h):
        # no face: a price sticker where the face should be
        img = T.ramp(T.noise("stface", shape=(h, w), sigma=1), "#f08a1a", "#f89a2a")
        img[0:1] = T.hexc("#2a1e14")
        img[3:5, 1:7] = T.hexc("#1a1a1a")
        img[3, 4] = T.hexc("#f89a2a")
        return img

    def head_side(w, h):
        img = skin(w, h)
        img[0:2] = T.hexc("#2a1e14")
        return img

    def vest(w, h):
        img = T.ramp(T.noise(f"stv{w}{h}", shape=(h, w), sigma=0.8), "#b01e24", "#c02830")
        if w == 8 and h == 12:
            img[:, 3:5] = T.hexc("#e8e4d8")  # shirt under the vest
            img[3:5, 5:7] = T.hexc("#f0f0f0")  # name tag, blank
        return img

    def arm(w, h):
        img = T.ramp(T.noise(f"sta{w}{h}", shape=(h, w), sigma=0.8), "#e8e4d8", "#f0ece0")
        if h > 6:
            img[-3:] = skin(w, 3)
        return img

    def khaki(w, h):
        img = T.ramp(T.noise(f"stk{w}{h}", shape=(h, w), sigma=0.8), "#a8946a", "#b8a47a")
        img[-2:] = T.hexc("#1a1612")
        return img

    paint_humanoid(c, head_side, vest, arm, khaki, face=face)
    return c.out()


entity_model("night_stocker", humanoid(arm_len=14, leg_len=13))
entity_tex("night_stocker", stocker_tex)
lang("entity.noclip.night_stocker", "Night Stocker")
lang("item.noclip.night_stocker_spawn_egg", "Night Stocker Spawn Egg")
ITEM_MODELS["night_stocker_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def store_tone():
    d = 30.0
    t = Y.t_axis(d)
    # compressors cycling: on for a while, off, on again
    comp_env = np.clip(np.sin(2 * np.pi * t / 15.0) * 3, -1, 1) * 0.5 + 0.5
    comp = (Y.hum(d, 60.0, "smcomp", buzz=0.1, harmonics=(1, 2, 3)) * 0.25 + Y.bp(Y.white(d, "smfan"), 150, 900) * 0.3)
    comp = Y.lp(comp, 900) * Y.lp(comp_env, 2)
    tubes = Y.hp(Y.hum(d, 60.0, "smtube", buzz=0.6, harmonics=(2, 4, 6, 8)), 300) * 0.12
    room = Y.lp(Y.brown(d, "smroom"), 120) * 0.25
    x = comp + tubes + room
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.002), "stereo"


def cart_rattle():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "smcr"), 500, 5000) * (0.4 + 0.6 * (np.sin(2 * np.pi * 13 * t) > 0.3))
    x *= np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(x, 3.2, 0.75, "smcr"), 0.5)


def can_drop():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.impact("smcan", 0.25, ring=0.9), 0.05)
    t0 = 0.4
    for k in range(9):  # rolling
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, np.sin(2 * np.pi * 1800 * tt) * np.exp(-tt * 60) * 0.3, t0 + k * 0.17, 1 - k * 0.1)
    return Y.norm(Y.reverb(out, 2.5, 0.6, "smcan"), 0.7)


def scanner():
    out = np.zeros(int(1.5 * Y.SR))
    n = int(0.12 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, np.sin(2 * np.pi * 2950 * tt) * 0.6, 0.1)
    return Y.norm(Y.reverb(out, 2.0, 0.7, "smscan"), 0.5)


def freezer_thunk():
    out = np.zeros(int(2.0 * Y.SR))
    Y.place(out, Y.impact("smfzth", 0.4), 0.05)
    hiss = Y.bp(Y.white(0.5, "smfzh"), 2000, 8000) * np.exp(-np.arange(int(0.5 * Y.SR)) / (0.1 * Y.SR)) * 0.3
    Y.place(out, hiss, 0.05)
    return Y.norm(Y.reverb(out, 2.0, 0.6, "smfzth"), 0.6)


def feedback():
    d = 1.8
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * 2200 * t * (1 + 0.01 * t)) * np.sin(np.pi * t / d) ** 2 * 0.4
    x += Y.bp(Y.white(d, "smfb"), 300, 3000) * 0.15 * (t < 0.3)
    return Y.norm(Y.reverb(x, 2.5, 0.6, "smfb"), 0.4)


def intercom(seed, syl, notes):
    def f():
        c = K.chime([K.hz(n) for n in notes], gap=0.4, decay=1.4, seed=seed)
        v = K.pa_voice(seed + "v", syllables=syl, f0=112, rate=0.16, band=(350, 2800))
        click = Y.bp(Y.white(0.05, seed + "k"), 1000, 6000) * 0.5
        out = np.zeros(len(c) + len(v) + int(0.6 * Y.SR))
        Y.place(out, click, 0.0)
        Y.place(out, c, 0.1, 0.8)
        Y.place(out, v, 1.1, 0.85)
        Y.place(out, click, 1.2 + len(v) / Y.SR)
        return Y.norm(Y.reverb(out, 2.8, 0.55, seed), 0.85)
    return f


def price_gun():
    out = np.zeros(int(1.0 * Y.SR))
    for k, at in enumerate((0.05, 0.3)):
        n = int(0.08 * Y.SR)
        tt = np.arange(n) / Y.SR
        click = Y.bp(Y.white(0.08, f"stpg{k}"), 800, 6000)[:n] * np.exp(-tt * 80) + np.sin(2 * np.pi * 600 * tt) * np.exp(-tt * 70) * 0.5
        Y.place(out, click, at, 1.0)
    return Y.norm(out, 0.8)


sound("ambient.supermarket", [("ambient/supermarket", store_tone)], stream=True)
sound("ambient.supermarket.additions",
      [("ambient/supermarket_cart", cart_rattle), ("ambient/supermarket_can", can_drop),
       ("ambient/supermarket_scan", scanner), ("ambient/supermarket_freezer", freezer_thunk),
       ("ambient/supermarket_feedback", feedback)],
      subtitle="The store at 3 a.m.")
sound("event.supermarket_intercom", [("event/supermarket_intercom", intercom("smic", 14, ["G5", "E5"]))],
      subtitle="The intercom")
sound("event.supermarket_cleanup", [("event/supermarket_cleanup", intercom("smcu", 9, ["E5", "E5", "C5"]))],
      subtitle="Clean-up announced")
sound("entity.night_stocker.gun", [("entity/night_stocker_gun", price_gun)], subtitle="A price gun clicks")

level("supermarket", fog="#1c1e20", sky="#1c1e20", ambient="ambient.supermarket",
      additions="ambient.supermarket.additions", additions_chance=0.005, fixed_time=18000, height=48)
