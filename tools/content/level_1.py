"""Level 1 - Habitable Zone. An endless concrete warehouse in fog. Supplies, puddles, and blackouts."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, exit_block, level, sound, stairs, flat_model, BLOCK_MODELS
import tex as T
import synth as Y

S = 32


def concrete(seed, lo="#5d5d5a", hi="#77776f", stains=True):
    img = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.7 + T.noise(seed + "f", sigma=0.5) * 0.3, lo, hi)
    pores = (T.rng(seed + "p").random((S, S)) > 0.94).astype(float)
    img = T.shade(img, 1 - pores * 0.15)
    if stains:
        img = T.stains(img, seed, "#3f3e3a", count=2, strength=0.3, size=8)
    return T.grain(img, seed, 0.04)


def floor_tex():
    img = concrete("l1f", "#545450", "#6b6b65")
    return T.cracks(img, "l1fc", "#353532", count=1, alpha=0.5)


def floor_line():
    img = floor_tex()
    paint = T.grain(T.ramp(T.noise("paint", sigma=1), "#b8962a", "#d0ad3a"), "pl", 0.06)
    worn = (T.noise("worn", sigma=1.2) > 0.3).astype(float)
    mask = np.zeros((S, S))
    mask[:, 12:20] = worn[:, 12:20]
    return T.blend(img, paint, mask * 0.9)


def wet_floor():
    img = concrete("l1w", "#3c3d3c", "#4d4f4e", stains=False)
    sheen = T.noise("sheen1", sigma=3)
    return T.shade(img, 0.9 + sheen * 0.25)


def wall():
    img = concrete("l1wall", "#6a6a64", "#7d7d75")
    yy = np.arange(S)[:, None]
    seam = (yy % 16 == 0).astype(float) * np.ones((1, S)) * 0.12
    return T.shade(img, 1 - seam)


def pillar_side():
    img = concrete("l1p", "#707069", "#85857c")
    return img


def pillar_hazard():
    img = pillar_side()
    yy, xx = np.mgrid[0:S, 0:S]
    stripe = (((xx + yy) // 6) % 2 == 0)
    band = (yy > 14)
    col = np.where(stripe[..., None], T.hexc("#c9a227"), T.hexc("#1c1c1a"))
    worn = (T.noise("hz", sigma=0.8) > 0.25)
    m = (band & worn).astype(float)
    return T.blend(img, col, m)


def ceiling():
    img = concrete("l1c", "#3a3a38", "#474744", stains=False)
    yy, xx = np.mgrid[0:S, 0:S]
    beam = ((xx % 32) < 3).astype(float) * 0.25
    return T.shade(img, 1 - beam)


def strip_light():
    img = T.solid("#fffdf2")
    img = T.border(img, "#a8a8a0", 2)
    return img


def strip_light_off():
    img = T.solid("#8a8a84")
    return T.border(img, "#a8a8a0", 2)


def housing():
    return T.grain(T.ramp(T.noise("hous", sigma=1), "#8e8e88", "#a2a29a"), "hous", 0.03)


def crate_side():
    img = T.ramp(T.fbm("crate", octaves=3, base=3), "#7a5a34", "#94703e")
    yy, xx = np.mgrid[0:S, 0:S]
    planks = ((yy % 8) == 0).astype(float) * 0.25
    img = T.shade(img, 1 - planks)
    img = T.border(img, "#4e3a22", 2)
    # diagonal brace
    diag = (np.abs(xx - yy) < 2).astype(float)
    img = T.blend(img, T.solid("#5e4426"), diag * 0.8)
    return T.grain(img, "crate", 0.05)


def crate_top():
    img = T.ramp(T.fbm("cratet", octaves=3, base=3), "#7a5a34", "#94703e")
    xx = np.arange(S)[None, :] * np.ones((S, 1))
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.25)
    return T.border(img, "#4e3a22", 2)


def cardboard():
    img = T.ramp(T.noise("cb", sigma=1.5), "#a5824e", "#b8935a")
    img = T.rect(img, 0, 14, 32, 18, "#c9b088")
    return T.grain(img, "cb", 0.04)


def shelf_metal():
    return T.grain(T.ramp(T.noise("sm", sigma=1), "#3f5466", "#4d6478"), "sm", 0.04)


def shelf_board():
    return T.grain(T.ramp(T.noise("sb", sigma=1), "#8a7a60", "#9a8a6e"), "sb", 0.04)


def chainlink():
    rgb = np.ones((S, S, 3)) * T.hexc("#8a8e90")
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx + yy) % 8 == 0) | ((xx - yy) % 8 == 0)).astype(float)
    return rgb, a


def metal_door():
    img = T.ramp(T.noise("md", sigma=2), "#4a5a52", "#566a60")
    img = T.border(img, "#2e3834", 2)
    img = T.rect(img, 24, 14, 28, 18, "#b0b0a8")
    img = T.rect(img, 6, 4, 26, 12, "#1a1f1c")
    return T.grain(img, "md", 0.05)


def puddle():
    rgb = T.ramp(T.noise("pud", sigma=2.5), "#1c2224", "#2c3538")
    a = np.clip(T.noise("puda", sigma=3) * 1.6 - 0.2, 0, 0.85)
    return rgb, a


tex("l1_floor", floor_tex)
tex("l1_floor_line", floor_line)
tex("l1_floor_wet", wet_floor)
tex("l1_wall", wall)
tex("l1_pillar", pillar_side)
tex("l1_pillar_hazard", pillar_hazard)
tex("l1_ceiling", ceiling)
tex("l1_strip", strip_light)
tex("l1_strip_off", strip_light_off)
tex("l1_housing", housing)
tex("l1_crate", crate_side)
tex("l1_crate_top", crate_top)
tex("l1_cardboard", cardboard)
tex("l1_shelf_metal", shelf_metal)
tex("l1_shelf_board", shelf_board)
tex("l1_chainlink", chainlink)
tex("l1_metal_door", metal_door)
tex("l1_puddle", puddle)

cube("warehouse_concrete", "l1_floor", display="Warehouse Concrete")
cube("painted_concrete", "l1_floor_line", display="Line-Painted Concrete")
cube("wet_concrete", "l1_floor_wet", display="Wet Concrete")
cube("concrete_wall", "l1_wall", display="Poured Concrete Wall")
cube("concrete_pillar", "l1_pillar", display="Concrete Pillar")
cube("hazard_pillar", "l1_pillar_hazard", display="Hazard-Striped Pillar")
cube("warehouse_ceiling", "l1_ceiling", display="Warehouse Ceiling", flat=True)
cube_faces("wooden_crate", top="l1_crate_top", bottom="l1_crate_top", side="l1_crate", sound="wood",
           unbreakable=False, strength=2.0, display="Wooden Crate")
pane("chain_link_fence", "l1_chainlink", display="Chain-Link Fence")
exit_block("metal_service_door", "l1_metal_door", "DOOR", trigger="USE", solid=True, display="Service Door",
           sound="metal")

# hanging fluorescent strip: housing with an emissive underside
shaped("strip_light", [
    (2, 12, 6, 14, 16, 10, {"all": "housing", "down": "tube"}),
], {"housing": "l1_housing", "tube": "l1_strip"}, display="Strip Light", light=15, emissive=True,
    unbreakable=True, collides=False, sound="glass", render="solid")
shaped("strip_light_off", [
    (2, 12, 6, 14, 16, 10, {"all": "housing", "down": "tube"}),
], {"housing": "l1_housing", "tube": "l1_strip_off"}, display="Dead Strip Light", unbreakable=True,
    collides=False, sound="glass", render="solid")

shaped("cardboard_boxes", [
    (1, 0, 1, 9, 7, 9, "cb"),
    (8, 0, 6, 15, 6, 14, "cb"),
    (3, 7, 3, 9, 12, 8, "cb"),
], {"cb": "l1_cardboard"}, display="Cardboard Boxes", sound="wool")

shaped("metal_shelf", [
    (0, 0, 0, 1, 16, 1, "metal"), (15, 0, 0, 16, 16, 1, "metal"),
    (0, 0, 15, 1, 16, 16, "metal"), (15, 0, 15, 16, 16, 16, "metal"),
    (0, 1, 0, 16, 2, 16, "board"), (0, 8, 0, 16, 9, 16, "board"), (0, 15, 0, 16, 16, 16, "board"),
    (2, 2, 3, 8, 7, 12, "cb"), (9, 9, 2, 14, 13, 10, "cb"),
], {"metal": "l1_shelf_metal", "board": "l1_shelf_board", "cb": "l1_cardboard"}, display="Metal Shelving",
    sound="metal", unbreakable=True)

shaped("supply_crate", [
    (2, 0, 2, 14, 10, 14, {"all": "side", "up": "top"}),
], {"side": "l1_crate", "top": "l1_crate_top"}, display="Supply Crate", kind="pickup",
    item="noclip:almond_water*5,noclip:battery*3,noclip:canned_beans*3,noclip:energy_bar*3,noclip:flashlight*1",
    sound="wood", strength=0.5)

# a thin translucent layer of standing water
BLOCK_MODELS_PUDDLE = shaped("puddle", [(0, 0, 0, 16, 0.5, 16, "w")], {"w": "l1_puddle"}, display="Puddle",
                             collides=False, render="translucent", unbreakable=True, sound="mud")

stairs("concrete_stairs", "l1_floor", "warehouse_concrete", display="Concrete Stairs")
exit_block("stairwell_dark", "pitch_black", "DESCEND", display="Stairwell Darkness")

# ------------------------------------------------------------------ sound


def warehouse_ambience():
    d = 30.0
    t = Y.t_axis(d)
    rumble = Y.lp(Y.brown(d, "l1rum"), 120) * 0.6
    vent = Y.bp(Y.pink(d, "l1vent"), 300, 2200) * 0.12 * (0.8 + 0.2 * np.sin(2 * np.pi * t / 10))
    hum = Y.hum(d, 50.0, "l1hum", buzz=0.15, harmonics=(1, 2, 3)) * 0.18
    x = rumble + vent + Y.lp(hum, 600)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 1.5), 0.0012), "stereo"


def drip(i):
    def f():
        out = np.zeros(int(2.0 * Y.SR))
        for k in range(1 + i % 2):
            n = int(0.12 * Y.SR)
            tt = np.arange(n) / Y.SR
            fr = 1400 + i * 300 - tt * 3000
            plink = np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 40)
            Y.place(out, plink, 0.1 + k * 0.7, 0.7)
        return Y.reverb(out, 2.0, 0.6, f"drip{i}")
    return f


def clang(i):
    def f():
        out = np.zeros(int(4.0 * Y.SR))
        Y.place(out, Y.impact(f"clang{i}", 0.6, ring=0.8), 0.05)
        return Y.reverb(Y.lp(out, 3000), 3.5, 0.7, f"clang{i}")
    return f


def chain():
    d = 1.6
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("chain")
    for k in range(14):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        link = Y.bp(Y.white(0.05, f"ch{k}"), 2500, 9000)[:n] * np.exp(-tt * 80)
        Y.place(out, link, 0.05 + k * 0.08 + r.random() * 0.03, 0.4 + r.random() * 0.5)
    return Y.reverb(out, 2.0, 0.55, "chain")


def power_down():
    d = 3.0
    t = Y.t_axis(d)
    f0 = 120 * np.exp(-t * 0.9) + 20
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.exp(-t * 0.7)
    x += Y.lp(Y.white(d, "pd"), 300) * np.exp(-t * 2) * 0.6
    thunk = np.zeros_like(t)
    Y.place(thunk, Y.impact("pdthunk", 1.2), 0.0, 1.0)
    return Y.norm(Y.reverb(x + thunk * 0.8, 2.5, 0.5, "pd"), 0.9)


sound("ambient.level_1", [("ambient/level_1", warehouse_ambience)], stream=True)
sound("ambient.level_1.additions",
      [(f"ambient/level_1_drip{i}", drip(i)) for i in range(3)] +
      [(f"ambient/level_1_clang{i}", clang(i)) for i in range(2)] + [("ambient/level_1_chain", chain)],
      subtitle="Something in the warehouse")
sound("event.power_down", [("event/power_down", power_down)], subtitle="The power dies")

level("level_1", fog="#5f6468", sky="#5f6468", ambient="ambient.level_1", additions="ambient.level_1.additions",
      additions_chance=0.006, fixed_time=6000, height=96)
