"""Silent Beach. Grey sand, a grey sea, waves rolling in without a sound. Things wash up.
In a place this quiet, anything you do carries, and something in the surf is listening."""
import json
import os
import numpy as np
from assets import cube, cube_faces, cross, shaped, exit_block, fence, level, sound, lang, entity_model, entity_tex, \
    ITEM_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import animated, draw_text, centered, paint_metal
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32
tx = A.tex


def sand(seed, lo="#8a8680", mid="#9c9890", hi="#aca8a0", wet=False):
    img = T.ramp(T.noise(seed, sigma=0.5) * 0.5 + T.fbm(seed + "f", octaves=3, base=5) * 0.5, lo, mid, hi)
    shells = (T.rng(seed + "sh").random((S, S)) > 0.985).astype(float)
    img = T.blend(img, T.solid("#d8d4cc"), shells * 0.7)
    yy, xx = np.mgrid[0:S, 0:S]
    ripple = (np.sin((xx + T.noise(seed + "r", sigma=3) * 8) * 0.9) > 0.75).astype(float) * 0.06
    img = T.shade(img, 1 - ripple)
    if wet:
        img = T.shade(img, 0.72 + T.noise(seed + "w", sigma=2.5) * 0.12)
        sheen = (T.noise(seed + "s", sigma=2) > 0.75).astype(float)
        img = T.blend(img, T.solid("#b8c0c8"), sheen * 0.3)
    return T.grain(img, seed, 0.05)


tx("sb_sand", lambda: sand("sbs"))
tx("sb_sand_dune", lambda: sand("sbd", "#948f86", "#a49f96", "#b4afa6"))
tx("sb_sand_wet", lambda: sand("sbw", "#6a6862", "#76746e", "#82807a", wet=True))
tx("sb_sand_side", lambda: sand("sbside", "#7e7a72", "#8a867e", "#96928a"))
cube("sb_sand", "sb_sand", sound="sand", display="Grey Sand")
cube_faces("sb_sand_dune", top="sb_sand_dune", bottom="sb_sand_side", side="sb_sand_side", sound="sand", display="Dune Sand")
cube("sb_sand_wet", "sb_sand_wet", sound="sand", display="Wet Sand")


def beach_grass():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("sbgrass")
    for _ in range(18):
        x0, h = 8 + r.random() * 16, 14 + r.random() * 18
        lean = (r.random() - 0.5) * 1.2
        for y in range(int(h)):
            px = int(x0 + lean * y * (y / h))
            if 0 <= px < S:
                rgb[S - 1 - y, px] = T.hexc("#7a7a5a") * (0.75 + 0.35 * y / h)
                a[S - 1 - y, px] = 1
    return rgb, a


tx("sb_beach_grass", beach_grass)
cross("sb_beach_grass", "sb_beach_grass", display="Beach Grass")


def foam_big(i, frames=16):
    """One wave front crossing four blocks of shallows toward the land, then dissolving."""
    w = 4 * S
    rgb = np.ones((S, w, 3)) * T.hexc("#e4e8ea")
    yy, xx = np.mgrid[0:S, 0:w]
    t = i / frames
    front = w * (1 - t * 1.1) + np.sin(yy / 4 + i) * 3 + T.noise("sbfoamj", shape=(S, w), sigma=2) * 4
    d = xx - front
    lace = T.noise(f"sbfoam{i % 4}", shape=(S, w), sigma=0.8)
    a = np.where((d > 0) & (d < 10), np.clip(1 - d / 10, 0, 1) * 0.85, 0)
    a = a * (lace > 0.35) + np.where((d >= 10) & (d < 28), 0.25 * (1 - (d - 10) / 18) * (lace > 0.55), 0)
    a = a * np.clip(1.3 - t, 0, 1)
    return rgb, np.clip(a, 0, 1)


def foam_slice(c):
    def frame(i):
        rgb, a = foam_big(i)
        return rgb[:, c * S:(c + 1) * S], a[:, c * S:(c + 1) * S]
    return frame


