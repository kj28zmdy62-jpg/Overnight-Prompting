# NOCLIP — Backrooms Mod Plan

Fabric mod for Minecraft 1.21.1. Quality over quantity: every level must feel unsettling or make you excited for the next. No reskins, no slop. Stop adding levels when quality would drop.

## Build environment needs (network allowlist)
maven.fabricmc.net, meta.fabricmc.net, libraries.minecraft.net, piston-meta.mojang.com,
piston-data.mojang.com, launchermeta.mojang.com, launcher.mojang.com,
resources.download.minecraft.net, plugins.gradle.org, repo.maven.apache.org

## Visuals (make it not look like Minecraft)
- Built-in post-processing per level: VHS grain, chromatic aberration, lens warp, vignette, fluorescent tint, bloom, timestamp overlay. Each level gets its own profile.
- Custom non-cube models (thin walls, ceiling tiles, pipes, trim, rounded pool edges).
- High-detail grimy textures.
- Camera: head bob, sway, flashlight cone, dense fog, narrower FOV.
- Optional Iris shader pack (reflections, volumetrics, DOF).
- Sound is half the horror: hum, reverb, floor-specific footsteps, footsteps that aren't yours.

## Core systems
- Noclipping between levels (rare random clips + level-specific exits), discovered-level map.
- Sanity: drains in darkness/isolation/near entities; low sanity = hallucinations. Restored by Almond Water.
- Items: flashlight + batteries, Almond Water, camcorder (reveals hidden things), level map, wanderer's journal, crowbar, chalk, compass that fails in some levels.
- Optional no-HUD mode. Multiplayer-aware (noclipping is per player).
- Each level = its own dimension with custom infinite generation.
- Data-driven level system: layout + palette + lighting + filter + soundscape + rules + entities + exits.

## Entities
Smilers, Hounds, Partygoers, Skin-Stealers, Facelings, Deathmoths, plus originals:
The Mimic (repeats your sounds), The Lift Attendant. More matched to levels as built.

## Build order
1. Project setup, level framework, post-processing pipeline
2. Level 0 fully finished (sound, noclip, Almond Water)
3. Sanity, flashlight, camcorder
4. Wiki classics with their entities
5. Remaining levels in groups, committing and pushing after every batch
6. Optional Iris shader pack, README + install/play guide, CREDITS (wiki content is CC BY-SA)

## Levels (target ~100)

### 1. Wiki levels (verify details against the wiki before building)
1. Level 0 – The Lobby: endless yellow rooms, damp carpet, constant hum
2. Level 1 – Habitable Zone: foggy concrete warehouse, crates, flickering lights
3. Level 2 – Pipe Dreams: hot narrow tunnels, steam leaks
4. Level 3 – Electrical Station: loud machinery maze, live wires
5. Level 4 – Abandoned Office: endless cubicles, Almond Water coolers
6. Level 5 – Terror Hotel: fancy old halls, boiler room below
7. Level 6 – Lights Out: total darkness, flashlight only
8. Level 7 – Thalassophobia: room opening into an endless black ocean
9. Level 8 – Cave System: huge dark caves, crawling things
10. Level 9 – Darkened Suburbs: the same house over and over at 2 a.m.
11. Level 10 – Bumper Crop: endless wheat under a gray sky
12. Level 11 – The Endless City: empty city, safe hub to rest and trade
13. Level 13 – Infinite Apartments: apartment halls and units forever
14. Level 37 – The Poolrooms: white tiles, warm water, beautiful and wrong
15. Level 94 – Motion: rolling hills, everything moves like stop-motion
16. Level 188 – Courtyard of Windows: courtyard ringed by always-lit windows
17. Level Fun =): endless birthday party, Partygoers
18. Level ! – Run For Your Life: red-lit hallway, something chasing you
19. The Manila Room: small, safe, quiet room
20. Airport: empty terminals, flights that never board

