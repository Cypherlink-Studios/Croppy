# Tasks

## 1. Resource Pack Foundation & Directory Hierarchy

- [x] 1.1 Create `resourcepack/pack.mcmeta` with integer `pack_format: 64` and `supported_formats` bounds (63-64), and create a placeholder `resourcepack/pack.png`.
- [x] 1.2 Scaffold the canonical 1.21.8 directory tree under `resourcepack/assets/croppy/` (`items/`, `models/item/`, `models/block/`, `textures/item/`, `textures/block/`, `textures/gui/sprites/`, `font/`, `sounds/`).

## 2. Modern Item Definitions & Font Configuration

- [x] 2.1 Configure `resourcepack/assets/croppy/font/default.json` declaring a `space` font provider with negative advances (`\uF801` through `\uF880`).
- [x] 2.2 Create modern item model definitions in `resourcepack/assets/croppy/items/` for tools, watering cans, recipe book, and misc items (`recipe_book.json`, `copper_can.json`, `gold_can.json`, `iridium_can.json`, `rusty_can.json`, `silver_can.json`, `inspector.json`, `thermometer.json`, `burnt_food.json`).
- [x] 2.3 Create modern item model definitions in `resourcepack/assets/croppy/items/` for station items (`cooking_pot.json`, `skillet.json`, `stove.json`) with `oversized_in_gui: true`.

## 3. Strict Quality Verification

- [x] 3.1 Execute `audit_pack_1218.py` on `resourcepack/` and verify 0 critical errors and 0 warnings.
- [x] 3.2 Execute Gradle test suite (`.\gradlew.bat test`) and verify 100% test pass rate across all modules.
