# Spec Delta

## MODIFIED Requirements

### Requirement: Declarative Crop Species Definition
The system SHALL represent a crop species loaded from the `crops/` directory with a unique Key, display configuration (block identifier and seed item Key referencing an item in the item registry), ordered growth stages, environmental requirements, and harvest drops.

#### Scenario: Complete species loading
- **WHEN** a valid crop YAML definition is loaded from the `crops/` configuration folder
- **THEN** the species SHALL be registered with its unique Key
- **THEN** its display block and associated seed item Key referencing `ItemRegistry` SHALL be queryable

#### Scenario: Malformed species rejection
- **WHEN** a crop definition is missing required fields such as id, display block, or stages
- **THEN** the system SHALL reject the definition with an informative validation exception
