"""Dead Arcade - blacklight carpet, rows of dark cabinets that wake up as you walk past, a prize
counter at the back. Stare into a screen long enough and it pays out tickets. It also takes something."""
import numpy as np
import assets as A
from assets import cube, tex, shaped, custom, level, sound
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32
GAMES = {
    "blaster": ("STAR", "BLASTER", "#2a2ac8", "#ff3ad0"),
    "muncher": ("MAZE", "MUNCHER", "#c8a020", "#3af0ff"),
    "fury": ("ROAD", "FURY", "#b02020", "#ffd040"),
    "knockout": ("KNOCK", "OUT", "#2a8a2a", "#ff8a2a"),
    "bricks": ("BRICK", "BREAK", "#6a2ab0", "#7aff6a"),
    "level0": ("LEVEL", "ZERO", "#8a7a3a", "#f0e080"),
}


def carpet(seed="arcp"):
    img = T.ramp(T.noise(seed, sigma=0.5), "#06060c", "#0e0e18")
    r = T.rng(seed + "shapes")
    cols = ["#ff3ad0", "#3af0ff", "#f0f040", "#9a5aff", "#5aff8a"]
    for k in range(14):
        c = T.hexc(cols[k % len(cols)])
        x, y = r.random() * S, r.random() * S
        kind = k % 4
        if kind == 0:  # squiggle
            m = np.zeros((S, S))
            for t in range(8):
                m = np.maximum(m, K.line_mask(S, S, (x + t * 1.5) % S, (y + 2 * np.sin(t)) % S, (x + t * 1.5 + 1.5) % S,
                                              (y + 2 * np.sin(t + 1)) % S, 1.1))
        elif kind == 1:  # ring
            d = np.sqrt((np.mgrid[0:S, 0:S][1] - x) ** 2 + (np.mgrid[0:S, 0:S][0] - y) ** 2)
            m = (np.abs(d - 2.5) < 0.7).astype(float)
        elif kind == 2:  # triangle
            yy, xx = np.mgrid[0:S, 0:S]
            m = ((yy - y >= 0) & (yy - y < 5) & (np.abs(xx - x) <= (yy - y) * 0.7)).astype(float)
        else:  # star
            m = K.disc(S, S, x, y, 0.8).astype(float)
            m = np.maximum(m, K.line_mask(S, S, x - 2, y, x + 2, y, 0.8))
            m = np.maximum(m, K.line_mask(S, S, x, y - 2, x, y + 2, 0.8))
        img = T.blend(img, np.ones((S, S, 3)) * c, m * 0.85)
    return T.grain(img, seed, 0.03)


def wall():
    img = T.ramp(T.fbm("arw", octaves=3, base=5), "#0c0c14", "#14141e")
    img[20:23] = T.hexc("#ff3ad0")
    img[24:25] = T.hexc("#3af0ff")
    return img


def ceiling():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("arc_c", sigma=1), "#08080c", "#101016")
    return T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.4)


def neon(col):
    def f():
        img = np.ones((S, S, 3)) * T.hexc(col)
        yy, xx = np.mgrid[0:S, 0:S]
        core = np.abs(yy - 15.5) < 3
        img = T.blend(img * 0.5, np.ones((S, S, 3)) * np.minimum(T.hexc(col) * 0.5 + 0.55, 1), core.astype(float))
        return img
    return f


def body(game):
    _, _, col, acc = GAMES[game]

    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.ramp(T.noise("arb" + game, sigma=1.5), "#141418", "#1c1c22")
        art = (np.sin(xx / 3.0 + yy / 5.0) > 0.6) | ((xx + yy) % 13 == 0)
        img = T.blend(img, T.solid(col), art * 0.7)
        img = T.blend(img, T.solid(acc), ((xx - yy) % 17 == 0) * 0.8)
        img = T.border(img, "#2a2a30", 1)
        return T.grain(img, "arb" + game, 0.03)
    return f


