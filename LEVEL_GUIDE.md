# NOCLIP level authoring guide

This is how every level in the mod is built. Read it fully before writing a level.
Look at the three reference levels first; they show every pattern you need:

| Level | Java | Assets (Python) | Shows |
|---|---|---|---|
| Level 0, The Lobby | `src/main/java/com/noclip/levels/Level0.java` | `tools/content/level_0.py` | irregular room grid, set pieces, a behaviour that changes the world when unseen |
| Level 1, Habitable Zone | `levels/Level1.java` | `tools/content/level_1.py` | `CellGrid`, props, loot crates, stairwell exit, level-wide blackouts |
| Level 6, Lights Out | `levels/Level6.java` | `tools/content/level_6.py` | true darkness (`darkness(1f)`), maze, a glowing exit |

## The quality bar (this is the whole point)

The person this mod is for asked for "the highest quality Minecraft has ever seen" and
said quality matters more than quantity. A level fails if any of these is true:

1. **Recolour.** It is another level's layout with different blocks.
2. **No rule.** It has no mechanic of its own that changes how you play. Every level needs
   at least one: lights that die, rooms that move when unseen, a timer, rising water, a
   sound you must avoid, a door that only opens when..., gravity, a puzzle exit...
3. **Not unsettling.** Nothing builds tension. There is no "something is wrong here".
4. **Flat.** One room repeated forever. Good levels are mostly uniform with rare things that
   are *off*: one lit window, one door in the wrong place, one room too clean. Use rare set
   pieces (0.5%-5% of cells) generously.
5. **No sound of its own.** Every level gets its own ambient loop and 2+ one-shot additions.
6. **Undesigned exit.** Exits should be found, figured out or earned (doors, holes, stairs,
   signs, puzzles), not only random noclipping.
7. **Wrong entity.** Danger should come from the place. Reuse a core entity only when it fits
   (Deathmoths near light, Smilers in the dark), or make a new one for the level.
8. **Broken.** Floating blocks, no floor, spawn inside walls, unescapable pockets, lag.

The test: would someone remember this level and describe it to a friend?
"The one where every door is room 6" passes. "The grey hallway one" fails.

Make the textures good. 32x32, seamless, with grime, wear, stains and variation blocks.
Make it read as a real place, not Minecraft. Use `flat=True` for ceilings and glowing panels
(Minecraft darkens downward faces to 50% otherwise).

## Files for one level

1. `tools/content/<id>.py` - textures, blocks, sounds, dimension/biome data.
2. `src/main/java/com/noclip/levels/<ClassName>.java` - a class with
   `public static BackroomsLevel create()`. It is picked up automatically (`tools/gen_index.py`).
3. Optional: new entities, see "Entities".

Never edit another level's files. Framework files (`registry/`, `world/`, `level/`, `Noclip.java`,
client code, `tools/assets.py`, `tools/build.py`) are shared: do not edit them unless you truly
must, and if you do, keep the change small, additive and mention it in your report.

## Names must be unique

Every block, texture, sound event and level id lives in one global namespace and the build
fails on duplicates. Use names specific to your level: `hotel_carpet`, not `carpet`.
Texture names: prefix with a short level key (`l5_carpet`, `mall_tile`). Sound events:
`ambient.<id>`, `ambient.<id>.additions`, `entity.<name>.<sound>`, `event.<id>_<thing>`.
Before inventing a block, check whether a shared one already fits:

Shared blocks: `void_filler` (black, put above ceilings), `subfloor` (dark concrete, fill below
floors), `dark_doorway` (DOOR exit, pitch black, walk into it), `bottomless_pit` (FALL exit),
`almond_water_bottle` (pickup), `supply_crate` (random loot pickup), `wooden_crate`,
`cardboard_boxes`, `metal_shelf`, `chain_link_fence`, `puddle`, `office_chair`,
`strip_light`, `fluorescent_light`, `flickering_fluorescent_light`, `dead_fluorescent_light`,
`concrete_wall`, `warehouse_concrete`, `cinder_block`, `exit_sign`, Level 0 wallpaper family.

