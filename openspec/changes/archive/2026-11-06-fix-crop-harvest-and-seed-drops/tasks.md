# Tasks

## 1. ItemFactory Enhancement

- [x] 1.1 Add `resolveItemStack(String identifier, int amount)` to `ItemFactory` supporting both custom `ItemDefinition` keys and vanilla Bukkit materials, and verify with unit tests in `ItemFactoryTest`
- [x] 1.2 Verify edge cases in `ItemFactoryTest` (e.g. unknown custom item, invalid material string, negative or zero amounts) returning `Optional.empty()`

## 2. Listener Updates and Dependency Injection

- [x] 2.1 Update `VirtualInteractionPacketListener` constructors to accept `ItemFactory` and update `performHarvest`, `performDismantle`, and `DROP_ITEM` to resolve drops via `itemFactory.resolveItemStack`, verifying with unit tests
- [x] 2.2 Update `CropBreakListener` constructors to accept `ItemFactory` and update mature crop harvest drops and immature seed drops to resolve via `itemFactory.resolveItemStack`, verifying with unit tests
- [x] 2.3 Update `CroppyPlugin.registerListeners()` to pass `this.itemFactory` to `CropBreakListener` and `VirtualInteractionPacketListener`

## 3. Verification & Quality Assurance

- [x] 3.1 Execute `.\gradlew.bat test` across all modules (`croppy-api`, `croppy-core`, `croppy-paper`) and verify 100% test pass rate with 0 regressions
