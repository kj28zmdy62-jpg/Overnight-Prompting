"""Room of Doors - a marble hall of freestanding doors, hundreds of them, almost all wrong.
Everyone who came before tried the wrong ones and left their marks. The right one is the door nobody touched."""
import numpy as np
from assets import cube, tex, shaped, level, sound, BLOCK_MODELS
import tex as T
import synth as Y
from content.rb_shared import text, text_center, wood, line, disc, split

S = 32
COLORS = {"white": ("#d8d2c4", "#e6e0d2"), "oak": ("#7a5434", "#946a42"), "green": ("#2e4a3a", "#3a5c48")}


def door_base(color, top, seed):
    lo, hi = COLORS[color]
    if color == "oak":
        img = wood(f"rodw{color}{top}{seed}", lo, hi, grain_dir="v", rings=4)
    else:
        img = T.grain(T.ramp(T.noise(f"rodp{color}{top}", sigma=2), lo, hi), f"rodp{color}{top}", 0.02)
    panels = ((5, 30),) if top else ((2, 24),)
    for (y0, y1) in panels:
        for (x0, x1) in ((5, 14), (18, 27)):
            img[y0:y1, x0] = img[y0:y1, x0] * 0.72
            img[y0, x0:x1] = img[y0, x0:x1] * 0.72
            img[y0:y1, x1] = np.clip(img[y0:y1, x1] * 1.18, 0, 1)
            img[y1, x0:x1 + 1] = np.clip(img[y1, x0:x1 + 1] * 1.18, 0, 1)
    if not top:
        img = T.rect(img, 23, 3, 26, 6, "#a8803a")
        img = T.rect(img, 24, 4, 25, 5, "#f0d080")
        img = T.rect(img, 23, 8, 26, 10, "#6a5022")
    else:
        img = T.rect(img, 13, 1, 19, 4, "#a8803a")
    return img


def scratches(img, seed):
    """Claw marks around the base, as if someone tried to get back in."""
    r = T.rng("rodscr", seed)
    for k in range(7):
        x0 = 4 + r.random() * 24
        y0 = 14 + r.random() * 8
        ln = 6 + r.random() * 10
        for j in range(3):
            img = line(img, x0 + j * 1.5, y0, x0 + j * 1.5 + (r.random() - 0.5) * 3, min(31, y0 + ln),
                       "#e8e0cc" if r.random() < 0.5 else "#2a1a10", 0.7)
    return img


def handprints(img, seed, top):
    """Dark smudged palms at handle height and higher, some dragged downward."""
    r = T.rng("rodhand", seed, top)
    yy, xx = np.mgrid[0:S, 0:S]
    for k in range(3 if top else 2):
        cx = 6 + r.random() * 20
        cy = (6 + r.random() * 18) if top else (4 + r.random() * 10)
        palm = ((xx - cx) / 3.0) ** 2 + ((yy - cy) / 3.6) ** 2 < 1
        m = palm.astype(float)
        for f in range(4):
            fx = cx - 2.2 + f * 1.5
            m = np.maximum(m, ((np.abs(xx - fx) < 0.7) & (yy < cy - 2) & (yy > cy - 7 + abs(f - 1.5))).astype(float))
        drag = (np.abs(xx - cx) < 2.5) & (yy > cy) & (yy < cy + 6 + r.random() * 6)
        m = np.maximum(m, drag.astype(float) * 0.45)
        img = T.blend(img, T.solid("#2a1610"), m * 0.7)
    return img


def chalk_x(img, seed):
    r = T.rng("rodx", seed)
    a = 0.85
    img = line(img, 6, 8, 26, 28, "#f0ece0", a, width=2)
    img = line(img, 26, 8, 6, 28, "#f0ece0", a, width=2)
    if r.random() < 0.5:
        img = text(img, "NO", 3, 2, "#f0ece0", alpha=0.8)
    return img


VARIANTS_LOW = ("clean", "scratch", "hands")
VARIANTS_HIGH = ("clean", "chalk", "hands")

for color in COLORS:
    for v in VARIANTS_LOW:
        def mk(c=color, v=v):
            img = door_base(c, False, v)
            if v == "scratch":
                img = scratches(img, c + v)
            elif v == "hands":
                img = handprints(img, c + v, False)
            return T.grain(img, f"rodl{c}{v}", 0.015)
        tex(f"rod_{color}_low_{v}", mk)
    for v in VARIANTS_HIGH:
        def mk(c=color, v=v):
            img = door_base(c, True, v)
            if v == "chalk":
                img = chalk_x(img, c + v)
            elif v == "hands":
                img = handprints(img, c + v, True)
            return T.grain(img, f"rodh{c}{v}", 0.015)
        tex(f"rod_{color}_high_{v}", mk)


def frame():
    return wood("rodframe", "#4a3020", "#5e3e28", grain_dir="v", rings=3)


