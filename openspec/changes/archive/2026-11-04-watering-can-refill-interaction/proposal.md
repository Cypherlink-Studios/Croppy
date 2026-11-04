# Proposal: Watering Can Refill Interaction

## Why
When players right-click on water source blocks (such as single-block water holes or deep bodies of water) with a watering can, the watering can fails to refill and instead triggers an "empty watering can" warning. In Minecraft vanilla, custom items whose base materials are not buckets or bottles do not trigger client-side fluid raycasting; the client ignores the water block and targets either the solid block below/behind the water (producing `RIGHT_CLICK_BLOCK` on dirt/stone) or nothing within reach (producing `RIGHT_CLICK_AIR`). Because `WateringListener` currently ignores `RIGHT_CLICK_AIR` and checks `isWaterSource()` solely against `event.getClickedBlock()`, water blocks are never recognized, completely blocking watering can refilling.

## What Changes
- Add server-side fluid-aware raycasting (`player.rayTraceBlocks(5.0, FluidCollisionMode.ALWAYS)`) in `WateringListener` to detect targeted water source blocks, water cauldrons, and waterlogged blocks.
- Update `WateringListener` to accept both `Action.RIGHT_CLICK_BLOCK` and `Action.RIGHT_CLICK_AIR` for watering can interactions.
- Provide a fallback check against `clickedBlock.getRelative(event.getBlockFace())` and `clickedBlock` if raycasting misses, ensuring 1-block puddles where the player clicked the bottom or side block correctly identify the adjacent water.
- Preserve existing crop and farmland hydration mechanics when right-clicking non-water solid blocks with a non-empty watering can.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `watering-mechanics`: Update interaction scenarios for watering can refilling to specify server-side fluid interaction detection covering source blocks, deep water (`RIGHT_CLICK_AIR`), waterlogged blocks, and water cauldrons.

## Impact
- `croppy-paper`: `WateringListener.java` will be updated with server-side fluid raytracing and multi-action handling.
- Specs: `openspec/changes/watering-can-refill-interaction/specs/watering-mechanics/spec.md`.
- No breaking changes or public API contract modifications in `croppy-api` or `croppy-core`.
