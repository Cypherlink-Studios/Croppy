# Proposal: Crop Instance Tracking & Lifecycle Management

## Why

With crop species definitions established, Croppy needs a concrete mechanism to track living crops planted in the Minecraft world. Without instance tracking and event interception, vanilla growth mechanics interfere with custom simulation, and crops cannot maintain persistent stage progress, custom harvest yields, or environmental health.

## What Changes

- Introduce the spatial and instance domain contracts in `croppy-api`:
  - `BlockPosition`: Immutable decoupled coordinate record `(String worldName, int x, int y, int z)`.
  - `CropInstance`: Represents an individual planted crop in the world (species key, position, stage index, current progress, plantedAt, and lastUpdated).
- Introduce custom Paper domain events in `croppy-paper` (or `croppy-api`):
  - `CropPlantEvent`: Fired when a player plants a registered crop seed; cancellable.
  - `CropHarvestEvent`: Fired when a player harvests a mature crop; cancellable.
  - `CropBreakEvent`: Fired when a crop is broken before maturity or destroyed by environmental physics (water, trampling, pistons); cancellable.
- Implement in-memory instance tracking in `croppy-core`:
  - `CropManager`: Service responsible for storing, retrieving, updating, and removing active `CropInstance` objects indexed by `BlockPosition`.
  - Pure Java JUnit 5 tests verifying lifecycle transitions and query mechanics without Bukkit dependencies.
- Implement platform event listeners in `croppy-paper`:
  - `BlockPlaceEvent`: Detects planting of registered species seeds, instantiates a `CropInstance`, and fires `CropPlantEvent`.
  - `BlockGrowEvent`: Cancels vanilla Minecraft growth random ticks for any block tracked by Croppy to preserve full control by our engine.
  - `BlockBreakEvent`: Intercepts block breaking; if mature, drops configured yields and fires `CropHarvestEvent`; if immature, drops seed and fires `CropBreakEvent`; unregisters the instance.
  - Physics/Environment listeners (`BlockPhysicsEvent`, `BlockFromToEvent`, `PlayerInteractEvent` trampling): Cleanly removes instances when the supporting farmland is destroyed or washed away.

## Capabilities

### New Capabilities
- `crop-lifecycle`: Live crop instance tracking at spatial coordinates, binding planted blocks to species definitions, overriding vanilla growth ticks, and dispatching custom plant, harvest, and break lifecycle events.

### Modified Capabilities
<!-- None: Builds upon existing core-registry and species-definitions -->

## Impact

- Adds spatial tracking and live state management to `croppy-api` and `croppy-core`.
- Adds listeners in `croppy-paper` that take authoritative control over registered crop blocks.
- Prepares the foundation for future simulation changes (`growth-simulation-engine`) to iterate over tracked instances and simulate progress.
