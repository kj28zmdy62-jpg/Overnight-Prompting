"""The Stairwell. One concrete fire stair, forever. Going up takes you down; the numbers painted
on the walls go wrong, loop, and stop being numbers."""
import numpy as np
from assets import cube, tex, flicker_light, shaped, stairs, custom, level, sound, BLOCK_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import glyph_mask, centered, text_mask, ink, brushed

S = 32

GREEN = ("#4f6a58", "#5c7965")
WHITE = ("#c9c6b6", "#d6d3c4")


def paint(seed, lo, hi, grime=0.0):
    img = T.ramp(T.fbm(seed, octaves=3, base=5) * 0.6 + T.noise(seed + "n", sigma=0.6) * 0.4, lo, hi)
    # roller texture and pinholes in the block wall under the paint
    yy, xx = np.mgrid[0:S, 0:S]
    off = ((yy // 16) % 2) * 16
    mortar = ((yy % 16) == 0) | (((xx + off) % 32) == 0)
    img = T.shade(img, 1 - mortar * 0.09)
    pits = (T.rng(seed + "p").random((S, S)) > 0.95).astype(float)
    img = T.shade(img, 1 - pits * 0.07)
    if grime:
        g = np.clip((yy - 10) / 22.0, 0, 1) * T.noise(seed + "g", sigma=2)
        img = T.shade(img, 1 - g * grime)
    return T.grain(img, seed, 0.03)


def wall_white():
    return T.stains(paint("swh", *WHITE), "swhs", "#9a9480", count=1, strength=0.18, size=9)


def wall_green():
    img = paint("sgr", *GREEN, grime=0.15)
    # scuffs at shoulder height, where people lean on the turn
    r = T.rng("scuff")
    for _ in range(5):
        x, y = int(r.random() * 28), int(r.random() * 24)
        ln = 3 + int(r.random() * 6)
        img[y, x:x + ln] = img[y, x:x + ln] * 0.8
    return img


def wall_band():
    """Green below with the dark dado stripe at the top edge, white above."""
    img = paint("sbd", *GREEN, grime=0.05)
    top = paint("sbdw", *WHITE)
    img[:10] = top[:10]
    img[10:13] = T.grain(T.solid("#2c3a30", shape=(3, S)), "sbds", 0.05)
    return img


def wall_base():
    img = paint("sbs", *GREEN, grime=0.35)
    img[24:] = T.grain(T.ramp(T.noise("sbsk", sigma=1, shape=(8, S)), "#232a25", "#2e3630"), "sbsk", 0.05)
    img[24] = T.hexc("#3a463e")
    return img


def steps():
    img = T.ramp(T.fbm("sst", octaves=4, base=4) * 0.7 + T.noise("sstn", sigma=0.5) * 0.3, "#6a6964", "#7d7c76")
    yy, xx = np.mgrid[0:S, 0:S]
    worn = np.exp(-((xx - 16) ** 2) / 60.0) * 0.08
    img = T.shade(img, 1 + worn)
    img = T.stains(img, "ssts", "#4a4944", count=2, strength=0.25, size=6)
    return T.grain(T.cracks(img, "sstc", "#4a4a46", 1, 0.4), "sst", 0.04)


def landing():
    img = T.ramp(T.fbm("sla", octaves=4, base=5), "#6e6d67", "#807f78")
    yy, xx = np.mgrid[0:S, 0:S]
    # a yellow anti-slip nosing strip along one edge, worn through in the middle
    strip = (yy < 3)
    worn = T.noise("slaw", sigma=1) > 0.35
    img = T.blend(img, T.solid("#b8952a"), (strip & worn).astype(float) * 0.85)
    img = T.stains(img, "slas", "#55544e", count=2, strength=0.3, size=7)
    return T.grain(img, "sla", 0.04)


def underside():
    img = T.ramp(T.fbm("sun", octaves=3, base=4), "#4a4945", "#57564f")
    return T.grain(T.stains(img, "suns", "#2e2d29", count=2, strength=0.4, size=7), "sun", 0.05)


def rail_paint():
    img = T.ramp(T.noise("srp", sigma=1.2), "#7a2a22", "#93362c")
    chips = T.noise("srpc", sigma=0.7) > 0.8
    img = T.blend(img, T.solid("#4a4a48"), chips.astype(float) * 0.8)
    return T.grain(img, "srp", 0.05)


def lamp(on=True):
    img = paint("slp", *WHITE)
    yy, xx = np.mgrid[0:S, 0:S]
    cx, cy = 15.5, 15.5
    d = np.sqrt((xx - cx) ** 2 + ((yy - cy) * 0.8) ** 2)
    base = (d < 11)
    img = T.blend(img, T.solid("#3a3c38"), base.astype(float))
    glass = d < 8.5
    col = T.ramp(np.clip(1 - d / 9, 0, 1), "#e8d9a8", "#fff8e8") if on else T.ramp(np.clip(1 - d / 9, 0, 1), "#5a5850", "#77746a")
    img = T.blend(img, col, glass.astype(float))
    cage = glass & ((np.abs(xx - cx) < 0.6) | (np.abs(yy - cy) < 0.6) | (np.abs(d - 5) < 0.5))
    img = T.blend(img, T.solid("#2a2a28"), cage.astype(float))
    if on:
        halo = np.clip(1 - d / 15, 0, 1) * (d >= 11)
        img = T.blend(img, T.solid("#f4ecd0"), halo * 0.35)
    return img


def door(open_=False, top=False):
    img = T.ramp(T.noise("sdo" + str(top), sigma=2), "#7e2a22", "#8e342a")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.border(img, "#4a1a14", 1)
    img[:, 0:2] = T.hexc("#3a3c3a")  # hinge-side frame
    if top:
        # wired-glass vision panel
        win = (xx >= 9) & (xx <= 22) & (yy >= 6) & (yy <= 26)
        gl = T.ramp(T.noise("sdog", sigma=2), "#1a1e1c", "#2a302c") if not open_ else T.ramp(T.noise("sdog", sigma=2), "#c8b888", "#f0e4b8")
        img = T.blend(img, gl, win.astype(float))
        mesh = win & ((((xx - 9) + (yy - 6)) % 5 == 0) | (((xx - 9) - (yy - 6)) % 5 == 0))
        img = T.blend(img, T.solid("#6a6a62"), mesh.astype(float) * 0.6)
        img = T.blend(img, T.solid("#2a2a28"), (win & ((xx == 9) | (xx == 22) | (yy == 6) | (yy == 26))).astype(float))
    else:
        # push bar
        bar = (yy >= 2) & (yy <= 5) & (xx >= 4) & (xx <= 29)
        img = T.blend(img, brushed("sdob", "#8a8a84", "#c0c0b8", vertical=False), bar.astype(float))
        img = T.blend(img, T.solid("#2a2a28"), ((yy == 6) & (xx >= 4) & (xx <= 29)).astype(float) * 0.6)
        kick = yy >= 26
        img = T.blend(img, brushed("sdok", "#6a6a64", "#8a8a84", vertical=False), kick.astype(float))
    if open_:
        # light leaking around the edges of a door that is not quite shut
        gap = (xx >= 30)
        img = T.blend(img, T.solid("#fff2c8"), gap.astype(float))
    img = T.stains(img, "sdos" + str(top), "#4a1a14", count=1, strength=0.2, size=6)
    return T.grain(img, "sdo" + str(top), 0.04)


EYE = np.array([
    [0, 0, 1, 1, 1, 0, 0],
    [0, 1, 0, 0, 0, 1, 0],
    [1, 0, 0, 1, 0, 0, 1],
    [1, 0, 1, 1, 1, 0, 1],
    [1, 0, 0, 1, 0, 0, 1],
    [0, 1, 0, 0, 0, 1, 0],
    [0, 0, 1, 1, 1, 0, 0]], dtype=float)


def digit(g):
    """A stencilled glyph on white paint. g: 0-9, 10 blank, 11 ?, 12 -, 13 B, 14 an eye, 15 painted-out smear."""
    img = paint("sdg", *WHITE)
    if g == 10:
        return img
    m = np.zeros((S, S))
    if g <= 9 or g in (11, 12, 13):
        ch = str(g) if g <= 9 else {11: "?", 12: "-", 13: "B"}[g]
        gm = np.kron(glyph_mask(ch), np.ones((4, 4)))  # 20 x 28
        # stencil bridges: break the strokes with two thin horizontal gaps
        gm[9:10] = 0
        gm[19:20] = 0
        m[2:30, 6:26] = gm
    elif g == 14:
        e = np.kron(EYE, np.ones((4, 4)))
        m[2:30, 2:30] = e
    elif g == 15:
        r = T.rng("smear")
        for k in range(9):
            y = 6 + k * 2 + int(r.random() * 2)
            x0 = 4 + int(r.random() * 5)
            m[y:y + 2, x0:x0 + 18 + int(r.random() * 6)] = 1
    col = "#141414" if g != 15 else "#3a1210"
    img = ink(img, m, col, 0.92, wear_seed=f"sdgw{g}", wear=0.12)
    # drips under fresh paint for the wrong ones
    if g in (11, 14, 15):
        r = T.rng(f"drip{g}")
        for _ in range(4):
            x = 6 + int(r.random() * 20)
            ys = np.where(m[:, x] > 0)[0]
            if len(ys):
                y0 = ys.max()
                for y in range(y0, min(S, y0 + 2 + int(r.random() * 8))):
                    img[y, x] = T.hexc(col)
    return img


def graffiti(lines, color, seed):
    def f():
        img = paint(seed, *WHITE)
        m = np.zeros((S, S))
        y = 4 if len(lines) > 1 else 12
        for ln in lines:
            m = np.maximum(m, centered(ln, y=y, scale=1, spacing=1))
            y += 11
        # hand-drawn: the second line slumps a pixel to the right
        m[16:] = np.roll(m[16:], 1, axis=1)
        return ink(img, m, color, 0.85, wear_seed=seed + "w", wear=0.15)
    return f


def hose(top=False):
    img = T.ramp(T.noise("sho" + str(top), sigma=1.5), "#8a1a16", "#a2241e")
    img = T.border(img, "#5a100c", 2)
    yy, xx = np.mgrid[0:S, 0:S]
    if top:
        win = (xx >= 5) & (xx <= 26) & (yy >= 8) & (yy <= 28)
        img = T.blend(img, T.solid("#1c1c1a"), win.astype(float))
        d = np.sqrt((xx - 15.5) ** 2 + (yy - 18) ** 2)
        coil = win & (np.abs((d % 3) - 1.5) < 0.7) & (d < 9)
        img = T.blend(img, T.solid("#b8a070"), coil.astype(float))
        img = ink(img, centered("FIRE", y=1, scale=1), "#f0e8d8")
    else:
        img = ink(img, centered("HOSE", y=12, scale=1), "#f0e8d8")
    return T.grain(img, "sho" + str(top), 0.04)


def chair_metal():
    return T.grain(T.ramp(T.noise("sch", sigma=1), "#4a4c50", "#62656a"), "sch", 0.04)


def paper():
    rgb = T.ramp(T.noise("spa", sigma=1), "#c8c2b0", "#e0dccc")
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.zeros((S, S))
    r = T.rng("spap")
    for _ in range(5):
        x0, y0 = int(r.random() * 22), int(r.random() * 22)
        w, h = 6 + int(r.random() * 6), 8 + int(r.random() * 4)
        a[y0:y0 + h, x0:x0 + w] = 1
        rgb[y0 + 2:y0 + h - 1:2, x0 + 1:x0 + w - 1] *= 0.75
    return rgb, a


tex("stair_wall_white", wall_white)
tex("stair_wall_green", wall_green)
tex("stair_wall_band", wall_band)
tex("stair_wall_base", wall_base)
tex("stair_steps", steps)
tex("stair_landing", landing)
tex("stair_underside", underside)
tex("stair_rail", rail_paint)
tex("stair_lamp", lambda: lamp(True))
tex("stair_lamp_off", lambda: lamp(False))
tex("stair_door", lambda: door(False, False))
tex("stair_door_top", lambda: door(False, True))
tex("stair_door_open", lambda: door(True, False))
tex("stair_door_top_open", lambda: door(True, True))
for g in range(16):
    tex(f"stair_digit_{g}", (lambda g: (lambda: digit(g)))(g))
tex("stair_graffiti_out", graffiti(["1 IS", "OUT"], "#1a1a6a", "sgo"))
tex("stair_graffiti_up", graffiti(["UP IS", "DOWN"], "#5a1010", "sgu"))
tex("stair_graffiti_count", graffiti(["DONT", "COUNT"], "#1a1a1a", "sgc"))
tex("stair_graffiti_arrow", graffiti(["^", "v ^"], "#1a1a1a", "sga"))
tex("stair_hose", lambda: hose(False))
tex("stair_hose_top", lambda: hose(True))
tex("stair_chair", chair_metal)
tex("stair_paper", paper)

cube("stair_wall_white", "stair_wall_white", display="Painted Block Wall")
cube("stair_wall_green", "stair_wall_green", display="Green Painted Block Wall")
cube("stair_wall_band", "stair_wall_band", display="Dado-Striped Block Wall")
cube("stair_wall_base", "stair_wall_base", display="Scuffed Block Wall")
cube("stair_landing", "stair_landing", display="Stair Landing")
cube("stair_underside", "stair_underside", display="Stair Soffit")
stairs("stair_steps", "stair_steps", "stair_landing", display="Concrete Stairs")
flicker_light("stair_lamp", "stair_lamp", "stair_lamp_off", "stair_wall_white", light=15, flicker=0.0, face="all",
              display="Caged Wall Lamp")
flicker_light("stair_lamp_flicker", "stair_lamp", "stair_lamp_off", "stair_wall_white", light=15, flicker=0.8,
              face="all", display="Failing Caged Wall Lamp")
cube("stair_lamp_dead", "stair_lamp_off", display="Dead Caged Wall Lamp")
for nm in ("out", "up", "count", "arrow"):
    cube(f"stair_graffiti_{nm}", f"stair_graffiti_{nm}", display="Scrawled Wall")
cube("stair_hose", "stair_hose", display="Fire Hose Cabinet")
cube("stair_hose_top", "stair_hose_top", display="Fire Hose Cabinet")

# handrail: posts and a top bar, one block tall, runs along x when facing north
shaped("stair_handrail", [
    (0, 0, 7, 2, 14, 9, "r"), (7, 0, 7, 9, 14, 9, "r"), (14, 0, 7, 16, 14, 9, "r"),
    (0, 14, 6.5, 16, 16, 9.5, "r"),
], {"r": "stair_rail"}, display="Handrail", sound="metal", unbreakable=True)

shaped("stair_chair", [
    (3, 7, 3, 13, 8, 13, "m"), (3, 8, 12, 13, 18, 13, "m"),
    (3, 0, 3, 4, 7, 4, "m"), (12, 0, 3, 13, 7, 4, "m"), (3, 0, 12, 4, 18, 13, "m"), (12, 0, 12, 13, 18, 13, "m"),
], {"m": "stair_chair"}, display="Folding Chair", sound="metal")
shaped("stair_paper", [(0, 0, 0, 16, 0.25, 16, {"all": None, "up": "p"})], {"p": "stair_paper"},
       display="Loose Papers", collides=False, sound="wool", unbreakable=True)

# painted floor number: one block, sixteen glyphs
custom("stair_digit", "com.noclip.block.StairDigitBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/stair_digit_10"}},
       display="Painted Floor Number", unbreakable=True)
