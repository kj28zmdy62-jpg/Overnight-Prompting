"""Flooded Lobby - Level 0 under knee-deep water. The lights still work. Some of them are in the water."""
import numpy as np
from assets import cube, tex, shaped, exit_block, level, sound, flicker_light
import tex as T
import synth as Y
from content.deepend_audio import wrapverb, fit
from content.level_0 import wallpaper_base_pattern, carpet as l0_carpet, ceiling as l0_ceiling, light_on, light_off

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def tide_wall():
    """Wallpaper just above the waterline: a brown tide mark, and the paper bubbling where it drank."""
    img = wallpaper_base_pattern("flw")
    tide = np.clip(1 - np.abs(yy - 27) / 2.5, 0, 1) * (0.7 + 0.3 * T.noise("flt", sigma=1))
    img = T.blend(img, T.solid("#5a4a24"), tide * 0.6)
    wick = np.clip((yy - 14) / 14.0, 0, 1) * (0.5 + 0.5 * T.noise("flwick", sigma=2))
    img = T.blend(img, T.solid("#8a7a3a"), wick * 0.45)
    bubbles = (T.noise("flbub", sigma=0.7) > 0.8) & (yy > 16)
    return T.shade(img, 1 - bubbles * 0.12)


def wall_upper():
    img = wallpaper_base_pattern("flwu")
    return T.stains(img, "flwus", "#7d6a2c", count=2, strength=0.3, size=8)


def drowned_base():
    img = wallpaper_base_pattern("flb")
    img = T.blend(img, T.solid("#4a3e1a"), 0.55)
    board = T.ramp(T.fbm("flboard", octaves=3, base=2), "#3a2a18", "#4a3620")
    img[S - 8:] = board[S - 8:]
    return T.grain(img, "flb", 0.04)


def drowned_carpet():
    img = l0_carpet()
    img = T.shade(img, 0.62)
    silt = T.noise("flsilt", sigma=2.5)
    return T.blend(img, T.solid("#4a4428"), np.clip(silt - 0.4, 0, 1) * 0.7)


def ceiling():
    img = l0_ceiling()
    img = T.stains(img, "flcs", "#7a5a28", count=3, strength=0.55, size=9)
    sag = np.clip(1 - np.sqrt((xx - 16) ** 2 + (yy - 16) ** 2) / 14, 0, 1)
    return T.shade(img, 1 - sag * 0.12)


def fallen_on():
    img = light_on()
    crack = np.zeros((S, S))
    r = T.rng("flcrack")
    x, y = 6.0, 4.0
    for i in range(40):
        crack[int(y) % S, int(x) % S] = 1
        x += 0.6 + r.random() * 0.4
        y += (r.random() - 0.3) * 1.2
    return T.blend(img, T.solid("#9adfff"), crack * 0.8)


def fallen_off():
    return light_off()


def drain_dark():
    img = T.solid("#020204")
    return T.grain(img, "fldd", 0.01)


def float_tile():
    return l0_ceiling()


tex("fl_wall_tide", tide_wall)
tex("fl_wall_upper", wall_upper)
tex("fl_base", drowned_base)
tex("fl_carpet", drowned_carpet)
tex("fl_ceiling", ceiling)
tex("fl_fallen_on", fallen_on)
tex("fl_fallen_off", fallen_off)
tex("fl_drain", drain_dark)
tex("fl_float_tile", float_tile)

cube("flooded_wallpaper_tide", "fl_wall_tide", display="Tide-Marked Wallpaper", sound="wool")
cube("flooded_wallpaper", "fl_wall_upper", display="Damp Yellow Wallpaper", sound="wool")
cube("flooded_baseboard", "fl_base", display="Drowned Baseboard", sound="wool")
cube("flooded_carpet", "fl_carpet", display="Drowned Carpet", sound="mud")
cube("flooded_ceiling_tile", "fl_ceiling", display="Water-Damaged Ceiling Tile", sound="wool", flat=True)
flicker_light("flooded_fallen_light", "fl_fallen_on", "fl_fallen_off", "fl_fallen_off", light=12, flicker=0.0,
              face="up", display="Fallen Light Fixture (Live)")
