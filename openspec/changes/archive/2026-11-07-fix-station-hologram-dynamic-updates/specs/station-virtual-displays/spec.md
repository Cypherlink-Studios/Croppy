# Spec Delta

## MODIFIED Requirements

### Requirement: Multi-Tier Distance Culling and Level of Detail
The system SHALL organize station visual layers into discrete distance thresholds to provide Level-of-Detail (LoD) culling and prevent visual clutter, rendering process item displays when items are stored in inputs or when a recipe is progressing, and culling text displays when idle if configured.

#### Scenario: Long-range view renders base model only
- **WHEN** a player is within the base model threshold (up to 48 blocks) but outside the process item threshold
- **THEN** only the base station model virtual entity is rendered for that player

#### Scenario: Mid-range view activates process item display
- **WHEN** a player enters the process item threshold (up to 24 blocks) and the station has stored inputs or an active process
- **THEN** the item virtual display is spawned and synchronized for that player displaying the appropriate item model

#### Scenario: Short-range view activates holographic text display
- **WHEN** a player enters the interaction threshold (up to 8 blocks) and the station is active, heated, holding items, or configured with `hide_when_idle: false`
- **THEN** the floating status text display is spawned and synchronized for that player

#### Scenario: Idle cold station dynamically culls text display
- **WHEN** a station is configured with `hide_when_idle: true` and is cold (ambient temperature), contains no input/output items, and has no active process
- **THEN** the virtual Text Display entity is destroyed for tracking players within the interaction threshold until heat or items are introduced

### Requirement: Templated MiniMessage Text Holograms
The system SHALL render dynamic status holograms formatted with configurable MiniMessage templates and contextual placeholders, adapting content across idle, active, paused, and completed states.

#### Scenario: Format dynamic status text with placeholders
- **WHEN** a station with an active process is evaluated for text rendering
- **THEN** template tokens including `{recipe_name}`, `{recipe_display}`, `{progress_percent}`, `{progress_bar}`, `{temperature_c}`, `{thermal_zone}`, and `{status_message}` are resolved and rendered via Text Display metadata

#### Scenario: Update text display on process tick
- **WHEN** station progress, thermal zone, or status text changes
- **THEN** an updated entity metadata packet with the newly formatted MiniMessage text is dispatched to tracking players within the text threshold

#### Scenario: Update text display on thermal convergence
- **WHEN** station temperature changes by 0.5°C or more or transitions into a new thermal zone while idle or cooking
- **THEN** the text display is marked dirty and updated metadata is dispatched to tracking players

#### Scenario: Hologram billboard orientation invariant to station placement yaw
- **WHEN** a floating text display hologram with camera tracking (e.g., CENTER or VERTICAL billboard) is rendered for a station with non-zero placement yaw
- **THEN** the text display transformation does not inherit the station instance's horizontal placement yaw in its left-rotation quaternion or translation offsets, maintaining a level camera-facing orientation

#### Scenario: Packet caching prevents redundant metadata dispatches
- **WHEN** a station display is evaluated for synchronization but the serialized rendered text component matches the previous cycle's text
- **THEN** redundant entity metadata packets are suppressed to conserve client and network bandwidth
