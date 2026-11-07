# Proposal

## Why

Croppy addons can declare items, crops, recipes, crafting stations, and sequential processes in declarative YAML content packs or compiled JARs, but cannot currently provide custom inventory menus. Stations defined by addons (such as coffee roasters, grinders, or custom culinary stations) are forced to fallback to `croppy:default_station` because `menus/` directories and JAR paths are completely skipped during addon discovery, and menus registered programmatically are leaked during decommissioning because `PaperAddonContext` and `AddonDecommissioner` do not track or purge `MenuRegistry` entries.

Extending addon discovery and the lifecycle context to support `menus/` resolves this limitation, allowing addons to provide custom UI layouts, button bindings, and process progress bars while guaranteeing leak-proof 5-phase decommissioning upon reload or disable.

## What Changes

- Add discovery and parsing of `menus/*.yml` configurations from unpacked addon directories and compiled JAR/ZIP archives in `PaperAddonManager` using `YamlMenuLoader`.
- Extend `PaperAddonContext` to track registered `MenuDefinition` keys (`trackMenu(Key key)` and `getRegisteredMenus()`).
- Update `AddonDecommissioner` Phase 4 (Domain Registry Purging) to purge tracked menu keys from `core.getMenuRegistry()` when an addon is disabled or unloaded.
- Ensure all discovered menus are validated against `MenuDefinition` pattern and slot rules and registered under their respective namespaced keys (e.g. `croppy_coffee:coffee_roaster_menu`).

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `addon-system`: Extend scoped execution tracking and 5-phase decommissioning to include `MenuDefinition` registrations, and add declarative menu discovery to addon content loading.

## Impact

- `croppy-paper`:
  - `PaperAddonManager`: Scans `menus/` in both `loadDirectoryContent` and `loadJarContent`, loading YAML files into `MenuRegistry` and tracking their keys in `PaperAddonContext`.
  - `PaperAddonContext`: Adds menu key tracking (`trackMenu`, `getRegisteredMenus`).
  - `AddonDecommissioner`: Unregisters tracked menus from `MenuRegistry` during Phase 4.
- Existing addons (like `croppy-coffee`): Can immediately introduce custom YAML station menus in `menus/` and bind them to station definitions via `menu: "namespace:id"`.
- Backward Compatibility: Fully backward-compatible; stations with missing or unspecified menus continue falling back to `croppy:default_station`.
