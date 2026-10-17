# Fusion Factions: server guide

For server admins running Fusion Factions 0.3.0 on Minecraft 26.3 with NeoForge 26.3.0.86 or newer.

Install the mod jar on the server **and** on every client. It has no other mod dependencies.

**JEI** is optional. With it, players see the machine recipes (crushing, mixing, fabricating, assembling) in JEI, with the machine as the place to make them. Its transfer button, which moves the ingredients into a machine, needs JEI on the server too.

## Updating from 0.2.0

- **NeoForge 26.3.0.86 or newer** is needed, on the server and on every client (0.2.0 ran on 26.3.0.52-beta).
- **Power:** an existing config keeps `requirePower = false`. Set it to `true` to make launchers, silos, radars and the new machines need FE (see `[missiles]`). Interceptors always need FE now.
- **Interceptors need ammo:** every shot uses one interceptor rocket from its menu (right click it). Existing interceptors stay quiet until someone loads them.
- **Recipes moved into machines:** weapon parts (barrel, grip, fusion core), missiles and the missile blocks are built in the assembler; the old crafting recipes are gone. Ammo is now copper, niter and redstone. Players who had parts keep them.
- **Ores only generate in new chunks** (see Ores below).
- New config sections `[power]` and `[rocketLauncher]` are added with their defaults when the server first starts with 0.3.0.

## Config files

| File | What's in it | When changes apply |
|---|---|---|
| `config/fusionfactions-synced.toml` | Supply crates, weapon upgrades, missiles, power and the rocket launcher | Restart the server. Clients receive the values when they join. |
| `<world>/syncedconfig/fusionfactions-synced.toml` | Optional per-world copy of the file above; overrides it for that world | Restart the server. |
| `config/fusionfactions-startup.toml` | Heavy weapon recipes | Restart the server. Also applies to the whole server, not per world. |

Every option is commented in the file itself. Delete a file (or a section) to get the defaults back.

### Supply crates: `[supplyCrates]`

| Option | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Drop crates automatically on a timer. The admin commands work either way. |
| `intervalMinutes` | `90` | Minutes between automatic drops. |
| `minPlayersOnline` | `2` | The timer only runs while at least this many players are online. Downtime and empty hours don't count. |
| `zoneMode` | `SPAWN` | `SPAWN`: crates land within `zoneRadius` of the world spawn. `CENTER`: around `zoneCenterX` / `zoneCenterZ`. |
| `zoneCenterX`, `zoneCenterZ` | `0`, `0` | Zone center for `CENTER` mode. |
| `zoneRadius` | `250` | Crates land at most this many blocks from the center, on X and on Z (a square). |
| `countdownSeconds` | `60` | Time between the announcement and the crate starting to fall. |
| `despawnMinutes` | `30` | A landed crate that nobody empties disappears after this long, with its contents. |
| `announcePrecision` | `EXACT` | `EXACT` coordinates, `ROUNDED` to 50 blocks, or `DIRECTION` (compass direction and rough distance from spawn). |
| `dimension` | `minecraft:overworld` | Dimension crates drop in. |
| `rarityWeights.common` / `.rare` / `.legendary` | `60` / `30` / `10` | Relative chance of each crate rarity. `0` disables a rarity. |

Crates are unbreakable until emptied or despawned. While a crate is falling or waiting, its chunk is kept loaded.

### Weapon upgrades: `[weaponUpgrades]`

`[weaponUpgrades.tiers]` sets the effect of tiers I, II and III of each upgrade type, as a list of three numbers:

| Type | Default | Unit |
|---|---|---|
| `penetration` | `[10, 20, 30]` | Extra armor piercing, percentage points (max 100%) |
| `magazine` | `[20, 40, 60]` | Extra rounds, percent of the magazine |
| `noise` | `[25, 50, 75]` | Shorter hearing range of shots, percent |
| `recoil` | `[20, 35, 50]` | Less recoil, percent |
| `reload` | `[15, 30, 45]` | Less reload time per round, percent |
| `accuracy` | `[20, 35, 50]` | Less spread, percent |
| `range` | `[15, 30, 50]` | Faster bullets (so more range, less drop), percent |
| `sight` | `[25, 50, 100]` | Extra zoom while aiming, percent (25 = 1.25×) |

