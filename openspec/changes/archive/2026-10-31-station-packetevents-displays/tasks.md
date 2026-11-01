# Tasks

## 1. Pure API Contracts (`croppy-api`)

- [x] 1.1 Create immutable visual records in `dev.darkblade.croppy.api.station.visual` (`StationVisualDefinition`, `DisplayLayerConfig`, `TextDisplayTemplate`, `DisplayTransform`, `BillboardMode`) and verify `./gradlew :croppy-api:compileJava` succeeds with zero external dependencies
- [x] 1.2 Extend `StationDefinition` to support `StationVisualDefinition` with backward-compatible constructors and verify existing tests in `croppy-api` compile and pass
- [x] 1.3 Extend `DisplayComponent` to expose `float getProgress()`, `void setProgress(float progress)`, `String getThermalZone()`, and `void setThermalZone(String zone)` and verify interface contracts

## 2. Core Domain Modeling & YAML Parsing (`croppy-core`)

- [x] 2.1 Update `DefaultDisplayComponent` to implement progress and thermal zone tracking with dirty flags and verify unit tests in `croppy-core`
- [x] 2.2 Update `YamlStationLoader` to parse the `visual:` section into `StationVisualDefinition` and verify parsing in `YamlStationLoaderTest`
- [x] 2.3 Implement pure Java status text placeholder formatting utility in `croppy-core` and verify with unit tests covering all token replacements

## 3. Platform Adapter & PacketEvents Integration (`croppy-paper`)

- [x] 3.1 Configure PacketEvents repository and `compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")` in `croppy-paper/build.gradle.kts` and declare `depend: [packetevents]` in `paper-plugin.yml`
- [x] 3.2 Implement `VirtualEntityIdGenerator` with atomic negative ID allocation and verify unit tests
- [x] 3.3 Implement `PacketStationDisplay` to maintain the 3 virtual entity IDs and per-tier viewer tracking sets
- [x] 3.4 Implement `PacketEventsStationRenderer` constructing and dispatching `WrapperPlayServerSpawnEntity`, `WrapperPlayServerEntityMetadata`, and `WrapperPlayServerDestroyEntities` packets
- [x] 3.5 Implement `StationViewerTracker` managing periodic proximity scans across the 3 LoD thresholds (48, 24, 8 blocks)
- [x] 3.6 Implement client-interpolated transformation animations (smooth rotation and thermal jitter vibration for boiling/searing temperatures)
- [x] 3.7 Add packet destruction cleanup hooks in `StationBlockListener`, `ChunkLifecycleListener`, and player connection listeners
- [x] 3.8 Update `PaperStationTicker` to drive virtual display updates and retire legacy world-persisted `ItemDisplay` spawning in `PaperDisplaySynchronizer`

## 4. Configuration & Full Test Verification

- [x] 4.1 Update `cooking_pot.yml` and `skillet.yml` with declarative `visual:` definitions and MiniMessage formatted status text templates
- [x] 4.2 Execute the full pre-commit verification gate (`./gradlew test`) across all modules and confirm 0 test failures
