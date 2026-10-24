"""Locker Rooms. Benches, rows of lockers, showers running with nobody under them, steam."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, custom, level, sound, exit_block
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, wood, tiles, text_mask

S = 32


def locker_front(top, num, colour=("#2a4a7a", "#34568a")):
    def f():
        img = T.ramp(T.noise("lkf" + num + str(top), sigma=2), *colour)
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.border(img, "#1a2a44", 1)
        img[:, 15:17] = T.hexc("#1a2a44")
        if top:
            for half in (0, 16):
                vents = ((yy >= 4) & (yy <= 14) & ((yy % 3) == 0) & (xx >= half + 3) & (xx <= half + 12))
                img = T.blend(img, T.solid("#0a1220"), vents.astype(float))
                img = T.rect(img, half + 4, 20, half + 12, 25, "#c8c4b0")
            img = ink(img, text_mask(num[:2], S, S, 4, 20, 1, 1), "#1a1a1a")
            img = ink(img, text_mask(num[2:4], S, S, 20, 20, 1, 1), "#1a1a1a")
        else:
            for half in (0, 16):
                img = T.rect(img, half + 11, 2, half + 13, 9, "#a8a8a0")
        dents = T.noise("lkd" + num + str(top), sigma=1.5) > 0.82
        img = T.shade(img, 1 - dents * 0.12)
        scratches = T.rng("lks" + num).random((S, S)) > 0.985
        img = T.blend(img, T.solid("#8a8a84"), scratches.astype(float) * 0.6)
        return T.grain(img, "lkf" + num + str(top), 0.03)
    return f


def locker_side():
    return T.grain(T.ramp(T.noise("lks", sigma=2), "#24426e", "#2e4e7e"), "lks", 0.03)


def locker_odd_top():
    img = locker_front(True, "1313")()
    # the vents are dark all the way back, and something has scratched round the latch
    yy, xx = np.mgrid[0:S, 0:S]
    r = T.rng("lko")
    for _ in range(12):
        x, y = 8 + int(r.random() * 6), 18 + int(r.random() * 12)
        img[y, x:x + 3] = T.hexc("#9a9a94")
    return img


def floor():
    img = tiles("lfl", "#7a8084", "#868c90", "#4a4e50", n=8, grout_w=1)
    sheen = T.noise("lfls", sigma=3)
    img = T.shade(img, 0.92 + sheen * 0.18)
    return T.grain(img, "lfl", 0.02)


def wall():
    return T.grain(tiles("lwa", "#d8dcdc", "#e4e8e8", "#9aa0a2", n=4, grout_w=1), "lwa", 0.015)


def wall_band():
    img = tiles("lwb", "#2a4a7a", "#34568a", "#1a2a44", n=4, grout_w=1)
    return T.grain(img, "lwb", 0.015)


def wall_mold():
    img = wall()
    m = T.noise("lwm", sigma=1.5) * np.clip((np.arange(S)[:, None] - 8) / 24, 0, 1)
    return T.blend(img, T.solid("#2a3a28"), np.clip(m * 1.4 - 0.4, 0, 1))


def ceiling():
    img = T.ramp(T.fbm("lce", base=4), "#b8bcbe", "#c4c8ca")
    return T.grain(T.stains(img, "lces", "#7a8070", count=3, strength=0.4, size=7), "lce", 0.03)


def light(on=True):
    img = T.solid("#3a3c3e")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    glass = d < 11
    img = T.blend(img, T.ramp(np.clip(1 - d / 12, 0, 1), "#d8e4e0", "#ffffff") if on else T.solid("#5a5e5c"), glass.astype(float))
    cage = glass & ((np.abs(xx - 15.5) < 0.6) | (np.abs(yy - 15.5) < 0.6) | (np.abs(d - 6) < 0.5))
    return T.blend(img, T.solid("#1a1a1a"), cage.astype(float))


def bench():
    return wood("lbe", "#8a6a44", "#a07a50", plank=8)


def chrome():
    return brushed("lch", "#8a8e90", "#d0d4d6", vertical=True)


def drain():
    img = floor()
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#0a0a0a"), (d < 6).astype(float))
    img = T.blend(img, T.solid("#9a9a94"), ((d < 6) & ((xx % 3) == 0)).astype(float))
    return img


def towel():
    img = T.ramp(T.noise("lto", sigma=0.6), "#d8d4c8", "#e4e0d4")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    return T.blend(img, T.solid("#2a4a7a"), ((yy % 12) < 2).astype(float))


tex("lock_front", locker_front(False, "0000"))
for n in ("0412", "0413", "0711", "2201", "0999"):
    tex(f"lock_front_top_{n}", locker_front(True, n))
tex("lock_front_odd", locker_odd_top)
tex("lock_side", locker_side)
tex("lock_floor", floor)
tex("lock_wall", wall)
tex("lock_wall_band", wall_band)
tex("lock_wall_mold", wall_mold)
tex("lock_ceiling", ceiling)
tex("lock_light", lambda: light(True))
tex("lock_light_off", lambda: light(False))
tex("lock_bench", bench)
tex("lock_chrome", chrome)
tex("lock_drain", drain)
tex("lock_towel", towel)

for nm, front in [("lock_locker", "lock_front")] + [(f"lock_locker_top_{n}", f"lock_front_top_{n}") for n in
                                                   ("0412", "0413", "0711", "2201", "0999")]:
    shaped(nm, [(0, 0, 0, 16, 16, 16, {"all": "s", "north": "f"})], {"f": front, "s": "lock_side"},
           display="Locker", sound="metal", unbreakable=True, render="solid")
custom("lock_locker_odd", "com.noclip.block.LockerOddBlock", {
    "parent": "minecraft:block/block",
    "textures": {"f": "noclip:block/lock_front_odd", "s": "noclip:block/lock_side", "particle": "noclip:block/lock_side"},
    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": dict(
        {f: {"texture": "#s"} for f in ("south", "east", "west", "up", "down")}, north={"texture": "#f"})}]},
       display="Locker 1313", unbreakable=True, sound="metal")
from assets import BLOCKSTATES
BLOCKSTATES["lock_locker_odd"] = {"variants": {f"facing={f}": ({"model": "noclip:block/lock_locker_odd", "y": r} if r else
                                                              {"model": "noclip:block/lock_locker_odd"})
                                               for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}

cube("lock_floor", "lock_floor", display="Wet Tile Floor", sound="tile")
cube("lock_wall", "lock_wall", display="Changing Room Tile", sound="tile")
cube("lock_wall_band", "lock_wall_band", display="Changing Room Tile", sound="tile")
cube("lock_wall_mold", "lock_wall_mold", display="Mouldy Tile", sound="tile")
cube("lock_ceiling", "lock_ceiling", display="Damp Ceiling", flat=True)
flicker_light("lock_light", "lock_light", "lock_light_off", "lock_ceiling", light=13, flicker=0.1, display="Caged Light")
flicker_light("lock_light_bad", "lock_light", "lock_light_off", "lock_ceiling", light=13, flicker=0.85, display="Caged Light")
shaped("lock_bench", [(0, 7, 4, 16, 9, 12, "w"), (2, 0, 6, 4, 7, 10, "c"), (12, 0, 6, 14, 7, 10, "c")],
       {"w": "lock_bench", "c": "lock_chrome"}, display="Changing Bench", unbreakable=True)
shaped("lock_bench_towel", [(0, 7, 4, 16, 9, 12, "w"), (2, 0, 6, 4, 7, 10, "c"), (12, 0, 6, 14, 7, 10, "c"),
                            (5, 9, 4, 11, 10, 12, "t")],
       {"w": "lock_bench", "c": "lock_chrome", "t": "lock_towel"}, display="Changing Bench", unbreakable=True)
shaped("lock_shower", [(6, 6, 0, 10, 9, 3, "c"), (7, 7, 3, 9, 8, 7, "c"), (5, 4, 6, 11, 7, 10, "c"),
                       (6, 1, 0, 10, 3, 1, "c")],
       {"c": "lock_chrome"}, display="Shower Head", collides=False, unbreakable=True, sound="metal")
cube("lock_drain_tile", "lock_drain", display="Shower Drain", sound="tile")
exit_block("lock_drain_open", "lock_drain", "WATER", display="Shower Drain")

# ------------------------------------------------------------------ sound


def steam_room():
    d = 24.0
    t = Y.t_axis(d)
    water = Y.bp(Y.pink(d, "lsw"), 600, 7000) * 0.35 * (0.8 + 0.2 * np.sin(2 * np.pi * t / 8))
    hiss = Y.hp(Y.white(d, "lsh"), 5000) * 0.04
    vent = Y.lp(Y.brown(d, "lsv"), 150) * 0.3
    x = Y.reverb(water + hiss + vent, 2.8, 0.6, "lsrv")[: len(t)]
    return Y.stereoize(Y.loopable(Y.norm(x, 0.42), 1.5), 0.002), "stereo"


def shower():
    d = 6.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "lshw"), 800, 9000) * (0.85 + 0.15 * Y.lp(Y.white(d, "lshm"), 8) * 20)
    splash = Y.bp(Y.white(d, "lsp"), 200, 1500) * 0.4
    x = (x + splash) * Y.env(len(t), 0.3, 0.5)
    return Y.norm(Y.reverb(x, 1.2, 0.3, "lshr"), 0.6)


def bang(i):
    def f():
        out = np.zeros(int(2.5 * Y.SR))
        for k in range(1 + i):
            Y.place(out, Y.impact(f"lbg{i}{k}", 0.35, ring=0.35), 0.05 + k * 0.32, 1.0 - k * 0.2)
        return Y.norm(Y.reverb(Y.bp(out, 120, 6000), 1.6, 0.4, f"lbgr{i}"), 0.85)
    return f


def slam_far():
    out = np.zeros(int(3 * Y.SR))
    Y.place(out, Y.impact("lsf", 0.4, ring=0.4), 0.05)
    return Y.norm(Y.reverb(Y.lp(out, 2500), 3.0, 0.75, "lsfr"), 0.4)


def squeak():
    d = 0.9
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * np.cumsum(900 + 500 * t) / Y.SR) * np.sin(np.pi * t / d) * (0.6 + 0.4 * np.sin(2 * np.pi * 40 * t))
    return Y.norm(Y.reverb(x, 2.0, 0.6, "lsqr"), 0.3)


def drip():
    out = np.zeros(int(2.0 * Y.SR))
    n = int(0.09 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, np.sin(2 * np.pi * np.cumsum(1300 - tt * 3000) / Y.SR) * np.exp(-tt * 45), 0.05)
    return Y.norm(Y.reverb(out, 2.5, 0.7, "ldrr"), 0.35)


def locker_open():
    out = np.zeros(int(2.0 * Y.SR))
    Y.place(out, Y.bp(Y.white(0.05, "lop"), 1500, 7000) * np.exp(-np.arange(int(0.05 * Y.SR)) / Y.SR * 70), 0.0)
    t = Y.t_axis(1.0)
    hinge = np.sin(2 * np.pi * np.cumsum(500 - 200 * t) / Y.SR) * np.sin(np.pi * t) * 0.35
    Y.place(out, hinge, 0.1)
    return Y.norm(Y.reverb(out, 1.5, 0.4, "lopr"), 0.7)


sound("ambient.locker_rooms", [("ambient/locker_rooms", steam_room)], stream=True)
sound("ambient.locker_rooms.additions", [("ambient/locker_rooms_slam", slam_far), ("ambient/locker_rooms_squeak", squeak),
                                         ("ambient/locker_rooms_drip", drip)], subtitle="Somewhere in the changing rooms")
sound("event.locker_rooms_shower", [("event/locker_rooms_shower", shower)], subtitle="A shower running")
sound("event.locker_rooms_bang", [(f"event/locker_rooms_bang{i}", bang(i)) for i in range(3)], subtitle="Something inside a locker")
sound("event.locker_rooms_open", [("event/locker_rooms_open", locker_open)], subtitle="A locker opens")

level("locker_rooms", fog="#b8c0c4", sky="#b8c0c4", ambient="ambient.locker_rooms",
      additions="ambient.locker_rooms.additions", additions_chance=0.004, fixed_time=6000, height=32,
      particle="minecraft:white_ash", particle_chance=0.004)
