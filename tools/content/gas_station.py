"""Repeating Gas Station. A lit station on a dark two-lane road at night. Walk away in
any direction and you walk back into it from the other side. Each time, something has
changed."""
import numpy as np
from assets import cube, cube_faces, cross, shaped, pane, custom, exit_block, level, sound, lang, entity_model, \
    entity_tex, flicker_light, BLOCK_MODELS, ITEM_MODELS
import assets as A
import tex as T
import synth as Y
from content.outside_kit import sliced, draw_text, centered, text_width, box_model, facing_states, paint_metal
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32
tx = A.tex

# ------------------------------------------------------------------ ground


def asphalt(seed="gsa", line=None):
    img = T.ramp(T.fbm(seed, octaves=4, base=4) * 0.5 + T.noise(seed + "f", sigma=0.4) * 0.5,
                 "#1e1f21", "#2a2b2d", "#35363a")
    agg = (T.rng(seed + "ag").random((S, S)) > 0.93).astype(float)
    img = T.blend(img, T.solid("#55565a"), agg * 0.5)
    img = T.cracks(img, seed + "c", "#121314", count=1, alpha=0.5)
    if line:
        col, lo, hi = line
        paint = T.grain(T.ramp(T.noise(seed + "p", sigma=1), col, col), seed + "pp", 0.08)
        yy = np.arange(S)[:, None] * np.ones((1, S))
        worn = (T.noise(seed + "w", sigma=0.8) > 0.25).astype(float)
        img = T.blend(img, paint, ((yy >= lo) & (yy < hi)) * worn * 0.92)
    return T.grain(img, seed, 0.04)


def gravel():
    img = T.ramp(T.noise("gsgr", sigma=0.5), "#3a3936", "#5a5852", "#6e6a62")
    return T.grain(T.shade(img, 0.9 + T.noise("gsgr2", sigma=2) * 0.2), "gsgr", 0.06)


def concrete(seed, oil=False):
    img = T.ramp(T.fbm(seed, octaves=4, base=5), "#5a5a56", "#6a6a65", "#76756f")
    img = T.border(img, "#3e3e3b", 1, sides="nw")
    if oil:
        img = T.stains(img, seed + "o", "#1a1a1c", count=2, strength=0.75, size=9)
    img = T.stains(img, seed + "t", "#4a4a46", count=2, strength=0.3, size=7)
    return T.grain(img, seed, 0.04)


def night_grass():
    img = T.ramp(T.fbm("gsng", octaves=4, base=5) * 0.6 + T.noise("gsng2", sigma=0.4) * 0.4,
                 "#2a3022", "#37402c", "#465036")
    r = T.rng("gsngb")
    for _ in range(120):
        x, y = int(r.random() * S), int(r.random() * S)
        for k in range(3):
            img[(y + k) % S, x] = T.hexc("#56603e") * (0.8 + r.random() * 0.3)
    return T.grain(img, "gsng", 0.05)


def dirt():
    img = T.ramp(T.fbm("gsdirt", octaves=4, base=4), "#2e2820", "#3c342a")
    return T.grain(img, "gsdirt", 0.05)


def dirt_side():
    img = dirt()
    top = night_grass()
    yy = np.mgrid[0:S, 0:S][0]
    fr = 4 + (T.noise("gsfr", shape=(1, S), sigma=0.8)[0] * 5).astype(int)
    return T.blend(img, top, (yy < fr[None, :]).astype(float))


def weeds():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("gsweed")
    for _ in range(16):
        x0 = 3 + r.random() * 26
        h = 6 + r.random() * 16
        lean = (r.random() - 0.5) * 0.6
        c = T.ramp(np.array([[r.random()]]), "#3a4228", "#5e6440")[0, 0]
        for y in range(int(h)):
            px, py = int(x0 + lean * y), S - 1 - y
            if 0 <= px < S:
                rgb[py, px] = c
                a[py, px] = 1
        if r.random() < 0.5:
            py, px = int(S - h), int(x0 + lean * h)
            for dy in range(2):
                for dx in range(-1, 2):
                    if 0 <= px + dx < S and 0 <= py + dy < S:
                        rgb[py + dy, px + dx] = T.hexc("#8a8460")
                        a[py + dy, px + dx] = 1
    return rgb, a


tx("gs_asphalt", lambda: asphalt())
tx("gs_asphalt_yellow", lambda: asphalt("gsay", ("#b08a22", 13, 19)))
tx("gs_asphalt_white", lambda: asphalt("gsaw", ("#b8b6ac", 13, 17)))
tx("gs_gravel", gravel)
tx("gs_concrete", lambda: concrete("gsc"))
tx("gs_concrete_oil", lambda: concrete("gsco", oil=True))
tx("gs_grass_top", night_grass)
tx("gs_dirt", dirt)
tx("gs_dirt_side", dirt_side)
tx("gs_weeds", weeds)

