# NOCLIP: The Backrooms

A Backrooms mod for **Minecraft Java 1.21.1 (Fabric)** with **100 levels**. Clip out of reality into the Backrooms:
endless, generated levels, each with its own look, sound, rules and inhabitants, filmed through
the lens of a 1996 camcorder.

> If you're not careful and you noclip out of reality in the wrong areas, you'll end up in the
> Backrooms, where it's nothing but the stink of old moist carpet, the madness of mono-yellow,
> the endless background noise of fluorescent lights at maximum hum-buzz, and approximately six
> hundred million square miles of randomly segmented empty rooms to be trapped in.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft **1.21.1**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) (any 1.21.1 build) in your `mods` folder.
3. Put `noclip-<version>.jar` in your `mods` folder.
4. Start Minecraft with the Fabric profile.

**Recommended:** turn **Brightness to Moody**. Dark levels remove ambient light regardless, but the
mod is designed to be played dark. Headphones help a lot: most of the horror is sound.

### Build it yourself

You need Java 21.

```bash
./gradlew build
# the mod jar is in build/libs/noclip-<version>.jar
```

## Level Catalogue

Press **Backrooms Levels** on the title screen, or **K** in game, to browse all 100 levels with a photo,
survival class, description and where each level's exits lead. Search by name or number.

## Getting in

- **Suffocate inside a wall** in the real world. Each time you take suffocation damage there is a
  chance you slip through into **Level 0**.
- **Lean into a wall.** Push against any wall for long enough and it may give way. In the Backrooms
  this is how you noclip between levels: after about five seconds of pressing into a wall the
  picture starts to warp and a low tone tells you the wall feels thin. Keep pushing.
- Operators can use `/noclip go <level>`, `/noclip home`, `/noclip list` and `/noclip sanity <0-100>`.

You arrive with a **Wanderer's Journal** and a bottle of **Almond Water**. Everything else you find
or bring with you.

## Surviving

- **Sanity** drains the whole time you are in the Backrooms: faster in the dark, faster near
  entities, slower with friends nearby. The camcorder strap shows your **heart rate** (bottom left);
  as it climbs, the image starts to swim, sounds follow you, and eventually you see people who are
  not there. At zero sanity your mind starts to give out.
- **Almond Water** restores sanity. Look for bottles on the floor, water coolers and supply crates.
- **Flashlight**: right-click to switch on; it casts real light (friends see it too) and drains its
  battery. Sneak + right-click with a **Battery** in your inventory to swap batteries.
- **Camcorder**: hold right-click to look through the viewfinder (night vision).
- **Chalk**: mark walls so you know where you have been. Sneak to draw a different mark.
- **Journal**: every level you have survived, and how many are left to find.
- Each level shows its **survival class** on arrival (Class 0 safe ... Class 4 deadly).
- Exits are part of each level: doors, holes, stairwells, signs, things you have to figure out.
  Noclipping into walls always works too, but you don't choose where you land.
- Dying returns you to the real world.

### Crafting (prepare before you go)

| Item | Recipe |
|---|---|
| Flashlight | glass pane over iron-redstone-iron over iron |
| Battery (x2) | copper ingot, redstone, copper ingot (vertical) |
| Almond Water | any potion/water bottle + sugar + wheat seeds |
| Wanderer's Journal | book + charcoal |
| Chalk | calcite + bone meal |
| Camcorder | iron row / glass, redstone, battery / iron row |

## Entities (a few of them)

| Entity | What it does |
|---|---|
| Smiler | Two eyes and a grin in the dark. Creeps closer while you look away; shine a light on it too long and it charges. |
| Hound | Hunts by sound. Sprinting, jumping and fighting give you away. Crouch and stay still. |
| Faceling | People with no faces. Mostly harmless if you leave them alone. |
| Deathmoth | Moths the size of dogs, drawn to your flashlight. |
| Skin-Stealer | Wears someone else's skin. In multiplayer, it wears one of your friends. |
| Mannequin | Moves only when nobody is looking at it. |
| Partygoer | "Hello friend =)" |
| Lurker | Invisible to the naked eye, standing just behind you. Look through a camcorder. |
| Mimic | Footsteps that walk where you walked, a few seconds behind. They don't always stop when you do. |
| Shadow Figure | Someone at the end of the hall. Walk toward them and there was never anyone there. |

Many levels add their own.

## The look

Everything you see is filtered through a camcorder: barrel lens, VHS grain and tracking noise,
chromatic aberration, bloom on lights, contact shadows (SSAO), per-level colour grading and fog,
and a REC overlay with timestamp. Dark levels remove all ambient light so only real light sources
(and your flashlight) show anything. This is built into the mod and needs no shader pack or other mods.
It has not been tested together with Iris/OptiFine shader packs; if something looks broken with
a pack enabled, turn the pack off.

## Multiplayer

Works on dedicated servers (install the mod and Fabric API on both server and clients). Each
player noclips on their own; you can end up in different levels. Sanity drains slower when you
stay together.

## For developers

- `tools/build.py` generates every texture, model, blockstate, sound, dimension and biome from
  Python sources in `tools/content/`. Run it after changing any of them.
- `tools/autotest.sh <levels>` boots the real game headlessly and screenshots each level.
- `LEVEL_GUIDE.md` explains how levels are built and the quality bar each one is held to.

## Credits

Inspired by the original Backrooms image (4chan /x/, 2019) and the community-written Backrooms
wikis, whose level and entity concepts are licensed CC BY-SA 3.0. Level and entity names from those
wikis (Level 0, Level 1, Smilers, Hounds, Facelings, Deathmoths, Partygoers, Skin-Stealers, the
Poolrooms, Level Fun, Level !, and others) are used under that license, and this mod's adaptations
of them are shared under CC BY-SA 4.0. All textures, models and sounds in this mod were generated
procedurally from code in `tools/`.

## Gamerules

| Rule | Default | What it does |
|---|---|---|
| `noclipFromSuffocation` | true | Suffocating in a wall in the real world can drop you into Level 0 |
| `noclipFromWalls` | true | Pushing into walls long enough can make them give way |
| `backroomsSanity` | true | Sanity drains in the Backrooms (turn off for a calmer game) |
