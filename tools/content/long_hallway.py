"""The Long Hallway - a hotel corridor that grows while you watch the door at the end of it.
The doors along the walls are painted on."""
import numpy as np
from assets import cube, tex, shaped, custom, exit_block, level, sound
import tex as T
import synth as Y
from content.rb_shared import text, text_center, split, wood, line

S = 32


def wallpaper():
    """Faded bottle-green stripes on dull cream, darker where hands trail along it."""
    yy, xx = np.mgrid[0:S, 0:S]
    base = T.ramp(T.fbm("lhwp", octaves=3, base=5) * 0.5 + 0.25, "#a8a27a", "#b6b088", "#c0ba92")
    stripe = ((xx % 16) >= 3) & ((xx % 16) < 7)
    thin = (xx % 16) == 10
    img = T.blend(base, T.solid("#4c5a3e"), stripe.astype(float) * 0.7 + thin.astype(float) * 0.45)
    # tiny dotted motif between stripes
    dots = ((xx % 16) == 13) & ((yy % 6) == 2)
    img = T.blend(img, T.solid("#6a6a4a"), dots.astype(float) * 0.6)
    img = T.shade(img, 0.9 + T.noise("lhwpn", sigma=3) * 0.15)
    return T.grain(img, "lhwp", 0.03)


def wainscot():
    """Cream-painted panelling, kicked and scuffed along the bottom."""
    img = T.ramp(T.noise("lhwain", sigma=2), "#bdb59a", "#cbc3a6")
    img = T.rect(img, 3, 9, 29, 27, "#000000", 0.08)
    img[9, 3:29] = img[9, 3:29] * 0.85
    img[9:27, 3] = img[9:27, 3] * 0.85
    img[26, 3:29] = np.clip(img[26, 3:29] * 1.08, 0, 1)
    img[0:4] = T.shade(wood("lhrail", "#5a3a22", "#6e4a2c", shape=(4, S), grain_dir="h"), 1.0)
    img[4] = img[4] * 0.6
    img[29:] = T.hexc("#3a2a1e")
    scuff = (T.noise("lhscuff", sigma=1) > 0.7) & (np.mgrid[0:S, 0:S][0] > 22)
    img = T.blend(img, T.solid("#6a6252"), scuff.astype(float) * 0.5)
    return T.grain(img, "lhwain", 0.03)


def boards():
    """Old floorboards running down the hall."""
    img = wood("lhboard", "#4a3220", "#5e4028", grain_dir="v", rings=5)
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.35)
    return T.grain(img, "lhboard", 0.03)


