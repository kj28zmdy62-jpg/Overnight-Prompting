"""Photo Gallery - quiet white rooms hung with photographs of these same rooms. Look away and look back:
the photo has changed. There is someone in them now. It is you, from behind. And someone behind you."""
import numpy as np
from assets import cube, tex, shaped, level, sound, lang, entity_model, entity_tex, part, ITEM_MODELS, BLOCKS, LANG
import tex as T
import synth as Y
from content.rb_shared import split, ellipse, wood
from content.entities_core import paint_humanoid
from entity_paint import Canvas

S = 32
H = 64


def room_photo(seed):
    """A gallery room in one-point perspective, printed in faded silver: back wall, floor, frames on the walls."""
    yy, xx = np.mgrid[0:H, 0:H].astype(float)
    img = np.ones((H, H)) * 0.72
    # back wall rectangle
    bx0, bx1, by0, by1 = 20, 44, 18, 40
    floor = yy > by1 + (np.abs(xx - 32) - 12).clip(0) * ((64 - by1) / 32.0)
    floor = yy > by1
    side_floor = (yy > by1) & (yy > by1 + (np.maximum(bx0 - xx, xx - bx1)).clip(0) * 0.9)
    left = (xx < bx0) & (yy > by0 - (bx0 - xx) * 0.9) & (yy < by1 + (bx0 - xx) * 0.9)
    right = (xx > bx1) & (yy > by0 - (xx - bx1) * 0.9) & (yy < by1 + (xx - bx1) * 0.9)
    ceil = yy < by0 - (np.maximum(bx0 - xx, xx - bx1)).clip(0) * 0.9
    img[left] = 0.62
    img[right] = 0.66
    img[ceil] = 0.8
    img[(yy >= by1) & ~left & ~right] = 0.42
    # parquet lines on the floor converging on the back wall
    fl = (yy >= by1) & ~left & ~right
    for k in range(-6, 7):
        line = np.abs((xx - 32) - k * 3 * (1 + (yy - by1) / 8.0)) < 0.6
        img[line & fl] -= 0.06
    # frames on the walls
    for fx in (24, 34):
        img[24:31, fx:fx + 6] = 0.3
        img[25:30, fx + 1:fx + 5] = 0.55
    for (x0, x1, y0, y1) in ((6, 12, 20, 30), (52, 58, 20, 30)):
        img[y0:y1, x0:x1] = 0.32
    # a doorway in the back wall
    img[30:40, 30:34] = 0.12
    # ceiling spot glow
    img += np.exp(-((xx - 32) ** 2 + (yy - 6) ** 2) / 60.0) * 0.25
    return img


def figure(img, cx, base, h, tone=0.06, thin=False):
    """A person seen from behind, as a dark silhouette: head, shoulders, legs."""
    yy, xx = np.mgrid[0:H, 0:H].astype(float)
    w = h * (0.16 if thin else 0.22)
    head = ((xx - cx) / (h * 0.09)) ** 2 + ((yy - (base - h * 0.88)) / (h * 0.1)) ** 2 < 1
    body = (np.abs(xx - cx) < w) & (yy > base - h * 0.78) & (yy < base - h * 0.4)
    legs = (np.abs(np.abs(xx - cx) - w * 0.45) < w * 0.35) & (yy >= base - h * 0.42) & (yy < base)
    m = head | body | legs
    img[m] = tone
    return img


def print_finish(img, seed, warm=True):
    """Silver-gelatin print: grain, vignette, a white border, toned sepia."""
    g = T.rng("pgr", seed).random((H, H)) - 0.5
    yy, xx = np.mgrid[0:H, 0:H]
    vig = 1 - ((xx - 32) ** 2 + (yy - 32) ** 2) / (2 * 40.0 ** 2)
    v = np.clip(img * vig + g * 0.09, 0, 1)
    rgb = T.ramp(v, "#1a140e", "#8a7a64", "#e8dcc4") if warm else T.ramp(v, "#101012", "#7a7a7c", "#e4e4e0")
    rgb[:4] = rgb[-4:] = T.hexc("#ece6d8")
    rgb[:, :4] = rgb[:, -4:] = T.hexc("#ece6d8")
    return rgb


