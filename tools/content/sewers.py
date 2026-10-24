"""Sewers - old brick tunnels, a channel of sludge down the middle, narrow ledges either side, and
almost no light. Wading is slow. Something in the sludge takes hold of ankles."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, ladder, level, sound, custom, lang, \
    entity_model, entity_tex, part, ITEM_MODELS
from entity_paint import Canvas, skin
from content.ind_kit import anim_tex, text, vstreaks, rust, steel, grate, glow_spot
import tex as T
import synth as Y

S = 32


def brick(seed="swb", slime=0.0, dark=1.0):
    yy, xx = np.mgrid[0:S, 0:S]
    row = yy // 6
    off = (row % 2) * 7
    base = T.ramp(T.fbm(seed, octaves=3, base=2), "#4a2c1e", "#5e3a26", "#6a4430")
    per = T.rng(seed + "p").random((6, 3))
    base = T.shade(base, 0.8 + per[row % 6, ((xx + off) // 14) % 3] * 0.35)
    mortar = ((yy % 6) == 0) | (((xx + off) % 14) == 0)
    img = T.blend(base, T.solid("#2a221c"), mortar.astype(float) * 0.85)
    img = vstreaks(img, seed + "ef", "#a8a490", 3, 0.3, (6, 20))
    if slime:
        m = np.clip(T.fbm(seed + "sl", octaves=3, base=3) * 1.5 - (1.1 - slime), 0, 1)
        m = np.clip(m + np.clip((yy - 16) / 16.0, 0, 1) * slime * 0.8, 0, 1)
        img = T.blend(img, T.ramp(T.noise(seed + "slc", sigma=0.8), "#1e2a12", "#3a4a1e"), m * 0.85)
    return T.grain(T.shade(img, dark), seed, 0.05)


def ledge():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("swl", octaves=4, base=4), "#3e3a32", "#504a40")
    img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.35)
    sheen = T.noise("swls", sigma=2)
    img = T.stains(img, "swlst", "#1e2214", 2, 0.5, 8)
    return T.grain(T.shade(img, 0.88 + sheen * 0.2), "swl", 0.04)


def sludge(i, n=8):
    yy, xx = np.mgrid[0:S, 0:S]
    shift = int(i * S / n)
    base = np.roll(T.fbm("swsl", octaves=4, base=4), shift, axis=0)
    img = T.ramp(base, "#1e1a0e", "#3a3418", "#4a4420", "#5a5428")
    bits = np.roll((T.rng("swbits").random((S, S)) > 0.985).astype(float), shift, axis=0)
    img = T.blend(img, T.solid("#9a9070"), bits * 0.8)
    foam = np.roll(np.clip(T.noise("swfoam", sigma=1.2) * 2 - 1.4, 0, 1), shift, axis=0)
    img = T.blend(img, T.solid("#7a7450"), foam * 0.5)
    return img, np.full((S, S), 0.93)


def bed():
    return T.grain(T.ramp(T.fbm("swbed", octaves=3, base=3), "#141208", "#24200e"), "swbed", 0.05)


def plank():
    yy, xx = np.mgrid[0:S, 0:S]
    grain = np.sin(xx / 1.4 + T.noise("swpl", sigma=2) * 6) * 0.5 + 0.5
    img = T.ramp(grain * 0.4 + T.fbm("swplf", octaves=3, base=3) * 0.6, "#2a2014", "#40301c", "#4a3a22")
    img = T.shade(img, 1 - ((yy % 11) == 0) * 0.6)
    rot = np.clip(T.fbm("swplr", octaves=3, base=3) * 1.8 - 1.0, 0, 1)
    return T.blend(img, T.solid("#141008"), rot * 0.7)


def gate():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = rust(steel("swgate", "#3a3a36", "#4a4a44", brushed=False), "swgr", 0.7, "#4a2414", "#6a3418")
    a = (((xx % 6) < 2) | ((yy % 16) < 2) | (xx < 2) | (xx > 29)).astype(float)
    return rgb, a


def manhole():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = rust(steel("swmh", "#2a2a28", "#3a3a36", brushed=False), "swmhr", 0.4)
    holes = ((xx % 7 == 3) & (yy % 7 == 3)) & (d < 13)
    img = np.where(holes[..., None], T.solid("#fff4d8"), img)
    seep = np.clip(1 - np.abs(d - 14.5) / 1.0, 0, 1)
    img = T.blend(img, T.solid("#ffeec0"), seep)
    return img


def rungs():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = rust(steel("swr", "#3a3a36", "#4a4a44", brushed=False), "swrr", 0.6, "#4a2414", "#6a3418")
    a = (((yy % 8) < 2) & (xx > 4) & (xx < 27)) | (((xx < 6) | (xx > 25)) & ((yy % 8) < 4))
    return rgb, a.astype(float)


def outfall():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(T.noise("swof", sigma=2), "#030302", "#0a0806")
    dribble = (np.abs(xx - 15.5) < 2) & (yy > 15)
    img = np.where(dribble[..., None], T.solid("#3a3418"), img)
    return np.where((d >= 12)[..., None], brick("swofb", 0.6), img)


def lamp():
    return T.ramp(T.noise("swlamp", sigma=2), "#e8d090", "#fff0c8")


def lamp_cage():
    yy, xx = np.mgrid[0:S, 0:S]
    return T.solid("#1a1a18"), (((xx % 5) < 1) | ((yy % 5) < 1)).astype(float)


def scrawl(lines):
    def f():
        img = brick("swsc" + lines[0])
        y = 16 - len(lines) * 4
        for ln in lines:
            img = text(img, ln, 16 - (len(ln) * 4 - 1) // 2, y, "#c8c0a0")
            y += 8
        return img
    return f


def sump_hole():
    yy, xx = np.mgrid[0:S, 0:S]
    return T.ramp(T.noise("swsump", sigma=3), "#000000", "#06050a")


tex("sw_brick", lambda: brick())
tex("sw_brick_slime", lambda: brick("swbs", 0.7))
tex("sw_brick_dark", lambda: brick("swbd", 0.2, 0.75))
tex("sw_ledge", ledge)
anim_tex("sw_sludge", sludge, 8, frametime=4, interpolate=True, alpha=True)
tex("sw_bed", bed)
tex("sw_plank", plank)
tex("sw_gate", gate)
tex("sw_manhole", manhole)
tex("sw_rungs", rungs)
tex("sw_outfall", outfall)
tex("sw_lamp", lamp)
tex("sw_lamp_cage", lamp_cage)
tex("sw_scrawl_below", scrawl(["WE ARE", "BELOW"]))
tex("sw_scrawl_hands", scrawl(["NOT IN", "THE", "WATER"]))
tex("sw_sump", sump_hole)

cube("sw_brick", "sw_brick", display="Sewer Brick")
cube("sw_brick_slime", "sw_brick_slime", display="Slimed Brick")
cube("sw_brick_dark", "sw_brick_dark", display="Old Brick")
cube("sw_ledge", "sw_ledge", display="Sewer Ledge")
custom("sw_sludge", "com.noclip.block.SludgeBlock",
       {"parent": "minecraft:block/block", "textures": {"s": "noclip:block/sw_sludge", "particle": "noclip:block/sw_sludge"},
        "elements": [{"from": [0, 0, 0], "to": [16, 13, 16],
                      "faces": {"up": {"texture": "#s", "cullface": "up"}, "north": {"texture": "#s", "cullface": "north"},
                                "south": {"texture": "#s", "cullface": "south"}, "west": {"texture": "#s", "cullface": "west"},
                                "east": {"texture": "#s", "cullface": "east"}}}]},
       display="Sludge", sound="mud", unbreakable=True, render="translucent")
cube("sw_bed", "sw_bed", display="Silt", sound="mud")
shaped("sw_plank", [(0, 13, 1, 16, 16, 7, "p"), (0, 13, 9, 16, 16, 15, "p")], {"p": "sw_plank"},
       display="Rotten Planks", sound="wood", unbreakable=True)
exit_block("sw_gate", "sw_gate", "DOOR", trigger="USE", solid=True, render="cutout", display="Iron Gate",
           sound="metal")
exit_block("sw_manhole", "sw_manhole", "ASCEND", trigger="USE", solid=True, display="Manhole Cover", light=7,
           emissive=True, sound="metal")
ladder("sw_rungs", "sw_rungs", display="Iron Rungs", unbreakable=True)
cube("sw_outfall", "sw_outfall", display="Outfall")
shaped("sw_lamp", [(5, 4, 0, 11, 10, 4, {"all": "c", "south": "g"}), (6, 5, 0, 10, 9, 3, "g")],
       {"g": "sw_lamp", "c": "sw_lamp_cage"}, display="Old Lamp", light=11, emissive=True, collides=False,
       unbreakable=True, sound="glass")
cube("sw_scrawl_below", "sw_scrawl_below", display="Scrawl")
cube("sw_scrawl_hands", "sw_scrawl_hands", display="Scrawl")
exit_block("sw_sump", "sw_sump", "WATER", display="Sump")

# ------------------------------------------------------------------ entity: the lurker


def lurker_parts():
    return [
        part("body", (0, 14, 0), cubes=[(0, 16, -4, 0, -2, 8, 10, 4)]),
        part("head", (0, 14, 0), cubes=[(0, 0, -4, -8, -4, 8, 8, 8)]),
        part("right_arm", (-5, 15, 0), cubes=[(24, 16, -2, -1, -1.5, 3, 16, 3)]),
        part("left_arm", (5, 15, 0), cubes=[(36, 16, -1, -1, -1.5, 3, 16, 3)]),
    ]


def lurker_tex():
    c = Canvas()
    hide = skin("swlk", "#2a2a1a", "#3e3e26", sigma=0.6, spots="#5a5a3a")

    def face(w, h):
        img = hide(w, h)
        img[3, 1:3] = T.hexc("#e8e8c0")
        img[3, 5:7] = T.hexc("#e8e8c0")
        img[6, 2:6] = T.hexc("#0a0a06")
        return img

    c.box(0, 0, 8, 8, 8, all=hide, front=face)
    c.box(0, 16, 8, 10, 4, all=hide)
    c.box(24, 16, 3, 16, 3, all=hide)
    c.box(36, 16, 3, 16, 3, all=hide)
    return c.out()


entity_model("sludge_lurker", lurker_parts())
entity_tex("sludge_lurker", lurker_tex)
lang("entity.noclip.sludge_lurker", "Sludge Lurker")
lang("item.noclip.sludge_lurker_spawn_egg", "Sludge Lurker Spawn Egg")
ITEM_MODELS["sludge_lurker_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def sewer():
    d = 26.0
    t = Y.t_axis(d)
    trickle = Y.bp(Y.pink(d, "swtr"), 300, 2500) * 0.35 * (0.8 + 0.2 * np.sin(2 * np.pi * t / 9))
    gurgle = Y.bp(Y.white(d, "swgu"), 200, 800) * (0.5 + 0.5 * np.sin(2 * np.pi * 2.3 * t + 3 * np.sin(2 * np.pi * 0.3 * t))) ** 3 * 0.4
    room = Y.lp(Y.brown(d, "swroom"), 60) * 0.7
    out = np.zeros(len(t))
    r = Y.rng("swdrips")
    for k in range(30):
        n = int(0.12 * Y.SR)
        tt = np.arange(n) / Y.SR
        fr = 1100 + r.random() * 900 - tt * 2500
        Y.place(out, np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 35), 0.5 + r.random() * (d - 1), 0.1 + r.random() * 0.25)
    x = trickle + gurgle + room + Y.reverb(out, 2.0, 0.7, "swdr")[:len(t)]
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 2.0), 0.0015), "stereo"


def squeak(i):
    def f():
        out = np.zeros(int(1.5 * Y.SR))
        r = Y.rng(f"swsq{i}")
        for k in range(2 + i):
            d = 0.08 + r.random() * 0.06
            t = Y.t_axis(d)
            f0 = 3200 + r.random() * 1500 + 800 * np.sin(np.pi * t / d)
            Y.place(out, np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * t / d), 0.05 + k * 0.17, 0.4)
        return Y.norm(Y.reverb(out, 1.5, 0.5, f"swsq{i}"), 0.35)
    return f


def splash(i):
    def f():
        d = 2.5
        t = Y.t_axis(d)
        x = Y.bp(Y.white(d, f"swsp{i}"), 200, 3000) * np.exp(-t * 5) * (1 - np.exp(-t * 50))
        x += Y.lp(Y.white(d, f"swsp2{i}"), 300) * np.exp(-t * 3) * 0.6
        return Y.norm(Y.reverb(x, 2.5, 0.6, f"swsp{i}"), 0.55)
    return f


def grab():
    d = 1.6
    t = Y.t_axis(d)
    slosh = Y.lp(Y.white(d, "swgrab"), 700) * np.exp(-t * 4)
    squelch = Y.bp(Y.white(d, "swgrab2"), 300, 1500) * np.exp(-((t - 0.2) ** 2) / 0.005) * 0.8
    return Y.norm(slosh + squelch, 0.9)


def surface():
    d = 2.5
    t = Y.t_axis(d)
    out = np.zeros(len(t))
    r = Y.rng("swsurf")
    for k in range(10):
        n = int(0.1 * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 200 + r.random() * 300 + 400 * tt
        Y.place(out, np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.exp(-tt * 20), 0.1 + k * 0.15 + r.random() * 0.05, 0.4 + r.random() * 0.4)
    return Y.norm(Y.reverb(out + Y.lp(Y.white(d, "swsurf2"), 400) * 0.2 * np.sin(np.pi * t / d), 1.5, 0.4, "swsurf"), 0.6)


def wade():
    d = 0.6
    t = Y.t_axis(d)
    return Y.norm(Y.lp(Y.white(d, "swwade"), 900) * np.exp(-t * 6) * (1 - np.exp(-t * 40)), 0.5)


sound("ambient.sewers", [("ambient/sewers", sewer)], stream=True)
sound("ambient.sewers.additions",
      [(f"ambient/sewers_squeak{i}", squeak(i)) for i in range(2)] + [(f"ambient/sewers_splash{i}", splash(i)) for i in range(2)],
      subtitle="Something moves in the sewer")
sound("event.sewers_grab", [("event/sewers_grab", grab)], subtitle="Something grabs your ankle")
sound("event.sewers_wade", [("event/sewers_wade", wade)], subtitle="Wading")
sound("entity.sludge_lurker.surface", [("entity/sludge_lurker_surface", surface)], subtitle="Bubbles rise from the sludge")

level("sewers", fog="#0c0e08", sky="#0c0e08", ambient="ambient.sewers", additions="ambient.sewers.additions",
      additions_chance=0.006, fixed_time=18000, height=48, particle="minecraft:dripping_water", particle_chance=0.003)
