"""Salt Flats. White ground to every horizon, a skin of brine that mirrors the sky, and
a door standing on its own in the middle of nothing. There are other doors, too. They
shimmer. The real one is the one that casts a shadow."""
import numpy as np
from assets import cube, cube_faces, shaped, exit_block, level, sound, BLOCK_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import box_model, draw_text, centered, paint_metal

S = 32
tx = A.tex


def hex_crust(seed, lo="#dcdad4", hi="#f4f2ec", ridge="#ffffff", dark="#b8b4aa"):
    """Polygonal salt crust: flat cells separated by raised, bright ridges with a shadowed side."""
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    r = T.rng(seed)
    pts = r.random((9, 2)) * S
    d1 = np.full((S, S), 1e9)
    d2 = np.full((S, S), 1e9)
    for px, py in pts:
        for ox in (-S, 0, S):
            for oy in (-S, 0, S):
                d = np.sqrt((xx - px - ox) ** 2 + (yy - py - oy) ** 2)
                d2 = np.where(d < d1, d1, np.minimum(d2, d))
                d1 = np.minimum(d1, d)
    edge = np.clip(1.6 - (d2 - d1), 0, 1)
    img = T.ramp(T.fbm(seed + "f", octaves=3, base=4), lo, hi)
    img = T.blend(img, T.solid(ridge), edge * 0.9)
    shadow = np.clip(2.6 - (d2 - d1), 0, 1) - edge
    img = T.blend(img, T.solid(dark), np.clip(shadow, 0, 1) * 0.35)
    grit = (T.rng(seed + "g").random((S, S)) > 0.96).astype(float)
    img = T.blend(img, T.solid("#9a948a"), grit * 0.25)
    return T.grain(img, seed, 0.025)


def crust_shadow():
    return T.shade(hex_crust("slc2", "#9aa2b4", "#aab2c4", "#c4ccdc", "#7a8294"), 0.95)


def brine():
    rgb = T.ramp(T.noise("slbr", sigma=3), "#cfe2f0", "#e8f2fa")
    yy, xx = np.mgrid[0:S, 0:S]
    ripple = (np.sin((xx + yy * 0.3) * 0.7 + T.noise("slbrr", sigma=2) * 4) > 0.92).astype(float)
    rgb = T.blend(rgb, T.solid("#ffffff"), ripple * 0.5)
    a = 0.38 + ripple * 0.3 + T.noise("slbra", sigma=2) * 0.1
    # salt rims poking through the film
    rim = (T.noise("slrim", sigma=1.0) > 0.86).astype(float)
    rgb = T.blend(rgb, T.solid("#f8f6f0"), rim)
    a = np.maximum(a, rim * 0.95)
    return rgb, np.clip(a, 0, 1)


