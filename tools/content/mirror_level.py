"""Mirror Level - an endless dressing-room gallery split down the middle by glass. On the far side, everything
is mirrored, including you; your reflection lags behind. It can use doors that are not on your side."""
import numpy as np
from assets import cube, tex, shaped, pane, level, sound, lang, ITEM_MODELS
import tex as T
import synth as Y
from content.rb_shared import text, text_center, wood, ellipse, disc

S = 32


def wallpaper():
    yy, xx = np.mgrid[0:S, 0:S]
    base = T.ramp(T.fbm("mrwp", octaves=3, base=5) * 0.5 + 0.25, "#a8aeb4", "#b8bec4", "#c4cad0")
    pin = ((xx % 8) == 0).astype(float) * 0.12
    d = np.abs((xx % 16) - 8) + np.abs((yy % 16) - 8)
    diamond = (d == 4).astype(float) * 0.1
    img = T.shade(base, 1 - pin - diamond)
    return T.grain(img, "mrwp", 0.02)


def wainscot():
    img = wood("mrwain", "#2a2a30", "#3a3a42", grain_dir="v", rings=2)
    img = T.rect(img, 4, 8, 28, 28, "#000000", 0.12)
    img[0:3] = T.hexc("#c8c8d0")
    img[3] = T.hexc("#8a8a94")
    return T.grain(img, "mrwain", 0.02)


def checker(flip):
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx // 16) + (yy // 16)) % 2 == (1 if flip else 0))
    n = T.fbm("mrchk" + str(flip), octaves=3, base=4)
    white = T.ramp(n, "#d8d8dc", "#ececf0")
    black = T.ramp(n, "#141418", "#22222a")
    img = np.where(a[..., None], white, black)
    img = T.shade(img, 1 - (((xx % 16) == 0) | ((yy % 16) == 0)) * 0.15)
    sheen = T.noise("mrsheen", sigma=4)
    return T.grain(T.shade(img, 0.93 + sheen * 0.12), "mrchk" + str(flip), 0.015)


def ceiling():
    img = T.ramp(T.fbm("mrceil", octaves=3, base=5), "#c8ccd0", "#d6dade")
    return T.grain(T.border(img, "#a8acb0", 1, alpha=0.5), "mrceil", 0.02)


def glass():
    """Near-invisible glass with long silver streaks: you only see it when the light catches it."""
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = np.ones((S, S, 3)) * T.hexc("#dce4ec")
    streak = np.clip(1 - np.abs(((xx + yy * 0.7) % 32) - 9) / 3.0, 0, 1)
    a = 0.07 + streak * 0.16 + T.noise("mrgl", sigma=3) * 0.04
    a[:, :1] = 0.5
    return rgb, np.clip(a, 0, 1)


def mirror_face():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(np.clip(yy / 32.0, 0, 1) * 0.5 + T.noise("mrmf", sigma=4) * 0.5, "#8a929a", "#b8c0c8")
    streak = np.clip(1 - np.abs((xx + yy * 0.5) - 14) / 4.0, 0, 1) * 0.2
    return np.clip(img + streak[..., None], 0, 1)


def dark_wood():
    return wood("mrdw", "#2a1a14", "#3e281e")


def bulb():
    return T.ramp(T.noise("mrbulb", sigma=2), "#fff4dc", "#fffaf0")


def chrome():
    return T.grain(T.ramp(T.noise("mrchrome", sigma=2), "#8a8e94", "#d8dce0"), "mrchrome", 0.02)


def door(top, open_=False):
    img = wood("mrdoor" + str(top), "#2a1a14", "#3e281e", grain_dir="v", rings=3)
    if open_:
        img = T.solid("#020204")
        img[:, :3] = T.hexc("#3e281e")
        img[:, -3:] = T.hexc("#3e281e")
        return img
    for (y0, y1) in (((5, 29),) if top else ((2, 24),)):
        for (x0, x1) in ((5, 14), (18, 27)):
            img[y0:y1, x0] = img[y0:y1, x0] * 0.6
            img[y0, x0:x1] = img[y0, x0:x1] * 0.6
    if not top:
        # handle on the left: it is a mirror image of a door
        img = T.rect(img, 6, 3, 9, 6, "#c8a050")
    img[:, :2] = T.hexc("#120a06")
    img[:, -2:] = T.hexc("#120a06")
    return T.grain(img, "mrdoor" + str(top), 0.02)


