# Design

## Context

See [proposal.md](proposal.md) for motivation. Currently, `ItemDefinition` (`croppy-api`) and `YamlItemLoader` (`croppy-core`) model only display/model/PDC metadata, while `ItemFactory` (`croppy-paper`) applies name, lore, custom model data, native `item_model`, skull texture, and `ItemMetadataContributor` outputs. Meanwhile, `Croppy` already provides `ExpressionCompiler` / `EvaluationContext` for boolean conditions, `AddonContext` / `PaperAddonContext` for scoped addon ownership, and `CroppyPlaceholderExpansion` for player-scoped telemetry.

Paper 1.21.8 exposes `DataComponentTypes.FOOD`, `DataComponentTypes.CONSUMABLE`, `DataComponentTypes.USE_REMAINDER`, and `DataComponentTypes.USE_COOLDOWN`, allowing any base `Material` (e.g., `POTION`, `PAPER`, `APPLE`) to behave natively as food or drink on the client. However, Croppy's hexagonal architecture forbids importing Bukkit/Paper/Kyori types into `croppy-api` or `croppy-core`.

## Goals / Non-Goals

**Goals:**
- Represent food, consumption physics, and declarative effects purely in `croppy-api` / `croppy-core` without platform dependencies.
- Materialize native Paper 1.21.8 consumption Data Components deterministically in `ItemFactory` while preserving item stackability per `(ItemDefinition, quality)`.
- Provide both an extensible instant `ConsumeEffectRegistry` and a stateful `PlayerEffectService` / `StatusEffectRegistry` accessible to addons via `AddonContext`.
- Deliver configurable, staged alcohol intoxication (`croppy:intoxication`) and opt-in hangover (`croppy:hangover`) mechanics usable declaratively in YAML or programmatically by addons.

**Non-Goals:**
- Migrating `Croppy-addons` (`coffee` or `brewery`) in this change (will be handled as a follow-up in `Croppy-addons`).
- Supporting arbitrary player-scoped SQLite persistence for status effects (opt-in PDC storage on the player entity is sufficient).
- Overwriting Croppy's existing inspector or watering-can ActionBar HUD by default.

## Decisions

### 1. Inline `ConsumableProperties` on `ItemDefinition` (with `nutrition:` Alias)
- **Decision**: Add an optional `ConsumableProperties` record (`FoodProperties`, consumption parameters, `QualityScaling`, and `List<ConsumeEffectSpec>`) directly to `ItemDefinition`, preserving all existing `ItemDefinition` constructors for backward compatibility. In `YamlItemLoader`, accept both `food:` and `nutrition:` (used by existing coffee addon definitions) as aliases.
- **Alternative Considered**: A separate `ConsumableRegistry` decoupled from `ItemDefinition`. Rejected as the primary mechanism because Croppy items are self-contained YAML files under `items/<category>/<id>.yml`.

### 2. Client Data Components vs. Server-Authoritative Effect Execution
- **Decision**: In `ItemFactory`, populate `DataComponentTypes.FOOD` (with quality-scaled `nutrition` and `saturation`), `DataComponentTypes.CONSUMABLE` (`consumeSeconds`, `animation`, `sound`, `hasConsumeParticles`), `DataComponentTypes.USE_REMAINDER`, and `DataComponentTypes.USE_COOLDOWN`, while leaving `Consumable.consumeEffects()` **empty**. Execute all `ConsumeEffectSpec` entries exclusively on the server inside `CroppyConsumptionListener` (`PlayerItemConsumeEvent`).
- **Rationale**: Embedding `PotionEffect`s inside the item's `Consumable` Data Component causes the client/vanilla server to apply effects unconditionally, bypassing `min-quality`, `chance`, `Expression` conditions, event cancellation, and custom handlers. Keeping `consumeEffects()` empty preserves native client eating/drinking UX (speed, animation, sound, remainder, cooldown) with a single server-authoritative execution path.
- **Alternative Considered**: Putting unconditional `croppy:potion` effects into `Consumable.consumeEffects()` and running only custom effects on `PlayerItemConsumeEvent`. Rejected due to split execution paths and inability to scale/gate cleanly when quality or conditions apply.

