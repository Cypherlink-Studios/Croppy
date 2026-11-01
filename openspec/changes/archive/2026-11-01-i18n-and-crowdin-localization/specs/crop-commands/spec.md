# Spec Delta

## MODIFIED Requirements

### Requirement: Configuration Hot Reload
The system SHALL support reloading configuration settings, YAML crop definitions, item definitions across all categories, and localization resource files at runtime via `/croppy reload`.

#### Scenario: Successful configuration reload
- **WHEN** an administrator runs `/croppy reload`
- **THEN** the system re-reads `config.yml`, reloads all crop and item definitions from disk, atomically refreshes the localization message catalog, and sends a localized confirmation message.

#### Scenario: Failed reload due to syntax error
- **WHEN** an administrator runs `/croppy reload` with an invalid definition YAML file or corrupted locale file
- **THEN** the error is logged to console and an error notification is sent to the sender while preserving existing valid registrations and catalogs.

### Requirement: In-game Crop Inspection
The system SHALL provide an inspection command `/croppy inspect [x y z]` that displays real-time species, stage progress, environmental light, and water fluid levels formatted with localized MiniMessage templates.

#### Scenario: Inspect by line of sight
- **WHEN** an authorized player points at a registered crop block within 5 blocks and runs `/croppy inspect`
- **THEN** the system resolves the targeted crop and displays its detailed status card localized to the player's client locale.

#### Scenario: Inspect by explicit coordinates
- **WHEN** an administrator runs `/croppy inspect <x> <y> <z>` targeting a registered crop block
- **THEN** the system displays the crop's species, current stage, progress percentage, ambient light level, and water fluid level in the sender's locale.

#### Scenario: Inspect non-crop block
- **WHEN** an administrator runs `/croppy inspect` pointing at a block with no registered crop instance
- **THEN** the system sends a localized message stating that no active crop was found at that location.
