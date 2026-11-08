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

### Requirement: Culinary Recipe Book Player Command
The system SHALL provide a command `/croppy recipes` allowing players to open their culinary compendium cookbook interface directly.

#### Scenario: Open recipe compendium via command
- **WHEN** a player executes `/croppy recipes`
- **THEN** the culinary compendium GUI opens for that player displaying their discovered recipes

#### Scenario: Console execution rejection
- **WHEN** `/croppy recipes` is executed from the server console
- **THEN** the system rejects execution indicating that only in-game players can view the recipe book

### Requirement: Recipe Knowledge Administrative Commands
The system SHALL provide administrative commands `/croppy recipe give <player> <recipe_id>` and `/croppy recipe revoke <player> <recipe_id>` to inspect, grant, or revoke recipe knowledge.

#### Scenario: Admin grants recipe discovery
- **WHEN** an administrator runs `/croppy recipe give Steve croppy:tomato_soup`
- **THEN** recipe `croppy:tomato_soup` is marked as discovered for Steve and a confirmation message is displayed

#### Scenario: Admin revokes recipe discovery
- **WHEN** an administrator runs `/croppy recipe revoke Steve croppy:tomato_soup`
- **THEN** Steve's knowledge state for `croppy:tomato_soup` is reset to unknown and a confirmation message is displayed

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

### Requirement: Resource Pack Management Commands
The system SHALL provide administrative commands under `/croppy pack` to build the unified resource pack from discovered addon assets and dispatch the pack to connected players, requiring permission node `croppy.admin.pack`.

#### Scenario: Building the unified resource pack
- **WHEN** an administrator runs `/croppy pack build`
- **THEN** the system triggers asynchronous compilation of the resource pack, writes `croppy-unified-pack.zip` to the exports directory, updates the active SHA-1 checksum in memory, and outputs compilation time, byte size, and asset count formatted with MiniMessage

#### Scenario: Dispatching resource pack to target player
- **WHEN** an administrator runs `/croppy pack send <player>` with a valid online player name
- **THEN** the system sends the active resource pack prompt to that player and notifies the command sender

#### Scenario: Dispatching resource pack to all players from console
- **WHEN** the server console runs `/croppy pack send` without specifying a player
- **THEN** the system dispatches the resource pack prompt to all online players and outputs confirmation to the console

#### Scenario: Unauthorized pack command execution
- **WHEN** a player lacking permission `croppy.admin.pack` executes `/croppy pack build` or `/croppy pack send`
- **THEN** the system denies execution and returns a localized unauthorized error message

### Requirement: Interactive Command Listing and Output Formatting
The system SHALL format all command list outputs (`/croppy item list`, `/croppy recipe list`, `/croppy addon list`) using unified list headers `<gold><bold>=== <Title> (<count>) ===</bold></gold>` and bulleted items ` <dark_gray>•</dark_gray> <yellow><key></yellow> - <info>`, embedding interactive MiniMessage hover tooltips and click actions on all registered identifiers.

#### Scenario: Interactive item list entry
- **WHEN** an administrator runs `/croppy item list`
- **THEN** each displayed item key includes a hover tooltip with item details and a click action that suggests the corresponding `/croppy item give` command.

#### Scenario: Interactive recipe list entry
- **WHEN** an administrator runs `/croppy recipe list`
- **THEN** each displayed recipe key includes a hover tooltip with station/category info and a click action suggesting the grant command.

#### Scenario: Interactive addon list entry
- **WHEN** an administrator runs `/croppy addon list`
- **THEN** each displayed addon ID includes a hover tooltip and a click action that executes `/croppy addon status <id>`.

### Requirement: Localized Command Execution Feedback
The system SHALL deliver all command execution feedback, success messages, administrative notifications, and errors (including addon lifecycle actions and recipe transfers) exclusively through `PaperMessageService` using catalog translation keys prepended with `general.prefix`.

#### Scenario: Localized addon lifecycle feedback
- **WHEN** an administrator executes `/croppy addon load`, `/croppy addon enable`, `/croppy addon disable`, or `/croppy addon unload`
- **THEN** the outcome is reported using localized translation keys with dynamic arguments and `general.prefix`.

#### Scenario: Localized recipe unlock notification
- **WHEN** an administrator unlocks a recipe for a target player via `/croppy recipe give`
- **THEN** the target player receives a localized notification prepended with `general.prefix` in their client locale.
