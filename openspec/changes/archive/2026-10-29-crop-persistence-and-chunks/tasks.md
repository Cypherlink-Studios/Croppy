# Tasks: Persistencia y Ciclo de Chunks (Crop Persistence and Chunks)

## 1. Storage API & Dependencies Setup

- [x] 1.1 Add AlessioDP repository, Libby Paper dependency, and compileOnly HikariCP 7.1.0 and SQLite JDBC to `croppy-paper/build.gradle.kts`; verify with `./gradlew :croppy-paper:dependencies`.
- [x] 1.2 Model `CropStorage` service interface in `croppy-api` and verify compilation with `./gradlew :croppy-api:classes`.
- [x] 1.3 Integrate `CropStorage` into `CroppyCore` service registry and verify compilation with `./gradlew :croppy-core:classes`.

## 2. Chunk PDC Storage Implementation & Unit Tests

- [x] 2.1 Implement `PdcCropStorage` in `croppy-paper` using binary stream encoding (`DataOutputStream`/`DataInputStream`) over `PersistentDataType.BYTE_ARRAY`.
- [x] 2.2 Create unit tests verifying crop binary serialization/deserialization and verify with `./gradlew test`.

## 3. SQLite Storage with HikariCP & Libby Runtime Loading

- [x] 3.1 Implement `LibbyDependencyLoader` in `croppy-paper` to dynamically load HikariCP 7.1.0 and SQLite JDBC at runtime.
- [x] 3.2 Implement `SqliteCropStorage` in `croppy-paper` with HikariCP connection pooling, indexed schema creation, and chunk batch persistence.
- [x] 3.3 Implement `CropStorageFactory` in `croppy-paper` instantiating PDC or SQLite storage based on `config.yml`.

## 4. Chunk Lifecycle Coordination & Server Integration

- [x] 4.1 Implement `ChunkLifecycleListener` in `croppy-paper` loading crops on `ChunkLoadEvent` and persisting/evicting on `ChunkUnloadEvent`.
- [x] 4.2 Wire crop lifecycle listeners (`CropPlantListener`, `CropBreakListener`) to save or delete crops in active `CropStorage`.
- [x] 4.3 Update `CroppyPlugin.onEnable()` and `onDisable()` to initialize storage, load active chunks on startup, and flush all crops on shutdown.
- [x] 4.4 Update `croppy-paper/src/main/resources/config.yml` with `storage.type` options and SQLite pool configuration.

## 5. Full Verification

- [x] 5.1 Execute full build and unit tests with `./gradlew test` and `./gradlew build`, verifying all tests pass and shadow JAR builds cleanly.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and commit to git branch main.
