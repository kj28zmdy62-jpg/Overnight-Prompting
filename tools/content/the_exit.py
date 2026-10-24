"""The Exit - the secret ending. The Lobby fades to white, opens onto a meadow, and there is a door."""
import numpy as np
from assets import cube, cube_faces, tex, exit_block, level, sound, cross, flicker_light
import tex as T
import synth as Y
from content.deepend_audio import wrapverb, fit
from content.level_0 import wallpaper_base_pattern, carpet as l0_carpet, ceiling as l0_ceiling, light_on as l0_light

S = 32
yy, xx = np.mgrid[0:S, 0:S]
WHITE = T.hexc("#f6f5f0")
STAGES = 5


def fade(img, k):
    """k=0 is the Lobby as you know it; k=4 is nothing but white."""
    t = k / (STAGES - 1)
    grey = img.mean(axis=2, keepdims=True) * np.ones((1, 1, 3))
    img = img * (1 - t * 0.8) + grey * t * 0.8
    return np.clip(img * (1 - t) + (WHITE * (0.92 + 0.08 * img.mean(axis=2, keepdims=True))) * t, 0, 1)


def wall(k):
    return lambda: fade(wallpaper_base_pattern(f"ex{k}"), k)


def base(k):
    def f():
        img = wallpaper_base_pattern(f"exb{k}")
        board = T.ramp(T.fbm("exboard", octaves=3, base=2), "#5a4630", "#6e573a")
        img[S - 6:] = board[S - 6:]
        return fade(img, k)
    return f


def floor(k):
    return lambda: fade(l0_carpet(), k)


def meadow():
    img = T.ramp(T.fbm("exmd", octaves=4, base=3) * 0.6 + T.noise("exmdn", sigma=0.4) * 0.4, "#7aa848", "#8eba54", "#a4c860")
    return T.grain(img, "exmd", 0.04)


def meadow_side():
    img = T.ramp(T.fbm("exmds", octaves=3, base=3), "#6a5038", "#7a5e42")
    img[:4] = meadow()[:4]
    return img


def flower(c):
    def f():
        rgb = np.zeros((S, S, 3))
        a = np.zeros((S, S))
        for (sx, top) in ((10, 12), (21, 8)):
            rgb[top:, sx] = T.hexc("#5a8a3a")
            a[top:, sx] = 1
            d = np.sqrt((xx - sx) ** 2 + (yy - top) ** 2)
            m = d < 3.2
            rgb[m] = T.hexc(c)
            a[m] = 1
            rgb[d < 1.2] = T.hexc("#f0c838")
        return rgb, a
    return f


def tall_grass():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("extg")
    for k in range(14):
        x0 = int(r.random() * 30) + 1
        h = 10 + int(r.random() * 20)
        for y in range(S - h, S):
            x = int(x0 + (S - y) * (r.random() - 0.5) * 0.15)
            rgb[y, x % S] = T.hexc("#86b450") * (0.8 + 0.2 * r.random())
            a[y, x % S] = 1
    return rgb, a


def door_tex(top):
    def f():
        img = T.ramp(T.fbm("exdoor", octaves=3, base=3), "#6a4a2a", "#7e5a34")
        img = T.shade(img, 1 - ((xx % 8) == 0) * 0.2)
        # daylight leaking through the gaps around the door
        glow = ((xx < 2) | (xx > 29)).astype(float)
        if top:
            glow = np.maximum(glow, (yy < 2).astype(float))
        img = T.blend(img, T.solid("#fff8e0"), glow)
        if not top:
            img = T.rect(img, 24, 2, 27, 5, "#c8b070")
        return T.grain(img, "exdoor" + str(top), 0.03)
    return f


def frame_tex():
    img = T.ramp(T.noise("exframe", sigma=2), "#ece8de", "#f8f4ea")
    return T.grain(T.border(img, "#d8d4ca", 1, sides="we"), "exframe", 0.02)


def glare():
    return T.solid("#ffffff")


for k in range(STAGES):
    tex(f"exit_wall_{k}", wall(k))
    tex(f"exit_base_{k}", base(k))
    tex(f"exit_floor_{k}", floor(k))
