# item-registry Specification

## MODIFIED Requirements

### Requirement: Declarative Item Definition Schema
The system SHALL represent custom items using an immutable `ItemDefinition` identified by a namespaced `Key`, belonging to a category, specifying Minecraft material, optional custom model data integer, optional native item model `Key`, optional skull texture string, MiniMessage display name, lore lines, custom PersistentDataContainer (PDC) metadata key-value pairs, and optional `ConsumableProperties` (food, consumption behavior, and effect entries).

#### Scenario: Parse valid item configuration from YAML
- **WHEN** an item YAML definition is loaded from `items/<category>/<item_id>.yml`
- **THEN** the system parses the item into an `ItemDefinition` with its category, material, display name, lore, and custom PDC mappings

#### Scenario: Parse native item model key from YAML
- **WHEN** an item YAML definition specifies `item-model` or `item_model` (e.g., `croppy:copper_can` or `croppy:item/copper_can`)
- **THEN** the system parses the value into a namespaced `Key` stored in `ItemDefinition#itemModel()`

#### Scenario: Parse custom skull texture from YAML
- **WHEN** an item YAML definition specifies `texture: "<base64_or_url>"` or `skull-texture: "<base64_or_url>"`
- **THEN** the system stores the texture string in `ItemDefinition#skullTexture()`

#### Scenario: Parse optional consumable properties from YAML
- **WHEN** an item YAML definition specifies `food` (or `nutrition`), `consumable`, or `effects` sections
- **THEN** the system parses them into `ItemDefinition#consumableProperties()`, returning empty when omitted

#### Scenario: Reject item definition with invalid or missing material
- **WHEN** an item YAML definition specifies an invalid or null Minecraft material
- **THEN** the system logs a descriptive validation warning and rejects the definition from the registry

## ADDED Requirements

### Requirement: Native Consumable Data Component Application
The system SHALL apply Paper native `FOOD`, `CONSUMABLE` (with empty vanilla consume-effects), `USE_REMAINDER`, and `USE_COOLDOWN` Data Components when constructing an `ItemStack` from an `ItemDefinition` (or `StoredItem` with quality) that declares consumable properties.

#### Scenario: Materialize custom drink on non-food base material
- **WHEN** an `ItemStack` is created for an item with `material: "POTION"`, `food` nutrition, `animation: "DRINK"`, `consume-seconds: 0.8`, and `remainder: "minecraft:glass_bottle"`
- **THEN** the resulting item carries native `FOOD`, `CONSUMABLE` (0.8s DRINK animation with empty native consume-effects), and `USE_REMAINDER` components

#### Scenario: Apply deterministic Data Components for item stacking
- **WHEN** two `ItemStack` instances are created from the same `ItemDefinition` and identical `StoredItem` quality
- **THEN** both stacks receive identical Data Components and remain stackable up to the material stack limit

### Requirement: Optional Consumable Auto-Lore Formatting
The system SHALL optionally append localized nutrition and effect summary lore lines when `consumables.auto-lore` is enabled in configuration, preserving manual YAML lore when disabled.

#### Scenario: Preserve manual lore when auto-lore is disabled
- **WHEN** `consumables.auto-lore` is false and an item defines custom lore and `food` attributes
- **THEN** the constructed `ItemStack` displays only the declared YAML lore and metadata contributor lines
