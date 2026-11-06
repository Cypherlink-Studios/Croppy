# Spec Delta

## ADDED Requirements

### Requirement: Addon Lifecycle Administrative Commands
The system SHALL provide administrative commands under `/croppy addon <list|status|load|unload|enable|disable> [addonId]` with Cloud 2.0 tab completion, enforcing permission `croppy.admin.addon` for all operations.

#### Scenario: Listing discovered addons
- **WHEN** an administrator executes `/croppy addon list`
- **THEN** the system outputs a formatted table showing each addon's ID, version, type (Java/Content Pack), lifecycle state, and dependencies

#### Scenario: Displaying detailed addon status
- **WHEN** an administrator executes `/croppy addon status <addonId>`
- **THEN** the system displays the addon's namespace, authors, loaded classes count, registered registry keys, active listeners, and task statistics

#### Scenario: Enabling an addon in hot
- **WHEN** an administrator executes `/croppy addon enable <addonId>`
- **THEN** the system enables any pending dependencies first, invokes the addon's lifecycle initialization, activates its resources, and displays a success confirmation

#### Scenario: Disabling an addon in hot
- **WHEN** an administrator executes `/croppy addon disable <addonId>`
- **THEN** the system triggers the 5-phase decommissioning protocol for that addon and displays a confirmation message

#### Scenario: Unloading an addon in hot
- **WHEN** an administrator executes `/croppy addon unload <addonId>`
- **THEN** the system disables the addon if enabled, closes its classloader, severs dependency graph links, and confirms complete unloading

#### Scenario: Unauthorized addon command execution
- **WHEN** a user without permission `croppy.admin.addon` runs any `/croppy addon` subcommand
- **THEN** execution is rejected with an unauthorized error message
