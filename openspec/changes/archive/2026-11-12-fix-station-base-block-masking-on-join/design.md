# Design

## Context
See `proposal.md` for background and problem motivation.

In Croppy, crafting stations with 3D model displays (`visual.base_model`) retain physical blocks (`minecraft:furnace`, `minecraft:cauldron`, `minecraft:blast_furnace`, `minecraft:grindstone`, `minecraft:smoker`, `minecraft:brewing_stand`) in the Paper world for server-side persistence, physics, and interaction routing. To prevent visual interference with virtual `ItemDisplay` entities, definitions set `visual.hide_base_block: true`.

Currently:
1. `StationPlayerConnectionListener` handles `PlayerQuitEvent`, `PlayerTeleportEvent`, and `PlayerChangedWorldEvent`, but its `onPlayerJoin()` only calls `blockTracker.tickViewers()` (for 3D crops), completely ignoring stations.
2. When a player receives a world chunk, Paper transmits `ClientboundLevelChunkWithLightPacket` containing the raw world block. Croppy has no listener for `io.papermc.paper.event.packet.PlayerChunkLoadEvent`, so the chunk packet overwrites any previous client-side barrier mask.
3. `StationViewerTracker.syncStation()` only sends `sendBlockChange(BARRIER)` upon initial distance transition when `!viewing`. Once `display.addViewer(VisualTier.BASE_MODEL, playerId)` is marked, subsequent ticker cycles never re-send the mask.
4. `StationVirtualInteractionPacketListener` checks `viewerTracker.getDisplay(blockPos)` rather than `stationManager.getStation(blockPos)`, meaning block changes occurring before the first 20-tick ticker cycle are missed.

## Goals / Non-Goals

**Goals:**
- Guarantee that whenever a player receives a chunk containing stations with `hide_base_block: true`, the base block is immediately masked as `BARRIER` via `PlayerChunkLoadEvent`.
- Eliminate the 0–1000ms delay for station displays upon player login by evaluating nearby stations immediately in `PlayerJoinEvent`.
- Decouple block change packet rewriting in `StationVirtualInteractionPacketListener` from active display state by querying `DefaultStationManager` directly.
- Preserve 100% backward compatibility for all existing station definitions and ensure zero compilation or unit test regressions.

**Non-Goals:**
- Modifying crop display logic (`VirtualBlockDisplayTracker`).
- Removing physical blocks from the Paper world (functional server blocks are required for world physics and storage).
- Parsing raw chunk byte buffers via PacketEvents.

## Decisions

### Decision 1: Listen to `PlayerChunkLoadEvent` in `StationPlayerConnectionListener`
- **Choice**: Inject `DefaultStationManager` into `StationPlayerConnectionListener`. Register an `@EventHandler(priority = EventPriority.MONITOR)` for `io.papermc.paper.event.packet.PlayerChunkLoadEvent`.
- **Logic**:
  1. Extract `Chunk chunk = event.getChunk()` and `Player player = event.getPlayer()`.
  2. Query `stationManager.getStationsInChunk(new ChunkPosition(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()))`.
  3. For each station whose definition declares `visual.hideBaseBlock() == true`:
     - Resolve `BlockData barrierData = viewerTracker.resolveBarrierBlockData()`.
     - Dispatch `player.sendBlockChange(stationLoc, barrierData)`.
- **Rationale**: Paper fires `PlayerChunkLoadEvent` synchronously when a chunk packet has been dispatched to a player. Appending `sendBlockChange` right after the chunk transmission ensures the client receives and processes the barrier mask immediately after the chunk blocks are populated, eliminating visual overlap with zero frame flickering.
- **Alternatives Considered**: PacketEvents chunk data packet interception (`WrapperPlayServerChunkData`). *Rejected* because decompressing and re-encoding full chunk byte arrays in Java on every chunk transmission is CPU-intensive, version-fragile, and redundant given Paper's native packet event.

### Decision 2: Immediate Station Projection on `PlayerJoinEvent`
- **Choice**: Add `tickPlayer(Player player, DefaultStationManager stationManager)` in `StationViewerTracker` (or trigger station proximity evaluation from `StationPlayerConnectionListener.onPlayerJoin`).
- **Logic**:
  1. When `PlayerJoinEvent` fires, in addition to `blockTracker.tickViewers()`, invoke station proximity evaluation for the joining player.
  2. For every station in loaded chunks within viewing distance, call `syncStation(station, world, process)` (or dispatch base model and interaction entities immediately).
- **Rationale**: Eliminates the up to 20-tick (1 second) delay where a joining player sees nothing or sees delayed pop-ins.

### Decision 3: Direct `DefaultStationManager` Query in `StationVirtualInteractionPacketListener`
- **Choice**: Inject `DefaultStationManager` into `StationVirtualInteractionPacketListener`.
- **Logic**: In `handleBlockChange` and `handleMultiBlockChange`, lookup stations using `stationManager.getStation(blockPos)` instead of `viewerTracker.getDisplay(blockPos)`.
- **Rationale**: `activeDisplays` is only populated once `syncStation` runs. Querying the spatial station manager directly guarantees block change packets are intercepted and masked even before the initial display ticker cycle.

## Risks / Trade-offs

- **[Performance on Chunk Load]**  
  *Risk*: Frequent `PlayerChunkLoadEvent` dispatches could cause main thread overhead if looking up stations is slow.  
  *Mitigation*: `DefaultStationManager.getStationsInChunk()` performs an $O(1)$ spatial hash lookup on `chunkIndex` (`Map<ChunkPosition, Set<BlockPosition>>`), iterating only the few (typically 0-5) stations within that chunk. If a chunk contains no stations, it returns `Collections.emptySet()` immediately.

- **[Player Disconnection During Chunk Transmission]**  
  *Risk*: If a player disconnects while chunks are being sent, calling `sendBlockChange` could trigger an error.  
  *Mitigation*: Guard `player.isOnline()` and wrap packet dispatch with exception handling logged to `logger.trace()`.
