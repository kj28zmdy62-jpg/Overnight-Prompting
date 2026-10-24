"""Shared blocks, items and sounds used across many levels."""
import numpy as np
from assets import cube, tex, item_tex, sound, shaped, exit_block, lang
import tex as T
import synth as Y

# ------------------------------------------------------------------ textures

tex("void", lambda: T.grain(T.solid("#030303"), "void", 0.015))
tex("subfloor", lambda: T.grain(T.shade(T.ramp(T.fbm("subfloor"), "#2b2a27", "#3a3833"), 1.0), "subfloor", 0.05))
tex("pitch_black", lambda: T.solid("#000000"))


def _dark_doorway():
    img = T.solid("#020202")
    n = T.noise("doorway", sigma=3)
    img = T.blend(img, T.solid("#0b0b0e"), n * 0.6)
    return T.grain(img, "doorway", 0.02)


tex("dark_doorway", _dark_doorway)

cube("void_filler", "void", display="Nothing")
cube("subfloor", "subfloor", display="Subfloor")
exit_block("dark_doorway", "dark_doorway", "DOOR", display="Dark Doorway")
exit_block("bottomless_pit", "pitch_black", "FALL", display="Bottomless Dark")

# ------------------------------------------------------------------ items (16x16 pixel art)


def sprite(rows, palette):
    h, w = len(rows), len(rows[0])
    rgb = np.zeros((h, w, 3))
    a = np.zeros((h, w))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch in palette:
                rgb[y, x] = T.hexc(palette[ch])
                a[y, x] = 1
    return rgb, a


item_tex("almond_water", lambda: sprite([
    "......cc........",
    "......kk........",
    ".....gkkg.......",
    ".....gwwg.......",
    "....gwwwwg......",
    "....gwwwwg......",
    "....gLLLLg......",
    "....gLlLLg......",
    "....gLLLLg......",
    "....gwwwwg......",
    "....gwwwwg......",
    "....gwwwhg......",
    "....gwwwhg......",
    "....gwwwwg......",
    ".....gggg.......",
    "................"], {"c": "#d9d2c0", "k": "#9c9488", "g": "#c8d4d8", "w": "#e8e3cf", "h": "#ffffff",
                         "L": "#c9a46a", "l": "#7a5a2e"}))

item_tex("flashlight", lambda: sprite([
    "................",
    "................",
    "...........yy...",
    "..........yYYy..",
    ".........mYYYYm.",
    "........mmYYYm..",
    ".......bbmmmm...",
    "......bbbbm.....",
    ".....bbsbb......",
    "....bbbbb.......",
    "...bbbbb........",
    "..bbbbb.........",
    "..dbbb..........",
    "..ddb...........",
    "................",
    "................"], {"y": "#fff3b0", "Y": "#fffbe0", "m": "#9a9a9a", "b": "#2c2c30", "s": "#c03030",
                         "d": "#18181a"}))

item_tex("battery", lambda: sprite([
    "................",
    "................",
    ".......nn.......",
    "......kkkk......",
    "......cccc......",
    "......cccc......",
    "......cBBc......",
    "......cBBc......",
    "......cccc......",
    "......cccc......",
    "......oooo......",
    "......oooo......",
    "......oooo......",
    "......kkkk......",
    "................",
    "................"], {"n": "#c8c8c8", "k": "#777777", "c": "#2a2a2a", "B": "#d8b030", "o": "#c86a20"}))

item_tex("journal", lambda: sprite([
    "................",
    "...bbbbbbbbbb...",
    "..bBBBBBBBBBbp..",
    "..bBBBBBBBBBbp..",
    "..bBBwwwwwBBbp..",
    "..bBBwkkkwBBbp..",
    "..bBBwwwwwBBbp..",
    "..bBBBBBBBBBbp..",
    "..bBBBBBBBBBbp..",
    "..bBBBBBBBBBbp..",
    "..bBBBBBBBBBbp..",
    "..bBBBBBBBBBbp..",
    "..bBBBBBBBBBbp..",
    "..bbbbbbbbbbbp..",
    "...ppppppppppp..",
    "................"], {"b": "#3a2a1a", "B": "#6b4a2a", "w": "#e8dcc0", "k": "#333333", "p": "#d8ccb0"}))

