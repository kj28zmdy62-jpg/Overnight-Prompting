"""Endless Restrooms. Stalls forever, one always locked. Mirrors, dripping taps, hand dryers that
start on their own."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, exit_block, door, custom, level, sound, \
    BLOCK_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, voice, tiles

S = 32


def wall_tile():
    img = tiles("rwt", "#dedcd2", "#eae8de", "#a8a69a", n=8, grout_w=1)
    img = T.stains(img, "rwts", "#b8b08a", count=1, strength=0.15, size=10)
    return T.grain(img, "rwt", 0.015)


def wall_band():
    img = tiles("rwb", "#8aa898", "#9ab8a8", "#6a7a70", n=8, grout_w=1)
    return T.grain(img, "rwb", 0.015)


def wall_base():
    img = tiles("rwbs", "#5a6a60", "#66766c", "#3a4440", n=8, grout_w=1)
    yy = np.arange(S)[:, None] * np.ones((1, S))
    grime = np.clip((yy - 16) / 16, 0, 1) * T.noise("rwbg", sigma=2)
    return T.grain(T.shade(img, 1 - grime * 0.3), "rwbs", 0.02)


def floor():
    img = tiles("rfl", "#d4d2c8", "#e2e0d6", "#8a887c", n=8, grout_w=1)
    yy, xx = np.mgrid[0:S, 0:S]
    dark = (T.rng("rflb").random((8, 8)) < 0.1)[yy // 4, xx // 4] & ((yy % 4) > 0) & ((xx % 4) > 0)
    img = T.blend(img, T.solid("#2a2a2a"), dark.astype(float))
    img = T.stains(img, "rfls", "#8a8060", count=2, strength=0.25, size=7)
    return T.grain(img, "rfl", 0.02)


def ceiling():
    return T.grain(T.ramp(T.fbm("rce", base=4), "#d4d2c8", "#dedcd2"), "rce", 0.02)


def light(on=True):
    img = T.solid("#f2fff8") if on else T.solid("#7a827c")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (np.abs(xx - 16) > 9) * 0.25)
    if not on:
        img = T.stains(img, "rlo", "#3a3a30", count=2, strength=0.4, size=5)
    return T.border(img, "#a8aaa4", 2)


def partition():
    img = T.ramp(T.noise("rpa", sigma=2), "#b8ad94", "#c4b8a0")
    img = T.blend(img, T.solid("#8a8070"), (T.noise("rpas", sigma=0.7) > 0.85).astype(float) * 0.5)
    # carved initials
    r = T.rng("rpac")
    for _ in range(3):
        x, y = int(r.random() * 26), int(r.random() * 26)
        img[y, x:x + 4] *= 0.75
        img[y:y + 3, x] *= 0.75
    return T.grain(img, "rpa", 0.03)


def stall_door(locked):
    img = partition()
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.border(img, "#8a8070", 1)
    # latch with an indicator window
    img = T.rect(img, 23, 12, 30, 18, "#9a9a98")
    img = T.rect(img, 24, 13, 29, 17, "#c82020" if locked else "#20a040")
    img = T.rect(img, 3, 4, 8, 8, "#8a8a88")  # coat hook
    return img


def toilet():
    img = T.ramp(T.noise("rto", sigma=2), "#e8e8e4", "#f4f4f0")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 + np.exp(-((xx - 10) ** 2) / 20) * 0.06)
    return T.grain(img, "rto", 0.01)


def water():
    img = T.ramp(T.noise("rwa", sigma=2), "#8aa8a0", "#a0bcb4")
    return img


def dark_water():
    img = T.ramp(T.noise("rdw", sigma=3), "#020403", "#0a1210")
    return T.grain(img, "rdw", 0.02)


def counter():
    img = T.ramp(T.fbm("rco", base=4), "#4a4c4a", "#585a56")
    flecks = T.rng("rcof").random((S, S)) > 0.93
    img = T.blend(img, T.solid("#c8c8c0"), flecks.astype(float) * 0.5)
    return T.grain(img, "rco", 0.03)


def mirror(fig=False):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(np.clip((xx * 0.5 + yy) / 48 + T.noise("rmi", sigma=3) * 0.2, 0, 1), "#5a6460", "#a8b0ac")
    # the room behind you, dimly: the stall row and a light
    img = T.blend(img, T.solid("#7a7464"), ((yy > 14) & (yy < 30) & ((xx % 8) > 1)).astype(float) * 0.35)
    img = T.blend(img, T.solid("#e8f0ec"), ((yy < 3) & (np.abs(xx - 16) < 8)).astype(float) * 0.6)
    if fig:
        head = ((xx - 20) ** 2 + (yy - 10) ** 2) < 9
        body = (np.abs(xx - 20) < 5 - (yy < 16) * 2) & (yy > 12)
        img = T.blend(img, T.solid("#141614"), (head | body).astype(float) * 0.92)
    # desilvering in the corners
    corner = np.clip(1 - np.minimum(np.minimum(xx, 31 - xx), np.minimum(yy, 31 - yy)) / 6, 0, 1) * T.noise("rmd", sigma=1)
    img = T.blend(img, T.solid("#2a2a22"), corner * 0.6)
    img = T.border(img, "#b8b8b4", 1)
    return T.grain(img, "rmi" + str(fig), 0.02)


def chrome():
    return brushed("rch", "#8a8e90", "#d8dcde", vertical=True)


def dryer():
    img = brushed("rdr", "#9a9ea0", "#d0d4d6", vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.border(img, "#6a6e70", 1)
    img = T.rect(img, 10, 22, 22, 26, "#2a2a2a")
    img = ink(img, centered("PUSH", y=6, scale=1), "#2a2a2a")
    return img


def drain():
    img = T.solid("#1a1a18")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    grate = (d < 12) & (((xx % 4) < 2))
    img = T.blend(floor(), T.solid("#050505"), (d < 12).astype(float))
    img = T.blend(img, T.solid("#6a6a64"), grate.astype(float))
    img = T.blend(img, T.solid("#6a6a64"), (np.abs(d - 12) < 1).astype(float))
    return img


def shoes():
    return T.grain(T.ramp(T.noise("rsh", sigma=1), "#1a1412", "#2a201a"), "rsh", 0.04)


def paper():
    return T.grain(T.ramp(T.noise("rpp", sigma=1), "#e8e6dc", "#f4f2ea"), "rpp", 0.02)


tex("rest_tile", wall_tile)
tex("rest_tile_band", wall_band)
tex("rest_tile_base", wall_base)
tex("rest_floor", floor)
tex("rest_ceiling", ceiling)
tex("rest_light", lambda: light(True))
tex("rest_light_off", lambda: light(False))
tex("rest_partition", partition)
tex("rest_stall_door", lambda: stall_door(False))
tex("rest_stall_locked", lambda: stall_door(True))
tex("rest_porcelain", toilet)
tex("rest_water", water)
tex("rest_dark_water", dark_water)
tex("rest_counter", counter)
tex("rest_mirror", lambda: mirror(False))
tex("rest_mirror_fig", lambda: mirror(True))
tex("rest_chrome", chrome)
tex("rest_dryer", dryer)
tex("rest_drain", drain)
tex("rest_shoes", shoes)
tex("rest_paper", paper)

cube("rest_tile", "rest_tile", display="Restroom Tile", sound="tile")
cube("rest_tile_band", "rest_tile_band", display="Restroom Tile", sound="tile")
cube("rest_tile_base", "rest_tile_base", display="Restroom Tile", sound="tile")
cube("rest_floor", "rest_floor", display="Penny Tile Floor", sound="tile")
cube("rest_ceiling", "rest_ceiling", display="Restroom Ceiling", flat=True)
flicker_light("rest_light", "rest_light", "rest_light_off", "rest_ceiling", light=14, flicker=0.12, display="Restroom Light")
flicker_light("rest_light_bad", "rest_light", "rest_light_off", "rest_ceiling", light=14, flicker=0.9, display="Restroom Light")
cube_faces("rest_light_dead", bottom="rest_light_off", top="rest_ceiling", side="rest_ceiling", flat=True,
           display="Dead Restroom Light", sound="glass")
# stall partition: a thin wall that stops short of the floor and ceiling
shaped("rest_partition", [(0, 2, 7, 16, 16, 9, "p")], {"p": "rest_partition"}, display="Stall Partition",
       unbreakable=True, sound="metal")
shaped("rest_partition_top", [(0, 0, 7, 16, 12, 9, "p")], {"p": "rest_partition"}, display="Stall Partition",
       unbreakable=True, sound="metal")
door("rest_stall_door", "rest_stall_door", "rest_stall_door", display="Stall Door", sound="metal", unbreakable=True)
# the locked one: Java block, knock on it
custom("rest_stall_locked", "com.noclip.block.RestroomStallBlock", {
    "parent": "minecraft:block/block",
    "textures": {"d": "noclip:block/rest_stall_locked", "particle": "noclip:block/rest_stall_locked"},
    "elements": [{"from": [0, 2, 7], "to": [16, 16, 9], "faces": {f: {"texture": "#d"} for f in
                                                                   ("north", "south", "east", "west", "up", "down")}}]},
       display="Occupied Stall", unbreakable=True, sound="metal")
BLOCK_MODELS["rest_stall_locked_top"] = {
    "parent": "minecraft:block/block",
    "textures": {"d": "noclip:block/rest_partition", "particle": "noclip:block/rest_partition"},
    "elements": [{"from": [0, 0, 7], "to": [16, 12, 9], "faces": {f: {"texture": "#d"} for f in
                                                                   ("north", "south", "east", "west", "up", "down")}}]}
BLOCKSTATES["rest_stall_locked"] = {"variants": {
    f"facing={f},half={h}": dict({"model": "noclip:block/rest_stall_locked" + ("_top" if h == "upper" else "")},
                                 **({"y": r} if r else {}))
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for h in ("lower", "upper")}}

shaped("rest_toilet", [(4, 0, 6, 12, 6, 15, {"all": "p", "up": "w"}), (5, 6, 6, 11, 7, 14, "p"),
                       (3, 6, 0, 13, 13, 4, "p"), (4, 13, 0.5, 12, 14, 3.5, "p"), (5, 0, 4, 11, 6, 6, "p")],
       {"p": "rest_porcelain", "w": "rest_water"}, display="Toilet", unbreakable=True, sound="stone")
exit_block("rest_toilet_dark", "rest_dark_water", "WATER", display="Something Under the Water",
           model={"parent": "minecraft:block/block",
                  "textures": {"p": "noclip:block/rest_porcelain", "w": "noclip:block/rest_dark_water",
                               "particle": "noclip:block/rest_porcelain"},
                  "elements": [{"from": [4, 0, 6], "to": [12, 6, 15], "faces": dict(
                      {f: {"texture": "#p"} for f in ("north", "south", "east", "west", "down")}, up={"texture": "#w"})},
                               {"from": [3, 6, 0], "to": [13, 13, 4], "faces": {f: {"texture": "#p"} for f in
                                                                                ("north", "south", "east", "west", "up", "down")}}]})
shaped("rest_sink", [(0, 10, 2, 16, 13, 16, {"all": "c", "up": "c"}), (4, 12, 5, 12, 13.1, 12, "p"),
                     (7, 13, 2, 9, 16, 4, "ch"), (7, 15, 4, 9, 16, 7, "ch"), (2, 0, 13, 14, 10, 15, "c")],
       {"c": "rest_counter", "p": "rest_porcelain", "ch": "rest_chrome"}, display="Sink", unbreakable=True)
shaped("rest_mirror", [(0, 0, 0, 16, 16, 1, {"all": "f", "south": "m"})], {"m": "rest_mirror", "f": "rest_chrome"},
       display="Mirror", collides=False, unbreakable=True, sound="glass")
shaped("rest_mirror_fig", [(0, 0, 0, 16, 16, 1, {"all": "f", "south": "m"})], {"m": "rest_mirror_fig", "f": "rest_chrome"},
       display="Mirror", collides=False, unbreakable=True, sound="glass")
shaped("rest_dryer", [(3, 2, 0, 13, 12, 6, {"all": "c", "south": "d"}), (6, 1, 3, 10, 2, 6, "c")],
       {"c": "rest_chrome", "d": "rest_dryer"}, display="Hand Dryer", collides=False, unbreakable=True, sound="metal")
shaped("rest_shoes", [(3, 0, 4, 7, 2, 11, "s"), (9, 0, 4, 13, 2, 11, "s"), (3, 2, 8, 7, 4, 11, "s"), (9, 2, 8, 13, 4, 11, "s")],
       {"s": "rest_shoes"}, display="Shoes", collides=False, unbreakable=True)
shaped("rest_paper", [(0, 0, 0, 16, 0.3, 16, {"all": None, "up": "p"})], {"p": "rest_paper"}, display="Paper Towels",
       collides=False, unbreakable=True, sound="wool")
exit_block("rest_drain", "rest_drain", "FALL", display="Floor Drain", solid=False)

# ------------------------------------------------------------------ sound


def room_tone():
    d = 24.0
    t = Y.t_axis(d)
    hiss = Y.bp(Y.white(d, "rhs"), 2500, 7000) * 0.03 * (0.5 + 0.5 * np.clip(np.sin(2 * np.pi * t / 24 * 2), 0, 1))
    buzz = Y.hum(d, 60.0, "rbz", buzz=0.4, harmonics=(2, 4, 6)) * 0.08
    air = Y.lp(Y.pink(d, "rai"), 300) * 0.3
    drips = np.zeros(len(t))
    for k in range(int(d / 1.7)):
        n = int(0.08 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(drips, np.sin(2 * np.pi * np.cumsum(1500 - tt * 3000) / Y.SR) * np.exp(-tt * 60) * 0.25, k * 1.7 + 0.3)
    x = Y.reverb(hiss + air + drips, 1.6, 0.5, "rrv")[: len(t)] + Y.hp(buzz, 200)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.4), 1.0), 0.0012), "stereo"


def flush(dist=0.0):
    def f():
        d = 6.0
        t = Y.t_axis(d)
        rush = Y.bp(Y.white(d, f"rfl{dist}"), 300, 4000) * np.exp(-((t - 1.5) ** 2) / 1.2)
        gurgle = Y.lp(Y.white(d, f"rfg{dist}"), 500) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t)) * np.exp(-((t - 3) ** 2) / 1.0)
        refill = Y.bp(Y.white(d, f"rfr{dist}"), 2000, 6000) * 0.15 * np.clip((t - 2.5) / 3, 0, 1)
        x = rush + gurgle * 0.6 + refill
        out = Y.reverb(Y.lp(x, 5000 - dist * 3000), 2.0, 0.4 + dist * 0.3, f"rflr{dist}")
        return Y.norm(out, 0.8 - dist * 0.4)
    return f


def stall_slam():
    out = np.zeros(int(3 * Y.SR))
    Y.place(out, Y.impact("rss", 0.4, ring=0.2), 0.0)
    Y.place(out, Y.impact("rss2", 0.2), 0.12, 0.4)
    return Y.norm(Y.reverb(Y.bp(out, 150, 6000), 2.5, 0.6, "rssr"), 0.5)


def cough():
    v = voice("rcough", 0.5, pitch=140, breath=0.9)
    out = np.zeros(int(2.5 * Y.SR))
    Y.place(out, v, 0.1)
    Y.place(out, v * 0.7, 0.55)
    return Y.norm(Y.reverb(Y.lp(out, 2500), 2.5, 0.7, "rcr"), 0.35)


def dryer_roar():
    d = 4.5
    t = Y.t_axis(d)
    env = np.clip(t / 0.3, 0, 1) * np.clip((d - t) / 0.6, 0, 1)
    motor = Y.hum(d, 115.0, "rdm", buzz=0.8, harmonics=(1, 2, 3, 5)) * 0.2
    air = Y.bp(Y.white(d, "rda"), 400, 6000)
    x = (air + motor) * env
    return Y.norm(Y.reverb(x, 1.5, 0.35, "rdrv"), 0.9)


def knock_back():
    out = np.zeros(int(2 * Y.SR))
    for k in range(3):
        Y.place(out, Y.impact(f"rkb{k}", 0.3), 0.6 + k * 0.25)
    return Y.norm(Y.reverb(Y.bp(out, 100, 4000), 1.2, 0.3, "rkbr"), 0.8)


def knock():
    out = np.zeros(int(1 * Y.SR))
    Y.place(out, Y.impact("rkn", 0.25), 0.02)
    return Y.norm(Y.bp(out, 120, 5000), 0.7)


def unlock():
    out = np.zeros(int(1.6 * Y.SR))
    n = int(0.06 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, Y.bp(Y.white(0.06, "rul"), 1500, 8000) * np.exp(-tt * 60), 0.0)
    t = Y.t_axis(1.2)
    creak = np.sin(2 * np.pi * np.cumsum(300 + 120 * np.sin(2 * np.pi * 0.7 * t)) / Y.SR) * np.sin(np.pi * t / 1.2) * 0.3
    Y.place(out, creak, 0.3)
    return Y.norm(Y.reverb(out, 1.5, 0.4, "rulr"), 0.7)


sound("ambient.restrooms", [("ambient/restrooms", room_tone)], stream=True)
sound("ambient.restrooms.additions", [("ambient/restrooms_flush", flush(0.8)), ("ambient/restrooms_slam", stall_slam),
                                      ("ambient/restrooms_cough", cough)], subtitle="Someone in another stall")
sound("event.restrooms_dryer", [("event/restrooms_dryer", dryer_roar)], subtitle="A hand dryer starts")
sound("event.restrooms_knock", [("event/restrooms_knock", knock)], subtitle="Knocking")
sound("event.restrooms_knock_back", [("event/restrooms_knock_back", knock_back)], subtitle="Knocking from inside")
sound("event.restrooms_unlock", [("event/restrooms_unlock", unlock)], subtitle="The lock slides back")
sound("event.restrooms_flush", [("event/restrooms_flush", flush(0.0))], subtitle="A toilet flushes")

level("restrooms", fog="#c8d0c8", sky="#c8d0c8", ambient="ambient.restrooms", additions="ambient.restrooms.additions",
      additions_chance=0.004, fixed_time=6000, height=32)
