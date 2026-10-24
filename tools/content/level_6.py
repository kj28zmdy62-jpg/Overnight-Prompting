"""Level 6 - Lights Out. No light at all. Only what you carry."""
import numpy as np
from assets import cube, tex, shaped, exit_block, level, sound
import tex as T
import synth as Y

S = 32


def cinder():
    img = T.ramp(T.fbm("cind", octaves=3, base=3), "#3c3b38", "#4a4945")
    yy, xx = np.mgrid[0:S, 0:S]
    off = ((yy // 8) % 2) * 8
    mortar = ((yy % 8) == 0) | (((xx + off) % 16) == 0)
    img = T.shade(img, 1 - mortar * 0.35)
    pits = (T.rng("cp").random((S, S)) > 0.9).astype(float)
    img = T.shade(img, 1 - pits * 0.2)
    return T.grain(T.stains(img, "cinds", "#22211e", count=2, strength=0.4, size=7), "cind", 0.05)


def scratched():
    img = cinder()
    r = T.rng("scr")
    for k in range(4):
        x0 = 8 + k * 5 + int(r.random() * 2)
        for y in range(6, 26):
            x = x0 + int((y - 6) * 0.25)
            img[y, x % S] = T.hexc("#6a675f")
    return img


def rough_floor():
    img = T.ramp(T.fbm("rf", octaves=4, base=4), "#2e2d2a", "#3d3c38")
    return T.grain(T.cracks(img, "rfc", "#1a1a18", 2, 0.6), "rf", 0.06)


def low_ceiling():
    return T.grain(T.ramp(T.fbm("lc", octaves=3, base=4), "#2a2927", "#34332f"), "lc", 0.04)


def exit_sign():
    img = T.solid("#0e2a14")
    # crude pixel EXIT
    g = T.hexc("#7cff9a")
    letters = {
        "E": ["111", "100", "110", "100", "111"],
        "X": ["101", "101", "010", "101", "101"],
        "I": ["111", "010", "010", "010", "111"],
        "T": ["111", "010", "010", "010", "010"],
    }
    x = 3
    for ch in "EXIT":
        for yy, row in enumerate(letters[ch]):
            for xx, c in enumerate(row):
                if c == "1":
                    img[12 + yy * 2:14 + yy * 2, x + xx * 2:x + xx * 2 + 2] = g
        x += 7
    return T.border(img, "#2a2a2a", 1)


def exit_door():
    img = T.ramp(T.noise("ed", sigma=2), "#2c2a26", "#36332e")
    img = T.border(img, "#141210", 2)
    img = T.rect(img, 22, 15, 26, 18, "#8a8478")
    return T.grain(img, "ed", 0.05)


tex("l6_cinder", cinder)
tex("l6_cinder_scratched", scratched)
tex("l6_floor", rough_floor)
tex("l6_ceiling", low_ceiling)
tex("l6_exit_sign", exit_sign)
tex("l6_exit_door", exit_door)

cube("cinder_block", "l6_cinder", display="Cinder Block")
cube("scratched_cinder_block", "l6_cinder_scratched", display="Scratched Cinder Block")
cube("rough_concrete", "l6_floor", display="Rough Concrete")
cube("low_ceiling", "l6_ceiling", display="Low Ceiling", flat=True)
shaped("exit_sign", [(2, 4, 0, 14, 10, 2, {"all": "frame", "south": "sign"})],
       {"sign": "l6_exit_sign", "frame": "l6_ceiling"}, display="Exit Sign", light=7, emissive=True,
       unbreakable=True, collides=False, sound="glass", render="solid")
exit_block("dark_exit_door", "l6_exit_door", "DOOR", trigger="USE", solid=True, display="Unmarked Door", sound="wood")


def breathing():
    d = 16.0
    t = Y.t_axis(d)
    cycle = 4.0
    ph = (t % cycle) / cycle
    inhale = np.clip(np.sin(np.pi * ph / 0.45), 0, None) * (ph < 0.45)
    exhale = np.clip(np.sin(np.pi * (ph - 0.5) / 0.45), 0, None) * (ph >= 0.5) * (ph < 0.95)
    br = Y.bp(Y.white(d, "br"), 300, 1800) * inhale * 0.35 + Y.bp(Y.white(d, "br2"), 200, 1200) * exhale * 0.5
    room = Y.lp(Y.brown(d, "l6room"), 90) * 0.35
    x = Y.loopable(Y.norm(br + room, 0.35), 0.5)
    return Y.stereoize(x, 0.0004), "stereo"


def scrape(i):
    def f():
        d = 2.5
        t = Y.t_axis(d)
        x = Y.bp(Y.white(d, f"scrape{i}"), 900 + i * 300, 4000) * np.sin(np.pi * t / d) ** 2
        x *= 0.6 + 0.4 * np.sin(2 * np.pi * (11 + i * 3) * t)
        return Y.reverb(Y.norm(x, 0.6), 2.5, 0.6, f"scr{i}")
    return f


sound("ambient.level_6", [("ambient/level_6", breathing)], stream=True)
sound("ambient.level_6.additions", [(f"ambient/level_6_scrape{i}", scrape(i)) for i in range(3)],
      subtitle="Something scrapes in the dark")

level("level_6", fog="#000000", sky="#000000", ambient="ambient.level_6", additions="ambient.level_6.additions",
      additions_chance=0.004, fixed_time=18000, height=48)
