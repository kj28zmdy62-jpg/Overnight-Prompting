"""The Waiting Room - take a number. Rows of orange plastic chairs, a ticket dispenser, a NOW SERVING
display and a voice calling numbers. When your number is called a counter door opens. Miss it and the
room gets bigger."""
import numpy as np
import assets as A
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, level, sound
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32


def vinyl(seed, worn=False):
    yy, xx = np.mgrid[0:S, 0:S]
    cell = T.rng(seed).random((2, 2))
    tint = np.kron(cell, np.ones((16, 16)))
    img = T.ramp(tint * 0.4 + T.noise(seed + "n", sigma=1.5) * 0.3 + 0.15, "#b4ad9c", "#c4bdab")
    flecks = T.rng(seed + "f").random((S, S))
    img = T.blend(img, T.solid("#6e6a60"), (flecks > 0.93).astype(float) * 0.5)
    img = T.blend(img, T.solid("#e8e2d2"), (flecks < 0.04).astype(float) * 0.5)
    img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.2)
    if worn:
        scuff = np.clip(T.fbm(seed + "w", octaves=3, base=5) - 0.35, 0, 1) * 1.4
        img = T.blend(img, T.solid("#8e8676"), scuff * 0.55)
        for k in range(3):
            m = K.line_mask(S, S, *(T.rng(seed, k).random(4) * S), 1.0)
            img = T.blend(img, T.solid("#3a3632"), m * 0.35)
    return T.grain(img, seed, 0.03)


