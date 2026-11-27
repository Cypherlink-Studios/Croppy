# Tasks

## 1. Domain Contracts in `croppy-api`

- [x] 1.1 Add immutable consumable model records (`FoodProperties`, `ConsumeCooldown`, `QualityScaling`, `ConsumeEffectSpec`, `UseAnimation`, and `ConsumableProperties`) in `croppy-api` (`dev.darkblade.croppy.api.item`), enforcing parameter validation and defensive copies, and verify compilation with `./gradlew.bat :croppy-api:compileJava`.
- [x] 1.2 Extend `ItemDefinition` with optional `ConsumableProperties` while preserving all backward-compatible constructors, and verify compilation with `./gradlew.bat :croppy-api:compileJava`.
- [x] 1.3 Define instant consume effect contracts (`ConsumeEffectHandler`, `ConsumeEffectRegistry`, `ConsumeExecutionContext`) in `dev.darkblade.croppy.api.consumable` and stateful status effect contracts (`CustomStatusEffect`, `ActiveStatusEffect`, `StackingPolicy`, `StatusEffectStage`, `StatusEffectContext`, `StatusEffectRegistry`, `PlayerEffectService`, `PlayerEffectStorage`) in `dev.darkblade.croppy.api.effect`, verifying `:croppy-api:compileJava` succeeds with zero external dependencies.
- [x] 1.4 Expose `getConsumeEffectRegistry()`, `getStatusEffectRegistry()`, and `getPlayerEffectService()` on `AddonContext` with safe defaults where appropriate, and verify compilation across all modules (`./gradlew.bat :croppy-api:test`).

## 2. Core YAML Loading, Registries & Status Engine in `croppy-core`

- [x] 2.1 Update `YamlItemLoader` to parse `food` (including `nutrition` alias), `quality-scaling`, `consumable` (`consume-seconds`, `animation`, `sound`, `particles`, `remainder`, `cooldown`), and `effects` (`type`, `chance`, `min-quality`, `condition`, arbitrary parameters) including duration strings (`45s`, `3m`, `900t`), and verify with new JUnit 5 tests in `YamlItemLoaderTest`.
- [x] 2.2 Implement `DefaultConsumeEffectRegistry`, `DefaultStatusEffectRegistry`, and `ConsumptionEvaluator` (evaluating `min-quality`, `chance`, and `ExpressionCompiler` `condition` expressions against `EvaluationContext`), and verify edge cases with JUnit 5 unit tests in `croppy-core`.
- [x] 2.3 Implement `DefaultPlayerEffectService` supporting thread-safe per-player storage, `StackingPolicy` (`REPLACE`, `EXTEND`, `UPGRADE`, `STACK_INTENSITY`), continuous decay, `reduce` / `remove` operations, tick advancement, and offline timestamp decay restoration, and verify all policies and decay math with JUnit 5 unit tests.
- [x] 2.4 Wire `DefaultConsumeEffectRegistry`, `DefaultStatusEffectRegistry`, and `DefaultPlayerEffectService` into `CroppyCore` initialization and shutdown lifecycle, verifying `./gradlew.bat :croppy-core:test` passes.

## 3. Paper Data Components & Consumption Pipeline in `croppy-paper`

- [x] 3.1 Update `ItemFactory` to apply Paper 1.21.8 `DataComponentTypes.FOOD` (with quality-scaled nutrition), `DataComponentTypes.CONSUMABLE` (preserving empty native `consumeEffects`), `DataComponentTypes.USE_REMAINDER`, `DataComponentTypes.USE_COOLDOWN`, and opt-in `consumables.auto-lore` lines, and verify stackability and component output in `ItemFactory` unit tests.
- [x] 3.2 Implement built-in Paper `ConsumeEffectHandler`s (`croppy:potion`, `croppy:remove_potion`, `croppy:clear_effects`, `croppy:heal`, `croppy:damage`, `croppy:oxygen`, `croppy:sound`, `croppy:particle`, `croppy:message`, `croppy:teleport_randomly`, `croppy:apply_status`, `croppy:reduce_status`, `croppy:remove_status`, `croppy:alcohol`), and verify parameter parsing and execution behavior with unit tests.
- [x] 3.3 Implement `CroppyConsumptionListener` (`PlayerItemConsumeEvent`) executing server-authoritative effects on the main thread and handling `Material.MILK_BUCKET` reduction/cure rules, plus `PaperEffectTicker` and `PdcPlayerEffectStorage` (saving/restoring `persistent: true` effects on quit/join and clearing on death when configured), verified with Paper tests.
- [x] 3.4 Add `consumables` configuration settings (including `auto-lore`, effect tick interval, HUD options, and alcohol/hangover parameters) to `PluginConfig`, `config.yml`, and localization keys to `locales/en-US.yml` and `locales/es-ES.yml`, verifying config loading tests pass.

## 4. Built-In Alcohol Intoxication & Hangover Mechanics

- [x] 4.1 Implement `PaperIntoxicationEffect` (`croppy:intoxication`, `0.0..100.0` scale) with configurable stage thresholds (`Tipsy`, `Drunk`, `Heavily Drunk`, `Blackout`), pulsed `NAUSEA`, stage buffs/debuffs, opt-in `AsyncChatEvent` slurring, and opt-in grounded sway drift, verified with unit tests covering stage transitions and pulse timing.
- [x] 4.2 Implement opt-in `PaperHangoverEffect` (`croppy:hangover`) triggered when decaying out of heavy intoxication (`>= 70.0`) and removable/reducible via `croppy:reduce_status`, plus update bundled starter-pack food item YAMLs (e.g., `fried_egg.yml`, `tomato_soup.yml`, `vegetable_stew.yml`, `tomato.yml`) with appropriate `food`/`consumable` attributes, verified by loading and testing starter-pack items.

## 5. Addon Ownership Tracking & PlaceholderAPI Telemetry

- [x] 5.1 Update `PaperAddonContext` and addon teardown/core reload flows in `CroppyPlugin` to track and clean up addon-registered `ConsumeEffectHandler` and `CustomStatusEffect` keys (and purge removed status effects from `PlayerEffectService`), and verify cleanup in addon lifecycle tests.
- [x] 5.2 Extend `CroppyPlaceholderExpansion` (`croppy-integrations/papi`) to resolve `%croppy_effect_active_<key>%`, `%croppy_effect_remaining_<key>%`, `%croppy_effect_amplifier_<key>%`, `%croppy_effect_intensity_<key>%`, and `%croppy_effect_stage_<key>%`, and verify placeholder resolution with unit tests.

## 6. Cross-Module Verification

- [x] 6.1 Run the full Gradle test suite (`./gradlew.bat test`) and `openspec validate --all --strict` to confirm zero compilation errors, zero test failures, and valid OpenSpec artifacts across all modules.
