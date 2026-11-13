# Proposal

## Why

In interior or enclosed player builds (such as kitchens with standard 2-block ceilings, counters with overhead cabinets, or compact cellars), station floating status holograms (`TextDisplay`) collide with solid blocks directly above (`Y+1`). Because Minecraft's rendering engine enforces standard depth buffer occlusion on Text Display entities, the top lines of the status hologram get clipped or buried inside solid world geometry, impairing readability and player feedback.

Rather than inflating station configuration schemas with rigid layout parameters, this change delegates visual adaptation to Croppy's declarative rule system (`rules/*.yml`), allowing server administrators and pack creators to configure responsive layout tiers (adaptive headroom centering, scaling, and action bar fallbacks) across individual or grouped stations.

## What Changes

- Add a generic `stations` filter to `RuleDefinition`, supporting `INCLUDE` and `EXCLUDE` modes to scope any rule to specific station identifiers.
- Introduce `RuleTrigger.DISPLAY_LAYOUT` evaluated when updating station visual layouts.
- Implement first-match resolution semantics for `DISPLAY_LAYOUT` rules, applying the highest-priority matching layout rule.
- Calculate dynamic vertical clearance (`display.headroom`) in `StationViewerTracker` using world collision bounds above the station (`pos.y() + 1`) with per-station caching and block-change invalidation.
- Support responsive display layout actions:
  - `TEXT_DISPLAY_FIT`: Dynamically centers text display within the available vertical clearance pocket.
  - `TEXT_DISPLAY_SCALE`: Adjusts text scale and optionally applies a compact single-line or multi-line format template.
  - `TEXT_DISPLAY_FALLBACK`: Suppresses the text display entity when clearance is critical (< 0.30m) and redirects telemetry to the player's Action Bar via interaction and proximity view-cone checks.
- Add default layout rules in `rules/` for standard, tight, and critical headroom tiers.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `rule-engine`: Introduces generic `stations` filter on `RuleDefinition`, `RuleTrigger.DISPLAY_LAYOUT`, and priority first-match execution for visual layout rules.
- `station-virtual-displays`: Adds vertical headroom detection (`display.headroom`), dynamic layout transformation (fitting and scaling), and Action Bar fallback projection for obstructed stations.

## Impact

- **API & Core**:
  - `croppy-api`: Updated `RuleDefinition` with `stations` filter; added `RuleTrigger.DISPLAY_LAYOUT`.
  - `croppy-core`: `YamlRuleLoader` parses `stations` mapping or list shorthand; `RuleRegistry` / `RuleEngine` indexes rules by station key and supports first-match resolution for `DISPLAY_LAYOUT`.
- **Platform (croppy-paper)**:
  - `StationViewerTracker`: Measures and caches `display.headroom` using Bukkit `Block#getCollisionShape()`; evaluates `DISPLAY_LAYOUT` rules; dispatches adjusted transforms or culls `TextDisplay`; handles view-cone Action Bar updates for critical tiers.
  - `PacketEventsStationRenderer`: Supports dynamic scale and custom translation overrides when synchronizing `TextDisplay` metadata.
- **Content & Configuration**:
  - Adds default rules in `croppy-paper/src/main/resources/rules/` for responsive station displays without requiring changes to existing station YAML schemas.
