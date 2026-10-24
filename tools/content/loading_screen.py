"""Loading Screen - islands of dirt-background in the dark, each with a progress bar that never finishes.
The bridges between them load in with the bar, a slice at a time, and unload again at a hundred percent.
Cross while it is loaded. Somewhere there is a Done button."""
import numpy as np
from assets import cube, tex, shaped, exit_block, level, sound
import tex as T
import synth as Y
from content.rb_shared import text, text_center, split

S = 32


def dirt_bg(dark=0.25, seed="lsdirt"):
    """The options-screen dirt: chunky brown pixels, darkened as if behind a menu."""
    r = T.rng(seed)
    small = r.random((8, 8))
    img = T.ramp(np.kron(small, np.ones((4, 4))), "#3a2a1a", "#5a4028", "#7a5838")
    specks = np.kron((r.random((8, 8)) > 0.86).astype(float), np.ones((4, 4)))
    img = T.blend(img, T.solid("#8a7a6a"), specks * 0.6)
    return T.shade(img, 1 - dark)


def chunk_tile():
    """Grass, freshly streamed in: chunk borders still drawn on it, like the debug overlay left on."""
    yy, xx = np.mgrid[0:S, 0:S]
    r = T.rng("lschunk")
    img = T.ramp(np.kron(r.random((8, 8)), np.ones((4, 4))), "#4a7a2a", "#5e9036", "#6ea040")
    img[(xx == 0) | (yy == 0)] = T.hexc("#f0e040")
    img[(xx == 16) | (yy == 16)] = T.hexc("#40a0f0") * 0.8 + img[(xx == 16) | (yy == 16)] * 0.2
    return img


def bar(full):
    img = T.solid("#202020")
    img[:9] = dirt_bg(0.25, "lsbar")[:9]
    img[-9:] = dirt_bg(0.25, "lsbar2")[-9:]
    img[9:23] = T.hexc("#e0e0e0")
    img[11:21] = T.hexc("#80ff20" if full else "#1a1a1a")
    if full:
        img[11:13] = T.hexc("#c0ff80")
    return img


def caption(lines, w_tiles, seed):
    W = 32 * w_tiles
    base = np.concatenate([dirt_bg(0.25, seed + str(i)) for i in range(w_tiles)], axis=1)
    img = base.copy()
    y = 6 if len(lines) == 2 else 12
    for ln, (txt, col) in enumerate(lines):
        img = text_center(img, txt, y + ln * 12, col, scale=2 if ln == 0 else 1)
    return img


def done_button(part):
    W = 96
    img = np.ones((32, W, 3)) * T.hexc("#6e6e6e")
    img[:2] = T.hexc("#a8a8a8")
    img[-3:] = T.hexc("#3a3a3a")
    img[:, :2] = T.hexc("#a8a8a8")
    img[:, -2:] = T.hexc("#3a3a3a")
    img = T.shade(img, 0.95 + T.noise("lsbtn", shape=(32, W), sigma=1) * 0.08)
    img = text_center(img, "DONE", 10, "#ffffa0", scale=3)
    return split(img, 3, 1)[part]


tex("ls_dirt", lambda: dirt_bg(0.25))
tex("ls_dirt_dark", lambda: dirt_bg(0.55, "lsdd"))
tex("ls_chunk", chunk_tile)
tex("ls_bar_empty", lambda: bar(False))
tex("ls_bar_full", lambda: bar(True))
CAPTIONS = {
    "load": [("LOADING TERRAIN...", "#ffffff")],
    "build": [("BUILDING TERRAIN", "#ffffff"), ("PREPARING SPAWN AREA: 99%", "#a0a0a0")],
    "save": [("SAVING WORLD", "#ffffff"), ("DO NOT TURN OFF", "#a0a0a0")],
}
for key, lines in CAPTIONS.items():
    for i in range(5):
        tex(f"ls_cap_{key}_{i}", (lambda l, k, ii: lambda: split(caption(l, 5, "lscap" + k), 5, 1)[ii])(lines, key, i))
for i, q in enumerate(("l", "m", "r")):
    tex(f"ls_done_{q}", (lambda ii: lambda: done_button(ii))(i))

