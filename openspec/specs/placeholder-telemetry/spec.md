# placeholder-telemetry Specification

## Purpose

Exposes real-time agricultural, soil chemistry, culinary workstation, and player progression telemetry through PlaceholderAPI expansions using configurable dynamic raycasting.

## Requirements

### Requirement: Dynamic Raycast Target Resolution
The system SHALL perform dynamic block raycasting from the player's eye location up to a configurable maximum distance to resolve the active crop or workstation under the player's crosshair.

#### Scenario: Player looks directly at crop within range
- **WHEN** a player evaluates target placeholders while aiming at a crop within the configured raycast distance
- **THEN** the system resolves target crop identifiers and telemetry accurately

#### Scenario: Player looks at block beyond range or empty air
- **WHEN** a player evaluates target placeholders while looking at empty air or beyond the maximum distance
- **THEN** target-specific placeholders return empty or fallback indicator values

### Requirement: Target Crop and Soil Telemetry
The system SHALL parse placeholder requests for target crop species (key and display name), developmental stage, maximum stage index, total stage count, growth progress percentage, and soil hydration level.

#### Scenario: Requesting target crop growth data
- **WHEN** a player placeholder requests `%croppy_target_crop_stage%` while aiming at a stage 3 crop
- **THEN** the placeholder resolves to "3"

#### Scenario: Requesting target soil moisture
- **WHEN** a player placeholder requests `%croppy_target_soil_water%` while aiming at farmland with water level 5
- **THEN** the placeholder resolves to "5"

#### Scenario: Requesting target crop species canonical key
- **WHEN** a player evaluates `%croppy_target_crop_species%` aiming at a grape crop
- **THEN** the placeholder resolves to the canonical namespaced key "croppy:grapes"

#### Scenario: Requesting target crop species display name
- **WHEN** a player evaluates `%croppy_target_crop_species_name%` aiming at a crop with name "<dark_purple>Sweet Grapes</dark_purple>"
- **THEN** the placeholder resolves to the formatted display name string

#### Scenario: Requesting target crop stage metrics
- **WHEN** a player evaluates `%croppy_target_crop_stage%`, `%croppy_target_crop_max_stage%`, and `%croppy_target_crop_total_stages%` aiming at stage 3 of a 5-stage crop
- **THEN** `%croppy_target_crop_stage%` resolves to "3", `%croppy_target_crop_max_stage%` resolves to "4", and `%croppy_target_crop_total_stages%` resolves to "5"

#### Scenario: Requesting target crop growth progress
- **WHEN** a player evaluates `%croppy_target_crop_progress%` aiming at a crop with progress 0.745
- **THEN** the placeholder resolves to "74.5%"

#### Scenario: Requesting target soil hydration percentage
- **WHEN** a player evaluates `%croppy_target_soil_moisture%` aiming at farmland with moisture level 6 (or a crop planted on it)
- **THEN** the placeholder resolves to "86%"

### Requirement: Target Workstation Telemetry
The system SHALL parse placeholder requests for target cooking workstation type or name, operating temperature, active cooking status, and process completion progress.

#### Scenario: Requesting workstation temperature
- **WHEN** a player placeholder requests `%croppy_target_station_temperature%` while aiming at a 180°C skillet
- **THEN** the placeholder resolves to the formatted temperature string "180.0°C"

#### Scenario: Requesting workstation name alias
- **WHEN** a player evaluates `%croppy_target_station_name%` or `%croppy_target_station_type%` aiming at a skillet workstation
- **THEN** both placeholders resolve to "croppy:skillet"

#### Scenario: Requesting active cooking progress percentage
- **WHEN** a player evaluates `%croppy_target_station_cooking_progress%` aiming at a station executing a recipe that is 62% complete across its phases
- **THEN** the placeholder resolves to "62.0%"

#### Scenario: Requesting cooking progress on idle station
- **WHEN** a player evaluates `%croppy_target_station_cooking_progress%` aiming at an idle station with no active process
- **THEN** the placeholder resolves to "0.0%"

### Requirement: Player Compendium Progression
The system SHALL parse player-scoped placeholder requests reporting the count of discovered culinary recipes, total registered recipes, completion percentage, and specific recipe discovery flags, supporting both canonical and descriptive alias tokens.

#### Scenario: Requesting discovered recipe count
- **WHEN** a player placeholder requests `%croppy_compendium_discovered%`
- **THEN** the placeholder resolves to the numeric count of unique recipes discovered by the requesting player

#### Scenario: Requesting discovered recipe count via aliases
- **WHEN** a player evaluates `%croppy_compendium_discovered%` or `%croppy_compendium_discovered_recipes%`
- **THEN** both placeholders resolve to identical numeric counts of unique recipes discovered by the requesting player

#### Scenario: Requesting total recipes count via aliases
- **WHEN** a player evaluates `%croppy_compendium_total%` or `%croppy_compendium_total_recipes%`
- **THEN** both placeholders resolve to identical total counts of registered recipes on the server

#### Scenario: Requesting compendium completion percentage via aliases
- **WHEN** a player evaluates `%croppy_compendium_percentage%` or `%croppy_compendium_completion_percentage%`
- **THEN** both placeholders resolve to identical formatted percentage strings with one decimal place

### Requirement: Global Plugin Statistics
The system SHALL parse server-wide placeholder requests reporting active plugin version, total tracked crop instances, and total registered cooking stations.

#### Scenario: Requesting plugin version
- **WHEN** a placeholder requests `%croppy_version%`
- **THEN** the placeholder resolves to the active Croppy version string