def wall_upper():
    img = T.ramp(T.fbm("wru", octaves=4, base=4) * 0.6 + 0.2, "#9fb0b8", "#aebfc6")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.06)  # painted block wall
    img = T.shade(img, 1 - (((xx + (yy // 8) % 2 * 8) % 16) == 0) * 0.06)
    return T.grain(T.stains(img, "wrus", "#7a8a8e", count=1, strength=0.12, size=10), "wru", 0.03)


def wall_lower():
    img = T.ramp(T.fbm("wrl", octaves=3, base=4), "#3e5868", "#466272")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.12)
    img[:3] = T.hexc("#c8c2b2")
    img[3] = T.hexc("#7a766c")
    img[28:] = T.hexc("#22262a")
    img = K.grime_bottom(img, "wrlg", 10, 0.25)
    # chair backs have scraped the paint
    for k in range(4):
        y = 8 + int(T.rng("wrsc", k).random() * 14)
        x0 = int(T.rng("wrsx", k).random() * 24)
        img[y, x0:x0 + 6] = T.hexc("#7a8e98")
    return T.grain(img, "wrl", 0.03)


def ceiling():
    base = T.ramp(T.fbm("wrc", octaves=2, base=4), "#d4d2c8", "#dedcd2")
    pits = (T.rng("wrcp").random((S, S)) > 0.86).astype(float)
    img = T.shade(base, 1 - pits * 0.1)
    img = T.border(img, "#a8a69c", 1)
    return T.grain(img, "wrc", 0.02)


def light_panel(on=True):
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        img = T.solid("#f4fbff")
        img = T.shade(img, 1 - ((xx % 2 == 0) & (yy % 2 == 0)) * 0.04)
        img = T.blend(img, T.solid("#dff0ff"), (np.abs((xx % 16) - 8) > 5) * 0.4)
    else:
        img = T.ramp(T.noise("wrlo", sigma=2), "#6c7074", "#7c8084")
        img = T.rect(img, 7, 3, 9, 29, "#4c5054")
        img = T.rect(img, 23, 3, 25, 29, "#4c5054")
    img = T.border(img, "#b0b0aa", 2)
    return T.border(img, "#7c7c76", 1)


def plastic_orange():
    img = T.ramp(T.noise("wrpo", sigma=2), "#c8641e", "#d87428")
    return T.grain(T.shade(img, 1 + np.clip(T.noise("wrpos", sigma=3) - 0.65, 0, 1) * 0.6), "wrpo", 0.02)


def chrome():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp((np.sin(yy / 3.0) * 0.5 + 0.5) * 0.5 + T.noise("wrch", sigma=1) * 0.5, "#7a7e82", "#d0d4d8")
    return T.grain(img, "wrch", 0.02)


def digit_tex(d):
    """Red seven-segment digit on black, with the dead segments faintly visible."""
    segs = {"0": "abcdef", "1": "bc", "2": "abged", "3": "abgcd", "4": "fgbc", "5": "afgcd", "6": "afgedc",
            "7": "abc", "8": "abcdefg", "9": "abcdfg", " ": ""}
    img = T.ramp(T.noise("wrdg", sigma=1.5), "#0c0606", "#140a0a")
    yy, xx = np.mgrid[0:S, 0:S]
    rects = {"a": (9, 4, 23, 7), "g": (9, 15, 23, 18), "d": (9, 26, 23, 29),
             "f": (7, 6, 10, 16), "b": (22, 6, 25, 16), "e": (7, 17, 10, 27), "c": (22, 17, 25, 27)}
    on = np.zeros((S, S))
    off = np.zeros((S, S))
    for k, (x0, y0, x1, y1) in rects.items():
        m = ((xx >= x0) & (xx < x1) & (yy >= y0) & (yy < y1)).astype(float)
        if k in segs[d]:
            on = np.maximum(on, m)
        else:
            off = np.maximum(off, m)
    img = T.blend(img, T.solid("#3a0e0c"), off * 0.8)
    img = T.blend(img, T.solid("#ff2a16"), K.glow(on, 1.6) * 0.5)
    img = T.blend(img, T.solid("#ff7a5a"), on)
    img = T.border(img, "#222222", 1)
    return img


def label_tex(text, part):
    h, w = 32, 64
    img = T.ramp(T.noise("wrlb" + text, sigma=1.5, shape=(h, w)), "#0c0606", "#140a0a")
    words = text.split()
    m = np.zeros((h, w))
    if len(words) == 2:
        m = np.maximum(m, K.centered(words[0], h, w, 5, font=K.F57, scale=1))
        m = np.maximum(m, K.centered(words[1], h, w, 18, font=K.F57, scale=1))
    else:
        m = K.centered(text, h, w, 12, font=K.F57, scale=1)
    img = T.blend(img, T.solid("#ff2a16", shape=(h, w)), K.glow(m, 1.2) * 0.4)
    img = T.blend(img, T.solid("#ff8a6a", shape=(h, w)), m)
    img[0], img[-1] = T.hexc("#222222"), T.hexc("#222222")
    if part == 0:
        img[:, 0] = T.hexc("#222222")
    else:
        img[:, -1] = T.hexc("#222222")
    return img[:, part * 32:(part + 1) * 32]


def poster(lines, bg, fg, seed):
    def f():
        img = T.ramp(T.noise(seed, sigma=2), bg, bg)
        img = T.shade(img, 0.92 + T.noise(seed + "p", sigma=3) * 0.12)
        y = 16 - len(lines) * 4
        for ln in lines:
            img = T.blend(img, T.solid(fg), K.centered(ln, S, S, y, font=K.F35))
            y += 7
        img = T.border(img, "#f0ece0", 1)
        # tape and age
        img = T.rect(img, 1, 1, 4, 3, "#d8d0a0", 0.7)
        img = T.rect(img, 28, 1, 31, 3, "#d8d0a0", 0.7)
        img = T.stains(img, seed + "s", "#8a7a50", count=1, strength=0.2, size=6)
        wall = wall_upper()
        out = wall.copy()
        out[2:30, 2:30] = img[2:30, 2:30]
        return T.grain(out, seed, 0.02)
    return f


def clock():
    img = wall_upper()
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#2a2a2a"), ((d < 13) & (d >= 11.5)).astype(float))
    img = T.blend(img, T.solid("#f2f0e8"), (d < 11.5).astype(float))
    for h in range(12):
        a = h / 12 * 2 * np.pi
        x, y = 15.5 + np.sin(a) * 9.5, 15.5 - np.cos(a) * 9.5
        img = T.blend(img, T.solid("#2a2a2a"), K.disc(S, S, x, y, 0.7).astype(float))
    # it has said 3:07 for a long time
    img = T.blend(img, T.solid("#1a1a1a"), K.line_mask(S, S, 15.5, 15.5, 15.5 + 6, 15.5 + 0.5, 1.6))
    img = T.blend(img, T.solid("#1a1a1a"), K.line_mask(S, S, 15.5, 15.5, 15.5 + 4.6, 15.5 - 7.4, 1.0))
    img = T.blend(img, T.solid("#c02020"), K.line_mask(S, S, 15.5, 15.5, 15.5 - 3, 15.5 + 8, 0.6))
    return img


def entrance():
    img = T.ramp(T.noise("wren", sigma=3), "#05070a", "#0a0e14")
    yy, xx = np.mgrid[0:S, 0:S]
    streak = np.clip(1 - np.abs((xx + yy * 0.5) % 30 - 10) / 3, 0, 1) * 0.25
    img = T.blend(img, T.solid("#5a6a78"), streak)
    img = T.border(img, "#8a9094", 2)
    img = T.rect(img, 3, 14, 29, 16, "#a8aeb2")
    return img


def entrance_sign():
    img = entrance()
    m = K.centered("CLOSED", S, S, 4, font=K.F35)
    return T.blend(img, T.solid("#c8c0b0"), m * 0.6)


def counter_door(top):
    img = T.ramp(T.noise("wrcd" + str(top), sigma=2), "#8a7a5e", "#968668")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 7) == 0) * 0.08)  # wood veneer grain
    img = T.border(img, "#4a4236", 2, sides="we" + ("n" if top else "s"))
    if top:
        img = T.rect(img, 10, 6, 22, 22, "#2a3034")
        img = T.rect(img, 11, 7, 21, 21, "#c4ccd0", 0.5)
        m = K.centered("STAFF", S, S, 25, font=K.F35)
        img = T.blend(img, T.solid("#e8e0c8"), m)
    else:
        img = T.rect(img, 22, 2, 26, 5, "#c0b890")
        img = K.grime_bottom(img, "wrcdg", 8, 0.3)
    return T.grain(img, "wrcd" + str(top), 0.03)


def open_door():
    yy, xx = np.mgrid[0:S, 0:S]
    g = np.clip(1 - np.abs(xx - 15.5) / 16, 0, 1)
    img = T.ramp(g * 0.7 + T.noise("wrod", sigma=3) * 0.3, "#f8e8c0", "#fffaf0")
    return T.border(img, "#4a4236", 1, sides="we")


def counter_window():
    rgb = T.ramp(T.noise("wrcw", sigma=3), "#1c2228", "#262e34")
    yy, xx = np.mgrid[0:S, 0:S]
    streak = np.clip(1 - np.abs((xx - yy * 0.6) % 24 - 7) / 2.5, 0, 1)
    rgb = T.blend(rgb, T.solid("#a8b8c0"), streak * 0.35)
    a = 0.4 + streak * 0.3
    # a round speaking hole
    hole = K.disc(S, S, 15.5, 22, 3)
    a = np.where(hole, 0.0, a)
    frame = (xx < 1) | (xx > 30) | (yy < 1) | (yy > 30)
    rgb = T.blend(rgb, T.solid("#6c6a64"), frame.astype(float))
    a = np.where(frame, 1.0, a)
    return rgb, a


def laminate():
    img = T.ramp(T.noise("wrlam", sigma=1.5), "#d8d0bc", "#e2dac6")
    img[:2] = T.hexc("#8a8270")
    return T.grain(img, "wrlam", 0.02)


def plate(n):
    img = T.ramp(T.noise("wrpl", sigma=2), "#2a3a44", "#32444e")
    img = T.blend(img, T.solid("#e8e4d8"), K.centered("COUNTER", S, S, 6, font=K.F35, spacing=0))
    img = T.blend(img, T.solid("#f8f4e8"), K.centered(str(n), S, S, 14, font=K.F57, scale=2 if n < 10 else 1,
                                                      spacing=1))
    return T.border(img, "#9aa0a4", 1)


def red_metal():
    img = T.ramp(T.noise("wrrm", sigma=1.5), "#a82020", "#c02a26")
    return T.grain(T.shade(img, 1 + np.clip(T.noise("wrrms", sigma=3) - 0.6, 0, 1) * 0.5), "wrrm", 0.02)


def dispenser_front():
    img = red_metal()
    img = T.blend(img, T.solid("#f8f0e0"), K.centered("TAKE A", S, S, 3, font=K.F35))
    img = T.blend(img, T.solid("#f8f0e0"), K.centered("NUMBER", S, S, 10, font=K.F35))
    img = T.rect(img, 8, 20, 24, 23, "#1a1a1a")
    img = T.rect(img, 10, 22, 22, 30, "#f4f0e2")
    img = T.blend(img, T.solid("#3a3a3a"), K.centered("047", S, S, 24, font=K.F35) * 0.6)
    return img


def paper():
    return T.grain(T.solid("#f2eee0"), "wrpap", 0.03)


def magazines():
    img = T.ramp(T.noise("wrmag", sigma=1), "#8a5a3a", "#9a6a46")
    r = T.rng("wrmags")
    for k in range(4):
        x, y = int(r.random() * 20), int(r.random() * 20)
        col = ["#c83a3a", "#3a7ac8", "#e8c040", "#e8e4d8"][k]
        img = T.rect(img, x, y, x + 11, y + 13, col)
        img = T.rect(img, x + 1, y + 2, x + 10, y + 4, "#f8f8f0", 0.8)
    return img


def wood_dark():
    return T.grain(T.ramp(T.fbm("wrwd", octaves=3, base=3), "#5a3e26", "#6a4a2e"), "wrwd", 0.04)


def cooler_body():
    img = T.ramp(T.noise("wrcb", sigma=2), "#d8d6cc", "#e6e4da")
    img = T.rect(img, 12, 6, 20, 10, "#3a6ac8")
    img = T.rect(img, 6, 12, 26, 13, "#9a9890")
    return T.grain(img, "wrcb", 0.02)


def cooler_bottle():
    rgb = T.ramp(T.noise("wrcbt", sigma=2), "#a8c8e8", "#c8e0f4")
    return rgb, np.ones((S, S)) * 0.75


def leaves():
    rgb = T.ramp(T.noise("wrlv", sigma=0.7), "#2a5a2a", "#4a7a3a")
    a = (T.noise("wrlva", sigma=0.8) > 0.35).astype(float)
    return rgb, a


def pot():
    img = T.ramp(T.noise("wrpot", sigma=1.5), "#8a4a2a", "#9a5632")
    img[:3] = T.hexc("#6a3a22")
    return T.grain(img, "wrpot", 0.03)


tex("wr_floor", lambda: vinyl("wrf"))
tex("wr_floor_worn", lambda: vinyl("wrfw", worn=True))
tex("wr_wall_upper", wall_upper)
tex("wr_wall_lower", wall_lower)
tex("wr_ceiling", ceiling)
tex("wr_light", lambda: light_panel(True))
tex("wr_light_off", lambda: light_panel(False))
tex("wr_plastic_orange", plastic_orange)
tex("wr_chrome", chrome)
for d in "0123456789":
    tex(f"wr_digit_{d}", (lambda d=d: digit_tex(d)))
tex("wr_digit_blank", lambda: digit_tex(" "))
for p in range(2):
    tex(f"wr_label_now_{p}", (lambda p=p: label_tex("NOW SERVING", p)))
    tex(f"wr_label_counter_{p}", (lambda p=p: label_tex("COUNTER", p)))
POSTERS = [
    (["PLEASE", "TAKE A", "NUMBER"], "#e8e2cc", "#2a3a6a"),
    (["HAVE", "FORMS", "READY"], "#dce8e0", "#2a4a3a"),
    (["THANK", "YOU FOR", "WAITING"], "#e8dcd0", "#6a2a2a"),
    (["STAY IN", "YOUR", "SEAT"], "#e8e4c0", "#3a3a3a"),
    (["DO NOT", "MISS", "YOUR", "TURN"], "#f0d8d0", "#8a1a1a"),
]
for n, (lines, bg, fg) in enumerate(POSTERS):
    tex(f"wr_poster_{n}", poster(lines, bg, fg, f"wrp{n}"))
tex("wr_clock", clock)
tex("wr_entrance", entrance)
tex("wr_entrance_sign", entrance_sign)
tex("wr_counter_door_top", lambda: counter_door(True))
tex("wr_counter_door_bottom", lambda: counter_door(False))
tex("wr_open_door", open_door)
tex("wr_counter_window", counter_window)
tex("wr_laminate", laminate)
for n in range(1, 14):
    tex(f"wr_plate_{n}", (lambda n=n: plate(n)))
tex("wr_red_metal", red_metal)
tex("wr_dispenser_front", dispenser_front)
tex("wr_paper", paper)
tex("wr_magazines", magazines)
tex("wr_wood_dark", wood_dark)
tex("wr_cooler_body", cooler_body)
tex("wr_cooler_bottle", cooler_bottle)
tex("wr_leaves", leaves)
tex("wr_pot", pot)

cube("wr_floor", "wr_floor", display="Vinyl Floor Tile", sound="stone")
cube("wr_floor_worn", "wr_floor_worn", display="Worn Vinyl Floor Tile", sound="stone")
cube("wr_wall_upper", "wr_wall_upper", display="Painted Block Wall", sound="stone")
cube("wr_wall_lower", "wr_wall_lower", display="Wainscot", sound="stone")
cube("wr_ceiling", "wr_ceiling", display="Waiting Room Ceiling", sound="wool", flat=True)
flicker_light("wr_light", "wr_light", "wr_light_off", "wr_ceiling", light=15, flicker=0.0, display="Panel Light")
flicker_light("wr_light_flicker", "wr_light", "wr_light_off", "wr_ceiling", light=15, flicker=0.4,
              display="Flickering Panel Light")
for d in "0123456789":
    cube(f"wr_digit_{d}", f"wr_digit_{d}", display="Display Digit", sound="glass", light=4, emissive=True)
cube("wr_digit_blank", "wr_digit_blank", display="Display Digit", sound="glass")
for p in range(2):
    cube(f"wr_label_now_{p}", f"wr_label_now_{p}", display="Now Serving Display", sound="glass", light=4, emissive=True)
    cube(f"wr_label_counter_{p}", f"wr_label_counter_{p}", display="Counter Display", sound="glass", light=4,
         emissive=True)
for n in range(len(POSTERS)):
    cube(f"wr_poster_{n}", f"wr_poster_{n}", display="Notice", sound="stone")
cube("wr_clock", "wr_clock", display="Wall Clock (3:07)", sound="stone")
cube("wr_entrance", "wr_entrance", display="Locked Entrance", sound="glass")
cube("wr_entrance_sign", "wr_entrance_sign", display="Locked Entrance", sound="glass")
cube("wr_counter_door_top", "wr_counter_door_top", display="Counter Door", sound="wood")
cube("wr_counter_door", "wr_counter_door_bottom", display="Counter Door", sound="wood")
exit_block("wr_counter_door_open", "wr_open_door", "DOOR", display="Open Counter Door", light=12, emissive=True)
from assets import pane
pane("wr_counter_window", "wr_counter_window", display="Counter Window", sound="glass", render="translucent")
for n in range(1, 14):
    cube(f"wr_plate_{n}", f"wr_plate_{n}", display=f"Counter {n}", sound="metal")
cube_faces("wr_counter_desk", top="wr_laminate", bottom="wr_laminate", side="wr_wall_lower", display="Counter Desk")

shaped("wr_chair", [
    (2, 6, 2, 14, 8, 13, "plastic"), (2, 8, 12, 14, 18, 14, "plastic"),
    (2, 0, 3, 3, 6, 4, "chrome"), (13, 0, 3, 14, 6, 4, "chrome"),
    (2, 0, 11, 3, 6, 12, "chrome"), (13, 0, 11, 14, 6, 12, "chrome"),
    (0, 5, 3, 16, 6, 12, "chrome"),
], {"plastic": "wr_plastic_orange", "chrome": "wr_chrome"}, display="Waiting Room Chair", unbreakable=True)
shaped("wr_chair_tipped", [
    (2, 0, 2, 14, 2, 13, "plastic"), (2, 0, 13, 14, 10, 15, "plastic"),
    (2, 2, 4, 3, 8, 5, "chrome"), (13, 2, 4, 14, 8, 5, "chrome"),
], {"plastic": "wr_plastic_orange", "chrome": "wr_chrome"}, display="Tipped Chair", unbreakable=True)
shaped("wr_side_table", [
    (2, 0, 2, 3, 9, 3, "wood"), (13, 0, 2, 14, 9, 3, "wood"), (2, 0, 13, 3, 9, 14, "wood"),
    (13, 0, 13, 14, 9, 14, "wood"), (1, 9, 1, 15, 10, 15, {"all": "wood", "up": "mags"}),
], {"wood": "wr_wood_dark", "mags": "wr_magazines"}, display="Magazine Table", unbreakable=True)
shaped("wr_dispenser_post", [(6.5, 0, 6.5, 9.5, 16, 9.5, "chrome"), (4, 0, 4, 12, 1, 12, "chrome")],
       {"chrome": "wr_chrome"}, display="Dispenser Stand", unbreakable=True)
shaped("wr_ticket_dispenser", [
    (4, 0, 5, 12, 10, 11, {"all": "red", "north": "front"}),
    (6, 3, 4, 10, 4, 5, "paper"),
    (5, 10, 6, 11, 12, 10, "chrome"),
], {"red": "wr_red_metal", "front": "wr_dispenser_front", "paper": "wr_paper", "chrome": "wr_chrome"},
    display="Ticket Dispenser", unbreakable=True)
# the dispenser is a real block class: right-click it for a number
A.BLOCKS[-1]["kind"] = "class"
A.BLOCKS[-1]["class"] = "com.noclip.block.TicketDispenserBlock"
shaped("wr_water_cooler", [
    (3, 0, 3, 13, 14, 13, "body"), (5, 14, 5, 11, 16, 11, "bottle"),
], {"body": "wr_cooler_body", "bottle": "wr_cooler_bottle"}, display="Water Cooler", kind="pickup",
    item="noclip:almond_water*2", render="translucent", sound="metal")
shaped("wr_plant", [
    (4, 0, 4, 12, 7, 12, "pot"), (1, 7, 1, 15, 16, 15, "leaves"),
], {"pot": "wr_pot", "leaves": "wr_leaves"}, display="Potted Ficus", unbreakable=True)

# ------------------------------------------------------------------ sound


def room_tone():
    d = 24.0
    t = Y.t_axis(d)
    hum = Y.hum(d, 60.0, "wrhum", buzz=0.5, harmonics=(2, 4, 6, 8)) * 0.35
    vent = Y.bp(Y.pink(d, "wrvent"), 200, 1600) * 0.18
    room = Y.lp(Y.brown(d, "wrroom"), 200) * 0.2
    # the clock on the wall, which has stopped, still ticks
    tick = np.zeros_like(t)
    for s in range(int(d)):
        n = int(0.03 * Y.SR)
        tt = np.arange(n) / Y.SR
        click = Y.bp(Y.white(0.03, f"wrtk{s % 2}"), 2500 if s % 2 else 1800, 8000)[:n] * np.exp(-tt * 300)
        Y.place(tick, click, s + 0.02, 0.5 if s % 2 else 0.4)
    tick = Y.reverb(tick, 0.8, 0.3, "wrtick")[:len(t)]
    x = Y.lp(hum, 3000) + vent + room + tick * 0.25
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.0009), "stereo"


