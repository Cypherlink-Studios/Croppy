# Spec Delta

## MODIFIED Requirements

### Requirement: Declarative Crop Species Definition
The system SHALL represent a crop species loaded from the `crops/` directory with a unique Key, display configuration (vanilla block identifier or 3D model configuration with rotation mode, and seed item Key referencing an item in the item registry), ordered growth stages, environmental requirements, and harvest drops.

#### Scenario: Complete species loading
- **WHEN** a valid crop YAML definition is loaded from the `crops/` configuration folder
- **THEN** the species SHALL be registered with its unique Key
- **THEN** its display configuration (block identifier or 3D model) and associated seed item Key referencing `ItemRegistry` SHALL be queryable

#### Scenario: 3D model display species loading
- **WHEN** a crop definition specifies display type as model_3d with rotation mode and seed item
- **THEN** the species SHALL register the 3D model visual parameters and rotation policy

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
