"""Level 5 - Terror Hotel. 1920s corridors, numbered doors, chandeliers, a gramophone playing
behind a wall somewhere. The room numbers count down. Room 000 goes to the boiler room."""
import numpy as np
from assets import cube, cube_faces, pillar, tex, shaped, door, cross, exit_block, custom, level, sound, lang, \
    BLOCK_MODELS, BLOCKSTATES, ITEM_MODELS, entity_model, entity_tex, part
import tex as T
import synth as Y
from content.classics_kit import draw_text, centered, box_model, metal, bricks, FONT
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas, skin

S = 32
yy, xx = np.mgrid[0:S, 0:S]


# ------------------------------------------------------------------ textures

def carpet():
    base = T.ramp(T.noise("l5c", sigma=0.5), "#5a0e12", "#6e1418")
    # medallion lattice: gold diamonds with black hearts, a repeat every 16 px
    u, v = (xx % 16) - 7.5, (yy % 16) - 7.5
    diamond = np.abs(u) + np.abs(v)
    gold = (np.abs(diamond - 6.5) < 0.9)
    inner = diamond < 3.2
    dots = (np.abs(u) < 1) & (np.abs(v) < 1)
    base = T.blend(base, T.solid("#b08a3a"), gold * 0.85)
    base = T.blend(base, T.solid("#1a0a08"), inner * 0.85)
    base = T.blend(base, T.solid("#c89a42"), dots * 0.9)
    corner = (np.abs(u) > 6.5) & (np.abs(v) > 6.5)
    base = T.blend(base, T.solid("#2a3a20"), corner * 0.7)
    wear = np.clip(T.fbm("l5cw", octaves=3, base=5) * 1.5 - 0.7, 0, 1)
    base = T.blend(base, T.solid("#3a1a14"), wear * 0.4)
    return T.grain(base, "l5c", 0.05)


def damask(seed="l5wp", bg=("#24382a", "#2c4232"), fg="#8a7a44"):
    img = T.ramp(T.noise(seed, sigma=1.2), *bg)
    u, v = (xx % 16) - 7.5, (yy % 32) - 15.5
    # a stylised flame/fleur motif built from ellipses
    m = ((u / 3.0) ** 2 + ((v + 4) / 7.0) ** 2 < 1) & ~((u / 1.6) ** 2 + ((v + 4) / 4.5) ** 2 < 1)
    m |= ((np.abs(u) - 4) ** 2 / 4 + ((v - 6) / 3.0) ** 2 < 1)
    m |= (np.abs(u) < 0.8) & (v > 2) & (v < 12)
    # offset second motif on alternate columns
    u2, v2 = ((xx + 8) % 16) - 7.5, ((yy + 16) % 32) - 15.5
    m2 = ((u2 / 2.0) ** 2 + (v2 / 3.0) ** 2 < 1)
    img = T.blend(img, T.solid(fg), (m | m2) * 0.55)
    img = T.shade(img, 1 - (T.fbm(seed + "a", octaves=3, base=6) - 0.5) * 0.25)
    return T.grain(img, seed, 0.03)


def wallpaper():
    return damask()


def wallpaper_stain():
    img = damask("l5ws")
    img = T.stains(img, "l5wss", "#14100a", count=2, strength=0.5, size=7)
    return img


def wallpaper_scratched():
    img = damask("l5wsc")
    r = T.rng("l5scr")
    for k in range(4):
        x0 = 8 + k * 4 + int(r.random() * 2)
        for y in range(4, 28):
            x = x0 + int((y - 4) * 0.15)
            img[y, x % S] = T.hexc("#c8b890")
            img[y, (x + 1) % S] = img[y, (x + 1) % S] * 0.6
    return img


def crown():
    img = damask("l5cr")
    img = T.rect(img, 0, 0, 32, 7, "#d8ccae")
    img = T.rect(img, 0, 7, 32, 8, "#8a7a5a")
    img = T.rect(img, 0, 2, 32, 3, "#b8aa88")
    for x in range(0, 32, 4):
        img = T.rect(img, x, 4, x + 2, 6, "#eae0c6")
    img = T.rect(img, 0, 8, 32, 9, "#1a1410")
    return img


