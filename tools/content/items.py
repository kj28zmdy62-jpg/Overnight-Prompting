"""Survival kit: camcorder, chalk and its marks, and crafting recipes so you can prepare before you noclip."""
import numpy as np
from assets import item_tex, tex, shaped, recipe, lang, ITEM_MODELS
import tex as T
from content.common import sprite

item_tex("camcorder", lambda: sprite([
    "................",
    "................",
    "................",
    "..kkkkkkkkk.....",
    ".kBBBBBBBBBkgg..",
    ".kBrBBBBBBBkggg.",
    ".kBBBBBBBBBkLLg.",
    ".kBBBBBBBBBkggg.",
    ".kBBBBBBBBBkgg..",
    "..kkkkkkkkk.....",
    "...ss...........",
    "...ss...........",
    "................",
    "................",
    "................",
    "................"], {"k": "#141416", "B": "#2c2c32", "r": "#e02828", "g": "#4a4a50", "L": "#8ab0d0",
                         "s": "#3a3a40"}))

item_tex("chalk", lambda: sprite([
    "................",
    "................",
    "................",
    "................",
    "...........ww...",
    "..........wWWw..",
    ".........wWWw...",
    "........wWWw....",
    ".......wWWw.....",
    "......wWWw......",
    ".....wWWw.......",
    "....wWWw........",
    "....wWw.........",
    ".....w..........",
    "................",
    "................"], {"w": "#c8c4b8", "W": "#f2f0ea"}))


def chalk_mark(kind):
    def f():
        rgb = np.ones((32, 32, 3)) * T.hexc("#ecebe6")
        a = np.zeros((32, 32))
        yy, xx = np.mgrid[0:32, 0:32]
        if kind == "arrow":
            shaft = (np.abs(yy - 16) <= 1) & (xx > 5) & (xx < 25)
            head = (np.abs(yy - 16) <= (xx - 18)) & (np.abs(yy - 16) >= (xx - 21)) & (xx >= 18) & (xx <= 27)
            m = shaft | head
        elif kind == "cross":
            m = ((np.abs(xx - yy) <= 1) | (np.abs(xx + yy - 31) <= 1)) & (xx > 6) & (xx < 26)
        elif kind == "tally":
            m = np.zeros((32, 32), bool)
            for k in range(4):
                m |= (np.abs(xx - (8 + k * 4)) <= 0) & (yy > 8) & (yy < 24)
            m |= (np.abs((yy - 22) + (xx - 6) * 0.75) <= 1) & (xx > 5) & (xx < 25)
        else:
            d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
            m = np.abs(d - 9) <= 1
        rough = T.noise(f"chalk{kind}", sigma=0.4) > 0.25
        a = (m & rough).astype(float) * 0.9
        return rgb, a
    return f


for kind in ("arrow", "cross", "tally", "circle"):
    tex(f"chalk_{kind}", chalk_mark(kind))
    shaped(f"chalk_{kind}", [(0, 0, 15.9, 16, 16, 16, {"north": "m", "south": None, "up": None, "down": None,
                                                         "west": None, "east": None})],
           {"m": f"chalk_{kind}"}, display="Chalk Mark", collides=False, render="cutout", strength=0.1,
           sound="gravel")

lang("item.noclip.camcorder", "Camcorder")
lang("item.noclip.chalk", "Chalk")


def shaped_recipe(name, pattern, key, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                  "key": {k: {"item": v} for k, v in key.items()},
                  "result": {"id": result, "count": count}})


def shapeless(name, items, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": "misc",
                  "ingredients": [{"item": i} for i in items], "result": {"id": result, "count": count}})


shaped_recipe("flashlight", [" G ", "IRI", " I "], {"G": "minecraft:glass_pane", "I": "minecraft:iron_ingot",
                                                    "R": "minecraft:redstone"}, "noclip:flashlight")
shaped_recipe("battery", ["C", "R", "C"], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone"},
              "noclip:battery", 2)
shapeless("almond_water", ["minecraft:potion", "minecraft:sugar", "minecraft:wheat_seeds"], "noclip:almond_water")
shapeless("journal", ["minecraft:book", "minecraft:charcoal"], "noclip:journal")
shapeless("chalk", ["minecraft:calcite", "minecraft:bone_meal"], "noclip:chalk")
shaped_recipe("camcorder", ["III", "GRB", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass",
                                                   "R": "minecraft:redstone", "B": "noclip:battery"}, "noclip:camcorder")
