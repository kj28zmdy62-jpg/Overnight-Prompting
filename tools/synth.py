"""Procedural audio. Every sound in the mod is synthesised here: hums, drones,
footsteps, impacts, voices made of filtered noise. Output is float32 in [-1,1]."""
import numpy as np
from scipy.signal import butter, sosfilt, fftconvolve

SR = 44100


def t_axis(dur, sr=SR):
    return np.arange(int(dur * sr)) / sr


def rng(seed):
    import hashlib
    h = hashlib.sha256(str(seed).encode()).digest()
    return np.random.default_rng(int.from_bytes(h[:8], "little"))


def white(dur, seed):
    return rng(seed).standard_normal(int(dur * SR)).astype(np.float64)


def pink(dur, seed):
    n = int(dur * SR)
    w = rng(seed).standard_normal(n)
    f = np.fft.rfft(w)
    freqs = np.fft.rfftfreq(n, 1 / SR)
    freqs[0] = 1
    f /= np.sqrt(freqs)
    p = np.fft.irfft(f, n)
    return p / (np.abs(p).max() + 1e-9)


def brown(dur, seed):
    n = int(dur * SR)
    w = rng(seed).standard_normal(n)
    f = np.fft.rfft(w)
    freqs = np.fft.rfftfreq(n, 1 / SR)
    freqs[0] = 1
    f /= freqs
    p = np.fft.irfft(f, n)
    return p / (np.abs(p).max() + 1e-9)


def lp(x, hz, order=2):
    return sosfilt(butter(order, min(hz, SR / 2 - 100), "low", fs=SR, output="sos"), x)


def hp(x, hz, order=2):
    return sosfilt(butter(order, hz, "high", fs=SR, output="sos"), x)


def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, min(hi, SR / 2 - 100)], "band", fs=SR, output="sos"), x)


def env(n, attack=0.01, release=0.1, sr=SR):
    e = np.ones(n)
    a = max(1, int(attack * sr))
    r = max(1, int(release * sr))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r)
    return e


def decay(n, tau, sr=SR):
    return np.exp(-np.arange(n) / (tau * sr))


def norm(x, peak=0.9):
    m = np.abs(x).max()
    return x * (peak / m) if m > 0 else x


def reverb(x, seconds=1.2, wet=0.35, seed="rv", damp=4000):
    """Cheap convolution reverb with a synthetic decaying-noise impulse."""
    n = int(seconds * SR)
    ir = rng(seed).standard_normal(n) * np.exp(-np.arange(n) / (seconds * SR / 5))
    ir = lp(ir, damp)
    ir /= np.abs(ir).sum() ** 0.5 * 4
    y = fftconvolve(x, ir)[: len(x) + n]
    out = np.zeros(len(y))
    out[: len(x)] += x * (1 - wet)
    out += y * wet
    return out


def loopable(x, fade=1.0):
    """Crossfade the tail into the head so the clip loops without a click."""
    f = int(fade * SR)
    head, tail = x[:f], x[-f:]
    ramp = np.linspace(0, 1, f)
    mixed = tail * (1 - ramp) + head * ramp
    return np.concatenate([mixed, x[f:-f]])


def tone(freq, dur, phase=0.0):
    return np.sin(2 * np.pi * freq * t_axis(dur) + phase)


def hum(dur, base=60.0, seed="hum", buzz=0.25, harmonics=(1, 2, 3, 4, 5, 6, 8, 10), bright=1.0):
    """Mains hum with fluorescent ballast buzz. Uses integer cycles so it loops."""
    t = t_axis(dur)
    out = np.zeros_like(t)
    r = rng(seed)
    for i, h in enumerate(harmonics):
        amp = (1.0 / (h ** (1.2 / bright))) * (0.6 + 0.4 * r.random())
        out += amp * np.sin(2 * np.pi * base * h * t + r.random() * 6.28)
    # ballast buzz: rectified, slightly gritty 120 Hz pulse
    saw = (t * base * 2) % 1.0
    pulse = np.exp(-saw * 18) * buzz
    out += hp(pulse, 400) * 0.8
    return out