def wainscot():
    wood = T.ramp(T.fbm("l5wd", octaves=2, base=3) * 0.5 + T.noise("l5wdg", sigma=0.5) * 0.3, "#2e140c", "#401e12")
    wood = T.shade(wood, 1 + np.sin(xx * 0.5 + T.noise("l5wg2", sigma=3) * 4) * 0.04)
    img = wood.copy()
    img = T.rect(img, 3, 6, 29, 26, "#36180e")
    img = T.border(img, "#1c0c06", 1)
    img[6, 3:29] = T.hexc("#5a2e1a")
    img[6:26, 3] = T.hexc("#5a2e1a")
    img[25, 3:29] = T.hexc("#1a0a06")
    img[6:26, 28] = T.hexc("#1a0a06")
    img = T.rect(img, 0, 0, 32, 3, "#5a3420")
    img = T.rect(img, 0, 29, 32, 32, "#1a0c06")
    return T.grain(img, "l5wd", 0.03)


def ceiling():
    img = T.ramp(T.fbm("l5ce", octaves=3, base=5), "#c8bc9c", "#d6cbac")
    img = T.border(img, "#a89a78", 2)
    img = T.border(img, "#e2d8bc", 1)
    img = T.stains(img, "l5ces", "#8a7a50", 1, 0.2, 8)
    return T.grain(img, "l5ce", 0.02)


def door_tex(top, seed="l5d"):
    def f():
        wood = T.ramp(T.fbm(seed, octaves=2, base=3) * 0.5 + 0.25, "#3a1a0e", "#4c2414")
        wood = T.shade(wood, 1 + np.sin(yy * 0.4 + T.noise(seed + "g", sigma=3) * 5) * 0.05)
        img = T.border(wood, "#1a0a04", 2)
        if top:
            img = T.rect(img, 6, 8, 26, 32, "#2a1008")
            img = T.rect(img, 7, 9, 25, 32, "#44200f")
            img = T.rect(img, 14, 14, 18, 18, "#c8a44a")
            img = T.rect(img, 15, 15, 17, 17, "#0a0604")
        else:
            img = T.rect(img, 6, 0, 26, 24, "#2a1008")
            img = T.rect(img, 7, 0, 25, 23, "#44200f")
            img = T.rect(img, 22, 2, 26, 6, "#d8b458")
            img = T.rect(img, 23, 3, 25, 5, "#8a6a28")
            img = T.rect(img, 22, 8, 25, 10, "#1a1008")
        return T.grain(img, seed, 0.03)
    return f


def boiler_door(top):
    def f():
        img = door_tex(top, "l5bd")()
        # red light leaking round the edges: it is ajar
        glow = T.hexc("#ff3a12")
        img[:, 0:2] = glow
        img[:, 2] = T.hexc("#a01808")
        if top:
            img = T.rect(img, 9, 2, 23, 7, "#c8a44a")
            img = centered(img, "000", 2, "#1a0a04")
        return img
    return f


def brass():
    img = T.ramp(T.noise("l5br", sigma=1.5), "#9a7428", "#d8b058")
    img = T.shade(img, 0.85 + 0.3 * np.sin(np.pi * (yy + 0.5) / S))
    return T.grain(img, "l5br", 0.03)


def digit(n):
    def f():
        img = brass()
        img = T.blend(img, T.solid("#6a4a18"), (((xx < 2) | (xx > 29) | (yy < 2) | (yy > 29))).astype(float) * 0.8)
        g = FONT[str(n)]
        for gy, row in enumerate(g):
            for gx, bit in enumerate(row):
                if bit == "1":
                    img[4 + gy * 5:4 + gy * 5 + 5, 8 + gx * 6:8 + gx * 6 + 5] = T.hexc("#1a1008")
        return img
    return f