def marble(dark):
    """Polished marble tile with soft veins."""
    n = T.fbm("rodm" + str(dark), octaves=4, base=6)
    yy, xx = np.mgrid[0:S, 0:S]
    vein = np.abs(np.sin((xx * 0.35 + yy * 0.2 + n * 9.0))) < 0.08
    if dark:
        img = T.ramp(n, "#141416", "#24242a")
        img = T.blend(img, T.solid("#6a6a70"), vein.astype(float) * 0.5)
    else:
        img = T.ramp(n, "#d6d2c8", "#ece8e0")
        img = T.blend(img, T.solid("#8a8478"), vein.astype(float) * 0.45)
    img = T.border(img, "#7a7468" if not dark else "#0a0a0c", 1, alpha=0.4)
    sheen = T.noise("rodsheen" + str(dark), sigma=4)
    return T.grain(T.shade(img, 0.92 + sheen * 0.14), "rodm" + str(dark), 0.015)


def ceiling():
    img = T.ramp(T.fbm("rodceil", octaves=3, base=5), "#2a2420", "#3a322a")
    img = T.border(img, "#1a1612", 3)
    img = T.border(img, "#6a5a3a", 1)
    return T.grain(img, "rodceil", 0.03)


def crystal():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("rodcry", sigma=0.8), "#f8f0d8", "#fffbea")
    facet = ((xx + yy) % 6 == 0) | ((xx - yy) % 6 == 0)
    img = T.shade(img, 1 - facet * 0.12)
    return img


def floor_scrawl():
    """64x64 chalk on marble, written by someone who got out. Alpha only where the chalk is."""
    W = 64
    rgb = np.ones((W, W, 3)) * T.hexc("#f2eee2")
    img = np.zeros((W, W, 3))
    img = text_center(img, "THE ONE", 12, "#ffffff", scale=2)
    img = text_center(img, "NOBODY", 28, "#ffffff", scale=2)
    img = text_center(img, "TOUCHED", 44, "#ffffff", scale=2)
    a = (img.sum(axis=2) > 0.5).astype(float) * (0.65 + 0.3 * T.noise("rodchk", shape=(W, W), sigma=0.5))
    return rgb, a


def brass():
    return T.grain(T.ramp(T.noise("rodbrass", sigma=1.5), "#7a5a24", "#c09048"), "rodbrass", 0.03)


tex("rod_frame", frame)
tex("rod_marble_white", lambda: marble(False))
tex("rod_marble_black", lambda: marble(True))
tex("rod_ceiling", ceiling)
tex("rod_crystal", crystal)
tex("rod_brass", brass)
for idx, part in enumerate(("tl", "tr", "bl", "br")):
    tex(f"rod_scrawl_{part}", (lambda i: lambda: (lambda rgba: (split(rgba[0], 2, 2)[i], split(rgba[1][..., None], 2, 2)[i][..., 0]))(floor_scrawl()))(idx))

cube("rod_marble_white", "rod_marble_white", display="White Marble", sound="tile")
cube("rod_marble_black", "rod_marble_black", display="Black Marble", sound="tile")
cube("rod_ceiling", "rod_ceiling", display="Coffered Ceiling", flat=True)

# A freestanding door in its frame, centred in the block, both faces showing.
# Lower half carries the side posts; upper half adds the lintel.
for color in COLORS:
    for half, variants in (("low", VARIANTS_LOW), ("high", VARIANTS_HIGH)):
        for v in variants:
            boxes = [(1, 0, 7, 15, 16, 9, {"all": "leaf", "up": None if half == "low" else "frame", "down": None if half == "high" else "frame",
                                           "east": "frame", "west": "frame"}),
                     (-1, 0, 6, 1, 16, 10, "frame"), (15, 0, 6, 17, 16, 10, "frame")]
            if half == "high":
                boxes.append((-1, 16, 6, 17, 18, 10, "frame"))
            shaped(f"rod_{color}_{half}_{v}", boxes, {"leaf": f"rod_{color}_{half}_{v}", "frame": "rod_frame"},
                   display="A Door", kind="class", unbreakable=True, sound="wood",
                   **{"class": "com.noclip.block.RoomDoorBlock"})

# floor chalk, a thin decal
for part in ("tl", "tr", "bl", "br"):
    shaped(f"rod_scrawl_{part}", [(0, 0, 0, 16, 0.2, 16, {"all": None, "up": "c"})], {"c": f"rod_scrawl_{part}"},
           display="Chalk", collides=False, unbreakable=True, render="cutout", sound="tile")

# a crystal chandelier hanging from the ceiling
shaped("rod_chandelier", [(7, 10, 7, 9, 16, 9, "brass"), (2, 4, 2, 14, 6, 14, "brass"),
                          (3, 0, 3, 5, 6, 5, "crystal"), (11, 0, 3, 13, 6, 5, "crystal"), (3, 0, 11, 5, 6, 13, "crystal"),
                          (11, 0, 11, 13, 6, 13, "crystal"), (6, -2, 6, 10, 4, 10, "crystal")],
       {"brass": "rod_brass", "crystal": "rod_crystal"}, display="Chandelier", light=15, emissive=True,
       collides=False, unbreakable=True, sound="glass")

