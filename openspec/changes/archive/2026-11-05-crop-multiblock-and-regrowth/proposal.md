# Proposal

## Why

Croppy currently models all crops as single-block (`1x1x1`), monocarpic entities that are destructively destroyed and removed from memory upon harvest. This prevents realistic simulation of climbing, vertical trellis crops (such as grapes, hops, and pole beans), flooded semi-submerged crops with emergent stalks (such as paddy rice), and perennial harvesting where plants yield fruit repeatedly without replanting.

Introducing multi-block spatial footprints, stage-based regrowth cycles, and unified declarative interactions enables rich agricultural mechanics like vineyards and rice paddies as natural configurations of domain capabilities.

## What Changes

- **Multi-Block Spatial Footprints**: Crop species and growth stages can define vertical spatial footprints (`spatial.height >= 1`). `CropManager` tracks multi-block occupancy through a secondary spatial index (`occupancyMap`), preventing block placement or trampling in secondary blocks (e.g., `Y+1`).
- **Semantic Stage Regrowth**: Crops can define a `regrowth` policy within `harvest`, designating a `target_stage` ID (e.g., `flowering_vine`) to which the plant reverts upon harvest. Rather than destroying the crop instance and clearing displays, the engine resets developmental progress and transitions visual displays smoothly back to the target stage.
- **Unified Declarative Crop Interactions**: Generalize the declarative `InteractionEngine` from cooking stations to also support `target_crop` keys. Add `SNEAK_LEFT_CLICK` trigger mode and `HARVEST_CROP` / `DISMANTLE_CROP` actions, enabling tool-sensitive harvesting (e.g. shears bonus) and dismantling (e.g. axe or sneak break) completely via YAML.
- **Bundled Sample Species & Interactions**: Add reference configurations for Trellis Grapes (`croppy:grapes`) and Paddy Rice (`croppy:rice`) with declarative interaction routing.

## Capabilities

### New Capabilities
<!-- No brand new standalone capabilities needed; existing durable capabilities are extended -->

### Modified Capabilities
- `species-definitions`: Add declarative `spatial.height` configuration and `harvest.regrowth` containing `target_stage` ID with fail-fast validation.
- `crop-lifecycle`: Support multi-block occupancy spatial reservation in `CropManager`, vertical collision safeguards, and non-destructive regrowth stage transitions.
- `interaction-engine`: Extend interaction definitions with `target_crop` routing, `SNEAK_LEFT_CLICK` trigger mode, and crop harvest/dismantle execution actions.

## Impact

- **API Changes (`croppy-api`)**:
  - `Species`: Added accessors for spatial footprint (`getHeight()`) and convenience helpers (`getStageById(String)`, `getStageIndex(String)`).
  - `HarvestDefinition`: Extended with `Optional<RegrowthDefinition> regrowth()`, where `RegrowthDefinition` holds `String targetStageId()`.
  - `InteractionDefinition`: Added `Optional<Key> targetCrop()`.
  - `InteractionTrigger`: Added `SNEAK_LEFT_CLICK`.
- **Core Changes (`croppy-core`)**:
  - `CropManager`: Added secondary `occupancyMap` indexing secondary block positions to root positions.
  - `YamlSpeciesLoader`: Added validation for `height >= 1` and semantic verification that `target_stage` corresponds to an existing non-terminal growth stage.
  - `YamlInteractionLoader` & `InteractionRouter`: Added routing support for crop species keys and crop context variables (`crop.stage_id`, `crop.is_mature`, `crop.height`).
- **Paper Changes (`croppy-paper`)**:
  - `CropPlantListener`: Extended `onBlockCollisionSafeguard` to inspect full vertical occupancy.
  - `VirtualInteractionPacketListener`: Delegates player clicks on virtual crop displays to `InteractionRouter` when matching interactions exist, executing `HARVEST_CROP` and `DISMANTLE_CROP`.
  - `VirtualBlockDisplayTracker` & `PacketEventsBlockRenderer`: Adjusts interaction entity height when crops expand vertically across stages.
