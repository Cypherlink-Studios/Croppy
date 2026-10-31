# Spec Delta

## MODIFIED Requirements

### Requirement: Dual Interaction Modes
The system SHALL support both physical in-world item interactions (right-clicking the station with an ingredient or tool in hand) and virtual container GUI menus depending on station configuration.

#### Scenario: Physical right-click interaction
- **WHEN** player right-clicks a physical-interaction station holding a valid ingredient
- **THEN** the item is transferred into the station's input storage and visual feedback is triggered

#### Scenario: Empty physical station interaction feedback
- **WHEN** player right-clicks a physical-interaction station with an empty hand and no completed outputs or active processes
- **THEN** informative action bar feedback is displayed indicating station status and temperature

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

#### Scenario: Chunk unload persistence
- **WHEN** a chunk containing registered crafting stations unloads from memory
- **THEN** all active station instances within that chunk are saved to persistent storage and evicted from active station memory

#### Scenario: Block destruction cleanup
- **WHEN** a player breaks a crafting station block
- **THEN** the station custom item and all stored items drop to the ground, runtime state is cleared, and persistence records are deleted