exit_block("flooded_drain", "fl_drain", "WATER", display="Something Deeper")
shaped("flooded_floating_tile", [(1, 0, 2, 15, 1, 14, {"all": "t"})], {"t": "fl_float_tile"},
       display="Floating Ceiling Tile", collides=False, sound="wool", unbreakable=True)


def flooded_hum():
    """Level 0's hum, but wet: the ballast buzz through water, drips, and a slow slosh against the walls."""
    d = 26.0
    t = Y.t_axis(d)
    n = len(t)
    hum = Y.lp(Y.hum(d, 60.0, "flhum", buzz=0.45), 2400) * 0.35
    slosh = Y.lp(Y.pink(d, "flslosh"), 600) * 0.25 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 6.5) ** 2)
    drips = np.zeros(n)
    r = Y.rng("fldrips")
    for k in range(24):
        nn = int(0.09 * Y.SR)
        tt = np.arange(nn) / Y.SR
        fr = 900 + r.random() * 900 - tt * 3000
        pl = np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 45)
        Y.place(drips, pl, r.random() * (d - 0.2), 0.2 + r.random() * 0.2)
    x = wrapverb(slosh + drips, 2.2, 0.55, "flroom", damp=3500) + hum
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.2), 0.0015), "stereo"


def far_splash():
    out = np.zeros(int(3.5 * Y.SR))
    n = int(0.7 * Y.SR)
    tt = np.arange(n) / Y.SR
    s = Y.bp(Y.white(0.7, "flsp"), 300, 4000)[:n] * np.exp(-tt * 6)
    Y.place(out, s, 0.05)
    return Y.reverb(Y.lp(out, 2000), 3.0, 0.75, "flsp")


def crackle():
    d = 1.5
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("flcrk")
    for k in range(30):
        nn = int(0.01 * Y.SR)
        Y.place(out, Y.bp(Y.white(0.01, f"flc{k}"), 2000, 9000)[:nn], r.random() * 1.2, 0.3 + r.random() * 0.6)
    out += Y.hum(d, 60.0, "flcrkh", buzz=1.0) * 0.2 * np.exp(-np.arange(len(out)) / Y.SR * 2)
    return Y.norm(out, 0.8)


def surfacing():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.lp(Y.brown(d, "flsurf"), 400) * np.exp(-((t - 0.5) ** 2) / 0.05)
    x += Y.bp(Y.white(d, "flsurf2"), 500, 3000) * np.exp(-((t - 0.7) ** 2) / 0.02) * 0.5
    return Y.reverb(Y.norm(x, 0.7), 2.0, 0.6, "flsurf")


def zap():
    d = 0.8
    t = Y.t_axis(d)
    x = Y.hum(d, 60.0, "flzap", buzz=1.0) * np.exp(-t * 4) + Y.bp(Y.white(d, "flzap2"), 1500, 9000) * np.exp(-t * 8)
    return Y.norm(x, 0.9)


def brush():
    d = 1.6
    t = Y.t_axis(d)
    x = Y.lp(Y.pink(d, "flbrush"), 900) * np.sin(np.pi * t / d) ** 2
    return Y.norm(x, 0.7)


sound("ambient.flooded_lobby", [("ambient/flooded_lobby", flooded_hum)], stream=True)
sound("ambient.flooded_lobby.additions", [("ambient/flooded_lobby_splash", far_splash),
                                         ("ambient/flooded_lobby_crackle", crackle),
                                         ("ambient/flooded_lobby_surfacing", surfacing)],
      subtitle="Something in the water")
sound("event.flooded_lobby_zap", [("event/flooded_lobby_zap", zap)], subtitle="The water is live")
sound("event.flooded_lobby_brush", [("event/flooded_lobby_brush", brush)], subtitle="Something brushes past")

level("flooded_lobby", fog="#8a8448", sky="#8a8448", water="#8a8a3a", water_fog="#3a3a18",
      ambient="ambient.flooded_lobby", additions="ambient.flooded_lobby.additions", additions_chance=0.005,
      fixed_time=6000, height=64, particle="minecraft:falling_water", particle_chance=0.0015)
