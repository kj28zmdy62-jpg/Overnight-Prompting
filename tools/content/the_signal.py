"""The Signal - a moor at night in a storm that never passes, radio masts blinking red
every few hundred metres. Every mast has a hut, every hut has a radio. One frequency
opens the transmitter room. Another one is already taken."""
import numpy as np
from assets import cube, cube_faces, cross, tex, shaped, exit_block, level, sound, custom, ladder
from content.ind_kit import anim_tex, text, rivets, steel, concrete, rust, vstreaks, box_model, facing_states, glow_spot
import tex as T
import synth as Y

S = 32


def grass_top():
    img = T.ramp(T.fbm("sigg", octaves=4, base=5) * 0.6 + T.noise("sigg2", sigma=0.5) * 0.4,
                 "#1e2418", "#2a3020", "#363a26")
    r = T.rng("siggb")
    for _ in range(90):
        x, y = int(r.random() * S), int(r.random() * S)
        c = ["#4a3a3a", "#3a4428", "#5a4a30"][int(r.random() * 3)]
        img[y, x] = T.hexc(c)
    wet = (T.noise("siggw", sigma=2) > 0.75).astype(float)
    return T.grain(T.blend(img, T.solid("#1a2024"), wet * 0.35), "sigg", 0.05)


def peat():
    img = T.ramp(T.fbm("sigp", octaves=4, base=4), "#1a140e", "#2a2018")
    fib = (T.noise("sigpf", sigma=0.5) > 0.85).astype(float)
    return T.grain(T.blend(img, T.solid("#3a2a1a"), fib * 0.5), "sigp", 0.05)


def peat_side():
    img = peat()
    yy = np.mgrid[0:S, 0:S][0]
    fr = 4 + (T.noise("sigpsf", shape=(1, S), sigma=0.8)[0] * 4).astype(int)
    return T.blend(img, grass_top(), (yy < fr[None, :]).astype(float))


def granite(seed="sigr"):
    img = T.ramp(T.fbm(seed, octaves=4, base=4) * 0.6 + T.noise(seed + "f", sigma=0.6) * 0.4,
                 "#3a3c3e", "#55575a", "#6a6c6c")
    lich = np.clip(T.fbm(seed + "l", octaves=3, base=3) * 2 - 1.3, 0, 1)
    img = T.blend(img, T.solid("#7a7a52"), lich * 0.5)
    return T.grain(img, seed, 0.05)


def drystone():
    img = T.solid("#141414")
    stone = granite("sigds")
    r = T.rng("sigds2")
    for row in range(5):
        y0 = row * 6 + 1
        x = -int(r.random() * 6)
        while x < S:
            wdt = 5 + int(r.random() * 7)
            shade = 0.75 + r.random() * 0.4
            x0, x1 = max(x, 0), min(x + wdt - 1, S)
            img[y0:y0 + 5, x0:x1] = stone[y0:y0 + 5, x0:x1] * shade
            x += wdt
    return T.grain(img, "sigds", 0.04)


def heather():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("sighe")
    for _ in range(40):
        x0, h = 2 + r.random() * 28, 5 + r.random() * 12
        lean = (r.random() - 0.5) * 0.5
        for k in range(int(h)):
            y = S - 1 - k
            x = int(x0 + lean * k) % S
            rgb[y, x] = T.hexc("#3a3020") if k < h * 0.6 else T.hexc(["#6a3a5a", "#5a3048", "#4a3a2a"][int(r.random() * 3)])
            a[y, x] = 1
    return rgb, a


def tussock():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("sigtu")
    for _ in range(60):
        x0, h = 6 + r.random() * 20, 8 + r.random() * 20
        lean = (r.random() - 0.5) * 1.2
        for k in range(int(h)):
            y = S - 1 - k
            x = int(x0 + lean * k * (k / h)) % S
            rgb[y, x] = T.hexc("#4a4a30") * (0.6 + 0.4 * k / h)
            a[y, x] = 1
    return rgb, a


