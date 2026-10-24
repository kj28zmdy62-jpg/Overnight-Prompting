"""Asset registry: blocks, models, blockstates, loot tables, lang and level data.

Theme modules in tools/content/ call these functions; build.py runs them all and
writes everything under src/main/resources.
"""
import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "noclip")
DATA = os.path.join(RES, "data", "noclip")

BLOCKS = []          # specs written to /noclip/blocks.json for Java
TEXTURES = {}        # texture name -> callable returning (rgb, alpha|None)
BLOCK_MODELS = {}    # model name -> json
ITEM_MODELS = {}     # item name -> json
BLOCKSTATES = {}     # block name -> json
LOOT = {}            # block name -> json
LANG = {}            # key -> text
ITEM_TEXTURES = {}   # item name -> callable
LEVELS = {}          # level id -> dict
SOUNDS = {}          # event -> dict(files=[(name, fn)], stream=bool, subtitle)


def title(name):
    return " ".join(w.capitalize() for w in name.split("_"))


def tex(name, fn):
    """Register a block texture painter. fn() -> rgb array or (rgb, alpha)."""
    TEXTURES[name] = fn
    return "noclip:block/" + name


def item_tex(name, fn):
    ITEM_TEXTURES[name] = fn
    return "noclip:item/" + name


def _ref(t):
    return t if ":" in t else "noclip:block/" + t


def _block(name, kind, display=None, item_model=None, **props):
    spec = {"name": name, "kind": kind}
    spec.update({k: v for k, v in props.items() if v is not None})
    BLOCKS.append(spec)
    LANG[f"block.noclip.{name}"] = display or title(name)
    if not props.get("unbreakable"):
        LOOT[name] = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"noclip:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        }
    ITEM_MODELS[name] = item_model or {"parent": f"noclip:block/{name}"}
    return name


def _simple_state(name, model=None):
    BLOCKSTATES[name] = {"variants": {"": {"model": f"noclip:block/{model or name}"}}}


# ---------------------------------------------------------------- block kinds

def flat_model(down, up, side):
    """Cube model without directional face shading (ceilings, glowing panels)."""
    d, u, s = _ref(down), _ref(up), _ref(side)
    return {"parent": "noclip:base/cube_flat",
            "textures": {"down": d, "up": u, "north": s, "south": s, "west": s, "east": s, "particle": s}}


def cube(name, texture, display=None, sound="stone", unbreakable=True, light=None, render=None,
         emissive=None, strength=None, kind="cube", flat=False, **extra):
    """Full cube, same texture on every face. flat=True disables face shading."""
    BLOCK_MODELS[name] = flat_model(texture, texture, texture) if flat else \
        {"parent": "minecraft:block/cube_all", "textures": {"all": _ref(texture)}}
    _simple_state(name)
    return _block(name, kind, display, sound=sound, unbreakable=unbreakable or None, light=light,
                  render=render, emissive=emissive, strength=strength, **extra)


def cube_faces(name, display=None, sound="stone", unbreakable=True, light=None, render=None, emissive=None,
               strength=None, kind="cube", top=None, bottom=None, side=None, flat=False, **extra):
    """Full cube with distinct top / bottom / side textures."""
    BLOCK_MODELS[name] = flat_model(bottom, top, side) if flat else \
        {"parent": "minecraft:block/cube_bottom_top",
         "textures": {"top": _ref(top), "bottom": _ref(bottom), "side": _ref(side)}}
    _simple_state(name)
    return _block(name, kind, display, sound=sound, unbreakable=unbreakable or None, light=light,
                  render=render, emissive=emissive, strength=strength, **extra)