for g in range(16):
    BLOCK_MODELS[f"stair_digit_{g}"] = {"parent": "minecraft:block/cube_all",
                                        "textures": {"all": f"noclip:block/stair_digit_{g}"}}
BLOCKSTATES["stair_digit"] = {"variants": {f"glyph={g}": {"model": f"noclip:block/stair_digit_{g}"} for g in range(16)}}


# fire doors: openable only on the right floor (Java decides)
def door_models(name, closed_tex, open_tex):
    for suffix, t in (("", closed_tex), ("_open", open_tex)):
        BLOCK_MODELS[name + suffix] = {"parent": "minecraft:block/block", "textures": {"f": f"noclip:block/{t}",
                                       "s": "noclip:block/stair_underside", "particle": f"noclip:block/{t}"},
                                       "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                                           "north": {"texture": "#f", "uv": [0, 0, 16, 16]},
                                           "south": {"texture": "#s"}, "east": {"texture": "#s"},
                                           "west": {"texture": "#s"}, "up": {"texture": "#s"}, "down": {"texture": "#s"}}}]}
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    v = {}
    for f, r in rot.items():
        for o in ("false", "true"):
            m = {"model": f"noclip:block/{name}{'_open' if o == 'true' else ''}"}
            if r:
                m["y"] = r
            v[f"facing={f},open={o}"] = m
    BLOCKSTATES[name] = {"variants": v}


