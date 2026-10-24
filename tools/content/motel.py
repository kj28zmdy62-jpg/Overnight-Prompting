"""Roadside Motel - a strip of doors under a buzzing sign, a parking lot, a road, a field, night.
Every door is room 6. They all open into the same room, and the room keeps changing."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, flicker_light, cross, pillar, custom, level, sound, \
    BLOCK_MODELS
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32


def asphalt(seed="mta", line=None):
    img = T.ramp(T.noise(seed, sigma=0.5) * 0.6 + T.fbm(seed + "f", octaves=3, base=5) * 0.4, "#1e1e20", "#2c2c2e")
    stones = T.rng(seed + "s").random((S, S))
    img = T.blend(img, T.solid("#4a4a4c"), (stones > 0.93) * 0.6)
    img = T.cracks(img, seed + "c", "#121214", 1, 0.5)
    if line == "white":
        yy, xx = np.mgrid[0:S, 0:S]
        worn = T.noise(seed + "w", sigma=0.8) > 0.25
        img = T.blend(img, T.solid("#c8c8c0"), ((xx >= 13) & (xx < 19) & worn) * 0.85)
    elif line == "yellow":
        yy, xx = np.mgrid[0:S, 0:S]
        worn = T.noise(seed + "w", sigma=0.8) > 0.2
        img = T.blend(img, T.solid("#d8a820"), ((yy >= 12) & (yy < 20) & (xx < 22) & worn) * 0.85)
    return T.grain(img, seed, 0.04)


def concrete(seed, lo="#7a7872", hi="#8a8880"):
    img = T.ramp(T.fbm(seed, octaves=4, base=5), lo, hi)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (((xx % 32) == 0) | ((yy % 32) == 0)) * 0.3)
    img = T.stains(img, seed + "s", "#5a5650", count=2, strength=0.25, size=7)
    return T.grain(img, seed, 0.04)


def brick():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("mtb", octaves=3, base=3) * 0.5 + T.noise("mtbn", sigma=0.6) * 0.5, "#c08a6a", "#d09a78")
    off = ((yy // 8) % 2) * 8
    mortar = ((yy % 8) == 7) | (((xx + off) % 16) == 15)
    img = T.blend(img, T.solid("#e8dccc"), mortar * 0.85)
    return K.grime_bottom(T.grain(img, "mtb", 0.04), "mtbg", 10, 0.3)


def door(top, inside=False):
    img = T.ramp(T.noise("mtd" + str(top) + str(inside), sigma=2), "#2a7a78", "#348886")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((np.abs(xx - 15.5) > 11) & (np.abs(xx - 15.5) < 12.5)) * 0.25)
    img = T.border(img, "#e8dccc", 2, sides="we" + ("n" if top else "s"))
    if top:
        # brass numeral 6, and a peephole
        six = K.text_mask("6", S, S, 11, 6, font=K.F57, scale=2)
        img = T.blend(img, T.solid("#3a2a10"), np.roll(six, 1, axis=1) * 0.8)
        img = T.blend(img, T.solid("#e0c060"), six)
        img = T.blend(img, T.solid("#101010"), K.disc(S, S, 15.5, 25, 1.2).astype(float))
    else:
        img = T.rect(img, 22, 2, 27, 5, "#d8b850")
        img = T.rect(img, 23, 5, 25, 9, "#a88830")
        if not inside:
            img = K.grime_bottom(img, "mtdg", 8, 0.35)
    if inside:
        # the chain, the deadbolt, the card about checkout time
        img = T.rect(img, 4, 2, 12, 10, "#f0ead8")
        img = T.blend(img, T.solid("#3a3a3a"), K.text_mask("OUT 11", S, S, 4, 4, font=K.F35, spacing=0) * 0.7)
    return T.grain(img, "mtdgr" + str(top) + str(inside), 0.03)


def door7():
    img = T.ramp(T.noise("mtd7", sigma=2), "#5a1a1a", "#6a2222")
    img = T.border(img, "#e8dccc", 2, sides="wen")
    seven = K.text_mask("7", S, S, 11, 6, font=K.F57, scale=2)
    img = T.blend(img, T.solid("#e0c060"), seven)
    return T.grain(img, "mtd7", 0.03)


def window_lit():
    yy, xx = np.mgrid[0:S, 0:S]
    folds = 0.5 + 0.5 * np.sin(xx / 32 * 2 * np.pi * 5)
    glow = np.clip(1 - np.abs(yy - 14) / 22, 0, 1)
    img = T.ramp(folds * 0.4 + glow * 0.6, "#8a4a1a", "#e89040", "#ffd080")
    gap = (np.abs(xx - 15.5) < 1.0)
    img = T.blend(img, T.solid("#fff0c0"), gap * 0.8)
    frame = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29)
    return T.blend(img, T.solid("#d8d4c8"), frame.astype(float))


def window_dark():
    yy, xx = np.mgrid[0:S, 0:S]
    folds = 0.5 + 0.5 * np.sin(xx / 32 * 2 * np.pi * 5)
    img = T.ramp(folds * 0.5 + T.noise("mtwd", sigma=2) * 0.3, "#1a1410", "#2e241a")
    frame = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29)
    return T.blend(img, T.solid("#d8d4c8"), frame.astype(float))


def soffit():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtsf", sigma=1.5), "#c8c4b8", "#d4d0c4")
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.15)
    return T.stains(T.grain(img, "mtsf", 0.03), "mtsfs", "#8a7a5a", count=1, strength=0.3, size=8)


def bulb():
    img = soffit()
    d = np.sqrt((np.mgrid[0:S, 0:S][1] - 15.5) ** 2 + (np.mgrid[0:S, 0:S][0] - 15.5) ** 2)
    img = T.blend(img, T.ramp(np.clip(1 - d / 7, 0, 1), "#ffc870", "#fff4d8"), (d < 7).astype(float))
    # bugs in the fixture
    for k, (x, y) in enumerate(((13, 14), (18, 17), (15, 19))):
        img[y, x] = T.hexc("#3a2a1a")
    return img


def fascia():
    img = T.ramp(T.noise("mtfa", sigma=1.5), "#2a8a88", "#329896")
    img[:3] = T.hexc("#e8dccc")
    img[-3:] = T.hexc("#1e6a68")
    return T.grain(img, "mtfa", 0.03)


def gravel():
    img = T.ramp(T.noise("mtgr", sigma=0.5), "#4a4844", "#6a6862")
    return T.grain(img, "mtgr", 0.06)


def sign_image(text, color, lit=True, small=None):
    h, w = 32, 96
    img = T.ramp(T.noise("mtsg" + text, sigma=2, shape=(h, w)), "#f0ece0", "#f8f4ea") * 0.25
    img = T.border(img, "#d8d0c0", 2)
    m = K.centered(text, h, w, 4 if small else 9, scale=2)
    if small:
        m = np.maximum(m, K.centered(small, h, w, 22, scale=1))
    c = T.hexc(color)
    if lit:
        img = T.blend(img, np.ones((h, w, 3)) * c, K.glow(m, 2.0) * 0.6)
        img = T.blend(img, np.ones((h, w, 3)) * (c * 0.5 + 0.5), m)
    else:
        img = T.blend(img, np.ones((h, w, 3)) * (c * 0.3 + 0.1), m)
    return img


def pole():
    return T.grain(T.ramp(T.noise("mtpl", sigma=1), "#5a6064", "#7a8084"), "mtpl", 0.03)


def lamp_head():
    img = T.ramp(T.noise("mtlh", sigma=1), "#ffb050", "#ffd090")
    return T.border(img, "#3a3a3c", 3)


def wood_pole():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtwp", sigma=0.6, shape=(S, S)) * 0.5 + (np.sin(xx / 1.5) * 0.25 + 0.25), "#2e2218", "#4a3828")
    return T.grain(img, "mtwp", 0.04)


def dirt():
    img = T.ramp(T.fbm("mtdi", octaves=3, base=4), "#3a3020", "#4a3e2a")
    grass = (T.noise("mtdig", sigma=0.6) > 0.55).astype(float)
    img = T.blend(img, T.solid("#5a5a30"), grass * 0.6)
    return T.grain(img, "mtdi", 0.05)


def dry_grass():
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.zeros((S, S))
    r = T.rng("mtdg")
    for k in range(14):
        x0 = r.random() * S
        lean = (r.random() - 0.5) * 8
        top = 4 + r.random() * 18
        a = np.maximum(a, K.line_mask(S, S, x0, 32, x0 + lean, top, 1.0))
    rgb = T.ramp(T.noise("mtdgc", sigma=1), "#6a6232", "#9a8a4a")
    return rgb, a


def green_metal():
    return T.grain(T.ramp(T.noise("mtgm", sigma=1.5), "#1e4a2e", "#2a5a3a"), "mtgm", 0.04)


def ice_front():
    img = T.ramp(T.noise("mtice", sigma=1.5), "#d8dce0", "#e8ecf0")
    img = T.blend(img, T.solid("#1a5ab0"), K.centered("ICE", S, S, 8, font=K.F57, scale=1))
    img = T.rect(img, 8, 24, 24, 30, "#3a3e42")
    return T.grain(img, "mtice", 0.02)


def vending_front():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.solid("#0e1418")
    for row in range(4):
        for col in range(4):
            c = ["#d02a2a", "#2a6ad0", "#e0c020", "#2ab04a"][(row + col) % 4]
            img = T.rect(img, 3 + col * 5, 3 + row * 6, 6 + col * 5, 7 + row * 6, c)
    img = T.rect(img, 24, 3, 30, 27, "#2a2e32")
    img = T.rect(img, 25, 6, 29, 9, "#3aff6a")
    img = T.blend(img, T.solid("#d8e8ff"), (np.clip(1 - np.abs(xx - 12) / 12, 0, 1) * 0.15))
    return T.border(img, "#b02020", 2)


def carpet():
    img = T.ramp(T.noise("mtcp", sigma=0.5) * 0.5 + T.fbm("mtcpf", octaves=3, base=4) * 0.5, "#5a3a1e", "#6e4826")
    r = T.rng("mtflor")
    for k in range(5):  # tired floral pattern
        x, y = r.random() * S, r.random() * S
        for a in range(5):
            ang = a / 5 * 2 * np.pi
            img = T.blend(img, T.solid("#b8762a"), K.disc(S, S, (x + np.cos(ang) * 2.5) % S, (y + np.sin(ang) * 2.5) % S, 1.2).astype(float) * 0.7)
        img = T.blend(img, T.solid("#3a6a2a"), K.disc(S, S, x, y, 1.0).astype(float) * 0.8)
    img = T.stains(img, "mtcps", "#2a1a0e", count=2, strength=0.35, size=6)
    return T.grain(img, "mtcp", 0.04)


def wallpaper():
    yy, xx = np.mgrid[0:S, 0:S]
    stripe = ((xx // 4) % 4)
    cols = np.array([T.hexc("#c8a060"), T.hexc("#b88a4a"), T.hexc("#8a5a2a"), T.hexc("#b88a4a")])
    img = cols[stripe]
    img = T.shade(img, 0.9 + T.noise("mtwpn", sigma=2) * 0.15)
    img = T.stains(img, "mtwps", "#5a3a1a", count=1, strength=0.2, size=9)
    return T.grain(img, "mtwpp", 0.03)


def popcorn():
    img = T.ramp(T.noise("mtpop", sigma=0.4), "#d8d0bc", "#ece6d4")
    return T.stains(T.grain(img, "mtpop", 0.05), "mtpops", "#a8884a", count=1, strength=0.35, size=8)


def bathroom_tile():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtbt", sigma=1.5), "#d8e0dc", "#e8f0ec")
    img = T.shade(img, 1 - (((xx % 8) == 0) | ((yy % 8) == 0)) * 0.25)
    return T.grain(T.stains(img, "mtbts", "#8a8a6a", count=1, strength=0.2, size=6), "mtbt", 0.02)


def bedspread():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtbs", sigma=1), "#8a3a2a", "#9a4632")
    diamonds = (np.abs((xx % 8) - 4) + np.abs((yy % 8) - 4)) < 2
    img = T.blend(img, T.solid("#d8a040"), diamonds * 0.5)
    return T.grain(img, "mtbs", 0.03)


def bedspread_unmade():
    img = bedspread()
    img = T.shade(img, 0.85 + T.noise("mtbsu", sigma=1.2) * 0.3)
    return T.blend(img, T.solid("#e8e4d8"), (T.noise("mtbsw", sigma=2) > 0.6) * 0.8)


def wood():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtwo", sigma=1.0, shape=(S, S)) * 0.4 + (np.sin(yy / 2.0 + T.noise("mtwog", sigma=3) * 4) * 0.3 + 0.3),
                 "#4a2e18", "#6a4226")
    return T.grain(img, "mtwo", 0.03)


def lampshade():
    img = T.ramp(T.noise("mtls", sigma=1.5), "#f0d8a0", "#fff0c8")
    return img


def tv_screen(kind):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        if kind == "off":
            img = T.ramp(T.noise("mttv", sigma=3), "#141614", "#222622")
            img = T.blend(img, T.solid("#5a6a62"), np.clip(1 - np.abs((xx - yy) % 30 - 6) / 2, 0, 1) * 0.3)
        elif kind.startswith("static"):
            img = np.repeat(T.rng("mtst" + kind).random((S, S))[..., None], 3, axis=2) * 0.85 + 0.1
            img = T.shade(img, 1 - (yy % 2 == 0) * 0.2)
        else:  # it shows this room, from where the TV is
            img = T.ramp(T.noise("mttvr", sigma=3), "#1a1410", "#2a2018")
            img = T.rect(img, 6, 18, 26, 26, "#8a3a2a")
            img = T.rect(img, 6, 15, 26, 18, "#e8e4d8")
            img = T.rect(img, 14, 9, 16, 15, "#0a0806")  # someone standing at the foot of the bed
            img = T.blend(img, T.solid("#0a0806"), K.disc(S, S, 15, 7.5, 1.5).astype(float))
            img = T.shade(img, 1 - (yy % 2 == 0) * 0.25)
        d = np.sqrt(((xx - 15.5) / 16) ** 2 + ((yy - 15.5) / 16) ** 2)
        img = T.shade(img, 1 - np.clip(d - 0.6, 0, 1) * 1.2)
        return T.border(img, "#1a1a1a", 3)
    return f


def painting(kind):
    def f():
        img = T.solid("#3a2a1a")
        inner = np.zeros((24, 24, 3))
        yy, xx = np.mgrid[0:24, 0:24]
        if kind == "lake":
            inner = T.vgrad(24, 24, "#e8a060", "#3a5a7a")
            inner[14:] = T.vgrad(10, 24, "#2a4a5a", "#1a2a3a")
            inner = T.blend(inner, T.solid("#1a2a1a", shape=(24, 24)), ((yy > 10) & (yy < 15) & ((xx * 0.5 + yy) % 7 < 4)) * 0.8)
        else:  # the motel, at night, from the parking lot. one window lit.
            inner = T.vgrad(24, 24, "#0a0c18", "#141824")
            inner[12:20] = T.hexc("#7a5a48")
            inner[11] = T.hexc("#2a8a88")
            for k in range(5):
                inner[14:18, 2 + k * 5:4 + k * 5] = T.hexc("#2a2018")
            inner[14:18, 12:14] = T.hexc("#ffc060")
            inner[20:] = T.hexc("#1e1e20")
            inner[21:23, 11] = T.hexc("#0a0a0a")
            inner[19:21, 11] = T.hexc("#0a0a0a")
        img[4:28, 4:28] = inner
        return T.border(img, "#b8963a", 2)
    return f


def mirror():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtmr", sigma=3), "#7a8a8e", "#a8b8bc")
    img = T.blend(img, T.solid("#e0ecf0"), np.clip(1 - np.abs((xx + yy * 0.7) % 26 - 8) / 3, 0, 1) * 0.5)
    return T.border(img, "#c8c8c8", 2)


def porcelain():
    return T.grain(T.ramp(T.noise("mtpc", sigma=2), "#e0e0d8", "#f0f0e8"), "mtpc", 0.02)


def phone():
    return T.grain(T.ramp(T.noise("mtph", sigma=1.5), "#c8b8a0", "#d8c8b0"), "mtph", 0.02)


def office_window():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtow", sigma=2), "#e8d8a8", "#f8ecc8")
    img = T.rect(img, 0, 20, 32, 32, "#6a4a2a")  # the counter
    img = T.rect(img, 14, 17, 18, 20, "#d0b040")  # the bell
    img = T.blend(img, T.solid("#2a2a2a"), K.centered("RING BELL", S, S, 4, font=K.F35, spacing=0) * 0.7)
    frame = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29)
    return T.blend(img, T.solid("#d8d4c8"), frame.astype(float))


tex("motel_asphalt", asphalt)
tex("motel_asphalt_line", lambda: asphalt("mtal", "white"))
tex("motel_asphalt_yellow", lambda: asphalt("mtay", "yellow"))
tex("motel_concrete", lambda: concrete("mtco"))
tex("motel_curb", lambda: concrete("mtcu", "#9a988e", "#aaa89e"))
tex("motel_brick", brick)
tex("motel_door_top", lambda: door(True))
tex("motel_door_bottom", lambda: door(False))
tex("motel_door_inside_top", lambda: door(True, True))
tex("motel_door_inside_bottom", lambda: door(False, True))
tex("motel_door_7", door7)
tex("motel_window_lit", window_lit)
tex("motel_window_dark", window_dark)
tex("motel_soffit", soffit)
tex("motel_bulb", bulb)
tex("motel_fascia", fascia)
tex("motel_gravel", gravel)
for part, t in enumerate(K.tiles(sign_image("MOTEL", "#ff3a3a"))):
    tex(f"motel_sign_motel_{part}", (lambda t=t: t))
for part, t in enumerate(K.tiles(sign_image("VACANCY", "#3aff8a", True, "COLOR TV  ICE"))):
    tex(f"motel_sign_vacancy_{part}", (lambda t=t: t))
for part, t in enumerate(K.tiles(sign_image("VACANCY", "#3aff8a", False, "COLOR TV  ICE"))):
    tex(f"motel_sign_vacancy_off_{part}", (lambda t=t: t))
for part, t in enumerate(K.tiles(sign_image("OFFICE", "#ffc040"))):
    tex(f"motel_sign_office_{part}", (lambda t=t: t))
tex("motel_pole", pole)
tex("motel_lamp_head", lamp_head)
tex("motel_wood_pole", wood_pole)
tex("motel_dirt", dirt)
tex("motel_dry_grass", dry_grass)
tex("motel_green_metal", green_metal)
tex("motel_ice_front", ice_front)
tex("motel_vending_front", vending_front)
tex("motel_carpet", carpet)
tex("motel_wallpaper", wallpaper)
tex("motel_popcorn", popcorn)
tex("motel_bathroom_tile", bathroom_tile)
tex("motel_bedspread", bedspread)
tex("motel_bedspread_unmade", bedspread_unmade)
tex("motel_wood", wood)
tex("motel_lampshade", lampshade)
for k in ("off", "room"):
    tex(f"motel_tv_{k}", tv_screen(k))
K.animated("motel_tv_static", lambda i: tv_screen(f"static{i}")(), 4, 1)
tex("motel_painting_lake", painting("lake"))
tex("motel_painting_motel", painting("motel"))
tex("motel_mirror", mirror)
tex("motel_porcelain", porcelain)
tex("motel_phone", phone)
tex("motel_office_window", office_window)

# ------------------------------------------------------------------ blocks: the strip

cube("motel_asphalt", "motel_asphalt", display="Asphalt", sound="stone")
cube("motel_asphalt_line", "motel_asphalt_line", display="Parking Line", sound="stone")
cube("motel_asphalt_yellow", "motel_asphalt_yellow", display="Center Line", sound="stone")
cube("motel_concrete", "motel_concrete", display="Walkway Concrete", sound="stone")
cube("motel_curb", "motel_curb", display="Curb", sound="stone")
cube("motel_brick", "motel_brick", display="Painted Brick", sound="stone")
cube("motel_door_top", "motel_door_top", display="Room 6", sound="wood")
custom("motel_door", "com.noclip.block.MotelDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/motel_door_bottom"}},
       display="Room 6", sound="wood", unbreakable=True)
cube("motel_door_inside_top", "motel_door_inside_top", display="Room 6 (Inside)", sound="wood")
custom("motel_door_inside", "com.noclip.block.MotelExitDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/motel_door_inside_bottom"}},
       display="Room 6 (Inside)", sound="wood", unbreakable=True)
exit_block("motel_door_7", "motel_door_7", "SECRET", trigger="USE", solid=True, display="Room 7", sound="wood")
cube("motel_window_lit", "motel_window_lit", display="Lit Window", sound="glass", light=7, emissive=True)
cube("motel_window_dark", "motel_window_dark", display="Dark Window", sound="glass")
cube("motel_soffit", "motel_soffit", display="Soffit", sound="wood", flat=True)
flicker_light("motel_bulb", "motel_bulb", "motel_soffit", "motel_soffit", light=12, flicker=0.08,
              display="Porch Light")
cube("motel_fascia", "motel_fascia", display="Teal Fascia", sound="wood")
cube("motel_gravel", "motel_gravel", display="Roof Gravel", sound="gravel")
for part in range(3):
    cube(f"motel_sign_motel_{part}", f"motel_sign_motel_{part}", display="Motel Sign", sound="glass", light=10,
         emissive=True)
    flicker_light(f"motel_sign_vacancy_{part}", f"motel_sign_vacancy_{part}", f"motel_sign_vacancy_off_{part}",
                  f"motel_sign_vacancy_{part}", light=8, flicker=0.9, face="all", display="Vacancy Sign")
    cube(f"motel_sign_office_{part}", f"motel_sign_office_{part}", display="Office Sign", sound="glass", light=8,
         emissive=True)
pillar("motel_pole", "motel_pole", "motel_pole", display="Steel Pole", sound="metal")
pillar("motel_wood_pole", "motel_wood_pole", "motel_wood_pole", display="Utility Pole", sound="wood")
cube_faces("motel_lamp", bottom="motel_lamp_head", top="motel_pole", side="motel_pole", display="Sodium Lamp",
           light=15, emissive=True, sound="glass", flat=True)
cube("motel_dirt", "motel_dirt", display="Vacant Lot", sound="gravel")
cross("motel_dry_grass", "motel_dry_grass", display="Dry Grass")
shaped("motel_wheel_stop", [(1, 0, 5, 15, 3, 11, "c")], {"c": "motel_curb"}, display="Wheel Stop", unbreakable=True)
shaped("motel_post", [(6.5, 0, 6.5, 9.5, 16, 9.5, "p")], {"p": "motel_pole"}, display="Walkway Post", unbreakable=True)
shaped("motel_ac", [(1, 2, 0, 15, 12, 8, {"all": "case", "south": "grille"})],
       {"case": "motel_curb", "grille": "motel_soffit"}, display="Wall AC Unit", unbreakable=True)
shaped("motel_dumpster", [(0, 0, 1, 16, 14, 15, {"all": "metal"}), (0, 14, 0, 16, 15, 16, "metal")],
       {"metal": "motel_green_metal"}, display="Dumpster", unbreakable=True)
shaped("motel_ice_machine", [(1, 0, 2, 15, 16, 14, {"all": "case", "north": "front"})],
       {"case": "motel_curb", "front": "motel_ice_front"}, display="Ice Machine", unbreakable=True)
shaped("motel_vending", [(1, 0, 2, 15, 16, 14, {"all": "red", "north": "front"})],
       {"red": "motel_green_metal", "front": "motel_vending_front"}, display="Vending Machine", kind="pickup",
       item="noclip:almond_water*1,noclip:energy_bar*2", light=6, emissive=True, sound="metal")
cube("motel_office_window", "motel_office_window", display="Office Window", sound="glass", light=9, emissive=True)
exit_block("motel_office_door", "motel_door_inside_bottom", "DOOR", trigger="USE", solid=True,
           display="Check Out", sound="wood")

# ------------------------------------------------------------------ blocks: room 6

cube("motel_carpet", "motel_carpet", display="Motel Carpet", sound="wool")
cube("motel_wallpaper", "motel_wallpaper", display="Motel Wallpaper", sound="wool")
cube("motel_popcorn", "motel_popcorn", display="Popcorn Ceiling", sound="wool", flat=True)
cube("motel_bathroom_tile", "motel_bathroom_tile", display="Bathroom Tile", sound="stone")
for kind in ("", "_unmade"):
    shaped(f"motel_bed{kind}", [(0, 0, 0, 16, 9, 16, {"all": "wood", "up": "spread"}), (0, 9, 0, 16, 10, 16, "spread")],
           {"wood": "motel_wood", "spread": f"motel_bedspread{kind}"}, display="Motel Bed", unbreakable=True)
shaped("motel_headboard", [(0, 0, 0, 16, 16, 2, "wood")], {"wood": "motel_wood"}, display="Headboard",
       unbreakable=True)
shaped("motel_nightstand", [(2, 0, 2, 14, 10, 14, "wood")], {"wood": "motel_wood"}, display="Nightstand",
       unbreakable=True)
shaped("motel_lamp_table", [(6, 0, 6, 10, 6, 10, "wood"), (4, 6, 4, 12, 14, 12, "shade")],
       {"wood": "motel_wood", "shade": "motel_lampshade"}, display="Table Lamp", light=11, emissive=True,
       unbreakable=True, collides=False)
shaped("motel_lamp_table_off", [(6, 0, 6, 10, 6, 10, "wood"), (4, 6, 4, 12, 14, 12, "shade")],
       {"wood": "motel_wood", "shade": "motel_porcelain"}, display="Table Lamp", unbreakable=True, collides=False)
for k in ("off", "static", "room"):
    shaped(f"motel_tv_{k}", [(0, 0, 2, 16, 8, 14, "wood"), (2, 8, 4, 14, 16, 13, {"all": "case", "north": "screen"})],
           {"wood": "motel_wood", "case": "motel_green_metal", "screen": f"motel_tv_{k}"}, display="Television",
           unbreakable=True, light=None if k == "off" else 6, emissive=None if k == "off" else True)
cube("motel_painting_lake", "motel_painting_lake", display="Painting", sound="wood")
cube("motel_painting_motel", "motel_painting_motel", display="Painting", sound="wood")
cube("motel_mirror", "motel_mirror", display="Mirror", sound="glass")
shaped("motel_sink", [(1, 8, 0, 15, 12, 10, {"all": "p"}), (6, 0, 0, 10, 8, 4, "p")], {"p": "motel_porcelain"},
       display="Sink", unbreakable=True)
shaped("motel_toilet", [(4, 0, 4, 12, 7, 14, "p"), (3, 7, 3, 13, 9, 14, "p"), (4, 7, 0, 12, 16, 4, "p")],
       {"p": "motel_porcelain"}, display="Toilet", unbreakable=True)
shaped("motel_phone", [(2, 0, 2, 14, 10, 14, "wood"), (4, 10, 5, 12, 13, 12, "p"), (4, 13, 5, 12, 15, 7, "p")],
       {"wood": "motel_wood", "p": "motel_phone"}, display="Nightstand with Telephone", unbreakable=True)

# ------------------------------------------------------------------ sound


def night_air():
    d = 30.0
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    # crickets: bursts of a narrow chirp, several insects out of step
    for k, (rate, f0, g) in enumerate(((0.83, 4300, 0.06), (1.07, 4700, 0.045), (0.61, 3900, 0.05))):
        n = int(0.12 * Y.SR)
        tt = np.arange(n) / Y.SR
        chirp = np.sin(2 * np.pi * f0 * tt) * (np.sin(2 * np.pi * 45 * tt) > 0) * np.sin(np.pi * tt / tt[-1])
        for at in np.arange(0.2 + k * 0.31, d, rate):
            K.circular_place(out, chirp, at, g)
    buzz = Y.hp(Y.hum(d, 60.0, "mtneon", buzz=1.2, harmonics=(2, 4, 6, 8, 10)), 200) * 0.06
    crackle = (Y.rng("mtcrk").random(len(t)) > 0.9997).astype(float)
    crackle = Y.bp(crackle, 1500, 8000) * 2
    wind = Y.lp(Y.pink(d, "mtwind"), 500) * 0.25 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 15))
    x = out + buzz + crackle * 0.3 + wind
    return Y.stereoize(Y.loopable(Y.norm(x, 0.42), 1.0), 0.004), "stereo"


def truck():
    d = 7.0
    t = Y.t_axis(d)
    env = np.exp(-((t - 3.5) ** 2) / 2.0)
    x = Y.lp(Y.brown(d, "mttr"), 400) * env + Y.bp(Y.white(d, "mttr2"), 300, 2500) * env * 0.3
    f = 70 * (1.05 - 0.1 * np.tanh(t - 3.5))
    x += np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * env * 0.3
    return Y.norm(x, 0.5)


def dog():
    out = np.zeros(int(4 * Y.SR))
    for k in range(3):
        n = int(0.22 * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 420 - 200 * tt / tt[-1]
        bark = (np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) + Y.bp(Y.white(0.22, f"mtdog{k}"), 500, 2500)[:n] * 0.6) * np.exp(-tt * 12)
        Y.place(out, bark, 0.2 + k * 0.6, 1 - k * 0.15)
    return Y.norm(Y.reverb(Y.lp(out, 1500), 3.5, 0.8, "mtdog"), 0.35)


def ice_drop():
    out = np.zeros(int(2.5 * Y.SR))
    r = Y.rng("mtice")
    for k in range(18):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = Y.bp(Y.white(0.05, f"mti{k}"), 2000, 9000)[:n] * np.exp(-tt * 90)
        Y.place(out, c, 0.1 + r.random() * 0.8, 0.3 + r.random() * 0.5)
    return Y.norm(Y.reverb(out, 1.5, 0.5, "mtice"), 0.5)


def tv_murmur():
    d = 4.0
    out = np.zeros(int(d * Y.SR))
    v = K.pa_voice("mttvm", syllables=18, f0=130, rate=0.2, band=(200, 1200))
    Y.place(out, v, 0.1, 0.6)
    laugh = Y.bp(Y.white(1.0, "mttvl"), 500, 2500) * np.sin(np.pi * np.arange(int(Y.SR)) / Y.SR) * (np.sin(2 * np.pi * 7 * np.arange(int(Y.SR)) / Y.SR) > 0)
    Y.place(out, laugh, 2.7, 0.3)
    return Y.norm(Y.lp(out, 900), 0.35)


def door_open():
    d = 1.8
    out = np.zeros(int(d * Y.SR))
    n = int(0.08 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, Y.bp(Y.white(0.08, "mtkey"), 1500, 8000)[:n] * np.exp(-tt * 60), 0.05)
    Y.place(out, Y.bp(Y.white(0.08, "mtkey2"), 1000, 6000)[:n] * np.exp(-tt * 60), 0.25)
    t = Y.t_axis(1.2)
    creak = np.sin(2 * np.pi * np.cumsum(220 + 90 * np.sin(2 * np.pi * 1.3 * t)) / Y.SR) * np.sin(np.pi * t / 1.2)
    creak *= (np.sin(2 * np.pi * 40 * t) > -0.2)
    Y.place(out, Y.bp(creak, 300, 3000) * 0.5, 0.45)
    return Y.norm(Y.reverb(out, 0.8, 0.3, "mtdoor"), 0.8)


def phone_ring():
    out = np.zeros(int(5.0 * Y.SR))
    for ring in range(2):
        n = int(1.8 * Y.SR)
        tt = np.arange(n) / Y.SR
        bell = (np.sin(2 * np.pi * 1100 * tt) + 0.6 * np.sin(2 * np.pi * 1650 * tt)) * (np.sin(2 * np.pi * 20 * tt) > 0)
        Y.place(out, bell * 0.4, 0.1 + ring * 2.6)
    return Y.norm(Y.reverb(out, 1.0, 0.3, "mtph"), 0.7)


def shower():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mtsh"), 1500, 9000) * 0.5 + Y.bp(Y.white(d, "mtsh2"), 200, 1200) * 0.3
    x *= np.minimum(t * 3, 1) * np.minimum((d - t) * 3, 1)
    return Y.norm(Y.reverb(x, 1.0, 0.4, "mtsh"), 0.5)


sound("ambient.motel", [("ambient/motel", night_air)], stream=True)
sound("ambient.motel.additions",
      [("ambient/motel_truck", truck), ("ambient/motel_dog", dog), ("ambient/motel_ice", ice_drop),
       ("ambient/motel_tv", tv_murmur)],
      subtitle="Night at the motel")
sound("event.motel_door", [("event/motel_door", door_open)], subtitle="Room 6 opens")
sound("event.motel_phone", [("event/motel_phone", phone_ring)], subtitle="The room phone rings")
sound("event.motel_shower", [("event/motel_shower", shower)], subtitle="The shower is running")

level("motel", fog="#0c1018", sky="#070a14", ambient="ambient.motel", additions="ambient.motel.additions",
      additions_chance=0.004, fixed_time=18000, skylight=True, ceiling=False, height=48)
