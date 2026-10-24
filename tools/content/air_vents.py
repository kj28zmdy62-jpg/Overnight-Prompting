"""Air Vents - crawl-only galvanized duct maze. Fans strobe light through their blades.
Something else uses the ducts."""
import numpy as np
from assets import cube, cube_faces, tex, exit_block, level, sound, custom, lang, entity_model, entity_tex, part, \
    BLOCK_MODELS, ITEM_MODELS, shaped
from entity_paint import Canvas, skin
from content.ind_kit import anim_tex, text, rivets, vstreaks, glow_spot
import tex as T
import synth as Y

S = 32


def spangle(seed, lo="#7d858c", hi="#a3abb2", cells=16):
    """Galvanized zinc: crystalline spangle cells of slightly different sheen."""
    r = T.rng(seed)
    pts = r.random((cells, 2)) * S
    val = r.random(cells)
    yy, xx = np.mgrid[0:S, 0:S]
    best = np.full((S, S), 1e9)
    out = np.zeros((S, S))
    for (px, py), v in zip(pts, val):
        dx = np.minimum(np.abs(xx - px), S - np.abs(xx - px))
        dy = np.minimum(np.abs(yy - py), S - np.abs(yy - py))
        d = dx * dx + dy * dy
        m = d < best
        out[m] = v
        best[m] = d[m]
    n = out * 0.55 + T.noise(seed + "n", sigma=1.2) * 0.45
    return T.grain(T.ramp(n, lo, hi), seed, 0.035)


def duct(seed="avd", seam=True):
    img = spangle(seed)
    yy, xx = np.mgrid[0:S, 0:S]
    if seam:
        # lap joint every 16px: a raised lip with a rivet row
        lip = (xx % 16 == 0).astype(float)
        img = T.shade(img, 1 + lip * 0.18)
        img = T.shade(img, 1 - (xx % 16 == 1).astype(float) * 0.22)
        img = rivets(img, [(2, y) for y in range(2, 32, 6)] + [(18, y) for y in range(5, 32, 6)], "#c4cad0", "#3b4046")
    # dust settles on everything, darker toward the bottom of each panel
    dust = T.fbm(seed + "dust", octaves=3, base=4)
    img = T.blend(img, T.solid("#4e5052"), np.clip(dust * 0.5 - 0.15, 0, 0.3))
    return img


def duct_dented():
    img = duct("avdd")
    yy, xx = np.mgrid[0:S, 0:S]
    # pushed outward from the inside: concentric highlight/shadow
    for cx, cy, rad in ((12, 14, 9), (24, 22, 6)):
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / rad
        bowl = np.clip(1 - d, 0, 1)
        lightside = ((xx - cx) + (yy - cy)) / rad
        img = T.shade(img, 1 - bowl * 0.25 * (1 + lightside * 0.8))
    return T.cracks(img, "avddc", "#3a3a36", 1, 0.4)


def duct_rusted():
    img = duct("avdr")
    img = vstreaks(img, "avdrs", "#6a3c1c", count=6, strength=0.55, length=(8, 26))
    return T.stains(img, "avdrst", "#4a3018", count=2, strength=0.5, size=7)


def duct_scratched():
    img = duct("avds", seam=False)
    r = T.rng("avdsc")
    for k in range(4):
        x0 = 6 + k * 6 + int(r.random() * 2)
        for y in range(3, 29):
            x = x0 + int((y - 3) * 0.35 + np.sin(y / 3.0))
            img[y, x % S] = T.hexc("#dfe5ea")
            img[y, (x + 1) % S] = T.hexc("#40454a")
    return img


