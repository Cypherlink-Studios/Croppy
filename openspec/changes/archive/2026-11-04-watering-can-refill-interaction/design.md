# Design: Watering Can Refill Interaction

## Context
See `proposal.md` for the motivation. In Paper 1.21.8, player interaction with fluid blocks using custom non-bucket items (such as watering cans built on `COPPER_INGOT`, `IRON_INGOT`, etc.) does not perform fluid collision on the client. Consequently, clients fire `PlayerInteractEvent` with `Action.RIGHT_CLICK_AIR` (if looking into open water) or `Action.RIGHT_CLICK_BLOCK` targeting the underlying or bounding solid block (e.g., dirt at the bottom of a 1-block puddle). `WateringListener` currently ignores `RIGHT_CLICK_AIR` and checks only `isWaterSource(clickedBlock)`.

## Goals / Non-Goals

**Goals:**
- Reliably detect when a player aims at a water source block (level 0), water cauldron, or waterlogged block within standard player reach (up to 5.0 blocks).
- Support refilling watering cans in both shallow bodies (1-block puddle where client targets dirt beneath) and deep bodies (rivers/oceans where client sends `RIGHT_CLICK_AIR`).
- Maintain seamless watering of farmland and crops when targeting solid non-water blocks.

**Non-Goals:**
- Draining water from the world or reducing water levels in cauldrons (refills remain non-destructive as specified in `watering-mechanics`).
- Refilling from flowing water (level > 0).

## Decisions

### Decision 1: Server-side Raytracing with `FluidCollisionMode.ALWAYS`
- **Choice**: Call `player.rayTraceBlocks(5.0, FluidCollisionMode.ALWAYS)` when a player right-clicks with a watering can in their main hand.
- **Rationale**: Paper's server-side raytracer evaluates voxel collision boxes against the player's eye location and view vector. `FluidCollisionMode.ALWAYS` detects water surfaces prior to any solid blocks located behind or below the fluid.
- **Alternatives Considered**:
  - *Only checking `clickedBlock.getRelative(event.getBlockFace())`*: Fails when the player clicks in deep water (where `RIGHT_CLICK_AIR` is fired and `clickedBlock` is null).
  - *`FluidCollisionMode.SOURCE_ONLY`*: Ignores flowing water during raycast, which could hit solid blocks behind flowing water. Using `ALWAYS` and filtering via `isWaterSource()` ensures that pointing at flowing water does not water background farmland inadvertently.

### Decision 2: Event Action Handling
- **Choice**: Accept both `Action.RIGHT_CLICK_BLOCK` and `Action.RIGHT_CLICK_AIR` in `WateringListener`.
- **Rationale**: When aiming at deep water, the client sends a `UseItem` packet resulting in `RIGHT_CLICK_AIR`. Refill checks must evaluate this event. If refill succeeds, cancel the event and return. If refill does not trigger and the action was `RIGHT_CLICK_AIR`, return immediately without attempting crop hydration.

### Decision 3: Adjacency Fallback
- **Choice**: If `rayTraceBlocks` returns null or a non-water block, but `event.getClickedBlock()` is present, check `isWaterSource(clickedBlock)` and `isWaterSource(clickedBlock.getRelative(event.getBlockFace()))`.
- **Rationale**: Provides defense-in-depth for edge cases where raytracing might graze a block boundary while the client clicked a block immediately enclosing the water.

## Risks / Trade-offs

- **[Risk] Multiple calls to `player.rayTraceBlocks` on every right-click** → *Mitigation*: Fast-path guards filter out off-hand clicks (`event.getHand() != EquipmentSlot.HAND`) and non-watering-can items (`!wateringCanService.isWateringCan(item)`) before executing any raycast. Raycasting is lightweight (raymarching at most 5 blocks).
- **[Risk] Refilling when aiming near crops that border water** → *Mitigation*: Crosshair accuracy dictates player intent. If crosshair intersects water, refill occurs; if crosshair intersects farmland or crop, hydration occurs.
