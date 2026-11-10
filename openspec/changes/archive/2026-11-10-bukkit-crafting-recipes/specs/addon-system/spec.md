# Spec Delta

## MODIFIED Requirements

### Requirement: Scoped and Tracked Execution Context
The system SHALL provide each enabled addon with an isolated `AddonContext` tracking all domain registry keys (including species, items, item categories, crafting recipes, culinary recipes, recipe categories, processes, stations, menus, and interactions), Bukkit event listeners, scheduled tasks, and growth modifiers registered during its lifecycle.

#### Scenario: Scoped registration tracking
- **WHEN** an enabled addon registers custom items, item categories, crafting recipes, species definitions, menus, processes, stations, interactions, recipe categories, or Bukkit event listeners via its `AddonContext`
- **THEN** the system records ownership of those resources under the addon's unique identifier

#### Scenario: Managed async task execution
- **WHEN** an addon schedules asynchronous work through its `AddonContext`
- **THEN** the tasks execute within a managed thread executor monitored for orderly shutdown

### Requirement: Safe 5-Phase Decommissioning Protocol
The system SHALL execute a five-phase decommissioning protocol upon addon disable or unload, ensuring complete resource cleanup and Metaspace leak prevention.

#### Scenario: Disabling an active addon
- **WHEN** an administrator disables an active addon
- **THEN** the system executes `onDisable()`, unregisters all Bukkit listeners, terminates managed executor tasks, removes or deactivates its registry entries (including species, items, crafting recipes, recipes, recipe categories, processes, stations, menus, and interactions), unregisters Bukkit crafting recipes, and transitions the addon state to `LOADED`

#### Scenario: Unloading an addon completely
- **WHEN** an administrator unloads an addon
- **THEN** the system disables the addon if active, severs all classloader graph references, closes the `URLClassLoader`, and transitions the addon state to `ON_DISK`

#### Scenario: Handling active world crops for disabled addon species
- **WHEN** growth simulation ticks a crop in the world whose species belongs to a currently disabled addon
- **THEN** the system marks the crop dormant and skips the growth cycle without throwing exceptions or corrupting chunk state

## ADDED Requirements

### Requirement: Addon Declarative Crafting Recipe Discovery
The system SHALL discover, parse, and validate `crafting/*.yml` and nested YAML files from addon directories and JAR archives during addon enabling, registering them into `CraftingRecipeRegistry` under their namespaced keys.

#### Scenario: Load crafting recipes from addon directory
- **WHEN** an enabled addon directory contains a valid YAML file in its `crafting/` subdirectory or nested paths
- **THEN** the system parses the crafting recipe using `YamlCraftingRecipeLoader`, registers it in `CraftingRecipeRegistry`, registers the Bukkit recipe, and tracks its key in `PaperAddonContext`

#### Scenario: Load crafting recipes from addon JAR archive
- **WHEN** an enabled addon JAR archive contains a valid `crafting/*.yml` or nested YAML entry
- **THEN** the system extracts, parses, registers the recipe in `CraftingRecipeRegistry`, registers the Bukkit recipe, and tracks its key in `PaperAddonContext`

#### Scenario: Skip invalid crafting recipe configuration gracefully
- **WHEN** an addon's crafting recipe YAML file contains syntax errors or missing mandatory identifiers
- **THEN** the system logs a warning detailing the failure and continues loading remaining addon resources without crashing
