# Proposal

## Why

Minecraft Paper servers and agricultural simulation plugins struggle with rendering custom 3D models for crops, appliances, and decorative blocks. Placing real Bukkit entities (`ItemDisplay`, `ArmorStand`, or `Interaction`) into the world pollutes chunk NBT, creates persistent "ghost entities" upon server crashes or unmonitored block breaks, and causes server-side tick lag. Conversely, overriding vanilla blockstates (e.g. NoteBlocks or Tripwire) limits models to 16x16 pixels and causes friction with other plugins.

Croppy requires a high-performance, packet-based virtual 3D display architecture that couples client-side `ItemDisplay` models with virtual `Interaction` entity hitboxes. This provides seamless, continuous player-facing rotation on placement, stage-specific dynamic hitboxes, and packet-level click routing with zero chunk persistence.

## What Changes

- **Virtual 3D Model Display Pipeline**: Introduce client-side `ItemDisplay` projection for crops and custom blocks via PacketEvents, referencing Minecraft 1.21.8 `item_model` definitions or custom model data without spawning persistent entities in the world.
- **Virtual Interaction Hitbox Pipeline**: Project client-side `Interaction` entities alongside 3D displays with configurable dimensions (`width`, `height`) that dynamically scale with developmental growth stages.
- **PacketEvents Inbound Click Interception**: Intercept client `WrapperPlayClientInteractEntity` packets (`ATTACK` for left-click breaking/punching and `INTERACT` / `INTERACT_AT` for right-click harvesting, watering, or opening interfaces) and dispatch them safely to the Bukkit main thread.
- **Player Placement Orientation & Snapping**: Capture player yaw on planting/placement and support configurable rotation modes (`CARDINAL_4`, `CARDINAL_8`, `FREE_YAW`, `RANDOM_JITTER`) mapped to Y-axis transformation quaternions.
- **Crop Persistence V3 Schema**: Update `CropInstance` and `CropBinaryCodec` to Version 3 to persist placement `yaw` (4-byte float) with backward compatibility for V1 and V2 records.
- **Declarative YAML Configuration**: Extend `species/*.yml` and block definitions to support `display.type: model_3d`, `rotation_mode`, and stage-specific `model` and `hitbox` parameters.

## Capabilities

### New Capabilities
- `virtual-3d-block-displays`: Manages client-side packet-based virtual 3D display entities (`ItemDisplay`) paired with virtual `Interaction` entities for crops and functional blocks, tracking player placement orientation and dispatching client interaction packets without server entity persistence.

### Modified Capabilities
- `species-definitions`: Enhances declarative crop species definitions to support `model_3d` display configurations, rotation policies, and stage-specific 3D model geometry with dynamic hitboxes.
- `crop-persistence`: Extends chunk PDC binary encoding and database storage to persist crop orientation yaw values with schema versioning.

## Impact

- **croppy-api**:
  - Adds `RotationMode` enum (`CARDINAL_4`, `CARDINAL_8`, `FREE_YAW`, `RANDOM_JITTER`).
  - Adds `Model3DDefinition` immutable record (item model key, scale, translation, hitbox dimensions).
  - Adds `getYaw()` / `setYaw(float yaw)` to `CropInstance`.
- **croppy-core**:
  - Updates `YamlSpeciesLoader` to parse `model_3d`, rotation modes, and stage-level 3D models.
  - Updates `CropBinaryCodec` with schema `VERSION_3` serializing yaw while retaining V1 and V2 deserialization.
  - Adds `RotationCalculator` utility for snapping and quaternion generation.
- **croppy-paper**:
  - Implements `PacketEventsBlockRenderer` and `VirtualBlockDisplay` tracking.
  - Implements `VirtualInteractionPacketListener` for `WrapperPlayClientInteractEntity`.
  - Integrates rotation and virtual display lifecycle in `PaperDisplaySynchronizer` and placement event listeners.