tex("exit_ceiling", lambda: fade(l0_ceiling(), 3))
tex("exit_light", lambda: np.clip(l0_light() * 1.05, 0, 1))
tex("exit_meadow", meadow)
tex("exit_meadow_side", meadow_side)
tex("exit_flower_white", flower("#f8f8f4"))
tex("exit_flower_blue", flower("#8ab0f0"))
tex("exit_tall_grass", tall_grass)
tex("exit_door", door_tex(False))
tex("exit_door_top", door_tex(True))
tex("exit_frame", frame_tex)
tex("exit_glare", glare)

for k in range(STAGES):
    cube(f"exit_wallpaper_{k}", f"exit_wall_{k}", display="Fading Wallpaper", sound="wool")
    cube(f"exit_baseboard_{k}", f"exit_base_{k}", display="Fading Baseboard", sound="wool")
    cube(f"exit_carpet_{k}", f"exit_floor_{k}", display="Fading Carpet", sound="wool")
cube("exit_ceiling_tile", "exit_ceiling", display="Pale Ceiling Tile", sound="wool", flat=True)
flicker_light("exit_ceiling_light", "exit_light", "exit_ceiling", "exit_ceiling", light=15, flicker=0.0,
              display="Bright Light")
cube_faces("exit_meadow_grass", top="exit_meadow", bottom="exit_meadow_side", side="exit_meadow_side",
           display="Meadow", sound="grass")
cross("exit_flower_white", "exit_flower_white", display="Wildflowers")
cross("exit_flower_blue", "exit_flower_blue", display="Wildflowers")
cross("exit_tall_grass", "exit_tall_grass", display="Tall Grass")
exit_block("exit_final_door", "exit_door", "SECRET", trigger="USE", solid=True, display="The Door", light=7,
           emissive=True, sound="wood")
exit_block("exit_final_door_top", "exit_door_top", "SECRET", trigger="USE", solid=True, display="The Door", light=7,
           emissive=True, sound="wood")
cube("exit_door_frame", "exit_frame", display="Door Frame", sound="wood")
cube("exit_glare_wall", "exit_glare", display="Daylight", light=15, emissive=True, sound="glass", flat=True)


def ending_loop():
    """The Lobby's hum, thinning out into a slow major chord and morning air."""
    d = 30.0
    t = Y.t_axis(d)
    hum = Y.lp(Y.hum(d, 60.0, "exhum", buzz=0.2), 1500) * 0.12 * (0.5 + 0.5 * np.cos(2 * np.pi * t / 30.0))
    pad = Y.drone(d, [130.81, 164.81, 196.0, 261.63, 329.63], "expad", detune=0.3, lfo=1 / 15.0) * 0.12
    pad = Y.lp(pad, 2200) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 30.0 - 1.2) ** 2)
    air = Y.bp(Y.pink(d, "exair"), 300, 4000) * 0.04
    x = wrapverb(pad + air, 3.0, 0.5, "exroom", damp=5000) + hum
    return Y.stereoize(Y.loopable(Y.norm(x, 0.4), 2.0), 0.003), "stereo"


def far_bird():
    out = np.zeros(int(3.0 * Y.SR))
    for j, f0 in enumerate((2800, 3300, 3000, 3600)):
        nn = int(0.12 * Y.SR)
        tt = np.arange(nn) / Y.SR
        chirp = np.sin(2 * np.pi * np.cumsum(f0 + 700 * np.sin(np.pi * tt / 0.12)) / Y.SR) * np.sin(np.pi * tt / 0.12) ** 2
        Y.place(out, chirp, 0.3 + j * 0.2, 0.3)
    return Y.reverb(out, 2.5, 0.6, "exbird")


def music_note():
    d = 4.0
    t = Y.t_axis(d)
    x = (np.sin(2 * np.pi * 523.25 * t) + 0.3 * np.sin(2 * np.pi * 1046.5 * t)) * np.exp(-t * 1.5)
    return Y.reverb(Y.norm(x, 0.4), 3.5, 0.7, "exnote")


def whisper_name():
    return Y.reverb(Y.whisper("exname", 1.0), 3.0, 0.7, "exname")


sound("ambient.the_exit", [("ambient/the_exit", ending_loop)], stream=True)
sound("ambient.the_exit.additions", [("ambient/the_exit_bird", far_bird), ("ambient/the_exit_note", music_note),
                                    ("ambient/the_exit_whisper", whisper_name)], subtitle="Somewhere, morning")

level("the_exit", fog="#f2f0ea", sky="#dfe8f0", ambient="ambient.the_exit", additions="ambient.the_exit.additions",
      additions_chance=0.003, fixed_time=1500, skylight=True, ceiling=False, height=48)
