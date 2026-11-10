# Tasks

## 1. Domain Models & Ports (croppy-api)

- [x] 1.1 Create `CraftingType` enum (`SHAPED`, `SHAPELESS`) and `IngredientChoice` record in `dev.darkblade.croppy.api.crafting` and verify unit tests validate null safety
- [x] 1.2 Create immutable `CraftingRecipe` record supporting shaped matrices, shapeless ingredient lists, recipe book groups, categories, and result quantities, and verify validation tests
- [x] 1.3 Create `CraftingRecipeRegistry` interface port with register, get, unregister, and view methods
- [x] 1.4 Add `getCraftingRecipeRegistry()` to `AddonContext` port and verify compilation

## 2. Core Registry & YAML Loading (croppy-core)

- [x] 2.1 Implement `DefaultCraftingRecipeRegistry` in `dev.darkblade.croppy.core.crafting` with thread-safe storage and verify unit tests
- [x] 2.2 Implement `YamlCraftingRecipeLoader` in `croppy-core` supporting shaped and shapeless YAML definitions with fail-fast validation for mismatched symbols
- [x] 2.3 Expose `CraftingRecipeRegistry` in `CroppyCore` and wire initialization into core services
- [x] 2.4 Add comprehensive JUnit 5 tests covering shaped parsing, shapeless parsing, directory loading, and validation edge cases

## 3. Paper Platform Adapter & Recipe Lifecycle (croppy-paper)

- [x] 3.1 Implement `CraftingRecipeChoiceResolver` in `croppy-paper` converting ingredients to Bukkit `MaterialChoice` (materials and tags) or `ExactChoice` (via `ItemFactory` for custom items) and verify unit tests
- [x] 3.2 Implement `PaperCraftingRecipeService` managing Bukkit `ShapedRecipe` and `ShapelessRecipe` registration, tracking registered keys, and cleanly removing them via `Bukkit.removeRecipe`
- [x] 3.3 Add `crafting.auto-discover-recipes` configuration option and `CraftingRecipeDiscoveryListener` granting recipes on `PlayerJoinEvent`
- [x] 3.4 Wire `PaperCraftingRecipeService` into `CroppyPlugin` enable, disable, and reload cycles
- [x] 3.5 Update `PaperAddonManager` and `PaperAddonContext` to track addon crafting recipes and discover declarative `crafting/**/*.yml` in addon packages
- [x] 3.6 Create Paper unit tests validating recipe translation, ExactChoice PDC preservation, and reload cleanup

## 4. Starter Pack Recipes & End-to-End Verification

- [x] 4.1 Author bundled starter crafting recipes in `croppy-paper/src/main/resources/crafting/` for stations (`cooking_pot`, `stove`, `skillet`), tools (`inspector`, `thermometer`), and watering cans (`wooden_can`, `copper_can`, `iron_can`)
- [x] 4.2 Configure default resource extraction for `crafting/` files in `CroppyPlugin`
- [x] 4.3 Implement an integration test verifying all bundled starter crafting recipes parse cleanly, target registered items, and produce valid Bukkit recipes
- [x] 4.4 Run `./gradlew.bat test` and confirm all test suites across all modules pass with 0 failures
