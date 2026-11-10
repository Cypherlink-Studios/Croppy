# item-registry Specification

## Purpose
Provides a centralized, categorized custom item registry with declarative YAML definitions, standardized namespaced PersistentDataContainer tagging, and item generation services.

## Requirements

### Requirement: Declarative Item Definition Schema
The system SHALL represent custom items using an immutable `ItemDefinition` identified by a namespaced `Key`, belonging to a category, specifying Minecraft material, optional custom model data integer, optional native item model `Key`, optional skull texture string, MiniMessage display name, lore lines, and custom PersistentDataContainer (PDC) metadata key-value pairs.

#### Scenario: Parse valid item configuration from YAML
- **WHEN** an item YAML definition is loaded from `items/<category>/<item_id>.yml`
- **THEN** the system parses the item into an `ItemDefinition` with its category, material, display name, lore, and custom PDC mappings

#### Scenario: Parse native item model key from YAML
- **WHEN** an item YAML definition specifies `item-model` or `item_model` (e.g., `croppy:copper_can` or `croppy:item/copper_can`)
- **THEN** the system parses the value into a namespaced `Key` stored in `ItemDefinition#itemModel()`

#### Scenario: Parse custom skull texture from YAML
- **WHEN** an item YAML definition specifies `texture: "<base64_or_url>"` or `skull-texture: "<base64_or_url>"`
- **THEN** the system stores the texture string in `ItemDefinition#skullTexture()`

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

### Requirement: Native Item Model Application
The system SHALL set the native `item_model` Data Component on the item stack's `ItemMeta` whenever an `ItemDefinition` specifies an `itemModel` key, while preserving `custom_model_data` if also defined.

#### Scenario: Apply native item model to ItemStack
- **WHEN** an `ItemStack` is constructed from an `ItemDefinition` with an `itemModel` key
- **THEN** the resulting `ItemMeta` has its item model set to the corresponding Bukkit `NamespacedKey`

#### Scenario: Coexistence with custom model data
- **WHEN** an `ItemDefinition` defines both `customModelData` and `itemModel`
- **THEN** the resulting `ItemMeta` sets both `custom_model_data` and the native item model without conflict

### Requirement: Custom Head Texture Application
The system SHALL apply custom skin textures to `player_head` items and placed skull blocks when an `ItemDefinition` specifies a base64 texture string or Mojang texture URL.

#### Scenario: Apply base64 texture to player head item
- **WHEN** an `ItemStack` is constructed for an item definition specifying `material: "minecraft:player_head"` and a base64 texture string
- **THEN** its `SkullMeta` receives a `PlayerProfile` containing the decoded texture property with a deterministic UUID

#### Scenario: Apply Mojang texture URL to player head item
- **WHEN** an `ItemStack` is constructed for an item definition specifying a Mojang texture URL (`http://textures.minecraft.net/texture/...`)
- **THEN** the system encodes the URL into a valid skin texture payload and sets the corresponding `PlayerProfile` on the `SkullMeta`

#### Scenario: Synchronize head texture to in-world skull block
- **WHEN** a custom station with a player head base block is placed in the world
- **THEN** the target block's `Skull` tile state is updated with the definition's texture `PlayerProfile`

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
