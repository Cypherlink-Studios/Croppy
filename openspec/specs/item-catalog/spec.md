# item-catalog Specification

## Purpose
Governs the interactive item catalog GUI, multi-action administrative item spawning, category tab navigation, horizontal category pagination, real-time search, and dynamic lore augmentation.

## Requirements

### Requirement: Menu Engine Item Catalog Integration
The system SHALL render the interactive item catalog through `menu-system` YAML configuration mapping dynamic item display slots, category navigation tabs, pagination controls, and active tab highlights to inventory GUI components.

#### Scenario: Open catalog menu displaying items
- **WHEN** an authorized administrator runs `/croppy items`
- **THEN** the configured `items_menu.yml` opens displaying the first page of items with the "all" category active.

#### Scenario: Item pagination navigation
- **WHEN** an administrator clicks the next page action button in the catalog
- **THEN** the inventory view updates to display the subsequent page of item entries.

#### Scenario: Category tab selection and filtering
- **WHEN** an administrator clicks a category tab button (e.g., `seeds`, `watering_cans`)
- **THEN** the items grid filters to show only items belonging to that category and the page resets to 1.

#### Scenario: Active category visual indicator
- **WHEN** an administrator filters the catalog by a specific category
- **THEN** the active category tab icon renders with an enchantment glint and an active indicator tag.

#### Scenario: Horizontal category pagination
- **WHEN** registered item categories exceed the available category slots in the menu header
- **THEN** horizontal category navigation controls allow switching between category tab pages.

### Requirement: Multi-Action Administrative Spawning
The system SHALL support specialized click actions on catalog item cards to spawn, inspect, or drop items directly for players with permission `croppy.admin.items`.

#### Scenario: Left-click gives single item
- **WHEN** an administrator left-clicks an item card in the catalog
- **THEN** the system creates 1 unit of the item, adds it to the player's inventory, and plays an item pickup sound.

#### Scenario: Right-click gives maximum stack
- **WHEN** an administrator right-clicks an item card in the catalog
- **THEN** the system creates a full stack (up to the item's maximum stack size) and places it into the player's inventory.

#### Scenario: Shift-click suggests command in chat
- **WHEN** an administrator shift-clicks an item card in the catalog
- **THEN** the system sends a MiniMessage chat message containing an interactive suggest-command click action pre-filled with `/croppy item give <category> <item_id> 1`.

#### Scenario: Drop key drops item at feet
- **WHEN** an administrator presses the drop key (Q) while hovering over an item card
- **THEN** the system spawns the item entity naturally on the ground at the player's location.

#### Scenario: Full inventory overflow drops naturally
- **WHEN** an administrator clicks to receive an item but their inventory has no remaining space
- **THEN** the overflow items are dropped naturally at the player's feet without loss.

### Requirement: Dynamic Administrative Lore Augmentation
The system SHALL dynamically append administrative action instructions to item lore in the catalog without altering the original `ItemDefinition` or stored item metadata.

#### Scenario: Render administrative action shortcuts in lore
- **WHEN** rendering an item card in the catalog
- **THEN** the item displays its defined display name and lore followed by a separator and localized shortcut hints for Left-Click (1x), Right-Click (64x), Shift-Click (Suggest command), and Drop (Drop at feet).

### Requirement: Real-Time Catalog Search
The system SHALL support filtering items by text query matching item display names, material keys, and namespaced keys using interactive chat prompts.

#### Scenario: Initiate search prompt
- **WHEN** an administrator clicks the search action button in the catalog
- **THEN** the inventory view closes temporarily and a chat prompt requests the search query.

#### Scenario: Filter items by text query
- **WHEN** an administrator enters a search query in response to the prompt
- **THEN** the catalog re-opens displaying only items matching the query across display name or key.

#### Scenario: Reset search filter
- **WHEN** an administrator clicks the clear filter action button or enters an empty search query
- **THEN** the search filter is cleared and the full category items are displayed.