def tixe():
    img = T.solid("#0e2a14")
    img = text_center(img, "EXIT", 10, "#7cff9a", scale=2, mirror=True)
    return T.border(img, "#2a2a2a", 1)


def plaque(mirrored):
    img = T.ramp(T.noise("mrplq", shape=(32, 64), sigma=2), "#b8903a", "#d8b060")
    img = T.border(img, "#6a4a1a", 2)
    img = text_center(img, "IT GOES WHERE", 9, "#2a1a0a", mirror=mirrored)
    img = text_center(img, "YOU CANNOT", 18, "#2a1a0a", mirror=mirrored)
    return img


tex("mr_wallpaper", wallpaper)
tex("mr_wainscot", wainscot)
tex("mr_floor_a", lambda: checker(False))
tex("mr_floor_b", lambda: checker(True))
tex("mr_ceiling", ceiling)
tex("mr_glass", glass)
tex("mr_mirror", mirror_face)
tex("mr_darkwood", dark_wood)
tex("mr_bulb", bulb)
tex("mr_chrome", chrome)
tex("mr_door_low", lambda: door(False))
tex("mr_door_high", lambda: door(True))
tex("mr_door_open", lambda: door(False, True))
tex("mr_tixe", tixe)
for m in (False, True):
    for idx, half in enumerate(("l", "r")):
        tex(f"mr_plaque{'_m' if m else ''}_{half}", (lambda mm, i: lambda: plaque(mm)[:, i * 32:(i + 1) * 32].copy())(m, idx))

cube("mr_wallpaper", "mr_wallpaper", display="Silver Wallpaper", sound="wool")
cube("mr_wainscot", "mr_wainscot", display="Black Panelling", sound="wood")
cube("mr_floor_a", "mr_floor_a", display="Checkered Marble", sound="tile")
cube("mr_floor_b", "mr_floor_b", display="Checkered Marble", sound="tile")
cube("mr_ceiling", "mr_ceiling", display="Pale Ceiling", flat=True)
pane("mr_glass", "mr_glass", display="The Glass", sound="glass", render="translucent")
cube("mr_door_low", "mr_door_low", display="A Door on the Other Side", sound="wood")
cube("mr_door_high", "mr_door_high", display="A Door on the Other Side", sound="wood")
cube("mr_door_open", "mr_door_open", display="An Open Door", sound="wood")
shaped("mr_tixe", [(2, 4, 0, 14, 10, 2, {"all": "frame", "south": "sign"})], {"sign": "mr_tixe", "frame": "mr_darkwood"},
       display="TIXE", light=7, emissive=True, unbreakable=True, collides=False, sound="glass", render="solid")
for m in ("", "_m"):
    for half in ("l", "r"):
        shaped(f"mr_plaque{m}_{half}", [(0, 4, 0, 16, 12, 1, {"all": "frame", "south": "p"})],
               {"p": f"mr_plaque{m}_{half}", "frame": "mr_darkwood"}, display="Brass Plaque", collides=False,
               unbreakable=True, sound="metal")

# vanity table with a round mirror and a row of bulbs, against the back wall
shaped("mr_vanity", [(0, 10, 0, 16, 12, 9, "wood"), (1, 0, 1, 3, 10, 3, "wood"), (13, 0, 1, 15, 10, 3, "wood"),
                     (1, 0, 6, 3, 10, 8, "wood"), (13, 0, 6, 15, 10, 8, "wood")],
       {"wood": "mr_darkwood"}, display="Vanity", unbreakable=True)
shaped("mr_vanity_mirror", [(2, 0, 0, 14, 14, 1, {"all": "chrome", "south": "mirror"}),
                            (1, 14, 0, 3, 16, 3, "bulb"), (7, 14, 0, 9, 16, 3, "bulb"), (13, 14, 0, 15, 16, 3, "bulb")],
       {"mirror": "mr_mirror", "chrome": "mr_chrome", "bulb": "mr_bulb"}, display="Vanity Mirror", light=11,
       emissive=True, collides=False, unbreakable=True, sound="glass")