def cough(i):
    def f():
        out = np.zeros(int(2.5 * Y.SR))
        for k in range(2 + i % 2):
            n = int(0.22 * Y.SR)
            tt = np.arange(n) / Y.SR
            c = Y.bp(Y.white(0.22, f"wrcg{i}{k}"), 300 + i * 80, 2200)[:n] * np.exp(-tt * 14) * (1 - np.exp(-tt * 200))
            c += np.sin(2 * np.pi * (140 + i * 20) * tt) * np.exp(-tt * 20) * 0.4
            Y.place(out, c, 0.1 + k * 0.32, 1 - k * 0.2)
        return Y.reverb(Y.lp(out, 2500), 1.8, 0.5, f"wrcg{i}")
    return f


def rustle():
    d = 2.2
    t = Y.t_axis(d)
    env = np.abs(Y.lp(Y.white(d, "wrrs"), 8)) * 30
    x = Y.bp(Y.white(d, "wrrs2"), 2000, 9000) * np.clip(env, 0, 1) * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(x, 1.2, 0.4, "wrrs"), 0.6)


def chair_scrape():
    d = 1.4
    t = Y.t_axis(d)
    f0 = 300 + 120 * np.sin(np.pi * t / d)
    sq = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * 0.3
    x = Y.bp(sq + Y.white(d, "wrcs") * 0.4, 400, 3000) * np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(x, 1.8, 0.5, "wrcs"), 0.6)


