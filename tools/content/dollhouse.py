"""Giant's Dollhouse - you are two centimetres tall in a house built for something enormous.
Chair legs like pillars, a table for a sky, toys the size of buildings. Something lives here.
When the floor starts to shake, get under something."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, lang, entity_model, entity_tex, part, \
    ITEM_MODELS, pillar
import tex as T
import synth as Y
from content.rb_shared import text, text_center, wood, line, disc, ellipse, split
from content.entities_core import paint_humanoid
from entity_paint import Canvas

S = 32

# ------------------------------------------------------------------ the house


def floorboard(joint=False):
    """One giant floorboard per block: grain running along x, a deep dark gap at its edge."""
    img = wood("dhfloor" + str(joint), "#8a6038", "#a87848", grain_dir="h", rings=3)
    img[0:2] = T.hexc("#2a1a0e")
    img[2] = img[2] * 0.7
    if joint:
        img[:, 0:2] = T.hexc("#2a1a0e")
        img[:, 2] = img[:, 2] * 0.7
    # waxed sheen and the odd scratch, which at this size are trenches
    img = T.shade(img, 0.92 + T.noise("dhfsheen" + str(joint), sigma=4) * 0.14)
    img = T.cracks(img, "dhfscr" + str(joint), "#5a3a20", count=1, alpha=0.4)
    return T.grain(img, "dhfloor" + str(joint), 0.025)


def wallpaper(quadrant):
    """A 64x64 repeat of huge cabbage roses on duck-egg blue, spread over 2x2 blocks."""
    H = 64
    yy, xx = np.mgrid[0:H, 0:H]
    base = T.ramp(T.fbm("dhwp", shape=(H, H), octaves=3, base=8) * 0.4 + 0.3, "#9ab8b4", "#a8c4c0", "#b4ccc8")
    img = base.copy()
    for (cx, cy) in ((0, 0), (32, 32)):
        dx = np.minimum(np.abs(xx - cx), H - np.abs(xx - cx))
        dy = np.minimum(np.abs(yy - cy), H - np.abs(yy - cy))
        r = np.sqrt(dx ** 2 + dy ** 2)
        ang = np.arctan2(dy, dx)
        petals = r < (9 + 2.5 * np.sin(ang * 5 + r * 0.6))
        img[petals] = T.ramp(np.clip(r / 11.0, 0, 1), "#7a2a3a", "#c8606a", "#e8a0a0")[petals]
        swirl = petals & ((np.sin(r * 1.6 + ang * 2) > 0.6))
        img[swirl] = img[swirl] * 0.75
        leaf = (np.abs(dx - 12) < 4) & (np.abs(dy + 0) < 2.5) | (np.abs(dy - 12) < 4) & (np.abs(dx) < 2.5)
        leaf &= ~petals
        img[leaf] = T.hexc("#4a6a4a")
    stripe = ((xx % 32) == 31)
    img[stripe] = img[stripe] * 0.9
    img = T.shade(img, 0.92 + T.noise("dhwpn", shape=(H, H), sigma=2) * 0.12)
    img = T.grain(img, "dhwp", 0.02)
    return split(img, 2, 2)[quadrant]


def baseboard():
    img = T.ramp(T.noise("dhbase", sigma=3), "#e8e4da", "#f2eee6")
    img[0:3] = T.hexc("#d0ccc0")
    img[3] = T.hexc("#b8b4a8")
    img[30:] = T.hexc("#a8a498")
    chips = T.noise("dhbchip", sigma=0.8) > 0.88
    img = T.blend(img, T.solid("#9a8a70"), chips.astype(float) * 0.6)
    return T.grain(img, "dhbase", 0.02)


def plaster():
    return T.grain(T.ramp(T.fbm("dhplast", octaves=3, base=6), "#e2ddd2", "#ece8de"), "dhplast", 0.02)


def frame_white():
    img = T.ramp(T.noise("dhframe", sigma=2), "#e8e4da", "#f4f0e8")
    return T.grain(T.border(img, "#c8c4b8", 1), "dhframe", 0.02)


def window_glass():
    """Late-afternoon light through frosted glass. Too bright to see what is outside."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(np.clip(yy / 40.0 + T.noise("dhglass", sigma=4) * 0.2, 0, 1), "#fff8e0", "#f0f0e8", "#e0ecf0")
    return img


