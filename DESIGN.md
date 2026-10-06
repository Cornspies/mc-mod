# Faction Warfare Mod — Design Document

> **Working title** (real name is open decision OD-01).
> **Status:** Draft v0.1 · **Last updated:** 2026-10-06
> **Audience:** the project owner and the coding agent (Claude Code). Read together with `CLAUDE.md`.

---

## 0. How to use this document

| Section | What it means for the agent |
|---|---|
| §2 Decided | Fixed. Do not change without the owner's approval. |
| §3 Technical baseline | Agreed during planning. Treat as decided unless the owner overrides it. |
| §4–§6 Feature specs | What to build. Anything marked *(OD-xx)* depends on an open decision. |
| §7 Open decisions | **Do not decide these silently.** If an open decision blocks the current task, ask the owner. If it doesn't block, implement the listed recommendation as a default (behind a constant or config option) and record it in §10. |
| §9 Technical notes | Facts about Minecraft 26.x / NeoForge that may be newer than the agent's training data. |
| §10 Decision log | Append a row whenever something gets decided. |

`<modid>` is a placeholder until OD-01 is answered.

---

## 1. Vision

A NeoForge mod for a **faction server, where teams fight each other**:

- **Phase 1:** sci-fi "fusion" style firearms, and **supply crates** that fall from the sky at intervals, announced to the whole server, so teams converge and fight over the loot.
- **Phase 2:** a **missile launcher** for long-range raiding, designed to sit alongside Mekanism.

**Inspiration:** *Fusion Warfare* by TeamMonumental (Minecraft 1.7.10, last updated April 2016, GPLv2), <https://github.com/TeamMonumental/FusionWarfare>. We take the *feel* of its weapons and its falling supply crate. This is **not a port**: no code is copied and models are not reproduced 1:1. Appendix A summarizes what the original did, as reference only.

---

## 2. Decided

| ID | Decision | Notes |
|---|---|---|
| D1 | Purpose: faction / team-vs-team PvP server | Multiplayer and dedicated server first. |
| D2 | **Minecraft Java 26.3** ("Wilderness Bound", released 2026-09-15) | Newest version; performance improvements; Mekanism is targeting it. |
| D3 | **NeoForge** only | Mekanism is NeoForge-only. No Fabric or multi-loader setup. |
| D4 | **Java 25** | Required by Minecraft 26.1 and newer. |
| D5 | Fusion Warfare is **inspiration only** | No code copied. New textures and models in a similar style, not 1:1 copies. |
| D6 | **Phase 1 scope:** weapons in a similar style + falling, publicly announced supply crate | |
| D7 | **Phase 2 scope:** missile launcher | |
| D8 | Phase 2 is **partially based on Mekanism** | How deep the integration goes is OD-24. |
| D9 | Everything else from Fusion Warfare is **out of scope** | Machines, generators, reactors, cables and beacons, auras and turrets, team GUI, reinforced blocks, scuba gear, tools and armor, ore generation. Can be revisited later. |

---

## 3. Technical baseline (adopted)

### 3.1 Toolchain

- Start from the official NeoForge MDK for 26.3 (ModDevGradle): <https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle>.
  - As of 2026-09-27 the MDK ships ModDevGradle `2.0.147`, Gradle `9.2.1`, Java toolchain `25`, and `neo_version=26.3.0.23-beta`.
- At project start, **bump `neo_version` to the newest `26.3.0.x` build**. Mekanism builds against `26.3.0.52-beta` as of 2026-10-06.
- **Pin the NeoForge version and only bump it on purpose.** NeoForge for 26.3 is still beta and breaking changes are still landing. Read the changelog when bumping.
- IDE: IntelliJ IDEA **2025.3 or newer** (needed for full Java 25 support).
- Gradle run tasks from the MDK: `runClient`, `runServer`, `runGameTestServer`, `runData`.

### 3.2 Multiplayer & security

These rules exist because the original mod broke all of them (see Appendix A.4).

