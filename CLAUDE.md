# ExyliaClasses

HCF PvP classes picked by the armour a player wears, free and open source (GPL-3.0), public at `Exylia-Plugins/ExyliaClasses`.

## Releasing

- Every finished change is released, without asking and without `/exylia-release`: bump `version` in `build.gradle` (patch for fixes, minor for features, major for breaking commands or config), commit `chore: release <version>`, tag that commit `v<version>`, and push the branch and the tag.
- The release workflow runs only on `v*` tags and expects the tag to match the `build.gradle` version; it publishes the GitHub release and the fixed-name `ExyliaClasses.jar` that servers auto-update from. A push to `main` without a tag only builds.

## ExyliaLib

- Always the newest: `build.gradle` resolves the latest GitHub release (or a newer one in mavenLocal) at build time, and that version becomes plugin.yml's `exylia-lib`. There is no pinned version to bump; `-PexyliaLibVersion` pins one for a single build.
- The bootstrap installer (`bootstrap/ExyliaLibInstaller`, run from `ExyliaClassesPlugin` when ExyliaLib is missing) downloads the latest release, falling back to the built-against one.
- Updates of this plugin come from ExyliaLib's `PluginUpdater` (`exylia-updates` in plugin.yml, the fixed-name `ExyliaClasses.jar` on every release).

## Layout

- `ExyliaClassesPlugin` is the Bukkit entry point and stays free of ExyliaLib types; `ExyliaClasses` is the core it starts.
- One class is one file in `classes/`; the six defaults ship in `src/main/resources/classes/`.
