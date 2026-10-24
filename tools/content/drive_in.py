"""Drive-In Theater - a huge screen glowing over rows of empty cars, playing a film of
the lot it stands in: the cars, the speaker posts, and someone walking between them
toward the camera. When the reel runs out, everything goes dark; stand by a speaker."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, pane
from content.ind_kit import anim_tex, text, steel, concrete, rust, vstreaks, glow_spot
import assets as A
import tex as T
import synth as Y

S = 32
COLS, ROWS, FRAMES = 24, 12, 4
W, H = COLS * S, ROWS * S


def _film(i):
    """One frame of the film: the lot from the screen, black and white, and a figure coming closer."""
    yy, xx = np.mgrid[0:H, 0:W].astype(float)
    r = np.random.default_rng(1000 + i)
    sky = 0.92 - 0.2 * (yy / H)
    img = np.where(yy < H * 0.42, sky, 0.0)
    # ground plane with perspective rows of cars
    gy = H * 0.42
    ground = 0.62 + 0.3 * ((yy - gy) / (H - gy))
    img = np.where(yy >= gy, ground, img)
    for row, (ry, scale) in enumerate(((0.48, 0.25), (0.56, 0.4), (0.68, 0.62), (0.84, 0.95))):
        cy = H * ry
        cw, ch = 70 * scale * 2.2, 26 * scale * 2.2
        n = int(W / (cw * 1.6)) + 2
        off = (row * 37) % int(cw)
        for k in range(n):
            cx = k * cw * 1.6 - off
            body = (xx > cx) & (xx < cx + cw) & (yy > cy - ch * 0.55) & (yy < cy)
            roof = (xx > cx + cw * 0.22) & (xx < cx + cw * 0.78) & (yy > cy - ch) & (yy < cy - ch * 0.5)
            img = np.where(body | roof, 0.06 + 0.04 * (k % 3), img)
            post = (np.abs(xx - (cx - cw * 0.3)) < 2 * scale + 1) & (yy > cy - ch * 1.1) & (yy < cy)
            img = np.where(post, 0.05, img)
    # the figure, a little closer every frame
    sc = [0.18, 0.32, 0.55, 0.95][i]
    fx, fy = W * (0.55 - 0.03 * i), H * (0.5 + 0.13 * i)
    fh, fw = 200 * sc, 60 * sc
    head = ((xx - fx) ** 2 / (fw * 0.3) ** 2 + (yy - (fy - fh * 0.85)) ** 2 / (fw * 0.36) ** 2) < 1
    torso = (np.abs(xx - fx) < fw * 0.5 * (1 - 0.3 * (yy - (fy - fh * 0.7)) / fh)) & (yy > fy - fh * 0.7) & (yy < fy)
    img = np.where(head | torso, 0.02, img)
    # film: grain, scratches, gate weave vignette, flicker
    img = img + r.normal(0, 0.05, img.shape)
    for _ in range(3 + i):
        x = int(r.random() * W)
        img[:, x:x + 1] = np.clip(img[:, x:x + 1] + 0.35, 0, 1)
    for _ in range(40):
        x, y = int(r.random() * W), int(r.random() * H)
        img[y:y + 2, x:x + 2] = 0.95 if r.random() < 0.5 else 0.0
    d = np.sqrt(((xx - W / 2) / (W / 2)) ** 2 + ((yy - H / 2) / (H / 2)) ** 2)
    img = img * (1.05 - 0.45 * d ** 2) * (0.92 + 0.08 * (i % 2))
    img = np.clip(img, 0, 1)
    tone = np.stack([img * 1.0 + 0.04, img * 0.97 + 0.03, img * 0.9 + 0.02], axis=-1)
    return np.clip(tone, 0, 1)


_cache = {}


def _frame(i):
    if i not in _cache:
        _cache[i] = _film(i)
    return _cache[i]


SCREEN = []
for r in range(ROWS):
    for c in range(COLS):
        name = f"dri_scr_{c}_{r}"
        anim_tex(name, lambda i, c=c, r=r: _frame(i)[r * S:(r + 1) * S, c * S:(c + 1) * S], FRAMES, frametime=30)
        cube(name, name, display="Drive-In Screen", sound="wool", light=12, emissive=True)
        SCREEN.append(name)


def screen_back():
    img = T.ramp(T.noise("dribk", sigma=1.5), "#4a4a46", "#5a5a54")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.3)
    return vstreaks(rust(img, "dribkr", 0.2), "dribkv", "#2a2622", 3, 0.35)


def screen_frame():
    img = steel("drifr", "#2a2c2e", "#3a3c3e", brushed=False)
    return rust(img, "drifrr", 0.3)


def gravel():
    img = T.ramp(T.noise("drigv", sigma=0.5), "#3a3832", "#5a564c", "#6e6a5e")
    img = T.shade(img, 0.9 + T.noise("drigv2", sigma=3) * 0.2)
    ruts = (np.abs(np.mgrid[0:S, 0:S][1] % 16 - 4) < 2).astype(float)
    return T.grain(T.blend(img, T.solid("#2a2822"), ruts * 0.25), "drigv", 0.05)


def asphalt():
    img = T.ramp(T.fbm("drias", octaves=4, base=4), "#22221f", "#2e2e2a")
    return T.grain(T.cracks(img, "driasc", "#121210", 1, 0.5), "drias", 0.05)


def asphalt_line():
    img = asphalt()
    yy = np.mgrid[0:S, 0:S][0]
    worn = (T.noise("drialw", sigma=0.8) > 0.3).astype(float)
    return T.blend(img, T.solid("#c8c0a0"), ((yy >= 14) & (yy < 18)) * worn * 0.8)


def speaker():
    img = steel("drisp", "#5a5e50", "#6e7262", brushed=False)
    yy, xx = np.mgrid[0:S, 0:S]
    grill = ((xx % 3) == 0) & (yy > 6) & (yy < 22) & (xx > 6) & (xx < 26)
    img = T.blend(img, T.solid("#1a1a18"), grill.astype(float))
    img = T.rect(img, 10, 24, 22, 28, "#e0c070")
    img = text(img, "VOL", 11, 24, "#3a2a10")
    return rust(img, "drispr", 0.3)


def post():
    return rust(steel("dripo", "#4a4e44", "#5a5e54"), "dripor", 0.35)


def booth_wall():
    img = T.ramp(T.noise("dribw", sigma=2), "#b8ac90", "#cabea2")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (((yy % 8) == 0) | (((xx + (yy // 8) * 8) % 16) == 0)) * 0.12)
    return T.stains(img, "dribws", "#6a5a40", 2, 0.35, 8)


def booth_window():
    g = glow_spot((S, S), 16, 16, 18, 1.0)
    img = T.ramp(g, "#3a2a10", "#ffd890", "#ffffff")
    img = T.border(img, "#2a2620", 3)
    return img


def neon(label):
    def f():
        img = T.ramp(T.noise("drine" + label, sigma=2), "#100808", "#1a0c0c")
        img = text(img, label, 16 - (len(label) * 4 - 1) // 2, 13, "#ff4a6a")
        g = glow_spot((S, S), 16, 15, 16, 1.5)
        return T.blend(img, T.solid("#ff4a6a"), g * 0.15)
    return f


def booth_door():
    img = T.ramp(T.noise("dribd", sigma=1.5), "#6a2a20", "#7a3428")
    img = T.border(img, "#2a1410", 2)
    img = T.rect(img, 4, 4, 28, 12, "#e8e0c8")
    img = text(img, "STAFF", 6, 5, "#1a1a1a")
    img = T.rect(img, 22, 18, 26, 21, "#c8a050")
    return img


def lamp():
    g = glow_spot((S, S), 15.5, 15.5, 15, 1.0)
    return T.ramp(g, "#5a3008", "#ffa040", "#fff0c0")


def fence():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("drife", sigma=1), "#5a5e5a", "#7a7e7a")
    a = ((np.abs((xx + yy) % 8 - 4) < 1) | (np.abs((xx - yy) % 8 - 4) < 1) | (yy < 2)).astype(float)
    return rgb, a


for i, (n, fn) in enumerate((("dri_screen_back", screen_back), ("dri_screen_frame", screen_frame),
                             ("dri_gravel", gravel), ("dri_asphalt", asphalt), ("dri_asphalt_line", asphalt_line),
                             ("dri_speaker", speaker), ("dri_post", post), ("dri_booth_wall", booth_wall),
                             ("dri_booth_window", booth_window), ("dri_neon_snacks", neon("SNACKS")),
                             ("dri_booth_door", booth_door), ("dri_lamp", lamp), ("dri_fence", fence))):
    tex(n, fn)

cube("dri_screen_back", "dri_screen_back", display="Screen Back", sound="metal")
cube("dri_screen_frame", "dri_screen_frame", display="Screen Frame", sound="metal")
cube("dri_gravel", "dri_gravel", display="Gravel", sound="gravel")
cube("dri_asphalt", "dri_asphalt", display="Asphalt")
cube("dri_asphalt_line", "dri_asphalt_line", display="Asphalt")
shaped("dri_speaker", [(7, 0, 7, 9, 12, 9, "p"), (4, 10, 6, 12, 16, 10, {"all": "p", "north": "s", "south": "s"})],
       {"p": "dri_post", "s": "dri_speaker"}, display="Speaker Post", sound="metal", unbreakable=True, light=3)
cube("dri_booth_wall", "dri_booth_wall", display="Painted Block")
cube("dri_booth_window", "dri_booth_window", display="Projection Window", sound="glass", light=13, emissive=True)
cube("dri_neon_snacks", "dri_neon_snacks", display="Snack Bar Sign", sound="glass", light=8, emissive=True)
exit_block("dri_booth_door", "dri_booth_door", "DOOR", trigger="USE", solid=True, display="Projection Booth Door",
           sound="wood")
shaped("dri_lamp", [(7, 0, 7, 9, 16, 9, "p"), (4, 13, 2, 12, 16, 14, {"all": "p", "down": "l"})],
       {"p": "dri_post", "l": "dri_lamp"}, display="Lot Lamp", sound="metal", unbreakable=True, light=14,
       emissive=True, collides=False)
cube("dri_pole", "dri_post", display="Pole", sound="metal")
pane("dri_fence", "dri_fence", display="Chain-Link Fence", sound="metal")


# ------------------------------------------------------------------ sound

def film_loop():
    d = 24.0
    t = Y.t_axis(d)
    # the soundtrack, far away across the lot: a slow organ chord that never resolves, warbling
    wow = 1 + 0.004 * np.sin(2 * np.pi * (round(0.5 * d) / d) * t)
    chord = sum(np.sin(2 * np.pi * (round(f * d) / d) * np.cumsum(wow) / Y.SR) * g
                for f, g in ((130.8, 0.2), (155.6, 0.15), (196.0, 0.12), (233.1, 0.08)))
    chord = Y.lp(chord, 1500) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 12.0) ** 2)
    clatter = (np.sin(2 * np.pi * 24 * t) > 0.92) * Y.bp(Y.white(d, "dricl"), 1500, 6000) * 0.08
    crickets = np.zeros(len(t))
    r = Y.rng("dricr")
    for k in range(30):
        n = int(0.25 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = np.sin(2 * np.pi * 4300 * tt) * (np.sin(2 * np.pi * 30 * tt) > 0) * Y.env(n, 0.01, 0.05)
        Y.place(crickets, c, r.random() * (d - 1), 0.04)
    wind = Y.bp(Y.pink(d, "driwind"), 100, 800) * 0.3
    x = Y.reverb(chord * 0.6 + clatter, 2.5, 0.5, "drirv")[:len(t)] + crickets + wind
    return Y.stereoize(Y.loopable(Y.norm(x, 0.55), 1.5), 0.004), "stereo"


def horn():
    d = 1.6
    t = Y.t_axis(d)
    x = (np.sign(np.sin(2 * np.pi * 350 * t)) * 0.3 + np.sign(np.sin(2 * np.pi * 440 * t)) * 0.2) * ((t > 0.1) & (t < 0.9))
    return Y.norm(Y.reverb(Y.lp(x, 2500), 3.0, 0.7, "drihn"), 0.35)


def speaker_crackle():
    d = 2.5
    t = Y.t_axis(d)
    syl = (np.sin(2 * np.pi * 3.5 * t) > 0.2) * (t > 0.2) * (t < 2.0)
    buzz = np.sign(np.sin(2 * np.pi * np.cumsum(120 + 25 * np.sin(2 * np.pi * 0.9 * t)) / Y.SR))
    x = Y.bp(buzz, 400, 2200) * syl * 0.5 + Y.bp(Y.white(d, "drisc"), 1000, 5000) * 0.15
    return Y.norm(Y.bp(x, 300, 3000), 0.4)


def door_slam():
    return Y.norm(Y.reverb(Y.impact("drids", 0.5, ring=0.3), 3.0, 0.6, "dridsr"), 0.45)


def reel_end():
    d = 4.0
    t = Y.t_axis(d)
    rate = 24 * np.clip(1 - t / 3.0, 0.05, 1)
    flap = (np.sin(2 * np.pi * np.cumsum(rate) / Y.SR) > 0.85) * Y.bp(Y.white(d, "drire"), 800, 6000)
    motor = np.sin(2 * np.pi * np.cumsum(60 * np.clip(1 - t / 3.5, 0.1, 1)) / Y.SR) * 0.2
    return Y.norm((flap * 0.6 + motor) * np.clip((d - t) / 0.8, 0, 1), 0.8)


def footsteps():
    out = np.zeros(int(4.0 * Y.SR))
    for k in range(6):
        Y.place(out, Y.footstep(f"drift{k}", "concrete"), 0.2 + k * 0.6, 0.4 + k * 0.1)
    return Y.norm(Y.reverb(Y.bp(out, 200, 6000), 2.0, 0.4, "driftr"), 0.6)


sound("ambient.drive_in", [("ambient/drive_in", film_loop)], stream=True)
sound("ambient.drive_in.additions",
      [("ambient/drive_in_horn", horn), ("ambient/drive_in_speaker", speaker_crackle),
       ("ambient/drive_in_door", door_slam)], subtitle="The film plays to nobody")
sound("event.drive_in_reel_end", [("event/drive_in_reel_end", reel_end)], subtitle="The reel runs out")
sound("event.drive_in_footsteps", [("event/drive_in_footsteps", footsteps)], subtitle="Footsteps on gravel")
sound("event.drive_in_voice", [("event/drive_in_voice", speaker_crackle)], subtitle="The speaker keeps talking")

level("drive_in", fog="#0c0e14", sky="#06080e", ambient="ambient.drive_in", additions="ambient.drive_in.additions",
      additions_chance=0.005, fixed_time=18000, skylight=True, ceiling=False, height=64)