for c in range(4):
    animated(f"sb_foam_{c}", foam_slice(c), 16, frametime=4)
    shaped(f"sb_foam_{c}", [(0, 0, 0, 16, 0.5, 16, {"all": None, "up": "f!"})], {"f": f"sb_foam_{c}"},
           display="Silent Surf", collides=False, unbreakable=True, sound="sand", render="translucent")

# ------------------------------------------------------------------ the pier and the lifeguard tower


def planks(seed="sbpl"):
    img = T.ramp(T.fbm(seed, octaves=3, base=2), "#6a645a", "#7e786c", "#8a8476")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.45)
    img = T.shade(img, 1 - (np.sin(np.mgrid[0:S, 0:S][0] * 0.8 + T.noise(seed + "g", sigma=2) * 4) > 0.85) * 0.1)
    nails = ((xx % 8 == 2) | (xx % 8 == 5)) & (np.mgrid[0:S, 0:S][0] % 16 == 3)
    img[nails] = T.hexc("#3a2a20")
    return T.grain(img, seed, 0.05)


def pile():
    img = T.ramp(T.fbm("sbpile", octaves=3, base=2), "#3a362e", "#4e483e")
    yy = np.mgrid[0:S, 0:S][0]
    barn = (T.rng("sbbarn").random((S, S)) > 0.9) & (yy > 20)
    img[barn] = T.hexc("#c8c8c0")
    weed = (yy > 24) & (T.noise("sbweed", sigma=1) > 0.5)
    img[weed] = T.hexc("#2a3a2a")
    return T.grain(img, "sbpile", 0.05)


tx("sb_planks", planks)
tx("sb_pile", pile)
cube("sb_planks", "sb_planks", sound="wood", display="Pier Decking")
shaped("sb_pile", [(5, 0, 5, 11, 16, 11, "p")], {"p": "sb_pile"}, display="Pier Pile", unbreakable=True, sound="wood")
tx("sb_rail", lambda: planks("sbrail"))
fence("sb_pier_rail", "sb_rail", display="Pier Railing", sound="wood")


def lantern(lit):
    def f():
        img = paint_metal("sblan" + str(lit), "#2a2a2a", "#3a3a3a", rust=0.3)
        img = T.rect(img, 8, 8, 24, 26, "#ffe0a0" if lit else "#3a3a34")
        img[16, 8:24] = T.hexc("#2a2a2a")
        img[8:26, 16] = T.hexc("#2a2a2a")
        return img
    return f


tx("sb_lantern_on", lantern(True))
tx("sb_lantern_off", lantern(False))
tx("sb_iron", lambda: paint_metal("sbiron", "#2e2e2c", "#3e3e3a", rust=0.4))
shaped("sb_lamp_post", [(7, 0, 7, 9, 16, 9, "i")], {"i": "sb_iron"}, display="Pier Lamp Post", unbreakable=True)
shaped("sb_lantern", [(7, 0, 7, 9, 3, 9, "i"), (5, 3, 5, 11, 11, 11, "l"), (4, 11, 4, 12, 12, 12, "i")],
       {"i": "sb_iron", "l": "sb_lantern_on"}, display="Pier Lantern", light=13, emissive=True, unbreakable=True)
shaped("sb_lantern_dead", [(7, 0, 7, 9, 3, 9, "i"), (5, 3, 5, 11, 11, 11, "l"), (4, 11, 4, 12, 12, 12, "i")],
       {"i": "sb_iron", "l": "sb_lantern_off"}, display="Dead Pier Lantern", unbreakable=True)
tx("sb_deep_dark", lambda: T.blend(T.solid("#020406"), T.solid("#0a1418"), T.noise("sbdeep", sigma=3) * 0.5))
exit_block("sb_deep", "sb_deep_dark", "WATER", display="Something Very Deep")