def shade_tex():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 20) ** 2)
    img = T.ramp(np.clip(1 - d / 26, 0, 1), "#e8a050", "#ffd890", "#fff2d0")
    img = T.shade(img, 1 - ((xx % 4) == 0) * 0.08)
    return img


def crystal():
    rgb = T.ramp(T.noise("l5cry", sigma=0.7), "#fff4d8", "#ffffff")
    a = np.ones((S, S)) * 0.9
    facets = ((xx + yy) % 6 == 0) | ((xx - yy) % 6 == 0)
    rgb = T.blend(rgb, T.solid("#ffd890"), facets * 0.6)
    return rgb, a


def chain_tex():
    rgb = T.solid("#8a6a28")
    a = ((np.abs(xx - 15.5) < 1.2) & ((yy % 6) < 4)).astype(float)
    a += ((np.abs(xx - 15.5) < 0.6) & ((yy % 6) >= 4)).astype(float)
    return rgb, np.clip(a, 0, 1)


def painting(kind):
    def f():
        if kind == 0:   # a portrait whose face has been scratched out
            img = T.vgrad(S, S, "#2a2018", "#14100c")
            d = np.sqrt(((xx - 16) / 6.0) ** 2 + ((yy - 12) / 7.5) ** 2)
            img = T.blend(img, T.solid("#c8a888"), (d < 1).astype(float))
            body = (yy > 19) & (np.abs(xx - 16) < 10 - (31 - yy) * 0.1)
            img = T.blend(img, T.solid("#1a1a24"), body.astype(float))
            r = T.rng("l5scratch")
            for _ in range(40):
                x, y = 11 + int(r.random() * 10), 6 + int(r.random() * 12)
                img[y, x] = T.hexc("#e8dcc8") if r.random() < 0.5 else T.hexc("#3a2a20")
        elif kind == 1:   # the hotel itself, at night, every window dark but one
            img = T.vgrad(S, S, "#0c1020", "#1a1a28")
            img = T.rect(img, 5, 10, 27, 30, "#2a2220")
            for wy in range(12, 28, 4):
                for wx in range(7, 26, 4):
                    img = T.rect(img, wx, wy, wx + 2, wy + 2, "#100c0a")
            img = T.rect(img, 19, 20, 21, 22, "#ffc860")
            img = T.rect(img, 3, 30, 29, 32, "#1a2a14")
        else:   # a stormy sea
            img = T.vgrad(S, S, "#3a4048", "#1a2028")
            waves = np.sin(xx / 3.0 + yy * 0.7) * 0.5 + 0.5
            sea = yy > 16
            img = T.blend(img, T.ramp(waves * 0.6 + T.noise("l5sea", sigma=1) * 0.4, "#0e1a24", "#3a4e5a"), sea * 1.0)
        img = T.border(img, "#c8a44a", 2)
        img = T.border(img, "#6a4a18", 1)
        return T.grain(img, f"l5p{kind}", 0.03)
    return f


def sheets():
    img = T.ramp(T.noise("l5sh", sigma=1.5), "#d8d0c0", "#e8e2d4")
    img = T.shade(img, 1 - ((T.noise("l5shw", sigma=2) > 0.7) * 0.08))
    return T.grain(img, "l5sh", 0.02)


def blanket():
    img = T.ramp(T.noise("l5bl", sigma=1), "#6a1418", "#7a1c20")
    img = T.blend(img, T.solid("#b08a3a"), ((yy % 8) == 0) * 0.6)
    return T.grain(img, "l5bl", 0.03)


def dark_wood():
    return T.grain(T.ramp(T.fbm("l5dkw", octaves=2, base=3), "#2a1208", "#3a1a0e"), "l5dkw", 0.03)


def boiler_shell():
    img = metal("l5bs", "#3a3430", "#4a4440", sigma=2, rust=("#4a2a16", "#7a3a18"), rust_amt=0.15)
    img = T.shade(img, 0.75 + 0.45 * np.sin(np.pi * (xx + 0.5) / S))
    for y in (3, 28):
        for x in range(2, 32, 5):
            img[y, x] = T.hexc("#1a1614")
            img[y - 1, x] = T.hexc("#6a6460")
    img = T.rect(img, 0, 14, 32, 16, "#2a2420")
    return T.grain(img, "l5bs", 0.03)


