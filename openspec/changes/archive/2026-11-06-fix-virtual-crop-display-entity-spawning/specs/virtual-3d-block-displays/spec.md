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

#### Scenario: Reliable PacketWrapper dispatch
- **WHEN** virtual entity spawn, metadata, or destroy packets are dispatched to a player
- **THEN** the system SHALL dispatch them as typed PacketWrapper instances to ensure error-free transmission across the network pipeline

#### Scenario: Immediate projection on player connection
- **WHEN** a player connects to the server and is within tracking range of an active 3D crop
- **THEN** the system SHALL evaluate proximity and dispatch the virtual display entity pair immediately without waiting for background polling cycles
