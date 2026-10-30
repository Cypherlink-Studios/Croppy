# Tasks

## 1. Domain Model and API Contracts (`croppy-api`)

- [x] 1.1 Extend `CropInstance` with `getLastWateredDay()`, `setLastWateredDay(long)`, `getFertilizerKey()`, and `setFertilizerKey(Key)` methods and verify compilation with `./gradlew :croppy-api:compileJava`
- [x] 1.2 Define `Fertilizer` record/interface, `FertilizerType` enum (`SPEED`, `RETAINING`, `QUALITY`), and `FertilizerRegistry` service in `croppy-api` and verify compilation with `./gradlew :croppy-api:compileJava`
- [x] 1.3 Define `UnwateredPolicy` model and `UnwateredPolicyType` enum (`PAUSE`, `SLOW`, `DECAY_PROGRESS`, `WITHER`) in `croppy-api` and verify compilation with `./gradlew :croppy-api:compileJava`

## 2. Core Implementation and Storage Serialization (`croppy-core`)

- [x] 2.1 Update `DefaultCropInstance` with `lastWateredDay` and `fertilizerKey` fields and unit test instance creation and mutations in `DefaultCropInstanceTest`
- [x] 2.2 Upgrade `CropBinaryCodec` to Version 2 supporting `lastWateredDay` and `fertilizerKey`, preserving backward compatibility for Version 1 payloads, and verify with roundtrip and migration unit tests in `CropBinaryCodecTest`
- [x] 2.3 Implement `DefaultFertilizerRegistry` and `YamlFertilizerLoader` in `croppy-core` with SnakeYAML and verify with unit tests in `YamlFertilizerLoaderTest`
- [x] 2.4 Implement `FertilizerGrowthModifier` and integrate `UnwateredPolicy` execution and daily hydration checks into `GrowthEngine`, verifying with JUnit 5 unit tests in `GrowthEngineTest`

## 3. Persistent Storage Migration (`croppy-paper`)

- [x] 3.1 Update `SqliteCropStorage` schema to add `last_watered_day` and `fertilizer` columns and update insert, select, and batch update queries, verifying syntax and execution via test suite
- [x] 3.2 Update `PdcCropStorage` to persist and load Version 2 binary payloads seamlessly from Chunk PersistentDataContainers

## 4. Watering Mechanics and Tools (`croppy-paper`)

- [x] 4.1 Implement `WateringCanService` and item definition covering tiers (Copper, Iron, Gold, Diamond, Netherite), capacity tracking via PDC/lore, and AoE calculation (1x1, 1x3, 3x3, 5x5)
- [x] 4.2 Implement `WateringListener` handling right-click watering on crops and farmland, water refill from source blocks and water cauldrons, water usage decrement, and splash particle/sound feedback
- [x] 4.3 Implement `RainHydrationTask` and weather listeners to automatically hydrate sky-exposed crops during rain storms in precipitation-supporting biomes
- [x] 4.4 Implement `TimeSkipListener` for bed sleep events to synchronize daily moisture transitions smoothly

## 5. Fertilizer Mechanics and Soil Interactions (`croppy-paper`)

- [x] 5.1 Implement `FertilizerService` with a 30-second transient cache for empty tilled farmland coordinates
- [x] 5.2 Implement `FertilizerListener` handling player right-click application to active crops (enforcing 1-fertilizer limit) and tilled soil, consuming items and emitting particles/sounds
- [x] 5.3 Implement day-change retention roll for crops with `RETAINING` fertilizer, preserving hydration when the world day advances

## 6. Inspection, Commands, and Integration Verification (`croppy-paper`)

- [x] 6.1 Update `CropInspectorService` status card to display daily hydration status, dry day count, active fertilizer name, and unwatered policy, updating `CropInspectorServiceTest`
- [x] 6.2 Register Cloud 2.0 admin commands (`/croppy water`, `/croppy fertilize`, `/croppy give <watering_can|fertilizer>`) in `CroppyCommands`
- [x] 6.3 Run full project verification with `./gradlew clean check test build` to ensure all modules, tests, and artifacts compile and pass without warnings
