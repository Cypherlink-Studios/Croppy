# Design: Starter Pack Assets Completion & Vinery Isolation

## Context

Croppy bundled resources include 28 base items, of which 23 currently lack custom models or textures. Concurrently, the viticulture/grape mechanics bundled in the base pack introduce specific thematic assumptions (Vinery/Let's Do Vinery tropes) that deviate from the neutral baseline target.

This design provides the architectural blueprint to:
1. Create the separate `green_grapes` item and disambiguate grape varieties.
2. Isolate all grape/vinery assets to `.internal/resourcepack/` to safeguard them for potential future addon extraction while keeping the core companion pack neutral.
3. Provide rigorous, structured prompts for an autonomous agent using the `blockbench` MCP server to generate clean, Minecraft 1.21.8 compliant textures and models for all 23 missing items.
4. Integrate the new assets and wire all item YAML definitions.

## Goals / Non-Goals

**Goals:**
- Provide complete visual coverage (textures, JSON models, and item definitions) for all 28 starter pack items.
- Disambiguate `croppy:green_grapes` and `croppy:purple_grapes` while preserving backwards compatibility for `croppy:grapes`.
- Isolate all grape plant and grape item assets into `.internal/resourcepack/vinery/`.
- Provide machine-executable Blockbench prompts that specify palette constraints, element dimensions, UV mappings, and file paths.
- Ensure 100% compliance with Minecraft Java 1.21.8 strict JSON parsing (`pack_format: 64`).

**Non-Goals:**
- Removing the `crops/grapes.yml` crop configuration from the Java plugin in this change (only the asset distribution in the companion pack is isolated).
- Modifying the core API or storage structures (`ItemDefinition`, `ItemRegistry`, codecs).
- Altering existing workstations (`cooking_pot`, `skillet`, `stove`) or `fried_egg` which already have full custom assets.

## Decisions

### 1. Grape Disambiguation & Compatibility Strategy
- **Decision**: Introduce `items/ingredients/green_grapes.yml` with key `croppy:green_grapes`. In parallel, create `purple_grapes.yml` (`croppy:purple_grapes`) while maintaining `croppy:grapes` as an alias or existing item targeting `purple_grapes` to avoid breaking `crops/grapes.yml` yields and existing unit tests.
- **Alternatives Considered**:
  - *Hard-renaming `croppy:grapes` to `croppy:purple_grapes`*: Rejected because it would immediately break tests (`CropResourcesIntegrityTest`, `ItemFactoryTest`) and existing saved item states.

### 2. Vinery Isolation in `.internal/resourcepack/`
- **Decision**: Create `.internal/resourcepack/vinery/` mirroring the standard assets tree (`assets/croppy/{items,models,textures}/...`) and move all grape plant stages (`grape_plant_stage_0` to `5`) and grape fruit assets there.
- **Alternatives Considered**:
  - *Deleting grape assets entirely*: Rejected because significant modeling and texturing work was invested in the 6 growth stages of grapes, which will be repurposed into an independent winery/viticulture addon in a future phase.

### 3. Blockbench Prompt Architecture (MCP Tools)
- **Decision**: Divide missing assets into 6 cohesive domain prompts. Each prompt specifies exact element coordinates, 16x16 pixel-art canvas specifications, color palettes, and strict JSON export paths via MCP tools (`blockbench:new_project`, `blockbench:create_texture`, `blockbench:paint_texture`, `blockbench:export_project`, `blockbench:save_project`).
- **Texture Standard**: 16x16 pixel art with standard Minecraft shading rules (top-left highlight, bottom-right shadow, limited 4-6 tone ramps).
- **Model Standard**: Parent `minecraft:item/generated` with `layer0` texture mapping for flat items; minimal 3D cubes for watering cans if 3D is preferred, or stylized flat sprites matching vanilla tools.

### 4. Aligning Misnamed and Missing Definitions
- **Decision**:
  - Rename `assets/croppy/items/inspector.json` to `inspection_tool.json` to match `items/tools/inspector.yml` (`item-model: "croppy:inspection_tool"`).
  - Add missing `assets/croppy/items/<type>_fertilizer.json` definitions.
  - Wire `items/tools/thermometer.yml` with `item-model: "croppy:thermometer"`.

## Risks / Trade-offs

- **[Risk] Resource pack missing grape assets while `crops/grapes.yml` still references them** → *Mitigation*: In `crops/grapes.yml`, if grape assets are moved to `.internal`, the block displays in test environments would look for those models. However, tests in Paper verify definition loading rather than client-side rendering. For servers wishing to use grapes, an overlay or external pack will be provided.
- **[Risk] Incomplete texture generation by autonomous agent** → *Mitigation*: Provide explicit hex color palettes and pixel grids in the prompts, allowing iterative validation via `blockbench:inspect_model` and `blockbench:screenshot`.
- **[Risk] Strict JSON compliance failures in 1.21.8** → *Mitigation*: Mandate strict JSON output (no trailing commas, quoted keys, valid lowercase asset paths) verified via automated tests.
