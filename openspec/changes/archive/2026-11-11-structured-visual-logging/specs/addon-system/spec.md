# Spec Delta

## MODIFIED Requirements

### Requirement: Scoped and Tracked Execution Context
The system SHALL provide each enabled addon with an isolated `AddonContext` tracking all domain registry keys (including species, items, item categories, crafting recipes, culinary recipes, recipe categories, processes, stations, menus, and interactions), Bukkit event listeners, scheduled tasks, growth modifiers, and a scoped logging bridge routing `AddonContext.getLogger()` JUL output and default `getCroppyLogger()` calls under the addon's scope tag.

#### Scenario: Scoped registration tracking
- **WHEN** an enabled addon registers custom items, item categories, crafting recipes, species definitions, menus, processes, stations, interactions, recipe categories, or Bukkit event listeners via its `AddonContext`
- **THEN** the system records ownership of those resources under the addon's unique identifier

#### Scenario: Managed async task execution
- **WHEN** an addon schedules asynchronous work through its `AddonContext`
- **THEN** the tasks execute within a managed thread executor monitored for orderly shutdown

#### Scenario: Backwards-compatible JUL addon logging bridge
- **WHEN** a code addon logs messages through `AddonContext.getLogger()` (`java.util.logging.Logger`)
- **THEN** log records are forwarded to the host `CroppyLogger` scoped under `Addon:<id>` on the `ADDON` channel while respecting active log levels and formatting

#### Scenario: Direct CroppyLogger access on AddonContext
- **WHEN** a code addon invokes `AddonContext.getCroppyLogger()`
- **THEN** the system returns the scoped `CroppyLogger` instance (or a JUL-backed adapter if a test context omits an explicit override) without breaking existing `AddonContext` implementations

## ADDED Requirements

### Requirement: Consolidated Addon Lifecycle Reporting
The system SHALL summarize discovered, enabled, and failed addons during startup and reload under the addon lifecycle phase while routing per-asset and per-definition addon loading details to `DEBUG`.

#### Scenario: Summarizing enabled addons at startup
- **WHEN** `PaperAddonManager` enables discovered directory or JAR addons during plugin startup
- **THEN** the startup phase displays a consolidated status entry per addon (including ID, version, type, and load duration) without spamming individual YAML definition `INFO` lines
