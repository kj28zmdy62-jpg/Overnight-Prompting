"""Shared helpers for the empty-building levels (daycare .. call_center): a tiny pixel
font for painted numbers and signs, and a few texture/audio recipes. Registers nothing."""
import numpy as np
import tex as T
import synth as Y

S = 32

# 5x7 pixel font. Each glyph is 7 rows of 5 chars.
FONT = {
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
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
    "?": ["01110", "10001", "00001", "00010", "00100", "00000", "00100"],
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    "!": ["00100", "00100", "00100", "00100", "00100", "00000", "00100"],
    ".": ["00000", "00000", "00000", "00000", "00000", "00000", "00100"],
    ":": ["00000", "00100", "00000", "00000", "00000", "00100", "00000"],
    "/": ["00001", "00010", "00010", "00100", "01000", "01000", "10000"],
    "#": ["01010", "11111", "01010", "01010", "01010", "11111", "01010"],
    "^": ["00100", "01110", "10101", "00100", "00100", "00100", "00100"],
    "v": ["00100", "00100", "00100", "00100", "10101", "01110", "00100"],
    "<": ["00010", "00100", "01000", "10000", "01000", "00100", "00010"],
    ">": ["01000", "00100", "00010", "00001", "00010", "00100", "01000"],
    " ": ["00000"] * 7,
}


def glyph_mask(ch):
    rows = FONT.get(ch, FONT["?"])
    return np.array([[c == "1" for c in r] for r in rows], dtype=float)


def text_mask(text, h=S, w=S, x=0, y=0, scale=1, spacing=1):
    """Boolean-ish mask of text drawn at (x, y) with integer pixel scale."""
    m = np.zeros((h, w))
    cx = x
    for ch in text:
        g = glyph_mask(ch)
        g = np.kron(g, np.ones((scale, scale)))
        gh, gw = g.shape
        ya, xa = max(0, y), max(0, cx)
        yb, xb = min(h, y + gh), min(w, cx + gw)
        if xb > xa and yb > ya:
            m[ya:yb, xa:xb] = np.maximum(m[ya:yb, xa:xb], g[ya - y:yb - y, xa - cx:xb - cx])
        cx += gw + spacing * scale
    return m


def text_width(text, scale=1, spacing=1):
    return len(text) * (5 + spacing) * scale - spacing * scale


