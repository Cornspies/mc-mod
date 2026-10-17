# Fusion Factions — Design Document

> **Mod ID:** `fusionfactions` · **Package:** `com.cornspies.fusionfactions` · **License:** MIT
> **Status:** Phase 1 released as v0.1.0; Phase 2 released as v0.2.0; Phase 3 planned (D60–D74) · **Last updated:** 2026-10-16
> **Audience:** the project owner and the coding agent (Claude Code). Read together with `CLAUDE.md`.

---

## 0. How to use this document

| Section | What it means for the agent |
|---|---|
| §2 Decided | Fixed. Do not change without the owner's approval. |
| §3 Technical baseline | Agreed during planning. Treat as decided unless the owner overrides it. |
| §4–§6b Feature specs | What to build. Anything marked *(OD-xx)* depends on an open decision. |
| §7 Open decisions | **Do not decide these silently.** If an open decision blocks the current task, ask the owner. If it doesn't block, implement the listed recommendation as a default (behind a constant or config option) and record it in §10. |
| §9 Technical notes | Facts about Minecraft 26.x / NeoForge that may be newer than the agent's training data. |
| §10 Decision log | Append a row whenever something gets decided. |

---

## 1. Vision

A NeoForge mod for a **faction server, where teams fight each other**:

- **Phase 1:** sci-fi "fusion" style firearms, and **supply crates** that fall from the sky at intervals, announced to the whole server, so teams converge and fight over the loot.
- **Phase 2:** a **missile launcher** for long-range raiding, designed to sit alongside Mekanism.
- **Phase 3:** **industry** (ores, power, machines, materials) that missiles and weapons are built from, and a guided **rocket launcher**.

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
| D9 | Everything else from Fusion Warfare is **out of scope** | Machines, generators, reactors, cables and beacons, auras and turrets, team GUI, reinforced blocks, scuba gear, tools and armor, ore generation. Can be revisited later. **Amended by D60:** Phase 3 adds our own ores, machines, generators, cables and one helmet. |
| D10 | Name **Fusion Factions**, mod ID **`fusionfactions`**, Java package and group ID **`com.cornspies.fusionfactions`** | Was OD-01. |
| D11 | License: **MIT** | Was OD-02. |
| D12 | Crate schedule: **fixed interval** (default 90 min), only while at least `minPlayersOnline` players are online (default 2) | Was OD-15. Both values are config options. See §10 for how the player minimum is applied. |
| D13 | Crate claim: **instant open** in v1 | Was OD-16. A capture timer is a v1.1 candidate. |
| D14 | Announcement precision: **exact coordinates** by default | Was OD-14. Configurable (`announcePrecision`). |
| D15 | **3 rarity tiers**: common / rare / legendary (blue / orange / red), weights 60 / 30 / 10 | Was OD-17. Weights are configurable. |
| D16 | Drop zone: random X/Z within **±250 blocks of world spawn** (square) | Was OD-18. Configurable. |
| D17 | Crates are **unbreakable and immovable** until emptied or despawned (default 30 min) | Was OD-20. |
| D18 | Crates drop in the **overworld only** | Was OD-21. The dimension is a config option. |
| D19 | Ballistics: **fast projectile entities** for all v1 weapons | Was OD-04. Revisit for the sniper. |
| D20 | Ammo: **one shared ammo item** for every weapon | Was OD-05. |
| D21 | Weapon damage **bypasses the hurt cooldown** (i-frames), via the `minecraft:bypasses_cooldown` damage-type tag on our damage type only | Was OD-07. Other damage keeps vanilla i-frames. |
| D22 | Latency: **server authority plus a cosmetic client tracer** (muzzle flash, tracer, recoil) on the shooter's client | Was OD-12. |
| D23 | v1 roster: **pistol, auto pistol, SMG, shotgun, sniper rifle, gatling gun** | Was OD-03. Rocket launcher and grenades later. |
| D24 | Damage is **rebalanced around vanilla armor, with partial armor piercing**: bullets ignore part of the armor | Was OD-06. Numbers are in §4.3 and §10. |
| D25 | **Headshots do bonus damage** | Was OD-11. |
| D26 | Pistol, auto pistol, SMG and shotgun are **craftable**; the **sniper and gatling are crate-only** by default, with a config option to enable their recipes | Was OD-08. |
| D27 | Recipes use **vanilla items plus our own parts** (barrel, core, grip) | Was OD-09. |
| D28 | **Hip-fire** for all weapons; the **sniper has a zoom key** that cycles zoom levels | Was OD-10. Superseded by D38–D41. |
| D29 | **Static 3D item models** with a simple held pose, no animations, in v1 | Was OD-13. |
| D30 | Phase 1 adds **weapon modification**: a craftable **weapon workbench** with the weapon in a center field and **upgrade fields around it, depending on the weapon** | Owner idea, 2026-10-08. Spec in §4.5. |
| D31 | Upgrades are **tiered pure upgrades**: tiers **I / II / III**, each improves one stat with no downside | Higher tiers are rarer. |
| D32 | Upgrade types: **penetration, magazine size, noise, recoil, reload speed, accuracy, range**. **No damage upgrade** | Owner named the first four and picked the next three; damage was left out to keep time-to-kill readable. |
| D33 | Upgrade parts are **crate-only** | No recipes. The workbench itself is craftable. |
| D34 | Installed parts are **visible on the weapon model** | First and third person, so enemies can read a loadout. |
| D35 | Parts can be **removed at the workbench and are returned** intact. Installing is **free** apart from the part | |
| D36 | Weapon modification is the **new M4**, before crate loot and the v0.1.0 release (now M5) | So the crate loot (OD-19) includes parts from the start. |
| D37 | **Left click (attack) fires**; with a weapon in hand it no longer hits or mines | Owner, 2026-10-08. The client sends trigger pressed / released only. |
| D38 | **Every weapon aims down sights** while **right click is held**: the weapon moves to the middle of the view, the crosshair goes, the view zooms a little (1.1×) | Owner, 2026-10-08. Like first-person shooters. |
| D39 | **Aiming halves the spread and slows the player** (no sprinting). The **sniper is inaccurate from the hip** and only fully accurate when scoped | Owner, 2026-10-08. |
| D40 | **Sights are an upgrade field** on every weapon but the sniper, tiered: **I red dot (1.25×), II holographic (1.5×), III 2× optic**, each with its own reticle | Owner, 2026-10-08. |
| D41 | The **sniper scopes in** with right click; **zoom key and mouse wheel** switch 3× / 6× while scoped | Owner, 2026-10-08. |
| D42 | Aiming and controls are the **new M5**, before crate loot and the release (now M6) | Agent, following D36's reasoning: sights go into the crate loot. *Provisional.* |
| D43 | Supply crates hold **only mod items**: ammo, weapon parts, upgrade parts of the crate's tier, and weapons | Was OD-19. Contents in §5.6. |
| D44 | Heavy weapons (sniper, gatling) are a **chance in legendary crates**, not guaranteed | Owner, 2026-10-08. Default 50%. |
| D45 | **No claims-mod integration** in v0.1.0 | Was OD-22. Revisit later (OPAC remains the candidate). |
| D46 | **Raid windows**: missiles can only be launched during days and hours set in the config. An empty schedule means always | Was OD-23. Missiles already in the air still land; radars and interceptors always work. |
| D47 | **Soft Mekanism integration**: standard FE and common `c:` tags, no dependency. Deeper optional compat once Mekanism 10.8.0 for 26.3 is out | Was OD-24. |
| D48 | **Four missile types**: explosive, breaching, EMP, incendiary, each in **tiers I / II / III** | Was OD-25. |
| D49 | Targets are set with a **designator item** | Was OD-26. Someone has to scout the target. |
| D50 | **Range by tier**, configurable (default 500 / 1000 / 2000 blocks) | Was OD-26. |
| D51 | **Blast resistance counts**: missiles are stronger than TNT, but obsidian-class blocks only break to breaching missiles. Bedrock and a configurable block tag never break | Was OD-27. Claim mods that block explosions keep working (§6.6). |
| D52 | Destroyed blocks **drop nothing**, but **containers spill their contents** | Was OD-27. No lag from item floods; raiding still pays. |
| D53 | Counterplay: **launch cooldown**, **interceptor block**, missiles can be **shot down** with weapons, and a **radar block** instead of a plain warning | Was OD-28. |
| D54 | The **radar** sees every missile whose route enters its range, reports the launch spot as a **fixed-size area** (default ±100 blocks) that contains it, and alerts the **team of the player who placed it** | Owner, 2026-10-08. |
| D55 | **Missile launcher block** for tiers I–II, **missile silo multiblock** for tier III | Owner, 2026-10-08. |
| D56 | **Flight: up, then down.** The missile climbs out of the sky, flies "off-screen" for a time based on distance, then comes down on the target as an entity | Owner, 2026-10-08. Only the launch and target areas are loaded. |
| D57 | Missiles of **tier I–II are crafted**, **tier III comes only from legendary crates** | Owner, 2026-10-08. Matches the weapons. |
| D58 | **Anyone can use** a launcher, silo, radar or interceptor | Owner, 2026-10-08. Capturing an enemy launcher is part of the game. |
| D59 | Power: the blocks take **FE**, but `requirePower` is **off by default** until the server has a tech mod | Owner, 2026-10-08. Vanilla has no power source and D9 rules out our own generators. |
| D60 | **Phase 3 scope:** a guided rocket launcher, resources generated in the world, materials made from them, machines to process them and the power to run them | Owner, 2026-10-16. Amends D9 (ore generation, machines, generators, cables, armor). |
| D61 | Rocket launcher **locks on by aiming**: hold aim on a player to lock, then the rocket homes on them with a **limited turn rate** | Owner, 2026-10-16. |
| D62 | **Lock warning** only for players with gear: a **helmet with a threat-warning implant**, or a **handheld radar** held in a hand that **needs FE** | Owner, 2026-10-16. Without the gear, no warning. |
| D63 | The warning helmet is **strong armor** (diamond-level); the implant is installed into it at the weapon workbench | Owner, 2026-10-16. OD-30 (weapon knockback) stays open. |
| D64 | Rockets **damage entities only**, no blocks | Owner, 2026-10-16. |
| D65 | Rocket launcher **legendary-crate-only**; its **rockets are crafted** and are **their own ammo item** | Owner, 2026-10-16. Amends D20 (one shared ammo item). |
| D66 | The **interceptor needs FE and ammo**: its own crafted **interceptor rockets** | Owner, 2026-10-16. |
| D67 | Power: **combustion generator and solar panel**, our **own FE cable**, a **charging station**; `requirePower` **defaults to true** for every missile block and machine | Owner, 2026-10-16. Amends D59. |
| D68 | Missile inventory icons redrawn at **64×64**, clearer | Owner, 2026-10-16. |
| D69 | Ores: **bauxite** (aluminum), **niter** (oxidizer), **rare earth**, **lithium**. Bauxite and niter everywhere; rare earth and lithium only in some biomes | Owner, 2026-10-16. Contested regions. |
| D70 | Machines: **crusher, chemical mixer, circuit fabricator, assembler** | Owner, 2026-10-16. |
| D71 | **Three chip tiers**: basic, advanced, military, for missile tiers I / II / III | Owner, 2026-10-16. |
| D72 | The new materials are needed by **missiles, missile blocks and weapons, every part** (barrel, grip, core and ammo) | Owner, 2026-10-16. |
| D73 | Build order: **industry first** (M11–M13), then the rocket launcher (M14), then interceptor ammo, icons and the release (M15) | Owner, 2026-10-16. |
| D74 | Missiles, rockets and missile blocks are built in the **assembler** instead of the crafting table | Owner, 2026-10-16 (part of D70). |

