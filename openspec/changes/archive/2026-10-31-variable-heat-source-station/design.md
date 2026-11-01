# Design

## Context

Croppy follows a strict Hexagonal Architecture across three Gradle modules:
- `croppy-api`: Pure Java 21 ports, records, and contracts with zero external dependencies.
- `croppy-core`: Pure Java 21 domain simulation, registries, and binary serialization codecs.
- `croppy-paper`: Paper 1.21.8+ platform adapter handling Bukkit events, PacketEvents displays, menu interaction, and scheduler tickers.

Currently, culinary cookware (`ThermalComponent`) evaluates heat by inspecting the vanilla block directly beneath (`y - 1`) via `PaperThermalEvaluator`. This design establishes a dedicated heat-generation architecture that enables custom variable heat source stations (stoves, burners, hearths) to emit adjustable heat, consume fuel dynamically, persist state across restarts, and interact via GUI and chat prompts.

See `proposal.md` for background and motivation.

## Goals / Non-Goals

**Goals:**
- Provide a pure `HeatSourceComponent` contract in `croppy-api` and in-memory implementation in `croppy-core`.
- Implement a modular `FuelProvider` port in `croppy-api` with a default registry in `croppy-core` and a platform adapter in `croppy-paper` supporting vanilla fuels, PDC-tagged custom fuels, and future ExecutableItems hooks.
- Scale fuel consumption dynamically with target temperature relative to station limits ($burn\_cost \propto T_{target}$).
- Support configurable station definitions with `min_temperature`, `max_temperature`, `heating_rate`, `cooling_rate`, and `fuel_burn_multiplier`.
- Enhance `PaperThermalEvaluator` to query the station manager for active heat source stations at `(x, y - 1, z)` prior to vanilla block evaluation.
- Provide a 27-slot control GUI with temperature increment/decrement buttons, fuel input slot, power toggle, and a chat prompt button.
- Implement `ChatPromptService` listening on `AsyncChatEvent` to allow numerical temperature input, validating station bounds, and immediately reopening the station GUI on the main thread.
- Render dynamic `DUST` particle feedback reflecting the station's operational temperature.
- Upgrade `StationBinaryCodec` to version 3 to persist `HeatSourceComponent` state.

**Non-Goals:**
- Adding a compile-time dependency on ExecutableItems (extensibility is achieved via the `FuelProvider` port).
- Entity burn damage when standing on active stoves (empty air thermal dissipation without damage).
- Ambient room heating or fluid heating beyond direct vertical transmission (`y + 1`).

## Decisions

### 1. Dedicated `HeatSourceComponent` vs. Reusing `ThermalComponent`
- **Decision**: Create a distinct `HeatSourceComponent` rather than overloading `ThermalComponent`.
- **Rationale**: `ThermalComponent` models heat consumers/cookware (tracking convergence, thermal zones for recipes, and passive cooling). In contrast, heat producers manage fuel combustion, remaining burn ticks, on/off power state, and target output emissions. Separating them prevents domain model pollution while allowing a machine to act as both if necessary.
- **Alternatives Considered**: Adding fuel and emission fields to `ThermalComponent` (rejected as it bloats pots, pans, and kegs with unused fuel fields).

### 2. Extensible `FuelProvider` Port
- **Decision**: Introduce a `FuelProvider` interface in `croppy-api`:
  ```java
  public interface FuelProvider {
      Optional<FuelProperties> resolveFuel(Key itemKey);
  }
  public record FuelProperties(long burnTicks, double maxAchievableTemp, double efficiencyMultiplier) {}
  ```
  In `croppy-paper`, `PaperFuelService` queries the pure registry first, then checks Bukkit/vanilla fuel values, then checks item PDC (`croppy:fuel_ticks`), and can delegate to external item plugins.
- **Rationale**: Keeps domain logic completely free of Bukkit/NBT types while granting full flexibility for custom item integrations.

### 3. Scaled Fuel Combustion Dynamics
- **Decision**: Fuel burn ticks are consumed according to operating temperature:
  $$\Delta ticks = base\_tick \times \left(1.0 + \frac{T_{output} - T_{min}}{T_{max} - T_{min}} \times 1.5\right)$$
- **Rationale**: Operating at a low simmer consumes fuel at base rate, whereas searing or scorch temperatures burn fuel up to 2.5x faster. This introduces strategic resource management for culinary recipes.

### 4. Chat Temperature Input with Automatic GUI Reopen
- **Decision**: When the player clicks `[SET]`, the station GUI closes and the player's UUID is registered in `ChatPromptService` with a 30-second expiry. An `AsyncChatEvent` handler intercepts the next message:
  - If the player types "cancel", the prompt cancels.
  - If a number is parsed, it is validated against `[minTemperature, maxTemperature]`.
  - On valid input, the prompt is resolved, target temperature updated, and `Bukkit.getScheduler().runTask()` is called to reopen the station GUI on the server main thread.
- **Rationale**: Ensures responsive chat input without blocking server threads, and guarantees Bukkit inventory operations execute strictly on the main thread.

### 5. Binary Persistence Version 3 Upgrade
- **Decision**: Bump `StationBinaryCodec.CURRENT_VERSION` to 3. Add serialization of `HeatSourceComponent`:
  - `boolean hasHeatSource`
  - `boolean isActive`
  - `double targetTemperature`
  - `double outputTemperature`
  - `long remainingBurnTicks`
  - `long maxBurnTicks`
- **Rationale**: Maintains backwards compatibility with version 1 and 2 records while persisting station state across chunk lifecycles.

## Risks / Trade-offs

- **[Risk] Chat Interception Leaking or Hanging** → *Mitigation*: Automatically expire pending chat prompts after 30 seconds with an audible expiration chime, and cancel the event so prompt responses are never broadcast to the server chat.
- **[Risk] Rapid GUI Clicking / Desync** → *Mitigation*: Synchronize state updates in `DefaultHeatSourceComponent` using thread-safe setters and refresh inventory contents on the tick following click actions.
- **[Risk] Particle Emission Overhead** → *Mitigation*: Limit `DUST` particle spawning to active stations during the recurring station ticker loop (only in loaded chunks, using low count per tick).
