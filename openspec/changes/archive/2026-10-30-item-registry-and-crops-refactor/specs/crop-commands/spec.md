# Spec Delta

## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Crop Item Gifting
**Reason**: Replaced by the centralized `/croppy item give` command supporting categorized item distribution, flags, and dynamic autocompletion.
**Migration**: Use `/croppy item give <category> <item_id> [quantity] [--target <player>] [--silent]` instead.

## ADDED Requirements

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
