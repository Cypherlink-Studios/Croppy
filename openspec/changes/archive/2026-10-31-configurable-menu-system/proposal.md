# Proposal: Configurable Menu System

## Why

Currently, station GUI menus in Croppy are hardcoded in `StationGuiListener` to a static 27-slot layout with fixed single input, status, and output slots (slots 11, 13, and 15). This prevents multi-input/output stations (like the Cooking Pot with 6 inputs and 2 outputs) from displaying or managing their full inventory, blocks customization of menu borders, titles, progress indicators, or action buttons, and cannot be reused for other systems (such as recipe books or crop inspectors). 

A decoupled, extensible, and YAML-driven menu engine is needed to allow server administrators and developers to declare custom GUI layouts via pattern matrices, integrate dynamic conditions and expressions, support multi-slot capacities, and eliminate inventory duplication exploits.

## What Changes

- **Pure API Menu Modeling (`croppy-api`)**:
  - Introduce `MenuDefinition`, `SlotDefinition`, `SlotType` (`INPUT`, `OUTPUT`, `STATUS`, `ACTION`, `DECORATION`), `MenuItemTemplate`, `ConditionalTemplate`, `MenuAction`, and `MenuRegistry`.
  - Add explicit `Key menuKey` property to `StationDefinition` (with default fallback to `croppy:default_station`).
- **Core YAML Parsing & Validation (`croppy-core`)**:
  - Implement `DefaultMenuRegistry` for thread-safe runtime lookup of menu definitions.
  - Implement `YamlMenuLoader` using SnakeYAML to load and validate 9xN pattern matrices, character-to-element mappings, and conditional template branches.
  - Integrate AST expression evaluation with `EvaluationContext` to determine dynamic slot icons and action condition gates.
- **Paper Platform Menu Adapter (`croppy-paper`)**:
  - Implement `PaperMenuHolder` and `PaperMenuRenderer` to construct Adventure MiniMessage inventories, resolve custom model data and item meta, and bind storage inputs/outputs.
  - Implement `PaperMenuListener` with strict anti-duplication security: safe shift-click routing to `INPUT` slots only, cancellation on `DECORATION`/`STATUS`/`ACTION`, pickup-only rules on `OUTPUT`, and hotbar swap / double-click protections.
  - Implement `PaperMenuTicker` for lightweight live refreshes of `STATUS` and progress indicators on open inventories without full inventory rebuilds.
  - Ship default YAML menu configurations in `menus/default_station.yml`, `menus/stations/cooking_pot.yml`, and `menus/stations/skillet.yml`.

## Capabilities

### New Capabilities
- `menu-system`: Configurable, extensible inventory GUI engine for Croppy. Defines pattern matrices, slot types, action triggers, conditions, and template rendering.

### Modified Capabilities
- `crafting-stations`: Update station registration and dual interaction modes to support explicit `menuKey` property on `StationDefinition` and bind virtual container interactions to the dynamic menu system.

## Impact

- **APIs**:
  - `croppy-api`: New menu package `dev.darkblade.croppy.api.menu`. `StationDefinition` adds `menuKey()` parameter.
- **Dependencies**:
  - SnakeYAML 2.2 in `croppy-core` (already present).
  - Kyori Adventure & Paper API in `croppy-paper` (already present).
- **Configuration & Storage**:
  - New configuration directory `menus/` deployed into the plugin data folder.
  - Backward compatible: stations without an explicit `menu` field fall back to `croppy:default_station`.
