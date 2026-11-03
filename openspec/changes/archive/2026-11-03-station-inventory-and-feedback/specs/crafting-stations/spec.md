# Spec Delta: crafting-stations

## MODIFIED Requirements

### Requirement: Dual Interaction Modes
The system SHALL support both physical in-world item interactions (right-clicking the station with an ingredient or tool in hand) and virtual container GUI menus dynamically rendered from the station's bound `MenuDefinition`.

#### Scenario: Physical right-click interaction
- **WHEN** player right-clicks a physical-interaction station holding a valid ingredient
- **THEN** the item is transferred into the station's input storage, merging into existing identical item stacks when available, and visual feedback is triggered

#### Scenario: Empty physical station interaction feedback
- **WHEN** player right-clicks a physical-interaction station with an empty hand and no active processes
- **THEN** informative action bar feedback displays the summary of stored input ingredients when present, or indicates empty only when storage contains zero items

#### Scenario: Physical input extraction
- **WHEN** player triggers an input extraction interaction on a station containing stored inputs
- **THEN** stored input items are removed from input storage and transferred into the player's inventory

#### Scenario: Virtual inventory GUI interaction
- **WHEN** player right-clicks a GUI-configured station without sneaking
- **THEN** a custom inventory GUI opens rendered from the station's configured `MenuDefinition` accommodating all defined input and output capacities

#### Scenario: Sneak interaction override
- **WHEN** player sneak-right-clicks a GUI station
- **THEN** direct physical extraction or inspection executes without opening the inventory window
