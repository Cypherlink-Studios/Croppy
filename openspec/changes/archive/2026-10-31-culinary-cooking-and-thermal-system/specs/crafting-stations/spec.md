# Spec Delta

## MODIFIED Requirements

### Requirement: Display and Visual Synchronization
The system SHALL synchronize visual representations including block display entities, item display entities with custom spatial transforms, floating status indicators, and ambient culinary particle and audio loops on the server main thread.

#### Scenario: Update item display on state change
- **WHEN** a station enters a new processing phase
- **THEN** the floating item display updates its rendered model and particles are emitted

#### Scenario: Synchronize cookware item display orientation
- **WHEN** items are placed inside a cooking pot or onto a skillet
- **THEN** an `ItemDisplay` entity renders with height offsets and planar rotation matching the cookware geometry

## ADDED Requirements

### Requirement: Cookware Station Thermal and Multi-Sensory Ticking
The system SHALL execute recurring thermal checks and multi-sensory culinary audio and particle feedback loops for active cooking cookware during station ticking.

#### Scenario: Cookware boiling loop feedback
- **WHEN** a cooking pot is ticking with an active recipe above simmer temperature
- **THEN** bubble and steam particles emit from the top of the pot and water boiling sounds play periodically

#### Scenario: Cookware sizzling loop feedback
- **WHEN** a skillet is ticking with active ingredients above sear temperature
- **THEN** grease spark and smoke particles emit from the plate surface and sizzling sounds play periodically