def lattice():
    """Red and white painted steel lattice, X-braced, open between the members."""
    yy, xx = np.mgrid[0:S, 0:S]
    red = T.ramp(T.noise("sigl", sigma=1), "#8a1a14", "#a82a1e")
    white = T.ramp(T.noise("siglw", sigma=1), "#b8b4aa", "#d0ccc2")
    rgb = np.where(((yy // 16) % 2 == 0)[..., None], red, white)
    rgb = rust(rgb, "siglr", 0.25)
    a = ((xx < 3) | (xx > 28) | (yy < 2) | (np.abs(xx - yy) < 2) | (np.abs(xx - (31 - yy)) < 2)).astype(float)
    return rgb, a


def platform():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = rust(steel("sigpl", "#3a3e42", "#545a60", brushed=False), "sigplr", 0.3)
    a = (((xx % 4) < 1) | ((yy % 4) < 1)).astype(float)
    a[:1, :] = a[-1:, :] = a[:, :1] = a[:, -1:] = 1
    return rgb, a


def beacon(i):
    on = i in (0, 1, 2, 3)
    yy, xx = np.mgrid[0:S, 0:S]
    g = glow_spot((S, S), 15.5, 15.5, 15, 1.0)
    img = T.ramp(g, "#ff2010", "#ff8070", "#ffffff") if on else T.ramp(g, "#3a0806", "#5a100c")
    return T.border(img, "#1a1a1a", 2)


def hut_wall():
    img = concrete("sighw", "#6a6a64", "#80807a", stain="#3a3a34", stains=3)
    peb = (T.rng("sighwp").random((S, S)) > 0.7).astype(float)
    img = T.shade(img, 1 - peb * 0.12 + (T.rng("sighwq").random((S, S)) > 0.9) * 0.1)
    return vstreaks(img, "sighwv", "#2a2c28", 4, 0.4)


def hut_roof():
    img = T.ramp(T.noise("sighr", sigma=1.5), "#141414", "#222220")
    return T.grain(T.stains(img, "sighrs", "#2a2e2a", 2, 0.3, 8), "sighr", 0.05)


def hut_floor():
    img = concrete("sighf", "#4a4a46", "#5a5a54", stains=2)
    return T.stains(img, "sighfm", "#2a2620", 2, 0.4, 7)


def hut_window():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("sighwn", sigma=2), "#2a2a1a", "#4a422a")
    g = glow_spot((S, S), 16, 18, 20, 1.0)
    img = T.blend(img, T.solid("#c8a050"), g * 0.6)
    streak = np.zeros((S, S))
    r = T.rng("sighwr")
    for _ in range(10):
        x = int(r.random() * S)
        streak[:, x] = 0.35
    img = T.blend(img, T.solid("#8a9aa8"), streak)
    img = T.border(img, "#2a2a28", 3)
    img[14:17, :] = T.hexc("#2a2a28")
    img[:, 14:17] = T.hexc("#2a2a28")
    return img


def door(locked=True):
    img = rust(steel("sigdr", "#4a5048", "#5a6058", brushed=False), "sigdrr", 0.45)
    img = T.border(img, "#1e221e", 2)
    img = T.rect(img, 3, 4, 29, 18, "#e8e0c8")
    img = text(img, "TX", 12, 5, "#c01818")
    img = text(img, "AUTH", 8, 12, "#1a1a1a")
    img = T.rect(img, 22, 22, 26, 25, "#1a1a1a")
    return img


def radio_front(i):
    img = T.ramp(T.noise("sigrf", sigma=1), "#2a2620", "#3a342a")
    img = T.border(img, "#141210", 2)
    dial = T.ramp(T.noise("sigrd", sigma=2), "#c89040", "#e8b060")
    img[5:12, 4:28] = dial[5:12, 4:28]
    for x in range(5, 27, 2):
        img[5 if x % 4 else 6, x] = T.hexc("#3a2410")
    img = text(img, "88 96 104", 4, 8, "#3a2410")
    needle = 6 + (i * 7) % 20
    img[5:12, needle] = T.hexc("#c01010")
    yy, xx = np.mgrid[0:S, 0:S]
    for cx in (9, 23):
        d = np.sqrt((xx - cx) ** 2 + (yy - 22) ** 2)
        img = T.blend(img, T.solid("#141210"), (d < 5).astype(float))
        img = T.blend(img, T.solid("#5a5450"), (d < 3.5).astype(float))
    grill = ((xx % 2) == 0) & (yy > 15) & (yy < 29) & (xx > 13) & (xx < 19)
    img = T.blend(img, T.solid("#0a0a0a"), grill.astype(float))
    return img


def radio_side():
    img = T.ramp(T.noise("sigrs", sigma=1.5), "#4a3220", "#5a3e28")
    return T.border(T.grain(img, "sigrs", 0.04), "#2a1a10", 1)


def desk():
    img = T.ramp(T.noise("sigdk", sigma=1.2), "#4a3a2a", "#5a4632")
    yy = np.mgrid[0:S, 0:S][0]
    return T.grain(T.shade(img, 1 - ((yy % 8) == 0) * 0.2), "sigdk", 0.04)


def bulb():
    g = glow_spot((S, S), 15.5, 15.5, 12, 1.2)
    return T.ramp(g, "#4a3010", "#ffc060", "#fff8e0")


def sign_tx():
    img = T.solid("#e8e4d8")
    img = T.border(img, "#c01818", 2)
    img = text(img, "DANGER", 4, 5, "#c01818")
    img = text(img, "RF", 12, 13, "#1a1a1a")
    img = text(img, "KEEP", 8, 20, "#1a1a1a")
    img = text(img, "OFF", 10, 26, "#1a1a1a")
    return img


def ladder_tex():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = rust(steel("sigld", "#5a5e62", "#7a7e82"), "sigldr", 0.3)
    a = ((xx < 4) | (xx > 27) | ((yy % 8) < 2)).astype(float)
    return rgb, a


def exit_top():
    g = glow_spot((S, S), 15.5, 15.5, 16, 0.8)
    return T.ramp(g, "#1a0a08", "#ff3020", "#ffd0c0")


tex("sig_grass", grass_top)
tex("sig_peat", peat)
tex("sig_peat_side", peat_side)
tex("sig_granite", lambda: granite())
tex("sig_drystone", drystone)
tex("sig_heather", heather)
tex("sig_tussock", tussock)
tex("sig_lattice", lattice)
tex("sig_platform", platform)
anim_tex("sig_beacon", beacon, 8, frametime=4)
tex("sig_hut_wall", hut_wall)
tex("sig_hut_roof", hut_roof)
tex("sig_hut_floor", hut_floor)
tex("sig_hut_window", hut_window)
tex("sig_door", door)
anim_tex("sig_radio_front", radio_front, 4, frametime=12)
tex("sig_radio_side", radio_side)
tex("sig_desk", desk)
tex("sig_bulb", bulb)
tex("sig_sign_tx", sign_tx)
tex("sig_ladder", ladder_tex)
tex("sig_exit_top", exit_top)

cube_faces("sig_grass", top="sig_grass", bottom="sig_peat", side="sig_peat_side", display="Moorland", sound="grass")
cube("sig_peat", "sig_peat", display="Peat", sound="mud")
cube("sig_granite", "sig_granite", display="Granite")
cube("sig_drystone", "sig_drystone", display="Dry Stone Wall")
cross("sig_heather", "sig_heather", display="Heather")
cross("sig_tussock", "sig_tussock", display="Tussock Grass")
cube("sig_lattice", "sig_lattice", display="Mast Lattice", sound="metal", render="cutout")
shaped("sig_platform", [(0, 14, 0, 16, 16, 16, "p")], {"p": "sig_platform"}, display="Mast Platform", sound="metal",
       unbreakable=True)
shaped("sig_beacon", [(5, 0, 5, 11, 7, 11, "b"), (4, 0, 4, 12, 1, 12, "f")], {"b": "sig_beacon", "f": "sig_platform"},
       display="Aircraft Warning Light", sound="glass", unbreakable=True, light=10, emissive=True, collides=False)
cube("sig_hut_wall", "sig_hut_wall", display="Pebbledash")
cube_faces("sig_hut_roof", top="sig_hut_roof", bottom="sig_hut_floor", side="sig_hut_roof", display="Felt Roof")
cube("sig_hut_floor", "sig_hut_floor", display="Hut Floor")
cube("sig_hut_window", "sig_hut_window", display="Hut Window", sound="glass", light=6, emissive=True)
custom("sig_radio", "com.noclip.block.SigRadioBlock",
       box_model([(0, 0, 0, 16, 9, 16, {"all": "side", "up": "top"}), (2, 9, 4, 14, 17, 12, {"all": "rs", "south": "front"})],
                 {"side": "sig_desk", "top": "sig_desk", "rs": "sig_radio_side", "front": "sig_radio_front"}),
       blockstate=facing_states(lambda v: "sig_radio"), display="Radio Set", sound="wood", unbreakable=True,
       light=4, render="cutout")
custom("sig_tx_door", "com.noclip.block.SigDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/sig_door"}},
       display="Transmitter Room Door", sound="metal", unbreakable=True)