1. **The server is authoritative.** The client never sends damage, weapon stats, ammo counts, results, or coordinates of blocks to act on. Client→server packets carry **intent only** (e.g. "reload pressed"). The server re-checks everything: held item, ammo, cooldown, reach/distance, ownership.
2. **Typed packets.** Use NeoForge's payload system (`CustomPacketPayload` + `StreamCodec`). Validate every field, clamp numbers, and range-check positions and slots. Make sure handlers that touch the world run on the main server thread.
3. **Client/common separation.** Code that touches client-only classes lives in a `client` package. It is reached only from the client entry point (the MDK pattern `@Mod(value = MODID, dist = Dist.CLIENT)`). Run `runServer` at every milestone to catch leaks.
4. **Damage attribution.** All weapon damage uses a **custom damage type**. The *causing* entity is the shooting player and the *direct* entity is the projectile. That way vanilla team friendly-fire settings, kill messages, and claim/PvP-protection mods work without extra code.
5. **Chunk loading.** Anything that must keep running with no players nearby (the falling crate now, missiles later) holds a forced-chunk ticket through NeoForge's chunk-ticket API. Release it as soon as it's no longer needed.
6. **Performance.** No per-block entities for destruction effects. Batch block changes. Keep per-tick work small.

### 3.3 Data and config

- Item state such as ammo and reload progress lives in **data components**, not NBT.
- Supply-crate loot lives in **loot table JSON** files, one per rarity. Generate them with `runData` where practical.
- Gameplay numbers (schedules, weights, ranges, toggles) go in a **SERVER config** (`ModConfigSpec`).
- Weapon stats start as one central record/table in code (single source of truth). Whether to move them to datapack JSON later is OD-29.

### 3.4 Integration policy

- Third-party mods are **optional (soft) dependencies**. Their code lives in an isolated `compat` package that is loaded only when the other mod is present. Our mod must work without them.
- Prefer NeoForge's standard **energy capability (FE)** and common `c:` item tags. Mekanism and most tech mods interoperate through these.

### 3.5 Definition of done (every feature)

- `./gradlew build` passes.
- A dedicated server (`runServer`) starts with the mod and the feature works there.
- Tested with **at least two players** (two clients on `runServer`).
- New config options are commented. All player-facing text is in `assets/<modid>/lang/en_us.json`.
- No client classes are referenced from common code.
- §10 is updated if any decision was made along the way.

---

## 4. Phase 1 — Weapons

### 4.1 Goals

- A handful of weapons with clearly different roles, fun in team fights.
- Cheat-resistant (see §3.2).
- The best weapons come from supply crates, which gives teams a reason to fight over drops.

### 4.2 Behaviour (baseline)

- **Firing:** use the vanilla item-use flow (right-click starts using the item; while held, the use-tick callback runs on the server). The **server** spawns projectiles at the weapon's fire rate. Weapons are either semi-auto (one shot per click) or full-auto (fires while held). Verify the exact method names in 26.3; don't rely on memory.
- **Ammo:** the magazine count is a data component on the weapon. **Reload** is a keybind (default **R**). The client sends a "reload" intent and the server consumes ammo from the player's inventory over the reload time. *(Ammo model: OD-05.)*
  - **26.3 caveat:** input moved from GLFW to SDL and key codes changed. Define default keys with the `InputConstants` constants, not raw numbers.
- **Projectiles:** fast server-side entities with an owner and slight gravity. Hits apply the custom damage type (§3.2.4). *(Projectiles vs hitscan: OD-04.)*
- **Spread** is calculated on the server. **Recoil** is a cosmetic camera kick on the client.
- **Invulnerability frames:** vanilla's hurt cooldown would swallow rapid-fire hits. *(OD-07.)*
- **HUD:** ammo counter while a weapon is held. The item durability bar shows the magazine level (Fusion Warfare did this).
- **Melee:** bashing with a gun does small damage. Optional.

### 4.3 Proposed roster (draft; final list is OD-03)

| Weapon | Role | Proposed source |
|---|---|---|
| Pistol | Semi-auto sidearm | Craftable |
| SMG | Full-auto, close range | Craftable |
| Shotgun | Pellet burst, close range | Craftable |
| Sniper rifle | High damage, long range, zoom | **Crate-only** |

**Later candidates:** auto pistol, gatling gun (spins up before firing), rocket launcher (explosive), grenades (frag / flash / gas). Fusion Warfare's numbers are in Appendix A.1 as inspiration only; they need rebalancing (OD-06).

### 4.4 Assets