def rug():
    """Wool rug, but every tuft is as thick as your arm."""
    yy, xx = np.mgrid[0:S, 0:S]
    tufts = T.noise("dhrug", sigma=0.9)
    img = T.ramp(tufts, "#5a2a3a", "#7a3a4a", "#8a4a58")
    loops = ((xx % 4 == 0) | (yy % 4 == 0)).astype(float) * 0.12
    return T.grain(T.shade(img, 1 - loops), "dhrug", 0.04)


def mousehole():
    img = T.solid("#020101")
    img = T.blend(img, T.solid("#0e0806"), T.noise("dhmh", sigma=3) * 0.7)
    return img


tex("dh_floor", lambda: floorboard(False))
tex("dh_floor_joint", lambda: floorboard(True))
for i, q in enumerate(("tl", "tr", "bl", "br")):
    tex(f"dh_wall_{q}", (lambda k: lambda: wallpaper(k))(i))
tex("dh_baseboard", baseboard)
tex("dh_plaster", plaster)
tex("dh_frame", frame_white)
tex("dh_glass", window_glass)
tex("dh_rug", rug)
tex("dh_mousehole", mousehole)

cube("dh_floor", "dh_floor", display="Giant Floorboard", sound="wood")
cube("dh_floor_joint", "dh_floor_joint", display="Giant Floorboard", sound="wood")
for q in ("tl", "tr", "bl", "br"):
    cube(f"dh_wall_{q}", f"dh_wall_{q}", display="Giant Wallpaper", sound="wool")
cube("dh_baseboard", "dh_baseboard", display="Giant Skirting Board", sound="wood")
cube("dh_plaster", "dh_plaster", display="Giant Ceiling", flat=True)
cube("dh_frame", "dh_frame", display="Giant Window Frame", sound="wood")
cube("dh_glass", "dh_glass", display="Window Light", light=15, emissive=True, flat=True, sound="glass")
cube("dh_rug", "dh_rug", display="Giant Rug", sound="wool")
exit_block("dh_mousehole", "dh_mousehole", "DOOR", display="Mouse Hole")

# ------------------------------------------------------------------ furniture


def furniture_wood():
    return wood("dhfw", "#5a3a22", "#7a5030", grain_dir="v", rings=5)


def furniture_wood_h():
    return wood("dhfwh", "#5a3a22", "#7a5030", grain_dir="h", rings=5)


def chair_paint():
    img = T.ramp(T.noise("dhchp", sigma=2), "#c8b890", "#d8c8a0")
    worn = T.noise("dhchw", sigma=1) > 0.82
    return T.grain(T.blend(img, furniture_wood(), worn.astype(float)), "dhchp", 0.02)


