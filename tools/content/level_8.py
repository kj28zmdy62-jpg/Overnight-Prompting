"""Level 8 - Cave System. Enormous, lightless caves. Things crawl on the walls; they hate your light,
and your batteries are not infinite."""
import numpy as np
from assets import cube, tex, shaped, cross, exit_block, carpet, level, sound, lang, ITEM_MODELS, entity_model, \
    entity_tex, part
import tex as T
import synth as Y
from content.classics_kit import metal
from entity_paint import Canvas, skin

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def rock(seed, lo, hi, veins=None):
    def f():
        img = T.ramp(T.fbm(seed, octaves=4, base=4) * 0.7 + T.noise(seed + "n", sigma=0.6) * 0.3, lo, hi)
        strata = np.sin(yy / 2.2 + T.fbm(seed + "s", octaves=2, base=4) * 6) * 0.06
        img = T.shade(img, 1 + strata)
        img = T.cracks(img, seed + "c", "#0a0908", 2, 0.5)
        if veins:
            m = (T.noise(seed + "v", sigma=0.7) > 0.8).astype(float)
            img = T.blend(img, T.solid(veins), m * 0.7)
        return T.grain(img, seed, 0.05)
    return f


def wet_rock():
    img = rock("l8w", "#1c1a18", "#2a2622")()
    sheen = (T.noise("l8ws", sigma=1.5) > 0.6).astype(float)
    return T.shade(img, 1 + sheen * 0.35)


def gravel():
    img = T.ramp(T.noise("l8g", sigma=0.5), "#2a2622", "#3e3832")
    pebbles = T.noise("l8gp", sigma=1.0)
    img = T.shade(img, 0.8 + (pebbles > 0.6) * 0.3)
    return T.grain(img, "l8g", 0.06)


def mud():
    img = T.ramp(T.fbm("l8m", octaves=3, base=4), "#1e1810", "#2c2418")
    return T.grain(T.shade(img, 0.9 + T.noise("l8ms", sigma=2) * 0.25), "l8m", 0.04)


def stalagmite():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for y in range(S):
        half = 2 + (y / S) * 9
        m = np.abs(xx[y] - 15.5) < half
        rgb[y, m] = T.ramp(T.noise("l8sg", sigma=1)[y, m] * 0.5 + 0.3 * (y / S), "#2a2622", "#4a443c")
        a[y, m] = 1
    return rgb, a


def stalactite():
    rgb, a = stalagmite()
    return rgb[::-1].copy(), a[::-1].copy()


def fungus():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("l8fun")
    for k in range(6):
        cx, cy = 4 + r.random() * 24, 16 + r.random() * 12
        rad = 2 + r.random() * 3
        for y in range(int(cy), S):
            rgb[y, int(cx)] = T.hexc("#3a6a6a")
            a[y, int(cx)] = 1
        d = np.sqrt((xx - cx) ** 2 + ((yy - cy) * 1.8) ** 2)
        cap = (d < rad) & (yy <= cy)
        rgb[cap] = T.hexc("#7af0d0") if k % 2 else T.hexc("#a8fff0")
        a[cap] = 1
    return rgb, a


def lantern():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(np.clip(1 - d / 20, 0, 1), "#c86a20", "#ffc860", "#fff0c8")
    return T.shade(img, 1 - (((xx % 10) == 0) | ((yy % 10) == 0)) * 0.6)


def daylight():
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.ramp(np.clip(1 - d / 26 + T.noise("l8dl", sigma=3) * 0.2, 0, 1), "#9aa8b0", "#d8e2e8", "#ffffff")


def rope():
    rgb = T.ramp(T.noise("l8rope", sigma=0.5), "#8a6a40", "#a88a58")
    rgb = T.shade(rgb, 1 - (((xx + yy) % 4) == 0) * 0.3)
    return rgb


def mine_door():
    img = T.ramp(T.fbm("l8md", octaves=2, base=3), "#3a2614", "#4a3220")
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.5)
    img = T.rect(img, 0, 6, 32, 9, "#2a2a28")
    img = T.rect(img, 0, 23, 32, 26, "#2a2a28")
    img = T.rect(img, 3, 12, 29, 19, "#c8b42a")
    from content.classics_kit import centered
    img = centered(img, "SHAFT 8", 13, "#1a1408")
    return T.grain(img, "l8md", 0.04)


def chasm():
    return T.solid("#000000")


TEX = {"l8_rock": rock("l8r", "#24211e", "#34302a"), "l8_rock_dark": rock("l8rd", "#141210", "#201c18"),
       "l8_rock_vein": rock("l8rv", "#24211e", "#34302a", veins="#6a5a3a"), "l8_rock_wet": wet_rock,
       "l8_gravel": gravel, "l8_mud": mud, "l8_stalagmite": stalagmite, "l8_stalactite": stalactite,
       "l8_fungus": fungus, "l8_lantern": lantern, "l8_daylight": daylight, "l8_rope": rope, "l8_mine_door": mine_door,
       "l8_chasm": chasm}
