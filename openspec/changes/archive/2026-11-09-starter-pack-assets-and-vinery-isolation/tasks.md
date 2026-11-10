# Tasks

## 1. Grape Disambiguation and Item Definitions

- [x] 1.1 Create `croppy-paper/src/main/resources/items/ingredients/green_grapes.yml` with `id: "croppy:green_grapes"`, `item-model: "croppy:green_grapes"`, and verify file existence and YAML validity
- [x] 1.2 Create `croppy-paper/src/main/resources/items/ingredients/purple_grapes.yml` with `id: "croppy:purple_grapes"`, `item-model: "croppy:purple_grapes"`, while maintaining `grapes.yml` compatibility, and verify both definitions load in `ItemRegistry`
- [x] 1.3 Update or add unit tests in `croppy-paper` to verify `croppy:green_grapes` and `croppy:purple_grapes` parse cleanly and resolve their item models

## 2. Vinery Asset Isolation

- [x] 2.1 Create `.internal/resourcepack/vinery/` directory structure mirroring `assets/croppy/{items,models,textures}/`
- [x] 2.2 Relocate grape plant block models (`models/block/plant/grape/*.json`), textures (`textures/block/plant/grape/*.png`), and item definitions (`items/plant/grape/*.json`, `items/grape_stage_*.json`) to `.internal/resourcepack/vinery/`
- [x] 2.3 Relocate grape item assets (`items/grapes.json`, `items/purple_grapes.json`, `items/green_grapes.json`, `models/item/*grapes*.json`, `textures/item/*grapes*.png`) to `.internal/resourcepack/vinery/`
- [x] 2.4 Verify that `resourcepack/assets/croppy/` is free of grape/vinery files and only contains workstations, fried egg, and base plant stages

## 3. Blockbench Prompt Authoring for Missing Assets

- [x] 3.1 Author detailed Blockbench modeling prompt for the 6 watering cans (`wooden_can`, `copper_can`, `iron_can`, `golden_can`, `diamond_can`, `netherite_can`) with palette ramps and element geometries
- [x] 3.2 Author detailed Blockbench prompt for the 3 fertilizers (`growth_fertilizer`, `hydrating_fertilizer`, `quality_fertilizer`)
- [x] 3.3 Author detailed Blockbench prompt for the 2 tools (`inspection_tool`, `thermometer`)
- [x] 3.4 Author detailed Blockbench prompt for the 6 seeds (`carrot_seeds`, `grape_seeds`, `potato_seeds`, `rice_seeds`, `tomato_seeds`, `wheat_seeds`)
- [x] 3.5 Author detailed Blockbench prompt for the 4 harvests and dishes (`tomato`, `rice_panicle`, `tomato_soup`, `vegetable_stew`)
- [x] 3.6 Author detailed Blockbench prompt for the 2 misc items (`burnt_food`, `recipe_book`)

## 4. Resource Pack Integration and Item Wiring

- [x] 4.1 Generate and place textures under `resourcepack/assets/croppy/textures/item/` for all 23 items
- [x] 4.2 Generate and place item models under `resourcepack/assets/croppy/models/item/` for all 23 items
- [x] 4.3 Generate and place modern 1.21.8 item definition JSONs under `resourcepack/assets/croppy/items/<item>.json` with `minecraft:model` dispatch
- [x] 4.4 Fix misnamed `inspector.json` by creating `assets/croppy/items/inspection_tool.json`
- [x] 4.5 Update all YAML files in `croppy-paper/src/main/resources/items/**/*.yml` to declare explicit `item-model: "croppy:<item>"`
- [x] 4.6 Verify strict JSON compliance across all generated `.json` files in `resourcepack/` (no trailing commas, valid lowercase resource locations)

## 5. Verification and Quality Assurance

- [x] 5.1 Run `./gradlew test` across all modules and confirm 0 test failures
- [x] 5.2 Validate that every registered item in `ItemCatalogController` resolves a valid custom item stack without missing texture fallbacks
