# Proposal

## Why

Server administrators currently have no in-game or automated mechanism to check for newer Croppy releases or stage updates, requiring manual checking of GitHub, manual downloading of JARs, and manual placement into the plugins directory. Providing `/croppy update check` and `/croppy update apply` gives server operators frictionless version awareness and safe, verified staging of updates directly from the server console or in-game chat.

## What Changes

- Add a decoupled update subsystem (`UpdateService`) in `croppy-api` with pure Java 21 contracts, semantic version comparison, and immutable result records.
- Implement `PaperUpdateService` in `croppy-paper` utilizing Java 21's native `HttpClient` to asynchronously query GitHub Releases API (`Cypherlink-Studios/Croppy`), filter by release channel (`STABLE` vs `BETA`), verify SHA-256 cryptographic checksums, and safely stage downloaded JARs into `plugins/update/<currentJarName>`.
- Introduce configuration block `updates:` in `config.yml` controlling `enabled`, `check-on-startup`, and `channel` (`STABLE` or `BETA`).
- Register administrative subcommands `/croppy update check` and `/croppy update apply` in `CroppyCommands` with permission `croppy.admin.update` and interactive MiniMessage formatting (click-to-apply prompts, progress indicators).
- Add passive update checking on server boot and send private notifications to operators with `croppy.admin.update` when joining the server.
- Add localization translation keys in `en-US.yml` and `es-ES.yml` for all update checks, progress stages, integrity errors, and staging confirmations.

## Capabilities

### New Capabilities
- `plugin-updater`: Automated GitHub Releases querying, semantic versioning comparison, release channel resolution (`STABLE` vs `BETA`), SHA-256 integrity verification against release checksum manifests, off-tick asynchronous downloading, and automated staging to Paper's `plugins/update/` directory.

### Modified Capabilities
- `crop-commands`: Extends root `/croppy` command dispatcher to register administrative subcommands `/croppy update check` and `/croppy update apply` with permission `croppy.admin.update` and interactive MiniMessage feedback.

## Impact

- **API & Core**: New pure Java 21 ports and domain models in `croppy-api` (`dev.darkblade.croppy.api.update`).
- **Paper Adapter**: `PaperUpdateService`, GitHub release HTTP client, SHA-256 verification, and player join listener in `croppy-paper`.
- **Configuration**: Addition of `updates` section in `config.yml` and `PluginConfig.java`.
- **Localization**: Additional message catalog keys under `update.*` in `en-US.yml` and `es-ES.yml`.
- **Dependencies**: Uses native Java 21 `java.net.http.HttpClient` without external HTTP client dependencies.
