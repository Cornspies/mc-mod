# Fusion Factions: server guide

For server admins running Fusion Factions 0.2.0 on Minecraft 26.3 with NeoForge 26.3.0.52-beta.

Install the mod jar on the server **and** on every client. It has no other mod dependencies.

## Config files

| File | What's in it | When changes apply |
|---|---|---|
| `config/fusionfactions-synced.toml` | Supply crates, weapon upgrades and missiles | Restart the server. Clients receive the values when they join. |
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
| `requirePower` | `false` | Whether launchers, silos, radars and interceptors need FE from another mod (Mekanism or any mod with FE cables). Minecraft itself has no power source, so leave it off without such a mod. |
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
| `interceptor.energyPerShot` | `10000` | FE per interceptor shot, if `requirePower` is on. |

Missile blasts break blocks without dropping them; containers spill their contents. Blocks in the block tag `fusionfactions:missile_immune` never break (bedrock, barriers, portals, command blocks and supply crates by default). Add your own with a datapack at `data/fusionfactions/tags/block/missile_immune.json`. Protection and claim mods that stop explosions also stop missiles.

Tier III missiles only come from legendary supply crates and need a **missile silo**: a 3×3 shaft, 4 blocks deep. Bottom layer: 9 silo casing. The two layers above: 8 casing around an empty middle. Top layer: 8 casing around a silo hatch. Replace any one casing of the three upper layers with the silo controller, which has the launch menu. The sky above the hatch must be open.

**Counterplay.** A radar alerts the team of the player who placed it (their vanilla `/team`, or just them without one) about every missile that enters its range, with a launch area that contains the launch spot. It gives a redstone signal while it sees missiles. Interceptors shoot down missiles coming down near them, and players can shoot them down with weapons.

Missiles in flight are saved with the world and keep flying after a restart. Their launch and target areas are kept loaded, so they work with nobody nearby.

### Heavy weapon recipes: `fusionfactions-startup.toml`

`heavyWeaponRecipes = false` (default): the sniper rifle and gatling gun can't be crafted and only come from supply crates. Set it to `true` to enable their recipes.

## Crate loot

Each rarity has a loot table: `fusionfactions:supply_crate/common`, `/rare` and `/legendary`. To change what crates hold, override them in a datapack at `data/fusionfactions/loot_table/supply_crate/<rarity>.json`. The default tables are a good starting point; they're in the mod jar at the same path.

Default contents:

| Rarity | Always | Chances |
|---|---|---|
| Common | 16–32 ammo, 1–2 stacks of weapon parts, 1–2 tier I upgrades | 50%: pistol or auto pistol |
| Rare | 32–48 ammo, 2–3 stacks of weapon parts, 1–2 tier II upgrades | 50%: an extra tier I upgrade. 70%: SMG, shotgun or auto pistol |
| Legendary | 48–64 ammo, 1–2 tier III upgrades, 1–2 tier II upgrades | 50%: sniper rifle or gatling gun. 33%: SMG or shotgun. 25%: a tier III missile |

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
- Defense: a radar alerts your `/team` in chat when a missile comes into its range, with the area it was launched from; its dish stops turning while it is shut down. Interceptors shoot down missiles coming down near them (right click one to see whether it's ready), and a missile on its way down can be shot with any weapon.
