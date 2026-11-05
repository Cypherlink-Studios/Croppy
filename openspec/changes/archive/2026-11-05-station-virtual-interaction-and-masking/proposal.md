# Proposal: Station Virtual Interaction and Base Block Masking

## Why
When placing custom stations such as `croppy:skillet` that use player heads (`minecraft:player_head`) as their base block and project 3D Item Displays (e.g. `minecraft:heavy_weighted_pressure_plate`), Minecraft renders an opaque Steve head overlapping the custom visual model. In vanilla Java Edition, skull textures cannot be transparent on the base layer. Furthermore, using full blocks forces a 1x1x1 hitbox that clashes with flat cookware like skillets.

Introducing packet-level base block masking and virtual `INTERACTION` entities allows stations to conceal the underlying physical block from client view while projecting custom-sized interaction hitboxes that cleanly dispatch left-click break actions and right-click cooking/menu interactions.

## What Changes
- **croppy-api**:
  - Add `hideBaseBlock` boolean property and optional `InteractionBox` record (`width`, `height`) to `StationVisualDefinition`.
- **croppy-core**:
  - Update `YamlStationLoader` to parse `visual.hide_base_block` and `visual.interaction` (`width`, `height`) from station YAML definitions.
- **croppy-paper**:
  - Extend `PacketStationDisplay` to allocate an entity ID and UUID for an `INTERACTION` virtual entity tier.
  - Extend `PacketEventsStationRenderer` to spawn and destroy virtual `INTERACTION` entities (`EntityTypes.INTERACTION`) with configurable width and height bounding boxes.
  - Update `StationViewerTracker` to send packet block changes (`sendBlockChange` to `BARRIER` or `AIR`) to viewing players when `hide_base_block` is enabled, and tear down masking when displays are destroyed.
  - Implement a dedicated PacketEvents inbound packet handler (`StationVirtualInteractionPacketListener` or extended `VirtualInteractionPacketListener`) intercepting `INTERACT_ENTITY` packets for stations:
    - `ATTACK` (left-click): Dismantles and drops the station and its stored contents, triggering break particles and sounds.
    - `INTERACT` (right-click): Routes to the declarative `InteractionRouter` or fallback station interaction pipeline.
  - Update default `croppy:skillet` station definition to enable `hide_base_block: true` and define interaction dimensions (`width: 0.8`, `height: 0.25`).

## Capabilities

### Modified Capabilities
- `station-virtual-displays`: Add requirements for client-side base block packet masking and virtual `INTERACTION` entity projection with packet-level click routing.

## Impact
- **APIs**: Backward-compatible constructor updates in `StationVisualDefinition`.
- **Packet Events**: Inbound packet interception for station `INTERACTION` entity IDs.
- **Rendering**: Station base blocks are cleanly masked for clients without mutating server world chunk state.
