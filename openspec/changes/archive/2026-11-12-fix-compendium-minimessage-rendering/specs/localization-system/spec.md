# Spec Delta: localization-system

## MODIFIED Requirements

### Requirement: Content Definition Localization Overrides
The system SHALL support localized display names and lore lines for custom items, crop species, recipe categories, and stations by checking for content translation keys before falling back to native definition fields parsed as MiniMessage components.

#### Scenario: Localized item name resolution
- **WHEN** generating an item stack or GUI element for a player and a translation key `content.item.<namespace>.<value>.name` exists for their locale
- **THEN** the item displays the localized name instead of the default display name from its item definition YAML.

#### Scenario: Fallback to definition default name
- **WHEN** no content localization override exists for the target locale
- **THEN** the item retains its default display name as declared in the content YAML file.

#### Scenario: Localized recipe category name resolution
- **WHEN** resolving a registered recipe category display name for a player and a translation key `content.category.<namespace>.<value>.name` exists for their locale
- **THEN** the system returns the localized override deserialized as a MiniMessage component.

#### Scenario: Fallback to registered recipe category display name
- **WHEN** no `content.category.<namespace>.<value>.name` translation override exists for a registered recipe category
- **THEN** the system deserializes the category's registered MiniMessage display name into an Adventure component so nested tags render cleanly inside message templates.

#### Scenario: Fallback for unregistered recipe category key
- **WHEN** resolving a category key that is missing from the category registry and has no translation override
- **THEN** the system returns a plain component containing the category key's value segment.
