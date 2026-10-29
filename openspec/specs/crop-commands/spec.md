# crop-commands Specification

## Purpose
Provides administrative commands, interactive inspection tools, debugging controls, hot-reload routines, and crop item distribution for Croppy.

## Requirements

### Requirement: Command Dispatch and Registration
The system SHALL register root command `/croppy` with subcommands using Incendo Cloud Paper 2.0 targeting `CommandSourceStack`, enforcing permission nodes for all administrative operations.

#### Scenario: Unauthorized player execution
- **WHEN** a player without permission `croppy.admin` runs `/croppy reload` or `/croppy inspect`
- **THEN** the system denies execution and sends an unauthorized error message.

#### Scenario: Command syntax feedback
- **WHEN** an administrator runs `/croppy` without subcommands or with invalid arguments
- **THEN** the system outputs command suggestions and usage help.

### Requirement: In-game Crop Inspection
The system SHALL provide an inspection command `/croppy inspect [x y z]` that displays real-time species, stage progress, environmental light, and water fluid levels using Adventure MiniMessage.

#### Scenario: Inspect by line of sight
- **WHEN** an authorized player points at a registered crop block within 5 blocks and runs `/croppy inspect`
- **THEN** the system resolves the targeted crop and displays its detailed status card.

#### Scenario: Inspect by explicit coordinates
- **WHEN** an administrator runs `/croppy inspect <x> <y> <z>` targeting a registered crop block
- **THEN** the system displays the crop's species, current stage, progress percentage, ambient light level, and water fluid level.

#### Scenario: Inspect non-crop block
- **WHEN** an administrator runs `/croppy inspect` pointing at a block with no registered crop instance
- **THEN** the system sends a message stating that no active crop was found at that location.

### Requirement: Interactive Inspection Tool
The system SHALL provide an inspection tool item via `/croppy tool` tagged with PersistentDataContainer key `croppy:tool` that inspects crops on block right-click.

#### Scenario: Issue inspection tool
- **WHEN** an administrator runs `/croppy tool`
- **THEN** the system places the configured inspection tool item into the player's inventory.

#### Scenario: Right-click crop with inspection tool
- **WHEN** a player holding the inspection tool right-clicks a registered crop block
- **THEN** the interaction is cancelled, preventing block placement or modification, and the crop's real-time status card is displayed.

#### Scenario: Right-click non-crop block with tool
- **WHEN** a player holding the inspection tool right-clicks an unregistered block
- **THEN** the interaction is cancelled and a notification indicates no crop exists at the targeted block.

### Requirement: Configuration Hot Reload
The system SHALL support reloading configuration settings and YAML species definitions at runtime via `/croppy reload`.

#### Scenario: Successful configuration reload
- **WHEN** an administrator runs `/croppy reload`
- **THEN** the system re-reads `config.yml`, reloads all species YAML files into the `SpeciesRegistry`, and sends a confirmation message.

#### Scenario: Failed reload due to syntax error
- **WHEN** an administrator runs `/croppy reload` with an invalid species definition YAML file
- **THEN** the error is logged to console and an error notification is sent to the sender while preserving existing valid registrations.

### Requirement: Debug Growth State Manipulation
The system SHALL provide `/croppy setstage <stageIndex>` and `/croppy setprogress <progress>` to modify targeted crop state and re-synchronize visual display blocks immediately.

#### Scenario: Set stage index
- **WHEN** an administrator looks at a crop and executes `/croppy setstage 2`
- **THEN** the crop's stage is updated to index 2, progress is reset to 0.0, the visual block appearance is synchronized, and a confirmation message is displayed.

#### Scenario: Set progress percentage
- **WHEN** an administrator looks at a crop and executes `/croppy setprogress 0.75`
- **THEN** the crop's progress is updated to 0.75 without changing the stage index, and a confirmation is displayed.

#### Scenario: Out of range stage index
- **WHEN** an administrator executes `/croppy setstage 99` where the species only defines 4 stages
- **THEN** the system rejects the modification and displays an invalid stage index error.

### Requirement: Crop Item Gifting
The system SHALL provide `/croppy give <player> <species> [amount]` to issue species items directly to player inventories.

#### Scenario: Give crop item to online player
- **WHEN** an administrator runs `/croppy give Steve croppy:wheat 3`
- **THEN** 3 items representing `croppy:wheat` are deposited into Steve's inventory, or dropped at feet if inventory is full.

#### Scenario: Unknown species argument
- **WHEN** an administrator runs `/croppy give Steve croppy:unknown 1`
- **THEN** the command fails parsing with an unknown species error message.
