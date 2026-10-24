"""Inverted Lobby - Level 0 upside down. Walk on the ceiling tiles; the carpet hangs overhead like a sky."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, flicker_light, entity_model, entity_tex, \
    lang, ITEM_MODELS
import tex as T
import synth as Y
from content.deepend_audio import wrapverb, fit
from content.level_0 import wallpaper_base_pattern, wallpaper_base, carpet as l0_carpet, ceiling as l0_ceiling, \
    light_on, light_off, chair_fabric, chair_metal
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32


def flip(fn):
    return lambda: np.ascontiguousarray(fn()[::-1])


def tile_floor():
    """Ceiling tiles, walked on: scuffed grey footprints across the pits."""
    img = l0_ceiling()
    scuff = T.noise("invscuff", sigma=2) > 0.6
    return T.blend(img, T.solid("#9a9480"), scuff.astype(float) * 0.35)


def tile_floor_cracked():
    img = tile_floor()
    return T.cracks(img, "invcrack", "#6a6450", count=2, alpha=0.7)


def hole():
    img = T.solid("#000000")
    return T.border(img, "#a8a28c", 1)


tex("inv_wall", flip(lambda: wallpaper_base_pattern("invw")))
tex("inv_wall_base", flip(wallpaper_base))
tex("inv_floor", tile_floor)
tex("inv_floor_cracked", tile_floor_cracked)
tex("inv_sky_carpet", l0_carpet)
tex("inv_light", light_on)
tex("inv_light_off", light_off)
tex("inv_hole", hole)

cube("inverted_wallpaper", "inv_wall", display="Upside-Down Wallpaper", sound="wool")
cube("inverted_baseboard", "inv_wall_base", display="Upside-Down Baseboard", sound="wool")
cube("inverted_floor_tile", "inv_floor", display="Ceiling Tile (Floor)", sound="wool")
cube("inverted_floor_tile_cracked", "inv_floor_cracked", display="Cracked Ceiling Tile (Floor)", sound="wool")
cube("inverted_carpet_sky", "inv_sky_carpet", display="Carpet (Overhead)", sound="wool", flat=True)
flicker_light("inverted_floor_light", "inv_light", "inv_light_off", "inv_floor", light=15, flicker=0.0, face="up",
              display="Floor Light")
flicker_light("inverted_floor_light_flicker", "inv_light", "inv_light_off", "inv_floor", light=15, flicker=0.6,
              face="up", display="Flickering Floor Light")
exit_block("inverted_missing_tile", "inv_hole", "FALL", display="Missing Tile")

# an office chair stuck to the carpet above, upside down
chair_boxes = [(3, 6, 3, 13, 8, 13, "fabric"), (3, 8, 12, 13, 18, 14, "fabric"), (7, 1, 7, 9, 6, 9, "metal"),
               (2, 0, 7, 14, 1, 9, "metal"), (7, 0, 2, 9, 1, 14, "metal")]
flipped = []
for (x1, y1, z1, x2, y2, z2, k) in chair_boxes:
    flipped.append((x1, max(0, 16 - y2), z1, x2, 16 - y1, z2, k))
tex("inv_chair_fabric", chair_fabric)
tex("inv_chair_metal", chair_metal)
shaped("inverted_office_chair", flipped, {"fabric": "inv_chair_fabric", "metal": "inv_chair_metal"},
       display="Upside-Down Office Chair", sound="wool", collides=False, unbreakable=True)

# ------------------------------------------------------------------ the people walking on the carpet

entity_model("inverted_walker", humanoid(arm_len=14, leg_len=14))


def walker_tex():
    c = Canvas()
    skin = lambda w, h: T.ramp(T.noise(f"invws{w}{h}", shape=(h, w), sigma=1.2), "#b8a890", "#c8b8a0")
    shirt = lambda w, h: T.grain(T.solid("#d8c890", shape=(h, w)), f"invsh{w}{h}", 0.05)
    trousers = lambda w, h: T.grain(T.solid("#4a4438", shape=(h, w)), f"invtr{w}{h}", 0.05)

    def face(w, h):
        img = skin(w, h)
        img[0:2] = T.hexc("#2a2018")
        return img
    paint_humanoid(c, skin, shirt, lambda w, h: shirt(w, h), trousers, face=face)
    c.box(0, 0, 8, 8, 8, up=lambda w, h: T.solid("#2a2018", shape=(h, w)))
    return c.out()


entity_tex("inverted_walker", walker_tex)
lang("entity.noclip.inverted_walker", "Someone Upstairs")
lang("item.noclip.inverted_walker_spawn_egg", "Someone Upstairs Spawn Egg")
ITEM_MODELS["inverted_walker_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}


def inverted_hum():
    """The Lobby's hum played backwards: swelling into each cycle instead of fading out."""
    d = 24.0
    t = Y.t_axis(d)
    x = Y.hum(d, 60.0, "invhum", buzz=0.35) * 0.5
    x = Y.lp(x, 2500)
    swell = np.zeros_like(t)
    for k in range(6):
        seg = Y.t_axis(4.0)
        s = Y.bp(Y.pink(4.0, f"invsw{k}"), 200, 2000) * (seg / 4.0) ** 3
        Y.place(swell, s, k * 4.0, 0.25)
    x = x + wrapverb(swell, 2.0, 0.5, "invroom")
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 1.0), 0.0012), "stereo"


def steps_above(i):
    def f():
        out = np.zeros(int(4.0 * Y.SR))
        for k in range(6):
            Y.place(out, Y.footstep(f"invst{i}{k}", "carpet"), 0.2 + k * 0.55, 0.6)
        out = Y.lp(out, 500)
        return Y.reverb(out, 1.5, 0.5, f"invst{i}")
    return f


def reversed_thump():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.impact("invthump", 1.2), 0.1)
    out = out[::-1].copy()
    return Y.reverb(Y.lp(out, 600), 2.0, 0.5, "invthump")


def drop():
    d = 1.5
    t = Y.t_axis(d)
    whoosh = Y.bp(Y.white(d, "invdrop"), 300, 3000) * np.clip(t / 0.5, 0, 1) * (t < 0.6)
    out = whoosh * 0.6
    Y.place(out, Y.impact("invdropi", 0.9), 0.6, 1.0)
    return Y.norm(out, 0.95)


sound("ambient.inverted_lobby", [("ambient/inverted_lobby", inverted_hum)], stream=True)
sound("ambient.inverted_lobby.additions", [(f"ambient/inverted_lobby_steps{i}", steps_above(i)) for i in range(2)] +
      [("ambient/inverted_lobby_thump", reversed_thump)], subtitle="Footsteps overhead")
sound("entity.inverted_walker.drop", [("entity/inverted_walker_drop", drop)], subtitle="Something drops from above")

level("inverted_lobby", fog="#3a3418", sky="#3a3418", ambient="ambient.inverted_lobby",
      additions="ambient.inverted_lobby.additions", additions_chance=0.005, fixed_time=6000, height=48)
