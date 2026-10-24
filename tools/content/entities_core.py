"""Core Backrooms entities: models and textures."""
import numpy as np
from assets import entity_model, entity_tex, part, lang
from entity_paint import Canvas, skin, flat
import tex as T

PI = np.pi


def humanoid(head_extra=(), arm_len=12, leg_len=12, body_extra=(), slim=False):
    """Standard 64x64 player-skin humanoid. Part names drive the shared walk animation."""
    aw = 3 if slim else 4
    leg_y = 12
    return [
        part("head", (0, 0, 0), cubes=[(0, 0, -4, -8, -4, 8, 8, 8)] + list(head_extra)),
        part("body", (0, 0, 0), cubes=[(16, 16, -4, 0, -2, 8, 12, 4)] + list(body_extra)),
        part("right_arm", (-5, 2, 0), cubes=[(40, 16, -aw + 1, -2, -2, aw, arm_len, 4)]),
        part("left_arm", (5, 2, 0), cubes=[(32, 48, -1, -2, -2, aw, arm_len, 4)]),
        part("right_leg", (-1.9, leg_y, 0), cubes=[(0, 16, -2, 0, -2, 4, leg_len, 4)]),
        part("left_leg", (1.9, leg_y, 0), cubes=[(16, 48, -2, 0, -2, 4, leg_len, 4)]),
    ]


def paint_humanoid(c, head, body, arm, leg, face=None, slim=False, legs2=None, arms2=None):
    aw = 3 if slim else 4
    c.box(0, 0, 8, 8, 8, all=head, front=face or head)
    c.box(16, 16, 8, 12, 4, all=body)
    c.box(40, 16, aw, 12, 4, all=arm)
    c.box(32, 48, aw, 12, 4, all=arms2 or arm)
    c.box(0, 16, 4, 12, 4, all=leg)
    c.box(16, 48, 4, 12, 4, all=legs2 or leg)


# ------------------------------------------------------------------ Faceling

def faceling_tex(variant, shirt, trousers, skin_c, hair):
    def f():
        c = Canvas()

        def head_side(w, h):
            img = T.ramp(T.noise(f"fh{variant}{w}", shape=(h, w), sigma=1), skin_c, skin_c)
            img[:2] = T.hexc(hair)
            return img

        def face(w, h):
            # nothing. just skin where a face should be, with the faintest dents
            img = T.ramp(T.noise(f"ff{variant}", shape=(h, w), sigma=1.5), skin_c, skin_c)
            img = T.shade(img, 1 - ((np.mgrid[0:h, 0:w][0] == 3) & (np.mgrid[0:h, 0:w][1] % 3 == 1)) * 0.06)
            img[:1] = T.hexc(hair)
            return img

        def shirt_p(w, h):
            img = T.grain(T.solid(shirt, shape=(h, w)), f"sh{variant}{w}{h}", 0.06)
            return img

        def arm_p(w, h):
            img = shirt_p(w, h)
            img[-3:] = T.hexc(skin_c)
            return img

        def leg_p(w, h):
            img = T.grain(T.solid(trousers, shape=(h, w)), f"tr{variant}{w}{h}", 0.06)
            img[-2:] = T.hexc("#1d1b1a")
            return img

        paint_humanoid(c, head_side, shirt_p, arm_p, leg_p, face=face)
        c.box(0, 0, 8, 8, 8, up=lambda w, h: T.grain(T.solid(hair, shape=(h, w)), "hairtop", 0.08))
        return c.out()
    return f


entity_model("faceling", humanoid())
FACELINGS = [
    ("#d9c8b4", "#c8b8a0", "#e3d0b8", "#3a2a1a"),
    ("#5d6b7a", "#2c2f36", "#c9a88a", "#1a1410"),
    ("#8a3a32", "#3a3530", "#8a6448", "#0e0c0a"),
    ("#e8e6e0", "#545a62", "#efdcc8", "#6a4a2a"),
]
for i, (sh, tr, sk, hr) in enumerate(FACELINGS):
    entity_tex(f"faceling_{i}", faceling_tex(i, sh, tr, sk, hr))

# ------------------------------------------------------------------ Mannequin (Dead Mall)


