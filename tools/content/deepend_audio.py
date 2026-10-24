"""Audio helpers shared by the deep-end levels. No sounds are registered here."""
import numpy as np
import synth as Y


def wrapverb(x, seconds=2.0, wet=0.4, seed="wv", damp=4000):
    """Reverb whose tail wraps around to the start, so a loop stays seamless and the same length."""
    n = len(x)
    y = Y.reverb(x, seconds, wet, seed, damp)
    out = y[:n].copy()
    tail = y[n:]
    while len(tail):
        k = min(n, len(tail))
        out[:k] += tail[:k]
        tail = tail[k:]
    return out


def fit(x, n):
    """Pad or trim to exactly n samples."""
    if len(x) >= n:
        return x[:n]
    return np.concatenate([x, np.zeros(n - len(x))])
