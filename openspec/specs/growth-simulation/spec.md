# growth-simulation Specification

## Purpose
Simulates deterministic crop developmental progress based on environmental conditions, extensible modifier calculations, chunk availability, and visual block state synchronization.

## Requirements

### Requirement: Periodic Simulation Ticking and Chunk Safety
The system SHALL periodically evaluate registered crop instances at a configurable tick interval while skipping crops located in unloaded chunks.

#### Scenario: Configurable simulation interval execution
- **WHEN** the growth simulation ticker executes at the configured tick interval
- **THEN** the system SHALL process active crop instances

#### Scenario: Unloaded chunk evaluation skip
- **WHEN** a registered crop instance is located in an unloaded world chunk
- **THEN** the system SHALL skip environmental evaluation and growth processing for that crop without loading the chunk

### Requirement: Environmental Condition Verification
The system SHALL evaluate ambient conditions for a crop, verifying that block light meets species minimum light and that nearby water meets required fluid level within a 9x9 horizontal area.

#### Scenario: Requirements met allows growth processing
- **WHEN** the crop location light level >= species minLight and nearby water fluid level >= species waterFluidLevel
- **THEN** the environment SHALL be validated as satisfying growth conditions

#### Scenario: Deficient light halts growth
- **WHEN** the crop location light level is strictly less than species minLight
- **THEN** growth processing SHALL yield zero progress for the current cycle

#### Scenario: Deficient water halts growth
- **WHEN** the highest water fluid level in the 9x9 horizontal area is strictly less than species waterFluidLevel
- **THEN** growth processing SHALL yield zero progress for the current cycle

### Requirement: Deterministic Progress Accumulation and Modifiers
The system SHALL accumulate developmental progress deterministically based on base progress and an extensible chain of growth modifiers.

#### Scenario: Progress calculation with modifiers
- **WHEN** environmental conditions are satisfied
- **THEN** effective progress gained SHALL equal base progress multiplied by registered growth modifiers

#### Scenario: Stage transition upon progress completion
- **WHEN** accumulated progress equals or exceeds the current stage required progress and the crop is not mature
- **THEN** the system SHALL advance the crop developmental stage and reset stage progress

### Requirement: World Display Synchronization
The system SHALL synchronize the physical world block state to reflect the crop instance's developmental stage.

#### Scenario: Vanilla block age visual update
- **WHEN** a crop instance successfully advances to a new developmental stage
- **THEN** the system SHALL update the physical block data to match the new stage block age