def control_panel(game):
    _, _, col, acc = GAMES[game]

    def f():
        img = T.ramp(T.noise("arcp" + game, sigma=1.5), "#1a1a20", "#24242a")
        img = T.blend(img, T.solid(col), (T.noise("arcpa" + game, sigma=2) > 0.55) * 0.4)
        img = T.blend(img, T.solid("#e02020"), K.disc(S, S, 8, 16, 3).astype(float))
        img = T.blend(img, T.solid("#101010"), K.disc(S, S, 8, 16, 1.2).astype(float))
        for k, c in enumerate(("#e0e020", "#20c0e0", "#e02080")):
            img = T.blend(img, T.solid(c), K.disc(S, S, 18 + k * 5, 15 + (k % 2) * 3, 1.8).astype(float))
        return img
    return f


def coin_door():
    img = T.ramp(T.noise("arcd", sigma=1.5), "#141418", "#1c1c22")
    img = T.rect(img, 10, 6, 22, 24, "#3a3a40")
    img = T.rect(img, 12, 9, 15, 13, "#e8a020")
    img = T.rect(img, 17, 9, 20, 13, "#e8a020")
    img = T.rect(img, 13, 10, 14, 12, "#1a1a1a")
    img = T.rect(img, 18, 10, 19, 12, "#1a1a1a")
    img = T.blend(img, T.solid("#e8e0c8"), K.centered("25C", S, S, 16, font=K.F35) * 0.8)
    return img


