"""The Manila Room. One small, quiet, manila-coloured room. Perfectly safe. The light has no source,
the clock has no hands, the door is painted on, and the chair in the corner keeps turning to face you."""
import numpy as np
from assets import cube, tex, shaped, exit_block, level, sound
import tex as T
import synth as Y
from content.classics_kit import centered

S = 32
yy, xx = np.mgrid[0:S, 0:S]
MANILA = ("#d8c08a", "#e2cb96", "#ead6a4")


def paper(seed, lines=True):
    img = T.ramp(T.fbm(seed, octaves=3, base=6) * 0.5 + T.noise(seed + "f", sigma=0.4) * 0.5, *MANILA)
    fibres = (T.rng(seed + "fb").random((S, S)) > 0.97)
    img = T.blend(img, T.solid("#c8ac74"), fibres * 0.4)
    if lines:
        img = T.blend(img, T.solid("#a8b4c0"), ((yy % 8) == 6) * 0.25)
    return T.grain(img, seed, 0.02)


def wall():
    img = paper("mrw")
    # the fold of a folder, running along the top
    img = T.rect(img, 0, 2, 32, 3, "#c4aa74")
    return img


def wall_base():
    img = paper("mrb")
    img = T.rect(img, 0, 27, 32, 32, "#b89a62")
    return T.rect(img, 0, 27, 32, 28, "#cfb27a")


def carpet():
    img = T.ramp(T.noise("mrc", sigma=0.5), "#c8ae78", "#d4ba84")
    return T.grain(img, "mrc", 0.04)


def ceiling():
    img = T.ramp(T.noise("mrce", sigma=3), "#f4e6c0", "#fff2d0")
    return img


def painted_door(top):
    def f():
        img = paper("mrd" + str(top), lines=False)
        # a door, painted carefully onto the wall, with painted shadows
        img = T.rect(img, 3, 0 if top else 0, 29, 32, "#b89a62")
        img = T.rect(img, 5, 2 if top else 0, 27, 32, "#c8aa70")
        img = T.rect(img, 25, 2 if top else 0, 27, 32, "#9a7e4c")
        if top:
            img = T.rect(img, 3, 0, 29, 2, "#9a7e4c")
            img = T.rect(img, 8, 6, 24, 18, "#bea064")
        else:
            img = T.rect(img, 8, 4, 24, 24, "#bea064")
            img = T.rect(img, 21, 2, 24, 5, "#7a6038")
        return img
    return f


def clock():
    img = paper("mrcl")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#f6efe0"), (d < 11).astype(float))
    img = T.blend(img, T.solid("#6a5634"), (np.abs(d - 11) < 1).astype(float))
    for k in range(12):
        a = k * np.pi / 6
        img[int(round(15.5 + np.sin(a) * 9)), int(round(15.5 + np.cos(a) * 9))] = T.hexc("#3a2a18")
    return img   # no hands


def picture():
    """A photograph of this room. Someone is standing in it."""
    img = T.solid("#e2cb96")
    img = T.rect(img, 4, 20, 28, 28, "#c8ae78")
    img = T.rect(img, 4, 4, 28, 20, "#dcc490")
    img = T.rect(img, 7, 14, 12, 20, "#8a6a3a")
    img = T.rect(img, 21, 8, 25, 18, "#b89a62")
    img = T.rect(img, 16, 12, 17, 20, "#2a2018")
    img = T.rect(img, 15, 10, 18, 12, "#2a2018")
    img = T.border(img, "#5a4428", 2)
    return T.grain(img, "mrpic", 0.03)


def folder():
    img = paper("mrf", lines=False)
    img = T.rect(img, 0, 0, 12, 4, "#c4aa74")
    img = centered(img, "YOU", 14, "#5a4428")
    return img


TEX = {"mr_wall": wall, "mr_wall_base": wall_base, "mr_carpet": carpet, "mr_ceiling": ceiling,
       "mr_door_top": painted_door(True), "mr_door_bottom": painted_door(False), "mr_clock": clock,
       "mr_picture": picture, "mr_folder": folder}
