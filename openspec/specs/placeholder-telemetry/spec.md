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
The system SHALL parse placeholder requests for target crop species, developmental stage, maximum stage, growth progress percentage, soil hydration level, and nitrogen-phosphorus-potassium (N-P-K) nutrient values.

#### Scenario: Requesting target crop growth data
- **WHEN** a player placeholder requests `%croppy_target_crop_stage%` while aiming at a stage 3 crop
- **THEN** the placeholder resolves to "3"

#### Scenario: Requesting target soil moisture
- **WHEN** a player placeholder requests `%croppy_target_soil_water%` while aiming at farmland with water level 5
- **THEN** the placeholder resolves to "5"

### Requirement: Target Workstation Telemetry
The system SHALL parse placeholder requests for target cooking workstation type, current internal temperature, active cooking status, and process completion progress.

#### Scenario: Requesting workstation temperature
- **WHEN** a player placeholder requests `%croppy_target_station_temperature%` while aiming at a 180°C skillet
- **THEN** the placeholder resolves to the formatted temperature string

### Requirement: Player Compendium Progression
The system SHALL parse player-scoped placeholder requests reporting the count of discovered culinary recipes, total registered recipes, completion percentage, and specific recipe discovery flags.

#### Scenario: Requesting discovered recipe count
- **WHEN** a player placeholder requests `%croppy_compendium_discovered%`
- **THEN** the placeholder resolves to the numeric count of unique recipes discovered by the requesting player

### Requirement: Global Plugin Statistics
The system SHALL parse server-wide placeholder requests reporting active plugin version, total tracked crop instances, and total registered cooking stations.

#### Scenario: Requesting plugin version
- **WHEN** a placeholder requests `%croppy_version%`
- **THEN** the placeholder resolves to the active Croppy version string
