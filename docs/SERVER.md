# Fusion Factions: server guide

For server admins running Fusion Factions 0.1.0 on Minecraft 26.3 with NeoForge 26.3.0.52-beta.

Install the mod jar on the server **and** on every client. It has no other mod dependencies.

## Config files

| File | What's in it | When changes apply |
|---|---|---|
| `config/fusionfactions-synced.toml` | Supply crates and weapon upgrades | Restart the server. Clients receive the values when they join. |
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

### Heavy weapon recipes: `fusionfactions-startup.toml`

`heavyWeaponRecipes = false` (default): the sniper rifle and gatling gun can't be crafted and only come from supply crates. Set it to `true` to enable their recipes.

## Crate loot

Each rarity has a loot table: `fusionfactions:supply_crate/common`, `/rare` and `/legendary`. To change what crates hold, override them in a datapack at `data/fusionfactions/loot_table/supply_crate/<rarity>.json`. The default tables are a good starting point; they're in the mod jar at the same path.

Default contents:

| Rarity | Always | Chances |
|---|---|---|
| Common | 16–32 ammo, 1–2 stacks of weapon parts, 1–2 tier I upgrades | 50%: pistol or auto pistol |
| Rare | 32–48 ammo, 2–3 stacks of weapon parts, 1–2 tier II upgrades | 50%: an extra tier I upgrade. 70%: SMG, shotgun or auto pistol |
| Legendary | 48–64 ammo, 1–2 tier III upgrades, 1–2 tier II upgrades | 50%: sniper rifle or gatling gun. 33%: SMG or shotgun |

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

## Things players should know

- **Left click** fires, **right click** (hold) aims down sights, **R** reloads, **Z** or the mouse wheel switches the sniper's zoom while scoped. Keys can be changed under Controls → Fusion Factions.
- With a gun in hand, left click doesn't hit or break blocks.
- Weapon damage counts as the shooter's attack, so vanilla team friendly-fire settings and PvP-protection mods apply.