### 2. Empty buildings
21. Dead Mall – 90s mall, slowed muzak, mannequins move when unseen
22. The Waiting Room – take a number; when called, a door opens
23. School After Hours – random bells; chalkboards give escape hints
24. Hospital Ward – curtained beds, monitors beep with your sanity
25. Night Supermarket – endless aisles, intercom says your name
26. Parking Garage – concrete, one car alarm somewhere
27. Indoor Playground – tubes, ball pits, laughter, no kids
28. Closed Water Park – slides into darkness
29. Dead Arcade – machines turn on as you pass
30. Bowling Alley – pins reset by themselves
31. Movie Theater – the screen shows you in the seats
32. Laundromat – machines spinning with nothing in them
33. Roadside Motel – every door is room 6
34. Daycare – bright colors; toys aren't where you left them
35. Museum of You – exhibits about your playthrough
36. The Archive – endless tapes, some showing what you did
37. Subway Station – the train passes and never stops
38. The Lift Lobby – the Lift Attendant asks which floor
39. The Stairwell – going up takes you down
40. Endless Restrooms – stalls forever, one always locked
41. Locker Rooms – showers running, nobody there
42. Hotel Ballroom – wedding reception, no guests
43. Bingo Hall – numbers called, no players
44. Convention Center – empty booths, badges with your name
45. Call Center – phones ringing; answer and hear yourself

### 3. Industrial and underground
46. Server Farm – cold, blinking lights, deafening fans
47. Storm Drains – rising water, echoes
48. Boiler Room – heat damage, pressure valves
49. Cold Storage – freezing damage, plastic curtains
50. Assembly Line – factory making office chairs forever
51. Container Yard – shipping container maze
52. The Bunker – military halls, blast doors
53. Mine Shaft – mine carts, lanterns going out
54. Water Treatment Plant – huge tanks, catwalks
55. Steel Mill – molten glow, heavy machinery
56. Elevator Shaft – climb forever
57. Air Vents – crawl-only tight spaces
58. Sewers – dripping, dark, things in the water
59. Power Plant – control room, unanswered alarms
60. Dam Interior – giant concrete chambers, roaring water

### 4. Outside, but wrong
61. Static Fields – grass, gray sky, one TV playing static
62. The Signal – radio tower in a storm; tune to find the exit
63. Endless Highway – night road, no cars, mile markers counting down
64. Repeating Gas Station – walk away, arrive back
65. Endless Parking Lot – empty spaces under orange lights
66. Golden Hour Suburbs – permanent sunset, perfectly still
67. Silent Beach – waves with no sound
68. Telephone Pole Forest – poles instead of trees, humming wires
69. Snow Plains – streetlights in a blizzard, no road
70. Golf Course at Dusk – ticking sprinklers, a flag in every hole
71. Salt Flats – white ground forever, one standing door
72. Foggy Playground – swings moving with no wind
73. The Dock – a lake dock repeating into fog
74. Sky Rooms – rooms floating above clouds
75. Overgrown Lobby – Level 0 reclaimed by plants
76. Rain City – always-raining neon streets, nobody around
77. Construction Site – unfinished building at night
78. Drive-In Theater – a film playing to empty cars
79. The Rooftops – endless connected rooftops at night
80. Closed Carnival – rides still running

### 5. Reality-breaking
81. Recursion – same room repeating; spot the change to leave
82. Inverted Lobby – Level 0 upside down
83. Level Zero-Zero – pure white, no edges or shadows
84. Mirror Level – everything mirrored, your reflection lags
85. Missing Textures – purple/black checkerboard, the game falling apart
86. The Grid – black void, glowing lines
87. Giant's Dollhouse – you're tiny in a huge house
88. The Long Hallway – gets longer as you walk
89. Flooded Lobby – Level 0 under knee-deep water
90. The Red Room – one deep red room, constant low tone
91. Room of Doors – hundreds of doors, almost all wrong
92. Loading Screen – a level that never finishes loading
93. Gravity Rooms – the floor changes walls
94. Photo Gallery – photos of this level, with you in them
95. Level ??? – glitched level whose name keeps changing

### 6. The deep end
96. The Hub – central hallway of doors to other levels
97. The Abyss – a fall that never ends
98. The Void – nothing, just you and the hum
99. The Frontrooms – looks like the real world, until it doesn't
100. The Exit – secret ending
