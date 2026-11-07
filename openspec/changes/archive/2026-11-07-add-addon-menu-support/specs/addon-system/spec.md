# Spec Delta

## ADDED Requirements

### Requirement: Addon Declarative Menu Discovery
The system SHALL discover, parse, and validate `menus/*.yml` definition files from addon directories and JAR archives during addon enabling, registering them into `MenuRegistry` under their namespaced keys.

#### Scenario: Load menu definition from addon directory
- **WHEN** an enabled addon directory contains a valid YAML file in its `menus/` subdirectory
- **THEN** the system parses the menu pattern and slot definitions and registers the `MenuDefinition` in the global `MenuRegistry`

#### Scenario: Load menu definition from addon JAR archive
- **WHEN** an enabled addon JAR archive contains a valid `menus/*.yml` entry
- **THEN** the system extracts, parses, and registers the `MenuDefinition` in `MenuRegistry` under its namespaced key

#### Scenario: Skip invalid menu configuration gracefully
- **WHEN** an addon's menu YAML file contains syntax errors or invalid row pattern lengths
- **THEN** the system logs a warning detailing the failure and continues loading remaining addon resources without crashing

## MODIFIED Requirements

### Requirement: Scoped and Tracked Execution Context
The system SHALL provide each enabled addon with an isolated `AddonContext` tracking all domain registry keys (including species, items, recipes, processes, stations, and menus), Bukkit event listeners, scheduled tasks, and growth modifiers registered during its lifecycle.

#### Scenario: Scoped registration tracking
- **WHEN** an enabled addon registers custom items, species definitions, menus, or Bukkit event listeners via its `AddonContext`
- **THEN** the system records ownership of those resources under the addon's unique identifier

#### Scenario: Managed async task execution
- **WHEN** an addon schedules asynchronous work through its `AddonContext`
- **THEN** the tasks execute within a managed thread executor monitored for orderly shutdown

### Requirement: Safe 5-Phase Decommissioning Protocol
The system SHALL execute a five-phase decommissioning protocol upon addon disable or unload, ensuring complete resource cleanup and Metaspace leak prevention.

#### Scenario: Disabling an active addon
- **WHEN** an administrator disables an active addon
- **THEN** the system executes `onDisable()`, unregisters all Bukkit listeners, terminates managed executor tasks, removes or deactivates its registry entries (including species, items, recipes, processes, stations, and menus), and transitions the addon state to `LOADED`

#### Scenario: Unloading an addon completely
- **WHEN** an administrator unloads an addon
- **THEN** the system disables the addon if active, severs all classloader graph references, closes the `URLClassLoader`, and transitions the addon state to `ON_DISK`

#### Scenario: Handling active world crops for disabled addon species
- **WHEN** growth simulation ticks a crop in the world whose species belongs to a currently disabled addon
- **THEN** the system marks the crop dormant and skips the growth cycle without throwing exceptions or corrupting chunk state