Reductions are capped at 95%.

`[weaponUpgrades.fields]` lists, per weapon, which upgrade types it accepts: these are the open fields around it in the weapon workbench. Remove a type to disallow it. A part that is already installed stays on the weapon, shows "no effect on this server" and can still be taken out.

**Updating from an older version:** a config written by an older version keeps its old field lists. When a version adds a new upgrade type, add it to the lists yourself, or delete the `[weaponUpgrades.fields]` section to get the new defaults.

### Missiles: `[missiles]`

New in 0.2.0. A config file from 0.1.0 gets this section with its defaults when the server first starts with 0.2.0.

| Option | Default | Meaning |
|---|---|---|
| `raidWindows` | `[]` | When missiles can be launched. Entries like `"SAT 18:00-23:00"`, `"MON-FRI 20:00-22:00"`, `"SAT,SUN 14:00-02:00"` or `"DAILY 20:00-22:00"`. An end before the start runs past midnight. Empty: any time. Missiles already in the air still land. |
| `raidTimeZone` | `""` | Time zone for the raid windows, e.g. `Europe/Berlin`. Empty: the server's time zone. |
| `requirePower` | `true` | Whether launchers, silos, radars and machines need FE, from our generators or any other mod's. Interceptors always need it. **A config file from 0.2.0 keeps `false`**: set it to `true` by hand to make power matter. |
| `range` | `[500, 1000, 2000]` | Maximum distance to the target for missile tiers I, II and III, in blocks. |
| `minRange` | `64` | Minimum distance to the target, in blocks. |
| `flightSpeed` | `25.0` | Blocks per second on the way to the target. Flight time is this plus a 3 s climb and a 4 s descent. |
| `launcherCooldownSeconds` | `60` | Time a launcher needs between launches. |
| `launchEnergy` | `[50000, 100000, 200000]` | FE per launch by tier, if `requirePower` is on. |
| `designatorRange` | `256` | How far away the target designator can mark a block. |
| `hardBlockResistance` | `600.0` | Blocks with at least this blast resistance (obsidian is 1200) stop missile blasts. |
| `explosivePower` | `[6, 9, 12]` | Blast power of explosive missiles by tier (TNT is 4). |
| `siloCooldownSeconds` | `120` | Time a missile silo needs between launches. |
| `breachingPower` | `[4, 5, 6]` | Blast power of breaching missiles by tier. They also break hard blocks like obsidian. |
| `empRadius` | `[8, 12, 16]` | Radius in which EMP missiles drain all FE, by tier. |
| `empDisableSeconds` | `[30, 60, 90]` | How long EMP missiles shut down missile launchers, silos, radars and interceptors in their radius, by tier. |
| `incendiaryPower` | `[2, 3, 4]` | Blast power of incendiary missiles by tier. |
| `incendiaryFireRadius` | `[6, 8, 10]` | Radius incendiary missiles set on fire, by tier. |
| `missileHealth` | `[10, 20, 40]` | Health of a missile on its way down, by tier. Bullets and interceptors damage it. |
| `radar.range` | `256` | How far a radar sees missiles (horizontal). |
| `radar.launchAreaSize` | `100` | Half the size of the square a radar reports as the launch area. |
| `radar.energyPerTick` | `20` | FE a radar uses per tick, if `requirePower` is on. |
| `interceptor.range` | `96` | How far an interceptor reaches. It needs a clear line of sight. |
| `interceptor.cooldownTicks` | `40` | Ticks between two interceptor shots. |
| `interceptor.damage` | `20.0` | Damage of one interceptor hit. |
| `interceptor.energyPerShot` | `10000` | FE per interceptor shot. Interceptors always need it, and one interceptor rocket per shot. |

Missile blasts break blocks without dropping them; containers spill their contents. Blocks in the block tag `fusionfactions:missile_immune` never break (bedrock, barriers, portals, command blocks and supply crates by default). Add your own with a datapack at `data/fusionfactions/tags/block/missile_immune.json`. Protection and claim mods that stop explosions also stop missiles.

Tier III missiles only come from legendary supply crates and need a **missile silo**: a 3×3 shaft, 4 blocks deep. Bottom layer: 9 silo casing. The two layers above: 8 casing around an empty middle. Top layer: 8 casing around a silo hatch. Replace any one casing of the three upper layers with the silo controller, which has the launch menu. The sky above the hatch must be open.

