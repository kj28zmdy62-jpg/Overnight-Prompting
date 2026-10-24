"""Gravity Rooms - a tower of bedrooms stacked on top of each other, each one turned a different way.
You fall through a hatch and the bed is on the wall, the rug is beside you, the door is underneath you."""
import numpy as np
from assets import cube, cube_faces, tex, exit_block, level, sound
import tex as T
import synth as Y
from content.rb_shared import wood, disc, line

S = 32


def wallpaper():
    """Dusty blue paper with a scatter of tiny sprigs: no stripes, so it has no up."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("gvwp", octaves=3, base=5) * 0.5 + 0.25, "#7a8aa0", "#8696ac", "#90a0b6")
    r = T.rng("gvsprig")
    for _ in range(6):
        cx, cy = r.random() * S, r.random() * S
        for k in range(3):
            a = r.random() * np.pi * 2
            px, py = int(cx + np.cos(a) * 1.5) % S, int(cy + np.sin(a) * 1.5) % S
            img[py, px] = T.hexc("#e8e0d0")
        img[int(cy) % S, int(cx) % S] = T.hexc("#c87a7a")
    return T.grain(img, "gvwp", 0.025)


def boards():
    img = wood("gvboard", "#6a4a2e", "#84603c", grain_dir="h", rings=4)
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.35)
    return T.grain(img, "gvboard", 0.03)


def ceiling():
    img = T.ramp(T.fbm("gvceil", octaves=2, base=6), "#d8d4ca", "#e4e0d6")
    return T.grain(img, "gvceil", 0.015)


def ceiling_rose():
    img = ceiling()
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img[(d > 9) & (d < 11)] = T.hexc("#b8b4aa")
    img[(d > 5) & (d < 6)] = T.hexc("#c4c0b6")
    img[d < 4] = T.hexc("#fff4d8")
    return img


def carpet():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("gvcarp", sigma=1.2), "#7a3a2e", "#8a4a36")
    border = (np.minimum(np.minimum(xx, 31 - xx), np.minimum(yy, 31 - yy)) < 3)
    img[border] = T.hexc("#c8a060")
    d = np.abs(xx - 15.5) + np.abs(yy - 15.5)
    img[(d > 6) & (d < 8)] = T.hexc("#3a2a4a")
    return T.grain(img, "gvcarp", 0.04)


def quilt():
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((xx // 8 + yy // 8) % 2 == 0)
    img = np.where(a[..., None], T.hexc("#a83a3a"), T.hexc("#e8dcc4"))
    img = T.shade(img, 1 - (((xx % 8) == 0) | ((yy % 8) == 0)) * 0.25)
    return T.grain(img, "gvquilt", 0.03)


def mattress():
    return T.grain(T.ramp(T.noise("gvmat", sigma=1.5), "#e0dace", "#ece6da"), "gvmat", 0.03)


def wardrobe():
    img = wood("gvward", "#4a301c", "#5e3e24", grain_dir="v", rings=3)
    img[:, 15:17] = T.hexc("#2a1a0e")
    img = T.rect(img, 12, 14, 14, 18, "#c8a050")
    img = T.rect(img, 18, 14, 20, 18, "#c8a050")
    return T.grain(img, "gvward", 0.02)


def clock():
    """A wall clock. It reads twelve o'clock in whichever direction is up for the room it is in."""
    img = wood("gvclk", "#4a301c", "#5e3e24")
    face = disc(S, S, 15.5, 15.5, 12)
    img[face] = T.hexc("#ece6d4")
    for k in range(12):
        a = k / 12 * 2 * np.pi
        img[int(round(15.5 - np.cos(a) * 10)), int(round(15.5 + np.sin(a) * 10))] = T.hexc("#2a2018")
    img = line(img, 15.5, 15.5, 15.5, 6, "#1a1410")
    img = line(img, 15.5, 15.5, 15.5, 9, "#1a1410")
    return img