def duct_message(msg_lines):
    def f():
        img = duct("avdm" + msg_lines[0], seam=False)
        y = 16 - len(msg_lines) * 4
        for ln in msg_lines:
            w = len(ln) * 4 - 1
            img = text(img, ln, (S - w) // 2, y, "#e2e7ec", 1)
            img = text(img, ln, (S - w) // 2 + 1, y + 1, "#383c40", 1, 0.6)
            y += 8
        return img
    return f


def duct_floor(seed="avf"):
    img = spangle(seed, "#6b7279", "#8a9198")
    dust = T.ramp(T.fbm(seed + "d", octaves=4, base=3), "#4c463c", "#6a6152")
    yy, xx = np.mgrid[0:S, 0:S]
    # two crawl lanes where knees and elbows wiped the dust away
    lane = np.exp(-((xx - 9) ** 2) / 10.0) + np.exp(-((xx - 23) ** 2) / 10.0)
    wobble = T.noise(seed + "w", sigma=2)
    m = np.clip(0.85 - lane * 0.7 * (0.6 + wobble * 0.6), 0, 1)
    return T.grain(T.blend(img, dust, m), seed, 0.04)


def duct_floor_hands():
    img = duct_floor("avfh")
    yy, xx = np.mgrid[0:S, 0:S]
    metal = spangle("avfhm", "#727a81", "#959ca3")
    m = np.zeros((S, S))
    for (cx, cy, rot) in ((9, 8, -0.3), (22, 20, 0.25), (8, 27, -0.2)):
        c, s = np.cos(rot), np.sin(rot)
        u = (xx - cx) * c - (yy - cy) * s
        v = (xx - cx) * s + (yy - cy) * c
        palm = (u / 3.0) ** 2 + (v / 3.6) ** 2 < 1
        fingers = np.zeros_like(palm)
        for fx in (-2.2, -0.8, 0.7, 2.1):
            fingers |= (np.abs(u - fx) < 0.6) & (v < -2.5) & (v > -7.5 + abs(fx) * 0.8)
        m = np.maximum(m, (palm | fingers).astype(float))
    return T.blend(img, metal, m * 0.85)


def duct_floor_drag():
    img = duct_floor("avfd")
    yy, xx = np.mgrid[0:S, 0:S]
    trail = (np.abs(xx - 16 - np.sin(yy / 5.0) * 2) < 4).astype(float)
    img = T.blend(img, spangle("avfdm", "#727a81", "#959ca3"), trail * 0.75)
    img = T.blend(img, T.solid("#2a1410"), (np.abs(xx - 15 - np.sin(yy / 5.0) * 2) < 1.2) *
                  (T.noise("avfdb", sigma=1) > 0.35) * 0.6)
    return img


def duct_ceiling():
    return T.shade(duct("avc"), 0.82)


def flex():
    yy, xx = np.mgrid[0:S, 0:S]
    rib = np.sin(yy / 2.0 * np.pi) * 0.5 + 0.5
    crinkle = T.noise("avflx", sigma=0.6)
    n = rib * 0.6 + crinkle * 0.4
    img = T.ramp(n, "#5c6066", "#b8bec4", "#dde2e6")
    return T.grain(T.stains(img, "avflxs", "#55503f", count=2, strength=0.3, size=8), "avflx", 0.03)


def fan_frame(i, n, lit=True, floor=False):
    """A fan seen through its guard, lit from behind. Blades turn 1/n of a pitch per frame."""
    yy, xx = np.mgrid[0:S, 0:S]
    cx = cy = 15.5
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    ang = np.arctan2(yy - cy, xx - cx)
    back = T.ramp(T.noise("avfanb", sigma=2), "#fff2d8", "#fffaf0") if lit else T.solid("#3c3a36")
    if lit:
        back = T.shade(back, 0.75 + 0.25 * np.clip(1 - d / 16, 0, 1))
    blades = 5
    phase = 2 * np.pi * i / (n * blades)
    a = (ang - phase - d * 0.045) % (2 * np.pi / blades)
    blade = (a < (2 * np.pi / blades) * 0.36) & (d > 3) & (d < 14.5)
    blade_col = T.ramp(T.noise("avfanbl", sigma=1), "#1d1f22", "#2c2f33") if lit else T.solid("#62676d")
    img = back.copy()
    # motion blur: a faint second blade position makes the spin read
    if lit:
        a2 = (ang - phase + 0.25 - d * 0.045) % (2 * np.pi / blades)
        ghost = (a2 < (2 * np.pi / blades) * 0.36) & (d > 3) & (d < 14.5)
        img = T.blend(img, blade_col, ghost.astype(float) * 0.3)
    img = T.blend(img, blade_col, blade.astype(float))
    hub = d < 3.2
    img[hub] = T.hexc("#3a3d42") if lit else T.hexc("#5a5e63")
    img[(d < 1.2)] = T.hexc("#8a9096")
    # guard: concentric rings and a square housing
    rings = (np.abs(d % 6 - 3) < 0.4) & (d > 5) & (d < 15)
    img = T.blend(img, T.solid("#50565c"), rings.astype(float) * 0.8)
    spokes = ((np.abs(xx - cx) < 0.6) | (np.abs(yy - cy) < 0.6)) & (d < 15)
    img = T.blend(img, T.solid("#50565c"), spokes.astype(float) * 0.8)
    housing = d >= 15
    img[housing] = spangle("avfanh", "#5f666d", "#7c838a")[housing]
    if floor:
        img = T.shade(img, 0.85)
        g = ((xx % 16) == 0) | ((xx % 16) == 1) | ((yy % 16) == 0)
        img = T.blend(img, T.solid("#59606a"), g.astype(float))
    img = T.border(img, "#3b4045", 1)
    return img


def louver():
    yy, xx = np.mgrid[0:S, 0:S]
    warm = T.ramp(T.noise("avlv", sigma=3), "#ffcc88", "#ffe2b0")
    slat = ((yy % 5) < 2)
    img = np.where(slat[..., None], T.ramp(T.noise("avlvs", sigma=1), "#2b2e31", "#3a3e42"), warm)
    img = T.border(img, "#7a8187", 2)
    return T.grain(img, "avlv", 0.02)


def ceiling_light():
    img = T.ramp(T.noise("avcl", sigma=2), "#d9eaf4", "#f2fbff")
    yy, xx = np.mgrid[0:S, 0:S]
    slots = ((xx % 6) < 2) & (yy > 4) & (yy < 27)
    img = np.where(slots[..., None], T.solid("#30353a"), img)
    img = T.border(img, "#848b92", 3)
    return img


def floor_grille():
    yy, xx = np.mgrid[0:S, 0:S]
    img = spangle("avfg", "#646b72", "#8a9198")
    a = (((xx % 6) < 2) | ((yy % 8) < 2)).astype(float)
    a[:2, :] = a[-2:, :] = 1
    a[:, :2] = a[:, -2:] = 1
    img = rivets(img, [(1, 1), (30, 1), (1, 30), (30, 30)], "#c4cad0", "#30343a")
    return img, a


def access_panel():
    img = spangle("avap", "#7a828a", "#9aa2aa")
    img = T.border(img, "#4a5056", 2)
    img = T.border(img, "#a8b0b6", 1)
    for (x, y) in ((4, 4), (27, 4), (4, 27), (27, 27)):
        img = T.rect(img, x - 1, y - 1, x + 2, y + 2, "#2c3034")
        img[y, x] = T.hexc("#b8bec4")
    img = text(img, "ACCESS", 5, 11, "#26292c")
    img = text(img, "NO ENTRY", 1, 18, "#7a1e18")
    return T.stains(img, "avaps", "#4e463a", 1, 0.3, 6)


tex("av_duct", lambda: duct())
tex("av_duct_dented", duct_dented)
tex("av_duct_rusted", duct_rusted)
tex("av_duct_scratched", duct_scratched)
tex("av_duct_msg_stop", duct_message(["DONT", "STOP"]))
tex("av_duct_msg_light", duct_message(["STAY", "IN THE", "LIGHT"]))
tex("av_duct_msg_hears", duct_message(["IT", "HEARS", "YOU"]))
tex("av_floor", lambda: duct_floor())
tex("av_floor_hands", duct_floor_hands)
tex("av_floor_drag", duct_floor_drag)
tex("av_ceiling", duct_ceiling)
tex("av_flex", flex)
anim_tex("av_fan", lambda i: fan_frame(i, 6), 6, frametime=1)
tex("av_fan_off", lambda: fan_frame(0, 6, lit=False))
anim_tex("av_floor_fan", lambda i: fan_frame(i, 6, floor=True), 6, frametime=1)
tex("av_louver", louver)
tex("av_ceiling_light", ceiling_light)
tex("av_floor_grille", floor_grille)
tex("av_access_panel", access_panel)

cube("av_duct_wall", "av_duct", display="Galvanized Duct", sound="metal")
cube("av_duct_dented", "av_duct_dented", display="Dented Duct", sound="metal")
cube("av_duct_rusted", "av_duct_rusted", display="Rusted Duct", sound="metal")
cube("av_duct_scratched", "av_duct_scratched", display="Scratched Duct", sound="metal")
cube("av_duct_msg_stop", "av_duct_msg_stop", display="Scratched Duct", sound="metal")
cube("av_duct_msg_light", "av_duct_msg_light", display="Scratched Duct", sound="metal")
cube("av_duct_msg_hears", "av_duct_msg_hears", display="Scratched Duct", sound="metal")
cube_faces("av_duct_floor", top="av_floor", bottom="av_duct", side="av_duct", display="Dusty Duct Floor", sound="metal")
cube_faces("av_duct_floor_hands", top="av_floor_hands", bottom="av_duct", side="av_duct", display="Handprints", sound="metal")
cube_faces("av_duct_floor_drag", top="av_floor_drag", bottom="av_duct", side="av_duct", display="Drag Marks", sound="metal")
cube("av_duct_ceiling", "av_ceiling", display="Duct Ceiling", sound="metal", flat=True)
cube("av_flex_duct", "av_flex", display="Flex Duct", sound="wool")
cube_faces("av_floor_fan", top="av_floor_fan", bottom="av_duct", side="av_duct", display="Floor Fan", sound="metal",
           light=11, emissive=True, flat=True)
cube_faces("av_ceiling_fan", bottom="av_fan", top="av_ceiling", side="av_duct", display="Ceiling Fan",
           sound="metal", light=12, emissive=True, flat=True)
cube_faces("av_ceiling_light", bottom="av_ceiling_light", top="av_ceiling", side="av_ceiling",
           display="Duct Lamp", sound="glass", light=11, emissive=True, flat=True)
cube("av_louver", "av_louver", display="Lit Louver", sound="metal", light=7, emissive=True, flat=True)
exit_block("av_floor_grille", "av_floor_grille", "FALL", trigger="USE", solid=True, render="cutout",
           display="Loose Floor Grille", sound="metal")
exit_block("av_access_panel", "av_access_panel", "DOOR", trigger="USE", solid=True, display="Access Panel",
           sound="metal")

# booster fan: spins (lit, hurts) or stands still (dark, passable); the level behaviour cycles it
BLOCK_MODELS["av_vent_fan_off"] = {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/av_fan_off"}}
custom("av_vent_fan", "com.noclip.block.VentFanBlock",
       {"parent": "noclip:base/cube_flat", "textures": {k: "noclip:block/av_fan" for k in
                                                        ("down", "up", "north", "south", "west", "east", "particle")}},
       blockstate={"variants": {"lit=true": {"model": "noclip:block/av_vent_fan"},
                                "lit=false": {"model": "noclip:block/av_vent_fan_off"}}},
       display="Booster Fan", sound="metal", unbreakable=True)

# ------------------------------------------------------------------ entity: the duct crawler


def crawler_parts():
    return [
        part("body", (0, 16, 0), cubes=[(0, 0, -2.5, -1.5, -7, 5, 3, 14)]),
        part("head", (0, 16, -7), cubes=[(0, 20, -2.5, -2.5, -5, 5, 5, 5)]),
        part("jaw", (0, 1.5, -2), parent="head", cubes=[(0, 32, -1.5, 0, -3, 3, 4, 3)]),
        part("leg_fl", (2.6, 16, -5), rot=(0, 0, -0.35), cubes=[(24, 20, -1, -1, -1, 2, 9, 2)]),
        part("leg_fr", (-2.6, 16, -5), rot=(0, 0, 0.35), cubes=[(32, 20, -1, -1, -1, 2, 9, 2)]),
        part("leg_bl", (2.4, 17, 5), rot=(0, 0, -0.3), cubes=[(40, 20, -1, -1, -1, 2, 8, 2)]),
        part("leg_br", (-2.4, 17, 5), rot=(0, 0, 0.3), cubes=[(48, 20, -1, -1, -1, 2, 8, 2)]),
    ]


def crawler_tex():
    c = Canvas(64, 64)
    hide = skin("avcr", "#a9a69a", "#c8c4b6", sigma=0.6, spots="#6e6a60")

    def back(w, h):
        img = hide(w, h)
        # vertebrae pushing through the skin
        for y in range(0, h, 2):
            img[y, w // 2 - 1:w // 2 + 1] = T.hexc("#e2ded2")
            if y + 1 < h:
                img[y + 1, w // 2 - 1:w // 2 + 1] = T.hexc("#7a766a")
        return img

    def face(w, h):
        img = hide(w, h)
        img[1:3, 0:2] = T.hexc("#050505")
        img[1:3, 3:5] = T.hexc("#050505")
        img[3:, 2] = T.hexc("#1a0c0a")
        return img

    c.box(0, 0, 5, 3, 14, all=hide, up=back)
    c.box(0, 20, 5, 5, 5, all=hide, front=face)
    c.box(0, 32, 3, 4, 3, all=lambda w, h: T.solid("#2a1210", shape=(h, w)))
    for u in (24, 32):
        c.box(u, 20, 2, 9, 2, all=hide)
    for u in (40, 48):
        c.box(u, 20, 2, 8, 2, all=hide)
    return c.out()


entity_model("duct_crawler", crawler_parts())
entity_tex("duct_crawler", crawler_tex)
lang("entity.noclip.duct_crawler", "Duct Crawler")
lang("item.noclip.duct_crawler_spawn_egg", "Duct Crawler Spawn Egg")
ITEM_MODELS["duct_crawler_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def duct_air():
    d = 24.0
    t = Y.t_axis(d)
    rumble = Y.lp(Y.brown(d, "avrum"), 90) * 0.7
    # air through a long sheet-metal pipe: resonant bands of noise
    p = Y.pink(d, "avair")
    tube = sum(Y.bp(p, f * 0.94, f * 1.06) * g for f, g in ((118, 1.0), (236, 0.6), (354, 0.35), (472, 0.2)))
    swell = 0.75 + 0.25 * np.sin(2 * np.pi * t / 12.0) + 0.1 * np.sin(2 * np.pi * t / 4.0)
    hiss = Y.bp(Y.white(d, "avhiss"), 2000, 7000) * 0.03 * swell
    fan = Y.hum(d, 25.0, "avfan", buzz=0.0, harmonics=(1, 2, 3, 5)) * 0.08
    x = rumble + tube * 1.8 * swell + hiss + Y.lp(fan, 400)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 1.5), 0.0015), "stereo"


def oilcan(i):
    """Sheet metal flexing with heat: a deep 'bwong'."""
    def f():
        d = 2.2
        t = Y.t_axis(d)
        f0 = 140 + i * 37
        x = np.sin(2 * np.pi * f0 * t * (1 - 0.04 * t)) * np.exp(-t * 3.5)
        x += 0.5 * np.sin(2 * np.pi * f0 * 2.76 * t) * np.exp(-t * 6)
        click = Y.bp(Y.white(0.03, f"oc{i}"), 800, 6000)
        out = np.zeros(len(t))
        Y.place(out, click, 0.0, 0.8)
        out += x * 0.7
        return Y.norm(Y.reverb(Y.lp(out, 2400), 1.6, 0.45, f"oc{i}"), 0.7)
    return f


def distant_knocks(i):
    """Something crawling, far down a duct: a run of knocks that drifts closer, then stops."""
    def f():
        out = np.zeros(int(4.5 * Y.SR))
        r = Y.rng(f"dk{i}")
        tt = 0.1
        for k in range(6 + i):
            tt += 0.22 + r.random() * 0.25
            g = 0.3 + k * 0.1
            Y.place(out, Y.impact(f"dk{i}{k}", 0.35, ring=0.25), tt, g)
        return Y.norm(Y.reverb(Y.lp(out, 1500 - i * 200), 2.2, 0.6, f"dk{i}"), 0.6)
    return f


def fan_spool():
    d = 5.0
    t = Y.t_axis(d)
    rate = np.clip(t / 2.5, 0, 1) * np.clip((d - t) / 1.5, 0, 1)
    f0 = 8 + 30 * rate
    ph = 2 * np.pi * np.cumsum(f0) / Y.SR
    chop = 0.5 + 0.5 * np.sin(ph * 5)
    x = Y.bp(Y.pink(d, "avsp"), 150, 1800) * chop * rate
    x += np.sin(ph * 5 * 4) * 0.15 * rate
    return Y.norm(Y.reverb(x, 1.5, 0.4, "avsp"), 0.6)


def scratching():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "avscr"), 1500, 7000) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t)) ** 3
    x *= np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(x, 1.2, 0.5, "avscr"), 0.5)


