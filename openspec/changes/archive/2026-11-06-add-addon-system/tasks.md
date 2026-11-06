# Tasks

## 1. API Contracts and Registry Unregistration

- [x] 1.1 Add `boolean unregister(Key key)` and `int unregisterAll(Predicate<T> filter)` to `Registry<T>` interface in `croppy-api` and verify clean compilation.
- [x] 1.2 Implement `unregister` and `unregisterAll` in `SimpleRegistry` in `croppy-core`, and add unit tests in `SimpleRegistryTest` verifying entry removal, non-existent key handling, and bulk predicate filtering.
- [x] 1.3 Define `AddonState`, `AddonDescription`, `AddonContext`, `CroppyAddon`, and `AddonManager` interfaces in `dev.darkblade.croppy.api.addon` in `croppy-api` and verify zero external dependencies with clean compilation.

## 2. Core Addon Domain, DAG Validator, and Manifest Parsing

- [x] 2.1 Implement `YamlAddonDescriptionLoader` in `croppy-core` to parse `addon.yml` from files and streams with strict validation of required and optional attributes. Add unit tests in `YamlAddonDescriptionLoaderTest`.
- [x] 2.2 Implement `AddonGraphValidator` in `croppy-core` using Kahn's topological sort algorithm to compute deterministic dependency order and reject cyclic dependencies. Add unit tests in `AddonGraphValidatorTest`.
- [x] 2.3 Implement `DefaultAddonManager` in `croppy-core` managing addon discovery, state tracking, and registry association. Add unit tests in `DefaultAddonManagerTest`.

## 3. Paper ClassLoader Graph, Tracked Context, and Hot-Swap Protocol

- [x] 3.1 Implement `AddonClassLoader` in `croppy-paper` extending `URLClassLoader` with horizontal cross-addon class delegation before parent fallback. Add unit tests verifying cross-addon class resolution and isolation.
- [x] 3.2 Implement `PaperAddonContext` in `croppy-paper` tracking Bukkit listeners, managed asynchronous executors, and registered domain keys per addon. Add unit tests verifying registration tracking.
- [x] 3.3 Implement `AddonDecommissioner` in `croppy-paper` executing the 5-phase decommissioning protocol (`onDisable`, listener unregistration, executor draining, registry purging, ClassLoader closure, and dormant crop state handling). Add unit tests verifying hot-unload cleanup.

## 4. Resource Pack Integration, Commands, and Plugin Lifecycle

- [x] 4.1 Update companion resource pack compiler in `croppy-paper` to discover and merge `assets/<namespace>/` subdirectories from enabled addons. Add unit tests verifying asset aggregation.
- [x] 4.2 Implement `/croppy addon <list|status|load|unload|enable|disable> [addonId]` with Cloud Paper 2.0 tab completion and `croppy.admin.addon` permission checks. Add unit tests for command routing.
- [x] 4.3 Integrate `PaperAddonManager` into `CroppyPlugin.onEnable()`, `onDisable()`, and register fallback dormant handling in `GrowthEngine`. Run full test suite `./gradlew test` to verify zero test regressions.

## Workflow follow-up

- Review and apply change via `/openspec-apply-change`.
- Verify full test suite passing with `./gradlew test`.
- Archive change and synchronize durable specs via `/openspec-archive-change`.
