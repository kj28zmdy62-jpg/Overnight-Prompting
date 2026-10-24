"""Movie Theater - a multiplex corridor of numbered doors, auditoriums of red seats on steep risers.
The film on the screen is a shot of the seats, from the front. You are in it, a few seconds late.
Sit through it and the exit by the screen opens."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, stairs, custom, flicker_light, level, sound
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32


def carpet_corridor():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtcc", sigma=0.5), "#2a0e3a", "#341446")
    diamond = (np.abs((xx % 16) - 8) + np.abs((yy % 16) - 8)) < 5
    ring = (np.abs((np.abs((xx % 16) - 8) + np.abs((yy % 16) - 8)) - 5) < 0.8)
    img = T.blend(img, T.solid("#b8902a"), ring * 0.8)
    img = T.blend(img, T.solid("#5a1a6a"), diamond * 0.5)
    stars = (np.abs((xx % 16) - 0) < 1) & (np.abs((yy % 16) - 0) < 1)
    img = T.blend(img, T.solid("#e8c040"), stars.astype(float))
    return T.grain(T.stains(img, "mtccs", "#1a0a1a", count=2, strength=0.35, size=6), "mtcc", 0.03)


def carpet_aud():
    img = T.ramp(T.noise("mtca", sigma=0.6), "#3a0a12", "#4a0e18")
    img = T.stains(img, "mtcas", "#1a0408", count=3, strength=0.45, size=5)
    return T.grain(img, "mtca", 0.03)


def sticky():
    img = carpet_aud()
    img = T.blend(img, T.solid("#6a3a0a"), np.clip(T.noise("mtst", sigma=2) - 0.35, 0, 1) * 1.2)
    img = T.shade(img, 1 + np.clip(T.noise("mtsts", sigma=1.5) - 0.6, 0, 1) * 1.0)  # it shines
    return img


def wall_fabric():
    yy, xx = np.mgrid[0:S, 0:S]
    folds = np.sin(xx / 32 * 2 * np.pi * 4) * 0.5 + 0.5
    img = T.ramp(folds * 0.5 + T.noise("mtwf", sigma=1) * 0.3, "#1a0a1e", "#2a1230")
    return T.grain(img, "mtwf", 0.03)


def wall_corridor():
    img = T.ramp(T.fbm("mtwc", octaves=3, base=4), "#3a1a2a", "#462234")
    yy, xx = np.mgrid[0:S, 0:S]
    img[22:24] = T.hexc("#b8902a")
    img[24:] = T.ramp(T.noise("mtwcl", sigma=1, shape=(8, S)), "#1a0a12", "#220e18")
    return T.grain(img, "mtwc", 0.03)


def screen_base(seed="mtsc", you=0):
    """The projected image: rows of red seats seen from the front, in blue projector light."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=3) * 0.3 + 0.5, "#5a6a8a", "#7a8aa8")
    for row in range(2):
        y0 = 4 + row * 16
        backs = (yy >= y0) & (yy < y0 + 9) & (((xx + row * 4) % 8) >= 1) & (((xx + row * 4) % 8) <= 6)
        img = T.blend(img, T.solid("#7a2a3a"), backs * 0.9)
        img = T.blend(img, T.solid("#3a1a22"), ((yy >= y0 + 9) & (yy < y0 + 12)) * 0.8)
        tops = (yy == y0) & backs
        img = T.blend(img, T.solid("#a84a5a"), tops * 0.8)
    if you:
        # someone sitting in the middle seat. you.
        img = T.blend(img, T.solid("#0a0a10"), K.disc(S, S, 16, 9, 3.6).astype(float))
        img = T.blend(img, T.solid("#0a0a10"), ((yy >= 12) & (yy < 22) & (np.abs(xx - 16) < 6)).astype(float))
        if you == 2:
            # it has turned round to look at the back of the room, at you
            img = T.blend(img, T.solid("#d8d0c0"), (K.disc(S, S, 14.5, 9, 0.8) | K.disc(S, S, 17.5, 9, 0.8)).astype(float))
    img = T.shade(img, 0.9 + 0.1 * np.sin(yy * 2.1))  # projector flicker banding
    grainy = T.rng(seed + "g").random((S, S)) - 0.5
    return np.clip(img + grainy[..., None] * 0.08, 0, 1)


def screen_text(text):
    def f():
        img = T.solid("#06060a")
        img = T.blend(img, T.solid("#e8e4d8"), K.centered(text, S, S, 9, font=K.F57, scale=2))
        return img
    return f


def seat_velvet():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("mtsv", sigma=0.6), "#8a1a2a", "#a02234")
    img = T.shade(img, 1 - ((xx % 8) == 0) * 0.2)
    return T.grain(img, "mtsv", 0.03)


