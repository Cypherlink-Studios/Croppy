# consumable-effects Specification

## Purpose

Defines declarative food and consumable attributes on custom items, quality-scaled nutrition, native client consumption mechanics, and an extensible server-side pipeline for instant consumption effects.

## Requirements

### Requirement: Declarative Food and Consumable Attributes
The system SHALL support optional `food` (and backward-compatible `nutrition` alias) and `consumable` configuration sections on item definitions, specifying hunger, saturation, `can-always-eat`, consumption duration in seconds, use animation, sound, consume particles, optional remainder item key, optional cooldown (`seconds` and `group`), and optional quality scaling multipliers.

#### Scenario: Parse food and consumable sections from YAML
- **WHEN** an item YAML file defines `food` (`hunger`, `saturation`, `can-always-eat`) and `consumable` (`consume-seconds`, `animation`, `sound`, `particles`, `remainder`, `cooldown`)
- **THEN** the loader parses those attributes into the item's consumable properties with validated non-negative values

#### Scenario: Parse legacy nutrition alias block
- **WHEN** an item YAML file defines a `nutrition` block with `hunger` and `saturation` without a `food` key
- **THEN** the loader maps `nutrition` into the item's food properties seamlessly

#### Scenario: Reject invalid consumable values
- **WHEN** an item YAML file specifies negative `hunger`, negative `saturation`, or non-positive `consume-seconds` or `cooldown.seconds`
- **THEN** the loader logs a validation error and rejects or fails validation for the invalid consumable configuration

### Requirement: Quality-Scaled Nutrition and Effect Duration
The system SHALL support optional per-item quality scaling multipliers (`hunger-per-level`, `saturation-per-level`, `duration-per-level`) applied according to an item stack's quality grade (`0` to `3`).

#### Scenario: Scale nutrition for Gold quality consumable
- **WHEN** an item stack with quality `2` is constructed from a definition declaring `hunger: 4` and `hunger-per-level: 0.25`
- **THEN** the resulting item's food nutrition is scaled to `6` (`4 * (1 + 2 * 0.25)`)

#### Scenario: Preserve base nutrition when quality scaling is absent
- **WHEN** an item stack with quality `3` is constructed from a definition without `quality-scaling`
- **THEN** the item retains its base configured `hunger` and `saturation` values

### Requirement: Declarative Consumption Effect Entries
The system SHALL allow item definitions to declare an ordered list of `effects` entries, each specifying an effect type key, optional `chance` (`0.0` to `1.0`, default `1.0`), optional `min-quality` (`0` to `3`, default `0`), optional boolean `condition` expression, and arbitrary effect parameters.

#### Scenario: Gate effect by minimum item quality
- **WHEN** a player consumes an item of quality `0` whose effect entry declares `min-quality: 1`
- **THEN** the consumption pipeline skips that effect while still executing qualifying entries

#### Scenario: Evaluate conditional effect expression
- **WHEN** an effect entry specifies `condition: "item.quality >= 2"` and the consumed item has quality `2`
- **THEN** the consumption pipeline evaluates the condition as true and executes the effect

#### Scenario: Evaluate probabilistic effect chance
- **WHEN** an effect entry specifies `chance: 0.5`
- **THEN** the pipeline rolls a random value in `[0.0, 1.0)` and executes the effect only when the roll succeeds

### Requirement: Server-Authoritative Consumption Execution
The system SHALL apply physical food/drink behavior on the client item while keeping native Data Component consume-effects empty, executing all declared effects exclusively on the server when consumption completes.

#### Scenario: Execute effects on completed consumption
- **WHEN** a player finishes consuming a custom consumable item
- **THEN** the server resolves the item definition, quality, and properties, evaluates each effect entry, and dispatches matching effects on the main server thread

#### Scenario: Cancel or interrupt consumption before completion
- **WHEN** a player cancels eating or drinking before the consumption duration completes, or the consume event is cancelled
- **THEN** no custom or built-in consumption effects are executed

### Requirement: Extensible Consume Effect Registry and Built-In Handlers
The system SHALL maintain a thread-safe consume effect registry keyed by namespaced `Key`, pre-populated with built-in handlers (`croppy:potion`, `croppy:remove_potion`, `croppy:clear_effects`, `croppy:heal`, `croppy:damage`, `croppy:oxygen`, `croppy:sound`, `croppy:particle`, `croppy:message`, `croppy:teleport_randomly`, `croppy:apply_status`, `croppy:reduce_status`, `croppy:remove_status`, and `croppy:alcohol`), and extensible by addons.

#### Scenario: Apply built-in potion and heal effects on consumption
- **WHEN** a player consumes an item declaring `croppy:potion` (`minecraft:speed`) and `croppy:heal` (`amount: 4.0`)
- **THEN** the player receives the Speed potion effect and heals up to `4.0` health points without exceeding max health

#### Scenario: Dispatch to addon-registered custom consume handler
- **WHEN** an addon registers a handler under key `my_addon:custom_boost` and a player consumes an item referencing that type
- **THEN** the registry invokes the addon handler with the consumption context and parsed parameters

#### Scenario: Log missing consume effect handler gracefully
- **WHEN** an item references an unregistered effect type key during consumption
- **THEN** the system logs a warning with the item key and missing effect key and continues executing remaining valid effects