## Python: assets (`tools/content/<id>.py`)

```python
import numpy as np
from assets import cube, cube_faces, pillar, flicker_light, shaped, pane, fence, slab, stairs, carpet, \
    cross, exit_block, hazard, door, trapdoor, ladder, custom, tex, level, sound, lang
import tex as T          # texture toolkit: noise, fbm, ramp, stains, cracks, border, rect, grain...
import synth as Y        # audio toolkit: hum, drone, pink/brown/white noise, footstep, impact, reverb...
```

Block helpers (all take `display=`, `sound=` one of wool/stone/metal/glass/wood/gravel/sand/grass/
tile/mud/snow/copper/chain/moss, `unbreakable=` default True for structure, `light=`, `emissive=`):

- `cube(name, texture, flat=False)` / `cube_faces(name, top=, bottom=, side=, flat=False)`
- `pillar(name, end, side)` - rotates like logs
- `flicker_light(name, on_tex, off_tex, side_tex, light=15, flicker=0.25, face="down"|"up"|"all")`
- `shaped(name, boxes, textures, collides=True, light=, emissive=, kind="shaped")` - furniture and props
  from boxes in 1/16 units, authored facing north. Each box is `(x1,y1,z1,x2,y2,z2, tex_key)` or a
  per-face dict `{"all": key, "down": key2}` (set a face to None to drop it). Becomes rotatable.
  `kind="pickup", item="noclip:almond_water*3,noclip:battery*1"` makes it a loot pickup.
- `pane` (thin connecting walls, fences, bars), `fence`, `slab`, `stairs(name, tex, base_block)`,
  `carpet`, `cross` (X sprite: plants, cobwebs, hanging wires), `door(name, top_tex, bottom_tex)`,
  `trapdoor`, `ladder`.
- `exit_block(name, texture, "DOOR"|"FALL"|"DESCEND"|"ASCEND"|"WATER"|"SECRET", trigger="INSIDE"|"USE",
  solid=False)` - walking into (INSIDE) or right-clicking (USE) it leaves the level.
- `hazard(name, texture, damage=2.0, solid=True)` - hurts on touch (live wires, scalding pipes).
- `custom(name, "com.noclip.block.YourBlock", model_json, blockstate=..., ...)` - your own Java block
  class with a `(BlockBehaviour.Properties)` constructor.

Textures are functions returning a float RGB array `(32,32,3)` in [0,1] or `(rgb, alpha)`:
`tex("l5_carpet", lambda: T.grain(T.ramp(T.fbm("l5c"), "#5a1a1a", "#7a2a2a"), "l5c", 0.05))`.
Seeds are strings; use unique ones. Preview: `python3 tools/preview.py "l5_*" /tmp/x.png` then Read it.

Level data (dimension + biome): `level("<id>", fog="#hex", sky="#hex", water=, water_fog=,
ambient="ambient.<id>", additions="ambient.<id>.additions", additions_chance=0.004,
fixed_time=6000|18000|None, skylight=False, ceiling=True, height=64..384, particle="minecraft:ash",
particle_chance=0.01)`. Height is the world height from y=0; keep it as small as the level needs.
Outdoor levels: `skylight=True, ceiling=False`, and `.sky()` on the Java Visuals to draw a sky.

Sounds: `sound("ambient.<id>", [("ambient/<id>", fn)], stream=True)` for the loop (return
`(stereo_array, "stereo")`, use `Y.loopable` so it loops seamlessly), and a list of one-shots for
`ambient.<id>.additions` (mono). Entity sounds must be mono. Keep loops 16-30 s.
Look at `level_0.py`, `level_1.py`, `level_6.py`, `entity_sounds.py` for synthesis recipes.

## Java: the level (`levels/<ClassName>.java`)

