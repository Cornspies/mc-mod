# CLAUDE.md

Minecraft mod for a faction PvP server: sci-fi weapons and falling supply crates (Phase 1), a missile launcher (Phase 2).
The full design, including what is decided and what is still open, is in **@DESIGN.md**. Read it before starting a task.

## Current status

- **Phase 1, milestone M0 (setup): not started.**
- Blocking open decisions before scaffolding: **OD-01** (mod name / mod ID / package) and **OD-02** (license). Ask the owner for these first.

## Version pins (do not change without asking)

- Minecraft **26.3**
- NeoForge **26.3.0.x-beta**, pinned in `gradle.properties`
- Java **25**
- ModDevGradle, from the NeoForge MDK: <https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle>

Don't bump NeoForge or add dependencies without the owner's OK.

## Commands

On Windows use `gradlew.bat` instead of `./gradlew`.

- `./gradlew build` — compile and package
- `./gradlew runClient` — start a dev client
- `./gradlew runServer` — start a dedicated dev server (must work before a feature counts as done)
- `./gradlew runData` — data generation (loot tables, models, lang) into `src/generated/resources`
- `./gradlew runGameTestServer` — run game tests

## Rules

1. **Open decisions.** Never decide items in DESIGN.md §7 silently.
   - If one blocks your current task, stop and ask.
   - Otherwise, implement the listed recommendation as a default (behind a constant or config option) and add a row marked *provisional* to the decision log in DESIGN.md §10.
2. **Don't trust your memory of the APIs.** Your training data may predate Minecraft 26.x.
   - Before using a class or method, verify it against the actual 26.3 sources in the dev environment (the game is unobfuscated, so names are readable).
   - Other references: the Mekanism `26.3` branch and the NeoForge migration primers (DESIGN.md §9).
   - Prefer searching the sources over guessing. Known traps: `ResourceLocation` is now `Identifier`; 26.3 uses SDL key codes; the loot table JSON format changed in 26.3.
3. **The server is authoritative** (DESIGN.md §3.2).
   - Client→server packets carry intent only.
   - The server re-validates held item, ammo, cooldowns, distances and ownership.
   - Weapon damage uses our custom damage type, with the shooting player as the causing entity.
4. **Client code** lives only in the `client` package and is reached only from the `Dist.CLIENT` entry point. Never reference client classes from common code.
5. **Definition of done** is DESIGN.md §3.5. At minimum: `build` passes, `runServer` starts, and the feature works with two players.
6. **Fusion Warfare** (GPLv2) is inspiration only. Don't copy its code or assets.
7. **Keep DESIGN.md current.** When the owner decides something, move it from §7 to §2 or §10. Mark milestones as done in §8.
8. Make small, focused commits with clear messages, one logical step at a time.