cube("gs_asphalt", "gs_asphalt", display="Asphalt")
cube("gs_asphalt_yellow", "gs_asphalt_yellow", display="Asphalt (Centre Line)")
cube("gs_asphalt_white", "gs_asphalt_white", display="Asphalt (Edge Line)")
cube("gs_gravel", "gs_gravel", sound="gravel", display="Road Shoulder")
cube("gs_concrete", "gs_concrete", display="Forecourt Concrete")
cube("gs_concrete_oil", "gs_concrete_oil", display="Oil-Stained Concrete")
cube_faces("gs_grass", top="gs_grass_top", bottom="gs_dirt", side="gs_dirt_side", sound="grass", display="Roadside Grass")
cube("gs_dirt", "gs_dirt", sound="gravel", display="Roadside Dirt")
cross("gs_weeds", "gs_weeds", display="Roadside Weeds")

# ------------------------------------------------------------------ the shop


def block_wall():
    img = T.ramp(T.fbm("gswall", octaves=3, base=3), "#a8a496", "#bcb8aa")
    yy, xx = np.mgrid[0:S, 0:S]
    off = ((yy // 8) % 2) * 8
    mortar = ((yy % 8) == 0) | (((xx + off) % 16) == 0)
    img = T.shade(img, 1 - mortar * 0.18)
    grime = np.clip((yy - 18) / 14, 0, 1) * T.noise("gswg", sigma=2)
    img = T.shade(img, 1 - grime * 0.25)
    return T.grain(T.stains(img, "gsws", "#6a6658", count=1, strength=0.3, size=6), "gswall", 0.04)


def window():
    rgb = T.ramp(T.noise("gswin", sigma=3), "#8aa0a8", "#b0c4c8")
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.ones((S, S)) * 0.28
    streak = (np.abs((xx + yy * 0.7) % 26 - 6) < 2).astype(float)
    a = a + streak * 0.2
    # sun-faded stickers and a taped-up notice
    rgb = T.rect(rgb, 4, 20, 12, 28, "#d8c8a0")
    a[20:28, 4:12] = 0.95
    rgb = draw_text(rgb, "NO", 5, 21, "#3a2a1a")
    frame = (xx < 1) | (xx > 30) | (yy < 1) | (yy > 30)
    rgb[frame] = T.hexc("#6a6a68")
    a[frame] = 1
    return rgb, a


def tile_floor():
    yy, xx = np.mgrid[0:S, 0:S]
    chk = ((xx // 16 + yy // 16) % 2).astype(float)
    img = T.blend(T.solid("#c8c4b4"), T.solid("#8a3a32"), chk[..., None] * np.ones(3))
    img = T.shade(img, 0.92 + T.noise("gstf", sigma=2) * 0.12)
    img = T.border(img, "#6a6660", 1)
    return T.grain(T.stains(img, "gstfs", "#5a5040", count=1, strength=0.3, size=6), "gstf", 0.04)


def roof():
    return T.grain(T.ramp(T.noise("gsroof", sigma=0.6), "#26262a", "#3a3a3e"), "gsroof", 0.08)


def shop_sign():
    w, h = 10 * S, S
    img = np.ones((h, w, 3)) * T.hexc("#ece6d4")
    img = T.shade(img, 0.92 + T.noise("gsss", shape=(h, w), sigma=4) * 0.1)
    img[:3] = T.hexc("#a0221c")
    img[-3:] = T.hexc("#a0221c")
    img = draw_text(img, "FOOD MART", 22, 9, "#a0221c", scale=3, gap=2)
    img = draw_text(img, "OPEN 24 HOURS", 172, 11, "#2a2a30", scale=2, gap=2)
    # two letters are dead: the tubes behind them burned out
    dead = np.zeros((h, w))
    dead[:, 22 + 11 * 2: 22 + 11 * 3] = 1
    img = T.shade(img, 1 - dead[..., None] * 0.45)
    return img


def cooler(i):
    def f():
        img = T.solid("#e8f0f0")
        yy, xx = np.mgrid[0:S, 0:S]
        r = T.rng("gscool", i)
        cols = ["#c03028", "#2a5aa8", "#e8c040", "#3a8a3a", "#e0e0e0", "#a83a8a"]
        for shelf in range(4):
            y0 = 3 + shelf * 7
            img = T.rect(img, 1, y0 + 5, 31, y0 + 6, "#8a9090")
            x = 2
            while x < 29:
                c = cols[int(r.random() * len(cols))]
                if r.random() < 0.12:
                    x += 3
                    continue
                img = T.rect(img, x, y0, x + 2, y0 + 5, c)
                img = T.rect(img, x, y0, x + 2, y0 + 1, "#f0f0f0")
                x += 3
        img = T.border(img, "#9aa0a0", 1)
        img[:, 15:17] = T.hexc("#b0b8b8")
        return T.shade(img, 1 + ((xx + yy) % 19 < 2) * 0.08)
    return f


def counter():
    img = T.ramp(T.noise("gscnt", sigma=2), "#6a4a32", "#7a583c")
    img[:4] = T.hexc("#c8c0a8")
    return T.grain(img, "gscnt", 0.04)


def register():
    img = T.ramp(T.noise("gsreg", sigma=1), "#2e2e30", "#3e3e40")
    img = T.rect(img, 4, 4, 28, 12, "#1a2a1a")
    img = draw_text(img, "0.00", 8, 6, "#5aff7a")
    for k in range(9):
        img = T.rect(img, 6 + (k % 3) * 7, 16 + (k // 3) * 5, 10 + (k % 3) * 7, 19 + (k // 3) * 5, "#8a8a86")
    return img


tx("gs_wall", block_wall)
tx("gs_window", window)
tx("gs_tile", tile_floor)
tx("gs_roof", roof)
sliced("gs_shopsign", shop_sign, 10, 1)
for i in range(3):
    tx(f"gs_cooler_{i}", cooler(i))
tx("gs_counter", counter)
tx("gs_register", register)

cube("gs_wall", "gs_wall", display="Painted Block Wall")
pane("gs_window", "gs_window", display="Shop Window", sound="glass", render="translucent")
cube("gs_tile", "gs_tile", display="Shop Floor", sound="tile")
cube("gs_roof", "gs_roof", display="Tar Roof", sound="gravel")
for c in range(10):
    shaped(f"gs_shopsign_{c}", [(0, 0, 0, 16, 16, 16, {"all": "wall", "north": "sign"})],
           {"wall": "gs_wall", "sign": f"gs_shopsign_{c}_0"}, display="Shop Sign", light=10, unbreakable=True,
           sound="glass", render="solid")
for i in range(3):
    shaped(f"gs_cooler_{i}", [(0, 0, 0, 16, 16, 16, {"all": "wall", "north": "door"})],
           {"wall": "gs_wall", "door": f"gs_cooler_{i}"}, display="Drinks Cooler", light=9, emissive=True,
           unbreakable=True, sound="glass", render="solid")
shaped("gs_counter", [(0, 0, 2, 16, 14, 16, "c")], {"c": "gs_counter"}, display="Shop Counter", unbreakable=True)
shaped("gs_register", [(0, 0, 2, 16, 14, 16, "c"), (3, 14, 5, 13, 20, 13, {"all": "r", "north": "r"})],
       {"c": "gs_counter", "r": "gs_register"}, display="Cash Register", unbreakable=True, sound="metal")


def open_sign():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    m = np.zeros((S, S, 3))
    m = centered(m, "OPEN", 13, "#ff3a2a", scale=1)
    on = m.sum(axis=2) > 0.1
    from scipy.ndimage import binary_dilation
    glow = binary_dilation(on, iterations=1)
    rgb[glow] = T.hexc("#ff7a5a")
    a[glow] = 0.5
    rgb[on] = T.hexc("#ffd0c0")
    a[on] = 1
    return rgb, a


def glass_door(top):
    def f():
        rgb = np.ones((S, S, 3)) * T.hexc("#9aa8ac")
        yy, xx = np.mgrid[0:S, 0:S]
        a = np.zeros((S, S))
        frame = (xx < 3) | (xx > 28) | ((yy < 3) if top else (yy > 26))
        rgb[frame] = T.hexc("#7a7c7a")
        a[frame] = 1
        smudge = (T.noise("gsgd" + str(top), sigma=1.5) > 0.82) & ~frame
        rgb[smudge] = T.hexc("#c8d0d0")
        a[smudge] = 1
        if not top:
            bar = (yy > 2) & (yy < 5) & (xx > 4) & (xx < 27)
            rgb[bar] = T.hexc("#c0c0b8")
            a[bar] = 1
            rgb = draw_text(rgb, "PUSH", 9, 8, "#2a2a2a")
            a[8:13, 9:24] = np.maximum(a[8:13, 9:24], (rgb[8:13, 9:24].sum(axis=2) < 1.0))
        else:
            sticker = (yy > 18) & (yy < 26) & (xx > 8) & (xx < 23)
            rgb[sticker] = T.hexc("#e8c040")
            a[sticker] = 1
            rgb = draw_text(rgb, "24H", 10, 20, "#2a2a2a")
        return rgb, a
    return f


tx("gs_door_top", glass_door(True))
tx("gs_door_bottom", glass_door(False))
from assets import door
door("gs_glass_door", "gs_door_top", "gs_door_bottom", display="Shop Door", sound="metal", unbreakable=True)

tx("gs_open", open_sign)
shaped("gs_open_sign", [(1, 4, 7.5, 15, 12, 8.5, {"all": None, "north": "o", "south": "o"})], {"o": "gs_open"},
       display="Neon OPEN Sign", light=8, emissive=True, collides=False, unbreakable=True, sound="glass")

# ------------------------------------------------------------------ canopy and pumps


def canopy_under():
    img = T.ramp(T.noise("gscu", sigma=2), "#b8b8b2", "#c8c8c0")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.15)
    return T.grain(T.stains(img, "gscus", "#7a7464", count=1, strength=0.35, size=8), "gscu", 0.03)


def canopy_light(on=True):
    def f():
        img = T.solid("#fffef4" if on else "#8c8c86")
        yy, xx = np.mgrid[0:S, 0:S]
        if on:
            img = T.shade(img, 1 - ((xx % 4 == 0) | (yy % 4 == 0)) * 0.03)
        else:
            img = T.stains(img, "gsclo", "#3a3a2a", count=2, strength=0.5, size=7)
        img = T.border(img, "#9a9a94", 3)
        return img
    return f


def fascia():
    img = T.solid("#e6e2d8")
    img = T.shade(img, 0.92 + T.noise("gsfa", sigma=3) * 0.1)
    img[10:22] = T.hexc("#b0241c")
    img[14:16] = T.hexc("#e6e2d8")
    img = T.stains(img, "gsfas", "#6a6050", count=1, strength=0.25, size=8)
    return T.grain(img, "gsfa", 0.03)


def pillar():
    img = T.ramp(T.noise("gspil", sigma=2), "#d0ccc2", "#dedad0")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - np.clip((yy - 24) / 8, 0, 1) * 0.2)
    return T.grain(T.stains(img, "gspils", "#7a7060", count=1, strength=0.3, size=5), "gspil", 0.03)


def pump_body(top=False):
    def f():
        img = T.ramp(T.noise("gspb" + str(top), sigma=2), "#b42a22", "#c43a2c")
        img = T.grain(img, "gspb", 0.05)
        if top:
            img[2:6] = T.hexc("#e8e4d8")
            img = T.rect(img, 5, 10, 27, 24, "#141814")
            img = draw_text(img, "000.0", 7, 12, "#ffb040")
            img = draw_text(img, "00.00", 7, 18, "#ffb040")
        else:
            img = T.rect(img, 6, 4, 26, 12, "#e8e4d8")
            img = draw_text(img, "87", 9, 6, "#1a1a1a")
            img = T.rect(img, 20, 6, 24, 10, "#3a6a3a")
            img = T.rect(img, 8, 16, 24, 28, "#2a2a2a")
            img = T.rect(img, 12, 17, 20, 20, "#5a5a5a")
        img = T.stains(img, "gspbs" + str(top), "#4a1a14", count=1, strength=0.3, size=5)
        return img
    return f


def hose():
    return T.grain(T.solid("#141414"), "gshose", 0.05)


def curb():
    img = T.ramp(T.noise("gscurb", sigma=1), "#c8a832", "#d8b83e")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, T.solid("#1a1a18"), ((((xx + yy) // 6) % 2) == 0) * 0.85)
    return T.grain(img, "gscurb", 0.06)


tx("gs_canopy_under", canopy_under)
tx("gs_canopy_light", canopy_light(True))
tx("gs_canopy_light_off", canopy_light(False))
tx("gs_fascia", fascia)
tx("gs_pillar", pillar)
tx("gs_pump_lower", pump_body(False))
tx("gs_pump_upper", pump_body(True))
tx("gs_hose", hose)
tx("gs_curb", curb)

cube_faces("gs_canopy", top="gs_roof", bottom="gs_canopy_under", side="gs_fascia", flat=True, display="Canopy")
cube_faces("gs_canopy_edge", top="gs_roof", bottom="gs_canopy_under", side="gs_fascia", display="Canopy Fascia")
flicker_light("gs_canopy_light", "gs_canopy_light", "gs_canopy_light_off", "gs_fascia", top_tex="gs_roof",
              light=15, flicker=0.0, display="Canopy Light")
flicker_light("gs_canopy_light_flicker", "gs_canopy_light", "gs_canopy_light_off", "gs_fascia", top_tex="gs_roof",
              light=15, flicker=0.7, display="Failing Canopy Light")
cube_faces("gs_canopy_light_dead", bottom="gs_canopy_light_off", top="gs_roof", side="gs_fascia", flat=True,
           display="Dead Canopy Light")
cube("gs_pillar", "gs_pillar", display="Canopy Pillar")
shaped("gs_pump_lower", [(2, 0, 4, 14, 16, 12, "body"), (14, 4, 7, 15, 12, 9, "hose")],
       {"body": "gs_pump_lower", "hose": "gs_hose"}, display="Fuel Pump", unbreakable=True, sound="metal")
shaped("gs_pump_upper", [(2, 0, 4, 14, 10, 12, "body"), (1, 10, 3, 15, 12, 13, "top")],
       {"body": "gs_pump_upper", "top": "gs_canopy_under"}, display="Fuel Pump", unbreakable=True, sound="metal",
       light=4)
shaped("gs_curb", [(0, 0, 0, 16, 5, 16, {"all": "curb", "up": "conc"})], {"curb": "gs_curb", "conc": "gs_concrete"},
       display="Pump Island", unbreakable=True)

# ------------------------------------------------------------------ the price sign


def brand():
    img = np.ones((S, 2 * S, 3)) * T.hexc("#ece6d4")
    img = T.shade(img, 0.94 + T.noise("gsbr", shape=(S, 2 * S), sigma=3) * 0.08)
    img[:4] = T.hexc("#b0241c")
    img[-4:] = T.hexc("#b0241c")
    img = centered(img, "GAS", 9, "#b0241c", scale=3, gap=2)
    return T.border(img, "#5a5a58", 1)


PRICES = [("REG", "3.29", "SUP", "3.49"), ("REG", "3.29", "SUP", "3.4"), ("REG", "0.00", "SUP", "0.00"),
          (None, "WELCOME", None, "BACK"), (None, "AGAIN", None, "AGAIN"), (None, "STAY", None, "")]


def price(v):
    def f():
        img = np.ones((S, 2 * S, 3)) * T.hexc("#101210")
        a1, b1, a2, b2 = PRICES[v]
        amber = "#ffb040"
        if a1:
            img = draw_text(img, a1, 5, 7, "#d8d0b8")
            img = draw_text(img, b1, 26, 3, amber, scale=2)
            img = draw_text(img, a2, 5, 21, "#d8d0b8")
            img = draw_text(img, b2, 26, 17, amber, scale=2)
        else:
            img = centered(img, b1, 6, amber, scale=2 if text_width(b1, 2) < 60 else 1)
            if b2:
                img = centered(img, b2, 19, amber, scale=2 if text_width(b2, 2) < 60 else 1)
        return T.border(img, "#5a5a58", 1)
    return f


sliced("gs_brand", brand, 2, 1)
for v in range(len(PRICES)):
    sliced(f"gs_price{v}", price(v), 2, 1)


def sign_block(name, left, right, light=10):
    # south face is what the road sees; the north face shows the other half so both sides read correctly
    shaped(name + "_l", [(0, 0, 5, 16, 16, 11, {"all": "frame", "south": "a", "north": "b"})],
           {"frame": "gs_pole", "a": left, "b": right}, display="Price Sign", light=light, emissive=True,
           unbreakable=True, sound="metal", render="solid")
    shaped(name + "_r", [(0, 0, 5, 16, 16, 11, {"all": "frame", "south": "a", "north": "b"})],
           {"frame": "gs_pole", "a": right, "b": left}, display="Price Sign", light=light, emissive=True,
           unbreakable=True, sound="metal", render="solid")


tx("gs_pole", lambda: paint_metal("gspole", "#5a5c5e", "#707274", rust=0.3))
sign_block("gs_sign_brand", "gs_brand_0_0", "gs_brand_1_0")
for v in range(len(PRICES)):
    sign_block(f"gs_sign_price{v}", f"gs_price{v}_0_0", f"gs_price{v}_1_0", light=7)
shaped("gs_sign_post", [(6, 0, 6, 10, 16, 10, "p")], {"p": "gs_pole"}, display="Sign Post", unbreakable=True,
       sound="metal")

# ------------------------------------------------------------------ roadside


def wood_pole():
    img = T.ramp(T.fbm("gswp", octaves=3, base=2), "#3a3028", "#4e4236")
    xx = np.mgrid[0:S, 0:S][1]
    img = T.shade(img, 1 - (np.sin(xx * 1.9 + T.noise("gswpn", sigma=2) * 5) > 0.75) * 0.18)
    return T.grain(img, "gswp", 0.05)


def wire_tex():
    rgb = np.ones((S, S, 3)) * T.hexc("#101010")
    return rgb, np.ones((S, S))


tx("gs_wood_pole", wood_pole)
tx("gs_wire", wire_tex)
tx("gs_insulator", lambda: T.ramp(T.noise("gsins", sigma=1), "#4a6a5a", "#6a8a7a"))
shaped("gs_pole", [(6, 0, 6, 10, 16, 10, "p")], {"p": "gs_wood_pole"}, display="Telephone Pole", unbreakable=True)
shaped("gs_pole_top", [(6, 0, 6, 10, 14, 10, "p"), (0, 10, 7, 16, 11.5, 9, "p"),
                       (0.5, 11.5, 7.5, 1.5, 13, 8.5, "i"), (14.5, 11.5, 7.5, 15.5, 13, 8.5, "i")],
       {"p": "gs_wood_pole", "i": "gs_insulator"}, display="Telephone Pole", unbreakable=True)
shaped("gs_wire", [(0.8, 12.6, 0, 1.2, 13, 16, "w"), (14.8, 12.6, 0, 15.2, 13, 16, "w")], {"w": "gs_wire"},
       display="Overhead Wire", collides=False, unbreakable=True, sound="metal")


def billboard():
    w, h = 4 * S, 2 * S
    img = T.ramp(T.fbm("gsbb", shape=(h, w), octaves=3, base=6), "#c8b890", "#d8caa4")
    # peeling layers of older posters underneath
    peel = (T.noise("gsbbp", shape=(h, w), sigma=3) > 0.72).astype(float)
    img = T.blend(img, T.solid("#5a7a8a", shape=(h, w)), peel * 0.7)
    img = draw_text(img, "LAST GAS", (w - text_width("LAST GAS", 3, 2)) // 2, 10, "#8a1a14", scale=3, gap=2)
    img = draw_text(img, "NEXT EXIT: HERE", (w - text_width("NEXT EXIT: HERE", 1)) // 2, 40, "#2a2a2a")
    img = T.border(img, "#3a3a3a", 2)
    return T.grain(img, "gsbb", 0.04)


sliced("gs_billboard", billboard, 4, 2)
for r in range(2):
    for c in range(4):
        shaped(f"gs_billboard_{c}_{r}", [(0, 0, 7, 16, 16, 9, {"all": "back", "north": "face"})],
               {"back": "gs_pole", "face": f"gs_billboard_{c}_{r}"}, display="Billboard", unbreakable=True,
               render="solid")


def cart_metal():
    rgb = np.ones((S, S, 3)) * T.hexc("#8a8c8e")
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx % 4) == 0) | ((yy % 4) == 0)).astype(float)
    return rgb, a


tx("gs_cart", cart_metal)
shaped("gs_cart", [(2, 4, 1, 14, 13, 15, "c"), (3, 0, 2, 4, 4, 3, "p"), (12, 0, 2, 13, 4, 3, "p"),
                   (3, 0, 13, 4, 4, 14, "p"), (12, 0, 13, 13, 4, 14, "p"), (2, 13, 15, 14, 14, 16, "p")],
       {"c": "gs_cart", "p": "gs_pole"}, display="Shopping Cart", unbreakable=True, sound="metal")


def warning_board():
    img = T.ramp(T.fbm("gswb", octaves=3, base=3), "#8a7a5a", "#9a8a6a")
    img = T.shade(img, 1 - ((np.mgrid[0:S, 0:S][0] % 8) == 0) * 0.2)
    img = centered(img, "YOU", 6, "#1a1210")
    img = centered(img, "WERE", 13, "#1a1210")
    img = centered(img, "HERE", 20, "#1a1210")
    return T.grain(img, "gswb", 0.05)


tx("gs_warning", warning_board)
shaped("gs_warning_sign", [(7, 0, 7, 9, 10, 9, "p"), (1, 8, 6.5, 15, 16, 7.5, {"all": "p", "north": "b", "south": "p"})],
       {"p": "gs_wood_pole", "b": "gs_warning"}, display="Hand-Painted Sign", unbreakable=True)

# ------------------------------------------------------------------ restroom, ice, dumpster, phone


def restroom_door(upper):
    def f():
        img = T.ramp(T.noise("gsrd" + str(upper), sigma=2), "#5a6a72", "#66767e")
        img = T.border(img, "#3a464c", 2)
        if upper:
            img = T.rect(img, 7, 10, 25, 20, "#e8e4d8")
            img = centered(img, "RESTROOM", 13, "#1a3a6a", scale=1, gap=0)
        else:
            img = T.rect(img, 23, 2, 27, 6, "#a8a8a0")
            img = T.stains(img, "gsrds", "#2a2a24", count=1, strength=0.4, size=6)
        return T.grain(img, "gsrd" + str(upper), 0.04)
    return f


def restroom_dark():
    img = T.solid("#050506")
    yy, xx = np.mgrid[0:S, 0:S]
    # a hint of tile in the dark and something at the far end
    img = T.blend(img, T.solid("#141818"), (((xx % 8) == 0) | ((yy % 8) == 0)) * 0.5)
    return img


def ice():
    img = T.ramp(T.noise("gsice", sigma=2), "#d8dee4", "#e8eef2")
    img = T.rect(img, 3, 4, 29, 16, "#2a5aa8")
    img = centered(img, "ICE", 6, "#ffffff", scale=2)
    img = T.stains(img, "gsices", "#6a7a8a", count=1, strength=0.3, size=6)
    return T.grain(img, "gsice", 0.03)


def dumpster():
    img = paint_metal("gsdump", "#2a4a32", "#365a3e", rust=0.35)
    return T.shade(img, 1 - ((np.mgrid[0:S, 0:S][1] % 8) == 0) * 0.2)


def phone_body():
    img = paint_metal("gsph", "#7a7c7e", "#9a9c9e", rust=0.1)
    img = T.rect(img, 8, 12, 24, 28, "#2a2a2c")
    for k in range(12):
        img = T.rect(img, 10 + (k % 3) * 5, 14 + (k // 3) * 3, 13 + (k % 3) * 5, 16 + (k // 3) * 3, "#c8c8c0")
    img = T.rect(img, 6, 3, 26, 9, "#1a3a8a")
    img = centered(img, "PHONE", 4, "#ffffff")
    return img


tx("gs_restroom_upper", restroom_door(True))
tx("gs_restroom_lower", restroom_door(False))
tx("gs_restroom_dark", restroom_dark)
tx("gs_ice", ice)
tx("gs_dumpster", dumpster)
tx("gs_phone", phone_body)
tx("gs_handset", lambda: T.grain(T.solid("#18181a"), "gshand", 0.05))

shaped("gs_restroom_lower", [(0, 0, 0, 16, 16, 16, {"all": "wall", "north": "door"})],
       {"wall": "gs_wall", "door": "gs_restroom_lower"}, display="Restroom Door", unbreakable=True, sound="metal")
shaped("gs_restroom_upper", [(0, 0, 0, 16, 16, 16, {"all": "wall", "north": "door"})],
       {"wall": "gs_wall", "door": "gs_restroom_upper"}, display="Restroom Door", unbreakable=True, sound="metal")
exit_block("gs_restroom_open", "gs_restroom_dark", "DOOR", display="Open Restroom Door")
shaped("gs_ice", [(1, 0, 3, 15, 16, 15, {"all": "s", "north": "f"})], {"s": "gs_canopy_under", "f": "gs_ice"},
       display="Ice Machine", unbreakable=True, sound="metal")
shaped("gs_dumpster", [(0, 0, 2, 16, 14, 14, "d"), (0, 14, 1, 16, 15, 15, "d")], {"d": "gs_dumpster"},
       display="Dumpster", unbreakable=True, sound="metal")

PHONE_BOXES = [(5, 0, 12, 11, 8, 14, "pole"), (3, 8, 11, 13, 22, 16, {"all": "pole", "north": "body!"}),
               (2, 22, 9, 14, 24, 16, "pole"), (3.5, 14, 10, 5.5, 21, 11, "hand")]
PHONE_OFF = [(5, 0, 12, 11, 8, 14, "pole"), (3, 8, 11, 13, 22, 16, {"all": "pole", "north": "body!"}),
             (2, 22, 9, 14, 24, 16, "pole"), (2, 2, 9, 4, 8, 11, "hand")]
PTX = {"pole": "gs_pole", "body": "gs_phone", "hand": "gs_handset"}
BLOCK_MODELS["gs_payphone_hung"] = box_model(PHONE_BOXES, PTX)
BLOCK_MODELS["gs_payphone_ring"] = box_model(PHONE_BOXES, PTX)
custom("gs_payphone", "com.noclip.block.PayphoneBlock", box_model(PHONE_BOXES, PTX),
       blockstate=facing_states(lambda c: "gs_payphone_hung", {"ringing": ["true", "false"]}),
       display="Payphone", sound="metal", unbreakable=True, render="cutout")

# ------------------------------------------------------------------ the attendant


def attendant_tex():
    c = Canvas()
    skin_c = "#cdb8a0"
    sk = lambda w, h: T.ramp(T.noise(f"gsat{w}{h}", shape=(h, w), sigma=1.2), skin_c, "#d8c4ac")

    def face(w, h):
        img = sk(w, h)
        img[:2] = T.hexc("#7a1a14")
        return img

    def head(w, h):
        img = sk(w, h)
        img[:3] = T.hexc("#7a1a14")
        return img

    def shirt(w, h):
        img = T.grain(T.ramp(T.noise(f"gsats{w}{h}", shape=(h, w), sigma=1), "#a42a22", "#b4362a"), f"gsatg{w}", 0.05)
        if w == 8 and h == 12:
            img[2:4, 1:4] = T.hexc("#e8e4d8")
            img[3, 1:4] = T.hexc("#3a3a3a")
            img[6:, :] = T.hexc("#e8e4d8") * 0.95
            img[6, :] = T.hexc("#b0241c")
        return img

    def arm(w, h):
        img = shirt(w, h)
        img[5:] = T.hexc(skin_c)
        return img

    def legs(w, h):
        img = T.grain(T.solid("#2a2e3a", shape=(h, w)), f"gsatl{w}", 0.05)
        img[-2:] = T.hexc("#141414")
        return img

    paint_humanoid(c, head, shirt, arm, legs, face=face)
    c.box(0, 0, 8, 8, 8, up=lambda w, h: T.grain(T.solid("#8a1a14", shape=(h, w)), "gscap", 0.05))
    c.box(24, 0, 8, 1, 4, all=lambda w, h: T.solid("#8a1a14", shape=(h, w)))
    return c.out()


entity_model("gs_attendant", humanoid(head_extra=[(24, 0, -4, -8.5, -8, 8, 1, 4)]))
entity_tex("gs_attendant", attendant_tex)
lang("entity.noclip.gs_attendant", "The Attendant")
lang("item.noclip.gs_attendant_spawn_egg", "The Attendant Spawn Egg")
ITEM_MODELS["gs_attendant_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def night_loop():
    d = 24.0
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    r = Y.rng("gscrick")
    # a field of crickets, each with its own rhythm
    for k in range(9):
        f = 3800 + r.random() * 1400
        rate = 2.2 + r.random() * 2.5
        rate = round(rate * d) / d
        ph = r.random() * 6.28
        pulse = (np.sin(2 * np.pi * rate * t + ph) > 0.35).astype(float)
        trill = 0.5 + 0.5 * np.sin(2 * np.pi * 45 * t)
        g = 0.05 + r.random() * 0.06
        out += np.sin(2 * np.pi * (round(f * d) / d) * t) * pulse * trill * g
    buzz = Y.hum(d, 60.0, "gshum", buzz=0.5, harmonics=(2, 4, 6, 8)) * 0.05
    road = Y.lp(Y.brown(d, "gsroad"), 160) * 0.35
    x = Y.loopable(Y.norm(out + Y.hp(buzz, 200) + road, 0.45), 1.0)
    return Y.stereoize(x, 0.0011), "stereo"


def chime():
    out = np.zeros(int(3.0 * Y.SR))
    for at, f in ((0.0, 659.3), (0.45, 523.3)):
        n = int(1.6 * Y.SR)
        tt = np.arange(n) / Y.SR
        tone = (np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt * 6)) * np.exp(-tt * 2.2)
        Y.place(out, tone, at, 0.6)
    return Y.reverb(Y.norm(out, 0.6), 1.5, 0.35, "gschime")


def ice_drop():
    out = np.zeros(int(2.0 * Y.SR))
    r = Y.rng("gsice")
    Y.place(out, Y.lp(Y.white(0.6, "gsicehum"), 200) * 0.3, 0.0)
    for k in range(14):
        n = int(0.04 * Y.SR)
        tt = np.arange(n) / Y.SR
        click = np.sin(2 * np.pi * (2200 + r.random() * 2500) * tt) * np.exp(-tt * 120)
        Y.place(out, click, 0.4 + r.random() * 0.5, 0.3 + r.random() * 0.4)
    return Y.reverb(Y.norm(out, 0.5), 0.8, 0.3, "gsice")


def truck_horn():
    d = 4.0
    t = Y.t_axis(d)
    env = np.clip(t / 0.2, 0, 1) * np.clip((2.6 - t) / 0.4, 0, 1)
    dop = 1 - 0.04 * np.tanh((t - 1.3) * 3)
    x = np.zeros_like(t)
    for f in (155.6, 196.0, 233.1):
        x += np.sign(np.sin(2 * np.pi * np.cumsum(f * dop) / Y.SR)) * 0.3
    x = Y.lp(x * env, 900)
    return Y.reverb(Y.norm(x, 0.35), 3.5, 0.75, "gshorn")


def pump_click():
    out = np.zeros(int(1.6 * Y.SR))
    Y.place(out, Y.impact("gspump", 0.2), 0.0, 0.5)
    n = int(0.25 * Y.SR)
    tt = np.arange(n) / Y.SR
    Y.place(out, np.sin(2 * np.pi * 1800 * tt) * (tt < 0.12) * 0.4, 0.5)
    Y.place(out, np.sin(2 * np.pi * 1800 * tt) * (tt < 0.12) * 0.4, 0.75)
    return Y.reverb(Y.norm(out, 0.5), 1.2, 0.3, "gspump")


def ring():
    d = 2.4
    t = Y.t_axis(d)
    bell = (np.sin(2 * np.pi * 440 * t) + np.sin(2 * np.pi * 480 * t)) * (np.sin(2 * np.pi * 20 * t) > 0)
    x = bell * (t < 1.6) * Y.env(len(t), 0.01, 0.05)
    return Y.norm(Y.bp(x, 300, 3000), 0.7)


def dial():
    d = 1.5
    t = Y.t_axis(d)
    x = (np.sin(2 * np.pi * 350 * t) + np.sin(2 * np.pi * 440 * t)) * Y.env(len(t), 0.01, 0.1)
    return Y.norm(Y.bp(x, 300, 3400), 0.4)


def breath_line():
    """What you hear when you answer: your own breathing, a moment late."""
    d = 3.0
    t = Y.t_axis(d)
    ph = (t % 1.5) / 1.5
    br = Y.bp(Y.white(d, "gsbr"), 400, 2400) * np.clip(np.sin(np.pi * ph / 0.5), 0, None) * (ph < 0.5)
    br += Y.bp(Y.white(d, "gsbr2"), 300, 1500) * np.clip(np.sin(np.pi * (ph - 0.55) / 0.4), 0, None) * (ph > 0.55) * (ph < 0.95)
    line = Y.bp(Y.white(d, "gsline"), 2000, 3400) * 0.04
    return Y.norm(Y.bp(br + line, 300, 3400), 0.6)


sound("ambient.gas_station", [("ambient/gas_station", night_loop)], stream=True)
sound("ambient.gas_station.additions",
      [("ambient/gas_station_ice", ice_drop), ("ambient/gas_station_horn", truck_horn),
       ("ambient/gas_station_pump", pump_click)],
      subtitle="Something at the station")
sound("event.gas_station_chime", [("event/gas_station_chime", chime)], subtitle="The shop door chimes")
sound("event.gas_station_ring", [("event/gas_station_ring", ring)], subtitle="A payphone rings")
sound("event.gas_station_dial", [("event/gas_station_dial", dial)], subtitle="Dial tone")
sound("event.gas_station_breath", [("event/gas_station_breath", breath_line)], subtitle="Breathing on the line")

level("gas_station", fog="#0b0d14", sky="#06070d", ambient="ambient.gas_station",
      additions="ambient.gas_station.additions", additions_chance=0.003, fixed_time=18000, skylight=True,
      ceiling=False, height=80)
