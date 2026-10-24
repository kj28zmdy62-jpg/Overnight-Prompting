"""Level Zero-Zero - white. No texture, no shading, no shadows. You cannot tell where a wall ends.
The only marks are footprints: yours, which fade, and someone else's, which lead somewhere."""
import numpy as np
from assets import cube, tex, shaped, exit_block, level, sound, lang, entity_model, entity_tex, ITEM_MODELS, BLOCK_MODELS
import tex as T
import synth as Y
from content.rb_shared import split, ellipse
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32
WHITE = "#f0f0ee"


def white():
    # almost nothing: a variation of under one percent, so the eye has nothing to hold on to
    img = T.solid(WHITE)
    return np.clip(img + (T.noise("zzw", sigma=6)[..., None] - 0.5) * 0.008, 0, 1)


def shadow():
    """A soft shadow on the floor, 64x64 over 2x2 blocks, with nothing above it to cast it."""
    H = 64
    yy, xx = np.mgrid[0:H, 0:H]
    d = np.sqrt(((xx - 31.5) / 26.0) ** 2 + ((yy - 33.5) / 22.0) ** 2)
    a = np.clip(1.0 - d, 0, 1) ** 1.4
    a = a * (0.85 + 0.15 * T.noise("zzsh", shape=(H, H), sigma=3))
    img = T.blend(np.ones((H, H, 3)) * T.hexc(WHITE), T.solid("#6a6a70", shape=(H, H)), a * 0.9)
    return img


def prints(bare):
    """A pair of footprints, toes north. Yours are shoes; theirs are bare."""
    rgb = np.ones((S, S, 3)) * T.hexc("#9a9a9e" if not bare else "#7a7a80")
    a = np.zeros((S, S))
    for (cx, cy) in ((11, 20), (21, 11)):
        if bare:
            sole = ellipse(S, S, cx, cy + 2, 3.0, 6.0)
            for t, (ox, oy, r) in enumerate(((-2.5, -6, 1.2), (-0.8, -7, 1.1), (0.9, -7, 1.0), (2.4, -6.3, 0.9), (3.4, -5, 0.8))):
                sole |= ellipse(S, S, cx + ox, cy + oy, r, r)
            arch = ellipse(S, S, cx + 2.2, cy + 2.5, 1.4, 2.8)
            m = sole & ~arch
        else:
            m = ellipse(S, S, cx, cy - 1.5, 3.2, 4.4) | ellipse(S, S, cx, cy + 5, 2.6, 2.6)
            tread = ((np.mgrid[0:S, 0:S][0] % 2) == 0)
            m = m & ~(tread & ellipse(S, S, cx, cy - 1.5, 2.0, 3.2))
        a[m] = 0.55 + 0.25 * T.noise("zzpr" + str(bare), sigma=0.8)[m]
    return rgb, a


tex("zz_white", white)
for idx, q in enumerate(("tl", "tr", "bl", "br")):
    tex(f"zz_shadow_{q}", (lambda i: lambda: split(shadow(), 2, 2)[i])(idx))
tex("zz_prints", lambda: prints(False))
tex("zz_prints_bare", lambda: prints(True))

cube("zz_white", "zz_white", display="Nothing in Particular", sound="stone", flat=True)
cube("zz_white_soft", "zz_white", display="Nothing in Particular", sound="wool", flat=True)
for q in ("tl", "tr", "bl", "br"):
    exit_block(f"zz_shadow_{q}", f"zz_shadow_{q}", "FALL", display="A Shadow",
               model={"parent": "noclip:base/cube_flat",
                      "textures": {k: f"noclip:block/zz_shadow_{q}" for k in
                                   ("down", "up", "north", "south", "west", "east", "particle")}})
for name, t, disp in (("zz_footprints", "zz_prints", "Your Footprints"), ("zz_footprints_bare", "zz_prints_bare", "Footprints")):
    shaped(name, [(0, 0, 0, 16, 0.15, 16, {"all": None, "up": "p"})], {"p": t}, display=disp, collides=False,
           unbreakable=True, render="cutout", sound="stone")

# ------------------------------------------------------------------ the Blank


def blank_tex():
    c = Canvas()
    w = lambda ww, hh: np.ones((hh, ww, 3)) * T.hexc("#f2f2f0")
    paint_humanoid(c, w, w, w, w)
    return c.out()


entity_model("zz_blank", humanoid(arm_len=14, leg_len=14))
entity_tex("zz_blank", blank_tex)
lang("entity.noclip.zz_blank", "The Blank")
lang("item.noclip.zz_blank_spawn_egg", "The Blank Spawn Egg")
ITEM_MODELS["zz_blank_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def silence():
    """Almost nothing. The ringing in your own ears, and air that does not move."""
    d = 24.0
    t = Y.t_axis(d)
    ring = np.sin(2 * np.pi * round(8200 * d) / d * t) * 0.012 * (0.7 + 0.3 * np.sin(2 * np.pi * t / 12.0))
    ring += np.sin(2 * np.pi * round(11100 * d) / d * t) * 0.004
    air = Y.lp(Y.pink(d, "zzair"), 900) * 0.05
    x = Y.loopable(ring + air, 1.0)
    return Y.stereoize(x * 1.4, 0.0002), "stereo"


def one_step():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.footstep("zzstep", "tile"), 0.1, 0.8)
    return Y.reverb(out, 2.8, 0.75, "zzstep")


def bump():
    out = np.zeros(int(2.0 * Y.SR))
    Y.place(out, Y.impact("zzbump", 0.3), 0.05, 0.6)
    return Y.reverb(Y.lp(out, 600), 2.0, 0.7, "zzbump")


def breath():
    d = 1.6
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "zzbr"), 400, 2400) * np.clip(np.sin(np.pi * t / 1.3), 0, 1) * (t < 1.3)
    return Y.norm(x, 0.4)


def erase():
    """The Blank touched you. A burst of white, and then less of you."""
    d = 2.0
    t = Y.t_axis(d)
    x = Y.white(d, "zzer") * np.exp(-t * 2.5) * 0.8
    x += np.sin(2 * np.pi * 8200 * t) * np.exp(-t * 0.8) * 0.3
    return Y.norm(x, 0.85), "stereo"


sound("ambient.zero_zero", [("ambient/zero_zero", silence)], stream=True)
sound("ambient.zero_zero.additions",
      [("ambient/zero_zero_step", one_step), ("ambient/zero_zero_bump", bump), ("ambient/zero_zero_breath", breath)],
      subtitle="Something else in the white")
sound("event.zero_zero_erase", [("event/zero_zero_erase", erase)], subtitle="Everything goes white")

level("zero_zero", fog="#f0f0ee", sky="#f0f0ee", ambient="ambient.zero_zero", additions="ambient.zero_zero.additions",
      additions_chance=0.0025, fixed_time=6000, height=32, ambient_light=1.0)