shaped("sig_bulb", [(6, 6, 6, 10, 12, 10, "b"), (7, 12, 7, 9, 16, 9, "c")], {"b": "sig_bulb", "c": "sig_radio_side"},
       display="Bare Bulb", sound="glass", unbreakable=True, light=13, emissive=True, collides=False)
shaped("sig_sign_tx", [(1, 2, 0, 15, 14, 1, {"all": "s"})], {"s": "sig_sign_tx"}, display="RF Warning Sign",
       sound="metal", unbreakable=True, collides=False)
ladder("sig_ladder", "sig_ladder", display="Mast Ladder", unbreakable=True)
exit_block("sig_mast_top", "sig_exit_top", "ASCEND", trigger="INSIDE", display="Top of the Mast", light=12,
           emissive=True, sound="metal")


# ------------------------------------------------------------------ sound

def storm_loop():
    d = 28.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "sigwind"), 150, 1800)
    gust = 0.6 + 0.4 * np.sin(2 * np.pi * t / 7.0) * np.sin(2 * np.pi * t / 4.0 + 1)
    howl = sum(np.sin(2 * np.pi * (round(f * d) / d) * t + k) * g for k, (f, g) in enumerate(((330, 0.02), (494, 0.012))))
    rain = Y.bp(Y.white(d, "sigrain"), 2000, 12000) * 0.35
    rumble = Y.lp(Y.brown(d, "sigrum"), 70) * 0.8 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 14.0) ** 2)
    x = wind * gust * 0.9 + howl * gust + rain + rumble
    return Y.stereoize(Y.loopable(Y.norm(x, 0.6), 1.5), 0.006), "stereo"


