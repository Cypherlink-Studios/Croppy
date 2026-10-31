# Design: Configurable Menu System

## Context

See `proposal.md` for problem motivation and capability scope.

Croppy strictly adheres to Hexagonal Architecture:
- `croppy-api`: Pure Java 21 ports, records, and domain identifiers with zero external dependencies.
- `croppy-core`: Pure Java 21 domain simulation, codecs, SnakeYAML parsing, and AST condition evaluation with zero platform/Bukkit types.
- `croppy-paper`: Paper 1.21.8 platform adapter handling Bukkit inventories, Adventure MiniMessage, player events, and scheduler tasks on the server main thread.

Currently, `StationGuiListener` hardcodes slot indices 11, 13, and 15 in a 27-slot chest inventory, dropping any station inputs or outputs beyond index 0. This change introduces a generic, decoupled Menu Engine driven by YAML matrix patterns and backed by anti-dupe inventory protections.

## Goals / Non-Goals

**Goals:**
- Provide pure Java 21 records and contracts in `croppy-api` for immutable menu definitions, slot definitions, template models, and action specifications.
- Implement SnakeYAML 9xN pattern matrix parsing, validation, and registry management in `croppy-core`.
- Support prioritized dynamic conditional templates in status slots evaluated against runtime `EvaluationContext`.
- Enforce strict anti-duplication inventory protection in `croppy-paper` across all Bukkit click types, shift-clicks, drags, and hotbar swaps.
- Implement a lightweight ticker in `croppy-paper` that dynamically refreshes status icons and progress lore on open menus without cursor resets or inventory recreation.
- Maintain backward compatibility for existing stations via a default station menu fallback (`croppy:default_station`).

**Non-Goals:**
- Custom client-side protocol packets, resource pack GUI textures, or Bedrock forms (all GUIs use standard Paper `Inventory` containers).
- In-game visual editor commands for modifying menus (menus are declared and edited via YAML configuration files).

## Decisions

### 1. ASCII Matrix Pattern Syntax over Explicit Slot Indices
- **Decision**: Define inventory layouts using a list of 9-character ASCII strings representing rows, paired with an `elements` dictionary mapping characters to slot definitions.
- **Rationale**: An ASCII matrix (`"X X X X X X X X X"`, `"X I I I S O O X X"`) is visually intuitive, directly mirrors the chest layout, and eliminates coordinate math errors.
- **Alternatives Considered**:
  - *Explicit numeric index lists (`input_slots: [10, 11, 12]`)*: Fragile, tedious to visualize, and prone to overlapping slot collisions.

### 2. Decoupled Generic Menu Engine with Station Context Binding
- **Decision**: Create a generic `MenuDefinition` and `MenuRegistry` in `croppy-api`/`croppy-core`. Stations associate with a menu via an explicit `menuKey` property on `StationDefinition`. In `croppy-paper`, `PaperMenuService.openStationMenu` binds the station's `StorageComponent` and `ProcessComponent` to the menu's `INPUT`, `OUTPUT`, and `STATUS` slots.
- **Rationale**: Keeps the menu engine extensible for future systems (e.g., recipe books, seed encyclopedias, soil analyzers) while providing first-class integration for crafting stations.
- **Alternatives Considered**:
  - *Hardcoding menu models specifically to stations*: Prevents code reuse across other plugin GUI features.

### 3. Pratt AST Condition Engine Integration for Dynamic Slot States
- **Decision**: Leverage Croppy's existing `ExpressionCompiler` and `EvaluationContext` to evaluate conditional templates for `STATUS` slots and gate button actions.
- **Rationale**: Reuses existing expressions (e.g. `process.state == 'RUNNING'`, `station.heat_level >= 100`, `%player_level% >= 5`) without creating a redundant expression syntax.

### 4. Anti-Duplication Security Architecture
- **Decision**: Enforce comprehensive event filtering in `PaperMenuListener`:
  - `DECORATION`, `STATUS`, `ACTION`: Cancel all click, drag, and swap events.
  - `OUTPUT`: Allow extraction (pickup) only; reject deposit, drag-place, or swap.
  - `INPUT`: Allow normal interaction; on change, atomically synchronize with `StorageComponent`.
  - Shift-click: Intercept player inventory shift-clicks and route items sequentially into empty `INPUT` slots; if inputs are full or non-existent, cancel the event immediately to prevent Bukkit from overflowing items into decorative or output slots.
  - Hotbar number keys (1-9) and offhand swap ('F'): Cancel when hovering over non-input slots.
  - Drag events: Cancel entirely if any dragged raw slot is not an `INPUT` slot.
- **Rationale**: Closes all standard Minecraft inventory duplication vectors.

### 5. Lightweight Periodic Menu Ticking
- **Decision**: Implement `PaperMenuTicker` scheduled every 10 ticks (0.5s). It scans active viewers of `PaperMenuHolder` instances that have active processes, re-evaluates the `STATUS` slot conditional template, and updates the `ItemStack` in-place.
- **Rationale**: Provides smooth visual progress updates while avoiding full inventory rebuilds that desynchronize cursor items.

## Risks / Trade-offs

- **[Risk] Station input/output capacity exceeds menu layout slots**  
  → *Mitigation*: `YamlMenuLoader` and `PaperMenuRenderer` log a warning if a station's `inputCapacity` or `outputCapacity` exceeds the available slots in its bound menu.
- **[Risk] High player count with open GUIs causing ticker lag**  
  → *Mitigation*: The ticker only inspects players with an open `PaperMenuHolder` where `station.hasActiveProcess()` is true, touching only the status slot `ItemStack`.
- **[Risk] Rapid clicking on action buttons triggering race conditions**  
  → *Mitigation*: Click events run synchronously on the Paper main thread, and `ProcessEngine.tryStartProcess` validates prerequisites atomically.

## Migration Plan

1. Deploy new default configurations to `croppy-paper/src/main/resources/menus/`:
   - `menus/default_station.yml`
   - `menus/stations/cooking_pot.yml`
   - `menus/stations/skillet.yml`
2. Update `cooking_pot.yml` and `skillet.yml` under `stations/` with `menu: "croppy:cooking_pot"` and `menu: "croppy:skillet"`.
3. If an existing station configuration omits `menu`, it defaults to `croppy:default_station`.
