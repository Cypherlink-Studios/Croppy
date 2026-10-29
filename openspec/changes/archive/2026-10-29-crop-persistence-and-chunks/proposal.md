# Proposal: Persistencia y Ciclo de Chunks (Crop Persistence and Chunks)

## Why

Currently, crop instances and their developmental progress exist solely in volatile memory (`CropManager`). When the server restarts or when chunks unload and reload, all crop data is lost, leaving blocks orphaned in the world. To make Croppy production-ready for survival servers, the plugin requires a persistent storage engine and chunk lifecycle coordination that saves and loads crops atomically while keeping RAM usage bounded strictly to active chunks.

## What Changes

- **Core Storage Abstractions (`croppy-api`)**:
  - Introduce `CropStorage` service interface defining contracts for chunk-level loading, saving, and individual crop persistence.
- **Hybrid Storage Backends (`croppy-paper`)**:
  - Implement `PdcCropStorage` storing serialized crops directly within Minecraft's native Chunk `PersistentDataContainer` using compact binary encoding (`BYTE_ARRAY`).
  - Implement `SqliteCropStorage` using HikariCP 7.1.0 connection pooling and SQLite JDBC (`croppy_crops` table with indexed chunk coordinates).
  - Integrate Libby Paper (`net.byteflux:libby-paper:1.3.2`) to download database drivers dynamically at runtime only when SQLite storage is selected, keeping the shaded jar lightweight (~100KB instead of 20MB+).
  - Add configurable storage selection in `config.yml` (`storage.type: "pdc"` or `"sqlite"`).
- **Chunk Lifecycle Management (`croppy-paper`)**:
  - Implement `ChunkLifecycleListener` responding to `ChunkLoadEvent` (deserializing and registering crops into `CropManager`) and `ChunkUnloadEvent` (persisting state and removing from memory to free RAM).
  - Ensure server shutdown (`onDisable`) safely flushes all active crops to the configured storage backend.

## Capabilities

### New Capabilities
- `crop-persistence`: Provides persistent storage for crop instances across restarts and chunk unloads, supporting hybrid backends (Chunk PDC and SQLite via HikariCP 7.1.0 with Libby runtime dependency management).

### Modified Capabilities
- `crop-lifecycle`: Bind in-memory crop tracking to chunk lifecycle events, loading active crops upon chunk load and persisting/evicting crops upon chunk unload.

## Impact

- **croppy-api**: Adds `CropStorage` interface in `dev.darkblade.croppy.api.storage`.
- **croppy-core**: Integrates optional `CropStorage` into `CroppyCore`.
- **croppy-paper**: Adds `PdcCropStorage`, `SqliteCropStorage`, `ChunkLifecycleListener`, Libby dependency manager, and `config.yml` storage options.
- **Dependencies**: Adds `net.byteflux:libby-paper:1.3.2` to `croppy-paper`, with HikariCP 7.1.0 and sqlite-jdbc loaded via Libby at runtime.