def tower_wall():
    img = T.ramp(T.noise("sbtw", sigma=2), "#c8c4b8", "#d8d4c8")
    img[:6] = T.hexc("#b02a22")
    return T.grain(T.stains(img, "sbtws", "#6a6a60", count=2, strength=0.3, size=7), "sbtw", 0.04)


def tower_door():
    img = T.ramp(T.noise("sbtd", sigma=2), "#9a3a2a", "#aa4a34")
    img = T.border(img, "#e8e4dc", 2)
    img = centered(img, "GUARD", 6, "#e8e4dc")
    img = T.rect(img, 22, 16, 25, 19, "#c8c8c0")
    return T.grain(img, "sbtd", 0.04)


tx("sb_tower_wall", tower_wall)
tx("sb_tower_door", tower_door)
cube("sb_tower_wall", "sb_tower_wall", sound="wood", display="Lifeguard Tower")
exit_block("sb_tower_door", "sb_tower_door", "DOOR", trigger="USE", solid=True, display="Lifeguard Tower Door",
           sound="wood")
from assets import ladder
tx("sb_ladder", lambda: (np.ones((S, S, 3)) * T.hexc("#7a7468"),
                         (((np.mgrid[0:S, 0:S][1] < 4) | (np.mgrid[0:S, 0:S][1] > 27) | (np.mgrid[0:S, 0:S][0] % 8 < 3))).astype(float)))
ladder("sb_ladder", "sb_ladder", display="Tower Ladder", sound="wood", unbreakable=True)

# ------------------------------------------------------------------ washed up


def driftwood():
    img = T.ramp(T.fbm("sbdw", octaves=3, base=2), "#8a8478", "#a8a294")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - (np.sin(yy * 1.3 + T.noise("sbdwg", sigma=2) * 5) > 0.7) * 0.18)
    return T.grain(img, "sbdw", 0.04)


tx("sb_driftwood", driftwood)
shaped("sb_driftwood", [(0, 0, 5, 16, 5, 10, "d"), (10, 0, 2, 13, 3, 6, "d")], {"d": "sb_driftwood"},
       display="Driftwood", unbreakable=True)


def kelp():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("sbkelp")
    for _ in range(30):
        x, y = r.random() * S, r.random() * S
        for k in range(8):
            px, py = int(x + k * (r.random() - 0.3)) % S, int(y + np.sin(k) * 2) % S
            rgb[py, px] = T.hexc("#2a3220") * (0.8 + r.random() * 0.4)
            a[py, px] = 1
    return rgb, a


tx("sb_kelp", kelp)
shaped("sb_kelp", [(0, 0, 0, 16, 0.4, 16, {"all": None, "up": "k"})], {"k": "sb_kelp"}, display="Washed-Up Kelp",
       collides=False, unbreakable=True, sound="moss")


def suitcase():
    img = T.ramp(T.noise("sbsc", sigma=2), "#3a4a5a", "#4a5a6a")
    img = T.rect(img, 0, 14, 32, 17, "#2a2a2a")
    img = T.rect(img, 12, 2, 20, 5, "#8a8a80")
    img = T.stains(img, "sbscs", "#5a6a5a", count=2, strength=0.5, size=6)
    return T.grain(img, "sbsc", 0.04)


tx("sb_suitcase", suitcase)
shaped("sb_suitcase", [(2, 0, 4, 14, 4, 13, "s")], {"s": "sb_suitcase"}, display="Waterlogged Suitcase",
       kind="pickup", item="noclip:almond_water*3,noclip:battery*2,noclip:canned_beans*1", sound="wool", strength=0.5)


def fridge():
    img = T.ramp(T.noise("sbfr", sigma=2), "#c8c4b8", "#d4d0c4")
    img = T.rect(img, 26, 6, 28, 26, "#8a8a80")
    img = T.stains(img, "sbfrs", "#8a5a3a", count=3, strength=0.5, size=6)
    return T.grain(img, "sbfr", 0.04)


