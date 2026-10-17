# Fusion Factions

A NeoForge mod for Minecraft 26.3 that adds sci-fi weapons, weapon upgrades, falling supply crates, missiles and the industry to build them for faction PvP servers.

## Features

- **Seven weapons:** pistol, auto pistol, SMG, shotgun, sniper rifle, gatling gun and a guided rocket launcher. All shots are fired by the server; headshots do extra damage, and bullets pierce part of the armor.
- **Aim down sights** on every weapon. The sniper has a 3× / 6× scope.
- **Weapon workbench:** install upgrade parts in three tiers: penetration, magazine, suppressor, recoil, reload speed, accuracy, range and sights (red dot, holographic, 2× optic). Installed parts show on the gun.
- **Supply crates** fall from the sky on a timer, announced to the whole server, in three rarities. They hold ammo, weapon parts, upgrade parts and weapons; the sniper rifle, gatling gun and rocket launcher only come from crates.
- **Rocket launcher:** hold aim on a player to lock on; the rocket homes on them but can be dodged. Only players with a tactical helmet and threat-warning implant, or a charged handheld radar, are warned.
- **Missiles** for raiding other teams' bases: explosive, breaching (breaks obsidian), EMP and incendiary, in three tiers. Mark the target with a designator and fire from a missile launcher (tiers I–II) or a missile silo you build (tier III, from legendary crates). Launches can be limited to raid windows.
- **Counterplay:** a radar alerts your team about incoming missiles and where they were launched from, interceptors shoot them down, and players can shoot them out of the sky.
- **Industry:** bauxite, niter, rare earth and lithium ores (the rare ones only in some biomes), combustion generators, solar panels and FE cables, and four machines (crusher, chemical mixer, circuit fabricator, assembler) that turn them into aluminum, rocket fuel, chips, batteries and airframes. Missiles, missile blocks and weapon parts are built from them.
- Everything runs on FE, so Mekanism and other tech mods' power works too. With [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) installed, the machine recipes show up there.

## Controls

| Key | Action |
|---|---|
| Left click | Fire |
| Right click (hold) | Aim down sights |
| R | Reload |
| Z or mouse wheel (while scoped) | Switch the sniper scope between 3× and 6× |

Keys can be changed under Controls → Fusion Factions; firing and aiming follow the attack and use keys.

## For server admins

Install the jar on the server and on every client (NeoForge 26.3.0.86 or newer). Config options, crate loot, recipes and admin commands are described in [docs/SERVER.md](docs/SERVER.md).

## Development

- Design and roadmap: [DESIGN.md](DESIGN.md)
- Build: `./gradlew build` (Java 25). The jar ends up in `build/libs/`.
- License: [MIT](LICENSE). `TEMPLATE_LICENSE.txt` covers the files taken from the NeoForge MDK.
