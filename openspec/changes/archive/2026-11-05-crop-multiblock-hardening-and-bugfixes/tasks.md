# Tasks

## 1. Pre-Plant Validation & Collision Hardening

- [x] 1.1 Implement pre-plant vertical clearance checks in `CropPlantListener.java` verifying that all coordinates returned by `species.getSpatialFootprint().calculateOccupiedPositions(origin)` are clear of solid blocks and existing crops before planting.
- [x] 1.2 Add unit tests in `CropPlantListenerTest.java` verifying that planting under an obstruction or into an occupied coordinate cancels the event gracefully with 0 uncaught exceptions.

## 2. Chunk Lifecycle & Startup Persistence Hardening

- [x] 2.1 Update `CroppyPlugin.loadActiveChunkCrops()` and `ChunkLifecycleListener.onChunkLoad()` to resolve species definitions and register full vertical heights (`cropManager.registerCrop(crop, species.getHeight())`).
- [x] 2.2 Trigger immediate `displaySynchronizer.synchronizeDisplay(crop, species, stage)` upon chunk loading and server startup so all restored crops (including mature crops) spawn displays and hitboxes immediately.
- [x] 2.3 Update `ChunkLifecycleListener.onChunkUnload()` to call `displayTracker.removeDisplay(crop.getPosition())` for all evicted crops in the chunk.
- [x] 2.4 Add unit tests in `ChunkLifecycleListenerTest.java` verifying multi-block occupancy retention and immediate display synchronization on chunk load.

## 3. Destruction, Persistence Deletion & Ghost Cleanup

- [x] 3.1 Refactor `CropBreakListener.java`, `VirtualInteractionPacketListener.java`, and `CropPhysicsListener.java` to invoke `storage.deleteCrop(crop.getPosition())` using the canonical root coordinate instead of secondary interaction coordinates.
- [x] 3.2 In `VirtualInteractionPacketListener.java`, clear the physical world base block (`world.getBlockAt(rootPos).setType(Material.AIR)`) upon virtual crop dismantling and monocarpic harvest.
- [x] 3.3 In `CropPhysicsListener.java` and `PaperGrowthTicker.handleWitheredCrop`, inject and invoke `displayTracker.removeDisplay(crop.getPosition())` to destroy virtual displays and hitboxes when crops are destroyed by water, pistons, trampling, or withering.
- [x] 3.4 Add unit tests verifying canonical persistence deletion, physical base block air clearing, and virtual display cleanup upon environmental physics destruction.

## 4. Spatial Occupancy Query Migration

- [x] 4.1 Update `WateringListener.java` to query `cropManager.getCropAt(pos)`, verifying players can hydrate multi-block crops by watering secondary vertical blocks.
- [x] 4.2 Update `FertilizerListener.java`, `CropInspectorService.java`, and `CroppyCommands.java` (`inspect`, `setstage`, `advance`, `remove`) to query `cropManager.getCropAt(pos)`.
- [x] 4.3 Add unit tests in `WateringListenerTest.java`, `FertilizerListenerTest.java`, and `CropInspectorServiceTest.java` verifying interaction and inspection on secondary vertical coordinates (`Y+1`).

## 5. Continuous Player Viewer Proximity Tracking

- [x] 5.1 Implement a periodic player proximity tracking task in `VirtualBlockDisplayTracker.java` evaluating player distance and dispatching spawn/destroy packets to approaching and departing players.
- [x] 5.2 Start and stop the display viewer tracking task in `CroppyPlugin.java` during plugin enable/disable.
- [x] 5.3 Write unit tests in `VirtualBlockDisplayTrackerTest.java` verifying viewer addition and removal as player distances change.

## 6. End-to-End Verification Gate

- [x] 6.1 Run `./gradlew.bat test` across all modules (`croppy-api`, `croppy-core`, `croppy-paper`) and verify that all test suites pass with 0 errors.
