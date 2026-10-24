"""Daycare. Primary colours, tiny chairs, cubbies with names on them, nap mats in rows.
The toys are never where you left them."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, exit_block, custom, level, sound, BLOCK_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import centered, glyph_mask, ink, wood, chime, voice

S = 32
PRIMARY = {"red": ("#c8302a", "#d8403a"), "yellow": ("#e8b828", "#f4c838"), "blue": ("#2a5ab8", "#3a6ac8"),
           "green": ("#3a9a4a", "#4aaa5a")}


def foam(c1, c2, seed):
    """Interlocking foam floor tiles: two colours, puzzle-edge teeth."""
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        a = T.ramp(T.noise(seed + "a", sigma=0.6), *PRIMARY[c1])
        b = T.ramp(T.noise(seed + "b", sigma=0.6), *PRIMARY[c2])
        q = ((xx // 16) + (yy // 16)) % 2
        # teeth along the seams
        tx = ((xx % 16) < 3) & (((yy // 4) % 2) == 0)
        ty = ((yy % 16) < 3) & (((xx // 4) % 2) == 0)
        q = np.where(tx | ty, 1 - q, q)
        img = T.blend(a, b, q.astype(float))
        img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.15)
        dimples = ((xx % 3) == 1) & ((yy % 3) == 1)
        img = T.shade(img, 1 - dimples * 0.04)
        return T.grain(T.stains(img, seed + "s", "#5a4a30", count=1, strength=0.15, size=6), seed, 0.03)
    return f


def wall(colour, seed):
    def f():
        img = T.ramp(T.fbm(seed, octaves=2, base=6), "#ece4d0", "#f4ecda")
        yy, xx = np.mgrid[0:S, 0:S]
        lo, hi = PRIMARY[colour]
        band = yy >= 20
        img = T.blend(img, T.ramp(T.noise(seed + "b", sigma=1), lo, hi), band.astype(float))
        img = T.blend(img, T.solid("#f8f4ea"), (yy == 19).astype(float))
        # crayon scribbles low down, at a child's height
        r = T.rng(seed + "c")
        for _ in range(2):
            x0, y0 = int(r.random() * 24), 22 + int(r.random() * 6)
            col = ["#2a2a8a", "#8a2a8a", "#1a6a2a"][int(r.random() * 3)]
            for t in np.linspace(0, 1, 30):
                x = int(x0 + t * 8) % S
                y = int(y0 + np.sin(t * 9) * 2) % S
                img[y, x] = T.hexc(col)
        return T.grain(img, seed, 0.02)
    return f


def frieze():
    """Alphabet border running round the room."""
    img = T.solid("#f4ecda")
    yy, xx = np.mgrid[0:S, 0:S]
    cols = ["#c8302a", "#e8b828", "#2a5ab8", "#3a9a4a"]
    for k, ch in enumerate("AB"):
        x0 = 3 + k * 16
        card = (xx >= x0 - 2) & (xx < x0 + 12) & (yy >= 7) & (yy < 25)
        img = T.blend(img, T.solid(cols[k]), card.astype(float))
        g = np.kron(glyph_mask(ch), np.ones((2, 2)))
        m = np.zeros((S, S))
        m[9:23, x0:x0 + 10] = g
        img = T.blend(img, T.solid("#ffffff"), m)
    return T.grain(img, "dcf", 0.02)


def frieze_wrong():
    img = frieze()
    # one card has been drawn over
    r = T.rng("dcfw")
    for _ in range(60):
        x, y = 1 + int(r.random() * 14), 7 + int(r.random() * 18)
        img[y, x:x + 2] = T.hexc("#141414")
    return img


def ceiling():
    img = T.ramp(T.fbm("dcc", base=4), "#e4e0d4", "#eeeadf")
    return T.grain(T.border(img, "#c4c0b4", 1), "dcc", 0.02)


def light(on=True):
    img = T.solid("#fffaf0") if on else T.solid("#8a8880")
    yy, xx = np.mgrid[0:S, 0:S]
    # a paper cut-out sun stuck to the diffuser
    d = np.sqrt((xx - 22) ** 2 + (yy - 10) ** 2)
    img = T.blend(img, T.solid("#f4c838"), (d < 4).astype(float) * (0.6 if on else 0.9))
    return T.border(img, "#c8c4b8", 2)


def drawing(kind):
    def f():
        img = T.ramp(T.noise("dcd" + kind, sigma=2), "#f4f0e4", "#fcf8ee")
        yy, xx = np.mgrid[0:S, 0:S].astype(float)
        ink_c = {"sun": "#f4c838", "fam": "#2a2a8a", "tall": "#141414", "door": "#2a5ab8"}[kind]

        def stick(cx, top, h, col):
            out = np.zeros((S, S), bool)
            head = (xx - cx) ** 2 + (yy - top - 2) ** 2
            out |= (head < 6) & (head > 2)
            out |= (np.abs(xx - cx) < 0.6) & (yy > top + 4) & (yy < top + h * 0.65)
            out |= (np.abs(yy - top - h * 0.35) < 0.6) & (np.abs(xx - cx) < 3)
            leg = (yy > top + h * 0.65) & (yy < top + h)
            out |= leg & (np.abs((xx - cx) - (yy - top - h * 0.65) * 0.5) < 0.7)
            out |= leg & (np.abs((xx - cx) + (yy - top - h * 0.65) * 0.5) < 0.7)
            return out
        if kind == "sun":
            d = np.sqrt((xx - 22) ** 2 + (yy - 8) ** 2)
            img = T.blend(img, T.solid(ink_c), (d < 4).astype(float))
            img = T.blend(img, T.solid("#3a9a4a"), (yy > 25).astype(float))
            m = stick(10, 12, 14, "#c8302a")
            img = T.blend(img, T.solid("#c8302a"), m.astype(float))
        elif kind == "fam":
            img = T.blend(img, T.solid("#3a9a4a"), (yy > 27).astype(float))
            for cx, top, h in ((6, 12, 15), (12, 14, 13), (18, 18, 9)):
                img = T.blend(img, T.solid("#2a2a8a"), stick(cx, top, h, "").astype(float))
            # and one more, taller than the house, with no face
            img = T.blend(img, T.solid("#141414"), stick(26, 2, 25, "").astype(float))
        elif kind == "tall":
            img = T.blend(img, T.solid("#141414"), stick(16, 1, 30, "").astype(float))
            img = ink(img, text_like("HI"), "#c8302a")
        else:
            door = (xx > 10) & (xx < 22) & (yy > 8) & (yy < 30)
            img = T.blend(img, T.solid("#2a5ab8"), door.astype(float))
            img = T.blend(img, T.solid("#f4c838"), ((xx - 19) ** 2 + (yy - 20) ** 2 < 2).astype(float))
            img = ink(img, text_like("OUT"), "#c8302a")
        tape = ((yy < 3) & (np.abs(xx - 16) < 5))
        img = T.blend(img, T.solid("#e8e0c0"), tape.astype(float) * 0.8)
        return img
    return f


def text_like(t):
    return centered(t, y=2, scale=1)


def cubby():
    img = wood("dcb", "#d8b880", "#e4c890", plank=32)
    yy, xx = np.mgrid[0:S, 0:S]
    hole = (xx >= 3) & (xx <= 28) & (yy >= 3) & (yy <= 28)
    inside = T.ramp(np.clip((yy - 3) / 26, 0, 1), "#3a2a1a", "#6a5030")
    img = T.blend(img, inside, hole.astype(float))
    # a little coat hook and something hanging on it
    img = T.rect(img, 14, 5, 18, 8, "#9a9a98")
    return T.grain(img, "dcb", 0.03)


def cubby_coat(col):
    def f():
        img = cubby()
        yy, xx = np.mgrid[0:S, 0:S]
        coat = (yy >= 8) & (yy <= 26) & (np.abs(xx - 16) < 4 + (yy - 8) * 0.25)
        return T.blend(img, T.ramp(T.noise("dcco" + col, sigma=1), col, col), coat.astype(float))
    return f


def toy_block(letter, col):
    def f():
        img = T.ramp(T.noise("dtb" + letter, sigma=1.5), col, col)
        img = T.border(img, "#f4ecda", 2)
        g = np.kron(glyph_mask(letter), np.ones((3, 3)))
        m = np.zeros((S, S))
        m[5:26, 8:23] = g
        img = T.blend(img, T.solid("#ffffff"), m)
        return T.grain(img, "dtb" + letter, 0.03)
    return f


def mat(colour):
    def f():
        lo, hi = PRIMARY[colour]
        img = T.ramp(T.noise("dmt" + colour, sigma=1.2), lo, hi)
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.shade(img, 1 - ((yy % 16) == 0) * 0.3)
        img = T.border(img, "#f4ecda", 1)
        return T.grain(img, "dmt" + colour, 0.03)
    return f


def plastic(colour):
    lo, hi = PRIMARY[colour]
    return lambda: T.grain(T.ramp(T.noise("dpl" + colour, sigma=1.5), lo, hi), "dpl" + colour, 0.02)


def bear():
    return T.grain(T.ramp(T.noise("dbe", sigma=0.5), "#8a5a30", "#a06a3a"), "dbe", 0.06)


def bear_face():
    img = bear()
    yy, xx = np.mgrid[0:S, 0:S]
    for ex in (10, 21):
        img = T.blend(img, T.solid("#0a0a0a"), (((xx - ex) ** 2 + (yy - 12) ** 2) < 5).astype(float))
    img = T.blend(img, T.solid("#d8b890"), (((xx - 15.5) ** 2 + (yy - 21) ** 2) < 30).astype(float))
    img = T.blend(img, T.solid("#2a1a10"), (((xx - 15.5) ** 2 + (yy - 18) ** 2) < 4).astype(float))
    return img


def fire_door():
    img = T.ramp(T.noise("dfd", sigma=2), "#2a5ab8", "#3466c4")
    img = T.border(img, "#1a3a7a", 2)
    img = T.rect(img, 3, 3, 29, 13, "#f4ecda")
    img = ink(img, centered("EXIT", y=5, scale=1), "#2a8a3a")
    img = T.rect(img, 4, 20, 28, 23, "#c0c0b8")
    return T.grain(img, "dfd", 0.03)


tex("dc_foam_ry", foam("red", "yellow", "dfry"))
tex("dc_foam_bg", foam("blue", "green", "dfbg"))
for c in PRIMARY:
    tex(f"dc_wall_{c}", wall(c, "dcw" + c))
    tex(f"dc_mat_{c}", mat(c))
    tex(f"dc_plastic_{c}", plastic(c))
tex("dc_wall_plain", lambda: T.grain(T.ramp(T.fbm("dcwp", octaves=2, base=6), "#ece4d0", "#f4ecda"), "dcwp", 0.02))
tex("dc_frieze", frieze)
tex("dc_frieze_wrong", frieze_wrong)
tex("dc_ceiling", ceiling)
tex("dc_light", lambda: light(True))
tex("dc_light_off", lambda: light(False))
for k in ("sun", "fam", "tall", "door"):
    tex(f"dc_drawing_{k}", drawing(k))
tex("dc_cubby", cubby)
tex("dc_cubby_red", cubby_coat("#c8302a"))
tex("dc_cubby_blue", cubby_coat("#2a5ab8"))
for letter, col in (("A", "#c8302a"), ("B", "#2a5ab8"), ("C", "#3a9a4a"), ("U", "#e8b828")):
    tex(f"dc_toy_{letter.lower()}", toy_block(letter, col))
tex("dc_bear", bear)
tex("dc_bear_face", bear_face)
tex("dc_fire_door", fire_door)
tex("dc_wood", lambda: wood("dcwd", "#d8b880", "#e4c890", plank=8))

cube("dc_foam_ry", "dc_foam_ry", display="Foam Floor Tiles", sound="wool")
cube("dc_foam_bg", "dc_foam_bg", display="Foam Floor Tiles", sound="wool")
for c in PRIMARY:
    cube(f"dc_wall_{c}", f"dc_wall_{c}", display="Painted Nursery Wall")
cube("dc_wall_plain", "dc_wall_plain", display="Painted Nursery Wall")
cube("dc_frieze", "dc_frieze", display="Alphabet Frieze")
cube("dc_frieze_wrong", "dc_frieze_wrong", display="Alphabet Frieze")
cube("dc_ceiling", "dc_ceiling", display="Nursery Ceiling", flat=True)
flicker_light("dc_light", "dc_light", "dc_light_off", "dc_ceiling", light=15, flicker=0.0, display="Nursery Light")
flicker_light("dc_light_flicker", "dc_light", "dc_light_off", "dc_ceiling", light=15, flicker=0.5, display="Nursery Light")
for k in ("sun", "fam", "tall", "door"):
    shaped(f"dc_drawing_{k}", [(1, 1, 0, 15, 15, 0.5, {"all": None, "south": "d"})], {"d": f"dc_drawing_{k}"},
           display="Crayon Drawing", collides=False, unbreakable=True, sound="wool")

# tiny furniture
shaped("dc_chair_red", [(4, 5, 4, 12, 6, 12, "p"), (4, 6, 11, 12, 11, 12, "p"),
                        (4, 0, 4, 5, 5, 5, "p"), (11, 0, 4, 12, 5, 5, "p"), (4, 0, 11, 5, 5, 12, "p"), (11, 0, 11, 12, 5, 12, "p")],
       {"p": "dc_plastic_red"}, display="Tiny Chair", unbreakable=True, sound="wood")
shaped("dc_chair_blue", [(4, 5, 4, 12, 6, 12, "p"), (4, 6, 11, 12, 11, 12, "p"),
                         (4, 0, 4, 5, 5, 5, "p"), (11, 0, 4, 12, 5, 5, "p"), (4, 0, 11, 5, 5, 12, "p"), (11, 0, 11, 12, 5, 12, "p")],
       {"p": "dc_plastic_blue"}, display="Tiny Chair", unbreakable=True, sound="wood")
shaped("dc_table", [(0, 8, 0, 16, 9, 16, {"all": "w", "up": "y"}), (1, 0, 1, 3, 8, 3, "w"), (13, 0, 1, 15, 8, 3, "w"),
                    (1, 0, 13, 3, 8, 15, "w"), (13, 0, 13, 15, 8, 15, "w")],
       {"w": "dc_wood", "y": "dc_plastic_yellow"}, display="Tiny Table", unbreakable=True)
shaped("dc_mat_red", [(1, 0, 0, 15, 1.5, 16, "m")], {"m": "dc_mat_red"}, display="Nap Mat", unbreakable=True, sound="wool")
shaped("dc_mat_blue", [(1, 0, 0, 15, 1.5, 16, "m")], {"m": "dc_mat_blue"}, display="Nap Mat", unbreakable=True, sound="wool")
shaped("dc_mat_green", [(1, 0, 0, 15, 1.5, 16, "m")], {"m": "dc_mat_green"}, display="Nap Mat", unbreakable=True, sound="wool")

# toys: these are the ones that move
for letter in ("a", "b", "c", "u"):
    shaped(f"dc_toy_{letter}", [(4, 0, 4, 12, 8, 12, "t")], {"t": f"dc_toy_{letter}"}, display="Toy Block",
           unbreakable=True, sound="wood")
shaped("dc_toy_bear", [(4, 0, 5, 12, 7, 11, "b"), (5, 7, 6, 11, 12, 10, {"all": "b", "north": "f"}),
                       (4, 11, 7, 6, 13, 9, "b"), (10, 11, 7, 12, 13, 9, "b")],
       {"b": "dc_bear", "f": "dc_bear_face"}, display="Teddy Bear", unbreakable=True, sound="wool")
shaped("dc_toy_ball", [(4, 0, 4, 12, 8, 12, "r"), (5, 8, 5, 11, 9, 11, "r"), (5, -0.5, 5, 11, 0, 11, "r")],
       {"r": "dc_plastic_red"}, display="Ball", unbreakable=True, sound="wool")

# cubbies: Java block so the one with your name can be opened
custom("dc_cubby", "com.noclip.block.DaycareCubbyBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/dc_cubby"}},
       display="Cubby", unbreakable=True, sound="wood")
BLOCK_MODELS["dc_cubby_red"] = {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/dc_cubby_red"}}
BLOCK_MODELS["dc_cubby_blue"] = {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/dc_cubby_blue"}}
BLOCKSTATES["dc_cubby"] = {"variants": {"coat=0": {"model": "noclip:block/dc_cubby"},
                                        "coat=1": {"model": "noclip:block/dc_cubby_red"},
                                        "coat=2": {"model": "noclip:block/dc_cubby_blue"}}}
exit_block("dc_fire_door", "dc_fire_door", "DOOR", trigger="USE", solid=True, display="Fire Exit", sound="wood")

# ------------------------------------------------------------------ sound


def music_box(slow=1.0, seed="mb"):
    d = 24.0
    n = int(d * Y.SR)
    out = np.zeros(n)
    # twinkle-ish, but one note is always a semitone wrong
    mel = [523.3, 523.3, 784.0, 784.0, 880.0, 880.0, 784.0, 0, 698.5, 698.5, 659.3, 659.3, 587.3, 554.4, 523.3, 0]
    beat = 0.75 * slow
    for rep in range(2):
        for k, f in enumerate(mel):
            if not f:
                continue
            at = (rep * len(mel) + k) * beat
            if at > d - 1:
                break
            t = Y.t_axis(1.6)
            tone = (np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 3.01 * t) * np.exp(-t * 6)) * np.exp(-t * 2.8)
            Y.place(out, tone * 0.25, at)
    return out


def nursery_air():
    d = 24.0
    t = Y.t_axis(d)
    mb = music_box(1.0)
    from content.eb_kit import wow
    mb = wow(mb, depth=0.008, rate=0.2, seed="dcwow")
    mb = Y.reverb(Y.lp(mb, 3500), 3.0, 0.6, "dcrv")[: len(t)] * 0.35
    air = Y.lp(Y.pink(d, "dca"), 500) * 0.25
    buzz = Y.hum(d, 60.0, "dcbz", buzz=0.1, harmonics=(2,)) * 0.03
    x = Y.loopable(Y.norm(mb + air + buzz, 0.4), 1.5)
    return Y.stereoize(x, 0.0015), "stereo"


def giggle(i):
    def f():
        d = 1.4
        out = np.zeros(int(d * Y.SR))
        for k in range(4 + i):
            v = voice(f"dg{i}{k}", 0.12, pitch=420 + 40 * i - k * 15, breath=0.35, formants=[(800, 1600), (600, 2400)])
            Y.place(out, v, 0.1 + k * 0.14, 0.8 - k * 0.1)
        return Y.norm(Y.reverb(out, 2.0, 0.6, f"dgr{i}"), 0.5)
    return f


def squeak():
    d = 0.5
    t = Y.t_axis(d)
    f0 = 1800 + 900 * np.sin(np.pi * t / d)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(x, 1.0, 0.3, "dsq"), 0.5)


def block_tumble():
    out = np.zeros(int(1.6 * Y.SR))
    for k, at in enumerate((0.0, 0.18, 0.31, 0.5)):
        Y.place(out, Y.impact(f"dtt{k}", 0.15), at, 0.8 - k * 0.15)
    return Y.norm(Y.reverb(Y.bp(out, 300, 5000), 1.5, 0.4, "dttr"), 0.6)


def lullaby():
    x = music_box(1.6, "lul")[: int(12 * Y.SR)]
    from content.eb_kit import wow
    x = wow(x, depth=0.02, rate=0.15, seed="lulw")
    return Y.norm(Y.reverb(x, 3.0, 0.6, "lulr"), 0.7)


def clap():
    out = np.zeros(int(2.0 * Y.SR))
    for k in range(3):
        Y.place(out, Y.bp(Y.white(0.08, f"dcl{k}"), 800, 5000) * np.exp(-np.arange(int(0.08 * Y.SR)) / Y.SR * 50), 0.1 + k * 0.4)
    return Y.norm(Y.reverb(out, 2.5, 0.6, "dclr"), 0.5)


sound("ambient.daycare", [("ambient/daycare", nursery_air)], stream=True)
sound("ambient.daycare.additions", [(f"ambient/daycare_giggle{i}", giggle(i)) for i in range(3)] +
      [("ambient/daycare_clap", clap), ("ambient/daycare_squeak", squeak)], subtitle="A child laughs somewhere")
sound("event.daycare_toy", [("event/daycare_toy", block_tumble), ("event/daycare_squeak", squeak)], subtitle="A toy moves")
sound("event.daycare_nap", [("event/daycare_nap", lullaby)], subtitle="Nap time")
sound("event.daycare_giggle", [(f"event/daycare_giggle{i}", giggle(i + 3)) for i in range(2)], subtitle="Giggling, close")

level("daycare", fog="#e8dcc0", sky="#e8dcc0", ambient="ambient.daycare", additions="ambient.daycare.additions",
      additions_chance=0.003, fixed_time=6000, height=32)