def centered(text, h=S, w=S, y=None, scale=1, spacing=1):
    tw = text_width(text, scale, spacing)
    th = 7 * scale
    return text_mask(text, h, w, (w - tw) // 2, (h - th) // 2 if y is None else y, scale, spacing)


def ink(img, mask, color, alpha=1.0, wear_seed=None, wear=0.0):
    """Paint mask onto img; optional wear knocks out random pixels like old stencil paint."""
    m = mask * alpha
    if wear_seed is not None and wear > 0:
        n = T.noise(wear_seed, sigma=0.7, shape=mask.shape)
        m = m * (n > wear)
    return T.blend(img, T.solid(color, shape=mask.shape), m)


def brushed(seed, lo, hi, vertical=True, shape=(S, S)):
    """Brushed metal: streaky noise along one axis."""
    h, w = shape
    r = T.rng("brush", seed).random((h, w))
    from scipy.ndimage import gaussian_filter
    r = gaussian_filter(r, (6, 0.5) if vertical else (0.5, 6), mode="wrap")
    r = (r - r.min()) / (r.max() - r.min() + 1e-9)
    return T.ramp(r, lo, hi)


def tiles(seed, color_lo, color_hi, grout, n=4, grout_w=1, shape=(S, S), bevel=0.06):
    """Square ceramic tiles, n per side, with grout and per-tile tone variation."""
    h, w = shape
    yy, xx = np.mgrid[0:h, 0:w]
    ts = w // n
    tid = (yy // ts) * n + (xx // ts)
    r = T.rng("tiles", seed)
    tone = r.random(n * n)[tid]
    img = T.ramp(tone * 0.6 + T.noise(seed + "tn", sigma=1.5, shape=shape) * 0.4, color_lo, color_hi)
    gm = ((yy % ts) < grout_w) | ((xx % ts) < grout_w)
    hi = ((yy % ts) == grout_w) | ((xx % ts) == grout_w)
    lo = ((yy % ts) == ts - 1) | ((xx % ts) == ts - 1)
    img = T.shade(img, 1 + hi * bevel - lo * bevel)
    return T.blend(img, T.solid(grout, shape=shape), gm.astype(float))


def wood(seed, lo, hi, plank=8, vertical=False, shape=(S, S)):
    h, w = shape
    yy, xx = np.mgrid[0:h, 0:w]
    a, b = (xx, yy) if vertical else (yy, xx)
    pid = a // plank
    r = T.rng("wood", seed)
    off = r.random(64)[pid % 64]
    grain = np.sin((b / w * 2 * np.pi * (2 + off * 3)) + (a % plank) * 0.9 + off * 6) * 0.5 + 0.5
    from scipy.ndimage import gaussian_filter
    fib = T.rng("woodf", seed).random((h, w))
    fib = gaussian_filter(fib, (0.4, 3) if not vertical else (3, 0.4), mode="wrap")
    fib = (fib - fib.min()) / (fib.max() - fib.min() + 1e-9)
    t = grain * 0.35 + fib * 0.45 + off * 0.2
    img = T.ramp(t, lo, hi)
    seam = (a % plank) == 0
    return T.shade(img, 1 - seam * 0.3)


# ------------------------------------------------------------------ audio


def wow(x, depth=0.004, rate=0.31, seed="wow"):
    """Tape wow: resample with a slowly wandering read head."""
    n = len(x)
    t = np.arange(n) / Y.SR
    r = Y.rng(seed)
    drift = np.sin(2 * np.pi * rate * t + r.random() * 6) + 0.5 * np.sin(2 * np.pi * rate * 2.3 * t + r.random() * 6)
    pos = np.arange(n) + np.cumsum(drift * depth)
    pos = np.clip(pos, 0, n - 1)
    return np.interp(pos, np.arange(n), x)


def epiano(freq, dur, seed="ep", bright=1.0):
    t = Y.t_axis(dur)
    mod = np.sin(2 * np.pi * freq * 1.0 * t) * np.exp(-t * 3) * 1.6 * bright
    x = np.sin(2 * np.pi * freq * t + mod) * np.exp(-t * 1.4)
    x += 0.25 * np.sin(2 * np.pi * freq * 2 * t) * np.exp(-t * 3)
    return x * Y.env(len(t), 0.004, 0.2)


def chime(freqs, dur=2.5, decay=1.6):
    t = Y.t_axis(dur)
    out = np.zeros_like(t)
    for i, f in enumerate(freqs):
        start = int(i * 0.45 * Y.SR)
        tt = t[: len(t) - start]
        tone = (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt * 4)
                + 0.15 * np.sin(2 * np.pi * f * 5.4 * tt) * np.exp(-tt * 8)) * np.exp(-tt * decay)
        out[start:] += tone
    return out


def voice(seed, dur=1.6, pitch=120.0, formants=None, breath=0.15):
    """A crude vowel-ish voice: a glottal pulse train through moving formants. Unintelligible on purpose."""
    n = int(dur * Y.SR)
    t = np.arange(n) / Y.SR
    r = Y.rng(seed)
    f0 = pitch * (1 + 0.06 * np.sin(2 * np.pi * 0.7 * t + r.random() * 6)) * (1 - 0.12 * t / dur)
    ph = np.cumsum(f0) / Y.SR
    pulse = (ph % 1.0)
    src = (1 - pulse) ** 6 - 0.15
    src += Y.white(dur, seed + "br") * breath
    out = np.zeros(n)
    seg = int(0.13 * Y.SR)
    vowels = formants or [(700, 1200), (400, 2000), (300, 900), (550, 1700), (650, 1050)]
    for i in range(0, n, seg):
        f1, f2 = vowels[int(r.random() * len(vowels))]
        s = src[i:i + seg * 2]
        part = Y.bp(s, f1 * 0.8, f1 * 1.25) + 0.6 * Y.bp(s, f2 * 0.85, f2 * 1.2)
        e = np.sin(np.linspace(0, np.pi, len(part))) ** 0.7 * (0.4 + 0.6 * r.random())
        j = min(n, i + len(part))
        out[i:j] += (part * e)[: j - i]
    return Y.norm(out * Y.env(n, min(0.03, dur / 4), min(0.15, dur / 3)), 0.7)


def speaker(x, lo=350, hi=3200):
    """Through a cheap ceiling speaker / telephone."""
    y = Y.bp(x, lo, hi)
    y = np.tanh(y * 2.5) / 2.5
    return y