def screen(game, on):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        if not on:
            img = T.ramp(T.noise("arso" + game, sigma=3), "#0a0c0c", "#121616")
            img = T.blend(img, T.solid("#4a5a5a"), np.clip(1 - np.abs((xx - yy) % 30 - 6) / 2.5, 0, 1) * 0.3)
            d = np.sqrt(((xx - 15.5) / 16) ** 2 + ((yy - 15.5) / 16) ** 2)
            return T.border(T.shade(img, 1 - np.clip(d - 0.7, 0, 1)), "#000000", 2)
        img = T.solid("#05050a")
        r = T.rng("arsn" + game)
        if game == "blaster":
            for row in range(3):
                for c in range(5):
                    x0, y0 = 4 + c * 5, 4 + row * 5
                    col = ["#ff3ad0", "#3af0ff", "#7aff6a"][row]
                    img = T.rect(img, x0, y0, x0 + 3, y0 + 2, col)
                    img[y0 + 2, x0] = T.hexc(col)
                    img[y0 + 2, x0 + 2] = T.hexc(col)
            img = T.rect(img, 14, 26, 18, 28, "#7aff6a")
            img = T.rect(img, 15, 25, 17, 26, "#7aff6a")
            img[20:24, 16] = T.hexc("#ffffff")
        elif game == "muncher":
            m = ((xx % 8 == 1) | (yy % 8 == 1)) & ~((xx % 16 > 4) & (xx % 16 < 12) & (yy % 8 == 1) & (yy % 16 < 8))
            img = T.blend(img, T.solid("#2a3aff"), m.astype(float))
            for k in range(10):
                img[5 + (k // 5) * 8, 4 + (k % 5) * 6] = T.hexc("#f0e0c0")
            img = T.blend(img, T.solid("#f0e020"), K.disc(S, S, 12, 21, 2.5).astype(float))
            img = T.blend(img, T.solid("#ff3a3a"), K.disc(S, S, 22, 21, 2.2).astype(float))
        elif game == "fury":
            img = T.vgrad(S, S, "#1a0a3a", "#ff3a7a")
            road = (yy > 14) & (np.abs(xx - 15.5) < (yy - 14) * 0.9)
            img = T.blend(img, T.solid("#202020"), road.astype(float))
            img = T.blend(img, T.solid("#f0f0f0"), (road & (np.abs(xx - 15.5) < 0.7) & (yy % 4 < 2)).astype(float))
            img = T.rect(img, 12, 24, 20, 29, "#e02020")
            img = T.blend(img, T.solid("#ffb030"), K.disc(S, S, 16, 9, 4).astype(float))
        elif game == "knockout":
            img = T.vgrad(S, S, "#2a1a0a", "#5a3a1a")
            img = T.rect(img, 6, 10, 12, 26, "#e0b080")
            img = T.rect(img, 20, 10, 26, 26, "#80c0e0")
            img = T.rect(img, 11, 14, 15, 17, "#e02020")
            img = T.rect(img, 17, 15, 21, 18, "#2040e0")
            img = T.rect(img, 0, 28, 32, 32, "#d0d0d0")
        elif game == "bricks":
            for row in range(5):
                col = ["#ff3a3a", "#ff8a2a", "#f0e020", "#3aff6a", "#3a8aff"][row]
                for c in range(7):
                    if r.random() < 0.85:
                        img = T.rect(img, 2 + c * 4, 3 + row * 3, 5 + c * 4, 5 + row * 3, col)
            img = T.rect(img, 12, 28, 20, 29, "#f0f0f0")
            img[21:23, 17:19] = T.hexc("#f0f0f0")
        else:  # level0: a top-down maze of yellow rooms, and a tiny figure
            img = T.solid("#b8a656")
            m = ((xx % 8 == 0) & (yy % 12 > 3)) | ((yy % 8 == 0) & (xx % 12 > 4))
            img = T.blend(img, T.solid("#6a5a2a"), m.astype(float))
            img[15:17, 15] = T.hexc("#1a1a1a")
            img[13:15, 25] = T.hexc("#e8e8e8")
        img = T.shade(img, 1 - (yy % 2 == 0) * 0.18)
        d = np.sqrt(((xx - 15.5) / 16) ** 2 + ((yy - 15.5) / 16) ** 2)
        img = T.shade(img, 1 - np.clip(d - 0.7, 0, 1) * 1.2)
        return T.border(img, "#000000", 2)
    return f


def marquee(game, on):
    top, bottom, col, acc = GAMES[game]

    def f():
        img = T.ramp(T.noise("arm" + game, sigma=2), col, col)
        img = T.shade(img, 0.55 if not on else 1.0)
        m = np.maximum(K.centered(top, S, S, 6, font=K.F35), K.centered(bottom, S, S, 15, font=K.F35))
        c = T.hexc(acc) * (1.0 if on else 0.35)
        if on:
            img = T.blend(img, np.ones((S, S, 3)) * c, K.glow(m, 1.2) * 0.5)
        img = T.blend(img, np.ones((S, S, 3)) * c, m)
        return T.border(img, "#1a1a1a", 1)
    return f


def felt():
    return T.grain(T.ramp(T.noise("arfe", sigma=1), "#1a5a2a", "#22682e"), "arfe", 0.03)


def hockey():
    img = T.ramp(T.noise("arhk", sigma=2), "#e8ecf0", "#f4f6f8")
    yy, xx = np.mgrid[0:S, 0:S]
    holes = (xx % 3 == 1) & (yy % 3 == 1)
    img = T.shade(img, 1 - holes * 0.15)
    img[:, 0:2] = T.hexc("#d02020")
    img[14:16] = T.hexc("#2040d0")
    return img


def claw_glass():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("arcg", sigma=3), "#c8d8e8", "#e0ecf8")
    a = 0.18 + np.clip(1 - np.abs((xx + yy) % 24 - 6) / 3, 0, 1) * 0.3
    frame = (xx < 1) | (xx > 30) | (yy < 1) | (yy > 30)
    rgb = T.blend(rgb, T.solid("#3a3a40"), frame.astype(float))
    return rgb, np.where(frame, 1.0, a)


def plush():
    img = T.solid("#1a1a1a")
    r = T.rng("arpl")
    cols = ["#e86aa0", "#6ac8e8", "#e8d06a", "#a06ae8", "#6ae88a"]
    for k in range(40):
        x, y = r.random() * S, r.random() * S
        img = T.blend(img, T.solid(cols[int(r.random() * 5)]), K.disc(S, S, x, y, 3).astype(float))
        img = T.blend(img, T.solid("#101010"), (K.disc(S, S, x - 1, y - 1, 0.5) | K.disc(S, S, x + 1, y - 1, 0.5)).astype(float))
    return img


def prize_shelf():
    img = T.ramp(T.noise("arps", sigma=1.5), "#2a1a3a", "#341e48")
    r = T.rng("arpss")
    for row in range(2):
        img[row * 16 + 14:row * 16 + 16] = T.hexc("#d0d0d0")
        x = 1
        while x < 28:
            w = 3 + int(r.random() * 4)
            h = 5 + int(r.random() * 7)
            col = ["#e83a3a", "#3a8ae8", "#e8d03a", "#3ae86a", "#e83ad0", "#f0f0f0"][int(r.random() * 6)]
            img = T.rect(img, x, row * 16 + 14 - h, min(31, x + w), row * 16 + 14, col)
            x += w + 1
    return img


def prize_counter():
    img = T.ramp(T.noise("arpc", sigma=2), "#1a1a24", "#22222e")
    img[:3] = T.hexc("#c8c8d0")
    img = T.blend(img, T.solid("#ff3ad0"), K.centered("PRIZES", S, S, 8, font=K.F35))
    img = T.blend(img, T.solid("#3af0ff"), K.centered("12 TIX", S, S, 17, font=K.F35))
    return img


def bell():
    return T.grain(T.ramp(T.noise("arbl", sigma=1), "#c8a030", "#e8c050"), "arbl", 0.02)


def skee_lane():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("arsk", sigma=1, shape=(S, S)) * 0.4 + (np.sin(yy / 1.5) * 0.25 + 0.3), "#8a5a2a", "#a87038")
    img[:, :2] = T.hexc("#e8e020")
    img[:, -2:] = T.hexc("#e8e020")
    return img


