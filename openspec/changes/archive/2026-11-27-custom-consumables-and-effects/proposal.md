# Proposal

## Why

Croppy custom food items and addon beverages currently lack native edible/drinkable attributes and a unified effect execution engine. As a result, base food items in `croppy-paper` rely on vanilla material fallbacks (such as `COOKED_BEEF` or `BEETROOT_SOUP`), addon items like Coffee's `POTION`-based drinks declare unimplemented `nutrition:` blocks, and addons like Brewery have had to build isolated `PlayerItemConsumeEvent` listeners and ad-hoc intoxication maps.

Introducing native Paper 1.21.8 consumable Data Components alongside an extensible instant and stateful custom effect system unlocks custom foods, drinks, quality-scaled nutrition, alcohol intoxication mechanics, and addon-registered effects across Croppy and its ecosystem.

## What Changes

- **Declarative Food & Consumable Attributes on `ItemDefinition`**: Extend `ItemDefinition` (`croppy-api`) and `YamlItemLoader` (`croppy-core`) with an optional `ConsumableProperties` model supporting `food` (aliasing `nutrition`: `hunger`, `saturation`, `can-always-eat`, optional `quality-scaling`), `consumable` (`consume-seconds`, `animation`, `sound`, `particles`, `remainder`, `cooldown`), and declarative `effects` (`chance`, `min-quality`, `condition` via `ExpressionCompiler`, and parameters).
- **Native Paper 1.21.8 Data Component Materialization**: Update `ItemFactory` (`croppy-paper`) to apply `DataComponentTypes.FOOD`, `DataComponentTypes.CONSUMABLE` (keeping vanilla `consumeEffects` empty so server-side evaluation remains authoritative), `DataComponentTypes.USE_REMAINDER`, and `DataComponentTypes.USE_COOLDOWN`, with optional opt-in nutrition/effects auto-lore.
- **Extensible Instant Consumption Effect Pipeline**: Introduce `ConsumeEffectHandler`, `ConsumeEffectRegistry`, and `ConsumeExecutionContext` in `croppy-api`/`croppy-core`, executed by `CroppyConsumptionListener` (`croppy-paper`) on `PlayerItemConsumeEvent`. Ship built-in handlers (`croppy:potion`, `croppy:remove_potion`, `croppy:clear_effects`, `croppy:heal`, `croppy:damage`, `croppy:oxygen`, `croppy:sound`, `croppy:particle`, `croppy:message`, `croppy:teleport_randomly`, `croppy:apply_status`, `croppy:reduce_status`, `croppy:remove_status`, `croppy:alcohol`).
- **Stateful Custom Status Effect Engine**: Introduce `CustomStatusEffect`, `ActiveStatusEffect`, `StatusEffectRegistry`, and `PlayerEffectService` in `croppy-api`/`croppy-core` supporting stacking policies (`REPLACE`, `EXTEND`, `UPGRADE`, `STACK_INTENSITY`), continuous decay, main-thread ticking (`PaperEffectTicker`), milk/antidote handling, and opt-in PDC persistence with offline timestamp decay.
- **Built-In Alcohol Intoxication Mechanics**: Provide configurable `croppy:intoxication` (staged progression: *Tipsy*, *Drunk*, *Heavily Drunk*, *Blackout*, pulsed nausea, optional chat slurring/sway, and natural reduction via water/milk/coffee) and opt-in `croppy:hangover` status effects.
- **Addon & Telemetry Integration**: Expose `ConsumeEffectRegistry`, `StatusEffectRegistry`, and `PlayerEffectService` on `AddonContext` with ownership tracking and cleanup on addon disable/reload, and expose `%croppy_effect_*%` placeholders in `CroppyPlaceholderExpansion`.

## Capabilities

### New Capabilities
- `consumable-effects`: Declarative item food/consumable attributes, Paper 1.21.8 Data Component materialization, quality-aware nutrition scaling, conditional/chance-based effect evaluation, extensible `ConsumeEffectRegistry`, and built-in instant consumption handlers.
- `player-status-effects`: Stateful timed/decaying `CustomStatusEffect` registry and `PlayerEffectService`, stacking policies, main-thread ticker execution, opt-in PDC persistence, milk/antidote rules, and built-in alcohol intoxication (`croppy:intoxication`) and hangover (`croppy:hangover`) mechanics.

### Modified Capabilities
- `item-registry`: `ItemDefinition` and `YamlItemLoader` accept optional `ConsumableProperties` (including `nutrition:` alias compatibility), and `ItemFactory` applies native `FOOD`, `CONSUMABLE`, `USE_REMAINDER`, and `USE_COOLDOWN` Data Components when present.
- `addon-system`: `AddonContext` exposes `ConsumeEffectRegistry`, `StatusEffectRegistry`, and `PlayerEffectService`, and tracks addon-registered consume/status effect keys for deterministic cleanup during addon disable/unload and core reloads.
- `placeholder-telemetry`: `CroppyPlaceholderExpansion` exposes player-scoped status effect placeholders (`%croppy_effect_active_<key>%`, `%croppy_effect_remaining_<key>%`, `%croppy_effect_amplifier_<key>%`, `%croppy_effect_intensity_<key>%`, and `%croppy_effect_stage_<key>%`).

## Impact

- **`croppy-api`**: Updates `ItemDefinition` (preserving backward-compatible constructors) and `AddonContext`; adds `api.consumable` / `api.effect` contracts and registry/service interfaces.
- **`croppy-core`**: Updates `YamlItemLoader` and `CroppyCore`; adds `DefaultConsumeEffectRegistry`, `DefaultStatusEffectRegistry`, `DefaultPlayerEffectService`, `QualityScaling`, and pure-Java status effect state/tick logic with JUnit 5 tests.
- **`croppy-integrations/papi`**: Updates `CroppyPlaceholderExpansion` with player status effect telemetry queries.
- **`croppy-paper`**: Updates `ItemFactory`, `PaperAddonContext`, `PluginConfig`, `config.yml`, `locales/en-US.yml` / `es-ES.yml`, and `CroppyPlugin`; adds `CroppyConsumptionListener`, `PaperEffectTicker`, `PdcPlayerEffectStorage`, built-in Paper consume/status handlers (including `croppy:intoxication` and `croppy:hangover`), and unit/integration tests.
- **`Croppy-addons` (Non-blocking follow-up)**: No immediate changes required in `Croppy-addons`; `Croppy` preserves backward compatibility while exposing the new registries for future migration of `coffee` and `brewery`.
