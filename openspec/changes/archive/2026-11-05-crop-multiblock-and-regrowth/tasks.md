# Tasks

## 1. Domain API Contracts (`croppy-api`)

- [x] 1.1 Add `SpatialFootprint` record and `getHeight()` accessor to `Species` contract, verifying pure Java 21 compilation with `./gradlew :croppy-api:compileJava`
- [x] 1.2 Add `RegrowthDefinition` record with `String targetStageId()`, update `HarvestDefinition`, and add `getStageById(String)` and `getStageIndex(String)` default helpers to `Species` interface, verifying compilation with `./gradlew :croppy-api:compileJava`
- [x] 1.3 Add `SNEAK_LEFT_CLICK` to `InteractionTrigger` enum and `Optional<Key> targetCrop()` to `InteractionDefinition` record, verifying compilation with `./gradlew :croppy-api:compileJava`

## 2. Core Engine & Registries (`croppy-core`)

- [x] 2.1 Update `CropManager` to maintain thread-safe `occupancyMap` secondary index for multi-block coordinates, supporting `getCropAt(BlockPosition)` and `isOccupied(BlockPosition)`, and verify with new unit tests in `CropManagerTest`
- [x] 2.2 Update `SpeciesDefinition` and `YamlSpeciesLoader` to parse `spatial.height` and `harvest.regrowth.target_stage`, enforcing fail-fast validation against invalid or terminal stage IDs, and verify with new unit tests in `YamlSpeciesLoaderTest`
- [x] 2.3 Generalize `YamlInteractionLoader` and `InteractionRouter` to load and evaluate crop interactions with `crop.stage_id`, `crop.is_mature`, and `crop.height` context variables, and verify with new unit tests in `InteractionRouterTest`

## 3. Paper Platform Adapters (`croppy-paper`)

- [x] 3.1 Update `CropPlantListener.onBlockCollisionSafeguard` to query `cropManager.getCropAt(pos)` across all spanned vertical blocks and prevent block placement into occupied space, verifying with unit tests in `CropPlantListenerTest`
- [x] 3.2 Update `VirtualInteractionPacketListener` and `CropBreakListener` to route crop clicks through `InteractionRouter`, handling `HARVEST_CROP` with stage regrowth and `DISMANTLE_CROP` with full footprint clearance, and verify with unit tests
- [x] 3.3 Update `PacketEventsBlockRenderer` and `VirtualBlockDisplayTracker` to adjust virtual `Interaction` entity height according to the current stage model's vertical footprint, verifying with unit tests

## 4. Reference Content & Configuration

- [x] 4.1 Create Trellis Grapes reference species (`crops/grapes.yml`), seed item (`items/seeds/grape_seeds.yml`), ingredient item (`items/ingredients/grapes.yml`), and declarative interactions (`interactions/crops/grapes.yml`), verifying valid loading during YAML species tests
- [x] 4.2 Create Paddy Rice reference species (`crops/rice.yml`), seed item (`items/seeds/rice_seeds.yml`), ingredient item (`items/ingredients/rice_panicle.yml`), and declarative interactions (`interactions/crops/rice.yml`), verifying valid loading during YAML species tests

## 5. End-to-End Verification

- [x] 5.1 Execute full automated test suite with `./gradlew test` across `croppy-api`, `croppy-core`, and `croppy-paper` and confirm 100% test pass rate with 0 errors
