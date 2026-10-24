"""Helpers shared by the reality-breaking levels (recursion, red_room, ...): a tiny pixel font
and a few painting utilities. Registers nothing on its own."""
import numpy as np
import tex as T

# 3x5 pixel font, rows top to bottom
FONT = {
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
    ".": ["000", "000", "000", "000", "010"], ",": ["000", "000", "000", "010", "100"],
    "!": ["010", "010", "010", "000", "010"], "?": ["110", "001", "010", "000", "010"],
    "-": ["000", "000", "111", "000", "000"], ":": ["000", "010", "000", "010", "000"],
    "'": ["010", "010", "000", "000", "000"], "/": ["001", "001", "010", "100", "100"],
    "%": ["101", "001", "010", "100", "101"], "#": ["101", "111", "101", "111", "101"],
    "=": ["000", "111", "000", "111", "000"], "(": ["010", "100", "100", "100", "010"],
    ")": ["010", "001", "001", "001", "010"], "_": ["000", "000", "000", "000", "111"],
    " ": ["000", "000", "000", "000", "000"],
}


def text_width(s, scale=1):
    return (len(s) * 4 - 1) * scale


def text(img, s, x, y, color, scale=1, alpha=1.0, mirror=False, jitter=None):
    """Draw pixel text onto img (H,W,3) at top-left x,y. Returns a new image."""
    out = img.copy()
    c = T.hexc(color) if isinstance(color, str) else np.asarray(color)
    h, w = out.shape[:2]
    r = T.rng("txt", s, x, y) if jitter else None
    cx = x
    chars = s[::-1] if mirror else s
    for ch in chars.upper():
        g = FONT.get(ch, FONT["?"])
        dy = int(round((r.random() - 0.5) * jitter)) if r is not None else 0
        for gy, row in enumerate(g):
            for gx, bit in enumerate(row[::-1] if mirror else row):
                if bit != "1":
                    continue
                for sy in range(scale):
                    for sx in range(scale):
                        py, px = y + dy + gy * scale + sy, cx + gx * scale + sx
                        if 0 <= py < h and 0 <= px < w:
                            out[py, px] = out[py, px] * (1 - alpha) + c * alpha
        cx += 4 * scale
    return out


def text_center(img, s, y, color, scale=1, alpha=1.0, mirror=False, jitter=None):
    w = img.shape[1]
    return text(img, s, (w - text_width(s, scale)) // 2, y, color, scale, alpha, mirror, jitter)


def split(img, n_x, n_y, size=32):
    """Cut a large painting into block-sized tiles, row-major, top row first."""
    tiles = []
    for j in range(n_y):
        for i in range(n_x):
            tiles.append(img[j * size:(j + 1) * size, i * size:(i + 1) * size].copy())
    return tiles


def disc(h, w, cx, cy, r):
    yy, xx = np.mgrid[0:h, 0:w]
    return ((xx - cx) ** 2 + (yy - cy) ** 2) <= r * r


def ellipse(h, w, cx, cy, rx, ry):
    yy, xx = np.mgrid[0:h, 0:w]
    return (((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2) <= 1.0


def line(img, x0, y0, x1, y1, color, alpha=1.0, width=1):
    out = img.copy()
    c = T.hexc(color) if isinstance(color, str) else np.asarray(color)
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    h, w = out.shape[:2]
    for t in np.linspace(0, 1, n):
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t
        for oy in range(width):
            for ox in range(width):
                px, py = int(round(x)) + ox - width // 2, int(round(y)) + oy - width // 2
                if 0 <= px < w and 0 <= py < h:
                    out[py, px] = out[py, px] * (1 - alpha) + c * alpha
    return out


def wood(seed, lo, hi, shape=(32, 32), grain_dir="v", rings=6.0):
    """Wood grain: stretched noise with ring bands."""
    h, w = shape
    n = T.noise(seed, sigma=0, shape=(h, w))
    from scipy.ndimage import gaussian_filter
    if grain_dir == "v":
        n = gaussian_filter(n, (6, 0.6), mode="wrap")
    else:
        n = gaussian_filter(n, (0.6, 6), mode="wrap")
    n = (n - n.min()) / (n.max() - n.min() + 1e-9)
    band = 0.5 + 0.5 * np.sin(n * rings * np.pi * 2)
    img = T.ramp(n * 0.6 + band * 0.4, lo, hi)
    return T.grain(img, seed, 0.03)
