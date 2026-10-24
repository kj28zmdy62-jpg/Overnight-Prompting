"""Hotel Ballroom. A wedding reception laid for two hundred, with nobody in it. String lights, a cake,
a band stage with the instruments still out. The waltz is slowing down."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, wood, chime, epiano, wow

S = 32


def carpet():
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = T.ramp(T.noise("bca", sigma=0.6) * 0.4 + 0.3, "#4a0e16", "#6a1a22")
    # gold scrolls: two interleaved sine vines and medallions
    vine = (np.abs(yy - 8 - np.sin(xx / 32 * 2 * np.pi * 2) * 4) < 0.9) | (np.abs(yy - 24 + np.sin(xx / 32 * 2 * np.pi * 2) * 4) < 0.9)
    med = (np.abs(np.sqrt((xx - 16) ** 2 + (yy - 16) ** 2) - 4) < 0.8) | (np.abs(np.sqrt((xx % 32) ** 2 + (yy % 32) ** 2) - 4) < 0.8)
    img = T.blend(img, T.solid("#b8923a"), (vine | med).astype(float) * 0.75)
    dots = ((xx.astype(int) % 8 == 4) & (yy.astype(int) % 8 == 0))
    img = T.blend(img, T.solid("#2a060a"), dots.astype(float))
    return T.grain(T.stains(img, "bcas", "#2a0a0a", count=1, strength=0.2, size=7), "bca", 0.04)


def dance_floor():
    img = wood("bdf", "#8a5a2a", "#a87038", plank=8)
    yy, xx = np.mgrid[0:S, 0:S]
    q = ((xx // 16) + (yy // 16)) % 2
    alt = wood("bdf2", "#6a4220", "#84542a", plank=8, vertical=True)
    img = T.blend(img, alt, q.astype(float))
    img = T.shade(img, 1 + np.exp(-((xx + yy - 20) % 32 - 8) ** 2 / 12.0) * 0.1)
    return T.grain(img, "bdf", 0.02)


def wall_panel():
    img = T.ramp(T.fbm("bwp", base=6), "#d8ccb0", "#e4d8bc")
    yy, xx = np.mgrid[0:S, 0:S]
    frame = ((xx == 3) | (xx == 28) | (yy == 3) | (yy == 28)) & (xx >= 3) & (xx <= 28) & (yy >= 3) & (yy <= 28)
    img = T.blend(img, T.solid("#b8963e"), frame.astype(float) * 0.85)
    img = T.shade(img, 1 - ((xx == 4) | (yy == 4)) * 0.1)
    return T.grain(img, "bwp", 0.015)


def wall_base():
    img = wood("bwb", "#3a1a10", "#4a2416", plank=32, vertical=True)
    img[0:3] = brushed("bwbg", "#9a7a32", "#d0aa52", vertical=False)[0:3]
    return img


def drape():
    yy, xx = np.mgrid[0:S, 0:S]
    folds = np.sin(xx / 32 * 2 * np.pi * 4) * 0.5 + 0.5
    img = T.ramp(folds * 0.7 + T.noise("bdr", sigma=1) * 0.3, "#3a0810", "#7a1a24")
    return T.grain(img, "bdr", 0.03)


def ceiling():
    img = T.ramp(T.fbm("bce", base=5), "#2a1e18", "#34261e")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, T.solid("#6a5228"), ((xx % 16 == 0) | (yy % 16 == 0)).astype(float) * 0.6)
    return T.grain(img, "bce", 0.03)


def cloth():
    img = T.ramp(T.noise("bcl", sigma=1.5), "#e8e4dc", "#f4f0e8")
    yy, xx = np.mgrid[0:S, 0:S]
    folds = np.sin(xx / 32 * 2 * np.pi * 6) * 0.03
    return T.grain(T.shade(img, 1 + folds), "bcl", 0.01)


def cloth_top():
    img = cloth()
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    # place settings: plates round the edge, a ring of cutlery
    for ang in np.linspace(0, 2 * np.pi, 6, endpoint=False):
        cx, cy = 16 + np.cos(ang) * 10, 16 + np.sin(ang) * 10
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        img = T.blend(img, T.solid("#ffffff"), (d < 3.2).astype(float))
        img = T.blend(img, T.solid("#c8a850"), (np.abs(d - 3.4) < 0.5).astype(float))
    return img


def chair_gold():
    return brushed("bcg", "#9a7a32", "#d4b05a", vertical=True)


def sash():
    return T.grain(T.ramp(T.noise("bsa", sigma=1), "#c8a8b0", "#d8b8c0"), "bsa", 0.02)


def candle():
    return T.grain(T.ramp(T.noise("bcn", sigma=1), "#f0e8d0", "#faf4e0"), "bcn", 0.02)


def flame():
    yy, xx = np.mgrid[0:S, 0:S]
    return T.ramp(np.clip(1 - np.abs(xx - 16) / 16, 0, 1), "#ffb030", "#fff8d0")


def flowers():
    rgb = T.ramp(T.noise("bfl", sigma=0.8), "#e8e0e4", "#ffffff")
    r = T.rng("bflr").random((S, S))
    rgb = T.blend(rgb, T.solid("#d8a0b0"), (r > 0.8).astype(float))
    rgb = T.blend(rgb, T.solid("#3a5a2a"), (r < 0.15).astype(float))
    return rgb


def cake():
    img = T.ramp(T.noise("bck", sigma=1.5), "#f4f0e4", "#fcf8ee")
    yy, xx = np.mgrid[0:S, 0:S]
    piping = ((yy % 8) == 0) | (((yy % 8) == 1) & ((xx % 3) == 0))
    img = T.blend(img, T.solid("#e8dcc8"), piping.astype(float))
    pearls = ((yy % 8) == 4) & ((xx % 4) == 0)
    return T.blend(img, T.solid("#ffffff"), pearls.astype(float))


def cake_cut():
    img = cake()
    yy, xx = np.mgrid[0:S, 0:S]
    wedge = (xx > 14) & (yy > 4)
    sponge = T.ramp(T.noise("bccs", sigma=1), "#e8c890", "#f0d8a0")
    img = T.blend(img, sponge, wedge.astype(float))
    jam = wedge & ((yy % 9) == 0)
    return T.blend(img, T.solid("#8a1a2a"), jam.astype(float))


def stage():
    return wood("bst", "#2a1a10", "#3a2416", plank=8)


def bulb_string():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    wire_y = 4 + (np.sin(xx / 32 * np.pi) * 6).astype(int)
    wire = (yy == wire_y)
    rgb[wire] = T.hexc("#1a1a1a")
    a[wire] = 1
    for bx in (4, 12, 20, 28):
        by = 4 + int(np.sin(bx / 32 * np.pi) * 6) + 3
        b = ((xx - bx) ** 2 + (yy - by) ** 2) < 4
        rgb[b] = T.hexc("#fff0b8")
        a[b] = 1
    return rgb, a


def crystal():
    rgb = T.ramp(T.noise("bcr", sigma=0.6), "#e8f0f4", "#ffffff")
    yy, xx = np.mgrid[0:S, 0:S]
    sparkle = ((xx + yy) % 5 == 0)
    rgb = T.shade(rgb, 1 - sparkle * 0.15)
    return rgb, np.ones((S, S)) * 0.85


def drum():
    img = T.ramp(T.noise("bdm", sigma=1.5), "#7a1a1a", "#9a2222")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, brushed("bdmr", "#8a8a88", "#d0d0cc", vertical=False), ((yy < 3) | (yy > 28)).astype(float))
    return img


def drum_head():
    return T.grain(T.ramp(T.noise("bdh", sigma=2), "#e8e4d8", "#f0ece0"), "bdh", 0.02)


def piano():
    img = T.ramp(T.noise("bpi", sigma=2), "#0a0a0a", "#1a1a1a")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 + np.exp(-((xx - yy) % 32 - 6) ** 2 / 8.0) * 0.6)
    return img


def keys():
    img = T.solid("#f4f0e4")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 4) == 0) * 0.3)
    black = (yy < 18) & ((xx % 4) == 2) & ((xx // 4) % 7 not in ()) & (((xx // 4) % 7) != 2) & (((xx // 4) % 7) != 6)
    return T.blend(img, T.solid("#0a0a0a"), black.astype(float))


def ballroom_door(exit_=False):
    img = wood("bdo" + str(exit_), "#4a2416", "#5a2e1c", plank=16, vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, brushed("bdob", "#9a7a32", "#d4b05a"), (((xx == 2) | (xx == 29)) & (yy > 6)).astype(float))
    img = T.rect(img, 12, 14, 20, 16, "#d4b05a")
    if exit_:
        img = T.rect(img, 6, 2, 26, 9, "#0e2a14")
        img = ink(img, centered("EXIT", y=2, scale=1), "#40ff80")
    return T.grain(img, "bdo" + str(exit_), 0.03)


def card():
    return T.grain(T.solid("#f8f4ea"), "bcd", 0.01)


tex("ball_carpet", carpet)
tex("ball_dance_floor", dance_floor)
tex("ball_wall", wall_panel)
tex("ball_wall_base", wall_base)
tex("ball_drape", drape)
tex("ball_ceiling", ceiling)
tex("ball_cloth", cloth)
tex("ball_cloth_top", cloth_top)
tex("ball_gold", chair_gold)
tex("ball_sash", sash)
tex("ball_candle", candle)
tex("ball_flame", flame)
tex("ball_flowers", flowers)
tex("ball_cake", cake)
tex("ball_cake_cut", cake_cut)
tex("ball_stage", stage)
tex("ball_bulbs", bulb_string)
tex("ball_crystal", crystal)
tex("ball_drum", drum)
tex("ball_drum_head", drum_head)
tex("ball_piano", piano)
tex("ball_keys", keys)
tex("ball_door", lambda: ballroom_door(False))
tex("ball_door_exit", lambda: ballroom_door(True))
tex("ball_card", card)

cube("ball_carpet", "ball_carpet", display="Ballroom Carpet", sound="wool")
cube("ball_dance_floor", "ball_dance_floor", display="Dance Floor", sound="wood")
cube("ball_wall", "ball_wall", display="Ballroom Panelling")
cube("ball_wall_base", "ball_wall_base", display="Ballroom Panelling", sound="wood")
cube("ball_drape", "ball_drape", display="Velvet Drapes", sound="wool")
cube("ball_ceiling", "ball_ceiling", display="Ballroom Ceiling", flat=True)
cube_faces("ball_stage", top="ball_stage", bottom="ball_stage", side="ball_drape", display="Band Stage", sound="wood")
shaped("ball_table", [(1, 0, 1, 15, 13, 15, {"all": "c", "up": "t"})], {"c": "ball_cloth", "t": "ball_cloth_top"},
       display="Reception Table", unbreakable=True, sound="wool")
shaped("ball_chair", [(4, 7, 4, 12, 8, 12, "g"), (4, 8, 11, 12, 20, 12, "g"), (4, 12, 11.5, 12, 15, 12.5, "s"),
                      (4, 0, 4, 5, 7, 5, "g"), (11, 0, 4, 12, 7, 5, "g"), (4, 0, 11, 5, 8, 12, "g"), (11, 0, 11, 12, 8, 12, "g")],
       {"g": "ball_gold", "s": "ball_sash"}, display="Chiavari Chair", unbreakable=True)
shaped("ball_centerpiece", [(6, -3, 6, 10, 2, 10, "f"), (7.5, -3, 3, 8.5, 1, 4, "c"), (7.5, 1, 3, 8.5, 2, 4, "fl"),
                            (12, -3, 7.5, 13, 0, 8.5, "c"), (12, 0, 7.5, 13, 1, 8.5, "fl")],
       {"f": "ball_flowers", "c": "ball_candle", "fl": "ball_flame"}, display="Centrepiece", light=6, emissive=True,
       collides=False, unbreakable=True)
shaped("ball_cake", [(1, 0, 1, 15, 6, 15, "c"), (3, 6, 3, 13, 11, 13, "c"), (5, 11, 5, 11, 15, 11, "c"),
                     (7, 15, 7, 9, 16, 9, "s")],
       {"c": "ball_cake", "s": "ball_sash"}, display="Wedding Cake", unbreakable=True, kind="pickup",
       item="noclip:almond_water*1,noclip:energy_bar*2", strength=0.4)
shaped("ball_bulbs", [(0, 6, 7.5, 16, 16, 8.5, {"all": None, "north": "b", "south": "b"})], {"b": "ball_bulbs"},
       display="String Lights", light=12, emissive=True, collides=False, unbreakable=True, sound="glass")
shaped("ball_chandelier", [(7.5, 10, 7.5, 8.5, 16, 8.5, "g"), (3, 6, 3, 13, 10, 13, "c"), (5, 2, 5, 11, 6, 11, "c"),
                           (7, 0, 7, 9, 2, 9, "c")],
       {"g": "ball_gold", "c": "ball_crystal"}, display="Chandelier", light=15, emissive=True, collides=False,
       unbreakable=True, sound="glass", render="translucent")
shaped("ball_drum", [(2, 0, 2, 14, 10, 14, {"all": "d", "up": "h"}), (0, 12, 0, 6, 13, 6, "m"), (2.5, 0, 2.5, 3.5, 12, 3.5, "m")],
       {"d": "ball_drum", "h": "ball_drum_head", "m": "ball_gold"}, display="Drum Kit", unbreakable=True)
shaped("ball_piano", [(0, 4, 2, 16, 14, 16, {"all": "p", "north": "p"}), (0, 10, 0, 16, 11, 2, {"all": "p", "up": "k"}),
                      (1, 0, 3, 3, 4, 5, "p"), (13, 0, 3, 15, 4, 5, "p")],
       {"p": "ball_piano", "k": "ball_keys"}, display="Piano", unbreakable=True)
shaped("ball_mic", [(7, 0, 7, 9, 1, 9, "g"), (7.5, 1, 7.5, 8.5, 22, 8.5, "g"), (7, 22, 6, 9, 25, 9, "p")],
       {"g": "ball_gold", "p": "ball_piano"}, display="Microphone", collides=False, unbreakable=True)
exit_block("ball_exit_door", "ball_door_exit", "DOOR", trigger="USE", solid=True, display="Ballroom Exit", sound="wood")
cube("ball_door", "ball_door", display="Ballroom Door", sound="wood")
exit_block("ball_backstage", "pitch_black", "DOOR", display="Backstage")

# ------------------------------------------------------------------ sound


def waltz():
    """Twelve seconds of a waltz. The level plays it slower and slower by lowering the pitch."""
    d = 12.0
    out = np.zeros(int(d * Y.SR))
    bar = 1.5
    prog = [(130.8, [261.6, 329.6, 392.0]), (98.0, [246.9, 293.7, 392.0]), (110.0, [261.6, 329.6, 440.0]),
            (87.3, [261.6, 349.2, 440.0]), (130.8, [261.6, 329.6, 392.0]), (98.0, [246.9, 293.7, 349.2]),
            (130.8, [261.6, 329.6, 392.0]), (98.0, [246.9, 293.7, 392.0])]
    mel = [659.3, 587.3, 523.3, 587.3, 659.3, 659.3, 659.3, 0, 587.3, 587.3, 587.3, 0, 659.3, 784.0, 784.0, 0,
           659.3, 587.3, 523.3, 587.3, 659.3, 659.3, 659.3, 659.3]
    for b, (bass, ch) in enumerate(prog):
        t0 = b * bar
        tb = Y.t_axis(0.8)
        Y.place(out, np.sin(2 * np.pi * bass * tb) * np.exp(-tb * 3) * 0.4, t0)
        for beat in (1, 2):
            for f in ch:
                Y.place(out, epiano(f, 0.5, f"wz{b}{beat}{f}", 0.6) * 0.08, t0 + beat * bar / 3)
    for k, f in enumerate(mel):
        if f:
            t = Y.t_axis(0.9)
            v = np.sin(2 * np.pi * f * t + 0.3 * np.sin(2 * np.pi * 5.5 * t)) * Y.env(len(t), 0.06, 0.3) * 0.14
            Y.place(out, v + 0.3 * v * np.sin(2 * np.pi * f * 2 * t), k * bar / 2)
    out = wow(out, depth=0.004, rate=0.3, seed="wzw")
    return Y.norm(Y.reverb(Y.lp(out, 4000), 2.5, 0.5, "wzr"), 0.8)


def room():
    d = 24.0
    t = Y.t_axis(d)
    air = Y.lp(Y.pink(d, "bra"), 400) * 0.4
    murmur = Y.bp(Y.pink(d, "brm"), 250, 1200) * 0.12 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 12))
    x = Y.reverb(air + murmur, 4.0, 0.7, "brrv")[: len(t)]
    return Y.stereoize(Y.loopable(Y.norm(x, 0.3), 1.5), 0.002), "stereo"


def clink():
    out = np.zeros(int(2.5 * Y.SR))
    n = int(1.5 * Y.SR)
    tt = np.arange(n) / Y.SR
    ring = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * 5) for f in (2650, 3720, 5120))
    Y.place(out, ring * 0.3, 0.05)
    Y.place(out, ring * 0.2, 0.28)
    return Y.norm(Y.reverb(out, 3.0, 0.7, "bclr"), 0.35)


def applause():
    d = 4.0
    t = Y.t_axis(d)
    claps = np.zeros(len(t))
    r = Y.rng("bap")
    for k in range(160):
        at = r.random() * 3.0
        n = int(0.03 * Y.SR)
        Y.place(claps, Y.bp(Y.white(0.03, f"bap{k}"), 1000, 6000) * np.exp(-np.arange(n) / Y.SR * 90) * r.random(), at)
    env = np.clip(t / 0.4, 0, 1) * np.clip((3.6 - t) / 1.2, 0, 1)
    return Y.norm(Y.reverb(claps * env, 3.5, 0.7, "bapr"), 0.35)


def chair_scrape():
    d = 1.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "bcs"), 300, 2000) * np.sin(np.pi * t / d) * (0.6 + 0.4 * np.sin(2 * np.pi * 27 * t))
    return Y.norm(Y.reverb(x, 3.0, 0.7, "bcsr"), 0.35)


def needle_off():
    d = 2.0
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * np.cumsum(400 * np.exp(-t * 2.5) + 30) / Y.SR) * np.exp(-t * 1.2)
    return Y.norm(Y.reverb(x, 3.0, 0.6, "bnor"), 0.7)


sound("ambient.ballroom", [("ambient/ballroom", room)], stream=True)
sound("ambient.ballroom.additions", [("ambient/ballroom_clink", clink), ("ambient/ballroom_applause", applause),
                                     ("ambient/ballroom_chair", chair_scrape)], subtitle="The reception goes on without you")
sound("event.ballroom_waltz", [("event/ballroom_waltz", waltz)], subtitle="The band plays a waltz")
sound("event.ballroom_stop", [("event/ballroom_stop", needle_off)], subtitle="The music winds down")

level("ballroom", fog="#2a1a14", sky="#2a1a14", ambient="ambient.ballroom", additions="ambient.ballroom.additions",
      additions_chance=0.003, fixed_time=18000, height=48, ambient_light=0.04)
