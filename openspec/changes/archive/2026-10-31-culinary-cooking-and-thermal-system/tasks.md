# Tasks

## 1. Domain API Contracts (croppy-api)

- [x] 1.1 Create `ThermalZone` enum and `ThermalComponent` interface extending `StationComponent` in `croppy-api`, and verify compilation with `./gradlew.bat :croppy-api:compileJava`.
- [x] 1.2 Create `InteractionDefinition`, `InteractionTrigger`, and `InteractionAction` records and interfaces in `dev.darkblade.croppy.api.interaction`, and verify compilation with `./gradlew.bat :croppy-api:compileJava`.
- [x] 1.3 Create `RuleDefinition`, `RuleTrigger`, `RuleAction`, and `RuleSeverity` records in `dev.darkblade.croppy.api.rule`, and verify compilation with `./gradlew.bat :croppy-api:compileJava`.

## 2. Thermal Component & Persistence (croppy-core)

- [x] 2.1 Implement `DefaultThermalComponent` with temperature tracking, target convergence, and thermal zone calculation, and verify with JUnit 5 unit tests.
- [x] 2.2 Update `StationBinaryCodec` in `croppy-core` to serialize and deserialize `ThermalComponent` state, and verify with round-trip unit tests.

## 3. Declarative Interaction Engine (croppy-core)

- [x] 3.1 Implement `InteractionRegistry` and `DefaultInteractionRegistry` in `croppy-core`, and verify with unit tests.
- [x] 3.2 Implement `YamlInteractionLoader` using SnakeYAML to parse interaction definitions, and verify parsing and validation with unit tests.
- [x] 3.3 Implement `InteractionRouter` in `croppy-core` to match incoming player actions against registered interactions by priority, hand item, and conditions, and verify with unit tests.

## 4. Configurable Rule Engine (croppy-core)

- [x] 4.1 Implement `RuleRegistry` and `DefaultRuleRegistry` in `croppy-core`, and verify registration and priority sorting with unit tests.
- [x] 4.2 Implement `YamlRuleLoader` compiling Pratt AST condition expressions via `ExpressionCompiler`, and verify with unit tests.
- [x] 4.3 Implement `RuleEngine` executing consequences (`REPLACE_OUTPUT`, `REDUCE_QUALITY`, `DAMAGE_PLAYER`, `PLAY_SOUND`, `EMIT_PARTICLES`) upon triggered events, and verify with unit tests.

## 5. Platform Thermal & Interaction Adapters (croppy-paper)

- [x] 5.1 Implement `PaperThermalEvaluator` in `croppy-paper` detecting heat source blocks (campfires, furnaces, smokers, fire, magma/lava) at `y - 1`, and verify heat detection with unit tests.
- [x] 5.2 Update `StationInteractListener` to delegate to `InteractionRouter`, handling both physical tool actions and GUI invocations, and verify event routing.
- [x] 5.3 Implement culinary thermometer tool (`croppy:thermometer`) logic with configurable durability, sound playback, and MiniMessage action bar feedback.

## 6. Multi-Sensory Feedback & Default Content (croppy-paper)

- [x] 6.1 Update `PaperDisplaySynchronizer` to support cookware item display transforms (pot liquid depth and skillet flat rotation), and verify display entity positioning.
- [x] 6.2 Update `PaperStationTicker` to execute thermal convergence, ambient boiling/sizzling sounds, and steam/grease particles during active cooking processes.
- [x] 6.3 Create default YAML content files for cookware stations (`cooking_pot.yml`, `skillet.yml`), interactions, rules (`overcooking_burn.yml`, `thermal_burn.yml`), and items (`thermometer.yml`, `burnt_food.yml`).

## 7. Verification Gate

- [x] 7.1 Execute full test suite via `./gradlew.bat test` ensuring zero test failures and full build success.
