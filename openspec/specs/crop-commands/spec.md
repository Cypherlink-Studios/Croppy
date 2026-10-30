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
The system SHALL support an interactive inspection tool item tagged with PersistentDataContainer key `croppy:item_id` set to `croppy:inspection_tool` that inspects crops on block right-click, distributable through the centralized item command.

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
The system SHALL support reloading configuration settings, YAML crop definitions, and item definitions across all categories at runtime via `/croppy reload`.

#### Scenario: Successful configuration reload
- **WHEN** an administrator runs `/croppy reload`
- **THEN** the system re-reads `config.yml`, reloads all crop and item definitions from disk, and sends a confirmation message.

#### Scenario: Failed reload due to syntax error
- **WHEN** an administrator runs `/croppy reload` with an invalid definition YAML file
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

### Requirement: Centralized Item Give Command
The system SHALL provide `/croppy item give <category> <item_id> [quantity] [--target <player>] [--silent]` with dynamic Cloud 2.0 tab completion, granting custom items to the target player (defaulting to command sender).

#### Scenario: Give item to command sender by default
- **WHEN** an administrator player runs `/croppy item give watering_cans croppy:copper_watering_can`
- **THEN** 1 copper watering can is placed into the executing player's inventory and a success message is displayed.

#### Scenario: Give item with target and quantity flags
- **WHEN** an administrator runs `/croppy item give fertilizers croppy:speed_gro 5 --target Steve`
- **THEN** 5 Speed-Gro fertilizer items are added to Steve's inventory, dropping at feet if full, and confirmation is sent to the administrator.

#### Scenario: Give item silently
- **WHEN** an administrator executes `/croppy item give seeds croppy:wheat_seeds 1 --target Steve --silent`
- **THEN** the item is added to Steve's inventory without sending a notification message to Steve.

#### Scenario: Dynamic autocompletion for categories and items
- **WHEN** a user enters `/croppy item give ` and presses tab
- **THEN** the system suggests all registered categories, and after choosing a category, suggests only item IDs registered under that category.

### Requirement: Item Registry Listing Command
The system SHALL provide `/croppy item list [category]` to display registered item definitions and their categories.

#### Scenario: List all registered items
- **WHEN** an administrator runs `/croppy item list`
- **THEN** the system displays all registered items grouped by category with their formatted display names and keys.

#### Scenario: List items filtered by category
- **WHEN** an administrator runs `/croppy item list seeds`
- **THEN** the system displays only items registered under the `seeds` category.
