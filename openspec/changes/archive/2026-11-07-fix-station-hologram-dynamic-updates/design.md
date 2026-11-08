# Design

## Context

Croppy v0.5.0 introduced sequential cooking processes and `ProcessDisplayFormatter` for phase-based holograms, but the runtime connection in `croppy-paper` was incomplete:
- `ProcessEngine` never called `display.setProgress(...)`.
- `StationViewerTracker` used elapsed ticks as remaining ticks and overrode the active phase template with `visual.text_display.format`.
- `PaperThermalEvaluator` converged temperatures without setting the dirty flag, leaving holograms visually stuck.
- Stations lacked an idle visibility culling mechanism, leading to unnecessary floating text for unused workstations.

See `proposal.md` for background motivation.

## Goals / Non-Goals

**Goals:**
- Unify `ProcessEngine` progress calculation with `DisplayComponent.setProgress(float)`.
- Correct time remaining calculations across `StationPlaceholderFormatter` and `ProcessDisplayFormatter`.
- Implement a state-aware hologram renderer in `StationViewerTracker` that activates phase templates during cooking and clean idle summaries when heating.
- Add `hide_when_idle` configuration to `TextDisplayTemplate` with dynamic entity destruction and respawning.
- Flag display updates dirty on thermal convergence (>= 0.5°C delta or zone shift) and GUI container interactions.
- Cache rendered text components in `PacketStationDisplay` to prevent redundant metadata packets.

**Non-Goals:**
- Floating holograms for crops (specifically scoped out).
- Modifying recipe matching or process state machine transition rules.
- Breaking database schemas or binary storage codecs.

## Decisions

### Decision 1: State-Aware Template Delegation in `StationViewerTracker`
- **Choice**: When a station has an active process (`ProcessState.RUNNING` or `AWAITING_INPUT`), delegate the rendered text component to `displayComp.getStatusText()` (formatted by `ProcessDisplayFormatter` with phase-specific templates, bars, and colors). When `IDLE`, format using the station definition's `visual.text_display.format` enriched with live temperature and thermal zone data. When `COMPLETED`, display the output ready prompt.
- **Alternative Considered**: Enforcing station YAML templates as the sole container and forcing `{status_message}` on all users. Rejected because it breaks phase-specific customized progress bars and sound/particle aesthetic coordination configured in `processes/*.yml`.

### Decision 2: Configurable Idle Visibility (`hide_when_idle`)
- **Choice**: Add `boolean hideWhenIdle` to `TextDisplayTemplate` (default: `false` for backwards compatibility). When enabled, if a station is at ambient temperature (<= 25.0°C), has empty storage (no inputs/outputs), and has no active process, `StationViewerTracker` destroys the `TEXT_DISPLAY` virtual entity for nearby viewers. As soon as heat or items are detected, the entity is spawned automatically.
- **Alternative Considered**: Hardcoding idle culling globally. Rejected because server owners may want persistent signage/labels on decorative kitchen blocks.

### Decision 3: Thermal Dirty Flagging
- **Choice**: Track previous converged temperature on `ThermalComponent` (or compare inside `PaperThermalEvaluator`). When the change exceeds 0.5°C or the `ThermalZone` changes, mark `displayComp.setDirty(true)`.
- **Alternative Considered**: Marking dirty on every single tick. Rejected because it would cause continuous packet evaluations even when stations have reached thermal equilibrium.

### Decision 4: Metadata Packet Caching on `PacketStationDisplay`
- **Choice**: Store the last serialized MiniMessage string or Adventure `Component` sent to client players on `PacketStationDisplay`. If `isDirty` is true but the resulting rendered Component is identical to the cached value, skip packet transmission.
- **Alternative Considered**: Relying solely on `isDirty` flags. Rejected because string comparisons guarantee zero redundant network packets during stable or paused states.

## Risks / Trade-offs

- **[Risk] Packet overhead from frequent temperature or progress updates**  
  → *Mitigation*: The station ticker runs at 20-tick (1s) intervals, distance culling limits rendering to players within 8 blocks, and packet caching eliminates duplicate transmissions.
- **[Risk] Missing phase display definition**  
  → *Mitigation*: If a process phase omits `display.template`, `ProcessDisplayFormatter` provides sensible default formatting with progress bars, and `StationViewerTracker` falls back to the station format if status text is empty.
- **[Risk] Backwards compatibility with custom station YAMLs**  
  → *Mitigation*: `hide_when_idle` defaults to `false` when omitted; all existing placeholders in `StationPlaceholderFormatter` and `ProcessDisplayFormatter` remain supported and are expanded.
