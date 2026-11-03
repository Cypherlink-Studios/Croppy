# Tasks

## 1. Storage API & Core Component Enhancements

- [x] 1.1 Add `List<StoredItem> extractInputs()` to `StorageComponent` port in `croppy-api` and verify module compiles.
- [x] 1.2 Implement `extractInputs()` in `DefaultStorageComponent` in `croppy-core` with thread-safe atomic collection and clearing.
- [x] 1.3 Enhance `DefaultStorageComponent.addInput` to merge/stack identical items (matching `itemKey` and `quality`) up to max stack size (64) before occupying new capacity slots.
- [x] 1.4 Enhance `DefaultStorageComponent.removeInput` to cumulatively deduct requested amounts across multiple fragmented slots if split.
- [x] 1.5 Add comprehensive unit tests in `DefaultStorageComponentTest` covering `extractInputs`, slot stacking, multi-slot removal, and capacity limits.

## 2. Interaction Engine & Evaluation Context

- [x] 2.1 Expose storage state context variables (`storage.has_inputs`, `storage.has_outputs`, `storage.is_empty`, `storage.input_count`) to `EvaluationContext` builders in `StationInteractListener` and `PaperStationTicker`.
- [x] 2.2 Implement the `EXTRACT_INPUTS` action type handling in `StationInteractListener.executeInteractionPipeline`, transferring items to player inventory and dropping excess items naturally into the world.
- [x] 2.3 Add unit tests for `EXTRACT_INPUTS` action execution and storage condition routing in interaction listener test suites.

## 3. Feedback, Visual Displays & Station Configuration

- [x] 3.1 Refactor `sendEmptyPhysicalFeedback` in `StationInteractListener` to inspect storage and format stored input summaries when idle with ingredients, reporting `"Empty"` only when both inputs and outputs are absent.
- [x] 3.2 Add localization key `station.idle-with-inputs` in `en-US.yml` and `es-ES.yml` for configurable input summary formatting.
- [x] 3.3 Update `DisplayComponent.setDisplayedItem()` in `StationInteractListener` upon input insertion and clear upon extraction to synchronize 3D item models on cookware plates.
- [x] 3.4 Update `skillet_interactions.yml` with declarative rules enabling sneak-right-click input extraction and menu GUI opening based on storage conditions.

## 4. End-to-End Verification

- [x] 4.1 Execute full test suite via `./gradlew test` and verify zero test failures across `croppy-api`, `croppy-core`, and `croppy-paper`.
- [x] 4.2 Validate OpenSpec change integrity with `openspec validate --change station-inventory-and-feedback --strict`.

## Workflow follow-up

- Review and approve the implementation.
- Run `/openspec-apply-change` to implement the tasks.
- Sync delta specs to durable specifications and archive the change upon completion.
