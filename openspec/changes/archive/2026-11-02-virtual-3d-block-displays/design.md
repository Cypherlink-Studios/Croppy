# Design

## Context

Croppy follows a strict Hexagonal Architecture across three modules (`croppy-api`, `croppy-core`, `croppy-paper`). Currently, agricultural crops use vanilla blockstates (`Ageable.setAge(...)` in `PaperDisplaySynchronizer`), while crafting stations utilize virtual PacketEvents displays for process animations. However, neither crops nor generic functional blocks currently support customizable 3D models with interactive hitboxes and player-facing placement orientation. See `proposal.md` for full motivation.

## Goals / Non-Goals

**Goals:**
- Provide a unified virtual 3D display architecture for crops and functional blocks using PacketEvents.
- Project paired client-side `ItemDisplay` (model geometry) and `Interaction` (raycast hitbox) entities with zero server-side chunk NBT persistence.
- Dynamically resize the virtual `Interaction` entity hitbox across developmental crop stages.
- Capture and snap player yaw on placement (`CARDINAL_4`, `CARDINAL_8`, `FREE_YAW`, `RANDOM_JITTER`) mapped to Y-axis quaternions.
- Intercept client click packets (`WrapperPlayClientInteractEntity`) targeting virtual interaction entities and safely route them to domain interaction handlers on the Bukkit main thread.
- Upgrade `CropBinaryCodec` to Version 3 to persist placement `yaw` with full retrocompatibility for V1 and V2 data.

**Non-Goals:**
- Overriding vanilla blockstates (such as NoteBlocks or Mushroom blocks) in the resource pack.
- Spawning persistent Bukkit entities (`world.spawn(..., ItemDisplay.class)`) in the server world.
- Supporting continuous multi-axis simultaneous rotation (`[x, y, z]`), as Minecraft 1.21.8 resource pack geometry restricts rotation to single-axis (-45° to +45°).

## Decisions

### 1. Dual Virtual Entity Pipeline: ItemDisplay + Interaction
- **Decision**: For every 3D block or crop, track two virtual entity IDs managed by a unified `VirtualBlockDisplay` tracker:
  1. `EntityTypes.ITEM_DISPLAY`: Holds an `ItemStack` referencing the 1.21.8 `item_model` (or custom model data) with scale, translation, and Y-axis rotation quaternion.
  2. `EntityTypes.INTERACTION`: Serves as the raycast hitbox with configurable `width` and `height`.
- **Rationale**: Combining both provides native Minecraft client crosshair targeting and outline rendering without persisting any entity in chunk NBT.
- **Alternatives Considered**:
  - *Bukkit Entity Spawning*: Simpler to implement via Bukkit API, but causes persistent ghost entities on server crashes and increases chunk save file sizes.
  - *Tripwire / NoteBlock Resource Overrides*: Overrides vanilla mechanics, limits models to 16x16, and conflicts with other plugins.

### 2. PacketEvents Inbound Click Interception
- **Decision**: Register a PacketEvents packet listener intercepting `PacketType.Play.Client.INTERACT_ENTITY` (`WrapperPlayClientInteractEntity`).
  - Matches `targetEntityId` against active `VirtualBlockDisplay` interaction entity IDs.
  - If matched, syncs execution to the Bukkit main thread via `Bukkit.getScheduler().runTask(...)`.
  - Dispatches `ATTACK` (left-click) to block break / punching handlers.
  - Dispatches `INTERACT` / `INTERACT_AT` (right-click) to tool usage (watering can, fertilizer, harvesting, station UI).
- **Rationale**: Virtual entities do not exist on the Bukkit server, so Bukkit's `PlayerInteractEntityEvent` will never fire for them. Intercepting the raw protocol packet is necessary and allows zero-latency click routing.

### 3. Placement Orientation & Rotation Modes
- **Decision**: Capture player yaw on planting/placement and compute Y-axis quaternion:
  - `CARDINAL_4`: Snaps to 0°, 90°, 180°, 270° (ideal for cutting boards, stoves, workbenches).
  - `CARDINAL_8`: Snaps to 45° increments (0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°).
  - `FREE_YAW`: Stores exact player facing angle (ideal for single-plant crops).
  - `RANDOM_JITTER`: Adds `(random() - 0.5) * 30.0` degrees to player yaw for organic agricultural variation.
  - Rotation quaternion: $q = \left(0, \sin(\theta/2), 0, \cos(\theta/2)\right)$ where $\theta$ is in radians.

### 4. Crop Persistence Version 3 Binary Schema
- **Decision**: Bump `CropBinaryCodec.CURRENT_VERSION` to `3`:
  - V3 appends a 4-byte `float` for `yaw` per crop instance: `out.writeFloat(crop.getYaw())`.
  - Backward compatibility: V1 and V2 readers default `yaw` to `0.0f`.
  - SQLite backend: Add `yaw REAL DEFAULT 0.0` column in database migration.

### 5. Hexagonal Architecture Distribution
- **`croppy-api`**:
  - `RotationMode` enum (`CARDINAL_4`, `CARDINAL_8`, `FREE_YAW`, `RANDOM_JITTER`).
  - `Model3DDefinition` immutable record (`Key itemModel`, `float scaleX`, `float scaleY`, `float scaleZ`, `float translationX`, `float translationY`, `float translationZ`, `float hitboxWidth`, `float hitboxHeight`).
  - `CropInstance`: Add `float getYaw()` and `void setYaw(float yaw)`.
- **`croppy-core`**:
  - `CropBinaryCodec`: Version 3 serialization/deserialization.
  - `YamlSpeciesLoader`: Parses `display.type: model_3d`, `rotation_mode`, and stage-specific `model` and `hitbox` mappings.
  - `RotationCalculator`: Pure utility converting angles to cardinal snaps and quaternions.
- **`croppy-paper`**:
  - `VirtualBlockDisplay`: State holder for block position, item display ID, and interaction ID.
  - `PacketEventsBlockRenderer`: Dispatches spawn, metadata, and destroy packets for item displays and interaction hitboxes.
  - `VirtualInteractionPacketListener`: Intercepts client packets and routes clicks.
  - Placement listener: Intercepts seed planting, calculates yaw, and initializes the virtual display.

## Risks / Trade-offs

- **[Double Click Trigger on Main Hand / Off Hand]** → *Mitigation*: In `VirtualInteractionPacketListener`, ignore `OFF_HAND` interactions when the `MAIN_HAND` interaction was already processed in the same tick.
- **[Accidental Block Placement into Virtual Block Space]** → *Mitigation*: Register a Bukkit `BlockPlaceEvent` listener that checks `CropManager` and cancels block placements at coordinates occupied by active crops or virtual blocks.
- **[Farmland Reversion to Dirt]** → *Mitigation*: Maintain `AIR` or non-solid pass-through blocks at the crop coordinate so vanilla Minecraft farmland lighting and hydration checks succeed.
