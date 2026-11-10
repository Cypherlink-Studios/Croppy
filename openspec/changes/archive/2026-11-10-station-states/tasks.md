# Tasks

## 1. Pure API Contracts (croppy-api)

- [x] 1.1 Create `StationModelOverride` immutable record in `dev.darkblade.croppy.api.station.visual` with `Key itemModel`, `int customModelData`, `DisplayTransform transform`, and validation. Verify compilation with `./gradlew :croppy-api:compileJava`.
- [x] 1.2 Create `StationStateDefinition` immutable record in `dev.darkblade.croppy.api.station` with `String id`, `int priority`, `Optional<String> condition`, `Optional<StationModelOverride> model`, `Optional<StationInteractionBox> hitbox`, and `Optional<PhaseEffects> effects`. Verify compilation with `./gradlew :croppy-api:compileJava`.
- [x] 1.3 Update `StationDefinition` to expose `List<StationStateDefinition> states()`, providing backward-compatible constructors and defensive copying. Verify unit tests pass with `./gradlew :croppy-api:test`.
- [x] 1.4 Add active state tracking accessors to `StationInstance` interface (`String getActiveStateId()`, `void setActiveStateId(String stateId)`). Verify compilation with `./gradlew :croppy-api:test`.

## 2. Core Domain Engine & Parsing (croppy-core)

- [x] 2.1 Update `DefaultStationInstance` in `croppy-core` to implement `getActiveStateId` and `setActiveStateId` using volatile thread-safe field defaulting to empty or fallback state. Verify with `./gradlew :croppy-core:test`.
- [x] 2.2 Update `YamlStationLoader` in `croppy-core` to parse `states:` blocks in workstation YAML files into `List<StationStateDefinition>` supporting model overrides, hitbox dimensions, and sensory effects. Add YAML parsing unit tests and verify with `./gradlew :croppy-core:test`.
- [x] 2.3 Implement `StationStateEvaluator` in `croppy-core` constructing `EvaluationContext` with physical metrics (`temperature`, `thermal_zone`, `fuel_ticks`, `is_active`, `process_state`, `has_inputs`, `input_count`, `has_outputs`) and resolving the active state by descending priority. Add unit tests covering priority evaluation and fallback in `StationStateEvaluatorTest` and verify with `./gradlew :croppy-core:test`.

## 3. Paper Platform Adapter & PacketEvents (croppy-paper)

- [x] 3.1 Update `ItemFactory` in `croppy-paper` with helper method to derive an `ItemStack` applying a `StationModelOverride` (`item_model` and `custom_model_data`). Verify unit tests with `./gradlew :croppy-paper:test`.
- [x] 3.2 Implement `updateBaseModelMetadata` and `updateInteractionHitboxMetadata` in `PacketEventsStationRenderer` dispatching `WrapperPlayServerEntityMetadata` for `INDEX_ITEM`, `INDEX_TRANSLATION`, `INDEX_SCALE`, `INDEX_INTERACTION_WIDTH`, and `INDEX_INTERACTION_HEIGHT` without entity respawning. Verify with `./gradlew :croppy-paper:test`.
- [x] 3.3 Update `StationViewerTracker` to evaluate `StationStateEvaluator.evaluate()`, dirty-check active state transitions on `StationInstance`, and trigger hot metadata packet dispatch to viewers when state transitions occur. Verify with `./gradlew :croppy-paper:test`.
- [x] 3.4 Update `PaperStationTicker` to execute ambient particles and sound playback for the station's active state during recurring tick loops. Verify with `./gradlew :croppy-paper:test`.

## 4. Configuration & Full Test Suite Verification

- [x] 4.1 Update workstation resource configurations (`cooking_pot.yml`, `stove.yml`, `skillet.yml`) to define sample `states:` with model overrides, dynamic hitboxes, and ambient effects. Verify loading with `./gradlew test`.
- [x] 4.2 Execute full workspace build and test suite (`./gradlew test`) ensuring zero compilation errors, zero test failures, and architectural integrity.
