"""The Void - nothing at all. Black, the hum, and something very far away."""
import numpy as np
from assets import cube, tex, level, sound, entity_model, entity_tex, lang, ITEM_MODELS
import tex as T
import synth as Y
from content.deepend_audio import wrapverb, fit
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas

S = 32


def nothing():
    return np.zeros((S, S, 3)), np.zeros((S, S))


tex("void_nothing", nothing)
cube("the_void_floor", "void_nothing", display="Nothing (Solid)", render="cutout", sound="snow")

entity_model("void_far_thing", humanoid(arm_len=22, leg_len=20))


def far_thing_tex():
    c = Canvas()
    black = lambda w, h: np.zeros((h, w, 3))
    paint_humanoid(c, black, black, black, black)
    return c.out()


def far_thing_glow():
    """Pale, faceless, very tall. The only thing here that light touches."""
    c = Canvas()
    pale = lambda w, h: T.ramp(T.noise(f"vft{w}{h}", shape=(h, w), sigma=1.2), "#bdb9b0", "#e8e4dc")

    def face(w, h):
        img = pale(w, h)
        img[3:5, 1:3] *= 0.55
        img[3:5, 5:7] *= 0.55
        return img
    paint_humanoid(c, pale, pale, pale, pale, face=face)
    rgb, a = c.out()
    return rgb, (rgb.sum(axis=2) > 0.05).astype(float)


entity_tex("void_far_thing", far_thing_tex)
entity_tex("void_far_thing_glow", far_thing_glow)
lang("entity.noclip.void_far_thing", "Something Far Away")
lang("item.noclip.void_far_thing_spawn_egg", "Something Far Away Spawn Egg")
ITEM_MODELS["void_far_thing_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}


def void_hum():
    """The hum, with nothing to hum in. Lower than Level 0's, and closer."""
    d = 30.0
    t = Y.t_axis(d)
    x = Y.drone(d, [40.0, 60.0, 80.0], "vhum", detune=0.15, lfo=1 / 15.0) * 0.6
    x += Y.lp(Y.hum(d, 60.0, "vhum60", buzz=0.1, harmonics=(1, 2, 3)), 400) * 0.25
    x += Y.lp(Y.brown(d, "vroom"), 60) * 0.3
    return Y.stereoize(Y.loopable(Y.norm(x, 0.5), 2.0), 0.006), "stereo"


def groan():
    d = 6.0
    t = Y.t_axis(d)
    f0 = 55 + 25 * np.sin(np.pi * t / d) - t * 3
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) + 0.4 * np.sin(2 * np.pi * np.cumsum(f0 * 2.01) / Y.SR)
    x *= np.sin(np.pi * t / d) ** 2
    return Y.reverb(Y.norm(x, 0.6), 5.0, 0.8, "vgroan", damp=1200)


def bell():
    d = 7.0
    t = Y.t_axis(d)
    x = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * k) for f, k in ((196.0, 0.5), (392.4, 0.8), (523.0, 1.2), (619.0, 1.5)))
    return Y.reverb(Y.lp(Y.norm(x, 0.5), 2000), 5.0, 0.85, "vbell")


def own_name():
    x = Y.whisper("vname", 1.2)
    return Y.reverb(x, 4.0, 0.8, "vname")


sound("ambient.the_void", [("ambient/the_void", void_hum)], stream=True)
sound("ambient.the_void.additions", [("ambient/the_void_groan", groan), ("ambient/the_void_bell", bell),
                                    ("ambient/the_void_name", own_name)], subtitle="Nothing")

level("the_void", fog="#000000", sky="#000000", ambient="ambient.the_void", additions="ambient.the_void.additions",
      additions_chance=0.0025, fixed_time=18000, height=128)
