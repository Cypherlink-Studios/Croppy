# Design

## Context

In Minecraft 1.20-1.21, the client-side rendering pipeline for `Display` entities (`BlockDisplay`, `ItemDisplay`, `TextDisplay`) applies orientation constraints in a strict sequence:
1. Entity world coordinates
2. **Billboard Constraint**: If `billboard` is not `FIXED` (e.g., `CENTER` or `VERTICAL`), the client rotates the entity's coordinate space to align with the camera/player line of sight.
3. **Transformation Matrix**: `Translation * LeftRotation * Scale * RightRotation` is multiplied within the billboard-oriented coordinate space.

In `croppy-paper`, `PacketEventsStationRenderer.java` uses `applyTransformMetadata()` to compute and append translation, scale, left-rotation, and billboard metadata. When `stationYaw` was integrated for dynamic placement, it was added directly into `applyTransformMetadata()`:
```java
float yawRad = (float) Math.toRadians(-stationYaw);
...
Quaternionf q = new Quaternionf().rotateY(yawRad).rotateXYZ(transform.pitch(), transform.yaw(), transform.roll());
```
Because `BASE_MODEL` uses `BillboardMode.FIXED`, applying `stationYaw` correctly aligns the 3D block/item model with the placed station. However, `TEXT_DISPLAY` uses `BillboardMode.CENTER`. Injecting `stationYaw` into `left_rotation` on an entity that is already aligned to the camera introduces an oblique rotation with roll/tilt in 3D perspective whenever the player views the block at a downward pitch angle.

## Goals / Non-Goals

**Goals:**
- Make `applyTransformMetadata()` in `PacketEventsStationRenderer` billboard-aware:
  - If `billboard == BillboardMode.FIXED`: Apply `stationYaw` to horizontal translation offsets and prepend `rotateY(yawRad)` to `left_rotation`.
  - If `billboard != BillboardMode.FIXED`: Ignore `stationYaw` (`effectiveYaw = 0.0f`), preserving camera-aligned, flat text holograms.
- Ensure all floating text holograms (`VisualTier.TEXT_DISPLAY`) remain completely level, horizontal, and legible from all viewing angles and station placement rotations.
- Provide unit tests validating left-rotation quaternions across various billboard modes and station placement yaws.

**Non-Goals:**
- Modify `DisplayLayerConfig`, `TextDisplayTemplate`, or any `croppy-api` records.
- Change the rendering of `BASE_MODEL` or `PROCESS_ITEM` when their billboard mode is `FIXED`.
- Alter YAML station definitions or serialization formats.

## Decisions

### Decision 1: Billboard-Aware Yaw Calculation in `applyTransformMetadata`
Inside `applyTransformMetadata()`, evaluate the billboard constraint:
```java
boolean isFixed = (billboard == null || billboard == BillboardMode.FIXED);
float effectiveYaw = isFixed ? stationYaw : 0.0f;
float yawRad = (float) Math.toRadians(-effectiveYaw);
float cos = (float) Math.cos(yawRad);
float sin = (float) Math.sin(yawRad);

double tx = transform.translationX();
double ty = transform.translationY();
double tz = transform.translationZ();
float rotatedTx = isFixed ? (float) (tx * cos - tz * sin) : (float) tx;
float rotatedTz = isFixed ? (float) (tx * sin + tz * cos) : (float) tz;

metadata.add(new EntityData(INDEX_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(rotatedTx, (float) ty, rotatedTz)));
metadata.add(new EntityData(INDEX_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(transform.scaleX(), transform.scaleY(), transform.scaleZ())));

Quaternionf q = new Quaternionf();
if (isFixed) {
    q.rotateY(yawRad);
}
q.rotateXYZ(transform.pitch(), transform.yaw(), transform.roll());
```
**Rationale**:
- Encapsulating this condition inside `applyTransformMetadata()` provides safety for all visual layers (base models, custom particle displays, text holograms). If any layer uses billboard tracking (`CENTER`, `VERTICAL`, `HORIZONTAL`), it will not suffer from coordinate space corruption.
- Keeping `isFixed` support ensures that if a server administrator or custom station explicitly specifies `billboard: FIXED` for a text display (e.g. text painted on the station block's face), it will still rotate with the block as desired.

**Alternatives Considered**:
- *Pass `0.0f` as `stationYaw` only in `updateTextDisplayMetadata()`*: While this would fix text displays, it leaves `applyTransformMetadata()` mathematically unsound if another billboarded layer is ever added. Handling it based on `billboard` in `applyTransformMetadata()` is cleaner and architecturally robust.

## Risks / Trade-offs

- **[Risk]** Non-zero base transform rotations (`pitch`, `yaw`, `roll`) on text displays.
  - **Mitigation**: Text display templates typically have `pitch = 0`, `yaw = 0`, `roll = 0`. If a template specifies local tilt, `q.rotateXYZ()` will still apply it relative to the billboard plane without interference from `stationYaw`.
