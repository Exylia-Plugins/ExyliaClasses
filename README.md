# ExyliaClasses

Free, open-source **HCF PvP classes** for Paper and Folia. A player becomes a class by wearing its armour: after a short warmup they get its passive effects, abilities, hold effects and weapons. Six classes ship (Archer, Bard, Miner, Rogue, Sorcerer, Tank), and every class is a YAML file you can edit, copy or delete.

Built on [ExyliaLib](https://github.com/DiGround-s/ExyliaLib).

## Requirements

- Paper or Folia 1.21+, Java 21
- [PacketEvents](https://modrinth.com/plugin/packetevents)
- [ExyliaLib](https://github.com/DiGround-s/ExyliaLib/releases). If it is missing, the plugin downloads the latest release on first start and asks for a restart.

Optional: PlaceholderAPI, to expose the placeholders to other plugins.

## Installing

1. Put `ExyliaClasses.jar` (from the [latest release](https://github.com/Exylia-Plugins/ExyliaClasses/releases/latest)), PacketEvents and ExyliaLib in `plugins/`.
2. Start the server. It writes `config.yml`, `lang/<language>/messages.yml` and the six class files under `classes/`.
3. Put on a full set of a class's armour. Edit the class files and run `/classes reload` to apply changes.

## Updates

ExyliaLib keeps both itself and this plugin up to date from their GitHub releases: a new version is downloaded in the background and applied on the next restart. Turn it off or tune it under `plugin-updates` in `plugins/ExyliaLib/config.yml`; `/exylialib updates` shows what is pending.

## Documentation

Classes, abilities, energy, commands, placeholders and the API: [docs.exylia.net](https://docs.exylia.net/en/docs/exyliaclasses/).

## Building

```
./gradlew build
```

The jar is written to `build/libs/`. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[GNU General Public License v3.0](LICENSE).
