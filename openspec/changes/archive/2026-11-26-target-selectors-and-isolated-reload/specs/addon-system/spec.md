# Spec Delta

## ADDED Requirements

### Requirement: Hard Addon Reload and Rediscovery
The system SHALL support hard-reloading an individual addon or all addons by unloading target addons and any active transitive dependents in reverse topological order, safely refreshing addon manifests and paths from `plugins/Croppy/addons/` without duplicate-ID collisions, and re-enabling them in topological dependency order.

#### Scenario: Hard-reloading an independent addon
- **WHEN** an administrator triggers a hard reload on an enabled addon with no active dependents
- **THEN** the system unloads the addon (closing its classloader and purging its tracked resources), re-reads its manifest and archive from disk, and re-enables it cleanly

#### Scenario: Hard-reloading an addon with active dependents
- **WHEN** Addon B declares a dependency or soft dependency on Addon A and both are active, and an administrator triggers a hard reload on Addon A
- **THEN** the system unloads Addon B before unloading Addon A, refreshes from disk, and re-enables Addon A before Addon B

#### Scenario: Rediscovering updated or removed addons without duplicate errors
- **WHEN** the addon manager re-scans `plugins/Croppy/addons/` after unloading an addon whose manifest version changed or whose archive was removed from disk
- **THEN** the system updates the discovered metadata for the refreshed addon or removes stale entries for missing archives without logging false duplicate-ID errors

### Requirement: Addon Registry Ownership Preservation Across Core Reload
The system SHALL allow the host plugin to identify registry keys owned by enabled addons so that reloading host definitions purges only host-owned entries while preserving all active addon registrations.

#### Scenario: Querying addon-owned keys during core reload
- **WHEN** the host plugin reloads core definitions while addons are enabled
- **THEN** registry entries tracked by any enabled addon's `AddonContext` are preserved intact across species, items, item categories, crafting recipes, culinary recipes, recipe categories, menus, stations, processes, and interactions

## MODIFIED Requirements

### Requirement: Consolidated Addon Lifecycle Reporting
The system SHALL summarize discovered, enabled, and failed addons during startup and addon reload under the addon lifecycle phase while routing per-asset and per-definition addon loading details to `DEBUG`.

#### Scenario: Summarizing enabled addons at startup
- **WHEN** `PaperAddonManager` enables discovered directory or JAR addons during plugin startup or addon reload
- **THEN** the lifecycle report displays a consolidated status entry per addon (including ID, version, type, and load duration) without spamming individual YAML definition `INFO` lines