def surge():
    d = 7.0
    t = Y.t_axis(d)
    env = np.clip(t / 2.0, 0, 1) ** 2 * np.clip((d - t) / 2.5, 0, 1)
    x = Y.bp(Y.pink(d, "avsg"), 80, 3000) * env
    x += Y.lp(Y.brown(d, "avsg2"), 120) * env * 1.4
    whistle = np.sin(2 * np.pi * np.cumsum(300 + 500 * env) / Y.SR) * 0.06 * env
    thunk = np.zeros(len(t))
    Y.place(thunk, Y.impact("avsgk", 1.0, ring=0.4), 0.05, 0.8)
    return Y.norm(Y.reverb(x + whistle + thunk, 2.5, 0.5, "avsg"), 0.9)


def crawler_knock(i):
    def f():
        out = np.zeros(int(1.4 * Y.SR))
        for k in range(3 + i):
            Y.place(out, Y.impact(f"ck{i}{k}", 0.3, ring=0.5), 0.05 + k * (0.13 + 0.04 * i), 0.6 + 0.1 * k)
        return Y.norm(Y.lp(out, 2600), 0.8)
    return f


def crawler_hiss():
    d = 1.4
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "crh"), 1800, 9000) * np.exp(-t * 2.2) * (1 - np.exp(-t * 40))
    x += Y.bp(Y.white(d, "crh2"), 300, 900) * 0.4 * np.exp(-t * 3) * (0.6 + 0.4 * np.sin(2 * np.pi * 37 * t))
    return Y.norm(x, 0.9)