def pillar(name, end, side, display=None, sound="stone", unbreakable=True, **extra):
    BLOCK_MODELS[name] = {"parent": "minecraft:block/cube_column", "textures": {"end": _ref(end), "side": _ref(side)}}
    BLOCK_MODELS[name + "_horizontal"] = {"parent": "minecraft:block/cube_column_horizontal",
                                          "textures": {"end": _ref(end), "side": _ref(side)}}
    BLOCKSTATES[name] = {"variants": {
        "axis=y": {"model": f"noclip:block/{name}"},
        "axis=z": {"model": f"noclip:block/{name}_horizontal", "x": 90},
        "axis=x": {"model": f"noclip:block/{name}_horizontal", "x": 90, "y": 90},
    }}
    return _block(name, "pillar", display, sound=sound, unbreakable=unbreakable or None, **extra)


def flicker_light(name, on_tex, off_tex, side_tex, top_tex=None, display=None, light=14, flicker=0.25,
                  sound="glass", unbreakable=True, face="down"):
    """Ceiling (face=down) or wall/floor light that randomly cuts out."""
    top_tex = top_tex or side_tex
    for suffix, t in (("", on_tex), ("_off", off_tex)):
        if face == "down":
            BLOCK_MODELS[name + suffix] = flat_model(t, top_tex, side_tex)
        elif face == "up":
            BLOCK_MODELS[name + suffix] = flat_model(side_tex, t, side_tex)
        else:
            BLOCK_MODELS[name + suffix] = flat_model(t, t, t)
    BLOCKSTATES[name] = {"variants": {"lit=true": {"model": f"noclip:block/{name}"},
                                      "lit=false": {"model": f"noclip:block/{name}_off"}}}
    return _block(name, "flicker", display, sound=sound, unbreakable=unbreakable or None, light=light,
                  flicker=flicker, emissive=True)


def shaped(name, boxes, textures, display=None, sound="wood", unbreakable=False, collides=True, render="cutout",
           light=None, strength=1.0, emissive=None, particle=None, kind="shaped", **extra):
    """Rotatable model built from boxes. boxes: [(x1,y1,z1,x2,y2,z2, tex_or_facemap), ...] facing north.

    tex_or_facemap is a texture key from `textures`, or a dict face->key; faces
    may be omitted with None to cull them.
    """
    elements = []
    for b in boxes:
        x1, y1, z1, x2, y2, z2, t = b
        faces = {}
        for f in ("down", "up", "north", "south", "west", "east"):
            key = t.get(f, t.get("all")) if isinstance(t, dict) else t
            if key is None:
                continue
            uv = _uv(f, x1, y1, z1, x2, y2, z2)
            faces[f] = {"texture": "#" + key, "uv": uv}
        elements.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces})
    tx = {k: _ref(v) for k, v in textures.items()}
    tx["particle"] = _ref(particle or list(textures.values())[0])
    BLOCK_MODELS[name] = {"parent": "minecraft:block/block", "textures": tx, "elements": elements}
    BLOCKSTATES[name] = {"variants": {
        "facing=north": {"model": f"noclip:block/{name}"},
        "facing=east": {"model": f"noclip:block/{name}", "y": 90},
        "facing=south": {"model": f"noclip:block/{name}", "y": 180},
        "facing=west": {"model": f"noclip:block/{name}", "y": 270},
    }}
    if kind not in ("shaped", "pickup"):
        # exits/hazards/pickups with a box model have no facing property
        BLOCKSTATES[name] = {"variants": {"": {"model": f"noclip:block/{name}"}}}
    return _block(name, kind, display, sound=sound, unbreakable=unbreakable or None, collides=collides,
                  render=render, light=light, strength=strength, emissive=emissive,
                  boxes=[list(b[:6]) for b in boxes], **extra)


def _uv(face, x1, y1, z1, x2, y2, z2):
    if face in ("up", "down"):
        return [x1, z1, x2, z2]
    if face in ("north", "south"):
        return [x1, 16 - y2, x2, 16 - y1]
    return [z1, 16 - y2, z2, 16 - y1]