for k, fn in TEX.items():
    tex(k, fn)

cube("manila_wall", "mr_wall", display="Manila Wall", sound="wool")
cube("manila_wall_base", "mr_wall_base", display="Manila Wall", sound="wool")
cube("manila_carpet", "mr_carpet", display="Manila Carpet", sound="wool")
cube("manila_ceiling", "mr_ceiling", display="Manila Ceiling", flat=True, light=15, emissive=True, sound="wool")
cube("manila_clock", "mr_clock", display="Clock With No Hands", sound="wool")
exit_block("manila_painted_door", "mr_door_bottom", "DOOR", trigger="USE", solid=True, display="Painted Door",
           sound="wool")
exit_block("manila_painted_door_top", "mr_door_top", "DOOR", trigger="USE", solid=True, display="Painted Door",
           sound="wool")
shaped("manila_picture", [(2, 2, 0, 14, 14, 1, {"all": "frame", "south": "pic"})],
       {"frame": "mr_wall_base", "pic": "mr_picture"}, display="Photograph", collides=False, sound="wool",
       unbreakable=True)
shaped("manila_folder", [(3, 0, 4, 13, 0.6, 12, "f")], {"f": "mr_folder"}, display="A Folder With Your Name On It",
       kind="pickup", item="noclip:journal*1,noclip:almond_water*3,noclip:battery*2", collides=False, sound="wool",
       strength=0.2)

# ------------------------------------------------------------------ sound


def room_tone():
    d = 20.0
    t = Y.t_axis(d)
    tone = Y.lp(Y.pink(d, "mrtone"), 250) * 0.25
    hum = np.sin(2 * np.pi * 50 * t) * 0.01
    tick = np.zeros_like(t)
    for k in range(int(d)):
        n = int(0.02 * Y.SR)
        tt = np.arange(n) / Y.SR
        click = Y.bp(Y.white(0.02, f"mrt{k % 2}"), 2000, 8000)[:n] * np.exp(-tt * 250)
        Y.place(tick, click, k + 0.05, 0.35 if k % 2 == 0 else 0.25)
    x = Y.loopable(tone + hum + Y.reverb(tick, 0.4, 0.2, "mrtk")[: len(t)], 0.5)
    return Y.stereoize(Y.norm(x, 0.25), 0.0005), "stereo"


def rustle():
    d = 1.6
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mrru"), 1500, 7000) * (np.sin(2 * np.pi * 9 * t) > 0.2) * np.sin(np.pi * t / d)
    return Y.norm(x, 0.25)


def wall_knock():
    out = np.zeros(int(1.5 * Y.SR))
    Y.place(out, Y.impact("mrkn", 0.2), 0.05)
    return Y.norm(Y.lp(out, 600), 0.3)


def exhale():
    d = 2.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mrex"), 300, 1800) * np.sin(np.pi * t / d) ** 2
    return Y.norm(x, 0.2)


def scrape():
    d = 0.9
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mrsc"), 200, 1600) * (1 + np.sign(np.sin(2 * np.pi * 37 * t))) * np.sin(np.pi * t / d)
    return Y.norm(Y.reverb(x, 0.6, 0.25, "mrsc"), 0.5)


sound("event.manila_room_scrape", [("event/manila_room_scrape", scrape)], subtitle="A chair scrapes")
sound("ambient.manila_room", [("ambient/manila_room", room_tone)], stream=True)
sound("ambient.manila_room.additions",
      [("ambient/manila_room_rustle", rustle), ("ambient/manila_room_knock", wall_knock),
       ("ambient/manila_room_exhale", exhale)],
      subtitle="The room is quiet")

level("manila_room", fog="#e2cb96", sky="#e2cb96", ambient="ambient.manila_room",
      additions="ambient.manila_room.additions", additions_chance=0.002, fixed_time=6000, height=32)
