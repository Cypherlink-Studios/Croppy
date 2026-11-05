# Spec Delta

## MODIFIED Requirements

### Requirement: Virtual 3D Model and Interaction Entity Pairing
The system SHALL project a pair of client-side virtual entities for each 3D crop and functional block: an ItemDisplay rendering the 3D model and an Interaction entity intercepting player raycasts and clicks, without creating or persisting entities in the world chunk NBT, and SHALL destroy both entities when destroyed by physics, trampling, or withering.

#### Scenario: Spawning virtual display pair
- **WHEN** a player enters tracking range of a crop or block configured with a 3D model
- **THEN** the system SHALL dispatch PacketEvents spawn and metadata packets for an ItemDisplay entity and an Interaction entity positioned at the block center

#### Scenario: Despawning virtual display pair
- **WHEN** a player leaves tracking range or the crop or block is removed
- **THEN** the system SHALL dispatch PacketEvents destroy packets removing both the ItemDisplay and Interaction entities from the client

#### Scenario: Despawning upon physics destruction or withering
- **WHEN** a 3D crop is destroyed by water flow, pistons, farmland trampling, or withering
- **THEN** the system SHALL immediately destroy both virtual entities for all tracking players

## ADDED Requirements

### Requirement: Dynamic Player Viewer Range Tracking
The system SHALL continuously evaluate player proximity to active virtual 3D crop displays, sending spawn packets to newly approaching players and destroy packets to departing players.

#### Scenario: Player moves within viewing distance of active crop
- **WHEN** a player moves within the configured viewing distance of an active 3D crop
- **THEN** the system SHALL register the player as a viewer and dispatch virtual entity spawn packets

#### Scenario: Player moves outside viewing distance of active crop
- **WHEN** a tracked player moves beyond the configured viewing distance of an active 3D crop
- **THEN** the system SHALL unregister the player as a viewer and dispatch virtual entity destroy packets
