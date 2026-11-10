# Proposal

## Why

Crafting station base models and interaction hitboxes in Croppy are currently static once spawned via PacketEvents, limiting visual immersion for multi-stage cooking, heating, and storage states (such as unlit vs. burning stoves, or empty vs. boiling pots). Adding a declarative, condition-driven Station States system enables workstations to dynamically swap their 3D item models, custom model data, interaction hitboxes, and ambient sensory effects purely reactively based on physical components (temperature, fuel, inputs, process state) without entity respawning or database schema changes.

## What Changes

- Introduce declarative `states` definitions in station YAML configurations with condition expressions (`when`), priority sorting, model overrides, dynamic hitbox dimensions, and ambient sensory effects (particles and sound).
- Add pure Java 21 immutable API records in `croppy-api`: `StationStateDefinition`, `StationModelOverride`, and update `StationDefinition` to expose configured states.
- Implement reactive AST condition evaluation in `croppy-core` (`StationStateEvaluator`) backed by `EvaluationContext`, populating station physical metrics (`temperature`, `fuel_ticks`, `process_state`, `has_inputs`, `is_active`) and calculating the active state without persistent storage mutations.
- Implement hot metadata packet updates in `croppy-paper` (`PacketEventsStationRenderer` and `StationViewerTracker`) dispatching updated `INDEX_ITEM` (item model / custom model data), `INDEX_TRANSLATION`, `INDEX_SCALE`, and `INDEX_INTERACTION_WIDTH`/`HEIGHT` with zero visual entity flickering or despawn cycles.
- Integrate ambient state particles and sound playback into `PaperStationTicker` for active station states.

## Capabilities

### New Capabilities
<!-- No new standalone capabilities; this extends existing workstation modeling and visual projection. -->

### Modified Capabilities
- `crafting-stations`: Add requirement for reactive station states configuration, expression-based state resolution from physical components, and runtime state tracking on `StationInstance`.
- `station-virtual-displays`: Add requirement for hot base model metadata updating (item model, custom model data, transformations), dynamic interaction hitbox resizing via metadata packets, and state-level ambient sensory effect loops.

## Impact

- **API (`croppy-api`)**: Non-breaking additions: `StationStateDefinition`, `StationModelOverride`, updated constructor/factory for `StationDefinition`, and active state accessors on `StationInstance`.
- **Core (`croppy-core`)**: `YamlStationLoader` parses `states:` blocks; `StationStateEvaluator` compiles and evaluates AST condition expressions against station components. No changes to binary codecs or database schemas (reactive from physical state).
- **Paper (`croppy-paper`)**: `PacketEventsStationRenderer` adds `updateBaseModelMetadata` and `updateInteractionHitboxMetadata`; `StationViewerTracker` dirty-checks active state transitions; `PaperStationTicker` ticks ambient state effects.
- **Resource Pack / Configurations**: Workstation configs (`stove.yml`, `cooking_pot.yml`, `skillet.yml`) can define multiple visual states referencing modern `item_model` paths or `custom_model_data`.
