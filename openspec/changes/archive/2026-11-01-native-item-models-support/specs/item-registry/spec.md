# Spec Delta: item-registry

## MODIFIED Requirements

### Requirement: Declarative Item Definition Schema
The system SHALL represent custom items using an immutable `ItemDefinition` identified by a namespaced `Key`, belonging to a category, specifying Minecraft material, optional custom model data integer, optional native item model `Key`, MiniMessage display name, lore lines, and custom PersistentDataContainer (PDC) metadata key-value pairs.

#### Scenario: Parse valid item configuration from YAML
- **WHEN** an item YAML definition is loaded from `items/<category>/<item_id>.yml`
- **THEN** the system parses the item into an `ItemDefinition` with its category, material, display name, lore, and custom PDC mappings

#### Scenario: Parse native item model key from YAML
- **WHEN** an item YAML definition specifies `item-model` or `item_model` (e.g., `croppy:copper_can` or `croppy:item/copper_can`)
- **THEN** the system parses the value into a namespaced `Key` stored in `ItemDefinition#itemModel()`

#### Scenario: Reject item definition with invalid or missing material
- **WHEN** an item YAML definition specifies an invalid or null Minecraft material
- **THEN** the system logs a descriptive validation warning and rejects the definition from the registry

## ADDED Requirements

### Requirement: Native Item Model Application
The system SHALL set the native `item_model` Data Component on the item stack's `ItemMeta` whenever an `ItemDefinition` specifies an `itemModel` key, while preserving `custom_model_data` if also defined.

#### Scenario: Apply native item model to ItemStack
- **WHEN** an `ItemStack` is constructed from an `ItemDefinition` with an `itemModel` key
- **THEN** the resulting `ItemMeta` has its item model set to the corresponding Bukkit `NamespacedKey`

#### Scenario: Coexistence with custom model data
- **WHEN** an `ItemDefinition` defines both `customModelData` and `itemModel`
- **THEN** the resulting `ItemMeta` sets both `custom_model_data` and the native item model without conflict
