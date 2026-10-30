# Design

## Context

Croppy is structured as a multi-module project (`croppy-api`, `croppy-core`, `croppy-paper`) targeting Java 21 and Paper 1.21.8. Currently, `CropInstance` stores position, species key, stage index, progress, and timestamps. Growth is evaluated by `GrowthEngine` using `CropRequirements` against ambient light and nearby water fluid levels scanned by `PaperEnvironmentProvider`. Persistence is handled via `PdcCropStorage` (using `CropBinaryCodec`) and `SqliteCropStorage` (using HikariCP).

See `proposal.md` for the motivation and feature scope of this change.

## Goals / Non-Goals

**Goals:**
- Provide discrete daily soil hydration using an absolute world day index (`lastWateredDay`), enabling $O(1)$ offline catch-up for unloaded chunks without tick loops.
- Support multi-tiered watering cans with configurable water capacity, AoE hydration patterns (1x1, 1x3, 3x3, 5x5), and refill mechanics.
- Auto-hydrate rain-exposed crops in precipitation biomes without overhead obstructions.
- Introduce declarative YAML fertilizer definitions with a single active fertilizer limit per crop, persisting through multi-harvest regrowth.
- Support pre-plant soil fertilization via a 30-second transient coordinate cache without creating orphan persistent records.
- Implement extensible `UnwateredPolicy` mechanics (`PAUSE`, `SLOW`, `DECAY_PROGRESS`, `WITHER`) configurable globally and per species.
- Provide binary codec upgrade to Version 2 and SQLite schema evolution with seamless backward compatibility for existing worlds.

**Non-Goals:**
- Automated pipe/sprinkler network machinery (reserved for a dedicated automation update).
- Persisting unplanted farmland blocks to disk PDC (avoids memory leaks and orphan blocks on piston/trample/explosion events).
- Custom weather engines (uses Paper world storm and biome precipitation properties).

## Decisions

### Decision 1: Absolute World Day Tracking (`lastWateredDay`) vs Continuous Timers
- **Choice**: Store `lastWateredDay` as a `long` calculated as `world.getFullTime() / 24000L`.
- **Rationale**: Real-time interval timers fail when chunks unload or servers restart. If a chunk unloads for 3 days, storing the absolute day allows instantaneous evaluation upon chunk load: `dryDays = currentDay - lastWateredDay`. If `dryDays == 0`, the crop was watered today; if `dryDays > 0`, it is dry.
- **Alternatives Considered**: 
  - *Per-second moisture decay*: Requires constant ticking on all loaded crops and complex catch-up math for unloaded chunks.
  - *Boolean `isWatered` flag*: Requires a server-wide wipe at dawn, failing to update unloaded chunks.

### Decision 2: Attaching State to `CropInstance` with Transient Pre-Plant Buffer
- **Choice**: Store hydration and fertilizer directly on `CropInstance`, accompanied by a short-lived (30-second) in-memory Guava/Caffeine cache for empty tilled soil coordinates.
- **Rationale**: Paper blocks (`Material.FARMLAND`) lack `PersistentDataContainer` support. Persisting empty soil blocks to chunk PDC or SQLite risks orphan records when players trample farmland, pistons push it, or explosions destroy it. Attaching data to `CropInstance` leverages existing atomic lifecycle cleanup. The 30s cache satisfies the player fantasy of "fertilize the soil, then plant the seed".
- **Alternatives Considered**:
  - *Separate `SoilStorage` database table*: High maintenance cost to intercept all world events (trampling, physics, fades, WorldEdit) to prevent data leaks.

### Decision 3: Decoupling Vanilla Water Block Proximity from Active Hydration
- **Choice**: Proximity to static water blocks (or waterlogged blocks) satisfies `CropRequirements.waterFluidLevel()` (the passive habitat requirement), but does NOT automatically mark soil as hydrated for the day. Active daily hydration requires a watering can, rain exposure, or retaining fertilizer rolls.
- **Rationale**: If static water blocks auto-hydrated soil every day, conventional vanilla farm trenches would render watering cans completely obsolete.

### Decision 4: Polymorphic `UnwateredPolicy` Pipeline
- **Choice**: Model `UnwateredPolicy` in `croppy-api` with implementations for `PAUSE`, `SLOW`, `DECAY_PROGRESS`, and `WITHER`.
- **Rationale**: Allows server owners to tune the experience between relaxing cozy gameplay (pause progress) and survival challenge (withering after $N$ dry days).

### Decision 5: Storage Codec Versioning (V2) & Database Migration
- **Choice**: Increment `CropBinaryCodec.VERSION` to 2. Serialize `lastWateredDay` and an optional `fertilizerKey`. When reading Version 1 data, assign `lastWateredDay = -1` and `fertilizerKey = null`. In SQLite, execute `ALTER TABLE croppy_crops ADD COLUMN ...` defensively if columns are absent.
- **Rationale**: Zero data loss or corruption for existing player worlds.

## Risks / Trade-offs

- **[Risk] Bed sleeping and `/time set` skips days abruptly** → *Mitigation*: Register a Paper `TimeSkipEvent` listener and check world day index deltas during growth ticks.
- **[Risk] Biome precipitation vs visual rain** → *Mitigation*: Verify both `world.hasStorm()` and `biome.hasPrecipitation()`. In deserts or badlands where it storms without rainfall, crops are not hydrated.
- **[Risk] Overhead structure / Greenhouse detection cost** → *Mitigation*: Perform `world.getHighestBlockYAt(x, z)` check. If highest block $Y > \text{crop } Y$, check if intervening blocks are transparent glass or opaque roofs.

## Migration Plan

1. **PDC Storage**: `CropBinaryCodec` detects `VERSION == 1` headers and seamlessly reads old structures while outputting `VERSION = 2` on subsequent saves.
2. **SQLite Storage**: `SqliteCropStorage.initialize()` executes `ALTER TABLE croppy_crops ADD COLUMN last_watered_day BIGINT DEFAULT -1` and `ALTER TABLE croppy_crops ADD COLUMN fertilizer VARCHAR(128)` wrapped in try-catch blocks to accommodate existing schemas.
