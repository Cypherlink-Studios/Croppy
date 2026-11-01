# Tasks

## 1. Pure API Contracts (`croppy-api`)

- [x] 1.1 Create `HeatSourceComponent` interface in `dev.darkblade.croppy.api.station.component` defining state getters/setters (`isActive`, `targetTemperature`, `outputTemperature`, `burnTicksRemaining`, `maxBurnTicks`, limits, and rates) and verify compilation with `./gradlew.bat :croppy-api:compileJava`
- [x] 1.2 Create `FuelProvider` interface and `FuelProperties` record in `dev.darkblade.croppy.api.fuel` and verify compilation with `./gradlew.bat :croppy-api:compileJava`

## 2. Core Domain Logic & Persistence (`croppy-core`)

- [x] 2.1 Implement `DefaultHeatSourceComponent` in `croppy-core` with scaled fuel burn dynamics and thermal convergence, and verify with unit tests in `DefaultHeatSourceComponentTest`
- [x] 2.2 Implement `DefaultFuelRegistry` in `croppy-core` providing mapping for item keys to `FuelProperties`, and verify with unit tests in `DefaultFuelRegistryTest`
- [x] 2.3 Upgrade `StationBinaryCodec` to version 3 to serialize and deserialize `HeatSourceComponent` while maintaining backwards compatibility with version 2 data, and verify with `StationBinaryCodecTest`

## 3. Platform Thermal Propagation & Fuel Adaptation (`croppy-paper`)

- [x] 3.1 Implement `PaperFuelService` implementing `FuelProvider` with support for vanilla fuels and custom PDC tags (`croppy:fuel_ticks`), and verify with unit tests
- [x] 3.2 Update `PaperThermalEvaluator` to query `stationManager` for an active `HeatSourceComponent` at `(x, y - 1, z)` before inspecting vanilla blocks, and verify with unit tests in `PaperThermalEvaluatorTest`
- [x] 3.3 Update `StationBlockListener` to instantiate and attach `DefaultHeatSourceComponent` when station definitions declare `heat_source: "true"`, and verify with unit tests

## 4. Station Control GUI & Chat Prompt Service (`croppy-paper`)

- [x] 4.1 Implement `ChatPromptService` listening on `AsyncChatEvent` with 30s timeout, bounds validation against station limits, and main-thread callback for GUI reopening, and verify with unit tests
- [x] 4.2 Extend `PaperMenuListener` to handle new station actions (`TOGGLE_POWER`, `ADJUST_TEMP`, `PROMPT_CHAT_TEMP`), and verify action execution logic with unit tests
- [x] 4.3 Configure a default heat source station (`croppy:stove`) and control menu definition (`croppy:stove_menu`) in resources, and verify loading via `YamlStationLoaderTest` and `YamlMenuLoaderTest`

## 5. Visual Feedback & Final Verification (`croppy-paper`)

- [x] 5.1 Add dynamic colored `DUST` particle emission in `PaperStationTicker` for active heat source stations reflecting current temperature tiers, and verify with ticker unit tests
- [x] 5.2 Execute full Gradle test verification gate (`./gradlew.bat test`) across all modules to ensure zero regressions and 100% test pass rate
