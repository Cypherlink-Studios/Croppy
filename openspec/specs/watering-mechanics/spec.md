# watering-mechanics Specification

## Purpose

Enables active soil hydration through tiered watering cans, natural rain exposure, and daily moisture tracking for crops.

## Requirements

### Requirement: Tiered Watering Cans
The system SHALL provide tiered watering cans with defined water capacity, area-of-effect hydration patterns, and refill capabilities.

#### Scenario: Watering crops within area of effect
- **WHEN** a player right-clicks with a filled watering can on a crop or farmland
- **THEN** the target block and surrounding blocks within the tool tier area of effect SHALL become hydrated for the current day
- **AND** the watering can remaining water uses SHALL decrement by one
- **AND** water splash particles and audio feedback SHALL play at the watered location

#### Scenario: Empty watering can prevents watering
- **WHEN** a player attempts to water with a watering can whose remaining water uses equal zero
- **THEN** no blocks SHALL be hydrated and an empty feedback sound or warning SHALL occur

#### Scenario: Refilling watering can
- **WHEN** a player right-clicks on a water source block or filled water cauldron with a non-full watering can
- **THEN** the watering can water uses SHALL be restored to maximum capacity
- **AND** refill audio feedback SHALL play

### Requirement: Discrete Daily Soil Hydration
The system SHALL track soil hydration discrete to the world day, marking crops as watered for the current day and displaying moist farmland visuals.

#### Scenario: Manual hydration sets current day
- **WHEN** a crop is watered manually by a player
- **THEN** the crop lastWateredDay SHALL be updated to the current world day index
- **AND** the supporting farmland block data SHALL be updated to maximum moisture level

#### Scenario: Day turnover dries unwatered crops
- **WHEN** the world day increments
- **THEN** crops whose lastWateredDay is strictly less than the current day SHALL be evaluated as unwatered for the new day

### Requirement: Natural Rain Exposure Hydration
The system SHALL automatically hydrate crops exposed to rainfall in biomes supporting precipitation.

#### Scenario: Exposed crop hydrated by rain
- **WHEN** the world is storming, the crop biome supports precipitation, and the crop has an unobstructed line of sight to the sky
- **THEN** the crop lastWateredDay SHALL update to the current world day index

#### Scenario: Sheltered crop not hydrated by rain
- **WHEN** the world is storming but the crop is covered by overhead blocks or greenhouse structures
- **THEN** the crop SHALL not be automatically hydrated by rainfall
