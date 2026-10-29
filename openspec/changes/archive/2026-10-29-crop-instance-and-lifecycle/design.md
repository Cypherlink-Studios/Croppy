# Design: Crop Instance Tracking & Lifecycle Management

## Context

See `proposal.md` for motivation. With species definitions registered in memory (`SpeciesRegistry`), we now bridge physical Minecraft blocks with domain-level crop instances.

## Goals / Non-Goals

**Goals:**
- Provide an immutable, decoupled coordinate model (`BlockPosition`) in `croppy-api` free of Bukkit dependencies.
- Model active crop instances (`CropInstance`) with stage index, progress points, and creation timestamps.
- Implement an in-memory `CropManager` in `croppy-core` supporting thread-safe registration, spatial lookup, stage transition, and removal.
- Create custom Paper events (`CropPlantEvent`, `CropHarvestEvent`, `CropBreakEvent`) enabling plugin extensibility and protection integration.
- Implement authoritative event interception in `croppy-paper`:
  - Detect seed planting on farmland and bind `CropInstance`.
  - Cancel vanilla `BlockGrowEvent` for all Croppy-managed crops.
  - Intercept `BlockBreakEvent` to award configured harvest drops upon maturity or refund seeds when immature.
  - Cleanly unregister instances destroyed by water streams, pistons, or farmland trampling.
- Verify instance management mechanics in `croppy-core` with headless JUnit 5 tests.

**Non-Goals:**
- No periodic growth simulation ticker (deferred to `growth-simulation-engine`).
- No database disk persistence / chunk catch-up across restarts (deferred to `crop-persistence-and-chunks`).

## Decisions

### 1. Decoupled Spatial Coordinate (`BlockPosition`)
- **Decision:** Implement `BlockPosition` as an immutable record:
  ```java
  public record BlockPosition(String worldName, int x, int y, int z)
  ```
- **Rationale:** Decouples crop instance tracking from `org.bukkit.Location`. Allows headless testing in `croppy-core` and simple hashing for map keys.
- **Alternatives Considered:** Using Bukkit's `Location`. Rejected because it would violate compiler isolation in `croppy-core`.

### 2. Thread-Safe In-Memory Tracking (`CropManager`)
- **Decision:** Back `CropManager` with `ConcurrentHashMap<BlockPosition, CropInstance>`.
- **Rationale:** Fast O(1) lookups whenever blocks are broken, grown, or interacted with, without lock contention.

### 3. Authoritative Growth Control (Cancelling `BlockGrowEvent`)
- **Decision:** Register a high-priority listener for Bukkit's `BlockGrowEvent`. If the block's position is tracked by `CropManager`, call `event.setCancelled(true)`.
- **Rationale:** Vanilla Minecraft random ticks must not randomly increment block age. Croppy retains 100% authority over crop advancement.

### 4. Custom Paper Lifecycle Events
- **Decision:** Implement `CropPlantEvent`, `CropHarvestEvent`, and `CropBreakEvent` extending `org.bukkit.event.Event` with `Cancellable` in `croppy-paper.event`.
- **Rationale:** Allows server owners, protection plugins (WorldGuard, GriefPrevention), and third-party integrations to hook into Croppy's crop lifecycle.

## Risks / Trade-offs

- **[Server reload/restart resets crops]** → Accepted for this change. Disk persistence (SQLite / PDC) will be introduced in the persistence change.
- **[Orphaned crop instances from physics]** → Handle `BlockPhysicsEvent`, `BlockFromToEvent` (water wash), and farmland trampling to ensure instances are removed when the plant block is popped off.
