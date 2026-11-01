# Spec Delta

## MODIFIED Requirements

### Requirement: Spatial Lifecycle and Persistence
The system SHALL persist station state and attached component data across chunk lifecycles and server restarts, restoring active station data on chunk load.

#### Scenario: Chunk load restore
- **WHEN** a chunk containing registered crafting stations is loaded into memory
- **THEN** all station instances within that chunk are loaded into memory and registered with the station manager

#### Scenario: Chunk unload persistence
- **WHEN** a chunk containing registered crafting stations unloads from memory
- **THEN** all active station instances within that chunk are saved to persistent storage and evicted from active station memory

#### Scenario: Block destruction cleanup
- **WHEN** a player breaks a crafting station block
- **THEN** the station custom item and all stored items drop to the ground, runtime state is cleared, and persistence records are deleted

#### Scenario: Heat source station persistence
- **WHEN** a heat source station with active fuel and temperature settings unloads and reloads
- **THEN** its active status, output temperature, target temperature, and remaining burn ticks are fully restored from binary storage