cube("ls_dirt", "ls_dirt", display="Options Background", sound="gravel", flat=True)
cube("ls_dirt_dark", "ls_dirt_dark", display="Options Background", sound="gravel", flat=True)
cube("ls_chunk", "ls_chunk", display="Freshly Loaded Chunk", sound="grass")
for state in ("empty", "full"):
    shaped(f"ls_bar_{state}", [(0, 0, 0, 16, 16, 1, {"all": "edge", "south": "bar"})],
           {"bar": f"ls_bar_{state}", "edge": "ls_dirt"}, display="Progress Bar", collides=False, unbreakable=True,
           light=8 if state == "full" else None, emissive=True if state == "full" else None, sound="glass")
for key in CAPTIONS:
    for i in range(5):
        shaped(f"ls_cap_{key}_{i}", [(0, 0, 0, 16, 16, 1, {"all": "edge", "south": "t"})],
               {"t": f"ls_cap_{key}_{i}", "edge": "ls_dirt"}, display="Loading", collides=False, unbreakable=True,
               light=5, emissive=True, sound="glass")
for q in ("l", "m", "r"):
    exit_block(f"ls_done_{q}", f"ls_done_{q}", "DOOR", trigger="USE", solid=True, display="Done", sound="stone",
               light=6, emissive=True)

# ------------------------------------------------------------------ sound


def drive():
    """A machine trying very hard: a fan, a hard drive seeking, seeking, seeking."""
    d = 24.0
    t = Y.t_axis(d)
    fan = Y.bp(Y.pink(d, "lsfan"), 120, 900) * 0.25 + np.sin(2 * np.pi * round(118 * d) / d * t) * 0.05
    out = np.zeros_like(t)
    r = Y.rng("lsseek")
    for k in range(70):
        n = int(0.012 * Y.SR)
        tt = np.arange(n) / Y.SR
        click = Y.bp(Y.white(0.012, f"lsk{k}"), 1500, 7000)[:n] * np.exp(-tt * 400)
        Y.place(out, click, r.random() * (d - 0.1), 0.2 + r.random() * 0.4)
    x = Y.loopable(Y.norm(fan + out * 0.5, 0.4), 1.0)
    return Y.stereoize(x, 0.0008), "stereo"


def grind():
    d = 1.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "lsgr"), 800, 4000) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 23 * t)))
    return Y.norm(x * np.sin(np.pi * t / d), 0.4)


def spinup():
    d = 3.0
    t = Y.t_axis(d)
    f = 40 + 260 * (t / d) ** 0.5
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.4 + Y.bp(Y.pink(d, "lsspin"), 200, 2000) * (t / d) * 0.4
    return Y.norm(x * np.sin(np.pi * t / d), 0.5)


def pop_in():
    d = 0.5
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * (180 - 120 * t / d) * t) * np.exp(-t * 14)
    x += Y.lp(Y.white(d, "lspop"), 1200) * np.exp(-t * 30) * 0.6
    return Y.norm(x, 0.6)


def unload():
    d = 1.2
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "lsun"), 300, 5000) * np.exp(-t * 3)
    x = np.round(x * 6) / 6
    x += np.sin(2 * np.pi * (300 * np.exp(-t * 3)) * t) * np.exp(-t * 2) * 0.4
    return Y.norm(x, 0.7)


sound("ambient.loading_screen", [("ambient/loading_screen", drive)], stream=True)
sound("ambient.loading_screen.additions", [("ambient/loading_screen_grind", grind), ("ambient/loading_screen_spinup", spinup)],
      subtitle="The machine strains")
sound("event.loading_screen_pop", [("event/loading_screen_pop", pop_in)], subtitle="Terrain loads in")
sound("event.loading_screen_unload", [("event/loading_screen_unload", unload)], subtitle="Terrain unloads")

level("loading_screen", fog="#0a0806", sky="#0a0806", ambient="ambient.loading_screen",
      additions="ambient.loading_screen.additions", additions_chance=0.004, fixed_time=18000, height=48, ambient_light=0.35)
