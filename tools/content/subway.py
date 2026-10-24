"""Subway Station. Tiled platforms, tracks, iron columns. The train comes through at full speed and
never stops. Step down onto the tracks and it comes sooner."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, exit_block, hazard, pillar, custom, level, sound, \
    BLOCK_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import glyph_mask, centered, ink, brushed, wood, voice, speaker, chime

S = 32
LETTERS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ"


def subway_tiles(seed, lo, hi, grout="#8a8a80", rows=4, cols=2, gloss=True):
    yy, xx = np.mgrid[0:S, 0:S]
    th, tw = S // rows, S // cols
    off = ((yy // th) % 2) * (tw // 2)
    tid = (yy // th) * 7 + ((xx + off) // tw)
    r = T.rng("st", seed)
    tone = r.random(64)[tid % 64]
    img = T.ramp(tone * 0.5 + T.noise(seed + "n", sigma=1.2) * 0.5, lo, hi)
    ly, lx = yy % th, (xx + off) % tw
    edge_hi = (ly == 1) | (lx == 1)
    edge_lo = (ly == th - 1) | (lx == tw - 1)
    img = T.shade(img, 1 + edge_hi * 0.08 - edge_lo * 0.12)
    if gloss:
        # a soft specular streak across each tile
        spec = np.exp(-((lx - ly * 1.5 - tw * 0.3) ** 2) / 6.0) * (ly > 1) * (ly < th - 1)
        img = T.shade(img, 1 + spec * 0.08)
    gm = (ly == 0) | (lx == 0)
    g = T.ramp(T.noise(seed + "g", sigma=1), grout, "#5a5a52")
    img = T.blend(img, g, gm.astype(float))
    return img


def tile_white():
    img = subway_tiles("sw", "#d8d6cc", "#e8e6dc")
    img = T.stains(img, "sws", "#a8a28c", count=1, strength=0.15, size=8)
    # crazing
    img = T.cracks(img, "swc", "#b0aea4", 1, 0.35)
    return T.grain(img, "sw", 0.015)


def tile_band():
    img = subway_tiles("sb", "#1e4a3c", "#285a4a", grout="#6a7a70")
    img[1:3] = T.hexc("#c8a84a")
    img[-3:-1] = T.hexc("#c8a84a")
    return T.grain(img, "sb", 0.02)


def tile_base():
    img = subway_tiles("sbs", "#4a2a22", "#5a3428", grout="#3a3430")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    grime = np.clip((yy - 12) / 20, 0, 1) * T.noise("sbsg", sigma=2)
    return T.grain(T.shade(img, 1 - grime * 0.35), "sbs", 0.03)


def letter(i):
    def f():
        img = T.ramp(T.noise(f"sl{i}", sigma=1.5), "#d8d0b8", "#e4dcc4")
        img = T.border(img, "#1e4a3c", 3, sides="ns")
        img = T.border(img, "#c8a84a", 1, sides="ns")
        # mosaic grid
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.shade(img, 1 - (((yy % 3) == 0) | ((xx % 3) == 0)) * 0.05)
        if LETTERS[i] != " ":
            m = np.zeros((S, S))
            g = np.kron(glyph_mask(LETTERS[i]), np.ones((3, 3)))
            m[6:27, 8:23] = g
            img = ink(img, m, "#1a1a1a", 0.95)
        return T.grain(img, f"sl{i}", 0.02)
    return f


def platform():
    img = T.ramp(T.fbm("spl", octaves=4, base=4) * 0.6 + T.noise("spln", sigma=0.4) * 0.4, "#5e5e5a", "#72716c")
    chips = T.rng("splc").random((S, S))
    img = T.blend(img, T.solid("#9a988e"), (chips > 0.97).astype(float) * 0.6)
    img = T.blend(img, T.solid("#2a2a28"), (chips < 0.012).astype(float) * 0.8)  # old gum
    return T.grain(T.stains(img, "spls", "#3e3e3a", count=2, strength=0.3, size=8), "spl", 0.03)


def edge():
    img = platform()
    yy, xx = np.mgrid[0:S, 0:S]
    strip = yy >= 12
    yel = T.ramp(T.noise("sey", sigma=1), "#c8a020", "#dcb430")
    img = T.blend(img, yel, strip.astype(float))
    bumps = strip & (((xx % 6) - 3) ** 2 + (((yy - 12) % 6) - 3) ** 2 < 3)
    img = T.shade(img, 1 + bumps * 0.18 - (strip & ~bumps) * 0.0)
    worn = (T.noise("sew", sigma=1.5) > 0.7) & strip
    img = T.blend(img, T.solid("#6a6a62"), worn.astype(float) * 0.5)
    img[-2:] = T.hexc("#3a3a36")
    return T.grain(img, "se", 0.03)


def ballast():
    r = T.rng("sbal").random((S, S))
    from scipy.ndimage import gaussian_filter
    stones = gaussian_filter(r, 0.8, mode="wrap")
    stones = (stones - stones.min()) / (stones.max() - stones.min())
    img = T.ramp(stones, "#1e1c1a", "#3a3632", "#4e4a44")
    rust = T.noise("sbalr", sigma=2) > 0.7
    img = T.blend(img, T.solid("#4a3020"), rust.astype(float) * 0.3)
    return T.grain(img, "sbal", 0.05)


def sleeper():
    img = ballast()
    yy, xx = np.mgrid[0:S, 0:S]
    tie = (xx % 16) < 7
    w = wood("ssl", "#2e2418", "#3e3020", plank=32, vertical=True)
    img = T.blend(img, w, tie.astype(float))
    return T.grain(img, "ssl", 0.04)


def steel():
    img = brushed("ssr", "#4a4844", "#6a6660", vertical=False)
    img = T.blend(img, T.solid("#5a3a22"), (T.noise("ssrr", sigma=1.5) > 0.6).astype(float) * 0.4)
    return img


def rail_top():
    img = brushed("srt", "#8a8a88", "#c0c0bc", vertical=False)
    return img


def third_rail_cover():
    img = T.ramp(T.noise("s3r", sigma=1.5), "#8a7a52", "#9e8e62")
    yy, xx = np.mgrid[0:S, 0:S]
    img = ink(img, centered("DANGER", y=12, scale=1, spacing=0), "#7a1010", 0.9)
    return T.grain(img, "s3r", 0.05)


def column():
    img = T.ramp(T.noise("sco", sigma=1.5), "#2a3e34", "#344a3e")
    yy, xx = np.mgrid[0:S, 0:S]
    flange = (xx < 4) | (xx > 27)
    img = T.shade(img, 1 + flange * 0.15)
    img = T.shade(img, 1 - ((xx == 4) | (xx == 27)) * 0.4)
    rivets = flange & ((yy % 8) == 4) & ((xx == 2) | (xx == 29))
    img = T.blend(img, T.solid("#5a6a60"), rivets.astype(float))
    chips = T.noise("scoc", sigma=0.8) > 0.82
    img = T.blend(img, T.solid("#4a2a1a"), chips.astype(float) * 0.6)
    return T.grain(img, "sco", 0.03)


def column_top():
    return T.grain(T.ramp(T.noise("scot", sigma=1.5), "#2a3e34", "#344a3e"), "scot", 0.03)


def ceiling(pipes=False):
    img = T.ramp(T.fbm("sce" + str(pipes), octaves=3, base=4), "#3a3a36", "#46463f")
    img = T.stains(img, "sces" + str(pipes), "#22221e", count=2, strength=0.4, size=8)
    if pipes:
        yy, xx = np.mgrid[0:S, 0:S]
        for cy, w, col in ((6, 3, "#5a5248"), (14, 2, "#3a2a22"), (22, 4, "#4a4a46")):
            p = np.abs(yy - cy) <= w / 2
            shade = 1 + (cy - yy) / (w + 1) * 0.3
            img = T.blend(img, T.shade(T.solid(col), shade), p.astype(float))
    return T.grain(img, "sce" + str(pipes), 0.04)


def tunnel_wall():
    img = T.ramp(T.fbm("stw", octaves=4, base=5), "#262624", "#323230")
    yy, xx = np.mgrid[0:S, 0:S]
    soot = np.clip(1 - yy / 32, 0, 1) * 0.25
    img = T.shade(img, 1 - soot)
    for cy in (9, 12):
        img = T.blend(img, T.solid("#141412"), (np.abs(yy - cy - np.sin(xx / 6) * 0.6) < 0.8).astype(float))
    return T.grain(T.stains(img, "stws", "#1a1a18", count=3, strength=0.35, size=7), "stw", 0.05)


def strip(on=True):
    img = T.solid("#f2fff6") if on else T.solid("#6a6e6a")
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        img = T.shade(img, 1 - (np.abs(yy - 16) > 10) * 0.06)
    return T.border(img, "#7a807a", 2)


def housing():
    return T.grain(T.ramp(T.noise("sho", sigma=1), "#5a605c", "#6a706c"), "shou", 0.03)


def bench_wood():
    return wood("sbw", "#5a3a20", "#764c2a", plank=8)


def bin_metal():
    img = brushed("sbn", "#3a4a42", "#4a5a52")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 4) == 0) * 0.25)
    return img


def route_map():
    img = T.ramp(T.noise("smp", sigma=2), "#e8e4d4", "#f0ece0")
    img = T.border(img, "#2a2a28", 2)
    yy, xx = np.mgrid[0:S, 0:S]
    # a green line that runs off the right edge and comes back in from the left, forever
    line = np.abs(yy - (14 + np.round(np.sin(xx / 32 * 2 * np.pi) * 5))) <= 1
    img = T.blend(img, T.solid("#1e7a4a"), (line & (xx > 2) & (xx < 29)).astype(float))
    for k, x in enumerate((6, 12, 18, 24)):
        y = int(14 + round(np.sin(x / 32 * 2 * np.pi) * 5))
        img[y - 1:y + 2, x - 1:x + 2] = T.hexc("#f0ece0")
        img[y, x] = T.hexc("#1a1a1a")
    # YOU ARE HERE, twice
    for x in (12, 24):
        y = int(14 + round(np.sin(x / 32 * 2 * np.pi) * 5))
        img[y - 1:y + 2, x - 1:x + 2] = T.hexc("#d02020")
    img = ink(img, centered("LINE", y=24, scale=1), "#2a2a28")
    return T.grain(img, "smp", 0.02)


def board(text):
    def f():
        img = T.solid("#0a0a08")
        yy, xx = np.mgrid[0:S, 0:S]
        grid = ((xx % 2) == 0) & ((yy % 2) == 0)
        img = T.blend(img, T.solid("#1a1408"), grid.astype(float))
        m = centered(text, y=12, scale=1, spacing=1 if len(text) <= 5 else 0)
        img = T.blend(img, T.solid("#ff9a20"), m * 0.95)
        img = T.border(img, "#2a2a28", 2)
        return img
    return f


def shutter():
    img = T.ramp(T.noise("ssh", sigma=2), "#6a6a64", "#7a7a72")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    img = T.shade(img, 1 + np.sin(yy / 2.0 * np.pi) * 0.12)
    img = T.blend(img, T.solid("#5a3a22"), (T.noise("sshr", sigma=1.5) > 0.7).astype(float) * 0.35)
    return T.grain(img, "ssh", 0.04)


def service_door():
    img = T.ramp(T.noise("ssd", sigma=2), "#4e5250", "#5a5e5a")
    img = T.border(img, "#2a2c2a", 2)
    img = T.rect(img, 3, 4, 29, 13, "#d8c020")
    img = ink(img, centered("NO", y=5, scale=1), "#1a1a1a")
    img = T.rect(img, 23, 18, 27, 21, "#a8a8a0")
    return T.grain(T.stains(img, "ssds", "#2a2a26", count=2, strength=0.3, size=6), "ssd", 0.05)


def service_door_top():
    img = T.ramp(T.noise("ssdt", sigma=2), "#4e5250", "#5a5e5a")
    img = T.border(img, "#2a2c2a", 2)
    img = T.rect(img, 3, 18, 29, 28, "#d8c020")
    img = ink(img, centered("ENTRY", y=20, scale=1, spacing=0), "#1a1a1a")
    return T.grain(img, "ssdt", 0.05)


def signal(color):
    def f():
        img = T.solid("#141412")
        yy, xx = np.mgrid[0:S, 0:S]
        d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
        img = T.blend(img, T.solid("#2a2a28"), (d < 11).astype(float))
        img = T.blend(img, T.ramp(np.clip(1 - d / 8, 0, 1), color, "#ffffff"), (d < 7).astype(float))
        return img
    return f


def exit_up():
    img = T.solid("#020202")
    return T.grain(T.blend(img, T.solid("#0a0c0a"), T.noise("seu", sigma=3) * 0.5), "seu", 0.02)


# --- the train

def train_body():
    img = brushed("stb", "#9a9c98", "#b8bab4", vertical=False)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, T.solid("#2a6a4a"), ((yy >= 10) & (yy <= 14)).astype(float))
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.3)
    return T.grain(img, "stb", 0.02)


def train_window(fig=False):
    yy, xx = np.mgrid[0:S, 0:S]
    img = brushed("stw2", "#9a9c98", "#b8bab4", vertical=False)
    win = (yy >= 4) & (yy <= 27) & (xx >= 3) & (xx <= 28)
    inside = T.ramp(np.clip(1 - np.abs(yy - 12) / 20, 0, 1), "#d8d0a8", "#fff8dc")
    img = T.blend(img, inside, win.astype(float))
    if fig:
        # someone standing very still, facing out
        head = ((xx - 16) ** 2 + (yy - 10) ** 2) < 12
        body = (np.abs(xx - 16) < 6 - (yy < 16) * 2) & (yy >= 13)
        img = T.blend(img, T.solid("#1a1a18"), ((head | body) & win).astype(float))
    else:
        bar = win & ((yy == 9) | (np.abs(xx - 8) < 1) | (np.abs(xx - 23) < 1)) & (yy < 12)
        img = T.blend(img, T.solid("#8a8a80"), bar.astype(float) * 0.7)
    img = T.blend(img, T.solid("#3a3a38"), (win & ((xx == 3) | (xx == 28) | (yy == 4) | (yy == 27))).astype(float))
    return img


def train_roof():
    img = brushed("str", "#5a5c58", "#707270", vertical=False)
    yy, xx = np.mgrid[0:S, 0:S]
    return T.shade(img, 1 - ((yy % 8) == 0) * 0.2)


def train_front():
    img = brushed("stf", "#9a9c98", "#b8bab4", vertical=True)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, T.solid("#2a6a4a"), ((yy >= 10) & (yy <= 14)).astype(float))
    for cx in (7, 24):
        d = np.sqrt((xx - cx) ** 2 + (yy - 22) ** 2)
        img = T.blend(img, T.solid("#fffbe8"), (d < 3).astype(float))
    win = (yy >= 2) & (yy <= 8) & (xx >= 4) & (xx <= 27)
    img = T.blend(img, T.solid("#141414"), win.astype(float))
    return img


tex("sub_tile_white", tile_white)
tex("sub_tile_band", tile_band)
tex("sub_tile_base", tile_base)
for i in range(len(LETTERS)):
    tex(f"sub_letter_{i}", letter(i))
tex("sub_platform", platform)
tex("sub_edge", edge)
tex("sub_edge_n", lambda: edge()[::-1].copy())
tex("sub_ballast", ballast)
tex("sub_sleeper", sleeper)
tex("sub_steel", steel)
tex("sub_rail_top", rail_top)
tex("sub_third_rail", third_rail_cover)
tex("sub_column", column)
tex("sub_column_top", column_top)
tex("sub_ceiling", lambda: ceiling(False))
tex("sub_ceiling_pipes", lambda: ceiling(True))
tex("sub_tunnel_wall", tunnel_wall)
tex("sub_strip", lambda: strip(True))
tex("sub_strip_off", lambda: strip(False))
tex("sub_housing", housing)
tex("sub_bench", bench_wood)
tex("sub_bin", bin_metal)
tex("sub_map", route_map)
for nm, txt in (("next", "NEXT"), ("min", "--MIN"), ("not", "NOT IN"), ("service", "USE"),
                ("do", "DO NOT"), ("board", "BOARD")):
    tex(f"sub_board_{nm}", board(txt))
tex("sub_shutter", shutter)
tex("sub_service_door", service_door)
tex("sub_service_door_top", service_door_top)
tex("sub_signal_red", signal("#e02818"))
tex("sub_signal_green", signal("#30d860"))
tex("sub_exit_up", exit_up)
tex("sub_train_body", train_body)
tex("sub_train_window", lambda: train_window(False))
tex("sub_train_window_fig", lambda: train_window(True))
tex("sub_train_roof", train_roof)
tex("sub_train_front", train_front)

cube("sub_tile_white", "sub_tile_white", display="Subway Tile", sound="tile")
cube("sub_tile_band", "sub_tile_band", display="Banded Subway Tile", sound="tile")
cube("sub_tile_base", "sub_tile_base", display="Dark Subway Tile", sound="tile")
cube("sub_platform", "sub_platform", display="Platform Concrete")
cube_faces("sub_edge", top="sub_edge", bottom="sub_platform", side="sub_tile_base", display="Platform Edge")
cube_faces("sub_edge_n", top="sub_edge_n", bottom="sub_platform", side="sub_tile_base", display="Platform Edge")
cube("sub_ballast", "sub_ballast", display="Track Ballast", sound="gravel")
cube("sub_sleeper", "sub_sleeper", display="Sleepers", sound="gravel")
pillar("sub_column", "sub_column_top", "sub_column", display="Iron Column", sound="metal")
cube("sub_ceiling", "sub_ceiling", display="Station Ceiling", flat=True)
cube("sub_ceiling_pipes", "sub_ceiling_pipes", display="Station Ceiling", flat=True)
cube("sub_tunnel_wall", "sub_tunnel_wall", display="Tunnel Wall")
cube("sub_shutter", "sub_shutter", display="Roller Shutter", sound="metal")
cube("sub_map", "sub_map", display="Line Map", sound="tile")

# running rail: a steel bar along x, on a tie plate
shaped("sub_rail", [(0, 0, 6, 16, 1, 10, "steel"), (0, 1, 7, 16, 3, 9, {"all": "steel", "up": "top"})],
       {"steel": "sub_steel", "top": "sub_rail_top"}, display="Running Rail", sound="metal", unbreakable=True)
# the third rail: kills on touch
hazard("sub_third_rail", "sub_third_rail", damage=9.0, solid=False, display="Third Rail", render="cutout")
BLOCK_MODELS["sub_third_rail"] = {"parent": "minecraft:block/block",
                                  "textures": {"c": "noclip:block/sub_third_rail", "s": "noclip:block/sub_steel",
                                               "particle": "noclip:block/sub_third_rail"},
                                  "elements": [
                                      {"from": [0, 0, 5], "to": [16, 5, 11], "faces": {f: {"texture": "#s"} for f in
                                                                                       ("north", "south", "up", "down", "east", "west")}},
                                      {"from": [0, 5, 4], "to": [16, 7, 12], "faces": {f: {"texture": "#c"} for f in
                                                                                       ("north", "south", "up", "down", "east", "west")}}]}

shaped("sub_strip", [(1, 13, 6, 15, 16, 10, {"all": "housing", "down": "tube"}), (15, 13, 6, 16, 16, 10, "housing"),
                     (0, 13, 6, 1, 16, 10, "housing")],
       {"housing": "sub_housing", "tube": "sub_strip"}, display="Platform Strip Light", light=14, emissive=True,
       unbreakable=True, collides=False, sound="glass", render="solid")
shaped("sub_strip_off", [(1, 13, 6, 15, 16, 10, {"all": "housing", "down": "tube"})],
       {"housing": "sub_housing", "tube": "sub_strip_off"}, display="Dead Platform Light", unbreakable=True,
       collides=False, sound="glass", render="solid")
shaped("sub_bench", [
    (1, 6, 3, 15, 7, 13, "wood"), (1, 7, 12, 15, 15, 13, "wood"),
    (2, 0, 4, 3, 6, 12, "metal"), (13, 0, 4, 14, 6, 12, "metal"),
], {"wood": "sub_bench", "metal": "sub_bin"}, display="Platform Bench", sound="wood", unbreakable=True)
shaped("sub_bin", [(4, 0, 4, 12, 12, 12, "m"), (3, 12, 3, 13, 13, 13, "m")], {"m": "sub_bin"},
       display="Litter Bin", sound="metal", unbreakable=True)

# dot-matrix boards hang in pairs; each face carries half a message
def board_block(name, north, south):
    shaped(name, [(0, 0, 7, 16, 9, 9, {"all": "frame", "north": "n", "south": "s"}), (7, 9, 7.5, 9, 16, 8.5, "frame")],
           {"n": north, "s": south, "frame": "sub_housing"}, display="Departure Board", sound="metal",
           unbreakable=True, collides=False, emissive=True, render="solid")


board_block("sub_board_a1", "sub_board_min", "sub_board_next")
board_block("sub_board_a2", "sub_board_next", "sub_board_min")
board_block("sub_board_b1", "sub_board_service", "sub_board_not")
board_block("sub_board_b2", "sub_board_not", "sub_board_service")
board_block("sub_board_c1", "sub_board_board", "sub_board_do")
board_block("sub_board_c2", "sub_board_do", "sub_board_board")

flicker_light("sub_signal_red", "sub_signal_red", "sub_signal_red", "sub_tunnel_wall", light=5, flicker=0.0,
              face="all", display="Tunnel Signal")
flicker_light("sub_signal_green", "sub_signal_green", "sub_signal_green", "sub_tunnel_wall", light=5, flicker=0.0,
              face="all", display="Tunnel Signal")
exit_block("sub_exit_up", "sub_exit_up", "ASCEND", display="Way Out")
exit_block("sub_service_door", "sub_service_door", "DOOR", trigger="USE", solid=True, display="Service Door",
           sound="metal")
cube("sub_service_door_top", "sub_service_door_top", display="Service Door", sound="metal")

# station name tiles: one block, 27 glyphs (space + A-Z)
custom("sub_letter", "com.noclip.block.SubwayLetterBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/sub_letter_0"}},
       display="Station Name Tile", unbreakable=True, sound="tile")
for i in range(len(LETTERS)):
    BLOCK_MODELS[f"sub_letter_{i}"] = {"parent": "minecraft:block/cube_all", "textures": {"all": f"noclip:block/sub_letter_{i}"}}
BLOCKSTATES["sub_letter"] = {"variants": {f"letter={i}": {"model": f"noclip:block/sub_letter_{i}"} for i in range(len(LETTERS))}}

# the train: full cubes that do not block light, so it can move through the station cheaply
for nm, faces in (
        ("sub_train_body", {"all": "body", "up": "roof"}),
        ("sub_train_window", {"all": "body", "north": "win", "south": "win", "up": "roof"}),
        ("sub_train_window_fig", {"all": "body", "north": "fig", "south": "win", "up": "roof"}),
        ("sub_train_roof", {"all": "roof"}),
        ("sub_train_front", {"all": "body", "east": "front", "west": "front", "up": "roof"})):
    shaped(nm, [(0, 0, 0, 16, 16, 16, faces)],
           {"body": "sub_train_body", "win": "sub_train_window", "fig": "sub_train_window_fig",
            "roof": "sub_train_roof", "front": "sub_train_front"},
           display="Train", sound="metal", unbreakable=True, render="cutout", emissive=(nm != "sub_train_roof") or None)

# ------------------------------------------------------------------ sound


def tunnel_air():
    d = 26.0
    t = Y.t_axis(d)
    wind = Y.lp(Y.pink(d, "sta"), 260) * (0.7 + 0.3 * np.sin(2 * np.pi * t / 13))
    rumble = Y.lp(Y.brown(d, "str"), 70) * (0.4 + 0.6 * np.exp(-((t - 17) ** 2) / 8))
    buzz = Y.hum(d, 50.0, "stbz", buzz=0.3, harmonics=(2, 4, 6)) * 0.05
    x = Y.reverb(wind + rumble, 3.0, 0.5, "starv")[: len(t)] + Y.hp(buzz, 300)
    x = Y.loopable(Y.norm(x, 0.5), 1.5)
    return Y.stereoize(x, 0.002), "stereo"


def screech():
    d = 3.5
    t = Y.t_axis(d)
    f0 = 2600 + 400 * np.sin(2 * np.pi * 0.7 * t) + 120 * np.sin(2 * np.pi * 9 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * t / d) ** 2
    x += 0.5 * np.sin(2 * np.pi * np.cumsum(f0 * 1.51) / Y.SR) * np.sin(np.pi * t / d) ** 3
    return Y.norm(Y.reverb(Y.lp(x, 5000), 4.0, 0.8, "scr"), 0.25)


def announcement(i):
    def f():
        out = np.zeros(int(6.5 * Y.SR))
        Y.place(out, chime([659.3, 523.3, 392.0], dur=2.4, decay=1.8) * 0.5, 0.0)
        Y.place(out, voice(f"ann{i}", 2.6 + i * 0.5, pitch=150 - i * 15, breath=0.12), 1.8)
        out = speaker(out, 400, 3400)
        return Y.norm(Y.reverb(out, 3.0, 0.65, f"annrv{i}"), 0.45)
    return f


def far_train():
    d = 9.0
    t = Y.t_axis(d)
    env = np.exp(-((t - 4.5) ** 2) / 5.0)
    x = Y.lp(Y.brown(d, "ftr"), 160) * env
    clack = np.zeros_like(t)
    for k in range(18):
        at = 2.0 + k * 0.28
        Y.place(clack, Y.impact(f"ftc{k}", 0.2), at, 0.15)
    return Y.norm(Y.reverb(x + Y.lp(clack, 900) * env, 3.0, 0.6, "ftrv"), 0.4)


def drip():
    out = np.zeros(int(2.5 * Y.SR))
    n = int(0.1 * Y.SR)
    tt = np.arange(n) / Y.SR
    plink = np.sin(2 * np.pi * np.cumsum(1700 - tt * 4000) / Y.SR) * np.exp(-tt * 50)
    Y.place(out, plink, 0.1)
    return Y.norm(Y.reverb(out, 2.2, 0.7, "sdrip"), 0.35)


def train_pass():
    """The full pass: 5 s of approach, the horn, ~1.3 s of carriages, then it is gone into the tunnel."""
    d = 11.0
    n = int(d * Y.SR)
    t = Y.t_axis(d)
    peak = 5.2
    env = np.where(t < peak, np.exp((t - peak) * 0.9), np.exp(-(t - peak - 1.2).clip(0) * 1.1))
    roar = Y.lp(Y.brown(d, "tpr"), 400) * 1.2 + Y.bp(Y.white(d, "tpw"), 300, 5000) * 0.5 * env ** 2
    # wheels over rail joints: a doubled clack per carriage at the speed of the train
    clack = np.zeros(n)
    for k in range(40):
        at = peak - 1.0 + k * 0.09
        if at < d - 0.2:
            Y.place(clack, Y.impact(f"tpc{k}", 0.15), at, 0.8)
    rush = Y.bp(Y.white(d, "tpwind"), 800, 9000) * np.exp(-((t - peak - 0.6) ** 2) / 0.4) * 0.8
    horn = np.zeros(n)
    th = Y.t_axis(1.4)
    h = (np.sign(np.sin(2 * np.pi * 311 * th)) + np.sign(np.sin(2 * np.pi * 370 * th))) * 0.25
    h = Y.lp(h, 2500) * Y.env(len(th), 0.05, 0.3)
    # doppler: the horn bends down as it passes
    Y.place(horn, h, peak - 1.3, 0.5)
    x = roar * env + clack * env + rush + horn
    x = Y.reverb(x, 2.5, 0.4, "tprv")[:n]
    x = Y.norm(x, 0.95)
    return Y.stereoize(x, 0.004), "stereo"


def rumble_warn():
    d = 5.0
    t = Y.t_axis(d)
    x = Y.lp(Y.brown(d, "rw"), 90) * np.clip(t / d, 0, 1) ** 1.5
    rails = Y.bp(Y.white(d, "rwr"), 3000, 8000) * 0.06 * (t / d) ** 2 * (0.5 + 0.5 * np.sin(2 * np.pi * 23 * t))
    return Y.norm(Y.reverb(x + rails, 2.5, 0.5, "rwrv"), 0.7)


sound("ambient.subway", [("ambient/subway", tunnel_air)], stream=True)
sound("ambient.subway.additions",
      [("ambient/subway_screech", screech), ("ambient/subway_far_train", far_train), ("ambient/subway_drip", drip)] +
      [(f"ambient/subway_announce{i}", announcement(i)) for i in range(3)],
      subtitle="Somewhere in the station")
sound("event.subway_train", [("event/subway_train", train_pass)], subtitle="A train passes at full speed")
sound("event.subway_rumble", [("event/subway_rumble", rumble_warn)], subtitle="The rails start to sing")

level("subway", fog="#161a18", sky="#161a18", ambient="ambient.subway", additions="ambient.subway.additions",
      additions_chance=0.004, fixed_time=18000, height=32)
