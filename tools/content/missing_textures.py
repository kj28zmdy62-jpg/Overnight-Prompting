"""Missing Textures - the world before anyone finished it: magenta and black everywhere, fragments hanging in the
air, ERROR plates standing in the field. Some of the floor was never loaded. Keep moving, or it forgets you too.
Somewhere stands one thing that was finished: a real wooden door."""
import numpy as np
from assets import cube, tex, shaped, exit_block, level, sound, lang, entity_model, entity_tex, ITEM_MODELS
import tex as T
import synth as Y
from content.rb_shared import text_center, wood
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32
MAG, BLK = "#f800f8", "#000000"


def checker(phase=0, dirty=False):
    yy, xx = np.mgrid[0:S, 0:S]
    a = ((((xx + phase) // 16) + ((yy + phase) // 16)) % 2) == 0
    img = np.where(a[..., None], T.hexc(MAG), T.hexc(BLK))
    if dirty:
        # a texture that started to load and gave up partway
        rows = T.rng("mtdirty").random(S) > 0.7
        img[rows] = np.roll(img[rows], 5, axis=1)
    return img.astype(float)


def error_plate():
    img = T.solid("#000000")
    img = text_center(img, "ERROR", 6, "#ff3030", scale=1)
    img = text_center(img, "MISSING", 14, "#f800f8")
    img = text_center(img, "NULL", 22, "#ffffff")
    return img


def question():
    img = checker()
    img[6:26, 6:26] = T.hexc("#000000")
    img = text_center(img, "?", 9, "#f800f8", scale=3)
    return img


def wireframe():
    """A block whose faces never arrived: only the edges were drawn."""
    rgb = np.ones((S, S, 3)) * T.hexc("#f8f8f8")
    a = np.zeros((S, S))
    a[:1] = a[-1:] = 1
    a[:, :1] = a[:, -1:] = 1
    yy, xx = np.mgrid[0:S, 0:S]
    a[(xx == yy)] = 0.6
    return rgb, a


def real_door(top):
    """Finished. Somebody actually painted this one. Oak, brass, a little light around the edges."""
    img = wood("mtdoor" + str(top), "#6a4424", "#8a5a30", grain_dir="v", rings=4)
    for (y0, y1) in (((5, 29),) if top else ((2, 24),)):
        for (x0, x1) in ((5, 14), (18, 27)):
            img[y0:y1, x0] = img[y0:y1, x0] * 0.7
            img[y0, x0:x1] = img[y0, x0:x1] * 0.7
            img[y0:y1, x1] = np.clip(img[y0:y1, x1] * 1.25, 0, 1)
            img[y1, x0:x1 + 1] = np.clip(img[y1, x0:x1 + 1] * 1.25, 0, 1)
    img[:, :2] = T.hexc("#fff0c0")
    img[:, -2:] = T.hexc("#fff0c0")
    if top:
        img[:2] = T.hexc("#fff0c0")
    else:
        img = T.rect(img, 23, 2, 26, 6, "#c8a050")
        img = T.rect(img, 24, 3, 25, 5, "#fff0b0")
    return T.grain(img, "mtdoor" + str(top), 0.02)


def frame():
    return wood("mtframe", "#e8e2d4", "#f4f0e6", grain_dir="v", rings=2)


tex("mt_missing", lambda: checker(0))
tex("mt_missing_offset", lambda: checker(8))
tex("mt_missing_dirty", lambda: checker(0, True))
tex("mt_error", error_plate)
tex("mt_question", question)
tex("mt_wire", wireframe)
tex("mt_door_low", lambda: real_door(False))
tex("mt_door_high", lambda: real_door(True))
tex("mt_frame", frame)

cube("mt_missing", "mt_missing", display="Missing Texture", sound="stone", flat=True)
cube("mt_missing_dirty", "mt_missing_dirty", display="Missing Texture", sound="stone", flat=True)
# never loaded: looks almost the same, but the squares do not line up with their neighbours, and nothing holds you
cube("mt_unloaded", "mt_missing_offset", display="Unloaded Chunk", sound="stone", flat=True, no_collision=True, render="cutout")
cube("mt_question", "mt_question", display="?", sound="stone", flat=True, light=6, emissive=True)
cube("mt_wire", "mt_wire", display="Missing Model", sound="glass", render="cutout", flat=True)
shaped("mt_error_sign", [(7, 0, 7, 9, 10, 9, "post"), (0, 8, 7, 16, 16, 9, {"all": "post", "south": "plate", "north": "plate"})],
       {"plate": "mt_error", "post": "mt_missing"}, display="ERROR", light=5, emissive=True, unbreakable=True,
       collides=True, sound="metal")
exit_block("mt_door_low", "mt_door_low", "DOOR", trigger="USE", solid=True, display="A Finished Door", sound="wood",
           light=9, emissive=True)
exit_block("mt_door_high", "mt_door_high", "DOOR", trigger="USE", solid=True, display="A Finished Door", sound="wood",
           light=9, emissive=True)
cube("mt_frame", "mt_frame", display="Painted Door Frame", sound="wood")

# ------------------------------------------------------------------ the Null


def null_tex():
    c = Canvas()
    chk = lambda w, h: np.where(((np.mgrid[0:h, 0:w][0] // 2 + np.mgrid[0:h, 0:w][1] // 2) % 2 == 0)[..., None],
                                T.hexc(MAG), T.hexc(BLK)).astype(float)
    paint_humanoid(c, chk, chk, chk, chk)
    return c.out()


entity_model("mt_null", humanoid(arm_len=15, leg_len=14))
entity_tex("mt_null", null_tex)
lang("entity.noclip.mt_null", "null")
lang("item.noclip.mt_null_spawn_egg", "null Spawn Egg")
ITEM_MODELS["mt_null_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def digital():
    """A sound engine with nothing to play: a DC hum, a buffer that keeps catching on the same few milliseconds."""
    d = 20.0
    t = Y.t_axis(d)
    hum = np.sign(np.sin(2 * np.pi * 50 * t)) * 0.08 + np.sin(2 * np.pi * 100 * t) * 0.1
    out = np.zeros_like(t)
    r = Y.rng("mtbuf")
    for k in range(14):
        n = int((0.2 + r.random() * 0.6) * Y.SR)
        g = Y.bp(Y.white(0.012, f"mtg{k}"), 300, 4000)
        seg = np.tile(g, n // len(g) + 1)[:n] * 0.25
        Y.place(out, seg, r.random() * (d - 1), 0.5 + r.random() * 0.5)
    x = Y.lp(hum, 800) + out
    x = np.round(x * 24) / 24
    return Y.stereoize(Y.loopable(Y.norm(x, 0.4), 0.5), 0.001), "stereo"


def error_bonk():
    d = 0.7
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * 220 * t)) * (t < 0.25) + np.sign(np.sin(2 * np.pi * 165 * t)) * (t > 0.3) * (t < 0.6)
    return Y.norm(Y.lp(x, 3000) * 0.5, 0.5)


def crunch():
    d = 0.9
    x = Y.white(d, "mtcr")
    x = np.round(x * 3) / 3
    x = np.repeat(x[::12], 12)[: int(d * Y.SR)]
    t = Y.t_axis(d)
    return Y.norm(x * np.exp(-t * 4), 0.5)


def unload():
    d = 1.6
    t = Y.t_axis(d)
    f = 900 * np.exp(-t * 2) + 40
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR))
    x = np.repeat(x[::20], 20)[: len(t)] * np.exp(-t * 1.2)
    return Y.norm(x * 0.6, 0.7)


def blink():
    d = 0.4
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "mtbl"), 2000, 9000) * np.exp(-t * 18)
    x += np.sign(np.sin(2 * np.pi * 70 * t)) * np.exp(-t * 12) * 0.5
    return Y.norm(x, 0.7)


sound("ambient.missing_textures", [("ambient/missing_textures", digital)], stream=True)
sound("ambient.missing_textures.additions",
      [("ambient/missing_textures_bonk", error_bonk), ("ambient/missing_textures_crunch", crunch)],
      subtitle="Something fails to load")
sound("event.missing_textures_unload", [("event/missing_textures_unload", unload)], subtitle="The ground unloads")
sound("entity.mt_null.blink", [("entity/mt_null_blink", blink)], subtitle="null skips closer")

level("missing_textures", fog="#000000", sky="#000000", ambient="ambient.missing_textures",
      additions="ambient.missing_textures.additions", additions_chance=0.006, fixed_time=6000, height=64,
      ambient_light=0.5)
