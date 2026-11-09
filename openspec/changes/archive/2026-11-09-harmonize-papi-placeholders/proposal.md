# Proposal: Harmonize PlaceholderAPI Telemetry & Documentation

## Why

PlaceholderAPI telemetry outputs and identifiers in `CroppyPlaceholderExpansion` diverge from the official documentation (`docs/en/integrations.md` and `docs/es/integrations.md`) and the durable capability specification. Several documented placeholders return `null` due to naming discrepancies (e.g. `%croppy_compendium_discovered_recipes%` vs `%croppy_compendium_discovered%`, `%croppy_target_station_name%` vs `%croppy_target_station_type%`), crop species telemetry returns formatted display names rather than canonical namespaced keys, and soil telemetry lacks concrete runtime resolution for farmland hydration levels.

Now is the ideal time to harmonize the placeholder subsystem with backward-compatible aliases, dedicated key vs display name tokens, actual cooking progress calculation, and accurate soil moisture resolution before third-party server networks build dependent HUDs and menus on disjointed keys.

## What Changes

- **Compendium Compatibility Aliases**: Support both short and descriptive keys for player culinary compendium telemetry (`%croppy_compendium_discovered%` and `%croppy_compendium_discovered_recipes%`, `%croppy_compendium_total%` and `%croppy_compendium_total_recipes%`, `%croppy_compendium_percentage%` and `%croppy_compendium_completion_percentage%`).
- **Target Crop Species Key vs Display Name**:
  - `%croppy_target_crop_species%`: Resolves strictly to the canonical namespaced key (`croppy:grapes`), matching the documentation and enabling reliable equality checks in menu and script conditions.
  - `%croppy_target_crop_species_name%` (New): Resolves to the human-readable MiniMessage formatted display name (`Sweet Grapes`).
- **Target Crop Stage & Max Stage Precision**:
  - `%croppy_target_crop_stage%`: Current 0-based stage index.
  - `%croppy_target_crop_max_stage%`: Maximum 0-based stage index (`totalStages - 1`).
  - `%croppy_target_crop_total_stages%` (New): Total stage count (`totalStages`).
  - `%croppy_target_crop_progress%`: Formatted consistently to one decimal place (`%.1f%%`, e.g. `74.5%`).
- **Target Cooking Station Telemetry**:
  - Support `%croppy_target_station_name%` as a direct alias for `%croppy_target_station_type%`.
  - Format `%croppy_target_station_temperature%` to one decimal place (`%.1f°C`, e.g. `185.0°C`).
  - Implement `%croppy_target_station_cooking_progress%` computing active multi-phase cooking progress from elapsed ticks vs total required duration ticks (`%.1f%%`).
- **Target Soil & Moisture Telemetry**:
  - Resolve soil blocks intelligently: if looking at a crop, check the block beneath (`y - 1`); if looking at farmland directly, check the targeted block.
  - Implement `%croppy_target_soil_moisture%` as percentage (`(moisture * 100) / 7`%).
  - Implement `%croppy_target_soil_water%` as vanilla moisture level integer (`0..7`).
  - Omit phantom N-P-K nutrient placeholders from active documentation until the dedicated soil chemistry engine is implemented.
- **Documentation & Test Suite Synchronization**: Update `docs/en/integrations.md`, `docs/es/integrations.md`, and expand `CroppyPlaceholderExpansionTest` to cover all placeholders and aliases.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `placeholder-telemetry`: Harmonize placeholder identifiers, formatting, and target resolutions for crops, stations, soil, and compendium.

## Impact

- **Code**: `croppy-integrations/papi/src/main/java/dev/darkblade/croppy/integration/papi/CroppyPlaceholderExpansion.java`.
- **Tests**: `croppy-integrations/papi/src/test/java/dev/darkblade/croppy/integration/papi/CroppyPlaceholderExpansionTest.java`.
- **Documentation**: `docs/en/integrations.md` and `docs/es/integrations.md`.
- **Breaking Changes**: None. All legacy and short-form identifiers remain fully supported via aliases.