---

## 3. Technical baseline (adopted)

### 3.1 Toolchain

- Start from the official NeoForge MDK for 26.3 (ModDevGradle): <https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle>.
  - As of 2026-09-27 the MDK ships ModDevGradle `2.0.147`, Gradle `9.2.1`, Java toolchain `25`, and `neo_version=26.3.0.23-beta`.
- At project start, **bump `neo_version` to the newest `26.3.0.x` build**. Mekanism builds against `26.3.0.52-beta` as of 2026-10-06.
  - Done in M0: pinned to **`26.3.0.52-beta`**, the newest build on 2026-10-06.
  - Bumped on 2026-10-17 (owner) to **`26.3.0.86`**, the newest build; NeoForge 26.3 left beta with `26.3.0.76`. Mekanism's `26.3` branch requires `[26.3.0.76,)`.
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
- Gameplay numbers (schedules, weights, ranges, toggles) go in a **server-side config** (`ModConfigSpec`). In FML 12 (26.x) that is the `SYNCED` config type: `config/fusionfactions-synced.toml`, overridable per world in `<world>/syncedconfig`.
- Weapon stats start as one central record/table in code (single source of truth). Whether to move them to datapack JSON later is OD-29.

### 3.4 Integration policy

- Third-party mods are **optional (soft) dependencies**. Their code lives in an isolated `compat` package that is loaded only when the other mod is present. Our mod must work without them.
- Prefer NeoForge's standard **energy capability (FE)** and common `c:` item tags. Mekanism and most tech mods interoperate through these.

### 3.5 Definition of done (every feature)

- `./gradlew build` passes.
- A dedicated server (`runServer`) starts with the mod and the feature works there.
- Tested with **at least two players** (two clients on `runServer`).
- New config options are commented. All player-facing text is in `assets/fusionfactions/lang/en_us.json`.
- No client classes are referenced from common code.
- §10 is updated if any decision was made along the way.

---

## 4. Phase 1 — Weapons

### 4.1 Goals

- A handful of weapons with clearly different roles, fun in team fights.
- Cheat-resistant (see §3.2).
- The best weapons come from supply crates, which gives teams a reason to fight over drops.

### 4.2 Behaviour (baseline)

- **Firing** *(D37)*: the attack button (left click, or whatever it is bound to) is the trigger. The client sends only "trigger pressed" / "trigger released" (`TriggerPayload`); the **server** fires on its own ticks while the trigger is held (`Trigger`), at the weapon's fire rate. Weapons are either semi-auto (one shot per pull) or full-auto (fires while held). With a weapon in hand the attack button no longer hits or mines.
- **Ammo:** the magazine count is a data component on the weapon. **Reload** is a keybind (default **R**). The client sends a "reload" intent and the server consumes ammo from the player's inventory over the reload time. *(One shared ammo item: D20.)*
  - **26.3 caveat:** input moved from GLFW to SDL and key codes changed. Define default keys with the `InputConstants` constants, not raw numbers.
- **Projectiles:** fast server-side entities with an owner and slight gravity. Hits apply the custom damage type (§3.2.4). *(Projectile entities: D19.)* In water they slow down like arrows, which gives roughly 9 blocks of range, then curve down and sink harmlessly to the ground. A bullet that leaves the water keeps its lost speed and drops in a short arc (§10).
- **Spread** is calculated on the server. **Recoil** is a cosmetic camera kick on the client.
- **Invulnerability frames:** vanilla's hurt cooldown would swallow rapid-fire hits, so our damage type bypasses it *(D21)*.
- **HUD:** ammo counter while a weapon is held. The item durability bar shows the magazine level (Fusion Warfare did this).
- **Knockback:** a per-weapon value (§4.3); knockback resistance reduces it. Armor that reduces or removes it is OD-30.
- **Headshots** *(D25)* do 1.5× damage. A hit counts as a headshot if it lands no more than 0.3 blocks below the target's eyes. The shooter hears a hit marker on every hit, and a different one for headshots.
- **Armor piercing** *(D24)*: bullets ignore 30% of the armor's protection (the sniper 50%). Enchantments such as Protection and Projectile Protection still count in full.
- **Aiming down sights** *(D38–D41)*: holding the use button (right click) aims. It is vanilla's item use, so the server knows who aims.
  - First person: the weapon moves from the hip to the middle of the view in 4 ticks, so the iron sights (or the sight) line up with the middle of the screen. The crosshair is hidden; the view zooms 1.1×, or by the sight (§4.5). Each shot kicks the weapon back a little.
  - Aiming halves the spread and slows the player to 80% of walking speed, no sprinting. The sniper has ±4° hip-fire spread and none when scoped.
  - Once the weapon is raised, a sight replaces it with a sight picture drawn on the HUD, so it frames the target instead of covering it:
    - red dot (I) and holographic (II): a thin frame around a large, faintly tinted window on a short base with the tier color, and a glowing reticle in the middle (red dot; red ring with a dot and four ticks). The holographic window is bigger and has a hood.
    - 2× optic (III): a scope view (black around a round lens) with an amber chevron whose tip is the aim point, a post with range marks below it and thin lines in from the sides.
  - The reticles are textures from `tools/scope_overlay.py` with a soft glow, sized by the screen height.
  - The sniper shows a scope instead: black around a round lens, a duplex mil-dot reticle and the zoom level. Zoom key (default **Z**) or mouse wheel switch 3× / 6×. The weapon is hidden while scoped.
  - Zoom slows the mouse to match. Third person: players hold every weapon with both arms raised (the crossbow aiming pose).
  - Right clicking a block that has an action (door, chest, workbench) still uses the block instead of aiming, as in vanilla.
- **Melee:** none: the attack button fires (D37).

### 4.3 Roster (D23)

| Weapon | Role | Source (D26) |
|---|---|---|
| Pistol | Semi-auto sidearm | Craftable |
| Auto pistol | Full-auto sidearm | Craftable |
| SMG | Full-auto, close range | Craftable |
| Shotgun | Pellet burst, close range | Craftable |
| Sniper rifle | High damage, long range, scope (D41) | **Crate-only** (recipe behind a config option) |
| Gatling gun | Spins up before firing, very high rate of fire | **Crate-only** (recipe behind a config option) |

**Later candidates:** rocket launcher (explosive), grenades (frag / flash / gas). Fusion Warfare's numbers are in Appendix A.1 as inspiration only.

**First balance pass (M3, provisional).** The single source of truth is `weapon/Weapons.java`. Damage is per pellet, in HP. Speed is in blocks per tick.

| Weapon | Damage | Pellets | Magazine | Ticks between shots | Auto | Reload (ticks/round) | Ammo/round | Spread | Speed | Range (ticks) | Knockback | Piercing |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Pistol | 6 | 1 | 12 | 4 | no | 5 | 1 | ±1° | 4 | 50 | 0.4 | 30% |
| Auto pistol | 4 | 1 | 20 | 3 | yes | 3 | 1 | ±2.5° | 4 | 40 | 0.25 | 30% |
| SMG | 3.5 | 1 | 30 | 2 | yes | 3 | 1 | ±3° | 4 | 30 | 0.15 | 30% |
| Shotgun | 2.5 | 8 | 6 | 16 | no | 10 | 2 | ±6° | 3.5 | 12 | 0.2 | 30% |
| Sniper rifle | 18 | 1 | 5 | 30 | no | 20 | 2 | 0° | 8 | 40 | 0.8 | 50% |
| Gatling gun | 2.5 | 1 | 120 | 1 (after 20 ticks spin-up) | yes | 1 | 1 | ±3.5° | 4 | 40 | 0.05 | 30% |

Body-shot hits to kill a full-health player at the fastest fire rate. Time to kill is in brackets: from the first hit, without the gatling's spin-up. Armor sets are full sets without enchantments. Shotgun numbers assume all 8 pellets hit.

| Weapon | No armor | Iron | Diamond | Netherite |
|---|---|---|---|---|
| Pistol | 4 (0.6 s) | 6 (1.0 s) | 7 (1.2 s) | 8 (1.4 s) |
| Auto pistol | 5 (0.6 s) | 8 (1.05 s) | 11 (1.5 s) | 11 (1.5 s) |
| SMG | 6 (0.5 s) | 10 (0.9 s) | 13 (1.2 s) | 13 (1.2 s) |
| Shotgun | 1 | 2 (0.8 s) | 3 (1.6 s) | 3 (1.6 s) |
| Sniper rifle | 2 (1.5 s) | 2 (1.5 s) | 2 (1.5 s) | 2 (1.5 s) |
| Gatling gun | 8 (0.35 s) | 14 (0.65 s) | 18 (0.85 s) | 18 (0.85 s) |

A sniper headshot kills an unarmored or iron-armored player in one hit.

**Recipes** *(D27)*. Three parts are made from vanilla items, and the weapons from the parts:

- Barrel: `iron, copper, iron` in a row.
- Fusion core: amethyst shard in the middle, redstone on the sides, gold nuggets in the corners.
- Grip: two leather in a column, iron next to the top one.
- Ammo (8): copper ingot, gunpowder and redstone, shapeless.
- Weapons are shapeless. Pistol = barrel + core + grip. Auto pistol = pistol + core. SMG = auto pistol + barrel + grip. Shotgun = 2 barrels + core + grip.
- Sniper rifle = 2 barrels + 2 cores + grip + spyglass, and gatling gun = 4 barrels + 2 cores + grip + iron block. These two only load when `heavyWeaponRecipes = true` in `config/fusionfactions-startup.toml` *(D26)*. It's a startup config, so changing it needs a server restart.

### 4.4 Assets

- Own models and textures in a sci-fi "fusion" style, using Fusion Warfare's look as a reference. Not 1:1 copies (D5).
  - v1: the weapon models are built from boxes by `tools/weapon_models.py`, which writes `models/item/<weapon>.json` and a shared palette texture. Edit the script, not the JSON. Blockbench models can replace them later.
- Sounds: self-made or CC0/CC-BY. Keep an `ATTRIBUTION.md` listing sources.
  - v1: `sounds.json` points at vanilla sound files as placeholders, so there is nothing to attribute yet.

---

### 4.5 Weapon modification (D30–D35)

**Goal:** a reason to keep fighting over crates after a team has its guns, and a way to make a weapon your own.

**Weapon workbench** (block, craftable):

- Right-click opens a menu: the weapon goes in a center field, with up to 8 upgrade fields around it (a 3×3 grid).
- Each field is one upgrade type, always in the same position, so the layout is familiar across weapons. Only the fields the weapon supports are open; the others are locked and drawn greyed out.
- Placing a part in a field installs it on the weapon right away; taking it out removes it and gives it back (D35). Taking the weapon out keeps its parts on it.
- The workbench stores nothing. Closing the menu returns anything still in it to the player, like a crafting table, so nothing can be stolen from it.
- Every change happens through the server-side menu, which validates the weapon, the field and the part (§3.2).

