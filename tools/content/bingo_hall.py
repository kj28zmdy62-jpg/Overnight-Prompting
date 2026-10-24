"""Bingo Hall. Rows of folding tables and cards, a number board, a caller calling numbers to nobody.
Somebody is winning. It had better be you."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, exit_block, custom, level, sound, BLOCK_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, wood, voice, speaker, text_mask

S = 32


def lino():
    yy, xx = np.mgrid[0:S, 0:S]
    q = ((xx // 16) + (yy // 16)) % 2
    a = T.ramp(T.noise("bla", sigma=1), "#b8a27a", "#c4ae86")
    b = T.ramp(T.noise("blb", sigma=1), "#6a5236", "#765e40")
    img = T.blend(a, b, q.astype(float))
    scuff = T.noise("bls", sigma=1.2) > 0.78
    img = T.shade(img, 1 - scuff * 0.1)
    return T.grain(T.stains(img, "blst", "#4a3a20", count=2, strength=0.2, size=6), "bli", 0.03)


def paneling():
    return T.grain(wood("bpn", "#5a3e24", "#6e4c2c", plank=8, vertical=True), "bpn", 0.02)


def wall_upper():
    img = T.ramp(T.fbm("bwu", base=6), "#c8bc98", "#d4c8a4")
    img = T.stains(img, "bwus", "#9a8a5a", count=2, strength=0.25, size=9)
    return T.grain(img, "bwu", 0.02)


def ceiling():
    img = T.ramp(T.fbm("bce", base=4), "#a8a088", "#b4ac94")
    img = T.stains(img, "bces", "#7a6a40", count=3, strength=0.35, size=6)  # decades of smoke
    return T.grain(T.border(img, "#8a8270", 1), "bce", 0.03)


def tube(on=True):
    img = T.solid("#f8ffe0") if on else T.solid("#7a7a6a")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (np.abs(yy - 16) > 5) * 0.4)
    return T.border(img, "#8a8a7a", 2)


def table_top():
    img = T.ramp(T.noise("btt", sigma=2), "#d8d4c8", "#e4e0d4")
    return T.grain(T.border(img, "#8a8a84", 1), "btt", 0.02)


def chair_metal():
    return brushed("bcm", "#3a3c40", "#56585c")


def card(daubs):
    def f():
        img = T.solid("#f4f0e0")
        yy, xx = np.mgrid[0:S, 0:S]
        hdr = yy < 6
        img = T.blend(img, T.solid("#c83030"), hdr.astype(float))
        img = ink(img, text_mask("BINGO", S, S, 1, 0, 1, 1) * 0 + centered("BINGO", y=0, scale=1, spacing=1) * (yy < 7), "#ffffff")
        # 5x5 grid of tiny numbers
        for gy in range(5):
            for gx in range(5):
                x0, y0 = 1 + gx * 6, 7 + gy * 5
                img[y0:y0 + 4, x0:x0 + 5] = T.hexc("#e8e4d4")
                if not (gx == 2 and gy == 2):
                    img[y0 + 1, x0 + 1:x0 + 4] = T.hexc("#2a2a2a")
                    img[y0 + 2, x0 + 2] = T.hexc("#2a2a2a")
        r = T.rng(f"daub{daubs}")
        cells = [(gx, gy) for gx in range(5) for gy in range(5)]
        order = r.permutation(len(cells))
        n = [0, 4, 9, 15, 25][daubs]
        for idx in order[:n]:
            gx, gy = cells[idx]
            cx, cy = 3 + gx * 6, 9 + gy * 5
            d = (xx - cx) ** 2 + (yy - cy) ** 2
            img = T.blend(img, T.solid("#d838a8" if daubs < 4 else "#e83020"), (d < 6).astype(float) * 0.85)
        # the middle square is free, and already marked
        d = (xx - 15) ** 2 + (yy - 19) ** 2
        img = T.blend(img, T.solid("#d838a8"), (d < 6).astype(float) * 0.85)
        return img
    return f


def cage_wire():
    rgb = np.ones((S, S, 3)) * T.hexc("#c8b060")
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx % 6) == 0) | ((yy % 6) == 0)).astype(float)
    return rgb, a


def ball():
    img = T.solid("#f4f0e4")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, T.solid("#c83030"), ((xx + yy) % 16 < 4).astype(float))
    return img


def podium():
    img = wood("bpo", "#4a3018", "#5a3a20", plank=32, vertical=True)
    img = T.rect(img, 4, 6, 28, 18, "#c83030")
    img = ink(img, centered("BINGO", y=9, scale=1), "#f4e8a0")
    return img


def stage():
    return wood("bsg", "#4a3018", "#5c3c20", plank=8)


def curtain():
    yy, xx = np.mgrid[0:S, 0:S]
    folds = np.sin(xx / 32 * 2 * np.pi * 4) * 0.5 + 0.5
    return T.grain(T.ramp(folds * 0.7 + T.noise("bcu", sigma=1) * 0.3, "#3a2a10", "#6a5020"), "bcu", 0.03)


def board_back():
    img = T.solid("#141210")
    yy, xx = np.mgrid[0:S, 0:S]
    holes = ((xx % 8) - 4) ** 2 + ((yy % 8) - 4) ** 2 < 5
    return T.blend(img, T.solid("#2a2418"), holes.astype(float))


def ashtray():
    return T.grain(T.ramp(T.noise("bat", sigma=1), "#8a8a84", "#a0a09a"), "bat", 0.03)


def prize_door():
    img = T.ramp(T.noise("bpd", sigma=2), "#c8a030", "#d8b040")
    img = T.border(img, "#6a5010", 2)
    img = ink(img, centered("PRIZE", y=5, scale=1, spacing=0), "#6a1010")
    img = T.rect(img, 22, 16, 26, 19, "#6a5010")
    return T.grain(img, "bpd", 0.03)


def fire_exit():
    img = T.ramp(T.noise("bfe", sigma=2), "#5a5e5a", "#666a66")
    img = T.border(img, "#2a2c2a", 2)
    img = T.rect(img, 4, 3, 28, 10, "#0e2a14")
    img = ink(img, centered("EXIT", y=3, scale=1), "#40ff80")
    img = T.rect(img, 3, 18, 29, 21, "#b0b0a8")
    return T.grain(img, "bfe", 0.04)


tex("bingo_lino", lino)
tex("bingo_panel", paneling)
tex("bingo_wall", wall_upper)
tex("bingo_ceiling", ceiling)
tex("bingo_tube", lambda: tube(True))
tex("bingo_tube_off", lambda: tube(False))
tex("bingo_table", table_top)
tex("bingo_metal", chair_metal)
for d in range(5):
    tex(f"bingo_card_{d}", card(d))
tex("bingo_cage", cage_wire)
tex("bingo_ball", ball)
tex("bingo_podium", podium)
tex("bingo_stage", stage)
tex("bingo_curtain", curtain)
tex("bingo_board", board_back)
tex("bingo_ashtray", ashtray)
tex("bingo_prize_door", prize_door)
tex("bingo_fire_exit", fire_exit)

cube("bingo_lino", "bingo_lino", display="Linoleum", sound="stone")
cube("bingo_panel", "bingo_panel", display="Wood Panelling", sound="wood")
cube("bingo_wall", "bingo_wall", display="Hall Wall")
cube("bingo_ceiling", "bingo_ceiling", display="Smoke-Stained Ceiling", flat=True)
flicker_light("bingo_tube", "bingo_tube", "bingo_tube_off", "bingo_ceiling", light=14, flicker=0.05, display="Hall Light")
flicker_light("bingo_tube_bad", "bingo_tube", "bingo_tube_off", "bingo_ceiling", light=14, flicker=0.7, display="Hall Light")
cube_faces("bingo_stage", top="bingo_stage", bottom="bingo_stage", side="bingo_curtain", display="Caller's Stage", sound="wood")
cube("bingo_curtain", "bingo_curtain", display="Stage Curtain", sound="wool")
cube("bingo_board", "bingo_board", display="Number Board", sound="metal")
shaped("bingo_table", [(0, 11, 0, 16, 12, 16, {"all": "m", "up": "t"}), (1, 0, 7, 2, 11, 9, "m"), (14, 0, 7, 15, 11, 9, "m")],
       {"t": "bingo_table", "m": "bingo_metal"}, display="Folding Table", unbreakable=True, sound="metal")
shaped("bingo_chair", [(3, 7, 3, 13, 8, 13, "m"), (3, 8, 12, 13, 17, 13, "m"), (3, 0, 3, 4, 7, 4, "m"),
                       (12, 0, 3, 13, 7, 4, "m"), (3, 0, 12, 4, 17, 13, "m"), (12, 0, 12, 13, 17, 13, "m")],
       {"m": "bingo_metal"}, display="Folding Chair", unbreakable=True, sound="metal")
shaped("bingo_cage", [(3, 6, 3, 13, 16, 13, "w"), (6, 8, 6, 8, 10, 8, "b"), (8, 7, 9, 10, 9, 11, "b"), (5, 0, 7, 11, 6, 9, "s")],
       {"w": "bingo_cage", "b": "bingo_ball", "s": "bingo_metal"}, display="Bingo Blower", unbreakable=True,
       render="cutout", collides=False)
shaped("bingo_podium", [(2, 0, 3, 14, 14, 13, {"all": "w", "north": "p", "south": "p"}), (1, 14, 2, 15, 16, 14, "w")],
       {"p": "bingo_podium", "w": "bingo_stage"}, display="Caller's Podium", unbreakable=True)
shaped("bingo_ashtray", [(5, -4, 5, 11, -3, 11, "a"), (5, -3, 5, 6, -2, 11, "a"), (10, -3, 5, 11, -2, 11, "a")],
       {"a": "bingo_ashtray"}, display="Ashtray", collides=False, unbreakable=True)
cube("bingo_prize_locked", "bingo_prize_door", display="Prize Room", sound="wood")
exit_block("bingo_prize_door", "bingo_prize_door", "DOOR", trigger="USE", solid=True, display="Prize Room", sound="wood")
exit_block("bingo_fire_exit", "bingo_fire_exit", "DOOR", trigger="USE", solid=True, display="Fire Exit", sound="metal")


# bingo cards: a Java block, flat on the table, marked by nobody
def card_model(d):
    return {"parent": "minecraft:block/block",
            "textures": {"c": f"noclip:block/bingo_card_{d}", "particle": f"noclip:block/bingo_card_{d}"},
            "elements": [{"from": [3, -4, 2], "to": [13, -3.8, 14], "faces": {"up": {"texture": "#c", "uv": [0, 0, 16, 16]},
                                                                             "down": {"texture": "#c"}}}]}


custom("bingo_card", "com.noclip.block.BingoCardBlock", card_model(0), display="Bingo Card", unbreakable=True,
       sound="wool", render="cutout")
for d in range(5):
    BLOCK_MODELS[f"bingo_card_{d}"] = card_model(d)
_v = {}
for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for d in range(5):
        m = {"model": f"noclip:block/bingo_card_{d}"}
        if r:
            m["y"] = r
        _v[f"facing={f},daubs={d}"] = m
BLOCKSTATES["bingo_card"] = {"variants": _v}

# ------------------------------------------------------------------ sound


def hall_tone():
    d = 24.0
    t = Y.t_axis(d)
    hum = Y.hum(d, 60.0, "bhh", buzz=0.5, harmonics=(1, 2, 4)) * 0.12
    blower = Y.bp(Y.pink(d, "bbl"), 200, 2500) * 0.25
    rattle = np.zeros(len(t))
    r = Y.rng("bbr")
    for k in range(int(d * 18)):
        at = r.random() * (d - 0.1)
        n = int(0.015 * Y.SR)
        Y.place(rattle, Y.bp(Y.white(0.015, f"bbr{k}"), 2000, 7000) * np.exp(-np.arange(n) / Y.SR * 200) * r.random() * 0.4, at)
    x = Y.reverb(blower + rattle, 2.0, 0.5, "bhrv")[: len(t)] + Y.hp(hum, 100)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.4), 1.0), 0.0015), "stereo"


def call(i):
    def f():
        out = np.zeros(int(2.6 * Y.SR))
        v1 = voice(f"bc{i}a", 0.45, pitch=135 - i * 5, breath=0.15)
        v2 = voice(f"bc{i}b", 0.9, pitch=128 - i * 5, breath=0.15)
        Y.place(out, v1, 0.05)
        Y.place(out, v2, 0.75)
        out = speaker(out, 350, 3600)
        return Y.norm(Y.reverb(out, 2.8, 0.55, f"bcr{i}"), 0.85)
    return f


def bingo_shout():
    v = voice("bshout", 1.0, pitch=260, breath=0.3, formants=[(800, 1300), (700, 1100), (500, 1800)])
    return Y.norm(Y.reverb(v, 3.0, 0.7, "bshr"), 0.8)


def dauber():
    out = np.zeros(int(0.6 * Y.SR))
    Y.place(out, Y.impact("bdb", 0.08), 0.0, 0.8)
    return Y.norm(Y.reverb(Y.bp(out, 200, 3000), 1.5, 0.4, "bdbr"), 0.5)


def ball_pop():
    out = np.zeros(int(1.2 * Y.SR))
    for k, at in enumerate((0.0, 0.07, 0.2)):
        n = int(0.03 * Y.SR)
        Y.place(out, Y.bp(Y.white(0.03, f"bbp{k}"), 1500, 7000) * np.exp(-np.arange(n) / Y.SR * 120), at, 1 - k * 0.3)
    return Y.norm(Y.reverb(out, 1.5, 0.4, "bbpr"), 0.6)


def cough():
    v = voice("bcough", 0.4, pitch=120, breath=1.0)
    out = np.zeros(int(2 * Y.SR))
    Y.place(out, v, 0.05)
    Y.place(out, v * 0.6, 0.45)
    return Y.norm(Y.reverb(Y.lp(out, 2500), 2.5, 0.7, "bcgr"), 0.3)


sound("ambient.bingo_hall", [("ambient/bingo_hall", hall_tone)], stream=True)
sound("ambient.bingo_hall.additions", [("ambient/bingo_hall_cough", cough), ("ambient/bingo_hall_dauber", dauber)],
      subtitle="Somebody marks a card")
sound("event.bingo_call", [(f"event/bingo_call{i}", call(i)) for i in range(4)], subtitle="The caller calls a number")
sound("event.bingo_shout", [("event/bingo_shout", bingo_shout)], subtitle="BINGO!")
sound("event.bingo_dauber", [("event/bingo_dauber", dauber)], subtitle="A card is marked")
sound("event.bingo_ball", [("event/bingo_ball", ball_pop)], subtitle="A ball drops")

level("bingo_hall", fog="#7a7454", sky="#7a7454", ambient="ambient.bingo_hall", additions="ambient.bingo_hall.additions",
      additions_chance=0.003, fixed_time=18000, height=32, particle="minecraft:white_ash", particle_chance=0.002)
