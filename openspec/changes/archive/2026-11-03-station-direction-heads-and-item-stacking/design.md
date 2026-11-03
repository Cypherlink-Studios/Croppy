# Technical Design: Dynamic Station Direction, Skull Textures, and Item Stacking Fix

## Context

Croppy uses a 3-module Hexagonal Architecture (`croppy-api`, `croppy-core`, `croppy-paper`).
See [proposal.md](proposal.md) for background and motivation.

- `croppy-api`: Defines ports and immutable records. `StationInstance` currently lacks a horizontal yaw orientation, whereas `CropInstance` already has `getYaw()` and `RotationMode`.
- `croppy-core`: `StationBinaryCodec` (version 3) serializes spatial stations into SQLite BLOBs but omits yaw. `YamlItemLoader` lacks parsing for custom player head skin textures.
- `croppy-paper`: `PacketEventsStationRenderer` handles virtual entity displays via PacketEvents. `createBukkitItem` logic is duplicated across 4 classes, wrapping vanilla items in synthetic `ItemDefinition` records with gray display names and PDC tags, breaking vanilla stackability.

## Goals / Non-Goals

**Goals:**
- Enable crafting station models to dynamically face the player upon placement using `CARDINAL_4`, `CARDINAL_8`, or `FREE_YAW`.
- Persist station orientation across chunk unloads and restarts with backward compatibility for legacy records (V1-V3 defaulting to 0.0f).
- Support base64 textures and direct Mojang texture URLs on `minecraft:player_head` items and in-world skull block states.
- Eliminate vanilla item modification in station storage, restoring seamless stackability with vanilla items.

**Non-Goals:**
- Modifying crop rotation mechanics or `CropBinaryCodec`.
- Implementing custom skin uploading or external HTTP skin fetching (only standard Mojang skin URLs and base64 payloads supported locally).
- Redesigning the menu rendering architecture or `StorageComponent` contracts.

## Decisions

### 1. Station Orientation Tracking & Persistence
- **Decision**: Add `float getYaw()` and `void setYaw(float yaw)` to `StationInstance` and `DefaultStationInstance`. In `StationDefinition`, add `RotationMode rotationMode` defaulting to `RotationMode.CARDINAL_4`.
- **Placement Facing**: When a player places a station, calculate the facing angle towards the player: `player.getLocation().getYaw() + 180.0f`, passed through `RotationCalculator.calculate(yaw, rotationMode)`.
- **Binary Codec Version 4**:
  - `StationBinaryCodec.CURRENT_VERSION = 4`.
  - For V4, write `out.writeFloat(station.getYaw())` after block coordinates `(x, y, z)`.
  - When deserializing V1, V2, or V3 data, default `yaw` to `0.0f`.
- **Alternatives Considered**:
  - Storing facing in a separate station component: Rejected because spatial coordinates and orientation belong to core instance identity.

### 2. Virtual Display Rotation Matrix
- **Decision**: In `PacketEventsStationRenderer`:
  - Calculate combined base model quaternion: `new Quaternionf().rotateY((float) Math.toRadians(-stationYaw)).rotateXYZ(pitch, baseYaw, roll)`.
  - For `process_display`, rotate horizontal translation offsets `(tx, tz)` by `stationYaw` so offsets remain relative to the station's facing.
- **Alternatives Considered**:
  - Setting entity yaw in `WrapperPlayServerSpawnEntity`: Display entities in Minecraft calculate translations relative to local coordinates when yaw is applied at entity level, which can cause offset drift with block displays. Computing combined left rotation quaternion ensures exact geometric alignment.

### 3. Base64 & Mojang Texture URL Support for Player Heads
- **Decision**:
  - Add optional `String skullTexture` to `ItemDefinition`.
  - In `YamlItemLoader`, parse `texture` or `skull-texture` from YAML.
  - If a Mojang URL is provided (`http://textures.minecraft.net/texture/...`), construct the canonical skin JSON payload and encode to Base64:
    `{"textures":{"SKIN":{"url":"..."}}}`
  - In `ItemFactory.applyToMeta`, if `meta instanceof SkullMeta skullMeta`:
    Use a deterministic UUID `UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8))` to instantiate `Bukkit.createProfile(uuid)` and set `new ProfileProperty("textures", base64)`.
  - In `StationBlockListener`, if `event.getBlockPlaced().getState() instanceof Skull skull`, set the profile and call `skull.update(true, false)`.
- **Alternatives Considered**:
  - Using random UUIDs (`UUID.randomUUID()`): Rejected because clients re-request skin textures on every entity spawn if UUIDs change, causing stutter and head flickering.

### 4. Centralized, Non-Destructive Item Construction in `ItemFactory`
- **Decision**:
  - Add `public ItemStack createBukkitItem(StoredItem stored)` to `ItemFactory`.
  - If `itemRegistry.get(stored.itemKey())` is present: build custom item via `createItemStack(definition, stored.amount())`.
  - If absent: resolve material via `Material.matchMaterial(stored.itemKey().value())`. If valid vanilla material, construct `new ItemStack(material, stored.amount())` directly without touching `ItemMeta`.
  - Replace duplicate implementations in `PaperMenuRenderer`, `StationInteractListener`, `StationBlockListener`, and `StationGuiListener`.
- **Alternatives Considered**:
  - Storing serialized `ItemStack` bytes inside `StoredItem`: Rejected because domain storage (`croppy-api`/`croppy-core`) must remain strictly isolated from Bukkit platform objects.

## Risks / Trade-offs

- **[Risk]** Legacy station storage BLOBs might fail deserialization.
  → **Mitigation**: Add unit tests in `StationBinaryCodecTest` asserting V1, V2, and V3 byte payloads deserialize with yaw defaulting to `0.0f` without exception.
- **[Risk]** Malformed or empty texture URLs cause exceptions during YAML parsing or profile creation.
  → **Mitigation**: Validate URL format and base64 structure gracefully, logging warnings instead of crashing plugin initialization.
- **[Risk]** Process displays with complex transforms could clip through blocks when rotated.
  → **Mitigation**: Restrict process display rotation to horizontal 2D plane `(tx, tz)` preserving elevation `ty`.
