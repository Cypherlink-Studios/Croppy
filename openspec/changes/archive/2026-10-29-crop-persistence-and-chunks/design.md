# Design: Persistencia y Ciclo de Chunks (Crop Persistence and Chunks)

## Context

See `proposal.md` for motivation. Currently, `CropManager` stores all crop instances in an in-memory `ConcurrentHashMap`. When the server stops or chunks are unloaded by Minecraft, data is lost. This design specifies the hybrid storage engine, the dynamic library management via Libby, and the chunk lifecycle coordination.

## Goals / Non-Goals

**Goals:**
- Provide a clean storage port (`CropStorage`) in `croppy-api` decoupled from Minecraft.
- Implement `PdcCropStorage` using Chunk `PersistentDataContainer` with compact binary encoding (`PersistentDataType.BYTE_ARRAY`).
- Implement `SqliteCropStorage` using HikariCP 7.1.0 connection pooling and SQLite JDBC.
- Use Libby Paper (`net.byteflux:libby-paper:1.3.2`) to dynamically download HikariCP and SQLite JDBC only when SQLite storage is selected, keeping the shaded jar under 150KB.
- Coordinate chunk lifecycle: load crops into `CropManager` on `ChunkLoadEvent`, flush and evict crops on `ChunkUnloadEvent`, maintaining $O(\text{active})$ RAM footprint.
- Flush all active crops safely on server shutdown (`onDisable`).

**Non-Goals:**
- Catch-up growth / offline growth calculations (explicitly deferred per user direction).
- Cross-server synchronization or external MySQL/PostgreSQL support (reserved for future milestones).

## Decisions

### 1. Hexagonal Storage Port (`CropStorage`)
- **Decision**: Define `CropStorage` in `croppy-api/src/main/java/dev/darkblade/croppy/api/storage/CropStorage.java` implementing `Service`.
  - Methods: `loadChunk(world, chunkX, chunkZ)`, `saveChunk(world, chunkX, chunkZ, crops)`, `saveCrop(crop)`, `deleteCrop(position)`.
- **Rationale**: Keeps domain architecture clean and allows `croppy-core` to test persistence workflows headlessly if needed.

### 2. Dynamic Library Management with Libby Paper
- **Decision**: Add `implementation("net.byteflux:libby-paper:1.3.2")` to `croppy-paper`. Configure HikariCP 7.1.0 and SQLite JDBC as `compileOnly` in Gradle, while loading them at runtime via `PaperLibraryManager` only when `storage.type == "sqlite"`.
- **Rationale**: Shading HikariCP and sqlite-jdbc balloons the plugin jar from ~100KB to over 20MB. Libby downloads drivers on first launch into `plugins/Croppy/libraries/`, saving bandwidth and disk space when PDC is used.
- **Alternatives Considered**: Direct Shadow jar shading (rejected: creates bloated jar).

### 3. Binary Encoding for Chunk PDC
- **Decision**: Use `DataOutputStream` / `DataInputStream` over `PersistentDataType.BYTE_ARRAY`. Each crop is stored as:
  - `String speciesKey`, `int x`, `int y`, `int z`, `int stageIndex`, `double progress`, `long plantedAt`, `long lastUpdated`.
  - Stored under NamespacedKey `croppy:crops`. If a chunk has 0 crops, the key is removed.
- **Rationale**: Takes ~40 bytes per crop vs 400+ bytes for verbose NBT tag containers or JSON, ensuring zero lag or file bloat in `.mca` Anvil chunk files.

### 4. SQLite Schema with Indexed Chunk Coordinates
- **Decision**:
  ```sql
  CREATE TABLE IF NOT EXISTS croppy_crops (
      world VARCHAR(64) NOT NULL,
      x INT NOT NULL,
      y INT NOT NULL,
      z INT NOT NULL,
      chunk_x INT NOT NULL,
      chunk_z INT NOT NULL,
      species VARCHAR(128) NOT NULL,
      stage_index INT NOT NULL,
      progress DOUBLE NOT NULL,
      planted_at BIGINT NOT NULL,
      last_updated BIGINT NOT NULL,
      PRIMARY KEY (world, x, y, z)
  );
  CREATE INDEX IF NOT EXISTS idx_croppy_chunk ON croppy_crops (world, chunk_x, chunk_z);
  ```
- **Rationale**: Chunk-based querying (`SELECT * WHERE world=? AND chunk_x=? AND chunk_z=?`) runs in sub-millisecond time.

### 5. Chunk Lifecycle Coordination and RAM Boundedness
- **Decision**: `ChunkLifecycleListener` hooks:
  - `ChunkLoadEvent`: queries `storage.loadChunk(...)` and registers crops in `CropManager`.
  - `ChunkUnloadEvent`: queries crops in chunk, calls `storage.saveChunk(...)`, then evicts them from `CropManager`.
- **Rationale**: Guarantees that inactive crops never stay in memory, keeping RAM usage strictly bounded to actively loaded chunks.

## Risks / Trade-offs

- **[First-run SQLite Network Access]**: If SQLite is chosen, Libby needs internet access to download drivers from Maven Central.
  - *Mitigation*: Fail cleanly with an informative message if offline, or fall back to PDC.
- **[Server Crash without Clean Unload]**: Sudden power loss or kill -9.
  - *Mitigation*: For PDC, Minecraft saves chunk PDC with chunk auto-saves. For SQLite, `saveCrop` updates on key lifecycle events (planting, stage advance).
