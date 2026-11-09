# Design: Harmonize PlaceholderAPI Telemetry & Documentation

## Context

Croppy exposes real-time runtime state to PlaceholderAPI via `CroppyPlaceholderExpansion` in `croppy-integrations:papi`. See `proposal.md` for problem background.

Currently:
1. Compendium tokens diverge between the code implementation (`compendium_total`, `compendium_discovered`, `compendium_percentage`) and documentation (`compendium_total_recipes`, `compendium_discovered_recipes`, `compendium_completion_percentage`).
2. Station tokens diverge (`target_station_type` vs `target_station_name`), and cooking progress is exposed only as a boolean (`target_station_is_cooking`) rather than a percentage.
3. Crop species placeholder returns the MiniMessage formatted display name instead of the raw namespaced key.
4. Soil moisture is not parsed, and documentation advertises unbuilt N-P-K tokens.

## Goals / Non-Goals

**Goals:**
- Provide 100% backward compatibility for all existing placeholder keys while supporting documented descriptive variants.
- Ensure strict separation between machine-parsable keys (`croppy:grapes`) and player-facing display names (`Sweet Grapes`).
- Accurately compute active cooking progress from multi-phase process definitions.
- Provide dynamic soil moisture resolution supporting both direct farmland targeting and crop targeting.
- Align bilingual documentation (`docs/en` and `docs/es`) with the unified expansion tokens.

**Non-Goals:**
- Implement a full N-P-K soil biochemical matrix in this change (deferred to a dedicated soil chemistry spec).
- Introduce external dependencies or leak Bukkit types into `croppy-core` or `croppy-api`.

## Decisions

### 1. Dual-Key Parsing Strategy for Compendium & Station Aliases
- **Decision**: Normalize queries in `onRequest` by mapping descriptive aliases to their canonical equivalents:
  - `compendium_discovered_recipes` $\rightarrow$ `compendium_discovered`
  - `compendium_total_recipes` $\rightarrow$ `compendium_total`
  - `compendium_completion_percentage` $\rightarrow$ `compendium_percentage`
  - `target_station_name` $\rightarrow$ `target_station_type`
- **Rationale**: Zero performance overhead (simple string equality / switch dispatch) and guarantees existing user setups and wiki configurations work seamlessly without breaking changes.
- **Alternatives Considered**: Deprecating short names with log warnings. Rejected because PAPI requests execute on tick/render threads where logging causes severe console spam.

### 2. Separation of Canonical Species Key from Display Name
- **Decision**:
  - `%croppy_target_crop_species%` will return the raw `crop.getSpeciesKey().asString()` (e.g. `croppy:grapes`).
  - Introduce `%croppy_target_crop_species_name%` returning `Species::getName` (e.g. `<dark_purple>Sweet Grapes</dark_purple>`).
- **Rationale**: Administrative tools, DeluxeMenus, and conditional triggers require exact key comparisons without MiniMessage formatting tags. Display HUDs can use `_species_name`.
- **Alternatives Considered**: Leaving `_species` as display name and adding `_species_key`. Rejected because standard PAPI conventions and our own documentation designate `species` as the namespaced key.

### 3. Active Process Duration & Progress Calculation
- **Decision**:
  When evaluating `%croppy_target_station_cooking_progress%`:
  1. Retrieve active `ProcessInstance` from station's `ProcessComponent`.
  2. If none, return `0.0%`.
  3. Look up `ProcessDefinition` in `core.getProcessRegistry()`.
  4. Sum `durationTicks` across all `definition.phases()`.
  5. If total duration is 0, return `100.0%`.
  6. Calculate `double pct = Math.min(100.0, (process.getTotalElapsedTicks() * 100.0) / totalDuration)`.
  7. Format via `String.format(Locale.ROOT, "%.1f%%", pct)`.
- **Rationale**: Accurately reflects sequential phase advancement and matches the documented example `62.0%`.

### 4. Smart Target Soil Block Resolution
- **Decision**:
  When evaluating `%croppy_target_soil_moisture%` or `%croppy_target_soil_water%`:
  1. Check if the targeted block is `org.bukkit.block.data.type.Farmland`.
  2. If not, check if the block immediately below (`targetBlock.getRelative(BlockFace.DOWN)`) is `Farmland`.
  3. If farmland is found, extract `farmland.getMoisture()` (integer `0..7`).
  4. Return `moisture` for `target_soil_water`, or `String.format(Locale.ROOT, "%.0f%%", (moisture * 100.0) / 7.0)` for `target_soil_moisture`.
  5. If no farmland is resolved, return `0%` / `0`.
- **Rationale**: Players naturally aim at the crop plant itself when inspecting growth, but may also aim at unplanted farmland. Supporting both sightlines provides an intuitive UX.

### 5. Documentation Cleanup of Phantom Nutrients
- **Decision**: Remove `%croppy_target_soil_nitrogen%`, `%croppy_target_soil_phosphorus%`, and `%croppy_target_soil_potassium%` from `docs/en/integrations.md` and `docs/es/integrations.md`. Add documentation for previously undocumented tokens (`version`, `total_active_crops`, `total_active_stations`, `target_is_crop`, `target_is_station`, `target_crop_is_watered`, `target_crop_fertilizer`, `target_station_is_cooking`).
- **Rationale**: Avoids server owner confusion and bug reports for unimplemented features.

## Risks / Trade-offs

- **[Risk] Existing configurations expecting display name from `%croppy_target_crop_species%`**:
  $\rightarrow$ *Mitigation*: Clearly document `%croppy_target_crop_species_name%` and highlight the change in release notes.
- **[Risk] Zero duration in custom processes causing division by zero**:
  $\rightarrow$ *Mitigation*: Guard against `totalDuration <= 0` by returning `100.0%` if elapsed ticks > 0 or `0.0%` otherwise.

## Migration Plan

1. Implement updated resolution and alias branches in `CroppyPlaceholderExpansion.java`.
2. Expand `CroppyPlaceholderExpansionTest.java` with mock block data and recipe processes to verify all branches.
3. Update `docs/en/integrations.md` and `docs/es/integrations.md`.
