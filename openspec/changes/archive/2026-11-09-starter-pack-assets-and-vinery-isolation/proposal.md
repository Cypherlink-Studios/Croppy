# Proposal: Starter Pack Assets Completion & Vinery Isolation

## Why

Croppy is designed to provide an extensible, modular, and theme-neutral simulation engine for agriculture, processing, and cooking on Minecraft Paper servers. However, an audit of the bundled starter pack content revealed that 23 of the 28 base items lack custom textures or models, falling back to vanilla hoes, sticks, and raw food, or referencing missing resource pack files.

Additionally, the existing grape assets originate from theme-specific viticulture/vinery mechanics that conflict with the clean, neutral baseline desired for the core plugin. Orphaned assets (such as `green_grapes.json` and its textures) currently exist in the companion resource pack without proper item registration, while `croppy:grapes` lacks clean separation between grape varieties.

Isolating vinery assets to `.internal/resourcepack/` and creating complete, vanilla-faithful models and textures for all remaining starter-pack items ensures a polished, professional out-of-the-box experience while preserving architectural and aesthetic neutrality.

## What Changes

- **Grape Disambiguation & Item Creation**:
  - Register `croppy:green_grapes` with its dedicated item definition, display name, lore, and native model binding to `croppy:item/green_grapes`.
  - Formalize `croppy:purple_grapes` while preserving `croppy:grapes` compatibility for existing crops, recipes, and tests.
- **Vinery Asset Isolation**:
  - Relocate all grape plant stage models (`plant/grape/stage_{0..5}`), textures, block models, and grape item definitions from `resourcepack/assets/croppy/` to `.internal/resourcepack/vinery/` so the core companion resource pack remains strictly neutral.
- **Blockbench Modeling Briefs & Prompts**:
  - Structure comprehensive, reusable prompt specifications for generating 3D models and 16x16 pixel-art textures using the `blockbench` MCP server across the 6 missing item categories:
    1. Watering Cans (`wooden`, `copper`, `iron`, `golden`, `diamond`, `netherite`).
    2. Fertilizers (`growth_fertilizer`, `hydrating_fertilizer`, `quality_fertilizer`).
    3. Tools (`inspection_tool`, `thermometer`).
    4. Seeds (`carrot_seeds`, `grape_seeds`, `potato_seeds`, `rice_seeds`, `tomato_seeds`, `wheat_seeds`).
    5. Harvests & Prepared Foods (`tomato`, `rice_panicle`, `tomato_soup`, `vegetable_stew`).
    6. Miscellaneous (`burnt_food`, `recipe_book`).
- **Resource Pack Integration & Model Binding**:
  - Add native item model JSONs (`assets/croppy/items/<item>.json`), item models (`assets/croppy/models/item/<item>.json`), and textures (`assets/croppy/textures/item/<item>.png`) to the main resource pack.
  - Update all YAML files in `croppy-paper/src/main/resources/items/**/*.yml` to bind `item-model: "croppy:<item>"`.
  - Fix misaligned tool models (e.g. `inspection_tool` vs `inspector.json`).

## Capabilities

### Modified Capabilities
- `companion-resource-pack`: Update item model definitions to isolate non-neutral grape assets, provide complete item model definitions for all base starter-pack items, and resolve model misalignments.
- `item-registry`: Require all bundled starter pack items to declare and resolve valid native `item-model` keys without relying on vanilla hoe/item fallbacks.

## Impact

- **Configuration & YAMLs**: `croppy-paper/src/main/resources/items/**/*.yml` updated with explicit `item-model` bindings; new `green_grapes.yml` registered.
- **Resource Pack**: `resourcepack/assets/croppy/` purged of viticulture assets (relocated to `.internal/resourcepack/vinery/`); populated with complete textures and models for all starter pack items.
- **Automated Tests**: Unit test suites (`ItemFactoryTest`, `CropResourcesIntegrityTest`) remain fully passing with zero regressions.