def seat_frame():
    return T.grain(T.ramp(T.noise("mtsf", sigma=1), "#1a1a1e", "#26262c"), "mtsf", 0.03)


def door_tex(top):
    img = T.ramp(T.noise("mtdr" + str(top), sigma=2), "#5a1a24", "#6a2230")
    yy, xx = np.mgrid[0:S, 0:S]
    tufts = ((xx % 8 == 4) & (yy % 8 == 4))
    img = T.blend(img, T.solid("#b8902a"), tufts.astype(float))
    img = T.border(img, "#b8902a", 1, sides="we" + ("n" if top else "s"))
    if not top:
        img = T.rect(img, 4, 2, 28, 4, "#c8a040")
    return T.grain(img, "mtdr" + str(top), 0.03)


def number_sign(n):
    def f():
        img = T.solid("#120a10")
        img = T.blend(img, T.solid("#ffe080"), K.centered(str(n), S, S, 9, font=K.F57, scale=2))
        return T.border(img, "#b8902a", 2)
    return f


def poster(title, top_col, bottom_col, seed):
    def f():
        img = T.vgrad(S, S, top_col, bottom_col)
        img = T.border(img, "#b8902a", 2)
        img = T.blend(img, T.solid("#f0e8d0"), K.centered(title, S, S, 5, font=K.F35))
        yy, xx = np.mgrid[0:S, 0:S]
        # a silhouette, tiny, in an empty auditorium
        for r in range(3):
            img = T.blend(img, T.solid("#2a0a10"), ((yy == 20 + r * 3) & (xx % 3 != 0) & (xx > 4) & (xx < 28)) * 0.8)
        img = T.blend(img, T.solid("#0a0a0a"), K.disc(S, S, 16, 22, 1.2).astype(float))
        img = T.blend(img, T.solid("#f0e8d0"), K.centered("SOON", S, S, 13, font=K.F35) * 0.6)
        return T.grain(img, seed, 0.03)
    return f


def popcorn_tex():
    img = T.solid("#f0e0a0")
    r = T.rng("mtpc")
    for k in range(80):
        x, y = r.random() * S, r.random() * S
        img = T.blend(img, T.solid(["#fff4c8", "#e8c060", "#f8e8a8"][k % 3]), K.disc(S, S, x, y, 1.6).astype(float))
    return img


def popcorn_spill():
    rgb = popcorn_tex()
    a = np.zeros((S, S))
    r = T.rng("mtps")
    for k in range(14):
        x, y = r.random() * S, r.random() * S
        a = np.maximum(a, K.disc(S, S, x, y, 1.2).astype(float))
    return rgb, a


def machine_glass():
    yy, xx = np.mgrid[0:S, 0:S]
    img = popcorn_tex()
    img = T.blend(img, T.solid("#fff8e0"), np.clip(1 - np.abs((xx + yy) % 20 - 5) / 3, 0, 1) * 0.4)
    img[:4] = T.hexc("#c82020")
    img = T.blend(img, T.solid("#f8f0d0"), K.centered("POPCORN", S, S, 0, font=K.F35, spacing=0) * 0.0)
    return T.border(img, "#c8c8c8", 1)


def red_metal():
    return T.grain(T.ramp(T.noise("mtrm", sigma=1.5), "#a81a1a", "#c02424"), "mtrm", 0.02)


def booth_window():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(np.clip(1 - d / 14, 0, 1), "#1a1a28", "#e8f0ff")
    return T.border(img, "#1a1a1a", 3)


def exit_locked(top):
    img = T.ramp(T.noise("mtel" + str(top), sigma=2), "#2a2a30", "#34343a")
    img = T.border(img, "#5a5a60", 2, sides="we" + ("n" if top else "s"))
    if not top:
        img = T.rect(img, 3, 0, 29, 4, "#8a8a90")
        img = T.rect(img, 13, 6, 19, 10, "#c8a030")  # padlock
    return T.grain(img, "mtel" + str(top), 0.03)


tex("mt_carpet", carpet_corridor)
tex("mt_carpet_aud", carpet_aud)
tex("mt_sticky", sticky)
tex("mt_wall_fabric", wall_fabric)
tex("mt_wall_corridor", wall_corridor)
tex("mt_screen", lambda: screen_base())
tex("mt_screen_b", lambda: screen_base("mtscb"))
tex("mt_screen_you", lambda: screen_base("mtscy", 1))
tex("mt_screen_you_turned", lambda: screen_base("mtscy2", 2))
tex("mt_screen_the", screen_text("THE"))
tex("mt_screen_end", screen_text("END"))
tex("mt_screen_dark", screen_text(""))
tex("mt_seat_velvet", seat_velvet)
tex("mt_seat_frame", seat_frame)
tex("mt_door_top", lambda: door_tex(True))
tex("mt_door_bottom", lambda: door_tex(False))
for n in range(1, 13):
    tex(f"mt_number_{n}", number_sign(n))
