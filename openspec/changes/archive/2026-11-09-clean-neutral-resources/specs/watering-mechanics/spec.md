# Spec Delta

## MODIFIED Requirements

### Requirement: Tiered Watering Cans
The system SHALL provide tiered watering cans aligned with native Minecraft material tiers (`wooden`, `copper`, `iron`, `golden`, `diamond`, `netherite`) configured in `items/watering_cans/*.yml` and registered in `ItemRegistry`, identified via PDC `croppy:item_id` with dynamic water use tracking in PDC, defined water capacity, area-of-effect hydration patterns, and refill capabilities.

#### Scenario: Watering crops within area of effect
- **WHEN** a player right-clicks with a filled watering can on a crop or farmland
- **THEN** the target block and surrounding blocks within the tool tier area of effect SHALL become hydrated for the current day
- **AND** the watering can remaining water uses in PDC SHALL decrement by one
- **AND** water splash particles and audio feedback SHALL play at the watered location

#### Scenario: Empty watering can prevents watering
- **WHEN** a player attempts to water with a watering can whose remaining water uses equal zero
- **THEN** no blocks SHALL be hydrated and an empty feedback sound or warning SHALL occur

#### Scenario: Refilling watering can
- **WHEN** a player right-clicks while targeting a water source block (still water), a waterlogged block, or a filled water cauldron with a non-full watering can
- **THEN** server-side fluid interaction detection SHALL recognize the water block across both right-click block and right-click air actions
- **AND** the watering can water uses in PDC SHALL be restored to maximum capacity
- **AND** refill audio feedback and splash particles SHALL play
