# Spec Delta

## MODIFIED Requirements

### Requirement: Templated MiniMessage Text Holograms
The system SHALL render dynamic status holograms formatted with configurable MiniMessage templates and contextual placeholders resolved dynamically from the unified evaluation context, adapting content across idle, active, paused, and completed states without requiring dedicated Java parameter bindings.

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
- **THEN** redundant packet dispatches are skipped

#### Scenario: Format text display with namespaced component tokens
- **WHEN** a non-cooking station (such as a stove or burner) configures a text display format using `{heat.fuel_ticks}` or `{thermal.temp_c}`
- **THEN** the tokens are dynamically resolved from the evaluation context and rendered without process placeholder dependencies

#### Scenario: Format text display with typed custom variable tokens
- **WHEN** a station configures a text display format referencing typed variables like `{var.cleanliness}` or `{var.tank.fluid}`
- **THEN** the values are extracted from the station's typed variable store and rendered into the MiniMessage text