def boiler_top():
    img = metal("l5bt", "#3a3430", "#4a4440", sigma=2, rust=("#4a2a16", "#7a3a18"), rust_amt=0.1)
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.shade(img, 1 - (np.abs(d - 10) < 1) * 0.4)


def furnace():
    img = boiler_shell()
    img = T.rect(img, 6, 8, 26, 26, "#1a1210")
    fire = T.ramp(T.fbm("l5fire", octaves=3, base=2), "#a01808", "#ff6a10", "#ffd060")
    m = np.zeros((S, S))
    m[10:24, 8:24] = 1
    m[10:24, 8:24] *= ((xx[10:24, 8:24] % 4) != 0)
    img = T.blend(img, fire, m)
    return img


def boiler_brick():
    img = bricks("l5bb", "#4a2016", "#5e2a1c", mortar="#1e1410", bw=8, bh=4)
    soot = T.fbm("l5bbs", octaves=3, base=4)
    return T.blend(img, T.solid("#0e0a08"), np.clip(soot * 1.4 - 0.6, 0, 0.7))


def boiler_floor():
    img = T.ramp(T.fbm("l5bf", octaves=4, base=4), "#2a2420", "#3a322c")
    img = T.cracks(img, "l5bfc", "#120e0c", 2, 0.6)
    return T.stains(img, "l5bfs", "#0a0806", 2, 0.5, 8)


def glow_grate():
    base = T.ramp(T.noise("l5gg", sigma=3), "#5a1004", "#c83a0c")
    bar = ((xx % 8) < 2) | ((yy % 8) < 2)
    steel = metal("l5ggs", "#2a2624", "#3e3834")
    return np.where(bar[..., None], steel, base)


def furnace_hatch():
    img = metal("l5fh", "#2a2420", "#3a322c", sigma=2)
    img = T.border(img, "#120e0c", 2)
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.ramp(T.fbm("l5fh2", octaves=2, base=2), "#ff4a10", "#ffd060"), (d < 9).astype(float))
    img = T.blend(img, T.solid("#120e0c"), ((np.abs(d - 9) < 1.2) | ((np.abs(xx - 15.5) < 1) & (d < 9))).astype(float))
    return img


TEX = {
    "l5_carpet": carpet, "l5_wallpaper": wallpaper, "l5_wallpaper_stain": wallpaper_stain,
    "l5_wallpaper_scratched": wallpaper_scratched, "l5_crown": crown, "l5_wainscot": wainscot,
    "l5_ceiling": ceiling, "l5_door_low": door_tex(False), "l5_door_top": door_tex(True),
    "l5_boiler_door_low": boiler_door(False), "l5_boiler_door_top": boiler_door(True),
    "l5_brass": brass, "l5_shade": shade_tex, "l5_crystal": crystal, "l5_chain": chain_tex,
    "l5_painting_0": painting(0), "l5_painting_1": painting(1), "l5_painting_2": painting(2),
    "l5_sheets": sheets, "l5_blanket": blanket, "l5_dark_wood": dark_wood,
    "l5_boiler_shell": boiler_shell, "l5_boiler_top": boiler_top, "l5_furnace": furnace,
    "l5_boiler_brick": boiler_brick, "l5_boiler_floor": boiler_floor, "l5_glow_grate": glow_grate,
    "l5_furnace_hatch": furnace_hatch,
}
for k, fn in TEX.items():
    tex(k, fn)
for n in range(10):
    tex(f"l5_digit_{n}", digit(n))

# ------------------------------------------------------------------ blocks

