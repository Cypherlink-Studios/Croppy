# Proposal: Dynamic Station Direction, Skull Textures, and Item Stacking Fix

## Why

Currently, crafting stations are always placed facing a fixed zero-degree orientation regardless of player position, preventing natural placement against walls or workbenches. Furthermore, custom items defined as `minecraft:player_head` (such as the cooking pot) lack base64 and texture URL parsing, defaulting to Steve skins. Finally, a critical bug exists in station item retrieval where vanilla items are wrapped in a synthetic `ItemDefinition` with gray display names and Croppy PDC tags, altering their `ItemMeta` and permanently breaking stacking compatibility with identical vanilla items.

Addressing these issues now ensures visual polish, immersive 3D/block display placement from any angle, proper custom head rendering, and clean, non-destructive item handling in station inventories.

## What Changes

- **Station Dynamic Facing & Rotation Modes**: Capture player yaw on station placement (`BlockPlaceEvent`), orienting station models towards the placing player with support for `CARDINAL_4` (default), `CARDINAL_8`, and `FREE_YAW` rotation modes.
- **Station Spatial Orientation Persistence**: Add `yaw` orientation to `StationInstance` and `DefaultStationInstance`, incrementing `StationBinaryCodec` to version 4 with backward-compatible deserialization for legacy versions 1-3.
- **Virtual Display Orientation Alignment**: Update `PacketEventsStationRenderer` and `StationViewerTracker` to compose station base model rotations using the dynamic placement yaw, and transform horizontal offsets for child process displays.
- **Base64 & URL Texture Support for Player Heads**: Add optional `skullTexture` property to `ItemDefinition` and `YamlItemLoader` supporting base64 strings and Mojang texture URLs (`http://textures.minecraft.net/texture/...`), and configure `ItemFactory` to apply deterministic `PlayerProfile` data to `SkullMeta`.
- **Physical Head Block State Profile Synchronization**: Synchronize custom head textures to placed in-world `org.bukkit.block.Skull` blocks when a station's base block is a player head.
- **Clean Item Extraction & Vanilla Stacking Fix**: Centralize stored item resolution in `ItemFactory#createBukkitItem`, cleanly returning unmodified vanilla `ItemStack` instances for standard materials without synthetic display names or PDC tagging, resolving item stacking failures.

## Capabilities

### New Capabilities
*(None; this change enhances existing core capabilities.)*

### Modified Capabilities
- `crafting-stations`: Introduce dynamic placement facing and rotation modes (`CARDINAL_4`, `CARDINAL_8`, `FREE_YAW`), persist orientation in `StationBinaryCodec` V4, and ensure non-destructive item extraction from station storage.
- `station-virtual-displays`: Incorporate station placement yaw into PacketEvents virtual base model quaternion transformations and child layer spatial offsets.
- `item-registry`: Add `skullTexture` support (base64 and Mojang texture URLs) to item definitions, YAML loading, `SkullMeta` profile application, and placed skull block state updates.

## Impact

- **API & Core**:
  - `StationInstance` and `DefaultStationInstance` gain `getYaw()` and `setYaw(float)`.
  - `StationDefinition` gains optional `rotationMode` (defaults to `RotationMode.CARDINAL_4`).
  - `StationBinaryCodec` version bumped to 4 with V1-V3 backwards compatibility.
  - `ItemDefinition` gains optional `skullTexture` field and getter.
  - `YamlItemLoader` and `YamlStationLoader` parse new texture and rotation properties.
- **Paper Adapter**:
  - `StationBlockListener` records player placement yaw, applies rotation mode, and updates physical `Skull` block states if applicable.
  - `PacketEventsStationRenderer` applies dynamic yaw rotation to Item/Block displays.
  - `ItemFactory` centralizes `createBukkitItem(StoredItem)` and sets `PlayerProfile` on `SkullMeta`.
  - `PaperMenuRenderer`, `StationInteractListener`, `StationBlockListener`, and `StationGuiListener` delegate to centralized `ItemFactory#createBukkitItem`.
