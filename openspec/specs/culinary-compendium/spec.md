# culinary-compendium Specification

## Purpose

Governs the player-facing cookbook interface, dynamic pagination, category filtering, search, and state-aware visual representations of culinary recipes.

## Requirements

### Requirement: Menu Engine Recipe Catalog Integration
The system SHALL render the culinary compendium via `menu-system` YAML configuration mapping dynamic recipe display slots, pagination navigation, dynamic category registry filters, and active tab highlights to inventory GUI components.

#### Scenario: Open compendium menu
- **WHEN** a player executes the recipe book command or clicks a station compendium button
- **THEN** the configured `recipes_menu.yml` opens displaying the first page of accessible recipes

#### Scenario: Pagination navigation
- **WHEN** a player clicks the next page action button
- **THEN** the inventory view updates to display the subsequent page of recipe entries

#### Scenario: Category filtering
- **WHEN** a player clicks a category filter button such as baking or soups
- **THEN** the recipe entries are filtered to match the selected category and the page resets to 1

#### Scenario: Active category visual indicator
- **WHEN** a player filters the compendium by a specific category
- **THEN** the active category tab icon renders with an enchantment glint and an active indicator tag

#### Scenario: Category horizontal pagination
- **WHEN** registered recipe categories exceed available category slots in the menu header
- **THEN** category navigation controls allow switching between category tab pages

### Requirement: State-Aware Recipe Presentation
The system SHALL dynamically render recipe item representations according to the player's knowledge state (`DISCOVERED`, `HINTED`, or `UNKNOWN`), resolving localized item names, workstation names, registered recipe category display names, hint descriptions, thermal boundaries, and total cooking durations as parsed Adventure components without exposing literal MiniMessage tags.

#### Scenario: Render discovered recipe
- **WHEN** rendering a recipe the player has discovered
- **THEN** the item icon displays the resulting food item with localized display names for ingredients, station name, operating thermal range, duration in seconds, and cooking steps

#### Scenario: Render discovered recipe category with MiniMessage styling
- **WHEN** rendering a `DISCOVERED` recipe card whose category is registered with MiniMessage formatting tags (such as `<aqua>Soups & Stews</aqua>`)
- **THEN** the category lore line renders the parsed styled text without literal MiniMessage tags in the plain text output

#### Scenario: Render hinted recipe
- **WHEN** rendering a recipe in the `HINTED` state
- **THEN** the item icon displays the recipe's configured hint presentation displaying partial clues and mysterious lore

#### Scenario: Render hinted recipe category and hint description with MiniMessage styling
- **WHEN** rendering a `HINTED` recipe card or delivering culinary hint feedback containing MiniMessage tags in the category display name or hint description
- **THEN** both the category name and hint description render as parsed styled text without literal MiniMessage tags

#### Scenario: Render unknown recipe
- **WHEN** rendering an unknown recipe on a server with `compendium.show-unknown-recipes` enabled
- **THEN** a placeholder question-mark icon displays with hidden details indicating an undiscovered preparation

#### Scenario: Render unknown recipe category with MiniMessage styling
- **WHEN** rendering an `UNKNOWN` recipe card whose category is registered with MiniMessage formatting tags
- **THEN** the category lore line resolves the registered category display name and renders it as parsed styled text without literal MiniMessage tags

#### Scenario: Fallback for unregistered recipe category key
- **WHEN** rendering a recipe card whose category key is not present in the category registry
- **THEN** the category lore line safely falls back to the category key's value segment without failing

#### Scenario: Render unknown recipe with visibility disabled
- **WHEN** rendering the compendium on a server with `compendium.show-unknown-recipes` disabled
- **THEN** undiscovered recipes are omitted entirely from the compendium pages

### Requirement: Inventory Resource Availability Indicator
The system SHALL indicate in the compendium recipe lore whether the viewing player currently possesses the required input items and is standing within proximity of a compatible station.

#### Scenario: Ingredients present in player inventory
- **WHEN** a player views a discovered recipe and has all required ingredients in their inventory
- **THEN** the lore displays the inputs highlighted in green with satisfied checkmarks

#### Scenario: Ingredients missing from player inventory
- **WHEN** a player views a discovered recipe and lacks one or more required ingredients
- **THEN** the missing inputs are highlighted in red indicating missing components

#### Scenario: Compatible station nearby
- **WHEN** a player views a recipe and is standing within 5 blocks of a compatible workstation
- **THEN** the lore displays the station requirement highlighted in green with a nearby indicator

#### Scenario: Compatible station not nearby
- **WHEN** a player views a recipe and is not within 5 blocks of a compatible workstation
- **THEN** the lore displays the station requirement indicating no nearby apparatus was detected

### Requirement: Recipe Detail Inspection Interface
The system SHALL open an interactive recipe detail inspection menu when a player clicks an unlocked recipe card in the compendium grid, displaying localized result yields and parsed category display names alongside sequential cooking phases, required temperatures, timeouts, manual interaction prompts, and spoil policies.

#### Scenario: Open recipe inspection detail view
- **WHEN** a player clicks a discovered recipe item in the compendium grid
- **THEN** the system opens the recipe detail inspection menu displaying the sequential timeline of process phases and ingredient requirements

#### Scenario: Render detail result card category and yields
- **WHEN** rendering the result card in the recipe detail inspection menu for a recipe with a registered MiniMessage-styled category
- **THEN** the result card lore renders the localized yield and category lines with parsed category formatting and no literal MiniMessage tags

#### Scenario: Back navigation from detail view
- **WHEN** a player clicks the back button in the recipe detail inspection menu
- **THEN** the system reopens the compendium at the player's previous category and page index

#### Scenario: Click unknown recipe card
- **WHEN** a player clicks an unknown or hinted recipe card in the compendium grid
- **THEN** the interaction is cancelled without opening the detail view

### Requirement: Compendium Query and Inventory Filtering
The system SHALL support searching recipes by text query matching dish names, result items, and ingredients, as well as filtering to display only recipes ready for immediate crafting based on player inventory contents.

#### Scenario: Search recipes by query
- **WHEN** a player executes `/croppy recipes search <query>`
- **THEN** the compendium displays all discovered recipes matching the search term across name, result item, or ingredients

#### Scenario: Filter recipes ready to cook
- **WHEN** a player toggles the ready-to-cook filter
- **THEN** the compendium displays only discovered recipes for which the player possesses all required ingredient quantities

### Requirement: Recipe Category Registry Port
The system SHALL provide a decoupled `RecipeCategoryRegistry` port in `croppy-api` implemented by `DefaultRecipeCategoryRegistry` in `croppy-core`, enabling programmatic and addon registration of custom recipe categories with custom icons and localized display names.

#### Scenario: Lookup registered recipe category
- **WHEN** querying the category registry with a registered category key
- **THEN** the system returns the corresponding `RecipeCategory` instance with its icon key and display name

#### Scenario: Register custom addon category
- **WHEN** an addon registers a custom recipe category via `AddonContext`
- **THEN** the category is added to `RecipeCategoryRegistry` and made available for filtering and command completion
