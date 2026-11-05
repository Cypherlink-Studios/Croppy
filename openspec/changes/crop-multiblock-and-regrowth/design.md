# Design

## Context

See `proposal.md` for motivation. Currently, `CropManager` (`croppy-core`) tracks crop instances using a 1-to-1 mapping of `BlockPosition` to `CropInstance`. When harvested, `CropBreakListener` and `VirtualInteractionPacketListener` (`croppy-paper`) assume destructive unregistration and deletion from persistence. Additionally, player interactions with crops are hardcoded in listeners, unlike crafting stations which leverage the declarative `InteractionRouter` with Pratt-AST condition evaluation.

This technical design addresses how to implement multi-block vertical footprints, semantic stage regrowth, and generalized declarative crop interactions across all three modules (`croppy-api`, `croppy-core`, `croppy-paper`).

## Goals / Non-Goals

**Goals:**
- Provide pure Java 21 domain contracts in `croppy-api` for multi-block vertical height and semantic stage regrowth without platform coupling.
- Implement thread-safe secondary spatial occupancy indexing (`occupancyMap`) in `CropManager` to resolve any spanned block position to the root crop instance in O(1) time.
- Implement fail-fast validation in `YamlSpeciesLoader` ensuring `height >= 1` and verifying that `target_stage` IDs exist and do not point to the terminal mature stage.
- Generalize `InteractionDefinition` and `InteractionRouter` to support `target_crop` keys, `SNEAK_LEFT_CLICK` trigger mode, and `HARVEST_CROP` / `DISMANTLE_CROP` actions.
- Provide declarative sample configs for Trellis Grapes (`croppy:grapes`) and Paddy Rice (`croppy:rice`).

**Non-Goals:**
- Horizontal 2D multi-block crops (e.g. 2x2 or 3x3 giant pumpkins); vertical height (`height: N`) fulfills trellis crops and emergent paddy rice.
- Modifying underlying binary storage format (`CropBinaryCodec`); regrowth stage indices and progress reset seamlessly fit into the existing V3 binary schema.
- Genetic cross-breeding or crop mutation engines (out of scope for this change).

## Decisions

### Decision 1: Secondary `occupancyMap` Indexing vs Duplicating Crop Instances
- **Choice**: `CropManager` maintains `primaryCrops: Map<BlockPosition, CropInstance>` for the root position and `occupancyMap: Map<BlockPosition, BlockPosition>` linking every secondary spanned position (e.g. `(x, y+1, z)`) back to the root `BlockPosition`.
- **Rationale**: Keeps database persistence (SQLite and PDC) strictly 1 record per crop, avoids entity duplication, and guarantees O(1) lookup whether a query hits `(x, y, z)` or `(x, y+1, z)`.
- **Alternatives Considered**: Creating linked dummy/child `CropInstance` objects at each secondary block. Rejected because it introduces dual-lifecycle synchronization bugs, chunk unload races, and database schema bloat.

### Decision 2: Semantic Stage ID (`target_stage: "flowering_vine"`) vs Numerical Index (`2`)
- **Choice**: The regrowth configuration in YAML specifies the string stage ID. `Species` resolves it to a stage index via `getStageIndex(String)`.
- **Rationale**: Eliminates silent bugs when content creators insert or reorder growth stages in YAML. Allows fail-fast validation during plugin startup to catch typos immediately.
- **Alternatives Considered**: Raw integer stage index. Rejected due to fragility and lack of semantic clarity.

### Decision 3: Unifying Crop Interactions into `InteractionRouter`
- **Choice**: Generalize `InteractionDefinition` with an optional `targetCrop` field and evaluate crop interactions using the existing `InteractionRouter`.
- **Rationale**: Avoids duplicating interaction logic. Reuses Pratt-AST expression evaluation (`crop.stage_id`, `crop.is_mature`), priority resolution, item matching, and audio/particle pipelines already battle-tested for stations.
- **Alternatives Considered**: Hardcoded `CropInteractListener` with procedural if-else statements. Rejected because it prevents server admins from customizing tools, tools durability damage, or sneak-dismantle mechanics in YAML.

### Decision 4: Single Virtual `Interaction` Entity with Dynamic Height
- **Choice**: A single `EntityTypes.INTERACTION` entity placed at the root position with `hitboxHeight` adjusted to span the full vertical footprint (e.g. `height: 2.0`).
- **Rationale**: Covers client raycasts continuously from ground level to the top vine with 1 entity ID, minimizing packet transmission and client-side entity tracking overhead.
- **Alternatives Considered**: Spawning multiple stacked interaction entities. Rejected due to increased packet count and potential hit registration conflicts.

## Risks / Trade-offs

- **[Client Ghost Blocks in Y+1 Space]** → When a player right-clicks aiming at a block adjacent to Y+1, the client may briefly predict block placement before server rollback.
  *Mitigation*: `CropPlantListener.onBlockCollisionSafeguard` intercepts `BlockPlaceEvent` against `cropManager.getCropAt(placedPos)` and cancels immediately, forcing a server-to-client block update.
- **[Farmland Trampling below Multi-Block Crops]** → Players or mobs jumping on the crop could trample farmland into dirt.
  *Mitigation*: Intercept `PlayerInteractEvent` with `Action.PHYSICAL` on farmland if `cropManager.getCropAt(pos.above())` is present and cancel trample.
- **[Infinite Instant Harvest Loop]** → A misconfigured YAML setting `target_stage` to the mature stage would allow infinite harvests in one tick.
  *Mitigation*: `YamlSpeciesLoader` strictly enforces that `target_stage` is strictly an earlier stage (`index < totalStages - 1`).

## Migration Plan

- Backward-compatible: existing monocarpic crops (wheat, carrot, potato, tomato) omit `regrowth` and `spatial.height` (defaulting to `height: 1` and `RegrowthPolicy.None`), functioning identically to previous behavior.
- Test verification: Comprehensive JUnit 5 unit tests in `croppy-core` verifying `occupancyMap` resolution, multi-block safeguards, semantic regrowth stage reset, and `InteractionRouter` crop matching.
