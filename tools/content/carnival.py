"""Closed Carnival. Midway lights, game booths, a carousel turning with nobody on it and a
ferris wheel that never stops. The rides run for you. The only way out is up: ride the
wheel to the top."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, custom, exit_block, level, sound, BLOCK_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import animated, sliced, draw_text, centered, text_width, box_model, facing_states, paint_metal

S = 32
tx = A.tex

# ------------------------------------------------------------------ ground


def sawdust():
    img = T.ramp(T.noise("cvsd", sigma=0.5) * 0.6 + T.fbm("cvsd2", octaves=3, base=4) * 0.4, "#6a5232", "#8a6c44", "#a4865a")
    r = T.rng("cvsdc")
    for _ in range(40):
        x, y = int(r.random() * S), int(r.random() * S)
        img[y, x] = T.hexc("#c4a474")
    return T.grain(img, "cvsd", 0.05)


def trampled():
    img = T.ramp(T.fbm("cvgr", octaves=4, base=4), "#2e3a1e", "#3e4a28", "#52583a")
    mud = (T.fbm("cvmud", octaves=3, base=5) > 0.6).astype(float)
    img = T.blend(img, T.solid("#3a2e20"), mud * 0.6)
    return T.grain(img, "cvgr", 0.05)


def dirt():
    return T.grain(T.ramp(T.fbm("cvdirt", octaves=4, base=4), "#2e2418", "#3e3224"), "cvdirt", 0.05)


def litter(i):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        r = T.rng("cvlit", i)
        items = [("#e8e0d0", "#c03028"), ("#f0d060", "#e8e8e0"), ("#e04070", "#e8e8e0"), ("#d8b878", "#a83a2a")]
        for k in range(3 + i):
            c1, c2 = items[int(r.random() * len(items))]
            x, y = int(r.random() * 24) + 2, int(r.random() * 24) + 2
            w, h = 3 + int(r.random() * 4), 2 + int(r.random() * 4)
            rgb[y:y + h, x:x + w] = T.hexc(c1)
            rgb[y:y + 1, x:x + w] = T.hexc(c2)
            a[y:y + h, x:x + w] = 1
        # torn tickets
        for k in range(4):
            x, y = int(r.random() * 28), int(r.random() * 28)
            rgb[y:y + 2, x:x + 4] = T.hexc("#e85a3a")
            a[y:y + 2, x:x + 4] = 1
        return rgb, a
    return f


tx("cv_sawdust", sawdust)
tx("cv_grass", trampled)
tx("cv_dirt", dirt)
cube("cv_sawdust", "cv_sawdust", sound="sand", display="Sawdust")
cube_faces("cv_grass", top="cv_grass", bottom="cv_dirt", side="cv_dirt", sound="grass", display="Trampled Grass")
cube("cv_dirt", "cv_dirt", sound="gravel", display="Fairground Dirt")
for i in range(3):
    tx(f"cv_litter_{i}", litter(i))
    shaped(f"cv_litter_{i}", [(0, 0, 0, 16, 0.3, 16, {"all": None, "up": "l"})], {"l": f"cv_litter_{i}"},
           display="Litter", collides=False, unbreakable=True, sound="wool")

# ------------------------------------------------------------------ lights


def bulbs(i, colors=("#ffd860", "#ff6a4a", "#7ad0ff", "#a0ff8a", "#ff8ad8"), spacing=8):
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    rgb[15:17, :] = T.hexc("#141414")
    a[15:17, :] = 1
    for k in range(S // spacing):
        cx = k * spacing + spacing // 2
        on = ((k + i) % 3) != 0
        col = T.hexc(colors[(k + i // 3) % len(colors)])
        m = ((xx - cx) ** 2 + (yy - 19) ** 2) < 7
        rgb[m] = col if on else col * 0.3
        a[m] = 1
        if on:
            glow = (((xx - cx) ** 2 + (yy - 19) ** 2) < 14) & ~m
            rgb[glow] = col * 0.8
            a[glow] = 0.6
    return rgb, a


animated("cv_string", lambda i: bulbs(i), 6, frametime=6)
shaped("cv_string_lights", [(0, 4, 7.5, 16, 12, 8.5, {"all": None, "north": "s!", "south": "s!"})],
       {"s": "cv_string"}, display="String Lights", light=12, emissive=True, collides=False, unbreakable=True,
       sound="glass", render="translucent")
shaped("cv_string_sag", [(0, 0, 7.5, 16, 8, 8.5, {"all": None, "north": "s!", "south": "s!"})],
       {"s": "cv_string"}, display="String Lights", light=12, emissive=True, collides=False, unbreakable=True,
       sound="glass", render="translucent")


def marquee(i):
    img = T.solid("#1a1414")
    yy, xx = np.mgrid[0:S, 0:S]
    for k in range(4):
        for j in range(4):
            cx, cy = 4 + k * 8, 4 + j * 8
            on = ((k + j + i) % 2) == 0
            m = ((xx - cx) ** 2 + (yy - cy) ** 2) < 6
            img[m] = T.hexc("#fff2b0") if on else T.hexc("#4a3a20")
    return img


animated("cv_marquee", marquee, 2, frametime=8)
cube("cv_marquee", "cv_marquee", display="Marquee Lights", light=14, emissive=True, flat=True, sound="glass")


def pole():
    img = T.ramp(T.noise("cvpole", sigma=1.5), "#c8c4b8", "#d8d4c8")
    yy, xx = np.mgrid[0:S, 0:S]
    stripe = (((yy + xx) // 5) % 2 == 0)
    img = T.blend(img, T.solid("#b02a22"), stripe * 0.9)
    return T.grain(img, "cvpole", 0.03)


tx("cv_pole", pole)
shaped("cv_pole", [(6.5, 0, 6.5, 9.5, 16, 9.5, "p")], {"p": "cv_pole"}, display="Striped Pole", unbreakable=True)

# ------------------------------------------------------------------ booths

GAMES = ["RING TOSS", "DUCK POND", "DARTS", "FORTUNES"]
STRIPES = [("#c8241c", "#ece6d6"), ("#1e4a9a", "#f0c838"), ("#2a7a3a", "#ece6d6"), ("#6a2a8a", "#f0c838")]


def awning(c1, c2):
    def f():
        xx = np.mgrid[0:S, 0:S][1]
        img = np.where(((xx // 8) % 2 == 0)[..., None], T.hexc(c1), T.hexc(c2))
        img = T.shade(img, 0.92 + T.noise("cvaw" + c1, sigma=2) * 0.1)
        yy = np.mgrid[0:S, 0:S][0]
        scallop = (yy > 26) & (((xx % 8) - 4) ** 2 + (yy - 26) ** 2 * 2 > 18)
        img[scallop] = img[scallop] * 0.5
        return T.grain(img, "cvaw" + c1, 0.03)
    return f


def sign(text, c1, c2):
    def f():
        w, h = 3 * S, S
        img = np.ones((h, w, 3)) * T.hexc(c2)
        img = T.shade(img, 0.92 + T.noise("cvsg" + text, shape=(h, w), sigma=3) * 0.1)
        img[:3] = T.hexc(c1)
        img[-3:] = T.hexc(c1)
        sc = 2 if text_width(text, 2, 2) < w - 8 else 1
        img = draw_text(img, text, (w - text_width(text, sc, 2)) // 2, (h - 5 * sc) // 2, c1, scale=sc, gap=2)
        # bulbs around the edge
        for x in range(4, w, 8):
            img[1:3, x:x + 2] = T.hexc("#fff2b0")
            img[-3:-1, x:x + 2] = T.hexc("#fff2b0")
        return img
    return f


def prize_wall(variant):
    def f():
        img = T.ramp(T.noise("cvpw" + str(variant), sigma=2), "#3a2a22", "#4a362a")
        r = T.rng("cvpwp", variant)
        cols = ["#e85a8a", "#7ac8f0", "#f0d060", "#a0e08a", "#c8a0f0", "#f0a060"]
        for k in range(6):
            cx, cy = 5 + (k % 3) * 11, 8 + (k // 3) * 14
            c = cols[int(r.random() * len(cols))]
            yy, xx = np.mgrid[0:S, 0:S]
            body = ((xx - cx) ** 2 / 9 + (yy - cy - 3) ** 2 / 12) < 1
            head = ((xx - cx) ** 2 + (yy - cy + 2) ** 2) < 7
            ears = ((xx - cx - 2.5) ** 2 + (yy - cy + 4.5) ** 2 < 2) | ((xx - cx + 2.5) ** 2 + (yy - cy + 4.5) ** 2 < 2)
            m = body | head | ears
            img[m] = T.hexc(c)
            img[(head & (np.abs(xx - cx) == 1) & (yy == cy - 2))] = T.hexc("#101010")
        return T.grain(img, "cvpw" + str(variant), 0.03)
    return f


def counter(variant):
    def f():
        img = T.ramp(T.noise("cvct" + str(variant), sigma=2), "#8a6a44", "#9a7a50")
        img[:4] = T.hexc("#ece6d6")
        return T.grain(img, "cvct", 0.04)
    return f


def counter_top(variant):
    def f():
        img = T.ramp(T.noise("cvcto", sigma=2), "#7a5a3a", "#8a6a44")
        yy, xx = np.mgrid[0:S, 0:S]
        r = T.rng("cvcto", variant)
        if variant == 0:      # bottles
            for k in range(12):
                cx, cy = 3 + (k % 6) * 5, 8 + (k // 6) * 12
                img[((xx - cx) ** 2 + (yy - cy) ** 2) < 5] = T.hexc("#2a6a3a")
                img[((xx - cx) ** 2 + (yy - cy) ** 2) < 1.5] = T.hexc("#8ac89a")
        elif variant == 1:    # ducks in a trough
            img = T.rect(img, 2, 6, 30, 26, "#2a4a6a")
            for k in range(5):
                cx, cy = 5 + k * 6, 12 + (k % 2) * 6
                img[((xx - cx) ** 2 + (yy - cy) ** 2) < 5] = T.hexc("#f0d040")
                img[((xx - cx - 2) ** 2 + (yy - cy) ** 2) < 1] = T.hexc("#e86a20")
        elif variant == 2:    # darts
            for k in range(6):
                x = 4 + k * 5
                img = T.rect(img, x, 10, x + 1, 22, "#c8c8c0")
                img = T.rect(img, x - 1, 20, x + 2, 23, "#c03028")
        else:                 # tarot cards
            for k in range(3):
                img = T.rect(img, 4 + k * 9, 8, 11 + k * 9, 20, "#e8dcc0")
                img = T.rect(img, 5 + k * 9, 9, 10 + k * 9, 19, "#5a2a6a")
        return T.grain(img, "cvcto" + str(variant), 0.03)
    return f


def bulb_row():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for x in range(2, S, 6):
        rgb[12:18, x:x + 3] = T.hexc("#fff2c0")
        a[12:18, x:x + 3] = 1
    return rgb, a


tx("cv_bulb_row", bulb_row)
for v, (c1, c2) in enumerate(STRIPES):
    tx(f"cv_awning_{v}", awning(c1, c2))
for v, game in enumerate(GAMES):
    c1, c2 = STRIPES[v]
    sliced(f"cv_sign_{v}", sign(game, c1, c2), 3, 1)
    tx(f"cv_prizes_{v}", prize_wall(v))
    tx(f"cv_counter_{v}", counter(v))
    tx(f"cv_counter_top_{v}", counter_top(v))
    c1, c2 = STRIPES[v]
    # awning: a sloped striped canopy over the counter, with a row of bulbs underneath
    shaped(f"cv_awning_{v}", [(0, 6, 0, 16, 8, 16, {"all": "a", "down": "a"}), (0, 4, 0, 16, 6, 1, {"all": None, "north": "b!", "south": "b!"})],
           {"a": f"cv_awning_{v}", "b": "cv_bulb_row"}, display="Booth Awning", light=11, unbreakable=True,
           collides=False, sound="wool")
    for c in range(3):
        shaped(f"cv_sign_{v}_{c}", [(0, 0, 6, 16, 16, 10, {"all": "w", "north": "s", "south": "w"})],
               {"w": "cv_booth_wood", "s": f"cv_sign_{v}_{c}_0"}, display="Booth Sign", light=9, emissive=True,
               unbreakable=True, sound="wood", render="solid")
    cube(f"cv_prizes_{v}", f"cv_prizes_{v}", display="Prize Wall", sound="wool")
    shaped(f"cv_counter_{v}", [(0, 0, 2, 16, 14, 16, {"all": "c", "up": "t"})],
           {"c": f"cv_counter_{v}", "t": f"cv_counter_top_{v}"}, display="Game Counter", unbreakable=True)

tx("cv_booth_wood", lambda: T.grain(T.ramp(T.fbm("cvbw", octaves=3, base=3), "#5a3e2a", "#6a4a32"), "cvbw", 0.04))
cube("cv_booth_wood", "cv_booth_wood", display="Booth Planks", sound="wood")


def plush(c):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        yy, xx = np.mgrid[0:S, 0:S]
        body = ((xx - 16) ** 2 / 40 + (yy - 22) ** 2 / 50) < 1
        head = ((xx - 16) ** 2 + (yy - 10) ** 2) < 30
        ears = ((xx - 11) ** 2 + (yy - 5) ** 2 < 6) | ((xx - 21) ** 2 + (yy - 5) ** 2 < 6)
        m = body | head | ears
        rgb[m] = T.hexc(c)
        a[m] = 1
        eyes = ((xx - 14) ** 2 + (yy - 9) ** 2 < 1.5) | ((xx - 18) ** 2 + (yy - 9) ** 2 < 1.5)
        rgb[eyes] = T.hexc("#101010")
        # stitched mouth
        rgb[(yy == 13) & (np.abs(xx - 16) < 3)] = T.hexc("#3a1010")
        rgb[0:4, 15:17] = T.hexc("#8a8a80")
        a[0:4, 15:17] = 1
        return rgb, a
    return f


for name, c in (("pink", "#e88aa8"), ("blue", "#8ac0e8"), ("yellow", "#e8d070")):
    tx(f"cv_plush_{name}", plush(c))
    cross(f"cv_plush_{name}", f"cv_plush_{name}", display="Prize Bear")

# ------------------------------------------------------------------ carousel


def platform():
    img = T.ramp(T.noise("cvplat", sigma=2), "#8a6a44", "#9a7a50")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.25)
    return T.grain(img, "cvplat", 0.04)


def canopy(c1, c2):
    def f():
        xx = np.mgrid[0:S, 0:S][1]
        img = np.where(((xx // 16) % 2 == 0)[..., None], T.hexc(c1), T.hexc(c2))
        img = T.shade(img, 0.9 + T.noise("cvcan" + c1, sigma=2) * 0.12)
        return T.grain(img, "cvcan" + c1, 0.03)
    return f


def mirror_panel(i):
    img = T.ramp(np.linspace(0, 1, S)[:, None] * np.ones((1, S)), "#5a6a7a", "#c8d8e8")
    img = T.border(img, "#c8a040", 3)
    yy, xx = np.mgrid[0:S, 0:S]
    for k in range(4):
        on = (k + i) % 2 == 0
        img[1:3, 4 + k * 8:6 + k * 8] = T.hexc("#fff2b0") if on else T.hexc("#6a5a30")
        img[-3:-1, 4 + k * 8:6 + k * 8] = T.hexc("#fff2b0") if not on else T.hexc("#6a5a30")
    return img


tx("cv_platform", platform)
tx("cv_canopy_a", canopy("#c8241c", "#ece6d6"))
tx("cv_canopy_b", canopy("#1e4a9a", "#f0c838"))
animated("cv_mirror", mirror_panel, 2, frametime=7)
shaped("cv_platform", [(0, 0, 0, 16, 5, 16, "p")], {"p": "cv_platform"}, display="Carousel Platform", unbreakable=True)
cube("cv_canopy_a", "cv_canopy_a", display="Carousel Canopy", sound="wool")
cube("cv_canopy_b", "cv_canopy_b", display="Carousel Canopy", sound="wool")
cube("cv_mirror", "cv_mirror", display="Carousel Mirror Panel", light=12, emissive=True, flat=True, sound="glass")
tx("cv_brass", lambda: T.grain(T.ramp(T.noise("cvbrass", sigma=1), "#a88a3a", "#d0b05a"), "cvbrass", 0.03))
shaped("cv_brass_pole", [(7, 0, 7, 9, 16, 9, "b")], {"b": "cv_brass"}, display="Brass Pole", unbreakable=True,
       collides=False)


def horse_paint(c, mane):
    def f():
        img = T.ramp(T.noise("cvhorse" + c, sigma=2), c, c)
        img = T.shade(img, 0.9 + T.noise("cvh2" + c, sigma=1) * 0.15)
        return T.grain(img, "cvh" + c, 0.02)
    return f


tx("cv_horse_white", horse_paint("#e8e4dc", "#c8a040"))
tx("cv_horse_black", horse_paint("#2a2624", "#c8a040"))
tx("cv_saddle", lambda: T.grain(T.ramp(T.noise("cvsad", sigma=1), "#8a1a1a", "#a82a24"), "cvsad", 0.03))
for colour in ("white", "black"):
    shaped(f"cv_horse_{colour}", [
        (6, 6, 2, 10, 11, 13, "h"),              # body
        (6, 9, 0, 10, 15, 3, "h"),               # neck and head
        (6, 1, 3, 7, 7, 4, "h"), (9, 1, 3, 10, 7, 4, "h"),      # front legs
        (6, 1, 11, 7, 7, 12, "h"), (9, 3, 11, 10, 7, 12, "h"),  # back legs, one lifted
        (5.5, 10, 6, 10.5, 11.5, 10, "s"),       # saddle
        (7, 0, 7, 9, 16, 9, "b"),                # the pole through it
        (7.5, 8, 13, 8.5, 12, 16, "h"),          # tail
    ], {"h": f"cv_horse_{colour}", "s": "cv_saddle", "b": "cv_brass"}, display="Carousel Horse",
        unbreakable=True, collides=False)

# ------------------------------------------------------------------ ferris wheel


def truss():
    rgb = np.ones((S, S, 3)) * T.hexc("#e8e4dc")
    rgb = T.shade(rgb, 0.9 + T.noise("cvtr", sigma=2) * 0.1)
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((xx < 3) | (xx > 28) | (yy < 3) | (yy > 28) | (np.abs(xx - yy) < 2) | (np.abs(xx + yy - 31) < 2)).astype(float)
    return rgb, a


def rim(i):
    rgb, a = truss()
    yy, xx = np.mgrid[0:S, 0:S]
    for k in range(4):
        on = (k + i) % 4 < 2
        cx = 4 + k * 8
        for cy in (4, 27):
            m = ((xx - cx) ** 2 + (yy - cy) ** 2) < 5
            rgb[m] = T.hexc("#fff2b0") if on else T.hexc("#7a5a2a")
            a[m] = 1
    return rgb, a


tx("cv_truss", truss)
animated("cv_rim", rim, 4, frametime=4)
cube("cv_truss", "cv_truss", display="Steel Truss", sound="metal", render="cutout")
cube("cv_rim", "cv_rim", display="Wheel Rim Lights", sound="metal", render="cutout", light=13, emissive=True)
tx("cv_hub", lambda: paint_metal("cvhub", "#8a8a86", "#a8a8a4", rust=0.2))
cube("cv_hub", "cv_hub", display="Wheel Hub", sound="metal")


def gondola_paint(c):
    def f():
        img = T.ramp(T.noise("cvgon" + c, sigma=2), c, c)
        img = T.shade(img, 0.9 + T.noise("cvgon2" + c, sigma=1) * 0.12)
        img[:3] = T.hexc("#e8e4dc")
        return T.grain(img, "cvgon" + c, 0.03)
    return f


tx("cv_gondola", gondola_paint("#1e6a8a"))
GONDOLA = [(1, 0, 1, 15, 2, 15, "g"), (1, 2, 1, 15, 9, 2, "g"), (1, 2, 14, 15, 9, 15, "g"),
           (1, 2, 2, 2, 9, 14, "g"), (14, 2, 2, 15, 9, 14, "g"), (7.5, 9, 7.5, 8.5, 16, 8.5, "h"),
           (2, 4, 7, 14, 5, 9, "h")]
custom("cv_gondola", "com.noclip.block.GondolaBlock", box_model(GONDOLA, {"g": "cv_gondola", "h": "cv_hub"}),
       display="Ferris Wheel Car", sound="metal", unbreakable=True, render="cutout")

# ------------------------------------------------------------------ funhouse, food stand, exit gate


def clown():
    w, h = 5 * S, 4 * S
    yy, xx = np.mgrid[0:h, 0:w]
    img = np.ones((h, w, 3)) * T.hexc("#e8dcc8")
    img = T.shade(img, 0.9 + T.noise("cvclown", shape=(h, w), sigma=4) * 0.12)
    cx, cy = w / 2, h * 0.45
    face = ((xx - cx) ** 2 / (w * 0.42) ** 2 + (yy - cy) ** 2 / (h * 0.42) ** 2) < 1
    img[face] = T.hexc("#f4ece0")
    for ex in (cx - 28, cx + 28):
        eye = ((xx - ex) ** 2 / 100 + (yy - cy + 18) ** 2 / 160) < 1
        img[eye] = T.hexc("#1a1a2a")
        pupil = ((xx - ex) ** 2 + (yy - cy + 16) ** 2) < 12
        img[pupil] = T.hexc("#f0f0f0")
        tri = (np.abs(xx - ex) < (yy - (cy - 2)) * 0.6) & (yy > cy - 2) & (yy < cy + 12)
        img[tri] = T.hexc("#2a4aa8")
    nose = ((xx - cx) ** 2 + (yy - cy + 2) ** 2) < 120
    img[nose] = T.hexc("#d0201a")
    mouth = ((xx - cx) ** 2 / (52 ** 2) + (yy - cy - 30) ** 2 / (22 ** 2)) < 1
    img[mouth] = T.hexc("#c01818")
    teeth = mouth & (yy < cy + 18) & (((xx - cx) // 6) % 2 == 0)
    img[teeth] = T.hexc("#f0ece0")
    hair = (~face) & (((xx - cx) ** 2 / (w * 0.5) ** 2 + (yy - cy) ** 2 / (h * 0.5) ** 2) < 1) & (np.abs(xx - cx) > w * 0.3)
    img[hair] = T.hexc("#e86a20")
    img = T.stains(img, "cvclowns", "#6a5a40", count=3, strength=0.3, size=14)
    return T.grain(img, "cvclown", 0.03)


names = sliced("cv_clown", clown, 5, 4)
for n in names:
    cube(n, n, display="Funhouse Facade", sound="wood")
tx("cv_mouth_dark", lambda: T.blend(T.solid("#030202"), T.solid("#3a0808"), T.noise("cvmd", sigma=3) * 0.4))
exit_block("cv_funhouse_mouth", "cv_mouth_dark", "DOOR", display="Funhouse Entrance")
tx("cv_funhouse_wall", lambda: canopy("#6a2a8a", "#f0c838")())
cube("cv_funhouse_wall", "cv_funhouse_wall", display="Funhouse Wall", sound="wood")


def food_sign():
    w, h = 3 * S, S
    img = np.ones((h, w, 3)) * T.hexc("#f0c838")
    img = T.shade(img, 0.92 + T.noise("cvfood", shape=(h, w), sigma=3) * 0.08)
    img = centered(img, "CORN DOGS", 6, "#b02a22", scale=2)
    img = centered(img, "LEMONADE  POPCORN", 22, "#2a2a2a")
    return T.border(img, "#b02a22", 2)


sliced("cv_food", food_sign, 3, 1)
for c in range(3):
    shaped(f"cv_food_sign_{c}", [(0, 0, 6, 16, 16, 10, {"all": "w", "north": "s"})],
           {"w": "cv_trailer", "s": f"cv_food_{c}_0"}, display="Food Stand Sign", light=10, emissive=True,
           unbreakable=True, render="solid")
tx("cv_trailer", lambda: T.grain(T.ramp(T.noise("cvtrl", sigma=2), "#d8d4c8", "#e4e0d4"), "cvtrl", 0.03))
cube("cv_trailer", "cv_trailer", display="Food Trailer", sound="metal")


def exit_sign():
    w, h = 3 * S, S
    img = np.ones((h, w, 3)) * T.hexc("#1a1414")
    img = centered(img, "EXIT", 8, "#ff3a2a", scale=3, gap=3)
    for x in range(3, w, 7):
        img[1:3, x:x + 2] = T.hexc("#fff2b0")
        img[-3:-1, x:x + 2] = T.hexc("#fff2b0")
    return img


sliced("cv_exit", exit_sign, 3, 1)
for c in range(3):
    shaped(f"cv_exit_sign_{c}", [(0, 0, 6, 16, 16, 10, {"all": "w", "north": "s"})],
           {"w": "cv_hub", "s": f"cv_exit_{c}_0"}, display="Exit Sign", light=12, emissive=True, unbreakable=True,
           render="solid")
tx("cv_turnstile_dark", lambda: T.solid("#050404"))
exit_block("cv_turnstile", "cv_turnstile_dark", "DOOR", display="Exit Turnstile",
           model=box_model([(7, 0, 0, 9, 14, 16, "m"), (0, 8, 7, 16, 9, 9, "m")], {"m": "cv_hub"}), render="cutout")


def hay():
    img = T.ramp(T.noise("cvhay", sigma=0.6), "#a88a44", "#c8a85a")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 10) < 1) * 0.3)
    return T.grain(img, "cvhay", 0.06)


tx("cv_hay", hay)
cube("cv_hay", "cv_hay", display="Hay Bale", sound="grass")


def balloon():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    b = ((xx - 16) ** 2 / 40 + (yy - 9) ** 2 / 56) < 1
    rgb[b] = T.hexc("#d0201a")
    rgb[b & ((xx - 13) ** 2 + (yy - 6) ** 2 < 4)] = T.hexc("#ff8a7a")
    a[b] = 1
    for y in range(17, 32):
        x = int(16 + np.sin(y / 3) * 1.5)
        rgb[y, x] = T.hexc("#e8e8e0")
        a[y, x] = 1
    return rgb, a


tx("cv_balloon", balloon)
cross("cv_balloon", "cv_balloon", display="Balloon")

# ------------------------------------------------------------------ sound


def calliope_notes(seq, tempo, detune, seed):
    out = np.zeros(int((len(seq) * tempo + 2) * Y.SR))
    for i, nt in enumerate(seq):
        if nt is None:
            continue
        f = 440 * 2 ** ((nt - 69) / 12) * detune
        n = int(tempo * 1.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        wob = 1 + 0.008 * np.sin(2 * np.pi * 5.5 * tt)
        ph = 2 * np.pi * np.cumsum(f * wob) / Y.SR
        pipe = np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.3 * np.sin(3 * ph) + 0.15 * Y.white(n / Y.SR, f"{seed}{i}")[:n]
        pipe *= Y.env(n, 0.02, 0.15)
        Y.place(out, pipe, i * tempo, 0.4)
    return out


WALTZ = [67, 71, 74, 79, 78, 74, 76, 72, 69, 71, 67, None, 66, 69, 72, 76, 74, 71, 72, 67, 64, 66, 62, None]


def midway_loop():
    d = 28.0
    t = Y.t_axis(d)
    tune = calliope_notes(WALTZ, 0.62, 0.985, "cvmw")
    tune = Y.lp(tune, 1800)
    tune = Y.reverb(tune, 2.5, 0.7, "cvmw")
    music = np.zeros_like(t)
    Y.place(music, tune, 4.0, 0.35)
    gen = Y.hum(d, 60.0, "cvgen", buzz=0.4, harmonics=(1, 2, 3, 4)) * 0.04 + Y.lp(Y.brown(d, "cvgenr"), 120) * 0.25
    wind = Y.bp(Y.pink(d, "cvwind"), 300, 1500) * 0.08
    x = Y.loopable(Y.norm(music + gen + wind, 0.45), 1.5)
    return Y.stereoize(x, 0.0014), "stereo"


def bell():
    out = np.zeros(int(3.0 * Y.SR))
    n = int(2.6 * Y.SR)
    tt = np.arange(n) / Y.SR
    b = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * k) for f, k in ((880, 1.2), (2215, 2.5), (3520, 4)))
    Y.place(out, Y.impact("cvthunk", 0.3), 0.0, 0.5)
    Y.place(out, b, 0.35, 0.6)
    return Y.reverb(Y.norm(out, 0.45), 2.0, 0.6, "cvbell")


def pop():
    out = np.zeros(int(1.5 * Y.SR))
    n = int(0.15 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, Y.white(0.15, "cvpop")[:n] * np.exp(-tt * 60), 0.02, 1.0)
    return Y.reverb(Y.norm(out, 0.6), 2.0, 0.6, "cvpop")


def announcer():
    v = Y.bp(Y.whisper("cvpa", 2.6), 400, 3000) * 2
    out = np.zeros(int(4.0 * Y.SR))
    Y.place(out, Y.bp(Y.white(0.2, "cvclick"), 1000, 3000) * 0.3, 0.0)
    Y.place(out, v, 0.3)
    out = np.tanh(out * 3) * 0.5
    return Y.reverb(Y.norm(out, 0.4), 3.0, 0.75, "cvpa")


def klaxon():
    d = 2.0
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * 440 * t)) * 0.4 + np.sign(np.sin(2 * np.pi * 554 * t)) * 0.3
    x *= (t < 1.4) * Y.env(len(t), 0.02, 0.1)
    return Y.reverb(Y.lp(Y.norm(x, 0.4), 2500), 2.5, 0.6, "cvklax")


def organ():
    tune = calliope_notes([60, 64, 67, 72, 67, 64, 62, 65, 69, 74, 69, 65], 0.4, 0.97, "cvorg")
    return Y.norm(Y.lp(tune[: int(5.2 * Y.SR)], 2500), 0.5)


def creak():
    d = 2.5
    t = Y.t_axis(d)
    f = 55 + 20 * np.sin(np.pi * t / d)
    x = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)), 150, 1200) * np.sin(np.pi * t / d) ** 2
    x += Y.lp(Y.brown(d, "cvmotor"), 200) * 0.5
    return Y.norm(x, 0.5)


def apex():
    out = np.zeros(int(4.0 * Y.SR))
    for i, f in enumerate((523.3, 659.3, 784.0, 1046.5)):
        n = int(2.0 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, np.sin(2 * np.pi * f * tt) * np.exp(-tt * 1.5), i * 0.25, 0.4)
    return Y.reverb(Y.norm(out, 0.5), 3.0, 0.6, "cvapex")


sound("ambient.carnival", [("ambient/carnival", midway_loop)], stream=True)
sound("ambient.carnival.additions",
      [("ambient/carnival_bell", bell), ("ambient/carnival_pop", pop), ("ambient/carnival_pa", announcer),
       ("ambient/carnival_klaxon", klaxon)],
      subtitle="Something on the midway")
sound("event.carnival_organ", [("event/carnival_organ", organ)], subtitle="The carousel organ plays")
sound("event.carnival_creak", [("event/carnival_creak", creak)], subtitle="The wheel turns")
sound("event.carnival_apex", [("event/carnival_apex", apex)], subtitle="The top of the wheel")

level("carnival", fog="#120c14", sky="#080610", ambient="ambient.carnival", additions="ambient.carnival.additions",
      additions_chance=0.004, fixed_time=18000, skylight=True, ceiling=False, height=96)