```java
public static BackroomsLevel create() {
    return BackroomsLevel.builder("level_5")
        .number("5").title("Terror Hotel").order(50)   // number "" for named (non-wiki) levels
        .tagline("One line that sets the mood.")
        .danger(BackroomsLevel.Danger.MODERATE)        // SAFE LOW MODERATE HIGH DEADLY UNKNOWN
        .generator(Gen::new)
        .behavior(new MyRule())                        // optional LevelBehavior
        .visuals(new Visuals().tint(r,g,b).saturation(..).contrast(..).brightness(..).grain(..)
                 .aberration(..).vignette(..).warp(..).scanlines(..).flicker(..).bloom(..)
                 .fog(start, end).fogColor(0xRRGGBB).darkness(0..1).sky().clouds().noRec())
        .sanityDrain(0.03f)                            // per second; 0.02 calm .. 0.12 brutal
        .spawnY(FLOOR + 1, FLOOR + 1)                  // where players may be placed on arrival
        .spawn(SpawnRule.of(ModEntities.lazy("hound"), maxNearby, chancePerAttempt).dark().dist(min,max).weight(w))
        .exit(ExitType.NOCLIP, "level_6", 3).exit(ExitType.DOOR, "level_7", 1)
        .build();
}
```

- Generator: implement `LevelGenerator.generate(ChunkWriter w)`. Use world coordinates;
  `w.set/fill/layer/walls/get` clip to the chunk. Must be deterministic from `w.seed` and coordinates
  (use `Hash`, `CellGrid`), never from `java.util.Random` without a seed derived from position.
  Fill below the floor with `subfloor` and above the ceiling with `void_filler`.
  Features spanning chunk borders: compute them per cell with `CellGrid.forCells` - writes outside
  the chunk are dropped and the neighbouring chunk draws its part.
- Blocks: `ModBlocks.state("name")`; facing via `.setValue(HorizontalDirectionalBlock.FACING, Direction.X)`.
- `darkness(1f)` removes all ambient light (only light sources and flashlights show anything).
  Use it for genuinely dark levels; otherwise light the level with emissive light blocks.
- Behaviour hooks: `tickPlayer`, `onEnter`, `tickLevel`. Useful tools: `Blackouts.start(level, ticks)`,
  `Sanity.add(player, amount)`, `Travel.sendTo(player, "level_x")`, `Travel.exit(player, ExitType, pos)`,
  `ServerPlayNetworking.send(player, new NoclipNet.EventPayload("flash"|"pulse"|"blackout", strength))`,
  `level.playSound(null, pos, ModSounds.get("event"), SoundSource.AMBIENT, vol, pitch)`.
  Only change blocks out of the player's sight (see `Level0.Shifting.seen`).
- Exits reference level ids from the master list below. Exits to levels not built yet are skipped
  automatically, so link generously and sensibly (wiki-accurate where the wiki says where exits go).
  `"overworld"` returns the player to the real world (rare; the deep end only).

## Entities