item_tex("canned_beans", lambda: sprite([
    "................",
    "................",
    "....mmmmmmmm....",
    "...mMMMMMMMMm...",
    "...rrrrrrrrrr...",
    "...rRRRRRRRRr...",
    "...rRwwwwwwRr...",
    "...rRwkbbkwRr...",
    "...rRwwwwwwRr...",
    "...rRRRRRRRRr...",
    "...rrrrrrrrrr...",
    "...mMMMMMMMMm...",
    "....mmmmmmmm....",
    "................",
    "................",
    "................"], {"m": "#8a8a8a", "M": "#bdbdbd", "r": "#8a2a1a", "R": "#b8432a", "w": "#efe6cc",
                         "k": "#6a3a1a", "b": "#a0522d"}))

item_tex("energy_bar", lambda: sprite([
    "................",
    "................",
    "................",
    "................",
    "..s...........s.",
    ".ssbbbbbbbbbbss.",
    ".sbYYYYYYYYYYbs.",
    ".sbYkkYYkYkYYbs.",
    ".sbYYYYYYYYYYbs.",
    ".ssbbbbbbbbbbss.",
    "..s...........s.",
    "................",
    "................",
    "................",
    "................",
    "................"], {"s": "#9a9a9a", "b": "#1a3a8a", "Y": "#e8c020", "k": "#1a1a1a"}))

lang("item.noclip.almond_water", "Almond Water")
lang("item.noclip.flashlight", "Flashlight")
lang("item.noclip.flashlight.battery", "Battery: %s:%s")
lang("item.noclip.flashlight.hint", "Sneak + use with a battery in your inventory to recharge")
lang("item.noclip.battery", "Battery")
lang("item.noclip.journal", "Wanderer's Journal")
lang("item.noclip.canned_beans", "Canned Beans")
lang("item.noclip.energy_bar", "Energy Bar")
lang("itemGroup.noclip", "NOCLIP: The Backrooms")
lang("death.attack.noclip.insanity", "%1$s lost their mind")
lang("death.attack.noclip.insanity.player", "%1$s lost their mind while fleeing %2$s")

# ------------------------------------------------------------------ pickups placed by generators


def _bottle_glass():
    img = T.ramp(T.noise("bottle", sigma=2), "#d6e2e4", "#f4f2e6")
    return img, np.ones((32, 32)) * 0.85


def _bottle_label():
    img = T.ramp(T.noise("label", sigma=1), "#c49a5c", "#d8b276")
    img = T.rect(img, 4, 12, 28, 20, "#6b4a22")
    return img


tex("almond_bottle_glass", _bottle_glass)
tex("almond_bottle_label", _bottle_label)
tex("almond_bottle_cap", lambda: T.grain(T.solid("#cfc9b8"), "cap", 0.05))

shaped("almond_water_bottle", [
    (6, 0, 6, 10, 6, 10, {"all": "glass", "down": "glass", "up": "glass"}),
    (6, 2, 6, 10, 5, 10, "label"),
    (7, 6, 7, 9, 8, 9, "glass"),
    (7, 8, 7, 9, 9, 9, "cap"),
], {"glass": "almond_bottle_glass", "label": "almond_bottle_label", "cap": "almond_bottle_cap"},
    display="Almond Water", kind="pickup", item="noclip:almond_water", collides=False, sound="glass",
    render="translucent", strength=0.1)

# ------------------------------------------------------------------ sounds


def _noclip_whoosh():
    d = 2.2
    n = int(d * Y.SR)
    w = Y.white(d, "noclip")
    sweep = np.linspace(0, 1, n)
    x = Y.bp(w, 200, 9000) * (sweep ** 2) * np.exp(-((sweep - 0.55) ** 2) / 0.02)
    # bit-crushed stutter in the middle
    crush = np.round(x * 6) / 6
    x = x * 0.5 + crush * 0.5
    sub = np.sin(2 * np.pi * np.cumsum(np.linspace(90, 30, n)) / Y.SR) * np.exp(-np.maximum(sweep - 0.5, 0) * 6) * (sweep > 0.5)
    x = x + sub * 0.9
    x = Y.reverb(x, 2.0, 0.4, "noclip")
    return Y.norm(x, 0.85), "stereo"