def window():
    """Daylight. Real daylight, from a direction that cannot be right."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(np.clip(yy / 32.0, 0, 1), "#b8d8f0", "#e8f0f4")
    img[:, 15:17] = T.hexc("#f0ece4")
    img[15:17, :] = T.hexc("#f0ece4")
    img = T.border(img, "#e0dcd2", 2)
    return img


def shade():
    yy = np.mgrid[0:S, 0:S][0]
    return T.grain(T.ramp(np.clip(1 - np.abs(yy - 16) / 18.0, 0, 1), "#d8a860", "#fff0c0"), "gvshade", 0.02)


def door(top):
    img = wood("gvdoor" + str(top), "#5a3a22", "#6e4a2c", grain_dir="v", rings=3)
    img = T.border(img, "#2a1a10", 2)
    if not top:
        img = T.rect(img, 23, 2, 26, 6, "#c8a050")
    else:
        img = T.rect(img, 6, 6, 26, 26, "#000000", 0.15)
    return T.grain(img, "gvdoor" + str(top), 0.02)


def frame():
    return wood("gvframe", "#e8e2d4", "#f2ecde")


tex("gv_wallpaper", wallpaper)
tex("gv_boards", boards)
tex("gv_ceiling", ceiling)
tex("gv_ceiling_rose", ceiling_rose)
tex("gv_carpet", carpet)
tex("gv_quilt", quilt)
tex("gv_mattress", mattress)
tex("gv_wardrobe", wardrobe)
tex("gv_wood", lambda: wood("gvwood", "#5a3a22", "#6e4a2c"))
tex("gv_clock", clock)
tex("gv_window", window)
tex("gv_shade", shade)
tex("gv_door_low", lambda: door(False))
tex("gv_door_high", lambda: door(True))
tex("gv_frame", frame)

cube("gv_wallpaper", "gv_wallpaper", display="Bedroom Wallpaper", sound="wool")
cube("gv_boards", "gv_boards", display="Bedroom Floorboards", sound="wood")
cube("gv_ceiling", "gv_ceiling", display="Bedroom Ceiling", sound="stone", flat=True)
cube("gv_ceiling_light", "gv_ceiling_rose", display="Ceiling Light", light=14, emissive=True, flat=True, sound="glass")
cube("gv_carpet", "gv_carpet", display="Bedroom Rug", sound="wool")
cube("gv_quilt", "gv_quilt", display="Quilt", sound="wool")
cube("gv_mattress", "gv_mattress", display="Mattress", sound="wool")
cube("gv_wardrobe", "gv_wardrobe", display="Wardrobe", sound="wood")
cube("gv_wood", "gv_wood", display="Furniture", sound="wood")
cube("gv_clock", "gv_clock", display="Wall Clock", sound="wood")
cube("gv_window", "gv_window", display="Window", light=12, emissive=True, flat=True, sound="glass")
cube("gv_shade", "gv_shade", display="Lampshade", light=11, emissive=True, flat=True, sound="wool")
cube("gv_frame", "gv_frame", display="Door Frame", sound="wood")
exit_block("gv_door_low", "gv_door_low", "DOOR", trigger="USE", solid=True, display="The Way Out", sound="wood")
exit_block("gv_door_high", "gv_door_high", "DOOR", trigger="USE", solid=True, display="The Way Out", sound="wood")

# ------------------------------------------------------------------ sound


def shaft():
    """Wind down a long shaft, a house creaking as it turns, and the hum your inner ear makes when it gives up."""
    d = 26.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "gvwind"), 120, 900) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 13.0)) * 0.4
    drone = Y.drone(d, [49.0, 73.4], "gvdrone", lfo=1 / 13.0) * 0.18
    creaks = np.zeros_like(t)
    r = Y.rng("gvcr")
    for k in range(4):
        n = int(1.2 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = np.sin(2 * np.pi * np.cumsum(90 + 40 * tt + 15 * np.sin(2 * np.pi * 3 * tt)) / Y.SR) * (r.random(n) > 0.6)
        Y.place(creaks, Y.bp(c, 150, 1500) * np.sin(np.pi * tt / 1.2), 2 + k * 6 + r.random() * 2, 0.25)
    x = Y.loopable(Y.norm(wind + drone + Y.reverb(creaks, 2.0, 0.6, "gvcrv")[: len(t)], 0.45), 1.5)
    return Y.stereoize(x, 0.003), "stereo"


def slide():
    d = 2.2
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "gvsl"), 150, 1200) * np.sin(np.pi * t / d) * (0.6 + 0.4 * np.sin(2 * np.pi * 19 * t))
    out = np.zeros(int(2.8 * Y.SR))
    Y.place(out, x, 0, 0.6)
    Y.place(out, Y.impact("gvsli", 0.6), 2.1, 0.8)
    return Y.reverb(Y.lp(out, 1500), 1.8, 0.5, "gvsl")


def rolling_glass():
    d = 3.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for k in range(12):
        n = int(0.08 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(x, np.sin(2 * np.pi * (2600 + 200 * (k % 3)) * tt) * np.exp(-tt * 60), 0.1 + k * 0.22 + 0.01 * k * k, 0.3)
    return Y.reverb(x, 1.5, 0.4, "gvrg")


def shift():
    """The room decides which way is down."""
    d = 1.6
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * (60 - 25 * t) * t) * np.exp(-t * 2.5) * (1 - np.exp(-t * 40))
    x += Y.lp(Y.white(d, "gvshift"), 400) * np.exp(-t * 4) * 0.5
    return Y.norm(Y.reverb(x, 1.5, 0.4, "gvshift"), 0.8), "stereo"


sound("ambient.gravity_rooms", [("ambient/gravity_rooms", shaft)], stream=True)
sound("ambient.gravity_rooms.additions", [("ambient/gravity_rooms_slide", slide), ("ambient/gravity_rooms_glass", rolling_glass)],
      subtitle="Something slides across a wall")
sound("event.gravity_rooms_shift", [("event/gravity_rooms_shift", shift)], subtitle="Down changes")

level("gravity_rooms", fog="#20242c", sky="#20242c", ambient="ambient.gravity_rooms",
      additions="ambient.gravity_rooms.additions", additions_chance=0.004, fixed_time=6000, height=96)