- Own Blockbench models and textures in a sci-fi "fusion" style, using Fusion Warfare's look as a reference. Not 1:1 copies (D5).
- Sounds: self-made or CC0/CC-BY. Keep an `ATTRIBUTION.md` listing sources.

---

## 5. Phase 1 — Supply crate

### 5.1 Goals

- A recurring, server-wide event that pulls teams into contested fights.
- The main source of top-tier gear.

### 5.2 Flow

1. **Schedule** *(OD-15)*: a server-side manager decides when the next drop happens. Store the next drop time and any active drops in **SavedData**, so restarts don't reset the timer or lose an active crate.
2. **Pick a location** *(OD-18)* inside the drop zone:
   - Find the surface with a heightmap.
   - Reject water, lava and unsafe spots, and claimed land if claims integration is enabled (OD-22).
   - Retry up to N times; if nothing valid is found, skip this drop and log it.
3. **Announce** *(OD-14)* to all players: rarity, location, and a countdown (e.g. 60 seconds) with reminders. Optionally play a server-wide sound.
4. **Force-load the landing chunk** (§3.2.5).
5. **Drop:** spawn a falling-crate entity high above the target. It descends slowly (parachute look) with a visible smoke trail.
6. **Land:** place a crate block.
   - It is a block entity container, with the rarity stored in its block state.
   - Loot comes from the loot table `<modid>:supply_crate/<rarity>`.
   - Show a visible marker (smoke or beam) until it's opened.
   - Announce the landing.
7. **Claim and open** *(OD-16)*.
8. **Clean up:** remove the crate when it's emptied or the despawn timer runs out *(default 30 min, like the original)*. Release the chunk ticket.

### 5.3 Admin commands (permission level 2)

- `/<modid> drop now [rarity] [x z]` — force a drop now.
- `/<modid> drop next` — show when the next drop is due.
- `/<modid> drop list` — list active crates.
- `/<modid> drop clear` — remove all active crates.

### 5.4 Server config

- `enabled`
- `intervalMinMinutes`, `intervalMaxMinutes`, `minPlayersOnline`
- `zoneMode`, `zoneCenter`, `zoneRadius`
- `rarityWeights`
- `countdownSeconds`, `despawnMinutes`
- `announcePrecision`
- `dimension`

### 5.5 26.3 caveat for loot tables

The loot table JSON format changed in 26.3: new loot data types, number providers split into int and float variants, and changes to loot conditions/functions. Write the tables for the 26.3 format (ideally generated with `runData`). Check the 26.3 migration primer (§9) instead of copying older examples.

---

## 6. Phase 2 — Missile launcher (draft)

### 6.1 Goals

- An expensive, long-range team-vs-team raiding tool, with counterplay.
- Powered and crafted in a way that fits a Mekanism tech progression (OD-24).

### 6.2 Baseline requirements

- Missiles keep flying and detonate correctly with **no players near the target** (chunk tickets along the path and at impact).
- Explosions edit blocks in batches. No per-block entities.
- Targeting is validated on the server (range, permissions).
- Respect claims and raid rules (OD-22, OD-23, OD-27).
- Powered with FE (Mekanism-compatible).

### 6.3 Mekanism status (as of 2026-10-06)

- **Not yet released for 26.3.** The latest public release is 10.7.19 for 1.21.1.
- Active development is on the `26.3` branch of <https://github.com/mekanism/Mekanism>:
  - mod version `10.8.0`, NeoForge `26.3.0.52-beta`, Java 25
  - about 130 commits in the last 30 days
  - the build file lists 1.21.1 as the previous version
- Their 26.1 and 26.2 branches stopped when the next drop came out, without a release. If a 26.4 drop comes out first, their target could move again.
- License: **MIT**, so their source is a good reference for block entities, menus, energy and models on 26.3. Keep attribution if anything is adapted.
- **Plan:** no hard dependency until 10.8.0 is released. Use FE and `c:` tags until then.

### 6.4 Reference

What Fusion Warfare's missiles did, and what to avoid, is in Appendix A.3.

---

## 7. Open decisions

**Blocks** = the milestone (§8) that needs the answer.