def pane(name, texture, edge=None, display=None, sound="metal", unbreakable=True, render="cutout"):
    """Thin connecting wall (IronBarsBlock), e.g. cubicle partitions, chain-link fence."""
    t, e = _ref(texture), _ref(edge or texture)
    for part in ("post", "side", "side_alt", "noside", "noside_alt"):
        BLOCK_MODELS[f"{name}_{part}"] = {"parent": f"minecraft:block/template_glass_pane_{part}",
                                          "textures": {"pane": t, "edge": e}}
    m = lambda p: f"noclip:block/{name}_{p}"
    BLOCKSTATES[name] = {"multipart": [
        {"apply": {"model": m("post")}},
        {"when": {"north": "true"}, "apply": {"model": m("side")}},
        {"when": {"east": "true"}, "apply": {"model": m("side"), "y": 90}},
        {"when": {"south": "true"}, "apply": {"model": m("side_alt")}},
        {"when": {"west": "true"}, "apply": {"model": m("side_alt"), "y": 90}},
        {"when": {"north": "false"}, "apply": {"model": m("noside")}},
        {"when": {"east": "false"}, "apply": {"model": m("noside_alt")}},
        {"when": {"south": "false"}, "apply": {"model": m("noside_alt"), "y": 90}},
        {"when": {"west": "false"}, "apply": {"model": m("noside"), "y": 270}},
    ]}
    return _block(name, "pane", display, item_model={"parent": "minecraft:item/generated", "textures": {"layer0": t}},
                  sound=sound, unbreakable=unbreakable or None, render=render)


def fence(name, texture, display=None, sound="metal", unbreakable=True, render="cutout"):
    t = _ref(texture)
    BLOCK_MODELS[name + "_post"] = {"parent": "minecraft:block/fence_post", "textures": {"texture": t}}
    BLOCK_MODELS[name + "_side"] = {"parent": "minecraft:block/fence_side", "textures": {"texture": t}}
    BLOCK_MODELS[name + "_inventory"] = {"parent": "minecraft:block/fence_inventory", "textures": {"texture": t}}
    m = lambda p: f"noclip:block/{name}_{p}"
    BLOCKSTATES[name] = {"multipart": [
        {"apply": {"model": m("post")}},
        {"when": {"north": "true"}, "apply": {"model": m("side"), "uvlock": True}},
        {"when": {"east": "true"}, "apply": {"model": m("side"), "y": 90, "uvlock": True}},
        {"when": {"south": "true"}, "apply": {"model": m("side"), "y": 180, "uvlock": True}},
        {"when": {"west": "true"}, "apply": {"model": m("side"), "y": 270, "uvlock": True}},
    ]}
    return _block(name, "fence", display, item_model={"parent": m("inventory")}, sound=sound,
                  unbreakable=unbreakable or None, render=render)


def slab(name, texture, display=None, sound="stone", unbreakable=True, full=None):
    t = _ref(texture)
    BLOCK_MODELS[name] = {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}}
    BLOCK_MODELS[name + "_top"] = {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}}
    BLOCK_MODELS[name + "_double"] = {"parent": "minecraft:block/cube_all", "textures": {"all": t}}
    BLOCKSTATES[name] = {"variants": {
        "type=bottom": {"model": f"noclip:block/{name}"},
        "type=top": {"model": f"noclip:block/{name}_top"},
        "type=double": {"model": f"noclip:block/{full}" if full else f"noclip:block/{name}_double"},
    }}
    return _block(name, "slab", display, sound=sound, unbreakable=unbreakable or None)


