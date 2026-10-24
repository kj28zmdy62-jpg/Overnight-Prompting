"""The Hub - a hotel-like hallway of numbered doors, each one opening onto a different level."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, custom, flicker_light
import tex as T
import synth as Y
from content.deepend_font import draw, text_width
from content.deepend_audio import wrapverb, fit

S = 32
yy, xx = np.mgrid[0:S, 0:S]

# Every level a door can lead to, with the label on its brass plaque.
PLAQUES = [
    ("level_0", "0"), ("level_1", "1"), ("level_2", "2"), ("level_3", "3"), ("level_4", "4"), ("level_5", "5"),
    ("level_6", "6"), ("level_7", "7"), ("level_8", "8"), ("level_9", "9"), ("level_10", "10"), ("level_11", "11"),
    ("level_13", "13"), ("level_37", "37"), ("level_94", "94"), ("level_188", "188"), ("level_fun", "=)"),
    ("level_run", "!"), ("manila_room", "MANILA"), ("airport", "AIRPORT"),
    ("dead_mall", "MALL"), ("waiting_room", "WAITING"), ("school", "SCHOOL"), ("hospital", "HOSPITAL"),
    ("supermarket", "MARKET"), ("parking_garage", "GARAGE"), ("playground", "PLAY"), ("water_park", "SLIDES"),
    ("arcade", "ARCADE"), ("bowling_alley", "BOWLING"), ("movie_theater", "CINEMA"), ("laundromat", "LAUNDRY"),
    ("motel", "MOTEL"), ("daycare", "DAYCARE"), ("museum", "MUSEUM"), ("archive", "ARCHIVE"), ("subway", "SUBWAY"),
    ("lift_lobby", "LIFTS"), ("stairwell", "STAIRS"), ("restrooms", "WC"), ("locker_rooms", "LOCKERS"),
    ("ballroom", "BALLROOM"), ("bingo_hall", "BINGO"), ("convention_center", "EXPO"), ("call_center", "CALLS"),
    ("server_farm", "SERVERS"), ("storm_drains", "DRAINS"), ("boiler_room", "BOILER"), ("cold_storage", "COLD"),
    ("assembly_line", "FACTORY"), ("container_yard", "YARD"), ("bunker", "BUNKER"), ("mine_shaft", "MINE"),
    ("water_treatment", "TANKS"), ("steel_mill", "STEEL"), ("elevator_shaft", "SHAFT"), ("air_vents", "VENTS"),
    ("sewers", "SEWERS"), ("power_plant", "POWER"), ("dam", "DAM"),
    ("static_fields", "STATIC"), ("the_signal", "SIGNAL"), ("endless_highway", "HIGHWAY"), ("gas_station", "GAS"),
    ("parking_lot", "PARKING"), ("golden_hour", "GOLDEN"), ("silent_beach", "BEACH"), ("pole_forest", "POLES"),
    ("snow_plains", "SNOW"), ("golf_course", "GOLF"), ("salt_flats", "SALT"), ("foggy_playground", "FOG"),
    ("the_dock", "DOCK"), ("sky_rooms", "SKY"), ("overgrown_lobby", "GROWN"), ("rain_city", "RAIN"),
    ("construction_site", "SITE"), ("drive_in", "DRIVE-IN"), ("rooftops", "ROOFS"), ("carnival", "CARNIVAL"),
    ("recursion", "AGAIN"), ("inverted_lobby", "INVERTED"), ("zero_zero", "0.0"), ("mirror_level", "MIRROR"),
    ("missing_textures", "MISSING"), ("the_grid", "GRID"), ("dollhouse", "DOLLS"), ("long_hallway", "HALLWAY"),
    ("flooded_lobby", "FLOODED"), ("red_room", "RED"), ("room_of_doors", "DOORS"), ("loading_screen", "LOADING"),
    ("gravity_rooms", "GRAVITY"), ("photo_gallery", "PHOTOS"), ("glitch_level", "???"),
    ("the_abyss", "ABYSS"), ("the_void", "VOID"), ("frontrooms", "HOME"), ("unmarked", ""),
]


def damask(seed, lo, hi, motif):
    img = T.ramp(T.fbm(seed, octaves=3, base=6) * 0.5 + 0.25, lo, hi)
    # a repeating fleur-ish motif, embossed
    u = (xx % 16) - 7.5
    v = (yy % 16) - 7.5
    shape = (np.abs(u) * 1.4 + np.abs(v) < 6) & ~(np.abs(u) * 1.4 + np.abs(v) < 3.2)
    shape |= (np.abs(u) < 0.8) & (np.abs(v) < 7)
    img = T.blend(img, T.solid(motif), shape.astype(float) * 0.55)
    return T.grain(T.stains(img, seed, "#20140c", count=1, strength=0.15, size=8), seed, 0.03)


def wallpaper():
    return damask("hubw", "#3a1a1e", "#4a2228", "#6a3a30")


def wainscot():
    img = T.ramp(T.fbm("hubwain", octaves=3, base=3), "#2a1a10", "#3a2414")
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.35)
    img = T.rect(img, 0, 0, 32, 3, "#4a3220")
    img[3] = T.hexc("#1a1008")
    inset = ((xx % 16 > 2) & (xx % 16 < 14) & (yy > 7) & (yy < 28))
    img = T.shade(img, 1 - inset * 0.12)
    return T.grain(img, "hubwain", 0.03)


def plaque_tex(label):
    def f():
        img = wallpaper()
        if label == "":
            # the plaque has been unscrewed. Four holes and a clean rectangle.
            img = T.rect(img, 6, 10, 26, 22, "#5a3236")
            for (x, y) in ((7, 11), (24, 11), (7, 20), (24, 20)):
                img[y, x] = T.hexc("#0a0606")
            return img
        sc = 2 if len(label) <= 3 else 1
        tw = text_width(label, sc)
        w = max(14, tw + 6)
        x0 = (S - w) // 2
        y0, y1 = (9, 23) if sc == 2 else (11, 21)
        brass = T.ramp(T.noise("brass" + label, sigma=2, shape=(S, S)) * 0.5 + yy / 64.0, "#8a6a2a", "#c8a050", "#e8c878")
        m = np.zeros((S, S))
        m[y0:y1, x0:x0 + w] = 1
        img = T.blend(img, brass, m)
        img[y0, x0:x0 + w] = T.hexc("#f0d890")
        img[y1 - 1, x0:x0 + w] = T.hexc("#5a4018")
        img = draw(img, label, "#2a1a08", scale=sc, y=y0 + (y1 - y0 - 5 * sc) // 2)
        return img
    return f


def door_tex(style, top):
    def f():
        cols = {"oak": ("#3a2414", "#4a2e1a"), "white": ("#c8c2b4", "#d8d2c4"), "green": ("#1e3a2a", "#2a4a36"),
                "steel": ("#4a5258", "#5a6268")}
        lo, hi = cols[style]
        img = T.ramp(T.fbm("hubdoor" + style, octaves=3, base=3), lo, hi)
        img = T.border(img, "#140a06" if style != "white" else "#8a8478", 1)
        # raised panels
        if top:
            for (x0, x1) in ((5, 14), (18, 27)):
                img = T.shade(img, 1 - ((xx >= x0) & (xx < x1) & (yy >= 4) & (yy < 30)) * 0.12)
                img[4, x0:x1] = img[4, x0:x1] * 1.25
        else:
            for (x0, x1) in ((5, 14), (18, 27)):
                img = T.shade(img, 1 - ((xx >= x0) & (xx < x1) & (yy >= 2) & (yy < 24)) * 0.12)
            img = T.rect(img, 23, 2, 27, 5, "#c8a050")       # brass knob
            img[2, 23:27] = T.hexc("#f0d890")
            img = T.rect(img, 24, 6, 26, 8, "#1a1008")       # keyhole
        return T.grain(np.clip(img, 0, 1), "hubdoor" + style + str(top), 0.03)
    return f


def white_door(top):
    def f():
        img = T.ramp(T.noise("hubwhite" + str(top), sigma=3), "#f4f4f0", "#ffffff")
        img = T.border(img, "#e8e8e4", 1)
        if not top:
            img = T.rect(img, 23, 2, 27, 5, "#e0e0dc")
        return img
    return f


def carpet():
    img = T.ramp(T.fbm("hubcarp", octaves=3, base=4) * 0.6 + T.noise("hubcf", sigma=0.5) * 0.4, "#5a0e14", "#6e1418")
    diamonds = (np.abs((xx % 16) - 7.5) + np.abs((yy % 16) - 7.5)) < 2.5
    img = T.blend(img, T.solid("#a0782a"), diamonds.astype(float) * 0.6)
    return T.grain(img, "hubcarp", 0.04)


def carpet_edge():
    img = carpet()
    img = T.rect(img, 0, 0, 32, 4, "#a0782a")
    img = T.rect(img, 0, 4, 32, 5, "#3a0a0c")
    return img


def parquet():
    img = T.ramp(T.fbm("hubpar", octaves=3, base=3), "#3a2414", "#4e321c")
    blocks = ((xx // 8 + yy // 16) % 2 == 0)
    grain_dir = np.where(blocks, np.sin(yy * 1.7) * 0.05, np.sin(xx * 1.7) * 0.05)
    img = T.shade(img, 1 + grain_dir)
    img = T.shade(img, 1 - (((xx % 8) == 0) | ((yy % 16) == 0)) * 0.25)
    return T.grain(img, "hubpar", 0.03)


def ceiling():
    img = T.ramp(T.fbm("hubceil", octaves=2, base=4), "#c8bca4", "#d8ccb4")
    img = T.border(img, "#a89c84", 2)
    img = T.border(img, "#ddd2bc", 1)
    return T.grain(img, "hubceil", 0.02)


def shade_tex():
    d = np.abs(xx - 15.5) / 16
    return T.ramp(1 - d, "#c08040", "#f0c070", "#fff0c8")


def brass():
    return T.grain(T.ramp(T.noise("hubbrass", sigma=1), "#8a6a2a", "#c8a050"), "hubbrass", 0.03)


tex("hub_wallpaper", wallpaper)
tex("hub_wainscot", wainscot)
for lid, label in PLAQUES:
    tex(f"hub_plaque_{lid}", plaque_tex(label))
for style in ("oak", "white", "green", "steel"):
    tex(f"hub_door_{style}", door_tex(style, False))
    tex(f"hub_door_{style}_top", door_tex(style, True))
tex("hub_white_door", white_door(False))
tex("hub_white_door_top", white_door(True))
tex("hub_carpet", carpet)
tex("hub_carpet_edge", carpet_edge)
tex("hub_parquet", parquet)
tex("hub_ceiling", ceiling)
tex("hub_lampshade", shade_tex)
tex("hub_brass", brass)

cube("hub_wallpaper", "hub_wallpaper", display="Hub Wallpaper", sound="wool")
cube("hub_wainscot", "hub_wainscot", display="Hub Wainscot", sound="wood")
for lid, label in PLAQUES:
    cube(f"hub_plaque_{lid}", f"hub_plaque_{lid}", display=f"Door Plaque ({label or 'blank'})", sound="wool")
for style in ("oak", "white", "green", "steel"):
    for top in (False, True):
        name = f"hub_door_{style}" + ("_top" if top else "")
        custom(name, "com.noclip.block.HubDoorBlock",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"noclip:block/{name}"}},
               display="Hub Door", unbreakable=True, sound="wood" if style != "steel" else "metal")
exit_block("hub_white_door", "hub_white_door", "SECRET", trigger="USE", solid=True, display="A White Door",
           light=6, emissive=True, sound="wood")
exit_block("hub_white_door_top", "hub_white_door_top", "SECRET", trigger="USE", solid=True, display="A White Door",
           light=6, emissive=True, sound="wood")
cube("hub_carpet_runner", "hub_carpet", display="Hub Carpet Runner", sound="wool")
cube("hub_carpet_runner_edge", "hub_carpet_edge", display="Hub Carpet Border", sound="wool")
cube("hub_parquet", "hub_parquet", display="Hub Parquet", sound="wood")
cube("hub_ceiling", "hub_ceiling", display="Hub Ceiling", sound="wool", flat=True)
shaped("hub_sconce", [
    (6, 4, 14, 10, 6, 16, "brass"), (7, 6, 12, 9, 7, 15, "brass"),
    (5, 7, 10, 11, 12, 15, {"all": "shade", "up": None, "down": None}),
], {"brass": "hub_brass", "shade": "hub_lampshade"}, display="Wall Sconce", light=11, emissive=True,
    collides=False, unbreakable=True, sound="metal")
shaped("hub_pendant", [
    (7.5, 6, 7.5, 8.5, 16, 8.5, "brass"), (4, 1, 4, 12, 6, 12, {"all": "shade", "up": "brass"}),
], {"brass": "hub_brass", "shade": "hub_lampshade"}, display="Pendant Lamp", light=13, emissive=True,
    collides=False, unbreakable=True, sound="metal")

# ------------------------------------------------------------------ sound


def hub_air():
    """A long carpeted hall, and through every door, faintly, a different level."""
    d = 30.0
    t = Y.t_axis(d)
    n = len(t)
    hush = Y.lp(Y.pink(d, "hubhush"), 500) * 0.25
    lobby = Y.lp(Y.hum(d, 60.0, "hub60", buzz=0.2), 900) * 0.07 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 15.0) ** 2)
    rain = Y.bp(Y.white(d, "hubrain"), 1500, 5000) * 0.03 * np.sin(2 * np.pi * t / 30.0 + 1) ** 2
    party = np.zeros(n)
    for k, f0 in enumerate((392.0, 440.0, 392.0, 523.25, 493.88)):
        tone = np.sin(2 * np.pi * f0 * Y.t_axis(1.2)) * np.exp(-Y.t_axis(1.2) * 3)
        Y.place(party, tone, 18.0 + k * 0.6, 0.06)
    alarm = np.sign(np.sin(2 * np.pi * np.cumsum(600 + 300 * ((t % 2) / 2)) / Y.SR)) * 0.02 * ((t > 6) & (t < 11))
    x = wrapverb(hush + lobby + Y.lp(rain + party + Y.lp(alarm, 1500), 1200), 2.5, 0.5, "hubair", damp=2500)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.4), 1.5), 0.0025), "stereo"


def door_close():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.impact("hubdc", 0.7), 0.0)
    latch = Y.bp(Y.white(0.08, "hubl"), 2000, 7000)[: int(0.08 * Y.SR)] * np.exp(-np.arange(int(0.08 * Y.SR)) / Y.SR * 60)
    Y.place(out, latch, 0.04, 0.6)
    return Y.reverb(Y.lp(out, 2500), 3.0, 0.7, "hubdc")


def knock_back():
    out = np.zeros(int(2.5 * Y.SR))
    for k in range(3):
        Y.place(out, Y.impact(f"hubk{k}", 0.22), 0.1 + k * 0.33)
    return Y.reverb(Y.lp(out, 1500), 2.0, 0.6, "hubk")


def ding():
    d = 3.0
    t = Y.t_axis(d)
    x = (np.sin(2 * np.pi * 1318.5 * t) + 0.5 * np.sin(2 * np.pi * 2637 * t)) * np.exp(-t * 2.2)
    return Y.reverb(Y.lp(x * 0.5, 4000), 3.0, 0.75, "hubding")


def creak():
    d = 1.4
    t = Y.t_axis(d)
    f0 = 180 + 120 * np.sin(2 * np.pi * 0.7 * t)
    x = Y.bp(np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * (1 + 0.5 * np.sin(2 * np.pi * 23 * t)), 300, 2400)
    x *= np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(x, 1.2, 0.35, "hubcreak"), 0.6)


def locked():
    out = np.zeros(int(1.0 * Y.SR))
    for k in range(3):
        Y.place(out, Y.impact(f"hublk{k}", 0.12, ring=0.4), 0.05 + k * 0.16, 0.6)
    return Y.norm(Y.reverb(out, 0.7, 0.3, "hublk"), 0.8)


sound("ambient.the_hub", [("ambient/the_hub", hub_air)], stream=True)
sound("ambient.the_hub.additions",
      [("ambient/the_hub_close", door_close), ("ambient/the_hub_knock", knock_back), ("ambient/the_hub_ding", ding)],
      subtitle="A door somewhere down the hall")
sound("event.the_hub_open", [("event/the_hub_open", creak)], subtitle="A door opens")
sound("event.the_hub_locked", [("event/the_hub_locked", locked)], subtitle="Locked")

level("the_hub", fog="#1a0e0a", sky="#1a0e0a", ambient="ambient.the_hub", additions="ambient.the_hub.additions",
      additions_chance=0.004, fixed_time=18000, height=32)