### Project

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-01 | Mod name, mod ID, Java package / group ID | — | Pick before scaffolding | **M0** |
| OD-02 | License of this mod | MIT / LGPL / GPL / All Rights Reserved | Free choice, since no Fusion Warfare code or assets are reused. MIT is simple and common | **M0** |

### Weapons

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-03 | Weapon roster for v1 | See §4.3 | Pistol, SMG, shotgun, sniper; others later | M3 |
| OD-04 | Ballistics | Projectile entities / hitscan / mixed | Fast projectile entities for all v1 weapons; revisit for the sniper | M2 |
| OD-05 | Ammo model | One shared ammo item / ammo per weapon class / FE-charged weapons | One shared ammo item, which keeps a loot and crafting economy | M2 |
| OD-06 | Damage scale and armor interaction | Keep the original's scale (12–60 HP per hit, far above vanilla) / rebalance around vanilla armor | Rebalance against target time-to-kill vs. diamond and netherite armor; armor applies | M3 |
| OD-07 | Invulnerability frames vs rapid fire | Keep vanilla i-frames / bypass for weapon damage only | Bypass for our damage type only, e.g. via the vanilla damage-type tag for bypassing the hurt cooldown (verify it still exists in 26.3) | M2 |
| OD-08 | How weapons are obtained | Craftable / crate-only, per weapon | Low tier craftable; sniper and future heavy weapons crate-only (config toggle) | M3 |
| OD-09 | Recipe materials | Vanilla / own components / Mekanism parts | Vanilla plus a few own components (barrel, core, grip) | M3 |
| OD-10 | Aiming UX | Zoom / aim-down-sights / crosshair style / scope levels | Hip-fire for all; sniper gets zoom levels | M3 |
| OD-11 | Headshots / hit zones | Yes / no | No for v1 | M3 |
| OD-12 | Latency feel | Pure server / client-side cosmetic tracer for instant feedback | Server authority plus a cosmetic client tracer | M2–M4 |
| OD-13 | Art and animation depth | Static 3D item models / simple recoil and reload animations | Static models plus simple held pose for v1 | M3 |

### Supply crate

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-14 | Announcement precision | Exact coordinates / rounded area / direction + distance | Exact (like the original); configurable | M1 |
| OD-15 | Schedule | Fixed interval / random window / moon phase / min players online | Random 60–120 real minutes, only with ≥ N players online | M1 |
| OD-16 | Claim mechanic | Instant open / capture timer (stand near it X s, reset by taking damage) / locks to first team | Instant open for v1; capture timer as v1.1 | M1 |
| OD-17 | Rarity tiers and weights | Original: 3 tiers, 60% / 30% / 10% | Keep 3 tiers, configurable | M1 |
| OD-18 | Drop zone | Radius around spawn / anywhere inside world border / admin-defined zones | Radius around spawn (original: ±250), configurable | M1 |
| OD-19 | Loot contents per tier | — | Define once weapons exist | M4 |
| OD-20 | Can crates be broken or moved? | Yes / no | No (unbreakable until emptied or despawned) | M1 |
| OD-21 | Dimensions | Overworld only / configurable list | Overworld only | M1 |

