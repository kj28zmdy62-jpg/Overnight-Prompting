"""Mine Shaft - timbered drifts through wet rock, lit by hanging lanterns that go out behind you.
Ore carts on rails, chalk marks, shafts that drop into the dark. Something knocks in the walls."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, ladder, level, sound, custom, BLOCK_MODELS
from content.ind_kit import text, vstreaks, rust, steel, box_model, facing_states
import tex as T
import synth as Y

S = 32


def rock(seed="msr", lo="#4a4238", hi="#5e554a", wet=0.0):
    yy, xx = np.mgrid[0:S, 0:S]
    strata = np.sin((yy + T.noise(seed + "s", sigma=3) * 10) / 3.0) * 0.5 + 0.5
    n = T.fbm(seed, octaves=4, base=4) * 0.6 + strata * 0.25 + T.noise(seed + "g", sigma=0.6) * 0.15
    img = T.ramp(n, "#2e2822", lo, hi)
    cracks = T.cracks(np.zeros((S, S, 3)) + 1, seed + "c", "#000000", 3, 1.0)[..., 0] < 0.5
    img = T.shade(img, 1 - cracks * 0.35)
    if wet:
        sheen = T.noise(seed + "w", sigma=2)
        img = T.shade(img, 0.8 + sheen * 0.35 * wet)
        img = vstreaks(img, seed + "wv", "#1e1a16", 4, 0.35, (10, 30))
    return T.grain(img, seed, 0.05)


def rock_ore(color):
    def f():
        img = rock("msro" + color)
        r = T.rng("msor" + color)
        for _ in range(9):
            x, y = int(r.random() * 30) + 1, int(r.random() * 30) + 1
            img[y, x] = T.hexc(color)
            img[y, x + 1] = T.hexc(color) * 0.7
            img[y + 1, x] = T.hexc(color) * 0.6
        return img
    return f


def chalk(kind):
    def f():
        img = rock("msch" + kind)
        c = T.hexc("#d8d4c8")
        if kind == "arrow":
            for i in range(16):
                img[16, 8 + i] = c
            for d in range(5):
                img[16 - d, 23 - d] = c
                img[16 + d, 23 - d] = c
        elif kind == "x":
            for i in range(14):
                img[9 + i, 9 + i] = c
                img[9 + i, 22 - i] = c
        else:
            img = text(img, "NO", 10, 7, "#d8d4c8")
            img = text(img, "LIGHT", 6, 15, "#d8d4c8")
            img = text(img, "BEHIND", 4, 22, "#d8d4c8")
        return img
    return f


def floor():
    img = T.ramp(T.fbm("msf", octaves=4, base=3), "#2e2820", "#40382c")
    stones = (T.rng("msfs").random((S, S)) > 0.9).astype(float)
    img = T.shade(img, 1 + stones * 0.3)
    return T.grain(T.stains(img, "msfw", "#1a1612", 2, 0.4, 8), "msf", 0.05)


def timber(seed, horizontal=False):
    yy, xx = np.mgrid[0:S, 0:S]
    a = yy if horizontal else xx
    grain = np.sin(a / 1.6 + T.noise(seed, sigma=2) * 6) * 0.5 + 0.5
    img = T.ramp(grain * 0.4 + T.fbm(seed + "f", octaves=3, base=3) * 0.6, "#2e2016", "#4a3422", "#5a4028")
    img = T.cracks(img, seed + "c", "#140e08", 2, 0.6)
    return T.grain(T.stains(img, seed + "s", "#1a120a", 2, 0.4, 7), seed, 0.05)


def timber_end():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp((np.sin(d * 1.3) * 0.5 + 0.5) * 0.5 + T.noise("mste", sigma=1) * 0.5, "#3a2a1a", "#5a4228")
    return T.grain(img, "mste", 0.04)


def lantern_glass(on=True):
    if on:
        return T.ramp(T.noise("mslg", sigma=2), "#ffb050", "#ffe8b0")
    return T.ramp(T.noise("mslgo", sigma=2), "#2a2420", "#3a322a")


def lantern_frame():
    return T.grain(T.ramp(T.noise("mslf", sigma=1), "#1e1c1a", "#34302c"), "mslf", 0.05)


def rails():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    sleeper = ((yy % 8) < 4)
    wood = timber("msslp", True)
    rgb[sleeper] = wood[sleeper]
    a[sleeper] = 1
    for rx in (6, 25):
        rgb[:, rx:rx + 2] = T.hexc("#6a6a66")
        rgb[:, rx] = T.hexc("#9a9a94")
        a[:, rx:rx + 2] = 1
    return rgb, a


def cart_iron():
    img = steel("mscart", "#3a3632", "#4e4842", brushed=False)
    return rust(img, "mscartr", 0.5, "#4a2414", "#6a3418")


def ore_load():
    img = T.ramp(T.noise("msload", sigma=0.8), "#2a2622", "#4a4238")
    r = T.rng("msloadg")
    for _ in range(14):
        x, y = int(r.random() * 31), int(r.random() * 31)
        img[y, x] = T.hexc("#c8a040")
    return img


def board_sign(lines, col="#c8c0a8"):
    def f():
        img = timber("msbs" + lines[0], True)
        img = T.shade(img, 1 - ((np.mgrid[0:S, 0:S][0] % 11) == 0) * 0.4)
        y = 16 - len(lines) * 4
        for ln in lines:
            img = text(img, ln, 16 - (len(ln) * 4 - 1) // 2, y, col)
            y += 8
        return img
    return f


def adit_door():
    img = timber("msdoor")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.5)
    for y in (6, 25):
        img = T.rect(img, 0, y, 32, y + 2, "#2a2622")
    img = text(img, "ADIT", 9, 13, "#c8c0a8")
    return img


def crate_tnt():
    img = timber("mstnt", True)
    img = T.rect(img, 4, 10, 28, 22, "#8a1e14")
    img = text(img, "DANGER", 4, 13, "#e8dcc0")
    return img


def pit():
    return T.ramp(T.noise("mspit", sigma=3), "#000000", "#040302")


tex("ms_rock", lambda: rock())
tex("ms_rock_dark", lambda: rock("msrd", "#3a342c", "#4a4238"))
tex("ms_rock_wet", lambda: rock("msrw", "#3a3630", "#4e4840", wet=1.0))
tex("ms_ore_gold", rock_ore("#d8b048"))
tex("ms_ore_copper", rock_ore("#4a9a7a"))
tex("ms_chalk_arrow", chalk("arrow"))
tex("ms_chalk_x", chalk("x"))
tex("ms_chalk_text", chalk("text"))
tex("ms_floor", floor)
tex("ms_timber", lambda: timber("mst"))
tex("ms_timber_h", lambda: timber("msth", True))
tex("ms_timber_end", timber_end)
tex("ms_lantern_on", lambda: lantern_glass(True))
tex("ms_lantern_off", lambda: lantern_glass(False))
tex("ms_lantern_frame", lantern_frame)
tex("ms_rails", rails)
tex("ms_cart", cart_iron)
tex("ms_ore_load", ore_load)
tex("ms_sign_danger", board_sign(["DANGER", "NO", "ENTRY"], "#d8b048"))
tex("ms_sign_level", board_sign(["LEVEL", "1700"]))
tex("ms_adit_door", adit_door)
tex("ms_tnt", crate_tnt)
tex("ms_pit", pit)

cube("ms_rock", "ms_rock", display="Rock", sound="stone")
cube("ms_rock_dark", "ms_rock_dark", display="Rock", sound="stone")
cube("ms_rock_wet", "ms_rock_wet", display="Wet Rock", sound="stone")
cube("ms_ore_gold", "ms_ore_gold", display="Glinting Rock", sound="stone")
cube("ms_ore_copper", "ms_ore_copper", display="Glinting Rock", sound="stone")
for k in ("arrow", "x", "text"):
    cube(f"ms_chalk_{k}", f"ms_chalk_{k}", display="Chalk Marks", sound="stone")
cube("ms_floor", "ms_floor", display="Rubble Floor", sound="gravel")
cube_faces("ms_post", top="ms_timber_end", bottom="ms_timber_end", side="ms_timber", display="Timber Post",
           sound="wood")
cube_faces("ms_cap", top="ms_timber_h", bottom="ms_timber_h", side="ms_timber_h", display="Timber Cap", sound="wood")
shaped("ms_rails", [(0, 0, 0, 16, 1, 16, "r")], {"r": "ms_rails"}, display="Mine Rails", collides=False,
       unbreakable=True, sound="metal")
shaped("ms_cart", [(2, 2, 0, 14, 11, 16, "c"), (3, 11, 1, 13, 12, 15, "o"), (3, 0, 2, 5, 2, 5, "c"),
                   (11, 0, 2, 13, 2, 5, "c"), (3, 0, 11, 5, 2, 14, "c"), (11, 0, 11, 13, 2, 14, "c")],
       {"c": "ms_cart", "o": "ms_ore_load"}, display="Ore Cart", sound="metal", unbreakable=True)
shaped("ms_tnt", [(2, 0, 3, 14, 8, 13, "t")], {"t": "ms_tnt"}, display="Old Crate", sound="wood", unbreakable=True)
cube("ms_sign_danger", "ms_sign_danger", display="Warning Boards", sound="wood")
cube("ms_sign_level", "ms_sign_level", display="Level Board", sound="wood")
exit_block("ms_adit_door", "ms_adit_door", "DOOR", trigger="USE", solid=True, display="Adit Door", sound="wood")
exit_block("ms_pit", "ms_pit", "FALL", display="Shaft Bottom")
exit_block("ms_sump", "ms_pit", "DESCEND", display="Lower Workings")
ladder("ms_ladder", "ms_timber_h", display="Timber Ladder", unbreakable=True)

# lanterns hang from the cap timbers; the level behaviour puts them out, a hand can relight them
LANTERN = [(5.5, 2, 5.5, 10.5, 9, 10.5, {"all": "g", "up": "f", "down": "f"}), (5, 1, 5, 11, 2, 11, "f"),
           (5, 9, 5, 11, 10, 11, "f"), (7.5, 10, 7.5, 8.5, 16, 8.5, "f"), (5, 2, 5, 6, 9, 6, "f"),
           (10, 2, 10, 11, 9, 11, "f")]
BLOCK_MODELS["ms_lantern_lit"] = box_model(LANTERN, {"g": "ms_lantern_on", "f": "ms_lantern_frame"})
BLOCK_MODELS["ms_lantern_out"] = box_model(LANTERN, {"g": "ms_lantern_off", "f": "ms_lantern_frame"})
custom("ms_lantern", "com.noclip.block.MineLanternBlock", BLOCK_MODELS["ms_lantern_lit"],
       blockstate={"variants": {"lit=true": {"model": "noclip:block/ms_lantern_lit"},
                                "lit=false": {"model": "noclip:block/ms_lantern_out"}}},
       display="Miner's Lantern", sound="lantern", unbreakable=True, render="cutout")

# ------------------------------------------------------------------ sound


def deep():
    d = 28.0
    t = Y.t_axis(d)
    rumble = Y.lp(Y.brown(d, "msrum"), 55) * 1.0
    air = Y.bp(Y.pink(d, "msair"), 150, 600) * 0.12 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 14))
    out = np.zeros(len(t))
    r = Y.rng("msdrips")
    for k in range(12):
        n = int(0.14 * Y.SR)
        tt = np.arange(n) / Y.SR
        fr = 700 + r.random() * 700 - tt * 1600
        Y.place(out, np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 28), 1 + r.random() * (d - 2), 0.12 + r.random() * 0.15)
    x = rumble + air + Y.reverb(out, 3.0, 0.8, "msdr")[:len(t)]
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 2.0), 0.002), "stereo"


def knockers(i):
    """Tommyknockers: pick-taps inside the rock, in a pattern."""
    def f():
        out = np.zeros(int(4.0 * Y.SR))
        pattern = [(0, 0.2), (1, 0.45), (2, 0.6), (3, 1.3), (4, 1.45)] if i == 0 else \
            [(k, 0.15 + k * 0.32) for k in range(7)]
        for k, at in pattern:
            Y.place(out, Y.impact(f"mskn{i}{k}", 0.25, ring=0.15), at, 0.7)
        return Y.norm(Y.reverb(Y.lp(out, 1100), 2.5, 0.65, f"mskn{i}"), 0.6)
    return f


def timber_creak():
    d = 3.0
    t = Y.t_axis(d)
    f0 = 95 + 20 * np.sin(2 * np.pi * 0.6 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (Y.rng("mstc").random(len(t)) > 0.55) * np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(Y.bp(x, 80, 1500), 2.5, 0.6, "mstc"), 0.6)


def far_cart():
    d = 5.0
    t = Y.t_axis(d)
    env = np.exp(-((t - 2.5) ** 2) / 2.0)
    clack = np.zeros(len(t))
    for k in range(18):
        Y.place(clack, Y.impact(f"msck{k}", 0.2, ring=0.4), 0.3 + k * 0.24, 0.5)
    x = (Y.lp(Y.brown(d, "msfc"), 200) * 1.2 + Y.lp(clack, 1500)) * env
    return Y.norm(Y.reverb(x, 3.5, 0.75, "msfc"), 0.55)


def puff():
    d = 1.2
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mspuff"), 400, 3000) * np.exp(-t * 7) * (1 - np.exp(-t * 80))
    clink = np.zeros(len(t))
    Y.place(clink, Y.impact("mspcl", 0.1, ring=0.6), 0.02, 0.3)
    return Y.norm(x + clink, 0.6)


def strike():
    d = 1.5
    t = Y.t_axis(d)
    scratch = Y.bp(Y.white(d, "msstr"), 1500, 8000) * np.exp(-t * 18)
    flare = Y.bp(Y.white(d, "msfl"), 200, 1500) * np.exp(-((t - 0.15) ** 2) / 0.02) * 0.6
    burn = Y.lp(Y.white(d, "msburn"), 600) * 0.15 * np.clip(t / 0.3, 0, 1) * np.exp(-t * 2)
    return Y.norm(scratch + flare + burn, 0.7)


sound("ambient.mine_shaft", [("ambient/mine_shaft", deep)], stream=True)
sound("ambient.mine_shaft.additions",
      [(f"ambient/mine_shaft_knock{i}", knockers(i)) for i in range(2)] +
      [("ambient/mine_shaft_creak", timber_creak), ("ambient/mine_shaft_cart", far_cart)],
      subtitle="Something knocks in the rock")
sound("event.mine_shaft_knock", [(f"event/mine_shaft_knock{i}", knockers(i)) for i in range(2)],
      subtitle="Tapping in the rock")
sound("event.mine_shaft_out", [("event/mine_shaft_out", puff)], subtitle="A lantern goes out")
sound("event.mine_shaft_strike", [("event/mine_shaft_strike", strike)], subtitle="A match flares")

level("mine_shaft", fog="#0c0a08", sky="#0c0a08", ambient="ambient.mine_shaft", additions="ambient.mine_shaft.additions",
      additions_chance=0.005, fixed_time=18000, height=96, particle="minecraft:white_ash", particle_chance=0.002)
