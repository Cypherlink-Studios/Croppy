# addon-system Specification

## Purpose
Provides a decoupled, extensible addon architecture for Croppy, supporting declarative content packs, Java code addons with DAG ClassLoader graphs, and safe hot-swapping lifecycle management.

## Requirements

### Requirement: Addon Manifest Specification
The system SHALL discover and parse `addon.yml` manifest files located in JAR archives or directories within `plugins/Croppy/addons/`, validating mandatory attributes (`id`, `name`, `version`, `namespace`) and optional attributes (`main`, `author`, `dependencies`, `softDependencies`).

#### Scenario: Valid declarative content pack manifest
- **WHEN** an addon directory or JAR without a `main` class attribute contains a valid `addon.yml`
- **THEN** the system registers the addon as a declarative content pack with state `LOADED`

#### Scenario: Valid Java code addon manifest
- **WHEN** an addon JAR declares an existing class implementing `CroppyAddon` in the `main` attribute
- **THEN** the system registers the addon as a code addon with state `LOADED` and maps its entrypoint

#### Scenario: Missing mandatory manifest fields
- **WHEN** an addon manifest omits `id`, `name`, `version`, or `namespace`
- **THEN** the system rejects the addon, logs a validation failure, and leaves the addon in state `ON_DISK`

### Requirement: Addon Dependency Resolution and DAG Validation
The system SHALL validate inter-addon dependencies using Kahn's topological sort algorithm, establishing deterministic initialization and teardown orders while rejecting circular dependencies.

#### Scenario: Acyclic dependency chain resolution
- **WHEN** Addon C depends on Addon B, and Addon B depends on Addon A
- **THEN** the system computes the enable order as [Addon A, Addon B, Addon C] and disable order as [Addon C, Addon B, Addon A]

#### Scenario: Circular dependency detection
- **WHEN** Addon A depends on Addon B and Addon B depends on Addon A
- **THEN** the system fails validation, refuses to load both addons, and logs the circular cycle in console

#### Scenario: Missing required dependency
- **WHEN** an addon declares a required dependency that is absent from `plugins/Croppy/addons/`
- **THEN** the system marks the addon as unsatisfied, prevents state transition to `LOADED`, and logs missing dependency details

### Requirement: ClassLoader Graph with Horizontal Delegation
The system SHALL instantiate an `AddonClassLoader` for each Java code addon that delegates class searches to declared dependency classloaders before falling back to Croppy's parent classloader.

#### Scenario: Resolving classes across dependent addons
- **WHEN** Addon B references a public API class defined in Addon A and declares Addon A as a dependency
- **THEN** Addon B's `AddonClassLoader` successfully resolves and links the class from Addon A's classloader

#### Scenario: Class isolation between independent addons
- **WHEN** Addon C attempts to load an internal class from Addon A without declaring Addon A as a dependency
- **THEN** Addon C's `AddonClassLoader` throws `ClassNotFoundException` preserving namespace isolation

### Requirement: Scoped and Tracked Execution Context
The system SHALL provide each enabled addon with an isolated `AddonContext` tracking all domain registry keys (including species, items, recipes, processes, stations, menus, and interactions), Bukkit event listeners, scheduled tasks, and growth modifiers registered during its lifecycle.

#### Scenario: Scoped registration tracking
- **WHEN** an enabled addon registers custom items, species definitions, menus, processes, stations, interactions, or Bukkit event listeners via its `AddonContext`
- **THEN** the system records ownership of those resources under the addon's unique identifier

#### Scenario: Managed async task execution
- **WHEN** an addon schedules asynchronous work through its `AddonContext`
- **THEN** the tasks execute within a managed thread executor monitored for orderly shutdown

### Requirement: Safe 5-Phase Decommissioning Protocol
The system SHALL execute a five-phase decommissioning protocol upon addon disable or unload, ensuring complete resource cleanup and Metaspace leak prevention.

#### Scenario: Disabling an active addon
- **WHEN** an administrator disables an active addon
- **THEN** the system executes `onDisable()`, unregisters all Bukkit listeners, terminates managed executor tasks, removes or deactivates its registry entries (including species, items, recipes, processes, stations, menus, and interactions), and transitions the addon state to `LOADED`

