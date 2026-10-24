"""The Abyss - a fall that never ends, past pieces of every level, toward something looking up."""
import numpy as np
from assets import exit_block, tex, level, sound
import tex as T
import synth as Y
from content.deepend_audio import wrapverb, fit

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def sclera():
    img = T.ramp(T.fbm("absc", octaves=3, base=5) * 0.4 + 0.4, "#c8beb0", "#e8e0d4")
    r = T.rng("abvein")
    for k in range(5):
        x, y = r.random() * 32, r.random() * 32
        ang = r.random() * 6.28
        for i in range(30):
            img[int(y) % S, int(x) % S] = T.hexc("#a02020") * (0.8 + 0.2 * r.random())
            ang += (r.random() - 0.5) * 0.8
            x += np.cos(ang)
            y += np.sin(ang)
    return T.grain(img, "absc", 0.03)


def iris():
    a = np.arctan2(yy - 15.5, xx - 15.5)
    streaks = 0.5 + 0.5 * np.sin(a * 23 + T.noise("abir", sigma=1) * 4)
    img = T.ramp(streaks * 0.6 + T.noise("abir2", sigma=0.7) * 0.4, "#5a1a08", "#c86a18", "#f0b040")
    return T.grain(img, "abir", 0.04)


def pupil():
    img = T.ramp(T.noise("abpu", sigma=3), "#000000", "#06060a")
    return img


def flesh():
    img = T.ramp(T.fbm("abfl", octaves=4, base=3), "#1a0606", "#3a0e0c", "#4a1410")
    folds = np.sin(xx / 3.0 + T.noise("abff", sigma=2) * 6) * 0.08
    return T.grain(T.shade(img, 1 + folds), "abfl", 0.04)


tex("abyss_sclera", sclera)
tex("abyss_iris", iris)
tex("abyss_pupil", pupil)
tex("abyss_flesh", flesh)

exit_block("abyss_eye_sclera", "abyss_sclera", "FALL", display="The Bottom", light=5, emissive=True)
exit_block("abyss_eye_iris", "abyss_iris", "FALL", display="The Bottom", light=13, emissive=True)
exit_block("abyss_eye_pupil", "abyss_pupil", "FALL", display="The Bottom")
exit_block("abyss_eye_flesh", "abyss_flesh", "FALL", display="The Bottom", light=2, emissive=True)


def falling_wind():
    """Air rushing past forever, rising and falling as you tumble, and far below a pressure that is almost a note."""
    d = 24.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.white(d, "abwind"), 200, 3500) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 6.0) ** 2) * 0.5
    roar = Y.lp(Y.brown(d, "abroar"), 150) * 0.7
    note = Y.drone(d, [36.7, 55.0], "abnote", lfo=1 / 12.0) * 0.25
    x = wind + roar + note
    return Y.stereoize(Y.loopable(Y.norm(x, 0.55), 1.5), 0.005), "stereo"


def pass_by(i):
    """Something goes past you on the way down: a hum, a party, an alarm, a drip, for half a second."""
    def f():
        d = 2.0
        t = Y.t_axis(d)
        env = np.exp(-((t - 0.8) ** 2) / 0.08)
        if i == 0:
            x = Y.hum(d, 60.0, "abp0", buzz=0.4)
        elif i == 1:
            x = sum(np.sin(2 * np.pi * f0 * t) for f0 in (392.0, 523.25, 659.25)) * 0.4
        elif i == 2:
            x = np.sign(np.sin(2 * np.pi * np.cumsum(500 + 600 * (t % 1)) / Y.SR)) * 0.3
        else:
            x = Y.bp(Y.white(d, "abp3"), 400, 4000)
        # doppler: the pitch drops as it passes
        warp = np.cumsum(1.15 - 0.3 * np.clip((t - 0.8) * 3 + 0.5, 0, 1)) / Y.SR
        idx = np.clip((warp * Y.SR).astype(int), 0, len(x) - 1)
        return Y.norm(Y.lp(x[idx] * env, 3000), 0.6)
    return f


def heartbeat_slow():
    out = np.zeros(int(3.0 * Y.SR))
    for k in range(2):
        n = int(0.3 * Y.SR)
        tt = np.arange(n) / Y.SR
        th = np.sin(2 * np.pi * (45 - 20 * tt / 0.3) * tt) * np.exp(-tt * 15)
        Y.place(out, th, 0.2 + k * 1.3, 1.0)
        Y.place(out, th * 0.6, 0.45 + k * 1.3, 1.0)
    return Y.reverb(Y.lp(out, 150), 3.0, 0.6, "abhb")


def blink():
    """Below you, something enormous and wet closes and opens."""
    d = 2.5
    t = Y.t_axis(d)
    x = Y.lp(Y.brown(d, "abblink"), 200) * np.exp(-((t - 0.6) ** 2) / 0.05)
    x += Y.lp(Y.white(d, "abbl2"), 600) * np.exp(-((t - 0.9) ** 2) / 0.02) * 0.4
    return Y.reverb(Y.norm(x, 0.8), 4.0, 0.7, "abblink")


sound("ambient.the_abyss", [("ambient/the_abyss", falling_wind)], stream=True)
sound("ambient.the_abyss.additions", [(f"ambient/the_abyss_pass{i}", pass_by(i)) for i in range(4)] +
      [("ambient/the_abyss_heart", heartbeat_slow)], subtitle="Something rushes past")
sound("event.the_abyss_blink", [("event/the_abyss_blink", blink)], subtitle="Something below blinks")

level("the_abyss", fog="#05060a", sky="#05060a", ambient="ambient.the_abyss", additions="ambient.the_abyss.additions",
      additions_chance=0.012, fixed_time=18000, height=384)
