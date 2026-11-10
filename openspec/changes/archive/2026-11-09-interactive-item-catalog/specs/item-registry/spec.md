# Spec Delta: item-registry

## ADDED Requirements

### Requirement: Item Category Registry Port
The system SHALL provide an extensible `ItemCategoryRegistry` port in `croppy-api` implemented by `DefaultItemCategoryRegistry` in `croppy-core`, maintaining categorized metadata including category key, display icon material, optional native item model, display name, and sorting priority.

#### Scenario: Lookup registered item category
- **WHEN** querying the category registry with a registered category key
- **THEN** the system returns the corresponding `ItemCategory` instance containing its icon, display name, and priority.

#### Scenario: Default category initialization
- **WHEN** the item registry subsystem initializes
- **THEN** default categories (`all`, `seeds`, `fertilizers`, `watering_cans`, `tools`, `ingredients`, `stations`, `misc`) are registered with configured icons and localized display names.

#### Scenario: Register custom addon category
- **WHEN** an addon registers a custom item category via `ItemCategoryRegistry`
- **THEN** the category is added to the registry and made available for catalog navigation and tab filtering.