def stereoize(x, width=0.003, seed="st"):
    d = int(width * SR)
    l = x
    r = np.concatenate([np.zeros(d), x[:-d]]) if d > 0 else x
    return np.stack([l, r], axis=1)


def footstep(seed, surface="carpet", gain=1.0):
    n = int(0.35 * SR)
    w = rng(seed).standard_normal(n)
    if surface == "carpet":
        x = lp(w, 900) * decay(n, 0.03)
        thump = lp(rng(seed + "t").standard_normal(n), 120) * decay(n, 0.05) * 3
        x = x + thump
    elif surface == "concrete":
        x = bp(w, 300, 5000) * decay(n, 0.02) + lp(w, 200) * decay(n, 0.04)
    elif surface == "water":
        x = bp(w, 600, 6000) * decay(n, 0.08) * (0.5 + 0.5 * np.sin(np.arange(n) / 90.0))
    elif surface == "metal":
        x = bp(w, 200, 6000) * decay(n, 0.03)
        for f in (430, 910, 1650):
            x += np.sin(2 * np.pi * f * np.arange(n) / SR) * decay(n, 0.12) * 0.3
    elif surface == "tile":
        x = bp(w, 800, 9000) * decay(n, 0.015) + lp(w, 300) * decay(n, 0.03) * 0.6
    else:
        x = lp(w, 2000) * decay(n, 0.03)
    return norm(x, 0.8 * gain)


def place(buf, clip, at, gain=1.0):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    if i < len(buf):
        buf[i:j] += clip[: j - i] * gain


def impact(seed, size=1.0, ring=0.0):
    n = int((0.8 + size) * SR)
    w = rng(seed).standard_normal(n)
    x = lp(w, 300 / size) * decay(n, 0.12 * size) * 2 + bp(w, 200, 3000) * decay(n, 0.02)
    if ring:
        for f in (110, 233, 371):
            x += np.sin(2 * np.pi * f / size * np.arange(n) / SR) * decay(n, 0.6) * ring
    return norm(x)


def drone(dur, freqs, seed="drone", detune=0.3, lfo=0.05):
    t = t_axis(dur)
    out = np.zeros_like(t)
    r = rng(seed)
    for f in freqs:
        for k in range(3):
            ff = f + (r.random() - 0.5) * detune
            # round to whole cycles over dur so it loops
            ff = round(ff * dur) / dur
            out += np.sin(2 * np.pi * ff * t + r.random() * 6.28) / 3
    lfo_f = round(lfo * dur) / dur if lfo > 0 else 0
    out *= 0.75 + 0.25 * np.sin(2 * np.pi * lfo_f * t)
    return out


def whisper(seed, dur=1.6):
    """Breathy formant-filtered noise that reads as unintelligible speech."""
    n = int(dur * SR)
    w = rng(seed).standard_normal(n)
    r = rng(seed + "f")
    out = np.zeros(n)
    seg = int(0.11 * SR)
    for i in range(0, n - seg, seg):
        f1 = 300 + r.random() * 600
        f2 = 900 + r.random() * 1800
        s = bp(w[i:i + seg * 2][:seg], f1, f1 * 1.6) * 0.6 + bp(w[i:i + seg], f2, f2 * 1.3) * 0.4
        e = np.sin(np.linspace(0, np.pi, seg)) * (0.3 + r.random())
        out[i:i + seg] += s * e
    return norm(hp(out, 250) * env(n, 0.1, 0.3), 0.6)


def write(path, x, stereo=False):
    import subprocess, tempfile, os, wave
    x = np.asarray(x, dtype=np.float64)
    if stereo and x.ndim == 1:
        x = stereoize(x)
    if not stereo and x.ndim == 2:
        x = x.mean(axis=1)
    x = np.clip(x, -1, 1)
    data = (x * 32767).astype("<i2")
    ch = 2 if x.ndim == 2 else 1
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        tmp = f.name
    with wave.open(tmp, "wb") as wv:
        wv.setnchannels(ch)
        wv.setsampwidth(2)
        wv.setframerate(SR)
        wv.writeframes(data.tobytes())
    os.makedirs(os.path.dirname(path), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.unlink(tmp)
