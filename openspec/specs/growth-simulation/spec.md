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
The system SHALL evaluate ambient conditions for a crop, verifying that block light meets species minimum light, nearby water meets required fluid level within a 9x9 horizontal area, and daily soil hydration is evaluated.

#### Scenario: Requirements met allows growth processing
- **WHEN** the crop location light level >= species minLight, nearby water fluid level >= species waterFluidLevel, and the crop is hydrated for the current day
- **THEN** the environment SHALL be validated as satisfying standard growth conditions

#### Scenario: Deficient light halts growth
- **WHEN** the crop location light level is strictly less than species minLight
- **THEN** growth processing SHALL yield zero progress for the current cycle

#### Scenario: Deficient water halts growth
- **WHEN** the highest water fluid level in the 9x9 horizontal area is strictly less than species waterFluidLevel
- **THEN** growth processing SHALL yield zero progress for the current cycle

#### Scenario: Unwatered crop applies unwatered policy
- **WHEN** the crop is not hydrated for the current day
- **THEN** growth processing SHALL execute the configured unwatered policy rather than standard progress accumulation

### Requirement: Unwatered Growth Policies
The system SHALL enforce configurable unwatered growth policies when crops lack daily hydration.

#### Scenario: Pause policy halts progress
- **WHEN** an unwatered crop is evaluated under the PAUSE policy
- **THEN** developmental progress gained SHALL be zero

#### Scenario: Slow policy reduces progress
- **WHEN** an unwatered crop is evaluated under the SLOW policy
- **THEN** developmental progress gained SHALL be calculated using the configured slow growth penalty rate

#### Scenario: Decay progress policy reduces accumulated progress
- **WHEN** an unwatered crop is evaluated under the DECAY_PROGRESS policy
- **THEN** accumulated progress SHALL be reduced by the configured decay amount without dropping below zero

#### Scenario: Wither policy destroys crop after exceeding maximum dry days
- **WHEN** an unwatered crop is evaluated under the WITHER policy and consecutive dry days exceed the configured maximum
- **THEN** the crop instance SHALL be destroyed and replaced with a withered block or visual state

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
