# Design

## Context

See `proposal.md` for motivation. While `CropManager` in `croppy-core` supports secondary spatial indexing (`occupancyMap`), the `croppy-paper` platform adapter currently fails to restore multi-block heights on chunk load, queries secondary block coordinates during persistence deletion, leaks physical blocks on virtual dismantling, and misses player viewer tracking for virtual 3D crop displays.

This technical design details the architectural corrections and hardening required across `croppy-api`, `croppy-core`, and `croppy-paper`.

## Goals / Non-Goals

**Goals:**
- Guarantee that multi-block crops retain their full vertical footprint across chunk unloads and server restarts.
- Prevent ghost entity leaks (ItemDisplays and Interaction hitboxes) when crops are broken by players, physics, trampling, or withering.
- Ensure all persistent storage operations (save/delete) resolve to the canonical root coordinate `(x, y, z)`.
- Ensure tool interactions (watering, fertilizing, inspecting, admin commands) seamlessly work on any vertical block occupied by a multi-block crop.
- Provide continuous viewer tracking so approaching or reconnecting players see existing 3D crop models and hitboxes immediately.

**Non-Goals:**
- Placing physical barrier or collision blocks in the secondary spaces (`Y+1`): secondary spaces remain `Material.AIR` with collision handled by the virtual `Interaction` entity hitbox.
- Dynamic footprint expansion per stage: crops reserve their full vertical height (`species.getHeight()`) starting at developmental stage 0.
- Horizontal multi-block footprints (e.g. 2x2 or 3x3 trees/pumpkins) remain out of scope for this hardening pass.

## Decisions

### Decision 1: Pre-Plant Vertical Space Validation & Friendly Feedback
- **Choice**: In `CropPlantListener.onBlockPlace`, before registering the crop, inspect all positions returned by `species.getSpatialFootprint().calculateOccupiedPositions(origin)`:
  - Verify every secondary position (`y + 1 ... y + height - 1`) is either air or passable/replaceable (e.g. tall grass, light).
  - Verify none of the occupied positions are already tracked in `cropManager.isOccupied(pos)`.
  - If obstructed, cancel `BlockPlaceEvent` and send action bar feedback (`"Not enough vertical space for this crop"`).
- **Rationale**: Prevents planting multi-block crops inside solid ceilings, eliminates unhandled `IllegalStateException` crashes in event listeners, and prevents visual model clipping.
- **Alternatives Considered**: Automatically breaking or replacing overhead blocks. Rejected as disruptive and potentially exploitable for griefing.

### Decision 2: Canonical Root Coordinate Deletion
- **Choice**: In `CropBreakListener`, `VirtualInteractionPacketListener`, and `CropPhysicsListener`, resolve the target crop via `cropManager.getCropAt(pos)` and invoke `storage.deleteCrop(crop.getPosition())`.
- **Rationale**: Persisted crop rows in SQLite and PDC keys are stored exclusively at the root coordinate `(x, y, z)`. Calling `deleteCrop` with a secondary position (`y + 1`) fails silently, leaving orphaned database rows that resurrect destroyed crops upon restart.
- **Alternatives Considered**: Storing redundant duplicate database rows for secondary blocks. Rejected because it complicates transactions and wastes storage.

### Decision 3: Immediate Visual Synchronization & Full-Height Reload
- **Choice**: In `CroppyPlugin.loadActiveChunkCrops()` and `ChunkLifecycleListener.onChunkLoad()`:
  - Query `speciesRegistry.get(crop.getSpeciesKey())`.
  - Register with `cropManager.registerCrop(crop, species.getHeight())`.
  - Immediately call `displaySynchronizer.synchronizeDisplay(crop, species, species.getStage(crop.getStageIndex()))`.
- **Rationale**: Prevents multi-block crops from silently downgrading to 1x1x1 on reload, and prevents mature crops (which are skipped by `PaperGrowthTicker`) from becoming permanently invisible.
- **Alternatives Considered**: Relying on the growth ticker to synchronize displays. Rejected because mature crops never tick growth and ticker intervals (1200 ticks / 1 min) cause unacceptable visual pop-in delays.

### Decision 4: Virtual Dismantle Physical Cleanliness
- **Choice**: When `VirtualInteractionPacketListener.performDismantle` or monocarpic harvest executes:
  - Fetch the world block at `crop.getPosition()` and execute `block.setType(Material.AIR)`.
  - Despawn virtual display entities via `displayTracker.removeDisplay(crop.getPosition())`.
- **Rationale**: In packet-based interactions, vanilla Minecraft does not trigger block destruction for client-side entities. Without explicit cleanup, the seed/base block placed by the player remains in the world as an invisible obstacle.

### Decision 5: Periodic Player Proximity Tracking in `VirtualBlockDisplayTracker`
- **Choice**: Introduce a lightweight recurring task (e.g. running every 20 ticks / 1 second) in `VirtualBlockDisplayTracker` that evaluates nearby online players against active displays, dispatching spawn packets for players entering the 48-block radius and destroy packets for players leaving.
- **Rationale**: Matches the proven architecture of `StationViewerTracker`, ensuring players who walk toward a farm see the 3D crops and their hitboxes immediately without requiring crops to advance a growth stage.
- **Alternatives Considered**: Listening to `PlayerMoveEvent`. Rejected due to heavy main-thread performance overhead on busy servers.

### Decision 6: Global Migration to `CropManager.getCropAt(pos)`
- **Choice**: Replace all calls to `cropManager.getCrop(pos)` with `cropManager.getCropAt(pos)` in `WateringListener`, `FertilizerListener`, `CropInspectorService`, and `CroppyCommands`.
- **Rationale**: Unifies the API contract so that interacting with any vertical block of a multi-block crop routes to the underlying instance.

## Risks / Trade-offs

- **[Periodic Viewer Ticker Overhead]** → Iterating active displays across players every second could increase CPU usage if tens of thousands of crops are active simultaneously.
  *Mitigation*: Skip chunk-unloaded displays, iterate only over loaded players in the target world, and calculate squared horizontal distance (`distSq <= 48 * 48`) with early bailouts.
- **[Player Clipping in Y+1 Space]** → Because secondary blocks are `AIR`, players can walk into the top half of a tall crop.
  *Mitigation*: Confirmed as desired behavior (transparent foliage / trellis lattice walkthrough); physical blocking would obstruct movement in vineyards. Interaction raycasts are intercepted by the virtual `Interaction` entity hitbox.

## Migration Plan

- 100% backward-compatible: Existing databases and configuration files require zero modifications.
- Tests: Add comprehensive unit tests in `croppy-paper` validating pre-plant vertical collision cancellation, full-height chunk reload, root-based persistence deletion, and `getCropAt` resolution.