def crawler_hurt():
    d = 0.6
    t = Y.t_axis(d)
    f0 = 900 - 500 * t / d
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.exp(-t * 7) + Y.bp(Y.white(d, "crhu"), 1000, 5000) * np.exp(-t * 9) * 0.5
    return Y.norm(x, 0.8)


sound("ambient.air_vents", [("ambient/air_vents", duct_air)], stream=True)
sound("ambient.air_vents.additions",
      [(f"ambient/air_vents_oilcan{i}", oilcan(i)) for i in range(3)] +
      [(f"ambient/air_vents_knocks{i}", distant_knocks(i)) for i in range(2)] +
      [("ambient/air_vents_spool", fan_spool), ("ambient/air_vents_scratch", scratching)],
      subtitle="The ducts creak")
sound("event.air_vents_fan", [("event/air_vents_fan", fan_spool)], subtitle="A fan spins up")
sound("event.air_vents_surge", [("event/air_vents_surge", surge)], subtitle="Air rushes through the ducts")
sound("entity.duct_crawler.knock", [(f"entity/duct_crawler_knock{i}", crawler_knock(i)) for i in range(3)],
      subtitle="Knocking inside the duct")
sound("entity.duct_crawler.hiss", [("entity/duct_crawler_hiss", crawler_hiss)], subtitle="Something hisses")
sound("entity.duct_crawler.hurt", [("entity/duct_crawler_hurt", crawler_hurt)], subtitle="Duct Crawler hurts")

level("air_vents", fog="#0b0c0d", sky="#0b0c0d", ambient="ambient.air_vents", additions="ambient.air_vents.additions",
      additions_chance=0.006, fixed_time=18000, height=48, particle="minecraft:white_ash", particle_chance=0.004)
