"""Museum of You. White galleries, parquet, spotlights on plinths. Every exhibit is about you."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, wood, voice, speaker, chime

S = 32


def parquet():
    yy, xx = np.mgrid[0:S, 0:S]
    # herringbone: 4 px wide staves, 16 long, alternating 45-degree runs
    u = (xx + yy) % 32
    v = (xx - yy) % 32
    blk = ((xx // 8) + (yy // 8)) % 2
    stave = np.where(blk == 0, (u // 4), (v // 4))
    r = T.rng("mpq").random(64)
    tone = r[stave % 64]
    img = T.ramp(tone * 0.5 + T.noise("mpqn", sigma=0.6) * 0.5, "#8a5e34", "#a87444")
    seam = np.where(blk == 0, (u % 4) == 0, (v % 4) == 0)
    img = T.shade(img, 1 - seam * 0.2)
    img = T.shade(img, 1 + np.exp(-((xx - yy - 8) % 32 - 8) ** 2 / 20.0) * 0.06)
    return T.grain(img, "mpq", 0.03)


def wall_white():
    img = T.ramp(T.fbm("mww", octaves=2, base=6), "#e2dfd6", "#ebe8e0")
    return T.grain(img, "mww", 0.012)


def wall_base():
    img = wall_white()
    img[22:] = T.ramp(T.noise("mwb", sigma=1, shape=(10, S)), "#3a3632", "#46423c")
    img[22] = T.hexc("#5a5650")
    return img


def wall_top():
    img = wall_white()
    img[0:4] = T.hexc("#d0ccc2")
    img[4] = T.hexc("#b8b4aa")
    return img


def ceiling():
    img = T.ramp(T.fbm("mce", octaves=2, base=6), "#e6e3dc", "#eeebe4")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.04)
    return T.grain(img, "mce", 0.01)


def plinth_side():
    img = T.ramp(T.fbm("mps", octaves=2, base=6), "#e8e6e0", "#f2f0ea")
    img[-3:] = T.hexc("#2a2826")
    img = T.shade(img, 1 - (np.arange(S)[None, :] * np.ones((S, 1)) == 0) * 0.06)
    return T.grain(img, "mps", 0.01)


def plinth_top():
    return T.grain(T.border(T.solid("#f4f2ec"), "#d8d6d0", 1), "mpt", 0.01)


def glass():
    rgb = T.solid("#dfe8ea")
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.ones((S, S)) * 0.12
    streak = np.abs(((xx + yy) % 32) - 9) < 2
    a = a + streak * 0.12
    edge = (xx == 0) | (yy == 0) | (xx == S - 1) | (yy == S - 1)
    rgb = T.blend(rgb, T.solid("#8a9496"), edge.astype(float))
    a = np.where(edge, 0.9, a)
    return rgb, a


def spot(on=True):
    img = T.solid("#1a1a1a")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    if on:
        img = T.blend(img, T.ramp(np.clip(1 - d / 10, 0, 1), "#f0e2c0", "#ffffff"), (d < 10).astype(float))
    else:
        img = T.blend(img, T.solid("#3a3a38"), (d < 10).astype(float))
    return img


def rope():
    return T.grain(T.ramp(T.noise("mro", sigma=0.6), "#6a0e14", "#8a1a20"), "mro", 0.05)


def brass():
    return brushed("mbr", "#8a6a2a", "#d8b45a")


def leather():
    return T.grain(T.ramp(T.noise("mle", sigma=1.2), "#1a1a1a", "#2a2a2a"), "mle", 0.03)


def frame_wrap(paint, seed, gold=True):
    """Put a canvas inside a picture frame."""
    img = paint
    c1, c2 = ("#8a6420", "#d0a850") if gold else ("#141414", "#2a2a2a")
    fr = brushed(seed + "fr", c1, c2, vertical=False)
    yy, xx = np.mgrid[0:S, 0:S]
    border = (xx < 3) | (xx > 28) | (yy < 3) | (yy > 28)
    img = T.blend(img, fr, border.astype(float))
    img = T.shade(img, 1 + ((xx == 0) | (yy == 0)) * 0.2 - ((xx == 31) | (yy == 31)) * 0.3)
    inner = ((xx == 3) | (xx == 28) | (yy == 3) | (yy == 28)) & ~border
    img = T.shade(img, 1 - inner * 0.35)
    return img


def brush_noise(img, seed, amt=0.08):
    """Oil-paint dabs: a bit of directional texture on top of a flat picture."""
    from scipy.ndimage import gaussian_filter
    r = T.rng("bn", seed).random((S, S))
    r = gaussian_filter(r, (0.4, 1.2), mode="wrap")
    r = (r - r.min()) / (r.max() - r.min())
    return T.shade(img, 1 + (r - 0.5) * amt * 2)


def art_lobby():
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = T.solid("#c8b464")
    # one-point perspective corridor
    vx, vy = 16, 14
    fl = yy > vy + (np.abs(xx - vx)) * 0.55
    ce = yy < vy - (np.abs(xx - vx)) * 0.45
    img = T.blend(img, T.solid("#8a7834"), fl.astype(float))
    img = T.blend(img, T.solid("#d8cfa0"), ce.astype(float))
    door = (np.abs(xx - vx) < 2) & (yy > vy - 3) & (yy < vy + 2)
    img = T.blend(img, T.solid("#3a3014"), door.astype(float))
    for k in (6, 9, 11):
        lt = ce & (np.abs(yy - (vy - k * 0.9)) < 0.6) & (np.abs(xx - vx) < k * 0.6)
        img = T.blend(img, T.solid("#fffbe0"), lt.astype(float))
    return frame_wrap(brush_noise(img, "al"), "al")


def art_dark():
    img = T.ramp(T.noise("adk", sigma=3), "#050505", "#141414")
    img = T.rect(img, 13, 10, 19, 13, "#3cff7a")
    img = T.rect(img, 14, 14, 18, 22, "#101010")
    return frame_wrap(brush_noise(img, "ad"), "ad", gold=False)


def art_warehouse():
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = T.ramp(np.clip(yy / 32, 0, 1), "#7a7e82", "#4a4c4c")
    for px, w in ((7, 2), (15, 1.5), (21, 1), (25, 0.8)):
        col = np.abs(xx - px) < w
        img = T.blend(img, T.solid("#3a3c3c"), col.astype(float) * (0.9 - px / 40))
    fog = T.noise("awf", sigma=3)
    img = T.blend(img, T.solid("#9a9ea0"), fog * 0.3)
    return frame_wrap(brush_noise(img, "aw"), "aw")


def art_you():
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = T.ramp(np.clip(1 - np.abs(xx - 16) / 14, 0, 1), "#141210", "#4a4232")
    head = ((xx - 16) ** 2 + (yy - 12) ** 2) < 7
    body = (np.abs(xx - 16) < 4 + (yy - 16) * 0.2) & (yy > 14)
    img = T.blend(img, T.solid("#0a0806"), (head | body).astype(float))
    return frame_wrap(brush_noise(img, "ay", 0.12), "ay")


def art_stairs():
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = T.solid("#5c7965")
    for k in range(6):
        y0 = 26 - k * 4
        step = (yy >= y0) & (yy < y0 + 4) & (xx >= 4 + k * 4)
        img = T.blend(img, T.solid("#c9c6b6" if k % 2 else "#a8a594"), step.astype(float))
    img = ink(img, centered("?", y=6, scale=1), "#141414")
    return frame_wrap(brush_noise(img, "as"), "as")


def art_door():
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = T.ramp(T.noise("adr", sigma=2), "#2a2420", "#3a322a")
    door = (xx > 10) & (xx < 22) & (yy > 6)
    img = T.blend(img, T.solid("#1a1410"), door.astype(float))
    gap = (xx > 19) & (xx < 22) & (yy > 6)
    img = T.blend(img, T.solid("#fff0c0"), gap.astype(float))
    spill = (yy > 26) & (xx > 18) & (xx < 30)
    img = T.blend(img, T.solid("#c8b080"), spill.astype(float) * 0.6)
    return frame_wrap(brush_noise(img, "adr"), "adr")


def art_portrait():
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = T.ramp(np.clip(1 - np.sqrt((xx - 16) ** 2 + (yy - 14) ** 2) / 20, 0, 1), "#2a1e14", "#5a4430")
    head = ((xx - 16) / 5) ** 2 + ((yy - 13) / 6.5) ** 2 < 1
    img = T.blend(img, T.ramp(T.noise("apf", sigma=2), "#c8a888", "#d8b898"), head.astype(float))
    shoulders = (yy > 21) & (np.abs(xx - 16) < 4 + (yy - 21) * 1.3)
    img = T.blend(img, T.solid("#2a2a3a"), shoulders.astype(float))
    return frame_wrap(brush_noise(img, "apo", 0.1), "apo")


def art_blank():
    img = T.ramp(T.noise("abl", sigma=2), "#ece8dc", "#f4f0e6")
    img = ink(img, centered("SOON", y=13, scale=1), "#8a8478", 0.6)
    return frame_wrap(img, "abl", gold=False)


def souvenir_shelf():
    img = wood("mss", "#c8b088", "#d8c098", plank=16)
    r = T.rng("mssi")
    for sy in (2, 18):
        x = 1
        while x < 30:
            w = 3 + int(r.random() * 4)
            h = 6 + int(r.random() * 6)
            col = ["#d84a3a", "#3a7ad8", "#e8c838", "#e8e4dc", "#3aa860"][int(r.random() * 5)]
            img[sy + 13 - h:sy + 13, x:x + w] = T.hexc(col)
            img[sy + 13 - h, x:x + w] *= 1.2
            x += w + 1
        img[sy + 13:sy + 15] = T.hexc("#8a7050")
    return T.grain(np.clip(img, 0, 1), "mss", 0.03)


def gift_sign():
    img = T.solid("#1a1a1a")
    img = ink(img, centered("GIFTS", y=12, scale=1), "#e8c838")
    return T.border(img, "#e8c838", 1)


def exit_door():
    img = T.ramp(T.noise("med", sigma=2), "#e0ddd4", "#e8e5dc")
    img = T.border(img, "#8a8680", 2)
    img = T.rect(img, 23, 15, 27, 18, "#8a8a84")
    img = T.rect(img, 6, 4, 26, 10, "#1a1a1a")
    img = ink(img, centered("EXIT", y=4, scale=1), "#40ff80")
    return T.grain(img, "med", 0.02)


tex("mus_parquet", parquet)
tex("mus_wall", wall_white)
tex("mus_wall_base", wall_base)
tex("mus_wall_top", wall_top)
tex("mus_ceiling", ceiling)
tex("mus_plinth_side", plinth_side)
tex("mus_plinth_top", plinth_top)
tex("mus_glass", glass)
tex("mus_spot", lambda: spot(True))
tex("mus_spot_off", lambda: spot(False))
tex("mus_rope", rope)
tex("mus_brass", brass)
tex("mus_leather", leather)
ARTS = {"lobby": art_lobby, "dark": art_dark, "warehouse": art_warehouse, "you": art_you, "stairs": art_stairs,
        "door": art_door, "portrait": art_portrait, "blank": art_blank}
for nm, fn in ARTS.items():
    tex(f"mus_art_{nm}", fn)
tex("mus_souvenirs", souvenir_shelf)
tex("mus_gift_sign", gift_sign)
tex("mus_exit_door", exit_door)

cube("mus_parquet", "mus_parquet", display="Herringbone Parquet", sound="wood")
cube("mus_wall", "mus_wall", display="Gallery Wall")
cube("mus_wall_base", "mus_wall_base", display="Gallery Wall")
cube("mus_wall_top", "mus_wall_top", display="Gallery Wall")
cube("mus_ceiling", "mus_ceiling", display="Gallery Ceiling", flat=True)
cube_faces("mus_plinth", top="mus_plinth_top", bottom="mus_plinth_top", side="mus_plinth_side", display="Plinth")
cube_faces("mus_plinth_empty", top="mus_plinth_top", bottom="mus_plinth_top", side="mus_plinth_side",
           display="Your Plinth")
shaped("mus_vitrine", [(1, 0, 1, 15, 14, 15, "g")], {"g": "mus_glass"}, display="Vitrine", sound="glass",
       unbreakable=True, render="translucent")
shaped("mus_spot", [(5, 12, 5, 11, 16, 11, {"all": "can", "down": "lens"})], {"can": "mus_leather", "lens": "mus_spot"},
       display="Track Spotlight", light=13, emissive=True, collides=False, unbreakable=True, sound="glass")
shaped("mus_spot_off", [(5, 12, 5, 11, 16, 11, {"all": "can", "down": "lens"})], {"can": "mus_leather", "lens": "mus_spot_off"},
       display="Track Spotlight", collides=False, unbreakable=True, sound="glass")
shaped("mus_rope", [
    (7, 0, 7, 9, 12, 9, "brass"), (6, 0, 6, 10, 1, 10, "brass"), (6.5, 12, 6.5, 9.5, 13, 9.5, "brass"),
    (0, 9, 7.5, 7, 10, 8.5, "rope"), (9, 9, 7.5, 16, 10, 8.5, "rope"),
], {"brass": "mus_brass", "rope": "mus_rope"}, display="Velvet Rope", sound="metal", unbreakable=True)
shaped("mus_bench", [(0, 7, 3, 16, 10, 13, "l"), (2, 0, 4, 4, 7, 12, "b"), (12, 0, 4, 14, 7, 12, "b")],
       {"l": "mus_leather", "b": "mus_brass"}, display="Gallery Bench", sound="wool", unbreakable=True)
for nm in ARTS:
    shaped(f"mus_art_{nm}", [(0, 0, 0, 16, 16, 1.5, {"all": "f", "south": "a"})],
           {"a": f"mus_art_{nm}", "f": "mus_leather"}, display="Painting", collides=False, unbreakable=True)
shaped("mus_souvenir_shelf", [(0, 0, 4, 16, 16, 12, {"all": "w", "north": "s", "south": "s"})],
       {"s": "mus_souvenirs", "w": "mus_plinth_side"}, display="Souvenir Shelf", unbreakable=True)
shaped("mus_souvenir", [(4, 0, 4, 12, 6, 12, "s")], {"s": "mus_souvenirs"}, display="Souvenir", kind="pickup",
       item="noclip:almond_water*2,noclip:journal*1,noclip:energy_bar*2", strength=0.3)
shaped("mus_gift_sign", [(0, 0, 7, 16, 8, 9, {"all": "f", "north": "s", "south": "s"})],
       {"s": "mus_gift_sign", "f": "mus_leather"}, display="Gift Shop Sign", collides=False, unbreakable=True,
       emissive=True)
exit_block("mus_exit_door", "mus_exit_door", "DOOR", trigger="USE", solid=True, display="Exit Through the Gift Shop",
           sound="wood")

# ------------------------------------------------------------------ sound


def gallery_air():
    d = 26.0
    t = Y.t_axis(d)
    air = Y.lp(Y.pink(d, "mga"), 700) * 0.5
    strings = Y.drone(d, [110.0, 164.8, 220.0], "mgs", lfo=1 / 13) * 0.06
    tick = np.zeros_like(t)
    for k in range(int(d)):
        Y.place(tick, Y.bp(Y.white(0.02, f"mt{k}"), 2000, 6000) * 0.08, k + 0.5)
    x = Y.reverb(air + strings + tick, 3.5, 0.6, "mgrv")[: len(t)]
    return Y.stereoize(Y.loopable(Y.norm(x, 0.35), 1.5), 0.0015), "stereo"


def heels(i):
    def f():
        out = np.zeros(int(5 * Y.SR))
        for k in range(6 + i):
            Y.place(out, Y.footstep(f"mh{i}{k}", "tile"), 0.2 + k * (0.55 + 0.05 * i), 0.5 + 0.3 * (k % 2))
        return Y.norm(Y.reverb(Y.lp(out, 3000), 3.5, 0.75, f"mhr{i}"), 0.4)
    return f


def creak():
    d = 1.4
    t = Y.t_axis(d)
    f0 = 90 + 40 * np.sin(2 * np.pi * 0.9 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * (0.5 + 0.5 * np.sin(2 * np.pi * 17 * t)) * np.sin(np.pi * t / d) ** 2
    return Y.norm(Y.reverb(Y.lp(x, 1500), 2.5, 0.6, "mcr"), 0.4)


def shutter():
    out = np.zeros(int(2.5 * Y.SR))
    for k, at in enumerate((0.0, 0.08)):
        Y.place(out, Y.bp(Y.white(0.05, f"msh{k}"), 1500, 8000) * np.exp(-np.arange(int(0.05 * Y.SR)) / Y.SR * 80), at)
    return Y.norm(Y.reverb(out, 3.0, 0.75, "mshr"), 0.4)


def closing():
    out = np.zeros(int(8.0 * Y.SR))
    Y.place(out, chime([523.3, 659.3, 784.0, 1046.5], dur=3.0, decay=1.2) * 0.5, 0.0)
    Y.place(out, voice("mclose", 3.2, pitch=170, breath=0.1), 2.6)
    out = speaker(out, 350, 3600)
    return Y.norm(Y.reverb(out, 3.5, 0.7, "mclr"), 0.6)


def acquire():
    d = 5.0
    t = Y.t_axis(d)
    x = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * (0.6 + i * 0.4)) / (i + 1) for i, f in enumerate((55.0, 110.3, 166.1, 221.7)))
    return Y.norm(Y.reverb(x, 4.0, 0.6, "macq"), 0.8)


sound("ambient.museum", [("ambient/museum", gallery_air)], stream=True)
sound("ambient.museum.additions",
      [(f"ambient/museum_heels{i}", heels(i)) for i in range(2)] +
      [("ambient/museum_creak", creak), ("ambient/museum_shutter", shutter)],
      subtitle="Someone else in the galleries")
sound("event.museum_closing", [("event/museum_closing", closing)], subtitle="The museum is closing")
sound("event.museum_acquire", [("event/museum_acquire", acquire)], subtitle="Acquired")

level("museum", fog="#cfcac0", sky="#cfcac0", ambient="ambient.museum", additions="ambient.museum.additions",
      additions_chance=0.003, fixed_time=6000, height=48)