def stage(n):
    img = room_photo("pg" + str(n))
    if n == 1:
        img = figure(img, 32, 40, 12)
    elif n == 2:
        img = figure(img, 22, 48, 22)
    elif n == 3:
        img = figure(img, 30, 54, 30)
        img = figure(img, 32, 40, 13, tone=0.02, thin=True)   # someone in the doorway
    elif n == 4:
        img = figure(img, 30, 62, 40)
        img = figure(img, 44, 50, 26, tone=0.02, thin=True)
    elif n == 5:
        # the back of your head, right in front of the lens; and a pale face just past your shoulder
        yy, xx = np.mgrid[0:H, 0:H].astype(float)
        head = ((xx - 26) / 15.0) ** 2 + ((yy - 40) / 18.0) ** 2 < 1
        shoulders = (yy > 52) & (np.abs(xx - 26) < 30)
        img[head | shoulders] = 0.05
        face = ((xx - 50) / 6.0) ** 2 + ((yy - 30) / 8.0) ** 2 < 1
        img[face] = 0.9
        img[((xx - 48) ** 2 + (yy - 28) ** 2) < 2] = 0.05
        img[((xx - 53) ** 2 + (yy - 28) ** 2) < 2] = 0.05
    return print_finish(img, "st" + str(n), warm=True)


def exit_photo():
    img = room_photo("pgexit")
    yy, xx = np.mgrid[0:H, 0:H].astype(float)
    # the doorway at the back stands open, and there is daylight in it
    img[26:40, 28:36] = 1.0
    glow = np.exp(-((xx - 32) ** 2 / 40.0 + (yy - 40) ** 2 / 30.0)) * 0.5
    img = np.clip(img + glow * (yy > 36), 0, 1)
    return print_finish(img, "exit", warm=True)


def wall():
    img = T.ramp(T.fbm("pgwall", octaves=3, base=6), "#e6e2da", "#eeeae4")
    return T.grain(img, "pgwall", 0.012)


def wall_rail():
    img = wall()
    img[2:5] = T.hexc("#c8c2b6")
    img[5] = T.hexc("#a8a296")
    return img


def skirting():
    img = wall()
    img[22:] = T.ramp(T.noise("pgsk", shape=(10, S), sigma=2), "#d8d2c6", "#e2dcd0")
    img[22] = T.hexc("#b8b2a6")
    img[31] = T.hexc("#9a948a")
    return img


