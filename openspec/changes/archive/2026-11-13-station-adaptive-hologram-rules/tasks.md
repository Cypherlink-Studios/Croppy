# Tasks

## 1. API & Core Models

- [x] 1.1 Add `StationScope` and `stations` filter field to `RuleDefinition` in `croppy-api`, and verify compilation with `./gradlew :croppy-api:compileJava`
- [x] 1.2 Add `RuleTrigger.DISPLAY_LAYOUT` enum entry in `croppy-api`, and verify API tests pass
- [x] 1.3 Update `YamlRuleLoader` in `croppy-core` to parse `stations` mapping (`mode`, `ids`) and list shorthand with fallback to all stations
- [x] 1.4 Add unit tests in `croppy-core` for `YamlRuleLoader` validating `stations` filter loading and warning logging for unknown station keys

## 2. Rule Registry & Resolution Semantics

- [x] 2.1 Update `RuleRegistry` and `RuleEngine` in `croppy-core` to index rules by `(trigger, stationKey)` and filter non-matching stations
- [x] 2.2 Implement first-match resolution semantics for `RuleTrigger.DISPLAY_LAYOUT` in `RuleEngine`
- [x] 2.3 Add unit tests in `croppy-core` verifying station filtering and priority first-match execution for `DISPLAY_LAYOUT`

## 3. Platform Headroom Measurement & Transform Adaptation

- [x] 3.1 Implement vertical headroom measurement in `StationViewerTracker` (`croppy-paper`) using `Block#getCollisionShape()` and station top height, and expose `display.headroom` to `EvaluationContext`
- [x] 3.2 Add block change invalidation listener in `croppy-paper` to clear cached headroom when blocks at `pos.y() + 1` are altered
- [x] 3.3 Interpret `TEXT_DISPLAY_FIT` and `TEXT_DISPLAY_SCALE` actions in `StationViewerTracker` and update `PacketEventsStationRenderer` to dispatch adjusted translation and scale vectors
- [x] 3.4 Add unit tests for headroom calculation and transform adaptation logic

## 4. Action Bar Fallback Telemetry (Tier 3)

- [x] 4.1 Implement `TEXT_DISPLAY_FALLBACK` action handling in `StationViewerTracker` to suppress/despawn virtual `TextDisplay` entities
- [x] 4.2 Implement proximity view-cone look detection (`dot(look, dir) > cos(15°)`, distance $\le$ 4.0m) with 40-tick cooldown and priority check
- [x] 4.3 Implement flattened single-line telemetry formatting with configurable separator and optional template override for Action Bar delivery
- [x] 4.4 Hook Action Bar telemetry into station interaction handlers (`StationInteractListener`) for critical headroom stations

## 5. Default Rules & Full Integration

- [x] 5.1 Create default responsive display layout rules in `croppy-paper/src/main/resources/rules/` (`display_tight_headroom.yml`, `display_critical_headroom.yml`)
- [x] 5.2 Execute complete test suite (`./gradlew test`) and confirm zero failures across `croppy-api`, `croppy-core`, and `croppy-paper`