**Counterplay.** A radar alerts the team of the player who placed it (their vanilla `/team`, or just them without one) about every missile that enters its range, with a launch area that contains the launch spot. It gives a redstone signal while it sees missiles. Interceptors shoot down missiles coming down near them, and players can shoot them down with weapons.

Missiles in flight are saved with the world and keep flying after a restart. Their launch and target areas are kept loaded, so they work with nobody nearby.

### Power: `[power]`

New in 0.3.0. Generators, cables and the charging station.

| Option | Default | Meaning |
|---|---|---|
| `generatorOutput` | `40` | FE per tick a combustion generator makes while it burns. Fuel lasts as long as in a furnace. |
| `solarOutput` | `8` | FE per tick a solar panel makes under open sky by day. Half in rain, nothing at night. |
| `cableThroughput` | `2000` | FE per tick one network of connected cables carries in total. |
| `chargingRate` | `500` | FE per tick the charging station puts into the item in its slot. |

### Rocket launcher: `[rocketLauncher]`

New in 0.3.0. How the rocket launcher locks on, how its rockets steer and what the handheld radar uses. The launcher's own numbers (16 damage at the blast's centre, one rocket per load, 3 s reload, rockets at 1.5 blocks per tick for 10 s) are fixed like every weapon's.

| Option | Default | Meaning |
|---|---|---|
| `lockTicks` | `30` | Ticks (20 = 1 s) a player has to stay near the crosshair while aiming before the launcher locks on. |
| `lockGraceTicks` | `10` | Ticks the target may leave the crosshair or go behind cover before the lock is lost. |
| `lockRange` | `96` | Blocks within which the launcher can lock on. The range upgrade raises it. |
| `lockConeDegrees` | `5.0` | How far from the crosshair a player may be and still count as aimed at. |
| `turnDegrees` | `6.0` | How far a guided rocket can turn per tick. Lower makes it easier to dodge. |
| `blastRadius` | `3.0` | Blocks over which a rocket's damage falls off to nothing. Rockets never break blocks. |
| `radarDrain` | `10` | FE per tick a handheld radar uses while held. |

### Machines and recipes

New in 0.3.0. Missiles, the missile blocks and the weapons' parts (barrel, grip, fusion core) are no longer crafted: they come out of four machines that need power (crusher, chemical mixer, circuit fabricator, assembler). Their recipes are datapack recipes of the types `fusionfactions:crushing`, `mixing`, `fabricating` and `assembling`; replace or add them with a datapack in `data/<namespace>/recipe/`, for example:

```json
{
  "type": "fusionfactions:crushing",
  "inputs": [{ "ingredient": "fusionfactions:raw_bauxite", "count": 1 }],
  "result": { "id": "fusionfactions:aluminum_dust", "count": 2 },
  "ticks": 100,
  "energy_per_tick": 20
}
```

A recipe matches when the machine's input slots hold at least its inputs and nothing else. `ticks` and `energy_per_tick` are optional (each machine has a default).

### Ores

New in 0.3.0. Four ores generate in the overworld:

| Ore | Where | Mined with |
|---|---|---|
| Bauxite (aluminum) | everywhere, y −16 to 112, about as common as copper | stone pickaxe |
| Niter | everywhere, y 0 to 80 | stone pickaxe |
| Rare earth | only in mountain biomes, y −32 to 160 | iron pickaxe |
| Lithium | only in desert and badlands biomes, y −32 to 64 | iron pickaxe |

**Ores only generate in new chunks.** In a world that already exists, players find them only in areas nobody has visited yet. To get them near spawn, pre-generate or reset those regions.

The biomes for each ore are the biome tags `fusionfactions:has_ore/bauxite`, `…/niter`, `…/rare_earth` and `…/lithium`. Change them, or replace the features in `data/fusionfactions/worldgen/`, with a datapack. The items carry common `c:` tags (`c:ingots/aluminum`, `c:dusts/niter`, …), so other mods' aluminum works in our recipes and ours in theirs.

### Heavy weapon recipes: `fusionfactions-startup.toml`

