"""Level 2 - Pipe Dreams. Hot, narrow maintenance tunnels lined with pipes. When the pipes knock, get down."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, custom, carpet, ladder, level, sound, lang
import tex as T
import synth as Y
from content.classics_kit import draw_text, centered, box_model, metal

S = 32
yy, xx = np.mgrid[0:S, 0:S]


# ------------------------------------------------------------------ textures

def l2_concrete(seed, lo="#4a3f36", hi="#5e5145"):
    img = T.ramp(T.fbm(seed, octaves=4, base=5) * 0.7 + T.noise(seed + "n", sigma=0.6) * 0.3, lo, hi)
    pores = (T.rng(seed + "p").random((S, S)) > 0.93).astype(float)
    img = T.shade(img, 1 - pores * 0.18)
    # rust run-off and soot
    img = T.stains(img, seed + "rust", "#6b3a1c", count=2, strength=0.35, size=7)
    img = T.stains(img, seed + "soot", "#1e1a16", count=1, strength=0.3, size=10)
    return T.grain(img, seed, 0.05)


def wall():
    img = l2_concrete("l2w")
    seam = (yy % 16 == 15).astype(float) * 0.18
    return T.shade(img, 1 - seam)


def painted_pipe(img, y0, d, col_lo, col_hi, seed, bracket=True):
    """Paint a horizontal pipe of diameter d with cylinder shading across rows y0..y0+d."""
    t = (np.arange(d) + 0.5) / d
    shade = 0.55 + 0.6 * np.sin(np.pi * t) ** 0.7 - 0.2 * t
    body = T.ramp(T.noise(seed, sigma=1.0, shape=(d, S)) * 0.5 + 0.25, col_lo, col_hi)
    body = body * shade[:, None, None]
    out = img.copy()
    out[y0:y0 + d] = np.clip(body, 0, 1)
    # shadow cast on the wall beneath the pipe
    if y0 + d < S:
        out[y0 + d:y0 + d + 2] *= 0.6
    if bracket:
        out[y0 - 1:y0 + d + 1, 14:17] = out[y0 - 1:y0 + d + 1, 14:17] * 0.4 + T.hexc("#2a2622") * 0.6
        out[y0 - 1:y0 + d + 1, 15] = T.hexc("#6a645c")
    return out


def wall_pipes():
    img = l2_concrete("l2wp")
    img = painted_pipe(img, 6, 7, "#5a3a26", "#8a5a36", "l2pp1")
    img = painted_pipe(img, 17, 4, "#3e4a44", "#5a6a62", "l2pp2", bracket=False)
    img = painted_pipe(img, 24, 3, "#7a6a4a", "#9a8a62", "l2pp3", bracket=False)
    # drips of rust from the big pipe
    for x in (5, 22, 27):
        ln = 4 + (x % 5)
        img[13:13 + ln, x] = img[13:13 + ln, x] * 0.6 + T.hexc("#5a2e14") * 0.4
    return T.grain(img, "l2wp", 0.03)


def wall_risers():
    img = l2_concrete("l2wr")
    for x0, d, lo, hi in ((3, 8, "#5a3a26", "#8a5a36"), (15, 5, "#3e4a44", "#5a6a62"), (24, 4, "#6a5a3a", "#8a7a52")):
        t = (np.arange(d) + 0.5) / d
        shade = 0.55 + 0.6 * np.sin(np.pi * t) ** 0.7 - 0.2 * t
        body = T.ramp(T.noise(f"vr{x0}", sigma=1.0, shape=(S, d)) * 0.5 + 0.25, lo, hi) * shade[None, :, None]
        img[:, x0:x0 + d] = np.clip(body, 0, 1)
        img[:, x0 + d:x0 + d + 1] *= 0.6
        img[10:12, x0 - 1:x0 + d + 1] = T.hexc("#2c2824")
    return T.grain(img, "l2wr", 0.03)


def wall_base():
    img = l2_concrete("l2wb", "#3e352d", "#524539")
    grime = np.clip((yy - 14) / 14.0, 0, 1)[:, :] * T.noise("l2grime", sigma=1.5)
    img = T.shade(img, 1 - grime * 0.45)
    img = painted_pipe(img, 22, 5, "#4a4038", "#6a5c4e", "l2bp", bracket=False)
    return img


def wall_scorched():
    img = l2_concrete("l2ws", "#3a2c24", "#4e3a2e")
    soot = T.fbm("l2soot", octaves=3, base=4)
    img = T.blend(img, T.solid("#120e0c"), np.clip(soot * 1.4 - 0.45, 0, 0.85))
    img = painted_pipe(img, 8, 7, "#6a2a14", "#a0441c", "l2hot1")
    return img


def wall_frost():
    img = T.ramp(T.fbm("l2fr", octaves=4, base=5), "#5a6670", "#8a98a2")
    img = painted_pipe(img, 6, 7, "#8aa0ae", "#c8d8e2", "l2fp1")
    img = painted_pipe(img, 17, 4, "#7a8e9a", "#aec0ca", "l2fp2", bracket=False)
    crystals = (T.noise("l2frc", sigma=0.4) > 0.78).astype(float)
    img = T.blend(img, T.solid("#e8f4fa"), crystals * 0.6)
    return T.grain(img, "l2fr", 0.04)


def graffiti(lines, seed, color="#9a1e14"):
    def f():
        img = wall()
        y = 3
        for ln in lines:
            img = centered(img, ln, y, color, alpha=0.95, jitter=0.15)
            y += 7
        # paint drips
        r = T.rng(seed)
        for _ in range(5):
            x = 4 + int(r.random() * 24)
            y0 = 8 + int(r.random() * 18)
            for yy_ in range(y0, min(S, y0 + 2 + int(r.random() * 5))):
                img[yy_, x] = img[yy_, x] * 0.4 + T.hexc(color) * 0.6
        return img
    return f


def ceiling():
    img = l2_concrete("l2c", "#3a322b", "#4a4037")
    # a bundle of pipes running overhead
    for x0, d, lo, hi in ((2, 6, "#4a3424", "#6e4c32"), (10, 4, "#3a4440", "#56625c"), (17, 7, "#5a3a26", "#8a5a36"),
                          (27, 3, "#6a6050", "#8a806a")):
        t = (np.arange(d) + 0.5) / d
        shade = 0.6 + 0.55 * np.sin(np.pi * t) - 0.15 * t
        body = T.ramp(T.noise(f"cp{x0}", sigma=1.0, shape=(S, d)) * 0.5 + 0.25, lo, hi) * shade[None, :, None]
        img[:, x0:x0 + d] = np.clip(body, 0, 1)
    return T.grain(img, "l2c", 0.03)


def floor():
    img = l2_concrete("l2f", "#3a332d", "#4c433a")
    img = T.cracks(img, "l2fc", "#221e1a", count=2, alpha=0.5)
    oil = T.stains(img, "l2oil", "#141210", count=2, strength=0.5, size=6)
    return oil


def grate():
    base = T.solid("#0a0807")
    glow = T.ramp(T.noise("l2gg", sigma=3), "#120c08", "#2a1408")
    base = T.blend(base, glow, np.ones((S, S)) * 0.8)
    bar = ((xx % 8) < 2) | ((yy % 8) < 2)
    steel = metal("l2gr", "#3e3a36", "#5e5852", rust=("#5a3018", "#7a4220"), rust_amt=0.15)
    img = np.where(bar[..., None], steel, base)
    img = np.where(((xx % 8) == 0)[..., None] & bar[..., None], T.shade(steel, 1.25), img)
    return img


def floor_wet():
    img = l2_concrete("l2fw", "#2a2622", "#38322c")
    sheen = T.noise("l2sheen", sigma=3)
    return T.shade(img, 0.85 + sheen * 0.35)


def floor_frost():
    img = T.ramp(T.fbm("l2ff", octaves=4, base=4), "#6a7680", "#9aa8b2")
    return T.grain(T.blend(img, T.solid("#eef6fa"), (T.noise("l2ffc", sigma=0.5) > 0.7) * 0.5), "l2ff", 0.04)


def pipe_tex(seed, lo, hi, rust=True):
    def f():
        img = T.ramp(T.noise(seed, sigma=1.2) * 0.6 + T.fbm(seed + "f", octaves=3, base=3) * 0.4, lo, hi)
        if rust:
            m = np.clip(T.fbm(seed + "r", octaves=4, base=3) * 2.0 - 1.1, 0, 1)
            img = T.blend(img, T.ramp(T.noise(seed + "rc", sigma=0.6), "#4a2210", "#7a3a18"), m)
        # flange ring every half block
        ring = ((xx % 16) < 2).astype(float)
        img = T.shade(img, 1 - ring * 0.35)
        img = T.blend(img, T.solid("#2a2420"), ((xx % 16) == 2).astype(float) * 0.4)
        return T.grain(img, seed, 0.05)
    return f


def pipe_hot():
    img = T.ramp(T.fbm("l2ph", octaves=3, base=5) * 0.7 + 0.15, "#7a1e08", "#d8501a", "#ff9a40")
    ring = ((xx % 16) < 2).astype(float)
    img = T.shade(img, 1 - ring * 0.5)
    scale = np.clip(T.fbm("l2phc", octaves=3, base=2) * 2.2 - 1.15, 0, 1)
    img = T.blend(img, T.solid("#2a0e06"), scale * 0.8)
    return T.grain(img, "l2ph", 0.03)


def bracket():
    return metal("l2br", "#2a2622", "#3e3832")


def bulb():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(np.clip(1 - d / 22, 0, 1), "#ffb050", "#ffd890", "#fff6dc")
    filament = (np.abs(yy - 16) < 1) & (np.abs(xx - 16) < 7)
    return T.blend(img, T.solid("#ffffff"), filament.astype(float))


def bulb_dead():
    img = T.ramp(T.noise("l2bd", sigma=2), "#4a4236", "#5e5446")
    filament = (np.abs(yy - 16) < 1) & (np.abs(xx - 16) < 7)
    return T.blend(img, T.solid("#1a1612"), filament.astype(float))


def bulb_cold():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.ramp(np.clip(1 - d / 22, 0, 1), "#5a9ad8", "#a8d0f0", "#eaf6ff")


def cage():
    rgb = T.solid("#2a2622")
    a = (((xx % 8) < 1) | ((yy % 10) < 1)).astype(float)
    return rgb, a


def valve_red():
    return metal("l2v", "#7a1a12", "#b02a1c", rust=("#3a1a10", "#5a2a16"), rust_amt=-0.1)


def gauge():
    img = T.solid("#e8e2d0")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#1a1816"), (np.abs(d - 13.5) < 1.6).astype(float))
    # tick marks and a red zone
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    ticks = (np.abs(d - 10.5) < 1.5) & ((np.round(ang / (np.pi / 8)) * (np.pi / 8) - ang) ** 2 < 0.004)
    img = T.blend(img, T.solid("#2a2826"), ticks.astype(float))
    red = (np.abs(d - 10.5) < 1.6) & (ang > -0.2) & (ang < 0.9)
    img = T.blend(img, T.solid("#c02018"), red.astype(float))
    # needle pinned past the red
    for k in range(11):
        px, py = int(15.5 + np.cos(0.75) * k), int(15.5 + np.sin(0.75) * k)
        img[py, px] = T.hexc("#101010")
    img = T.blend(img, T.solid("#101010"), (d > 15).astype(float))
    return img


def door():
    img = metal("l2d", "#3e4a44", "#56645c", rust=("#4a2a16", "#6a3a1c"), rust_amt=0.05)
    img = T.border(img, "#1e2420", 2)
    img = T.rect(img, 4, 6, 28, 15, "#c8b42a")
    img = centered(img, "MAINT", 8, "#141410")
    img = T.rect(img, 23, 18, 27, 22, "#8a8a80")
    return T.grain(img, "l2d", 0.04)


def shaft_glow():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(np.clip(1 - d / 26 + T.noise("l2sh", sigma=2) * 0.3, 0, 1), "#200804", "#7a2408", "#e86a18")
    return img


def ladder_tex():
    rgb = metal("l2lad", "#4a4440", "#6a625a", rust=("#4a2a16", "#6a3a1c"), rust_amt=-0.2)
    a = (((xx >= 3) & (xx <= 5)) | ((xx >= 26) & (xx <= 28)) | ((yy % 8 < 2) & (xx > 3) & (xx < 28))).astype(float)
    return rgb, a


def hatch():
    img = metal("l2hat", "#3a3e3a", "#525852", rust=("#4a2a16", "#6a3a1c"), rust_amt=0.1)
    img = T.border(img, "#1c1e1c", 2)
    img = T.rect(img, 12, 12, 20, 20, "#7a7a70")
    img = T.rect(img, 14, 14, 18, 18, "#3a3a34")
    img = centered(img, "UP", 24, "#c8b42a")
    return img


def bedroll():
    img = T.ramp(T.noise("l2bed", sigma=1), "#3a4a2a", "#4e5e36")
    img = T.blend(img, T.solid("#2a3420"), ((yy % 10) < 1).astype(float))
    return T.grain(img, "l2bed", 0.06)


def steam_floor():
    img = grate()
    return T.blend(img, T.solid("#d8d0c4"), T.noise("l2stm", sigma=2) * 0.25)


tex("l2_wall", wall)
tex("l2_wall_pipes", wall_pipes)
tex("l2_wall_risers", wall_risers)
tex("l2_wall_base", wall_base)
tex("l2_wall_scorched", wall_scorched)
tex("l2_wall_frost", wall_frost)
tex("l2_graffiti_knock", graffiti(["PIPES", "KNOCK?", "GET", "DOWN"], "l2g1"))
tex("l2_graffiti_hot", graffiti(["IT GETS", "HOTTER", "DOWN", "THERE"], "l2g2", "#d8d0c0"))
tex("l2_graffiti_arrow", graffiti(["EXIT", "-->", "NOT", "REALLY"], "l2g3", "#e0b020"))
tex("l2_ceiling", ceiling)
tex("l2_floor", floor)
tex("l2_floor_wet", floor_wet)
tex("l2_floor_frost", floor_frost)
tex("l2_grate", grate)
tex("l2_steam_grate", steam_floor)
tex("l2_pipe", pipe_tex("l2p", "#5a3a26", "#8a5a36"))
tex("l2_pipe_green", pipe_tex("l2pg", "#33403a", "#526058"))
tex("l2_pipe_frost", pipe_tex("l2pf", "#9ab0be", "#d4e2ea", rust=False))
tex("l2_pipe_hot", pipe_hot)
tex("l2_bracket", bracket)
tex("l2_bulb", bulb)
tex("l2_bulb_dead", bulb_dead)
tex("l2_bulb_cold", bulb_cold)
tex("l2_cage", cage)
tex("l2_valve", valve_red)
tex("l2_gauge", gauge)
tex("l2_door", door)
tex("l2_shaft_glow", shaft_glow)
tex("l2_ladder", ladder_tex)
tex("l2_hatch", hatch)
tex("l2_bedroll", bedroll)

# ------------------------------------------------------------------ blocks

cube("pipe_tunnel_wall", "l2_wall", display="Tunnel Wall")
cube("pipe_tunnel_wall_lined", "l2_wall_pipes", display="Pipe-Lined Wall", sound="metal")
cube("pipe_tunnel_wall_risers", "l2_wall_risers", display="Riser-Lined Wall", sound="metal")
cube("pipe_tunnel_wall_base", "l2_wall_base", display="Grimy Tunnel Wall")
cube("pipe_tunnel_wall_scorched", "l2_wall_scorched", display="Scorched Tunnel Wall")
cube("pipe_tunnel_wall_frost", "l2_wall_frost", display="Frosted Tunnel Wall")
cube("pipe_tunnel_graffiti_knock", "l2_graffiti_knock", display="Scrawled Warning")
cube("pipe_tunnel_graffiti_hot", "l2_graffiti_hot", display="Scrawled Warning")
cube("pipe_tunnel_graffiti_arrow", "l2_graffiti_arrow", display="Scrawled Arrow")
cube("pipe_tunnel_ceiling", "l2_ceiling", display="Pipe Bundle Ceiling", flat=True, sound="metal")
cube("pipe_tunnel_floor", "l2_floor", display="Tunnel Floor")
cube("pipe_tunnel_floor_wet", "l2_floor_wet", display="Wet Tunnel Floor")
cube("pipe_tunnel_floor_frost", "l2_floor_frost", display="Frosted Tunnel Floor")
cube("pipe_tunnel_grate", "l2_grate", display="Floor Grate", sound="metal")

PIPE_RUN = [
    (0, 9, 1, 16, 14, 8, "pipe"), (0, 8, 2, 16, 15, 7, "pipe"),
    (0, 10.5, 9, 16, 13.5, 13, "pipe2"), (0, 10, 9.5, 16, 14, 12.5, "pipe2"),
    (7, 7, 0, 9, 16, 14, "bracket"),
]
shaped("pipe_run", PIPE_RUN, {"pipe": "l2_pipe", "pipe2": "l2_pipe_green", "bracket": "l2_bracket"},
       display="Pipe Run", sound="metal", unbreakable=True)
shaped("pipe_run_hot", [(0, 9, 1, 16, 14, 8, "hot"), (0, 8, 2, 16, 15, 7, "hot"), (0, 10.5, 9, 16, 13.5, 13, "pipe2"),
                        (0, 10, 9.5, 16, 14, 12.5, "pipe2"), (7, 7, 0, 9, 16, 14, "bracket")],
       {"hot": "l2_pipe_hot", "pipe2": "l2_pipe_green", "bracket": "l2_bracket"}, display="Glowing Pipe Run",
       sound="metal", unbreakable=True, light=8, emissive=True)
shaped("pipe_run_frost", [(0, 9, 1, 16, 14, 8, "pipe"), (0, 8, 2, 16, 15, 7, "pipe"), (0, 10, 9, 16, 14, 13, "pipe"),
                          (7, 7, 0, 9, 16, 14, "pipe")],
       {"pipe": "l2_pipe_frost"}, display="Frosted Pipe Run", sound="metal", unbreakable=True)
shaped("pipe_run_low", [(0, 0, 1, 16, 5, 6, "pipe"), (7, 0, 0, 9, 6, 2, "bracket")],
       {"pipe": "l2_pipe_green", "bracket": "l2_bracket"}, display="Floor Pipe", sound="metal", unbreakable=True)
shaped("pipe_cross_low", [(0, 10, 4, 16, 15, 12, "pipe"), (0, 9, 5, 16, 16, 11, "pipe"), (3, 15, 7, 5, 16, 9, "bracket"),
                          (11, 15, 7, 13, 16, 9, "bracket")],
       {"pipe": "l2_pipe", "bracket": "l2_bracket"}, display="Low Pipe", sound="metal", unbreakable=True)
shaped("pipe_run_broken", [(0, 8, 1, 9, 15, 8, "pipe"), (11, 9, 2, 16, 14, 7, "pipe"), (0, 10, 9, 16, 14, 13, "pipe2")],
       {"pipe": "l2_pipe", "pipe2": "l2_pipe_green"}, display="Burst Pipe", sound="metal", unbreakable=True)

BULB = [
    (6, 15, 6, 10, 16, 10, "bracket"),
    (7.5, 10, 7.5, 8.5, 15, 8.5, "bracket"),
    (6.5, 5, 6.5, 9.5, 10, 9.5, "bulb"),
    (5, 4, 5, 11, 11, 11, "cage"),
]
shaped("caged_bulb", BULB, {"bracket": "l2_bracket", "bulb": "l2_bulb", "cage": "l2_cage"}, display="Caged Bulb",
       light=14, emissive=True, collides=False, sound="glass", unbreakable=True)
shaped("caged_bulb_dead", BULB, {"bracket": "l2_bracket", "bulb": "l2_bulb_dead", "cage": "l2_cage"},
       display="Dead Caged Bulb", collides=False, sound="glass", unbreakable=True)
shaped("caged_bulb_cold", BULB, {"bracket": "l2_bracket", "bulb": "l2_bulb_cold", "cage": "l2_cage"},
       display="Cold Caged Bulb", light=9, emissive=True, collides=False, sound="glass", unbreakable=True)

shaped("pressure_valve", [
    (7, 7, 0, 9, 9, 4, "stem"),
    (2, 2, 4, 14, 4, 5, "red"), (2, 12, 4, 14, 14, 5, "red"),
    (2, 4, 4, 4, 12, 5, "red"), (12, 4, 4, 14, 12, 5, "red"),
    (7, 4, 4, 9, 12, 5, "red"), (4, 7, 4, 12, 9, 5, "red"),
], {"stem": "l2_bracket", "red": "l2_valve"}, display="Pressure Valve", sound="metal", unbreakable=True)
shaped("pressure_gauge", [(4, 6, 0, 12, 14, 2, {"all": "rim", "south": "face"}), (7, 2, 0, 9, 6, 2, "rim")],
       {"rim": "l2_bracket", "face": "l2_gauge"}, display="Pressure Gauge", sound="metal", unbreakable=True,
       collides=False)

custom("scalding_riser", "com.noclip.block.L2ScaldingPipeBlock",
       box_model([(3, 0, 3, 13, 16, 13, "hot"), (2, 6, 2, 14, 8, 14, "ring")], {"hot": "l2_pipe_hot", "ring": "l2_bracket"}),
       display="Scalding Riser", sound="metal", unbreakable=True, light=6, emissive=True, render="cutout")
custom("steam_vent_grate", "com.noclip.block.L2SteamVentBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/l2_steam_grate"}},
       display="Steam Vent", sound="metal", unbreakable=True)

exit_block("maintenance_door", "l2_door", "DOOR", trigger="USE", solid=True, display="Maintenance Door", sound="metal")
exit_block("pipe_shaft_glow", "l2_shaft_glow", "FALL", display="Heat From Below", light=10, emissive=True)
exit_block("ceiling_hatch", "l2_hatch", "ASCEND", trigger="USE", solid=True, display="Ceiling Hatch", sound="metal")
ladder("pipe_tunnel_ladder", "l2_ladder", display="Service Ladder", unbreakable=True)
carpet("wanderer_bedroll", "l2_bedroll", display="Bedroll")

# ------------------------------------------------------------------ sound


def ring(seed, size=1.0, dur=1.5, gain=1.0):
    """Struck metal pipe: inharmonic partials with a noisy attack."""
    t = Y.t_axis(dur)
    r = Y.rng(seed)
    x = np.zeros_like(t)
    for k, f in enumerate((180, 471, 897, 1430, 2210)):
        ff = f / size * (0.97 + r.random() * 0.06)
        x += np.sin(2 * np.pi * ff * t + r.random() * 6) * np.exp(-t * (3 + k * 2.5) / max(size, 0.3)) / (k + 1)
    clank = Y.bp(Y.white(dur, seed + "c"), 300, 5000) * np.exp(-t * 60)
    return Y.norm(x + clank * 0.8, 0.9 * gain)


def knock_series(seed, n=5, spacing=0.32, accel=0.85, size=1.0, dur=4.0):
    out = np.zeros(int(dur * Y.SR))
    at = 0.1
    for k in range(n):
        Y.place(out, ring(f"{seed}{k}", size, 1.6), at, 0.6 + 0.4 * (k % 2))
        at += spacing
        spacing *= accel
    return out


def tunnel_loop():
    d = 26.0
    t = Y.t_axis(d)
    rumble = Y.lp(Y.brown(d, "l2rum"), 90) * 0.8
    boil = Y.bp(Y.pink(d, "l2boil"), 60, 400) * 0.25 * (0.7 + 0.3 * np.sin(2 * np.pi * t * 3 / d))
    hiss = Y.bp(Y.white(d, "l2hiss"), 3000, 9000) * 0.05 * (0.6 + 0.4 * np.sin(2 * np.pi * t * 2 / d + 1))
    drone = Y.drone(d, [55.0, 82.5], "l2dr", lfo=0.08) * 0.18
    x = rumble + boil + hiss + drone
    # thermal ticking: tiny metallic clicks at irregular times
    r = Y.rng("l2tick")
    for k in range(24):
        Y.place(x, ring(f"tk{k}", 0.25, 0.25, 0.05), r.random() * (d - 1), 0.08 + r.random() * 0.1)
    # one distant water hammer, far down the line
    far = Y.lp(knock_series("l2far", 4, 0.4, 0.9, 1.3), 1200)
    far = Y.reverb(far, 3.0, 0.7, "l2farv")
    Y.place(x, far, 11.0, 0.35)
    x = Y.loopable(Y.norm(x, 0.5), 1.0)
    return Y.stereoize(x, 0.0011), "stereo"


def add_knock(i):
    def f():
        x = knock_series(f"l2k{i}", 3 + i, 0.35 - i * 0.04, 0.9, 1.0 + i * 0.2)
        return Y.norm(Y.reverb(Y.lp(x, 2500), 2.8, 0.6, f"l2kv{i}"), 0.8)
    return f


def add_hiss(i):
    def f():
        d = 3.5
        t = Y.t_axis(d)
        env = np.clip(t / 0.15, 0, 1) * np.exp(-np.maximum(t - 0.3, 0) * (1.5 + i))
        x = Y.bp(Y.white(d, f"l2h{i}"), 1500 + i * 800, 9000) * env
        return Y.norm(Y.reverb(x, 2.0, 0.5, f"l2hv{i}"), 0.55)
    return f


def add_groan():
    d = 4.0
    t = Y.t_axis(d)
    f0 = 48 + 14 * np.sin(np.pi * t / d) + 3 * np.sin(2 * np.pi * 0.7 * t)
    ph = 2 * np.pi * np.cumsum(f0) / Y.SR
    x = (np.sin(ph) + 0.6 * np.sin(2.01 * ph) + 0.4 * np.sin(3.03 * ph)) * np.sin(np.pi * t / d) ** 1.2
    creak = Y.bp(Y.white(d, "l2gc"), 400, 1600) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t)) * 0.25
    return Y.norm(Y.reverb(x + creak * np.sin(np.pi * t / d), 3.0, 0.6, "l2gv"), 0.8)


def add_drip():
    out = np.zeros(int(2.5 * Y.SR))
    for k in range(3):
        n = int(0.1 * Y.SR)
        tt = np.arange(n) / Y.SR
        fr = 1700 - tt * 4000
        Y.place(out, np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 45), 0.2 + k * 0.6, 0.6)
    return Y.reverb(out, 2.2, 0.6, "l2drip")


def warn_knock():
    """Close, fast, accelerating water hammer: two seconds before the steam goes."""
    x = knock_series("l2warn", 9, 0.26, 0.86, 0.8, dur=2.6)
    t = Y.t_axis(2.6)
    groan = np.sin(2 * np.pi * np.cumsum(70 + 40 * t) / Y.SR) * np.clip(t / 2.6, 0, 1) * 0.4
    return Y.norm(Y.reverb(x + groan, 1.4, 0.35, "l2wv"), 0.95)


def burst():
    d = 3.2
    t = Y.t_axis(d)
    bang = Y.impact("l2bang", 0.8)
    x = np.zeros_like(t)
    Y.place(x, bang, 0.0, 0.9)
    jet = Y.bp(Y.white(d, "l2jet"), 900, 12000) * np.clip(t / 0.05, 0, 1) * np.exp(-np.maximum(t - 1.2, 0) * 2.2)
    roar = Y.lp(Y.white(d, "l2roar"), 500) * np.exp(-t * 1.2) * 0.5
    return Y.norm(Y.reverb(x + jet + roar, 1.6, 0.35, "l2bv"), 0.95)


def scald():
    d = 0.6
    t = Y.t_axis(d)
    return Y.norm(Y.bp(Y.white(d, "l2sc"), 2500, 10000) * np.exp(-t * 6), 0.7)


sound("ambient.level_2", [("ambient/level_2", tunnel_loop)], stream=True)
sound("ambient.level_2.additions",
      [(f"ambient/level_2_knock{i}", add_knock(i)) for i in range(3)] +
      [(f"ambient/level_2_hiss{i}", add_hiss(i)) for i in range(2)] +
      [("ambient/level_2_groan", add_groan), ("ambient/level_2_drip", add_drip)],
      subtitle="Pipes knock somewhere")
sound("event.level_2_knock", [("event/level_2_knock", warn_knock)], subtitle="The pipes are knocking")
sound("event.level_2_burst", [("event/level_2_burst", burst)], subtitle="Steam bursts")
sound("event.level_2_scald", [("event/level_2_scald", scald)], subtitle="Hot metal hisses")

level("level_2", fog="#3a2a1e", sky="#3a2a1e", ambient="ambient.level_2", additions="ambient.level_2.additions",
      additions_chance=0.006, fixed_time=18000, height=32, particle="minecraft:white_ash", particle_chance=0.004)
