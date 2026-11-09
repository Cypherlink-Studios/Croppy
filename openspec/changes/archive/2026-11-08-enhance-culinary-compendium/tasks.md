# Tasks: Enhance Culinary Compendium

## 1. Category Registry & Addon Lifecycle Foundation

- [x] 1.1 Define `RecipeCategoryRegistry` interface port in `croppy-api` extending `Registry<RecipeCategory>` and `Service`, and verify compilation with `./gradlew :croppy-api:compileJava`.
- [x] 1.2 Implement `DefaultRecipeCategoryRegistry` in `croppy-core` pre-populated with standard categories (`ALL`, `BAKERY`, `SOUPS`, `COOKING`, `BEVERAGES`, `PRESERVES`), and verify with unit tests in `DefaultRecipeCategoryRegistryTest`.
- [x] 1.3 Register `RecipeCategoryRegistry` as a core service in `CroppyCore.java` and expose `RecipeCategoryRegistry getRecipeCategoryRegistry()` in `AddonContext.java`.
- [x] 1.4 Update `PaperAddonContext` to track registered category keys and update `AddonDecommissioner` (Phase 4) to clean up addon categories upon disable or unload.

## 2. Compendium Card Formatting, Proximity & Filtering

- [x] 2.1 Extend `CompendiumSession` in `CompendiumMenuController` to track `searchQuery`, `onlyReadyToCook`, and category pagination state.
- [x] 2.2 Refactor `renderDiscoveredCard` in `CompendiumMenuController` to format ingredients with localized display names via `PaperMessageService`, station display name, thermal bounds and duration from `ProcessRegistry`, and nearby station detection via `DefaultStationManager`.
- [x] 2.3 Implement active category tab visual feedback (enchantment glint and active title indicator) and horizontal category pagination in `CompendiumMenuController`.
- [x] 2.4 Implement search query filtering and "Ready to Cook" inventory satisfaction filtering in `CompendiumMenuController`, supporting `compendium.show-unknown-recipes` visibility toggling.

## 3. Recipe Detail Inspection Sub-Menu

- [x] 3.1 Author bundled default `croppy-paper/src/main/resources/menus/recipe_detail_menu.yml` defining layout for dish result, required ingredients, sequential phase timeline, and back navigation.
- [x] 3.2 Implement `RecipeDetailMenuController` in `dev.darkblade.croppy.platform.paper.menu` to render phase cards (`TIMED`, `MANUAL_INTERACTION`, `ENVIRONMENTAL`) with operating temperatures, durations, timeouts, manual items, and spoil policies.
- [x] 3.3 Update `PaperMenuListener` to intercept clicks on discovered recipe cards in `recipes_menu.yml` to launch the detail view, and handle back navigation returning to the originating compendium session.

## 4. Commands, Localization, Configuration & Verification

- [x] 4.1 Update `CroppyCommands.java` to support `/croppy recipes search <query>`, `/croppy recipes ready`, and dynamic tab completions from `RecipeCategoryRegistry`.
- [x] 4.2 Add `compendium:` configuration section to `config.yml` (`show-unknown-recipes`, `enable-detail-view`, `station-proximity-distance`).
- [x] 4.3 Synchronize `en-US.yml` and `es-ES.yml` with all new compendium GUI strings, detail inspection labels, search feedback, and filter status lines.
- [x] 4.4 Update `CompendiumMenuControllerTest` and add unit tests for `RecipeDetailMenuController`, verifying that `./gradlew.bat test` passes with 0 failures across all 28 test suites.
