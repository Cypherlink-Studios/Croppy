# Spec Delta

## ADDED Requirements

### Requirement: Extensible Command Target Selectors
The system SHALL support extensible `@`-prefixed target selectors on selector-enabled command arguments, evaluating registered selector rules in order before falling back to single-target resolution, and exposing dynamic autocompletion for both selector tokens and single identifiers.

#### Scenario: Resolving an all-targets selector
- **WHEN** an administrator executes a selector-enabled command with `@all`
- **THEN** the system resolves all eligible targets for that command domain and executes the operation across the resolved collection

#### Scenario: Rejecting an unknown or empty selector
- **WHEN** an administrator executes a selector-enabled command with an unrecognized `@`-prefixed token or a valid selector matching zero targets
- **THEN** the system rejects execution with a descriptive localized error rather than falling back to single-key parsing

#### Scenario: Tab-completing selector tokens and individual targets
- **WHEN** an administrator requests tab completion on a selector-enabled argument
- **THEN** the system suggests matching `@`-prefixed selectors alongside valid individual identifiers

## MODIFIED Requirements

### Requirement: Configuration Hot Reload
The system SHALL support reloading Croppy's own configuration settings (`growth`, `storage`, `locale`, and `logging` blocks in `config.yml`), YAML crop definitions, item definitions across all categories, stations, processes, menus, interactions, rules, crafting recipes, and localization resource files at runtime via `/croppy reload`, resetting temporary runtime debug channel overrides back to `config.yml` while leaving active addons and addon-owned registry entries untouched.

#### Scenario: Successful configuration reload
- **WHEN** an administrator runs `/croppy reload`
- **THEN** the system re-reads `config.yml`, reloads all Croppy core definitions from disk, atomically refreshes the localization message catalog, resets runtime debug channels to `config.yml`, preserves all enabled addons and their registered entries without disabling or re-enabling addons, logs a concise reload summary, and sends a localized confirmation message

#### Scenario: Addon entries preserved across core reload
- **WHEN** one or more addons are enabled and an administrator runs `/croppy reload`
- **THEN** all species, items, categories, recipes, crafting recipes, menus, stations, processes, and interactions registered by those addons remain active and registered without triggering addon `onDisable` or `onEnable`

#### Scenario: Failed reload due to syntax error
- **WHEN** an administrator runs `/croppy reload` with an invalid definition YAML file or corrupted locale file
- **THEN** the error is logged to console and an error notification is sent to the sender while preserving existing valid registrations and catalogs

### Requirement: Recipe Knowledge Administrative Commands
The system SHALL provide administrative commands `/croppy recipe give <player> <recipe>` and `/croppy recipe revoke <player> <recipe>` supporting individual recipe keys, `@all`, and `@<categoryKey>` (including unqualified `@<category>` shorthand) to grant or revoke recipe knowledge with concise summary feedback on bulk operations.

#### Scenario: Admin grants recipe discovery
- **WHEN** an administrator runs `/croppy recipe give Steve croppy:tomato_soup`
- **THEN** recipe `croppy:tomato_soup` is marked as discovered for Steve and single-recipe confirmation messages are displayed to both the administrator and Steve

#### Scenario: Admin grants recipes by category selector
- **WHEN** an administrator runs `/croppy recipe give Steve @soups` or `/croppy recipe give Steve @croppy:soups`
- **THEN** all registered recipes in category `croppy:soups` are marked as discovered for Steve and concise summary messages reporting the granted count are sent to the administrator and Steve without per-recipe chat spam

#### Scenario: Admin grants all recipes via selector
- **WHEN** an administrator runs `/croppy recipe give Steve @all`
- **THEN** all registered recipes are marked as discovered for Steve and summary confirmation feedback is sent to both the administrator and Steve

#### Scenario: Admin revokes recipe discovery
- **WHEN** an administrator runs `/croppy recipe revoke Steve croppy:tomato_soup`
- **THEN** Steve's knowledge state for `croppy:tomato_soup` is reset to unknown and a confirmation message is displayed

#### Scenario: Admin revokes recipes by category or all selector
- **WHEN** an administrator runs `/croppy recipe revoke Steve @soups` or `/croppy recipe revoke Steve @all`
- **THEN** all matching recipe knowledge entries for Steve are reset to unknown and a single summary confirmation is sent to the administrator

### Requirement: Addon Lifecycle Administrative Commands
The system SHALL provide administrative commands under `/croppy addon <list|status|load|unload|enable|disable|reload> [target]` with dynamic tab completion, accepting `<addonId>` or `@all` on lifecycle subcommands and enforcing permission `croppy.admin.addon` for all operations.

#### Scenario: Listing discovered addons
- **WHEN** an administrator executes `/croppy addon list`
- **THEN** the system outputs a formatted table showing each addon's ID, version, type (Java/Content Pack), lifecycle state, and dependencies

#### Scenario: Displaying detailed addon status
- **WHEN** an administrator executes `/croppy addon status <addonId>`
- **THEN** the system displays the addon's namespace, authors, loaded classes count, registered registry keys, active listeners, and task statistics

#### Scenario: Enabling an addon in hot
- **WHEN** an administrator executes `/croppy addon enable <addonId>` or `/croppy addon enable @all`
- **THEN** the system enables any pending dependencies first, invokes lifecycle initialization, activates resources, and displays a localized confirmation

#### Scenario: Disabling an addon in hot
- **WHEN** an administrator executes `/croppy addon disable <addonId>` or `/croppy addon disable @all`
- **THEN** the system triggers the 5-phase decommissioning protocol for the target addon(s) and displays a confirmation message

#### Scenario: Unloading an addon in hot
- **WHEN** an administrator executes `/croppy addon unload <addonId>` or `/croppy addon unload @all`
- **THEN** the system disables the target addon(s) if enabled, closes classloaders, severs dependency graph links, and confirms complete unloading

#### Scenario: Hard-reloading an addon or all addons
- **WHEN** an administrator executes `/croppy addon reload <addonId>` or `/croppy addon reload @all`
- **THEN** the system performs a hard reload (unloading target addon(s) and active dependents, rediscovering from disk, and re-enabling in topological dependency order) and reports a localized confirmation summary

#### Scenario: Rejecting legacy unprefixed all literal
- **WHEN** an administrator runs `/croppy addon enable all` or `/croppy addon reload all` without `@` and no addon is named `all`
- **THEN** the system rejects the argument as an unknown addon instead of matching all addons

#### Scenario: Unauthorized addon command execution
- **WHEN** a user without permission `croppy.admin.addon` runs any `/croppy addon` subcommand
- **THEN** execution is rejected with an unauthorized error message