tx("sb_fridge", fridge)
shaped("sb_fridge", [(1, 0, 2, 15, 9, 15, "f")], {"f": "sb_fridge"}, display="Washed-Up Refrigerator",
       unbreakable=True, sound="metal")


def bottle():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    body = (np.abs(yy - 18) < 4) & (xx > 6) & (xx < 22)
    neck = (np.abs(yy - 18) < 1.5) & (xx >= 22) & (xx < 28)
    rgb[body | neck] = T.hexc("#4a7a5a")
    a[body | neck] = 0.9
    note = (np.abs(yy - 18) < 2) & (xx > 9) & (xx < 19)
    rgb[note] = T.hexc("#e8dcc0")
    return rgb, a


tx("sb_bottle", bottle)
shaped("sb_bottle", [(0, 0, 0, 16, 0.5, 16, {"all": None, "up": "b"})], {"b": "sb_bottle"},
       display="Message in a Bottle", kind="pickup", item="noclip:almond_water", collides=False, sound="glass",
       strength=0.2, render="cutout")


def boat_hull():
    img = T.ramp(T.noise("sbboat", sigma=2), "#2a4a5a", "#345462")
    img[:4] = T.hexc("#c8c4b8")
    img = T.stains(img, "sbboats", "#6a5a4a", count=2, strength=0.5, size=7)
    return T.grain(img, "sbboat", 0.04)


tx("sb_boat", boat_hull)
shaped("sb_boat_mid", [(0, 0, 0, 2, 8, 16, "h"), (14, 0, 0, 16, 8, 16, "h"), (0, 0, 0, 16, 2, 16, "h"),
                       (2, 5, 6, 14, 6, 10, "w")], {"h": "sb_boat", "w": "sb_driftwood"}, display="Beached Rowboat",
       unbreakable=True)
shaped("sb_boat_end", [(1, 0, 0, 3, 7, 12, "h"), (13, 0, 0, 15, 7, 12, "h"), (1, 0, 0, 15, 2, 12, "h"),
                       (3, 0, 10, 13, 7, 14, "h")], {"h": "sb_boat"}, display="Beached Rowboat", unbreakable=True)

# ------------------------------------------------------------------ the tide walker


def walker_tex():
    c = Canvas()
    skin_c = ("#a8b4b8", "#bcc6c8")
    sk = lambda w, h: T.ramp(T.noise(f"sbtw{w}{h}", shape=(h, w), sigma=1), *skin_c)

    def hair(w, h):
        img = T.ramp(T.noise(f"sbhair{w}{h}", shape=(h, w), sigma=0.6), "#0e1010", "#1e2220")
        return img

    def face(w, h):
        img = hair(w, h)
        # hair hangs over the face; a sliver of grey skin and one eye show through
        img[5:7, 5:7] = T.hexc(skin_c[0])
        img[5, 5] = T.hexc("#e8ecec")
        return img

    def body(w, h):
        img = T.ramp(T.noise(f"sbtb{w}{h}", shape=(h, w), sigma=1), "#4a524e", "#5a625c")
        tear = T.noise(f"sbtt{w}{h}", shape=(h, w), sigma=0.7) > 0.7
        img[tear] = T.hexc(skin_c[0])
        weed = T.noise(f"sbtwd{w}{h}", shape=(h, w), sigma=0.8) > 0.8
        img[weed] = T.hexc("#24301e")
        return img

    def arm(w, h):
        img = sk(w, h)
        img[: h // 3] = body(w, h)[: h // 3]
        return img

    paint_humanoid(c, hair, body, arm, body, face=face)
    return c.out()


entity_model("sb_tidewalker", humanoid(arm_len=15, leg_len=13))
entity_tex("sb_tidewalker", walker_tex)
lang("entity.noclip.sb_tidewalker", "Tide Walker")
lang("item.noclip.sb_tidewalker_spawn_egg", "Tide Walker Spawn Egg")
ITEM_MODELS["sb_tidewalker_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound: almost none


def hush():
    d = 26.0
    t = Y.t_axis(d)
    air = Y.lp(Y.brown(d, "sbair"), 90) * 0.35
    hiss = Y.bp(Y.pink(d, "sbhiss"), 1500, 6000) * 0.02 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 13))
    ring = np.sin(2 * np.pi * (round(6900 * d) / d) * t) * 0.005
    x = Y.loopable(Y.norm(air + hiss + ring, 0.18), 2.0)
    return Y.stereoize(x, 0.002), "stereo"