def skee_target():
    img = T.solid("#101018")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    for rr, c in ((14, "#3a3aff"), (10, "#ff3a3a"), (6, "#f0e020"), (3, "#101010")):
        img = T.blend(img, T.solid(c), (d < rr).astype(float))
        img = T.blend(img, T.solid("#101010"), ((d < rr) & (d > rr - 1)).astype(float))
    return img


tex("arc_carpet", carpet)
tex("arc_carpet_b", lambda: carpet("arcp2"))
tex("arc_wall", wall)
tex("arc_ceiling", ceiling)
tex("arc_neon_pink", neon("#ff3ad0"))
tex("arc_neon_cyan", neon("#3af0ff"))
tex("arc_coin_door", coin_door)
for g in GAMES:
    tex(f"arc_body_{g}", body(g))
    tex(f"arc_panel_{g}", control_panel(g))
    tex(f"arc_screen_{g}", screen(g, False))
    tex(f"arc_screen_{g}_on", screen(g, True))
    tex(f"arc_marquee_{g}", marquee(g, False))
    tex(f"arc_marquee_{g}_on", marquee(g, True))
tex("arc_felt", felt)
tex("arc_hockey", hockey)
tex("arc_claw_glass", claw_glass)
tex("arc_plush", plush)
tex("arc_prize_shelf", prize_shelf)
tex("arc_prize_counter", prize_counter)
tex("arc_bell", bell)
tex("arc_skee_lane", skee_lane)
tex("arc_skee_target", skee_target)

cube("arc_carpet", "arc_carpet", display="Blacklight Carpet", sound="wool", emissive=True)
cube("arc_carpet_b", "arc_carpet_b", display="Blacklight Carpet", sound="wool", emissive=True)
cube("arc_wall", "arc_wall", display="Arcade Wall", sound="stone")
cube("arc_ceiling", "arc_ceiling", display="Arcade Ceiling", sound="stone", flat=True)
for c in ("pink", "cyan"):
    shaped(f"arc_neon_{c}", [(0, 13, 7, 16, 16, 9, "n")], {"n": f"arc_neon_{c}"}, display="Neon Tube", light=8,
           emissive=True, unbreakable=True, collides=False, sound="glass", render="solid")

for g in GAMES:
    shaped(f"arc_cab_lower_{g}", [
        (1, 0, 4, 15, 10, 16, {"all": "body", "north": "door"}),
        (1, 10, 1, 15, 13, 16, {"all": "body", "up": "panel"}),
        (1, 13, 6, 15, 16, 16, "body"),
    ], {"body": f"arc_body_{g}", "door": "arc_coin_door", "panel": f"arc_panel_{g}"}, display="Arcade Cabinet",
        unbreakable=True, sound="wood")
    for on in (False, True):
        sfx = "_on" if on else ""
        shaped(f"arc_cab_upper_{g}{sfx}", [
            (1, 0, 6, 15, 11, 16, {"all": "body", "north": "screen"}),
            (1, 11, 3, 15, 16, 16, {"all": "body", "north": "marquee"}),
        ], {"body": f"arc_body_{g}", "screen": f"arc_screen_{g}{sfx}", "marquee": f"arc_marquee_{g}{sfx}"},
            display="Arcade Cabinet", unbreakable=True, sound="wood", light=7 if on else None, emissive=True if on else None)

