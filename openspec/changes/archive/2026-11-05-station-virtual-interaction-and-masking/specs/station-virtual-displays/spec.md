# Spec Delta: station-virtual-displays

## ADDED Requirements

### Requirement: Base Block Client Masking
The system SHALL mask the physical station base block with a client-side packet block change for viewing players when `hide_base_block` is enabled in the station visual configuration.

#### Scenario: Conceal physical base block when player enters viewing range
- **WHEN** a player enters the viewing range of a station configured with `hide_base_block: true`
- **THEN** a block change packet representing an invisible barrier or air is dispatched to the player's connection while the server world retains the registered base block

#### Scenario: Restore real block appearance when station is broken or unregistered
- **WHEN** a station with base block masking is broken or removed from the world
- **THEN** virtual display entities are destroyed and block state updates reflect the true world state for tracking players

### Requirement: Virtual Interaction Entity and Packet Routing
The system SHALL project a client-side virtual `INTERACTION` entity for stations configured with an interaction hitbox, capturing player click packets and routing them to station destruction or gameplay interactions.

#### Scenario: Project virtual interaction hitbox for station viewers
- **WHEN** a player tracks a station configured with visual interaction dimensions
- **THEN** an `INTERACTION` entity is spawned at the station position with the configured width and height bounding box

#### Scenario: Route attack packet to station dismantling
- **WHEN** an inbound `INTERACT_ENTITY` packet with `ATTACK` action targets a station interaction entity ID
- **THEN** the station is dismantled on the main thread, dropping the station item and any stored inputs/outputs with break effects

#### Scenario: Route interact packet to station interaction pipeline
- **WHEN** an inbound `INTERACT_ENTITY` packet with `INTERACT` action targets a station interaction entity ID
- **THEN** the interaction is forwarded on the main thread to the declarative interaction router or station GUI and cooking handlers
