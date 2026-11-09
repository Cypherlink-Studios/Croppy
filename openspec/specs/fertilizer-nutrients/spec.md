# fertilizer-nutrients Specification

## Purpose
Enables declarative soil nutrient fertilizers that enhance crop growth rates, water retention, and harvest yield quality.

## Requirements

### Requirement: Declarative Fertilizer Definitions
The system SHALL load fertilizer definitions using neutral agronomical nomenclature (`growth_fertilizer`, `hydrating_fertilizer`, `quality_fertilizer`) from `items/fertilizers/*.yml` configuration files, registering their growth and water retention effects in `FertilizerRegistry` and their item metadata in `ItemRegistry` with standardized PDC tagging.

#### Scenario: Registering valid fertilizer definition
- **WHEN** a fertilizer YAML configuration is loaded with an identifier, effect parameters, and item definition
- **THEN** the system SHALL register the fertilizer effect into the `FertilizerRegistry` and register its item into `ItemRegistry` under category `fertilizers`

#### Scenario: Rejecting invalid fertilizer configuration
- **WHEN** a fertilizer configuration lacks a valid identifier or recognized effect type
- **THEN** the system SHALL log a descriptive error and refuse to register the invalid entry

### Requirement: Single Fertilizer Application Constraint
The system SHALL permit at most one active fertilizer per crop instance, persisting the fertilizer across multi-harvest regrowth cycles.

#### Scenario: Applying fertilizer to unfertilized crop
- **WHEN** a player applies a registered fertilizer to an active crop that has no existing fertilizer
- **THEN** the fertilizer identifier SHALL be associated with the crop instance
- **AND** the fertilizer item SHALL be decremented from the player inventory

#### Scenario: Applying fertilizer to empty farmland buffer
- **WHEN** a player applies fertilizer to tilled farmland before planting a seed
- **THEN** the system SHALL record the fertilizer in a transient location cache for up to 30 seconds
- **AND** when a crop is planted at that location within 30 seconds, the crop SHALL adopt the fertilizer

#### Scenario: Rejecting duplicate fertilizer application
- **WHEN** a player attempts to apply fertilizer to a crop that already has an active fertilizer
- **THEN** the application SHALL be rejected and the player inventory SHALL remain unchanged

#### Scenario: Fertilizer retention across harvest
- **WHEN** a multi-harvest crop is harvested and reverts to an earlier developmental stage
- **THEN** the active fertilizer on the crop instance SHALL be preserved

### Requirement: Fertilizer Effects Execution
The system SHALL execute fertilizer effects according to their registered effect types during growth simulation and day change.

#### Scenario: Speed fertilizer enhances growth progress
- **WHEN** a crop with a speed fertilizer is evaluated during growth ticking
- **THEN** the effective growth progress gained SHALL be multiplied by the fertilizer speed modifier

#### Scenario: Retaining soil preserves daily hydration
- **WHEN** a world day turnover occurs for a crop with a water-retaining fertilizer
- **THEN** a probability roll based on the retention chance SHALL determine if the crop remains watered for the next day without manual watering
