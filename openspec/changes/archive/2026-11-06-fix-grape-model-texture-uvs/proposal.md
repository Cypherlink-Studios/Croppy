# Proposal

## Why

In Minecraft Java Edition, all JSON model faces (`models/block/*.json` and `models/item/*.json`) operate in a normalized UV coordinate space of `0.0` to `16.0`, regardless of the pixel resolution of the underlying PNG texture. The 3D grape crop developmental stage models (`grape_plant_stage_0.json` through `grape_plant_stage_5.json`) were authored with `"uv": [0, 0, 16, 32]`, causing Minecraft's texture stitcher to sample at 200% of the sprite's height ($V = 32.0 / 16.0 = 2.0$) on the atlas.

This causes two severe visual bugs in-game:
1. The lower half of the 2-block plant quads ($Y = 0 \dots 16$) bleeds into whatever neighboring sprite is packed directly below on the texture atlas (rendering an open book texture from `minecraft:item/knowledge_book` on farmland).
2. The entire 32-pixel height of the grape texture is compressed into the upper half ($Y = 16 \dots 32$), causing early stage sprouts (e.g. `stage_0`) to hover in mid-air one block above the soil.

Fixing the model face UV definitions to normalized `[0, 0, 16, 16]` will map the 16x32px texture seamlessly across the full 32-unit height of the quad, eliminating atlas texture bleeding and grounding early growth stages directly onto the soil.

## What Changes

- **Update Grape Stage Models UV Mapping**:
  - In `resourcepack/assets/croppy/models/block/plant/grape/grape_plant_stage_0.json` through `grape_plant_stage_5.json`, update the `north` and `south` element faces from `"uv": [0, 0, 16, 32]` to `"uv": [0, 0, 16, 16]`.
  - Remove extraneous non-standard `"texture_size": [16, 32]` metadata tags in the block model JSONs to conform to canonical Java Edition model format specifications.
- **Audit and Regression Gate**:
  - Audit all companion resource pack block and item model JSON files to guarantee no UV coordinates exceed `16.0`.
  - Verify that `CropResourcesIntegrityTest` and resource pack validation suites continue passing cleanly.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `companion-resource-pack`: Add model UV coordinate normalization requirements ensuring all custom block and item models stay strictly within `[0.0, 16.0]` bounds on both axes to prevent texture atlas bleeding.

## Impact

- **Affected Assets**:
  - `resourcepack/assets/croppy/models/block/plant/grape/grape_plant_stage_0.json`
  - `resourcepack/assets/croppy/models/block/plant/grape/grape_plant_stage_1.json`
  - `resourcepack/assets/croppy/models/block/plant/grape/grape_plant_stage_2.json`
  - `resourcepack/assets/croppy/models/block/plant/grape/grape_plant_stage_3.json`
  - `resourcepack/assets/croppy/models/block/plant/grape/grape_plant_stage_4.json`
  - `resourcepack/assets/croppy/models/block/plant/grape/grape_plant_stage_5.json`
- **APIs & Dependencies**: Zero breaking API changes in `croppy-api`, `croppy-core`, or `croppy-paper`. No external dependencies modified.
- **Client Visuals**: Eliminates atlas texture bleeding (open book visual glitch) and corrects vertical positioning of grape developmental stages in the world.