POSTERS = [("THE SEATS", "#1a2a5a", "#0a0a1a"), ("ROW 9", "#5a1a1a", "#1a0a0a"), ("STAY", "#1a4a2a", "#0a1a0a"),
           ("CREDITS", "#4a3a1a", "#1a120a")]
for n, (t, a, b) in enumerate(POSTERS):
    tex(f"mt_poster_{n}", poster(t, a, b, f"mtp{n}"))
tex("mt_popcorn", popcorn_tex)
tex("mt_popcorn_spill", popcorn_spill)
tex("mt_machine_glass", machine_glass)
tex("mt_red_metal", red_metal)
tex("mt_booth_window", booth_window)
tex("mt_exit_locked_top", lambda: exit_locked(True))
tex("mt_exit_locked_bottom", lambda: exit_locked(False))

cube("mt_carpet", "mt_carpet", display="Theater Carpet", sound="wool")
cube("mt_carpet_aud", "mt_carpet_aud", display="Auditorium Carpet", sound="wool")
custom("mt_sticky", "com.noclip.block.StickyFloorBlock", {"parent": "minecraft:block/cube_all",
                                                          "textures": {"all": "noclip:block/mt_sticky"}},
       display="Sticky Floor", sound="mud", unbreakable=True)
stairs("mt_steps", "mt_carpet_aud", "mt_carpet_aud", display="Aisle Steps", sound="wool")
cube("mt_wall_fabric", "mt_wall_fabric", display="Acoustic Fabric", sound="wool")
cube("mt_wall_corridor", "mt_wall_corridor", display="Corridor Wall", sound="stone")
for name in ("mt_screen", "mt_screen_b", "mt_screen_you", "mt_screen_you_turned", "mt_screen_the", "mt_screen_end"):
    cube(name, name, display="Screen", sound="wool", light=6, emissive=True)
cube("mt_screen_dark", "mt_screen_dark", display="Screen", sound="wool")
shaped("mt_seat", [
    (1, 0, 3, 3, 9, 13, "frame"), (13, 0, 3, 15, 9, 13, "frame"),
    (3, 4, 3, 13, 7, 12, "velvet"), (3, 7, 11, 13, 18, 14, "velvet"),
    (13, 9, 5, 15, 10, 12, "frame"),
], {"velvet": "mt_seat_velvet", "frame": "mt_seat_frame"}, display="Theater Seat", unbreakable=True)
cube("mt_door_top", "mt_door_top", display="Auditorium Door", sound="wool")
from assets import exit_block
cube("mt_door_bottom", "mt_door_bottom", display="Auditorium Door", sound="wool")
for n in range(1, 13):
    cube(f"mt_number_{n}", f"mt_number_{n}", display=f"Auditorium {n}", sound="glass", light=5, emissive=True)
for n in range(len(POSTERS)):
    cube(f"mt_poster_{n}", f"mt_poster_{n}", display="Coming Soon", sound="wood", light=4, emissive=True)
shaped("mt_popcorn_spill", [(0, 0, 0, 16, 0.5, 16, "p")], {"p": "mt_popcorn_spill"}, display="Spilled Popcorn",
       collides=False, unbreakable=True)
shaped("mt_popcorn_machine", [(1, 0, 1, 15, 8, 15, "red"), (2, 8, 2, 14, 16, 14, {"all": "glass", "up": "red"})],
       {"red": "mt_red_metal", "glass": "mt_machine_glass"}, display="Popcorn Machine", light=8, emissive=True,
       unbreakable=True)
shaped("mt_popcorn_box", [(4, 0, 4, 12, 9, 12, {"all": "red", "up": "corn"}), (5, 9, 5, 11, 11, 11, "corn")],
       {"red": "mt_red_metal", "corn": "mt_popcorn"}, display="Popcorn", kind="pickup",
       item="noclip:energy_bar*2,noclip:almond_water*1", collides=False, sound="wool")
cube("mt_booth_window", "mt_booth_window", display="Projection Booth", sound="glass", light=9, emissive=True)
cube("mt_exit_locked", "mt_exit_locked_bottom", display="Exit (Locked)", sound="metal")
cube("mt_exit_locked_top", "mt_exit_locked_top", display="Exit (Locked)", sound="metal")
cube_faces("mt_counter", top="mt_red_metal", bottom="mt_red_metal", side="mt_wall_corridor", display="Concession Counter")
flicker_light("mt_sconce", "mt_number_1", "mt_wall_fabric", "mt_wall_fabric", light=6, flicker=0.0, face="all",
              display="Sconce")

