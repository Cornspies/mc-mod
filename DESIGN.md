# Fusion Factions — Design Document

> **Mod ID:** `fusionfactions` · **Package:** `com.cornspies.fusionfactions` · **License:** MIT
> **Status:** Draft v0.1 · **Last updated:** 2026-10-08
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

---

## 3. Technical baseline (adopted)

### 3.1 Toolchain

- Start from the official NeoForge MDK for 26.3 (ModDevGradle): <https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle>.
  - As of 2026-09-27 the MDK ships ModDevGradle `2.0.147`, Gradle `9.2.1`, Java toolchain `25`, and `neo_version=26.3.0.23-beta`.
- At project start, **bump `neo_version` to the newest `26.3.0.x` build**. Mekanism builds against `26.3.0.52-beta` as of 2026-10-06.
  - Done in M0: pinned to **`26.3.0.52-beta`**, the newest build on 2026-10-06.
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
  - Sights show their reticle on the HUD at the middle of the screen, inside the sight's frame: a red dot (I), a red ring with a dot (II), an amber chevron with stadia lines (III).
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

**Parts** are items, one per type and tier (8 × 3 = 24), named like "Magazine Upgrade II"; the sights are "Red Dot Sight", "Holographic Sight" and "2x Optic". They come only from supply crates (D33). Proposal for OD-19: tier I mostly in common crates, tier II in rare, tier III in legendary.

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
   - Reject water, lava and unsafe spots, and claimed land if claims integration is enabled (OD-22).
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

### Weapons

None open. All v1 weapon decisions are in §2 (D19–D29); armor against weapons is OD-30 under "Later".

### Supply crate

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-19 | Loot contents per tier | — | Define once weapons and upgrade parts exist. Parts: tier I in common, II in rare, III in legendary crates (§4.5) | M6 |

### Server and faction

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| OD-22 | Team and claims mod | Open Parties and Claims (OPAC) / other / none | OPAC as an optional integration. NeoForge 26.3 beta builds exist (latest 0.32.8, 2026-10-05) | M6 |
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
| OD-30 | Armor against weapons (owner wants it later; D9 had armor out of scope) | Armor that reduces or removes weapon knockback / also damage / enchantment instead of armor | — | Later |

---

## 8. Milestones (Phase 1)

The supply crate comes first because it is self-contained and proves out the shared plumbing: SavedData, chunk tickets, loot tables, commands and config.

| Milestone | Deliverables |
|---|---|
| **M0 Setup** ✅ *done 2026-10-06* | OD-01 and OD-02 answered. Project created from the 26.3 MDK; NeoForge bumped and pinned; mod ID and package set; `.gitignore`; git repo. Builds, and runs on both `runClient` and `runServer`. |
| **M1 Supply crate MVP** ✅ *done 2026-10-08* | Crate block and block entity, three rarity loot tables (placeholder vanilla loot), falling-crate entity, scheduler with SavedData, location picking, announcements and countdown, chunk ticket, despawn, admin commands, server config. |
| **M2 Weapon framework** ✅ *done 2026-10-08* | Weapon base item, central stats record, ammo item, ammo/reload data components, reload keybind and packet, projectile entity, custom damage type, placeholder sounds, ammo HUD. One working test gun. |
| **M3 Weapon roster** ✅ *done 2026-10-08* | v1 roster (OD-03) with models and textures, recipes (OD-09), balance pass (OD-06). |
| **M4 Weapon modification** ✅ *done 2026-10-08* | Weapon workbench block, menu and screen; 21 upgrade parts; installed-parts data component and effective stats; visible parts on the weapon models; tooltips; game tests (§4.5, D30–D36). |
| **M5 Aiming and controls** 🟡 *implemented 2026-10-08; two-player test pending* | Left click fires (trigger packet, server-side firing), right click aims down sights for every weapon, aimed spread and movement, sight upgrade with three tiers and reticles, sniper scope on right click with 3× / 6× (D37–D42). |
| **M6 Integration & polish** | Weapons and upgrade parts in crate loot (OD-19), optional OPAC integration (OD-22), config documentation, two-player playtest on a dedicated server, release v0.1.0. |

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
