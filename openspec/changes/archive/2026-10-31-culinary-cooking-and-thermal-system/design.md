# Design

## Context

Croppy features an established Hexagonal Architecture across `croppy-api`, `croppy-core`, and `croppy-paper`. The engine recently integrated sequential processes (`ProcessEngine`), crafting stations (`DefaultStationManager`), and a Pratt AST expression compiler (`ExpressionCompiler`).

To implement Farmer's Delight-style culinary mechanics without bloating the codebase or hardcoding interactions, we need:
1. A thermal component model attached to stations.
2. A declarative interaction engine routing player actions via YAML configs in `content/interactions/`.
3. A declarative rule engine evaluating pre-compiled AST conditions to execute consequences from `content/rules/`.
4. Visual, particle, and sound synchronization for cookware in `croppy-paper`.

See `proposal.md` for the underlying motivation and architectural drivers.

## Goals / Non-Goals

**Goals:**
- **Zero Platform Leakage**: Keep `ThermalComponent`, `InteractionDefinition`, and `RuleDefinition` completely pure in `croppy-api` with zero Bukkit imports.
- **Fast AST Evaluation**: Pre-compile rule condition expressions at startup using `ExpressionCompiler` so evaluation during station ticks and interactions is zero-allocation and non-blocking.
- **Extensible Cookware & Actions**: Provide data-driven YAML loaders for stations (`content/stations/`), interactions (`content/interactions/`), rules (`content/rules/`), and items (`content/items/`).
- **Tactile Feedback**: Support `ItemDisplay` rendering (soup liquid levels inside pots, items laying flat on skillet surfaces), dynamic ambient particles, and procedural cooking sound loops.
- **Fail-Fast & Resilient**: Fail fast on invalid YAML configurations during startup, but protect runtime tick loops with fault-tolerant try-catch containment.

**Non-Goals:**
- Complex thermodynamic diffusion or atmospheric thermodynamics across adjacent chunks.
- Requiring client-side resource packs or custom client mods (relies entirely on Paper 1.21.8 Display Entities, Player Heads, and Vanilla particles).
- Custom furnace fuels or multiblock brick ovens (focuses on cookware placed above standard heat source blocks).

## Decisions

### 1. Thermal Modeling as a Modular Station Component (`ThermalComponent`)
- **Decision**: Define `ThermalComponent` extending `StationComponent` in `croppy-api`. The component tracks `currentTemperature`, `targetTemperature`, `heatSourcePresent`, and provides the derived `ThermalZone` enum (`COLD`, `WARM`, `SIMMER`, `BOIL`, `SEAR`, `SCORCH`).
- **Rationale**: Keeps thermal tracking optional and modular. Non-thermal machines (like a wooden seed separator or press) do not allocate thermal state, while cooking pots and skillets declare thermal support.
- **Alternatives Considered**: Storing temperature as an untyped entry in `StationInstance.properties()`. Rejected because typed components provide compile-time safety and clean binary serialization.

### 2. Declarative Interaction Dispatcher (`InteractionEngine`)
- **Decision**: Move player interaction logic out of static `StationInteractListener` branches into a dedicated `InteractionEngine` that evaluates registered `InteractionDefinition` models loaded from `content/interactions/*.yml`.
- **Rationale**: Allows server owners and content creators to modify how players interact with stations (e.g. shift-right-click to open GUI vs right-click with tool to stir/inspect) purely through configuration.
- **Alternatives Considered**: Hardcoded Java switch-cases for every station type. Rejected because it violates Croppy's data-driven extensibility principles.

### 3. Pre-Compiled Rule Engine (`RuleEngine`)
- **Decision**: Define `RuleDefinition` with trigger types (`STATION_TICK`, `STATION_INTERACT`, `PROCESS_FINISH`) and compile condition strings into Pratt AST `Expression` instances during YAML load.
- **Rationale**: Pre-compiling AST trees guarantees instantaneous condition checks on the main tick thread without string parsing overhead.
- **Alternatives Considered**: JavaScript/Nashorn or Groovy scripting. Rejected due to security sandbox risks and memory overhead on Paper servers.

### 4. Display Entity Transformation for Cookware
- **Decision**: Coordinate `ItemDisplay` entities using local translation and rotation vectors:
  - Cooking Pot: Render item horizontally inside pot interior ($Y + 0.35$).
  - Skillet: Render items laying flat ($X$-axis pitch $90^\circ$, $Y + 0.05$) to simulate food frying on the pan.
- **Rationale**: Paper 1.21.8 Display Entities provide smooth interpolation and client-side rendering with zero server tick lag compared to older ArmorStand invisible marker tricks.
- **Alternatives Considered**: ArmorStands with items in hand. Deprecated in modern Paper; Display Entities are vastly superior in visual quality and network packet efficiency.

## Risks / Trade-offs

- **[Display Entity Lifecycle Orphaning]** → *Mitigation*: Track spawned display entity UUIDs in `PaperDisplaySynchronizer` and enforce despawn checks on station destruction, chunk unloads, and plugin disable.
- **[Thermal Ticking Performance]** → *Mitigation*: Throttle heat source checks to every 10 ticks (`PaperStationTicker`), caching the underlying block material rather than querying Bukkit block states every single tick.
- **[Rule Cascades / Infinite Loops]** → *Mitigation*: Rules are evaluated sequentially with strict priority ordering and a non-reentrant execution context.