def floor():
    img = wood("pgfloor", "#8a7a66", "#a08e76", grain_dir="h", rings=3)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.25 - (((xx + (yy // 8) * 11) % 32) == 0) * 0.25)
    return T.grain(T.shade(img, 0.94 + T.noise("pgfsh", sigma=4) * 0.1), "pgfloor", 0.02)


def ceiling():
    return T.grain(T.ramp(T.fbm("pgceil", octaves=2, base=6), "#e8e6e0", "#f0eee8"), "pgceil", 0.01)


def spot():
    yy, xx = np.mgrid[0:S, 0:S]
    img = np.ones((S, S, 3)) * T.hexc("#2a2a2c")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img[d < 9] = T.hexc("#fff6e0")
    img[(d >= 9) & (d < 11)] = T.hexc("#8a8a8c")
    return img


def frame_wood():
    return wood("pgframe", "#1a1410", "#2a2018", grain_dir="v", rings=2)


def bench():
    return wood("pgbench", "#5a4a3a", "#6e5c48", grain_dir="h", rings=3)


tex("pg_wall", wall)
tex("pg_wall_rail", wall_rail)
tex("pg_skirting", skirting)
tex("pg_floor", floor)
tex("pg_ceiling", ceiling)
tex("pg_spot", spot)
tex("pg_frame", frame_wood)
tex("pg_bench", bench)
tex("pg_steel", lambda: T.grain(T.ramp(T.noise("pgst", sigma=2), "#4a4a4e", "#6a6a6e"), "pgst", 0.02))
QUADS = ("tl", "tr", "bl", "br")
PHOTOS = {f"pg_ph{n}": (lambda k: lambda: stage(k))(n) for n in range(6)}
PHOTOS["pg_phx"] = exit_photo
for name, fn in PHOTOS.items():
    for idx, q in enumerate(QUADS):
        tex(f"{name}_{q}", (lambda f, i: lambda: split(f(), 2, 2)[i])(fn, idx))

cube("pg_wall", "pg_wall", display="Gallery Wall", sound="stone", flat=True)
cube("pg_wall_rail", "pg_wall_rail", display="Gallery Wall", sound="stone", flat=True)
cube("pg_skirting", "pg_skirting", display="Gallery Wall", sound="stone", flat=True)
cube("pg_floor", "pg_floor", display="Gallery Floor", sound="wood")
cube("pg_ceiling", "pg_ceiling", display="Gallery Ceiling", flat=True)
cube("pg_spot", "pg_spot", display="Gallery Spotlight", light=15, emissive=True, flat=True, sound="glass")

# framed print quarters; the frame moulding only on the outer edges of each quarter
EDGES = {"tl": ("north", "west"), "tr": ("north", "east"), "bl": ("south", "west"), "br": ("south", "east")}
for name in PHOTOS:
    for q in QUADS:
        top = q[0] == "t"
        left = q[1] == "l"
        boxes = [(0, 0, 0, 16, 16, 1, {"all": "frame", "south": "photo"})]
        boxes.append((0, 14 if top else 0, 0, 16, 16 if top else 2, 2, "frame"))
        boxes.append((0 if left else 14, 0, 0, 2 if left else 16, 16, 2, "frame"))
        shaped(f"{name}_{q}", boxes, {"photo": f"{name}_{q}", "frame": "pg_frame"}, display="Photograph",
               collides=False, unbreakable=True, sound="wood")
        if name == "pg_phx":
            # the photograph of a way out is one: a wall-mounted block that opens when used
            for b in BLOCKS:
                if b["name"] == f"{name}_{q}":
                    b["kind"] = "class"
                    b["class"] = "com.noclip.block.PhotoExitBlock"
            LANG[f"block.noclip.{name}_{q}"] = "A Photograph of a Way Out"

shaped("pg_bench", [(0, 6, 3, 16, 8, 13, "wood"), (1, 0, 4, 3, 6, 12, "steel"), (13, 0, 4, 15, 6, 12, "steel")],
       {"wood": "pg_bench", "steel": "pg_steel"}, display="Gallery Bench", unbreakable=True)

# ------------------------------------------------------------------ the Photographer


def photographer_tex():
    c = Canvas()
    suit = lambda w, h: T.grain(T.ramp(T.noise(f"pgs{w}{h}", shape=(h, w), sigma=1), "#141416", "#1e1e22"), f"pgs{w}", 0.03)
    leather = lambda w, h: T.grain(T.ramp(T.noise(f"pgl{w}{h}", shape=(h, w), sigma=0.8), "#1a1612", "#2a241e"), f"pgl{w}", 0.04)

    def camera_front(w, h):
        img = leather(w, h)
        yy, xx = np.mgrid[0:h, 0:w]
        d = np.sqrt((xx - w / 2 + 0.5) ** 2 + (yy - h / 2 + 0.5) ** 2)
        img[d < 3.2] = T.hexc("#6a6a70")
        img[d < 2.4] = T.hexc("#0a0a10")
        img[(d < 1.0)] = T.hexc("#e8f0ff")
        return img

    def hand(w, h):
        img = suit(w, h)
        img[-3:] = T.hexc("#c8c0b4")
        return img

    paint_humanoid(c, leather, suit, hand, suit, face=camera_front)
    c.box(32, 0, 4, 4, 4, all=lambda w, h: T.solid("#4a4a50", shape=(h, w)), front=lambda w, h: T.solid("#e8f0ff", shape=(h, w)))
    return c.out()


entity_model("pg_photographer", [
    part("head", (0, 0, 0), cubes=[(0, 0, -4, -8, -4, 8, 8, 8), (32, 0, -2, -6, -8, 4, 4, 4)]),
    part("body", (0, 0, 0), cubes=[(16, 16, -4, 0, -2, 8, 12, 4)]),
    part("right_arm", (-5, 2, 0), cubes=[(40, 16, -3, -2, -2, 4, 14, 4)]),
    part("left_arm", (5, 2, 0), cubes=[(32, 48, -1, -2, -2, 4, 14, 4)]),
    part("right_leg", (-1.9, 12, 0), cubes=[(0, 16, -2, 0, -2, 4, 12, 4)]),
    part("left_leg", (1.9, 12, 0), cubes=[(16, 48, -2, 0, -2, 4, 12, 4)]),
])
entity_tex("pg_photographer", photographer_tex)
lang("entity.noclip.pg_photographer", "The Photographer")
lang("item.noclip.pg_photographer_spawn_egg", "The Photographer Spawn Egg")
ITEM_MODELS["pg_photographer_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def museum():
    """After hours: ventilation, the hush of big empty rooms, a building settling."""
    d = 28.0
    t = Y.t_axis(d)
    hvac = Y.bp(Y.pink(d, "pghvac"), 150, 1200) * 0.18 * (0.85 + 0.15 * np.sin(2 * np.pi * t / 14.0))
    room = Y.lp(Y.brown(d, "pgroom"), 120) * 0.35
    hum = Y.lp(Y.hum(d, 60.0, "pghum", buzz=0.05, harmonics=(1, 2)), 300) * 0.04
    x = Y.loopable(Y.norm(hvac + room + hum, 0.4), 1.5)
    return Y.stereoize(x, 0.002), "stereo"


def shutter(far):
    def f():
        d = 1.0
        out = np.zeros(int(d * Y.SR))
        for k, at in enumerate((0.0, 0.09)):
            n = int(0.04 * Y.SR)
            tt = np.arange(n) / Y.SR
            c = Y.bp(Y.white(0.04, f"pgsh{far}{k}"), 1500, 8000)[:n] * np.exp(-tt * 150)
            c += np.sin(2 * np.pi * 900 * tt) * np.exp(-tt * 120) * 0.3
            Y.place(out, c, 0.02 + at, 0.9)
        t = Y.t_axis(d)
        wind = Y.bp(np.sign(np.sin(2 * np.pi * 60 * t)) * (t > 0.2) * (t < 0.7), 400, 3000) * 0.15
        out = out + wind
        if far:
            out = Y.reverb(Y.lp(out, 2500), 2.5, 0.7, "pgshf")[: len(out)]
        return Y.norm(out, 0.6 if far else 0.9)
    return f


def steps():
    out = np.zeros(int(5.0 * Y.SR))
    for k in range(6):
        Y.place(out, Y.footstep(f"pgfs{k}", "tile"), 0.2 + k * 0.7, 0.5)
    return Y.reverb(Y.lp(out, 2500), 3.0, 0.7, "pgfs")


def flash_charge():
    d = 1.4
    t = Y.t_axis(d)
    f = 2000 + 5000 * (t / d) ** 0.5
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.15 * (t / d)
    return Y.norm(x, 0.4)


sound("ambient.photo_gallery", [("ambient/photo_gallery", museum)], stream=True)
sound("ambient.photo_gallery.additions", [("ambient/photo_gallery_shutter", shutter(True)), ("ambient/photo_gallery_steps", steps)],
      subtitle="Someone else in the gallery")
sound("event.photo_gallery_shutter", [("event/photo_gallery_shutter", shutter(False))], subtitle="A camera shutter, close")
sound("event.photo_gallery_charge", [("event/photo_gallery_charge", flash_charge)], subtitle="A flash charging")

level("photo_gallery", fog="#2a2622", sky="#2a2622", ambient="ambient.photo_gallery",
      additions="ambient.photo_gallery.additions", additions_chance=0.004, fixed_time=18000, height=32)
