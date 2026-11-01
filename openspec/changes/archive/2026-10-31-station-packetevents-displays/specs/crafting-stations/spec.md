# Spec Delta: crafting-stations

## MODIFIED Requirements

### Requirement: Display and Visual Synchronization
The system SHALL synchronize visual representations including client-side virtual display entities (item displays with custom spatial transforms, base models, and floating status holograms) via packet protocols, combined with ambient culinary particle and audio loops on the server main thread.

#### Scenario: Update item display on state change
- **WHEN** a station enters a new processing phase
- **THEN** the virtual item display updates its rendered model via packet metadata and ambient particles are emitted

#### Scenario: Synchronize cookware item display orientation
- **WHEN** items are placed inside a cooking pot or onto a skillet
- **THEN** a virtual `ItemDisplay` entity renders with height offsets and planar rotation matching the cookware geometry without persisting in the world

#### Scenario: Virtual cleanup on block destruction
- **WHEN** a crafting station block is broken
- **THEN** virtual display entities are destroyed for all viewers via packet signals and no orphaned entity remains in the world
