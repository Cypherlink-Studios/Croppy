# Spec Delta

## MODIFIED Requirements

### Requirement: Spatial Crop Instance Binding
The system SHALL track planted crops at unique spatial block positions, associating each instance with a species key, developmental stage index, accumulated progress, timestamps, and multi-block spatial occupancy across all spanned block coordinates.

#### Scenario: Crop instance creation upon planting
- **WHEN** a player plants a registered crop seed on valid farmland
- **THEN** a new CropInstance SHALL be bound to that block position with initial stage 0 and 0.0 progress
- **THEN** the crop SHALL be queryable by its block position

#### Scenario: Duplicate planting at same position prevention
- **WHEN** an attempt is made to bind a crop instance to an already occupied position
- **THEN** the system SHALL reject registration with an IllegalStateException

#### Scenario: Multi-block occupancy registration
- **WHEN** a crop with spatial height greater than 1 is registered
- **THEN** all vertical positions spanned by the height SHALL map to the root crop instance in the occupancy tracker

### Requirement: Crop Harvest and Destruction Lifecycle
The system SHALL handle crop destruction and harvesting, dropping configured yields for mature crops or seeds for immature crops, and either reverting the developmental stage for crops with regrowth or unregistering monocarpic instances.

#### Scenario: Mature crop harvest
- **WHEN** a player breaks a tracked crop that is at its mature stage
- **THEN** the system SHALL drop items defined in the species harvest yield
- **THEN** the crop instance SHALL be removed from the active tracker

#### Scenario: Immature crop breakage
- **WHEN** a player or environmental physics breaks an immature crop
- **THEN** the system SHALL drop the species seed item
- **THEN** the crop instance SHALL be removed from the active tracker

#### Scenario: Mature crop harvest with regrowth
- **WHEN** a player harvests a mature crop configured with a valid regrowth target stage
- **THEN** the system SHALL drop items defined in the species harvest yield
- **THEN** the crop instance SHALL remain tracked, its stage index SHALL revert to the target stage, and its progress SHALL reset to 0.0

## ADDED Requirements

### Requirement: Multi-Block Spatial Collision Safeguard
The system SHALL protect all block positions occupied by a multi-block crop against accidental block placement or physical obstruction.

#### Scenario: Block placement into multi-block crop space
- **WHEN** a player attempts to place a block into a coordinate occupied by a multi-block crop's vertical footprint
- **THEN** the system SHALL cancel the block placement event and preserve the crop's space
