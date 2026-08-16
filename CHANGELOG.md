### far future plans

- bugs (like ladybugs, beetles, butterflies)
- more birds
  - seagull, sparrow, hawk, kingfisher, owl, ...
  - for 1.0 update I want every vanilla biome to have at least one specific bird (desert, snowy, mountains, ...)
- random events (flying in circles above the player for example)
- server-side support (least likely to happen lol)
- advancements if i can figure out how to do them client-side only
- neoforge support (might rely just on synitra connector)
- quilt support (this won't happen ever, but I want to keep it in mind just in case)
- sounds (idk where to get them from, might just not add them, will figure it out eventually)

### quick todo

**v0.6.0 (The "Interactions" Update)**
- some kind of interaction with spyglass
- arrow, tnt, and other projectile interaction
- sneaking halves the scare distance
- dying state with a new texture
- option for birds to hop small distance on ground when perched (varied by distance and amount depending on bird type)
- rework Crow texture, making it unified with the new texture style
- update banner
- rework all of showcase images

**v0.5.1**
- update wiki with images
- [add biome configuration](https://github.com/RealNmS/Atmospheric-Fauna/issues/4)

## current update notes *(wip v0.5.1)*

- Added 2x chance multiplier to perch on weirdly shaped blocks (like fences, walls, levers, etc.)
- Added 4x chance multiplier to perch when it's bad weather (rain, snow, thunder)
- Added config options to set spawning pack sizes and max flock size for each bird type
  - by @gjacka in https://github.com/RealNmS/Atmospheric-Fauna/pull/5
- Improved stuck detection when landing
- Improved group takeoff logic to include other birds that are either landing or already flying
- Improved debugging texts in console and logs
- Removed creative mode check for "spawn" command
- Removed useless JSpecify annotations dependency
- Fixed birds being able to perch onto a block below another block
- Fixed stuck detection when landing
- Fixed Northern Cardinal config toggles not working properly (ty @gjacka)
- Optimized backend logic when spawning on chunk load

---

## v0.5.0 [1.21 - 26.2] `13.08.2026`

It seems I am unable to release small updates. It's still not as big as the last one, but is still packed with features and a lot of fixes. Once again, if you find any bugs or have any suggestions, please open an issue on GitHub.

- Added new bird "Northern Cardinal"
- Added new bird "Common Swift"
- Updated Blue Jay textures
- Added new spawning system "Midair Border Spawning" (great for skyblock or similar)
  - Spawns birds in the air at the edge of render distance, making them fly into the world
  - Can be turned on in the config screen, but is off by default
- Added new subcommand "spawn", which spawns a bird of a specific type at the player's location
- Added config options to disable biome and block checks for spawning birds
- Added config option to enable debug screen on joining a world
- Added config option to disable spawning around players in Spectator mode
- Added "Client" badge to the mod in Mod Menu
- Reworked perching
  - Improved the logic for selecting perching spots, making it more sparse and randomized
  - Added ability to perch on top of fences, walls, leaves, and other blocks with collision
  - Added ability to perch inside of blocks without collision (like flowers, grass, etc.)
  - Added gradualy diminishing chance to perch on the same block as other birds based on the amount
- Changed landing to be more smooth and dynamic
- Changed pecking animation to be more realistic and less random
- Changed Chunk Load Spawning to spawn only half of max birds per species, instead of the full amount
- Changed fly speed to be slightly randomized for each flock on spawn
- Changed flying above void, making birds less prone to flying straight down
- Changed sprite flipping to be simpler, removing separate flipped sprites
- Changed most of the config option texts to be more clear and understandable
- Fixed birds clipping through walls when landing
- Fixed birds flocking with removed birds when particle limit was reached
- Fixed a bug making birds fly infinitely to the sky (for a second time)
- Fixed birds not facing the right direction when perched
- Fixed birds being able to perch under water
- Fixed birds flying into water (and other fluids)
- Fixed birds not being cleared from memory properly when changing dimensions
- Optimized clearBirds command to remove all birds at once instead of one by one
- Optimized ground scanning to be less frequent (every 10 ticks instead of every tick)
- and a lot of other small optimizations and fixes

## v0.4.0 [1.21 - 26.2] `25.07.2026`

Hello once again. This is a big update, huge even. I've been working basically nonstop for a week and this is the result. I made a lot of changes to the backend, so I hope everything is working and there are no bugs or crashes. I also figured out the multiversion thing, which is cool. If you find any issues, want a new feature, or just want something to get improved, please open an issue on GitHub. I will read it all and try to implement it if I can or if it's a good idea. I still have lots of plans for this mod, but I can assure you this was probably the biggest update it will receive for a while.

**Changes:**
- Reworked project to use Stonecutter addon for multiversion support
  - Added support for Minecraft 1.21 - 1.21.11
- Created GitHub Wiki for documentation on spawn data
- Changed some of the spawning conditions
  - Blue Jays will now spawn only in biomes with tags: IS_FOREST, IS_TAIGA, IS_JUNGLE
  - Crows can now spawn on Terracotta
- Added ocean avoidance for Blue Jays
- Reworked block avoidance, making birds bounce off walls and ceilings instead of clipping through them
- Reworked flight height handling, making it more varied and smooth
- Made Blue Jays fly lower to the ground than crows
- Changed flock flight behaviour to navigate more smoothly
- Added small random delay before taking off normally
- Added small delay before scared take off based on player distance from each bird
- Updated sprite handling *(this is like the herobrine being removed every update)*
- Changed base command to be prettier, with a link to the wiki
- Added new subcommand "help" to see links to the wiki and issues
- Added config option to disable spawning for each bird type
- Removed unused mixins, making the mod compatible with Sinytra Connector
- Refactored most of the backend for easier management and better performance tracking
- Fixed max flock size not working properly, adding cooldown for joining a flock
- Fixed a bug making birds fly infinitely to the sky (hopefully)
- Fixed perching cooldown not having any effect, also making it longer
- Fixed birds being able to perch onto a block above them
- Fixed taking off animation making birds clip through walls
- Fixed birds clipping into the ground when landing
- Optimized a bunch of things, making the mod faster than ever
  - Added grid partitioning to the flocking logic
  - Caching flock neighbours to avoid recalculating them every tick
  - Ground sampling is using heightmap instead of raycasting
  - Optimized perching logic and scanning
  - Optimized lookup of the same species in the flocking logic
  - Small optimization when trying to spawn

---

## v0.3.0 [26.1 - 26.2] `17.07.2026`

Hello again, I'm sorry for the big pause on updates. I didn't have a lot of motivation or time to continue working on this, but I managed to make yet another banger update. With the new additions I figured multiversion handling will be a lot harder than I thought, and for this reason I won't be updating below the version 26.1 for now. I will look into Stonecutter gradle addon so I can hopefully manage it better, but no promises.

**Changes:**
- Added a new bird **Blue Jay**
- Reworked crow textures
- Updated flocking behaviour and added max flock size (crows are unlimited)
- Added commands, for now mainly just for debugging
- Fixed multiple texts in debugging and config screen
- Updated the sprite handling (once again, this is the final time, I promise)
- Changed some of the backend logic to support more bird additions in future
- Fixed daylight spawning

This update might still be buggy, I hope I fixed everything and made it polished enough, if anything comes up, please flood my Issues section on GitHub. I will read it all, even if it's dumb stuff.

---

## v0.2.3 [1.21.11] `15.04.2026`

> Sorry for the late update, I could not decide if I want to keep updating the old version. However, since I made it easy for myself I can update it for a while. I will keep updating it until there are either too many merge conflicts or it's way too hard to update. For now there are no plans to support older versions since it would require me to rewrite a lot of code, but maybe in the future...

**Changes:**
- Changed the way sprites are facing the camera

---

## v0.2.3 [26.1] `29.03.2026`

**Changes:**
- Ported to Minecraft 26.1
- Changed the way sprites are facing the camera

---

## v0.2.2 [1.21.11] `05.02.2026`

**Changes:**
- Added "Spawning on Chunk Load" and "Ambient Spawning" config options
- Added minimum values to the config options (to prevent user inputting negative values)
- Changed some of the flocking behavior
- Changed crow take off to be more smooth
- Lowered crow wing flapping speed when rising
- Optimized a bunch of small things
- Fixed crow spawning when crow limit number in config was really low
- Fixed crow sprite not updating when going fast upwards
- Fixed possible crashes
- Fixed small memory leak

---

## v0.2.1 [1.21.11] `18.01.2026`

**Changes:**
- Added "Spawn Below Sea Level" config option
- Optimized the flocking data management
- Fixed several spawning issues
- Fixed some of the spawning condition checks
- Fixed possible memory leaks
- Changed the log messages to be unified with the rest of the project

---

## v0.2.0 [1.21.11] `16.01.2026`

At first I thought I would release a few of small updates, gradually fixing what was broken. However since Modrinth took forever to review this mod (not blaming them though, I hope they enjoyed their break 🥰) I didn't feel like releasing the changes and they somehow built up into this massive release. Honestly it feels like a completely different mod at this point, but it's still early in development. There are still quite a lot of things I would like to fix before I start adding more bird variants.

**Changes:**
- Completely refactored and optimized the backend, now supporting easy addition of new birds
- Fixed the crow spawning:
  - Crows spawn like they should (just set the Minecraft var alwaysSpawn to true lol)
  - Removed the custom height check -> Now only able to spawn above sea level
  - Rewrote the spawn location calculation to be further away (closer to config value)
  - Optimized the crow spawning if the max limit is reached
  - Added spawning attempt after loading every 4th chunk (to populate the world when it loads)
  - Added a list of spawnable blocks (to prevent spawning on things like water...)
- Added a despawn distance (render distance + 16 blocks)
- Added a random wing flapping offset to each crow
- Rewrote perching logic:
  - Crows now should land more sparsely from each other when in group
  - Less inclined to perch after spawning
  - Fixed some bugs with group perching
  - Added longer perch cooldown based on how long the crows were perched
- Added a check that adds any missing values inside the config file
- Added max bird particle count config value
- Replaced the missing Cloth Config disabled button with centered label
- Changed some of the config tooltip texts
- Categorized the config values inside the JSON file
- Changed some of the default config values:
  - Max number of crows: 120 -> 50
  - Spawn tick delay: 100 -> 200

---

## v0.1.0 [1.21.11] `08.01.2026`

**Changes:**
- Updated the wing flapping speed to be based on the vertical speed of the crow
- Added crow debug text option to the config
- Turned the mod to be completely  client-side only (will figure out the server-side later on)
- Reduced final jar file size (by not including the /docs folder)
- Updated the mod icon

---

## v0.1.0 (alpha) [1.21.11] `06.01.2026`

The first release, yippiee!!

**Important info:**
- This is the alpha release, most things will be bugged or just not working at all
    - mostly the spawning and crow behaviour is not finished (but somewhat working)
- Only works for Fabric loader and Minecraft 1.21.11
