# Spec Delta: crop-commands

## MODIFIED Requirements

### Requirement: Culinary Recipe Book Player Command
The system SHALL provide a command `/croppy recipes [action] [query]` allowing players to open their culinary compendium cookbook interface directly, with support for pagination, dynamic category filters, search queries, and inventory-ready filtering.

#### Scenario: Open recipe compendium via command
- **WHEN** a player executes `/croppy recipes`
- **THEN** the culinary compendium GUI opens for that player displaying their discovered recipes

#### Scenario: Console execution rejection
- **WHEN** `/croppy recipes` is executed from the server console
- **THEN** the system rejects execution indicating that only in-game players can view the recipe book

#### Scenario: Filter compendium by category argument
- **WHEN** a player executes `/croppy recipes <category_key>`
- **THEN** the compendium opens filtered to the specified category

#### Scenario: Search compendium by text query
- **WHEN** a player executes `/croppy recipes search <query>`
- **THEN** the compendium opens displaying dishes matching the search term

#### Scenario: Filter compendium by ready dishes
- **WHEN** a player executes `/croppy recipes ready`
- **THEN** the compendium opens showing only dishes the player has ingredients to cook