`heavyWeaponRecipes = false` (default): the sniper rifle and gatling gun can't be crafted and only come from supply crates. Set it to `true` to enable their recipes.

## Crate loot

Each rarity has a loot table: `fusionfactions:supply_crate/common`, `/rare` and `/legendary`. To change what crates hold, override them in a datapack at `data/fusionfactions/loot_table/supply_crate/<rarity>.json`. The default tables are a good starting point; they're in the mod jar at the same path.

Default contents:

| Rarity | Always | Chances |
|---|---|---|
| Common | 16–32 ammo, 1–2 stacks of weapon parts, 1–2 tier I upgrades | 50%: pistol or auto pistol |
| Rare | 32–48 ammo, 2–3 stacks of weapon parts, 1–2 tier II upgrades | 50%: an extra tier I upgrade. 70%: SMG, shotgun or auto pistol |
| Legendary | 48–64 ammo, 1–2 tier III upgrades, 1–2 tier II upgrades | 50%: sniper rifle or gatling gun. 33%: SMG or shotgun. 25%: a tier III missile. 25%: the rocket launcher with 4–8 rockets |

Weapons in crates come loaded.

## Admin commands

Permission level 2 (operators).

| Command | What it does |
|---|---|
| `/fusionfactions drop now` | Drops a crate of a random rarity at a random spot in the zone, after the countdown. |
| `/fusionfactions drop now <common\|rare\|legendary\|random> [<x> <z>]` | Same, with a chosen rarity and, optionally, a position. |
| `/fusionfactions drop next` | Shows when the next automatic drop is due. |
| `/fusionfactions drop list` | Lists active crates. |
| `/fusionfactions drop clear` | Removes all active crates (their contents are deleted). |
| `/fusionfactions missile launch <type> <tier> <x> <z> [<from x> <from z>]` | Launches a missile at the surface of a column, from another column or from where you are. Ignores raid windows, range and power. |
| `/fusionfactions missile list` | Lists missiles in flight with their target and time to impact. |
| `/fusionfactions missile clear [<id>]` | Removes all missiles in flight, or one, without detonating them. |

## Things players should know

- **Left click** fires, **right click** (hold) aims down sights, **R** reloads, **Z** or the mouse wheel switches the sniper's zoom while scoped. Keys can be changed under Controls → Fusion Factions.
- With a gun in hand, left click doesn't hit or break blocks.
- Weapon damage counts as the shooter's attack, so vanilla team friendly-fire settings and PvP-protection mods apply.
- Missiles: mark a target with the target designator (right click a block, up to 256 blocks away), put it and a missile into a missile launcher under open sky, and press Launch. Anyone can use any launcher. Explosive missiles blow craters, breaching missiles also break obsidian, EMP missiles drain power and shut down launchers, radars and interceptors, incendiary missiles set fires.
- Power: combustion generators burn anything a furnace burns, solar panels work by day. FE cables carry the power to launchers, radars, interceptors and machines, split evenly between them. Any mod's FE cables work too.
- Production: ores go through the crusher (two dusts each), quartz becomes silicon, the circuit fabricator makes wafers and the three chips, the chemical mixer makes rocket fuel (which also burns well in generators), and the assembler builds airframes, batteries, weapon parts, missile blocks and missiles. Ammo is copper, niter and redstone at a crafting table.
- Rocket launcher (from legendary crates): hold right click on a player to lock on (a tone, then brackets that close in and turn red), then left click. The rocket homes on them but turns slowly, so a sharp dodge or cover can beat it. It hurts players and mobs, never blocks. Rockets are its ammo, built in the assembler.
- Lock warnings: only a tactical helmet with a threat-warning implant (installed at the weapon workbench, like an upgrade part) or a charged handheld radar held in either hand warns you that someone is locking on, has locked on, or that a rocket is coming, with tones and an arrow pointing at the threat. The radar is charged at the charging station. Without the gear, all you get is the rocket's sound and smoke.
- Defense: a radar alerts your `/team` in chat when a missile comes into its range, with the area it was launched from; its dish stops turning while it is shut down. Interceptors shoot down missiles coming down near them, one interceptor rocket per shot (built in the assembler; load them in the interceptor's menu or with a hopper), and a missile on its way down can be shot with any weapon.
