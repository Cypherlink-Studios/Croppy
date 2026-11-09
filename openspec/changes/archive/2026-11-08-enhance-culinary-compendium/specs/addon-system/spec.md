# Spec Delta: addon-system

## MODIFIED Requirements

### Requirement: Scoped and Tracked Execution Context
The system SHALL provide each enabled addon with an isolated `AddonContext` tracking all domain registry keys (including species, items, recipes, recipe categories, processes, stations, menus, and interactions), Bukkit event listeners, scheduled tasks, and growth modifiers registered during its lifecycle.

#### Scenario: Scoped registration tracking
- **WHEN** an enabled addon registers custom items, species definitions, menus, processes, stations, interactions, recipe categories, or Bukkit event listeners via its `AddonContext`
- **THEN** the system records ownership of those resources under the addon's unique identifier

#### Scenario: Managed async task execution
- **WHEN** an addon schedules asynchronous work through its `AddonContext`
- **THEN** the tasks execute within a managed thread executor monitored for orderly shutdown

### Requirement: Safe 5-Phase Decommissioning Protocol
The system SHALL execute a five-phase decommissioning protocol upon addon disable or unload, ensuring complete resource cleanup and Metaspace leak prevention.

#### Scenario: Disabling an active addon
- **WHEN** an administrator disables an active addon
- **THEN** the system executes `onDisable()`, unregisters all Bukkit listeners, terminates managed executor tasks, removes or deactivates its registry entries (including species, items, recipes, recipe categories, processes, stations, menus, and interactions), and transitions the addon state to `LOADED`

#### Scenario: Unloading an addon completely
- **WHEN** an administrator unloads an addon
- **THEN** the system disables the addon if active, severs all classloader graph references, closes the `URLClassLoader`, and transitions the addon state to `ON_DISK`

#### Scenario: Handling active world crops for disabled addon species
- **WHEN** growth simulation ticks a crop in the world whose species belongs to a currently disabled addon
- **THEN** the system marks the crop dormant and skips the growth cycle without throwing exceptions or corrupting chunk state
