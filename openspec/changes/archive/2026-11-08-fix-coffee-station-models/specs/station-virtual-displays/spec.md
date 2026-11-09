# Spec Delta: station-virtual-displays

## MODIFIED Requirements

### Requirement: Virtual Display Entity Projection
The system SHALL manage client-side virtual display entities for crafting stations via PacketEvents without creating or persisting entities in the server world, orienting base model rotations and spatial layer offsets according to the station's dynamic placement yaw.

#### Scenario: Spawn virtual station entities for nearby players
- **WHEN** a player moves within the configured viewing range of a registered crafting station
- **THEN** virtual display spawn and metadata packets are dispatched to the player's connection and no Bukkit entity is created in the world

#### Scenario: Spawn base model aligned with station placement yaw
- **WHEN** a virtual station base model display is spawned for a tracking player
- **THEN** its left-rotation quaternion transformation incorporates the station instance's horizontal placement yaw combined with any configured base transform rotation

#### Scenario: Destroy virtual entities on station removal or chunk unload
- **WHEN** a crafting station is broken by a player or its enclosing chunk is unloaded
- **THEN** virtual entity destruction packets are sent to all tracking players and the virtual IDs are unmapped

#### Scenario: Resolve station base model item via item registry
- **WHEN** a station declares an item-based base model visual layer
- **THEN** the base model item key is resolved against the domain ItemRegistry to construct a configured ItemStack preserving custom item-model metadata
