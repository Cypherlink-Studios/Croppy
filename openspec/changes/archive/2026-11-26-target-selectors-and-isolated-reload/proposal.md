# Proposal

## Why

Currently, `/croppy reload` tears down and re-enables the entire addon ecosystem (`disableAll()` / `enableAll()`), coupling core configuration/asset reloading to external extension lifecycles and causing unnecessary addon restarts or state disruption. At the same time, there is no dedicated command to hard-reload addons independently, and administrative bulk operations (`/croppy addon ...`, `/croppy recipe give|revoke`) rely on ad-hoc `"all"` literals or single keys rather than a unified, extensible `@`-prefixed target selector mechanism in Incendo Cloud 2.0.

## What Changes

- **Isolate `/croppy reload` from addons**: Update `CroppyPlugin.reloadPlugin()` so `/croppy reload` reloads only Croppy's own configuration (`config.yml`), localization catalogs, and core YAML definitions while preserving all enabled addons, their classloaders/listeners/tasks, and every registry entry owned by active addons.
- **Introduce `/croppy addon reload <<addonId> | @all>` (Hard Reload)**: Add a dedicated hard-reload lifecycle operation that unloads the target addon(s) (and any active transitive dependents in reverse topological order), re-discovers manifests/archives on disk safely, and re-enables them in DAG dependency order.
- **Extensible Cloud 2.0 Target Selector system (`@...`)**: Introduce a composable, generic `TargetSelection<T>` / `SelectableTargetParser<C, T>` abstraction built on Incendo Cloud 2.0 `ArgumentParser` + `SuggestionProvider` with pluggable `SelectorRule`s and string-compatible Brigadier mappings (avoiding `ArgumentTypes.key()` client rejection of `@`).
- **BREAKING**: Migrate `/croppy addon <load|unload|enable|disable|reload>` from the plain `"all"` literal to the `@all` target selector.
- **Bulk Recipe Knowledge Selectors**: Update `/croppy recipe <give|revoke> <player> <recipe>` to accept `@all` and `@<categoryKey>` (supporting both `@croppy:soups` and unqualified `@soups` shorthand) alongside individual recipe keys, emitting concise summary messages to both the administrator and target player when multiple recipes are affected.

## Capabilities

### New Capabilities
<!-- None — this change extends existing command and addon lifecycle capabilities. -->

### Modified Capabilities
- `crop-commands`: Isolate `/croppy reload` from addons; introduce extensible `@`-prefixed target selectors in Cloud 2.0; migrate `/croppy addon <load|unload|enable|disable|reload>` to `@all`; add `@all` and `@<categoryKey>` selectors with summary feedback to `/croppy recipe give|revoke`.
- `addon-system`: Support hard addon reload with safe disk rediscovery, transitive dependent cascade unloading/re-enabling via DAG order, and ownership-aware core registry preservation during host plugin reload.

## Impact

- **`croppy-core`**:
  - `DefaultAddonManager` (`discoverAddons()` / `registerDiscovered()` / rediscovery refresh for unloaded or removed addons).
- **`croppy-paper`**:
  - `CroppyPlugin.reloadPlugin()` (ownership-aware selective registry cleanup preserving addon-owned entries; removal of `addonManager.disableAll()/enableAll()`).
  - `CroppyPlugin.setupCommands()` (register `TargetSelection` parsers and Brigadier string mappings instead of `ArgumentTypes.key()` for selector-enabled arguments).
  - `PaperAddonManager` (hard `reloadAddon(String)` with transitive dependent cascade, `reloadAll()`, and disk rediscovery).
  - `CroppyCommands` (new `/croppy addon reload` command, migrated `/croppy addon` and `/croppy recipe` signatures, summary feedback).
  - New command selector infrastructure under `dev.darkblade.croppy.platform.paper.command.selector` / `parser`.
  - Localization bundles (`en-US.yml`, `es-ES.yml`) for addon reload, selector errors, and bulk recipe summary keys.
  - Unit tests in `croppy-core` and `croppy-paper`.
