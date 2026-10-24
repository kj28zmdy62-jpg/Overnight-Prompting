"""The Grid - a black plain ruled with glowing lines, wireframe shapes standing on it, and a striped sun on a
horizon that never gets any closer. Light pulses run along the lines toward the one place they fail to meet."""
import numpy as np
from assets import cube, tex, shaped, level, sound, lang, entity_model, entity_tex, part, ITEM_MODELS
import tex as T
import synth as Y
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32
CYAN = "#30e8ff"


def black():
    n = T.noise("grblk", sigma=4)
    return T.grain(T.ramp(n, "#020306", "#060810"), "grblk", 0.01)


def line(kind, color=CYAN, core="#e8ffff"):
    """A glowing line through the middle of a black tile, with a soft falloff."""
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    img = black()
    d = np.full((S, S), 99.0)
    if kind in ("x", "cross"):
        d = np.minimum(d, np.abs(yy - 15.5))
    if kind in ("z", "cross"):
        d = np.minimum(d, np.abs(xx - 15.5))
    glow = np.exp(-(d ** 2) / 10.0)
    img = T.blend(img, T.solid(color), glow[..., None].squeeze() * 0.85)
    img = T.blend(img, T.solid(core), (d < 1.2).astype(float))
    return img


def wire():
    img = T.solid(CYAN)
    img[12:20, 12:20] = T.hexc("#e8ffff")
    return img


def sun():
    rgb, a = sun_face()
    full = np.zeros((32, 128, 3))
    fa = np.zeros((32, 128))
    full[:, :64] = rgb
    fa[:, :64] = a
    full[:, 64:] = rgb[:, ::-1]
    fa[:, 64:] = a[:, ::-1]
    return full, fa


def sun_face():
    """64x32: half a sun on the horizon, cut by horizontal bands that thicken toward the bottom."""
    W, H = 64, 32
    yy, xx = np.mgrid[0:H, 0:W].astype(float)
    r = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2)
    disc = r < 30
    grad = T.ramp(np.clip(yy / 32.0, 0, 1), "#ffe860", "#ff8040", "#ff2a8a")
    band = np.zeros((H, W), bool)
    for k, y0 in enumerate((14, 19, 23, 26, 29)):
        band |= (yy >= y0) & (yy < y0 + 1 + k * 0.5)
    a = (disc & ~band).astype(float)
    # the glow layer ignores alpha, so everything outside the sun must be black
    return grad * a[..., None], a


tex("gr_black", black)
tex("gr_line_x", lambda: line("x"))
tex("gr_line_z", lambda: line("z"))
tex("gr_cross", lambda: line("cross"))
tex("gr_pulse", lambda: line("cross", "#ffffff", "#ffffff"))
tex("gr_wire", wire)
tex("gr_red", lambda: T.solid("#ff2a4a"))

cube("gr_black", "gr_black", display="The Grid", sound="glass", flat=True)
cube("gr_line_x", "gr_line_x", display="Grid Line", sound="glass", flat=True, light=6, emissive=True)
cube("gr_line_z", "gr_line_z", display="Grid Line", sound="glass", flat=True, light=6, emissive=True)
cube("gr_cross", "gr_cross", display="Grid Node", sound="glass", flat=True, light=8, emissive=True)
cube("gr_pulse", "gr_pulse", display="Pulse", sound="glass", flat=True, light=13, emissive=True)
# wireframe edges: one standing bar and one lying bar that rotates
shaped("gr_wire_y", [(7, 0, 7, 9, 16, 9, "w")], {"w": "gr_wire"}, display="Wireframe", light=9, emissive=True,
       collides=False, unbreakable=True, sound="glass", render="solid")
shaped("gr_wire_h", [(7, 7, 0, 9, 9, 16, "w")], {"w": "gr_wire"}, display="Wireframe", light=9, emissive=True,
       collides=False, unbreakable=True, sound="glass", render="solid")
shaped("gr_wire_node", [(6, 6, 6, 10, 10, 10, "w")], {"w": "gr_wire"}, display="Wireframe", light=10, emissive=True,
       collides=False, unbreakable=True, sound="glass", render="solid")
shaped("gr_wire_red", [(7, 0, 7, 9, 16, 9, "w")], {"w": "gr_red"}, display="Wireframe", light=7, emissive=True,
       collides=False, unbreakable=True, sound="glass", render="solid")

# ------------------------------------------------------------------ the horizon and the sentinels

entity_model("gr_horizon", [part("body", (0, 0, 0), cubes=[(0, 0, -32, -32, 0, 64, 32, 0)])], tex_size=(128, 32))
entity_tex("gr_horizon", sun)


