# Design

## Context

See `proposal.md` for problem motivation and background.
Croppy's grape crops are multi-block 2-block tall climbing plants rendered via client-side virtual `ItemDisplay` entities. The developmental stages (`stage_0` through `stage_5`) use 16x32 pixel PNG textures mapped onto two intersecting 32-unit tall planar quads (`from: [-1, 0, 8]` to `[17, 32, 8]` with ±45° Y-axis rotation).

In Minecraft Java Edition block/item model format (`models/block/*.json`), face UV coordinates do not represent raw pixel dimensions; they are normalized where `0.0` is the sprite top/left and `16.0` is the sprite bottom/right (100% of the allocated atlas rectangle). Authored `"uv": [0, 0, 16, 32]` values caused the bottom half of the quads to oversample into neighboring atlas sprites.

## Goals / Non-Goals

**Goals:**
- Eliminate texture atlas oversampling and adjacent sprite bleeding across all 6 grape growth stages.
- Ground early growth stages (e.g. `stage_0` sprout) directly onto farmland without vertical floating.
- Maintain 1:1 texel-to-unit density across the 32-unit tall element.
- Clean up non-standard Blockbench metadata (`texture_size`) from Java block model JSONs.
- Audit all resource pack model JSONs to prevent UV overflow regressions.

**Non-Goals:**
- Modifying pixel art textures (textures remain 16x32px).
- Splitting the plant into separate 16x16 top and bottom textures.
- Changing server-side Java logic in `croppy-paper` or `croppy-core`.

## Decisions

### 1. Normalized `[0, 0, 16, 16]` UV Mapping for 32-Unit Quads
- **Choice**: In `grape_plant_stage_0.json` through `grape_plant_stage_5.json`, configure the `north` and `south` faces with `"uv": [0, 0, 16, 16]`.
- **Rationale**:
  - In Minecraft's atlas stitcher, rectangular sprites (16x32) have $V_{min}$ at row 0 and $V_{max}$ at row 31.
  - The atlas UV conversion formula is:
    $$V_{atlas} = V_{min} + (V_{max} - V_{min}) \times \frac{v}{16.0}$$
  - Setting $v = 16.0$ maps exactly to $V_{max}$ (100% of the 32px height).
  - Because the element height is 32 units ($Y = 0 \dots 32$), mapping the 32-pixel height across 32 units yields exactly 1 texel per unit (crisp 1:1 pixel rendering).
  - Eliminates the 100%–200% oversampling range that sampled the neighboring book texture.
- **Alternatives Considered**:
  - *Split into two 16x16 textures (e.g. `grape_bottom.png` and `grape_top.png`)*: Rejected because PacketEvents virtual `ItemDisplay` renders a single 2-block tall model cleanly without needing separate top/bottom block items or complex multi-entity synchronization.

### 2. Remove Non-Standard `"texture_size"` Property
- **Choice**: Strip `"texture_size": [16, 32]` from the root of all grape block model JSONs.
- **Rationale**: `"texture_size"` is a Blockbench / Bedrock entity format artifact. Vanilla Java Edition's `BlockModel` parser ignores it, but third-party strict parsers and linters can flag it as extraneous schema noise.
- **Alternatives Considered**: Keeping it for Blockbench re-import. *Rejected* because Blockbench can re-infer texture dimensions directly from the imported PNG.

### 3. Comprehensive UV Audit Gate
- **Choice**: Scan all model files in `resourcepack/assets/croppy/models/` to ensure no face UV coordinate exceeds `16.0`.
- **Rationale**: Prevents accidental atlas bleeding in other existing or future 3D models (such as cooking stations or future multi-block crops).

## Risks / Trade-offs

- **[Risk] Texture flipping or inverted orientation**
  - *Mitigation*: Ensure `v1 = 0` (top of texture, $Y = 32$) and `v2 = 16` (bottom of texture / roots, $Y = 0$) matches the vertical quad vertices. In Minecraft element quads, $v1$ binds to the maximum Y vertex and $v2$ binds to the minimum Y vertex, which maps the roots cleanly to the soil.
- **[Risk] Client resource pack caching**
  - *Mitigation*: After updating pack files, players reload textures with `F3 + T` (or re-apply server resource pack).

## Migration & Verification Plan

1. Edit the 6 model files: `grape_plant_stage_0.json` to `grape_plant_stage_5.json`.
2. Run automated UV boundary audit script across `resourcepack/`.
3. Run `./gradlew test` to ensure `CropResourcesIntegrityTest` and build integrity remain green.
