# Proposal: Motor de Simulación de Crecimiento (Growth Simulation Engine)

## Why

With crop instances spatially tracked and vanilla random ticks suppressed, Croppy crops currently remain static in the world. To make agriculture functional, the plugin requires an authoritative, deterministic growth simulation engine that periodically evaluates crop instances, verifies environmental conditions (light level and nearby water fluid level), accumulates developmental progress, and synchronizes the physical block representation when developmental stages advance.

## What Changes

- **Core Growth Abstractions (`croppy-api`)**:
  - Introduce `CropEnvironment` interface/record representing ambient physical conditions (`lightLevel`, `waterFluidLevel`, `isLoaded`).
  - Introduce `GrowthContext` encapsulating crop instance, species definition, evaluated environment, and base progress.
  - Introduce `GrowthModifier` functional interface enabling extensible multiplier pipelines for environmental quality or mathematical formulas.
- **Engine Simulation Core (`croppy-core`)**:
  - Introduce `GrowthEngine` service orchestrating crop evaluation cycles, condition enforcement, modifier calculations, and stage advancement.
  - Provide a deterministic progress formula with modifier chaining, fully testable in headless JUnit 5 unit tests.
- **Paper Platform Ticker & World Synchronization (`croppy-paper`)**:
  - Add `PaperGrowthTicker` running as a scheduled Bukkit task at a configurable interval (default: 20 ticks / 1 second) in `config.yml`.
  - Add chunk safety checks (`world.isChunkLoaded(chunkX, chunkZ)`) preventing chunk thrashing or unintended loading.
  - Add `PaperEnvironmentProvider` scanning local block light and a 9x9 Chebyshev horizontal area for the highest water `FluidState` level (0 to 8).
  - Add cancellable `CropGrowEvent` fired when a crop reaches required stage progress before advancing.
  - Add `DisplaySynchronizer` updating the physical world block state (`Ageable.setAge(...)` for vanilla displays).

## Capabilities

### New Capabilities
- `growth-simulation`: Handles periodic simulation ticking, chunk-safe environmental condition evaluation (light level and fluid water levels), deterministic progress accumulation via modifier pipelines, and physical block display synchronization.

### Modified Capabilities
- `crop-lifecycle`: Extend domain lifecycle events to include cancellable `CropGrowEvent` fired when a crop accumulates sufficient progress to advance developmental stages.

## Impact

- **croppy-api**: Adds `CropEnvironment`, `GrowthContext`, `GrowthModifier`.
- **croppy-core**: Adds `GrowthEngine` service and pure Java test fixtures.
- **croppy-paper**: Adds `PaperGrowthTicker`, `PaperEnvironmentProvider`, `DisplaySynchronizer`, `CropGrowEvent`, and `config.yml` growth settings.
- **Dependencies**: No external dependencies added (uses Paper API on platform module, standard Java 21 on core/api).
