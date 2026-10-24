"""Level 4 - Abandoned Office. Endless cubicles under humming lights. The water coolers still hold
Almond Water. Some of the dead computers are not dead: follow what they tell you."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, door, cross, exit_block, flicker_light, level, sound, \
    BLOCK_MODELS
import tex as T
import synth as Y
from content.classics_kit import draw_text, centered, metal

S = 32
yy, xx = np.mgrid[0:S, 0:S]


# ------------------------------------------------------------------ textures

def carpet():
    base = T.ramp(T.fbm("l4c", octaves=3, base=4) * 0.6 + T.noise("l4cf", sigma=0.4) * 0.4, "#3e4652", "#4a5361", "#525c6a")
    # carpet tiles: faint seams every half block, alternating pile direction
    tile = ((xx // 16) + (yy // 16)) % 2
    base = T.shade(base, 1 + (tile - 0.5)[..., None][..., 0] * 0.05)
    seam = ((xx % 16) == 0) | ((yy % 16) == 0)
    base = T.shade(base, 1 - seam * 0.12)
    fleck = (T.rng("l4fl").random((S, S)) > 0.96)
    base = T.blend(base, T.solid("#7a8494"), fleck * 0.5)
    return T.grain(base, "l4c", 0.04)


def carpet_stain():
    img = carpet()
    return T.stains(img, "l4cst", "#2a2a24", count=2, strength=0.5, size=8)


def ceiling():
    base = T.ramp(T.noise("l4ct", sigma=0.5), "#d8d8d2", "#e6e6e0")
    holes = (T.rng("l4ch").random((S, S)) > 0.9).astype(float)
    base = T.shade(base, 1 - holes * 0.08)
    base = T.border(base, "#b8bab6", 1)
    return T.grain(base, "l4ct", 0.02)


def ceiling_stain():
    return T.stains(ceiling(), "l4cs", "#a08a5a", 1, 0.5, 10)


def troffer():
    img = T.solid("#f4f8ff")
    prism = (((xx + yy) % 3) == 0) | (((xx - yy) % 3) == 0)
    img = T.shade(img, 1 - prism * 0.05)
    img = T.border(img, "#c8cac8", 2)
    img = T.rect(img, 0, 15, 32, 17, "#d0d2d0")
    return img


def troffer_off():
    img = T.solid("#9a9c9e")
    prism = (((xx + yy) % 3) == 0) | (((xx - yy) % 3) == 0)
    img = T.shade(img, 1 - prism * 0.08)
    img = T.border(img, "#c8cac8", 2)
    return img


def drywall():
    img = T.ramp(T.fbm("l4dw", octaves=3, base=6), "#c9c4b6", "#d6d1c4")
    img = T.stains(img, "l4dws", "#a8a090", 1, 0.15, 9)
    return T.grain(img, "l4dw", 0.025)


def drywall_base():
    img = drywall()
    img = T.rect(img, 0, 27, 32, 32, "#3a3a3c")
    img = T.rect(img, 0, 27, 32, 28, "#5a5a5c")
    scuffs = (T.noise("l4scf", sigma=0.6) > 0.82) & (yy > 20) & (yy < 27)
    return T.blend(img, T.solid("#6a6458"), scuffs * 0.4)


def drywall_clock():
    img = drywall()
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 13.5) ** 2)
    img = T.blend(img, T.solid("#f2f0ea"), (d < 9).astype(float))
    img = T.blend(img, T.solid("#2a2a2a"), (np.abs(d - 9) < 1).astype(float))
    for k in range(12):
        a = k * np.pi / 6
        img[int(round(13.5 + np.sin(a) * 7.3)), int(round(15.5 + np.cos(a) * 7.3))] = T.hexc("#2a2a2a")
    # 4:59, forever
    for t in range(1, 7):
        img[int(round(13.5 + t * np.sin(np.radians(120 - 90)))), int(round(15.5 + t * np.cos(np.radians(120 - 90))))] = T.hexc("#101010")
    for t in range(1, 8):
        img[int(round(13.5 - t)), 15] = T.hexc("#101010")
    img[13:15, 15:17] = T.hexc("#a01810")
    return img


def pillar():
    img = drywall()
    img = T.rect(img, 0, 0, 3, 32, "#9a9c9e")
    img = T.rect(img, 29, 0, 32, 32, "#9a9c9e")
    return img


def partition():
    fab = T.ramp(T.noise("l4pf", sigma=0.35), "#5a6270", "#677080")
    weave = ((xx + yy) % 2 == 0)
    fab = T.shade(fab, 1 - weave * 0.04)
    fab = T.rect(fab, 0, 0, 32, 3, "#a8acb0")
    fab = T.rect(fab, 0, 29, 32, 32, "#3a3e44")
    pins = [(6, 10), (22, 14), (12, 20)]
    r = T.rng("l4memo")
    for (px, py) in pins:
        if r.random() < 0.7:
            fab = T.rect(fab, px, py, px + 5, py + 5, ("#f0e68c", "#f4f2ec", "#ffc8c8")[int(r.random() * 3)])
            fab[py + 1:py + 4:2, px + 1:px + 4] *= 0.75
    return T.grain(fab, "l4pf", 0.03)


def partition_edge():
    return T.solid("#a8acb0")


def desk_top():
    img = T.ramp(T.fbm("l4wood", octaves=2, base=3) * 0.5 + T.noise("l4wg", sigma=0.4, shape=(S, S)) * 0.2, "#9a8a6e", "#ae9c7c")
    grain_ = np.sin(yy * 0.9 + T.noise("l4wg2", sigma=3) * 6) * 0.03
    img = T.shade(img, 1 + grain_)
    ring = T.stains(img, "l4ring", "#6a5a44", 1, 0.25, 3)
    return T.grain(ring, "l4dt", 0.02)


def desk_side():
    return metal("l4ds", "#6a6c70", "#7a7c80", sigma=2)


def crt_case():
    img = T.ramp(T.noise("l4crt", sigma=2), "#c8c0aa", "#d6ceb8")
    return T.grain(T.stains(img, "l4crty", "#a8987a", 1, 0.3, 8), "l4crt", 0.02)


def screen(lines=(), fg="#a8d8ff", bg="#0a1420", scan=True, big=None, glow=True):
    def f():
        img = T.solid(bg)
        if glow:
            d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
            img = T.shade(img, 1.3 - d / 40)
        y = 5 if big is None else 3
        for ln in lines:
            img = centered(img, ln, y, fg)
            y += 7
        if big is not None:
            img = centered(img, big, y + 2, fg, scale=3)
        if scan:
            img = T.shade(img, 1 - ((yy % 2) == 0) * 0.18)
        img = T.border(img, "#2a2826", 2)
        return T.border(img, "#141210", 1)
    return f


def screen_dead():
    img = T.ramp(T.noise("l4sd", sigma=3), "#121414", "#1e2222")
    refl = np.clip(1 - np.abs((xx - yy) - 8) / 4.0, 0, 1) * (xx < 20)
    img = T.blend(img, T.solid("#5a6064"), refl * 0.35)
    img = T.border(img, "#2a2826", 2)
    return T.border(img, "#141210", 1)


def keyboard():
    img = T.solid("#c8c0aa")
    keys = ((xx % 3) != 0) & ((yy % 4) != 0)
    return T.blend(img, T.solid("#e2dccc"), keys * 0.8)


def cooler_body():
    img = T.ramp(T.noise("l4cb", sigma=2), "#dcdcd6", "#ecece6")
    img = T.rect(img, 10, 6, 22, 12, "#3a6aa8")
    img = T.rect(img, 12, 8, 14, 10, "#c02020")
    img = T.rect(img, 18, 8, 20, 10, "#2060c0")
    return T.grain(img, "l4cb", 0.02)


def cooler_jug():
    rgb = T.ramp(T.noise("l4jug", sigma=2), "#9ac0d8", "#c8e0f0")
    rgb = T.shade(rgb, 1 - ((yy % 8) == 0) * 0.15)
    # the water inside is faintly the wrong colour
    rgb = T.blend(rgb, T.solid("#e8dcc0"), np.clip((yy - 6) / 26.0, 0, 1) * 0.35)
    return rgb, np.ones((S, S)) * 0.8


def vend_low():
    img = T.solid("#1a1a1e")
    img = T.rect(img, 2, 2, 22, 22, "#e8e8f0")
    for row in range(3):
        for col in range(4):
            c = ("#c02020", "#e0a020", "#2a6ac0", "#30a040", "#e06a20")[(row * 4 + col) % 5]
            img = T.rect(img, 3 + col * 5, 3 + row * 6, 7 + col * 5, 7 + row * 6, c)
            img = T.rect(img, 3 + col * 5, 7 + row * 6, 7 + col * 5, 8 + row * 6, "#6a6a70")
    img = T.rect(img, 24, 4, 30, 18, "#3a3a40")
    img = T.rect(img, 25, 5, 29, 8, "#30ff60")
    img = T.rect(img, 3, 24, 21, 30, "#0a0a0c")
    return img


def vend_up():
    img = T.solid("#1a1a1e")
    img = T.rect(img, 2, 0, 22, 32, "#e8e8f0")
    for row in range(5):
        for col in range(4):
            c = ("#30a040", "#c02020", "#e0a020", "#2a6ac0", "#8a3ac0")[(row + col * 2) % 5]
            img = T.rect(img, 3 + col * 5, 2 + row * 6, 7 + col * 5, 6 + row * 6, c)
            img = T.rect(img, 3 + col * 5, 6 + row * 6, 7 + col * 5, 7 + row * 6, "#6a6a70")
    img = T.rect(img, 24, 2, 30, 30, "#c02028")
    img = draw_text(img, "C", 25, 4, "#ffffff")
    img = draw_text(img, "O", 25, 10, "#ffffff")
    img = draw_text(img, "L", 25, 16, "#ffffff")
    img = draw_text(img, "A", 25, 22, "#ffffff")
    return img


def vend_side():
    return metal("l4vs", "#a01820", "#b82028", sigma=2)


def filing():
    img = metal("l4fc", "#b8b2a0", "#c8c2b0", sigma=2)
    for y0 in (1, 11, 21):
        img = T.rect(img, 2, y0, 30, y0 + 9, "#c4bea8")
        img = T.rect(img, 2, y0 + 9, 30, y0 + 10, "#8a8676")
        img = T.rect(img, 12, y0 + 3, 20, y0 + 5, "#7a7666")
        img = T.rect(img, 14, y0 + 6, 18, y0 + 8, "#f2f0e6")
    return img


def copier_side():
    img = metal("l4cp", "#c8c8c4", "#d8d8d4", sigma=2)
    img = T.rect(img, 3, 6, 29, 9, "#5a5a5c")
    img = T.rect(img, 3, 16, 29, 18, "#9a9a9c")
    img = T.rect(img, 22, 2, 28, 5, "#30d050")
    return img


def copier_top():
    img = metal("l4cpt", "#c8c8c4", "#d8d8d4", sigma=2)
    img = T.rect(img, 4, 4, 28, 26, "#2a3438")
    img = T.rect(img, 6, 6, 18, 22, "#f6f6f2")
    return img


def whiteboard(lines, color="#2040a0"):
    def f():
        img = T.solid("#f2f4f6")
        img = T.blend(img, T.solid("#c8ccd0"), T.noise("l4wb" + lines[0], sigma=3) * 0.25)
        y = 4
        for ln in lines:
            img = centered(img, ln, y, color, jitter=0.25)
            y += 7
        return T.border(img, "#9a9ea2", 2)
    return f


def table_top():
    return T.grain(T.ramp(T.fbm("l4tt", octaves=2, base=4), "#5a4636", "#6e5642"), "l4tt", 0.03)


def window():
    img = T.solid("#05070a")
    r = T.rng("l4win")
    for _ in range(6):
        x, y = int(r.random() * 28) + 2, int(r.random() * 10) + 18
        img[y, x] = T.hexc("#3a3428")
    refl = np.clip(1 - np.abs((xx * 0.6 - yy) - 4) / 3.0, 0, 1)
    img = T.blend(img, T.solid("#2a3036"), refl * 0.4)
    img = T.rect(img, 0, 15, 32, 17, "#7a7c7e")
    img = T.rect(img, 15, 0, 17, 32, "#7a7c7e")
    return T.border(img, "#8a8c8e", 2)


def window_city():
    img = T.solid("#0a0c18")
    sky = T.vgrad(S, S, "#1a1830", "#3a2a3a")
    img = sky
    r = T.rng("l4city")
    for b in range(7):
        x0 = int(r.random() * 28)
        w = 3 + int(r.random() * 5)
        h = 8 + int(r.random() * 18)
        img = T.rect(img, x0, S - h, min(S, x0 + w), S, "#0e0e14")
        for wy in range(S - h + 2, S - 1, 3):
            for wx in range(x0 + 1, min(S - 1, x0 + w - 1), 2):
                if r.random() < 0.45:
                    img[wy, wx] = T.hexc("#ffd890" if r.random() < 0.8 else "#c8e8ff")
    img = T.rect(img, 0, 15, 32, 17, "#7a7c7e")
    img = T.rect(img, 15, 0, 17, 32, "#7a7c7e")
    return T.border(img, "#8a8c8e", 2)


def glass():
    rgb = T.solid("#b8c8d0")
    a = np.ones((S, S)) * 0.22
    a[(xx < 2) | (xx > 29)] = 1
    rgb[(xx < 2) | (xx > 29)] = T.hexc("#8a8c8e")
    band = (yy > 12) & (yy < 16)
    a[band] = 0.55
    rgb[band] = T.hexc("#e8eef2")
    return rgb, a


def office_door(top):
    def f():
        img = T.ramp(T.fbm("l4door", octaves=2, base=3), "#8a6a48", "#9a7a56")
        img = T.shade(img, 1 + np.sin(xx * 0.7 + T.noise("l4dg", sigma=3) * 5) * 0.03)
        img = T.border(img, "#5a4a36", 1)
        if top:
            img = T.rect(img, 8, 6, 24, 26, "#b8c8d0")
            img = T.rect(img, 9, 7, 23, 25, "#d8e4ea")
        else:
            img = T.rect(img, 24, 2, 28, 5, "#c8c4b8")
        return img
    return f


def stair_door(top):
    def f():
        img = metal("l4sdr" + str(top), "#7a2a22", "#8a3428", sigma=2)
        img = T.border(img, "#3a1a16", 1)
        if top:
            img = T.rect(img, 4, 4, 28, 14, "#f2f0e6")
            img = centered(img, "STAIRS", 6, "#141414")
            img = T.rect(img, 10, 18, 22, 30, "#b8c8d0")
            img = T.rect(img, 11, 19, 21, 29, "#1a1e22")
        else:
            img = T.rect(img, 3, 6, 29, 9, "#c8c4b8")
            img = T.rect(img, 3, 6, 6, 12, "#c8c4b8")
        return img
    return f


def plant():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    rgb[22:, 10:22] = T.hexc("#6a4a32")
    a[22:, 10:22] = 1
    rgb[22, 9:23] = T.hexc("#7a5a40")
    a[22, 9:23] = 1
    r = T.rng("l4plant")
    for k in range(9):
        x, y = 16.0, 22.0
        ang = -np.pi / 2 + (r.random() - 0.5) * 1.6
        for t in range(14 + int(r.random() * 6)):
            x += np.cos(ang) * 0.8
            y += np.sin(ang) * 0.8
            ang += 0.07 * (1 if k % 2 else -1)
            xi, yi = int(x) % S, int(np.clip(y, 0, S - 1))
            col = "#7a6a3a" if t > 8 else "#5a5030"
            rgb[yi, xi] = T.hexc(col)
            a[yi, xi] = 1
            if t > 6 and r.random() < 0.4:
                rgb[yi, (xi + 1) % S] = T.hexc("#8a7840")
                a[yi, (xi + 1) % S] = 1
    return rgb, a


def counter_tex():
    img = T.ramp(T.noise("l4ctr", sigma=1.5), "#d2ccbc", "#e0dacc")
    return T.grain(T.rect(img, 0, 0, 32, 3, "#8a8478"), "l4ctr", 0.02)


def coffee():
    img = T.solid("#1a1a1c")
    img = T.rect(img, 6, 14, 26, 28, "#3a2414")
    img = T.rect(img, 8, 16, 24, 22, "#5a3a20")
    img = T.rect(img, 22, 4, 26, 7, "#ff3020")
    return img


tex("l4_carpet", carpet)
tex("l4_carpet_stain", carpet_stain)
tex("l4_ceiling", ceiling)
tex("l4_ceiling_stain", ceiling_stain)
tex("l4_troffer", troffer)
tex("l4_troffer_off", troffer_off)
tex("l4_drywall", drywall)
tex("l4_drywall_base", drywall_base)
tex("l4_drywall_clock", drywall_clock)
tex("l4_pillar", pillar)
tex("l4_partition", partition)
tex("l4_partition_edge", partition_edge)
tex("l4_desk_top", desk_top)
tex("l4_desk_side", desk_side)
tex("l4_crt_case", crt_case)
tex("l4_keyboard", keyboard)
tex("l4_screen_dead", screen_dead)
SCREENS = {
    "dos": screen(["C:/>_"], fg="#c8c8c8", bg="#000000", glow=False),
    "nosignal": screen(["NO", "SIGNAL"], fg="#ffffff", bg="#1030a0"),
    "newmsg": screen(["NEW MSG", "FROM:", "YOU"], fg="#a8d8ff"),
    "login": screen(["LOGIN:", "", "****"], fg="#60ff90", bg="#041008"),
    "stay": screen(["STAY", "AT YOUR", "DESK"], fg="#ffb040", bg="#140a02"),
    "time": screen(["4:59", "PM"], fg="#ff5040", bg="#100404"),
    "hello": screen(["HELLO?"], fg="#c8c8c8", bg="#000000", glow=False),
    "exit_up": screen(["EXIT"], fg="#60ff90", bg="#041008", big="^"),
    "exit_left": screen(["EXIT"], fg="#60ff90", bg="#041008", big="<"),
    "exit_right": screen(["EXIT"], fg="#60ff90", bg="#041008", big=">"),
    "exit_back": screen(["TURN", "AROUND"], fg="#60ff90", bg="#041008"),
}
for k, fn in SCREENS.items():
    tex(f"l4_screen_{k}", fn)
tex("l4_cooler_body", cooler_body)
tex("l4_cooler_jug", cooler_jug)
tex("l4_vend_low", vend_low)
tex("l4_vend_up", vend_up)
tex("l4_vend_side", vend_side)
tex("l4_filing", filing)
tex("l4_copier_side", copier_side)
tex("l4_copier_top", copier_top)
tex("l4_whiteboard_a", whiteboard(["WHERE", "DID", "THEY GO"]))
tex("l4_whiteboard_b", whiteboard(["Q4:", "1 FIND", "  EXIT", "2 ???"], "#a02020"))
tex("l4_whiteboard_c", whiteboard(["NEVER", "GO", "DOWN", "AGAIN"], "#202020"))
tex("l4_table", table_top)
tex("l4_window", window)
tex("l4_window_city", window_city)
tex("l4_glass", glass)
tex("l4_door_top", office_door(True))
tex("l4_door_bottom", office_door(False))
tex("l4_stair_door_top", stair_door(True))
tex("l4_stair_door_bottom", stair_door(False))
tex("l4_plant", plant)
tex("l4_counter", counter_tex)
tex("l4_coffee", coffee)

# ------------------------------------------------------------------ blocks

cube("office_carpet", "l4_carpet", display="Office Carpet", sound="wool")
cube("office_carpet_stained", "l4_carpet_stain", display="Stained Office Carpet", sound="wool")
cube("office_ceiling_tile", "l4_ceiling", display="Office Ceiling Tile", flat=True, sound="wool")
cube("office_ceiling_tile_stained", "l4_ceiling_stain", display="Stained Office Ceiling Tile", flat=True, sound="wool")
flicker_light("office_troffer", "l4_troffer", "l4_troffer_off", "l4_ceiling", light=15, flicker=0.02,
              display="Office Light")
flicker_light("office_troffer_flicker", "l4_troffer", "l4_troffer_off", "l4_ceiling", light=15, flicker=0.5,
              display="Flickering Office Light")
cube("office_drywall", "l4_drywall", display="Drywall")
cube("office_drywall_base", "l4_drywall_base", display="Drywall Baseboard")
cube("office_drywall_clock", "l4_drywall_clock", display="Stopped Clock")
cube("office_pillar", "l4_pillar", display="Office Pillar")
cube("office_window", "l4_window", display="Office Window", sound="glass")
pane("cubicle_partition", "l4_partition", edge="l4_partition_edge", display="Cubicle Partition", sound="wool")
pane("meeting_glass", "l4_glass", edge="l4_partition_edge", display="Meeting Room Glass", sound="glass",
     render="translucent")
door("office_door", "l4_door_top", "l4_door_bottom", display="Office Door", unbreakable=True)

shaped("office_desk", [
    (0, 14, 0, 16, 16, 16, {"all": "side", "up": "top"}),
    (0, 0, 0, 1, 14, 16, "side"), (15, 0, 0, 16, 14, 16, "side"),
    (1, 4, 0, 15, 14, 1, "side"),
], {"top": "l4_desk_top", "side": "l4_desk_side"}, display="Office Desk", sound="wood", unbreakable=True)

CRT = [
    (2, 0, 5, 14, 11, 14, {"all": "case", "south": "screen"}),
    (4, 1, 1, 12, 9, 5, "case"),
    (3, 0, 14.5, 13, 0.6, 16, {"all": "case", "up": "kb"}),
]


def crt_block(name, screen_tex, display, light=None):
    shaped(name, CRT, {"case": "l4_crt_case", "screen": screen_tex, "kb": "l4_keyboard"}, display=display,
           sound="glass", unbreakable=True, light=light, emissive=True if light else None, collides=False)
    BLOCK_MODELS[name]["elements"][0]["faces"]["south"]["uv"] = [0, 0, 16, 16]


crt_block("office_computer", "l4_screen_dead", "Dead Computer")
for k in SCREENS:
    crt_block(f"office_computer_{k}", f"l4_screen_{k}", "Computer", light=5)

shaped("water_cooler", [
    (4, 0, 4, 12, 11, 12, "body"),
    (5, 11, 5, 11, 16, 11, "jug"),
], {"body": "l4_cooler_body", "jug": "l4_cooler_jug"}, display="Water Cooler", kind="pickup",
    item="noclip:almond_water", sound="glass", render="translucent", strength=0.5)
shaped("vending_machine", [(1, 0, 1, 15, 16, 15, {"all": "side", "south": "front"})],
       {"side": "l4_vend_side", "front": "l4_vend_low"}, display="Vending Machine", sound="metal", unbreakable=True,
       light=7, emissive=True)
shaped("vending_machine_top", [(1, 0, 1, 15, 16, 15, {"all": "side", "south": "front"})],
       {"side": "l4_vend_side", "front": "l4_vend_up"}, display="Vending Machine", sound="metal", unbreakable=True,
       light=7, emissive=True)
shaped("filing_cabinet", [(1, 0, 3, 15, 16, 15, {"all": "side", "south": "front"})],
       {"side": "l4_desk_side", "front": "l4_filing"}, display="Filing Cabinet", sound="metal", unbreakable=True)
shaped("office_copier", [(1, 0, 2, 15, 12, 14, {"all": "side", "up": "top"}), (2, 12, 3, 14, 13, 13, "side")],
       {"side": "l4_copier_side", "top": "l4_copier_top"}, display="Copier", sound="metal", unbreakable=True)
for k in "abc":
    shaped(f"office_whiteboard_{k}", [(0, 3, 7, 16, 16, 9, {"all": "edge", "south": "board", "north": "board"}),
                                      (1, 0, 7.5, 2, 3, 8.5, "edge"), (14, 0, 7.5, 15, 3, 8.5, "edge")],
           {"board": f"l4_whiteboard_{k}", "edge": "l4_desk_side"}, display="Whiteboard", sound="wood",
           unbreakable=True)
shaped("conference_table", [(0, 12, 0, 16, 14, 16, "top"), (6, 0, 6, 10, 12, 10, "leg")],
       {"top": "l4_table", "leg": "l4_desk_side"}, display="Conference Table", sound="wood", unbreakable=True)
shaped("break_table", [(1, 12, 1, 15, 13.5, 15, "top"), (7, 0, 7, 9, 12, 9, "leg"), (4, 0, 4, 12, 1, 12, "leg")],
       {"top": "l4_desk_top", "leg": "l4_desk_side"}, display="Break Room Table", sound="wood", unbreakable=True)
shaped("break_counter", [(0, 0, 1, 16, 15, 16, {"all": "side", "up": "top"}), (0, 15, 0, 16, 16, 16, "top")],
       {"side": "l4_desk_side", "top": "l4_counter"}, display="Counter", sound="wood", unbreakable=True)
shaped("coffee_maker", [(4, 0, 6, 12, 10, 14, {"all": "case", "south": "front"})],
       {"case": "l4_crt_case", "front": "l4_coffee"}, display="Coffee Maker", sound="metal", unbreakable=True,
       collides=False, light=2)
cross("office_plant", "l4_plant", display="Dead Office Plant")

exit_block("office_window_city", "l4_window_city", "SECRET", trigger="USE", solid=True, display="A Window With A View",
           sound="glass", light=4)
exit_block("stairwell_door", "l4_stair_door_bottom", "DESCEND", trigger="USE", solid=True, display="Stairwell Door",
           sound="metal")
exit_block("stairwell_door_top", "l4_stair_door_top", "DESCEND", trigger="USE", solid=True, display="Stairwell Door",
           sound="metal")

# ------------------------------------------------------------------ sound


def office_loop():
    d = 28.0
    t = Y.t_axis(d)
    hvac = Y.lp(Y.pink(d, "l4hvac"), 700) * 0.5 * (0.85 + 0.15 * np.sin(2 * np.pi * t * 2 / d))
    hum = Y.hum(d, 60.0, "l4hum", buzz=0.12, harmonics=(2, 4, 6)) * 0.1
    room = Y.lp(Y.brown(d, "l4room"), 160) * 0.3
    x = hvac + Y.lp(hum, 1800) + room
    # a computer fan somewhere, whining slightly sharp
    x += np.sin(2 * np.pi * (round(2310 * d) / d) * t) * 0.004
    x = Y.loopable(Y.norm(x, 0.42), 1.0)
    return Y.stereoize(x, 0.0012), "stereo"


def phone_ring(i):
    def f():
        d = 6.5
        t = Y.t_axis(d)
        tone_ = (np.sin(2 * np.pi * 440 * t) + np.sin(2 * np.pi * 480 * t)) * 0.5
        gate = np.zeros_like(t)
        for k in range(2 + i % 2):
            s0 = 0.2 + k * 3.0
            gate += ((t > s0) & (t < s0 + 1.6)).astype(float) * (0.6 + 0.4 * (np.sin(2 * np.pi * 20 * t) > 0))
        x = Y.bp(tone_ * gate, 300, 3000)
        return Y.norm(Y.reverb(Y.lp(x, 1800 - i * 400), 2.5, 0.6, f"l4ph{i}"), 0.5)
    return f


def typing():
    out = np.zeros(int(4.0 * Y.SR))
    r = Y.rng("l4type")
    at = 0.2
    while at < 3.6:
        n = int(0.03 * Y.SR)
        tt = np.arange(n) / Y.SR
        click = Y.bp(Y.white(0.03, f"k{at:.3f}"), 1500, 7000)[:n] * np.exp(-tt * 200)
        Y.place(out, click, at, 0.5 + r.random() * 0.5)
        at += 0.06 + r.random() * 0.14 + (0.5 if r.random() < 0.05 else 0)
    return Y.norm(Y.reverb(Y.lp(out, 3000), 2.0, 0.6, "l4type"), 0.45)


def copier_sound():
    d = 4.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "l4cop"), 300, 2500) * (0.5 + 0.5 * np.sin(2 * np.pi * 1.6 * t) ** 2)
    x += np.sin(2 * np.pi * 95 * t) * 0.3
    x *= np.sin(np.pi * t / d) ** 0.5
    return Y.norm(Y.reverb(x, 2.0, 0.5, "l4cop"), 0.45)


def glug():
    out = np.zeros(int(2.5 * Y.SR))
    for k in range(4):
        n = int(0.15 * Y.SR)
        tt = np.arange(n) / Y.SR
        fr = 180 + 260 * tt / 0.15
        Y.place(out, np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 25), 0.2 + k * 0.32, 0.8)
    return Y.norm(Y.reverb(out, 1.6, 0.5, "l4glug"), 0.5)


def chime():
    d = 3.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for k, f in enumerate((784.0, 659.3, 523.3)):
        n = int(1.6 * Y.SR)
        tt = np.arange(n) / Y.SR
        note = (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(4 * np.pi * f * tt)) * np.exp(-tt * 2.2)
        Y.place(x, note, 0.1 + k * 0.45, 0.7)
    return Y.norm(Y.reverb(x, 2.2, 0.4, "l4chime"), 0.7)


def closing():
    """A tired voice on the PA, too muffled to understand."""
    d = 4.5
    x = Y.whisper("l4pa", d) * 0.6
    t = Y.t_axis(d)
    f0 = 120 + 15 * np.sin(2 * np.pi * 0.7 * t)
    voice = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * 0.25
    syll = (np.sin(2 * np.pi * 3.1 * t) > -0.2).astype(float) * np.clip(np.sin(np.pi * t / d) * 3, 0, 1)
    voice = Y.bp(voice * syll, 300, 2400)
    x = Y.bp(x + voice, 400, 3000)
    x = np.round(x * 10) / 10
    return Y.norm(Y.reverb(x, 2.5, 0.5, "l4pa"), 0.6)


sound("ambient.level_4", [("ambient/level_4", office_loop)], stream=True)
sound("ambient.level_4.additions",
      [(f"ambient/level_4_phone{i}", phone_ring(i)) for i in range(3)] +
      [("ambient/level_4_typing", typing), ("ambient/level_4_copier", copier_sound), ("ambient/level_4_glug", glug)],
      subtitle="Somewhere, a phone")
sound("event.level_4_chime", [("event/level_4_chime", chime)], subtitle="PA chime")
sound("event.level_4_closing", [("event/level_4_closing", closing)], subtitle="An announcement")

level("level_4", fog="#a8acb0", sky="#a8acb0", ambient="ambient.level_4", additions="ambient.level_4.additions",
      additions_chance=0.005, fixed_time=6000, height=32)
