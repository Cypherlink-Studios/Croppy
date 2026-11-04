# Spec Delta: station-virtual-displays

## MODIFIED Requirements

### Requirement: Templated MiniMessage Text Holograms
The system SHALL render dynamic status holograms formatted with configurable MiniMessage templates and contextual placeholders.

#### Scenario: Format dynamic status text with placeholders
- **WHEN** a station with an active process is evaluated for text rendering
- **THEN** template tokens including `{recipe_name}`, `{recipe_display}`, `{progress_percent}`, `{progress_bar}`, `{temperature_c}`, `{thermal_zone}`, and `{status_message}` are resolved and rendered via Text Display metadata

#### Scenario: Update text display on process tick
- **WHEN** station progress, thermal zone, or status text changes
- **THEN** an updated entity metadata packet with the newly formatted MiniMessage text is dispatched to tracking players within the text threshold

#### Scenario: Hologram billboard orientation invariant to station placement yaw
- **WHEN** a floating text display hologram with camera tracking (e.g., CENTER or VERTICAL billboard) is rendered for a station with non-zero placement yaw
- **THEN** the text display transformation does not inherit the station instance's horizontal placement yaw in its left-rotation quaternion or translation offsets, maintaining a level camera-facing orientation
