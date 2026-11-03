# Spec Delta

## ADDED Requirements

### Requirement: Bundled Default Recipe Configurations
The system SHALL provide default bundled recipe YAML configurations in plugin resources and unpack them into the server's `recipes/` directory during startup if absent, registering them into the `RecipeRegistry`.

#### Scenario: Unpack default recipes on startup
- **WHEN** the server starts and the `recipes/` directory does not contain default recipes
- **THEN** the system unpacks `tomato_soup.yml`, `vegetable_stew.yml`, and `fried_egg.yml` from plugin resources and registers them into the `RecipeRegistry`

#### Scenario: Preserve customized recipe files
- **WHEN** a default recipe file already exists in the `recipes/` directory
- **THEN** the system preserves the existing file without overwriting it and registers its configured definition
