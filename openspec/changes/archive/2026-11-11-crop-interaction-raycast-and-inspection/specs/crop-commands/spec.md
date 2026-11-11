# Spec Delta

## MODIFIED Requirements

### Requirement: In-game Crop Inspection
The system SHALL provide an inspection command `/croppy inspect [x y z]` that displays real-time species, stage progress, environmental light, and water fluid levels formatted with localized MiniMessage templates, resolving targets by line of sight against virtual interaction entity hitboxes, physical crop blocks, and supporting soil.

#### Scenario: Inspect by line of sight
- **WHEN** an authorized player points at a registered crop block, its virtual interaction entity, or its supporting farmland within 5 blocks without solid block occlusion and runs `/croppy inspect`
- **THEN** the system resolves the targeted crop and displays its detailed status card localized to the player's client locale.

#### Scenario: Inspect by explicit coordinates
- **WHEN** an administrator runs `/croppy inspect <x> <y> <z>` targeting a registered crop block
- **THEN** the system displays the crop's species, current stage, progress percentage, ambient light level, and water fluid level in the sender's locale.

#### Scenario: Inspect non-crop block
- **WHEN** an administrator runs `/croppy inspect` pointing at a block or location with no registered crop instance or virtual interaction entity
- **THEN** the system sends a localized message stating that no active crop was found at that location.

#### Scenario: Inspect by line of sight on 3D crop interaction entity
- **WHEN** an authorized player points directly at an active 3D crop's virtual interaction entity within 5 blocks and runs `/croppy inspect`
- **THEN** the system resolves the targeted crop from the interaction entity bounding box and displays its detailed status card localized to the player's client locale.

### Requirement: Interactive Inspection Tool
The system SHALL support an interactive inspection tool item defined in the item registry and tagged with PersistentDataContainer key `croppy:item_id` set to `croppy:inspection_tool` that inspects crops on physical block right-click or virtual interaction entity right-click, distributable through the centralized item command without relying on dedicated tool appearance keys in `config.yml`.

#### Scenario: Issue inspection tool
- **WHEN** an administrator runs `/croppy item give tools croppy:inspection_tool`
- **THEN** the system places the configured inspection tool item into the player's inventory.

#### Scenario: Right-click crop with inspection tool
- **WHEN** a player holding the inspection tool right-clicks a registered crop block
- **THEN** the interaction is cancelled, preventing block placement or modification, and the crop's real-time status card is displayed.

#### Scenario: Right-click non-crop block with tool
- **WHEN** a player holding the inspection tool right-clicks an unregistered block
- **THEN** the interaction is cancelled and a notification indicates no crop exists at the targeted block.

#### Scenario: Right-click virtual interaction entity with inspection tool
- **WHEN** a player holding the inspection tool right-clicks a crop's virtual interaction entity
- **THEN** the interaction is intercepted, preventing crop harvesting or damage, and the crop's real-time status card is displayed.

### Requirement: Debug Growth State Manipulation
The system SHALL provide `/croppy setstage <stageIndex>` and `/croppy setprogress <progress>` resolving targets by line of sight against virtual interaction entity hitboxes, physical crop blocks, and supporting soil to modify targeted crop state and re-synchronize visual display blocks immediately.

#### Scenario: Set stage index
- **WHEN** an administrator looks at a crop's virtual interaction entity, physical block, or supporting soil and executes `/croppy setstage 2`
- **THEN** the crop's stage is updated to index 2, progress is reset to 0.0, the visual block appearance is synchronized, and a confirmation message is displayed.

#### Scenario: Set progress percentage
- **WHEN** an administrator looks at a crop's virtual interaction entity, physical block, or supporting soil and executes `/croppy setprogress 0.75`
- **THEN** the crop's progress is updated to 0.75 without changing the stage index, and a confirmation is displayed.

#### Scenario: Out of range stage index
- **WHEN** an administrator executes `/croppy setstage 99` where the species only defines 4 stages
- **THEN** the system rejects the modification and displays an invalid stage index error.

## ADDED Requirements

### Requirement: Administrative Crop Care Commands
The system SHALL provide administrative commands `/croppy water` and `/croppy fertilize <fertilizer>` resolving targets by line of sight against virtual interaction entity hitboxes, physical crop blocks, and supporting soil to hydrate or fertilize targeted crops.

#### Scenario: Water crop via line of sight
- **WHEN** an administrator looks at a crop's virtual interaction entity, physical block, or supporting soil and executes `/croppy water`
- **THEN** the targeted crop is hydrated for the current day, supporting farmland moisture is set to maximum, hydration particles are spawned, and a confirmation message is displayed.

#### Scenario: Fertilize crop via line of sight
- **WHEN** an administrator looks at a crop's virtual interaction entity, physical block, or supporting soil and executes `/croppy fertilize <fertilizer>` with a valid fertilizer key
- **THEN** the targeted crop receives the fertilizer, fertilizer particles are spawned, and a confirmation message is displayed.
