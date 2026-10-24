"""The Bunker - green-and-cream military corridors, bunk rooms, map rooms, blast doors. Sometimes the
alarm starts: the lights go red, the blast doors close, and the drill begins."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, ladder, level, sound, custom, lang, \
    entity_model, entity_tex, ITEM_MODELS, BLOCK_MODELS
from entity_paint import Canvas, flat
from content.entities_core import humanoid, paint_humanoid
from content.ind_kit import text, vstreaks, rust, steel, concrete, box_model, facing_states, rivets, hazard_stripes
import tex as T
import synth as Y

S = 32
OLIVE = ("#3e4a32", "#4a5838")
CREAM = ("#c8c2a8", "#d6d0b4")


def paint(seed, cols, chips=0.2):
    img = T.ramp(T.noise(seed, sigma=2.5) * 0.6 + T.fbm(seed + "f", octaves=3, base=4) * 0.4, *cols)
    chip = (T.noise(seed + "c", sigma=0.7) > 1 - chips * 0.4).astype(float)
    img = T.blend(img, concrete(seed + "u", "#6a6a64", "#7a7a72", stains=0), chip * 0.8)
    return T.grain(img, seed, 0.03)


def wall_low():
    img = paint("bkwl", OLIVE)
    yy = np.arange(S)[:, None] * np.ones((1, S))
    img = T.shade(img, 1 - (yy > 27) * 0.35)
    return vstreaks(img, "bkwlv", "#2a3020", 2, 0.25, (6, 14))


def wall_stripe():
    img = paint("bkws", OLIVE)
    yy = np.arange(S)[:, None] * np.ones((1, S))
    img = np.where((yy < 8)[..., None], paint("bkwsc", CREAM), img)
    img = np.where(((yy >= 8) & (yy < 12))[..., None], paint("bkwsr", ("#8a1e18", "#9a2a20")), img)
    return img


def wall_high():
    return vstreaks(paint("bkwh", CREAM, 0.15), "bkwhv", "#8a8470", 3, 0.2, (8, 24))


def stencil(lines, col="#1e2418", bg=None):
    def f():
        img = wall_stripe() if bg is None else paint("bkst" + lines[0], bg)
        y = 14 if bg is None else 16 - len(lines) * 4
        for ln in lines:
            w = len(ln) * 4 - 1
            img = text(img, ln, 16 - w // 2, y, col)
            y += 7
        return img
    return f


def ceiling():
    img = concrete("bkc", "#5a5a54", "#6a6a62", stains=2)
    yy, xx = np.mgrid[0:S, 0:S]
    conduit = (np.abs(xx - 10) < 1.5)
    img = T.blend(img, T.solid("#7a7a72"), conduit.astype(float))
    img = T.shade(img, 1 - (np.abs(xx - 12) < 1) * 0.3)
    return img


def floor():
    yy, xx = np.mgrid[0:S, 0:S]
    a = T.ramp(T.noise("bkf1", sigma=1.5), "#5a6052", "#666c5e")
    b = T.ramp(T.noise("bkf2", sigma=1.5), "#4e5448", "#5a6052")
    chk = (((xx // 16) + (yy // 16)) % 2 == 0)
    img = np.where(chk[..., None], a, b)
    img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.25)
    return T.grain(T.stains(img, "bkfs", "#3a3e34", 2, 0.3, 7), "bkf", 0.03)


def blast_door():
    yy, xx = np.mgrid[0:S, 0:S]
    img = steel("bkbd", "#5a5e56", "#6e7268", brushed=False)
    img = rivets(img, [(x, y) for x in (3, 28) for y in range(3, 32, 5)] + [(x, y) for y in (3, 28) for x in range(8, 26, 5)])
    img = T.border(img, "#3a3e38", 1)
    img = T.blend(img, T.solid("#c8a020"), ((yy > 13) & (yy < 18)).astype(float) * 0.85)
    img = text(img, "SEALED", 4, 14, "#1a1a14")
    return rust(img, "bkbdr", 0.2)


def frame():
    img = steel("bkfr", "#3a3e38", "#4a4e46", brushed=False)
    return hazard_stripes(img, 0, 32, "bkfrh", width=5, worn=0.2)


def bunk_metal():
    return steel("bkbm", "#3a4232", "#4a5440", brushed=False)


def blanket():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("bkbl", sigma=1), "#4a5038", "#5a6044")
    img = T.shade(img, 1 - ((yy % 10) == 0) * 0.15)
    return T.stains(img, "bkbls", "#2a3020", 2, 0.4, 6)


def locker():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("bklk", sigma=2), "#5a6450", "#66705c")
    vents = ((yy % 3) == 0) & (yy < 9) & (xx > 8) & (xx < 24)
    img = T.shade(img, 1 - vents * 0.5)
    img = T.border(img, "#3a4232", 1)
    img = T.rect(img, 24, 14, 27, 20, "#2a2a2a")
    img = text(img, "17", 11, 22, "#e0dcc8")
    return rust(img, "bklkr", 0.15)


def radio(i):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("bkrd", sigma=1.5), "#4a5040", "#565c4a")
    img = T.border(img, "#2a2e26", 2)
    for k, cx in enumerate((9, 23)):
        d = np.sqrt((xx - cx) ** 2 + (yy - 11) ** 2)
        img = T.blend(img, T.solid("#e0d8a8" if (i + k) % 2 == 0 else "#a89a60"), (d < 5).astype(float))
        ang = (i * 0.9 + k * 1.7)
        for r in range(5):
            img[int(11 - np.sin(ang) * r), int(cx + np.cos(ang) * r)] = T.hexc("#1a1a1a")
    for k in range(6):
        on = ((i + k) % 3) != 0
        img[24:26, 5 + k * 4:7 + k * 4] = T.hexc("#ff4020" if on and k % 2 else "#40ff60" if on else "#2a2a2a")
    return img


def table_top():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("bktt", sigma=1), "#4a5440", "#56604a")
    return T.border(img, "#2e3428", 1)


def map_slice(kind, i, j, nx, ny):
    """A big wall map spanning nx x ny blocks."""
    def f():
        W, H = S * nx, S * ny
        yy, xx = np.mgrid[0:H, 0:W]
        if kind == "topo":
            n = T.fbm("bkmap", shape=(H, W), octaves=4, base=8)
            img = T.ramp(n, "#c8c0a0", "#b8b08a", "#a8a07a")
            contours = (np.abs((n * 12) % 1 - 0.5) < 0.06)
            img = T.blend(img, T.solid("#7a6a4a", shape=(H, W)), contours.astype(float) * 0.7)
            river = np.abs(yy - (H / 2 + np.sin(xx / 14.0) * 10)) < 1.2
            img = T.blend(img, T.solid("#4a6a8a", shape=(H, W)), river.astype(float))
            r = T.rng("bkpins")
            for _ in range(9):
                px, py = int(r.random() * (W - 4)) + 2, int(r.random() * (H - 4)) + 2
                img[py - 1:py + 2, px - 1:px + 2] = T.hexc("#c01818")
        else:
            # a map of the Backrooms: levels as boxes joined by lines, most of it crossed out
            img = T.ramp(T.noise("bkbmap", shape=(H, W), sigma=3), "#d8d2b8", "#e4dec4")
            from content.ind_kit import text as tx
            labs = ["0", "1", "2", "3", "4", "6", "?", "37", "!", "FUN"]
            r = T.rng("bkbm")
            pts = []
            for k, lab in enumerate(labs):
                px = 8 + (k % 5) * (W - 16) // 4
                py = 12 + (k // 5) * (H - 28)
                px += int(r.random() * 6) - 3
                pts.append((px, py))
                img[py - 6:py + 7, px - 9:px + 10] = T.hexc("#2a2a24")
                img[py - 5:py + 6, px - 8:px + 9] = T.hexc("#e8e2c8")
                img = tx(img, lab, px - (len(lab) * 4 - 1) // 2, py - 2, "#1a1a14")
            for a, b in ((0, 1), (1, 2), (2, 3), (1, 6), (3, 4), (0, 9), (4, 8), (6, 7), (5, 6), (8, 9)):
                (x1, y1), (x2, y2) = pts[a], pts[b]
                for t in np.linspace(0, 1, 120):
                    x, y = int(x1 + (x2 - x1) * t), int(y1 + (y2 - y1) * t)
                    if 0 <= y < H and 0 <= x < W and img[y, x].sum() > 2.0:
                        img[y, x] = T.hexc("#3a3a30")
            for (px, py) in pts[5:8]:
                for t in range(-8, 9):
                    for o in (0, 1):
                        y, x = py + t // 2 + o, px + t
                        if 0 <= y < H and 0 <= x < W:
                            img[y, x] = T.hexc("#b01810")
            img = tx(img, "DO NOT FOLLOW", 4, H - 9, "#b01810")
        img = T.grain(img, "bkmapg" + kind, 0.03)
        piece = img[(ny - 1 - j) * S:(ny - j) * S, i * S:(i + 1) * S]
        return T.border(piece, "#3a3020", 1, ("n" if j == ny - 1 else "") + ("s" if j == 0 else "") +
                        ("w" if i == 0 else "") + ("e" if i == nx - 1 else ""))
    return f


def bulb_glass():
    return T.ramp(T.noise("bkbulb", sigma=2), "#ffd890", "#fff2c8")


def cage():
    yy, xx = np.mgrid[0:S, 0:S]
    return T.solid("#2a2e26"), (((xx % 6) < 1) | ((yy % 6) < 1)).astype(float)


def beacon(i, n):
    yy, xx = np.mgrid[0:S, 0:S]
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    sweep = np.cos(ang - 2 * np.pi * i / n) * 0.5 + 0.5
    return T.border(T.ramp(sweep ** 3, "#5a0806", "#ff2a1a", "#fff0e0"), "#2a2a2a", 2)


def hatch():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = steel("bkhatch", "#4a5040", "#5a6050", brushed=False)
    img = T.blend(img, T.solid("#2a2e26"), (np.abs(d - 12) < 1).astype(float))
    spokes = (np.abs(xx - 15.5) < 1) | (np.abs(yy - 15.5) < 1)
    img = T.blend(img, T.solid("#8a1e18"), (spokes & (d < 6)).astype(float))
    seep = np.clip(1 - np.abs(d - 13.2) / 1.0, 0, 1)
    return T.blend(img, T.solid("#fff4d0"), seep * 0.9)


def steel_door():
    img = steel("bksd", "#4a5040", "#5a6050", brushed=False)
    img = T.border(img, "#2a2e26", 2)
    img = T.rect(img, 22, 14, 27, 18, "#a8a090")
    img = T.rect(img, 6, 4, 26, 10, "#c8c2a8")
    img = text(img, "SECT 7", 4, 5, "#1a1a14")
    return rust(img, "bksdr", 0.2)


tex("bk_wall_low", wall_low)
tex("bk_wall_stripe", wall_stripe)
tex("bk_wall_high", wall_high)
tex("bk_stencil_sector", stencil(["SECTOR", "4"]))
tex("bk_stencil_air", stencil(["AIRLOCK"]))
tex("bk_stencil_nosmoke", stencil(["NO", "SMOKING"], "#e8e2c8", ("#8a1e18", "#9a2a20")))
tex("bk_stencil_masks", stencil(["MASKS", "ON AT", "ALARM"], "#1e2418", CREAM))
tex("bk_ceiling", ceiling)
tex("bk_floor", floor)
tex("bk_blast_door", blast_door)
tex("bk_frame", frame)
tex("bk_bunk_metal", bunk_metal)
tex("bk_blanket", blanket)
tex("bk_locker", locker)
from content.ind_kit import anim_tex
anim_tex("bk_radio", radio, 4, frametime=8)
tex("bk_table", table_top)
for i in range(3):
    for j in range(2):
        tex(f"bk_map_topo_{i}{j}", map_slice("topo", i, j, 3, 2))
        tex(f"bk_map_br_{i}{j}", map_slice("backrooms", i, j, 3, 2))
tex("bk_bulb", bulb_glass)
tex("bk_cage", cage)
anim_tex("bk_beacon", lambda i: beacon(i, 6), 6, frametime=1)
tex("bk_beacon_off", lambda: T.border(T.ramp(T.noise("bkbo", sigma=2), "#3a0a08", "#4a1210"), "#2a2a2a", 2))
tex("bk_hatch", hatch)
tex("bk_steel_door", steel_door)

cube("bk_wall_low", "bk_wall_low", display="Painted Bunker Wall")
cube("bk_wall_stripe", "bk_wall_stripe", display="Painted Bunker Wall")
cube("bk_wall_high", "bk_wall_high", display="Painted Bunker Wall")
for n in ("sector", "air", "nosmoke", "masks"):
    cube(f"bk_stencil_{n}", f"bk_stencil_{n}", display="Stencilled Wall")
cube("bk_ceiling", "bk_ceiling", display="Bunker Ceiling", flat=True)
cube("bk_floor", "bk_floor", display="Linoleum")
cube("bk_blast_door", "bk_blast_door", display="Blast Door", sound="metal")
cube("bk_frame", "bk_frame", display="Blast Door Frame", sound="metal")
shaped("bk_bunk", [(0, 0, 0, 1, 16, 1, "m"), (15, 0, 0, 16, 16, 1, "m"), (0, 0, 15, 1, 16, 16, "m"),
                   (15, 0, 15, 16, 16, 16, "m"), (0, 3, 0, 16, 4, 16, "m"), (1, 4, 1, 15, 6, 15, "b"),
                   (0, 12, 0, 16, 13, 16, "m"), (1, 13, 1, 15, 15, 15, "b")],
       {"m": "bk_bunk_metal", "b": "bk_blanket"}, display="Bunk", sound="metal", unbreakable=True)
custom("bk_locker", "com.noclip.block.FacingCubeBlock",
       {"parent": "minecraft:block/orientable", "textures": {"front": "noclip:block/bk_locker",
                                                             "side": "noclip:block/bk_bunk_metal",
                                                             "top": "noclip:block/bk_bunk_metal"}},
       blockstate=facing_states(lambda v: "bk_locker"), display="Locker", sound="metal", unbreakable=True)
custom("bk_radio", "com.noclip.block.FacingCubeBlock",
       {"parent": "minecraft:block/orientable", "textures": {"front": "noclip:block/bk_radio",
                                                             "side": "noclip:block/bk_bunk_metal",
                                                             "top": "noclip:block/bk_bunk_metal"}},
       blockstate=facing_states(lambda v: "bk_radio"), display="Radio Set", sound="metal", unbreakable=True,
       light=5)
shaped("bk_table", [(0, 13, 0, 16, 15, 16, {"all": "m", "up": "t"}), (1, 0, 1, 3, 13, 3, "m"),
                    (13, 0, 1, 15, 13, 3, "m"), (1, 0, 13, 3, 13, 15, "m"), (13, 0, 13, 15, 13, 15, "m")],
       {"m": "bk_bunk_metal", "t": "bk_table"}, display="Field Table", sound="metal", unbreakable=True)
for i in range(3):
    for j in range(2):
        cube(f"bk_map_topo_{i}{j}", f"bk_map_topo_{i}{j}", display="Wall Map")
        exit_block(f"bk_map_br_{i}{j}", f"bk_map_br_{i}{j}", "SECRET", trigger="USE", solid=True,
                   display="Map of Somewhere Else")
shaped("bk_bulb", [(5, 9, 5, 11, 15, 11, "c"), (6, 10, 6, 10, 14, 10, "g"), (4, 15, 4, 12, 16, 12, "c")],
       {"g": "bk_bulb", "c": "bk_cage"}, display="Caged Bulb", light=13, emissive=True, collides=False,
       unbreakable=True, sound="glass")
BLOCK_MODELS["bk_beacon_on"] = box_model([(5, 10, 5, 11, 16, 11, "b")], {"b": "bk_beacon"})
BLOCK_MODELS["bk_beacon_off"] = box_model([(5, 10, 5, 11, 16, 11, "b")], {"b": "bk_beacon_off"})
custom("bk_beacon", "com.noclip.block.AlarmBeaconBlock", BLOCK_MODELS["bk_beacon_off"],
       blockstate={"variants": {"lit=false": {"model": "noclip:block/bk_beacon_off"},
                                "lit=true": {"model": "noclip:block/bk_beacon_on"}}},
       display="Emergency Light", sound="glass", unbreakable=True, render="cutout", no_collision=True)
exit_block("bk_hatch", "bk_hatch", "ASCEND", trigger="USE", solid=True, display="Surface Hatch", sound="metal",
           light=6, emissive=True)
exit_block("bk_steel_door", "bk_steel_door", "DOOR", trigger="USE", solid=True, display="Sector Door",
           sound="metal")
ladder("bk_ladder", "bk_bunk_metal", display="Steel Ladder", unbreakable=True)

# ------------------------------------------------------------------ entity: the drill


def drill_tex():
    c = Canvas()
    coat = lambda w, h: T.grain(T.ramp(T.noise(f"bkdc{w}{h}", shape=(h, w), sigma=1), "#3a4030", "#465038"),
                                f"bkdc{w}{h}", 0.05)

    def head(w, h):
        img = T.ramp(T.noise(f"bkdh{w}", shape=(h, w), sigma=1), "#1a1c18", "#24261e")
        return img

    def face(w, h):
        img = head(w, h)
        for ex in (1, 5):
            img[2:5, ex:ex + 2] = T.hexc("#8a9a90")
            img[3, ex] = T.hexc("#d0e0d8")
        img[6:8, 3:5] = T.hexc("#3a3c34")
        return img

    def legs(w, h):
        img = coat(w, h)
        img[-3:] = T.hexc("#141410")
        return img

    paint_humanoid(c, head, coat, coat, legs, face=face)
    c.box(32, 0, 2, 3, 2, all="#2a2c24")
    return c.out()


def drill_glow():
    c = Canvas()

    def face(w, h):
        img = np.zeros((h, w, 3))
        for ex in (1, 5):
            img[3, ex:ex + 2] = T.hexc("#a0c8b0")
        return img

    c.box(0, 0, 8, 8, 8, front=face)
    rgb, a = c.out()
    return rgb, (rgb.sum(axis=2) > 0.1).astype(float)


entity_model("the_drill", humanoid(head_extra=[(32, 0, -1, -3, -6, 2, 3, 2)], arm_len=13, leg_len=13))
entity_tex("the_drill", drill_tex)
entity_tex("the_drill_glow", drill_glow)
lang("entity.noclip.the_drill", "The Drill")
lang("item.noclip.the_drill_spawn_egg", "The Drill Spawn Egg")
ITEM_MODELS["the_drill_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def ventilation():
    d = 24.0
    t = Y.t_axis(d)
    air = Y.bp(Y.pink(d, "bkair"), 100, 1500) * 0.5
    gen = Y.hum(d, 50.0, "bkgen", buzz=0.1, harmonics=(1, 2, 3, 4)) * 0.18
    thrum = Y.lp(Y.brown(d, "bkthrum"), 70) * 0.6 * (0.85 + 0.15 * np.sin(2 * np.pi * t / 3.0))
    x = air + Y.lp(gen, 700) + thrum
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.5), 0.0012), "stereo"


def march():
    out = np.zeros(int(5.0 * Y.SR))
    for k in range(10):
        Y.place(out, Y.footstep(f"bkm{k}", "concrete"), 0.2 + k * 0.48, 0.5 + 0.05 * k)
        Y.place(out, Y.footstep(f"bkm2{k}", "concrete"), 0.24 + k * 0.48, 0.4)
    return Y.norm(Y.reverb(Y.lp(out, 1600), 2.5, 0.65, "bkmarch"), 0.6)


def morse():
    d = 4.0
    t = Y.t_axis(d)
    pattern = "... --- ...  .-.. --- ... -"
    gate = np.zeros(len(t))
    pos = 0.1
    for ch in pattern:
        ln = 0.06 if ch == "." else 0.18 if ch == "-" else 0.0
        if ln:
            gate[int(pos * Y.SR):int((pos + ln) * Y.SR)] = 1
        pos += ln + (0.06 if ch != " " else 0.12)
    x = np.sin(2 * np.pi * 700 * t) * gate * 0.4 + Y.bp(Y.white(d, "bkmn"), 500, 3000) * 0.05
    return Y.norm(Y.reverb(x, 1.5, 0.4, "bkmorse"), 0.4)


def door_clank():
    out = np.zeros(int(3.5 * Y.SR))
    Y.place(out, Y.impact("bkclank", 1.2, ring=0.8), 0.05)
    return Y.norm(Y.reverb(Y.lp(out, 2000), 3.0, 0.7, "bkclank"), 0.6)


def pa_chime():
    d = 4.0
    t = Y.t_axis(d)
    x = np.zeros(len(t))
    for k, f in enumerate((523.3, 659.3, 784.0)):
        tt = np.clip(t - k * 0.45, 0, None)
        x += np.sin(2 * np.pi * f * tt) * np.exp(-tt * 2.2) * (t >= k * 0.45)
    voice = Y.whisper("bkpa", 1.8)
    out = np.zeros(len(t))
    Y.place(out, Y.bp(voice, 300, 2500) * 1.2, 1.6, 1.0)
    x = x * 0.6 + out
    crackle = Y.hp(Y.white(d, "bkcr"), 3000) * (Y.rng("bkcr2").random(len(t)) > 0.97) * 0.3
    return Y.norm(Y.reverb(Y.bp(x + crackle, 250, 4000), 2.5, 0.6, "bkpa"), 0.6)


def klaxon():
    d = 2.0
    t = Y.t_axis(d)
    f0 = 420 + 260 * np.clip((t % 1.0) / 0.8, 0, 1)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * 0.5
    x = Y.lp(x, 3000) * (((t % 1.0) < 0.85).astype(float))
    return Y.norm(Y.reverb(x, 2.0, 0.5, "bkklax"), 0.85)


def blast():
    d = 4.0
    t = Y.t_axis(d)
    grind = Y.bp(Y.white(d, "bkgrind"), 200, 2000) * np.clip(t / 0.3, 0, 1) * np.clip((1.6 - t) / 0.3, 0, 1) * 0.5
    out = np.zeros(len(t))
    Y.place(out, Y.impact("bkboom", 1.8, ring=0.5), 1.5, 1.0)
    return Y.norm(Y.reverb(grind + out, 3.0, 0.6, "bkblast"), 0.95)


def drill_breath():
    d = 3.0
    t = Y.t_axis(d)
    ph = (t % 1.5) / 1.5
    env = np.sin(np.pi * np.clip(ph / 0.45, 0, 1)) * (ph < 0.45) + 0.7 * np.sin(np.pi * np.clip((ph - 0.5) / 0.45, 0, 1)) * (ph >= 0.5) * (ph < 0.95)
    x = Y.bp(Y.white(d, "bkdb"), 200, 900) * env + Y.bp(Y.white(d, "bkdb2"), 1800, 2600) * env * 0.3
    return Y.norm(x, 0.7)


def drill_hurt():
    d = 0.5
    t = Y.t_axis(d)
    return Y.norm(Y.bp(Y.white(d, "bkdhu"), 300, 1500) * np.exp(-t * 8), 0.8)


sound("ambient.bunker", [("ambient/bunker", ventilation)], stream=True)
sound("ambient.bunker.additions",
      [("ambient/bunker_march", march), ("ambient/bunker_morse", morse), ("ambient/bunker_clank", door_clank),
       ("ambient/bunker_pa", pa_chime)], subtitle="Somewhere in the bunker")
sound("event.bunker_klaxon", [("event/bunker_klaxon", klaxon)], subtitle="Alarm klaxon")
sound("event.bunker_blast_door", [("event/bunker_blast_door", blast)], subtitle="A blast door closes")
sound("event.bunker_pa", [("event/bunker_pa", pa_chime)], subtitle="The PA crackles")
sound("entity.the_drill.breath", [("entity/the_drill_breath", drill_breath)], subtitle="Breathing through a filter")
sound("entity.the_drill.hurt", [("entity/the_drill_hurt", drill_hurt)], subtitle="The Drill grunts")

level("bunker", fog="#1c1e18", sky="#1c1e18", ambient="ambient.bunker", additions="ambient.bunker.additions",
      additions_chance=0.004, fixed_time=6000, height=48)
