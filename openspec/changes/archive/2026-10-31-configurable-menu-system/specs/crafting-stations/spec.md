# Spec Delta

## MODIFIED Requirements

### Requirement: Station Registration and Identification
The system SHALL register immutable `StationDefinition` instances identified by unique `Key` identifiers, including an optional `menuKey` binding to a configured `MenuDefinition`, and manage runtime `StationInstance` models bound to discrete `BlockPosition` spatial coordinates.

#### Scenario: Register station definition
- **WHEN** a station definition for `croppy:cooking_pot` is registered
- **THEN** it is retrievable by its key from the central station registry

#### Scenario: Register station definition with explicit menu key
- **WHEN** a station definition specifies `menu: "croppy:cooking_pot"`
- **THEN** its `menuKey()` resolves to `croppy:cooking_pot`, falling back to `croppy:default_station` if omitted

#### Scenario: Instantiate station in world
- **WHEN** a valid station item is placed at a block coordinate
- **THEN** a `StationInstance` is spawned at that `BlockPosition` with default component states

### Requirement: Dual Interaction Modes
The system SHALL support both physical in-world item interactions (right-clicking the station with an ingredient or tool in hand) and virtual container GUI menus dynamically rendered from the station's bound `MenuDefinition`.

#### Scenario: Physical right-click interaction
- **WHEN** player right-clicks a physical-interaction station holding a valid ingredient
- **THEN** the item is transferred into the station's input storage and visual feedback is triggered

#### Scenario: Empty physical station interaction feedback
- **WHEN** player right-clicks a physical-interaction station with an empty hand and no completed outputs or active processes
- **THEN** informative action bar feedback is displayed indicating station status and temperature

#### Scenario: Virtual inventory GUI interaction
- **WHEN** player right-clicks a GUI-configured station without sneaking
- **THEN** a custom inventory GUI opens rendered from the station's configured `MenuDefinition` accommodating all defined input and output capacities

#### Scenario: Sneak interaction override
- **WHEN** player sneak-right-clicks a GUI station
- **THEN** direct physical extraction or inspection executes without opening the inventory window
