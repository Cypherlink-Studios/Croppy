# Proposal: Enhance Culinary Compendium

## Why

The Culinary Compendium serves as the central encyclopedia and cookbook for players navigating Croppy's culinary mechanics. While the core catalog and knowledge state tracking exist, the current system exhibits severe usability and architectural limitations:
1. Category tabs are hardcoded in menu YAMLs, preventing expansion addons (such as `croppy-coffee`) from exposing custom culinary categories cleanly.
2. Recipe cards display raw namespaced keys (`input.itemKey().value()`) instead of localized display names, and omit vital cooking metadata (thermal zones, process durations, failure policies, station proximity).
3. Clicking recipes in the grid is non-interactive (`SlotType.DECORATION`), leaving players without an in-depth view of sequential cooking phases or manual interactions.
4. Players lack search capabilities and survival-friendly filtering (e.g. "Ready to Cook" based on current inventory).

Resolving these gaps now will establish a polished, addon-friendly, and comprehensive cookbook experience across the platform.

## What Changes

- **Recipe Category Registry (`croppy-api` & `croppy-core`)**:
  - Introduce `RecipeCategoryRegistry` port in `croppy-api` and `DefaultRecipeCategoryRegistry` in `croppy-core`.
  - Expose category registration to `AddonContext` for declarative/runtime addon category contributions and decommission lifecycle.
- **Dynamic Category Navigation & Active Indicator (`croppy-paper`)**:
  - Support category pagination/scrolling in the compendium GUI for setups with numerous categories.
  - Highlight the currently active category tab with an enchantment glint and active formatting indicator.
  - Dynamically populate command suggestions in `/croppy recipes <tab>` from the category registry.
- **Recipe Inspection & Detail Sub-Menu (`croppy-paper`)**:
  - Enable interactive clicking on discovered recipe cards to open `recipe_detail_menu.yml`.
  - Display sequential process phases (`TIMED`, `MANUAL_INTERACTION`, `ENVIRONMENTAL`), thermal operating zones, timeouts, prompt messages, and failure/spoil policies.
  - Provide a back navigation action returning to the player's previous compendium page and category.
- **Search & Inventory-Ready Filtering**:
  - Add search functionality via `/croppy recipes search <query>` matching recipe names, result items, and ingredients.
  - Add "Ready to Cook" toggle filter displaying only discovered recipes whose ingredients are fully present in the player's inventory.
  - Add configuration setting `compendium.show-unknown-recipes` in `config.yml` to toggle visibility of undiscovered recipes.
- **Rich Card Formatting & Proximity**:
  - Format ingredient counts using localized item names and MiniMessage styling via `PaperMessageService`.
  - Display thermal zone and estimated cooking duration extracted from the linked `ProcessDefinition`.
  - Check and display workstation proximity (`✔ Nearby` / `✖ Not Nearby`) based on a 5-block radius scan.

## Capabilities

### Modified Capabilities
- `culinary-compendium`: Adds requirements for recipe inspection detail sub-menu, dynamic category navigation/registration, search query execution, "ready to cook" inventory filtering, and station proximity indication.
- `crop-commands`: Adds `/croppy recipes search <query>`, `/croppy recipes ready`, and dynamic category completion arguments.
- `addon-system`: Expands addon context and decommission lifecycle to register and deregister custom recipe categories.

## Impact

- **API Changes (`croppy-api`)**:
  - New `dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry` interface.
  - Extended `AddonContext` with `RecipeCategoryRegistry getRecipeCategoryRegistry()`.
- **Core Changes (`croppy-core`)**:
  - New `DefaultRecipeCategoryRegistry` implementation pre-populated with standard categories (`ALL`, `BAKERY`, `SOUPS`, `COOKING`, `BEVERAGES`, `PRESERVES`).
  - Integration into `CroppyCore` service registry.
- **Platform Changes (`croppy-paper`)**:
  - New `RecipeDetailMenuController` and bundled default `menus/recipe_detail_menu.yml`.
  - Enhanced `CompendiumMenuController` supporting search, ready-to-cook filtering, rich ingredient resolution, and category scrolling.
  - Updated `PaperMenuListener` handling recipe card clicks and detail back button.
  - Updated `CroppyCommands` with search, ready, and dynamic category autocompletions.
  - Synchronized `en-US.yml` and `es-ES.yml` with all new GUI, search, and detail message keys.
