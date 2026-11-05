# Proposal

## Why

Croppy recently introduced vertical multi-block spatial footprints (`spatial.height >= 1`) and stage-based regrowth cycles, but real-world testing and codebase auditing revealed critical lifecycle desynchronizations:
1. Multi-block crops lose their multi-block occupancy indices upon chunk reload or server restart, permanently reverting to single-block crops in memory.
2. Breaking or dismantling multi-block crops from secondary block positions fails to delete the persisted crop instance from database storage (causing crop "resurrection" on reload).
3. 3D virtual display entities and interaction hitboxes become orphaned ghost entities when crops are destroyed by physics (water flows, pistons, trampling) or withering, while newly loaded chunks or restarts leave mature 3D crops completely invisible because display synchronization never fires.
4. Player interactions (watering cans, fertilizers, inspect tools, and administrative commands) query root positions exclusively, failing whenever a player targets a crop's secondary vertical blocks.

Hardening these multi-block systems now prevents state corruption, visual glitches, and player confusion across all multi-block agriculture features.

## What Changes

- **Pre-Plant Vertical Clearance Validation**: Validate that all blocks spanning `species.getSpatialFootprint().calculateOccupiedPositions(origin)` are clear of physical obstructions (solid blocks) and not occupied by other crops before completing placement, returning player feedback instead of throwing an unhandled exception.
- **Full-Height Restoration on Chunk Load & Startup**: When restoring crops from storage in `CroppyPlugin` and `ChunkLifecycleListener`, resolve the species and register the full vertical height in `CropManager` while immediately invoking `DisplaySynchronizer` to project virtual displays (ensuring mature crops never become invisible).
- **Canonical Root-Based Persistence Deletion**: Ensure all break, dismantle, physics, and interaction handlers delete persistent storage entries using `crop.getPosition()` (the root origin), preventing failed queries against secondary coordinates.
- **Virtual Display Lifecycle & Ghost Cleanup**: Invert lifecycle cleanup so `VirtualBlockDisplayTracker.removeDisplay` is called whenever a crop is destroyed by physics, trampling, or withering, and cleanup viewer states when chunks unload.
- **Physical Base Block Cleansing on Virtual Dismantle/Harvest**: When dismantling or harvesting monocarpic 3D crops via virtual interaction packets, clean up the base physical block in the world (`Material.AIR`) so orphaned physical blocks are not left behind.
- **Spatial Occupancy Resolution in Tools & Commands**: Migrate `WateringListener`, `FertilizerListener`, `CropInspectorService`, and `CroppyCommands` to query `CropManager.getCropAt(pos)` instead of `getCrop(pos)`, enabling seamless interaction across any block height of a multi-block crop.
- **Dynamic Player Viewer Tracking for Virtual Crops**: Equip `VirtualBlockDisplayTracker` with continuous player range tracking so approaching or reconnecting players receive display spawn packets without waiting for stage growth transitions.

## Capabilities

### New Capabilities
<!-- No brand new standalone capabilities needed; existing durable capabilities are extended -->

### Modified Capabilities
- `crop-lifecycle`: Add pre-plant vertical obstruction checks, ensure chunk load and server startup register full multi-block occupancy footprints, enforce canonical root position deletion in persistent storage, and clean base physical blocks on virtual dismantling.
- `virtual-3d-block-displays`: Add immediate visual synchronization on chunk load/restart, display cleanup on physics destruction and withering, chunk unload cleanup, and dynamic player viewer tracking for active crop displays.

## Impact

- **API Changes (`croppy-api`)**: No breaking changes. Existing contracts (`SpatialFootprint`, `CropStorage`, `CropManager`) remain compatible.
- **Core Changes (`croppy-core`)**: Minor helper enhancements to `CropManager` or `SpatialFootprint` if needed for clearance evaluation.
- **Paper Changes (`croppy-paper`)**:
  - `CropPlantListener`: Pre-placement vertical air/passable checks and collision handling.
  - `ChunkLifecycleListener` & `CroppyPlugin`: Full-height registration on reload and immediate visual synchronization.
  - `CropBreakListener`, `VirtualInteractionPacketListener`, `CropPhysicsListener`: Persistence deletion using root position and physical base block cleanup to air.
  - `PaperGrowthTicker`: Display cleanup on withering.
  - `WateringListener`, `FertilizerListener`, `CropInspectorService`, `CroppyCommands`: Use `getCropAt(pos)`.
  - `VirtualBlockDisplayTracker`: Dynamic player viewer tracking.
- **Dependencies & Storage**: Fully backwards-compatible with existing SQLite and PDC databases; no schema migration required.
