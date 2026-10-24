"""Procedural texture toolkit. Every texture in the mod is drawn by code here.

All functions work on float numpy arrays in [0,1] (H, W, 3) and tile seamlessly
because every noise source wraps around the edges.
"""
import hashlib
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter

S = 32  # default texture size


def rng(*key):
    h = hashlib.sha256("/".join(map(str, key)).encode()).digest()
    return np.random.default_rng(int.from_bytes(h[:8], "little"))


def hexc(c):
    c = c.lstrip("#")
    return np.array([int(c[i:i + 2], 16) / 255.0 for i in (0, 2, 4)])


def noise(seed, size=S, sigma=1.0, shape=None):
    """Periodic gaussian-filtered noise normalised to [0,1]."""
    h, w = shape or (size, size)
    r = rng("noise", seed, h, w, sigma).random((h, w))
    if sigma > 0:
        r = gaussian_filter(r, sigma, mode="wrap")
    lo, hi = r.min(), r.max()
    return (r - lo) / (hi - lo + 1e-9)


def fbm(seed, size=S, octaves=4, base=4.0, shape=None):
    h, w = shape or (size, size)
    out = np.zeros((h, w))
    amp, total = 1.0, 0.0
    sigma = base
    for i in range(octaves):
        out += amp * noise(f"{seed}/{i}", sigma=sigma, shape=(h, w))
        total += amp
        amp *= 0.5
        sigma /= 2.0
    return out / total


def solid(color, size=S, shape=None):
    h, w = shape or (size, size)
    return np.ones((h, w, 3)) * hexc(color)


def ramp(t, *colors):
    """Map scalar field t in [0,1] through a list of colours."""
    cs = [hexc(c) for c in colors]
    t = np.clip(t, 0, 1) * (len(cs) - 1)
    i = np.clip(np.floor(t).astype(int), 0, len(cs) - 2)
    f = (t - i)[..., None]
    a = np.array(cs)[i]
    b = np.array(cs)[i + 1]
    return a * (1 - f) + b * f


def shade(img, amount):
    """Multiply brightness by a scalar field or number (1 = unchanged)."""
    if np.ndim(amount) == 2:
        amount = amount[..., None]
    return np.clip(img * amount, 0, 1)


def blend(img, other, mask):
    if np.ndim(mask) == 2:
        mask = mask[..., None]
    return img * (1 - mask) + other * mask


def grain(img, seed, amount=0.04):
    n = rng("grain", seed).random(img.shape[:2]) - 0.5
    return np.clip(img + n[..., None] * amount, 0, 1)


def stains(img, seed, color="#5a4a20", count=3, strength=0.35, size=6.0):
    """Soft blotchy water stains."""
    h, w = img.shape[:2]
    r = rng("stain", seed)
    m = np.zeros((h, w))
    for _ in range(count):
        cx, cy = r.random() * w, r.random() * h
        rad = size * (0.5 + r.random())
        yy, xx = np.mgrid[0:h, 0:w]
        dx = np.minimum(np.abs(xx - cx), w - np.abs(xx - cx))
        dy = np.minimum(np.abs(yy - cy), h - np.abs(yy - cy))
        d = np.sqrt(dx * dx + dy * dy) / rad
        m = np.maximum(m, np.clip(1 - d, 0, 1))
    edge = noise(f"{seed}/edge", sigma=1.2, shape=(h, w))
    m = np.clip(m * (0.6 + edge * 0.8) - 0.15, 0, 1)
    ring = np.clip(1 - np.abs(m - 0.3) * 6, 0, 1) * 0.5
    return blend(img, solid(color, shape=(h, w)), np.clip(m * strength + ring * strength * 0.6, 0, 1))


def border(img, color, width=1, sides="nesw", alpha=1.0):
    out = img.copy()
    c = hexc(color)
    if "n" in sides:
        out[:width] = out[:width] * (1 - alpha) + c * alpha
    if "s" in sides:
        out[-width:] = out[-width:] * (1 - alpha) + c * alpha
    if "w" in sides:
        out[:, :width] = out[:, :width] * (1 - alpha) + c * alpha
    if "e" in sides:
        out[:, -width:] = out[:, -width:] * (1 - alpha) + c * alpha
    return out


def bevel(img, light=1.12, dark=0.82, width=1):
    out = img.copy()
    out[:width] *= light
    out[:, :width] *= light
    out[-width:] *= dark
    out[:, -width:] *= dark
    return np.clip(out, 0, 1)


def rect(img, x0, y0, x1, y1, color, alpha=1.0):
    out = img.copy()
    c = hexc(color) if isinstance(color, str) else color
    out[y0:y1, x0:x1] = out[y0:y1, x0:x1] * (1 - alpha) + c * alpha
    return out


def vgrad(h, w, top, bottom):
    t = np.linspace(0, 1, h)[:, None] * np.ones((1, w))
    return ramp(t, top, bottom)


def cracks(img, seed, color="#2a2a2a", count=2, alpha=0.6):
    h, w = img.shape[:2]
    r = rng("crack", seed)
    out = img.copy()
    c = hexc(color)
    for _ in range(count):
        x, y = r.random() * w, r.random() * h
        ang = r.random() * np.pi * 2
        for _ in range(int(h * 0.8)):
            ang += (r.random() - 0.5) * 0.9
            x = (x + np.cos(ang)) % w
            y = (y + np.sin(ang)) % h
            out[int(y), int(x)] = out[int(y), int(x)] * (1 - alpha) + c * alpha
    return out


def save(arr, path, alpha=None):
    a = (np.clip(arr, 0, 1) * 255 + 0.5).astype(np.uint8)
    if alpha is not None:
        al = (np.clip(alpha, 0, 1) * 255 + 0.5).astype(np.uint8)
        im = Image.fromarray(np.dstack([a, al]), "RGBA")
    else:
        im = Image.fromarray(a, "RGB")
    im.save(path)
