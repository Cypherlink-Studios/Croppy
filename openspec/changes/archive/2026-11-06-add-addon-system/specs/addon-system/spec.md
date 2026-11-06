# Spec Delta

## Purpose

Provides a decoupled, extensible addon architecture for Croppy, supporting declarative content packs, Java code addons with DAG ClassLoader graphs, and safe hot-swapping lifecycle management.

## ADDED Requirements

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
The system SHALL provide each enabled addon with an isolated `AddonContext` tracking all domain registry keys, Bukkit event listeners, scheduled tasks, and growth modifiers registered during its lifecycle.

#### Scenario: Scoped registration tracking
- **WHEN** an enabled addon registers custom items, species definitions, or Bukkit event listeners via its `AddonContext`
- **THEN** the system records ownership of those resources under the addon's unique identifier

#### Scenario: Managed async task execution
- **WHEN** an addon schedules asynchronous work through its `AddonContext`
- **THEN** the tasks execute within a managed thread executor monitored for orderly shutdown

### Requirement: Safe 5-Phase Decommissioning Protocol
The system SHALL execute a five-phase decommissioning protocol upon addon disable or unload, ensuring complete resource cleanup and Metaspace leak prevention.

#### Scenario: Disabling an active addon
- **WHEN** an administrator disables an active addon
- **THEN** the system executes `onDisable()`, unregisters all Bukkit listeners, terminates managed executor tasks, removes or deactivates its registry entries, and transitions the addon state to `LOADED`

#### Scenario: Unloading an addon completely
- **WHEN** an administrator unloads an addon
- **THEN** the system disables the addon if active, severs all classloader graph references, closes the `URLClassLoader`, and transitions the addon state to `ON_DISK`

#### Scenario: Handling active world crops for disabled addon species
- **WHEN** growth simulation ticks a crop in the world whose species belongs to a currently disabled addon
- **THEN** the system marks the crop dormant and skips the growth cycle without throwing exceptions or corrupting chunk state
