"""Level ??? - a level that was never saved properly: torn copies of other levels, smeared and shifted.
Its name keeps changing. The torn doorways take you to whatever it is calling itself right now."""
import numpy as np
from assets import cube, tex, custom, level, sound
import tex as T
import synth as Y
from content.rb_shared import text_center

S = 32


def smear():
    """Yellow wallpaper dragged sideways across the screen by a broken copy."""
    yy, xx = np.mgrid[0:S, 0:S]
    rows = T.rng("glsm").random(S)
    base = T.ramp(np.repeat(rows[:, None], S, axis=1), "#a89850", "#d6c67c", "#8a7a40")
    tear = (T.rng("glsm2").random(S) > 0.8)
    base[tear] = base[tear][:, ::-1] * 0.7
    return T.grain(base, "glsm", 0.04)


def stripes():
    """RGB channels pulled apart."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = np.zeros((S, S, 3))
    for c, off in enumerate((0, 3, 7)):
        img[..., c] = ((((xx + off) // 2) % 5) < 2).astype(float) * (0.5 + 0.5 * T.noise("glst" + str(c), sigma=0.3))
    bands = (T.rng("glstb").random(S) > 0.6)
    img[bands] = img[bands][:, ::-1]
    return img * 0.85


def static():
    return T.grain(np.repeat(T.rng("glstat").random((S, S))[..., None], 3, axis=2) * 0.8, "glstat", 0.1)


def doorway():
    """A torn doorway: black with a ragged fringe of the colours of other levels."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = np.zeros((S, S, 3))
    fringe = (xx < 3 + 3 * T.rng("gldr").random((S, 1))) | (xx > 28 - 3 * T.rng("gldr2").random((S, 1)))
    cols = np.array([T.hexc(c) for c in ("#d6c67c", "#6a6a64", "#ff2a6a", "#2affc8", "#7a3a8a")])
    pick = T.rng("gldr3").integers(0, len(cols), (S, S))
    img[fringe] = cols[pick[fringe]]
    img[~fringe] = T.hexc("#020204")
    return img


def carpet_corrupt():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("glcarp", octaves=3, base=5), "#6e5f2e", "#9c8a44")
    blocks = (T.rng("glblk").random((4, 4)) > 0.7)
    m = np.kron(blocks, np.ones((8, 8))).astype(bool)
    img[m] = np.roll(img, 11, axis=1)[m] * np.array([1.2, 0.7, 1.3])
    return np.clip(img, 0, 1)


def concrete_corrupt():
    img = T.ramp(T.fbm("glcon", octaves=4, base=5), "#545450", "#6b6b65")
    rows = (T.rng("glcr").random(S) > 0.75)
    img[rows] = np.roll(img[rows], 9, axis=1)
    img[rows] = img[rows] * np.array([0.6, 1.1, 1.2])
    return np.clip(T.grain(img, "glcon", 0.05), 0, 1)


def wrong_sign():
    img = T.solid("#0a0a14")
    img = text_center(img, "LEVEL", 7, "#e8e8e8")
    img = text_center(img, "???", 15, "#ff3a6a", scale=2)
    return T.grain(img, "glsign", 0.06)


tex("gl_smear", smear)
tex("gl_stripes", stripes)
tex("gl_static", static)
tex("gl_doorway", doorway)
tex("gl_carpet", carpet_corrupt)
tex("gl_concrete", concrete_corrupt)
tex("gl_sign", wrong_sign)

cube("gl_smear", "gl_smear", display="Smeared Wallpaper", sound="wool")
cube("gl_stripes", "gl_stripes", display="Corrupted Block", sound="glass", light=4, emissive=True)
cube("gl_static", "gl_static", display="Static", sound="glass", light=6, emissive=True, flat=True)
cube("gl_carpet", "gl_carpet", display="Corrupted Carpet", sound="wool")
cube("gl_concrete", "gl_concrete", display="Corrupted Concrete")
cube("gl_sign", "gl_sign", display="Level ???", light=5, emissive=True, flat=True)
custom("gl_doorway", "com.noclip.block.GlitchDoorBlock",
       {"parent": "minecraft:block/cube_all", "textures": {"all": "noclip:block/gl_doorway"}},
       display="Torn Doorway", unbreakable=True, no_collision=True, light=3, emissive=True, sound="glass")

# ------------------------------------------------------------------ sound


def chopped():
    """The Lobby's hum, a warehouse, a breath in the dark: cut into pieces and played back wrong."""
    d = 24.0
    t = Y.t_axis(d)
    src = [Y.hum(d, 60.0, "glh", buzz=0.4) * 0.5, Y.lp(Y.brown(d, "glb"), 150) * 0.8,
           Y.bp(Y.white(d, "glw"), 300, 1800) * 0.25, Y.drone(d, [41.2, 43.6], "gld") * 0.4]
    out = np.zeros_like(t)
    r = Y.rng("glchop")
    pos = 0
    while pos < len(t):
        n = int((0.05 + r.random() * 0.9) * Y.SR)
        s = src[int(r.random() * len(src))]
        start = int(r.random() * (len(t) - n - 1))
        seg = s[start:start + n].copy()
        if r.random() < 0.25:
            # stuck buffer: one tiny grain repeated
            g = seg[: int(0.02 * Y.SR)]
            seg = np.tile(g, n // len(g) + 1)[:n]
        if r.random() < 0.2:
            seg = seg[::-1]
        m = min(n, len(t) - pos)
        fade = np.minimum(1, np.minimum(np.arange(m), np.arange(m)[::-1]) / 200.0)
        out[pos:pos + m] += seg[:m] * fade
        pos += m
    x = Y.loopable(Y.norm(Y.lp(out, 6000), 0.5), 0.5)
    return Y.stereoize(x, 0.003), "stereo"


def stutter():
    d = 1.2
    g = Y.bp(Y.white(0.03, "glstut"), 500, 6000)
    x = np.tile(g, int(d / 0.03))
    t = Y.t_axis(len(x) / Y.SR)
    return Y.norm(np.round(x * np.exp(-t * 2) * 8) / 8, 0.6)


def beep():
    d = 0.6
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * 880 * t)) * (t < 0.15) + np.sign(np.sin(2 * np.pi * 440 * t)) * (t > 0.3) * (t < 0.45)
    return Y.norm(np.round(x * 4) / 4 * 0.5, 0.5)


def reverse_whoosh():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "glrw"), 300, 7000) * (t / d) ** 3
    return Y.norm(x, 0.6)


def name_tick():
    d = 0.08
    t = Y.t_axis(d)
    return Y.norm(np.sign(np.sin(2 * np.pi * 2400 * t)) * np.exp(-t * 60), 0.3)


sound("ambient.glitch_level", [("ambient/glitch_level", chopped)], stream=True)
sound("ambient.glitch_level.additions",
      [("ambient/glitch_level_stutter", stutter), ("ambient/glitch_level_beep", beep),
       ("ambient/glitch_level_whoosh", reverse_whoosh)],
      subtitle="The level skips")
sound("event.glitch_level_tick", [("event/glitch_level_tick", name_tick)], subtitle="The name changes")

level("glitch_level", fog="#1a1424", sky="#1a1424", ambient="ambient.glitch_level",
      additions="ambient.glitch_level.additions", additions_chance=0.006, fixed_time=6000, height=48)
