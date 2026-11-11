# Tasks

## 1. Crop Line-of-Sight Target Resolver

- [x] 1.1 Implement `CropTargetResolver` in `dev.darkblade.croppy.platform.paper.util` (or `service`) computing line-of-sight ray intersections against nearby active `VirtualBlockDisplay` AABB hitboxes (`hitboxWidth`, `hitboxHeight`), solid block occlusions, physical crop blocks, and supporting farmland (`y + 1`).
- [x] 1.2 Create `CropTargetResolverTest` unit test suite covering virtual entity hitbox hits, occluded entities behind solid walls, physical block hits, farmland fallback, and out-of-reach misses.

## 2. Virtual Interaction Inspection Tool Support

- [x] 2.1 Update `VirtualInteractionPacketListener` to check `CropInspectorService.isInspectionTool(itemInHand, plugin)` on `INTERACT` actions, invoking `CropInspectorService.inspect()` and returning immediately to prevent accidental harvesting or destruction.
- [x] 2.2 Add unit test in `VirtualInteractionPacketListenerTest` verifying that right-clicking a virtual interaction entity with `croppy:inspection_tool` triggers inspection instead of default harvest or damage.

## 3. Command Surface Unification

- [x] 3.1 Refactor `/croppy inspect` in `CroppyCommands` to resolve targets using `CropTargetResolver`.
- [x] 3.2 Refactor `/croppy water` and `/croppy fertilize <fertilizer>` in `CroppyCommands` to resolve targets using `CropTargetResolver`.
- [x] 3.3 Refactor `/croppy setstage` and `/croppy setprogress` in `CroppyCommands` to resolve targets using `CropTargetResolver`.
- [x] 3.4 Add/update unit tests in `CroppyCommandsTest` validating line-of-sight target resolution across all 5 crop commands.

## 4. Verification & Quality Assurance Gate

- [x] 4.1 Run full Gradle test suite (`.\gradlew.bat test`) and verify 100% test pass with 0 failures across `croppy-api`, `croppy-core`, and `croppy-paper`.
