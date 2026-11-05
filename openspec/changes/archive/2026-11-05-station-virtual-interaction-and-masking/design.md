# Technical Design: Station Virtual Interaction and Base Block Masking

## Context
See `proposal.md` for background and motivation.

Croppy currently renders three virtual display tiers for crafting stations via `PacketEventsStationRenderer` and `StationViewerTracker`: `BASE_MODEL` (Item or Block Display), `PROCESS_ITEM` (Item Display), and `TEXT_DISPLAY` (Text Display). Crops in Croppy already leverage client-side `INTERACTION` entities (`VirtualInteractionPacketListener`) to capture left/right clicks without physical block collision. Stations, in contrast, relied directly on physical Bukkit block placement and `PlayerInteractEvent(RIGHT_CLICK_BLOCK)`. When configuring cookware such as `croppy:skillet` with a player head base block and a pressure plate or 3D item display, Steve's opaque skull model clips through the display.

## Goals / Non-Goals

**Goals:**
- Provide declarative YAML configuration for `hide_base_block` and custom `interaction` hitbox dimensions (`width`, `height`).
- Keep pure domain records in `croppy-api` with zero Paper/PacketEvents dependencies.
- Project client-side virtual `INTERACTION` entities via PacketEvents for stations with configured interaction hitboxes.
- Dispatch packet block masking (`sendBlockChange` to `BARRIER`) to viewing players so the physical base block is invisible in Survival mode.
- Intercept client click packets (`INTERACT_ENTITY`) to dispatch `ATTACK` (station break/drop) and `INTERACT` (cooking/GUI).

**Non-Goals:**
- Modifying crop interaction pipelines (crops retain `VirtualBlockDisplayTracker`).
- Removing the server-side block from the Paper world (the block remains in world storage and chunk PDC for spatial persistence and chunk lifecycle events).
- Client mods or shader-based transparency.

## Decisions

### 1. Pure Domain Modeling in `croppy-api`
Define an immutable record in `dev.darkblade.croppy.api.station.visual`:
```java
public record StationInteractionBox(float width, float height) {
    public StationInteractionBox {
        if (width <= 0.0f) throw new IllegalArgumentException("width must be positive");
        if (height <= 0.0f) throw new IllegalArgumentException("height must be positive");
    }
}
```
Update `StationVisualDefinition`:
- Fields: `DisplayLayerConfig baseModel`, `DisplayLayerConfig processDisplay`, `TextDisplayTemplate textDisplay`, `boolean hideBaseBlock`, `StationInteractionBox interactionBox`.
- Maintain backward-compatible constructors defaulting `hideBaseBlock` to `false` and `interactionBox` to `null`.

*Alternative considered*: Storing interaction dimensions in generic `properties` map. Rejected because visual geometry and hitbox dimensions are core visual contract properties that belong in typed records.

### 2. Interaction Tier in `PacketStationDisplay`
Add `INTERACTION` to `VisualTier` enum:
- Allocate `interactionId` from `VirtualEntityIdGenerator` and `interactionUuid` from `UUID.randomUUID()`.
- Add lookup helper `getDisplayByInteractionId(int entityId)` in `StationViewerTracker`.

### 3. Base Block Packet Masking
In `StationViewerTracker`, when adding a viewer for a station whose `visualDefinition.hideBaseBlock()` is `true`:
- Dispatch `player.sendBlockChange(location, Material.BARRIER.createBlockData())`.
- When removing viewers or destroying the station display, restore the real block data via `player.sendBlockChange(location, block.getBlockData())`.

*Alternative considered*: Sending `Material.AIR`. Rejected because `AIR` allows players holding blocks to place items right inside the station position, whereas `BARRIER` prevents block placement into the occupied voxel while remaining 100% invisible in Survival mode.

### 4. Packet-Level Click Interception via `StationVirtualInteractionPacketListener`
Create an inbound `PacketListenerAbstract` listening for `PacketType.Play.Client.INTERACT_ENTITY`:
1. Check if the targeted entity ID matches an active station interaction entity in `StationViewerTracker`.
2. Cancel the raw entity interaction packet.
3. Switch execution to the Paper main thread:
   - **`ATTACK`**: Triggers station dismantling via a shared method in `StationBlockListener`, dropping the station item and stored ingredients with block break sound and particles.
   - **`INTERACT`**: Forwards player, station instance, and equipment hand to `StationInteractListener.handleStationInteraction()`.

## Risks / Trade-offs

- **[Risk] Paper resends real block data on cancelled interactions**  
  *Mitigation*: If a player right-clicks with an item in hand, Bukkit's cancellation handler may resend the real block data (`player_head`). When handling station interaction, `StationViewerTracker` re-asserts the `sendBlockChange` mask to keep the station invisible.

- **[Risk] Hitbox desynchronization with dynamic station yaw**  
  *Mitigation*: Unlike asymmetrical models, `INTERACTION` entities in Minecraft Java are axis-aligned bounding boxes centered at the block center (`pos.x() + 0.5`, `pos.y()`, `pos.z() + 0.5`), which perfectly covers horizontal rotations of compact stations.
