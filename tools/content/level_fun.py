"""Level Fun =) - an endless birthday party. Balloons, streamers, cake. Partygoers. Don't pop the balloons."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, carpet, cross, custom, BLOCK_MODELS, \
    BLOCKSTATES, lang, flicker_light
import tex as T
import synth as Y
from content.deepend_font import draw
from content.deepend_audio import wrapverb, fit

S = 32
yy, xx = np.mgrid[0:S, 0:S]

PASTELS = {
    "pink": ("#f2b8c8", "#f8cad6", "#ffffff"),
    "mint": ("#b4e6cc", "#c6efda", "#ffffff"),
    "blue": ("#b2d4f2", "#c4e0f8", "#fff6b8"),
    "lemon": ("#f4e6a0", "#f8eeb8", "#f29ab4"),
}


def paper(name):
    lo, hi, accent = PASTELS[name]
    img = T.ramp(T.fbm("fun" + name, octaves=3, base=6) * 0.5 + 0.25, lo, hi)
    if name == "pink":       # polka dots
        d = ((xx % 16 - 7.5) ** 2 + (yy % 16 - 7.5) ** 2) < 7
        d2 = (((xx + 8) % 16 - 7.5) ** 2 + ((yy + 8) % 16 - 7.5) ** 2) < 7
        img = T.blend(img, T.solid(accent), (d | d2).astype(float) * 0.85)
    elif name == "mint":     # candy stripes
        img = T.blend(img, T.solid(accent), ((xx % 8) < 3).astype(float) * 0.7)
    elif name == "blue":     # little yellow stars
        for (cx, cy) in ((7, 7), (23, 22), (24, 6), (8, 24)):
            star = (np.abs(xx - cx) + np.abs(yy - cy) * 2.2 < 3.5) | (np.abs(xx - cx) * 2.2 + np.abs(yy - cy) < 3.5)
            img = T.blend(img, T.solid(accent), star.astype(float))
    else:                    # confetti flecks
        r = T.rng("lemconf")
        for k in range(14):
            x, y = int(r.random() * 31), int(r.random() * 31)
            c = ["#f29ab4", "#7ec8f0", "#9ee0a8", "#c8a0f0"][k % 4]
            img[y:y + 2, x:x + 1] = T.hexc(c)
    return T.grain(img, "fun" + name, 0.03)


def scrawl(name, text, seed, scale=2, color="#d8202c", y=None):
    """Somebody drew on the wall in crayon. They pressed hard."""
    def f():
        img = paper(name)
        ink = draw(np.zeros((S, S, 3)), text, "#ffffff", scale=scale, y=y, jitter_seed=seed)
        m = ink[..., 0]
        wax = 0.75 + 0.25 * T.noise(seed + "wax", sigma=0.4)
        img = T.blend(img, T.solid(color), m * wax)
        return T.grain(img, seed, 0.03)
    return f


def baseboard():
    img = paper("pink")
    board = T.ramp(T.noise("funbb", sigma=1), "#f6f2ee", "#fffcf8")
    img[S - 7:] = board[S - 7:]
    img[S - 7] = T.hexc("#d8d0cc")
    # crayon scribbles low on the wall, where children would reach
    r = T.rng("funscrib")
    for k in range(3):
        x0, y0 = int(r.random() * 26), 14 + int(r.random() * 8)
        c = T.hexc(["#3a6ad8", "#d8202c", "#2a9a3a"][k])
        for t in range(18):
            x = int(x0 + t * 0.4) % S
            y = int(y0 + 2.5 * np.sin(t * 1.3 + k))
            img[y, x] = c
    return T.grain(img, "funbb", 0.03)


def party_carpet(stained=False):
    def f():
        base = T.ramp(T.fbm("funcarp", octaves=3, base=4) * 0.6 + T.noise("funcf", sigma=0.5) * 0.4,
                      "#5a6ec0", "#6a80d0", "#7a90da")
        r = T.rng("funcarpdots")
        img = base.copy()
        for k in range(10):
            x, y = int(r.random() * 30), int(r.random() * 30)
            c = ["#f2c84a", "#f07aa0", "#8ee0b0"][k % 3]
            img[y:y + 2, x:x + 2] = T.hexc(c) * 0.85
        if stained:
            img = T.stains(img, "funpunch", "#8a1a3a", count=2, strength=0.6, size=8)
        return T.grain(img, "funcarp" + str(stained), 0.04)
    return f


def confetti():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("funconfetti")
    cols = ["#f04a6a", "#4aa8f0", "#f8d040", "#6ad08a", "#b07af0", "#ffffff"]
    for k in range(70):
        x, y = int(r.random() * 31), int(r.random() * 31)
        w, h = (2, 1) if r.random() < 0.5 else (1, 2)
        rgb[y:y + h, x:x + w] = T.hexc(cols[k % len(cols)])
        a[y:y + h, x:x + w] = 1
    return rgb, a


def ceiling():
    base = T.ramp(T.fbm("funceil", octaves=2, base=4), "#f2eee8", "#faf6f0")
    img = T.border(base, "#d8d2c8", 1)
    return T.grain(img, "funceil", 0.02)


def light_on():
    img = T.solid("#fff4e0")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#ffffff"), (d < 8).astype(float) * 0.7)
    return T.border(T.border(img, "#e8d8c8", 2), "#c8b8a8", 1)


def light_off():
    img = T.ramp(T.noise("funloff", sigma=2), "#8a827a", "#9a928a")
    return T.border(T.border(img, "#e8d8c8", 2), "#c8b8a8", 1)


def streamer(c1, c2, seed):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        for k, (x0, col, ln) in enumerate(((6, c1, 26), (15, c2, 30), (24, c1, 22))):
            for y in range(ln):
                x = int(round(x0 + 1.5 * np.sin(y / 3.0 + k)))
                w = 2 + (y // 3) % 2
                rgb[y, x:x + w] = T.hexc(col) * (0.85 + 0.15 * ((y // 2) % 2))
                a[y, x:x + w] = 1
        return rgb, a
    return f


def bunting():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    rgb[2:3, :] = T.hexc("#f6f2ea")
    a[2:3, :] = 1
    for k, c in enumerate(["#f04a6a", "#f8d040", "#4aa8f0", "#6ad08a"]):
        cx = 4 + k * 8
        for y in range(3, 13):
            half = (13 - y) * 0.35
            m = np.abs(np.arange(S) - cx) <= half
            rgb[y, m] = T.hexc(c)
            a[y, m] = 1
    return rgb, a


def balloon_tex(c, seed):
    def f():
        img = T.ramp(T.noise(seed, sigma=3) * 0.3 + 0.35, c, c)
        hl = np.clip(1 - np.sqrt((xx - 10) ** 2 + (yy - 9) ** 2) / 7, 0, 1)
        img = np.clip(img + hl[..., None] * 0.5, 0, 1)
        sh = np.clip((xx + yy - 34) / 30.0, 0, 1)
        return T.shade(img, 1 - sh * 0.35)
    return f


def tablecloth():
    img = T.ramp(T.noise("funtc", sigma=1.5), "#f4f0ec", "#fcfaf6")
    check = (((xx // 8) + (yy // 8)) % 2 == 0)
    img = T.blend(img, T.solid("#f2a8bc"), check.astype(float) * 0.55)
    return T.grain(T.stains(img, "funtcs", "#c84a6a", count=1, strength=0.3, size=5), "funtc", 0.02)


def frosting():
    img = T.ramp(T.noise("funfrost", sigma=1.5), "#fbe8ee", "#fff6f8")
    drip = (yy < 6 + 3 * np.abs(np.sin(xx / 2.3))).astype(float)
    img = T.blend(img, T.solid("#f48aa8"), drip * 0.0 + (yy < 3) * 0.6)
    r = T.rng("sprink")
    for k in range(30):
        x, y = int(r.random() * 31), int(r.random() * 31)
        img[y, x] = T.hexc(["#f04a6a", "#4aa8f0", "#f8d040", "#6ad08a"][k % 4])
    return img


def sponge():
    img = T.ramp(T.noise("funsponge", sigma=0.7), "#e8c890", "#f4d8a8")
    img = T.blend(img, T.solid("#f8f0f0"), ((yy % 10) < 2).astype(float))
    return img


def cake_top():
    img = frosting()
    # writing on the cake. It knows your name. It just doesn't spell it.
    return draw(img, "=)", "#d8202c", scale=3)


def candle():
    img = T.solid("#8ad0f0")
    img = T.blend(img, T.solid("#ffffff"), (((yy + xx // 2) % 6) < 2).astype(float))
    return img


def flame():
    t = np.clip(1 - np.abs(xx - 15.5) / 8 - np.abs(yy - 18) / 16, 0, 1)
    return T.ramp(t, "#f08020", "#ffd060", "#fffbe0")


def hat_tex():
    img = np.where((((xx + yy) // 4) % 2 == 0)[..., None], T.hexc("#f04a6a"), T.hexc("#f8d040"))
    return T.grain(img.astype(float), "funhat", 0.03)


def wrap(c1, c2, seed):
    def f():
        img = T.ramp(T.noise(seed, sigma=1.5), c1, c1)
        dots = (((xx % 8) - 3.5) ** 2 + ((yy % 8) - 3.5) ** 2) < 3
        img = T.blend(img, T.solid(c2), dots.astype(float) * 0.8)
        ribbon = (np.abs(xx - 15.5) < 2) | (np.abs(yy - 15.5) < 2)
        img = T.blend(img, T.solid("#f6f2ea"), ribbon.astype(float))
        return T.grain(img, seed, 0.03)
    return f


def exit_door():
    img = T.ramp(T.noise("funexit", sigma=2), "#f6f2ee", "#fffcf8")
    img = T.border(img, "#c8a0b8", 2)
    img = draw(img, "EXIT", "#2a9a3a", scale=1, y=6)
    img = draw(img, "=)", "#d8202c", scale=2, y=16)
    img = T.rect(img, 25, 22, 28, 25, "#c8b070")
    return T.grain(img, "funexit", 0.03)


def exit_door_top():
    img = T.ramp(T.noise("funexitt", sigma=2), "#f6f2ee", "#fffcf8")
    img = T.border(img, "#c8a0b8", 2)
    # a small window, black behind the glass
    img = T.rect(img, 9, 7, 23, 24, "#141018")
    img = T.rect(img, 15, 7, 17, 24, "#f6f2ee")
    return img


def banner_letter(ch):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        rgb[3:5, :] = T.hexc("#f6f2ea")
        a[3:5, :] = 1
        cols = {"H": "#f04a6a", "A": "#f8d040", "P": "#4aa8f0", "Y": "#6ad08a", "B": "#b07af0",
                "I": "#f04a6a", "R": "#f8d040", "T": "#4aa8f0", "D": "#6ad08a"}
        for y in range(5, 30):
            half = 13 - max(0, y - 18) * 1.1
            m = np.abs(np.arange(S) - 15.5) <= half
            rgb[y, m] = T.hexc(cols.get(ch, "#f04a6a"))
            a[y, m] = 1
        if ch != " ":
            rgb = draw(rgb, ch, "#ffffff", scale=3, y=9)
        return rgb, a
    return f


for n in PASTELS:
    tex(f"fun_paper_{n}", (lambda n=n: lambda: paper(n))())
SCRAWLS = [("pink", "=)", 3, None), ("mint", "=)", 3, None), ("blue", "=)=)", 2, None), ("lemon", "=)", 4, None),
           ("pink", "STAY", 2, None), ("mint", "FUN", 2, None), ("blue", "=(", 3, None)]
for idx, (n, txt, sc, y) in enumerate(SCRAWLS):
    tex(f"fun_scrawl_{idx}", scrawl(n, txt, f"funscr{idx}", sc, y=y))
tex("fun_baseboard", baseboard)
tex("fun_carpet", party_carpet())
tex("fun_carpet_stained", party_carpet(True))
tex("fun_confetti", confetti)
tex("fun_ceiling", ceiling)
tex("fun_light", light_on)
tex("fun_light_off", light_off)
tex("fun_streamer_a", streamer("#f04a6a", "#f8d040", "sa"))
tex("fun_streamer_b", streamer("#4aa8f0", "#6ad08a", "sb"))
tex("fun_bunting", bunting)
BALLOONS = {"red": "#e23a4a", "blue": "#3a7ae2", "yellow": "#f2c838", "pink": "#f27ab0", "green": "#4ac26a"}
for n, c in BALLOONS.items():
    tex(f"fun_balloon_{n}", balloon_tex(c, "fb" + n))
tex("fun_string", lambda: T.solid("#e8e4dc"))
tex("fun_tablecloth", tablecloth)
tex("fun_table_leg", lambda: T.grain(T.solid("#c8c4bc"), "funleg", 0.03))
tex("fun_frosting", frosting)
tex("fun_sponge", sponge)
tex("fun_cake_top", cake_top)
tex("fun_candle", candle)
tex("fun_flame", flame)
tex("fun_hat", hat_tex)
tex("fun_wrap_a", wrap("#e23a4a", "#f8d040", "fwa"))
tex("fun_wrap_b", wrap("#3a7ae2", "#f6f2ea", "fwb"))
tex("fun_exit_door", exit_door)
tex("fun_exit_door_top", exit_door_top)
LETTERS = sorted(set("HAPYBIRTD"))
for ch in LETTERS:
    tex(f"fun_banner_{ch.lower()}", banner_letter(ch))

for n in PASTELS:
    cube(f"party_wallpaper_{n}", f"fun_paper_{n}", display=f"Party Wallpaper", sound="wool")
for idx in range(len(SCRAWLS)):
    cube(f"party_wallpaper_scrawl_{idx}", f"fun_scrawl_{idx}", display="Scrawled Party Wallpaper", sound="wool")
cube("party_baseboard", "fun_baseboard", display="Party Baseboard", sound="wool")
cube("party_carpet", "fun_carpet", display="Party Carpet", sound="wool")
cube("party_carpet_stained", "fun_carpet_stained", display="Punch-Stained Party Carpet", sound="wool")
carpet("party_confetti", "fun_confetti", display="Confetti")
cube("party_ceiling", "fun_ceiling", display="Party Ceiling", sound="wool", flat=True)
flicker_light("party_light", "fun_light", "fun_light_off", "fun_ceiling", light=15, flicker=0.0, display="Party Light")
flicker_light("party_light_flicker", "fun_light", "fun_light_off", "fun_ceiling", light=15, flicker=0.5,
              display="Flickering Party Light")
cross("party_streamers_a", "fun_streamer_a", display="Streamers", sound="wool")
cross("party_streamers_b", "fun_streamer_b", display="Streamers", sound="wool")
shaped("party_bunting", [(0, 6, 7.5, 16, 16, 8.5, {"all": None, "north": "b", "south": "b"})], {"b": "fun_bunting"},
       display="Bunting", collides=False, sound="wool")
for ch in LETTERS:
    shaped(f"party_banner_{ch.lower()}", [(0, 0, 14.5, 16, 16, 15.5, {"all": None, "north": "b", "south": "b"})],
           {"b": f"fun_banner_{ch.lower()}"}, display="Birthday Banner", collides=False, sound="wool")


def balloon_model(n, high):
    """A balloon on a string. high=True floats it near the ceiling; otherwise it bobs at head height."""
    by = 6 if high else 0
    boxes = [
        (5, by + 4, 5, 11, by + 10, 11, "rubber"),
        (4, by + 5, 6, 12, by + 9, 10, "rubber"),
        (6, by + 5, 4, 10, by + 9, 12, "rubber"),
        (6.5, by + 10, 6.5, 9.5, by + 11, 9.5, "rubber"),
        (7.5, by + 3, 7.5, 8.5, by + 4, 8.5, "rubber"),
        (7.8, 0, 7.8, 8.2, by + 3, 8.2, "string"),
    ]
    return boxes


for n in BALLOONS:
    for high in (False, True):
        name = f"party_balloon_{n}" + ("_high" if high else "")
        boxes = balloon_model(n, high)
        elements = []
        for (x1, y1, z1, x2, y2, z2, k) in boxes:
            faces = {f: {"texture": "#" + k} for f in ("down", "up", "north", "south", "west", "east")}
            elements.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces})
        model = {"parent": "minecraft:block/block", "elements": elements,
                 "textures": {"rubber": f"noclip:block/fun_balloon_{n}", "string": "noclip:block/fun_string",
                              "particle": f"noclip:block/fun_balloon_{n}"}}
        custom(name, "com.noclip.block.FunBalloonBlock", model, display="Balloon", render="cutout", sound="wool",
               unbreakable=False, strength=0.0, no_collision=True)

shaped("party_table", [
    (0, 13, 0, 16, 16, 16, {"all": "cloth"}),
    (0, 9, 0, 16, 13, 0.5, "cloth"), (0, 9, 15.5, 16, 13, 16, "cloth"),
    (0, 9, 0, 0.5, 13, 16, "cloth"), (15.5, 9, 0, 16, 13, 16, "cloth"),
    (2, 0, 2, 3, 13, 3, "leg"), (13, 0, 2, 14, 13, 3, "leg"), (2, 0, 13, 3, 13, 14, "leg"), (13, 0, 13, 14, 13, 14, "leg"),
], {"cloth": "fun_tablecloth", "leg": "fun_table_leg"}, display="Party Table", sound="wood", unbreakable=True)

shaped("party_cake", [
    (2, 0, 2, 14, 6, 14, {"all": "sponge", "up": "frost"}),
    (4, 6, 4, 12, 10, 12, {"all": "sponge", "up": "top"}),
    (5, 10, 6, 6, 13, 7, "candle"), (10, 10, 6, 11, 13, 7, "candle"), (7.5, 10, 10, 8.5, 13, 11, "candle"),
    (5, 13, 6, 6, 14.5, 7, "flame"), (10, 13, 6, 11, 14.5, 7, "flame"), (7.5, 13, 10, 8.5, 14.5, 11, "flame"),
], {"sponge": "fun_sponge", "frost": "fun_frosting", "top": "fun_cake_top", "candle": "fun_candle",
    "flame": "fun_flame"}, display="Birthday Cake", light=8, sound="wool", kind="pickup",
    item="noclip:almond_water*1", collides=False, strength=0.2)

shaped("party_hat", [
    (5, 0, 5, 11, 3, 11, "hat"), (6, 3, 6, 10, 6, 10, "hat"), (7, 6, 7, 9, 9, 9, "hat"), (7.5, 9, 7.5, 8.5, 11, 8.5, "flame"),
], {"hat": "fun_hat", "flame": "fun_balloon_yellow"}, display="Party Hat", collides=False, sound="wool")

for v, t in (("a", "fun_wrap_a"), ("b", "fun_wrap_b")):
    model = {"parent": "minecraft:block/block", "textures": {"w": f"noclip:block/{t}", "particle": f"noclip:block/{t}"},
             "elements": [
                 {"from": [2, 0, 2], "to": [14, 10, 14], "faces": {f: {"texture": "#w"} for f in
                                                                  ("down", "up", "north", "south", "west", "east")}},
                 {"from": [1.5, 10, 1.5], "to": [14.5, 12, 14.5], "faces": {f: {"texture": "#w"} for f in
                                                                            ("down", "up", "north", "south", "west", "east")}},
                 {"from": [6, 12, 7.5], "to": [10, 15, 8.5], "faces": {f: {"texture": "#w"} for f in
                                                                      ("north", "south", "up")}},
             ]}
    custom(f"party_present_{v}", "com.noclip.block.FunPresentBlock", model, display="Present", render="cutout",
           sound="wool", unbreakable=False, strength=0.3)

exit_block("party_exit_door", "fun_exit_door", "DOOR", trigger="USE", solid=True, display="Door Marked EXIT =)",
           sound="wood")
exit_block("party_exit_door_top", "fun_exit_door_top", "DOOR", trigger="USE", solid=True, display="Door Marked EXIT =)",
           sound="wood")

# ------------------------------------------------------------------ sound

NOTES = {"G4": 392.0, "A4": 440.0, "B4": 493.88, "C5": 523.25, "D5": 587.33, "E5": 659.25, "F5": 698.46, "G5": 783.99}
SONG = [("G4", .75), ("G4", .25), ("A4", 1), ("G4", 1), ("C5", 1), ("B4", 2),
        ("G4", .75), ("G4", .25), ("A4", 1), ("G4", 1), ("D5", 1), ("C5", 2),
        ("G4", .75), ("G4", .25), ("G5", 1), ("E5", 1), ("C5", 1), ("B4", 1), ("A4", 2),
        ("F5", .75), ("F5", .25), ("E5", 1), ("C5", 1), ("D5", 1), ("C5", 3)]


def music_box(beat=0.42, detune=0.0, wow=0.0, seed="mb", stop_at=None):
    total = sum(d for _, d in SONG) * beat + 2.0
    out = np.zeros(int(total * Y.SR))
    t = 0.3
    for idx, (n, d) in enumerate(SONG):
        if stop_at is not None and idx >= stop_at:
            break
        dur = 1.6
        tt = Y.t_axis(dur)
        f0 = NOTES[n] * (1 + detune * np.sin(idx * 1.7))
        warble = 1 + wow * np.sin(2 * np.pi * 0.7 * (tt + t))
        ph = 2 * np.pi * np.cumsum(f0 * warble) / Y.SR
        tone = (np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.15 * np.sin(4.02 * ph)) * np.exp(-tt * 3.2)
        Y.place(out, tone, t, 0.6)
        t += d * beat
    return out


def party_ambience():
    """Next room over: a music box playing Happy Birthday, too slow, and a crowd that never gets louder."""
    d = 26.0
    n = int(d * Y.SR)
    song = fit(music_box(beat=0.58, detune=0.012, wow=0.008, seed="amb"), n)
    song = Y.lp(song, 1600) * 0.5
    crowd = np.zeros(n)
    for k in range(6):
        crowd += fit(Y.whisper(f"funcrowd{k}", d), n) * (0.6 + 0.1 * k)
    crowd = Y.lp(crowd, 900) * 0.25
    t = Y.t_axis(d)
    hum = Y.hum(d, 60.0, "funhum", buzz=0.08, harmonics=(1, 2, 3)) * 0.05
    x = wrapverb(song + crowd, 2.8, 0.55, "funroom", damp=2500) + Y.lp(hum, 400)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.2), 0.0015), "stereo"


def party_horn():
    """A party blower, sad and wet, from the next room."""
    d = 1.8
    t = Y.t_axis(d)
    f0 = 340 + 40 * np.sin(2 * np.pi * 3 * t) - t * 60
    ph = 2 * np.pi * np.cumsum(f0) / Y.SR
    x = np.sign(np.sin(ph)) * 0.4 + np.sin(2 * ph) * 0.3
    e = np.clip(t / 0.05, 0, 1) * np.clip((1.3 - t) / 0.3, 0, 1)
    x = Y.bp(x * e, 300, 3000) + Y.bp(Y.white(d, "phn"), 800, 2500) * e * 0.1
    return Y.reverb(Y.norm(x, 0.7), 2.0, 0.5, "phorn")


def wind_down():
    """The music box running out, two notes at a time."""
    x = music_box(beat=0.9, detune=0.03, wow=0.02, seed="wd", stop_at=7)
    x = x[: int(6.5 * Y.SR)]
    t = Y.t_axis(len(x) / Y.SR)
    x *= np.clip(1 - t / 6.5, 0, 1)
    return Y.reverb(Y.lp(x, 2000), 2.5, 0.55, "wdown")


def cheer():
    """Everyone in the next room cheers. Then stops, all at once."""
    d = 3.0
    n = int(d * Y.SR)
    x = np.zeros(n)
    for k in range(10):
        x += fit(Y.whisper(f"cheer{k}", 1.4), n) * (0.5 + 0.05 * k)
    x = Y.bp(x, 300, 3000)
    t = Y.t_axis(d)
    x *= np.clip(t / 0.2, 0, 1) * (t < 1.35)
    return Y.reverb(Y.norm(x, 0.7), 2.0, 0.55, "cheer")


def balloon_squeak():
    d = 1.0
    t = Y.t_axis(d)
    f0 = 900 + 500 * np.sin(2 * np.pi * 2.2 * t) ** 2
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * t / d) ** 2
    x += Y.bp(Y.white(d, "sq"), 1500, 5000) * 0.1 * np.sin(np.pi * t / d)
    return Y.reverb(Y.norm(x, 0.5), 1.5, 0.45, "squeak")


def pop():
    d = 1.2
    n = int(d * Y.SR)
    w = Y.white(d, "pop")
    x = Y.bp(w, 400, 9000) * Y.decay(n, 0.008) * 1.6 + Y.lp(w, 250) * Y.decay(n, 0.03)
    return Y.norm(Y.reverb(x, 1.2, 0.35, "pop"), 0.95)


def surprise():
    d = 3.2
    n = int(d * Y.SR)
    out = np.zeros(n)
    Y.place(out, party_horn()[: int(1.8 * Y.SR)], 0.0, 0.7)
    c = cheer()
    Y.place(out, c[: n], 0.25, 0.9)
    return Y.norm(out, 0.9)


def paper_rip():
    d = 0.9
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "rip"), 1200, 8000) * (0.4 + 0.6 * (np.sin(2 * np.pi * 37 * t) > 0)) * np.exp(-t * 3)
    return Y.norm(x, 0.7)


sound("ambient.level_fun", [("ambient/level_fun", party_ambience)], stream=True)
sound("ambient.level_fun.additions",
      [("ambient/level_fun_horn", party_horn), ("ambient/level_fun_winddown", wind_down),
       ("ambient/level_fun_cheer", cheer), ("ambient/level_fun_squeak", balloon_squeak)],
      subtitle="The party goes on")
sound("event.level_fun_pop", [("event/level_fun_pop", pop)], subtitle="A balloon pops")
sound("event.level_fun_surprise", [("event/level_fun_surprise", surprise)], subtitle="SURPRISE")
sound("event.level_fun_present", [("event/level_fun_present", paper_rip)], subtitle="Wrapping paper tears")

level("level_fun", fog="#f2d8e0", sky="#f2d8e0", ambient="ambient.level_fun", additions="ambient.level_fun.additions",
      additions_chance=0.005, fixed_time=6000, height=48)
