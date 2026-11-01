# Spec Delta: crop-commands

## ADDED Requirements

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