def stairs(name, texture, base, display=None, sound="stone", unbreakable=True):
    t = _ref(texture)
    for suf, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        BLOCK_MODELS[name + suf] = {"parent": f"minecraft:block/{parent}",
                                    "textures": {"bottom": t, "top": t, "side": t}}
    variants = {}
    rot = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing in ("east", "south", "west", "north"):
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = name + ("" if shape == "straight" else ("_inner" if "inner" in shape else "_outer"))
                y = rot[facing]
                if shape in ("inner_left", "outer_left"):
                    y = (y + 270) % 360
                x = 0
                if half == "top":
                    x = 180
                    if shape in ("inner_left", "outer_left"):
                        y = (y + 90) % 360
                    elif shape in ("inner_right", "outer_right"):
                        y = (y + 90) % 360
                v = {"model": f"noclip:block/{model}"}
                if x:
                    v["x"] = x
                if y:
                    v["y"] = y
                if x or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    BLOCKSTATES[name] = {"variants": variants}
    return _block(name, "stairs", display, sound=sound, unbreakable=unbreakable or None, base=base)


def carpet(name, texture, display=None, sound="wool", unbreakable=False):
    BLOCK_MODELS[name] = {"parent": "minecraft:block/carpet", "textures": {"wool": _ref(texture)}}
    _simple_state(name)
    return _block(name, "carpet", display, sound=sound, unbreakable=unbreakable or None, render="cutout")


def cross(name, texture, display=None, sound="grass", kind="shaped", collides=False, **extra):
    """X-shaped plant-like sprite (cobwebs, wires, plants)."""
    BLOCK_MODELS[name] = {"parent": "minecraft:block/cross", "textures": {"cross": _ref(texture)}}
    BLOCKSTATES[name] = {"variants": {f"facing={f}": {"model": f"noclip:block/{name}"}
                                      for f in ("north", "east", "south", "west")}}
    return _block(name, kind, display, item_model={"parent": "minecraft:item/generated",
                                                   "textures": {"layer0": _ref(texture)}},
                  sound=sound, render="cutout", collides=collides, boxes=[[2, 0, 2, 14, 13, 14]], **extra)


def exit_block(name, texture, exit_type, trigger="INSIDE", display=None, solid=False, render="solid",
               light=None, emissive=None, sound="stone", model=None):
    """A way out. Uses a cube_all model unless a custom model json is given."""
    BLOCK_MODELS[name] = model or {"parent": "minecraft:block/cube_all", "textures": {"all": _ref(texture)}}
    _simple_state(name)
    return _block(name, "exit", display, sound=sound, unbreakable=True, exit=exit_type, trigger=trigger,
                  solid=solid, render=render, light=light, emissive=emissive,
                  no_collision=None if solid else True)


def hazard(name, texture, damage=2.0, display=None, solid=True, sound="metal", light=None, emissive=None,
           render="solid"):
    BLOCK_MODELS[name] = {"parent": "minecraft:block/cube_all", "textures": {"all": _ref(texture)}}
    _simple_state(name)
    return _block(name, "hazard", display, sound=sound, unbreakable=True, damage=damage, solid=solid,
                  light=light, emissive=emissive, render=render, no_collision=None if solid else True)


def lang(key, text):
    LANG[key] = text


# ---------------------------------------------------------------- level data

def level(id, *, fog="#000000", sky="#000000", water="#3f76e4", water_fog="#050533", ambient=None,
          additions=None, additions_chance=0.006, mood=None, fixed_time=18000, skylight=False,
          ceiling=True, ambient_light=0.0, height=256, effects="minecraft:overworld", particle=None,
          particle_chance=0.0):
    LEVELS[id] = dict(fog=fog, sky=sky, water=water, water_fog=water_fog, ambient=ambient, additions=additions,
                      additions_chance=additions_chance, mood=mood, fixed_time=fixed_time, skylight=skylight,
                      ceiling=ceiling, ambient_light=ambient_light, height=height, effects=effects,
                      particle=particle, particle_chance=particle_chance)


def sound(event, files, stream=False, subtitle=None, volume=1.0, attenuation=None):
    """files: list of (file_name, painter_fn) - painter returns (samples float32 mono/stereo, rate)."""
    SOUNDS[event] = dict(files=files, stream=stream, subtitle=subtitle, volume=volume)
    if subtitle:
        LANG[f"subtitles.noclip.{event}"] = subtitle