def mirror_sky():
    """What the brine reflects: the sky, pale at the horizon."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("slsky", sigma=6) * 0.4 + 0.3, "#9ec0dc", "#b8d2e8", "#d6e6f2")
    return T.grain(img, "slsky", 0.01)


def track():
    img = hex_crust("sltr")
    xx = np.mgrid[0:S, 0:S][1]
    ruts = ((np.abs(xx - 7) < 3) | (np.abs(xx - 25) < 3)).astype(float)
    tread = (((np.mgrid[0:S, 0:S][0] // 2) % 2) == 0).astype(float) * 0.1
    return T.blend(img, T.solid("#8e8a80"), ruts * (0.45 + tread))


def line():
    img = hex_crust("slln")
    xx = np.mgrid[0:S, 0:S][1]
    band = (np.abs(xx - 16) < 6).astype(float) * (0.8 + 0.2 * T.noise("sllnn", sigma=1))
    return T.blend(img, T.solid("#141416"), band)


def footprints():
    img = hex_crust("slfp")
    yy, xx = np.mgrid[0:S, 0:S]
    for cx, cy in ((10, 8), (22, 24)):
        m = ((xx - cx) ** 2 / 9 + (yy - cy) ** 2 / 30) < 1
        img = T.blend(img, T.solid("#c6c0b4"), m * 0.7)
    return img


tx("sl_crust", lambda: hex_crust("slc"))
tx("sl_crust_b", lambda: hex_crust("slcb"))
tx("sl_crust_shadow", crust_shadow)
tx("sl_brine", brine)
tx("sl_mirror_sky", mirror_sky)
tx("sl_track", track)
tx("sl_line", line)
tx("sl_footprints", footprints)
tx("sl_salt_side", lambda: T.grain(T.ramp(T.fbm("slside", octaves=3, base=3), "#c8c2b6", "#dcd6ca"), "slside", 0.03))

cube_faces("sl_crust", top="sl_crust", bottom="sl_salt_side", side="sl_salt_side", sound="sand", display="Salt Crust")
cube_faces("sl_crust_b", top="sl_crust_b", bottom="sl_salt_side", side="sl_salt_side", sound="sand", display="Salt Crust")
cube_faces("sl_crust_shadow", top="sl_crust_shadow", bottom="sl_salt_side", side="sl_salt_side", sound="sand",
           display="Salt Crust (In Shadow)")
cube_faces("sl_track", top="sl_track", bottom="sl_salt_side", side="sl_salt_side", sound="sand", display="Tyre Tracks")
cube_faces("sl_line", top="sl_line", bottom="sl_salt_side", side="sl_salt_side", sound="sand", display="Painted Course Line")
cube_faces("sl_footprints", top="sl_footprints", bottom="sl_salt_side", side="sl_salt_side", sound="sand",
           display="Footprints")
cube("sl_brine", "sl_brine", display="Brine Film", sound="sand", kind="glass", render="translucent")
cube("sl_mirror_sky", "sl_mirror_sky", display="Reflected Sky", light=12, emissive=True, flat=True)
cube("sl_salt", "sl_salt_side", display="Salt", sound="sand")

# ------------------------------------------------------------------ the door (and the doors that aren't)


def door_wood(seed):
    img = T.ramp(T.fbm(seed, octaves=3, base=3), "#7a6248", "#8e7454")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (np.sin(yy * 0.9 + T.noise(seed + "w", sigma=2) * 3) > 0.85) * 0.12)
    return T.grain(img, seed, 0.04)


def door_lower():
    img = door_wood("sldl")
    img = T.rect(img, 5, 2, 27, 28, "#6a5440")
    img = T.rect(img, 7, 4, 25, 26, "#7e6650")
    img = T.rect(img, 24, 0, 28, 4, "#c8b060")
    return img


def door_upper():
    img = door_wood("sldu")
    img = T.rect(img, 5, 4, 27, 30, "#6a5440")
    img = T.rect(img, 7, 6, 25, 28, "#7e6650")
    return img


def frame():
    return T.grain(T.ramp(T.fbm("slfr", octaves=3, base=2), "#e8e4dc", "#f4f0e8"), "slfr", 0.02)


tx("sl_door_lower", door_lower)
tx("sl_door_upper", door_upper)
tx("sl_frame", frame)
tx("sl_door_edge", lambda: door_wood("slde"))

KNOB = (12.5, 13, 6.4, 13.5, 14, 9.6, "edge")


def door_boxes(part):
    if part == "lower":
        return [(1, 0, 7, 3, 16, 9, "frame"), (13, 0, 7, 15, 16, 9, "frame"),
                (3, 0, 7.2, 13, 16, 8.8, {"all": "edge", "north": "door", "south": "door"}),
                (11, 13.5, 6.4, 12, 14.5, 9.6, "frame")]
    if part == "upper":
        return [(1, 0, 7, 3, 16, 9, "frame"), (13, 0, 7, 15, 16, 9, "frame"),
                (3, 0, 7.2, 13, 16, 8.8, {"all": "edge", "north": "door", "south": "door"})]
    return [(0, 0, 6.5, 16, 3, 9.5, "frame")]


for part, door_tex in (("lower", "sl_door_lower"), ("upper", "sl_door_upper"), ("top", "sl_door_upper")):
    texs = {"frame": "sl_frame", "edge": "sl_door_edge", "door": door_tex}
    if part == "top":
        texs = {"frame": "sl_frame"}
    # the real door: use it to leave
    exit_block(f"sl_door_{part}", "sl_frame", "DOOR", trigger="USE", solid=True, display="Standing Door",
               sound="wood", model=box_model(door_boxes(part), texs), render="cutout")
    # mirages look exactly the same
    shaped(f"sl_mirage_{part}", door_boxes(part), texs, display="Standing Door", unbreakable=True, sound="wood")

# ------------------------------------------------------------------ odds and ends


def survey_pole():
    img = T.solid("#e8e4dc")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.blend(img, T.solid("#c03028"), ((yy // 8) % 2 == 0)[..., None] * np.ones(3))
    return T.grain(img, "slsp", 0.03)


def flag():
    rgb = np.ones((S, S, 3)) * T.hexc("#e06a2a")
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((yy > 2) & (yy < 18) & (xx < 4 + (18 - np.abs(yy - 10)) * 1.4)).astype(float)
    return rgb, a


tx("sl_survey", survey_pole)
tx("sl_flag", flag)
shaped("sl_survey_pole", [(7, 0, 7, 9, 16, 9, "p")], {"p": "sl_survey"}, display="Survey Pole", unbreakable=True)
shaped("sl_survey_flag", [(7, 0, 7, 9, 10, 9, "p"), (9, 4, 8, 16, 14, 8.01, {"all": None, "north": "f", "south": "f"})],
       {"p": "sl_survey", "f": "sl_flag"}, display="Survey Flag", unbreakable=True, collides=False)

tx("sl_chair_cloth", lambda: T.grain(T.ramp(T.noise("slcc", sigma=1), "#2a5a8a", "#346a9a"), "slcc", 0.04))
tx("sl_alu", lambda: paint_metal("slalu", "#a8aaac", "#c8cacc"))
shaped("sl_folding_chair", [(2, 0, 2, 3, 7, 3, "m"), (13, 0, 2, 14, 7, 3, "m"), (2, 0, 13, 3, 16, 14, "m"),
                            (13, 0, 13, 14, 16, 14, "m"), (2, 6, 2, 14, 7, 14, "c"), (2, 9, 13, 14, 16, 13.5, "c")],
       {"m": "sl_alu", "c": "sl_chair_cloth"}, display="Folding Chair", unbreakable=True)
tx("sl_table_top", lambda: T.grain(T.ramp(T.noise("sltt", sigma=2), "#d8d4c8", "#e4e0d4"), "sltt", 0.02))
shaped("sl_folding_table", [(1, 0, 1, 2, 12, 2, "m"), (14, 0, 1, 15, 12, 2, "m"), (1, 0, 14, 2, 12, 15, "m"),
                            (14, 0, 14, 15, 12, 15, "m"), (0, 12, 0, 16, 13, 16, "t")],
       {"m": "sl_alu", "t": "sl_table_top"}, display="Folding Table", unbreakable=True)


def bones():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    yy, xx = np.mgrid[0:S, 0:S]
    spine = (np.abs(yy - 16 - np.sin(xx / 5) * 2) < 1.2) & (xx > 3) & (xx < 29)
    ribs = (((xx - 6) % 4) == 0) & (np.abs(yy - 16) < 7) & (xx > 8) & (xx < 24)
    skull = ((xx - 27) ** 2 + (yy - 16) ** 2) < 7
    m = spine | ribs | skull
    rgb[m] = T.hexc("#ece6d6")
    a[m] = 1
    return rgb, a


tx("sl_bones", bones)
shaped("sl_bones", [(0, 0.1, 0, 16, 0.2, 16, {"all": None, "up": "b"})], {"b": "sl_bones"}, display="Bleached Bones",
       collides=False, unbreakable=True, sound="stone")

tx("sl_pool_wall", lambda: T.grain(T.ramp(T.noise("slpw", sigma=1.5), "#7a8a8e", "#9aaaae"), "slpw", 0.04))
cube("sl_pool_wall", "sl_pool_wall", display="Brine-Soaked Salt", sound="sand")
tx("sl_pool_bottom", lambda: T.solid("#02060a"))
exit_block("sl_pool_bottom", "sl_pool_bottom", "WATER", display="Bottom of the Brine Pool")

# ------------------------------------------------------------------ sound


def glare_loop():
    d = 24.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "slwind"), 900, 4500) * 0.18 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 12))
    low = Y.lp(Y.brown(d, "sllow"), 70) * 0.25
    ring = np.sin(2 * np.pi * (round(8200 * d) / d) * t) * 0.012 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 8))
    ring += np.sin(2 * np.pi * (round(8231 * d) / d) * t) * 0.008
    x = Y.loopable(Y.norm(wind + low + ring, 0.35), 1.5)
    return Y.stereoize(x, 0.0022), "stereo"


def crackle():
    out = np.zeros(int(1.5 * Y.SR))
    r = Y.rng("slcrk")
    for k in range(22):
        n = int(0.01 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = Y.white(0.01, f"slc{k}")[:n] * np.exp(-tt * 400)
        Y.place(out, Y.hp(c, 2500), 0.05 + (k / 22) * 0.6 + r.random() * 0.05, 0.3 + r.random() * 0.6)
    return Y.reverb(Y.norm(out, 0.45), 0.6, 0.2, "slcrk")


def far_door():
    out = np.zeros(int(5.0 * Y.SR))
    Y.place(out, Y.impact("slslam", 0.8), 0.1, 1.0)
    return Y.reverb(Y.lp(Y.norm(out, 0.4), 1500), 4.0, 0.85, "slslam")


def engine():
    d = 6.0
    t = Y.t_axis(d)
    env = np.sin(np.pi * t / d) ** 2
    f = 90 + 40 * np.sin(np.pi * t / d)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)) * 0.4 + Y.lp(Y.white(d, "sleng"), 300) * 0.4
    return Y.reverb(Y.lp(Y.norm(x * env, 0.3), 1200), 3.0, 0.7, "sleng")


def shimmer():
    d = 2.2
    t = Y.t_axis(d)
    x = Y.hp(Y.white(d, "slshim"), 5000) * (t / d) ** 2 * np.exp(-np.maximum(t - 1.9, 0) * 30)
    x += np.sin(2 * np.pi * 1760 * t * (1 + t * 0.2)) * 0.15 * (t / d) ** 3
    return Y.reverb(Y.norm(x, 0.5), 1.5, 0.6, "slshim")


def door_tone():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.drone(d, [55.0, 82.4, 110.0], "sltone", lfo=0) * np.sin(np.pi * t / d) ** 2
    return Y.norm(x, 0.6)


sound("ambient.salt_flats", [("ambient/salt_flats", glare_loop)], stream=True)
sound("ambient.salt_flats.additions",
      [("ambient/salt_flats_crackle", crackle), ("ambient/salt_flats_door", far_door),
       ("ambient/salt_flats_engine", engine)],
      subtitle="Something far out on the flats")
sound("event.salt_flats_shimmer", [("event/salt_flats_shimmer", shimmer)], subtitle="The door shimmers away")
sound("event.salt_flats_tone", [("event/salt_flats_tone", door_tone)], subtitle="A low tone from the door")

level("salt_flats", fog="#e6eef4", sky="#9cc4e4", ambient="ambient.salt_flats",
      additions="ambient.salt_flats.additions", additions_chance=0.003, fixed_time=8600, skylight=True,
      ceiling=False, height=80)