cube("hotel_carpet", "l5_carpet", display="Hotel Carpet", sound="wool")
cube("hotel_wallpaper", "l5_wallpaper", display="Damask Wallpaper", sound="wool")
cube("hotel_wallpaper_stained", "l5_wallpaper_stain", display="Stained Damask Wallpaper", sound="wool")
cube("hotel_wallpaper_scratched", "l5_wallpaper_scratched", display="Scratched Damask Wallpaper", sound="wool")
cube("hotel_crown_molding", "l5_crown", display="Crown Molding", sound="wool")
cube("hotel_wainscot", "l5_wainscot", display="Mahogany Wainscot", sound="wood")
cube("hotel_ceiling", "l5_ceiling", display="Plaster Ceiling", flat=True)
cube("hotel_door_closed", "l5_door_low", display="Room Door", sound="wood")
cube("hotel_door_closed_top", "l5_door_top", display="Room Door", sound="wood")
door("hotel_room_door", "l5_door_top", "l5_door_low", display="Unlocked Room Door", unbreakable=True)

custom("hotel_boiler_door", "com.noclip.block.L5BoilerDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/l5_boiler_door_low"}},
       display="Room 000", sound="wood", unbreakable=True, light=5)
custom("hotel_boiler_door_top", "com.noclip.block.L5BoilerDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/l5_boiler_door_top"}},
       display="Room 000", sound="wood", unbreakable=True, light=5)

# room number plaque: a brass plate with three digit tiles, one block per (facing, hundreds, tens, units)
BLOCK_MODELS["hotel_room_plaque_base"] = box_model([(3.5, 4, 0, 12.5, 9, 0.6, "brass")], {"brass": "l5_brass"})
DIGIT_X = (4, 7, 10)
for pos in range(3):
    for n in range(10):
        x0 = DIGIT_X[pos]
        m = {"parent": "minecraft:block/block", "textures": {"d": f"noclip:block/l5_digit_{n}",
                                                              "particle": "noclip:block/l5_brass"},
             "elements": [{"from": [x0, 4.5, 0.6], "to": [x0 + 2, 8.5, 0.8],
                           "faces": {"south": {"texture": "#d", "uv": [0, 0, 16, 16]}}}]}
        BLOCK_MODELS[f"hotel_room_plaque_d{pos}_{n}"] = m
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
mp = []
for f, y in ROT.items():
    e = {"model": "noclip:block/hotel_room_plaque_base"}
    if y:
        e["y"] = y
    mp.append({"when": {"facing": f}, "apply": e})
    for pos, prop in enumerate(("hundreds", "tens", "units")):
        for n in range(10):
            e = {"model": f"noclip:block/hotel_room_plaque_d{pos}_{n}"}
            if y:
                e["y"] = y
            mp.append({"when": {"facing": f, prop: str(n)}, "apply": e})
custom("hotel_room_plaque", "com.noclip.block.L5PlaqueBlock", BLOCK_MODELS["hotel_room_plaque_base"],
       blockstate={"multipart": mp}, display="Room Number", sound="metal", unbreakable=True, render="cutout",
       no_collision=True)
BLOCK_MODELS["hotel_room_plaque"] = BLOCK_MODELS["hotel_room_plaque_base"]

shaped("hotel_sconce", [
    (6, 3, 0, 10, 5, 1, "brass"), (7.5, 5, 0.5, 8.5, 9, 3, "brass"),
    (5, 9, 1, 11, 14, 6, "shade"),
], {"brass": "l5_brass", "shade": "l5_shade"}, display="Wall Sconce", light=12, emissive=True, collides=False,
    sound="glass", unbreakable=True)
shaped("hotel_sconce_dead", [
    (6, 3, 0, 10, 5, 1, "brass"), (7.5, 5, 0.5, 8.5, 9, 3, "brass"),
    (5, 9, 1, 11, 14, 6, "wood"),
], {"brass": "l5_brass", "wood": "l5_dark_wood"}, display="Dead Sconce", collides=False, sound="glass",
    unbreakable=True)
