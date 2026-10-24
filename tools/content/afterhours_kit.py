"""Shared painting helpers for the "empty buildings at night" levels (dead_mall, waiting_room,
school, hospital, supermarket, motel, ...). Registers nothing by itself.

Pixel fonts are drawn by hand so signs, chalkboards and displays stay crisp at 32 px per block
and do not depend on system fonts.
"""
import numpy as np
import tex as T

# 5x7 block capitals for signage
F57 = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["01110", "00100", "00100", "00100", "00100", "00100", "01110"],
    "J": ["00111", "00010", "00010", "00010", "00010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "10001", "11001", "10101", "10011", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
    "3": ["11111", "00010", "00100", "00010", "00001", "10001", "01110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
    " ": ["00000"] * 7,
    ".": ["00000", "00000", "00000", "00000", "00000", "01100", "01100"],
    ",": ["00000", "00000", "00000", "00000", "01100", "00100", "01000"],
    "'": ["00100", "00100", "01000", "00000", "00000", "00000", "00000"],
    "!": ["00100", "00100", "00100", "00100", "00100", "00000", "00100"],
    "?": ["01110", "10001", "00001", "00010", "00100", "00000", "00100"],
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    "&": ["01100", "10010", "10100", "01000", "10101", "10010", "01101"],
    "#": ["01010", "01010", "11111", "01010", "11111", "01010", "01010"],
    ":": ["00000", "01100", "01100", "00000", "01100", "01100", "00000"],
    "/": ["00001", "00010", "00010", "00100", "01000", "01000", "10000"],
    ">": ["01000", "00100", "00010", "00001", "00010", "00100", "01000"],
    "<": ["00010", "00100", "01000", "10000", "01000", "00100", "00010"],
    "=": ["00000", "00000", "11111", "00000", "11111", "00000", "00000"],
    "+": ["00000", "00100", "00100", "11111", "00100", "00100", "00000"],
    "(": ["00010", "00100", "01000", "01000", "01000", "00100", "00010"],
    ")": ["01000", "00100", "00010", "00010", "00010", "00100", "01000"],
}

# 3x5 small capitals for chalk, labels and tiny print
F35 = {
    "A": ["010", "101", "111", "101", "101"], "B": ["110", "101", "110", "101", "110"],
    "C": ["011", "100", "100", "100", "011"], "D": ["110", "101", "101", "101", "110"],
    "E": ["111", "100", "110", "100", "111"], "F": ["111", "100", "110", "100", "100"],
    "G": ["011", "100", "101", "101", "011"], "H": ["101", "101", "111", "101", "101"],
    "I": ["111", "010", "010", "010", "111"], "J": ["001", "001", "001", "101", "010"],
    "K": ["101", "101", "110", "101", "101"], "L": ["100", "100", "100", "100", "111"],
    "M": ["101", "111", "111", "101", "101"], "N": ["110", "101", "101", "101", "101"],
    "O": ["010", "101", "101", "101", "010"], "P": ["110", "101", "110", "100", "100"],
    "Q": ["010", "101", "101", "110", "011"], "R": ["110", "101", "110", "101", "101"],
    "S": ["011", "100", "010", "001", "110"], "T": ["111", "010", "010", "010", "010"],
    "U": ["101", "101", "101", "101", "111"], "V": ["101", "101", "101", "101", "010"],
    "W": ["101", "101", "111", "111", "101"], "X": ["101", "101", "010", "101", "101"],
    "Y": ["101", "101", "010", "010", "010"], "Z": ["111", "001", "010", "100", "111"],
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
    "2": ["110", "001", "010", "100", "111"], "3": ["110", "001", "010", "001", "110"],
    "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "110", "001", "110"],
    "6": ["011", "100", "111", "101", "111"], "7": ["111", "001", "010", "010", "010"],
    "8": ["111", "101", "111", "101", "111"], "9": ["111", "101", "111", "001", "110"],
    " ": ["000"] * 5, ".": ["000", "000", "000", "000", "010"], "!": ["010", "010", "010", "000", "010"],
    "?": ["110", "001", "010", "000", "010"], "-": ["000", "000", "111", "000", "000"],
    ",": ["000", "000", "000", "010", "100"], "'": ["010", "010", "000", "000", "000"],
    ":": ["000", "010", "000", "010", "000"], "/": ["001", "001", "010", "100", "100"],
    ">": ["100", "010", "001", "010", "100"], "<": ["001", "010", "100", "010", "001"],
    "=": ["000", "111", "000", "111", "000"], "+": ["000", "010", "111", "010", "000"],
    "#": ["101", "111", "101", "111", "101"], "(": ["010", "100", "100", "100", "010"],
    ")": ["010", "001", "001", "001", "010"], "&": ["010", "101", "010", "101", "011"],
}


def text_mask(text, h, w, x, y, font=F57, scale=1, spacing=1, jitter=None, seed="txt"):
    """Mask (h, w) with text drawn from (x, y). jitter: max per-glyph vertical wobble (handwriting)."""
    m = np.zeros((h, w))
    r = T.rng("textjit", seed, text)
    cx = x
    for ch in text.upper():
        g = font.get(ch, font.get("?"))
        gh, gw = len(g), len(g[0])
        dy = int(round((r.random() - 0.5) * 2 * jitter)) if jitter else 0
        for yy, row in enumerate(g):
            for xx, c in enumerate(row):
                if c == "1":
                    py, px = y + dy + yy * scale, cx + xx * scale
                    m[max(0, py):max(0, min(h, py + scale)), max(0, px):max(0, min(w, px + scale))] = 1
        cx += (gw + spacing) * scale
    return m


def text_width(text, font=F57, scale=1, spacing=1):
    total = 0
    for ch in text.upper():
        g = font.get(ch, font.get("?"))
        total += (len(g[0]) + spacing) * scale
    return total - spacing * scale


def centered(text, h, w, y, font=F57, scale=1, spacing=1, **kw):
    x = (w - text_width(text, font, scale, spacing)) // 2
    return text_mask(text, h, w, x, y, font, scale, spacing, **kw)


def glow(mask, radius=1.5):
    from scipy.ndimage import gaussian_filter
    g = gaussian_filter(mask.astype(float), radius, mode="constant")
    return np.clip(g / (g.max() + 1e-9), 0, 1)


def tiles(img, size=32):
    """Split a big painted image (H, W, 3) into a row-major list of size x size tiles."""
    out = []
    for ty in range(img.shape[0] // size):
        for tx in range(img.shape[1] // size):
            out.append(img[ty * size:(ty + 1) * size, tx * size:(tx + 1) * size].copy())
    return out


def disc(h, w, cx, cy, r):
    yy, xx = np.mgrid[0:h, 0:w]
    return ((xx - cx) ** 2 + (yy - cy) ** 2) <= r * r


def line_mask(h, w, x0, y0, x1, y1, width=1.0):
    yy, xx = np.mgrid[0:h, 0:w]
    dx, dy = x1 - x0, y1 - y0
    L2 = dx * dx + dy * dy + 1e-9
    t = np.clip(((xx - x0) * dx + (yy - y0) * dy) / L2, 0, 1)
    d = np.sqrt((xx - x0 - t * dx) ** 2 + (yy - y0 - t * dy) ** 2)
    return (d <= width / 2).astype(float)


def grime_bottom(img, seed, height=10, strength=0.25):
    S = img.shape[0]
    yy = np.arange(S)[:, None]
    g = np.clip((yy - (S - height)) / height, 0, 1) * T.noise(seed, sigma=1.5, shape=img.shape[:2])
    return T.shade(img, 1 - g * strength)


# ------------------------------------------------------------------ audio helpers
import synth as Y  # noqa: E402

VOWELS = {  # (F1, F2, F3) in Hz
    "a": (730, 1090, 2440), "e": (530, 1840, 2480), "i": (270, 2290, 3010), "o": (570, 840, 2410),
    "u": (300, 870, 2240), "ae": (660, 1720, 2410), "er": (490, 1350, 1690), "uh": (520, 1190, 2390),
}


def _formant_filter(x, f, bw):
    from scipy.signal import lfilter
    r = np.exp(-np.pi * bw / Y.SR)
    th = 2 * np.pi * f / Y.SR
    a = [1, -2 * r * np.cos(th), r * r]
    return lfilter([1 - r], a, x)


def pa_voice(seed, syllables=8, f0=118.0, rate=0.17, female=False, band=(320, 3200)):
    """Unintelligible public-address speech: a pulse-train voice through moving vowel formants,
    with consonant bursts, band-limited like a ceiling speaker. Mono, about syllables*rate seconds."""
    r = Y.rng("pav" + seed)
    if female:
        f0 *= 1.75
    seg = int(rate * Y.SR)
    n = seg * syllables + int(0.3 * Y.SR)
    t = np.arange(n) / Y.SR
    contour = f0 * (1.08 - 0.18 * t / t[-1])
    bumps = np.zeros(n)
    for k in range(syllables):
        bumps[k * seg:(k + 1) * seg] = (r.random() - 0.4) * 0.12
    from scipy.ndimage import uniform_filter1d
    contour *= 1 + uniform_filter1d(bumps, seg // 2)
    phase = np.cumsum(contour / Y.SR)
    pulse = (np.diff(np.floor(phase), prepend=0) > 0).astype(float)
    pulse = Y.lp(pulse, 3500) * 6 + Y.rng("pavb" + seed).standard_normal(n) * 0.02
    keys = list(VOWELS)
    out = np.zeros(n)
    amp = np.zeros(n)
    prev = VOWELS[keys[int(r.random() * len(keys))]]
    for k in range(syllables):
        v = VOWELS[keys[int(r.random() * len(keys))]]
        i0, i1 = k * seg, min(n, (k + 1) * seg)
        chunk = pulse[i0:i1]
        mix = np.zeros(i1 - i0)
        for j, (fa, fb) in enumerate(zip(prev, v)):
            ya = _formant_filter(chunk, fa, 90 + 40 * j)
            yb = _formant_filter(chunk, fb, 90 + 40 * j)
            w = np.linspace(0, 1, i1 - i0) ** 0.5
            mix += (ya * (1 - w) + yb * w) / (1 + j * 0.6)
        out[i0:i1] = mix
        e = np.sin(np.linspace(0, np.pi, i1 - i0)) ** 0.6
        if r.random() < 0.15:
            e *= 0.2  # a pause between words
        amp[i0:i1] = e
        if r.random() < 0.7:
            cn = int(0.035 * Y.SR)
            burst = Y.bp(Y.white(0.05, f"pac{seed}{k}"), 2500, 7000)[:cn] * np.exp(-np.arange(cn) / (0.01 * Y.SR))
            out[i0:i0 + cn] += burst * 0.25 * np.abs(mix).max()
        prev = v
    x = out * Y.lp(amp, 30)
    x = Y.bp(x, band[0], band[1], order=3)
    x = np.tanh(Y.norm(x, 1.0) * 2.2) * 0.8  # small cheap speaker distortion
    return Y.norm(x, 0.8)


def chime(notes, gap=0.55, decay=1.6, seed="chime"):
    """Soft PA attention chime (vibraphone-like partials)."""
    d = gap * len(notes) + decay + 0.3
    out = np.zeros(int(d * Y.SR))
    for k, f in enumerate(notes):
        n = int((decay + 0.3) * Y.SR)
        tt = np.arange(n) / Y.SR
        tone = (np.sin(2 * np.pi * f * tt) + 0.35 * np.sin(2 * np.pi * f * 4.0 * tt) * np.exp(-tt * 6)
                + 0.15 * np.sin(2 * np.pi * f * 2.01 * tt))
        tone *= np.exp(-tt / (decay / 3)) * (1 - np.exp(-tt * 300))
        Y.place(out, tone, 0.05 + k * gap, 0.8)
    return Y.norm(out, 0.8)


def epiano(freq, dur, vel=1.0):
    """FM electric piano note (Rhodes-ish bell tine)."""
    t = Y.t_axis(dur)
    idx = 2.2 * vel * np.exp(-t * 3.5) + 0.25
    mod = np.sin(2 * np.pi * freq * t) * idx
    car = np.sin(2 * np.pi * freq * t + mod)
    tine = np.sin(2 * np.pi * freq * 7.1 * t) * np.exp(-t * 18) * 0.12
    e = np.exp(-t * 1.1) * (1 - np.exp(-t * 400))
    return (car + tine) * e * vel


def circular_place(buf, clip, at, gain=1.0):
    """Like Y.place but wraps around the end so loops have no gap."""
    i = int(at * Y.SR) % len(buf)
    n = len(clip)
    first = min(n, len(buf) - i)
    buf[i:i + first] += clip[:first] * gain
    done = first
    while done < n:
        k = min(n - done, len(buf))
        buf[:k] += clip[done:done + k] * gain
        done += k


def resample(x, factor):
    """Stretch by factor (>1 = slower and lower), linear interpolation."""
    n = int(len(x) * factor)
    src = np.linspace(0, len(x) - 1, n)
    return np.interp(src, np.arange(len(x)), x)


def wow(x, depth=0.003, rate=0.6, seed="wow"):
    """Tape wow and flutter: slowly varying read position."""
    n = len(x)
    t = np.arange(n) / Y.SR
    r = Y.rng(seed)
    dev = depth * Y.SR * (np.sin(2 * np.pi * rate * t + r.random() * 6) + 0.3 * np.sin(2 * np.pi * 7.3 * t))
    pos = np.clip(np.arange(n) + dev, 0, n - 1)
    return np.interp(pos, np.arange(n), x)


def loop_reverb(x, seconds=2.5, wet=0.4, seed="lrv", damp=4000):
    """Reverb whose tail wraps back to the start, for seamless ambient loops."""
    y = Y.reverb(x, seconds, wet, seed, damp)
    out = y[:len(x)].copy()
    tail = y[len(x):]
    out[:len(tail)] += tail
    return out


NOTE = {n: i for i, n in enumerate(["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"])}


def hz(name):
    """'A4' -> 440.0; accepts sharps (C#4)."""
    octave = int(name[-1])
    return 440.0 * 2 ** ((NOTE[name[:-1]] + 12 * (octave + 1) - 69) / 12)
