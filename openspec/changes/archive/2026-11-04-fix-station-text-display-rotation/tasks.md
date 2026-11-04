# Tasks

## 1. Renderer Transformation Fix

- [x] 1.1 Update `applyTransformMetadata()` in `PacketEventsStationRenderer.java` to evaluate `billboard == BillboardMode.FIXED`: apply `stationYaw` to horizontal translation offsets and `left_rotation` only when `FIXED`, and keep effective yaw as `0.0f` for dynamic billboards (`CENTER`, `VERTICAL`, `HORIZONTAL`).
- [x] 1.2 Inspect `spawnTextDisplay` and `updateTextDisplayMetadata` to ensure `template.billboard()` is properly passed and handled without unintended coordinate mutations.

## 2. Unit Testing & Verification

- [x] 2.1 Add unit tests in `PacketEventsStationRendererTest` validating that `applyTransformMetadata()` produces an unskewed rotation quaternion (identity or local transform only) when `billboard` is `CENTER` or `VERTICAL` for non-zero station yaws (e.g., 45.0°, 90.0°, 180.0°, 270.0°).
- [x] 2.2 Add unit tests validating that `applyTransformMetadata()` preserves horizontal offset rotation and Y-axis rotation when `billboard` is `BillboardMode.FIXED` with non-zero station yaw.
- [x] 2.3 Run `./gradlew.bat test` across all modules to confirm 100% test pass rate with zero regressions.