### 3. Hexagonal Separation of Instant and Stateful Effect Engines
- **Decision**: Split effect contracts cleanly across modules:
  - **`croppy-api`**:
    - `dev.darkblade.croppy.api.item`: `ConsumableProperties`, `FoodProperties`, `ConsumeCooldown`, `QualityScaling`, `ConsumeEffectSpec`, `UseAnimation`.
    - `dev.darkblade.croppy.api.consumable`: `ConsumeEffectHandler` (`Keyed`), `ConsumeEffectRegistry` (`Registry` + `Service`), `ConsumeExecutionContext` (player UUID, `ItemDefinition`, `StoredItem`, `EvaluationContext`, and a `PlayerActionPort` or platform-backed capability interface so built-ins remain testable).
    - `dev.darkblade.croppy.api.effect`: `CustomStatusEffect` (`Keyed`), `StatusEffectRegistry`, `ActiveStatusEffect`, `StackingPolicy` (`REPLACE`, `EXTEND`, `UPGRADE`, `STACK_INTENSITY`), `StatusEffectStage`, `PlayerEffectService`, `PlayerEffectStorage`, and `StatusEffectContext`.
  - **`croppy-core`**:
    - `DefaultConsumeEffectRegistry`, `DefaultStatusEffectRegistry`, `DefaultPlayerEffectService` (thread-safe via `ConcurrentHashMap`, pure Java time/tick advancement, offline decay calculation).
    - Pure-Java duration parser (`"45s"`, `"3m"`, `"900t"`, or integer ticks) and `ExpressionCompiler` integration for `condition:` strings.
  - **`croppy-paper`**:
    - Built-in Paper `ConsumeEffectHandler`s (`croppy:potion`, `croppy:remove_potion`, `croppy:clear_effects`, `croppy:heal`, `croppy:damage`, `croppy:oxygen`, `croppy:sound`, `croppy:particle`, `croppy:message`, `croppy:teleport_randomly`, `croppy:apply_status`, `croppy:reduce_status`, `croppy:remove_status`, `croppy:alcohol`).
    - `CroppyConsumptionListener` (`PlayerItemConsumeEvent` + `MILK_BUCKET` handling), `PaperEffectTicker` (main-thread periodic tick task), `PdcPlayerEffectStorage` (serializing `persistent: true` effects to player PDC with epoch-millis timestamps for offline decay), and `PaperIntoxicationEffect` + `PaperHangoverEffect`.
- **Rationale**: `croppy-core` stays 100% free of Bukkit/Paper/Kyori imports, unit-testable with JUnit 5, while `croppy-paper` handles all Bukkit `Player`, `PotionEffect`, `Sound`, `Particle`, and PDC mutations on the main thread.

### 4. Alcohol Intoxication (`croppy:intoxication`) & Hangover (`croppy:hangover`) Model
- **Decision**: Model `croppy:intoxication` as a `STACK_INTENSITY` built-in `CustomStatusEffect` on a `0.0..100.0` scale with continuous decay (`decay-per-second` configurable in `config.yml`), four configurable threshold stages (`Tipsy`, `Drunk`, `Heavily Drunk`, `Blackout`), short-pulsed `NAUSEA` (avoiding continuous screen distortion), stage-based buffs/debuffs, opt-in chat slurring (`AsyncChatEvent`) and subtle ground drift sway, and opt-in `croppy:hangover` triggered when decaying out of heavy intoxication (`>= 70.0`).
- **Rationale**: Gives any YAML consumable (`croppy:alcohol` with `abv`, `volume-ml`, `factor`) rich gameplay out of the box and establishes an official engine that `brewery` and `coffee` (`croppy:reduce_status`) can integrate with directly.

### 5. Addon Ownership Tracking & Core Reload Safety
- **Decision**: Track addon-registered `ConsumeEffectHandler` keys and `CustomStatusEffect` keys inside `PaperAddonContext` (parallel to items/recipes/interactions). On addon disable/unload, unregister those keys and purge active `PlayerEffectService` entries referencing removed status effects. On `/croppy reload`, reload host items and config while preserving addon-owned registrations.
- **Alternative Considered**: Global shared registries without addon tracking. Rejected because it violates `addon-system`'s leak-proof decommissioning guarantee.

### 6. Telemetry & Player Feedback Defaults
- **Decision**: Expose `%croppy_effect_active_<key>%`, `%croppy_effect_remaining_<key>%`, `%croppy_effect_amplifier_<key>%`, `%croppy_effect_intensity_<key>%`, and `%croppy_effect_stage_<key>%` in `CroppyPlaceholderExpansion`. Keep `consumables.auto-lore` opt-in (`false` by default) and HUD ActionBar/BossBar feedback configurable so existing item lore and inspector/watering ActionBars are never unexpectedly overwritten.

## Risks / Trade-offs

- **[Risk] ItemMeta / DataComponent mutation breaking item stacking** -> **Mitigation**: Derive `FOOD`, `CONSUMABLE`, `USE_REMAINDER`, and `USE_COOLDOWN` deterministically from `(ItemDefinition, quality)` inside `ItemFactory` without per-instance random fields, and verify stackability in `ItemFactory` tests.
- **[Risk] Nausea or sway mechanics annoying players or conflicting with anti-cheats** -> **Mitigation**: Apply `NAUSEA` in short configurable pulses, make physical sway and chat slurring opt-in (`false` by default in `config.yml`), and restrict sway to grounded players at tiny impulses.
- **[Risk] Milk bucket clearing custom status effects unexpectedly** -> **Mitigation**: Intercept `PlayerItemConsumeEvent` for `Material.MILK_BUCKET` and evaluate each active status effect's `milk-curable` / `milk-reduction` policy instead of treating milk as all-or-nothing.
- **[Risk] Addon disabling while players hold active addon status effects** -> **Mitigation**: Purge active player instances of any status effect whose definition is unregistered during `PaperAddonContext` teardown.
