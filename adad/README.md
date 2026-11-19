# The Stalker — Fabric 1.21.1

A dark-survival horror mod prototype for Minecraft Java Edition 1.21.1. Requires Java 21, Fabric Loader, and Fabric API.

## Current source features

### The Observer
- A separate, stationary sighting entity with no conventional AI goals.
- Naturally spawns, not often, in dark Overworld locations (light level 3 or lower, mostly caves), in packs of 1-10.
- The first time a player comes within 16 blocks, it exhales a wave: an expanding ring of cloud particles that snuffs up to 5 nearby lights (torches, lanterns, candles, campfires), nearest first, each going out as the ring reaches it. Relight them by right-clicking the unlit blocks.
- Appears at normal hostile-mob spawn distances, watches the area, and dissolves when a player gets within roughly 4.5 blocks.
- Disappearance combines an expanding smoke cloud, teleport sound, and a 12-tick scale-down animation.
- The original block position, dimension, player UUID, and a random 200–400 tick delay are recorded server-side.

### The delayed ambush
- The ambush is tied to the Observer's original block position, with a 1.65-block trigger radius around its center.
- It only triggers if the player returns after the timer and their current light level is 0–2.
- Entering the trigger area at light level 3 or higher cancels the pending ambush.
- On trigger, a no-AI Stalker is spawned directly behind the player in a raised-arm grab pose, a client packet starts a brief screen-grab overlay, and the server turns the player around.
- The grabber is removed and the player is killed 10 ticks later. Server-side checks remain authoritative; this is a short model-and-overlay cinematic rather than a multi-stage animation rig.

### The Hunter
- A separate hostile entity with melee, wandering, swimming, target acquisition, and look behaviours.
- Its hitbox matches its model (tall when standing, compact only while crawling beneath a low ceiling), and its model folds into a crawl pose there.
- Tracks players, remembers last-known positions, investigates when line of sight is lost, pauses when watched at range, and periodically breaks its approach to make stalking less predictable.
- Seeks dark cover when exposed to light level 10+; light level 10+ (open daylight, or within a few blocks of a torch) causes burning, smoke, and repeated screeches. Shade and dim areas are safe for it. If it stays on fire for more than 10 seconds it retreats into smoke and disappears.
- Periodically extinguishes visible torches and converts them into relightable unlit torches.
- Has a local home-ambush jumpscare: when the player is near a chest, an unlit furnace, or a painting, the Hunter can vanish and crawl out of that nearby prop. It does not need a second container as an entrance or an exit.
- Before emerging, it snuffs nearby torches, lanterns, soul lanterns, candles, and campfires. The lights go out first; only then does the Hunter disappear and begin its emergence.
- A chest opens as the Hunter crawls out; a painting releases a burst of portal particles; an unlit furnace releases a burst of smoke as the creature unfolds from its front.
- Chest emergence is a synchronized, 72-tick, multi-beat animation: hooked fingers catch the rim, one hand slips and catches again, the head peeks through, shoulders pull the torso forward, hips and legs drag free, and the creature settles into its usual stance. The stalker is oriented toward the nearby player, with matching lid, sound, and particle cues.

### Torches
- Floor and wall torches can be extinguished by the Hunter.
- Unlit torches emit no light and can be relit with a right-click.
- Extinguishing and relighting use smoke/flame particles and sounds.

## Development status / limitations

This is a substantial source prototype, not a verified release build. The current environment does not have Gradle installed and cannot reach the Gradle distribution host, so a Gradle compile or in-game test could not be run here. In particular, the client rendering/networking callbacks and exact runtime behaviour still need validation in a real 1.21.1 Fabric dev client. The jumpscare remains a screen overlay and camera turn paired with a raised-arm entity grab pose; it is not a full player-skeleton grab animation. The local container ambush is an initial implementation: supported extinguishable lights are torches, lanterns, soul lanterns, candles, and campfires; permanent blocks such as glowstone and sea lanterns cannot be blown out. Furnaces are eligible only while unlit. The Hunter crawls when it finds itself under a low ceiling, but vanilla pathfinding plans with the standing hitbox, so it will not deliberately route into one-block-high gaps.

## Build

Install a Java 21 JDK and Gradle 8.8 or use your preferred Gradle installation. From this folder run:

```bash
gradle build
```

The output JAR, if the build succeeds, will be in `build/libs/`.