def gull():
    out = np.zeros(int(3.5 * Y.SR))
    for k, at in enumerate((0.1, 0.9)):
        d = 0.6
        tt = Y.t_axis(d)
        f = 1400 + 600 * np.sin(np.pi * tt / d) - 500 * tt
        call = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * np.sin(np.pi * tt / d) ** 2
        call += Y.bp(Y.white(d, f"sbg{k}"), 1500, 4000) * 0.2 * np.sin(np.pi * tt / d)
        Y.place(out, call[::-1] if k == 1 else call, at, 0.6)
    return Y.reverb(Y.lp(Y.norm(out, 0.3), 3500), 3.5, 0.85, "sbgull")


def foghorn():
    d = 5.0
    t = Y.t_axis(d)
    env = np.clip(t / 0.4, 0, 1) * np.clip((3.6 - t) / 0.6, 0, 1)
    x = (np.sign(np.sin(2 * np.pi * 82 * t)) * 0.4 + np.sin(2 * np.pi * 164 * t) * 0.3) * env
    return Y.reverb(Y.lp(Y.norm(x, 0.35), 500), 4.5, 0.9, "sbhorn")


def buoy_bell():
    out = np.zeros(int(4.0 * Y.SR))
    for at in (0.0, 1.3):
        n = int(2.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        b = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * k) for f, k in ((392, 1.2), (1046, 2.0), (1680, 3.5)))
        Y.place(out, b, at, 0.4)
    return Y.reverb(Y.lp(Y.norm(out, 0.3), 3000), 4.0, 0.85, "sbbuoy")


def sand_steps():
    out = np.zeros(int(3.0 * Y.SR))
    for k in range(5):
        n = int(0.25 * Y.SR)
        tt = np.arange(n) / Y.SR
        step = Y.bp(Y.white(0.25, f"sbst{k}"), 800, 5000) * np.exp(-tt * 18)
        Y.place(out, step, 0.1 + k * 0.5, 0.5)
    return Y.reverb(Y.norm(out, 0.35), 1.2, 0.3, "sbsteps")


def rise():
    d = 2.5
    t = Y.t_axis(d)
    slosh = Y.bp(Y.white(d, "sbrise"), 300, 2500) * np.sin(np.pi * t / d) ** 2 * 0.5
    gasp = Y.bp(Y.white(d, "sbgasp"), 600, 1800) * np.exp(-((t - 1.6) ** 2) / 0.02) * 1.5
    return Y.norm(slosh + gasp, 0.6)


sound("ambient.silent_beach", [("ambient/silent_beach", hush)], stream=True)
sound("ambient.silent_beach.additions",
      [("ambient/silent_beach_gull", gull), ("ambient/silent_beach_foghorn", foghorn),
       ("ambient/silent_beach_buoy", buoy_bell), ("ambient/silent_beach_steps", sand_steps)],
      subtitle="Something far off, over the water")
sound("event.silent_beach_rise", [("event/silent_beach_rise", rise)], subtitle="Something stands up in the surf")

level("silent_beach", fog="#a4a8aa", sky="#a4a8aa", water="#5a6870", water_fog="#2a3236",
      ambient="ambient.silent_beach", additions="ambient.silent_beach.additions", additions_chance=0.0025,
      fixed_time=1000, skylight=True, ceiling=False, height=80)