def far_static():
    d = 2.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "sigfs"), 600, 4000) * Y.env(len(t), 0.1, 0.8)
    x = x * (0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 7 * t)))
    return Y.norm(Y.reverb(x, 3.0, 0.7, "sigfsr"), 0.35)


def lattice_moan():
    d = 4.0
    t = Y.t_axis(d)
    f = 180 + 40 * np.sin(2 * np.pi * 0.3 * t)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.4 + np.sin(2 * np.pi * np.cumsum(f * 2.01) / Y.SR) * 0.2
    x = x * Y.env(len(t), 1.0, 1.5) + Y.bp(Y.pink(d, "siglm"), 400, 2000) * 0.2
    return Y.norm(Y.reverb(x, 3.0, 0.6, "siglmr"), 0.4)


def thunder_far():
    d = 6.0
    t = Y.t_axis(d)
    x = Y.lp(Y.brown(d, "sigth"), 200) * np.exp(-t / 2.0) * (1 - np.exp(-t * 6))
    return Y.norm(Y.reverb(x, 4.0, 0.5, "sigthr"), 0.7)


def thunder_near():
    d = 6.0
    t = Y.t_axis(d)
    crack = Y.bp(Y.white(d, "sigtc"), 800, 9000) * np.exp(-t / 0.15)
    boom = Y.lp(Y.brown(d, "sigtb"), 180) * np.exp(-t / 2.0) * 1.5
    return Y.norm(Y.reverb(crack + boom, 4.0, 0.5, "sigtnr"), 0.95)