shaped("arc_hockey", [(0, 8, 0, 16, 12, 16, {"all": "body", "up": "top"}), (2, 0, 2, 14, 8, 14, "body")],
       {"body": "arc_body_blaster", "top": "arc_hockey"}, display="Air Hockey Table", unbreakable=True)
shaped("arc_claw_lower", [(1, 0, 1, 15, 16, 15, {"all": "body", "north": "door"})],
       {"body": "arc_body_muncher", "door": "arc_coin_door"}, display="Claw Machine", unbreakable=True)
shaped("arc_claw_upper", [(2, 0, 2, 14, 5, 14, {"all": "plush"}), (1, 0, 1, 15, 16, 15, "glass")],
       {"plush": "arc_plush", "glass": "arc_claw_glass"}, display="Claw Machine", unbreakable=True,
       render="translucent", light=5, emissive=True)
shaped("arc_skee_lane", [(1, 0, 0, 15, 6, 16, {"all": "body", "up": "lane"})],
       {"body": "arc_body_fury", "lane": "arc_skee_lane"}, display="Skee-Ball Lane", unbreakable=True)
shaped("arc_skee_target", [(1, 0, 6, 15, 16, 16, {"all": "body", "north": "target"})],
       {"body": "arc_body_fury", "target": "arc_skee_target"}, display="Skee-Ball Target", unbreakable=True,
       light=4, emissive=True)
cube("arc_prize_shelf", "arc_prize_shelf", display="Prize Wall", sound="wood")
cube("arc_prize_counter", "arc_prize_counter", display="Prize Counter", sound="wood")
custom("arc_prize_bell", "com.noclip.block.PrizeBellBlock",
       {"parent": "minecraft:block/block", "textures": {"b": "noclip:block/arc_bell", "particle": "noclip:block/arc_bell"},
        "elements": [{"from": [5, 0, 5], "to": [11, 3, 11],
                      "faces": {f: {"texture": "#b", "uv": [0, 0, 6, 3]} for f in ("north", "south", "east", "west", "up", "down")}},
                     {"from": [7.5, 3, 7.5], "to": [8.5, 4, 8.5],
                      "faces": {f: {"texture": "#b", "uv": [0, 0, 1, 1]} for f in ("north", "south", "east", "west", "up")}}]},
       display="Counter Bell (Ring for Prizes)", sound="metal", unbreakable=True, render="cutout")

# ------------------------------------------------------------------ sound


def chip(freq, dur, duty=0.5):
    t = Y.t_axis(dur)
    return np.where((t * freq) % 1 < duty, 1.0, -1.0) * np.minimum(t * 200, 1) * np.exp(-t * 3)


def arcade_tone():
    """Many attract modes at once, from across a carpeted room, slowly going out of tune."""
    d = 24.0
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("artone")
    for m in range(5):
        base = 220 * 2 ** (r.integers(0, 7) / 12) * (1 + (r.random() - 0.5) * 0.02)
        step = 0.12 + r.random() * 0.1
        pattern = [0, 4, 7, 12, 7, 4, 0, -5][: 4 + m]
        t0 = r.random() * 2
        k = 0
        while t0 < d:
            f = base * 2 ** (pattern[k % len(pattern)] / 12)
            K.circular_place(out, chip(f, step * 0.9, 0.25 + 0.25 * (m % 2)), t0, 0.05)
            t0 += step
            k += 1
            if k % 16 == 0:
                t0 += 1.5 + r.random() * 2
    out = Y.lp(out, 1800)
    out = K.loop_reverb(out, 2.5, 0.75, "artone")
    t = Y.t_axis(d)
    hum = Y.lp(Y.hum(d, 60.0, "arhum", buzz=0.3, harmonics=(1, 2, 3)), 600) * 0.12
    x = out + hum + Y.lp(Y.brown(d, "arroom"), 120) * 0.2
    return Y.stereoize(Y.loopable(Y.norm(x, 0.42), 1.0), 0.002), "stereo"


