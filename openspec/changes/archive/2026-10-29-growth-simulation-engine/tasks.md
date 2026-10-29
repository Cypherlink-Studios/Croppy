# Tasks: Motor de Simulación de Crecimiento (Growth Simulation Engine)

## 1. Core API Growth Abstractions (croppy-api)

- [x] 1.1 Model `CropEnvironment` record with `lightLevel`, `waterFluidLevel`, and `isLoaded` properties and verify compilation with `./gradlew :croppy-api:classes`.
- [x] 1.2 Model `GrowthContext` and `GrowthModifier` functional interface in `croppy-api` and verify compilation with `./gradlew :croppy-api:classes`.
- [x] 1.3 Define `EnvironmentProvider` and `DisplaySynchronizer` port interfaces in `croppy-api` and verify compilation with `./gradlew :croppy-api:classes`.

## 2. Core Simulation Engine & Testing (croppy-core)

- [x] 2.1 Implement `GrowthEngine` in `croppy-core` with condition verification, modifier pipeline calculation, and stage advancement.
- [x] 2.2 Register and wire `GrowthEngine` into `CroppyCore` service management and lifecycle.
- [x] 2.3 Create comprehensive JUnit 5 unit tests in `GrowthEngineTest` covering light thresholds, water fluid level thresholds (0 to 8), modifier multiplications, and stage advancement; verify with `./gradlew :croppy-core:test`.

## 3. Paper Adapters, Events & World Display Sync (croppy-paper)

- [x] 3.1 Implement cancellable `CropGrowEvent` in `croppy-paper/event` package and verify compilation with `./gradlew :croppy-paper:classes`.
- [x] 3.2 Implement `PaperEnvironmentProvider` with chunk load safety checks (`world.isChunkLoaded`) and 9x9 horizontal water fluid scanning (mapping source to 8, flowing water to 1..7).
- [x] 3.3 Implement `PaperDisplaySynchronizer` updating world block data (`Ageable.setAge(...)`) for vanilla display types.
- [x] 3.4 Implement `PaperGrowthTicker` as a scheduled Bukkit task driving the simulation, evaluating crops, dispatching `CropGrowEvent`, and updating display.
- [x] 3.5 Add growth settings to `croppy-paper/src/main/resources/config.yml` and wire ticker startup/cancellation in `CroppyPlugin.onEnable()` and `onDisable()`.

## 4. Full Verification & Integration Check

- [x] 4.1 Execute full build and unit tests with `./gradlew test` and `./gradlew build`, verifying zero compile warnings and all unit tests passing.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and commit to git branch main.
