# Tasks

## 1. Watering Can & Fertilizer Resource Neutralization

- [x] 1.1 Replace Stardew watering cans (`rusty`, `silver`, `iridium`) with native Minecraft materials (`wooden_can`, `copper_can`, `iron_can`, `golden_can`, `diamond_can`, `netherite_can`) in `croppy-paper/src/main/resources/items/watering_cans/`, configuring vanilla-aligned capacities and Standard English names and lores
- [x] 1.2 Rename fertilizers (`speed_gro.yml` -> `growth_fertilizer.yml`, `retaining_soil.yml` -> `hydrating_fertilizer.yml`, `quality_fertilizer.yml`) in `croppy-paper/src/main/resources/items/fertilizers/`, updating their IDs and translating names and lores to Standard English
- [x] 1.3 Translate `crops/carrot.yml` ("Carrot") and `items/tools/inspector.yml` ("Crop Inspector") to Standard English and purge mystical fantasy lore across all tools and item definitions

## 2. Resource Pack Cleanup & Model Definitions

- [x] 2.1 Delete orphan/modern asset `resourcepack/assets/croppy/textures/item/hamburger.png`
- [x] 2.2 Synchronize watering can item model definitions in `resourcepack/assets/croppy/items/`, removing deprecated ones (`rusty_can.json`, `silver_can.json`, `iridium_can.json`) and adding definitions for `wooden_can.json`, `iron_can.json`, `diamond_can.json`, and `netherite_can.json`
- [x] 2.3 Synchronize grape item model mapping between `crops/grapes.yml`, `items/ingredients/grapes.yml`, and companion resource pack item definitions
- [x] 2.4 Scaffold standardized stage item model definitions and update `crops/tomato.yml` and `crops/rice.yml` stage models (`croppy:plant/tomato/stage_*`, `croppy:plant/rice/stage_*`) ready for external Blockbench 3D model delivery

## 3. Test Suites & Verification Gate

- [x] 3.1 Synchronize all test fixtures across `croppy-api`, `croppy-core`, and `croppy-paper` that assert old IDs (`speed_gro`, `retaining_soil`, `copper_can`, etc.) to new neutral identifiers
- [x] 3.2 Execute `./gradlew.bat test` and verify that all test suites pass with 0 errors