def mannequin_tex():
    c = Canvas()
    plastic = lambda w, h: T.ramp(T.noise(f"mq{w}{h}", shape=(h, w), sigma=2), "#d8d4cc", "#ece8e0")

    def joint(w, h):
        img = plastic(w, h)
        img[h // 2] = T.hexc("#9a968e")
        return img

    paint_humanoid(c, plastic, plastic, joint, joint)
    return c.out()


entity_model("mannequin", humanoid())
entity_tex("mannequin", mannequin_tex)

# ------------------------------------------------------------------ Shadow figure (the one at the end of the hall)


def shadow_tex():
    c = Canvas()
    dark = lambda w, h: T.ramp(T.noise(f"sd{w}{h}", shape=(h, w), sigma=1), "#030303", "#0d0c0e")
    paint_humanoid(c, dark, dark, dark, dark)
    return c.out()


def shadow_glow():
    c = Canvas()

    def eyes(w, h):
        img = np.zeros((h, w, 3))
        img[4, 2] = img[4, 5] = T.hexc("#f0e6c8")
        return img

    c.box(0, 0, 8, 8, 8, front=eyes)
    rgb, a = c.out()
    a = (rgb.sum(axis=2) > 0.1).astype(float)
    return rgb, a


entity_model("shadow_figure", humanoid(arm_len=15, leg_len=15))
entity_tex("shadow_figure", shadow_tex)
entity_tex("shadow_figure_glow", shadow_glow)

# ------------------------------------------------------------------ Skin-Stealer (fallback skin)


def skinstealer_tex():
    c = Canvas()
    pale = skin("ss", "#c9b6a2", "#ddcab4", sigma=0.7, spots="#8a5a50")

    def face(w, h):
        img = pale(w, h)
        img[3, 1:3] = T.hexc("#120a08")
        img[3, 5:7] = T.hexc("#120a08")
        img[6, 1:7] = T.hexc("#3a1a14")
        img[5, 0] = img[5, 7] = T.hexc("#3a1a14")
        return img

    def stitched(w, h):
        img = pale(w, h)
        img[:, w // 2] = T.hexc("#5a2a22")
        return img

    paint_humanoid(c, pale, stitched, pale, pale, face=face)
    return c.out()


entity_model("skin_stealer", humanoid(arm_len=16, leg_len=14))
entity_tex("skin_stealer", skinstealer_tex)

# ------------------------------------------------------------------ Partygoer (Level Fun =))


def partygoer_tex():
    c = Canvas()
    yellow = lambda w, h: T.grain(T.ramp(T.noise(f"pg{w}{h}", shape=(h, w), sigma=2), "#e8c838", "#f4d84a"), f"pg{w}", 0.04)

    def face(w, h):
        img = yellow(w, h)
        ink = T.hexc("#141008")
        img[2:4, 1] = ink
        img[2:4, 6] = ink
        for x in range(0, 8):
            y = 5 + int(round(1.2 * np.sin(np.pi * x / 7)))
            img[min(y, h - 1), x] = ink
        img[5, 0] = img[5, 7] = ink
        return img

    paint_humanoid(c, yellow, yellow, yellow, yellow, face=face)
    stripes = lambda w, h: np.where(((np.mgrid[0:h, 0:w][0] + np.mgrid[0:h, 0:w][1]) % 4 < 2)[..., None],
                                     T.hexc("#e04070"), T.hexc("#40a0e0"))
    c.box(32, 0, 4, 6, 4, all=stripes)
    return c.out()


entity_model("partygoer", humanoid(head_extra=[(32, 0, -2, -14, -2, 4, 6, 4)], arm_len=13))
entity_tex("partygoer", partygoer_tex)

# ------------------------------------------------------------------ Hound


def hound_parts():
    return [
        part("body", (0, 11, 0), cubes=[(0, 0, -3.5, -3, -8, 7, 6, 16)]),
        part("head", (0, 10, -8), cubes=[(46, 0, -3, -3.5, -6, 6, 7, 6)]),
        part("jaw", (0, 2.5, -2), parent="head", cubes=[(46, 13, -2.5, 0, -4, 5, 2, 4)]),
        part("leg_fl", (3.2, 10, -6), cubes=[(0, 22, -1.5, -1, -1.5, 3, 15, 3)]),
        part("leg_fr", (-3.2, 10, -6), cubes=[(12, 22, -1.5, -1, -1.5, 3, 15, 3)]),
        part("leg_bl", (3, 10, 6), cubes=[(24, 22, -1.5, -1, -1.5, 3, 15, 3)]),
        part("leg_br", (-3, 10, 6), cubes=[(36, 22, -1.5, -1, -1.5, 3, 15, 3)]),
    ]


def hound_tex():
    c = Canvas(128, 64)
    hide = skin("hd", "#1a1816", "#2e2a26", sigma=0.6, spots="#463e36")

    def face(w, h):
        img = T.ramp(T.noise("hface", shape=(h, w), sigma=1), "#b8aa98", "#cbbfae")
        img[2:4, 1:3] = T.hexc("#050404")
        img[2:4, 4:6] = T.hexc("#050404")
        img[5:, 1:5] = T.hexc("#2a1210")
        return img

    c.box(0, 0, 7, 6, 16, all=hide)
    c.box(46, 0, 6, 7, 6, all=hide, front=face)
    c.box(46, 13, 5, 2, 4, all=lambda w, h: T.solid("#3a1a16", shape=(h, w)))
    for u in (0, 12, 24, 36):
        c.box(u, 22, 3, 15, 3, all=hide)
    return c.out()


entity_model("hound", hound_parts(), tex_size=(128, 64))
entity_tex("hound", hound_tex)

# ------------------------------------------------------------------ Deathmoth


def moth_parts():
    return [
        part("body", (0, 14, 0), cubes=[(0, 0, -2.5, -2.5, -4, 5, 5, 12)]),
        part("head", (0, 14, -4), cubes=[(0, 17, -2, -2, -3, 4, 4, 3)]),
        part("antenna_l", (1, -1.5, -3), parent="head", rot=(-0.6, 0.3, 0), cubes=[(14, 17, 0, -6, 0, 1, 6, 1)]),
        part("antenna_r", (-1, -1.5, -3), parent="head", rot=(-0.6, -0.3, 0), cubes=[(18, 17, -1, -6, 0, 1, 6, 1)]),
        part("wing_l", (2.5, 12, -2), cubes=[(0, 32, 0, 0, -6, 18, 1, 14)]),
        part("wing_r", (-2.5, 12, -2), cubes=[(0, 48, -18, 0, -6, 18, 1, 14)]),
        part("leg_fl", (2, 16.5, -2), cubes=[(24, 17, 0, 0, 0, 1, 7, 1)]),
        part("leg_fr", (-2, 16.5, -2), cubes=[(28, 17, -1, 0, 0, 1, 7, 1)]),
        part("leg_bl", (2, 16.5, 3), cubes=[(32, 17, 0, 0, 0, 1, 7, 1)]),
        part("leg_br", (-2, 16.5, 3), cubes=[(36, 17, -1, 0, 0, 1, 7, 1)]),
    ]


def moth_tex():
    c = Canvas(64, 64)
    fuzz = skin("mf", "#6a5e4e", "#8a7c66", sigma=0.5)
    c.box(0, 0, 5, 5, 12, all=fuzz)

    def head(w, h):
        img = fuzz(w, h)
        img[1:3, 0] = T.hexc("#101010")
        img[1:3, -1] = T.hexc("#101010")
        return img

    c.box(0, 17, 4, 4, 3, all=fuzz, front=head)
    for u in (14, 18, 24, 28, 32, 36):
        c.box(u, 17, 1, 7 if u >= 24 else 6, 1, all="#3a3228")

    def wing(seed, mirror):
        def f(w, h):
            yy, xx = np.mgrid[0:h, 0:w]
            xs = (w - 1 - xx) if mirror else xx
            base = T.ramp(T.fbm(f"wing{seed}", shape=(h, w), octaves=3, base=2), "#7a6c58", "#a8987e")
            # eye spot that stares back
            d = np.sqrt((xs - w * 0.62) ** 2 + (yy - h * 0.45) ** 2)
            base = T.blend(base, T.solid("#e8dcc0", shape=(h, w)), (d < 3.2).astype(float))
            base = T.blend(base, T.solid("#141008", shape=(h, w)), (d < 1.8).astype(float))
            veins = ((xs + yy) % 5 == 0).astype(float) * 0.15
            base = T.shade(base, 1 - veins)
            edge = ((yy - h / 2) ** 2 / (h / 2) ** 2 + (xs / w) ** 2 * 0.9) < 1.0
            return base, edge.astype(float)
        return f

    for v, mirror in ((32, False), (48, True)):
        for face, (x, y, w, h) in __import__("assets").box_uv(0, v, 18, 1, 14).items():
            if face in ("up", "down"):
                img, al = wing(v, mirror)(w, h)
                c.fill_rect(x, y, w, h, img, al)
    return c.out()


entity_model("deathmoth", moth_parts())
entity_tex("deathmoth", moth_tex)

# ------------------------------------------------------------------ Smiler (billboard, no model)


def smiler_tex():
    w, h = 64, 32
    rgb = np.zeros((h, w, 3))
    a = np.zeros((h, w))
    yy, xx = np.mgrid[0:h, 0:w]
    glow = T.hexc("#f6f2e4")
    for ex in (22, 42):
        d = np.sqrt(((xx - ex) / 3.2) ** 2 + ((yy - 9) / 2.2) ** 2)
        m = d < 1
        rgb[m] = glow
        a[m] = 1
    # the grin: a wide arc of teeth
    cy = 4.0
    r_out, r_in = 26.0, 21.0
    d = np.sqrt((xx - 32) ** 2 + (yy - cy) ** 2)
    arc = (d < r_out) & (d > r_in) & (yy > 14)
    teeth_gap = ((xx % 4) == 0) & arc
    rgb[arc] = glow
    a[arc] = 1
    rgb[teeth_gap] = T.hexc("#8a8678")
    return rgb, a


entity_tex("smiler", smiler_tex)

for name, disp in (("faceling", "Faceling"), ("mannequin", "Mannequin"), ("shadow_figure", "Wanderer?"),
                   ("skin_stealer", "Skin-Stealer"), ("partygoer", "Partygoer"), ("hound", "Hound"),
                   ("deathmoth", "Deathmoth"), ("smiler", "Smiler")):
    lang(f"entity.noclip.{name}", disp)
    lang(f"item.noclip.{name}_spawn_egg", f"{disp} Spawn Egg")

from assets import ITEM_MODELS
for name in ("faceling", "mannequin", "shadow_figure", "skin_stealer", "partygoer", "hound", "deathmoth", "smiler"):
    ITEM_MODELS[f"{name}_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}