## Testing commands

```mcfunction
/summon thestalker:observer
/summon thestalker:stalker
/give @s thestalker:unlit_torch
/give @s thestalker:unlit_wall_torch
/give @s thestalker:unlit_lantern
/give @s thestalker:unlit_soul_lantern
```

The entities also have natural Overworld spawn entries (Hunter weight 25; Observer weight 3 in packs of 1-10) and light-based spawn restrictions (the Hunter spawns below light level 10).


### Local container ambushes (0.4.0)

The Hunter can vanish and emerge directly from a chest, an unlit furnace, or a painting near the player. It first extinguishes nearby torches, lanterns, candles, and campfires. This is a jumpscare/hiding-place mechanic, not container-to-container travel. Lit lanterns become unlit lantern blocks; right-click them to relight them. Soul lanterns have the same treatment. Furnaces are only eligible when their `LIT` state is false.


## v0.5.0 — Soul-sand offering

- Dropping soul sand within 2.5 blocks of a player, on a spot with block light 3 or lower, can trigger an offering. The ground answers with a rising hand-like animation made from soul wisps, soul-fire motes, and smoke; one soul sand is consumed.
- After the 28-tick offering animation, that player receives a 24,000-tick (one Minecraft day) truce. The Stalker continues to seek and follow the player more insistently, but its melee attacks and Observer-triggered delayed jumpscares are suppressed for the truce.
- The offering is player-specific. Other players are not protected by someone else's offering.

**Prototype limitation:** truce timers currently live in server memory and are cleared by a full server restart. The hand is particle-animated rather than a separately modelled hand entity. A later persistence/animation pass can make both survive restarts and render as a fully articulated hand.


## v0.5.1 — Reliability pass

- Offering protection now uses the Overworld game-time clock, so changing dimensions does not alter the 24,000-tick truce duration. (The timer is still memory-only and resets on server restart.)
- Observer target selection now explicitly chooses the closest eligible nearby player instead of relying on the order returned by the world player list.
- A delayed Observer jumpscare is cancelled if the player dies, enters Creative/Spectator, changes dimension during the grab, or disconnects.
- The delayed kill is no longer scheduled if the grabber entity fails to spawn, preventing an invisible/unexplained death.
- Chest opening/closing block events now support trapped chests as well as regular chests.

The source was reviewed and resource files were checked, but no Gradle compilation or Minecraft runtime test was possible in this environment.


## v0.5.2 — Bug-fix pass

- **Networking:** the jumpscare packet now uses the 1.20.5+ `CustomPayload` API (`JumpscarePayload`) instead of the removed buffer-based API.
- **Camera turn:** the ambush now turns the player with a teleport request so the client camera really rotates.
- **Compile fixes:** `tryAttack(Entity)` uses the 1.21.1 signature; `addSyncedBlockEvent` receives a `Block`; `UnlitTorchBlock` takes a `SimpleParticleType`.
- **Loot tables:** moved to `data/thestalker/loot_table/` (singular, required since 1.21) so unlit torches and lanterns drop again.
- **Torch snuffing:** no longer gated on the Hunter's own light level, so it actually fires near torches.
- **Targeting:** the Hunter now loses track of a player based on *its* line of sight to them, not theirs to it.
- **Hitbox:** pose-aware dimensions with matching eye height; fixes the 0.92-block hitbox on a ~2-block model.
- **Painting ambush:** removed the unsupported painting invisibility; the portal particles carry the effect.
- **Persistence safety:** a jumpscare grabber saved mid-scare is removed on reload, and a Hunter can no longer stay invisible after a reload mid-travel.
- **Offering:** the light check now uses block light, as documented; a half-finished offering animation is dropped on disconnect.

Still unverified: no Gradle compile or in-game test was possible. Truce timers are still memory-only.


## v0.5.3

- **Burn threshold:** the Hunter now only burns at light level 10 or higher (`StalkerEntity.BURN_LIGHT_LEVEL`). The same value controls where it may spawn and when it runs for cover. The Observer is unchanged (still burns at 4+).
- **Frequency:** natural Hunter spawn weight raised from 2 to 25, and a new `StalkerDirector` sends a Hunter after every survival player in the Overworld from the first minute. Every 10 seconds, a player with no Hunter within 64 blocks has a 50% chance of one appearing 22-38 blocks away, out of sight and below light level 10, already heading for them. Idle Hunters are re-pointed at the player 60% of the time. Creative and spectator players and Peaceful difficulty are skipped. All numbers are constants at the top of `StalkerDirector.java`.
- **Unlit torch:** texture redrawn on the vanilla torch layout (2px stick, burnt-out tip), cutout render layer registered for torches and lanterns (fixes black boxes), wall-torch rotations corrected, torch items now use a flat sprite.


