# Tasks

## 1. Domain API & Core Simulation

- [x] 1.1 Update `TextDisplayTemplate` record in `croppy-api` with `boolean hideWhenIdle` and backwards-compatible constructors, and verify compilation.
- [x] 1.2 Update `YamlStationLoader` in `croppy-core` to parse `hide_when_idle` and verify unit tests in `YamlStationLoaderTest`.
- [x] 1.3 Update `ProcessEngine` in `croppy-core` to calculate progress fractions, update `DisplayComponent.setProgress(float)`, and verify in `ProcessEngineTest`.
- [x] 1.4 Expand `StationPlaceholderFormatter` in `croppy-core` with direct and inverted process placeholders (`{progress_percent_left}`, `{progress_seconds_left}`, `{phase_name}`, etc.) and verify in `StationPlaceholderFormatterTest`.

## 2. Thermal Convergence, Dirty Flagging & Caching

- [x] 2.1 Update `PaperThermalEvaluator` in `croppy-paper` to detect temperature shifts (>= 0.5°C or zone change) and mark `DisplayComponent.setDirty(true)`, verifying in `PaperThermalEvaluatorTest`.
- [x] 2.2 Update `PaperMenuListener` and `StationInteractListener.handleServeOutput` in `croppy-paper` to mark `DisplayComponent.setDirty(true)` and clear/update displayed items on inventory operations.
- [x] 2.3 Add rendered component caching to `PacketStationDisplay` in `croppy-paper` to suppress redundant metadata packet transmissions when text is unchanged.

## 3. Station Viewer Tracker & PacketEvents Synchronization

- [x] 3.1 Update `StationViewerTracker.syncStation` in `croppy-paper` to implement the state-aware template hierarchy (IDLE/HEATING vs RUNNING phase template vs PAUSED vs COMPLETED) with accurate remaining tick countdowns.
- [x] 3.2 Implement dynamic `hide_when_idle` culling in `StationViewerTracker` (destroying text display entity when cold/empty and respawning upon heat or item placement).
- [x] 3.3 Add comprehensive unit tests in `croppy-paper` validating state-machine text generation, packet suppression, and `hide_when_idle` lifecycle.

## 4. Configuration & End-to-End Verification

- [x] 4.1 Update bundled `cooking_pot.yml` with `hide_when_idle: true` in `visual.text_display`.
- [x] 4.2 Run `./gradlew.bat test` across all modules and verify 100% test pass rate with 0 errors.