# set pieces: a door lying on the floor, a leaning stack
shaped("rod_fallen_door", [(0, 0, 0, 16, 2, 16, {"all": "frame", "up": "leaf"})],
       {"leaf": "rod_white_low_scratch", "frame": "rod_frame"}, display="Fallen Door", unbreakable=True, sound="wood")

# ------------------------------------------------------------------ sound


def hall():
    """A vast marble room: deep air, a slow pressure, doors closing far away inside the reverb."""
    d = 30.0
    t = Y.t_axis(d)
    air = Y.lp(Y.brown(d, "rodair"), 110) * 0.55
    hiss = Y.bp(Y.pink(d, "rodhiss"), 400, 3000) * 0.04
    out = np.zeros_like(t)
    r = Y.rng("rodfar")
    for k in range(6):
        Y.place(out, Y.impact(f"rodfar{k}", 0.6), 1 + k * 4.6 + r.random() * 2, 0.15 + r.random() * 0.15)
    far = Y.reverb(Y.lp(out, 500), 5.0, 0.9, "rodfarv")[: len(t)]
    x = air + hiss + far
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 2.0), 0.002), "stereo"


def slam(i):
    def f():
        out = np.zeros(int(5.0 * Y.SR))
        Y.place(out, Y.impact(f"rodslam{i}", 0.9, ring=0.05), 0.05, 1.0)
        return Y.reverb(Y.lp(out, max(500, 2500 - i * 600)), 4.0, 0.8, f"rodslam{i}")
    return f


def knock_inside():
    out = np.zeros(int(3.5 * Y.SR))
    for k in range(4):
        Y.place(out, Y.impact(f"rodki{k}", 0.25), 0.1 + k * 0.33 + (0.25 if k == 3 else 0), 0.8)
    return Y.reverb(Y.lp(out, 1000), 2.0, 0.4, "rodki")


def rattle():
    d = 1.5
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("rodrat")
    for k in range(10):
        n = int(0.06 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = Y.bp(Y.white(0.06, f"rodr{k}"), 1500, 6000)[:n] * np.exp(-tt * 70)
        c += np.sin(2 * np.pi * 900 * tt) * np.exp(-tt * 50) * 0.3
        Y.place(out, c, 0.05 + k * 0.12 + r.random() * 0.03, 0.5 + r.random() * 0.4)
    return Y.reverb(out, 1.5, 0.4, "rodrat")


def open_door():
    d = 1.8
    out = np.zeros(int(d * Y.SR))
    n = int(0.08 * Y.SR)
    Y.place(out, Y.bp(Y.white(0.08, "rodo1"), 1500, 7000) * Y.decay(n, 0.008), 0.0, 0.7)
    t = Y.t_axis(1.0)
    creak = np.sin(2 * np.pi * np.cumsum(220 + 160 * t + 40 * np.sin(2 * np.pi * 3 * t)) / Y.SR)
    creak *= (Y.rng("rodcr").random(len(t)) > 0.55)
    Y.place(out, Y.bp(creak, 300, 2800) * np.sin(np.pi * t / 1.0), 0.1, 0.35)
    return Y.norm(Y.reverb(out, 1.8, 0.5, "rodo"), 0.8)


def right_door():
    """The one that was right: warm air, a breath out."""
    d = 3.0
    t = Y.t_axis(d)
    x = sum(np.sin(2 * np.pi * f * t) for f in (261.6, 329.6, 392.0)) * np.exp(-t * 1.1) * (1 - np.exp(-t * 8))
    x += Y.bp(Y.white(d, "rodright"), 300, 2000) * np.exp(-t * 2) * 0.3
    return Y.norm(Y.reverb(x, 2.0, 0.5, "rodright"), 0.7), "stereo"


sound("ambient.room_of_doors", [("ambient/room_of_doors", hall)], stream=True)
sound("ambient.room_of_doors.additions",
      [(f"ambient/room_of_doors_slam{i}", slam(i)) for i in range(3)] +
      [("ambient/room_of_doors_knock", knock_inside), ("ambient/room_of_doors_rattle", rattle)],
      subtitle="A door, somewhere in the hall")
sound("event.room_of_doors_open", [("event/room_of_doors_open", open_door)], subtitle="A door opens")
sound("event.room_of_doors_slam", [(f"event/room_of_doors_slam", slam(9))], subtitle="A door slams")
sound("event.room_of_doors_right", [("event/room_of_doors_right", right_door)], subtitle="Warm air")

level("room_of_doors", fog="#1a1612", sky="#1a1612", ambient="ambient.room_of_doors",
      additions="ambient.room_of_doors.additions", additions_chance=0.005, fixed_time=18000, height=48)