def weave(c1, c2, seed):
    """Upholstery at this scale: you can see every thread."""
    yy, xx = np.mgrid[0:S, 0:S]
    over = ((xx // 4 + yy // 4) % 2 == 0)
    thread_h = np.abs(np.sin((yy % 4) / 4 * np.pi))
    thread_v = np.abs(np.sin((xx % 4) / 4 * np.pi))
    shade = np.where(over, thread_h, thread_v)
    img = T.ramp(shade * 0.6 + T.noise(seed, sigma=1) * 0.4, c1, c2)
    return T.grain(img, seed, 0.03)


def gingham():
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((xx // 16) % 2) == 0
    b = ((yy // 16) % 2) == 0
    img = np.where((a & b)[..., None], T.hexc("#3a5a9a"), np.where((a ^ b)[..., None], T.hexc("#8aa0cc"), T.hexc("#e8ecf4")))
    return weave("#000000", "#ffffff", "dhgw") * 0.15 + img * 0.85


def mattress():
    return weave("#d8d0c0", "#ece6d8", "dhmat")


tex("dh_wood", furniture_wood)
tex("dh_wood_h", furniture_wood_h)
tex("dh_chair", chair_paint)
tex("dh_sofa", lambda: weave("#5a6a3a", "#7a8a4a", "dhsofa"))
tex("dh_gingham", gingham)
tex("dh_mattress", mattress)
tex("dh_pillow", lambda: weave("#e8e4dc", "#f8f4ec", "dhpil"))

cube("dh_wood", "dh_wood", display="Giant Table Leg", sound="wood")
cube("dh_wood_h", "dh_wood_h", display="Giant Tabletop", sound="wood")
cube("dh_chair", "dh_chair", display="Giant Chair", sound="wood")
cube("dh_sofa", "dh_sofa", display="Giant Sofa", sound="wool")
cube("dh_gingham", "dh_gingham", display="Giant Blanket", sound="wool")
cube("dh_mattress", "dh_mattress", display="Giant Mattress", sound="wool")
cube("dh_pillow", "dh_pillow", display="Giant Pillow", sound="wool")

# ------------------------------------------------------------------ toys and debris

TOY_COLORS = {"red": ("#b02a22", "#c83a2e"), "blue": ("#2a4a9a", "#3a5ab0"), "yellow": ("#d8a820", "#e8bc30"),
              "green": ("#2a7a3a", "#3a8a4a")}


def toy_face(color, letter=None):
    lo, hi = TOY_COLORS[color]
    img = T.ramp(T.noise("dhtoy" + color, sigma=3), lo, hi)
    img = T.shade(img, 0.95 + T.noise("dhtoyc" + color, sigma=0.5) * 0.08)
    if letter:
        img = T.solid("#f0e8d0") * 0.0 + img
        img = text_center(img, letter, 6, "#f4ecd8", scale=4)
    return T.grain(img, "dhtoy" + color + str(letter), 0.02)


def toy_edge(color):
    lo, hi = TOY_COLORS[color]
    img = T.ramp(T.noise("dhtoye" + color, sigma=3), lo, hi)
    img = T.border(img, "#f4ecd8", 3)
    return T.grain(img, "dhtoye" + color, 0.02)


for color in TOY_COLORS:
    tex(f"dh_toy_{color}", (lambda c: lambda: toy_face(c))(color))
    tex(f"dh_toy_{color}_edge", (lambda c: lambda: toy_edge(c))(color))
    cube(f"dh_toy_{color}", f"dh_toy_{color}", display="Giant Toy Block", sound="wood")
    cube(f"dh_toy_{color}_edge", f"dh_toy_{color}_edge", display="Giant Toy Block", sound="wood")
for letter, color in (("A", "red"), ("B", "blue"), ("C", "yellow"), ("D", "green")):
    tex(f"dh_toy_letter_{letter.lower()}", (lambda l, c: lambda: toy_face(c, l))(letter, color))
    cube(f"dh_toy_letter_{letter.lower()}", f"dh_toy_letter_{letter.lower()}", display="Giant Toy Block", sound="wood")


def crayon(color):
    lo, hi = TOY_COLORS[color]
    img = T.ramp(T.noise("dhcray" + color, sigma=1), lo, hi)
    yy, xx = np.mgrid[0:S, 0:S]
    paper = (yy % 32 > 6) & (yy % 32 < 26)
    wr = T.ramp(T.noise("dhcrw" + color, sigma=1), "#d8d0b8", "#e8e0c8")
    wr = T.blend(wr, T.solid(lo), ((yy % 32 > 10) & (yy % 32 < 22) & ((xx % 8) < 2)).astype(float) * 0.6)
    img = np.where(paper[..., None], wr, img)
    return T.grain(img, "dhcray" + color, 0.03)


def pencil():
    img = T.ramp(T.noise("dhpen", sigma=1), "#d8a020", "#e8b830")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 11) == 0) * 0.25)
    return T.grain(img, "dhpen", 0.02)


tex("dh_crayon_red", lambda: crayon("red"))
tex("dh_crayon_blue", lambda: crayon("blue"))
tex("dh_pencil", pencil)
tex("dh_pencil_wood", lambda: wood("dhpw", "#c8a070", "#dcb488"))
tex("dh_lead", lambda: T.grain(T.solid("#2a2a2e"), "dhlead", 0.04))
tex("dh_eraser", lambda: T.grain(T.ramp(T.noise("dhers", sigma=2), "#d87a8a", "#e88a9a"), "dhers", 0.03))
tex("dh_ferrule", lambda: T.grain(T.ramp(np.mgrid[0:S, 0:S][1] % 4 / 4.0, "#8a8a8a", "#c8c8c8"), "dhfer", 0.02))
tex("dh_coin", lambda: T.grain(T.ramp(T.noise("dhcoin", sigma=2), "#a8803a", "#d8b060"), "dhcoin", 0.03))
tex("dh_dust", lambda: T.grain(T.ramp(T.noise("dhdust", sigma=0.6), "#7a7670", "#a8a49c"), "dhdust", 0.06))
tex("dh_ball_a", lambda: T.grain(T.ramp(T.noise("dhba", sigma=3), "#c83a3a", "#d84a48"), "dhba", 0.02))
tex("dh_ball_b", lambda: T.grain(T.ramp(T.noise("dhbb", sigma=3), "#f0e8d0", "#fff8e0"), "dhbb", 0.02))
tex("dh_lego", lambda: T.grain(T.ramp(T.noise("dhlego", sigma=3), "#c02820", "#d03828"), "dhlego", 0.015))

for c in ("red", "blue"):
    cube(f"dh_crayon_{c}", f"dh_crayon_{c}", display="Giant Crayon", sound="wood")
cube("dh_pencil", "dh_pencil", display="Giant Pencil", sound="wood")
cube("dh_pencil_wood", "dh_pencil_wood", display="Giant Pencil", sound="wood")
cube("dh_lead", "dh_lead", display="Giant Pencil Lead", sound="stone")
cube("dh_eraser", "dh_eraser", display="Giant Eraser", sound="wool")
cube("dh_ferrule", "dh_ferrule", display="Giant Pencil", sound="metal")
cube("dh_coin", "dh_coin", display="Giant Coin", sound="metal")
cube("dh_dust", "dh_dust", display="Dust", sound="wool", unbreakable=False, strength=0.3)
cube("dh_ball_a", "dh_ball_a", display="Giant Ball", sound="wool")
cube("dh_ball_b", "dh_ball_b", display="Giant Ball", sound="wool")
cube("dh_lego", "dh_lego", display="Giant Brick", sound="stone")
shaped("dh_lego_stud", [(2, 0, 2, 14, 10, 14, "l")], {"l": "dh_lego"}, display="Giant Brick", unbreakable=True,
       sound="stone")

# ------------------------------------------------------------------ the toy dollhouse (your size)


def toyhouse_wall(window=False):
    img = T.ramp(T.noise("dhth", sigma=2), "#e0a8b0", "#eab8c0")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.08)
    if window:
        img = T.rect(img, 6, 6, 26, 26, "#f8f4ec")
        img = T.rect(img, 9, 9, 23, 23, "#3a4a6a")
        img[15:17, 9:23] = T.hexc("#f8f4ec")
        img[9:23, 15:17] = T.hexc("#f8f4ec")
        # painted on, and badly: a face in the window
        img[12:14, 18:21] = T.hexc("#e8d8c8")
    return T.grain(img, "dhth" + str(window), 0.02)


def toyhouse_roof():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("dhroof", sigma=1), "#6a3a2a", "#7a4a34")
    off = ((yy // 8) % 2) * 4
    sh = ((yy % 8) == 0) | (((xx + off) % 8) == 0)
    return T.grain(T.shade(img, 1 - sh * 0.3), "dhroof", 0.03)


def toyhouse_door(top):
    img = T.ramp(T.noise("dhtd" + str(top), sigma=2), "#3a6a8a", "#4a7a9a")
    img = T.border(img, "#f4ecd8", 2)
    if not top:
        img = T.rect(img, 22, 4, 26, 8, "#e8c040")
    else:
        img = T.rect(img, 8, 6, 24, 18, "#f8e8a0")
        img = T.rect(img, 10, 8, 22, 16, "#ffd860")
    return T.grain(img, "dhtd" + str(top), 0.02)


tex("dh_th_wall", lambda: toyhouse_wall(False))
tex("dh_th_window", lambda: toyhouse_wall(True))
tex("dh_th_roof", toyhouse_roof)
tex("dh_th_door_low", lambda: toyhouse_door(False))
tex("dh_th_door_high", lambda: toyhouse_door(True))
cube("dh_th_wall", "dh_th_wall", display="Toy House Wall", sound="wood")
cube("dh_th_window", "dh_th_window", display="Toy House Window", sound="wood")
cube("dh_th_roof", "dh_th_roof", display="Toy House Roof", sound="wood")
exit_block("dh_th_door_low", "dh_th_door_low", "SECRET", trigger="USE", solid=True, display="Toy House Door", sound="wood")
exit_block("dh_th_door_high", "dh_th_door_high", "SECRET", trigger="USE", solid=True, display="Toy House Door",
           sound="wood", light=6, emissive=True)


def note():
    """A torn scrap of lined paper, 96x64 over 3x2 blocks. The handwriting is yours-sized."""
    W, H = 96, 64
    yy, xx = np.mgrid[0:H, 0:W]
    img = T.ramp(T.noise("dhnote", shape=(H, W), sigma=3), "#e8e4d4", "#f4f0e2")
    img[(yy % 10) == 6] = T.hexc("#9ab0d0")
    img[:, 10] = T.hexc("#d08080")
    img = text(img, "WHEN THE FLOOR", 16, 9, "#2a2a4a", jitter=1)
    img = text(img, "SHAKES, GET", 16, 19, "#2a2a4a", jitter=1)
    img = text(img, "UNDER SOMETHING.", 16, 29, "#2a2a4a", jitter=1)
    img = text(img, "IT LOOKS DOWN.", 16, 45, "#6a1a1a", jitter=1)
    torn = (np.abs(yy - (H - 3 - 2 * np.sin(xx * 0.7))) < 1.5)
    img[torn] = img[torn] * 0.85
    return img


for idx, q in enumerate(("tl", "tm", "tr", "bl", "bm", "br")):
    tex(f"dh_note_{q}", (lambda i: lambda: split(note(), 3, 2)[i])(idx))
    shaped(f"dh_note_{q}", [(0, 0, 0, 16, 0.3, 16, {"all": None, "up": "n"})], {"n": f"dh_note_{q}"},
           display="Scrap of Paper", collides=False, unbreakable=True, sound="wool")

# ------------------------------------------------------------------ the tin soldier


def soldier_tex():
    c = Canvas()
    red = lambda w, h: T.grain(T.ramp(T.noise(f"dts{w}{h}", shape=(h, w), sigma=2), "#a8201a", "#c02a22"), f"dts{w}", 0.03)
    navy = lambda w, h: T.grain(T.ramp(T.noise(f"dtn{w}{h}", shape=(h, w), sigma=2), "#1a2244", "#242c54"), f"dtn{w}", 0.03)

    def head(w, h):
        img = T.ramp(T.noise(f"dth{w}{h}", shape=(h, w), sigma=2), "#e8c8a8", "#f0d0b0")
        img[:3] = T.hexc("#101010")  # bearskin hat brim
        return img

    def face(w, h):
        img = head(w, h)
        img[4, 1:3] = T.hexc("#101010")
        img[4, 5:7] = T.hexc("#101010")
        img[5, 0:2] = T.hexc("#d06060")
        img[5, 6:8] = T.hexc("#d06060")
        img[6, 3:5] = T.hexc("#6a1a1a")
        # paint chipped off one eye, bare tin beneath
        img[3:5, 5:8] = T.hexc("#a8a8a8")
        return img

    def body(w, h):
        img = red(w, h)
        if w == 8:
            for y in range(2, h - 2, 3):
                img[y, 3:5] = T.hexc("#e8c040")
            img[-3:] = T.hexc("#f0ece0")
        return img

    paint_humanoid(c, head, body, red, navy, face=face)
    hat = lambda w, h: T.grain(T.ramp(T.noise(f"dthat{w}{h}", shape=(h, w), sigma=0.6), "#060606", "#1a1a1a"), "dthat", 0.05)
    c.box(32, 0, 8, 8, 8, all=hat)
    c.box(0, 32, 2, 2, 6, all=lambda w, h: T.solid("#a8a8a8", shape=(h, w)))
    return c.out()


entity_model("dh_tin_soldier", [
    part("head", (0, 0, 0), cubes=[(0, 0, -4, -8, -4, 8, 8, 8), (32, 0, -4, -16, -4, 8, 8, 8, 0.3)]),
    part("body", (0, 0, 0), cubes=[(16, 16, -4, 0, -2, 8, 12, 4), (0, 32, -1, 4, 2, 2, 2, 6)]),
    part("right_arm", (-5, 2, 0), cubes=[(40, 16, -3, -2, -2, 4, 12, 4)]),
    part("left_arm", (5, 2, 0), cubes=[(32, 48, -1, -2, -2, 4, 12, 4)]),
    part("right_leg", (-1.9, 12, 0), cubes=[(0, 16, -2, 0, -2, 4, 12, 4)]),
    part("left_leg", (1.9, 12, 0), cubes=[(16, 48, -2, 0, -2, 4, 12, 4)]),
])
entity_tex("dh_tin_soldier", soldier_tex)
lang("entity.noclip.dh_tin_soldier", "Tin Soldier")
lang("item.noclip.dh_tin_soldier_spawn_egg", "Tin Soldier Spawn Egg")
ITEM_MODELS["dh_tin_soldier_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def house():
    """The house at your size: a clock the size of a church ticking once a second, rumbles through the boards,
    and far above, a television the size of a cinema saying something you will never understand."""
    d = 30.0
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    for k in range(int(d)):
        n = int(0.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        thunk = np.sin(2 * np.pi * (70 if k % 2 else 62) * tt) * np.exp(-tt * 14) + Y.lp(Y.white(0.4, f"dhtk{k}"), 400)[:n] * np.exp(-tt * 30) * 0.5
        Y.place(out, thunk, k + 0.1, 0.5)
    tick = Y.reverb(out, 3.0, 0.7, "dhtick")[: len(t)]
    rumble = Y.lp(Y.brown(d, "dhrum"), 70) * 0.5
    tv = Y.lp(Y.whisper("dhtv", d) * 0.8, 300) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 15.0))
    x = tick * 0.6 + rumble + tv * 0.35
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 1.5), 0.004), "stereo"


def giant_voice():
    """Someone enormous talking in another room, a whole sentence in one breath."""
    d = 5.0
    x = Y.whisper("dhvoice", d)
    x = Y.lp(np.interp(np.arange(0, len(x), 0.45), np.arange(len(x)), x)[: int(d * Y.SR)], 600)
    t = Y.t_axis(d)
    voiced = np.sin(2 * np.pi * np.cumsum(55 + 15 * np.sin(2 * np.pi * 0.7 * t)) / Y.SR) * np.abs(x) * 6
    return Y.norm(Y.reverb(x + Y.lp(voiced, 400), 2.5, 0.6, "dhvoice"), 0.6)


def giant_door():
    out = np.zeros(int(6.0 * Y.SR))
    Y.place(out, Y.impact("dhgd", 3.0), 0.1, 1.0)
    return Y.norm(Y.reverb(Y.lp(out, 200), 4.0, 0.6, "dhgd"), 0.8)


def pipes():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.bp(Y.pink(d, "dhpipe"), 80, 500) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.4 * t)) * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(x, 2.0, 0.5, "dhpipe"), 0.5)


