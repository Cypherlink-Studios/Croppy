# Design: Crafting Stations, Condition Engine, and Process Runtime

## Context

Croppy has established domain models for crop agriculture, soil hydration, fertilizers, and binary persistence. To evolve into an artisan processing and cooking simulation engine (see `proposal.md - Why`), Croppy requires three foundational systems:
1. An expression and condition evaluation engine.
2. A crafting station and custom block runtime.
3. A sequential, multi-phase process state machine.

These systems must strictly adhere to the project's hexagonal architecture (`AGENTS.md`), keeping pure domain logic in `croppy-core` and platform-specific Bukkit/Paper adaptations in `croppy-paper`.

## Goals / Non-Goals

**Goals:**
- **Zero-Dependency Core Ports (`croppy-api`)**: Define pure Java 21 records and ports for expressions, evaluation contexts, station components, and process definitions.
- **High-Performance AST Expression Evaluator (`croppy-core`)**: Implement a Pratt / recursive-descent parser that compiles human-readable string expressions into immutable AST nodes evaluated in nanoseconds.
- **Sister Subsystem Station Manager (`croppy-core`)**: Implement `StationManager` parallel to `CropManager`, sharing spatial coordinates (`BlockPosition`, `ChunkPosition`) without degrading crop simulation performance.
- **Dual Station Interaction (`croppy-paper`)**: Seamless support for tactile in-world right-click item insertion and virtual container GUI inspection.
- **Extensible Multi-Phase State Machine (`croppy-core`)**: Execute staged recipes supporting timed, interactive, and environmental phases with configurable failure and quality policies.

**Non-Goals:**
- Refactoring `CropInstance` into a generic block component (existing crop lifecycle remains intact as a specialized sister subsystem).
- Heavy external script runtimes (Nashorn, Rhino, Lua). The expression engine is custom, lightweight, and sandbox-safe.
- Resource pack model generation (visuals leverage vanilla materials and Paper `ItemDisplay` / `BlockDisplay` entities).

## Decisions

### 1. Sister Subsystem Architecture over Monolithic ECS
- **Decision**: Keep `CropManager` for biological farming and introduce `StationManager` for mechanical stations, sharing the underlying spatial indexing (`ChunkPosition` -> `BlockPosition`).
- **Rationale**: Isolates biological concerns (growth stages, sunlight, soil moisture) from mechanical concerns (inventories, recipes, multi-phase states). Prevents performance degradation when ticking thousands of crops.
- **Alternatives Considered**: Refactoring `CropInstance` to a pure ECS block model. Rejected due to unnecessary complexity, object allocation overhead on mass crops, and regression risk on verified crop code.

### 2. Pratt AST Expression Engine with Context Resolvers
- **Decision**: Implement a custom tokenizer and Pratt parser in `croppy-core` compiling compact string expressions (e.g. `temp >= 100 && %papi% > 5`) into an immutable AST (`ExpressionNode`).
- **Rationale**: Expressions are compiled once upon configuration load. Runtime evaluation is a simple tree traversal without string allocations or regex matching.
- **Alternatives Considered**: 
  - Verbose YAML node trees: Difficult to read and write for content creators.
  - Third-party expression libraries (MVEL, SpEL): Introduced unwanted external dependencies in `croppy-api` / `croppy-core`.

### 3. Hexagonal PlaceholderAPI Resolution
- **Decision**: Define a pure `VariableResolver` port in `croppy-api`. In `croppy-paper`, implement `PlaceholderApiVariableResolver` that intercepts `%...%` tokens when an actor (Player) is present in the `EvaluationContext`.
- **Rationale**: Preserves zero-dependency integrity in API/Core while providing seamless PlaceholderAPI support on Paper servers. If PlaceholderAPI is absent, queries safely resolve to empty without runtime errors.

### 4. Component-Based Station State
- **Decision**: `StationInstance` holds discrete, optional components:
  - `StorageComponent`: Input, output, and fuel slot handling.
  - `ProcessComponent`: Current `ProcessInstance` execution state.
  - `DisplayComponent`: In-world visual display tracking (`ItemDisplay` / particles).
- **Rationale**: Allows lightweight stations (e.g., cutting board with just storage) and complex stations (e.g., industrial distillery with processes, fuel, and displays) to share one unified lifecycle.

### 5. Multi-Phase Finite State Machine (`ProcessInstance`)
- **Decision**: Model recipes as a sequence of `PhaseDefinition` objects with explicit execution modes:
  - `TIMED`: Advances after fixed tick count if conditions hold.
  - `MANUAL_INTERACTION`: Enters `AWAITING_INPUT` state, awaiting player item application within a configurable `timeout_ticks`.
  - `ENVIRONMENTAL`: Advances when world/block condition thresholds are met.
- **Rationale**: Elegantly unifies single-stage artisan machines (Kegs fermenting for 1000 ticks) and interactive culinary recipes (Boil -> Add Vegetables -> Stir -> Simmer).

## Risks / Trade-offs

- **[Risk] High frequency ticking on multiple stations**  
  → *Mitigation*: Only stations with an active `ProcessComponent` in a running state are registered with the active station ticker. Idle stations incur zero tick overhead.
- **[Risk] Player desynchronization with in-world displays**  
  → *Mitigation*: All visual display transformations and particle emissions execute synchronously on the Paper main server thread through a dedicated adapter.
- **[Risk] Spoilage and timeout tracking across chunk unload/server restart**  
  → *Mitigation*: Store timestamps (`worldTime` / system epoch) on active phases so elapsed time is accurately recalculated when chunks reload.