def distant_phone():
    out = np.zeros(int(6 * Y.SR))
    for ring in range(2):
        n = int(1.6 * Y.SR)
        tt = np.arange(n) / Y.SR
        bell = (np.sin(2 * np.pi * 440 * tt) + np.sin(2 * np.pi * 480 * tt)) * (np.sin(2 * np.pi * 20 * tt) > 0)
        Y.place(out, bell * 0.4, 0.2 + ring * 3.0)
    return Y.norm(Y.reverb(Y.bp(out, 300, 2500), 2.5, 0.75, "wrph"), 0.4)


def call():
    c = K.chime([K.hz("D6")], gap=0.4, decay=1.4, seed="wrcall")
    v = K.pa_voice("wrcallv", syllables=11, f0=120, rate=0.15, female=True, band=(400, 3000))
    out = np.zeros(len(c) + len(v))
    Y.place(out, c, 0.0, 0.9)
    Y.place(out, v, 0.7, 0.8)
    return Y.norm(Y.reverb(out, 1.6, 0.4, "wrcallr"), 0.85)


def ticket():
    d = 1.3
    t = Y.t_axis(d)
    whirr = Y.bp(Y.white(d, "wrtw"), 1500, 6000) * (np.sin(2 * np.pi * 90 * t) * 0.3 + 0.7) * (t < 0.8) * 0.5
    motor = np.sin(2 * np.pi * 220 * t) * 0.15 * (t < 0.8)
    tear = Y.bp(Y.white(d, "wrtt"), 2500, 9000) * np.exp(-np.maximum(t - 0.95, 0) * 30) * (t > 0.95) * 1.2
    return Y.norm(whirr + motor + tear, 0.7)