def tune_click():
    d = 0.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "sigtn"), 500, 5000) * Y.env(len(t), 0.005, 0.25) * 0.8
    Y.place(x, Y.impact("sigtnk", 0.1) * 0.3, 0.0, 1.0)
    return Y.norm(x, 0.5)


def numbers():
    """A numbers station: three tones, then a flat synthetic voice counting."""
    d = 6.0
    out = np.zeros(int(d * Y.SR))
    for k, f in enumerate((1200, 900, 1200)):
        n = int(0.25 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, np.sin(2 * np.pi * f * tt) * 0.4, 0.2 + k * 0.35, 1.0)
    r = Y.rng("signum")
    for k in range(6):
        n = int(0.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        f0 = 110 + r.random() * 20
        buzz = np.sign(np.sin(2 * np.pi * f0 * tt))
        vow = Y.bp(buzz, 300 + r.random() * 500, 1200 + r.random() * 1200) * np.sin(np.pi * tt / (n / Y.SR))
        Y.place(out, vow * 0.8, 1.6 + k * 0.65, 1.0)
    out += Y.bp(Y.white(d, "signumh"), 500, 4000) * 0.06
    return Y.norm(Y.bp(out, 250, 3800), 0.7)


def breathing():
    d = 5.0
    t = Y.t_axis(d)
    stat = Y.bp(Y.white(d, "sigbr"), 400, 4000) * 0.15
    b = Y.bp(Y.pink(d, "sigbrp"), 150, 1500) * (np.abs(np.sin(np.pi * t / 1.25)) ** 3) * 0.9
    low = np.sin(2 * np.pi * np.cumsum(60 + 10 * np.sin(2 * np.pi * 0.2 * t)) / Y.SR) * 0.2 * Y.env(len(t), 1.0, 1.0)
    return Y.norm(Y.bp(stat + b + low, 100, 4000), 0.75)


sound("ambient.the_signal", [("ambient/the_signal", storm_loop)], stream=True)
sound("ambient.the_signal.additions",
      [("ambient/the_signal_static", far_static), ("ambient/the_signal_moan", lattice_moan),
       ("ambient/the_signal_thunder", thunder_far)], subtitle="The storm over the moor")
sound("event.the_signal_thunder", [("event/the_signal_thunder", thunder_near)], subtitle="Lightning strikes")
sound("event.the_signal_tune", [("event/the_signal_tune", tune_click)], subtitle="Radio tunes")
sound("event.the_signal_numbers", [("event/the_signal_numbers", numbers)], subtitle="A voice reads numbers")
sound("event.the_signal_breathing", [("event/the_signal_breathing", breathing)], subtitle="Breathing on the frequency")

level("the_signal", fog="#14181e", sky="#0c0f14", water="#1a2228", water_fog="#05080a", ambient="ambient.the_signal",
      additions="ambient.the_signal.additions", additions_chance=0.006, fixed_time=18000, skylight=True,
      ceiling=False, height=80, particle="minecraft:rain", particle_chance=0.2)
