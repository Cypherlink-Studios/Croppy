# Design

## Context

Croppy follows a strict Hexagonal Architecture across `croppy-api` (zero-dependency ports), `croppy-core` (pure domain engines and storage models), and `croppy-paper` (Bukkit/Paper adapters, interaction listeners, and PacketEvents virtual displays).

Currently, `StationInteractListener.sendEmptyPhysicalFeedback` reports `"Empty"` whenever `ProcessComponent.hasActiveProcess()` is false, even if `StorageComponent.getInputs()` holds items. Additionally, `DefaultStorageComponent` treats each inserted item as a separate slot without stacking, `removeInput` fails on multi-item requests across fragmented slots, and physical stations offer no mechanism to extract input items.

See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Provide clear, accurate ActionBar feedback that summarizes stored inputs when a station is idle with ingredients, reserving `"Empty"` for completely vacant stations.
- Extend `StorageComponent` with an `extractInputs()` port method to allow atomic retrieval of all stored input items.
- Enable smart ingredient stacking in `DefaultStorageComponent.addInput` and cumulative deduction across split slots in `removeInput`.
- Expose storage occupancy variables (`storage.has_inputs`, `storage.has_outputs`, `storage.is_empty`, `storage.input_count`) to `EvaluationContext`.
- Implement `EXTRACT_INPUTS` as a declarative interaction action in `StationInteractListener`.
- Update `skillet_interactions.yml` to support declarative sneak extraction and menu access.
- Synchronize virtual 3D item displays when items are inserted physically into cookware.

**Non-Goals:**
- Modifying binary persistence codec schemas (`StationBinaryCodec` already serializes `List<StoredItem>` dynamically).
- Altering the core sequential process state machine or culinary discovery logic.
- Adding automatic inventory sorting or filtering beyond item merging.

## Decisions

### Decision 1: Symmetry of Storage Ports (`extractInputs` alongside `extractOutputs`)
- **Choice**: Add `List<StoredItem> extractInputs()` to `StorageComponent` and implement it in `DefaultStorageComponent`.
- **Rationale**: Keeps input retrieval atomic, clean, and symmetrical with `extractOutputs()`.
- **Alternatives considered**:
  - Calling `getInputs()` followed by `clear()`: Not atomic; clears outputs if present.
  - Calling `removeInput()` in a loop: Fragile and prone to `ConcurrentModificationException`.

### Decision 2: In-Slot Stacking in `DefaultStorageComponent.addInput`
- **Choice**: When `addInput(StoredItem item)` is invoked, check if an existing input element has matching `itemKey` and `quality`. If found and the combined quantity does not exceed 64 (or max stack), increment the existing entry's `amount`. Otherwise, if `inputs.size() < inputCapacity`, append a new entry.
- **Rationale**: Physical right-click interaction deposits 1 item per click. Without stacking, a player inserting 4 eggs consumes all 4 discrete slots of a skillet (`input_capacity: 4`), making multi-ingredient cooking impossible.
- **Alternatives considered**:
  - Rigid single-item-per-slot: Severely constrains physical crafting stations.

### Decision 3: Cumulative Multi-Slot Deduction in `removeInput`
- **Choice**: If a recipe or system requires removing $N$ items of a given key, iterate through `inputs` deducting from matching slots until the total required quantity is fulfilled.
- **Rationale**: If items ever become split across multiple slots, `removeInput` must reliably satisfy recipe requirements rather than failing when no single slot contains $\ge N$ items.

### Decision 4: Declarative `EXTRACT_INPUTS` Action & Context Variables
- **Choice**: Implement `EXTRACT_INPUTS` within `executeInteractionPipeline` and populate `storage.*` variables in `EvaluationContext`.
- **Rationale**: Preserves the data-driven architecture of `interaction-engine`. Any station YAML can bind input extraction to sneak-clicks, tool clicks, or conditional states.
- **Alternatives considered**:
  - Hardcoding sneak extraction in `StationInteractListener`: Bypasses configuration and breaks modularity.

### Decision 5: ActionBar Feedback State Hierarchy
- **Choice**: Determine the ActionBar status text in `sendEmptyPhysicalFeedback` according to this precedence:
  1. Active Process: `<yellow>{STATE}</yellow>` (e.g. `RUNNING`, `AWAITING_INPUT`).
  2. Completed Outputs Present: `<green>Output Ready</green>`.
  3. Stored Inputs Present: Format ingredient summary, e.g. `<white>{count}x {ItemName}</white>` (or `<white>{total} Items</white>`).
  4. Fully Empty: `<gray>Empty</gray>` (`station.empty`).
- **Rationale**: Provides immediate, truthful feedback to the player under all possible station states.

## Risks / Trade-offs

- **[Risk] Player inventory full during `EXTRACT_INPUTS`**:
  - *Mitigation*: Any items that cannot fit into the player's inventory are spawned into the world at the station's location via `world.dropItemNaturally()`.
- **[Risk] Display item selection when multiple distinct ingredients exist in inputs**:
  - *Mitigation*: Set `displayComp.setDisplayedItem()` to the most recently inserted item or the first item in `storage.getInputs()`. When inputs are cleared, clear `displayedItem` so the virtual entity disappears.
