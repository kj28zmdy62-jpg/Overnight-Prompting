"""Entity voices: all synthesised, all mono so they position in 3D."""
import numpy as np
from assets import sound
import synth as Y


def _howl(i):
    def f():
        d = 2.6
        t = Y.t_axis(d)
        f0 = 180 + i * 25 + 140 * np.sin(np.pi * t / d) ** 1.5
        ph = 2 * np.pi * np.cumsum(f0) / Y.SR
        voice = np.sin(ph) + 0.5 * np.sin(2 * ph + 0.3) + 0.3 * np.sin(3 * ph)
        voice *= np.sin(np.pi * t / d) ** 0.6
        breath = Y.bp(Y.white(d, f"howlb{i}"), 400, 2400) * 0.3 * np.sin(np.pi * t / d)
        rasp = 1 + 0.4 * np.sin(2 * np.pi * 31 * t)
        x = Y.lp(voice * rasp, 2500) + breath
        return Y.norm(Y.reverb(x, 1.8, 0.35, f"howl{i}"), 0.9)
    return f


def _growl(i):
    def f():
        d = 1.6
        t = Y.t_axis(d)
        x = Y.lp(Y.white(d, f"gr{i}"), 260) * (0.6 + 0.4 * np.sin(2 * np.pi * (23 + i * 4) * t))
        x *= np.sin(np.pi * t / d)
        return Y.norm(x, 0.8)
    return f


def _hurt():
    d = 0.5
    t = Y.t_axis(d)
    f0 = 420 - 260 * t / d
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * np.exp(-t * 6)
    x += Y.bp(Y.white(d, "hh"), 600, 3000) * np.exp(-t * 10) * 0.5
    return Y.norm(x, 0.9)


def _scream():
    d = 1.8
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for k, base in enumerate((880, 1046, 1318)):
        f0 = base * (1 + 0.02 * np.sin(2 * np.pi * 7 * t + k))
        x += np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) / (k + 1)
    x += Y.hp(Y.white(d, "scr"), 2500) * 0.4
    x *= (1 - np.exp(-t * 30)) * np.exp(-t * 1.1)
    return Y.norm(Y.reverb(x, 1.2, 0.3, "scr"), 0.95)


def _vanish():
    d = 1.2
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "van"), 300, 5000) * np.exp(-t * 4)
    x = Y.reverb(x[::-1].copy(), 1.0, 0.5, "van")
    return Y.norm(x[: int(d * Y.SR)], 0.5)


def _creak(i):
    def f():
        d = 0.8
        n = int(d * Y.SR)
        t = Y.t_axis(d)
        clicks = np.zeros(n)
        rate = 90 + i * 30
        idx = (np.cumsum(np.ones(n) * rate / Y.SR * (1 + 0.5 * np.sin(2 * np.pi * 1.3 * t))) % 1) < (rate / Y.SR)
        clicks[idx] = 1
        x = Y.bp(clicks, 500, 3500) * np.sin(np.pi * t / d)
        return Y.norm(x, 0.8)
    return f


def _giggle(i):
    def f():
        d = 1.2
        out = np.zeros(int(d * Y.SR))
        for k in range(4 + i % 3):
            nn = int(0.09 * Y.SR)
            tt = np.arange(nn) / Y.SR
            f0 = 520 + i * 40 + k * 15
            syl = np.sin(2 * np.pi * f0 * tt) * np.sin(np.pi * tt / tt[-1]) ** 2
            syl += Y.bp(Y.white(0.09, f"gg{i}{k}"), 1500, 4000)[:nn] * 0.2
            Y.place(out, syl, 0.05 + k * 0.13, 0.8 - k * 0.08)
        return Y.norm(Y.reverb(out, 0.8, 0.2, f"gig{i}"), 0.85)
    return f


def _flutter():
    d = 1.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "flut"), 80, 900) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 18 * t)))
    x *= np.sin(np.pi * t / d)
    return Y.norm(x, 0.7)


sound("entity.hound.howl", [(f"entity/hound_howl{i}", _howl(i)) for i in range(2)], subtitle="A Hound howls")
sound("entity.hound.ambient", [(f"entity/hound_growl{i}", _growl(i)) for i in range(3)], subtitle="Something growls")
sound("entity.hound.hurt", [("entity/hound_hurt", _hurt)], subtitle="Hound yelps")
sound("entity.smiler.scream", [("entity/smiler_scream", _scream)], subtitle="A Smiler shrieks")
sound("entity.smiler.vanish", [("entity/smiler_vanish", _vanish)], subtitle="Something fades")
sound("entity.mannequin.creak", [(f"entity/mannequin_creak{i}", _creak(i)) for i in range(3)], subtitle="Plastic creaks")
sound("entity.partygoer.giggle", [(f"entity/partygoer_giggle{i}", _giggle(i)) for i in range(3)], subtitle="Partygoer giggles")
sound("entity.deathmoth.flutter", [("entity/deathmoth_flutter", _flutter)], subtitle="Wings flutter")
