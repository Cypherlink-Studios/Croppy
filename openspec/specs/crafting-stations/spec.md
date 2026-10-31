# crafting-stations Specification

## Purpose
Provides registration, spatial tracking, physical in-world and GUI interactions, and lifecycle persistence for interactive crafting stations and artisan machines.

## Requirements

### Requirement: Station Registration and Identification
The system SHALL register immutable `StationDefinition` instances identified by unique `Key` identifiers and manage runtime `StationInstance` models bound to discrete `BlockPosition` spatial coordinates.

#### Scenario: Register station definition
- **WHEN** a station definition for `croppy:cooking_pot` is registered
- **THEN** it is retrievable by its key from the central station registry

#### Scenario: Instantiate station in world
- **WHEN** a valid station item is placed at a block coordinate
- **THEN** a `StationInstance` is spawned at that `BlockPosition` with default component states

### Requirement: Dual Interaction Modes
The system SHALL support both physical in-world item interactions (right-clicking the station with an ingredient or tool in hand) and virtual container GUI menus depending on station configuration.

#### Scenario: Physical right-click interaction
- **WHEN** player right-clicks a physical-interaction station holding a valid ingredient
- **THEN** the item is transferred into the station's input storage and visual feedback is triggered

#### Scenario: Virtual inventory GUI interaction
- **WHEN** player right-clicks a GUI-configured station without sneaking
- **THEN** a custom inventory GUI opens displaying input, output, and progress slots

#### Scenario: Sneak interaction override
- **WHEN** player sneak-right-clicks a GUI station
- **THEN** direct physical extraction or inspection executes without opening the inventory window

### Requirement: Spatial Lifecycle and Persistence
The system SHALL persist station state across chunk lifecycles and server restarts, restoring active station data on chunk load.

#### Scenario: Chunk load restore
- **WHEN** a chunk containing registered crafting stations is loaded into memory
- **THEN** all station instances within that chunk are loaded into memory and registered with the station manager

#### Scenario: Block destruction cleanup
- **WHEN** a player breaks a crafting station block
- **THEN** stored items and components drop to the ground, runtime state is cleared, and persistence records are deleted

### Requirement: Display and Visual Synchronization
The system SHALL synchronize visual representations including block display entities, item display entities, and floating status indicators on the server main thread.

#### Scenario: Update item display on state change
- **WHEN** a station enters a new processing phase
- **THEN** the floating item display updates its rendered model and particles are emitted
