# Tasks: Native Item Models Support (Minecraft 1.21.4+)

## 1. Domain API Contracts (`croppy-api`)

- [x] 1.1 Extend `ItemDefinition` record with optional `Key itemModel`, overloaded 7-arg constructor, and `getItemModel()` accessor, verifying with unit tests in `croppy-api`

## 2. Core Deserialization Engine (`croppy-core`)

- [x] 2.1 Update `YamlItemLoader` to parse `item-model` and `item_model` keys from flat and nested item YAML definitions into a namespaced `Key`
- [x] 2.2 Add unit tests in `croppy-core` testing parsing of items with `item-model`, both CMD and item-model, and missing item-model

## 3. Platform Adapter & Stack Generation (`croppy-paper`)

- [x] 3.1 Update `ItemFactory` in `croppy-paper` to apply `meta.setItemModel(NamespacedKey)` when `itemModel` is present on the `ItemDefinition`
- [x] 3.2 Add unit tests in `croppy-paper` validating `ItemFactory#createItemStack` sets `itemModel` and preserves `customModelData`
- [x] 3.3 Update bundled item YAML definitions in resources to include modern `item-model` keys alongside `custom-model-data`
- [x] 3.4 Execute full Gradle test suite `./gradlew.bat test` and verify zero regressions across all modules