custom("stair_door", "com.noclip.block.StairwellDoorBlock", {}, display="Fire Door", unbreakable=True, sound="metal")
custom("stair_door_top", "com.noclip.block.StairwellDoorBlock", {}, display="Fire Door", unbreakable=True, sound="metal")
door_models("stair_door", "stair_door", "stair_door_open")
door_models("stair_door_top", "stair_door_top", "stair_door_top_open")

# ------------------------------------------------------------------ sound


def shaft_air():
    d = 24.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "sw"), 60, 420) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 12) ** 2)
    whistle = Y.bp(Y.white(d, "swh"), 780, 820) * 0.05 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 8 + 1))
    tone = Y.drone(d, [38.0, 57.0], "stdrone", lfo=1 / 12) * 0.25
    x = Y.reverb(wind + whistle, 3.5, 0.6, "swrv")[: len(t)] + tone
    x = Y.loopable(Y.norm(x, 0.45), 1.5)
    return Y.stereoize(x, 0.0015), "stereo"


def concrete_steps(seed, n, rate, dist):
    def f():
        out = np.zeros(int((n * rate + 1.5) * Y.SR))
        for k in range(n):
            Y.place(out, Y.footstep(f"{seed}{k}", "concrete"), 0.1 + k * rate, 0.5 + 0.4 * (k % 2))
        out = Y.lp(out, 2400 - dist * 1400)
        return Y.norm(Y.reverb(out, 3.0, 0.45 + dist * 0.35, seed), 0.8 - dist * 0.4)
    return f


