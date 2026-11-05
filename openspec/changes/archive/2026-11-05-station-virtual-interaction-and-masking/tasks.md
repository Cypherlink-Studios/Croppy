# Tasks: Station Virtual Interaction and Base Block Masking

## 1. Domain Modeling (croppy-api)

- [x] 1.1 Create `StationInteractionBox` record (`float width, float height`) with positive dimension validation and unit tests
- [x] 1.2 Update `StationVisualDefinition` with `hideBaseBlock` and optional `StationInteractionBox interactionBox`, preserving backward-compatible constructors and verifying unit tests

## 2. Configuration & Loader (croppy-core)

- [x] 2.1 Update `YamlStationLoader` to parse `visual.hide_base_block` and `visual.interaction` (`width`, `height`)
- [x] 2.2 Add unit tests in `YamlStationLoaderTest` verifying station YAML parsing with `hide_base_block` and `interaction` properties

## 3. Virtual Entity Projection & Block Masking (croppy-paper)

- [x] 3.1 Extend `VisualTier` with `INTERACTION` and update `PacketStationDisplay` to allocate interaction entity ID and UUID
- [x] 3.2 Implement `spawnInteraction` and entity destroy logic for stations in `PacketEventsStationRenderer`
- [x] 3.3 Update `StationViewerTracker` to spawn/destroy interaction entities and dispatch `sendBlockChange` packet masking (`BARRIER`) when `hideBaseBlock` is enabled
- [x] 3.4 Add unit tests for `StationViewerTracker` and `PacketEventsStationRenderer` covering interaction tier and block masking

## 4. Inbound Packet Interaction & Dismantling (croppy-paper)

- [x] 4.1 Implement `StationVirtualInteractionPacketListener` intercepting `INTERACT_ENTITY` packets for station interaction IDs
- [x] 4.2 Route `ATTACK` (left-click) to station dismantling logic, dropping the station item and stored inputs/outputs with break effects
- [x] 4.3 Route `INTERACT` (right-click) to `StationInteractListener` and the declarative `InteractionRouter`
- [x] 4.4 Register `StationVirtualInteractionPacketListener` in `CroppyPlugin` PacketEvents lifecycle

## 5. Station Configuration & Integration Verification

- [x] 5.1 Update default `skillet.yml` station definition with `hide_base_block: true` and `interaction` dimensions (`width: 0.8`, `height: 0.25`)
- [x] 5.2 Execute Gradle verification (`.\gradlew.bat test`) and verify 100% test pass rate across all modules
