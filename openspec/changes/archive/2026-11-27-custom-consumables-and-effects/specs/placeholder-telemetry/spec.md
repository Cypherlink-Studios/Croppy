# placeholder-telemetry Specification

## ADDED Requirements

### Requirement: Player Status Effect Telemetry
The system SHALL parse player-scoped PlaceholderAPI requests querying active custom status effects by namespaced key (`%croppy_effect_active_<key>%`, `%croppy_effect_remaining_<key>%`, `%croppy_effect_amplifier_<key>%`, `%croppy_effect_intensity_<key>%`, and `%croppy_effect_stage_<key>%`).

#### Scenario: Query active status effect flags and metrics
- **WHEN** a player has `croppy:intoxication` active with intensity `52.0`, remaining seconds `120`, and stage `"Drunk"`
- **THEN** `%croppy_effect_active_croppy:intoxication%` resolves to `"true"`, `%croppy_effect_intensity_croppy:intoxication%` resolves to `"52.0"`, `%croppy_effect_remaining_croppy:intoxication%` resolves to `"120s"` (or numeric seconds), and `%croppy_effect_stage_croppy:intoxication%` resolves to `"Drunk"`

#### Scenario: Query inactive status effect fallback values
- **WHEN** a player does not have `croppy_coffee:caffeinated` active
- **THEN** `%croppy_effect_active_croppy_coffee:caffeinated%` resolves to `"false"` and numeric/stage placeholders resolve to `"0"` or empty string
