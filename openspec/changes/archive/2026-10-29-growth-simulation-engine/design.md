# Design: Motor de Simulación de Crecimiento (Growth Simulation Engine)

## Context

See `proposal.md` for motivation. Croppy currently tracks crop instances in memory (`CropManager`) and suppresses vanilla `BlockGrowEvent`. However, crops remain static because there is no active simulation ticking or progress calculation. This design establishes the growth calculation pipeline, environmental condition verification, world block display synchronization, and task scheduling.

## Goals / Non-Goals

**Goals:**
- Provide a clean hexagonal boundary (`croppy-api`) allowing `croppy-core` to calculate growth, evaluate requirements, and chain modifiers headlessly in JUnit 5 tests.
- Support deterministic base progress with an extensible `GrowthModifier` pipeline for environmental quality, formulas, and future traits.
- Ensure zero chunk-thrashing in Paper: verify chunk loaded status (`world.isChunkLoaded`) before accessing any world block data.
- Scan nearby water within the standard vanilla 9x9 horizontal area and verify fluid levels against the 0–8 requirement.
- Fire cancellable `CropGrowEvent` before advancing stages, keeping stage progression intact and safe if cancelled.
- Synchronize visual age (`Ageable.setAge`) on vanilla crops in Paper.

**Non-Goals:**
- Chunk catch-up growth / offline growth calculations across restarts (deferred to a dedicated chunk persistence milestone).
- Non-vanilla block display entities (e.g. ItemDisplay models or armor stands; reserved for future display types).
- Fertilizer/bonemeal player interaction mechanics (deferred to a dedicated interaction milestone).

## Decisions

### 1. Hexagonal Ports and Adapters for Environment and Visual Sync
- **Decision**: Define `CropEnvironment`, `EnvironmentProvider`, and `DisplaySynchronizer` in `croppy-api` / `croppy-core`.
  - `croppy-core` implements `GrowthEngine` depending only on these interfaces.
  - `croppy-paper` provides `PaperEnvironmentProvider` (using `Block.getLightLevel()` and 9x9 water fluid scanning) and `PaperDisplaySynchronizer` (using `Ageable.setAge(...)`).
- **Rationale**: Keeps `croppy-core` 100% free of Bukkit classes, enabling instant, mock-free unit testing for growth mechanics.
- **Alternatives Considered**: Direct Bukkit block access inside growth algorithms (rejected: violates clean architecture and breaks headless tests).

### 2. Extensible GrowthModifier Pipeline
- **Decision**: Define a functional interface `GrowthModifier`:
  ```java
  @FunctionalInterface
  public interface GrowthModifier {
      double calculateModifier(GrowthContext context);
  }
  ```
  The growth engine calculates:
  `effectiveProgress = baseProgress * product(modifiers)`.
  If base requirements (light, water) are unmet, progress defaults to `0.0` without evaluating complex modifiers.
- **Rationale**: Keeps the core deterministic while giving full flexibility for environmental quality factors, soil formulas, or custom server math.
- **Alternatives Considered**: Hardcoded mathematical formula in a single method (rejected: inflexible and difficult to extend with plugins/features).

### 3. Chunk-Safety & Scheduling in Paper
- **Decision**: `PaperGrowthTicker` is scheduled via Bukkit's scheduler on the main thread at a configurable interval (`growth.tick-interval`, default 20 ticks = 1s).
  Before inspecting any block, it verifies:
  ```java
  int chunkX = pos.x() >> 4;
  int chunkZ = pos.z() >> 4;
  if (!world.isChunkLoaded(chunkX, chunkZ)) {
      continue;
  }
  ```
- **Rationale**: Accessing blocks in unloaded chunks causes synchronous chunk loading, severe memory pressure, and TPS drops.
- **Alternatives Considered**: Asynchronous block reading (rejected: Bukkit world block access is not thread-safe for fluid state reading without risking concurrent modification).

### 4. Water Fluid Level Scanning (9x9 Area)
- **Decision**: For crops requiring water (`waterFluidLevel > 0`), scan horizontally from `x - 4` to `x + 4`, `z - 4` to `z + 4` at farmland level (`y - 1`) and crop level (`y`). Determine the maximum `FluidState` level found (where source water = 8, flowing water = 1..7).
- **Rationale**: Matches vanilla hydration spatial radius and accurately maps to Croppy's `waterFluidLevel` specification.

### 5. Cancellable CropGrowEvent
- **Decision**: Dispatched via `Bukkit.getPluginManager().callEvent(new CropGrowEvent(...))` right when a crop completes its required stage progress.
  - If NOT cancelled: `crop.advanceStage()` is invoked and `DisplaySynchronizer` updates the block in the world.
  - If cancelled: `crop.advanceStage()` is NOT invoked, and progress is capped at `stage.progressRequired()` so it does not overflow or advance silently.
- **Rationale**: Gives external protection plugins or quest hooks full control over growth transitions without corrupting Croppy's internal state.

## Risks / Trade-offs

- **[Performance] Scanning 9x9 blocks per crop on every tick**: If thousands of crops are active, scanning 81 blocks per crop could add up.
  - *Mitigation*: The ticker runs every 20 ticks (1s) rather than every tick; dry crops (`waterFluidLevel == 0`) skip the water scan entirely; crops in unloaded chunks are skipped upfront; the water scan breaks early if a level 8 (source block) is encountered.
- **[Visual Desync] Block data modification conflict**: Another plugin or player breaks the block at the exact moment of growth.
  - *Mitigation*: Verify the block type matches the expected display before calling `setBlockData`.
