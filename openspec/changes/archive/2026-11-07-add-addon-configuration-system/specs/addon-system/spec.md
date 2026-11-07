# Spec Delta

## ADDED Requirements

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
