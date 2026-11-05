# Spec Delta

## MODIFIED Requirements

### Requirement: Chunk-Bounded In-Memory Crop Lifecycle
The system SHALL bind active in-memory crop instances to chunk lifecycle events, registering crops when their containing chunk loads with their full multi-block spatial footprint and saving and evicting them when their chunk unloads.

#### Scenario: Chunk load registers crops into active memory
- **WHEN** a chunk containing saved crops is loaded by the world
- **THEN** the system SHALL load the crops from storage and register them into the active tracker using their species height and spatial footprint
- **THEN** all vertical coordinates occupied by multi-block crops SHALL be mapped in the occupancy tracker

#### Scenario: Chunk unload evicts crops from active memory
- **WHEN** a chunk containing active crops is unloaded by the world
- **THEN** the system SHALL save the crop states to storage and remove them from the active tracker

#### Scenario: Chunk load triggers immediate visual display synchronization
- **WHEN** a chunk containing 3D crops is loaded
- **THEN** the system SHALL immediately synchronize virtual 3D displays and interaction hitboxes for each restored crop regardless of maturity status

### Requirement: Crop Harvest and Destruction Lifecycle
The system SHALL handle crop destruction and harvesting, dropping configured yields for mature crops or seeds for immature crops, deleting stored records using canonical root positions, setting physical base blocks to air upon virtual dismantling, and either reverting the developmental stage for crops with regrowth or unregistering monocarpic instances.

#### Scenario: Mature crop harvest
- **WHEN** a player breaks a tracked crop that is at its mature stage
- **THEN** the system SHALL drop items defined in the species harvest yield
- **THEN** the crop instance SHALL be removed from the active tracker and deleted from persistence using its root position

#### Scenario: Immature crop breakage
- **WHEN** a player or environmental physics breaks an immature crop
- **THEN** the system SHALL drop the species seed item
- **THEN** the crop instance SHALL be removed from the active tracker and deleted from persistence using its root position

#### Scenario: Mature crop harvest with regrowth
- **WHEN** a player harvests a mature crop configured with a valid regrowth target stage
- **THEN** the system SHALL drop items defined in the species harvest yield
- **THEN** the crop instance SHALL remain tracked, its stage index SHALL revert to the target stage, and its progress SHALL reset to 0.0

#### Scenario: Virtual dismantling clears physical base block
- **WHEN** a player dismantles a virtual 3D crop via packet interaction
- **THEN** the system SHALL set the physical base block in the world to air and remove all virtual display entities

### Requirement: Multi-Block Spatial Collision Safeguard
The system SHALL protect all block positions occupied by a multi-block crop against accidental block placement, physical obstruction, or planting in obstructed spaces.

#### Scenario: Block placement into multi-block crop space
- **WHEN** a player attempts to place a block into a coordinate occupied by a multi-block crop's vertical footprint
- **THEN** the system SHALL cancel the block placement event and preserve the crop's space

#### Scenario: Planting prevented when vertical footprint is obstructed
- **WHEN** a player attempts to plant a multi-block crop whose vertical footprint collides with a non-air block or an existing tracked crop
- **THEN** the system SHALL cancel the plant event and prevent registration without unhandled exceptions
