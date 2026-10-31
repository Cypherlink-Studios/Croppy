# crafting-stations Specification

## Purpose
Provides registration, spatial tracking, physical in-world and GUI interactions, and lifecycle persistence for interactive crafting stations and artisan machines.

## Requirements

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

### Requirement: Spatial Lifecycle and Persistence
The system SHALL persist station state across chunk lifecycles and server restarts, restoring active station data on chunk load.

#### Scenario: Chunk load restore
- **WHEN** a chunk containing registered crafting stations is loaded into memory
- **THEN** all station instances within that chunk are loaded into memory and registered with the station manager

#### Scenario: Chunk unload persistence
- **WHEN** a chunk containing registered crafting stations unloads from memory
- **THEN** all active station instances within that chunk are saved to persistent storage and evicted from active station memory

#### Scenario: Block destruction cleanup
- **WHEN** a player breaks a crafting station block
- **THEN** the station custom item and all stored items drop to the ground, runtime state is cleared, and persistence records are deleted

### Requirement: Display and Visual Synchronization
The system SHALL synchronize visual representations including block display entities, item display entities with custom spatial transforms, floating status indicators, and ambient culinary particle and audio loops on the server main thread.

#### Scenario: Update item display on state change
- **WHEN** a station enters a new processing phase
- **THEN** the floating item display updates its rendered model and particles are emitted

#### Scenario: Synchronize cookware item display orientation
- **WHEN** items are placed inside a cooking pot or onto a skillet
- **THEN** an `ItemDisplay` entity renders with height offsets and planar rotation matching the cookware geometry

### Requirement: Cookware Station Thermal and Multi-Sensory Ticking
The system SHALL execute recurring thermal checks and multi-sensory culinary audio and particle feedback loops for active cooking cookware during station ticking.

#### Scenario: Cookware boiling loop feedback
- **WHEN** a cooking pot is ticking with an active recipe above simmer temperature
- **THEN** bubble and steam particles emit from the top of the pot and water boiling sounds play periodically

#### Scenario: Cookware sizzling loop feedback
- **WHEN** a skillet is ticking with active ingredients above sear temperature
- **THEN** grease spark and smoke particles emit from the plate surface and sizzling sounds play periodically
