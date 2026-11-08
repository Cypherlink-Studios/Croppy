# Spec Delta

## MODIFIED Requirements

### Requirement: Configurable Phase Display and Placeholder Resolution
The system SHALL render customizable phase display status holograms using MiniMessage templates enriched with direct and inverted (`_left`) progress, time, and progress bar placeholders, delegating active rendering to the active phase template.

#### Scenario: Render formatted phase status with progress bar
- **WHEN** an active process is ticked during a `TIMED` phase
- **THEN** placeholders including `{progress_percent}`, `{progress_percent_left}`, `{progress_seconds_left}`, and `{progress_bar}` are resolved and displayed on the station hologram

#### Scenario: Render interaction prompt with remaining timeout
- **WHEN** an active process is in `AWAITING_INPUT` state
- **THEN** configured prompt text and remaining timeout placeholders are resolved and displayed on the station hologram

#### Scenario: Real-time progress and remaining time countdown
- **WHEN** an active process advances through game ticks
- **THEN** the station display component progress fraction is synchronized from elapsed duration divided by target duration, and remaining time countdown reflects remaining ticks rather than elapsed ticks

#### Scenario: Active phase template precedence over station default
- **WHEN** a cooking process with phase-defined display templates is actively running or awaiting input on a workstation
- **THEN** the active phase's MiniMessage template and bar styling take precedence over the station's default idle format
