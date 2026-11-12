# Spec Delta: culinary-compendium

## MODIFIED Requirements

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
