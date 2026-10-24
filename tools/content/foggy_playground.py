"""Foggy Playground. Playgrounds in a fog so thick you can only see the next swing set.
The swings are moving. There is no wind. Every so often, somewhere, a music box counts
to ten. Hide."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, exit_block, pane, level, sound, lang, entity_model, entity_tex, \
    ITEM_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import draw_text, centered, paint_metal
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32
tx = A.tex


def chips():
    img = T.ramp(T.noise("fpch", sigma=0.5), "#5a4430", "#7a5c40", "#8e6e4c")
    r = T.rng("fpchips")
    for _ in range(60):
        x, y = int(r.random() * S), int(r.random() * S)
        ln = 2 + int(r.random() * 3)
        horiz = r.random() < 0.5
        for k in range(ln):
            px, py = (x + k) % S if horiz else x, y if horiz else (y + k) % S
            img[py, px] = T.hexc("#a8885c") * (0.8 + r.random() * 0.3)
    return T.grain(img, "fpch", 0.05)


def rubber():
    img = T.ramp(T.noise("fprub", sigma=0.6), "#3a5a6a", "#46687a")
    img = T.border(img, "#2a3a44", 1)
    return T.grain(img, "fprub", 0.04)


def grass():
    img = T.ramp(T.fbm("fpgr", octaves=4, base=4) * 0.6 + T.noise("fpgr2", sigma=0.4) * 0.4, "#4a5a3a", "#5a6a46", "#6a7a52")
    return T.grain(img, "fpgr", 0.05)


def soil():
    return T.grain(T.ramp(T.fbm("fpsoil", octaves=3, base=3), "#3a3026", "#4a3e30"), "fpsoil", 0.05)


def path():
    img = T.ramp(T.fbm("fppath", octaves=3, base=4), "#8a8680", "#9a968e")
    img = T.border(img, "#6a6660", 1, sides="nw")
    return T.grain(img, "fppath", 0.03)


def sand():
    return T.grain(T.ramp(T.noise("fpsand", sigma=0.5), "#b8a888", "#c8b898"), "fpsand", 0.05)


tx("fp_chips", chips)
tx("fp_rubber", rubber)
tx("fp_grass", grass)
tx("fp_soil", soil)
tx("fp_path", path)
tx("fp_sand", sand)
tx("fp_grass_side", lambda: T.blend(soil(), grass(), (np.mgrid[0:S, 0:S][0] < 5).astype(float)))
cube("fp_chips", "fp_chips", sound="gravel", display="Wood Chips")
cube("fp_rubber", "fp_rubber", sound="wool", display="Rubber Matting")
cube_faces("fp_grass", top="fp_grass", bottom="fp_soil", side="fp_grass_side", sound="grass", display="Park Grass")
cube("fp_soil", "fp_soil", sound="gravel", display="Soil")
cube("fp_path", "fp_path", display="Park Path")
cube("fp_sand", "fp_sand", sound="sand", display="Sandbox Sand")


def painted(c, seed):
    def f():
        img = T.ramp(T.noise(seed, sigma=1.5), c, c)
        img = T.shade(img, 0.9 + T.noise(seed + "b", sigma=1) * 0.15)
        chips_ = (T.noise(seed + "c", sigma=0.6) > 0.82)
        img[chips_] = T.hexc("#6a6a66")
        rust = T.noise(seed + "r", sigma=1.2) > 0.86
        img[rust] = T.hexc("#7a4a2a")
        return T.grain(img, seed, 0.03)
    return f


for name, c in (("red", "#a8382c"), ("blue", "#2c5a98"), ("yellow", "#c8a032"), ("green", "#3a7a44")):
    tx(f"fp_paint_{name}", painted(c, "fpp" + name))
tx("fp_chain", lambda: (np.ones((S, S, 3)) * T.hexc("#8a8a86"),
                        ((np.abs(np.mgrid[0:S, 0:S][1] - 15.5) < 1.5) & (np.mgrid[0:S, 0:S][0] % 4 != 0)).astype(float)))
tx("fp_seat", lambda: T.grain(T.solid("#1a1a1c"), "fpseat", 0.05))

# swing set frame: a top bar along x on A-frame legs
shaped("fp_swing_bar", [(0, 13, 7, 16, 16, 9, "p")], {"p": "fp_paint_red"}, display="Swing Bar", unbreakable=True,
       collides=False)
shaped("fp_swing_leg", [(6, 0, 6, 10, 16, 10, "p")], {"p": "fp_paint_red"}, display="Swing Frame", unbreakable=True)
shaped("fp_swing_apex", [(6, 0, 6, 10, 16, 10, "p"), (0, 13, 7, 16, 16, 9, "p")], {"p": "fp_paint_red"},
       display="Swing Frame", unbreakable=True)

# A swing in three positions, each one block whose model hangs a block below it and
# rotates as one rigid piece about the bar above.
from assets import custom
from content.outside_kit import box_model
for pos, ang in (("back", -22.5), ("mid", 0), ("fwd", 22.5)):
    rot = ("x", ang, (8, 30, 8)) if ang else None
    boxes = [(3, -12, 7.5, 3.6, 30, 8.5, "c!", rot), (12.4, -12, 7.5, 13, 30, 8.5, "c!", rot),
             (2.5, -13, 5.5, 13.5, -12, 10.5, "s", rot)]
    custom(f"fp_swing_{pos}", "net.minecraft.world.level.block.Block",
           box_model(boxes, {"c": "fp_chain", "s": "fp_seat"}), display="Swing", unbreakable=True,
           no_collision=True, render="cutout", sound="chain")


def deck():
    img = painted("#c8a032", "fpdeck")()
    yy, xx = np.mgrid[0:S, 0:S]
    holes = ((xx % 6) < 2) & ((yy % 6) < 2)
    img[holes] = T.hexc("#3a3020")
    return img


tx("fp_deck", deck)
shaped("fp_deck", [(0, 13, 0, 16, 16, 16, {"all": "d"})], {"d": "fp_deck"}, display="Platform Deck", unbreakable=True)
shaped("fp_post", [(6, 0, 6, 10, 16, 10, "p")], {"p": "fp_paint_blue"}, display="Play Structure Post", unbreakable=True)
shaped("fp_slide", [(1, 0, 0, 15, 2, 16, "s"), (0, 0, 0, 1, 5, 16, "s"), (15, 0, 0, 16, 5, 16, "s")],
       {"s": "fp_paint_yellow"}, display="Slide", unbreakable=True)
shaped("fp_rail", [(0, 0, 7, 16, 1.5, 9, "p"), (0, 10, 7, 16, 11.5, 9, "p"), (2, 0, 7.5, 3, 11, 8.5, "p"),
                   (13, 0, 7.5, 14, 11, 8.5, "p")], {"p": "fp_paint_green"}, display="Railing", unbreakable=True)


def tube():
    img = painted("#2c5a98", "fptube")()
    yy = np.mgrid[0:S, 0:S][0]
    return T.shade(img, 1 - ((yy % 16) < 2) * 0.25)


tx("fp_tube", tube)
shaped("fp_tube", [(0, 0, 0, 2, 16, 16, "t"), (14, 0, 0, 16, 16, 16, "t"), (0, 0, 0, 16, 2, 16, "t"),
                   (0, 14, 0, 16, 16, 16, "t")], {"t": "fp_tube"}, display="Tube Slide", unbreakable=True)
tx("fp_tube_dark", lambda: T.blend(T.solid("#020204"), T.solid("#0a1428"), T.noise("fptd", sigma=3) * 0.4))
exit_block("fp_tube_end", "fp_tube_dark", "DESCEND", display="The End of the Tube")

# merry-go-round
tx("fp_mgr_top", lambda: T.border(T.blend(painted("#a8382c", "fpmgr")(), T.solid("#c8a032"),
                                          (((np.mgrid[0:S, 0:S][1] + np.mgrid[0:S, 0:S][0]) // 8) % 2 == 0)[..., None] * np.ones(3) * 0.8), "#5a5a56", 1))
shaped("fp_mgr_deck", [(0, 0, 0, 16, 4, 16, {"all": "p", "up": "t"})], {"p": "fp_paint_red", "t": "fp_mgr_top"},
       display="Merry-Go-Round", unbreakable=True)
shaped("fp_mgr_hub", [(0, 0, 0, 16, 4, 16, {"all": "p", "up": "t"}), (6, 4, 6, 10, 14, 10, "y"), (2, 13, 7, 14, 14, 9, "y")],
       {"p": "fp_paint_red", "t": "fp_mgr_top", "y": "fp_paint_yellow"}, display="Merry-Go-Round", unbreakable=True)
shaped("fp_mgr_handle", [(0, 0, 0, 16, 4, 16, {"all": "p", "up": "t"}), (7, 4, 7, 9, 12, 9, "y"), (2, 11, 7, 14, 12.5, 9, "y")],
       {"p": "fp_paint_red", "t": "fp_mgr_top", "y": "fp_paint_yellow"}, display="Merry-Go-Round", unbreakable=True)

# spring rider and seesaw
shaped("fp_spring_duck", [(7, 0, 7, 9, 5, 9, "c"), (5, 5, 3, 11, 10, 13, "y"), (6, 9, 1, 10, 14, 5, "y"),
                          (7, 11, 0, 9, 12, 1, "r")], {"c": "fp_chain", "y": "fp_paint_yellow", "r": "fp_paint_red"},
       display="Spring Rider", unbreakable=True)
shaped("fp_seesaw_mid", [(6, 0, 6, 10, 6, 10, "p"), (7, 5, 0, 9, 7, 16, "b")], {"p": "fp_paint_red", "b": "fp_paint_blue"},
       display="Seesaw", unbreakable=True)
shaped("fp_seesaw_up", [(7, 7, 0, 9, 9, 16, "b"), (5, 9, 2, 11, 10, 8, "s")], {"b": "fp_paint_blue", "s": "fp_seat"},
       display="Seesaw", unbreakable=True, collides=False)
shaped("fp_seesaw_down", [(7, 1, 0, 9, 3, 16, "b"), (5, 3, 8, 11, 4, 14, "s")], {"b": "fp_paint_blue", "s": "fp_seat"},
       display="Seesaw", unbreakable=True, collides=False)


def bars():
    rgb = np.ones((S, S, 3)) * T.hexc("#3a7a44")
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((xx < 3) | (xx > 28) | (yy < 3) | (yy > 28) | (np.abs(yy - 15.5) < 1.5)).astype(float)
    return rgb, a


tx("fp_bars", bars)
cube("fp_climber", "fp_bars", display="Jungle Gym", render="cutout", sound="metal")


def fence_tex():
    rgb = np.ones((S, S, 3)) * T.hexc("#8a8e90")
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx + yy) % 8 == 0) | ((xx - yy) % 8 == 0)).astype(float)
    return rgb, a


tx("fp_fence", fence_tex)
pane("fp_fence", "fp_fence", display="Playground Fence")


def bucket():
    return painted("#c8382c", "fpbuck")()


tx("fp_bucket", bucket)
shaped("fp_pail", [(5, 0, 5, 11, 5, 11, {"all": "b", "up": None}), (10, 0, 3, 14, 1, 13, "y")],
       {"b": "fp_bucket", "y": "fp_paint_yellow"}, display="Sand Pail", unbreakable=True, collides=False)
tx("fp_bench_wood", lambda: T.grain(T.ramp(T.fbm("fpbw", octaves=3, base=2), "#5a4430", "#6a5038"), "fpbw", 0.04))
shaped("fp_bench", [(1, 0, 4, 2, 7, 12, "w"), (14, 0, 4, 15, 7, 12, "w"), (0, 6, 4, 16, 7, 11, "w"),
                    (0, 7, 11, 16, 14, 12, "w")], {"w": "fp_bench_wood"}, display="Park Bench", unbreakable=True)


def bare_branches():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("fpbare")
    for _ in range(10):
        x, y = 16.0, S - 1.0
        ang = (r.random() - 0.5) * 2.4
        for k in range(26):
            ang += (r.random() - 0.5) * 0.3
            x += np.sin(ang)
            y -= np.cos(ang)
            if 0 <= int(x) < S and 0 <= int(y) < S:
                rgb[int(y), int(x)] = T.hexc("#2a2420")
                a[int(y), int(x)] = 1
    return rgb, a


tx("fp_branches", bare_branches)
cross("fp_branches", "fp_branches", display="Bare Branches")
tx("fp_bark", lambda: T.grain(T.ramp(T.fbm("fpbark", octaves=3, base=2), "#2a2420", "#3a3028"), "fpbark", 0.05))
cube("fp_trunk", "fp_bark", display="Tree Trunk", sound="wood")

# ------------------------------------------------------------------ the seeker


def seeker_tex():
    c = Canvas()
    hood = lambda w, h: T.grain(T.ramp(T.noise(f"fpsh{w}{h}", shape=(h, w), sigma=1), "#8a2a2a", "#9a3434"), f"fpsh{w}", 0.04)

    def face(w, h):
        img = hood(w, h)
        img[2:7, 1:7] = T.hexc("#050505")
        return img

    def legs(w, h):
        img = T.grain(T.solid("#3a4a6a", shape=(h, w)), f"fpsl{w}", 0.05)
        img[-2:] = T.hexc("#e8e4dc")
        return img

    def arms(w, h):
        img = hood(w, h)
        img[-2:] = T.hexc("#d8c8b0")
        return img

    paint_humanoid(c, hood, hood, arms, legs, face=face)
    return c.out()


entity_model("fp_seeker", humanoid(arm_len=12, leg_len=12))
entity_tex("fp_seeker", seeker_tex)
lang("entity.noclip.fp_seeker", "The Seeker")
lang("item.noclip.fp_seeker_spawn_egg", "The Seeker Spawn Egg")
ITEM_MODELS["fp_seeker_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def fog_loop():
    d = 24.0
    t = Y.t_axis(d)
    air = Y.lp(Y.pink(d, "fpair"), 400) * 0.25
    drip = np.zeros_like(t)
    r = Y.rng("fpdrip")
    for k in range(8):
        n = int(0.1 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(drip, np.sin(2 * np.pi * (1500 + r.random() * 800) * tt) * np.exp(-tt * 45) * 0.08, r.random() * (d - 0.2))
    creak = np.zeros_like(t)
    for k in range(int(d / 2.4)):
        n = int(0.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        f = 380 + 60 * np.sin(np.pi * tt / 0.5)
        c = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)), 400, 2500) * np.sin(np.pi * tt / 0.5) * 0.04
        Y.place(creak, c, k * 2.4 + 0.3)
    x = Y.loopable(Y.norm(air + Y.reverb(drip, 1.5, 0.6, "fpd")[:len(t)] + Y.reverb(creak, 2.0, 0.7, "fpc")[:len(t)], 0.4), 1.5)
    return Y.stereoize(x, 0.0018), "stereo"


def laugh():
    out = np.zeros(int(3.0 * Y.SR))
    for k in range(5):
        d = 0.16
        tt = Y.t_axis(d)
        f = 520 + 40 * k - 300 * tt
        ha = (np.sin(2 * np.pi * np.cumsum(f) / Y.SR) + Y.bp(Y.white(d, f"fpl{k}"), 800, 3000) * 0.6) * np.sin(np.pi * tt / d)
        Y.place(out, ha, 0.2 + k * 0.2, 0.5 - k * 0.05)
    return Y.reverb(Y.lp(Y.norm(out, 0.3), 3500), 3.0, 0.85, "fplaugh")


def chain_rattle():
    out = np.zeros(int(1.6 * Y.SR))
    r = Y.rng("fpchain")
    for k in range(10):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.05, f"fpc{k}"), 2500, 9000)[:n] * np.exp(-tt * 80), 0.05 + k * 0.1 + r.random() * 0.03, 0.4)
    return Y.reverb(Y.norm(out, 0.4), 1.5, 0.6, "fpchain")


def ball_bounce():
    out = np.zeros(int(3.0 * Y.SR))
    t_at, g = 0.05, 1.0
    for k in range(7):
        Y.place(out, Y.impact(f"fpball{k}", 0.15), t_at, g)
        t_at += 0.5 * (0.75 ** k)
        g *= 0.7
    return Y.reverb(Y.norm(out, 0.4), 2.0, 0.7, "fpball")


def countdown():
    """A music box picks out ten notes, slower and slower, then stops."""
    out = np.zeros(int(9.0 * Y.SR))
    notes = [76, 74, 72, 71, 69, 67, 65, 64, 62, 60]
    at = 0.1
    for i, nt in enumerate(notes):
        f = 440 * 2 ** ((nt - 69) / 12)
        n = int(1.2 * Y.SR)
        tt = np.arange(n) / Y.SR
        tone = np.sin(2 * np.pi * f * tt) * np.exp(-tt * 4) + 0.3 * np.sin(2 * np.pi * f * 3 * tt) * np.exp(-tt * 8)
        Y.place(out, tone, at, 0.5)
        at += 0.55 + i * 0.06
    return Y.reverb(Y.norm(out, 0.5), 2.5, 0.6, "fpcount")


def found():
    d = 1.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "fpfound"), 600, 4000) * np.exp(-t * 3)
    x += np.sin(2 * np.pi * np.cumsum(900 - 400 * t) / Y.SR) * np.exp(-t * 2) * 0.6
    return Y.norm(x, 0.8)


sound("ambient.foggy_playground", [("ambient/foggy_playground", fog_loop)], stream=True)
sound("ambient.foggy_playground.additions",
      [("ambient/foggy_playground_laugh", laugh), ("ambient/foggy_playground_chain", chain_rattle),
       ("ambient/foggy_playground_ball", ball_bounce)],
      subtitle="Somebody playing in the fog")
sound("event.foggy_playground_count", [("event/foggy_playground_count", countdown)], subtitle="A music box counts to ten")
sound("event.foggy_playground_found", [("event/foggy_playground_found", found)], subtitle="Found you")

level("foggy_playground", fog="#c4c8c6", sky="#c4c8c6", ambient="ambient.foggy_playground",
      additions="ambient.foggy_playground.additions", additions_chance=0.003, fixed_time=1500, skylight=True,
      ceiling=False, height=64)
