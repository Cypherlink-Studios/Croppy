# Proposal

## Why

3D crops configured with `type: "model_3d"` (such as grapes, tomatoes, and rice) currently fail to render their virtual display entities when planted or when players enter their viewing range. In `PacketEventsBlockRenderer.java`, the internal packet dispatch method accepts `Object` rather than `PacketWrapper<?>`, causing Java overload resolution to bind to `PlayerManager.sendPacket(Object channel, Object packet)` rather than `sendPacket(Object player, PacketWrapper<?> packet)`. At runtime on Paper, this throws a `ClassCastException` that is silently swallowed, completely preventing `ItemDisplay` and `Interaction` virtual entities from reaching the client and leaving crops invisible in the world.

## What Changes

- **Fix PacketEvents dispatch overload**: Strongly type the `sendPacket` method in `PacketEventsBlockRenderer` to accept `PacketWrapper<?>`, ensuring proper dispatch to Paper players via PacketEvents without runtime class casting errors.
- **Null and Online Player Guards**: Ensure packets are only dispatched to valid, online players in `PacketEventsBlockRenderer`.
- **Viewer Registration Resilience**: Prevent viewer tracking deadlocks in `VirtualBlockDisplayTracker` where a failed dispatch leaves the player marked as viewing, locking them out of future spawn evaluations.
- **Immediate Login Projection**: Hook `PlayerJoinEvent` into `StationPlayerConnectionListener` so that virtual block and station displays in loaded chunks surrounding joining players are evaluated immediately without waiting for the 20-tick poll cycle.
- **Unit Test Regression Suite**: Introduce a dedicated `PacketEventsBlockRendererTest` verifying packet creation, types, and PacketEvents wrapper dispatch.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `virtual-3d-block-displays`: Fixes and guarantees the projection of client-side PacketEvents virtual entities (`ItemDisplay` and `Interaction`) upon planting, chunk loading, and player connection.

## Impact

- **Affected Code**:
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/display/PacketEventsBlockRenderer.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/display/VirtualBlockDisplayTracker.java`
  - `croppy-paper/src/main/java/dev/darkblade/croppy/platform/paper/station/listener/StationPlayerConnectionListener.java`
- **APIs & Dependencies**: No breaking API changes in `croppy-api` or `croppy-core`. No changes to external dependencies.