shaped("hotel_chandelier", [
    (7.5, 12, 7.5, 8.5, 16, 8.5, "chain"),
    (3, 9, 3, 13, 12, 13, "brass"),
    (1, 4, 6, 4, 10, 10, "crystal"), (12, 4, 6, 15, 10, 10, "crystal"),
    (6, 4, 1, 10, 10, 4, "crystal"), (6, 4, 12, 10, 10, 15, "crystal"),
    (6, 1, 6, 10, 9, 10, "crystal"),
], {"chain": "l5_brass", "brass": "l5_brass", "crystal": "l5_crystal"}, display="Chandelier", light=15,
    emissive=True, collides=False, sound="glass", unbreakable=True, render="translucent")
cross("hotel_chain", "l5_chain", display="Brass Chain", unbreakable=True)
for k in range(3):
    shaped(f"hotel_painting_{k}", [(1, 1, 0, 15, 15, 1, {"all": "frame", "south": "art"})],
           {"frame": "l5_brass", "art": f"l5_painting_{k}"}, display="Painting", collides=False, sound="wood",
           unbreakable=True)
shaped("hotel_bed", [
    (0, 3, 0, 16, 8, 16, {"all": "sheets", "up": "blanket"}),
    (0, 0, 0, 2, 3, 2, "wood"), (14, 0, 0, 16, 3, 2, "wood"), (0, 0, 14, 2, 3, 16, "wood"), (14, 0, 14, 16, 3, 16, "wood"),
    (0, 3, 0, 16, 14, 2, "wood"),
], {"sheets": "l5_sheets", "blanket": "l5_blanket", "wood": "l5_dark_wood"}, display="Hotel Bed", sound="wool",
    unbreakable=True)
shaped("hotel_nightstand_lamp", [
    (2, 0, 2, 14, 10, 14, "wood"), (7, 10, 7, 9, 14, 9, "brass"), (5, 14, 5, 11, 19, 11, "shade"),
], {"wood": "l5_dark_wood", "brass": "l5_brass", "shade": "l5_shade"}, display="Nightstand Lamp", light=10,
    emissive=True, sound="wood", unbreakable=True)

pillar("hotel_boiler_shell", "l5_boiler_top", "l5_boiler_shell", display="Boiler", sound="metal")
cube_faces("hotel_boiler_furnace", top="l5_boiler_top", bottom="l5_boiler_top", side="l5_furnace", display="Furnace",
           sound="metal", light=13, emissive=True)
cube("hotel_boiler_brick", "l5_boiler_brick", display="Boiler Room Brick")
cube("hotel_boiler_floor", "l5_boiler_floor", display="Boiler Room Floor")
cube("hotel_boiler_grate", "l5_glow_grate", display="Glowing Grate", sound="metal", light=9, emissive=True)
exit_block("hotel_furnace_hatch", "l5_furnace_hatch", "DOOR", trigger="USE", solid=True, display="Furnace Hatch",
           sound="metal", light=11, emissive=True)

# ------------------------------------------------------------------ the Bellhop