### Server and faction

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-22 | Team and claims mod | Open Parties and Claims (OPAC) / other / none | OPAC as an optional integration. NeoForge 26.3 beta builds exist (latest 0.32.8, 2026-10-05) | M4 |
| OD-23 | Raid rules | Always raidable / raid windows ("War Day", an idea from the original's TODO list) / offline protection | Decide before Phase 2 | Phase 2 |

### Phase 2

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-24 | Mekanism integration depth | Soft (FE + `c:` tags) / hard (Mekanism parts in recipes, Mekanism chemicals as missile fuel) | Soft first; hard after Mekanism 10.8.0 for 26.3 is released | Phase 2 |
| OD-25 | Missile types and tiers | — | — | Phase 2 |
| OD-26 | Targeting UX and max range | Designator item / coordinate GUI / map | — | Phase 2 |
| OD-27 | Block damage rules | Vs. claims, vs. specific blocks | — | Phase 2 |
| OD-28 | Counterplay | Interceptors / incoming-missile warning / cooldowns | — | Phase 2 |

### Later

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-29 | Weapon stats source | Code table / server config / datapack JSON | Code table now; datapack later if admins want tuning | Later |

---

## 8. Milestones (Phase 1)

The supply crate comes first because it is self-contained and proves out the shared plumbing: SavedData, chunk tickets, loot tables, commands and config.

| Milestone | Deliverables |
|---|---|
| **M0 Setup** | OD-01 and OD-02 answered. Project created from the 26.3 MDK; NeoForge bumped and pinned; mod ID and package set; `.gitignore`; git repo. Builds, and runs on both `runClient` and `runServer`. |
| **M1 Supply crate MVP** | Crate block and block entity, three rarity loot tables (placeholder vanilla loot), falling-crate entity, scheduler with SavedData, location picking, announcements and countdown, chunk ticket, despawn, admin commands, server config. |
| **M2 Weapon framework** | Weapon base item, central stats record, ammo item, ammo/reload data components, reload keybind and packet, projectile entity, custom damage type, placeholder sounds, ammo HUD. One working test gun. |
| **M3 Weapon roster** | v1 roster (OD-03) with models and textures, recipes (OD-09), balance pass (OD-06). |
| **M4 Integration & polish** | Weapons in crate loot (OD-19), optional OPAC integration (OD-22), config documentation, two-player playtest on a dedicated server, release v0.1.0. |

---

## 9. Technical notes for Minecraft 26.x (for the coding agent)

**Your training data may predate these versions. Do not write 1.20 / 1.21.1-era API calls from memory without checking them against the actual 26.3 sources in the dev environment.**

### Versions

- Version scheme is `YY.D.H`: year, drop, hotfix. `26.3` is the third drop of 2026. `1.21.11` was the last `1.x` version.
- NeoForge versions follow it: `26.3.0.x(-beta)`.

### Code and mappings

- Since 26.1 the game ships **unobfuscated**, with Mojang's real names, including parameter and local-variable names. No Yarn or Parchment mappings are needed.
- `ResourceLocation` was renamed to **`Identifier`** in 1.21.11.
- Mods built for 1.21.11 or older do not work on 26.x, not even as compile-only dependencies.

### Documentation

- docs.neoforged.net currently only covers up to **26.1**.
- For anything changed since then, read the **migration primers** (one per version, 1.21.2 through 26.3): <https://github.com/neoforged/.github/tree/main/primers>. They are written against vanilla using official names.

### Primer highlights relevant to this project

- **26.1:** Java 25 and deobfuscation; data component initializers for items; item instances and stack templates; removal of the old block and item renderers; SavedData changes; container screen changes; loot type unrolling; validation overhaul.
- **26.2:** large rendering overhaul (Vulkan backend, vertex formats rewritten, GUI reorganization, feature rendering); more resource keys.
- **26.3:**
  - LWJGL now uses **SDL instead of GLFW**, so key codes changed; use `InputConstants`.
  - "Renderpearl" client rendering changes; item quads.
  - Reloadable datapack registries.
  - New loot data types; number providers split into int and float; loot condition and function changes.
  - Command response changes.
- If your knowledge stops around 1.21.1, also skim the 1.21.2 – 1.21.11 primers. Rendering, item models and GUIs changed a lot in that span.

### Reference code

- **Mekanism `26.3` branch** (MIT): <https://github.com/mekanism/Mekanism/tree/26.3>. Real-world code for block entities, menus and screens, energy, models and data generation on this exact version.
- **NeoForge and vanilla sources** in the dev environment, now with readable names.
- **Fusion Warfare** (GPLv2): design ideas only. Its 1.7.10 code is useless on 26.3 anyway. Do not copy it.

### Ecosystem on 26.3 (as of 2026-10-06)

- JEI: beta builds.
- Open Parties and Claims: NeoForge beta `0.32.8`.
- Curios: `17.0.0-beta.2+26.3` (used by Mekanism's build).
- Mekanism: not released yet (§6.3).

---

## 10. Decision log

| Date | ID | Decision | By |
|---|---|---|---|
| 2026-10-06 | D1–D9 | Initial decisions from planning (see §2) | Owner |

---

## Appendix A — Fusion Warfare reference (inspiration only)

Source: <https://github.com/TeamMonumental/FusionWarfare>, version `1.7.10-1.3.2`, GPLv2.

### A.1 Weapons

Damage is in health points (20 HP = full health; a vanilla diamond sword does 7).

| Weapon | Damage per hit | Pellets | Magazine | Reload (ticks per round) | Spread | Auto | Recoil (° pitch kick) | Source |
|---|---|---|---|---|---|---|---|---|
| Fusion Pistol | 12 | 1 | 16 | 5 | ±1° | no | 5 | Craftable; blue crate |
| Fusion Auto Pistol | 10 | 1 | 24 | 3 | ±3° | yes | 1 | Craftable; blue crate |
| Fusion Shotgun | 30 | 6 | 4 | 10 | ±8° | no | 20 | Craftable; orange crate |
| Fusion SMG | 10 | 1 | 32 | 3 | ±2° | yes | 2 | Craftable; orange crate |
| Fusion Sniper Rifle | 60 | 1 | 3 | 20 | 0° | no | 25 | Red crate only (by default) |
| Fusion Gatling Gun | 1 | 1 | 128 | 1 | ±1° | yes, after ~5-tick spin-up | 0 | Red crate only (by default) |
| Rocket Launcher | Explosion power 6 (TNT is 4), no fire | — | Uses "rocket" items | — | — | no | — | Red crate only (by default) |

Mechanics notes:

- **Ammo:** one shared "fusion ammo" item. Reloading adds one round per *N* ticks and consumes `pellets` ammo items per round, so partial reloads were possible. Reload key: **R**.
- **Bullets:** entities moving about 4 blocks per tick with slight gravity. The original reset the target's invulnerability timer on hit, so rapid fire dealt full damage.
- **Sniper:** left-click cycled through 3 scope settings.
- **Melee:** bashing with a gun did 4 HP to players.
- **Recipes:** shapeless, from gun parts (barrel, core, handle) plus chips. Upgrades were crafted from the previous gun (pistol → auto pistol → SMG). High-tier weapon recipes (sniper, gatling, rocket launcher) were **disabled by default**, which made crates the only source.
- **Grenades** (fusion / flash / chemical) existed in code but were disabled in the last release.

### A.2 Supply crate

- **When:** overworld only, once per **full-moon night**, after midnight (in-game time > 18000). That is roughly every 2 h 40 min of real time, and sleeping skips nights.
- **Where:** random X and Z within **±250 blocks of world spawn**. The crate starts falling from y = 256.
- **Rarity:** blue 60%, orange 30%, red 10%.
- **Announcement:** chat message to all players with the **exact X/Z**, color-coded by rarity, plus a global sound.
- **Loot:** per-rarity list with "normal" and rarer "critical" entries.
  - Blue: pistols, ammo, materials.
  - Orange: shotgun, SMG, materials.
  - Red: sniper, gatling, rocket launcher, armor, lots of ammo.
- **Crate block:** disappears when emptied, or after 30 minutes. The timer only counted while the chunk was loaded.
- No chunk loading, no claims check, schedule not configurable.

### A.3 Missiles (Phase 2 reference)

- **Types:**
  - **Velocity** T1–T3: destroys normal blocks in a sphere (radius 4 / 6 / 8) and weakens "reinforced" blocks by 1 level.
  - **Breaching** T1–T3: radius 5; weakens reinforced blocks by 2 / 3 / 4 levels and destroys normal blocks.
  - **EMP** T1–T3: sets stored energy of machines in a radius (4 / 6 / 8) to zero.
  - **Pyroblast:** spawns blazes around the target area.
  - A few joke missiles.
- **Launch:** from a missile launcher block or a missile silo multiblock. The target was set with a designator or location-linker item. The silo had a "spray mode" that scattered missiles ±25 blocks.
- **Flight:** the missile climbs to y = 300, teleports to directly above the target, falls, and detonates on impact.

### A.4 Problems not to repeat

- The client sent damage, pellet count and spread when firing, and the server trusted them. Ammo wasn't checked server-side.
- Machine toggle packets accepted any coordinates, with no team or distance check.
- Client-only classes were referenced from common item and registry code, a dedicated-server crash risk.
- The team GUI worked by typing `/scoreboard` commands for the player, which needs operator permissions.
- Team "security" only blocked opening the GUI. Enemies could still mine the machines.
- Missiles froze in unloaded chunks above the target.
- Every block destroyed by an explosion spawned an entity, causing lag during sieges.