def runner(part):
    """A long burgundy runner with a gold-and-black border, worn down the middle."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("lhrun" + part, sigma=1), "#5e1a1c", "#6e2224")
    # repeating lozenges down the middle
    d = np.abs((xx % 16) - 8) + np.abs((yy % 16) - 8)
    img = T.blend(img, T.solid("#3a1214"), ((d > 5) & (d < 7)).astype(float) * 0.8)
    img = T.blend(img, T.solid("#a07a3a"), (d < 2).astype(float) * 0.7)
    if part != "m":
        bx = xx if part == "l" else 31 - xx
        img = T.blend(img, T.solid("#1a1412"), (bx < 9).astype(float))
        img = T.blend(img, T.solid("#a07a3a"), ((bx >= 9) & (bx < 11)).astype(float))
        img = T.blend(img, T.solid("#1a1412"), ((bx >= 11) & (bx < 12)).astype(float))
        img = T.blend(img, boards(), (bx < 5).astype(float))
    wear = np.exp(-((xx - (16 if part == "m" else (32 if part == "l" else -1))) ** 2) / 120.0)
    img = T.shade(img, 1 - wear * 0.18 * (T.noise("lhrw", sigma=2) + 0.5))
    return T.grain(img, "lhrun" + part, 0.035)


def ceiling():
    img = T.ramp(T.fbm("lhceil", octaves=3, base=5), "#b8b09a", "#c6bea8")
    img = T.stains(img, "lhceils", "#8a7a5a", count=1, strength=0.25, size=9)
    return T.grain(img, "lhceil", 0.02)


def painted_door(top, tint="#6a4a30", seed="a"):
    """A door painted straight onto the wall: flat, no seam, the perspective very slightly wrong.
    The paint is flaking, and the wallpaper stripe shows through underneath."""
    under = wallpaper() if top else wainscot()
    yy, xx = np.mgrid[0:S, 0:S]
    door = wood("lhpd" + seed + str(top), tint, "#8a6444", grain_dir="v", rings=3)
    door = T.shade(door, 0.95)
    mask = ((xx >= 4) & (xx < 28)).astype(float)
    if top:
        mask *= (yy >= 3)
    img = T.blend(under, door, mask)
    # painted panels, drawn with a heavy hand and a slight lean
    for (y0, y1) in (((7, 30),) if top else ((1, 22),)):
        for (x0, x1) in ((7, 15), (17, 25)):
            lean = 1 if top else 0
            img = line(img, x0 + lean, y0, x1 + lean, y0, "#3a2614", 0.8)
            img = line(img, x0, y1, x1, y1, "#c09a6a", 0.6)
            img = line(img, x0 + lean, y0, x0, y1, "#3a2614", 0.8)
            img = line(img, x1 + lean, y0, x1, y1, "#c09a6a", 0.5)
    # a painted frame and a painted shadow that falls the wrong way
    rows = mask[:, 4] > 0
    frame = np.zeros((S, S))
    frame[rows, 2:4] = 1
    frame[rows, 28:30] = 1
    if top:
        frame[1:3, 2:30] = 1
    img = T.blend(img, T.solid("#e0d6b8"), frame * 0.9)
    # the painted shadow falls left, though every sconce in the hall is to its right
    img = T.blend(img, T.solid("#2a2018"), ((xx >= 0) & (xx < 2) & rows[:, None]).astype(float) * 0.45)
    if not top:
        img = T.rect(img, 22, 4, 25, 7, "#c8a050")
        img = T.rect(img, 23, 5, 24, 6, "#f0d890")
        img = T.rect(img, 4, 29, 28, 32, "#3a2a1e")
    else:
        # brass number plate, painted, numbers rubbed away
        img = T.rect(img, 12, 11, 20, 15, "#b08a40")
        img = T.rect(img, 13, 12, 19, 14, "#8a6a30")
    flake = (T.noise("lhflake" + seed + str(top), sigma=0.8) > 0.8) & (mask > 0)
    img = T.blend(img, under, flake.astype(float) * 0.8)
    return T.grain(img, "lhpd" + seed + str(top), 0.025)


def real_door(top):
    """The door at the end. Real wood, real hinges, a line of light underneath."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = wood("lhreal" + str(top), "#2a1a10", "#3e2818", grain_dir="v", rings=4)
    for (y0, y1) in (((5, 29),) if top else ((2, 24),)):
        for (x0, x1) in ((5, 14), (18, 27)):
            img[y0:y1, x0] = img[y0:y1, x0] * 0.6
            img[y0, x0:x1] = img[y0, x0:x1] * 0.6
            img[y0:y1, x1] = np.clip(img[y0:y1, x1] * 1.4, 0, 1)
            img[y1, x0:x1 + 1] = np.clip(img[y1, x0:x1 + 1] * 1.4, 0, 1)
    img[:, :2] = T.hexc("#120a06")
    img[:, -2:] = T.hexc("#120a06")
    if top:
        img[:2] = T.hexc("#120a06")
        img = T.rect(img, 12, 10, 20, 15, "#c8a050")
        img = text_center(img, "1", 11, "#2a1a10")
    else:
        img = T.rect(img, 23, 2, 26, 6, "#d8b060")
        img = T.rect(img, 24, 3, 25, 5, "#fff0b0")
        img[29:32, 2:30] = T.hexc("#ffe8a8")
        img[28, 2:30] = T.hexc("#a08048")
    return T.grain(img, "lhreal" + str(top), 0.02)