#### Scenario: Unloading an addon completely
- **WHEN** an administrator unloads an addon
- **THEN** the system disables the addon if active, severs all classloader graph references, closes the `URLClassLoader`, and transitions the addon state to `ON_DISK`

#### Scenario: Handling active world crops for disabled addon species
- **WHEN** growth simulation ticks a crop in the world whose species belongs to a currently disabled addon
- **THEN** the system marks the crop dormant and skips the growth cycle without throwing exceptions or corrupting chunk state

### Requirement: Addon Configuration and Settings Persistence
The system SHALL provide each enabled addon with access to an isolated `AddonConfig` mapped to its dedicated data folder at `plugins/Croppy/addons/<addon-id>/config.yml` (or the addon root directory for directory addons), supporting default resource extraction, typed value queries, runtime modification, and reloading.

#### Scenario: Extracting bundled default configuration
- **WHEN** an addon invokes `saveDefaultConfig()` and `config.yml` does not exist on disk
- **THEN** the system extracts the bundled `config.yml` from the addon archive to its designated data folder without overwriting existing files

#### Scenario: Reading typed configuration values
- **WHEN** an addon queries `AddonConfig` for string, integer, boolean, double, list, or nested section values
- **THEN** the system returns the configured value or the provided fallback default value if the key is missing

#### Scenario: Dynamic configuration reloading
- **WHEN** an administrator or addon triggers `reloadConfig()`
- **THEN** the system re-reads `config.yml` from disk, updating in-memory configuration state without restarting the server or addon

#### Scenario: Saving in-memory configuration modifications
- **WHEN** an addon modifies a configuration key and calls `saveConfig()`
- **THEN** the system persists the updated configuration to `config.yml` on disk

### Requirement: Addon Path Ownership and Collision Prevention
The system SHALL track filesystem paths for all discovered addon roots and data folders, rejecting duplicate addon identifiers and preventing cross-occupancy collisions between compiled JAR addons and directory addons.

#### Scenario: Duplicate addon identifier detection
- **WHEN** two addon archives or directories declare identical `id` values in their `addon.yml` manifests
- **THEN** the system rejects registration of the colliding addon, logs a severe collision error, and leaves the duplicate in an uninitialized state

#### Scenario: JAR addon colliding with occupied directory addon path
- **WHEN** a compiled JAR addon targets a data folder path that is already occupied as the root directory of a distinct directory addon
- **THEN** the system halts initialization of the JAR addon with a descriptive fail-fast error, preventing file overwrite or data corruption

#### Scenario: Directory addon binding root directory as data folder
- **WHEN** an addon is installed as an unpacked directory in `plugins/Croppy/addons/<folder-name>/`
- **THEN** the system binds that exact root directory as the addon's `dataFolder` and reads `config.yml` directly from its root

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

### Requirement: Addon Declarative Interaction Discovery
The system SHALL discover, parse, and validate `interactions/*.yml` and nested YAML files from addon directories and JAR archives during addon enabling, registering them into `InteractionRegistry` under their namespaced keys.

#### Scenario: Load interaction definition from addon directory
- **WHEN** an enabled addon directory contains a valid YAML file in its `interactions/` subdirectory or nested paths
- **THEN** the system parses the interaction definition using `YamlInteractionLoader` and registers the `InteractionDefinition` in the global `InteractionRegistry` and tracks its key in `PaperAddonContext`

#### Scenario: Load interaction definition from addon JAR archive
- **WHEN** an enabled addon JAR archive contains a valid `interactions/*.yml` or nested YAML entry
- **THEN** the system extracts, parses, registers the `InteractionDefinition` in `InteractionRegistry`, and tracks its key in `PaperAddonContext`

#### Scenario: Skip invalid interaction configuration gracefully
- **WHEN** an addon's interaction YAML file contains syntax errors or missing mandatory identifiers
- **THEN** the system logs a warning detailing the failure and continues loading remaining addon resources without crashing
