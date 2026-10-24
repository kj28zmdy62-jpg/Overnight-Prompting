#!/usr/bin/env python3
"""Generate every asset and data file for the mod.

    python3 tools/build.py            # textures, models, data; sounds only if missing
    python3 tools/build.py --sounds   # also re-synthesise every sound
    python3 tools/build.py --only level_0   # only re-synthesise sounds whose event contains this text
"""
import importlib
import json
import os
import pkgutil
import shutil
import sys

sys.path.insert(0, os.path.dirname(__file__))
import assets as A  # noqa: E402
import tex  # noqa: E402
import synth  # noqa: E402
import content  # noqa: E402


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def clean(path):
    if os.path.isdir(path):
        shutil.rmtree(path)


def main():
    force_sounds = "--sounds" in sys.argv
    only = None
    if "--only" in sys.argv:
        only = sys.argv[sys.argv.index("--only") + 1]
        force_sounds = True

    for m in sorted(pkgutil.iter_modules(content.__path__), key=lambda m: m.name):
        importlib.import_module("content." + m.name)

    # generated folders are wiped so removed content does not linger
    for sub in ("textures/block", "textures/item", "textures/entity", "models/block", "models/item", "blockstates"):
        clean(os.path.join(A.ASSETS, sub))
    for sub in ("dimension", "dimension_type", "worldgen/biome", "loot_table/blocks", "recipe", "advancement"):
        clean(os.path.join(A.DATA, sub))

    for name, fn in A.TEXTURES.items():
        out = fn()
        rgb, alpha = (out if isinstance(out, tuple) else (out, None))
        path = os.path.join(A.ASSETS, "textures", "block", name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        tex.save(rgb, path, alpha)
    for name, fn in A.ITEM_TEXTURES.items():
        out = fn()
        rgb, alpha = (out if isinstance(out, tuple) else (out, None))
        path = os.path.join(A.ASSETS, "textures", "item", name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        tex.save(rgb, path, alpha)

    for name, fn in A.ENTITY_TEXTURES.items():
        out = fn()
        rgb, alpha = (out if isinstance(out, tuple) else (out, None))
        path = os.path.join(A.ASSETS, "textures", "entity", name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        tex.save(rgb, path, alpha)
    import gen_models
    gen_models.write()

    for name, m in A.BLOCK_MODELS.items():
        dump(os.path.join(A.ASSETS, "models", "block", name + ".json"), m)
    for name, m in A.ITEM_MODELS.items():
        dump(os.path.join(A.ASSETS, "models", "item", name + ".json"), m)
    for name in A.ITEM_TEXTURES:
        p = os.path.join(A.ASSETS, "models", "item", name + ".json")
        if not os.path.exists(p):
            dump(p, {"parent": "minecraft:item/generated", "textures": {"layer0": "noclip:item/" + name}})
    for name, s in A.BLOCKSTATES.items():
        dump(os.path.join(A.ASSETS, "blockstates", name + ".json"), s)
    for name, l in A.LOOT.items():
        dump(os.path.join(A.DATA, "loot_table", "blocks", name + ".json"), l)
    dump(os.path.join(A.RES, "noclip", "blocks.json"), A.BLOCKS)
    for name, a in A.ADVANCEMENTS.items():
        dump(os.path.join(A.DATA, "advancement", name + ".json"), a)
    for name, r in A.RECIPES.items():
        dump(os.path.join(A.DATA, "recipe", name + ".json"), r)

    # lang: generated keys merged over the hand-written base file
    base_lang = os.path.join(os.path.dirname(__file__), "lang_base.json")
    lang = json.load(open(base_lang)) if os.path.exists(base_lang) else {}
    lang.update(A.LANG)
    dump(os.path.join(A.ASSETS, "lang", "en_us.json"), dict(sorted(lang.items())))

    for lid, L in A.LEVELS.items():
        write_level(lid, L)

    write_sounds(force_sounds, only)
    import gen_index
    nl, ne, nr = gen_index.run()
    print(f"indexed {nl} levels, {ne} extra entities, {nr} extra renderers")
    print(f"{len(A.BLOCKS)} blocks, {len(A.TEXTURES)} textures, {len(A.ITEM_TEXTURES)} item textures, "
          f"{len(A.LEVELS)} levels, {len(A.SOUNDS)} sound events")


def color(c):
    return int(c.lstrip("#"), 16)


def write_level(lid, L):
    dt = {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0,
        "has_skylight": L["skylight"], "has_ceiling": L["ceiling"],
        "ambient_light": L["ambient_light"],
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "piglin_safe": True, "bed_works": False, "respawn_anchor_works": False, "has_raids": False,
        "logical_height": L["height"], "min_y": 0, "height": L["height"],
        "infiniburn": "#minecraft:infiniburn_overworld", "effects": L["effects"],
    }
    if L["fixed_time"] is not None:
        dt["fixed_time"] = L["fixed_time"]
    dump(os.path.join(A.DATA, "dimension_type", lid + ".json"), dt)
    dump(os.path.join(A.DATA, "dimension", lid + ".json"), {
        "type": "noclip:" + lid,
        "generator": {"type": "noclip:backrooms", "level": lid,
                      "biome_source": {"type": "minecraft:fixed", "biome": "noclip:" + lid}},
    })
    effects = {"fog_color": color(L["fog"]), "sky_color": color(L["sky"]),
               "water_color": color(L["water"]), "water_fog_color": color(L["water_fog"])}
    if L["ambient"]:
        effects["ambient_sound"] = "noclip:" + L["ambient"]
    if L["additions"]:
        effects["additions_sound"] = {"sound": "noclip:" + L["additions"], "tick_chance": L["additions_chance"]}
    if L["mood"]:
        effects["mood_sound"] = {"sound": "noclip:" + L["mood"], "tick_delay": 6000, "block_search_extent": 8,
                                 "offset": 2.0}
    if L["particle"]:
        effects["particle"] = {"options": {"type": L["particle"]}, "probability": L["particle_chance"]}
    dump(os.path.join(A.DATA, "worldgen", "biome", lid + ".json"), {
        "has_precipitation": False, "temperature": 0.6, "downfall": 0.0, "effects": effects,
        "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": [],
    })


def write_sounds(force, only):
    sj = {}
    for event, S in A.SOUNDS.items():
        entries = []
        for fname, fn in S["files"]:
            path = os.path.join(A.ASSETS, "sounds", fname + ".ogg")
            regen = not os.path.exists(path) or (force and (only is None or only in event))
            if regen:
                out = fn()
                stereo = isinstance(out, tuple) and out[1] == "stereo"
                x = out[0] if isinstance(out, tuple) else out
                synth.write(path, x, stereo=stereo)
            e = {"name": "noclip:" + fname}
            if S["stream"]:
                e["stream"] = True
            if S["volume"] != 1.0:
                e["volume"] = S["volume"]
            entries.append(e)
        d = {"sounds": entries}
        if S["subtitle"]:
            d["subtitle"] = "subtitles.noclip." + event
        sj[event] = d
    dump(os.path.join(A.ASSETS, "sounds.json"), sj)


if __name__ == "__main__":
    main()