def bellhop_tex():
    c = Canvas()
    red = lambda w, h: T.grain(T.ramp(T.noise(f"bh{w}{h}", shape=(h, w), sigma=1), "#7a1014", "#8e161a"), f"bh{w}{h}", 0.04)

    def jacket(w, h):
        img = red(w, h)
        for y in range(1, h, 3):
            if w >= 8:
                img[y, w // 2 - 2] = T.hexc("#d8b458")
                img[y, w // 2 + 1] = T.hexc("#d8b458")
        return img

    def head(w, h):
        return T.ramp(T.noise(f"bhh{w}", shape=(h, w), sigma=1.5), "#d8ccb8", "#e4d8c6")

    def face(w, h):
        img = head(w, h)
        img[5, 1:7] = T.hexc("#c8b8a2")   # the faintest line where a mouth would be
        return img

    def arm(w, h):
        img = red(w, h)
        img[-3:] = T.hexc("#f2f0ea")
        img[-4] = T.hexc("#d8b458")
        return img

    def leg(w, h):
        img = T.grain(T.solid("#1a1418", shape=(h, w)), f"bhl{w}", 0.04)
        img[:, w // 2] = T.hexc("#7a1014")
        img[-2:] = T.hexc("#0a0808")
        return img

    paint_humanoid(c, head, jacket, arm, leg, face=face)
    c.box(32, 0, 6, 3, 6, all=lambda w, h: red(w, h), up=lambda w, h: T.solid("#7a1014", shape=(h, w)))
    c.box(32, 0, 6, 1, 6, front=lambda w, h: T.solid("#d8b458", shape=(h, w)))
    return c.out()


entity_model("bellhop", humanoid(head_extra=[(32, 0, -3, -11, -3, 6, 3, 6)], arm_len=14, leg_len=14))
entity_tex("bellhop", bellhop_tex)
lang("entity.noclip.bellhop", "The Bellhop")
lang("item.noclip.bellhop_spawn_egg", "Bellhop Spawn Egg")
ITEM_MODELS["bellhop_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def gramophone():
    """A waltz in a minor key, heard through two walls and a hundred years of dust."""
    d = 24.0
    t = Y.t_axis(d)
    beat = 0.5
    bars = int(d / (beat * 3))
    chords = [(220.0, 261.6, 329.6), (207.7, 246.9, 329.6), (220.0, 261.6, 329.6), (146.8, 174.6, 220.0),
              (164.8, 207.7, 246.9), (220.0, 261.6, 329.6), (174.6, 220.0, 261.6), (164.8, 207.7, 246.9)]
    x = np.zeros_like(t)
    for b in range(bars):
        ch = chords[b % len(chords)]
        for k in range(3):
            at = (b * 3 + k) * beat
            n = int(beat * 1.6 * Y.SR)
            tt = np.arange(n) / Y.SR
            env = np.exp(-tt * 4.5)
            if k == 0:
                note = np.sin(2 * np.pi * ch[0] / 2 * tt) * 1.2
            else:
                note = sum(np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(4 * np.pi * f * tt) for f in ch[1:]) * 0.5
            Y.place(x, note * env, at, 0.6)
        # a little melody on top, slightly out of tune
        mel = [659.3, 622.3, 659.3, 523.3, 587.3, 659.3, 698.5, 659.3][b % 8] * 1.006
        n = int(beat * 2.5 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(x, np.sin(2 * np.pi * mel * tt) * np.exp(-tt * 2.5) * (1 + 0.3 * np.sin(2 * np.pi * 5 * tt)), b * 3 * beat, 0.35)
    wow = 1 + 0.004 * np.sin(2 * np.pi * 0.5 * t)
    idx = np.clip((np.cumsum(wow) - 1).astype(int), 0, len(x) - 1)
    x = x[idx]
    x = Y.bp(x, 250, 2200)
    x = Y.lp(x, 900)   # through the walls
    crackle = (Y.rng("l5crk").random(len(t)) > 0.9985).astype(float) * Y.white(d, "l5crk2") * 0.3
    hiss = Y.hp(Y.white(d, "l5hiss"), 3000) * 0.01
    room = Y.lp(Y.brown(d, "l5room"), 140) * 0.25
    x = Y.norm(x, 0.35) + Y.bp(crackle, 1000, 6000) + hiss + room
    x = Y.reverb(x, 2.5, 0.45, "l5rv")[: len(t)]
    x = Y.loopable(Y.norm(x, 0.45), 1.0)
    return Y.stereoize(x, 0.0015), "stereo"


def knock_set(seed, n=3, gap=0.3, size=0.25):
    out = np.zeros(int(2.5 * Y.SR))
    for k in range(n):
        Y.place(out, Y.impact(f"{seed}{k}", size), 0.1 + k * gap, 1.0 - k * 0.1)
    return out


def add_knock(i):
    def f():
        return Y.norm(Y.reverb(Y.lp(knock_set(f"l5k{i}", 2 + i, 0.32 - i * 0.04), 1500), 2.4, 0.6, f"l5kv{i}"), 0.6)
    return f


def add_slam():
    out = np.zeros(int(3.5 * Y.SR))
    Y.place(out, Y.impact("l5slam", 1.2), 0.05)
    return Y.norm(Y.reverb(Y.lp(out, 800), 3.0, 0.65, "l5slam"), 0.6)


def add_bell():
    d = 3.0
    t = Y.t_axis(d)
    x = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * dk) / (k + 1) for k, (f, dk) in
            enumerate(((2093.0, 1.5), (5230.0, 3.0), (3140.0, 2.2))))
    return Y.norm(Y.reverb(x, 2.0, 0.5, "l5bell"), 0.45)


def add_steps():
    out = np.zeros(int(5 * Y.SR))
    for k in range(6):
        Y.place(out, Y.footstep(f"l5st{k}", "carpet"), 0.3 + k * 0.62, 0.5 + 0.08 * k)
    return Y.norm(Y.reverb(Y.lp(out, 700), 2.0, 0.55, "l5st"), 0.5)


def add_laugh():
    d = 1.8
    out = np.zeros(int(d * Y.SR))
    for k in range(5):
        n = int(0.16 * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 330 - k * 18
        v = Y.bp(np.sign(np.sin(2 * np.pi * f0 * tt)) * np.sin(np.pi * tt / 0.16), 400, 2600)
        Y.place(out, v, 0.1 + k * 0.22, 0.7)
    return Y.norm(Y.reverb(Y.lp(out, 1500), 2.5, 0.7, "l5laugh"), 0.35)


def close_knock():
    return Y.norm(Y.reverb(knock_set("l5ck", 3, 0.28, 0.3), 1.2, 0.3, "l5ckv"), 0.9)


def descend():
    d = 4.0
    t = Y.t_axis(d)
    creak = np.sin(2 * np.pi * np.cumsum(140 + 60 * np.sin(2 * np.pi * 1.3 * t)) / Y.SR) * np.exp(-t * 1.5) * 0.5
    creak *= (1 + 0.5 * np.sign(np.sin(2 * np.pi * 23 * t)))
    roar = Y.lp(Y.brown(d, "l5roar"), 200) * np.clip(t / 1.5, 0, 1)
    return Y.norm(Y.reverb(Y.bp(creak, 100, 3000) + roar, 2.0, 0.4, "l5des"), 0.9)


def boiler_roar():
    d = 8.0
    t = Y.t_axis(d)
    x = Y.lp(Y.brown(d, "l5br1"), 120) + Y.bp(Y.pink(d, "l5br2"), 80, 600) * 0.5
    x *= 0.8 + 0.2 * np.sin(2 * np.pi * 0.6 * t)
    clank = np.zeros_like(t)
    Y.place(clank, Y.impact("l5brc", 0.7, ring=0.6), 3.2, 0.5)
    return Y.norm((x + clank) * np.sin(np.pi * t / d) ** 0.3, 0.8)


sound("ambient.level_5", [("ambient/level_5", gramophone)], stream=True)
sound("ambient.level_5.additions",
      [(f"ambient/level_5_knock{i}", add_knock(i)) for i in range(2)] +
      [("ambient/level_5_slam", add_slam), ("ambient/level_5_bell", add_bell), ("ambient/level_5_steps", add_steps),
       ("ambient/level_5_laugh", add_laugh)],
      subtitle="The hotel settles")
sound("entity.bellhop.bell", [("entity/bellhop_bell", add_bell)], subtitle="A service bell rings")
sound("event.level_5_knock", [("event/level_5_knock", close_knock)], subtitle="Knocking on a door")
sound("event.level_5_descend", [("event/level_5_descend", descend)], subtitle="Room 000 opens")
sound("event.level_5_boiler", [("event/level_5_boiler", boiler_roar)], subtitle="The boilers roar")

level("level_5", fog="#2a160e", sky="#2a160e", ambient="ambient.level_5", additions="ambient.level_5.additions",
      additions_chance=0.006, fixed_time=18000, height=48)
