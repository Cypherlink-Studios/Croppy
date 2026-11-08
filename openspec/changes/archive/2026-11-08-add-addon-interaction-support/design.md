# Design: Addon Interaction Discovery and Lifecycle Purging

## Context

Croppy addons can define crafting stations and crops that require interactive routing handled by the data-driven `interaction-engine`. Currently, while `PaperAddonManager` loads items, crops, recipes, stations, processes, and menus, it skips `interactions/` entirely. Furthermore, `PaperAddonContext` does not track interaction keys and `AddonDecommissioner` does not purge them during Phase 4 (Domain Registry Purging), leaking interactions on reload or disable.

See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Discover and parse YAML interaction definitions from `interactions/` in both unpacked addon directories and compiled JARs.
- Support both single definitions and lists of definitions per file via `YamlInteractionLoader.loadMultiple()`.
- Support nested subdirectories (e.g., `interactions/crops/`) within addon directories.
- Track registered interaction keys in `PaperAddonContext` with a thread-safe `Set<Key>`.
- Purge all tracked interactions from `InteractionRegistry` during Phase 4 in `AddonDecommissioner`.
- Guarantee fault tolerance: syntax errors in one interaction file must not crash the addon loader or server tick.

**Non-Goals:**
- Modifying `croppy-api` interfaces: `AddonContext.getInteractionRegistry()` already exists and `InteractionRegistry` supports `unregister(Key)`.
- Modifying the Pratt AST condition parser or interaction execution pipeline in `croppy-core`.

## Decisions

### 1. Multi-Definition Support via `YamlInteractionLoader.loadMultiple()`
- **Decision**: Use `loader.loadMultiple(in, name)` rather than `loader.load(in, name)`.
- **Rationale**: Built-in interaction files (such as `skillet_interactions.yml`) contain lists of multiple interactions within one file. `loadMultiple()` handles both single interaction maps and YAML lists seamlessly, giving addon authors flexibility in structuring definitions.
- **Alternatives Considered**: Using `load()` would fail on files declaring lists of interactions, forcing one file per interaction.

### 2. Deep Directory Traversal for Unpacked Addons
- **Decision**: Use `Files.walk(interactionsDir)` filtering for `.yml` and `.yaml`.
- **Rationale**: Aligns with the core plugin's `interactions/crops/` structure, allowing addons with many crops or complex stations to group interaction files into subfolders.

### 3. Thread-Safe Key Tracking in `PaperAddonContext`
- **Decision**: Use `ConcurrentHashMap.newKeySet()` for `registeredInteractions`, exposing `trackInteraction(Key key)` and `getRegisteredInteractions()`.
- **Rationale**: Consistent with existing tracking for `registeredSpecies`, `registeredItems`, `registeredProcesses`, and `registeredMenus`.

### 4. Phase 4 Decommissioning Purge in `AddonDecommissioner`
- **Decision**: Iterate over `context.getRegisteredInteractions()` and invoke `core.getInteractionRegistry().unregister(key)`.
- **Rationale**: Guarantees that when an addon is disabled or reloaded, interactions targeting addon-owned stations or crops are cleanly removed, preventing null pointer errors or invalid routing against disabled content.

## Risks / Trade-offs

- **[Risk]** Addon interaction key collision with core or other addons.
  - **Mitigation**: `SimpleRegistry.register()` will overwrite existing keys or log warnings as per registry design. Addon authors are advised to namespace interaction keys (`<addon_namespace>:<interaction_name>`).
- **[Risk]** Malformed YAML in an addon's `interactions/` folder.
  - **Mitigation**: Each file is parsed within an isolated `try-catch` block logging a warning with the file path without halting the load of subsequent addon files or other addons.
