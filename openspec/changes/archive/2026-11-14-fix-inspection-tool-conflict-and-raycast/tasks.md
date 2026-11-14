# Tasks: Fix Inspection Tool Navigation Wand Conflict and Raycast Targeting

## 1. Item Definition & Recipe Update

- [x] 1.1 Update `items/tools/inspector.yml` material to `minecraft:stick` and verify item definition consistency in `BundledCraftingRecipesIntegrityTest`.
- [x] 1.2 Update documentation in `docs/en/configuration.md` and `docs/es/configuration.md` reflecting `minecraft:stick` as the base material for `croppy:inspection_tool`.

## 2. InspectionToolListener Refactor & Raycasting

- [x] 2.1 Refactor `InspectionToolListener.java` to inject `CropTargetResolver`, update handler to `@EventHandler(priority = EventPriority.LOWEST)`, and handle both `Action.RIGHT_CLICK_BLOCK` and `Action.RIGHT_CLICK_AIR` with immediate event cancellation.
- [x] 2.2 Update `CroppyPlugin.java` to inject `CropTargetResolver` into `InspectionToolListener`.
- [x] 2.3 Integrate `CropTargetResolver.resolveTarget(player)` in `InspectionToolListener`, dispatching the crop inspection card or localized `command.inspect.no-crop-found` message.

## 3. Testing & Verification

- [x] 3.1 Create unit tests in `InspectionToolListenerTest.java` verifying cancellation of `RIGHT_CLICK_BLOCK` and `RIGHT_CLICK_AIR`, invocation of `CropTargetResolver`, and accurate target resolution.
- [x] 3.2 Run `./gradlew.bat test` to verify that all test suites pass with 0 failures.
