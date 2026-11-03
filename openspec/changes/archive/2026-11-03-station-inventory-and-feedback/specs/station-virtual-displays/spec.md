# Spec Delta: station-virtual-displays

## MODIFIED Requirements

### Requirement: Multi-Tier Distance Culling and Level of Detail
The system SHALL organize station visual layers into discrete distance thresholds to provide Level-of-Detail (LoD) culling and prevent visual clutter, rendering process item displays when items are stored in inputs or when a recipe is progressing.

#### Scenario: Long-range view renders base model only
- **WHEN** a player is within the base model threshold (up to 48 blocks) but outside the process item threshold
- **THEN** only the base station model virtual entity is rendered for that player

#### Scenario: Mid-range view activates process item display
- **WHEN** a player enters the process item threshold (up to 24 blocks) and the station has stored inputs or an active process
- **THEN** the item virtual display is spawned and synchronized for that player displaying the appropriate item model

#### Scenario: Short-range view activates holographic text display
- **WHEN** a player enters the interaction threshold (up to 8 blocks)
- **THEN** the floating status text display is spawned and synchronized for that player