def step():
    """One footstep of the thing that lives here. You feel it in the floor before you hear it."""
    d = 3.5
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * (34 - 8 * t / d) * t) * np.exp(-t * 2.2) * (1 - np.exp(-t * 50))
    x += Y.lp(Y.white(d, "dhstep"), 180) * np.exp(-t * 5) * 1.2
    creak = Y.bp(Y.white(d, "dhstc"), 200, 900) * np.exp(-((t - 0.25) ** 2) / 0.02) * 0.4
    return Y.norm(Y.reverb(x + creak, 3.0, 0.5, "dhstep"), 0.95), "stereo"


def lift():
    d = 2.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "dhlift"), 200, 4000) * np.sin(np.pi * t / d) ** 2
    x += np.sin(2 * np.pi * (60 + 120 * t / d) * t) * np.sin(np.pi * t / d) * 0.5
    return Y.norm(x, 0.8), "stereo"


def wind_up():
    """Clockwork: a spring winding down inside a painted tin chest."""
    d = 1.8
    out = np.zeros(int(d * Y.SR))
    for k in range(int(d * 14)):
        n = int(0.03 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = np.sin(2 * np.pi * (3200 if k % 2 else 2600) * tt) * np.exp(-tt * 200)
        Y.place(out, c, k / 14.0, 0.6)
    return Y.norm(Y.bp(out, 800, 8000), 0.6)


def march():
    out = np.zeros(int(1.2 * Y.SR))
    for k in range(2):
        Y.place(out, Y.footstep(f"dhmar{k}", "metal"), 0.05 + k * 0.5, 0.8)
    return Y.norm(out, 0.7)


sound("ambient.dollhouse", [("ambient/dollhouse", house)], stream=True)
sound("ambient.dollhouse.additions",
      [("ambient/dollhouse_voice", giant_voice), ("ambient/dollhouse_door", giant_door), ("ambient/dollhouse_pipes", pipes)],
      subtitle="The house moves around you")
sound("event.dollhouse_step", [("event/dollhouse_step", step)], subtitle="Enormous footsteps")
sound("event.dollhouse_lift", [("event/dollhouse_lift", lift)], subtitle="You are lifted")
sound("entity.dh_tin_soldier.windup", [("entity/dh_tin_soldier_windup", wind_up)], subtitle="Clockwork whirs")
sound("entity.dh_tin_soldier.march", [("entity/dh_tin_soldier_march", march)], subtitle="Tin feet march")

level("dollhouse", fog="#c8b898", sky="#c8b898", ambient="ambient.dollhouse", additions="ambient.dollhouse.additions",
      additions_chance=0.003, fixed_time=6000, height=48, ambient_light=0.12)
