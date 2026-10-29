# Proposal: Crop Inspection and Administration Commands

## Why
Server administrators, developers, and builders currently have no in-game mechanism to inspect the real-time simulation state of crops (active species, current stage, progress percentage, environmental light, and water fluid levels) or manipulate them for debugging and content testing. Furthermore, configuration updates require a full server restart, and there is no administrative way to grant custom crop seeds or items to players. 

Introducing a command framework based on Incendo Cloud 2.0 and an interactive inspection tool allows staff to diagnose crop health, test growth parameters rapidly, reload definitions on the fly, and distribute crops seamlessly.

## What Changes
- Add **Incendo Cloud Paper 2.0.1** and **Cloud Annotations 2.0.0** command framework integration targeting Paper 1.21.8 `CommandSourceStack`.
- Introduce a custom Cloud `ParserDescriptor` / argument parser for registered `Species` keys with dynamic asynchronous tab completion.
- Implement `/croppy inspect [x y z]` permitting raytraced or coordinate-targeted crop inspection displaying rich Kyori Adventure / MiniMessage status cards.
- Implement `/croppy tool` and an interactive right-click inspection listener using a configurable item (e.g. `SPYGLASS` or `STICK`) tagged with PersistentDataContainer (`croppy:tool`).
- Implement `/croppy reload` to hot-reload `config.yml` and species definitions from disk without server restarts.
- Implement debug manipulation commands `/croppy setstage <stageIndex>` and `/croppy setprogress <progress>` to adjust targeted crop state instantly and re-synchronize display blocks.
- Implement `/croppy give <player> <species> [amount]` to issue species items directly to player inventories.
- Provide user-facing configurable messages and tool settings in `config.yml`.

## Capabilities

### New Capabilities
- `crop-commands`: Administrative commands, in-game inspection tool, state manipulation, hot-reloading, and crop item distribution powered by Incendo Cloud 2.0.

### Modified Capabilities
<!-- None: existing capability contracts remain unmodified; this capability consumes existing lifecycle, growth, and registry services. -->

## Impact
- **Dependencies**: Adds `org.incendo:cloud-paper:2.0.1` and `org.incendo:cloud-annotations:2.0.0` to `croppy-paper`.
- **croppy-paper**: Registers `PaperCommandManager<CommandSourceStack>`, custom parsers, `CroppyCommands` controller, and `InspectionToolListener`.
- **croppy-core & croppy-api**: Unmodified (preserves clean architecture boundaries without any Bukkit or Cloud dependencies).
- **Configuration**: Adds `inspection-tool` and `messages` blocks to `config.yml`.