## v0.5.4

- **Observer spawning:** rarer (weight 5 down to 3) but now spawns in packs of 1-10, and `getLimitPerChunk` is raised to 10 so vanilla's default cap of 4 per chunk no longer truncates packs. Observers only spawn at light level 3 or lower, so look in caves and dark rooms.
- **Light wave:** new `LightWaveManager`. An Observer exhales one wave the first time a player comes within 16 blocks. A wave snuffs up to 5 lights within 16 blocks, with an expanding ring of cloud particles, a gust at each light and a swooping sound. `start(...)` is reusable, so any mob can cast one. Constants are at the top of `ObserverEntity.java` and `LightWaveManager.java`.
- **Unlit torches** (floor and wall) no longer emit ambient smoke.


## v0.5.5

- **Retreat into smoke:** a Hunter that has been burning continuously for more than 10 seconds (`StalkerEntity.MAX_BURN_TICKS`) dissolves in a burst of smoke with a teleport sound and is removed. The `StalkerDirector` can later send a fresh one.

## Local source patch notes (assistant update)
- Hunter stalking is more persistent: lost sight is tolerated for 90 ticks, reposition pauses are shorter and occur more often, and environmental ambush attempts are more frequent.
- Chest/furnace emergence is now a 72-tick, staged animation with articulated arms, legs, head, torso, and new hooked fingers. The stalker faces the nearby player as it emerges; chest lid motion, creaking, smoke, portal motes, and dust are timed to the animation.
- Torch snuffing now tries to extinguish up to five visible torches per pass, with a shorter cooldown.
- The Observer's separate implementation is not included in this source archive; it cannot be directly modified here.
- Build verification is pending because the archive contains no Gradle build files or wrapper.


## Update notes — Observer and Stalker redesign

- Observer disappearance now requires a player to look directly at it with an unobstructed line of sight (tight gaze cone, up to 48 blocks). Proximity and bright light no longer trigger disappearance or burn it away.
- Increased the Observer per-chunk limit to 20 so more can coexist.
- Increased Stalker movement speed and pursuit speed; reduced stalking pauses.
- Added a wall-climbing pursuit state when the player is above the Stalker and a climbable wall face is nearby.
- Reworked Stalker model proportions: slimmer torso, longer head profile, elongated forearms and legs, and a more hunched crawling/climbing pose.
- Repainted the Stalker texture with charcoal mottling, scar-like streaks, and subdued red eye details.

Build validation note: this archive does not include a Gradle wrapper JAR, and Gradle was unavailable in the execution environment, so a Minecraft/Fabric build could not be run here.


## Recent testing changes
- Stalker movement speed lowered from 0.55 to 0.48.
- Frequent, client-side interface invasion effects trigger while a Stalker is within 36 blocks: animated red eye over hotbar slot 4 and subtle screen-edge effects on HUD, inventory/crafting, settings, and other screens. These are visual only and do not alter inventory items or block menu controls.
- Crawling hitbox is 0.49 blocks wide and high, allowing clearance through half-block gaps when crawling.
- If blocked while pursuing a player, the Stalker can very slowly break one non-tile block in its way after about 45 seconds.


## v0.5.6 — Staged chest emergence animation

- Replaced the short linear emergence pose with a 72-tick sequence: hands reach first, fingers hook the rim, the head peeks out, the shoulders pull forward, the hips and legs drag free, and the stalker settles back to idle.
- Added articulated hooked fingers, alternating arm pulls, torso twist and weight shifts, head movement, and a two-stage leg drag.
- Turned the stalker toward its nearby target as it emerges.
- Timed chest lid opening/closing, low-pitched chest creaks, smoke, portal motes, and ground dust to the animation beats.
- The animation is client-synchronized through the existing tracked travel state.

Build validation remains pending: Gradle 8.8 is configured in the GitHub Actions workflow, but this environment has no Gradle installation and cannot reach the Gradle distribution host.
