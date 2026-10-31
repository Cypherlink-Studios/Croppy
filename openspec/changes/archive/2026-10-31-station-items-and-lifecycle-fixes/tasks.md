# Tasks

## 1. Station Custom Item Definitions & Resource Extraction

- [x] 1.1 Create `items/stations/cooking_pot.yml` under `croppy-paper/src/main/resources/items/stations/` with category `stations`, material `minecraft:player_head`, and MiniMessage lore
- [x] 1.2 Create `items/stations/skillet.yml` under `croppy-paper/src/main/resources/items/stations/` with category `stations`, material `minecraft:heavy_weighted_pressure_plate`, and MiniMessage lore
- [x] 1.3 Update `CroppyPlugin.setupItemDefinitions()` to extract default `items/stations/` files and verify registration into `ItemRegistry`

## 2. Station Block Destruction Drop

- [x] 2.1 Update `StationBlockListener.onBlockBreak` to drop the station's custom `ItemStack` via `ItemFactory` along with stored inputs/outputs
- [x] 2.2 Add unit test in `croppy-paper` verifying that station block breakage produces both stored contents and the station item

## 3. Dynamic Chunk Lifecycle Persistence

- [x] 3.1 Update `ChunkLifecycleListener` to accept `StationStorage` and `DefaultStationManager`
- [x] 3.2 Implement chunk station loading in `ChunkLifecycleListener.onChunkLoad` and chunk station persistence/eviction in `ChunkLifecycleListener.onChunkUnload`
- [x] 3.3 Update `CroppyPlugin` instantiation and listener registration of `ChunkLifecycleListener`
- [x] 3.4 Add unit tests for `ChunkLifecycleListener` chunk load/unload handling with stations and verify with `./gradlew test`

## 4. Physical Empty Station Feedback & Verification

- [x] 4.1 Update `StationInteractListener` to display an action bar status indicator and audio cue when right-clicking an empty physical station with an empty hand
- [x] 4.2 Execute `./gradlew test` across all modules and confirm 0 test failures