def _click(seed, pitch=1.0):
    n = int(0.08 * Y.SR)
    w = Y.white(0.08, seed)
    x = Y.bp(w, 1500 * pitch, 8000) * Y.decay(n, 0.004) + Y.lp(w, 400) * Y.decay(n, 0.01) * 0.5
    return Y.norm(x, 0.7)


def _battery():
    a = _click("bat1", 0.7)
    b = _click("bat2", 1.2)
    out = np.zeros(int(0.4 * Y.SR))
    Y.place(out, a, 0.0)
    Y.place(out, b, 0.22)
    return out


sound("event.noclip", [("event/noclip", _noclip_whoosh)], subtitle="Reality gives way")
sound("item.flashlight_click", [("item/flashlight_click", lambda: _click("fl"))], subtitle="Flashlight clicks")
sound("item.battery", [("item/battery", _battery)], subtitle="Battery clicks in")


def _whisper(i):
    def f():
        x = Y.whisper(f"wh{i}", 1.4 + 0.3 * i)
        return Y.reverb(x, 1.5, 0.5, f"wh{i}")
    return f


def _steps_behind(i):
    def f():
        out = np.zeros(int(3.2 * Y.SR))
        for k in range(5):
            Y.place(out, Y.footstep(f"sb{i}{k}", "carpet"), 0.15 + k * (0.52 - i * 0.03), 0.6 + k * 0.08)
        return Y.reverb(out, 1.0, 0.3, f"sb{i}")
    return f


def _knock(i):
    def f():
        out = np.zeros(int(2.5 * Y.SR))
        for k in range(3 + i % 2):
            Y.place(out, Y.impact(f"kn{i}{k}", 0.25), 0.1 + k * 0.28)
        return Y.reverb(out, 1.8, 0.45, f"kn{i}")
    return f


sound("halluc.whisper", [(f"halluc/whisper{i}", _whisper(i)) for i in range(4)], subtitle="Something whispers")
sound("halluc.footsteps", [(f"halluc/steps{i}", _steps_behind(i)) for i in range(3)], subtitle="Footsteps behind you")
sound("halluc.knock", [(f"halluc/knock{i}", _knock(i)) for i in range(3)], subtitle="Knocking")


def _sting():
    d = 3.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for f in (233.0, 246.9, 261.6, 277.2):
        x += np.sin(2 * np.pi * f * t + np.sin(2 * np.pi * 5 * t) * 0.6)
    x *= np.exp(-t * 1.2) * (1 - np.exp(-t * 40))
    x += Y.hp(Y.white(d, "sting"), 3000) * np.exp(-t * 3) * 0.3
    return Y.reverb(Y.norm(x), 2.5, 0.5, "sting"), "stereo"


sound("event.sting", [("event/sting", _sting)], subtitle="Your heart lurches")


def _heartbeat():
    out = np.zeros(int(0.9 * Y.SR))
    for at, g in ((0.0, 1.0), (0.22, 0.7)):
        n = int(0.25 * Y.SR)
        t = np.arange(n) / Y.SR
        thump = np.sin(2 * np.pi * (55 - 25 * t / 0.25) * t) * np.exp(-t * 22)
        Y.place(out, thump, at, g)
    return Y.norm(Y.lp(out, 200), 0.95)


def _wall_thin():
    d = 2.5
    t = Y.t_axis(d)
    x = Y.drone(d, [41.2, 82.4], "thin", lfo=0) * np.sin(np.pi * t / d) ** 2
    x += Y.bp(Y.white(d, "thin2"), 2000, 6000) * 0.05 * np.sin(np.pi * t / d)
    return Y.norm(x, 0.7)


sound("event.heartbeat", [("event/heartbeat", _heartbeat)], subtitle="Your heartbeat")
sound("event.wall_thin", [("event/wall_thin", _wall_thin)], subtitle="The wall feels thin")
