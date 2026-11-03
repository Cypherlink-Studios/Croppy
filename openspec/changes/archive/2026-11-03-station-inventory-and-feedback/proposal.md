# Proposal

## Why

During physical interaction with crafting stations (such as `croppy:skillet`), right-clicking with an empty hand to inspect the station status displays `"Empty"` on the ActionBar even when ingredients (e.g., 4 `minecraft:egg`) have been inserted into the station's storage. This misleading feedback occurs because the feedback logic only evaluates whether an active `ProcessInstance` is currently running, conflating the absence of a running cooking process with the absence of stored items.

Furthermore, several critical inventory management gaps exist in physical stations:
1. `DefaultStorageComponent` does not stack identical input items, causing each physical right-click to consume a discrete slot and fragment quantities, which causes subsequent multi-item recipe consumption (`removeInput`) to fail.
2. Physical stations lack a mechanism to extract or retrieve inserted input ingredients without breaking the block.
3. Declarative interaction pipelines lack an `EXTRACT_INPUTS` action type and storage-aware context variables.
4. Virtual 3D item displays are not synchronized with inserted input ingredients prior to recipe completion.

## What Changes

- **Accurate Station Inspection Feedback**: Update empty-hand interaction feedback in `StationInteractListener` to inspect `StorageComponent`. When idle with inputs, the ActionBar displays a summary of stored ingredients (e.g., `<name> • 4x Egg • 20.0°C`) instead of declaring `"Empty"`. The status `"Empty"` is displayed only when both input and output storages are truly empty.
- **Port and Core Storage Enhancement**:
  - Add `List<StoredItem> extractInputs()` to `StorageComponent` in `croppy-api` and implement it in `DefaultStorageComponent` in `croppy-core`.
  - Update `DefaultStorageComponent.addInput` to stack/merge with existing entries matching the same `Key` and `quality` up to max stack capacity.
  - Update `DefaultStorageComponent.removeInput` to cumulatively deduct amounts across multiple fragmented slots if necessary.
- **Declarative `EXTRACT_INPUTS` Action**:
  - Support `EXTRACT_INPUTS` in `executeInteractionPipeline` to return input items to the interacting player's inventory.
  - Expose `storage.has_inputs`, `storage.has_outputs`, `storage.is_empty`, and `storage.input_count` in `EvaluationContext`.
- **Configurable Station Interactions for Skillet**:
  - Update `skillet_interactions.yml` with declarative rules allowing both sneak-right-click input extraction and/or menu GUI opening according to storage context.
- **Visual Display Synchronization for Inserted Inputs**:
  - Update `DisplayComponent.setDisplayedItem()` when input items are inserted physically into cookware stations so 3D models render resting in the station immediately.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `crafting-stations`: Update physical station interaction feedback to reflect stored ingredients when idle instead of unconditionally reporting empty, and support input extraction.
- `interaction-engine`: Add support for `EXTRACT_INPUTS` action type and expose storage state variables in evaluation contexts.
- `station-virtual-displays`: Synchronize client-side virtual item displays with inserted ingredients in cookware stations prior to and during process execution.

## Impact

- `croppy-api`: Added `extractInputs()` method to `StorageComponent`. Backward-compatible port addition.
- `croppy-core`: Enhanced `DefaultStorageComponent` with item stacking, cumulative deduction, and input extraction. Expanded `EvaluationContext` builders.
- `croppy-paper`: Refactored `StationInteractListener` ActionBar formatting, added `EXTRACT_INPUTS` action handling, updated `skillet_interactions.yml` and `cooking_pot_interactions.yml`, and updated display synchronization on input insertion.
- Localization: Added localization message tokens for station input summary if applicable (`station.idle-with-inputs`).
