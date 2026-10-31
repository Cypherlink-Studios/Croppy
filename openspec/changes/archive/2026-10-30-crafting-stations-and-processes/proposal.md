# Proposal: Crafting Stations and Process Engine

## Why

Croppy needs to expand beyond raw agriculture into processing, culinary crafting, and interactive machinery (e.g., fermenters, preserves jars, cooking pots, mills). Currently, the engine only tracks biological crops with no generic framework for interactive tile stations, condition checking with variables, or multi-phase recipe state machines. Introducing these foundational systems as decoupled, reusable engines unlocks complex artisan processing, multi-stage cooking, and condition-gated mechanics across the plugin.

## What Changes

- **Condition & Expression Engine (`croppy-api` & `croppy-core`)**:
  - Compact string expression syntax supporting comparisons (`==`, `!=`, `<`, `<=`, `>`, `>=`), logical operators (`&&`, `||`, `!`), grouping parentheses, and arithmetic (`+`, `-`, `*`, `/`).
  - Pure Java Pratt/recursive-descent AST parser and evaluator caching parsed ASTs for ultra-fast, zero-allocation runtime evaluation.
  - Extensible `EvaluationContext` with local variables and variable resolver ports.
  - Adapter in `croppy-paper` integrating PlaceholderAPI (`%...%` tokens) for player context.

- **Crafting Stations & Custom Blocks (`croppy-api`, `croppy-core`, `croppy-paper`)**:
  - `StationDefinition` and `StationInstance` models registered via `Key` with spatial tracking at `BlockPosition`.
  - Component/Capability model supporting storage (`StorageComponent`), visual displays (`DisplayComponent`), and active processes (`ProcessComponent`).
  - Dual interaction support: physical in-world interaction (right-click with items in hand) and virtual container GUI inspection.
  - Chunk-indexed lifecycle management (placement, breaking, chunk load/unload) and display synchronization.

- **Sequential Process Runtime Engine (`croppy-api` & `croppy-core`)**:
  - Multi-phase finite state machine (`ProcessInstance`) executing sequential stages (`PhaseDefinition`).
  - Diverse phase execution modes: `TIMED` (automatic duration advancement), `MANUAL_INTERACTION` (awaiting player input), and `ENVIRONMENTAL` (awaiting world/block condition thresholds).
  - Robust failure policies (`CANCEL`, `SPOIL`, `BYPRODUCT`) with configurable phase timeouts.
  - Dynamic `OutputResolver` computing final product amounts and quality tiers (Normal, Silver, Gold, Iridium) based on ingredient quality and execution accuracy.

## Capabilities

### New Capabilities
- `condition-engine`: Parsing and evaluating compact string expressions with context variables and PlaceholderAPI integration.
- `crafting-stations`: Registration, spatial placement, dual interaction (in-world and GUI), and lifecycle management of crafting stations.
- `sequential-processes`: Multi-phase state machine runtime executing staged crafting and artisan processing recipes.

### Modified Capabilities
None.

## Impact

- **`croppy-api`**: New ports and records for expressions, evaluation contexts, station definitions/instances, components, and process definitions/instances. Zero Bukkit dependencies.
- **`croppy-core`**: Expression compiler/AST, station manager, process ticker, and recipe registries.
- **`croppy-paper`**: Bukkit event listeners (`PlayerInteractEvent`, `BlockPlaceEvent`, `BlockBreakEvent`), virtual GUI holders, and PlaceholderAPI adapter.
- **Dependencies**: Soft-dependency on PlaceholderAPI in `croppy-paper`. No new mandatory external libraries required.