def missed():
    d = 4.0
    t = Y.t_axis(d)
    buzz = np.sign(np.sin(2 * np.pi * 110 * t)) * (t < 0.9) * 0.4
    buzz = Y.lp(buzz, 1200)
    f0 = 55 * np.exp(-t * 0.25)
    groan = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * np.clip(t - 0.6, 0, None) / 3.4) * (t > 0.6)
    stretch = Y.bp(Y.white(d, "wrst"), 60, 400) * np.sin(np.pi * t / d) * 0.6
    return Y.norm(Y.reverb(buzz + groan + stretch, 4.0, 0.6, "wrmiss"), 0.9)


def door_buzz():
    d = 1.2
    t = Y.t_axis(d)
    b = np.sign(np.sin(2 * np.pi * 60 * t)) * (t < 0.7) * 0.5
    clack = np.zeros_like(t)
    Y.place(clack, Y.impact("wrdc", 0.3), 0.7, 0.8)
    return Y.norm(Y.bp(b, 100, 3000) + clack, 0.8)


sound("ambient.waiting_room", [("ambient/waiting_room", room_tone)], stream=True)
sound("ambient.waiting_room.additions",
      [(f"ambient/waiting_room_cough{i}", cough(i)) for i in range(3)] +
      [("ambient/waiting_room_rustle", rustle), ("ambient/waiting_room_scrape", chair_scrape),
       ("ambient/waiting_room_phone", distant_phone)],
      subtitle="Someone else is waiting")
sound("event.waiting_room_call", [("event/waiting_room_call", call)], subtitle="A number is called")
sound("event.waiting_room_ticket", [("event/waiting_room_ticket", ticket)], subtitle="A ticket prints")
sound("event.waiting_room_missed", [("event/waiting_room_missed", missed)], subtitle="The room stretches")
sound("event.waiting_room_door", [("event/waiting_room_door", door_buzz)], subtitle="A door buzzes open")

level("waiting_room", fog="#8a9498", sky="#8a9498", ambient="ambient.waiting_room",
      additions="ambient.waiting_room.additions", additions_chance=0.005, fixed_time=6000, height=48)
