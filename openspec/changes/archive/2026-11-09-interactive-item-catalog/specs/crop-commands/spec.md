# Spec Delta: crop-commands

## ADDED Requirements

### Requirement: Interactive Item Catalog Command
The system SHALL provide an administrative command `/croppy items [action] [query]` to open the interactive item catalog GUI, with tab filtering and navigation controls, requiring permission node `croppy.admin.items`.

#### Scenario: Open default catalog
- **WHEN** an authorized player executes `/croppy items`
- **THEN** the system opens the interactive item catalog GUI displaying all registered items.

#### Scenario: Open catalog filtered by category
- **WHEN** an administrator runs `/croppy items <category>` with a valid category name
- **THEN** the catalog opens directly filtered to the specified category.

#### Scenario: Open catalog search prompt
- **WHEN** an administrator runs `/croppy items search [query]`
- **THEN** if a query is provided, the catalog opens filtered to matching items; otherwise, an interactive chat prompt is started to capture the search query.

#### Scenario: Catalog navigation subcommands
- **WHEN** an administrator executes `/croppy items next` or `/croppy items prev`
- **THEN** the system updates the player's active catalog session to the respective page and re-renders the menu view.

#### Scenario: Console execution rejection
- **WHEN** the server console executes `/croppy items`
- **THEN** execution is rejected with a message stating that the command can only be executed by players.

#### Scenario: Unauthorized player execution
- **WHEN** a player lacking permission `croppy.admin.items` runs `/croppy items`
- **THEN** execution is rejected with a standard localized permission error message.
