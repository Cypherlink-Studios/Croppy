# Spec Delta: culinary-compendium

## Purpose

Governs the player-facing cookbook interface, dynamic pagination, category filtering, search, and state-aware visual representations of culinary recipes.

## ADDED Requirements

### Requirement: Menu Engine Recipe Catalog Integration
The system SHALL render the culinary compendium via `menu-system` YAML configuration mapping dynamic recipe display slots, pagination navigation, and category filters to inventory GUI components.

#### Scenario: Open compendium menu
- **WHEN** a player executes the recipe book command or clicks a station compendium button
- **THEN** the configured `recipes_menu.yml` opens displaying the first page of accessible recipes

#### Scenario: Pagination navigation
- **WHEN** a player clicks the next page action button
- **THEN** the inventory view updates to display the subsequent page of recipe entries

#### Scenario: Category filtering
- **WHEN** a player clicks a category filter button such as baking or soups
- **THEN** the recipe entries are filtered to match the selected category and the page resets to 1

### Requirement: State-Aware Recipe Presentation
The system SHALL dynamically render recipe item representations according to the player's knowledge state (`DISCOVERED`, `HINTED`, or `UNKNOWN`).

#### Scenario: Render discovered recipe
- **WHEN** rendering a recipe the player has discovered
- **THEN** the item icon displays the resulting food item with full lore showing inputs, station type, and cooking steps

#### Scenario: Render hinted recipe
- **WHEN** rendering a recipe in the `HINTED` state
- **THEN** the item icon displays the recipe's configured hint presentation displaying partial clues and mysterious lore

#### Scenario: Render unknown recipe
- **WHEN** rendering an unknown recipe on a server with unknown visibility enabled
- **THEN** a placeholder question-mark icon displays with hidden details indicating an undiscovered preparation

### Requirement: Inventory Resource Availability Indicator
The system SHALL indicate in the compendium recipe lore whether the viewing player currently possesses the required input items and is standing near a compatible station.

#### Scenario: Ingredients present in player inventory
- **WHEN** a player views a discovered recipe and has all required ingredients in their inventory
- **THEN** the lore displays the inputs highlighted in green indicating ready availability

#### Scenario: Ingredients missing from player inventory
- **WHEN** a player views a discovered recipe and lacks one or more required ingredients
- **THEN** the missing inputs are highlighted in red indicating missing components