# ------------------------------------------------------------------ sound


def film_through_walls():
    """Somebody else's film, through two walls: bass swells, a muffled voice, an orchestra hit."""
    d = 30.0
    t = Y.t_axis(d)
    swell = Y.lp(Y.brown(d, "mtsw"), 90) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 15)) * 0.7
    voice = np.zeros_like(t)
    for k, at in enumerate((3.0, 9.5, 17.0, 23.5)):
        K.circular_place(voice, K.pa_voice(f"mtfv{k}", syllables=12 + k, f0=105 + k * 20, rate=0.17, band=(150, 900)), at, 0.4)
    hits = np.zeros_like(t)
    for at in (13.0, 27.0):
        n = int(2.0 * Y.SR)
        tt = np.arange(n) / Y.SR
        hit = sum(np.sin(2 * np.pi * f * tt) for f in (55, 82.4, 110, 164.8)) * np.exp(-tt * 1.5)
        K.circular_place(hits, hit, at, 0.3)
    proj = Y.bp(Y.white(d, "mtproj"), 800, 3000) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 24 * t))) * 0.03
    x = Y.lp(swell + voice + hits, 700) + proj
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.0025), "stereo"


def shush():
    d = 1.4
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mtsh"), 2500, 7000) * np.sin(np.pi * t / d) ** 0.7
    return Y.norm(Y.reverb(x, 1.5, 0.4, "mtsh"), 0.5)


def slurp():
    d = 1.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mtsl"), 400, 3000) * (np.sin(2 * np.pi * 9 * t) > 0) * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(x, 1.5, 0.5, "mtsl"), 0.4)


def wrapper():
    d = 1.8
    t = Y.t_axis(d)
    crk = (Y.rng("mtwr").random(len(t)) > 0.995).astype(float)
    x = Y.bp(crk, 2000, 9000) * 2 * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(x, 1.5, 0.5, "mtwr"), 0.4)


def seat_creak():
    d = 1.0
    t = Y.t_axis(d)
    f = 180 + 80 * np.sin(np.pi * t / d)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)) * (np.sin(2 * np.pi * 30 * t) > 0) * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(Y.bp(x, 300, 2500), 1.5, 0.4, "mtsc"), 0.4)


def laughter():
    d = 2.5
    out = np.zeros(int(d * Y.SR))
    for k in range(6):
        v = K.pa_voice(f"mtl{k}", syllables=5, f0=150 + k * 30, rate=0.1, band=(200, 2500))
        Y.place(out, v, 0.1 + k * 0.05, 0.4)
    t = Y.t_axis(d)
    out *= (t < 1.3)  # and then it stops. all at once.
    return Y.norm(Y.reverb(Y.lp(out, 1500), 3.0, 0.7, "mtlaugh"), 0.4)


def film_end():
    d = 5.0
    t = Y.t_axis(d)
    chord = sum(np.sin(2 * np.pi * f * t) * (1 + 0.003 * np.sin(2 * np.pi * 5 * t)) for f in (130.8, 164.8, 196.0, 261.6))
    chord *= np.minimum(t * 2, 1) * np.exp(-t * 0.5) * 0.4
    motor = Y.bp(Y.white(d, "mtcm"), 100, 600) * 0.2 * (t > 1.5) * (t < 4.5)
    out = chord + motor
    Y.place(out, Y.impact("mtunl", 0.3, ring=0.4), 4.6, 0.6)
    return Y.norm(Y.reverb(out, 3.0, 0.6, "mtend"), 0.8)


sound("ambient.movie_theater", [("ambient/movie_theater", film_through_walls)], stream=True)
sound("ambient.movie_theater.additions",
      [("ambient/movie_theater_shush", shush), ("ambient/movie_theater_slurp", slurp),
       ("ambient/movie_theater_wrapper", wrapper), ("ambient/movie_theater_seat", seat_creak),
       ("ambient/movie_theater_laugh", laughter)],
      subtitle="Someone else in the audience")
sound("event.movie_theater_end", [("event/movie_theater_end", film_end)], subtitle="The film ends")
sound("event.movie_theater_shush", [("event/movie_theater_shush", shush)], subtitle="Shhh")

level("movie_theater", fog="#0a060c", sky="#0a060c", ambient="ambient.movie_theater",
      additions="ambient.movie_theater.additions", additions_chance=0.006, fixed_time=18000, height=48)
