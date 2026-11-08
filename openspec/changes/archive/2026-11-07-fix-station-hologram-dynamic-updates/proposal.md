# Proposal

## Why

Floating status holograms (`TextDisplay`) above crafting and cooking workstations currently fail to update dynamically during server execution. Although `ProcessDisplayFormatter` and phase templates were introduced in v0.5.0, cooking progress is stuck at 0%, progress bars remain empty, time countdowns reflect elapsed rather than remaining ticks, thermal convergence on heat sources fails to trigger metadata packets, and idle stations clutter player kitchens with permanent, unstyled text. This change resolves the architectural disconnect between simulation and PacketEvents rendering to make cooking holograms truly reactive, informative, and configurable.

## What Changes

- **Progress & Countdown Synchronization**: Connect `ProcessEngine` to `DisplayComponent.setProgress(float)` and fix remaining time calculations so progress bars, percentages, and time countdowns update in real-time.
- **Dynamic Hologram State Machine**: Implement a context-aware rendering hierarchy in `StationViewerTracker`:
  - When **IDLE / HEATING**: Renders station name, current temperature, and thermal zone classification.
  - When **RUNNING / AWAITING_INPUT**: Delegates directly to the active phase's `display.template` (with custom progress bar lengths, fill/empty colors, and prompts) from `processes/*.yml`.
  - When **PAUSED**: Displays an explicit alert when temperature falls below phase requirements (e.g., `<yellow>Paused: Insufficient Heat</yellow>`).
  - When **COMPLETED**: Renders a serving alert (e.g., `<green>Cooking Complete! Click to serve</green>`).
- **Thermal & Interaction Dirty Flagging**: Automatically flag `DisplayComponent.setDirty(true)` when temperature changes significantly (>= 0.5°C or zone transition), when items are placed or extracted via station GUIs (`PaperMenuListener`), and when cooked dishes are served (`handleServeOutput`).
- **Idle Visibility Toggle (`hide_when_idle`)**: Introduce `hide_when_idle: true/false` in `visual.text_display` (in `croppy-api`, `croppy-core`, and workstation YAMLs) so cold, empty, inactive stations dynamically cull their floating text entities to avoid visual clutter.
- **PacketEvents Metadata Caching**: Cache the last rendered Adventure `Component` or serialized string per station in `PacketStationDisplay` to prevent redundant packet dispatches when text has not changed.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `station-virtual-displays`: Update holographic text requirements to include dynamic state-machine rendering, `hide_when_idle` dynamic entity culling, and dirty tracking on thermal convergence.
- `sequential-processes`: Update phase display requirements to ensure active phase templates take precedence over default station templates and remaining countdowns reflect real time left.

## Impact

- **`croppy-api`**: `TextDisplayTemplate` record updated with `hideWhenIdle` field.
- **`croppy-core`**: `ProcessEngine` updates `DisplayComponent` progress fraction; `YamlStationLoader` parses `hide_when_idle`; `StationPlaceholderFormatter` unified with process placeholders.
- **`croppy-paper`**: `StationViewerTracker` dynamic state evaluation and entity visibility culling; `PacketStationDisplay` text caching; `PaperThermalEvaluator` and `PaperMenuListener` dirty flagging; default `cooking_pot.yml` updated.
- **Backward Compatibility**: Fully backward compatible; existing workstation YAMLs default `hide_when_idle` to `false`.
