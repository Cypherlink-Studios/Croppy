# Spec Delta: crafting-stations

## ADDED Requirements

### Requirement: Dynamic Station Placement and Facing
The system SHALL capture player yaw upon station block placement and orient the station towards the player according to the station definition's configured rotation mode (`CARDINAL_4`, `CARDINAL_8`, or `FREE_YAW`).

#### Scenario: Place station with 4-way cardinal snapping
- **WHEN** a player places a station configured with `CARDINAL_4` rotation mode
- **THEN** the station instance yaw is snapped to the nearest 90-degree angle facing towards the player

#### Scenario: Place station with 8-way compass snapping
- **WHEN** a player places a station configured with `CARDINAL_8` rotation mode
- **THEN** the station instance yaw is snapped to the nearest 45-degree angle facing towards the player

#### Scenario: Place station with free continuous yaw
- **WHEN** a player places a station configured with `FREE_YAW` rotation mode
- **THEN** the station instance preserves the exact continuous horizontal yaw angle opposite the player facing

## MODIFIED Requirements

### Requirement: Station Registration and Identification
The system SHALL register immutable `StationDefinition` instances identified by unique `Key` identifiers, including an optional `menuKey` binding to a configured `MenuDefinition` and an optional `rotationMode`, and manage runtime `StationInstance` models bound to discrete `BlockPosition` spatial coordinates and horizontal yaw orientation.

#### Scenario: Register station definition
- **WHEN** a station definition for `croppy:cooking_pot` is registered
- **THEN** it is retrievable by its key from the central station registry

#### Scenario: Register station definition with explicit menu key
- **WHEN** a station definition specifies `menu: "croppy:cooking_pot"`
- **THEN** its `menuKey()` resolves to `croppy:cooking_pot`, falling back to `croppy:default_station` if omitted

#### Scenario: Instantiate station in world
- **WHEN** a valid station item is placed at a block coordinate
- **THEN** a `StationInstance` is spawned at that `BlockPosition` with default component states and calculated placement yaw

### Requirement: Dual Interaction Modes
The system SHALL support both physical in-world item interactions (right-clicking the station with an ingredient or tool in hand) and virtual container GUI menus dynamically rendered from the station's bound `MenuDefinition`, ensuring extracted items retain their original metadata and stackability.

#### Scenario: Physical right-click interaction
- **WHEN** player right-clicks a physical-interaction station holding a valid ingredient
- **THEN** the item is transferred into the station's input storage, merging into existing identical item stacks when available, and visual feedback is triggered

#### Scenario: Empty physical station interaction feedback
- **WHEN** player right-clicks a physical-interaction station with an empty hand and no active processes
- **THEN** informative action bar feedback displays the summary of stored input ingredients when present, or indicates empty only when storage contains zero items

#### Scenario: Physical input extraction
- **WHEN** player triggers an input extraction interaction on a station containing stored inputs
- **THEN** stored input items are removed from input storage and returned to the player's inventory as clean items without synthetic display names or PDC tags, stacking with identical vanilla items

#### Scenario: Virtual inventory GUI interaction
- **WHEN** player right-clicks a GUI-configured station without sneaking
- **THEN** a custom inventory GUI opens rendered from the station's configured `MenuDefinition` accommodating all defined input and output capacities

#### Scenario: Sneak interaction override
- **WHEN** player sneak-right-clicks a GUI station
- **THEN** direct physical extraction or inspection executes without opening the inventory window

### Requirement: Spatial Lifecycle and Persistence
The system SHALL persist station state, horizontal yaw orientation, and attached component data across chunk lifecycles and server restarts, restoring active station data on chunk load.

#### Scenario: Chunk load restore
- **WHEN** a chunk containing registered crafting stations is loaded into memory
- **THEN** all station instances within that chunk are loaded into memory and registered with the station manager, restoring their placement yaw

#### Scenario: Chunk unload persistence
- **WHEN** a chunk containing registered crafting stations unloads from memory
- **THEN** all active station instances within that chunk are saved to persistent storage and evicted from active station memory

#### Scenario: Block destruction cleanup
- **WHEN** a player breaks a crafting station block
- **THEN** the station custom item and all stored items drop to the ground, runtime state is cleared, and persistence records are deleted

#### Scenario: Heat source station persistence
- **WHEN** a heat source station with active fuel and temperature settings unloads and reloads
- **THEN** its active status, output temperature, target temperature, and remaining burn ticks are fully restored from binary storage