def chalk_mural():
    """96x64 across the dead-end wall behind you. Somebody worked it out."""
    W, H = 96, 64
    top = np.concatenate([wallpaper()] * 3, axis=1)
    img = np.concatenate([top, top], axis=0)
    # grimed dark where hundreds of shoulders have leaned, so the chalk reads
    yy, xx = np.mgrid[0:H, 0:W]
    grime = np.clip(1.2 - (((xx - 48) / 52.0) ** 2 + ((yy - 30) / 36.0) ** 2), 0, 1)
    img = T.shade(T.blend(img, T.solid("#3a3a2a", shape=(H, W)), grime * 0.75), 0.9)
    chalk = "#f4f0e4"
    img = text_center(img, "IT ONLY GROWS", 8, chalk, scale=2, alpha=0.85, jitter=1)
    img = text_center(img, "WHILE YOU WATCH IT", 26, chalk, scale=1, alpha=0.85, jitter=1)
    img = text_center(img, "WALK AWAY FROM IT", 40, chalk, scale=1, alpha=0.75, jitter=1)
    # tally marks
    for k in range(17):
        x = 10 + k * 3 + (k // 5) * 3
        img = line(img, x, 52, x + (2 if k % 5 == 4 else 0), 60 - (6 if k % 5 == 4 else 0), chalk, 0.6)
    img = line(img, 70, 54, 86, 54, chalk, 0.6)
    img = line(img, 80, 50, 86, 54, chalk, 0.6)
    img = line(img, 80, 58, 86, 54, chalk, 0.6)
    return img


def sconce_shade():
    yy = np.mgrid[0:S, 0:S][0]
    img = T.ramp(np.clip(1 - yy / 32.0, 0, 1), "#e8b870", "#fff2c8")
    return T.grain(img, "lhshade", 0.02)


def brass():
    return T.grain(T.ramp(T.noise("lhbrass", sigma=1.5), "#7a5a24", "#b8904a"), "lhbrass", 0.03)


tex("lh_wallpaper", wallpaper)
tex("lh_wainscot", wainscot)
tex("lh_boards", boards)
for p in ("l", "m", "r"):
    tex(f"lh_runner_{p}", (lambda q: lambda: runner(q))(p))
tex("lh_ceiling", ceiling)
tex("lh_pdoor_low", lambda: painted_door(False))
tex("lh_pdoor_high", lambda: painted_door(True))
tex("lh_pdoor2_low", lambda: painted_door(False, "#3a4a5a", "b"))
tex("lh_pdoor2_high", lambda: painted_door(True, "#3a4a5a", "b"))
tex("lh_door_low", lambda: real_door(False))
tex("lh_door_high", lambda: real_door(True))
for idx, part in enumerate(("tl", "tm", "tr", "bl", "bm", "br")):
    tex(f"lh_chalk_{part}", (lambda i: lambda: split(chalk_mural(), 3, 2)[i])(idx))
tex("lh_shade", sconce_shade)
tex("lh_brass", brass)

cube("lh_wallpaper", "lh_wallpaper", display="Hallway Wallpaper", sound="wool")
cube("lh_wainscot", "lh_wainscot", display="Hallway Panelling", sound="wood")
cube("lh_boards", "lh_boards", display="Hallway Floorboards", sound="wood")
for p in ("l", "m", "r"):
    cube(f"lh_runner_{p}", f"lh_runner_{p}", display="Hallway Runner", sound="wool")
cube("lh_ceiling", "lh_ceiling", display="Hallway Ceiling", flat=True, sound="wool")
for v in ("pdoor", "pdoor2"):
    for half in ("low", "high"):
        custom(f"lh_{v}_{half}", "com.noclip.block.PaintedDoorBlock",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"noclip:block/lh_{v}_{half}"}},
               display="Painted Door", unbreakable=True, sound="wool")
exit_block("lh_door_low", "lh_door_low", "DOOR", trigger="USE", solid=True, display="The Door at the End", sound="wood")
exit_block("lh_door_high", "lh_door_high", "DOOR", trigger="USE", solid=True, display="The Door at the End", sound="wood")
for part in ("tl", "tm", "tr", "bl", "bm", "br"):
    cube(f"lh_chalk_{part}", f"lh_chalk_{part}", display="Chalked Wallpaper", sound="wool")

