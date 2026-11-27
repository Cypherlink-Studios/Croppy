# player-status-effects Specification

## Purpose

Manages stateful timed and continuously decaying custom status effects on players, supporting configurable stacking policies, lifecycle hooks, opt-in persistence, antidote interactions, and built-in alcohol intoxication mechanics.

## Requirements

### Requirement: Custom Status Effect Registry and Lifecycle
The system SHALL maintain a thread-safe status effect registry keyed by namespaced `Key`, allowing core and addons to define status effects with stacking policy (`REPLACE`, `EXTEND`, `UPGRADE`, `STACK_INTENSITY`), maximum duration, maximum amplifier, maximum intensity, continuous decay rate, persistence flag, milk cure policy, and lifecycle callbacks (`onApply`, `onTick`, `onExpire`, `onRemove`).

#### Scenario: Register and apply a custom timed status effect
- **WHEN** a custom status effect is applied to a player for a positive duration
- **THEN** the service stores the active effect state, invokes `onApply`, and ticks it periodically on the main server thread until expiration or removal

#### Scenario: Expire status effect cleanly
- **WHEN** an active status effect's remaining duration reaches zero or its decaying intensity reaches zero
- **THEN** the service removes the active effect and invokes `onExpire`

### Requirement: Status Effect Stacking and Intensity Policies
The system SHALL resolve repeated applications of the same status effect according to its configured `StackingPolicy`, and SHALL allow instant effects or API calls to reduce or remove active status effects.

#### Scenario: Accumulate intensity under STACK_INTENSITY policy
- **WHEN** an effect with `STACK_INTENSITY` policy is applied to a player who already has that effect active
- **THEN** the new intensity is added to the current intensity, clamped to `maxIntensity`, and remaining duration is refreshed

#### Scenario: Reduce active status intensity via antidote
- **WHEN** a player consumes an item configured with `croppy:reduce_status` targeting an active status effect
- **THEN** the service subtracts the specified intensity or duration and removes the effect if the remaining value reaches zero

### Requirement: Opt-In PDC Persistence with Offline Decay
The system SHALL serialize active status effects marked `persistent: true` to the player's PersistentDataContainer upon disconnect and restore them upon join after applying elapsed offline time decay.

#### Scenario: Restore persistent status effect after reconnect
- **WHEN** a player with a persistent status effect disconnects for 30 seconds and rejoins
- **THEN** the system restores the effect minus 30 seconds of elapsed duration or decay, expiring it immediately if elapsed time exhausted the effect

#### Scenario: Discard volatile status effects on quit or death
- **WHEN** a player with a non-persistent status effect disconnects or dies (when `clear-on-death` applies)
- **THEN** the volatile status effect is cleared without persisting across sessions

### Requirement: Milk and Antidote Consumption Handling
The system SHALL intercept player consumption of milk buckets and apply each active status effect's configured milk policy (`milk-curable` and optional `milk-reduction` fraction).

#### Scenario: Milk partially reduces alcohol intoxication
- **WHEN** a player with active `croppy:intoxication` drinks a milk bucket and config sets `milk-reduction: 0.5`
- **THEN** the player's intoxication intensity is halved rather than instantly cleared, while `milk-curable: true` effects are removed

### Requirement: Built-In Alcohol Intoxication and Hangover Mechanics
The system SHALL provide a built-in configurable `croppy:intoxication` status effect (scale `0.0` to `100.0`) triggered by `croppy:alcohol` or API calls, progressing through configurable stages (`Tipsy`, `Drunk`, `Heavily Drunk`, `Blackout`) with pulsed nausea, stage-appropriate buffs/debuffs, opt-in chat slurring and physical sway, and an opt-in `croppy:hangover` transition.

#### Scenario: Progress from Tipsy to Drunk upon repeated alcohol consumption
- **WHEN** a player consumes alcoholic beverages raising `croppy:intoxication` from `25.0` into the `Drunk` stage threshold (`>= 45.0`)
- **THEN** the player loses the early `Tipsy` buff and receives pulsed nausea and `Slowness I` while intoxication continuously decays over time

#### Scenario: Trigger opt-in hangover after heavy intoxication subsides
- **WHEN** `hangover.enabled` is true, a player's intoxication reached `>= 70.0`, and subsequently decays below the recovery threshold
- **THEN** the system applies the `croppy:hangover` status effect, which can be alleviated by `croppy:reduce_status` items such as coffee
