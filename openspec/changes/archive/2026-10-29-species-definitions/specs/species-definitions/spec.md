# Spec Delta: Species Definitions

## Purpose

Defines declarative schemas for agricultural crop species, growth stage progression, environmental requirements (including light and fluid level), and harvest yields, with validated YAML serialization.

## ADDED Requirements

### Requirement: Declarative Crop Species Definition
The system SHALL represent a crop species with a unique Key, display configuration (block identifier and seed item identifier), ordered growth stages, environmental requirements, and harvest drops.

#### Scenario: Complete species loading
- **WHEN** a valid species YAML definition is loaded
- **THEN** the species SHALL be registered with its unique Key
- **THEN** its display block and seed item SHALL be queryable

#### Scenario: Malformed species rejection
- **WHEN** a species definition is missing required fields such as id, display block, or stages
- **THEN** the system SHALL reject the definition with an informative validation exception

### Requirement: Growth Stage Progression Modeling
The system SHALL model crop development through an ordered sequence of stages, each defining required progress points and a display mapping.

#### Scenario: Ordered stage transitions
- **WHEN** a species defines sequential stages with progress requirements
- **THEN** the final stage SHALL be designated as mature with zero remaining progress required
- **THEN** each stage SHALL provide its display block age

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
The system SHALL define harvest drops containing item identifiers with minimum and maximum drop quantities.

#### Scenario: Harvest drop range validation
- **WHEN** a species specifies harvest drops with min and max bounds
- **THEN** min quantity SHALL be greater than 0 and max quantity SHALL be greater than or equal to min quantity
