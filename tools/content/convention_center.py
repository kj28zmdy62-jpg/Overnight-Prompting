"""Convention Center. An exhibition hall of empty booths under a truss roof, carpet seams running off
into the distance, a lanyard with your name on every badge, and a PA that keeps paging you."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, pane, custom, level, sound
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, wood, voice, speaker, chime

S = 32

BANNERS = [("EXIT", "PRO"), ("NO", "CLIP"), ("HUM", "LITE"), ("YELLO", "CORP"), ("ALMND", "WATER"), ("LEVEL", "ZERO"),
           ("THE", "HUB"), ("FIRE", "EXIT")]
BANNER_COLS = [("#1a3a8a", "#f4f0e0"), ("#8a1a1a", "#f4e8c8"), ("#1a6a3a", "#f4f0e0"), ("#e8c020", "#1a1a1a"),
               ("#c8a060", "#3a2010"), ("#c8b464", "#3a3010"), ("#2a2a2a", "#e8e8e8"), ("#c83020", "#ffffff")]


def aisle_carpet():
    img = T.ramp(T.noise("cac", sigma=0.5) * 0.6 + T.fbm("cac2", base=5) * 0.4, "#28344a", "#323e56")
    yy, xx = np.mgrid[0:S, 0:S]
    # the seam: a pale line running forever along the aisle, and the tile edges across it
    seam = (yy == 15) | (yy == 16)
    img = T.blend(img, T.solid("#4a5670"), seam.astype(float) * 0.7)
    img = T.shade(img, 1 - (xx == 0) * 0.12)
    tape = (yy >= 14) & (yy <= 17) & ((xx % 16) < 3)
    img = T.blend(img, T.solid("#b8b8a8"), tape.astype(float) * 0.5)
    return T.grain(img, "cac", 0.04)


def aisle_carpet_x():
    return aisle_carpet().transpose(1, 0, 2).copy()


def booth_carpet(seed, lo, hi):
    def f():
        img = T.ramp(T.noise(seed, sigma=0.5), lo, hi)
        return T.grain(T.border(img, "#1a1a1a", 1, alpha=0.3), seed, 0.04)
    return f


def concrete():
    img = T.ramp(T.fbm("cco", base=5), "#5e5e5a", "#6a6a64")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx == 0) | (yy == 0)) * 0.2)
    return T.grain(T.stains(img, "ccos", "#3e3e3a", count=2, strength=0.3, size=7), "cco", 0.04)


def truss():
    rgb = np.ones((S, S, 3)) * T.hexc("#5a5c60")
    yy, xx = np.mgrid[0:S, 0:S]
    chord = (yy < 3) | (yy > 28) | (xx < 3) | (xx > 28)
    diag = (np.abs(xx - yy) < 2) | (np.abs(xx - (31 - yy)) < 2)
    a = (chord | diag).astype(float)
    rgb = T.shade(rgb, 1 + (chord * 0.15))
    return rgb, a


def roof():
    img = T.ramp(T.fbm("cro", base=4), "#141618", "#1c1e22")
    yy, xx = np.mgrid[0:S, 0:S]
    return T.shade(img, 1 - ((yy % 8) == 0) * 0.3)


def high_bay(on=True):
    img = T.solid("#202224")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    col = T.ramp(np.clip(1 - d / 13, 0, 1), "#e8dcc0", "#fffaf0") if on else T.solid("#4a4a48")
    return T.blend(img, col, (d < 13).astype(float))


def drape():
    rgb = T.ramp(np.sin(np.arange(S)[None, :] / 32 * 2 * np.pi * 5) * 0.5 + 0.5 + np.zeros((S, S)), "#20242e", "#343a48")
    rgb = T.grain(rgb, "cdr", 0.03)
    return rgb


def pipe():
    return brushed("cpi", "#9a9c9e", "#d0d2d4", vertical=True)


def banner(i):
    def f():
        bg, fg = BANNER_COLS[i]
        img = T.ramp(T.noise(f"cbn{i}", sigma=2), bg, bg)
        l1, l2 = BANNERS[i]
        img = ink(img, centered(l1, y=7, scale=1, spacing=1 if len(l1) < 5 else 0), fg)
        img = ink(img, centered(l2, y=18, scale=1, spacing=1 if len(l2) < 5 else 0), fg)
        img = T.border(img, fg, 1, alpha=0.6)
        return T.grain(img, f"cbn{i}", 0.02)
    return f


def table_cloth():
    img = T.ramp(T.noise("ctc", sigma=1.5), "#1a2a5a", "#22346a")
    yy, xx = np.mgrid[0:S, 0:S]
    return T.grain(T.shade(img, 1 + np.sin(xx / 32 * 2 * np.pi * 5) * 0.04), "ctc", 0.02)


def brochures():
    img = T.solid("#e8e4dc")
    r = T.rng("cbr")
    for _ in range(9):
        x, y = int(r.random() * 24), int(r.random() * 24)
        col = ["#c83020", "#1a6a3a", "#e8c020", "#1a3a8a"][int(r.random() * 4)]
        img = T.rect(img, x, y, x + 8, y + 10, col)
        img = T.rect(img, x + 1, y + 6, x + 7, y + 7, "#f4f0e8")
    return img


def screen(on):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.solid("#0a0a0c")
    scr = (xx >= 2) & (xx <= 29) & (yy >= 4) & (yy <= 26)
    if on:
        pic = T.ramp(np.clip(yy / 32, 0, 1), "#1a3a8a", "#3a6ac8")
        pic = ink(pic, centered("WELCOME", y=8, scale=1, spacing=0), "#ffffff")
        pic = ink(pic, centered("BACK", y=17, scale=1), "#e8c020")
    else:
        pic = T.ramp(np.clip((xx + yy) / 64, 0, 1), "#0a0a0c", "#1a1c22")
    return T.blend(img, pic, scr.astype(float))


def standee():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    head = ((xx - 16) ** 2 + (yy - 5) ** 2) < 16
    body = (np.abs(xx - 16) < 6) & (yy > 8)
    arm = (yy > 10) & (yy < 13) & (xx > 20) & (xx < 28)
    m = head | body | arm
    rgb[m] = T.hexc("#1a3a8a")
    rgb[head] = T.hexc("#e8c8a8")
    # the smile has been cut out of the cardboard
    smile = (np.abs(np.sqrt((xx - 16) ** 2 + (yy - 4) ** 2) - 2.2) < 0.7) & (yy > 5)
    a[m] = 1
    a[smile] = 0
    rgb[body & (yy > 12) & (yy < 15)] = T.hexc("#f4f0e8")
    return rgb, a


def lanyard_rack():
    img = brushed("clr", "#6a6c70", "#8a8c90")
    yy, xx = np.mgrid[0:S, 0:S]
    for k, x in enumerate((4, 12, 20, 28)):
        strap = (np.abs(xx - x) < 1) & (yy < 18)
        img = T.blend(img, T.solid("#c83020" if k % 2 else "#1a3a8a"), strap.astype(float))
        badge = (np.abs(xx - x) < 3) & (yy >= 18) & (yy <= 27)
        img = T.blend(img, T.solid("#f4f0e8"), badge.astype(float))
        img = T.blend(img, T.solid("#c83020"), (badge & (yy < 20)).astype(float))
    return img


def info_desk():
    img = T.ramp(T.noise("cid", sigma=2), "#e8e4dc", "#f0ece4")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 16) ** 2 + (yy - 14) ** 2)
    img = T.blend(img, T.solid("#1a3a8a"), (d < 10).astype(float))
    img = ink(img, centered("i", y=8, scale=2) * (d < 10), "#ffffff")
    img = T.blend(img, T.solid("#2a2a2a"), (yy > 28).astype(float))
    return img


def bell():
    return brushed("cbe", "#a8862a", "#e0c060")


tex("conv_aisle", aisle_carpet)
tex("conv_aisle_x", aisle_carpet_x)
for nm, lo, hi in (("red", "#5a1a1a", "#6a2222"), ("green", "#1a4a2a", "#225634"), ("grey", "#4a4a4c", "#565658"),
                   ("tan", "#7a6a4a", "#867454")):
    tex(f"conv_booth_{nm}", booth_carpet("cbc" + nm, lo, hi))
tex("conv_concrete", concrete)
tex("conv_truss", truss)
tex("conv_roof", roof)
tex("conv_high_bay", lambda: high_bay(True))
tex("conv_high_bay_off", lambda: high_bay(False))
tex("conv_drape", drape)
tex("conv_pipe", pipe)
for i in range(len(BANNERS)):
    tex(f"conv_banner_{i}", banner(i))
tex("conv_cloth", table_cloth)
tex("conv_brochures", brochures)
tex("conv_screen", lambda: screen(True))
tex("conv_screen_off", lambda: screen(False))
tex("conv_standee", standee)
tex("conv_lanyards", lanyard_rack)
tex("conv_info", info_desk)
tex("conv_bell", bell)

cube("conv_aisle", "conv_aisle", display="Aisle Carpet", sound="wool")
cube("conv_aisle_x", "conv_aisle_x", display="Aisle Carpet", sound="wool")
for nm in ("red", "green", "grey", "tan"):
    cube(f"conv_booth_{nm}", f"conv_booth_{nm}", display="Booth Carpet", sound="wool")
cube("conv_concrete", "conv_concrete", display="Hall Floor")
cube("conv_roof", "conv_roof", display="Hall Roof", flat=True)
shaped("conv_truss", [(0, 0, 7, 16, 16, 9, {"all": None, "north": "t", "south": "t"})], {"t": "conv_truss"},
       display="Roof Truss", collides=False, unbreakable=True, sound="metal")
flicker_light("conv_high_bay", "conv_high_bay", "conv_high_bay_off", "conv_roof", light=15, flicker=0.0, display="High-Bay Light")
flicker_light("conv_high_bay_bad", "conv_high_bay", "conv_high_bay_off", "conv_roof", light=15, flicker=0.5, display="High-Bay Light")
pane("conv_drape", "conv_drape", display="Pipe and Drape", sound="wool")
shaped("conv_pipe", [(7, 0, 7, 9, 16, 9, "p")], {"p": "conv_pipe"}, display="Upright", unbreakable=True, sound="metal")
for i in range(len(BANNERS)):
    shaped(f"conv_banner_{i}", [(0, 0, 7.5, 16, 16, 8.5, {"all": "p", "north": "b", "south": "b"})],
           {"b": f"conv_banner_{i}", "p": "conv_pipe"}, display="Booth Banner", collides=False, unbreakable=True, sound="wool")
shaped("conv_table", [(0, 0, 2, 16, 12, 14, {"all": "c", "up": "b"})], {"c": "conv_cloth", "b": "conv_brochures"},
       display="Booth Table", unbreakable=True, sound="wool")
shaped("conv_tv", [(1, 4, 7, 15, 14, 8, {"all": "p", "north": "s"}), (7, 0, 7, 9, 4, 9, "p")], {"s": "conv_screen", "p": "conv_pipe"},
       display="Booth Screen", emissive=True, collides=False, unbreakable=True)
shaped("conv_standee", [(0, 0, 7.5, 16, 16, 8.5, {"all": None, "north": "s", "south": "s"})], {"s": "conv_standee"},
       display="Cardboard Standee", collides=False, unbreakable=True, sound="wool")
shaped("conv_lanyards", [(1, 0, 7, 15, 16, 9, {"all": "p", "north": "l", "south": "l"})], {"l": "conv_lanyards", "p": "conv_pipe"},
       display="Lanyard Rack", collides=False, unbreakable=True, sound="wool")
custom("conv_info", "com.noclip.block.ConventionDeskBlock", {
    "parent": "minecraft:block/block",
    "textures": {"f": "noclip:block/conv_info", "c": "noclip:block/conv_cloth", "b": "noclip:block/conv_bell",
                 "particle": "noclip:block/conv_info"},
    "elements": [{"from": [0, 0, 0], "to": [16, 15, 16], "faces": {"north": {"texture": "#f"}, "south": {"texture": "#f"},
                                                                   "east": {"texture": "#f"}, "west": {"texture": "#f"},
                                                                   "up": {"texture": "#c"}, "down": {"texture": "#c"}}},
                 {"from": [6, 15, 6], "to": [10, 18, 10], "faces": {f: {"texture": "#b"} for f in
                                                                    ("north", "south", "east", "west", "up", "down")}}]},
       display="Information Desk", unbreakable=True, sound="wood")

# ------------------------------------------------------------------ sound


def hall_air():
    d = 26.0
    t = Y.t_axis(d)
    air = Y.lp(Y.pink(d, "cha"), 350) * 0.5
    hvac = Y.bp(Y.pink(d, "chv"), 400, 1500) * 0.08 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 13))
    murmur = Y.reverb(Y.bp(Y.pink(d, "chm"), 300, 1100) * 0.06, 5.0, 0.9, "chmr")[: len(t)]
    x = Y.reverb(air + hvac, 5.0, 0.7, "charv")[: len(t)] + murmur
    return Y.stereoize(Y.loopable(Y.norm(x, 0.35), 1.5), 0.003), "stereo"


def pa(i):
    def f():
        out = np.zeros(int(7.5 * Y.SR))
        Y.place(out, chime([784.0, 659.3, 523.3], dur=2.4, decay=1.6) * 0.5, 0.0)
        Y.place(out, voice(f"cpa{i}", 3.6 + i * 0.4, pitch=160 + i * 12, breath=0.12), 1.9)
        out = speaker(out, 350, 3600)
        return Y.norm(Y.reverb(out, 5.0, 0.75, f"cpar{i}"), 0.7)
    return f


def forklift():
    out = np.zeros(int(4 * Y.SR))
    t = Y.t_axis(0.35)
    beep = np.sign(np.sin(2 * np.pi * 1100 * t)) * 0.3 * Y.env(len(t), 0.01, 0.02)
    for k in range(5):
        Y.place(out, beep, k * 0.7)
    return Y.norm(Y.reverb(Y.lp(out, 3000), 5.0, 0.85, "cflr"), 0.25)


def hammering():
    out = np.zeros(int(4 * Y.SR))
    for k in range(6):
        Y.place(out, Y.impact(f"cham{k}", 0.15, ring=0.3), 0.3 + k * 0.42)
    return Y.norm(Y.reverb(Y.lp(out, 2500), 5.0, 0.85, "chmr2"), 0.3)


def desk_bell():
    t = Y.t_axis(2.5)
    x = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * (1.5 + i)) / (i + 1) for i, f in enumerate((2093.0, 4400.0, 6100.0)))
    return Y.norm(Y.reverb(x, 3.0, 0.6, "cbel"), 0.7)


sound("ambient.convention_center", [("ambient/convention_center", hall_air)], stream=True)
sound("ambient.convention_center.additions", [("ambient/convention_center_forklift", forklift),
                                              ("ambient/convention_center_hammer", hammering)],
      subtitle="Somebody setting up a booth")
sound("event.convention_pa", [(f"event/convention_pa{i}", pa(i)) for i in range(3)], subtitle="The PA pages someone")
sound("event.convention_bell", [("event/convention_bell", desk_bell)], subtitle="A desk bell")

level("convention_center", fog="#1c2028", sky="#1c2028", ambient="ambient.convention_center",
      additions="ambient.convention_center.additions", additions_chance=0.003, fixed_time=18000, height=48, ambient_light=0.05)