def poweron():
    out = np.zeros(int(1.6 * Y.SR))
    thunk = Y.impact("arpo", 0.3) * 0.6
    Y.place(out, thunk, 0.0)
    whine = Y.t_axis(0.5)
    Y.place(out, np.sin(2 * np.pi * (3000 + 4000 * whine) * whine) * np.exp(-whine * 6) * 0.15, 0.02)
    for k, n in enumerate((523.25, 659.25, 783.99, 1046.5)):
        Y.place(out, chip(n, 0.1, 0.5) * 0.4, 0.4 + k * 0.08)
    return Y.norm(Y.reverb(out, 1.2, 0.3, "arpo"), 0.7)


def tickets():
    out = np.zeros(int(1.5 * Y.SR))
    for k in range(12):
        n = int(0.03 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.03, f"artk{k}"), 1500, 7000)[:n] * np.exp(-tt * 100), 0.05 + k * 0.08, 0.7)
    Y.place(out, chip(880, 0.15) * 0.3, 1.0)
    return Y.norm(out, 0.7)


def fanfare():
    out = np.zeros(int(2.5 * Y.SR))
    for k, n in enumerate((523.25, 659.25, 783.99, 1046.5, 783.99, 1046.5)):
        Y.place(out, chip(n, 0.25 if k < 5 else 0.8, 0.5) * 0.5, k * 0.15)
    return Y.norm(Y.reverb(out, 1.5, 0.3, "arfan"), 0.7)


def denied():
    out = np.zeros(int(1.0 * Y.SR))
    Y.place(out, chip(110, 0.6, 0.5) * 0.6, 0.0)
    return Y.norm(out, 0.6)


def insert_coin():
    out = np.zeros(int(2.0 * Y.SR))
    for k, n in enumerate((987.77, 1318.5)):
        Y.place(out, chip(n, 0.12, 0.5) * 0.5, 0.1 + k * 0.1)
    return Y.norm(Y.reverb(out, 2.5, 0.6, "aric"), 0.4)


def explosion_bleep():
    d = 1.2
    t = Y.t_axis(d)
    x = np.sign(Y.lp(Y.white(d, "arex"), 300)) * np.exp(-t * 4) * 0.4
    x = np.round(x * 4) / 4
    return Y.norm(Y.reverb(x, 2.5, 0.6, "arex"), 0.4)


def tokens():
    out = np.zeros(int(1.5 * Y.SR))
    r = Y.rng("artok")
    for k in range(8):
        n = int(0.06 * Y.SR)
        tt = np.arange(n) / Y.SR
        ping = np.sin(2 * np.pi * (3200 + r.random() * 900) * tt) * np.exp(-tt * 50)
        Y.place(out, ping, 0.05 + k * 0.1 + r.random() * 0.03, 0.5)
    return Y.norm(Y.reverb(out, 2.0, 0.6, "artok"), 0.4)


sound("ambient.arcade", [("ambient/arcade", arcade_tone)], stream=True)
sound("ambient.arcade.additions",
      [("ambient/arcade_coin", insert_coin), ("ambient/arcade_explosion", explosion_bleep),
       ("ambient/arcade_tokens", tokens)],
      subtitle="Machines in attract mode")
sound("event.arcade_poweron", [("event/arcade_poweron", poweron)], subtitle="A cabinet wakes up")
sound("event.arcade_tickets", [("event/arcade_tickets", tickets)], subtitle="Tickets pay out")
sound("event.arcade_fanfare", [("event/arcade_fanfare", fanfare)], subtitle="Prize!")
sound("event.arcade_denied", [("event/arcade_denied", denied)], subtitle="Not enough tickets")

level("arcade", fog="#08060e", sky="#08060e", ambient="ambient.arcade", additions="ambient.arcade.additions",
      additions_chance=0.006, fixed_time=18000, height=48)
