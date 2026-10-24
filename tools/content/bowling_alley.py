"""Bowling Alley - rows of maple lanes under a dark masking unit, cosmic carpet, orange seats.
Nobody is bowling, but the balls keep rolling, and the pins keep resetting themselves. The scoreboard
keeps score for player 2."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, level, sound
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32


def maple(seed="bwl", arrows=False, dots=False, foul=False):
    yy, xx = np.mgrid[0:S, 0:S]
    boards = (xx // 3)
    tint = T.rng(seed).random(11)[np.clip(boards, 0, 10)]
    img = T.ramp(tint[..., None][..., 0] * 0.4 + T.noise(seed + "g", sigma=0.6, shape=(S, S)) * 0.3 + 0.2, "#c89a5a", "#e0b878")
    img = T.shade(img, 1 - ((xx % 3) == 0) * 0.12)
    img = T.shade(img, 1 + np.clip(T.noise(seed + "sh", sigma=4) - 0.5, 0, 1) * 0.5)  # lane oil
    if arrows:
        tri = (yy >= 8) & (yy <= 22) & (np.abs(xx - 15.5) <= (22 - yy) * 0.35)
        img = T.blend(img, T.solid("#2a1a10"), tri.astype(float) * 0.85)
    if dots:
        for x in (6, 15, 24):
            img = T.blend(img, T.solid("#2a1a10"), K.disc(S, S, x, 16, 1.4).astype(float))
    if foul:
        img[14:18] = T.hexc("#1a1a1a")
    return T.grain(img, seed, 0.02)


def gutter():
    yy, xx = np.mgrid[0:S, 0:S]
    curve = 1 - np.abs(xx - 15.5) / 16
    img = T.ramp(curve * 0.7 + T.noise("bwgu", sigma=2) * 0.3, "#2a2a30", "#5a5a62")
    return T.grain(img, "bwgu", 0.02)


def capping():
    img = T.ramp(T.noise("bwcp", sigma=1.5), "#2a2a3a", "#343448")
    img[:, 14:18] = T.hexc("#c8c8d0")
    return img


def carpet():
    img = T.ramp(T.noise("bwca", sigma=0.5), "#1a0a2a", "#24103a")
    r = T.rng("bwcas")
    yy, xx = np.mgrid[0:S, 0:S]
    for k in range(10):
        x, y = r.random() * S, r.random() * S
        col = ["#ff6a2a", "#2ae0ff", "#ffe02a", "#ff2a9a"][k % 4]
        if k % 2:
            m = (np.abs(np.sqrt((xx - x) ** 2 + (yy - y) ** 2) - 3) < 0.7)
        else:
            m = K.line_mask(S, S, x, y, (x + 7) % S, (y + 3) % S, 1.2) > 0
        img = T.blend(img, T.solid(col), m * 0.8)
    return T.grain(img, "bwca", 0.03)


def wall():
    img = T.ramp(T.fbm("bwwa", octaves=3, base=5), "#1a2244", "#20284e")
    yy, xx = np.mgrid[0:S, 0:S]
    stripe = (yy >= 12) & (yy < 16)
    img = T.blend(img, T.solid("#ff6a2a"), stripe * 0.9)
    img = T.blend(img, T.solid("#ffe02a"), ((yy >= 17) & (yy < 19)) * 0.9)
    return T.grain(img, "bwwa", 0.03)


def masking(text, col):
    def f():
        h, w = 32, 64
        img = T.ramp(T.noise("bwmk" + text, sigma=2, shape=(h, w)), "#0a0a1a", "#121228")
        yy, xx = np.mgrid[0:h, 0:w]
        stars = (T.rng("bwmks" + text).random((h, w)) > 0.97)
        img = T.blend(img, T.solid("#e8e8ff", shape=(h, w)), stars * 0.7)
        m = K.centered(text, h, w, 12, font=K.F57)
        img = T.blend(img, T.solid(col, shape=(h, w)), K.glow(m, 1.6) * 0.6)
        img = T.blend(img, T.solid(col, shape=(h, w)), m)
        return img
    return f


def pin_white():
    img = T.ramp(T.noise("bwpw", sigma=2), "#e8e4dc", "#f8f6f0")
    img[8:10] = T.hexc("#c82020")
    img[11:12] = T.hexc("#c82020")
    return img


def ball_tex():
    yy, xx = np.mgrid[0:S, 0:S]
    swirl = np.sin(xx / 3.0 + np.sin(yy / 4.0) * 2) * 0.5 + 0.5
    img = T.ramp(swirl * 0.6 + T.noise("bwbl", sigma=2) * 0.4, "#1a0a3a", "#5a1a8a")
    img = T.shade(img, 1 + np.clip(T.noise("bwbls", sigma=3) - 0.6, 0, 1) * 0.8)
    for x, y in ((12, 10), (19, 10), (15.5, 17)):
        img = T.blend(img, T.solid("#050505"), K.disc(S, S, x, y, 1.6).astype(float))
    return img


def seat():
    return T.grain(T.ramp(T.noise("bwst", sigma=1.5), "#d8641e", "#e8742a"), "bwst", 0.02)


def chrome():
    yy, xx = np.mgrid[0:S, 0:S]
    return T.grain(T.ramp((np.sin(yy / 3.0) * 0.5 + 0.5) * 0.6 + T.noise("bwch", sigma=1) * 0.4, "#6a6e72", "#c8ccd0"), "bwch", 0.02)


def score_image():
    h, w = 32, 64
    img = T.solid("#020a20", shape=(h, w))
    yy, xx = np.mgrid[0:h, 0:w]
    grid = ((xx % 6 == 0) & (xx > 12)) | (yy == 15) | (yy == 0) | (yy == 31)
    img = T.blend(img, T.solid("#3a6ac8", shape=(h, w)), grid.astype(float) * 0.7)
    m1 = K.text_mask("1", h, w, 3, 4, font=K.F35)
    m2 = K.text_mask("2", h, w, 3, 20, font=K.F35)
    img = T.blend(img, T.solid("#f0f0f0", shape=(h, w)), np.maximum(m1, m2))
    # player 1 (you): nothing. player 2: strike after strike.
    for f in range(8):
        x0 = 14 + f * 6
        img = T.blend(img, T.solid("#ffe02a", shape=(h, w)), K.line_mask(h, w, x0, 19, x0 + 3, 23, 0.9))
        img = T.blend(img, T.solid("#ffe02a", shape=(h, w)), K.line_mask(h, w, x0 + 3, 19, x0, 23, 0.9))
        img = T.blend(img, T.solid("#5a6a8a", shape=(h, w)), K.line_mask(h, w, x0 + 1, 7, x0 + 2, 7, 0.9))
    return img


def return_hood():
    img = T.ramp(T.noise("bwrh", sigma=1.5), "#2a3a6a", "#344476")
    img[:3] = T.hexc("#c8ccd0")
    return T.grain(img, "bwrh", 0.02)


def pit_dark():
    return T.grain(T.solid("#020202"), "bwpit", 0.01)


def counter_top():
    img = T.ramp(T.noise("bwct", sigma=1.5), "#c8c4b8", "#d8d4c8")
    return T.grain(img, "bwct", 0.02)


def shoes():
    img = T.ramp(T.noise("bwsh", sigma=1), "#3a2a1a", "#4a3626")
    for cy in range(2):
        for cx in range(4):
            x0, y0 = 1 + cx * 8, 2 + cy * 16
            img = T.rect(img, x0, y0 + 8, x0 + 6, y0 + 12, "#c82a2a")
            img = T.rect(img, x0 + 3, y0 + 8, x0 + 6, y0 + 12, "#2a5ac8")
            img = T.rect(img, x0, y0 + 12, x0 + 6, y0 + 13, "#f0f0f0")
            img[min(y0 + 14, S - 1)] = T.hexc("#8a7a6a")
    return img


def lane_light():
    img = T.solid("#fff4e0")
    return T.border(img, "#3a3a4a", 3)


tex("bowl_lane", lambda: maple())
tex("bowl_lane_arrows", lambda: maple("bwla", arrows=True))
tex("bowl_lane_dots", lambda: maple("bwld", dots=True))
tex("bowl_lane_foul", lambda: maple("bwlf", foul=True))
tex("bowl_gutter", gutter)
tex("bowl_capping", capping)
tex("bowl_carpet", carpet)
tex("bowl_wall", wall)
for n, (text, col) in enumerate((("STRIKE", "#ff6a2a"), ("SPARE", "#2ae0ff"), ("GUTTER", "#ff2a9a"), ("BOWL", "#ffe02a"))):
    for p, t in enumerate(K.tiles(masking(text, col)())):
        tex(f"bowl_mask_{n}_{p}", (lambda t=t: t))
tex("bowl_pin", pin_white)
tex("bowl_ball", ball_tex)
tex("bowl_seat", seat)
tex("bowl_chrome", chrome)
for p, t in enumerate(K.tiles(score_image())):
    tex(f"bowl_score_{p}", (lambda t=t: t))
tex("bowl_return", return_hood)
tex("bowl_pit", pit_dark)
tex("bowl_counter", counter_top)
tex("bowl_shoes", shoes)
tex("bowl_lane_light", lane_light)

cube("bowl_lane", "bowl_lane", display="Maple Lane", sound="wood")
cube("bowl_lane_arrows", "bowl_lane_arrows", display="Maple Lane (Arrows)", sound="wood")
cube("bowl_lane_dots", "bowl_lane_dots", display="Maple Lane (Dots)", sound="wood")
cube("bowl_lane_foul", "bowl_lane_foul", display="Foul Line", sound="wood")
cube("bowl_gutter", "bowl_gutter", display="Gutter", sound="metal")
cube("bowl_capping", "bowl_capping", display="Lane Capping", sound="wood")
cube("bowl_carpet", "bowl_carpet", display="Cosmic Carpet", sound="wool")
cube("bowl_wall", "bowl_wall", display="Alley Wall", sound="stone")
for n in range(4):
    for p in range(2):
        cube(f"bowl_mask_{n}_{p}", f"bowl_mask_{n}_{p}", display="Masking Unit", sound="wood", light=5, emissive=True)
for p in range(2):
    cube(f"bowl_score_{p}", f"bowl_score_{p}", display="Scoreboard", sound="glass", light=5, emissive=True)
exit_block("bowl_pit", "bowl_pit", "FALL", display="The Pit Behind the Pins")
cube("bowl_counter", "bowl_counter", display="Counter", sound="wood")
cube("bowl_shoes", "bowl_shoes", display="Rental Shoes", sound="wood")
cube_faces("bowl_lane_light", bottom="bowl_lane_light", top="bowl_capping", side="bowl_capping",
           display="Lane Light", light=13, emissive=True, flat=True, sound="glass")

# ten pins in one block, standing in their triangle; and the same ten, scattered
PIN_XZ = [(8, 2), (6, 5), (10, 5), (4, 8), (8, 8), (12, 8), (2, 11), (6, 11), (10, 11), (14, 11)]
shaped("bowl_pins", [(x - 0.75, 0, z - 0.75, x + 0.75, 6, z + 0.75, "pin") for x, z in PIN_XZ],
       {"pin": "bowl_pin"}, display="Pins", unbreakable=True, collides=False)
shaped("bowl_pins_down", [(1, 0, 3, 7, 1.5, 4.5, "pin"), (9, 0, 2, 10.5, 1.5, 8, "pin"), (3, 0, 10, 9, 1.5, 11.5, "pin"),
                          (11, 0, 11, 15, 1.5, 12.5, "pin"), (12, 0, 4, 13.5, 6, 5.5, "pin"), (5, 0, 13, 6.5, 1.5, 15, "pin")],
       {"pin": "bowl_pin"}, display="Fallen Pins", unbreakable=True, collides=False)
shaped("bowl_ball", [(4, 0, 4, 12, 8, 12, "b"), (3, 1, 5, 13, 7, 11, "b"), (5, 1, 3, 11, 7, 13, "b")],
       {"b": "bowl_ball"}, display="Bowling Ball", unbreakable=True, collides=False)
shaped("bowl_return", [(1, 0, 0, 15, 8, 16, {"all": "hood", "up": "hood"}), (2, 8, 2, 14, 12, 14, "hood"),
                       (3, 12, 3, 8, 16, 8, "ball"), (8, 12, 8, 13, 16, 13, "ball")],
       {"hood": "bowl_return", "ball": "bowl_ball"}, display="Ball Return", unbreakable=True)
shaped("bowl_seat", [(2, 6, 2, 14, 8, 13, "seat"), (2, 8, 12, 14, 15, 14, "seat"), (7, 0, 6, 9, 6, 9, "chrome"),
                     (4, 0, 4, 12, 1, 11, "chrome")],
       {"seat": "bowl_seat", "chrome": "bowl_chrome"}, display="Molded Seat", unbreakable=True)

# ------------------------------------------------------------------ sound


def alley_tone():
    d = 28.0
    t = Y.t_axis(d)
    room = Y.lp(Y.brown(d, "bwroom"), 150) * 0.4 + Y.bp(Y.pink(d, "bwhvac"), 120, 800) * 0.12
    # the pinsetters, far off, still cycling: a slow mechanical clank pattern
    mech = np.zeros_like(t)
    for at in np.arange(0.5, d, 3.5):
        Y.place(mech, Y.impact(f"bwm{int(at)}", 0.3, ring=0.4), at, 0.4)
        Y.place(mech, Y.impact(f"bwn{int(at)}", 0.2, ring=0.2), at + 0.6, 0.25)
    mech = K.loop_reverb(Y.lp(mech, 1500), 3.0, 0.8, "bwmech")
    # a jukebox through two walls: just the bass line
    bass = np.zeros_like(t)
    notes = [K.hz(n) for n in ("E2", "E2", "G2", "A2", "C3", "B2", "A2", "G2")]
    for k, at in enumerate(np.arange(0, d, 0.5)):
        tt = Y.t_axis(0.45)
        K.circular_place(bass, np.sin(2 * np.pi * notes[k % 8] * tt) * np.exp(-tt * 4), at, 0.25)
    bass = Y.lp(bass, 180)
    x = room + mech * 0.6 + bass
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.002), "stereo"


def roll():
    d = 3.2
    t = Y.t_axis(d)
    rumble = Y.lp(Y.white(d, "bwroll"), 220) * (0.6 + 0.4 * np.sin(2 * np.pi * 11 * t))
    x = rumble * np.minimum(t * 3, 1) * np.minimum((d - t) * 6, 1)
    x += Y.lp(Y.brown(d, "bwroll2"), 80) * 0.5
    return Y.norm(Y.reverb(x, 2.0, 0.4, "bwroll"), 0.9)


def strike():
    out = np.zeros(int(3.0 * Y.SR))
    r = Y.rng("bwstr")
    Y.place(out, Y.impact("bwhit", 0.4, ring=0.3), 0.0, 1.0)
    for k in range(16):
        n = int(0.12 * Y.SR)
        tt = np.arange(n) / Y.SR
        clack = Y.bp(Y.white(0.12, f"bwcl{k}"), 600, 5000)[:n] * np.exp(-tt * 45)
        clack += np.sin(2 * np.pi * (700 + r.random() * 500) * tt) * np.exp(-tt * 40) * 0.4
        Y.place(out, clack, 0.02 + r.random() * 0.6, 0.4 + r.random() * 0.6)
    return Y.norm(Y.reverb(out, 3.0, 0.6, "bwstr"), 0.95)


def reset():
    d = 3.5
    t = Y.t_axis(d)
    motor = np.sin(2 * np.pi * np.cumsum(90 + 30 * np.sin(2 * np.pi * 0.5 * t)) / Y.SR) * 0.2 * (t < 2.8)
    sweep = Y.bp(Y.white(d, "bwsw"), 200, 2000) * np.exp(-((t - 0.8) ** 2) / 0.1) * 0.6
    out = motor + sweep
    Y.place(out, Y.impact("bwrs", 0.4, ring=0.6), 2.9, 0.8)
    return Y.norm(Y.reverb(out, 2.0, 0.5, "bwrs"), 0.8)


def clap():
    out = np.zeros(int(4.0 * Y.SR))
    for k in range(6):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.05, f"bwcp{k}"), 800, 4000)[:n] * np.exp(-tt * 70), 0.2 + k * 0.45, 0.8)
    return Y.norm(Y.reverb(out, 3.5, 0.85, "bwclap"), 0.5)


def ball_return():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.lp(Y.white(d, "bwbr"), 150) * np.sin(np.pi * t / d) * 0.7
    out = x.copy()
    Y.place(out, Y.impact("bwbrk", 0.5, ring=0.1), 2.5, 0.7)
    return Y.norm(Y.reverb(out, 2.5, 0.6, "bwbr"), 0.6)


sound("ambient.bowling_alley", [("ambient/bowling_alley", alley_tone)], stream=True)
sound("ambient.bowling_alley.additions",
      [("ambient/bowling_alley_strike", strike), ("ambient/bowling_alley_clap", clap),
       ("ambient/bowling_alley_return", ball_return)],
      subtitle="Somebody bowling")
sound("event.bowling_roll", [("event/bowling_roll", roll)], subtitle="A ball rolls down the lane")
sound("event.bowling_strike", [("event/bowling_strike", strike)], subtitle="Strike")
sound("event.bowling_reset", [("event/bowling_reset", reset)], subtitle="The pinsetter resets")

level("bowling_alley", fog="#0c0a18", sky="#0c0a18", ambient="ambient.bowling_alley",
      additions="ambient.bowling_alley.additions", additions_chance=0.004, fixed_time=18000, height=48)
