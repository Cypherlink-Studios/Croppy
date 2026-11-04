# Tasks

## 1. WateringListener Refill Interaction Implementation

- [x] 1.1 Update `WateringListener.java` event guards to accept both `Action.RIGHT_CLICK_BLOCK` and `Action.RIGHT_CLICK_AIR` for main-hand watering can interactions
- [x] 1.2 Implement server-side fluid raytracing with `player.rayTraceBlocks(5.0, FluidCollisionMode.ALWAYS)` in `WateringListener.java`, resolving targeted water source blocks (level 0), water cauldrons, or waterlogged blocks
- [x] 1.3 Implement fallback detection for `clickedBlock.getRelative(event.getBlockFace())` and `clickedBlock` when raycasting does not directly yield a fluid source, ensuring single-block water holes refill correctly
- [x] 1.4 Guard watering and empty warning execution so that `RIGHT_CLICK_AIR` without water returns cleanly without sending empty warnings or attempting crop hydration

## 2. Testing and Quality Assurance

- [x] 2.1 Add unit tests in `croppy-paper` verifying `WateringListener` refill and interaction branches
- [x] 2.2 Execute `./gradlew test` and verify all tests pass with 0 failures