def slam(seed, size=1.4):
    def f():
        out = np.zeros(int(5 * Y.SR))
        Y.place(out, Y.impact(seed, size, ring=0.15), 0.05)
        rattle = Y.bp(Y.white(0.4, seed + "r"), 1500, 6000) * np.exp(-np.arange(int(0.4 * Y.SR)) / Y.SR * 12)
        Y.place(out, rattle, 0.1, 0.3)
        return Y.norm(Y.reverb(Y.lp(out, 2500), 4.0, 0.75, seed), 0.7)
    return f


def rail_ring():
    out = np.zeros(int(4 * Y.SR))
    n = int(3 * Y.SR)
    tt = np.arange(n) / Y.SR
    ring = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * (2 + i)) / (i + 1) for i, f in enumerate((612, 1381, 2290, 3405)))
    Y.place(out, ring * 0.6 + Y.bp(Y.white(3, "rr"), 2000, 7000) * np.exp(-tt * 40) * 0.5, 0.05)
    return Y.norm(Y.reverb(out, 3.5, 0.6, "rrrv"), 0.5)


def locked():
    out = np.zeros(int(1.2 * Y.SR))
    for k, at in enumerate((0.0, 0.18, 0.31, 0.52)):
        n = int(0.09 * Y.SR)
        tt = np.arange(n) / Y.SR
        clack = Y.bp(Y.white(0.09, f"lk{k}"), 900, 6000) * np.exp(-tt * 50)
        clack += np.sin(2 * np.pi * (480 + k * 30) * tt) * np.exp(-tt * 35) * 0.5
        Y.place(out, clack, at, 0.9 - k * 0.1)
    return Y.norm(Y.reverb(out, 1.2, 0.3, "lkrv"), 0.8)