**Upgrade types and fields.** Field position in the 3×3 grid, where the part shows on the model, and the default effect per tier (I / II / III). The weapon is drawn with its muzzle to the left, like its inventory icon, so muzzle parts sit on the left and the stock on the right. The sight sits on top, like on the gun. On the model, every part has a glowing accent in its tier color (blue / orange / red).

| Type | Field | On the model | Effect per tier |
|---|---|---|---|
| Penetration | top right | glowing core in a different color | +10 / +20 / +30 percentage points of armor piercing |
| Magazine | bottom | bigger magazine | +20 / +40 / +60% rounds |
| Noise (suppressor) | left | suppressor on the muzzle | fire sound carries 25 / 50 / 75% less far |
| Recoil | right | recoil pad (a brace on the pistols) | −20 / −35 / −50% camera kick |
| Reload speed | bottom right | magazine coupler | −15 / −30 / −45% reload time per round |
| Accuracy | top left | stabilizer under the barrel | −20 / −35 / −50% spread |
| Sight (D40) | top | I red dot, II holographic, III 2× optic, on the top rail | zoom while aiming 1.25× / 1.5× / 2× (+25 / +50 / +100%) |
| Range | bottom left | accelerator rings on the barrel | +15 / +30 / +50% bullet speed, so the same lifetime carries that much further, with less drop |

**Fields per weapon (default).** Upgrades that would do nothing are left out, e.g. a sight on the sniper, which has its scope.

| Weapon | Penetration | Magazine | Noise | Recoil | Reload | Accuracy | Range | Sight |
|---|---|---|---|---|---|---|---|---|
| Pistol | ✓ | ✓ | ✓ | | ✓ | ✓ | ✓ | ✓ |
| Auto pistol | | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ |
| SMG | | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ |
| Shotgun | | ✓ | | ✓ | ✓ | ✓ | ✓ | ✓ |
| Sniper rifle | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | |
| Gatling gun | ✓ | ✓ | | ✓ | ✓ | ✓ | | ✓ |

**Parts** are items, one per type and tier (8 × 3 = 24), named like "Magazine Upgrade II"; the sights are "Red Dot Sight", "Holographic Sight" and "2x Optic". They come only from supply crates (D33): tier I in common crates, II in rare, III in legendary (§5.6).

**Stats.** The installed parts are a data component on the weapon (`fusionfactions:upgrades`, type → tier). The server computes the effective stats from the base stats in `Weapons` plus the parts (`UpgradeRules`); the client uses the same calculation for cosmetic effects, the ammo HUD and tooltips. A weapon's tooltip lists its parts. Taking out a magazine upgrade drops the rounds that no longer fit.

**Tuning (owner request).** The tier values and the fields per weapon are in the synced server config, section `[weaponUpgrades]` of `config/fusionfactions-synced.toml` (per world: `<world>/syncedconfig/`). Clients get the values when they join. Restart the server after editing. A config written by an older version keeps its old field lists: when a new upgrade type is added (like `sight`), add it to the lists by hand or delete the `[weaponUpgrades.fields]` section to get the new defaults. A part whose type is removed from a weapon's list stays on the weapon, shown as "no effect on this server", and can still be taken out at the workbench. The defaults live in `UpgradeType` (values) and `UpgradeConfig` (fields).

**Noise and hearing range.** Every weapon has a hearing range (48 blocks; the sniper 96). Shots are sent to players within that range by our own packet, and the sound fades out at that distance. The suppressor shortens the range.

## 5. Phase 1 — Supply crate

### 5.1 Goals

- A recurring, server-wide event that pulls teams into contested fights.
- The main source of top-tier gear.

### 5.2 Flow

