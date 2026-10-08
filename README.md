# Fusion Factions

A NeoForge mod for Minecraft 26.3 that adds sci-fi weapons, weapon upgrades and falling supply crates for faction PvP servers.

## Features

- **Six weapons:** pistol, auto pistol, SMG, shotgun, sniper rifle and gatling gun. All shots are fired by the server; headshots do extra damage, and bullets pierce part of the armor.
- **Aim down sights** on every weapon. The sniper has a 3× / 6× scope.
- **Weapon workbench:** install upgrade parts in three tiers: penetration, magazine, suppressor, recoil, reload speed, accuracy, range and sights (red dot, holographic, 2× optic). Installed parts show on the gun.
- **Supply crates** fall from the sky on a timer, announced to the whole server, in three rarities. They hold ammo, weapon parts, upgrade parts and weapons; the sniper rifle and gatling gun only come from crates.

## Controls

| Key | Action |
|---|---|
| Left click | Fire |
| Right click (hold) | Aim down sights |
| R | Reload |
| Z or mouse wheel (while scoped) | Switch the sniper scope between 3× and 6× |

Keys can be changed under Controls → Fusion Factions; firing and aiming follow the attack and use keys.

## For server admins

Install the jar on the server and on every client. Config options, crate loot and admin commands are described in [docs/SERVER.md](docs/SERVER.md).

## Development

- Design and roadmap: [DESIGN.md](DESIGN.md)
- Build: `./gradlew build` (Java 25). The jar ends up in `build/libs/`.
- License: [MIT](LICENSE). `TEMPLATE_LICENSE.txt` covers the files taken from the NeoForge MDK.
