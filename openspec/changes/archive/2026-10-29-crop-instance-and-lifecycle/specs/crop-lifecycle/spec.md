# Spec Delta: Crop Lifecycle

## Purpose

Manages spatial tracking and state transitions of planted crop instances in the world, overriding vanilla growth and coordinating plant, break, and harvest events.

## ADDED Requirements

### Requirement: Spatial Crop Instance Binding
The system SHALL track planted crops at unique spatial block positions, associating each instance with a species key, developmental stage index, accumulated progress, and timestamps.

#### Scenario: Crop instance creation upon planting
- **WHEN** a player plants a registered crop seed on valid farmland
- **THEN** a new CropInstance SHALL be bound to that block position with initial stage 0 and 0.0 progress
- **THEN** the crop SHALL be queryable by its block position

#### Scenario: Duplicate planting at same position prevention
- **WHEN** an attempt is made to bind a crop instance to an already occupied position
- **THEN** the system SHALL reject registration with an IllegalStateException

### Requirement: Vanilla Growth Suppression
The system SHALL intercept and cancel vanilla Minecraft random growth ticks for all active crop instances to preserve engine authority over growth.

#### Scenario: Vanilla growth cancellation
- **WHEN** the server triggers a BlockGrowEvent on a block tracked by Croppy
- **THEN** the event SHALL be cancelled and the block data SHALL remain unmodified

### Requirement: Crop Harvest and Destruction Lifecycle
The system SHALL handle crop destruction and harvesting, dropping configured yields for mature crops or seeds for immature crops, and unregistering the instance.

#### Scenario: Mature crop harvest
- **WHEN** a player breaks a tracked crop that is at its mature stage
- **THEN** the system SHALL drop items defined in the species harvest yield
- **THEN** the crop instance SHALL be removed from the active tracker

#### Scenario: Immature crop breakage
- **WHEN** a player or environmental physics breaks an immature crop
- **THEN** the system SHALL drop the species seed item
- **THEN** the crop instance SHALL be removed from the active tracker

### Requirement: Domain Lifecycle Events Dispatch
The system SHALL fire cancellable custom domain events for plant, harvest, and break lifecycle actions.

#### Scenario: Cancelled plant event prevents instance creation
- **WHEN** a CropPlantEvent is cancelled by an external listener
- **THEN** the system SHALL not create a CropInstance and the seed placement SHALL be cancelled
