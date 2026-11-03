# Spec Delta: item-registry

## ADDED Requirements

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

## MODIFIED Requirements

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
