# Proposal

## Why

When players connect to the server, teleport, or load world chunks containing crafting stations configured with 3D model displays and `hide_base_block: true` (such as `croppy:skillet`, `croppy:cooking_pot`, `croppy:stove`, or addon stations), Paper transmits the chunk's true Minecraft block states (`minecraft:furnace`, `minecraft:cauldron`, `minecraft:blast_furnace`, `minecraft:grindstone`, `minecraft:smoker`, `minecraft:brewing_stand`) via chunk packets. Currently, Croppy lacks a listener for `PlayerChunkLoadEvent`, does not evaluate stations during `PlayerJoinEvent`, and only sends block masking packets (`sendBlockChange` to `BARRIER`) during initial distance transitions when `!viewing`. Consequently, chunk packet arrival overwrites client-side block masks and leaves the station's physical base block permanently rendered and overlapping with the 3D model display.

## What Changes

- **Listen for `PlayerChunkLoadEvent`**: Register an event handler for `io.papermc.paper.event.packet.PlayerChunkLoadEvent` in `StationPlayerConnectionListener` that queries `DefaultStationManager.getStationsInChunk()` and dispatches immediate `sendBlockChange` barrier masking packets for all stations configured with `hide_base_block: true`.
- **Immediate Station Join Evaluation**: Trigger immediate station proximity evaluation in `StationPlayerConnectionListener.onPlayerJoin()` so that station virtual display entities and block masks spawn immediately upon player login without waiting up to 20 ticks for the recurring ticker.
- **Robust Block Masking and Lookup**:
  - Update `StationVirtualInteractionPacketListener` block change interceptors (`BLOCK_CHANGE` and `MULTI_BLOCK_CHANGE`) to check `DefaultStationManager` directly instead of relying solely on `activeDisplays`, ensuring blocks are masked even before the initial ticker cycle.
  - In `StationViewerTracker.syncStation()`, ensure viewing players with dirty or state-changed stations re-assert their block mask if needed.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `station-virtual-displays`: Extends the Base Block Client Masking requirement to explicitly guarantee concealment upon receiving chunk packets (`PlayerChunkLoadEvent`) and immediate login projection, preventing base block clipping with 3D model displays.

## Impact

- **Affected Code**:
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/station/listener/StationPlayerConnectionListener.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/station/listener/StationVirtualInteractionPacketListener.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/station/display/StationViewerTracker.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/CroppyPlugin.java`
  - Unit tests in `croppy-paper/src/test`
- **APIs & Dependencies**: Paper API 1.21.8+ (`io.papermc.paper.event.packet.PlayerChunkLoadEvent`), zero changes to `croppy-api` or `croppy-core`.
