# Spec Delta

## ADDED Requirements

### Requirement: Dynamic Vertical Headroom Detection and Layout Adaptation
The system SHALL measure vertical clearance (`display.headroom`) above crafting stations using world collision bounding boxes, evaluate `DISPLAY_LAYOUT` rules, and adapt Text Display translation, scale, and formatting to fit within constrained spaces.

#### Scenario: Measure vertical headroom from world collision shapes
- **WHEN** a crafting station visual layout is evaluated
- **THEN** the system calculates vertical clearance between the top of the station model and the lowest collision boundary of blocks at `Y+1` and exposes `display.headroom` to the rule evaluation context

#### Scenario: Fit text display within available clearance
- **WHEN** an evaluated `DISPLAY_LAYOUT` rule specifies a `TEXT_DISPLAY_FIT` action
- **THEN** the Text Display vertical translation is dynamically adjusted to center the hologram within the available clearance gap without clipping into ceiling blocks

#### Scenario: Scale text display and apply compact formatting
- **WHEN** an evaluated `DISPLAY_LAYOUT` rule specifies a `TEXT_DISPLAY_SCALE` action
- **THEN** transformation metadata updates the Text Display scale vector and formats text using the rule's compact template or standard format

#### Scenario: Cache headroom and invalidate on block change
- **WHEN** the physical block above a station (`pos.y() + 1`) is placed, broken, moved by pistons, or altered
- **THEN** the cached headroom value is invalidated and recomputed on the next visual update cycle

### Requirement: Critical Clearance Action Bar Telemetry Fallback
The system SHALL suppress virtual Text Display entities when vertical headroom is critically constrained and redirect station telemetry to nearby player Action Bars.

#### Scenario: Suppress text display on critical clearance
- **WHEN** a station evaluates a `DISPLAY_LAYOUT` rule specifying `TEXT_DISPLAY_FALLBACK` with target `ACTION_BAR`
- **THEN** any existing virtual Text Display entity is destroyed for tracking players and spawn packets are withheld

#### Scenario: Dispatch Action Bar telemetry on proximity view cone look
- **WHEN** a player within configured maximum distance looks toward a station with an active Action Bar fallback within the configured view-cone angle
- **THEN** station telemetry is formatted onto the player's Action Bar, respecting a priority cooldown if recent messages were delivered

#### Scenario: Dispatch Action Bar telemetry on station interaction
- **WHEN** a player interacts with a station configured with an active Action Bar fallback
- **THEN** the station status, progress, and thermal telemetry are dispatched immediately to the player's Action Bar
