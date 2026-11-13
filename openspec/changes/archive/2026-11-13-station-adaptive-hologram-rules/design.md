# Design: Station Adaptive Hologram Rules

## Context

See `proposal.md` for background and motivation. Currently, crafting station `TextDisplay` entities use a static `translation` configured in station YAML files (e.g., `translation: [0.0, 1.35, 0.0]`). In builds with standard 2-block ceilings or overhead cabinetry, this causes the text display to intersect solid blocks at `pos.y() + 1`, occluding status text.

Croppy features an existing declarative rule engine (`croppy-core/src/main/java/dev/darkblade/croppy/core/rule`) that evaluates AST conditions and executes consequence actions. We leverage this engine to dynamically control visual layout without polluting station definition schemas.

## Goals / Non-Goals

**Goals:**
- Enable declarative visual layout adjustments for station holograms via `rules/*.yml`.
- Add a generic `stations` filter (`INCLUDE` / `EXCLUDE`) to `RuleDefinition` reusable across all rules.
- Accurately measure vertical headroom above stations using Minecraft collision bounding boxes.
- Implement responsive layout tiers: centering within available air gaps, reducing scale with compact formats, and falling back to Action Bar telemetry when space is critically constrained (< 0.30m).
- Maintain near-zero runtime performance overhead through caching and station-level layout evaluation.

**Non-Goals:**
- Modifying underlying 3D base models or collision hitboxes of stations.
- Supporting raycast-heavy client shaders or custom client-side mods.
- Redesigning crop visual displays (which already handle growth stage scaling).

## Decisions

### 1. Generic Station Scope Filter on `RuleDefinition`
- **Decision**: Add a `StationScope` record or model to `RuleDefinition`:
  ```java
  public record StationScope(FilterMode mode, Set<Key> stationKeys) {}
  ```
  Supporting YAML syntax:
  ```yaml
  stations:
    mode: INCLUDE # or EXCLUDE
    ids: ["croppy:cooking_pot", "croppy:skillet"]
  ```
  with list shorthand `stations: ["croppy:cooking_pot"]` defaulting to `INCLUDE`. If absent, matches all stations.
- **Rationale**: Keeps rule configuration clean and flexible. Other rules (such as `croppy:thermal_burn`) immediately benefit from being able to restrict themselves to specific cookware.
- **Alternatives considered**: Station-specific display flags inside `stations/*.yml`. Rejected to avoid schema bloat and keep presentation rules centralized.

### 2. First-Match Resolution for `DISPLAY_LAYOUT` Trigger
- **Decision**: Introduce `RuleTrigger.DISPLAY_LAYOUT`. Unlike simulation triggers (`STATION_TICK`, `STATION_INTERACT`) that apply all matching rules, `DISPLAY_LAYOUT` resolves the single highest-priority rule whose conditions evaluate to true.
- **Rationale**: Visual properties (scale, translation, fallback mode) are mutually exclusive tiers. First-match guarantees deterministic, conflict-free layouts.

### 3. Vertical Headroom Measurement & Caching (`croppy-paper`)
- **Decision**:
  - In `StationViewerTracker`, inspect `world.getBlockAt(pos.x(), pos.y() + 1, pos.z())`.
  - Use `Block#getCollisionShape().getBoundingBoxes()` to find the lowest collision point $Y_{ceiling}$ above the station.
  - Calculate $\text{headroom} = Y_{ceiling} - Y_{top}$, where $Y_{top}$ is the visual top of the station model.
  - Cache headroom per station in `PacketStationDisplay` or tracking state.
  - Invalidate cache when block physics or block placement/destruction occurs at `pos.y() + 1`, augmented with a low-frequency re-evaluation check (~every 200 ticks).
- **Rationale**: Paper's `VoxelShape` collision bounds accurately reflect slabs (top/bottom), trapdoors, stairs, and full blocks. Caching ensures zero per-tick world queries.

### 4. Layout Actions Execution
- **`TEXT_DISPLAY_FIT`**: Adjusts translation Y dynamically:
  $$Y_{target} = Y_{top} + \frac{\text{headroom}}{2}$$
  bounded by configured padding.
- **`TEXT_DISPLAY_SCALE`**: Applies scale vector (e.g., `0.75`) and optional compact format override.
- **`TEXT_DISPLAY_FALLBACK` (`target: ACTION_BAR`)**:
  - Suppresses spawning of the virtual `TextDisplay` entity (or despawns it).
  - Telemetry is dispatched to player Action Bar on interaction (right-click) or via proximity view-cone check (`dot(look, dir) > cos(15°)`, distance $\le$ 4.0 blocks, evaluated every 10 ticks for Tier 3 stations).
  - Formats telemetry using a flattened single-line template with customizable `separator` (` • ` by default).
  - Respects a 2-second priority window so transient messages are not prematurely overwritten.

## Risks / Trade-offs

- **[Risk] World mutations at `Y+1` not triggering immediate update** → **Mitigation**: Listen to `BlockBreakEvent`, `BlockPlaceEvent`, `BlockPistonExtendEvent`, `BlockPistonRetractEvent`, and `EntityExplodeEvent` targeting monitored station ceiling positions, combined with periodic safety re-sampling.
- **[Risk] Action Bar spam or flicker** → **Mitigation**: Cooldown check of 40 ticks between repeated view-cone dispatches unless the formatted string changes, plus a priority lock preventing overwrite of active interaction messages.
- **[Risk] Unknown station key in YAML filter** → **Mitigation**: Log a gentle warning instead of throwing an exception during rule loading, supporting dynamic addon loading.
