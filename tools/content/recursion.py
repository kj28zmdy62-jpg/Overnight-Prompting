"""Recursion - the same sitting room, over and over. One thing changes every time you go through a door.
Leave through the wall where the newest change happened."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, custom, level, sound, lang, entity_model, entity_tex, ITEM_MODELS
import tex as T
import synth as Y
from content.rb_shared import text, text_center, split, disc, ellipse, line, wood
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32

# ------------------------------------------------------------------ walls, floor, ceiling


def wallpaper(seed="recwp"):
    """Warm cream paper with a small sage damask motif, faintly yellowed."""
    base = T.ramp(T.fbm(seed, octaves=3, base=5) * 0.5 + 0.25, "#d9cfb2", "#e4dbc0", "#ece4cc")
    yy, xx = np.mgrid[0:S, 0:S]
    # diamond lattice of small fleurs every 16px, offset rows
    m = np.zeros((S, S))
    for cy, cx in ((8, 8), (24, 24), (8, 24 + 8), (24, 8 + 8)):
        cx %= S
        dx = np.minimum(np.abs(xx - cx), S - np.abs(xx - cx))
        dy = np.minimum(np.abs(yy - cy), S - np.abs(yy - cy))
        petal = (np.abs(dx) + np.abs(dy) * 0.6 < 3.2) | (np.abs(dx) * 0.6 + np.abs(dy) < 3.2)
        m = np.maximum(m, petal.astype(float))
        m = np.maximum(m, ((dx == 0) & (dy < 6)).astype(float) * 0.6)
    img = T.blend(base, T.solid("#9aa48a"), m * 0.55)
    stripe = ((xx % 16) == 0).astype(float) * 0.05
    img = T.shade(img, 1 - stripe)
    return T.grain(img, seed, 0.025)


def wainscot():
    """Dark stained wood panelling, one raised panel per block with a cap rail on top."""
    img = wood("recwain", "#3e2617", "#5a3a22")
    img = T.rect(img, 4, 8, 28, 28, "#000000", 0.14)
    img = T.rect(img, 5, 9, 27, 27, "#6b4a2e", 0.25)
    img[8, 4:28] = img[8, 4:28] * 0.6
    img[9:28, 4] = img[9:28, 4] * 0.65
    img[27, 4:28] = np.clip(img[27, 4:28] * 1.3, 0, 1)
    img[0:4] = T.shade(wood("reccap", "#4a2e1b", "#6a4428", shape=(4, S), grain_dir="h"), 1.15)
    img[4] = img[4] * 0.55
    return T.grain(img, "recwain", 0.02)


def parquet():
    """Herringbone oak parquet, scuffed and waxed."""
    img = np.zeros((S, S, 3))
    yy, xx = np.mgrid[0:S, 0:S]
    a = wood("recpq1", "#7a5534", "#9a6e44", grain_dir="h", rings=4)
    b = wood("recpq2", "#71502f", "#936940", grain_dir="v", rings=4)
    blk = ((xx // 8 + yy // 8) % 2 == 0)
    img = np.where(blk[..., None], a, b)
    seam = ((xx % 8) == 0) | ((yy % 8) == 0)
    img = T.shade(img, 1 - seam * 0.3)
    stagger = (((yy // 8) % 2 == 0) & ((xx % 8) == 4) & blk) | (((xx // 8) % 2 == 1) & ((yy % 8) == 4) & ~blk)
    img = T.shade(img, 1 - stagger * 0.2)
    wax = T.noise("recwax", sigma=3)
    img = T.shade(img, 0.92 + wax * 0.14)
    return T.grain(img, "recpq", 0.03)


def rug(border=False):
    """Faded wool rug: deep red field with navy and ochre medallions."""
    yy, xx = np.mgrid[0:S, 0:S]
    field = T.ramp(T.noise("recrug", sigma=1.2), "#6e1f1c", "#82302a")
    d = np.abs(xx - 15.5) + np.abs(yy - 15.5)
    img = T.blend(field, T.solid("#1f2c4a"), ((d > 9) & (d < 12)).astype(float))
    img = T.blend(img, T.solid("#b78a3a"), ((d > 4) & (d < 6)).astype(float))
    img = T.blend(img, T.solid("#d8c7a0"), (d < 2.5).astype(float))
    corner = (np.minimum(xx, 31 - xx) + np.minimum(yy, 31 - yy)) < 6
    img = T.blend(img, T.solid("#1f2c4a"), corner.astype(float) * 0.8)
    if border:
        img = T.ramp(T.noise("recrugb", sigma=1), "#1d2944", "#26355a")
        zig = (np.abs(((xx % 8) - 4)) == np.abs((yy % 8) - 4))
        img = T.blend(img, T.solid("#b78a3a"), zig.astype(float))
        img[:3] = T.hexc("#6e1f1c")
    wear = T.noise("recwear" + str(border), sigma=2.5)
    img = T.shade(img, 0.85 + wear * 0.25)
    fib = T.noise("recfib", sigma=0.3)
    return T.grain(T.shade(img, 0.92 + fib * 0.12), "recrug", 0.03)


def ceiling():
    base = T.ramp(T.fbm("recceil", octaves=3, base=6), "#d6cfbb", "#e2dcca")
    img = T.stains(base, "recceils", "#b5a682", count=1, strength=0.2, size=10)
    return T.grain(img, "recceil", 0.02)


def cornice():
    img = ceiling()
    img[24:] = T.ramp(np.linspace(0, 1, 8)[:, None] * np.ones((1, S)), "#cfc6ae", "#9a917c")
    img[26] = T.hexc("#efe9d8")
    img[30] = T.hexc("#857c68")
    return img


def door(top):
    """Six-panel painted door, chipped, brass knob on the lower half."""
    img = T.ramp(T.noise("recdoor" + str(top), sigma=2), "#ded6c2", "#e8e1cf")
    rows = [(4, 14), (18, 30)] if top else [(2, 16), (20, 28)]
    for (y0, y1) in rows:
        for (x0, x1) in ((5, 14), (18, 27)):
            img = T.rect(img, x0, y0, x1, y1, "#b8ae96", 0.35)
            img[y0, x0:x1] = img[y0, x0:x1] * 0.82
            img[y0:y1, x0] = img[y0:y1, x0] * 0.82
            img[y1 - 1, x0:x1] = np.clip(img[y1 - 1, x0:x1] * 1.08, 0, 1)
    img[:, :2] = T.hexc("#5a3a22")
    img[:, -2:] = T.hexc("#5a3a22")
    if top:
        img[:2] = T.hexc("#5a3a22")
    else:
        img = T.rect(img, 23, 2, 27, 6, "#a8803a")
        img = T.rect(img, 24, 3, 26, 5, "#e0c070")
        img = T.rect(img, 22, 8, 28, 9, "#7a5a22")
        # scuffs where a hundred hands pushed it
        img = T.stains(img, "recdoorsc", "#8a7e66", count=2, strength=0.25, size=4)
    chips = (T.rng("recchip" + str(top)).random((S, S)) > 0.985)
    img = T.blend(img, T.solid("#7a6a52"), chips.astype(float))
    return T.grain(img, "recdoor" + str(top), 0.02)


tex("rec_wallpaper", wallpaper)
tex("rec_wainscot", wainscot)
tex("rec_parquet", parquet)
tex("rec_rug", lambda: rug(False))
tex("rec_rug_border", lambda: rug(True))
tex("rec_ceiling", ceiling)
tex("rec_cornice", cornice)
tex("rec_door_low", lambda: door(False))
tex("rec_door_high", lambda: door(True))

cube("rec_wallpaper", "rec_wallpaper", display="Sitting Room Wallpaper", sound="wool")
cube("rec_wainscot", "rec_wainscot", display="Sitting Room Panelling", sound="wood")
cube("rec_parquet", "rec_parquet", display="Parquet", sound="wood")
cube("rec_rug", "rec_rug", display="Rug", sound="wool")
cube("rec_rug_border", "rec_rug_border", display="Rug Border", sound="wool")
cube("rec_ceiling", "rec_ceiling", display="Plaster Ceiling", flat=True)
cube_faces("rec_cornice", top="rec_ceiling", bottom="rec_ceiling", side="rec_cornice", display="Cornice")
for half in ("low", "high"):
    custom(f"rec_door_{half}", "com.noclip.block.RecursionDoorBlock",
           {"parent": "minecraft:block/cube_all", "textures": {"all": f"noclip:block/rec_door_{half}"}},
           display="Sitting Room Door", unbreakable=True, sound="wood")

# ------------------------------------------------------------------ the twelve things that can change
# north: clock (subtle), bookshelf (medium), painting (creepy)
# east:  floor lamp (subtle), armchair (medium), window (creepy)
# south: plant (subtle), sofa (medium), photograph (creepy)
# west:  vase (subtle), chair before the mirror (medium), mirror (creepy)


def clock(hands=True):
    img = T.solid("#3a2416")
    img = T.blend(img, wood("recclk", "#4a2e1b", "#6a4428"), np.ones((S, S)))
    face = disc(S, S, 15.5, 15.5, 12)
    img = T.blend(img, T.ramp(T.noise("recclkf", sigma=2), "#e8dfc6", "#f2ead6"), face.astype(float))
    ring = disc(S, S, 15.5, 15.5, 12) & ~disc(S, S, 15.5, 15.5, 11)
    img = T.blend(img, T.solid("#a8803a"), ring.astype(float))
    for k in range(12):
        a = k / 12 * 2 * np.pi
        x, y = 15.5 + np.sin(a) * 9.5, 15.5 - np.cos(a) * 9.5
        img[int(round(y)), int(round(x))] = T.hexc("#2a2018")
    if hands:
        img = line(img, 15.5, 15.5, 15.5 + np.sin(-np.pi / 3) * 6, 15.5 - np.cos(-np.pi / 3) * 6, "#1a1410")
        img = line(img, 15.5, 15.5, 15.5 + np.sin(np.pi / 3) * 9, 15.5 - np.cos(np.pi / 3) * 9, "#1a1410")
    img[15:17, 15:17] = T.hexc("#1a1410")
    return T.grain(img, "recclk" + str(hands), 0.02)


def books():
    img = wood("recshelf", "#3a2416", "#4e3220", grain_dir="h")
    r = T.rng("recbooks")
    cols = ["#6a1e1a", "#1e3a2a", "#2a2e4a", "#7a6a3a", "#4a2a3a", "#2e2a24", "#8a5a2a", "#3a4a5a"]
    for (y0, y1) in ((2, 15), (18, 31)):
        x = 1
        while x < 31:
            w = 2 + int(r.random() * 3)
            hgt = int((y1 - y0) * (0.7 + r.random() * 0.3))
            c = T.hexc(cols[int(r.random() * len(cols))])
            for xx in range(x, min(31, x + w)):
                img[y1 - hgt:y1, xx] = c * (0.8 + 0.25 * (xx == x))
            img[y1 - hgt + 2:y1 - hgt + 3, x:min(31, x + w)] = c * 1.4
            x += w + (1 if r.random() < 0.2 else 0)
        img[y1:y1 + 1] = T.hexc("#2a1a10")
    return T.grain(np.clip(img, 0, 1), "recbooks", 0.03)


def shelf_empty():
    img = wood("recshelf", "#3a2416", "#4e3220", grain_dir="h")
    img = T.shade(img, 0.55)
    for (y0, y1) in ((2, 15), (18, 31)):
        img[y1:y1 + 1] = T.hexc("#2a1a10")
        # dust outlines of where the books stood
        r = T.rng("recdust" + str(y0))
        x = 1
        while x < 31:
            w = 2 + int(r.random() * 3)
            img[y1 - 1, x:x + w - 1] = T.hexc("#7a6a58")
            x += w
    return T.grain(img, "recshelfe", 0.03)


def painting(figure=False):
    """A 64x64 pastoral: hills, a lane, a white house. In the wrong version someone stands in the lane."""
    H = 64
    yy, xx = np.mgrid[0:H, 0:H]
    sky = T.ramp(np.clip(yy / 30.0, 0, 1), "#8aa4b4", "#d8d0b0")
    img = sky.copy()
    hill1 = yy > 30 + 6 * np.sin(xx / 11.0) + 3 * np.sin(xx / 5.0 + 1)
    hill2 = yy > 40 + 4 * np.sin(xx / 9.0 + 2)
    img[hill1] = T.ramp(T.noise("rph1", shape=(H, H), sigma=1.5), "#5a7040", "#6e8450")[hill1]
    img[hill2] = T.ramp(T.noise("rph2", shape=(H, H), sigma=1.5), "#486034", "#5a7442")[hill2]
    lane = (np.abs(xx - (32 + (yy - 40) * 0.6)) < (yy - 34) * 0.25) & (yy > 36)
    img[lane] = T.hexc("#b8a678")
    # house
    img[24:34, 40:52] = T.hexc("#e8e2d0")
    for k in range(7):
        img[17 + k, 40 + 6 - k:52 - 6 + k] = T.hexc("#6a3a2a")
    img[28:34, 44:47] = T.hexc("#3a2a20")
    img[26:29, 48:51] = T.hexc("#f0d880")
    # clouds and painterly texture
    cl = (T.noise("rpcl", shape=(H, H), sigma=4) > 0.62) & (yy < 22)
    img[cl] = img[cl] * 0.4 + T.hexc("#f4f0e6") * 0.6
    img = T.shade(img, 0.9 + T.noise("rpbr", shape=(H, H), sigma=0.7) * 0.18)
    if figure:
        # a thin dark figure on the lane, facing out of the canvas
        img[42:56, 21:24] = T.hexc("#141210")
        img[37:41, 21:24] = T.hexc("#141210")
        img[38, 21] = img[38, 23] = T.hexc("#d8d2c0")
        img[43:50, 20] = img[43:50, 24] = T.hexc("#141210")
    # gilt frame
    fr = T.ramp(T.noise("rpfr", shape=(H, H), sigma=1), "#8a6a2a", "#c8a050")
    edge = (np.minimum(np.minimum(xx, 63 - xx), np.minimum(yy, 63 - yy)) < 4)
    img[edge] = fr[edge]
    inner = (np.minimum(np.minimum(xx, 63 - xx), np.minimum(yy, 63 - yy)) == 4)
    img[inner] = T.hexc("#4a3818")
    return img


def window(face=False):
    """Night window: black sky, the faint shape of a garden, net curtains. Someone may be outside."""
    H = 64
    yy, xx = np.mgrid[0:H, 0:H]
    img = T.ramp(np.clip(yy / 64.0, 0, 1), "#0a0e18", "#141a24")
    trees = yy > 40 + 6 * np.sin(xx / 7.0) + 3 * np.sin(xx / 3.0)
    img[trees] = T.hexc("#05070a")
    if face:
        f = ellipse(H, H, 30, 30, 7, 9)
        img[f] = T.ramp(T.noise("recface", shape=(H, H), sigma=1.5), "#b8b0a0", "#d0c8b8")[f]
        for ex in (27, 33):
            img[ellipse(H, H, ex, 28, 1.6, 2.2)] = T.hexc("#0a0806")
        img[ellipse(H, H, 30, 35, 2.2, 1.2)] = T.hexc("#2a1a16")
        hands = ellipse(H, H, 19, 40, 3, 4) | ellipse(H, H, 41, 40, 3, 4)
        img[hands] = T.hexc("#a8a090")
    # glass sheen and net curtains at the sides
    sheen = np.clip(1 - np.abs((xx - yy * 0.6) - 18) / 5.0, 0, 1) * 0.12
    img = np.clip(img + sheen[..., None], 0, 1)
    curtain = (xx < 14) | (xx > 49)
    lace = T.ramp(T.noise("reclace", shape=(H, H), sigma=0.5), "#b8b4a8", "#d8d4c8")
    lace = T.shade(lace, 0.85 + 0.15 * np.sin(xx * 1.4))
    img = T.blend(img, lace, curtain.astype(float) * 0.85)
    # sash bars and frame
    img[:, 31:33] = T.hexc("#e8e2d2")
    img[31:33, :] = T.hexc("#e8e2d2")
    edge = (np.minimum(np.minimum(xx, 63 - xx), np.minimum(yy, 63 - yy)) < 3)
    img[edge] = T.hexc("#ddd6c4")
    img[60:, :] = T.hexc("#cfc8b4")
    return T.grain(img, "recwin" + str(face), 0.02)


def photo(scratched=False):
    """A small framed family photograph: three people on a sofa. This sofa."""
    img = T.solid("#2a1a10")
    img[3:29, 3:29] = T.ramp(T.noise("recph", shape=(26, 26), sigma=2), "#8a7a62", "#a8987a")
    img[18:29, 3:29] = T.hexc("#3e5a3a")  # the green sofa
    for i, (x, h) in enumerate(((8, 11), (15, 13), (22, 9))):
        img[29 - h - 6:29 - 6, x - 2:x + 3] = T.hexc(["#4a3a5a", "#2a2a3a", "#7a3a2a"][i])
        hy = 29 - h - 10
        img[hy:hy + 4, x - 1:x + 2] = T.hexc("#d8bca0")
        if scratched:
            for k in range(6):
                img[hy - 1 + k, x - 2 + (k % 4)] = T.hexc("#f0ece0")
                img[hy - 1 + k, x + 2 - (k % 3)] = T.hexc("#e0dcd0")
    img = T.grain(img, "recph" + str(scratched), 0.03)
    img = T.shade(img, 0.9 + T.noise("recphs", sigma=3) * 0.15)
    img[:2] = img[-2:] = T.hexc("#c8a050")
    img[:, :2] = img[:, -2:] = T.hexc("#c8a050")
    return img


def mirror(figure=False):
    """Tall mirror, 32x64: a grey, slightly warm reflection of a room that is not quite this one."""
    H, W = 64, 32
    yy, xx = np.mgrid[0:H, 0:W]
    img = T.ramp(np.clip(yy / 64.0, 0, 1) * 0.6 + T.noise("recmir", shape=(H, W), sigma=4) * 0.4,
                 "#7a7c78", "#9a9a92", "#6a6a64")
    streak = np.clip(1 - np.abs((xx + yy * 0.4) - 20) / 6.0, 0, 1) * 0.18
    img = np.clip(img + streak[..., None], 0, 1)
    # the reflected doorway across the room
    img[30:52, 11:20] = T.hexc("#4a4438")
    if figure:
        img[ellipse(H, W, 15.5, 22, 3.2, 4)] = T.hexc("#16141a")
        img[26:50, 11:21] = T.hexc("#16141a")
        img[50:58, 12:15] = T.hexc("#16141a")
        img[50:58, 17:20] = T.hexc("#16141a")
    # foxing at the edges, silver gone black in spots
    fox = (T.noise("recfox", shape=(H, W), sigma=1.2) > 0.78) & (
        (np.minimum(xx, W - 1 - xx) < 6) | (np.minimum(yy, H - 1 - yy) < 6))
    img[fox] = img[fox] * 0.45
    edge = (np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy)) < 3)
    img[edge] = T.ramp(T.noise("recmfr", shape=(H, W), sigma=1), "#8a6a2a", "#c8a050")[edge]
    return img


def velvet():
    img = T.ramp(T.noise("recvel", sigma=0.8), "#2e4430", "#3e5a3e")
    sheen = T.noise("recvels", sigma=3)
    return T.grain(T.shade(img, 0.85 + sheen * 0.3), "recvel", 0.03)


def lamp_shade(on=True):
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        img = T.ramp(np.clip(1 - np.abs(yy - 16) / 18.0, 0, 1), "#d8a860", "#ffe8b0")
    else:
        img = T.ramp(np.clip(1 - np.abs(yy - 16) / 18.0, 0, 1), "#5a4a34", "#7a6648")
    pleat = ((xx % 4) == 0).astype(float) * 0.08
    img = T.shade(img, 1 - pleat)
    img[:2] = img[-2:] = T.hexc("#8a6a3a")
    return T.grain(img, "recshade" + str(on), 0.02)


def leaves(dead=False):
    rgb = T.ramp(T.noise("recleaf" + str(dead), sigma=0.8),
                 *(("#5a4a2a", "#7a6438", "#8a7040") if dead else ("#1e4a22", "#2e6a30", "#4a8a3a")))
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.zeros((S, S))
    r = T.rng("recleafs" + str(dead))
    for _ in range(9 if dead else 16):
        ang = (r.random() - 0.5) * (2.6 if not dead else 3.0)
        ln = 8 + r.random() * 8
        bx, by = 16, 31
        for t in np.linspace(0, 1, 30):
            px = bx + np.sin(ang) * ln * t * (1.4 if dead else 1)
            py = by - np.cos(ang) * ln * t * (1.2 if dead else 1.8) + (t * t * 9 if dead else 0)
            wdt = 2.2 * np.sin(np.pi * min(1, t * 1.2)) + 0.4
            m = ((xx - px) ** 2 + (yy - py) ** 2) < wdt * wdt
            a[m] = 1
    return rgb, a


def pot():
    img = T.ramp(T.noise("recpot", sigma=1.5), "#8a4a2a", "#a85a34")
    img[:4] = T.hexc("#b86a40")
    return T.grain(img, "recpot", 0.04)


def vase():
    img = T.ramp(T.noise("recvase", sigma=2), "#2a3a6a", "#3a4e88")
    yy, xx = np.mgrid[0:S, 0:S]
    band = (np.abs(yy - 14) < 3) & ((xx + yy) % 6 < 3)
    img = T.blend(img, T.solid("#e8e2d2"), band.astype(float))
    return T.grain(img, "recvase", 0.03)


def water():
    rgb = T.ramp(T.noise("recwat", sigma=2), "#5a5040", "#7a705a")
    a = np.clip(T.noise("recwata", sigma=3) * 1.4 - 0.2, 0, 0.7)
    return rgb, a


def sampler():
    """A cross-stitch sampler in a frame, 96x32. Someone made this for whoever comes next."""
    W, H = 96, 32
    img = T.ramp(T.noise("recsamp", shape=(H, W), sigma=1.5), "#e6dcc0", "#efe6cc")
    yy, xx = np.mgrid[0:H, 0:W]
    weave = (((xx + yy) % 2) == 0).astype(float) * 0.05
    img = T.shade(img, 1 - weave)
    img = text_center(img, "ONE THING CHANGES", 8, "#6e1f1c")
    img = text_center(img, "LEAVE BY ITS WALL", 18, "#1f2c4a")
    for x in range(8, 88, 4):
        img[3, x:x + 2] = T.hexc("#3e5a3a")
        img[28, x:x + 2] = T.hexc("#3e5a3a")
    edge = (np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy)) < 2)
    img[edge] = T.hexc("#4a2e1b")
    return img


def brass():
    return T.grain(T.ramp(T.noise("recbrass", sigma=1.5), "#8a6a2a", "#c8a050"), "recbrass", 0.03)


def dark_wood():
    return wood("recdw", "#3a2416", "#5a3a22")


def china():
    img = T.ramp(T.noise("recchina", sigma=2), "#e8e4dc", "#f6f2ea")
    img[12:14] = T.hexc("#3a4e88")
    return img


tex("rec_clock", lambda: clock(True))
tex("rec_clock_nohands", lambda: clock(False))
tex("rec_books", books)
tex("rec_shelf_empty", shelf_empty)
for name, img_fn in (("rec_paint", lambda: painting(False)), ("rec_paintfig", lambda: painting(True)),
                     ("rec_win", lambda: window(False)), ("rec_winface", lambda: window(True))):
    for idx, part in enumerate(("tl", "tr", "bl", "br")):
        tex(f"{name}_{part}", (lambda f, i: lambda: split(f(), 2, 2)[i])(img_fn, idx))
for name, img_fn in (("rec_mirror", lambda: mirror(False)), ("rec_mirrorfig", lambda: mirror(True))):
    for idx, part in enumerate(("top", "bot")):
        tex(f"{name}_{part}", (lambda f, i: lambda: split(f(), 1, 2)[i])(img_fn, idx))
for idx, part in enumerate(("l", "m", "r")):
    tex(f"rec_sampler_{part}", (lambda i: lambda: split(sampler(), 3, 1)[i])(idx))
tex("rec_photo", lambda: photo(False))
tex("rec_photo_scratched", lambda: photo(True))
tex("rec_velvet", velvet)
tex("rec_shade_on", lambda: lamp_shade(True))
tex("rec_shade_off", lambda: lamp_shade(False))
tex("rec_leaves", lambda: leaves(False))
tex("rec_leaves_dead", lambda: leaves(True))
tex("rec_pot", pot)
tex("rec_vase", vase)
tex("rec_water", water)
tex("rec_brass", brass)
tex("rec_darkwood", dark_wood)
tex("rec_china", china)

# wall-mounted pieces: authored against the north side (z=0), facing south into the room
def wall_piece(name, texture, display, thick=1, frame="rec_darkwood", **kw):
    shaped(name, [(0, 0, 0, 16, 16, thick, {"all": "frame", "south": "face"})],
           {"face": texture, "frame": frame}, display=display, collides=False, unbreakable=True, sound="wood", **kw)


wall_piece("rec_clock", "rec_clock", "Wall Clock", thick=2)
wall_piece("rec_clock_nohands", "rec_clock_nohands", "Wall Clock", thick=2)
for part in ("tl", "tr", "bl", "br"):
    wall_piece(f"rec_paint_{part}", f"rec_paint_{part}", "Painting", frame="rec_brass")
    wall_piece(f"rec_paintfig_{part}", f"rec_paintfig_{part}", "Painting", frame="rec_brass")
for part in ("l", "m", "r"):
    wall_piece(f"rec_sampler_{part}", f"rec_sampler_{part}", "Sampler")
wall_piece("rec_photo", "rec_photo", "Family Photograph", thick=1, frame="rec_brass")
wall_piece("rec_photo_scratched", "rec_photo_scratched", "Family Photograph", thick=1, frame="rec_brass")

# windows and the mirror replace the wall block itself (only the room face is ever seen)
for part in ("tl", "tr", "bl", "br"):
    cube(f"rec_win_{part}", f"rec_win_{part}", display="Window", sound="glass", flat=True)
    cube(f"rec_winface_{part}", f"rec_winface_{part}", display="Window", sound="glass", flat=True)
for part in ("top", "bot"):
    cube(f"rec_mirror_{part}", f"rec_mirror_{part}", display="Mirror", sound="glass", flat=True)
    cube(f"rec_mirrorfig_{part}", f"rec_mirrorfig_{part}", display="Mirror", sound="glass", flat=True)

# bookshelf: a deep case against the north wall
shaped("rec_bookshelf", [(0, 0, 0, 16, 16, 10, {"all": "wood", "south": "books"})],
       {"books": "rec_books", "wood": "rec_darkwood"}, display="Bookshelf", unbreakable=True)
shaped("rec_bookshelf_empty", [(0, 0, 0, 16, 16, 10, {"all": "wood", "south": "books"})],
       {"books": "rec_shelf_empty", "wood": "rec_darkwood"}, display="Empty Bookshelf", unbreakable=True)

# wingback armchair, back to the south, facing north
shaped("rec_armchair", [
    (1, 4, 1, 15, 8, 14, "vel"),          # seat
    (1, 8, 12, 15, 22, 15, "vel"),        # back
    (0, 4, 1, 3, 13, 14, "vel"),          # arms
    (13, 4, 1, 16, 13, 14, "vel"),
    (0, 16, 10, 3, 21, 15, "vel"),        # wings
    (13, 16, 10, 16, 21, 15, "vel"),
    (2, 0, 2, 4, 4, 4, "wood"), (12, 0, 2, 14, 4, 4, "wood"),
    (2, 0, 12, 4, 4, 14, "wood"), (12, 0, 12, 14, 4, 14, "wood"),
], {"vel": "rec_velvet", "wood": "rec_darkwood"}, display="Wingback Armchair", unbreakable=True, sound="wool")

# three-seat sofa in three pieces, back to the south
SOFA_BASE = [(0, 3, 2, 16, 8, 15, "vel"), (0, 8, 12, 16, 17, 16, "vel")]
shaped("rec_sofa_l", SOFA_BASE + [(13, 3, 2, 16, 12, 16, "vel"), (13, 0, 3, 15, 3, 5, "wood")],
       {"vel": "rec_velvet", "wood": "rec_darkwood"}, display="Sofa", unbreakable=True, sound="wool")
shaped("rec_sofa_m", SOFA_BASE, {"vel": "rec_velvet", "wood": "rec_darkwood"}, display="Sofa", unbreakable=True,
       sound="wool")
shaped("rec_sofa_r", SOFA_BASE + [(0, 3, 2, 3, 12, 16, "vel"), (1, 0, 3, 3, 3, 5, "wood")],
       {"vel": "rec_velvet", "wood": "rec_darkwood"}, display="Sofa", unbreakable=True, sound="wool")

# floor lamp: base piece and lit / dark shade
shaped("rec_lamp_pole", [(6, 0, 6, 10, 1, 10, "brass"), (7, 1, 7, 9, 16, 9, "brass")], {"brass": "rec_brass"},
       display="Floor Lamp", collides=False, unbreakable=True, sound="metal")
shaped("rec_lamp_on", [(7, 0, 7, 9, 4, 9, "brass"), (3, 4, 3, 13, 12, 13, {"all": "shade", "up": None, "down": None})],
       {"shade": "rec_shade_on", "brass": "rec_brass"}, display="Lamp", light=13, emissive=True, collides=False,
       unbreakable=True, sound="wool")
shaped("rec_lamp_off", [(7, 0, 7, 9, 4, 9, "brass"), (3, 4, 3, 13, 12, 13, {"all": "shade", "up": None, "down": None})],
       {"shade": "rec_shade_off", "brass": "rec_brass"}, display="Lamp", collides=False, unbreakable=True, sound="wool")

# potted plant: pot + leaves as one block
shaped("rec_plant", [(4, 0, 4, 12, 7, 12, "pot"), (0, 6, 8, 16, 16, 8, {"all": None, "north": "leaf", "south": "leaf"}),
                     (8, 6, 0, 8, 16, 16, {"all": None, "east": "leaf", "west": "leaf"})],
       {"pot": "rec_pot", "leaf": "rec_leaves"}, display="Potted Fern", collides=False, unbreakable=True, sound="grass")
shaped("rec_plant_dead", [(4, 0, 4, 12, 7, 12, "pot"),
                          (0, 6, 8, 16, 16, 8, {"all": None, "north": "leaf", "south": "leaf"}),
                          (8, 6, 0, 8, 16, 16, {"all": None, "east": "leaf", "west": "leaf"})],
       {"pot": "rec_pot", "leaf": "rec_leaves_dead"}, display="Dead Fern", collides=False, unbreakable=True,
       sound="grass")

# sideboard (against the north side, facing south) with or without the vase on top
SIDEBOARD = [(0, 0, 0, 16, 12, 9, {"all": "wood"}), (1, 2, 9, 15, 3, 10, "brass")]
shaped("rec_sideboard", SIDEBOARD, {"wood": "rec_darkwood", "brass": "rec_brass"}, display="Sideboard",
       unbreakable=True)
shaped("rec_sideboard_vase", SIDEBOARD + [(5, 12, 2, 10, 20, 7, "vase"), (6, 20, 3, 9, 23, 6, "vase")],
       {"wood": "rec_darkwood", "brass": "rec_brass", "vase": "rec_vase"}, display="Sideboard", unbreakable=True)
shaped("rec_sideboard_fallen", SIDEBOARD + [(3, 12, 2, 12, 16, 7, "vase"), (12, 12, 3, 15, 15, 6, "vase"),
                                            (0, 12, 0, 16, 12.2, 9, {"all": None, "up": "water"})],
       {"wood": "rec_darkwood", "brass": "rec_brass", "vase": "rec_vase", "water": "rec_water"},
       display="Sideboard", unbreakable=True, render="translucent")

# plain dining chair (the one that turns up in front of the mirror), back to the south
shaped("rec_dining_chair", [
    (3, 7, 3, 13, 9, 13, "wood"), (3, 9, 11, 13, 24, 13, "wood"),
    (3, 0, 3, 5, 7, 5, "wood"), (11, 0, 3, 13, 7, 5, "wood"), (3, 0, 11, 5, 7, 13, "wood"), (11, 0, 11, 13, 7, 13, "wood"),
], {"wood": "rec_darkwood"}, display="Dining Chair", unbreakable=True)

# centre pieces that never change: a little table with a teacup, a ceiling light
shaped("rec_side_table", [(2, 12, 2, 14, 14, 14, "wood"), (7, 0, 7, 9, 12, 9, "wood"), (4, 0, 4, 12, 1, 12, "wood"),
                          (6, 14, 6, 10, 17, 10, "china")],
       {"wood": "rec_darkwood", "china": "rec_china"}, display="Side Table", unbreakable=True)
shaped("rec_ceiling_light", [(7, 12, 7, 9, 16, 9, "brass"), (3, 6, 3, 13, 12, 13, {"all": "shade", "up": "brass"})],
       {"shade": "rec_shade_on", "brass": "rec_brass"}, display="Ceiling Light", light=14, emissive=True,
       collides=False, unbreakable=True, sound="glass")

# ------------------------------------------------------------------ the Resident


def resident_tex():
    c = Canvas()
    suit = lambda w, h: T.grain(T.ramp(T.noise(f"rrs{w}{h}", shape=(h, w), sigma=1), "#24262a", "#2e3034"), f"rrs{w}", 0.04)

    def head(w, h):
        img = T.ramp(T.noise(f"rrh{w}{h}", shape=(h, w), sigma=1.5), "#c8beac", "#d6ccba")
        img[:2] = T.hexc("#2a241e")
        return img

    def face(w, h):
        img = head(w, h)
        # the same face as the figure in the painting: two dark hollows and nothing else
        img[3:5, 1:3] = T.hexc("#16120e")
        img[3:5, 5:7] = T.hexc("#16120e")
        return img

    def body(w, h):
        img = suit(w, h)
        if w == 8:
            img[:, 3:5] = T.hexc("#e8e4d8")
            img[1:8, 3:5] = T.hexc("#6a1e1a")
        return img

    def arm(w, h):
        img = suit(w, h)
        img[-2:] = T.hexc("#c8beac")
        return img

    def leg(w, h):
        img = suit(w, h)
        img[-2:] = T.hexc("#120e0a")
        return img

    paint_humanoid(c, head, body, arm, leg, face=face)
    return c.out()


entity_model("rec_resident", humanoid(arm_len=13, leg_len=13))
entity_tex("rec_resident", resident_tex)
lang("entity.noclip.rec_resident", "The Resident")
lang("item.noclip.rec_resident_spawn_egg", "The Resident Spawn Egg")
ITEM_MODELS["rec_resident_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def clock_room():
    """A long-case clock ticking somewhere in the house, and a room that holds its breath."""
    d = 24.0
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    for k in range(int(d)):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        pitch = 2200 if k % 2 == 0 else 1850
        click = np.sin(2 * np.pi * pitch * tt) * np.exp(-tt * 140) + Y.white(0.05, f"rtk{k}")[:n] * np.exp(-tt * 300) * 0.4
        Y.place(out, click, k + 0.02, 0.5)
    out = Y.reverb(Y.lp(out, 5000), 0.9, 0.5, "rectick")[: len(t)]
    room = Y.lp(Y.brown(d, "recroom"), 140) * 0.35
    hiss = Y.bp(Y.pink(d, "rechiss"), 2000, 7000) * 0.015
    x = Y.loopable(Y.norm(out * 0.5 + room + hiss, 0.45), 1.0)
    return Y.stereoize(x, 0.0009), "stereo"


def chime():
    """The clock strikes. It strikes the wrong number of times."""
    d = 9.0
    out = np.zeros(int(d * Y.SR))
    for k in range(5):
        n = int(2.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        bell = sum(np.sin(2 * np.pi * f * tt) * a * np.exp(-tt * (1.4 + i))
                   for i, (f, a) in enumerate(((392.0, 1.0), (784.4, 0.45), (1176.0, 0.25), (1568.0, 0.12))))
        Y.place(out, bell, 0.2 + k * 1.45, 0.7 - 0.08 * k)
    return Y.norm(Y.reverb(Y.lp(out, 3000), 2.5, 0.5, "recchime"), 0.6)


def floor_creak(i):
    def f():
        d = 1.6
        t = Y.t_axis(d)
        f0 = 140 + i * 40 + 30 * np.sin(2 * np.pi * 0.8 * t)
        tone = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)
        grit = (Y.rng(f"rcr{i}").random(len(t)) > 0.97) * 1.0
        x = Y.bp(tone * 0.4 + grit, 200, 2500) * np.sin(np.pi * t / d) ** 2
        return Y.reverb(Y.norm(Y.lp(x, 1500), 0.5), 1.6, 0.5, f"rcr{i}")
    return f


def door_elsewhere():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.impact("recdoorx", 0.5), 0.1, 0.8)
    n = int(0.1 * Y.SR)
    latch = Y.bp(Y.white(0.1, "reclatch"), 2000, 8000) * Y.decay(n, 0.01)
    Y.place(out, latch, 0.12, 0.4)
    return Y.reverb(Y.lp(out, 900), 2.5, 0.65, "recdx")


def muffled_tv():
    """A television in another room. You cannot make out the words. Nobody is watching it."""
    d = 4.0
    t = Y.t_axis(d)
    voice = Y.whisper("rectv", d) * 1.2
    voice = Y.lp(voice + Y.bp(Y.white(d, "rectv2"), 200, 900) * 0.1, 700)
    applause = Y.bp(Y.pink(d, "recapp"), 800, 3000) * np.clip(np.sin(np.pi * (t - 2.6) / 1.4), 0, 1) * 0.4
    return Y.reverb(Y.norm(voice + applause, 0.4) * np.sin(np.pi * t / d) ** 0.5, 1.2, 0.5, "rectv")


def door_pass():
    """Handle, hinge, the door shutting behind you."""
    d = 1.6
    out = np.zeros(int(d * Y.SR))
    n = int(0.08 * Y.SR)
    Y.place(out, Y.bp(Y.white(0.08, "rdp1"), 1500, 7000) * Y.decay(n, 0.008), 0.0, 0.6)
    t = Y.t_axis(0.5)
    hinge = Y.bp(np.sin(2 * np.pi * np.cumsum(300 + 200 * t) / Y.SR) * (Y.rng("rdh").random(len(t)) > 0.6), 400, 3000)
    Y.place(out, hinge * np.sin(np.pi * t / 0.5), 0.1, 0.25)
    Y.place(out, Y.impact("rdp2", 0.4), 0.75, 0.7)
    Y.place(out, Y.bp(Y.white(0.08, "rdp3"), 1500, 7000) * Y.decay(n, 0.006), 0.78, 0.5)
    return Y.norm(Y.reverb(Y.lp(out, 5000), 1.0, 0.35, "rdp"), 0.8)


def reset_sting():
    """The room is clean again. Everything you noticed was for nothing."""
    d = 4.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for f in (55.0, 58.3, 110.0):
        x += np.sin(2 * np.pi * f * t) * np.exp(-t * 0.8)
    x += Y.lp(Y.white(d, "rrs"), 200) * np.exp(-t * 2) * 0.6
    rev = Y.reverb(Y.bp(Y.white(d, "rrs2"), 1000, 6000) * np.exp(-((t - 0.5) ** 2) / 0.05) * 0.3, 2.5, 0.8, "rrs2")
    return Y.norm(x + rev[: len(t)], 0.85), "stereo"


def resident_breath():
    d = 2.2
    t = Y.t_axis(d)
    inhale = Y.bp(Y.white(d, "rrb"), 300, 1600) * np.clip(np.sin(np.pi * t / 1.0), 0, 1) * (t < 1.0)
    exhale = Y.bp(Y.white(d, "rrb2"), 200, 1100) * np.clip(np.sin(np.pi * (t - 1.1) / 1.0), 0, 1) * (t > 1.1)
    return Y.norm(Y.lp(inhale * 0.6 + exhale, 2000), 0.5)


sound("ambient.recursion", [("ambient/recursion", clock_room)], stream=True)
sound("ambient.recursion.additions",
      [("ambient/recursion_chime", chime), ("ambient/recursion_door", door_elsewhere),
       ("ambient/recursion_tv", muffled_tv)] + [(f"ambient/recursion_creak{i}", floor_creak(i)) for i in range(2)],
      subtitle="Something else in the house")
sound("event.recursion_door", [("event/recursion_door", door_pass)], subtitle="A door closes behind you")
sound("event.recursion_reset", [("event/recursion_reset", reset_sting)], subtitle="Everything is back where it was")
sound("entity.rec_resident.breath", [("entity/rec_resident_breath", resident_breath)], subtitle="Someone breathes")

level("recursion", fog="#2a2016", sky="#2a2016", ambient="ambient.recursion", additions="ambient.recursion.additions",
      additions_chance=0.0035, fixed_time=18000, height=32)