for k, fn in TEX.items():
    tex(k, fn)

cube("cave_rock", "l8_rock", display="Cave Rock")
cube("cave_rock_dark", "l8_rock_dark", display="Dark Cave Rock")
cube("cave_rock_vein", "l8_rock_vein", display="Veined Cave Rock")
cube("cave_rock_wet", "l8_rock_wet", display="Wet Cave Rock")
cube("cave_gravel", "l8_gravel", display="Cave Gravel", sound="gravel")
cube("cave_mud", "l8_mud", display="Cave Mud", sound="mud")
cross("cave_stalagmite", "l8_stalagmite", display="Stalagmite", collides=True)
cross("cave_stalactite", "l8_stalactite", display="Stalactite")
cross("cave_glow_fungus", "l8_fungus", display="Glowing Fungus", light=9, emissive=True)
shaped("cave_lantern", [(5, 0, 5, 11, 9, 11, "glass"), (6, 9, 6, 10, 11, 10, "rope")],
       {"glass": "l8_lantern", "rope": "l8_rope"}, display="Old Lantern", light=12, emissive=True, sound="metal",
       collides=False, unbreakable=True)
shaped("cave_rope_coil", [(3, 0, 3, 13, 4, 13, "rope")], {"rope": "l8_rope"}, display="Rope Coil", sound="wool",
       collides=False)
exit_block("cave_daylight", "l8_daylight", "ASCEND", display="Light Far Above", light=15, emissive=True)
exit_block("cave_chasm_bottom", "l8_chasm", "FALL", display="Bottom Of The Chasm")
exit_block("cave_mine_door", "l8_mine_door", "DOOR", trigger="USE", solid=True, display="Mine Door", sound="wood")

# ------------------------------------------------------------------ the Cave Crawler


def crawler_parts():
    legs = []
    for name, x, z, rz in (("leg_fl", 4, -4, -0.9), ("leg_fr", -4, -4, 0.9), ("leg_bl", 4, 5, -0.9), ("leg_br", -4, 5, 0.9)):
        legs.append(part(name, (x, 15, z), rot=(0, 0, rz), cubes=[(0, 40, -1, -1, -1, 2, 18, 2)]))
    return [
        part("body", (0, 15, 2), cubes=[(0, 0, -4, -3, -6, 8, 6, 14)]),
        part("head", (0, 15, -5), cubes=[(0, 22, -3.5, -3, -6, 7, 6, 6)]),
        part("jaw", (0, 2, -5), parent="head", cubes=[(28, 22, -2.5, 0, -2, 5, 2, 3)]),
    ] + legs


def crawler_tex():
    c = Canvas(64, 64)
    pale = skin("cc", "#b8b0a0", "#d0c8b6", sigma=0.7, spots="#8a7a68")

    def face(w, h):
        img = pale(w, h)
        for (x, y) in ((1, 1), (5, 1), (2, 3), (4, 3), (3, 1)):
            if x < w and y < h:
                img[y, x] = T.hexc("#120806")
        return img

    c.box(0, 0, 8, 6, 14, all=pale)
    c.box(0, 22, 7, 6, 6, all=pale, front=face)
    c.box(28, 22, 5, 2, 3, all=lambda w, h: T.solid("#4a1a14", shape=(h, w)))
    c.box(0, 40, 2, 18, 2, all=skin("ccl", "#8a8070", "#a49a88", sigma=0.6))
    return c.out()


def crawler_glow():
    c = Canvas(64, 64)
    from assets import box_uv
    x, y, w, h = box_uv(0, 22, 7, 6, 6)["front"]
    for (ex, ey) in ((1, 1), (5, 1), (2, 3), (4, 3), (3, 1)):
        c.fill_rect(x + ex, y + ey, 1, 1, "#ff3020")
    rgb, a = c.out()
    return rgb, (rgb.sum(axis=2) > 0.1).astype(float)


