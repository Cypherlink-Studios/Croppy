# Design

## Context

See `proposal.md` for background and problem motivation.
`croppy-paper` utilizes PacketEvents to project client-side virtual entities (`ITEM_DISPLAY` and `INTERACTION`) for crops configured with `model_3d`. A compilation oversight in `PacketEventsBlockRenderer` caused packet parameters to be typed as `Object`, leading to runtime `ClassCastException` failures inside PacketEvents' `PlayerManager`, completely breaking virtual entity rendering.

## Goals / Non-Goals

**Goals:**
- Guarantee error-free packet dispatch for all virtual 3D crop displays and hitboxes.
- Align `PacketEventsBlockRenderer` with `PacketEventsStationRenderer` regarding method signatures and safety checks.
- Prevent viewer tracking state desynchronization in `VirtualBlockDisplayTracker`.
- Provide zero-delay visual display generation when a player logs into the server near 3D crops.
- Provide comprehensive unit test coverage with packet interception and assertion.

**Non-Goals:**
- Changing entity types or metadata index structures.
- Altering core crop lifecycle logic or database schemas in `croppy-core` / `croppy-api`.

## Decisions

### 1. Strongly Typed `sendPacket` in `PacketEventsBlockRenderer`
- **Choice**: Change `sendPacket(Player player, Object packetWrapper)` to:
  ```java
  public void sendPacket(Player player, PacketWrapper<?> packet) {
      if (player == null || !player.isOnline()) return;
      try {
          PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
      } catch (Exception e) {
          logger.log(Level.WARNING, "Failed to dispatch packet to player " + player.getName(), e);
      }
  }
  ```
- **Rationale**: PacketEvents' `PlayerManager` exposes `sendPacket(Object channel, Object rawPacket)` and `sendPacket(Object player, PacketWrapper<?> packet)`. With `PacketWrapper<?>`, the Java compiler strictly binds to the wrapper overload, enabling PacketEvents to convert the packet into Paper's NMS packet and send it to the player.
- **Alternatives Considered**: Casting inside the method body without changing the method parameter signature. *Rejected* because strong parameter typing catches invalid packet types at compile time and makes API contracts explicit.

### 2. Guard Viewer Tracking State on Dispatch
- **Choice**: In `VirtualBlockDisplayTracker.syncDisplay` and `tickViewers`, ensure `display.addViewer(playerId)` accurately represents successfully queued/sent viewers.
- **Rationale**: Previously, if packet dispatch threw an exception, the player was already marked as `viewing`, preventing subsequent distance checks from re-attempting `spawnBlockDisplay`.

### 3. Immediate Projection via `PlayerJoinEvent`
- **Choice**: Add `@EventHandler(priority = EventPriority.MONITOR)` for `PlayerJoinEvent` in `StationPlayerConnectionListener` that triggers:
  ```java
  if (blockTracker != null) {
      blockTracker.tickViewers();
  }
  if (viewerTracker != null) {
      viewerTracker.tickViewers();
  }
  ```
- **Rationale**: Eliminates the 0–1000ms visual pop-in delay where a player logs into a garden and waits up to 20 ticks before crops render.

### 4. Unit Test Suite using Mock PacketEventsAPI
- **Choice**: Create `PacketEventsBlockRendererTest` using a captured `PlayerManager` mock (matching the pattern in `PacketEventsStationRendererTest`).
- **Rationale**: Verifies that `spawnBlockDisplay`, `updateBlockDisplay`, and `destroyBlockDisplay` invoke `sendPacket(Player, PacketWrapper<?>)` and create valid `WrapperPlayServerSpawnEntity` and `WrapperPlayServerEntityMetadata` packets.

## Risks / Trade-offs

- **[Performance on Player Join]** → *Mitigation*: `tickViewers()` iterates only active displays in loaded chunks and performs simple fast distance squared checks (`viewDistanceSq = 48 * 48 = 2304`).
