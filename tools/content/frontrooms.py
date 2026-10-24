"""The Frontrooms - a sunny suburban street that looks exactly like home, until it doesn't."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, pane, fence, stairs, door, pillar, \
    flicker_light, slab
import tex as T
import synth as Y
from content.deepend_font import draw
from content.deepend_audio import wrapverb, fit

S = 32
yy, xx = np.mgrid[0:S, 0:S]

SIDING = {"white": ("#d8d6ce", "#e6e4dc"), "blue": ("#9eb4c4", "#aec2d0"), "beige": ("#c8b898", "#d6c8a8"),
          "sage": ("#a4b496", "#b2c2a4")}


def siding(c):
    def f():
        lo, hi = SIDING[c]
        img = T.ramp(T.fbm("frs" + c, octaves=3, base=6) * 0.5 + 0.25, lo, hi)
        boards = (yy % 8)
        img = T.shade(img, 1 - (boards == 7) * 0.28 + (boards == 0) * 0.08 - boards * 0.006)
        return T.grain(img, "frs" + c, 0.02)
    return f


def trim():
    img = T.ramp(T.noise("frtrim", sigma=2), "#e8e6e0", "#f4f2ec")
    return T.grain(T.border(img, "#c8c6c0", 1, sides="we"), "frtrim", 0.02)


def shingles(lo, hi, seed):
    def f():
        img = T.ramp(T.fbm(seed, octaves=3, base=3) * 0.6 + T.noise(seed + "n", sigma=0.5) * 0.4, lo, hi)
        row = yy // 8
        off = (row % 2) * 4
        tabs = ((xx + off) % 8 == 0)
        img = T.shade(img, 1 - ((yy % 8) == 7) * 0.4 - tabs * 0.25)
        return T.grain(img, seed, 0.04)
    return f


def interior_wall(seed="frw", lo="#e6dcc8", hi="#eee4d2"):
    img = T.ramp(T.fbm(seed, octaves=2, base=8) * 0.4 + 0.3, lo, hi)
    # the faintest stripe of an old wallpaper painted over
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.025)
    return T.grain(img, seed, 0.015)


def interior_switch():
    img = interior_wall("frws")
    img = T.rect(img, 13, 10, 19, 20, "#f4f0e8")
    img = T.rect(img, 15, 13, 17, 17, "#d8d0c4")
    return img


def floorboards():
    img = T.ramp(T.fbm("frfb", octaves=3, base=3), "#8a6440", "#a07850")
    planks = (yy % 8 == 0)
    ends = ((xx + (yy // 8) * 11) % 32 == 0)
    img = T.shade(img, 1 - planks * 0.3 - ends * 0.25 + np.sin(xx * 0.9 + (yy // 8) * 2) * 0.03)
    return T.grain(img, "frfb", 0.025)


def carpet():
    img = T.ramp(T.noise("frcarp", sigma=0.6) * 0.5 + T.fbm("frcarpf", octaves=2, base=6) * 0.5, "#b8a890", "#c8b8a0")
    return T.grain(img, "frcarp", 0.04)


def kitchen_tile():
    check = ((xx // 8 + yy // 8) % 2 == 0)
    img = np.where(check[..., None], T.hexc("#e8e4dc"), T.hexc("#3a3a3c")).astype(float)
    img = T.shade(img, 1 - (((xx % 8) == 0) | ((yy % 8) == 0)) * 0.12)
    return T.grain(img, "frkt", 0.025)


def ceiling():
    img = T.ramp(T.fbm("frceil", octaves=2, base=6) * 0.3 + 0.5, "#ece8e0", "#f4f0e8")
    pop = (T.rng("frpop").random((S, S)) > 0.8).astype(float)
    return T.grain(T.shade(img, 1 - pop * 0.04), "frceil", 0.015)


def lawn():
    img = T.ramp(T.fbm("frlawn", octaves=4, base=3) * 0.5 + T.noise("frln", sigma=0.4) * 0.5, "#4a7a2c", "#5e9236", "#6ea440")
    stripes = ((xx // 8) % 2 == 0)
    return T.grain(T.shade(img, 1 + stripes * 0.06), "frlawn", 0.04)


def lawn_side():
    img = T.ramp(T.fbm("frdirt", octaves=3, base=3), "#5a4430", "#6e5438")
    img[:4] = lawn()[:4]
    return img


def sidewalk():
    img = T.ramp(T.fbm("frsw", octaves=3, base=5) * 0.6 + 0.2, "#a8a49c", "#b8b4ac")
    img = T.shade(img, 1 - ((xx % 32) == 0) * 0.3 - ((yy % 32) == 0) * 0.3)
    return T.grain(img, "frsw", 0.03)


def asphalt(line=False):
    def f():
        img = T.ramp(T.noise("frasp", sigma=0.5) * 0.6 + T.fbm("fraspf", octaves=2, base=6) * 0.4, "#2e2e30", "#3c3c3e")
        if line:
            m = (np.abs(yy - 15.5) < 1.5) & (xx < 20)
            img = T.blend(img, T.solid("#e0c040"), m.astype(float) * 0.9)
        return T.grain(img, "frasp" + str(line), 0.05)
    return f


def window_glass():
    t = (xx + yy) / 62.0
    rgb = T.ramp(t * 0.7 + 0.15, "#a8c8e0", "#d0e4f0")
    a = np.ones((S, S)) * 0.35
    frame = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29) | (np.abs(xx - 15.5) < 1) | (np.abs(yy - 15.5) < 1)
    rgb[frame] = T.hexc("#f4f4f0")
    a[frame] = 1
    return rgb, a


def front_door(top):
    def f():
        img = T.ramp(T.fbm("frfd", octaves=3, base=3), "#6a2a22", "#7a322a")
        img = T.border(img, "#4a1a14", 1)
        if top:
            img = T.rect(img, 6, 6, 26, 14, "#c0d4e0")
            img = T.rect(img, 6, 18, 26, 30, T.hexc("#5a2219"))
            img = draw(img, "13", "#d8b860", scale=2, y=20)
        else:
            img = T.rect(img, 6, 2, 26, 22, T.hexc("#5a2219"))
            img = T.rect(img, 24, 26 - 26, 27, 3, "#d8b860")
        return T.grain(img, "frfd" + str(top), 0.03)
    return f


def closet_door():
    img = T.ramp(T.noise("frcd", sigma=3), "#e8e4dc", "#f2eee6")
    img = T.border(img, "#c8c4bc", 1)
    img = T.shade(img, 1 - ((xx > 4) & (xx < 28) & (yy > 4) & (yy < 28)) * 0.05)
    img = T.rect(img, 24, 16, 27, 19, "#b8a060")
    return T.grain(img, "frcd", 0.02)


def basement_dark():
    img = T.solid("#030303")
    return T.grain(img, "frbd", 0.01)


def mailbox():
    img = T.ramp(T.noise("frmb", sigma=2), "#2a2a2c", "#3a3a3e")
    img = draw(img, "13", "#e8e4dc", scale=2, y=10)
    return img


def leaves():
    rgb = T.ramp(T.fbm("frleaf", octaves=3, base=2) * 0.6 + T.noise("frlfn", sigma=0.5) * 0.4, "#2a5a1c", "#3a7024", "#4e8a30")
    a = (T.noise("frlfa", sigma=0.6) > 0.18).astype(float)
    return rgb, a


def bark():
    img = T.ramp(T.fbm("frbark", octaves=3, base=2), "#4a3a2a", "#5a4632")
    return T.grain(T.shade(img, 1 - ((xx % 5) == 0) * 0.25), "frbark", 0.04)


def picket():
    rgb = np.ones((S, S, 3)) * T.hexc("#f2f0ea")
    rgb = T.grain(rgb, "frpick", 0.02)
    return rgb


def fabric(c1, c2, seed):
    def f():
        img = T.ramp(T.noise(seed, sigma=0.6) * 0.5 + T.fbm(seed + "f", octaves=2, base=4) * 0.5, c1, c2)
        return T.grain(img, seed, 0.03)
    return f


def wood_dark():
    return T.grain(T.ramp(T.fbm("frwd", octaves=3, base=3), "#3a2618", "#4a321e"), "frwd", 0.03)


def tv_screen(kind):
    def f():
        if kind == "static":
            r = T.rng("frstat").random((S, S))
            img = np.stack([r, r, r], axis=2) * 0.8 + 0.1
            img = T.shade(img, 1 - ((yy % 2) == 0) * 0.15)
        elif kind == "lobby":
            # the TV is showing a yellow room. A long way in, a light is flickering.
            img = T.vgrad(S, S, "#d0c070", "#8a7a3a")
            img = T.rect(img, 12, 8, 20, 22, "#5a4a1c")
            img = T.rect(img, 4, 2, 10, 4, "#fffbe0")
            img = T.rect(img, 22, 2, 28, 4, "#fffbe0")
            img = T.shade(img, 1 - ((yy % 2) == 0) * 0.15)
        elif kind == "smile":
            img = T.solid("#101010")
            img = draw(img, "=)", "#f0f0f0", scale=3)
        else:
            img = T.solid("#0a0c0e")
            img = T.blend(img, T.solid("#2a3038"), np.clip(1 - (xx + yy) / 30.0, 0, 1) * 0.5)
        img = T.border(img, "#1a1a1a", 2)
        return img
    return f


def clock(kind):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
        face = d < 14
        rgb[face] = T.hexc("#f4f0e4")
        a[face] = 1
        rim = (d >= 12.5) & (d < 14)
        rgb[rim] = T.hexc("#4a3a2a")
        n = 13 if kind == "thirteen" else 12
        for k in range(n):
            ang = 2 * np.pi * k / n
            px, py = 15.5 + np.sin(ang) * 10.5, 15.5 - np.cos(ang) * 10.5
            rgb[int(py), int(px)] = T.hexc("#1a1a1a")
            if k % 3 == 0 and n == 12:
                rgb[int(py), int(px) - 1:int(px) + 1] = T.hexc("#1a1a1a")
        if kind == "thirteen":
            rgb = draw(rgb, "13", "#8a1a1a", scale=1, y=6)

        def hand(ang, ln, w=1):
            for t in np.linspace(0, ln, 40):
                px, py = 15.5 + np.sin(ang) * t, 15.5 - np.cos(ang) * t
                rgb[int(py), int(px)] = T.hexc("#1a1a1a")
        if kind == "normal":
            hand(np.pi / 2, 6)
            hand(0.0, 9)
        elif kind == "backwards":
            hand(-np.pi / 2, 6)
            hand(-0.3, 9)
            hand(-0.6, 9)
        elif kind == "thirteen":
            hand(np.pi * 1.2, 6)
            hand(np.pi * 0.2, 9)
        return rgb, a
    return f


def photo(kind):
    def f():
        img = T.solid("#3a2a1a")
        inner = T.vgrad(S, S, "#a8c0d0", "#6a8a5a")
        m = np.zeros((S, S))
        m[4:28, 4:28] = 1
        img = T.blend(img, inner, m)
        if kind in ("family", "you"):
            people = [(10, "#c8a088", "#3a5a8a"), (16, "#d8b8a0", "#8a3a3a"), (22, "#c8a088", "#3a6a4a")] \
                if kind == "family" else [(16, "#d8b8a0", "#4a4a5a")]
            for (cx, skin, shirt) in people:
                head = ((xx - cx) ** 2 / 4.5 + (yy - 13) ** 2 / 6) < 1
                body = (np.abs(xx - cx) < 3) & (yy > 16) & (yy < 28)
                img[body & (m > 0)] = T.hexc(shirt)
                img[head] = T.hexc(skin)   # no eyes. no mouth. smooth.
            if kind == "you":
                img = draw(img, "YOU", "#f0f0f0", scale=1, y=5)
        elif kind == "lobby":
            img = T.blend(img, T.vgrad(S, S, "#d8c878", "#9a8a40"), m)
            img = T.rect(img, 13, 10, 19, 26, "#4a3c14")
        elif kind == "house":
            img = T.rect(img, 9, 14, 23, 26, "#d8d6ce")
            for x in range(9, 23):
                img[14 - (x - 9 if x < 16 else 22 - x) // 2, x] = T.hexc("#5a3a2a")
            img = T.rect(img, 12, 18, 14, 21, "#1a1a1a")
            img = T.rect(img, 18, 18, 20, 21, "#f0e090")
            img[19, 19] = T.hexc("#0a0a0a")   # someone is in the window
        rgb = img
        a = np.ones((S, S))
        return rgb, a
    return f


def mirror():
    img = T.vgrad(S, S, "#c8d4dc", "#9aa8b0")
    # it shows the room. It does not show you.
    img = T.rect(img, 4, 20, 28, 30, "#b8a890")
    img = T.rect(img, 18, 8, 26, 20, "#e6dcc8")
    img = T.shade(img, 1 + np.clip(1 - np.abs(xx - yy - 4) / 3.0, 0, 1) * 0.15)
    return T.border(img, "#5a4a3a", 2)


def counter_top():
    img = T.ramp(T.noise("frct", sigma=1), "#d8d4cc", "#e4e0d8")
    specks = (T.rng("frcts").random((S, S)) > 0.9).astype(float)
    return T.grain(T.shade(img, 1 - specks * 0.12), "frct", 0.02)


def cabinet():
    img = T.ramp(T.fbm("frcab", octaves=2, base=4), "#d8d0bc", "#e4dcc8")
    img = T.border(img, "#b8b0a0", 1)
    img = T.shade(img, 1 - ((xx == 15) | (xx == 16)) * 0.3)
    img = T.rect(img, 12, 6, 14, 10, "#8a8478")
    img = T.rect(img, 18, 6, 20, 10, "#8a8478")
    return T.grain(img, "frcab", 0.02)


def fridge(top):
    def f():
        img = T.ramp(T.noise("frfr" + str(top), sigma=3), "#e8e8e4", "#f4f4f0")
        img = T.border(img, "#c8c8c4", 1)
        img = T.rect(img, 26, 4 if top else 2, 28, 28 if top else 16, "#a8a8a4")
        if not top:
            # a child's drawing held by a magnet: a family, a house, and a yellow room
            img = T.rect(img, 6, 6, 20, 18, "#f8f6f0")
            img = T.rect(img, 8, 12, 11, 16, "#3a6ad8")
            img = T.rect(img, 13, 12, 16, 16, "#d8202c")
            img = T.rect(img, 16, 7, 19, 10, "#e8c838")
        return T.grain(img, "frfr" + str(top), 0.02)
    return f


def lamp_shade():
    d = np.abs(xx - 15.5) / 16
    return T.ramp(1 - d, "#d8b878", "#f8e8c0", "#fffaf0")



for c in SIDING:
    tex(f"fr_siding_{c}", siding(c))
tex("fr_trim", trim)
tex("fr_shingles_grey", shingles("#4a4a4c", "#5a5a5e", "frshg"))
tex("fr_shingles_brown", shingles("#5a3a2a", "#6a4632", "frshb"))
tex("fr_wall", interior_wall)
tex("fr_wall_switch", interior_switch)
tex("fr_floorboards", floorboards)
tex("fr_carpet", carpet)
tex("fr_kitchen_tile", kitchen_tile)
tex("fr_ceiling", ceiling)
tex("fr_lawn", lawn)
tex("fr_lawn_side", lawn_side)
tex("fr_sidewalk", sidewalk)
tex("fr_asphalt", asphalt())
tex("fr_asphalt_line", asphalt(True))
tex("fr_window", window_glass)
tex("fr_window_edge", lambda: T.solid("#f4f4f0"))
tex("fr_front_door", front_door(False))
tex("fr_front_door_top", front_door(True))
tex("fr_closet_door", closet_door)
tex("fr_basement_dark", basement_dark)
tex("fr_mailbox", mailbox)
tex("fr_leaves", leaves)
tex("fr_bark", bark)
tex("fr_picket", picket)
tex("fr_sofa", fabric("#6a7a5a", "#7a8a68", "frsofa"))
tex("fr_bedsheet", fabric("#8aa0c0", "#9ab0d0", "frbed"))
tex("fr_pillow", fabric("#f0ece4", "#f8f4ec", "frpil"))
tex("fr_wood_dark", wood_dark)
for k in ("static", "lobby", "smile", "off"):
    tex(f"fr_tv_{k}", tv_screen(k))
for k in ("normal", "nohands", "backwards", "thirteen"):
    tex(f"fr_clock_{k}", clock(k))
for k in ("family", "you", "lobby", "house"):
    tex(f"fr_photo_{k}", photo(k))
tex("fr_mirror", mirror)
tex("fr_counter", counter_top)
tex("fr_cabinet", cabinet)
tex("fr_fridge", fridge(False))
tex("fr_fridge_top", fridge(True))
tex("fr_lampshade", lamp_shade)
tex("fr_metal", lambda: T.grain(T.ramp(T.noise("frmet", sigma=1), "#5a5c60", "#7a7c80"), "frmet", 0.03))

for c in SIDING:
    cube(f"frontroom_siding_{c}", f"fr_siding_{c}", display="Vinyl Siding", sound="wood")
cube("frontroom_trim", "fr_trim", display="White Trim", sound="wood")
for c in ("grey", "brown"):
    cube(f"frontroom_roof_{c}", f"fr_shingles_{c}", display="Roof Shingles")
    stairs(f"frontroom_roof_{c}_stairs", f"fr_shingles_{c}", f"frontroom_roof_{c}", display="Roof Shingles")
cube("frontroom_wall", "fr_wall", display="Painted Wall", sound="wool")
cube("frontroom_wall_switch", "fr_wall_switch", display="Light Switch", sound="wool")
cube("frontroom_floorboards", "fr_floorboards", display="Floorboards", sound="wood")
cube("frontroom_carpet", "fr_carpet", display="Living Room Carpet", sound="wool")
cube("frontroom_kitchen_tile", "fr_kitchen_tile", display="Kitchen Tile", sound="tile")
cube("frontroom_ceiling", "fr_ceiling", display="Popcorn Ceiling", sound="wool", flat=True)
cube_faces("frontroom_lawn", top="fr_lawn", bottom="fr_lawn_side", side="fr_lawn_side", display="Lawn", sound="grass")
cube("frontroom_sidewalk", "fr_sidewalk", display="Sidewalk")
cube("frontroom_asphalt", "fr_asphalt", display="Asphalt")
cube("frontroom_asphalt_line", "fr_asphalt_line", display="Road Marking")
pane("frontroom_window", "fr_window", edge="fr_window_edge", display="Window", sound="glass", render="translucent")
door("frontroom_front_door", "fr_front_door_top", "fr_front_door", display="Front Door", unbreakable=True)
exit_block("frontroom_closet_door", "fr_closet_door", "DOOR", trigger="USE", solid=True, display="Closet Door",
           sound="wood")
exit_block("frontroom_basement_dark", "fr_basement_dark", "DESCEND", display="Basement")
fence("frontroom_picket_fence", "fr_picket", display="Picket Fence", sound="wood")
cube("frontroom_leaves", "fr_leaves", display="Leaves", sound="grass", render="cutout")
pillar("frontroom_trunk", "fr_bark", "fr_bark", display="Tree Trunk", sound="wood")
shaped("frontroom_mailbox", [(7, 0, 7, 9, 10, 9, "post"), (4, 10, 3, 12, 16, 13, {"all": "box"})],
       {"post": "fr_wood_dark", "box": "fr_mailbox"}, display="Mailbox", unbreakable=True, sound="metal")
shaped("frontroom_street_lamp", [(7, 0, 7, 9, 16, 9, "metal")], {"metal": "fr_metal"}, display="Lamp Post",
       unbreakable=True, sound="metal")
shaped("frontroom_street_lamp_head", [(7, 0, 7, 9, 4, 9, "metal"), (4, 4, 4, 12, 9, 12, {"all": "shade", "up": "metal"})],
       {"metal": "fr_metal", "shade": "fr_lampshade"}, display="Street Light", light=14, emissive=True,
       unbreakable=True, sound="glass")
shaped("frontroom_sofa", [
    (0, 2, 2, 16, 8, 14, {"all": "fab"}), (0, 8, 11, 16, 15, 14, "fab"), (0, 8, 2, 2, 11, 11, "fab"),
    (14, 8, 2, 16, 11, 11, "fab"), (1, 0, 3, 2, 2, 4, "wood"), (14, 0, 3, 15, 2, 4, "wood"),
], {"fab": "fr_sofa", "wood": "fr_wood_dark"}, display="Sofa", unbreakable=True, sound="wool")
for k in ("static", "lobby", "smile", "off"):
    shaped(f"frontroom_tv_{k}", [
        (0, 0, 4, 16, 6, 12, "wood"), (2, 6, 5, 14, 15, 11, {"all": "case", "north": "screen"}),
    ], {"wood": "fr_wood_dark", "case": "fr_metal", "screen": f"fr_tv_{k}"}, display="Television",
        light=None if k == "off" else 6, emissive=None if k == "off" else True, unbreakable=True, sound="metal")
for k in ("normal", "nohands", "backwards", "thirteen"):
    shaped(f"frontroom_clock_{k}", [(1, 1, 15, 15, 15, 16, {"all": None, "north": "face"})], {"face": f"fr_clock_{k}"},
           display="Wall Clock", collides=False, unbreakable=True, sound="wood")
for k in ("family", "you", "lobby", "house"):
    shaped(f"frontroom_photo_{k}", [(4, 2, 15, 12, 12, 16, {"all": "frame", "north": "pic"})],
           {"pic": f"fr_photo_{k}", "frame": "fr_wood_dark"}, display="Family Photo", collides=False,
           unbreakable=True, sound="wood")
shaped("frontroom_mirror", [(2, 0, 15, 14, 16, 16, {"all": "frame", "north": "glass"})],
       {"glass": "fr_mirror", "frame": "fr_wood_dark"}, display="Mirror", collides=False, unbreakable=True,
       sound="glass")
cube_faces("frontroom_counter", top="fr_counter", bottom="fr_cabinet", side="fr_cabinet", display="Kitchen Counter",
           sound="wood")
cube_faces("frontroom_fridge", top="fr_fridge_top", bottom="fr_fridge", side="fr_fridge", display="Fridge",
           sound="metal")
cube_faces("frontroom_fridge_top", top="fr_fridge_top", bottom="fr_fridge_top", side="fr_fridge_top",
           display="Fridge", sound="metal")
shaped("frontroom_bed", [
    (0, 0, 0, 16, 9, 16, {"all": "sheet"}), (1, 9, 1, 15, 11, 6, "pillow"), (0, 0, 0, 16, 14, 1, "wood"),
], {"sheet": "fr_bedsheet", "pillow": "fr_pillow", "wood": "fr_wood_dark"}, display="Bed", unbreakable=True,
    sound="wool")
shaped("frontroom_table", [
    (0, 13, 0, 16, 15, 16, "wood"), (1, 0, 1, 3, 13, 3, "wood"), (13, 0, 1, 15, 13, 3, "wood"),
    (1, 0, 13, 3, 13, 15, "wood"), (13, 0, 13, 15, 13, 15, "wood"),
], {"wood": "fr_wood_dark"}, display="Table", unbreakable=True, sound="wood")
shaped("frontroom_floor_lamp", [
    (7, 0, 7, 9, 12, 9, "metal"), (5, 0, 5, 11, 1, 11, "metal"), (4, 12, 4, 12, 16, 12, {"all": "shade", "up": None}),
], {"metal": "fr_metal", "shade": "fr_lampshade"}, display="Floor Lamp", light=12, emissive=True,
    unbreakable=True, sound="metal")

# ------------------------------------------------------------------ sound


def suburb():
    """A Saturday afternoon. Birds, a lawnmower two streets over, wind in the trees. It loops a little too exactly."""
    d = 24.0
    t = Y.t_axis(d)
    n = len(t)
    wind = Y.bp(Y.pink(d, "frwind"), 200, 2500) * 0.12 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 12.0) ** 2)
    mower = Y.lp(Y.hum(d, 37.5, "frmower", buzz=0.6, harmonics=(1, 2, 3, 4)), 900) * 0.05
    mower *= 0.6 + 0.4 * np.sin(2 * np.pi * t / 8.0) ** 2
    birds = np.zeros(n)
    r = Y.rng("frbirds")
    # the same three-note song, at the same moments, every time
    for k in range(9):
        at = 1.0 + k * 2.6
        for j, f0 in enumerate((3200, 3800, 3400)):
            nn = int(0.09 * Y.SR)
            tt = np.arange(nn) / Y.SR
            chirp = np.sin(2 * np.pi * np.cumsum(f0 + 900 * np.sin(np.pi * tt / 0.09)) / Y.SR) * np.sin(np.pi * tt / 0.09) ** 2
            Y.place(birds, chirp, at + j * 0.13, 0.12 + r.random() * 0.05)
    x = wrapverb(wind + birds, 1.5, 0.3, "frair", damp=6000) + mower
    return Y.stereoize(Y.loopable(Y.norm(x, 0.4), 1.2), 0.004), "stereo"


def dog():
    out = np.zeros(int(3.0 * Y.SR))
    for k in range(3):
        nn = int(0.18 * Y.SR)
        tt = np.arange(nn) / Y.SR
        b = Y.bp(Y.white(0.18, f"frdog{k}"), 400, 2500)[:nn] * np.exp(-tt * 18)
        b += np.sin(2 * np.pi * (520 - tt * 900) * tt) * np.exp(-tt * 20) * 0.6
        Y.place(out, b, 0.1 + k * 0.35, 0.8)
    return Y.reverb(Y.lp(out, 2500), 2.5, 0.6, "frdog")


def ice_cream():
    """An ice cream van, very far away, playing a tune that winds down."""
    notes = [523.25, 587.33, 659.25, 523.25, 659.25, 587.33, 523.25, 392.0]
    out = np.zeros(int(6.0 * Y.SR))
    for k, f0 in enumerate(notes):
        tt = Y.t_axis(0.6)
        slow = 1 - k * 0.03
        tone = np.sin(2 * np.pi * f0 * slow * tt) * np.exp(-tt * 4) + 0.3 * np.sin(4 * np.pi * f0 * slow * tt) * np.exp(-tt * 6)
        Y.place(out, tone, 0.2 + k * 0.42 * (1 + k * 0.05), 0.5)
    return Y.reverb(Y.lp(out, 2200), 3.0, 0.7, "fricecream")


def chimes():
    out = np.zeros(int(4.0 * Y.SR))
    r = Y.rng("frchime")
    for k in range(6):
        f0 = [1046.5, 1174.7, 1318.5, 1568.0, 1760.0][int(r.random() * 5)]
        tt = Y.t_axis(2.5)
        Y.place(out, np.sin(2 * np.pi * f0 * tt) * np.exp(-tt * 2.5), 0.1 + k * 0.4 + r.random() * 0.2, 0.25)
    return Y.reverb(out, 2.0, 0.5, "frchime")


def hum_bleed():
    """For a second, under everything: the fluorescent hum of somewhere else."""
    d = 3.0
    t = Y.t_axis(d)
    x = Y.hum(d, 60.0, "frbleed", buzz=0.35) * np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.lp(x, 2500), 0.5)


def tick():
    out = np.zeros(int(4.0 * Y.SR))
    for k in range(4):
        nn = int(0.03 * Y.SR)
        c = Y.bp(Y.white(0.03, f"frtick{k}"), 2000, 7000)[:nn] * np.exp(-np.arange(nn) / Y.SR * 200)
        Y.place(out, c, 0.05 + k * 1.0, 0.7)
    return Y.reverb(out, 0.6, 0.2, "frtick")


sound("ambient.frontrooms", [("ambient/frontrooms", suburb)], stream=True)
sound("ambient.frontrooms.additions",
      [("ambient/frontrooms_dog", dog), ("ambient/frontrooms_icecream", ice_cream), ("ambient/frontrooms_chimes", chimes),
       ("ambient/frontrooms_hum", hum_bleed)],
      subtitle="An ordinary afternoon")
sound("event.frontrooms_tick", [("event/frontrooms_tick", tick)], subtitle="A clock ticks")

level("frontrooms", fog="#c8dcec", sky="#78a8e0", ambient="ambient.frontrooms", additions="ambient.frontrooms.additions",
      additions_chance=0.004, fixed_time=6000, skylight=True, ceiling=False, height=48)