entity_model("cave_crawler", crawler_parts())
entity_tex("cave_crawler", crawler_tex)
entity_tex("cave_crawler_glow", crawler_glow)
lang("entity.noclip.cave_crawler", "Crawler")
lang("item.noclip.cave_crawler_spawn_egg", "Crawler Spawn Egg")
ITEM_MODELS["cave_crawler_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def cave_loop():
    d = 28.0
    t = Y.t_axis(d)
    drone = Y.drone(d, [36.7, 55.0], "l8dr", lfo=0.04) * 0.4
    wind = Y.bp(Y.pink(d, "l8wind"), 150, 900) * 0.3 * (0.4 + 0.6 * np.sin(2 * np.pi * t * 2 / d) ** 2)
    drips = np.zeros_like(t)
    r = Y.rng("l8drip")
    for k in range(16):
        n = int(0.1 * Y.SR)
        tt = np.arange(n) / Y.SR
        fr = 1200 + r.random() * 900 - tt * 3000
        Y.place(drips, np.sin(2 * np.pi * np.cumsum(fr) / Y.SR) * np.exp(-tt * 40), r.random() * (d - 1), 0.2 + r.random() * 0.3)
    drips = Y.reverb(drips, 3.5, 0.75, "l8dv")[: len(t)]
    rumble = Y.lp(Y.brown(d, "l8rum"), 50) * 0.8
    x = drone + wind + drips * 0.5 + rumble
    x = Y.loopable(Y.norm(x, 0.5), 1.5)
    return Y.stereoize(x, 0.0025), "stereo"


def scuttle(i):
    def f():
        d = 2.2
        out = np.zeros(int(d * Y.SR))
        r = Y.rng(f"l8sc{i}")
        at = 0.1
        while at < d - 0.2:
            n = int(0.02 * Y.SR)
            tt = np.arange(n) / Y.SR
            Y.place(out, Y.bp(Y.white(0.02, f"sc{i}{at:.3f}"), 2000, 9000)[:n] * np.exp(-tt * 300), at, 0.4 + r.random() * 0.6)
            at += 0.025 + r.random() * 0.05
        out *= np.sin(np.pi * np.arange(len(out)) / len(out))
        return Y.norm(Y.reverb(out, 2.5, 0.6, f"l8scv{i}"), 0.5)
    return f


def rockfall():
    out = np.zeros(int(4 * Y.SR))
    r = Y.rng("l8rf")
    for k in range(12):
        Y.place(out, Y.impact(f"rf{k}", 0.3 + r.random() * 0.5), 0.1 + r.random() * 1.5, 0.3 + r.random() * 0.7)
    return Y.norm(Y.reverb(Y.lp(out, 2500), 3.5, 0.7, "l8rfv"), 0.6)


def far_wail():
    d = 4.0
    t = Y.t_axis(d)
    f0 = 260 + 80 * np.sin(np.pi * t / d) - 60 * t / d
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.sin(np.pi * t / d) ** 2
    x = Y.bp(x + Y.white(d, "l8fw") * 0.1, 200, 1500)
    return Y.norm(Y.reverb(Y.lp(x, 900), 4.0, 0.85, "l8fwv"), 0.35)


def chitter():
    d = 1.4
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * 38 * t)) * np.sin(2 * np.pi * 1800 * t) * (0.5 + 0.5 * np.sin(np.pi * t / d))
    x += Y.bp(Y.white(d, "l8ch"), 3000, 8000) * (np.sin(2 * np.pi * 21 * t) > 0.3) * 0.5
    return Y.norm(Y.bp(x, 800, 7000) * np.sin(np.pi * t / d), 0.7)


def hiss():
    d = 1.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "l8hs"), 2500, 9000) * np.clip(t / 0.05, 0, 1) * np.exp(-t * 3)
    x += np.sign(np.sin(2 * np.pi * 90 * t)) * np.exp(-t * 6) * 0.3
    return Y.norm(x, 0.8)


def crawler_hurt():
    d = 0.5
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * np.cumsum(1400 - 900 * t) / Y.SR) * np.exp(-t * 7)
    return Y.norm(x + Y.bp(Y.white(d, "l8hu"), 2000, 7000) * np.exp(-t * 12) * 0.5, 0.8)


sound("ambient.level_8", [("ambient/level_8", cave_loop)], stream=True)
sound("ambient.level_8.additions",
      [(f"ambient/level_8_scuttle{i}", scuttle(i)) for i in range(2)] +
      [("ambient/level_8_rockfall", rockfall), ("ambient/level_8_wail", far_wail)],
      subtitle="Something moves in the rock")
sound("event.level_8_rumble", [("event/level_8_rumble", rockfall)], subtitle="The rock shifts overhead")
sound("entity.cave_crawler.chitter", [("entity/cave_crawler_chitter", chitter)], subtitle="Crawler chitters")
sound("entity.cave_crawler.hiss", [("entity/cave_crawler_hiss", hiss)], subtitle="Crawler recoils from the light")
sound("entity.cave_crawler.hurt", [("entity/cave_crawler_hurt", crawler_hurt)], subtitle="Crawler shrieks")

level("level_8", fog="#000000", sky="#000000", water="#0a1410", water_fog="#020504", ambient="ambient.level_8",
      additions="ambient.level_8.additions", additions_chance=0.006, fixed_time=18000, height=96,
      particle="minecraft:mycelium", particle_chance=0.003)
