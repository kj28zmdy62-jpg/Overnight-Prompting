"""Helpers for painting entity textures onto vanilla box UV layouts."""
import numpy as np
import tex as T
from assets import box_uv


class Canvas:
    def __init__(self, w=64, h=64):
        self.rgb = np.zeros((h, w, 3))
        self.a = np.zeros((h, w))

    def fill_rect(self, x, y, w, h, img, alpha=1.0):
        if w <= 0 or h <= 0:
            return
        if isinstance(img, str):
            img = T.solid(img, shape=(h, w))
        elif callable(img):
            img = img(w, h)
        self.rgb[y:y + h, x:x + w] = img[:h, :w]
        if np.ndim(alpha) == 2:
            self.a[y:y + h, x:x + w] = alpha[:h, :w]
        else:
            self.a[y:y + h, x:x + w] = alpha

    def box(self, u, v, w, h, d, all=None, **faces):
        """Paint every face of a box. Face painters are colours, arrays or fn(w, h) -> array."""
        for face, (x, y, fw, fh) in box_uv(u, v, w, h, d).items():
            p = faces.get(face, all)
            if p is None:
                continue
            self.fill_rect(x, y, fw, fh, p)

    def out(self):
        return self.rgb, self.a


def skin(seed, c1, c2, sigma=0.8, spots=None):
    """Fleshy/organic noise painter."""
    def f(w, h):
        n = T.noise(f"{seed}{w}{h}", shape=(h, w), sigma=sigma)
        img = T.ramp(n, c1, c2)
        if spots:
            m = (T.noise(f"{seed}s{w}{h}", shape=(h, w), sigma=0.6) > 0.75).astype(float)
            img = T.blend(img, T.solid(spots, shape=(h, w)), m * 0.5)
        return img
    return f


def flat(c, noise=0.05, seed="flat"):
    def f(w, h):
        return T.grain(T.solid(c, shape=(h, w)), f"{seed}{c}{w}{h}", noise)
    return f
