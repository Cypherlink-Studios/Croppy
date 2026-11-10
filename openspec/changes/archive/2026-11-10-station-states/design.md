# Design

## Context

Croppy workstations (`StationDefinition`) currently define a static `StationVisualDefinition` whose `base_model` is spawned once as a virtual `ItemDisplay` or `BlockDisplay` entity via PacketEvents. While `process_display` (in-process item) and `text_display` (hologram) update dynamically during cooking and ticking, the base model cannot visually reflect physical state transitions (e.g., fuel ignition, temperature increase, lid removal, liquid boiling).

See `proposal.md` for problem motivation and scope.

## Goals / Non-Goals

**Goals:**
- Provide declarative `states` definitions in station YAML configurations with condition expressions (`when`), priority sorting, model overrides, dynamic hitbox dimensions, and ambient sensory effects.
- Model state definitions as pure Java 21 immutable records in `croppy-api` (`StationStateDefinition`, `StationModelOverride`), maintaining zero platform dependencies.
- Implement pure reactive runtime state evaluation in `croppy-core` (`StationStateEvaluator`) backed by the AST `condition-engine` (`EvaluationContext`), resolving the active state from physical components without database or binary codec persistence.
- Dispatch hot PacketEvents metadata packets in `croppy-paper` (`INDEX_ITEM`, `INDEX_TRANSLATION`, `INDEX_SCALE`, `INDEX_INTERACTION_WIDTH/HEIGHT`) during state transitions without destroying or respawning virtual entities.
- Execute ambient particle and sound sensory loops for active states in `PaperStationTicker`.

**Non-Goals:**
- Persistent database fields or binary codec serialization for active station state (states are derived reactively from physical components).
- Functional workstation gameplay mutation (altering menu definitions or recipe filtering per state) in this initial phase; focus is on visual and sensory state.
- Dynamic block material swapping of the underlying world block (virtual display packet updates only).

## Decisions

### Decision 1: Hybrid Reactive AST Condition Resolution (Option C)
- **Choice**: Workstation states are defined with a priority integer and an optional `when` condition string evaluated by `ExpressionCompiler` (Pratt AST). The highest-priority matching state is selected. If no condition matches, the workstation resolves to a fallback default state (`priority: 0` or implicit base visual).
- **Rationale**: Physical workstations in Minecraft change state organically (temperature rising, fuel burning, items placed into storage). A reactive predicate engine decouples state changes from imperative code and allows addon authors to declare custom visual rules without writing Java code.
- **Alternatives Considered**:
  - *Strict Finite State Machine (FSM)*: Rejected due to rigid transition graphs that poorly model continuous multi-variable conditions (e.g. temperature + fuel + storage).

### Decision 2: Zero Persistence for Station States
- **Choice**: Active station states are not persisted in SQLite or PersistentDataContainers (PDC). When a chunk loads, the station state is evaluated freshly from its restored physical components (`ThermalComponent`, `HeatSourceComponent`, `StorageComponent`, `ProcessComponent`).
- **Rationale**: Prevents database migration complexity and avoids desynchronization bugs (e.g. a station loading as "boiling" when its temperature was cooled while unloaded).

### Decision 3: Zero-Despawn Metadata Packet Updates
- **Choice**: When a workstation's active state changes, `PacketEventsStationRenderer` sends a `WrapperPlayServerEntityMetadata` packet updating:
  - `INDEX_ITEM` (index 23): Updated `ItemStack` with configured `item_model` or `custom_model_data`.
  - `INDEX_TRANSLATION` (index 11) & `INDEX_SCALE` (index 12): Updated transform with interpolation ticks.
  - `INDEX_INTERACTION_WIDTH` (index 8) & `INDEX_INTERACTION_HEIGHT` (index 9): Updated hitbox dimensions on the virtual `INTERACTION` entity.
- **Rationale**: Despawning and respawning display entities causes client-side visual stutter and resets interpolation animations. Updating metadata preserves client frame stability.

### Decision 4: Direct `item_model` and `custom_model_data` Overrides
- **Choice**: Station state model configurations directly support `item_model` (Minecraft 1.21.4+ native namespaced key) and `custom_model_data` (integer), mirroring the structure used in crop developmental stages (`Model3DDefinition`).
- **Rationale**: Familiar configuration syntax for content creators and direct compatibility with modern resource packs.

### Decision 5: Ambient Sensory Loop Integration
- **Choice**: `StationStateDefinition` supports optional `PhaseEffects` (particle and periodic sound definitions). During the recurring `PaperStationTicker` run, the active state's ambient effects are emitted at the workstation location.
- **Rationale**: Provides multi-sensory feedback (e.g. smoke and crackle sounds for lit stoves; steam and bubbling for boiling pots) natively without requiring an active cooking recipe.

## Risks / Trade-offs

- **[Risk] Condition evaluation overhead on every tick** → *Mitigation*: Expression ASTs are precompiled and cached by `ExpressionCompiler`. `StationViewerTracker` dirty-checks `activeStateId` to avoid redundant packet dispatches.
- **[Risk] Flickering on invalid item model key** → *Mitigation*: `ItemFactory` validates `item_model` resolution and falls back safely to the workstation's base item if an invalid key is specified.
- **[Risk] Interaction hitbox desynchronization on client** → *Mitigation*: Hitbox updates are sent in the same synchronization tick as base model updates.