1. **Schedule** *(D12)*: a server-side manager decides when the next drop happens. Store the next drop time and any active drops in **SavedData**, so restarts don't reset the timer or lose an active crate.
2. **Pick a location** *(D16)* inside the drop zone:
   - Find the surface with a heightmap.
   - Reject water, lava and unsafe spots. (Claimed land isn't checked in v0.1.0, D45.)
   - Retry up to N times; if nothing valid is found, skip this drop and log it.
3. **Announce** *(D14)* to all players: rarity, location, and a countdown (e.g. 60 seconds) with reminders. Optionally play a server-wide sound.
4. **Force-load the landing chunk** (§3.2.5).
5. **Drop:** spawn a falling-crate entity high above the target. It descends slowly (parachute look) with a visible smoke trail.
6. **Land:** place a crate block.
   - It is a block entity container, with the rarity stored in its block state.
   - Loot comes from the loot table `fusionfactions:supply_crate/<rarity>`.
   - Show a visible marker (smoke or beam) until it's opened.
   - Announce the landing.
7. **Claim and open** *(D13)*: anyone can open the crate as soon as it lands.
8. **Clean up:** remove the crate when it's emptied or the despawn timer runs out *(default 30 min, like the original)*. Release the chunk ticket.

### 5.3 Admin commands (permission level 2)

- `/fusionfactions drop now [<rarity>|random [<x z>]]` — force a drop now.
- `/fusionfactions drop next` — show when the next drop is due.
- `/fusionfactions drop list` — list active crates.
- `/fusionfactions drop clear` — remove all active crates.

### 5.4 Server config

- `enabled`
- `intervalMinutes`, `minPlayersOnline`
- `zoneMode` (`SPAWN` / `CENTER`), `zoneCenterX`, `zoneCenterZ`, `zoneRadius`
- `rarityWeights.common`, `.rare`, `.legendary`
- `countdownSeconds`, `despawnMinutes`
- `announcePrecision`
- `dimension`

### 5.5 26.3 caveat for loot tables

The loot table JSON format changed in 26.3: new loot data types, number providers split into int and float variants, and changes to loot conditions/functions. Write the tables for the 26.3 format (ideally generated with `runData`). Check the 26.3 migration primer (§9) instead of copying older examples.

---

### 5.6 Loot (D43, D44)

Generated by `client.datagen.SupplyCrateLoot`; one pool per kind of loot so the odds stay readable. Servers can override the tables with a datapack (`docs/SERVER.md`).

| Rarity | Always | Chances |
|---|---|---|
| Common | 16–32 ammo, 1–2 stacks of weapon parts (1–2 each), 1–2 tier I upgrades | 50%: pistol (30%) or auto pistol (20%) |
| Rare | 32–48 ammo, 2–3 stacks of weapon parts (1–3 each), 1–2 tier II upgrades | 50%: an extra tier I upgrade. 70%: SMG (30%), shotgun (30%) or auto pistol (10%) |
| Legendary | 48–64 ammo, 1–2 tier III upgrades, 1–2 tier II upgrades | 50%: sniper rifle or gatling gun (25% each). 33%: SMG or shotgun. 25%: a tier III missile of any type (D57) |

Every upgrade type is equally likely within a tier. Weapons come loaded.

## 6. Phase 2 — Missile launcher

Decided in D46–D59. The numbers below are first defaults, *provisional* until the Phase 2 playtests, and all of them are in the synced config (`[missiles]`).

### 6.1 Goals

- An expensive, long-range team-vs-team raiding tool, with counterplay.
- Powered and crafted in a way that fits a Mekanism tech progression (D47).

### 6.2 Baseline requirements

- Missiles keep flying and detonate correctly with **no players near the target** (chunk tickets at launch and at impact, D56).
- Explosions edit blocks in batches. No per-block entities.
- Targeting is validated on the server (range, raid window, cooldown, power).
- Raid windows (D46); claim mods are respected through the explosion event (§6.6), without direct integration (D45).
- Powered with FE (Mekanism-compatible), optional (D59).

### 6.3 Mekanism status (as of 2026-10-06, checked again 2026-10-17)

- **Not yet released for 26.3.** The latest public release is 10.7.19 for 1.21.1. On 2026-10-17 there was still no 26.3 build anywhere: not on Modrinth, GitHub releases or their Maven (modmaven.dev), and their CI only publishes from `release/**` branches. The `26.3` branch now needs NeoForge `[26.3.0.76,)`.
- Active development is on the `26.3` branch of <https://github.com/mekanism/Mekanism>:
  - mod version `10.8.0`, NeoForge `26.3.0.52-beta`, Java 25
  - about 130 commits in the last 30 days
  - the build file lists 1.21.1 as the previous version
- Their 26.1 and 26.2 branches stopped when the next drop came out, without a release. If a 26.4 drop comes out first, their target could move again.
- License: **MIT**, so their source is a good reference for block entities, menus, energy and models on 26.3. Keep attribution if anything is adapted.
- **Plan:** no hard dependency until 10.8.0 is released. Use FE and `c:` tags until then.

### 6.4 Items and blocks

| Thing | What it is | Source |
|---|---|---|
| Missiles (12) | One item per type and tier: "Explosive Missile I" … "Incendiary Missile III". Stack to 4 | I–II crafted, III legendary crates (D57) |
| Target designator | Right click a block up to 256 blocks away to store it as the target (position and dimension, shown in the tooltip). Sneak + right click clears it | Crafted |
| Missile launcher | Block with a menu: missile slot, designator slot, the target and distance, power bar, launch button and its status. Fires tiers I–II | Crafted |
| Missile silo | Multiblock (§6.8) that fires every tier, including III | Crafted parts |
| Radar | Block that tracks missiles in its range and alerts its owner's team (§6.9) | Crafted |
| Interceptor | Block that shoots down missiles coming down near it (§6.9) | Crafted |

Anyone can use the blocks (D58). Anyone can also break them; they are as hard as iron blocks.

### 6.5 Launch and flight (D46, D49, D50, D53, D56)

1. A player puts a missile and a designator into a launcher (or the silo controller) and presses **Launch**.
2. The **server** checks: a missile and a target in the same dimension; the target within the tier's range (I 500, II 1000, III 2000 blocks, horizontal) and at least 64 blocks away; the launcher can fire that tier; open sky above the launcher; the launcher's cooldown is over (launcher 60 s, silo 120 s); the current time is inside a raid window; enough power if `requirePower` is on. The status line says which check failed.
3. **Launch:** the missile takes off as an entity with a smoke trail and climbs for 3 seconds, through anything in its way. The launch chunk is held by a chunk ticket meanwhile.
4. **Cruise:** above the sky the entity is removed and the flight becomes a record in SavedData (launch spot, target, type, tier, launching player, time of impact). Flight time is the 3 s climb, the distance at 25 blocks per second and the 4 s descent, so 1000 blocks take 47 s. Its position on the straight line between launch and target is what radars see. Flights survive a restart.
5. **Descent:** 10 s before impact the target chunks are force-loaded; 4 s before impact the missile appears 160 blocks above the target as an entity and falls onto it. It can be **shot**: health 10 / 20 / 40 by tier, bullets and interceptors damage it, and a missile destroyed in the air explodes harmlessly.
6. **Impact:** it detonates on the first block or entity it touches (§6.6), then the tickets are released.

**Raid windows** (D46): `raidWindows` is a list like `"SAT 18:00-23:00"` or `"DAILY 20:00-22:00"`, in the server's time zone (or `raidTimeZone`). Empty means always. The launcher shows when the next window opens.

**Who did it:** the launching player is the causing entity of the missile's damage (custom damage type `fusionfactions:missile`), so kill messages and friendly-fire rules work as for weapons.

### 6.6 Block damage (D51, D52)

- The blast is computed like vanilla's explosion (rays from the center that lose strength through blocks by their blast resistance), with more power than TNT, so walls shield what's behind them.
- **Hard blocks** (blast resistance ≥ 600: obsidian, crying obsidian, netherite and ancient debris, reinforced deepslate …) stop the rays and don't break, except to **breaching** missiles, which treat them as stone.
- `#fusionfactions:missile_immune` (bedrock, barriers, portal frames, command blocks, …) never breaks, not even to breaching missiles. Admins can add blocks with a datapack.
- Destroyed blocks drop nothing. **Containers spill** their contents (chests, barrels, shulker boxes keep their items as a shulker box item, furnaces …).
- The block list goes through NeoForge's explosion event before anything breaks, so **claim and protection mods that stop explosions** also stop missiles (D45 means no direct integration).
- Blocks change in one batch per tick (large blasts spread over a few ticks), no per-block entities.

| Type | Effect per tier I / II / III |
|---|---|
| Explosive | Blast power 6 / 9 / 12 (TNT is 4): craters roughly 5 / 7 / 9 blocks in radius. Hurts entities in range |
| Breaching | Blast power 4 / 5 / 6, also breaks hard blocks. For bunkers |
| EMP | No block damage. Drains all FE from blocks within 8 / 12 / 16 blocks, and shuts down launchers, silos, radars and interceptors there for 30 / 60 / 90 s |
| Incendiary | Blast power 2 / 3 / 4, then sets fire on flammable surfaces within 6 / 8 / 10 blocks and ignites entities |

### 6.7 Power (D47, D59)

- The launcher, silo controller, radar and interceptor have an FE buffer and accept FE from any side (NeoForge's energy capability, so Mekanism cables work).
- `requirePower = false` by default: nothing needs power. With `true`: a launch costs 50k / 100k / 200k FE by tier, the radar uses 20 FE per tick, an interceptor shot costs 10k FE.
- EMP missiles drain the buffers either way.

### 6.8 Missile silo (D55)

- A 3×3 shaft, 4 blocks deep, built into the ground: a full floor of **silo casing**, three rings of casing above it with the shaft open in the middle, and a **silo hatch** in the middle of the top ring. One casing on a side is replaced by the **silo controller**, which has the menu.
- The controller checks the structure while its menu is open and before every launch. The menu shows how many blocks are missing or wrong.
- Missiles start at the bottom of the shaft and leave through the hatch, which opens for the launch and closes 4 s later. The sky above the hatch must be open; range is measured from the shaft.
- Anyone can break the silo's blocks (D58); an incomplete silo can't fire.
- **Formed look** (owner request): once the silo is complete, the controller forms it layer by layer from the floor up (sparks and rising tones, then a beam of light out of the hatch). Formed blocks are darker and connect: the outside walls show one continuous panel with a warning band along the top, the top ring frames the hatch in warning stripes, the shaft is lined inside, the controller shows an animated control panel and the hatch blinks. As soon as a block is missing, the silo turns back to plain blocks. Textures and per-position models come from `tools/silo_textures.py`.

### 6.9 Counterplay (D53, D54)

**Radar:**

- Scans a radius of 256 blocks (horizontal, any height) once a second.
- Sees every missile whose position is in range: on the launch climb, in cruise (its point on the line) and on descent.
- On the first contact with a missile it alerts the **team of the player who placed it** (vanilla scoreboard team; just that player if they have none), wherever they are, in chat and with a sound: the radar's position, whether the missile comes down inside the range and when, and the **launch area**: a ±100-block square that contains the launch spot, placed at random around it (not centered on it).
- Its menu shows a scope (north up) with a blip per contact and a list: distance, direction and the seconds to impact for missiles coming down in range (red), or "passing". While it has contacts it gives a redstone signal (for alarms).
- A radar placed without a player (e.g. by a command) has no owner and alerts nobody; its menu still works. The launch area is the same for every radar, so two radars can't narrow it down.
- An EMP shuts it down like a launcher.
- Look: a low housing with a scope screen and a dish on a mast. The dish turns while the radar is online (faster while it sees missiles) and stops without power or after an EMP, so attackers can see that an EMP worked.

**Interceptor:**

- Fires at descending missiles within 96 blocks with a clear line of sight, one shot every 2 s.
- A shot is a fast homing projectile that deals 20 damage, so tier I–II missiles take one hit and tier III two. Interceptors skip a missile that rockets already on their way will bring down, so several interceptors share the work.
- It works on its own; right click shows its state (ready, reloading, no power, shut down by an EMP).
- Look: a low housing with a pod of four launch tubes. Its rockets are small white rockets with a red nose and a smoke trail.

**Shooting down:** weapon bullets damage a descending missile like any entity (sniper 18, pistol 6 …). Its hitbox is the drawn missile, about 5.5 blocks long. A missile destroyed in the air bursts harmlessly and its flight ends; the player who brought it down sees "Missile shot down!".

### 6.10 Admin commands (permission level 2)

- `/fusionfactions missile list`: missiles in flight, with launch spot, target and time to impact.
- `/fusionfactions missile clear [<id>]`: removes all missiles in flight, or one.
- `/fusionfactions missile launch <type> <tier> <x> <z> [<from x> <from z>]`: launches a missile without a launcher, for testing (ignores raid windows and power).

### 6.11 Reference

What Fusion Warfare's missiles did, and what to avoid, is in Appendix A.3.

---

## 6b. Phase 3 — Industry and the rocket launcher

Decided in D60–D74. As in Phase 2, the numbers below are first defaults, *provisional* until the Phase 3 playtests, and go into the synced config where they are balance values.

### 6b.1 Goals

- Missiles become an industrial effort: real-world-inspired materials (aluminum, oxidizer, chips, batteries) mined from the world and processed in machines that need power. Teams have to build, defend and raid factories, not just crates.
- A guided rocket launcher for fights between players, with counterplay for those who carry the right gear.
- Contested regions: the scarce ores only exist in some biomes.

### 6b.2 Resources (D69)

Each ore has a stone and a deepslate variant and drops a raw item (Fortune works like on vanilla ores). All items get `c:` tags (`c:ores/aluminum`, `c:raw_materials/aluminum`, `c:ingots/aluminum`, `c:dusts/…`), so aluminum or niter from other mods works in our recipes and ours in theirs (D47).

| Ore | Drops | Where | Processed into |
|---|---|---|---|
| Bauxite | raw bauxite | everywhere, y −16 to 112 (most around 48), about as common as copper: 16 veins of up to 10 per chunk | aluminum ingot (furnace), aluminum dust (crusher, 2 per raw) |
| Niter | 2–4 niter | everywhere, y 0 to 80, 8 veins of up to 8 per chunk | used as is: the oxidizer for rocket fuel and ammo |
| Rare earth | raw rare earth | **only in mountain biomes** (`c:is_mountain`), y −32 to 160 so it also shows in the mountains themselves, 8 veins of up to 7 per chunk (about 5 blocks per chunk) | rare earth dust (crusher, 2 per raw) |
| Lithium | raw lithium | **only in desert and badlands biomes** (`c:is_desert`, `c:is_badlands`), y −32 to 64, 3 veins of up to 7 per chunk (about 10 blocks per chunk) | lithium ingot (furnace), lithium dust (crusher) |

Bauxite and niter need a stone pickaxe, rare earth and lithium an iron one. Raw bauxite and raw lithium (and the ores and dusts) smelt into ingots; raw rare earth is only ground in the crusher (M13). Where an ore generates is the biome tag `fusionfactions:has_ore/<ore>`, which a datapack can change.

Silicon comes from vanilla materials: the crusher grinds nether quartz into 2 silicon dust (sand into 1, slowly), which smelts into silicon.

**Existing worlds:** ores only generate in chunks generated after the update. A server that has explored the area around spawn has to explore further out, or reset parts of the map.

### 6b.3 Power (D66, D67, D59 amended)

- **Combustion generator:** burns furnace fuels into FE, 40 FE/t, for as long as a furnace would burn them. Coal lasts 1600 ticks (64k FE); rocket fuel (M13) will burn four times as long. Buffer 100k FE. It pauses while its buffer is full, so no fuel is wasted, and pushes up to 2000 FE/t into the blocks around it. A menu shows the fuel, the flame, its output and its energy.
- **Solar panel:** a low panel; 8 FE/t under open sky by day (sky light 15, so glass is fine), half in rain, nothing at night. Buffer 10k FE; pushes into the blocks around it. Right click shows its output.
- **FE cable:** one tier, connects automatically to cables and to any block with the energy capability (ours, Mekanism's, …), carries 2000 FE/t. Cables form a network; whatever is pushed into it is split evenly between the blocks on it that take FE, never back to the block it came from. Up to 4096 cables per network. Cables hold no FE.
- **Charging station:** charges the FE item in its slot (the handheld radar, or any FE item from other mods) at 500 FE/t, from a 50k FE buffer. Hoppers can take items out once they are full.
- `requirePower` now defaults to **true**: launchers, silo, radar, interceptor and all machines need FE. Existing config files keep their old value (`false`) until the admin changes it (`docs/SERVER.md` says so). The interceptor always needs FE (D66). Output, throughput and charging rate are in `[power]`.
- Recipes (crafting table, so power comes before the machines): generator from iron, copper, a furnace and redstone; solar panel from glass panes, aluminum, a daylight detector and iron; 8 cables from copper and dried kelp; charging station from iron, copper, aluminum and a redstone block.

### 6b.4 Machines (D70)

Every machine is a block with a menu: input slots, an output slot, a progress arrow and an energy bar (50k FE buffer). It works only while powered (if `requirePower`), pausing without power. Hoppers and pipes insert from the top and sides and extract from the bottom; each input slot holds one kind of item, so a hopper can't fill every slot with the same thing. Machine recipes are datapack recipes (types `fusionfactions:crushing`, `mixing`, `fabricating`, `assembling`, generated with `runData`), so servers can change them.

A recipe is shapeless: its inputs (with counts) can be in any input slots, and **nothing else may be in them**. So recipes differ in the kinds of items they take, not only in amounts (the barrel takes copper, the airframe doesn't). Changing the inputs to another recipe restarts the progress. The crusher, mixer and fabricator show their state in a readout; the assembler's 3×3 grid leaves no room, so its arrow's tooltip shows it.

| Machine | Does | FE/t, ticks |
|---|---|---|
| Crusher | raw bauxite, rare earth, lithium or their ore → 2 dusts; niter ore → 5 niter; quartz → 2 silicon dust; sand → 1 silicon dust (200 ticks); aluminum ingot → aluminum dust | 20, 100 |
| Chemical mixer | 1 aluminum dust + 2 niter + 1 binder (slime ball or resin clump) → 2 rocket fuel | 40, 200 |
| Circuit fabricator | silicon → 2 wafers; wafer + inputs → chip (below) | 60, 200 (wafers 100, advanced chip 300, military chip 400 at 100 FE/t) |
| Assembler | builds missiles, rockets, interceptor rockets, the missile blocks and the weapons' parts from 3×3 inputs, like a powered crafting table with its own recipes | 40, 100 (missile blocks 200–300; missiles 80 FE/t, 200 / 400 ticks by tier) |

### 6b.5 Materials (D71, D72)

| Material | Recipe | Used for |
|---|---|---|
| Aluminum ingot / dust | bauxite, see 6b.2 | airframes, weapon barrels and grips, fuel |
| Rocket fuel | chemical mixer | missiles, rockets, interceptor rockets; also generator fuel |
| Silicon | silicon dust, smelted (`c:silicon`) | wafers |
| Silicon wafer | circuit fabricator | chips |
| Basic chip | fabricator: wafer + copper ingot + 2 redstone | tier I missiles, interceptor rockets, guided rockets, fusion core, designator |
| Advanced chip | fabricator: basic chip + wafer + gold ingot + rare earth dust | tier II missiles, radar, launcher, handheld radar, implant |
| Military chip | fabricator: advanced chip + 2 rare earth dust + diamond + lithium dust | tier III missiles (crafted ones too now: see below), silo controller |
| Battery | assembler: lithium ingot + copper ingot + redstone | handheld radar, guided rockets |
| Airframe | assembler: 4 aluminum ingots + iron ingot | missiles (one per tier level), rockets |

**Recipe changes (D72):**

- **Missiles:** built in the assembler. Tier I: an airframe, 2 rocket fuel, a basic chip and the payload; tier II: 2 airframes, 4 rocket fuel, an advanced chip and a richer payload. Payloads I / II: explosive 2 / 4 TNT; breaching a diamond and TNT / 2 netherite scrap and 2 TNT; EMP 2 lightning rods / 2 lightning rods and 2 redstone blocks; incendiary 4 blaze powder and TNT / 4 blaze rods and 2 TNT. Tier III stays crate-only (D57) unless the owner decides otherwise.
- **Missile blocks** (assembler): designator = spyglass, basic chip, 2 aluminum, redstone; launcher = 4 aluminum, 2 obsidian, an iron block, an advanced chip, a dispenser; 8 silo casing = 4 obsidian, 4 aluminum, an iron block; silo hatch = 2 casing, an iron trapdoor, 2 aluminum, redstone; silo controller = 2 casing, a launcher, a military chip, a redstone block; radar = 4 aluminum, a compass, 2 amethyst, an advanced chip, a redstone block; interceptor = 4 aluminum, 2 firework rockets, a dispenser, an advanced chip.
- **Machines** (crafting table): crusher from iron, a grindstone, copper and a piston (no aluminum, so it can come first); chemical mixer from aluminum, glass, a cauldron, iron and a piston; circuit fabricator from aluminum, redstone, comparators, a diamond and iron; assembler from aluminum, pistons, a crafting table, iron and a redstone block.
- **Weapons, every part** (assembler): barrel = 2 aluminum + iron + copper; grip = 2 leather + aluminum; fusion core = basic chip + amethyst + aluminum; ammo (8) = copper ingot + niter + redstone, shapeless (niter replaces the gunpowder). Weapon recipes themselves stay as they are (built from the parts).

### 6b.6 Rocket launcher (D61–D65)

- A seventh weapon, **legendary-crate-only** (like the sniper and gatling: 25% of legendary crates, on top of their other loot). Its **rockets are their own ammo item** (D65, amends D20), built in the assembler from an airframe, rocket fuel, a basic chip, a battery and TNT → 4 rockets.
- **Lock-on** (D61): aim down sights at a player within 96 blocks with a clear line of sight. After 1.5 s of keeping them near the crosshair, the shooter hears a lock tone and sees a lock marker on them. Losing them for more than 0.5 s resets the lock.
- Fire with a lock: the rocket homes on that player, flying 1.5 blocks/tick and turning at most 6° per tick, so dodging sideways at the last moment or getting behind cover beats it. Without a lock it flies straight. It lives for 10 s.
- **Blast:** entities only, no block damage (D64). 16 damage at the center, falling off over 3 blocks, our weapon damage type with the shooter as the causing entity (so friendly fire rules apply).
- Magazine 1, reload 3 s. Workbench fields: reload speed, range (lock range), sight. Third-person pose like the other weapons, held on the shoulder.

### 6b.7 Lock warning gear (D62, D63)

The target only gets a warning if they have one of these:

- **Tactical helmet** (D63): diamond-level protection (3 armor, 2 toughness), crafted from aluminum, diamonds and an advanced chip. It has one implant field at the weapon workbench.
- **Threat-warning implant:** installed into the tactical helmet at the workbench. With it, the wearer hears a warning tone and sees a direction marker while someone is locking on, and a faster tone once they are locked.
- **Handheld radar:** an FE item (100k FE, a battery and an advanced chip), charged at the charging station. Held in either hand it gives the same warning and uses 10 FE/t while held. Empty, it does nothing.

Players without the gear get no warning: the rocket's sound and smoke trail are all they have.

### 6b.8 Interceptor changes (D66)

- The interceptor **always needs FE and ammo**: one **interceptor rocket** per shot (assembler: airframe, rocket fuel, basic chip → 4 rockets).
- It gets a menu: a rocket slot (stacks of 16, hoppers can fill it), the energy bar and its status. Right click opens the menu instead of the status message.

### 6b.9 Missile icons (D68)

The 12 missile inventory icons are redrawn at **64×64**: a missile body at an angle with a large, readable type symbol on it (explosive: blast, breaching: drill tip, EMP: lightning, incendiary: flame), the tier color on the nose and fins, and tier marks (I / II / III stripes). Only the inventory icons change; the 3D model stays.

---

## 7. Open decisions

**Blocks** = the milestone (§8) that needs the answer.

### Weapons

None open. All v1 weapon decisions are in §2 (D19–D29); armor against weapons is OD-30 under "Later".

### Supply crate

None open (OD-19 → D43).

### Server and faction

None open (OD-23 → D46).

### Phase 2

None open (OD-24 to OD-28 → D47–D59).

### Phase 3

None open (D60–D74). Details in §6b are *provisional* (§10).

### Later

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-29 | Weapon stats source | Code table / server config / datapack JSON | Code table now; datapack later if admins want tuning | Later |
| OD-30 | Armor against weapons (owner wants it later; D9 had armor out of scope) | Armor that reduces or removes weapon knockback / also damage / enchantment instead of armor | — | Later |

---

## 8. Milestones

### Phase 1

The supply crate comes first because it is self-contained and proves out the shared plumbing: SavedData, chunk tickets, loot tables, commands and config.

| Milestone | Deliverables |
|---|---|
| **M0 Setup** ✅ *done 2026-10-06* | OD-01 and OD-02 answered. Project created from the 26.3 MDK; NeoForge bumped and pinned; mod ID and package set; `.gitignore`; git repo. Builds, and runs on both `runClient` and `runServer`. |
| **M1 Supply crate MVP** ✅ *done 2026-10-08* | Crate block and block entity, three rarity loot tables (placeholder vanilla loot), falling-crate entity, scheduler with SavedData, location picking, announcements and countdown, chunk ticket, despawn, admin commands, server config. |
| **M2 Weapon framework** ✅ *done 2026-10-08* | Weapon base item, central stats record, ammo item, ammo/reload data components, reload keybind and packet, projectile entity, custom damage type, placeholder sounds, ammo HUD. One working test gun. |
| **M3 Weapon roster** ✅ *done 2026-10-08* | v1 roster (OD-03) with models and textures, recipes (OD-09), balance pass (OD-06). |
| **M4 Weapon modification** ✅ *done 2026-10-08* | Weapon workbench block, menu and screen; 21 upgrade parts; installed-parts data component and effective stats; visible parts on the weapon models; tooltips; game tests (§4.5, D30–D36). |
| **M5 Aiming and controls** ✅ *done 2026-10-08* | Left click fires (trigger packet, server-side firing), right click aims down sights for every weapon, aimed spread and movement, sight upgrade with three tiers and reticles, sniper scope on right click with 3× / 6× (D37–D42). |
| **M6 Integration & polish** ✅ *done 2026-10-08, released as v0.1.0* | Weapons and upgrade parts in crate loot (D43, D44), config documentation (`docs/SERVER.md`, README), version 0.1.0, two-player playtest on a dedicated server, release v0.1.0. No claims integration (D45). |

### Phase 2

| Milestone | Deliverables |
|---|---|
| **M7 Missile core** ✅ *done 2026-10-09* | Designator, missile launcher block and menu, explosive missiles I–II, flight (launch, cruise in SavedData, descent, chunk tickets), block damage rules (§6.6), raid windows, cooldown, optional FE, `[missiles]` config, admin commands, game tests. |
| **M8 Missile types and silo** ✅ *done 2026-10-09* | Breaching, EMP and incendiary missiles; tier III; missile silo multiblock; recipes; tier III missiles in legendary crates. |
| **M9 Counterplay** ✅ *done 2026-10-16* | Radar block with team alerts and launch area, interceptor block, shooting missiles down. |
| **M10 Integration & release** ✅ *done 2026-10-16, released as v0.2.0* | Models and textures pass, `docs/SERVER.md`, two-player playtest on a dedicated server, release v0.2.0. |

### Phase 3

| Milestone | Deliverables |
|---|---|
| **M11 Resources** ✅ *done 2026-10-16* | Bauxite, niter, rare earth and lithium ores (stone and deepslate), raw items, dusts, ingots, world generation with biome limits, `c:` tags, smelting, ore textures, game tests. |
| **M12 Power** ✅ *done 2026-10-16* | Combustion generator, solar panel, FE cable network, charging station, `requirePower` default true, game tests. |
| **M13 Machines and materials** ✅ *implemented, waiting for the owner's test* | Crusher, chemical mixer, circuit fabricator, assembler with their recipe types; silicon, wafers, chips I–III, rocket fuel, battery, airframe; new recipes for missiles, missile blocks and weapon parts and ammo (D72). |
| **M14 Rocket launcher** | Rocket launcher weapon and model, rockets, lock-on, guided rocket, tactical helmet with implant, handheld radar, lock warning HUD and sounds. |
| **M15 Integration & release** | Interceptor ammo and menu, 64×64 missile icons, crate loot, `docs/SERVER.md`, two-player playtest, release v0.3.0. |

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

### Learned while building M1 (verified against 26.3 sources)

- **Config types:** FML 12 has `LOCAL`, `CLIENT`, `SYNCED` and `STARTUP`. There is no `SERVER` type any more; `SYNCED` is the per-server one.
- **SavedData** is codec-based (`SavedDataType` with an `Identifier`, a constructor and a `Codec`). Server-wide data lives in `MinecraftServer.getDataStorage()`, not in the overworld's storage. A namespaced ID is saved as `data/<namespace>/<path>.dat`.
- **Loot tables** are a reloadable registry. `LootTableProvider` is a `SingleRegistryBootstrap`; generate it with `GatherDataEvent.createReloadableRegistryObjects(new RegistrySetBuilder().add(Registries.LOOT_TABLE, ...))`. Counts use `ContextIntProviders`, and the JSON uses `modifier` instead of `functions`.
- **Chunk tickets:** `TicketController.forceChunk` uses vanilla's `FORCED` ticket (persists, loads and simulates). Mod tickets don't show up in `/forceload query`.
- **Dedicated server:** spawn chunks are no longer kept loaded, and the server stops ticking after `pause-when-empty-seconds` (default 60) with nobody online.
- **Renamed or moved:** `PushReaction.IMMOVEABLE` (no `BLOCK`), `BlockEntityTypes` / `EntityTypes` hold the vanilla constants, the world spawn is `server.getRespawnData().globalPos()`, `Player.playNotifySound` is gone (send a `ClientboundSoundPacket`), and `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` is permission level 2.
### Learned while building M2 (verified against 26.3 sources)

- **Keys:** the keyboard input type is `InputConstants.Type.KEYBOARD` (no `KEYSYM`). Key categories are `KeyMapping.Category` records, registered with `RegisterKeyMappingsEvent.registerCategory`; the label key is `key.category.<namespace>.<path>`.
- **HUD:** `GuiLayer` draws to a `GuiGraphicsExtractor` (`text(...)`, colors are ARGB and need the alpha byte). The F1 state is `Minecraft.gui.hud.isHidden()`, and mod layers are not hidden automatically.
- **Items:** the `minecraft:use_effects` component (`UseEffects`) controls the slowdown while an item is in use (default 20% speed, no sprinting). Data component types can be built with `ignoreSwapAnimation()`. NeoForge registers data component types before items, so `Item.Properties.component(...)` can use our own types.
- **Damage:** the `minecraft:bypasses_cooldown` damage-type tag exists and is checked in `LivingEntity`. `ServerPlayer.hurtServer` checks team friendly fire against `source.getEntity()`, so the shooter as causing entity is enough.
- **GameTests** are registry entries: functions in `Registries.TEST_FUNCTION` (a `DeferredRegister`), tests via `RegisterGameTestsEvent.registerTest` with a `FunctionGameTestInstance` and `TestData`. `minecraft:empty` is a 1×1×1 structure. Only the structure's chunks tick, so force-load any extra chunks a test uses (the runner un-forces them after the batch). Use `makeMockServerPlayer(GameType)`; mock players aren't ticked by the server.
- **Sounds:** our `sounds.json` can point at vanilla sound files, which is how the placeholder weapon sounds work.
### Learned while building M3 (verified against 26.3 sources)

- **Startup config:** the `STARTUP` config type loads as soon as it's registered, before datapacks. That makes it the right place for anything a recipe or loot condition reads. `SYNCED` only loads in `ServerLifecycleHooks.handleServerAboutToStart`, after the datapacks.
- **Recipes** are a reloadable registry like loot tables: generate them with `RecipeProvider.asBootstrap(...)` in `createReloadableRegistryObjects`. NeoForge conditions still work: `output.withConditions(ICondition)` writes `neoforge:conditions`, and custom conditions are a `MapCodec` registered in `NeoForgeRegistries.Keys.CONDITION_CODECS`.
- **Armor piercing:** `LivingIncomingDamageEvent` is the only event early enough to change the armor reduction, via `DamageContainer.addModifier(Reduction.ARMOR, ...)`. Vanilla armor: `realArmor = clamp(armor − damage / (2 + toughness / 4), 0.2 × armor, 20)`, reduction `realArmor / 25`. A newly equipped armor piece only counts after a tick.
- **Item models:** cuboid `elements` allow free rotation on any axis and `light_emission` from 0 to 15. The model must fit in −16..32 on every axis. Display transforms apply translation (in pixels), then rotation, then scale. First person looks along −z. A custom arm pose comes from `IClientItemExtensions.getArmPose`, registered with `RegisterClientExtensionsEvent`. `CROSSBOW_HOLD` turns the holding arm 0.3 rad inward (`AnimationUtils.animateCrossbowHold`).
- **Client input:** `ComputeFovModifierEvent` for zoom and `CalculatePlayerTurnEvent` for mouse sensitivity. Turn speed is `(sensitivity × 0.6 + 0.2)³ × 8` (`MouseHandler`). `RenderGuiLayerEvent.Pre` (cancel by layer name, e.g. `VanillaGuiLayers.CROSSHAIR`) hides vanilla HUD parts, and cancelling `RenderHandEvent` hides the first-person hand and item.
- **GameTests run concurrently in one world.** Give each test a backstop wall and filter projectiles by owner, or stray bullets from one test hit another test's targets.
### Learned while building M4 (verified against 26.3 sources)

- **Sounds:** a variable-range `SoundEvent` played with volume ≤ 1 is only sent to players within **16 blocks** (`SoundEvent.getRange`). The client fades a sound out linearly to `attenuation_distance` × max(volume, 1); a volume below 1 makes it quieter but does not shorten that distance. For a per-shot range, send your own payload and play a `SimpleSoundInstance` whose `getOrResolve` swaps in a `Sound` with a different attenuation distance.
- **Server-to-client payloads** can be registered in common code with `playToClient(type, codec)` and no handler; the client adds the handler with `RegisterClientPayloadHandlersEvent`. `PacketDistributor.sendToPlayersNear` sends by distance.
- **Menus and screens:** `MenuType` registers with `new MenuType<>(Constructor::new, FeatureFlags.VANILLA_SET)`, screens with `RegisterMenuScreensEvent`. Screens draw in `extractBackground` / `extractLabels` / `extractTooltip` (the 26.2 GUI rename). A slot's empty icon is `Slot.getNoItemIcon()`, a sprite under `textures/gui/sprites/`. A container can be a live view of an item's component (`workbench.UpgradeFields`): vanilla's click code only uses `setItem` / `removeItem`, so `getItem` may return a fresh copy.
- **Item model properties:** custom range-dispatch properties register with `RegisterRangeSelectItemModelPropertyEvent`, which also fires in the data run, so `ItemModelUtils.rangeSelect` / `composite` / `EmptyModel.Unbaked` can generate item definitions that use them.
- **Item names:** `Item.Properties` sets `item_name` after the component initializer, so a colored name needs an override of `Item.getName(ItemStack)`.
- **Config lists:** `defineList(path, default, newElement, validator, ModConfigSpec.Range.of(n, n))` checks the list length. Values can be read before the config is loaded through `getDefault()` (check `SPEC.isLoaded()`).
- **Block loot:** `BlockLootSubProvider` needs `getKnownBlocks()` overridden to the mod's own blocks. Its constructor takes the feature flags (`FeatureFlags.REGISTRY.allFlags()`).
- **GameTests:** `makeMockServerPlayer` players have no connection, so anything that sends them packets (e.g. putting items back in their inventory) crashes. Mark them removed to take the "drop at their feet" path instead.
### Learned while building M5 (verified against 26.3 sources)

- **Attack button:** `InputEvent.InteractionKeyMappingTriggered` (with `isAttack()`) fires for both the first click and continued mining; cancel it and `setSwingHand(false)` to stop hitting, mining and swinging. Read the key with `options.keyAttack.isDown()`. While the player uses an item, vanilla already skips mining and swallows attack clicks.
- **First-person item position:** `IClientItemExtensions.applyForgeHandTransform` replaces vanilla's arm transform and swing (return true). The item is then drawn at hand + display translation + scale × (model point / 16 − 0.5); the hand renders with a fixed 70° FOV, unaffected by zoom.
- **Renamed in 26.x:** `PlayerRenderState` is in `net.minecraft.client.renderer.state.level`; the open screen is `Minecraft.gui.screen()`.
- **Client resources:** reload listeners register through `AddClientReloadListenersEvent.addListener(id, listener)`.
- **Non-saved player state:** a NeoForge `AttachmentType` without a serializer lives on the entity and is gone after death or logout.
- **Never compare item stacks by identity on the client.** When the server changes a stack's components (every shot, every loaded round), the client gets a new `ItemStack` object, applied between render frames. `player.getUseItem()` only catches up on the next tick (`LivingEntity.updatingUsingItem`, which keeps the use going while the item type matches). Check the hand and the item type instead.
- If your knowledge stops around 1.21.1, also skim the 1.21.2 – 1.21.11 primers. Rendering, item models and GUIs changed a lot in that span.

### Learned while building M7 (verified against 26.3 sources)

- **Energy** moved to NeoForge's transfer API: `Capabilities.Energy.BLOCK` is a `BlockCapability<EnergyHandler, Direction>` (no `IEnergyStorage`). `SimpleEnergyHandler` is a ready buffer (`serialize` / `deserialize` to a `ValueOutput` child), and changes go through transactions (`Transaction.openRoot()`); `onEnergyChanged` fires on commit. Register with `RegisterCapabilitiesEvent.registerBlockEntity`.
- **Explosions:** `ServerExplosion` keeps its ray casting private. For a custom blast, compute the blocks yourself, but build a `ServerExplosion` anyway and pass it to `EventHooks.onExplosionStart` (cancellable) and `onExplosionDetonate` (mods edit the block and entity lists), so claim mods see it. `BlockState.onBlockExploded` sets air (container block entities spill their contents in `preRemoveSideEffects`, shulker boxes don't). Players get explosion knockback and effects through `ClientboundExplodePacket`.
- **Menus with extra data:** `IMenuTypeExtension.create((id, inventory, buffer) -> ...)` and `player.openMenu(provider, pos)` (writes the position). Menu data slots (`ContainerData`) are synced as **shorts**: clamp or scale larger numbers. Menu buttons: `clickMenuButton` on both sides, the screen sends `gameMode.handleInventoryButtonClick`.
- **Items in entity renderers:** `ItemModelResolver.updateForNonLiving(state, stack, ItemDisplayContext.NONE, entity)` and `ItemStackRenderState.submit`. An item definition can pick a flat model for inventories and a 3D one elsewhere with `ItemModelGenerators.createFlatModelDispatch`. Rotate a `PoseStack` with `rotateDegrees(Axis.XP, angle)`.
- **Entities are drawn only if their render section is compiled and visible** (`LevelExtractor.isEntityVisible`), on top of `shouldRenderAtSqrDistance`. Thin models are hard to see beyond ~30 blocks; trails of long-lived particles (`CAMPFIRE_COSY_SMOKE`) with `alwaysShow` carry the visibility.
- **Heightmaps** of unloaded chunks read as the world's bottom: load the chunk (`level.getChunk`) before `getHeightmapPos`.
- **Player messages:** `sendOverlayMessage` (action bar) and `sendSystemMessage`.
- **Config:** `defineListAllowEmpty` for lists that may be empty. Two config options with the same leaf name share one translation key (`fusionfactions.configuration.<name>`).
- **GameTests:** the framework's own test block sits at the test's origin; keep things that fall from the sky off that column.

### Learned while building M10 (verified against 26.3 sources)

- **Block models can only use block-atlas textures** (`textures/block/…`); a block model part with an item texture is rejected (`SimpleModelWrapper.findNonBlockSprites`). The block and item atlases are separate since 1.21.11.
- **Models drawn by a renderer:** register them with `ModelEvent.RegisterStandalone` (`StandaloneModelKey` plus `SimpleUnbakedStandaloneModel.simpleModelWrapper(id)` for a `BlockStateModelPart`), get them with `ModelManager.getStandaloneModel(key)`, and draw them with `SubmitNodeCollector.submitBlockModel(pose, Sheets.cutoutBlockItemSheet(), parts, tints, light, overlay, outline)`. This works in entity and block entity renderers.
- **Block entity renderers** have the same extract/submit split as entity renderers (`createRenderState`, `extractRenderState`, `submit`), register with `EntityRenderersEvent.RegisterRenderers.registerBlockEntityRenderer`. Animation state that the server doesn't need (the radar dish's angle) can live in a field of the block entity, updated by a client ticker.
- **Blocks smaller than a cube** need `Properties.noOcclusion()` and a `getShape` override.

### Learned while building M11 (verified against 26.3 sources)

- **Configured features are gone as a separate type:** `Registries.FEATURE` (`worldgen/feature`) holds the configured feature itself, e.g. `new OreFeature(List<BlockReplacement>, size)`; `PlacedFeature` points at a `Holder<Feature>`. Generate them, the placed features and NeoForge biome modifiers (`BiomeModifiers.AddFeaturesBiomeModifier`) with `GatherDataEvent.createWorldRegistryObjects`.
- **Render layers come from the texture:** each quad picks solid, cutout or translucent from its sprite's transparency. A two-element model (vanilla stone or deepslate, then a transparent overlay) makes an ore without copying vanilla's textures, like the grass block's side overlay.
- **Recipe IDs:** `RecipeBuilder.save(output, String)` parses the id in the `minecraft` namespace, which the mod-filtered datagen drops. Save with a `ResourceKey` in our namespace.
- NeoForge applies biome modifiers before game tests run, so `Biome.getGenerationSettings().features()` shows the added features.

### Learned while building M12 (verified against 26.3 sources)

- **Fuel is a data component:** `DataComponents.COOKING_FUEL` (`CookingFuel`, with a `ResolvableInt` burn time resolved against a loot context, `BaseContainerBlockEntity.getLootContext(level, stack)`). There is no fuel map any more; an item becomes fuel by having the component.
- **Transaction-aware state:** extend `SnapshotJournal` and call `updateSnapshots(transaction)` before changing the state (the cable network's per-tick throughput), so simulated transfers roll back.
- **Block capabilities without a block entity:** `RegisterCapabilitiesEvent.registerBlock(capability, (level, pos, state, blockEntity, side) -> …, blocks)`. Item energy: `ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM)`.
- **Time of day lives in world clocks** (`server.clockManager()`); `ServerLevel.setDayTime` is gone. `/time set noon` still works.
- `canSeeSky` is sky light 15, which the light engine updates a tick or so after a block changes.
- `BlockModelGenerators.createFurnace(block, TexturedModel.ORIENTABLE_ONLY_TOP)` makes a facing block with a lit front (`_front`, `_front_on`, `_side`, `_top`); `MultiPartGenerator` with `condition().term(...)` makes pipe-like blocks.



### Learned while building M13 (verified against 26.3 sources)

- **Recipes:** `RecipeSerializer` is a record of a `MapCodec` and a `StreamCodec`; `RecipeType.simple(id)` makes a type (register both with `DeferredRegister`). A recipe returns an `ItemStackTemplate` result (`create()` makes the stack), a `PlacementInfo` (`NOT_PLACEABLE`) and a `RecipeBookCategory`, which is a registry entry (`Registries.RECIPE_BOOK_CATEGORY`). Machines look recipes up with `RecipeManager.createCheck(type)`; `ServerLevel.recipeAccess()` is the recipe manager.
- NeoForge's `SizedIngredient` (`NESTED_CODEC`: `{"ingredient": …, "count": n}`) is an ingredient with a count. `RecipeOutput.accept(key, recipe, null)` saves a recipe without an unlock advancement.
- The data run deletes generated files it no longer writes (old crafting recipes vanish when they move to a machine).

### Learned while bumping NeoForge to 26.3.0.86 (2026-10-17)

- **Datapack registry entries** (loot tables, recipes, world gen, biome modifiers) are no longer generated from `GatherDataEvent.createReloadableRegistryObjects` / `createWorldRegistryObjects`. They come from `GatherDataRegistryEntriesEvent` (mod bus, fired before the providers), which has `add(registry, bootstrap)`, `lootTable(...)` and `recipe(...)` and sorts the layers itself. Only the mods given with `--mod` are written.
- **Recipes on the client:** since 1.21.2 recipes stay on the server. A mod asks NeoForge to send a recipe type with `OnDatapackSyncEvent.sendRecipes(type)`; the client gets them in `RecipesReceivedEvent` (also in single player). JEI listens to that event with the lowest priority and only starts afterwards, so a normal listener can store the recipes for a JEI plugin; JEI's API doesn't expose them.
- **JEI 31 on 26.3:** categories extend `AbstractRecipeCategory` with an `IRecipeHolderType.create(vanillaType)`; slots are placed at the item position (the background is drawn 1 px outside, like vanilla slots); `addAnimatedRecipeArrowWidget` replaces `addAnimatedRecipeArrow`. `Ingredient.items()` is deprecated in favour of `SlotDisplay`, which has no count, so a counted input still needs `items()`.
- **Shared block and item tags** moved to `Tags.BlockItems` (`BlockItemTagId`, `.item()` / `.block()`); the old `Tags.Items.SANDS`, `OBSIDIANS`, `STORAGE_BLOCKS_*`, `GLASS_*` are deprecated for removal in 26.4.

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
| 2026-10-06 | OD-01 → D10 | Name Fusion Factions, mod ID `fusionfactions`, package `com.cornspies.fusionfactions` | Owner |
| 2026-10-06 | OD-02 → D11 | License MIT | Owner |
| 2026-10-07 | OD-15 → D12 | Fixed drop interval, default 90 min, with a minimum player count | Owner |
| 2026-10-07 | D12 detail | The countdown to the next drop only runs while at least `minPlayersOnline` (default 2) players are online, and it is saved across restarts. Server downtime and empty hours don't count. *Provisional.* | Agent |
| 2026-10-07 | OD-16 → D13 | Instant open | Owner |
| 2026-10-07 | OD-14 → D14 | Exact coordinates, configurable | Owner |
| 2026-10-07 | OD-17 → D15 | 3 tiers common / rare / legendary, 60 / 30 / 10, configurable | Owner |
| 2026-10-07 | OD-18 → D16 | ±250 blocks around world spawn, configurable | Owner |
| 2026-10-07 | OD-20 → D17 | Crates are unbreakable until emptied or despawned | Owner |
| 2026-10-07 | OD-21 → D18 | Overworld only, configurable | Owner |
| 2026-10-08 | M1 details | The crate falls from 100 blocks above the landing spot at 4 blocks/s with a smoke trail. Reminders come at 30 s and 10 s. Extra announcements when a crate is looted or despawns. A despawned or admin-removed crate deletes its contents instead of spilling them. Crates have no item form. *Provisional.* | Agent |
| 2026-10-08 | OD-04 → D19 | Projectile entities for all v1 weapons | Owner |
| 2026-10-08 | OD-05 → D20 | One shared ammo item | Owner |
| 2026-10-08 | OD-07 → D21 | Bypass the hurt cooldown for our damage type only | Owner |
| 2026-10-08 | OD-12 → D22 | Server authority plus a cosmetic client tracer | Owner |
| 2026-10-08 | M2 weapon handling | Test pistol: 5 HP per hit, 12 rounds, 4 ticks between shots, 6 ticks per round to reload (placeholder until OD-06). New weapons come loaded. Firing an empty magazine starts a reload. Holding the trigger doesn't interrupt a reload, pulling it again does, and switching items cancels it (rounds already loaded stay). Creative players reload for free, like bows. *Provisional.* | Agent |
| 2026-10-08 | M2 bullet damage | The bullet damage type is also tagged `minecraft:is_projectile`, so Projectile Protection reduces it. Vanilla knockback is kept for now; with the i-frame bypass, rapid fire knocks targets back a lot. Revisit both in the M3 balance pass (OD-06). Bullets are not saved with chunks. *Provisional.* | Agent |
| 2026-10-08 | Bullets underwater | Bullets keep flying in water but slow down like arrows (0.6 drag per tick), about 9 blocks of range. Same damage on a hit. Once spent (slower than 0.25 blocks/tick) they don't hurt anything; stronger water gravity makes them curve down and sink at about 2 blocks/s until they touch the ground and disappear (at most 30 s). Underwater the shooter sees a bubble puff and the bullet's dense bubble trail instead of the instant spark tracer (agent detail after owner feedback). | Owner |
| 2026-10-08 | Water exit | A bullet that has been in water keeps losing speed after it leaves the water (10% per tick) and uses the heavy water gravity, so it drops in a short arc. The more water it went through, the shorter the arc (about 15 / 9 / 5 blocks after 1 / 2 / 3 ticks in water). | Owner |
| 2026-10-08 | Knockback | Vanilla knockback on bullet hits is fine for now. Knockback becomes a per-weapon value in the M3 balance pass. The owner also wants armor that reduces or removes knockback later (OD-30). | Owner |
| 2026-10-08 | M1, M2 done | Two-player test on a dedicated server passed for both checklists. | Owner |
| 2026-10-08 | OD-03 → D23 | Six weapons: pistol, auto pistol, SMG, shotgun, sniper, gatling | Owner |
| 2026-10-08 | OD-06 → D24 | Rebalance around vanilla armor, with partial armor piercing | Owner |
| 2026-10-08 | OD-11 → D25 | Headshot bonus | Owner |
| 2026-10-08 | OD-08 → D26 | Low tier craftable, heavy weapons crate-only with a config toggle | Owner |
| 2026-10-08 | D26 detail | "Heavy weapons" read as the sniper **and** the gatling (as in Fusion Warfare). *Provisional.* | Agent |
| 2026-10-08 | OD-09 → D27 | Vanilla items plus own parts | Owner |
| 2026-10-08 | OD-10 → D28 | Hip-fire, sniper zoom | Owner |
| 2026-10-08 | OD-13 → D29 | Static 3D models | Owner |
| 2026-10-08 | M3 balance | First balance pass as in §4.3. Targets: craftable weapons kill full diamond in about 1.2–1.5 s of sustained hits, and the sniper kills in two body shots through any armor. Bullets ignore 30% of the armor's protection, the sniper 50%; enchantments count in full. Shotgun shells and sniper rounds cost 2 ammo each. The gatling needs 20 ticks of held trigger before it fires. *Provisional.* | Agent |
| 2026-10-08 | M3 headshots | Headshots do 1.5× damage. A hit counts as one if it lands no more than 0.3 blocks below the target's eyes. The shooter hears a hit marker on every hit, with a separate headshot sound. *Provisional.* | Agent |
| 2026-10-08 | M3 knockback | Per weapon: sniper 0.8, pistol 0.4, auto pistol 0.25, shotgun 0.2 per pellet, SMG 0.15, gatling 0.05 (a vanilla melee hit is 0.4). *Provisional.* | Agent |
| 2026-10-08 | M3 recipes | Parts and recipes as in §4.3. The auto pistol and SMG are upgrades of the previous weapon. The heavy-weapon recipe switch is a startup config (`heavyWeaponRecipes`, off), not a synced one, because recipes load before the synced config. So it is per server, not per world, and needs a restart. *Provisional.* | Agent |
| 2026-10-08 | M3 aiming and models | Zoom is 3× and 6× on the Z key, first person only, with slower mouse movement. The scope overlay was first the vanilla spyglass; after owner feedback it is a typical shooter sniper scope (§4.2), with its texture from `tools/scope_overlay.py`. Weapons are held in the crossbow aiming pose in third person. Models come from `tools/weapon_models.py`; sounds are vanilla placeholders. *Provisional.* | Agent |
| 2026-10-08 | M3 done | Two-player test on a dedicated server passed. The owner approved the sniper scope HUD. | Owner |
| 2026-10-08 | D30–D36 | Weapon modification: workbench with weapon-dependent upgrade fields; tiered pure upgrades (I/II/III); seven upgrade types, no damage; crate-only parts; visible on the model; removable and free to install; built as the new M4 before crate loot and release | Owner |
| 2026-10-08 | §4.5 details | Field layout, per-weapon fields, per-tier effects, one item per type and tier, workbench that stores nothing. *Provisional* until the M4 balance and playtest. | Agent |
| 2026-10-08 | Upgrade tuning | Owner asked for easy tuning of the values and of which weapon accepts which upgrade. They are in the synced server config (`[weaponUpgrades]`), so no build is needed. Weapon base stats stay in code (OD-29 unchanged). | Owner / Agent |
| 2026-10-08 | M4 details | Workbench recipe: iron, fusion core, barrel, crafting table, grip (`IFI / BCG / III`). Parts stack to 16. Taking out a magazine upgrade drops the rounds that no longer fit. Range raises bullet speed only (range follows). Part names are colored by tier. Parts of a type the weapon no longer accepts stay on it, without effect. *Provisional.* | Agent |
| 2026-10-08 | Gunshot range | Found while building the suppressor: gunshots only reached players within 16 blocks (vanilla's limit for normal sounds). Shots now carry 48 blocks (sniper 96), the distances `sounds.json` already intended, and fade out there. *Provisional.* | Agent |
| 2026-10-08 | M4 done | Two-player test passed. | Owner |
| 2026-10-08 | D37–D41 | Left click fires; right click aims down sights for every weapon; aiming halves spread and slows; tiered sights as an upgrade field (not on the sniper); sniper scope zoom on the zoom key and mouse wheel | Owner |
| 2026-10-08 | M5 details | Aiming takes 4 ticks; without a sight it zooms 1.1×; while aiming players move at 80% of walking speed. The sight field takes the top of the grid and accuracy moves to the top-left field, as a stabilizer under the barrel. Every gun but the sniper got iron sights. A shot kicks the weapon back a little. *Provisional.* | Agent |
| 2026-10-08 | Sight pictures | Owner feedback: the sights looked blocky, covered more than they helped, had small reticles, and the magnified one needed a scope view. The owner kept the red dot / holographic / 2× lineup. When fully aimed, sights now show a HUD sight picture instead of the weapon model; the 2× optic gets a scope view. The owner pointed to Flan's Mod Ultimate 2's scopes as a reference. Its code and assets are CC BY-NC-SA 3.0, which is incompatible with our MIT license (D11), so nothing was copied: we only looked at how its sights (aim through the model) and scopes (full-screen overlay) work. *Provisional.* | Owner / Agent |
| 2026-10-08 | M5 done | Two-player test passed, including the new sight pictures. M4 and M5 merged into `main`. | Owner |
| 2026-10-08 | OD-19 → D43, D44 | Crates hold only mod items; heavy weapons are a chance in legendary crates | Owner |
| 2026-10-08 | OD-22 → D45 | No claims integration for now | Owner |
| 2026-10-08 | M6 loot details | Pool layout and odds as in §5.6 (50% heavy weapon in legendary crates, 50% sidearm in common, 70% weapon in rare). Rare crates may add a tier I part, legendary crates hold tier II parts besides tier III. *Provisional.* | Agent |
| 2026-10-08 | M6 done, v0.1.0 | Two-player playtest passed. Phase 1 released as v0.1.0. | Owner |
| 2026-10-08 | OD-23 → D46 | Raid windows, configurable; empty schedule = always | Owner |
| 2026-10-08 | OD-24 → D47 | Soft Mekanism integration (FE + `c:` tags) | Owner |
| 2026-10-08 | OD-25 → D48 | Explosive, breaching, EMP and incendiary missiles, tiers I–III | Owner |
| 2026-10-08 | OD-26 → D49, D50 | Designator item; range by tier | Owner |
| 2026-10-08 | OD-27 → D51, D52 | Blast resistance counts, bedrock and a tag are immune; only containers drop | Owner |
| 2026-10-08 | OD-28 → D53, D54 | Cooldown, interceptor, shooting down, and a radar that reports the launch area to the placer's team | Owner |
| 2026-10-08 | D55–D59 | Launcher for I–II and silo for III; up-then-down flight; tier III crate-only; anyone can use the blocks; FE with `requirePower` off by default | Owner |
| 2026-10-08 | Phase 2 spec | Numbers and details in §6.4–§6.10 (radii, flight speed, cooldowns, health, silo shape, power costs). *Provisional* until the Phase 2 playtests. | Agent |
| 2026-10-09 | M9 details | Radar: scans once a second; alerts the owner's team in chat with a sound, once per missile; launch area as in §6.9, from a random offset seeded by the flight. Interceptor: looks for targets every 2 ticks; its rocket flies 4 blocks per tick straight at the missile's middle and gives up after 4 s; rockets on their way count against the missile's health so interceptors don't overkill. Missile health 10 / 20 / 40, hitbox 0.8 × 5.4 blocks. Radar and interceptor have a 100k FE buffer each. Recipes: radar from amethyst, a compass, iron, a fusion core and a redstone block; interceptor from barrels, a firework rocket, iron, a fusion core and a dispenser. *Provisional.* | Agent |
| 2026-10-09 | M7, M8 done | Owner tested missiles, the silo and its formed look, and approved both milestones. Merged into `main`. | Owner |
| 2026-10-09 | Silo look, launcher screen | Owner asked for a formation animation and a darker, connected look for a complete silo, and for the readout text to stay inside its panel. Each formed block gets a block state `part` (its place in the 3×3×4 silo) with its own model; the controller checks its silo every 10 ticks and forms it over about 1.5 s. The readout lays out target, distance and status line by line in four lines and drops the distance if a long status needs the room. *Provisional.* | Owner / Agent |
| 2026-10-09 | M8 details | Incendiary missiles: entities within the fire radius burn for 8 s; fire lands on about a third of the spots in the radius where it can stay, after the blast, and those spots go through the explosion event too. EMP missiles drain every FE handler in the radius from every side and break nothing; missile blocks hit by one show "shut down" with the seconds left. The silo is checked whenever its menu is open and before each launch, not on every block change. Recipes: each type's tier I is the explosive recipe with its payload (diamond, lightning rod, blaze powder), tier II adds a richer payload (netherite scrap, redstone blocks, blaze rods); silo casing (4) from iron and obsidian, hatch from casing, an iron trapdoor, iron and redstone, controller from casing, a missile launcher, barrels, a fusion core and a redstone block. A quarter of the legendary crates hold one tier III missile. Missile types are told apart by the band on the model and icon: dark, violet, cyan, orange. *Provisional.* | Agent |
| 2026-10-08 | M7 details | The launcher needs open sky above it. Blast model: rays lose 1.3 strength per block of air and a quarter of a block's blast resistance per block (vanilla loses about 3.3 times the resistance), so explosive missiles reach about 5 / 7 / 9 blocks in the open and dig a few blocks into stone. Entity damage uses vanilla's formula over twice the reach; the damage type is in `minecraft:is_explosion`, so Blast Protection counts. Hard blocks: blast resistance 600 and up. Blasts break at most 1024 blocks per tick. The flying missile is drawn 3 times the item's size (about 5.5 blocks long) with a continuous smoke trail. Missiles stack to 4. Recipes: designator from a spyglass, redstone, iron and a fusion core; launcher from barrels, obsidian, iron blocks, a fusion core and a dispenser; explosive missile I from TNT, iron, gunpowder and a fusion core; II from missile I, TNT, diamonds and a fusion core. *Provisional.* | Agent |
| 2026-10-16 | M9 done | Owner tested the radar, interceptor and shooting missiles down, and approved the milestone. Merged into `main`. | Owner |
| 2026-10-16 | M10 models and textures | The launcher, plain silo, radar and interceptor textures get the bevelled look of the crates and the workbench. The radar and the interceptor become 3D models (`tools/machine_models.py`), smaller than a block; the radar's dish turns while it is online. Interceptor rockets are drawn as small rockets instead of only their trail. Missile, weapon and crate models are unchanged. *Provisional.* | Agent |
| 2026-10-16 | M10 done, v0.2.0 | Owner playtested Phase 2 and approved it, together with glowing night sights and accents on the weapons and redrawn upgrade icons. Phase 2 released as v0.2.0. | Owner |
| 2026-10-16 | D60–D74 | Phase 3: guided rocket launcher, lock warning gear, interceptor ammo and power, generators and cables, four ores, four machines, three chip tiers, materials in missiles, missile blocks and weapons, 64×64 missile icons, industry first | Owner |
| 2026-10-16 | Phase 3 spec | Material chain, numbers and placement in §6b (ore heights and biomes, generator output, cable throughput, machine recipes and times, chip recipes, rocket stats, helmet stats and recipe, handheld radar capacity, rocket launcher in 25% of legendary crates, tier III missiles stay crate-only). *Provisional.* | Agent |
| 2026-10-16 | M11 details | Ore amounts tuned by counting a freshly generated area (per chunk: bauxite 40–80, niter 15–35, rare earth about 5 in mountains, lithium about 10 in deserts). Ores are as hard as iron ore; niter ore drops experience like redstone ore. Ingots smelt from raw items, ores and dusts (faster in a blast furnace). The ores' `c:` tags follow the material (`c:ores/aluminum` for bauxite, `c:dusts/niter` and `c:dusts/saltpeter` for niter). Ores are drawn as an overlay on vanilla stone and deepslate (`tools/resource_textures.py`). *Provisional.* | Agent |
| 2026-10-16 | M11 done | Owner tested the ores, drops and amounts and approved them. | Owner |
| 2026-10-16 | M12 details | Generator buffer 100k FE, pauses when full, pushes 2000 FE/t; solar panel 6 px high, half output in rain; cable networks split FE evenly and never send it back; charging station buffer 50k; recipes as in §6b.3; the power blocks share a machine base (menu, readout, energy gauge) that M13's machines will use. Game tests run with the dev config, so they now charge the blocks they fire and pass with `requirePower` on or off. *Provisional.* | Agent |
| 2026-10-16 | M12 done | Owner tested generators, cables, the solar panel and power requirements, and approved them. | Owner |
| 2026-10-16 | M13 details | Recipes and times as in §6b.4–§6b.5. A machine recipe must use every item in the input slots, so recipes are told apart by item kinds (the barrel gained a copper ingot to differ from the airframe). Rocket fuel burns 6400 ticks in furnaces and generators. Machines have a 50k FE buffer and pause without power. All four machines share one block entity and one screen, laid out per machine. *Provisional.* | Agent |
| 2026-10-17 | NeoForge 26.3.0.86 | Owner asked for the newest NeoForge that JEI supports: `26.3.0.86` (JEI 31.15 accepts any 26.3 build). The mod now needs 26.3.0.86 or newer. No Mekanism build for 26.3 exists yet (§6.3). | Owner |
| 2026-10-17 | JEI plugin | Owner asked for one. The server always sends the machine recipes to clients (small; needed for any recipe viewer). JEI shows one category per machine, laid out like its screen, with FE/t, time and total FE; the machines are the catalysts; clicking a machine's arrow shows its recipes; the transfer button fills the first input slots in order (needs JEI on the server). | Owner / Agent |
| 2026-10-17 | JEI in dev | JEI (31.15.0.75 for 26.3, from maven.blamejared.com) runs in the dev client, server, game tests and data run, so recipes can be looked up while testing. Only as `localRuntime`: no dependency, not in the jar. Our machine recipes don't show in it until a JEI plugin is written. | Owner |

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
