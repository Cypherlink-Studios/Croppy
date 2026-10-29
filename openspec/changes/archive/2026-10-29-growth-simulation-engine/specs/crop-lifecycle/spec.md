# Spec Delta: crop-lifecycle

## MODIFIED Requirements

### Requirement: Domain Lifecycle Events Dispatch
The system SHALL fire cancellable custom domain events for plant, harvest, break, and stage growth lifecycle actions.

#### Scenario: Cancelled plant event prevents instance creation
- **WHEN** a CropPlantEvent is cancelled by an external listener
- **THEN** the system SHALL not create a CropInstance and the seed placement SHALL be cancelled

#### Scenario: Cancelled growth event halts stage advancement
- **WHEN** a CropGrowEvent is cancelled by an external listener
- **THEN** the system SHALL not advance the crop instance stage and SHALL preserve the crop state safely
