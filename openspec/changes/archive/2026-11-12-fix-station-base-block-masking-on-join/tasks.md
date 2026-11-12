# Tasks

## 1. PlayerChunkLoadEvent and Immediate Join Projection

- [x] 1.1 Update `StationPlayerConnectionListener` to accept `DefaultStationManager` and add `@EventHandler` for `io.papermc.paper.event.packet.PlayerChunkLoadEvent` dispatching `sendBlockChange(BARRIER)` for stations in the loaded chunk configured with `hide_base_block: true`
- [x] 1.2 Add immediate station viewer evaluation method to `StationViewerTracker` and invoke it from `StationPlayerConnectionListener.onPlayerJoin` to project nearby stations upon login without waiting for the 20-tick cycle
- [x] 1.3 Add unit tests in `StationPlayerConnectionListenerTest` verifying that `PlayerChunkLoadEvent` sends barrier block changes to joining/receiving players for stations with `hide_base_block: true` and that `onPlayerJoin` evaluates station proximity

## 2. Spatial Querying in Packet Interceptors and Plugin Registration

- [x] 2.1 Update `StationVirtualInteractionPacketListener` to accept `DefaultStationManager` and query `stationManager.getStation()` directly in `handleBlockChange` and `handleMultiBlockChange`
- [x] 2.2 Update `CroppyPlugin` dependency injection to pass `core.getStationManager()` to `StationPlayerConnectionListener` and `StationVirtualInteractionPacketListener`
- [x] 2.3 Update `StationVirtualInteractionPacketListenerTest` verifying that block change rewriting functions when querying `DefaultStationManager` before displays are active

## 3. Verification & Quality Assurance

- [x] 3.1 Run `./gradlew test` to verify that all unit test suites across `croppy-api`, `croppy-core`, and `croppy-paper` pass with zero failures
