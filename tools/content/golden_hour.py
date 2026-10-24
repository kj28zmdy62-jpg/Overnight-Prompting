"""Golden Hour Suburbs. The sun is always about to set over identical houses on identical
streets. Long shadows, warm light, nobody. It is the calmest place in the Backrooms.
Keep moving anyway."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, exit_block, stairs, slab, fence, level, sound, BLOCK_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import draw_text, centered, paint_metal

S = 32
tx = A.tex

SUN = np.array([1.0, 0.72, 0.42])      # warm light on sunlit faces
SKY = np.array([0.55, 0.6, 0.85])      # cool fill in the shade


def lit(img, k=1.12):
    return np.clip(img * k * (0.55 + 0.45 * SUN / SUN.max()) * np.array([1.08, 1.0, 0.86]), 0, 1)


def shade(img, k=0.48):
    return np.clip(img * k * (0.5 + 0.5 * SKY / SKY.max()), 0, 1)


def sunlit_cube(name, painter, display, sound="stone", top=None):
    """A full cube with the low western sun baked in: bright warm west faces, cool dark east faces."""
    base = painter()
    tx(name + "_w", lambda: lit(base, 1.18))
    tx(name + "_u", lambda: lit(top() if top else base, 1.0))
    tx(name + "_n", lambda: np.clip(lit(base, 0.86) * 0.92, 0, 1))
    tx(name + "_e", lambda: shade(base))
    cube(name, name + "_n", display=display, sound=sound)
    r = lambda s: "noclip:block/" + name + s
    BLOCK_MODELS[name] = {"parent": "noclip:base/cube_flat", "textures": {
        "west": r("_w"), "up": r("_u"), "north": r("_n"), "south": r("_n"), "east": r("_e"),
        "down": r("_e"), "particle": r("_n")}}


# ------------------------------------------------------------------ ground: sunlit and in shadow


def lawn():
    img = T.ramp(T.fbm("ghl", octaves=4, base=5) * 0.5 + T.noise("ghl2", sigma=0.4) * 0.5, "#5a7a2e", "#6c8c36", "#7e9a40")
    yy, xx = np.mgrid[0:S, 0:S]
    stripes = ((xx // 8) % 2) * 0.05   # mown stripes
    return T.grain(T.shade(img, 1 + stripes), "ghl", 0.04)


def sidewalk():
    img = T.ramp(T.fbm("ghs", octaves=3, base=5), "#a8a296", "#b8b2a6")
    img = T.border(img, "#7a766c", 1, sides="nw")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx == 16) * 0.12))
    return T.grain(img, "ghs", 0.03)


def road():
    img = T.ramp(T.fbm("ghr", octaves=4, base=4) * 0.6 + T.noise("ghr2", sigma=0.5) * 0.4, "#3e3c3a", "#4a4846", "#565452")
    img = T.cracks(img, "ghrc", "#2a2826", count=1, alpha=0.4)
    return T.grain(img, "ghr", 0.04)


def driveway():
    img = T.ramp(T.fbm("ghd", octaves=3, base=5), "#9a948a", "#aaa49a")
    img = T.stains(img, "ghds", "#5a564c", count=1, strength=0.35, size=8)
    return T.grain(img, "ghd", 0.03)


def curb():
    img = T.ramp(T.noise("ghc", sigma=1), "#b8b2a6", "#c8c2b6")
    img[:3] = T.hexc("#d0cabe")
    return T.grain(img, "ghc", 0.03)


GROUND = {"lawn": lawn, "sidewalk": sidewalk, "road": road, "drive": driveway, "curb": curb}
for key, fn in GROUND.items():
    tx(f"gh_{key}_sun", lambda fn=fn: lit(fn(), 1.1))
    tx(f"gh_{key}_shadow", lambda fn=fn: shade(fn(), 0.42))
    tx(f"gh_{key}_side", lambda fn=fn: np.clip(lit(fn(), 0.8) * 0.9, 0, 1))
    for variant in ("sun", "shadow"):
        name = f"gh_{key}_{variant}"
        snd = "grass" if key == "lawn" else "stone"
        cube_faces(name, top=name, bottom=f"gh_{key}_shadow", side=f"gh_{key}_side", flat=True, sound=snd,
                   display={"lawn": "Lawn", "sidewalk": "Sidewalk", "road": "Street", "drive": "Driveway",
                            "curb": "Curb"}[key] + (" (In Shadow)" if variant == "shadow" else ""))


def storm_drain():
    img = lit(curb(), 1.0)
    img = T.rect(img, 4, 14, 28, 26, "#0a0a0a")
    for k in range(5):
        img = T.rect(img, 6 + k * 5, 14, 7 + k * 5, 26, "#3a3632")
    return img


tx("gh_drain", storm_drain)
exit_block("gh_storm_drain", "gh_drain", "FALL", display="Storm Drain")

# ------------------------------------------------------------------ houses

SIDING = {"cream": ("#d8ccb0", "#e4d8bc"), "mint": ("#a6c0a8", "#b2ccb4"), "blue": ("#9eb0c4", "#aabcd0"),
          "yellow": ("#d6c488", "#e2d094"), "salmon": ("#d0a48c", "#dcb098")}


def siding(lo, hi, seed):
    def f():
        img = T.ramp(T.noise(seed, sigma=2), lo, hi)
        yy, xx = np.mgrid[0:S, 0:S]
        board = (yy % 6 == 0).astype(float) * 0.22 + (yy % 6 == 1).astype(float) * -0.06
        img = T.shade(img, 1 - board)
        return T.grain(img, seed, 0.025)
    return f


for colour, (lo, hi) in SIDING.items():
    sunlit_cube(f"gh_siding_{colour}", siding(lo, hi, "ghsid" + colour), "Siding", sound="wood")


def trim():
    img = T.ramp(T.noise("ghtrim", sigma=2), "#e8e4dc", "#f2eee6")
    return T.grain(img, "ghtrim", 0.02)


sunlit_cube("gh_trim", trim, "White Trim", sound="wood")


def brick():
    img = T.ramp(T.fbm("ghbr", octaves=3, base=3), "#8a4a36", "#9a5640")
    yy, xx = np.mgrid[0:S, 0:S]
    off = ((yy // 4) % 2) * 4
    mortar = ((yy % 4) == 0) | (((xx + off) % 8) == 0)
    return T.grain(T.blend(img, T.solid("#c8bca8"), mortar * 0.7), "ghbr", 0.04)


sunlit_cube("gh_foundation", brick, "Brick Foundation")


def shingles():
    img = T.ramp(T.noise("ghsh", sigma=0.8), "#3e3a38", "#524c48")
    yy, xx = np.mgrid[0:S, 0:S]
    off = ((yy // 4) % 2) * 3
    lines = ((yy % 4) == 0) | ((((xx + off) % 6) == 0) & ((yy % 4) < 3))
    return T.grain(T.shade(img, 1 - lines * 0.25), "ghsh", 0.04)


tx("gh_shingles", lambda: lit(shingles(), 0.95))
cube("gh_roof_block", "gh_shingles", display="Roof", sound="wood")
stairs("gh_roof", "gh_shingles", "gh_roof_block", display="Roof", sound="wood")
slab("gh_roof_slab", "gh_shingles", display="Roof Ridge", sound="wood")


def window(kind="plain"):
    def f():
        img = T.solid("#2a2420")
        yy, xx = np.mgrid[0:S, 0:S]
        # sunset reflected in the glass, then curtains drawn behind it
        glow = np.clip(1 - (yy / 32.0), 0, 1)
        sky = T.ramp(glow, "#5a3a3a", "#e8a060", "#ffd890")
        img = T.blend(img, sky, (0.55 + 0.15 * T.noise("ghw" + kind, sigma=2))[..., None] * np.ones(3))
        curtain = ((xx < 9) | (xx > 22)) & (yy > 3)
        img = T.blend(img, T.ramp(T.noise("ghcur", sigma=1), "#c8b090", "#d8c0a0"), curtain * 0.8)
        if kind == "lit":
            img = T.blend(img, T.solid("#ffd8a0"), (~curtain & (yy > 3)) * 0.85)
        if kind == "figure":
            head = ((xx - 16) ** 2 + (yy - 11) ** 2) < 9
            body = (np.abs(xx - 16) < 5) & (yy > 14)
            img = T.blend(img, T.solid("#100c0a"), ((head | body) & ~curtain) * 0.92)
        img = T.border(img, "#ece8e0", 2)
        img[15:17, :] = T.hexc("#ece8e0")
        img[:, 15:17] = T.hexc("#ece8e0")
        return img
    return f


for kind in ("plain", "lit", "figure"):
    tx(f"gh_window_{kind}", window(kind))
cube("gh_window", "gh_window_plain", display="Window", sound="glass")
cube("gh_window_lit", "gh_window_lit", display="Lit Window", sound="glass", light=10)
cube("gh_window_figure", "gh_window_figure", display="Window", sound="glass")
for kind, name in (("plain", "gh_window"), ("lit", "gh_window_lit"), ("figure", "gh_window_figure")):
    BLOCK_MODELS[name] = {"parent": "noclip:base/cube_flat", "textures": {
        f: "noclip:block/gh_window_" + kind for f in ("west", "east", "north", "south", "up", "down", "particle")}}


def front_door(top):
    def f():
        img = T.ramp(T.noise("ghfd" + str(top), sigma=2), "#6a2a22", "#7a342a")
        img = T.border(img, "#ece8e0", 2)
        yy, xx = np.mgrid[0:S, 0:S]
        if top:
            img = T.rect(img, 7, 6, 25, 14, "#4a1e18")
            img = T.rect(img, 7, 20, 25, 30, "#4a1e18")
            img = T.rect(img, 13, 22, 19, 24, "#c8a040")
        else:
            img = T.rect(img, 7, 2, 25, 26, "#4a1e18")
            img = T.rect(img, 22, 2, 25, 6, "#c8a040")
        return T.grain(img, "ghfd", 0.03)
    return f


tx("gh_door_top", front_door(True))
tx("gh_door_bottom", front_door(False))
for half in ("top", "bottom"):
    sunlit_cube(f"gh_door_{half}", lambda h=half: T.ramp(T.noise("ghfd" + h, sigma=2), "#6a2a22", "#7a342a"),
                "Front Door", sound="wood")
    BLOCK_MODELS[f"gh_door_{half}"]["textures"]["west"] = "noclip:block/gh_door_" + half
    BLOCK_MODELS[f"gh_door_{half}"]["textures"]["east"] = "noclip:block/gh_door_" + half


def open_dark():
    img = T.solid("#030202")
    yy, xx = np.mgrid[0:S, 0:S]
    # a hallway you can't see the end of
    img = T.blend(img, T.solid("#1a1210"), (np.abs(xx - 16) > 11) * 0.6)
    return img


tx("gh_open_dark", open_dark)
exit_block("gh_open_door", "gh_open_dark", "DOOR", display="Open Front Door")


def porch_light():
    img = T.ramp(T.noise("ghpl", sigma=1), "#3a3028", "#4a3c30")
    img = T.rect(img, 10, 8, 22, 26, "#c8b890")
    return img


tx("gh_porch_light", porch_light)
shaped("gh_porch_light", [(6, 4, 0, 10, 12, 3, "l")], {"l": "gh_porch_light"}, display="Porch Light",
       collides=False, unbreakable=True, sound="metal")

# ------------------------------------------------------------------ yards and streets

tx("gh_picket", lambda: lit(T.grain(T.solid("#ece8e0"), "ghpk", 0.03), 0.95))
fence("gh_picket_fence", "gh_picket", display="Picket Fence", sound="wood")


def bark():
    img = T.ramp(T.fbm("ghbk", octaves=3, base=2), "#3a2c20", "#4e3c2c")
    xx = np.mgrid[0:S, 0:S][1]
    return T.grain(T.shade(img, 1 - (np.sin(xx * 1.4 + T.noise("ghbkn", sigma=2) * 6) > 0.6) * 0.2), "ghbk", 0.05)


def leaves(variant):
    def f():
        r = T.rng("ghlv", variant)
        base = T.ramp(T.noise("ghlv" + variant, sigma=0.7), "#3e5a22", "#5a7a2c", "#7a9a3a")
        if variant == "sun":
            base = lit(base, 1.1)
        elif variant == "shadow":
            base = shade(base, 0.7)
        a = (r.random((S, S)) > 0.18).astype(float)
        holes = T.noise("ghlvh" + variant, sigma=1.2) > 0.78
        a[holes] = 0
        return base, a
    return f


sunlit_cube("gh_trunk", bark, "Tree Trunk", sound="wood")
for v in ("sun", "mid", "shadow"):
    tx(f"gh_leaves_{v}", leaves(v))
cube("gh_leaves", "gh_leaves_mid", display="Leaves", sound="grass", render="cutout_mipped")
BLOCK_MODELS["gh_leaves"] = {"parent": "noclip:base/cube_flat", "textures": {
    "west": "noclip:block/gh_leaves_sun", "up": "noclip:block/gh_leaves_sun", "north": "noclip:block/gh_leaves_mid",
    "south": "noclip:block/gh_leaves_mid", "east": "noclip:block/gh_leaves_shadow",
    "down": "noclip:block/gh_leaves_shadow", "particle": "noclip:block/gh_leaves_mid"}}

tx("gh_lamp_pole", lambda: paint_metal("ghlp", "#3a3a38", "#4a4a46"))
tx("gh_lamp_head", lambda: T.ramp(T.noise("ghlh", sigma=1), "#c8c4b8", "#d8d4c8"))
shaped("gh_lamp_pole", [(7, 0, 7, 9, 16, 9, "p")], {"p": "gh_lamp_pole"}, display="Streetlight", unbreakable=True)
shaped("gh_lamp_head", [(7, 0, 7, 9, 6, 9, "p"), (7, 5, 1, 9, 6, 9, "p"), (5.5, 3, 0, 10.5, 5, 5, {"all": "p", "down": "h"})],
       {"p": "gh_lamp_pole", "h": "gh_lamp_head"}, display="Streetlight (Off)", unbreakable=True)


def mailbox_tex():
    img = paint_metal("ghmb", "#2a3a5a", "#34466a")
    img = T.rect(img, 22, 4, 26, 14, "#c03028")
    return img


tx("gh_mailbox", mailbox_tex)
tx("gh_post", lambda: lit(T.ramp(T.fbm("ghpost", octaves=2, base=2), "#5a4a38", "#6a5a46"), 0.95))
shaped("gh_mailbox", [(7, 0, 7, 9, 10, 9, "p"), (5, 10, 3, 11, 15, 13, "m")], {"p": "gh_post", "m": "gh_mailbox"},
       display="Mailbox", unbreakable=True, sound="metal")


def hoop_board():
    img = T.solid("#e8e4dc")
    img = T.border(img, "#c03028", 2)
    img = T.rect(img, 10, 14, 22, 24, "#e8e4dc")
    img[14, 10:22] = T.hexc("#c03028")
    img[14:24, 10] = T.hexc("#c03028")
    img[14:24, 21] = T.hexc("#c03028")
    return lit(img, 0.95)


tx("gh_hoop", hoop_board)
tx("gh_net", lambda: (np.ones((S, S, 3)) * T.hexc("#e8e4dc"),
                      ((((np.mgrid[0:S, 0:S][0] + np.mgrid[0:S, 0:S][1]) % 6) == 0) |
                       (((np.mgrid[0:S, 0:S][0] - np.mgrid[0:S, 0:S][1]) % 6) == 0)).astype(float)))
shaped("gh_hoop", [(2, 4, 14, 14, 16, 15, {"all": "b", "north": "b"}), (5, 3, 9, 11, 4, 14, "r"),
                   (5.5, -3, 9.5, 10.5, 3, 13.5, {"all": "n", "up": None, "down": None})],
       {"b": "gh_hoop", "r": "gh_mailbox", "n": "gh_net"}, display="Basketball Hoop", unbreakable=True,
       collides=False, sound="metal")


def tricycle_tex():
    return T.grain(T.ramp(T.noise("ghtri", sigma=1), "#b02a22", "#c4362a"), "ghtri", 0.04)


tx("gh_tricycle", tricycle_tex)
tx("gh_tyre", lambda: T.grain(T.solid("#141414"), "ghty", 0.04))
shaped("gh_tricycle", [(7, 0, 2, 9, 5, 6, "t"), (3, 0, 11, 5, 3, 14, "t"), (11, 0, 11, 13, 3, 14, "t"),
                       (7.5, 3, 4, 8.5, 6, 13, "r"), (5, 5, 8, 11, 6, 12, "r"), (7.5, 6, 3, 8.5, 9, 4, "r"),
                       (5, 9, 3, 11, 10, 4, "r")], {"t": "gh_tyre", "r": "gh_tricycle"},
       display="Tricycle", unbreakable=True, collides=False, sound="metal")

# ------------------------------------------------------------------ sound


def dusk_loop():
    d = 26.0
    t = Y.t_axis(d)
    air = Y.lp(Y.pink(d, "ghair"), 500) * 0.25 * (0.8 + 0.2 * np.sin(2 * np.pi * t / 13))
    hum = Y.drone(d, [110.0, 164.8, 220.0], "ghhum", detune=0.15, lfo=1 / 13) * 0.04
    ring = np.sin(2 * np.pi * (round(5600 * d) / d) * t) * 0.006 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 6.5))
    x = Y.loopable(Y.norm(air + hum + ring, 0.35), 1.5)
    return Y.stereoize(x, 0.0016), "stereo"


def ice_cream_truck():
    """A music box tune from a truck nobody will ever see, slowed and drifting."""
    notes = [72, 74, 72, 69, 67, 69, 72, 74, 76, 74, 72, 69]
    out = np.zeros(int(7.0 * Y.SR))
    for i, nt in enumerate(notes):
        f = 440 * 2 ** ((nt - 69) / 12) * 0.92
        n = int(0.6 * Y.SR)
        tt = np.arange(n) / Y.SR
        wob = 1 + 0.012 * np.sin(2 * np.pi * 0.6 * (tt + i * 0.45))
        tone = np.sin(2 * np.pi * np.cumsum(f * wob) / Y.SR) * np.exp(-tt * 5)
        tone += 0.3 * np.sin(2 * np.pi * np.cumsum(f * 3 * wob) / Y.SR) * np.exp(-tt * 9)
        Y.place(out, tone, 0.2 + i * 0.48, 0.5)
    out = Y.lp(out, 2500)
    return Y.reverb(Y.norm(out, 0.3), 3.0, 0.7, "ghtruck")


def screen_door():
    out = np.zeros(int(3.0 * Y.SR))
    d = 0.9
    tt = Y.t_axis(d)
    creak = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(180 + 120 * np.sin(tt * 4)) / Y.SR)), 400, 3000) * np.sin(np.pi * tt / d) * 0.3
    Y.place(out, creak, 0.1)
    Y.place(out, Y.impact("ghslam", 0.35), 1.2, 0.7)
    return Y.reverb(Y.lp(Y.norm(out, 0.4), 4000), 2.5, 0.6, "ghsd")


def dog():
    out = np.zeros(int(2.5 * Y.SR))
    d = 0.22
    tt = Y.t_axis(d)
    f0 = 600 - tt * 900
    bark = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) + Y.white(d, "ghdog") * 0.4, 400, 2200) * np.sin(np.pi * tt / d)
    Y.place(out, bark, 0.1, 0.8)
    Y.place(out, bark[: int(len(bark) * 0.4)], 0.45, 0.5)   # cut off
    return Y.reverb(Y.lp(Y.norm(out, 0.35), 2500), 3.0, 0.75, "ghdog")


def chimes():
    out = np.zeros(int(5.0 * Y.SR))
    r = Y.rng("ghch")
    for k in range(6):
        f = [1318.5, 1568.0, 1760.0, 2093.0][int(r.random() * 4)]
        n = int(2.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, np.sin(2 * np.pi * f * tt) * np.exp(-tt * 2.5), 0.2 + r.random() * 2.0, 0.2 + r.random() * 0.2)
    return Y.reverb(Y.norm(out, 0.3), 2.0, 0.5, "ghch")


def lullaby():
    """Played while you stand still. It is very kind."""
    notes = [67, 72, 76, 74, 72, 67, 69, 72]
    out = np.zeros(int(5.0 * Y.SR))
    for i, nt in enumerate(notes):
        f = 440 * 2 ** ((nt - 69) / 12)
        n = int(1.2 * Y.SR)
        tt = np.arange(n) / Y.SR
        tone = np.sin(2 * np.pi * f * tt) * np.exp(-tt * 3.5) + 0.2 * np.sin(2 * np.pi * 2 * f * tt) * np.exp(-tt * 6)
        Y.place(out, tone, 0.1 + i * 0.55, 0.5)
    return Y.reverb(Y.norm(out, 0.4), 2.5, 0.55, "ghlull")


sound("ambient.golden_hour", [("ambient/golden_hour", dusk_loop)], stream=True)
sound("ambient.golden_hour.additions",
      [("ambient/golden_hour_truck", ice_cream_truck), ("ambient/golden_hour_screen_door", screen_door),
       ("ambient/golden_hour_dog", dog), ("ambient/golden_hour_chimes", chimes)],
      subtitle="Somewhere down the street")
sound("event.golden_hour_lullaby", [("event/golden_hour_lullaby", lullaby)], subtitle="A music box, very close")

level("golden_hour", fog="#e09a60", sky="#d88a58", ambient="ambient.golden_hour",
      additions="ambient.golden_hour.additions", additions_chance=0.0025, fixed_time=11800, skylight=True,
      ceiling=False, height=80)
