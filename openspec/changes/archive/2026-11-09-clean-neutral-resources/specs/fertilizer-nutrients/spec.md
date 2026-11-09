# Spec Delta

## MODIFIED Requirements

### Requirement: Declarative Fertilizer Definitions
The system SHALL load fertilizer definitions using neutral agronomical nomenclature (`growth_fertilizer`, `hydrating_fertilizer`, `quality_fertilizer`) from `items/fertilizers/*.yml` configuration files, registering their growth and water retention effects in `FertilizerRegistry` and their item metadata in `ItemRegistry` with standardized PDC tagging.

#### Scenario: Registering valid fertilizer definition
- **WHEN** a fertilizer YAML configuration is loaded with an identifier, effect parameters, and item definition
- **THEN** the system SHALL register the fertilizer effect into the `FertilizerRegistry` and register its item into `ItemRegistry` under category `fertilizers`

#### Scenario: Rejecting invalid fertilizer configuration
- **WHEN** a fertilizer configuration lacks a valid identifier or recognized effect type
- **THEN** the system SHALL log a descriptive error and refuse to register the invalid entry
