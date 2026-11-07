# Design

## Context

Croppy features an extensible YAML-driven menu engine centered around `MenuDefinition`, `YamlMenuLoader`, and `MenuRegistry`. Station definitions (`StationDefinition`) already support binding custom menus via `menuKey` (YAML `menu: "namespace:id"`).

However, while `PaperAddonManager` loads items, crops, recipes, stations, and processes from addon folders and JAR archives, it lacks scanning for `menus/`. Additionally, `PaperAddonContext` does not maintain a tracked set of registered menu keys, and `AddonDecommissioner` does not purge `MenuRegistry` during Phase 4 (Domain Registry Purging), creating lifecycle leaks on addon reload or disable.

## Goals / Non-Goals

**Goals:**
- Scan and load `menus/*.yml` and `menus/*.yaml` from unpacked addon directories in `PaperAddonManager.loadDirectoryContent()`.
- Scan and load `menus/**/*.yml` and `menus/**/*.yaml` from addon JAR archives in `PaperAddonManager.loadJarContent()`.
- Track registered menu keys in `PaperAddonContext` with a thread-safe `Set<Key> registeredMenus`.
- Purge all tracked menus from `core.getMenuRegistry()` during Phase 4 in `AddonDecommissioner.decommission()`.
- Provide automated unit tests verifying directory and JAR menu loading, context tracking, and decommissioning.

**Non-Goals:**
- Modifying `MenuDefinition`, `YamlMenuLoader`, or inventory pattern parsing rules.
- Changing `croppy-api` contracts, as `AddonContext.getMenuRegistry()` already provides registry access.
- Implementing GUI pagination or dynamic submenus (handled separately by existing menu controllers).

## Decisions

### 1. Reuse Core `YamlMenuLoader` Directly in Paper Adapter
- **Decision**: In `PaperAddonManager`, instantiate `new YamlMenuLoader()` to parse menu files, matching the pattern used for items, recipes, stations, and processes.
- **Rationale**: `YamlMenuLoader` in `croppy-core` already validates pattern dimensions, slot elements, conditional templates, and action bindings.
- **Alternative considered**: Implementing a separate parser or duplicating logic in `croppy-paper` (rejected: violates Hexagonal Architecture and DRY).

### 2. Scoped Tracking in `PaperAddonContext` and Purging in `AddonDecommissioner`
- **Decision**: Add `private final Set<Key> registeredMenus = ConcurrentHashMap.newKeySet();` along with `trackMenu(Key key)` and `getRegisteredMenus()` in `PaperAddonContext`. In `AddonDecommissioner.decommission()`, iterate over `context.getRegisteredMenus()` and call `core.getMenuRegistry().unregister(key)`.
- **Rationale**: Follows the exact established pattern used for species, items, recipes, processes, and stations, guaranteeing leak-free hot-reloading and disabling.
- **Alternative considered**: Purging by namespace wildcard on disable (rejected: an addon could potentially overwrite or interact with shared namespaces; explicit key tracking is deterministic and safe).

### 3. Fault-Tolerant File Loading
- **Decision**: Wrap each individual menu loading attempt in a `try-catch (Exception e)` block within `loadDirectoryContent` and `loadJarContent`.
- **Rationale**: In line with Croppy's runtime fault-tolerance conventions (Section 5.2 of `AGENTS.md`), a malformed menu file in an addon must log a descriptive warning with the file path and continue loading remaining resources rather than aborting addon startup.

## Risks / Trade-offs

- **[Risk] Open Viewers During Addon Decommissioning**: If an addon is disabled while a player has an open station menu belonging to that addon.
  - *Mitigation*: `MenuRegistry.unregister(key)` removes future lookups. To prevent runtime issues, any ongoing interaction will be blocked by `PaperMenuListener` if the definition is no longer present, and normal closing behavior applies.
- **[Trade-off] Multi-directory Menu Structures**: Addons may organize menus in subdirectories (e.g. `menus/stations/grinder.yml`).
  - *Mitigation*: Ensure `Files.walk` in directory scanning and recursive path matching (`name.startsWith("menus/")`) in JAR scanning cover arbitrarily nested subfolders.
