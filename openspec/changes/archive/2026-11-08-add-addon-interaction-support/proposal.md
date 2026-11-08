# Proposal: Addon Declarative Interaction Discovery and Lifecycle Tracking

## Why

Croppy addons can define custom crops, items, recipes, crafting stations, sequential processes, and menus in declarative YAML packs or compiled JARs. However, custom crafting stations and crops often require bespoke player interaction routing (such as advancing multi-phase processes with specific tools, extracting ingredients, or performing non-standard harvesting actions) using the data-driven `interaction-engine`.

Currently, `PaperAddonManager` does not scan `interactions/` directories or JAR entries, forcing server owners to manually place addon interaction files in the root `plugins/Croppy/interactions/` directory. Furthermore, programmatic registrations via `AddonContext.getInteractionRegistry()` are not tracked by `PaperAddonContext` and are not purged during Phase 4 of `AddonDecommissioner`, leaving orphan interactions that cause memory and registry leaks on addon disable or reload.

## What Changes

- **Declarative Addon Interaction Discovery**:
  - Scan `interactions/` (including nested subdirectories like `interactions/crops/`) in unpacked addon directories during `PaperAddonManager.loadDirectoryContent()`.
  - Scan `interactions/` entries in compiled addon JARs during `PaperAddonManager.loadJarContent()`.
  - Parse interaction definitions using `YamlInteractionLoader.loadMultiple()`, register them in `InteractionRegistry`, and track their keys in `PaperAddonContext`.
- **Scoped Interaction Lifecycle Tracking**:
  - Add thread-safe `registeredInteractions` set to `PaperAddonContext` with `trackInteraction(Key key)` and `getRegisteredInteractions()`.
- **Safe 5-Phase Decommissioning Purge**:
  - Update `AddonDecommissioner` (Phase 4: Domain Registry Purging) to unregister all tracked interaction keys from `InteractionRegistry`.
- **Unit & Integration Test Coverage**:
  - Add tests in `PaperAddonContextTest`, `AddonDecommissionerTest`, and `PaperAddonManagerTest` verifying directory and JAR discovery, lifecycle unregistration, and isolation.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `addon-system`: Extend scoped execution tracking and 5-phase decommissioning to cover `InteractionRegistry`, and introduce declarative discovery for `interactions/*.yml` from addon directories and JARs.

## Impact

- **Affected Systems**:
  - `croppy-paper`:
    - `PaperAddonContext`: adds interaction key tracking.
    - `PaperAddonManager`: adds `interactions/` scanning in both directory and JAR loaders.
    - `AddonDecommissioner`: purges tracked interactions from `InteractionRegistry`.
- **API Impact**:
  - Pure `croppy-api` already exposes `AddonContext.getInteractionRegistry()` and `InteractionRegistry.unregister(Key)` via `SimpleRegistry`. No breaking API changes.
- **Dependencies**: None.
