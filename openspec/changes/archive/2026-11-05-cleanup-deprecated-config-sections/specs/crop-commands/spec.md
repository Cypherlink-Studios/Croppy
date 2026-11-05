# Spec Delta

## MODIFIED Requirements

### Requirement: Interactive Inspection Tool
The system SHALL support an interactive inspection tool item defined in the item registry and tagged with PersistentDataContainer key `croppy:item_id` set to `croppy:inspection_tool` that inspects crops on block right-click, distributable through the centralized item command without relying on dedicated tool appearance keys in `config.yml`.

#### Scenario: Issue inspection tool
- **WHEN** an administrator runs `/croppy item give tools croppy:inspection_tool`
- **THEN** the system places the configured inspection tool item into the player's inventory.

#### Scenario: Right-click crop with inspection tool
- **WHEN** a player holding the inspection tool right-clicks a registered crop block
- **THEN** the interaction is cancelled, preventing block placement or modification, and the crop's real-time status card is displayed.

#### Scenario: Right-click non-crop block with tool
- **WHEN** a player holding the inspection tool right-clicks an unregistered block
- **THEN** the interaction is cancelled and a notification indicates no crop exists at the targeted block.

### Requirement: Configuration Hot Reload
The system SHALL support reloading configuration settings (`growth`, `storage`, and `locale` blocks in `config.yml`), YAML crop definitions, item definitions across all categories, and localization resource files at runtime via `/croppy reload`.

#### Scenario: Successful configuration reload
- **WHEN** an administrator runs `/croppy reload`
- **THEN** the system re-reads `config.yml`, reloads all crop and item definitions from disk, atomically refreshes the localization message catalog, and sends a localized confirmation message.

#### Scenario: Failed reload due to syntax error
- **WHEN** an administrator runs `/croppy reload` with an invalid definition YAML file or corrupted locale file
- **THEN** the error is logged to console and an error notification is sent to the sender while preserving existing valid registrations and catalogs.
