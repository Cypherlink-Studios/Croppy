# Tasks: Fix Coffee Station Display Models

## 1. Asset File Renaming and Model Target Alignment

- [x] 1.1 Rename `addons/coffee/src/main/resources/assets/croppy_coffee/items/block/expresso_machine.json` to `espresso_machine.json` and update its model target to `"croppy_coffee:block/espresso_machine"`.
- [x] 1.2 Verify all three station item definitions in `assets/croppy_coffee/items/block/` point to valid Blockbench model files in `assets/croppy_coffee/models/block/`.

## 2. Item Definition Alignment

- [x] 2.1 Update `addons/coffee/src/main/resources/items/stations/coffee_grinder.yml` to set `item-model: "croppy_coffee:block/coffee_grinder"`.
- [x] 2.2 Update `addons/coffee/src/main/resources/items/stations/coffee_roaster.yml` to set `item-model: "croppy_coffee:block/coffee_roaster"`.
- [x] 2.3 Update `addons/coffee/src/main/resources/items/stations/espresso_machine.yml` to set `item-model: "croppy_coffee:block/espresso_machine"`.
- [x] 2.4 Align `seeds/coffee_sapling.yml`, `seeds/coffee_seeds.yml`, and `ingredients/green_coffee_beans.yml` to `croppy_coffee:item/<id>` matching their asset paths in `assets/croppy_coffee/items/item/`.

## 3. Station Base Model Identifier Correction

- [x] 3.1 Update `addons/coffee/src/main/resources/stations/coffee_grinder.yml` to set `visual.base_model.item: "croppy_coffee:coffee_grinder"`.
- [x] 3.2 Update `addons/coffee/src/main/resources/stations/coffee_roaster.yml` to set `visual.base_model.item: "croppy_coffee:coffee_roaster"`.
- [x] 3.3 Update `addons/coffee/src/main/resources/stations/espresso_machine.yml` to set `visual.base_model.item: "croppy_coffee:espresso_machine"`.

## 4. Automated Integrity Testing & Quality Assurance

- [x] 4.1 Enhance `CoffeeResourcesIntegrityTest` to validate that all station `base_model.item` keys exist in `items/stations/` and that all `item-model` keys match physical asset JSON files.
- [x] 4.2 Execute Gradle test suites on `Croppy-addons` and `Croppy` root, ensuring all tests pass with zero errors.