# wall sconce, mounted on the north side, facing south
shaped("lh_sconce", [(7, 2, 0, 9, 4, 4, "brass"), (5, 4, 2, 11, 10, 8, {"all": "shade", "up": None}),
                     (6, 0, 0, 10, 6, 1, "brass")],
       {"shade": "lh_shade", "brass": "lh_brass"}, display="Wall Sconce", light=11, emissive=True,
       collides=False, unbreakable=True, sound="glass")
shaped("lh_sconce_dead", [(7, 2, 0, 9, 4, 4, "brass"), (5, 4, 2, 11, 10, 8, {"all": "brass", "up": None}),
                          (6, 0, 0, 10, 6, 1, "brass")],
       {"brass": "lh_brass"}, display="Dead Sconce", collides=False, unbreakable=True, sound="glass")

# ------------------------------------------------------------------ sound


def corridor():
    """Close, carpeted air; sconces buzzing at the edge of hearing; a slow pressure that never resolves."""
    d = 28.0
    t = Y.t_axis(d)
    air = Y.lp(Y.brown(d, "lhair"), 180) * 0.5
    buzz = Y.lp(Y.hum(d, 50.0, "lhbuzz", buzz=0.25, harmonics=(2, 4, 6)), 1800) * 0.06
    swell = Y.drone(d, [36.7, 55.0], "lhdrone", lfo=1 / 14.0) * 0.18
    tick = np.zeros_like(t)
    x = air + buzz + swell + tick
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.5), 0.0015), "stereo"


def knock(i):
    def f():
        out = np.zeros(int(3.5 * Y.SR))
        for k in range(3):
            Y.place(out, Y.impact(f"lhk{i}{k}", 0.3), 0.1 + k * (0.42 + 0.05 * i), 0.9 - 0.15 * k)
        return Y.reverb(Y.lp(out, 1200), 2.6, 0.6, f"lhk{i}")
    return f


def far_steps(i):
    def f():
        out = np.zeros(int(6.0 * Y.SR))
        for k in range(8):
            Y.place(out, Y.footstep(f"lhfs{i}{k}", "concrete"), 0.2 + k * 0.62, 0.3 + 0.08 * k if i == 0 else 0.9 - 0.09 * k)
        return Y.reverb(Y.lp(out, 1400), 3.0, 0.7, f"lhfs{i}")
    return f


def far_door():
    out = np.zeros(int(4.0 * Y.SR))
    t = Y.t_axis(1.2)
    creak = Y.bp(np.sin(2 * np.pi * np.cumsum(180 + 120 * t) / Y.SR) * (Y.rng("lhcr").random(len(t)) > 0.5), 300, 2500)
    Y.place(out, creak * np.sin(np.pi * t / 1.2), 0.1, 0.3)
    Y.place(out, Y.impact("lhfd", 0.7), 1.5, 0.8)
    return Y.reverb(Y.lp(out, 1500), 3.2, 0.7, "lhfd")


def loop_thump():
    """Felt more than heard: the pressure in your ears when the hall resets."""
    d = 1.4
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * (38 - 10 * t) * t) * np.exp(-t * 3) * (1 - np.exp(-t * 60))
    return Y.norm(Y.lp(x, 150), 0.7)


def plaster_thud():
    out = np.zeros(int(0.8 * Y.SR))
    Y.place(out, Y.impact("lhpt", 0.2), 0.0, 1.0)
    return Y.norm(Y.lp(out, 700), 0.8)


sound("ambient.long_hallway", [("ambient/long_hallway", corridor)], stream=True)
sound("ambient.long_hallway.additions",
      [(f"ambient/long_hallway_knock{i}", knock(i)) for i in range(2)] +
      [(f"ambient/long_hallway_steps{i}", far_steps(i)) for i in range(2)] +
      [("ambient/long_hallway_door", far_door)],
      subtitle="Somewhere down the hall")
sound("event.long_hallway_loop", [("event/long_hallway_loop", loop_thump)], subtitle="Your ears pop")
sound("event.long_hallway_thud", [("event/long_hallway_thud", plaster_thud)], subtitle="Your hand meets plaster")

level("long_hallway", fog="#2a2014", sky="#2a2014", ambient="ambient.long_hallway",
      additions="ambient.long_hallway.additions", additions_chance=0.004, fixed_time=18000, height=32)