def sentinel_tex():
    c = Canvas()

    def wireframe(w, h):
        img = np.zeros((h, w, 3))
        img[:] = T.hexc("#120206")
        img[0, :] = img[-1, :] = T.hexc("#ff2a4a")
        img[:, 0] = img[:, -1] = T.hexc("#ff2a4a")
        return img

    def face(w, h):
        img = wireframe(w, h)
        img[3, 1:7] = T.hexc("#ff8a9a")
        return img

    paint_humanoid(c, wireframe, wireframe, wireframe, wireframe, face=face)
    return c.out()


def sentinel_glow():
    rgb, a = sentinel_tex()
    a = (rgb[..., 0] > 0.5).astype(float)
    return rgb, a


entity_model("gr_sentinel", humanoid(arm_len=13, leg_len=14))
entity_tex("gr_sentinel", sentinel_tex)
entity_tex("gr_sentinel_glow", sentinel_glow)
for n, d in (("gr_horizon", "The Horizon"), ("gr_sentinel", "Sentinel")):
    lang(f"entity.noclip.{n}", d)
    lang(f"item.noclip.{n}_spawn_egg", f"{d} Spawn Egg")
    ITEM_MODELS[f"{n}_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def pad():
    """An analogue pad left running in an empty studio: detuned saws through a slow filter, and pings far off."""
    d = 32.0
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for f in (55.0, 82.4, 110.0, 164.8):
        for det in (-0.25, 0.25):
            ff = round((f + det) * d) / d
            x += ((t * ff) % 1.0 - 0.5)
    sweep = 400 + 300 * np.sin(2 * np.pi * t / 16.0)
    out = np.zeros_like(x)
    # crude time-varying low-pass: blend two filtered copies
    lo, hi = Y.lp(x, 300), Y.lp(x, 900)
    mix = (sweep - 100) / 600.0
    out = lo * (1 - mix) + hi * mix
    pings = np.zeros_like(t)
    for k in range(5):
        n = int(0.3 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(pings, np.sin(2 * np.pi * (1760 if k % 2 else 1318.5) * tt) * np.exp(-tt * 12), 3 + k * 6.1, 0.2)
    pings = Y.reverb(pings, 3.0, 0.8, "grping")[: len(t)]
    y = Y.loopable(Y.norm(out * 0.6 + pings, 0.45), 2.0)
    return Y.stereoize(y, 0.006), "stereo"


def far_ping():
    d = 5.0
    out = np.zeros(int(d * Y.SR))
    n = int(0.4 * Y.SR)
    tt = np.arange(n) / Y.SR
    for k, f in enumerate((987.8, 1318.5, 1975.5)):
        Y.place(out, np.sin(2 * np.pi * f * tt) * np.exp(-tt * 8), k * 0.18, 0.5)
    return Y.norm(Y.reverb(out, 4.0, 0.85, "grfp"), 0.5)


def sweep():
    d = 3.0
    t = Y.t_axis(d)
    x = Y.white(d, "grsw")
    x = Y.lp(x, 4000) * np.sin(np.pi * t / d) ** 2
    lo = Y.lp(x, 600)
    return Y.norm((x * (t / d) + lo * (1 - t / d)) * 0.5, 0.5)


def chirp():
    d = 0.5
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(2000 + 3000 * np.sin(2 * np.pi * 9 * t)) / Y.SR)) * np.exp(-t * 6)
    return Y.norm(Y.lp(x, 6000) * 0.4, 0.4)


def blip():
    d = 0.25
    t = Y.t_axis(d)
    return Y.norm(np.sin(2 * np.pi * 1318.5 * t) * np.exp(-t * 20), 0.4)


def derez():
    d = 1.5
    t = Y.t_axis(d)
    f = 1200 * np.exp(-t * 2.5) + 60
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)) * np.exp(-t * 1.5)
    x = np.repeat(x[::16], 16)[: len(t)]
    return Y.norm(x * 0.6, 0.8)


sound("ambient.the_grid", [("ambient/the_grid", pad)], stream=True)
sound("ambient.the_grid.additions", [("ambient/the_grid_ping", far_ping), ("ambient/the_grid_sweep", sweep),
                                     ("ambient/the_grid_chirp", chirp)], subtitle="Signals on the Grid")
sound("event.the_grid_blip", [("event/the_grid_blip", blip)], subtitle="A pulse passes")
sound("entity.gr_sentinel.derez", [("entity/gr_sentinel_derez", derez)], subtitle="Something derezzes")

level("the_grid", fog="#02040a", sky="#02040a", ambient="ambient.the_grid", additions="ambient.the_grid.additions",
      additions_chance=0.004, fixed_time=18000, height=64, ceiling=False)
