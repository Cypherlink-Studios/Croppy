# species-definitions Specification

## Purpose
Defines declarative schemas for agricultural crop species, growth stage progression, environmental requirements (including light and fluid level), and harvest yields, with validated YAML serialization.

## Requirements

### Requirement: Declarative Crop Species Definition
The system SHALL represent a crop species loaded from the `crops/` directory with a unique Key, display configuration (vanilla block identifier or 3D model configuration with rotation mode, and seed item Key referencing an item in the item registry), optional spatial footprint configuration specifying vertical height, ordered growth stages, environmental requirements, and harvest drops.

#### Scenario: Complete species loading
- **WHEN** a valid crop YAML definition is loaded from the `crops/` configuration folder
- **THEN** the species SHALL be registered with its unique Key
- **THEN** its display configuration (block identifier or 3D model) and associated seed item Key referencing `ItemRegistry` SHALL be queryable

#### Scenario: 3D model display species loading
- **WHEN** a crop definition specifies display type as model_3d with rotation mode and seed item
- **THEN** the species SHALL register the 3D model visual parameters and rotation policy

#### Scenario: Multi-block vertical height loading
- **WHEN** a crop definition specifies a spatial height greater than 1
- **THEN** the species SHALL register the vertical spatial footprint for multi-block occupancy tracking

#### Scenario: Malformed species rejection
- **WHEN** a crop definition is missing required fields such as id, display block or model, or stages
- **THEN** the system SHALL reject the definition with an informative validation exception

### Requirement: Growth Stage Progression Modeling
The system SHALL model crop development through an ordered sequence of stages, each defining required progress points and a display mapping supporting vanilla block age or stage-specific 3D models with hitbox dimensions.

#### Scenario: Ordered stage transitions
- **WHEN** a species defines sequential stages with progress requirements
- **THEN** the final stage SHALL be designated as mature with zero remaining progress required
- **THEN** each stage SHALL provide its display block age or 3D model definition with hitbox dimensions

#### Scenario: Stage with 3D model and hitbox
- **WHEN** a stage specifies a 3D item model, scale, and hitbox dimensions
- **THEN** the stage definition SHALL expose these parameters for visual synchronization

#### Scenario: Invalid stage configuration rejection
- **WHEN** a species definition contains zero stages or negative progress requirements
- **THEN** the system SHALL reject the species definition during validation

### Requirement: Environmental Growth Requirements
The system SHALL model environmental constraints for each species, including minimum light level and water fluid level thresholds.

#### Scenario: Water fluid level threshold configuration
- **WHEN** a species defines a water fluid level between 1 and 8
- **THEN** the requirement SHALL specify that value as the minimum fluid level required for growth

#### Scenario: Dry-farming crop without water requirement
- **WHEN** a species defines water fluid level as 0 or omits the water requirement
- **THEN** the species SHALL be designated as requiring no water for growth

### Requirement: Harvest Yield Specification
The system SHALL define harvest drops containing item identifiers with minimum and maximum drop quantities, and optional cyclic regrowth policy indicating whether the plant persists upon harvest.

#### Scenario: Harvest drop range validation
- **WHEN** a species specifies harvest drops with min and max bounds
- **THEN** min quantity SHALL be greater than 0 and max quantity SHALL be greater than or equal to min quantity

#### Scenario: Regrowth policy configuration
- **WHEN** a species specifies harvest regrowth with a target stage identifier
- **THEN** the harvest definition SHALL record the regrowth policy targeting the specified stage

### Requirement: Crop Regrowth Semantic Validation
The system SHALL validate that any species specifying a regrowth target stage references a defined stage identifier within that species, and SHALL reject definitions where the target stage is the terminal mature stage.

#### Scenario: Valid regrowth target stage
- **WHEN** a species specifies a regrowth target stage matching an intermediate stage ID in its stages list
- **THEN** the system SHALL successfully validate and register the species

#### Scenario: Non-existent regrowth stage rejection
- **WHEN** a species specifies a regrowth target stage ID that is not present in its stages list
- **THEN** the system SHALL reject the species definition during startup with an informative validation exception

#### Scenario: Terminal stage regrowth rejection
- **WHEN** a species specifies a regrowth target stage ID pointing to the final mature stage
- **THEN** the system SHALL reject the species definition to prevent instant infinite harvesting loops
