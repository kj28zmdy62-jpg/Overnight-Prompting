"""Indoor Playground - foam floors, netted climbing frames, tubes, slides and ball pits in a dark hall.
Children laugh somewhere. Every so often someone counts to ten, and then comes looking."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, stairs, custom, flicker_light, level, sound, lang, \
    entity_model, entity_tex, part, ITEM_MODELS
import tex as T
import synth as Y
from content import afterhours_kit as K
from entity_paint import Canvas
from content.entities_core import humanoid, paint_humanoid

S = 32
PRIMARY = {"red": ("#c82a2a", "#d83a34"), "blue": ("#2a5ac8", "#3468d4"), "yellow": ("#e8c020", "#f0cc30"),
           "green": ("#2a9a3a", "#36a846")}


def foam(seed, col):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=0.5) * 0.4 + T.fbm(seed + "f", octaves=2, base=5) * 0.6, *col)
    # interlocking puzzle edges
    teeth = ((xx < 2) & ((yy // 6) % 2 == 0)) | ((yy < 2) & ((xx // 6) % 2 == 1))
    img = T.shade(img, 1 - teeth * 0.25)
    img = T.shade(img, 1 - (T.noise(seed + "dent", sigma=1.2) > 0.82) * 0.12)  # dents from small feet
    return T.grain(img, seed, 0.02)


def padding(seed, col):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=2), *col)
    seam = (yy % 16 == 0) | (xx % 32 == 0)
    img = T.shade(img, 1 - seam * 0.3)
    stitch = ((yy % 16 == 2) & (xx % 3 == 0))
    img = T.shade(img, 1 - stitch * 0.15)
    img = T.shade(img, 1 + np.clip(T.noise(seed + "s", sigma=3) - 0.6, 0, 1) * 0.5)  # vinyl sheen
    return T.grain(img, seed, 0.02)


def balls(seed="ppb"):
    img = T.solid("#1a1a24")
    r = T.rng(seed)
    cols = ["#e83a3a", "#3a7ae8", "#f0d030", "#3ac84a", "#e87ad0", "#f08a2a"]
    for k in range(60):
        x, y = r.random() * S, r.random() * S
        c = T.hexc(cols[int(r.random() * len(cols))])
        for dx in (-S, 0, S):
            for dy in (-S, 0, S):
                m = K.disc(S, S, x + dx, y + dy, 3.4)
                img = T.blend(img, np.ones((S, S, 3)) * c, m.astype(float))
                hl = K.disc(S, S, x + dx - 1.2, y + dy - 1.2, 0.9)
                img = T.blend(img, np.ones((S, S, 3)) * np.minimum(c + 0.35, 1), hl.astype(float) * 0.8)
    return T.grain(img, seed, 0.02)


def net():
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx + yy) % 8 == 0) | ((xx - yy) % 8 == 0)).astype(float)
    rgb = T.ramp(T.noise("ppnet", sigma=1), "#1a1a1a", "#2e2e2e")
    return rgb, a


def tube(col):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        lo, hi = col
        ring = np.sin(xx / 32 * 2 * np.pi * 2) * 0.5 + 0.5
        img = T.ramp(ring * 0.6 + T.noise("pptb" + lo, sigma=2) * 0.4, lo, hi)
        img = T.shade(img, 1 + np.clip(np.sin(yy / 32 * np.pi) - 0.7, 0, 1) * 0.8)  # glossy highlight
        bubble = ((xx - 15.5) ** 2 / 64 + (yy - 15.5) ** 2 / 64) < 1
        img = T.blend(img, T.ramp(T.noise("pptbw" + lo, sigma=2), "#2a3440", "#4a5a6a"), bubble * 0.85)
        img = T.blend(img, T.solid("#e8f0f8"), (bubble & ((xx - 12) ** 2 + (yy - 12) ** 2 < 6)) * 0.5)
        return T.grain(img, "pptb" + lo, 0.02)
    return f


def tube_floor():
    img = padding("pptf", ("#3a3a44", "#46464e"))
    return img


def slide():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("ppsl", sigma=2), "#e8c020", "#f8dc40")
    img = T.shade(img, 1 + np.clip(T.noise("ppsls", sigma=3) - 0.5, 0, 1) * 0.6)
    img = T.shade(img, 1 - ((xx < 3) | (xx > 28)) * 0.25)
    return T.grain(img, "ppsl", 0.02)


def wall():
    img = T.ramp(T.fbm("ppw", octaves=3, base=5), "#3a3a5a", "#44446a")
    yy, xx = np.mgrid[0:S, 0:S]
    r = T.rng("ppstars")
    for k in range(4):  # painted stars and clouds on a night-blue mural
        x, y = int(r.random() * 28), int(r.random() * 28)
        img = T.blend(img, T.solid("#e8d870"), K.disc(S, S, x, y, 1.4).astype(float) * 0.8)
    return T.grain(img, "ppw", 0.03)


def mural_face():
    """The sun from the mural. Someone painted the eyes again, darker."""
    img = wall()
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#f0c030"), (d < 10).astype(float))
    for a in range(12):
        ang = a / 12 * 2 * np.pi
        img = T.blend(img, T.solid("#f0c030"), K.line_mask(S, S, 15.5 + np.cos(ang) * 11, 15.5 + np.sin(ang) * 11,
                                                          15.5 + np.cos(ang) * 15, 15.5 + np.sin(ang) * 15, 1.6))
    img = T.blend(img, T.solid("#0a0806"), (K.disc(S, S, 12, 13, 1.8) | K.disc(S, S, 19, 13, 1.8)).astype(float))
    smile = (np.abs(d - 5.5) < 0.8) & (yy > 16)
    img = T.blend(img, T.solid("#7a2a10"), smile.astype(float))
    drip = (np.abs(xx - 12) < 0.6) & (yy > 14) & (yy < 22)
    return T.blend(img, T.solid("#0a0806"), drip * 0.8)


def ceiling():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("ppc", octaves=3, base=5), "#18181e", "#202028")
    return T.shade(img, 1 - ((xx % 8) < 1) * 0.3)


def lamp(on):
    img = T.solid("#fff4dc") if on else T.ramp(T.noise("pplo", sigma=2), "#4a4840", "#5a5850")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, ceiling(), (d > 12).astype(float))
    return T.blend(img, T.solid("#6a6a6a"), ((d > 12) & (d < 13.5)).astype(float))


def cubby():
    """Shoe cubbies. Every one has a pair of small shoes in it."""
    img = T.ramp(T.noise("ppcb", sigma=2), "#c8a070", "#d4ac7c")
    for cy in range(2):
        for cx in range(2):
            x0, y0 = cx * 16 + 2, cy * 16 + 2
            img = T.rect(img, x0, y0, x0 + 12, y0 + 12, "#2a1e14")
            col = ["#e83a6a", "#3a7ae8", "#f0f0f0", "#2a2a2a"][(cx + cy * 2) % 4]
            img = T.rect(img, x0 + 2, y0 + 8, x0 + 6, y0 + 12, col)
            img = T.rect(img, x0 + 7, y0 + 8, x0 + 11, y0 + 12, col)
    return T.grain(img, "ppcb", 0.02)


def exit_door(top):
    img = T.ramp(T.noise("ppd" + str(top), sigma=2), "#2a5ac8", "#3468d4")
    img = T.border(img, "#e8c020", 2, sides="we" + ("n" if top else "s"))
    if top:
        img = T.blend(img, T.solid("#f8f4e8"), K.centered("BYE", S, S, 10, font=K.F57))
        img = T.blend(img, T.solid("#f8f4e8"), K.centered("BYE", S, S, 19, font=K.F35))
    else:
        img = T.rect(img, 4, 2, 28, 6, "#e8c020")
    return T.grain(img, "ppd" + str(top), 0.03)


for c, col in PRIMARY.items():
    tex(f"pp_foam_{c}", (lambda c=c, col=col: foam("ppf" + c, col)))
    tex(f"pp_pad_{c}", (lambda c=c, col=col: padding("ppp" + c, col)))
    tex(f"pp_tube_{c}", tube(col))
tex("pp_balls", balls)
tex("pp_net", net)
tex("pp_tube_floor", tube_floor)
tex("pp_slide", slide)
tex("pp_wall", wall)
tex("pp_mural_sun", mural_face)
tex("pp_ceiling", ceiling)
tex("pp_lamp", lambda: lamp(True))
tex("pp_lamp_off", lambda: lamp(False))
tex("pp_cubby", cubby)
tex("pp_exit_top", lambda: exit_door(True))
tex("pp_exit_bottom", lambda: exit_door(False))

for c in PRIMARY:
    cube(f"pp_foam_{c}", f"pp_foam_{c}", display="Foam Floor Tile", sound="wool")
    cube(f"pp_pad_{c}", f"pp_pad_{c}", display="Vinyl Padding", sound="wool")
    cube(f"pp_tube_{c}", f"pp_tube_{c}", display="Crawl Tube", sound="wool")
pane("pp_net", "pp_net", display="Safety Netting", sound="wool")
cube("pp_tube_floor", "pp_tube_floor", display="Tube Padding", sound="wool")
stairs("pp_slide", "pp_slide", "pp_pad_yellow", display="Slide", sound="wool")
stairs("pp_steps", "pp_foam_blue", "pp_pad_blue", display="Foam Steps", sound="wool")
cube("pp_wall", "pp_wall", display="Night Mural", sound="stone")
cube("pp_mural_sun", "pp_mural_sun", display="The Sun (Mural)", sound="stone")
cube("pp_ceiling", "pp_ceiling", display="Hall Ceiling", sound="metal", flat=True)
flicker_light("pp_lamp", "pp_lamp", "pp_lamp_off", "pp_ceiling", light=15, flicker=0.05, display="High Bay Lamp")
cube("pp_cubby", "pp_cubby", display="Shoe Cubbies", sound="wood")
custom("pp_balls", "com.noclip.block.BallPitBlock", {"parent": "minecraft:block/cube_all",
                                                      "textures": {"all": "noclip:block/pp_balls"}},
       display="Ball Pit", sound="wool", unbreakable=True, no_collision=True, render="cutout")
from assets import exit_block
exit_block("pp_exit", "pp_exit_bottom", "DOOR", trigger="USE", solid=True, display="Exit (Bye Bye!)", sound="wool")
cube("pp_exit_top", "pp_exit_top", display="Exit (Bye Bye!)", sound="wool")
shaped("pp_foam_block", [(1, 0, 1, 15, 14, 15, "pad")], {"pad": "pp_pad_red"}, display="Foam Block", sound="wool",
       unbreakable=True)
shaped("pp_foam_arch", [(0, 0, 4, 4, 12, 12, "pad"), (12, 0, 4, 16, 12, 12, "pad"), (0, 12, 4, 16, 16, 12, "pad")],
       {"pad": "pp_pad_blue"}, display="Foam Arch", sound="wool", unbreakable=True)
shaped("pp_foam_cylinder", [(4, 0, 4, 12, 10, 12, "pad"), (3, 2, 3, 13, 8, 13, "pad")], {"pad": "pp_pad_green"},
       display="Foam Roll", sound="wool", unbreakable=True)
shaped("pp_post", [(5, 0, 5, 11, 16, 11, "pad")], {"pad": "pp_pad_red"}, display="Padded Post", sound="wool",
       unbreakable=True)
shaped("pp_post_blue", [(5, 0, 5, 11, 16, 11, "pad")], {"pad": "pp_pad_blue"}, display="Padded Post", sound="wool",
       unbreakable=True)
shaped("pp_lone_shoe", [(6, 0, 4, 10, 3, 11, "shoe")], {"shoe": "pp_pad_red"}, display="A Small Shoe",
       unbreakable=True, collides=False)

# ------------------------------------------------------------------ the mascot


def mascot_tex():
    c = Canvas(128, 64)
    fur = lambda w, h: T.ramp(T.noise(f"pmf{w}{h}", shape=(h, w), sigma=0.5), "#8a4ab0", "#9a5ac0")
    belly = lambda w, h: T.ramp(T.noise(f"pmb{w}{h}", shape=(h, w), sigma=0.5), "#e8d8b0", "#f0e0c0")

    def face(w, h):
        img = fur(w, h)
        yy, xx = np.mgrid[0:h, 0:w]
        for ex in (w * 0.3, w * 0.7):
            img[(xx - ex) ** 2 + (yy - h * 0.35) ** 2 < (w * 0.12) ** 2] = T.hexc("#f8f8f0")
            img[(xx - ex) ** 2 + (yy - h * 0.35) ** 2 < (w * 0.05) ** 2] = T.hexc("#0a0a0a")
        mouth = (np.abs(np.sqrt((xx - w / 2) ** 2 + (yy - h * 0.45) ** 2) - w * 0.32) < 0.8) & (yy > h * 0.55)
        img[mouth] = T.hexc("#2a0a10")
        return img

    def body(w, h):
        img = fur(w, h)
        if w == 8 and h == 12:
            img[3:10, 2:6] = belly(4, 7)
        return img

    def glove(w, h):
        img = fur(w, h)
        if h > 6:
            img[-3:] = T.hexc("#f0f0e8")
        return img

    def shoe(w, h):
        img = fur(w, h)
        if h > 6:
            img[-3:] = T.hexc("#e8c020")
        return img

    paint_humanoid(c, fur, body, glove, shoe, face=face)
    # the big costume head
    c.box(64, 0, 10, 10, 10, all=fur, front=face)
    return c.out()


def mascot_parts():
    parts = humanoid(arm_len=13, leg_len=12)
    parts[0] = part("head", (0, 0, 0), cubes=[(64, 0, -5, -10, -5, 10, 10, 10)])
    return parts


entity_model("playplace_mascot", mascot_parts(), tex_size=(128, 64))
entity_tex("playplace_mascot", mascot_tex)
lang("entity.noclip.playplace_mascot", "It")
lang("item.noclip.playplace_mascot_spawn_egg", "Mascot Spawn Egg")
ITEM_MODELS["playplace_mascot_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def child_laugh(seed, pitch=1.0, n=5):
    out = np.zeros(int(1.6 * Y.SR))
    r = Y.rng(seed)
    for k in range(n):
        nn = int(0.1 * Y.SR)
        tt = np.arange(nn) / Y.SR
        f0 = (420 + r.random() * 60) * pitch * (1 - 0.05 * k)
        ph = 2 * np.pi * f0 * tt
        syl = (np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.25 * np.sin(3 * ph)) * np.sin(np.pi * tt / tt[-1]) ** 2
        syl = K._formant_filter(syl, 900, 200) + K._formant_filter(syl, 1600, 250) * 0.6
        syl += Y.bp(Y.white(0.1, f"{seed}{k}"), 2000, 6000)[:nn] * 0.15 * np.sin(np.pi * tt / tt[-1])
        Y.place(out, syl, 0.05 + k * 0.14, 1 - k * 0.12)
    return Y.norm(out, 0.8)


def hall_tone():
    d = 26.0
    t = Y.t_axis(d)
    hvac = Y.lp(Y.brown(d, "pphv"), 140) * 0.45 + Y.bp(Y.pink(d, "pphv2"), 150, 700) * 0.12
    hum = Y.lp(Y.hum(d, 60.0, "pphum", buzz=0.2, harmonics=(2, 4)), 800) * 0.1
    laughs = np.zeros_like(t)
    for k, at in enumerate((4.0, 13.5, 21.0)):
        K.circular_place(laughs, child_laugh(f"pphl{k}", 1.0 + k * 0.08, 4), at, 0.12)
    laughs = K.loop_reverb(Y.lp(laughs, 2500), 4.0, 0.9, "pplr")
    x = hvac + hum + laughs
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.003), "stereo"


def giggle(i):
    def f():
        return Y.norm(Y.reverb(child_laugh(f"ppg{i}", 1.0 + i * 0.1, 4 + i), 3.0, 0.65, f"ppg{i}"), 0.6)
    return f


def running():
    out = np.zeros(int(3 * Y.SR))
    for k in range(10):
        Y.place(out, Y.footstep(f"pprun{k}", "carpet", 0.6), 0.1 + k * 0.22, 0.6 + 0.4 * np.sin(np.pi * k / 10))
    return Y.norm(Y.reverb(out, 3.0, 0.7, "pprun"), 0.5)


def slide_whoosh():
    d = 1.6
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "ppsw"), 300, 3000) * np.sin(np.pi * t / d) ** 2
    squeak = np.sin(2 * np.pi * (900 - 300 * t) * t) * 0.2 * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(x + squeak, 2.5, 0.6, "ppsw"), 0.5)


def ball_rustle():
    d = 1.2
    t = Y.t_axis(d)
    clicks = (Y.rng("pprus").random(len(t)) > 0.997).astype(float)
    x = Y.bp(clicks, 1500, 7000) * 3 + Y.bp(Y.white(d, "pprus2"), 800, 3000) * 0.1
    return Y.norm(x * np.sin(np.pi * t / d), 0.6)


def counting():
    out = np.zeros(int(7.5 * Y.SR))
    for k in range(10):
        v = K.pa_voice(f"ppcount{k}", syllables=1 if k not in (6, 9) else 2, f0=290, rate=0.22, band=(250, 5000))
        Y.place(out, v, 0.2 + k * 0.55, 0.8)
    ready = K.pa_voice("ppready", syllables=5, f0=320, rate=0.13, band=(250, 5000))
    Y.place(out, ready, 6.0, 1.0)
    return Y.norm(Y.reverb(out, 3.5, 0.6, "ppcount"), 0.8)


def found():
    out = np.zeros(int(2.5 * Y.SR))
    v = K.pa_voice("ppfound", syllables=3, f0=340, rate=0.14, band=(250, 5000))
    Y.place(out, v, 0.05, 1.0)
    Y.place(out, child_laugh("ppfl", 1.15, 5), 0.6, 0.8)
    return Y.norm(Y.reverb(out, 2.0, 0.4, "ppfound"), 0.9)


def free():
    out = np.zeros(int(4.0 * Y.SR))
    v = K.pa_voice("ppfree", syllables=7, f0=300, rate=0.24, band=(250, 5000))
    Y.place(out, v, 0.1, 0.8)
    return Y.norm(Y.reverb(out, 4.0, 0.8, "ppfree"), 0.6)


def squeak_steps():
    out = np.zeros(int(2.0 * Y.SR))
    for k in range(3):
        n = int(0.12 * Y.SR)
        tt = np.arange(n) / Y.SR
        sq = np.sin(2 * np.pi * (1400 + k * 80) * tt + 3 * np.sin(2 * np.pi * 30 * tt)) * np.sin(np.pi * tt / tt[-1])
        Y.place(out, sq * 0.5 + Y.footstep(f"ppsq{k}", "carpet")[:n] * 0.5, 0.1 + k * 0.6)
    return Y.norm(out, 0.7)


sound("ambient.playground", [("ambient/playground", hall_tone)], stream=True)
sound("ambient.playground.additions",
      [(f"ambient/playground_giggle{i}", giggle(i)) for i in range(3)] +
      [("ambient/playground_running", running), ("ambient/playground_slide", slide_whoosh)],
      subtitle="Children laughing")
sound("event.playground_count", [("event/playground_count", counting)], subtitle="Someone counts to ten")
sound("event.playground_found", [("event/playground_found", found)], subtitle="Found you!")
sound("event.playground_free", [("event/playground_free", free)], subtitle="Olly olly oxen free")
sound("event.playground_balls", [("event/playground_balls", ball_rustle)], subtitle="Balls shift")
sound("entity.playplace_mascot.step", [("entity/playplace_mascot_step", squeak_steps)], subtitle="Squeaking shoes")

level("playground", fog="#14141c", sky="#14141c", ambient="ambient.playground",
      additions="ambient.playground.additions", additions_chance=0.006, fixed_time=18000, height=48)