shaped("mr_stool", [(4, 6, 4, 12, 8, 12, "wood"), (7, 0, 7, 9, 6, 9, "chrome"), (5, 0, 5, 11, 1, 11, "chrome")],
       {"wood": "mr_darkwood", "chrome": "mr_chrome"}, display="Stool", unbreakable=True)

lang("entity.noclip.mr_reflection", "Your Reflection")
lang("item.noclip.mr_reflection_spawn_egg", "Reflection Spawn Egg")
ITEM_MODELS["mr_reflection_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def glass_room():
    """A hall of glass: wineglass partials drifting in and out, slowed, and air on the other side."""
    d = 28.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    r = Y.rng("mrgr")
    for f in (523.3, 784.0, 1046.5, 1318.5, 1568.0):
        ff = round(f * d) / d
        lfo = round((0.03 + r.random() * 0.06) * d) / d
        x += np.sin(2 * np.pi * ff * t) * (0.5 + 0.5 * np.sin(2 * np.pi * lfo * t + r.random() * 6)) ** 3 / 5
    room = Y.lp(Y.brown(d, "mrroom"), 160) * 0.4
    out = Y.norm(x * 0.12 + room, 0.4)
    return Y.stereoize(Y.loopable(out, 1.5), 0.004), "stereo"


def glass_knock(i):
    def f():
        out = np.zeros(int(2.5 * Y.SR))
        for k in range(2 + i):
            n = int(0.25 * Y.SR)
            tt = np.arange(n) / Y.SR
            hit = (np.sin(2 * np.pi * 1800 * tt) * 0.4 + np.sin(2 * np.pi * 3100 * tt) * 0.2) * np.exp(-tt * 30)
            hit += Y.lp(Y.white(0.25, f"mrk{i}{k}"), 600)[:n] * np.exp(-tt * 50)
            Y.place(out, hit, 0.1 + k * 0.32, 0.8)
        return Y.reverb(out, 1.5, 0.4, f"mrk{i}")
    return f


def ring():
    d = 4.0
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * 1244 * t) * np.exp(-t * 0.9) * (1 + 0.3 * np.sin(2 * np.pi * 3 * t))
    return Y.norm(Y.reverb(x, 2.0, 0.5, "mrring"), 0.4)


def reversed_whisper():
    x = Y.whisper("mrrw", 2.0)
    x = Y.reverb(x, 1.6, 0.6, "mrrw")[::-1].copy()
    return Y.norm(x, 0.5)


def door_other_side():
    d = 2.0
    out = np.zeros(int(d * Y.SR))
    t = Y.t_axis(1.2)
    creak = np.sin(2 * np.pi * np.cumsum(260 - 100 * t) / Y.SR) * (Y.rng("mrdc").random(len(t)) > 0.5)
    Y.place(out, Y.bp(creak, 300, 2500) * np.sin(np.pi * t / 1.2), 0.0, 0.4)
    return Y.norm(Y.reverb(Y.lp(out, 2000), 2.0, 0.6, "mrdo"), 0.7)


sound("ambient.mirror_level", [("ambient/mirror_level", glass_room)], stream=True)
sound("ambient.mirror_level.additions",
      [("ambient/mirror_level_ring", ring), ("ambient/mirror_level_whisper", reversed_whisper),
       ("ambient/mirror_level_knock", glass_knock(0))],
      subtitle="Something on the other side of the glass")
sound("event.mirror_level_knock", [("event/mirror_level_knock", glass_knock(1))], subtitle="Knocking on the glass")
sound("event.mirror_level_door", [("event/mirror_level_door", door_other_side)], subtitle="A door opens on the other side")

level("mirror_level", fog="#3a3e44", sky="#3a3e44", ambient="ambient.mirror_level",
      additions="ambient.mirror_level.additions", additions_chance=0.004, fixed_time=18000, height=32)