def loop_sting():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "lps"), 200, 4000) * np.exp(-((t - 1.4) ** 2) / 0.05)
    x = x[::-1].copy()
    sub = np.sin(2 * np.pi * 42 * t) * np.exp(-((t - 0.6) ** 2) / 0.08) * 0.8
    return Y.norm(Y.reverb(x + sub, 2.0, 0.5, "lpsrv"), 0.5)


def door_give():
    out = np.zeros(int(2.5 * Y.SR))
    Y.place(out, locked()[: int(0.3 * Y.SR)], 0.0, 0.8)
    t = Y.t_axis(1.8)
    creak = np.sin(2 * np.pi * np.cumsum(220 + 90 * np.sin(2 * np.pi * 0.8 * t)) / Y.SR) * np.sin(np.pi * t / 1.8) * 0.3
    Y.place(out, creak * (0.5 + 0.5 * np.sin(2 * np.pi * 13 * t)), 0.35)
    return Y.norm(Y.reverb(out, 1.5, 0.4, "dgrv"), 0.7)


sound("ambient.stairwell", [("ambient/stairwell", shaft_air)], stream=True)
sound("ambient.stairwell.additions",
      [("ambient/stairwell_steps_far0", concrete_steps("ssf0", 8, 0.42, 0.8)),
       ("ambient/stairwell_steps_far1", concrete_steps("ssf1", 6, 0.6, 0.9)),
       ("ambient/stairwell_slam_far", slam("slf", 1.8)), ("ambient/stairwell_rail", rail_ring)],
      subtitle="Something in the stairwell")
sound("event.stairwell_steps", [("event/stairwell_steps0", concrete_steps("sse0", 7, 0.4, 0.3)),
                                ("event/stairwell_steps1", concrete_steps("sse1", 5, 0.33, 0.4))],
      subtitle="Footsteps on the stairs")
sound("event.stairwell_slam", [("event/stairwell_slam", slam("sls", 1.3))], subtitle="A door slams")
sound("event.stairwell_locked", [("event/stairwell_locked", locked)], subtitle="Locked")
sound("event.stairwell_loop", [("event/stairwell_loop", loop_sting)], subtitle="Something repeats")
sound("event.stairwell_door", [("event/stairwell_door", door_give)], subtitle="The door gives")

level("stairwell", fog="#1e2420", sky="#1e2420", ambient="ambient.stairwell",
      additions="ambient.stairwell.additions", additions_chance=0.004, fixed_time=18000, height=192, ambient_light=0.06)
