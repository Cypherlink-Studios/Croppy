# item-registry Specification

## Purpose
Provides a centralized, categorized custom item registry with declarative YAML definitions, standardized namespaced PersistentDataContainer tagging, and item generation services.

## Requirements

### Requirement: Declarative Item Definition Schema
The system SHALL represent custom items using an immutable `ItemDefinition` identified by a namespaced `Key`, belonging to a category, specifying Minecraft material, optional custom model data, MiniMessage display name, lore lines, and custom PersistentDataContainer (PDC) metadata key-value pairs.

#### Scenario: Parse valid item configuration from YAML
- **WHEN** an item YAML definition is loaded from `items/<category>/<item_id>.yml`
- **THEN** the system parses the item into an `ItemDefinition` with its category, material, display name, lore, and custom PDC mappings

#### Scenario: Reject item definition with invalid or missing material
- **WHEN** an item YAML definition specifies an invalid or null Minecraft material
- **THEN** the system logs a descriptive validation warning and rejects the definition from the registry

### Requirement: Categorized In-Memory Item Registry
The system SHALL maintain a thread-safe registry of item definitions, allowing items to be registered, retrieved by unique Key, and queried or listed by category.

#### Scenario: Retrieve item definition by namespaced key
- **WHEN** an item definition is queried with its unique Key
- **THEN** the system returns an `Optional` containing the `ItemDefinition` if present, or empty if absent

#### Scenario: Query all items within a category
- **WHEN** items are requested for a specific category (e.g., `seeds`, `fertilizers`, `watering_cans`, `tools`)
- **THEN** the system returns an unmodifiable collection of all definitions belonging to that category

### Requirement: Standardized Persistent Data Container Tagging
The system SHALL tag every custom item stack created from an `ItemDefinition` with PersistentDataContainer keys `croppy:item_id` (String representation of the Key), `croppy:category` (String name of the category), and all defined custom PDC entries.

#### Scenario: Generate item stack with standardized tags
- **WHEN** an `ItemStack` is produced from an `ItemDefinition`
- **THEN** its `ItemMeta` PDC contains `croppy:item_id` equal to the definition key and `croppy:category` equal to its category

#### Scenario: Identify custom item from ItemStack
- **WHEN** an `ItemStack` is inspected by an item service or listener
- **THEN** the system determines its item definition key and category by reading the PDC tags `croppy:item_id` and `croppy:category`
