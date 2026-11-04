# Proposal

## Why

When dynamic placement and rotation modes (`CARDINAL_4`, `CARDINAL_8`, `FREE_YAW`) were implemented for crafting stations in commit `24bec164ec49`, `stationYaw` was indiscriminately passed to `applyTransformMetadata()` across all visual tiers, including floating status text holograms (`VisualTier.TEXT_DISPLAY`). Because station text displays are configured with camera-tracking billboards (`billboard: CENTER`), Minecraft's client-side rendering applies the billboard matrix to orient the entity toward the camera *before* multiplying the entity's `left_rotation` and translation.

Consequently, injecting the station's placement yaw into `left_rotation` rotates the text inside the camera/screen coordinate frame. Whenever players view a station at a pitch angle (e.g., looking down at a block on the ground), this rotation introduces severe roll and perspective skew, causing the text hologram to appear diagonally tilted and distorted ("achuecado"). Station text holograms must always remain level, horizontal, and legible to players, completely invariant to the station block's placement orientation.

## What Changes

- Modify `applyTransformMetadata()` in `PacketEventsStationRenderer` to account for `billboard` orientation mode:
  - When `billboard == BillboardMode.FIXED`: Apply `stationYaw` to horizontal translation offsets and prepend `rotateY(yawRad)` to `left_rotation` so 3D base models align with the placed station block.
  - When `billboard != BillboardMode.FIXED` (such as `CENTER`, `VERTICAL`, or `HORIZONTAL`): Treat effective yaw as `0.0f` so that `left_rotation` and translation offsets are not skewed by the station's block placement yaw, keeping holographic text level and camera-facing.
- Add comprehensive unit test coverage in `croppy-paper` validating that `PacketEventsStationRenderer` generates identity rotation quaternions for billboard `CENTER` / `VERTICAL` text displays regardless of station placement yaw (e.g., 45°, 90°, 180°, 270°).

## Capabilities

### New Capabilities

<!-- None -->

### Modified Capabilities

- `station-virtual-displays`: Update the specification for `Templated MiniMessage Text Holograms` to explicitly require that camera-tracking billboard text displays remain level and invariant to the station's placement yaw.

## Impact

- `croppy-paper`: `dev.darkblade.croppy.platform.paper.station.display.PacketEventsStationRenderer` and test suite `PacketEventsStationRendererTest`.
- No breaking API changes or binary persistence changes.