Core entities (`ModEntities.lazy(name)`): `hound` (hunts by sound), `smiler` (dark only, charges if lit
too long), `faceling` (passive, faceless people), `mannequin` (moves only when unseen), `shadow_figure`
(stands far away, vanishes when approached; use `.seen().dist(28,44)`), `skin_stealer` (wears a
player's skin), `partygoer` (Level Fun), `deathmoth` (flies to flashlights).

A new entity for your level:
1. Model + texture in Python with `entity_model(name, parts, tex_size)` and `entity_tex(name, fn)`
   (see `tools/content/entities_core.py`; part names `head`, `body`, `right_arm`, `left_arm`,
   `right_leg`, `left_leg`, `leg_fl/fr/bl/br`, `wing_l/r`, `jaw` animate automatically). Add
   `lang("entity.noclip.<name>", ...)`, the spawn egg lang and `ITEM_MODELS[f"{name}_spawn_egg"] =
   {"parent": "minecraft:item/template_spawn_egg"}`.
2. Java class in `src/main/java/com/noclip/entity/` extending `BackroomsMonster` (or `PathfinderMob`
   implementing `Sanity.Frightening`), with `createAttributes()` and
   `public static EntityType<X> TYPE; public static void register() { TYPE = ModEntities.register("name",
   X::new, MobCategory.MONSTER, width, height, createAttributes(), eggColor1, eggColor2); }`.
3. Client renderer: `src/client/java/com/noclip/client/render/entities/<X>Renderers.java` with
   `public static void register() { EntityRenderers.generic(X.TYPE, "name", GenericModel.Style.HUMANOID, shadow, scale, null); }`
   Styles: HUMANOID, ZOMBIE (arms out when aggressive), STIFF, QUADRUPED, FLYER, FLOAT.
Both registration classes are discovered automatically.

## Build and test loop

```bash
python3 tools/build.py                       # regenerates assets + indexes (add --only <id> to resynth your sounds)
./gradlew compileJava compileClientJava --no-daemon -q --console=plain
tools/autotest.sh level_5,level_7            # boots the real game headlessly, screenshots each level
tools/autotest.sh level_6 flash              # same, holding a lit flashlight
NOCLIP_AUTOTEST_ENTITIES=hound,my_thing tools/autotest.sh level_5   # entities lined up in view
```

Screenshots: `run/screenshots/autotest_<id>_{card,0,1,2,3}.png` - look at them with the Read tool,
judge them honestly against the quality bar, and iterate. The game client is slow (software
rendering) and at most two run at a time across all workers, so batch several levels into one
autotest run. Errors show in the autotest output and in `run/autotest.log`.

Commit your work in your branch whenever a level is done (`git add -A && git commit`). Do not push.

## Master level list (ids for exits)

Wiki levels: `level_0` Lobby, `level_1` Habitable Zone, `level_2` Pipe Dreams, `level_3` Electrical
Station, `level_4` Abandoned Office, `level_5` Terror Hotel, `level_6` Lights Out, `level_7`
Thalassophobia, `level_8` Cave System, `level_9` Darkened Suburbs, `level_10` Bumper Crop, `level_11`
Endless City, `level_13` Infinite Apartments, `level_37` Poolrooms, `level_94` Motion, `level_188`
Courtyard of Windows, `level_fun` Level Fun =), `level_run` Level ! Run For Your Life, `manila_room`
The Manila Room, `airport` Airport.

Empty buildings: `dead_mall`, `waiting_room`, `school`, `hospital`, `supermarket`, `parking_garage`,
`playground`, `water_park`, `arcade`, `bowling_alley`, `movie_theater`, `laundromat`, `motel`,
`daycare`, `museum`, `archive`, `subway`, `lift_lobby`, `stairwell`, `restrooms`, `locker_rooms`,
`ballroom`, `bingo_hall`, `convention_center`, `call_center`.

Industrial and underground: `server_farm`, `storm_drains`, `boiler_room`, `cold_storage`,
`assembly_line`, `container_yard`, `bunker`, `mine_shaft`, `water_treatment`, `steel_mill`,
`elevator_shaft`, `air_vents`, `sewers`, `power_plant`, `dam`.

Outside, but wrong: `static_fields`, `the_signal`, `endless_highway`, `gas_station`, `parking_lot`,
`golden_hour`, `silent_beach`, `pole_forest`, `snow_plains`, `golf_course`, `salt_flats`,
`foggy_playground`, `the_dock`, `sky_rooms`, `overgrown_lobby`, `rain_city`, `construction_site`,
`drive_in`, `rooftops`, `carnival`.

Reality-breaking: `recursion`, `inverted_lobby`, `zero_zero`, `mirror_level`, `missing_textures`,
`the_grid`, `dollhouse`, `long_hallway`, `flooded_lobby`, `red_room`, `room_of_doors`,
`loading_screen`, `gravity_rooms`, `photo_gallery`, `glitch_level`.

The deep end: `the_hub`, `the_abyss`, `the_void`, `frontrooms`, `the_exit`.

The one-line concept for each is in `PLAN.md`. Order numbers (`.order(n)`): wiki levels use
`number * 10`; named levels use 1000+ in the order of this list (empty buildings 1000-1240, industrial
2000-2140, outside 3000-3190, reality 4000-4140, deep end 9000-9040).
