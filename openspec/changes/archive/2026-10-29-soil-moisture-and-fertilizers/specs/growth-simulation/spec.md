# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
