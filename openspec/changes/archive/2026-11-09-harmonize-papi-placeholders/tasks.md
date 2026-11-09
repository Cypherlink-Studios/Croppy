# Tasks: Harmonize PlaceholderAPI Telemetry & Documentation

## 1. Expansion Core Logic

- [x] 1.1 Implement compendium compatibility aliases (`compendium_discovered_recipes`, `compendium_total_recipes`, `compendium_completion_percentage`) in `CroppyPlaceholderExpansion.java`
- [x] 1.2 Update target crop telemetry in `CroppyPlaceholderExpansion.java` to return canonical namespaced key for `target_crop_species`, add `target_crop_species_name`, support `target_crop_total_stages`, and format `target_crop_progress` to one decimal place (`%.1f%%`)
- [x] 1.3 Implement station aliases (`target_station_name` -> `target_station_type`), format `target_station_temperature` to one decimal place (`%.1f°C`), and implement multi-phase `target_station_cooking_progress`
- [x] 1.4 Implement smart soil raycasting and moisture placeholders (`target_soil_moisture` and `target_soil_water`) resolving both direct farmland targeting and crop targeting

## 2. Unit Testing & Verification

- [x] 2.1 Expand `CroppyPlaceholderExpansionTest.java` with unit tests verifying compendium aliases, species key vs name, stage metrics, station cooking progress, and soil moisture calculations
- [x] 2.2 Execute `./gradlew test` and confirm all test suites pass with zero failures

## 3. Documentation Synchronization

- [x] 3.1 Synchronize `docs/en/integrations.md` with unified placeholder tables, documented aliases, removed phantom N-P-K tokens, and previously unlisted tokens
- [x] 3.2 Synchronize `docs/es/integrations.md` with unified placeholder tables matching the English documentation

## Workflow follow-up

- Sync delta specs to main specs under `openspec/specs/placeholder-telemetry/spec.md`.
- Archive change `harmonize-papi-placeholders`.
